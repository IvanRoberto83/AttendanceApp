package com.example.absenywm

import java.util.*

object TimeUtils {

    fun extractStartTime(shiftRange: String?): String {
        return try {
            shiftRange?.split(" - ")?.get(0)?.replace(".", ":") ?: "08:00"
        } catch (e: Exception) {
            "08:00"
        }
    }

    fun determineStatus(
        selectedStatus: String,
        shiftStart: String,
        serverTime: Date
    ): String {
        if (selectedStatus == "Izin" || selectedStatus == "Sakit") {
            return selectedStatus
        }

        val cal = Calendar.getInstance().apply { time = serverTime }
        val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        val parts = shiftStart.split(":")
        val shiftHour   = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val shiftMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val shiftMinutes = shiftHour * 60 + shiftMinute

        var diff = nowMinutes - shiftMinutes
        if (diff < -12 * 60) diff += 24 * 60
        if (diff > 12 * 60) diff -= 24 * 60

        return when {
            diff < 0         -> "Alpa"
            diff <= 30       -> "Hadir"
            diff <= 60       -> "Telat"
            else             -> "Alpa"
        }
    }

    fun isWithinShiftWindow(shiftStart: String, serverTime: Date): Boolean {
        val cal = Calendar.getInstance().apply { time = serverTime }
        val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        val parts = shiftStart.split(":")
        val shiftHour   = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val shiftMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val shiftMinutes = shiftHour * 60 + shiftMinute

        var diff = nowMinutes - shiftMinutes
        if (diff < -12 * 60) diff += 24 * 60
        if (diff > 12 * 60)  diff -= 24 * 60

        return diff in 0..60
    }

    fun isLate(shiftStart: String, serverTime: Date): Boolean {
        val cal = Calendar.getInstance().apply { time = serverTime }
        val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        val parts = shiftStart.split(":")
        val shiftHour   = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val shiftMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val shiftMinutes = shiftHour * 60 + shiftMinute

        var diff = nowMinutes - shiftMinutes
        if (diff < -12 * 60) diff += 24 * 60
        if (diff > 12 * 60)  diff -= 24 * 60

        return diff > 30
    }
}