package io.github.luannamorim.myfit.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.luannamorim.myfit.ui.feature.evolution.EvolutionScreen
import io.github.luannamorim.myfit.ui.feature.history.HistoryScreen
import io.github.luannamorim.myfit.ui.feature.home.HomeScreen
import io.github.luannamorim.myfit.ui.feature.routines.RoutinesScreen
import io.github.luannamorim.myfit.ui.feature.settings.SettingsScreen
import io.github.luannamorim.myfit.ui.theme.MyFitTheme

@Composable
fun MyFitNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
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
        NavHost(navController = navController, startDestination = HomeRoute) {
            composable<HomeRoute> { HomeScreen() }
            composable<RoutinesRoute> { RoutinesScreen() }
            composable<HistoryRoute> { HistoryScreen() }
            composable<EvolutionRoute> { EvolutionScreen() }
            composable<SettingsRoute> { SettingsScreen() }
        }
    }
}

private fun NavDestination?.isOn(top: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(top.routeClass) } == true

fun NavHostController.navigateToTopLevel(top: TopLevelDestination) {
    navigate(top.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
