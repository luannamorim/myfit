package io.github.luannamorim.myfit.platform.backup

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.work.workDataOf
import io.github.luannamorim.myfit.data.backup.BackupDestination
import io.github.luannamorim.myfit.data.backup.BackupDestinationFactory
import io.github.luannamorim.myfit.data.backup.BackupManager
import io.github.luannamorim.myfit.data.backup.FileBackupDestination
import io.github.luannamorim.myfit.data.backup.FolderBackupWriter
import io.github.luannamorim.myfit.data.images.ImageStore
import io.github.luannamorim.myfit.data.prefs.BackupFailure
import io.github.luannamorim.myfit.data.prefs.BackupStateStore
import io.github.luannamorim.myfit.data.repository.BackupTrigger
import io.github.luannamorim.myfit.domain.backup.BackupError
import io.github.luannamorim.myfit.domain.backup.BackupFileInfo
import io.github.luannamorim.myfit.testutil.SessionEnv
import io.github.luannamorim.myfit.testutil.TEST_ITERATIONS
import io.github.luannamorim.myfit.testutil.testKeyStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File
import java.io.OutputStream
import java.nio.file.Files

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class BackupWorkerTest {
    private val env = SessionEnv()
    private val context: Context = env.context
    private val keyStore = testKeyStore()
    private val manager = BackupManager(context, env.db, env.settings, keyStore, ImageStore(context, Dispatchers.Unconfined), env.repo, BackupTrigger { }, env.time, Dispatchers.Unconfined)
    private val writer = FolderBackupWriter(manager, env.time)
    private val dir: File = Files.createTempDirectory("backups").toFile()
    private val store = BackupStateStore(
        PreferenceDataStoreFactory.create(scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)) { File(Files.createTempDirectory("state").toFile(), "s.preferences_pb") },
    )
    private var factory: BackupDestinationFactory = BackupDestinationFactory { FileBackupDestination(dir, env.time.zone()) }

    @After fun tearDown() = env.close()

    private fun worker(force: Boolean = false): BackupWorker =
        TestListenableWorkerBuilder<BackupWorker>(context)
            .setInputData(workDataOf(BackupWorker.KEY_FORCE to force))
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(appContext: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker =
                    BackupWorker(appContext, workerParameters, store, env.settings, factory, writer, BackupNotifier(appContext), env.time)
            })
            .build()

    private suspend fun configured() {
        keyStore.setPassword("senha".toCharArray(), TEST_ITERATIONS)
        store.setFolder("folder", "MyFit Backup")
    }

    private fun files() = dir.list().orEmpty().filter { it.endsWith(".myfit") }

    @Test
    fun notConfigured_notForced_skipsSilently() = runBlocking {
        store.markChanged()
        assertTrue(worker().doWork() is ListenableWorker.Result.Success)
        assertNull(store.current().lastError)
        assertTrue(files().isEmpty())
    }

    @Test
    fun clean_notForced_skips() = runBlocking {
        configured()
        assertTrue(worker(force = true).doWork() is ListenableWorker.Result.Success)
        assertEquals(1, files().size)
        env.time.advance(60_000)
        assertTrue(worker().doWork() is ListenableWorker.Result.Success) // nothing changed since
        assertEquals(1, files().size)
    }

    @Test
    fun dirty_writes_andClearsDirty() = runBlocking {
        configured()
        store.markChanged()
        assertTrue(worker().doWork() is ListenableWorker.Result.Success)
        val state = store.current()
        assertFalse(state.hasUnsavedChanges)
        assertEquals(files().single(), state.lastFileName)
        assertNull(state.lastError)
    }

    @Test
    fun changeDuringRun_keepsDirty() = runBlocking {
        configured()
        store.markChanged()
        factory = BackupDestinationFactory {
            val real = FileBackupDestination(dir, env.time.zone())
            object : BackupDestination by real {
                override suspend fun write(name: String, block: suspend (OutputStream) -> Unit): BackupFileInfo {
                    store.markChanged() // the user edits something while the backup is being written
                    return real.write(name, block)
                }
            }
        }
        assertTrue(worker().doWork() is ListenableWorker.Result.Success)
        assertTrue(store.current().hasUnsavedChanges)
    }

    @Test
    fun folderUnavailable_setsError_keepsDirty_noRetry() = runBlocking {
        configured()
        store.markChanged()
        factory = BackupDestinationFactory { throw BackupError.FolderUnavailable("revoked") }
        assertTrue(worker().doWork() is ListenableWorker.Result.Failure)
        assertEquals(BackupFailure.FOLDER_UNAVAILABLE, store.current().lastError)
        assertTrue(store.current().hasUnsavedChanges)
    }

    @Test
    fun noPassword_setsError() = runBlocking {
        store.setFolder("folder", null)
        assertTrue(worker(force = true).doWork() is ListenableWorker.Result.Failure)
        assertEquals(BackupFailure.NO_PASSWORD, store.current().lastError)
    }

    @Test
    fun forced_withoutFolder_reportsNoFolder() = runBlocking {
        assertTrue(worker(force = true).doWork() is ListenableWorker.Result.Failure)
        assertEquals(BackupFailure.NO_FOLDER, store.current().lastError)
    }

    @Test
    fun onDataChanged_marksChanged_andEnqueuesDebounce() = runBlocking {
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        val scheduler = BackupScheduler(context, store, CoroutineScope(Dispatchers.Unconfined))
        scheduler.onDataChanged()
        assertTrue(store.current().hasUnsavedChanges)
        val infos = WorkManager.getInstance(context).getWorkInfosForUniqueWork(BackupScheduler.DEBOUNCE).get()
        assertEquals(WorkInfo.State.ENQUEUED, infos.single().state)
        scheduler.cancelPending()
        assertEquals(WorkInfo.State.CANCELLED, WorkManager.getInstance(context).getWorkInfosForUniqueWork(BackupScheduler.DEBOUNCE).get().single().state)
    }
}
