package io.github.luannamorim.myfit.domain.model

data class Exercise(
    val id: Long = 0,
    val name: String,
    val muscleGroup: MuscleGroup,
    val movementType: MovementType,
    val imagePath: String? = null,
    val notes: String? = null,
    val isCustom: Boolean = true,
)

data class RoutineItem(
    val id: Long = 0,
    val exercise: Exercise,
    val sets: Int,
    val reps: Int,
    val restSeconds: Int,
    val loadKg: Double? = null,
    val notes: String? = null,
)

data class Routine(
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val items: List<RoutineItem> = emptyList(),
)

data class RoutineOverview(
    val id: Long,
    val name: String,
    val description: String?,
    val exerciseCount: Int,
    val lastDoneEpochDay: Long?,
)
