package io.github.luannamorim.myfit.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.luannamorim.myfit.domain.model.AppSettings
import io.github.luannamorim.myfit.domain.model.Reminder
import io.github.luannamorim.myfit.domain.model.RestSound
import io.github.luannamorim.myfit.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @Named("settings") private val dataStore: DataStore<Preferences>,
) {
    private object Keys {
        val theme = stringPreferencesKey("theme_mode")
        val defaultRest = intPreferencesKey("default_rest_seconds")
        val restSound = stringPreferencesKey("rest_sound")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")
        val updateLoads = booleanPreferencesKey("update_routine_loads_on_finish")
        val autoBackup = booleanPreferencesKey("auto_backup")
        val reminders = stringPreferencesKey("reminders")
    }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val remindersSerializer = ListSerializer(Reminder.serializer())

    val settings: Flow<AppSettings> = dataStore.data.map { it.toSettings() }

    suspend fun current(): AppSettings = settings.first()

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { prefs ->
            val next = transform(prefs.toSettings()).let {
                it.copy(defaultRestSeconds = it.defaultRestSeconds.coerceIn(AppSettings.MIN_REST_SECONDS, AppSettings.MAX_REST_SECONDS))
            }
            prefs[Keys.theme] = next.themeMode.name
            prefs[Keys.defaultRest] = next.defaultRestSeconds
            prefs[Keys.restSound] = next.restSound.name
            prefs[Keys.keepScreenOn] = next.keepScreenOn
            prefs[Keys.updateLoads] = next.updateRoutineLoadsOnFinish
            prefs[Keys.autoBackup] = next.autoBackup
            prefs[Keys.reminders] = json.encodeToString(remindersSerializer, next.reminders)
        }
    }

    suspend fun exportJson(): String = json.encodeToString(AppSettings.serializer(), current())

    suspend fun importJson(text: String) {
        val imported = json.decodeFromString(AppSettings.serializer(), text)
        update { imported }
    }

    private fun Preferences.toSettings(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            themeMode = this[Keys.theme]?.let { enumValueOrNull<ThemeMode>(it) } ?: defaults.themeMode,
            defaultRestSeconds = this[Keys.defaultRest] ?: defaults.defaultRestSeconds,
            restSound = this[Keys.restSound]?.let { enumValueOrNull<RestSound>(it) } ?: defaults.restSound,
            keepScreenOn = this[Keys.keepScreenOn] ?: defaults.keepScreenOn,
            updateRoutineLoadsOnFinish = this[Keys.updateLoads] ?: defaults.updateRoutineLoadsOnFinish,
            autoBackup = this[Keys.autoBackup] ?: defaults.autoBackup,
            reminders = this[Keys.reminders]?.let { runCatching { json.decodeFromString(remindersSerializer, it) }.getOrNull() } ?: defaults.reminders,
        )
    }
}

inline fun <reified E : Enum<E>> enumValueOrNull(name: String): E? = enumValues<E>().firstOrNull { it.name == name }
