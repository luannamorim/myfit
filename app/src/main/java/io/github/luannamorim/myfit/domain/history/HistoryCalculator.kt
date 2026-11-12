package io.github.luannamorim.myfit.domain.history

import io.github.luannamorim.myfit.domain.activity.Activity
import io.github.luannamorim.myfit.domain.activity.Sport
import io.github.luannamorim.myfit.domain.model.Intensity
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
    val intensity: Intensity? = null,
    val imagePath: String? = null,
)

data class ExerciseFrequency(val name: String, val sessions: Int)

/** How often a sport was logged; [name] tells apart the user's own "free" activities. */
data class SportFrequency(val sport: Sport, val name: String?, val times: Int)

data class PeriodSummary(
    val sessions: Int,
    /** Workouts plus logged activities. */
    val totalDurationMs: Long,
    /** Average workouts + activities per week over the period. */
    val avgPerWeek: Double,
    val exercises: List<ExerciseFrequency>,
    val activities: Int = 0,
    val sports: List<SportFrequency> = emptyList(),
)

data class SessionExerciseDetail(val name: String, val status: ItemStatus, val sets: List<SessionSet>)

data class SessionDetail(val summary: SessionSummary, val exercises: List<SessionExerciseDetail>)

object HistoryCalculator {

    fun summarize(items: List<SessionSummary>, range: ClosedRange<LocalDate>, activities: List<Activity> = emptyList()): PeriodSummary {
        val inRange = items.filter { it.startDate in range }
        val activitiesInRange = activities.filter { it.date in range }
        val days = ChronoUnit.DAYS.between(range.start, range.endInclusive) + 1
        val weeks = days / 7.0
        val frequency = inRange
            .flatMap { s -> s.exercises.distinct() }
            .groupingBy { it }
            .eachCount()
            .map { (name, count) -> ExerciseFrequency(name, count) }
            .sortedWith(compareByDescending<ExerciseFrequency> { it.sessions }.thenBy { it.name })
        val sports = activitiesInRange
            .groupingBy { a -> a.sport to a.name?.trim()?.lowercase()?.takeIf { a.sport == Sport.OTHER } }
            .eachCount()
            .map { (key, count) ->
                val name = if (key.first == Sport.OTHER) activitiesInRange.first { it.sport == Sport.OTHER && it.name?.trim()?.lowercase() == key.second }.name?.trim() else null
                SportFrequency(key.first, name, count)
            }
            .sortedWith(compareByDescending<SportFrequency> { it.times }.thenBy { it.sport.ordinal }.thenBy { it.name })
        val count = inRange.size + activitiesInRange.size
        return PeriodSummary(
            sessions = inRange.size,
            totalDurationMs = inRange.sumOf { it.durationMs } + activitiesInRange.sumOf { it.durationMs },
            avgPerWeek = if (weeks > 0) count / weeks else 0.0,
            exercises = frequency,
            activities = activitiesInRange.size,
            sports = sports,
        )
    }

    fun daysWithSessions(items: List<SessionSummary>): Set<LocalDate> = items.mapTo(mutableSetOf()) { it.startDate }

    /** Days with a workout or a logged activity (calendar marks and the Home week). */
    fun activeDays(items: List<SessionSummary>, activities: List<Activity>): Set<LocalDate> =
        daysWithSessions(items) + activities.map { it.date }

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
