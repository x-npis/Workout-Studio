package com.nikita.workoutstudio.data

import android.content.Context
import com.nikita.workoutstudio.model.Exercise
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ExerciseRepository(context: Context) {

    private val prefs = context.getSharedPreferences("exercises", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    private val _exercises = MutableStateFlow(load())
    val exercises: StateFlow<List<Exercise>> = _exercises.asStateFlow()

    private fun load(): List<Exercise> {
        val raw = prefs.getString(KEY, null) ?: return defaults()
        return try {
            json.decodeFromString<List<Exercise>>(raw)
        } catch (e: Exception) {
            defaults()
        }
    }

    private fun persist(list: List<Exercise>) {
        _exercises.value = list
        prefs.edit().putString(KEY, json.encodeToString(list)).apply()
    }

    fun add(name: String, restSeconds: Int, reps: Int, sets: Int) {
        val id = (_exercises.value.maxOfOrNull { it.id } ?: 0L) + 1L
        persist(_exercises.value + Exercise(id, name, restSeconds, reps, sets))
    }

    fun update(exercise: Exercise) {
        persist(_exercises.value.map { if (it.id == exercise.id) exercise else it })
    }

    fun delete(id: Long) {
        persist(_exercises.value.filterNot { it.id == id })
    }

    fun get(id: Long): Exercise? = _exercises.value.firstOrNull { it.id == id }

    private fun defaults(): List<Exercise> = listOf(
        Exercise(1, "Подтягивания", 90, 8, 3),
        Exercise(2, "Приседания", 60, 15, 3),
        Exercise(3, "Махи руками", 30, 20, 3)
    )

    companion object {
        private const val KEY = "exercise_list"
    }
}
