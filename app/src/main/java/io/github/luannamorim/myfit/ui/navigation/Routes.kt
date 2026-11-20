package io.github.luannamorim.myfit.ui.navigation

import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data object RoutinesRoute
@Serializable data object HistoryRoute
@Serializable data object EvolutionRoute
@Serializable data object SettingsRoute
@Serializable data object NutritionRoute
@Serializable data object NutritionRemindersRoute
@Serializable data class MealEditorRoute(val mealId: Long = 0, val dateEpochDay: Long)

@Serializable data class RoutineEditorRoute(val routineId: Long? = null)
@Serializable data object ExercisePickerRoute
@Serializable data object ExerciseLibraryRoute
@Serializable data class ExerciseEditorRoute(val exerciseId: Long? = null)
@Serializable data object WorkoutRoute
@Serializable data class SessionSummaryRoute(val sessionId: Long)
@Serializable data class SessionDetailRoute(val sessionId: Long)
@Serializable data class AssessmentEditorRoute(val assessmentId: Long? = null)
@Serializable data object AssessmentCompareRoute
@Serializable data class PhotoCompareRoute(val firstId: Long, val secondId: Long)
@Serializable data object BackupRoute
@Serializable data object CreditsRoute
@Serializable data object RemindersRoute
@Serializable data class ReminderEditorRoute(val reminderId: Long? = null)

/**
 * [activityId] 0 creates a new one; [dateEpochDay] pre-fills its date (History's selected day);
 * [review] is the "activity done" step right after a timed activity (intensity, notes, photo).
 */
/** [sport] (a [io.github.luannamorim.myfit.domain.activity.Sport] name) and [name] preselect a new activity (Home's next-workout card). */
@Serializable data class ActivityEditorRoute(
    val activityId: Long = 0,
    val dateEpochDay: Long? = null,
    val review: Boolean = false,
    val sport: String? = null,
    val name: String? = null,
)
@Serializable data object LiveActivityRoute
