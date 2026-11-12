package io.github.luannamorim.myfit.domain.reminder

import io.github.luannamorim.myfit.domain.model.Reminder
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/** Pure scheduling rules for workout reminders. */
object ReminderSchedule {
    private const val MINUTES_PER_DAY = 24 * 60

    fun localTime(minuteOfDay: Int): LocalTime = LocalTime.ofSecondOfDay(minuteOfDay.coerceIn(0, MINUTES_PER_DAY - 1) * 60L)

    /**
     * Next moment [reminder] should fire, strictly after [now] and in now's zone; null when it never will
     * (a one-off in the past). Ignores [Reminder.enabled]. On a DST gap the time moves forward by the gap
     * (02:30 becomes 03:30); on an overlap the earlier of the two instants is used.
     */
    fun nextTrigger(reminder: Reminder, now: ZonedDateTime): ZonedDateTime? {
        val time = localTime(reminder.minuteOfDay)
        if (!reminder.isRepeating) {
            val date = reminder.dateEpochDay?.let(LocalDate::ofEpochDay) ?: return null
            return ZonedDateTime.of(date, time, now.zone).takeIf { it.isAfter(now) }
        }
        val today = now.toLocalDate()
        // Today (if still ahead) through the same weekday next week.
        return (0L..7L).asSequence()
            .map { today.plusDays(it) }
            .filter { it.dayOfWeek in reminder.days }
            .map { ZonedDateTime.of(it, time, now.zone) }
            .firstOrNull { it.isAfter(now) }
    }

    /** A one-off reminder whose moment has passed: it will not fire again until edited. */
    fun isPast(reminder: Reminder, now: ZonedDateTime): Boolean = !reminder.isRepeating && nextTrigger(reminder, now) == null

    /** Id for a new reminder, never equal to an existing one. */
    fun nextId(reminders: List<Reminder>): Long = (reminders.maxOfOrNull { it.id } ?: 0L) + 1
}
