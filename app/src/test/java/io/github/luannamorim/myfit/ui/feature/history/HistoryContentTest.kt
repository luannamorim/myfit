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

    private fun show(period: SummaryPeriod, dayNutrition: NutritionStatus?, periodNutrition: List<NutritionStatus>) = compose.setContent {
        MyFitTheme(ThemeMode.DARK) {
            HistoryContent(
                state = HistoryUiState(
                    month = YearMonth.of(2026, 11), today = day, selected = day, period = period,
                    summaryRange = if (period == SummaryPeriod.WEEK) week else HistoryCalculator.monthOf(YearMonth.of(2026, 11)),
                    dayNutrition = dayNutrition, periodNutrition = periodNutrition, isLoading = false,
                ),
                onSelect = {}, onMonthChange = {}, onPeriod = {}, onOpenSession = {},
            )
        }
    }

    @Test
    fun day_showsItsNutritionAgainstTheTargets() {
        show(SummaryPeriod.WEEK, status(day, 2900, 2410), emptyList())
        compose.onNodeWithText("2,9 / 2,8 L").assertIsDisplayed()
        compose.onNodeWithText("2.410 / 2.300 kcal").assertIsDisplayed()
        compose.onNodeWithText("Nada registrado neste dia.").assertDoesNotExist() // nutrition counts as something logged
    }

    @Test
    fun week_countsTheDaysOnTarget_amongTheLoggedOnes() {
        val days = (0L..6L).map { week.start.plusDays(it) }
        val list = days.mapIndexed { i, d ->
            when (i) {
                0 -> status(d, 3000, 1500) // water hit, kcal missed
                1 -> status(d, 2900, 2300) // both hit
                2 -> status(d, 1000, 2400) // water missed, kcal hit
                else -> status(d, 0, null) // nothing logged
            }
        }
        show(SummaryPeriod.WEEK, null, list)
        compose.onNodeWithText("Nutrição").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithText("2/3").assertCountEquals(2)
    }

    @Test
    fun month_saysHowManyDaysOnTarget() {
        val month = HistoryCalculator.monthOf(YearMonth.of(2026, 11))
        val list = (0L until 30L).map { month.start.plusDays(it) }.mapIndexed { i, d -> if (i < 4) status(d, if (i < 3) 3000 else 500, null) else status(d, 0, null) }
        show(SummaryPeriod.MONTH, null, list)
        compose.onNodeWithText("3 de 4 dias").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun nothingLoggedInThePeriod_hidesTheCard() {
        show(SummaryPeriod.WEEK, null, (0L..6L).map { status(week.start.plusDays(it), 0, null) })
        compose.onNodeWithText("Nutrição").assertDoesNotExist()
    }
}
