package io.github.luannamorim.myfit.data.db

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.data.db.entity.SessionExerciseEntity
import io.github.luannamorim.myfit.data.db.entity.SessionSetEntity
import io.github.luannamorim.myfit.data.db.entity.WorkoutSessionEntity
import io.github.luannamorim.myfit.domain.model.MovementType
import io.github.luannamorim.myfit.domain.model.MuscleGroup
import io.github.luannamorim.myfit.domain.model.SessionStatus
import io.github.luannamorim.myfit.testutil.T0
import io.github.luannamorim.myfit.testutil.inMemoryDb
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class SessionDaoTest {
    private val db = inMemoryDb()

    @After fun tearDown() = db.close()

    @Test
    fun activeSession_returnsOnlyActive_withExercisesAndSets() = runTest {
        db.sessionDao().insertSession(
            WorkoutSessionEntity(routineId = null, routineName = "Antigo", status = SessionStatus.FINISHED, started = T0, startDateEpochDay = 1),
        )
        assertNull(db.sessionDao().observeActive().first())

        val id = db.sessionDao().insertSession(
            WorkoutSessionEntity(routineId = null, routineName = "Costas", status = SessionStatus.ACTIVE, started = T0, paused = null, startDateEpochDay = 2),
        )
        val exId = db.sessionDao().insertExercise(
            SessionExerciseEntity(sessionId = id, exerciseId = null, position = 0, name = "Remada", muscleGroup = MuscleGroup.BACK, movementType = MovementType.PULL, restSeconds = 90),
        )
        db.sessionDao().insertSets((0 until 3).map { SessionSetEntity(sessionExerciseId = exId, setIndex = it, plannedReps = 12) })

        val active = db.sessionDao().observeActive().first()!!
        assertEquals("Costas", active.session.routineName)
        assertNull(active.session.paused)
        assertEquals(T0, active.session.started)
        assertEquals(3, active.exercises.single().sets.size)
    }
}
