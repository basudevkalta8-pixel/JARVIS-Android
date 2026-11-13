package com.stark.jarvis.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val StarkNavyBg = Color(0xFF030712)
val StarkPanelBg = Color(0xFF05112E)
val StarkHeaderBg = Color(0xFF020817)
val StarkCyan = Color(0xFF06B6D4)
val StarkCyanGlow = Color(0xFF22D3EE)
val StarkTextPrimary = Color(0xFFCFFAFE)
val StarkTextMuted = Color(0xFF67E8F9)
val StarkEmerald = Color(0xFF34D399)
val StarkAmber = Color(0xFFFBBF24)
val StarkRose = Color(0xFFFB7185)

private val JarvisColorScheme = darkColorScheme(
    primary = StarkCyan,
    secondary = StarkEmerald,
    tertiary = StarkAmber,
    background = StarkNavyBg,
    surface = StarkPanelBg,
    onPrimary = Color.Black,
    onBackground = StarkTextPrimary,
    onSurface = StarkTextPrimary
)

@Composable
fun JarvisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = JarvisColorScheme,
        content = content
    )
}
