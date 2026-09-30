package com.marsa.absen.domain.model

import java.time.LocalDate

/** Satu hari absen yang sudah dihitung statusnya. */
data class DayRecord(
    val date: LocalDate,
    val item: AbsenHariIni,
    val lateMinutes: Long,
    /** Sudah absen masuk tapi tidak absen pulang, pada hari yang sudah lewat. */
    val lupaPulang: Boolean
)
