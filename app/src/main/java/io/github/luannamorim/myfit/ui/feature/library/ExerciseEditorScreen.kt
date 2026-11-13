package io.github.luannamorim.myfit.ui.feature.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.domain.model.ExerciseMeasure
import io.github.luannamorim.myfit.domain.model.MovementType
import io.github.luannamorim.myfit.domain.model.MuscleGroup
import io.github.luannamorim.myfit.ui.common.ArtSource
import io.github.luannamorim.myfit.ui.common.ExerciseArt
import io.github.luannamorim.myfit.ui.common.ExerciseDrawing
import io.github.luannamorim.myfit.ui.common.MuscleMap
import io.github.luannamorim.myfit.ui.common.labelRes
import io.github.luannamorim.myfit.ui.common.localImageModel
import io.github.luannamorim.myfit.ui.common.openExerciseVideo
import io.github.luannamorim.myfit.ui.components.ConfirmDialog
import io.github.luannamorim.myfit.ui.components.ContentMaxWidth
import io.github.luannamorim.myfit.ui.components.MyFitChip
import io.github.luannamorim.myfit.ui.components.MyFitTextField
import io.github.luannamorim.myfit.ui.components.MyFitTopBar
import io.github.luannamorim.myfit.ui.components.SecondaryButton
import io.github.luannamorim.myfit.ui.components.SectionHeader
import io.github.luannamorim.myfit.ui.components.TextAction
import io.github.luannamorim.myfit.ui.theme.MyFitShapes
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import io.github.luannamorim.myfit.ui.theme.Spacing

@Composable
fun ExerciseEditorScreen(onClose: () -> Unit, viewModel: ExerciseEditorViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onClose() }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.pickImage(uri)
    }
    ExerciseEditorContent(
        state = state,
        onName = viewModel::setName,
        onGroup = viewModel::setGroup,
        onMovement = viewModel::setMovement,
        onNotes = viewModel::setNotes,
        onPickImage = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onRemoveImage = viewModel::removeImage,
        onSave = { viewModel.save() },
        onDelete = viewModel::delete,
        onBack = onClose,
        onMeasure = viewModel::setMeasure,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseEditorContent(
    state: ExerciseEditorUiState,
    onName: (String) -> Unit,
    onGroup: (MuscleGroup) -> Unit,
    onMovement: (MovementType) -> Unit,
    onNotes: (String) -> Unit,
    onPickImage: () -> Unit,
    onRemoveImage: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    onMeasure: (ExerciseMeasure) -> Unit = {},
) {
    val c = MyFitTheme.colors
    var confirmDelete by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = c.background,
        topBar = {
            MyFitTopBar(
                title = stringResource(if (state.isNew) R.string.exercise_editor_new else R.string.exercise_editor_edit),
                onBack = onBack,
                actions = { TextAction(stringResource(R.string.action_save), onClick = onSave, enabled = !state.isLoading && !state.isImporting) },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .widthIn(max = ContentMaxWidth)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.screen, vertical = Spacing.s),
                verticalArrangement = Arrangement.spacedBy(Spacing.l),
            ) {
                MyFitTextField(
                    value = state.name,
                    onValueChange = onName,
                    label = stringResource(R.string.exercise_name),
                    isError = state.nameError,
                    supportingText = when {
                        state.nameError -> stringResource(R.string.exercise_name_required)
                        state.nameLocked -> stringResource(R.string.exercise_name_locked)
                        else -> null
                    },
                    capitalization = KeyboardCapitalization.Sentences,
                    enabled = !state.nameLocked,
                )
                SectionHeader(stringResource(R.string.exercise_group))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    MuscleGroup.entries.forEach { group ->
                        MyFitChip(stringResource(group.labelRes), selected = state.muscleGroup == group, onClick = { onGroup(group) })
                    }
                }
                MuscleMap(state.muscleGroup, Modifier.fillMaxWidth().height(180.dp).padding(vertical = Spacing.xs))
                SectionHeader(stringResource(R.string.exercise_movement))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    MovementType.entries.forEach { type ->
                        MyFitChip(stringResource(type.labelRes), selected = state.movementType == type, onClick = { onMovement(type) })
                    }
                }
                SectionHeader(stringResource(R.string.exercise_measure))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                    MyFitChip(stringResource(R.string.exercise_measure_reps), selected = state.measure == ExerciseMeasure.REPS, onClick = { onMeasure(ExerciseMeasure.REPS) })
                    MyFitChip(stringResource(R.string.exercise_measure_time), selected = state.measure == ExerciseMeasure.TIME, onClick = { onMeasure(ExerciseMeasure.TIME) })
                }
                SectionHeader(stringResource(R.string.exercise_image))
                ImageBox(state, onPickImage, onRemoveImage)
                if (state.name.isNotBlank()) {
                    val context = LocalContext.current
                    SecondaryButton(
                        stringResource(R.string.exercise_video),
                        onClick = { openExerciseVideo(context, state.name.trim()) },
                        icon = Icons.Rounded.PlayCircle,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                MyFitTextField(
                    value = state.notes,
                    onValueChange = onNotes,
                    label = stringResource(R.string.exercise_notes),
                    singleLine = false,
                    minLines = 3,
                )
                if (!state.isNew) {
                    SecondaryButton(
                        stringResource(R.string.action_delete),
                        onClick = { confirmDelete = true },
                        icon = Icons.Rounded.Delete,
                        contentColor = c.danger,
                        modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.xl),
                    )
                }
            }
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.exercise_delete_title),
            message = stringResource(R.string.exercise_delete_message),
            confirmText = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = { confirmDelete = false; onDelete() },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun ImageBox(state: ExerciseEditorUiState, onPick: () -> Unit, onRemove: () -> Unit) {
    val c = MyFitTheme.colors
    val model = localImageModel(state.imagePath)
    val art = ExerciseArt.artFor(state.seedKey)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(16f / 10f).clip(MyFitShapes.medium).background(c.surfaceRaised),
            contentAlignment = Alignment.Center,
        ) {
            when {
                state.isImporting -> CircularProgressIndicator(color = c.accent)
                model != null -> AsyncImage(model, contentDescription = state.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                art != null -> ExerciseDrawing(art, c.textPrimary, Modifier.fillMaxSize().padding(Spacing.l), contentDescription = state.name)
                else -> Icon(Icons.Rounded.Image, contentDescription = null, tint = c.textTertiary)
            }
        }
        if (model == null && art != null && !state.isImporting) {
            val source = when (ExerciseArt.source(art)) {
                ArtSource.EVERKINETIC -> "Everkinetic"
                ArtSource.WORKOUT_GUIDE -> "Bryl Lim / Workout Guide"
            }
            Text(
                stringResource(R.string.exercise_image_credit, source) + "\n" + stringResource(R.string.exercise_image_drawing_hint),
                style = MyFitTheme.type.caption,
                color = c.textTertiary,
            )
        }
        if (state.imageError) {
            Text(stringResource(R.string.exercise_image_error), style = MyFitTheme.type.caption, color = c.danger)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            TextAction(
                stringResource(if (model == null) R.string.exercise_image_pick else R.string.exercise_image_replace),
                onClick = onPick,
            )
            if (model != null) TextAction(stringResource(R.string.exercise_image_remove), onClick = onRemove, color = c.textSecondary)
        }
    }
}
