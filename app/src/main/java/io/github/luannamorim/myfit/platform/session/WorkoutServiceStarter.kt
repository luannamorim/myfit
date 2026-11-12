package io.github.luannamorim.myfit.platform.session

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface WorkoutServiceStarter {
    fun start()
    fun stop()
}

@Singleton
class AndroidWorkoutServiceStarter @Inject constructor(
    @ApplicationContext private val context: Context,
) : WorkoutServiceStarter {
    override fun start() {
        try {
            ContextCompat.startForegroundService(context, Intent(context, WorkoutService::class.java))
        } catch (e: IllegalStateException) {
            // Background start not allowed (e.g. ForegroundServiceStartNotAllowedException): the
            // session is safe in the database and the app offers "Retomar" when opened.
            Log.w(TAG, "could not start workout service", e)
        }
    }

    override fun stop() {
        context.stopService(Intent(context, WorkoutService::class.java))
    }

    private companion object {
        const val TAG = "WorkoutService"
    }
}
