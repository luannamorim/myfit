package io.github.luannamorim.myfit.ui.feature.workout

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.domain.model.ItemStatus
import io.github.luannamorim.myfit.domain.model.ThemeMode
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Done moves the cursor to the next set and starts the rest in one write. While the set panel fades out,
 * it must keep showing the set that was just done, not the next set's planned values (user report).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = "pt-rBR-w411dp-h891dp-xxhdpi")
class WorkoutPhaseTransitionTest {
    @get:Rule val compose = createComposeRule()

    private val doing = WorkoutUiState(
        loading = false,
        hasSession = true,
        sessionId = 1,
        routineName = "Costas",
        exerciseName = "Remada curvada",
        setNumber = 2,
        setCount = 3,
        plannedReps = 12,
        reps = 7, // lowered with the stepper
        exercises = listOf(ExerciseChip("Remada curvada", ItemStatus.PENDING, true, 1, 3)),
        restOver = true,
    )

    /** What Room emits right after Done: the cursor is on set 3 (planned 12) and the rest is running. */
    private val resting = doing.copy(
        setNumber = 3,
        reps = 12,
        restOver = false,
        rest = RestUi(remainingMs = 59_000, progress = 0.98f, nextName = "Remada curvada", nextSet = 3, nextSetCount = 3),
    )

    @Test
    fun outgoing_set_panel_keeps_the_done_value_while_fading_out() {
        var state by mutableStateOf(doing)
        compose.setContent {
            MyFitTheme(ThemeMode.DARK) {
                WorkoutContent(state, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
            }
        }
        compose.waitForIdle()
        assertEquals(1, compose.onAllNodesWithText("7").fetchSemanticsNodes().size)

        compose.mainClock.autoAdvance = false
        state = resting
        Snapshot.sendApplyNotifications()
        repeat(4) { compose.mainClock.advanceTimeByFrame() }

        // Mid fade: the leaving panel still says 7; the planned 12 of the next set never shows.
        assertEquals(1, compose.onAllNodesWithText("7").fetchSemanticsNodes().size)
        assertEquals(0, compose.onAllNodesWithText("12").fetchSemanticsNodes().size)
    }

    @Test
    fun a_panel_still_follows_changes_within_its_phase() {
        var state by mutableStateOf(doing)
        compose.setContent {
            MyFitTheme(ThemeMode.DARK) {
                WorkoutContent(state, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})
            }
        }
        state = doing.copy(reps = 8)
        compose.waitForIdle()
        assertEquals(1, compose.onAllNodesWithText("8").fetchSemanticsNodes().size)

        state = resting
        compose.waitForIdle()
        state = resting.copy(rest = resting.rest!!.copy(remainingMs = 41_000))
        compose.waitForIdle()
        assertEquals(1, compose.onAllNodesWithText("0:41").fetchSemanticsNodes().size)
    }
}
