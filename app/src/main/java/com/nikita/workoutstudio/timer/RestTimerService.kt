package com.nikita.workoutstudio.timer

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

class RestTimerService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startInForeground()
        return START_STICKY
    }

    private fun startInForeground() {
        val s = RestTimerController.state.value
        val notification = RestNotifications.buildOngoing(
            this, s.exerciseName, s.remainingSeconds, s.currentSet, s.totalSets
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                RestNotifications.ONGOING_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(RestNotifications.ONGOING_ID, notification)
        }
    }

    override fun onDestroy() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        super.onDestroy()
    }

    companion object {
        fun updateNotification(context: Context) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as? android.app.NotificationManager ?: return
            val s = RestTimerController.state.value
            val notification = RestNotifications.buildOngoing(
                context, s.exerciseName, s.remainingSeconds, s.currentSet, s.totalSets
            )
            manager.notify(RestNotifications.ONGOING_ID, notification)
        }
    }
}
