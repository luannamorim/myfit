package io.github.luannamorim.myfit.domain.model

import kotlinx.serialization.Serializable
import java.time.DayOfWeek

/**
 * A workout reminder: weekly on [days] at [minuteOfDay], or once on [dateEpochDay] when [days] is empty.
 * Kept inside [AppSettings] (DataStore), so the settings backup carries it.
 */
@Serializable
data class Reminder(
    val id: Long,
    /** Routine the reminder points to; null for a free-text activity. */
    val routineId: Long? = null,
    /** Routine name when the reminder was saved, or the free text. */
    val label: String,
    /** Local time of day in minutes, 0..1439. */
    val minuteOfDay: Int,
    /** Date of a one-off reminder (only used when [days] is empty). */
    val dateEpochDay: Long? = null,
    /** Weekdays it repeats on; empty means one-off. */
    val days: Set<DayOfWeek> = emptySet(),
    val enabled: Boolean = true,
) {
    val isRepeating: Boolean get() = days.isNotEmpty()
}
