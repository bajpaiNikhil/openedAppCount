package com.example.openedappcount

data class UnlockTimeline(
    val hourlyData: List<Int>,   // index = hour 0–23, value = unlock count
    val mostActiveHour: Int,     // hour with highest count
    val morningCount: Int,       // hours 6–11
    val nightCount: Int          // hours 21–23 + 0–4
)
