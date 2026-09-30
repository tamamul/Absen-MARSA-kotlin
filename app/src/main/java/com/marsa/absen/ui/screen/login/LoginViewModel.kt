package com.marsa.absen.ui.screen.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marsa.absen.data.remote.ApiResult
import com.marsa.absen.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val loading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val auth: AuthRepository
) : ViewModel() {

    var state by mutableStateOf(LoginUiState())
        private set

    fun login(login: String, password: String) {
        if (state.loading) return
        viewModelScope.launch {
            state = LoginUiState(loading = true)
            // Kalau sukses, token tersimpan dan SessionViewModel otomatis pindah ke layar utama.
            state = when (val r = auth.login(login, password)) {
                is ApiResult.Success -> LoginUiState()
                is ApiResult.Error -> LoginUiState(error = r.message)
            }
        }
    }

    fun dismissError() {
        state = state.copy(error = null)
    }
}
