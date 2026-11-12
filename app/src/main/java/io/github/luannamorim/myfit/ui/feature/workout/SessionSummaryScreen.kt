package io.github.luannamorim.myfit.ui.feature.workout

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.toggleable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.core.format.formatThousands
import io.github.luannamorim.myfit.core.format.Durations
import io.github.luannamorim.myfit.core.format.formatKg
import io.github.luannamorim.myfit.domain.model.Intensity
import io.github.luannamorim.myfit.ui.common.IntensityPicker
import io.github.luannamorim.myfit.ui.common.PhotoField
import io.github.luannamorim.myfit.ui.components.ContentMaxWidth
import io.github.luannamorim.myfit.ui.components.MyFitTopBar
import io.github.luannamorim.myfit.ui.components.PrimaryButton
import io.github.luannamorim.myfit.ui.components.SectionHeader
import io.github.luannamorim.myfit.ui.components.StatTile
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import io.github.luannamorim.myfit.ui.theme.Spacing

@Composable
fun SessionSummaryScreen(
    onBack: () -> Unit,
    onDone: (Long) -> Unit,
    viewModel: SessionSummaryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.finishedSessionId, state.loading, state.hasSession) {
        val finished = state.finishedSessionId
        if (finished != null) onDone(finished) else if (!state.loading && !state.hasSession) onBack()
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) viewModel.setPhoto(uri)
    }
    SessionSummaryContent(
        state,
        onBack,
        viewModel::setUpdateLoads,
        viewModel::confirm,
        onIntensity = viewModel::setIntensity,
        onPickPhoto = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onRemovePhoto = viewModel::removePhoto,
    )
}

@Composable
fun SessionSummaryContent(
    state: SessionSummaryUiState,
    onBack: () -> Unit,
    onUpdateLoads: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onIntensity: (Intensity?) -> Unit = {},
    onPickPhoto: () -> Unit = {},
    onRemovePhoto: () -> Unit = {},
) {
    val c = MyFitTheme.colors
    Scaffold(
        containerColor = c.background,
        topBar = { MyFitTopBar(title = stringResource(R.string.summary_title), onBack = onBack) },
        bottomBar = {
            Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(Spacing.l), contentAlignment = Alignment.Center) {
                PrimaryButton(
                    stringResource(R.string.summary_confirm),
                    onClick = onConfirm,
                    enabled = state.hasSession,
                    modifier = Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth(),
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screen),
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                Text(state.routineName, style = MyFitTheme.type.display, color = c.textPrimary)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                    StatTile(stringResource(R.string.summary_duration), Durations.clock(state.durationMs), Modifier.weight(1f))
                    StatTile(stringResource(R.string.summary_sets), "${state.setsDone}/${state.setsTotal}", Modifier.weight(1f))
                }
                StatTile(stringResource(R.string.summary_volume), formatThousands(state.volumeKg), Modifier.fillMaxWidth(), unit = stringResource(R.string.unit_kg))
                // Asked before saving: how hard it was, and an optional photo (both editable later in history).
                IntensityPicker(state.intensity, onIntensity, Modifier.padding(top = Spacing.s))
                PhotoField(state.imagePath, onPick = onPickPhoto, onRemove = onRemovePhoto, error = state.photoError)
                SectionHeader(stringResource(R.string.workout_exercises))
                state.exercises.forEach { ex ->
                    Row(Modifier.fillMaxWidth().padding(vertical = Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(ex.name, style = MyFitTheme.type.bodyStrong, color = if (ex.skipped) c.textTertiary else c.textPrimary)
                            Text(
                                if (ex.skipped) stringResource(R.string.summary_skipped) else stringResource(R.string.summary_exercise_sets, ex.done, ex.total),
                                style = MyFitTheme.type.caption,
                                color = c.textSecondary,
                            )
                        }
                        ex.bestLoadKg?.let { Text(formatKg(it), style = MyFitTheme.type.numeric, color = c.textSecondary) }
                    }
                    HorizontalDivider(color = c.outline)
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .toggleable(value = state.updateLoads, role = Role.Checkbox, onValueChange = onUpdateLoads)
                        .padding(vertical = Spacing.m),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = state.updateLoads,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(checkedColor = c.accent, checkmarkColor = c.onAccent, uncheckedColor = c.outlineStrong),
                    )
                    Text(stringResource(R.string.summary_update_loads), style = MyFitTheme.type.body, color = c.textPrimary, modifier = Modifier.padding(start = Spacing.m))
                }
            }
        }
    }
}
