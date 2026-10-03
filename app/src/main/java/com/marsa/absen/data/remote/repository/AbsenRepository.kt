package com.marsa.absen.data.repository

import com.marsa.absen.data.remote.ApiResult
import com.marsa.absen.data.remote.MarsaApi
import com.marsa.absen.data.remote.apiCall
import com.marsa.absen.domain.model.AbsenMode
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AbsenRepository @Inject constructor(
    private val api: MarsaApi
) {

    suspend fun cekLokasi(lat: Double, lng: Double, piket: Boolean = false): ApiResult<JsonElement> =
        apiCall {
            val body = buildJsonObject {
                put("latitude", lat.toString())
                put("longitude", lng.toString())
            }
            if (piket) api.cekLokasiPiket(body) else api.cekLokasi(body)
        }

    suspend fun kirim(
        mode: AbsenMode,
        lat: Double,
        lng: Double,
        foto: File,
        piket: Boolean = false
    ): ApiResult<JsonElement> {
        val text = "text/plain".toMediaType()
        val latBody = lat.toString().toRequestBody(text)
        val lngBody = lng.toString().toRequestBody(text)
        val filename = if (mode == AbsenMode.MASUK) "foto_masuk.jpg" else "foto_keluar.jpg"
        val part = MultipartBody.Part.createFormData(
            name = "foto",
            filename = filename,
            body = foto.asRequestBody("image/jpeg".toMediaType())
        )
        return apiCall {
            when {
                piket && mode == AbsenMode.MASUK -> api.piketMasuk(latBody, lngBody, part)
                piket -> api.piketKeluar(latBody, lngBody, part)
                mode == AbsenMode.MASUK -> api.absenMasuk(latBody, lngBody, part)
                else -> api.absenKeluar(latBody, lngBody, part)
            }
        }
    }
}
