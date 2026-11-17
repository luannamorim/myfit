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
import io.github.luannamorim.myfit.data.repository.RoutineRepository
import io.github.luannamorim.myfit.domain.activity.LiveActivity
import io.github.luannamorim.myfit.domain.body.BodyComparison
import io.github.luannamorim.myfit.domain.body.BodyMetrics
import io.github.luannamorim.myfit.domain.body.BodySnapshot
import io.github.luannamorim.myfit.domain.history.HistoryCalculator
import io.github.luannamorim.myfit.domain.history.SessionSummary
import io.github.luannamorim.myfit.domain.model.Reminder
import io.github.luannamorim.myfit.domain.model.RoutineOverview
import io.github.luannamorim.myfit.domain.reminder.ReminderSchedule
import io.github.luannamorim.myfit.domain.session.ActiveSession
import io.github.luannamorim.myfit.platform.activity.LiveActivityController
import io.github.luannamorim.myfit.platform.session.SessionController
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime
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
    /** A sport being timed right now. */
    val live: LiveActivity? = null,
    val todaySessions: List<SessionSummary> = emptyList(),
    val suggested: RoutineOverview? = null,
    /** When [suggested] comes from a workout reminder: that reminder's moment (local). */
    val suggestedAt: LocalDateTime? = null,
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

/** A routine with a reminder pointing to it, and the reminder's moment. */
data class ScheduledRoutine(val routine: RoutineOverview, val at: LocalDateTime)

/** How long a missed workout stays the next workout after its reminder's time. */
const val MISSED_WORKOUT_HOURS = 4L

/**
 * The routine of the next workout reminder. A missed one stays for [MISSED_WORKOUT_HOURS] after its time,
 * or until another workout reminder's time comes; then its slot after that counts. A slot is done once its
 * routine was done after the reminder's previous day (a one-off: since today or its day), ahead of time
 * included. Disabled and free-text reminders, and routines that are gone or have no exercises, are skipped.
 */
fun scheduledRoutine(routines: List<RoutineOverview>, reminders: List<Reminder>, now: ZonedDateTime): ScheduledRoutine? {
    val today = now.toLocalDate()
    val byId = routines.filter { it.exerciseCount > 0 }.associateBy { it.id }
    val workouts = reminders.filter { it.enabled }.mapNotNull { r -> r.routineId?.let(byId::get)?.let { r to it } }
    // Another workout reminder's time came after [slot], up to now.
    fun overtaken(reminder: Reminder, slot: ZonedDateTime) = workouts.any { (other, _) ->
        other.id != reminder.id && ReminderSchedule.nextTrigger(other, slot)?.isAfter(now) == false
    }
    return workouts
        .mapNotNull { (reminder, routine) ->
            val slot = pendingSlot(reminder, routine, now.minusHours(MISSED_WORKOUT_HOURS), today) ?: return@mapNotNull null
            val at = if (!slot.isAfter(now) && overtaken(reminder, slot)) pendingSlot(reminder, routine, slot, today) else slot
            at?.let { ScheduledRoutine(routine, it.toLocalDateTime()) to it.toInstant() }
        }
        .minByOrNull { it.second }
        ?.first
}

/** First slot of [reminder] after [after] whose [routine] isn't done yet (see [scheduledRoutine]). */
private fun pendingSlot(reminder: Reminder, routine: RoutineOverview, after: ZonedDateTime, today: LocalDate): ZonedDateTime? {
    val slot = ReminderSchedule.nextTrigger(reminder, after) ?: return null
    val day = slot.toLocalDate()
    val since = ReminderSchedule.previousDay(reminder, day) ?: minOf(day, today).minusDays(1)
    val done = (routine.lastDoneEpochDay ?: Long.MIN_VALUE) > since.toEpochDay()
    return if (done) ReminderSchedule.nextTrigger(reminder, day.atTime(LocalTime.MAX).atZone(after.zone)) else slot
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    routines: RoutineRepository,
    history: HistoryRepository,
    activities: ActivityRepository,
    controller: SessionController,
    liveActivity: LiveActivityController,
    body: BodyRepository,
    settings: SettingsRepository,
    time: TimeSource,
) : ViewModel() {
    private val today = time.today()
    private val week = HistoryCalculator.weekOf(today)

    val state: StateFlow<HomeUiState> = combine(
        combine(routines.observeOverviews(), settings.settings.map { it.reminders }.distinctUntilChanged()) { r, rem -> r to rem },
        combine(history.observeSummaries(week), activities.observe(week)) { s, a -> s to a },
        history.observeLast(),
        combine(controller.active, liveActivity.live) { a, l -> a to l },
        combine(body.observeAssessments(), settings.settings.map { it.sex }.distinctUntilChanged()) { raw, sex ->
            BodyMetrics.withStatus(BodyComparison.snapshot(BodyMetrics.derive(raw, sex)), sex)
        },
    ) { (list, reminders), (weekSessions, weekActivities), last, (active, live), bodySnapshot ->
        val now = Instant.ofEpochMilli(time.now().wallMs).atZone(time.zone())
        val scheduled = scheduledRoutine(list, reminders, now)
        val trainedDays = HistoryCalculator.activeDays(weekSessions, weekActivities)
        HomeUiState(
            loading = false,
            greeting = Greeting.forHour(now.hour),
            today = today,
            active = active,
            live = live,
            todaySessions = weekSessions.filter { it.startDate == today },
            suggested = scheduled?.routine ?: suggestRoutine(list),
            suggestedAt = scheduled?.at,
            routines = list,
            last = last,
            body = bodySnapshot,
            week = WeekStrip(
                days = (0L..6L).map { week.start.plusDays(it) }.map { DayMark(it, it in trainedDays, it == today) },
                sessions = weekSessions.size,
                totalDurationMs = weekSessions.sumOf { it.durationMs } + weekActivities.sumOf { it.durationMs },
                activities = weekActivities.size,
            ),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
