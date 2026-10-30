package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.BentoCard
import com.example.ui.theme.BirizAmber
import com.example.ui.theme.BirizBorder
import com.example.ui.theme.BirizDarkCardBorder
import com.example.ui.theme.BirizDarkTextMuted
import com.example.ui.theme.BirizDarkTextPrimary
import com.example.ui.theme.BirizDarkTextSecondary
import com.example.ui.theme.BirizNavyDark
import com.example.ui.theme.BirizNavyLight
import com.example.ui.theme.BirizNavyPrimary
import com.example.ui.theme.BirizSuccess
import com.example.ui.theme.BirizTextPrimary
import com.example.ui.theme.BirizTextSecondary
import com.example.ui.theme.BirizTurquoise
import com.example.ui.viewmodel.DashboardSubPage

@Composable
fun ModulePreviewsSubScreen(
    subPage: DashboardSubPage,
    isDarkTheme: Boolean,
    onBackToClassList: () -> Unit
) {
    val pageIcon: ImageVector = when (subPage) {
        DashboardSubPage.DERS_PROGRAMI -> Icons.Default.DateRange
        DashboardSubPage.OTURMA_PLANI -> Icons.Default.TableChart
        DashboardSubPage.GRUP_OLUSTURMA -> Icons.Default.Group
        DashboardSubPage.SANSLI_OGRENCI -> Icons.Default.AutoAwesome
        DashboardSubPage.YILDIZLAR_SINIFI -> Icons.Default.Star
        DashboardSubPage.ZAMANLAYICI -> Icons.Default.HourglassBottom
        DashboardSubPage.KITAPLIK_LISTE,
        DashboardSubPage.KITAPLIK_KAYITLAR,
        DashboardSubPage.KITAPLIK_DEGERLENDIRME -> Icons.Default.MenuBook
        DashboardSubPage.TURNUVA_DEVAM_EDENLER,
        DashboardSubPage.TURNUVA_TAMAMLANANLAR -> Icons.Default.EmojiEvents
        else -> Icons.Default.School
    }

    val pageDescription = when (subPage) {
        DashboardSubPage.DERS_PROGRAMI -> "Haftalık ilkokul ders saatleri, tenefüsler ve ders programı şablonu bu ekrandan yönetilecektir."
        DashboardSubPage.OTURMA_PLANI -> "2'li ve 3'lü sıra düzenleri, pedagojik oturma planı ve sürükle-bırak öğrenci yerleşimi bu ekranda yer alacaktır."
        DashboardSubPage.GRUP_OLUSTURMA -> "Sınıf içi etkinlikler ve istasyon çalışmaları için dengeli karma gruplar otomatik oluşturulacaktır."
        DashboardSubPage.SANSLI_OGRENCI -> "Ders içi soru-cevap ve tahtaya kaldırma çarkı; öğrencileri heyecanlandıran rastgele seçim motoru."
        DashboardSubPage.YILDIZLAR_SINIFI -> "Haftanın ve ayın yıldız öğrencileri, olumlu davranış rozetleri ve veli tebrik kartları."
        DashboardSubPage.ZAMANLAYICI -> "Sınav, etkinlik ve kitap okuma saatleri için animasyonlu sınıf geri sayım sayacı."
        DashboardSubPage.KITAPLIK_LISTE -> "Sınıf kitaplığındaki tüm hikaye ve masal kitaplarının barkod ve ISBN takibi."
        DashboardSubPage.KITAPLIK_KAYITLAR -> "Hangi öğrencinin hangi kitabı okuduğu ve iade geçmişi kayıtları."
        DashboardSubPage.KITAPLIK_DEGERLENDIRME -> "Okunan kitaplara ilişkin öğrenci anlama ve özet değerlendirme notları."
        DashboardSubPage.TURNUVA_DEVAM_EDENLER -> "Sınıf içi zeka oyunları, mangala ve kelime turnuvalarının anlık fikstür ve eşleşmeleri."
        DashboardSubPage.TURNUVA_TAMAMLANANLAR -> "Tamamlanan sınıf turnuvalarının madalya ve şampiyonluk arşivleri."
        else -> "Bu modül yakında aktif olacaktır."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Üst Başlık & Kategori Rozeti
        BentoCard(
            isDark = isDarkTheme,
            glowColor = BirizTurquoise.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isDarkTheme) Color(0x3528B8C2) else BirizNavyPrimary,
                    modifier = Modifier.size(68.dp),
                    border = BorderStroke(2.dp, BirizTurquoise)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = pageIcon,
                            contentDescription = null,
                            tint = if (isDarkTheme) BirizTurquoise else Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDarkTheme) Color(0x3028B8C2) else Color(0x1513294B)
                ) {
                    Text(
                        text = subPage.category,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = subPage.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = BirizAmber.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, BirizAmber.copy(alpha = 0.5f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = BirizAmber,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "BENTO MİKRO-ÖNİZLEME • ÇOK YAKINDA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BirizAmber
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = pageDescription,
                    fontSize = 13.sp,
                    color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        }

        // Modüle Özel Zengin Önizleme Simülasyonu
        when (subPage) {
            DashboardSubPage.OTURMA_PLANI -> {
                SeatingPlanPreviewCard(isDarkTheme = isDarkTheme)
            }
            DashboardSubPage.YILDIZLAR_SINIFI -> {
                ClassStarsPreviewCard(isDarkTheme = isDarkTheme)
            }
            DashboardSubPage.SANSLI_OGRENCI -> {
                LuckyStudentPreviewCard(isDarkTheme = isDarkTheme)
            }
            DashboardSubPage.ZAMANLAYICI -> {
                TimerPreviewCard(isDarkTheme = isDarkTheme)
            }
            DashboardSubPage.DERS_PROGRAMI -> {
                SchedulePreviewCard(isDarkTheme = isDarkTheme)
            }
            else -> {
                GeneralModulePreviewCard(subPage = subPage, isDarkTheme = isDarkTheme)
            }
        }

        // Sınıf Listesine Dön Butonu
        val backBtnContentColor = if (isDarkTheme) BirizNavyDark else Color.White
        Button(
            onClick = onBackToClassList,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                contentColor = backBtnContentColor
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = backBtnContentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Sınıf Listesine Dön",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = backBtnContentColor
            )
        }

        Spacer(modifier = Modifier.height(80.dp)) // Floating dock payı
    }
}

