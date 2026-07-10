package com.nikita.workoutstudio.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.nikita.workoutstudio.WorkoutApp
import com.nikita.workoutstudio.model.Exercise
import com.nikita.workoutstudio.model.TimerSettings
import com.nikita.workoutstudio.timer.RestTimerController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val application = app as WorkoutApp
    private val exerciseRepo = application.exerciseRepository
    private val settingsRepo = application.settingsRepository
    private val reportRepo = application.reportRepository

    val exercises = exerciseRepo.exercises
    val settings = settingsRepo.settings
    val reports = reportRepo.reports
    val timerState = RestTimerController.state

    // UI-only: whether the running timer is collapsed to a banner so the user
    // can browse the rest of the app while the session keeps going underneath.
    private val _timerMinimized = MutableStateFlow(false)
    val timerMinimized: StateFlow<Boolean> = _timerMinimized.asStateFlow()

    /** True while a workout session is in progress (READY / RESTING / DONE). */
    val isSessionActive: Boolean
        get() = timerState.value.phase != RestTimerController.Phase.IDLE

    fun minimizeTimer() { _timerMinimized.value = true }
    fun expandTimer() { _timerMinimized.value = false }

    fun addExercise(name: String, restSeconds: Int, reps: Int, sets: Int) =
        exerciseRepo.add(name, restSeconds, reps, sets)

    fun updateExercise(exercise: Exercise) = exerciseRepo.update(exercise)

    fun deleteExercise(id: Long) = exerciseRepo.delete(id)

    fun duplicateExercise(id: Long) = exerciseRepo.duplicate(id)

    fun moveUp(id: Long) = exerciseRepo.move(id, up = true)

    fun moveDown(id: Long) = exerciseRepo.move(id, up = false)

    fun getExercise(id: Long): Exercise? = exerciseRepo.get(id)

    fun updateSettings(newSettings: TimerSettings) {
        settingsRepo.update(newSettings)
        RestTimerController.updateSettings(newSettings)
    }

    /** Run just this one exercise. */
    fun startSingle(exercise: Exercise) {
        if (isSessionActive) return
        _timerMinimized.value = false
        RestTimerController.startSingle(exercise, settingsRepo.current())
    }

    /** Run the whole workout starting from the given exercise to the end of the list. */
    fun startWorkoutFrom(exercise: Exercise) {
        if (isSessionActive) return
        val all = exercises.value
        val start = all.indexOfFirst { it.id == exercise.id }
        if (start < 0) return
        _timerMinimized.value = false
        RestTimerController.startSession(all.subList(start, all.size), settingsRepo.current())
    }

    fun startRest(actualReps: Int) = RestTimerController.startRest(actualReps)
    fun addRestSeconds(extra: Int) = RestTimerController.addSeconds(extra)
    fun skipRest() = RestTimerController.skip()

    // Reviewing / rewinding to earlier sets during an active session.
    fun browseBack() = RestTimerController.browseBack()
    fun browseForward() = RestTimerController.browseForward()
    fun editBrowsedReps(reps: Int) = RestTimerController.editBrowsedReps(reps)
    fun rewindHere() = RestTimerController.rewindHere()

    fun cancelTimer() {
        RestTimerController.cancel()
        _timerMinimized.value = false
    }

    fun deleteReport(id: Long) = reportRepo.delete(id)
    fun deleteReports(ids: Set<Long>) = reportRepo.deleteMany(ids)
    fun deleteAllReports() = reportRepo.deleteAll()

    fun reportsByIds(ids: Set<Long>): List<com.nikita.workoutstudio.model.WorkoutReport> =
        reports.value.filter { it.id in ids }

    fun allReports(): List<com.nikita.workoutstudio.model.WorkoutReport> = reports.value
}
