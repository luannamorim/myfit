package io.github.luannamorim.myfit.ui.feature.reminders

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.testing.invoke
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.core.time.FakeTimeSource
import io.github.luannamorim.myfit.core.time.Mark
import io.github.luannamorim.myfit.data.prefs.ReminderRepository
import io.github.luannamorim.myfit.data.repository.BackupTrigger
import io.github.luannamorim.myfit.data.repository.RoutineRepository
import io.github.luannamorim.myfit.domain.model.Reminder
import io.github.luannamorim.myfit.testutil.MainDispatcherRule
import io.github.luannamorim.myfit.testutil.awaitUntil
import io.github.luannamorim.myfit.testutil.inMemoryDb
import io.github.luannamorim.myfit.testutil.insertExercise
import io.github.luannamorim.myfit.testutil.insertRoutine
import io.github.luannamorim.myfit.testutil.testSettings
import io.github.luannamorim.myfit.ui.navigation.ReminderEditorRoute
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class ReminderEditorViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val zone = ZoneId.of("America/Sao_Paulo")
    private val today = LocalDate.parse("2026-11-12")

    // 10:20 on Thursday, 2026-11-12.
    private val time = FakeTimeSource(Mark(LocalDateTime.parse("2026-11-12T10:20").atZone(zone).toInstant().toEpochMilli(), 1_000, 1), zone)
    private val db = inMemoryDb()
    private val routines = RoutineRepository(db, time, BackupTrigger { })
    private val reminders = ReminderRepository(testSettings())

    @After fun tearDown() = db.close()

    private suspend fun routine(name: String): Long = db.insertRoutine(name, listOf(db.insertExercise("Remada $name")))

    private suspend fun editor(id: Long? = null): ReminderEditorViewModel {
        val vm = ReminderEditorViewModel(SavedStateHandle(ReminderEditorRoute(id)), reminders, routines, time)
        vm.state.awaitUntil { !it.loading }
        return vm
    }

    @Test
    fun new_defaultsToFirstRoutine_nextFullHourToday_once() = runBlocking {
        val costas = routine("Costas")
        routine("Pernas")
        val vm = editor()
        val s = vm.state.value
        assertTrue(s.isNew)
        assertEquals(listOf("Costas", "Pernas"), s.routines.map { it.name })
        assertEquals(costas, s.routineId)
        assertEquals(11 * 60, s.minuteOfDay)
        assertEquals(today, s.date)
        assertTrue(s.days.isEmpty())

        vm.save()
        vm.state.awaitUntil { it.done }
        assertEquals(
            Reminder(id = 1, routineId = costas, label = "Costas", minuteOfDay = 11 * 60, dateEpochDay = today.toEpochDay()),
            reminders.all().single(),
        )
    }

    @Test
    fun otherActivity_requiresText_andSavesTrimmed() = runBlocking {
        val vm = editor()
        assertNull(vm.state.value.routineId) // no routines: other activity
        assertFalse(vm.validate())
        assertTrue(vm.state.value.activityError)

        vm.setActivity("  Corrida ")
        assertFalse(vm.state.value.activityError)
        vm.save()
        vm.state.awaitUntil { it.done }
        val saved = reminders.all().single()
        assertEquals("Corrida", saved.label)
        assertNull(saved.routineId)
    }

    @Test
    fun once_inThePast_isRejected_weekly_isNot() = runBlocking {
        val vm = editor()
        vm.setActivity("Ioga")
        vm.setTime(10 * 60) // 10:00 today, already gone
        assertFalse(vm.validate())
        assertTrue(vm.state.value.pastError)

        vm.toggleDay(DayOfWeek.THURSDAY) // weekly: next Thursday at 10:00 is fine
        assertFalse(vm.state.value.pastError)
        vm.save()
        vm.state.awaitUntil { it.done }
        val saved = reminders.all().single()
        assertEquals(setOf(DayOfWeek.THURSDAY), saved.days)
        assertNull(saved.dateEpochDay)
    }

    @Test
    fun once_tomorrowEarlier_isAccepted() = runBlocking {
        val vm = editor()
        vm.setActivity("Ioga")
        vm.setTime(6 * 60)
        vm.setDate(today.plusDays(1))
        assertTrue(vm.validate())
    }

    @Test
    fun edit_loadsReminder_reEnablesOnSave_andDeletes() = runBlocking {
        val pernas = routine("Pernas")
        val id = reminders.save(Reminder(id = 0, routineId = pernas, label = "Pernas", minuteOfDay = 7 * 60, days = setOf(DayOfWeek.MONDAY), enabled = false))
        val vm = editor(id)
        val s = vm.state.value
        assertFalse(s.isNew)
        assertEquals(pernas, s.routineId)
        assertEquals(setOf(DayOfWeek.MONDAY), s.days)
        assertEquals(7 * 60, s.minuteOfDay)

        vm.toggleDay(DayOfWeek.WEDNESDAY)
        vm.save()
        vm.state.awaitUntil { it.done }
        val saved = reminders.get(id)!!
        assertEquals(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), saved.days)
        assertTrue(saved.enabled)

        val again = editor(id)
        again.delete()
        again.state.awaitUntil { it.done }
        assertNull(reminders.get(id))
    }

    @Test
    fun edit_withDeletedRoutine_fallsBackToOtherActivity() = runBlocking {
        val id = reminders.save(Reminder(id = 0, routineId = 404, label = "Treino antigo", minuteOfDay = 8 * 60, dateEpochDay = today.plusDays(3).toEpochDay()))
        val s = editor(id).state.value
        assertNull(s.routineId)
        assertEquals("Treino antigo", s.activity)
        assertEquals(today.plusDays(3), s.date)
    }
}
