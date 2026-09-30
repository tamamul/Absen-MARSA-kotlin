package com.marsa.absen.ui.screen.absen

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marsa.absen.data.remote.ApiResult
import com.marsa.absen.data.remote.serverMessage
import com.marsa.absen.data.remote.serverStatus
import com.marsa.absen.data.repository.AbsenRepository
import com.marsa.absen.data.repository.AuthRepository
import com.marsa.absen.data.repository.LocationVerifier
import com.marsa.absen.domain.model.AbsenMode
import com.marsa.absen.domain.model.AbsenRequest
import com.marsa.absen.domain.model.LocationCheck
import com.marsa.absen.util.preparePortraitPhoto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import com.marsa.absen.util.QualityReport
import com.marsa.absen.util.assessPhoto

enum class AbsenStep { LOCATING, LOCATION_ERROR, CAMERA, CHECKING, REVIEW, SUBMITTING, DONE }

data class AbsenUiState(
    val step: AbsenStep = AbsenStep.LOCATING,
    val loc: LocationCheck.Inside? = null,
    val problem: String? = null,
    val problemJarak: Int? = null,
    val verifying: Boolean = false,
    val photo: File? = null,
    val quality: QualityReport? = null,
    val message: String? = null,
    val success: Boolean = false
)

@HiltViewModel
class AbsenViewModel @Inject constructor(
    private val repo: AbsenRepository,
    private val verifier: LocationVerifier,
    private val auth: AuthRepository
) : ViewModel() {

    var state by mutableStateOf(AbsenUiState())
        private set

    private var request: AbsenRequest? = null
    private var started = false

    fun start(req: AbsenRequest) {
        if (started) return
        started = true
        request = req

        val pre = req.pre
        if (pre != null && pre.isFresh()) {
            // Lokasi sudah terverifikasi di beranda: langsung tampilkan kamera,
            // sambil memperbarui lokasi di latar belakang.
            state = AbsenUiState(step = AbsenStep.CAMERA, loc = pre, verifying = true)
            viewModelScope.launch { applyResult(verifier.verify(), silent = true) }
        } else {
            locate()
        }
    }

    /** Dipanggil saat layar absen benar-benar ditutup. */
    fun reset() {
        state.photo?.delete()
        started = false
        request = null
        state = AbsenUiState()
    }

    fun locate() {
        viewModelScope.launch {
            state = AbsenUiState(step = AbsenStep.LOCATING)
            applyResult(verifier.verify(), silent = false)
        }
    }

    private fun applyResult(r: LocationCheck, silent: Boolean) {
        when (r) {
            is LocationCheck.Inside -> {
                val next = if (state.step == AbsenStep.LOCATING ||
                    state.step == AbsenStep.LOCATION_ERROR
                ) AbsenStep.CAMERA else state.step
                state = state.copy(loc = r, verifying = false, problem = null, step = next)
            }

            is LocationCheck.Problem -> {
                // Pembaruan senyap yang gagal karena GPS/jaringan tidak mengganggu pengguna;
                // server tetap memvalidasi saat absen dikirim.
                if (silent && !r.outside) {
                    state = state.copy(verifying = false)
                    return
                }
                if (state.step == AbsenStep.SUBMITTING || state.step == AbsenStep.DONE) return
                state = state.copy(
                    step = AbsenStep.LOCATION_ERROR,
                    problem = r.message,
                    problemJarak = r.jarak,
                    verifying = false
                )
            }

            LocationCheck.NoPermission -> state = state.copy(
                step = AbsenStep.LOCATION_ERROR,
                problem = "Izin lokasi belum diberikan.",
                verifying = false
            )

            LocationCheck.Unauthorized -> viewModelScope.launch { auth.logout() }

            else -> Unit
        }
    }

        fun onPhotoCaptured(file: File) {
        viewModelScope.launch {
            state = state.copy(step = AbsenStep.CHECKING, message = null)
            val processed = preparePortraitPhoto(file)
            val report = assessPhoto(processed)
            state = if (report.ok) {
                state.copy(step = AbsenStep.REVIEW, photo = processed, quality = report, message = null)
            } else {
                processed.delete()
                state.copy(
                    step = AbsenStep.CAMERA,
                    photo = null,
                    quality = null,
                    message = "Foto ditolak: " + report.problems.joinToString("; ") + ". Coba lagi."
                )
            }
        }
    }

    fun retake() {
        state.photo?.delete()
        state = state.copy(step = AbsenStep.CAMERA, photo = null, quality = null, message = null)
    }

    fun backToReview() {
        state = state.copy(step = AbsenStep.REVIEW, message = null)
    }

    fun submit() {
        val req = request ?: return
        val photo = state.photo ?: return
        val loc = state.loc ?: return

        viewModelScope.launch {
            state = state.copy(step = AbsenStep.SUBMITTING, message = null)
            when (val r = repo.kirim(req.mode, loc.lat, loc.lng, photo)) {
                is ApiResult.Success -> {
                    val ok = r.data.serverStatus() != false
                    val defaultOk =
                        if (req.mode == AbsenMode.MASUK) "Absen masuk berhasil." else "Absen pulang berhasil."
                    state = state.copy(
                        step = AbsenStep.DONE,
                        success = ok,
                        message = r.data.serverMessage()
                            ?: if (ok) defaultOk else "Absen ditolak oleh server."
                    )
                }

                is ApiResult.Error -> {
                    if (r.code == 401) {
                        auth.logout()
                        return@launch
                    }
                    state = state.copy(step = AbsenStep.DONE, success = false, message = r.message)
                }
            }
        }
    }
}
