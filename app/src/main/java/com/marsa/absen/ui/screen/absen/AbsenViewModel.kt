package com.marsa.absen.ui.screen.absen

import android.content.Context
import android.location.LocationManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marsa.absen.data.remote.ApiResult
import com.marsa.absen.data.remote.serverMessage
import com.marsa.absen.data.remote.serverStatus
import com.marsa.absen.data.repository.AbsenRepository
import com.marsa.absen.data.repository.AuthRepository
import com.marsa.absen.domain.model.AbsenMode
import com.marsa.absen.domain.model.AbsenRequest
import com.marsa.absen.util.compressPhoto
import com.marsa.absen.util.currentLocation
import com.marsa.absen.util.distanceMeters
import com.marsa.absen.util.isFake
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.io.File
import javax.inject.Inject

enum class AbsenStep { LOCATING, LOCATION_ERROR, CAMERA, REVIEW, SUBMITTING, DONE }

data class AbsenUiState(
    val step: AbsenStep = AbsenStep.LOCATING,
    val lat: Double? = null,
    val lng: Double? = null,
    val accuracy: Float? = null,
    val distance: Float? = null,
    val radius: Double? = null,
    val photo: File? = null,
    val message: String? = null,
    val success: Boolean = false,
    val cekRaw: String? = null,
    val resultRaw: String? = null
)

@HiltViewModel
class AbsenViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: AbsenRepository,
    private val auth: AuthRepository
) : ViewModel() {

    var state by mutableStateOf(AbsenUiState())
        private set

    private var request: AbsenRequest? = null
    private var started = false
    private val pretty = Json { prettyPrint = true }

    fun start(req: AbsenRequest) {
        if (started) return
        started = true
        request = req
        locate()
    }

    /** Dipanggil saat layar absen benar-benar ditutup. */
    fun reset() {
        state.photo?.delete()
        started = false
        request = null
        state = AbsenUiState()
    }

    fun locate() {
        val req = request ?: return
        viewModelScope.launch {
            state = AbsenUiState(step = AbsenStep.LOCATING)

            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (!LocationManagerCompat.isLocationEnabled(lm)) {
                state = AbsenUiState(
                    step = AbsenStep.LOCATION_ERROR,
                    message = "Lokasi HP sedang mati. Aktifkan GPS lalu coba lagi."
                )
                return@launch
            }

            val loc = withTimeoutOrNull(20_000) { context.currentLocation() }
            if (loc == null) {
                state = AbsenUiState(
                    step = AbsenStep.LOCATION_ERROR,
                    message = "Gagal mendapatkan lokasi. Pastikan GPS aktif dan sinyal baik, lalu coba lagi."
                )
                return@launch
            }
            if (loc.isFake) {
                state = AbsenUiState(
                    step = AbsenStep.LOCATION_ERROR,
                    message = "Terdeteksi lokasi palsu (mock location). Matikan aplikasi lokasi palsu lalu coba lagi."
                )
                return@launch
            }

            val tLat = req.profil.latitude?.toDoubleOrNull()
            val tLng = req.profil.longitude?.toDoubleOrNull()
            val distance = if (tLat != null && tLng != null) {
                distanceMeters(loc.latitude, loc.longitude, tLat, tLng)
            } else null

            val base = AbsenUiState(
                lat = loc.latitude,
                lng = loc.longitude,
                accuracy = loc.accuracy,
                distance = distance,
                radius = req.profil.radius?.toDoubleOrNull()
            )

            when (val cek = repo.cekLokasi(loc.latitude, loc.longitude)) {
                is ApiResult.Error -> {
                    if (cek.code == 401) {
                        auth.logout()
                        return@launch
                    }
                    state = base.copy(step = AbsenStep.LOCATION_ERROR, message = cek.message)
                }

                is ApiResult.Success -> {
                    val raw = pretty.encodeToString(JsonElement.serializer(), cek.data)
                    state = if (cek.data.serverStatus() == false) {
                        base.copy(
                            step = AbsenStep.LOCATION_ERROR,
                            message = cek.data.serverMessage() ?: "Lokasi di luar area absen.",
                            cekRaw = raw
                        )
                    } else {
                        base.copy(step = AbsenStep.CAMERA, cekRaw = raw)
                    }
                }
            }
        }
    }

    fun onPhotoCaptured(file: File) {
        viewModelScope.launch {
            val processed = compressPhoto(file)
            state = state.copy(step = AbsenStep.REVIEW, photo = processed, message = null)
        }
    }

    fun onCameraError(message: String) {
        state = state.copy(message = message)
    }

    fun retake() {
        state.photo?.delete()
        state = state.copy(step = AbsenStep.CAMERA, photo = null, message = null)
    }

    fun backToReview() {
        state = state.copy(step = AbsenStep.REVIEW, message = null)
    }

    fun submit() {
        val req = request ?: return
        val photo = state.photo ?: return
        val lat = state.lat ?: return
        val lng = state.lng ?: return

        viewModelScope.launch {
            state = state.copy(step = AbsenStep.SUBMITTING, message = null)
            when (val r = repo.kirim(req.mode, lat, lng, photo)) {
                is ApiResult.Success -> {
                    val ok = r.data.serverStatus() != false
                    val defaultOk =
                        if (req.mode == AbsenMode.MASUK) "Absen masuk berhasil." else "Absen pulang berhasil."
                    state = state.copy(
                        step = AbsenStep.DONE,
                        success = ok,
                        message = r.data.serverMessage()
                            ?: if (ok) defaultOk else "Absen ditolak oleh server.",
                        resultRaw = pretty.encodeToString(JsonElement.serializer(), r.data)
                    )
                }

                is ApiResult.Error -> {
                    if (r.code == 401) {
                        auth.logout()
                        return@launch
                    }
                    state = state.copy(
                        step = AbsenStep.DONE,
                        success = false,
                        message = r.message,
                        resultRaw = null
                    )
                }
            }
        }
    }
}
