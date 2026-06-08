package com.nikita.workoutstudio.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import com.nikita.workoutstudio.timer.RestTimerController
import com.nikita.workoutstudio.ui.AppViewModel

@Composable
fun TimerScreen(
    vm: AppViewModel,
    state: RestTimerController.State,
    onClose: () -> Unit
) {
    val settings = vm.settings
    // Keep screen on while the timer screen is visible, if enabled in settings.
    val view = LocalView.current
    val keepOn = settings.value.keepScreenOn
    DisposableEffect(keepOn) {
        view.keepScreenOn = keepOn
        onDispose { view.keepScreenOn = false }
    }

    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top bar: exercise name + close
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                if (state.exerciseCount > 1) {
                    Text(
                        "Упражнение ${state.exerciseIndex}/${state.exerciseCount}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onSurfaceVariant
                    )
                }
                Text(
                    state.exerciseName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onBackground
                )
                Text(
                    "${state.reps} повторений",
                    fontSize = 15.sp,
                    color = scheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onClose) { Text("Закрыть") }
        }

        Spacer(Modifier.height(8.dp))
        SetDots(current = state.currentSet, total = state.totalSets)
        Spacer(Modifier.weight(1f))

        when (state.phase) {
            RestTimerController.Phase.DONE -> DoneContent(
                textColor = scheme.onBackground,
                wholeWorkout = state.exerciseCount > 1
            )
            else -> CountdownContent(state = state)
        }

        // "Next up" hint only matters while resting on the last set of an exercise
        // that has a follow-up in the chain.
        if (state.phase == RestTimerController.Phase.RESTING &&
            state.currentSet >= state.totalSets &&
            state.nextExerciseName != null
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                "Далее: ${state.nextExerciseName}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = scheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.weight(1f))
        Controls(vm = vm, state = state, onClose = onClose)
    }
}

@Composable
private fun SetDots(current: Int, total: Int) {
    val scheme = MaterialTheme.colorScheme
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (i in 1..total) {
            val done = i < current
            val active = i == current
            val color = when {
                active -> scheme.primary
                done -> scheme.onSurfaceVariant
                else -> scheme.outline
            }
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(if (active) 28.dp else 16.dp)
                    .background(color, RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
private fun CountdownContent(state: RestTimerController.State) {
    val scheme = MaterialTheme.colorScheme
    val resting = state.phase == RestTimerController.Phase.RESTING
    val total = state.restSeconds.coerceAtLeast(1)
    val progress = if (resting) state.remainingSeconds.toFloat() / total else 1f
    val animated by animateFloatAsState(targetValue = progress, label = "ring")

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(280.dp)) {
        val ringColor = scheme.primary
        val trackColor = scheme.outline
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 18.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * animated.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formatTime(if (resting) state.remainingSeconds else state.restSeconds),
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
                color = scheme.onBackground
            )
            Text(
                text = if (resting) "отдых" else "готов к подходу",
                fontSize = 16.sp,
                color = scheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DoneContent(textColor: Color, wholeWorkout: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Готово!", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = textColor)
        Spacer(Modifier.height(8.dp))
        Text(
            if (wholeWorkout) "Тренировка завершена" else "Все подходы выполнены",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun Controls(
    vm: AppViewModel,
    state: RestTimerController.State,
    onClose: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    when (state.phase) {
        RestTimerController.Phase.READY -> ReadyControls(vm = vm, state = state)
        RestTimerController.Phase.RESTING -> {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { vm.addRestSeconds(15) },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) { Text("+15 сек", fontSize = 16.sp) }
                    OutlinedButton(
                        onClick = { vm.skipRest() },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) { Text("Пропустить", fontSize = 16.sp) }
                }
                TextButton(
                    onClick = onClose,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Отменить тренировку", color = scheme.error) }
            }
        }
        RestTimerController.Phase.DONE -> {
            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth().height(64.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = scheme.primary,
                    contentColor = scheme.onPrimary
                )
            ) { Text("Завершить", fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }
        }
        else -> {}
    }
}

@Composable
private fun ReadyControls(vm: AppViewModel, state: RestTimerController.State) {
    val scheme = MaterialTheme.colorScheme
    // Default the rep count to the planned target; the user nudges it if they did fewer/more.
    var actual by remember(state.exerciseName, state.currentSet) { mutableStateOf(state.reps) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            "Сколько повторений сделал?",
            fontSize = 14.sp,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepperButton("–", onClick = { if (actual > 0) actual-- })
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "$actual",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onBackground
                )
                Text(
                    "цель: ${state.reps}",
                    fontSize = 13.sp,
                    color = scheme.onSurfaceVariant
                )
            }
            StepperButton("+", onClick = { actual++ })
        }
        Button(
            onClick = { vm.startRest(actual) },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = scheme.primary,
                contentColor = scheme.onPrimary
            )
        ) {
            Text(
                "Готово — начать отдых",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun StepperButton(label: String, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.size(64.dp),
        shape = RoundedCornerShape(18.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
    ) {
        Text(label, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = scheme.onSurface)
    }
}

private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}
