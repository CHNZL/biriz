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
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
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
 * BİRİZ Güncellenmiş 6698 Sayılı KVKK Aydınlatma ve Açık Rıza Metni Penceresi
 */
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
                .padding(vertical = 20.dp)
                .testTag("kvkk_dialog"),
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
                            text = "KVKK Aydınlatma ve Açık Rıza Metni",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.5.sp,
                            color = if (isDarkTheme) Color.White else BirizNavyPrimary
                        )
                        Text(
                            text = "6698 Sayılı Kanun • Öğretmen & Veli Veri Güvencesi",
                            fontSize = 11.5.sp,
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

                Spacer(modifier = Modifier.height(10.dp))

                // Bilgi Rozeti
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
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Hem Öğretmen hem de Veli rolüyle devam edebilmek için bu aydınlatma metninin onaylanması zorunludur.",
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDarkTheme) Color(0xFFE2E8F0) else BirizNavyPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Kaydırılabilir Hukuki Metin Alanı
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
                        KvkkSectionTitle("1. Veri Sorumlusu, Amaç ve Kapsam", isDarkTheme)
                        KvkkBodyText(
                            "BİRİZ İlkokul Dijital Sınıf ve Veli Asistanı platformu olarak; 6698 sayılı Kişisel Verilerin Korunması Kanunu ('KVKK') ve Milli Eğitim Bakanlığı veri mahremiyeti ilkeleri doğrultusunda, sınıf öğretmenleri ile öğrenci velileri arasındaki eğitsel iletişimi ve sınıf yönetimi süreçlerini güvenli, şeffaf ve reklamsız bir ortamda yürütmekteyiz.",
                            isDarkTheme
                        )

                        KvkkSectionTitle("2. Bağımsız Öğrenci Hesabı Bulunmaması (Çocuk Güvenliği)", isDarkTheme)
                        KvkkBodyText(
                            "BİRİZ platformunda ilkokul çağındaki çocukların dijital güvenliğini korumak amacıyla kesinlikle 'Öğrenci' adında bağımsız bir kullanıcı hesabı açılmaz ve öğrenciler doğrudan sisteme giriş yapmaz. Öğrencilere ait eğitsel gelişim verileri yalnızca yetkili sınıf öğretmeni ve tekil katılım koduyla doğrulanmış kendi velisi tarafından görüntülenebilir.",
                            isDarkTheme
                        )

                        KvkkSectionTitle("3. Rol Bazlı İşlenen Kişisel Veriler", isDarkTheme)
                        KvkkBodyText(
                            "• Öğretmen Rolü Kapsamında: Ad-soyad, Google kimlik doğrulama e-postası, görev yapılan il/ilçe/okul ve şube bilgisi, haftalık ders programı, akıllı oturma planı, çalışma grupları, sınıf kitaplığı, eğitsel turnuvalar, sınıf duyuruları/anketleri ve anlık bildirim tercihleri.\n" +
                            "• Veli Rolü Kapsamında: Ad-soyad, Google kimlik doğrulama e-postası, yakınlık derecesi, isteğe bağlı iletişim numarası, öğretmenin ürettiği 8 haneli tekil Veli Katılım Kodu ile yapılan öğrenci eşleşmesi, öğretmen-veli birebir mesajlaşmaları, duyuru okundu/anket yanıtları ve bildirim tercihleri.\n" +
                            "• Öğrenci Eğitsel Takip Verileri: Okul numarası, ad-soyad, cinsiyet, sıra ve çalışma grubu yerleşimi, kazanılan davranış/gelişim yıldızları, okunan kitap kayıtları ve sınıf içi turnuva eşleşmeleri.",
                            isDarkTheme
                        )

                        KvkkSectionTitle("4. Tek Şube – Tek Öğretmen ve Veli İzolasyonu Güvencesi", isDarkTheme)
                        KvkkBodyText(
                            "Her ilkokul şubesi (Örn: Sivas Merkez Süleyman Sami Kepenek İlkokulu 4/D) aynı anda yalnızca tek bir yetkili sınıf öğretmenine tahsis edilir. Veliler ise yalnızca kendilerine özel üretilen 8 haneli Veli Katılım Kodu veya karekod ile kendi çocuklarının eğitim bilgilerine erişebilir; diğer öğrencilerin özel mesajlarını veya kişisel verilerini göremezler.",
                            isDarkTheme
                        )

                        KvkkSectionTitle("5. Akıllı Tahta ve Bulut Senkronizasyon Güvenliği", isDarkTheme)
                        KvkkBodyText(
                            "Sınıf içi Akıllı Tahta ('cihanogretmen.com/tahta') bağlantılarında yalnızca öğretmenin başlattığı tek kullanımlık oturum kodu üzerinden eğitsel modüller (oturma planı, gruplar, yıldızlar, kitaplık, turnuva, zamanlayıcı ve eğitim ekranı) yansıtılır. Veli özel mesajlaşmaları veya kişisel iletişim bilgileri hiçbir koşulda Akıllı Tahta ekranına aktarılmaz.",
                            isDarkTheme
                        )

                        KvkkSectionTitle("6. Veri Saklama, 'Unutulma Hakkı' ve Kalıcı Hesap Silme", isDarkTheme)
                        KvkkBodyText(
                            "Kişisel verileriniz hiçbir zaman reklam, profilleme veya ticari pazarlama amacıyla üçüncü taraflarla paylaşılmaz. Öğretmen ve veli kullanıcılarımız uygulama içerisinden diledikleri anda 'Rolümü Sil' veya 'Hesabımı Kalıcı Olarak Sil' özelliğini kullanarak hem kimlik doğrulama (Authentication) hesaplarını hem de bulut veritabanındaki ('users', 'classrooms' ve tüm alt koleksiyonlar) verilerini geri döndürülemez biçimde anında silebilirler.",
                            isDarkTheme
                        )

                        KvkkSectionTitle("7. KVKK Madde 11 Kapsamındaki Haklarınız", isDarkTheme)
                        KvkkBodyText(
                            "Kullanıcılarımız KVKK'nın 11. maddesi uyarınca; kişisel verilerinin işlenip işlenmediğini öğrenme, işlenme amacına uygun kullanılıp kullanılmadığını denetleme, eksik veya yanlış işlenmiş verilerin düzeltilmesini isteme ve verilerin kalıcı olarak silinmesini/yok edilmesini talep etme haklarına sahiptir.",
                            isDarkTheme
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

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
        fontSize = 13.5.sp,
        color = if (isDark) BirizTurquoise else BirizNavyPrimary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun KvkkBodyText(text: String, isDark: Boolean = false) {
    Text(
        text = text,
        fontSize = 12.5.sp,
        lineHeight = 18.sp,
        color = if (isDark) Color(0xFFE2E8F0) else BirizTextSecondary,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}
