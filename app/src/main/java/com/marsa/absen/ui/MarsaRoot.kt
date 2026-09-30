package com.marsa.absen.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marsa.absen.domain.model.AbsenRequest
import com.marsa.absen.ui.screen.absen.AbsenScreen
import com.marsa.absen.ui.screen.home.HomeScreen
import com.marsa.absen.ui.screen.login.LoginScreen

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
                    HomeScreen(
                        onLogout = session::logout,
                        onAbsen = { mode, profil -> absenRequest = AbsenRequest(mode, profil, pre) },
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
private fun SplashScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
