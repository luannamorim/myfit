package io.github.luannamorim.myfit.domain.model

import io.github.luannamorim.myfit.domain.body.Sex
import io.github.luannamorim.myfit.domain.nutrition.NutritionGoal
import io.github.luannamorim.myfit.domain.nutrition.NutritionReminders
import kotlinx.serialization.Serializable

enum class ThemeMode { DARK, LIGHT, SYSTEM }

/**
 * How the rest-end alert sounds: always ring on the alarm stream (default, heard with the phone on
 * vibrate or silent), or follow the phone ringer mode (only vibrates when the phone does).
 */
enum class RestSound { FOLLOW_RINGER, ALWAYS }

@Serializable
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val defaultRestSeconds: Int = 90,
    val restSound: RestSound = RestSound.ALWAYS,
    /** Three short beeps in the last 3 seconds of rest (with the app's own end-of-rest sound). */
    val countdownBeeps: Boolean = true,
    val keepScreenOn: Boolean = true,
    val updateRoutineLoadsOnFinish: Boolean = true,
    val autoBackup: Boolean = true,
    val reminders: List<Reminder> = emptyList(),
    /** Only for the body-fat estimate; null = not set (no estimate). */
    val sex: Sex? = null,
    /** Birth date as epoch day, for the calorie target (age); null = not set. */
    val birthEpochDay: Long? = null,
    /** What the calorie target aims for. */
    val nutritionGoal: NutritionGoal = NutritionGoal.MAINTAIN,
    /** Meal times and the water interval (1.9.0). */
    val nutritionReminders: NutritionReminders = NutritionReminders(),
    /** The two quick water buttons of the Nutrition tab, in ml (the first one is also the reminder's button). */
    val waterButtonsMl: List<Int> = DEFAULT_WATER_BUTTONS_ML,
    /** The user's own daily targets; null = the app's recommendation. */
    val customKcal: Int? = null,
    val customWaterMl: Int? = null,
) {
    companion object {
        const val MIN_REST_SECONDS = 15
        const val MAX_REST_SECONDS = 600
        val DEFAULT_WATER_BUTTONS_ML = listOf(250, 500)
        val CUSTOM_KCAL_RANGE = 800..6000
        val CUSTOM_WATER_RANGE_ML = 500..10_000
    }
}
