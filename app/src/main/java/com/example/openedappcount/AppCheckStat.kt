package com.example.openedappcount

/**
 * One app's "check pattern" for a time window: how often it was opened vs.
 * how long those opens actually lasted on average.
 */
data class AppCheckStat(
    val packageName: String,
    val appName: String,
    val opens: Int,
    val totalTimeMillis: Long,
    val avgSessionMillis: Long,
    val isReflex: Boolean,
)

/**
 * Fuses open counts with usage time into [AppCheckStat] records.
 *
 * `opens` comes from [UsageStatsRepository.getAppOpenCounts] (every
 * MOVE_TO_FOREGROUND, not gated on screen-on); `usage` comes from
 * [UsageStatsRepository.getAppUsageInRange] (screen-on gated, capped sessions).
 * `avgSessionMillis` is therefore a heuristic that skews low, which means
 * slightly more apps get flagged as reflex than a perfectly-aligned measure
 * would. That's acceptable for an awareness signal — the thresholds were
 * picked with this skew in mind. Do not "fix" by re-deriving opens; keep
 * reusing the existing queries.
 */
fun computeCheckPatterns(
    opens: List<AppOpenCount>,
    usage: List<AppUsageInfo>,
    minOpens: Int,
    shortSessionMs: Long,
): List<AppCheckStat> {
    val timeByPkg = usage.associateBy { it.packageName }

    return opens.map { o ->
        val timeMs = timeByPkg[o.packageName]?.totalTimeInMillis ?: 0L
        val avg = if (o.count > 0) timeMs / o.count else 0L
        AppCheckStat(
            packageName = o.packageName,
            appName = o.appName,
            opens = o.count,
            totalTimeMillis = timeMs,
            avgSessionMillis = avg,
            isReflex = o.count >= minOpens && avg < shortSessionMs,
        )
    }.sortedByDescending { it.opens }
}
