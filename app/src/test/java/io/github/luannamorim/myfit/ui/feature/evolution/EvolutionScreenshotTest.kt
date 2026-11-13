package io.github.luannamorim.myfit.ui.feature.evolution

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.luannamorim.myfit.domain.body.BodyAssessment
import io.github.luannamorim.myfit.domain.body.BodyComparison
import io.github.luannamorim.myfit.domain.body.BodyMetrics
import io.github.luannamorim.myfit.domain.body.Measure
import io.github.luannamorim.myfit.domain.body.Sex
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

    // Height only on the first; body fat typed on the older three, estimated (Navy, male) on the newer ones.
    private val assessments = BodyMetrics.derive(
        (0 until 6).map { i ->
            BodyAssessment(
                id = i + 1L,
                date = today.minusWeeks((5 - i) * 4L),
                values = buildMap {
                    put(Measure.WEIGHT, 71.0 + i * 0.45)
                    if (i == 0) put(Measure.HEIGHT, 178.0)
                    if (i < 3) put(Measure.BODY_FAT, 17.0 - i * 0.3)
                    put(Measure.NECK, 38.0)
                    put(Measure.WAIST, 79.5 - i * 0.2)
                    put(Measure.ABDOMEN, 86.0 - i * 0.4)
                    put(Measure.ARM_RIGHT, 29.0 + i * 0.4)
                    put(Measure.CHEST, 92.0 + i * 0.8)
                },
            )
        }.reversed(),
        Sex.MALE,
    )

    private fun state(section: EvolutionSection) = EvolutionUiState(
        loading = false,
        section = section,
        assessments = assessments,
        snapshot = BodyMetrics.withStatus(BodyComparison.snapshot(assessments), Sex.MALE),
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
    fun evolution_measures_weight_only() = compose.snapBothThemes("evolution_measures_weight_only") {
        // Only weight and waist typed, no sex: body fat and BMI cards stay, saying what is missing.
        val weightOnly = assessments.map {
            it.copy(values = mapOf(Measure.WEIGHT to it.values.getValue(Measure.WEIGHT), Measure.WAIST to 78.0), estimated = emptySet())
        }
        EvolutionContent(
            state(EvolutionSection.MEASURES).copy(assessments = weightOnly, snapshot = BodyMetrics.withStatus(BodyComparison.snapshot(weightOnly), null)),
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
                lastValues = mapOf(Measure.WEIGHT to 72.8, Measure.HEIGHT to 178.0, Measure.CHEST to 95.0, Measure.BODY_FAT to 15.5),
                bmi = 23.1,
                estimatedBodyFat = 15.7,
                invalid = setOf(Measure.WAIST).minus(Measure.WAIST),
            ),
            {}, { _, _ -> }, {}, {}, {}, {},
        )
    }
}
