package io.github.luannamorim.myfit.core.time

import java.time.ZoneId

/** Controllable clock for tests. */
class FakeTimeSource(
    var current: Mark = Mark(wallMs = 1_731_400_000_000, elapsedMs = 10_000_000, bootCount = 1),
    var zoneId: ZoneId = ZoneId.of("America/Sao_Paulo"),
) : TimeSource {
    override fun now(): Mark = current
    override fun zone(): ZoneId = zoneId

    /** Real time passes: both clocks move. */
    fun advance(ms: Long) {
        current = current.copy(wallMs = current.wallMs + ms, elapsedMs = current.elapsedMs + ms)
    }

    /** User/network changes the wall clock only. */
    fun jumpWall(ms: Long) {
        current = current.copy(wallMs = current.wallMs + ms)
    }

    fun reboot(wallMs: Long) {
        current = Mark(wallMs = wallMs, elapsedMs = 0, bootCount = current.bootCount + 1)
    }
}
