package io.github.luannamorim.myfit.platform.session

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.domain.model.RestSound
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/** The app's own rest sounds: a countdown beep and the end-of-rest "go" chime. */
enum class RestCue { TICK, GO }

interface RestSoundPlayer {
    /** Plays [cue] on the alarm stream ([RestSound.ALWAYS]) or the notification stream (silent on vibrate). */
    suspend fun play(cue: RestCue, mode: RestSound)
}

@Singleton
class SoundPoolRestSoundPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
) : RestSoundPlayer {
    private class Bank(val pool: SoundPool, val tick: Int, val go: Int, val ready: CompletableDeferred<Unit>)

    private val banks = mutableMapOf<RestSound, Bank>()

    override suspend fun play(cue: RestCue, mode: RestSound) {
        val bank = synchronized(banks) { banks.getOrPut(mode) { load(mode) } }
        withTimeoutOrNull(LOAD_TIMEOUT_MS) { bank.ready.await() } ?: return
        bank.pool.play(if (cue == RestCue.TICK) bank.tick else bank.go, 1f, 1f, 1, 0, 1f)
    }

    private fun load(mode: RestSound): Bank {
        val usage = if (mode == RestSound.ALWAYS) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_NOTIFICATION_EVENT
        val attributes = AudioAttributes.Builder().setUsage(usage).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        val pool = SoundPool.Builder().setMaxStreams(2).setAudioAttributes(attributes).build()
        val pending = AtomicInteger(2)
        val ready = CompletableDeferred<Unit>()
        pool.setOnLoadCompleteListener { _, _, _ -> if (pending.decrementAndGet() == 0) ready.complete(Unit) }
        return Bank(pool, pool.load(context, R.raw.rest_tick, 1), pool.load(context, R.raw.rest_go, 1), ready)
    }

    private companion object {
        const val LOAD_TIMEOUT_MS = 1_500L
    }
}
