package com.eduard.crvlauncher

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eduard.crvlauncher.data.AppEntry
import com.eduard.crvlauncher.data.Apps
import com.eduard.crvlauncher.data.JancarBluetooth
import com.eduard.crvlauncher.data.JancarKey
import com.eduard.crvlauncher.data.JancarMedia
import com.eduard.crvlauncher.data.JancarRadio
import com.eduard.crvlauncher.data.MediaRepository
import com.eduard.crvlauncher.data.Weather
import com.eduard.crvlauncher.data.WeatherRepository
import com.eduard.crvlauncher.ui.AppBackground
import com.eduard.crvlauncher.ui.AppDrawer
import com.eduard.crvlauncher.ui.DESIGN_H
import com.eduard.crvlauncher.ui.DESIGN_W
import com.eduard.crvlauncher.ui.HomeActions
import com.eduard.crvlauncher.ui.HomeScreen
import com.eduard.crvlauncher.ui.HomeState
import com.eduard.crvlauncher.ui.LocalUnit
import com.eduard.crvlauncher.ui.Notice
import com.eduard.crvlauncher.ui.RadioInfo
import com.eduard.crvlauncher.ui.NoticePill
import com.eduard.crvlauncher.ui.WifiStatus
import kotlinx.coroutines.delay

/** How long Jancar's radio screen must be on top before its audio is running. */
private const val RADIO_WAKE_MS = 1200L

class MainActivity : ComponentActivity() {

    private lateinit var media: MediaRepository

    // Jancar firmware services: FM tuner, audio-source arbiter, and its Bluetooth stack.
    private lateinit var jRadio: JancarRadio
    private lateinit var jMedia: JancarMedia
    private lateinit var jBt: JancarBluetooth

    /** Audio source as the firmware reports it (JancarMedia.Type), polled while visible. */
    private var audioSource by mutableIntStateOf(JancarMedia.Type.NONE)
    private var onScreen by mutableStateOf(false)
    private var weather by mutableStateOf<Weather?>(null)
    private var bluetoothOn by mutableStateOf(false)
    private var wifi by mutableStateOf<WifiStatus?>(null)
    private var drawerApps by mutableStateOf<List<AppEntry>?>(null)

    /** Bumped on every "car start" (app launch or screen waking from quick-boot sleep). */
    private var welcomeEpoch by mutableIntStateOf(0)
    private var showWelcome by mutableStateOf(true)
    private var notice by mutableStateOf<Notice?>(null)

    /** -1 = automatic (GPS), otherwise index into [Config.CITIES]. */
    private var cityIndex by mutableIntStateOf(-1)
    private val prefs by lazy { getSharedPreferences("launcher", MODE_PRIVATE) }

