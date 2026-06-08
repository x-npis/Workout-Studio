package com.nikita.workoutstudio.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == ACTION_STOP_ALARM) {
            AlarmPlayer.stop()
            RestNotifications.cancelAlarm(context.applicationContext)
        }
    }

    companion object {
        const val ACTION_STOP_ALARM = "com.nikita.workoutstudio.STOP_ALARM"
    }
}
