package com.marsa.absen.ui.screen.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marsa.absen.data.remote.ApiResult
import com.marsa.absen.data.repository.AuthRepository
import com.marsa.absen.data.repository.HomeRepository
import com.marsa.absen.domain.model.AbsenHariIni
import com.marsa.absen.domain.model.PegawaiProfil
import com.marsa.absen.util.todayIso
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.marsa.absen.data.local.TokenStore
import kotlinx.coroutines.flow.distinctUntilChanged

enum class AbsenStatus { BELUM_MASUK, SUDAH_MASUK, SELESAI }

data class HomeUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val profil: PegawaiProfil? = null,
    val hariIni: AbsenHariIni? = null,
    val error: String? = null
) {
    /** Hanya dianggap "hari ini" jika tanggalnya sama dengan tanggal HP. */
    val absenHariIni: AbsenHariIni?
        get() = hariIni?.takeIf { it.tanggalMasuk == todayIso() }

    val status: AbsenStatus
        get() = when {
            absenHariIni?.jamMasuk.isNullOrBlank() -> AbsenStatus.BELUM_MASUK
            absenHariIni?.jamKeluar.isNullOrBlank() -> AbsenStatus.SUDAH_MASUK
            else -> AbsenStatus.SELESAI
        }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: HomeRepository,
    private val auth: AuthRepository,
    private val tokenStore: TokenStore
) : ViewModel() {

    var state by mutableStateOf(HomeUiState())
        private set

    init {
        viewModelScope.launch {
            tokenStore.token.distinctUntilChanged().collect { token ->
                if (token.isNullOrBlank()) state = HomeUiState() else load()
            }
        }
    }

    fun refresh() = load()

    private fun load() {
        viewModelScope.launch {
            val firstLoad = state.profil == null
            state = state.copy(loading = firstLoad, refreshing = !firstLoad, error = null)

            val (p, h) = coroutineScope {
                val profil = async { repo.profil() }
                val hariIni = async { repo.hariIni() }
                profil.await() to hariIni.await()
            }

            val unauthorized = (p as? ApiResult.Error)?.code == 401 ||
                (h as? ApiResult.Error)?.code == 401
            if (unauthorized) {
                auth.logout() // token ditolak server -> kembali ke layar login
                return@launch
            }

            state = state.copy(
                loading = false,
                refreshing = false,
                profil = when (p) {
                    is ApiResult.Success -> p.data
                    is ApiResult.Error -> state.profil
                },
                hariIni = when (h) {
                    is ApiResult.Success -> h.data
                    is ApiResult.Error -> state.hariIni
                },
                error = (p as? ApiResult.Error)?.message ?: (h as? ApiResult.Error)?.message
            )
        }
    }
}
