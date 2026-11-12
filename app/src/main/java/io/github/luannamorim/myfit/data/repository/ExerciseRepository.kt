package io.github.luannamorim.myfit.data.repository

import io.github.luannamorim.myfit.core.format.SearchText
import io.github.luannamorim.myfit.core.time.TimeSource
import io.github.luannamorim.myfit.data.db.dao.ExerciseDao
import io.github.luannamorim.myfit.data.db.entity.ExerciseEntity
import io.github.luannamorim.myfit.domain.model.Exercise
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
    /** Active (non-archived) library, filtered by accent-insensitive [query] and [groups] (empty = all). */
    fun observeLibrary(query: String = "", groups: Set<MuscleGroup> = emptySet()): Flow<List<Exercise>> =
        dao.observeActive().map { list ->
            list.asSequence()
                .filter { groups.isEmpty() || it.muscleGroup in groups }
                .filter { SearchText.matches(it.name, query) }
                .map { it.toDomain() }
                .sortedBy { SearchText.normalize(it.name) }
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
                ),
            )
        } else {
            val current = checkNotNull(dao.get(exercise.id)) { "exercise ${exercise.id} not found" }
            dao.update(
                current.copy(
                    name = name,
                    muscleGroup = exercise.muscleGroup,
                    movementType = exercise.movementType,
                    imagePath = exercise.imagePath,
                    notes = exercise.notes?.trim()?.ifEmpty { null },
                ),
            )
            exercise.id
        }
        backup.onDataChanged()
        return id
    }

    /** Deletes the exercise, or archives it when a routine still references it. */
    suspend fun delete(id: Long): ExerciseDeleteResult {
        val result = if (dao.routineUsageCount(id) > 0) {
            dao.get(id)?.let { dao.update(it.copy(archived = true)) }
            ExerciseDeleteResult.ARCHIVED
        } else {
            dao.delete(id)
            ExerciseDeleteResult.DELETED
        }
        backup.onDataChanged()
        return result
    }
}
