package io.github.luannamorim.myfit.ui.feature.home

import android.text.format.DateFormat
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
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Pool
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.luannamorim.myfit.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.annotation.StringRes
import io.github.luannamorim.myfit.ui.common.activityTitle
import java.time.LocalTime
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.clickable
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
import io.github.luannamorim.myfit.ui.feature.activity.LiveActivityBar
import io.github.luannamorim.myfit.ui.feature.evolution.BodySnapshotTiles
import io.github.luannamorim.myfit.ui.feature.nutrition.NutritionTodayCards
import io.github.luannamorim.myfit.ui.feature.reminders.reminderDayText
import io.github.luannamorim.myfit.ui.feature.reminders.timeText
import io.github.luannamorim.myfit.ui.navigation.ActiveSessionBar
import io.github.luannamorim.myfit.ui.theme.MyFitShapes
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import io.github.luannamorim.myfit.ui.theme.Spacing
import java.time.LocalDate
import java.time.LocalDateTime

data class HomeActions(
    val onStart: (Long) -> Unit = {},
    val onResume: () -> Unit = {},
    val onOpenRoutine: (Long) -> Unit = {},
    val onCreateRoutine: () -> Unit = {},
    val onSeeRoutines: () -> Unit = {},
    val onOpenSession: (Long) -> Unit = {},
    val onOpenHistory: () -> Unit = {},
    val onOpenBody: () -> Unit = {},
    val onLogActivity: () -> Unit = {},
    val onOpenLiveActivity: () -> Unit = {},
    val onOpenSettings: () -> Unit = {},
    val onOpenNutrition: () -> Unit = {},
    val onOpenActivity: (Long) -> Unit = {},
)

@Composable
fun HomeScreen(actions: HomeActions, modifier: Modifier = Modifier, viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Home can stay alive overnight: coming back after midnight shows the new day (week, nutrition).
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshToday()
        onPauseOrDispose { }
    }
    HomeContent(state, actions, modifier, body = {
        val snapshot = state.body
        if (snapshot == null) BodyEmptyCard(actions.onOpenBody) else BodySnapshotTiles(snapshot, onOpenWeight = actions.onOpenBody)
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
        actions = {
            // Settings left the tab bar for Nutrition (1.8.0): same quiet style as the bell.
            IconButton(onClick = actions.onOpenSettings) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.nav_settings),
                    tint = MyFitTheme.colors.textSecondary,
                    modifier = Modifier.size(22.dp),
                )
            }
        },
    ) {
        if (state.loading) return@TabScreen
        val active = state.active
        state.live?.let { LiveActivityBar(it, onOpen = actions.onOpenLiveActivity) }
        when {
            active != null -> ActiveSessionBar(active, onResume = actions.onResume)
            state.todaySessions.isNotEmpty() || state.todayActivities.isNotEmpty() -> TodayDone(state, actions)
        }

        if (state.routines.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.home_empty_title),
                message = stringResource(R.string.home_empty_message),
                action = { PrimaryButton(stringResource(R.string.routines_new), onClick = actions.onCreateRoutine, icon = Icons.Rounded.Add) },
            )
        } else {
            state.suggested?.takeIf { active == null }?.let { NextWorkout(it, state.suggestedAt, state.today, actions) }
            SectionHeader(stringResource(R.string.home_your_routines), action = { TextAction(stringResource(R.string.home_see_all), onClick = actions.onSeeRoutines) })
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                state.routines.forEach { RoutineTile(it, state.today.toEpochDay(), actions, enabled = active == null) }
            }
        }
        // Swimming, bike, soccer...: logged after the fact, they count in the week and in History.
        SecondaryButton(
            stringResource(R.string.activity_log),
            onClick = actions.onLogActivity,
            icon = Icons.Rounded.Pool,
            modifier = Modifier.fillMaxWidth(),
        )

        state.nutrition?.let { nutrition ->
            SectionHeader(stringResource(R.string.home_nutrition), Modifier.padding(top = Spacing.s))
            NutritionTodayCards(nutrition, onOpen = actions.onOpenNutrition)
        }

        SectionHeader(stringResource(R.string.home_body), Modifier.padding(top = Spacing.s))
        body()

        SectionHeader(stringResource(R.string.home_week), Modifier.padding(top = Spacing.s))
        WeekCard(state, onClick = actions.onOpenHistory)
    }
}

/** One line of "done today": a finished workout (with its start time) or a logged activity. */
private data class DoneItem(val time: LocalTime?, val title: String, val durationMs: Long, val open: () -> Unit)

/**
 * "Done today". One thing: the card as it always was. Several (user request, option A): the day's count and total
 * time on top, then each one, latest workout first, then the activities; each line opens it.
 */
