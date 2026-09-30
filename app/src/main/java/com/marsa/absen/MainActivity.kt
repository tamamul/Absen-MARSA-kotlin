package com.marsa.absen

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.marsa.absen.ui.MarsaRoot
import com.marsa.absen.ui.SessionState
import com.marsa.absen.ui.SessionViewModel
import com.marsa.absen.ui.theme.MarsaTheme
import com.marsa.absen.ui.theme.ThemeMode
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val session: SessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Wajib dipanggil sebelum super.onCreate()
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Tahan splash sampai status login diketahui (maksimal 2,5 detik)
        val start = SystemClock.elapsedRealtime()
        splash.setKeepOnScreenCondition {
            session.state.value is SessionState.Loading &&
                SystemClock.elapsedRealtime() - start < 2_500
        }

        setContent {
            MarsaTheme(
                themeMode = ThemeMode.SYSTEM,
                dynamicColor = true,
                amoled = false
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MarsaRoot()
                }
            }
        }
    }
}
