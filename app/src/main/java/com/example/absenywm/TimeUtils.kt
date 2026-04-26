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

    fun isWithinAbsenTime(shiftStart: String): Boolean {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        val shiftParsed = format.parse(shiftStart)!!

        val shiftCal = Calendar.getInstance()
        val tempCal = Calendar.getInstance().apply { time = shiftParsed }

        shiftCal.set(Calendar.HOUR_OF_DAY, tempCal.get(Calendar.HOUR_OF_DAY))
        shiftCal.set(Calendar.MINUTE, tempCal.get(Calendar.MINUTE))
        shiftCal.set(Calendar.SECOND, 0)

        val endCal = shiftCal.clone() as Calendar
        endCal.add(Calendar.HOUR_OF_DAY, 1)

        val now = Calendar.getInstance()
        return now.after(shiftCal) && now.before(endCal)
    }

    fun isLate(shiftStart: String, absenTime: String): Boolean {
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())

        val shift = format.parse(shiftStart)!!
        val absen = format.parse(absenTime.substring(0, 5))!!

        val batasHadir = Calendar.getInstance().apply {
            time = shift
            add(Calendar.MINUTE, 30)
        }

        return absen.after(batasHadir.time)
    }
}