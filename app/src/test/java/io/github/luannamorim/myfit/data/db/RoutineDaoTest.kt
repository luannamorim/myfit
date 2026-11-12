package io.github.luannamorim.myfit.data.db

import android.database.sqlite.SQLiteConstraintException
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.data.db.entity.RoutineExerciseEntity
import io.github.luannamorim.myfit.data.db.entity.WorkoutSessionEntity
import io.github.luannamorim.myfit.domain.model.SessionStatus
import io.github.luannamorim.myfit.testutil.T0
import io.github.luannamorim.myfit.testutil.inMemoryDb
import io.github.luannamorim.myfit.testutil.insertExercise
import io.github.luannamorim.myfit.testutil.insertRoutine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class RoutineDaoTest {
    private val db = inMemoryDb()

    @After fun tearDown() = db.close()

    @Test
    fun deletingRoutine_cascadesRoutineExercises() = runTest {
        val ex = db.insertExercise("Remada curvada")
        val routine = db.insertRoutine("Costas", listOf(ex))
        db.routineDao().deleteRoutine(routine)
        assertTrue(db.routineDao().getAllItems().isEmpty())
    }

    @Test
    fun deletingRoutine_keepsSessionWithNullRoutineId_andSnapshotName() = runTest {
        val ex = db.insertExercise("Remada curvada")
        val routine = db.insertRoutine("Costas", listOf(ex))
        val sessionId = db.sessionDao().insertSession(
            WorkoutSessionEntity(routineId = routine, routineName = "Costas", status = SessionStatus.FINISHED, started = T0, startDateEpochDay = 20_000),
        )
        db.routineDao().deleteRoutine(routine)
        val session = db.sessionDao().getSession(sessionId)!!.session
        assertNull(session.routineId)
        assertEquals("Costas", session.routineName)
    }

    @Test
    fun deletingExerciseInUse_isRestricted() = runTest {
        val ex = db.insertExercise("Pullover")
        db.insertRoutine("Costas", listOf(ex))
        try {
            db.exerciseDao().delete(ex)
            fail("expected foreign key violation")
        } catch (_: SQLiteConstraintException) {
        }
        assertEquals(1, db.exerciseDao().routineUsageCount(ex))
    }

    @Test
    fun observeRoutineWithExercises_containsAllItems_withPositions() = runTest {
        val a = db.insertExercise("A")
        val b = db.insertExercise("B")
        val routine = db.insertRoutine("Treino", listOf(a, b))
        db.routineDao().deleteItems(routine)
        db.routineDao().insertItems(
            listOf(
                RoutineExerciseEntity(routineId = routine, exerciseId = b, position = 0, sets = 3, reps = 10, restSeconds = 60),
                RoutineExerciseEntity(routineId = routine, exerciseId = a, position = 1, sets = 4, reps = 8, restSeconds = 120),
            ),
        )
        val loaded = db.routineDao().observeWithItems(routine).first()!!
        val ordered = loaded.items.sortedBy { it.item.position }.map { it.exercise.name }
        assertEquals(listOf("B", "A"), ordered)
    }

    @Test
    fun overviews_countExercises_andLastFinishedDay() = runTest {
        val ex = db.insertExercise("Remada")
        val routine = db.insertRoutine("Costas", listOf(ex, ex))
        db.sessionDao().insertSession(
            WorkoutSessionEntity(routineId = routine, routineName = "Costas", status = SessionStatus.FINISHED, started = T0, startDateEpochDay = 20_010),
        )
        db.sessionDao().insertSession(
            WorkoutSessionEntity(routineId = routine, routineName = "Costas", status = SessionStatus.ACTIVE, started = T0, startDateEpochDay = 20_020),
        )
        val overview = db.routineDao().observeOverviews().first().single()
        assertEquals(2, overview.exerciseCount)
        assertEquals(20_010L, overview.lastDoneEpochDay)
    }
}
