package io.github.luannamorim.myfit.domain.model

import io.github.luannamorim.myfit.domain.body.Sex
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
) {
    companion object {
        const val MIN_REST_SECONDS = 15
        const val MAX_REST_SECONDS = 600
    }
}
