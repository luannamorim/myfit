package io.github.luannamorim.myfit.ui.common

import androidx.annotation.StringRes
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.domain.body.Measure
import io.github.luannamorim.myfit.domain.body.MeasureUnit
import io.github.luannamorim.myfit.domain.model.MovementType
import io.github.luannamorim.myfit.domain.model.PhotoPose
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

@get:StringRes
val Measure.labelRes: Int
    get() = when (this) {
        Measure.WEIGHT -> R.string.measure_weight
        Measure.BODY_FAT -> R.string.measure_body_fat
        Measure.NECK -> R.string.measure_neck
        Measure.SHOULDERS -> R.string.measure_shoulders
        Measure.CHEST -> R.string.measure_chest
        Measure.WAIST -> R.string.measure_waist
        Measure.ABDOMEN -> R.string.measure_abdomen
        Measure.HIPS -> R.string.measure_hips
        Measure.ARM_RIGHT -> R.string.measure_arm_right
        Measure.ARM_LEFT -> R.string.measure_arm_left
        Measure.FOREARM_RIGHT -> R.string.measure_forearm_right
        Measure.FOREARM_LEFT -> R.string.measure_forearm_left
        Measure.THIGH_RIGHT -> R.string.measure_thigh_right
        Measure.THIGH_LEFT -> R.string.measure_thigh_left
        Measure.CALF_RIGHT -> R.string.measure_calf_right
        Measure.CALF_LEFT -> R.string.measure_calf_left
    }

@get:StringRes
val MeasureUnit.labelRes: Int
    get() = when (this) {
        MeasureUnit.KG -> R.string.unit_kg
        MeasureUnit.PERCENT -> R.string.unit_percent
        MeasureUnit.CM -> R.string.unit_cm
    }

@get:StringRes
val PhotoPose.labelRes: Int
    get() = when (this) {
        PhotoPose.FRONT -> R.string.pose_front
        PhotoPose.SIDE -> R.string.pose_side
        PhotoPose.BACK -> R.string.pose_back
        PhotoPose.OTHER -> R.string.pose_other
    }
