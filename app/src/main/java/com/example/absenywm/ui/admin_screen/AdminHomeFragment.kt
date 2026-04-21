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

    private fun loadAttendanceData() {

        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.time

        val endOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.time

        val startOfMonth = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.time

        db.collection("attendance")
            .get()
            .addOnSuccessListener { result ->

                var todayHadir = 0
                var todayTelat = 0
                var todayIzin = 0
                var todayAlpa = 0

                var monthHadir = 0
                var monthSakit = 0
                var monthIzin = 0
                var monthTelat = 0
                var monthAlpa = 0

                for (doc in result) {

                    val status = doc.getString("status") ?: continue
                    val timestamp = doc.getTimestamp("timestamp")?.toDate() ?: continue

                    // ===== HARI INI =====
                    if (timestamp.after(startOfDay) && timestamp.before(endOfDay)) {
                        when (status) {
                            "Hadir" -> todayHadir++
                            "Telat" -> todayTelat++
                            "Izin", "Sakit" -> todayIzin++
                            "Alpa" -> todayAlpa++
                        }
                    }

                    // ===== BULAN INI =====
                    if (timestamp.after(startOfMonth)) {
                        when (status) {
                            "Hadir" -> monthHadir++
                            "Telat" -> monthTelat++
                            "Izin" -> monthIzin++
                            "Sakit" -> monthSakit++
                            "Alpa" -> monthAlpa++
                        }
                    }
                }

                // ===== SET UI =====
                tvTodayHadir.text = todayHadir.toString()
                tvTodayTelat.text = todayTelat.toString()
                tvTodayIzin.text = todayIzin.toString()
                tvTodayAlpa.text = todayAlpa.toString()

                tvMonthHadir.text = monthHadir.toString()
                tvMonthSakit.text = monthSakit.toString()
                tvMonthIzin.text = monthIzin.toString()
                tvMonthTelat.text = monthTelat.toString()
                tvMonthAlpa.text = monthAlpa.toString()
                tvMonthTukarShift.text = "0"

                val total = monthHadir + monthSakit + monthIzin + monthAlpa
                val persen = if (total > 0) (monthHadir * 100 / total) else 0

                progressKehadiran.progress = persen
                tvPersentaseHadir.text = "$persen%"

            }
            .addOnFailureListener {
                tvTodayHadir.text = "0"
            }
    }
}