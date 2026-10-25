package com.eduard.crvlauncher

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.eduard.crvlauncher.data.GeoFix
import com.eduard.crvlauncher.ui.AppBackground
import com.eduard.crvlauncher.ui.DESIGN_H
import com.eduard.crvlauncher.ui.DESIGN_W
import com.eduard.crvlauncher.ui.HomeActions
import com.eduard.crvlauncher.ui.HomeScreen
import com.eduard.crvlauncher.ui.HomeState
import com.eduard.crvlauncher.ui.LocalUnit
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLooper
import java.io.File

/**
 * Renders the home screen with the real offline map (../mapas/honduras.map on
 * the PC). Mapsforge draws tiles on background threads, so we pump the looper
 * for a while before capturing.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LiveMapScreenshotTest {

    @get:Rule val compose = createComposeRule()

    private val mapFile = File("../../mapas/honduras.map").absoluteFile

    @Test
    @Config(qualifiers = "w1024dp-h600dp-land-mdpi", sdk = [34])
    fun home_with_live_map() {
        assumeTrue("map file missing: $mapFile", mapFile.canRead())
        val state = HomeState(
            cityLabel = "San Pedro Sula",
            // Driving north-east through central San Pedro Sula at 42 km/h.
            fix = GeoFix(lat = 15.5050, lon = -88.0255, bearing = 35f, speedKmh = 42),
            mapFile = mapFile,
        )
        compose.setContent {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val unit = minOf(maxWidth / DESIGN_W, maxHeight / DESIGN_H)
                CompositionLocalProvider(LocalUnit provides unit) {
                    AppBackground()
                    HomeScreen(state, HomeActions({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}))
                }
            }
        }
        repeat(40) {
            Thread.sleep(250)
            ShadowLooper.idleMainLooper()
            compose.mainClock.advanceTimeBy(250)
        }
        compose.onRoot().captureRoboImage("../preview/home_live_map_1024x600.png")
    }
}
