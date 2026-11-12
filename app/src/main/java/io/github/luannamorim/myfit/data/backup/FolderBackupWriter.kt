package io.github.luannamorim.myfit.data.backup

import io.github.luannamorim.myfit.core.time.TimeSource
import io.github.luannamorim.myfit.domain.backup.BackupError
import io.github.luannamorim.myfit.domain.backup.BackupFileInfo
import io.github.luannamorim.myfit.domain.backup.BackupHeader
import io.github.luannamorim.myfit.domain.backup.Retention
import javax.inject.Inject

/** Writes a new backup into the folder, checks it and then prunes old ones (keeps 7). */
class FolderBackupWriter @Inject constructor(
    private val manager: BackupManager,
    private val time: TimeSource,
) {
    suspend fun run(destination: BackupDestination): BackupFileInfo {
        val name = Retention.fileName(time.now().wallMs, time.zone())
        val info = destination.write(name) { out -> manager.create(out) }
        // Read the file back before deleting anything older.
        if (info.sizeBytes <= BackupHeader.SIZE) throw BackupError.Corrupted("written file too small")
        destination.open(info).use { BackupHeader.read(it) }
        // Never prune the file just written (a clock set back would sort it as the oldest).
        Retention.toDelete(destination.list().filterNot { it.name == info.name }, Retention.KEEP - 1).forEach { destination.delete(it) }
        return info
    }
}
