package com.nikita.workoutstudio.data

import android.content.Context
import com.nikita.workoutstudio.model.WorkoutReport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ReportRepository(context: Context) {

    private val prefs = context.getSharedPreferences("reports", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private val _reports = MutableStateFlow(load())
    val reports: StateFlow<List<WorkoutReport>> = _reports.asStateFlow()

    private fun load(): List<WorkoutReport> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<WorkoutReport>>(raw)
                .sortedByDescending { it.startedAt }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun persist(list: List<WorkoutReport>) {
        val trimmed = list.sortedByDescending { it.startedAt }.take(MAX_REPORTS)
        _reports.value = trimmed
        prefs.edit().putString(KEY, json.encodeToString(trimmed)).apply()
    }

    fun add(report: WorkoutReport) {
        if (report.totalSets == 0) return
        persist(_reports.value + report)
    }

    fun delete(id: Long) {
        persist(_reports.value.filterNot { it.id == id })
    }

    fun deleteMany(ids: Set<Long>) {
        if (ids.isEmpty()) return
        persist(_reports.value.filterNot { it.id in ids })
    }

    fun deleteAll() {
        persist(emptyList())
    }

    companion object {
        private const val KEY = "report_list"
        private const val MAX_REPORTS = 500
    }
}
