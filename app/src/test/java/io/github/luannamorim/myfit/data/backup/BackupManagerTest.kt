package io.github.luannamorim.myfit.data.backup

import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.data.images.ImageKind
import io.github.luannamorim.myfit.data.images.ImageStore
import io.github.luannamorim.myfit.data.repository.BackupTrigger
import io.github.luannamorim.myfit.domain.backup.BackupError
import io.github.luannamorim.myfit.domain.backup.KeyDerivation
import io.github.luannamorim.myfit.domain.model.ThemeMode
import io.github.luannamorim.myfit.testutil.SessionEnv
import io.github.luannamorim.myfit.testutil.TEST_ITERATIONS
import io.github.luannamorim.myfit.testutil.populateEverything
import io.github.luannamorim.myfit.testutil.testKeyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class BackupManagerTest {
    private val env = SessionEnv()
    private val images = ImageStore(env.context, Dispatchers.Unconfined)
    private val keyStore = testKeyStore()
    private var cancelled = 0
    private val trigger = object : BackupTrigger {
        override fun onDataChanged() {}
        override fun cancelPending() { cancelled++ }
    }
    private val manager = BackupManager(env.context, env.db, env.settings, keyStore, images, env.repo, trigger, env.time, Dispatchers.Unconfined)

    @After fun tearDown() = env.close()

    private suspend fun photo(name: String): String {
        val file = File(env.context.cacheDir, "$name.jpg")
        file.outputStream().use { Bitmap.createBitmap(40, 60, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.CYAN) }.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        return images.import(Uri.fromFile(file), ImageKind.BODY)
    }

    private suspend fun finishActive() {
        env.controller.finish(updateRoutineLoads = false)
    }

    private suspend fun backupBytes(): ByteArray {
        keyStore.key() ?: keyStore.setPassword("minha senha".toCharArray(), TEST_ITERATIONS)
        return ByteArrayOutputStream().also { manager.create(it) }.toByteArray()
    }

    @Test
    fun createRestore_roundTrip_restoresDbImagesAndSettings() = runBlocking {
        val path = photo("p")
        val photoBytes = images.file(path).readBytes()
        env.populateEverything(listOf(path))
        finishActive()
        env.settings.update { it.copy(themeMode = ThemeMode.LIGHT, defaultRestSeconds = 120) }
        val before = SnapshotMapper.export(env.db)
        val backup = backupBytes()

        // Wipe local state.
        io.github.luannamorim.myfit.data.backup.SnapshotMapper.replaceAll(env.db, io.github.luannamorim.myfit.domain.backup.BackupDataV1())
        images.delete(path)
        env.settings.update { it.copy(themeMode = ThemeMode.DARK, defaultRestSeconds = 60) }
        val orphan = photo("orphan")

        val result = manager.restore(ByteArrayInputStream(backup), password = null)
        assertTrue("$result", result is RestoreResult.Success)
        assertEquals(before, SnapshotMapper.export(env.db))
        assertArrayEquals(photoBytes, images.file(path).readBytes())
        assertFalse("orphan image removed", images.file(orphan).exists())
        assertEquals(ThemeMode.LIGHT, env.settings.current().themeMode)
        assertEquals(120, env.settings.current().defaultRestSeconds)
        assertEquals(1, cancelled)
    }

    @Test
    fun restore_wrongPassword_leavesLocalDataUntouched() = runBlocking {
        env.populateEverything()
        finishActive()
        val backup = backupBytes()
        val before = SnapshotMapper.export(env.db)
        assertEquals(RestoreResult.WrongPassword, manager.restore(ByteArrayInputStream(backup), "errada".toCharArray()))
        assertEquals(before, SnapshotMapper.export(env.db))
    }

    @Test
    fun restore_corrupted_leavesLocalDataUntouched() = runBlocking {
        env.populateEverything()
        finishActive()
        val backup = backupBytes()
        backup[backup.size / 2] = (backup[backup.size / 2].toInt() xor 0xFF).toByte()
        val before = SnapshotMapper.export(env.db)
        assertTrue(manager.restore(ByteArrayInputStream(backup), null) is RestoreResult.Corrupted)
        assertEquals(before, SnapshotMapper.export(env.db))
        assertFalse(File(env.context.filesDir, "restore_staging").exists())
    }

    @Test
    fun restore_withActiveSession_refused() = runBlocking {
        val backup = backupBytes()
        env.startWorkout(1)
        assertEquals(RestoreResult.SessionActive, manager.restore(ByteArrayInputStream(backup), null))
    }

    @Test
    fun restore_otherPassword_requiresPassword_thenStoresThatPassword() = runBlocking {
        val backup = backupBytes() // weak test KDF parameters
        keyStore.setPassword("nova senha".toCharArray(), TEST_ITERATIONS) // e.g. a new phone with another setup
        assertEquals(RestoreResult.PasswordRequired, manager.restore(ByteArrayInputStream(backup), null))
        assertTrue(manager.restore(ByteArrayInputStream(backup), "minha senha".toCharArray()) is RestoreResult.Success)
        // Weak parameters from a file are never adopted: the password is re-derived with the defaults.
        val stored = keyStore.key()!!
        assertEquals(KeyDerivation.DEFAULT_ITERATIONS, stored.iterations)
        assertTrue(keyStore.unlock("minha senha".toCharArray()))
    }

    @Test
    fun create_withoutPassword_throwsNoPassword() = runBlocking {
        try {
            manager.create(ByteArrayOutputStream())
            fail("expected NoPassword")
        } catch (e: BackupError.NoPassword) {
        }
    }

    @Test
    fun recoverInterruptedRestore_cleansLeftovers() = runBlocking {
        val staging = File(env.context.filesDir, "restore_staging/images/body").apply { mkdirs() }
        File(staging, "x.jpg").writeText("x")
        val orphan = photo("leftover")
        manager.recoverInterruptedRestore()
        assertFalse(File(env.context.filesDir, "restore_staging").exists())
        assertFalse(images.file(orphan).exists())
    }
}
