package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Announcement
import com.example.data.model.BirizNotification
import com.example.data.model.Book
import com.example.data.model.ChatMessage
import com.example.data.model.ClassRoom
import com.example.data.model.DeskPosition
import com.example.data.model.DoorPosition
import com.example.data.model.NotificationType
import com.example.data.model.ScheduleConfig
import com.example.data.model.ScheduleLesson
import com.example.data.model.ScheduleTimelineItem
import com.example.data.model.SeatingConfig
import com.example.data.model.SeatingPlanState
import com.example.data.model.StarRewardRecord
import com.example.data.model.Student
import com.example.data.model.Tournament
import com.example.data.model.TournamentMatch
import com.example.data.model.UserRole
import com.example.data.model.WindowPosition
import com.example.data.repository.StarRewardPersistenceHelper
import com.example.ui.components.BentoCard
import com.example.ui.components.NotificationCenterDialog
import com.example.ui.components.UserAvatarCircle
import com.example.ui.components.UserProfileMenuDialog
import com.example.ui.theme.BirizAmber
import com.example.ui.theme.BirizBorder
import com.example.ui.theme.BirizDarkCardBorder
import com.example.ui.theme.BirizDarkCardGlass
import com.example.ui.theme.BirizDarkSurfaceGlass
import com.example.ui.theme.BirizDarkTextMuted
import com.example.ui.theme.BirizDarkTextPrimary
import com.example.ui.theme.BirizDarkTextSecondary
import com.example.ui.theme.BirizGold
import com.example.ui.theme.BirizNavyDark
import com.example.ui.theme.BirizNavyLight
import com.example.ui.theme.BirizNavyPrimary
import com.example.ui.theme.BirizTextMuted
import com.example.ui.theme.BirizTextPrimary
import com.example.ui.theme.BirizTextSecondary
import com.example.ui.theme.BirizTurquoise
import com.example.ui.viewmodel.AuthenticatedUser
import com.example.util.LibraryPersistenceHelper
import com.example.util.ParentPrivacyHelper
import com.example.util.ScheduleHelper
import com.example.util.TournamentEngine
import com.example.util.TournamentPersistenceHelper
import com.example.util.TurkishTextHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Veli Portalı Navigasyon Sekmeleri
 */
