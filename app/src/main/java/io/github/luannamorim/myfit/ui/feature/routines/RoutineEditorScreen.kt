package io.github.luannamorim.myfit.ui.feature.routines

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.core.format.Durations
import io.github.luannamorim.myfit.core.format.formatKg
import io.github.luannamorim.myfit.domain.model.ExerciseMeasure
import io.github.luannamorim.myfit.ui.components.ConfirmDialog
import io.github.luannamorim.myfit.ui.components.ContentMaxWidth
import io.github.luannamorim.myfit.ui.components.MyFitCard
import io.github.luannamorim.myfit.ui.components.MyFitTextField
import io.github.luannamorim.myfit.ui.components.MyFitTopBar
import io.github.luannamorim.myfit.ui.components.SecondaryButton
import io.github.luannamorim.myfit.ui.components.SectionHeader
import io.github.luannamorim.myfit.ui.components.TextAction
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import io.github.luannamorim.myfit.ui.theme.Spacing
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun RoutineEditorScreen(
    pickedExerciseIds: LongArray?,
    onPickedConsumed: () -> Unit,
    onAddExercises: () -> Unit,
    onClose: () -> Unit,
    viewModel: RoutineEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(pickedExerciseIds) {
        if (pickedExerciseIds != null) {
            viewModel.addExercises(pickedExerciseIds.toList())
            onPickedConsumed()
        }
    }
    LaunchedEffect(state.saved) { if (state.saved) onClose() }

    RoutineEditorContent(
        state = state,
        onNameChange = viewModel::setName,
        onDescriptionChange = viewModel::setDescription,
        onAddExercises = onAddExercises,
        onMove = viewModel::moveByKey,
        onUpdateItem = viewModel::updateItem,
        onRemoveItem = viewModel::remove,
        onSave = { viewModel.save() },
        onClose = onClose,
    )
}

@Composable
fun RoutineEditorContent(
    state: RoutineEditorUiState,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onAddExercises: () -> Unit,
    onMove: (Any, Any) -> Unit,
    onUpdateItem: (Int, (DraftItem) -> DraftItem) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
) {
    val c = MyFitTheme.colors
    var editingKey by remember { mutableStateOf<Long?>(null) }
    var confirmDiscard by remember { mutableStateOf(false) }
    val requestClose = { if (state.dirty) confirmDiscard = true else onClose() }
    BackHandler(enabled = state.dirty) { confirmDiscard = true }

    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to -> onMove(from.key, to.key) }

    Scaffold(
        containerColor = c.background,
        topBar = {
            MyFitTopBar(
                title = stringResource(if (state.isNew) R.string.editor_new_title else R.string.editor_edit_title),
                onBack = requestClose,
                actions = { TextAction(stringResource(R.string.action_save), onClick = onSave, enabled = !state.isLoading) },
            )
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(bottom = Spacing.xxxl),
        ) {
            item(key = "fields") {
                Column(
                    Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth().padding(horizontal = Spacing.screen),
                    verticalArrangement = Arrangement.spacedBy(Spacing.m),
                ) {
                    MyFitTextField(
                        value = state.name,
                        onValueChange = onNameChange,
                        label = stringResource(R.string.editor_name_label),
                        placeholder = stringResource(R.string.editor_name_placeholder),
                        isError = state.nameError,
                        supportingText = if (state.nameError) stringResource(R.string.editor_name_required) else null,
                        capitalization = KeyboardCapitalization.Sentences,
                        textStyle = MyFitTheme.type.title,
                    )
                    MyFitTextField(
                        value = state.description,
                        onValueChange = onDescriptionChange,
                        label = stringResource(R.string.editor_description_label),
                        singleLine = false,
                    )
                    SectionHeader(stringResource(R.string.editor_exercises), Modifier.padding(top = Spacing.m))
                    if (state.items.isEmpty() && !state.isLoading) {
                        Text(stringResource(R.string.editor_no_exercises), style = MyFitTheme.type.body, color = c.textSecondary)
                    }
                }
            }
            items(state.items, key = { it.key }) { item ->
                ReorderableItem(reorderState, key = item.key) { isDragging ->
                    val index = state.items.indexOfFirst { it.key == item.key }
                    MyFitCard(
                        onClick = { editingKey = item.key },
                        modifier = Modifier
                            .widthIn(max = ContentMaxWidth)
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.screen, vertical = Spacing.xs),
                        color = if (isDragging) c.surfaceRaised else c.surface,
                        contentPadding = Spacing.m,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                (index + 1).toString().padStart(2, '0'),
                                style = MyFitTheme.type.numeric,
                                color = c.textTertiary,
                                modifier = Modifier.padding(start = Spacing.xs, end = Spacing.m),
                            )
                            Column(Modifier.weight(1f)) {
                                Text(item.exercise.name, style = MyFitTheme.type.bodyStrong, color = c.textPrimary)
                                val load = item.loadKg?.let { " · " + formatKg(it) }.orEmpty()
                                Text(
                                    if (item.exercise.measure == ExerciseMeasure.TIME) {
                                        stringResource(R.string.item_summary_time, item.sets, Durations.setTime(item.reps), Durations.rest(item.restSeconds)) + load
                                    } else {
                                        stringResource(R.string.item_summary, item.sets, item.reps, Durations.rest(item.restSeconds)) + load
                                    },
                                    style = MyFitTheme.type.caption,
                                    color = c.textSecondary,
                                )
                            }
                            IconButton(onClick = {}, modifier = Modifier.draggableHandle()) {
                                Icon(Icons.Rounded.DragHandle, contentDescription = stringResource(R.string.editor_reorder), tint = c.textTertiary)
                            }
                        }
                    }
                }
            }
            item(key = "add") {
                Column(Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth().padding(horizontal = Spacing.screen)) {
                    Spacer(Modifier.height(Spacing.m))
                    SecondaryButton(
                        stringResource(R.string.editor_add_exercises),
                        onClick = onAddExercises,
                        icon = Icons.Rounded.Add,
                        modifier = Modifier.fillMaxWidth().alpha(if (state.isLoading) 0f else 1f),
                    )
                }
            }
        }
    }

    val editingIndex = state.items.indexOfFirst { it.key == editingKey }
    if (editingIndex >= 0) {
        RoutineItemSheet(
            item = state.items[editingIndex],
            onChange = { transform -> onUpdateItem(editingIndex, transform) },
            onRemove = { onRemoveItem(editingIndex); editingKey = null },
            onDismiss = { editingKey = null },
        )
    }
    if (confirmDiscard) {
        ConfirmDialog(
            title = stringResource(R.string.editor_discard_title),
            message = stringResource(R.string.editor_discard_message),
            confirmText = stringResource(R.string.action_discard),
            destructive = true,
            onConfirm = { confirmDiscard = false; onClose() },
            onDismiss = { confirmDiscard = false },
        )
    }
}
