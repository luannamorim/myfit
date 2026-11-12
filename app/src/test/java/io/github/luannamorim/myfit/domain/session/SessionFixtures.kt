package io.github.luannamorim.myfit.domain.session

import io.github.luannamorim.myfit.core.time.Mark
import io.github.luannamorim.myfit.domain.model.ItemStatus
import io.github.luannamorim.myfit.domain.model.MuscleGroup

/** Builds a session with [setsPerExercise] sets per exercise, 12 reps, 40 kg, [restSeconds] rest. */
fun sessionOf(start: Mark, vararg setsPerExercise: Int, restSeconds: Int = 90): ActiveSession {
    var setId = 1L
    return ActiveSession(
        id = 1,
        routineId = 1,
        routineName = "Costas",
        startedAt = start,
        pausedAt = null,
        totalPausedMs = 0,
        exercises = setsPerExercise.mapIndexed { i, n ->
            SessionExercise(
                id = i + 1L,
                exerciseId = i + 100L,
                name = "Exercício ${i + 1}",
                muscleGroup = MuscleGroup.BACK,
                imagePath = null,
                restSeconds = restSeconds,
                notes = null,
                status = ItemStatus.PENDING,
                sets = List(n) { SessionSet(id = setId++, plannedReps = 12, plannedLoadKg = 40.0) },
            )
        },
        cursor = Cursor(0, 0),
        rest = null,
    )
}
