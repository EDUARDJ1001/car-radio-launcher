package com.eduard.crvlauncher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothDisabled
import androidx.compose.material.icons.rounded.CellTower
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WifiOff
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.eduard.crvlauncher.Config
import com.eduard.crvlauncher.R
import com.eduard.crvlauncher.data.NowPlaying
import com.eduard.crvlauncher.data.Weather
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Actions the home screen can trigger; wired to real apps in MainActivity. */
class HomeActions(
    val openNavigation: () -> Unit = {},
    val openPhone: () -> Unit = {},
    val openMusic: () -> Unit = {},
    /** Tap: switch the card to Bluetooth music in place. Long press: Jancar's full player. */
    val openBluetoothAudio: () -> Unit = {},
    val openBluetoothAudioApp: () -> Unit = {},
    /** Tap: turn the FM radio on in the card. Long press: Jancar's full radio screen. */
    val openRadio: () -> Unit = {},
    val openRadioApp: () -> Unit = {},
    val openAndroidAuto: () -> Unit = {},
    val openCarPlay: () -> Unit = {},
    val openApps: () -> Unit = {},
    val openSettings: () -> Unit = {},
    /** Opens whatever is currently playing (BT music app, Spotify, …). */
    val openNowPlaying: () -> Unit = {},
    val cycleCity: () -> Unit = {},
    val playPause: () -> Unit = {},
    val next: () -> Unit = {},
    val previous: () -> Unit = {},
)

/** FM radio as shown in the media card while it is the active source. */
data class RadioInfo(val freqKhz: Int, val stationName: String?, val muted: Boolean) {
    val freqLabel: String get() = "%.1f MHz".format(Locale.US, freqKhz / 1000f)
}

/** Wi-Fi link as shown in the top bar. */
data class WifiStatus(val bars: Int, val band: String)

data class HomeState(
    val weather: Weather? = null,
    val nowPlaying: NowPlaying? = null,
    /** "Bluetooth Audio", or the playing app's name. */
    val sourceLabel: String? = null,
    val bluetoothOn: Boolean = false,
    /** null when not connected. */
    val wifi: WifiStatus? = null,
    /** "· Bienvenido" next to the greeting; shown for a few minutes after the car starts. */
    val showWelcome: Boolean = true,
    /** Non-null while the FM radio is the active source: the card shows the station instead of music. */
    val radio: RadioInfo? = null,
)

private val spanish = Locale("es", "ES")

