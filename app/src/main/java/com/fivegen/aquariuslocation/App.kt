package com.fivegen.aquariuslocation

import android.app.Application
import android.location.Location
import android.util.Log
import androidx.room.Room
import com.fivegen.aquariuslocation.db.AppDatabase
import com.fivegen.aquariuslocation.db.DbLocation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.util.*

data class AppLocation(val latitude: Double, val longitude: Double, val altitude: Double)

data class LogMessage(val time: LocalDateTime, val msg: String, val isError: Boolean) {
    override fun toString(): String {
        val suffix = if (this.isError) "[ERR]" else ""
        return "${this.time.formatTime()}: $suffix ${this.msg}"
    }
}

class App : Application() {

    private val scope = CoroutineScope(Dispatchers.IO)

    private val db by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "locations").build()
    }

    private val locationDao by lazy { db.locationDao() }

    private lateinit var storage: AppStorage

    private val _locationHistory by lazy { locationDao.flowAll() }
    val locationHistory
        get() = _locationHistory.map {
            it.map { AppLocation(it.latitude, it.longitude, it.altitude) }
        }

    private val _recentLog = MutableSharedFlow<LogMessage>(replay = Int.MAX_VALUE, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val recentLog = _recentLog.asSharedFlow()


    override fun onCreate() {
        super.onCreate()
        instance = this
        storage = AppStorage(this)
    }

    fun publishCurrentLocation(location: Location) {
        logD("New location: ${location.formatLanLon()}")
        scope.launch {
            locationDao.insert(
                DbLocation(
                    date = Date(),
                    latitude = location.latitude,
                    longitude = location.longitude,
                    altitude = location.altitude
                )
            )
        }
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
