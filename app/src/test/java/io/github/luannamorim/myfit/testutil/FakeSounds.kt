package io.github.luannamorim.myfit.testutil

import io.github.luannamorim.myfit.domain.model.RestSound
import io.github.luannamorim.myfit.platform.session.RestCue
import io.github.luannamorim.myfit.platform.session.RestSoundPlayer

/** Records the rest sounds instead of playing them. */
class FakeSounds : RestSoundPlayer {
    val played = mutableListOf<Pair<RestCue, RestSound>>()
    override suspend fun play(cue: RestCue, mode: RestSound) {
        played += cue to mode
    }
}