enum class ParentPortalTab(
    val title: String,
    val iconVector: ImageVector,
    val emoji: String,
    val showInBottomBar: Boolean = true
) {
    HOME("Ana Sayfa", Icons.Default.Home, "🏠"),
    STARS("Yıldızlar", Icons.Default.Star, "⭐"),
    READING("Kitaplık", Icons.Default.MenuBook, "📚"),
    SCHEDULE("Dersler", Icons.Default.Schedule, "📅"),
    SEATING("Sıra Düzeni", Icons.Default.EventSeat, "🪑", showInBottomBar = false),
    TOURNAMENT("Turnuva", Icons.Default.EmojiEvents, "🏆"),
    COMMUNICATION("İletişim", Icons.Default.Chat, "💬")
}

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
    isNotificationCenterOpen: Boolean = false,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onLinkStudentByCode: (String) -> Unit,
    onUnlinkStudent: (String) -> Unit = {},
    onSelectActiveStudent: (String) -> Unit,
    onDeleteRole: () -> Unit = {},
    onDeleteEntireAccount: () -> Unit = {},
    onOpenNotificationCenter: () -> Unit,
    onCloseNotificationCenter: () -> Unit = {},
    onMarkNotificationAsRead: (String) -> Unit = {},
    onMarkAllNotificationsAsRead: () -> Unit = {},
    onOpenUpdateDialog: () -> Unit = {},
    onLogout: () -> Unit = {},
    onVotePoll: (String, String) -> Unit,
    onSendChatMessage: (String, String) -> Unit,
    onMarkAnnouncementRead: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(ParentPortalTab.HOME) }
    var showLinkCodeDialog by remember { mutableStateOf(false) }
    var showProfileMenuDialog by remember { mutableStateOf(false) }
    var studentToUnlink by remember { mutableStateOf<Student?>(null) }
    var inviteCodeInput by remember { mutableStateOf("") }
    var chatInputText by remember { mutableStateOf("") }

    // Velinin bağlı olduğu çocuk kimlikleri kümesi (Gizlilik ve filtreleme için)
    val linkedStudentIds = remember(linkedStudents) { linkedStudents.map { it.id }.toSet() }

    // Geri Tuşu: Detay ekranlarındayken ana sayfaya dön
    BackHandler(enabled = activeTab != ParentPortalTab.HOME) {
        activeTab = ParentPortalTab.HOME
    }

    // Aktif sınıf kimliği
    val classRoomId = classRoom?.id ?: ""

    // Tüm sınıfın yıldız kayıtları (Firestore canlı dinleyici ile anlık güncellenir)
    var allClassStars by remember(classRoomId) {
        mutableStateOf(
            if (classRoom != null) StarRewardPersistenceHelper.loadStarRecords(context, classRoom.id) else emptyList()
        )
    }

    DisposableEffect(classRoomId) {
        if (classRoom != null) {
            val reg = StarRewardPersistenceHelper.observeFirestoreStars(context, classRoom.id) { updated ->
                allClassStars = updated
            }
            onDispose { reg?.remove() }
        } else {
            onDispose {}
        }
    }

    // Aktif öğrencinin öğretmeninden aldığı yıldız kayıtları ve puanları
    val activeStudentStars = remember(allClassStars, activeStudent) {
        if (activeStudent != null) {
            allClassStars.filter { it.studentId == activeStudent.id }
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

    // Aktif öğrencinin kütüphane ve turnuva verilerini Firestore'dan canlı dinle
    var classBooks by remember { mutableStateOf(emptyList<Book>()) }
    var rawReadingRecords by remember { mutableStateOf(emptyList<com.example.data.model.ReadingRecord>()) }
    var classTournaments by remember { mutableStateOf(emptyList<Tournament>()) }
    var tournamentMatches by remember { mutableStateOf(emptyList<TournamentMatch>()) }

    DisposableEffect(classRoom?.id) {
        val cid = classRoom?.id
        if (!cid.isNullOrBlank()) {
            classBooks = LibraryPersistenceHelper.loadBooks(context, cid)
            rawReadingRecords = LibraryPersistenceHelper.loadReadingRecords(context, cid)
            classTournaments = TournamentPersistenceHelper.loadTournaments(context, cid)

            val booksListener = LibraryPersistenceHelper.observeFirestoreBooks(context, cid) { updated ->
                classBooks = updated
            }
            val recordsListener = LibraryPersistenceHelper.observeFirestoreReadingRecords(context, cid) { updated ->
                rawReadingRecords = updated
            }
            val tourneysListener = TournamentPersistenceHelper.observeFirestoreTournaments(context, cid) { updated ->
                classTournaments = updated
            }
            onDispose {
                booksListener?.remove()
                recordsListener?.remove()
                tourneysListener?.remove()
            }
        } else {
            onDispose {}
        }
    }

    val studentReadingRecords = remember(rawReadingRecords, activeStudent) {
        if (activeStudent != null) {
            rawReadingRecords.filter { it.studentId == activeStudent.id }
        } else emptyList()
    }

    val activeTournament = remember(classTournaments) {
        classTournaments.firstOrNull { it.status == com.example.data.model.TournamentStatus.DEVAM_EDIYOR }
            ?: classTournaments.firstOrNull()
    }

    DisposableEffect(activeTournament?.id) {
        val tid = activeTournament?.id
        if (!tid.isNullOrBlank()) {
            tournamentMatches = TournamentPersistenceHelper.loadMatches(context, tid)
            val matchesListener = TournamentPersistenceHelper.observeFirestoreMatches(context, tid) { updated ->
                tournamentMatches = updated
            }
            onDispose {
                matchesListener?.remove()
            }
        } else {
            tournamentMatches = emptyList()
            onDispose {}
        }
    }

    // Velinin görebileceği duyurular (Tüm sınıfa veya bu öğrenciye özel duyurular)
    val parentAnnouncements = remember(announcements, activeStudent) {
        if (activeStudent == null) announcements
        else announcements.filter { ann ->
            ann.targetType == com.example.data.model.AnnouncementTargetType.ALL_CLASS ||
            ann.targetStudentIds.isEmpty() ||
            ann.targetStudentIds.contains(activeStudent.id)
        }
    }

    // Bugünkü dersler (PZT, SAL, ÇAR, PER, CUM)
    val todayCode = remember {
        when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "PZT"
            Calendar.TUESDAY -> "SAL"
            Calendar.WEDNESDAY -> "ÇAR"
            Calendar.THURSDAY -> "PER"
            Calendar.FRIDAY -> "CUM"
            else -> "PZT" // Hafta sonu için Pazartesi planını önizle
        }
    }
    val todayLessons = remember(scheduleLessons, todayCode) {
        scheduleLessons.filter {
            it.dayOfWeek == todayCode ||
                    (todayCode in listOf("ÇAR", "CAR") && it.dayOfWeek in listOf("ÇAR", "CAR"))
        }.sortedBy { it.lessonIndex }
    }

    // Oturma yeri bilgisi
    val studentSeatDesc = remember(seatingPlanState, activeStudent) {
        if (activeStudent != null) {
            val entry = seatingPlanState.assignments.entries.firstOrNull { it.value == activeStudent.id }
            if (entry != null) {
                val parts = entry.key.split("-")
                val g = if (parts.isNotEmpty()) (parts[0].removePrefix("g").toIntOrNull() ?: 0) + 1 else 1
                val r = if (parts.size > 1) (parts[1].removePrefix("r").toIntOrNull() ?: 0) + 1 else 1
                "Grup $g, Sıra $r"
            } else "Henüz bir sıraya atanmadı"
        } else "Belirtilmedi"
    }

    val primaryTextColor = if (isDarkTheme) BirizDarkTextPrimary else BirizTextPrimary
    val secondaryTextColor = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkTheme) Color(0xFF070E1B) else Color(0xFFF8FAFC))
            .testTag("screen_parent_portal")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // =========================================================================
            // 1. ÜST HEADER: BENTO GLASS & KULLANICI / BİLDİRİM / PROFİL
            // =========================================================================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isDarkTheme) BirizDarkSurfaceGlass else BirizNavyPrimary,
                shadowElevation = if (isDarkTheme) 0.dp else 4.dp,
                border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isDarkTheme) {
                                Brush.verticalGradient(listOf(BirizDarkSurfaceGlass, Color(0xCC0E1A38)))
                            } else {
                                Brush.verticalGradient(listOf(BirizNavyPrimary, BirizNavyLight))
                            }
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Sol: BİRİZ Amblem + Selamlama (Tıklanınca Veli Ana Sayfasına Yönlendirir)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable { activeTab = ParentPortalTab.HOME }
                                    .testTag("parent_header_home_logo")
                            ) {
                                if (activeTab != ParentPortalTab.HOME) {
                                    IconButton(
                                        onClick = { activeTab = ParentPortalTab.HOME },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Ana Sayfaya Dön",
                                            tint = Color.White
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                }

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
                                        contentDescription = "BİRİZ Ana Sayfa",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "${currentUser.fullName} 👋",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = BirizTurquoise.copy(alpha = 0.25f)
                                        ) {
                                            Text(
                                                text = "Veli Bilgilendirme Portalı",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        if (classRoom != null) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "• ${classRoom.gradeLevel}/${classRoom.section}",
                                                fontSize = 11.5.sp,
                                                color = Color.White.copy(alpha = 0.9f),
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }

                            // Sağ: Bildirim Zili + Profil Avatarı
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val unreadNotificationsCount = notifications.count { !it.isRead }
                                Surface(
                                    modifier = Modifier
                                        .size(38.dp)
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
                                            modifier = Modifier.size(20.dp)
                                        )
                                        if (unreadNotificationsCount > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(top = 4.dp, end = 4.dp)
                                                    .size(9.dp)
                                                    .clip(CircleShape)
                                                    .background(BirizAmber)
                                            )
                                        }
                                    }
                                }

                                UserAvatarCircle(
                                    user = currentUser,
                                    isDarkTheme = isDarkTheme,
                                    onClick = { showProfileMenuDialog = true },
                                    size = 38.dp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // -------------------------------------------------------------
                        // 2. ÇOCUK / KARDEŞ SEÇİCİ ÇUBUĞU
                        // -------------------------------------------------------------
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
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("🔑 8 Haneli Veli Kodunu Gir ve Çocuğunu Bağla", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            } else {
                                linkedStudents.forEach { student ->
                                    val isSelected = activeStudent?.id == student.id
                                    Surface(
                                        onClick = { onSelectActiveStudent(student.id) },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) BirizTurquoise else Color.White.copy(alpha = 0.15f),
                                        modifier = Modifier.height(38.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = if (student.gender.equals("Kız", ignoreCase = true)) "👧" else "👦", fontSize = 15.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "${student.fullName} (${student.studentNumber})",
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                                color = if (isSelected) BirizNavyDark else Color.White
                                            )
                                            if (isSelected) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(20.dp)
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
                                                        modifier = Modifier.size(13.dp)
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
                                    modifier = Modifier.height(38.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Kardeş Ekle", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 2. ANA İÇERİK ALANI (SEÇİLİ SEKME VEYA BENTO DASHBOARD)
            // =========================================================================
            Box(modifier = Modifier.weight(1f)) {
                if (activeStudent == null) {
                    ParentEmptyStateView(
                        isDarkTheme = isDarkTheme,
                        onOpenCodeDialog = { showLinkCodeDialog = true }
                    )
                } else {
                    when (activeTab) {
                        ParentPortalTab.HOME -> {
                            ParentDashboardHomeView(
                                student = activeStudent,
                                classRoom = classRoom,
                                totalStars = totalStars,
                                starLevelBadge = starLevelBadge,
                                lastStarRecord = activeStudentStars.maxByOrNull { it.timestamp },
                                todayLessons = todayLessons,
                                studentSeatDesc = studentSeatDesc,
                                readingRecords = studentReadingRecords,
                                classBooks = classBooks,
                                activeTournament = activeTournament,
                                lastAnnouncement = parentAnnouncements.firstOrNull(),
                                lastChatMessage = chatMessages.filter { it.studentId == activeStudent.id }.lastOrNull(),
                                isDarkTheme = isDarkTheme,
                                onNavigate = { activeTab = it }
                            )
                        }

                        ParentPortalTab.STARS -> {
                            ParentStarsDetailView(
                                student = activeStudent,
                                stars = activeStudentStars,
                                allClassStars = allClassStars,
                                allStudents = allStudents,
                                classRoom = classRoom,
                                linkedStudentIds = linkedStudentIds,
                                isDarkTheme = isDarkTheme
                            )
                        }

                        ParentPortalTab.READING -> {
                            ParentReadingDetailView(
                                student = activeStudent,
                                records = studentReadingRecords,
                                classBooks = classBooks,
                                isDarkTheme = isDarkTheme
                            )
                        }

                        ParentPortalTab.SCHEDULE -> {
                            ParentScheduleDetailView(
                                scheduleConfig = scheduleConfig,
                                scheduleLessons = scheduleLessons,
                                isDarkTheme = isDarkTheme
                            )
                        }

                        ParentPortalTab.SEATING -> {
                            ParentSeatingDetailView(
                                student = activeStudent,
                                seatingPlanState = seatingPlanState,
                                allStudents = allStudents,
                                linkedStudentIds = linkedStudentIds,
                                isDarkTheme = isDarkTheme
                            )
                        }

                        ParentPortalTab.TOURNAMENT -> {
                            ParentTournamentDetailView(
                                student = activeStudent,
                                tournament = activeTournament,
                                matches = tournamentMatches,
                                linkedStudentIds = linkedStudentIds,
                                isDarkTheme = isDarkTheme
                            )
                        }

                        ParentPortalTab.COMMUNICATION -> {
                            ParentCommunicationDetailView(
                                student = activeStudent,
                                classRoom = classRoom,
                                announcements = parentAnnouncements,
                                chatMessages = chatMessages.filter { it.studentId == activeStudent.id },
                                currentUser = currentUser,
                                isDarkTheme = isDarkTheme,
                                onVotePoll = onVotePoll,
                                onMarkAnnouncementRead = onMarkAnnouncementRead,
                                onSendMessage = { text ->
                                    if (text.isNotBlank()) {
                                        onSendChatMessage(activeStudent.id, text)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // 3. ALT NAVİGASYON ÇUBUĞU (BOTTOM NAVIGATION) - VELİ ROLÜNE ÖZEL YÜKSEK GÖRÜNÜRLÜKLÜ RENKLİ TASARIM
            // =========================================================================
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                shape = RoundedCornerShape(22.dp),
                color = Color.Transparent,
                shadowElevation = 14.dp,
                border = BorderStroke(1.5.dp, BirizTurquoise.copy(alpha = 0.75f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isDarkTheme) {
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF0B192C), Color(0xFF133354), Color(0xFF0B192C))
                                )
                            } else {
                                Brush.horizontalGradient(
                                    listOf(BirizNavyPrimary, Color(0xFF17466E), BirizNavyPrimary)
                                )
                            }
                        )
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp,
                        windowInsets = WindowInsets(0, 0, 0, 0),
                        modifier = Modifier.height(74.dp)
                    ) {
                        ParentPortalTab.entries.filter { it.showInBottomBar }.forEach { tab ->
                            val isSelected = activeTab == tab
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { activeTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = tab.iconVector,
                                        contentDescription = tab.title,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.title,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                },
                                alwaysShowLabel = true,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = BirizNavyDark,
                                    selectedTextColor = Color(0xFF5EEAD4),
                                    unselectedIconColor = Color.White.copy(alpha = 0.88f),
                                    unselectedTextColor = Color.White.copy(alpha = 0.92f),
                                    indicatorColor = BirizTurquoise
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // DİYALOGLAR (KOD İLE BAĞLAMA, ÇIKARMA, PROFİL MENÜSÜ)
    // =========================================================================
    if (showLinkCodeDialog) {
        AlertDialog(
            onDismissRequest = { showLinkCodeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔑", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Veli Katılım Kodu Girin", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Sınıf öğretmeninizin çocuğunuz için paylaştığı 8 haneli Veli Katılım Kodunu girerek hesabınızı bağlayabilirsiniz.",
                        fontSize = 12.5.sp,
                        color = secondaryTextColor,
                        lineHeight = 17.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = inviteCodeInput,
                        onValueChange = { inviteCodeInput = TurkishTextHelper.normalizeVeliKatilimKodu(it) },
                        placeholder = { Text("Örn: 8K3N9P2X", fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
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
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BirizNavyPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Çocuğumu Bağla", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLinkCodeDialog = false }) { Text("İptal") }
            }
        )
    }

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
                        text = "${s.fullName} (${s.studentNumber}) adlı öğrencinin veli takip hesabınızdaki bağlantısını kaldırmak istediğinize emin misiniz?",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "İstediğiniz zaman öğretmenden alacağınız 8 haneli kod ile tekrar bağlayabilirsiniz.",
                        fontSize = 12.sp,
                        color = secondaryTextColor
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
                TextButton(onClick = { studentToUnlink = null }) { Text("Vazgeç") }
            }
        )
    }

    if (isNotificationCenterOpen) {
        NotificationCenterDialog(
            notifications = notifications,
            isDarkTheme = isDarkTheme,
            onDismiss = onCloseNotificationCenter,
            onNotificationClick = { notif ->
                onMarkNotificationAsRead(notif.id)
                if (notif.type == NotificationType.APP_UPDATE) {
                    onOpenUpdateDialog()
                } else {
                    activeTab = ParentPortalTab.COMMUNICATION
                }
            },
            onMarkAllAsRead = onMarkAllNotificationsAsRead
        )
    }

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
            onCheckUpdate = onOpenUpdateDialog,
            onLogout = onLogout,
            onDismissRequest = { showProfileMenuDialog = false }
        )
    }
}

// =========================================================================
// BİLEŞEN: VELİ BOŞ DURUMU (HİÇ ÇOCUK BAĞLANMAMIŞSA)
// =========================================================================
@Composable
private fun ParentEmptyStateView(
    isDarkTheme: Boolean,
    onOpenCodeDialog: () -> Unit
) {
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
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "👨‍👩‍👧‍👦", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "BİRİZ Veli Portalı",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDarkTheme) Color.White else BirizNavyPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Çocuğunuzun günlük ders akışını, yıldızlarını, kitap okumalarını, sınıf oturma yerini ve öğretmen duyurularını anlık takip edebilmek için öğretmeninizin verdiği 8 haneli Veli Katılım Kodunu girin.",
                    fontSize = 12.5.sp,
                    color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onOpenCodeDialog,
                    colors = ButtonDefaults.buttonColors(containerColor = BirizAmber),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("🔑 8 Haneli Veli Kodunu Gir", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

// =========================================================================
// BİLEŞEN: BENTO DASHBOARD ANA SAYFASI ("BUGÜN ÇOCUĞUM")
// =========================================================================
@Composable
private fun ParentDashboardHomeView(
    student: Student,
    classRoom: ClassRoom?,
    totalStars: Int,
    starLevelBadge: Pair<String, Color>,
    lastStarRecord: StarRewardRecord?,
    todayLessons: List<ScheduleLesson>,
    studentSeatDesc: String,
    readingRecords: List<com.example.data.model.ReadingRecord>,
    classBooks: List<Book> = emptyList(),
    activeTournament: Tournament?,
    lastAnnouncement: Announcement?,
    lastChatMessage: ChatMessage?,
    isDarkTheme: Boolean,
    onNavigate: (ParentPortalTab) -> Unit
) {
    val primaryText = if (isDarkTheme) BirizDarkTextPrimary else BirizTextPrimary
    val secondaryText = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary

    val currentlyReadingBook = remember(classBooks, student.id) {
        classBooks.firstOrNull { book ->
            book.activeAssignments.any { it.studentId == student.id } || book.currentStudentId == student.id
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- 1. HERO PROFİL KARTI ---
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isDarkTheme) Color(0xFF0F2038) else Color(0xFFEFF6FF),
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
                        color = if (student.gender.equals("Kız", ignoreCase = true)) Color(0xFFFCE7F3) else Color(0xFFE0F2FE),
                        modifier = Modifier.size(62.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = if (student.gender.equals("Kız", ignoreCase = true)) "👧" else "👦", fontSize = 34.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = BirizTurquoise
                            ) {
                                Text(
                                    text = "NO: ${student.studentNumber}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BirizNavyDark,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = classRoom?.let { "${it.gradeLevel}/${it.section} • Öğretmen: ${it.teacherName}" } ?: "Sınıf",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) Color.LightGray else Color(0xFF334155)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = student.fullName,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isDarkTheme) Color.White else BirizNavyPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = starLevelBadge.second.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, starLevelBadge.second)
                        ) {
                            Text(
                                text = starLevelBadge.first,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = starLevelBadge.second,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = BirizAmber.copy(alpha = 0.15f),
                            border = BorderStroke(1.5.dp, BirizAmber)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = "⭐ $totalStars", fontSize = 22.sp, fontWeight = FontWeight.Black, color = BirizAmber)
                                Text(text = "Yıldız", fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, color = BirizAmber)
                            }
                        }
                    }
                }
            }
        }

        // --- 2. BENTO KART: GÜNCEL YILDIZ & GELİŞİM NOTU ---
        item {
            BentoSummaryCard(
                icon = "⭐",
                title = "Günün Başarı & Yıldız Akışı",
                subtitle = if (lastStarRecord != null) {
                    val timeStr = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date(lastStarRecord.timestamp))
                    "Son Ödül: ${lastStarRecord.note.ifBlank { lastStarRecord.categoryName }} (+${lastStarRecord.starCount} ⭐) • $timeStr"
                } else {
                    "Henüz bugün yıldız kaydı bulunmuyor."
                },
                badgeText = "Detaylar →",
                accentColor = BirizAmber,
                isDarkTheme = isDarkTheme,
                onClick = { onNavigate(ParentPortalTab.STARS) }
            )
        }

        // --- 3. BENTO 2'Lİ GRID: BUGÜNKÜ DERSLER & OTURMA YERİ ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Bugünkü Dersler
                BentoCompactCard(
                    icon = "📅",
                    title = "Bugünkü Dersler",
                    highlight = if (todayLessons.isNotEmpty()) "${todayLessons.size} Ders Planlı" else "Program Yok",
                    description = if (todayLessons.isNotEmpty()) {
                        todayLessons.take(3).joinToString(", ") { "${it.lessonIndex}. ${it.subjectName}" }
                    } else "Bugün için ders girilmemiş",
                    modifier = Modifier.weight(1f),
                    accentColor = Color(0xFF0284C7),
                    isDarkTheme = isDarkTheme,
                    onClick = { onNavigate(ParentPortalTab.SCHEDULE) }
                )

                // Oturma Yeri
                BentoCompactCard(
                    icon = "🪑",
                    title = "Sınıftaki Yeri",
                    highlight = studentSeatDesc,
                    description = "Öğretmen masası & tahta karşısı konumu",
                    modifier = Modifier.weight(1f),
                    accentColor = Color(0xFF10B981),
                    isDarkTheme = isDarkTheme,
                    onClick = { onNavigate(ParentPortalTab.SEATING) }
                )
            }
        }

        // --- 4. BENTO 2'Lİ GRID: KİTAP OKUMA & TURNUVA ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Kitap Okuma (Aktif ödünç alınan kitap varsa öncelikli gösterilir)
                val lastCompleted = readingRecords.lastOrNull()
                val (highlightText, descText) = when {
                    currentlyReadingBook != null -> {
                        val activeAssign = currentlyReadingBook.activeAssignments.firstOrNull { it.studentId == student.id }
                        val days = if (activeAssign != null) {
                            ((System.currentTimeMillis() - activeAssign.assignmentDate) / (1000 * 60 * 60 * 24)).coerceAtLeast(1)
                        } else 1
                        "📖 ${currentlyReadingBook.name}" to "Şu An Okuyor (${days}. gün) • ${currentlyReadingBook.pageCount} Sayfa"
                    }
                    lastCompleted != null -> {
                        lastCompleted.bookName to "Son Tamamlanan • Toplam: ${readingRecords.size} Kitap"
                    }
                    else -> "Kitap Bekleniyor" to "Henüz okuma kaydı girilmedi"
                }

                BentoCompactCard(
                    icon = if (currentlyReadingBook != null) "📖" else "📚",
                    title = if (currentlyReadingBook != null) "Şu An Okuduğu Kitap" else "Kitap & Okuma",
                    highlight = highlightText,
                    description = descText,
                    modifier = Modifier.weight(1f),
                    accentColor = if (currentlyReadingBook != null) Color(0xFFD97706) else Color(0xFF8B5CF6),
                    isDarkTheme = isDarkTheme,
                    onClick = { onNavigate(ParentPortalTab.READING) }
                )

                // Turnuva
                BentoCompactCard(
                    icon = "🏆",
                    title = "Turnuva Durumu",
                    highlight = activeTournament?.name ?: "Aktif Turnuva Yok",
                    description = if (activeTournament != null) {
                        "Format: ${activeTournament.type.displayName} • Katılımcı"
                    } else "Sınıfta turnuva planlanınca görünür",
                    modifier = Modifier.weight(1f),
                    accentColor = BirizGold,
                    isDarkTheme = isDarkTheme,
                    onClick = { onNavigate(ParentPortalTab.TOURNAMENT) }
                )
            }
        }

        // --- 5. BENTO KART: ÖĞRETMENDEN SON DUYURU ---
        item {
            BentoSummaryCard(
                icon = "📢",
                title = "Öğretmenden Son Duyuru",
                subtitle = lastAnnouncement?.let { "${it.title}: ${it.content.take(70)}..." } ?: "Henüz yeni bir duyuru bulunmuyor.",
                badgeText = if (lastAnnouncement?.poll?.options?.isNotEmpty() == true) "Anket Var 🗳️" else "Tüm Duyurular →",
                accentColor = Color(0xFFEC4899),
                isDarkTheme = isDarkTheme,
                onClick = { onNavigate(ParentPortalTab.COMMUNICATION) }
            )
        }

        // --- 6. BENTO KART: ÖĞRETMENLE BİREBİR İLETİŞİM ---
        item {
            BentoSummaryCard(
                icon = "💬",
                title = "Öğretmen İletişimi",
                subtitle = lastChatMessage?.let {
                    val sender = if (it.isFromTeacher) "Öğretmen" else "Siz"
                    "$sender: ${it.text.take(60)}..."
                } ?: "Öğretmeniniz ile buradan birebir özel mesajlaşabilirsiniz.",
                badgeText = "Mesaj Yaz →",
                accentColor = BirizTurquoise,
                isDarkTheme = isDarkTheme,
                onClick = { onNavigate(ParentPortalTab.COMMUNICATION) }
            )
        }
    }
}

