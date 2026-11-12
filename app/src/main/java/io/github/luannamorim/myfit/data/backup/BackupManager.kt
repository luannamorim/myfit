package io.github.luannamorim.myfit.data.backup

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.luannamorim.myfit.BuildConfig
import io.github.luannamorim.myfit.core.time.TimeSource
import io.github.luannamorim.myfit.data.db.MyFitDatabase
import io.github.luannamorim.myfit.data.images.ImageStore
import io.github.luannamorim.myfit.data.prefs.SettingsRepository
import io.github.luannamorim.myfit.data.repository.BackupTrigger
import io.github.luannamorim.myfit.data.repository.SessionRepository
import io.github.luannamorim.myfit.di.IoDispatcher
import io.github.luannamorim.myfit.domain.backup.BackupArchive
import io.github.luannamorim.myfit.domain.backup.BackupCrypto
import io.github.luannamorim.myfit.domain.backup.BackupError
import io.github.luannamorim.myfit.domain.backup.BackupHeader
import io.github.luannamorim.myfit.domain.backup.KeyDerivation
import io.github.luannamorim.myfit.domain.backup.ManifestMeta
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FilterOutputStream
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class BackupInfo(val createdAtMs: Long, val sizeBytes: Long, val counts: Map<String, Int>)

sealed interface RestoreResult {
    data class Success(val counts: Map<String, Int>) : RestoreResult
    data object WrongPassword : RestoreResult
    /** The file was made with another password than the one stored on this device. */
    data object PasswordRequired : RestoreResult
    data class Corrupted(val reason: String) : RestoreResult
    data object UnsupportedVersion : RestoreResult
    data object InsufficientSpace : RestoreResult
    data object SessionActive : RestoreResult
}

/**
 * Creates encrypted backups of everything and restores them atomically:
 * the database is replaced in one transaction, images are merged before the commit (unique
 * names) and stale ones are removed after it, so a crash at any point leaves a consistent state.
 */
