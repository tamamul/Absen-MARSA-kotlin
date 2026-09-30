package com.marsa.absen

import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marsa.absen.data.local.AppSettings
import com.marsa.absen.ui.MarsaRoot
import com.marsa.absen.ui.SessionState
import com.marsa.absen.ui.SessionViewModel
import com.marsa.absen.ui.SettingsViewModel
import com.marsa.absen.ui.theme.MarsaTheme
import com.marsa.absen.ui.theme.ThemeMode
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val session: SessionViewModel by viewModels()
    private val settingsVm: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Wajib dipanggil sebelum super.onCreate()
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Tahan splash sampai status login dan tema terbaca (maksimal 2,5 detik)
        val start = SystemClock.elapsedRealtime()
        splash.setKeepOnScreenCondition {
            (session.state.value is SessionState.Loading || settingsVm.settings.value == null) &&
                SystemClock.elapsedRealtime() - start < 2_500
        }

        setContent {
            val stored by settingsVm.settings.collectAsStateWithLifecycle()
            val settings = stored ?: AppSettings()

            val dark = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // Ikon status bar/navigasi mengikuti tema aplikasi, bukan tema sistem
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                )
                onDispose { }
            }

            MarsaTheme(
                themeMode = settings.themeMode,
                dynamicColor = settings.dynamicColor,
                amoled = settings.amoled
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
