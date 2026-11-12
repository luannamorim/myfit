package io.github.luannamorim.myfit.platform.session

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.luannamorim.myfit.MainActivity
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.core.time.Mark
import io.github.luannamorim.myfit.domain.model.RestSound
import io.github.luannamorim.myfit.domain.session.ActiveSession
import io.github.luannamorim.myfit.domain.session.SessionEngine
import javax.inject.Inject
import javax.inject.Singleton

/** Builds and posts the workout notifications (ongoing session + rest end). */
@Singleton
class SessionNotifications @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager = NotificationManagerCompat.from(context)

    fun ensureChannels() {
        val system = context.getSystemService(NotificationManager::class.java)
        val vibration = longArrayOf(0, 400, 200, 400)
        val ongoing = NotificationChannel(CHANNEL_ONGOING, context.getString(R.string.channel_session_ongoing), NotificationManager.IMPORTANCE_LOW).apply {
            description = context.getString(R.string.channel_session_ongoing_desc)
            setShowBadge(false)
        }
        val restEnd = NotificationChannel(CHANNEL_REST_END, context.getString(R.string.channel_rest_end), NotificationManager.IMPORTANCE_HIGH).apply {
            description = context.getString(R.string.channel_rest_end_desc)
            enableVibration(true)
            vibrationPattern = vibration
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
            )
        }
        val restEndAlarm = NotificationChannel(CHANNEL_REST_END_ALARM, context.getString(R.string.channel_rest_end_alarm), NotificationManager.IMPORTANCE_HIGH).apply {
            description = context.getString(R.string.channel_rest_end_alarm_desc)
            enableVibration(true)
            vibrationPattern = vibration
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
            )
        }
        system.createNotificationChannels(listOf(ongoing, restEnd, restEndAlarm))
    }

    /** Ongoing notification: counts down during rest, counts the workout time up otherwise. */
    fun ongoing(session: ActiveSession, now: Mark): Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_stat_myfit)
            .setContentTitle(session.routineName)
            .setContentIntent(openWorkoutIntent())
            .setDeleteIntent(actionIntent(SessionActionReceiver.ACTION_REPOST, REQ_REPOST))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        val restRemaining = SessionEngine.restRemainingMs(session, now)
        val nextLabel = nextLabel(session)
        when {
            session.isPaused -> builder
                .setContentText(context.getString(R.string.notif_paused))
                .setUsesChronometer(false)
                .setShowWhen(false)
            restRemaining != null && restRemaining > 0 -> builder
                .setContentText(context.getString(R.string.notif_resting, nextLabel))
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setShowWhen(true)
                .setWhen(now.wallMs + restRemaining)
                .addAction(0, context.getString(R.string.notif_action_skip_rest), actionIntent(SessionActionReceiver.ACTION_SKIP_REST, REQ_SKIP_REST))
            else -> {
                val text = if (restRemaining == 0L) context.getString(R.string.notif_rest_over, nextLabel) else nextLabel
                builder
                    .setContentText(text)
                    .setUsesChronometer(true)
                    .setChronometerCountDown(false)
                    .setShowWhen(true)
                    .setWhen(now.wallMs - SessionEngine.elapsedMs(session, now))
                if (session.currentSet != null && !SessionEngine.isComplete(session)) {
                    builder.addAction(0, context.getString(R.string.notif_action_complete_set), actionIntent(SessionActionReceiver.ACTION_COMPLETE_SET, REQ_COMPLETE))
                }
            }
        }
        return builder.build()
    }

    fun postOngoing(session: ActiveSession, now: Mark) {
        if (manager.areNotificationsEnabled()) {
            @Suppress("MissingPermission")
            manager.notify(ID_ONGOING, ongoing(session, now))
        }
    }

    /** Loud "rest is over" alert (sound + vibration per channel), shown even with the app closed. */
    fun postRestEnd(session: ActiveSession, sound: RestSound) {
        if (!manager.areNotificationsEnabled()) return
        val channel = if (sound == RestSound.ALWAYS) CHANNEL_REST_END_ALARM else CHANNEL_REST_END
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_myfit)
            .setContentTitle(context.getString(R.string.notif_rest_end_title))
            .setContentText(context.getString(R.string.notif_rest_end_text, nextLabel(session)))
            .setContentIntent(openWorkoutIntent())
            .setAutoCancel(true)
            .setTimeoutAfter(REST_END_TIMEOUT_MS)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(0, context.getString(R.string.notif_action_complete_set), actionIntent(SessionActionReceiver.ACTION_COMPLETE_SET, REQ_COMPLETE))
            .build()
        @Suppress("MissingPermission")
        manager.notify(ID_REST_END, notification)
    }

    fun cancelRestEnd() = manager.cancel(ID_REST_END)

    fun cancelAll() {
        manager.cancel(ID_ONGOING)
        manager.cancel(ID_REST_END)
    }

    private fun nextLabel(session: ActiveSession): String {
        val exercise = session.currentExercise
        val set = session.currentSet
        if (exercise == null || set == null || SessionEngine.isComplete(session)) return context.getString(R.string.notif_all_done)
        return context.getString(R.string.notif_current, exercise.name, session.cursor.setIndex + 1, exercise.sets.size)
    }

    fun openWorkoutIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        REQ_OPEN,
        Intent(context, MainActivity::class.java)
            .setAction(MainActivity.ACTION_OPEN_WORKOUT)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun actionIntent(action: String, requestCode: Int): PendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        Intent(context, SessionActionReceiver::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        const val CHANNEL_ONGOING = "session_ongoing"
        const val CHANNEL_REST_END = "rest_end"
        const val CHANNEL_REST_END_ALARM = "rest_end_alarm"
        const val ID_ONGOING = 1001
        const val ID_REST_END = 1002
        private const val REQ_OPEN = 10
        private const val REQ_COMPLETE = 11
        private const val REQ_SKIP_REST = 12
        private const val REQ_REPOST = 13
        private const val REST_END_TIMEOUT_MS = 60_000L
    }
}
