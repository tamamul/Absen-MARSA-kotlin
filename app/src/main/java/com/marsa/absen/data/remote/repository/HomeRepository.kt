package com.marsa.absen.data.repository

import com.marsa.absen.data.remote.ApiResult
import com.marsa.absen.data.remote.MarsaApi
import com.marsa.absen.data.remote.apiCall
import com.marsa.absen.domain.model.AbsenHariIni
import com.marsa.absen.domain.model.PegawaiProfil
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeRepository @Inject constructor(
    private val api: MarsaApi,
    private val json: Json
) {

    suspend fun profil(): ApiResult<PegawaiProfil> =
        when (val r = apiCall { api.pegawaiProfil() }) {
            is ApiResult.Error -> r
            is ApiResult.Success -> decodeData(r.data, PegawaiProfil.serializer())
                ?.let { ApiResult.Success(it) }
                ?: ApiResult.Error("Data profil tidak ditemukan.")
        }

    /** Sukses dengan data null = belum ada absen hari ini. */
    suspend fun hariIni(): ApiResult<AbsenHariIni?> =
        when (val r = apiCall { api.absenHariIni() }) {
            is ApiResult.Error ->
                if (r.code == 404) ApiResult.Success(null) else r
            is ApiResult.Success ->
                ApiResult.Success(decodeData(r.data, AbsenHariIni.serializer()))
        }

    /** Ambil objek "data"; null jika data bukan objek (null / array kosong / tidak ada). */
    private fun <T> decodeData(root: JsonElement, serializer: KSerializer<T>): T? {
        val data = (root as? JsonObject)?.get("data") as? JsonObject ?: return null
        return runCatching { json.decodeFromJsonElement(serializer, data) }.getOrNull()
    }
}
