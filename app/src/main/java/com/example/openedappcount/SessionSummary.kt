package com.example.openedappcount

data class SessionSummary(
    val quickPickups: Int,       // sessions < 3 min
    val mediumSessions: Int,     // sessions 3–10 min
    val longSessions: Int,       // sessions >= 10 min
    val avgSessionMinutes: Float
)
