package io.github.luannamorim.myfit.ui.feature.workout

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.core.format.DecimalInput
import io.github.luannamorim.myfit.core.format.Durations
import io.github.luannamorim.myfit.domain.model.ItemStatus
import io.github.luannamorim.myfit.domain.model.ThemeMode
import io.github.luannamorim.myfit.ui.common.ExerciseVisual
import io.github.luannamorim.myfit.ui.common.openExerciseVideo
import io.github.luannamorim.myfit.ui.components.ConfirmDialog
import io.github.luannamorim.myfit.ui.components.ContentMaxWidth
import io.github.luannamorim.myfit.ui.components.NumberStepper
import io.github.luannamorim.myfit.ui.components.PrimaryButton
import io.github.luannamorim.myfit.ui.components.RestRing
import io.github.luannamorim.myfit.ui.components.SecondaryButton
import io.github.luannamorim.myfit.ui.components.TextAction
import io.github.luannamorim.myfit.ui.feature.routines.LOAD_STEP_KG
import io.github.luannamorim.myfit.ui.feature.routines.REST_STEP_SECONDS
import io.github.luannamorim.myfit.ui.theme.MyFitShapes
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import io.github.luannamorim.myfit.ui.theme.Spacing

@Composable
fun WorkoutScreen(
    onMinimize: () -> Unit,
    onFinish: (sessionId: Long) -> Unit,
    onSessionGone: () -> Unit,
    viewModel: WorkoutViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(state.loading, state.hasSession) {
        if (!state.loading && !state.hasSession) onSessionGone()
    }

    // Ask once for notifications so the rest alert can sound with the screen locked.
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    var askedPermission by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !askedPermission &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            askedPermission = true
            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val view = LocalView.current
    DisposableEffect(state.keepScreenOn) {
        view.keepScreenOn = state.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    MyFitTheme(ThemeMode.DARK) {
        WorkoutContent(
            state = state,
            onMinimize = onMinimize,
            onTogglePause = viewModel::togglePause,
            onChangeReps = viewModel::changeReps,
            onChangeLoad = viewModel::changeLoad,
            onCompleteSet = viewModel::completeSet,
            onSkipSet = viewModel::skipSet,
            onSkipExercise = viewModel::skipExercise,
            onGoToExercise = viewModel::goToExercise,
            onAdjustRest = viewModel::adjustRest,
            onSkipRest = viewModel::skipRest,
            onSetExerciseRest = viewModel::setExerciseRest,
            onFinish = { if (viewModel.requestFinish()) onFinish(state.sessionId) },
            onDiscard = viewModel::discard,
            onDismissDiscardPrompt = viewModel::dismissDiscardPrompt,
            onRestOver = { RestFeedback.restOver(context) },
            onStartHold = viewModel::startHold,
            onFinishHold = viewModel::finishHold,
            onCancelHold = viewModel::cancelHold,
        )
    }
}

@Composable
fun WorkoutContent(
    state: WorkoutUiState,
    onMinimize: () -> Unit,
    onTogglePause: () -> Unit,
    onChangeReps: (Int) -> Unit,
    onChangeLoad: (Double) -> Unit,
    onCompleteSet: () -> Unit,
    onSkipSet: () -> Unit,
    onSkipExercise: () -> Unit,
    onGoToExercise: (Int) -> Unit,
    onAdjustRest: (Int) -> Unit,
    onSkipRest: () -> Unit,
    onSetExerciseRest: (Int) -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
    onDismissDiscardPrompt: () -> Unit,
    onRestOver: () -> Unit,
    onStartHold: () -> Unit = {},
    onFinishHold: () -> Unit = {},
    onCancelHold: () -> Unit = {},
) {
    val c = MyFitTheme.colors
    var confirmDiscard by remember { mutableStateOf(false) }
    var editRest by remember { mutableStateOf(false) }
    BackHandler(onBack = onMinimize)

    // Pulse the accent when a rest ends while the screen is visible.
    val pulse = remember { Animatable(0f) }
    var wasResting by remember { mutableStateOf(false) }
    LaunchedEffect(state.rest != null, state.restOver) {
        if (wasResting && state.rest == null && state.restOver) {
            onRestOver()
            pulse.snapTo(1f)
            pulse.animateTo(0f, tween(900))
        }
        wasResting = state.rest != null
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(c.background)
            .background(c.accent.copy(alpha = 0.18f * pulse.value)),
    ) {
        Column(
            Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TopRow(state, onMinimize, onTogglePause, onSkipExercise, { editRest = true }, onFinish, { confirmDiscard = true })
            ExerciseStrip(state, onGoToExercise)
            Box(
                Modifier.weight(1f).widthIn(max = ContentMaxWidth).fillMaxWidth().padding(horizontal = Spacing.screen),
                contentAlignment = Alignment.Center,
            ) {
                AnimatedContent(
                    targetState = when {
                        state.allDone -> Phase.DONE
                        state.rest != null -> Phase.REST
                        else -> Phase.SET
                    },
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
                    label = "phase",
                ) { phase ->
                    when (phase) {
                        Phase.REST -> RestPanel(state, onAdjustRest, onSkipRest)
                        Phase.SET -> if (state.timed) {
                            TimedSetPanel(state, onChangeReps, onChangeLoad, onCompleteSet, onSkipSet, onStartHold, onFinishHold, onCancelHold, onRestOver)
                        } else {
                            SetPanel(state, onChangeReps, onChangeLoad, onCompleteSet, onSkipSet)
                        }
                        Phase.DONE -> DonePanel(onFinish)
                    }
                }
            }
        }
        if (state.isPaused) PausedOverlay(onTogglePause)
    }

    if (confirmDiscard) {
        ConfirmDialog(
            title = stringResource(R.string.workout_discard_title),
            message = stringResource(R.string.workout_discard_message),
            confirmText = stringResource(R.string.action_discard),
            destructive = true,
            onConfirm = { confirmDiscard = false; onDiscard() },
            onDismiss = { confirmDiscard = false },
        )
    }
    if (state.showDiscardPrompt) {
        ConfirmDialog(
            title = stringResource(R.string.workout_nothing_done_title),
            message = stringResource(R.string.workout_nothing_done_message),
            confirmText = stringResource(R.string.action_discard),
            destructive = true,
            onConfirm = onDiscard,
            onDismiss = onDismissDiscardPrompt,
        )
    }
    if (editRest) {
        RestDialog(state.exerciseRestSeconds, onConfirm = { onSetExerciseRest(it); editRest = false }, onDismiss = { editRest = false })
    }
}

private enum class Phase { SET, REST, DONE }

@Composable
private fun TopRow(
    state: WorkoutUiState,
    onMinimize: () -> Unit,
    onTogglePause: () -> Unit,
    onSkipExercise: () -> Unit,
    onEditRest: () -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
) {
    val c = MyFitTheme.colors
    var menu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.s, vertical = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onMinimize) {
            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = stringResource(R.string.workout_minimize), tint = c.textPrimary)
        }
        Column(Modifier.weight(1f)) {
            Text(state.routineName, style = MyFitTheme.type.label, color = c.textSecondary, maxLines = 1)
            Text(Durations.clock(state.elapsedMs), style = MyFitTheme.type.numericLarge.copy(fontSize = MyFitTheme.type.title.fontSize), color = c.textPrimary)
        }
        IconButton(onClick = onTogglePause) {
            Icon(
                if (state.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                contentDescription = stringResource(if (state.isPaused) R.string.workout_resume else R.string.workout_pause),
                tint = c.textPrimary,
            )
        }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.action_more), tint = c.textPrimary)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = c.surfaceRaised) {
                if (!state.allDone) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.workout_skip_exercise)) }, onClick = { menu = false; onSkipExercise() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.workout_exercise_rest)) }, onClick = { menu = false; onEditRest() })
                }
                DropdownMenuItem(text = { Text(stringResource(R.string.workout_end)) }, onClick = { menu = false; onFinish() })
                DropdownMenuItem(text = { Text(stringResource(R.string.workout_discard), color = c.danger) }, onClick = { menu = false; onDiscard() })
            }
        }
    }
}

