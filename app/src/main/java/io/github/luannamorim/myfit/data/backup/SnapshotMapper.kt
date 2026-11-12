package io.github.luannamorim.myfit.data.backup

import androidx.room.withTransaction
import io.github.luannamorim.myfit.core.time.Mark
import io.github.luannamorim.myfit.data.db.MyFitDatabase
import io.github.luannamorim.myfit.data.db.entity.BodyAssessmentEntity
import io.github.luannamorim.myfit.data.db.entity.BodyPhotoEntity
import io.github.luannamorim.myfit.data.db.entity.ExerciseEntity
import io.github.luannamorim.myfit.data.db.entity.RoutineEntity
import io.github.luannamorim.myfit.data.db.entity.RoutineExerciseEntity
import io.github.luannamorim.myfit.data.db.entity.SessionExerciseEntity
import io.github.luannamorim.myfit.data.db.entity.SessionSetEntity
import io.github.luannamorim.myfit.data.db.entity.WorkoutSessionEntity
import io.github.luannamorim.myfit.data.db.seed.ExerciseSeed
import io.github.luannamorim.myfit.domain.backup.AssessmentDto
import io.github.luannamorim.myfit.domain.backup.BackupDataV1
import io.github.luannamorim.myfit.domain.backup.ExerciseDto
import io.github.luannamorim.myfit.domain.backup.MarkDto
import io.github.luannamorim.myfit.domain.backup.PhotoDto
import io.github.luannamorim.myfit.domain.backup.RoutineDto
import io.github.luannamorim.myfit.domain.backup.RoutineExerciseDto
import io.github.luannamorim.myfit.domain.backup.SessionDto
import io.github.luannamorim.myfit.domain.backup.SessionExerciseDto
import io.github.luannamorim.myfit.domain.backup.SessionSetDto

/** Converts the whole database to/from the versioned backup DTOs. */
object SnapshotMapper {

    suspend fun export(db: MyFitDatabase): BackupDataV1 = db.withTransaction {
        BackupDataV1(
            exercises = db.exerciseDao().getAll().map { it.toDto() },
            routines = db.routineDao().getAllRoutines().map { it.toDto() },
            routineExercises = db.routineDao().getAllItems().map { it.toDto() },
            sessions = db.sessionDao().getAllSessions().map { it.toDto() },
            sessionExercises = db.sessionDao().getAllExercises().map { it.toDto() },
            sessionSets = db.sessionDao().getAllSets().map { it.toDto() },
            assessments = db.bodyDao().getAllAssessments().map { it.toDto() },
            photos = db.bodyDao().getAllPhotos().map { it.toDto() },
        )
    }

    /**
     * Replaces every table with [data] in a single transaction, preserving ids. Backups made before
     * library keys existed get their library rows linked by name, as the 2 → 3 migration does.
     */
    suspend fun replaceAll(db: MyFitDatabase, data: BackupDataV1) {
        val dao = db.backupDao()
        db.withTransaction {
            dao.clearSessionSets()
            dao.clearSessionExercises()
            dao.clearSessions()
            dao.clearRoutineExercises()
            dao.clearRoutines()
            dao.clearExercises()
            dao.clearPhotos()
            dao.clearAssessments()

            dao.insertExercises(data.exercises.map { it.toEntity() })
            dao.insertRoutines(data.routines.map { it.toEntity() })
            dao.insertRoutineExercises(data.routineExercises.map { it.toEntity() })
            dao.insertSessions(data.sessions.map { it.toEntity() })
            dao.insertSessionExercises(data.sessionExercises.map { it.toEntity() })
            dao.insertSessionSets(data.sessionSets.map { it.toEntity() })
            dao.insertAssessments(data.assessments.map { it.toEntity() })
            dao.insertPhotos(data.photos.map { it.toEntity() })
            ExerciseSeed.linkKeys(db.openHelper.writableDatabase)
        }
    }

    private fun Mark.toDto() = MarkDto(wallMs, elapsedMs, bootCount)
    private fun MarkDto.toMark() = Mark(wallMs, elapsedMs, bootCount)

