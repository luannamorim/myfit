package io.github.luannamorim.myfit.ui.feature.routines

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.testing.invoke
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.core.time.FakeTimeSource
import io.github.luannamorim.myfit.data.repository.BackupTrigger
import io.github.luannamorim.myfit.data.repository.ExerciseRepository
import io.github.luannamorim.myfit.data.repository.RoutineRepository
import io.github.luannamorim.myfit.testutil.MainDispatcherRule
import io.github.luannamorim.myfit.testutil.awaitUntil
import io.github.luannamorim.myfit.testutil.inMemoryDb
import io.github.luannamorim.myfit.testutil.testSettings
import io.github.luannamorim.myfit.ui.navigation.RoutineEditorRoute
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class RoutineEditorViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val db = inMemoryDb(seed = true)
    private val time = FakeTimeSource()
    private val exercises = ExerciseRepository(db.exerciseDao(), time, BackupTrigger { })
    private val routines = RoutineRepository(db, time, BackupTrigger { })
    private val settings = testSettings()

    @After fun tearDown() = db.close()

    private fun vm(id: Long? = null) =
        RoutineEditorViewModel(SavedStateHandle(route = RoutineEditorRoute(id)), routines, exercises, settings)

    private suspend fun ids(n: Int) = db.exerciseDao().getAll().take(n).map { it.id }

    @Test
    fun save_withBlankName_fails() = runBlocking {
        val vm = vm()
        vm.state.awaitUntil { !it.isLoading }
        assertFalse(vm.save())
        assertTrue(vm.state.value.nameError)
    }

    @Test
    fun addExercises_usesDefaultRestFromSettings() = runBlocking {
        settings.update { it.copy(defaultRestSeconds = 120) }
        val vm = vm()
        vm.state.awaitUntil { !it.isLoading }
        vm.addExercises(ids(2))
        val state = vm.state.awaitUntil { it.items.size == 2 }
        assertTrue(state.items.all { it.restSeconds == 120 && it.sets == 3 && it.reps == 12 && it.loadKg == null })
    }

    @Test
    fun move_reordersItems_andSave_persistsOrder() = runBlocking {
        val picked = ids(3)
        val vm = vm()
        vm.state.awaitUntil { !it.isLoading }
        vm.addExercises(picked)
        vm.state.awaitUntil { it.items.size == 3 }
        vm.move(2, 0)
        vm.setName("Costas")
        vm.updateItem(0) { it.copy(loadKg = 42.5, sets = 99) }
        assertEquals(listOf(picked[2], picked[0], picked[1]), vm.state.value.items.map { it.exercise.id })
        assertTrue(vm.save())
        vm.state.awaitUntil { it.saved }
        val saved = routines.observeOverviews().awaitUntil { it.isNotEmpty() }.single()
        val routine = routines.get(saved.id)!!
        assertEquals(listOf(picked[2], picked[0], picked[1]), routine.items.map { it.exercise.id })
        assertEquals(42.5, routine.items[0].loadKg!!, 0.0)
        assertEquals(20, routine.items[0].sets) // clamped
    }

    @Test
    fun editExisting_loadsItems() = runBlocking {
        val picked = ids(2)
        val vm1 = vm()
        vm1.state.awaitUntil { !it.isLoading }
        vm1.addExercises(picked)
        vm1.state.awaitUntil { it.items.size == 2 }
        vm1.setName("Pernas")
        vm1.save()
        vm1.state.awaitUntil { it.saved }
        val id = routines.observeOverviews().awaitUntil { it.isNotEmpty() }.single().id
        val vm2 = vm(id)
        val state = vm2.state.awaitUntil { !it.isLoading }
        assertEquals("Pernas", state.name)
        assertEquals(picked, state.items.map { it.exercise.id })
        assertFalse(state.isNew)
    }
}