@Composable
private fun ExerciseStrip(state: WorkoutUiState, onGoTo: (Int) -> Unit) {
    val c = MyFitTheme.colors
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = Spacing.screen, vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s),
    ) {
        state.exercises.forEachIndexed { index, chip ->
            val (container, content, border) = when {
                chip.isCurrent -> Triple(c.textPrimary, c.background, c.textPrimary)
                chip.status == ItemStatus.DONE -> Triple(c.accentMuted, c.accent, c.accentMuted)
                chip.status == ItemStatus.SKIPPED -> Triple(Color.Transparent, c.textTertiary, c.outline)
                else -> Triple(c.surface, c.textSecondary, c.outline)
            }
            Surface(
                onClick = { onGoTo(index) },
                shape = MyFitShapes.pill,
                color = container,
                contentColor = content,
                border = BorderStroke(1.dp, border),
            ) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (chip.status == ItemStatus.DONE && !chip.isCurrent) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.size(4.dp))
                    }
                    Text(chip.name, style = MyFitTheme.type.label, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun SetPanel(
    state: WorkoutUiState,
    onChangeReps: (Int) -> Unit,
    onChangeLoad: (Double) -> Unit,
    onCompleteSet: () -> Unit,
    onSkipSet: () -> Unit,
) {
    val c = MyFitTheme.colors
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = Spacing.l),
        verticalArrangement = Arrangement.spacedBy(Spacing.l, Alignment.CenterVertically),
    ) {
        if (state.restOver) {
            Text(
                stringResource(R.string.workout_rest_over).uppercase(),
                style = MyFitTheme.type.overline,
                color = c.accent,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(stringResource(R.string.workout_set_of, state.setNumber, state.setCount).uppercase(), style = MyFitTheme.type.overline, color = c.textSecondary)
            Text(state.exerciseName, style = MyFitTheme.type.display, color = c.textPrimary)
            state.exerciseNotes?.let { Text(it, style = MyFitTheme.type.body, color = c.textSecondary) }
        }
        Box {
            ExerciseVisual(
                imagePath = state.imagePath,
                seedKey = state.seedKey,
                muscleGroup = state.muscleGroup,
                name = state.exerciseName,
                modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp).aspectRatio(16f / 9f),
                background = c.surface,
            )
            IconButton(
                onClick = { openExerciseVideo(context, state.exerciseName) },
                modifier = Modifier.align(Alignment.BottomEnd),
            ) {
                Icon(Icons.Rounded.PlayCircle, contentDescription = stringResource(R.string.exercise_video), tint = c.textSecondary)
            }
        }
        NumberStepper(
            value = state.reps.toString(),
            label = stringResource(R.string.item_reps),
            onDecrement = { onChangeReps(-1) },
            onIncrement = { onChangeReps(1) },
            valueStyle = MyFitTheme.type.numericHero.copy(fontSize = MyFitTheme.type.numericHero.fontSize * 0.75f),
        )
        NumberStepper(
            value = state.loadKg?.let { DecimalInput.format(it) } ?: "—",
            unit = state.loadKg?.let { stringResource(R.string.unit_kg) },
            label = stringResource(R.string.item_load),
            onDecrement = { onChangeLoad(-LOAD_STEP_KG) },
            onIncrement = { onChangeLoad(LOAD_STEP_KG) },
            valueStyle = MyFitTheme.type.numericHero.copy(fontSize = MyFitTheme.type.numericHero.fontSize * 0.75f),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            PrimaryButton(
                stringResource(R.string.workout_complete_set),
                onClick = onCompleteSet,
                icon = Icons.Rounded.Check,
                minHeight = 64.dp,
                modifier = Modifier.fillMaxWidth(),
            )
            TextAction(stringResource(R.string.workout_skip_set), onClick = onSkipSet, color = c.textSecondary)
        }
    }
}

