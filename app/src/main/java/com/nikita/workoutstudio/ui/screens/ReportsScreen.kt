package com.nikita.workoutstudio.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nikita.workoutstudio.model.ExerciseReport
import com.nikita.workoutstudio.model.WorkoutReport
import com.nikita.workoutstudio.ui.AppViewModel
import com.nikita.workoutstudio.ui.theme.SoftCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportsScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    val reports by vm.reports.collectAsState()
    var selectedId by remember { mutableStateOf<Long?>(null) }

    val selected = reports.firstOrNull { it.id == selectedId }
    if (selected != null) {
        ReportDetail(
            report = selected,
            onBack = { selectedId = null },
            onDelete = { vm.deleteReport(selected.id); selectedId = null },
            modifier = modifier
        )
    } else {
        ReportList(
            reports = reports,
            onOpen = { selectedId = it.id },
            modifier = modifier
        )
    }
}

@Composable
private fun ReportList(
    reports: List<WorkoutReport>,
    onOpen: (WorkoutReport) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(reports, query) {
        if (query.isBlank()) reports
        else reports.filter { formatDateTime(it.startedAt).contains(query.trim(), ignoreCase = true) }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Отчёты",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp)
        )

        if (reports.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Здесь появятся твои тренировки.\nЗапусти упражнение и отметь повторения.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Поиск по дате (напр. 08.06)") },
            singleLine = true,
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
        )

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filtered, key = { it.id }) { report ->
                SoftCard(modifier = Modifier.fillMaxWidth().clickable { onOpen(report) }) {
                    Text(
                        formatDateTime(report.startedAt),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${report.exercises.size} упр. · ${report.totalSets} подх. · ${report.totalReps} повт. · ${formatDuration(report)}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportDetail(
    report: WorkoutReport,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var confirmDelete by remember { mutableStateOf(false) }

    // System back returns to the report list instead of leaving the app.
    BackHandler { onBack() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Назад",
                    tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(
                formatDateTime(report.startedAt),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            )
            IconButton(onClick = { confirmDelete = true }) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = "Удалить",
                    tint = MaterialTheme.colorScheme.error)
            }
        }

        Spacer(Modifier.height(8.dp))

        report.exercises.forEach { ex ->
            ExerciseReportCard(ex)
            Spacer(Modifier.height(12.dp))
        }

        SoftCard(modifier = Modifier.fillMaxWidth()) {
            SummaryRow("Начало", formatTime(report.startedAt))
            SummaryRow("Конец", formatTime(report.finishedAt))
            SummaryRow("Длительность", formatDuration(report))
            SummaryRow("Всего подходов", report.totalSets.toString())
            SummaryRow("Всего повторений", report.totalReps.toString(), last = true)
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Удалить отчёт?") },
            text = { Text("Запись этой тренировки будет удалена без возможности восстановления.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun ExerciseReportCard(ex: ExerciseReport) {
    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            ex.name,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "Отдых ${ex.restSeconds}с · цель ${ex.targetReps} повт.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        ex.sets.forEachIndexed { i, set ->
            val repColor = when {
                set.actualReps < set.targetReps -> MaterialTheme.colorScheme.error
                set.actualReps > set.targetReps -> SuccessGreen
                else -> MaterialTheme.colorScheme.onSurface
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Подход ${i + 1}",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${set.actualReps} / ${set.targetReps}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = repColor
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, last: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f))
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface)
    }
    if (!last) Spacer(Modifier.height(0.dp))
}

private val dateTimeFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

// Reads well on both the light and dark frosted cards.
private val SuccessGreen = Color(0xFF34A853)

private fun formatDateTime(millis: Long): String = dateTimeFormat.format(Date(millis))
private fun formatTime(millis: Long): String = timeFormat.format(Date(millis))

private fun formatDuration(report: WorkoutReport): String {
    val sec = ((report.finishedAt - report.startedAt) / 1000L).coerceAtLeast(0)
    val m = sec / 60
    val s = sec % 60
    return if (m > 0) "${m} мин ${s} с" else "${s} с"
}
