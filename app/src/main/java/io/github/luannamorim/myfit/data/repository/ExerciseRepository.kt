package io.github.luannamorim.myfit.data.repository

import io.github.luannamorim.myfit.core.format.SearchText
import io.github.luannamorim.myfit.core.time.TimeSource
import io.github.luannamorim.myfit.data.db.dao.ExerciseDao
import io.github.luannamorim.myfit.data.db.entity.ExerciseEntity
import io.github.luannamorim.myfit.data.db.seed.ExerciseSeed
import io.github.luannamorim.myfit.domain.model.Exercise
import io.github.luannamorim.myfit.domain.model.ExerciseMeasure
import io.github.luannamorim.myfit.domain.model.MuscleGroup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ExerciseDeleteResult { DELETED, ARCHIVED }

@Singleton
class ExerciseRepository @Inject constructor(
    private val dao: ExerciseDao,
    private val time: TimeSource,
    private val backup: BackupTrigger,
) {
    /**
     * Active (non-archived) library, filtered by accent-insensitive [query] and [groups] (empty = all).
     * Favorites come first, then alphabetical.
     */
    fun observeLibrary(query: String = "", groups: Set<MuscleGroup> = emptySet()): Flow<List<Exercise>> =
        dao.observeActive().map { list ->
            list.asSequence()
                .filter { groups.isEmpty() || it.muscleGroup in groups }
                .map { it.toDomain() }
                .filter { SearchText.matches(it.name, query) } // the shown name (library names follow the app's language)
                .sortedWith(compareByDescending<Exercise> { it.favorite }.thenBy { SearchText.normalize(it.name) })
                .toList()
        }

    suspend fun get(id: Long): Exercise? = dao.get(id)?.toDomain()

    suspend fun getAll(ids: List<Long>): List<Exercise> {
        val byId = dao.getAll(ids).associateBy { it.id }
        return ids.mapNotNull { byId[it]?.toDomain() }
    }

    /** Inserts a new custom exercise (id == 0) or updates an existing one. Returns its id. */
    suspend fun save(exercise: Exercise): Long {
        val name = exercise.name.trim()
        require(name.isNotEmpty()) { "name required" }
        val id = if (exercise.id == 0L) {
            dao.insert(
                ExerciseEntity(
                    name = name,
                    muscleGroup = exercise.muscleGroup,
                    movementType = exercise.movementType,
                    imagePath = exercise.imagePath,
                    notes = exercise.notes?.trim()?.ifEmpty { null },
                    isCustom = true,
                    createdAtMs = time.now().wallMs,
                    measure = exercise.measure,
                    favorite = exercise.favorite,
                ),
            )
        } else {
            val current = checkNotNull(dao.get(exercise.id)) { "exercise ${exercise.id} not found" }
            dao.update(
                current.copy(
                    // A library exercise can't be renamed: its stored name stays, the official one is shown.
                    name = if (ExerciseSeed.nameFor(current.seedKey) != null) current.name else name,
                    muscleGroup = exercise.muscleGroup,
                    movementType = exercise.movementType,
                    imagePath = exercise.imagePath,
                    notes = exercise.notes?.trim()?.ifEmpty { null },
                    measure = exercise.measure,
                    favorite = exercise.favorite,
                ),
            )
            exercise.id
        }
        backup.onDataChanged()
        return id
    }

    suspend fun setFavorite(id: Long, favorite: Boolean) {
        dao.setFavorite(id, favorite)
        backup.onDataChanged()
    }

    /** The one thing a library exercise lets the user change: whether its sets count reps or time. */
    suspend fun setMeasure(id: Long, measure: ExerciseMeasure) {
        dao.setMeasure(id, measure)
        backup.onDataChanged()
    }

    /**
     * Deletes the exercise, or archives it when a routine still references it. Library exercises stay:
     * returns null without touching them.
     */
    suspend fun delete(id: Long): ExerciseDeleteResult? {
        val current = dao.get(id) ?: return null
        if (ExerciseSeed.forKey(current.seedKey) != null) return null
        val result = if (dao.routineUsageCount(id) > 0) {
            dao.update(current.copy(archived = true))
            ExerciseDeleteResult.ARCHIVED
        } else {
            dao.delete(id)
            ExerciseDeleteResult.DELETED
        }
        backup.onDataChanged()
        return result
    }
}
