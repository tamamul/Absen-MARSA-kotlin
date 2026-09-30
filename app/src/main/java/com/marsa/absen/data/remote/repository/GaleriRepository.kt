package com.marsa.absen.data.repository

import com.marsa.absen.data.remote.ApiResult
import com.marsa.absen.data.remote.MarsaApi
import com.marsa.absen.data.remote.apiCall
import com.marsa.absen.domain.model.GaleriItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GaleriRepository @Inject constructor(
    private val api: MarsaApi,
    private val json: Json
) {
    /** [tanggal] format yyyy-MM-dd. Hasil diurutkan dari yang paling pagi absen masuk. */
    suspend fun galeri(tanggal: String): ApiResult<List<GaleriItem>> =
        when (val r = apiCall { api.galeri(tanggal) }) {
            is ApiResult.Error ->
                if (r.code == 404) ApiResult.Success(emptyList()) else r

            is ApiResult.Success -> {
                val array = (r.data as? JsonObject)?.get("data") as? JsonArray
                val list = array.orEmpty()
                    .mapNotNull { element ->
                        runCatching {
                            json.decodeFromJsonElement(GaleriItem.serializer(), element)
                        }.getOrNull()
                    }
                    .sortedBy { it.jamMasuk.orEmpty() }
                ApiResult.Success(list)
            }
        }
}
