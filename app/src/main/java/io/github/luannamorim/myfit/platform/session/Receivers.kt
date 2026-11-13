package io.github.luannamorim.myfit.platform.session

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import io.github.luannamorim.myfit.core.time.TimeSource
import io.github.luannamorim.myfit.data.prefs.SettingsRepository
import io.github.luannamorim.myfit.data.repository.SessionRepository
import io.github.luannamorim.myfit.di.ApplicationScope
import io.github.luannamorim.myfit.domain.session.SessionEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Logic run when the rest alarm fires (kept out of the receiver for testability). */
@Singleton
class RestEndHandler @Inject constructor(
    private val repository: SessionRepository,
    private val settings: SettingsRepository,
    private val notifications: SessionNotifications,
    private val time: TimeSource,
    private val sounds: RestSoundPlayer,
) {
    suspend fun onRestAlarm() {
        val session = repository.getActive() ?: return
        val now = time.now()
        val remaining = SessionEngine.restRemainingMs(session, now)
        // Ignore stale alarms (rest skipped/extended or paused after scheduling).
        if (session.isPaused || remaining == null || remaining > STALE_TOLERANCE_MS) return
        notifications.postRestEnd(session)
        notifications.postOngoing(session, now)
        // The app's own sound, even when notifications are off.
        sounds.play(RestCue.GO, settings.current().restSound)
    }

    /** Fired [RestAlarmScheduler.COUNTDOWN_MS] before the end: a beep at 3, 2 and 1 seconds left. */
    suspend fun onCountdownAlarm() {
        val prefs = settings.current()
        if (!prefs.countdownBeeps) return
        val first = repository.getActive() ?: return
        val left = SessionEngine.restRemainingMs(first, time.now()) ?: return
        if (first.isPaused || left !in COUNTDOWN_WINDOW_MS) return
        for (second in 3 downTo 1) {
            // Re-read each time: a skipped, paused or extended rest stops the countdown.
            val session = repository.getActive() ?: return
            if (session.isPaused || session.rest != first.rest) return
            val wait = (SessionEngine.restRemainingMs(session, time.now()) ?: return) - second * 1_000L
            if (wait > 0) delay(wait)
            if (wait > -LATE_BEEP_MS) sounds.play(RestCue.TICK, prefs.restSound)
        }
    }

    private companion object {
        const val STALE_TOLERANCE_MS = 1_500L
        val COUNTDOWN_WINDOW_MS = 1_500L..4_500L
        const val LATE_BEEP_MS = 400L
    }
}

@AndroidEntryPoint
class RestAlarmReceiver : BroadcastReceiver() {
    @Inject lateinit var handler: RestEndHandler
    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != ACTION_REST_END && action != ACTION_REST_COUNTDOWN) return
        // goAsync keeps the alarm's wake lock until finish(): the 3-second countdown fits well within it.
        val pending = goAsync()
        scope.launch {
            try {
                if (action == ACTION_REST_END) handler.onRestAlarm() else handler.onCountdownAlarm()
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_REST_END = "io.github.luannamorim.myfit.action.REST_END"
        const val ACTION_REST_COUNTDOWN = "io.github.luannamorim.myfit.action.REST_COUNTDOWN"
    }
}

/** Notification actions: complete set, skip rest, re-post the ongoing notification after a swipe. */
@AndroidEntryPoint
class SessionActionReceiver : BroadcastReceiver() {
    @Inject lateinit var controller: SessionController
    @Inject lateinit var notifications: SessionNotifications
    @Inject lateinit var time: TimeSource
    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pending = goAsync()
        scope.launch {
            try {
                when (action) {
                    ACTION_COMPLETE_SET -> controller.completeSetAsPlanned()
                    ACTION_SKIP_REST -> controller.skipRest()
                    ACTION_REPOST -> controller.current()?.let { notifications.postOngoing(it, time.now()) }
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_COMPLETE_SET = "io.github.luannamorim.myfit.action.COMPLETE_SET"
        const val ACTION_SKIP_REST = "io.github.luannamorim.myfit.action.SKIP_REST"
        const val ACTION_REPOST = "io.github.luannamorim.myfit.action.REPOST"
    }
}
