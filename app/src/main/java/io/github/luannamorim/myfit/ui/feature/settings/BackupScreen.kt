package io.github.luannamorim.myfit.ui.feature.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.core.format.Dates
import io.github.luannamorim.myfit.data.backup.KeyState
import io.github.luannamorim.myfit.data.prefs.BackupFailure
import io.github.luannamorim.myfit.domain.backup.BackupFileInfo
import io.github.luannamorim.myfit.domain.backup.Retention
import io.github.luannamorim.myfit.ui.components.ConfirmDialog
import io.github.luannamorim.myfit.ui.components.ContentMaxWidth
import io.github.luannamorim.myfit.ui.components.MyFitCard
import io.github.luannamorim.myfit.ui.components.MyFitTopBar
import io.github.luannamorim.myfit.ui.components.PrimaryButton
import io.github.luannamorim.myfit.ui.components.SecondaryButton
import io.github.luannamorim.myfit.ui.components.SectionHeader
import io.github.luannamorim.myfit.ui.components.TextAction
import io.github.luannamorim.myfit.ui.theme.MyFitShapes
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import io.github.luannamorim.myfit.ui.theme.Spacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun BackupScreen(onBack: () -> Unit, viewModel: BackupViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) viewModel.chooseFolder(uri)
    }
    val exportPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri: Uri? ->
        if (uri != null) viewModel.exportTo(uri)
    }
    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) viewModel.prepareRestore(uri)
    }
    BackupContent(
        state = state,
        onBack = onBack,
        onSetPassword = viewModel::setPassword,
        onUnlock = viewModel::unlock,
        onChooseFolder = { folderPicker.launch(null) },
        onAutoBackup = viewModel::setAutoBackup,
        onRunNow = viewModel::runNow,
        onExport = { exportPicker.launch(Retention.fileName(System.currentTimeMillis(), ZoneId.systemDefault())) },
        onImport = { importPicker.launch(arrayOf("*/*")) },
        onRestoreFile = viewModel::prepareRestore,
        onConfirmRestore = viewModel::confirmRestore,
        onCancelRestore = viewModel::cancelRestore,
        onMessageShown = viewModel::consumeMessage,
    )
}

