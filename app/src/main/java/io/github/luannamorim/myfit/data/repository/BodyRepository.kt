package io.github.luannamorim.myfit.data.repository

import io.github.luannamorim.myfit.core.time.TimeSource
import io.github.luannamorim.myfit.data.db.dao.BodyDao
import io.github.luannamorim.myfit.data.db.entity.BodyAssessmentEntity
import io.github.luannamorim.myfit.data.db.entity.BodyPhotoEntity
import io.github.luannamorim.myfit.domain.body.BodyAssessment
import io.github.luannamorim.myfit.domain.body.BodyPhoto
import io.github.luannamorim.myfit.domain.body.Measure
import io.github.luannamorim.myfit.domain.model.PhotoPose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BodyRepository @Inject constructor(
    private val dao: BodyDao,
    private val time: TimeSource,
    private val backup: BackupTrigger,
) {
    /** Newest first. */
    fun observeAssessments(): Flow<List<BodyAssessment>> = dao.observeAssessments().map { list -> list.map { it.toDomain() } }

    suspend fun get(id: Long): BodyAssessment? = dao.getAssessment(id)?.toDomain()

    suspend fun save(assessment: BodyAssessment): Long {
        val now = time.now().wallMs
        val id = if (assessment.id == 0L) {
            dao.insertAssessment(assessment.toEntity(createdAt = now, updatedAt = now))
        } else {
            val current = checkNotNull(dao.getAssessment(assessment.id)) { "assessment ${assessment.id} not found" }
            dao.updateAssessment(assessment.toEntity(createdAt = current.createdAtMs, updatedAt = now))
            assessment.id
        }
        backup.onDataChanged()
        return id
    }

    suspend fun delete(id: Long) {
        dao.deleteAssessment(id)
        backup.onDataChanged()
    }

    fun observePhotos(): Flow<List<BodyPhoto>> = dao.observePhotos().map { list -> list.map { it.toDomain() } }

    suspend fun getPhoto(id: Long): BodyPhoto? = dao.getPhoto(id)?.toDomain()

    suspend fun addPhoto(date: LocalDate, pose: PhotoPose, relativePath: String): Long {
        val id = dao.insertPhoto(BodyPhotoEntity(dateEpochDay = date.toEpochDay(), imagePath = relativePath, pose = pose, createdAtMs = time.now().wallMs))
        backup.onDataChanged()
        return id
    }

    /** Deletes the row and returns the image path so the caller can remove the file. */
    suspend fun deletePhoto(id: Long): String? {
        val photo = dao.getPhoto(id) ?: return null
        dao.deletePhoto(id)
        backup.onDataChanged()
        return photo.imagePath
    }
}

private fun BodyPhotoEntity.toDomain() = BodyPhoto(id, LocalDate.ofEpochDay(dateEpochDay), imagePath, pose)

internal fun BodyAssessmentEntity.toDomain(): BodyAssessment {
    val values = buildMap {
        weightKg?.let { put(Measure.WEIGHT, it) }
        heightCm?.let { put(Measure.HEIGHT, it) }
        bodyFatPct?.let { put(Measure.BODY_FAT, it) }
        neckCm?.let { put(Measure.NECK, it) }
        shouldersCm?.let { put(Measure.SHOULDERS, it) }
        chestCm?.let { put(Measure.CHEST, it) }
        waistCm?.let { put(Measure.WAIST, it) }
        abdomenCm?.let { put(Measure.ABDOMEN, it) }
        hipsCm?.let { put(Measure.HIPS, it) }
        armRightCm?.let { put(Measure.ARM_RIGHT, it) }
        armLeftCm?.let { put(Measure.ARM_LEFT, it) }
        forearmRightCm?.let { put(Measure.FOREARM_RIGHT, it) }
        forearmLeftCm?.let { put(Measure.FOREARM_LEFT, it) }
        thighRightCm?.let { put(Measure.THIGH_RIGHT, it) }
        thighLeftCm?.let { put(Measure.THIGH_LEFT, it) }
        calfRightCm?.let { put(Measure.CALF_RIGHT, it) }
        calfLeftCm?.let { put(Measure.CALF_LEFT, it) }
    }
    return BodyAssessment(id, LocalDate.ofEpochDay(dateEpochDay), values, notes)
}

private fun BodyAssessment.toEntity(createdAt: Long, updatedAt: Long) = BodyAssessmentEntity(
    id = id,
    dateEpochDay = date.toEpochDay(),
    weightKg = values[Measure.WEIGHT],
    heightCm = values[Measure.HEIGHT],
    // Only what the user typed is stored; BMI has no column and an estimate is recalculated.
    bodyFatPct = values[Measure.BODY_FAT]?.takeIf { Measure.BODY_FAT !in estimated },
    neckCm = values[Measure.NECK],
    shouldersCm = values[Measure.SHOULDERS],
    chestCm = values[Measure.CHEST],
    waistCm = values[Measure.WAIST],
    abdomenCm = values[Measure.ABDOMEN],
    hipsCm = values[Measure.HIPS],
    armRightCm = values[Measure.ARM_RIGHT],
    armLeftCm = values[Measure.ARM_LEFT],
    forearmRightCm = values[Measure.FOREARM_RIGHT],
    forearmLeftCm = values[Measure.FOREARM_LEFT],
    thighRightCm = values[Measure.THIGH_RIGHT],
    thighLeftCm = values[Measure.THIGH_LEFT],
    calfRightCm = values[Measure.CALF_RIGHT],
    calfLeftCm = values[Measure.CALF_LEFT],
    notes = notes?.trim()?.ifEmpty { null },
    createdAtMs = createdAt,
    updatedAtMs = updatedAt,
)
