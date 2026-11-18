package io.github.luannamorim.myfit.ui.feature.nutrition

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.domain.model.ThemeMode
import io.github.luannamorim.myfit.domain.nutrition.DailyTargets
import io.github.luannamorim.myfit.domain.nutrition.MealReminder
import io.github.luannamorim.myfit.domain.nutrition.NutritionDay
import io.github.luannamorim.myfit.domain.nutrition.NutritionReminders
import io.github.luannamorim.myfit.domain.nutrition.WaterReminder
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "pt-rBR-w360dp-h1600dp-xxhdpi")
class NutritionRemindersContentTest {
    @get:Rule val compose = createComposeRule()

    private fun show(reminders: NutritionReminders, actions: NutritionRemindersActions = NutritionRemindersActions(), notifications: Boolean = true) =
        compose.setContent {
            MyFitTheme(ThemeMode.DARK) {
                NutritionRemindersContent(NutritionRemindersUiState(loading = false, reminders = reminders, notificationsEnabled = notifications), actions, is24Hour = true)
            }
        }

    @Test
    fun empty_explainsAndOffersATime() {
        show(NutritionReminders())
        compose.onNodeWithText("Lembretes de nutrição").assertIsDisplayed()
        compose.onNodeWithText("Nenhum horário de refeição.").assertIsDisplayed()
        compose.onNodeWithText("Adicionar horário").assertIsDisplayed()
        compose.onNodeWithText("Lembrar de beber água").assertIsDisplayed()
    }

    @Test
    fun mealsAndWater_showTheirSettings_andChangesGoToTheActions() {
        val every = mutableListOf<Int>()
        val toggled = mutableListOf<Pair<Long, Boolean>>()
        val deleted = mutableListOf<Long>()
        show(
            NutritionReminders(
                meals = listOf(MealReminder(1, 7 * 60), MealReminder(2, 12 * 60 + 30, enabled = false)),
                water = WaterReminder(enabled = true, everyMinutes = 120, fromMinute = 8 * 60, untilMinute = 22 * 60),
            ),
            NutritionRemindersActions(onWaterEvery = { every += it }, onMealEnabled = { id, on -> toggled += id to on }, onDeleteMeal = { deleted += it }),
        )
        compose.onNodeWithText("07:00").assertIsDisplayed()
        compose.onNodeWithText("12:30").assertIsDisplayed()
        compose.onNodeWithContentDescription("Refeição às 12:30").performClick() // its switch
        compose.onNodeWithContentDescription("Excluir 07:00").performClick()
        assertEquals(listOf(2L to true), toggled)
        assertEquals(listOf(1L), deleted)

        compose.onNodeWithText("2 h").performScrollTo().assertIsSelected()
        compose.onNodeWithText("1 h 30 min").performClick()
        assertEquals(listOf(90), every)
        compose.onNodeWithText("08:00").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("22:00").assertIsDisplayed()
        compose.onNodeWithText("8 avisos por dia. Param quando você bate a meta de água.").assertIsDisplayed()
    }

    @Test
    fun mealName_isShown_andTappingItRenames() {
        val renamed = mutableListOf<Pair<Long, String>>()
        show(
            NutritionReminders(meals = listOf(MealReminder(1, 12 * 60 + 30, label = "Almoço"))),
            NutritionRemindersActions(onMealLabel = { id, name -> renamed += id to name }),
        )
        compose.onNodeWithText("Almoço").performClick()
        compose.onNodeWithText("Nome da refeição").performTextClearance()
        compose.onNodeWithText("Nome da refeição").performTextInput("Almoço leve")
        compose.onNodeWithText("OK").performClick()
        assertEquals(listOf(1L to "Almoço leve"), renamed)
    }

    @Test
    fun notificationsOff_warns() {
        show(NutritionReminders(meals = listOf(MealReminder(1, 7 * 60))), notifications = false)
        compose.onNodeWithText("Notificações desligadas: os lembretes não vão aparecer.", substring = true).assertIsDisplayed()
    }

    @Test
    fun nutritionTab_bellOpensTheReminders() {
        var opened = 0
        val today = LocalDate.of(2026, 11, 17)
        compose.setContent {
            MyFitTheme(ThemeMode.DARK) {
                NutritionContent(
                    NutritionUiState(loading = false, date = today, today = today, day = NutritionDay(today, emptyList(), 0, 0), targets = DailyTargets(null, null, emptySet())),
                    NutritionActions(onOpenReminders = { opened++ }),
                )
            }
        }
        compose.onNodeWithContentDescription("Lembretes de nutrição").performClick()
        assertEquals(1, opened)
    }
}
