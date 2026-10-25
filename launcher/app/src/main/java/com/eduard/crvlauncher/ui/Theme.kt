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
    val Background = Color(0xFF050B1F)
    val BackgroundGlow = Color(0xFF0F2A78)
    val Rail = Color(0xFF070F27)
    val RailSelected = Color(0xFF0C1C47)
    val Accent = Color(0xFF3D8BFF)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFA9BCE8)
    val Divider = Color(0x332F4A8A)

    val HeroTop = Color(0xFF123A9E)
    val HeroBottom = Color(0xFF0A1F5E)
    val HeroBorder = Color(0xFF2C57C2)

    val NavTop = Color(0xFF0C2160)
    val NavBottom = Color(0xFF0A1A4C)
    val NavBorder = Color(0xFF2F6BE6)

    val MediaTop = Color(0xFF2B1F9A)
    val MediaBottom = Color(0xFF3A2BC4)
    val MediaBorder = Color(0xFF5B49E8)

    val TileTop = Color(0xFF0E1A3C)
    val TileBottom = Color(0xFF0A1430)
    val TileBorder = Color(0xFF1F2E5C)

    val PhoneGreen = Color(0xFF22C55E)
    val CarPlayGreen = Color(0xFF2BD15A)
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
