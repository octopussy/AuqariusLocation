package com.fivegen.aquariuslocation

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.os.IBinder
import com.yayandroid.locationmanager.LocationManager
import com.yayandroid.locationmanager.configuration.DefaultProviderConfiguration
import com.yayandroid.locationmanager.configuration.GooglePlayServicesConfiguration
import com.yayandroid.locationmanager.configuration.LocationConfiguration
import com.yayandroid.locationmanager.constants.FailType
import com.yayandroid.locationmanager.constants.ProcessType
import com.yayandroid.locationmanager.constants.ProviderType
import com.yayandroid.locationmanager.listener.LocationListener

class LocationService : Service() {

    private var locationManager: LocationManager? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        locationManager?.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_RESET || locationManager == null) {
            setupLocationManager()
        }
        return START_STICKY
    }

    private fun setupLocationManager() {
        locationManager?.onDestroy()

        App.instance.logD("-----------------------")
        App.instance.logD(" Setup location manager...")
        App.instance.logD(" acceptableTimePeriod: ${App.storage.acceptableTimePeriod}")
        App.instance.logD(" requiredTimeInterval: ${App.storage.requiredTimeInterval}")
        App.instance.logD(" requiredDistanceInterval: ${App.storage.requiredDistanceInterval}")
        App.instance.logD(" acceptableAccuracy: ${App.storage.acceptableAccuracy}")
        App.instance.logD(" setWaitPeriodGPS: ${App.storage.setWaitPeriodGPS}")
        App.instance.logD(" setWaitPeriodNetwork: ${App.storage.setWaitPeriodNetwork}")
        App.instance.logD("-----------------------")

        val awesomeConfiguration = LocationConfiguration.Builder()
            .keepTracking(true)
            .useGooglePlayServices(
                GooglePlayServicesConfiguration.Builder()
                    //.locationRequest(YOUR_CUSTOM_LOCATION_REQUEST_OBJECT)
                    .fallbackToDefault(true)
                    .askForGooglePlayServices(false)
                    .askForSettingsApi(true)
                    //.failOnConnectionSuspended(true)
                    .failOnSettingsApiSuspended(false)
                    .ignoreLastKnowLocation(false)
                    .setWaitPeriod(20 * 1000)
                    .build()
            )
            .useDefaultProviders(
                DefaultProviderConfiguration.Builder()
                    .acceptableTimePeriod(App.storage.acceptableTimePeriod)
                    .requiredTimeInterval(App.storage.requiredTimeInterval)
                    .requiredDistanceInterval(App.storage.requiredDistanceInterval)
                    .acceptableAccuracy(App.storage.acceptableAccuracy)
                    //.gpsMessage("Turn on GPS?")
                    //.gpsDialogProvider(YourCustomDialogProvider())
                    .setWaitPeriod(ProviderType.GPS, App.storage.setWaitPeriodGPS)
                    .setWaitPeriod(ProviderType.NETWORK, App.storage.setWaitPeriodNetwork)
                    .build()
            )
            .build()

        locationManager = LocationManager.Builder(applicationContext)
            //.activity(this) // Only required to ask permission and/or GoogleApi - SettingsApi
            .configuration(awesomeConfiguration)
            .notify(MyLocationListener())
            .build()

        locationManager?.get()
    }

    inner class MyLocationListener : LocationListener {

        override fun onProcessTypeChanged(processType: Int) {
            val type = when (processType) {
                ProcessType.ASKING_PERMISSIONS -> "ASKING_PERMISSIONS"
                ProcessType.GETTING_LOCATION_FROM_CUSTOM_PROVIDER -> "GETTING_LOCATION_FROM_CUSTOM_PROVIDER"
                ProcessType.GETTING_LOCATION_FROM_GOOGLE_PLAY_SERVICES -> "GETTING_LOCATION_FROM_GOOGLE_PLAY_SERVICES"
                ProcessType.GETTING_LOCATION_FROM_GPS_PROVIDER -> "GETTING_LOCATION_FROM_GPS_PROVIDER"
                ProcessType.GETTING_LOCATION_FROM_NETWORK_PROVIDER -> "GETTING_LOCATION_FROM_NETWORK_PROVIDER"
                else -> "UNKNOWN"
            }
            App.instance.logD("onProcessTypeChanged $type")
        }

        @SuppressLint("SetTextI18n")
        override fun onLocationChanged(location: Location?) {
            if (location != null) {
                App.instance.publishCurrentLocation(location)
            }
        }

        override fun onLocationFailed(type: Int) {
            val t = when (type) {
                FailType.TIMEOUT -> {
                    setupLocationManager()
                    "TIMEOUT"
                }
                FailType.PERMISSION_DENIED -> "PERMISSION_DENIED"
                FailType.NETWORK_NOT_AVAILABLE -> "NETWORK_NOT_AVAILABLE"
                FailType.GOOGLE_PLAY_SERVICES_NOT_AVAILABLE -> "GOOGLE_PLAY_SERVICES_NOT_AVAILABLE"
                FailType.GOOGLE_PLAY_SERVICES_SETTINGS_DIALOG -> "GOOGLE_PLAY_SERVICES_SETTINGS_DIALOG"
                FailType.GOOGLE_PLAY_SERVICES_SETTINGS_DENIED -> "GOOGLE_PLAY_SERVICES_SETTINGS_DENIED"
                FailType.VIEW_DETACHED -> "VIEW_DETACHED"
                FailType.VIEW_NOT_REQUIRED_TYPE -> "VIEW_NOT_REQUIRED_TYPE"
                else -> "UNKNOWN"
            }
            App.instance.logE("onLocationFailed $t")
        }

        override fun onPermissionGranted(alreadyHadPermission: Boolean) {

        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            App.instance.logD("onProviderEnabled $provider")
        }

        override fun onProviderEnabled(provider: String?) {
            App.instance.logD("onProviderEnabled $provider")
        }

        override fun onProviderDisabled(provider: String?) {
            App.instance.logD("onProviderDisabled $provider")
        }
    }

    companion object {
        private const val ACTION_RESET = "reset"

        fun start(context: Context) {
            context.startService(Intent(context, LocationService::class.java))
        }

        fun resetLocationManager(context: Context) {
            context.startService(Intent(context, LocationService::class.java).apply {
                action = ACTION_RESET
            })
        }
    }

}
