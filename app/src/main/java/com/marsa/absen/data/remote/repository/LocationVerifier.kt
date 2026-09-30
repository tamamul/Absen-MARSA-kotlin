package com.marsa.absen.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.marsa.absen.data.remote.ApiResult
import com.marsa.absen.data.remote.bool
import com.marsa.absen.data.remote.int
import com.marsa.absen.data.remote.serverStatus
import com.marsa.absen.data.remote.str
import com.marsa.absen.domain.model.LocationCheck
import com.marsa.absen.util.currentLocation
import com.marsa.absen.util.isFake
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

/** Ambil lokasi GPS lalu verifikasi ke server (absen/cek-lokasi). */
@Singleton
class LocationVerifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: AbsenRepository
) {

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    suspend fun verify(): LocationCheck {
        if (!hasPermission()) return LocationCheck.NoPermission

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!LocationManagerCompat.isLocationEnabled(lm)) {
            return LocationCheck.Problem("Lokasi HP sedang mati. Aktifkan GPS lalu coba lagi.")
        }

        val loc = withTimeoutOrNull(20_000) { context.currentLocation() }
            ?: return LocationCheck.Problem(
                "Gagal mendapatkan lokasi. Pastikan GPS aktif dan sinyal baik, lalu coba lagi."
            )
        if (loc.isFake) {
            return LocationCheck.Problem(
                "Terdeteksi lokasi palsu (mock location). Matikan aplikasi lokasi palsu lalu coba lagi."
            )
        }

        return when (val r = repo.cekLokasi(loc.latitude, loc.longitude)) {
            is ApiResult.Error ->
                if (r.code == 401) LocationCheck.Unauthorized else LocationCheck.Problem(r.message)

            is ApiResult.Success -> parse(loc, r.data)
        }
    }

    private fun parse(loc: Location, data: JsonElement): LocationCheck {
        val obj = data as? JsonObject ?: return LocationCheck.Problem("Respons server tidak valid.")
        val jarak = obj.int("jarak")
        val message = obj.str("message")
        // Server mengirim status=true walau di luar area; yang menentukan adalah "didalam".
        val ok = obj.serverStatus() != false && obj.bool("didalam") != false
        return if (ok) {
            LocationCheck.Inside(
                lat = loc.latitude,
                lng = loc.longitude,
                accuracy = loc.accuracy,
                jarak = jarak,
                radius = obj.str("radius"),
                lokasi = obj.str("lokasi"),
                message = message
            )
        } else {
            LocationCheck.Problem(
                message = message ?: "Anda berada di luar area absen.",
                jarak = jarak,
                outside = true
            )
        }
    }
}
