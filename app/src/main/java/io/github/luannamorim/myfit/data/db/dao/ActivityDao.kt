package io.github.luannamorim.myfit.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.github.luannamorim.myfit.data.db.entity.ActivityEntity
import io.github.luannamorim.myfit.domain.activity.Sport
import kotlinx.coroutines.flow.Flow

data class SportLastDayRow(val sport: Sport, val name: String?, val epochDay: Long)

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activity WHERE dateEpochDay BETWEEN :fromEpochDay AND :toEpochDay ORDER BY dateEpochDay DESC, id DESC")
    fun observeRange(fromEpochDay: Long, toEpochDay: Long): Flow<List<ActivityEntity>>

    /** Latest day per sport and name (see [SportLastDayRow]). */
    @Query("SELECT sport, name, MAX(dateEpochDay) AS epochDay FROM activity GROUP BY sport, name")
    fun observeLastDays(): Flow<List<SportLastDayRow>>

    @Query("SELECT * FROM activity WHERE id = :id")
    suspend fun get(id: Long): ActivityEntity?

    @Query("SELECT * FROM activity ORDER BY id")
    suspend fun getAll(): List<ActivityEntity>

    @Insert
    suspend fun insert(activity: ActivityEntity): Long

    @Update
    suspend fun update(activity: ActivityEntity)

    @Query("DELETE FROM activity WHERE id = :id")
    suspend fun delete(id: Long)
}
