package io.github.luannamorim.myfit.ui.feature.evolution

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.domain.body.BodyAssessment
import io.github.luannamorim.myfit.domain.body.BodyComparison
import io.github.luannamorim.myfit.domain.body.BodyMetrics
import io.github.luannamorim.myfit.domain.body.Measure
import io.github.luannamorim.myfit.domain.body.Sex
import io.github.luannamorim.myfit.domain.model.ThemeMode
import io.github.luannamorim.myfit.testutil.PHONE
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate

/** The health-range dot on the Home/Progress cards: only BMI and body fat, read with the label ("BMI Good"). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36], qualifiers = PHONE)
class BodySnapshotTilesTest {
    @get:Rule val compose = createComposeRule()

    private fun show(values: Map<Measure, Double>, sex: Sex?) {
        val items = BodyMetrics.derive(listOf(BodyAssessment(1, LocalDate.of(2026, 11, 12), values)), sex)
        val snapshot = BodyMetrics.withStatus(BodyComparison.snapshot(items), sex)!!
        compose.setContent { MyFitTheme(ThemeMode.DARK) { BodySnapshotTiles(snapshot) } }
    }

    /** Cards whose label ends with this status word (the dot is inline content read as that word). */
    private fun count(status: String) = compose.onAllNodes(hasText(" $status", substring = true), useUnmergedTree = true)

    @Test
    fun bmiAndBodyFat_showTheirDot_weightDoesNot() {
        show(mapOf(Measure.WEIGHT to 73.25, Measure.HEIGHT to 178.0, Measure.NECK to 38.0, Measure.ABDOMEN to 85.0), Sex.MALE)
        count("Good").assertCountEquals(2) // BMI 23.1, body fat 16.4 %
        compose.onNodeWithText("BMI Good", useUnmergedTree = true).assertExists()
        count("Caution").assertCountEquals(0)
        count("Poor").assertCountEquals(0)
    }

    @Test
    fun bmiMissing_showsHint_bodyFatKeepsItsDot() {
        // No height, so no BMI value: its card stays, with "—" and what is missing.
        show(mapOf(Measure.WEIGHT to 73.0, Measure.BODY_FAT to 20.0), Sex.MALE)
        count("Caution").assertCountEquals(1)
        compose.onNodeWithText("Height missing", useUnmergedTree = true).assertExists()
    }

    @Test
    fun weightOnly_showsTwoEmptyCards() {
        show(mapOf(Measure.WEIGHT to 73.0), null)
        compose.onNodeWithText("Set your sex", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Height missing", useUnmergedTree = true).assertExists()
        compose.onAllNodes(hasText("—"), useUnmergedTree = true).assertCountEquals(2)
    }

    @Test
    fun withoutSex_bodyFatHasNoDot_bmiStillDoes() {
        show(mapOf(Measure.WEIGHT to 95.0, Measure.HEIGHT to 170.0, Measure.BODY_FAT to 30.0), null)
        count("Poor").assertCountEquals(1) // BMI 32.9; body fat 30 % gets nothing without sex
        compose.onNodeWithText("Set your sex", useUnmergedTree = true).assertDoesNotExist() // it was typed: there is a value
    }

    @Test
    fun emptyWeight_hasNoTrendLine() {
        val items = listOf(
            BodyAssessment(1, LocalDate.of(2026, 9, 1), mapOf(Measure.WEIGHT to 71.0)),
            BodyAssessment(2, LocalDate.of(2026, 10, 1), mapOf(Measure.WEIGHT to 72.0)),
            BodyAssessment(3, LocalDate.of(2026, 11, 12), mapOf(Measure.WAIST to 80.0)),
        )
        val snapshot = BodyMetrics.withStatus(BodyComparison.snapshot(BodyMetrics.derive(items, null)), null)!!
        compose.setContent { MyFitTheme(ThemeMode.DARK) { BodySnapshotTiles(snapshot) } }
        compose.onAllNodes(hasText("—"), useUnmergedTree = true).assertCountEquals(3) // weight, body fat, BMI
        compose.onAllNodes(hasText("since", substring = true), useUnmergedTree = true).assertCountEquals(0)
        compose.onNodeWithText("Weight missing", useUnmergedTree = true).assertExists()
    }
}
