package io.github.luannamorim.myfit.data.backup

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.domain.backup.AssessmentDto
import io.github.luannamorim.myfit.domain.backup.BackupArchive
import io.github.luannamorim.myfit.domain.backup.BackupDataV1
import io.github.luannamorim.myfit.testutil.SessionEnv
import io.github.luannamorim.myfit.testutil.inMemoryDb
import io.github.luannamorim.myfit.testutil.populateEverything
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        env.populateEverything(listOf("images/body/p.jpg"), sessionImage = "images/sessions/s.jpg", activityImage = "images/sessions/a.jpg")
        val exported = SnapshotMapper.export(env.db)
        assertTrue(exported.counts().values.all { it > 0 })
        assertEquals(1, exported.counts()["activities"])
        assertEquals("HIGH", exported.sessions.first { it.status == "FINISHED" }.intensity)
        assertEquals(178.0, exported.assessments.single().heightCm!!, 0.0)

        val other = inMemoryDb(seed = true) // different content that must be fully replaced
        SnapshotMapper.replaceAll(other, exported)
        assertEquals(exported, SnapshotMapper.export(other))
        assertEquals(setOf("images/body/p.jpg", "images/sessions/s.jpg", "images/sessions/a.jpg"), exported.referencedImages())
        other.close()
    }

    @Test
    fun counts_ofBackupsWithoutActivities_matchManifestsWrittenBeforeVersion4() {
        // Older manifests have no "activities" key; the check compares maps exactly.
        val before = setOf("exercises", "routines", "routineExercises", "sessions", "sessionExercises", "sessionSets", "assessments", "photos")
        assertEquals(before, BackupDataV1().counts().keys)
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

    @Test
    fun assessmentOfBackupsMadeBeforeHeight_hasNoHeight() {
        val old = """{"id":1,"dateEpochDay":20400,"weightKg":73.2,"createdAtMs":1,"updatedAtMs":2}"""
        val dto = BackupArchive.json.decodeFromString(AssessmentDto.serializer(), old)
        assertNull(dto.heightCm)
        assertEquals(73.2, dto.weightKg!!, 0.0)
    }

    @Test
    fun replaceAll_linksLibraryRowsOfBackupsMadeBeforeSeedKeys() = runBlocking {
        val db = inMemoryDb(seed = true)
        val old = SnapshotMapper.export(db).let { data ->
            data.copy(exercises = data.exercises.map { it.copy(seedKey = null) }) // a v1.2 backup
        }
        val other = inMemoryDb(seed = false)
        SnapshotMapper.replaceAll(other, old)
        val restored = other.exerciseDao().getAll()
        assertEquals(old.exercises.size, restored.size)
        assertTrue(restored.all { it.seedKey != null })
        assertEquals("barbell-bench-press", restored.single { it.name == "Supino reto com barra" }.seedKey)
        db.close()
        other.close()
    }
}
