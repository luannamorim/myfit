package io.github.luannamorim.myfit.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.domain.model.MuscleGroup
import io.github.luannamorim.myfit.domain.model.MuscleGroup.ABS
import io.github.luannamorim.myfit.domain.model.MuscleGroup.BACK
import io.github.luannamorim.myfit.domain.model.MuscleGroup.BICEPS
import io.github.luannamorim.myfit.domain.model.MuscleGroup.CALVES
import io.github.luannamorim.myfit.domain.model.MuscleGroup.CHEST
import io.github.luannamorim.myfit.domain.model.MuscleGroup.FOREARMS
import io.github.luannamorim.myfit.domain.model.MuscleGroup.GLUTES
import io.github.luannamorim.myfit.domain.model.MuscleGroup.HAMSTRINGS
import io.github.luannamorim.myfit.domain.model.MuscleGroup.QUADRICEPS
import io.github.luannamorim.myfit.domain.model.MuscleGroup.SHOULDERS
import io.github.luannamorim.myfit.domain.model.MuscleGroup.TRICEPS
import io.github.luannamorim.myfit.ui.theme.MyFitTheme

/**
 * One fixed body drawing, front and back, where the regions of [group] light up in the accent
 * colour. Full body lights every muscle; cardio and "other" show the body without highlight.
 * The caller sizes it; the drawing is scaled to fit and centred.
 */
@Composable
fun MuscleMap(group: MuscleGroup?, modifier: Modifier = Modifier) {
    val c = MyFitTheme.colors
    val description = stringResource(R.string.muscle_map_a11y, stringResource((group ?: MuscleGroup.OTHER).labelRes))
    Canvas(modifier.semantics { contentDescription = description }) {
        drawBody(group, base = c.outline, muscle = c.outlineStrong, highlight = c.accent)
    }
}

private fun DrawScope.drawBody(group: MuscleGroup?, base: Color, muscle: Color, highlight: Color) {
    val s = minOf(size.width / BodyMap.WIDTH, size.height / BodyMap.HEIGHT)
    withTransform({
        translate((size.width - BodyMap.WIDTH * s) / 2f, (size.height - BodyMap.HEIGHT * s) / 2f)
        scale(s, s, pivot = Offset.Zero)
    }) {
        listOf(0f to BodyMap.front, BodyMap.BACK_OFFSET to BodyMap.back).forEach { (dx, regions) ->
            translate(left = dx) {
                regions.forEach { region ->
                    val color = when {
                        region.group == null -> base
                        BodyMap.lights(group, region.group) -> highlight
                        else -> muscle
                    }
                    drawPath(region.path, color)
                    if (region.mirror) scale(-1f, 1f, pivot = Offset(BodyMap.VIEW_WIDTH / 2f, 0f)) { drawPath(region.path, color) }
                }
            }
        }
    }
}

/** Body regions drawn in a 100 × 205 box per view; most are the left half, mirrored at x = 50. */
internal object BodyMap {
    const val VIEW_WIDTH = 100f
    const val BACK_OFFSET = 110f
    const val WIDTH = 210f
    const val HEIGHT = 205f

    class Region(val group: MuscleGroup?, val path: Path, val mirror: Boolean)

    fun lights(selected: MuscleGroup?, region: MuscleGroup): Boolean = when (selected) {
        null, MuscleGroup.CARDIO, MuscleGroup.OTHER -> false
        MuscleGroup.FULL_BODY -> true
        else -> selected == region
    }

    private fun r(group: MuscleGroup?, d: String, mirror: Boolean = true) = Region(group, PathParser().parsePathString(d).toPath(), mirror)

    private const val HEAD = "M50 3.5 C55.5 3.5 59 8 59 14 C59 20.5 55.5 25 50 25 C44.5 25 41 20.5 41 14 C41 8 44.5 3.5 50 3.5 Z"
    private const val FOREARM = "M21.4 74.4 C18.8 81.6 17 89.6 16.6 97.4 C18 99.2 21 99.2 22.2 97.4 C24.4 90 26.8 82.4 28.2 75.4 C26.6 72.8 23 72.6 21.4 74.4 Z"
    private const val HAND = "M16.8 100.6 C15.2 103.8 15 108.2 16.2 111.4 C18 113 20.6 112.4 21.6 110.2 C22.4 107 22.6 103.6 22 100.8 C20.4 99.6 18.2 99.6 16.8 100.6 Z"
    private const val KNEE = "M37.8 144.4 C40 146.4 43.4 146.4 45.4 144.4 C46 147.4 45.6 150.6 44.2 152.6 C42 153.6 40 153.4 38.6 152.4 C37.4 150 37.2 147 37.8 144.4 Z"
    private const val FOOT = "M38.8 186.4 C38.2 190.4 37.6 194.4 37.2 197.2 C38.6 199 42.4 199.2 44.2 197.8 C43.8 194.2 43.2 190.2 42.6 186.6 C41.4 185.6 40 185.6 38.8 186.4 Z"

