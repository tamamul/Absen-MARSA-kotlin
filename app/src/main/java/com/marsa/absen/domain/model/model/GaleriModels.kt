package com.marsa.absen.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GaleriItem(
    val id: String? = null,
    @SerialName("jam_masuk") val jamMasuk: String? = null,
    @SerialName("jam_keluar") val jamKeluar: String? = null,
    @SerialName("foto_masuk") val fotoMasuk: String? = null,
    @SerialName("foto_keluar") val fotoKeluar: String? = null,
    val nama: String? = null,
    val jabatan: String? = null
)

/** Kunci unik untuk daftar dan tampilan penuh. */
val GaleriItem.uid: String
    get() = id ?: "${nama.orEmpty()}-${jamMasuk.orEmpty()}"
