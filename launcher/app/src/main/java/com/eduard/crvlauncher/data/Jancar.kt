package com.eduard.crvlauncher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Parcel
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/*
 * Clients for the Jancar head-unit firmware's central service (com.jancar.services).
 *
 * Its radio/media services are exported without a permission, but their AIDL
 * stubs only ship inside Jancar's own apps, so we speak the Binder protocol by
 * hand. Transaction codes and parcel layouts were read from the decompiled
 * com.jancar.services.radio.IRadio / media.IMedia stubs (firmware JCAC10003,
 * Oct 2025) — if Jancar updates the firmware they must be re-checked.
 */

private const val TAG = "CrvJancar"
private const val SERVICES_PKG = "com.jancar.services"

/** Which side asked us to do something through the "opener" callback (steering wheel, MODE key…). */
enum class JancarKey { PLAY, PAUSE, PLAY_PAUSE, NEXT, PREV, STOP, SUSPEND, RESUME, QUIT }

/** Shared bind/transact plumbing for one Jancar service interface. */
private abstract class JancarService(
    private val context: Context,
    private val action: String,
    protected val descriptor: String,
    private val pkg: String = SERVICES_PKG,
) {
    @Volatile private var remote: IBinder? = null
    private val pending = mutableListOf<() -> Unit>()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Log.i(TAG, "$action connected")
            remote = service
            synchronized(pending) { pending.toList().also { pending.clear() } }.forEach { it() }
        }
        override fun onServiceDisconnected(name: ComponentName) {
            Log.w(TAG, "$action disconnected")
            remote = null
        }
    }

    fun bind(): Boolean = runCatching {
        context.bindService(Intent(action).setPackage(pkg), connection, Context.BIND_AUTO_CREATE)
    }.onFailure { Log.w(TAG, "bind $action failed", it) }.getOrDefault(false)

    fun unbind() {
        runCatching { context.unbindService(connection) }
        remote = null
    }

    /** Runs [block] now if bound, or as soon as the service connects. */
    protected fun whenBound(block: () -> Unit) {
        if (remote != null) block() else synchronized(pending) { pending += block }
    }

    protected fun <T> call(code: Int, args: Parcel.() -> Unit = {}, result: Parcel.() -> T): T? {
        val binder = remote ?: return null
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        return try {
            data.writeInterfaceToken(descriptor)
            data.args()
            binder.transact(code, data, reply, 0)
            reply.readException()
            reply.result()
        } catch (e: Exception) {
            Log.w(TAG, "$descriptor#$code failed", e)
            null
        } finally {
            data.recycle()
            reply.recycle()
        }
    }

    protected fun call(code: Int, args: Parcel.() -> Unit = {}) {
        call(code, args) { }
    }
}

/** com.jancar.services.radio.IRadio — the FM/AM tuner. Frequencies are in kHz (96100 = 96.1 MHz). */
class JancarRadio(context: Context) {

    data class State(
        /** True while the radio is the active source (we opened it and nobody took over). */
        val active: Boolean,
        val freqKhz: Int,
        /** RDS station name, when the station broadcasts one. */
        val stationName: String?,
        val muted: Boolean,
    )

    private val _state = MutableStateFlow(State(active = false, freqKhz = 0, stationName = null, muted = false))
    val state: StateFlow<State> = _state

    /** Keys the firmware routes to whoever opened the radio (steering wheel, MODE…). */
    var onKey: (JancarKey) -> Unit = {}

    private val main = Handler(Looper.getMainLooper())
    private val packageName = context.packageName

