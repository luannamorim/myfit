package io.github.luannamorim.myfit.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.domain.model.Intensity
import io.github.luannamorim.myfit.ui.components.MyFitChip
import io.github.luannamorim.myfit.ui.components.SecondaryButton
import io.github.luannamorim.myfit.ui.theme.MyFitShapes
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import io.github.luannamorim.myfit.ui.theme.Spacing

/** Low / medium / high chips. Tapping the selected one clears it (intensity is optional). */
@Composable
fun IntensityPicker(selected: Intensity?, onSelect: (Intensity?) -> Unit, modifier: Modifier = Modifier) {
    val c = MyFitTheme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        Text(stringResource(R.string.intensity_title), style = MyFitTheme.type.caption, color = c.textSecondary)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
            Intensity.entries.forEach { level ->
                MyFitChip(
                    stringResource(level.labelRes),
                    selected = selected == level,
                    onClick = { onSelect(if (selected == level) null else level) },
                )
            }
        }
    }
}

/**
 * Optional photo of a workout or activity: the picture with "Change" / "Remove", or an
 * "Add a photo" button. The caller launches the picker and imports the image.
 */
@Composable
fun PhotoField(imagePath: String?, onPick: () -> Unit, onRemove: () -> Unit, modifier: Modifier = Modifier, error: Boolean = false) {
    val c = MyFitTheme.colors
    val model = localImageModel(imagePath)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
        if (model != null) {
            AsyncImage(
                model,
                contentDescription = stringResource(R.string.workout_photo_a11y),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f).heightIn(max = 320.dp).clip(MyFitShapes.medium),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                SecondaryButton(stringResource(R.string.workout_photo_change), onClick = onPick, icon = Icons.Rounded.Image, modifier = Modifier.weight(1f))
                SecondaryButton(stringResource(R.string.workout_photo_remove), onClick = onRemove, icon = Icons.Rounded.Delete, contentColor = c.danger)
            }
        } else {
            SecondaryButton(stringResource(R.string.workout_photo_add), onClick = onPick, icon = Icons.Rounded.AddAPhoto, modifier = Modifier.fillMaxWidth())
        }
        if (error) Text(stringResource(R.string.exercise_image_error), style = MyFitTheme.type.caption, color = c.danger)
    }
}
