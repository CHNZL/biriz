package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object AppUpdateNotificationHelper {

    const val CHANNEL_ID = "biriz_update_notifications"
    const val NOTIFICATION_ID = 2026

    const val EXTRA_OPEN_UPDATE_DIALOG = "extra_open_update_dialog"
    const val EXTRA_UPDATE_VERSION = "extra_update_version"
    const val EXTRA_DOWNLOAD_URL = "extra_download_url"
    const val EXTRA_RELEASE_NOTES = "extra_release_notes"

    private const val PREFS_NAME = "biriz_update_notification_prefs"
    private const val KEY_LAST_NOTIFIED_VERSION = "last_notified_version_code"

    /**
     * Android 8.0+ için bildirim kanalını oluşturur.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Uygulama Güncellemeleri",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "BİRİZ yeni sürüm ve otomatik güncelleme bildirimleri"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * Sistem bildirim çekmecesine/çubuğuna yeni güncelleme bildirimi gönderir.
     * Tıklandığında MainActivity'yi açarak doğrudan güncelleme yükleme penceresine yönlendirir.
     */
    fun showUpdateNotification(context: Context, updateInfo: UpdateInfo, forceResend: Boolean = false) {
        if (!updateInfo.isUpdateAvailable) return

        // Aynı sürüm için tekrar tekrar zil sesi/titreşimle rahatsız etmeyi engelle
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastNotified = prefs.getInt(KEY_LAST_NOTIFIED_VERSION, -1)
        if (!forceResend && lastNotified == updateInfo.latestVersionCode) {
            return
        }

        createNotificationChannel(context)

        // Android 13+ (API 33) için POST_NOTIFICATIONS izin kontrolü
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionStatus = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                // İzin henüz verilmemişse sistem bildirimi gönderilemez, uygulama içi afiş görünür
                return
            }
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_UPDATE_DIALOG, true)
            putExtra(EXTRA_UPDATE_VERSION, updateInfo.latestVersionName)
            putExtra(EXTRA_DOWNLOAD_URL, updateInfo.downloadUrl)
            putExtra(EXTRA_RELEASE_NOTES, updateInfo.releaseNotes)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cleanNotes = updateInfo.releaseNotes.lines()
            .filter { it.isNotBlank() }
            .take(3)
            .joinToString("\n") { "• ${it.trim().trimStart('-', '*', '•').trim()}" }

        val bigText = buildString {
            append("BİRİZ v${updateInfo.latestVersionName} yeni sürümü kullanıma hazır! 🎉\n")
            if (cleanNotes.isNotBlank()) {
                append("\nYenilikler:\n$cleanNotes\n")
            }
            append("\nYüklemek için dokunun.")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("BİRİZ - Yeni Güncelleme Yayında! 🚀")
            .setContentText("v${updateInfo.latestVersionName} hazır. Güncellemeyi yüklemek için dokunun.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.stat_sys_download,
                "Hemen Güncelle",
                pendingIntent
            )
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            prefs.edit().putInt(KEY_LAST_NOTIFIED_VERSION, updateInfo.latestVersionCode).apply()
        } catch (_: SecurityException) {
            // İzin verilmediyse güvenli şekilde yakala
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Güncelleme yüklendiğinde veya kullanıcı işlemi tamamladığında bildirimi kaldırır.
     */
    fun cancelUpdateNotification(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        } catch (_: Exception) {}
    }
}
