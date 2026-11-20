package io.github.luannamorim.myfit.ui.feature.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.domain.history.HistoryCalculator
import io.github.luannamorim.myfit.domain.activity.Sport
import io.github.luannamorim.myfit.domain.model.RoutineOverview
import io.github.luannamorim.myfit.domain.model.ThemeMode
import io.github.luannamorim.myfit.domain.nutrition.DailyTargets
import io.github.luannamorim.myfit.domain.nutrition.Meal
import io.github.luannamorim.myfit.domain.nutrition.NutritionDay
import io.github.luannamorim.myfit.domain.nutrition.NutritionGoal
import io.github.luannamorim.myfit.domain.nutrition.NutritionStatus
import io.github.luannamorim.myfit.testutil.PHONE_TALL
import io.github.luannamorim.myfit.testutil.snapBothThemes
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = PHONE_TALL)
class HomeScreenshotTest {
    @get:Rule val compose = createComposeRule()
    private val today = LocalDate.of(2026, 11, 12)

    @Test
    fun header_hasOnlyTheSettingsGear_theRemindersBellMovedToWorkouts() {
        var opened = 0
        compose.setContent {
            MyFitTheme(ThemeMode.DARK) {
                HomeContent(
                    HomeUiState(loading = false, greeting = Greeting.MORNING, today = today, week = week(emptySet(), 0, 0)),
                    HomeActions(onOpenSettings = { opened++ }),
                    body = { BodyEmptyCard {} },
                )
            }
        }
        compose.onNodeWithContentDescription("Reminders").assertDoesNotExist()
        compose.onNodeWithContentDescription("Settings").performClick()
        assertEquals(1, opened)
    }

    @Test
    fun home_empty() = compose.snapBothThemes("home_empty") {
        HomeContent(HomeUiState(loading = false, greeting = Greeting.MORNING, today = today, week = week(emptySet(), 0, 0)), HomeActions(), body = { BodyEmptyCard {} })
    }

    @Test
    fun home_next_activity() = compose.snapBothThemes("home_next_activity") {
        HomeContent(
            HomeUiState(
                loading = false,
                greeting = Greeting.MORNING,
                today = today,
                routines = listOf(RoutineOverview(1, "Back day", null, 6, today.toEpochDay() - 3)),
                suggestedActivity = ActivityTarget(Sport.SWIMMING, null, today.toEpochDay() - 3),
                suggestedAt = today.atTime(18, 30),
                week = week(emptySet(), 0, 0),
            ),
            HomeActions(),
            body = { BodyEmptyCard {} },
        )
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
                week = week(setOf(today.minusDays(3), today.minusDays(1)), 2, 113 * 60_000L),
                nutrition = NutritionStatus(
                    NutritionDay(today, listOf(Meal(1, today, 12 * 60, "Lunch", 1880)), 1500, 6),
                    DailyTargets(2300, 2800, emptySet()),
                    NutritionGoal.MAINTAIN,
                ),
            ),
            HomeActions(),
            body = { BodyEmptyCard {} },
        )
    }

    @Test
    fun nextWorkout_showsWhenTheReminderIsDue() {
        val routine = RoutineOverview(3, "Legs", null, 5, today.toEpochDay() - 5)
        compose.setContent {
            MyFitTheme(ThemeMode.DARK) {
                HomeContent(
                    HomeUiState(
                        loading = false, today = today, suggested = routine, suggestedAt = today.plusDays(1).atTime(7, 30),
                        routines = listOf(routine), week = week(emptySet(), 0, 0),
                    ),
                    HomeActions(),
                    body = { BodyEmptyCard {} },
                )
            }
        }
        compose.onNodeWithText("Tomorrow at", substring = true).assertIsDisplayed()
    }

    @Test
    fun home_scheduled() = compose.snapBothThemes("home_scheduled") {
        val routines = listOf(
            RoutineOverview(1, "Back day", null, 6, today.toEpochDay() - 3),
            RoutineOverview(2, "Chest & triceps", null, 7, today.toEpochDay() - 1),
        )
        HomeContent(
            HomeUiState(
                loading = false, greeting = Greeting.MORNING, today = today,
                suggested = routines[1], suggestedAt = today.atTime(18, 30), routines = routines,
                week = week(setOf(today.minusDays(1)), 1, 61 * 60_000L),
            ),
            HomeActions(),
            body = { BodyEmptyCard {} },
        )
    }

    private fun week(trained: Set<LocalDate>, sessions: Int, ms: Long): WeekStrip {
        val range = HistoryCalculator.weekOf(today)
        // Food and water on target on most past days, so the screenshot shows the three rows.
        return WeekStrip(
            (0L..6L).map { range.start.plusDays(it) }.map {
                DayMark(it, it in trained, it == today, ateOnTarget = !it.isAfter(today) && it.dayOfMonth % 3 != 0, drankOnTarget = !it.isAfter(today) && it.dayOfMonth % 2 == 0)
            },
            sessions,
            ms,
        )
    }
}
