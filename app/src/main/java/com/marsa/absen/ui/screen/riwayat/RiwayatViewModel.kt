package com.marsa.absen.ui.screen.riwayat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marsa.absen.data.local.TokenStore
import com.marsa.absen.data.remote.ApiResult
import com.marsa.absen.data.repository.AuthRepository
import com.marsa.absen.data.repository.HomeRepository
import com.marsa.absen.data.repository.RiwayatRepository
import com.marsa.absen.domain.model.AbsenHariIni
import com.marsa.absen.domain.model.DayRecord
import com.marsa.absen.util.cleanDate
import com.marsa.absen.util.cleanTime
import com.marsa.absen.util.lateMinutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class RiwayatUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val records: Map<LocalDate, DayRecord> = emptyMap(),
    val jadwalMasuk: String? = null,
    val error: String? = null
)

@HiltViewModel
class RiwayatViewModel @Inject constructor(
    private val repo: RiwayatRepository,
    private val home: HomeRepository,
    private val auth: AuthRepository,
    private val tokenStore: TokenStore
) : ViewModel() {

    var state by mutableStateOf(RiwayatUiState())
        private set

    private var loaded = false

    init {
        viewModelScope.launch {
            tokenStore.token.distinctUntilChanged().collect { token ->
                if (token.isNullOrBlank()) {
                    loaded = false
                    state = RiwayatUiState()
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

            val (r, p) = coroutineScope {
                val riwayat = async { repo.riwayat() }
                val profil = async { home.profil() }
                riwayat.await() to profil.await()
            }

            if ((r as? ApiResult.Error)?.code == 401 || (p as? ApiResult.Error)?.code == 401) {
                auth.logout()
                return@launch
            }

            val jadwal = when (p) {
                is ApiResult.Success -> p.data.jamMasuk
                is ApiResult.Error -> state.jadwalMasuk
            }

            state = when (r) {
                is ApiResult.Success -> {
                    loaded = true
                    state.copy(
                        loading = false,
                        refreshing = false,
                        records = build(r.data, jadwal),
                        jadwalMasuk = jadwal,
                        error = null
                    )
                }

                is ApiResult.Error -> state.copy(
                    loading = false,
                    refreshing = false,
                    jadwalMasuk = jadwal,
                    error = r.message
                )
            }
        }
    }

    private fun build(list: List<AbsenHariIni>, jadwal: String?): Map<LocalDate, DayRecord> {
        val today = LocalDate.now()
        val map = LinkedHashMap<LocalDate, DayRecord>()
        for (item in list) {
            val date = item.tanggalMasuk.cleanDate()?.take(10)
                ?.let { s -> runCatching { LocalDate.parse(s) }.getOrNull() }
                ?: continue
            if (item.jamMasuk.cleanTime() == null) continue
            if (date in map) continue
            map[date] = DayRecord(
                date = date,
                item = item,
                lateMinutes = lateMinutes(item.jamMasuk, jadwal),
                lupaPulang = item.jamKeluar.cleanTime() == null && date.isBefore(today)
            )
        }
        return map
    }
}