    private val btReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = refreshBluetooth()
    }

    // The unit sleeps instead of powering off, so ignition = screen on, not a fresh launch.
    private val screenOnReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            welcomeEpoch++
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        media = MediaRepository(applicationContext)
        jRadio = JancarRadio(applicationContext).apply { connect(); onKey = ::onRadioKey }
        jMedia = JancarMedia(applicationContext).apply { connect(); onKey = ::onBluetoothSourceKey }
        jBt = JancarBluetooth(applicationContext).apply { connect() }
        registerReceiver(screenOnReceiver, IntentFilter(Intent.ACTION_SCREEN_ON))
        cityIndex = prefs.getInt("city", -1).coerceIn(-1, Config.CITIES.lastIndex)
        // Ask only for what's missing: re-requesting granted location on Android 10+
        // pops the "allow all the time?" upgrade dialog every launch.
        val missing = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ).filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) requestPermissions(missing.toTypedArray(), 1)

        val actions = HomeActions(
            openNavigation = {
                // Maps is useless offline on this unit: say so quietly instead of opening it.
                if (hasInternet()) open(Config.NAVIGATION, "Navegación")
                else notice = Notice("Sin conexión a internet", Icons.Rounded.CloudOff)
            },
            openPhone = { open(Config.PHONE, "Teléfono") },
            openMusic = { open(Config.MUSIC, "Audio") },
            openBluetoothAudio = { switchToBluetooth() },
            openBluetoothAudioApp = { openBluetoothAudio() },
            openRadio = { switchToRadio() },
            openRadioApp = { open(Config.RADIO, "Radio FM") },
            openAndroidAuto = { open(Config.ANDROID_AUTO, "Android Auto") },
            openCarPlay = { open(Config.CARPLAY, "CarPlay") },
            openApps = { drawerApps = Apps.installedLaunchables(this) },
            openSettings = { if (!Apps.launchAny(this, Config.SETTINGS)) Apps.openSettings(this) },
            openNowPlaying = {
                val source = media.state.value?.sourcePackage
                when {
                    source == null -> open(Config.MUSIC, "Audio")
                    source in Config.BLUETOOTH_MEDIA_SOURCES -> openBluetoothAudio()
                    !Apps.launch(this, source) -> open(Config.MUSIC, "Audio")
                }
            },
            cycleCity = {
                cityIndex = if (cityIndex >= Config.CITIES.lastIndex) -1 else cityIndex + 1
                prefs.edit().putInt("city", cityIndex).apply()
                weather = null
                // The design has no city label, so confirm the choice with a toast.
                val name = Config.CITIES.getOrNull(cityIndex)?.name ?: "Automático (GPS)"
                notice = Notice("Clima: $name", Icons.Rounded.LocationOn)
            },
            // Route the card's buttons to whatever is actually playing: the tuner, Jancar's
            // Bluetooth stack (the generic BT media session ignores commands on this
            // firmware), or a regular app session such as Spotify.
            playPause = {
                when {
                    radioMode() -> jRadio.toggleMute()
                    bluetoothSource() -> jBt.playPause()
                    else -> media.playPause()
                }
            },
            next = {
                when {
                    radioMode() -> jRadio.stepUp()
                    bluetoothSource() -> jBt.next()
                    else -> media.next()
                }
            },
            previous = {
                when {
                    radioMode() -> jRadio.stepDown()
                    bluetoothSource() -> jBt.previous()
                    else -> media.previous()
                }
            },
        )

        setContent {
            val nowPlaying by media.state.collectAsStateWithLifecycle()
            val radioState by jRadio.state.collectAsStateWithLifecycle()
            val sourceLabel = remember(nowPlaying?.sourcePackage) { sourceLabel(nowPlaying?.sourcePackage) }

            LaunchedEffect(cityIndex) {
                val city = Config.CITIES.getOrNull(cityIndex)
                while (true) {
                    WeatherRepository.fetch(applicationContext, city)?.let { weather = it }
                    delay(if (weather == null) 15_000L else 15 * 60_000L)
                }
            }
            LaunchedEffect(welcomeEpoch) {
                showWelcome = true
                delay(Config.WELCOME_MINUTES * 60_000L)
                showWelcome = false
            }
            LaunchedEffect(notice?.id) {
                if (notice != null) {
                    delay(2500)
                    notice = null
                }
            }
            // Which source is on, and (for the radio) frequency + RDS name. The firmware
            // doesn't push these to third-party apps, so poll — only while on screen.
            LaunchedEffect(onScreen) {
                while (onScreen) {
                    jMedia.activeMedia()?.let { audioSource = it }
                    if (radioMode()) jRadio.poll()
                    delay(if (radioMode()) 1000L else 2500L)
                }
            }
            LaunchedEffect(Unit) {
                while (true) {
                    wifi = readWifi()
                    delay(10_000L)
                }
            }

            BoxWithConstraints(Modifier.fillMaxSize()) {
                val unit = minOf(maxWidth / DESIGN_W, maxHeight / DESIGN_H)
                CompositionLocalProvider(LocalUnit provides unit) {
                    AppBackground()
                    val apps = drawerApps
                    if (apps != null) {
                        BackHandler { drawerApps = null }
                        AppDrawer(
                            apps,
                            onLaunch = { Apps.launch(this@MainActivity, it.packageName); drawerApps = null },
                            onClose = { drawerApps = null },
                        )
                    } else {
                        BackHandler { /* home screen: swallow back */ }
                        val radio = if (audioSource == JancarMedia.Type.RADIO) {
                            RadioInfo(radioState.freqKhz, radioState.stationName, radioState.muted)
                        } else null
                        HomeScreen(HomeState(weather, nowPlaying, sourceLabel, bluetoothOn, wifi, showWelcome, radio), actions)
                    }
                    NoticePill(notice)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        onScreen = true
        media.start()
        registerReceiver(btReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        refreshBluetooth()
    }

    override fun onStop() {
        onScreen = false
        media.stop()
        unregisterReceiver(btReceiver)
        super.onStop()
    }

    override fun onDestroy() {
        unregisterReceiver(screenOnReceiver)
        jRadio.disconnect()
        jMedia.disconnect()
        jBt.disconnect()
        super.onDestroy()
    }

    /** Pressing Home while already home closes the app drawer. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        drawerApps = null
    }

    private fun refreshBluetooth() {
        @Suppress("DEPRECATION")
        bluetoothOn = runCatching { BluetoothAdapter.getDefaultAdapter()?.isEnabled == true }.getOrDefault(false)
    }

    /** True only when the active network has been validated as reaching the internet. */
    private fun hasInternet(): Boolean = runCatching {
        val cm = getSystemService(ConnectivityManager::class.java)
        val caps = cm.getNetworkCapabilities(cm.activeNetwork)
        caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }.getOrDefault(false)

    /** Signal bars (0–4) and band of the current Wi-Fi link, or null when not connected. */
    @Suppress("DEPRECATION")
    private fun readWifi(): WifiStatus? = runCatching {
        val wm = applicationContext.getSystemService(WifiManager::class.java) ?: return null
        val info = wm.connectionInfo ?: return null
        if (info.networkId == -1) return null
        WifiStatus(
            bars = WifiManager.calculateSignalLevel(info.rssi, 5).coerceIn(0, 4),
            band = if (info.frequency >= 4900) "5G" else "2.4G",
        )
    }.getOrNull()

    private fun sourceLabel(pkg: String?): String? = when {
        pkg == null || pkg in Config.BLUETOOTH_MEDIA_SOURCES -> "Bluetooth Audio"
        else -> runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
        }.getOrNull()
    }

    private fun radioMode() = audioSource == JancarMedia.Type.RADIO

    private fun bluetoothSource(): Boolean {
        val pkg = media.state.value?.sourcePackage
        return pkg == null || pkg in Config.BLUETOOTH_MEDIA_SOURCES
    }

    /**
     * FM on, shown in the media card.
     *
     * The tuner's audio is rendered by Jancar's own radio app, and it only starts it while
     * its screen is on top (opening the tuner ourselves powers it but plays nothing, and
     * asking the source arbiter for RADIO doesn't wake that app reliably). So: pause the
     * phone (A2DP streaming would grab the source back), flash the radio app without
     * animation, and come straight back — the radio keeps playing in the background.
     */
    private fun switchToRadio() {
        jBt.pause()
        val noAnim = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION
        val (pkg, cls) = Config.RADIO_ACTIVITY
        if (runCatching { startActivity(Intent().setComponent(ComponentName(pkg, cls)).addFlags(noAnim)) }.isFailure) {
            return open(Config.RADIO, "Radio FM")
        }
        audioSource = JancarMedia.Type.RADIO
        window.decorView.postDelayed({
            startActivity(Intent(this, MainActivity::class.java).addFlags(noAnim or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
            overridePendingTransition(0, 0)
        }, RADIO_WAKE_MS)
    }

    /** Hands the audio source back to Bluetooth (stops the tuner) and resumes the phone. */
    private fun switchToBluetooth() {
        jMedia.open(JancarMedia.Type.A2DP)
        jBt.play()
        audioSource = JancarMedia.Type.A2DP
    }

    // Whoever opens a source receives the steering-wheel / bezel keys for it, so once
    // the launcher has taken the radio or Bluetooth over it must act on them.
    // Radio next/prev move one tuning step, like Jancar's own radio app (its seek
    // mode finds nothing on this unit, and its station list is private to that app).
    private fun onRadioKey(key: JancarKey) {
        val muted = jRadio.state.value.muted
        when (key) {
            JancarKey.NEXT -> jRadio.stepUp()
            JancarKey.PREV -> jRadio.stepDown()
            JancarKey.PLAY_PAUSE -> jRadio.toggleMute()
            JancarKey.PAUSE -> if (!muted) jRadio.toggleMute()
            JancarKey.PLAY, JancarKey.RESUME -> if (muted) jRadio.toggleMute()
            else -> Unit // STOP / SUSPEND / QUIT: another source took over; state already updated
        }
    }

    private fun onBluetoothSourceKey(key: JancarKey) {
        if (audioSource == JancarMedia.Type.RADIO) return onRadioKey(key)
        when (key) {
            JancarKey.NEXT -> jBt.next()
            JancarKey.PREV -> jBt.previous()
            JancarKey.PLAY_PAUSE -> jBt.playPause()
            JancarKey.PLAY, JancarKey.RESUME -> jBt.play()
            // Another source (radio, a call…) took the speakers: don't keep the phone playing silently.
            JancarKey.PAUSE, JancarKey.SUSPEND, JancarKey.STOP -> jBt.pause()
            JancarKey.QUIT -> Unit
        }
    }

    /** Jancar's Bluetooth-music screen directly; falls back to the music app. */
    private fun openBluetoothAudio() {
        val (pkg, cls) = Config.BLUETOOTH_AUDIO_ACTIVITY
        val intent = Intent().setComponent(ComponentName(pkg, cls)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (runCatching { startActivity(intent) }.isFailure) open(Config.MUSIC, "Bluetooth Audio")
    }

    private fun open(candidates: List<String>, what: String) {
        if (!Apps.launchAny(this, candidates)) Apps.notInstalled(this, what)
    }
}
