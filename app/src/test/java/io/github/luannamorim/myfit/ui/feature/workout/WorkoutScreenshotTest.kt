package io.github.luannamorim.myfit.ui.feature.workout

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.luannamorim.myfit.domain.model.Intensity
import io.github.luannamorim.myfit.domain.model.ItemStatus
import io.github.luannamorim.myfit.domain.model.MuscleGroup
import io.github.luannamorim.myfit.domain.model.ThemeMode
import io.github.luannamorim.myfit.testutil.PHONE
import io.github.luannamorim.myfit.testutil.SCREENSHOT_DIR
import io.github.luannamorim.myfit.testutil.snapBothThemes
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = PHONE)
class WorkoutScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private val base = WorkoutUiState(
        loading = false,
        hasSession = true,
        sessionId = 1,
        routineName = "Back day",
        elapsedMs = 23 * 60_000L + 14_000,
        exerciseIndex = 1,
        exerciseName = "One-arm dumbbell row",
        exerciseNotes = "Keep the elbow close to the body",
        seedKey = "one-arm-dumbbell-row",
        muscleGroup = MuscleGroup.BACK,
        setNumber = 2,
        setCount = 3,
        plannedReps = 10,
        plannedLoadKg = 22.5,
        reps = 10,
        loadKg = 22.5,
        exerciseRestSeconds = 60,
        exercises = listOf(
            ExerciseChip("Bent-over row", ItemStatus.DONE, false, 3, 3),
            ExerciseChip("One-arm dumbbell row", ItemStatus.PENDING, true, 1, 3),
            ExerciseChip("Pullover", ItemStatus.PENDING, false, 0, 3),
            ExerciseChip("Lat pulldown", ItemStatus.SKIPPED, false, 0, 3),
        ),
    )

    private fun snap(name: String, state: WorkoutUiState) {
        compose.setContent {
            MyFitTheme(ThemeMode.DARK) { Content(state) }
        }
        compose.onRoot().captureRoboImage("$SCREENSHOT_DIR/$name.png")
    }

    @Composable
    private fun Content(state: WorkoutUiState) = WorkoutContent(
        state, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
    )

    @Test fun workout_set() = snap("workout_set", base.copy(restOver = true))

    @Test fun workout_set_custom_exercise() = snap("workout_set_custom", base.copy(exerciseName = "Landmine row", seedKey = null))

    @Test fun workout_rest() = snap("workout_rest", base.copy(rest = RestUi(52_000, 0.58f, "One-arm dumbbell row", 2, 3)))

    @Test fun workout_timed_set() = snap("workout_timed_set", base.copy(exerciseName = "Plank", seedKey = "plank", muscleGroup = MuscleGroup.ABS, timed = true, reps = 45, loadKg = null))

    @Test fun workout_hold() = snap(
        "workout_hold",
        base.copy(exerciseName = "Plank", seedKey = "plank", muscleGroup = MuscleGroup.ABS, timed = true, reps = 45, loadKg = null, hold = HoldUi(32_000, 45_000)),
    )

    @Test fun workout_paused() = snap("workout_paused", base.copy(isPaused = true))

    @Test fun workout_done() = snap("workout_done", base.copy(allDone = true))

    @Test
    fun summary() = compose.snapBothThemes("session_summary") {
        SessionSummaryContent(
            SessionSummaryUiState(
                loading = false,
                hasSession = true,
                routineName = "Back day",
                durationMs = 52 * 60_000L + 10_000,
                setsDone = 11,
                setsTotal = 12,
                volumeKg = 4_870.0,
                exercises = listOf(
                    SummaryExercise("Bent-over row", 3, 3, false, 42.5),
                    SummaryExercise("One-arm dumbbell row", 3, 3, false, 22.5),
                    SummaryExercise("Pullover", 3, 3, false, 20.0),
                    SummaryExercise("Lat pulldown", 2, 3, false, 50.0),
                ),
                updateLoads = true,
                intensity = Intensity.HIGH,
            ),
            {}, {}, {},
        )
    }
}
