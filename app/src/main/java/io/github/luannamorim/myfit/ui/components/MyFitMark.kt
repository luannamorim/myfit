package io.github.luannamorim.myfit.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.luannamorim.myfit.ui.theme.Geist
import io.github.luannamorim.myfit.ui.theme.MyFitTheme

/**
 * Geometry of the MyFit mark on a 24x24 grid: an "M" drawn as one continuous progress line whose
 * last segment climbs above the first peak, ending at a detached accent dot (the latest data point).
 * Shared with the launcher/notification vector drawables (see docs/BRAND.md).
 */
internal object MarkGeometry {
    val points = listOf(Offset(3f, 19f), Offset(8f, 9f), Offset(12f, 14.5f), Offset(16.74f, 8.18f))
    val dot = Offset(19.5f, 4.5f)
    const val DOT_RADIUS = 2.3f
    const val STROKE = 2.6f
}

@Composable
fun MyFitMark(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    lineColor: Color = MyFitTheme.colors.textPrimary,
    dotColor: Color = MyFitTheme.colors.accent,
) {
    Canvas(modifier.size(size).semantics { contentDescription = "MyFit" }) {
        val s = this.size.minDimension / 24f
        val path = Path().apply {
            MarkGeometry.points.forEachIndexed { i, p ->
                if (i == 0) moveTo(p.x * s, p.y * s) else lineTo(p.x * s, p.y * s)
            }
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = MarkGeometry.STROKE * s, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawCircle(dotColor, radius = MarkGeometry.DOT_RADIUS * s, center = MarkGeometry.dot * s)
    }
}

@Composable
fun MyFitWordmark(modifier: Modifier = Modifier, fontSize: TextUnit = 22.sp, showMark: Boolean = true) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (showMark) MyFitMark(size = (fontSize.value * 1.25f).dp)
        Text(
            text = "myfit",
            fontFamily = Geist,
            fontWeight = FontWeight.SemiBold,
            fontSize = fontSize,
            letterSpacing = (-0.04).em,
            color = MyFitTheme.colors.textPrimary,
        )
    }
}
