package com.example.openedappcount

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class LeaderboardPeriod { TODAY, WEEK, AVG }

class WellbeingViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UsageStatsRepository(application)

    // ── Global period (drives hero cards + top apps) ──────────────────────────
    var globalPeriod by mutableStateOf(LeaderboardPeriod.TODAY)

    // ── Today data (fast path) ────────────────────────────────────────────────
    private var unlockCountToday by mutableIntStateOf(0)
    private var appUsageToday by mutableStateOf<List<AppUsageInfo>>(emptyList())
    var isLoading by mutableStateOf(true)
        private set

    // ── Week / avg data (extended path) ───────────────────────────────────────
    private var weekUnlockCount by mutableIntStateOf(0)
    private var appUsageWeek by mutableStateOf<List<AppUsageInfo>>(emptyList())
    private var appUsageAvg by mutableStateOf<List<AppUsageInfo>>(emptyList())

    // ── Displayed values (react to globalPeriod) ──────────────────────────────
    val displayedUnlockCount: Int
        get() = when (globalPeriod) {
            LeaderboardPeriod.TODAY -> unlockCountToday
            LeaderboardPeriod.WEEK -> weekUnlockCount
            LeaderboardPeriod.AVG  -> if (weekUnlockCount > 0) weekUnlockCount / 7 else 0
        }

    val displayedAppUsage: List<AppUsageInfo>
        get() = when (globalPeriod) {
            LeaderboardPeriod.TODAY -> appUsageToday
            LeaderboardPeriod.WEEK -> appUsageWeek
            LeaderboardPeriod.AVG  -> appUsageAvg
        }

    val displayedTotalMillis: Long
        get() = displayedAppUsage.sumOf { it.totalTimeInMillis }

    // ── App Opens leaderboard (its own period) ────────────────────────────────
    var leaderboardPeriod by mutableStateOf(LeaderboardPeriod.TODAY)

    private var leaderboardToday by mutableStateOf<List<AppOpenCount>>(emptyList())
    private var leaderboardWeek by mutableStateOf<List<AppOpenCount>>(emptyList())
    private var leaderboardAvg by mutableStateOf<List<AppOpenCount>>(emptyList())

    val appOpenLeaderboard: List<AppOpenCount>
        get() = when (leaderboardPeriod) {
            LeaderboardPeriod.TODAY -> leaderboardToday
            LeaderboardPeriod.WEEK -> leaderboardWeek
            LeaderboardPeriod.AVG  -> leaderboardAvg
        }

    // ── Other dashboard data ──────────────────────────────────────────────────
    var unlockTimeline by mutableStateOf<UnlockTimeline?>(null)
        private set

    var sessionSummary by mutableStateOf<SessionSummary?>(null)
        private set

    var streaks by mutableStateOf<List<StreakInfo>>(emptyList())
        private set

    var isLoadingExtended by mutableStateOf(false)
        private set

    // ── Check patterns (compulsion lens) ──────────────────────────────────────
    var checkStats by mutableStateOf<List<AppCheckStat>>(emptyList())
        private set

    val reflexChecks: Int
        get() = checkStats.filter { it.isReflex }.sumOf { it.opens }

    val reflexAppCount: Int
        get() = checkStats.count { it.isReflex }

    val mostDeliberate: AppCheckStat?
        get() = checkStats.filter { it.opens >= 2 }.maxByOrNull { it.avgSessionMillis }

    // ── Monthly calendar data ─────────────────────────────────────────────
    var monthlyDayStats by mutableStateOf<List<DayStats?>>(emptyList())
        private set
    var selectedDay by mutableStateOf<Int?>(null) // null = today, 1-31 for a specific day
    var isLoadingMonthly by mutableStateOf(false)
        private set

    val selectedDayStats: DayStats?
        get() {
            val day = selectedDay ?: java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH)
            return monthlyDayStats.getOrNull(day - 1)
        }

    val avgUnlockCount: Int?
        get() {
            val filled = monthlyDayStats.filterNotNull().filter { it.unlockCount > 0 }
            return if (filled.isEmpty()) null else filled.sumOf { it.unlockCount } / filled.size
        }

    init {
        refresh(showLoading = true)
        refreshExtended()
        refreshMonthly()
    }

    fun refreshMonthly() {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { isLoadingMonthly = true }
            val stats = try { repository.getMonthlyStats() } catch (e: Exception) { emptyList() }
            withContext(Dispatchers.Main) {
                monthlyDayStats = stats
                isLoadingMonthly = false
            }
        }
    }

    fun refresh(showLoading: Boolean = false) {
        if (showLoading) isLoading = true
        viewModelScope.launch(Dispatchers.IO) {
            val unlocks = try { repository.getUnlockCount() } catch (e: SecurityException) { 0 }
            val usage = try { repository.getAppUsageStats() } catch (e: SecurityException) { emptyList() }
            withContext(Dispatchers.Main) {
                unlockCountToday = unlocks
                appUsageToday = usage
                isLoading = false
            }
        }
    }

    fun refreshTimeline() {
        viewModelScope.launch(Dispatchers.IO) {
            val timeline = try { repository.getUnlocksByHour() } catch (e: Exception) { return@launch }
            withContext(Dispatchers.Main) { unlockTimeline = timeline }
        }
    }

    fun refreshExtended() {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { isLoadingExtended = true }
            try {
                val now = System.currentTimeMillis()
                val todayMidnight = midnightMillis(0)
                val weekStart = midnightMillis(6)

                val timeline = repository.getUnlocksByHour()

                // Week unlock count — sum individual days so we get accurate midnight-bounded counts
                val weekUnlocks = (0..6).sumOf { day ->
                    try { repository.getDailyUnlockCount(day) } catch (e: Exception) { 0 }
                }

                // App usage for week and avg
                val weekUsage = repository.getAppUsageInRange(weekStart, now)
                val avgUsage = weekUsage.map { it.copy(totalTimeInMillis = it.totalTimeInMillis / 7) }
                    .sortedByDescending { it.totalTimeInMillis }

                // App opens leaderboard
                val todayOpenList = repository.getAppOpenCounts(todayMidnight, now)
                val weekOpenList = repository.getAppOpenCounts(weekStart, now)
                val avgOpenList = weekOpenList.map { it.copy(count = maxOf(1, it.count / 7)) }

                val sessions = repository.getSessionsToday()
                val streakList = buildStreaks()
                val checks = repository.getCheckPatterns(todayMidnight, now)

                withContext(Dispatchers.Main) {
                    unlockTimeline = timeline
                    weekUnlockCount = weekUnlocks
                    appUsageWeek = weekUsage
                    appUsageAvg = avgUsage
                    leaderboardToday = todayOpenList
                    leaderboardWeek = weekOpenList
                    leaderboardAvg = avgOpenList
                    sessionSummary = sessions
                    streaks = streakList
                    checkStats = checks
                    isLoadingExtended = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { isLoadingExtended = false }
            }
        }
    }

    private fun buildStreaks(): List<StreakInfo> {
        val disciplineDays = (0..13).map { offset ->
            try { repository.getDailyUnlockCount(offset) <= 60 } catch (e: Exception) { false }
        }
        val nightFreeDays = (0..13).map { offset ->
            try { !repository.hasLateNightUnlock(offset) } catch (e: Exception) { true }
        }

        fun streakCount(days: List<Boolean>): Int {
            var count = 0
            for (met in days) { if (met) count++ else break }
            return count
        }

        return listOf(
            StreakInfo(
                title = "Daily Discipline",
                description = "Under 60 unlocks / day",
                streakDays = streakCount(disciplineDays),
                isActive = disciplineDays.firstOrNull() == true,
                last7Days = disciplineDays.take(7).reversed(),
                type = StreakType.DISCIPLINE
            ),
            StreakInfo(
                title = "Night Owl Free",
                description = "No unlock after 1 AM",
                streakDays = streakCount(nightFreeDays),
                isActive = nightFreeDays.firstOrNull() == true,
                last7Days = nightFreeDays.take(7).reversed(),
                type = StreakType.NIGHT_OWL
            )
        )
    }

    private fun midnightMillis(daysBack: Int): Long =
        java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, -daysBack)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
}
