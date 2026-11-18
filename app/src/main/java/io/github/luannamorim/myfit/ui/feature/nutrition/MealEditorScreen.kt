package io.github.luannamorim.myfit.ui.feature.nutrition

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.core.format.Dates
import io.github.luannamorim.myfit.ui.common.rememberPhotoSource
import io.github.luannamorim.myfit.ui.components.ConfirmDialog
import io.github.luannamorim.myfit.ui.components.ContentMaxWidth
import io.github.luannamorim.myfit.ui.components.MyFitTextField
import io.github.luannamorim.myfit.ui.components.MyFitTopBar
import io.github.luannamorim.myfit.ui.components.PrimaryButton
import io.github.luannamorim.myfit.ui.components.SecondaryButton
import io.github.luannamorim.myfit.ui.components.TimeDialog
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import io.github.luannamorim.myfit.ui.theme.Spacing
import java.time.LocalTime

data class MealEditorActions(
    val onBack: () -> Unit = {},
    val onDescription: (String) -> Unit = {},
    val onKcal: (String) -> Unit = {},
    val onMinute: (Int) -> Unit = {},
    /** Hands the prompt to an AI app (the app itself has no internet access). */
    val onAskAi: (String) -> Unit = {},
    /** Takes or picks a photo, then hands it with this prompt to an AI app. */
    val onPhotoToAi: (String) -> Unit = {},
    val onSave: () -> Unit = {},
    val onDelete: () -> Unit = {},
)

/** Android share sheet with the prompt as plain text, so any installed AI app can answer it. */
fun askAiIntent(prompt: String, chooserTitle: String): Intent =
    Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, prompt), chooserTitle)

/**
 * Share sheet with the meal photo and the prompt. The read grant travels in the ClipData, which the
 * chooser passes on to the AI app the user picks.
 */
fun askAiPhotoIntent(photo: Uri, prompt: String, chooserTitle: String): Intent {
    val send = Intent(Intent.ACTION_SEND)
        .setType("image/jpeg")
        .putExtra(Intent.EXTRA_STREAM, photo)
        .putExtra(Intent.EXTRA_TEXT, prompt)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    send.clipData = ClipData.newRawUri("", photo)
    return Intent.createChooser(send, chooserTitle)
}

@Composable
fun MealEditorScreen(onClose: () -> Unit, modifier: Modifier = Modifier, viewModel: MealEditorViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val chooserTitle = stringResource(R.string.meal_ask_ai)
    val photoChooserTitle = stringResource(R.string.meal_photo_ai)
    // Survives Android killing the app while the camera is open.
    var photoPrompt by rememberSaveable { mutableStateOf("") }
    val photoSource = rememberPhotoSource(viewModel::preparePhoto)
    LaunchedEffect(state.done) { if (state.done) onClose() }
    LaunchedEffect(state.photoToShare) {
        val photo = state.photoToShare ?: return@LaunchedEffect
        context.startActivity(askAiPhotoIntent(photo, photoPrompt, photoChooserTitle))
        viewModel.onPhotoHandled()
    }
    LaunchedEffect(state.photoFailed) {
        if (!state.photoFailed) return@LaunchedEffect
        Toast.makeText(context, R.string.meal_photo_failed, Toast.LENGTH_SHORT).show()
        viewModel.onPhotoHandled()
    }
    MealEditorContent(
        state = state,
        actions = MealEditorActions(
            onBack = onClose,
            onDescription = viewModel::setDescription,
            onKcal = viewModel::setKcal,
            onMinute = viewModel::setMinute,
            onAskAi = { prompt -> context.startActivity(askAiIntent(prompt, chooserTitle)) },
            onPhotoToAi = { prompt ->
                photoPrompt = prompt
                photoSource.choose()
            },
            onSave = viewModel::save,
            onDelete = viewModel::delete,
        ),
        modifier = modifier,
    )
}

@Composable
fun MealEditorContent(state: MealEditorState, actions: MealEditorActions, modifier: Modifier = Modifier) {
    val c = MyFitTheme.colors
    var pickingTime by rememberSaveable { mutableStateOf(false) }
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    val prompt = stringResource(R.string.meal_ai_prompt, state.description.trim())
    val photoQuestion = stringResource(R.string.meal_ai_photo_prompt)
    val photoDetails = stringResource(R.string.meal_ai_photo_details, state.description.trim())
    val photoPrompt = if (state.description.isBlank()) photoQuestion else "$photoQuestion $photoDetails"
    Scaffold(
        modifier = modifier,
        containerColor = c.background,
        topBar = {
            MyFitTopBar(
                title = stringResource(if (state.isNew) R.string.meal_new else R.string.meal_edit),
                onBack = actions.onBack,
                actions = {
                    if (!state.isNew) {
                        IconButton(onClick = { confirmingDelete = true }) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (state.loading) return@Scaffold
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = ContentMaxWidth)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.screen)
                    .padding(top = Spacing.s, bottom = Spacing.xxl),
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                MyFitTextField(
                    value = state.description,
                    onValueChange = actions.onDescription,
                    label = stringResource(R.string.meal_description),
                    placeholder = stringResource(R.string.meal_description_hint),
                    singleLine = false,
                    minLines = 3,
                )
                SecondaryButton(
                    stringResource(R.string.meal_ask_ai),
                    onClick = { actions.onAskAi(prompt) },
                    enabled = state.canAsk,
                    icon = Icons.Outlined.AutoAwesome,
                    modifier = Modifier.fillMaxWidth(),
                )
                SecondaryButton(
                    stringResource(R.string.meal_photo_ai),
                    onClick = { actions.onPhotoToAi(photoPrompt) },
                    enabled = state.canSendPhoto,
                    icon = Icons.Outlined.PhotoCamera,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(stringResource(R.string.meal_ask_ai_hint), style = MyFitTheme.type.caption, color = c.textTertiary)
                MyFitTextField(
                    value = state.kcalText,
                    onValueChange = actions.onKcal,
                    label = stringResource(R.string.meal_kcal),
                    keyboardType = KeyboardType.Number,
                    suffix = "kcal",
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button, onClickLabel = stringResource(R.string.action_edit)) { pickingTime = true }
                        .padding(vertical = Spacing.s),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.meal_time), style = MyFitTheme.type.body, color = c.textSecondary, modifier = Modifier.weight(1f))
                    Text(Dates.time(LocalTime.of(state.minuteOfDay / 60, state.minuteOfDay % 60)), style = MyFitTheme.type.bodyStrong, color = c.textPrimary)
                }
                PrimaryButton(stringResource(R.string.action_save), onClick = actions.onSave, enabled = state.canSave, modifier = Modifier.fillMaxWidth())
            }
        }
    }
    if (pickingTime) {
        TimeDialog(
            minuteOfDay = state.minuteOfDay,
            is24Hour = android.text.format.DateFormat.is24HourFormat(LocalContext.current),
            onConfirm = { actions.onMinute(it); pickingTime = false },
            onDismiss = { pickingTime = false },
        )
    }
    if (confirmingDelete) {
        ConfirmDialog(
            title = stringResource(R.string.meal_delete_title),
            message = stringResource(R.string.meal_delete_message),
            confirmText = stringResource(R.string.action_delete),
            onConfirm = { confirmingDelete = false; actions.onDelete() },
            onDismiss = { confirmingDelete = false },
            destructive = true,
        )
    }
}
