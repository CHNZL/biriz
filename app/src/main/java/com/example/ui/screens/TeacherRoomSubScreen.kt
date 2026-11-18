package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClassRoom
import com.example.data.model.Student
import com.example.ui.theme.BirizAmber
import com.example.ui.theme.BirizBorder
import com.example.ui.theme.BirizDarkCardBorder
import com.example.ui.theme.BirizDarkCardGlass
import com.example.ui.theme.BirizDarkTextMuted
import com.example.ui.theme.BirizDarkTextPrimary
import com.example.ui.theme.BirizDarkTextSecondary
import com.example.ui.theme.BirizNavyDark
import com.example.ui.theme.BirizNavyPrimary
import com.example.ui.theme.BirizTextMuted
import com.example.ui.theme.BirizTextPrimary
import com.example.ui.theme.BirizTextSecondary
import com.example.ui.theme.BirizTurquoise
import com.example.ui.viewmodel.DashboardSubPage

private data class UpcomingTeacherRoomModule(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color
)

@Composable
fun TeacherRoomSubScreen(
    classRoom: ClassRoom,
    students: List<Student>,
    currentSubPage: DashboardSubPage,
    isDarkTheme: Boolean,
    onSelectSubPage: (DashboardSubPage) -> Unit,
    onBackToClassList: () -> Unit
) {
    BackHandler {
        onBackToClassList()
    }

    val primaryText = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
    val secondaryText = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
    val mutedText = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted

    val upcomingModules = listOf(
        UpcomingTeacherRoomModule(
            title = "Zümre Toplantı Tutanağı",
            subtitle = "Sınıf düzeyi kararları ve dönemlik zümre belgeleri",
            icon = Icons.Default.Groups,
            accentColor = Color(0xFF4F46E5)
        ),
        UpcomingTeacherRoomModule(
            title = "Veli Toplantısı Evrakları",
            subtitle = "Toplantı gündemi, imza çizelgesi ve görüşme tutanağı",
            icon = Icons.Default.People,
            accentColor = Color(0xFF0D9488)
        ),
        UpcomingTeacherRoomModule(
            title = "Sosyal Kulüp Planları",
            subtitle = "Kulüp öğrenci dağılımı ve yıllık çalışma planı",
            icon = Icons.Default.Coffee,
            accentColor = Color(0xFFD97706)
        ),
        UpcomingTeacherRoomModule(
            title = "Okul Gezisi Belgeleri",
            subtitle = "Veli izin dilekçesi ve gezi katılım listeleri",
            icon = Icons.Default.DirectionsBus,
            accentColor = Color(0xFF0284C7)
        ),
        UpcomingTeacherRoomModule(
            title = "Hazır Dilekçe ve Formlar",
            subtitle = "Resmî yazışma şablonları ve hızlı belge oluşturma",
            icon = Icons.Default.Article,
            accentColor = Color(0xFF7C3AED)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 88.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 580.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Ana Hazırlık Aşamasında Kartı
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("teacher_room_coming_soon_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkTheme) BirizDarkCardGlass else Color.White
                ),
                border = BorderStroke(
                    width = 1.2.dp,
                    color = if (isDarkTheme) Color(0x55F59E0B) else Color(0xFFFDE68A)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDarkTheme) 0.dp else 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = if (isDarkTheme) {
                                    listOf(Color(0x35F59E0B), Color(0x284F46E5), Color.Transparent)
                                } else {
                                    listOf(Color(0xFFFFFBEB), Color(0xFFEEF2FF), Color.White)
                                }
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Katmanlı İkon Rozeti
                        Box(
                            modifier = Modifier.size(84.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isDarkTheme) Color(0x30F59E0B) else Color(0xFFFEF3C7),
                                border = BorderStroke(1.5.dp, BirizAmber.copy(alpha = 0.6f)),
                                modifier = Modifier.size(76.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Coffee,
                                        contentDescription = "Öğretmenler Odası",
                                        tint = if (isDarkTheme) Color(0xFFFCD34D) else Color(0xFFB45309),
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = if (isDarkTheme) Color(0xFF1E293B) else BirizNavyPrimary,
                                border = BorderStroke(2.dp, if (isDarkTheme) Color(0xFF0F172A) else Color.White),
                                modifier = Modifier
                                    .size(30.dp)
                                    .align(Alignment.BottomEnd)
                                    .offset(x = (-2).dp, y = (-2).dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Construction,
                                        contentDescription = null,
                                        tint = Color(0xFFFBBF24),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Durum Rozeti
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (isDarkTheme) Color(0x38F59E0B) else Color(0xFFFEF3C7),
                            border = BorderStroke(
                                1.dp,
                                if (isDarkTheme) Color(0x80F59E0B) else Color(0xFFF59E0B)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = if (isDarkTheme) Color(0xFFFDE68A) else Color(0xFFB45309),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "HAZIRLIK AŞAMASINDA",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.6.sp,
                                    color = if (isDarkTheme) Color(0xFFFDE68A) else Color(0xFFB45309)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Öğretmenler Odası",
                            fontSize = 21.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = primaryText,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Bu bölüm şu anda geliştirme ve hazırlık aşamasındadır. " +
                                "Öğretmenlerimizin günlük evrak, tutanak ve planlama işlerini " +
                                "kolaylaştıracak tüm araçlar yakında burada kullanıma açılacaktır.",
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = secondaryText,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // 2. Yakında Eklenecek Özellikler Önizleme Kartı
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkTheme) BirizDarkCardGlass else Color.White
                ),
                border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Yakında Öğretmenler Odası'nda",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Hazırlanan resmî belge ve toplantı modülleri",
                        fontSize = 11.5.sp,
                        color = mutedText
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    upcomingModules.forEachIndexed { index, module ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = module.accentColor.copy(alpha = if (isDarkTheme) 0.24f else 0.12f),
                                border = BorderStroke(1.dp, module.accentColor.copy(alpha = 0.35f)),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = module.icon,
                                        contentDescription = null,
                                        tint = if (isDarkTheme) Color.White else module.accentColor,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = module.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkTheme) BirizDarkTextPrimary else BirizTextPrimary
                                )
                                Text(
                                    text = module.subtitle,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = secondaryText
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDarkTheme) Color(0x28334E68) else Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = "Yakında",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = mutedText,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (index < upcomingModules.lastIndex) {
                            HorizontalDivider(
                                color = if (isDarkTheme) Color(0x2264748B) else Color(0xFFF1F5F9),
                                thickness = 0.8.dp
                            )
                        }
                    }
                }
            }

            // 3. Ana Sayfaya Dön Butonu (48dp+ dokunma hedefi)
            Button(
                onClick = onBackToClassList,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                    contentColor = if (isDarkTheme) BirizNavyDark else Color.White
                ),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("teacher_room_back_home_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Ana Sayfaya Dön",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
