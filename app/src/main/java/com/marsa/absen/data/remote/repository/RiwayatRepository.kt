package com.marsa.absen.data.repository

import com.marsa.absen.data.remote.ApiResult
import com.marsa.absen.data.remote.MarsaApi
import com.marsa.absen.data.remote.apiCall
import com.marsa.absen.domain.model.AbsenHariIni
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RiwayatRepository @Inject constructor(
    private val api: MarsaApi,
    private val json: Json
) {
    suspend fun riwayat(limit: Int = 365): ApiResult<List<AbsenHariIni>> =
        when (val r = apiCall { api.riwayat(limit) }) {
            is ApiResult.Error -> r
            is ApiResult.Success -> {
                val array = (r.data as? JsonObject)?.get("data") as? JsonArray
                val list = array.orEmpty().mapNotNull { element ->
                    runCatching {
                        json.decodeFromJsonElement(AbsenHariIni.serializer(), element)
                    }.getOrNull()
                }
                ApiResult.Success(list)
            }
        }
}
