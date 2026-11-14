package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.ui.theme.BirizDarkCardBorder
import com.example.ui.theme.BirizDarkTextMuted
import com.example.ui.theme.BirizDarkTextPrimary
import com.example.ui.theme.BirizError
import com.example.ui.theme.BirizNavyPrimary
import com.example.ui.theme.BirizSuccess
import com.example.ui.theme.BirizTextMuted
import com.example.ui.theme.BirizTurquoise
import com.example.util.AppUpdateDownloadController
import com.example.util.AppUpdateManager
import com.example.util.AppUpdateNotificationHelper
import com.example.util.BackgroundUpdateState
import com.example.util.UpdateInfo
import kotlinx.coroutines.launch
import java.io.File

sealed interface UpdateDialogState {
    object Idle : UpdateDialogState
    object Checking : UpdateDialogState
    data class Available(val info: UpdateInfo) : UpdateDialogState
    data class Downloading(
        val info: UpdateInfo,
        val progress: Float,
        val downloadedBytes: Long = 0L,
        val totalBytes: Long = -1L
    ) : UpdateDialogState
    data class ReadyToInstall(val info: UpdateInfo, val apkFile: File) : UpdateDialogState
    data class UpToDate(val info: UpdateInfo) : UpdateDialogState
    data class Error(val message: String, val retryInfo: UpdateInfo? = null) : UpdateDialogState
}

