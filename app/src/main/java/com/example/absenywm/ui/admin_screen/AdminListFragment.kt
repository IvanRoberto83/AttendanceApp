package com.example.absenywm.ui.admin_screen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.absenywm.R
import com.google.android.material.chip.ChipGroup
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AdminListFragment : Fragment() {

    private lateinit var adapter: AdminAbsenAdapter
    private val listData = mutableListOf<AdminListViewModel>()

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_admin_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()

        initView(view)
        setupMonthLabel()
        setupFilter()
        loadKaryawan()
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

        adapter = AdminAbsenAdapter(listData)
        rvAttendance.adapter = adapter
    }

    private fun setupMonthLabel() {
        val currentMonth = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
            .format(Date())
        tvMonth.text = currentMonth
    }

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

                val adapter = object : ArrayAdapter<String>(
                    requireContext(),
                    R.layout.item_dropdown_karyawan,
                    karyawanList
                ) {
                    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val view = super.getView(position, convertView, parent) as TextView
                        val selectedText = actvKaryawan.text.toString()
                        view.setTextColor(
                            ContextCompat.getColor(
                                context,
                                if (view.text == selectedText) R.color.orange_bold else R.color.black
                            )
                        )
                        return view
                    }

                    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                        val view = super.getDropDownView(position, convertView, parent) as TextView
                        val selectedText = actvKaryawan.text.toString()
                        view.setTextColor(
                            ContextCompat.getColor(
                                context,
                                if (view.text == selectedText) R.color.orange_bold else R.color.black
                            )
                        )
                        return view
                    }
                }

                actvKaryawan.setAdapter(adapter)

                actvKaryawan.setText(karyawanList[0], false)
                selectedUser = "ALL"

                actvKaryawan.setOnItemClickListener { _, _, position, _ ->
                    selectedUser = if (position == 0) "ALL" else karyawanList[position]
                    loadAttendance()
                }

                loadAttendance()
            }
    }

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

    private fun loadAttendance() {

        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val today = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

        listData.clear()

        var hadir = 0
        var telat = 0
        var izin = 0
        var alpa = 0

        db.collection("users")
            .whereEqualTo("role", "Karyawan")
            .get()
            .addOnSuccessListener { users ->

                var processedUser = 0
                val totalUser = users.size()

                for (user in users) {

                    val userId = user.id
                    val username = user.getString("username") ?: ""

                    if (selectedUser != "ALL" && selectedUser != username) {
                        processedUser++
                        continue
                    }

                    db.collection("absensi")
                        .document(userId)
                        .collection("records")
                        .get()
                        .addOnSuccessListener { records ->

                            val tanggalSet = mutableSetOf<String>()

                            for (doc in records) {

                                val tanggal = doc.getString("tanggal") ?: continue
                                if (!tanggal.startsWith(currentMonth)) continue

                                val status = doc.getString("status") ?: continue
                                val type = doc.getString("type") ?: "-"
                                val waktu = doc.getString("waktu") ?: "-"
                                val foto = doc.getString("foto")

                                tanggalSet.add(tanggal)

                                when (status.lowercase()) {
                                    "hadir" -> hadir++
                                    "telat" -> telat++
                                    "izin", "sakit" -> izin++
                                }

                                if (selectedFilter == "ALL" || status.equals(selectedFilter, true)) {
                                    listData.add(
                                        AdminListViewModel(
                                            username,
                                            tanggal,
                                            type,
                                            waktu,
                                            status,
                                            foto
                                        )
                                    )
                                }
                            }

                            val calendar = Calendar.getInstance()
                            calendar.set(Calendar.DAY_OF_MONTH, 1)

                            while (calendar.get(Calendar.DAY_OF_MONTH) <= today) {

                                val tanggal = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                    .format(calendar.time)

                                if (!tanggalSet.contains(tanggal)) {

                                    alpa++

                                    if (selectedFilter == "ALL" || selectedFilter == "Alpa") {
                                        listData.add(
                                            AdminListViewModel(
                                                username,
                                                tanggal,
                                                "Tidak Melakukan Absensi",
                                                "-",
                                                "Alpa",
                                                null
                                            )
                                        )
                                    }
                                }

                                calendar.add(Calendar.DAY_OF_MONTH, 1)
                            }

                            processedUser++

                            if (processedUser == totalUser) {

                                listData.sortByDescending { it.tanggal }

                                adapter.notifyDataSetChanged()

                                updateStat(hadir, telat, izin, alpa)

                                toggleEmpty(listData.isEmpty())
                            }
                        }
                }
            }
    }

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