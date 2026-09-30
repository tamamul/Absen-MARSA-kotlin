package com.marsa.absen.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PegawaiProfil(
    val id: String? = null,
    val nama: String? = null,
    val nip: String? = null,
    val foto: String? = null,
    @SerialName("no_handphone") val noHandphone: String? = null,
    val jabatan: String? = null,
    @SerialName("nama_lokasi") val namaLokasi: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    val radius: String? = null,
    @SerialName("jam_masuk") val jamMasuk: String? = null,
    @SerialName("jam_pulang") val jamPulang: String? = null,
    @SerialName("jam_pulang_maksimal") val jamPulangMaksimal: String? = null,
    @SerialName("mode_absen") val modeAbsen: String? = null,
    @SerialName("face_recognition") val faceRecognition: String? = null,
    @SerialName("face_required") val faceRequired: String? = null
)

@Serializable
data class AbsenHariIni(
    val id: String? = null,
    @SerialName("id_pegawai") val idPegawai: String? = null,
    @SerialName("jenis_presensi") val jenisPresensi: String? = null,
    @SerialName("tanggal_masuk") val tanggalMasuk: String? = null,
    @SerialName("jam_masuk") val jamMasuk: String? = null,
    @SerialName("foto_masuk") val fotoMasuk: String? = null,
    @SerialName("tanggal_keluar") val tanggalKeluar: String? = null,
    @SerialName("jam_keluar") val jamKeluar: String? = null,
    @SerialName("foto_keluar") val fotoKeluar: String? = null
)
