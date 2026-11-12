package io.github.luannamorim.myfit.core.time

/**
 * A point in time captured with both clocks. Within the same boot the monotonic
 * [elapsedMs] is immune to wall-clock changes; across reboots only [wallMs] is comparable.
 */
data class Mark(val wallMs: Long, val elapsedMs: Long, val bootCount: Int)

/** Milliseconds from this mark until [later], never negative. */
fun Mark.millisUntil(later: Mark): Long {
    val delta = if (bootCount == later.bootCount) later.elapsedMs - elapsedMs else later.wallMs - wallMs
    return delta.coerceAtLeast(0)
}
