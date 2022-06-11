package com.fivegen.aquariuslocation

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.fivegen.aquariuslocation.ui.theme.AquariusLocationTheme


class MainActivity : AppCompatActivity() {

    private val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { p ->
        if (p.values.all { it == true }) {
            startLocationService()
        } else {
            Toast.makeText(this, "We need permissions", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AquariusLocationTheme {
                MainView(App.instance)
            }
        }

        checkPermissions()
    }

    private fun startLocationService() {
        LocationService.start(this)
    }

    private fun checkPermissions() {
        when {
            permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED } -> {
                startLocationService()
            }
            else -> {
                requestPermissionLauncher.launch(permissions)
            }
        }
    }
}

@Composable
fun MainView(app: App) {
    var isSettingsVisible by remember { mutableStateOf(false) }
    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedButton(onClick = { isSettingsVisible = true }) {
            Text(text = "Settings")
        }

        var log by remember { mutableStateOf("") }
        LaunchedEffect(app) {
            app.recentLog.collect { log += "$it\n" }
        }

        Text(modifier = Modifier.weight(1f), text = log, fontSize = 12.sp)
    }

    AnimatedVisibility(visible = isSettingsVisible,
        enter = slideIn { IntOffset(it.width, 0) },
        exit = slideOut { IntOffset(it.width, 0) }
    ) {
        SettingsScreen(onClose = {
            isSettingsVisible = false
        })
    }
}

@Composable
fun SettingsScreen(onClose: () -> Unit) {
    val storage = App.storage
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

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
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
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(onClick = onClose) {
                Text(text = "Close")
            }
        }
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
private fun Line(label: String, value: String, keyboardType: KeyboardType, onChange: (String) -> Unit) {
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