@Composable
fun BackupContent(
    state: BackupUiState,
    onBack: () -> Unit,
    onSetPassword: (CharArray) -> Unit,
    onUnlock: (CharArray) -> Unit,
    onChooseFolder: () -> Unit,
    onAutoBackup: (Boolean) -> Unit,
    onRunNow: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onRestoreFile: (BackupFileInfo) -> Unit,
    onConfirmRestore: (CharArray?) -> Unit,
    onCancelRestore: () -> Unit,
    onMessageShown: () -> Unit,
) {
    val c = MyFitTheme.colors
    val snackbar = remember { SnackbarHostState() }
    var passwordDialog by remember { mutableStateOf(false) }
    var unlockDialog by remember { mutableStateOf(false) }
    val messageText = state.message?.let { stringResource(it.textRes()) }
    LaunchedEffect(state.message) {
        if (messageText != null) {
            snackbar.showSnackbar(messageText)
            onMessageShown()
        }
    }

    Scaffold(
        containerColor = c.background,
        topBar = { MyFitTopBar(stringResource(R.string.backup_title), onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screen),
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = c.accent, trackColor = c.surfaceRaised)

                SectionHeader(stringResource(R.string.backup_password_section))
                MyFitCard(Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(
                            when (state.keyState) {
                                KeyState.NONE -> R.string.backup_password_none
                                KeyState.READY -> R.string.backup_password_set
                                KeyState.LOCKED -> R.string.backup_password_locked
                            },
                        ),
                        style = MyFitTheme.type.body,
                        color = if (state.keyState == KeyState.LOCKED) c.warning else c.textSecondary,
                    )
                    Spacer(Modifier.height(Spacing.m))
                    SecondaryButton(
                        stringResource(
                            when (state.keyState) {
                                KeyState.NONE -> R.string.backup_password_define
                                KeyState.READY -> R.string.backup_password_change
                                KeyState.LOCKED -> R.string.backup_password_enter
                            },
                        ),
                        onClick = { if (state.keyState == KeyState.LOCKED) unlockDialog = true else passwordDialog = true },
                        icon = Icons.Rounded.Key,
                    )
                }

                SectionHeader(stringResource(R.string.backup_folder_section))
                MyFitCard(Modifier.fillMaxWidth()) {
                    Text(
                        state.backup.folderName ?: stringResource(R.string.backup_folder_none),
                        style = MyFitTheme.type.bodyStrong,
                        color = if (state.backup.folderUri == null) c.textSecondary else c.textPrimary,
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(stringResource(R.string.backup_folder_hint), style = MyFitTheme.type.caption, color = c.textSecondary)
                    Spacer(Modifier.height(Spacing.m))
                    SecondaryButton(
                        stringResource(if (state.backup.folderUri == null) R.string.backup_folder_choose else R.string.backup_folder_change),
                        onClick = onChooseFolder,
                        icon = Icons.Rounded.Folder,
                    )
                }

                SectionHeader(stringResource(R.string.backup_status_section))
                MyFitCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.backup_last), style = MyFitTheme.type.body, color = c.textSecondary, modifier = Modifier.weight(1f))
                        Text(lastBackupText(state.backup.lastSuccessMs), style = MyFitTheme.type.bodyStrong, color = c.textPrimary)
                    }
                    val error = state.backup.lastError
                    if (error != null || state.folderError) {
                        Spacer(Modifier.height(Spacing.s))
                        Text(stringResource(failureText(error)), style = MyFitTheme.type.caption, color = c.danger)
                    }
                    Spacer(Modifier.height(Spacing.m))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.backup_auto), style = MyFitTheme.type.bodyStrong, color = c.textPrimary)
                            Text(stringResource(R.string.backup_auto_hint), style = MyFitTheme.type.caption, color = c.textSecondary)
                        }
                        Switch(
                            checked = state.autoBackup,
                            onCheckedChange = onAutoBackup,
                            colors = SwitchDefaults.colors(checkedTrackColor = c.accent, checkedThumbColor = c.onAccent),
                        )
                    }
                    Spacer(Modifier.height(Spacing.m))
                    PrimaryButton(
                        stringResource(if (state.running) R.string.backup_running else R.string.backup_now),
                        onClick = onRunNow,
                        enabled = !state.running && state.backup.folderUri != null && state.keyState == KeyState.READY,
                        icon = Icons.Rounded.CloudUpload,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (state.backup.folderUri != null) {
                    SectionHeader(stringResource(R.string.backup_files_section))
                    if (state.files.isEmpty()) {
                        Text(stringResource(R.string.backup_files_empty), style = MyFitTheme.type.body, color = c.textSecondary)
                    }
                    state.files.forEach { file ->
                        MyFitCard(onClick = { onRestoreFile(file) }, modifier = Modifier.fillMaxWidth(), contentPadding = Spacing.m) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(dateTimeText(file.createdAtMs), style = MyFitTheme.type.bodyStrong, color = c.textPrimary, modifier = Modifier.weight(1f))
                                Text(sizeText(file.sizeBytes), style = MyFitTheme.type.numeric, color = c.textSecondary)
                            }
                        }
                    }
                }

                SectionHeader(stringResource(R.string.backup_file_section))
                SecondaryButton(stringResource(R.string.backup_export), onClick = onExport, modifier = Modifier.fillMaxWidth(), enabled = state.keyState == KeyState.READY)
                SecondaryButton(stringResource(R.string.backup_import), onClick = onImport, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(Spacing.xxl))
            }
        }
    }

    if (passwordDialog) {
        PasswordDialog(confirm = true, onDismiss = { passwordDialog = false }, onSubmit = { onSetPassword(it); passwordDialog = false })
    }
    if (unlockDialog) {
        PasswordDialog(confirm = false, onDismiss = { unlockDialog = false }, onSubmit = { onUnlock(it); unlockDialog = false })
    }
    state.pending?.let { pending ->
        if (pending.needsPassword) {
            PasswordDialog(
                confirm = false,
                title = stringResource(R.string.backup_restore_title),
                message = stringResource(R.string.backup_restore_message, dateTimeText(pending.createdAtMs)) + "\n\n" + stringResource(R.string.backup_restore_password),
                error = if (pending.passwordError) stringResource(R.string.backup_password_wrong) else null,
                submitText = stringResource(R.string.backup_restore_confirm),
                onDismiss = onCancelRestore,
                onSubmit = { onConfirmRestore(it) },
            )
        } else {
            ConfirmDialog(
                title = stringResource(R.string.backup_restore_title),
                message = stringResource(R.string.backup_restore_message, dateTimeText(pending.createdAtMs)),
                confirmText = stringResource(R.string.backup_restore_confirm),
                destructive = true,
                onConfirm = { onConfirmRestore(null) },
                onDismiss = onCancelRestore,
            )
        }
    }
}

