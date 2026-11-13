package io.github.luannamorim.myfit.ui.feature.workout

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.domain.model.ExerciseMeasure
import io.github.luannamorim.myfit.domain.model.MuscleGroup
import io.github.luannamorim.myfit.testutil.FakeSounds
import io.github.luannamorim.myfit.testutil.MainDispatcherRule
import io.github.luannamorim.myfit.testutil.SessionEnv
import io.github.luannamorim.myfit.testutil.awaitUntil
import io.github.luannamorim.myfit.testutil.insertExercise
import io.github.luannamorim.myfit.testutil.insertRoutine
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class WorkoutViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val env = SessionEnv()

    @After fun tearDown() = env.close()

    private val sounds = FakeSounds()
    private fun vm() = WorkoutViewModel(env.controller, env.time, env.settings, sounds)

    @Test
    fun timedSet_stepsBySeconds_andHoldRecordsTheTimeActuallyHeld() = runBlocking {
        val plank = env.db.insertExercise("Prancha", MuscleGroup.ABS, measure = ExerciseMeasure.TIME)
        val routine = env.db.insertRoutine("Core", listOf(plank)) // 3 × "12" → 12 s
        env.controller.start(routine)
        val vm = vm()
        val start = vm.state.awaitUntil { it.hasSession }
        assertTrue(start.timed)
        assertEquals(12, start.reps)
        vm.changeReps(1)
        assertEquals(15, vm.state.awaitUntil { it.reps == 15 }.reps) // 5 s steps under a minute
        vm.startHold()
        vm.state.awaitUntil { it.hold != null }
        env.time.advance(18_400) // held past the 15 s target
        vm.tick()
        assertTrue(vm.state.awaitUntil { it.hold?.reachedTarget == true }.hold!!.reachedTarget)
        vm.finishHold()
        vm.state.awaitUntil { it.hold == null && it.setNumber == 2 }
        val done = env.repo.getActive()!!.exercises.single().sets.first()
        assertEquals(18, done.actualReps) // seconds held
    }

    @Test
    fun restCountdown_derivesFromMarks_notTicks() = runBlocking {
        env.startWorkout(3)
        val vm = vm()
        vm.state.awaitUntil { it.hasSession }
        vm.completeSet()
        vm.state.awaitUntil { it.rest != null }
        env.time.advance(30_000)
        vm.tick()
        val state = vm.state.awaitUntil { it.rest?.remainingMs == 60_000L }
        assertEquals(30_000L, state.elapsedMs)
        env.time.advance(60_000)
        vm.tick()
        val over = vm.state.awaitUntil { it.rest == null }
        assertTrue(over.restOver)
        assertEquals(2, over.setNumber)
    }

    @Test
    fun steppers_changeValuesUsedToCompleteSet() = runBlocking {
        env.startWorkout(2)
        val vm = vm()
        vm.state.awaitUntil { it.hasSession }
        vm.changeReps(-2)
        vm.changeLoad(2.5)
        assertEquals(10, vm.state.value.reps)
        assertEquals(42.5, vm.state.value.loadKg!!, 0.0)
        vm.completeSet()
        vm.state.awaitUntil { it.setNumber == 2 }
        val done = env.repo.getActive()!!.exercises[0].sets[0]
        assertEquals(10, done.actualReps)
        assertEquals(42.5, done.actualLoadKg!!, 0.0)
        // Load carried forward to the next set, reps reset to plan.
        assertEquals(12, vm.state.value.reps)
        assertEquals(42.5, vm.state.value.loadKg!!, 0.0)
    }

    @Test
    fun finishWithoutCompletedSets_emitsDiscardPrompt() = runBlocking {
        env.startWorkout(2)
        val vm = vm()
        vm.state.awaitUntil { it.hasSession }
        assertFalse(vm.requestFinish())
        assertTrue(vm.state.awaitUntil { it.showDiscardPrompt }.showDiscardPrompt)
        vm.discard()
        vm.state.awaitUntil { !it.hasSession && !it.loading }
        Unit
    }

    @Test
    fun pause_freezesElapsed() = runBlocking {
        env.startWorkout(2)
        val vm = vm()
        vm.state.awaitUntil { it.hasSession }
        env.time.advance(10_000)
        vm.togglePause()
        vm.state.awaitUntil { it.isPaused }
        env.time.advance(50_000)
        vm.tick()
        assertEquals(10_000L, vm.state.awaitUntil { it.isPaused }.elapsedMs)
    }
}
