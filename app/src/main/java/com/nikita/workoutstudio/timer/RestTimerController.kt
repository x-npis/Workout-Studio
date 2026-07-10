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
        val nextExerciseName: String? = null,
        // True while the user is reviewing an already-completed set (not the live frontier).
        val browsing: Boolean = false,
        // Reps recorded for the set being reviewed (only meaningful while browsing).
        val loggedReps: Int = 0,
        // Whether the back/forward review buttons should be enabled.
        val canGoBack: Boolean = false,
        val canGoForward: Boolean = false
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

    // Review cursor: index into completedPositions() while the user is browsing
    // already-finished sets; -1 means we're on the live frontier (not browsing).
    private var browseCursor: Int = -1
    // The live frontier state stashed while browsing, restored when we step back to it.
    private var savedFrontier: State? = null

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
        browseCursor = -1
        savedFrontier = null
        sessionStartedAt = System.currentTimeMillis()
        reportSaved = false
        emitReady(firstSet = true)
    }

    /**
     * User finished the current set with [actualReps] reps done — log it and
     * start the rest countdown.
     */
    fun startRest(actualReps: Int) {
        if (_state.value.phase != Phase.READY || _state.value.browsing) return
        AlarmPlayer.stop()
        RestNotifications.cancelAlarm(appContext)
        logSet(actualReps)
        val s = _state.value
        // After the very last set of the whole session there is nothing to rest for:
        // skip the countdown (and its alarm) entirely and finish straight away.
        if (s.currentSet >= s.totalSets && index >= queue.size - 1) {
            cancelTick()
            stopService()
            RestNotifications.cancelOngoing(appContext)
            saveReport()
            _state.value = s.copy(phase = Phase.DONE, remainingSeconds = 0)
        } else {
            beginCountdown(s.restSeconds)
        }
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
        browseCursor = -1
        savedFrontier = null
        _state.value = State(phase = Phase.IDLE)
    }

    // ---- Reviewing already-completed sets -----------------------------------

    /**
     * All finished sets in chronological order as (exerciseIndex, setNumber) pairs.
     * The live frontier sits right after the last entry.
     */
    private fun completedPositions(): List<Pair<Int, Int>> {
        val result = mutableListOf<Pair<Int, Int>>()
        for (exIdx in 0..index) {
            val done = setLog[exIdx]?.size ?: 0
            for (setNo in 1..done) result.add(exIdx to setNo)
        }
        return result
    }

    /** Step one completed set backwards (entering review mode on the first press). */
    fun browseBack() {
        if (_state.value.phase != Phase.READY) return
        val positions = completedPositions()
        if (positions.isEmpty()) return
        when {
            browseCursor < 0 -> {
                savedFrontier = _state.value
                browseCursor = positions.size - 1
            }
            browseCursor > 0 -> browseCursor -= 1
            else -> return
        }
        emitBrowse(positions)
    }

    /** Step one completed set forward; stepping past the last returns to the live frontier. */
    fun browseForward() {
        if (browseCursor < 0) return
        val positions = completedPositions()
        if (browseCursor >= positions.size - 1) {
            exitBrowse()
        } else {
            browseCursor += 1
            emitBrowse(positions)
        }
    }

    /** Overwrite the recorded reps of the set currently under review. */
    fun editBrowsedReps(reps: Int) {
        if (!_state.value.browsing || browseCursor < 0) return
        val positions = completedPositions()
        val (exIdx, setNo) = positions.getOrNull(browseCursor) ?: return
        val list = setLog[exIdx] ?: return
        val entry = list.getOrNull(setNo - 1) ?: return
        val fixed = reps.coerceAtLeast(0)
        list[setNo - 1] = entry.copy(actualReps = fixed)
        _state.value = _state.value.copy(loggedReps = fixed)
    }

    /**
     * Rewind the live session to the set under review: everything recorded at and
     * after this point is erased, and the workout continues by redoing this set.
     */
    fun rewindHere() {
        if (!_state.value.browsing || browseCursor < 0) return
        val positions = completedPositions()
        val (exIdx, setNo) = positions.getOrNull(browseCursor) ?: return
        // Drop this set's own entry and every later one across all exercises.
        setLog[exIdx]?.let { list -> while (list.size >= setNo) list.removeAt(list.size - 1) }
        setLog.keys.filter { it > exIdx }.forEach { setLog.remove(it) }
        index = exIdx
        browseCursor = -1
        savedFrontier = null
        reportSaved = false
        val ex = queue[exIdx]
        _state.value = State(
            phase = Phase.READY,
            exerciseName = ex.name,
            reps = ex.reps,
            currentSet = setNo,
            totalSets = ex.sets.coerceAtLeast(1),
            restSeconds = ex.restSeconds,
            remainingSeconds = ex.restSeconds,
            exerciseIndex = exIdx + 1,
            exerciseCount = queue.size,
            nextExerciseName = queue.getOrNull(exIdx + 1)?.name,
            canGoBack = completedPositions().isNotEmpty()
        )
    }

    private fun emitBrowse(positions: List<Pair<Int, Int>>) {
        val (exIdx, setNo) = positions[browseCursor]
        val ex = queue[exIdx]
        val logged = setLog[exIdx]?.getOrNull(setNo - 1)?.actualReps ?: 0
        _state.value = _state.value.copy(
            phase = Phase.READY,
            browsing = true,
            exerciseName = ex.name,
            reps = ex.reps,
            currentSet = setNo,
            totalSets = ex.sets.coerceAtLeast(1),
            restSeconds = ex.restSeconds,
            remainingSeconds = ex.restSeconds,
            exerciseIndex = exIdx + 1,
            exerciseCount = queue.size,
            nextExerciseName = queue.getOrNull(exIdx + 1)?.name,
            loggedReps = logged,
            canGoBack = browseCursor > 0,
            canGoForward = true
        )
    }

    private fun exitBrowse() {
        val frontier = savedFrontier
        browseCursor = -1
        savedFrontier = null
        if (frontier != null) {
            _state.value = frontier.copy(
                browsing = false,
                loggedReps = 0,
                canGoBack = completedPositions().isNotEmpty(),
                canGoForward = false
            )
        }
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
            nextExerciseName = queue.getOrNull(index + 1)?.name,
            canGoBack = completedPositions().isNotEmpty()
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
        AlarmPlayer.start(
            appContext,
            sound = settings.sound,
            vibrate = settings.vibrate,
            soundUri = settings.alarmSoundUri
        )
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
                    remainingSeconds = s.restSeconds,
                    canGoBack = completedPositions().isNotEmpty()
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
