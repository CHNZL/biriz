package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.BirizAmber
import com.example.ui.theme.BirizDarkCardBorder
import com.example.ui.theme.BirizDarkSurface
import com.example.ui.theme.BirizDarkTextPrimary
import com.example.ui.theme.BirizDarkTextSecondary
import com.example.ui.theme.BirizNavyPrimary
import com.example.ui.theme.BirizTextPrimary
import com.example.ui.theme.BirizTextSecondary
import com.example.ui.theme.BirizTurquoise
import com.example.util.QrCodeHelper

/**
 * Velilerin BİRİZ uygulamasını (Biriz.apk) doğrudan telefonlarına indirebilmeleri için
 * ekranda tam boyutlu Karekod (QR Code) ve indirme bağlantısı gösteren modern M3 Modal Diyaloğu.
 */
@Composable
fun ParentDownloadQrDialog(
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onPrintParentCards: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val downloadUrl = QrCodeHelper.LATEST_APK_DOWNLOAD_URL

    // Yüksek çözünürlüklü QR Kod Bitmap'i
    val qrBitmap = remember(downloadUrl) {
        QrCodeHelper.generateQrBitmap(
            content = downloadUrl,
            size = 400,
            foregroundColor = android.graphics.Color.parseColor("#13294B")
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (isDarkTheme) BirizDarkSurface else Color.White,
            border = BorderStroke(
                1.dp,
                if (isDarkTheme) BirizDarkCardBorder else Color(0xFFE2E8F0)
            ),
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .testTag("parent_download_qr_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Başlık Satırı
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isDarkTheme) Color(0x3038BDF8) else Color(0xFFE0F2FE),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.QrCode2,
                                    contentDescription = null,
                                    tint = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Veli Uygulama İndirme",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDarkTheme) BirizDarkTextPrimary else BirizTextPrimary
                            )
                            Text(
                                text = "BİRİZ Android (APK)",
                                fontSize = 11.sp,
                                color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
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
                            tint = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Karekod Gösterim Alanı (Beyaz Fonlu Çerçeve)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(2.dp, if (isDarkTheme) BirizTurquoise else Color(0xFFBFDBFE)),
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(210.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "BİRİZ APK İndirme Karekodu",
                                modifier = Modifier.size(186.dp)
                            )
                        } else {
                            Text("Karekod yüklenemedi", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Yönerge Metni
                Text(
                    text = "📲 Veliler telefon kameralarını bu karekoda tutarak en güncel BİRİZ uygulamasını anında doğrudan telefonlarına indirebilirler.",
                    fontSize = 12.5.sp,
                    color = if (isDarkTheme) BirizDarkTextPrimary else BirizTextPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(12.dp))

                // İndirme Bağlantısı Kutusu ve Kopyala Butonu
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDarkTheme) BirizDarkSurface else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val clip = ClipData.newPlainText("BİRİZ İndirme Bağlantısı", downloadUrl)
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(clip)
                            Toast.makeText(context, "İndirme bağlantısı panoya kopyalandı! 📋", Toast.LENGTH_SHORT).show()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Doğrudan İndirme Bağlantısı:",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                            )
                            Text(
                                text = downloadUrl,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Kopyala",
                            tint = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Eylem Butonları
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // WhatsApp / SMS ile Bağlantıyı Paylaş
                    OutlinedButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "BİRİZ Okul ve Sınıf İletişim Uygulamasını indirmek için aşağıdaki bağlantıya tıklayabilirsiniz:\n$downloadUrl"
                                )
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "BİRİZ İndirme Linkini Paylaş"))
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isDarkTheme) BirizTurquoise else BirizNavyPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Paylaş",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                        )
                    }

                    // Veli Katılım Kartları (PDF) Yazdır
                    if (onPrintParentCards != null) {
                        Button(
                            onClick = {
                                onDismiss()
                                onPrintParentCards()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Kuponları Yazdır",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
