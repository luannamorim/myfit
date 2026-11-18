package io.github.luannamorim.myfit.ui.feature.history

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import io.github.luannamorim.myfit.domain.nutrition.DailyTargets
import io.github.luannamorim.myfit.domain.nutrition.Meal
import io.github.luannamorim.myfit.domain.nutrition.NutritionDay
import io.github.luannamorim.myfit.domain.nutrition.NutritionGoal
import io.github.luannamorim.myfit.domain.nutrition.NutritionStatus
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

    private val day = LocalDate.of(2026, 11, 17) // a Tuesday
    private val week = HistoryCalculator.weekOf(day)
    private fun status(date: LocalDate, waterMl: Int, kcal: Int?) = NutritionStatus(
        NutritionDay(date, kcal?.let { listOf(Meal(1, date, 600, null, it)) }.orEmpty(), waterMl, if (waterMl > 0) 1 else 0),
        DailyTargets(2300, 2800, emptySet()),
        NutritionGoal.MAINTAIN,
    )

    private fun show(period: SummaryPeriod, dayNutrition: NutritionStatus?) = compose.setContent {
        MyFitTheme(ThemeMode.DARK) {
            HistoryContent(
                state = HistoryUiState(
                    month = YearMonth.of(2026, 11), today = day, selected = day, period = period,
                    summaryRange = if (period == SummaryPeriod.WEEK) week else HistoryCalculator.monthOf(YearMonth.of(2026, 11)),
                    dayNutrition = dayNutrition, isLoading = false,
                ),
                onSelect = {}, onMonthChange = {}, onPeriod = {}, onOpenSession = {},
            )
        }
    }

    @Test
    fun day_showsItsNutritionAgainstTheTargets() {
        show(SummaryPeriod.WEEK, status(day, 2900, 2410))
        compose.onNodeWithText("2,9 / 2,8 L").assertIsDisplayed()
        compose.onNodeWithText("2.410 / 2.300 kcal").assertIsDisplayed()
        compose.onNodeWithText("Nada registrado neste dia.").assertDoesNotExist() // nutrition counts as something logged
    }

    @Test
    fun calendar_marksWorkoutFoodAndWaterPerDay_withALegend() {
        compose.setContent {
            MyFitTheme(ThemeMode.DARK) {
                HistoryContent(
                    state = HistoryUiState(
                        month = YearMonth.of(2026, 11), today = day, selected = day,
                        marked = setOf(day), foodOnTarget = setOf(day), waterOnTarget = setOf(day, day.minusDays(1)),
                        summaryRange = week, isLoading = false,
                    ),
                    onSelect = {}, onMonthChange = {}, onPeriod = {}, onOpenSession = {},
                )
            }
        }
        compose.onNodeWithContentDescription("com treino, calorias na meta, água na meta", substring = true).assertExists()
        compose.onNodeWithContentDescription("segunda-feira, 16 de novembro, água na meta", substring = true).assertExists()
        compose.onNodeWithText("comida").assertIsDisplayed()
        compose.onNodeWithText("água").assertIsDisplayed()
    }
}
