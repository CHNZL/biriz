package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.BirizBorder
import com.example.ui.theme.BirizNavyDark
import com.example.ui.theme.BirizNavyPrimary
import com.example.ui.theme.BirizTextMuted
import com.example.ui.theme.BirizTextPrimary
import com.example.ui.theme.BirizTextSecondary
import com.example.ui.theme.BirizTurquoise
import com.example.ui.theme.BirizTurquoiseSoft

@Composable
fun KvkkDialog(
    onDismissRequest: () -> Unit,
    onAcceptAndClose: () -> Unit = onDismissRequest,
    isDarkTheme: Boolean = false,
    isReadOnly: Boolean = false
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 24.dp)
                .testTag("kvkk_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkTheme) Color(0xFF13203E) else Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isDarkTheme) Color(0xFF334E68) else Color.Transparent
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Başlık Satırı
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = if (isDarkTheme) Color(0x3528B8C2) else BirizTurquoiseSoft,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = BirizTurquoise,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "KVKK Aydınlatma Metni",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = if (isDarkTheme) Color.White else BirizNavyPrimary
                        )
                        Text(
                            text = "6698 Sayılı Kanun & Veri Güvenliği",
                            fontSize = 12.sp,
                            color = if (isDarkTheme) Color(0xFF94A3B8) else BirizTextMuted
                        )
                    }
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.testTag("kvkk_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = if (isDarkTheme) Color(0xFF94A3B8) else BirizTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Kaydırılabilir Hukuki Metin Alanı
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .background(
                            if (isDarkTheme) Color(0xFF0F1A30) else Color(0xFFF8FAFC),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Column {
                        KvkkSectionTitle("1. Veri Sorumlusu ve Güvenlik Taahhüdü", isDarkTheme)
                        KvkkBodyText(
                            "BİRİZ Dijital Sınıf ve Veli Asistanı platformu olarak, ilkokul çağındaki öğrencilerin gelişimini desteklemek amacıyla öğretmen ve veli iletişimini en üst seviyede veri gizliliği standartlarıyla sağlıyoruz.",
                            isDarkTheme
                        )

                        KvkkSectionTitle("2. Öğrenci Hesabı ve Girişi Bulunmamaktadır", isDarkTheme)
                        KvkkBodyText(
                            "BİRİZ platformunda kesinlikle 'Öğrenci' adında bağımsız bir kullanıcı hesabı oluşturulmaz ve sisteme öğrenciler tarafından giriş yapılmaz. Öğrencilere ait tüm eğitim ve gelişim verileri, sınıf öğretmeni ve doğrulanmış veli tarafından ortaklaşa takip edilir.",
                            isDarkTheme
                        )

                        KvkkSectionTitle("3. İşlenen Kişisel Veriler", isDarkTheme)
                        KvkkBodyText(
                            "• Öğretmen Bilgileri: Ad, soyad, e-posta, telefon, görev yapılan il, ilçe, okul ve atandığı şube bilgisi.\n" +
                            "• Sınıf & Kurum Bilgileri: MEB standartlarına uyumlu sınıf düzeyi ve şube bilgisi.\n" +
                            "• Veli Bilgileri: Öğretmen onaylı sınıf katılım kodu ile eşleştirilecek veli iletişim bilgileri.",
                            isDarkTheme
                        )

                        KvkkSectionTitle("4. Tek Sınıf Öğretmeni Güvencesi", isDarkTheme)
                        KvkkBodyText(
                            "Sınıf içi verilerin tutarlılığı ve güvenliği için her şube (Örn: Sivas Merkez Atatürk İlkokulu 4/A) yalnızca bir tek yetkili sınıf öğretmenine tahsis edilir. Başka bir öğretmenin aynı şubeye yetkisiz erişimi engellenir.",
                            isDarkTheme
                        )

                        KvkkSectionTitle("5. Haklarınız (KVKK Madde 11)", isDarkTheme)
                        KvkkBodyText(
                            "Kullanıcılar diledikleri zaman kişisel verilerinin işlenip işlenmediğini öğrenme, yanlış verilerin düzeltilmesini talep etme ve hesaplarının silinmesini isteme hakkına sahiptir.",
                            isDarkTheme
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Butonlar
                if (isReadOnly) {
                    Button(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("kvkk_close_readonly_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                            contentColor = if (isDarkTheme) BirizNavyDark else Color.White
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            "Anladım ve Kapat",
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) BirizNavyDark else Color.White
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = onDismissRequest,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isDarkTheme) Color(0xFF334E68) else Color(0xFFCBD5E1)
                            )
                        ) {
                            Text(
                                "Vazgeç",
                                color = if (isDarkTheme) Color.White else BirizNavyPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = onAcceptAndClose,
                            modifier = Modifier
                                .weight(1.4f)
                                .testTag("kvkk_accept_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                                contentColor = if (isDarkTheme) BirizNavyDark else Color.White
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = if (isDarkTheme) BirizNavyDark else BirizTurquoise,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Okudum & Onayla",
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) BirizNavyDark else Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KvkkSectionTitle(title: String, isDark: Boolean = false) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = if (isDark) BirizTurquoise else BirizNavyPrimary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun KvkkBodyText(text: String, isDark: Boolean = false) {
    Text(
        text = text,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = if (isDark) Color(0xFFE2E8F0) else BirizTextSecondary,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}
