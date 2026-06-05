package com.example.absenywm.ui.list_screen

data class AbsenModel(
    val tanggal: String,
    val type: String,
    val waktu: String,
    val shiftLabel: String = "",
    val status: String,
    val foto: String?,
    val tukarShift: Boolean = false,
    val approvalStatus: String = "" // "pending", "approved", "rejected", atau "" (bukan tukar shift)
)
