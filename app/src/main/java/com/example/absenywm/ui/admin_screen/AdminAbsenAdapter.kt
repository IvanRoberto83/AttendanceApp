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
        val tvNama: TextView = itemView.findViewById(R.id.tvNama)
        val tvTanggal: TextView = itemView.findViewById(R.id.tvTanggal)
        val tvType: TextView = itemView.findViewById(R.id.tvType)
        val tvWaktu: TextView = itemView.findViewById(R.id.tvWaktu)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvLinkFoto: TextView = itemView.findViewById(R.id.tvLinkFoto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_attendance, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item = list[position]

        holder.tvNama.text = item.username
        holder.tvTanggal.text = item.tanggal

        holder.tvType.text = if (item.status == "Alpa") {
            "Tidak melakukan absensi"
        } else item.type

        holder.tvWaktu.text = item.waktu
        holder.tvStatus.text = item.status
        holder.tvLinkFoto.text = item.foto ?: "Tidak ada foto"

        holder.tvLinkFoto.setOnClickListener {
            val url = item.foto
            if (!url.isNullOrEmpty()) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                holder.itemView.context.startActivity(intent)
            } else {
                Toast.makeText(holder.itemView.context, "Foto tidak tersedia", Toast.LENGTH_SHORT).show()
            }
        }

        val bg = when (item.status.lowercase()) {
            "hadir" -> R.drawable.bg_status_hadir
            "telat" -> R.drawable.bg_status_telat
            "alpa" -> R.drawable.bg_status_alpa
            "sakit" -> R.drawable.bg_status_sakit
            "izin" -> R.drawable.bg_status_izin
            else -> R.drawable.bg_status_hadir
        }

        holder.tvStatus.setBackgroundResource(bg)
        holder.tvStatus.setTextColor(
            ContextCompat.getColor(holder.itemView.context, R.color.white)
        )

        holder.itemView.setOnClickListener {
            showEditDialog(holder.itemView, item)
        }
    }

    private fun showEditDialog(view: View, item: AdminListViewModel) {

        val options = arrayOf("Hadir", "Telat", "Izin", "Sakit", "Alpa")

        AlertDialog.Builder(view.context)
            .setTitle("Edit Status (${item.username})")
            .setItems(options) { _, which ->
                val selectedStatus = options[which]
                updateStatus(view, item, selectedStatus)
            }
            .show()
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
                    val shiftMasuk = userDoc.getString("shiftMasuk") ?: "08:00 - 15:00"
                    val waktuShift = shiftMasuk.split(" - ")[0]

                    val newDocId = "${item.tanggal}_masuk"
                    val dataBaru = mapOf(
                        "tanggal" to item.tanggal,
                        "status" to newStatus,
                        "type" to "masuk",
                        "waktu" to waktuShift,
                        "shiftMasuk" to shiftMasuk,
                        "createdByAdmin" to true
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