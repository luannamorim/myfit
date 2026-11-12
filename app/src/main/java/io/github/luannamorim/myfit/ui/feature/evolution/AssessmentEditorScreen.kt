package io.github.luannamorim.myfit.ui.feature.evolution

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.core.format.Dates
import io.github.luannamorim.myfit.core.format.DecimalInput
import io.github.luannamorim.myfit.domain.body.Measure
import io.github.luannamorim.myfit.ui.common.labelRes
import io.github.luannamorim.myfit.ui.components.ConfirmDialog
import io.github.luannamorim.myfit.ui.components.ContentMaxWidth
import io.github.luannamorim.myfit.ui.components.MyFitTextField
import io.github.luannamorim.myfit.ui.components.MyFitTopBar
import io.github.luannamorim.myfit.ui.components.SecondaryButton
import io.github.luannamorim.myfit.ui.components.SectionHeader
import io.github.luannamorim.myfit.ui.components.TextAction
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import io.github.luannamorim.myfit.ui.theme.Spacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val Groups: List<Pair<Int, List<Measure>>> = listOf(
    R.string.assessment_group_general to listOf(Measure.WEIGHT, Measure.BODY_FAT),
    R.string.assessment_group_trunk to listOf(Measure.NECK, Measure.SHOULDERS, Measure.CHEST, Measure.WAIST, Measure.ABDOMEN, Measure.HIPS),
    R.string.assessment_group_arms to listOf(Measure.ARM_RIGHT, Measure.ARM_LEFT, Measure.FOREARM_RIGHT, Measure.FOREARM_LEFT),
    R.string.assessment_group_legs to listOf(Measure.THIGH_RIGHT, Measure.THIGH_LEFT, Measure.CALF_RIGHT, Measure.CALF_LEFT),
)

@Composable
fun AssessmentEditorScreen(onClose: () -> Unit, viewModel: AssessmentEditorViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onClose() }
    AssessmentEditorContent(state, viewModel::setDate, viewModel::setField, viewModel::setNotes, { viewModel.save() }, viewModel::delete, onClose)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssessmentEditorContent(
    state: AssessmentEditorUiState,
    onDate: (LocalDate) -> Unit,
    onField: (Measure, String) -> Unit,
    onNotes: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
) {
    val c = MyFitTheme.colors
    var pickDate by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    Scaffold(
        containerColor = c.background,
        topBar = {
            MyFitTopBar(
                title = stringResource(if (state.isNew) R.string.assessment_new else R.string.assessment_edit),
                onBack = onBack,
                actions = { TextAction(stringResource(R.string.action_save), onClick = onSave, enabled = !state.loading) },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = Spacing.screen),
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                SecondaryButton(
                    stringResource(R.string.assessment_date) + ": " + Dates.medium(state.date),
                    onClick = { pickDate = true },
                    icon = Icons.Rounded.CalendarMonth,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (state.emptyError) Text(stringResource(R.string.assessment_empty_error), style = MyFitTheme.type.caption, color = c.danger)
                Groups.forEach { (title, measures) ->
                    SectionHeader(stringResource(title), Modifier.padding(top = Spacing.s))
                    measures.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                            pair.forEach { m ->
                                MyFitTextField(
                                    value = state.fields[m].orEmpty(),
                                    onValueChange = { onField(m, it) },
                                    label = stringResource(m.labelRes),
                                    placeholder = state.lastValues[m]?.let { stringResource(R.string.assessment_last_hint, DecimalInput.format(it)) },
                                    keyboardType = KeyboardType.Decimal,
                                    suffix = unitLabel(m),
                                    isError = m in state.invalid,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (pair.size == 1) Box(Modifier.weight(1f))
                        }
                    }
                }
                MyFitTextField(
                    value = state.notes,
                    onValueChange = onNotes,
                    label = stringResource(R.string.assessment_notes),
                    singleLine = false,
                    minLines = 2,
                    modifier = Modifier.padding(top = Spacing.s),
                )
                if (!state.isNew) {
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
    if (pickDate) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextAction(stringResource(R.string.action_save), onClick = {
                    pickerState.selectedDateMillis?.let { onDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                    pickDate = false
                })
            },
            dismissButton = { TextAction(stringResource(R.string.action_cancel), onClick = { pickDate = false }, color = c.textSecondary) },
        ) { DatePicker(state = pickerState) }
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.assessment_delete_title),
            message = stringResource(R.string.assessment_delete_message),
            confirmText = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = { confirmDelete = false; onDelete() },
            onDismiss = { confirmDelete = false },
        )
    }
}
