package com.fivegen.aquariuslocation

import android.location.Location
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

private val dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

fun Location.formatLanLon() = String.format(Locale.US, "%.6f %.6f", this.latitude, this.longitude)

fun LocalDateTime.formatTime(): String = dateTimeFormatter.format(this)