// =========================================================================
// YARDIMCI BENTO KARTLARI
// =========================================================================
@Composable
private fun BentoSummaryCard(
    icon: String,
    title: String,
    subtitle: String,
    badgeText: String,
    accentColor: Color,
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (isDarkTheme) BirizDarkCardGlass else Color.White,
        border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = accentColor.copy(alpha = 0.15f),
                modifier = Modifier.size(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = icon, fontSize = 26.sp)
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.5.sp,
                    color = if (isDarkTheme) Color.White else BirizNavyPrimary
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accentColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = badgeText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun BentoCompactCard(
    icon: String,
    title: String,
    highlight: String,
    description: String,
    modifier: Modifier = Modifier,
    accentColor: Color,
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (isDarkTheme) BirizDarkCardGlass else Color.White,
        border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
        modifier = modifier.height(148.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = icon, fontSize = 24.sp)
                Text(
                    text = "Detay →",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor
                )
            }
            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = highlight,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isDarkTheme) Color.White else BirizNavyPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// =========================================================================
// DETAY 1: YILDIZLAR & GELİŞİM (KVKK SIRALAMA İLE)
// =========================================================================
@Composable
private fun ParentStarsDetailView(
    student: Student,
    stars: List<StarRewardRecord>,
    allClassStars: List<StarRewardRecord>,
    allStudents: List<Student>,
    classRoom: ClassRoom?,
    linkedStudentIds: Set<String>,
    isDarkTheme: Boolean
) {
    val primaryText = if (isDarkTheme) BirizDarkTextPrimary else BirizTextPrimary
    val secondaryText = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary

    // Sınıf yıldız sıralaması (Diğer öğrenciler 'Öğrenci' olarak anonimleştirilir)
    val studentStarTotals = remember(allStudents, allClassStars) {
        allStudents.map { s ->
            val sum = allClassStars.filter { it.studentId == s.id }.sumOf { it.starCount }
            Triple(s.id, s.fullName, sum)
        }.sortedByDescending { it.third }
    }
    val studentRank = remember(studentStarTotals, student) {
        val idx = studentStarTotals.indexOfFirst { it.first == student.id }
        if (idx >= 0) idx + 1 else 1
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Özet Kartı
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = BirizAmber.copy(alpha = 0.12f),
                border = BorderStroke(1.5.dp, BirizAmber),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "⭐ Toplam Yıldız", fontSize = 13.5.sp, color = BirizAmber, fontWeight = FontWeight.Bold)
                        Text(text = "${stars.sumOf { it.starCount }} Yıldız", fontSize = 28.sp, fontWeight = FontWeight.Black, color = primaryText)
                        Text(
                            text = "🏆 Sınıfta ${studentRank}. Sırada (İlk %${(studentRank * 100 / maxOf(1, allStudents.size))} dilim)",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryText
                        )
                    }
                    Text(text = "🌟", fontSize = 42.sp)
                }
            }
        }

        // Yıldız Geçmişi Listesi
        item {
            Text(text = "📜 Yıldız Kazanım Geçmişi & Notlar", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = primaryText)
        }

        if (stars.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDarkTheme) BirizDarkCardGlass else Color.White,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    Text(
                        text = "Henüz öğretmen tarafından verilmiş bir yıldız kaydı bulunmuyor.",
                        fontSize = 13.sp,
                        color = secondaryText,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(stars.sortedByDescending { it.timestamp }) { rec ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDarkTheme) BirizDarkCardGlass else Color.White,
                    border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = rec.note.ifBlank { rec.categoryName },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryText
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(rec.timestamp))
                            Text(text = "$dateStr • ${rec.categoryName}", fontSize = 12.5.sp, color = secondaryText)
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = BirizAmber.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "+${rec.starCount} ⭐",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Black,
                                color = BirizAmber,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }

        // Sınıf Sıralama Tablosu
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🏆 Sınıf Başarı Sıralaması", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = primaryText)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Sınıf mevcudundaki puan dağılımı ve çocuğunuzun konumu",
                fontSize = 12.sp,
                color = secondaryText
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        items(studentStarTotals.take(15)) { (sId, sName, count) ->
            val isMyChild = linkedStudentIds.contains(sId)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isMyChild) BirizTurquoise.copy(alpha = 0.2f) else if (isDarkTheme) BirizDarkCardGlass else Color.White,
                border = BorderStroke(
                    if (isMyChild) 1.5.dp else 1.dp,
                    if (isMyChild) BirizTurquoise else if (isDarkTheme) BirizDarkCardBorder else BirizBorder
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isMyChild) "★" else "•",
                            fontSize = 15.sp,
                            color = if (isMyChild) BirizTurquoise else secondaryText,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isMyChild) "$sName (Çocuğunuz)" else "Öğrenci",
                            fontWeight = if (isMyChild) FontWeight.ExtraBold else FontWeight.Medium,
                            fontSize = 13.5.sp,
                            color = if (isMyChild) primaryText else secondaryText
                        )
                    }
                    Text(
                        text = "$count ⭐",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.5.sp,
                        color = if (isMyChild) BirizAmber else secondaryText
                    )
                }
            }
        }
    }
}