@Composable
private fun PasswordDialog(
    confirm: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (CharArray) -> Unit,
    title: String = stringResource(R.string.backup_password_dialog_title),
    message: String = stringResource(R.string.backup_password_dialog_message),
    error: String? = null,
    submitText: String = stringResource(R.string.action_save),
) {
    val c = MyFitTheme.colors
    var password by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    val tooShort = stringResource(R.string.backup_password_too_short)
    val mismatch = stringResource(R.string.backup_password_mismatch)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = c.textPrimary, unfocusedBorderColor = c.outline, cursorColor = c.accent,
        focusedLabelColor = c.textPrimary, unfocusedLabelColor = c.textSecondary,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surfaceRaised,
        shape = MyFitShapes.large,
        title = { Text(title, style = MyFitTheme.type.title, color = c.textPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                Text(message, style = MyFitTheme.type.body, color = c.textSecondary)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; localError = null },
                    label = { Text(stringResource(R.string.backup_password_field)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = fieldColors,
                )
                if (confirm) {
                    OutlinedTextField(
                        value = repeat,
                        onValueChange = { repeat = it; localError = null },
                        label = { Text(stringResource(R.string.backup_password_confirm)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = fieldColors,
                    )
                }
                (localError ?: error)?.let { Text(it, style = MyFitTheme.type.caption, color = c.danger) }
            }
        },
        confirmButton = {
            TextAction(submitText, onClick = {
                when {
                    confirm && password.length < MIN_PASSWORD -> localError = tooShort
                    confirm && password != repeat -> localError = mismatch
                    password.isEmpty() -> Unit
                    else -> onSubmit(password.toCharArray())
                }
            })
        },
        dismissButton = { TextAction(stringResource(R.string.action_cancel), onClick = onDismiss, color = c.textSecondary) },
    )
}

private const val MIN_PASSWORD = 8

@Composable
private fun dateTimeText(ms: Long): String {
    val dt = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
    val time = Dates.time(dt.toLocalTime())
    return if (dt.toLocalDate() == LocalDate.now()) stringResource(R.string.backup_today_at, time)
    else stringResource(R.string.backup_date_at, Dates.medium(dt.toLocalDate()), time)
}

@Composable
private fun lastBackupText(ms: Long?): String = if (ms == null) stringResource(R.string.backup_never) else dateTimeText(ms)

private fun sizeText(bytes: Long): String = when {
    bytes >= 1_048_576 -> "${io.github.luannamorim.myfit.core.format.formatOneDecimal(bytes / 1_048_576.0)} MB"
    else -> "${(bytes / 1024).coerceAtLeast(1)} KB"
}

private fun failureText(failure: BackupFailure?): Int = when (failure) {
    BackupFailure.NO_PASSWORD -> R.string.backup_error_password
    BackupFailure.NO_SPACE -> R.string.backup_error_space
    BackupFailure.UNKNOWN -> R.string.backup_error_unknown
    BackupFailure.NO_FOLDER, BackupFailure.FOLDER_UNAVAILABLE, null -> R.string.backup_error_folder
}

private fun BackupMessage.textRes(): Int = when (this) {
    BackupMessage.SAVED -> R.string.backup_msg_saved
    BackupMessage.EXPORTED -> R.string.backup_msg_exported
    BackupMessage.RESTORED -> R.string.backup_msg_restored
    BackupMessage.WRONG_PASSWORD -> R.string.backup_msg_wrong_password
    BackupMessage.CORRUPTED -> R.string.backup_msg_corrupted
    BackupMessage.UNSUPPORTED -> R.string.backup_msg_unsupported
    BackupMessage.NO_SPACE -> R.string.backup_msg_space
    BackupMessage.SESSION_ACTIVE -> R.string.backup_msg_session_active
    BackupMessage.NO_PASSWORD -> R.string.backup_msg_no_password
    BackupMessage.FOLDER_ERROR -> R.string.backup_msg_folder_error
    BackupMessage.ERROR -> R.string.backup_msg_error
}
