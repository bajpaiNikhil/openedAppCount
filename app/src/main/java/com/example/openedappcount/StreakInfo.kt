package com.example.openedappcount

enum class StreakType { DISCIPLINE, NIGHT_OWL }

data class StreakInfo(
    val title: String,
    val description: String,
    val streakDays: Int,
    val isActive: Boolean,
    val last7Days: List<Boolean>,
    val type: StreakType
)
