package io.github.luannamorim.myfit.data.prefs

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.github.luannamorim.myfit.domain.body.Sex
import io.github.luannamorim.myfit.domain.model.AppSettings
import io.github.luannamorim.myfit.domain.model.Reminder
import io.github.luannamorim.myfit.domain.model.RestSound
import io.github.luannamorim.myfit.domain.model.ThemeMode
import io.github.luannamorim.myfit.domain.nutrition.MealReminder
import io.github.luannamorim.myfit.domain.nutrition.NutritionGoal
import io.github.luannamorim.myfit.domain.nutrition.NutritionReminders
import io.github.luannamorim.myfit.domain.nutrition.WaterReminder
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
import java.time.LocalDate

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
    fun reminders_exportedBeforeTheSportField_restoreWithoutSport() = runTest {
        val old = repo(File(tmp.root, "old-reminders.preferences_pb"))
        old.importJson("""{"reminders":[{"id":1,"label":"Natação","minuteOfDay":420,"days":["MONDAY"]}]}""")
        val reminder = old.current().reminders.single()
        assertEquals("Natação", reminder.label)
        assertNull(reminder.sport)
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

    @Test
    fun birthDateAndGoal_setAndCleared_andTravelInExports() = runTest {
        val r = repo()
        assertNull(r.current().birthEpochDay)
        assertEquals(NutritionGoal.MAINTAIN, r.current().nutritionGoal)

        val born = LocalDate.of(1996, 5, 20).toEpochDay()
        r.update { it.copy(birthEpochDay = born, nutritionGoal = NutritionGoal.LOSE) }
        assertEquals(born, r.current().birthEpochDay)
        assertEquals(NutritionGoal.LOSE, r.current().nutritionGoal)

        val copy = repo(File(tmp.root, "copy.preferences_pb"))
        copy.importJson(r.exportJson())
        assertEquals(born, copy.current().birthEpochDay)
        assertEquals(NutritionGoal.LOSE, copy.current().nutritionGoal)

        r.update { it.copy(birthEpochDay = null) }
        assertNull(r.current().birthEpochDay)

        // Settings exported before 1.8.0: no birth date, maintain.
        val old = repo(File(tmp.root, "old18.preferences_pb"))
        old.importJson("""{"themeMode":"LIGHT"}""")
        assertNull(old.current().birthEpochDay)
        assertEquals(NutritionGoal.MAINTAIN, old.current().nutritionGoal)
    }

    @Test
    fun firstDayOfWeek_mondayByDefault_persists_andTravelsInExports() = runTest {
        val r = repo()
        assertEquals(DayOfWeek.MONDAY, r.current().firstDayOfWeek)
        r.update { it.copy(firstDayOfWeek = DayOfWeek.SUNDAY) }
        assertEquals(DayOfWeek.SUNDAY, r.current().firstDayOfWeek)
        val copy = repo(File(tmp.root, "week.preferences_pb"))
        copy.importJson(r.exportJson())
        assertEquals(DayOfWeek.SUNDAY, copy.current().firstDayOfWeek)
        // A backup from before the setting existed keeps Monday.
        copy.importJson("""{"themeMode":"LIGHT"}""")
        assertEquals(DayOfWeek.MONDAY, copy.current().firstDayOfWeek)
    }

    @Test
    fun waterButtons_default250And500_persist_andTravelInExports() = runTest {
        val r = repo()
        assertEquals(listOf(250, 500), r.current().waterButtonsMl)
        r.update { it.copy(waterButtonsMl = listOf(300, 750)) }
        assertEquals(listOf(300, 750), r.current().waterButtonsMl)
        val copy = repo(File(tmp.root, "buttons.preferences_pb"))
        copy.importJson(r.exportJson())
        assertEquals(listOf(300, 750), copy.current().waterButtonsMl)
        // Out-of-range or missing values fall back to the defaults.
        r.update { it.copy(waterButtonsMl = listOf(0, 9000)) }
        assertEquals(listOf(250, 500), r.current().waterButtonsMl)
    }

    @Test
    fun customTargets_persist_clear_andIgnoreNonsense() = runTest {
        val r = repo()
        assertNull(r.current().customKcal)
        assertNull(r.current().customWaterMl)
        r.update { it.copy(customKcal = 2500, customWaterMl = 3200) }
        assertEquals(2500, r.current().customKcal)
        assertEquals(3200, r.current().customWaterMl)
        val copy = repo(File(tmp.root, "targets.preferences_pb"))
        copy.importJson(r.exportJson())
        assertEquals(2500, copy.current().customKcal)
        r.update { it.copy(customKcal = null, customWaterMl = 50) } // back to recommended; 50 ml isn't a daily target
        assertNull(r.current().customKcal)
        assertNull(r.current().customWaterMl)
    }

    @Test
    fun nutritionReminders_persist_andTravelInExports() = runTest {
        val r = repo()
        assertEquals(NutritionReminders(), r.current().nutritionReminders)
        val set = NutritionReminders(
            meals = listOf(MealReminder(1, 7 * 60), MealReminder(2, 12 * 60, enabled = false)),
            water = WaterReminder(enabled = true, everyMinutes = 90, fromMinute = 9 * 60, untilMinute = 21 * 60),
        )
        r.update { it.copy(nutritionReminders = set) }
        assertEquals(set, r.current().nutritionReminders)

        val copy = repo(File(tmp.root, "nutri.preferences_pb"))
        copy.importJson(r.exportJson())
        assertEquals(set, copy.current().nutritionReminders)

        // Settings exported before 1.9.0: none, water off.
        val old = repo(File(tmp.root, "old19.preferences_pb"))
        old.importJson("""{"themeMode":"LIGHT"}""")
        assertEquals(NutritionReminders(), old.current().nutritionReminders)
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
