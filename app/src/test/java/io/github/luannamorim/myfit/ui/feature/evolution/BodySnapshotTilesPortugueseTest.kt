package io.github.luannamorim.myfit.ui.feature.evolution

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.luannamorim.myfit.domain.body.BodyAssessment
import io.github.luannamorim.myfit.domain.body.BodyComparison
import io.github.luannamorim.myfit.domain.body.BodyMetrics
import io.github.luannamorim.myfit.domain.body.Measure
import io.github.luannamorim.myfit.domain.body.Sex
import io.github.luannamorim.myfit.domain.model.ThemeMode
import io.github.luannamorim.myfit.testutil.SCREENSHOT_DIR
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/**
 * Portuguese on a small phone: "Gordura corporal (estimada)" wraps in a half-width card, and the dot
 * must stay right after the last word (it is part of the label text), not float at the card's edge.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "pt-rBR-w360dp-h800dp-xxhdpi")
class BodySnapshotTilesPortugueseTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun dotFollowsTheWrappedLabel_andIsReadWithIt() {
        // Man, 80 kg, 1,78 m, neck 38, abdomen 92: BMI 25,2 and estimated body fat 21,5 % — both "Atenção".
        val values = mapOf(Measure.WEIGHT to 80.0, Measure.HEIGHT to 178.0, Measure.NECK to 38.0, Measure.ABDOMEN to 92.0)
        val items = BodyMetrics.derive(listOf(BodyAssessment(1, LocalDate.of(2026, 11, 12), values)), Sex.MALE)
        val snapshot = BodyMetrics.withStatus(BodyComparison.snapshot(items), Sex.MALE)!!
        compose.setContent {
            MyFitTheme(ThemeMode.LIGHT) { Box(Modifier.background(MyFitTheme.colors.background)) { BodySnapshotTiles(snapshot) } }
        }
        compose.onRoot().captureRoboImage("$SCREENSHOT_DIR/body_tiles_pt_360_light.png")
        compose.onNodeWithText("Gordura corporal (estimada) Atenção", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("IMC Atenção", useUnmergedTree = true).assertExists()
    }

    @Test
    fun pairHasEqualHeights_evenWhenTheBodyFatLabelWraps() {
        val values = mapOf(Measure.WEIGHT to 80.0, Measure.HEIGHT to 178.0, Measure.NECK to 38.0, Measure.ABDOMEN to 92.0)
        val items = BodyMetrics.derive(listOf(BodyAssessment(1, LocalDate.of(2026, 11, 12), values)), Sex.MALE)
        val snapshot = BodyMetrics.withStatus(BodyComparison.snapshot(items), Sex.MALE)!!
        compose.setContent { MyFitTheme(ThemeMode.LIGHT) { BodySnapshotTiles(snapshot) } }
        val results = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("Gordura corporal (estimada) Atenção", useUnmergedTree = true).fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        assertEquals(2, results.single().lineCount) // the label does wrap here
        val fat = compose.onNodeWithTag("bodyTile_BODY_FAT").getBoundsInRoot()
        val bmi = compose.onNodeWithTag("bodyTile_BMI").getBoundsInRoot()
        assertEquals(fat.bottom - fat.top, bmi.bottom - bmi.top)
    }
}
