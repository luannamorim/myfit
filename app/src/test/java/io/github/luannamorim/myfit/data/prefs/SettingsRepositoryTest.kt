package io.github.luannamorim.myfit.data.prefs

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.github.luannamorim.myfit.domain.body.Sex
import io.github.luannamorim.myfit.domain.model.AppSettings
import io.github.luannamorim.myfit.domain.model.Reminder
import io.github.luannamorim.myfit.domain.model.RestSound
import io.github.luannamorim.myfit.domain.model.ThemeMode
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.DayOfWeek

class SettingsRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun TestScope.repo(file: File = File(tmp.root, "settings.preferences_pb")) =
        SettingsRepository(PreferenceDataStoreFactory.create(scope = backgroundScope) { file })

    @Test
    fun settings_defaults() = runTest {
        val current = repo().current()
        assertEquals(AppSettings(), current)
        assertEquals(ThemeMode.DARK, current.themeMode)
        // The rest end must be heard at the gym, where phones are often on vibrate.
        assertEquals(RestSound.ALWAYS, current.restSound)
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

    @Test
    fun sex_setAndCleared_andTravelsInExports() = runTest {
        val r = repo()
        r.update { it.copy(sex = Sex.FEMALE) }
        assertEquals(Sex.FEMALE, r.current().sex)
        r.update { it.copy(sex = Sex.MALE) }
        assertTrue(r.exportJson().contains("\"sex\":\"MALE\""))
        r.update { it.copy(sex = null) }
        assertNull(r.current().sex)

        // Settings exported before the field existed: no sex.
        val old = repo(File(tmp.root, "old.preferences_pb"))
        old.importJson("""{"themeMode":"LIGHT"}""")
        assertNull(old.current().sex)
        assertEquals(ThemeMode.LIGHT, old.current().themeMode)
    }

    private val reminders = listOf(
        Reminder(id = 1, routineId = 7, label = "Costas", minuteOfDay = 7 * 60, days = setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)),
        Reminder(id = 2, label = "Corrida", minuteOfDay = 18 * 60 + 30, dateEpochDay = 20_410, enabled = false),
    )

    @Test
    fun reminders_surviveOtherUpdates() = runTest {
        val r = repo()
        r.update { it.copy(reminders = reminders) }
        r.update { it.copy(themeMode = ThemeMode.LIGHT, defaultRestSeconds = 120) }
        assertEquals(reminders, r.current().reminders)
        assertEquals(ThemeMode.LIGHT, r.current().themeMode)
    }

    @Test
    fun reminders_exportImport_roundTrip() = runTest {
        val source = repo(File(tmp.root, "a.preferences_pb"))
        source.update { it.copy(autoBackup = false, reminders = reminders) }
        val json = source.exportJson()
        // Backup format: weekdays by name.
        assertTrue(json, json.contains(""""days":["MONDAY","FRIDAY"]"""))
        val target = repo(File(tmp.root, "b.preferences_pb"))
        target.importJson(json)
        assertEquals(reminders, target.current().reminders)
        assertEquals(source.current(), target.current())
    }

    @Test
    fun import_oldSettingsWithoutReminders_keepsTheOtherValues() = runTest {
        // settings.json written by 1.3.0, before reminders existed.
        val old = """{"themeMode":"LIGHT","defaultRestSeconds":120,"restSound":"ALWAYS","keepScreenOn":false,"updateRoutineLoadsOnFinish":true,"autoBackup":false}"""
        val r = repo()
        r.update { it.copy(reminders = reminders) }
        r.importJson(old)
        val current = r.current()
        assertEquals(AppSettings(ThemeMode.LIGHT, 120, RestSound.ALWAYS, keepScreenOn = false, autoBackup = false), current)
        assertEquals(emptyList<Reminder>(), current.reminders)
    }
}
