package com.example.ui.screens

import com.example.data.model.UserRole
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Settings
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
import com.example.R
import com.example.data.model.Announcement
import com.example.data.model.BirizNotification
import com.example.data.model.ChatMessage
import com.example.data.model.ClassRoom
import com.example.data.model.ScheduleConfig
import com.example.data.model.ScheduleLesson
import com.example.data.model.SeatingPlanState
import com.example.data.model.Student
import com.example.data.repository.StarRewardPersistenceHelper
import com.example.ui.components.BentoCard
import com.example.ui.components.ThemeToggleIconButton
import com.example.ui.components.UserAvatarCircle
import com.example.ui.components.UserProfileMenuDialog
import com.example.ui.theme.BirizAmber
import com.example.ui.theme.BirizBorder
import com.example.ui.theme.BirizDarkCardBorder
import com.example.ui.theme.BirizDarkSurfaceGlass
import com.example.ui.theme.BirizDarkTextMuted
import com.example.ui.theme.BirizDarkTextSecondary
import com.example.ui.theme.BirizNavyDark
import com.example.ui.theme.BirizNavyLight
import com.example.ui.theme.BirizNavyPrimary
import com.example.ui.theme.BirizTextMuted
import com.example.ui.theme.BirizTextSecondary
import com.example.ui.theme.BirizTurquoise
import com.example.ui.viewmodel.AuthenticatedUser
import com.example.util.LibraryPersistenceHelper
import com.example.util.ScheduleHelper
import com.example.util.TurkishTextHelper
import androidx.compose.ui.res.painterResource

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
    currentUser: AuthenticatedUser,
    notifications: List<BirizNotification>,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onLinkStudentByCode: (String) -> Unit,
    onUnlinkStudent: (String) -> Unit = {},
    onSelectActiveStudent: (String) -> Unit,
    onDeleteRole: () -> Unit = {},
    onDeleteEntireAccount: () -> Unit = {},
    onOpenNotificationCenter: () -> Unit,
    onLogout: () -> Unit = {},
    onVotePoll: (String, String) -> Unit,
    onSendChatMessage: (String, String) -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(ParentPortalTab.DASHBOARD) }
    var showLinkCodeDialog by remember { mutableStateOf(false) }
    var showProfileMenuDialog by remember { mutableStateOf(false) }
    var studentToUnlink by remember { mutableStateOf<Student?>(null) }
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
            // --- 1. ÜST HEADER: BENTO GLASSMORPHISM ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isDarkTheme) BirizDarkSurfaceGlass else BirizNavyPrimary,
                shadowElevation = if (isDarkTheme) 0.dp else 6.dp,
                border = BorderStroke(
                    1.dp,
                    if (isDarkTheme) BirizDarkCardBorder else Color.Transparent
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isDarkTheme) {
                                Brush.verticalGradient(
                                    listOf(BirizDarkSurfaceGlass, Color(0xCC0E1A38))
                                )
                            } else {
                                Brush.verticalGradient(
                                    listOf(BirizNavyPrimary, BirizNavyLight)
                                )
                            }
                        )
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Sol: BİRİZ Amblem + Kullanıcı İsmi (Tek Satır)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .padding(3.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    androidx.compose.foundation.Image(
                                        painter = painterResource(id = R.drawable.ic_biriz_emblem),
                                        contentDescription = "BİRİZ Amblem",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "${currentUser.fullName} 👋",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.5.sp,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "BİRİZ Veli Modülü",
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            // Sağ İkon Butonları: Bildirim (sol) + Kullanıcı Avatarı (sağ başta)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // 🔔 Bildirim Rozeti (Profil Avatarının solunda)
                                val unreadNotificationsCount = notifications.count { !it.isRead }
                                Surface(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .clickable { onOpenNotificationCenter() }
                                        .testTag("parent_notification_bell"),
                                    shape = CircleShape,
                                    color = if (isDarkTheme) Color(0x35334E68) else Color(0x20FFFFFF),
                                    border = BorderStroke(1.dp, if (isDarkTheme) Color(0x4064748B) else Color(0x30FFFFFF))
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Notifications,
                                            contentDescription = "Bildirimler",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        if (unreadNotificationsCount > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(top = 3.dp, end = 3.dp)
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(BirizAmber)
                                            )
                                        }
                                    }
                                }

                                // 👤 Sağ Başta Kullanıcı Avatarı (Google profil fotosu / baş harfler)
                                UserAvatarCircle(
                                    user = currentUser,
                                    isDarkTheme = isDarkTheme,
                                    onClick = { showProfileMenuDialog = true },
                                    size = 34.dp
                                )
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
                                            if (isSelected) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(18.dp)
                                                        .clip(CircleShape)
                                                        .background(BirizNavyDark.copy(alpha = 0.2f))
                                                        .clickable { studentToUnlink = student }
                                                        .testTag("chip_unlink_${student.id}"),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Öğrenciyi Kaldır",
                                                        tint = BirizNavyDark,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
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
                                        Spacer(modifier = Modifier.height(4.dp))
                                        IconButton(
                                            onClick = { studentToUnlink = activeStudent },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .testTag("unlink_student_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Öğrenci Bağlantısını Kaldır",
                                                tint = Color(0xFFEF4444).copy(alpha = 0.85f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
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
                            val studentReadingRecords = remember(activeStudent, classRoom) {
                                if (activeStudent != null && classRoom != null) {
                                    LibraryPersistenceHelper.loadReadingRecords(context, classRoom.id)
                                        .filter { it.studentId == activeStudent.id }
                                } else emptyList()
                            }
                            BentoCard(isDark = isDarkTheme, modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(text = "📖 Okuma Değerlendirmeleri ve Kitap Geçmişi", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = if (isDarkTheme) Color.White else BirizNavyPrimary)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    if (studentReadingRecords.isEmpty()) {
                                        Text(text = "Henüz öğretmen tarafından kitap okuma kaydı girilmedi.", fontSize = 12.sp, color = Color.Gray)
                                    } else {
                                        studentReadingRecords.forEach { rec ->
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
                                                        Text(text = rec.bookName, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                        Text(text = "+${rec.earnedStars} ⭐", fontSize = 12.sp, fontWeight = FontWeight.Black, color = BirizAmber)
                                                    }
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(text = "Sayfa: ${rec.pageCount} • Tamamlandı", fontSize = 11.sp, color = Color.Gray)
                                                }
                                            }
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
                                    Text(text = "Henüz öğretmen tarafından sınav veya kazanım değerlendirmesi girilmedi.", fontSize = 12.sp, color = Color.Gray)
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
                            val seatEntry = seatingPlanState.assignments.entries.firstOrNull { it.value == activeStudent?.id }
                            val seatDesc = if (seatEntry != null) {
                                val parts = seatEntry.key.split("-")
                                val g = if (parts.isNotEmpty()) (parts[0].removePrefix("g").toIntOrNull() ?: 0) + 1 else 1
                                val r = if (parts.size > 1) (parts[1].removePrefix("r").toIntOrNull() ?: 0) + 1 else 1
                                "Grup $g, Sıra $r"
                            } else null

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
                                                Text(text = "${activeStudent?.fullName ?: ""} (Sıra Konumu)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                                                Text(
                                                    text = seatDesc ?: "Sınıf oturma planında henüz yerleştirilmedi.",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF166534)
                                                )
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
                        placeholder = { Text("Örn: 8K3N9P2X", fontSize = 12.sp) },
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

    // --- ÖĞRENCİ BAĞLANTISINI KALDIRMA ONAY DİYALOĞU ---
    if (studentToUnlink != null) {
        val s = studentToUnlink!!
        AlertDialog(
            onDismissRequest = { studentToUnlink = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⚠️", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Öğrenci Bağlantısını Kaldır", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = "${s.fullName} (#${s.studentNumber}) adlı öğrencinin veli takip bağlantısını kaldırmak istediğinize emin misiniz?",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Bu işlem sonucunda öğrenci veli profilinizden kaldırılacaktır. Dilediğiniz zaman öğretmenden alacağınız 8 haneli Veli Katılım Kodu ile tekrar bağlayabilirsiniz.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUnlinkStudent(s.id)
                        studentToUnlink = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Evet, Kaldır", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { studentToUnlink = null }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    // --- VELİ PROFİLİ VE HESAP AYARLARI DİYALOĞU ---
    if (showProfileMenuDialog) {
        UserProfileMenuDialog(
            currentUser = currentUser,
            currentRole = UserRole.PARENT,
            classRoom = classRoom,
            linkedStudents = linkedStudents,
            isDarkTheme = isDarkTheme,
            onToggleTheme = onToggleTheme,
            onDeleteRole = onDeleteRole,
            onDeleteEntireAccount = onDeleteEntireAccount,
            onCheckUpdate = {},
            onLogout = onLogout,
            onDismissRequest = { showProfileMenuDialog = false }
        )
    }
}
