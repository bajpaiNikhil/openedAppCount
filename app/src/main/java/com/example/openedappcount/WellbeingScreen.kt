package com.example.openedappcount

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.openedappcount.ui.theme.BlueAccent
import com.example.openedappcount.ui.theme.CardBg
import com.example.openedappcount.ui.theme.CardBorder
import com.example.openedappcount.ui.theme.PurpleAccent
import com.example.openedappcount.ui.theme.ScreenBg
import com.example.openedappcount.ui.theme.SectionDiv
import com.example.openedappcount.ui.theme.TabInactiveBg
import com.example.openedappcount.ui.theme.TextDim
import com.example.openedappcount.ui.theme.TextHeading
import com.example.openedappcount.ui.theme.TextMid
import com.example.openedappcount.ui.theme.TextPrimary
import com.example.openedappcount.ui.theme.TimeCardBd
import com.example.openedappcount.ui.theme.TimeCardBg
import com.example.openedappcount.ui.theme.UnlockCardBd
import com.example.openedappcount.ui.theme.UnlockCardBg
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WellbeingScreen(vm: WellbeingViewModel = viewModel()) {
    val displayedUsage = vm.displayedAppUsage
    val totalMillis = vm.displayedTotalMillis
    val maxTime = displayedUsage.firstOrNull()?.totalTimeInMillis ?: 1L
    val dateStr = remember {
        SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBg)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {

            // ── Header ───────────────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Text(
                        text = dateStr,
                        color = TextMid,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Screen Habits",
                        color = TextHeading,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // ── Global tab row (Today / This Week / Avg Day) ─────────────
            item {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val tabs = listOf(
                        "Today" to LeaderboardPeriod.TODAY,
                        "This Week" to LeaderboardPeriod.WEEK,
                        "Avg / Day" to LeaderboardPeriod.AVG
                    )
                    tabs.forEach { (label, period) ->
                        val selected = vm.globalPeriod == period
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (selected) BlueAccent else TabInactiveBg)
                                .clickable { vm.globalPeriod = period }
                                .padding(horizontal = 14.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (selected) Color.White else TextMid,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // ── Hero cards (unlocks + screen time) ────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val unlockLabel = when (vm.globalPeriod) {
                        LeaderboardPeriod.TODAY -> "UNLOCKS"
                        LeaderboardPeriod.WEEK  -> "UNLOCKS · WEEK"
                        LeaderboardPeriod.AVG   -> "UNLOCKS · AVG/DAY"
                    }
                    val timeLabel = when (vm.globalPeriod) {
                        LeaderboardPeriod.TODAY -> "SCREEN TIME"
                        LeaderboardPeriod.WEEK  -> "SCREEN TIME · WEEK"
                        LeaderboardPeriod.AVG   -> "SCREEN TIME · AVG/DAY"
                    }
                    HeroCard(
                        value = vm.displayedUnlockCount.toString(),
                        label = unlockLabel,
                        valueColor = BlueAccent,
                        bgColor = UnlockCardBg,
                        borderColor = UnlockCardBd,
                        modifier = Modifier.weight(1f)
                    )
                    HeroCard(
                        value = formatDuration(totalMillis),
                        label = timeLabel,
                        valueColor = PurpleAccent,
                        bgColor = TimeCardBg,
                        borderColor = TimeCardBd,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ── Unlock Timeline ───────────────────────────────────────────
            item { SectionLabel("Unlock timeline") }
            item {
                val timeline = vm.unlockTimeline
                if (vm.isLoadingExtended || timeline == null) {
                    ExtendedLoadingCard()
                } else {
                    UnlockTimelineSection(timeline = timeline)
                }
            }

            // ── Top Apps (period-aware) ───────────────────────────────────
            item {
                val topAppsLabel = when (vm.globalPeriod) {
                    LeaderboardPeriod.TODAY -> "Top apps today"
                    LeaderboardPeriod.WEEK  -> "Top apps · this week"
                    LeaderboardPeriod.AVG   -> "Top apps · avg / day"
                }
                SectionLabel(topAppsLabel)
            }
            item {
                val showLoadingToday = vm.globalPeriod == LeaderboardPeriod.TODAY && vm.isLoading
                val showLoadingExtended = vm.globalPeriod != LeaderboardPeriod.TODAY && vm.isLoadingExtended
                when {
                    showLoadingToday || showLoadingExtended -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = BlueAccent, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        }
                    }
                    displayedUsage.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No screen time data yet", color = TextDim, fontSize = 12.sp)
                        }
                    }
                    else -> {
                        ListCard {
                            displayedUsage.take(7).forEachIndexed { i, app ->
                                AppUsageRow(
                                    app = app,
                                    rank = i + 1,
                                    maxTimeMillis = maxTime,
                                    totalTimeMillis = totalMillis
                                )
                                if (i < minOf(displayedUsage.size, 7) - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 0.dp),
                                        color = SectionDiv,
                                        thickness = 1.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── App Opens Leaderboard ─────────────────────────────────────
            item { SectionLabel("App opens leaderboard") }
            item {
                if (vm.isLoadingExtended) {
                    ExtendedLoadingCard()
                } else {
                    AppOpenLeaderboardSection(
                        apps = vm.appOpenLeaderboard,
                        period = vm.leaderboardPeriod,
                        onPeriodChange = { vm.leaderboardPeriod = it }
                    )
                }
            }

            // ── Session Patterns ──────────────────────────────────────────
            item { SectionLabel("Session patterns") }
            item {
                val sessions = vm.sessionSummary
                if (vm.isLoadingExtended || sessions == null) {
                    ExtendedLoadingCard()
                } else {
                    SessionSection(summary = sessions)
                }
            }

            // ── Streaks ───────────────────────────────────────────────────
            item { SectionLabel("Streaks") }
            item {
                if (vm.isLoadingExtended) {
                    ExtendedLoadingCard()
                } else {
                    StreakSection(streaks = vm.streaks)
                }
            }

            item {
                Spacer(Modifier.navigationBarsPadding().height(8.dp))
            }
        }
    }
}

@Composable
private fun HeroCard(
    value: String,
    label: String,
    valueColor: Color,
    bgColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(bgColor)
            .border(1.dp, borderColor, shape)
            .padding(16.dp)
    ) {
        Text(
            text = value,
            color = valueColor,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            lineHeight = 28.sp
        )
        Text(
            text = label,
            color = TextDim,
            fontSize = 11.sp,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = TextDim,
        fontSize = 10.sp,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(start = 20.dp, bottom = 10.dp, top = 4.dp)
    )
}

@Composable
fun ListCard(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .padding(bottom = 18.dp)
            .clip(shape)
            .background(CardBg)
            .border(1.dp, CardBorder, shape)
    ) {
        content()
    }
}

@Composable
private fun ExtendedLoadingCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 18.dp)
            .height(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = BlueAccent, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
    }
}
