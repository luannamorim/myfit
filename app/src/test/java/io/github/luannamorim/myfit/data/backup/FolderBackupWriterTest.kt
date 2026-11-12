package io.github.luannamorim.myfit.data.backup

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.data.images.ImageStore
import io.github.luannamorim.myfit.data.repository.BackupTrigger
import io.github.luannamorim.myfit.domain.backup.BackupError
import io.github.luannamorim.myfit.domain.backup.BackupFileInfo
import io.github.luannamorim.myfit.testutil.SessionEnv
import io.github.luannamorim.myfit.testutil.TEST_ITERATIONS
import io.github.luannamorim.myfit.testutil.testKeyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File
import java.io.InputStream
import java.nio.file.Files

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class FolderBackupWriterTest {
    private val env = SessionEnv()
    private val keyStore = testKeyStore()
    private val manager = BackupManager(env.context, env.db, env.settings, keyStore, ImageStore(env.context, Dispatchers.Unconfined), env.repo, BackupTrigger { }, env.time, Dispatchers.Unconfined)
    private val writer = FolderBackupWriter(manager, env.time)
    private val dir: File = Files.createTempDirectory("folder").toFile()
    private val destination = FileBackupDestination(dir, env.time.zone())

    @After fun tearDown() = env.close()

    @Test
    fun run_writesValidatesThenPrunes() = runBlocking {
        keyStore.setPassword("senha".toCharArray(), TEST_ITERATIONS)
        (1..9).forEach { File(dir, "myfit-backup-202601${"%02d".format(it)}-120000.myfit").writeText("old") }
        File(dir, "outra-coisa.txt").writeText("keep me")
        val info = writer.run(destination)
        val names = dir.list()!!.sorted()
        assertEquals(8, names.size) // 7 backups + the foreign file
        assertTrue(info.name in names)
        assertTrue("outra-coisa.txt" in names)
        assertTrue(names.none { it.endsWith(".partial") })
    }

    @Test
    fun run_validationFails_doesNotPrune() = runBlocking {
        keyStore.setPassword("senha".toCharArray(), TEST_ITERATIONS)
        (1..9).forEach { File(dir, "myfit-backup-202601${"%02d".format(it)}-120000.myfit").writeText("old") }
        val broken = object : BackupDestination by destination {
            override suspend fun open(file: BackupFileInfo): InputStream = "garbage-not-a-backup-header-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx".byteInputStream()
        }
        try {
            writer.run(broken)
            fail("expected Corrupted")
        } catch (e: BackupError.Corrupted) {
        }
        assertEquals(10, dir.list()!!.count { it.endsWith(".myfit") })
    }

    @Test
    fun run_withoutPassword_throwsNoPassword_andLeavesNoFiles() = runBlocking {
        try {
            writer.run(destination)
            fail("expected NoPassword")
        } catch (e: BackupError.NoPassword) {
        }
        assertEquals(0, dir.list()!!.size)
    }
}
