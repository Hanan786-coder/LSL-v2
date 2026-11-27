package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val FloodlitPitch = darkColorScheme(
    primary = Lsl.Volt,
    onPrimary = Lsl.VoltInk,
    primaryContainer = Lsl.Forest,
    onPrimaryContainer = Lsl.Chalk,
    secondary = Lsl.Field,
    onSecondary = Lsl.Chalk,
    secondaryContainer = Lsl.Teal,
    onSecondaryContainer = Lsl.Chalk,
    tertiary = Lsl.Aqua,
    onTertiary = Lsl.Night,
    tertiaryContainer = Lsl.Teal,
    onTertiaryContainer = Lsl.Chalk,
    error = Lsl.Tape,
    onError = Lsl.Night,
    background = Lsl.Night,
    onBackground = Lsl.Chalk,
    surface = Lsl.Deck,
    onSurface = Lsl.Chalk,
    surfaceVariant = Lsl.DeckRaised,
    onSurfaceVariant = Lsl.Mist,
    surfaceContainer = Lsl.Deck,
    surfaceContainerHigh = Lsl.Deck,
    surfaceContainerHighest = Lsl.DeckRaised,
    surfaceContainerLow = Lsl.Night,
    outline = Lsl.Line,
    outlineVariant = Lsl.Slate,
    inverseSurface = Lsl.Chalk,
    inverseOnSurface = Lsl.Night,
)

// Hierarchy of corners: small controls are tighter, sheets and the scoreboard softer.
private val LslShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FloodlitPitch,
        typography = Typography,
        shapes = LslShapes,
        content = content
    )
}
