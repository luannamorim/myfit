package io.github.luannamorim.myfit.ui.feature.settings

import androidx.core.net.toUri
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.luannamorim.myfit.R
import io.github.luannamorim.myfit.core.format.Durations
import io.github.luannamorim.myfit.domain.model.AppSettings
import io.github.luannamorim.myfit.domain.model.RestSound
import io.github.luannamorim.myfit.domain.model.ThemeMode
import io.github.luannamorim.myfit.ui.components.MyFitCard
import io.github.luannamorim.myfit.ui.components.MyFitChip
import io.github.luannamorim.myfit.ui.components.NumberStepper
import io.github.luannamorim.myfit.ui.components.SectionHeader
import io.github.luannamorim.myfit.ui.components.TabScreen
import io.github.luannamorim.myfit.ui.components.TextAction
import io.github.luannamorim.myfit.ui.feature.routines.REST_STEP_SECONDS
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import io.github.luannamorim.myfit.ui.theme.Spacing

@Composable
fun SettingsScreen(
    onOpenBackup: () -> Unit,
    onOpenLibrary: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshSystem()
        onPauseOrDispose { }
    }
    SettingsContent(
        state = state,
        onUpdate = viewModel::update,
        onOpenBackup = onOpenBackup,
        onOpenLibrary = onOpenLibrary,
        onOpenNotifications = {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        },
        onAllowBattery = {
            @SuppressLint("BatteryLife")
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, "package:${context.packageName}".toUri())
            runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                .onFailure { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        },
        modifier = modifier,
    )
}

@Composable
fun SettingsContent(
    state: SettingsUiState,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    onOpenBackup: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenNotifications: () -> Unit,
    onAllowBattery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = MyFitTheme.colors
    val s = state.settings
    TabScreen(title = stringResource(R.string.nav_settings), modifier = modifier) {
        SectionHeader(stringResource(R.string.settings_appearance))
        MyFitCard(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_theme), style = MyFitTheme.type.caption, color = c.textSecondary)
            Spacer(Modifier.height(Spacing.s))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                listOf(ThemeMode.DARK to R.string.settings_theme_dark, ThemeMode.LIGHT to R.string.settings_theme_light, ThemeMode.SYSTEM to R.string.settings_theme_system)
                    .forEach { (mode, label) -> MyFitChip(stringResource(label), selected = s.themeMode == mode, onClick = { onUpdate { it.copy(themeMode = mode) } }) }
            }
        }

        SectionHeader(stringResource(R.string.settings_workout), Modifier.padding(top = Spacing.s))
        MyFitCard(Modifier.fillMaxWidth()) {
            NumberStepper(
                value = Durations.rest(s.defaultRestSeconds),
                label = stringResource(R.string.settings_default_rest),
                onDecrement = { onUpdate { it.copy(defaultRestSeconds = it.defaultRestSeconds - REST_STEP_SECONDS) } },
                onIncrement = { onUpdate { it.copy(defaultRestSeconds = it.defaultRestSeconds + REST_STEP_SECONDS) } },
            )
            Spacer(Modifier.height(Spacing.l))
            Text(stringResource(R.string.settings_rest_sound), style = MyFitTheme.type.caption, color = c.textSecondary)
            Spacer(Modifier.height(Spacing.s))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.s)) {
                MyFitChip(stringResource(R.string.settings_rest_sound_follow), selected = s.restSound == RestSound.FOLLOW_RINGER, onClick = { onUpdate { it.copy(restSound = RestSound.FOLLOW_RINGER) } })
                MyFitChip(stringResource(R.string.settings_rest_sound_always), selected = s.restSound == RestSound.ALWAYS, onClick = { onUpdate { it.copy(restSound = RestSound.ALWAYS) } })
            }
            Spacer(Modifier.height(Spacing.m))
            SwitchRow(stringResource(R.string.settings_keep_screen), null, s.keepScreenOn) { v -> onUpdate { it.copy(keepScreenOn = v) } }
            SwitchRow(stringResource(R.string.settings_update_loads), stringResource(R.string.settings_update_loads_hint), s.updateRoutineLoadsOnFinish) { v ->
                onUpdate { it.copy(updateRoutineLoadsOnFinish = v) }
            }
        }

        SectionHeader(stringResource(R.string.settings_data), Modifier.padding(top = Spacing.s))
        NavRow(Icons.Rounded.Backup, stringResource(R.string.backup_title), stringResource(R.string.settings_backup_hint), onOpenBackup, error = state.backup.lastError != null)
        NavRow(Icons.AutoMirrored.Rounded.LibraryBooks, stringResource(R.string.routines_library), stringResource(R.string.routines_library_hint), onOpenLibrary)

        SectionHeader(stringResource(R.string.settings_system), Modifier.padding(top = Spacing.s))
        StatusRow(
            title = stringResource(R.string.settings_notifications),
            ok = state.system.notificationsEnabled,
            okText = stringResource(R.string.settings_notifications_on),
            problemText = stringResource(R.string.settings_notifications_off),
            actionText = stringResource(R.string.settings_open),
            onAction = onOpenNotifications,
        )
        StatusRow(
            title = stringResource(R.string.settings_battery),
            ok = state.system.batteryUnrestricted,
            okText = stringResource(R.string.settings_battery_ok),
            problemText = stringResource(R.string.settings_battery_restricted),
            actionText = stringResource(R.string.settings_battery_allow),
            onAction = onAllowBattery,
        )

        SectionHeader(stringResource(R.string.settings_about), Modifier.padding(top = Spacing.s))
        MyFitCard(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.settings_version, state.versionName), style = MyFitTheme.type.bodyStrong, color = c.textPrimary)
            Text(stringResource(R.string.settings_licenses), style = MyFitTheme.type.caption, color = c.textSecondary)
        }
    }
}

@Composable
private fun SwitchRow(title: String, hint: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = MyFitTheme.colors
    Row(
        Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Switch, onValueChange = onChange).padding(vertical = Spacing.s),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MyFitTheme.type.bodyStrong, color = c.textPrimary)
            if (hint != null) Text(hint, style = MyFitTheme.type.caption, color = c.textSecondary)
        }
        Switch(checked = checked, onCheckedChange = null, colors = SwitchDefaults.colors(checkedTrackColor = c.accent, checkedThumbColor = c.onAccent))
    }
}

@Composable
private fun NavRow(icon: ImageVector, title: String, hint: String, onClick: () -> Unit, error: Boolean = false) {
    val c = MyFitTheme.colors
    MyFitCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = if (error) c.danger else c.textSecondary)
            Spacer(Modifier.width(Spacing.m))
            Column(Modifier.weight(1f)) {
                Text(title, style = MyFitTheme.type.bodyStrong, color = c.textPrimary)
                Text(hint, style = MyFitTheme.type.caption, color = c.textSecondary)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = c.textTertiary)
        }
    }
}

@Composable
private fun StatusRow(title: String, ok: Boolean, okText: String, problemText: String, actionText: String, onAction: () -> Unit) {
    val c = MyFitTheme.colors
    MyFitCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MyFitTheme.type.bodyStrong, color = c.textPrimary)
                Text(if (ok) okText else problemText, style = MyFitTheme.type.caption, color = if (ok) c.textSecondary else c.warning)
            }
            if (ok) Icon(Icons.Rounded.Check, contentDescription = null, tint = c.accent) else TextAction(actionText, onClick = onAction)
        }
    }
}
