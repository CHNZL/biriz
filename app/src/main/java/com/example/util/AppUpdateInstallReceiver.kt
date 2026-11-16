package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.widget.Toast

/**
 * PackageInstaller oturumundan dönen kurulum sonucunu yakalar.
 * Kullanıcı onayı gerektiren ilk kurulumlarda ara hazırlık ekranı olmadan doğrudan
 * sistem onay penceresini açar; başarılı kurulumda geçici dosyaları ve bildirimleri temizler.
 */
class AppUpdateInstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val statusMessage = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        val apkPath = intent.getStringExtra(EXTRA_APK_PATH)
        val versionCode = intent.getIntExtra(EXTRA_VERSION_CODE, -1)

        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmIntent: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                if (confirmIntent != null) {
                    confirmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try {
                        context.startActivity(confirmIntent)
                    } catch (e: Exception) {
                        AppUpdateDownloadController.revertInstallingToReady(apkPath, versionCode)
                        Toast.makeText(
                            context,
                            "Kurulum onay ekranı açılamadı: ${e.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    AppUpdateDownloadController.revertInstallingToReady(apkPath, versionCode)
                }
            }

            PackageInstaller.STATUS_SUCCESS -> {
                AppUpdateNotificationHelper.cancelUpdateNotification(context)
                AppUpdateManager.cleanOldApkFiles(context)
                AppUpdateDownloadController.markIdle()
            }

            PackageInstaller.STATUS_FAILURE_ABORTED -> {
                AppUpdateDownloadController.revertInstallingToReady(apkPath, versionCode)
            }

            else -> {
                AppUpdateDownloadController.revertInstallingToReady(apkPath, versionCode)
                val detail = statusMessage?.takeIf { it.isNotBlank() } ?: "Kurulum tamamlanamadı."
                Toast.makeText(
                    context,
                    "Güncelleme başlatılamadı: $detail",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    companion object {
        const val ACTION_INSTALL_RESULT = "com.example.action.APP_UPDATE_INSTALL_RESULT"
        const val EXTRA_APK_PATH = "extra_install_apk_path"
        const val EXTRA_VERSION_CODE = "extra_install_version_code"
    }
}