@Composable
private fun TodayDone(state: HomeUiState, actions: HomeActions) {
    val c = MyFitTheme.colors
    val items = state.todaySessions.map { s -> DoneItem(s.startTime, s.routineName, s.durationMs) { actions.onOpenSession(s.id) } } +
        state.todayActivities.map { a -> DoneItem(null, activityTitle(a), a.durationMs) { actions.onOpenActivity(a.id) } }
    val single = items.singleOrNull()
    MyFitCard(Modifier.fillMaxWidth(), color = c.accentMuted, borderColor = c.accentMuted, onClick = single?.open) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(MyFitShapes.pill).background(c.accent), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = c.onAccent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(Spacing.m))
            Column {
                if (single != null) {
                    val label = if (state.todaySessions.isNotEmpty()) R.string.home_today_done else R.string.home_today_done_activity
                    Text(stringResource(label), style = MyFitTheme.type.label, color = c.textSecondary)
                    Text("${single.title} · ${Durations.human(single.durationMs)}", style = MyFitTheme.type.bodyStrong, color = c.textPrimary)
                } else {
                    val counts = listOfNotNull(
                        state.todaySessions.size.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.home_sessions_count, it, it) },
                        state.todayActivities.size.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.home_activities_count, it, it) },
                    ).joinToString(" · ")
                    Text(stringResource(R.string.home_today_count, counts), style = MyFitTheme.type.label, color = c.textSecondary)
                    Text(stringResource(R.string.home_today_total, Durations.human(items.sumOf { it.durationMs })), style = MyFitTheme.type.bodyStrong, color = c.textPrimary)
                }
            }
        }
        if (single == null) {
            Spacer(Modifier.height(Spacing.s))
            items.forEach { item ->
                Row(
                    Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = item.open).padding(start = 52.dp, top = Spacing.xs, bottom = Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(item.time?.let(Dates::time).orEmpty(), style = MyFitTheme.type.caption, color = c.textSecondary, modifier = Modifier.width(56.dp))
                    Text("${item.title} · ${Durations.human(item.durationMs)}", style = MyFitTheme.type.body, color = c.textPrimary)
                }
            }
        }
    }
}

@Composable
private fun NextWorkout(routine: RoutineOverview, at: LocalDateTime?, today: LocalDate, actions: HomeActions) {
    val c = MyFitTheme.colors
    val todayEpochDay = today.toEpochDay()
    MyFitCard(Modifier.fillMaxWidth(), contentPadding = Spacing.l) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.home_next).uppercase(), style = MyFitTheme.type.overline, color = c.textSecondary, modifier = Modifier.weight(1f))
            // From a reminder: when it is due ("Today at 18:00", "Thu at 07:00").
            if (at != null) {
                Icon(Icons.Rounded.NotificationsNone, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    stringResource(R.string.home_next_at, reminderDayText(at.toLocalDate(), today), timeText(at.hour * 60 + at.minute, DateFormat.is24HourFormat(LocalContext.current))),
                    style = MyFitTheme.type.caption,
                    color = c.textSecondary,
                    maxLines = 1,
                )
            }
        }
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
    val days = state.week.days
    MyFitCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        // One row per kind (icon at the start), one green check per day done (user-approved mockup, 1.13.0).
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(WEEK_ICON_COLUMN))
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween) {
                days.forEach { day ->
                    Text(
                        Dates.weekdayNarrow(day.date.dayOfWeek).uppercase(),
                        style = MyFitTheme.type.overline,
                        color = if (day.isToday) c.textPrimary else c.textTertiary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(WEEK_CHECK),
                    )
                }
            }
        }
        WeekRow(Icons.Outlined.FitnessCenter, days, R.string.home_week_workout, state.today) { it.trained }
        WeekRow(Icons.Outlined.Restaurant, days, R.string.home_week_food, state.today) { it.ateOnTarget }
        WeekRow(Icons.Outlined.WaterDrop, days, R.string.home_week_water, state.today) { it.drankOnTarget }
        Spacer(Modifier.height(Spacing.m))
        Text(
            stringResource(
                R.string.home_week_summary,
                pluralStringResource(R.plurals.home_sessions_count, state.week.sessions, state.week.sessions) +
                    if (state.week.activities > 0) {
                        " · " + pluralStringResource(R.plurals.home_activities_count, state.week.activities, state.week.activities)
                    } else {
                        ""
                    },
                Durations.human(state.week.totalDurationMs),
            ),
            style = MyFitTheme.type.caption,
            color = c.textSecondary,
        )
    }
}

@Composable
private fun WeekRow(icon: ImageVector, days: List<DayMark>, @StringRes doneLabel: Int, today: LocalDate, done: (DayMark) -> Boolean) {
    val c = MyFitTheme.colors
    Row(Modifier.fillMaxWidth().padding(top = Spacing.s), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(WEEK_ICON_COLUMN)) {
            Icon(icon, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(18.dp))
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween) {
            days.forEach { day ->
                val isDone = done(day)
                val label = if (isDone) stringResource(doneLabel, Dates.long(day.date)) else null
                Box(
                    Modifier
                        .alpha(if (day.date.isAfter(today)) FUTURE_ALPHA else 1f)
                        .size(WEEK_CHECK)
                        .clip(MyFitShapes.pill)
                        .background(if (isDone) c.accent else c.surfaceRaised)
                        .then(if (day.isToday && !isDone) Modifier.border(BorderStroke(1.dp, c.outlineStrong), MyFitShapes.pill) else Modifier)
                        .then(if (label != null) Modifier.semantics { contentDescription = label } else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isDone) Icon(Icons.Rounded.Check, contentDescription = null, tint = c.onAccent, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

private val WEEK_CHECK = 28.dp
private val WEEK_ICON_COLUMN = 30.dp
private const val FUTURE_ALPHA = 0.45f

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
