package io.github.luannamorim.myfit.ui.common

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.domain.model.MuscleGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class MuscleMapTest {
    private val regions = MuscleGroup.entries.filter { it !in setOf(MuscleGroup.FULL_BODY, MuscleGroup.CARDIO, MuscleGroup.OTHER) }

    @Test
    fun everyMuscleGroupHasARegion_onTheFrontOrTheBack() {
        val drawn = (BodyMap.front + BodyMap.back).mapNotNull { it.group }.toSet()
        assertEquals(regions.toSet(), drawn)
    }

    @Test
    fun selection_lightsOnlyItsGroup_fullBodyLightsAll_cardioAndOtherLightNothing() {
        assertTrue(BodyMap.lights(MuscleGroup.CHEST, MuscleGroup.CHEST))
        assertFalse(BodyMap.lights(MuscleGroup.CHEST, MuscleGroup.BACK))
        assertTrue(regions.all { BodyMap.lights(MuscleGroup.FULL_BODY, it) })
        assertTrue(regions.none { BodyMap.lights(MuscleGroup.CARDIO, it) || BodyMap.lights(MuscleGroup.OTHER, it) || BodyMap.lights(null, it) })
    }
}
