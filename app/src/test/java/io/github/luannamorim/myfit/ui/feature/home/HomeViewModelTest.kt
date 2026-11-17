package io.github.luannamorim.myfit.ui.feature.home

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.core.time.today
import io.github.luannamorim.myfit.data.prefs.SettingsRepository
import io.github.luannamorim.myfit.data.prefs.LiveActivityStore
import io.github.luannamorim.myfit.data.repository.ActivityRepository
import io.github.luannamorim.myfit.data.repository.BackupTrigger
import io.github.luannamorim.myfit.data.repository.BodyRepository
import io.github.luannamorim.myfit.data.repository.HistoryRepository
import io.github.luannamorim.myfit.data.repository.RoutineRepository
import io.github.luannamorim.myfit.domain.activity.Activity
import io.github.luannamorim.myfit.domain.activity.Sport
import io.github.luannamorim.myfit.domain.model.Reminder
import io.github.luannamorim.myfit.domain.model.RoutineOverview
import io.github.luannamorim.myfit.domain.session.SessionEngine
import io.github.luannamorim.myfit.platform.activity.ActivityServiceStarter
import io.github.luannamorim.myfit.platform.activity.LiveActivityController
import io.github.luannamorim.myfit.testutil.MainDispatcherRule
import io.github.luannamorim.myfit.testutil.SessionEnv
import io.github.luannamorim.myfit.testutil.awaitUntil
import io.github.luannamorim.myfit.testutil.insertExercise
import io.github.luannamorim.myfit.testutil.insertRoutine
import io.github.luannamorim.myfit.testutil.testSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class HomeViewModelTest {
    @get:Rule val tmp = TemporaryFolder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @get:Rule val main = MainDispatcherRule()
    private val env = SessionEnv()

    @After fun tearDown() {
        env.close()
        scope.cancel()
    }

    @Test
    fun greeting_byHour() {
        assertEquals(Greeting.EVENING, Greeting.forHour(4))
        assertEquals(Greeting.MORNING, Greeting.forHour(5))
        assertEquals(Greeting.MORNING, Greeting.forHour(11))
        assertEquals(Greeting.AFTERNOON, Greeting.forHour(12))
        assertEquals(Greeting.AFTERNOON, Greeting.forHour(17))
        assertEquals(Greeting.EVENING, Greeting.forHour(18))
    }

    @Test
    fun suggestion_prefersNeverDone_thenOldest() {
        fun r(id: Long, count: Int, last: Long?) = RoutineOverview(id, "R$id", null, count, last)
        assertEquals(2L, suggestRoutine(listOf(r(1, 5, 100), r(2, 5, null), r(3, 5, null)))?.id)
        assertEquals(3L, suggestRoutine(listOf(r(1, 5, 100), r(2, 0, null), r(3, 5, 90)))?.id)
        assertNull(suggestRoutine(listOf(r(1, 0, null))))
    }

    // Wednesday 2026-11-18, 10:00 in São Paulo.
    private val wed = ZonedDateTime.of(2026, 11, 18, 10, 0, 0, 0, ZoneId.of("America/Sao_Paulo"))
    private val todayDay = wed.toLocalDate().toEpochDay()
    private fun routine(id: Long, count: Int = 5, last: Long? = null) = RoutineOverview(id, "R$id", null, count, last)
    private fun weekly(id: Long, routineId: Long?, minute: Int, vararg days: DayOfWeek, enabled: Boolean = true) =
        Reminder(id, routineId, "R$routineId", minute, days = days.toSet(), enabled = enabled)

    @Test
    fun scheduled_picksTheNextReminderRoutine() {
        val routines = listOf(routine(1), routine(2), routine(3))
        val reminders = listOf(
            weekly(1, 1, 18 * 60, DayOfWeek.FRIDAY),
            weekly(2, 2, 7 * 60, DayOfWeek.THURSDAY),
            weekly(3, 3, 18 * 60, DayOfWeek.SATURDAY),
        )
        val next = scheduledRoutine(routines, reminders, wed)!!
        assertEquals(2L, next.routine.id)
        assertEquals(LocalDateTime.of(2026, 11, 19, 7, 0), next.at)
    }

    @Test
    fun scheduled_todaysReminderStaysAfterItsTime_untilThatRoutineIsDone() {
        val reminders = listOf(weekly(1, 1, 7 * 60, DayOfWeek.WEDNESDAY), weekly(2, 2, 18 * 60, DayOfWeek.THURSDAY))
        // 07:00 passed, routine 1 not done today: still today's workout.
        val pending = scheduledRoutine(listOf(routine(1, last = todayDay - 7), routine(2)), reminders, wed)!!
        assertEquals(1L, pending.routine.id)
        assertEquals(LocalDateTime.of(2026, 11, 18, 7, 0), pending.at)
        // Done today: tomorrow's reminder is next.
        val done = scheduledRoutine(listOf(routine(1, last = todayDay), routine(2)), reminders, wed)!!
        assertEquals(2L, done.routine.id)
        // Only reminder and done today: its next week's slot.
        val alone = scheduledRoutine(listOf(routine(1, last = todayDay)), reminders.take(1), wed)!!
        assertEquals(LocalDateTime.of(2026, 11, 25, 7, 0), alone.at)
    }

    @Test
    fun scheduled_missedWorkoutLeavesAfterFourHours_orWhenAnotherRemindersTimeComes() {
        val routines = listOf(routine(1), routine(2), routine(9))
        val tomorrow = weekly(9, 9, 18 * 60, DayOfWeek.THURSDAY)
        // 07:00 missed 3 h ago: still on the card.
        assertEquals(
            LocalDateTime.of(2026, 11, 18, 7, 0),
            scheduledRoutine(routines, listOf(weekly(1, 1, 7 * 60, DayOfWeek.WEDNESDAY), tomorrow), wed)?.at,
        )
        // 06:00 missed 4 h ago: gone, tomorrow's is next; alone, its next week's slot.
        assertEquals(9L, scheduledRoutine(routines, listOf(weekly(1, 1, 6 * 60, DayOfWeek.WEDNESDAY), tomorrow), wed)?.routine?.id)
        assertEquals(
            LocalDateTime.of(2026, 11, 25, 6, 0),
            scheduledRoutine(routines, listOf(weekly(1, 1, 6 * 60, DayOfWeek.WEDNESDAY)), wed)?.at,
        )
        // 07:00 missed, but another reminder's time (09:00) came: that one.
        val both = listOf(weekly(1, 1, 7 * 60, DayOfWeek.WEDNESDAY), weekly(2, 2, 9 * 60, DayOfWeek.WEDNESDAY), tomorrow)
        assertEquals(LocalDateTime.of(2026, 11, 18, 9, 0), scheduledRoutine(routines, both, wed)?.at)
        // That one done (the 07:00 skipped for it): the next, not the 07:00 again.
        assertEquals(9L, scheduledRoutine(listOf(routine(1), routine(2, last = todayDay), routine(9)), both, wed)?.routine?.id)
        // Disabled and free-text reminders' times don't move it.
        val others = listOf(
            weekly(1, 1, 7 * 60, DayOfWeek.WEDNESDAY),
            weekly(2, 2, 9 * 60, DayOfWeek.WEDNESDAY, enabled = false),
            weekly(3, null, 9 * 60, DayOfWeek.WEDNESDAY),
            tomorrow,
        )
        assertEquals(1L, scheduledRoutine(routines, others, wed)?.routine?.id)
        // Past midnight: last night's 22:00 still counts at 01:00.
        val night = ZonedDateTime.of(2026, 11, 18, 1, 0, 0, 0, wed.zone)
        assertEquals(
            LocalDateTime.of(2026, 11, 17, 22, 0),
            scheduledRoutine(routines, listOf(weekly(1, 1, 22 * 60, DayOfWeek.TUESDAY), tomorrow), night)?.at,
        )
    }

    @Test
    fun scheduled_routineDoneSinceItsPreviousSlot_skipsTheComingSlot() {
        val reminders = listOf(weekly(1, 1, 18 * 60, DayOfWeek.THURSDAY), weekly(2, 2, 7 * 60, DayOfWeek.FRIDAY))
        // Tomorrow's routine done ahead (today, or Monday after last Thursday's slot): Friday's comes first.
        for (last in listOf(todayDay, todayDay - 2)) {
            assertEquals(2L, scheduledRoutine(listOf(routine(1, last = last), routine(2)), reminders, wed)?.routine?.id)
        }
        // Done on its previous slot (last Thursday): tomorrow's still to do.
        assertEquals(1L, scheduledRoutine(listOf(routine(1, last = todayDay - 6), routine(2)), reminders, wed)?.routine?.id)
        // Only reminder and done ahead: the slot after tomorrow's.
        assertEquals(
            LocalDateTime.of(2026, 11, 26, 18, 0),
            scheduledRoutine(listOf(routine(1, last = todayDay)), reminders.take(1), wed)?.at,
        )
        // Several days: Monday/Thursday, done Tuesday counts for Thursday; done Monday was Monday's.
        val twice = listOf(weekly(1, 1, 18 * 60, DayOfWeek.MONDAY, DayOfWeek.THURSDAY))
        assertEquals(
            LocalDateTime.of(2026, 11, 23, 18, 0),
            scheduledRoutine(listOf(routine(1, last = todayDay - 1)), twice, wed)?.at,
        )
        assertEquals(
            LocalDateTime.of(2026, 11, 19, 18, 0),
            scheduledRoutine(listOf(routine(1, last = todayDay - 2)), twice, wed)?.at,
        )
    }

    @Test
    fun scheduled_oneOffReminders() {
        fun once(id: Long, routineId: Long, date: LocalDate, minute: Int) = Reminder(id, routineId, "R$routineId", minute, dateEpochDay = date.toEpochDay())
        val routines = listOf(routine(1), routine(2))
        assertEquals(
            LocalDateTime.of(2026, 11, 20, 9, 0),
            scheduledRoutine(routines, listOf(once(1, 1, LocalDate.of(2026, 11, 20), 9 * 60)), wed)?.at,
        )
        // Yesterday's one-off is gone; earlier today counts for 4 hours.
        assertNull(scheduledRoutine(routines, listOf(once(1, 1, LocalDate.of(2026, 11, 17), 9 * 60)), wed))
        assertEquals(1L, scheduledRoutine(routines, listOf(once(1, 1, LocalDate.of(2026, 11, 18), 7 * 60)), wed)?.routine?.id)
        assertNull(scheduledRoutine(routines, listOf(once(1, 1, LocalDate.of(2026, 11, 18), 6 * 60)), wed))
        // Friday's one-off done ahead today is gone; done before today it still counts.
        val friday = listOf(once(1, 1, LocalDate.of(2026, 11, 20), 9 * 60))
        assertNull(scheduledRoutine(listOf(routine(1, last = todayDay)), friday, wed))
        assertEquals(1L, scheduledRoutine(listOf(routine(1, last = todayDay - 1)), friday, wed)?.routine?.id)
    }

    @Test
    fun scheduled_skipsDisabledFreeTextMissingAndEmptyRoutines() {
        val routines = listOf(routine(1), routine(2, count = 0), routine(5))
        val reminders = listOf(
            weekly(1, 1, 18 * 60, DayOfWeek.THURSDAY, enabled = false),
            weekly(2, null, 18 * 60, DayOfWeek.THURSDAY),
            weekly(3, 2, 18 * 60, DayOfWeek.THURSDAY),
            weekly(4, 9, 18 * 60, DayOfWeek.THURSDAY),
            weekly(5, 5, 18 * 60, DayOfWeek.SUNDAY),
        )
        assertEquals(5L, scheduledRoutine(routines, reminders, wed)?.routine?.id)
        assertNull(scheduledRoutine(routines, reminders.take(4), wed))
    }

    @Test
    fun state_nextWorkoutFollowsReminders() = runBlocking {
        val exercise = env.db.insertExercise("Remada")
        val first = env.db.insertRoutine("A", listOf(exercise), position = 0)
        val second = env.db.insertRoutine("B", listOf(exercise), position = 1)
        val settings = testSettings()
        // No reminder: the never-done suggestion (first in order).
        assertEquals(first, homeViewModel(settings).state.awaitUntil { !it.loading }.suggested?.id)

        settings.update { it.copy(reminders = listOf(Reminder(1, second, "B", 23 * 60 + 59, days = DayOfWeek.entries.toSet()))) }
        val state = homeViewModel(settings).state.awaitUntil { !it.loading && it.suggestedAt != null }
        assertEquals(second, state.suggested?.id)
        assertEquals(LocalDateTime.of(env.time.today(), LocalTime.of(23, 59)), state.suggestedAt)
    }

    private fun homeViewModel(settings: SettingsRepository = testSettings()): HomeViewModel {
        val activities = ActivityRepository(env.db, env.time, BackupTrigger { })
        return HomeViewModel(
            RoutineRepository(env.db, env.time, BackupTrigger { }),
            HistoryRepository(env.db, env.time, BackupTrigger { }),
            activities,
            env.controller,
            LiveActivityController(
                LiveActivityStore(PreferenceDataStoreFactory.create(scope = scope) { File(tmp.newFolder(), "live.preferences_pb") }),
                activities, env.time,
                object : ActivityServiceStarter {
                    override fun start() = Unit
                    override fun stop() = Unit
                },
                scope,
            ),
            BodyRepository(env.db.bodyDao(), env.time, BackupTrigger { }),
            settings,
            env.time,
        )
    }

    @Test
    fun today_listsOnlyTodaysFinishedSessions() = runBlocking {
        env.startWorkout(1)
        val s = SessionEngine.completeSet(env.repo.getActive()!!, 12, 40.0, env.time.now())
        env.repo.finish(s, env.time.now(), updateRoutineLoads = false)
        env.startWorkout(1) // a second, still active session: not in "today"
        val activities = ActivityRepository(env.db, env.time, BackupTrigger { })
        activities.save(Activity(sport = Sport.SOCCER, date = env.time.today(), durationMinutes = 60))

        val vm = homeViewModel()
        val state = vm.state.awaitUntil { !it.loading && it.todaySessions.isNotEmpty() && it.active != null }
        assertEquals(1, state.todaySessions.size)
        assertEquals(1, state.week.sessions)
        assertEquals(1, state.week.activities)
        assertTrue(state.week.totalDurationMs >= 60 * 60_000L) // the activity's hour counts in the week
        assertEquals(1, state.week.days.count { it.trained })
        assertEquals(1, state.week.days.count { it.isToday })
    }
}
