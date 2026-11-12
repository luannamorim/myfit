package io.github.luannamorim.myfit.ui.feature.settings

import androidx.core.net.toUri
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.luannamorim.myfit.data.backup.BackupDestinationFactory
import io.github.luannamorim.myfit.data.backup.BackupKeyStore
import io.github.luannamorim.myfit.data.backup.BackupManager
import io.github.luannamorim.myfit.data.backup.KeyState
import io.github.luannamorim.myfit.data.backup.RestoreResult
import io.github.luannamorim.myfit.data.prefs.BackupState
import io.github.luannamorim.myfit.data.prefs.BackupStateStore
import io.github.luannamorim.myfit.data.prefs.SettingsRepository
import io.github.luannamorim.myfit.domain.backup.BackupError
import io.github.luannamorim.myfit.domain.backup.BackupFileInfo
import io.github.luannamorim.myfit.platform.backup.BackupScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.InputStream
import javax.inject.Inject

enum class BackupMessage { SAVED, EXPORTED, RESTORED, WRONG_PASSWORD, CORRUPTED, UNSUPPORTED, NO_SPACE, SESSION_ACTIVE, NO_PASSWORD, FOLDER_ERROR, ERROR }

/** A restore waiting for confirmation (and maybe a password). */
data class PendingRestore(val uri: String, val sizeBytes: Long?, val createdAtMs: Long, val needsPassword: Boolean, val passwordError: Boolean = false)

