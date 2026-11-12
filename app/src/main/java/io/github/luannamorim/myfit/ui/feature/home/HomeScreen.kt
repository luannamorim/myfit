package io.github.luannamorim.myfit.ui.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.core.format.Dates
import io.github.luannamorim.myfit.core.format.Durations
import io.github.luannamorim.myfit.core.format.appLocale
import io.github.luannamorim.myfit.domain.model.RoutineOverview
import io.github.luannamorim.myfit.ui.common.lastDoneText
import io.github.luannamorim.myfit.ui.components.EmptyState
import io.github.luannamorim.myfit.ui.components.MyFitCard
import io.github.luannamorim.myfit.ui.components.PrimaryButton
import io.github.luannamorim.myfit.ui.components.SecondaryButton
import io.github.luannamorim.myfit.ui.components.SectionHeader
import io.github.luannamorim.myfit.ui.components.TabScreen
import io.github.luannamorim.myfit.ui.components.TextAction
import io.github.luannamorim.myfit.ui.feature.evolution.BodySnapshotTiles
import io.github.luannamorim.myfit.ui.feature.history.SessionRow
import io.github.luannamorim.myfit.ui.navigation.ActiveSessionBar
import io.github.luannamorim.myfit.ui.theme.MyFitShapes
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import io.github.luannamorim.myfit.ui.theme.Spacing

data class HomeActions(
    val onStart: (Long) -> Unit = {},
    val onResume: () -> Unit = {},
    val onOpenRoutine: (Long) -> Unit = {},
    val onCreateRoutine: () -> Unit = {},
    val onSeeRoutines: () -> Unit = {},
    val onOpenSession: (Long) -> Unit = {},
    val onOpenHistory: () -> Unit = {},
    val onOpenBody: () -> Unit = {},
)

@Composable
fun HomeScreen(actions: HomeActions, modifier: Modifier = Modifier, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HomeContent(state, actions, modifier, body = {
        val snapshot = state.body
        if (snapshot == null) BodyEmptyCard(actions.onOpenBody) else BodySnapshotTiles(snapshot, onClick = actions.onOpenBody)
    })
}

@Composable
fun HomeContent(state: HomeUiState, actions: HomeActions, modifier: Modifier = Modifier, body: @Composable () -> Unit = {}) {
    val c = MyFitTheme.colors
    val greeting = stringResource(
        when (state.greeting) {
            Greeting.MORNING -> R.string.home_morning
            Greeting.AFTERNOON -> R.string.home_afternoon
            Greeting.EVENING -> R.string.home_evening
        },
    )
    TabScreen(
        title = greeting,
        subtitle = if (state.loading) null else Dates.long(state.today).replaceFirstChar { it.titlecase(appLocale) },
        modifier = modifier,
    ) {
        if (state.loading) return@TabScreen
        val active = state.active
        when {
            active != null -> ActiveSessionBar(active, onResume = actions.onResume)
            state.todaySessions.isNotEmpty() -> TodayDone(state)
        }

        if (state.routines.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.home_empty_title),
                message = stringResource(R.string.home_empty_message),
                action = { PrimaryButton(stringResource(R.string.routines_new), onClick = actions.onCreateRoutine, icon = Icons.Rounded.Add) },
            )
        } else {
            state.suggested?.takeIf { active == null }?.let { NextWorkout(it, state.today.toEpochDay(), actions) }
            SectionHeader(stringResource(R.string.home_your_routines), action = { TextAction(stringResource(R.string.home_see_all), onClick = actions.onSeeRoutines) })
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                state.routines.forEach { RoutineTile(it, state.today.toEpochDay(), actions, enabled = active == null) }
            }
        }

        state.last?.let { last ->
            SectionHeader(stringResource(R.string.home_last_activity), Modifier.padding(top = Spacing.s))
            SessionRow(last, showDate = true, onClick = { actions.onOpenSession(last.id) })
        }

        SectionHeader(stringResource(R.string.home_body), Modifier.padding(top = Spacing.s))
        body()

        SectionHeader(stringResource(R.string.home_week), Modifier.padding(top = Spacing.s))
        WeekCard(state, onClick = actions.onOpenHistory)
    }
}

