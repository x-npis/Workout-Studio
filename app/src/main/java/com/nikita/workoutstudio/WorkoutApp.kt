package com.nikita.workoutstudio

import android.app.Application
import com.nikita.workoutstudio.data.ExerciseRepository
import com.nikita.workoutstudio.data.SettingsRepository
import com.nikita.workoutstudio.timer.RestNotifications
import com.nikita.workoutstudio.timer.RestTimerController

class WorkoutApp : Application() {

    lateinit var exerciseRepository: ExerciseRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        exerciseRepository = ExerciseRepository(this)
        settingsRepository = SettingsRepository(this)
        RestTimerController.init(this)
        RestTimerController.updateSettings(settingsRepository.current())
        RestNotifications.ensureChannel(this)
    }
}
