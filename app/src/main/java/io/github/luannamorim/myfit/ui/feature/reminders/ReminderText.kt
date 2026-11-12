package io.github.luannamorim.myfit.ui.feature.reminders

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.core.format.Dates
import io.github.luannamorim.myfit.core.format.appLocale
import io.github.luannamorim.myfit.domain.model.Reminder
import io.github.luannamorim.myfit.domain.reminder.ReminderSchedule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Clock digits and, in 12-hour format, the AM/PM marker apart: "18:30" / ("6:30", "PM"). */
internal fun timeParts(minuteOfDay: Int, is24Hour: Boolean): Pair<String, String?> {
    val time = ReminderSchedule.localTime(minuteOfDay)
    return if (is24Hour) {
        time.format(DateTimeFormatter.ofPattern("HH:mm", appLocale)) to null
    } else {
        time.format(DateTimeFormatter.ofPattern("h:mm", appLocale)) to time.format(DateTimeFormatter.ofPattern("a", appLocale))
    }
}

internal fun timeText(minuteOfDay: Int, is24Hour: Boolean): String =
    timeParts(minuteOfDay, is24Hour).let { (digits, period) -> if (period == null) digits else "$digits $period" }

/** Time of day with a smaller AM/PM marker. */
@Composable
internal fun ReminderTime(minuteOfDay: Int, is24Hour: Boolean, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    val (digits, period) = timeParts(minuteOfDay, is24Hour)
    val text = buildAnnotatedString {
        append(digits)
        if (period != null) {
            withStyle(SpanStyle(fontSize = 0.45.em, fontWeight = FontWeight.Medium)) { append(" $period") }
        }
    }
    Text(text, style = style, color = color, modifier = modifier, maxLines = 1)
}

/** "Mon", "Seg". */
internal fun dayShort(day: DayOfWeek): String = Dates.weekdayShort(day).replaceFirstChar { it.titlecase(appLocale) }

/** "Mon, Wed, Fri", "Every day", or the one-off date ("Today", "Tomorrow", "Thu, Nov 19"). */
@Composable
internal fun scheduleText(reminder: Reminder, today: LocalDate): String {
    if (reminder.days.size == DayOfWeek.entries.size) return stringResource(R.string.reminders_every_day)
    if (reminder.isRepeating) return DayOfWeek.entries.filter { it in reminder.days }.joinToString(", ") { dayShort(it) }
    val date = reminder.dateEpochDay?.let(LocalDate::ofEpochDay) ?: return ""
    return when (date) {
        today -> stringResource(R.string.reminders_today)
        today.plusDays(1) -> stringResource(R.string.reminders_tomorrow)
        else -> if (date.year == today.year) Dates.weekdayDayMonth(date) else Dates.medium(date)
    }
}
