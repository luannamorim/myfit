package io.github.luannamorim.myfit.data.repository

import androidx.room.withTransaction
import io.github.luannamorim.myfit.core.time.Mark
import io.github.luannamorim.myfit.data.db.MyFitDatabase
import io.github.luannamorim.myfit.data.db.entity.SessionExerciseEntity
import io.github.luannamorim.myfit.data.db.entity.SessionSetEntity
import io.github.luannamorim.myfit.data.db.entity.SessionWithExercises
import io.github.luannamorim.myfit.data.db.entity.WorkoutSessionEntity
import io.github.luannamorim.myfit.data.db.seed.ExerciseSeed
import io.github.luannamorim.myfit.domain.model.Intensity
import io.github.luannamorim.myfit.domain.model.ItemStatus
import io.github.luannamorim.myfit.domain.model.SessionStatus
import io.github.luannamorim.myfit.domain.session.ActiveSession
import io.github.luannamorim.myfit.domain.session.Cursor
import io.github.luannamorim.myfit.domain.session.Rest
import io.github.luannamorim.myfit.domain.session.SessionEngine
import io.github.luannamorim.myfit.domain.session.SessionExercise
import io.github.luannamorim.myfit.domain.session.SessionSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

class ActiveSessionExistsException : IllegalStateException("there is already an active session")
class EmptyRoutineException : IllegalStateException("routine has no exercises")

/** Persists the in-progress session at every step and turns it into history when finished. */
@Singleton
class SessionRepository @Inject constructor(
    private val db: MyFitDatabase,
    private val backup: BackupTrigger,
) {
    private val dao = db.sessionDao()

    fun observeActive(): Flow<ActiveSession?> = dao.observeActive().map { it?.toActive() }.distinctUntilChanged()

    suspend fun getActive(): ActiveSession? = dao.getActive()?.toActive()

    /** Creates an active session from a routine, snapshotting names and targets. Returns the session id. */
    suspend fun start(routineId: Long, now: Mark, zone: ZoneId): Long = db.withTransaction {
        if (dao.getActive() != null) throw ActiveSessionExistsException()
        val routine = db.routineDao().getWithItems(routineId) ?: throw IllegalArgumentException("routine $routineId not found")
        val items = routine.items.sortedBy { it.item.position }
        if (items.isEmpty()) throw EmptyRoutineException()

        val sessionId = dao.insertSession(
            WorkoutSessionEntity(
                routineId = routine.routine.id,
                routineName = routine.routine.name,
                status = SessionStatus.ACTIVE,
                started = now,
                startDateEpochDay = Instant.ofEpochMilli(now.wallMs).atZone(zone).toLocalDate().toEpochDay(),
            ),
        )
        items.forEachIndexed { position, row ->
            val exerciseId = dao.insertExercise(
                SessionExerciseEntity(
                    sessionId = sessionId,
                    exerciseId = row.exercise.id,
                    position = position,
                    name = ExerciseSeed.displayName(row.exercise.seedKey, row.exercise.name),
                    muscleGroup = row.exercise.muscleGroup,
                    movementType = row.exercise.movementType,
                    imagePath = row.exercise.imagePath,
                    seedKey = row.exercise.seedKey,
                    measure = row.exercise.measure,
                    restSeconds = row.item.restSeconds,
                    notes = row.item.notes ?: row.exercise.notes,
                ),
            )
            dao.insertSets(
                (0 until row.item.sets).map { index ->
                    SessionSetEntity(
                        sessionExerciseId = exerciseId,
                        setIndex = index,
                        plannedReps = row.item.reps,
                        plannedLoadKg = row.item.loadKg,
                    )
                },
            )
        }
        sessionId
    }

    /** Writes the whole session state (session row, exercises, sets) in one transaction. */
    suspend fun save(session: ActiveSession) {
        db.withTransaction { writeState(session, status = SessionStatus.ACTIVE, ended = null, durationMs = null) }
    }

    /**
     * Turns the session into history. Duration excludes pauses. When [updateRoutineLoads] is set, the
     * routine's planned load per exercise becomes the load of its last completed set.
     */
    suspend fun finish(session: ActiveSession, now: Mark, updateRoutineLoads: Boolean, intensity: Intensity? = null, imagePath: String? = null): Long {
        val durationMs = SessionEngine.elapsedMs(session, now)
        db.withTransaction {
            writeState(
                session.copy(pausedAt = null, rest = null),
                status = SessionStatus.FINISHED,
                ended = now,
                durationMs = durationMs,
                intensity = intensity,
                imagePath = imagePath,
            )
            val routineId = session.routineId
            if (updateRoutineLoads && routineId != null) {
                session.exercises.forEach { exercise ->
                    val lastDone = exercise.sets.lastOrNull { it.status == ItemStatus.DONE } ?: return@forEach
                    val exerciseId = exercise.exerciseId ?: return@forEach
                    db.routineDao().updateLoad(routineId, exerciseId, lastDone.actualLoadKg)
                }
            }
        }
        backup.onDataChanged()
        return session.id
    }

    suspend fun discard(sessionId: Long) {
        dao.deleteSession(sessionId)
    }

    private suspend fun writeState(
        session: ActiveSession,
        status: SessionStatus,
        ended: Mark?,
        durationMs: Long?,
        intensity: Intensity? = null,
        imagePath: String? = null,
    ) {
        val current = dao.getSession(session.id) ?: throw IllegalStateException("session ${session.id} not found")
        dao.updateSession(
            current.session.copy(
                status = status,
                paused = session.pausedAt,
                totalPausedMs = session.totalPausedMs,
                ended = ended,
                durationMs = durationMs,
                cursorExercise = session.cursor.exerciseIndex,
                cursorSet = session.cursor.setIndex,
                restStarted = session.rest?.startedAt,
                restDurationMs = session.rest?.durationMs,
                intensity = intensity,
                imagePath = imagePath,
            ),
        )
        val exercisesById = current.exercises.associateBy { it.exercise.id }
        dao.updateExercises(
            session.exercises.mapNotNull { ex ->
                exercisesById[ex.id]?.exercise?.copy(status = ex.status, restSeconds = ex.restSeconds)
            },
        )
        val setsById = current.exercises.flatMap { it.sets }.associateBy { it.id }
        dao.updateSets(
            session.exercises.flatMap { it.sets }.mapNotNull { set ->
                setsById[set.id]?.copy(
                    plannedReps = set.plannedReps,
                    plannedLoadKg = set.plannedLoadKg,
                    actualReps = set.actualReps,
                    actualLoadKg = set.actualLoadKg,
                    status = set.status,
                    completedAtWallMs = set.completedAtWallMs,
                )
            },
        )
    }
}

