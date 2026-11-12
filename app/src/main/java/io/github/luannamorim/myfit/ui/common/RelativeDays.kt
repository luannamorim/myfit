package io.github.luannamorim.myfit.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.luannamorim.myfit.R

/** "Feito hoje", "Feito ontem", "Feito há 3 dias", "Ainda não feito". */
@Composable
fun lastDoneText(lastDoneEpochDay: Long?, todayEpochDay: Long): String {
    if (lastDoneEpochDay == null) return stringResource(R.string.relative_never)
    val days = (todayEpochDay - lastDoneEpochDay).toInt().coerceAtLeast(0)
    return when (days) {
        0 -> stringResource(R.string.relative_today)
        1 -> stringResource(R.string.relative_yesterday)
        else -> pluralStringResource(R.plurals.relative_days_ago, days, days)
    }
}
