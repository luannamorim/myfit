package io.github.luannamorim.myfit.ui.feature.evolution

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLineComponent
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import io.github.luannamorim.myfit.core.format.Dates
import io.github.luannamorim.myfit.core.format.DecimalInput
import io.github.luannamorim.myfit.ui.theme.Geist
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import java.time.LocalDate
import kotlin.math.max


/** Line chart of one indicator over time (x = epoch day, so spacing follows real dates). */
@Composable
fun MeasureChart(points: List<Pair<LocalDate, Double>>, modifier: Modifier = Modifier) {
    val c = MyFitTheme.colors
    // Synchronous model: renders on the first frame (no async producer), which keeps it testable.
    val model = remember(points) {
        CartesianChartModel(
            LineCartesianLayerModel.build { series(points.map { it.first.toEpochDay() }, points.map { it.second }) },
        )
    }
    // Fit the Y range to the data (body measures change by small amounts; starting at zero flattens them).
    val range = remember(points) { yRange(points.map { it.second }) }
    val labelStyle = TextStyle(color = c.textTertiary, fontSize = 11.sp, fontFamily = Geist)
    val labelSpacing = max(1, points.size / 4)
    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                rangeProvider = remember(range) { CartesianLayerRangeProvider.fixed(minY = range.first, maxY = range.second) },
                lineProvider = LineCartesianLayer.LineProvider.series(
                    LineCartesianLayer.rememberLine(
                        fill = LineCartesianLayer.LineFill.single(Fill(c.accent)),
                        areaFill = LineCartesianLayer.AreaFill.single(
                            Fill(Brush.verticalGradient(listOf(c.accent.copy(alpha = 0.16f), Color.Transparent))),
                        ),
                        pointProvider = LineCartesianLayer.PointProvider.single(
                            LineCartesianLayer.Point(rememberShapeComponent(fill = Fill(c.accent), shape = CircleShape), 7.dp),
                        ),
                    ),
                ),
            ),
            startAxis = VerticalAxis.rememberStart(
                line = null,
                tick = null,
                label = rememberAxisLabelComponent(style = labelStyle),
                guideline = rememberAxisGuidelineComponent(fill = Fill(c.outline)),
                valueFormatter = remember { CartesianValueFormatter { _, value, _ -> DecimalInput.format(value) } },
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                line = rememberAxisLineComponent(fill = Fill(c.outline)),
                tick = null,
                guideline = null,
                label = rememberAxisLabelComponent(style = labelStyle),
                valueFormatter = remember { CartesianValueFormatter { _, value, _ -> Dates.axis(LocalDate.ofEpochDay(value.toLong())) } },
                itemPlacer = remember(labelSpacing) { HorizontalAxis.ItemPlacer.aligned(spacing = { labelSpacing }) },
            ),
        ),
        model = model,
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        modifier = modifier.fillMaxWidth().height(180.dp),
    )
}

/** Y range with ~25% padding around the data, rounded to whole units, never below zero. */
internal fun yRange(values: List<Double>): Pair<Double, Double> {
    if (values.isEmpty()) return 0.0 to 1.0
    val min = values.min()
    val max = values.max()
    val pad = maxOf(1.0, (max - min) * 0.25)
    return kotlin.math.floor(maxOf(0.0, min - pad)) to kotlin.math.ceil(max + pad)
}
