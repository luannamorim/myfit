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
import io.github.luannamorim.myfit.ui.theme.MyFitTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Incremented whenever an intent asks to show the running workout / the backup screen. */
    private val openWorkoutRequest = mutableIntStateOf(0)
    private val openBackupRequest = mutableIntStateOf(0)

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
                    openBackupRequest = openBackupRequest.intValue,
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
            ACTION_OPEN_BACKUP -> openBackupRequest.intValue++
        }
    }

    companion object {
        const val ACTION_OPEN_WORKOUT = "io.github.luannamorim.myfit.action.OPEN_WORKOUT"
        const val ACTION_OPEN_BACKUP = "io.github.luannamorim.myfit.action.OPEN_BACKUP"
    }
}
