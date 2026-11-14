package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.Locale

sealed interface BackgroundUpdateState {
    object Idle : BackgroundUpdateState

    data class Downloading(
        val info: UpdateInfo,
        val progress: Float,
        val downloadedBytes: Long = 0L,
        val totalBytes: Long = -1L
    ) : BackgroundUpdateState

    data class ReadyToInstall(
        val info: UpdateInfo,
        val apkFile: File
    ) : BackgroundUpdateState

    data class Failed(
        val info: UpdateInfo,
        val message: String
    ) : BackgroundUpdateState
}

/**
 * Pencere kapatılsa veya ekran kilitlense bile arka planda devam eden APK indirme durumunu
 * tüm uygulama ekranları ve bildirimlerle tek merkezden senkronize eder.
 */
object AppUpdateDownloadController {

    private val _state = MutableStateFlow<BackgroundUpdateState>(BackgroundUpdateState.Idle)
    val state: StateFlow<BackgroundUpdateState> = _state.asStateFlow()

    @Volatile
    var isAppInForeground: Boolean = false

    @Volatile
    var lastAutoPromptedVersionCode: Int = -1

    fun startDownload(context: Context, info: UpdateInfo) {
        if (!info.isUpdateAvailable) return

        // Aynı sürüm zaten indiriliyorsa yeniden başlatma
        val current = _state.value
        if (current is BackgroundUpdateState.Downloading &&
            current.info.latestVersionCode == info.latestVersionCode
        ) {
            return
        }

        // Aynı sürümün APK'sı cihazda zaten hazırsa tekrar indirmeden kurulum aşamasına geç
        val cachedApk = AppUpdateManager.getDownloadedApkForVersion(context, info.latestVersionCode)
        if (cachedApk != null) {
            _state.value = BackgroundUpdateState.ReadyToInstall(info, cachedApk)
            AppUpdateNotificationHelper.cancelDownloadProgressNotification(context)
            AppUpdateNotificationHelper.showDownloadCompleteNotification(context, info, cachedApk.absolutePath)
            if (isAppInForeground) {
                lastAutoPromptedVersionCode = info.latestVersionCode
                AppUpdateManager.installApk(context, cachedApk)
            }
            return
        }

        _state.value = BackgroundUpdateState.Downloading(
            info = info,
            progress = 0f,
            downloadedBytes = 0L,
            totalBytes = -1L
        )

        val intent = Intent(context, AppUpdateDownloadService::class.java).apply {
            action = AppUpdateDownloadService.ACTION_START_DOWNLOAD
            putExtra(AppUpdateDownloadService.EXTRA_VERSION_NAME, info.latestVersionName)
            putExtra(AppUpdateDownloadService.EXTRA_VERSION_CODE, info.latestVersionCode)
            putExtra(AppUpdateDownloadService.EXTRA_CURRENT_VERSION_NAME, info.currentVersionName)
            putExtra(AppUpdateDownloadService.EXTRA_CURRENT_VERSION_CODE, info.currentVersionCode)
            putExtra(AppUpdateDownloadService.EXTRA_DOWNLOAD_URL, info.downloadUrl)
            putExtra(AppUpdateDownloadService.EXTRA_RELEASE_NOTES, info.releaseNotes)
            putExtra(AppUpdateDownloadService.EXTRA_RELEASE_TAG, info.releaseTag)
        }
        ContextCompat.startForegroundService(context.applicationContext, intent)
    }

    fun cancelDownload(context: Context) {
        val intent = Intent(context, AppUpdateDownloadService::class.java).apply {
            action = AppUpdateDownloadService.ACTION_CANCEL_DOWNLOAD
        }
        try {
            context.applicationContext.startService(intent)
        } catch (_: Exception) {
            // Servis zaten durmuşsa sadece durumu sıfırla
        }
        AppUpdateNotificationHelper.cancelDownloadProgressNotification(context)
        _state.value = BackgroundUpdateState.Idle
    }

    fun restoreCachedUpdateIfReady(context: Context, availableInfo: UpdateInfo?) {
        if (availableInfo == null || !availableInfo.isUpdateAvailable) {
            if (_state.value is BackgroundUpdateState.ReadyToInstall) {
                val ready = _state.value as BackgroundUpdateState.ReadyToInstall
                if (ready.info.latestVersionCode <= BuildConfig.VERSION_CODE || !ready.apkFile.exists()) {
                    _state.value = BackgroundUpdateState.Idle
                }
            }
            return
        }
        val current = _state.value
        if (current is BackgroundUpdateState.Downloading) return

        val cachedApk = AppUpdateManager.getDownloadedApkForVersion(context, availableInfo.latestVersionCode)
        if (cachedApk != null) {
            _state.value = BackgroundUpdateState.ReadyToInstall(availableInfo, cachedApk)
        } else if (current is BackgroundUpdateState.ReadyToInstall &&
            (current.info.latestVersionCode <= BuildConfig.VERSION_CODE || !current.apkFile.exists())
        ) {
            _state.value = BackgroundUpdateState.Idle
        }
    }

    internal fun updateProgress(
        info: UpdateInfo,
        progress: Float,
        downloadedBytes: Long,
        totalBytes: Long
    ) {
        _state.value = BackgroundUpdateState.Downloading(
            info = info,
            progress = progress.coerceIn(0f, 1f),
            downloadedBytes = downloadedBytes,
            totalBytes = totalBytes
        )
    }

    internal fun markReady(info: UpdateInfo, apkFile: File) {
        _state.value = BackgroundUpdateState.ReadyToInstall(info, apkFile)
    }

    internal fun markFailed(info: UpdateInfo, message: String) {
        _state.value = BackgroundUpdateState.Failed(info, message)
    }

    internal fun markIdle() {
        _state.value = BackgroundUpdateState.Idle
    }

    fun getActiveKeepFilePath(): String? {
        return when (val s = _state.value) {
            is BackgroundUpdateState.ReadyToInstall -> s.apkFile.absolutePath
            else -> null
        }
    }

    fun isActivelyDownloading(): Boolean = _state.value is BackgroundUpdateState.Downloading

    fun formatBytesMb(bytes: Long): String {
        if (bytes <= 0L) return "0.0 MB"
        val mb = bytes.toDouble() / (1024.0 * 1024.0)
        return String.format(Locale("tr"), "%.1f MB", mb)
    }
}
