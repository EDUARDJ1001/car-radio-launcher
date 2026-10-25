package com.eduard.crvlauncher

/**
 * Everything specific to this head unit lives here. For each shortcut the
 * first installed package in its list is launched.
 */
object Config {
    const val DRIVER_NAME = "Eduard"

    /**
     * Cities the weather can be pinned to (tap the weather to cycle).
     * "Automático" uses the unit's GPS and falls back to the first city.
     */
    val CITIES = listOf(
        City("Tegucigalpa", 14.0723, -87.1921),
        City("San Pedro Sula", 15.5042, -88.0250),
        City("Puerto Cortés", 15.8500, -87.9400),
        City("Santa Rosa de Copán", 14.7667, -88.7833),
    )

    // Real packages read from the unit over ADB (Jancar firmware on AutoChips AC8227L, Android 8.1).
    val NAVIGATION = listOf(
        "net.osmand",
        "net.osmand.plus",
        "app.organicmaps",
        "com.here.app.maps",
        "com.google.android.apps.maps",
    )
    val PHONE = listOf("com.jancar.bluetooth")
    val RADIO = listOf("com.jancar.radio")
    val MUSIC = listOf("com.spotify.music", "com.jancar.music")
    /** Wired Android Auto receiver first, then AutoAI's wireless link app. */
    val ANDROID_AUTO = listOf("com.google.android.projection.sink", "com.autoai.car.jlink")
    /** Media sessions that mean "music from the phone over Bluetooth". */
    val BLUETOOTH_MEDIA_SOURCES = setOf("com.android.bluetooth", "com.jancar.music", "com.jancar.bluetooth")
    val CARPLAY = listOf("com.autochips.carplayapp", "com.autoai.car.jlink")
}

data class City(val name: String, val lat: Double, val lon: Double)
