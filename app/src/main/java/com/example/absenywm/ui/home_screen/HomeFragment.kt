package com.example.absenywm.ui.home_screen

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.absenywm.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // 🔹 Ambil username dari session
        val sharedPref = requireActivity()
            .getSharedPreferences("USER_SESSION", AppCompatActivity.MODE_PRIVATE)

        val username = sharedPref.getString("USERNAME", "User")
        binding.tvUserName.text = username

        // 🔹 Set tanggal sekarang
        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
        binding.tvDate.text = dateFormat.format(Date())

        // 🔹 Button action (dengan validasi waktu)
        binding.btnCheckIn.setOnClickListener {
            if (!isWithinAbsenTime()) {
                Toast.makeText(requireContext(), "Diluar jam absen!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(requireContext(), AbsensiActivity::class.java)
            intent.putExtra("type", "masuk")
            startActivity(intent)
        }

        binding.btnCheckOut.setOnClickListener {
            if (!isWithinAbsenTime()) {
                Toast.makeText(requireContext(), "Diluar jam absen!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(requireContext(), AbsensiActivity::class.java)
            intent.putExtra("type", "keluar")
            startActivity(intent)
        }

        // 🔥 LOAD DATA
        loadTodayData()
        loadMonthlyStats()
        updateAbsenButtonState()

        return root
    }

    override fun onResume() {
        super.onResume()
        loadTodayData()
        loadMonthlyStats()
        updateAbsenButtonState()
    }

    // =========================
    // ⏰ VALIDASI JAM ABSEN
    // =========================
    private fun isWithinAbsenTime(): Boolean {
        val now = Calendar.getInstance()

        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)

        val currentTotal = currentHour * 60 + currentMinute

        val ranges = listOf(
            Pair(8 * 60, 8 * 60 + 30),    // 08:00 - 08:30
            Pair(15 * 60, 15 * 60 + 30),  // 15:00 - 15:30
            Pair(22 * 60, 22 * 60 + 30)   // 22:00 - 22:30
        )

        return ranges.any { currentTotal in it.first..it.second }
    }

    private fun updateAbsenButtonState() {
        val isOpen = isWithinAbsenTime()

        binding.btnCheckIn.isEnabled = isOpen
        binding.btnCheckOut.isEnabled = isOpen

        binding.btnCheckIn.alpha = if (isOpen) 1f else 0.5f
        binding.btnCheckOut.alpha = if (isOpen) 1f else 0.5f
    }

    // =========================
    // 🔥 DATA HARI INI
    // =========================
    private fun loadTodayData() {

        val userId = auth.currentUser?.uid ?: return

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val docMasuk = "${today}_masuk"
        val docKeluar = "${today}_keluar"

        val ref = db.collection("absensi")
            .document(userId)
            .collection("records")

        var checkInTime = "-"
        var checkOutTime = "-"
        var statusHariIni = "Belum Absen"

        ref.document(docMasuk).get()
            .addOnSuccessListener { docMasukSnap ->

                if (docMasukSnap.exists()) {
                    checkInTime = docMasukSnap.getString("waktu") ?: "-"
                    statusHariIni = docMasukSnap.getString("status") ?: "Hadir"
                }

                ref.document(docKeluar).get()
                    .addOnSuccessListener { docKeluarSnap ->

                        if (docKeluarSnap.exists()) {
                            checkOutTime = docKeluarSnap.getString("waktu") ?: "-"
                        }

                        if (_binding != null) {
                            binding.tvCheckTimes.text =
                                "Absen-masuk: $checkInTime | Absen-keluar: $checkOutTime"

                            binding.tvStatusBadge.text = statusHariIni
                        }
                    }
            }
    }

    private fun isLate(shiftTime: String, absenTime: String): Boolean {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())

        val shift = format.parse(shiftTime)
        val absen = format.parse(absenTime.substring(0, 5))

        val cal = Calendar.getInstance()
        cal.time = shift!!
        cal.add(Calendar.MINUTE, 30)

        return absen!!.after(cal.time)
    }

    private fun extractStartTime(shiftRange: String?): String {
        return try {
            shiftRange?.split(" - ")?.get(0) ?: "08:00"
        } catch (e: Exception) {
            "08:00"
        }
    }

    // =========================
    // 📊 REKAP BULANAN
    // =========================
    private fun loadMonthlyStats() {

        val userId = auth.currentUser?.uid ?: return

        val currentMonth =
            SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        db.collection("users").document(userId).get()
            .addOnSuccessListener { userDoc ->

                val shiftMasuk = userDoc.getString("shiftMasuk") ?: "08:00"

                db.collection("absensi")
                    .document(userId)
                    .collection("records")
                    .get()
                    .addOnSuccessListener { result ->

                        var hadir = 0
                        var sakit = 0
                        var izin = 0
                        var telat = 0
                        var alpa = 0
                        var tukarShiftCount = 0

                        val activeDates = mutableSetOf<String>()
                        val excusedDates = mutableSetOf<String>()

                        for (doc in result) {

                            val tanggal = doc.getString("tanggal") ?: continue
                            val type = doc.getString("type") ?: ""

                            if (tanggal.startsWith(currentMonth)) {

                                activeDates.add(tanggal)

                                val status = doc.getString("status")

                                if (status == "Sakit" || status == "Izin") {
                                    excusedDates.add(tanggal)
                                }

                                if (type == "masuk") {

                                    val waktu = doc.getString("waktu") ?: "00:00"
                                    val tukarShift = doc.getBoolean("tukarShift") ?: false

                                    val shiftDipakai = if (tukarShift) {
                                        extractStartTime(doc.getString("shiftPengganti"))
                                    } else {
                                        shiftMasuk
                                    }

                                    if (isLate(shiftDipakai, waktu)) {
                                        telat++
                                    }
                                }

                                when (status) {
                                    "Hadir" -> hadir++
                                    "Sakit" -> sakit++
                                    "Izin" -> izin++
                                }

                                if (doc.getBoolean("tukarShift") == true) {
                                    tukarShiftCount++
                                }
                            }
                        }

                        val calendar = Calendar.getInstance()
                        val todayDay = calendar.get(Calendar.DAY_OF_MONTH)

                        for (i in 1..todayDay) {

                            val day = String.format("%02d", i)
                            val date = "$currentMonth-$day"

                            if (!activeDates.contains(date) && !excusedDates.contains(date)) {
                                alpa++
                            }
                        }

                        if (_binding != null) {
                            binding.tvCountHadir.text = hadir.toString()
                            binding.tvCountSakit.text = sakit.toString()
                            binding.tvCountIzin.text = izin.toString()
                            binding.tvCountTelat.text = telat.toString()
                            binding.tvCountAlpa.text = alpa.toString()
                            binding.tvCountTukar.text = tukarShiftCount.toString()
                        }
                    }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}