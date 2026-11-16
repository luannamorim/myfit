package io.github.luannamorim.myfit.domain.model

/** Sounds while a timed set is held. */
enum class HoldCue {
    /** One of the last 3 seconds before the target. */
    COUNTDOWN,
    TARGET,

    /** Another full minute held (targets of a minute or more). */
    MINUTE,
}

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

    /**
     * Sounds of a hold, as (ms since start, cue) in time order and without end (stopping the hold stops
     * them): the last 3 seconds when [countdown], the target and, when the target is a minute or more,
     * a beep at each full minute held, also past the target. A minute on the target or on a countdown
     * second is not doubled.
     */
    fun holdCues(targetMs: Long, countdown: Boolean): Sequence<Pair<Long, HoldCue>> {
        val fixed = buildList {
            if (countdown) for (second in 3 downTo 1) if (targetMs > second * 1000L) add(targetMs - second * 1000L to HoldCue.COUNTDOWN)
            add(targetMs to HoldCue.TARGET)
        }
        val minutes = if (targetMs >= MINUTE_MS) {
            generateSequence(MINUTE_MS) { it + MINUTE_MS }.filter { m -> fixed.none { it.first == m } }
        } else {
            emptySequence()
        }
        return sequence {
            var next = 0
            for (minute in minutes) {
                while (next < fixed.size && fixed[next].first < minute) yield(fixed[next++])
                yield(minute to HoldCue.MINUTE)
            }
            while (next < fixed.size) yield(fixed[next++])
        }
    }

    private const val MINUTE_MS = 60_000L
}
