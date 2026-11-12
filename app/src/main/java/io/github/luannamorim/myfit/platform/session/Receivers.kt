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
) {
    suspend fun onRestAlarm() {
        val session = repository.getActive() ?: return
        val now = time.now()
        val remaining = SessionEngine.restRemainingMs(session, now)
        // Ignore stale alarms (rest skipped/extended or paused after scheduling).
        if (session.isPaused || remaining == null || remaining > STALE_TOLERANCE_MS) return
        notifications.postRestEnd(session, settings.current().restSound)
        notifications.postOngoing(session, now)
    }

    private companion object {
        const val STALE_TOLERANCE_MS = 1_500L
    }
}

@AndroidEntryPoint
class RestAlarmReceiver : BroadcastReceiver() {
    @Inject lateinit var handler: RestEndHandler
    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REST_END) return
        val pending = goAsync()
        scope.launch {
            try {
                handler.onRestAlarm()
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_REST_END = "io.github.luannamorim.myfit.action.REST_END"
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