    private val service = object : JancarService(context, "com.jancar.services.action.radio", "com.jancar.services.radio.IRadio") {
        fun open() = call(1) { writeStrongBinder(callback); writeString(packageName) }
        fun close() = call(2)
        fun setFreq(khz: Int) = call(3) { writeInt(khz) }
        fun getFreq(): Int? = call(4, {}) { readInt() }
        fun scanUp(fromKhz: Int) = call(8) { writeInt(fromKhz) }
        fun scanDown(fromKhz: Int) = call(9) { writeInt(fromKhz) }
        fun step(direction: Int) = call(12) { writeInt(direction) }
        fun getPsText(khz: Int): String? = call(22, { writeInt(khz) }) { readString() }
        fun mute() = call(28)
        fun unMute() = call(29)
        fun later(block: () -> Unit) = whenBound(block)
    }

    // IRadioCallback, implemented by hand (descriptor + codes from the decompiled stub).
    private val callback = object : Binder() {
        private val desc = "com.jancar.services.radio.IRadioCallback"
        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            if (code == INTERFACE_TRANSACTION) {
                reply?.writeString(desc)
                return true
            }
            data.enforceInterface(desc)
            when (code) {
                1, 4 -> setFreq(data.readInt())                            // onSetFreq / onFreqChanged
                2 -> _state.value = _state.value.copy(active = true)       // onPowerOn
                3 -> _state.value = _state.value.copy(active = false)      // onPowerOff
                21 -> {                                                    // onRdsPsChanged(pi, freq, ps)
                    data.readInt()
                    val freq = data.readInt()
                    val ps = data.readString()?.trim()?.ifEmpty { null }
                    if (freq == 0 || freq == _state.value.freqKhz) _state.value = _state.value.copy(stationName = ps)
                }
                10 -> key(JancarKey.SUSPEND)
                11 -> key(JancarKey.RESUME)
                12 -> key(JancarKey.PAUSE)
                13 -> key(JancarKey.PLAY)
                14 -> key(JancarKey.PLAY_PAUSE)
                15 -> key(JancarKey.STOP)
                16 -> key(JancarKey.NEXT)
                17 -> key(JancarKey.PREV)
                18 -> key(JancarKey.QUIT)
                else -> Log.v(TAG, "radio callback #$code ignored")
            }
            reply?.writeNoException()
            return true
        }
    }

    private fun setFreq(khz: Int) {
        if (khz <= 0) return
        val changed = khz != _state.value.freqKhz
        _state.value = _state.value.copy(freqKhz = khz, stationName = if (changed) null else _state.value.stationName)
    }

    private fun key(k: JancarKey) {
        Log.i(TAG, "radio key $k")
        if (k == JancarKey.QUIT || k == JancarKey.SUSPEND || k == JancarKey.STOP) {
            _state.value = _state.value.copy(active = false)
        }
        main.post { onKey(k) }
    }

    fun connect() = service.bind()
    fun disconnect() = service.unbind()

    /** Makes the radio the active audio source and starts receiving its events. */
    fun open() = service.later {
        service.open()
        service.getFreq()?.let(::setFreq)
        _state.value = _state.value.copy(active = true, muted = false)
    }

    fun close() = service.later {
        service.close()
        _state.value = _state.value.copy(active = false)
    }

    fun seekUp() = service.later { service.scanUp(_state.value.freqKhz) }
    fun seekDown() = service.later { service.scanDown(_state.value.freqKhz) }
    fun stepUp() = service.later { service.step(1) }
    fun stepDown() = service.later { service.step(-1) }
    fun tune(khz: Int) = service.later { service.setFreq(khz) }

    fun toggleMute() = service.later {
        val muted = !_state.value.muted
        if (muted) service.mute() else service.unMute()
        _state.value = _state.value.copy(muted = muted)
    }

    /**
     * Re-reads frequency and RDS name. The firmware pushes those events to its own
     * apps over an internal event bus, not through the open() callback, so the
     * card polls this while the radio is the active source.
     */
    fun poll() = service.later {
        service.getFreq()?.let(::setFreq)
        val f = _state.value.freqKhz
        if (f > 0) {
            val ps = service.getPsText(f)?.trim()?.ifEmpty { null }
            if (ps != _state.value.stationName) _state.value = _state.value.copy(stationName = ps)
        }
    }
}

