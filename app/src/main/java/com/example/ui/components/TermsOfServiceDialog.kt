package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Gavel
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
import com.example.ui.theme.BirizNavyDark
import com.example.ui.theme.BirizNavyPrimary
import com.example.ui.theme.BirizTextMuted
import com.example.ui.theme.BirizTextSecondary
import com.example.ui.theme.BirizTurquoise
import com.example.ui.theme.BirizTurquoiseSoft

/**
 * BİRİZ Güncellenmiş Kullanım Şartları ve Hizmet Sözleşmesi Penceresi
 */
@Composable
fun TermsOfServiceDialog(
    onDismissRequest: () -> Unit,
    onAcceptAndClose: (() -> Unit)? = null,
    isDarkTheme: Boolean = false,
    isReadOnly: Boolean = onAcceptAndClose == null
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 20.dp)
                .testTag("terms_of_service_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkTheme) Color(0xFF13203E) else Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            border = BorderStroke(
                1.dp,
                if (isDarkTheme) Color(0xFF334E68) else Color(0xFFE2E8F0)
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
                        modifier = Modifier.size(44.dp)
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
                            text = "BİRİZ Kullanım Şartları",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.5.sp,
                            color = if (isDarkTheme) Color.White else BirizNavyPrimary
                        )
                        Text(
                            text = "Öğretmen ve Veli Hizmet Kullanım Sözleşmesi",
                            fontSize = 11.5.sp,
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

                Spacer(modifier = Modifier.height(10.dp))

                // Zorunlu Onay Bilgi Bandı
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDarkTheme) Color(0x2828B8C2) else Color(0xFFEFF6FF),
                    border = BorderStroke(
                        1.dp,
                        if (isDarkTheme) BirizTurquoise.copy(alpha = 0.35f) else Color(0xFFBFDBFE)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gavel,
                            contentDescription = null,
                            tint = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Platformda Öğretmen veya Veli rolünü seçebilmek için Kullanım Şartları ve KVKK metninin kabul edilmesi zorunludur.",
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDarkTheme) Color(0xFFE2E8F0) else BirizNavyPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Kaydırılabilir Sözleşme Metni
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 370.dp)
                        .background(
                            if (isDarkTheme) Color(0xFF0F1A30) else Color(0xFFF8FAFC),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(14.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Column {
                        TermsSectionTitle("1. Taraflar, Hizmetin Tanımı ve Zorunlu Onay", isDarkTheme)
                        TermsBodyText(
                            "İşbu Kullanım Şartları, BİRİZ İlkokul Dijital Sınıf ve Veli Yönetim Sistemi uygulamasını kullanan sınıf öğretmenleri ve öğrenci velileri ile BİRİZ platformu arasında akdedilmiştir. Uygulama içerisinde 'Öğretmen' veya 'Veli' rolünü seçerek devam edebilmek için hem KVKK Aydınlatma Metninin hem de işbu Kullanım Şartlarının okunup onaylanması zorunludur.",
                            isDarkTheme
                        )

                        TermsSectionTitle("2. Google Kimlik Doğrulaması ve Hesap Güvenliği", isDarkTheme)
                        TermsBodyText(
                            "Sisteme girişler resmi Google Kimlik Doğrulama altyapısı (Google OAuth / Credential Manager) üzerinden şifresiz ve güvenli olarak gerçekleştirilir. Her kullanıcı kendi Google hesabının güvenliğinden ve doğru rol seçimi yapmaktan sorumludur.",
                            isDarkTheme
                        )

                        TermsSectionTitle("3. Öğretmen Rolü Kullanım Kuralları ve Yetkileri", isDarkTheme)
                        TermsBodyText(
                            "• Sınıf öğretmenleri yalnızca görev yaptıkları il, ilçe, okul ve kendi şubeleri adına sınıf kaydı oluşturabilirler; her şube tek bir yetkili öğretmene tahsis edilir.\n" +
                            "• Öğretmenler sınıf listesi, oturma planı, çalışma grupları, gelişim yıldızları, sınıf kitaplığı, eğitsel turnuvalar, ders programı ve duyuruları yalnızca eğitim-öğretim amacıyla yönetmeyi taahhüt eder.\n" +
                            "• Akıllı Tahta ('cihanogretmen.com/tahta') bağlantısı ve canlı ekran paylaşımı özelliği yalnızca sınıf içi eğitim etkinlikleri için kullanılmalıdır.",
                            isDarkTheme
                        )

                        TermsSectionTitle("4. Veli Rolü Kullanım Kuralları ve Yetkileri", isDarkTheme)
                        TermsBodyText(
                            "• Veliler yalnızca sınıf öğretmeni tarafından kendilerine teslim edilen 8 haneli tekil Veli Katılım Kodu veya karekod davetiyesi ile kendi çocuklarının kaydına bağlanabilirler.\n" +
                            "• Veli iletişim modülü üzerinden yapılan mesajlaşmalarda ve duyuru/anket etkileşimlerinde eğitim ortamına uygun iletişim dili ve nezaket kuralları esastır; öğretmenin belirlediği sessiz saat tercihlerine saygı gösterilir.\n" +
                            "• Sınıf içi paylaşılan duyuru, program ve etkinlik bilgileri üçüncü kişilerle veya sosyal medya ortamlarında izinsiz paylaşılamaz.",
                            isDarkTheme
                        )

                        TermsSectionTitle("5. Çocuk Güvenliği, Reklamsız Ortam ve Veri Silme Hakkı", isDarkTheme)
                        TermsBodyText(
                            "Platformda öğrenciler için ayrı bir hesap açılmaz ve hiçbir kullanıcı verisi reklam, analiz satışı veya ticari amaçlarla üçüncü taraflara aktarılmaz. Kullanıcılar diledikleri zaman 'Rolümü Sil' ile mevcut rollerini sıfırlayabilir veya 'Hesabımı Kalıcı Olarak Sil' seçeneği ile hem Authentication hem de Firestore bulut kayıtlarını tamamen silebilirler.",
                            isDarkTheme
                        )

                        TermsSectionTitle("6. Fikri Mülkiyet, Güncellemeler ve Yürürlük", isDarkTheme)
                        TermsBodyText(
                            "BİRİZ uygulamasının yazılım mimarisi, tasarımı, algoritmaları ve görsel bileşenleri telif haklarına tabidir. Platform, güvenliği ve eğitim kalitesini artırmak adına uygulama içi güncellemeler yayınlama hakkını saklı tutar. İşbu sözleşme kullanıcı tarafından elektronik ortamda onaylandığı anda yürürlüğe girer.",
                            isDarkTheme
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (isReadOnly || onAcceptAndClose == null) {
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
                } else {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = onDismissRequest,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
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
                                .weight(1.5f)
                                .testTag("terms_accept_button"),
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
private fun TermsSectionTitle(title: String, isDark: Boolean = false) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 13.5.sp,
        color = if (isDark) BirizTurquoise else BirizNavyPrimary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun TermsBodyText(text: String, isDark: Boolean = false) {
    Text(
        text = text,
        fontSize = 12.5.sp,
        lineHeight = 18.sp,
        color = if (isDark) Color(0xFFE2E8F0) else BirizTextSecondary,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}
