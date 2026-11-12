package io.github.luannamorim.myfit.platform.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.luannamorim.myfit.core.time.TimeSource
import io.github.luannamorim.myfit.domain.model.Reminder
import io.github.luannamorim.myfit.domain.reminder.ReminderSchedule
import java.time.Instant
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One AlarmManager alarm per enabled reminder, for its next occurrence only (the receiver arms the
 * following one). Exact and allowed in Doze when the app may use exact alarms, inexact otherwise.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val time: TimeSource,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    /** Ids armed by this process, so reminders deleted since then get their alarms cancelled. */
    private val armed = mutableSetOf<Long>()

    /** Arms the next alarm of every enabled reminder and cancels the alarms of disabled, past or removed ones. */
    @Synchronized
    fun rescheduleAll(reminders: List<Reminder>) {
        val now = now()
        val keep = reminders.filter { arm(it, now) }.map { it.id }.toSet()
        (armed - keep).forEach(::cancel)
        armed.clear()
        armed += keep
    }

    /** Arms [reminder]'s next occurrence strictly after [after], or cancels its alarm when there is none. */
    @Synchronized
    fun scheduleNext(reminder: Reminder, after: ZonedDateTime = now()) {
        if (arm(reminder, after)) armed += reminder.id else armed -= reminder.id
    }

    private fun arm(reminder: Reminder, after: ZonedDateTime): Boolean {
        val next = if (reminder.enabled) ReminderSchedule.nextTrigger(reminder, after) else null
        if (next == null) {
            cancel(reminder.id)
            return false
        }
        val atMs = next.toInstant().toEpochMilli()
        val operation = PendingIntent.getBroadcast(
            context,
            reminder.id.hashCode(),
            alarmIntent(reminder.id).putExtra(ReminderReceiver.EXTRA_TRIGGER_AT, atMs),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        try {
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, operation)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, operation)
            }
        } catch (_: SecurityException) {
            // Exact-alarm access revoked between the check and the call.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, operation)
        }
        return true
    }

    private fun cancel(id: Long) {
        val existing = PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            alarmIntent(id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
        ) ?: return
        alarmManager.cancel(existing)
        existing.cancel()
    }

    /** Unique per reminder (the data URI), so each one gets its own PendingIntent. */
    private fun alarmIntent(id: Long): Intent = Intent(context, ReminderReceiver::class.java)
        .setAction(ReminderReceiver.ACTION_REMINDER)
        .setData("myfit://reminder/$id".toUri())
        .putExtra(ReminderReceiver.EXTRA_ID, id)

    private fun now(): ZonedDateTime = Instant.ofEpochMilli(time.now().wallMs).atZone(time.zone())
}
