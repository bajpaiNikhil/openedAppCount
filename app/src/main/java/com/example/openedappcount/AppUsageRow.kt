package com.example.openedappcount

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.openedappcount.ui.theme.CyanAccent
import com.example.openedappcount.ui.theme.IconGradients
import com.example.openedappcount.ui.theme.IndigoAccent
import com.example.openedappcount.ui.theme.MutedText
import kotlin.math.roundToInt

@Composable
fun AppUsageRow(app: AppUsageInfo, maxTimeMillis: Long) {
    val safeMax = maxTimeMillis.coerceAtLeast(1L)
    val fraction = (app.totalTimeInMillis.toFloat() / safeMax).coerceIn(0f, 1f)
    val percent = (fraction * 100).roundToInt()
    val letter = app.appName.firstOrNull()?.uppercaseChar() ?: '?'
    val (iconStart, iconEnd) = IconGradients[letter.code % IconGradients.size]
    val rowShape = RoundedCornerShape(10.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(rowShape)
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.07f), rowShape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Brush.linearGradient(listOf(iconStart, iconEnd))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = letter.toString(),
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = app.appName,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "$percent%",
                    color = CyanAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = formatDuration(app.totalTimeInMillis),
                color = MutedText,
                fontSize = 9.sp
            )
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.08f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .background(Brush.linearGradient(listOf(CyanAccent, IndigoAccent)))
                )
            }
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
