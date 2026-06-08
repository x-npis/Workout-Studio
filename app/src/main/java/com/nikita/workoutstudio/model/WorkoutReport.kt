package com.nikita.workoutstudio.model

import kotlinx.serialization.Serializable

@Serializable
data class SetEntry(
    val actualReps: Int,
    val targetReps: Int
)

@Serializable
data class ExerciseReport(
    val name: String,
    val restSeconds: Int,
    val targetReps: Int,
    val plannedSets: Int,
    val sets: List<SetEntry> = emptyList()
)

@Serializable
data class WorkoutReport(
    val id: Long,
    val startedAt: Long,
    val finishedAt: Long,
    val exercises: List<ExerciseReport> = emptyList()
) {
    val totalSets: Int get() = exercises.sumOf { it.sets.size }
    val totalReps: Int get() = exercises.sumOf { ex -> ex.sets.sumOf { it.actualReps } }
}
