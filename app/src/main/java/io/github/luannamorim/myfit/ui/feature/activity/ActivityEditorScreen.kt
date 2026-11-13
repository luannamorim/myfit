package io.github.luannamorim.myfit.ui.feature.activity

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.core.format.Durations
import io.github.luannamorim.myfit.domain.activity.DistanceUnit
import io.github.luannamorim.myfit.domain.activity.Sport
import io.github.luannamorim.myfit.domain.model.Intensity
import io.github.luannamorim.myfit.ui.common.IntensityPicker
import io.github.luannamorim.myfit.ui.common.PhotoField
import io.github.luannamorim.myfit.ui.common.SportFigure
import io.github.luannamorim.myfit.ui.common.icon
import io.github.luannamorim.myfit.ui.common.labelRes
import io.github.luannamorim.myfit.ui.components.ConfirmDialog
import io.github.luannamorim.myfit.ui.components.ContentMaxWidth
import io.github.luannamorim.myfit.ui.components.DateField
import io.github.luannamorim.myfit.ui.components.MyFitCard
import io.github.luannamorim.myfit.ui.components.MyFitTextField
import io.github.luannamorim.myfit.ui.components.MyFitTopBar
import io.github.luannamorim.myfit.ui.components.PrimaryButton
import io.github.luannamorim.myfit.ui.components.SecondaryButton
import io.github.luannamorim.myfit.ui.components.StatTile
import io.github.luannamorim.myfit.ui.components.TextAction
import io.github.luannamorim.myfit.ui.theme.MyFitShapes
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import io.github.luannamorim.myfit.ui.theme.Spacing
import java.time.LocalDate

@Composable
fun ActivityEditorScreen(onClose: () -> Unit, onStarted: () -> Unit = {}, viewModel: ActivityEditorViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(state.done) { if (state.done) onClose() }
    LaunchedEffect(state.started) { if (state.started) onStarted() }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) viewModel.setPhoto(uri)
    }
    fun granted(permission: String) = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    // Notifications (the ongoing timer) and, for GPS distance, location while the app is in use.
    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        viewModel.startLive(locationGranted = granted(Manifest.permission.ACCESS_FINE_LOCATION))
    }
    val startNow = {
        if (viewModel.canStart()) {
            val needed = buildList {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !granted(Manifest.permission.POST_NOTIFICATIONS)) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
                if (state.gps && state.sport.gpsCapable && !granted(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    add(Manifest.permission.ACCESS_FINE_LOCATION)
                    add(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
            }
            if (needed.isEmpty()) viewModel.startLive(granted(Manifest.permission.ACCESS_FINE_LOCATION)) else permissions.launch(needed.toTypedArray())
        }
    }
    ActivityEditorContent(
        state = state,
        actions = ActivityEditorActions(
            onBack = onClose,
            onSave = { viewModel.save() },
            onDelete = viewModel::delete,
            onSport = viewModel::setSport,
            onName = viewModel::setName,
            onDate = viewModel::setDate,
            onDuration = viewModel::setDuration,
            onDistance = viewModel::setDistance,
            onIntensity = viewModel::setIntensity,
            onNotes = viewModel::setNotes,
            onPickPhoto = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            onRemovePhoto = viewModel::removePhoto,
            onGps = viewModel::setGps,
            onStartNow = startNow,
        ),
    )
}

data class ActivityEditorActions(
    val onBack: () -> Unit = {},
    val onSave: () -> Unit = {},
    val onDelete: () -> Unit = {},
    val onSport: (Sport) -> Unit = {},
    val onName: (String) -> Unit = {},
    val onDate: (LocalDate) -> Unit = {},
    val onDuration: (String) -> Unit = {},
    val onDistance: (String) -> Unit = {},
    val onIntensity: (Intensity?) -> Unit = {},
    val onNotes: (String) -> Unit = {},
    val onPickPhoto: () -> Unit = {},
    val onRemovePhoto: () -> Unit = {},
    val onGps: (Boolean) -> Unit = {},
    val onStartNow: () -> Unit = {},
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActivityEditorContent(state: ActivityEditorUiState, actions: ActivityEditorActions) {
    val c = MyFitTheme.colors
    var confirmDelete by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = c.background,
        topBar = {
            val title = when {
                state.review -> R.string.activity_title_done
                state.isNew -> R.string.activity_title_new
                else -> R.string.activity_title_edit
            }
            MyFitTopBar(
                title = stringResource(title),
                onBack = actions.onBack,
                actions = {
                    if (!state.review) TextAction(stringResource(R.string.action_save), onClick = actions.onSave, enabled = !state.loading)
                },
            )
        },
        bottomBar = {
            if (state.review && !state.loading) {
                Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(Spacing.l), contentAlignment = Alignment.Center) {
                    PrimaryButton(stringResource(R.string.action_save), onClick = actions.onSave, modifier = Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth())
                }
            }
        },
    ) { padding ->
        if (state.loading) return@Scaffold
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screen),
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                SportFigure(state.sport, Modifier.fillMaxWidth().height(150.dp))
                if (state.isNew) {
                    Text(stringResource(R.string.activity_sport), style = MyFitTheme.type.caption, color = c.textSecondary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.s), verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        Sport.entries.forEach { sport -> SportChip(sport, selected = sport == state.sport, onClick = { actions.onSport(sport) }) }
                    }
                } else {
                    // Logged: the sport is fixed (only a free activity's name can be fixed).
                    Text(
                        if (state.sport == Sport.OTHER) state.name.ifBlank { stringResource(R.string.sport_other) } else stringResource(state.sport.labelRes),
                        style = MyFitTheme.type.title,
                        color = c.textPrimary,
                    )
                }
                if (state.review) {
                    ReviewStats(state)
                } else if (state.sport == Sport.OTHER) {
                    MyFitTextField(
                        value = state.name,
                        onValueChange = actions.onName,
                        label = stringResource(R.string.activity_name),
                        isError = state.nameError,
                        supportingText = if (state.nameError) stringResource(R.string.activity_name_error) else null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (state.isNew) {
                    StartNowCard(state, actions)
                    Text(stringResource(R.string.activity_or_log), style = MyFitTheme.type.caption, color = c.textSecondary)
                }
                if (!state.review) {
                    DateField(stringResource(R.string.activity_date), state.date, actions.onDate, Modifier.fillMaxWidth(), maxDate = state.today)
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                        MyFitTextField(
                            value = state.duration,
                            onValueChange = actions.onDuration,
                            label = stringResource(R.string.activity_duration),
                            keyboardType = KeyboardType.Number,
                            suffix = stringResource(R.string.unit_min),
                            isError = state.durationError,
                            modifier = Modifier.weight(1f),
                        )
                        val unit = state.sport.distance
                        if (unit != null) {
                            MyFitTextField(
                                value = state.distance,
                                onValueChange = actions.onDistance,
                                label = stringResource(R.string.activity_distance),
                                keyboardType = if (unit == DistanceUnit.METERS) KeyboardType.Number else KeyboardType.Decimal,
                                suffix = stringResource(if (unit == DistanceUnit.METERS) R.string.unit_m else R.string.unit_km),
                                isError = state.distanceError,
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            Box(Modifier.weight(1f))
                        }
                    }
                    if (state.durationError) {
                        Text(stringResource(R.string.activity_duration_error), style = MyFitTheme.type.caption, color = c.danger)
                    }
                }
                IntensityPicker(state.intensity, actions.onIntensity)
                MyFitTextField(
                    value = state.notes,
                    onValueChange = actions.onNotes,
                    label = stringResource(R.string.activity_notes),
                    singleLine = false,
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                PhotoField(state.imagePath, onPick = actions.onPickPhoto, onRemove = actions.onRemovePhoto, error = state.photoError)
                if (!state.isNew && !state.review) {
                    SecondaryButton(
                        stringResource(R.string.action_delete),
                        onClick = { confirmDelete = true },
                        icon = Icons.Rounded.Delete,
                        contentColor = c.danger,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Box(Modifier.padding(bottom = Spacing.xxl))
            }
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.activity_delete_title),
            message = stringResource(R.string.activity_delete_message),
            confirmText = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = { confirmDelete = false; actions.onDelete() },
            onDismiss = { confirmDelete = false },
        )
    }
}

