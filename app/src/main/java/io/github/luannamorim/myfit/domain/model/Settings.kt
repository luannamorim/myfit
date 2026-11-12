package io.github.luannamorim.myfit.domain.model

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
    val keepScreenOn: Boolean = true,
    val updateRoutineLoadsOnFinish: Boolean = true,
    val autoBackup: Boolean = true,
    val reminders: List<Reminder> = emptyList(),
) {
    companion object {
        const val MIN_REST_SECONDS = 15
        const val MAX_REST_SECONDS = 600
    }
}
