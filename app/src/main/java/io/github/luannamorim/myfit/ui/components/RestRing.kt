package io.github.luannamorim.myfit.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.luannamorim.myfit.ui.theme.MyFitTheme

/** Rest countdown ring. [progress] is the fraction of rest remaining (1 → 0). */
@Composable
fun RestRing(
    progress: Float,
    remainingText: String,
    modifier: Modifier = Modifier,
    size: Dp = 240.dp,
    caption: String? = null,
    ringColor: Color = MyFitTheme.colors.accent,
) {
    val c = MyFitTheme.colors
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), animationSpec = tween(250), label = "rest")
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 10.dp.toPx()
            val inset = stroke / 2
            val arcSize = androidx.compose.ui.geometry.Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            drawArc(c.outline, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            drawArc(ringColor, -90f, 360f * animated, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(remainingText, style = MyFitTheme.type.numericHero, color = c.textPrimary)
            if (caption != null) Text(caption, style = MyFitTheme.type.label, color = c.textSecondary)
        }
    }
}
