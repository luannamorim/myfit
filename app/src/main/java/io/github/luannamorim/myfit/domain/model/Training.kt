package io.github.luannamorim.myfit.domain.model

data class Exercise(
    val id: Long = 0,
    val name: String,
    val muscleGroup: MuscleGroup,
    val movementType: MovementType,
    val imagePath: String? = null,
    val notes: String? = null,
    val isCustom: Boolean = true,
    /** Library entry this exercise came from (drawing, original name); null when user-created. */
    val seedKey: String? = null,
    /** For [ExerciseMeasure.TIME], the "reps" of its sets are seconds. */
    val measure: ExerciseMeasure = ExerciseMeasure.REPS,
    /** Starred by the user: listed first in the library. */
    val favorite: Boolean = false,
)

data class RoutineItem(
    val id: Long = 0,
    val exercise: Exercise,
    val sets: Int,
    /** Repetitions per set, or seconds per set when the exercise is timed. */
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
