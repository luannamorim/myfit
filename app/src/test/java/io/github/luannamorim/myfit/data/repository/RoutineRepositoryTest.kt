package io.github.luannamorim.myfit.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.core.time.FakeTimeSource
import io.github.luannamorim.myfit.domain.model.MuscleGroup
import io.github.luannamorim.myfit.domain.model.Routine
import io.github.luannamorim.myfit.domain.model.RoutineItem
import io.github.luannamorim.myfit.testutil.inMemoryDb
import io.github.luannamorim.myfit.testutil.insertRoutine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class RoutineRepositoryTest {
    private val db = inMemoryDb(seed = true)
    private val time = FakeTimeSource()
    private var backupCalls = 0
    private val trigger = BackupTrigger { backupCalls++ }
    private val exercises = ExerciseRepository(db.exerciseDao(), time, trigger)
    private val routines = RoutineRepository(db, time, trigger)

    @After fun tearDown() = db.close()

    private suspend fun byName(query: String) = exercises.observeLibrary(query).first().first()

    @Test
    fun save_assignsContiguousPositions_andKeepsOrder() = runTest {
        val a = byName("remada curvada")
        val b = byName("pullover")
        val c = byName("puxada frontal")
        val id = routines.save(
            Routine(
                name = "  Costas ",
                items = listOf(c, a, b).map { RoutineItem(exercise = it, sets = 3, reps = 12, restSeconds = 90) },
            ),
        )
        val loaded = routines.get(id)!!
        assertEquals("Costas", loaded.name)
        assertEquals(listOf(c.id, a.id, b.id), loaded.items.map { it.exercise.id })
        val positions = db.routineDao().getAllItems().filter { it.routineId == id }.map { it.position }.sorted()
        assertEquals(listOf(0, 1, 2), positions)
        assertTrue(backupCalls > 0)
    }

    @Test
    fun duplicate_copiesItemsWithNewIds_andSuffixName() = runTest {
        val a = byName("remada curvada")
        val id = routines.save(Routine(name = "Costas", items = listOf(RoutineItem(exercise = a, sets = 4, reps = 8, restSeconds = 120, loadKg = 42.5))))
        val copyId = routines.duplicate(id, "(cópia)")
        val original = routines.get(id)!!
        val copy = routines.get(copyId)!!
        assertEquals("Costas (cópia)", copy.name)
        assertEquals(original.items.map { it.copy(id = 0) }, copy.items.map { it.copy(id = 0) })
        assertNotEquals(original.items.single().id, copy.items.single().id)
    }

    @Test
    fun librarySearch_ignoresAccentsAndCase() = runTest {
        assertEquals("Prancha", exercises.observeLibrary("PRANCHA").first().single().name)
        assertEquals(listOf("Abdominal oblíquo"), exercises.observeLibrary("abdominal obliquo").first().map { it.name })
        val elevacao = exercises.observeLibrary("elevacao pelvica").first().map { it.name }
        assertEquals(listOf("Elevação pélvica"), elevacao)
        val supino = exercises.observeLibrary("SUPINO reto").first().map { it.name }
        assertTrue("Supino reto com barra" in supino)
        val backOnly = exercises.observeLibrary("", setOf(MuscleGroup.BACK)).first()
        assertTrue(backOnly.isNotEmpty() && backOnly.all { it.muscleGroup == MuscleGroup.BACK })
    }

    @Test
    fun deleteExerciseInUse_archivesInsteadOfDeleting() = runTest {
        val a = byName("remada curvada")
        db.insertRoutine("Costas", listOf(a.id))
        assertEquals(ExerciseDeleteResult.ARCHIVED, exercises.delete(a.id))
        assertTrue(exercises.observeLibrary("remada curvada").first().isEmpty())
        assertEquals("Remada curvada", exercises.get(a.id)!!.name)

        val free = byName("prancha")
        assertEquals(ExerciseDeleteResult.DELETED, exercises.delete(free.id))
        assertNull(exercises.get(free.id))
    }

    @Test
    fun updateLoads_changesOnlyGivenExercises() = runTest {
        val a = byName("remada curvada")
        val b = byName("pullover")
        val id = db.insertRoutine("Costas", listOf(a.id, b.id))
        routines.updateLoads(id, mapOf(a.id to 45.0))
        val items = routines.get(id)!!.items.associate { it.exercise.id to it.loadKg }
        assertEquals(45.0, items[a.id]!!, 0.0)
        assertEquals(40.0, items[b.id]!!, 0.0)
    }
}
