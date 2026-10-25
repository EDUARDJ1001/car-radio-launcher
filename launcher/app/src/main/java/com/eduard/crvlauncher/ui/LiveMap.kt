package com.eduard.crvlauncher.ui

import android.content.Context
import android.os.Environment
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import org.mapsforge.core.model.LatLong
import org.mapsforge.map.android.graphics.AndroidGraphicFactory
import org.mapsforge.map.android.util.AndroidUtil
import org.mapsforge.map.android.view.MapView
import org.mapsforge.map.layer.renderer.TileRendererLayer
import org.mapsforge.map.reader.MapFile
import org.mapsforge.map.rendertheme.XmlRenderTheme
import org.mapsforge.map.rendertheme.XmlRenderThemeMenuCallback
import java.io.File
import java.io.InputStream
import kotlin.math.ln
import kotlin.math.roundToInt

const val MAP_FILE_NAME = "honduras.map"
/** Zoom 16 on a 160 dpi head unit; one level less per doubling of density so the
 *  card shows roughly the same area on denser screens (e.g. a phone). */
private fun zoomFor(density: Float): Byte = (16 - (ln(density.toDouble()) / ln(2.0)).roundToInt()).coerceIn(13, 17).toByte()

/**
 * Where the offline map may live on the unit. Push it with:
 * adb push honduras.map /sdcard/Android/data/com.eduard.crvlauncher/files/
 */
fun findMapFile(context: Context): File? = listOfNotNull(
    context.getExternalFilesDir(null)?.let { File(it, MAP_FILE_NAME) },
    File(Environment.getExternalStorageDirectory(), "Maps/$MAP_FILE_NAME"),
    File(Environment.getExternalStorageDirectory(), MAP_FILE_NAME),
).firstOrNull { it.canRead() }

/** Our navy recolour of mapsforge's default theme; icons resolve from the mapsforge-themes jar. */
private class CrvDarkTheme(private val context: Context) : XmlRenderTheme {
    override fun getMenuCallback(): XmlRenderThemeMenuCallback? = null
    override fun getRelativePathPrefix(): String = "/assets/"
    override fun getRenderThemeAsStream(): InputStream = context.assets.open("crv-dark.xml")
    override fun getResourceProvider() = null
    override fun setMenuCallback(menuCallback: XmlRenderThemeMenuCallback?) = Unit
    override fun setResourceProvider(resourceProvider: org.mapsforge.map.rendertheme.XmlThemeResourceProvider?) = Unit
}

/**
 * Waze-style mini map: offline street map centred on [lat]/[lon], north-up,
 * with a heading arrow on top. Not interactive — the card handles taps.
 */
@Composable
fun LiveMap(
    mapFile: File,
    lat: Double,
    lon: Double,
    bearing: Float?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val mapView = remember {
        AndroidGraphicFactory.createInstance(context.applicationContext as android.app.Application)
        MapView(context).apply {
            isClickable = false
            mapScaleBar.isVisible = false
            setBuiltInZoomControls(false)
            model.displayModel.setUserScaleFactor(1.15f)
            // Navy instead of mapsforge's light grey while tiles are still being drawn.
            model.displayModel.backgroundColor = 0xFF0E1D4A.toInt()
            val cache = AndroidUtil.createTileCache(
                context, "navcard", model.displayModel.tileSize, 1f, model.frameBufferModel.overdrawFactor,
            )
            val layer = TileRendererLayer(
                cache, MapFile(mapFile), model.mapViewPosition, AndroidGraphicFactory.INSTANCE,
            ).apply { setXmlRenderTheme(CrvDarkTheme(context)) }
            layerManager.layers.add(layer)
            setCenter(LatLong(lat, lon))
            setZoomLevel(zoomFor(context.resources.displayMetrics.density))
        }
    }
    DisposableEffect(Unit) { onDispose { mapView.destroyAll() } }
    LaunchedEffect(lat, lon) {
        mapView.model.mapViewPosition.animateTo(LatLong(lat, lon))
    }

    Box(modifier) {
        AndroidView({ mapView }, Modifier.fillMaxSize())
        // Arrow when we know the heading (driving), plain dot when stopped.
        Box(Modifier.align(Alignment.Center)) {
            if (bearing != null) HeadingArrow(64.u, bearing) else PositionDot(72.u)
        }
    }
}

@Composable
private fun HeadingArrow(size: Dp, bearing: Float) {
    Canvas(Modifier.size(size).rotate(bearing)) {
        val w = this.size.width
        val h = this.size.height
        drawCircle(
            Brush.radialGradient(listOf(Palette.Accent.copy(alpha = 0.5f), Color.Transparent)),
            radius = w * 0.75f,
        )
        val arrow = Path().apply {
            moveTo(w * 0.5f, h * 0.08f)
            lineTo(w * 0.86f, h * 0.88f)
            lineTo(w * 0.5f, h * 0.70f)
            lineTo(w * 0.14f, h * 0.88f)
            close()
        }
        drawPath(arrow, Palette.Accent)
        drawPath(arrow, Color.White, style = Stroke(width = w * 0.07f, join = StrokeJoin.Round))
    }
}

@Composable
private fun PositionDot(size: Dp) {
    Canvas(Modifier.size(size)) {
        val r = this.size.minDimension / 2f
        drawCircle(Brush.radialGradient(listOf(Palette.Accent.copy(alpha = 0.5f), Color.Transparent)), radius = r)
        drawCircle(Color.White, radius = r * 0.36f, center = Offset(r, r))
        drawCircle(Palette.Accent, radius = r * 0.24f, center = Offset(r, r))
    }
}
