package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AmoledStatisticalColorScheme = darkColorScheme(
    primary = ImperialGold,
    onPrimary = Color(0xFF140D00),
    primaryContainer = ImperialGoldContainer,
    onPrimaryContainer = ImperialGoldLight,
    secondary = EmeraldAccent,
    onSecondary = Color(0xFF001C12),
    secondaryContainer = EmeraldContainer,
    onSecondaryContainer = EmeraldLight,
    tertiary = UnderCyan,
    onTertiary = Color(0xFF001A21),
    tertiaryContainer = UnderCyanContainer,
    onTertiaryContainer = UnderCyanLight,
    background = AmoledBlack,
    onBackground = TextPrimaryCream,
    surface = DeepFeltSurface,
    onSurface = TextPrimaryCream,
    surfaceVariant = ElevatedFeltCard,
    onSurfaceVariant = TextSecondarySage,
    outline = GlassFeltBorder,
    error = LossCrimson,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = AmoledStatisticalColorScheme,
        typography = Typography,
        content = content
    )
}
