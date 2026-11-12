package io.github.luannamorim.myfit.ui.common

import androidx.annotation.StringRes
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.domain.model.MovementType
import io.github.luannamorim.myfit.domain.model.MuscleGroup

@get:StringRes
val MuscleGroup.labelRes: Int
    get() = when (this) {
        MuscleGroup.CHEST -> R.string.muscle_chest
        MuscleGroup.BACK -> R.string.muscle_back
        MuscleGroup.SHOULDERS -> R.string.muscle_shoulders
        MuscleGroup.BICEPS -> R.string.muscle_biceps
        MuscleGroup.TRICEPS -> R.string.muscle_triceps
        MuscleGroup.FOREARMS -> R.string.muscle_forearms
        MuscleGroup.ABS -> R.string.muscle_abs
        MuscleGroup.QUADRICEPS -> R.string.muscle_quadriceps
        MuscleGroup.HAMSTRINGS -> R.string.muscle_hamstrings
        MuscleGroup.GLUTES -> R.string.muscle_glutes
        MuscleGroup.CALVES -> R.string.muscle_calves
        MuscleGroup.FULL_BODY -> R.string.muscle_full_body
        MuscleGroup.CARDIO -> R.string.muscle_cardio
        MuscleGroup.OTHER -> R.string.muscle_other
    }

@get:StringRes
val MovementType.labelRes: Int
    get() = when (this) {
        MovementType.PUSH -> R.string.movement_push
        MovementType.PULL -> R.string.movement_pull
        MovementType.LEGS -> R.string.movement_legs
        MovementType.OTHER -> R.string.movement_other
    }
