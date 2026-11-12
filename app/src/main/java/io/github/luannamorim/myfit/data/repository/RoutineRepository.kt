package io.github.luannamorim.myfit.data.repository

import androidx.room.withTransaction
import io.github.luannamorim.myfit.core.time.TimeSource
import io.github.luannamorim.myfit.data.db.MyFitDatabase
import io.github.luannamorim.myfit.data.db.entity.RoutineEntity
import io.github.luannamorim.myfit.data.db.entity.RoutineExerciseEntity
import io.github.luannamorim.myfit.domain.model.Routine
import io.github.luannamorim.myfit.domain.model.RoutineOverview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoutineRepository @Inject constructor(
    private val db: MyFitDatabase,
    private val time: TimeSource,
    private val backup: BackupTrigger,
) {
    private val dao = db.routineDao()

    fun observeOverviews(): Flow<List<RoutineOverview>> = dao.observeOverviews().map { rows -> rows.map { it.toDomain() } }

    fun observe(id: Long): Flow<Routine?> = dao.observeWithItems(id).map { it?.toDomain() }

    suspend fun get(id: Long): Routine? = dao.getWithItems(id)?.toDomain()

    /** Inserts (id == 0) or updates the routine and replaces its items, positions 0..n-1. */
    suspend fun save(routine: Routine): Long {
        val name = routine.name.trim()
        require(name.isNotEmpty()) { "name required" }
        val now = time.now().wallMs
        val id = db.withTransaction {
            val routineId = if (routine.id == 0L) {
                dao.insertRoutine(
                    RoutineEntity(
                        name = name,
                        description = routine.description?.trim()?.ifEmpty { null },
                        position = dao.maxPosition() + 1,
                        createdAtMs = now,
                        updatedAtMs = now,
                    ),
                )
            } else {
                val current = checkNotNull(dao.getWithItems(routine.id)) { "routine ${routine.id} not found" }.routine
                dao.updateRoutine(current.copy(name = name, description = routine.description?.trim()?.ifEmpty { null }, updatedAtMs = now))
                dao.deleteItems(routine.id)
                routine.id
            }
            dao.insertItems(
                routine.items.mapIndexed { index, item ->
                    RoutineExerciseEntity(
                        routineId = routineId,
                        exerciseId = item.exercise.id,
                        position = index,
                        sets = item.sets,
                        reps = item.reps,
                        restSeconds = item.restSeconds,
                        loadKg = item.loadKg,
                        notes = item.notes?.trim()?.ifEmpty { null },
                    )
                },
            )
            routineId
        }
        backup.onDataChanged()
        return id
    }

    /** Copies the routine and its items as "<name> <copySuffix>" (e.g. "(copy)"). */
    suspend fun duplicate(id: Long, copySuffix: String): Long {
        val source = checkNotNull(get(id)) { "routine $id not found" }
        return save(source.copy(id = 0, name = "${source.name} $copySuffix", items = source.items.map { it.copy(id = 0) }))
    }

    suspend fun delete(id: Long) {
        dao.deleteRoutine(id)
        backup.onDataChanged()
    }

    /** Sets the planned load per exercise (key = exercise id). */
    suspend fun updateLoads(routineId: Long, loads: Map<Long, Double?>) {
        if (loads.isEmpty()) return
        db.withTransaction {
            loads.forEach { (exerciseId, load) -> dao.updateLoad(routineId, exerciseId, load) }
        }
    }
}
