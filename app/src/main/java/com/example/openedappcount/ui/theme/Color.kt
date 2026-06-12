package com.example.openedappcount.ui.theme

import androidx.compose.ui.graphics.Color

// Material baseline (kept for Theme.kt)
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650A4)
val PurpleGrey40 = Color(0xFF625B71)
val Pink40 = Color(0xFF7D5260)

// ── Design system ────────────────────────────────────────────────────────────
val ScreenBg      = Color(0xFF0D0F1A)   // phone background
val CardBg        = Color(0xFF111527)   // card fill
val CardBorder    = Color(0xFF1A1F38)   // card border
val SectionDiv    = Color(0xFF0F1221)   // row dividers

val BlueAccent    = Color(0xFF3D7FFF)   // primary accent (unlocks, tabs, timeline peak)
val PurpleAccent  = Color(0xFFA78BFA)   // secondary accent
val OrangeAccent  = Color(0xFFF97316)   // screen time highlight
val GreenAccent   = Color(0xFF34D399)   // streak success

val TextHeading   = Color(0xFFE8EAF2)   // "Screen Habits" title
val TextPrimary   = Color(0xFFC9CCE0)   // app names, values
val TextMid       = Color(0xFF4B5280)   // date, tab inactive labels
val TextDim       = Color(0xFF3A3F5C)   // section labels, meta

val UnlockCardBg  = Color(0xFF161F3A)
val UnlockCardBd  = Color(0xFF1E2A4A)
val TimeCardBg    = Color(0xFF131A2E)
val TimeCardBd    = Color(0xFF1A2240)
val TabInactiveBg = Color(0xFF161929)

// Bar tiers in the unlock timeline
val BarPeak       = BlueAccent
val BarHigh       = Color(0xFF2A3B6A)
val BarMid        = Color(0xFF1E2545)
val BarEmpty      = Color(0xFF1A1F38)

// ── Per-app accent palette (cycled by appName.hashCode) ─────────────────────
val AppAccentColors = listOf(
    Color(0xFFEF4444), // red
    Color(0xFFF97316), // orange
    Color(0xFF38BDF8), // cyan
    Color(0xFF34D399), // green
    Color(0xFF818CF8), // indigo
    Color(0xFF60A5FA), // blue
    Color(0xFFF59E0B), // amber
    Color(0xFFEC4899), // pink
)

// ── Minimal home screen palette ──────────────────────────────────────────
val MinBg     = Color(0xFFFFFFFF)
val MinInk    = Color(0xFF111111)
val MinMuted  = Color(0xFF9A9A9A)
val MinFaint  = Color(0xFFECECEC)
val MinAccent = Color(0xFFFF5A1F)
val MinLine   = Color(0xFFEFEFEF)

// Legacy — kept for any references in the widget / older code
val CyanAccent   = Color(0xFF38BDF8)
val IndigoAccent = Color(0xFF818CF8)
val MutedText    = Color(0xFF94A3B8)
val SlateLabel   = Color(0xFF475569)
val SlateDate    = Color(0xFF64748B)
val GradientStart = Color(0xFF0F0C29)
val GradientMid   = Color(0xFF302B63)
val GradientEnd   = Color(0xFF24243E)
val IconGradients: List<Pair<androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color>> = listOf(
    Pair(Color(0xFFF43F5E), Color(0xFFFB923C)),
    Pair(Color(0xFFEF4444), Color(0xFF7F1D1D)),
    Pair(Color(0xFF22C55E), Color(0xFF14532D)),
    Pair(Color(0xFF6366F1), Color(0xFF312E81)),
    Pair(Color(0xFFF59E0B), Color(0xFFCA8A04)),
    Pair(Color(0xFF14B8A6), Color(0xFF0E7490)),
    Pair(Color(0xFFEC4899), Color(0xFFC026D3)),
    Pair(Color(0xFF3B82F6), Color(0xFF1E3A8A)),
)
