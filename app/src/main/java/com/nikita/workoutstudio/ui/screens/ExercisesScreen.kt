package com.nikita.workoutstudio.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.nikita.workoutstudio.model.Exercise
import com.nikita.workoutstudio.ui.AppViewModel
import com.nikita.workoutstudio.ui.theme.SoftCard

@Composable
fun ExercisesScreen(vm: AppViewModel, modifier: Modifier = Modifier) {
    val exercises by vm.exercises.collectAsState()
    var editing by remember { mutableStateOf<Exercise?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = null; showEditor = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("Добавить") }
            )
        }
    ) { padding ->
        Column(modifier = modifier.padding(padding)) {
            Text(
                text = "Упражнения",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp)
            )
            if (exercises.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Пока пусто.\nНажми «Добавить», чтобы создать упражнение.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(exercises, key = { it.id }) { ex ->
                        ExerciseRow(
                            exercise = ex,
                            onPlay = { vm.openTimer(ex) },
                            onEdit = { editing = ex; showEditor = true },
                            onDelete = { vm.deleteExercise(ex.id) }
                        )
                    }
                }
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
}

@Composable
private fun ExerciseRow(
    exercise: Exercise,
    onPlay: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    SoftCard(modifier = Modifier.fillMaxWidth().clickable { onEdit() }) {
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
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "Удалить",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onPlay) {
                Icon(
                    Icons.Outlined.PlayArrow,
                    contentDescription = "Старт",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
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
