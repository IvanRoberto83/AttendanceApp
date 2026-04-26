package com.example.absenywm.ui.home_screen

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.absenywm.TimeUtils
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

        val sharedPref = requireActivity()
            .getSharedPreferences("USER_SESSION", AppCompatActivity.MODE_PRIVATE)

        val username = sharedPref.getString("USERNAME", "User")
        binding.tvUserName.text = username

        val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
        binding.tvDate.text = dateFormat.format(Date())

        binding.btnCheckIn.setOnClickListener {
            val userId = auth.currentUser?.uid ?: return@setOnClickListener

            db.collection("users").document(userId).get()
                .addOnSuccessListener { doc ->

                    val shiftMasuk = doc.getString("shiftMasuk") ?: "08:00"
                    val start = TimeUtils.extractStartTime(shiftMasuk)

                    if (!TimeUtils.isWithinAbsenTime(start)) {
                        Toast.makeText(requireContext(), "Diluar jam absen", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    val intent = Intent(requireContext(), AbsensiActivity::class.java)
                    intent.putExtra("type", "masuk")
                    intent.putExtra("shiftAsli", shiftMasuk)
                    startActivity(intent)
                }
        }

        binding.btnCheckOut.setOnClickListener {
            val userId = auth.currentUser?.uid ?: return@setOnClickListener

            db.collection("users").document(userId).get()
                .addOnSuccessListener { doc ->

                    val shiftKeluar = doc.getString("shiftKeluar") ?: "08:00"
                    val start = TimeUtils.extractStartTime(shiftKeluar)

                    if (!TimeUtils.isWithinAbsenTime(start)) {
                        Toast.makeText(requireContext(), "Diluar jam absen", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    val intent = Intent(requireContext(), AbsensiActivity::class.java)
                    intent.putExtra("type", "keluar")
                    intent.putExtra("shiftAsli", shiftKeluar)
                    startActivity(intent)
                }
        }

        loadTodayData()
        loadMonthlyStats()

        return root
    }

    override fun onResume() {
        super.onResume()
        loadTodayData()
        loadMonthlyStats()
    }

    private fun canAbsen(shift: String): Boolean {
        val start = TimeUtils.extractStartTime(shift)
        return TimeUtils.isWithinAbsenTime(start)
    }

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

        val shift = format.parse(shiftTime)!!
        val absen = format.parse(absenTime.substring(0, 5))!!

        val batasHadir = Calendar.getInstance().apply {
            time = shift
            add(Calendar.MINUTE, 30)
        }

        return absen.after(batasHadir.time)
    }

    private fun extractStartTime(shiftRange: String?): String {
        return try {
            shiftRange?.split(" - ")?.get(0) ?: "08:00"
        } catch (e: Exception) {
            "08:00"
        }
    }

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

                                if (type == "masuk") {
                                    when (status) {
                                        "Hadir" -> hadir++
                                        "Sakit" -> sakit++
                                        "Izin" -> izin++
                                    }
                                }

                                if (type == "masuk" && doc.getBoolean("tukarShift") == true) {
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