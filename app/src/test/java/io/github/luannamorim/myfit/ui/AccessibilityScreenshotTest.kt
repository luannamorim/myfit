package io.github.luannamorim.myfit.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.luannamorim.myfit.domain.history.HistoryCalculator
import io.github.luannamorim.myfit.domain.model.ItemStatus
import io.github.luannamorim.myfit.domain.model.RoutineOverview
import io.github.luannamorim.myfit.testutil.SCREENSHOT_DIR
import io.github.luannamorim.myfit.ui.feature.home.DayMark
import io.github.luannamorim.myfit.ui.feature.home.Greeting
import io.github.luannamorim.myfit.ui.feature.home.HomeActions
import io.github.luannamorim.myfit.ui.feature.home.HomeContent
import io.github.luannamorim.myfit.ui.feature.home.HomeUiState
import io.github.luannamorim.myfit.ui.feature.home.WeekStrip
import io.github.luannamorim.myfit.ui.feature.workout.ExerciseChip
import io.github.luannamorim.myfit.ui.feature.workout.RestUi
import io.github.luannamorim.myfit.ui.feature.workout.WorkoutContent
import io.github.luannamorim.myfit.ui.feature.workout.WorkoutUiState
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/** Large font (1.5x) on a small phone, and tablet width: checks for clipping and layout. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class AccessibilityScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private val workout = WorkoutUiState(
        loading = false, hasSession = true, routineName = "Treino Costas", elapsedMs = 1_394_000, exerciseName = "Remada unilateral com halter",
        setNumber = 2, setCount = 3, reps = 10, loadKg = 22.5,
        exercises = listOf(ExerciseChip("Remada curvada", ItemStatus.DONE, false, 3, 3), ExerciseChip("Remada unilateral", ItemStatus.PENDING, true, 1, 3)),
    )

    @Test
    @Config(qualifiers = "w360dp-h740dp-xhdpi", fontScale = 1.5f)
    fun workout_largeFont_smallPhone() {
        compose.setContent { MyFitTheme { WorkoutContent(workout, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}) } }
        compose.onRoot().captureRoboImage("$SCREENSHOT_DIR/a11y_workout_font150.png")
    }

    @Test
    @Config(qualifiers = "w360dp-h740dp-xhdpi", fontScale = 1.5f)
    fun rest_largeFont_smallPhone() {
        compose.setContent { MyFitTheme { WorkoutContent(workout.copy(rest = RestUi(75_000, 0.8f, "Remada unilateral", 2, 3)), {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}) } }
        compose.onRoot().captureRoboImage("$SCREENSHOT_DIR/a11y_rest_font150.png")
    }

    @Test
    @Config(qualifiers = "w840dp-h1200dp-xhdpi")
    fun home_tablet() {
        val today = LocalDate.of(2026, 11, 12)
        val range = HistoryCalculator.weekOf(today)
        compose.setContent {
            MyFitTheme {
                HomeContent(
                    HomeUiState(
                        loading = false, greeting = Greeting.AFTERNOON, today = today,
                        suggested = RoutineOverview(1, "Pernas", null, 5, today.toEpochDay() - 5),
                        routines = listOf(RoutineOverview(1, "Pernas", null, 5, today.toEpochDay() - 5), RoutineOverview(2, "Costas", null, 6, today.toEpochDay() - 2)),
                        week = WeekStrip((0L..6L).map { range.start.plusDays(it) }.map { DayMark(it, it.dayOfMonth % 2 == 0, it == today) }, 3, 9_000_000),
                    ),
                    HomeActions(),
                )
            }
        }
        compose.onRoot().captureRoboImage("$SCREENSHOT_DIR/a11y_home_tablet.png")
    }
}
