package com.example.openedappcount

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val totalTimeInMillis: Long
)
