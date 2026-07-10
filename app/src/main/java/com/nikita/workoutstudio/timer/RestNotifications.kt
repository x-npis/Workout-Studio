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
    // Bumped whenever the alarm channel's settings change: notification channels
    // are immutable once created, so a new id is the only way to apply changes on
    // installs that already created an earlier version. v3 enables channel
    // vibration, which One UI (and stock Android) treat as a signal to actually
    // pop the notification as heads-up instead of a silent shade entry.
    const val ALARM_CHANNEL_ID = "rest_alarm_v3"
    private val LEGACY_ALARM_CHANNEL_IDS = listOf("rest_alarm", "rest_alarm_v2")
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

        // Drop stale earlier alarm channels so they don't linger in settings.
        LEGACY_ALARM_CHANNEL_IDS.forEach { legacy ->
            if (manager.getNotificationChannel(legacy) != null) {
                manager.deleteNotificationChannel(legacy)
            }
        }

        if (manager.getNotificationChannel(ALARM_CHANNEL_ID) == null) {
            // Channel is silent (no sound): AlarmPlayer owns the alarm-stream audio
            // so it rings even in mute mode, and we avoid a doubled beep. We DO
            // enable channel vibration though — it's the reliable signal that makes
            // One UI / Android surface this as a heads-up pop rather than a quiet
            // shade entry. The buzz here is separate from AlarmPlayer's own loop.
            val alarm = NotificationChannel(
                ALARM_CHANNEL_ID,
                context.getString(R.string.alarm_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.alarm_channel_desc)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 200, 250)
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

    // Used as a heads-up hint in showAlarm(): on Android 14+ without the
    // USE_FULL_SCREEN_INTENT grant this does NOT open AlarmActivity, it just
    // nudges the system into showing a proper heads-up pop. If a future build
    // requests that permission, this same intent brings back the over-the-lock
    // full-screen panel for free.
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
     * system clock's timer alert: pops as a heads-up card and carries a single
     * Dismiss action. Tapping the body opens the app; Dismiss silences the
     * AlarmPlayer.
     *
     * Why NOT setOngoing(true): One UI (and Android in general) demote ongoing
     * notifications to a quiet, non-popping shade entry — that was the "thin"
     * behaviour we saw. To reliably pop as heads-up we drop ongoing and instead
     * rely on IMPORTANCE_HIGH + CATEGORY_ALARM + a fullScreenIntent hint (which,
     * without the USE_FULL_SCREEN_INTENT grant, degrades to a strong heads-up
     * rather than opening a window) + channel vibration.
     */
    fun showAlarm(context: Context, title: String, text: String) {
        ensureChannels(context)
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val notification = NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(title)
            .setContentText(text)
            // BigTextStyle makes the notification render EXPANDED by default, so
            // the Dismiss action is visible immediately without the user having to
            // pull the card open.
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            // Don't auto-dismiss on tap; not ongoing (see kdoc) so it can pop.
            .setAutoCancel(false)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // Vibrate hint on the notification too — another heads-up trigger.
            .setVibrate(longArrayOf(0, 250, 200, 250))
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
            // Tapping the body opens the app (MainActivity.onResume silences the
            // alarm). The fullScreenIntent is a heads-up hint: on Android 14+
            // without the special grant it does NOT open the panel, it just makes
            // the system surface a proper heads-up pop.
            .setContentIntent(contentIntent(context))
            .setFullScreenIntent(fullScreenIntent(context, title, text), true)
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
