package com.nikita.workoutstudio

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nikita.workoutstudio.timer.RestTimerController
import com.nikita.workoutstudio.ui.theme.WorkoutStudioTheme

/**
 * Full-screen alarm panel shown over the lock screen when rest ends. Stays put
 * (like a real alarm) until the user taps a button — it is NOT auto-dismissed.
 */
class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        enableEdgeToEdge()

        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Отдых окончен"
        val text = intent?.getStringExtra(EXTRA_TEXT) ?: ""

        setContent {
            WorkoutStudioTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    AlarmContent(
                        title = title,
                        text = text,
                        onStop = {
                            RestTimerController.stopAlarm()
                            finish()
                        },
                        onOpen = {
                            RestTimerController.stopAlarm()
                            val open = Intent(this, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            }
                            startActivity(open)
                            finish()
                        }
                    )
                }
            }
        }
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguard = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguard?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    companion object {
        const val EXTRA_TITLE = "alarm_title"
        const val EXTRA_TEXT = "alarm_text"
    }
}

@Composable
private fun AlarmContent(
    title: String,
    text: String,
    onStop: () -> Unit,
    onOpen: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            title,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = scheme.onBackground,
            textAlign = TextAlign.Center
        )
        if (text.isNotBlank()) {
            Text(
                text,
                fontSize = 18.sp,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 14.dp)
            )
        }

        Button(
            onClick = onStop,
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(top = 48.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = scheme.primary,
                contentColor = scheme.onPrimary
            )
        ) {
            Text("Остановить", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        }

        OutlinedButton(
            onClick = onOpen,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(top = 16.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Text(
                "Открыть приложение",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = scheme.onSurface
            )
        }
    }
}
