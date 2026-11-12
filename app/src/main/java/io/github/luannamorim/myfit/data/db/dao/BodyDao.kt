package io.github.luannamorim.myfit.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.github.luannamorim.myfit.data.db.entity.BodyAssessmentEntity
import io.github.luannamorim.myfit.data.db.entity.BodyPhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyDao {
    @Query("SELECT * FROM body_assessment ORDER BY dateEpochDay DESC, id DESC")
    fun observeAssessments(): Flow<List<BodyAssessmentEntity>>

    @Query("SELECT * FROM body_assessment WHERE id = :id")
    suspend fun getAssessment(id: Long): BodyAssessmentEntity?

    @Query("SELECT * FROM body_assessment ORDER BY id")
    suspend fun getAllAssessments(): List<BodyAssessmentEntity>

    @Insert
    suspend fun insertAssessment(assessment: BodyAssessmentEntity): Long

    @Update
    suspend fun updateAssessment(assessment: BodyAssessmentEntity)

    @Query("DELETE FROM body_assessment WHERE id = :id")
    suspend fun deleteAssessment(id: Long)

    @Query("SELECT * FROM body_photo ORDER BY dateEpochDay DESC, id DESC")
    fun observePhotos(): Flow<List<BodyPhotoEntity>>

    @Query("SELECT * FROM body_photo WHERE id = :id")
    suspend fun getPhoto(id: Long): BodyPhotoEntity?

    @Query("SELECT * FROM body_photo ORDER BY id")
    suspend fun getAllPhotos(): List<BodyPhotoEntity>

    @Insert
    suspend fun insertPhoto(photo: BodyPhotoEntity): Long

    @Query("DELETE FROM body_photo WHERE id = :id")
    suspend fun deletePhoto(id: Long)
}
