package com.nikita.workoutstudio

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.nikita.workoutstudio.timer.RestTimerController
import com.nikita.workoutstudio.ui.AppRoot
import com.nikita.workoutstudio.ui.theme.WorkoutStudioTheme
import com.nikita.workoutstudio.ui.theme.resolveDarkTheme

class MainActivity : ComponentActivity() {

    private val requestNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        maybeRequestNotificationPermission()
        val settingsFlow = (application as WorkoutApp).settingsRepository.settings
        setContent {
            val settings by settingsFlow.collectAsState()
            WorkoutStudioTheme(darkTheme = resolveDarkTheme(settings.themeMode)) {
                AppRoot()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Opening the app (incl. tapping the alarm notification) silences the alarm.
        RestTimerController.stopAlarm()
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
