package io.github.luannamorim.myfit.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.EmojiPeople
import androidx.compose.material.icons.rounded.PedalBike
import androidx.compose.material.icons.rounded.Pool
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.SportsBasketball
import androidx.compose.material.icons.rounded.SportsGymnastics
import androidx.compose.material.icons.rounded.SportsKabaddi
import androidx.compose.material.icons.rounded.SportsMartialArts
import androidx.compose.material.icons.rounded.SportsMma
import androidx.compose.material.icons.rounded.SportsSoccer
import androidx.compose.material.icons.rounded.SportsTennis
import androidx.compose.material.icons.rounded.SportsVolleyball
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.domain.activity.Activity
import io.github.luannamorim.myfit.domain.activity.Sport
import io.github.luannamorim.myfit.domain.model.Intensity
import io.github.luannamorim.myfit.ui.theme.MyFitShapes
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import io.github.luannamorim.myfit.ui.theme.Spacing

@get:StringRes
val Intensity.labelRes: Int
    get() = when (this) {
        Intensity.LOW -> R.string.intensity_low
        Intensity.MEDIUM -> R.string.intensity_medium
        Intensity.HIGH -> R.string.intensity_high
    }

@get:StringRes
val Sport.labelRes: Int
    get() = when (this) {
        Sport.SWIMMING -> R.string.sport_swimming
        Sport.CYCLING -> R.string.sport_cycling
        Sport.INDOOR_CYCLING -> R.string.sport_indoor_cycling
        Sport.RUNNING -> R.string.sport_running
        Sport.TREADMILL -> R.string.sport_treadmill
        Sport.WALKING -> R.string.sport_walking
        Sport.SOCCER -> R.string.sport_soccer
        Sport.VOLLEYBALL -> R.string.sport_volleyball
        Sport.BASKETBALL -> R.string.sport_basketball
        Sport.BEACH_TENNIS -> R.string.sport_beach_tennis
        Sport.MUAY_THAI -> R.string.sport_muay_thai
        Sport.JIU_JITSU -> R.string.sport_jiu_jitsu
        Sport.BOXING -> R.string.sport_boxing
        Sport.YOGA -> R.string.sport_yoga
        Sport.PILATES -> R.string.sport_pilates
        Sport.STRETCHING -> R.string.sport_stretching
        Sport.OTHER -> R.string.sport_other
    }

/** Small figure for lists and chips (Material icons, Apache 2.0, already bundled). */
val Sport.icon: ImageVector
    get() = when (this) {
        Sport.SWIMMING -> Icons.Rounded.Pool
        Sport.CYCLING -> Icons.AutoMirrored.Rounded.DirectionsBike
        Sport.INDOOR_CYCLING -> Icons.Rounded.PedalBike
        Sport.RUNNING, Sport.TREADMILL -> Icons.AutoMirrored.Rounded.DirectionsRun
        Sport.WALKING -> Icons.AutoMirrored.Rounded.DirectionsWalk
        Sport.SOCCER -> Icons.Rounded.SportsSoccer
        Sport.VOLLEYBALL -> Icons.Rounded.SportsVolleyball
        Sport.BASKETBALL -> Icons.Rounded.SportsBasketball
        Sport.BEACH_TENNIS -> Icons.Rounded.SportsTennis
        Sport.MUAY_THAI -> Icons.Rounded.SportsMartialArts
        Sport.JIU_JITSU -> Icons.Rounded.SportsKabaddi
        Sport.BOXING -> Icons.Rounded.SportsMma
        Sport.YOGA -> Icons.Rounded.SelfImprovement
        Sport.PILATES -> Icons.Rounded.SportsGymnastics
        Sport.STRETCHING -> Icons.Rounded.AccessibilityNew
        Sport.OTHER -> Icons.Rounded.EmojiPeople
    }

/** Bundled line drawing for sports done on a machine (same set as the exercise drawings), else null. */
val Sport.art: String?
    get() = when (this) {
        Sport.INDOOR_CYCLING -> "wg-cycling"
        Sport.TREADMILL -> "wg-running"
        else -> null
    }

/** The name shown for an activity: the user's own name for "Free", else the sport's. */
@Composable
fun activityTitle(activity: Activity): String =
    activity.name?.takeIf { activity.sport == Sport.OTHER && it.isNotBlank() } ?: stringResource(activity.sport.labelRes)

/** Round badge with the sport's icon. */
@Composable
fun SportBadge(sport: Sport, size: Dp, modifier: Modifier = Modifier, selected: Boolean = false) {
    val c = MyFitTheme.colors
    Box(
        modifier.size(size).clip(MyFitShapes.pill).background(if (selected) c.accent else c.surfaceRaised),
        contentAlignment = Alignment.Center,
    ) {
        Icon(sport.icon, contentDescription = null, tint = if (selected) c.onAccent else c.textPrimary, modifier = Modifier.size(size * 0.55f))
    }
}

/** Large figure for the activity editor: the line drawing when there is one, else a big badge. */
@Composable
fun SportFigure(sport: Sport, modifier: Modifier = Modifier) {
    val c = MyFitTheme.colors
    val art = sport.art
    Box(modifier.clip(MyFitShapes.medium).background(c.surfaceRaised), contentAlignment = Alignment.Center) {
        if (art != null) {
            ExerciseDrawing(art, c.textPrimary, Modifier.fillMaxSize().padding(Spacing.m))
        } else {
            Icon(sport.icon, contentDescription = null, tint = c.accent, modifier = Modifier.fillMaxSize(0.5f))
        }
    }
}

