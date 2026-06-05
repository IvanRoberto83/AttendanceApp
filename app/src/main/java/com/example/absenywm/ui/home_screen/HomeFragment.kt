package com.example.absenywm.ui.home_screen

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.absenywm.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val mainHandler = Handler(Looper.getMainLooper())

    private var jadwalImageUrl: String? = null

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Nama user & tanggal
        val sharedPref = requireActivity().getSharedPreferences("USER_SESSION", AppCompatActivity.MODE_PRIVATE)
        binding.tvUserName.text = sharedPref.getString("USERNAME", "User")
        binding.tvDate.text = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID")).format(Date())

        // Tombol absen masuk
        binding.btnCheckIn.setOnClickListener {
            val userId = auth.currentUser?.uid ?: return@setOnClickListener
            db.collection("users").document(userId).get()
                .addOnSuccessListener { doc ->
                    if (!isAdded || _binding == null) return@addOnSuccessListener
                    val shift = doc.getString("shift") ?: "08:00 - 09:00"
                    startActivity(Intent(requireContext(), AbsensiActivity::class.java).apply {
                        putExtra("type", "masuk")
                        putExtra("shiftAsli", shift)
                    })
                }
        }

        // Tombol absen keluar
        binding.btnCheckOut.setOnClickListener {
            val userId = auth.currentUser?.uid ?: return@setOnClickListener
            db.collection("users").document(userId).get()
                .addOnSuccessListener { doc ->
                    if (!isAdded || _binding == null) return@addOnSuccessListener
                    val shift = doc.getString("shift") ?: "08:00 - 09:00"
                    startActivity(Intent(requireContext(), AbsensiActivity::class.java).apply {
                        putExtra("type", "keluar")
                        putExtra("shiftAsli", shift)
                    })
                }
        }

        // Tombol download jadwal
        binding.btnDownloadJadwal.setOnClickListener {
            downloadJadwal()
        }

        // State awal: sembunyikan gambar & tombol, tampilkan placeholder
        binding.btnDownloadJadwal.visibility       = View.GONE
        binding.imgJadwalUser.visibility           = View.GONE
        binding.layoutJadwalUserPlaceholder.visibility = View.VISIBLE

        loadJadwal()
    }

    override fun onResume() {
        super.onResume()
        if (isAdded && _binding != null) {
            loadTodayData()
            loadMonthlyStats()
        }
    }

    override fun onDestroyView() {
        // FIX: Glide terikat viewLifecycleOwner, otomatis cancel saat view destroyed.
        // Tidak perlu executor manual — cukup bersihkan handler.
        mainHandler.removeCallbacksAndMessages(null)
        _binding = null
        super.onDestroyView()
    }

    // ── Jadwal ────────────────────────────────────────────────────────────────

    private fun loadJadwal() {
        if (!isAdded || _binding == null) return

        // Tampilkan progress, sembunyikan tombol & gambar
        binding.progressJadwalUser.visibility         = View.VISIBLE
        binding.btnDownloadJadwal.visibility          = View.GONE
        binding.imgJadwalUser.visibility              = View.GONE
        binding.layoutJadwalUserPlaceholder.visibility = View.GONE

        db.collection("jadwal")
            .document("current")
            .get()
            .addOnSuccessListener { doc ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                // FIX: Log URL ke Logcat untuk memudahkan debug
                val imageUrl = doc.getString("imageUrl") ?: ""
                android.util.Log.d("JADWAL_DEBUG", "doc.exists=${doc.exists()}, imageUrl='$imageUrl'")

                if (!doc.exists() || imageUrl.isBlank()) {
                    showNoJadwal()
                    return@addOnSuccessListener
                }

                jadwalImageUrl = imageUrl
                loadImageFromUrl(imageUrl)
            }
            .addOnFailureListener { e ->
                android.util.Log.e("JADWAL_DEBUG", "Gagal ambil doc jadwal: ${e.message}")
                if (!isAdded || _binding == null) return@addOnFailureListener
                showNoJadwal()
            }
    }

    private fun loadImageFromUrl(url: String) {
        if (!isAdded || _binding == null) return

        android.util.Log.d("JADWAL_DEBUG", "Memuat gambar dari: $url")

        binding.progressJadwalUser.visibility = View.VISIBLE

        // FIX: Pakai this (Fragment) agar Glide terikat lifecycle fragment secara benar.
        Glide.with(this@HomeFragment)
            .load(url)
            .override(1080, 1080)   // FIX: Paksa ukuran eksplisit agar Glide tidak menunggu
            .centerInside()         // view selesai diukur — penyebab gambar tidak muncul
            .listener(object : RequestListener<android.graphics.drawable.Drawable> {

                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<android.graphics.drawable.Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    android.util.Log.e("JADWAL_DEBUG", "Glide gagal load: ${e?.message}")
                    // Log detail tiap root cause agar mudah di-debug
                    e?.logRootCauses("JADWAL_DEBUG")

                    if (!isAdded || _binding == null) return false
                    showNoJadwal()
                    return false
                }

                override fun onResourceReady(
                    resource: android.graphics.drawable.Drawable,
                    model: Any,
                    target: Target<android.graphics.drawable.Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    android.util.Log.d("JADWAL_DEBUG", "Glide berhasil load gambar. Source: $dataSource")

                    if (!isAdded || _binding == null) return false

                    // FIX: Tampilkan semua elemen yang diperlukan saat gambar berhasil dimuat.
                    // Sebelumnya btnDownloadJadwal tidak di-set VISIBLE di sini sehingga
                    // tombol download tidak pernah muncul meski gambar sudah tampil.
                    binding.progressJadwalUser.visibility         = View.GONE
                    binding.imgJadwalUser.visibility              = View.VISIBLE
                    binding.layoutJadwalUserPlaceholder.visibility = View.GONE
                    binding.btnDownloadJadwal.visibility          = View.VISIBLE

                    return false
                }
            })
            .into(binding.imgJadwalUser)
    }

    private fun showNoJadwal() {
        if (!isAdded || _binding == null) return

        binding.progressJadwalUser.visibility         = View.GONE
        binding.btnDownloadJadwal.visibility          = View.GONE
        binding.imgJadwalUser.visibility              = View.GONE
        binding.layoutJadwalUserPlaceholder.visibility = View.VISIBLE

        jadwalImageUrl = null
    }

    private fun downloadJadwal() {
        val url     = jadwalImageUrl ?: return
        val context = context ?: return
        try {
            val fileName = "jadwal_${System.currentTimeMillis()}.jpg"
            val request  = DownloadManager.Request(Uri.parse(url)).apply {
                setTitle("Jadwal Kerja")
                setDescription("Mengunduh gambar jadwal kerja...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }
            (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
            Toast.makeText(context, "Download dimulai. Cek folder Downloads.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal download: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ── Data Hari Ini ─────────────────────────────────────────────────────────

    private fun loadTodayData() {
        val userId = auth.currentUser?.uid ?: return
        val today  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val ref    = db.collection("absensi").document(userId).collection("records")

        var checkInTime   = "-"
        var checkOutTime  = "-"
        var statusHariIni = "Belum Absen"

        ref.document("${today}_masuk").get()
            .addOnSuccessListener { docMasukSnap ->
                if (!isAdded || _binding == null) return@addOnSuccessListener
                if (docMasukSnap.exists()) {
                    checkInTime   = docMasukSnap.getString("waktu")  ?: "-"
                    statusHariIni = docMasukSnap.getString("status") ?: "Hadir"
                }
                ref.document("${today}_keluar").get()
                    .addOnSuccessListener { docKeluarSnap ->
                        if (!isAdded || _binding == null) return@addOnSuccessListener
                        if (docKeluarSnap.exists()) {
                            checkOutTime = docKeluarSnap.getString("waktu") ?: "-"
                        }
                        binding.tvCheckTimes.text  = "Absen-masuk: $checkInTime | Absen-keluar: $checkOutTime"
                        binding.tvStatusBadge.text = statusHariIni
                    }
            }
    }

    // ── Statistik Bulanan ─────────────────────────────────────────────────────

    private fun loadMonthlyStats() {
        val userId       = auth.currentUser?.uid ?: return
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val startMonth = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)

        cal.add(Calendar.MONTH, 1)
        val nextMonth = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)

        db.collection("absensi")
            .document(userId)
            .collection("records")
            .whereGreaterThanOrEqualTo("tanggal", startMonth)
            .whereLessThan("tanggal", nextMonth)
            .get()
            .addOnSuccessListener { result ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                var hadir = 0; var sakit = 0; var izin = 0
                var telat = 0; var alpa  = 0; var tukarShiftCount = 0

                val datesWithMasukRecord = mutableSetOf<String>()
                val excusedDates         = mutableSetOf<String>()

                for (doc in result) {
                    val tanggal = doc.getString("tanggal") ?: continue
                    val type    = doc.getString("type")    ?: ""
                    if (!tanggal.startsWith(currentMonth)) continue
                    if (type != "masuk") continue
                    val status = doc.getString("status") ?: continue
                    datesWithMasukRecord.add(tanggal)
                    when (status) {
                        "Hadir" -> hadir++
                        "Telat" -> telat++
                        "Alpa"  -> alpa++
                        "Sakit" -> { sakit++; excusedDates.add(tanggal) }
                        "Izin"  -> { izin++;  excusedDates.add(tanggal) }
                    }
                    if (doc.getBoolean("tukarShift") == true) tukarShiftCount++
                }

                val todayDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                for (i in 1..todayDay) {
                    val date = "$currentMonth-${String.format("%02d", i)}"
                    if (!datesWithMasukRecord.contains(date) && !excusedDates.contains(date)) alpa++
                }

                if (!isAdded || _binding == null) return@addOnSuccessListener

                binding.tvCountHadir.text = hadir.toString()
                binding.tvCountSakit.text = sakit.toString()
                binding.tvCountIzin.text  = izin.toString()
                binding.tvCountTelat.text = telat.toString()
                binding.tvCountAlpa.text  = alpa.toString()
                binding.tvCountTukar.text = tukarShiftCount.toString()
            }
    }
}