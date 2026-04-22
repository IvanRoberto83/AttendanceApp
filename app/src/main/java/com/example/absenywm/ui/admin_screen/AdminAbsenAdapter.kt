package com.example.absenywm.ui.admin_screen

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.absenywm.R

class AdminAbsenAdapter(
    private val list: MutableList<AdminListViewModel>
) : RecyclerView.Adapter<AdminAbsenAdapter.ViewHolder>() {

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

        holder.tvNama.text = item.nama
        holder.tvTanggal.text = item.tanggal
        holder.tvType.text = if (item.status == "Alpa") {
            "Tidak melakukan absensi"
        } else {
            item.type
        }
        holder.tvWaktu.text = item.waktu
        holder.tvStatus.text = if (item.status == "Alpa") {
            "Alpa"
        } else {
            item.status
        }
        holder.tvLinkFoto.text = item.foto ?: "Tidak ada foto"

        holder.tvLinkFoto.setOnClickListener {
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

        holder.tvStatus.setBackgroundResource(bg)

        holder.tvStatus.setTextColor(
            ContextCompat.getColor(holder.itemView.context, R.color.white)
        )
    }

    fun updateData(newList: List<AdminListViewModel>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }
}