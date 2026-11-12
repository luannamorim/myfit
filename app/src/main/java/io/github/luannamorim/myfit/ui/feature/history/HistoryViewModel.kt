package io.github.luannamorim.myfit.ui.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.luannamorim.myfit.core.time.TimeSource
import io.github.luannamorim.myfit.core.time.today
import io.github.luannamorim.myfit.data.repository.HistoryRepository
import io.github.luannamorim.myfit.domain.history.HistoryCalculator
import io.github.luannamorim.myfit.domain.history.PeriodSummary
import io.github.luannamorim.myfit.domain.history.SessionSummary
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

enum class SummaryPeriod { WEEK, MONTH }

data class HistoryUiState(
    val month: YearMonth,
    val today: LocalDate,
    val selected: LocalDate,
    val marked: Set<LocalDate> = emptySet(),
    val daySessions: List<SessionSummary> = emptyList(),
    val monthSessions: List<SessionSummary> = emptyList(),
    val period: SummaryPeriod = SummaryPeriod.WEEK,
    val summary: PeriodSummary = PeriodSummary(0, 0, 0.0, emptyList()),
    val summaryRange: ClosedRange<LocalDate>,
    val isLoading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val history: HistoryRepository,
    time: TimeSource,
) : ViewModel() {
    private val today = time.today()
    private val month = MutableStateFlow(YearMonth.from(today))
    private val selected = MutableStateFlow(today)
    private val period = MutableStateFlow(SummaryPeriod.WEEK)

    /** Query covers the visible grid plus the selected week (which may spill into another month). */
    private val queryRange = combine(month, selected) { m, d ->
        val grid = HistoryCalculator.calendarGrid(m)
        val week = HistoryCalculator.weekOf(d)
        minOf(grid.start, week.start)..maxOf(grid.endInclusive, week.endInclusive)
    }.distinctUntilChanged()

    private val sessions = queryRange.flatMapLatest { history.observeSummaries(it) }

    val state: StateFlow<HistoryUiState> = combine(month, selected, period, sessions) { m, d, p, items ->
        val range = when (p) {
            SummaryPeriod.WEEK -> HistoryCalculator.weekOf(d)
            SummaryPeriod.MONTH -> HistoryCalculator.monthOf(m)
        }
        val monthRange = HistoryCalculator.monthOf(m)
        HistoryUiState(
            month = m,
            today = today,
            selected = d,
            marked = HistoryCalculator.daysWithSessions(items),
            daySessions = items.filter { it.startDate == d }.sortedBy { it.startTime },
            monthSessions = items.filter { it.startDate in monthRange },
            period = p,
            summary = HistoryCalculator.summarize(items, range),
            summaryRange = range,
            isLoading = false,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        HistoryUiState(month = YearMonth.from(today), today = today, selected = today, summaryRange = HistoryCalculator.weekOf(today)),
    )

    fun select(date: LocalDate) {
        selected.value = date
        val m = YearMonth.from(date)
        if (m != month.value) month.value = m
    }

    fun showMonth(target: YearMonth) {
        month.value = target
        // Keep the selection inside the visible month.
        val d = selected.value
        if (YearMonth.from(d) != target) selected.value = if (target == YearMonth.from(today)) today else target.atDay(1)
    }

    fun setPeriod(value: SummaryPeriod) {
        period.value = value
    }
}
