package com.marsa.absen.data.remote

object ApiConfig {
    // Retrofit mewajibkan baseUrl diakhiri "/"
    const val BASE_URL = "https://smk-maarif9kebumen.com/present/public/index.php/"

    private const val FOTO_BASE =
        "https://smk-maarif9kebumen.com/present/public/assets/img/foto_presensi/"

    const val FOTO_MASUK_URL = FOTO_BASE + "masuk/"
    const val FOTO_KELUAR_URL = FOTO_BASE + "keluar/" // asumsi: mengikuti pola folder masuk
}