internal fun SessionWithExercises.toActive(): ActiveSession {
    val restStarted = session.restStarted
    val restDuration = session.restDurationMs
    return ActiveSession(
        id = session.id,
        routineId = session.routineId,
        routineName = session.routineName,
        startedAt = session.started,
        pausedAt = session.paused,
        totalPausedMs = session.totalPausedMs,
        exercises = exercises.sortedBy { it.exercise.position }.map { row ->
            SessionExercise(
                id = row.exercise.id,
                exerciseId = row.exercise.exerciseId,
                name = ExerciseSeed.displayName(row.exercise.seedKey, row.exercise.name),
                muscleGroup = row.exercise.muscleGroup,
                imagePath = row.exercise.imagePath,
                seedKey = row.exercise.seedKey,
                measure = row.exercise.measure,
                restSeconds = row.exercise.restSeconds,
                notes = row.exercise.notes,
                status = row.exercise.status,
                sets = row.sets.sortedBy { it.setIndex }.map { s ->
                    SessionSet(
                        id = s.id,
                        plannedReps = s.plannedReps,
                        plannedLoadKg = s.plannedLoadKg,
                        actualReps = s.actualReps,
                        actualLoadKg = s.actualLoadKg,
                        status = s.status,
                        completedAtWallMs = s.completedAtWallMs,
                    )
                },
            )
        },
        cursor = Cursor(session.cursorExercise, session.cursorSet),
        rest = if (restStarted != null && restDuration != null) Rest(restStarted, restDuration) else null,
    )
}
