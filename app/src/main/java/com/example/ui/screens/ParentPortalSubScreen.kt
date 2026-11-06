package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Announcement
import com.example.data.model.ChatMessage
import com.example.data.model.ClassRoom
import com.example.data.model.ScheduleConfig
import com.example.data.model.ScheduleLesson
import com.example.data.model.SeatingPlanState
import com.example.data.model.Student
import com.example.data.repository.StarRewardPersistenceHelper
import com.example.ui.components.BentoCard
import com.example.ui.theme.BirizAmber
import com.example.ui.theme.BirizBorder
import com.example.ui.theme.BirizDarkCardBorder
import com.example.ui.theme.BirizDarkTextMuted
import com.example.ui.theme.BirizDarkTextSecondary
import com.example.ui.theme.BirizNavyDark
import com.example.ui.theme.BirizNavyPrimary
import com.example.ui.theme.BirizTextMuted
import com.example.ui.theme.BirizTextSecondary
import com.example.ui.theme.BirizTurquoise
import com.example.util.ScheduleHelper
import com.example.util.TurkishTextHelper

enum class ParentPortalTab(val title: String, val icon: String) {
    DASHBOARD("Gelişim & Yıldızlar", "🌟"),
    READING("Kitaplık & Okuma", "📚"),
    EXAMS("Sınav & Kazanım", "📊"),
    SCHEDULE("Ders Programı", "📅"),
    SEATING("Oturma Planı", "🪑"),
    ANNOUNCEMENTS("Duyuru & Anket", "📢"),
    CHAT("Öğretmen İletişim", "💬")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentPortalSubScreen(
    classRoom: ClassRoom?,
    allStudents: List<Student>,
    linkedStudents: List<Student>,
    activeStudent: Student?,
    announcements: List<Announcement>,
    chatMessages: List<ChatMessage>,
    scheduleConfig: ScheduleConfig,
    scheduleLessons: List<ScheduleLesson>,
    seatingPlanState: SeatingPlanState,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onLinkStudentByCode: (String) -> Unit,
    onSelectActiveStudent: (String) -> Unit,
    onSwitchToTeacherMode: () -> Unit,
    onVotePoll: (String, String) -> Unit,
    onSendChatMessage: (String, String) -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(ParentPortalTab.DASHBOARD) }
    var showLinkCodeDialog by remember { mutableStateOf(false) }
    var inviteCodeInput by remember { mutableStateOf("") }
    var chatInputText by remember { mutableStateOf("") }

    val activeStudentStars = remember(activeStudent, classRoom) {
        if (activeStudent != null && classRoom != null) {
            StarRewardPersistenceHelper.loadStarRecords(context, classRoom.id)
                .filter { it.studentId == activeStudent.id }
        } else emptyList()
    }

    val totalStars = remember(activeStudentStars) { activeStudentStars.sumOf { it.starCount } }

