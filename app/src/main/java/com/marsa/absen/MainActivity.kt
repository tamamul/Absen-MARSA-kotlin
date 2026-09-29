package com.marsa.absen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.marsa.absen.ui.screen.debug.ApiDebugScreen
import com.marsa.absen.ui.theme.MarsaTheme
import com.marsa.absen.ui.theme.ThemeMode
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MarsaTheme(
                themeMode = ThemeMode.SYSTEM,
                dynamicColor = true,
                amoled = false
            ) {
                Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
                    ApiDebugScreen(modifier = Modifier.padding(padding))
                }
            }
        }
    }
}
