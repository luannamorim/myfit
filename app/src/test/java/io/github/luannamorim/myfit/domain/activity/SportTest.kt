package io.github.luannamorim.myfit.domain.activity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class SportTest {
    @Test
    fun gym_isASportWithoutDistanceOrGps() {
        val gym = Sport.valueOf("GYM")
        assertNull(gym.distance)
        assertFalse(gym.gpsCapable)
        assertEquals(Sport.entries.indexOf(Sport.OTHER) - 1, Sport.entries.indexOf(gym)) // listed right before "Other"
    }
}
