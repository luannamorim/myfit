package io.github.luannamorim.myfit.ui.feature.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.domain.model.Exercise
import io.github.luannamorim.myfit.domain.model.MuscleGroup
import io.github.luannamorim.myfit.ui.common.labelRes
import io.github.luannamorim.myfit.ui.common.localImageModel
import io.github.luannamorim.myfit.ui.components.ContentMaxWidth
import io.github.luannamorim.myfit.ui.components.EmptyState
import io.github.luannamorim.myfit.ui.components.FilterChipRow
import io.github.luannamorim.myfit.ui.components.MyFitTopBar
import io.github.luannamorim.myfit.ui.components.PrimaryButton
import io.github.luannamorim.myfit.ui.theme.MyFitShapes
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import io.github.luannamorim.myfit.ui.theme.Spacing

@Composable
fun ExerciseLibraryScreen(
    onBack: () -> Unit,
    onOpen: (Long) -> Unit,
    onCreate: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LibraryScaffold(
        title = stringResource(R.string.library_title),
        state = state,
        onBack = onBack,
        onCreate = onCreate,
        onQuery = viewModel::setQuery,
        onToggleGroup = viewModel::toggleGroup,
        onItemClick = { onOpen(it.id) },
        selectable = false,
    )
}

@Composable
fun ExercisePickerScreen(
    onBack: () -> Unit,
    onConfirm: (LongArray) -> Unit,
    onCreate: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LibraryScaffold(
        title = stringResource(R.string.picker_title),
        state = state,
        onBack = onBack,
        onCreate = onCreate,
        onQuery = viewModel::setQuery,
        onToggleGroup = viewModel::toggleGroup,
        onItemClick = { viewModel.toggleSelected(it.id) },
        selectable = true,
        bottomBar = {
            if (state.selected.isNotEmpty()) {
                Box(Modifier.fillMaxWidth().background(MyFitTheme.colors.background).navigationBarsPadding().padding(Spacing.l)) {
                    PrimaryButton(
                        pluralStringResource(R.plurals.picker_confirm, state.selected.size, state.selected.size),
                        onClick = { onConfirm(state.selected.toLongArray()) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
    )
}

@Composable
private fun LibraryScaffold(
    title: String,
    state: LibraryUiState,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onQuery: (String) -> Unit,
    onToggleGroup: (MuscleGroup) -> Unit,
    onItemClick: (Exercise) -> Unit,
    selectable: Boolean,
    bottomBar: @Composable () -> Unit = {},
) {
    val c = MyFitTheme.colors
    Scaffold(
        containerColor = c.background,
        topBar = {
            MyFitTopBar(
                title = title,
                onBack = onBack,
                actions = {
                    IconButton(onClick = onCreate) {
                        Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.library_new_exercise))
                    }
                },
            )
        },
        bottomBar = bottomBar,
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(bottom = Spacing.xxl),
        ) {
            item(key = "search") {
                Column(
                    Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth().padding(horizontal = Spacing.screen),
                    verticalArrangement = Arrangement.spacedBy(Spacing.m),
                ) {
                    SearchField(state.query, onQuery)
                    FilterChipRow(
                        options = MuscleGroup.entries,
                        selected = state.groups,
                        label = { stringResource(it.labelRes) },
                        onToggle = onToggleGroup,
                    )
                    Spacer(Modifier.size(Spacing.xs))
                }
            }
            if (!state.isLoading && state.exercises.isEmpty()) {
                item(key = "empty") {
                    EmptyState(stringResource(R.string.library_empty), "")
                }
            }
            items(state.exercises, key = { it.id }) { exercise ->
                val isSelected = exercise.id in state.selected
                ExerciseRow(
                    exercise = exercise,
                    selected = if (selectable) isSelected else null,
                    onClick = { onItemClick(exercise) },
                    modifier = Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit) {
    val c = MyFitTheme.colors
    TextField(
        value = query,
        onValueChange = onQuery,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text(stringResource(R.string.library_search_placeholder), color = c.textTertiary) },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = c.textSecondary) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQuery("") }) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_clear), tint = c.textSecondary)
                }
            }
        },
        shape = MyFitShapes.pill,
        textStyle = MyFitTheme.type.body,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = c.surfaceRaised,
            unfocusedContainerColor = c.surfaceRaised,
            focusedIndicatorColor = c.surfaceRaised,
            unfocusedIndicatorColor = c.surfaceRaised,
            cursorColor = c.accent,
            focusedTextColor = c.textPrimary,
            unfocusedTextColor = c.textPrimary,
        ),
    )
}

@Composable
private fun ExerciseRow(
    exercise: Exercise,
    selected: Boolean?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = MyFitTheme.colors
    Column(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .semantics { if (selected != null) this.selected = selected }
                .clickable(role = if (selected != null) Role.Checkbox else Role.Button, onClick = onClick)
                .padding(horizontal = Spacing.screen, vertical = Spacing.m),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ExerciseThumb(exercise)
            Spacer(Modifier.width(Spacing.m))
            Column(Modifier.weight(1f)) {
                Text(exercise.name, style = MyFitTheme.type.bodyStrong, color = c.textPrimary)
                val meta = stringResource(exercise.muscleGroup.labelRes) + " · " + stringResource(exercise.movementType.labelRes) +
                    if (exercise.isCustom) " · " + stringResource(R.string.library_custom_badge) else ""
                Text(meta, style = MyFitTheme.type.caption, color = c.textSecondary)
            }
            if (selected != null) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(MyFitShapes.pill)
                        .background(if (selected) c.accent else c.surfaceRaised),
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = c.onAccent, modifier = Modifier.size(18.dp))
                }
            }
        }
        HorizontalDivider(Modifier.padding(start = Spacing.screen + 44.dp + Spacing.m), color = c.outline)
    }
}

@Composable
private fun ExerciseThumb(exercise: Exercise) {
    val c = MyFitTheme.colors
    val model = localImageModel(exercise.imagePath)
    Box(
        Modifier.size(44.dp).clip(MyFitShapes.small).background(c.surfaceRaised),
        contentAlignment = Alignment.Center,
    ) {
        if (model != null) {
            AsyncImage(model = model, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Text(
                exercise.name.take(1).uppercase(),
                style = MyFitTheme.type.bodyStrong,
                color = c.textSecondary,
            )
        }
    }
}
