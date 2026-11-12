package io.github.luannamorim.myfit.platform.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import io.github.luannamorim.myfit.core.time.TimeSource
import io.github.luannamorim.myfit.data.prefs.ReminderRepository
import io.github.luannamorim.myfit.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Logic run when a reminder alarm fires or the clock changes (kept out of the receivers for testability). */
@Singleton
class ReminderAlarmHandler @Inject constructor(
    private val repository: ReminderRepository,
    private val notifications: ReminderNotifications,
    private val scheduler: ReminderScheduler,
    private val time: TimeSource,
) {
    /**
     * Notifies, then arms the next week's occurrence (weekly) or turns the reminder off (one-off).
     * Alarms of reminders deleted or turned off meanwhile are ignored.
     */
    suspend fun onAlarm(id: Long, triggerAtMs: Long) {
        val reminder = repository.get(id)?.takeIf { it.enabled } ?: return
        val nowMs = time.now().wallMs
        notifications.post(reminder, whenMs = if (triggerAtMs > 0) triggerAtMs else nowMs)
        if (reminder.isRepeating) {
            // Never before the planned moment, so an alarm delivered a bit early does not fire twice.
            scheduler.scheduleNext(reminder, Instant.ofEpochMilli(maxOf(nowMs, triggerAtMs)).atZone(time.zone()))
        } else {
            repository.setEnabled(id, false)
        }
    }

    /** After a reboot, an app update or a clock/time zone change: alarms are lost or now point at the wrong time. */
    suspend fun rescheduleAll() = scheduler.rescheduleAll(repository.all())
}

/** Fires at a reminder's time (alarm set by [ReminderScheduler]). */
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var handler: ReminderAlarmHandler
    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REMINDER) return
        val id = intent.getLongExtra(EXTRA_ID, -1L)
        if (id <= 0L) return
        val triggerAt = intent.getLongExtra(EXTRA_TRIGGER_AT, 0L)
        val pending = goAsync()
        scope.launch {
            try {
                handler.onAlarm(id, triggerAt)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_REMINDER = "io.github.luannamorim.myfit.action.REMINDER"
        const val EXTRA_ID = "reminder_id"
        const val EXTRA_TRIGGER_AT = "trigger_at"
    }
}

/** Re-arms every reminder after a reboot, an app update or a clock/time zone change. */
@AndroidEntryPoint
class ReminderRescheduleReceiver : BroadcastReceiver() {
    @Inject lateinit var handler: ReminderAlarmHandler
    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in ACTIONS) return
        val pending = goAsync()
        scope.launch {
            try {
                handler.rescheduleAll()
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        val ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        )
    }
}
