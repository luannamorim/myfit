package io.github.luannamorim.myfit.domain.history

import io.github.luannamorim.myfit.domain.model.ItemStatus
import io.github.luannamorim.myfit.domain.session.SessionSet
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** A finished session as shown in history. [startDate] is the local day it started (crossing midnight keeps it). */
data class SessionSummary(
    val id: Long,
    val routineName: String,
    val startDate: LocalDate,
    val startTime: LocalTime,
    val durationMs: Long,
    val setsDone: Int,
    val exercises: List<String>,
)

data class ExerciseFrequency(val name: String, val sessions: Int)

data class PeriodSummary(
    val sessions: Int,
    val totalDurationMs: Long,
    /** Average sessions per week over the period. */
    val avgPerWeek: Double,
    val exercises: List<ExerciseFrequency>,
)

data class SessionExerciseDetail(val name: String, val status: ItemStatus, val sets: List<SessionSet>)

data class SessionDetail(val summary: SessionSummary, val exercises: List<SessionExerciseDetail>)

object HistoryCalculator {

    fun summarize(items: List<SessionSummary>, range: ClosedRange<LocalDate>): PeriodSummary {
        val inRange = items.filter { it.startDate in range }
        val days = ChronoUnit.DAYS.between(range.start, range.endInclusive) + 1
        val weeks = days / 7.0
        val frequency = inRange
            .flatMap { s -> s.exercises.distinct() }
            .groupingBy { it }
            .eachCount()
            .map { (name, count) -> ExerciseFrequency(name, count) }
            .sortedWith(compareByDescending<ExerciseFrequency> { it.sessions }.thenBy { it.name })
        return PeriodSummary(
            sessions = inRange.size,
            totalDurationMs = inRange.sumOf { it.durationMs },
            avgPerWeek = if (weeks > 0) inRange.size / weeks else 0.0,
            exercises = frequency,
        )
    }

    fun daysWithSessions(items: List<SessionSummary>): Set<LocalDate> = items.mapTo(mutableSetOf()) { it.startDate }

    /** Monday–Sunday week containing [date]. */
    fun weekOf(date: LocalDate): ClosedRange<LocalDate> {
        val start = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return start..start.plusDays(6)
    }

    fun monthOf(month: YearMonth): ClosedRange<LocalDate> = month.atDay(1)..month.atEndOfMonth()

    /** Full Monday–Sunday weeks covering the month, as displayed by the calendar grid. */
    fun calendarGrid(month: YearMonth): ClosedRange<LocalDate> {
        val start = month.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val end = month.atEndOfMonth().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        return start..end
    }
}
