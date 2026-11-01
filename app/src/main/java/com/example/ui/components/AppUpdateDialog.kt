package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.BuildConfig
import com.example.ui.theme.BirizAmber
import com.example.ui.theme.BirizDarkCardBorder
import com.example.ui.theme.BirizDarkSurfaceGlass
import com.example.ui.theme.BirizDarkTextMuted
import com.example.ui.theme.BirizDarkTextPrimary
import com.example.ui.theme.BirizError
import com.example.ui.theme.BirizNavyPrimary
import com.example.ui.theme.BirizSuccess
import com.example.ui.theme.BirizTextMuted
import com.example.ui.theme.BirizTurquoise
import com.example.util.AppUpdateManager
import com.example.util.AppUpdateNotificationHelper
import com.example.util.UpdateInfo
import kotlinx.coroutines.launch
import java.io.File

sealed interface UpdateDialogState {
    object Idle : UpdateDialogState
    object Checking : UpdateDialogState
    data class Available(val info: UpdateInfo) : UpdateDialogState
    data class Downloading(val info: UpdateInfo, val progress: Float) : UpdateDialogState
    data class ReadyToInstall(val info: UpdateInfo, val apkFile: File) : UpdateDialogState
    data class UpToDate(val info: UpdateInfo) : UpdateDialogState
    data class Error(val message: String) : UpdateDialogState
}

@Composable
fun AppUpdateDialog(
    isDarkTheme: Boolean,
    initialInfo: UpdateInfo? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var state by remember {
        mutableStateOf<UpdateDialogState>(
            if (initialInfo != null) {
                if (initialInfo.isUpdateAvailable) UpdateDialogState.Available(initialInfo)
                else UpdateDialogState.UpToDate(initialInfo)
            } else {
                UpdateDialogState.Checking
            }
        )
    }

    fun runCheck() {
        state = UpdateDialogState.Checking
        coroutineScope.launch {
            val repoToUse = AppUpdateManager.getGitHubRepo(context)
            val result = AppUpdateManager.checkForUpdates(context, repoToUse)
            if (result == null) {
                state = UpdateDialogState.Error(
                    "GitHub üzerinden güncellemeler denetlenirken bir sorun oluştu. İnternet bağlantınızı kontrol edip tekrar deneyin."
                )
            } else if (result.isUpdateAvailable) {
                state = UpdateDialogState.Available(result)
                AppUpdateNotificationHelper.showUpdateNotification(context, result)
            } else {
                state = UpdateDialogState.UpToDate(result)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (initialInfo == null) {
            runCheck()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isDarkTheme) Color(0xFF0D1B2A) else Color.White,
            border = androidx.compose.foundation.BorderStroke(
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
                when (val current = state) {
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
                                border = androidx.compose.foundation.BorderStroke(
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
                                            text = "Yapı #${info.latestVersionCode} • Tek tıkla hemen kurabilirsiniz",
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
                                    onClick = {
                                        state = UpdateDialogState.Downloading(info, 0f)
                                        coroutineScope.launch {
                                            val apk = AppUpdateManager.downloadApk(context, info.downloadUrl) { progress ->
                                                state = UpdateDialogState.Downloading(info, progress)
                                            }
                                            if (apk != null && apk.exists()) {
                                                state = UpdateDialogState.ReadyToInstall(info, apk)
                                            } else {
                                                state = UpdateDialogState.Error(
                                                    "APK dosyası indirilemedi. İnternet bağlantınızı kontrol edip tekrar deneyin."
                                                )
                                            }
                                        }
                                    },
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
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "BİRİZ Güncellemesi İndiriliyor...",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "%${(progress * 100).toInt()} tamamlandı",
                                fontSize = 12.sp,
                                color = BirizTurquoise,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                color = BirizTurquoise,
                                trackColor = if (isDarkTheme) Color(0x30FFFFFF) else Color(0xFFE2E8F0)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "İndirme tamamlandığında kurulum ekranı otomatik olarak açılacaktır.",
                                fontSize = 11.sp,
                                color = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted,
                                textAlign = TextAlign.Center
                            )
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
                                text = "Zaten en son BİRİZ sürümünü (v${BuildConfig.VERSION_NAME}) kullanıyorsunuz.",
                                fontSize = 12.sp,
                                color = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onDismiss,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BirizNavyPrimary)
                            ) {
                                Text("Harika, Tamam", fontSize = 12.sp)
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
                                text = "Kontrol Başarısız",
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
                                onClick = { runCheck() },
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
