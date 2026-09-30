package com.marsa.absen.ui.screen.galeri

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marsa.absen.data.local.TokenStore
import com.marsa.absen.data.remote.ApiResult
import com.marsa.absen.data.repository.AuthRepository
import com.marsa.absen.data.repository.GaleriRepository
import com.marsa.absen.domain.model.GaleriItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class GaleriUiState(
    val date: LocalDate = LocalDate.now(),
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val items: List<GaleriItem> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class GaleriViewModel @Inject constructor(
    private val repo: GaleriRepository,
    private val auth: AuthRepository,
    private val tokenStore: TokenStore
) : ViewModel() {

    var state by mutableStateOf(GaleriUiState())
        private set

    private var loadJob: Job? = null
    private var lastLoad = 0L

    init {
        viewModelScope.launch {
            tokenStore.token.distinctUntilChanged().collect { token ->
                if (token.isNullOrBlank()) {
                    loadJob?.cancel()
                    lastLoad = 0L
                    state = GaleriUiState()
                } else {
                    load(refresh = false)
                }
            }
        }
    }

    fun setDate(date: LocalDate) {
        if (date == state.date) return
        state = state.copy(date = date)
        load(refresh = false)
    }

    fun refresh() = load(refresh = true)

    /** Dipanggil saat tab dibuka: muat ulang jika datanya sudah lebih dari 1 menit. */
    fun onEnter() {
        val idle = !state.loading && !state.refreshing
        if (idle && System.currentTimeMillis() - lastLoad > 60_000) load(refresh = true)
    }

    private fun load(refresh: Boolean) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val date = state.date
            state = state.copy(
                loading = !refresh,
                refreshing = refresh,
                error = null,
                items = if (refresh) state.items else emptyList()
            )
            when (val r = repo.galeri(date.toString())) {
                is ApiResult.Success -> {
                    lastLoad = System.currentTimeMillis()
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
