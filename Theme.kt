package com.ascendant.sentiment.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Emerald500,
    secondary = Cyan400,
    tertiary = Amber500,
    background = BgDark,
    surface = CardDark,
    onPrimary = Slate950,
    onSecondary = Slate950,
    onBackground = Slate100,
    onSurface = Slate100
)

@Composable
fun AscendantSentimentTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
