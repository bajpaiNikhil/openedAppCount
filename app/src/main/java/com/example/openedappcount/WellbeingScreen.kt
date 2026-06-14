package com.example.openedappcount

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.layout.size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.openedappcount.ui.theme.Archivo
import com.example.openedappcount.ui.theme.JetBrainsMono
import com.example.openedappcount.ui.theme.MinAccent
import com.example.openedappcount.ui.theme.OrangeAccent
import com.example.openedappcount.ui.theme.MinBg
import com.example.openedappcount.ui.theme.MinFaint
import com.example.openedappcount.ui.theme.MinInk
import com.example.openedappcount.ui.theme.MinLine
import com.example.openedappcount.ui.theme.MinMuted
import java.util.Calendar
import kotlinx.coroutines.delay


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WellbeingScreen(vm: WellbeingViewModel = viewModel()) {
    val cal         = remember { Calendar.getInstance() }
    val today       = remember { cal.get(Calendar.DAY_OF_MONTH) }
    val daysInMonth = remember { cal.getActualMaximum(Calendar.DAY_OF_MONTH) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MinBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 30.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(40.dp))
            Greeting()
            Spacer(Modifier.height(4.dp))
            DateLine()

            // Hero
            Spacer(Modifier.height(44.dp))
            DotMatrixNumber(value = vm.displayedUnlockCount)
            Text(
                text = "UNLOCKS TODAY",
                fontFamily = Archivo,
                fontSize = 13.sp,
                letterSpacing = 3.sp,
                fontWeight = FontWeight.SemiBold,
                color = MinMuted,
                modifier = Modifier.padding(top = 20.dp),
            )
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = OrangeAccent)) {
                        append(formatScreenTime(vm.displayedTotalMillis))
                    }
                    append("  ")
                    withStyle(SpanStyle(color = MinMuted)) { append("active screen time") }
                },
                fontFamily = JetBrainsMono,
                fontSize = 17.sp,
                letterSpacing = 0.sp,
                color = MinInk,
                modifier = Modifier.padding(top = 16.dp),
            )

            Spacer(Modifier.height(28.dp))

            CalendarSection(
                daysInMonth  = daysInMonth,
                today        = today,
                monthlyStats = vm.monthlyDayStats,
                selectedDay  = vm.selectedDay,
                onDayClick   = { d -> vm.selectedDay = if (vm.selectedDay == d) null else d },
            )
            Text(
                text = "TAP A DAY",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
                textAlign = TextAlign.Center,
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
                letterSpacing = 1.5.sp,
                color = MinMuted,
            )
            Spacer(Modifier.height(30.dp))

            CheckPatternSection(
                checkStats = vm.checkStats,
                reflexChecks = vm.reflexChecks,
                reflexAppCount = vm.reflexAppCount,
                mostDeliberate = vm.mostDeliberate,
            )
        }

        val selDay = vm.selectedDay
        if (selDay != null) {
            ModalBottomSheet(
                onDismissRequest = { vm.selectedDay = null },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = Color.White,
                shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
            ) {
                SheetContent(
                    day          = selDay,
                    today        = today,
                    stats        = vm.selectedDayStats,
                    avgUnlocks   = vm.avgUnlockCount,
                    liveUnlocks  = if (selDay == today) vm.displayedUnlockCount else null,
                    liveScreenMs = if (selDay == today) vm.displayedTotalMillis else null,
                )
            }
        }
    }
}

// ── Greeting ──────────────────────────────────────────────────────────────────

private val GREETINGS = listOf(
    "Good morning",
    "Good afternoon",
    "Good evening",
    "Welcome back",
    "Hey there",
    "There you are",
    "Nice to see you",
    "Glad you're here",
    "Look who's here",
    "Back again",
    "Rise and shine",
    "Deep breath",
    "Breathe first",
    "Eyes open",
    "Stay grounded",
    "Be present",
    "Focus up",
    "Locked in",
    "Steady on",
    "One more day",
    "Another day",
    "Starting fresh",
    "Here we go",
    "Let's do this",
    "Make it count",
    "Own your time",
    "No time like now",
    "Time flies",
    "Stay curious",
    "Keep it real",
    "Still here",
    "Good to see you",
    "Welcome home",
    "The day is yours",
    "You've got this",
    "Making moves",
    "Game on",
    "Full send",
    "Slow down",
    "Still tracking",
    "Checking in",
    "Present and accounted for",
    "Here's your data",
    "The numbers don't lie",
    "Let's be intentional",
    "Mindful moment",
    "One screen at a time",
    "Presence over scrolling",
    "What a day it'll be",
    "The world waits",
)