// =========================================================================
// DETAY 2: KİTAPLIK & OKUMA KARNESİ
// =========================================================================
@Composable
private fun ParentReadingDetailView(
    student: Student,
    records: List<com.example.data.model.ReadingRecord>,
    classBooks: List<Book>,
    isDarkTheme: Boolean
) {
    val primaryText = if (isDarkTheme) BirizDarkTextPrimary else BirizTextPrimary
    val secondaryText = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary

    val activeReadingBooks = remember(classBooks, student.id) {
        classBooks.filter { book ->
            book.activeAssignments.any { it.studentId == student.id } || book.currentStudentId == student.id
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // İstatistik Kartı
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF8B5CF6).copy(alpha = 0.12f),
                border = BorderStroke(1.dp, Color(0xFF8B5CF6)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("📚 Okuma Karnesi", fontSize = 12.sp, color = Color(0xFF8B5CF6), fontWeight = FontWeight.Bold)
                        Text("${records.size} Kitap Tamamlandı", fontSize = 22.sp, fontWeight = FontWeight.Black, color = primaryText)
                        Text(
                            "Toplam ${records.sumOf { it.pageCount }} sayfa okundu • +${records.sumOf { it.earnedStars }} ⭐ kazanıldı",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = primaryText
                        )
                        if (activeReadingBooks.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "📖 Şu an ${activeReadingBooks.size} kitap ödünçte okunuyor",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD97706)
                            )
                        }
                    }
                    Text("📖", fontSize = 36.sp)
                }
            }
        }

        // --- 1. ŞU AN OKUDUĞU KİTAPLAR (ÖDÜNÇ ALINAN) ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "📖 Şu An Okuduğu Kitaplar",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.5.sp,
                    color = primaryText
                )
                if (activeReadingBooks.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEF3C7),
                        border = BorderStroke(1.dp, Color(0xFFFCD34D))
                    ) {
                        Text(
                            text = "${activeReadingBooks.size} Kitap Ödünçte",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        if (activeReadingBooks.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDarkTheme) BirizDarkCardGlass else Color.White,
                    border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📗", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Şu an ödünç alınmış aktif bir kitap bulunmuyor.",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryText
                            )
                            Text(
                                text = "Öğretmeniniz sınıftan kitap verdiğinde veriliş tarihi ve detayları burada anlık görünecektir.",
                                fontSize = 11.sp,
                                color = secondaryText
                            )
                        }
                    }
                }
            }
        } else {
            items(activeReadingBooks, key = { it.id }) { book ->
                val myAssignment = book.activeAssignments.firstOrNull { it.studentId == student.id }
                val assignDateMs = myAssignment?.assignmentDate ?: book.assignmentDate ?: System.currentTimeMillis()
                val daysAgo = ((System.currentTimeMillis() - assignDateMs) / (1000 * 60 * 60 * 24)).coerceAtLeast(1)
                val targetStars = LibraryPersistenceHelper.calculateReadingStars(book.pageCount)

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDarkTheme) Color(0x3578350F) else Color(0xFFFEF3C7),
                    border = BorderStroke(1.2.dp, Color(0xFFF59E0B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(13.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFD97706)
                                ) {
                                    Text(
                                        text = "#${book.registrationNo}",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = TurkishTextHelper.toTurkishTitleCase(book.name),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = primaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFD97706)
                            ) {
                                Text(
                                    text = "$daysAgo. Gününde",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }

                        if (book.author.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Yazar: ${book.author}",
                                fontSize = 11.5.sp,
                                color = secondaryText
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📄 ${book.pageCount} Sayfa • Veriliş: ${SimpleDateFormat("dd.MM.yyyy", Locale("tr")).format(Date(assignDateMs))}",
                                fontSize = 11.5.sp,
                                color = Color(0xFF92400E),
                                fontWeight = FontWeight.Medium
                            )

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = BirizGold.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, BirizGold)
                            ) {
                                Text(
                                    text = "İade edilince +$targetStars ⭐",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 2. TAMAMLANAN KİTAPLAR & OKUMA GEÇMİŞİ ---
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "📕 Tamamlanan Kitaplar & Okuma Geçmişi", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = primaryText)
        }

        if (records.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDarkTheme) BirizDarkCardGlass else Color.White,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "Henüz tamamlanmış kitap okuma kaydı bulunmuyor.",
                        fontSize = 12.sp,
                        color = secondaryText,
                        modifier = Modifier.padding(14.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(records, key = { it.id }) { rec ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDarkTheme) BirizDarkCardGlass else Color.White,
                    border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = rec.bookName, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = primaryText)
                            Spacer(modifier = Modifier.height(2.dp))
                            val readingDays = maxOf(1, ((rec.endDate - rec.startDate) / (1000 * 60 * 60 * 24)).toInt())
                            val endStr = SimpleDateFormat("dd.MM.yyyy", Locale("tr")).format(Date(rec.endDate))
                            Text(text = "${rec.pageCount} Sayfa • $readingDays günde okundu • Bitiş: $endStr", fontSize = 11.5.sp, color = secondaryText)
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = BirizAmber.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, BirizAmber.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "+${rec.earnedStars} ⭐",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Black,
                                color = BirizAmber,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // --- 3. SINIF KİTAPLIĞI ENVANTERİ (KVKK ANONİM GÖSTERİM) ---
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📚 Sınıf Kitaplığı Mevcut Envanteri",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = primaryText
                )
                Text(
                    text = "${classBooks.size} Eser",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                )
            }
        }

        if (classBooks.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDarkTheme) BirizDarkCardGlass else Color.White,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "Sınıf kütüphanesine henüz kitap eklenmedi.",
                        fontSize = 12.sp,
                        color = secondaryText,
                        modifier = Modifier.padding(14.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(classBooks.sortedBy { it.registrationNo }, key = { it.id }) { b ->
                val isMyChildReading = b.activeAssignments.any { it.studentId == student.id } || b.currentStudentId == student.id
                val isAnotherChildReading = !isMyChildReading && b.activeAssignments.isNotEmpty()
                val isCompletedByMyChild = records.any { it.bookId == b.id || it.bookName.trim().equals(b.name.trim(), ignoreCase = true) }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isMyChildReading) {
                        if (isDarkTheme) Color(0x30D97706) else Color(0xFFFEF3C7)
                    } else if (isDarkTheme) BirizDarkCardGlass else Color.White,
                    border = BorderStroke(
                        if (isMyChildReading) 1.2.dp else 1.dp,
                        if (isMyChildReading) Color(0xFFF59E0B) else (if (isDarkTheme) BirizDarkCardBorder else BirizBorder)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isDarkTheme) Color(0x40334E68) else Color(0xFFE2E8F0)
                            ) {
                                Text(
                                    text = "#${b.registrationNo}",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = TurkishTextHelper.toTurkishTitleCase(b.name),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = primaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${b.pageCount} Sayfa" + if (b.author.isNotBlank()) " • ${b.author}" else "",
                                    fontSize = 11.sp,
                                    color = secondaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Durum etiketi (KVKK kurallarına uygun: Başka bir öğrencinin adı ASLA veliye gösterilmez)
                        val (statusText, statusColor, statusBg) = when {
                            isMyChildReading -> Triple("Çocuğunuzda 📖", Color(0xFF92400E), Color(0xFFFDE68A))
                            isCompletedByMyChild -> Triple("Okundu ✓", Color(0xFF047857), Color(0xFFD1FAE5))
                            isAnotherChildReading -> Triple("Ödünçte", Color(0xFFB45309), Color(0xFFFEF3C7))
                            else -> Triple("Rafta", Color(0xFF065F46), Color(0xFFD1FAE5))
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = statusBg
                        ) {
                            Text(
                                text = statusText,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// DETAY 3A: SADECE HAFTALIK DERS PROGRAMI & ZİL SAATLERİ
// =========================================================================
@Composable
private fun ParentScheduleDetailView(
    scheduleConfig: ScheduleConfig,
    scheduleLessons: List<ScheduleLesson>,
    isDarkTheme: Boolean
) {
    val primaryText = if (isDarkTheme) BirizDarkTextPrimary else BirizTextPrimary
    val secondaryText = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
    val defaultDayCode = remember {
        when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "PZT"
            Calendar.TUESDAY -> "SAL"
            Calendar.WEDNESDAY -> "ÇAR"
            Calendar.THURSDAY -> "PER"
            Calendar.FRIDAY -> "CUM"
            else -> "PZT"
        }
    }
    var selectedDay by remember { mutableStateOf(defaultDayCode) }
    val dayNames = listOf("PZT" to "Pazartesi", "SAL" to "Salı", "ÇAR" to "Çarşamba", "PER" to "Perşembe", "CUM" to "Cuma")
    val dayLessons = remember(scheduleLessons, selectedDay) {
        scheduleLessons.filter {
            it.dayOfWeek == selectedDay ||
                    (selectedDay in listOf("ÇAR", "CAR") && it.dayOfWeek in listOf("ÇAR", "CAR"))
        }.sortedBy { it.lessonIndex }
    }
    val timeline = remember(scheduleConfig, dayLessons) {
        ScheduleHelper.calculateTimeline(scheduleConfig, dayLessons)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("parent_schedule_detail_view"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "📅 Haftalık Ders Programı & Zil Saatleri",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = primaryText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Sınıf öğretmeninin oluşturduğu güncel haftalık ders dağılımı ve ders saatleri",
                fontSize = 12.sp,
                color = secondaryText
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Gün Seçici Çipleri
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                dayNames.forEach { (code, name) ->
                    val isSel = selectedDay == code
                    Surface(
                        onClick = { selectedDay = code },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSel) BirizNavyPrimary else if (isDarkTheme) BirizDarkCardGlass else Color.White,
                        border = BorderStroke(
                            1.5.dp,
                            if (isSel) BirizNavyPrimary else if (isDarkTheme) BirizDarkCardBorder else BirizBorder
                        ),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp)) {
                            Text(
                                text = name,
                                fontSize = 13.sp,
                                fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (isSel) Color.White else primaryText
                            )
                        }
                    }
                }
            }
        }

        if (dayLessons.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDarkTheme) BirizDarkCardGlass else Color.White,
                    border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Bu gün için henüz ders programı girilmemiş.",
                        fontSize = 13.sp,
                        color = secondaryText,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(dayLessons) { lesson ->
                val slot = timeline.filterIsInstance<ScheduleTimelineItem.LessonSlot>()
                    .firstOrNull { it.lessonIndex == lesson.lessonIndex }
                val timeRange = if (slot != null) "${slot.startTime} - ${slot.endTime}" else "${lesson.lessonIndex}. Ders"
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDarkTheme) BirizDarkCardGlass else Color.White,
                    border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = BirizTurquoise.copy(alpha = 0.2f),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${lesson.lessonIndex}",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp,
                                        color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = lesson.subjectName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = primaryText)
                                Text(
                                    text = lesson.teacherName.ifBlank { "Sınıf Öğretmeni" },
                                    fontSize = 12.5.sp,
                                    color = secondaryText
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDarkTheme) Color(0x30334E68) else Color(0xFFF1F5F9)
                        ) {
                            Text(
                                text = timeRange,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryText,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// DETAY 3B: SADECE SINIF OTURMA PLANI KROKİSİ (ÖĞRETMENİN AYARLARI İLE BİREBİR)
// =========================================================================
@Composable
private fun ParentSeatingDetailView(
    student: Student,
    seatingPlanState: SeatingPlanState,
    allStudents: List<Student>,
    linkedStudentIds: Set<String>,
    isDarkTheme: Boolean
) {
    val primaryText = if (isDarkTheme) BirizDarkTextPrimary else BirizTextPrimary
    val secondaryText = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
    val config = seatingPlanState.config

    // Öğretmenin aktif atamalarında config sınırlarını aşan indeks varsa dinamik genişlet
    val maxAssignedGroup = remember(seatingPlanState.assignments) {
        seatingPlanState.assignments.keys.mapNotNull { key ->
            key.split("-").getOrNull(0)?.removePrefix("g")?.toIntOrNull()
        }.maxOrNull() ?: -1
    }
    val maxAssignedSeat = remember(seatingPlanState.assignments) {
        seatingPlanState.assignments.keys.mapNotNull { key ->
            key.split("-").getOrNull(2)?.removePrefix("s")?.toIntOrNull()
        }.maxOrNull() ?: -1
    }

    val effectiveGroupCount = maxOf(config.groupCount, maxAssignedGroup + 1).coerceAtLeast(1)
    val effectivePeoplePerRow = maxOf(config.peoplePerRow, maxAssignedSeat + 1).coerceAtLeast(1)

    val deskTypeLabel = when (effectivePeoplePerRow) {
        1 -> "Tekli Sıra Düzeni"
        2 -> "İkili Sıra Düzeni"
        3 -> "Üçlü Sıra Düzeni"
        else -> "${effectivePeoplePerRow}'lü Sıra Düzeni"
    }

    // Çocuğun tam sıra ve koltuk açıklaması
    val childSeatPositionText = remember(seatingPlanState, student, effectivePeoplePerRow) {
        val entry = seatingPlanState.assignments.entries.firstOrNull { it.value == student.id }
        if (entry != null) {
            val parts = entry.key.split("-")
            val g = (parts.getOrNull(0)?.removePrefix("g")?.toIntOrNull() ?: 0) + 1
            val r = (parts.getOrNull(1)?.removePrefix("r")?.toIntOrNull() ?: 0) + 1
            val s = parts.getOrNull(2)?.removePrefix("s")?.toIntOrNull()
            val seatPos = when {
                effectivePeoplePerRow <= 1 || s == null -> ""
                effectivePeoplePerRow == 2 -> if (s == 0) " • Sol Koltuk" else " • Sağ Koltuk"
                effectivePeoplePerRow == 3 -> when (s) {
                    0 -> " • Sol Koltuk"
                    1 -> " • Orta Koltuk"
                    else -> " • Sağ Koltuk"
                }
                else -> " • ${s + 1}. Koltuk"
            }
            "$g. Grup, $r. Sıra$seatPos"
        } else {
            "Henüz bir sıraya atanmadı"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp)
            .testTag("parent_seating_detail_view"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Üst Bilgi Kartı: Çocuğun Yeri & Öğretmenin Sıra Düzeni Özeti
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF10B981).copy(alpha = 0.14f),
            border = BorderStroke(1.2.dp, Color(0xFF10B981)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🪑 Çocuğunuzun Sınıftaki Yeri",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF059669)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = childSeatPositionText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = primaryText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Sınıf Düzeni: $effectiveGroupCount Grup • $deskTypeLabel",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = secondaryText
                    )
                }
                Text(text = "🎯", fontSize = 32.sp)
            }
        }

        // Renk Açıklama Lejantı
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF10B981))
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text("Çocuğunuz", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = primaryText)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text("Dolu Koltuk (Öğrenci)", fontSize = 11.sp, color = secondaryText)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isDarkTheme) Color(0x20334E68) else Color.White)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text("Boş Koltuk", fontSize = 11.sp, color = secondaryText)
            }
        }

        // Sınıf Ön Cephesi (Akıllı Tahta, Öğretmen Masası, Ön Kapı ve Yan Pencereler)
        ParentClassroomFrontBanner(
            config = config,
            isDarkTheme = isDarkTheme
        )

        // Öğretmenin Sıra ve Grup Kolonları (Yatay Kaydırılabilir Birebir Kroki)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                for (groupIndex in 0 until effectiveGroupCount) {
                    val configuredRows = config.rowsPerGroup.getOrElse(groupIndex) {
                        config.rowsPerGroup.lastOrNull() ?: 5
                    }
                    val maxAssignedRowInGroup = seatingPlanState.assignments.keys.mapNotNull { key ->
                        val parts = key.split("-")
                        val g = parts.getOrNull(0)?.removePrefix("g")?.toIntOrNull()
                        val r = parts.getOrNull(1)?.removePrefix("r")?.toIntOrNull()
                        if (g == groupIndex) r else null
                    }.maxOrNull() ?: -1
                    val rowsInGroup = maxOf(configuredRows, maxAssignedRowInGroup + 1).coerceAtLeast(1)

                    val columnWidth = when (effectivePeoplePerRow) {
                        1 -> 125.dp
                        2 -> 215.dp
                        3 -> 295.dp
                        else -> 360.dp
                    }

                    Column(
                        modifier = Modifier.width(columnWidth),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Grup Başlığı
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDarkTheme) Color(0x4013294B) else Color(0xFFE2E8F0),
                            border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFFCBD5E1)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${groupIndex + 1}. Grup • $rowsInGroup Sıra",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 5.dp)
                            )
                        }

                        // Sıralar (Önden Arkaya)
                        for (rowIndex in 0 until rowsInGroup) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDarkTheme) Color(0xFF13203E) else Color(0xFFF8FAFC)
                                ),
                                border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFFCBD5E1)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(6.dp)) {
                                    Text(
                                        text = if (rowIndex == 0) "1. Sıra (Ön)" else "${rowIndex + 1}. Sıra",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (rowIndex == 0) BirizAmber else secondaryText,
                                        modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        for (seatIndex in 0 until effectivePeoplePerRow) {
                                            val seatId = "g$groupIndex-r$rowIndex-s$seatIndex"
                                            val legacySeatId = "g$groupIndex-r$rowIndex"
                                            val studentIdInSeat = seatingPlanState.assignments[seatId]
                                                ?: if (seatIndex == 0) seatingPlanState.assignments[legacySeatId] else null

                                            val isActiveChild = studentIdInSeat == student.id
                                            val isLinkedSibling = !isActiveChild &&
                                                    studentIdInSeat != null &&
                                                    linkedStudentIds.contains(studentIdInSeat)
                                            val isOccupied = !studentIdInSeat.isNullOrBlank()
                                            val matchedOwnStudent = if (isActiveChild) {
                                                student
                                            } else if (isLinkedSibling) {
                                                allStudents.firstOrNull { it.id == studentIdInSeat }
                                            } else null

                                            val seatBg = when {
                                                isActiveChild -> Color(0xFF10B981)
                                                isLinkedSibling -> BirizTurquoise.copy(alpha = 0.25f)
                                                isOccupied -> if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                                                else -> if (isDarkTheme) Color(0x20334E68) else Color.White
                                            }
                                            val seatBorder = when {
                                                isActiveChild -> Color(0xFF059669)
                                                isLinkedSibling -> BirizTurquoise
                                                isOccupied -> if (isDarkTheme) BirizDarkCardBorder else Color(0xFFCBD5E1)
                                                else -> if (isDarkTheme) Color(0x4064748B) else Color(0xFFE2E8F0)
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = seatBg,
                                                border = BorderStroke(if (isActiveChild) 2.dp else 1.dp, seatBorder),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(54.dp)
                                                    .testTag("parent_seat_$seatId")
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(horizontal = 4.dp, vertical = 4.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    when {
                                                        matchedOwnStudent != null -> {
                                                            Column(
                                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                                verticalArrangement = Arrangement.Center
                                                            ) {
                                                                Text(
                                                                    text = "★ ${matchedOwnStudent.studentNumber}",
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.ExtraBold,
                                                                    color = if (isActiveChild) Color.White else primaryText,
                                                                    maxLines = 1
                                                                )
                                                                Text(
                                                                    text = TurkishTextHelper.toTurkishUpperCase(matchedOwnStudent.fullName),
                                                                    fontSize = 9.5.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = if (isActiveChild) Color.White else primaryText,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis,
                                                                    textAlign = TextAlign.Center
                                                                )
                                                            }
                                                        }
                                                        isOccupied -> {
                                                            Text(
                                                                text = "Öğrenci",
                                                                fontSize = 10.5.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = if (isDarkTheme) Color.LightGray else Color(0xFF475569),
                                                                maxLines = 1
                                                            )
                                                        }
                                                        else -> {
                                                            Text(
                                                                text = "Boş",
                                                                fontSize = 10.sp,
                                                                color = secondaryText,
                                                                maxLines = 1
                                                            )
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
                }
            }
        }

        // Sınıf Arka Cephesi (Arka Kapı ve Arka Pencereler)
        ParentClassroomBackBanner(
            config = config,
            isDarkTheme = isDarkTheme
        )
    }
}

@Composable
private fun ParentClassroomFrontBanner(
    config: SeatingConfig,
    isDarkTheme: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isDarkTheme) Color(0x600E1A38) else Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Sol Duvar Elemanları
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (config.doorPosition == DoorPosition.ON_SOL || config.doorPosition == DoorPosition.SOL_ON) {
                    ParentArchBadge(label = "🚪 Kapı", isDarkTheme = isDarkTheme, isDoor = true)
                }
                if (config.windowPosition == WindowPosition.LEFT) {
                    ParentArchBadge(label = "🪟 Pencere", isDarkTheme = isDarkTheme)
                }
                if (config.deskPosition == DeskPosition.LEFT) {
                    ParentTeacherDeskBadge(isDarkTheme = isDarkTheme)
                }
            }

            // Orta Duvar: Akıllı Tahta & Orta Öğretmen Masası
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFF334155),
                    border = BorderStroke(1.dp, Color(0xFF475569))
                ) {
                    Text(
                        text = "📋 AKILLI TAHTA",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
                if (config.deskPosition == DeskPosition.CENTER) {
                    ParentTeacherDeskBadge(isDarkTheme = isDarkTheme)
                }
            }

            // Sağ Duvar Elemanları
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (config.deskPosition == DeskPosition.RIGHT) {
                    ParentTeacherDeskBadge(isDarkTheme = isDarkTheme)
                }
                if (config.windowPosition == WindowPosition.RIGHT) {
                    ParentArchBadge(label = "🪟 Pencere", isDarkTheme = isDarkTheme)
                }
                if (config.doorPosition == DoorPosition.ON_SAG || config.doorPosition == DoorPosition.SAG_ON) {
                    ParentArchBadge(label = "🚪 Kapı", isDarkTheme = isDarkTheme, isDoor = true)
                }
            }
        }
    }
}