    private fun ExerciseEntity.toDto() = ExerciseDto(id, name, muscleGroup.name, movementType.name, imagePath, notes, isCustom, archived, createdAtMs, seedKey)
    private fun ExerciseDto.toEntity() = ExerciseEntity(id, name, enumOf(muscleGroup), enumOf(movementType), imagePath, notes, isCustom, archived, createdAtMs, seedKey)

    private fun RoutineEntity.toDto() = RoutineDto(id, name, description, position, createdAtMs, updatedAtMs)
    private fun RoutineDto.toEntity() = RoutineEntity(id, name, description, position, createdAtMs, updatedAtMs)

    private fun RoutineExerciseEntity.toDto() = RoutineExerciseDto(id, routineId, exerciseId, position, sets, reps, restSeconds, loadKg, notes)
    private fun RoutineExerciseDto.toEntity() = RoutineExerciseEntity(id, routineId, exerciseId, position, sets, reps, restSeconds, loadKg, notes)

    private fun WorkoutSessionEntity.toDto() = SessionDto(
        id, routineId, routineName, status.name, started.toDto(), paused?.toDto(), totalPausedMs, ended?.toDto(), durationMs,
        cursorExercise, cursorSet, restStarted?.toDto(), restDurationMs, startDateEpochDay,
    )
    private fun SessionDto.toEntity() = WorkoutSessionEntity(
        id, routineId, routineName, enumOf(status), started.toMark(), paused?.toMark(), totalPausedMs, ended?.toMark(), durationMs,
        cursorExercise, cursorSet, restStarted?.toMark(), restDurationMs, startDateEpochDay,
    )

    private fun SessionExerciseEntity.toDto() = SessionExerciseDto(
        id, sessionId, exerciseId, position, name, muscleGroup.name, movementType.name, imagePath, restSeconds, notes, status.name, seedKey,
    )
    private fun SessionExerciseDto.toEntity() = SessionExerciseEntity(
        id, sessionId, exerciseId, position, name, enumOf(muscleGroup), enumOf(movementType), imagePath, restSeconds, notes, enumOf(status), seedKey,
    )

    private fun SessionSetEntity.toDto() = SessionSetDto(id, sessionExerciseId, setIndex, plannedReps, plannedLoadKg, actualReps, actualLoadKg, status.name, completedAtWallMs)
    private fun SessionSetDto.toEntity() = SessionSetEntity(id, sessionExerciseId, setIndex, plannedReps, plannedLoadKg, actualReps, actualLoadKg, enumOf(status), completedAtWallMs)

    private fun BodyAssessmentEntity.toDto() = AssessmentDto(
        id, dateEpochDay, weightKg, bodyFatPct, neckCm, shouldersCm, chestCm, waistCm, abdomenCm, hipsCm,
        armRightCm, armLeftCm, forearmRightCm, forearmLeftCm, thighRightCm, thighLeftCm, calfRightCm, calfLeftCm, notes, createdAtMs, updatedAtMs,
    )
    private fun AssessmentDto.toEntity() = BodyAssessmentEntity(
        id, dateEpochDay, weightKg, bodyFatPct, neckCm, shouldersCm, chestCm, waistCm, abdomenCm, hipsCm,
        armRightCm, armLeftCm, forearmRightCm, forearmLeftCm, thighRightCm, thighLeftCm, calfRightCm, calfLeftCm, notes, createdAtMs, updatedAtMs,
    )

    private fun BodyPhotoEntity.toDto() = PhotoDto(id, dateEpochDay, imagePath, pose.name, createdAtMs)
    private fun PhotoDto.toEntity() = BodyPhotoEntity(id, dateEpochDay, imagePath, enumOf(pose), createdAtMs)

    private inline fun <reified E : Enum<E>> enumOf(name: String): E =
        enumValues<E>().firstOrNull { it.name == name } ?: throw IllegalArgumentException("unknown ${E::class.simpleName}: $name")
}