/** com.jancar.services.media.IMedia — the firmware's audio-source arbiter. */
class JancarMedia(context: Context) {

    /** IVIMedia.MediaType values used by the firmware. */
    object Type {
        const val NONE = -1
        const val MUSIC = 0
        const val RADIO = 1
        const val A2DP = 7
        const val THIRD_PARTY = 20
        const val CARPLAY_MUSIC = 25
    }

    var onKey: (JancarKey) -> Unit = {}

    private val main = Handler(Looper.getMainLooper())
    private val packageName = context.packageName

    private val service = object : JancarService(context, "com.jancar.services.action.media", "com.jancar.services.media.IMedia") {
        fun open(type: Int) = call(1) { writeInt(type); writeStrongBinder(callback); writeString(packageName) }
        fun close(type: Int) = call(2) { writeInt(type) }
        fun getActiveMedia(): Int? = call(7, {}) { readInt() }
        fun later(block: () -> Unit) = whenBound(block)
    }

    // IMediaControlCallback, implemented by hand.
    private val callback = object : Binder() {
        private val desc = "com.jancar.services.media.IMediaControlCallback"
        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            if (code == INTERFACE_TRANSACTION) {
                reply?.writeString(desc)
                return true
            }
            data.enforceInterface(desc)
            val k = when (code) {
                1 -> JancarKey.SUSPEND
                2 -> JancarKey.RESUME
                3 -> JancarKey.PAUSE
                4 -> JancarKey.PLAY
                5 -> JancarKey.PLAY_PAUSE
                6 -> JancarKey.QUIT
                7 -> JancarKey.STOP
                9 -> JancarKey.NEXT
                10 -> JancarKey.PREV
                else -> null
            }
            if (k != null) {
                Log.i(TAG, "media key $k")
                main.post { onKey(k) }
            }
            reply?.writeNoException()
            return true
        }
    }

    fun connect() = service.bind()
    fun disconnect() = service.unbind()

    /** Asks the firmware to switch the audio source to [type] (e.g. [Type.A2DP]). */
    fun open(type: Int) = service.later {
        Log.i(TAG, "media open($type)")
        service.open(type)
    }
    fun close(type: Int) = service.later { service.close(type) }

    /** Current source as the firmware sees it, or null if the service isn't reachable. */
    fun activeMedia(): Int? = service.run { getActiveMedia() }
}

/**
 * com.jancar.btservice.bluetooth.IBluetooth — Jancar's own Bluetooth stack front-end.
 * Its built-in BT-music player sends AVRCP commands through here; the generic
 * MediaSession transport controls of com.android.bluetooth are ignored on this
 * firmware, so the media card's buttons must use these.
 */
class JancarBluetooth(context: Context) {

    private val service = object : JancarService(
        context, "com.jancar.btservice.action.bluetooth", "com.jancar.btservice.bluetooth.IBluetooth", "com.jancar.btservice",
    ) {
        // Every music call takes an IBluetoothExecCallback for its result; we don't need it.
        fun music(code: Int) {
            Log.i(TAG, "bt music #$code")
            call(code) { writeStrongBinder(execCallback) }
        }
        fun later(block: () -> Unit) = whenBound(block)
    }

    private val execCallback = object : Binder() {
        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            if (code == INTERFACE_TRANSACTION) {
                reply?.writeString("com.jancar.btservice.bluetooth.IBluetoothExecCallback")
                return true
            }
            reply?.writeNoException()
            return true
        }
    }

    fun connect() = service.bind()
    fun disconnect() = service.unbind()

    fun next() = service.later { service.music(8) }
    fun previous() = service.later { service.music(9) }
    fun playPause() = service.later { service.music(10) }
    fun play() = service.later { service.music(11) }
    fun pause() = service.later { service.music(12) }
}