@Composable
fun HomeScreen(state: HomeState, actions: HomeActions) {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            // Only h:mm is shown: wake once per minute, on the minute (cheap on the AC8227L).
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
                    .padding(start = 20.u, end = 22.u, top = 24.u, bottom = 68.u),
                horizontalArrangement = Arrangement.spacedBy(18.u),
            ) {
                HeroCard(now, state.weather, state.showWelcome, actions.cycleCity, Modifier.weight(628f).fillMaxHeight())
                Column(
                    Modifier.weight(858f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(14.u),
                ) {
                    MediaCard(state, actions, Modifier.weight(364f).fillMaxWidth())
                    TileRow(Modifier.weight(180f)) {
                        Tile("Navegación", actions.openNavigation) {
                            CircleOutline(92.u, Color.White) {
                                Icon(Icons.Rounded.Navigation, null, tint = Color.White, modifier = Modifier.size(52.u))
                            }
                        }
                        Tile("Teléfono", actions.openPhone) {
                            Icon(Icons.Rounded.Phone, null, tint = Palette.PhoneGreen, modifier = Modifier.size(92.u))
                        }
                        Tile("Radio FM", actions.openRadio, onLongClick = actions.openRadioApp, highlighted = state.radio != null) {
                            Icon(Icons.Rounded.CellTower, null, tint = Palette.Accent, modifier = Modifier.size(92.u))
                        }
                        Tile("Bluetooth Audio", actions.openBluetoothAudio, onLongClick = actions.openBluetoothAudioApp) {
                            Box(
                                Modifier.size(80.u).clip(CircleShape).background(Palette.Accent),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Rounded.Bluetooth, null, tint = Color.White, modifier = Modifier.size(54.u))
                            }
                        }
                    }
                    TileRow(Modifier.weight(186f)) {
                        Tile("Apple CarPlay", actions.openCarPlay) { CarPlayMark(90.u) }
                        Tile("Android Auto", actions.openAndroidAuto) { AutoMark(90.u) }
                        Tile("Ajustes", actions.openSettings) {
                            Icon(Icons.Rounded.Settings, null, tint = Palette.Gear, modifier = Modifier.size(96.u))
                        }
                        Tile("Todas las Apps", actions.openApps) {
                            CircleOutline(88.u, Color.White) {
                                Icon(Icons.Rounded.GridView, null, tint = Color.White, modifier = Modifier.size(50.u))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- side rail

@Composable
private fun SideRail(actions: HomeActions) {
    Column(
        Modifier
            .width(124.u)
            .fillMaxHeight()
            .background(Palette.Rail)
            .padding(top = 120.u, bottom = 70.u),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        RailItem(Icons.Rounded.Home, "Inicio", selected = true) {}
        RailItem(Icons.Rounded.MusicNote, "Audio", onClick = actions.openMusic)
        RailItem(Icons.Rounded.Phone, "Teléfono", onClick = actions.openPhone)
        RailItem(Icons.Rounded.Navigation, "Navegación", onClick = actions.openNavigation)
        RailItem(Icons.Rounded.Apps, "Apps", onClick = actions.openApps)
    }
}

@Composable
private fun RailItem(icon: ImageVector, label: String, selected: Boolean = false, onClick: () -> Unit) {
    val tint = if (selected) Palette.Accent else Color.White
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(top = 12.u),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(52.u))
        Spacer(Modifier.height(8.u))
        Label(label, 22, color = tint)
        Spacer(Modifier.height(14.u))
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.u)
                .background(if (selected) Palette.Accent else Color.Transparent, RoundedCornerShape(2.u)),
        )
    }
}

// ---------------------------------------------------------------- top bar

@Composable
private fun TopBar(now: Date, state: HomeState) {
    val minute = now.time / 60000
    val time = remember(minute) { SimpleDateFormat("h:mm", spanish).format(now) }
    val amPm = remember(minute) { SimpleDateFormat("a", Locale.US).format(now) }
    val date = remember(minute) {
        SimpleDateFormat("EEE, d 'de' MMMM", spanish).format(now)
            .replace(".", "")
            .replaceFirstChar { it.titlecase(spanish) }
    }
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .height(94.u)
                .padding(start = 20.u, end = 36.u),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Label(time, 60, weight = FontWeight.Medium)
                Spacer(Modifier.width(8.u))
                Label(amPm, 32, modifier = Modifier.padding(bottom = 6.u))
            }
            Spacer(Modifier.width(40.u))
            VDivider()
            Spacer(Modifier.width(36.u))
            Label(date, 30, color = Color(0xFFD5DEF5))
            Spacer(Modifier.weight(1f))
            val wifi = state.wifi
            if (wifi != null) {
                SignalBars(wifi.bars, Modifier.size(40.u))
                Spacer(Modifier.width(20.u))
                Label(wifi.band, 30)
            } else {
                Icon(Icons.Rounded.WifiOff, null, tint = Palette.TextSecondary, modifier = Modifier.size(40.u))
            }
            Spacer(Modifier.width(26.u))
            Icon(
                if (state.bluetoothOn) Icons.Rounded.Bluetooth else Icons.Rounded.BluetoothDisabled, null,
                tint = if (state.bluetoothOn) Palette.Accent else Palette.TextSecondary,
                modifier = Modifier.size(44.u),
            )
            Spacer(Modifier.width(34.u))
            VDivider()
            Spacer(Modifier.width(30.u))
            Icon(Icons.Rounded.DirectionsCar, null, tint = Color.White, modifier = Modifier.size(58.u))
            Spacer(Modifier.width(22.u))
            Column {
                Label("Exterior", 22, color = Color(0xFFD5DEF5))
                Label(state.weather?.let { "${it.temp}°C" } ?: "--°C", 30)
            }
            Spacer(Modifier.width(24.u))
            WeatherIcon(state.weather?.code, state.weather?.isDay ?: true, 54.u)
        }
        Box(Modifier.fillMaxWidth().height(1.5.u).background(Color(0x553A5CA8)))
    }
}

