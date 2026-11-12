package io.github.luannamorim.myfit.ui.feature.settings

import android.content.Context
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.luannamorim.myfit.BuildConfig
import io.github.luannamorim.myfit.data.prefs.BackupState
import io.github.luannamorim.myfit.data.prefs.BackupStateStore
import io.github.luannamorim.myfit.data.prefs.SettingsRepository
import io.github.luannamorim.myfit.domain.model.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SystemStatus(val notificationsEnabled: Boolean = true, val batteryUnrestricted: Boolean = true)

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val backup: BackupState = BackupState(),
    val system: SystemStatus = SystemStatus(),
    val versionName: String = BuildConfig.VERSION_NAME,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    backupState: BackupStateStore,
) : ViewModel() {
    private val system = MutableStateFlow(readSystem())

    val state: StateFlow<SettingsUiState> = combine(settings.settings, backupState.state, system) { s, b, sys ->
        SettingsUiState(settings = s, backup = b, system = sys)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { settings.update(transform) }
    }

    /** Re-read notification/battery permissions (call on resume: the user may change them in system settings). */
    fun refreshSystem() {
        system.value = readSystem()
    }

    private fun readSystem(): SystemStatus {
        val power = context.getSystemService(PowerManager::class.java)
        return SystemStatus(
            notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled(),
            batteryUnrestricted = power?.isIgnoringBatteryOptimizations(context.packageName) ?: true,
        )
    }
}
