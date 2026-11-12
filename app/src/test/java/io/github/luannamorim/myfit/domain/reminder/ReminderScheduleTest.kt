package io.github.luannamorim.myfit.domain.reminder

import io.github.luannamorim.myfit.domain.model.Reminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

class ReminderScheduleTest {
    private val saoPaulo = ZoneId.of("America/Sao_Paulo")
    private val berlin = ZoneId.of("Europe/Berlin")

    private fun at(text: String, zone: ZoneId = saoPaulo): ZonedDateTime = LocalDateTime.parse(text).atZone(zone)

    private fun weekly(hour: Int, minute: Int, vararg days: DayOfWeek) =
        Reminder(id = 1, label = "Treino A", minuteOfDay = hour * 60 + minute, days = days.toSet())

    private fun once(date: String, hour: Int, minute: Int) =
        Reminder(id = 1, label = "Corrida", minuteOfDay = hour * 60 + minute, dateEpochDay = LocalDate.parse(date).toEpochDay())

    // 2026-11-12 is a Thursday.

    @Test
    fun weekly_sameDay_later_firesToday() {
        val next = ReminderSchedule.nextTrigger(weekly(18, 30, DayOfWeek.THURSDAY), at("2026-11-12T07:00"))
        assertEquals(at("2026-11-12T18:30"), next)
    }

    @Test
    fun weekly_sameDay_earlier_firesNextWeek() {
        val next = ReminderSchedule.nextTrigger(weekly(6, 0, DayOfWeek.THURSDAY), at("2026-11-12T07:00"))
        assertEquals(at("2026-11-19T06:00"), next)
    }

    @Test
    fun weekly_exactlyNow_isNotStrictlyAfter_firesNextWeek() {
        val next = ReminderSchedule.nextTrigger(weekly(7, 0, DayOfWeek.THURSDAY), at("2026-11-12T07:00"))
        assertEquals(at("2026-11-19T07:00"), next)
    }

    @Test
    fun weekly_wrapsAroundTheWeek() {
        // Saturday night, reminder on Mondays only.
        val next = ReminderSchedule.nextTrigger(weekly(7, 0, MONDAY), at("2026-11-14T22:00"))
        assertEquals(at("2026-11-16T07:00"), next)
        // Sunday -> Monday of the next ISO week.
        assertEquals(at("2026-11-16T07:00"), ReminderSchedule.nextTrigger(weekly(7, 0, MONDAY), at("2026-11-15T23:59")))
    }

    @Test
    fun weekly_severalDays_picksTheClosest() {
        val reminder = weekly(9, 0, MONDAY, WEDNESDAY, FRIDAY)
        assertEquals(at("2026-11-13T09:00"), ReminderSchedule.nextTrigger(reminder, at("2026-11-12T10:00")))
        assertEquals(at("2026-11-16T09:00"), ReminderSchedule.nextTrigger(reminder, at("2026-11-13T09:00:01")))
    }

    @Test
    fun weekly_everyDay_lateAtNight_firesTomorrowMorning() {
        val next = ReminderSchedule.nextTrigger(weekly(6, 0, *DayOfWeek.entries.toTypedArray()), at("2026-11-12T23:00"))
        assertEquals(at("2026-11-13T06:00"), next)
    }

    @Test
    fun oneOff_future_firesAtThatMoment_pastOrNow_never() {
        val reminder = once("2026-11-20", 18, 0)
        assertEquals(at("2026-11-20T18:00"), ReminderSchedule.nextTrigger(reminder, at("2026-11-12T07:00")))
        assertEquals(at("2026-11-20T18:00"), ReminderSchedule.nextTrigger(reminder, at("2026-11-20T17:59")))
        assertNull(ReminderSchedule.nextTrigger(reminder, at("2026-11-20T18:00")))
        assertNull(ReminderSchedule.nextTrigger(reminder, at("2026-11-21T08:00")))
        assertTrue(ReminderSchedule.isPast(reminder, at("2026-11-21T08:00")))
        assertFalse(ReminderSchedule.isPast(reminder, at("2026-11-12T07:00")))
    }

    @Test
    fun oneOff_withoutDate_neverFires() {
        val reminder = Reminder(id = 1, label = "x", minuteOfDay = 60)
        assertNull(ReminderSchedule.nextTrigger(reminder, at("2026-11-12T00:00")))
    }

