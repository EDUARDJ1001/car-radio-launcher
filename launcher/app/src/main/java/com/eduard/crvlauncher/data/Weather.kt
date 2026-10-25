package com.eduard.crvlauncher.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.util.Log
import com.eduard.crvlauncher.City
import com.eduard.crvlauncher.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.roundToInt

data class Weather(
    val temp: Int,
    val feelsLike: Int,
    val max: Int,
    val min: Int,
    val humidity: Int,
    val windKmh: Int,
    val code: Int,
    /** City name, or "GPS" when the unit's own location was used. */
    val place: String,
    val lat: Double,
    val lon: Double,
    val isDay: Boolean = true,
) {
    val description: String get() = describe(code)
}

/** Open-Meteo: free, no account or API key needed. */
object WeatherRepository {

    /** [city] null = automatic: GPS if there's a fix, otherwise the first configured city. */
    suspend fun fetch(context: Context, city: City?): Weather? = withContext(Dispatchers.IO) {
        val gps = if (city == null) lastLocation(context) else null
        val target = city ?: Config.CITIES.first()
        val lat = gps?.first ?: target.lat
        val lon = gps?.second ?: target.lon
        val place = if (gps != null) "GPS" else target.name
        val url = URL(
            "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,apparent_temperature,relative_humidity_2m,wind_speed_10m,weather_code,is_day" +
                "&daily=temperature_2m_max,temperature_2m_min&forecast_days=1&timezone=auto"
        )
        runCatching {
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
            }
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val json = JSONObject(body)
            val cur = json.getJSONObject("current")
            val daily = json.getJSONObject("daily")
            Weather(
                temp = cur.getDouble("temperature_2m").roundToInt(),
                feelsLike = cur.getDouble("apparent_temperature").roundToInt(),
                max = daily.getJSONArray("temperature_2m_max").getDouble(0).roundToInt(),
                min = daily.getJSONArray("temperature_2m_min").getDouble(0).roundToInt(),
                humidity = cur.getInt("relative_humidity_2m"),
                windKmh = cur.getDouble("wind_speed_10m").roundToInt(),
                code = cur.getInt("weather_code"),
                place = place,
                lat = lat,
                lon = lon,
                isDay = cur.optInt("is_day", 1) == 1,
            )
        }.onFailure { Log.w("CrvWeather", "weather fetch failed for $place", it) }.getOrNull()
    }

    @SuppressLint("MissingPermission")
    private fun lastLocation(context: Context): Pair<Double, Double>? {
        val granted = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return null
        val lm = context.getSystemService(LocationManager::class.java) ?: return null
        return listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
            ?.let { it.latitude to it.longitude }
    }
}

/** WMO weather codes → Spanish label. */
private fun describe(code: Int): String = when (code) {
    0 -> "Despejado"
    1 -> "Mayormente despejado"
    2 -> "Parcialmente nublado"
    3 -> "Nublado"
    45, 48 -> "Niebla"
    51, 53, 55, 56, 57 -> "Llovizna"
    61, 63, 65, 66, 67 -> "Lluvia"
    71, 73, 75, 77 -> "Nieve"
    80, 81, 82 -> "Chubascos"
    85, 86 -> "Chubascos de nieve"
    95, 96, 99 -> "Tormenta"
    else -> "—"
}
