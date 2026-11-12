package io.github.luannamorim.myfit.data.repository

import io.github.luannamorim.myfit.core.time.TimeSource
import io.github.luannamorim.myfit.data.db.MyFitDatabase
import io.github.luannamorim.myfit.data.db.entity.ActivityEntity
import io.github.luannamorim.myfit.domain.activity.Activity
import io.github.luannamorim.myfit.domain.activity.Sport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityRepository @Inject constructor(
    db: MyFitDatabase,
    private val time: TimeSource,
    private val backup: BackupTrigger,
) {
    private val dao = db.activityDao()

    /** Activities whose date is in [range], newest first. */
    fun observe(range: ClosedRange<LocalDate>): Flow<List<Activity>> =
        dao.observeRange(range.start.toEpochDay(), range.endInclusive.toEpochDay()).map { list -> list.map { it.toDomain() } }

    suspend fun get(id: Long): Activity? = dao.get(id)?.toDomain()

    /** Inserts (id 0) or updates. Returns the id. */
    suspend fun save(activity: Activity): Long {
        val clean = activity.copy(
            name = activity.name?.trim()?.takeIf { it.isNotEmpty() && activity.sport == Sport.OTHER },
            notes = activity.notes?.trim()?.takeIf { it.isNotEmpty() },
            distanceKm = activity.distanceKm?.takeIf { activity.sport.distance != null && it > 0 },
        )
        val id = if (clean.id == 0L) {
            dao.insert(clean.toEntity(createdAtMs = time.now().wallMs))
        } else {
            val existing = dao.get(clean.id) ?: return dao.insert(clean.copy(id = 0).toEntity(createdAtMs = time.now().wallMs))
            dao.update(clean.toEntity(createdAtMs = existing.createdAtMs))
            clean.id
        }
        backup.onDataChanged()
        return id
    }

    /** Deletes the activity and returns its image path (for the caller to delete the file). */
    suspend fun delete(id: Long): String? {
        val existing = dao.get(id) ?: return null
        dao.delete(id)
        backup.onDataChanged()
        return existing.imagePath
    }

    private fun ActivityEntity.toDomain() = Activity(
        id = id,
        sport = sport,
        name = name,
        date = LocalDate.ofEpochDay(dateEpochDay),
        durationMinutes = durationMinutes,
        distanceKm = distanceKm,
        intensity = intensity,
        notes = notes,
        imagePath = imagePath,
    )

    private fun Activity.toEntity(createdAtMs: Long) = ActivityEntity(
        id = id,
        sport = sport,
        name = name,
        dateEpochDay = date.toEpochDay(),
        durationMinutes = durationMinutes,
        distanceKm = distanceKm,
        intensity = intensity,
        notes = notes,
        imagePath = imagePath,
        createdAtMs = createdAtMs,
    )
}