/** Four rising bars, [bars] of them lit — the Wi-Fi strength readout of the design. */
@Composable
private fun SignalBars(bars: Int, modifier: Modifier) {
    Canvas(modifier) {
        val gap = size.width * 0.08f
        val w = (size.width - gap * 3) / 4f
        for (i in 0 until 4) {
            val h = size.height * (0.4f + 0.2f * i)
            drawRoundRect(
                if (i < bars) Color.White else Color.White.copy(alpha = 0.25f),
                topLeft = Offset(i * (w + gap), size.height - h),
                size = Size(w, h),
                cornerRadius = CornerRadius(w * 0.3f),
            )
        }
    }
}

@Composable
private fun VDivider() {
    Box(Modifier.width(1.5.u).height(50.u).background(Color(0x553A5590)))
}

// ---------------------------------------------------------------- greeting + weather

@Composable
private fun HeroCard(now: Date, weather: Weather?, showWelcome: Boolean, onCycleCity: () -> Unit, modifier: Modifier) {
    val hour = remember(now.time / 60000) { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = when (hour) {
        in 5..11 -> "Buenos días"
        in 12..18 -> "Buenas tardes"
        else -> "Buenas noches"
    }
    GlassCard(Palette.CardTop, Palette.CardBottom, Palette.CardBorder, modifier) {
        // Blue glow under the car, like a showroom floor light.
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width * 0.55f, size.height * 0.62f)
            drawCircle(
                Brush.radialGradient(listOf(Color(0xFF1E5BD8).copy(alpha = 0.45f), Color.Transparent), c, size.width * 0.6f),
                radius = size.width * 0.6f, center = c,
            )
        }
        Column(Modifier.fillMaxSize().padding(start = 34.u, end = 22.u, top = 36.u, bottom = 20.u)) {
            Label("Hola, ${Config.DRIVER_NAME}", 58, weight = FontWeight.Bold)
            Spacer(Modifier.height(6.u))
            Row {
                Label(greeting, 32, color = Palette.TextSecondary)
                AnimatedVisibility(showWelcome, enter = fadeIn(), exit = fadeOut(tween(1500))) {
                    Label(" · Bienvenido", 32, color = Palette.TextSecondary)
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Image(
                    painterResource(R.drawable.crv), null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(vertical = 6.u),
                )
            }
            // Tap the weather to cycle Automático → configured cities.
            Column(Modifier.clip(RoundedCornerShape(18.u)).clickable(onClick = onCycleCity)) {
                WeatherRow(weather)
                Spacer(Modifier.height(18.u))
                WeatherStats(weather)
            }
        }
    }
}

@Composable
private fun WeatherRow(weather: Weather?) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 6.u)) {
        WeatherIcon(weather?.code, weather?.isDay ?: true, 96.u)
        Spacer(Modifier.width(34.u))
        Label(weather?.let { "${it.temp}°" } ?: "--°", 92, weight = FontWeight.SemiBold)
        Spacer(Modifier.width(26.u))
        Column {
            Label(weather?.description ?: "Sin datos de clima", 32)
            Spacer(Modifier.height(4.u))
            Label(weather?.let { "Sensación ${it.feelsLike}°" } ?: "Conecta a internet", 28, color = Palette.TextSecondary)
        }
    }
}

