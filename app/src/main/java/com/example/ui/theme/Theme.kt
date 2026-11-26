package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ChalkAndMatting = darkColorScheme(
    primary = Lsl.Mint,
    onPrimary = Lsl.MintDeep,
    primaryContainer = Lsl.TurfRaised,
    onPrimaryContainer = Lsl.Chalk,
    secondary = Lsl.Sun,
    onSecondary = Lsl.Pitch,
    tertiary = Lsl.Sky,
    onTertiary = Lsl.Pitch,
    error = Lsl.Tape,
    onError = Lsl.Pitch,
    background = Lsl.Pitch,
    onBackground = Lsl.Chalk,
    surface = Lsl.Turf,
    onSurface = Lsl.Chalk,
    surfaceVariant = Lsl.TurfRaised,
    onSurfaceVariant = Lsl.Moss,
    surfaceContainer = Lsl.Turf,
    surfaceContainerHigh = Lsl.Turf,
    surfaceContainerHighest = Lsl.TurfRaised,
    surfaceContainerLow = Lsl.Pitch,
    outline = Lsl.Line,
    outlineVariant = Lsl.Line,
    inverseSurface = Lsl.Chalk,
    inverseOnSurface = Lsl.Pitch,
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
        colorScheme = ChalkAndMatting,
        typography = Typography,
        shapes = LslShapes,
        content = content
    )
}
