package io.github.luannamorim.myfit.domain.model

enum class MuscleGroup {
    CHEST, BACK, SHOULDERS, BICEPS, TRICEPS, FOREARMS, ABS,
    QUADRICEPS, HAMSTRINGS, GLUTES, CALVES, FULL_BODY, CARDIO, OTHER,
}

enum class MovementType { PUSH, PULL, LEGS, OTHER }

/** What a set of the exercise counts: repetitions, or seconds held/done (plank, cardio). */
enum class ExerciseMeasure { REPS, TIME }

enum class SessionStatus { ACTIVE, FINISHED }

enum class ItemStatus { PENDING, DONE, SKIPPED }

enum class PhotoPose { FRONT, SIDE, BACK, OTHER }

/** How hard a finished workout felt, chosen by the user (optional). */
enum class Intensity { LOW, MEDIUM, HIGH }
