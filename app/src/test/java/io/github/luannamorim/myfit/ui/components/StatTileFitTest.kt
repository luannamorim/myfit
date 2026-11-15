package io.github.luannamorim.myfit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.luannamorim.myfit.domain.model.ThemeMode
import io.github.luannamorim.myfit.testutil.SCREENSHOT_DIR
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import io.github.luannamorim.myfit.ui.theme.Spacing
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * History's summary on a small phone with a bigger system font: "12 h 30 min" shrinks to one line instead of
 * wrapping and making its tile taller than the others.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "pt-rBR-w360dp-h800dp-xxhdpi")
class StatTileFitTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun longValues_stayOnOneLine_andTilesKeepOneHeight() {
        compose.setContent {
            // Bigger system font, as on the user's phone, where the value wrapped.
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.3f)) {
                MyFitTheme(ThemeMode.LIGHT) {
                    Column(
                        Modifier.background(MyFitTheme.colors.background).padding(Spacing.screen),
                        verticalArrangement = Arrangement.spacedBy(Spacing.m),
                    ) {
                        // Same grid as History's summary.
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                            StatTile("Sessões", "3", Modifier.weight(1f).testTag("sessions"))
                            StatTile("Atividades", "2", Modifier.weight(1f).testTag("activities"))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                            StatTile("Tempo total", "12 h 30 min", Modifier.weight(1f).testTag("total"))
                            StatTile("Frequência", "2,5×/sem", Modifier.weight(1f).testTag("frequency"))
                        }
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage("$SCREENSHOT_DIR/stat_tile_long_value_pt_360.png")
        compose.onNodeWithText("12 h 30 min").assertExists()
        val heights = listOf("sessions", "activities", "total", "frequency").associateWith {
            compose.onNodeWithTag(it).getBoundsInRoot().let { b -> b.bottom - b.top }
        }
        // Before: "Tempo total" and "Frequência" wrapped to two lines, 136 dp tall against 99 dp.
        heights.forEach { (tag, h) -> assertEquals("$tag height", heights.getValue("sessions").value, h.value, 0.5f) }
    }
}
