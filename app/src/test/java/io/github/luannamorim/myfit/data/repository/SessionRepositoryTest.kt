package io.github.luannamorim.myfit.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.core.time.FakeTimeSource
import io.github.luannamorim.myfit.data.db.entity.ExerciseEntity
import io.github.luannamorim.myfit.domain.model.ItemStatus
import io.github.luannamorim.myfit.domain.model.SessionStatus
import io.github.luannamorim.myfit.domain.session.SessionEngine
import io.github.luannamorim.myfit.testutil.inMemoryDb
import io.github.luannamorim.myfit.testutil.insertExercise
import io.github.luannamorim.myfit.testutil.insertRoutine
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class SessionRepositoryTest {
    private val db = inMemoryDb()
    private val time = FakeTimeSource()
    private var backups = 0
    private val repo = SessionRepository(db, BackupTrigger { backups++ })

    @After fun tearDown() = db.close()

    private suspend fun routine(): Pair<Long, List<Long>> {
        val a = db.insertExercise("Remada curvada")
        val b = db.insertExercise("Pullover")
        return db.insertRoutine("Costas", listOf(a, b)) to listOf(a, b)
    }

    @Test
    fun start_snapshotsNamesAndTargets() = runTest {
        val (routineId, _) = routine()
        val id = repo.start(routineId, time.now(), time.zone())
        val s = repo.getActive()!!
        assertEquals(id, s.id)
        assertEquals("Costas", s.routineName)
        assertEquals(listOf("Remada curvada", "Pullover"), s.exercises.map { it.name })
        assertEquals(3, s.exercises[0].sets.size)
        assertTrue(s.exercises.flatMap { it.sets }.all { it.plannedReps == 12 && it.plannedLoadKg == 40.0 })
        assertEquals(90, s.exercises[0].restSeconds)
    }

    @Test(expected = ActiveSessionExistsException::class)
    fun start_withActiveSession_throws() = runTest {
        val (routineId, _) = routine()
        repo.start(routineId, time.now(), time.zone())
        repo.start(routineId, time.now(), time.zone())
    }

    @Test(expected = EmptyRoutineException::class)
    fun start_emptyRoutine_throws() = runTest {
        val routineId = db.insertRoutine("Vazio", emptyList())
        repo.start(routineId, time.now(), time.zone())
    }

    @Test
    fun save_thenGetActive_roundTrips() = runTest {
        val (routineId, _) = routine()
        repo.start(routineId, time.now(), time.zone())
        var s = repo.getActive()!!
        s = SessionEngine.completeSet(s, 10, 42.5, time.now())
        time.advance(20_000)
        s = SessionEngine.pause(s, time.now())
        repo.save(s)
        assertEquals(s, repo.getActive())
    }

    @Test
    fun routineDeletedDuringSession_sessionStillLoads() = runTest {
        val (routineId, _) = routine()
        repo.start(routineId, time.now(), time.zone())
        db.routineDao().deleteRoutine(routineId)
        val s = repo.getActive()!!
        assertNull(s.routineId)
        assertEquals("Costas", s.routineName)
        repo.save(SessionEngine.completeSet(s, 12, 40.0, time.now()))
        assertEquals(ItemStatus.DONE, repo.getActive()!!.exercises[0].sets[0].status)
    }

    @Test
    fun exerciseRenamedAfterFinish_historyKeepsOriginalName() = runTest {
        val (routineId, ids) = routine()
        repo.start(routineId, time.now(), time.zone())
        val s = SessionEngine.completeSet(repo.getActive()!!, 12, 40.0, time.now())
        val sessionId = repo.finish(s, time.now(), updateRoutineLoads = false)
        val entity: ExerciseEntity = db.exerciseDao().get(ids[0])!!
        db.exerciseDao().update(entity.copy(name = "Remada Pendlay"))
        val history = db.sessionDao().getSession(sessionId)!!
        assertEquals("Remada curvada", history.exercises.minBy { it.exercise.position }.exercise.name)
    }

    @Test
    fun finish_updatesRoutineLoadsWhenRequested() = runTest {
        val (routineId, ids) = routine()
        repo.start(routineId, time.now(), time.zone())
        var s = repo.getActive()!!
        s = SessionEngine.completeSet(s, 12, 42.5, time.now())
        s = SessionEngine.completeSet(s, 10, 45.0, time.now())
        repo.finish(s, time.now(), updateRoutineLoads = true)
        val loads = db.routineDao().getAllItems().associate { it.exerciseId to it.loadKg }
        assertEquals(45.0, loads[ids[0]]!!, 0.0)
        assertEquals(40.0, loads[ids[1]]!!, 0.0) // untouched: no completed sets
        assertTrue(backups > 0)
    }

    @Test
    fun finish_storesDurationExcludingPauses() = runTest {
        val (routineId, _) = routine()
        repo.start(routineId, time.now(), time.zone())
        var s = repo.getActive()!!
        time.advance(600_000)
        s = SessionEngine.pause(s, time.now())
        time.advance(300_000)
        s = SessionEngine.resume(s, time.now())
        time.advance(600_000)
        s = SessionEngine.completeSet(s, 12, 40.0, time.now())
        val id = repo.finish(s, time.now(), updateRoutineLoads = false)
        val row = db.sessionDao().getSession(id)!!.session
        assertEquals(SessionStatus.FINISHED, row.status)
        assertEquals(1_200_000L, row.durationMs)
        assertNull(row.paused)
        assertNull(repo.getActive())
    }
}
