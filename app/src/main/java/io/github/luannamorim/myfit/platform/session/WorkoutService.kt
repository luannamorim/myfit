package io.github.luannamorim.myfit.platform.session

import android.app.Notification
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.core.time.TimeSource
import io.github.luannamorim.myfit.data.repository.SessionRepository
import io.github.luannamorim.myfit.domain.session.SessionEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Keeps the workout visible while it runs: ongoing notification with a system chronometer and quick
 * actions. Timing never depends on this service being alive — it is derived from persisted marks.
 */
@AndroidEntryPoint
class WorkoutService : LifecycleService() {

    @Inject lateinit var repository: SessionRepository
    @Inject lateinit var notifications: SessionNotifications
    @Inject lateinit var time: TimeSource

    private var observer: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        notifications.ensureChannels()
        if (!enterForeground(placeholder())) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (observer == null) observer = lifecycleScope.launch { observe() }
        return START_STICKY
    }

    private suspend fun observe() {
        repository.observeActive().collectLatest { session ->
            if (session == null) {
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
                return@collectLatest
            }
            notifications.postOngoing(session, time.now())
            // Re-render when the rest ends so the text switches to the next set.
            val remaining = SessionEngine.restRemainingMs(session, time.now())
            if (!session.isPaused && remaining != null && remaining > 0) {
                delay(remaining + 250)
                notifications.postOngoing(session, time.now())
            }
        }
    }

    private fun enterForeground(notification: Notification): Boolean = try {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH else 0
        ServiceCompat.startForeground(this, SessionNotifications.ID_ONGOING, notification, type)
        true
    } catch (e: RuntimeException) {
        // e.g. ForegroundServiceStartNotAllowedException after a sticky restart from the background.
        Log.w(TAG, "startForeground failed", e)
        false
    }

    private fun placeholder(): Notification = NotificationCompat.Builder(this, SessionNotifications.CHANNEL_ONGOING)
        .setSmallIcon(R.drawable.ic_stat_myfit)
        .setContentTitle(getString(R.string.notif_workout_title))
        .setContentIntent(notifications.openWorkoutIntent())
        .setOngoing(true)
        .setSilent(true)
        .build()

    private companion object {
        const val TAG = "WorkoutService"
    }
}
