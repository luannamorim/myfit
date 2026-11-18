package io.github.luannamorim.myfit.ui.feature.home

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.domain.history.HistoryCalculator
import io.github.luannamorim.myfit.domain.model.ThemeMode
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "pt-rBR-w384dp-h1600dp-xxhdpi")
class HomeWeekTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun week_hasARowPerKind_withAGreenCheckOnEachDayDone() {
        val today = LocalDate.of(2026, 11, 19) // Thursday
        val days = HistoryCalculator.weekOf(today).let { w -> (0L..6L).map { w.start.plusDays(it) } }
        // Mon: all three; Tue: food; Wed: workout + water; Thu: workout + food.
        val marks = days.mapIndexed { i, d ->
            DayMark(d, trained = i in setOf(0, 2, 3), isToday = d == today, ateOnTarget = i in setOf(0, 1, 3), drankOnTarget = i in setOf(0, 2))
        }
        compose.setContent {
            MyFitTheme(ThemeMode.DARK) {
                HomeContent(HomeUiState(loading = false, greeting = Greeting.EVENING, today = today, week = WeekStrip(marks, 3, 0)), HomeActions(), body = { BodyEmptyCard {} })
            }
        }
        assertEquals(3, compose.onAllNodesWithContentDescription("Treino:", substring = true, useUnmergedTree = true).fetchSemanticsNodes().size)
        assertEquals(3, compose.onAllNodesWithContentDescription("Calorias na meta:", substring = true, useUnmergedTree = true).fetchSemanticsNodes().size)
        assertEquals(2, compose.onAllNodesWithContentDescription("Água na meta:", substring = true, useUnmergedTree = true).fetchSemanticsNodes().size)
        // User request: no "3 treinos · 1 h 16 min" line under the week.
        compose.onNodeWithText("3 treinos", substring = true).assertDoesNotExist()
    }
}
