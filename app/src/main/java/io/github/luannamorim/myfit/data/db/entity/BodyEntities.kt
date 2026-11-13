package io.github.luannamorim.myfit.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.luannamorim.myfit.domain.model.PhotoPose

@Entity(tableName = "body_assessment", indices = [Index("dateEpochDay")])
data class BodyAssessmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val weightKg: Double? = null,
    val bodyFatPct: Double? = null,
    val neckCm: Double? = null,
    val shouldersCm: Double? = null,
    val chestCm: Double? = null,
    val waistCm: Double? = null,
    val abdomenCm: Double? = null,
    val hipsCm: Double? = null,
    val armRightCm: Double? = null,
    val armLeftCm: Double? = null,
    val forearmRightCm: Double? = null,
    val forearmLeftCm: Double? = null,
    val thighRightCm: Double? = null,
    val thighLeftCm: Double? = null,
    val calfRightCm: Double? = null,
    val calfLeftCm: Double? = null,
    val notes: String? = null,
    val createdAtMs: Long,
    val updatedAtMs: Long,
    /** v6. Last column, like the ALTER TABLE that added it. */
    val heightCm: Double? = null,
)

@Entity(tableName = "body_photo", indices = [Index("dateEpochDay")])
data class BodyPhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val imagePath: String,
    val pose: PhotoPose,
    val createdAtMs: Long,
)
