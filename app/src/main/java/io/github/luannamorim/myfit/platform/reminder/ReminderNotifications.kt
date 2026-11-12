package io.github.luannamorim.myfit.platform.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.luannamorim.myfit.MainActivity
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.domain.model.Reminder
import javax.inject.Inject
import javax.inject.Singleton

/** "Time to work out" notifications for workout reminders. */
@Singleton
class ReminderNotifications @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager = NotificationManagerCompat.from(context)

    /** Creates (or renames, after a language change) the reminders channel: high importance, default sound. */
    fun ensureChannel() {
        val channel = NotificationChannel(CHANNEL_REMINDERS, context.getString(R.string.channel_reminders), NotificationManager.IMPORTANCE_HIGH).apply {
            description = context.getString(R.string.channel_reminders_desc)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Posts the reminder; false when the user turned notifications off for the app. */
    fun post(reminder: Reminder, whenMs: Long): Boolean {
        if (!manager.areNotificationsEnabled()) return false
        ensureChannel()
        val notification = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_stat_myfit)
            .setContentTitle(context.getString(R.string.reminder_notif_title))
            .setContentText(reminder.label)
            .setContentIntent(openAppIntent())
            .setAutoCancel(true)
            .setWhen(whenMs)
            .setShowWhen(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        @Suppress("MissingPermission")
        manager.notify(TAG, reminder.id.hashCode(), notification)
        return true
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        REQ_OPEN_APP,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        const val CHANNEL_REMINDERS = "reminders"
        /** Notification tag: keeps reminder ids apart from the session notification ids. */
        const val TAG = "reminder"
        private const val REQ_OPEN_APP = 30
    }
}
