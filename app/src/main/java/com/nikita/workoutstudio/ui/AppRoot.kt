package com.nikita.workoutstudio.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nikita.workoutstudio.timer.RestTimerController
import com.nikita.workoutstudio.ui.screens.ExercisesScreen
import com.nikita.workoutstudio.ui.screens.ReportsScreen
import com.nikita.workoutstudio.ui.screens.SettingsScreen
import com.nikita.workoutstudio.ui.screens.TimerScreen

private enum class Tab { Exercises, Reports, Settings }

@Composable
fun AppRoot(vm: AppViewModel = viewModel()) {
    val timerState by vm.timerState.collectAsState()
    val minimized by vm.timerMinimized.collectAsState()
    var tab by remember { mutableStateOf(Tab.Exercises) }

    val sessionActive = timerState.phase != RestTimerController.Phase.IDLE

    // When a session is active and expanded, the timer takes over the screen.
    if (sessionActive && !minimized) {
        BackHandler {
            // Back collapses a running workout to the banner; on the finished
            // screen there's nothing left to run, so it just closes.
            if (timerState.phase == RestTimerController.Phase.DONE) vm.cancelTimer()
            else vm.minimizeTimer()
        }
        TimerScreen(
            vm = vm,
            state = timerState,
            onMinimize = { vm.minimizeTimer() },
            onFinish = { vm.cancelTimer() }
        )
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column {
                // While a session runs in the background, a tappable banner sits
                // above the nav bar so it's always reachable from any tab.
                if (sessionActive) {
                    MiniTimerBar(state = timerState, onClick = { vm.expandTimer() })
                }
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    NavigationBarItem(
                        selected = tab == Tab.Exercises,
                        onClick = { tab = Tab.Exercises },
                        icon = { Icon(Icons.Outlined.FitnessCenter, contentDescription = null) },
                        label = { Text("Упражнения") },
                        colors = navColors()
                    )
                    NavigationBarItem(
                        selected = tab == Tab.Reports,
                        onClick = { tab = Tab.Reports },
                        icon = { Icon(Icons.Outlined.Assessment, contentDescription = null) },
                        label = { Text("Отчёты") },
                        colors = navColors()
                    )
                    NavigationBarItem(
                        selected = tab == Tab.Settings,
                        onClick = { tab = Tab.Settings },
                        icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                        label = { Text("Настройки") },
                        colors = navColors()
                    )
                }
            }
        }
    ) { padding ->
        when (tab) {
            Tab.Exercises -> ExercisesScreen(
                vm = vm,
                modifier = Modifier.fillMaxSize().padding(padding)
            )
            Tab.Reports -> ReportsScreen(
                vm = vm,
                modifier = Modifier.fillMaxSize().padding(padding)
            )
            Tab.Settings -> SettingsScreen(
                vm = vm,
                modifier = Modifier.fillMaxSize().padding(padding)
            )
        }
    }
}

@Composable
private fun MiniTimerBar(state: RestTimerController.State, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val (line1, line2) = miniBarText(state)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(scheme.primary)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                line1,
                color = scheme.onPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (line2.isNotEmpty()) {
                Text(
                    line2,
                    color = scheme.onPrimary.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(
            Icons.Outlined.KeyboardArrowUp,
            contentDescription = "Развернуть",
            tint = scheme.onPrimary
        )
    }
}

private fun miniBarText(state: RestTimerController.State): Pair<String, String> = when (state.phase) {
    RestTimerController.Phase.RESTING -> {
        val m = state.remainingSeconds / 60
        val s = state.remainingSeconds % 60
        state.exerciseName to "Отдых %d:%02d".format(m, s)
    }
    RestTimerController.Phase.READY ->
        state.exerciseName to "Подход ${state.currentSet}/${state.totalSets} · готов"
    RestTimerController.Phase.DONE ->
        "Тренировка завершена" to "Нажми, чтобы закрыть"
    else -> state.exerciseName to ""
}

@Composable
private fun navColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
    selectedTextColor = MaterialTheme.colorScheme.onBackground,
    indicatorColor = MaterialTheme.colorScheme.primary,
    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
)
