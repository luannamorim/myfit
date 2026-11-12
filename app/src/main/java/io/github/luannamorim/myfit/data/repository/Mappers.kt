package io.github.luannamorim.myfit.data.repository

import io.github.luannamorim.myfit.data.db.entity.ExerciseEntity
import io.github.luannamorim.myfit.data.db.entity.RoutineItemWithExercise
import io.github.luannamorim.myfit.data.db.entity.RoutineOverviewRow
import io.github.luannamorim.myfit.data.db.entity.RoutineWithItems
import io.github.luannamorim.myfit.domain.model.Exercise
import io.github.luannamorim.myfit.domain.model.Routine
import io.github.luannamorim.myfit.domain.model.RoutineItem
import io.github.luannamorim.myfit.domain.model.RoutineOverview

fun ExerciseEntity.toDomain() = Exercise(
    id = id,
    name = name,
    muscleGroup = muscleGroup,
    movementType = movementType,
    imagePath = imagePath,
    notes = notes,
    isCustom = isCustom,
)

fun RoutineItemWithExercise.toDomain() = RoutineItem(
    id = item.id,
    exercise = exercise.toDomain(),
    sets = item.sets,
    reps = item.reps,
    restSeconds = item.restSeconds,
    loadKg = item.loadKg,
    notes = item.notes,
)

fun RoutineWithItems.toDomain() = Routine(
    id = routine.id,
    name = routine.name,
    description = routine.description,
    items = items.sortedBy { it.item.position }.map { it.toDomain() },
)

fun RoutineOverviewRow.toDomain() = RoutineOverview(
    id = id,
    name = name,
    description = description,
    exerciseCount = exerciseCount,
    lastDoneEpochDay = lastDoneEpochDay,
)
