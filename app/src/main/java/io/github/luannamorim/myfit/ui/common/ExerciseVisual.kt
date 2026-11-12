package io.github.luannamorim.myfit.ui.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.data.db.seed.ExerciseSeed
import io.github.luannamorim.myfit.domain.model.MuscleGroup
import io.github.luannamorim.myfit.ui.theme.MyFitShapes
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import io.github.luannamorim.myfit.ui.theme.Spacing
import java.io.IOException

/** Where a bundled drawing comes from, for the image credits. */
enum class ArtSource { EVERKINETIC, WORKOUT_GUIDE }

/**
 * Line drawings bundled for the predefined exercises (`assets/exercises/<art>-<frame>.webp`, alpha
 * masks tinted at draw time). Decoded synchronously: each file is a few KB and stays cached.
 */
object ExerciseArt {
    private const val DIR = "exercises"
    private val cache = object : LruCache<String, Bitmap>(12 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    fun artFor(seedKey: String?): String? = ExerciseSeed.forKey(seedKey)?.art

    fun source(art: String): ArtSource = if (art.startsWith("wg-")) ArtSource.WORKOUT_GUIDE else ArtSource.EVERKINETIC

    /** Frame 1 is the start position, frame 2 (absent for static holds) the end position. */
    fun load(context: Context, art: String, frame: Int, sampleSize: Int = 1): ImageBitmap? {
        val key = "$art-$frame@$sampleSize"
        cache.get(key)?.let { return it.asImageBitmap() }
        val bitmap = try {
            context.assets.open("$DIR/$art-$frame.webp").use { input ->
                BitmapFactory.decodeStream(input, null, BitmapFactory.Options().apply { inSampleSize = sampleSize })
            }
        } catch (_: IOException) {
            null
        } ?: return null
        cache.put(key, bitmap)
        return bitmap.asImageBitmap()
    }
}

@Composable
private fun rememberFrame(art: String, frame: Int, sampleSize: Int): ImageBitmap? {
    val context = LocalContext.current
    return remember(art, frame, sampleSize) { ExerciseArt.load(context, art, frame, sampleSize) }
}

/** Start and end position side by side, tinted with [color]. */
@Composable
fun ExerciseDrawing(art: String, color: Color, modifier: Modifier = Modifier, contentDescription: String? = null) {
    val start = rememberFrame(art, 1, 1)
    val end = rememberFrame(art, 2, 1)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalAlignment = Alignment.CenterVertically) {
        val tint = ColorFilter.tint(color)
        if (start != null) {
            Image(start, contentDescription, Modifier.weight(1f).fillMaxHeight(), contentScale = ContentScale.Fit, colorFilter = tint)
        }
        if (end != null) {
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = MyFitTheme.colors.textTertiary, modifier = Modifier.size(16.dp))
            Image(end, null, Modifier.weight(1f).fillMaxHeight(), contentScale = ContentScale.Fit, colorFilter = tint)
        }
    }
}

/**
 * The picture of an exercise: the user's photo, else the bundled drawing of a library exercise,
 * else the muscle map of its group. Callers give it a size (typically 16:9).
 */
@Composable
fun ExerciseVisual(
    imagePath: String?,
    seedKey: String?,
    muscleGroup: MuscleGroup,
    name: String,
    modifier: Modifier = Modifier,
    background: Color = MyFitTheme.colors.surfaceRaised,
    lineColor: Color = MyFitTheme.colors.textPrimary,
) {
    val photo = localImageModel(imagePath)
    val art = ExerciseArt.artFor(seedKey)
    Box(modifier.clip(MyFitShapes.medium).background(background), contentAlignment = Alignment.Center) {
        when {
            photo != null -> AsyncImage(photo, contentDescription = name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            art != null -> ExerciseDrawing(art, lineColor, Modifier.fillMaxSize().padding(Spacing.m), contentDescription = name)
            else -> MuscleMap(muscleGroup, Modifier.fillMaxSize().padding(Spacing.m))
        }
    }
}

/** Small square version for lists: photo, else the end position of the drawing, else the map. */
@Composable
fun ExerciseThumbnail(imagePath: String?, seedKey: String?, muscleGroup: MuscleGroup, modifier: Modifier = Modifier) {
    val c = MyFitTheme.colors
    val photo = localImageModel(imagePath)
    val art = ExerciseArt.artFor(seedKey)
    Box(modifier.clip(MyFitShapes.small).background(c.surfaceRaised), contentAlignment = Alignment.Center) {
        when {
            photo != null -> AsyncImage(photo, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            art != null -> {
                val frame = rememberFrame(art, 2, 2) ?: rememberFrame(art, 1, 2)
                if (frame != null) {
                    Image(frame, null, Modifier.fillMaxSize().padding(3.dp), contentScale = ContentScale.Fit, colorFilter = ColorFilter.tint(c.textSecondary))
                }
            }
            else -> MuscleMap(muscleGroup, Modifier.fillMaxSize().padding(2.dp))
        }
    }
}

/** Opens a video search for the exercise (YouTube app or browser); the app itself stays offline. */
fun openExerciseVideo(context: Context, exerciseName: String) {
    val query = context.getString(R.string.exercise_video_query, exerciseName)
    val intent = Intent(Intent.ACTION_VIEW, ("https://www.youtube.com/results?search_query=" + Uri.encode(query)).toUri())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.exercise_video_unavailable, Toast.LENGTH_SHORT).show()
    }
}
