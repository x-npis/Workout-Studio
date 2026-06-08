package com.nikita.workoutstudio.ui.screens

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nikita.workoutstudio.model.Exercise
import com.nikita.workoutstudio.timer.RestTimerController
import com.nikita.workoutstudio.ui.AppViewModel
import com.nikita.workoutstudio.ui.theme.SoftCard

@Composable
fun ExercisesScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    val exercises by vm.exercises.collectAsState()
    val timerState by vm.timerState.collectAsState()
    val sessionActive = timerState.phase != RestTimerController.Phase.IDLE
    val context = LocalContext.current
    var editing by remember { mutableStateOf<Exercise?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var reorderMode by remember { mutableStateOf(false) }
    var startChooserFor by remember { mutableStateOf<Exercise?>(null) }
    var deleteConfirmFor by remember { mutableStateOf<Exercise?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Упражнения",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                if (reorderMode) {
                    Text(
                        "перестановка",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (sessionActive) {
                Text(
                    "Идёт тренировка. Правки применятся со следующего запуска.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 4.dp)
                )
            }

            if (exercises.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Пока пусто.\nНажми «+», чтобы создать упражнение.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 160.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(exercises, key = { it.id }) { ex ->
                        val pos = exercises.indexOfFirst { it.id == ex.id }
                        ExerciseRow(
                            exercise = ex,
                            reorderMode = reorderMode,
                            canMoveUp = pos > 0,
                            canMoveDown = pos < exercises.size - 1,
                            onClick = {
                                when {
                                    sessionActive -> Toast.makeText(
                                        context,
                                        "Тренировка уже идёт — сначала заверши её",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    // Last exercise: "whole workout from here" == single, so just start it.
                                    pos >= 0 && pos == exercises.size - 1 -> vm.startSingle(ex)
                                    else -> startChooserFor = ex
                                }
                            },
                            onEdit = { editing = ex; showEditor = true },
                            onDuplicate = { vm.duplicateExercise(ex.id) },
                            onDelete = { deleteConfirmFor = ex },
                            onMoveUp = { vm.moveUp(ex.id) },
                            onMoveDown = { vm.moveDown(ex.id) }
                        )
                    }
                }
            }
        }

        // FAB stack: pencil (reorder toggle) above plus (add). Lives inside the
        // already nav-bar-inset area, so it never hides behind the bottom bar.
        Column(
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (exercises.size > 1 || reorderMode) {
                SmallFloatingActionButton(
                    onClick = { reorderMode = !reorderMode },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    Icon(
                        if (reorderMode) Icons.Outlined.Check else Icons.Outlined.Edit,
                        contentDescription = if (reorderMode) "Готово" else "Переставить"
                    )
                }
            }
            FloatingActionButton(
                onClick = { editing = null; showEditor = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Добавить")
            }
        }
    }

    if (showEditor) {
        ExerciseEditorDialog(
            existing = editing,
            onDismiss = { showEditor = false },
            onSave = { name, rest, reps, sets ->
                val current = editing
                if (current == null) {
                    vm.addExercise(name, rest, reps, sets)
                } else {
                    vm.updateExercise(current.copy(name = name, restSeconds = rest, reps = reps, sets = sets))
                }
                showEditor = false
            }
        )
    }

    startChooserFor?.let { ex ->
        val pos = exercises.indexOfFirst { it.id == ex.id }
        val remaining = if (pos >= 0) exercises.size - pos else 1
        StartChooserDialog(
            exercise = ex,
            remainingCount = remaining,
            onSingle = { vm.startSingle(ex); startChooserFor = null },
            onWorkout = { vm.startWorkoutFrom(ex); startChooserFor = null },
            onDismiss = { startChooserFor = null }
        )
    }

    deleteConfirmFor?.let { ex ->
        AlertDialog(
            onDismissRequest = { deleteConfirmFor = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Удалить упражнение?") },
            text = { Text("«${ex.name}» будет удалено без возможности восстановления.") },
            confirmButton = {
                TextButton(onClick = { vm.deleteExercise(ex.id); deleteConfirmFor = null }) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmFor = null }) { Text("Отмена") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExerciseRow(
    exercise: Exercise,
    reorderMode: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }

    Box {
        val rowModifier = if (reorderMode) {
            Modifier.fillMaxWidth()
        } else {
            Modifier.fillMaxWidth().combinedClickable(
                onClick = onClick,
                onLongClick = { menuOpen = true }
            )
        }
        SoftCard(modifier = rowModifier) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        exercise.name,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Отдых ${formatRest(exercise.restSeconds)} · ${exercise.reps} повт. · ${exercise.sets} подх.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (reorderMode) {
                    IconButton(onClick = onMoveUp, enabled = canMoveUp) {
                        Icon(
                            Icons.Outlined.KeyboardArrowUp,
                            contentDescription = "Вверх",
                            tint = if (canMoveUp) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.outline
                        )
                    }
                    IconButton(onClick = onMoveDown, enabled = canMoveDown) {
                        Icon(
                            Icons.Outlined.KeyboardArrowDown,
                            contentDescription = "Вниз",
                            tint = if (canMoveDown) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    Icon(
                        Icons.Outlined.PlayArrow,
                        contentDescription = "Старт",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false }
        ) {
            DropdownMenuItem(
                text = { Text("Изменить") },
                leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                onClick = { menuOpen = false; onEdit() }
            )
            DropdownMenuItem(
                text = { Text("Дублировать") },
                leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                onClick = { menuOpen = false; onDuplicate() }
            )
            DropdownMenuItem(
                text = { Text("Удалить") },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                onClick = { menuOpen = false; onDelete() }
            )
        }
    }
}

@Composable
private fun StartChooserDialog(
    exercise: Exercise,
    remainingCount: Int,
    onSingle: () -> Unit,
    onWorkout: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text("Запустить") },
        text = {
            Column {
                Text("«${exercise.name}»", fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Сделать только это упражнение или пройти всю тренировку отсюда до конца ($remainingCount упр.)?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onWorkout) { Text("Вся тренировка") }
        },
        dismissButton = {
            TextButton(onClick = onSingle) { Text("Только это") }
        }
    )
}

private fun formatRest(seconds: Int): String {
    if (seconds < 60) return "${seconds}с"
    val m = seconds / 60
    val s = seconds % 60
    return if (s == 0) "${m}м" else "${m}м ${s}с"
}

@Composable
private fun ExerciseEditorDialog(
    existing: Exercise?,
    onDismiss: () -> Unit,
    onSave: (name: String, rest: Int, reps: Int, sets: Int) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var rest by remember { mutableStateOf((existing?.restSeconds ?: 60).toString()) }
    var reps by remember { mutableStateOf((existing?.reps ?: 10).toString()) }
    var sets by remember { mutableStateOf((existing?.sets ?: 3).toString()) }

    val nameValid = name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(if (existing == null) "Новое упражнение" else "Изменить упражнение") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    singleLine = true,
                    isError = !nameValid,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = rest,
                        onValueChange = { rest = it.filter { c -> c.isDigit() }.take(4) },
                        label = { Text("Отдых, сек") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = reps,
                        onValueChange = { reps = it.filter { c -> c.isDigit() }.take(4) },
                        label = { Text("Повторы") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = sets,
                    onValueChange = { sets = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text("Подходы") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = nameValid,
                onClick = {
                    onSave(
                        name.trim(),
                        rest.toIntOrNull()?.coerceIn(1, 3600) ?: 60,
                        reps.toIntOrNull()?.coerceIn(1, 999) ?: 10,
                        sets.toIntOrNull()?.coerceIn(1, 20) ?: 3
                    )
                }
            ) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
