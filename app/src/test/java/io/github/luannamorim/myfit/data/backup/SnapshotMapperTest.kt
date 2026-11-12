package io.github.luannamorim.myfit.data.backup

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.domain.backup.BackupArchive
import io.github.luannamorim.myfit.domain.backup.BackupDataV1
import io.github.luannamorim.myfit.testutil.SessionEnv
import io.github.luannamorim.myfit.testutil.inMemoryDb
import io.github.luannamorim.myfit.testutil.populateEverything
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class SnapshotMapperTest {
    private val env = SessionEnv()

    @After fun tearDown() = env.close()

    @Test
    fun exportReplaceAll_roundTripsEveryTable() = runBlocking {
        env.populateEverything(listOf("images/body/p.jpg"))
        val exported = SnapshotMapper.export(env.db)
        assertTrue(exported.counts().values.all { it > 0 })

        val other = inMemoryDb(seed = true) // different content that must be fully replaced
        SnapshotMapper.replaceAll(other, exported)
        assertEquals(exported, SnapshotMapper.export(other))
        assertEquals(setOf("images/body/p.jpg"), exported.referencedImages())
        other.close()
    }

    @Test
    fun json_isStable_andIgnoresUnknownFields() = runBlocking {
        env.populateEverything()
        val exported = SnapshotMapper.export(env.db)
        val text = BackupArchive.json.encodeToString(BackupDataV1.serializer(), exported)
        assertEquals(exported, BackupArchive.json.decodeFromString(BackupDataV1.serializer(), text))
        val withExtra = text.replaceFirst("{", """{"futureField":42,""")
        assertEquals(exported, BackupArchive.json.decodeFromString(BackupDataV1.serializer(), withExtra))
    }
}