/** Chip with the sport's icon, styled like [io.github.luannamorim.myfit.ui.components.MyFitChip]. */
@Composable
private fun SportChip(sport: Sport, selected: Boolean, onClick: () -> Unit) {
    val c = MyFitTheme.colors
    Surface(
        onClick = onClick,
        modifier = Modifier.semantics { this.selected = selected },
        shape = MyFitShapes.pill,
        color = if (selected) c.textPrimary else c.surface,
        contentColor = if (selected) c.background else c.textSecondary,
        border = BorderStroke(1.dp, if (selected) c.textPrimary else c.outline),
    ) {
        Row(Modifier.padding(start = 10.dp, end = 14.dp, top = 7.dp, bottom = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(sport.icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(sport.labelRes), style = MyFitTheme.type.label, modifier = Modifier.padding(start = 6.dp))
        }
    }
}

/** "Doing it now?": start a live timer (with GPS distance for outdoor sports) instead of logging after. */
@Composable
private fun StartNowCard(state: ActivityEditorUiState, actions: ActivityEditorActions) {
    val c = MyFitTheme.colors
    MyFitCard(Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.activity_now_title), style = MyFitTheme.type.bodyStrong, color = c.textPrimary)
        Text(stringResource(R.string.activity_now_hint), style = MyFitTheme.type.caption, color = c.textSecondary)
        if (state.sport.gpsCapable) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .toggleable(value = state.gps, role = Role.Switch, onValueChange = actions.onGps)
                    .padding(top = Spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(20.dp))
                Text(
                    stringResource(R.string.activity_now_gps),
                    style = MyFitTheme.type.body,
                    color = c.textPrimary,
                    modifier = Modifier.weight(1f).padding(start = Spacing.s),
                )
                Switch(
                    checked = state.gps,
                    onCheckedChange = null,
                    colors = SwitchDefaults.colors(checkedTrackColor = c.accent, checkedThumbColor = c.onAccent),
                )
            }
        }
        Spacer(Modifier.height(Spacing.m))
        PrimaryButton(stringResource(R.string.activity_now_start), onClick = actions.onStartNow, icon = Icons.Rounded.PlayArrow, modifier = Modifier.fillMaxWidth())
    }
}

/** What the timer measured, shown read-only on the "activity done" step. */
@Composable
private fun ReviewStats(state: ActivityEditorUiState) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
        StatTile(
            stringResource(R.string.activity_duration),
            Durations.human((state.duration.toIntOrNull() ?: 0) * 60_000L),
            Modifier.weight(1f),
        )
        val unit = state.sport.distance
        if (unit != null && state.distance.isNotBlank()) {
            StatTile(
                stringResource(R.string.activity_distance),
                state.distance,
                Modifier.weight(1f),
                unit = stringResource(if (unit == DistanceUnit.METERS) R.string.unit_m else R.string.unit_km),
            )
        } else {
            Box(Modifier.weight(1f))
        }
    }
}

