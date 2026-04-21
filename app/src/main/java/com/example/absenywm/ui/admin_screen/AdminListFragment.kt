package com.example.absenywm.ui.admin_screen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.absenywm.R
import com.google.android.material.chip.ChipGroup
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AdminListFragment : Fragment() {

    private lateinit var db: FirebaseFirestore

    private lateinit var actvKaryawan: AutoCompleteTextView
    private lateinit var rvAttendance: RecyclerView
    private lateinit var layoutEmpty: LinearLayout

    private lateinit var tvStatHadir: TextView
    private lateinit var tvStatTelat: TextView
    private lateinit var tvStatIzinSakit: TextView
    private lateinit var tvStatAlpa: TextView
    private lateinit var tvMonth: TextView

    private lateinit var chipGroup: ChipGroup

    private val karyawanList = mutableListOf<String>()

    private var selectedUser = "ALL"
    private var selectedFilter = "ALL"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_admin_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()

        initView(view)
        setupMonthLabel()
        loadKaryawan()
        setupFilter()
    }

    private fun initView(view: View) {
        actvKaryawan = view.findViewById(R.id.actvKaryawan)
        rvAttendance = view.findViewById(R.id.rvAdminAttendance)
        layoutEmpty = view.findViewById(R.id.layoutAdminEmpty)

        tvStatHadir = view.findViewById(R.id.tvStatHadir)
        tvStatTelat = view.findViewById(R.id.tvStatTelat)
        tvStatIzinSakit = view.findViewById(R.id.tvStatIzinSakit)
        tvStatAlpa = view.findViewById(R.id.tvStatAlpa)
        tvMonth = view.findViewById(R.id.tvAdminMonthLabel)

        chipGroup = view.findViewById(R.id.chipGroupAdminFilter)

        rvAttendance.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupMonthLabel() {
        val currentMonth = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
            .format(Date())
        tvMonth.text = currentMonth
    }

    // =========================
    // LOAD KARYAWAN
    // =========================
    private fun loadKaryawan() {
        karyawanList.clear()
        karyawanList.add("Semua Karyawan")

        db.collection("users")
            .whereEqualTo("role", "Karyawan")
            .get()
            .addOnSuccessListener { result ->

                for (doc in result) {
                    val name = doc.getString("username")
                    if (!name.isNullOrEmpty()) {
                        karyawanList.add(name)
                    }
                }

                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    karyawanList
                )

                actvKaryawan.setAdapter(adapter)

                actvKaryawan.setOnItemClickListener { _, _, position, _ ->
                    selectedUser = if (position == 0) {
                        "ALL"
                    } else {
                        karyawanList[position]
                    }

                    loadAttendance()
                }
            }
    }

    // =========================
    // FILTER CHIP
    // =========================
    private fun setupFilter() {
        chipGroup.setOnCheckedChangeListener { _, checkedId ->

            selectedFilter = when (checkedId) {
                R.id.chipAdminHadir -> "Hadir"
                R.id.chipAdminTelat -> "Telat"
                R.id.chipAdminSakit -> "Sakit"
                R.id.chipAdminIzin -> "Izin"
                R.id.chipAdminAlpa -> "Alpa"
                else -> "ALL"
            }

            loadAttendance()
        }
    }

    // =========================
    // LOAD ABSENSI
    // =========================
    private fun loadAttendance() {

        db.collection("attendance")
            .get()
            .addOnSuccessListener { result ->

                var hadir = 0
                var telat = 0
                var izin = 0
                var alpa = 0
                var total = 0

                for (doc in result) {

                    val nama = doc.getString("username")
                    val status = doc.getString("status")

                    // FILTER USER
                    if (selectedUser != "ALL" && selectedUser != nama) continue

                    // FILTER STATUS
                    if (selectedFilter != "ALL" &&
                        !selectedFilter.equals(status, ignoreCase = true)
                    ) continue

                    total++

                    when (status?.lowercase()) {
                        "hadir" -> hadir++
                        "telat" -> telat++
                        "izin", "sakit" -> izin++
                        "alpa" -> alpa++
                    }
                }

                updateStat(hadir, telat, izin, alpa)
                toggleEmpty(total == 0)
            }
    }

    // =========================
    // UPDATE UI
    // =========================
    private fun updateStat(hadir: Int, telat: Int, izin: Int, alpa: Int) {
        tvStatHadir.text = hadir.toString()
        tvStatTelat.text = telat.toString()
        tvStatIzinSakit.text = izin.toString()
        tvStatAlpa.text = alpa.toString()
    }

    private fun toggleEmpty(isEmpty: Boolean) {
        layoutEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        rvAttendance.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
}