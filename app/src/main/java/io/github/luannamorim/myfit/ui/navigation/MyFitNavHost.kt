package io.github.luannamorim.myfit.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.luannamorim.myfit.ui.feature.activity.ActivityEditorScreen
import io.github.luannamorim.myfit.ui.feature.evolution.AssessmentCompareScreen
import io.github.luannamorim.myfit.ui.feature.evolution.AssessmentEditorScreen
import io.github.luannamorim.myfit.ui.feature.evolution.EvolutionActions
import io.github.luannamorim.myfit.ui.feature.evolution.EvolutionScreen
import io.github.luannamorim.myfit.ui.feature.evolution.PhotoCompareScreen
import io.github.luannamorim.myfit.ui.feature.history.HistoryScreen
import io.github.luannamorim.myfit.ui.feature.history.SessionDetailScreen
import io.github.luannamorim.myfit.ui.feature.home.HomeActions
import io.github.luannamorim.myfit.ui.feature.home.HomeScreen
import io.github.luannamorim.myfit.ui.feature.library.ExerciseEditorScreen
import io.github.luannamorim.myfit.ui.feature.library.ExerciseLibraryScreen
import io.github.luannamorim.myfit.ui.feature.library.ExercisePickerScreen
import io.github.luannamorim.myfit.ui.feature.routines.RoutineEditorScreen
import io.github.luannamorim.myfit.ui.feature.routines.RoutineEditorViewModel
import io.github.luannamorim.myfit.ui.feature.routines.RoutinesScreen
import io.github.luannamorim.myfit.ui.feature.settings.BackupScreen
import io.github.luannamorim.myfit.ui.feature.settings.CreditsScreen
import io.github.luannamorim.myfit.ui.feature.settings.SettingsScreen
import io.github.luannamorim.myfit.ui.feature.workout.SessionSummaryScreen
import io.github.luannamorim.myfit.ui.feature.workout.WorkoutScreen
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import io.github.luannamorim.myfit.ui.theme.Spacing

