package com.marsa.absen.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PiketStatus(
    @SerialName("waktu_server") val waktuServer: String? = null,
    val aktif: PiketAktif? = null,
    val absen: PiketAbsen? = null,
    @SerialName("bisa_masuk") val bisaMasuk: Boolean = false,
    @SerialName("bisa_keluar") val bisaKeluar: Boolean = false,
    val alasan: String? = null,
    val jadwal: List<PiketJadwal> = emptyList()
)

@Serializable
data class PiketAbsen(
    val id: String? = null,
    @SerialName("tanggal_masuk") val tanggalMasuk: String? = null,
    @SerialName("jam_masuk") val jamMasuk: String? = null
)

@Serializable
data class PiketAktif(
    val id: String? = null,
    val hari: String? = null,
    val tanggal: String? = null,
    @SerialName("waktu_mulai") val waktuMulai: String? = null,
    @SerialName("waktu_selesai") val waktuSelesai: String? = null,
    @SerialName("nama_lokasi") val namaLokasi: String? = null,
    val radius: String? = null,
    val keterangan: String? = null,
    val buka: String? = null,
    val mulai: String? = null,
    val selesai: String? = null,
    val tutup: String? = null
)

@Serializable
data class PiketJadwal(
    val id: String? = null,
    val hari: String? = null,
    @SerialName("waktu_mulai") val waktuMulai: String? = null,
    @SerialName("waktu_selesai") val waktuSelesai: String? = null,
    val keterangan: String? = null,
    @SerialName("nama_lokasi") val namaLokasi: String? = null,
    val radius: String? = null
)
