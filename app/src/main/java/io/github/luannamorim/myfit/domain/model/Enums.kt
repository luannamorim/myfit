package io.github.luannamorim.myfit.domain.model

enum class MuscleGroup {
    CHEST, BACK, SHOULDERS, BICEPS, TRICEPS, FOREARMS, ABS,
    QUADRICEPS, HAMSTRINGS, GLUTES, CALVES, FULL_BODY, CARDIO, OTHER,
}

enum class MovementType { PUSH, PULL, LEGS, OTHER }

enum class SessionStatus { ACTIVE, FINISHED }

enum class ItemStatus { PENDING, DONE, SKIPPED }

enum class PhotoPose { FRONT, SIDE, BACK, OTHER }
