package com.marsa.absen.data.repository

import com.marsa.absen.data.remote.ApiResult
import com.marsa.absen.data.remote.MarsaApi
import com.marsa.absen.data.remote.apiCall
import com.marsa.absen.domain.model.PiketRiwayatItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PiketRiwayatRepository @Inject constructor(
    private val api: MarsaApi,
    private val json: Json
) {
    suspend fun riwayat(limit: Int = 200): ApiResult<List<PiketRiwayatItem>> =
        when (val r = apiCall { api.piketRiwayat(limit) }) {
            is ApiResult.Error -> r
            is ApiResult.Success -> {
                val array = (r.data as? JsonObject)?.get("data") as? JsonArray
                val list = array.orEmpty().mapNotNull { element ->
                    runCatching {
                        json.decodeFromJsonElement(PiketRiwayatItem.serializer(), element)
                    }.getOrNull()
                }
                ApiResult.Success(list)
            }
        }
}
