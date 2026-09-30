package com.marsa.absen.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marsa.absen.ui.screen.home.HomeScreen
import com.marsa.absen.ui.screen.login.LoginScreen

@Composable
fun MarsaRoot(session: SessionViewModel = hiltViewModel()) {
    val state by session.state.collectAsStateWithLifecycle()

    Crossfade(targetState = state, label = "session") { s ->
        when (s) {
            SessionState.Loading -> SplashScreen()
            SessionState.LoggedOut -> LoginScreen()
            SessionState.LoggedIn -> HomeScreen(onLogout = session::logout) 
        }
    }
}

@Composable
private fun SplashScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