    val starLevelBadge = remember(totalStars) {
        when {
            totalStars >= 100 -> Pair("💎 Süper Nova / Sınıf Efsanesi", Color(0xFF9333EA))
            totalStars >= 50 -> Pair("🥇 Sınıf Yıldızı", BirizAmber)
            totalStars >= 25 -> Pair("🥈 Yıldız Avcısı", Color(0xFF0284C7))
            totalStars >= 10 -> Pair("🥉 Çırak Yıldız", Color(0xFF10B981))
            else -> Pair("⭐ Yıldız Adayı", Color(0xFF64748B))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkTheme) Color(0xFF070E1B) else Color(0xFFF8FAFC))
            .testTag("screen_parent_portal")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // --- 1. ÜST HEADER VE ÇOCUK DEĞİŞTİRİCİ ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isDarkTheme) Color(0xFF0F1B33) else BirizNavyPrimary,
                border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFF1E3A8A))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "👨‍👩‍👧‍👦", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "BİRİZ Veli Portalı",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "🔒 Güvenli Öğrenci Gelişim Takibi",
                                    fontSize = 10.sp,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onToggleTheme,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Tema Değiştir",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Surface(
                                onClick = onSwitchToTeacherMode,
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SupervisorAccount,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Öğretmen Modu",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. SATIR: Bağlı Çocuklar Listesi / Seçici
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (linkedStudents.isEmpty()) {
                            Surface(
                                onClick = { showLinkCodeDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                color = BirizAmber,
                                modifier = Modifier.height(36.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("🔑 8 Haneli Veli Kodunu Gir ve Çocuğunu Bağla", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            // Quick Demo Button removed as requested by user
                        } else {
                            linkedStudents.forEach { student ->
                                val isSelected = activeStudent?.id == student.id
                                Surface(
                                    onClick = { onSelectActiveStudent(student.id) },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) BirizTurquoise else Color.White.copy(alpha = 0.15f),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = if (student.gender.equals("Kız", ignoreCase = true)) "👧" else "👦", fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${student.fullName} (#${student.studentNumber})",
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                            color = if (isSelected) BirizNavyDark else Color.White
                                        )
                                    }
                                }
                            }

                            Surface(
                                onClick = { showLinkCodeDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Kardeş Ekle", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // --- 2. SEKMELER DİZİSİ (PARENT PORTAL TABS) ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ParentPortalTab.entries.forEach { tab ->
                        val isSelected = activeTab == tab
                        Surface(
                            onClick = { activeTab = tab },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) {
                                if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                            } else Color.Transparent,
                            border = if (isSelected) null else BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFFCBD5E1)),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = tab.icon, fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tab.title,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                    color = if (isSelected) {
                                        if (isDarkTheme) BirizNavyDark else Color.White
                                    } else {
                                        if (isDarkTheme) Color.LightGray else Color(0xFF475569)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // --- 3. ANA İÇERİK EKRANI (SEÇİLİ SEKMEYE GÖRE) ---
            if (activeStudent == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BentoCard(
                        isDark = isDarkTheme,
                        glowColor = BirizAmber,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "👨‍👩‍👧‍👦", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "BİRİZ Veli Portalına Hoş Geldiniz",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDarkTheme) Color.White else BirizNavyPrimary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Çocuğunuzun gelişimini, yıldızlarını, sınav sonuçlarını ve ödevlerini canlı takip etmek için öğretmeninizin verdiği 8 haneli Veli Katılım Kodunu girin.",
                                fontSize = 12.sp,
                                color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 17.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showLinkCodeDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = BirizAmber),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("🔑 8 Haneli Katılım Kodunu Gir", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // TAB 1: DASHBOARD & YILDIZLAR
                    if (activeTab == ParentPortalTab.DASHBOARD) {
                        item {
                            // Student Hero Profile Card
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isDarkTheme) Color(0xFF0F1F38) else Color(0xFFEFF6FF),
                                border = BorderStroke(1.5.dp, if (isDarkTheme) BirizTurquoise else Color(0xFFBFDBFE)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (activeStudent.gender.equals("Kız", ignoreCase = true)) Color(0xFFFCE7F3) else Color(0xFFE0F2FE),
                                        modifier = Modifier.size(60.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(text = if (activeStudent.gender.equals("Kız", ignoreCase = true)) "👧" else "👦", fontSize = 32.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = BirizTurquoise
                                            ) {
                                                Text(
                                                    text = "NO: #${activeStudent.studentNumber}",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = BirizNavyDark,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = classRoom?.let { "${it.gradeLevel}/${it.section} Sınıfı" } ?: "Sınıf Belirtilmedi",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDarkTheme) Color.LightGray else Color(0xFF475569)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = activeStudent.fullName,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (isDarkTheme) Color.White else BirizNavyPrimary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = starLevelBadge.second.copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, starLevelBadge.second)
                                        ) {
                                            Text(
                                                text = starLevelBadge.first,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = starLevelBadge.second,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(text = "⭐ $totalStars", fontSize = 22.sp, fontWeight = FontWeight.Black, color = BirizAmber)
                                        Text(text = "Toplam Yıldız", fontSize = 10.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }

                        // Yıldız Kazanım Geçmişi
                        item {
                            BentoCard(isDark = isDarkTheme, modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(text = "📜 Yıldız Kazanım Geçmişi", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = if (isDarkTheme) Color.White else BirizNavyPrimary)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    if (activeStudentStars.isEmpty()) {
                                        Text(text = "Henüz yıldız kaydı bulunmuyor.", fontSize = 12.sp, color = Color.Gray)
                                    } else {
                                        activeStudentStars.take(6).forEach { record ->
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column {
                                                        val titleText = if (record.note.isNotBlank()) record.note else record.categoryName
                                                        val dateStr = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(record.timestamp))
                                                        Text(text = titleText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                        Text(text = "$dateStr • ${record.categoryName}", fontSize = 10.sp, color = Color.Gray)
                                                    }
                                                    Text(text = "+${record.starCount} ⭐", fontSize = 13.sp, fontWeight = FontWeight.Black, color = BirizAmber)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // TAB 2: KİTAPLIK & OKUMA
                    if (activeTab == ParentPortalTab.READING) {
                        item {
                            BentoCard(isDark = isDarkTheme, modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(text = "📖 Okuma Değerlendirmeleri ve Kitap Geçmişi", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = if (isDarkTheme) Color.White else BirizNavyPrimary)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    listOf(
                                        Pair("Okuma Hızı ve Akıcılığı", "⭐⭐⭐⭐⭐ (%90)"),
                                        Pair("Anlatma / Özetleme Başarısı", "⭐⭐⭐⭐⭐ (%95)"),
                                        Pair("Yazılı Değerlendirme Puanı", "88 / 100"),
                                        Pair("Kitap Sınavı Ortalaması", "92 Puan")
                                    ).forEach { (label, valStr) ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                            Text(text = valStr, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BirizTurquoise)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // TAB 3: SINAV & KAZANIM
                    if (activeTab == ParentPortalTab.EXAMS) {
                        item {
                            BentoCard(isDark = isDarkTheme, modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(text = "📊 Ders Sınavları & Kazanım Analizi", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = if (isDarkTheme) Color.White else BirizNavyPrimary)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    listOf(
                                        Triple("Türkçe 1. Yazılı", "90 Puan", "18 Doğru, 2 Yanlış • Sınıf Ort.: 82"),
                                        Triple("Matematik 1. Yazılı", "85 Puan", "17 Doğru, 3 Yanlış • Sınıf Ort.: 78"),
                                        Triple("Fen Bilimleri 1. Yazılı", "95 Puan", "19 Doğru, 1 Yanlış • Sınıf Ort.: 80")
                                    ).forEach { (exam, score, detail) ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(text = exam, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                                    Text(text = score, fontSize = 13.sp, fontWeight = FontWeight.Black, color = BirizTurquoise)
                                                }
                                                Text(text = detail, fontSize = 10.5.sp, color = Color.Gray)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // TAB 4: DERS PROGRAMI
                    if (activeTab == ParentPortalTab.SCHEDULE) {
                        item {
                            BentoCard(isDark = isDarkTheme, modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(text = "📅 Sınıf Haftalık Ders Programı", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = if (isDarkTheme) Color.White else BirizNavyPrimary)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    scheduleLessons.filter { it.dayOfWeek == "PZT" }.take(6).forEach { lesson ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(text = "${lesson.lessonIndex}. Ders: ${lesson.subjectName}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text(text = lesson.teacherName, fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // TAB 5: OTURMA PLANI
                    if (activeTab == ParentPortalTab.SEATING) {
                        item {
                            BentoCard(isDark = isDarkTheme, modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(text = "🪑 Çocuğunuzun Sınıftaki Oturma Yeri", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = if (isDarkTheme) Color.White else BirizNavyPrimary)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isDarkTheme) Color(0x3010B981) else Color(0xFFDCFCE7),
                                        border = BorderStroke(1.dp, Color(0xFF10B981)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = "📍", fontSize = 24.sp)
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(text = "${activeStudent.fullName} (Sıra Konumu)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                                                Text(text = "2. Küme • Sol Sıra • Ön-Orta Bölge", fontSize = 11.sp, color = Color(0xFF166534))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // TAB 6: DUYURU & ANKET
                    if (activeTab == ParentPortalTab.ANNOUNCEMENTS) {
                        item {
                            BentoCard(isDark = isDarkTheme, modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(text = "📢 Sınıf Öğretmeni Duyuruları & Anketler", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = if (isDarkTheme) Color.White else BirizNavyPrimary)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    if (announcements.isEmpty()) {
                                        Text(text = "Henüz yayınlanmış duyuru bulunmuyor.", fontSize = 12.sp, color = Color.Gray)
                                    } else {
                                        announcements.forEach { ann ->
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Text(text = ann.title, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(text = ann.content, fontSize = 11.5.sp, color = if (isDarkTheme) Color.LightGray else Color(0xFF334155))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // TAB 7: ÇAT / MESAJLAŞMA
                    if (activeTab == ParentPortalTab.CHAT) {
                        item {
                            BentoCard(isDark = isDarkTheme, modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(text = "💬 Sınıf Öğretmeni Birebir Sohbet", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = if (isDarkTheme) Color.White else BirizNavyPrimary)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    val filteredMsgs = chatMessages.filter { it.studentId == activeStudent.id }
                                    LazyColumn(modifier = Modifier.height(220.dp)) {
                                        items(filteredMsgs) { msg ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                                horizontalArrangement = if (msg.isFromTeacher) Arrangement.Start else Arrangement.End
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = if (msg.isFromTeacher) (if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFE2E8F0)) else BirizTurquoise
                                                ) {
                                                    Text(
                                                        text = msg.text,
                                                        fontSize = 11.5.sp,
                                                        color = if (msg.isFromTeacher) (if (isDarkTheme) Color.White else Color.Black) else BirizNavyDark,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedTextField(
                                            value = chatInputText,
                                            onValueChange = { chatInputText = it },
                                            placeholder = { Text("Öğretmene mesaj yazın...", fontSize = 11.sp) },
                                            modifier = Modifier.weight(1f).height(46.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Button(
                                            onClick = {
                                                if (chatInputText.isNotBlank()) {
                                                    onSendChatMessage(activeStudent.id, chatInputText)
                                                    chatInputText = ""
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = BirizTurquoise)
                                        ) {
                                            Icon(Icons.Default.Send, contentDescription = "Gönder", tint = BirizNavyDark, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- LINK STUDENT CODE DIALOG ---
    if (showLinkCodeDialog) {
        AlertDialog(
            onDismissRequest = { showLinkCodeDialog = false },
            title = { Text("🔑 8 Haneli Veli Katılım Kodu", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Sınıf öğretmeninizin verdiği 8 haneli Veli Katılım Kodunu girerek çocuğunuzun gelişim takip hesabını bağlayın.", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = inviteCodeInput,
                        onValueChange = { inviteCodeInput = TurkishTextHelper.normalizeVeliKatilimKodu(it) },
                        placeholder = { Text("Örn: TD8GDLU8 veya 8K3N9P2X", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inviteCodeInput.isNotBlank()) {
                            onLinkStudentByCode(inviteCodeInput.trim())
                            showLinkCodeDialog = false
                            inviteCodeInput = ""
                        }
                    }
                ) {
                    Text("Çocuğumu Bağla")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLinkCodeDialog = false }) { Text("İptal") }
            }
        )
    }
}
