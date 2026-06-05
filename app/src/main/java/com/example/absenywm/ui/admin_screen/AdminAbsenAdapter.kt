package com.example.absenywm.ui.admin_screen

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.cloudinary.android.MediaManager
import com.example.absenywm.R
import com.google.firebase.firestore.FirebaseFirestore

class AdminAbsenAdapter(
    private val list: MutableList<AdminListViewModel>,
    private val onDataChanged: () -> Unit
) : RecyclerView.Adapter<AdminAbsenAdapter.ViewHolder>() {

    private val db = FirebaseFirestore.getInstance()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNama: TextView           = itemView.findViewById(R.id.tvNama)
        val tvTanggal: TextView        = itemView.findViewById(R.id.tvTanggal)
        val tvType: TextView           = itemView.findViewById(R.id.tvType)
        val tvWaktu: TextView          = itemView.findViewById(R.id.tvWaktu)
        val tvStatus: TextView         = itemView.findViewById(R.id.tvStatus)
        val tvLinkFoto: TextView       = itemView.findViewById(R.id.tvLinkFoto)
        val tvBadgeTukarShift: TextView = itemView.findViewById(R.id.tvBadgeTukarShift)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_attendance, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item = list[position]
        val isAlpa = item.status.equals("Alpa", true)
        val isAlpaWithRecord = isAlpa && item.waktu.isNotEmpty() && item.waktu != "-"
        val isPending = item.tukarShift && item.approvalStatus == "pending"

        holder.tvNama.text    = item.username
        holder.tvTanggal.text = formatTanggal(item.tanggal)

        holder.tvType.text = when {
            isPending        -> "Pengajuan Tukar Shift (Menunggu)"
            isAlpaWithRecord -> "Absensi tidak sesuai jadwal shift"
            isAlpa           -> "Tidak melakukan absensi"
            else             -> item.type
        }

        holder.tvWaktu.text = if (item.shiftLabel.isNotEmpty()) "${item.waktu} • ${item.shiftLabel}" else item.waktu

        // Teks status: approved mengikuti status aktual dari Firebase (Hadir/Telat/Alpa)
        holder.tvStatus.text = when {
            item.tukarShift && item.approvalStatus == "pending"  -> "Pengajuan"
            item.tukarShift && item.approvalStatus == "approved" -> "${item.status}"
            else -> item.status
        }

        holder.tvLinkFoto.text = if (!item.foto.isNullOrEmpty()) "Foto Absensi" else "Tidak ada foto"

        holder.tvLinkFoto.setOnClickListener {
            val url = item.foto
            if (!url.isNullOrEmpty()) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                holder.itemView.context.startActivity(intent)
            } else {
                Toast.makeText(holder.itemView.context, "Foto tidak tersedia", Toast.LENGTH_SHORT).show()
            }
        }

        // Background status badge mengikuti status aktual setelah approved
        val bg = when {
            item.tukarShift && item.approvalStatus == "pending"  -> R.drawable.bg_status_pending
            item.tukarShift && item.approvalStatus == "approved" -> when (item.status.lowercase()) {
                "hadir" -> R.drawable.bg_status_hadir
                "telat" -> R.drawable.bg_status_telat
                "alpa"  -> R.drawable.bg_status_alpa
                "sakit" -> R.drawable.bg_status_sakit
                "izin"  -> R.drawable.bg_status_izin
                else    -> R.drawable.bg_status_hadir
            }
            item.tukarShift && item.approvalStatus == "rejected" -> R.drawable.bg_status_alpa
            else -> when (item.status.lowercase()) {
                "hadir" -> R.drawable.bg_status_hadir
                "telat" -> R.drawable.bg_status_telat
                "alpa"  -> R.drawable.bg_status_alpa
                "sakit" -> R.drawable.bg_status_sakit
                "izin"  -> R.drawable.bg_status_izin
                else    -> R.drawable.bg_status_hadir
            }
        }

        holder.tvStatus.setBackgroundResource(bg)
        holder.tvStatus.setTextColor(
            ContextCompat.getColor(holder.itemView.context, R.color.white)
        )

        // Badge Tukar Shift
        if (item.tukarShift) {
            holder.tvBadgeTukarShift.visibility = View.VISIBLE
            when (item.approvalStatus) {
                "approved" -> {
                    holder.tvBadgeTukarShift.text = "✓ Tukar Shift"
                    holder.tvBadgeTukarShift.setBackgroundResource(R.drawable.bg_status_tukarshift)
                }
                "rejected" -> {
                    holder.tvBadgeTukarShift.text = "✗ Ditolak"
                    holder.tvBadgeTukarShift.setBackgroundResource(R.drawable.bg_status_alpa)
                }
                else -> {
                    holder.tvBadgeTukarShift.text = "Tukar Shift"
                    holder.tvBadgeTukarShift.setBackgroundResource(R.drawable.bg_status_tukarshift)
                }
            }
        } else {
            holder.tvBadgeTukarShift.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            // Jika pending tukar shift, arahkan ke dialog approval langsung
            if (isPending) {
                showApprovalDialog(holder.itemView, item)
            } else {
                showEditDialog(holder.itemView, item)
            }
        }
    }

    private fun formatTanggal(date: String): String {
        return try {
            val input  = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val output = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale("id", "ID"))
            val parsed = input.parse(date)
            output.format(parsed!!)
        } catch (e: Exception) {
            date
        }
    }

    /** Dialog approve/reject khusus untuk item pending tukar shift */
    private fun showApprovalDialog(view: View, item: AdminListViewModel) {
        val statusJika = item.statusJikaDisetujui.ifEmpty { "Hadir" }

        AlertDialog.Builder(view.context)
            .setTitle("Pengajuan Tukar Shift — ${item.username}")
            .setMessage(
                "Tanggal: ${formatTanggal(item.tanggal)}\n" +
                        "Shift pengganti: ${item.shiftPengganti ?: "-"}\n\n" +
                        "Setujui → status \"$statusJika\"\n" +
                        "Tolak → status \"Alpa\""
            )
            .setPositiveButton("✓ Setujui") { _, _ ->
                approveRequest(view, item)
            }
            .setNegativeButton("✗ Tolak") { _, _ ->
                rejectRequest(view, item)
            }
            .setNeutralButton("Batal", null)
            .show()
            .also { dialog ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(android.graphics.Color.parseColor("#27AE60"))
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(android.graphics.Color.parseColor("#E24B4A"))
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
                    .setTextColor(android.graphics.Color.BLACK)
            }
    }

    private fun approveRequest(view: View, item: AdminListViewModel) {
        val newStatus = item.statusJikaDisetujui.ifEmpty { "Hadir" }

        db.collection("absensi")
            .document(item.userId)
            .collection("records")
            .document(item.docId)
            .update(mapOf("approvalStatus" to "approved", "status" to newStatus))
            .addOnSuccessListener {
                Toast.makeText(view.context, "Disetujui → $newStatus", Toast.LENGTH_SHORT).show()
                onDataChanged()
            }
            .addOnFailureListener {
                Toast.makeText(view.context, "Gagal menyetujui", Toast.LENGTH_SHORT).show()
            }
    }

    private fun rejectRequest(view: View, item: AdminListViewModel) {
        db.collection("absensi")
            .document(item.userId)
            .collection("records")
            .document(item.docId)
            .update(mapOf("approvalStatus" to "rejected", "status" to "Alpa"))
            .addOnSuccessListener {
                Toast.makeText(view.context, "Pengajuan ditolak", Toast.LENGTH_SHORT).show()
                onDataChanged()
            }
            .addOnFailureListener {
                Toast.makeText(view.context, "Gagal menolak", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showEditDialog(view: View, item: AdminListViewModel) {
        val options = arrayOf("Hadir", "Telat", "Izin", "Sakit", "Alpa", "Tukar Shift")

        AlertDialog.Builder(view.context)
            .setTitle("Edit Status (${item.username})")
            .setItems(options) { _, which ->
                if (which == 5) {
                    showTukarShiftConfirmation(view, item)
                } else {
                    showConfirmationDialog(view, item, options[which])
                }
            }
            .show()
    }

    private fun showConfirmationDialog(view: View, item: AdminListViewModel, newStatus: String) {
        val dialog = AlertDialog.Builder(view.context)
            .setTitle("Konfirmasi Perubahan")
            .setMessage("Ubah status ${item.username} pada ${item.tanggal} menjadi \"$newStatus\"?")
            .setPositiveButton("Ubah") { _, _ ->
                updateStatus(view, item, newStatus)
            }
            .setNegativeButton("Batal", null)
            .show()

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            .setTextColor(android.graphics.Color.BLACK)
    }

    private fun showTukarShiftConfirmation(view: View, item: AdminListViewModel) {
        if (item.docId == "ALPA") {
            Toast.makeText(view.context, "Tidak bisa mengubah tukar shift pada record Alpa", Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = AlertDialog.Builder(view.context)
            .setTitle("Konfirmasi Tukar Shift")
            .setMessage(
                if (item.tukarShift)
                    "Hapus status tukar shift pada ${item.username} di tanggal ${item.tanggal}?"
                else
                    "Tambahkan status tukar shift pada ${item.username} di tanggal ${item.tanggal}?"
            )
            .setPositiveButton("Ubah") { _, _ ->
                updateTukarShift(view, item)
            }
            .setNegativeButton("Batal", null)
            .show()

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            .setTextColor(android.graphics.Color.BLACK)
    }

    private fun updateTukarShift(view: View, item: AdminListViewModel) {
        val context = view.context

        db.collection("absensi")
            .document(item.userId)
            .collection("records")
            .document(item.docId)
            .update("tukarShift", !item.tukarShift)
            .addOnSuccessListener {
                val msg = if (!item.tukarShift) "Tukar shift ditambahkan" else "Tukar shift dihapus"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                onDataChanged()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Gagal update tukar shift", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateStatus(view: View, item: AdminListViewModel, newStatus: String) {
        val context = view.context

        if (newStatus == "Alpa") {
            if (item.docId == "ALPA") {
                Toast.makeText(context, "Status sudah Alpa", Toast.LENGTH_SHORT).show()
                return
            }

            db.collection("absensi")
                .document(item.userId)
                .collection("records")
                .document(item.docId)
                .get()
                .addOnSuccessListener { doc ->
                    val publicId = doc.getString("public_id")

                    if (!publicId.isNullOrEmpty()) {
                        Thread {
                            try {
                                MediaManager.get().cloudinary
                                    .uploader()
                                    .destroy(publicId, mapOf("invalidate" to true))
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }.start()
                    }

                    db.collection("absensi")
                        .document(item.userId)
                        .collection("records")
                        .document(item.docId)
                        .delete()
                        .addOnSuccessListener {
                            Toast.makeText(context, "Status dikembalikan ke Alpa", Toast.LENGTH_SHORT).show()
                            onDataChanged()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Gagal update", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Gagal mengambil data", Toast.LENGTH_SHORT).show()
                }

            return
        }

        if (item.docId != "ALPA") {
            db.collection("absensi")
                .document(item.userId)
                .collection("records")
                .document(item.docId)
                .update("status", newStatus)
                .addOnSuccessListener {
                    Toast.makeText(context, "Berhasil update", Toast.LENGTH_SHORT).show()
                    onDataChanged()
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Gagal update", Toast.LENGTH_SHORT).show()
                }
        } else {
            db.collection("users").document(item.userId).get()
                .addOnSuccessListener { userDoc ->
                    val shiftMasuk  = userDoc.getString("shiftMasuk") ?: "08:00 - 15:00"
                    val waktuShift  = shiftMasuk.split(" - ")[0]

                    val newDocId  = "${item.tanggal}_masuk"
                    val dataBaru  = mapOf(
                        "tanggal"        to item.tanggal,
                        "status"         to newStatus,
                        "type"           to "masuk",
                        "waktu"          to waktuShift,
                        "shiftMasuk"     to shiftMasuk,
                        "createdByAdmin" to true,
                        "approvalStatus" to "",
                        "tukarShift"     to false
                    )

                    db.collection("absensi")
                        .document(item.userId)
                        .collection("records")
                        .document(newDocId)
                        .set(dataBaru)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Alpa berhasil diubah", Toast.LENGTH_SHORT).show()
                            onDataChanged()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Gagal update", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Gagal ambil data user", Toast.LENGTH_SHORT).show()
                }
        }
    }
}