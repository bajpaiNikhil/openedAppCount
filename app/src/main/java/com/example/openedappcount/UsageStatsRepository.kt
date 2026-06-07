package com.example.openedappcount

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log

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

    fun getAppUsageStats(): List<AppUsageInfo> {

        val endTime = System.currentTimeMillis()

        val startTime =
            endTime - (24 * 60 * 60 * 1000)

        val events =
            usageStatsManager.queryEvents(
                startTime,
                endTime
            )

        val event = UsageEvents.Event()

        val foregroundMap =
            mutableMapOf<String, Long>()

        val usageMap =
            mutableMapOf<String, Long>()

        while (events.hasNextEvent()) {

            events.getNextEvent(event)

            when (event.eventType) {

                UsageEvents.Event.MOVE_TO_FOREGROUND -> {

                    foregroundMap[event.packageName] =
                        event.timeStamp
                }

                UsageEvents.Event.MOVE_TO_BACKGROUND -> {

                    val start =
                        foregroundMap[event.packageName]

                    if (start != null) {

                        val duration =
                            event.timeStamp - start

                        val currentDuration =
                            usageMap[event.packageName]
                                ?: 0L

                        usageMap[event.packageName] =
                            currentDuration + duration
                    }
                }
            }
        }

        val pm = context.packageManager
        return usageMap.map { (pkg, timeMillis) ->
            val appName = try {
                val info = pm.getApplicationInfo(pkg, 0)
                pm.getApplicationLabel(info).toString()
            } catch (e: Exception) {
                pkg
            }
            AppUsageInfo(
                packageName = pkg,
                appName = appName,
                totalTimeInMillis = timeMillis
            )
        }.sortedByDescending { it.totalTimeInMillis }
    }

}