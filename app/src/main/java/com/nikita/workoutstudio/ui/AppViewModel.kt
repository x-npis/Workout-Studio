package com.nikita.workoutstudio.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.nikita.workoutstudio.WorkoutApp
import com.nikita.workoutstudio.model.Exercise
import com.nikita.workoutstudio.model.TimerSettings
import com.nikita.workoutstudio.timer.RestTimerController

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val application = app as WorkoutApp
    private val exerciseRepo = application.exerciseRepository
    private val settingsRepo = application.settingsRepository
    private val reportRepo = application.reportRepository

    val exercises = exerciseRepo.exercises
    val settings = settingsRepo.settings
    val reports = reportRepo.reports
    val timerState = RestTimerController.state

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
    fun startSingle(exercise: Exercise) =
        RestTimerController.startSingle(exercise, settingsRepo.current())

    /** Run the whole workout starting from the given exercise to the end of the list. */
    fun startWorkoutFrom(exercise: Exercise) {
        val all = exercises.value
        val start = all.indexOfFirst { it.id == exercise.id }
        if (start < 0) return
        RestTimerController.startSession(all.subList(start, all.size), settingsRepo.current())
    }

    fun startRest(actualReps: Int) = RestTimerController.startRest(actualReps)
    fun addRestSeconds(extra: Int) = RestTimerController.addSeconds(extra)
    fun skipRest() = RestTimerController.skip()
    fun cancelTimer() = RestTimerController.cancel()

    fun deleteReport(id: Long) = reportRepo.delete(id)
}
