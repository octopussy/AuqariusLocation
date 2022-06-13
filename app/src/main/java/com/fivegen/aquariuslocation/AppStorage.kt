package com.fivegen.aquariuslocation

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

class AppStorage(context: Context) {

    private val prefs = context.getSharedPreferences("storage", Context.MODE_PRIVATE)

    var acceptableTimePeriod by LongPref(prefs, 5000L)
    var requiredTimeInterval by LongPref(prefs, 5000L)
    var requiredDistanceInterval by LongPref(prefs, 5L)
    var acceptableAccuracy by FloatPref(prefs, 10f)
    var setWaitPeriodGPS by LongPref(prefs,20 * 1000L)
    var setWaitPeriodNetwork by LongPref(prefs,20 * 1000L)
}

class LongPref(private val pref: SharedPreferences, private val defaultValue: Long) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Long = pref.getLong(property.name, defaultValue)

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Long) {
        pref.edit {
            putLong(property.name, value)
        }
    }
}

class FloatPref(private val pref: SharedPreferences, private val defaultValue: Float) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Float = pref.getFloat(property.name, defaultValue)

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Float) {
        pref.edit {
            putFloat(property.name, value)
        }
    }
}

