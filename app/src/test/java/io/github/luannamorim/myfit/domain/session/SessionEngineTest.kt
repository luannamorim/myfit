package io.github.luannamorim.myfit.domain.session

import io.github.luannamorim.myfit.core.time.FakeTimeSource
import io.github.luannamorim.myfit.domain.model.ItemStatus
import io.github.luannamorim.myfit.domain.session.SessionEngine as E
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionEngineTest {
    private val time = FakeTimeSource()
    private val now get() = time.now()

    @Test
    fun completeSet_startsRestWithExerciseRest_andCarriesLoadForward() {
        val s = E.completeSet(sessionOf(now, 3, 3), reps = 10, loadKg = 42.5, now = now)
        val set = s.exercises[0].sets[0]
        assertEquals(ItemStatus.DONE, set.status)
        assertEquals(10, set.actualReps)
        assertEquals(42.5, set.actualLoadKg!!, 0.0)
        assertEquals(Cursor(0, 1), s.cursor)
        assertEquals(90_000L, E.restRemainingMs(s, now))
        assertEquals(42.5, s.exercises[0].sets[1].plannedLoadKg!!, 0.0)
        assertEquals(40.0, s.exercises[1].sets[0].plannedLoadKg!!, 0.0)
    }

    @Test
    fun completeLastSetOfExercise_movesToNextExercise_andRests() {
        var s = sessionOf(now, 2, 2)
        s = E.completeSet(s, 12, 40.0, now)
        s = E.completeSet(s, 12, 40.0, now)
        assertEquals(Cursor(1, 0), s.cursor)
        assertEquals(ItemStatus.DONE, s.exercises[0].status)
        assertEquals(90_000L, E.restRemainingMs(s, now))
    }

    @Test
    fun completeFinalSet_noRest_sessionComplete() {
        var s = sessionOf(now, 1, 1)
        s = E.completeSet(s, 12, 40.0, now)
        s = E.completeSet(s, 12, 40.0, now)
        assertNull(s.rest)
        assertTrue(E.isComplete(s))
    }

    @Test
    fun skipSet_noRest() {
        val s = E.skipSet(sessionOf(now, 3), now)
        assertEquals(ItemStatus.SKIPPED, s.exercises[0].sets[0].status)
        assertEquals(Cursor(0, 1), s.cursor)
        assertNull(s.rest)
    }

    @Test
    fun skipExercise_marksPendingSkipped_andAdvances() {
        var s = sessionOf(now, 3, 2)
        s = E.completeSet(s, 12, 40.0, now)
        s = E.skipExercise(s, now)
        assertEquals(listOf(ItemStatus.DONE, ItemStatus.SKIPPED, ItemStatus.SKIPPED), s.exercises[0].sets.map { it.status })
        assertEquals(ItemStatus.DONE, s.exercises[0].status)
        assertEquals(Cursor(1, 0), s.cursor)
        assertNull(s.rest)
    }

    @Test
    fun goToSkippedExercise_reopensSkippedSets() {
        var s = sessionOf(now, 2, 2)
        s = E.skipExercise(s, now)
        assertEquals(ItemStatus.SKIPPED, s.exercises[0].status)
        s = E.goToExercise(s, 0)
        assertEquals(Cursor(0, 0), s.cursor)
        assertEquals(listOf(ItemStatus.PENDING, ItemStatus.PENDING), s.exercises[0].sets.map { it.status })
        assertEquals(ItemStatus.PENDING, s.exercises[0].status)
    }

    @Test
    fun afterLastExercise_wrapsBackToSkippedEarlierSets() {
        var s = sessionOf(now, 2, 1)
        s = E.skipSet(s, now) // ex0 set0 skipped → cursor (0,1)
        s = E.completeSet(s, 12, 40.0, now) // ex0 set1 → cursor (1,0)
        s = E.completeSet(s, 12, 40.0, now) // ex1 done, nothing pending
        assertTrue(E.isComplete(s))
        s = E.goToExercise(s, 0)
        assertEquals(Cursor(0, 0), s.cursor)
        assertFalse(E.isComplete(s))
    }

    @Test
    fun pauseResume_excludesPausedTimeFromElapsed() {
        var s = sessionOf(now, 3)
        time.advance(60_000)
        s = E.pause(s, now)
        time.advance(300_000)
        assertEquals(60_000L, E.elapsedMs(s, now))
        s = E.resume(s, now)
        time.advance(30_000)
        assertEquals(90_000L, E.elapsedMs(s, now))
        assertEquals(300_000L, s.totalPausedMs)
    }

    @Test
    fun pauseDuringRest_freezesRemaining() {
        var s = E.completeSet(sessionOf(now, 3), 12, 40.0, now)
        time.advance(30_000)
        s = E.pause(s, now)
        time.advance(120_000)
        assertEquals(60_000L, E.restRemainingMs(s, now))
        assertNull(E.restEndsAtWallMs(s, now))
        s = E.resume(s, now)
        time.advance(10_000)
        assertEquals(50_000L, E.restRemainingMs(s, now))
        assertEquals(now.wallMs + 50_000, E.restEndsAtWallMs(s, now))
    }

    @Test
    fun adjustRest_plus15_minusBelowZeroClamps() {
        var s = E.completeSet(sessionOf(now, 3), 12, 40.0, now)
        s = E.adjustRest(s, 15_000, now)
        assertEquals(105_000L, E.restRemainingMs(s, now))
        time.advance(100_000)
        s = E.adjustRest(s, -15_000, now)
        assertEquals(0L, E.restRemainingMs(s, now))
    }

    @Test
    fun skipRest_clearsRest() {
        val s = E.skipRest(E.completeSet(sessionOf(now, 3), 12, 40.0, now))
        assertNull(s.rest)
        assertNull(E.restRemainingMs(s, now))
    }

    @Test
    fun elapsed_wallClockJumpSameBoot_unaffected() {
        val s = sessionOf(now, 3)
        time.advance(120_000)
        time.jumpWall(-3_600_000)
        assertEquals(120_000L, E.elapsedMs(s, now))
    }

    @Test
    fun elapsed_afterReboot_usesWall() {
        val s = sessionOf(now, 3)
        time.reboot(wallMs = s.startedAt.wallMs + 600_000)
        assertEquals(600_000L, E.elapsedMs(s, now))
    }

    @Test
    fun checkFinish_noDoneSets_returnsNothingCompleted() {
        var s = sessionOf(now, 2)
        assertEquals(FinishCheck.NothingCompleted, E.checkFinish(s))
        s = E.skipSet(s, now)
        assertEquals(FinishCheck.NothingCompleted, E.checkFinish(s))
        s = E.completeSet(s, 12, 40.0, now)
        assertEquals(FinishCheck.Ok, E.checkFinish(s))
    }

    @Test
    fun editSet_changesActualValues_forDoneAndPlannedForPending() {
        var s = E.completeSet(sessionOf(now, 3), 12, 40.0, now)
        s = E.editSet(s, 0, 0, reps = 8, loadKg = 45.0)
        s = E.editSet(s, 0, 2, reps = 6, loadKg = 50.0)
        assertEquals(8, s.exercises[0].sets[0].actualReps)
        assertEquals(45.0, s.exercises[0].sets[0].actualLoadKg!!, 0.0)
        assertEquals(6, s.exercises[0].sets[2].plannedReps)
        assertEquals(50.0, s.exercises[0].sets[2].plannedLoadKg!!, 0.0)
    }

    @Test
    fun completeSetWhilePaused_resumesFirst() {
        var s = sessionOf(now, 3)
        s = E.pause(s, now)
        time.advance(60_000)
        s = E.completeSet(s, 12, 40.0, now)
        assertFalse(s.isPaused)
        assertEquals(60_000L, s.totalPausedMs)
    }

    @Test
    fun zeroRest_noRestStarted() {
        val s = E.completeSet(sessionOf(now, 3, restSeconds = 0), 12, 40.0, now)
        assertNull(s.rest)
    }
}
