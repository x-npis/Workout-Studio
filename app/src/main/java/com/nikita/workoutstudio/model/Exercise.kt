package com.nikita.workoutstudio.model

import kotlinx.serialization.Serializable

@Serializable
data class Exercise(
    val id: Long,
    val name: String,
    val restSeconds: Int,
    val reps: Int,
    val sets: Int = 3
)
