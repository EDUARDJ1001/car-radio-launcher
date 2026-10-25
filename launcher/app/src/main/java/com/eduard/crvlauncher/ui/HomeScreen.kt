package com.eduard.crvlauncher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.CellTower
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.eduard.crvlauncher.Config
import com.eduard.crvlauncher.R
import com.eduard.crvlauncher.data.GeoFix
import com.eduard.crvlauncher.data.NowPlaying
import com.eduard.crvlauncher.data.Weather
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Actions the home screen can trigger; wired to real apps in MainActivity. */
class HomeActions(
    val openNavigation: () -> Unit,
    val openPhone: () -> Unit,
    val openMusic: () -> Unit,
    val openRadio: () -> Unit,
    val openAndroidAuto: () -> Unit,
    val openCarPlay: () -> Unit,
    val openApps: () -> Unit,
    val openSettings: () -> Unit,
    val cycleCity: () -> Unit,
    val playPause: () -> Unit,
    val next: () -> Unit,
    val previous: () -> Unit,
)

data class HomeState(
    val weather: Weather? = null,
    val nowPlaying: NowPlaying? = null,
    val bluetoothOn: Boolean = false,
    val radioLabel: String = "Sintonizar",
    /** Selected weather location as shown to the user ("Automático", "Tegucigalpa", …). */
    val cityLabel: String = "Automático",
    /** Live GPS fix, null until the unit has one. */
    val fix: GeoFix? = null,
    /** Offline street map on the unit, null if it hasn't been copied yet. */
    val mapFile: File? = null,
)

private val spanish = Locale("es", "ES")

@Composable
fun HomeScreen(state: HomeState, actions: HomeActions) {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            // Only HH:mm is shown: wake once per minute, on the minute (cheap on the AC8227L).
            delay(60_000L - System.currentTimeMillis() % 60_000L)
        }
    }

    Row(Modifier.fillMaxSize()) {
        SideRail(actions)
        Column(Modifier.fillMaxSize()) {
            TopBar(now, state)
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(start = 36.u, end = 30.u, top = 22.u, bottom = 70.u),
                horizontalArrangement = Arrangement.spacedBy(25.u),
            ) {
                HeroCard(now, state, actions.cycleCity, Modifier.weight(614f).fillMaxHeight())
                Column(
                    Modifier.weight(458f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(22.u),
                ) {
                    NavigationCard(state, actions.openNavigation, Modifier.weight(295f).fillMaxWidth())
                    MediaCard(state.nowPlaying, actions, Modifier.weight(376f).fillMaxWidth())
                }
                Column(
                    Modifier.weight(359f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(18.u),
                ) {
                    ShortcutTile("Teléfono", "Conectar dispositivo", actions.openPhone) {
                        Icon(Icons.Rounded.Phone, null, tint = Palette.PhoneGreen, modifier = Modifier.size(84.u))
                    }
                    ShortcutTile("FM Radio", state.radioLabel, actions.openRadio) {
                        Icon(Icons.Rounded.CellTower, null, tint = Palette.Accent, modifier = Modifier.size(84.u))
                    }
                    ShortcutTile("Android Auto", "Conectar", actions.openAndroidAuto) { AutoMark(84.u) }
                    ShortcutTile("Apple CarPlay", "Conectar", actions.openCarPlay) { CarPlayMark(88.u) }
                }
            }
        }
    }
}

@Composable
private fun SideRail(actions: HomeActions) {
    Column(
        Modifier
            .width(128.u)
            .fillMaxHeight()
            .background(Palette.Rail)
            .padding(top = 135.u, bottom = 110.u),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        RailItem(Icons.Rounded.Home, selected = true) {}
        RailItem(Icons.Rounded.Navigation, onClick = actions.openNavigation)
        RailItem(Icons.Rounded.Phone, onClick = actions.openPhone)
        RailItem(Icons.Rounded.MusicNote, onClick = actions.openMusic)
        RailItem(Icons.Rounded.Apps, onClick = actions.openApps)
        RailItem(Icons.Rounded.Settings, onClick = actions.openSettings)
    }
}

@Composable
private fun RailItem(icon: ImageVector, selected: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(96.u)
            .background(if (selected) Palette.RailSelected else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .width(5.u)
                    .height(76.u)
                    .background(Palette.Accent, RoundedCornerShape(3.u))
            )
        }
        Icon(
            icon, null,
            tint = if (selected) Palette.Accent else Color(0xFFDDE6FF),
            modifier = Modifier.size(48.u),
        )
    }
}

