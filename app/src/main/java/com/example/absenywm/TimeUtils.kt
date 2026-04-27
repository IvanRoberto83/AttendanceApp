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

    fun isWithinAbsenTime(serverTime: Date): Boolean {
        val cal = Calendar.getInstance().apply { time = serverTime }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val nowMinutes = hour * 60 + minute

        val windows = listOf(
            8 * 60 to 9 * 60,
            15 * 60 to 16 * 60,
            22 * 60 to 23 * 60
        )

        return windows.any { (start, end) ->
            nowMinutes in start until end
        }
    }

    fun isLate(shiftStart: String, serverTime: Date): Boolean {
        return try {
            val parts = shiftStart.split(":")
            val shiftHour = parts[0].toInt()
            val shiftMinute = parts[1].toInt()

            val batasHadir = Calendar.getInstance().apply {
                time = serverTime
                set(Calendar.HOUR_OF_DAY, shiftHour)
                set(Calendar.MINUTE, shiftMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.MINUTE, 30)
            }

            serverTime.after(batasHadir.time)

        } catch (e: Exception) {
            false
        }
    }
}