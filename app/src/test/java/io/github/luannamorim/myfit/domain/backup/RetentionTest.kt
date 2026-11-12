package io.github.luannamorim.myfit.domain.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class RetentionTest {
    private val zone = ZoneId.of("America/Sao_Paulo")

    private fun file(day: Int, time: String = "120000") = "myfit-backup-202611${"%02d".format(day)}-$time.myfit".let { BackupFileInfo(it, it, 100, 0) }

    @Test
    fun fileName_roundTripsTimestamp() {
        val ms = LocalDateTime.of(2026, 11, 12, 14, 32, 5).atZone(zone).toInstant().toEpochMilli()
        val name = Retention.fileName(ms, zone)
        assertEquals("myfit-backup-20261112-143205.myfit", name)
        assertEquals(ms, Retention.parseCreatedAt(name, zone))
        assertNull(Retention.parseCreatedAt("foto.jpg", zone))
    }

    @Test
    fun toDelete_keepsNewest7() {
        val files = (1..9).map { file(it) }.shuffled()
        assertEquals(listOf(file(2), file(1)), Retention.toDelete(files))
    }

    @Test
    fun toDelete_ignoresForeignFiles() {
        val foreign = BackupFileInfo("x", "notas.txt", 1, 0)
        val partial = BackupFileInfo("y", "myfit-backup-20261101-120000.myfit.partial", 1, 0)
        val files = (1..8).map { file(it) } + foreign + partial
        assertEquals(listOf(file(1)), Retention.toDelete(files))
    }
}