@Composable
private fun TopBar(now: Date, state: HomeState) {
    val time = remember(now.time / 60000) { SimpleDateFormat("HH:mm", spanish).format(now) }
    val date = remember(now.time / 60000) {
        SimpleDateFormat("EEE, d 'de' MMMM", spanish).format(now)
            .replace(".", "")
            .replaceFirstChar { it.titlecase(spanish) }
    }
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .height(112.u)
                .padding(start = 36.u, end = 72.u),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Label(time, 52, weight = FontWeight.Medium)
            Spacer(Modifier.width(28.u))
            Label(date, 26, color = Color(0xFFD5DEF5))
            Spacer(Modifier.width(42.u))
            VDivider()
            Spacer(Modifier.weight(1f))
            Icon(Icons.Rounded.LocationOn, null, tint = Color.White, modifier = Modifier.size(38.u))
            Spacer(Modifier.width(14.u))
            Icon(
                if (state.bluetoothOn) Icons.Rounded.Bluetooth else Icons.Rounded.BluetoothDisabled, null,
                tint = if (state.bluetoothOn) Color.White else Palette.TextSecondary,
                modifier = Modifier.size(38.u),
            )
            Spacer(Modifier.width(14.u))
            Icon(Icons.Rounded.Phone, null, tint = Color.White, modifier = Modifier.size(38.u))
            Spacer(Modifier.width(34.u))
            VDivider()
            Spacer(Modifier.width(34.u))
            Icon(Icons.Rounded.DirectionsCar, null, tint = Color.White, modifier = Modifier.size(50.u))
            Spacer(Modifier.width(16.u))
            Column {
                Label("Exterior", 20, color = Color(0xFFD5DEF5))
                Label(state.weather?.let { "${it.temp}°C" } ?: "--°C", 28)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.u).background(Palette.Divider))
    }
}

@Composable
private fun VDivider() {
    Box(Modifier.width(1.5.u).height(44.u).background(Color(0x553A5590)))
}

@Composable
private fun HeroCard(now: Date, state: HomeState, onCycleCity: () -> Unit, modifier: Modifier) {
    val hour = remember(now.time / 60000) { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = when (hour) {
        in 5..11 -> "Buenos días"
        in 12..18 -> "Buenas tardes"
        else -> "Buenas noches"
    }
    GlassCard(Palette.HeroTop, Palette.HeroBottom, Palette.HeroBorder, modifier) {
        Column(Modifier.fillMaxSize().padding(start = 40.u, end = 28.u, top = 36.u, bottom = 24.u)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Label("Hola, ${Config.DRIVER_NAME}", 48, weight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.u))
                    Label("$greeting · Bienvenido", 26, color = Palette.TextSecondary)
                }
                CircleOutline(88.u, Color(0x88B8CCFF)) {
                    Icon(Icons.Rounded.DirectionsCar, null, tint = Color.White, modifier = Modifier.size(46.u))
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Image(
                    painterResource(R.drawable.crv), null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().offset(y = 6.u),
                )
            }
            WeatherBlock(state, onCycleCity)
            WeatherStats(state.weather)
        }
    }
}

@Composable
private fun WeatherBlock(state: HomeState, onCycleCity: () -> Unit) {
    val weather = state.weather
    val place = when {
        weather?.place == "GPS" -> "Ubicación actual"
        state.cityLabel == "Automático" -> weather?.place ?: "Automático"
        else -> state.cityLabel
    }
    Column(
        Modifier
            .clip(RoundedCornerShape(18.u))
            .clickable(onClick = onCycleCity)
            .padding(start = 6.u, top = 4.u, bottom = 4.u),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.LocationOn, null, tint = Palette.TextSecondary, modifier = Modifier.size(24.u))
            Spacer(Modifier.width(6.u))
            Label(place, 21, color = Palette.TextSecondary)
            Spacer(Modifier.width(10.u))
            Icon(Icons.Rounded.SwapHoriz, null, tint = Color(0x88A9BCE8), modifier = Modifier.size(24.u))
        }
        Spacer(Modifier.height(4.u))
        WeatherRow(weather)
    }
}

@Composable
private fun WeatherRow(weather: Weather?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        WeatherIcon(weather?.code, weather?.isDay ?: true, 76.u)
        Spacer(Modifier.width(30.u))
        Label(weather?.let { "${it.temp}°" } ?: "--°", 72, weight = FontWeight.SemiBold)
        Spacer(Modifier.width(28.u))
        Column {
            Label(weather?.description ?: "Sin datos de clima", 26)
            Label(weather?.let { "Sensación ${it.feelsLike}°" } ?: "Conecta a internet", 22, color = Palette.TextSecondary)
        }
    }
}

