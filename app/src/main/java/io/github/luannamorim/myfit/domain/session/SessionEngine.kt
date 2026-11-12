package io.github.luannamorim.myfit.domain.session

import io.github.luannamorim.myfit.core.time.Mark
import io.github.luannamorim.myfit.core.time.millisUntil
import io.github.luannamorim.myfit.domain.model.ItemStatus

/**
 * Pure state machine for a workout session: set → rest → next set → next exercise → finish.
 * Every operation returns a new [ActiveSession]; callers persist it immediately.
 */
object SessionEngine {

    /** Marks the current set done with the performed values, carries the load forward and starts the rest. */
    fun completeSet(session: ActiveSession, reps: Int, loadKg: Double?, now: Mark): ActiveSession {
        val s = resume(session, now)
        val (ei, si) = s.cursor
        val exercise = s.exercises.getOrNull(ei) ?: return s
        val set = exercise.sets.getOrNull(si) ?: return s
        if (set.status != ItemStatus.PENDING) return s

        val sets = exercise.sets.mapIndexed { i, it ->
            when {
                i == si -> it.copy(actualReps = reps.coerceAtLeast(0), actualLoadKg = loadKg, status = ItemStatus.DONE, completedAtWallMs = now.wallMs)
                // Carry the load forward to the remaining sets of this exercise.
                i > si && it.status == ItemStatus.PENDING -> it.copy(plannedLoadKg = loadKg)
                else -> it
            }
        }
        val updated = s.withExercise(ei, exercise.copy(sets = sets).withDerivedStatus())
        val next = nextPending(updated.exercises, updated.cursor)
        val restMs = exercise.restSeconds * 1000L
        val rest = if (next == null || restMs <= 0) null else Rest(now, restMs)
        return updated.copy(cursor = next ?: updated.cursor, rest = rest)
    }

    fun skipSet(session: ActiveSession, now: Mark): ActiveSession {
        val s = resume(session, now)
        val (ei, si) = s.cursor
        val exercise = s.exercises.getOrNull(ei) ?: return s
        if (exercise.sets.getOrNull(si)?.status != ItemStatus.PENDING) return s
        val sets = exercise.sets.mapIndexed { i, it -> if (i == si) it.copy(status = ItemStatus.SKIPPED) else it }
        val updated = s.withExercise(ei, exercise.copy(sets = sets).withDerivedStatus())
        return updated.copy(cursor = nextPending(updated.exercises, updated.cursor) ?: updated.cursor, rest = null)
    }

    fun skipExercise(session: ActiveSession, now: Mark): ActiveSession {
        val s = resume(session, now)
        val ei = s.cursor.exerciseIndex
        val exercise = s.exercises.getOrNull(ei) ?: return s
        if (!exercise.hasPending) return s
        val sets = exercise.sets.map { if (it.status == ItemStatus.PENDING) it.copy(status = ItemStatus.SKIPPED) else it }
        val updated = s.withExercise(ei, exercise.copy(sets = sets).withDerivedStatus())
        return updated.copy(cursor = nextPending(updated.exercises, updated.cursor) ?: updated.cursor, rest = null)
    }

    /** Jumps to another exercise. A skipped exercise has its skipped sets reopened. Rest keeps running. */
    fun goToExercise(session: ActiveSession, index: Int): ActiveSession {
        val exercise = session.exercises.getOrNull(index) ?: return session
        val reopened = if (!exercise.hasPending && exercise.sets.any { it.status == ItemStatus.SKIPPED }) {
            exercise.copy(sets = exercise.sets.map { if (it.status == ItemStatus.SKIPPED) it.copy(status = ItemStatus.PENDING) else it })
                .withDerivedStatus()
        } else {
            exercise
        }
        val firstPending = reopened.sets.indexOfFirst { it.status == ItemStatus.PENDING }
        if (firstPending < 0) return session
        return session.withExercise(index, reopened).copy(cursor = Cursor(index, firstPending))
    }