/**
 * A set measured in time (plank, cardio): target with ± steps and a timer that counts down with
 * beeps, keeps counting past the target and completes the set with the seconds actually held.
 */
@Composable
private fun TimedSetPanel(
    state: WorkoutUiState,
    onChangeTime: (Int) -> Unit,
    onChangeLoad: (Double) -> Unit,
    onCompleteSet: () -> Unit,
    onSkipSet: () -> Unit,
    onStartHold: () -> Unit,
    onFinishHold: () -> Unit,
    onCancelHold: () -> Unit,
    onTargetReached: () -> Unit,
) {
    val c = MyFitTheme.colors
    val hold = state.hold
    LaunchedEffect(hold?.reachedTarget) { if (hold?.reachedTarget == true) onTargetReached() }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = Spacing.l),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.l, Alignment.CenterVertically),
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(stringResource(R.string.workout_set_of, state.setNumber, state.setCount).uppercase(), style = MyFitTheme.type.overline, color = c.textSecondary)
            Text(state.exerciseName, style = MyFitTheme.type.display, color = c.textPrimary)
            state.exerciseNotes?.let { Text(it, style = MyFitTheme.type.body, color = c.textSecondary) }
        }
        if (hold == null) {
            ExerciseVisual(
                imagePath = state.imagePath,
                seedKey = state.seedKey,
                muscleGroup = state.muscleGroup,
                name = state.exerciseName,
                modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).aspectRatio(16f / 9f),
                background = c.surface,
            )
            NumberStepper(
                value = Durations.setTime(state.reps),
                label = stringResource(R.string.item_time),
                onDecrement = { onChangeTime(-1) },
                onIncrement = { onChangeTime(1) },
                valueStyle = MyFitTheme.type.numericHero.copy(fontSize = MyFitTheme.type.numericHero.fontSize * 0.75f),
            )
            NumberStepper(
                value = state.loadKg?.let { DecimalInput.format(it) } ?: "—",
                unit = state.loadKg?.let { stringResource(R.string.unit_kg) },
                label = stringResource(R.string.item_load),
                onDecrement = { onChangeLoad(-LOAD_STEP_KG) },
                onIncrement = { onChangeLoad(LOAD_STEP_KG) },
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                PrimaryButton(stringResource(R.string.workout_hold_start), onClick = onStartHold, icon = Icons.Rounded.PlayArrow, minHeight = 64.dp, modifier = Modifier.fillMaxWidth())
                Row {
                    TextAction(stringResource(R.string.workout_complete_set), onClick = onCompleteSet, color = c.textSecondary)
                    TextAction(stringResource(R.string.workout_skip_set), onClick = onSkipSet, color = c.textSecondary)
                }
            }
        } else {
            val over = hold.elapsedMs - hold.targetMs
            RestRing(
                progress = if (hold.reachedTarget) 1f else hold.elapsedMs.toFloat() / hold.targetMs.coerceAtLeast(1),
                remainingText = if (over >= 0) "+" + Durations.countdown(over) else Durations.countdown(-over),
                caption = stringResource(if (hold.reachedTarget) R.string.workout_hold_extra else R.string.workout_hold_running, Durations.setTime(state.reps)),
                size = 260.dp,
            )
            PrimaryButton(stringResource(R.string.workout_hold_done), onClick = onFinishHold, icon = Icons.Rounded.Check, minHeight = 64.dp, modifier = Modifier.fillMaxWidth())
            TextAction(stringResource(R.string.action_cancel), onClick = onCancelHold, color = c.textSecondary)
        }
    }
}

