package com.example.absenywm.ui.list_screen

import android.view.LayoutInflater
import android.view.ViewGroup
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

        holder.binding.tvTanggal.text = formatTanggal(item.tanggal)
        holder.binding.tvType.text = item.type.replaceFirstChar { it.uppercase() }
        holder.binding.tvWaktu.text = item.waktu
        holder.binding.tvStatus.text = item.status

        val bg = when (item.status) {
            "Hadir" -> R.drawable.bg_status_hadir
            "Telat" -> R.drawable.bg_status_telat
            "Sakit" -> R.drawable.bg_status_sakit
            "Izin" -> R.drawable.bg_status_izin
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