@Composable
fun MyFitNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    openWorkoutRequest: Int = 0,
    openBackupRequest: Int = 0,
    appViewModel: AppViewModel = hiltViewModel(),
) {
    LaunchedEffect(appViewModel) {
        appViewModel.events.collect { event ->
            when (event) {
                AppEvent.OpenWorkout -> navController.navigate(WorkoutRoute) { launchSingleTop = true }
            }
        }
    }
    // Re-arm rest alarm and foreground service whenever the app comes to the foreground.
    LifecycleStartEffect(appViewModel) {
        appViewModel.resync()
        onStopOrDispose { }
    }
    val activeSession by appViewModel.activeSession.collectAsStateWithLifecycle()
    LaunchedEffect(openBackupRequest) {
        if (openBackupRequest > 0) navController.navigate(BackupRoute) { launchSingleTop = true }
    }
    // Notification tap / launcher intent asking to show the workout.
    LaunchedEffect(openWorkoutRequest) {
        if (openWorkoutRequest > 0) {
            navController.navigate(WorkoutRoute) { launchSingleTop = true }
        }
    }

    val entry by navController.currentBackStackEntryAsState()
    val destination = entry?.destination
    val currentTop = TopLevelDestination.entries.firstOrNull { destination.isOn(it) }
    val c = MyFitTheme.colors

    val itemColors = NavigationSuiteDefaults.itemColors(
        navigationBarItemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = c.textPrimary,
            selectedTextColor = c.textPrimary,
            indicatorColor = c.surfaceRaised,
            unselectedIconColor = c.textTertiary,
            unselectedTextColor = c.textTertiary,
        ),
        navigationRailItemColors = NavigationRailItemDefaults.colors(
            selectedIconColor = c.textPrimary,
            selectedTextColor = c.textPrimary,
            indicatorColor = c.surfaceRaised,
            unselectedIconColor = c.textTertiary,
            unselectedTextColor = c.textTertiary,
        ),
    )
    val layoutType = if (currentTop == null && destination != null) {
        NavigationSuiteType.None
    } else {
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfoV2())
    }

    NavigationSuiteScaffold(
        modifier = modifier,
        layoutType = layoutType,
        containerColor = c.background,
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationBarContainerColor = c.background,
            navigationRailContainerColor = c.background,
            navigationBarContentColor = c.textSecondary,
            navigationRailContentColor = c.textSecondary,
        ),
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { top ->
                val selected = top == currentTop
                item(
                    selected = selected,
                    onClick = { navController.navigateToTopLevel(top) },
                    icon = { Icon(if (selected) top.selectedIcon else top.icon, contentDescription = null) },
                    label = { Text(stringResource(top.labelRes), style = MyFitTheme.type.caption) },
                    colors = itemColors,
                )
            }
        },
    ) {
        Column(Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = HomeRoute,
                modifier = Modifier.weight(1f),
                enterTransition = { fadeIn(tween(TRANSITION_MS)) },
                exitTransition = { fadeOut(tween(TRANSITION_MS)) },
                popEnterTransition = { fadeIn(tween(TRANSITION_MS)) },
                popExitTransition = { fadeOut(tween(TRANSITION_MS)) },
            ) {
                composable<HomeRoute> {
                    HomeScreen(
                        actions = HomeActions(
                            onStart = appViewModel::startRoutine,
                            onResume = { navController.navigate(WorkoutRoute) { launchSingleTop = true } },
                            onOpenRoutine = { navController.navigate(RoutineEditorRoute(it)) },
                            onCreateRoutine = { navController.navigate(RoutineEditorRoute()) },
                            onSeeRoutines = { navController.navigateToTopLevel(TopLevelDestination.ROUTINES) },
                            onOpenSession = { navController.navigate(SessionDetailRoute(it)) },
                            onOpenHistory = { navController.navigateToTopLevel(TopLevelDestination.HISTORY) },
                            onOpenBody = { navController.navigateToTopLevel(TopLevelDestination.EVOLUTION) },
                            onLogActivity = { navController.navigate(ActivityEditorRoute()) },
                        ),
                    )
                }
                composable<RoutinesRoute> {
                    RoutinesScreen(
                        onCreate = { navController.navigate(RoutineEditorRoute()) },
                        onEdit = { navController.navigate(RoutineEditorRoute(it)) },
                        onStart = appViewModel::startRoutine,
                        onOpenLibrary = { navController.navigate(ExerciseLibraryRoute) },
                    )
                }
                composable<HistoryRoute> {
                    HistoryScreen(
                        onOpenSession = { navController.navigate(SessionDetailRoute(it)) },
                        onOpenActivity = { navController.navigate(ActivityEditorRoute(activityId = it)) },
                        onNewActivity = { navController.navigate(ActivityEditorRoute(dateEpochDay = it.toEpochDay())) },
                    )
                }
                composable<EvolutionRoute> {
                    EvolutionScreen(
                        actions = EvolutionActions(
                            onNewAssessment = { navController.navigate(AssessmentEditorRoute()) },
                            onOpenAssessment = { navController.navigate(AssessmentEditorRoute(it)) },
                            onCompare = { navController.navigate(AssessmentCompareRoute) },
                            onComparePhotos = { a, b -> navController.navigate(PhotoCompareRoute(a, b)) },
                        ),
                    )
                }
                composable<AssessmentEditorRoute> { AssessmentEditorScreen(onClose = { navController.popBackStack() }) }
                composable<ActivityEditorRoute> { ActivityEditorScreen(onClose = { navController.popBackStack() }) }
                composable<AssessmentCompareRoute> { AssessmentCompareScreen(onBack = { navController.popBackStack() }) }
                composable<PhotoCompareRoute> { PhotoCompareScreen(onBack = { navController.popBackStack() }) }
                composable<SettingsRoute> {
                    SettingsScreen(
                        onOpenBackup = { navController.navigate(BackupRoute) },
                        onOpenLibrary = { navController.navigate(ExerciseLibraryRoute) },
                        onOpenCredits = { navController.navigate(CreditsRoute) },
                    )
                }
                composable<BackupRoute> { BackupScreen(onBack = { navController.popBackStack() }) }
                composable<CreditsRoute> { CreditsScreen(onBack = { navController.popBackStack() }) }

                composable<RoutineEditorRoute> { entry ->
                    val picked by entry.savedStateHandle
                        .getStateFlow<LongArray?>(RoutineEditorViewModel.PICKED_IDS_KEY, null)
                        .collectAsStateWithLifecycle()
                    RoutineEditorScreen(
                        pickedExerciseIds = picked,
                        onPickedConsumed = { entry.savedStateHandle[RoutineEditorViewModel.PICKED_IDS_KEY] = null },
                        onAddExercises = { navController.navigate(ExercisePickerRoute) },
                        onClose = { navController.popBackStack() },
                    )
                }
                composable<ExercisePickerRoute> {
                    ExercisePickerScreen(
                        onBack = { navController.popBackStack() },
                        onConfirm = { ids ->
                            navController.previousBackStackEntry?.savedStateHandle?.set(RoutineEditorViewModel.PICKED_IDS_KEY, ids)
                            navController.popBackStack()
                        },
                        onCreate = { navController.navigate(ExerciseEditorRoute()) },
                    )
                }
                composable<ExerciseLibraryRoute> {
                    ExerciseLibraryScreen(
                        onBack = { navController.popBackStack() },
                        onOpen = { navController.navigate(ExerciseEditorRoute(it)) },
                        onCreate = { navController.navigate(ExerciseEditorRoute()) },
                    )
                }
                composable<ExerciseEditorRoute> {
                    ExerciseEditorScreen(onClose = { navController.popBackStack() })
                }
                composable<SessionDetailRoute> {
                    SessionDetailScreen(onBack = { navController.popBackStack() })
                }
                composable<WorkoutRoute> {
                    WorkoutScreen(
                        onMinimize = { navController.popBackStack() },
                        onFinish = { navController.navigate(SessionSummaryRoute(it)) },
                        onSessionGone = { navController.popBackStack(WorkoutRoute, inclusive = true) },
                    )
                }
                composable<SessionSummaryRoute> {
                    SessionSummaryScreen(
                        onBack = { navController.popBackStack() },
                        onDone = {
                            navController.navigate(HomeRoute) {
                                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                            }
                        },
                    )
                }
            }
            val session = activeSession
            // Home shows its own resume card.
            if (session != null && currentTop != null && currentTop != TopLevelDestination.HOME) {
                ActiveSessionBar(
                    session,
                    onResume = { navController.navigate(WorkoutRoute) { launchSingleTop = true } },
                    modifier = Modifier.padding(horizontal = Spacing.m, vertical = Spacing.s),
                )
            }
        }
    }
}

private const val TRANSITION_MS = 200

private fun NavDestination?.isOn(top: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(top.routeClass) } == true

fun NavHostController.navigateToTopLevel(top: TopLevelDestination) {
    navigate(top.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
