package com.example.util

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.work.WorkManager
import java.io.File

/**
 * Kullanıcı "Hesabımı Sil" dediğinde cihazdaki tüm yerel BİRİZ izlerini
 * (tercihler, önbellek, bildirimler, arka plan işleri ve geçici dosyalar) eksiksiz temizler.
 */
object AppDataWipeHelper {
    private const val TAG = "AppDataWipeHelper"

    private val KNOWN_BIRIZ_PREFS = listOf(
        "biriz_app_settings",
        "biriz_star_rewards_prefs",
        "biriz_update_prefs",
        "biriz_update_notification_prefs",
        "biriz_chat_emoji_prefs",
        "biriz_communication_storage",
        "biriz_group_maker_storage",
        "biriz_library_prefs",
        "biriz_lucky_student_storage",
        "biriz_notification_prefs",
        "biriz_schedule_storage",
        "biriz_seating_storage",
        "biriz_smart_board_storage",
        "biriz_tournaments_prefs",
        "biriz_web_migration_prefs"
    )

    fun wipeAllLocalAppData(context: Context) {
        val appCtx = context.applicationContext ?: context

        // 1. Akıllı Tahta ekran paylaşımı & canlı yansıtma önbelleğini durdur ve sıfırla
        try {
            SmartBoardScreenCastService.stopServiceOnly(appCtx)
        } catch (e: Exception) {
            Log.w(TAG, "Failed stopping SmartBoardScreenCastService", e)
        }
        try {
            SmartBoardHelper.resetLocalState(appCtx)
        } catch (e: Exception) {
            Log.w(TAG, "Failed resetting SmartBoardHelper", e)
        }

        // 2. Arka plan senkronizasyon işlerini ve bildirimleri iptal et
        try {
            BackgroundNotificationSyncHelper.cancelAllBackgroundSync(appCtx)
        } catch (e: Exception) {
            Log.w(TAG, "Failed cancelling BackgroundNotificationSyncHelper", e)
        }
        try {
            WorkManager.getInstance(appCtx).cancelAllWork()
        } catch (e: Exception) {
            Log.w(TAG, "Failed cancelling WorkManager jobs", e)
        }
        try {
            NotificationManagerCompat.from(appCtx).cancelAll()
        } catch (e: Exception) {
            Log.w(TAG, "Failed cancelling notifications", e)
        }

        // 3. Bilinen ve biriz_ ile başlayan tüm SharedPreferences dosyalarını temizle ve sil
        val prefNames = KNOWN_BIRIZ_PREFS.toMutableSet()
        try {
            val prefsDir = File(appCtx.applicationInfo.dataDir, "shared_prefs")
            if (prefsDir.exists() && prefsDir.isDirectory) {
                prefsDir.listFiles()?.forEach { file ->
                    val name = file.name.removeSuffix(".xml")
                    if (name.startsWith("biriz_", ignoreCase = true)) {
                        prefNames.add(name)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed enumerating shared_prefs directory", e)
        }

        prefNames.forEach { prefName ->
            try {
                appCtx.getSharedPreferences(prefName, Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .commit()
            } catch (e: Exception) {
                Log.w(TAG, "Failed clearing prefs $prefName", e)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    appCtx.deleteSharedPreferences(prefName)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed deleting prefs $prefName", e)
                }
            } else {
                try {
                    val xmlFile = File(File(appCtx.applicationInfo.dataDir, "shared_prefs"), "$prefName.xml")
                    if (xmlFile.exists()) xmlFile.delete()
                } catch (_: Exception) {}
            }
        }

        // 4. Uygulama önbellek ve geçici dosyalarını temizle (ör. güncelleme APK'ları, paylaşılan geçici belgeler)
        try {
            appCtx.cacheDir?.listFiles()?.forEach { child ->
                try {
                    child.deleteRecursively()
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed clearing cacheDir", e)
        }
        try {
            appCtx.externalCacheDir?.listFiles()?.forEach { child ->
                try {
                    child.deleteRecursively()
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }
}
