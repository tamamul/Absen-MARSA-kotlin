package com.marsa.absen.domain.model

enum class AbsenMode { MASUK, KELUAR }

data class AbsenRequest(
    val mode: AbsenMode,
    val profil: PegawaiProfil,
    val pre: LocationCheck.Inside? = null,
    val piket: Boolean = false
)
