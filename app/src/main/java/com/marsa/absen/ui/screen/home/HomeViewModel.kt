package com.marsa.absen.ui.screen.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marsa.absen.data.local.TokenStore
import com.marsa.absen.data.remote.ApiResult
import com.marsa.absen.data.repository.AuthRepository
import com.marsa.absen.data.repository.HomeRepository
import com.marsa.absen.data.repository.LocationVerifier
import com.marsa.absen.domain.model.AbsenHariIni
import com.marsa.absen.domain.model.LocationCheck
import com.marsa.absen.domain.model.PegawaiProfil
import com.marsa.absen.util.cleanDate
import com.marsa.absen.util.cleanTime
import com.marsa.absen.util.toLocalTimeOrNull
import com.marsa.absen.util.todayIso
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject
import com.marsa.absen.data.repository.PiketRepository
import com.marsa.absen.domain.model.PiketStatus

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
        get() = hariIni?.takeIf { it.tanggalMasuk.cleanDate() == todayIso() }

    val status: AbsenStatus
        get() = when {
            absenHariIni?.jamMasuk.cleanTime() == null -> AbsenStatus.BELUM_MASUK
            absenHariIni?.jamKeluar.cleanTime() == null -> AbsenStatus.SUDAH_MASUK
            else -> AbsenStatus.SELESAI
        }

    /**
     * Mode "tegas": pulang baru dibuka setelah jam pulang.
     * (Asumsi arti "tegas"; ubah di sini jika aturan sekolah berbeda.)
     */
    fun pulangDibuka(now: LocalTime): Boolean {
        val bukaJam = profil?.jamPulang.toLocalTimeOrNull()
        val tegas = profil?.modeAbsen.equals("tegas", ignoreCase = true)
        return !tegas || bukaJam == null || !now.isBefore(bukaJam)
    }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: HomeRepository,
    private val auth: AuthRepository,
    private val tokenStore: TokenStore,
    private val verifier: LocationVerifier,
    private val piketRepo: PiketRepository
) : ViewModel() {

    var state by mutableStateOf(HomeUiState())
        private set

    var location by mutableStateOf<LocationCheck>(LocationCheck.Idle)
    var piket by mutableStateOf<PiketStatus?>(null)
        private set

    private var locationJob: Job? = null

    init {
        viewModelScope.launch {
            tokenStore.token.distinctUntilChanged().collect { token ->
                if (token.isNullOrBlank()) {
                    locationJob?.cancel()
                    state = HomeUiState()
                    location = LocationCheck.Idle
                    piket = null
                } else {
                    load()
                }
            }
        }
    }

    fun refresh() = load()

    fun refreshPiket() {
        viewModelScope.launch {
            when (val r = piketRepo.status()) {
                is ApiResult.Success -> piket = r.data
                is ApiResult.Error -> if (r.code == 401) auth.logout()
                // gangguan jaringan lain: pertahankan tampilan terakhir
            }
        }
    }

    fun hasLocationPermission(): Boolean = verifier.hasPermission()

    fun checkLocation() {
        if (locationJob?.isActive == true) return
        locationJob = viewModelScope.launch {
            location = LocationCheck.Checking
            val result = verifier.verify()
            if (result is LocationCheck.Unauthorized) {
                auth.logout()
                return@launch
            }
            location = result
        }
    }

    /** Dipanggil saat layar kembali tampil: periksa ulang jika hasil sudah basi. */
    fun checkLocationIfStale() {
        val stale = when (val cur = location) {
            is LocationCheck.Inside -> !cur.isFresh(60_000)
            LocationCheck.Checking -> false
            else -> true
        }
        if (stale) checkLocation()
    }

    private fun load() {
        refreshPiket()
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
                auth.logout()
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
