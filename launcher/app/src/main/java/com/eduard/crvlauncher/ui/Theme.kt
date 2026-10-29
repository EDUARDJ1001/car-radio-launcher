package com.eduard.crvlauncher.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

object Palette {
    val Background = Color(0xFF060D20)
    val BackgroundGlow = Color(0xFF123A8C)
    val Rail = Color(0xFF050B1B)
    val Accent = Color(0xFF2F7BFF)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF9FB3DA)
    val Divider = Color(0x332F4A8A)

    // Large cards (greeting, media): deep navy with a blue lift at the bottom.
    val CardTop = Color(0xFF0B1836)
    val CardBottom = Color(0xFF0D2252)
    val CardBorder = Color(0xFF1E3A72)

    // Shortcut tiles.
    val TileTop = Color(0xFF0E1830)
    val TileBottom = Color(0xFF0A1226)
    val TileBorder = Color(0xFF1C2948)
    val TileActiveTop = Color(0xFF0F2A5E)
    val TileActiveBottom = Color(0xFF0B1C44)

    val ControlBg = Color(0xFF13203F)
    val ControlBorder = Color(0xFF2A3E66)
    val ProgressTrack = Color(0xFF2A3654)

    val PhoneGreen = Color(0xFF22C55E)
    val Gear = Color(0xFFA3ACBF)
}

/**
 * The reference design is 1672×941 px. Every size in the UI is written in those
 * design pixels and scaled to the real screen, so the layout keeps its
 * proportions on a 1024×600 or 1280×720 unit alike.
 */
val LocalUnit = staticCompositionLocalOf { 1.dp }

val Number.u: Dp
    @Composable @ReadOnlyComposable
    get() = LocalUnit.current * this.toFloat()

val Number.us: TextUnit
    @Composable @ReadOnlyComposable
    get() = with(LocalDensity.current) { (LocalUnit.current * this@us.toFloat()).toSp() }

const val DESIGN_W = 1672f
const val DESIGN_H = 941f