@Composable
fun AppUpdateDialog(
    isDarkTheme: Boolean,
    initialInfo: UpdateInfo? = null,
    autoStartDownload: Boolean = false,
    onUpdateDiscovered: (UpdateInfo) -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val bgState by AppUpdateDownloadController.state.collectAsStateWithLifecycle()

    var localState by remember {
        val currentBg = AppUpdateDownloadController.state.value
        mutableStateOf<UpdateDialogState>(
            when (currentBg) {
                is BackgroundUpdateState.Downloading -> UpdateDialogState.Downloading(
                    currentBg.info,
                    currentBg.progress,
                    currentBg.downloadedBytes,
                    currentBg.totalBytes
                )
                is BackgroundUpdateState.ReadyToInstall -> UpdateDialogState.ReadyToInstall(
                    currentBg.info,
                    currentBg.apkFile
                )
                is BackgroundUpdateState.Failed -> UpdateDialogState.Error(
                    currentBg.message,
                    currentBg.info
                )
                is BackgroundUpdateState.Idle -> if (autoStartDownload && initialInfo != null && initialInfo.isUpdateAvailable) {
                    UpdateDialogState.Available(initialInfo)
                } else {
                    UpdateDialogState.Checking
                }
            }
        )
    }

    fun startBackgroundDownload(info: UpdateInfo) {
        if (!AppUpdateManager.canInstallPackages(context)) {
            Toast.makeText(
                context,
                "Lütfen BİRİZ uygulamasına güncelleme yükleme izni verin.",
                Toast.LENGTH_LONG
            ).show()
            AppUpdateManager.requestInstallPermission(context)
            return
        }
        AppUpdateDownloadController.startDownload(context, info)
    }

    fun runCheck() {
        val currentBg = AppUpdateDownloadController.state.value
        if (currentBg is BackgroundUpdateState.Downloading || currentBg is BackgroundUpdateState.ReadyToInstall) {
            return
        }
        AppUpdateDownloadController.markIdle()
        localState = UpdateDialogState.Checking
        coroutineScope.launch {
            val repoToUse = AppUpdateManager.getGitHubRepo(context)
            val result = AppUpdateManager.checkForUpdates(context, repoToUse)
            if (result == null) {
                localState = UpdateDialogState.Error(
                    "GitHub üzerinden güncellemeler denetlenirken bir sorun oluştu. İnternet bağlantınızı kontrol edip tekrar deneyin."
                )
            } else if (result.isUpdateAvailable) {
                onUpdateDiscovered(result)
                AppUpdateDownloadController.restoreCachedUpdateIfReady(context, result)
                val updatedBg = AppUpdateDownloadController.state.value
                if (updatedBg is BackgroundUpdateState.ReadyToInstall) {
                    localState = UpdateDialogState.ReadyToInstall(updatedBg.info, updatedBg.apkFile)
                    if (autoStartDownload) {
                        AppUpdateNotificationHelper.cancelUpdateNotification(context)
                        AppUpdateManager.installApk(context, updatedBg.apkFile)
                        onDismiss()
                    }
                } else {
                    localState = UpdateDialogState.Available(result)
                    if (autoStartDownload) {
                        startBackgroundDownload(result)
                    } else {
                        AppUpdateNotificationHelper.showUpdateNotification(context, result, forceResend = true)
                    }
                }
            } else {
                localState = UpdateDialogState.UpToDate(result)
            }
        }
    }

    LaunchedEffect(Unit) {
        when (val currentBg = AppUpdateDownloadController.state.value) {
            is BackgroundUpdateState.Downloading -> {
                localState = UpdateDialogState.Downloading(
                    currentBg.info,
                    currentBg.progress,
                    currentBg.downloadedBytes,
                    currentBg.totalBytes
                )
            }
            is BackgroundUpdateState.ReadyToInstall -> {
                localState = UpdateDialogState.ReadyToInstall(currentBg.info, currentBg.apkFile)
            }
            else -> {
                if (autoStartDownload && initialInfo != null && initialInfo.isUpdateAvailable) {
                    startBackgroundDownload(initialInfo)
                } else {
                    runCheck()
                }
            }
        }
    }

    // Arka plan indirme servisinden gelen ilerleme/tamamlanma/hata akışını canlı yansıt
    val effectiveState: UpdateDialogState = when (val currentBg = bgState) {
        is BackgroundUpdateState.Downloading -> UpdateDialogState.Downloading(
            info = currentBg.info,
            progress = currentBg.progress,
            downloadedBytes = currentBg.downloadedBytes,
            totalBytes = currentBg.totalBytes
        )
        is BackgroundUpdateState.ReadyToInstall -> UpdateDialogState.ReadyToInstall(
            info = currentBg.info,
            apkFile = currentBg.apkFile
        )
        is BackgroundUpdateState.Failed -> UpdateDialogState.Error(
            message = currentBg.message,
            retryInfo = currentBg.info
        )
        is BackgroundUpdateState.Idle -> localState
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isDarkTheme) Color(0xFF0D1B2A) else Color.White,
            border = BorderStroke(
                1.dp,
                if (isDarkTheme) BirizDarkCardBorder else Color(0xFFE2E8F0)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Başlık Satırı
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = if (isDarkTheme) Color(0x3028B8C2) else Color(0xFFE0F2FE),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = null,
                                    tint = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "BİRİZ Güncelleme",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                            )
                            Text(
                                text = "Mevcut: v${BuildConfig.VERSION_NAME} (#${BuildConfig.VERSION_CODE})",
                                fontSize = 11.sp,
                                color = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Durum İçeriği
                when (val current = effectiveState) {
                    is UpdateDialogState.Checking -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = BirizTurquoise,
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "GitHub üzerinden yeni sürüm denetleniyor...",
                                fontSize = 12.5.sp,
                                color = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted
                            )
                        }
                    }

                    is UpdateDialogState.Available -> {
                        val info = current.info
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDarkTheme) Color(0x3010B981) else Color(0xFFECFDF5),
                                border = BorderStroke(
                                    1.dp,
                                    if (isDarkTheme) Color(0xFF10B981) else Color(0xFFA7F3D0)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "🎉", fontSize = 26.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Yeni Sürüm Hazır: v${info.latestVersionName}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDarkTheme) Color(0xFF6EE7B7) else Color(0xFF047857)
                                        )
                                        Text(
                                            text = "Yapı #${info.latestVersionCode} • Arka planda kesintisiz indirilir",
                                            fontSize = 11.5.sp,
                                            color = if (isDarkTheme) Color(0xFF6EE7B7).copy(alpha = 0.8f) else Color(0xFF059669)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Daha Sonra", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = { startBackgroundDownload(info) },
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .testTag("start_app_update_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BirizTurquoise)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Hemen Güncelle", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    is UpdateDialogState.Downloading -> {
                        val progress = current.progress
                        val pct = (progress * 100).toInt().coerceIn(0, 100)
                        val sizeInfo = when {
                            current.totalBytes > 0L -> "${AppUpdateDownloadController.formatBytesMb(current.downloadedBytes)} / ${AppUpdateDownloadController.formatBytesMb(current.totalBytes)} (%$pct)"
                            current.downloadedBytes > 0L -> "${AppUpdateDownloadController.formatBytesMb(current.downloadedBytes)} indirildi..."
                            else -> "Bağlantı kuruluyor..."
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "BİRİZ v${current.info.latestVersionName} İndiriliyor",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = sizeInfo,
                                fontSize = 12.sp,
                                color = BirizTurquoise,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            if (current.totalBytes > 0L) {
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp),
                                    color = BirizTurquoise,
                                    trackColor = if (isDarkTheme) Color(0x30FFFFFF) else Color(0xFFE2E8F0)
                                )
                            } else {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp),
                                    color = BirizTurquoise,
                                    trackColor = if (isDarkTheme) Color(0x30FFFFFF) else Color(0xFFE2E8F0)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDarkTheme) Color(0x250EA5E9) else Color(0xFFF0F9FF),
                                border = BorderStroke(
                                    0.8.dp,
                                    if (isDarkTheme) BirizTurquoise.copy(alpha = 0.5f) else Color(0xFFBAE6FD)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Pencereyi kapatsanız veya ekranı kilitleseniz bile indirme arka planda kesintisiz devam eder. Bittiğinde bildirimle haber verilecektir.",
                                    fontSize = 11.sp,
                                    color = if (isDarkTheme) BirizDarkTextMuted else Color(0xFF0369A1),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 15.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(
                                    onClick = {
                                        AppUpdateDownloadController.cancelDownload(context)
                                        localState = UpdateDialogState.Available(current.info)
                                    },
                                    modifier = Modifier.weight(0.9f)
                                ) {
                                    Text("İptal Et", fontSize = 12.sp, color = BirizError, fontWeight = FontWeight.SemiBold)
                                }
                                Button(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .weight(1.4f)
                                        .testTag("continue_update_in_background_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BirizNavyPrimary)
                                ) {
                                    Text("Arka Planda Devam Et", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    is UpdateDialogState.ReadyToInstall -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = BirizSuccess,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "İndirme Başarıyla Tamamlandı!",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "BİRİZ v${current.info.latestVersionName} kuruluma hazır.",
                                fontSize = 12.sp,
                                color = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    AppUpdateNotificationHelper.cancelUpdateNotification(context)
                                    AppUpdateManager.installApk(context, current.apkFile)
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("install_app_update_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BirizSuccess)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdate,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Şimdi Yükle / Kur", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    is UpdateDialogState.UpToDate -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = BirizTurquoise,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Uygulamanız Güncel!",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Kurulu Sürüm: v${BuildConfig.VERSION_NAME} • GitHub Son Sürüm: v${current.info.latestVersionName}",
                                fontSize = 12.sp,
                                color = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { runCheck() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Tekrar Denetle", fontSize = 11.5.sp)
                                }
                                Button(
                                    onClick = onDismiss,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BirizNavyPrimary)
                                ) {
                                    Text("Tamam", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    is UpdateDialogState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = "İndirme / Kontrol Başarısız",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = BirizError
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = current.message,
                                fontSize = 11.5.sp,
                                color = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    val infoToRetry = current.retryInfo
                                    if (infoToRetry != null) {
                                        startBackgroundDownload(infoToRetry)
                                    } else {
                                        runCheck()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BirizTurquoise)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tekrar Dene", fontSize = 11.5.sp)
                            }
                        }
                    }

                    else -> {}
                }
            }
        }
    }
}