@Composable
private fun TodayDone(state: HomeUiState) {
    val c = MyFitTheme.colors
    val session = state.todaySessions.last()
    MyFitCard(Modifier.fillMaxWidth(), color = c.accentMuted, borderColor = c.accentMuted) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(MyFitShapes.pill).background(c.accent), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = c.onAccent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(Spacing.m))
            Column {
                Text(stringResource(R.string.home_today_done), style = MyFitTheme.type.label, color = c.textSecondary)
                Text(
                    "${session.routineName} · ${Durations.human(session.durationMs)}",
                    style = MyFitTheme.type.bodyStrong,
                    color = c.textPrimary,
                )
            }
        }
    }
}

@Composable
private fun NextWorkout(routine: RoutineOverview, todayEpochDay: Long, actions: HomeActions) {
    val c = MyFitTheme.colors
    MyFitCard(Modifier.fillMaxWidth(), contentPadding = Spacing.l) {
        Text(stringResource(R.string.home_next).uppercase(), style = MyFitTheme.type.overline, color = c.textSecondary)
        Spacer(Modifier.height(Spacing.s))
        Text(routine.name, style = MyFitTheme.type.headline, color = c.textPrimary)
        Text(
            pluralStringResource(R.plurals.exercise_count, routine.exerciseCount, routine.exerciseCount) + " · " + lastDoneText(routine.lastDoneEpochDay, todayEpochDay),
            style = MyFitTheme.type.caption,
            color = c.textSecondary,
        )
        Spacer(Modifier.height(Spacing.l))
        PrimaryButton(stringResource(R.string.home_start), onClick = { actions.onStart(routine.id) }, icon = Icons.Rounded.PlayArrow, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun RoutineTile(routine: RoutineOverview, todayEpochDay: Long, actions: HomeActions, enabled: Boolean) {
    val c = MyFitTheme.colors
    MyFitCard(onClick = { actions.onOpenRoutine(routine.id) }, modifier = Modifier.width(184.dp), contentPadding = Spacing.l) {
        Text(routine.name, style = MyFitTheme.type.bodyStrong, color = c.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(lastDoneText(routine.lastDoneEpochDay, todayEpochDay), style = MyFitTheme.type.caption, color = c.textSecondary, maxLines = 1)
        Spacer(Modifier.height(Spacing.m))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                pluralStringResource(R.plurals.exercise_count, routine.exerciseCount, routine.exerciseCount),
                style = MyFitTheme.type.caption,
                color = c.textTertiary,
                modifier = Modifier.weight(1f),
            )
            FilledIconButton(
                onClick = { actions.onStart(routine.id) },
                enabled = enabled && routine.exerciseCount > 0,
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = c.surfaceRaised,
                    contentColor = c.textPrimary,
                    disabledContainerColor = c.surfaceRaised,
                    disabledContentColor = c.textTertiary,
                ),
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.action_start) + " " + routine.name, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun WeekCard(state: HomeUiState, onClick: () -> Unit) {
    val c = MyFitTheme.colors
    MyFitCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            state.week.days.forEach { day ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        Dates.weekdayNarrow(day.date.dayOfWeek).uppercase(),
                        style = MyFitTheme.type.overline,
                        color = if (day.isToday) c.textPrimary else c.textTertiary,
                    )
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(MyFitShapes.pill)
                            .background(if (day.trained) c.accent else c.surfaceRaised)
                            .then(if (day.isToday && !day.trained) Modifier.border(BorderStroke(1.dp, c.outlineStrong), MyFitShapes.pill) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (day.trained) Icon(Icons.Rounded.Check, contentDescription = null, tint = c.onAccent, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(Spacing.m))
        Text(
            stringResource(
                R.string.home_week_summary,
                pluralStringResource(R.plurals.home_sessions_count, state.week.sessions, state.week.sessions),
                Durations.human(state.week.totalDurationMs),
            ),
            style = MyFitTheme.type.caption,
            color = c.textSecondary,
        )
    }
}

/** Placeholder until body assessments exist (filled by the Evolution stage). */
@Composable
fun BodyEmptyCard(onAdd: () -> Unit) {
    val c = MyFitTheme.colors
    MyFitCard(Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.home_body_empty), style = MyFitTheme.type.body, color = c.textSecondary)
        Spacer(Modifier.height(Spacing.m))
        SecondaryButton(stringResource(R.string.home_body_cta), onClick = onAdd, icon = Icons.Rounded.Add)
    }
}
