package io.github.luannamorim.myfit.ui.feature.home

import io.github.luannamorim.myfit.domain.activity.LiveActivity
import io.github.luannamorim.myfit.domain.activity.Sport
import io.github.luannamorim.myfit.domain.activity.SportLastDay
import io.github.luannamorim.myfit.domain.activity.lastDay
import io.github.luannamorim.myfit.domain.activity.sameActivityName
import io.github.luannamorim.myfit.domain.model.Reminder
import io.github.luannamorim.myfit.domain.model.RoutineOverview
import io.github.luannamorim.myfit.domain.reminder.ReminderSchedule
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime

/** Suggests the routine with exercises never done yet (first in order), else the one done longest ago. */
fun suggestRoutine(routines: List<RoutineOverview>): RoutineOverview? =
    routines.withIndex()
        .filter { it.value.exerciseCount > 0 }
        .minWithOrNull(compareBy<IndexedValue<RoutineOverview>>({ it.value.lastDoneEpochDay != null }, { it.value.lastDoneEpochDay ?: 0 }, { it.index }))
        ?.value

/** What a workout reminder points to on Home's "Next workout". */
sealed interface NextTarget

data class RoutineTarget(val routine: RoutineOverview) : NextTarget

/** A sport ([name] only for [Sport.OTHER]) and the last day it was logged. */
data class ActivityTarget(val sport: Sport, val name: String?, val lastDoneEpochDay: Long?) : NextTarget

/** The target of the next workout reminder, and the reminder's moment. */
data class ScheduledNext(val target: NextTarget, val at: LocalDateTime)

val ScheduledNext.routine: RoutineOverview? get() = (target as? RoutineTarget)?.routine

/** How long a missed reminder stays on the card (the user, 1.18.2; it was until midnight since 1.8.0, 4 h before). */
const val MISSED_WORKOUT_HOURS = 6L

/**
 * The target of the next workout reminder: a routine, or a sport (1.18.0). A missed one stays
 * [MISSED_WORKOUT_HOURS] after its time, or until another reminder's time comes; then its slot after that counts. A routine slot is done once that routine was done after the reminder's previous day
 * (a one-off: since today or its day), ahead of time included. A sport slot is done only by that sport logged
 * on the slot's own day (the user, 1.18.1: Wednesday's ride doesn't do Saturday's); a timer of that sport
 * running now counts as logged today, and any routine workout counts for [Sport.GYM]. Disabled reminders,
 * free-text ones not linked to a sport, and routines that are gone or have no exercises are skipped.
 */
fun scheduledNext(
    routines: List<RoutineOverview>,
    reminders: List<Reminder>,
    now: ZonedDateTime,
    sportDays: List<SportLastDay> = emptyList(),
    live: LiveActivity? = null,
): ScheduledNext? {
    val today = now.toLocalDate()
    val byId = routines.filter { it.exerciseCount > 0 }.associateBy { it.id }
    val targets = reminders.filter { it.enabled }.mapNotNull { r ->
        val routineId = r.routineId
        val sport = r.sport
        when {
            routineId != null -> byId[routineId]?.let { r to Slotted(RoutineTarget(it), it.lastDoneEpochDay, onItsDay = false) }
            sport != null -> {
                val name = r.label.takeIf { sport == Sport.OTHER }
                // Any routine workout counts for a gym reminder too.
                val gym = if (sport == Sport.GYM) routines.mapNotNull { it.lastDoneEpochDay }.maxOrNull() else null
                val last = listOfNotNull(sportDays.lastDay(sport, name), gym).maxOrNull()
                val timing = live != null && live.sport == sport && (sport != Sport.OTHER || sameActivityName(live.name, name))
                val done = if (timing) maxOf(last ?: Long.MIN_VALUE, today.toEpochDay()) else last
                r to Slotted(ActivityTarget(sport, name, last), done, onItsDay = true)
            }
            else -> null
        }
    }

    // Another reminder's time came after [slot], up to now.
    fun overtaken(reminder: Reminder, slot: ZonedDateTime) = targets.any { (other, _) ->
        other.id != reminder.id && ReminderSchedule.nextTrigger(other, slot)?.isAfter(now) == false
    }
    return targets
        .mapNotNull { (reminder, target) ->
            // Slots from MISSED_WORKOUT_HOURS ago on: a missed one stays that long.
            val slot = pendingSlot(reminder, target, now.minusHours(MISSED_WORKOUT_HOURS), today) ?: return@mapNotNull null
            val at = if (!slot.isAfter(now) && overtaken(reminder, slot)) pendingSlot(reminder, target, slot, today) else slot
            at?.let { ScheduledNext(target.target, it.toLocalDateTime()) to it.toInstant() }
        }
        .minByOrNull { it.second }
        ?.first
}

/** A target, the last day that counts as done for it, and whether only that slot's own day counts. */
private data class Slotted(val target: NextTarget, val doneEpochDay: Long?, val onItsDay: Boolean)

/** First slot of [reminder] after [after] not done yet (see [scheduledNext]). */
private fun pendingSlot(reminder: Reminder, target: Slotted, after: ZonedDateTime, today: LocalDate): ZonedDateTime? {
    val slot = ReminderSchedule.nextTrigger(reminder, after) ?: return null
    val day = slot.toLocalDate()
    val since = if (target.onItsDay) day.minusDays(1) else ReminderSchedule.previousDay(reminder, day) ?: minOf(day, today).minusDays(1)
    val done = (target.doneEpochDay ?: Long.MIN_VALUE) > since.toEpochDay()
    return if (done) ReminderSchedule.nextTrigger(reminder, day.atTime(LocalTime.MAX).atZone(after.zone)) else slot
}
