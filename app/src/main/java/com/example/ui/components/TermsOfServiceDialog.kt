package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.ui.theme.BirizNavyDark
import com.example.ui.theme.BirizNavyPrimary
import com.example.ui.theme.BirizTextMuted
import com.example.ui.theme.BirizTextSecondary
import com.example.ui.theme.BirizTurquoise
import com.example.ui.theme.BirizTurquoiseSoft

/**
 * BİRİZ Kullanım Şartları ve Hizmet Sözleşmesi Penceresi
 */
@Composable
fun TermsOfServiceDialog(
    onDismissRequest: () -> Unit,
    isDarkTheme: Boolean = false
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 24.dp)
                .testTag("terms_of_service_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkTheme) Color(0xFF13203E) else Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            border = BorderStroke(
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
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = BirizTurquoise,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Kullanım Şartları",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = if (isDarkTheme) Color.White else BirizNavyPrimary
                        )
                        Text(
                            text = "BİRİZ Hizmet ve Kullanım Sözleşmesi",
                            fontSize = 12.sp,
                            color = if (isDarkTheme) Color(0xFF94A3B8) else BirizTextMuted
                        )
                    }
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.testTag("terms_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = if (isDarkTheme) Color(0xFF94A3B8) else BirizTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Kaydırılabilir Sözleşme Metni
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
                        TermsSectionTitle("1. Taraflar ve Hizmetin Tanımı", isDarkTheme)
                        TermsBodyText(
                            "Bu sözleşme, BİRİZ İlkokul Dijital Sınıf ve Veli Yönetim Sistemi uygulamasını kullanan öğretmenler ve veliler ile platform sağlayıcısı arasında akdedilmiştir. BİRİZ, ilkokul şubelerinin sınıf düzeni, oturma planı, veli iletişimi ve eğitim asistanlığı süreçlerini dijitalleştiren güvenli bir eğitim platformudur.",
                            isDarkTheme
                        )

                        TermsSectionTitle("2. Hesap Güvenliği ve Google Kimlik Doğrulaması", isDarkTheme)
                        TermsBodyText(
                            "Sisteme girişler resmi Google Kimlik Doğrulama altyapısı (OAuth / Credential Manager) ile şifresiz ve güvenli olarak gerçekleştirilir. Kullanıcılar hesaplarının güvenliğini sağlamakla yükümlüdür.",
                            isDarkTheme
                        )

                        TermsSectionTitle("3. Öğretmen ve Veli Sorumlulukları", isDarkTheme)
                        TermsBodyText(
                            "• Öğretmenler: Sınıf listesini, öğrenci bilgilerini ve okul şube kayıtlarını yalnızca eğitim amacıyla ve KVKK prensiplerine uygun olarak işleyeceklerini taahhüt ederler.\n" +
                            "• Veliler: Kendilerine özel üretilen katılım kuponu veya karekod ile yalnızca kendi çocuklarının şubesine ve gelişim verilerine erişebilir; sınıf içi bilgileri üçüncü kişilerle paylaşamazlar.",
                            isDarkTheme
                        )

                        TermsSectionTitle("4. Öğrenci Verilerinin Korunması", isDarkTheme)
                        TermsBodyText(
                            "Platform üzerinde öğrencilere yönelik bağımsız bir hesap oluşturulmaz. Öğrenci verileri hiçbir suretle reklam, pazarlama veya ticari amaçlarla üçüncü taraflarla paylaşılmaz. Tüm veriler şifreli bulut altyapısında korunur.",
                            isDarkTheme
                        )

                        TermsSectionTitle("5. Fikri Mülkiyet ve Hizmet Değişiklikleri", isDarkTheme)
                        TermsBodyText(
                            "BİRİZ uygulamasının yazılım mimarisi, tasarımı, algoritmaları ve görsel bileşenleri telif haklarına tabidir. Geliştirici ekip, uygulamanın işlevselliğini artırmak amacıyla sürüm güncellemeleri ve iyileştirmeler yapma hakkını saklı tutar.",
                            isDarkTheme
                        )

                        TermsSectionTitle("6. Yürürlük ve İletişim", isDarkTheme)
                        TermsBodyText(
                            "Uygulamaya Google hesabı ile giriş yapılmasıyla birlikte bu Kullanım Şartları yürürlüğe girmiş kabul edilir. Şartlara ilişkin soru ve geri bildirimleriniz için BİRİZ destek kanalları üzerinden irtibat kurabilirsiniz.",
                            isDarkTheme
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("terms_accept_button"),
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
            }
        }
    }
}

@Composable
private fun TermsSectionTitle(title: String, isDark: Boolean = false) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        color = if (isDark) BirizTurquoise else BirizNavyPrimary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun TermsBodyText(text: String, isDark: Boolean = false) {
    Text(
        text = text,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = if (isDark) Color(0xFFE2E8F0) else BirizTextSecondary,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}
