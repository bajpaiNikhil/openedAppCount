package com.example.openedappcount

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.openedappcount.ui.theme.AppAccentColors
import com.example.openedappcount.ui.theme.Archivo
import com.example.openedappcount.ui.theme.BarEmpty
import com.example.openedappcount.ui.theme.BarHigh
import com.example.openedappcount.ui.theme.BarMid
import com.example.openedappcount.ui.theme.BarPeak
import com.example.openedappcount.ui.theme.BlueAccent
import com.example.openedappcount.ui.theme.CardBg
import com.example.openedappcount.ui.theme.CardBorder
import com.example.openedappcount.ui.theme.GreenAccent
import com.example.openedappcount.ui.theme.JetBrainsMono
import com.example.openedappcount.ui.theme.MinAccent
import com.example.openedappcount.ui.theme.MinFaint
import com.example.openedappcount.ui.theme.MinInk
import com.example.openedappcount.ui.theme.MinLine
import com.example.openedappcount.ui.theme.MinMuted
import com.example.openedappcount.ui.theme.PurpleAccent
import com.example.openedappcount.ui.theme.SectionDiv
import com.example.openedappcount.ui.theme.TextDim
import com.example.openedappcount.ui.theme.TextMid
import com.example.openedappcount.ui.theme.TextPrimary
import kotlin.math.abs

private val BIN_LABELS = listOf(
    "12a","2a","4a","6a","8a","10a","12p","2p","4p","6p","8p","10p"
)

// ─── Unlock Timeline ──────────────────────────────────────────────────────────

@Composable
fun UnlockTimelineSection(timeline: UnlockTimeline) {
    // Group 24 hourly buckets into 12 two-hour bins
    val binnedData = (0 until 12).map { bin ->
        timeline.hourlyData[bin * 2] + timeline.hourlyData[bin * 2 + 1]
    }
    val maxBin = binnedData.maxOrNull()?.coerceAtLeast(1) ?: 1
    val peakBin = timeline.mostActiveHour / 2
    val cardShape = RoundedCornerShape(16.dp)

    var selectedBin by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 18.dp)
            .clip(cardShape)
            .background(CardBg)
            .border(1.dp, CardBorder, cardShape)
            .padding(16.dp)
    ) {
        // Bar chart — each column = bar (grows from bottom) + label below
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            binnedData.forEachIndexed { i, count ->
                val fraction = (count.toFloat() / maxBin).coerceAtLeast(if (count > 0) 0.05f else 0f)
                val isSelected = selectedBin == i
                val hasSel = selectedBin != null
                val barColor = when {
                    isSelected                          -> BlueAccent
                    hasSel && count > 0                -> BarMid.copy(alpha = 0.4f)
                    hasSel                             -> BarEmpty.copy(alpha = 0.3f)
                    i == peakBin && count > 0          -> BarPeak
                    count.toFloat() / maxBin >= 0.25f  -> BarHigh
                    count > 0                          -> BarMid
                    else                               -> BarEmpty
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { selectedBin = if (selectedBin == i) null else i },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((fraction * 72).dp.coerceAtLeast(if (count > 0) 3.dp else 1.5.dp))
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(barColor)
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = BIN_LABELS[i],
                        color = if (isSelected) BlueAccent else TextDim,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // Selection info — fixed height to avoid layout jumps
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            contentAlignment = Alignment.Center
        ) {
            val selBin = selectedBin
            if (selBin != null) {
                val count = binnedData[selBin]
                val h1 = selBin * 2
                val h2 = (h1 + 2).coerceAtMost(24)
                val rangeText = if (h2 >= 24) "${hourLabel(h1)} – 12 AM"
                                else "${hourLabel(h1)} – ${hourLabel(h2)}"
                val unlockText = if (count == 1) "1 unlock" else "$count unlocks"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(BlueAccent)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "$rangeText  ·  $unlockText",
                        color = BlueAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Text(
                    text = "Tap a bar to see details",
                    color = TextDim,
                    fontSize = 11.sp
                )
            }
        }

        HorizontalDivider(color = CardBorder, thickness = 1.dp)

        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            MetaItem("Peak", hourLabel(timeline.mostActiveHour), BlueAccent, Modifier.weight(1f))
            MetaItem("Morning", timeline.morningCount.toString(), GreenAccent, Modifier.weight(1f))
            MetaItem("Night", timeline.nightCount.toString(), PurpleAccent, Modifier.weight(1f))
        }
    }
}

