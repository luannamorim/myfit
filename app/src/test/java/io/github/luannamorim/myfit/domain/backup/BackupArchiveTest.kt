package io.github.luannamorim.myfit.domain.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.random.Random

class BackupArchiveTest {
    @get:Rule val tmp = TemporaryFolder()

    private val data = BackupDataV1(
        exercises = listOf(ExerciseDto(1, "Remada", "BACK", "PULL", "images/exercises/a.jpg", null, true, false, 0)),
        photos = listOf(PhotoDto(1, 20_000, "images/body/b.jpg", "FRONT", 0)),
    )
    private val meta = ManifestMeta("1.0.0", 10, 1_731_400_000_000, 1)

    private fun image(name: String, size: Int = 5_000) = tmp.newFile(name).apply { writeBytes(Random(name.hashCode()).nextBytes(size)) }

    private fun archive(images: Map<String, File> = mapOf("images/exercises/a.jpg" to image("a.jpg"), "images/body/b.jpg" to image("b.jpg"))): File {
        val file = tmp.newFile("backup.zip")
        file.outputStream().use { BackupArchive.write(it, data, """{"themeMode":"DARK"}""", images, meta) }
        return file
    }

    private fun staging() = File(tmp.root, "staging")

    private inline fun <reified T : BackupError> assertFails(block: () -> Unit) {
        try {
            block(); fail("expected ${T::class.simpleName}")
        } catch (e: BackupError) {
            assertTrue("got $e", e is T)
        }
    }

    /** Rewrites a zip, letting [transform] change or add entries. */
    private fun rewrite(source: File, transform: (String, ByteArray) -> Pair<String, ByteArray>?, extra: List<Pair<String, ByteArray>> = emptyList()): File {
        val out = tmp.newFile("rewritten-${Random.nextInt()}.zip")
        ZipFile(source).use { zip ->
            ZipOutputStream(out.outputStream()).use { zos ->
                zip.entries().asSequence().forEach { e ->
                    val bytes = zip.getInputStream(e).readBytes()
                    transform(e.name, bytes)?.let { (name, b) -> zos.putNextEntry(ZipEntry(name)); zos.write(b); zos.closeEntry() }
                }
                extra.forEach { (name, b) -> zos.putNextEntry(ZipEntry(name)); zos.write(b); zos.closeEntry() }
            }
        }
        return out
    }

    @Test
    fun writeRead_roundTrip_withImages() {
        val a = image("x.jpg", 70_000)
        val contents = BackupArchive.read(archive(mapOf("images/exercises/a.jpg" to a, "images/body/b.jpg" to image("b.jpg"))), staging())
        assertEquals(data, contents.data)
        assertEquals("""{"themeMode":"DARK"}""", contents.settingsJson)
        assertEquals(data.counts(), contents.manifest.counts)
        assertArrayEquals(a.readBytes(), File(staging(), "images/exercises/a.jpg").readBytes())
        assertEquals(1, contents.manifest.formatVersion)
    }

    @Test
    fun hashMismatch_rejected() = assertFails<BackupError.Corrupted> {
        val bad = rewrite(archive(), { name, b -> if (name == "images/body/b.jpg") name to b.copyOf().also { it[10] = (it[10] + 1).toByte() } else name to b })
        BackupArchive.read(bad, staging())
    }

    @Test
    fun zipSlipEntry_rejected() {
        assertFalse(BackupArchive.isSafeImagePath("images/../../evil.jpg"))
        assertFalse(BackupArchive.isSafeImagePath("/images/body/a.jpg"))
        assertFalse(BackupArchive.isSafeImagePath("images/body/sub/a.jpg"))
        assertTrue(BackupArchive.isSafeImagePath("images/body/a.jpg"))
        assertFails<BackupError.Corrupted> {
            BackupArchive.read(rewrite(archive(), { n, b -> n to b }, listOf("images/../evil.jpg" to byteArrayOf(1))), staging())
        }
        assertFalse(File(tmp.root, "evil.jpg").exists())
    }

    @Test
    fun unsafeImagePathInData_rejected() = assertFails<BackupError.Corrupted> {
        val evil = data.copy(photos = listOf(PhotoDto(1, 20_000, "images/../../databases/myfit.db", "FRONT", 0)))
        val file = tmp.newFile("evil.zip")
        file.outputStream().use { BackupArchive.write(it, evil, "{}", emptyMap(), meta) }
        BackupArchive.read(file, staging())
    }

    @Test
    fun missingManifest_rejected() = assertFails<BackupError.Corrupted> {
        BackupArchive.read(rewrite(archive(), { n, b -> if (n == BackupArchive.MANIFEST) null else n to b }), staging())
    }

    @Test
    fun newerFormatVersion_rejected() = assertFails<BackupError.UnsupportedVersion> {
        BackupArchive.read(
            rewrite(archive(), { n, b -> if (n == BackupArchive.MANIFEST) n to b.decodeToString().replace("\"formatVersion\":1", "\"formatVersion\":2").toByteArray() else n to b }),
            staging(),
        )
    }

    @Test
    fun entryNotInManifest_rejected() = assertFails<BackupError.Corrupted> {
        BackupArchive.read(rewrite(archive(), { n, b -> n to b }, listOf("images/body/extra.jpg" to byteArrayOf(1, 2))), staging())
    }

    @Test
    fun notAZip_rejected() = assertFails<BackupError.Corrupted> {
        BackupArchive.read(tmp.newFile("junk.zip").apply { writeBytes(Random.nextBytes(1000)) }, staging())
    }
}
