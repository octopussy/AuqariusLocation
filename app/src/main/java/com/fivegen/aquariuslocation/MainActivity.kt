package com.fivegen.aquariuslocation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.fivegen.aquariuslocation.ui.theme.AquariusLocationTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.views.MapView


class MainActivity : AppCompatActivity() {

    companion object {

        private const val ACTION_SHUTDOWN_SERVICE = "shutdown"

        fun getShutdownIntent(context: Context) = Intent(context, MainActivity::class.java).apply {
            action = ACTION_SHUTDOWN_SERVICE
        }
    }

    private lateinit var mapController: MapController
    private lateinit var mapView: MapView

    private val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { p ->
            if (p.values.all { it }) {
                startLocationService()
            } else {
                Toast.makeText(this, "We need permissions", Toast.LENGTH_SHORT).show()
            }
        }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleShutdownIntent(intent)
        if (isFinishing) {
            return
        }

        setContent {
            AquariusLocationTheme {
                MainView(App.instance, mapView)
            }
        }

        val ctx: Context = applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))

        mapView = MapView(this)
        mapController = MapController(mapView, App.instance.locationHistory, lifecycleScope)

        checkPermissions()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleShutdownIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    private fun handleShutdownIntent(intent: Intent?) {
        if (intent?.action == ACTION_SHUTDOWN_SERVICE) {
            LocationService.stop(this)
            finish()
        }
    }

    private fun startLocationService() {
        LocationService.start(this)
    }

    private fun checkPermissions() {
        when {
            permissions.all {
                ContextCompat.checkSelfPermission(
                    this,
                    it
                ) == PackageManager.PERMISSION_GRANTED
            } -> {
                startLocationService()
            }
            else -> {
                requestPermissionLauncher.launch(permissions)
            }
        }
    }
}

@Composable
fun MainView(app: App, mapView: MapView) {

    val coroutineScope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState()

    Scaffold(scaffoldState = scaffoldState,
        drawerGesturesEnabled = scaffoldState.drawerState.isOpen,
        drawerContent = {
            SettingsScreen(
                logSource = app.recentLog,
                onClose = {
                    coroutineScope.launch {
                        scaffoldState.drawerState.close()
                    }
                })
        }) {

        AndroidView(modifier = Modifier.fillMaxSize(), factory = { context -> mapView })

        Box(modifier = Modifier.padding(16.dp)) {
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        scaffoldState.drawerState.open()
                    }
                }) {
                Icon(Icons.Default.Settings, contentDescription = "settings btn")
            }
        }
    }
}

@Composable
fun SettingsScreen(onClose: () -> Unit, logSource: Flow<LogMessage>) {
    var log by remember { mutableStateOf("") }
    val storage = App.storage

    LaunchedEffect(logSource) {
        logSource.collect {
            log += "$it\n"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color.White)
    ) {

        val context = LocalContext.current

        val acceptableTimePeriod = remember { mutableStateOf(storage.acceptableTimePeriod) }
        val requiredTimeInterval = remember { mutableStateOf(storage.requiredTimeInterval) }
        val requiredDistanceInterval = remember { mutableStateOf(storage.requiredDistanceInterval) }
        val acceptableAccuracy = remember { mutableStateOf(storage.acceptableAccuracy) }
        val setWaitPeriodGPS = remember { mutableStateOf(storage.setWaitPeriodGPS) }
        val setWaitPeriodNetwork = remember { mutableStateOf(storage.setWaitPeriodNetwork) }

        SettingsLineLong("acceptableTimePeriod", acceptableTimePeriod)
        SettingsLineLong("requiredTimeInterval", requiredTimeInterval)
        SettingsLineLong("requiredDistanceInterval", requiredDistanceInterval)
        SettingsLineFloat("acceptableAccuracy", acceptableAccuracy)
        SettingsLineLong("setWaitPeriodGPS", setWaitPeriodGPS)
        SettingsLineLong("setWaitPeriodNetwork", setWaitPeriodNetwork)

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = {
                storage.acceptableTimePeriod = acceptableTimePeriod.value
                storage.requiredTimeInterval = requiredTimeInterval.value
                storage.requiredDistanceInterval = requiredDistanceInterval.value
                storage.acceptableAccuracy = acceptableAccuracy.value
                storage.setWaitPeriodGPS = setWaitPeriodGPS.value
                storage.setWaitPeriodNetwork = setWaitPeriodNetwork.value
                LocationService.resetLocationManager(context)
                onClose()
            }) {
                Text(text = "Apply")
            }
        }

        Text(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            text = log,
            fontSize = 12.sp,
            onTextLayout = { l ->
                Log.d("onTextLayout", l.toString())
            }
        )

    }
}

@Composable
private fun SettingsLineLong(label: String, value: MutableState<Long>) {
    Line(label = label, value.value.toString(), keyboardType = KeyboardType.Number) { str ->
        if (str.isBlank()) {
            value.value = 0
        } else {
            runCatching { str.toLong() }.onSuccess { value.value = it }
        }
    }
}

@Composable
private fun SettingsLineFloat(label: String, value: MutableState<Float>) {
    Line(label = label, value.value.toString(), keyboardType = KeyboardType.Decimal) { str ->
        if (str.isBlank()) {
            value.value = 0.0f
        } else {
            runCatching { str.toFloat() }.onSuccess { value.value = it }
        }
    }
}

@Composable
private fun Line(
    label: String,
    value: String,
    keyboardType: KeyboardType,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        value = value,
        label = { Text(text = label) },
        onValueChange = onChange
    )
}
