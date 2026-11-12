package io.github.luannamorim.myfit.data.prefs

import io.github.luannamorim.myfit.domain.model.Reminder
import io.github.luannamorim.myfit.testutil.testSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek

class ReminderRepositoryTest {
    private val settings = testSettings()
    private val repo = ReminderRepository(settings)

    private fun reminder(label: String) = Reminder(id = 0, label = label, minuteOfDay = 420, days = setOf(DayOfWeek.MONDAY))

    @Test
    fun save_new_getsIdsAboveTheExistingOnes_andKeepsOrder() = runBlocking {
        assertEquals(1L, repo.save(reminder("A")))
        assertEquals(2L, repo.save(reminder("B")))
        repo.delete(1)
        assertEquals(3L, repo.save(reminder("C")))
        assertEquals(listOf(2L to "B", 3L to "C"), repo.reminders.first().map { it.id to it.label })
    }

    @Test
    fun save_existing_replacesInPlace() = runBlocking {
        val a = repo.save(reminder("A"))
        repo.save(reminder("B"))
        repo.save(reminder("A2").copy(id = a, minuteOfDay = 600))
        val all = repo.all()
        assertEquals(listOf("A2", "B"), all.map { it.label })
        assertEquals(600, repo.get(a)!!.minuteOfDay)
    }

    @Test
    fun setEnabled_andDelete() = runBlocking {
        val a = repo.save(reminder("A"))
        repo.setEnabled(a, false)
        assertFalse(repo.get(a)!!.enabled)
        repo.delete(a)
        assertNull(repo.get(a))
        assertEquals(emptyList<Reminder>(), settings.current().reminders)
    }
}
