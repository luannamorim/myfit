package io.github.luannamorim.myfit.ui.feature.routines

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.core.time.FakeTimeSource
import io.github.luannamorim.myfit.data.repository.BackupTrigger
import io.github.luannamorim.myfit.data.repository.RoutineRepository
import io.github.luannamorim.myfit.testutil.MainDispatcherRule
import io.github.luannamorim.myfit.testutil.awaitUntil
import io.github.luannamorim.myfit.testutil.inMemoryDb
import io.github.luannamorim.myfit.testutil.insertRoutine
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class RoutinesViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val db = inMemoryDb(seed = true)
    private val routines = RoutineRepository(db, FakeTimeSource(), BackupTrigger { })

    @After fun tearDown() = db.close()

    @Test
    fun duplicate_addsCopyToList() = runBlocking {
        val ex = db.exerciseDao().getAll().first()
        val id = db.insertRoutine("Costas", listOf(ex.id))
        val vm = RoutinesViewModel(routines, FakeTimeSource())
        vm.state.awaitUntil { it.routines.size == 1 }
        vm.duplicate(id, "(cópia)")
        val state = vm.state.awaitUntil { it.routines.size == 2 }
        assertEquals(listOf("Costas", "Costas (cópia)"), state.routines.map { it.name })
    }

    @Test
    fun delete_removesFromList() = runBlocking {
        val ex = db.exerciseDao().getAll().first()
        val id = db.insertRoutine("Costas", listOf(ex.id))
        val vm = RoutinesViewModel(routines, FakeTimeSource())
        vm.state.awaitUntil { it.routines.size == 1 }
        vm.delete(id)
        val state = vm.state.awaitUntil { it.routines.isEmpty() && !it.isLoading }
        assertEquals(0, state.routines.size)
    }
}