    /** Edits a set: performed values for done sets, planned values for pending sets. */
    fun editSet(session: ActiveSession, exerciseIndex: Int, setIndex: Int, reps: Int, loadKg: Double?): ActiveSession {
        val exercise = session.exercises.getOrNull(exerciseIndex) ?: return session
        val set = exercise.sets.getOrNull(setIndex) ?: return session
        val edited = when (set.status) {
            ItemStatus.DONE -> set.copy(actualReps = reps.coerceAtLeast(0), actualLoadKg = loadKg)
            ItemStatus.PENDING -> set.copy(plannedReps = reps.coerceAtLeast(1), plannedLoadKg = loadKg)
            ItemStatus.SKIPPED -> return session
        }
        val sets = exercise.sets.toMutableList().also { it[setIndex] = edited }
        return session.withExercise(exerciseIndex, exercise.copy(sets = sets))
    }

    /** Adds/removes time from the running rest; the remaining time never goes below zero. */
    fun adjustRest(session: ActiveSession, deltaMs: Long, now: Mark): ActiveSession {
        val rest = session.rest ?: return session
        val elapsed = rest.startedAt.millisUntil(session.pausedAt ?: now)
        val duration = (rest.durationMs + deltaMs).coerceAtLeast(elapsed)
        return session.copy(rest = rest.copy(durationMs = duration))
    }

    /** Changes the rest used after the next sets of an exercise. */
    fun setExerciseRest(session: ActiveSession, exerciseIndex: Int, seconds: Int): ActiveSession {
        val exercise = session.exercises.getOrNull(exerciseIndex) ?: return session
        return session.withExercise(exerciseIndex, exercise.copy(restSeconds = seconds.coerceIn(0, 600)))
    }

    fun skipRest(session: ActiveSession): ActiveSession = session.copy(rest = null)

    fun pause(session: ActiveSession, now: Mark): ActiveSession =
        if (session.pausedAt != null) session else session.copy(pausedAt = now)

    fun resume(session: ActiveSession, now: Mark): ActiveSession {
        val pausedAt = session.pausedAt ?: return session
        val pausedMs = pausedAt.millisUntil(now)
        val rest = session.rest?.let { r ->
            val remaining = r.durationMs - r.startedAt.millisUntil(pausedAt)
            if (remaining <= 0) null else Rest(now, remaining)
        }
        return session.copy(pausedAt = null, totalPausedMs = session.totalPausedMs + pausedMs, rest = rest)
    }

    /** Active training time: start → now, minus pauses. */
    fun elapsedMs(session: ActiveSession, now: Mark): Long =
        (session.startedAt.millisUntil(session.pausedAt ?: now) - session.totalPausedMs).coerceAtLeast(0)

    fun restRemainingMs(session: ActiveSession, now: Mark): Long? = session.rest?.let { r ->
        (r.durationMs - r.startedAt.millisUntil(session.pausedAt ?: now)).coerceAtLeast(0)
    }

    /** Wall-clock time the rest ends (for the alarm); null when paused or not resting. */
    fun restEndsAtWallMs(session: ActiveSession, now: Mark): Long? {
        if (session.isPaused) return null
        val remaining = restRemainingMs(session, now) ?: return null
        return now.wallMs + remaining
    }

    fun isComplete(session: ActiveSession): Boolean = session.exercises.none { it.hasPending }

    fun checkFinish(session: ActiveSession): FinishCheck =
        if (session.exercises.any { it.doneCount > 0 }) FinishCheck.Ok else FinishCheck.NothingCompleted

    /** Next pending set: same exercise first, then following exercises, then wrap to earlier ones. */
    internal fun nextPending(exercises: List<SessionExercise>, from: Cursor): Cursor? {
        val order = (from.exerciseIndex until exercises.size) + (0 until from.exerciseIndex)
        for (i in order) {
            val setIndex = exercises.getOrNull(i)?.sets?.indexOfFirst { it.status == ItemStatus.PENDING } ?: -1
            if (setIndex >= 0) return Cursor(i, setIndex)
        }
        return null
    }

    private fun ActiveSession.withExercise(index: Int, exercise: SessionExercise) =
        copy(exercises = exercises.toMutableList().also { it[index] = exercise })

    private fun SessionExercise.withDerivedStatus(): SessionExercise = copy(
        status = when {
            hasPending -> ItemStatus.PENDING
            doneCount > 0 -> ItemStatus.DONE
            else -> ItemStatus.SKIPPED
        },
    )
}
