package io.github.luannamorim.myfit.ui.feature.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
        onMeasure = viewModel::setMeasure,
        onPickImage = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onRemoveImage = viewModel::removeImage,
        onToggleFavorite = viewModel::toggleFavorite,
        onSave = { viewModel.save() },
        onDelete = viewModel::delete,
        onBack = onClose,
    )
}

/**
 * A library exercise is read-only (name, its muscle group with the body map, movement type, drawing, video
 * link) except for how sets count and the favorite star, both saved right away. A custom one is fully
 * editable and can be deleted.
 */
@Composable
fun ExerciseEditorContent(
    state: ExerciseEditorUiState,
    onName: (String) -> Unit = {},
    onGroup: (MuscleGroup) -> Unit = {},
    onMovement: (MovementType) -> Unit = {},
    onMeasure: (ExerciseMeasure) -> Unit = {},
    onPickImage: () -> Unit = {},
    onRemoveImage: () -> Unit = {},
    onToggleFavorite: () -> Unit = {},
    onSave: () -> Unit = {},
    onDelete: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    val c = MyFitTheme.colors
    var confirmDelete by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = c.background,
        topBar = {
            MyFitTopBar(
                title = stringResource(
                    when {
                        state.isLibrary -> R.string.exercise_detail_title
                        state.isNew -> R.string.exercise_editor_new
                        else -> R.string.exercise_editor_edit
                    },
                ),
                onBack = onBack,
                actions = {
                    if (!state.isNew && !state.isLoading) FavoriteToggle(state.favorite, onToggleFavorite)
                    if (!state.isLibrary) {
                        TextAction(stringResource(R.string.action_save), onClick = onSave, enabled = !state.isLoading && !state.isImporting)
                    }
                },
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
                if (state.isLibrary) {
                    LibraryDetail(state, onMeasure)
                } else {
                    CustomFields(state, onName, onGroup, onMovement, onMeasure, onPickImage, onRemoveImage)
                }
                if (state.name.isNotBlank()) {
                    val context = LocalContext.current
                    SecondaryButton(
                        stringResource(R.string.exercise_video),
                        onClick = { openExerciseVideo(context, state.name.trim()) },
                        icon = Icons.Rounded.PlayCircle,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (!state.isNew && !state.isLibrary) {
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
private fun FavoriteToggle(favorite: Boolean, onToggle: () -> Unit) {
    val c = MyFitTheme.colors
    IconToggleButton(checked = favorite, onCheckedChange = { onToggle() }) {
        Icon(
            if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
            contentDescription = stringResource(R.string.exercise_favorite),
            tint = if (favorite) c.accent else c.textSecondary,
        )
    }
}

/**
 * Same sections as the custom editor; group and movement type show only this exercise's value instead of
 * every option. How sets count stays a choice.
 */
@Composable
private fun ColumnScope.LibraryDetail(state: ExerciseEditorUiState, onMeasure: (ExerciseMeasure) -> Unit) {
    Text(state.name, style = MyFitTheme.type.title, color = MyFitTheme.colors.textPrimary)
    SectionHeader(stringResource(R.string.exercise_group))
    ValueChip(stringResource(state.muscleGroup.labelRes))
    MuscleMap(state.muscleGroup, Modifier.fillMaxWidth().height(180.dp).padding(vertical = Spacing.xs))
    SectionHeader(stringResource(R.string.exercise_movement))
    ValueChip(stringResource(state.movementType.labelRes))
    MeasureChoice(state.measure, onMeasure)
    SectionHeader(stringResource(R.string.exercise_image))
    ExerciseImage(state)
}

@Composable
private fun MeasureChoice(measure: ExerciseMeasure, onMeasure: (ExerciseMeasure) -> Unit) {
    SectionHeader(stringResource(R.string.exercise_measure))
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
        MyFitChip(stringResource(R.string.exercise_measure_reps), selected = measure == ExerciseMeasure.REPS, onClick = { onMeasure(ExerciseMeasure.REPS) })
        MyFitChip(stringResource(R.string.exercise_measure_time), selected = measure == ExerciseMeasure.TIME, onClick = { onMeasure(ExerciseMeasure.TIME) })
    }
}

/** Looks like a selected [MyFitChip] but isn't clickable: a value, not a choice. */
@Composable
private fun ValueChip(text: String) {
    val c = MyFitTheme.colors
    Surface(shape = MyFitShapes.pill, color = c.textPrimary, contentColor = c.background, border = BorderStroke(1.dp, c.textPrimary)) {
        Text(text, style = MyFitTheme.type.label, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnScope.CustomFields(
    state: ExerciseEditorUiState,
    onName: (String) -> Unit,
    onGroup: (MuscleGroup) -> Unit,
    onMovement: (MovementType) -> Unit,
    onMeasure: (ExerciseMeasure) -> Unit,
    onPickImage: () -> Unit,
    onRemoveImage: () -> Unit,
) {
    MyFitTextField(
        value = state.name,
        onValueChange = onName,
        label = stringResource(R.string.exercise_name),
        isError = state.nameError,
        supportingText = if (state.nameError) stringResource(R.string.exercise_name_required) else null,
        capitalization = KeyboardCapitalization.Sentences,
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
    MeasureChoice(state.measure, onMeasure)
    SectionHeader(stringResource(R.string.exercise_image))
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        ExerciseImage(state)
        val c = MyFitTheme.colors
        if (state.imageError) {
            Text(stringResource(R.string.exercise_image_error), style = MyFitTheme.type.caption, color = c.danger)
        }
        val hasPhoto = localImageModel(state.imagePath) != null
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            TextAction(
                stringResource(if (hasPhoto) R.string.exercise_image_replace else R.string.exercise_image_pick),
                onClick = onPickImage,
            )
            if (hasPhoto) TextAction(stringResource(R.string.exercise_image_remove), onClick = onRemoveImage, color = c.textSecondary)
        }
    }
}

/** The photo the user picked, else the library drawing (with its credit), else a placeholder. */
@Composable
private fun ExerciseImage(state: ExerciseEditorUiState) {
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
            Text(stringResource(R.string.exercise_image_credit, source), style = MyFitTheme.type.caption, color = c.textTertiary)
        }
    }
}
