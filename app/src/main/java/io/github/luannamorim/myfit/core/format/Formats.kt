package io.github.luannamorim.myfit.core.format

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

val PtBr: Locale = Locale.forLanguageTag("pt-BR")

/** Locale used for formatting: the device's current one. */
val appLocale: Locale get() = Locale.getDefault()

private val isPortuguese: Boolean get() = appLocale.language == "pt"

private fun decimalSeparator(): Char = java.text.DecimalFormatSymbols.getInstance(appLocale).decimalSeparator

/** Parses decimals with comma or dot ("42,5" or "42.5"); formats with the locale's separator. */
object DecimalInput {
    private val pattern = Regex("""^\d{1,4}([.,]\d{0,2})?$""")

    fun parse(text: String): Double? {
        val t = text.trim()
        if (t.isEmpty() || !pattern.matches(t)) return null
        return t.replace(',', '.').toDoubleOrNull()
    }

    fun format(value: Double?): String {
        if (value == null) return ""
        val rounded = Math.round(value * 100) / 100.0
        return if (rounded % 1.0 == 0.0) rounded.toLong().toString()
        else rounded.toString().trimEnd('0').trimEnd('.').replace('.', decimalSeparator())
    }
}

object Durations {
    /** "1:02:45" or "12:05" — clock style used for timers. */
    fun clock(ms: Long): String {
        val totalSeconds = (ms.coerceAtLeast(0) + 500) / 1000
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    /** Countdown style that never shows "0:00" before the end: rounds up. */
    fun countdown(ms: Long): String {
        val totalSeconds = (ms.coerceAtLeast(0) + 999) / 1000
        return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
    }

    /** "1 h 05 min", "45 min", "40 s". */
    fun human(ms: Long): String {
        val totalSeconds = ms.coerceAtLeast(0) / 1000
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        return when {
            h > 0 -> "%d h %02d min".format(h, m)
            m > 0 -> "$m min"
            else -> "${totalSeconds % 60} s"
        }
    }

    /** Rest label as gym-goers say it: "45 s", "90 s", "150 s", then "3 min", "3 min 30 s". */
    fun rest(seconds: Int): String {
        if (seconds < 180) return "$seconds s"
        val m = seconds / 60
        val s = seconds % 60
        return if (s == 0) "$m min" else "$m min $s s"
    }
}

object Dates {
    private fun pattern(pt: String, en: String) = DateTimeFormatter.ofPattern(if (isPortuguese) pt else en, appLocale)

    /** "quinta-feira, 12 de novembro" / "Thursday, November 12" */
    fun long(date: LocalDate): String = date.format(pattern("EEEE, d 'de' MMMM", "EEEE, MMMM d"))

    /** "12 de nov. de 2026" / "Nov 12, 2026" */
    fun medium(date: LocalDate): String = date.format(pattern("d 'de' MMM 'de' yyyy", "MMM d, yyyy"))

    /** "12/11/2026" / "11/12/26" */
    fun short(date: LocalDate): String = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(appLocale))

    /** "12 nov." / "Nov 12" */
    fun dayMonth(date: LocalDate): String = date.format(pattern("d MMM", "MMM d"))

    /** "qui., 12 nov." / "Thu, Nov 12" */
    fun weekdayDayMonth(date: LocalDate): String = date.format(pattern("EEE, d MMM", "EEE, MMM d"))

    /** Time of day in the locale's style: "18:30" / "6:30 PM". */
    fun time(time: java.time.LocalTime): String = time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(appLocale))

    /** Chart axis: "12/11" / "11/12" */
    fun axis(date: LocalDate): String = date.format(pattern("dd/MM", "MM/dd"))

    /** "Novembro 2026" / "November 2026" */
    fun monthYear(year: Int, month: Int): String {
        val name = java.time.Month.of(month).getDisplayName(TextStyle.FULL_STANDALONE, appLocale)
        return "${name.replaceFirstChar { it.titlecase(appLocale) }} $year"
    }

    fun weekdayShort(day: java.time.DayOfWeek): String = day.getDisplayName(TextStyle.SHORT, appLocale).take(3).replace(".", "")

    fun weekdayNarrow(day: java.time.DayOfWeek): String = day.getDisplayName(TextStyle.NARROW, appLocale)
}

/** Signed difference with unit, using a true minus sign: "+2 kg", "−1,5 cm", "0 cm". */
fun formatSigned(value: Double, unit: String): String {
    val abs = DecimalInput.format(kotlin.math.abs(value))
    val sign = when {
        value > 0 -> "+"
        value < 0 -> "\u2212"
        else -> ""
    }
    return "$sign$abs $unit".trim()
}

/** Whole number with the locale's thousands separator: 4870.0 -> "4.870" (pt) / "4,870" (en). */
fun formatThousands(value: Double): String = String.format(appLocale, "%,.0f", value)

/** Weight with unit: "42,5 kg". */
fun formatKg(value: Double?): String = if (value == null) "—" else "${DecimalInput.format(value)} kg"

/** One decimal with the locale's separator: 3.0 -> "3,0" (pt) / "3.0" (en). */
fun formatOneDecimal(value: Double): String = String.format(appLocale, "%.1f", value)
