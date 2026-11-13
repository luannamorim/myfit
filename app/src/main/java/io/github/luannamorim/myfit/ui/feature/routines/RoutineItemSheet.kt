package io.github.luannamorim.myfit.ui.feature.routines

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.core.format.DecimalInput
import io.github.luannamorim.myfit.core.format.Durations
import io.github.luannamorim.myfit.domain.model.ExerciseMeasure
import io.github.luannamorim.myfit.domain.model.TimedSets
import io.github.luannamorim.myfit.ui.components.MyFitTextField
import io.github.luannamorim.myfit.ui.components.NumberStepper
import io.github.luannamorim.myfit.ui.components.PrimaryButton
import io.github.luannamorim.myfit.ui.components.SecondaryButton
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import io.github.luannamorim.myfit.ui.theme.Spacing

const val LOAD_STEP_KG = 2.5
const val REST_STEP_SECONDS = 15

/** Bottom sheet to edit the targets of one exercise in a routine. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineItemSheet(
    item: DraftItem,
    onChange: ((DraftItem) -> DraftItem) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = MyFitTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var loadText by remember(item.key) { mutableStateOf(DecimalInput.format(item.loadKg)) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = c.surface) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Spacing.l),
        ) {
            Text(item.exercise.name, style = MyFitTheme.type.headline, color = c.textPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m)) {
                NumberStepper(
                    value = item.sets.toString(),
                    label = stringResource(R.string.item_sets),
                    onDecrement = { onChange { it.copy(sets = it.sets - 1) } },
                    onIncrement = { onChange { it.copy(sets = it.sets + 1) } },
                    modifier = Modifier.weight(1f),
                )
                if (item.exercise.measure == ExerciseMeasure.TIME) {
                    NumberStepper(
                        value = Durations.setTime(item.reps),
                        label = stringResource(R.string.item_time),
                        onDecrement = { onChange { it.copy(reps = TimedSets.step(it.reps, up = false)) } },
                        onIncrement = { onChange { it.copy(reps = TimedSets.step(it.reps, up = true)) } },
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    NumberStepper(
                        value = item.reps.toString(),
                        label = stringResource(R.string.item_reps),
                        onDecrement = { onChange { it.copy(reps = it.reps - 1) } },
                        onIncrement = { onChange { it.copy(reps = it.reps + 1) } },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            NumberStepper(
                value = Durations.rest(item.restSeconds),
                label = stringResource(R.string.item_rest),
                onDecrement = { onChange { it.copy(restSeconds = it.restSeconds - REST_STEP_SECONDS) } },
                onIncrement = { onChange { it.copy(restSeconds = it.restSeconds + REST_STEP_SECONDS) } },
            )
            NumberStepper(
                value = item.loadKg?.let { DecimalInput.format(it) } ?: "—",
                unit = item.loadKg?.let { stringResource(R.string.unit_kg) },
                label = stringResource(R.string.item_load),
                onDecrement = {
                    onChange { it.copy(loadKg = it.loadKg?.minus(LOAD_STEP_KG)?.takeIf { v -> v > 0 }) }
                    loadText = DecimalInput.format(item.loadKg?.minus(LOAD_STEP_KG)?.takeIf { v -> v > 0 })
                },
                onIncrement = {
                    val next = (item.loadKg ?: 0.0) + LOAD_STEP_KG
                    onChange { it.copy(loadKg = next) }
                    loadText = DecimalInput.format(next)
                },
            )
            MyFitTextField(
                value = loadText,
                onValueChange = { text ->
                    loadText = text
                    // Invalid text keeps the previous value; blank clears the load.
                    if (text.isBlank()) onChange { it.copy(loadKg = null) }
                    else DecimalInput.parse(text)?.let { v -> onChange { it.copy(loadKg = v) } }
                },
                label = stringResource(R.string.item_load),
                placeholder = stringResource(R.string.item_load_hint),
                keyboardType = KeyboardType.Decimal,
                suffix = stringResource(R.string.unit_kg),
                isError = loadText.isNotBlank() && DecimalInput.parse(loadText) == null,
            )
            MyFitTextField(
                value = item.notes.orEmpty(),
                onValueChange = { text -> onChange { it.copy(notes = text) } },
                label = stringResource(R.string.item_notes),
                singleLine = false,
                minLines = 2,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.m), modifier = Modifier.padding(bottom = Spacing.l)) {
                SecondaryButton(
                    stringResource(R.string.action_remove),
                    onClick = onRemove,
                    icon = Icons.Rounded.Delete,
                    contentColor = c.danger,
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton(stringResource(R.string.item_done), onClick = onDismiss, modifier = Modifier.weight(1f))
            }
        }
    }
}
