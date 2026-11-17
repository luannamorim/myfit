package io.github.luannamorim.myfit.ui.feature.nutrition

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.testing.invoke
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.core.time.FakeTimeSource
import io.github.luannamorim.myfit.data.repository.BackupTrigger
import io.github.luannamorim.myfit.data.repository.NutritionRepository
import io.github.luannamorim.myfit.domain.nutrition.Meal
import io.github.luannamorim.myfit.testutil.MainDispatcherRule
import io.github.luannamorim.myfit.testutil.awaitUntil
import io.github.luannamorim.myfit.testutil.inMemoryDb
import io.github.luannamorim.myfit.ui.navigation.MealEditorRoute
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class MealEditorViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val db = inMemoryDb()
    private val time = FakeTimeSource()
    private val repo = NutritionRepository(db, time, BackupTrigger { })
    private val day = LocalDate.of(2026, 11, 17)
    private val newRoute = MealEditorRoute(dateEpochDay = day.toEpochDay())

    @After fun tearDown() = db.close()

    private fun editor(route: MealEditorRoute = newRoute, handle: SavedStateHandle = SavedStateHandle(route = route)) =
        MealEditorViewModel(handle, repo, time)

    private fun nowMinute(): Int = Instant.ofEpochMilli(time.now().wallMs).atZone(time.zone()).toLocalTime().let { it.hour * 60 + it.minute }

    @Test
    fun newMeal_startsNow_needsValidKcal_andSavesOnTheRouteDay() = runBlocking {
        val vm = editor()
        val start = vm.state.awaitUntil { !it.loading }
        assertTrue(start.isNew)
        assertEquals(nowMinute(), start.minuteOfDay)
        assertFalse(start.canSave)
        assertFalse(start.canAsk) // nothing to ask about yet

        vm.setKcal("0")
        assertFalse(vm.state.value.canSave)
        vm.setKcal("4a5x0") // only digits
        assertEquals("450", vm.state.value.kcalText)
        vm.setKcal("123456") // at most 4 digits
        assertEquals("1234", vm.state.value.kcalText)
        vm.setKcal("450")
        vm.setDescription("Arroz e feijão")
        vm.setMinute(12 * 60 + 30)
        assertTrue(vm.state.value.canSave)
        assertTrue(vm.state.value.canAsk)

        vm.save()
        vm.state.awaitUntil { it.done }
        val meal = repo.observeDay(day).first().meals.single()
        assertEquals("Arroz e feijão", meal.description)
        assertEquals(450, meal.kcal)
        assertEquals(12 * 60 + 30, meal.minuteOfDay)
    }

    @Test
    fun existingMeal_loads_edits_andDeletes() = runBlocking {
        val id = repo.saveMeal(Meal(date = day, minuteOfDay = 480, description = "Pão", kcal = 200))
        val vm = editor(MealEditorRoute(mealId = id, dateEpochDay = day.toEpochDay()))
        val loaded = vm.state.awaitUntil { !it.loading }
        assertFalse(loaded.isNew)
        assertEquals("Pão", loaded.description)
        assertEquals("200", loaded.kcalText)
        assertEquals(480, loaded.minuteOfDay)

        vm.setKcal("250")
        vm.save()
        vm.state.awaitUntil { it.done }
        assertEquals(250, repo.getMeal(id)!!.kcal)

        val again = editor(MealEditorRoute(mealId = id, dateEpochDay = day.toEpochDay()))
        again.state.awaitUntil { !it.loading }
        again.delete()
        again.state.awaitUntil { it.done }
        assertNull(repo.getMeal(id))
    }

    @Test
    fun draft_survivesTheProcessBeingKilled_whileInTheAiApp() = runBlocking {
        val handle = SavedStateHandle(route = newRoute)
        val first = editor(handle = handle)
        first.state.awaitUntil { !it.loading }
        first.setDescription("Arroz, feijão e frango")
        first.setKcal("610")
        first.setMinute(13 * 60)

        // Android recreates the screen with the saved state.
        val restored = editor(handle = handle).state.awaitUntil { !it.loading }
        assertEquals("Arroz, feijão e frango", restored.description)
        assertEquals("610", restored.kcalText)
        assertEquals(13 * 60, restored.minuteOfDay)
    }
}
