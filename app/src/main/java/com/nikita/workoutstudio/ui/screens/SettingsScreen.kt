package com.nikita.workoutstudio.ui.screens

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nikita.workoutstudio.data.ReportExporter
import com.nikita.workoutstudio.model.ThemeMode
import com.nikita.workoutstudio.ui.AppViewModel
import com.nikita.workoutstudio.ui.theme.SoftCard

private const val GITHUB_URL = "https://github.com/x-npis/Workout-Studio"
private const val TELEGRAM_URL = "https://t.me/just_nvk"

@Composable
fun SettingsScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    val settings by vm.settings.collectAsState()
    val reports by vm.reports.collectAsState()
    val context = LocalContext.current

    var showExport by remember { mutableStateOf(false) }
    var confirmDeleteAll by remember { mutableStateOf(false) }

    val ringtonePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(
                    RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java
                )
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            // uri == null means "Silent" was picked; keep our toggle as the on/off control.
            vm.updateSettings(settings.copy(alarmSoundUri = uri?.toString()))
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Настройки",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // --- Сигнал ---
        SectionHeader("Сигнал")
        SoftCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(8.dp)) {
            ToggleRow(
                title = "Вибрация",
                subtitle = "Вибрировать, когда отдых закончился",
                checked = settings.vibrate,
                onChange = { vm.updateSettings(settings.copy(vibrate = it)) }
            )
            ToggleRow(
                title = "Звук",
                subtitle = "Звуковой сигнал в конце отдыха",
                checked = settings.sound,
                onChange = { vm.updateSettings(settings.copy(sound = it)) }
            )
            ActionRow(
                title = "Звук сигнала",
                value = ringtoneName(context, settings.alarmSoundUri),
                enabled = settings.sound,
                showDivider = false,
                onClick = {
                    val current = settings.alarmSoundUri?.let { runCatching { Uri.parse(it) }.getOrNull() }
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Звук сигнала")
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                        putExtra(
                            RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        )
                        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, current)
                    }
                    ringtonePicker.launch(intent)
                }
            )
        }

        // --- Таймер ---
        SectionHeader("Таймер")
        SoftCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(8.dp)) {
            ToggleRow(
                title = "Работать в фоне",
                subtitle = "Досчитать и уведомить, даже если приложение свёрнуто",
                checked = settings.runInBackground,
                onChange = { vm.updateSettings(settings.copy(runInBackground = it)) }
            )
            ToggleRow(
                title = "Держать экран активным",
                subtitle = "Экран не гаснет во время отсчёта",
                checked = settings.keepScreenOn,
                onChange = { vm.updateSettings(settings.copy(keepScreenOn = it)) },
                showDivider = false
            )
        }

        // --- Оформление ---
        SectionHeader("Оформление")
        SoftCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(8.dp)) {
            ThemeRow(
                current = settings.themeMode,
                onSelect = { vm.updateSettings(settings.copy(themeMode = it)) }
            )
        }

        // --- Отчёты ---
        SectionHeader("Отчёты")
        SoftCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(8.dp)) {
            ActionRow(
                title = "Экспорт всех отчётов",
                value = if (reports.isEmpty()) "Нет отчётов" else "${reports.size} шт. · JSON / MD / текст",
                enabled = reports.isNotEmpty(),
                leadingIcon = Icons.Outlined.Share,
                onClick = { showExport = true }
            )
            ActionRow(
                title = "Удалить все отчёты",
                value = "Очистить историю тренировок",
                enabled = reports.isNotEmpty(),
                destructive = true,
                showDivider = false,
                leadingIcon = Icons.Outlined.DeleteSweep,
                onClick = { confirmDeleteAll = true }
            )
        }

        // --- О приложении ---
        SectionHeader("О приложении")
        SoftCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(8.dp)) {
            ActionRow(
                title = "GitHub",
                value = GITHUB_URL,
                enabled = true,
                leadingIcon = Icons.Outlined.Code,
                onClick = { openUrl(context, GITHUB_URL) }
            )
            ActionRow(
                title = "Telegram автора",
                value = TELEGRAM_URL,
                enabled = true,
                showDivider = false,
                leadingIcon = Icons.Outlined.Send,
                onClick = { openUrl(context, TELEGRAM_URL) }
            )
        }

        Text(
            "Workout Studio · версия 1.4",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }

    if (showExport) {
        ExportDialog(
            count = reports.size,
            onDismiss = { showExport = false },
            onExport = { format, copy ->
                val content = ReportExporter.build(vm.allReports(), format)
                if (copy) ReportExporter.copyToClipboard(context, content)
                else ReportExporter.share(context, content, format)
                showExport = false
            }
        )
    }

    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Удалить все отчёты?") },
            text = { Text("Будут удалены все ${reports.size} записей без возможности восстановления.") },
            confirmButton = {
                TextButton(onClick = { confirmDeleteAll = false; vm.deleteAllReports() }) {
                    Text("Удалить всё", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteAll = false }) { Text("Отмена") }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

@Composable
private fun ThemeRow(current: String, onSelect: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp)) {
        Text(
            "Тема",
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeChip("Системная", current == ThemeMode.SYSTEM) { onSelect(ThemeMode.SYSTEM) }
            ThemeChip("Светлая", current == ThemeMode.LIGHT) { onSelect(ThemeMode.LIGHT) }
            ThemeChip("Тёмная", current == ThemeMode.DARK) { onSelect(ThemeMode.DARK) }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.ThemeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val bg = if (selected) scheme.primary else scheme.surfaceVariant
    val fg = if (selected) scheme.onPrimary else scheme.onSurface
    Text(
        label,
        fontSize = 14.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        color = fg,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick)
            .background(bg, androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
            .padding(vertical = 12.dp)
    )
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
                    "Все $count тренировок будут собраны в один документ.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(12.dp))
                Text("Формат", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                ReportExporter.Format.entries.forEach { f ->
                    FormatOption(label = f.label, selected = format == f) { format = f }
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
private fun FormatOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.height(0.dp))
        Text(label, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    showDivider: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
private fun ActionRow(
    title: String,
    value: String,
    enabled: Boolean,
    showDivider: Boolean = true,
    destructive: Boolean = false,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.4f
    val titleColor = if (destructive) MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (enabled) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            Icon(
                leadingIcon,
                contentDescription = null,
                tint = titleColor.copy(alpha = alpha),
                modifier = Modifier.padding(end = 14.dp)
            )
        }
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = titleColor.copy(alpha = alpha)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                value,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
            )
        }
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

private fun ringtoneName(context: android.content.Context, uriString: String?): String {
    val uri = uriString?.let { runCatching { Uri.parse(it) }.getOrNull() }
        ?: return "По умолчанию"
    return runCatching {
        RingtoneManager.getRingtone(context, uri)?.getTitle(context)
    }.getOrNull() ?: "Выбранный звук"
}
