package io.github.luannamorim.myfit.domain.history

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

class HistoryCalculatorTest {
    private fun s(id: Long, date: LocalDate, minutes: Long, vararg exercises: String, time: LocalTime = LocalTime.of(18, 0)) =
        SessionSummary(id, "Treino", date, time, minutes * 60_000, 9, exercises.toList())

    private val monday = LocalDate.of(2026, 11, 9)

    @Test
    fun sessionCrossingMidnight_countsOnStartDate() {
        val late = s(1, monday, 70, "Remada", time = LocalTime.of(23, 30))
        assertEquals(1, HistoryCalculator.summarize(listOf(late), monday..monday).sessions)
        assertEquals(0, HistoryCalculator.summarize(listOf(late), monday.plusDays(1)..monday.plusDays(1)).sessions)
        assertEquals(setOf(monday), HistoryCalculator.daysWithSessions(listOf(late)))
    }

    @Test
    fun summarize_week_countsAndTotals() {
        val items = listOf(
            s(1, monday, 60, "Remada", "Pullover"),
            s(2, monday.plusDays(2), 45, "Supino"),
            s(3, monday.plusDays(7), 50, "Remada"), // next week
        )
        val week = HistoryCalculator.summarize(items, HistoryCalculator.weekOf(monday.plusDays(3)))
        assertEquals(2, week.sessions)
        assertEquals(105 * 60_000L, week.totalDurationMs)
        assertEquals(2.0, week.avgPerWeek, 0.001)
    }

    @Test
    fun avgPerWeek_forMonth_of30Days() {
        val month = YearMonth.of(2026, 11)
        val items = (1..12).map { s(it.toLong(), month.atDay(it * 2), 50, "Remada") }
        val summary = HistoryCalculator.summarize(items, HistoryCalculator.monthOf(month))
        assertEquals(12, summary.sessions)
        assertEquals(12 / (30 / 7.0), summary.avgPerWeek, 0.001)
    }

    @Test
    fun exercises_sortedByFrequencyThenName() {
        val items = listOf(
            s(1, monday, 60, "Remada", "Pullover", "Remada"),
            s(2, monday, 60, "Pullover", "Agachamento"),
            s(3, monday, 60, "Pullover"),
        )
        val ex = HistoryCalculator.summarize(items, monday..monday).exercises
        assertEquals(listOf("Pullover" to 3, "Agachamento" to 1, "Remada" to 1), ex.map { it.name to it.sessions })
    }

    @Test
    fun weekOf_sunday_returnsPreviousMonday() {
        val sunday = LocalDate.of(2026, 11, 15)
        assertEquals(monday..sunday, HistoryCalculator.weekOf(sunday))
    }

    @Test
    fun calendarGrid_coversFullWeeks() {
        val grid = HistoryCalculator.calendarGrid(YearMonth.of(2026, 11))
        assertEquals(LocalDate.of(2026, 10, 26), grid.start) // Nov 1, 2026 is a Sunday
        assertEquals(LocalDate.of(2026, 12, 6), grid.endInclusive)
    }
}
