package io.github.luannamorim.myfit.testutil

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.luannamorim.myfit.core.time.Mark
import io.github.luannamorim.myfit.data.db.MyFitDatabase
import io.github.luannamorim.myfit.data.db.entity.ExerciseEntity
import io.github.luannamorim.myfit.data.db.entity.RoutineEntity
import io.github.luannamorim.myfit.data.db.entity.RoutineExerciseEntity
import io.github.luannamorim.myfit.data.db.seed.ExerciseSeed
import io.github.luannamorim.myfit.domain.model.ExerciseMeasure
import io.github.luannamorim.myfit.domain.model.MovementType
import io.github.luannamorim.myfit.domain.model.MuscleGroup

fun inMemoryDb(seed: Boolean = false): MyFitDatabase {
    val context = ApplicationProvider.getApplicationContext<Context>()
    return Room.inMemoryDatabaseBuilder(context, MyFitDatabase::class.java)
        .apply { if (seed) addCallback(ExerciseSeed.Callback()) }
        .allowMainThreadQueries()
        .build()
}

val T0 = Mark(wallMs = 1_731_400_000_000, elapsedMs = 10_000_000, bootCount = 1)

suspend fun MyFitDatabase.insertExercise(
    name: String,
    group: MuscleGroup = MuscleGroup.BACK,
    type: MovementType = MovementType.PULL,
    custom: Boolean = true,
    measure: ExerciseMeasure = ExerciseMeasure.REPS,
): Long = exerciseDao().insert(
    ExerciseEntity(name = name, muscleGroup = group, movementType = type, isCustom = custom, createdAtMs = 0, measure = measure),
)

/** Inserts a routine with one item per exercise id (3x12, 90s). */
suspend fun MyFitDatabase.insertRoutine(name: String, exerciseIds: List<Long>, position: Int = 0): Long {
    val id = routineDao().insertRoutine(RoutineEntity(name = name, position = position, createdAtMs = 0, updatedAtMs = 0))
    routineDao().insertItems(
        exerciseIds.mapIndexed { i, ex ->
            RoutineExerciseEntity(routineId = id, exerciseId = ex, position = i, sets = 3, reps = 12, restSeconds = 90, loadKg = 40.0)
        },
    )
    return id
}
