package com.example.absenywm.ui.list_screen

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class ListViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _absenList = MutableLiveData<List<AbsenModel>>()
    val absenList: LiveData<List<AbsenModel>> = _absenList

    private val _isEmpty = MutableLiveData<Boolean>()
    val isEmpty: LiveData<Boolean> = _isEmpty

    private val fullList = mutableListOf<AbsenModel>()

    fun loadMonthlyHistory() {

        val userId = auth.currentUser?.uid ?: return

        val currentMonth =
            SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        db.collection("absensi")
            .document(userId)
            .collection("records")
            .get()
            .addOnSuccessListener { result ->

                fullList.clear()

                for (doc in result) {

                    val tanggal = doc.getString("tanggal") ?: continue
                    if (!tanggal.startsWith(currentMonth)) continue

                    val status = doc.getString("status") ?: "-"
                    val waktuRaw = doc.getString("waktu") ?: "-"
                    val type = doc.getString("type") ?: "-"

                    val tukarShift = doc.getBoolean("tukarShift") ?: false
                    val shiftPengganti = doc.getString("shiftPengganti")

                    val shiftMasuk = "08:00" // fallback (bisa kamu ganti dari user/session)

                    val shiftDipakai = if (tukarShift) {
                        extractStartTime(shiftPengganti)
                    } else {
                        shiftMasuk
                    }

                    val finalStatus = if (
                        type == "masuk" &&
                        isLate(shiftDipakai, waktuRaw)
                    ) {
                        "Telat"
                    } else {
                        status
                    }

                    fullList.add(
                        AbsenModel(tanggal, type, waktuRaw, finalStatus)
                    )
                }

                fullList.sortByDescending { it.tanggal }

                _absenList.value = fullList
                _isEmpty.value = fullList.isEmpty()
            }
    }

    fun applyFilter(filter: String) {

        val filtered = if (filter == "Semua") {
            fullList
        } else {
            fullList.filter { it.status == filter }
        }

        _absenList.value = filtered
        _isEmpty.value = filtered.isEmpty()
    }

    // 🔥 FIX: shift-aware + aman format waktu
    private fun isLate(shift: String, waktu: String): Boolean {
        return try {

            val format = SimpleDateFormat("HH:mm", Locale.getDefault())

            val jamShift = format.parse(shift.take(5))
            val jamAbsen = format.parse(waktu.take(5))

            val cal = Calendar.getInstance()
            cal.time = jamShift!!
            cal.add(Calendar.MINUTE, 30) // toleransi telat

            jamAbsen!!.after(cal.time)

        } catch (e: Exception) {
            false
        }
    }

    // helper aman (kalau format "22:00 - 08:00")
    private fun extractStartTime(shift: String?): String {
        return try {
            shift?.split("-")?.get(0)?.trim() ?: "08:00"
        } catch (e: Exception) {
            "08:00"
        }
    }
}