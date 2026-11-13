package io.github.luannamorim.myfit.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.luannamorim.myfit.domain.activity.LiveActivity
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/** Persists the sport being timed, so a killed app resumes it with the right time. */
@Singleton
class LiveActivityStore @Inject constructor(
    @Named("live_activity") private val dataStore: DataStore<Preferences>,
) {
    private val key = stringPreferencesKey("live_activity")
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun read(): LiveActivity? = dataStore.data.first()[key]?.let { text ->
        runCatching { json.decodeFromString(LiveActivity.serializer(), text) }.getOrNull()
    }

    suspend fun write(activity: LiveActivity?) {
        dataStore.edit { prefs ->
            if (activity == null) prefs.remove(key) else prefs[key] = json.encodeToString(LiveActivity.serializer(), activity)
        }
    }
}
