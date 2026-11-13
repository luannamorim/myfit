package io.github.luannamorim.myfit.data.db.seed

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.domain.model.MuscleGroup
import io.github.luannamorim.myfit.testutil.inMemoryDb
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File
import java.text.Normalizer

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class ExerciseSeedTest {

    private fun key(s: String) = Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").lowercase()

    @Test
    fun seed_namesAreUnique_caseAndAccentInsensitive_inBothLanguages() {
        listOf(ExerciseSeed.items.map { key(it.namePt) }, ExerciseSeed.items.map { key(it.nameEn) }).forEach { keys ->
            assertEquals(keys.size, keys.toSet().size)
        }
    }

    @Test
    fun seed_usesDeviceLanguage() {
        val bench = ExerciseSeed.items.first()
        assertEquals("Supino reto com barra", bench.name(java.util.Locale.forLanguageTag("pt-BR")))
        assertEquals("Barbell bench press", bench.name(java.util.Locale.US))
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

    @Test
    fun seed_eachNameBelongsToOneEntry_acrossLanguages() {
        // insertMissing skips an entry when either of its names exists, so names must not collide.
        val owner = HashMap<String, SeedExercise>()
        ExerciseSeed.items.forEach { item ->
            setOf(key(item.namePt), key(item.nameEn)).forEach { name ->
                val previous = owner.put(name, item)
                assertTrue("$name is used by ${previous?.nameEn} and ${item.nameEn}", previous == null || previous === item)
            }
        }
    }

    @Test
    fun seed_keysAreUnique_andReleasedKeysNeverChange() {
        val keys = ExerciseSeed.items.map { it.key }
        assertEquals(keys.size, keys.toSet().size)
        val released = javaClass.getResource("/seed-keys.txt")!!.readText().lines()
            .map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }
        assertEquals("seed keys changed: append new keys to seed-keys.txt, never edit old ones", released, keys)
        keys.forEach { assertNotNull(ExerciseSeed.forKey(it)) }
    }

    @Test
    fun seed_everyExerciseHasABundledDrawing_andNoBundledDrawingIsUnused() {
        assertEquals(emptyList<String>(), ExerciseSeed.items.filter { it.art == null }.map { it.nameEn })
        val arts = ExerciseSeed.items.mapNotNull { it.art }.toSet()
        val bundled = File("src/main/assets/exercises").listFiles { f -> f.extension == "webp" }.orEmpty().map { it.name }.toSet()
        assertEquals(emptySet<String>(), arts.map { "$it-1.webp" }.toSet() - bundled) // start position is required
        val unused = bundled.filterNot { name -> arts.any { name == "$it-1.webp" || name == "$it-2.webp" } }
        assertEquals(emptyList<String>(), unused)
    }

    @Test
    fun seed_version3_addsIllustratedExercises() {
        val v3 = ExerciseSeed.items.filter { it.since == 3 }
        assertTrue(v3.size >= 50)
        assertTrue(v3.all { it.art != null })
        assertEquals(3, ExerciseSeed.VERSION)
    }

    @Test
    fun nameFor_followsLocale_andUnknownKeysFallBack() {
        val pt = java.util.Locale.forLanguageTag("pt-BR")
        assertEquals("Supino reto com barra", ExerciseSeed.nameFor("barbell-bench-press", pt))
        assertEquals("Barbell bench press", ExerciseSeed.nameFor("barbell-bench-press", java.util.Locale.US))
        assertNull(ExerciseSeed.nameFor("no-such-key", pt))
        assertNull(ExerciseSeed.nameFor(null, pt))
        assertEquals("Mine", ExerciseSeed.displayName("no-such-key", "Mine", pt))
        assertEquals("Supino reto com barra", ExerciseSeed.displayName("barbell-bench-press", "Meu supino", pt))
    }
}
