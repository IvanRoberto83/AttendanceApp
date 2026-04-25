package com.example.absenywm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "ReminderReceiver"
        // WakeLock statis agar tidak di-GC sebelum selesai
        private var wakeLock: PowerManager.WakeLock? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive() dipanggil! notif_id=${intent.getIntExtra("notif_id", -1)}")

        // goAsync() mencegah Android membunuh receiver sebelum selesai
        val pendingResult = goAsync()

        // WakeLock mencegah CPU tidur saat kita memproses alarm
        acquireWakeLock(context)

        try {
            val notifId = intent.getIntExtra("notif_id", 0)
            val hour    = intent.getIntExtra("hour", 8)
            val minute  = intent.getIntExtra("minute", 0)

            createChannelIfNeeded(context)
            showNotification(context, notifId)

            // Jadwalkan ulang untuk besok (kecuali alarm test)
            if (hour >= 0) {
                rescheduleAlarm(context, hour, minute, notifId)
            }

            Log.d(TAG, "onReceive() selesai untuk notif_id=$notifId")
        } finally {
            // Wajib dipanggil agar sistem tahu broadcast sudah selesai
            pendingResult.finish()
            releaseWakeLock()
        }
    }

    private fun acquireWakeLock(context: Context) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AbsenYWM::ReminderWakeLock"
        ).also {
            it.acquire(10_000L) // max 10 detik, cukup untuk tampilkan notif
        }
        Log.d(TAG, "WakeLock acquired")
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
        Log.d(TAG, "WakeLock released")
    }

    private fun showNotification(context: Context, notifId: Int) {
        val openIntent = Intent(context, AbsenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openPendingIntent = PendingIntent.getActivity(
            context, notifId, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, DismissReceiver::class.java).apply {
            putExtra("notif_id", notifId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context, notifId + 1000, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, "reminder_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Reminder Absensi")
            .setContentText("Jangan lupa absen!")
            .setPriority(NotificationCompat.PRIORITY_MAX) // Ubah ke MAX
            .setCategory(NotificationCompat.CATEGORY_ALARM) // Tambahkan kategori alarm
            .setFullScreenIntent(openPendingIntent, true) // !!! Ini trik agar notifikasi muncul di lockscreen
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notifId, notification)
        Log.d(TAG, "Notifikasi ditampilkan, notif_id=$notifId")
    }

    private fun createChannelIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel("reminder_channel") == null) {
                val channel = NotificationChannel(
                    "reminder_channel",
                    "Reminder Absensi",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { enableVibration(true) }
                manager.createNotificationChannel(channel)
                Log.d(TAG, "Notification channel dibuat")
            }
        }
    }

    private fun rescheduleAlarm(context: Context, hour: Int, minute: Int, requestCode: Int) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DATE, 1)
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("notif_id", requestCode)
            putExtra("hour", hour)
            putExtra("minute", minute)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmInfo = AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent)
        alarmManager.setAlarmClock(alarmInfo, pendingIntent)

        Log.d(TAG, "Alarm dijadwal ulang: ${hour}:${String.format("%02d", minute)} besok")
    }
}