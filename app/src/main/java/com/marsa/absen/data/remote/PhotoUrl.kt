package com.marsa.absen.data.remote

import android.net.Uri

object PhotoUrl {
    private val root = ApiConfig.BASE_URL.removeSuffix("index.php/")
    private const val PRESENSI = "assets/img/foto_presensi/"
    private const val PROFIL = "assets/img/user_profile/"

    fun masuk(name: String?): String? = build(PRESENSI + "masuk/", name)
    fun keluar(name: String?): String? = build(PRESENSI + "keluar/", name)
    fun piketMasuk(name: String?): String? = build(PRESENSI + "piketmasuk/", name)
    fun piketKeluar(name: String?): String? = build(PRESENSI + "piketkeluar/", name)
    fun profil(name: String?): String? = build(PROFIL, name)

    private fun build(path: String, name: String?): String? {
        val n = name?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return root + path + Uri.encode(n)
    }
}
