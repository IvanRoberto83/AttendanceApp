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
    private var isLoading = false

    private fun getShiftLabel(waktu: String): String {
        if (waktu == "-") return ""
        return try {
            val hour = waktu.split(".", ":")[0].trim().toInt()
            when {
                hour >= 22 || hour < 8 -> "Malam"
                hour >= 15             -> "Sore"
                else                   -> "Pagi"
            }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Menentukan status efektif yang ditampilkan ke karyawan.
     * Aturan:
     * - Jika tukarShift == true && approvalStatus == "pending" → tampilkan "Alpa" (sementara menunggu)
     * - Jika tukarShift == true && approvalStatus == "approved" → tampilkan status yang sudah disetujui
     * - Jika tukarShift == true && approvalStatus == "rejected" → tampilkan "Alpa"
     * - Selain itu → tampilkan status asli dari Firebase
     */
    private fun getEffectiveStatus(status: String, tukarShift: Boolean, approvalStatus: String): String {
        if (!tukarShift) return status
        return when (approvalStatus) {
            "pending"  -> "Alpa"   // Sementara dianggap Alpa sampai ada keputusan
            "approved" -> status   // Status sudah diupdate admin saat approve
            "rejected" -> "Alpa"   // Ditolak = Alpa
            else       -> status
        }
    }

    fun loadMonthlyHistory() {

        if (isLoading) return
        isLoading = true

        val userId = auth.currentUser?.uid ?: run { isLoading = false; return }

        val currentMonth =
            SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        db.collection("absensi")
            .document(userId)
            .collection("records")
            .get()
            .addOnSuccessListener { result ->

                fullList.clear()

                val tanggalDariRecord = mutableSetOf<String>()

                for (doc in result) {

                    val tanggal = doc.getString("tanggal") ?: continue
                    if (!tanggal.startsWith(currentMonth)) continue

                    val statusRaw      = doc.getString("status") ?: "-"
                    val waktuRaw       = doc.getString("waktu") ?: "-"
                    val type           = doc.getString("type") ?: "-"
                    val foto           = doc.getString("foto")
                    val tukarShift     = doc.getBoolean("tukarShift") ?: false
                    val approvalStatus = doc.getString("approvalStatus") ?: ""

                    // Status efektif untuk ditampilkan (pending tukar shift = Alpa)
                    val effectiveStatus = getEffectiveStatus(statusRaw, tukarShift, approvalStatus)

                    tanggalDariRecord.add(tanggal)

                    fullList.add(
                        AbsenModel(
                            tanggal        = tanggal,
                            type           = type,
                            waktu          = waktuRaw,
                            shiftLabel     = getShiftLabel(waktuRaw),
                            status         = effectiveStatus,
                            foto           = foto,
                            tukarShift     = tukarShift,
                            approvalStatus = approvalStatus
                        )
                    )
                }

                val calendar = Calendar.getInstance()
                calendar.set(Calendar.DAY_OF_MONTH, 1)

                val today = Calendar.getInstance()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val todayStr = dateFormat.format(today.time)

                while (true) {
                    val currentDate = dateFormat.format(calendar.time)
                    if (currentDate > todayStr) break

                    if (!tanggalDariRecord.contains(currentDate)) {
                        fullList.add(
                            AbsenModel(
                                tanggal        = currentDate,
                                type           = "masuk",
                                waktu          = "-",
                                shiftLabel     = "",
                                status         = "Alpa",
                                foto           = null,
                                tukarShift     = false,
                                approvalStatus = ""
                            )
                        )
                    }

                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                }

                fullList.sortByDescending { it.tanggal }

                _absenList.value = fullList
                _isEmpty.value = fullList.isEmpty()
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    fun applyFilter(filter: String) {

        val filtered = when (filter) {
            "Semua"       -> fullList
            "Tukar Shift" -> fullList.filter { it.tukarShift }
            else          -> fullList.filter { it.status.equals(filter, true) }
        }

        _absenList.value = filtered
        _isEmpty.value = filtered.isEmpty()
    }
}