@Composable
private fun WeatherStats(weather: Weather?) {
    Spacer(Modifier.height(16.u))
    Row(
        Modifier
            .fillMaxWidth()
            .height(68.u)
            .clip(RoundedCornerShape(34.u))
            .background(Color(0x66081A52))
            .padding(horizontal = 32.u),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Stat(Icons.Rounded.ArrowUpward, weather?.let { "${it.max}°" } ?: "--")
        PillDivider()
        Stat(Icons.Rounded.ArrowDownward, weather?.let { "${it.min}°" } ?: "--")
        PillDivider()
        Stat(Icons.Rounded.WaterDrop, weather?.let { "${it.humidity}%" } ?: "--")
        PillDivider()
        Stat(Icons.Rounded.Air, weather?.let { "${it.windKmh} km/h" } ?: "--")
    }
}

@Composable
private fun Stat(icon: ImageVector, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(28.u))
        Spacer(Modifier.width(8.u))
        Label(value, 26)
    }
}

@Composable
private fun PillDivider() {
    Box(Modifier.width(1.5.u).height(32.u).background(Color(0x553A5590)))
}

/** Material has no "rain cloud", so drizzle/rain draws a cloud with falling streaks. */
@Composable
private fun WeatherIcon(code: Int?, isDay: Boolean, size: Dp) {
    val rainy = code != null && (code in 51..67 || code in 80..82)
    if (!rainy) {
        Icon(weatherIcon(code, isDay), null, tint = Color.White, modifier = Modifier.size(size))
        return
    }
    Box(Modifier.size(size)) {
        Icon(
            Icons.Rounded.Cloud, null, tint = Color.White,
            modifier = Modifier.size(size * 0.92f).align(Alignment.TopCenter).offset(y = -size * 0.12f),
        )
        Canvas(Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            for (x in listOf(0.30f, 0.50f, 0.70f)) {
                drawLine(
                    Color.White,
                    Offset(w * x, h * 0.70f), Offset(w * (x - 0.06f), h * 0.92f),
                    strokeWidth = w * 0.07f, cap = StrokeCap.Round,
                )
            }
        }
    }
}

private fun weatherIcon(code: Int?, isDay: Boolean): ImageVector = when (code) {
    null -> Icons.Rounded.WbCloudy
    0, 1 -> if (isDay) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay
    2, 3, 45, 48 -> Icons.Rounded.Cloud
    in 71..77, 85, 86 -> Icons.Rounded.AcUnit
    in 95..99 -> Icons.Rounded.FlashOn
    else -> Icons.Rounded.WbCloudy
}

@Composable
private fun NavigationCard(state: HomeState, onClick: () -> Unit, modifier: Modifier) {
    // Follow the live GPS fix; without one, show the chosen (or default) city.
    val fix = state.fix
    val city = Config.CITIES.firstOrNull { it.name == state.cityLabel } ?: Config.CITIES.first()
    GlassCard(Palette.NavTop, Palette.NavBottom, Palette.NavBorder, modifier, onClick = onClick) {
        val mapFile = state.mapFile
        if (mapFile != null) {
            LiveMap(
                mapFile = mapFile,
                lat = fix?.lat ?: city.lat,
                lon = fix?.lon ?: city.lon,
                bearing = fix?.bearing,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Column(Modifier.align(Alignment.TopStart).padding(start = 30.u, top = 30.u, end = 30.u)) {
                Label("Mapa offline no instalado", 21, color = Palette.TextSecondary)
                Label("Copia $MAP_FILE_NAME al radio", 18, color = Palette.TextSecondary.copy(alpha = 0.7f))
            }
        }
        // Card is just a preview: this transparent layer takes every touch and opens the nav app.
        Box(Modifier.fillMaxSize().clickable(onClick = onClick))
        val speed = fix?.speedKmh
        if (speed != null) {
            Row(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 18.u, end = 18.u)
                    .clip(RoundedCornerShape(16.u))
                    .background(Color(0xE60B1E55))
                    .padding(horizontal = 16.u, vertical = 8.u),
                verticalAlignment = Alignment.Bottom,
            ) {
                Label("$speed", 34, weight = FontWeight.SemiBold)
                Spacer(Modifier.width(6.u))
                Label("km/h", 18, color = Palette.TextSecondary, modifier = Modifier.padding(bottom = 4.u))
            }
        }
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(110.u)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Palette.NavBottom.copy(alpha = 0.92f)))),
        )
        Row(
            Modifier.align(Alignment.BottomStart).padding(start = 30.u, bottom = 26.u),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(66.u).clip(CircleShape).background(Color(0xCC0B1E55))) {
                CircleOutline(66.u, Color(0xFFDCE7FF)) {
                    Icon(Icons.Rounded.Navigation, null, tint = Color.White, modifier = Modifier.size(36.u))
                }
            }
            Spacer(Modifier.width(18.u))
            Column {
                Label("Navegación", 26)
                Spacer(Modifier.height(4.u))
                Label("Ir a destino", 21, color = Palette.TextSecondary)
            }
        }
    }
}