@Composable
private fun WeatherStats(weather: Weather?) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(74.u)
            .clip(RoundedCornerShape(37.u))
            .background(Color(0x66091A44))
            .border(1.u, Color(0x332F5BB0), RoundedCornerShape(37.u))
            .padding(horizontal = 24.u),
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
        Icon(icon, null, tint = if (icon == Icons.Rounded.WaterDrop) Palette.Accent else Color.White, modifier = Modifier.size(32.u))
        Spacer(Modifier.width(8.u))
        Label(value, 28)
    }
}

@Composable
private fun PillDivider() {
    Box(Modifier.width(1.5.u).height(36.u).background(Color(0x553A5590)))
}

/** Material has no "rain cloud", so drizzle/rain draws a cloud with blue falling streaks. */
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
                    Palette.Accent,
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

// ---------------------------------------------------------------- media

@Composable
private fun MediaCard(state: HomeState, actions: HomeActions, modifier: Modifier) {
    val np = state.nowPlaying
    val radio = state.radio
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(np?.playing, radio == null) {
        while (radio == null && np?.playing == true) {
            tick++
            delay(1000)
        }
    }
    GlassCard(
        Palette.CardTop, Palette.CardBottom, Palette.CardBorder, modifier,
        onClick = if (radio != null) actions.openRadioApp else actions.openNowPlaying,
    ) {
        Row(Modifier.fillMaxSize().padding(start = 34.u, end = 38.u, top = 36.u, bottom = 36.u)) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f, matchHeightConstraintsFirst = true)
                    .clip(RoundedCornerShape(16.u))
                    .background(Brush.verticalGradient(listOf(Color(0xFF26407E), Color(0xFF101C3C)))),
                contentAlignment = Alignment.Center,
            ) {
                val art = np?.art
                when {
                    radio != null -> Icon(Icons.Rounded.CellTower, null, tint = Palette.Accent, modifier = Modifier.size(110.u))
                    art != null -> Image(art.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    else -> Icon(Icons.Rounded.MusicNote, null, tint = Color(0x88FFFFFF), modifier = Modifier.size(90.u))
                }
            }
            Spacer(Modifier.width(40.u))
            Column(Modifier.weight(1f).fillMaxHeight()) {
                val bt = np == null || np.sourcePackage in Config.BLUETOOTH_MEDIA_SOURCES
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        when {
                            radio != null -> Icons.Rounded.CellTower
                            bt -> Icons.Rounded.Bluetooth
                            else -> Icons.Rounded.MusicNote
                        },
                        null, tint = Palette.Accent, modifier = Modifier.size(34.u),
                    )
                    Spacer(Modifier.width(12.u))
                    Label(if (radio != null) "Radio FM" else state.sourceLabel ?: "Bluetooth Audio", 26, color = Palette.Accent)
                }
                Spacer(Modifier.height(8.u))
                if (radio != null) {
                    // RDS name as the title when the station sends one, otherwise the frequency.
                    Label(radio.stationName ?: radio.freqLabel, 42, weight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.u))
                    val sub = when {
                        radio.muted -> "Silenciado"
                        radio.stationName != null -> radio.freqLabel
                        else -> "FM"
                    }
                    Label(sub, 30, color = Palette.TextSecondary)
                    Spacer(Modifier.weight(1f))
                    FrequencyDial(radio.freqKhz)
                } else {
                    Label(np?.title?.ifBlank { null } ?: "Sin música", 42, weight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.u))
                    Label(np?.artist?.ifBlank { null } ?: "Toca para abrir", 30, color = Palette.TextSecondary)
                    Spacer(Modifier.weight(1f))
                    Progress(np, tick)
                }
                Spacer(Modifier.height(10.u))
                // On the radio, "pause" means mute: the tuner keeps running.
                val playing = if (radio != null) !radio.muted else np?.playing == true
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RoundButton(Icons.Rounded.SkipPrevious, 84, actions.previous)
                    Spacer(Modifier.width(50.u))
                    RoundButton(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, 112, actions.playPause)
                    Spacer(Modifier.width(50.u))
                    RoundButton(Icons.Rounded.SkipNext, 84, actions.next)
                }
            }
        }
    }
}

