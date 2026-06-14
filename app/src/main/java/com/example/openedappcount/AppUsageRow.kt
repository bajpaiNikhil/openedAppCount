package com.example.openedappcount

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.openedappcount.ui.theme.AppAccentColors
import com.example.openedappcount.ui.theme.SectionDiv
import com.example.openedappcount.ui.theme.TextDim
import com.example.openedappcount.ui.theme.TextMid
import com.example.openedappcount.ui.theme.TextPrimary
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun AppUsageRow(
    app: AppUsageInfo,
    rank: Int,
    maxTimeMillis: Long,
    totalTimeMillis: Long
) {
    val fraction = (app.totalTimeInMillis.toFloat() / maxTimeMillis.coerceAtLeast(1L)).coerceIn(0f, 1f)
    val percent = if (totalTimeMillis > 0)
        ((app.totalTimeInMillis.toFloat() / totalTimeMillis) * 100).roundToInt()
    else 0
    val accent = AppAccentColors[abs(app.appName.hashCode()) % AppAccentColors.size]
    val initials = appInitials(app.appName)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank
        Text(
            text = "$rank",
            color = TextDim,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(16.dp)
        )
        Spacer(Modifier.width(10.dp))
        // Icon
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(10.dp))
        // Name + time
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.appName,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatDuration(app.totalTimeInMillis),
                color = TextMid,
                fontSize = 11.sp
            )
        }
        Spacer(Modifier.width(10.dp))
        // Progress bar + %
        Column(
            modifier = Modifier.width(80.dp),
            horizontalAlignment = Alignment.End
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(SectionDiv)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .background(accent)
                )
            }
            Text(
                text = "$percent%",
                color = accent,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
    }
}

fun formatDuration(millis: Long): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    return when {
        hours > 0 -> "${hours}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m"
        else -> "${seconds}s"
    }
}

/**
 * Compact average-session formatter for the "Checked, not used" section.
 * Unlike [formatDuration], keeps leftover seconds for sub-10-minute durations
 * (e.g. "1m 50s") since these averages are usually small.
 */
fun shortAvg(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return when {
        minutes == 0L -> "${seconds}s"
        seconds == 0L -> "${minutes}m"
        else -> "${minutes}m ${seconds}s"
    }
}

private fun appInitials(name: String): String {
    val words = name.trim().split(Regex("\\s+"))
    return if (words.size >= 2) {
        "${words[0].first().uppercaseChar()}${words[1].first().lowercaseChar()}"
    } else if (name.length >= 2) {
        "${name[0].uppercaseChar()}${name[1].lowercaseChar()}"
    } else {
        name.uppercase()
    }
}
