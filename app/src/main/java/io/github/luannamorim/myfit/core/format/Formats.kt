package io.github.luannamorim.myfit.core.format

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

val PtBr: Locale = Locale.forLanguageTag("pt-BR")

/** Parses/format decimals the Brazilian way ("42,5"), also accepting "42.5". */
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
        else rounded.toString().trimEnd('0').trimEnd('.').replace('.', ',')
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
    private val dayMonth = DateTimeFormatter.ofPattern("d 'de' MMMM", PtBr)
    private val dayMonthYear = DateTimeFormatter.ofPattern("d 'de' MMM 'de' yyyy", PtBr)
    private val short = DateTimeFormatter.ofPattern("dd/MM/yyyy", PtBr)

    /** "quinta-feira, 12 de novembro" */
    fun long(date: LocalDate): String =
        "${date.dayOfWeek.getDisplayName(TextStyle.FULL, PtBr)}, ${date.format(dayMonth)}"

    /** "12 de nov. de 2026" */
    fun medium(date: LocalDate): String = date.format(dayMonthYear)

    fun short(date: LocalDate): String = date.format(short)

    /** "Novembro 2026" */
    fun monthYear(year: Int, month: Int): String {
        val name = java.time.Month.of(month).getDisplayName(TextStyle.FULL_STANDALONE, PtBr)
        return "${name.replaceFirstChar { it.titlecase(PtBr) }} $year"
    }
}

/** Whole number with Brazilian thousands separator: 4870.0 -> "4.870". */
fun formatThousands(value: Double): String = String.format(PtBr, "%,.0f", value)

/** Weight with unit: "42,5 kg". */
fun formatKg(value: Double?): String = if (value == null) "—" else "${DecimalInput.format(value)} kg"
