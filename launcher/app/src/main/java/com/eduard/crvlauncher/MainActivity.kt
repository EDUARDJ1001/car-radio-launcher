package com.eduard.crvlauncher

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eduard.crvlauncher.data.AppEntry
import com.eduard.crvlauncher.data.Apps
import com.eduard.crvlauncher.data.LocationRepository
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
import com.eduard.crvlauncher.ui.findMapFile
import kotlinx.coroutines.delay
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var media: MediaRepository
    private lateinit var location: LocationRepository
    private var mapFile by mutableStateOf<File?>(null)
    private var weather by mutableStateOf<Weather?>(null)
    private var bluetoothOn by mutableStateOf(false)
    private var drawerApps by mutableStateOf<List<AppEntry>?>(null)

    /** -1 = automatic (GPS), otherwise index into [Config.CITIES]. */
    private var cityIndex by mutableIntStateOf(-1)
    private val prefs by lazy { getSharedPreferences("launcher", MODE_PRIVATE) }

    private val btReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = refreshBluetooth()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        media = MediaRepository(applicationContext)
        location = LocationRepository(applicationContext)
        cityIndex = prefs.getInt("city", -1).coerceIn(-1, Config.CITIES.lastIndex)
        requestPermissions(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_EXTERNAL_STORAGE,
            ),
            1,
        )

        val actions = HomeActions(
            openNavigation = { open(Config.NAVIGATION, "Navegación") },
            openPhone = { open(Config.PHONE, "Teléfono") },
            openMusic = { open(Config.MUSIC, "Música") },
            openRadio = { open(Config.RADIO, "Radio FM") },
            openAndroidAuto = { open(Config.ANDROID_AUTO, "Android Auto") },
            openCarPlay = { open(Config.CARPLAY, "CarPlay") },
            openApps = { drawerApps = Apps.installedLaunchables(this) },
            openSettings = { Apps.openSettings(this) },
            cycleCity = {
                cityIndex = if (cityIndex >= Config.CITIES.lastIndex) -1 else cityIndex + 1
                prefs.edit().putInt("city", cityIndex).apply()
                weather = null
            },
            playPause = { media.playPause() },
            next = { media.next() },
            previous = { media.previous() },
        )

        setContent {
            val nowPlaying by media.state.collectAsStateWithLifecycle()
            val fix by location.fix.collectAsStateWithLifecycle()

            LaunchedEffect(cityIndex) {
                val city = Config.CITIES.getOrNull(cityIndex)
                while (true) {
                    WeatherRepository.fetch(applicationContext, city)?.let { weather = it }
                    delay(if (weather == null) 15_000L else 15 * 60_000L)
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
                        HomeScreen(
                            HomeState(
                                weather, nowPlaying, bluetoothOn,
                                cityLabel = Config.CITIES.getOrNull(cityIndex)?.name ?: "Automático",
                                fix = fix,
                                mapFile = mapFile,
                            ),
                            actions,
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        media.start()
        location.start()
        mapFile = findMapFile(this)
        registerReceiver(btReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        refreshBluetooth()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        location.start()
        mapFile = findMapFile(this)
    }

    override fun onStop() {
        media.stop()
        location.stop()
        unregisterReceiver(btReceiver)
        super.onStop()
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

    private fun open(candidates: List<String>, what: String) {
        if (!Apps.launchAny(this, candidates)) Apps.notInstalled(this, what)
    }
}
