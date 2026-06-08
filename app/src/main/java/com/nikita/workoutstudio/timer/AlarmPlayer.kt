package com.nikita.workoutstudio.timer

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Plays a looping, alarm-stream sound plus repeating vibration when rest ends.
 * Uses USAGE_ALARM so it is NOT silenced by the phone's mute/vibrate ringer mode
 * (alarm volume is a separate channel on Android, including Samsung).
 */
object AlarmPlayer {

    private const val AUTO_STOP_MS = 30_000L

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private var autoStopJob: Job? = null

    fun start(context: Context, sound: Boolean, vibrate: Boolean) {
        stop()
        if (sound) startSound(context)
        if (vibrate) startVibration(context)
        if (sound || vibrate) {
            autoStopJob = scope.launch {
                delay(AUTO_STOP_MS)
                stop()
            }
        }
    }

    fun stop() {
        autoStopJob?.cancel()
        autoStopJob = null
        try {
            player?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (e: Exception) {
            // ignore: player may already be released
        }
        player = null
        vibrator?.cancel()
        vibrator = null
    }

    private fun startSound(context: Context) {
        val uri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return
        try {
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context, uri)
                isLooping = true
                setOnPreparedListener { it.start() }
                prepareAsync()
            }
        } catch (e: Exception) {
            // Fallback: a one-shot ringtone is better than a crash.
            player = null
        }
    }

    private fun startVibration(context: Context) {
        val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        } ?: return
        if (!vib.hasVibrator()) return
        vibrator = vib
        val pattern = longArrayOf(0, 500, 400, 500, 400, 500, 400)
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .build()
        // repeat from index 0 → keeps buzzing until stop()
        vib.vibrate(VibrationEffect.createWaveform(pattern, 0), attrs)
    }
}
