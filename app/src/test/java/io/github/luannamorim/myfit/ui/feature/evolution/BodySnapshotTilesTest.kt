package io.github.luannamorim.myfit.ui.feature.evolution

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.domain.body.BodyAssessment
import io.github.luannamorim.myfit.domain.body.BodyComparison
import io.github.luannamorim.myfit.domain.body.BodyMetrics
import io.github.luannamorim.myfit.domain.body.Measure
import io.github.luannamorim.myfit.domain.body.Sex
import io.github.luannamorim.myfit.domain.model.ThemeMode
import io.github.luannamorim.myfit.testutil.PHONE
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import org.junit.Assert.assertEquals
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

    private fun show(values: Map<Measure, Double>, sex: Sex?, onOpenWeight: (() -> Unit)? = null) {
        val items = BodyMetrics.derive(listOf(BodyAssessment(1, LocalDate.of(2026, 11, 12), values)), sex)
        val snapshot = BodyMetrics.withStatus(BodyComparison.snapshot(items), sex)!!
        compose.setContent { MyFitTheme(ThemeMode.DARK) { BodySnapshotTiles(snapshot, onOpenWeight) } }
    }

    private fun currentRow(text: String) = hasTestTag("range_current") and hasAnyDescendant(hasText(text))

    /** Cards whose label is read with this status word ("BMI Good"); the dot itself is drawn in the corner. */
    private fun count(status: String) = compose.onAllNodes(hasContentDescription(" $status", substring = true), useUnmergedTree = true)

    @Test
    fun bmiAndBodyFat_showTheirDot_weightDoesNot() {
        show(mapOf(Measure.WEIGHT to 73.25, Measure.HEIGHT to 178.0, Measure.NECK to 38.0, Measure.ABDOMEN to 85.0), Sex.MALE)
        count("Good").assertCountEquals(2) // BMI 23.1, body fat 16.4 %
        compose.onNodeWithContentDescription("BMI Good", useUnmergedTree = true).assertExists()
        compose.onNodeWithContentDescription("Body fat Good", useUnmergedTree = true).assertExists() // no "(est.)"
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

    @Test
    fun tapBmi_showsRanges_withCurrentRow() {
        show(mapOf(Measure.WEIGHT to 73.25, Measure.HEIGHT to 178.0), null) // BMI 23.1
        compose.onNodeWithTag("bodyTile_BMI").performClick()
        compose.onNodeWithText("Below 16", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("30 or more", useUnmergedTree = true).assertExists()
        compose.onNode(currentRow("18.5 – 24.9"), useUnmergedTree = true).assertExists()
    }

    @Test
    fun emptyBmi_sheetHasNoCurrentRow() {
        show(mapOf(Measure.WEIGHT to 73.0), null)
        compose.onNodeWithTag("bodyTile_BMI").performClick()
        compose.onNodeWithText("18.5 – 24.9", useUnmergedTree = true).assertExists()
        compose.onAllNodes(hasTestTag("range_current"), useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun bodyFatWithoutSex_showsBothColumns() {
        show(mapOf(Measure.WEIGHT to 73.0, Measure.BODY_FAT to 20.0), null)
        compose.onNodeWithTag("bodyTile_BODY_FAT").performClick()
        compose.onNodeWithText("Men", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Women", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("6 – 17.9", useUnmergedTree = true).assertExists() // men's good band
        compose.onNodeWithText("14 – 24.9", useUnmergedTree = true).assertExists() // women's good band
        compose.onNodeWithText("Set your sex", useUnmergedTree = true).assertExists()
    }

    @Test
    fun tapWeight_opensProgress_tapBmi_doesNot() {
        var opened = 0
        show(mapOf(Measure.WEIGHT to 73.25, Measure.HEIGHT to 178.0), null, onOpenWeight = { opened++ })
        compose.onNodeWithTag("bodyTile_BMI").performClick()
        assertEquals(0, opened)
        compose.onNodeWithText("18.5 – 24.9", useUnmergedTree = true).assertExists()
    }

    @Test
    fun tapWeight_opensProgress() {
        var opened = 0
        show(mapOf(Measure.WEIGHT to 73.25, Measure.HEIGHT to 178.0), null, onOpenWeight = { opened++ })
        compose.onNodeWithTag("bodyTile_WEIGHT").performClick()
        assertEquals(1, opened)
    }

    @Test
    fun bodyFatDecimal_highlightsTheRowThatContainsIt() {
        show(mapOf(Measure.WEIGHT to 73.0, Measure.BODY_FAT to 24.5), Sex.MALE)
        compose.onNodeWithTag("bodyTile_BODY_FAT").performClick()
        compose.onNode(currentRow("18 – 24.9"), useUnmergedTree = true).assertExists()
    }

    @Test
    fun currentRow_isAnnouncedAsSelected() {
        show(mapOf(Measure.WEIGHT to 73.25, Measure.HEIGHT to 178.0), null)
        compose.onNodeWithTag("bodyTile_BMI").performClick()
        compose.onNode(isSelected() and hasText("18.5 – 24.9", substring = true)).assertExists()
    }

    @Test
    fun bodyFatWithoutSex_columnsAligned() {
        show(mapOf(Measure.WEIGHT to 73.0, Measure.BODY_FAT to 20.0), null)
        compose.onNodeWithTag("bodyTile_BODY_FAT").performClick()
        fun left(text: String) = compose.onNodeWithText(text, useUnmergedTree = true).getBoundsInRoot().left
        assertEquals(left("Men"), left("Below 2"))
        assertEquals(left("Women"), left("Below 10"))
        assertEquals(left("Women"), left("14 – 24.9"))
    }

    @Test
    @Config(qualifiers = "w640dp-h320dp-land-xhdpi")
    fun sheet_scrolls_onAShortScreen() {
        show(mapOf(Measure.WEIGHT to 73.25, Measure.HEIGHT to 178.0), null)
        compose.onNodeWithTag("bodyTile_BMI").performClick()
        compose.onNodeWithText("WHO ranges for adults. BMI doesn't tell muscle from fat.", useUnmergedTree = true)
            .performScrollTo().assertIsDisplayed()
    }
}