@Composable
private fun ParentClassroomBackBanner(
    config: SeatingConfig,
    isDarkTheme: Boolean
) {
    val hasBackDoorLeft = config.doorPosition == DoorPosition.ARKA_SOL || config.doorPosition == DoorPosition.SOL_ARKA
    val hasBackDoorRight = config.doorPosition == DoorPosition.ARKA_SAG || config.doorPosition == DoorPosition.SAG_ARKA
    val hasBackWindow = config.windowPosition == WindowPosition.BACK

    if (!hasBackDoorLeft && !hasBackDoorRight && !hasBackWindow) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isDarkTheme) Color(0x400E1A38) else Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            if (hasBackDoorLeft) {
                Box(modifier = Modifier.align(Alignment.CenterStart)) {
                    ParentArchBadge(label = "🚪 Kapı (Arka Sol)", isDarkTheme = isDarkTheme, isDoor = true)
                }
            }
            if (hasBackWindow) {
                ParentArchBadge(label = "🪟 Pencereler (Arka Cephe)", isDarkTheme = isDarkTheme)
            }
            if (hasBackDoorRight) {
                Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                    ParentArchBadge(label = "🚪 Kapı (Arka Sağ)", isDarkTheme = isDarkTheme, isDoor = true)
                }
            }
        }
    }
}

