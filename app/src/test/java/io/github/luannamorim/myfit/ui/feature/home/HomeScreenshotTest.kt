package io.github.luannamorim.myfit.ui.feature.home

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.domain.history.SessionSummary
import io.github.luannamorim.myfit.domain.model.RoutineOverview
import io.github.luannamorim.myfit.domain.history.HistoryCalculator
import io.github.luannamorim.myfit.testutil.PHONE_TALL
import io.github.luannamorim.myfit.testutil.snapBothThemes
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = PHONE_TALL)
class HomeScreenshotTest {
    @get:Rule val compose = createComposeRule()
    private val today = LocalDate.of(2026, 11, 12)

    @Test
    fun home_empty() = compose.snapBothThemes("home_empty") {
        HomeContent(HomeUiState(loading = false, greeting = Greeting.MORNING, today = today, week = week(emptySet(), 0, 0)), HomeActions(), body = { BodyEmptyCard {} })
    }

    @Test
    fun home_data() = compose.snapBothThemes("home_data") {
        val routines = listOf(
            RoutineOverview(1, "Back day", null, 6, today.toEpochDay() - 3),
            RoutineOverview(2, "Chest & triceps", null, 7, today.toEpochDay() - 1),
            RoutineOverview(3, "Legs", null, 5, today.toEpochDay() - 5),
        )
        HomeContent(
            HomeUiState(
                loading = false,
                greeting = Greeting.EVENING,
                today = today,
                suggested = routines[2],
                routines = routines,
                last = SessionSummary(9, "Chest & triceps", today.minusDays(1), LocalTime.of(19, 5), 61 * 60_000L, 14, listOf("Bench press")),
                week = week(setOf(today.minusDays(3), today.minusDays(1)), 2, 113 * 60_000L),
            ),
            HomeActions(),
            body = { BodyEmptyCard {} },
        )
    }

    private fun week(trained: Set<LocalDate>, sessions: Int, ms: Long): WeekStrip {
        val range = HistoryCalculator.weekOf(today)
        return WeekStrip((0L..6L).map { range.start.plusDays(it) }.map { DayMark(it, it in trained, it == today) }, sessions, ms)
    }
}
