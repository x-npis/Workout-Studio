package com.nikita.workoutstudio.timer

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import com.nikita.workoutstudio.model.Exercise
import com.nikita.workoutstudio.model.TimerSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

object RestTimerController {

    enum class Phase { IDLE, READY, RESTING, DONE }

    data class State(
        val phase: Phase = Phase.IDLE,
        val exerciseId: Long = -1L,
        val exerciseName: String = "",
        val reps: Int = 0,
        val currentSet: Int = 1,
        val totalSets: Int = 3,
        val restSeconds: Int = 0,
        val remainingSeconds: Int = 0
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tickJob: Job? = null
    private var endAtElapsed: Long = 0L

    private lateinit var appContext: Context
    private var settings: TimerSettings = TimerSettings()

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun updateSettings(newSettings: TimerSettings) {
        settings = newSettings
    }

    /** Open the timer for an exercise, positioned at the first set, no rest running yet. */
    fun prepare(exercise: Exercise, currentSettings: TimerSettings) {
        settings = currentSettings
        cancelTick()
        RestNotifications.cancelAll(appContext)
        _state.value = State(
            phase = Phase.READY,
            exerciseId = exercise.id,
            exerciseName = exercise.name,
            reps = exercise.reps,
            currentSet = 1,
            totalSets = exercise.sets.coerceAtLeast(1),
            restSeconds = exercise.restSeconds,
            remainingSeconds = exercise.restSeconds
        )
    }

    /** User finished the current set — start the rest countdown. */
    fun startRest() {
        val s = _state.value
        if (s.phase != Phase.READY) return
        beginCountdown(s.restSeconds)
    }

    fun addSeconds(extra: Int) {
        if (_state.value.phase != Phase.RESTING) return
        endAtElapsed += extra * 1000L
        val remaining = ((endAtElapsed - SystemClock.elapsedRealtime()) / 1000L).toInt().coerceAtLeast(0)
        _state.value = _state.value.copy(remainingSeconds = remaining)
        pushOngoingNotification()
    }

    /** Skip the rest right now and advance to the next set (no alert). */
    fun skip() {
        if (_state.value.phase != Phase.RESTING) return
        cancelTick()
        advanceAfterRest(signal = false)
    }

    /** Abort the whole session. */
    fun cancel() {
        cancelTick()
        stopService()
        RestNotifications.cancelAll(appContext)
        _state.value = State(phase = Phase.IDLE)
    }

    private fun beginCountdown(seconds: Int) {
        cancelTick()
        endAtElapsed = SystemClock.elapsedRealtime() + seconds * 1000L
        _state.value = _state.value.copy(phase = Phase.RESTING, remainingSeconds = seconds)
        startService()
        pushOngoingNotification()
        tickJob = scope.launch {
            while (true) {
                val remaining = ((endAtElapsed - SystemClock.elapsedRealtime()) / 1000L).toInt()
                if (remaining <= 0) {
                    _state.value = _state.value.copy(remainingSeconds = 0)
                    onRestFinished()
                    break
                }
                if (remaining != _state.value.remainingSeconds) {
                    _state.value = _state.value.copy(remainingSeconds = remaining)
                    pushOngoingNotification()
                }
                delay(200L)
            }
        }
    }

    private fun onRestFinished() {
        Signaler.fire(appContext, sound = settings.sound, vibrate = settings.vibrate)
        val s = _state.value
        val nextSet = s.currentSet + 1
        if (settings.runInBackground) {
            RestNotifications.showDone(
                appContext, s.exerciseName,
                set = nextSet.coerceAtMost(s.totalSets), total = s.totalSets
            )
        }
        advanceAfterRest(signal = true)
    }

    private fun advanceAfterRest(signal: Boolean) {
        val s = _state.value
        stopService()
        RestNotifications.cancelAll(appContext)
        if (s.currentSet >= s.totalSets) {
            _state.value = s.copy(phase = Phase.DONE, remainingSeconds = 0)
        } else {
            _state.value = s.copy(
                phase = Phase.READY,
                currentSet = s.currentSet + 1,
                remainingSeconds = s.restSeconds
            )
        }
    }

    private fun pushOngoingNotification() {
        if (!settings.runInBackground) return
        if (_state.value.phase != Phase.RESTING) return
        RestTimerService.updateNotification(appContext)
    }

    private fun startService() {
        if (!settings.runInBackground) return
        val intent = Intent(appContext, RestTimerService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.startForegroundService(intent)
        } else {
            appContext.startService(intent)
        }
    }

    private fun stopService() {
        val intent = Intent(appContext, RestTimerService::class.java)
        appContext.stopService(intent)
    }

    private fun cancelTick() {
        tickJob?.cancel()
        tickJob = null
    }
}
