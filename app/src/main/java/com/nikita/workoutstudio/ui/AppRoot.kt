package com.nikita.workoutstudio.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
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
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nikita.workoutstudio.timer.RestTimerController
import com.nikita.workoutstudio.ui.screens.ExercisesScreen
import com.nikita.workoutstudio.ui.screens.SettingsScreen
import com.nikita.workoutstudio.ui.screens.TimerScreen

private enum class Tab { Exercises, Settings }

@Composable
fun AppRoot(vm: AppViewModel = viewModel()) {
    val timerState by vm.timerState.collectAsState()
    var tab by remember { mutableStateOf(Tab.Exercises) }

    // When a timer session is active, show it full-screen above the tabs.
    if (timerState.phase != RestTimerController.Phase.IDLE) {
        TimerScreen(vm = vm, state = timerState, onClose = { vm.cancelTimer() })
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = tab == Tab.Exercises,
                    onClick = { tab = Tab.Exercises },
                    icon = { Icon(Icons.Outlined.FitnessCenter, contentDescription = null) },
                    label = { Text("Упражнения") },
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
    ) { padding ->
        when (tab) {
            Tab.Exercises -> ExercisesScreen(
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
private fun navColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
    selectedTextColor = MaterialTheme.colorScheme.onBackground,
    indicatorColor = MaterialTheme.colorScheme.primary,
    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
)
