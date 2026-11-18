package io.github.luannamorim.myfit.ui.feature.nutrition

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.testing.invoke
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.core.time.FakeTimeSource
import io.github.luannamorim.myfit.data.images.AiPhotoStore
import io.github.luannamorim.myfit.data.repository.BackupTrigger
import io.github.luannamorim.myfit.data.repository.NutritionRepository
import io.github.luannamorim.myfit.domain.nutrition.Meal
import io.github.luannamorim.myfit.testutil.MainDispatcherRule
import io.github.luannamorim.myfit.testutil.awaitUntil
import io.github.luannamorim.myfit.testutil.clearFileProviderCache
import io.github.luannamorim.myfit.testutil.inMemoryDb
import io.github.luannamorim.myfit.ui.navigation.MealEditorRoute
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Instant
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class MealEditorViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val db = inMemoryDb()
    private val time = FakeTimeSource()
    private val repo = NutritionRepository(db, time, BackupTrigger { })
    private val day = LocalDate.of(2026, 11, 17)
    private val newRoute = MealEditorRoute(dateEpochDay = day.toEpochDay())

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before fun resetFileProvider() = clearFileProviderCache()

    @After fun tearDown() = db.close()

    private fun editor(
        route: MealEditorRoute = newRoute,
        handle: SavedStateHandle = SavedStateHandle(route = route),
        io: CoroutineDispatcher = Dispatchers.Unconfined,
    ) = MealEditorViewModel(handle, repo, time, AiPhotoStore(context, io))

    private fun photo(): Uri {
        val bmp = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GREEN) }
        val file = File(context.cacheDir, "plate.jpg")
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        return Uri.fromFile(file)
    }

    private fun notAPhoto(): Uri = Uri.fromFile(File(context.cacheDir, "note.txt").apply { writeText("hello") })

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

    @Test
    fun doubleTapOnSave_savesTheMealOnce() = runBlocking {
        val vm = editor()
        vm.state.awaitUntil { !it.loading }
        vm.setKcal("320")
        vm.save()
        vm.save()
        vm.state.awaitUntil { it.done }
        assertEquals(1, repo.observeDay(day).first().meals.size)
    }

    @Test
    fun preparePhoto_success_exposesTheSharedUri() = runBlocking {
        val vm = editor()
        vm.state.awaitUntil { !it.loading }
        assertTrue(vm.state.value.canSendPhoto) // no description needed
        vm.preparePhoto(photo())
        val ready = vm.state.awaitUntil { it.photoToShare != null }
        assertEquals("${context.packageName}.camera", ready.photoToShare!!.authority)
        assertFalse(ready.preparingPhoto)
        assertFalse(ready.photoFailed)
        vm.onPhotoHandled()
        assertNull(vm.state.value.photoToShare)
    }

    @Test
    fun preparePhoto_failure_flagsErrorAndReenables() = runBlocking {
        val vm = editor()
        vm.state.awaitUntil { !it.loading }
        vm.preparePhoto(notAPhoto())
        val failed = vm.state.awaitUntil { it.photoFailed }
        assertTrue(failed.canSendPhoto)
        assertNull(failed.photoToShare)
        vm.onPhotoHandled()
        assertFalse(vm.state.value.photoFailed)
    }

    @Test
    fun preparePhoto_ignoresASecondCallWhileBusy() = runBlocking {
        val io = StandardTestDispatcher()
        val vm = editor(io = io)
        vm.state.awaitUntil { !it.loading }
        vm.preparePhoto(photo())
        assertTrue(vm.state.value.preparingPhoto)
        assertFalse(vm.state.value.canSendPhoto)
        vm.preparePhoto(notAPhoto()) // would end in photoFailed if it ran
        io.scheduler.advanceUntilIdle()
        val done = vm.state.awaitUntil { !it.preparingPhoto }
        assertTrue(done.photoToShare != null)
        assertFalse(done.photoFailed)
    }
}
