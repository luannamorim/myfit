package io.github.luannamorim.myfit.ui.feature.history

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.domain.activity.Sport
import io.github.luannamorim.myfit.domain.history.ExerciseFrequency
import io.github.luannamorim.myfit.domain.history.HistoryCalculator
import io.github.luannamorim.myfit.domain.history.PeriodSummary
import io.github.luannamorim.myfit.domain.history.SportFrequency
import io.github.luannamorim.myfit.domain.model.ThemeMode
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "pt-rBR-w384dp-h1600dp-xxhdpi")
class HistoryContentTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun noExercisesDoneCard_andNoAddActivityButton_sportsStay() {
        val today = LocalDate.of(2026, 11, 17)
        compose.setContent {
            MyFitTheme(ThemeMode.DARK) {
                HistoryContent(
                    state = HistoryUiState(
                        month = YearMonth.of(2026, 11),
                        today = today,
                        selected = today,
                        summary = PeriodSummary(
                            3, 120 * 60_000L, 3.0, listOf(ExerciseFrequency("Remada curvada", 2)),
                            activities = 1, sports = listOf(SportFrequency(Sport.SWIMMING, null, 1)),
                        ),
                        summaryRange = HistoryCalculator.weekOf(today),
                        isLoading = false,
                    ),
                    onSelect = {}, onMonthChange = {}, onPeriod = {}, onOpenSession = {},
                )
            }
        }
        compose.onNodeWithText("Exercícios realizados").assertDoesNotExist()
        compose.onNodeWithText("Remada curvada").assertDoesNotExist()
        compose.onNodeWithText("+ Atividade").assertDoesNotExist()
        compose.onNodeWithText("Esportes").performScrollTo().assertIsDisplayed()
    }
}
