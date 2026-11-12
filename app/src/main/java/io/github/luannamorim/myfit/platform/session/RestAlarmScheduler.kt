package io.github.luannamorim.myfit.platform.session

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface RestAlarmScheduler {
    fun schedule(triggerAtWallMs: Long)
    fun cancel()
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
) : RestAlarmScheduler {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun schedule(triggerAtWallMs: Long) {
        val operation = alarmIntent()
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (canExact) {
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAtWallMs, notifications.openWorkoutIntent()), operation)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtWallMs, operation)
        }
    }

    override fun cancel() = alarmManager.cancel(alarmIntent())

    private fun alarmIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, RestAlarmReceiver::class.java).setAction(RestAlarmReceiver.ACTION_REST_END),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private companion object {
        const val REQUEST_CODE = 20
    }
}
