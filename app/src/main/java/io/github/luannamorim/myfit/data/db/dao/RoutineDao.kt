package io.github.luannamorim.myfit.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import io.github.luannamorim.myfit.data.db.entity.RoutineEntity
import io.github.luannamorim.myfit.data.db.entity.RoutineExerciseEntity
import io.github.luannamorim.myfit.data.db.entity.RoutineOverviewRow
import io.github.luannamorim.myfit.data.db.entity.RoutineWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Query(
        """
        SELECT r.id, r.name, r.description, r.position,
            (SELECT COUNT(*) FROM routine_exercise re WHERE re.routineId = r.id) AS exerciseCount,
            (SELECT MAX(s.startDateEpochDay) FROM workout_session s
                WHERE s.routineId = r.id AND s.status = 'FINISHED') AS lastDoneEpochDay
        FROM routine r
        ORDER BY r.position, r.id
        """,
    )
    fun observeOverviews(): Flow<List<RoutineOverviewRow>>

    @Transaction
    @Query("SELECT * FROM routine WHERE id = :id")
    fun observeWithItems(id: Long): Flow<RoutineWithItems?>

    @Transaction
    @Query("SELECT * FROM routine WHERE id = :id")
    suspend fun getWithItems(id: Long): RoutineWithItems?

    @Query("SELECT * FROM routine ORDER BY id")
    suspend fun getAllRoutines(): List<RoutineEntity>

    @Query("SELECT * FROM routine_exercise ORDER BY id")
    suspend fun getAllItems(): List<RoutineExerciseEntity>

    @Query("SELECT COALESCE(MAX(position), -1) FROM routine")
    suspend fun maxPosition(): Int

    @Insert
    suspend fun insertRoutine(routine: RoutineEntity): Long

    @Update
    suspend fun updateRoutine(routine: RoutineEntity)

    @Query("DELETE FROM routine WHERE id = :id")
    suspend fun deleteRoutine(id: Long)

    @Insert
    suspend fun insertItems(items: List<RoutineExerciseEntity>)

    @Query("DELETE FROM routine_exercise WHERE routineId = :routineId")
    suspend fun deleteItems(routineId: Long)

    @Query("UPDATE routine_exercise SET loadKg = :loadKg WHERE routineId = :routineId AND exerciseId = :exerciseId")
    suspend fun updateLoad(routineId: Long, exerciseId: Long, loadKg: Double?)
}
