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
    private lateinit var tvEmptyMessage: TextView

    private val karyawanList = mutableListOf<String>()

    private var selectedUser = "NONE"
    private var selectedFilter = "ALL"

    private var queryToken = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_admin_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()

        initView(view)
        setupMonthLabel()
        setupFilter()

        // Sembunyikan semua sebelum data Firestore kembali
        layoutEmpty.visibility  = View.GONE
        rvAttendance.visibility = View.GONE

        setChipGroupEnabled(false)
        loadKaryawan()
    }

    private fun initView(view: View) {
        actvKaryawan    = view.findViewById(R.id.actvKaryawan)
        rvAttendance    = view.findViewById(R.id.rvAdminAttendance)
        layoutEmpty     = view.findViewById(R.id.layoutAdminEmpty)

        tvStatHadir     = view.findViewById(R.id.tvStatHadir)
        tvStatTelat     = view.findViewById(R.id.tvStatTelat)
        tvStatIzinSakit = view.findViewById(R.id.tvStatIzinSakit)
        tvStatAlpa      = view.findViewById(R.id.tvStatAlpa)
        tvMonth         = view.findViewById(R.id.tvAdminMonthLabel)

        chipGroup       = view.findViewById(R.id.chipGroupAdminFilter)
        tvEmptyMessage  = view.findViewById(R.id.tvEmptyMessage)

        rvAttendance.layoutManager = LinearLayoutManager(requireContext())

        adapter = AdminAbsenAdapter(listData) {
            if (selectedUser == "NONE") loadPendingApprovals()
            else loadAttendance()
        }

        rvAttendance.adapter = adapter
    }

    private fun setupMonthLabel() {
        val currentMonth = SimpleDateFormat("MMMM yyyy", Locale("id", "ID")).format(Date())
        tvMonth.text = currentMonth
    }

    private fun loadKaryawan() {
        karyawanList.clear()

        db.collection("users")
            .whereEqualTo("role", "Karyawan")
            .get()
            .addOnSuccessListener { result ->

                for (doc in result) {
                    val name = doc.getString("username")
                    if (!name.isNullOrEmpty()) karyawanList.add(name)
                }

                val dropdownAdapter = object : ArrayAdapter<String>(
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

                actvKaryawan.setAdapter(dropdownAdapter)
                actvKaryawan.setText("", false)

                actvKaryawan.setOnItemClickListener { _, _, position, _ ->
                    selectedUser   = karyawanList[position]
                    selectedFilter = "ALL"
                    setChipGroupEnabled(true)

                    chipGroup.clearCheck()                  // ← TAMBAH INI
                    chipGroup.check(R.id.chipAdminAll)      // sekarang pasti trigger listener
                }

                // Halaman pertama dibuka: muat pending tukar shift
                loadPendingApprovals()
            }
    }

    private fun setupFilter() {
        chipGroup.setOnCheckedChangeListener { _, checkedId ->
            // Chip hanya bereaksi setelah karyawan dipilih
            if (selectedUser == "NONE") return@setOnCheckedChangeListener

            selectedFilter = when (checkedId) {
                R.id.chipAdminHadir      -> "Hadir"
                R.id.chipAdminTelat      -> "Telat"
                R.id.chipAdminSakit      -> "Sakit"
                R.id.chipAdminIzin       -> "Izin"
                R.id.chipAdminAlpa       -> "Alpa"
                R.id.chipAdminTukarShift -> "TukarShift"
                else                     -> "ALL"
            }

            loadAttendance()
        }
    }

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
     * Saat halaman pertama dibuka (selectedUser == "NONE"):
     * Muat semua pengajuan Tukar Shift yang masih pending dari seluruh karyawan,
     * tampilkan di rvAttendance. Jika kosong, tampilkan empty state.
     */
    private fun loadPendingApprovals() {
        if (!isAdded) return

        listData.clear()
        adapter.notifyDataSetChanged()
        updateStat(0, 0, 0, 0)

        db.collection("users")
            .whereEqualTo("role", "Karyawan")
            .get()
            .addOnSuccessListener { users ->
                if (!isAdded) return@addOnSuccessListener

                val totalUser = users.size()
                if (totalUser == 0) {
                    toggleEmpty(true)
                    return@addOnSuccessListener
                }

                var processedUser = 0

                for (user in users) {
                    val userId   = user.id
                    val username = user.getString("username") ?: ""

                    db.collection("absensi")
                        .document(userId)
                        .collection("records")
                        .whereEqualTo("approvalStatus", "pending")
                        .get()
                        .addOnSuccessListener { records ->
                            if (!isAdded) return@addOnSuccessListener

                            for (doc in records) {
                                val type = doc.getString("type") ?: ""
                                if (type != "masuk") continue
                                val isTukarShift = doc.getBoolean("tukarShift") ?: false
                                if (!isTukarShift) continue

                                val waktu = doc.getString("waktu") ?: "-"
                                listData.add(
                                    AdminListViewModel(
                                        userId              = userId,
                                        docId               = doc.id,
                                        username            = username,
                                        tanggal             = doc.getString("tanggal") ?: "",
                                        type                = type,
                                        waktu               = waktu,
                                        shiftLabel          = getShiftLabel(waktu),
                                        status              = doc.getString("status") ?: "Alpa",
                                        foto                = doc.getString("foto"),
                                        tukarShift          = true,
                                        approvalStatus      = "pending",
                                        statusJikaDisetujui = doc.getString("statusJikaDisetujui") ?: "Hadir",
                                        shiftPengganti      = doc.getString("shiftPengganti")
                                    )
                                )
                            }

                            processedUser++
                            if (processedUser == totalUser) {
                                listData.sortByDescending { it.tanggal }
                                adapter.notifyDataSetChanged()
                                toggleEmpty(listData.isEmpty())
                            }
                        }
                        .addOnFailureListener {
                            processedUser++
                            if (processedUser == totalUser) {
                                adapter.notifyDataSetChanged()
                                toggleEmpty(listData.isEmpty())
                            }
                        }
                }
            }
    }

    private fun loadAttendance() {
        val token = ++queryToken

        listData.clear()
        adapter.notifyDataSetChanged()
        updateStat(0, 0, 0, 0)

        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        var hadir = 0
        var telat = 0
        var izinSakit = 0
        var alpa = 0

        db.collection("users")
            .whereEqualTo("role", "Karyawan")
            .whereEqualTo("username", selectedUser)
            .get()
            .addOnSuccessListener { users ->
                if (token != queryToken) return@addOnSuccessListener

                var processedUser = 0
                val totalUser = users.size()

                if (totalUser == 0) { toggleEmpty(true); return@addOnSuccessListener }

                for (user in users) {

                    val userId   = user.id
                    val username = user.getString("username") ?: ""

                    if (selectedUser != username) {
                        processedUser++
                        if (processedUser == totalUser) finalizeList(hadir, telat, izinSakit, alpa)
                        continue
                    }

                    db.collection("absensi")
                        .document(userId)
                        .collection("records")
                        .get()
                        .addOnSuccessListener { records ->
                            if (token != queryToken) return@addOnSuccessListener

                            val tanggalDenganRecord = mutableSetOf<String>()
                            val excusedDates        = mutableSetOf<String>()

                            for (doc in records) {

                                val tanggal = doc.getString("tanggal") ?: continue
                                if (!tanggal.startsWith(currentMonth)) continue

                                val type = doc.getString("type") ?: "-"
                                if (type != "masuk") continue

                                val statusRaw      = doc.getString("status") ?: continue
                                val waktu          = doc.getString("waktu") ?: "-"
                                val foto           = doc.getString("foto")
                                val tukarShift     = doc.getBoolean("tukarShift") ?: false
                                val approvalStatus = doc.getString("approvalStatus") ?: ""
                                val statusJikaDisetujui = doc.getString("statusJikaDisetujui") ?: ""
                                val shiftPengganti = doc.getString("shiftPengganti")

                                tanggalDenganRecord.add(tanggal)

                                // Status efektif: pending tukar shift = Alpa sementara
                                val effectiveStatus = if (tukarShift && approvalStatus == "pending") {
                                    "Alpa"
                                } else {
                                    statusRaw
                                }

                                when (effectiveStatus.lowercase()) {
                                    "hadir" -> hadir++
                                    "telat" -> telat++
                                    "izin"  -> { izinSakit++; excusedDates.add(tanggal) }
                                    "sakit" -> { izinSakit++; excusedDates.add(tanggal) }
                                    "alpa"  -> alpa++
                                }

                                val lolosFilter = when (selectedFilter) {
                                    "ALL"        -> true
                                    "TukarShift" -> tukarShift
                                    "Alpa"       -> effectiveStatus.equals("Alpa", true)
                                    else         -> effectiveStatus.equals(selectedFilter, true)
                                }

                                if (lolosFilter) {
                                    listData.add(
                                        AdminListViewModel(
                                            userId              = userId,
                                            docId               = doc.id,
                                            username            = username,
                                            tanggal             = tanggal,
                                            type                = type,
                                            waktu               = waktu,
                                            shiftLabel          = getShiftLabel(waktu),
                                            status              = effectiveStatus,
                                            foto                = foto,
                                            tukarShift          = tukarShift,
                                            approvalStatus      = approvalStatus,
                                            statusJikaDisetujui = statusJikaDisetujui,
                                            shiftPengganti      = shiftPengganti
                                        )
                                    )
                                }
                            }

                            val calendar   = Calendar.getInstance()
                            calendar.set(Calendar.DAY_OF_MONTH, 1)
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            val todayStr   = dateFormat.format(Calendar.getInstance().time)

                            while (true) {
                                val tanggal = dateFormat.format(calendar.time)
                                if (tanggal > todayStr) break

                                if (!tanggalDenganRecord.contains(tanggal) && !excusedDates.contains(tanggal)) {
                                    alpa++

                                    if (selectedFilter == "ALL" || selectedFilter == "Alpa") {
                                        listData.add(
                                            AdminListViewModel(
                                                userId     = userId,
                                                docId      = "ALPA",
                                                username   = username,
                                                tanggal    = tanggal,
                                                type       = "masuk",
                                                waktu      = "-",
                                                shiftLabel = "",
                                                status     = "Alpa",
                                                foto       = null,
                                                tukarShift = false
                                            )
                                        )
                                    }
                                }

                                calendar.add(Calendar.DAY_OF_MONTH, 1)
                            }

                            processedUser++
                            if (processedUser == totalUser) finalizeList(hadir, telat, izinSakit, alpa)
                        }
                }
            }
    }

    private fun finalizeList(hadir: Int, telat: Int, izinSakit: Int, alpa: Int) {
        listData.sortByDescending { it.tanggal }
        adapter.notifyDataSetChanged()
        updateStat(hadir, telat, izinSakit, alpa)
        toggleEmpty(listData.isEmpty())
    }

    private fun updateStat(hadir: Int, telat: Int, izin: Int, alpa: Int) {
        tvStatHadir.text     = hadir.toString()
        tvStatTelat.text     = telat.toString()
        tvStatIzinSakit.text = izin.toString()
        tvStatAlpa.text      = alpa.toString()
    }

    private fun toggleEmpty(isEmpty: Boolean) {
        layoutEmpty.visibility  = if (isEmpty) View.VISIBLE else View.GONE
        rvAttendance.visibility = if (isEmpty) View.GONE    else View.VISIBLE

        tvEmptyMessage.text = when {
            selectedUser == "NONE" -> "Tidak ada pengajuan Tukar Shift yang menunggu persetujuan"
            else                   -> "Tidak ada data untuk karyawan ini"
        }
    }

    private fun setChipGroupEnabled(enabled: Boolean) {
        for (i in 0 until chipGroup.childCount) {
            chipGroup.getChildAt(i).isEnabled = enabled
            chipGroup.getChildAt(i).alpha     = if (enabled) 1.0f else 0.4f
        }
        if (!enabled) chipGroup.clearCheck()
    }
}