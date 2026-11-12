package io.github.luannamorim.myfit.ui.feature.evolution

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.domain.body.BodyAssessment
import io.github.luannamorim.myfit.domain.body.BodyComparison
import io.github.luannamorim.myfit.domain.body.Measure
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
class EvolutionScreenshotTest {
    @get:Rule val compose = createComposeRule()
    private val today = LocalDate.of(2026, 11, 12)

    private val assessments = (0 until 6).map { i ->
        BodyAssessment(
            id = i + 1L,
            date = today.minusWeeks((5 - i) * 4L),
            values = mapOf(
                Measure.WEIGHT to 71.0 + i * 0.45,
                Measure.BODY_FAT to 17.0 - i * 0.3,
                Measure.WAIST to 79.5 - i * 0.2,
                Measure.ARM_RIGHT to 29.0 + i * 0.4,
                Measure.CHEST to 92.0 + i * 0.8,
            ),
        )
    }.reversed()

    private fun state(section: EvolutionSection) = EvolutionUiState(
        loading = false,
        section = section,
        assessments = assessments,
        snapshot = BodyComparison.snapshot(assessments),
        charts = buildCharts(assessments, DEFAULT_CHART_MEASURES, ChartPeriod.ALL, today),
        today = today,
    )

    @Test
    fun evolution_measures() = compose.snapBothThemes("evolution_measures") {
        EvolutionContent(state(EvolutionSection.MEASURES), EvolutionActions(), {}, {}, {}, {}, {}, {})
    }

    @Test
    fun evolution_charts() = compose.snapBothThemes("evolution_charts") {
        EvolutionContent(state(EvolutionSection.CHARTS), EvolutionActions(), {}, {}, {}, {}, {}, {})
    }

    @Test
    fun evolution_charts_all() = compose.snapBothThemes("evolution_charts_all") {
        val all = Measure.entries.toSet()
        EvolutionContent(
            state(EvolutionSection.CHARTS).copy(selectedMeasures = all, charts = buildCharts(assessments, all, ChartPeriod.ALL, today)),
            EvolutionActions(), {}, {}, {}, {}, {}, {},
        )
    }

    @Test
    fun evolution_photos() = compose.snapBothThemes("evolution_photos") {
        EvolutionContent(state(EvolutionSection.PHOTOS).copy(photoDate = today.minusMonths(2)), EvolutionActions(), {}, {}, {}, {}, {}, {})
    }

    @Test
    fun assessment_compare() = compose.snapBothThemes("assessment_compare") {
        val from = assessments.last()
        val to = assessments.first()
        AssessmentCompareContent(
            CompareUiState(loading = false, assessments = assessments.reversed(), from = from, to = to, rows = BodyComparison.compare(from, to).filter { it.from != null }),
            {}, {}, {},
        )
    }

    @Test
    fun assessment_editor() = compose.snapBothThemes("assessment_editor") {
        AssessmentEditorContent(
            AssessmentEditorUiState(
                loading = false,
                date = today,
                fields = mapOf(Measure.WEIGHT to "73.2", Measure.WAIST to "79"),
                lastValues = mapOf(Measure.WEIGHT to 72.8, Measure.CHEST to 95.0, Measure.BODY_FAT to 15.5),
                invalid = setOf(Measure.WAIST).minus(Measure.WAIST),
            ),
            {}, { _, _ -> }, {}, {}, {}, {},
        )
    }
}
