package com.marsa.absen.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marsa.absen.domain.model.AbsenMode
import com.marsa.absen.domain.model.AbsenRequest
import com.marsa.absen.domain.model.LocationCheck
import com.marsa.absen.domain.model.PegawaiProfil
import com.marsa.absen.ui.screen.absen.AbsenScreen
import com.marsa.absen.ui.screen.home.HomeScreen
import com.marsa.absen.ui.screen.login.LoginScreen
import com.marsa.absen.ui.screen.riwayat.RiwayatScreen

@Composable
fun MarsaRoot(session: SessionViewModel = hiltViewModel()) {
    val state by session.state.collectAsStateWithLifecycle()
    var absenRequest by remember { mutableStateOf<AbsenRequest?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }

    // Jika logout terjadi saat layar absen terbuka, tutup layar absen
    LaunchedEffect(state) {
        if (state != SessionState.LoggedIn) absenRequest = null
    }

    Crossfade(targetState = state, label = "session") { s ->
        when (s) {
            SessionState.Loading -> SplashScreen()
            SessionState.LoggedOut -> LoginScreen()
            SessionState.LoggedIn -> {
                val req = absenRequest
                if (req == null) {
                    MainScaffold(
                        onLogout = session::logout,
                        onAbsen = { mode, profil, pre ->
                            absenRequest = AbsenRequest(mode, profil, pre)
                        },
                        refreshKey = refreshKey
                    )
                } else {
                    AbsenScreen(
                        request = req,
                        onClose = { success ->
                            if (success) refreshKey++
                            absenRequest = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MainScaffold(
    onLogout: () -> Unit,
    onAbsen: (AbsenMode, PegawaiProfil, LocationCheck.Inside?) -> Unit,
    refreshKey: Int
) {
    var tab by rememberSaveable { mutableStateOf(0) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars,
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text("Beranda") }
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
                    label = { Text("Riwayat") }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (tab) {
                0 -> HomeScreen(onLogout = onLogout, onAbsen = onAbsen, refreshKey = refreshKey)
                else -> RiwayatScreen(refreshKey = refreshKey)
            }
        }
    }
}

@Composable
private fun SplashScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
