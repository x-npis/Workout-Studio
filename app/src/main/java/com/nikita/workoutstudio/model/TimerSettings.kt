package com.nikita.workoutstudio.model

import kotlinx.serialization.Serializable

@Serializable
data class TimerSettings(
    val vibrate: Boolean = true,
    val sound: Boolean = true,
    val runInBackground: Boolean = true,
    val keepScreenOn: Boolean = true,
    // Chosen alarm ringtone (system content:// uri). null = default alarm sound.
    val alarmSoundUri: String? = null
)
