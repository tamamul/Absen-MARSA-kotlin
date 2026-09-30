package com.marsa.absen.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val status: Boolean? = null,
    val message: String? = null,
    val data: LoginData? = null
)

@Serializable
data class LoginData(
    val token: String? = null,
    val user: UserDto? = null
)

@Serializable
data class UserDto(
    val id: String? = null,
    @SerialName("id_pegawai") val idPegawai: String? = null,
    val email: String? = null,
    val username: String? = null
)
