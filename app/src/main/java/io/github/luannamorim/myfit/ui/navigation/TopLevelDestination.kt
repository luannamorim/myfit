package io.github.luannamorim.myfit.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.luannamorim.myfit.R
import kotlin.reflect.KClass

enum class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    HOME(HomeRoute, HomeRoute::class, R.string.nav_home, Icons.Outlined.Home, Icons.Rounded.Home),
    ROUTINES(RoutinesRoute, RoutinesRoute::class, R.string.nav_routines, Icons.Outlined.FitnessCenter, Icons.Rounded.FitnessCenter),
    HISTORY(HistoryRoute, HistoryRoute::class, R.string.nav_history, Icons.Outlined.CalendarMonth, Icons.Rounded.CalendarMonth),
    EVOLUTION(EvolutionRoute, EvolutionRoute::class, R.string.nav_evolution, Icons.AutoMirrored.Outlined.ShowChart, Icons.AutoMirrored.Rounded.ShowChart),
    SETTINGS(SettingsRoute, SettingsRoute::class, R.string.nav_settings, Icons.Outlined.Settings, Icons.Rounded.Settings),
}
