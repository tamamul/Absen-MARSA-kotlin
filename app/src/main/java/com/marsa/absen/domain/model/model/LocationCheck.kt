package com.marsa.absen.domain.model

sealed interface LocationCheck {
    data object Idle : LocationCheck
    data object Checking : LocationCheck
    data object NoPermission : LocationCheck
    data object Unauthorized : LocationCheck

    /** Lokasi terverifikasi di dalam area absen oleh server. */
    data class Inside(
        val lat: Double,
        val lng: Double,
        val accuracy: Float,
        val jarak: Int?,
        val radius: String?,
        val lokasi: String?,
        val message: String?,
        val at: Long = System.currentTimeMillis()
    ) : LocationCheck {
        fun isFresh(maxAgeMs: Long = 120_000): Boolean =
            System.currentTimeMillis() - at < maxAgeMs
    }

    /** outside = true jika server menyatakan di luar area; false jika masalah GPS/jaringan. */
    data class Problem(
        val message: String,
        val jarak: Int? = null,
        val outside: Boolean = false
    ) : LocationCheck
}