private fun hourLabel(hour: Int): String {
    val h = when {
        hour == 0  -> "12"
        hour > 12  -> "${hour - 12}"
        else       -> "$hour"
    }
    val suffix = if (hour < 12) "AM" else "PM"
    return "$h $suffix"
}

@Composable
private fun MetaItem(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text(text = label, color = TextDim, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

// ─── App Open Leaderboard ─────────────────────────────────────────────────────

@Composable
fun AppOpenLeaderboardSection(
    apps: List<AppOpenCount>,
    period: LeaderboardPeriod,
    onPeriodChange: (LeaderboardPeriod) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 18.dp)
    ) {
        // Chip row
        Row(
            modifier = Modifier.padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LeaderboardPeriod.entries.forEach { p ->
                val selected = p == period
                val label = when (p) {
                    LeaderboardPeriod.TODAY -> "Today"
                    LeaderboardPeriod.WEEK -> "This Week"
                    LeaderboardPeriod.AVG -> "Avg / Day"
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selected) Color(0xFF0D1A35) else CardBg)
                        .border(
                            1.dp,
                            if (selected) BlueAccent.copy(alpha = 0.27f) else Color(0xFF1E2A4A),
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { onPeriodChange(p) }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = label,
                        color = if (selected) BlueAccent else TextMid,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
        }

        // List card
        val cardShape = RoundedCornerShape(16.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(CardBg)
                .border(1.dp, CardBorder, cardShape)
        ) {
            if (apps.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No data yet", color = TextDim, fontSize = 12.sp)
                }
            } else {
                apps.take(7).forEachIndexed { i, app ->
                    AppOpenRow(rank = i + 1, app = app)
                    if (i < minOf(apps.size, 7) - 1) {
                        HorizontalDivider(color = SectionDiv, thickness = 1.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppOpenRow(rank: Int, app: AppOpenCount) {
    val accent = AppAccentColors[abs(app.appName.hashCode()) % AppAccentColors.size]
    val initials = run {
        val words = app.appName.trim().split(Regex("\\s+"))
        if (words.size >= 2) "${words[0].first().uppercaseChar()}${words[1].first().lowercaseChar()}"
        else if (app.appName.length >= 2) "${app.appName[0].uppercaseChar()}${app.appName[1].lowercaseChar()}"
        else app.appName.uppercase()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$rank",
            color = TextDim,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(16.dp)
        )
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = initials, color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = app.appName,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${app.count}×",
            color = BlueAccent,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

// ─── Session Patterns ─────────────────────────────────────────────────────────

@Composable
fun SessionSection(summary: SessionSummary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SessionCard(
                count = summary.quickPickups,
                label = "Quick Pickups",
                sublabel = "< 3 min",
                color = BlueAccent,
                modifier = Modifier.weight(1f)
            )
            SessionCard(
                count = summary.longSessions,
                label = "Deep Sessions",
                sublabel = "≥ 10 min",
                color = PurpleAccent,
                modifier = Modifier.weight(1f)
            )
        }
        val avgText = if (summary.avgSessionMinutes < 1f) "< 1 min"
        else "${summary.avgSessionMinutes.toInt()} min"
        Text(
            text = "Avg session · $avgText · ${summary.mediumSessions} medium sessions",
            color = TextDim,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 18.dp)
        )
    }
}

@Composable
private fun SessionCard(
    count: Int,
    label: String,
    sublabel: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(CardBg)
            .border(1.dp, CardBorder, shape)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = count.toString(), color = color, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, lineHeight = 28.sp)
        Text(text = label, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 6.dp))
        Text(text = sublabel, color = TextDim, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

// ─── Streaks ──────────────────────────────────────────────────────────────────

@Composable
fun StreakSection(streaks: List<StreakInfo>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        streaks.forEach { StreakCard(it) }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun StreakCard(streak: StreakInfo) {
    // Colors per streak type, matching HTML
    val dotColor   = if (streak.type == StreakType.DISCIPLINE) BlueAccent else GreenAccent
    val countColor = if (streak.type == StreakType.DISCIPLINE) GreenAccent else PurpleAccent

    val cardShape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(CardBg)
            .border(1.dp, CardBorder, cardShape)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(streak.title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(streak.description, color = TextDim, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
            // 7 day dots
            Row(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                streak.last7Days.forEach { met ->
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(if (met) dotColor else CardBorder)
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = streak.streakDays.toString(),
                color = countColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                lineHeight = 28.sp
            )
            Text(
                text = if (streak.streakDays == 1) "day" else "days",
                color = TextDim,
                fontSize = 10.sp,
                textAlign = TextAlign.End
            )
        }
    }
}

// ─── Checked, Not Used (compulsion lens) ───────────────────────────────────────

@Composable
fun CheckPatternSection(
    checkStats: List<AppCheckStat>,
    reflexChecks: Int,
    reflexAppCount: Int,
    mostDeliberate: AppCheckStat?,
) {
    val reflexApps = checkStats.filter { it.isReflex }.take(5)
    val maxOpens = reflexApps.maxOfOrNull { it.opens } ?: 1

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = "CHECKED, NOT USED",
            color = MinMuted,
            fontFamily = Archivo,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.sp,
        )

        Spacer(Modifier.height(10.dp))

        if (reflexChecks == 0) {
            Text(
                text = "Nothing compulsive today.",
                color = MinMuted,
                fontFamily = JetBrainsMono,
                fontSize = 14.sp,
            )
        } else {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = MinAccent, fontWeight = FontWeight.Bold)) {
                        append("$reflexChecks reflex checks")
                    }
                    withStyle(SpanStyle(color = MinMuted)) {
                        append(" · $reflexAppCount ${if (reflexAppCount == 1) "app" else "apps"}")
                    }
                },
                fontFamily = JetBrainsMono,
                fontSize = 16.sp,
            )

            Spacer(Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                reflexApps.forEach { app ->
                    CheckPatternRow(app = app, maxOpens = maxOpens)
                }
            }
        }

        if (mostDeliberate != null) {
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MinLine, thickness = 1.dp)
            Spacer(Modifier.height(12.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = MinMuted)) { append("Most deliberate · ") }
                    withStyle(SpanStyle(color = MinInk, fontWeight = FontWeight.SemiBold)) {
                        append(mostDeliberate.appName)
                    }
                    withStyle(SpanStyle(color = MinMuted)) {
                        append(" ~${formatDuration(mostDeliberate.avgSessionMillis)} avg")
                    }
                },
                fontFamily = JetBrainsMono,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun CheckPatternRow(app: AppCheckStat, maxOpens: Int) {
    val accent = AppAccentColors[abs(app.appName.hashCode()) % AppAccentColors.size]
    val fraction = (app.opens.toFloat() / maxOpens.coerceAtLeast(1)).coerceIn(0f, 1f)

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = app.appName,
                color = MinInk,
                fontFamily = Archivo,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${app.opens}× · ${shortAvg(app.avgSessionMillis)}",
                color = MinMuted,
                fontFamily = JetBrainsMono,
                fontSize = 12.sp,
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(MinFaint)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(1.dp))
                    .background(MinAccent)
            )
        }
    }
}
