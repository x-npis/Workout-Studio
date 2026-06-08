package com.nikita.workoutstudio.timer

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import com.nikita.workoutstudio.data.ReportRepository
import com.nikita.workoutstudio.model.Exercise
import com.nikita.workoutstudio.model.ExerciseReport
import com.nikita.workoutstudio.model.SetEntry
import com.nikita.workoutstudio.model.TimerSettings
import com.nikita.workoutstudio.model.WorkoutReport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object RestTimerController {

    enum class Phase { IDLE, READY, RESTING, DONE }

    data class State(
        val phase: Phase = Phase.IDLE,
        val exerciseName: String = "",
        val reps: Int = 0,
        val currentSet: Int = 1,
        val totalSets: Int = 3,
        val restSeconds: Int = 0,
        val remainingSeconds: Int = 0,
        // Chain progress (1-based index of the current exercise within the session).
        val exerciseIndex: Int = 1,
        val exerciseCount: Int = 1,
        // Name of the exercise that comes after the current one finishes, if any.
        val nextExerciseName: String? = null
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tickJob: Job? = null
    private var endAtElapsed: Long = 0L

    private lateinit var appContext: Context
    private var settings: TimerSettings = TimerSettings()
    private var reportRepository: ReportRepository? = null

    // The session queue and our position in it.
    private var queue: List<Exercise> = emptyList()
    private var index: Int = 0

    // Report accumulation for the current session.
    private var sessionStartedAt: Long = 0L
    private val setLog = mutableMapOf<Int, MutableList<SetEntry>>()
    private var reportSaved = false

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun attachReportRepository(repo: ReportRepository) {
        reportRepository = repo
    }

    fun updateSettings(newSettings: TimerSettings) {
        settings = newSettings
    }

    /** Start a session with a single exercise. */
    fun startSingle(exercise: Exercise, currentSettings: TimerSettings) {
        startSession(listOf(exercise), currentSettings)
    }

    /** Start a chained session over several exercises, run back to back. */
    fun startSession(exercises: List<Exercise>, currentSettings: TimerSettings) {
        if (exercises.isEmpty()) return
        settings = currentSettings
        cancelTick()
        AlarmPlayer.stop()
        RestNotifications.cancelAll(appContext)
        queue = exercises
        index = 0
        setLog.clear()
        sessionStartedAt = System.currentTimeMillis()
        reportSaved = false
        emitReady(firstSet = true)
    }

    /**
     * User finished the current set with [actualReps] reps done — log it and
     * start the rest countdown.
     */
    fun startRest(actualReps: Int) {
        if (_state.value.phase != Phase.READY) return
        AlarmPlayer.stop()
        RestNotifications.cancelAlarm(appContext)
        logSet(actualReps)
        beginCountdown(_state.value.restSeconds)
    }

    fun addSeconds(extra: Int) {
        if (_state.value.phase != Phase.RESTING) return
        endAtElapsed += extra * 1000L
        val remaining = ((endAtElapsed - SystemClock.elapsedRealtime()) / 1000L).toInt().coerceAtLeast(0)
        _state.value = _state.value.copy(remainingSeconds = remaining)
        pushOngoingNotification()
    }

    /** Skip the rest right now and advance (no alert). */
    fun skip() {
        if (_state.value.phase != Phase.RESTING) return
        AlarmPlayer.stop()
        RestNotifications.cancelAlarm(appContext)
        cancelTick()
        advanceAfterRest()
    }

    /** Stop the alarm sound without changing the workout state. */
    fun stopAlarm() {
        AlarmPlayer.stop()
        RestNotifications.cancelAlarm(appContext)
    }

    /** Abort the whole session (saves whatever was logged so far). */
    fun cancel() {
        cancelTick()
        AlarmPlayer.stop()
        stopService()
        RestNotifications.cancelAll(appContext)
        saveReport()
        queue = emptyList()
        index = 0
        setLog.clear()
        _state.value = State(phase = Phase.IDLE)
    }

    private fun logSet(actualReps: Int) {
        val s = _state.value
        val target = queue.getOrNull(index)?.reps ?: s.reps
        val list = setLog.getOrPut(index) { mutableListOf() }
        list.add(SetEntry(actualReps = actualReps.coerceAtLeast(0), targetReps = target))
    }

    private fun emitReady(firstSet: Boolean) {
        val current = queue[index]
        val prevSet = if (firstSet) 1 else _state.value.currentSet
        _state.value = State(
            phase = Phase.READY,
            exerciseName = current.name,
            reps = current.reps,
            currentSet = prevSet,
            totalSets = current.sets.coerceAtLeast(1),
            restSeconds = current.restSeconds,
            remainingSeconds = current.restSeconds,
            exerciseIndex = index + 1,
            exerciseCount = queue.size,
            nextExerciseName = queue.getOrNull(index + 1)?.name
        )
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
        stopService()
        RestNotifications.cancelOngoing(appContext)
        // Alarm-stream sound + vibration: rings even when the phone is on mute.
        AlarmPlayer.start(appContext, sound = settings.sound, vibrate = settings.vibrate)
        val (title, text) = doneMessage()
        // Alarm-style panel so it surfaces over other apps / the lock screen.
        if (settings.runInBackground) {
            RestNotifications.showAlarm(appContext, title, text)
        }
        advanceAfterRest()
    }

    /** Compute the post-rest target without mutating state, for the notification text. */
    private fun doneMessage(): Pair<String, String> {
        val s = _state.value
        return when {
            s.currentSet < s.totalSets ->
                "Отдых окончен" to "${s.exerciseName} · подход ${s.currentSet + 1}/${s.totalSets}"
            index < queue.size - 1 -> {
                val next = queue[index + 1]
                "Отдых окончен" to "Далее: ${next.name} · подход 1/${next.sets.coerceAtLeast(1)}"
            }
            else -> "Тренировка завершена" to "Все упражнения выполнены"
        }
    }

    private fun advanceAfterRest() {
        stopService()
        RestNotifications.cancelOngoing(appContext)
        val s = _state.value
        when {
            // More sets of the current exercise.
            s.currentSet < s.totalSets -> {
                _state.value = s.copy(
                    phase = Phase.READY,
                    currentSet = s.currentSet + 1,
                    remainingSeconds = s.restSeconds
                )
            }
            // Move on to the next exercise in the chain.
            index < queue.size - 1 -> {
                index += 1
                emitReady(firstSet = true)
            }
            // Whole session finished.
            else -> {
                saveReport()
                _state.value = s.copy(phase = Phase.DONE, remainingSeconds = 0)
            }
        }
    }

    private fun saveReport() {
        if (reportSaved) return
        val repo = reportRepository ?: return
        val exercises = queue.mapIndexedNotNull { i, ex ->
            val sets = setLog[i]
            if (sets.isNullOrEmpty()) null
            else ExerciseReport(
                name = ex.name,
                restSeconds = ex.restSeconds,
                targetReps = ex.reps,
                plannedSets = ex.sets.coerceAtLeast(1),
                sets = sets.toList()
            )
        }
        if (exercises.isEmpty()) return
        repo.add(
            WorkoutReport(
                id = sessionStartedAt,
                startedAt = sessionStartedAt,
                finishedAt = System.currentTimeMillis(),
                exercises = exercises
            )
        )
        reportSaved = true
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
