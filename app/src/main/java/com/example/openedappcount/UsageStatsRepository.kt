package com.example.openedappcount

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import java.util.Calendar

class UsageStatsRepository(
    private val context: Context
) {

    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    fun getUnlockCount(): Int {

        val endTime = System.currentTimeMillis()

        val startTime = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        val events =
            usageStatsManager.queryEvents(
                startTime,
                endTime
            )

        val event = UsageEvents.Event()

        var unlockCount = 0

        while (events.hasNextEvent()) {

            events.getNextEvent(event)

            if (
                event.eventType ==
                UsageEvents.Event.KEYGUARD_HIDDEN
            ) {

                unlockCount++

                Log.d(
                    "UNLOCK_EVENT",
                    """
                    Unlock Detected
                    Package: ${event.packageName}
                    Time: ${event.timeStamp}
                    """.trimIndent()
                )
            }
        }

        return unlockCount
    }

    fun getOpenedApps(): List<AppOpenEvent> {

        val endTime = System.currentTimeMillis()

        val startTime =
            endTime - (24 * 60 * 60 * 1000)

        val events =
            usageStatsManager.queryEvents(
                startTime,
                endTime
            )

        val event = UsageEvents.Event()

        val appOpenEvents =
            mutableListOf<AppOpenEvent>()

        while (events.hasNextEvent()) {

            events.getNextEvent(event)

            if (
                event.eventType ==
                UsageEvents.Event.MOVE_TO_FOREGROUND
            ) {

                appOpenEvents.add(
                    AppOpenEvent(
                        packageName =
                            event.packageName,
                        timestamp =
                            event.timeStamp
                    )
                )
            }
        }

        return appOpenEvents
    }

    fun getAppUsageStats(): List<AppUsageInfo> =
        getAppUsageInRange(midnightOf(0), System.currentTimeMillis())

    fun getAppUsageInRange(startMs: Long, endMs: Long): List<AppUsageInfo> {
        val events = usageStatsManager.queryEvents(startMs, endMs)
        val event = UsageEvents.Event()
        val foregroundMap = mutableMapOf<String, Long>()
        val usageMap = mutableMapOf<String, Long>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND ->
                    foregroundMap[event.packageName] = event.timeStamp
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val start = foregroundMap[event.packageName]
                    if (start != null) {
                        usageMap[event.packageName] =
                            (usageMap[event.packageName] ?: 0L) + (event.timeStamp - start)
                    }
                }
            }
        }

        val pm = context.packageManager
        return usageMap.entries
            .filter { (pkg, _) ->
                try { pm.getLaunchIntentForPackage(pkg) != null } catch (e: Exception) { false }
            }
            .map { (pkg, timeMillis) ->
                val appName = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                } catch (e: Exception) { pkg }
                AppUsageInfo(packageName = pkg, appName = appName, totalTimeInMillis = timeMillis)
            }
            .sortedByDescending { it.totalTimeInMillis }
    }

    fun getUnlocksByHour(): UnlockTimeline {
        val now = System.currentTimeMillis()
        val midnight = midnightOf(0)

        val hourly = IntArray(24)
        val events = usageStatsManager.queryEvents(midnight, now)
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.KEYGUARD_HIDDEN) {
                val cal = Calendar.getInstance().apply { timeInMillis = event.timeStamp }
                hourly[cal.get(Calendar.HOUR_OF_DAY)]++
            }
        }

        val mostActiveHour = hourly.indices.maxByOrNull { hourly[it] } ?: 0
        val morningCount = (6..11).sumOf { hourly[it] }
        val nightCount = ((21..23) + (0..4)).sumOf { hourly[it] }

        return UnlockTimeline(
            hourlyData = hourly.toList(),
            mostActiveHour = mostActiveHour,
            morningCount = morningCount,
            nightCount = nightCount
        )
    }

    fun getAppOpenCounts(startMs: Long, endMs: Long): List<AppOpenCount> {
        val events = usageStatsManager.queryEvents(startMs, endMs)
        val event = UsageEvents.Event()
        val countMap = mutableMapOf<String, Int>()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                countMap[event.packageName] = (countMap[event.packageName] ?: 0) + 1
            }
        }
        val pm = context.packageManager
        return countMap.entries
            .filter { (pkg, _) ->
                try { pm.getLaunchIntentForPackage(pkg) != null } catch (e: Exception) { false }
            }
            .map { (pkg, count) ->
                val appName = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                } catch (e: Exception) { pkg }
                AppOpenCount(packageName = pkg, appName = appName, count = count)
            }
            .sortedByDescending { it.count }
    }

    fun getSessionsToday(): SessionSummary {
        val now = System.currentTimeMillis()
        val midnight = midnightOf(0)

        val unlocks = mutableListOf<Long>()
        val locks = mutableListOf<Long>()
        val events = usageStatsManager.queryEvents(midnight, now)
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.KEYGUARD_HIDDEN -> unlocks.add(event.timeStamp)
                UsageEvents.Event.KEYGUARD_SHOWN -> locks.add(event.timeStamp)
            }
        }

        var quick = 0; var medium = 0; var long = 0; var totalMs = 0L; var pairedCount = 0
        for (unlockTime in unlocks) {
            val lockTime = locks.firstOrNull { it > unlockTime } ?: continue
            val durationMs = lockTime - unlockTime
            totalMs += durationMs
            pairedCount++
            val durationMin = durationMs / 60_000.0
            when {
                durationMin < 3 -> quick++
                durationMin < 10 -> medium++
                else -> long++
            }
        }

        val avgMin = if (pairedCount > 0) (totalMs / pairedCount / 60_000f) else 0f
        return SessionSummary(
            quickPickups = quick,
            mediumSessions = medium,
            longSessions = long,
            avgSessionMinutes = avgMin
        )
    }

    fun getDailyUnlockCount(dayOffset: Int): Int {
        val start = midnightOf(dayOffset)
        val end = midnightOf(dayOffset - 1)
        val events = usageStatsManager.queryEvents(start, end)
        val event = UsageEvents.Event()
        var count = 0
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.KEYGUARD_HIDDEN) count++
        }
        return count
    }

    fun hasLateNightUnlock(dayOffset: Int): Boolean {
        val start = midnightOf(dayOffset)
        val end = midnightOf(dayOffset - 1)
        val events = usageStatsManager.queryEvents(start, end)
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.KEYGUARD_HIDDEN) {
                val cal = Calendar.getInstance().apply { timeInMillis = event.timeStamp }
                val h = cal.get(Calendar.HOUR_OF_DAY)
                if (h in 1..4) return true
            }
        }
        return false
    }

    // dayOffset=0 → today's midnight, dayOffset=1 → yesterday's midnight, etc.
    private fun midnightOf(dayOffset: Int): Long =
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -dayOffset)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

}