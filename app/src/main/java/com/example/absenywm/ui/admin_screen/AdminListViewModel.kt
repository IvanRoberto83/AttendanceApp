package com.example.absenywm.ui.admin_screen

data class AdminListViewModel(
    val userId: String,
    val docId: String,
    val username: String,
    val tanggal: String,
    val type: String,
    val waktu: String,
    val status: String,
    val foto: String?
)