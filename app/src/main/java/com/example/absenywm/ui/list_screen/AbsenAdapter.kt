package com.example.absenywm.ui.list_screen

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
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

        holder.binding.tvTanggal.text = formatTanggal(item.tanggal)

        holder.binding.tvType.text = if (isAlpa) {
            "Tidak melakukan absensi"
        } else {
            item.type
        }

        holder.binding.tvWaktu.text = item.waktu

        holder.binding.tvStatus.text = if (isAlpa) {
            "Alpa"
        } else {
            item.status
        }

        holder.binding.tvLinkFoto.text = item.foto ?: "Tidak ada foto"

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

        val bg = when (item.status.lowercase()) {
            "hadir" -> R.drawable.bg_status_hadir
            "telat" -> R.drawable.bg_status_telat
            "alpa" -> R.drawable.bg_status_alpa
            "sakit" -> R.drawable.bg_status_sakit
            "izin" -> R.drawable.bg_status_izin
            else -> R.drawable.bg_status_hadir
        }

        holder.binding.tvStatus.setBackgroundResource(bg)
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