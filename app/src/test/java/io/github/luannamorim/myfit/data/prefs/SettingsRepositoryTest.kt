package io.github.luannamorim.myfit.data.prefs

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.github.luannamorim.myfit.domain.model.AppSettings
import io.github.luannamorim.myfit.domain.model.RestSound
import io.github.luannamorim.myfit.domain.model.ThemeMode
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SettingsRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun TestScope.repo(file: File = File(tmp.root, "settings.preferences_pb")) =
        SettingsRepository(PreferenceDataStoreFactory.create(scope = backgroundScope) { file })

    @Test
    fun settings_defaults() = runTest {
        val current = repo().current()
        assertEquals(AppSettings(), current)
        assertEquals(ThemeMode.DARK, current.themeMode)
    }

    @Test
    fun update_clampsDefaultRest() = runTest {
        val r = repo()
        r.update { it.copy(defaultRestSeconds = 5) }
        assertEquals(15, r.current().defaultRestSeconds)
        r.update { it.copy(defaultRestSeconds = 9999) }
        assertEquals(600, r.current().defaultRestSeconds)
    }

    @Test
    fun settings_exportImport_roundTrip() = runTest {
        val source = repo(File(tmp.root, "a.preferences_pb"))
        source.update { it.copy(themeMode = ThemeMode.LIGHT, restSound = RestSound.ALWAYS, defaultRestSeconds = 120, autoBackup = false) }
        val json = source.exportJson()
        val target = repo(File(tmp.root, "b.preferences_pb"))
        target.importJson(json)
        assertEquals(source.current(), target.current())
    }
}
