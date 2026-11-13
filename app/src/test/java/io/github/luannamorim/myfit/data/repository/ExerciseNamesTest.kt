package io.github.luannamorim.myfit.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.data.db.entity.ExerciseEntity
import io.github.luannamorim.myfit.domain.model.MovementType
import io.github.luannamorim.myfit.domain.model.MuscleGroup
import io.github.luannamorim.myfit.domain.session.SessionEngine
import io.github.luannamorim.myfit.testutil.LocaleRule
import io.github.luannamorim.myfit.testutil.SessionEnv
import io.github.luannamorim.myfit.testutil.insertRoutine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Library exercises show their official name in the app's language; custom ones keep the user's name. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class ExerciseNamesTest {
    @get:Rule val locale = LocaleRule() // pt-BR
    private val env = SessionEnv()
    private val exercises = ExerciseRepository(env.db.exerciseDao(), env.time, BackupTrigger { })
    private val history = HistoryRepository(env.db, env.time, BackupTrigger { })

    @After fun tearDown() = env.close()

    private suspend fun library(stored: String) = env.db.exerciseDao().insert(
        ExerciseEntity(
            name = stored, muscleGroup = MuscleGroup.CHEST, movementType = MovementType.PUSH, isCustom = false, createdAtMs = 0,
            seedKey = "barbell-bench-press",
        ),
    )

    private suspend fun custom(name: String) = env.db.exerciseDao().insert(
        ExerciseEntity(name = name, muscleGroup = MuscleGroup.BACK, movementType = MovementType.PULL, isCustom = true, createdAtMs = 0),
    )

    @Test
    fun libraryRow_showsOfficialPortugueseName() = runBlocking {
        val lib = library("Barbell bench press") // seeded while the phone was in English
        val mine = custom("Meu exercício")
        assertEquals("Supino reto com barra", exercises.get(lib)!!.name)
        assertEquals("Meu exercício", exercises.get(mine)!!.name)
    }

    @Test
    fun userExerciseLinkedToTheLibrary_isALibraryExercise() = runBlocking {
        // Created by the user before the library had it, then linked by name (migration 2→3 / restore).
        val linked = env.db.exerciseDao().insert(
            ExerciseEntity(
                name = "Barbell bench press", muscleGroup = MuscleGroup.CHEST, movementType = MovementType.PUSH, isCustom = true,
                createdAtMs = 0, seedKey = "barbell-bench-press",
            ),
        )
        val shown = exercises.get(linked)!!
        assertEquals("Supino reto com barra", shown.name)
        assertFalse(shown.isCustom) // no "Custom" badge next to a locked, official name
        assertTrue(exercises.get(custom("Meu exercício"))!!.isCustom)
    }

    @Test
    fun renamedLibraryRow_showsOfficialName_andSaveKeepsStoredName() = runBlocking {
        val lib = library("Meu supino")
        val shown = exercises.get(lib)!!
        assertEquals("Supino reto com barra", shown.name)
        exercises.save(shown.copy(name = "Outro", notes = "pegada fechada"))
        val row = env.db.exerciseDao().get(lib)!!
        assertEquals("Meu supino", row.name) // stored name untouched
        assertEquals("pegada fechada", row.notes) // other fields still saved
    }

    @Test
    fun search_findsLibraryRowByPortugueseName() = runBlocking {
        val lib = library("Barbell bench press")
        assertTrue(exercises.observeLibrary("supino reto").first().any { it.id == lib })
        assertTrue(exercises.observeLibrary("Supíno").first().any { it.id == lib })
    }

    @Test
    fun historyAndActiveSession_useOfficialName() = runBlocking {
        val lib = library("Bench press (old)")
        val routine = env.db.insertRoutine("Peito", listOf(lib))
        env.controller.start(routine)
        assertEquals("Supino reto com barra", env.repo.getActive()!!.exercises.single().name)

        // A workout started by an older version kept the old name in its snapshot: it's resolved on read.
        env.db.openHelper.writableDatabase.execSQL("UPDATE session_exercise SET name = 'Bench press (old)'")
        val active = env.repo.getActive()!!
        assertEquals("Supino reto com barra", active.exercises.single().name)

        val done = SessionEngine.completeSet(active, 10, 40.0, env.time.now())
        env.time.advance(600_000)
        env.repo.finish(done, env.time.now(), updateRoutineLoads = false)
        assertEquals(listOf("Supino reto com barra"), history.observeLast().first()!!.exercises)
    }
}
