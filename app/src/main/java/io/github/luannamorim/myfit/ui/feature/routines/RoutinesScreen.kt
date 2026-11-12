package io.github.luannamorim.myfit.ui.feature.routines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.domain.model.RoutineOverview
import io.github.luannamorim.myfit.ui.common.lastDoneText
import io.github.luannamorim.myfit.ui.components.ConfirmDialog
import io.github.luannamorim.myfit.ui.components.EmptyState
import io.github.luannamorim.myfit.ui.components.MyFitCard
import io.github.luannamorim.myfit.ui.components.PrimaryButton
import io.github.luannamorim.myfit.ui.components.TabScreen
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import io.github.luannamorim.myfit.ui.theme.Spacing

@Composable
fun RoutinesScreen(
    onCreate: () -> Unit,
    onEdit: (Long) -> Unit,
    onStart: (Long) -> Unit,
    onOpenLibrary: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RoutinesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val copySuffix = stringResource(R.string.routine_copy_suffix)
    RoutinesContent(
        state = state,
        onCreate = onCreate,
        onEdit = onEdit,
        onStart = onStart,
        onDuplicate = { viewModel.duplicate(it, copySuffix) },
        onDelete = viewModel::delete,
        onOpenLibrary = onOpenLibrary,
        modifier = modifier,
    )
}

@Composable
fun RoutinesContent(
    state: RoutinesUiState,
    onCreate: () -> Unit,
    onEdit: (Long) -> Unit,
    onStart: (Long) -> Unit,
    onDuplicate: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onOpenLibrary: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingDelete by remember { mutableStateOf<RoutineOverview?>(null) }

    TabScreen(
        title = stringResource(R.string.nav_routines),
        modifier = modifier,
        actions = {
            FilledIconButton(
                onClick = onCreate,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MyFitTheme.colors.surfaceRaised,
                    contentColor = MyFitTheme.colors.textPrimary,
                ),
            ) { Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.routines_new)) }
        },
    ) {
        if (!state.isLoading && state.routines.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.routines_empty_title),
                message = stringResource(R.string.routines_empty_message),
                action = { PrimaryButton(stringResource(R.string.routines_new), onClick = onCreate, icon = Icons.Rounded.Add) },
            )
        }
        state.routines.forEach { routine ->
            RoutineCard(
                routine = routine,
                todayEpochDay = state.todayEpochDay,
                onOpen = { onEdit(routine.id) },
                onStart = { onStart(routine.id) },
                onDuplicate = { onDuplicate(routine.id) },
                onDelete = { pendingDelete = routine },
            )
        }
        Spacer(Modifier.height(Spacing.s))
        MyFitCard(onClick = onOpenLibrary, color = MyFitTheme.colors.background) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Rounded.LibraryBooks, contentDescription = null, tint = MyFitTheme.colors.textSecondary)
                Spacer(Modifier.width(Spacing.m))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.routines_library), style = MyFitTheme.type.bodyStrong, color = MyFitTheme.colors.textPrimary)
                    Text(stringResource(R.string.routines_library_hint), style = MyFitTheme.type.caption, color = MyFitTheme.colors.textSecondary)
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MyFitTheme.colors.textTertiary)
            }
        }
    }

    pendingDelete?.let { routine ->
        ConfirmDialog(
            title = stringResource(R.string.routines_delete_title),
            message = stringResource(R.string.routines_delete_message, routine.name),
            confirmText = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = { onDelete(routine.id); pendingDelete = null },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun RoutineCard(
    routine: RoutineOverview,
    todayEpochDay: Long,
    onOpen: () -> Unit,
    onStart: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    val c = MyFitTheme.colors
    var menuOpen by remember { mutableStateOf(false) }
    MyFitCard(onClick = onOpen, modifier = Modifier.fillMaxWidth(), contentPadding = Spacing.l) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(routine.name, style = MyFitTheme.type.title, color = c.textPrimary)
                val count = pluralStringResource(R.plurals.exercise_count, routine.exerciseCount, routine.exerciseCount)
                Text(
                    "$count · ${lastDoneText(routine.lastDoneEpochDay, todayEpochDay)}",
                    style = MyFitTheme.type.caption,
                    color = c.textSecondary,
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.action_more), tint = c.textSecondary)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, containerColor = c.surfaceRaised) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.action_edit)) }, onClick = { menuOpen = false; onOpen() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.action_duplicate)) }, onClick = { menuOpen = false; onDuplicate() })
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_delete), color = c.danger) },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
            }
            FilledIconButton(
                onClick = onStart,
                enabled = routine.exerciseCount > 0,
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = c.accent,
                    contentColor = c.onAccent,
                    disabledContainerColor = c.surfaceRaised,
                    disabledContentColor = c.textTertiary,
                ),
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.action_start) + " " + routine.name)
            }
        }
    }
}