@Composable
private fun RestPanel(state: WorkoutUiState, onAdjustRest: (Int) -> Unit, onSkipRest: () -> Unit) {
    val c = MyFitTheme.colors
    val rest = state.rest ?: return
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(vertical = Spacing.l),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xl, Alignment.CenterVertically),
    ) {
        RestRing(
            progress = rest.progress,
            remainingText = Durations.countdown(rest.remainingMs),
            caption = stringResource(R.string.workout_rest),
            size = 260.dp,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            SecondaryButton(stringResource(R.string.workout_rest_minus), onClick = { onAdjustRest(-REST_STEP_SECONDS) })
            SecondaryButton(stringResource(R.string.workout_rest_plus), onClick = { onAdjustRest(REST_STEP_SECONDS) })
        }
        if (rest.nextName != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.workout_next).uppercase(), style = MyFitTheme.type.overline, color = c.textSecondary)
                Text(
                    stringResource(R.string.workout_next_set, rest.nextName, rest.nextSet, rest.nextSetCount),
                    style = MyFitTheme.type.bodyStrong,
                    color = c.textPrimary,
                    textAlign = TextAlign.Center,
                )
            }
        }
        PrimaryButton(stringResource(R.string.workout_skip_rest), onClick = onSkipRest, modifier = Modifier.fillMaxWidth(), minHeight = 64.dp)
    }
}

@Composable
private fun DonePanel(onFinish: () -> Unit) {
    val c = MyFitTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.l)) {
        Box(Modifier.size(72.dp).clip(MyFitShapes.pill).background(c.accentMuted), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = c.accent, modifier = Modifier.size(36.dp))
        }
        Text(stringResource(R.string.workout_all_done), style = MyFitTheme.type.headline, color = c.textPrimary, textAlign = TextAlign.Center)
        Text(stringResource(R.string.workout_all_done_hint), style = MyFitTheme.type.body, color = c.textSecondary, textAlign = TextAlign.Center)
        PrimaryButton(stringResource(R.string.workout_finish), onClick = onFinish, modifier = Modifier.fillMaxWidth(), minHeight = 64.dp)
    }
}

@Composable
private fun PausedOverlay(onResume: () -> Unit) {
    val c = MyFitTheme.colors
    Box(Modifier.fillMaxSize().background(c.background.copy(alpha = 0.92f)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.l)) {
            Text(stringResource(R.string.workout_paused), style = MyFitTheme.type.headline, color = c.textPrimary)
            PrimaryButton(stringResource(R.string.workout_resume), onClick = onResume, icon = Icons.Rounded.PlayArrow, minHeight = 64.dp)
        }
    }
}

@Composable
private fun RestDialog(current: Int, onConfirm: (Int) -> Unit, onDismiss: () -> Unit) {
    var seconds by remember { mutableIntStateOf(current) }
    val c = MyFitTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surfaceRaised,
        shape = MyFitShapes.large,
        title = { Text(stringResource(R.string.workout_exercise_rest), style = MyFitTheme.type.title, color = c.textPrimary) },
        text = {
            NumberStepper(
                value = Durations.rest(seconds),
                label = stringResource(R.string.item_rest),
                onDecrement = { seconds = (seconds - REST_STEP_SECONDS).coerceAtLeast(0) },
                onIncrement = { seconds = (seconds + REST_STEP_SECONDS).coerceAtMost(600) },
            )
        },
        confirmButton = { TextAction(stringResource(R.string.action_save), onClick = { onConfirm(seconds) }) },
        dismissButton = { TextAction(stringResource(R.string.action_cancel), onClick = onDismiss, color = c.textSecondary) },
    )
}
