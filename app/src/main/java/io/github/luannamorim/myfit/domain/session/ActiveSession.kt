package io.github.luannamorim.myfit.domain.session

import io.github.luannamorim.myfit.core.time.Mark
import io.github.luannamorim.myfit.domain.model.ItemStatus
import io.github.luannamorim.myfit.domain.model.MuscleGroup

/** Snapshot of an in-progress workout. All timing derives from persisted [Mark]s. */
data class ActiveSession(
    val id: Long,
    val routineId: Long?,
    val routineName: String,
    val startedAt: Mark,
    val pausedAt: Mark?,
    val totalPausedMs: Long,
    val exercises: List<SessionExercise>,
    val cursor: Cursor,
    val rest: Rest?,
) {
    val isPaused: Boolean get() = pausedAt != null
    val currentExercise: SessionExercise? get() = exercises.getOrNull(cursor.exerciseIndex)
    val currentSet: SessionSet? get() = currentExercise?.sets?.getOrNull(cursor.setIndex)
}

data class SessionExercise(
    val id: Long,
    val exerciseId: Long?,
    val name: String,
    val muscleGroup: MuscleGroup,
    val imagePath: String?,
    val restSeconds: Int,
    val notes: String?,
    val status: ItemStatus,
    val sets: List<SessionSet>,
    val seedKey: String? = null,
) {
    val hasPending: Boolean get() = sets.any { it.status == ItemStatus.PENDING }
    val doneCount: Int get() = sets.count { it.status == ItemStatus.DONE }
}

data class SessionSet(
    val id: Long,
    val plannedReps: Int,
    val plannedLoadKg: Double?,
    val actualReps: Int? = null,
    val actualLoadKg: Double? = null,
    val status: ItemStatus = ItemStatus.PENDING,
    val completedAtWallMs: Long? = null,
)

data class Cursor(val exerciseIndex: Int, val setIndex: Int)

/** A rest period that started at [startedAt] and lasts [durationMs] (excluding pauses). */
data class Rest(val startedAt: Mark, val durationMs: Long)

sealed interface FinishCheck {
    data object Ok : FinishCheck
    data object NothingCompleted : FinishCheck
}
