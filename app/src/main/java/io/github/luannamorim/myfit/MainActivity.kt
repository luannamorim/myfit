package io.github.luannamorim.myfit

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableIntStateOf
import dagger.hilt.android.AndroidEntryPoint
import io.github.luannamorim.myfit.ui.navigation.MyFitNavHost
import io.github.luannamorim.myfit.ui.theme.MyFitTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Incremented whenever an intent asks to show the running workout. */
    private val openWorkoutRequest = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handle(intent)
        setContent {
            MyFitTheme {
                MyFitNavHost(openWorkoutRequest = openWorkoutRequest.intValue)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handle(intent)
    }

    private fun handle(intent: Intent?) {
        if (intent?.action == ACTION_OPEN_WORKOUT) openWorkoutRequest.intValue++
    }

    companion object {
        const val ACTION_OPEN_WORKOUT = "io.github.luannamorim.myfit.action.OPEN_WORKOUT"
        const val ACTION_OPEN_BACKUP = "io.github.luannamorim.myfit.action.OPEN_BACKUP"
    }
}