/** FM band 87.5–108 MHz with the tuned position marked: the radio's stand-in for a progress bar. */
@Composable
private fun FrequencyDial(freqKhz: Int) {
    val fraction = ((freqKhz - 87_500) / 20_500f).coerceIn(0f, 1f)
    Box(Modifier.fillMaxWidth().height(26.u), contentAlignment = Alignment.CenterStart) {
        Canvas(Modifier.fillMaxWidth().height(18.u)) {
            val ticks = 41
            for (i in 0 until ticks) {
                val x = size.width * i / (ticks - 1)
                val major = i % 4 == 0
                drawLine(
                    Palette.ProgressTrack,
                    Offset(x, if (major) 0f else size.height * 0.4f), Offset(x, size.height),
                    strokeWidth = if (major) 3f else 2f,
                )
            }
        }
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.fillMaxWidth(fraction))
            Box(Modifier.offset(x = (-3).u).width(6.u).height(26.u).clip(RoundedCornerShape(3.u)).background(Palette.Accent))
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 8.u)) {
        Label("87.5", 26, color = Palette.TextSecondary)
        Spacer(Modifier.weight(1f))
        Label("108", 26, color = Palette.TextSecondary)
    }
}

@Composable
private fun Progress(np: NowPlaying?, @Suppress("UNUSED_PARAMETER") tick: Long) {
    val pos = np?.currentPosition() ?: 0L
    val dur = np?.durationMs ?: 0L
    val fraction = if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f
    Box(Modifier.fillMaxWidth().height(26.u), contentAlignment = Alignment.CenterStart) {
        Box(Modifier.fillMaxWidth().height(7.u).clip(RoundedCornerShape(4.u)).background(Palette.ProgressTrack))
        Box(Modifier.fillMaxWidth(fraction).height(7.u).clip(RoundedCornerShape(4.u)).background(Palette.Accent))
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.fillMaxWidth(fraction))
            Box(Modifier.offset(x = (-13).u).size(26.u).clip(CircleShape).background(Color.White))
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 8.u)) {
        Label(formatTime(pos), 26, color = Palette.TextSecondary)
        Spacer(Modifier.weight(1f))
        Label(formatTime(dur), 26, color = Palette.TextSecondary)
    }
}

private fun formatTime(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}

/** Dark round control with a thin ring — the play/pause one is just bigger. */
@Composable
private fun RoundButton(icon: ImageVector, size: Int, onClick: () -> Unit) {
    Box(
        Modifier
            .size(size.u)
            .clip(CircleShape)
            .background(Palette.ControlBg)
            .border(2.u, Palette.ControlBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size((size * 0.52f).u))
    }
}

// ---------------------------------------------------------------- shortcut tiles

@Composable
private fun ColumnScope.TileRow(modifier: Modifier, content: @Composable RowScope.() -> Unit) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.u), content = content)
}

@Composable
private fun RowScope.Tile(
    label: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    highlighted: Boolean = false,
    icon: @Composable () -> Unit,
) {
    GlassCard(
        if (highlighted) Palette.TileActiveTop else Palette.TileTop,
        if (highlighted) Palette.TileActiveBottom else Palette.TileBottom,
        if (highlighted) Palette.Accent else Palette.TileBorder,
        Modifier.weight(1f).fillMaxHeight(),
        radius = 18.u,
        onClick = onClick,
        onLongClick = onLongClick,
    ) {
        Column(
            Modifier.fillMaxSize().padding(top = 18.u, bottom = 18.u),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { icon() }
            Spacer(Modifier.height(8.u))
            Label(label, 26)
        }
    }
}
