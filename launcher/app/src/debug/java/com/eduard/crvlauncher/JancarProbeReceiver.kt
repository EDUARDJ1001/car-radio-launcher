package com.eduard.crvlauncher

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.eduard.crvlauncher.data.JancarBluetooth
import com.eduard.crvlauncher.data.JancarMedia
import com.eduard.crvlauncher.data.JancarRadio

/**
 * Debug-only: drive the Jancar bridge from adb and watch logcat (tag CrvJancar).
 *   adb shell am broadcast -a com.eduard.crvlauncher.PROBE -n com.eduard.crvlauncher/.JancarProbeReceiver --es cmd radio_open
 */
class JancarProbeReceiver : BroadcastReceiver() {

    @SuppressLint("StaticFieldLeak")
    private companion object {
        var radio: JancarRadio? = null
        var media: JancarMedia? = null
        var bt: JancarBluetooth? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val r = radio ?: JancarRadio(app).also { it.connect(); it.onKey = { k -> Log.i("CrvJancar", "probe radio key $k") }; radio = it }
        val m = media ?: JancarMedia(app).also { it.connect(); it.onKey = { k -> Log.i("CrvJancar", "probe media key $k") }; media = it }
        val b = bt ?: JancarBluetooth(app).also { it.connect(); bt = it }
        val cmd = intent.getStringExtra("cmd")
        Log.i("CrvJancar", "probe cmd=$cmd")
        when (cmd) {
            "radio_open" -> r.open()
            "radio_close" -> r.close()
            "radio_seek_up" -> r.seekUp()
            "radio_seek_down" -> r.seekDown()
            "radio_step_up" -> r.stepUp()
            "radio_mute" -> r.toggleMute()
            "radio_poll" -> r.poll()
            "media_open_radio" -> m.open(JancarMedia.Type.RADIO)
            "media_open_a2dp" -> m.open(JancarMedia.Type.A2DP)
            "media_close_a2dp" -> m.close(JancarMedia.Type.A2DP)
            "bt_next" -> b.next()
            "bt_prev" -> b.previous()
            "bt_playpause" -> b.playPause()
            "bt_pause" -> b.pause()
            "media_active" -> Log.i("CrvJancar", "active media = ${m.activeMedia()}")
        }
        Log.i("CrvJancar", "radio state = ${r.state.value}")
    }
}
