package com.example.openedappcount.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.example.openedappcount.R

private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

private val archivoFont = GoogleFont("Archivo")
private val jetBrainsMonoFont = GoogleFont("JetBrains Mono")

// Archivo — display + body: characterful geometric sans, weights 300–900
val Archivo = FontFamily(
    Font(googleFont = archivoFont, fontProvider = provider, weight = FontWeight.Light),
    Font(googleFont = archivoFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = archivoFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = archivoFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = archivoFont, fontProvider = provider, weight = FontWeight.ExtraBold),
    Font(googleFont = archivoFont, fontProvider = provider, weight = FontWeight.Black),
)

// JetBrains Mono — all numbers, timestamps, data labels: reads as "instrumentation"
val JetBrainsMono = FontFamily(
    Font(googleFont = jetBrainsMonoFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = jetBrainsMonoFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = jetBrainsMonoFont, fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = jetBrainsMonoFont, fontProvider = provider, weight = FontWeight.ExtraBold),
)

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
)
