package io.github.luannamorim.myfit.ui.feature.activity

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.domain.activity.Sport
import io.github.luannamorim.myfit.domain.model.Intensity
import io.github.luannamorim.myfit.platform.activity.GpsStatus
import io.github.luannamorim.myfit.testutil.PHONE_TALL
import io.github.luannamorim.myfit.testutil.snapBothThemes
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = PHONE_TALL)
class ActivityScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private val today = LocalDate.of(2026, 11, 12)

    @Test
    fun activity_swimming() = compose.snapBothThemes("activity_swimming") {
        ActivityEditorContent(
            ActivityEditorUiState(
                loading = false, sport = Sport.SWIMMING, date = today, today = today, duration = "45", distance = "1500", intensity = Intensity.MEDIUM,
            ),
            ActivityEditorActions(),
        )
    }

    @Test
    fun activity_indoor_cycling() = compose.snapBothThemes("activity_indoor_cycling") {
        ActivityEditorContent(
            ActivityEditorUiState(loading = false, isNew = false, sport = Sport.INDOOR_CYCLING, date = today, today = today, duration = "40", distance = "18,5"),
            ActivityEditorActions(),
        )
    }

    @Test
    fun activity_review() = compose.snapBothThemes("activity_review") {
        ActivityEditorContent(
            ActivityEditorUiState(
                loading = false, isNew = false, review = true, sport = Sport.RUNNING, date = today, today = today,
                duration = "28", distance = "5,23", intensity = Intensity.HIGH,
            ),
            ActivityEditorActions(),
        )
    }

    @Test
    fun activity_free() = compose.snapBothThemes("activity_free") {
        ActivityEditorContent(
            ActivityEditorUiState(loading = false, sport = Sport.OTHER, date = today, today = today, nameError = true, durationError = true),
            ActivityEditorActions(),
        )
    }

    @Test
    fun activity_new_running() = compose.snapBothThemes("activity_new_running") {
        ActivityEditorContent(ActivityEditorUiState(loading = false, sport = Sport.RUNNING, date = today, today = today), ActivityEditorActions())
    }

    @Test
    fun live_running() = compose.snapBothThemes("live_running") {
        LiveActivityContent(
            LiveActivityUiState(
                loading = false, running = true, sport = Sport.RUNNING, elapsedMs = 27 * 60_000L + 41_000, gps = true,
                gpsStatus = GpsStatus.OK, distanceMeters = 5_230.0,
            ),
            {}, {}, {}, {},
        )
    }

    @Test
    fun live_paused_noGps() = compose.snapBothThemes("live_paused") {
        LiveActivityContent(
            LiveActivityUiState(loading = false, running = true, sport = Sport.INDOOR_CYCLING, elapsedMs = 12 * 60_000L, paused = true),
            {}, {}, {}, {},
        )
    }
}
