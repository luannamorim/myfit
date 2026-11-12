package io.github.luannamorim.myfit.ui.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.core.format.Dates
import io.github.luannamorim.myfit.core.format.Durations
import io.github.luannamorim.myfit.core.format.formatOneDecimal
import io.github.luannamorim.myfit.domain.history.SessionSummary
import io.github.luannamorim.myfit.ui.components.MyFitCard
import io.github.luannamorim.myfit.ui.components.MyFitChip
import io.github.luannamorim.myfit.ui.components.SectionHeader
import io.github.luannamorim.myfit.ui.components.StatTile
import io.github.luannamorim.myfit.ui.components.TabScreen
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import io.github.luannamorim.myfit.ui.theme.Spacing

@Composable
fun HistoryScreen(onOpenSession: (Long) -> Unit, modifier: Modifier = Modifier, viewModel: HistoryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HistoryContent(state, viewModel::select, viewModel::showMonth, viewModel::setPeriod, onOpenSession, modifier)
}

@Composable
fun HistoryContent(
    state: HistoryUiState,
    onSelect: (java.time.LocalDate) -> Unit,
    onMonthChange: (java.time.YearMonth) -> Unit,
    onPeriod: (SummaryPeriod) -> Unit,
    onOpenSession: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = MyFitTheme.colors
    TabScreen(title = stringResource(R.string.nav_history), modifier = modifier) {
        MyFitCard(contentPadding = Spacing.m) {
            MonthCalendar(
                month = state.month,
                marked = state.marked,
                selected = state.selected,
                today = state.today,
                onSelect = onSelect,
                onMonthChange = onMonthChange,
            )
        }

        SectionHeader(Dates.long(state.selected))
        if (state.daySessions.isEmpty()) {
            Text(stringResource(R.string.history_no_sessions_day), style = MyFitTheme.type.body, color = c.textSecondary)
        } else {
            state.daySessions.forEach { SessionRow(it, onClick = { onOpenSession(it.id) }) }
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
            StatTile(stringResource(R.string.history_total_time), Durations.human(s.totalDurationMs), Modifier.weight(1f))
        }
        StatTile(
            stringResource(R.string.history_frequency),
            stringResource(R.string.history_frequency_value, formatOneDecimal(s.avgPerWeek)),
            Modifier.fillMaxWidth(),
        )
        if (s.exercises.isNotEmpty()) {
            MyFitCard {
                Text(stringResource(R.string.history_exercises), style = MyFitTheme.type.caption, color = c.textSecondary)
                s.exercises.take(MAX_EXERCISES).forEach { ex ->
                    Row(Modifier.fillMaxWidth().padding(top = Spacing.s)) {
                        Text(ex.name, style = MyFitTheme.type.body, color = c.textPrimary, modifier = Modifier.weight(1f))
                        Text(stringResource(R.string.history_exercise_times, ex.sessions), style = MyFitTheme.type.numeric, color = c.textSecondary)
                    }
                }
            }
        }

        if (state.monthSessions.isNotEmpty()) {
            SectionHeader(stringResource(R.string.history_month_sessions), Modifier.padding(top = Spacing.m))
            state.monthSessions.forEach { SessionRow(it, showDate = true, onClick = { onOpenSession(it.id) }) }
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
                Text(
                    stringResource(R.string.history_session_meta, start, Durations.human(session.durationMs), session.setsDone),
                    style = MyFitTheme.type.caption,
                    color = c.textSecondary,
                )
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = c.textTertiary)
        }
    }
}
