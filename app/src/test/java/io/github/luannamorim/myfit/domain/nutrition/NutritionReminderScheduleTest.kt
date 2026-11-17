package io.github.luannamorim.myfit.domain.nutrition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class NutritionReminderScheduleTest {
    private val zone = ZoneId.of("America/Sao_Paulo")
    private fun at(text: String): ZonedDateTime = LocalDateTime.parse(text).atZone(zone)

    @Test
    fun meal_firesDaily_strictlyAfterNow() {
        val lunch = MealReminder(id = 1, minuteOfDay = 12 * 60)
        assertEquals(at("2026-11-17T12:00"), NutritionReminderSchedule.nextMeal(lunch, at("2026-11-17T10:00")))
        assertEquals(at("2026-11-18T12:00"), NutritionReminderSchedule.nextMeal(lunch, at("2026-11-17T12:00")))
        assertNull(NutritionReminderSchedule.nextMeal(lunch.copy(enabled = false), at("2026-11-17T10:00")))
    }

    @Test
    fun water_slotsEveryIntervalInsideTheWindow() {
        val water = WaterReminder(enabled = true, everyMinutes = 120, fromMinute = 8 * 60, untilMinute = 22 * 60)
        assertEquals((8..22 step 2).map { it * 60 }, NutritionReminderSchedule.waterSlots(water))
        val ninety = water.copy(everyMinutes = 90, untilMinute = 12 * 60)
        assertEquals(listOf(480, 570, 660), NutritionReminderSchedule.waterSlots(ninety)) // 8:00, 9:30, 11:00 (12:30 is past the window)
        // A window that ends before it starts keeps only its first slot.
        assertEquals(listOf(600), NutritionReminderSchedule.waterSlots(water.copy(fromMinute = 600, untilMinute = 540)))
    }

    @Test
    fun water_nextSlot_isTodayOrTomorrowsFirst() {
        val water = WaterReminder(enabled = true, everyMinutes = 120, fromMinute = 8 * 60, untilMinute = 22 * 60)
        assertEquals(at("2026-11-17T08:00"), NutritionReminderSchedule.nextWater(water, at("2026-11-17T06:30")))
        assertEquals(at("2026-11-17T12:00"), NutritionReminderSchedule.nextWater(water, at("2026-11-17T10:00"))) // strictly after
        assertEquals(at("2026-11-17T12:00"), NutritionReminderSchedule.nextWater(water, at("2026-11-17T10:30")))
        assertEquals(at("2026-11-18T08:00"), NutritionReminderSchedule.nextWater(water, at("2026-11-17T22:00")))
        assertNull(NutritionReminderSchedule.nextWater(water.copy(enabled = false), at("2026-11-17T10:00")))
    }

    @Test
    fun defaults_areOff_everyTwoHours_from8To22() {
        val r = NutritionReminders()
        assertEquals(emptyList<MealReminder>(), r.meals)
        assertEquals(WaterReminder(enabled = false, everyMinutes = 120, fromMinute = 480, untilMinute = 1320), r.water)
        assertEquals(listOf(60, 90, 120, 180), NutritionReminderSchedule.INTERVALS)
    }

    @Test
    fun nextMealId_neverRepeats() {
        assertEquals(1L, NutritionReminderSchedule.nextMealId(emptyList()))
        assertEquals(8L, NutritionReminderSchedule.nextMealId(listOf(MealReminder(7, 600), MealReminder(2, 700))))
    }
}
