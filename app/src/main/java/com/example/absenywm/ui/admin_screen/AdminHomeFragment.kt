package com.example.absenywm.ui.admin_screen

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.absenywm.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.FirebaseFirestore
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class AdminHomeFragment : Fragment() {

    private val db = FirebaseFirestore.getInstance()

    private val executor    = Executors.newCachedThreadPool()
    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var tvAdminName: TextView
    private lateinit var tvAdminDate: TextView
    private lateinit var tvTotalKaryawan: TextView

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

    private lateinit var imgJadwalPreview: ImageView
    private lateinit var layoutJadwalPlaceholder: LinearLayout
    private lateinit var tvJadwalFileName: TextView
    private lateinit var btnUploadJadwal: MaterialButton
    private lateinit var progressUploadJadwal: ProgressBar

    private lateinit var btnDownloadRekap: MaterialButton
    private lateinit var progressDownloadRekap: ProgressBar
    private lateinit var tvDownloadStatus: TextView
    private lateinit var tvRekapDesc: TextView
    private lateinit var tvRekapTitle: TextView

    private lateinit var actvKaryawanHome: AutoCompleteTextView
    private lateinit var actvBulanHome: AutoCompleteTextView

    private val karyawanList = mutableListOf<String>()
    private var selectedUserHome = "ALL"

    // ── Filter bulan: index 0–11 (sesuai Calendar.MONTH), tahun integer ──────
    private var selectedMonthIndex = Calendar.getInstance().get(Calendar.MONTH)   // 0 = Januari
    private var selectedYear       = Calendar.getInstance().get(Calendar.YEAR)

    private val cachedUsers = mutableMapOf<String, String>()

    @Volatile private var isViewDestroyed = false

    // ── Nama-nama bulan Indonesia ─────────────────────────────────────────────
    private val namaBulanList = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                val compressedFile = compressImageFromUri(uri)
                if (compressedFile != null) {
                    uploadJadwalToCloudinary(compressedFile)
                } else {
                    Toast.makeText(requireContext(), "Gagal membaca gambar", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_admin_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isViewDestroyed = false

        initViews(view)
        loadAdminData()

        btnUploadJadwal.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/jpg", "image/png"))
            }
            pickImageLauncher.launch(Intent.createChooser(intent, "Pilih Gambar Jadwal"))
        }

        btnDownloadRekap.setOnClickListener {
            downloadRekapPdf()
        }

        loadKaryawanAndData()
        loadJadwalPreview()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isViewDestroyed = true
        mainHandler.removeCallbacksAndMessages(null)
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    private fun initViews(view: View) {
        tvAdminName         = view.findViewById(R.id.tvAdminName)
        tvAdminDate         = view.findViewById(R.id.tvAdminDate)
        tvTotalKaryawan     = view.findViewById(R.id.tvTotalKaryawan)

        tvTodayHadir        = view.findViewById(R.id.tvTodayHadir)
        tvTodayTelat        = view.findViewById(R.id.tvTodayTelat)
        tvTodayIzin         = view.findViewById(R.id.tvTodayIzin)
        tvTodayAlpa         = view.findViewById(R.id.tvTodayAlpa)

        tvMonthHadir        = view.findViewById(R.id.tvMonthHadir)
        tvMonthSakit        = view.findViewById(R.id.tvMonthSakit)
        tvMonthIzin         = view.findViewById(R.id.tvMonthIzin)
        tvMonthTukarShift   = view.findViewById(R.id.tvMonthTukarShift)
        tvMonthTelat        = view.findViewById(R.id.tvMonthTelat)
        tvMonthAlpa         = view.findViewById(R.id.tvMonthAlpa)

        progressKehadiran   = view.findViewById(R.id.progressKehadiran)
        tvPersentaseHadir   = view.findViewById(R.id.tvPersentaseHadir)

        imgJadwalPreview        = view.findViewById(R.id.imgJadwalPreview)
        layoutJadwalPlaceholder = view.findViewById(R.id.layoutJadwalPlaceholder)
        tvJadwalFileName        = view.findViewById(R.id.tvJadwalFileName)
        btnUploadJadwal         = view.findViewById(R.id.btnUploadJadwal)
        progressUploadJadwal    = view.findViewById(R.id.progressUploadJadwal)

        btnDownloadRekap        = view.findViewById(R.id.btnDownloadRekap)
        progressDownloadRekap   = view.findViewById(R.id.progressDownloadRekap)
        tvDownloadStatus        = view.findViewById(R.id.tvDownloadStatus)
        tvRekapDesc             = view.findViewById(R.id.tvRekapDesc)
        tvRekapTitle            = view.findViewById(R.id.tvRekapTitle)

        actvKaryawanHome        = view.findViewById(R.id.actvKaryawanHome)
        actvBulanHome           = view.findViewById(R.id.actvBulanHome)

        setupBulanDropdown()
    }

    private fun loadAdminData() {
        val sharedPref = requireActivity().getSharedPreferences("USER_SESSION", 0)
        tvAdminName.text = sharedPref.getString("USERNAME", "Admin")
        tvAdminDate.text = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID")).format(Date())
    }

    // ── Dropdown Bulan ────────────────────────────────────────────────────────

    private fun setupBulanDropdown() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val bulanDisplayList = namaBulanList.map { "$it $currentYear" }

        // Gunakan filter=false agar semua 12 item selalu muncul,
        // tidak difilter berdasarkan teks yang sudah ada di field.
        val adapter = object : ArrayAdapter<String>(
            requireContext(),
            R.layout.item_dropdown_karyawan,
            bulanDisplayList
        ) {
            override fun getFilter() = object : android.widget.Filter() {
                override fun performFiltering(constraint: CharSequence?) = FilterResults().apply {
                    values = bulanDisplayList
                    count  = bulanDisplayList.size
                }
                override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                    notifyDataSetChanged()
                }
            }
        }
        actvBulanHome.setAdapter(adapter)

        // Default: bulan berjalan
        val defaultText = "${namaBulanList[selectedMonthIndex]} $currentYear"
        actvBulanHome.setText(defaultText, false)

        actvBulanHome.setOnItemClickListener { adapterView, _, position, _ ->
            // Ambil teks item yang dipilih lalu cocokkan ke namaBulanList
            // agar index selalu akurat meski adapter difilter ulang.
            val selectedText = adapterView.getItemAtPosition(position).toString()
            val monthIdx = namaBulanList.indexOfFirst { selectedText.startsWith(it) }
            if (monthIdx >= 0) {
                selectedMonthIndex = monthIdx
                selectedYear       = currentYear
            }
            updateRekapTitle()
            loadMonthlyStatsFromCache()
        }
    }

    // ── Helper: format bulan yang dipilih sebagai "yyyy-MM" ───────────────────

    private fun getSelectedMonthPrefix(): String {
        return String.format("%04d-%02d", selectedYear, selectedMonthIndex + 1)
    }

    // ── Update judul rekap ────────────────────────────────────────────────────

    private fun updateRekapTitle() {
        val namaBulan    = namaBulanList[selectedMonthIndex]
        val namaKaryawan = if (selectedUserHome == "ALL") "Semua Karyawan" else selectedUserHome
        tvRekapTitle.text = "Rekap Bulan $namaBulan ($namaKaryawan)"
        tvRekapDesc.text  = "Rekap absensi $namaKaryawan bulan $namaBulan $selectedYear"
    }

    // ── Data Karyawan & Statistik ─────────────────────────────────────────────

    private fun loadKaryawanAndData() {
        db.collection("users").whereEqualTo("role", "Karyawan").get()
            .addOnSuccessListener { result ->
                if (!isAdded) return@addOnSuccessListener

                cachedUsers.clear()
                karyawanList.clear()
                karyawanList.add("Semua Karyawan")

                val totalUser = result.size()
                tvTotalKaryawan.text = "Jumlah Karyawan: $totalUser Orang"

                for (doc in result) {
                    val name = doc.getString("username") ?: continue
                    if (name.isNotEmpty()) {
                        cachedUsers[doc.id] = name
                        karyawanList.add(name)
                    }
                }

                setupKaryawanDropdownAdapter()
                updateRekapTitle()

                if (totalUser == 0) {
                    resetTodayStats()
                    updateMonthStats(0, 0, 0, 0, 0, 0)
                    return@addOnSuccessListener
                }

                loadAllStats(cachedUsers)
            }
    }

    private fun setupKaryawanDropdownAdapter() {
        val adapter = object : ArrayAdapter<String>(
            requireContext(),
            R.layout.item_dropdown_karyawan,
            karyawanList
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                val selectedText = actvKaryawanHome.text.toString()
                view.setTextColor(ContextCompat.getColor(context,
                    if (view.text == selectedText) R.color.orange_bold else R.color.black))
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                val selectedText = actvKaryawanHome.text.toString()
                view.setTextColor(ContextCompat.getColor(context,
                    if (view.text == selectedText) R.color.orange_bold else R.color.black))
                return view
            }
        }

        actvKaryawanHome.setAdapter(adapter)
        actvKaryawanHome.setText(karyawanList[0], false)
        selectedUserHome = "ALL"

        actvKaryawanHome.setOnItemClickListener { _, _, position, _ ->
            selectedUserHome = if (position == 0) "ALL" else karyawanList[position]
            updateRekapTitle()
            loadMonthlyStatsFromCache()
        }
    }

    private fun loadAllStats(users: Map<String, String>) {
        val todayStr      = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val selectedMonth = getSelectedMonthPrefix()
        val totalUser     = users.size

        val processedUser   = AtomicInteger(0)
        val todayHadir      = AtomicInteger(0)
        val todayTelat      = AtomicInteger(0)
        val todayIzin       = AtomicInteger(0)
        val monthHadir      = AtomicInteger(0)
        val monthTelat      = AtomicInteger(0)
        val monthIzin       = AtomicInteger(0)
        val monthSakit      = AtomicInteger(0)
        val monthTukarShift = AtomicInteger(0)
        val monthAlpa       = AtomicInteger(0)

        for ((userId, username) in users) {
            db.collection("absensi").document(userId).collection("records").get()
                .addOnSuccessListener { records ->
                    if (!isAdded) return@addOnSuccessListener

                    val tanggalDenganRecord = mutableSetOf<String>()
                    val excusedDates        = mutableSetOf<String>()
                    var alpaDariDb = 0

                    for (doc in records) {
                        val tanggal = doc.getString("tanggal") ?: continue
                        val status  = doc.getString("status")  ?: continue
                        val type    = doc.getString("type")    ?: continue

                        if (tanggal == todayStr && type == "masuk") {
                            when (status) {
                                "Hadir" -> todayHadir.incrementAndGet()
                                "Telat" -> todayTelat.incrementAndGet()
                                "Izin", "Sakit" -> todayIzin.incrementAndGet()
                            }
                        }

                        if (tanggal.startsWith(selectedMonth) && type == "masuk") {
                            val shouldCount = selectedUserHome == "ALL" || selectedUserHome == username
                            if (shouldCount) {
                                tanggalDenganRecord.add(tanggal)
                                val isTukar = doc.getBoolean("tukarShift") ?: false
                                val approvalStatus = doc.getString("approvalStatus") ?: ""

                                // Pending tukar shift dianggap Alpa sementara
                                val effectiveStatus = if (isTukar && approvalStatus == "pending") "Alpa" else status

                                when (effectiveStatus) {
                                    "Hadir" -> monthHadir.incrementAndGet()
                                    "Telat" -> monthTelat.incrementAndGet()
                                    "Alpa"  -> alpaDariDb++
                                    "Sakit" -> { monthSakit.incrementAndGet(); excusedDates.add(tanggal) }
                                    "Izin"  -> { monthIzin.incrementAndGet();  excusedDates.add(tanggal) }
                                }
                                // Hitung Tukar Shift hanya yang sudah disetujui
                                if (isTukar && approvalStatus == "approved") monthTukarShift.incrementAndGet()
                            }
                        }
                    }

                    val shouldCountAlpa = selectedUserHome == "ALL" || selectedUserHome == username
                    if (shouldCountAlpa) {
                        monthAlpa.addAndGet(alpaDariDb)
                        val maxDay = getMaxDayForSelectedMonth()
                        for (i in 1..maxDay) {
                            val date = "$selectedMonth-${String.format("%02d", i)}"
                            if (!tanggalDenganRecord.contains(date) && !excusedDates.contains(date)) {
                                monthAlpa.incrementAndGet()
                            }
                        }
                    }

                    if (processedUser.incrementAndGet() == totalUser) {
                        val th = todayHadir.get(); val tt = todayTelat.get(); val ti = todayIzin.get()
                        val ta = totalUser - (th + tt + ti)
                        val hadirValid = th + tt
                        val totalAktif = totalUser - ti
                        val persen     = if (totalAktif > 0) (hadirValid * 100 / totalAktif) else 0

                        mainHandler.post {
                            if (!isAdded) return@post
                            tvTodayHadir.text = th.toString()
                            tvTodayTelat.text = tt.toString()
                            tvTodayIzin.text  = ti.toString()
                            tvTodayAlpa.text  = ta.toString()
                            progressKehadiran.progress = persen
                            tvPersentaseHadir.text = "$persen%"
                            updateMonthStats(
                                monthHadir.get(), monthTelat.get(), monthIzin.get(),
                                monthSakit.get(), monthTukarShift.get(), monthAlpa.get()
                            )
                        }
                    }
                }
                .addOnFailureListener {
                    if (processedUser.incrementAndGet() == totalUser) {
                        mainHandler.post {
                            if (!isAdded) return@post
                            val ta = totalUser - (todayHadir.get() + todayTelat.get() + todayIzin.get())
                            tvTodayAlpa.text = ta.toString()
                            updateMonthStats(
                                monthHadir.get(), monthTelat.get(), monthIzin.get(),
                                monthSakit.get(), monthTukarShift.get(), monthAlpa.get()
                            )
                        }
                    }
                }
        }
    }

    private fun loadMonthlyStatsFromCache() {
        val selectedMonth = getSelectedMonthPrefix()

        val usersToQuery: Map<String, String> = if (selectedUserHome == "ALL") {
            cachedUsers
        } else {
            cachedUsers.filterValues { it == selectedUserHome }
        }

        val totalToProcess = usersToQuery.size
        if (totalToProcess == 0) { updateMonthStats(0, 0, 0, 0, 0, 0); return }

        val processedUser   = AtomicInteger(0)
        val monthHadir      = AtomicInteger(0)
        val monthTelat      = AtomicInteger(0)
        val monthIzin       = AtomicInteger(0)
        val monthSakit      = AtomicInteger(0)
        val monthTukarShift = AtomicInteger(0)
        val monthAlpa       = AtomicInteger(0)

        for ((userId, _) in usersToQuery) {
            db.collection("absensi").document(userId).collection("records").get()
                .addOnSuccessListener { records ->
                    if (!isAdded) return@addOnSuccessListener

                    val tanggalDenganRecord = mutableSetOf<String>()
                    val excusedDates        = mutableSetOf<String>()
                    var alpaDariDb = 0

                    for (doc in records) {
                        val tanggal = doc.getString("tanggal") ?: continue
                        if (!tanggal.startsWith(selectedMonth)) continue
                        val type = doc.getString("type") ?: continue
                        if (type != "masuk") continue
                        val status  = doc.getString("status")  ?: continue
                        val isTukar = doc.getBoolean("tukarShift") ?: false

                        tanggalDenganRecord.add(tanggal)
                        val approvalStatus = doc.getString("approvalStatus") ?: ""

                        // Pending tukar shift dianggap Alpa sementara
                        val effectiveStatus = if (isTukar && approvalStatus == "pending") "Alpa" else status

                        when (effectiveStatus) {
                            "Hadir" -> monthHadir.incrementAndGet()
                            "Telat" -> monthTelat.incrementAndGet()
                            "Alpa"  -> alpaDariDb++
                            "Sakit" -> { monthSakit.incrementAndGet(); excusedDates.add(tanggal) }
                            "Izin"  -> { monthIzin.incrementAndGet();  excusedDates.add(tanggal) }
                        }
                        // Hitung Tukar Shift hanya yang sudah disetujui
                        if (isTukar && approvalStatus == "approved") monthTukarShift.incrementAndGet()
                    }

                    monthAlpa.addAndGet(alpaDariDb)

                    val maxDay = getMaxDayForSelectedMonth()
                    for (i in 1..maxDay) {
                        val date = "$selectedMonth-${String.format("%02d", i)}"
                        if (!tanggalDenganRecord.contains(date) && !excusedDates.contains(date)) {
                            monthAlpa.incrementAndGet()
                        }
                    }

                    if (processedUser.incrementAndGet() == totalToProcess) {
                        mainHandler.post {
                            if (!isAdded) return@post
                            updateMonthStats(
                                monthHadir.get(), monthTelat.get(), monthIzin.get(),
                                monthSakit.get(), monthTukarShift.get(), monthAlpa.get()
                            )
                        }
                    }
                }
                .addOnFailureListener {
                    if (processedUser.incrementAndGet() == totalToProcess) {
                        mainHandler.post {
                            if (!isAdded) return@post
                            updateMonthStats(
                                monthHadir.get(), monthTelat.get(), monthIzin.get(),
                                monthSakit.get(), monthTukarShift.get(), monthAlpa.get()
                            )
                        }
                    }
                }
        }
    }

    /**
     * Hitung batas hari maksimal untuk filter bulan.
     * - Jika bulan yang dipilih = bulan & tahun berjalan → pakai hari ini (seperti sebelumnya)
     * - Jika bulan lampau → pakai jumlah hari penuh bulan tersebut
     * - Jika bulan mendatang → 0 (belum ada data)
     */
    private fun getMaxDayForSelectedMonth(): Int {
        val now     = Calendar.getInstance()
        val nowYear = now.get(Calendar.YEAR)
        val nowMonth = now.get(Calendar.MONTH) // 0-based

        return when {
            selectedYear == nowYear && selectedMonthIndex == nowMonth -> {
                now.get(Calendar.DAY_OF_MONTH)
            }
            selectedYear < nowYear || (selectedYear == nowYear && selectedMonthIndex < nowMonth) -> {
                val cal = Calendar.getInstance()
                cal.set(selectedYear, selectedMonthIndex, 1)
                cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            }
            else -> 0 // bulan mendatang
        }
    }

    private fun resetTodayStats() {
        tvTodayHadir.text = "0"; tvTodayTelat.text = "0"
        tvTodayIzin.text  = "0"; tvTodayAlpa.text  = "0"
        progressKehadiran.progress = 0; tvPersentaseHadir.text = "0%"
    }

    private fun updateMonthStats(hadir: Int, telat: Int, izin: Int, sakit: Int, tukarShift: Int, alpa: Int) {
        if (!isAdded) return
        tvMonthHadir.text      = hadir.toString()
        tvMonthTelat.text      = telat.toString()
        tvMonthIzin.text       = izin.toString()
        tvMonthSakit.text      = sakit.toString()
        tvMonthTukarShift.text = tukarShift.toString()
        tvMonthAlpa.text       = alpa.toString()
    }

    // ── Download Rekap PDF ────────────────────────────────────────────────────

    data class RekapRecord(
        val tanggal: String, val jamMasuk: String, val jamKeluar: String,
        val status: String, val keterangan: String, val tukarShift: Boolean
    )

    data class KaryawanRekap(val nama: String, val records: List<RekapRecord>)

    private fun downloadRekapPdf() {
        if (!isAdded) return

        btnDownloadRekap.isEnabled = false
        progressDownloadRekap.visibility = View.VISIBLE
        tvDownloadStatus.visibility = View.VISIBLE
        tvDownloadStatus.text = "Mengambil data karyawan..."

        val selectedMonth = getSelectedMonthPrefix()
        val namaBulan     = "${namaBulanList[selectedMonthIndex]} $selectedYear"
        val namaBulanFile = "${namaBulanList[selectedMonthIndex]}_$selectedYear"

        // Filter karyawan sesuai pilihan dropdown
        val usersForPdf: Map<String, String> = if (selectedUserHome == "ALL") {
            if (cachedUsers.isNotEmpty()) cachedUsers else emptyMap()
        } else {
            cachedUsers.filterValues { it == selectedUserHome }
        }

        if (usersForPdf.isNotEmpty()) {
            generatePdfFromUsers(usersForPdf, selectedMonth, namaBulan, namaBulanFile)
        } else if (selectedUserHome == "ALL") {
            // Fallback: ambil dari Firestore jika cache kosong
            db.collection("users").whereEqualTo("role", "Karyawan").get()
                .addOnSuccessListener { users ->
                    if (!isAdded) return@addOnSuccessListener
                    val userMap = mutableMapOf<String, String>()
                    for (doc in users) {
                        val name = doc.getString("username") ?: continue
                        userMap[doc.id] = name
                    }
                    if (userMap.isEmpty()) {
                        resetDownloadUI()
                        Toast.makeText(requireContext(), "Tidak ada data karyawan", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }
                    generatePdfFromUsers(userMap, selectedMonth, namaBulan, namaBulanFile)
                }
                .addOnFailureListener { e ->
                    if (!isAdded) return@addOnFailureListener
                    resetDownloadUI()
                    Toast.makeText(requireContext(), "Gagal mengambil data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            resetDownloadUI()
            Toast.makeText(requireContext(), "Data karyawan tidak ditemukan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generatePdfFromUsers(
        users: Map<String, String>,
        selectedMonth: String,
        namaBulan: String,
        namaBulanFile: String
    ) {
        val totalUser        = users.size
        val allKaryawanRekap = Collections.synchronizedList(mutableListOf<KaryawanRekap>())
        val processedCount   = AtomicInteger(0)

        val maxDay = getMaxDayForSelectedMonth()

        for ((userId, namaKaryawan) in users) {
            mainHandler.post {
                if (isAdded) tvDownloadStatus.text = "Mengambil data: $namaKaryawan..."
            }

            db.collection("absensi").document(userId).collection("records").get()
                .addOnSuccessListener { records ->
                    if (!isAdded) return@addOnSuccessListener

                    val byDate       = mutableMapOf<String, MutableMap<String, Any?>>()
                    val excusedDates = mutableSetOf<String>()

                    for (doc in records) {
                        val tanggal    = doc.getString("tanggal")    ?: continue
                        if (!tanggal.startsWith(selectedMonth)) continue
                        val type       = doc.getString("type")       ?: ""
                        val waktu      = doc.getString("waktu")      ?: "-"
                        val status     = doc.getString("status")     ?: "-"
                        val keterangan = doc.getString("keterangan") ?: ""
                        val tukarShift = doc.getBoolean("tukarShift") ?: false

                        if (type == "masuk" && (status == "Izin" || status == "Sakit")) {
                            excusedDates.add(tanggal)
                        }

                        if (!byDate.containsKey(tanggal)) {
                            byDate[tanggal] = mutableMapOf(
                                "tanggal" to tanggal, "jamMasuk" to "-",
                                "jamKeluar" to "-", "status" to status,
                                "keterangan" to keterangan, "tukarShift" to tukarShift
                            )
                        }
                        val entry = byDate[tanggal]!!
                        when (type) {
                            "masuk"  -> {
                                entry["jamMasuk"]   = waktu
                                entry["status"]     = status
                                entry["keterangan"] = keterangan
                                entry["tukarShift"] = tukarShift
                            }
                            "keluar" -> entry["jamKeluar"] = waktu
                        }
                    }

                    for (i in 1..maxDay) {
                        val date = "$selectedMonth-${String.format("%02d", i)}"
                        if (!byDate.containsKey(date) && !excusedDates.contains(date)) {
                            byDate[date] = mutableMapOf(
                                "tanggal"    to date,
                                "jamMasuk"   to "-",
                                "jamKeluar"  to "-",
                                "status"     to "Alpa",
                                "keterangan" to "",
                                "tukarShift" to false
                            )
                        }
                    }

                    val rekapList = byDate.values.map { e ->
                        RekapRecord(
                            tanggal    = e["tanggal"]    as? String  ?: "",
                            jamMasuk   = e["jamMasuk"]   as? String  ?: "-",
                            jamKeluar  = e["jamKeluar"]  as? String  ?: "-",
                            status     = e["status"]     as? String  ?: "-",
                            keterangan = e["keterangan"] as? String  ?: "",
                            tukarShift = e["tukarShift"] as? Boolean ?: false
                        )
                    }.sortedBy { it.tanggal }

                    allKaryawanRekap.add(KaryawanRekap(namaKaryawan, rekapList))

                    if (processedCount.incrementAndGet() == totalUser) {
                        mainHandler.post { if (isAdded) tvDownloadStatus.text = "Membuat file PDF..." }
                        val sortedRekap = allKaryawanRekap.sortedBy { it.nama }
                        executor.execute { generateAndSavePdf(sortedRekap, namaBulan, namaBulanFile) }
                    }
                }
                .addOnFailureListener {
                    val fallbackRekap = mutableListOf<RekapRecord>()
                    for (i in 1..maxDay) {
                        val date = "$selectedMonth-${String.format("%02d", i)}"
                        fallbackRekap.add(RekapRecord(date, "-", "-", "Alpa", "", false))
                    }
                    allKaryawanRekap.add(KaryawanRekap(namaKaryawan, fallbackRekap))

                    if (processedCount.incrementAndGet() == totalUser) {
                        mainHandler.post { if (isAdded) tvDownloadStatus.text = "Membuat file PDF..." }
                        val sortedRekap = allKaryawanRekap.sortedBy { it.nama }
                        executor.execute { generateAndSavePdf(sortedRekap, namaBulan, namaBulanFile) }
                    }
                }
        }
    }

    private fun generateAndSavePdf(allRekap: List<KaryawanRekap>, namaBulan: String, namaBulanFile: String) {
        try {
            val fileName = "Rekap_Absensi_$namaBulanFile.pdf"
            val pdfFile  = File(requireContext().externalCacheDir, fileName)
            val pdfDoc   = PdfDocument(PdfWriter(FileOutputStream(pdfFile)))
            val document = Document(pdfDoc, PageSize.A4.rotate())

            val colorBlue      = DeviceRgb(26, 86, 219)
            val colorLightBlue = DeviceRgb(219, 234, 254)
            val colorGray      = DeviceRgb(248, 249, 250)
            val colorGreen     = DeviceRgb(39, 174, 96)
            val colorOrange    = DeviceRgb(230, 126, 34)
            val colorRed       = DeviceRgb(226, 75, 74)
            val colorPurple    = DeviceRgb(155, 89, 182)
            val colorWhite     = DeviceRgb(255, 255, 255)

            // Judul PDF mencerminkan filter yang aktif
            val filterLabel = if (selectedUserHome == "ALL") "Semua Karyawan" else selectedUserHome
            document.add(Paragraph("REKAP ABSENSI KARYAWAN YWM").setFontSize(16f).setBold()
                .setFontColor(colorBlue).setTextAlignment(TextAlignment.CENTER))
            document.add(Paragraph("Bulan $namaBulan — $filterLabel").setFontSize(12f)
                .setFontColor(DeviceRgb(100, 100, 100)).setTextAlignment(TextAlignment.CENTER).setMarginBottom(6f))
            document.add(Paragraph("Dicetak: ${SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id","ID")).format(Date())}")
                .setFontSize(9f).setFontColor(DeviceRgb(150,150,150)).setTextAlignment(TextAlignment.CENTER).setMarginBottom(20f))

            for (karyawan in allRekap) {
                document.add(Paragraph(karyawan.nama).setFontSize(11f).setBold()
                    .setFontColor(colorWhite).setBackgroundColor(colorBlue)
                    .setPadding(6f).setMarginTop(16f).setMarginBottom(0f))

                if (karyawan.records.isEmpty()) {
                    document.add(Paragraph("   Tidak ada data absensi bulan ini.")
                        .setFontSize(9f).setFontColor(DeviceRgb(150,150,150)).setItalic().setMarginBottom(8f))
                    continue
                }

                val table = Table(UnitValue.createPercentArray(floatArrayOf(5f,13f,12f,12f,12f,14f,22f,10f)))
                    .useAllAvailableWidth()

                for (header in listOf("No","Tanggal","Hari","Jam Masuk","Jam Keluar","Status","Keterangan","Tukar Shift")) {
                    table.addHeaderCell(Cell().add(Paragraph(header).setFontSize(9f).setBold().setFontColor(colorBlue))
                        .setBackgroundColor(colorLightBlue).setTextAlignment(TextAlignment.CENTER).setPadding(5f))
                }

                val sdfParse = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val sdfDate  = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val sdfHari  = SimpleDateFormat("EEEE", Locale("id", "ID"))

                karyawan.records.forEachIndexed { index, rec ->
                    val rowBg       = if (index % 2 == 0) colorWhite else colorGray
                    val displayDate = try { sdfDate.format(sdfParse.parse(rec.tanggal)!!) } catch (e: Exception) { rec.tanggal }
                    val displayHari = try { sdfHari.format(sdfParse.parse(rec.tanggal)!!) } catch (e: Exception) { "-" }
                    val statusColor = when (rec.status) {
                        "Hadir" -> colorGreen; "Telat" -> colorOrange
                        "Alpa"  -> colorRed;   "Izin"  -> colorPurple
                        "Sakit" -> colorGreen;  else   -> DeviceRgb(100,100,100)
                    }

                    fun tc(text: String, align: TextAlignment = TextAlignment.CENTER, color: DeviceRgb? = null): Cell {
                        val p = Paragraph(text).setFontSize(8.5f)
                        if (color != null) p.setFontColor(color).setBold()
                        return Cell().add(p).setBackgroundColor(rowBg).setTextAlignment(align)
                            .setPaddingTop(4f).setPaddingBottom(4f).setPaddingLeft(4f).setPaddingRight(4f)
                    }

                    table.addCell(tc("${index+1}"))
                    table.addCell(tc(displayDate))
                    table.addCell(tc(displayHari))
                    table.addCell(tc(rec.jamMasuk))
                    table.addCell(tc(rec.jamKeluar))
                    table.addCell(tc(rec.status, TextAlignment.CENTER, statusColor))
                    table.addCell(tc(rec.keterangan.ifEmpty { "-" }, TextAlignment.LEFT))
                    table.addCell(tc(if (rec.tukarShift) "Ya" else "-", TextAlignment.CENTER, if (rec.tukarShift) colorOrange else null))
                }

                document.add(table)
                document.add(Paragraph("   Ringkasan: Hadir=${karyawan.records.count{it.status=="Hadir"}}  " +
                        "Telat=${karyawan.records.count{it.status=="Telat"}}  Izin=${karyawan.records.count{it.status=="Izin"}}  " +
                        "Sakit=${karyawan.records.count{it.status=="Sakit"}}  Alpa=${karyawan.records.count{it.status=="Alpa"}}  " +
                        "Tukar Shift=${karyawan.records.count{it.tukarShift}}")
                    .setFontSize(8.5f).setFontColor(DeviceRgb(80,80,80))
                    .setBackgroundColor(colorGray).setPadding(5f).setMarginBottom(4f))
            }

            document.close()
            mainHandler.post {
                if (!isAdded) return@post
                saveToDownloadsAndOpen(pdfFile, "Rekap_Absensi_$namaBulanFile.pdf")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            mainHandler.post {
                if (!isAdded) return@post
                resetDownloadUI()
                Toast.makeText(requireContext(), "Gagal membuat PDF: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveToDownloadsAndOpen(sourceFile: File, fileName: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val cv = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val resolver = requireContext().contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { sourceFile.inputStream().copyTo(it) }
                    cv.clear(); cv.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(uri, cv, null, null)
                    resetDownloadUI()
                    Toast.makeText(requireContext(), "PDF tersimpan di folder Downloads!", Toast.LENGTH_LONG).show()
                    openPdf(uri)
                }
            } else {
                val destFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName)
                sourceFile.copyTo(destFile, overwrite = true)
                val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", destFile)
                resetDownloadUI()
                Toast.makeText(requireContext(), "PDF tersimpan di folder Downloads!", Toast.LENGTH_LONG).show()
                openPdf(uri)
            }
        } catch (e: Exception) {
            e.printStackTrace(); resetDownloadUI()
            Toast.makeText(requireContext(), "Gagal menyimpan file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun openPdf(uri: Uri) {
        try {
            startActivity(Intent.createChooser(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
                }, "Buka PDF dengan..."
            ))
        } catch (e: Exception) { /* Tidak ada PDF viewer */ }
    }

    private fun resetDownloadUI() {
        if (!isAdded) return
        btnDownloadRekap.isEnabled = true
        progressDownloadRekap.visibility = View.GONE
        tvDownloadStatus.visibility = View.GONE
    }

    // ── Jadwal ────────────────────────────────────────────────────────────────

    private fun loadJadwalPreview() {
        db.collection("jadwal").document("current").get()
            .addOnSuccessListener { doc ->
                if (!isAdded || isViewDestroyed) return@addOnSuccessListener
                if (doc.exists()) {
                    val imageUrl = doc.getString("imageUrl") ?: return@addOnSuccessListener
                    val fileName = doc.getString("fileName") ?: ""
                    if (imageUrl.isNotEmpty()) {
                        tvJadwalFileName.text = "File: $fileName"
                        btnUploadJadwal.text  = "Upload Jadwal Baru"
                        loadImageFromUrl(imageUrl)
                    }
                }
            }
    }

    private fun loadImageFromUrl(url: String) {
        if (!isAdded || isViewDestroyed) return
        Glide.with(this@AdminHomeFragment)
            .load(url)
            .override(1080, 1080)
            .centerInside()
            .listener(object : RequestListener<android.graphics.drawable.Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<android.graphics.drawable.Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    if (!isAdded || isViewDestroyed) return false
                    layoutJadwalPlaceholder.visibility = View.VISIBLE
                    imgJadwalPreview.visibility        = View.GONE
                    return false
                }

                override fun onResourceReady(
                    resource: android.graphics.drawable.Drawable,
                    model: Any,
                    target: Target<android.graphics.drawable.Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    if (!isAdded || isViewDestroyed) return false
                    layoutJadwalPlaceholder.visibility = View.GONE
                    imgJadwalPreview.visibility        = View.VISIBLE
                    tvJadwalFileName.visibility        = View.VISIBLE
                    return false
                }
            })
            .into(imgJadwalPreview)
    }

    // ── Compress & Upload ─────────────────────────────────────────────────────

    private fun compressImageFromUri(uri: Uri): File? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            val maxWidth = 1080
            val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val (w, h) = if (bitmap.width > bitmap.height)
                Pair(maxWidth, (maxWidth / ratio).toInt())
            else
                Pair((maxWidth * ratio).toInt(), maxWidth)

            val resized = Bitmap.createScaledBitmap(bitmap, w, h, true)
            val file    = File(requireContext().externalCacheDir, "jadwal_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { resized.compress(Bitmap.CompressFormat.JPEG, 75, it) }
            file
        } catch (e: Exception) { null }
    }

    private fun uploadJadwalToCloudinary(file: File) {
        btnUploadJadwal.isEnabled        = false
        progressUploadJadwal.visibility  = View.VISIBLE

        MediaManager.get().upload(file.path)
            .unsigned("WredhaMulya")
            .option("folder", "jadwalKerja")
            .option("quality", "auto:good")
            .option("fetch_format", "auto")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {}
                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {}

                override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                    val imageUrl = resultData?.get("secure_url")?.toString() ?: return
                    val publicId = resultData["public_id"]?.toString() ?: ""
                    val fileName = file.name

                    db.collection("jadwal").document("current").set(
                        mapOf("imageUrl" to imageUrl, "publicId" to publicId,
                            "fileName" to fileName, "uploadedAt" to System.currentTimeMillis())
                    ).addOnSuccessListener {
                        if (!isAdded) return@addOnSuccessListener
                        progressUploadJadwal.visibility = View.GONE
                        btnUploadJadwal.isEnabled       = true
                        btnUploadJadwal.text            = "Upload Jadwal Baru"
                        tvJadwalFileName.text           = "File: $fileName"
                        tvJadwalFileName.visibility     = View.VISIBLE
                        Toast.makeText(requireContext(), "Jadwal berhasil diupload!", Toast.LENGTH_SHORT).show()
                        loadImageFromUrl(imageUrl)
                    }.addOnFailureListener { e ->
                        if (!isAdded) return@addOnFailureListener
                        progressUploadJadwal.visibility = View.GONE
                        btnUploadJadwal.isEnabled       = true
                        Toast.makeText(requireContext(), "Gagal simpan data: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onError(requestId: String?, error: ErrorInfo?) {
                    if (!isAdded) return
                    progressUploadJadwal.visibility = View.GONE
                    btnUploadJadwal.isEnabled       = true
                    Toast.makeText(requireContext(), "Upload gagal: ${error?.description}", Toast.LENGTH_LONG).show()
                }

                override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
            })
            .dispatch()
    }
}