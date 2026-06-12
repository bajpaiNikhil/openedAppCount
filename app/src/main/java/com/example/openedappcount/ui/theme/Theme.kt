package com.example.openedappcount.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val MinimalColorScheme = lightColorScheme(
    background = MinBg,
    surface    = MinBg,
    primary    = MinAccent,
    onPrimary  = MinBg,
    onBackground = MinInk,
    onSurface    = MinInk,
)

@Composable
fun OpenedAppCountTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MinimalColorScheme,
        typography  = Typography,
        content     = content,
    )
}
