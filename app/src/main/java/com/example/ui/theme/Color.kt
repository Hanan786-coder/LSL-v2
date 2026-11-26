package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * "Chalk & Matting" palette.
 *
 * Built around a street pitch at dusk: deep matting green for the ground,
 * chalk for text (the stumps drawn on the wall), and a cool mint for every
 * action the scorer takes. Ball outcomes get their own fixed colours so a
 * glance at the over track reads like a scoreboard.
 */
object Lsl {
    // Ground
    val Pitch = Color(0xFF0F2420)        // app background
    val Turf = Color(0xFF173530)         // cards / panels
    val TurfRaised = Color(0xFF21453E)   // keys, inputs, raised rows
    val Line = Color(0xFF2E5A51)         // hairlines and outlines

    // Ink
    val Chalk = Color(0xFFEEF4EC)        // primary text
    val Moss = Color(0xFF9DBAB0)         // secondary text
    val Fade = Color(0xFF6A8C83)         // tertiary / disabled text

    // Action
    val Mint = Color(0xFF6FE3B5)         // primary action, striker, selection
    val MintDeep = Color(0xFF0E3B2F)     // text on mint

    // Ball outcomes
    val Sun = Color(0xFFFFC94A)          // four
    val Sky = Color(0xFF7CC8FF)          // six
    val Tape = Color(0xFFFF5D5D)         // wicket
    val Lilac = Color(0xFFC6A8FF)        // extras

    // Brand (from the LSL crest) – used sparingly
    val Flame = Color(0xFFFF8A3D)
}