private val MONIKERS = listOf(
    "friend", "champ", "legend", "chief", "ace",
    "captain", "boss", "hero", "star", "partner",
    "champion", "maverick", "trailblazer", "explorer", "pioneer",
    "warrior", "dreamer", "achiever", "thinker", "creator",
    "builder", "seeker", "wanderer", "maker", "doer",
    "hustler", "grinder", "striver", "mover", "shaker",
    "scholar", "sage", "oracle", "visionary", "strategist",
    "navigator", "pathfinder", "voyager", "ranger", "scout",
    "soul", "spirit", "mind", "force", "spark",
    "flame", "tide", "wave", "light", "north",
)

@Composable
private fun Greeting() {
    val greeting = remember { GREETINGS.random() }
    val moniker  = remember { MONIKERS.random() }
    Text(
        text = buildAnnotatedString {
            append("$greeting, ")
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = MinInk)) {
                append(moniker)
            }
            append(".")
        },
        fontFamily = Archivo,
        fontSize = 17.sp,
        color = MinMuted,
        fontWeight = FontWeight.Normal,
    )
}

@Composable
private fun DateLine() {
    val cal = remember { Calendar.getInstance() }
    val dowAbbrev = remember {
        arrayOf("SUN","MON","TUE","WED","THU","FRI","SAT")[cal.get(Calendar.DAY_OF_WEEK) - 1]
    }
    val day   = remember { cal.get(Calendar.DAY_OF_MONTH) }
    val month = remember {
        arrayOf("JANUARY","FEBRUARY","MARCH","APRIL","MAY","JUNE",
                "JULY","AUGUST","SEPTEMBER","OCTOBER","NOVEMBER","DECEMBER")[cal.get(Calendar.MONTH)]
    }
    val year = remember { cal.get(Calendar.YEAR) }
    Text(
        text = buildAnnotatedString {
            append(dowAbbrev)
            append(" ")
            withStyle(SpanStyle(color = MinAccent)) { append("·") }
            append(" $day $month $year")
        },
        fontFamily = JetBrainsMono,
        fontSize = 13.sp,
        letterSpacing = 2.5.sp,
        color = MinInk,
        fontWeight = FontWeight.SemiBold,
    )
}

// ── Calendar ──────────────────────────────────────────────────────────────────

private val DOW_HEADERS = listOf("M","T","W","T","F","S","S")

@Composable
private fun CalendarSection(
    daysInMonth: Int,
    today: Int,
    monthlyStats: List<DayStats?>,
    selectedDay: Int?,
    onDayClick: (Int) -> Unit,
) {
    val firstDow = remember {
        Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }.let { c ->
            val dow = c.get(Calendar.DAY_OF_WEEK)
            if (dow == Calendar.SUNDAY) 6 else dow - 2
        }
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        DOW_HEADERS.forEachIndexed { i, label ->
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                color = if (i >= 5) MinInk else MinMuted,
            )
        }
    }
    Spacer(Modifier.height(14.dp))

    val cells: List<Int?> = List(firstDow) { null } + (1..daysInMonth).map { it }
    cells.chunked(7).forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            row.forEach { day ->
                Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                    if (day != null) {
                        DayCell(
                            day        = day,
                            today      = today,
                            stats      = monthlyStats.getOrNull(day - 1),
                            isSelected = day == selectedDay,
                            onClick    = { onDayClick(day) },
                        )
                    }
                }
            }
            repeat(7 - row.size) { Box(modifier = Modifier.weight(1f).aspectRatio(1f)) }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun DayCell(
    day: Int,
    today: Int,
    stats: DayStats?,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val isFuture = day > today
    val isToday  = day == today

    val bgColor = when {
        isToday  -> MinAccent
        isFuture -> Color.Transparent
        else     -> unlockDotColor(stats?.unlockCount ?: 0)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(bgColor)
            .then(
                if (isFuture)
                    Modifier.border(1.5.dp, MinFaint, CircleShape)
                else Modifier
            )
            .then(
                if (isSelected && isToday)
                    Modifier.border(2.5.dp, MinBg, CircleShape)
                else if (isSelected)
                    Modifier.border(2.5.dp, MinInk, CircleShape)
                else Modifier
            )
            .clickable(enabled = !isFuture, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isToday || isFuture) {
            Text(
                text = day.toString(),
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isToday) Color.White else MinMuted.copy(alpha = 0.5f),
            )
        }
    }
}

private fun unlockDotColor(unlocks: Int): Color {
    val t = (unlocks / 110f).coerceIn(0f, 1f)
    val v = (220 + (17 - 220) * t).toInt()
    return Color(v, v, v)
}

// ── Bottom sheet ──────────────────────────────────────────────────────────────

private val MONTH_NAMES = arrayOf(
    "January","February","March","April","May","June",
    "July","August","September","October","November","December",
)
private val MONTH_ABBREVS = arrayOf(
    "JAN","FEB","MAR","APR","MAY","JUN","JUL","AUG","SEP","OCT","NOV","DEC",
)
private val DOW_FULL = arrayOf("Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday")

