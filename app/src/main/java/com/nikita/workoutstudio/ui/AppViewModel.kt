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

    val exercises = exerciseRepo.exercises
    val settings = settingsRepo.settings
    val timerState = RestTimerController.state

    fun addExercise(name: String, restSeconds: Int, reps: Int, sets: Int) =
        exerciseRepo.add(name, restSeconds, reps, sets)

    fun updateExercise(exercise: Exercise) = exerciseRepo.update(exercise)

    fun deleteExercise(id: Long) = exerciseRepo.delete(id)

    fun getExercise(id: Long): Exercise? = exerciseRepo.get(id)

    fun updateSettings(newSettings: TimerSettings) {
        settingsRepo.update(newSettings)
        RestTimerController.updateSettings(newSettings)
    }

    fun openTimer(exercise: Exercise) =
        RestTimerController.prepare(exercise, settingsRepo.current())

    fun startRest() = RestTimerController.startRest()
    fun addRestSeconds(extra: Int) = RestTimerController.addSeconds(extra)
    fun skipRest() = RestTimerController.skip()
    fun cancelTimer() = RestTimerController.cancel()
}
