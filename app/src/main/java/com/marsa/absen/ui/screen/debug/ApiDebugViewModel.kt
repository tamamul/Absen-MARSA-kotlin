package com.marsa.absen.ui.screen.debug

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marsa.absen.data.local.TokenStore
import com.marsa.absen.data.remote.ApiResult
import com.marsa.absen.data.remote.MarsaApi
import com.marsa.absen.data.remote.apiCall
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class ApiDebugViewModel @Inject constructor(
    private val api: MarsaApi,
    private val store: TokenStore
) : ViewModel() {

    var output by mutableStateOf("Belum ada permintaan.")
        private set
    var loading by mutableStateOf(false)
        private set

    val tokenSaved: StateFlow<Boolean> = store.token
        .map { !it.isNullOrBlank() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val pretty = Json { prettyPrint = true }

    fun login(login: String, password: String) =
        request("POST auth/login", "Username atau password salah.", onSuccess = { saveTokenFrom(it) }) {
            api.login(buildJsonObject {
                put("login", login)
                put("password", password)
            })
        }

    fun profile() = request("GET auth/profile") { api.profile() }
    fun pegawaiProfil() = request("GET pegawai/profil") { api.pegawaiProfil() }
    fun absenHariIni() = request("GET absen/hari-ini") { api.absenHariIni() }
    fun riwayat() = request("GET absen/riwayat?limit=365") { api.riwayat(365) }

    fun logout() = request("POST auth/logout", onSuccess = {
        store.clear()
        "Token lokal dihapus."
    }) { api.logout() }

    fun clearToken() {
        viewModelScope.launch {
            store.clear()
            output = "Token lokal dihapus."
        }
    }

    private fun request(
        label: String,
        unauthorizedMessage: String? = null,
        onSuccess: suspend (JsonElement) -> String? = { null },
        block: suspend () -> Response<JsonElement>
    ) {
        viewModelScope.launch {
            loading = true
            output = when (val r = apiCall(unauthorizedMessage, block)) {
                is ApiResult.Success -> {
                    val note = onSuccess(r.data)
                    buildString {
                        append(label).append("\n\n")
                        append(pretty.encodeToString(JsonElement.serializer(), r.data))
                        if (note != null) append("\n\n— ").append(note)
                    }
                }
                is ApiResult.Error -> "$label\n\nERROR ${r.code ?: ""}: ${r.message}"
            }
            loading = false
        }
    }

    private suspend fun saveTokenFrom(e: JsonElement): String {
        val token = findToken(e)
        return if (token != null) {
            store.save(token, e.toString())
            "Token tersimpan."
        } else {
            "Token TIDAK ditemukan di respons (cek nama field-nya)."
        }
    }

    // Mencari token di root atau di dalam "data"
    private fun findToken(e: JsonElement): String? {
        val root = e as? JsonObject ?: return null
        val scopes = listOfNotNull(root, root["data"] as? JsonObject)
        for (scope in scopes) {
            for (key in listOf("token", "api_token", "access_token")) {
                val value = (scope[key] as? JsonPrimitive)?.contentOrNull
                if (!value.isNullOrBlank()) return value
            }
        }
        return null
    }
}