@Composable
private fun SheetContent(
    day: Int,
    today: Int,
    stats: DayStats?,
    avgUnlocks: Int?,
    liveUnlocks: Int?,
    liveScreenMs: Long?,
) {
    val cal      = remember { Calendar.getInstance() }
    val monthIdx = remember { cal.get(Calendar.MONTH) }
    val isToday  = day == today
    val unlocks  = liveUnlocks ?: stats?.unlockCount
    val screenMs = liveScreenMs ?: stats?.totalScreenMillis

    val selectedDow = remember(day) {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, day)
        }.get(Calendar.DAY_OF_WEEK) - 1
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 30.dp)
            .padding(bottom = 38.dp)
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column {
                Text(
                    text = "${MONTH_NAMES[monthIdx]} $day",
                    fontFamily = Archivo,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp,
                    letterSpacing = (-0.5).sp,
                    color = MinInk,
                )
                Text(
                    text = DOW_FULL[selectedDow],
                    fontFamily = JetBrainsMono,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    color = MinMuted,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            Text(
                text = if (isToday) "TODAY" else "${MONTH_ABBREVS[monthIdx]} $day",
                modifier = Modifier
                    .background(Color(0xFFF3F3F3), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                fontFamily = JetBrainsMono,
                fontSize = 9.sp,
                letterSpacing = 1.sp,
                color = MinInk,
            )
        }

        Spacer(Modifier.height(24.dp))

        // Stats row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val stroke = 1.dp.toPx()
                    val c = Color(0xFFEFEFEF)
                    drawLine(c, Offset(0f, 0f), Offset(size.width, 0f), stroke)
                    drawLine(c, Offset(0f, size.height), Offset(size.width, size.height), stroke)
                }
        ) {
            SheetStat(
                key = "Unlocks",
                value = unlocks?.toString() ?: "—",
                detail = when {
                    isToday -> "still counting"
                    avgUnlocks != null && unlocks != null -> {
                        val diff = unlocks - avgUnlocks
                        when {
                            diff > 0 -> "▲ $diff vs avg"
                            diff < 0 -> "▼ ${-diff} vs avg"
                            else -> "· avg"
                        }
                    }
                    else -> ""
                },
                modifier = Modifier.weight(1f),
            )
            VerticalDivider(color = MinLine, modifier = Modifier.height(100.dp))
            SheetStat(
                key = "Active screen time",
                value = screenMs?.let { formatScreenTime(it) } ?: "—",
                detail = if (isToday) "still counting · incl. home" else "incl. home navigation",
                modifier = Modifier.weight(1f).padding(start = 20.dp),
                valueColor = OrangeAccent,
            )
        }

        // Hourly bars
        val hourlyData = stats?.hourlyUnlocks ?: List(24) { 0 }
        val maxHourly  = hourlyData.maxOrNull()?.coerceAtLeast(1) ?: 1
        val peakHour   = hourlyData.indexOfFirst { it == maxHourly }

        Spacer(Modifier.height(24.dp))
        Text(
            text = "BY HOUR",
            fontFamily = Archivo,
            fontSize = 10.sp,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.SemiBold,
            color = MinMuted,
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth().height(42.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            hourlyData.forEachIndexed { i, v ->
                val frac = v.toFloat() / maxHourly
                val barH = if (v == 0) 2.dp else (4 + frac * 38).dp
                val color = when {
                    i == peakHour && v > 0 -> MinAccent
                    v > 0 -> MinInk
                    else  -> MinFaint
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(barH)
                        .background(color, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            listOf("12a","6a","12p","6p","11p").forEach { label ->
                Text(label, fontFamily = JetBrainsMono, fontSize = 8.sp, color = MinMuted)
            }
        }

        // First / last pickup
        val firstTxt = stats?.firstPickup ?: "—"
        val lastTxt  = stats?.lastPickup  ?: "—"
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = MinMuted)) { append("First pickup  ") }
                    withStyle(SpanStyle(color = MinInk, fontWeight = FontWeight.Bold)) { append(firstTxt) }
                },
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
            )
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = MinMuted)) { append("Last  ") }
                    withStyle(SpanStyle(color = MinInk, fontWeight = FontWeight.Bold)) { append(lastTxt) }
                },
                fontFamily = JetBrainsMono,
                fontSize = 11.sp,
            )
        }

        // Top app card
        val topApp = stats?.topAppName
        if (!topApp.isNullOrBlank() && (stats?.topAppOpens ?: 0) > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp)
                    .background(Color(0xFFFAFAFA), RoundedCornerShape(16.dp))
                    .border(1.dp, MinLine, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .background(MinInk, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = topApp.first().uppercaseChar().toString(),
                        fontFamily = Archivo,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                    )
                }
                Column {
                    Text(
                        "MOST OPENED",
                        fontFamily = Archivo,
                        fontSize = 10.sp,
                        letterSpacing = 1.5.sp,
                        color = MinMuted,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "$topApp · ${stats?.topAppOpens} opens",
                        fontFamily = Archivo,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MinInk,
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetStat(
    key: String,
    value: String,
    detail: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MinInk,
) {
    Column(modifier = modifier.padding(vertical = 18.dp)) {
        Text(
            key.uppercase(),
            fontFamily = Archivo,
            fontSize = 10.sp,
            letterSpacing = 2.sp,
            color = MinMuted,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = value,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 34.sp,
            lineHeight = 34.sp,
            letterSpacing = (-1).sp,
            color = valueColor,
        )
        if (detail.isNotBlank()) {
            Text(
                text = detail,
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp,
                color = MinMuted,
                modifier = Modifier.padding(top = 7.dp),
            )
        }
    }
}

// ── Dot-matrix hero number ────────────────────────────────────────────────────

// Classic 5×7 LED-matrix glyphs; each row is 5 chars, 'X' = on
private val RAW_DIGITS = arrayOf(
    arrayOf(" XXX ", "X   X", "X   X", "X   X", "X   X", "X   X", " XXX "), // 0
    arrayOf("  X  ", " XX  ", "  X  ", "  X  ", "  X  ", "  X  ", " XXX "), // 1
    arrayOf(" XXX ", "X   X", "    X", "  XX ", " X   ", "X    ", "XXXXX"), // 2
    arrayOf(" XXX ", "X   X", "    X", "  XX ", "    X", "X   X", " XXX "), // 3
    arrayOf("X   X", "X   X", "XXXXX", "    X", "    X", "    X", "    X"), // 4
    arrayOf("XXXXX", "X    ", "XXXX ", "    X", "    X", "X   X", " XXX "), // 5
    arrayOf(" XXX ", "X    ", "XXXX ", "X   X", "X   X", "X   X", " XXX "), // 6
    arrayOf("XXXXX", "    X", "   X ", "  X  ", "  X  ", "  X  ", "  X  "), // 7
    arrayOf(" XXX ", "X   X", "X   X", " XXX ", "X   X", "X   X", " XXX "), // 8
    arrayOf(" XXX ", "X   X", "X   X", " XXXX", "    X", "    X", " XXX "), // 9
)

private val DIGIT_DOTS: Array<Array<BooleanArray>> = RAW_DIGITS.map { rows ->
    Array(7) { r -> BooleanArray(5) { c -> rows[r][c] == 'X' } }
}.toTypedArray()

private val DOT_SIZE    = 15.dp
private val DOT_GAP     = 5.dp
private val DIGIT_GAP   = 14.dp
private const val STAGGER_MS = 10

@Composable
private fun DotMatrixNumber(value: Int, modifier: Modifier = Modifier) {
    val digits = value.coerceAtLeast(0).toString().map { it.digitToInt() }
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(120) // let data settle before ignition
        entered = true
    }
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(DIGIT_GAP)) {
        digits.forEachIndexed { idx, digit ->
            DotMatrixDigit(digit = digit, entered = entered, digitIndex = idx)
        }
    }
}