    val front: List<Region> by lazy {
        listOf(
            r(null, HEAD, mirror = false),
            r(null, "M45.2 24 L54.8 24 L55.6 31.5 C53 33 47 33 44.4 31.5 Z", mirror = false),
            r(SHOULDERS, "M40.6 33 C34 33.2 28.4 35 25.8 39.2 C24.2 42.2 24 46.4 24.6 50.6 C27.2 47.4 30.4 44.6 34 42.6 C35.6 39.4 37.8 35.8 40.6 33 Z"),
            r(CHEST, "M49.2 37.6 L49.2 53.2 C45.5 56 38.6 56.2 34 53.4 C31.4 51.8 30.6 48.4 31.6 45.2 C33 41.6 37.4 39.6 41.6 38.6 C44.4 37.9 46.8 37.6 49.2 37.6 Z"),
            r(BICEPS, "M24.6 52.8 C22.4 57.8 21.4 64 21.8 70.6 C23.6 72.6 27.4 72.6 29 70.4 C30.6 64.6 31.4 58.6 31 53.2 C29.2 50.2 26.2 50.2 24.6 52.8 Z"),
            r(FOREARMS, FOREARM),
            r(null, HAND),
            r(ABS, "M45.2 57.6 L49.2 57.4 L49.2 63.8 L44.8 64.2 C44.6 61.8 44.8 59.6 45.2 57.6 Z"),
            r(ABS, "M44.7 66 L49.2 65.8 L49.2 72.4 L44.6 72.8 Z"),
            r(ABS, "M44.6 74.6 L49.2 74.4 L49.2 81.2 L44.8 81.8 Z"),
            r(ABS, "M44.9 83.6 L49.2 83.4 L49.2 94.6 C47.4 93.4 45.8 91.6 45 89.6 Z"),
            r(ABS, "M42.6 57.8 C38.6 60.4 35.8 65.6 35.4 72 C35 78.4 36.8 84.2 40.2 88.6 L43 87.4 C42.4 80.6 42.4 72 42.8 64.4 Z"),
            r(null, "M49.2 96.6 L49.2 104.6 C47 103.2 43.6 100.6 40.8 97.4 C40 95.8 40.6 93.4 41.8 92.2 C44.2 94.4 46.8 95.8 49.2 96.6 Z"),
            r(QUADRICEPS, "M38.8 97.6 C42.4 101.8 46.2 104.8 48.4 107.6 C48.6 117.6 47.6 129.6 45.6 140.4 C43.4 143.2 39.4 143.4 37.2 140.6 C35 130.4 34.4 118.6 35.4 108 C35.8 103.8 37 100.2 38.8 97.6 Z"),
            r(null, KNEE),
            r(CALVES, "M38 155.2 C35.8 163.4 36 173 38 183.2 C39.4 184.8 41.8 184.8 42.8 183 C44.4 173.6 44.8 163.6 43.8 155.6 C42 154 39.6 153.8 38 155.2 Z"),
            r(null, FOOT),
        )
    }

    val back: List<Region> by lazy {
        listOf(
            r(null, HEAD, mirror = false),
            r(null, "M45.2 24 L54.8 24 L55.4 29 C53 30 47 30 44.6 29 Z", mirror = false),
            r(BACK, "M50 27.6 C46.6 30.2 41.2 31.6 35.6 33.6 C40 36.8 44.2 41.4 46.4 47 C47.6 52 48.6 58.4 50 66 Z"),
            r(SHOULDERS, "M34.4 34.6 C29.6 35.4 26.2 37.6 25 41.6 C24.2 44.6 24.2 47.8 24.8 50.8 C28.2 46.8 32 43.4 37 41.2 C36.6 38.8 35.8 36.6 34.4 34.6 Z"),
            r(TRICEPS, "M24.6 53.2 C22.4 58.4 21.4 64.4 21.8 70.6 C23.6 72.6 27.4 72.6 29 70.4 C30.8 64.6 31.6 58.6 31.2 53.2 C29.4 50.4 26.2 50.4 24.6 53.2 Z"),
            r(FOREARMS, FOREARM),
            r(null, HAND),
            r(BACK, "M38.2 42.2 C42.8 45.6 45 50.4 45.8 56.8 C46.4 63.4 47 69 47.6 74.6 C44.2 78.2 40.6 81 37.8 83.4 C35.6 76.4 33.6 67.8 32.8 59.8 C32.4 53.6 34.4 46.8 38.2 42.2 Z"),
            r(BACK, "M49.2 70 L49.2 92.4 C46.4 91.6 43.6 90.2 41.4 88.2 C40.8 85.6 41 83.4 42 81.4 C44.6 78.8 46.8 75.4 49.2 70 Z"),
            r(GLUTES, "M49.2 95.2 L49.2 113.2 C45.6 115.4 40.2 115.6 36.8 112.8 C34.8 109.4 34.8 104.4 36.4 100 C38.6 95.6 43.6 93.6 49.2 95.2 Z"),
            r(HAMSTRINGS, "M36.6 116.6 C40.4 118.8 45.4 118.6 48.2 116.6 C48.4 124.6 47.4 133.4 45.6 141.4 C43.4 144 39.6 144 37.4 141.4 C35.8 133.6 35.4 124.6 36.6 116.6 Z"),
            r(null, KNEE),
            r(CALVES, "M37.8 154.8 C35.8 160.4 35.6 167.4 37 174.4 C38 180 39.4 183.6 40.6 184.4 C41.8 183.6 43.2 180 44 174.4 C45.2 167.4 45.4 160.4 43.4 155 C41.6 153.4 39.6 153.4 37.8 154.8 Z"),
            r(null, FOOT),
        )
    }
}
