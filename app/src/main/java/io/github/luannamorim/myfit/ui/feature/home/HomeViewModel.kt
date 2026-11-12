package io.github.luannamorim.myfit.ui.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.luannamorim.myfit.core.time.TimeSource
import io.github.luannamorim.myfit.core.time.today
import io.github.luannamorim.myfit.data.repository.ActivityRepository
import io.github.luannamorim.myfit.data.repository.BodyRepository
import io.github.luannamorim.myfit.data.repository.HistoryRepository
import io.github.luannamorim.myfit.data.repository.RoutineRepository
import io.github.luannamorim.myfit.domain.body.BodyComparison
import io.github.luannamorim.myfit.domain.body.BodySnapshot
import io.github.luannamorim.myfit.domain.history.HistoryCalculator
import io.github.luannamorim.myfit.domain.history.SessionSummary
import io.github.luannamorim.myfit.domain.model.RoutineOverview
import io.github.luannamorim.myfit.domain.session.ActiveSession
import io.github.luannamorim.myfit.platform.session.SessionController
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
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

data class DayMark(val date: LocalDate, val trained: Boolean, val isToday: Boolean)

/** [totalDurationMs] counts workouts and logged activities. */
data class WeekStrip(val days: List<DayMark>, val sessions: Int, val totalDurationMs: Long, val activities: Int = 0)

data class HomeUiState(
    val loading: Boolean = true,
    val greeting: Greeting = Greeting.MORNING,
    val today: LocalDate = LocalDate.MIN,
    val active: ActiveSession? = null,
    val todaySessions: List<SessionSummary> = emptyList(),
    val suggested: RoutineOverview? = null,
    val routines: List<RoutineOverview> = emptyList(),
    val last: SessionSummary? = null,
    val body: BodySnapshot? = null,
    val week: WeekStrip = WeekStrip(emptyList(), 0, 0),
)

/** Suggests the routine with exercises never done yet (first in order), else the one done longest ago. */
fun suggestRoutine(routines: List<RoutineOverview>): RoutineOverview? =
    routines.withIndex()
        .filter { it.value.exerciseCount > 0 }
        .minWithOrNull(compareBy<IndexedValue<RoutineOverview>>({ it.value.lastDoneEpochDay != null }, { it.value.lastDoneEpochDay ?: 0 }, { it.index }))
        ?.value

@HiltViewModel
class HomeViewModel @Inject constructor(
    routines: RoutineRepository,
    history: HistoryRepository,
    activities: ActivityRepository,
    controller: SessionController,
    body: BodyRepository,
    time: TimeSource,
) : ViewModel() {
    private val today = time.today()
    private val week = HistoryCalculator.weekOf(today)

    val state: StateFlow<HomeUiState> = combine(
        routines.observeOverviews(),
        combine(history.observeSummaries(week), activities.observe(week)) { s, a -> s to a },
        history.observeLast(),
        controller.active,
        body.observeAssessments(),
    ) { list, (weekSessions, weekActivities), last, active, assessments ->
        val hour = Instant.ofEpochMilli(time.now().wallMs).atZone(time.zone()).hour
        val trainedDays = HistoryCalculator.activeDays(weekSessions, weekActivities)
        HomeUiState(
            loading = false,
            greeting = Greeting.forHour(hour),
            today = today,
            active = active,
            todaySessions = weekSessions.filter { it.startDate == today },
            suggested = suggestRoutine(list),
            routines = list,
            last = last,
            body = BodyComparison.snapshot(assessments),
            week = WeekStrip(
                days = (0L..6L).map { week.start.plusDays(it) }.map { DayMark(it, it in trainedDays, it == today) },
                sessions = weekSessions.size,
                totalDurationMs = weekSessions.sumOf { it.durationMs } + weekActivities.sumOf { it.durationMs },
                activities = weekActivities.size,
            ),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
