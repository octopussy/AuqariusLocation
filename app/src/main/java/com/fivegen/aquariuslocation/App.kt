package com.fivegen.aquariuslocation

import android.app.Application
import android.location.Location
import android.util.Log
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import java.time.LocalDateTime

data class LogMessage(val time: LocalDateTime, val msg: String, val isError: Boolean) {
    override fun toString(): String {
        val suffix = if (this.isError) "[ERR]" else ""
        return "${this.time.formatTime()}: $suffix ${this.msg}"
    }
}

class App : Application() {

    private lateinit var storage: AppStorage

    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation = _currentLocation.asStateFlow()

    private val _locationHistory = MutableStateFlow<List<Location>>(emptyList())
    val locationHistory = _locationHistory.asStateFlow()

    private val _recentLog = MutableSharedFlow<LogMessage>(replay = 50, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val recentLog = _recentLog.asSharedFlow()

    override fun onCreate() {
        super.onCreate()
        instance = this
        storage = AppStorage(this)
    }

    fun publishCurrentLocation(location: Location) {
        logD("New location: ${location.formatLanLon()}")
        _locationHistory.update { it + location }
        _currentLocation.update { location }
    }

    fun logD(msg: String) {
        Log.d(TAG, msg)
        appendLog(msg)
    }

    fun logE(msg: String) {
        Log.e(TAG, msg)
        appendLog(msg, true)
    }

    private fun appendLog(msg: String, isError: Boolean = false) {
        _recentLog.tryEmit(
            LogMessage(
                time = LocalDateTime.now(),
                msg = msg,
                isError = isError
            )
        )
    }

    companion object {
        private const val TAG = "[LOCATION]"

        lateinit var instance: App

        val storage get() = instance.storage
    }
}
