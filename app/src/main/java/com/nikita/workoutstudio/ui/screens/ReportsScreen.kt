package com.nikita.workoutstudio.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nikita.workoutstudio.data.ReportExporter
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
    val context = LocalContext.current
    var selectedId by remember { mutableStateOf<Long?>(null) }

    // Multi-select state (Telegram-style): a non-empty set means selection mode is on.
    var selectedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var exportFor by remember { mutableStateOf<Set<Long>?>(null) }
    var confirmDeleteSelected by remember { mutableStateOf(false) }

    val selected = reports.firstOrNull { it.id == selectedId }
    if (selected != null) {
        ReportDetail(
            report = selected,
            onBack = { selectedId = null },
            onDelete = { vm.deleteReport(selected.id); selectedId = null },
            onExport = { exportFor = setOf(selected.id) },
            modifier = modifier
        )
    } else {
        ReportList(
            reports = reports,
            selectedIds = selectedIds,
            onOpen = { selectedId = it.id },
            onToggleSelect = { id ->
                selectedIds = if (id in selectedIds) selectedIds - id else selectedIds + id
            },
            onClearSelection = { selectedIds = emptySet() },
            onExportSelected = { exportFor = selectedIds },
            onDeleteSelected = { confirmDeleteSelected = true },
            modifier = modifier
        )
    }

    exportFor?.let { ids ->
        ExportDialog(
            count = ids.size,
            onDismiss = { exportFor = null },
            onExport = { format, copy ->
                val content = ReportExporter.build(vm.reportsByIds(ids), format)
                if (copy) ReportExporter.copyToClipboard(context, content)
                else ReportExporter.share(context, content, format)
                exportFor = null
            }
        )
    }

    if (confirmDeleteSelected) {
        AlertDialog(
            onDismissRequest = { confirmDeleteSelected = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Удалить выбранные?") },
            text = { Text("Будут удалены ${selectedIds.size} отчётов без возможности восстановления.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteReports(selectedIds)
                    selectedIds = emptySet()
                    confirmDeleteSelected = false
                }) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteSelected = false }) { Text("Отмена") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReportList(
    reports: List<WorkoutReport>,
    selectedIds: Set<Long>,
    onOpen: (WorkoutReport) -> Unit,
    onToggleSelect: (Long) -> Unit,
    onClearSelection: () -> Unit,
    onExportSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(reports, query) {
        if (query.isBlank()) reports
        else reports.filter { formatDateTime(it.startedAt).contains(query.trim(), ignoreCase = true) }
    }
    val selectionMode = selectedIds.isNotEmpty()

    // While selecting, system back clears the selection instead of leaving the tab.
    BackHandler(enabled = selectionMode) { onClearSelection() }

    Column(modifier = modifier.fillMaxSize()) {
        if (selectionMode) {
            SelectionBar(
                count = selectedIds.size,
                onClose = onClearSelection,
                onExport = onExportSelected,
                onDelete = onDeleteSelected
            )
        } else {
            Text(
                text = "Отчёты",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp)
            )
        }

        if (reports.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Здесь появятся твои тренировки.\nЗапусти упражнение и отметь повторения.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return
        }

        if (!selectionMode) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Поиск по дате (напр. 08.06)") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filtered, key = { it.id }) { report ->
                val isSelected = report.id in selectedIds
                val scheme = MaterialTheme.colorScheme
                val cardModifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            if (selectionMode) onToggleSelect(report.id) else onOpen(report)
                        },
                        onLongClick = { onToggleSelect(report.id) }
                    )
                Box {
                    SoftCard(modifier = cardModifier) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    formatDateTime(report.startedAt),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = scheme.onSurface
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "${report.exercises.size} упр. · ${report.totalSets} подх. · ${report.totalReps} повт. · ${formatDuration(report)}",
                                    fontSize = 14.sp,
                                    color = scheme.onSurfaceVariant
                                )
                            }
                            if (selectionMode) {
                                SelectionMark(selected = isSelected)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionBar(
    count: Int,
    onClose: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Outlined.Close, contentDescription = "Отменить выбор",
                tint = MaterialTheme.colorScheme.onBackground)
        }
        Text(
            "Выбрано: $count",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onExport) {
            Icon(Icons.Outlined.Share, contentDescription = "Экспорт выбранных",
                tint = MaterialTheme.colorScheme.onBackground)
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Удалить выбранные",
                tint = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun SelectionMark(selected: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val bg = if (selected) scheme.primary else Color.Transparent
    val border = if (selected) scheme.primary else scheme.outline
    Box(
        modifier = Modifier
            .padding(start = 8.dp)
            .size(26.dp)
            .background(bg, RoundedCornerShape(13.dp))
            .border(1.5.dp, border, RoundedCornerShape(13.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = null,
                tint = scheme.onPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun ExportDialog(
    count: Int,
    onDismiss: () -> Unit,
    onExport: (ReportExporter.Format, copy: Boolean) -> Unit
) {
    var format by remember { mutableStateOf(ReportExporter.Format.JSON) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Экспорт отчётов") },
        text = {
            Column {
                Text(
                    if (count == 1) "Отчёт будет сохранён в выбранном формате."
                    else "Все $count тренировок будут собраны в один документ.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(12.dp))
                Text("Формат", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                ReportExporter.Format.entries.forEach { f ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clickable { format = f }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = format == f, onClick = { format = f })
                        Text(f.label, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onExport(format, false) }) { Text("Поделиться") }
        },
        dismissButton = {
            TextButton(onClick = { onExport(format, true) }) { Text("Скопировать") }
        }
    )
}

@Composable
private fun ReportDetail(
    report: WorkoutReport,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit,
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
            IconButton(onClick = onExport) {
                Icon(Icons.Outlined.Share, contentDescription = "Экспорт",
                    tint = MaterialTheme.colorScheme.onBackground)
            }
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
