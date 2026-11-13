package io.github.luannamorim.myfit.ui.feature.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.core.format.DecimalInput
import io.github.luannamorim.myfit.core.format.Durations
import io.github.luannamorim.myfit.domain.activity.Sport
import io.github.luannamorim.myfit.domain.activity.LiveActivity
import io.github.luannamorim.myfit.platform.activity.GpsStatus
import io.github.luannamorim.myfit.ui.common.SportFigure
import io.github.luannamorim.myfit.ui.common.labelRes
import io.github.luannamorim.myfit.ui.components.ConfirmDialog
import io.github.luannamorim.myfit.ui.components.ContentMaxWidth
import io.github.luannamorim.myfit.ui.components.MyFitTopBar
import io.github.luannamorim.myfit.ui.components.PrimaryButton
import io.github.luannamorim.myfit.ui.components.SecondaryButton
import io.github.luannamorim.myfit.ui.components.TextAction
import io.github.luannamorim.myfit.ui.theme.MyFitShapes
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import io.github.luannamorim.myfit.ui.theme.Spacing
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun LiveActivityScreen(onClose: () -> Unit, onFinished: (Long) -> Unit, viewModel: LiveActivityViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.finishedId, state.loading, state.running) {
        val id = state.finishedId
        when {
            id != null -> onFinished(id)
            !state.loading && !state.running -> onClose()
        }
    }
    LiveActivityContent(state, onBack = onClose, onTogglePause = viewModel::togglePause, onFinish = viewModel::finish, onDiscard = viewModel::discard)
}

@Composable
fun LiveActivityContent(
    state: LiveActivityUiState,
    onBack: () -> Unit,
    onTogglePause: () -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
) {
    val c = MyFitTheme.colors
    var confirmDiscard by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = c.background,
        topBar = { MyFitTopBar(title = state.name ?: stringResource(state.sport.labelRes), onBack = onBack) },
    ) { padding ->
        if (!state.running) return@Scaffold
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screen),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                SportFigure(state.sport, Modifier.fillMaxWidth().height(140.dp))
                Text(
                    stringResource(if (state.paused) R.string.live_paused else R.string.live_running).uppercase(),
                    style = MyFitTheme.type.overline,
                    color = if (state.paused) c.textTertiary else c.accent,
                )
                Text(
                    Durations.clock(state.elapsedMs),
                    style = MyFitTheme.type.numericHero,
                    color = if (state.paused) c.textSecondary else c.textPrimary,
                )
                if (state.gps) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xl)) {
                        Metric(DecimalInput.format(state.distanceMeters / 1000.0), stringResource(R.string.unit_km))
                        paceOrSpeed(state.sport, state.distanceMeters, state.elapsedMs)?.let { (value, unit) -> Metric(value, unit) }
                    }
                    gpsHint(state.gpsStatus)?.let {
                        Text(it, style = MyFitTheme.type.caption, color = c.textTertiary, textAlign = TextAlign.Center)
                    }
                }
                Row(Modifier.padding(top = Spacing.m), horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                    SecondaryButton(
                        stringResource(if (state.paused) R.string.live_resume else R.string.live_pause),
                        onClick = onTogglePause,
                        icon = if (state.paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                        modifier = Modifier.weight(1f),
                    )
                    PrimaryButton(stringResource(R.string.live_finish), onClick = onFinish, icon = Icons.Rounded.Check, modifier = Modifier.weight(1f))
                }
                TextAction(stringResource(R.string.live_discard), onClick = { confirmDiscard = true }, color = c.danger)
                Box(Modifier.padding(bottom = Spacing.xxl))
            }
        }
    }
    if (confirmDiscard) {
        ConfirmDialog(
            title = stringResource(R.string.live_discard_title),
            message = stringResource(R.string.live_discard_message),
            confirmText = stringResource(R.string.live_discard),
            destructive = true,
            onConfirm = { confirmDiscard = false; onDiscard() },
            onDismiss = { confirmDiscard = false },
        )
    }
}

@Composable
private fun Metric(value: String, unit: String) {
    val c = MyFitTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MyFitTheme.type.numericLarge, color = c.textPrimary)
        Text(unit, style = MyFitTheme.type.caption, color = c.textSecondary)
    }
}

/** Pace (min/km) for running and walking, speed (km/h) for the rest; null until there is distance. */
@Composable
private fun paceOrSpeed(sport: Sport, meters: Double, elapsedMs: Long): Pair<String, String>? {
    if (meters < 50 || elapsedMs <= 0) return null
    return if (sport == Sport.RUNNING || sport == Sport.WALKING) {
        val secondsPerKm = (elapsedMs / 1000.0) / (meters / 1000.0)
        val total = secondsPerKm.roundToInt()
        String.format(Locale.ROOT, "%d:%02d", total / 60, total % 60) to stringResource(R.string.live_pace_unit)
    } else {
        DecimalInput.format((meters / 1000.0) / (elapsedMs / 3_600_000.0)) to stringResource(R.string.live_speed_unit)
    }
}

@Composable
private fun gpsHint(status: GpsStatus): String? = when (status) {
    GpsStatus.SEARCHING -> stringResource(R.string.live_gps_searching)
    GpsStatus.DISABLED -> stringResource(R.string.live_gps_disabled)
    GpsStatus.OFF -> stringResource(R.string.live_gps_off)
    GpsStatus.OK -> null
}

/** Shown on Home while a sport is being timed; tapping returns to it. */
@Composable
fun LiveActivityBar(activity: LiveActivity, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val c = MyFitTheme.colors
    Surface(onClick = onOpen, modifier = modifier.fillMaxWidth(), shape = MyFitShapes.medium, color = c.textPrimary, contentColor = c.background) {
        Row(
            Modifier.padding(horizontal = Spacing.l, vertical = Spacing.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.m),
        ) {
            Box(Modifier.size(10.dp).clip(MyFitShapes.pill).background(if (activity.isPaused) c.textTertiary else c.accent))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.live_bar_title, activity.name ?: stringResource(activity.sport.labelRes)),
                    style = MyFitTheme.type.label,
                    maxLines = 1,
                )
                Text(
                    stringResource(if (activity.isPaused) R.string.live_paused else R.string.live_running),
                    style = MyFitTheme.type.caption,
                    color = c.background.copy(alpha = 0.7f),
                )
            }
            Text(stringResource(R.string.active_bar_resume), style = MyFitTheme.type.bodyStrong)
        }
    }
}