/**
 * 1. Oturma Planı Önizleme Kartı (Image 1 Middle Phone Referansı)
 */
@Composable
private fun SeatingPlanPreviewCard(isDarkTheme: Boolean) {
    BentoCard(
        isDark = isDarkTheme,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Akıllı Oturma Düzeni Krokisi",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Tahta Göstergesi
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFF334155),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(26.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "YAZI TAHTASI / AKILLI TAHTA",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Öğrenci Sıra Matrisi (2'li masalar)
            val rows = listOf(
                Pair("101 Ali K.", "102 Zeynep D."),
                Pair("103 Yusuf T.", "104 Elif M."),
                Pair("105 Can B.", "106 Selin A.")
            )

            rows.forEachIndexed { index, pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    DeskItem(name = pair.first, isDark = isDarkTheme)
                    DeskItem(name = pair.second, isDark = isDarkTheme)
                }
                if (index < rows.size - 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "✨ Boy, görme durumu ve pedagojik eşleşmeye göre otomatik yerleşim",
                fontSize = 11.sp,
                color = BirizTurquoise,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DeskItem(name: String, isDark: Boolean) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isDark) Color(0x35334E68) else Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, if (isDark) BirizDarkCardBorder else BirizBorder),
        modifier = Modifier
            .width(135.dp)
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = BirizTurquoise,
                modifier = Modifier.size(16.dp)
            ) {}
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = name,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) BirizDarkTextPrimary else BirizNavyPrimary
            )
        }
    }
}

/**
 * 2. Yıldızlar Sınıfı Önizleme Kartı (Image 1 Middle Phone Referansı)
 */
