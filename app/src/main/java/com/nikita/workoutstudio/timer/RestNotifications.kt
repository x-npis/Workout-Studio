package com.nikita.workoutstudio.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.nikita.workoutstudio.AlarmActivity
import com.nikita.workoutstudio.MainActivity
import com.nikita.workoutstudio.R

object RestNotifications {

    const val CHANNEL_ID = "rest_timer"
    // Bumped to _v2 because notification channels are immutable once created:
    // older installs already had "rest_alarm" with different settings, so a new
    // id is required for the high-importance heads-up behaviour to take effect.
    const val ALARM_CHANNEL_ID = "rest_alarm_v2"
    private const val LEGACY_ALARM_CHANNEL_ID = "rest_alarm"
    const val ONGOING_ID = 1001
    const val ALARM_ID = 1003

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val ongoing = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.rest_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.rest_channel_desc)
                enableVibration(false)
                setSound(null, null)
            }
            manager.createNotificationChannel(ongoing)
        }

        // Drop the stale v1 channel so it doesn't linger in system settings.
        if (manager.getNotificationChannel(LEGACY_ALARM_CHANNEL_ID) != null) {
            manager.deleteNotificationChannel(LEGACY_ALARM_CHANNEL_ID)
        }

        if (manager.getNotificationChannel(ALARM_CHANNEL_ID) == null) {
            // No channel sound/vibration here: AlarmPlayer owns the alarm-stream
            // audio so it works even in mute mode. Channel stays silent to avoid
            // a doubled notification beep.
            val alarm = NotificationChannel(
                ALARM_CHANNEL_ID,
                context.getString(R.string.alarm_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.alarm_channel_desc)
                enableVibration(false)
                setSound(null, null)
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(alarm)
        }
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

    private fun stopAlarmIntent(context: Context): PendingIntent {
        val intent = Intent(context, AlarmActionReceiver::class.java).apply {
            action = AlarmActionReceiver.ACTION_STOP_ALARM
        }
        return PendingIntent.getBroadcast(
            context, 1, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    // Kept for a future full-screen (over-the-lock-screen) alarm mode. The
    // heads-up notification below no longer wires this up, so AlarmActivity is
    // currently launched only if this is reattached to a notification.
    @Suppress("unused")
    private fun fullScreenIntent(context: Context, title: String, text: String): PendingIntent {
        val intent = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmActivity.EXTRA_TITLE, title)
            putExtra(AlarmActivity.EXTRA_TEXT, text)
        }
        return PendingIntent.getActivity(
            context, 2, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun buildOngoing(context: Context, exerciseName: String, remaining: Int, set: Int, total: Int): Notification {
        ensureChannels(context)
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

    /**
     * Alarm-style heads-up notification shown when rest ends. Behaves like the
     * system clock's timer alert: pops as a heads-up card, stays pinned in the
     * shade (can't be swiped away), and carries a single Dismiss action.
     * Tapping the body opens the app; Dismiss silences the AlarmPlayer.
     */
    fun showAlarm(context: Context, title: String, text: String) {
        ensureChannels(context)
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val notification = NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(title)
            .setContentText(text)
            // Stays in the shade until the user acts on it (no auto-dismiss,
            // not swipe-dismissable) — just like an alarm/timer alert.
            .setAutoCancel(false)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // Tapping the notification body opens the app (MainActivity.onResume
            // silences the alarm). No full-screen intent: we deliberately want a
            // plain heads-up card, not an over-the-lock-screen panel.
            .setContentIntent(contentIntent(context))
            .addAction(0, "Dismiss", stopAlarmIntent(context))
            .build()
        manager.notify(ALARM_ID, notification)
    }

    fun cancelOngoing(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.cancel(ONGOING_ID)
    }

    fun cancelAlarm(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.cancel(ALARM_ID)
    }

    fun cancelAll(context: Context) {
        cancelOngoing(context)
        cancelAlarm(context)
    }
}
