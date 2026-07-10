package com.nikita.workoutstudio.data

import android.content.Context
import com.nikita.workoutstudio.model.TimerSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Persists timer settings in SharedPreferences. These survive an in-place app
 * update (same applicationId + signing key), so users keep their preferences
 * when installing a newer build over an existing one. `ignoreUnknownKeys` keeps
 * old JSON readable after new fields are added; a parse failure falls back to
 * defaults rather than crashing. Only a full uninstall clears the data.
 */
class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<TimerSettings> = _settings.asStateFlow()

    private fun load(): TimerSettings {
        val raw = prefs.getString(KEY, null) ?: return TimerSettings()
        return try {
            json.decodeFromString<TimerSettings>(raw)
        } catch (e: Exception) {
            TimerSettings()
        }
    }

    fun update(settings: TimerSettings) {
        _settings.value = settings
        prefs.edit().putString(KEY, json.encodeToString(settings)).apply()
    }

    fun current(): TimerSettings = _settings.value

    companion object {
        private const val KEY = "timer_settings"
    }
}
