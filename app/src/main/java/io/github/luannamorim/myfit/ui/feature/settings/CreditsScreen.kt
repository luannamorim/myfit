package io.github.luannamorim.myfit.ui.feature.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.data.db.seed.ExerciseSeed
import io.github.luannamorim.myfit.ui.common.ArtSource
import io.github.luannamorim.myfit.ui.common.ExerciseArt
import io.github.luannamorim.myfit.ui.common.ExerciseThumbnail
import io.github.luannamorim.myfit.ui.components.ContentMaxWidth
import io.github.luannamorim.myfit.ui.components.MyFitCard
import io.github.luannamorim.myfit.ui.components.MyFitTopBar
import io.github.luannamorim.myfit.ui.components.SectionHeader
import io.github.luannamorim.myfit.ui.components.TextAction
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import io.github.luannamorim.myfit.ui.theme.Spacing

private const val EVERKINETIC_URL = "https://github.com/everkinetic/data"
private const val WORKOUT_GUIDE_URL = "https://github.com/bryllim/workout-guide"
private const val BY_SA_4_URL = "https://creativecommons.org/licenses/by-sa/4.0/"

private data class CreditRow(val name: String, val art: String, val source: ArtSource, val seedKey: String)

/** Attribution for the bundled exercise drawings (CC BY-SA requires author, license, source and changes). */
@Composable
fun CreditsScreen(onBack: () -> Unit) {
    val c = MyFitTheme.colors
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val rows = remember(locale) {
        ExerciseSeed.items.mapNotNull { item ->
            item.art?.let { CreditRow(item.name(locale), it, ExerciseArt.source(it), item.key) }
        }.sortedBy { it.name.lowercase(locale) }
    }
    Scaffold(
        containerColor = c.background,
        topBar = { MyFitTopBar(title = stringResource(R.string.credits_title), onBack = onBack) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = Spacing.screen, vertical = Spacing.s),
                verticalArrangement = Arrangement.spacedBy(Spacing.m),
            ) {
                item {
                    Text(stringResource(R.string.credits_intro), style = MyFitTheme.type.body, color = c.textSecondary)
                }
                item {
                    SourceCard("Everkinetic", stringResource(R.string.credits_everkinetic)) { url -> open(context, url) }
                }
                item {
                    SourceCard("Workout Guide", stringResource(R.string.credits_workout_guide), sourceUrl = WORKOUT_GUIDE_URL) { url -> open(context, url) }
                }
                item {
                    Text(stringResource(R.string.credits_changes), style = MyFitTheme.type.caption, color = c.textTertiary)
                }
                item {
                    SectionHeader(stringResource(R.string.credits_exercises, rows.size), Modifier.padding(top = Spacing.s))
                }
                items(rows, key = { it.seedKey }) { row ->
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ExerciseThumbnail(null, row.seedKey, ExerciseSeed.forKey(row.seedKey)!!.muscleGroup, Modifier.size(36.dp))
                            Spacer(Modifier.width(Spacing.m))
                            Column(Modifier.weight(1f)) {
                                Text(row.name, style = MyFitTheme.type.body, color = c.textPrimary)
                                Text(
                                    when (row.source) {
                                        ArtSource.EVERKINETIC -> "Everkinetic · ${row.art}"
                                        ArtSource.WORKOUT_GUIDE -> "Workout Guide · ${row.art.removePrefix("wg-")}"
                                    },
                                    style = MyFitTheme.type.caption,
                                    color = c.textTertiary,
                                )
                            }
                        }
                        HorizontalDivider(Modifier.padding(top = Spacing.m), color = c.outline)
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceCard(title: String, body: String, sourceUrl: String = EVERKINETIC_URL, onOpen: (String) -> Unit) {
    val c = MyFitTheme.colors
    MyFitCard(Modifier.fillMaxWidth()) {
        Text(title, style = MyFitTheme.type.bodyStrong, color = c.textPrimary)
        Text(body, style = MyFitTheme.type.caption, color = c.textSecondary)
        Row {
            TextAction(stringResource(R.string.credits_open_source), onClick = { onOpen(sourceUrl) })
            TextAction(stringResource(R.string.credits_open_license), onClick = { onOpen(BY_SA_4_URL) })
        }
    }
}

private fun open(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        // No browser: the text above already names the author and the license.
    }
}
