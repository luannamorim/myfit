package io.github.luannamorim.myfit.ui.feature.home

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.data.repository.BackupTrigger
import io.github.luannamorim.myfit.data.repository.BodyRepository
import io.github.luannamorim.myfit.data.repository.HistoryRepository
import io.github.luannamorim.myfit.data.repository.RoutineRepository
import io.github.luannamorim.myfit.domain.model.RoutineOverview
import io.github.luannamorim.myfit.domain.session.SessionEngine
import io.github.luannamorim.myfit.testutil.MainDispatcherRule
import io.github.luannamorim.myfit.testutil.SessionEnv
import io.github.luannamorim.myfit.testutil.awaitUntil
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class HomeViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val env = SessionEnv()

    @After fun tearDown() = env.close()

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

    @Test
    fun today_listsOnlyTodaysFinishedSessions() = runBlocking {
        env.startWorkout(1)
        val s = SessionEngine.completeSet(env.repo.getActive()!!, 12, 40.0, env.time.now())
        env.repo.finish(s, env.time.now(), updateRoutineLoads = false)
        env.startWorkout(1) // a second, still active session: not in "today"

        val vm = HomeViewModel(
            RoutineRepository(env.db, env.time, BackupTrigger { }),
            HistoryRepository(env.db, env.time, BackupTrigger { }),
            env.controller,
            BodyRepository(env.db.bodyDao(), env.time, BackupTrigger { }),
            env.time,
        )
        val state = vm.state.awaitUntil { !it.loading && it.todaySessions.isNotEmpty() && it.active != null }
        assertEquals(1, state.todaySessions.size)
        assertEquals(1, state.week.sessions)
        assertEquals(1, state.week.days.count { it.trained })
        assertEquals(1, state.week.days.count { it.isToday })
    }
}
