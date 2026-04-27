package com.example.absenywm

import java.text.SimpleDateFormat
import java.util.*

object TimeUtils {

    fun extractStartTime(shiftRange: String?): String {
        return try {
            shiftRange?.split(" - ")?.get(0)?.replace(".", ":") ?: "08:00"
        } catch (e: Exception) {
            "08:00"
        }
    }

    fun isWithinAbsenTime(): Boolean {
        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val minute = now.get(Calendar.MINUTE)
        val nowMinutes = hour * 60 + minute

        val windows = listOf(
            8 * 60 to 9 * 60,
            15 * 60 to 16 * 60,
            22 * 60 to 23 * 60
        )

        return windows.any { (start, end) -> nowMinutes >= start && nowMinutes < end }
    }

    fun isLate(shiftStart: String, absenTime: String): Boolean {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())

        val shift = format.parse(shiftStart) ?: return false
        val absen = format.parse(absenTime.substring(0, 5)) ?: return false

        val batasHadir = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, shift.hours)
            set(Calendar.MINUTE, shift.minutes)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, 30)
        }

        val absenCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, absen.hours)
            set(Calendar.MINUTE, absen.minutes)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return absenCal.after(batasHadir)
    }
}