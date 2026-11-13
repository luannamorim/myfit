package io.github.luannamorim.myfit.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import io.github.luannamorim.myfit.ui.theme.Spacing

/** One photo in the viewer: its stored path and an optional caption (date, pose). */
data class ViewerPage(val imagePath: String, val caption: String? = null)

/**
 * Full-screen photo viewer, like a gallery: swipe between [pages], pinch or double-tap to zoom,
 * back or ✕ to close.
 */
@Composable
fun PhotoViewer(pages: List<ViewerPage>, initialPage: Int, onDismiss: () -> Unit) {
    if (pages.isEmpty()) return
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val pager = rememberPagerState(initialPage = initialPage.coerceIn(0, pages.lastIndex)) { pages.size }
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize(), key = { pages[it].imagePath }) { index ->
                ZoomableImage(pages[index].imagePath, pages[index].caption)
            }
            Row(
                Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.45f)).safeDrawingPadding().padding(end = Spacing.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_close), tint = Color.White)
                }
                Column(Modifier.weight(1f)) {
                    pages[pager.currentPage].caption?.let { Text(it, style = MyFitTheme.type.bodyStrong, color = Color.White) }
                }
                if (pages.size > 1) {
                    Text("${pager.currentPage + 1} / ${pages.size}", style = MyFitTheme.type.caption, color = Color.White.copy(alpha = 0.8f))
                }
            }
        }
    }
}

/**
 * Fits the photo on screen. Two fingers zoom (up to 5×) and, once zoomed, one finger pans; at 1×
 * single-finger drags are left to the pager. Double tap toggles 2.5× around the tap.
 */
@Composable
private fun ZoomableImage(imagePath: String, description: String?) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    fun clamp(value: Offset, s: Float): Offset {
        val maxX = size.width * (s - 1) / 2
        val maxY = size.height * (s - 1) / 2
        return Offset(value.x.coerceIn(-maxX, maxX), value.y.coerceIn(-maxY, maxY))
    }
    val model = localImageModel(imagePath)
    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { tap ->
                    if (scale > 1f) {
                        scale = 1f
                        offset = Offset.Zero
                    } else {
                        scale = DOUBLE_TAP_SCALE
                        val center = Offset(size.width / 2f, size.height / 2f)
                        offset = clamp((center - tap) * (DOUBLE_TAP_SCALE - 1), DOUBLE_TAP_SCALE)
                    }
                })
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val pinching = event.changes.count { it.pressed } > 1
                        if (pinching || scale > 1f) {
                            val next = (scale * event.calculateZoom()).coerceIn(1f, MAX_SCALE)
                            offset = if (next == 1f) Offset.Zero else clamp(offset + event.calculatePan(), next)
                            scale = next
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model,
            contentDescription = description,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            },
        )
    }
}

private const val MAX_SCALE = 5f
private const val DOUBLE_TAP_SCALE = 2.5f
