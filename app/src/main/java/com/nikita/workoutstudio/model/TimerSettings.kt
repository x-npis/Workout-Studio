package com.nikita.workoutstudio.model

import kotlinx.serialization.Serializable

@Serializable
data class TimerSettings(
    val vibrate: Boolean = true,
    val sound: Boolean = true,
    val runInBackground: Boolean = true,
    val keepScreenOn: Boolean = true,
    // Chosen alarm ringtone (system content:// uri). null = default alarm sound.
    val alarmSoundUri: String? = null,
    // "system" follows the OS setting; "light" / "dark" force a fixed theme.
    val themeMode: String = ThemeMode.SYSTEM
)

object ThemeMode {
    const val SYSTEM = "system"
    const val LIGHT = "light"
    const val DARK = "dark"
}
