package com.example.openedappcount.ui.theme

import androidx.compose.ui.graphics.Color

// Material baseline (kept for Theme.kt)
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650A4)
val PurpleGrey40 = Color(0xFF625B71)
val Pink40 = Color(0xFF7D5260)

// Dashboard background gradient
val GradientStart = Color(0xFF0F0C29)
val GradientMid   = Color(0xFF302B63)
val GradientEnd   = Color(0xFF24243E)

// Accent
val CyanAccent   = Color(0xFF38BDF8)
val PurpleAccent = Color(0xFFA78BFA)
val IndigoAccent = Color(0xFF818CF8)
val MutedText    = Color(0xFF94A3B8)
val SlateLabel   = Color(0xFF475569)  // "TOP APPS TODAY" label
val SlateDate    = Color(0xFF64748B)  // date text in header

// Letter-icon gradient pairs (start → end), 8 entries cycling by char code
val IconGradients: List<Pair<Color, Color>> = listOf(
    Pair(Color(0xFFF43F5E), Color(0xFFFB923C)), // Rose → Orange
    Pair(Color(0xFFEF4444), Color(0xFF7F1D1D)), // Red → Dark-red
    Pair(Color(0xFF22C55E), Color(0xFF14532D)), // Green → Dark-green
    Pair(Color(0xFF6366F1), Color(0xFF312E81)), // Indigo → Dark-indigo
    Pair(Color(0xFFF59E0B), Color(0xFFCA8A04)), // Amber → Yellow
    Pair(Color(0xFF14B8A6), Color(0xFF0E7490)), // Teal → Cyan
    Pair(Color(0xFFEC4899), Color(0xFFC026D3)), // Pink → Fuchsia
    Pair(Color(0xFF3B82F6), Color(0xFF1E3A8A)), // Blue → Dark-blue
)