@Composable
private fun ClassStarsPreviewCard(isDarkTheme: Boolean) {
    BentoCard(
        isDark = isDarkTheme,
        glowColor = BirizAmber.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Haftanın Yıldız Öğrencileri Podyumu",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
            )
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                PodiumColumn(rank = 2, name = "Zeynep D.", stars = "18 ⭐", height = 80.dp, color = Color(0xFF94A3B8), isDark = isDarkTheme)
                PodiumColumn(rank = 1, name = "Ahmet Y.", stars = "24 ⭐", height = 110.dp, color = BirizAmber, isDark = isDarkTheme)
                PodiumColumn(rank = 3, name = "Elif M.", stars = "15 ⭐", height = 65.dp, color = Color(0xFFCD7F32), isDark = isDarkTheme)
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isDarkTheme) Color(0x25E67E22) else Color(0xFFFEF3C7),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🏆 Velilere anlık tebrik kartı ve haftalık başarı rozetleri gönderimi",
                    fontSize = 11.sp,
                    color = if (isDarkTheme) BirizAmber else Color(0xFF92400E),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(10.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun PodiumColumn(
    rank: Int,
    name: String,
    stars: String,
    height: androidx.compose.ui.unit.Dp,
    color: Color,
    isDark: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) BirizDarkTextPrimary else BirizNavyPrimary
        )
        Text(
            text = stars,
            fontSize = 10.sp,
            color = BirizAmber,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
            color = color.copy(alpha = if (isDark) 0.35f else 0.25f),
            border = BorderStroke(1.5.dp, color),
            modifier = Modifier
                .width(85.dp)
                .height(height)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "#$rank",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = color
                )
            }
        }
    }
}

/**
 * 3. Şanslı Öğrenci Önizleme Kartı
 */
@Composable
private fun LuckyStudentPreviewCard(isDarkTheme: Boolean) {
    BentoCard(
        isDark = isDarkTheme,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Rastgele Soru-Cevap Çarkı",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
            )
            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = CircleShape,
                color = if (isDarkTheme) Color(0x3028B8C2) else Color(0xFFE0F7F6),
                border = BorderStroke(2.dp, BirizTurquoise),
                modifier = Modifier.size(110.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = BirizTurquoise,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            text = "Çarkı Çevir",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BirizTurquoise
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "🎯 Tüm öğrencilere eşit söz hakkı veren ses efektli şans tekerleği",
                fontSize = 11.sp,
                color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 4. Zamanlayıcı Önizleme Kartı
 */
@Composable
private fun TimerPreviewCard(isDarkTheme: Boolean) {
    BentoCard(
        isDark = isDarkTheme,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Sınıf İçi Pomodoro & Etkinlik Sayacı",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
            )
            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = CircleShape,
                color = if (isDarkTheme) Color(0x3513294B) else BirizNavyPrimary,
                border = BorderStroke(3.dp, BirizTurquoise),
                modifier = Modifier.size(120.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "25:00",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Kitap Okuma Saati",
                            fontSize = 9.sp,
                            color = BirizTurquoise
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "⏱️ Sesli zil uyarısı ve tenefüs hatırlatıcısı",
                fontSize = 11.sp,
                color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
            )
        }
    }
}

/**
 * 5. Ders Programı Önizleme Kartı
 */
@Composable
private fun SchedulePreviewCard(isDarkTheme: Boolean) {
    BentoCard(
        isDark = isDarkTheme,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Haftalık Ders Dağılım Matrisi",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))

            val days = listOf("Pzt", "Sal", "Çar", "Per", "Cum")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                days.forEach { day ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDarkTheme) Color(0x35334E68) else Color(0xFFF1F5F9),
                        modifier = Modifier
                            .weight(1f)
                            .padding(2.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                            Text(text = day, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = BirizTurquoise)
                            Text(text = "6 Ders", fontSize = 9.sp, color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 6. Genel Modül Kartı
 */
@Composable
private fun GeneralModulePreviewCard(subPage: DashboardSubPage, isDarkTheme: Boolean) {
    BentoCard(
        isDark = isDarkTheme,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = BirizTurquoise.copy(alpha = 0.2f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = BirizTurquoise,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "${subPage.title} Mimarisi Hazır",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                )
                Text(
                    text = "Faz 2 geliştirmeleri kapsamında kullanıma sunulacaktır.",
                    fontSize = 11.sp,
                    color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                )
            }
        }
    }
}
