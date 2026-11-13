package io.github.luannamorim.myfit.ui.feature.history

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.core.format.Dates
import io.github.luannamorim.myfit.core.format.DecimalInput
import io.github.luannamorim.myfit.core.format.Durations
import io.github.luannamorim.myfit.domain.history.SessionDetail
import io.github.luannamorim.myfit.domain.model.ExerciseMeasure
import io.github.luannamorim.myfit.domain.model.ItemStatus
import io.github.luannamorim.myfit.ui.components.ConfirmDialog
import io.github.luannamorim.myfit.ui.components.ContentMaxWidth
import io.github.luannamorim.myfit.domain.model.Intensity
import io.github.luannamorim.myfit.ui.common.IntensityPicker
import io.github.luannamorim.myfit.ui.common.PhotoField
import io.github.luannamorim.myfit.ui.components.MyFitCard
import io.github.luannamorim.myfit.ui.components.MyFitTopBar
import io.github.luannamorim.myfit.ui.components.StatTile
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import io.github.luannamorim.myfit.ui.theme.Spacing

@Composable
fun SessionDetailScreen(onBack: () -> Unit, viewModel: SessionDetailViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.deleted, state.loading, state.detail) {
        if (state.deleted || (!state.loading && state.detail == null)) onBack()
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) viewModel.setPhoto(uri)
    }
    SessionDetailContent(
        state.detail,
        onBack,
        viewModel::delete,
        onIntensity = viewModel::setIntensity,
        onPickPhoto = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onRemovePhoto = viewModel::removePhoto,
        photoError = state.photoError,
    )
}

@Composable
fun SessionDetailContent(
    detail: SessionDetail?,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onIntensity: (Intensity?) -> Unit = {},
    onPickPhoto: () -> Unit = {},
    onRemovePhoto: () -> Unit = {},
    photoError: Boolean = false,
) {
    val c = MyFitTheme.colors
    var confirm by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = c.background,
        topBar = {
            MyFitTopBar(
                title = detail?.summary?.routineName ?: stringResource(R.string.detail_title),
                onBack = onBack,
                actions = {
                    IconButton(onClick = { confirm = true }) {
                        Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.action_delete))
                    }
                },
            )
        },
    ) { padding ->
        if (detail == null) return@Scaffold
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screen),
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                val s = detail.summary
                Text(Dates.long(s.startDate), style = MyFitTheme.type.title, color = c.textPrimary)
                Text(stringResource(R.string.detail_started_at, Dates.time(s.startTime)), style = MyFitTheme.type.body, color = c.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                    StatTile(stringResource(R.string.summary_duration), Durations.clock(s.durationMs), Modifier.weight(1f))
                    StatTile(stringResource(R.string.summary_sets), s.setsDone.toString(), Modifier.weight(1f))
                }
                IntensityPicker(s.intensity, onIntensity)
                PhotoField(s.imagePath, onPick = onPickPhoto, onRemove = onRemovePhoto, error = photoError)
                detail.exercises.forEach { ex ->
                    MyFitCard(Modifier.fillMaxWidth()) {
                        Text(ex.name, style = MyFitTheme.type.bodyStrong, color = if (ex.status == ItemStatus.SKIPPED) c.textTertiary else c.textPrimary)
                        ex.sets.forEachIndexed { i, set ->
                            Row(Modifier.fillMaxWidth().padding(top = Spacing.s), verticalAlignment = Alignment.CenterVertically) {
                                Text("${i + 1}", style = MyFitTheme.type.numeric, color = c.textTertiary, modifier = Modifier.width(28.dp))
                                when (set.status) {
                                    ItemStatus.DONE -> {
                                        val load = set.actualLoadKg?.let { " × ${DecimalInput.format(it)} kg" }.orEmpty()
                                        val text = if (ex.measure == ExerciseMeasure.TIME) {
                                            Durations.setTime(set.actualReps ?: 0) + load
                                        } else {
                                            stringResource(R.string.detail_set_done, set.actualReps ?: 0, load)
                                        }
                                        Text(text, style = MyFitTheme.type.numeric, color = c.textPrimary)
                                    }
                                    ItemStatus.SKIPPED -> Text(stringResource(R.string.detail_set_skipped), style = MyFitTheme.type.body, color = c.textTertiary)
                                    ItemStatus.PENDING -> Text(stringResource(R.string.detail_set_not_done), style = MyFitTheme.type.body, color = c.textTertiary)
                                }
                            }
                        }
                    }
                }
                Box(Modifier.padding(bottom = Spacing.xxl))
            }
        }
    }
    if (confirm) {
        ConfirmDialog(
            title = stringResource(R.string.detail_delete_title),
            message = stringResource(R.string.detail_delete_message),
            confirmText = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = { confirm = false; onDelete() },
            onDismiss = { confirm = false },
        )
    }
}
