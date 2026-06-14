package com.example.openedappcount

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import java.util.Calendar

class UsageStatsRepository(
    private val context: Context
) {

    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    // Resolve the default home/launcher package once (e.g. "com.miui.home").
    // The launcher never registers a LAUNCHER-category intent for itself, so
    // getLaunchIntentForPackage returns null and we'd filter it out — but Digital
    // Wellbeing counts home screen time, so we must include it.
    private val launcherPackage: String? by lazy {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        context.packageManager
            .resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName
            .also { Log.d(TAG, "launcherPackage detected: $it") }
    }

    // True for any package that represents something the user interacts with:
    // user-installed apps (have a launcher entry) OR the home screen itself.
    private fun isUserVisible(pkg: String): Boolean {
        if (pkg == launcherPackage) return true
        return try {
            context.packageManager.getLaunchIntentForPackage(pkg) != null
        } catch (e: Exception) { false }
    }

    // ── Unlock count ──────────────────────────────────────────────────────────

    fun getUnlockCount(): Int {
        val endTime = System.currentTimeMillis()
        val startTime = midnightOf(0)

        val events = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        var unlockCount = 0

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.KEYGUARD_HIDDEN) unlockCount++
        }

        Log.d(TAG, "getUnlockCount → $unlockCount unlocks today")
        return unlockCount
    }

    // ── App open events (raw list, used by widget) ────────────────────────────

    fun getOpenedApps(): List<AppOpenEvent> {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 24 * 60 * 60 * 1000L

        val events = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        val appOpenEvents = mutableListOf<AppOpenEvent>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                appOpenEvents.add(AppOpenEvent(event.packageName, event.timeStamp))
            }
        }

        Log.d(TAG, "getOpenedApps → ${appOpenEvents.size} FG events in last 24h")
        return appOpenEvents
    }

    // ── App usage stats ───────────────────────────────────────────────────────

    fun getAppUsageStats(): List<AppUsageInfo> =
        getAppUsageInRange(midnightOf(0), System.currentTimeMillis())

    fun getAppUsageInRange(startMs: Long, endMs: Long): List<AppUsageInfo> {
        // Buffer 1 hour before startMs so apps opened before midnight (cross-midnight
        // sessions) have their MOVE_TO_FOREGROUND event captured. Pre-window FG events
        // are pinned to startMs so we only count time inside [startMs, endMs].
        val bufferMs = 3_600_000L
        val queryStart = startMs - bufferMs

        Log.d(TAG, "getAppUsageInRange: ${fmtMs(startMs)} → ${fmtMs(endMs)} (query from ${fmtMs(queryStart)})")

        val events = usageStatsManager.queryEvents(queryStart, endMs)
        val event = UsageEvents.Event()
        val foregroundMap = mutableMapOf<String, Long>()
        val usageMap = mutableMapOf<String, Long>()
        // Start assuming screen is OFF. The first KEYGUARD_HIDDEN in the event stream
        // will flip it on. This prevents counting phantom time when the phone was locked
        // during the buffer window (23:00-00:00) or overnight.
        var screenOn = false
        var screenOnSessions = 0
        var screenOffFinalizations = 0

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.KEYGUARD_HIDDEN -> {
                    // Screen turned on — sessions will open via subsequent FG events
                    screenOn = true
                }
                UsageEvents.Event.KEYGUARD_SHOWN -> {
                    // Screen turned off — finalize every open session right now.
                    // This prevents miui.home (and any other app) from accumulating
                    // time while the phone is locked overnight.
                    screenOn = false
                    foregroundMap.keys.toList().forEach { pkg ->
                        val fgStart = foregroundMap.remove(pkg) ?: return@forEach
                        val sessionStart = maxOf(fgStart, startMs)
                        val duration = event.timeStamp - sessionStart
                        if (duration > 0) {
                            usageMap[pkg] = (usageMap[pkg] ?: 0L) + duration
                            screenOffFinalizations++
                        }
                    }
                }
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    if (screenOn) {
                        foregroundMap[event.packageName] = maxOf(event.timeStamp, startMs)
                        screenOnSessions++
                    }
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val fgStart = foregroundMap.remove(event.packageName)
                    if (fgStart != null) {
                        val duration = event.timeStamp - fgStart
                        if (duration > 0) {
                            usageMap[event.packageName] = (usageMap[event.packageName] ?: 0L) + duration
                        }
                    }
                }
            }
        }

        Log.d(TAG, "  screenOn sessions=$screenOnSessions screenOff finalizations=$screenOffFinalizations")

        // Screen is currently on at endMs — count still-open sessions, cap to 1h for
        // processes killed by the OS without a MOVE_TO_BACKGROUND event.
        val maxOpenSessionMs = 3_600_000L
        if (screenOn && foregroundMap.isNotEmpty()) {
            Log.d(TAG, "  open sessions at query time: ${foregroundMap.size}")
            foregroundMap.forEach { (pkg, fgStart) ->
                val openDuration = minOf(endMs - fgStart, maxOpenSessionMs)
                if (openDuration > 0) {
                    Log.d(TAG, "    ${shortPkg(pkg)} open for ${openDuration / 60_000}m (capped=${openDuration == maxOpenSessionMs})")
                    usageMap[pkg] = (usageMap[pkg] ?: 0L) + openDuration
                }
            }
        }

        val pm = context.packageManager
        var filteredOutCount = 0
        val result = usageMap.entries
            .filter { (pkg, _) ->
                val passes = isUserVisible(pkg)
                if (!passes) {
                    filteredOutCount++
                    Log.d(TAG, "  filtered (not user-visible): ${shortPkg(pkg)}")
                }
                passes
            }
            .map { (pkg, timeMillis) ->
                val appName = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                } catch (e: Exception) { pkg }
                AppUsageInfo(packageName = pkg, appName = appName, totalTimeInMillis = timeMillis)
            }
            .sortedByDescending { it.totalTimeInMillis }

        val totalMs = result.sumOf { it.totalTimeInMillis }
        Log.d(TAG, "getAppUsageInRange → ${result.size} apps, total ${fmtDuration(totalMs)} (filtered out $filteredOutCount)")
        result.take(5).forEach { Log.d(TAG, "  ${it.appName}: ${fmtDuration(it.totalTimeInMillis)}") }

        return result
    }

    // ── Hourly unlock timeline ────────────────────────────────────────────────

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

        Log.d(TAG, "getUnlocksByHour → peak hour $mostActiveHour:00 (${hourly[mostActiveHour]} unlocks), morning=$morningCount, night=$nightCount")
        return UnlockTimeline(
            hourlyData = hourly.toList(),
            mostActiveHour = mostActiveHour,
            morningCount = morningCount,
            nightCount = nightCount,
        )
    }

    // ── App open counts (leaderboard) ─────────────────────────────────────────

    fun getAppOpenCounts(startMs: Long, endMs: Long): List<AppOpenCount> {
        Log.d(TAG, "getAppOpenCounts: ${fmtMs(startMs)} → ${fmtMs(endMs)}")

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
        val result = countMap.entries
            .filter { (pkg, _) -> isUserVisible(pkg) }
            .map { (pkg, count) ->
                val appName = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                } catch (e: Exception) { pkg }
                AppOpenCount(packageName = pkg, appName = appName, count = count)
            }
            .sortedByDescending { it.count }

        Log.d(TAG, "getAppOpenCounts → ${result.size} apps, top: ${result.firstOrNull()?.let { "${it.appName} (${it.count})" } ?: "none"}")
        return result
    }

    // ── Check patterns (compulsion lens) ──────────────────────────────────────

    fun getCheckPatterns(startMs: Long, endMs: Long): List<AppCheckStat> {
        val opens = getAppOpenCounts(startMs, endMs)
        val usage = getAppUsageInRange(startMs, endMs)
        return computeCheckPatterns(opens, usage, MIN_OPENS, SHORT_SESSION_MS)
    }

    // ── Session summary ───────────────────────────────────────────────────────

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
                UsageEvents.Event.KEYGUARD_SHOWN  -> locks.add(event.timeStamp)
            }
        }

        var quick = 0; var medium = 0; var long = 0; var totalMs = 0L; var pairedCount = 0
        var unpairedCount = 0
        for (unlockTime in unlocks) {
            val lockTime = locks.firstOrNull { it > unlockTime }
            if (lockTime == null) { unpairedCount++; continue }
            val durationMs = lockTime - unlockTime
            totalMs += durationMs
            pairedCount++
            val durationMin = durationMs / 60_000.0
            when {
                durationMin < 3  -> quick++
                durationMin < 10 -> medium++
                else             -> long++
            }
        }

        val avgMin = if (pairedCount > 0) (totalMs / pairedCount / 60_000f) else 0f
        Log.d(TAG, "getSessionsToday → quick=$quick medium=$medium long=$long avg=${avgMin.toInt()}m paired=$pairedCount unpaired=$unpairedCount")

        return SessionSummary(
            quickPickups = quick,
            mediumSessions = medium,
            longSessions = long,
            avgSessionMinutes = avgMin,
        )
    }

    // ── Per-day unlock count (used for streaks) ───────────────────────────────

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
        Log.d(TAG, "getDailyUnlockCount(dayOffset=$dayOffset) → $count")
        return count
    }

    // ── Late-night unlock check (used for streaks) ────────────────────────────

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
                if (h in 1..4) {
                    Log.d(TAG, "hasLateNightUnlock(dayOffset=$dayOffset) → true (hour=$h)")
                    return true
                }
            }
        }
        Log.d(TAG, "hasLateNightUnlock(dayOffset=$dayOffset) → false")
        return false
    }

    // ── Monthly calendar stats ────────────────────────────────────────────────

    fun getMonthlyStats(): List<DayStats?> {
        val cal = Calendar.getInstance()
        val today = cal.get(Calendar.DAY_OF_MONTH)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val monthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val now = System.currentTimeMillis()
        val pm = context.packageManager

        Log.d(TAG, "getMonthlyStats: querying $today days (month starts ${fmtMs(monthStart)})")

        val unlocksByDay     = IntArray(daysInMonth + 1)
        val hourlyByDay      = Array(daysInMonth + 1) { IntArray(24) }
        val firstPickupByDay = arrayOfNulls<Pair<Int, Int>>(daysInMonth + 1)
        val lastPickupByDay  = arrayOfNulls<Pair<Int, Int>>(daysInMonth + 1)
        val foregroundByDay  = Array(daysInMonth + 1) { mutableMapOf<String, Long>() }
        val screenTimeByDay  = Array(daysInMonth + 1) { mutableMapOf<String, Long>() }
        val opensByDay       = Array(daysInMonth + 1) { mutableMapOf<String, Int>() }

        // 1-hour buffer before monthStart to recover cross-midnight sessions on day 1
        val queryStart = monthStart - 3_600_000L
        val events = usageStatsManager.queryEvents(queryStart, now)
        val event = UsageEvents.Event()

        var totalEvents = 0
        // Screen state tracked across the full month event stream — same logic as
        // getAppUsageInRange: KEYGUARD_SHOWN closes all open sessions so no time
        // accumulates while the phone is locked (prevents miui.home overnight inflation).
        var screenOn = false

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            totalEvents++
            val eCal = Calendar.getInstance().apply { timeInMillis = event.timeStamp }
            val day = eCal.get(Calendar.DAY_OF_MONTH)
            if (day < 1 || day > today) continue

            when (event.eventType) {
                UsageEvents.Event.KEYGUARD_HIDDEN -> {
                    screenOn = true
                    unlocksByDay[day]++
                    val h = eCal.get(Calendar.HOUR_OF_DAY)
                    val m = eCal.get(Calendar.MINUTE)
                    hourlyByDay[day][h]++
                    if (firstPickupByDay[day] == null) firstPickupByDay[day] = Pair(h, m)
                    lastPickupByDay[day] = Pair(h, m)
                }
                UsageEvents.Event.KEYGUARD_SHOWN -> {
                    // Screen off — finalize every open session across all days right now
                    screenOn = false
                    for (d in 1..today) {
                        foregroundByDay[d].keys.toList().forEach { pkg ->
                            val fgStart = foregroundByDay[d].remove(pkg) ?: return@forEach
                            val dayMidnight = midnightOf(today - d)
                            val sessionStart = maxOf(fgStart, dayMidnight)
                            val duration = event.timeStamp - sessionStart
                            if (duration > 0) {
                                screenTimeByDay[d][pkg] = (screenTimeByDay[d][pkg] ?: 0L) + duration
                            }
                        }
                    }
                }
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    if (screenOn) try {
                        if (isUserVisible(event.packageName)) {
                            val dayMidnight = midnightOf(today - day)
                            foregroundByDay[day][event.packageName] =
                                maxOf(event.timeStamp, dayMidnight)
                            opensByDay[day][event.packageName] =
                                (opensByDay[day][event.packageName] ?: 0) + 1
                        }
                    } catch (_: Exception) {}
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val pkg = event.packageName
                    val start = foregroundByDay[day].remove(pkg)
                        ?: if (day > 1) foregroundByDay[day - 1].remove(pkg) else null
                    if (start != null) {
                        val dayMidnight = midnightOf(today - day)
                        val sessionStart = maxOf(start, dayMidnight)
                        val duration = event.timeStamp - sessionStart
                        if (duration > 0) {
                            screenTimeByDay[day][pkg] =
                                (screenTimeByDay[day][pkg] ?: 0L) + duration
                        }
                    }
                }
            }
        }

        // Open sessions at query time — cap to 1 hour to exclude killed-process inflation
        // Only count open sessions if the screen is currently on; if screen is off the
        // KEYGUARD_SHOWN handler already finalized them.
        val maxOpenSessionMs = 3_600_000L
        val openSessionCount = if (screenOn) foregroundByDay[today].size else 0
        if (screenOn) {
            foregroundByDay[today].forEach { (pkg, fgStart) ->
                val duration = minOf(now - fgStart, maxOpenSessionMs)
                if (duration > 0) {
                    screenTimeByDay[today][pkg] = (screenTimeByDay[today][pkg] ?: 0L) + duration
                }
            }
        }

        Log.d(TAG, "getMonthlyStats: processed $totalEvents events, screenOn=$screenOn openSessions=$openSessionCount")

        val todayTotal = screenTimeByDay[today].values.sum()
        val todayUnlocks = unlocksByDay[today]
        Log.d(TAG, "  today (day $today): unlocks=$todayUnlocks screenTime=${fmtDuration(todayTotal)}")

        return (1..daysInMonth).map { day ->
            if (day > today) null
            else {
                val topEntry = opensByDay[day].entries.maxByOrNull { it.value }
                val topName = topEntry?.key?.let { pkg ->
                    try { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }
                    catch (_: Exception) { null }
                }
                DayStats(
                    unlockCount = unlocksByDay[day],
                    totalScreenMillis = screenTimeByDay[day].values.sum(),
                    topAppName = topName,
                    topAppOpens = topEntry?.value ?: 0,
                    hourlyUnlocks = hourlyByDay[day].toList(),
                    firstPickup = firstPickupByDay[day]?.let { (h, m) ->
                        "$h:${m.toString().padStart(2, '0')}"
                    },
                    lastPickup = lastPickupByDay[day]?.let { (h, m) ->
                        "$h:${m.toString().padStart(2, '0')}"
                    },
                )
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    // dayOffset=0 → today's midnight, dayOffset=1 → yesterday's midnight, etc.
    private fun midnightOf(dayOffset: Int): Long =
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -dayOffset)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun fmtMs(ms: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = ms }
        return "%02d:%02d:%02d".format(
            c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND)
        )
    }

    private fun fmtDuration(ms: Long): String {
        val totalMin = ms / 60_000
        return "${totalMin / 60}h ${totalMin % 60}m"
    }

    private fun shortPkg(pkg: String) = pkg.split(".").takeLast(2).joinToString(".")

    companion object {
        private const val TAG = "AppUsage"

        // Tunables for the "Checked, not used" compulsion lens.
        // MIN_OPENS: minimum opens before a pattern is even considered for the reflex label.
        // SHORT_SESSION_MS: an average session below this reads as a glance, not real use.
        const val MIN_OPENS = 4
        const val SHORT_SESSION_MS = 30_000L
    }
}
