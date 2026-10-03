package com.marsa.absen.data.remote

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.serialization.json.JsonArray

sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>
}

suspend fun apiCall(
    unauthorizedMessage: String? = null,
    block: suspend () -> Response<JsonElement>
): ApiResult<JsonElement> = try {
    val res = block()
    if (res.isSuccessful) {
        ApiResult.Success(res.body() ?: JsonNull)
    } else {
        ApiResult.Error(
            message = errorMessage(res.code(), res.errorBody()?.string(), unauthorizedMessage),
            code = res.code()
        )
    }
} catch (e: CancellationException) {
    throw e
} catch (e: SocketTimeoutException) {
    ApiResult.Error("Koneksi timeout. Periksa jaringan Anda.")
} catch (e: IOException) {
    ApiResult.Error("Tidak dapat terhubung ke server.\nPastikan internet aktif.")
} catch (e: SerializationException) {
    ApiResult.Error("Respons server tidak valid.")
} catch (e: Exception) {
    ApiResult.Error("Terjadi kesalahan: ${e.localizedMessage}")
}

private fun errorMessage(code: Int, body: String?, unauthorized: String?): String {
    val fromBody = serverText(body)
    return when (code) {
        401 -> unauthorized ?: fromBody ?: "Sesi berakhir. Silakan login ulang."
        500 -> "Server sedang bermasalah. Coba lagi nanti."
        422 -> fromBody ?: "Data tidak valid. Periksa kembali."
        else -> fromBody ?: "Server error $code"
    }
}

/** Ambil teks pesan dari bentuk respons CodeIgniter: "message" atau "messages". */
private fun serverText(body: String?): String? {
    val obj = try {
        Json.parseToJsonElement(body ?: "") as? JsonObject
    } catch (_: Exception) {
        null
    } ?: return null

    (obj["message"] as? JsonPrimitive)?.contentOrNull?.let { return it }
    return when (val m = obj["messages"]) {
        is JsonPrimitive -> m.contentOrNull
        is JsonObject -> m.values.firstNotNullOfOrNull { (it as? JsonPrimitive)?.contentOrNull }
        is JsonArray -> m.firstNotNullOfOrNull { (it as? JsonPrimitive)?.contentOrNull }
        else -> null
    }
}
