package io.github.luannamorim.myfit.domain.model

/** Rules for timed sets (seconds stored where repetitions would be). */
object TimedSets {
    const val MAX_SECONDS = 3 * 60 * 60

    /** A sensible first target: half a minute for holds, ten minutes for cardio. */
    fun defaultSeconds(group: MuscleGroup): Int = if (group == MuscleGroup.CARDIO) 600 else 30

    /** The ± buttons move 5 s under a minute, 15 s under five minutes and a minute above. */
    fun step(seconds: Int, up: Boolean): Int {
        val reference = if (up) seconds else seconds - 1
        val size = when {
            reference < 60 -> 5
            reference < 300 -> 15
            else -> 60
        }
        val next = if (up) (seconds / size + 1) * size else ((seconds - 1) / size) * size
        return next.coerceIn(5, MAX_SECONDS)
    }
}
