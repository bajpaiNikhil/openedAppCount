package com.example.openedappcount

data class DayStats(
    val unlockCount: Int,
    val totalScreenMillis: Long,
    val topAppName: String?,
    val topAppOpens: Int,
    val hourlyUnlocks: List<Int>,   // 24 values, index = hour of day
    val firstPickup: String?,       // "7:14" format, null if no unlocks
    val lastPickup: String?,        // "23:18" format
)
