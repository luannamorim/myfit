package io.github.luannamorim.myfit.ui.feature.nutrition

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.domain.nutrition.MealReminder
import io.github.luannamorim.myfit.domain.nutrition.WaterReminder
import io.github.luannamorim.myfit.testutil.MainDispatcherRule
import io.github.luannamorim.myfit.testutil.awaitUntil
import io.github.luannamorim.myfit.testutil.testSettings
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "pt-rBR")
class NutritionRemindersViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val settings = testSettings()
    private fun vm() = NutritionRemindersViewModel(ApplicationProvider.getApplicationContext<Context>(), settings)
    private suspend fun stored() = settings.current().nutritionReminders

    @Test
    fun meals_addedInTimeOrder_withoutDuplicates_toggled_moved_andDeleted() = runBlocking {
        val vm = vm()
        vm.state.awaitUntil { !it.loading }
        vm.addMeal(12 * 60)
        vm.addMeal(7 * 60)
        vm.addMeal(12 * 60) // already there
        val meals = vm.state.awaitUntil { it.reminders.meals.size == 2 }.reminders.meals
        assertEquals(listOf(7 * 60, 12 * 60), meals.map { it.minuteOfDay })
        assertEquals(meals, stored().meals)

        val lunch = meals.first { it.minuteOfDay == 12 * 60 }
        vm.setMealEnabled(lunch.id, false)
        vm.setMealTime(lunch.id, 6 * 60) // moves before breakfast
        val moved = vm.state.awaitUntil { it.reminders.meals.first().id == lunch.id }.reminders.meals
        assertEquals(listOf(MealReminder(lunch.id, 6 * 60, enabled = false, label = lunch.label), meals.first { it.minuteOfDay == 7 * 60 }), moved)

        vm.deleteMeal(lunch.id)
        vm.state.awaitUntil { it.reminders.meals.size == 1 }
        assertEquals(listOf(7 * 60), stored().meals.map { it.minuteOfDay })
    }

    @Test
    fun newMeals_getANameByTheHour_andCanBeRenamed() = runBlocking {
        val vm = vm()
        vm.state.awaitUntil { !it.loading }
        listOf(7 * 60, 12 * 60, 16 * 60, 20 * 60).forEach { vm.addMeal(it) }
        val meals = vm.state.awaitUntil { it.reminders.meals.size == 4 }.reminders.meals
        assertEquals(listOf("Café da manhã", "Almoço", "Lanche", "Jantar"), meals.map { it.label })
        vm.setMealLabel(meals[2].id, "  Pré-treino ")
        vm.state.awaitUntil { it.reminders.meals[2].label == "Pré-treino" }
        assertEquals("Pré-treino", stored().meals[2].label)
    }

    @Test
    fun water_onIntervalAndWindow_keepTheWindowInOrder() = runBlocking {
        val vm = vm()
        vm.state.awaitUntil { !it.loading }
        vm.setWaterEnabled(true)
        vm.setWaterEvery(90)
        vm.setWaterFrom(23 * 60) // after the end: the end follows
        vm.state.awaitUntil { it.reminders.water.fromMinute == 23 * 60 }
        assertEquals(WaterReminder(enabled = true, everyMinutes = 90, fromMinute = 23 * 60, untilMinute = 23 * 60), stored().water)
        vm.setWaterUntil(7 * 60) // before the start: the start follows
        vm.state.awaitUntil { it.reminders.water.untilMinute == 7 * 60 }
        assertEquals(7 * 60, stored().water.fromMinute)
        assertTrue(stored().water.enabled)
    }
}
