package io.github.luannamorim.myfit.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.domain.model.ItemStatus
import io.github.luannamorim.myfit.domain.session.SessionEngine
import io.github.luannamorim.myfit.testutil.SessionEnv
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class HistoryRepositoryTest {
    private val env = SessionEnv()
    private val history = HistoryRepository(env.db, env.time, BackupTrigger { })

    @After fun tearDown() = env.close()

    private fun today(): LocalDate = LocalDate.ofInstant(java.time.Instant.ofEpochMilli(env.time.now().wallMs), env.time.zone())

    private suspend fun finishedWorkout(): Long {
        env.startWorkout(2, 1)
        var s = env.repo.getActive()!!
        s = SessionEngine.completeSet(s, 12, 40.0, env.time.now())
        s = SessionEngine.skipSet(s, env.time.now())
        env.time.advance(1_800_000)
        return env.repo.finish(s, env.time.now(), updateRoutineLoads = false)
    }

    @Test
    fun summaries_excludeActive() = runBlocking {
        env.startWorkout(2)
        assertTrue(history.observeSummaries(today()..today()).first().isEmpty())
    }

    @Test
    fun summaries_filteredByRange() = runBlocking {
        finishedWorkout()
        assertEquals(1, history.observeSummaries(today()..today()).first().size)
        assertTrue(history.observeSummaries(today().plusDays(1)..today().plusDays(7)).first().isEmpty())
        val summary = history.observeSummaries(today()..today()).first().single()
        assertEquals(1, summary.setsDone)
        assertEquals(listOf("Exercício 1"), summary.exercises)
        assertEquals(1_800_000L, summary.durationMs)
    }

    @Test
    fun detail_returnsSetsInOrder() = runBlocking {
        val id = finishedWorkout()
        val detail = history.observeDetail(id).first()!!
        assertEquals(listOf("Exercício 1", "Exercício 2"), detail.exercises.map { it.name })
        assertEquals(listOf(ItemStatus.DONE, ItemStatus.SKIPPED), detail.exercises[0].sets.map { it.status })
        history.deleteSession(id)
        assertEquals(null, history.observeDetail(id).first())
    }
}
