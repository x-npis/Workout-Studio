package com.nikita.workoutstudio.data

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import com.nikita.workoutstudio.model.WorkoutReport
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Turns one or several workout reports into a single shareable document
 * (JSON / Markdown / plain text) and hands it off to the share sheet or clipboard.
 * Everything is generated on-device at runtime — no files are committed.
 */
object ReportExporter {

    enum class Format(val label: String) {
        JSON("JSON"),
        MARKDOWN("Markdown (.md)"),
        TEXT("Обычный текст")
    }

    private val prettyJson = Json { prettyPrint = true; encodeDefaults = true }
    private val dateTimeFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    /** Build the export text for [reports] in the chosen [format]. */
    fun build(reports: List<WorkoutReport>, format: Format): String {
        val ordered = reports.sortedByDescending { it.startedAt }
        return when (format) {
            Format.JSON -> prettyJson.encodeToString(ordered)
            Format.MARKDOWN -> buildMarkdown(ordered)
            Format.TEXT -> buildText(ordered)
        }
    }

    private fun buildMarkdown(reports: List<WorkoutReport>): String = buildString {
        append("# Отчёты Workout Studio\n\n")
        append("Всего тренировок: ${reports.size}\n")
        reports.forEach { r ->
            append("\n---\n\n")
            append("## ${dateTimeFormat.format(Date(r.startedAt))}\n\n")
            append("- Начало: ${timeFormat.format(Date(r.startedAt))}\n")
            append("- Конец: ${timeFormat.format(Date(r.finishedAt))}\n")
            append("- Длительность: ${duration(r)}\n")
            append("- Итого: ${r.exercises.size} упр. · ${r.totalSets} подх. · ${r.totalReps} повт.\n")
            r.exercises.forEach { ex ->
                append("\n### ${ex.name}\n\n")
                append("Отдых ${ex.restSeconds}с · цель ${ex.targetReps} повт. · ${ex.plannedSets} подх.\n\n")
                append("| Подход | Сделал | Цель |\n")
                append("|---|---|---|\n")
                ex.sets.forEachIndexed { i, s ->
                    append("| ${i + 1} | ${s.actualReps} | ${s.targetReps} |\n")
                }
            }
        }
    }

    private fun buildText(reports: List<WorkoutReport>): String = buildString {
        append("Отчёты Workout Studio\n")
        append("Всего тренировок: ${reports.size}\n")
        reports.forEach { r ->
            append("\n========================================\n")
            append("${dateTimeFormat.format(Date(r.startedAt))}\n")
            append("Начало ${timeFormat.format(Date(r.startedAt))} · ")
            append("конец ${timeFormat.format(Date(r.finishedAt))} · ")
            append("${duration(r)}\n")
            append("Итого: ${r.exercises.size} упр. · ${r.totalSets} подх. · ${r.totalReps} повт.\n")
            r.exercises.forEach { ex ->
                append("\n  ${ex.name} (отдых ${ex.restSeconds}с, цель ${ex.targetReps})\n")
                ex.sets.forEachIndexed { i, s ->
                    append("    Подход ${i + 1}: ${s.actualReps} / ${s.targetReps}\n")
                }
            }
        }
    }

    private fun duration(r: WorkoutReport): String {
        val sec = ((r.finishedAt - r.startedAt) / 1000L).coerceAtLeast(0)
        val m = sec / 60
        val s = sec % 60
        return if (m > 0) "$m мин $s с" else "$s с"
    }

    fun share(context: Context, content: String, format: Format) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (format == Format.JSON) "application/json" else "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Отчёты Workout Studio")
            putExtra(Intent.EXTRA_TEXT, content)
        }
        val chooser = Intent.createChooser(intent, "Поделиться отчётами")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun copyToClipboard(context: Context, content: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Workout Studio", content))
    }
}
