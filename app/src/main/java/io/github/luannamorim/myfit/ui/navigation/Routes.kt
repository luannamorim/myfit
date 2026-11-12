package io.github.luannamorim.myfit.ui.navigation

import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data object RoutinesRoute
@Serializable data object HistoryRoute
@Serializable data object EvolutionRoute
@Serializable data object SettingsRoute

@Serializable data class RoutineEditorRoute(val routineId: Long? = null)
@Serializable data object ExercisePickerRoute
@Serializable data object ExerciseLibraryRoute
@Serializable data class ExerciseEditorRoute(val exerciseId: Long? = null)
@Serializable data object WorkoutRoute
@Serializable data class SessionSummaryRoute(val sessionId: Long)
@Serializable data class SessionDetailRoute(val sessionId: Long)
@Serializable data class AssessmentEditorRoute(val assessmentId: Long? = null)
@Serializable data object AssessmentCompareRoute
@Serializable data object PhotosRoute
@Serializable data object BackupRoute