@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: MyFitDatabase,
    private val settings: SettingsRepository,
    private val keyStore: BackupKeyStore,
    private val images: ImageStore,
    private val sessions: SessionRepository,
    private val backupTrigger: BackupTrigger,
    private val time: TimeSource,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    private val mutex = Mutex()
    private val stagingDir get() = File(context.filesDir, "restore_staging")
    private val workZip get() = File(context.cacheDir, "restore.zip")

    /** Writes an encrypted backup to [out]. Throws [BackupError.NoPassword] when no key is set. */
    suspend fun create(out: OutputStream): BackupInfo = mutex.withLock {
        withContext(io) {
            val key = keyStore.key() ?: throw BackupError.NoPassword()
            val now = time.now().wallMs
            val data = SnapshotMapper.export(db)
            val imageFiles = data.referencedImages().associateWith { images.file(it) }.filterValues { it.isFile }
            val tmp = File(context.cacheDir, "backup-$now.zip")
            try {
                tmp.outputStream().use {
                    BackupArchive.write(it, data, settings.exportJson(), imageFiles, ManifestMeta(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, now, MyFitDatabase.VERSION))
                }
                val header = BackupHeader(iterations = key.iterations, salt = key.salt, keyCheck = key.keyCheck, createdAtMs = now)
                val counting = CountingOutputStream(out)
                tmp.inputStream().use { BackupCrypto.encrypt(key.key, header, it, counting) }
                BackupInfo(now, counting.count, data.counts())
            } finally {
                tmp.delete()
            }
        }
    }

    /** Reads only the header (date shown before confirming a restore). */
    suspend fun inspect(input: InputStream): BackupHeader = withContext(io) { input.use { BackupHeader.read(it) } }

    /**
     * Restores a backup. With [password] == null the key stored on this device is used, which only
     * works for backups made with the same password setup ([RestoreResult.PasswordRequired] otherwise).
     */
    suspend fun restore(input: InputStream, password: CharArray?, sizeBytes: Long? = null): RestoreResult = mutex.withLock {
        withContext(io) {
            if (sessions.getActive() != null) return@withContext RestoreResult.SessionActive
            try {
                input.use { stream -> restoreLocked(stream, password, sizeBytes) }
            } catch (e: BackupError.WrongPassword) {
                RestoreResult.WrongPassword
            } catch (e: BackupError.UnsupportedVersion) {
                RestoreResult.UnsupportedVersion
            } catch (e: BackupError) {
                RestoreResult.Corrupted(e.message ?: "corrupted")
            } finally {
                workZip.delete()
                stagingDir.deleteRecursively()
            }
        }
    }

    private suspend fun restoreLocked(stream: InputStream, password: CharArray?, sizeBytes: Long?): RestoreResult {
        val header = BackupHeader.read(stream)
        val key = if (password != null) {
            KeyDerivation.derive(password, header.salt, header.iterations)
        } else {
            keyStore.key()?.takeIf { it.salt.contentEquals(header.salt) && it.iterations == header.iterations }?.key
                ?: return RestoreResult.PasswordRequired
        }
        if (!BackupCrypto.keyMatches(key, header)) {
            return if (password != null) RestoreResult.WrongPassword else RestoreResult.PasswordRequired
        }
        @Suppress("UsableSpace") // a rough pre-check is enough; failures while writing are handled too
        if (sizeBytes != null && context.cacheDir.usableSpace < sizeBytes * 2) return RestoreResult.InsufficientSpace

        // 1. Decrypt fully (truncation/tampering is only detected at the end) and verify the archive.
        workZip.outputStream().use { BackupCrypto.decrypt(key, header, stream, it) }
        val contents = BackupArchive.read(workZip, stagingDir)

        // 2. Merge images before the commit: names are unique, current images stay untouched.
        val imagesRoot = context.filesDir
        File(stagingDir, "images").walkTopDown().filter { it.isFile }.forEach { file ->
            val target = File(imagesRoot, file.relativeTo(stagingDir).invariantSeparatorsPath)
            target.parentFile?.mkdirs()
            if (!file.renameTo(target)) file.copyTo(target, overwrite = true)
        }

        // 3. Atomic data swap, then settings and cleanup of images nobody references anymore.
        backupTrigger.cancelPending()
        SnapshotMapper.replaceAll(db, contents.data)
        settings.importJson(contents.settingsJson)
        images.deleteOrphans(contents.data.referencedImages())

        // 4. Validate the result.
        val restored = SnapshotMapper.export(db)
        if (restored.counts() != contents.manifest.counts) throw BackupError.Corrupted("restored counts differ")
        val missing = contents.manifest.entries.filter { BackupArchive.isSafeImagePath(it.path) && !images.file(it.path).isFile }
        if (missing.isNotEmpty()) throw BackupError.Corrupted("missing images after restore: ${missing.size}")

        // The password that opened this backup becomes this device's backup password. A file's KDF
        // parameters are only adopted when they are at least as strong as ours; otherwise re-derive.
        if (password != null) {
            if (header.iterations >= KeyDerivation.DEFAULT_ITERATIONS) {
                keyStore.adopt(StoredKey(key, header.salt, header.iterations))
            } else {
                keyStore.setPassword(password)
            }
        }
        return RestoreResult.Success(restored.counts())
    }

    /** Cleans leftovers of a restore interrupted by process death (safe to call at app start). */
    suspend fun recoverInterruptedRestore() = mutex.withLock {
        withContext(io) {
            if (stagingDir.exists() || workZip.exists()) {
                stagingDir.deleteRecursively()
                workZip.delete()
                images.deleteOrphans(SnapshotMapper.export(db).referencedImages())
            }
        }
    }

    private class CountingOutputStream(out: OutputStream) : FilterOutputStream(out) {
        var count = 0L
            private set

        override fun write(b: Int) {
            out.write(b)
            count++
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            out.write(b, off, len)
            count += len
        }
    }
}
