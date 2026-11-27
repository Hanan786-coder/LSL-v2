package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * "Floodlit Pitch" palette. The ground is a deep pitch green taken from
 * #005543, the volt lime #8BF108 marks every action the scorer takes, and
 * teal, field green and slate fill the supporting roles. Ball outcomes keep
 * fixed colours so the over track reads like a scoreboard at a glance; gold
 * (four) and red (wicket) stay outside the brand palette because they carry
 * meaning, not decoration.
 */
object Lsl {
    // Brand palette
    val Slate = Color(0xFF6E818A)        // disabled, quiet outlines
    val Teal = Color(0xFF0D8888)         // secondary fills and containers
    val Field = Color(0xFF00A260)        // positive fills
    val Grass = Color(0xFF22C77F)        // keepers, positive text (Field, lifted)
    val Forest = Color(0xFF005543)       // hairlines, launch background

    // Ground (pitch green, darkened from Forest)
    val Night = Color(0xFF03201A)        // app background
    val Deck = Color(0xFF082C24)         // cards / panels
    val DeckRaised = Color(0xFF0E3A30)   // keys, inputs, raised rows
    val Line = Forest                    // hairlines and outlines

    // Ink (slate, lifted so it stays readable on the dark ground)
    val Chalk = Color(0xFFEEF6F2)        // primary text
    val Mist = Color(0xFFA6B8BF)         // secondary text
    val Fade = Color(0xFF8499A2)         // tertiary text

    // Action
    val Volt = Color(0xFF8BF108)         // primary action, striker, selection, six
    val VoltInk = Color(0xFF06281F)      // text on volt

    // Ball outcomes
    val Gold = Color(0xFFFFC53D)         // four, trophies
    val Aqua = Color(0xFF2FCFC4)         // extras, second innings (teal, lifted)
    val Tape = Color(0xFFFF5A64)         // wicket

    // Third place on podiums
    val Bronze = Color(0xFFD89A6A)
}
