package io.github.luannamorim.myfit.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.github.luannamorim.myfit.data.db.entity.ActivityEntity
import io.github.luannamorim.myfit.data.db.entity.BodyAssessmentEntity
import io.github.luannamorim.myfit.data.db.entity.BodyPhotoEntity
import io.github.luannamorim.myfit.data.db.entity.ExerciseEntity
import io.github.luannamorim.myfit.data.db.entity.RoutineEntity
import io.github.luannamorim.myfit.data.db.entity.RoutineExerciseEntity
import io.github.luannamorim.myfit.data.db.entity.SessionExerciseEntity
import io.github.luannamorim.myfit.data.db.entity.SessionSetEntity
import io.github.luannamorim.myfit.data.db.entity.WorkoutSessionEntity

/** Bulk operations used only by backup/restore (always inside a transaction). */
@Dao
interface BackupDao {
    @Query("DELETE FROM session_set") suspend fun clearSessionSets()
    @Query("DELETE FROM session_exercise") suspend fun clearSessionExercises()
    @Query("DELETE FROM workout_session") suspend fun clearSessions()
    @Query("DELETE FROM routine_exercise") suspend fun clearRoutineExercises()
    @Query("DELETE FROM routine") suspend fun clearRoutines()
    @Query("DELETE FROM exercise") suspend fun clearExercises()
    @Query("DELETE FROM body_photo") suspend fun clearPhotos()
    @Query("DELETE FROM body_assessment") suspend fun clearAssessments()
    @Query("DELETE FROM activity") suspend fun clearActivities()

    @Insert suspend fun insertExercises(items: List<ExerciseEntity>)
    @Insert suspend fun insertRoutines(items: List<RoutineEntity>)
    @Insert suspend fun insertRoutineExercises(items: List<RoutineExerciseEntity>)
    @Insert suspend fun insertSessions(items: List<WorkoutSessionEntity>)
    @Insert suspend fun insertSessionExercises(items: List<SessionExerciseEntity>)
    @Insert suspend fun insertSessionSets(items: List<SessionSetEntity>)
    @Insert suspend fun insertAssessments(items: List<BodyAssessmentEntity>)
    @Insert suspend fun insertPhotos(items: List<BodyPhotoEntity>)
    @Insert suspend fun insertActivities(items: List<ActivityEntity>)
}
