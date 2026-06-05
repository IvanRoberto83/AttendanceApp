package com.example.absenywm.ui.admin_screen

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.absenywm.R
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class TukarShiftApprovalAdapter(
    private val list: MutableList<AdminListViewModel>,
    private val onActionDone: () -> Unit
) : RecyclerView.Adapter<TukarShiftApprovalAdapter.ViewHolder>() {

    private val db = FirebaseFirestore.getInstance()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNama: TextView           = itemView.findViewById(R.id.tvApprovalNama)
        val tvTanggal: TextView        = itemView.findViewById(R.id.tvApprovalTanggal)
        val tvShiftPengganti: TextView = itemView.findViewById(R.id.tvApprovalShiftPengganti)
        val tvWaktu: TextView          = itemView.findViewById(R.id.tvApprovalWaktu)
        val tvFoto: TextView           = itemView.findViewById(R.id.tvApprovalFoto)
        val btnSetujui: ImageButton    = itemView.findViewById(R.id.btnSetujui)
        val btnTolak: ImageButton      = itemView.findViewById(R.id.btnTolak)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tukarshift_approval, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]

        holder.tvNama.text = item.username
        holder.tvTanggal.text = formatTanggal(item.tanggal)
        holder.tvShiftPengganti.text = "Shift pengganti: ${item.shiftPengganti ?: "-"}"
        holder.tvWaktu.text = "Waktu absen: ${item.waktu}"
        holder.tvFoto.text = if (!item.foto.isNullOrEmpty()) "Lihat Foto Absensi" else "Tidak ada foto"

        holder.tvFoto.setOnClickListener {
            val url = item.foto
            if (!url.isNullOrEmpty()) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                holder.itemView.context.startActivity(intent)
            } else {
                Toast.makeText(holder.itemView.context, "Foto tidak tersedia", Toast.LENGTH_SHORT).show()
            }
        }

        holder.btnSetujui.setOnClickListener {
            showConfirmDialog(
                view         = holder.itemView,
                item         = item,
                isApprove    = true,
                message      = "Setujui pengajuan Tukar Shift ${item.username} pada ${formatTanggal(item.tanggal)}?\n\nStatus akan berubah menjadi \"${item.statusJikaDisetujui}\"."
            )
        }

        holder.btnTolak.setOnClickListener {
            showConfirmDialog(
                view         = holder.itemView,
                item         = item,
                isApprove    = false,
                message      = "Tolak pengajuan Tukar Shift ${item.username} pada ${formatTanggal(item.tanggal)}?\n\nStatus akan tetap menjadi \"Alpa\"."
            )
        }
    }

    private fun showConfirmDialog(view: View, item: AdminListViewModel, isApprove: Boolean, message: String) {
        val title = if (isApprove) "Setujui Pengajuan" else "Tolak Pengajuan"
        val btnText = if (isApprove) "Setujui" else "Tolak"

        val dialog = AlertDialog.Builder(view.context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(btnText) { _, _ ->
                if (isApprove) approveRequest(view, item) else rejectRequest(view, item)
            }
            .setNegativeButton("Batal", null)
            .show()

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            .setTextColor(android.graphics.Color.BLACK)

        // Warna tombol aksi
        val actionColor = if (isApprove)
            android.graphics.Color.parseColor("#27AE60")
        else
            android.graphics.Color.parseColor("#E24B4A")

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(actionColor)
    }

    /**
     * Admin menyetujui pengajuan Tukar Shift:
     * - approvalStatus → "approved"
     * - status → statusJikaDisetujui (Hadir / Telat)
     */
    private fun approveRequest(view: View, item: AdminListViewModel) {
        val newStatus = item.statusJikaDisetujui.ifEmpty { "Hadir" }

        db.collection("absensi")
            .document(item.userId)
            .collection("records")
            .document(item.docId)
            .update(
                mapOf(
                    "approvalStatus" to "approved",
                    "status"         to newStatus
                )
            )
            .addOnSuccessListener {
                Toast.makeText(
                    view.context,
                    "Pengajuan ${item.username} disetujui → $newStatus",
                    Toast.LENGTH_SHORT
                ).show()
                onActionDone()
            }
            .addOnFailureListener {
                Toast.makeText(view.context, "Gagal menyetujui pengajuan", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Admin menolak pengajuan Tukar Shift:
     * - approvalStatus → "rejected"
     * - status tetap "Alpa"
     */
    private fun rejectRequest(view: View, item: AdminListViewModel) {
        db.collection("absensi")
            .document(item.userId)
            .collection("records")
            .document(item.docId)
            .update(
                mapOf(
                    "approvalStatus" to "rejected",
                    "status"         to "Alpa"
                )
            )
            .addOnSuccessListener {
                Toast.makeText(
                    view.context,
                    "Pengajuan ${item.username} ditolak",
                    Toast.LENGTH_SHORT
                ).show()
                onActionDone()
            }
            .addOnFailureListener {
                Toast.makeText(view.context, "Gagal menolak pengajuan", Toast.LENGTH_SHORT).show()
            }
    }

    fun updateData(newList: List<AdminListViewModel>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }

    private fun formatTanggal(date: String): String {
        return try {
            val input = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val output = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
            val parsed = input.parse(date)
            output.format(parsed!!)
        } catch (e: Exception) {
            date
        }
    }
}
