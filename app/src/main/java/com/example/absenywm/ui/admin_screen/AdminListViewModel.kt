package com.example.absenywm.ui.admin_screen

data class AdminListViewModel(
    val userId: String,
    val docId: String,
    val username: String,
    val tanggal: String,
    val type: String,
    val waktu: String,
    val shiftLabel: String = "",
    val status: String,
    val foto: String?,
    val tukarShift: Boolean = false,
    val approvalStatus: String = "",       // "pending", "approved", "rejected", atau ""
    val statusJikaDisetujui: String = "",  // Status yang akan berlaku jika admin menyetujui
    val shiftPengganti: String? = null     // Shift pengganti yang diajukan
)
