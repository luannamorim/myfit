package io.github.luannamorim.myfit.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.domain.session.ActiveSession
import io.github.luannamorim.myfit.domain.session.SessionEngine
import io.github.luannamorim.myfit.ui.theme.MyFitShapes
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import io.github.luannamorim.myfit.ui.theme.Spacing

/** Shown above the navigation on every tab while a workout is in progress. */
@Composable
fun ActiveSessionBar(session: ActiveSession, onResume: () -> Unit, modifier: Modifier = Modifier) {
    val c = MyFitTheme.colors
    val exercise = session.currentExercise
    val detail = when {
        session.isPaused -> stringResource(R.string.notif_paused)
        SessionEngine.isComplete(session) || exercise == null -> stringResource(R.string.workout_all_done)
        else -> stringResource(R.string.notif_current, exercise.name, session.cursor.setIndex + 1, exercise.sets.size)
    }
    Surface(
        onClick = onResume,
        modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.m, vertical = Spacing.s),
        shape = MyFitShapes.medium,
        color = c.textPrimary,
        contentColor = c.background,
    ) {
        Row(
            Modifier.padding(horizontal = Spacing.l, vertical = Spacing.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            Box(Modifier.size(10.dp).clip(MyFitShapes.pill).background(c.accent))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.active_bar_title) + " · " + session.routineName, style = MyFitTheme.type.label, maxLines = 1)
                Text(detail, style = MyFitTheme.type.caption, color = c.background.copy(alpha = 0.7f), maxLines = 1)
            }
            Text(stringResource(R.string.active_bar_resume), style = MyFitTheme.type.bodyStrong)
        }
    }
}
