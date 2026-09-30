package com.marsa.absen.data.remote

import android.net.Uri

object PhotoUrl {
    private val root = ApiConfig.BASE_URL.removeSuffix("index.php/")
    private const val DIR = "assets/img/foto_presensi/"

    fun masuk(name: String?): String? = build("masuk/", name)
    fun keluar(name: String?): String? = build("keluar/", name)

    private fun build(sub: String, name: String?): String? {
        val n = name?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return root + DIR + sub + Uri.encode(n)
    }
}
