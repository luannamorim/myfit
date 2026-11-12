package io.github.luannamorim.myfit.data.db.seed

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.domain.model.MuscleGroup
import io.github.luannamorim.myfit.testutil.inMemoryDb
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.text.Normalizer

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class ExerciseSeedTest {

    private fun key(s: String) = Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").lowercase()

    @Test
    fun seed_namesAreUnique_caseAndAccentInsensitive() {
        val keys = ExerciseSeed.items.map { key(it.name) }
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun seed_coversEveryMuscleGroupExceptOther() {
        val covered = ExerciseSeed.items.map { it.muscleGroup }.toSet()
        val missing = MuscleGroup.entries.filter { it != MuscleGroup.OTHER && it !in covered }
        assertTrue("missing: $missing", missing.isEmpty())
    }

    @Test
    fun freshDatabase_containsSeed() = runTest {
        val db = inMemoryDb(seed = true)
        val all = db.exerciseDao().getAll()
        assertEquals(ExerciseSeed.items.size, all.size)
        assertTrue(all.none { it.isCustom })
        db.close()
    }
}
