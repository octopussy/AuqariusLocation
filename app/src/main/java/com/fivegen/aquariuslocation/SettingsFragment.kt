package com.fivegen.aquariuslocation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import com.fivegen.aquariuslocation.ui.theme.AquariusLocationTheme

class SettingsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AquariusLocationTheme {
                    SettingsView(App.storage)
                }
            }
        }
    }
}

@Composable
fun SettingsView(storage: AppStorage) {
    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {

    }
}



@Composable
fun SettingsLineFloat(label: String, value: Float, onChange: (Float) -> Unit) {

}
