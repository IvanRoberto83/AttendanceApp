package com.example.absenywm.ui.admin_screen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.absenywm.R
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AdminHomeFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()

    private lateinit var tvAdminName: TextView
    private lateinit var tvAdminDate: TextView

    private lateinit var tvTodayHadir: TextView
    private lateinit var tvTodayTelat: TextView
    private lateinit var tvTodayIzin: TextView
    private lateinit var tvTodayAlpa: TextView

    private lateinit var tvMonthHadir: TextView
    private lateinit var tvMonthSakit: TextView
    private lateinit var tvMonthIzin: TextView
    private lateinit var tvMonthTukarShift: TextView
    private lateinit var tvMonthTelat: TextView
    private lateinit var tvMonthAlpa: TextView

    private lateinit var progressKehadiran: ProgressBar
    private lateinit var tvPersentaseHadir: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_admin_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        loadAdminData()
        loadAttendanceData()
    }

    private fun initViews(view: View) {
        tvAdminName = view.findViewById(R.id.tvAdminName)
        tvAdminDate = view.findViewById(R.id.tvAdminDate)

        tvTodayHadir = view.findViewById(R.id.tvTodayHadir)
        tvTodayTelat = view.findViewById(R.id.tvTodayTelat)
        tvTodayIzin = view.findViewById(R.id.tvTodayIzin)
        tvTodayAlpa = view.findViewById(R.id.tvTodayAlpa)

        tvMonthHadir = view.findViewById(R.id.tvMonthHadir)
        tvMonthSakit = view.findViewById(R.id.tvMonthSakit)
        tvMonthIzin = view.findViewById(R.id.tvMonthIzin)
        tvMonthTukarShift = view.findViewById(R.id.tvMonthTukarShift)
        tvMonthTelat = view.findViewById(R.id.tvMonthTelat)
        tvMonthAlpa = view.findViewById(R.id.tvMonthAlpa)

        progressKehadiran = view.findViewById(R.id.progressKehadiran)
        tvPersentaseHadir = view.findViewById(R.id.tvPersentaseHadir)
    }

    private fun loadAdminData() {
        val sharedPref = requireActivity().getSharedPreferences("USER_SESSION", 0)
        val username = sharedPref.getString("USERNAME", "Admin")

        tvAdminName.text = username

        val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
        tvAdminDate.text = sdf.format(Date())
    }

    private fun getCurrentDayOfMonth(): Int {
        return Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    }

    private fun loadAttendanceData() {

        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        db.collection("users")
            .whereEqualTo("role", "Karyawan")
            .get()
            .addOnSuccessListener { users ->

                val totalUser = users.size()

                var todayHadir = 0
                var todayTelat = 0
                var todayIzin = 0

                var monthHadir = 0
                var monthTelat = 0
                var monthIzin = 0
                var monthSakit = 0
                var monthTukarShift = 0

                var processedUser = 0

                for (user in users) {

                    val userId = user.id

                    db.collection("absensi")
                        .document(userId)
                        .collection("records")
                        .get()
                        .addOnSuccessListener { records ->

                            var isTodayAbsent = true

                            for (doc in records) {

                                val tanggal = doc.getString("tanggal") ?: continue
                                val status = doc.getString("status") ?: continue

                                if (tanggal == todayStr) {
                                    isTodayAbsent = false

                                    when (status) {
                                        "Hadir" -> todayHadir++
                                        "Telat" -> todayTelat++
                                        "Izin", "Sakit" -> todayIzin++
                                    }
                                }

                                if (tanggal.startsWith(currentMonth)) {
                                    when (status) {
                                        "Hadir" -> monthHadir++
                                        "Telat" -> monthTelat++
                                        "Izin" -> monthIzin++
                                        "Sakit" -> monthSakit++
                                    }
                                    val isTukar = doc.getBoolean("tukarShift") ?: false
                                    val type = doc.getString("type") ?: ""
                                    if (isTukar && type == "masuk") monthTukarShift++
                                }
                            }

                            if (isTodayAbsent) {
                                todayIzin += 0
                            }

                            processedUser++

                            if (processedUser == totalUser) {

                                val todayAlpa =
                                    totalUser - (todayHadir + todayTelat + todayIzin)

                                val monthAlpa =
                                    (totalUser * getCurrentDayOfMonth()) -
                                            (monthHadir + monthTelat + monthIzin + monthSakit)

                                tvTodayHadir.text = todayHadir.toString()
                                tvTodayTelat.text = todayTelat.toString()
                                tvTodayIzin.text = todayIzin.toString()
                                tvTodayAlpa.text = todayAlpa.toString()

                                tvMonthHadir.text = monthHadir.toString()
                                tvMonthSakit.text = monthSakit.toString()
                                tvMonthIzin.text = monthIzin.toString()
                                tvMonthTelat.text = monthTelat.toString()
                                tvMonthAlpa.text = monthAlpa.toString()
                                tvMonthTukarShift.text = monthTukarShift.toString()

                                val hadirValid = todayHadir + todayTelat
                                val totalAktif = totalUser - todayIzin

                                val persen = if (totalAktif > 0) {
                                    (hadirValid * 100 / totalAktif)
                                } else {
                                    0
                                }

                                progressKehadiran.progress = persen
                                tvPersentaseHadir.text = "$persen%"
                            }
                        }
                }
            }
    }
}