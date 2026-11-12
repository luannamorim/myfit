package io.github.luannamorim.myfit.platform.session

import io.github.luannamorim.myfit.core.time.TimeSource
import io.github.luannamorim.myfit.data.repository.SessionRepository
import io.github.luannamorim.myfit.domain.model.Intensity
import io.github.luannamorim.myfit.domain.session.ActiveSession
import io.github.luannamorim.myfit.domain.session.SessionEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single entry point for workout actions (UI, notification actions, alarm). Each operation runs the
 * pure engine, persists the result immediately, then syncs the rest alarm and notifications.
 */
@Singleton
class SessionController @Inject constructor(
    private val repository: SessionRepository,
    private val time: TimeSource,
    private val alarms: RestAlarmScheduler,
    private val service: WorkoutServiceStarter,
    private val notifications: SessionNotifications,
) {
    private val mutex = Mutex()

    val active: Flow<ActiveSession?> = repository.observeActive()

    suspend fun current(): ActiveSession? = repository.getActive()

    /** Starts a session from a routine (fails if one is already active or the routine is empty). */
    suspend fun start(routineId: Long): Long = mutex.withLock {
        val id = repository.start(routineId, time.now(), time.zone())
        service.start()
        id
    }

    suspend fun completeSet(reps: Int, loadKg: Double?) = update { s, now -> SessionEngine.completeSet(s, reps, loadKg, now) }

    /** Notification shortcut: completes the current set with its planned values. */
    suspend fun completeSetAsPlanned() = update { s, now ->
        val set = s.currentSet ?: return@update s
        SessionEngine.completeSet(s, set.plannedReps, set.plannedLoadKg, now)
    }

    suspend fun skipSet() = update { s, now -> SessionEngine.skipSet(s, now) }
    suspend fun skipExercise() = update { s, now -> SessionEngine.skipExercise(s, now) }
    suspend fun goToExercise(index: Int) = update { s, _ -> SessionEngine.goToExercise(s, index) }
    suspend fun editSet(exerciseIndex: Int, setIndex: Int, reps: Int, loadKg: Double?) =
        update { s, _ -> SessionEngine.editSet(s, exerciseIndex, setIndex, reps, loadKg) }
    suspend fun adjustRest(deltaSeconds: Int) = update { s, now -> SessionEngine.adjustRest(s, deltaSeconds * 1000L, now) }
    suspend fun setExerciseRest(exerciseIndex: Int, seconds: Int) = update { s, _ -> SessionEngine.setExerciseRest(s, exerciseIndex, seconds) }
    suspend fun skipRest() = update { s, _ -> SessionEngine.skipRest(s) }
    suspend fun pause() = update { s, now -> SessionEngine.pause(s, now) }
    suspend fun resume() = update { s, now -> SessionEngine.resume(s, now) }

    /** Saves the session to history. Returns its id, or null if there was no active session. */
    suspend fun finish(updateRoutineLoads: Boolean, intensity: Intensity? = null, imagePath: String? = null): Long? = mutex.withLock {
        val session = repository.getActive() ?: return@withLock null
        val id = repository.finish(session, time.now(), updateRoutineLoads, intensity, imagePath)
        tearDown()
        id
    }

    /** Deletes the active session without saving it to history. */
    suspend fun discard() = mutex.withLock {
        repository.getActive()?.let { repository.discard(it.id) }
        tearDown()
    }

    /** Re-arms alarm and service from the database (app start, service restart, after restore). */
    suspend fun resync() = mutex.withLock {
        val session = repository.getActive()
        if (session == null) {
            tearDown()
        } else {
            syncAlarm(session)
            service.start()
        }
    }

    private suspend fun update(op: (ActiveSession, io.github.luannamorim.myfit.core.time.Mark) -> ActiveSession) = mutex.withLock {
        val current = repository.getActive() ?: return@withLock
        val next = op(current, time.now())
        if (next == current) return@withLock
        repository.save(next)
        syncAlarm(next)
        if (next.rest != current.rest || next.cursor != current.cursor) notifications.cancelRestEnd()
    }

    private fun syncAlarm(session: ActiveSession) {
        val now = time.now()
        val endsAt = SessionEngine.restEndsAtWallMs(session, now)
        if (endsAt != null && endsAt > now.wallMs) alarms.schedule(endsAt) else alarms.cancel()
    }

    private fun tearDown() {
        alarms.cancel()
        notifications.cancelAll()
        service.stop()
    }
}
