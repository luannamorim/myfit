package io.github.luannamorim.myfit.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.github.luannamorim.myfit.data.db.entity.SessionExerciseEntity
import io.github.luannamorim.myfit.data.db.entity.SessionSetEntity
import io.github.luannamorim.myfit.data.db.entity.SessionWithExercises
import io.github.luannamorim.myfit.data.db.entity.WorkoutSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Transaction
    @Query("SELECT * FROM workout_session WHERE status = 'ACTIVE' ORDER BY id DESC LIMIT 1")
    fun observeActive(): Flow<SessionWithExercises?>

    @Transaction
    @Query("SELECT * FROM workout_session WHERE status = 'ACTIVE' ORDER BY id DESC LIMIT 1")
    suspend fun getActive(): SessionWithExercises?

    @Transaction
    @Query("SELECT * FROM workout_session WHERE id = :id")
    fun observeSession(id: Long): Flow<SessionWithExercises?>

    @Transaction
    @Query("SELECT * FROM workout_session WHERE id = :id")
    suspend fun getSession(id: Long): SessionWithExercises?

    @Transaction
    @Query(
        """
        SELECT * FROM workout_session
        WHERE status = 'FINISHED' AND startDateEpochDay BETWEEN :fromEpochDay AND :toEpochDay
        ORDER BY started_wallMs DESC
        """,
    )
    fun observeFinished(fromEpochDay: Long, toEpochDay: Long): Flow<List<SessionWithExercises>>

    @Transaction
    @Query("SELECT * FROM workout_session WHERE status = 'FINISHED' ORDER BY started_wallMs DESC LIMIT 1")
    fun observeLastFinished(): Flow<SessionWithExercises?>

    @Query("SELECT * FROM workout_session ORDER BY id")
    suspend fun getAllSessions(): List<WorkoutSessionEntity>

    @Query("SELECT * FROM session_exercise ORDER BY id")
    suspend fun getAllExercises(): List<SessionExerciseEntity>

    @Query("SELECT * FROM session_set ORDER BY id")
    suspend fun getAllSets(): List<SessionSetEntity>

    @Insert
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Insert
    suspend fun insertExercise(exercise: SessionExerciseEntity): Long

    @Insert
    suspend fun insertSets(sets: List<SessionSetEntity>)

    @Update
    suspend fun updateSession(session: WorkoutSessionEntity)

    @Update
    suspend fun updateExercises(exercises: List<SessionExerciseEntity>)

    @Update
    suspend fun updateSets(sets: List<SessionSetEntity>)

    @Query("DELETE FROM workout_session WHERE id = :id")
    suspend fun deleteSession(id: Long)

    /** [intensity] is an [io.github.luannamorim.myfit.domain.model.Intensity] name, or null to clear it. */
    @Query("UPDATE workout_session SET intensity = :intensity WHERE id = :id AND status = 'FINISHED'")
    suspend fun setIntensity(id: Long, intensity: String?)

    @Query("UPDATE workout_session SET imagePath = :imagePath WHERE id = :id AND status = 'FINISHED'")
    suspend fun setImagePath(id: Long, imagePath: String?)
}
