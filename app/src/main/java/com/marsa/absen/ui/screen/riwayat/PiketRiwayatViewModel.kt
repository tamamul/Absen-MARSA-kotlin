package com.marsa.absen.ui.screen.riwayat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marsa.absen.data.local.TokenStore
import com.marsa.absen.data.remote.ApiResult
import com.marsa.absen.data.repository.AuthRepository
import com.marsa.absen.data.repository.PiketRiwayatRepository
import com.marsa.absen.domain.model.PiketRiwayatItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PiketRiwayatUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val items: List<PiketRiwayatItem> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class PiketRiwayatViewModel @Inject constructor(
    private val repo: PiketRiwayatRepository,
    private val auth: AuthRepository,
    private val tokenStore: TokenStore
) : ViewModel() {

    var state by mutableStateOf(PiketRiwayatUiState())
        private set

    private var loaded = false

    init {
        viewModelScope.launch {
            tokenStore.token.distinctUntilChanged().collect { token ->
                if (token.isNullOrBlank()) {
                    loaded = false
                    state = PiketRiwayatUiState()
                } else {
                    load()
                }
            }
        }
    }

    fun refresh() = load()

    private fun load() {
        viewModelScope.launch {
            state = state.copy(loading = !loaded, refreshing = loaded, error = null)
            when (val r = repo.riwayat()) {
                is ApiResult.Success -> {
                    loaded = true
                    state = state.copy(loading = false, refreshing = false, items = r.data)
                }

                is ApiResult.Error -> {
                    if (r.code == 401) {
                        auth.logout()
                        return@launch
                    }
                    state = state.copy(loading = false, refreshing = false, error = r.message)
                }
            }
        }
    }
}
