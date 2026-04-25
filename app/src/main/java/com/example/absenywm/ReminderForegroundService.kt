package com.example.absenywm

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class ReminderForegroundService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(9999, buildPersistentNotification())

        // Langsung set ulang semua reminder setiap kali service dimulai.
        // Tidak ada flag IS_SET — ini memastikan alarm selalu aktif
        // bahkan setelah reboot atau app update.
        ReminderHelper.setMultipleReminders(this)

        // Service boleh berhenti setelah set alarm karena AlarmManager
        // yang akan membangunkan app saat waktunya tiba.
        stopSelf()

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildPersistentNotification(): Notification {
        val channelId = "foreground_service_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Layanan Pengingat Absensi",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Reminder Absensi Aktif")
            .setContentText("Alarm absensi sudah terjadwal.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(false) // Boleh di-swipe karena service langsung berhenti
            .build()
    }
}