@Composable
private fun DotMatrixDigit(digit: Int, entered: Boolean, digitIndex: Int) {
    val matrix = DIGIT_DOTS[digit]
    val baseOffset = digitIndex * 35 * STAGGER_MS
    Column(verticalArrangement = Arrangement.spacedBy(DOT_GAP)) {
        for (row in 0..6) {
            Row(horizontalArrangement = Arrangement.spacedBy(DOT_GAP)) {
                for (col in 0..4) {
                    val lit = matrix[row][col]
                    val delay = if (entered && lit) baseOffset + (row * 5 + col) * STAGGER_MS else 0
                    MatrixDot(lit = lit && entered, delayMs = delay)
                }
            }
        }
    }
}

@Composable
private fun MatrixDot(lit: Boolean, delayMs: Int) {
    val color by animateColorAsState(
        targetValue = if (lit) MinAccent else MinFaint,
        animationSpec = tween(durationMillis = 280, delayMillis = delayMs),
        label = "dotColor",
    )
    val dotScale by animateFloatAsState(
        targetValue = if (lit) 1f else 0.42f,
        animationSpec = tween(durationMillis = 260, delayMillis = delayMs),
        label = "dotScale",
    )
    Box(
        Modifier
            .size(DOT_SIZE)
            .scale(dotScale)
            .background(color, CircleShape)
    )
}

// ── Shared utils ──────────────────────────────────────────────────────────────

private fun formatScreenTime(millis: Long): String {
    val mins = millis / 60_000
    val h = mins / 60
    val m = mins % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
