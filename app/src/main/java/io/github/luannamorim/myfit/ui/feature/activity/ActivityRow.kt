package io.github.luannamorim.myfit.ui.feature.activity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.core.format.Dates
import io.github.luannamorim.myfit.core.format.Durations
import io.github.luannamorim.myfit.domain.activity.Activity
import io.github.luannamorim.myfit.domain.activity.DistanceUnit
import io.github.luannamorim.myfit.ui.common.SportBadge
import io.github.luannamorim.myfit.ui.common.activityTitle
import io.github.luannamorim.myfit.ui.common.labelRes
import io.github.luannamorim.myfit.ui.components.MyFitCard
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import io.github.luannamorim.myfit.ui.theme.Spacing

/** History/Home row for a logged sport: figure, name and "45 min · 1,5 km · High". */
@Composable
fun ActivityRow(activity: Activity, onClick: () -> Unit, showDate: Boolean = false) {
    val c = MyFitTheme.colors
    MyFitCard(onClick = onClick, modifier = Modifier.fillMaxWidth(), contentPadding = Spacing.m) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SportBadge(activity.sport, 40.dp)
            Spacer(Modifier.width(Spacing.m))
            Column(Modifier.weight(1f)) {
                Text(activityTitle(activity), style = MyFitTheme.type.bodyStrong, color = c.textPrimary)
                Text(activityMeta(activity, showDate), style = MyFitTheme.type.caption, color = c.textSecondary)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = c.textTertiary)
        }
    }
}

@Composable
fun activityMeta(activity: Activity, showDate: Boolean = false): String {
    val parts = buildList {
        if (showDate) add(Dates.weekdayDayMonth(activity.date))
        add(Durations.human(activity.durationMs))
        activity.distanceKm?.let { km ->
            add(
                when (activity.sport.distance) {
                    DistanceUnit.METERS -> "${ActivityEditorViewModel.formatDistance(activity.sport, km)} ${stringResource(R.string.unit_m)}"
                    else -> "${ActivityEditorViewModel.formatDistance(activity.sport, km)} ${stringResource(R.string.unit_km)}"
                },
            )
        }
        activity.intensity?.let { add(stringResource(it.labelRes)) }
    }
    return parts.joinToString(" · ")
}
