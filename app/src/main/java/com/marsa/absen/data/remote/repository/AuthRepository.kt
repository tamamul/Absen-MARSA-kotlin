package com.marsa.absen.data.repository

import com.marsa.absen.data.local.TokenStore
import com.marsa.absen.data.remote.ApiResult
import com.marsa.absen.data.remote.MarsaApi
import com.marsa.absen.data.remote.apiCall
import com.marsa.absen.domain.model.LoginResponse
import com.marsa.absen.domain.model.UserDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: MarsaApi,
    private val store: TokenStore,
    private val json: Json
) {

    suspend fun login(login: String, password: String): ApiResult<Unit> {
        val result = apiCall(unauthorizedMessage = "Username atau password salah.") {
            api.login(buildJsonObject {
                put("login", login)
                put("password", password)
            })
        }
        if (result is ApiResult.Error) return result
        result as ApiResult.Success

        val parsed = runCatching {
            json.decodeFromJsonElement(LoginResponse.serializer(), result.data)
        }.getOrNull() ?: return ApiResult.Error("Respons server tidak valid.")

        val token = parsed.data?.token
        if (parsed.status != true || token.isNullOrBlank()) {
            return ApiResult.Error(parsed.message ?: "Login gagal.")
        }

        store.save(
            token = token,
            userJson = json.encodeToString(UserDto.serializer(), parsed.data?.user ?: UserDto())
        )
        return ApiResult.Success(Unit)
    }

    /** Beri tahu server (kalau bisa), lalu apa pun hasilnya hapus token lokal. */
    suspend fun logout() {
        runCatching { api.logout() }
        store.clear()
    }
}
