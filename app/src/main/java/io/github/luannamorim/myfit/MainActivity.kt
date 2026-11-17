package io.github.luannamorim.myfit

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import io.github.luannamorim.myfit.domain.model.ThemeMode
import io.github.luannamorim.myfit.ui.navigation.AppViewModel
import io.github.luannamorim.myfit.ui.navigation.MyFitNavHost
import io.github.luannamorim.myfit.platform.nutrition.NutritionNotifications
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var nutritionNotifications: NutritionNotifications

    /** Incremented whenever an intent asks to show the running workout / the backup screen. */
    private val openWorkoutRequest = mutableIntStateOf(0)
    private val openActivityRequest = mutableIntStateOf(0)
    private val openBackupRequest = mutableIntStateOf(0)
    private val openNutritionRequest = mutableIntStateOf(0)
    private val addMealRequest = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handle(intent)
        setContent {
            val app: AppViewModel = hiltViewModel()
            val settings by app.settings.collectAsStateWithLifecycle()
            val mode = settings?.themeMode ?: ThemeMode.DARK
            val dark = when (mode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            DisposableEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }
            MyFitTheme(mode) {
                MyFitNavHost(
                    openWorkoutRequest = openWorkoutRequest.intValue,
                    openActivityRequest = openActivityRequest.intValue,
                    openBackupRequest = openBackupRequest.intValue,
                    openNutritionRequest = openNutritionRequest.intValue,
                    addMealRequest = addMealRequest.intValue,
                    appViewModel = app,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        when (intent?.action) {
            ACTION_OPEN_WORKOUT -> openWorkoutRequest.intValue++
            ACTION_OPEN_ACTIVITY -> openActivityRequest.intValue++
            ACTION_OPEN_BACKUP -> openBackupRequest.intValue++
            ACTION_OPEN_NUTRITION -> openNutritionRequest.intValue++
            ACTION_ADD_MEAL -> {
                addMealRequest.intValue++
                // Action buttons don't auto-cancel their notification.
                intent.getLongExtra(NutritionNotifications.EXTRA_MEAL_NOTIFICATION, -1L).takeIf { it > 0 }?.let(nutritionNotifications::cancelMeal)
            }
        }
    }

    companion object {
        const val ACTION_OPEN_WORKOUT = "io.github.luannamorim.myfit.action.OPEN_WORKOUT"
        const val ACTION_OPEN_ACTIVITY = "io.github.luannamorim.myfit.action.OPEN_ACTIVITY"
        const val ACTION_OPEN_BACKUP = "io.github.luannamorim.myfit.action.OPEN_BACKUP"
        /** Nutrition reminder tapped: the Nutrition tab. */
        const val ACTION_OPEN_NUTRITION = "io.github.luannamorim.myfit.action.OPEN_NUTRITION"
        /** "Log meal" on a meal reminder: the meal editor for today. */
        const val ACTION_ADD_MEAL = "io.github.luannamorim.myfit.action.ADD_MEAL"
    }
}