@Composable
private fun MediaCard(np: NowPlaying?, actions: HomeActions, modifier: Modifier) {
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(np?.playing) {
        while (np?.playing == true) {
            tick++
            delay(500)
        }
    }
    GlassCard(
        Palette.MediaTop, Palette.MediaBottom, Palette.MediaBorder, modifier,
        onClick = actions.openMusic,
    ) {
        Column(Modifier.fillMaxSize().padding(start = 36.u, end = 36.u, top = 34.u, bottom = 18.u)) {
            Row(Modifier.weight(1f)) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .aspectRatio(1f, matchHeightConstraintsFirst = true)
                        .clip(RoundedCornerShape(12.u))
                        .background(Brush.verticalGradient(listOf(Color(0xFF5B49E8), Color(0xFF1E1670)))),
                    contentAlignment = Alignment.Center,
                ) {
                    val art = np?.art
                    if (art != null) {
                        Image(art.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Icon(Icons.Rounded.MusicNote, null, tint = Color(0x99FFFFFF), modifier = Modifier.size(64.u))
                    }
                }
                Spacer(Modifier.width(26.u))
                Column(Modifier.weight(1f).padding(top = 40.u)) {
                    Label(np?.title?.ifBlank { null } ?: "Sin música", 26)
                    Spacer(Modifier.height(8.u))
                    Label(np?.artist?.ifBlank { null } ?: "Toca para abrir", 21, color = Color(0xFFCFC8FF))
                }
                if (np != null) {
                    // Bluetooth glyph only when the phone streams over A2DP; app sources get a note.
                    val bt = np.sourcePackage in Config.BLUETOOTH_MEDIA_SOURCES
                    Icon(
                        if (bt) Icons.Rounded.Bluetooth else Icons.Rounded.MusicNote, null,
                        tint = Color.White, modifier = Modifier.size(36.u),
                    )
                }
            }
            Spacer(Modifier.height(22.u))
            Progress(np, tick)
            Spacer(Modifier.height(8.u))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundButton(Icons.Rounded.SkipPrevious, 72, Color(0x33FFFFFF), Color.White, actions.previous)
                RoundButton(
                    if (np?.playing == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    92, Color.White, Color(0xFF2B1F9A), actions.playPause,
                )
                RoundButton(Icons.Rounded.SkipNext, 72, Color(0x33FFFFFF), Color.White, actions.next)
            }
        }
    }
}

@Composable
private fun Progress(np: NowPlaying?, @Suppress("UNUSED_PARAMETER") tick: Long) {
    val pos = np?.currentPosition() ?: 0L
    val dur = np?.durationMs ?: 0L
    val fraction = if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f
    Box(Modifier.fillMaxWidth().height(22.u), contentAlignment = Alignment.CenterStart) {
        Box(Modifier.fillMaxWidth().height(6.u).clip(RoundedCornerShape(3.u)).background(Color(0x55FFFFFF)))
        Box(Modifier.fillMaxWidth(fraction).height(6.u).clip(RoundedCornerShape(3.u)).background(Color.White))
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.fillMaxWidth(fraction))
            Box(Modifier.offset(x = (-11).u).size(22.u).clip(CircleShape).background(Color.White))
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 6.u)) {
        Label(formatTime(pos), 21)
        Spacer(Modifier.weight(1f))
        Label(formatTime(dur), 21)
    }
}

private fun formatTime(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}

@Composable
private fun RoundButton(icon: ImageVector, size: Int, bg: Color, fg: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(size.u).clip(CircleShape).background(bg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size((size * 0.5f).u))
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.ShortcutTile(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    GlassCard(
        Palette.TileTop, Palette.TileBottom, Palette.TileBorder,
        Modifier.weight(1f).fillMaxWidth(), radius = 20.u, onClick = onClick,
    ) {
        Row(
            Modifier.fillMaxSize().padding(start = 34.u, end = 20.u),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(96.u), contentAlignment = Alignment.Center) { icon() }
            Spacer(Modifier.width(26.u))
            TileText(title, subtitle)
        }
    }
}

@Composable
private fun RowScope.TileText(title: String, subtitle: String) {
    Column(Modifier.weight(1f)) {
        Label(title, 26)
        Spacer(Modifier.height(8.u))
        Label(subtitle, 20, color = Palette.TextSecondary)
    }
}
