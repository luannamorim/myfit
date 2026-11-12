package io.github.luannamorim.myfit.ui.feature.history

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.core.time.today
import io.github.luannamorim.myfit.data.repository.ActivityRepository
import io.github.luannamorim.myfit.data.repository.BackupTrigger
import io.github.luannamorim.myfit.data.repository.HistoryRepository
import io.github.luannamorim.myfit.domain.activity.Activity
import io.github.luannamorim.myfit.domain.activity.Sport
import io.github.luannamorim.myfit.domain.history.HistoryCalculator
import io.github.luannamorim.myfit.domain.model.Intensity
import io.github.luannamorim.myfit.domain.session.SessionEngine
import io.github.luannamorim.myfit.testutil.MainDispatcherRule
import io.github.luannamorim.myfit.testutil.SessionEnv
import io.github.luannamorim.myfit.testutil.awaitUntil
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.YearMonth

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class HistoryViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val env = SessionEnv()
    private val history = HistoryRepository(env.db, env.time, BackupTrigger { })
    private val activities = ActivityRepository(env.db, env.time, BackupTrigger { })

    @After fun tearDown() = env.close()

    /** Finishes a 1-set workout of [minutes] on the current fake day. */
    private suspend fun workout(minutes: Long) {
        env.startWorkout(1)
        val s = SessionEngine.completeSet(env.repo.getActive()!!, 12, 40.0, env.time.now())
        env.time.advance(minutes * 60_000)
        env.repo.finish(s, env.time.now(), updateRoutineLoads = false)
    }

    @Test
    fun selectingDay_listsThatDaysSessions_andMarksDays() = runBlocking {
        val day1 = env.time.today()
        workout(50)
        env.time.advance(2 * 86_400_000L)
        val day3 = env.time.today()
        workout(40)

        val vm = HistoryViewModel(history, activities, env.time)
        vm.select(day1)
        val state = vm.state.awaitUntil { !it.isLoading && it.selected == day1 && it.daySessions.isNotEmpty() }
        assertEquals(1, state.daySessions.size)
        assertTrue(day1 in state.marked && day3 in state.marked)

        vm.select(day1.plusDays(1))
        assertTrue(vm.state.awaitUntil { it.selected == day1.plusDays(1) }.daySessions.isEmpty())
    }

    @Test
    fun toggleMonth_switchesSummaryRange() = runBlocking {
        workout(30)
        val vm = HistoryViewModel(history, activities, env.time)
        val week = vm.state.awaitUntil { !it.isLoading && it.summary.sessions == 1 }
        assertEquals(HistoryCalculator.weekOf(week.selected), week.summaryRange)
        vm.setPeriod(SummaryPeriod.MONTH)
        val month = vm.state.awaitUntil { it.period == SummaryPeriod.MONTH }
        assertEquals(HistoryCalculator.monthOf(YearMonth.from(week.selected)), month.summaryRange)
        assertEquals(1, month.summary.sessions)
        assertEquals(30 * 60_000L, month.summary.totalDurationMs)
    }

    @Test
    fun activities_showOnTheirDay_markTheCalendar_andCountInTheSummary() = runBlocking {
        val today = env.time.today() // Tue 2024-11-12: yesterday is in the same week and month
        workout(30)
        activities.save(Activity(sport = Sport.SWIMMING, date = today, durationMinutes = 45, distanceKm = 1.5, intensity = Intensity.HIGH))
        activities.save(Activity(sport = Sport.OTHER, name = "Remo no lago", date = today.minusDays(1), durationMinutes = 20))

        val vm = HistoryViewModel(history, activities, env.time)
        val state = vm.state.awaitUntil { !it.isLoading && it.dayActivities.isNotEmpty() }
        assertEquals(listOf(Sport.SWIMMING), state.dayActivities.map { it.sport })
        assertEquals(1, state.daySessions.size)
        assertTrue(today.minusDays(1) in state.marked) // a day with only an activity is marked too

        val week = state.summary
        assertEquals(1, week.sessions)
        assertEquals(2, week.activities)
        assertEquals((30 + 45 + 20) * 60_000L, week.totalDurationMs)
        assertEquals(listOf(Sport.SWIMMING to null, Sport.OTHER to "Remo no lago"), week.sports.map { it.sport to it.name }.sortedBy { it.first.ordinal })
        assertEquals(2, vm.state.value.monthActivities.size)
    }

    @Test
    fun showMonth_movesSelectionIntoMonth() = runBlocking {
        val vm = HistoryViewModel(history, activities, env.time)
        val start = vm.state.awaitUntil { !it.isLoading }
        val next = start.month.plusMonths(1)
        vm.showMonth(next)
        val state = vm.state.awaitUntil { it.month == next }
        assertEquals(next.atDay(1), state.selected)
    }
}
