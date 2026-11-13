package io.github.luannamorim.myfit.platform.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.luannamorim.myfit.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

interface ActivityServiceStarter {
    fun start()
    fun stop()
}

@Singleton
class AndroidActivityServiceStarter @Inject constructor(
    @ApplicationContext private val context: Context,
) : ActivityServiceStarter {
    override fun start() {
        try {
            ContextCompat.startForegroundService(context, Intent(context, ActivityTrackingService::class.java))
        } catch (e: IllegalStateException) {
            // Not allowed from the background: the activity keeps its time and resumes when the app opens.
            Log.w("ActivityTracking", "could not start service", e)
        }
    }

    override fun stop() {
        context.stopService(Intent(context, ActivityTrackingService::class.java))
    }
}

/** Pause/resume from the ongoing notification. */
@AndroidEntryPoint
class LiveActivityActionReceiver : BroadcastReceiver() {
    @Inject lateinit var controller: LiveActivityController
    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TOGGLE_PAUSE) return
        val pending = goAsync()
        scope.launch {
            try {
                controller.togglePause()
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE_PAUSE = "io.github.luannamorim.myfit.action.LIVE_TOGGLE_PAUSE"
    }
}
