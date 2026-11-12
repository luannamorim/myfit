package io.github.luannamorim.myfit.ui.feature.settings

import android.net.Uri
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.data.backup.BackupManager
import io.github.luannamorim.myfit.data.backup.FileBackupDestination
import io.github.luannamorim.myfit.data.backup.KeyState
import io.github.luannamorim.myfit.data.images.ImageStore
import io.github.luannamorim.myfit.data.prefs.BackupStateStore
import io.github.luannamorim.myfit.data.repository.BackupTrigger
import io.github.luannamorim.myfit.platform.backup.BackupScheduler
import io.github.luannamorim.myfit.testutil.MainDispatcherRule
import io.github.luannamorim.myfit.testutil.SessionEnv
import io.github.luannamorim.myfit.testutil.TEST_ITERATIONS
import io.github.luannamorim.myfit.testutil.awaitUntil
import io.github.luannamorim.myfit.testutil.testKeyStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File
import java.nio.file.Files

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class BackupViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val env = SessionEnv()
    private val keyStore = testKeyStore()
    private val manager = BackupManager(env.context, env.db, env.settings, keyStore, ImageStore(env.context, Dispatchers.Unconfined), env.repo, BackupTrigger { }, env.time, Dispatchers.Unconfined)
    private val dir: File = Files.createTempDirectory("vm").toFile()
    private val store = BackupStateStore(PreferenceDataStoreFactory.create(scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)) { File(dir, "state.preferences_pb") })
    private val scheduler = BackupScheduler(env.context, store, CoroutineScope(Dispatchers.Unconfined))

    @After fun tearDown() = env.close()

    private fun vm() = BackupViewModel(env.context, manager, keyStore, store, env.settings, { FileBackupDestination(File(it)) }, scheduler)

    private fun backupFile(): Uri {
        val file = File(dir, "b.myfit")
        runBlocking { file.outputStream().use { manager.create(it) } }
        return Uri.fromFile(file)
    }

    @Test
    fun export_withoutPassword_reportsNoPassword() = runBlocking {
        val vm = vm()
        vm.state.awaitUntil { !it.loading }
        vm.exportTo(Uri.fromFile(File(dir, "out.myfit")))
        assertEquals(BackupMessage.NO_PASSWORD, vm.state.awaitUntil { it.message != null }.message)
    }

    @Test
    fun restore_showsBackupDateBeforeConfirm() = runBlocking {
        keyStore.setPassword("senha123".toCharArray(), TEST_ITERATIONS)
        val uri = backupFile()
        val vm = vm()
        vm.state.awaitUntil { it.keyState == KeyState.READY }
        vm.prepareRestore(uri)
        val pending = vm.state.awaitUntil { it.pending != null }.pending!!
        assertEquals(env.time.now().wallMs, pending.createdAtMs)
        assertEquals(false, pending.needsPassword)
        vm.confirmRestore(null)
        assertEquals(BackupMessage.RESTORED, vm.state.awaitUntil { it.message != null }.message)
    }

    @Test
    fun restoreResult_wrongPassword_flagsPasswordError() = runBlocking {
        keyStore.setPassword("senha123".toCharArray(), TEST_ITERATIONS)
        val uri = backupFile()
        keyStore.setPassword("outra-senha".toCharArray(), TEST_ITERATIONS)
        val vm = vm()
        vm.state.awaitUntil { !it.loading }
        vm.prepareRestore(uri)
        assertTrue(vm.state.awaitUntil { it.pending != null }.pending!!.needsPassword)
        vm.confirmRestore("errada".toCharArray())
        assertTrue(vm.state.awaitUntil { it.pending?.passwordError == true }.pending!!.passwordError)
    }

    @Test
    fun chooseFolder_listsExistingBackups() = runBlocking {
        keyStore.setPassword("senha123".toCharArray(), TEST_ITERATIONS)
        val folder = Files.createTempDirectory("folder").toFile()
        FileBackupDestination(folder).write("myfit-backup-20261101-120000.myfit") { manager.create(it) }
        val vm = vm()
        vm.state.awaitUntil { !it.loading }
        vm.chooseFolder(Uri.parse(folder.path))
        val state = vm.state.awaitUntil { it.files.isNotEmpty() && it.backup.folderUri != null }
        assertEquals("myfit-backup-20261101-120000.myfit", state.files.single().name)
        assertEquals(folder.path, state.backup.folderUri)
    }
}