    @Test
    fun usesTheZoneOfNow() {
        val reminder = once("2026-11-20", 18, 0)
        val next = ReminderSchedule.nextTrigger(reminder, at("2026-11-12T07:00", berlin))!!
        assertEquals(berlin, next.zone)
        assertEquals(LocalDateTime.parse("2026-11-20T17:00").toInstant(ZoneOffset.UTC), next.toInstant())
    }

    @Test
    fun dst_berlinSpringForward_gapTimeMovesForward_andWeeklyKeepsWallClock() {
        // 2026-03-29 (Sunday): 02:00 CET -> 03:00 CEST, so 02:30 does not exist.
        val next = ReminderSchedule.nextTrigger(weekly(2, 30, SUNDAY), at("2026-03-28T12:00", berlin))!!
        assertEquals(LocalDateTime.parse("2026-03-29T03:30"), next.toLocalDateTime())
        assertEquals(ZoneOffset.ofHours(2), next.offset)

        // Weekly 07:00 on Mondays: 07:00 local on both sides of the change, 6 days 23 hours apart.
        val monday = weekly(7, 0, MONDAY)
        val before = ReminderSchedule.nextTrigger(monday, at("2026-03-20T12:00", berlin))!!
        val after = ReminderSchedule.nextTrigger(monday, before)!!
        assertEquals(at("2026-03-23T07:00", berlin), before)
        assertEquals(at("2026-03-30T07:00", berlin), after)
        assertEquals(ZoneOffset.ofHours(1), before.offset)
        assertEquals(ZoneOffset.ofHours(2), after.offset)
        assertEquals((7 * 24 - 1) * 3_600_000L, after.toInstant().toEpochMilli() - before.toInstant().toEpochMilli())
    }

    @Test
    fun dst_berlinFallBack_overlapFiresOnce() {
        // 2026-10-25 (Sunday): 03:00 CEST -> 02:00 CET, so 02:30 happens twice.
        val reminder = weekly(2, 30, SUNDAY)
        val first = ReminderSchedule.nextTrigger(reminder, at("2026-10-24T12:00", berlin))!!
        assertEquals(LocalDateTime.parse("2026-10-25T02:30"), first.toLocalDateTime())
        assertEquals(ZoneOffset.ofHours(2), first.offset)
        // After it fired (even during the repeated hour), the next one is a week later.
        val repeatedHour = ZonedDateTime.ofLocal(LocalDateTime.parse("2026-10-25T02:15"), berlin, ZoneOffset.ofHours(1))
        assertEquals(ZoneOffset.ofHours(1), repeatedHour.offset)
        assertEquals(at("2026-11-01T02:30", berlin), ReminderSchedule.nextTrigger(reminder, first))
        assertEquals(at("2026-11-01T02:30", berlin), ReminderSchedule.nextTrigger(reminder, repeatedHour.plusMinutes(30)))
    }

    @Test
    fun dst_saoPaulo2018_midnightGap_oneOffMovesToOneAm() {
        // Brazil's last DST start: 2018-11-04 00:00 -> 01:00, so midnight did not exist in Sao Paulo.
        val reminder = Reminder(id = 1, label = "x", minuteOfDay = 30, dateEpochDay = LocalDate.parse("2018-11-04").toEpochDay())
        val next = ReminderSchedule.nextTrigger(reminder, at("2018-11-03T20:00"))!!
        assertEquals(LocalDateTime.parse("2018-11-04T01:30"), next.toLocalDateTime())
        assertEquals(ZoneOffset.ofHours(-2), next.offset)
        // The same Sunday as a weekly reminder.
        assertEquals(next, ReminderSchedule.nextTrigger(weekly(0, 30, SUNDAY), at("2018-11-03T20:00")))
    }

    @Test
    fun minuteOfDay_outOfRange_isClamped() {
        val reminder = weekly(0, 0, SATURDAY).copy(minuteOfDay = 5_000)
        assertEquals(at("2026-11-14T23:59"), ReminderSchedule.nextTrigger(reminder, at("2026-11-12T07:00")))
    }

    @Test
    fun nextId_isAboveEveryExistingId() {
        assertEquals(1L, ReminderSchedule.nextId(emptyList()))
        val list = listOf(once("2026-11-20", 8, 0).copy(id = 4), once("2026-11-20", 9, 0).copy(id = 2))
        assertEquals(5L, ReminderSchedule.nextId(list))
    }
}
