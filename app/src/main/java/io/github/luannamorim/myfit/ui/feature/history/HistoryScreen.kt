package io.github.luannamorim.myfit.ui.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.unit.dp
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.core.format.formatThousands
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import io.github.luannamorim.myfit.core.format.Dates
import io.github.luannamorim.myfit.core.format.Durations
import io.github.luannamorim.myfit.domain.history.SessionSummary
import io.github.luannamorim.myfit.ui.common.icon
import io.github.luannamorim.myfit.ui.common.labelRes
import io.github.luannamorim.myfit.ui.components.MyFitCard
import io.github.luannamorim.myfit.ui.components.MyFitChip
import io.github.luannamorim.myfit.ui.components.SectionHeader
import io.github.luannamorim.myfit.ui.components.StatTile
import io.github.luannamorim.myfit.ui.components.TabScreen
import io.github.luannamorim.myfit.ui.feature.activity.ActivityRow
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import io.github.luannamorim.myfit.ui.theme.Spacing

@Composable
fun HistoryScreen(
    onOpenSession: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onOpenActivity: (Long) -> Unit = {},
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HistoryContent(state, viewModel::select, viewModel::showMonth, viewModel::setPeriod, onOpenSession, modifier, onOpenActivity)
}

@Composable
fun HistoryContent(
    state: HistoryUiState,
    onSelect: (java.time.LocalDate) -> Unit,
    onMonthChange: (java.time.YearMonth) -> Unit,
    onPeriod: (SummaryPeriod) -> Unit,
    onOpenSession: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onOpenActivity: (Long) -> Unit = {},
) {
    val c = MyFitTheme.colors
    TabScreen(title = stringResource(R.string.nav_history), modifier = modifier) {
        MyFitCard(contentPadding = Spacing.m) {
            MonthCalendar(
                month = state.month,
                marked = state.marked,
                foodDays = state.foodOnTarget,
                waterDays = state.waterOnTarget,
                selected = state.selected,
                today = state.today,
                onSelect = onSelect,
                onMonthChange = onMonthChange,
            )
        }

        SectionHeader(Dates.long(state.selected))
        val dayNutrition = state.dayNutrition?.takeIf { it.logged }
        if (state.daySessions.isEmpty() && state.dayActivities.isEmpty() && dayNutrition == null) {
            Text(stringResource(R.string.history_no_sessions_day), style = MyFitTheme.type.body, color = c.textSecondary)
        } else {
            state.daySessions.forEach { SessionRow(it, onClick = { onOpenSession(it.id) }) }
            state.dayActivities.forEach { ActivityRow(it, onClick = { onOpenActivity(it.id) }) }
            dayNutrition?.let { DayNutritionCard(it) }
        }

        Row(Modifier.fillMaxWidth().padding(top = Spacing.m), verticalAlignment = Alignment.CenterVertically) {
            Text(
                rangeLabel(state).uppercase(),
                style = MyFitTheme.type.overline,
                color = c.textSecondary,
                modifier = Modifier.weight(1f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                MyFitChip(stringResource(R.string.history_week), selected = state.period == SummaryPeriod.WEEK, onClick = { onPeriod(SummaryPeriod.WEEK) })
                MyFitChip(stringResource(R.string.history_month), selected = state.period == SummaryPeriod.MONTH, onClick = { onPeriod(SummaryPeriod.MONTH) })
            }
        }
        val s = state.summary
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            StatTile(stringResource(R.string.history_sessions), s.sessions.toString(), Modifier.weight(1f))
            StatTile(stringResource(R.string.history_activities), s.activities.toString(), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
            StatTile(stringResource(R.string.history_total_time), Durations.human(s.totalDurationMs), Modifier.weight(1f))
            // Replaces "Frequency" (user request, 1.15.0): kg × reps of the sets done in the period.
            StatTile(stringResource(R.string.history_volume), formatThousands(s.volumeKg), Modifier.weight(1f), unit = "kg")
        }
        // Food and water days on target in the period (user-approved mockup, 1.15.0), in the kinds' colors.
        if (state.periodFood != null && state.periodWater != null) {
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                DaysOnTargetTile(stringResource(R.string.history_food_on_target), Icons.Outlined.Restaurant, c.food, state.periodFood, Modifier.weight(1f).fillMaxHeight())
                DaysOnTargetTile(stringResource(R.string.history_water_on_target), Icons.Outlined.WaterDrop, c.water, state.periodWater, Modifier.weight(1f).fillMaxHeight())
            }
        }

        if (s.sports.isNotEmpty()) {
            MyFitCard {
                Text(stringResource(R.string.history_sports), style = MyFitTheme.type.caption, color = c.textSecondary)
                s.sports.take(MAX_EXERCISES).forEach { sport ->
                    Row(Modifier.fillMaxWidth().padding(top = Spacing.s), verticalAlignment = Alignment.CenterVertically) {
                        Icon(sport.sport.icon, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(18.dp))
                        Text(
                            sport.name ?: stringResource(sport.sport.labelRes),
                            style = MyFitTheme.type.body,
                            color = c.textPrimary,
                            modifier = Modifier.weight(1f).padding(start = Spacing.s),
                        )
                        Text(stringResource(R.string.history_exercise_times, sport.times), style = MyFitTheme.type.numeric, color = c.textSecondary)
                    }
                }
            }
        }

        if (state.monthSessions.isNotEmpty()) {
            SectionHeader(stringResource(R.string.history_month_sessions), Modifier.padding(top = Spacing.m))
            state.monthSessions.forEach { SessionRow(it, showDate = true, onClick = { onOpenSession(it.id) }) }
        }
        if (state.monthActivities.isNotEmpty()) {
            SectionHeader(stringResource(R.string.history_month_activities), Modifier.padding(top = Spacing.m))
            state.monthActivities.forEach { ActivityRow(it, showDate = true, onClick = { onOpenActivity(it.id) }) }
        }
    }
}

private const val MAX_EXERCISES = 8

@Composable
private fun rangeLabel(state: HistoryUiState): String = when (state.period) {
    SummaryPeriod.MONTH -> Dates.monthYear(state.month.year, state.month.monthValue)
    SummaryPeriod.WEEK -> "${Dates.dayMonth(state.summaryRange.start)} – ${Dates.dayMonth(state.summaryRange.endInclusive)}"
}

@Composable
fun SessionRow(session: SessionSummary, onClick: () -> Unit, showDate: Boolean = false) {
    val c = MyFitTheme.colors
    MyFitCard(onClick = onClick, modifier = Modifier.fillMaxWidth(), contentPadding = Spacing.m) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(session.routineName, style = MyFitTheme.type.bodyStrong, color = c.textPrimary)
                val start = if (showDate) Dates.weekdayDayMonth(session.startDate) else Dates.time(session.startTime)
                val meta = stringResource(R.string.history_session_meta, start, Durations.human(session.durationMs), session.setsDone)
                val intensity = session.intensity?.let { " · " + stringResource(it.labelRes) }.orEmpty()
                Text(meta + intensity, style = MyFitTheme.type.caption, color = c.textSecondary)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = c.textTertiary)
        }
    }
}

@Composable
private fun DaysOnTargetTile(label: String, icon: ImageVector, tint: Color, value: DaysOnTarget, modifier: Modifier) {
    StatTile(
        label = label,
        value = stringResource(R.string.history_days_on_target, value.hit, value.days),
        unit = pluralStringResource(R.plurals.history_days_unit, value.days),
        icon = icon,
        iconTint = tint,
        modifier = modifier,
    )
}

