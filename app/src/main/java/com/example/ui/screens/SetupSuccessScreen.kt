package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ClassRoom
import com.example.ui.components.BentoCard
import com.example.ui.components.ThemeToggleIconButton
import com.example.ui.theme.BirizAmber
import com.example.ui.theme.BirizBorder
import com.example.ui.theme.BirizDarkBgBottom
import com.example.ui.theme.BirizDarkBgTop
import com.example.ui.theme.BirizDarkCardBorder
import com.example.ui.theme.BirizDarkSurfaceGlass
import com.example.ui.theme.BirizDarkTextPrimary
import com.example.ui.theme.BirizDarkTextSecondary
import com.example.ui.theme.BirizNavyDark
import com.example.ui.theme.BirizNavyLight
import com.example.ui.theme.BirizNavyPrimary
import com.example.ui.theme.BirizSuccess
import com.example.ui.theme.BirizTextPrimary
import com.example.ui.theme.BirizTextSecondary
import com.example.ui.theme.BirizTurquoise
import com.example.ui.theme.BirizTurquoiseSoft

@Composable
fun SetupSuccessScreen(
    classRoom: ClassRoom,
    onNavigateToDashboard: () -> Unit,
    onTestAnotherClass: () -> Unit,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {}
) {
    val backgroundBrush = if (isDarkTheme) {
        Brush.verticalGradient(listOf(BirizDarkBgTop, BirizDarkBgBottom))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9)))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        // Sağ üst köşe tema değiştirici (Sabit, dokunma engellenemez)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            ThemeToggleIconButton(
                isDark = isDarkTheme,
                onToggle = onToggleTheme
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Üst Rozet & Logo
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_biriz_emblem),
                    contentDescription = "BİRİZ Resmi Amblemi",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = BirizTurquoise.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, BirizTurquoise.copy(alpha = 0.5f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = BirizTurquoise,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "KURULUM BAŞARIYLA TAMAMLANDI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BirizTurquoise
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Tebrikler, Dijital Sınıfınız Hazır!",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${classRoom.schoolName} ${classRoom.gradeLevel}/${classRoom.section} şubesi BİRİZ ağına bağlandı.",
                fontSize = 13.sp,
                color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Bento Künye Kartı
            BentoCard(
                isDark = isDarkTheme,
                glowColor = BirizTurquoise.copy(alpha = 0.4f),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("setup_success_card")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sınıf Künyesi & Yetki Bilgisi",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                SuccessInfoRow("Okul Adı", classRoom.schoolName, isDark = isDarkTheme)
                SuccessInfoRow("Konum", "${classRoom.cityName} / ${classRoom.districtName}", isDark = isDarkTheme)
                SuccessInfoRow("Sınıf ve Şube", "${classRoom.gradeLevel}/${classRoom.section} Sınıfı", isDark = isDarkTheme)
                SuccessInfoRow("Yönetici Öğretmen", classRoom.teacherName, isDark = isDarkTheme)

                Spacer(modifier = Modifier.height(16.dp))

                // Öğrenciye Özel Veli Katılım Kodu Bilgilendirmesi (Sınıf Kodu İptal Edildi)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDarkTheme) Color(0x3028B8C2) else Color(0xFFE6FFFA),
                    border = BorderStroke(1.dp, BirizTurquoise.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = BirizTurquoise,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Öğrenciye Özel Veli Katılım Kodları",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Her öğrenciniz için sadece o öğrenciye ait 8 haneli benzersiz 'Veli Katılım Kodu', Sınıf Listesi modülünde otomatik olarak üretilecektir.",
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary,
                            lineHeight = 17.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Güvenlik & KVKK Rozeti
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = BirizSuccess,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "KVKK ve MEB standartları gereği bu şubenin tek yönetici öğretmenisiniz.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDarkTheme) Color(0xFF4ADE80) else Color(0xFF166534)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Aksiyon Butonları
            val primaryBtnContentColor = if (isDarkTheme) BirizNavyDark else Color.White
            Button(
                onClick = onNavigateToDashboard,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("go_to_dashboard_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                    contentColor = primaryBtnContentColor
                )
            ) {
                Text(
                    text = "Öğretmen Paneline Geç",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = primaryBtnContentColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = primaryBtnContentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onTestAnotherClass,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("test_another_class_button"),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder)
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = null,
                    tint = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Başka Bir Sınıf / Şube Test Et",
                    color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SuccessInfoRow(label: String, value: String, isDark: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = if (isDark) BirizDarkTextSecondary else BirizTextSecondary
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) BirizDarkTextPrimary else BirizNavyPrimary
        )
    }
}
