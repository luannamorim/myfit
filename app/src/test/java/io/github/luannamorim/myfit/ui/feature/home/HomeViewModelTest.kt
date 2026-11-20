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
import io.github.luannamorim.myfit.domain.nutrition.Meal
import io.github.luannamorim.myfit.domain.body.Sex
import io.github.luannamorim.myfit.domain.body.Measure
import io.github.luannamorim.myfit.domain.body.BodyAssessment
import io.github.luannamorim.myfit.data.repository.NutritionTracker
import io.github.luannamorim.myfit.data.repository.NutritionRepository
import io.github.luannamorim.myfit.data.repository.RoutineRepository
import io.github.luannamorim.myfit.domain.activity.Activity
import io.github.luannamorim.myfit.domain.activity.LiveActivity
import io.github.luannamorim.myfit.domain.activity.Sport
import io.github.luannamorim.myfit.domain.activity.SportLastDay
import io.github.luannamorim.myfit.core.time.Mark
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
import java.time.temporal.TemporalAdjusters

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

    private fun activity(id: Long, sport: Sport, minute: Int, vararg days: DayOfWeek, label: String = sport.name) =
        Reminder(id, null, label, minute, days = days.toSet(), sport = sport)
    private fun swimDay(day: Long, sport: Sport = Sport.SWIMMING, name: String? = null) = SportLastDay(sport, name, day)
    private fun live(sport: Sport, name: String? = null) = LiveActivity(sport, name, startedAt = Mark(wed.toInstant().toEpochMilli(), 0, 0))

    @Test
    fun scheduled_anActivityReminderIsNext() {
        val reminders = listOf(weekly(1, 1, 18 * 60, DayOfWeek.FRIDAY), activity(2, Sport.SWIMMING, 7 * 60, DayOfWeek.THURSDAY))
        // Last swim before last Thursday: this Thursday's slot is pending.
        val next = scheduledNext(listOf(routine(1)), reminders, wed, listOf(swimDay(todayDay - 7)))!!
        assertEquals(ActivityTarget(Sport.SWIMMING, null, todayDay - 7), next.target)
        assertNull(next.routine)
        assertEquals(LocalDateTime.of(2026, 11, 19, 7, 0), next.at)
    }

    @Test
    fun scheduled_anActivityIsDoneOnceThatSportIsLogged() {
        val swim = listOf(activity(1, Sport.SWIMMING, 7 * 60, DayOfWeek.WEDNESDAY))
        // Logged today: next week's slot.
        assertEquals(LocalDateTime.of(2026, 11, 25, 7, 0), scheduledNext(emptyList(), swim, wed, listOf(swimDay(todayDay)))?.at)
        // Another sport logged today doesn't count.
        assertEquals(LocalDateTime.of(2026, 11, 18, 7, 0), scheduledNext(emptyList(), swim, wed, listOf(swimDay(todayDay, Sport.RUNNING)))?.at)
        // Logged before the reminder's previous day (last Wednesday): today's slot is pending.
        assertEquals(LocalDateTime.of(2026, 11, 18, 7, 0), scheduledNext(emptyList(), swim, wed, listOf(swimDay(todayDay - 7)))?.at)
    }

    @Test
    fun scheduled_otherActivity_matchesItsNameIgnoringCase() {
        val volley = listOf(activity(1, Sport.OTHER, 7 * 60, DayOfWeek.WEDNESDAY, label = "Beach vôlei"))
        val logged = listOf(swimDay(todayDay, Sport.OTHER, "beach Vôlei "))
        val next = scheduledNext(emptyList(), volley, wed, logged)!!
        assertEquals(ActivityTarget(Sport.OTHER, "Beach vôlei", todayDay), next.target)
        assertEquals(LocalDateTime.of(2026, 11, 25, 7, 0), next.at)
        assertEquals(LocalDateTime.of(2026, 11, 18, 7, 0), scheduledNext(emptyList(), volley, wed, listOf(swimDay(todayDay, Sport.OTHER, "Surf")))?.at)
    }

    @Test
    fun scheduled_aRunningTimerOfThatSportCountsAsDone() {
        val swim = listOf(activity(1, Sport.SWIMMING, 7 * 60, DayOfWeek.WEDNESDAY))
        assertEquals(LocalDateTime.of(2026, 11, 25, 7, 0), scheduledNext(emptyList(), swim, wed, live = live(Sport.SWIMMING))?.at)
        assertEquals(LocalDateTime.of(2026, 11, 18, 7, 0), scheduledNext(emptyList(), swim, wed, live = live(Sport.RUNNING))?.at)
        val volley = listOf(activity(1, Sport.OTHER, 7 * 60, DayOfWeek.WEDNESDAY, label = "Beach vôlei"))
        assertEquals(LocalDateTime.of(2026, 11, 25, 7, 0), scheduledNext(emptyList(), volley, wed, live = live(Sport.OTHER, "BEACH VÔLEI"))?.at)
        assertEquals(LocalDateTime.of(2026, 11, 18, 7, 0), scheduledNext(emptyList(), volley, wed, live = live(Sport.OTHER, "Surf"))?.at)
    }

    @Test
    fun scheduled_routinesAndActivitiesShareTheRules() {
        val routines = listOf(routine(1))
        // The earliest wins, whichever kind.
        val both = listOf(weekly(1, 1, 18 * 60, DayOfWeek.WEDNESDAY), activity(2, Sport.RUNNING, 12 * 60, DayOfWeek.WEDNESDAY))
        assertEquals(Sport.RUNNING, (scheduledNext(routines, both, wed)?.target as ActivityTarget).sport)
        // A missed run (07:00) gives way once the routine reminder's time (09:00) has come.
        val missed = listOf(activity(1, Sport.RUNNING, 7 * 60, DayOfWeek.WEDNESDAY), weekly(2, 1, 9 * 60, DayOfWeek.WEDNESDAY))
        assertEquals(1L, scheduledNext(routines, missed, wed)?.routine?.id)
        // Free-text reminders not linked to a sport yet are skipped.
        assertNull(scheduledNext(routines, listOf(Reminder(1, null, "Natação", 12 * 60, days = setOf(DayOfWeek.WEDNESDAY))), wed))
    }

    @Test
    fun scheduled_picksTheNextReminderRoutine() {
        val routines = listOf(routine(1), routine(2), routine(3))
        val reminders = listOf(
            weekly(1, 1, 18 * 60, DayOfWeek.FRIDAY),
            weekly(2, 2, 7 * 60, DayOfWeek.THURSDAY),
            weekly(3, 3, 18 * 60, DayOfWeek.SATURDAY),
        )
        val next = scheduledNext(routines, reminders, wed)!!
        assertEquals(2L, next.routine?.id)
        assertEquals(LocalDateTime.of(2026, 11, 19, 7, 0), next.at)
    }

    @Test
    fun scheduled_todaysReminderStaysAfterItsTime_untilThatRoutineIsDone() {
        val reminders = listOf(weekly(1, 1, 7 * 60, DayOfWeek.WEDNESDAY), weekly(2, 2, 18 * 60, DayOfWeek.THURSDAY))
        // 07:00 passed, routine 1 not done today: still today's workout.
        val pending = scheduledNext(listOf(routine(1, last = todayDay - 7), routine(2)), reminders, wed)!!
        assertEquals(1L, pending.routine?.id)
        assertEquals(LocalDateTime.of(2026, 11, 18, 7, 0), pending.at)
        // Done today: tomorrow's reminder is next.
        val done = scheduledNext(listOf(routine(1, last = todayDay), routine(2)), reminders, wed)!!
        assertEquals(2L, done.routine?.id)
        // Only reminder and done today: its next week's slot.
        val alone = scheduledNext(listOf(routine(1, last = todayDay)), reminders.take(1), wed)!!
        assertEquals(LocalDateTime.of(2026, 11, 25, 7, 0), alone.at)
    }

    @Test
    fun scheduled_missedWorkoutStaysUntilMidnight_orUntilAnotherRemindersTimeComes() {
        val routines = listOf(routine(1), routine(2), routine(9))
        val tomorrow = weekly(9, 9, 18 * 60, DayOfWeek.THURSDAY)
        // 07:00 missed 3 h ago: still on the card.
        assertEquals(
            LocalDateTime.of(2026, 11, 18, 7, 0),
            scheduledNext(routines, listOf(weekly(1, 1, 7 * 60, DayOfWeek.WEDNESDAY), tomorrow), wed)?.at,
        )
        // 06:00 missed 4 h ago: still on the card, and still at 23:59.
        val early = listOf(weekly(1, 1, 6 * 60, DayOfWeek.WEDNESDAY), tomorrow)
        assertEquals(LocalDateTime.of(2026, 11, 18, 6, 0), scheduledNext(routines, early, wed)?.at)
        val lateNight = ZonedDateTime.of(2026, 11, 18, 23, 59, 0, 0, wed.zone)
        assertEquals(LocalDateTime.of(2026, 11, 18, 6, 0), scheduledNext(routines, early, lateNight)?.at)
        // 07:00 missed, but another reminder's time (09:00) came: that one.
        val both = listOf(weekly(1, 1, 7 * 60, DayOfWeek.WEDNESDAY), weekly(2, 2, 9 * 60, DayOfWeek.WEDNESDAY), tomorrow)
        assertEquals(LocalDateTime.of(2026, 11, 18, 9, 0), scheduledNext(routines, both, wed)?.at)
        // That one done (the 07:00 skipped for it): the next, not the 07:00 again.
        assertEquals(9L, scheduledNext(listOf(routine(1), routine(2, last = todayDay), routine(9)), both, wed)?.routine?.id)
        // Disabled and free-text reminders' times don't move it.
        val others = listOf(
            weekly(1, 1, 7 * 60, DayOfWeek.WEDNESDAY),
            weekly(2, 2, 9 * 60, DayOfWeek.WEDNESDAY, enabled = false),
            weekly(3, null, 9 * 60, DayOfWeek.WEDNESDAY),
            tomorrow,
        )
        assertEquals(1L, scheduledNext(routines, others, wed)?.routine?.id)
        // Past midnight it's gone: at 00:30, last night's 22:00 gives way to tomorrow's; alone, its next week's slot.
        val night = ZonedDateTime.of(2026, 11, 18, 0, 30, 0, 0, wed.zone)
        val lastNight = weekly(1, 1, 22 * 60, DayOfWeek.TUESDAY)
        assertEquals(9L, scheduledNext(routines, listOf(lastNight, tomorrow), night)?.routine?.id)
        assertEquals(LocalDateTime.of(2026, 11, 24, 22, 0), scheduledNext(routines, listOf(lastNight), night)?.at)
    }

    @Test
    fun scheduled_routineDoneSinceItsPreviousSlot_skipsTheComingSlot() {
        val reminders = listOf(weekly(1, 1, 18 * 60, DayOfWeek.THURSDAY), weekly(2, 2, 7 * 60, DayOfWeek.FRIDAY))
        // Tomorrow's routine done ahead (today, or Monday after last Thursday's slot): Friday's comes first.
        for (last in listOf(todayDay, todayDay - 2)) {
            assertEquals(2L, scheduledNext(listOf(routine(1, last = last), routine(2)), reminders, wed)?.routine?.id)
        }
        // Done on its previous slot (last Thursday): tomorrow's still to do.
        assertEquals(1L, scheduledNext(listOf(routine(1, last = todayDay - 6), routine(2)), reminders, wed)?.routine?.id)
        // Only reminder and done ahead: the slot after tomorrow's.
        assertEquals(
            LocalDateTime.of(2026, 11, 26, 18, 0),
            scheduledNext(listOf(routine(1, last = todayDay)), reminders.take(1), wed)?.at,
        )
        // Several days: Monday/Thursday, done Tuesday counts for Thursday; done Monday was Monday's.
        val twice = listOf(weekly(1, 1, 18 * 60, DayOfWeek.MONDAY, DayOfWeek.THURSDAY))
        assertEquals(
            LocalDateTime.of(2026, 11, 23, 18, 0),
            scheduledNext(listOf(routine(1, last = todayDay - 1)), twice, wed)?.at,
        )
        assertEquals(
            LocalDateTime.of(2026, 11, 19, 18, 0),
            scheduledNext(listOf(routine(1, last = todayDay - 2)), twice, wed)?.at,
        )
    }

    @Test
    fun scheduled_oneOffReminders() {
        fun once(id: Long, routineId: Long, date: LocalDate, minute: Int) = Reminder(id, routineId, "R$routineId", minute, dateEpochDay = date.toEpochDay())
        val routines = listOf(routine(1), routine(2))
        assertEquals(
            LocalDateTime.of(2026, 11, 20, 9, 0),
            scheduledNext(routines, listOf(once(1, 1, LocalDate.of(2026, 11, 20), 9 * 60)), wed)?.at,
        )
        // Yesterday's one-off is gone; earlier today counts until midnight.
        assertNull(scheduledNext(routines, listOf(once(1, 1, LocalDate.of(2026, 11, 17), 23 * 60)), wed))
        assertEquals(1L, scheduledNext(routines, listOf(once(1, 1, LocalDate.of(2026, 11, 18), 7 * 60)), wed)?.routine?.id)
        assertEquals(1L, scheduledNext(routines, listOf(once(1, 1, LocalDate.of(2026, 11, 18), 0)), wed)?.routine?.id)
        // Friday's one-off done ahead today is gone; done before today it still counts.
        val friday = listOf(once(1, 1, LocalDate.of(2026, 11, 20), 9 * 60))
        assertNull(scheduledNext(listOf(routine(1, last = todayDay)), friday, wed))
        assertEquals(1L, scheduledNext(listOf(routine(1, last = todayDay - 1)), friday, wed)?.routine?.id)
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
        assertEquals(5L, scheduledNext(routines, reminders, wed)?.routine?.id)
        assertNull(scheduledNext(routines, reminders.take(4), wed))
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

    @Test
    fun state_nextActivityReminder_showsTheSportAndItsLastDay() = runBlocking {
        val exercise = env.db.insertExercise("Remada")
        env.db.insertRoutine("A", listOf(exercise), position = 0)
        val yesterday = env.time.today().minusDays(1)
        ActivityRepository(env.db, env.time, BackupTrigger { }).save(Activity(sport = Sport.SWIMMING, date = yesterday, durationMinutes = 40))
        val settings = testSettings()
        settings.update { it.copy(reminders = listOf(Reminder(1, null, "Natação", 23 * 60 + 59, days = DayOfWeek.entries.toSet(), sport = Sport.SWIMMING))) }

        val state = homeViewModel(settings).state.awaitUntil { !it.loading && it.suggestedActivity?.lastDoneEpochDay != null }
        assertEquals(ActivityTarget(Sport.SWIMMING, null, yesterday.toEpochDay()), state.suggestedActivity)
        assertNull(state.suggested)
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
            NutritionTracker(
                NutritionRepository(env.db, env.time, BackupTrigger { }),
                BodyRepository(env.db.bodyDao(), env.time, BackupTrigger { }),
                HistoryRepository(env.db, env.time, BackupTrigger { }),
                activities,
                settings,
            ),
            settings,
            env.time,
        )
    }

    @Test
    fun state_nutritionToday_showsTheLogAndTheTargets() = runBlocking {
        val today = env.time.today()
        BodyRepository(env.db.bodyDao(), env.time, BackupTrigger { })
            .save(BodyAssessment(date = today.minusDays(3), values = mapOf(Measure.WEIGHT to 80.0, Measure.HEIGHT to 178.0)))
        val settings = testSettings().apply { update { it.copy(sex = Sex.MALE, birthEpochDay = today.minusYears(30).toEpochDay()) } }
        val nutrition = NutritionRepository(env.db, env.time, BackupTrigger { })
        nutrition.addWater(today, 500)
        nutrition.saveMeal(Meal(date = today, minuteOfDay = 480, description = null, kcal = 320))

        val state = homeViewModel(settings).state.awaitUntil { !it.loading && it.nutrition?.targets?.kcal != null && it.nutrition.day.kcal > 0 }
        val n = state.nutrition!!
        assertEquals(500, n.day.waterMl)
        assertEquals(320, n.day.kcal)
        assertEquals(2120, n.targets.kcal) // 80 kg, 178 cm, 30 years, no workouts
        assertEquals(2800, n.targets.waterMl)
    }

    @Test
    fun week_startsOnTheDayChosenInSettings() = runBlocking {
        val today = env.time.today()
        val settings = testSettings().apply { update { it.copy(firstDayOfWeek = DayOfWeek.SUNDAY) } }
        val vm = homeViewModel(settings)
        val sunday = vm.state.awaitUntil { !it.loading && it.week.days.firstOrNull()?.date?.dayOfWeek == DayOfWeek.SUNDAY }
        assertEquals(today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)), sunday.week.days.first().date)
        assertEquals(DayOfWeek.SATURDAY, sunday.week.days.last().date.dayOfWeek)

        settings.update { it.copy(firstDayOfWeek = DayOfWeek.MONDAY) }
        val monday = vm.state.awaitUntil { it.week.days.firstOrNull()?.date?.dayOfWeek == DayOfWeek.MONDAY }
        assertEquals(today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), monday.week.days.first().date)
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

    @Test
    fun nutritionToday_followsTheDateAfterMidnight_onResume() = runBlocking {
        val today = env.time.today()
        NutritionRepository(env.db, env.time, BackupTrigger { }).addWater(today, 500)
        val vm = homeViewModel()
        vm.state.awaitUntil { it.nutrition?.day?.waterMl == 500 }

        env.time.advance(24 * 60 * 60 * 1000L) // Home stayed alive overnight
        vm.refreshToday()
        val next = vm.state.awaitUntil { it.today == today.plusDays(1) && it.nutrition?.day?.date == today.plusDays(1) }
        assertEquals(0, next.nutrition!!.day.waterMl)
    }

    @Test
    fun today_listsLatestFirst_withTodaysActivities() = runBlocking {
        val first = env.startWorkout(1)
        env.repo.finish(SessionEngine.completeSet(env.repo.getActive()!!, 12, 40.0, env.time.now()), env.time.now(), updateRoutineLoads = false)
        env.time.advance(3 * 60 * 60 * 1000L)
        val second = env.startWorkout(1)
        env.repo.finish(SessionEngine.completeSet(env.repo.getActive()!!, 12, 40.0, env.time.now()), env.time.now(), updateRoutineLoads = false)
        val activities = ActivityRepository(env.db, env.time, BackupTrigger { })
        activities.save(Activity(sport = Sport.SWIMMING, date = env.time.today(), durationMinutes = 45))
        activities.save(Activity(sport = Sport.SOCCER, date = env.time.today().minusDays(1), durationMinutes = 60)) // not today

        val state = homeViewModel().state.awaitUntil { !it.loading && it.todaySessions.size == 2 && it.todayActivities.isNotEmpty() }
        assertEquals(listOf(second, first), state.todaySessions.map { it.id }) // latest first: the card used to show the first one
        assertEquals(listOf(Sport.SWIMMING), state.todayActivities.map { it.sport })
    }

    @Test
    fun week_marksTheDaysOnTargetForFoodAndWater() = runBlocking {
        val today = env.time.today()
        BodyRepository(env.db.bodyDao(), env.time, BackupTrigger { })
            .save(BodyAssessment(date = today.minusDays(3), values = mapOf(Measure.WEIGHT to 80.0, Measure.HEIGHT to 178.0)))
        val settings = testSettings().apply { update { it.copy(sex = Sex.MALE, birthEpochDay = today.minusYears(30).toEpochDay()) } }
        val nutrition = NutritionRepository(env.db, env.time, BackupTrigger { })
        nutrition.addWater(today, 3000) // 2.8 L target
        nutrition.saveMeal(Meal(date = today, minuteOfDay = 600, description = null, kcal = 2100)) // 2 120 target, maintain ±10 %

        val state = homeViewModel(settings).state.awaitUntil { s -> !s.loading && s.week.days.any { it.drankOnTarget } }
        val day = state.week.days.single { it.isToday }
        assertTrue(day.ateOnTarget)
        assertTrue(day.drankOnTarget)
        assertEquals(1, state.week.days.count { it.ateOnTarget })
    }
}
