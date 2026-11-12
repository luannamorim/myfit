package io.github.luannamorim.myfit.data.db.entity

import androidx.room.Embedded
import androidx.room.Relation

data class RoutineItemWithExercise(
    @Embedded val item: RoutineExerciseEntity,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: ExerciseEntity,
)

data class RoutineWithItems(
    @Embedded val routine: RoutineEntity,
    @Relation(entity = RoutineExerciseEntity::class, parentColumn = "id", entityColumn = "routineId")
    val items: List<RoutineItemWithExercise>,
)

data class RoutineOverviewRow(
    val id: Long,
    val name: String,
    val description: String?,
    val position: Int,
    val exerciseCount: Int,
    val lastDoneEpochDay: Long?,
)

data class SessionExerciseWithSets(
    @Embedded val exercise: SessionExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "sessionExerciseId")
    val sets: List<SessionSetEntity>,
)

data class SessionWithExercises(
    @Embedded val session: WorkoutSessionEntity,
    @Relation(entity = SessionExerciseEntity::class, parentColumn = "id", entityColumn = "sessionId")
    val exercises: List<SessionExerciseWithSets>,
)
