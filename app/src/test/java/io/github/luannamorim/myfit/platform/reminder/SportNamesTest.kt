package io.github.luannamorim.myfit.platform.reminder

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.domain.activity.Sport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "pt-rBR")
class SportNamesTest {
    private val names = SportNames(ApplicationProvider.getApplicationContext())

    @Test
    fun matchesASportsName_inPortugueseOrEnglish() {
        assertEquals(Sport.SWIMMING, names.match("Natação"))
        assertEquals(Sport.SWIMMING, names.match("  swimming "))
        assertEquals(Sport.RUNNING, names.match("corrida"))
        assertEquals(Sport.RUNNING, names.match("Running"))
        // The 1.17 editor hint suggested "Ioga"; the sport is called "Yoga".
        assertEquals(Sport.YOGA, names.match("ioga"))
    }

    @Test
    fun otherText_andTheFreeSportsOwnName_matchNothing() {
        assertNull(names.match("Beach vôlei"))
        assertNull(names.match("Livre"))
        assertNull(names.match("Other"))
    }
}
