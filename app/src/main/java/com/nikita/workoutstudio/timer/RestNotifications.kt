package com.nikita.workoutstudio.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.nikita.workoutstudio.MainActivity
import com.nikita.workoutstudio.R

object RestNotifications {

    const val CHANNEL_ID = "rest_timer"
    const val ONGOING_ID = 1001
    const val DONE_ID = 1002

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.rest_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.rest_channel_desc)
            enableVibration(false)
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    private fun contentIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun buildOngoing(context: Context, exerciseName: String, remaining: Int, set: Int, total: Int): Notification {
        ensureChannel(context)
        val mm = remaining / 60
        val ss = remaining % 60
        val time = "%d:%02d".format(mm, ss)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle("$exerciseName · подход $set/$total")
            .setContentText("Отдых: осталось $time")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(contentIntent(context))
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .build()
    }

    fun showDone(context: Context, exerciseName: String, set: Int, total: Int) {
        ensureChannel(context)
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle("Отдых окончен")
            .setContentText("$exerciseName · можно начинать подход $set/$total")
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(contentIntent(context))
            .build()
        manager.notify(DONE_ID, notification)
    }

    fun cancelAll(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.cancel(ONGOING_ID)
        manager.cancel(DONE_ID)
    }
}
