package com.marsa.absen.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PiketRiwayatItem(
    val id: String? = null,
    @SerialName("tanggal_masuk") val tanggalMasuk: String? = null,
    @SerialName("jam_masuk") val jamMasuk: String? = null,
    @SerialName("foto_masuk") val fotoMasuk: String? = null,
    @SerialName("tanggal_keluar") val tanggalKeluar: String? = null,
    @SerialName("jam_keluar") val jamKeluar: String? = null,
    @SerialName("foto_keluar") val fotoKeluar: String? = null,
    @SerialName("tanggal_jadwal") val tanggalJadwal: String? = null,
    @SerialName("waktu_mulai") val waktuMulai: String? = null,
    @SerialName("waktu_selesai") val waktuSelesai: String? = null,
    @SerialName("nama_lokasi") val namaLokasi: String? = null,
    @SerialName("menit_terlambat") val menitTerlambat: Int? = null,
    @SerialName("durasi_menit") val durasiMenit: Int? = null,
    val status: String? = null
)
