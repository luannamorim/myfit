package io.github.luannamorim.myfit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.luannamorim.myfit.ui.theme.MyFitShapes
import io.github.luannamorim.myfit.ui.theme.MyFitTheme

/** Thin rounded bar filled to [fraction] (0..1; anything above shows full). */
@Composable
fun ProgressBar(fraction: Float, modifier: Modifier = Modifier, color: Color = MyFitTheme.colors.accent) {
    val shown = fraction.coerceIn(0f, 1f)
    Box(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(MyFitShapes.pill)
            .background(MyFitTheme.colors.surfaceRaised)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(shown, 0f..1f) },
    ) {
        if (shown > 0f) Box(Modifier.fillMaxWidth(shown).fillMaxHeight().clip(MyFitShapes.pill).background(color))
    }
}
