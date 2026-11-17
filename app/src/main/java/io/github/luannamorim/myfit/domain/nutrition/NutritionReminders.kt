package io.github.luannamorim.myfit.domain.nutrition

import kotlinx.serialization.Serializable
import java.time.LocalTime
import java.time.ZonedDateTime

/** A daily "time to eat" reminder at [minuteOfDay]. */
@Serializable
data class MealReminder(val id: Long, val minuteOfDay: Int, val enabled: Boolean = true)

/** "Time to drink water" every [everyMinutes] from [fromMinute] until [untilMinute] (inclusive), every day. */
@Serializable
data class WaterReminder(
    val enabled: Boolean = false,
    val everyMinutes: Int = 120,
    val fromMinute: Int = 8 * 60,
    val untilMinute: Int = 22 * 60,
)

/** Nutrition reminders, kept in AppSettings (DataStore) so the settings backup carries them. */
@Serializable
data class NutritionReminders(val meals: List<MealReminder> = emptyList(), val water: WaterReminder = WaterReminder())

/** Pure scheduling rules for the nutrition reminders. */
object NutritionReminderSchedule {
    /** Water intervals offered in the editor, in minutes. */
    val INTERVALS = listOf(60, 90, 120, 180)
    private const val MINUTES_PER_DAY = 24 * 60

    /** Next moment [meal] fires, strictly after [now]; null when it's off. */
    fun nextMeal(meal: MealReminder, now: ZonedDateTime): ZonedDateTime? =
        if (meal.enabled) nextDaily(listOf(meal.minuteOfDay), now) else null

    /** Times of day the water reminder fires (minutes): from, from + every, … up to until. */
    fun waterSlots(water: WaterReminder): List<Int> {
        val from = water.fromMinute.coerceIn(0, MINUTES_PER_DAY - 1)
        val until = water.untilMinute.coerceIn(from, MINUTES_PER_DAY - 1)
        val step = water.everyMinutes.coerceAtLeast(15)
        return generateSequence(from) { it + step }.takeWhile { it <= until }.toList()
    }

    /** Next water slot strictly after [now] (today's or tomorrow's first); null when it's off. */
    fun nextWater(water: WaterReminder, now: ZonedDateTime): ZonedDateTime? =
        if (water.enabled) nextDaily(waterSlots(water), now) else null

    /** Id for a new meal reminder, never equal to an existing one. */
    fun nextMealId(meals: List<MealReminder>): Long = (meals.maxOfOrNull { it.id } ?: 0L) + 1

    /** Earliest of [minutes] (every day) strictly after [now]. A DST gap moves the time forward. */
    private fun nextDaily(minutes: List<Int>, now: ZonedDateTime): ZonedDateTime? {
        if (minutes.isEmpty()) return null
        val today = now.toLocalDate()
        return (0L..1L).asSequence()
            .flatMap { d -> minutes.sorted().asSequence().map { ZonedDateTime.of(today.plusDays(d), LocalTime.ofSecondOfDay(it * 60L), now.zone) } }
            .firstOrNull { it.isAfter(now) }
    }
}
