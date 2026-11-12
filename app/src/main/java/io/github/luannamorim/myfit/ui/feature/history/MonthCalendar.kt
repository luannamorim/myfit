package io.github.luannamorim.myfit.ui.feature.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.core.format.Dates
import io.github.luannamorim.myfit.domain.history.HistoryCalculator
import io.github.luannamorim.myfit.ui.theme.MyFitShapes
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import io.github.luannamorim.myfit.ui.theme.Spacing
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** Monday-first month grid. Trained days get an accent dot; swipe or arrows change month. */
@Composable
fun MonthCalendar(
    month: YearMonth,
    marked: Set<LocalDate>,
    selected: LocalDate?,
    today: LocalDate,
    onSelect: (LocalDate) -> Unit,
    onMonthChange: (YearMonth) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = MyFitTheme.colors
    var drag by remember { mutableFloatStateOf(0f) }
    Column(
        modifier.pointerInput(month) {
            detectHorizontalDragGestures(
                onDragEnd = {
                    if (drag > SWIPE_PX) onMonthChange(month.minusMonths(1)) else if (drag < -SWIPE_PX) onMonthChange(month.plusMonths(1))
                    drag = 0f
                },
                onHorizontalDrag = { _, amount -> drag += amount },
            )
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Dates.monthYear(month.year, month.monthValue), style = MyFitTheme.type.title, color = c.textPrimary, modifier = Modifier.weight(1f))
            IconButton(onClick = { onMonthChange(month.minusMonths(1)) }) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = stringResource(R.string.history_prev_month), tint = c.textSecondary)
            }
            IconButton(onClick = { onMonthChange(month.plusMonths(1)) }) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = stringResource(R.string.history_next_month), tint = c.textSecondary)
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = Spacing.s)) {
            DayOfWeek.entries.forEach { dow ->
                Text(
                    Dates.weekdayShort(dow).uppercase(),
                    style = MyFitTheme.type.overline,
                    color = c.textTertiary,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        val grid = HistoryCalculator.calendarGrid(month)
        var day = grid.start
        while (day <= grid.endInclusive) {
            Row(Modifier.fillMaxWidth()) {
                repeat(7) {
                    val date = day
                    DayCell(
                        date = date,
                        inMonth = YearMonth.from(date) == month,
                        isSelected = date == selected,
                        isToday = date == today,
                        hasSession = date in marked,
                        onClick = { onSelect(date) },
                        modifier = Modifier.weight(1f),
                    )
                    day = day.plusDays(1)
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    inMonth: Boolean,
    isSelected: Boolean,
    isToday: Boolean,
    hasSession: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = MyFitTheme.colors
    val description = if (hasSession) stringResource(R.string.calendar_has_workout, Dates.long(date)) else Dates.long(date)
    Box(
        modifier
            .aspectRatio(1f)
            .padding(3.dp)
            .clip(MyFitShapes.pill)
            .then(if (isSelected) Modifier.background(c.textPrimary) else Modifier)
            .then(if (isToday && !isSelected) Modifier.border(BorderStroke(1.dp, c.outlineStrong), MyFitShapes.pill) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = description
                this.selected = isSelected
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(
                date.dayOfMonth.toString(),
                style = MyFitTheme.type.numeric,
                color = when {
                    isSelected -> c.background
                    !inMonth -> c.textTertiary.copy(alpha = 0.5f)
                    else -> c.textPrimary
                },
            )
            Box(
                Modifier
                    .padding(top = 3.dp)
                    .size(5.dp)
                    .clip(MyFitShapes.pill)
                    .background(if (hasSession) (if (isSelected) c.background else c.accent) else androidx.compose.ui.graphics.Color.Transparent),
            )
        }
    }
}

private const val SWIPE_PX = 80f
