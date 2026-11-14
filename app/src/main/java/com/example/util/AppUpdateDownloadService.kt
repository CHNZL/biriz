package com.example.util

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import com.example.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Güncelleme penceresi kapatıldığında, uygulama arka plana alındığında veya ekran kilitlendiğinde
 * APK indirmesinin kesintiye uğramadan devam etmesini sağlayan Foreground Service (dataSync).
 */
class AppUpdateDownloadService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var downloadJob: Job? = null
    private var activeVersionCode: Int = -1

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CANCEL_DOWNLOAD -> {
                downloadJob?.cancel()
                downloadJob = null
                activeVersionCode = -1
                AppUpdateDownloadController.markIdle()
                AppUpdateNotificationHelper.cancelDownloadProgressNotification(this)
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_START_DOWNLOAD -> {
                val info = extractUpdateInfo(intent) ?: run {
                    ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return START_NOT_STICKY
                }

                // Aynı sürüm zaten indiriliyorsa tekrar başlatma
                if (downloadJob?.isActive == true && activeVersionCode == info.latestVersionCode) {
                    return START_NOT_STICKY
                }

                activeVersionCode = info.latestVersionCode
                startForegroundWithNotification(info, 0, 0L, -1L)

                downloadJob?.cancel()
                downloadJob = serviceScope.launch {
                    val wakeLock = acquirePartialWakeLock()
                    var lastNotifTime = 0L
                    var lastNotifPct = -1

                    try {
                        val apkFile = AppUpdateManager.downloadApk(
                            context = applicationContext,
                            downloadUrl = info.downloadUrl,
                            targetVersionCode = info.latestVersionCode,
                            onProgress = { progress, downloadedBytes, totalBytes ->
                                AppUpdateDownloadController.updateProgress(
                                    info = info,
                                    progress = progress,
                                    downloadedBytes = downloadedBytes,
                                    totalBytes = totalBytes
                                )
                                val pct = (progress * 100).toInt().coerceIn(0, 100)
                                val now = System.currentTimeMillis()
                                if (pct != lastNotifPct && (now - lastNotifTime >= 450L || pct == 100)) {
                                    lastNotifTime = now
                                    lastNotifPct = pct
                                    AppUpdateNotificationHelper.updateDownloadProgressNotification(
                                        context = applicationContext,
                                        info = info,
                                        progressPercent = pct,
                                        downloadedBytes = downloadedBytes,
                                        totalBytes = totalBytes
                                    )
                                }
                            }
                        )

                        if (apkFile != null && apkFile.exists() && apkFile.length() > 50_000L) {
                            AppUpdateDownloadController.markReady(info, apkFile)
                            ServiceCompat.stopForeground(
                                this@AppUpdateDownloadService,
                                ServiceCompat.STOP_FOREGROUND_REMOVE
                            )
                            AppUpdateNotificationHelper.cancelDownloadProgressNotification(applicationContext)
                            AppUpdateNotificationHelper.showDownloadCompleteNotification(
                                context = applicationContext,
                                updateInfo = info,
                                apkPath = apkFile.absolutePath
                            )

                            // Uygulama şu anda ön plandaysa (pencere kapalı olsa bile) doğrudan kurulumu başlat
                            if (AppUpdateDownloadController.isAppInForeground) {
                                AppUpdateDownloadController.lastAutoPromptedVersionCode = info.latestVersionCode
                                Handler(Looper.getMainLooper()).post {
                                    AppUpdateManager.installApk(applicationContext, apkFile)
                                }
                            }
                        } else {
                            val errMsg = "APK dosyası indirilemedi. İnternet bağlantınızı kontrol edip tekrar deneyin."
                            AppUpdateDownloadController.markFailed(info, errMsg)
                            ServiceCompat.stopForeground(
                                this@AppUpdateDownloadService,
                                ServiceCompat.STOP_FOREGROUND_REMOVE
                            )
                            AppUpdateNotificationHelper.showDownloadFailedNotification(applicationContext, info)
                        }
                    } catch (_: CancellationException) {
                        AppUpdateDownloadController.markIdle()
                        ServiceCompat.stopForeground(
                            this@AppUpdateDownloadService,
                            ServiceCompat.STOP_FOREGROUND_REMOVE
                        )
                        AppUpdateNotificationHelper.cancelDownloadProgressNotification(applicationContext)
                    } catch (e: Exception) {
                        val errMsg = e.localizedMessage?.takeIf { it.isNotBlank() }
                            ?: "Güncelleme indirilirken beklenmeyen bir ağ hatası oluştu."
                        AppUpdateDownloadController.markFailed(info, errMsg)
                        ServiceCompat.stopForeground(
                            this@AppUpdateDownloadService,
                            ServiceCompat.STOP_FOREGROUND_REMOVE
                        )
                        AppUpdateNotificationHelper.showDownloadFailedNotification(applicationContext, info)
                    } finally {
                        activeVersionCode = -1
                        if (wakeLock?.isHeld == true) {
                            try {
                                wakeLock.release()
                            } catch (_: Exception) {}
                        }
                        stopSelf(startId)
                    }
                }
                return START_REDELIVER_INTENT
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundWithNotification(
        info: UpdateInfo,
        pct: Int,
        downloadedBytes: Long,
        totalBytes: Long
    ) {
        val notification = AppUpdateNotificationHelper.buildDownloadProgressNotification(
            context = this,
            info = info,
            progressPercent = pct,
            downloadedBytes = downloadedBytes,
            totalBytes = totalBytes
        )
        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }
        ServiceCompat.startForeground(
            this,
            AppUpdateNotificationHelper.DOWNLOAD_NOTIFICATION_ID,
            notification,
            serviceType
        )
    }

    private fun acquirePartialWakeLock(): PowerManager.WakeLock? {
        return try {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Biriz:AppUpdateDownloadLock")?.apply {
                setReferenceCounted(false)
                acquire(10 * 60 * 1000L) // Güvenli 10 dakika üst sınırı
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun extractUpdateInfo(intent: Intent): UpdateInfo? {
        val url = intent.getStringExtra(EXTRA_DOWNLOAD_URL)?.trim().orEmpty()
        val versionName = intent.getStringExtra(EXTRA_VERSION_NAME)?.trim().orEmpty()
        val versionCode = intent.getIntExtra(EXTRA_VERSION_CODE, -1)
        if (url.isBlank() || versionName.isBlank() || versionCode <= 0) return null
        return UpdateInfo(
            isUpdateAvailable = true,
            currentVersionName = intent.getStringExtra(EXTRA_CURRENT_VERSION_NAME) ?: BuildConfig.VERSION_NAME,
            currentVersionCode = intent.getIntExtra(EXTRA_CURRENT_VERSION_CODE, BuildConfig.VERSION_CODE),
            latestVersionName = versionName,
            latestVersionCode = versionCode,
            downloadUrl = url,
            releaseNotes = intent.getStringExtra(EXTRA_RELEASE_NOTES).orEmpty(),
            releaseTag = intent.getStringExtra(EXTRA_RELEASE_TAG).orEmpty()
        )
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START_DOWNLOAD = "com.example.action.START_APP_UPDATE_DOWNLOAD"
        const val ACTION_CANCEL_DOWNLOAD = "com.example.action.CANCEL_APP_UPDATE_DOWNLOAD"

        const val EXTRA_VERSION_NAME = "extra_update_version_name"
        const val EXTRA_VERSION_CODE = "extra_update_version_code"
        const val EXTRA_CURRENT_VERSION_NAME = "extra_current_version_name"
        const val EXTRA_CURRENT_VERSION_CODE = "extra_current_version_code"
        const val EXTRA_DOWNLOAD_URL = "extra_update_download_url"
        const val EXTRA_RELEASE_NOTES = "extra_update_release_notes"
        const val EXTRA_RELEASE_TAG = "extra_update_release_tag"
    }
}
