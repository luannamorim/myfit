package io.github.luannamorim.myfit.ui.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.luannamorim.myfit.core.time.TimeSource
import io.github.luannamorim.myfit.core.time.today
import io.github.luannamorim.myfit.data.prefs.SettingsRepository
import io.github.luannamorim.myfit.data.repository.ActivityRepository
import io.github.luannamorim.myfit.data.repository.HistoryRepository
import io.github.luannamorim.myfit.data.repository.NutritionTracker
import io.github.luannamorim.myfit.domain.activity.Activity
import io.github.luannamorim.myfit.domain.nutrition.NutritionGoals
import io.github.luannamorim.myfit.domain.nutrition.NutritionStatus
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

enum class SummaryPeriod { WEEK, MONTH }

/** Days of the summary period (up to today) that hit a target: shown as "hit/days". */
data class DaysOnTarget(val hit: Int, val days: Int)

data class HistoryUiState(
    val month: YearMonth,
    val today: LocalDate,
    val selected: LocalDate,
    val marked: Set<LocalDate> = emptySet(),
    val daySessions: List<SessionSummary> = emptyList(),
    val monthSessions: List<SessionSummary> = emptyList(),
    val dayActivities: List<Activity> = emptyList(),
    val monthActivities: List<Activity> = emptyList(),
    val period: SummaryPeriod = SummaryPeriod.WEEK,
    val summary: PeriodSummary = PeriodSummary(0, 0, 0.0, emptyList()),
    val summaryRange: ClosedRange<LocalDate>,
    val isLoading: Boolean = true,
    /** The selected day's nutrition log against its targets. */
    val dayNutrition: NutritionStatus? = null,
    /** Calendar dots: days that hit the calorie target and the water target. */
    val foodOnTarget: Set<LocalDate> = emptySet(),
    val waterOnTarget: Set<LocalDate> = emptySet(),
    /** Summary tiles "Food/Water on target"; null = nothing logged in the period (tiles hidden). */
    val periodFood: DaysOnTarget? = null,
    val periodWater: DaysOnTarget? = null,
    /** First day of the calendar's weeks and of the week summary (Settings). */
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val history: HistoryRepository,
    activities: ActivityRepository,
    tracker: NutritionTracker,
    settings: SettingsRepository,
    time: TimeSource,
) : ViewModel() {
    private val today = time.today()
    private val month = MutableStateFlow(YearMonth.from(today))
    private val selected = MutableStateFlow(today)
    private val period = MutableStateFlow(SummaryPeriod.WEEK)

    /** What is on screen: the month, the selected day, the summary period and the weeks' first day. */
    private data class View(val month: YearMonth, val selected: LocalDate, val period: SummaryPeriod, val firstDay: DayOfWeek)

    private val view = combine(month, selected, period, settings.settings.map { it.firstDayOfWeek }.distinctUntilChanged()) { m, d, p, f ->
        View(m, d, p, f)
    }

    /** Query covers the visible grid plus the selected week (which may spill into another month). */
    private val queryRange = view.map { v ->
        val grid = HistoryCalculator.calendarGrid(v.month, v.firstDay)
        val week = HistoryCalculator.weekOf(v.selected, v.firstDay)
        minOf(grid.start, week.start)..maxOf(grid.endInclusive, week.endInclusive)
    }.distinctUntilChanged()

    private val sessions = queryRange.flatMapLatest { range ->
        combine(history.observeSummaries(range), activities.observe(range)) { s, a -> s to a }
    }

    private val nutrition = combine(selected, queryRange) { d, range -> d to range }
        .distinctUntilChanged()
        .flatMapLatest { (d, range) -> combine(tracker.observe(d), tracker.observeRange(range)) { day, days -> day to days } }

    val state: StateFlow<HistoryUiState> = combine(view, sessions, nutrition) { v, (items, logged), (dayNutrition, gridNutrition) ->
        val (m, d, p) = v
        val range = rangeOf(p, d, m, v.firstDay)
        // Days of the period so far: a week in progress isn't counted against days still to come.
        val elapsed = if (range.start.isAfter(today)) 0 else (range.start.toEpochDay()..minOf(range.endInclusive, today).toEpochDay()).count()
        val periodDays = gridNutrition.filter { it.day.date in range && !it.day.date.isAfter(today) }
        val periodLogged = periodDays.any { it.day.waterEntries > 0 || it.day.meals.isNotEmpty() }
        val monthRange = HistoryCalculator.monthOf(m)
        HistoryUiState(
            month = m,
            today = today,
            selected = d,
            marked = HistoryCalculator.activeDays(items, logged),
            daySessions = items.filter { it.startDate == d }.sortedBy { it.startTime },
            monthSessions = items.filter { it.startDate in monthRange },
            dayActivities = logged.filter { it.date == d }.sortedBy { it.id },
            monthActivities = logged.filter { it.date in monthRange },
            period = p,
            summary = HistoryCalculator.summarize(items, range, logged),
            summaryRange = range,
            isLoading = false,
            dayNutrition = dayNutrition,
            foodOnTarget = gridNutrition.filter { NutritionGoals.kcalHit(it.day, it.targets, it.goal) == true }.map { it.day.date }.toSet(),
            waterOnTarget = gridNutrition.filter { NutritionGoals.waterHit(it.day, it.targets) == true }.map { it.day.date }.toSet(),
            periodFood = if (periodLogged) DaysOnTarget(periodDays.count { NutritionGoals.kcalHit(it.day, it.targets, it.goal) == true }, elapsed) else null,
            periodWater = if (periodLogged) DaysOnTarget(periodDays.count { NutritionGoals.waterHit(it.day, it.targets) == true }, elapsed) else null,
            firstDayOfWeek = v.firstDay,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        HistoryUiState(month = YearMonth.from(today), today = today, selected = today, summaryRange = HistoryCalculator.weekOf(today)),
    )

    private fun rangeOf(p: SummaryPeriod, d: LocalDate, m: YearMonth, firstDay: DayOfWeek) = when (p) {
        SummaryPeriod.WEEK -> HistoryCalculator.weekOf(d, firstDay)
        SummaryPeriod.MONTH -> HistoryCalculator.monthOf(m)
    }

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
