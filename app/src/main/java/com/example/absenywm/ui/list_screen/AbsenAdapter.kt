package com.example.absenywm.ui.list_screen

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.absenywm.R
import com.example.absenywm.databinding.ItemAttendanceBinding
import java.text.SimpleDateFormat
import java.util.*

class AbsenAdapter(private val list: MutableList<AbsenModel>) :
    RecyclerView.Adapter<AbsenAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemAttendanceBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAttendanceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val item = list[position]
        val isAlpa = item.status.equals("Alpa", true)
        val isAlpaWithRecord = isAlpa && item.waktu.isNotEmpty() && item.waktu != "-"

        holder.binding.tvTanggal.text = formatTanggal(item.tanggal)

        holder.binding.tvType.text = when {
            // Tukar Shift pending: tampilkan keterangan khusus
            item.tukarShift && item.approvalStatus == "pending"  -> "Pengajuan Tukar Shift"
            item.tukarShift && item.approvalStatus == "approved" -> item.type
            item.tukarShift && item.approvalStatus == "rejected" -> item.type
            isAlpaWithRecord -> "Absensi tidak sesuai shift"
            isAlpa           -> "Tidak melakukan absensi"
            else             -> item.type
        }

        holder.binding.tvWaktu.text = if (item.shiftLabel.isNotEmpty()) "${item.waktu} • ${item.shiftLabel}" else item.waktu

        // Tampilkan status berdasarkan approvalStatus
        // Untuk approved: baca status aktual dari Firebase (Hadir/Telat/Alpa sesuai hasil pengecekan shift)
        holder.binding.tvStatus.text = when {
            item.tukarShift && item.approvalStatus == "pending"  -> "Menunggu Persetujuan"
            item.tukarShift && item.approvalStatus == "approved" -> "${item.status}"
            isAlpa -> "Alpa"
            else   -> item.status
        }

        holder.binding.tvLinkFoto.text = if (!item.foto.isNullOrEmpty()) "Foto Absensi" else "Tidak ada foto"

        holder.binding.tvLinkFoto.setOnClickListener {
            val url = item.foto
            if (!url.isNullOrEmpty()) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                holder.itemView.context.startActivity(intent)
            } else {
                Toast.makeText(
                    holder.itemView.context,
                    "Foto tidak tersedia",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Warna background status badge mengikuti status aktual setelah approved
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
        holder.binding.tvStatus.setBackgroundResource(bg)

        // Badge Tukar Shift: tampilkan dengan teks berbeda sesuai approval
        if (item.tukarShift) {
            holder.binding.tvBadgeTukarShift.visibility = View.VISIBLE
            when (item.approvalStatus) {
                "approved" -> {
                    holder.binding.tvBadgeTukarShift.text = "✓ Tukar Shift"
                    holder.binding.tvBadgeTukarShift.setBackgroundResource(R.drawable.bg_status_tukarshift)
                }
                "rejected" -> {
                    holder.binding.tvBadgeTukarShift.text = "✗ Ditolak"
                    holder.binding.tvBadgeTukarShift.setBackgroundResource(R.drawable.bg_status_alpa)
                }
                else -> {
                    holder.binding.tvBadgeTukarShift.text = "Tukar Shift"
                    holder.binding.tvBadgeTukarShift.setBackgroundResource(R.drawable.bg_status_tukarshift)
                }
            }
        } else {
            holder.binding.tvBadgeTukarShift.visibility = View.GONE
        }
    }

    fun updateData(newList: List<AbsenModel>) {
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