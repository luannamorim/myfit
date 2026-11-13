package io.github.luannamorim.myfit.platform.session

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.luannamorim.myfit.core.time.TimeSource
import javax.inject.Inject
import javax.inject.Singleton

interface RestAlarmScheduler {
    /** Rest end at [triggerAtWallMs]; also arms the countdown alarm [COUNTDOWN_MS] earlier when there's time. */
    fun schedule(triggerAtWallMs: Long)
    fun cancel()

    companion object {
        const val COUNTDOWN_MS = 3_000L
    }
}

/**
 * Uses setAlarmClock: the only exact alarm exempt from Doze/App Standby (and from OEM battery
 * savers such as One UI), so the rest-end alert fires on time with the screen locked.
 * Side effect: the status bar shows an alarm icon while resting.
 */
@Singleton
class AndroidRestAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notifications: SessionNotifications,
    private val time: TimeSource,
) : RestAlarmScheduler {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun schedule(triggerAtWallMs: Long) {
        set(triggerAtWallMs, alarmIntent(RestAlarmReceiver.ACTION_REST_END, REQUEST_CODE))
        val countdownAt = triggerAtWallMs - RestAlarmScheduler.COUNTDOWN_MS
        val countdown = alarmIntent(RestAlarmReceiver.ACTION_REST_COUNTDOWN, REQUEST_CODE_COUNTDOWN)
        // A rest shorter than the countdown (or almost over) gets no beeps.
        if (countdownAt > time.now().wallMs + 500) set(countdownAt, countdown) else alarmManager.cancel(countdown)
    }

    override fun cancel() {
        alarmManager.cancel(alarmIntent(RestAlarmReceiver.ACTION_REST_END, REQUEST_CODE))
        alarmManager.cancel(alarmIntent(RestAlarmReceiver.ACTION_REST_COUNTDOWN, REQUEST_CODE_COUNTDOWN))
    }

    private fun set(atWallMs: Long, operation: PendingIntent) {
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (canExact) {
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(atWallMs, notifications.openWorkoutIntent()), operation)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atWallMs, operation)
        }
    }

    private fun alarmIntent(action: String, requestCode: Int): PendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        Intent(context, RestAlarmReceiver::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private companion object {
        const val REQUEST_CODE = 20
        const val REQUEST_CODE_COUNTDOWN = 21
    }
}
