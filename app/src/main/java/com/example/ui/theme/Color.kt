package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * "Crest Night" palette, taken from the LSL crest: deep navy ground,
 * flame orange for every action the scorer takes, and fixed colours per
 * ball outcome so the over track reads like a scoreboard at a glance.
 */
object Lsl {
    // Ground
    val Night = Color(0xFF0D1526)        // app background
    val Deck = Color(0xFF16213A)         // cards / panels
    val DeckRaised = Color(0xFF1F2C4A)   // keys, inputs, raised rows
    val Line = Color(0xFF2B3A5C)         // hairlines and outlines

    // Ink
    val Chalk = Color(0xFFF2F5FA)        // primary text
    val Mist = Color(0xFF93A1BC)         // secondary text
    val Fade = Color(0xFF7E8DAA)         // tertiary text

    // Action
    val Flame = Color(0xFFFF7A2F)        // primary action, striker, selection
    val FlameInk = Color(0xFF1A0F06)     // text on flame

    // Ball outcomes
    val Gold = Color(0xFFFFC53D)         // four
    val Sky = Color(0xFF5CC8FF)          // six
    val Tape = Color(0xFFFF4D5E)         // wicket
    val Violet = Color(0xFFB69CFF)       // extras

    // Third place on podiums
    val Bronze = Color(0xFFD89A6A)
}
