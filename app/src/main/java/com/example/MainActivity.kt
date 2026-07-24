package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.ui.navigation.TaxiMeterNavGraph
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MeterViewModel
import com.example.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels()
    private val meterViewModel: MeterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // The inner padding is handled elegantly inside screens on components, 
                    // and top app bars automatically handle safe content boundaries.
                    TaxiMeterNavGraph(
                        meterViewModel = meterViewModel,
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
    }
}