data class BackupUiState(
    val loading: Boolean = true,
    val keyState: KeyState = KeyState.NONE,
    val backup: BackupState = BackupState(),
    val autoBackup: Boolean = true,
    val files: List<BackupFileInfo> = emptyList(),
    val folderError: Boolean = false,
    val running: Boolean = false,
    val busy: Boolean = false,
    val pending: PendingRestore? = null,
    val message: BackupMessage? = null,
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val manager: BackupManager,
    private val keyStore: BackupKeyStore,
    private val store: BackupStateStore,
    private val settings: SettingsRepository,
    private val destinations: BackupDestinationFactory,
    private val scheduler: BackupScheduler,
) : ViewModel() {
    private val local = MutableStateFlow(BackupUiState())
    private val running = scheduler.observeManualRun().map { it != null && !it.state.isFinished && it.state != WorkInfo.State.CANCELLED }

    val state: StateFlow<BackupUiState> = combine(local, store.state, settings.settings, keyStore.hasPassword, running) { l, b, s, _, r ->
        l.copy(backup = b, autoBackup = s.autoBackup, running = r)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BackupUiState())

    init {
        refresh()
        // Refresh the file list when a manual/automatic run finishes.
        viewModelScope.launch {
            store.state.map { it.lastSuccessMs to it.folderUri }.distinctUntilChanged().collect { refreshFiles() }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            local.update { it.copy(keyState = keyStore.state(), loading = false) }
            refreshFiles()
        }
    }

    private suspend fun refreshFiles() {
        val folder = store.current().folderUri ?: return local.update { it.copy(files = emptyList(), folderError = false) }
        try {
            val files = destinations.create(folder).list().sortedByDescending { it.createdAtMs }
            local.update { it.copy(files = files, folderError = false) }
        } catch (e: BackupError.FolderUnavailable) {
            local.update { it.copy(files = emptyList(), folderError = true) }
        }
    }

    fun setPassword(password: CharArray) = busy {
        keyStore.setPassword(password)
        local.update { it.copy(keyState = KeyState.READY) }
        store.markChanged() // new key: the next backup should use it
    }

    /** Returns via state: keyState READY on success, message WRONG_PASSWORD otherwise. */
    fun unlock(password: CharArray) = busy {
        if (keyStore.unlock(password)) local.update { it.copy(keyState = KeyState.READY) } else message(BackupMessage.WRONG_PASSWORD)
    }

    fun chooseFolder(uri: Uri) = busy {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching { context.contentResolver.takePersistableUriPermission(uri, flags) }
        val name = runCatching { DocumentFile.fromTreeUri(context, uri)?.name }.getOrNull()
        store.setFolder(uri.toString(), name)
        refreshFiles()
        if (keyStore.key() != null) scheduler.runNow()
    }

    fun setAutoBackup(enabled: Boolean) {
        viewModelScope.launch { settings.update { it.copy(autoBackup = enabled) } }
    }

    fun runNow() {
        if (state.value.keyState != KeyState.READY) return message(BackupMessage.NO_PASSWORD)
        scheduler.runNow()
    }

    fun exportTo(uri: Uri) = busy {
        try {
            context.contentResolver.openOutputStream(uri, "w")?.use { manager.create(it) } ?: throw BackupError.FolderUnavailable("cannot open")
            message(BackupMessage.EXPORTED)
        } catch (e: BackupError.NoPassword) {
            message(BackupMessage.NO_PASSWORD)
        } catch (e: Exception) {
            message(BackupMessage.ERROR)
        }
    }

    /** Step 1: read the header to show the backup date before replacing anything. */
    fun prepareRestore(uri: Uri, sizeBytes: Long? = null) = busy {
        try {
            val header = manager.inspect(open(uri))
            val stored = keyStore.key()
            val needsPassword = stored == null || !stored.salt.contentEquals(header.salt) || stored.iterations != header.iterations
            local.update { it.copy(pending = PendingRestore(uri.toString(), sizeBytes ?: sizeOf(uri), header.createdAtMs, needsPassword)) }
        } catch (e: BackupError.UnsupportedVersion) {
            message(BackupMessage.UNSUPPORTED)
        } catch (e: Exception) {
            message(BackupMessage.CORRUPTED)
        }
    }

    fun prepareRestore(file: BackupFileInfo) = prepareRestore(file.id.toUri(), file.sizeBytes)

    fun cancelRestore() = local.update { it.copy(pending = null) }

    /** Step 2: restore (password only when the file was made with another setup). */
    fun confirmRestore(password: CharArray?) = busy {
        val pending = state.value.pending ?: return@busy
        val result = try {
            manager.restore(open(pending.uri.toUri()), password, pending.sizeBytes)
        } catch (e: Exception) {
            RestoreResult.Corrupted(e.message ?: "")
        }
        when (result) {
            is RestoreResult.Success -> {
                local.update { it.copy(pending = null, keyState = keyStore.state()) }
                message(BackupMessage.RESTORED)
            }
            RestoreResult.PasswordRequired -> local.update { it.copy(pending = pending.copy(needsPassword = true)) }
            RestoreResult.WrongPassword -> local.update { it.copy(pending = pending.copy(needsPassword = true, passwordError = true)) }
            is RestoreResult.Corrupted -> fail(BackupMessage.CORRUPTED)
            RestoreResult.UnsupportedVersion -> fail(BackupMessage.UNSUPPORTED)
            RestoreResult.InsufficientSpace -> fail(BackupMessage.NO_SPACE)
            RestoreResult.SessionActive -> fail(BackupMessage.SESSION_ACTIVE)
        }
    }

    fun consumeMessage() = local.update { it.copy(message = null) }

    private fun fail(m: BackupMessage) {
        local.update { it.copy(pending = null) }
        message(m)
    }

    private fun message(m: BackupMessage) = local.update { it.copy(message = m) }

    private fun open(uri: Uri): InputStream = context.contentResolver.openInputStream(uri) ?: throw BackupError.FolderUnavailable("cannot open")

    private fun sizeOf(uri: Uri): Long? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getLong(0) else null
        }
    }.getOrNull()

    private fun busy(block: suspend () -> Unit) {
        viewModelScope.launch {
            local.update { it.copy(busy = true) }
            try {
                block()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                // Never crash the screen on provider/storage errors: report and keep going.
                android.util.Log.w("BackupViewModel", "backup action failed", e)
                message(if (e is BackupError.FolderUnavailable) BackupMessage.FOLDER_ERROR else BackupMessage.ERROR)
            } finally {
                local.update { it.copy(busy = false) }
            }
        }
    }
}
