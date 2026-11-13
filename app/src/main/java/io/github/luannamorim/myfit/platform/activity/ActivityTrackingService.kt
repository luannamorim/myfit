package io.github.luannamorim.myfit.platform.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import io.github.luannamorim.myfit.MainActivity
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.core.format.DecimalInput
import io.github.luannamorim.myfit.core.time.TimeSource
import io.github.luannamorim.myfit.domain.activity.GpsFix
import io.github.luannamorim.myfit.domain.activity.LiveActivity
import io.github.luannamorim.myfit.platform.session.SessionNotifications
import io.github.luannamorim.myfit.ui.common.labelRes
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Keeps a live activity going with the screen locked: ongoing notification with a chronometer and
 * pause/resume, and GPS updates (outdoor sports) turned into distance. Time never depends on this
 * service: it comes from the persisted marks.
 */
@AndroidEntryPoint
class ActivityTrackingService : LifecycleService() {

    @Inject lateinit var controller: LiveActivityController
    @Inject lateinit var notifications: SessionNotifications
    @Inject lateinit var time: TimeSource

    private var observing = false
    private var listening = false
    private val locationManager by lazy { getSystemService(LocationManager::class.java) }

    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val fix = GpsFix(location.latitude, location.longitude, location.time)
            lifecycleScope.launch { controller.onFix(fix, if (location.hasAccuracy()) location.accuracy else Float.MAX_VALUE) }
        }

        override fun onProviderDisabled(provider: String) = controller.onGpsStatus(GpsStatus.DISABLED)

        override fun onProviderEnabled(provider: String) = controller.onGpsStatus(GpsStatus.SEARCHING)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        notifications.ensureChannels()
        if (!enterForeground()) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!observing) {
            observing = true
            lifecycleScope.launch {
                controller.live.collect { live ->
                    if (live == null) {
                        stopLocation()
                        ServiceCompat.stopForeground(this@ActivityTrackingService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    } else {
                        if (live.gps && !live.isPaused) startLocation() else stopLocation()
                        post(live)
                    }
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopLocation()
        super.onDestroy()
    }

    private fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun enterForeground(): Boolean = try {
        var type = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) type = ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && hasLocationPermission()) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        ServiceCompat.startForeground(this, NOTIFICATION_ID, placeholder(), type)
        true
    } catch (e: RuntimeException) {
        Log.w(TAG, "startForeground failed", e)
        false
    }

    @SuppressLint("MissingPermission") // checked in hasLocationPermission()
    private fun startLocation() {
        if (listening) return
        if (!hasLocationPermission() || locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) != true) {
            controller.onGpsStatus(if (hasLocationPermission()) GpsStatus.DISABLED else GpsStatus.OFF)
            return
        }
        locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, INTERVAL_MS, 0f, listener, Looper.getMainLooper())
        listening = true
    }

    private fun stopLocation() {
        if (!listening) return
        locationManager?.removeUpdates(listener)
        listening = false
    }

    private fun post(live: LiveActivity) {
        val manager = NotificationManagerCompat.from(this)
        if (!manager.areNotificationsEnabled()) return
        @SuppressLint("MissingPermission")
        manager.notify(NOTIFICATION_ID, build(live))
    }

    private fun build(live: LiveActivity): Notification {
        val now = time.now()
        val title = live.name ?: getString(live.sport.labelRes)
        val distance = if (live.gps) "${DecimalInput.format(live.distanceMeters / 1000.0)} km" else null
        val builder = NotificationCompat.Builder(this, SessionNotifications.CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_stat_myfit)
            .setContentTitle(title)
            .setContentIntent(openIntent(this))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(
                0,
                getString(if (live.isPaused) R.string.live_resume else R.string.live_pause),
                PendingIntent.getBroadcast(
                    this, REQ_TOGGLE,
                    Intent(this, LiveActivityActionReceiver::class.java).setAction(LiveActivityActionReceiver.ACTION_TOGGLE_PAUSE),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
        if (live.isPaused) {
            builder.setContentText(listOfNotNull(getString(R.string.live_paused), distance).joinToString(" · ")).setShowWhen(false)
        } else {
            builder.setContentText(distance ?: getString(R.string.live_running))
                .setUsesChronometer(true)
                .setShowWhen(true)
                .setWhen(now.wallMs - live.elapsedMs(now))
        }
        return builder.build()
    }

    private fun placeholder(): Notification = NotificationCompat.Builder(this, SessionNotifications.CHANNEL_ONGOING)
        .setSmallIcon(R.drawable.ic_stat_myfit)
        .setContentTitle(getString(R.string.live_running))
        .setContentIntent(openIntent(this))
        .setOngoing(true)
        .setSilent(true)
        .build()

    companion object {
        const val NOTIFICATION_ID = 1003
        private const val REQ_OPEN = 30
        private const val REQ_TOGGLE = 31
        private const val INTERVAL_MS = 2_000L
        private const val TAG = "ActivityTracking"

        fun openIntent(context: Context): PendingIntent = PendingIntent.getActivity(
            context,
            REQ_OPEN,
            Intent(context, MainActivity::class.java)
                .setAction(MainActivity.ACTION_OPEN_ACTIVITY)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
