package io.github.luannamorim.myfit.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.luannamorim.myfit.core.time.TimeSource
import io.github.luannamorim.myfit.core.time.today
import io.github.luannamorim.myfit.data.prefs.SettingsRepository
import io.github.luannamorim.myfit.data.repository.ActivityRepository
import io.github.luannamorim.myfit.data.repository.BodyRepository
import io.github.luannamorim.myfit.data.repository.HistoryRepository
import io.github.luannamorim.myfit.data.repository.NutritionTracker
import io.github.luannamorim.myfit.data.repository.RoutineRepository
import io.github.luannamorim.myfit.domain.activity.LiveActivity
import io.github.luannamorim.myfit.domain.activity.Activity
import io.github.luannamorim.myfit.domain.body.BodyComparison
import io.github.luannamorim.myfit.domain.body.BodyMetrics
import io.github.luannamorim.myfit.domain.body.BodySnapshot
import io.github.luannamorim.myfit.domain.history.HistoryCalculator
import io.github.luannamorim.myfit.domain.history.SessionSummary
import io.github.luannamorim.myfit.domain.nutrition.NutritionGoals
import io.github.luannamorim.myfit.domain.nutrition.NutritionStatus
import io.github.luannamorim.myfit.domain.model.RoutineOverview
import io.github.luannamorim.myfit.domain.session.ActiveSession
import io.github.luannamorim.myfit.platform.activity.LiveActivityController
import io.github.luannamorim.myfit.platform.session.SessionController
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

enum class Greeting { MORNING, AFTERNOON, EVENING;
    companion object {
        fun forHour(hour: Int): Greeting = when (hour) {
            in 5..11 -> MORNING
            in 12..17 -> AFTERNOON
            else -> EVENING
        }
    }
}

/** A day of Home's week: a workout or activity done, and the calorie and water targets hit. */
data class DayMark(
    val date: LocalDate,
    val trained: Boolean,
    val isToday: Boolean,
    val ateOnTarget: Boolean = false,
    val drankOnTarget: Boolean = false,
)

/** [totalDurationMs] counts workouts and logged activities. */
data class WeekStrip(val days: List<DayMark>, val sessions: Int, val totalDurationMs: Long, val activities: Int = 0)

data class HomeUiState(
    val loading: Boolean = true,
    val greeting: Greeting = Greeting.MORNING,
    val today: LocalDate = LocalDate.MIN,
    val active: ActiveSession? = null,
    /** A sport being timed right now. */
    val live: LiveActivity? = null,
    /** Today's finished workouts, latest first. */
    val todaySessions: List<SessionSummary> = emptyList(),
    /** Today's logged activities (swimming, walking…). */
    val todayActivities: List<Activity> = emptyList(),
    val suggested: RoutineOverview? = null,
    /** The next reminder is an activity (then [suggested] is null). */
    val suggestedActivity: ActivityTarget? = null,
    /** When [suggested] or [suggestedActivity] comes from a workout reminder: that reminder's moment (local). */
    val suggestedAt: LocalDateTime? = null,
    val routines: List<RoutineOverview> = emptyList(),
    val body: BodySnapshot? = null,
    /** Today's water and calories against their targets. */
    val nutrition: NutritionStatus? = null,
    val week: WeekStrip = WeekStrip(emptyList(), 0, 0),
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel @Inject constructor(
    private val routines: RoutineRepository,
    private val history: HistoryRepository,
    private val activities: ActivityRepository,
    private val controller: SessionController,
    private val liveActivity: LiveActivityController,
    private val body: BodyRepository,
    private val nutrition: NutritionTracker,
    private val settings: SettingsRepository,
    private val time: TimeSource,
) : ViewModel() {
    /** Re-read on resume: Home can stay alive overnight, and "today" (week, nutrition) must follow. */
    private val todayFlow = MutableStateFlow(time.today())

    /** The week follows the first day chosen in Settings (Sunday or Monday). */
    val state: StateFlow<HomeUiState> = combine(todayFlow, settings.settings.map { it.firstDayOfWeek }.distinctUntilChanged()) { today, first -> today to first }
        .flatMapLatest { (today, first) -> observe(today, first) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    /** Bumped on resume: the next-workout card depends on the clock (a missed reminder leaves after 6 h). */
    private val resumed = MutableStateFlow(0L)

    fun refreshToday() {
        todayFlow.value = time.today()
        resumed.value++
    }

    private fun observe(today: LocalDate, firstDay: DayOfWeek): Flow<HomeUiState> {
        val week = HistoryCalculator.weekOf(today, firstDay)
        return combine(
            combine(routines.observeOverviews(), settings.settings.map { it.reminders }.distinctUntilChanged(), activities.observeLastDays(), resumed) { r, rem, days, _ ->
                Triple(r, rem, days)
            },
            combine(history.observeSummaries(week), activities.observe(week)) { s, a -> s to a },
            combine(nutrition.observe(today), nutrition.observeRange(week)) { n, days -> n to days.associateBy { it.day.date } },
            combine(controller.active, liveActivity.live) { a, l -> a to l },
            combine(body.observeAssessments(), settings.settings.map { it.sex }.distinctUntilChanged()) { raw, sex ->
                BodyMetrics.withStatus(BodyComparison.snapshot(BodyMetrics.derive(raw, sex)), sex)
            },
        ) { (list, reminders, sportDays), (weekSessions, weekActivities), (nutritionToday, weekNutrition), (active, live), bodySnapshot ->
            val now = Instant.ofEpochMilli(time.now().wallMs).atZone(time.zone())
            val scheduled = scheduledNext(list, reminders, now, sportDays, live)
            val trainedDays = HistoryCalculator.activeDays(weekSessions, weekActivities)
            HomeUiState(
                loading = false,
                greeting = Greeting.forHour(now.hour),
                today = today,
                active = active,
                live = live,
                todaySessions = weekSessions.filter { it.startDate == today },
                todayActivities = weekActivities.filter { it.date == today },
                suggested = if (scheduled == null) suggestRoutine(list) else scheduled.routine,
                suggestedActivity = scheduled?.target as? ActivityTarget,
                suggestedAt = scheduled?.at,
                routines = list,
                body = bodySnapshot,
                nutrition = nutritionToday,
                week = WeekStrip(
                    days = (0L..6L).map { week.start.plusDays(it) }.map { d ->
                        val n = weekNutrition[d]
                        DayMark(
                            date = d,
                            trained = d in trainedDays,
                            isToday = d == today,
                            ateOnTarget = n != null && NutritionGoals.kcalHit(n.day, n.targets, n.goal) == true,
                            drankOnTarget = n != null && NutritionGoals.waterHit(n.day, n.targets) == true,
                        )
                    },
                    sessions = weekSessions.size,
                    totalDurationMs = weekSessions.sumOf { it.durationMs } + weekActivities.sumOf { it.durationMs },
                    activities = weekActivities.size,
                ),
            )
        }
    }
}
