package io.github.luannamorim.myfit.domain.backup

import kotlinx.serialization.Serializable

/**
 * Backup data, format version 1. Field names are part of the file format: never rename them;
 * add new optional fields with defaults instead. Enums are stored by name.
 */
@Serializable
data class BackupDataV1(
    val exercises: List<ExerciseDto> = emptyList(),
    val routines: List<RoutineDto> = emptyList(),
    val routineExercises: List<RoutineExerciseDto> = emptyList(),
    val sessions: List<SessionDto> = emptyList(),
    val sessionExercises: List<SessionExerciseDto> = emptyList(),
    val sessionSets: List<SessionSetDto> = emptyList(),
    val assessments: List<AssessmentDto> = emptyList(),
    val photos: List<PhotoDto> = emptyList(),
) {
    fun counts(): Map<String, Int> = mapOf(
        "exercises" to exercises.size,
        "routines" to routines.size,
        "routineExercises" to routineExercises.size,
        "sessions" to sessions.size,
        "sessionExercises" to sessionExercises.size,
        "sessionSets" to sessionSets.size,
        "assessments" to assessments.size,
        "photos" to photos.size,
    )

    /** Every image path referenced by the data (relative to filesDir). */
    fun referencedImages(): Set<String> =
        (exercises.mapNotNull { it.imagePath } + sessionExercises.mapNotNull { it.imagePath } + photos.map { it.imagePath }).toSet()
}

@Serializable data class MarkDto(val wallMs: Long, val elapsedMs: Long, val bootCount: Int)

@Serializable
data class ExerciseDto(
    val id: Long, val name: String, val muscleGroup: String, val movementType: String,
    val imagePath: String? = null, val notes: String? = null, val isCustom: Boolean, val archived: Boolean = false, val createdAtMs: Long,
)

@Serializable
data class RoutineDto(val id: Long, val name: String, val description: String? = null, val position: Int, val createdAtMs: Long, val updatedAtMs: Long)

@Serializable
data class RoutineExerciseDto(
    val id: Long, val routineId: Long, val exerciseId: Long, val position: Int, val sets: Int, val reps: Int,
    val restSeconds: Int, val loadKg: Double? = null, val notes: String? = null,
)

@Serializable
data class SessionDto(
    val id: Long, val routineId: Long? = null, val routineName: String, val status: String,
    val started: MarkDto, val paused: MarkDto? = null, val totalPausedMs: Long = 0, val ended: MarkDto? = null,
    val durationMs: Long? = null, val cursorExercise: Int = 0, val cursorSet: Int = 0,
    val restStarted: MarkDto? = null, val restDurationMs: Long? = null, val startDateEpochDay: Long,
)

@Serializable
data class SessionExerciseDto(
    val id: Long, val sessionId: Long, val exerciseId: Long? = null, val position: Int, val name: String,
    val muscleGroup: String, val movementType: String, val imagePath: String? = null, val restSeconds: Int,
    val notes: String? = null, val status: String,
)

@Serializable
data class SessionSetDto(
    val id: Long, val sessionExerciseId: Long, val setIndex: Int, val plannedReps: Int, val plannedLoadKg: Double? = null,
    val actualReps: Int? = null, val actualLoadKg: Double? = null, val status: String, val completedAtWallMs: Long? = null,
)

@Serializable
data class AssessmentDto(
    val id: Long, val dateEpochDay: Long,
    val weightKg: Double? = null, val bodyFatPct: Double? = null, val neckCm: Double? = null, val shouldersCm: Double? = null,
    val chestCm: Double? = null, val waistCm: Double? = null, val abdomenCm: Double? = null, val hipsCm: Double? = null,
    val armRightCm: Double? = null, val armLeftCm: Double? = null, val forearmRightCm: Double? = null, val forearmLeftCm: Double? = null,
    val thighRightCm: Double? = null, val thighLeftCm: Double? = null, val calfRightCm: Double? = null, val calfLeftCm: Double? = null,
    val notes: String? = null, val createdAtMs: Long, val updatedAtMs: Long,
)

@Serializable
data class PhotoDto(val id: Long, val dateEpochDay: Long, val imagePath: String, val pose: String, val createdAtMs: Long)
