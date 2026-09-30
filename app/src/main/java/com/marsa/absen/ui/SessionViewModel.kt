package com.marsa.absen.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marsa.absen.data.local.TokenStore
import com.marsa.absen.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SessionState {
    data object Loading : SessionState
    data object LoggedIn : SessionState
    data object LoggedOut : SessionState
}

@HiltViewModel
class SessionViewModel @Inject constructor(
    store: TokenStore,
    private val auth: AuthRepository
) : ViewModel() {

    val state: StateFlow<SessionState> = store.token
        .map { token ->
            if (token.isNullOrBlank()) SessionState.LoggedOut else SessionState.LoggedIn
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SessionState.Loading)

    fun logout() {
        viewModelScope.launch { auth.logout() }
    }
}
