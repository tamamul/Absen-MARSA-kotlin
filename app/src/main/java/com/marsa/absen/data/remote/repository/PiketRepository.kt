package com.marsa.absen.data.repository

import com.marsa.absen.data.remote.ApiResult
import com.marsa.absen.data.remote.MarsaApi
import com.marsa.absen.data.remote.apiCall
import com.marsa.absen.domain.model.PiketStatus
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PiketRepository @Inject constructor(
    private val api: MarsaApi,
    private val json: Json
) {
    suspend fun status(): ApiResult<PiketStatus> =
        when (val r = apiCall { api.piketStatus() }) {
            is ApiResult.Error -> r
            is ApiResult.Success -> {
                val data = (r.data as? JsonObject)?.get("data") as? JsonObject
                val parsed = data?.let {
                    runCatching { json.decodeFromJsonElement(PiketStatus.serializer(), it) }.getOrNull()
                }
                if (parsed != null) ApiResult.Success(parsed)
                else ApiResult.Error("Data piket tidak valid.")
            }
        }
}
