package io.github.luannamorim.myfit.domain.reminder

import io.github.luannamorim.myfit.domain.activity.Sport
import io.github.luannamorim.myfit.domain.model.Reminder
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderLinksTest {
    private val match: (String) -> Sport? = { if (it == "Natação") Sport.SWIMMING else null }

    @Test
    fun freeText_getsTheMatchingSport_orOtherWithItsLabel() {
        val linked = linkSports(listOf(Reminder(1, null, "Natação", 420), Reminder(2, null, "Beach vôlei", 480)), match)
        assertEquals(Reminder(1, null, "Natação", 420, sport = Sport.SWIMMING), linked[0])
        assertEquals(Reminder(2, null, "Beach vôlei", 480, sport = Sport.OTHER), linked[1])
    }

    @Test
    fun routineAndLinkedReminders_areLeftAlone() {
        // A routine reminder stays one even when its routine was deleted (Home skips it).
        val routine = Reminder(1, 99, "Natação", 420)
        val linked = Reminder(2, null, "Natação", 420, sport = Sport.OTHER)
        val list = listOf(routine, linked)
        assertEquals(list, linkSports(list, match))
    }
}
