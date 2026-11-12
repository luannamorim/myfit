package io.github.luannamorim.myfit.ui.feature.reminders

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import io.github.luannamorim.myfit.domain.model.Reminder
import io.github.luannamorim.myfit.domain.model.ThemeMode
import io.github.luannamorim.myfit.testutil.PHONE
import io.github.luannamorim.myfit.testutil.SCREENSHOT_DIR
import io.github.luannamorim.myfit.testutil.snapBothThemes
import io.github.luannamorim.myfit.ui.theme.MyFitTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.DayOfWeek
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = PHONE)
class RemindersScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private val today = LocalDate.parse("2026-11-12")

    private val reminders = listOf(
        ReminderRow(Reminder(1, routineId = 1, label = "Leg day", minuteOfDay = 7 * 60, days = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))),
        ReminderRow(Reminder(2, label = "Run", minuteOfDay = 18 * 60 + 30, days = DayOfWeek.entries.toSet(), enabled = false)),
        ReminderRow(Reminder(3, routineId = 2, label = "Chest & triceps", minuteOfDay = 9 * 60 + 15, dateEpochDay = today.plusDays(2).toEpochDay())),
    )

    @Test
    fun reminders() = compose.snapBothThemes("reminders") {
        RemindersContent(
            RemindersUiState(loading = false, rows = reminders, today = today, notificationsEnabled = true),
            onBack = {}, onCreate = {}, onEdit = {}, onToggle = { _, _ -> }, onOpenNotifications = {}, is24Hour = false,
        )
    }

    @Test
    fun reminders_emptyAndNotificationsOff() = compose.snapBothThemes("reminders_empty") {
        RemindersContent(
            RemindersUiState(loading = false, today = today, notificationsEnabled = false),
            onBack = {}, onCreate = {}, onEdit = {}, onToggle = { _, _ -> }, onOpenNotifications = {}, is24Hour = false,
        )
    }

    @Test
    fun reminders_notificationsOff() = compose.snapBothThemes("reminders_notifications_off") {
        RemindersContent(
            RemindersUiState(loading = false, rows = reminders.take(1), today = today, notificationsEnabled = false),
            onBack = {}, onCreate = {}, onEdit = {}, onToggle = { _, _ -> }, onOpenNotifications = {}, is24Hour = false,
        )
    }

    private fun editorState(days: Set<DayOfWeek>, other: Boolean) = ReminderEditorUiState(
        loading = false,
        isNew = false,
        routines = listOf(RoutineChoice(1, "Leg day"), RoutineChoice(2, "Chest & triceps"), RoutineChoice(3, "Back day")),
        routineId = if (other) null else 2,
        activity = if (other) "Run" else "",
        minuteOfDay = 18 * 60 + 30,
        days = days,
        date = today.plusDays(2),
        today = today,
        pastError = other,
    )

    @Test
    fun editor_weekly() = compose.snapBothThemes("reminder_editor") {
        ReminderEditorContent(editorState(setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY), other = false), ReminderEditorActions(), is24Hour = false)
    }

    @Test
    fun editor_onceOtherActivity_withError() = compose.snapBothThemes("reminder_editor_once") {
        ReminderEditorContent(editorState(emptySet(), other = true), ReminderEditorActions(), is24Hour = true)
    }

    @Test
    fun editor_timeDialog() = snapDialogBothThemes("reminder_time_dialog", "6:30 PM")

    @Test
    fun editor_dateDialog() = snapDialogBothThemes("reminder_date_dialog", "Date: Nov 14, 2026")

    /** Opens a dialog by tapping [opener] and captures the whole screen (dialogs live in their own window). */
    @OptIn(ExperimentalRoborazziApi::class)
    private fun snapDialogBothThemes(name: String, opener: String) {
        var mode by mutableStateOf(ThemeMode.DARK)
        compose.setContent {
            MyFitTheme(mode) { ReminderEditorContent(editorState(emptySet(), other = false), ReminderEditorActions(), is24Hour = false) }
        }
        compose.onNodeWithText(opener).performClick()
        compose.waitForIdle()
        captureScreenRoboImage("$SCREENSHOT_DIR/${name}_dark.png")
        mode = ThemeMode.LIGHT
        compose.waitForIdle()
        captureScreenRoboImage("$SCREENSHOT_DIR/${name}_light.png")
    }
}
