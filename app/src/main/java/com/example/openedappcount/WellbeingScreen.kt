package com.example.openedappcount

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.openedappcount.ui.theme.CyanAccent
import com.example.openedappcount.ui.theme.GradientEnd
import com.example.openedappcount.ui.theme.GradientMid
import com.example.openedappcount.ui.theme.GradientStart
import com.example.openedappcount.ui.theme.MutedText
import com.example.openedappcount.ui.theme.PurpleAccent
import com.example.openedappcount.ui.theme.SlateDate
import com.example.openedappcount.ui.theme.SlateLabel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WellbeingScreen(vm: WellbeingViewModel = viewModel()) {
    val totalMillis = vm.appUsage.sumOf { it.totalTimeInMillis }
    val maxTime = vm.appUsage.firstOrNull()?.totalTimeInMillis ?: 1L
    val dateStr = remember {
        SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
            .format(Date())
            .uppercase()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(listOf(GradientStart, GradientMid, GradientEnd))
            )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            stickyHeader {
                WellbeingHeader(
                    dateStr = dateStr,
                    unlockCount = vm.unlockCount,
                    totalMillis = totalMillis
                )
            }

            item {
                Text(
                    text = "TOP APPS TODAY",
                    color = SlateLabel,
                    fontSize = 8.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                )
            }

            when {
                vm.isLoading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = CyanAccent)
                        }
                    }
                }
                vm.appUsage.isEmpty() -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No screen time data yet",
                                color = MutedText,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                else -> {
                    items(vm.appUsage) { app ->
                        AppUsageRow(app = app, maxTimeMillis = maxTime)
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }

            item {
                Spacer(
                    Modifier
                        .navigationBarsPadding()
                        .height(8.dp)
                )
            }
        }
    }
}

@Composable
private fun WellbeingHeader(dateStr: String, unlockCount: Int, totalMillis: Long) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.25f))
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = dateStr,
            color = SlateDate,
            fontSize = 9.sp,
            letterSpacing = 1.5.sp
        )
        Text(
            text = "Your Screen Time",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatPill(
                value = unlockCount.toString(),
                label = "Unlocks",
                valueColor = CyanAccent,
                bgColor = CyanAccent.copy(alpha = 0.12f),
                borderColor = CyanAccent.copy(alpha = 0.2f),
                modifier = Modifier.weight(1f)
            )
            StatPill(
                value = formatDuration(totalMillis),
                label = "Screen Time",
                valueColor = PurpleAccent,
                bgColor = PurpleAccent.copy(alpha = 0.12f),
                borderColor = PurpleAccent.copy(alpha = 0.2f),
                modifier = Modifier.weight(1f)
            )
        }
    }
    HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 1.dp)
}

@Composable
private fun StatPill(
    value: String,
    label: String,
    valueColor: Color,
    bgColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(bgColor)
            .border(1.dp, borderColor, shape)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = valueColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        Text(
            text = label,
            color = valueColor.copy(alpha = 0.7f),
            fontSize = 8.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 1.dp)
        )
    }
}