@Composable
private fun ParentArchBadge(label: String, isDarkTheme: Boolean, isDoor: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(7.dp),
        color = if (isDoor) {
            if (isDarkTheme) Color(0x40D97706) else Color(0xFFFEF3C7)
        } else {
            if (isDarkTheme) Color(0x400284C7) else Color(0xFFE0F2FE)
        },
        border = BorderStroke(1.dp, if (isDoor) Color(0xFFD97706) else Color(0xFF0284C7))
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDoor) Color(0xFFB45309) else Color(0xFF0284C7),
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ParentTeacherDeskBadge(isDarkTheme: Boolean) {
    Surface(
        shape = RoundedCornerShape(7.dp),
        color = if (isDarkTheme) Color(0x40F59E0B) else Color(0xFFFFFBEB),
        border = BorderStroke(1.dp, Color(0xFFF59E0B))
    ) {
        Text(
            text = "👩‍🏫 Öğretmen Masası",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFD97706),
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
        )
    }
}

// =========================================================================
// DETAY 4: TURNUVALAR (KVKK RAKİP MASKELEME İLE)
// =========================================================================
@Composable
private fun ParentTournamentDetailView(
    student: Student,
    tournament: Tournament?,
    matches: List<TournamentMatch>,
    linkedStudentIds: Set<String>,
    isDarkTheme: Boolean
) {
    val primaryText = if (isDarkTheme) BirizDarkTextPrimary else BirizTextPrimary
    val secondaryText = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary

    if (tournament == null) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text("Sınıfta şu an aktif turnuva bulunmuyor.", fontSize = 13.sp, color = secondaryText)
        }
        return
    }

    // Çocuğun yer aldığı maçlar
    val childMatches = remember(matches, student) {
        matches.filter { it.player1Id == student.id || it.player2Id == student.id }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Turnuva Başlık Kartı
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = BirizGold.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, BirizGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "🏆 Sınıf Turnuvası", fontSize = 11.sp, color = BirizGold, fontWeight = FontWeight.Bold)
                        Text(text = tournament.name, fontSize = 18.sp, fontWeight = FontWeight.Black, color = primaryText)
                        Text(
                            text = "Format: ${tournament.type.displayName} • ${matches.size} Planlı Maç",
                            fontSize = 11.sp,
                            color = secondaryText
                        )
                    }
                    Text(text = "🥇", fontSize = 36.sp)
                }
            }
        }

        item {
            Text(text = "⚔️ Çocuğunuzun Maçları & Karşılaşmaları", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = primaryText)
            Text(text = "Turnuva fikstüründeki karşılaşmalar ve anlık skorlar", fontSize = 11.sp, color = secondaryText)
        }

        if (childMatches.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDarkTheme) BirizDarkCardGlass else Color.White,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Çocuğunuzun bu turnuvada henüz planlanmış maçı bulunmuyor.",
                        fontSize = 12.sp,
                        color = secondaryText,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(childMatches) { m ->
                val maskedM = ParentPrivacyHelper.maskTournamentMatch(m, linkedStudentIds)
                val isCompleted = m.status == "completed"
                val isWon = m.winnerId == student.id

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDarkTheme) BirizDarkCardGlass else Color.White,
                    border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Tur ${m.round} • ${m.matchCode}", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = secondaryText)
                            if (isCompleted) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isWon) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)
                                ) {
                                    Text(
                                        text = if (isWon) "Kazandı ✓" else "Tamamlandı",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isWon) Color(0xFF065F46) else Color(0xFF991B1B),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            } else {
                                Text(text = "Oynanacak", fontSize = 10.sp, color = secondaryText)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${maskedM.player1Name} vs ${maskedM.player2Name}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = primaryText
                            )
                            if (m.score1 != null && m.score2 != null) {
                                Text(text = "${m.score1} - ${m.score2}", fontWeight = FontWeight.Black, fontSize = 14.sp, color = BirizAmber)
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// DETAY 5: DUYURU & ANKET & BİREBİR MESAJLAŞMA
// =========================================================================
@Composable
private fun ParentCommunicationDetailView(
    student: Student,
    classRoom: ClassRoom?,
    announcements: List<Announcement>,
    chatMessages: List<ChatMessage>,
    currentUser: AuthenticatedUser,
    isDarkTheme: Boolean,
    onVotePoll: (String, String) -> Unit,
    onMarkAnnouncementRead: (String) -> Unit = {},
    onSendMessage: (String) -> Unit
) {
    val primaryText = if (isDarkTheme) BirizDarkTextPrimary else BirizTextPrimary
    val secondaryText = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
    var chatText by remember { mutableStateOf("") }

    // Duyuruları görüntülendiğinde okundu olarak işaretle
    val parentDisplayName = currentUser.fullName.ifBlank { "Veli" }
    LaunchedEffect(announcements) {
        announcements.forEach { ann ->
            if (!ann.readReceipts.containsKey(parentDisplayName)) {
                onMarkAnnouncementRead(ann.id)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- 0. ÖĞRETMEN İLETİŞİM KARTI ---
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isDarkTheme) Color(0xFF0F2038) else Color(0xFFEFF6FF),
                border = BorderStroke(1.2.dp, if (isDarkTheme) BirizTurquoise else Color(0xFFBFDBFE)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = BirizNavyPrimary,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("👨‍🏫", fontSize = 24.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = classRoom?.teacherName ?: "Sınıf Öğretmeni",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) Color.White else BirizNavyPrimary
                        )
                        Text(
                            text = "${classRoom?.schoolName ?: "Okul"} • ${classRoom?.gradeLevel ?: 4}/${classRoom?.section ?: "D"} Şubesi",
                            fontSize = 12.sp,
                            color = secondaryText
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("İletişime Açık", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        }
                    }
                }
            }
        }

        // --- 1. DUYURULAR & ANKETLER ---
        item {
            Text(text = "📢 Sınıf Duyuruları & Veli Anketleri", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = primaryText)
        }

        if (announcements.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDarkTheme) BirizDarkCardGlass else Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Henüz yayınlanmış bir sınıf duyurusu bulunmuyor.",
                        fontSize = 13.sp,
                        color = secondaryText,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(announcements) { ann ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDarkTheme) BirizDarkCardGlass else Color.White,
                    border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = ann.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = primaryText)
                            val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(ann.createdAt))
                            Text(text = dateStr, fontSize = 12.sp, color = secondaryText)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = ann.content, fontSize = 13.5.sp, color = secondaryText, lineHeight = 19.sp)

                        // Veli Anketi Varsa
                        val poll = ann.poll
                        if (poll != null && poll.options.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "🗳️ ${poll.question.ifBlank { "Veli Anketi" }} - Tercihinizi Seçin:", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = primaryText)
                            Spacer(modifier = Modifier.height(8.dp))
                            poll.options.forEach { opt ->
                                val isVotedByMe = opt.voterParentNames.contains(parentDisplayName)
                                Surface(
                                    onClick = { onVotePoll(ann.id, opt.id) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isVotedByMe) BirizTurquoise.copy(alpha = 0.25f) else if (isDarkTheme) Color(0x30334E68) else Color(0xFFF1F5F9),
                                    border = BorderStroke(
                                        if (isVotedByMe) 1.5.dp else 1.dp,
                                        if (isVotedByMe) BirizTurquoise else if (isDarkTheme) BirizDarkCardBorder else Color(0xFFCBD5E1)
                                    ),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (isVotedByMe) {
                                                Text("✓ ", color = BirizTurquoise, fontWeight = FontWeight.Black)
                                            }
                                            Text(text = opt.text, fontSize = 13.5.sp, fontWeight = if (isVotedByMe) FontWeight.Bold else FontWeight.Medium, color = primaryText)
                                        }
                                        Text(text = "${opt.voteCount} Oy", fontSize = 12.sp, color = secondaryText, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 2. BİREBİR ÖĞRETMEN SOHBETİ ---
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "💬 Sınıf Öğretmeni Birebir Özel Mesajlaşma", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = primaryText)
            Text(text = "Bu sohbet yalnızca sizin ve sınıf öğretmeninizin arasındadır.", fontSize = 10.5.sp, color = secondaryText)
            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isDarkTheme) BirizDarkCardGlass else Color.White,
                border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Mesajlar Akışı
                    if (chatMessages.isEmpty()) {
                        Text(
                            text = "Henüz mesaj bulunmuyor. Öğretmeninize buradan kolayca mesaj iletebilirsiniz.",
                            fontSize = 11.5.sp,
                            color = secondaryText,
                            modifier = Modifier.padding(vertical = 20.dp),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            chatMessages.takeLast(15).forEach { msg ->
                                val isMe = !msg.isFromTeacher
                                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(msg.timestamp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(
                                            topStart = 12.dp,
                                            topEnd = 12.dp,
                                            bottomStart = if (isMe) 12.dp else 2.dp,
                                            bottomEnd = if (isMe) 2.dp else 12.dp
                                        ),
                                        color = if (isMe) BirizTurquoise else if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                                        modifier = Modifier.fillMaxWidth(0.82f)
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (msg.isFromTeacher) classRoom?.teacherName ?: "Öğretmen" else "Siz",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isMe) BirizNavyDark else secondaryText
                                                )
                                                Text(
                                                    text = timeStr,
                                                    fontSize = 9.5.sp,
                                                    color = if (isMe) BirizNavyDark.copy(alpha = 0.7f) else secondaryText
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = msg.text,
                                                fontSize = 13.sp,
                                                color = if (isMe) BirizNavyDark else primaryText
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Mesaj Yazma Alanı
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = chatText,
                            onValueChange = { chatText = it },
                            placeholder = { Text("Öğretmene mesaj yazın...", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = {
                                if (chatText.isNotBlank()) {
                                    onSendMessage(chatText.trim())
                                    chatText = ""
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BirizTurquoise),
                            modifier = Modifier.height(50.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Gönder", tint = BirizNavyDark, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
