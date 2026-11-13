package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Announcement
import com.example.data.model.AnnouncementTargetType
import com.example.data.model.BirizNotification
import com.example.data.model.Book
import com.example.data.model.ChatMessage
import com.example.data.model.ClassRoom
import com.example.data.model.DeskPosition
import com.example.data.model.DoorPosition
import com.example.data.model.MessageStatus
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
import com.example.util.AnnouncementIdentityHelper
import com.example.util.ChatVisibilityHelper
import com.example.util.CommunicationPersistenceHelper
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
    onMarkAnnouncementRead: (String) -> Unit = {},
    requestedTab: ParentPortalTab? = null,
    onRequestedTabConsumed: () -> Unit = {},
    onCommunicationTabStateChanged: (isCommTabActive: Boolean, isChatSubTabActive: Boolean) -> Unit = { _, _ -> },
    onMarkChatAsRead: (String) -> Unit = {},
    onDeleteChatMessagesForMe: (Set<String>) -> Unit = {},
    onDeleteChatMessagesForEveryone: (Set<String>) -> Unit = {},
    onClearChatHistoryForMe: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(ParentPortalTab.HOME) }
    var isCommChatSubTabActive by remember { mutableStateOf(true) }
    var showLinkCodeDialog by remember { mutableStateOf(false) }
    var showProfileMenuDialog by remember { mutableStateOf(false) }
    var studentToUnlink by remember { mutableStateOf<Student?>(null) }
    var inviteCodeInput by remember { mutableStateOf("") }
    var chatInputText by remember { mutableStateOf("") }

    // Dışarıdan (ör. bildirime dokunularak) belirli bir sekmeye geçiş istendiyse uygula
    LaunchedEffect(requestedTab) {
        if (requestedTab != null) {
            activeTab = requestedTab
            if (requestedTab == ParentPortalTab.COMMUNICATION) {
                isCommChatSubTabActive = true
            }
            onRequestedTabConsumed()
        }
    }

    // Aktif iletişim/sohbet durumu ViewModel'e canlı aktarılır (sohbet açıkken bildirim üretilmemesi için)
    LaunchedEffect(activeTab, isCommChatSubTabActive) {
        onCommunicationTabStateChanged(
            activeTab == ParentPortalTab.COMMUNICATION,
            isCommChatSubTabActive
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            onCommunicationTabStateChanged(false, false)
        }
    }

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
            classBooks = emptyList()
            rawReadingRecords = emptyList()
            classTournaments = emptyList()
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
                            val studentChatMessages = remember(chatMessages, activeStudent, currentUser.email) {
                                chatMessages.filter { msg ->
                                    msg.studentId == activeStudent.id && (
                                        activeStudent.allLinkedParents.size <= 1 ||
                                            msg.parentEmail.isBlank() ||
                                            currentUser.email.isBlank() ||
                                            currentUser.email.equals("veli@biriz.app", ignoreCase = true) ||
                                            msg.parentEmail.trim().equals(currentUser.email.trim(), ignoreCase = true)
                                    )
                                }
                            }
                            ParentCommunicationDetailView(
                                student = activeStudent,
                                linkedStudents = linkedStudents,
                                classRoom = classRoom,
                                announcements = parentAnnouncements,
                                chatMessages = studentChatMessages,
                                currentUser = currentUser,
                                isDarkTheme = isDarkTheme,
                                isChatSubTabActive = isCommChatSubTabActive,
                                onSelectChatSubTab = { isChat -> isCommChatSubTabActive = isChat },
                                onVotePoll = onVotePoll,
                                onMarkAnnouncementRead = onMarkAnnouncementRead,
                                onMarkChatAsRead = { onMarkChatAsRead(activeStudent.id) },
                                onSendMessage = { text ->
                                    if (text.isNotBlank()) {
                                        onSendChatMessage(activeStudent.id, text)
                                    }
                                },
                                onDeleteMessagesForMe = onDeleteChatMessagesForMe,
                                onDeleteMessagesForEveryone = onDeleteChatMessagesForEveryone,
                                onClearChatHistoryForMe = { onClearChatHistoryForMe(activeStudent.id) }
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // 3. ALT NAVİGASYON ÇUBUĞU (Klavye açıkken sohbet alanını daraltmamak için gizlenir)
            // =========================================================================
            val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
            val activeStudentUnreadChatCount = remember(chatMessages, activeStudent, currentUser.email) {
                if (activeStudent == null) 0
                else chatMessages.count { msg ->
                    msg.studentId == activeStudent.id &&
                        msg.isFromTeacher &&
                        msg.status != MessageStatus.READ && (
                            activeStudent.allLinkedParents.size <= 1 ||
                                msg.parentEmail.isBlank() ||
                                currentUser.email.isBlank() ||
                                currentUser.email.equals("veli@biriz.app", ignoreCase = true) ||
                                msg.parentEmail.trim().equals(currentUser.email.trim(), ignoreCase = true)
                        )
                }
            }
            if (!isImeVisible) {
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
                                        if (tab == ParentPortalTab.COMMUNICATION && activeStudentUnreadChatCount > 0) {
                                            BadgedBox(
                                                badge = {
                                                    Badge(
                                                        containerColor = BirizAmber,
                                                        contentColor = Color.White
                                                    ) {
                                                        Text(
                                                            text = if (activeStudentUnreadChatCount > 9) "9+" else "$activeStudentUnreadChatCount",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Black
                                                        )
                                                    }
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = tab.iconVector,
                                                    contentDescription = tab.title,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        } else {
                                            Icon(
                                                imageVector = tab.iconVector,
                                                contentDescription = tab.title,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = tab.title,
                                            fontSize = 10.5.sp,
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

    studentToUnlink?.let { s ->
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
                val targetStudentId = notif.targetId
                if (!targetStudentId.isNullOrBlank() && linkedStudents.any { it.id == targetStudentId }) {
                    onSelectActiveStudent(targetStudentId)
                }
                when (notif.type) {
                    NotificationType.APP_UPDATE -> {
                        onOpenUpdateDialog()
                    }
                    NotificationType.STAR_REWARD -> {
                        activeTab = ParentPortalTab.STARS
                    }
                    NotificationType.LIBRARY -> {
                        activeTab = ParentPortalTab.READING
                    }
                    NotificationType.TOURNAMENT -> {
                        activeTab = ParentPortalTab.TOURNAMENT
                    }
                    NotificationType.ANNOUNCEMENT -> {
                        isCommChatSubTabActive = false
                        activeTab = ParentPortalTab.COMMUNICATION
                    }
                    NotificationType.MESSAGE -> {
                        isCommChatSubTabActive = true
                        activeTab = ParentPortalTab.COMMUNICATION
                    }
                    NotificationType.PARENT_REGISTRATION -> {
                        activeTab = ParentPortalTab.HOME
                    }
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
    val canvasBackground = if (isDarkTheme) Color(0xFF070E1B) else Color(0xFFEAF0F6)
    val currentlyReadingBook = remember(classBooks, student.id) {
        classBooks.firstOrNull { book ->
            book.activeAssignments.any { it.studentId == student.id } || book.currentStudentId == student.id
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(canvasBackground),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        // --- 1. HERO PROFİL KARTI ---
        item {
            val heroShape = RoundedCornerShape(20.dp)
            val heroBrush = if (isDarkTheme) {
                Brush.linearGradient(listOf(Color(0xFF0F2038), lerp(Color(0xFF0F2038), BirizTurquoise, 0.16f)))
            } else {
                Brush.linearGradient(listOf(Color.White, lerp(Color.White, Color(0xFF0284C7), 0.10f)))
            }
            Surface(
                shape = heroShape,
                color = if (isDarkTheme) Color(0xFF0F2038) else Color.White,
                shadowElevation = if (isDarkTheme) 0.dp else 5.dp,
                border = BorderStroke(1.5.dp, if (isDarkTheme) BirizTurquoise.copy(alpha = 0.55f) else Color(0xFF60A5FA)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(heroShape)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(heroBrush)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (student.gender.equals("Kız", ignoreCase = true)) Color(0xFFFCE7F3) else Color(0xFFE0F2FE),
                            modifier = Modifier.size(52.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = if (student.gender.equals("Kız", ignoreCase = true)) "👧" else "👦", fontSize = 28.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = BirizTurquoise
                            ) {
                                Text(
                                    text = "NO: ${student.studentNumber}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BirizNavyDark,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                            Text(
                                text = classRoom?.let { "${it.gradeLevel}/${it.section} • ${it.teacherName}" } ?: "Sınıf",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) Color.LightGray else Color(0xFF334155),
                                maxLines = 2
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = BirizAmber.copy(alpha = 0.16f),
                            border = BorderStroke(1.5.dp, BirizAmber)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = "⭐ $totalStars", fontSize = 20.sp, fontWeight = FontWeight.Black, color = BirizAmber)
                                Text(text = "Yıldız", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = BirizAmber)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = student.fullName,
                        fontSize = 20.sp,
                        lineHeight = 25.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDarkTheme) Color.White else BirizNavyPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("parent_hero_student_name")
                    )
                }
            }
        }

        // --- 2. BENTO KART: GÜNCEL YILDIZ & GELİŞİM NOTU ---
        item {
            BentoSummaryCard(
                icon = "⭐",
                title = "Yıldız & Başarı Akışı",
                subtitle = if (lastStarRecord != null) {
                    val timeStr = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date(lastStarRecord.timestamp))
                    "Son: ${lastStarRecord.note.ifBlank { lastStarRecord.categoryName }} (+${lastStarRecord.starCount} ★) • $timeStr"
                } else {
                    "Toplam $totalStars yıldız • Henüz yeni ödül yok"
                },
                badgeText = "Detaylar →",
                accentColor = Color(0xFFD97706),
                isDarkTheme = isDarkTheme,
                onClick = { onNavigate(ParentPortalTab.STARS) }
            )
        }

        // --- 3. ÖĞRENCİ TAKİP MODÜLLERİ (2x2 BELİRGİN KUTULAR) ---
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ParentSectionHeader(
                    title = "GÜNLÜK TAKİP MODÜLLERİ",
                    isDark = isDarkTheme
                )

                // 1. Satır: Bugünkü Dersler & Sınıftaki Yeri
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BentoCompactCard(
                        icon = "📅",
                        title = "Bugünkü Dersler",
                        subtitle = if (todayLessons.isNotEmpty()) {
                            "${todayLessons.size} Ders • İlk: ${todayLessons.first().subjectName}"
                        } else {
                            "Bugün için ders yok"
                        },
                        modifier = Modifier.weight(1f),
                        accentColor = Color(0xFF0284C7),
                        isDarkTheme = isDarkTheme,
                        onClick = { onNavigate(ParentPortalTab.SCHEDULE) }
                    )

                    BentoCompactCard(
                        icon = "🪑",
                        title = "Sınıftaki Yeri",
                        subtitle = studentSeatDesc,
                        modifier = Modifier.weight(1f),
                        accentColor = Color(0xFF0D9488),
                        isDarkTheme = isDarkTheme,
                        onClick = { onNavigate(ParentPortalTab.SEATING) }
                    )
                }

                // 2. Satır: Kitap & Okuma & Turnuva Durumu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val readingSubtitle = when {
                        currentlyReadingBook != null -> {
                            val activeAssign = currentlyReadingBook.activeAssignments.firstOrNull { it.studentId == student.id }
                            val rawAssignMs = activeAssign?.assignmentDate ?: currentlyReadingBook.assignmentDate ?: System.currentTimeMillis()
                            val validAssignMs = if (rawAssignMs > 0L) rawAssignMs else System.currentTimeMillis()
                            val days = ((System.currentTimeMillis() - validAssignMs) / (1000L * 60L * 60L * 24L)).coerceAtLeast(1L)
                            "📖 ${currentlyReadingBook.name} (${days}. gün)"
                        }
                        readingRecords.isNotEmpty() -> "${readingRecords.size} Kitap Tamamlandı"
                        else -> "Henüz okuma kaydı yok"
                    }

                    BentoCompactCard(
                        icon = if (currentlyReadingBook != null) "📖" else "📚",
                        title = "Kitap & Okuma",
                        subtitle = readingSubtitle,
                        modifier = Modifier.weight(1f),
                        accentColor = if (currentlyReadingBook != null) Color(0xFFEA580C) else Color(0xFF7C3AED),
                        isDarkTheme = isDarkTheme,
                        onClick = { onNavigate(ParentPortalTab.READING) }
                    )

                    BentoCompactCard(
                        icon = "🏆",
                        title = "Turnuvalar",
                        subtitle = activeTournament?.name ?: "Aktif turnuva bulunmuyor",
                        modifier = Modifier.weight(1f),
                        accentColor = Color(0xFFE11D48),
                        isDarkTheme = isDarkTheme,
                        onClick = { onNavigate(ParentPortalTab.TOURNAMENT) }
                    )
                }
            }
        }

        // --- 4. İLETİŞİM & DUYURULAR ---
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ParentSectionHeader(
                    title = "İLETİŞİM & DUYURULAR",
                    isDark = isDarkTheme
                )

                BentoSummaryCard(
                    icon = "📢",
                    title = "Sınıf Duyuruları",
                    subtitle = lastAnnouncement?.let { "${it.title}: ${it.content}" } ?: "Henüz yeni bir duyuru bulunmuyor.",
                    badgeText = if (lastAnnouncement?.poll?.options?.isNotEmpty() == true) "Anket Var 🗳️" else "Duyurular →",
                    accentColor = Color(0xFFDB2777),
                    isDarkTheme = isDarkTheme,
                    onClick = { onNavigate(ParentPortalTab.COMMUNICATION) }
                )

                BentoSummaryCard(
                    icon = "💬",
                    title = "Öğretmen İletişimi",
                    subtitle = lastChatMessage?.let {
                        val sender = if (it.isFromTeacher) "Öğretmen" else "Siz"
                        "$sender: ${it.text}"
                    } ?: "Öğretmeninizle buradan birebir mesajlaşabilirsiniz.",
                    badgeText = "Mesaj Yaz →",
                    accentColor = Color(0xFF059669),
                    isDarkTheme = isDarkTheme,
                    onClick = { onNavigate(ParentPortalTab.COMMUNICATION) }
                )
            }
        }
    }
}

// =========================================================================
// YARDIMCI BENTO KARTLARI
// =========================================================================
@Composable
private fun ParentSectionHeader(
    title: String,
    isDark: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (isDark) BirizTurquoise else BirizNavyPrimary)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.6.sp,
            color = if (isDark) BirizTurquoise else BirizNavyPrimary
        )
    }
}

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
    val shape = RoundedCornerShape(18.dp)
    val cardBrush = if (isDarkTheme) {
        Brush.linearGradient(
            listOf(
                Color(0xFF132240),
                lerp(Color(0xFF132240), accentColor, 0.16f)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color.White,
                lerp(Color.White, accentColor, 0.09f)
            )
        )
    }
    val borderColor = if (isDarkTheme) {
        accentColor.copy(alpha = 0.52f)
    } else {
        accentColor.copy(alpha = 0.38f)
    }

    Surface(
        onClick = onClick,
        shape = shape,
        color = if (isDarkTheme) Color(0xFF132240) else Color.White,
        shadowElevation = if (isDarkTheme) 0.dp else 4.dp,
        border = BorderStroke(1.5.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBrush)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.5.dp)
                    .background(accentColor.copy(alpha = if (isDarkTheme) 0.88f else 0.82f))
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accentColor.copy(alpha = if (isDarkTheme) 0.28f else 0.14f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = icon, fontSize = 21.sp)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = if (isDarkTheme) Color.White else BirizNavyPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDarkTheme) BirizDarkTextSecondary else Color(0xFF475569),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = if (isDarkTheme) 0.28f else 0.14f)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDarkTheme) Color.White else accentColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BentoCompactCard(
    icon: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    accentColor: Color,
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    val tileBrush = if (isDarkTheme) {
        Brush.linearGradient(
            listOf(
                Color(0xFF132240),
                lerp(Color(0xFF132240), accentColor, 0.18f)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color.White,
                lerp(Color.White, accentColor, 0.11f)
            )
        )
    }
    val borderColor = if (isDarkTheme) {
        accentColor.copy(alpha = 0.54f)
    } else {
        accentColor.copy(alpha = 0.40f)
    }

    Surface(
        onClick = onClick,
        shape = shape,
        color = if (isDarkTheme) Color(0xFF132240) else Color.White,
        shadowElevation = if (isDarkTheme) 0.dp else 4.dp,
        border = BorderStroke(1.5.dp, borderColor),
        modifier = modifier.clip(shape)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(tileBrush)
        ) {
            // Her modül kartını görsel olarak ayıran renkli üst şerit
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.5.dp)
                    .background(accentColor.copy(alpha = if (isDarkTheme) 0.90f else 0.85f))
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 13.dp, vertical = 9.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = accentColor.copy(alpha = if (isDarkTheme) 0.28f else 0.14f),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = icon, fontSize = 16.sp)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(7.dp),
                        color = accentColor.copy(alpha = if (isDarkTheme) 0.28f else 0.14f)
                    ) {
                        Text(
                            text = "Detay →",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDarkTheme) Color.White else accentColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(7.dp))

                // 1. Büyük ve dikkat çekici başlık
                Text(
                    text = title,
                    fontSize = 17.5.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isDarkTheme) Color.White else BirizNavyPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                // 2. Tek temiz alt satır
                Text(
                    text = subtitle,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDarkTheme) BirizDarkTextSecondary else Color(0xFF475569),
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
    var selectedSubTab by remember { mutableIntStateOf(0) } // 0 = Yıldız Geçmişi, 1 = Sınıf Sıralaması

    // Sınıftaki TÜM öğrencilerin yıldız sıralaması (Diğer öğrenciler KVKK gereği 'Öğrenci' olarak maskelenir)
    val rankedStudents = remember(allStudents, allClassStars, linkedStudentIds, student.id) {
        ParentPrivacyHelper.buildMaskedClassStarRanking(
            allStudents = allStudents,
            allClassStars = allClassStars,
            linkedStudentIds = linkedStudentIds,
            activeStudentId = student.id
        )
    }
    val studentRank = remember(rankedStudents, student.id) {
        rankedStudents.firstOrNull { it.studentId == student.id }?.rank ?: 1
    }
    val sortedStarHistory = remember(stars) {
        stars.sortedByDescending { it.timestamp }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Üst Özet Kartı & İki Sekmeli Seçici
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = BirizAmber.copy(alpha = 0.12f),
                border = BorderStroke(1.5.dp, BirizAmber),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "⭐ Toplam Yıldız",
                            fontSize = 12.5.sp,
                            color = BirizAmber,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${stars.sumOf { it.starCount }} Yıldız",
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Black,
                            color = primaryText
                        )
                        Text(
                            text = "🏆 Sınıfta $studentRank. / ${maxOf(1, rankedStudents.size)} Öğrenci (İlk %${(studentRank * 100 / maxOf(1, rankedStudents.size))} dilim)",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryText
                        )
                    }
                    Text(text = "🌟", fontSize = 36.sp)
                }
            }

            // İki Sekmeli Geçiş Çubuğu: 1. Yıldız Geçmişi | 2. Sınıf Sıralaması
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isDarkTheme) Color(0x301E293B) else Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val tabs = listOf(
                        "📜 Yıldız Geçmişi (${sortedStarHistory.size})" to "tab_star_history",
                        "🏆 Sınıf Sıralaması (${rankedStudents.size})" to "tab_class_ranking"
                    )
                    tabs.forEachIndexed { index, (label, tag) ->
                        val isSelected = selectedSubTab == index
                        Surface(
                            onClick = { selectedSubTab = index },
                            shape = RoundedCornerShape(10.dp),
                            color = when {
                                isSelected -> if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                                else -> Color.Transparent
                            },
                            shadowElevation = if (isSelected) 2.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag(tag)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = when {
                                        isSelected -> if (isDarkTheme) BirizNavyDark else Color.White
                                        else -> secondaryText
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // Seçili Sekme İçeriği
        if (selectedSubTab == 0) {
            // SEKME 1: YILDIZ KAZANIM GEÇMİŞİ & NOTLAR
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (sortedStarHistory.isEmpty()) {
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
                                text = "Henüz öğretmen tarafından verilmiş bir yıldız kaydı bulunmuyor.",
                                fontSize = 13.sp,
                                color = secondaryText,
                                modifier = Modifier.padding(18.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(sortedStarHistory, key = { it.id }) { rec ->
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
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryText
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(rec.timestamp))
                                    Text(
                                        text = "$dateStr • ${rec.categoryName}",
                                        fontSize = 12.sp,
                                        color = secondaryText
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = BirizAmber.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "+${rec.starCount} ⭐",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = BirizAmber,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // SEKME 2: SINIF BAŞARI SIRALAMASI (Sınıftaki TÜM Öğrenciler - KVKK Korumalı)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDarkTheme) Color(0x300284C7) else Color(0xFFF0F9FF),
                        border = BorderStroke(1.dp, if (isDarkTheme) Color(0x5538BDF8) else Color(0xFFBAE6FD)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "KVKK Gizliliği",
                                tint = if (isDarkTheme) Color(0xFF38BDF8) else Color(0xFF0284C7),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "KVKK gereği sınıftaki tüm ${rankedStudents.size} öğrencinin yıldız sayısı gösterilir; diğer öğrenciler anonim olarak listelenir.",
                                fontSize = 11.5.sp,
                                color = if (isDarkTheme) BirizDarkTextSecondary else Color(0xFF0369A1),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                if (rankedStudents.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDarkTheme) BirizDarkCardGlass else Color.White,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Sınıfta kayıtlı öğrenci bilgisi bulunamadı.",
                                fontSize = 13.sp,
                                color = secondaryText,
                                modifier = Modifier.padding(16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(rankedStudents, key = { it.studentId }) { entry ->
                        val rankLabel = when (entry.rank) {
                            1 -> "🥇 1."
                            2 -> "🥈 2."
                            3 -> "🥉 3."
                            else -> "${entry.rank}."
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                entry.isActiveChild -> BirizTurquoise.copy(alpha = 0.22f)
                                entry.isMyChild -> BirizTurquoise.copy(alpha = 0.12f)
                                isDarkTheme -> BirizDarkCardGlass
                                else -> Color.White
                            },
                            border = BorderStroke(
                                if (entry.isActiveChild) 1.6.dp else 1.dp,
                                when {
                                    entry.isActiveChild -> BirizTurquoise
                                    entry.isMyChild -> BirizTurquoise.copy(alpha = 0.6f)
                                    isDarkTheme -> BirizDarkCardBorder
                                    else -> BirizBorder
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = when {
                                            entry.rank <= 3 -> BirizAmber.copy(alpha = 0.18f)
                                            entry.isMyChild -> BirizTurquoise.copy(alpha = 0.18f)
                                            isDarkTheme -> Color(0x30334E68)
                                            else -> Color(0xFFF1F5F9)
                                        },
                                        modifier = Modifier.widthIn(min = 42.dp)
                                    ) {
                                        Text(
                                            text = rankLabel,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = when {
                                                entry.rank <= 3 -> BirizAmber
                                                entry.isMyChild -> BirizTurquoise
                                                else -> secondaryText
                                            },
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Text(
                                        text = entry.displayName,
                                        fontWeight = if (entry.isMyChild) FontWeight.ExtraBold else FontWeight.Medium,
                                        fontSize = 13.5.sp,
                                        color = if (entry.isMyChild) primaryText else secondaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (entry.isMyChild) BirizAmber.copy(alpha = 0.18f) else Color.Transparent
                                ) {
                                    Text(
                                        text = "${entry.starCount} ⭐",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.5.sp,
                                        color = if (entry.isMyChild) BirizAmber else secondaryText,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
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
            items(activeReadingBooks.distinctBy { it.id }, key = { "active_${it.id}" }) { book ->
                val myAssignment = book.activeAssignments.firstOrNull { it.studentId == student.id }
                val rawAssignMs = myAssignment?.assignmentDate ?: book.assignmentDate ?: System.currentTimeMillis()
                val assignDateMs = if (rawAssignMs > 0L) rawAssignMs else System.currentTimeMillis()
                val daysAgo = ((System.currentTimeMillis() - assignDateMs) / (1000L * 60L * 60L * 24L)).coerceAtLeast(1L)
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
            items(records.distinctBy { it.id }, key = { "record_${it.id}" }) { rec ->
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
                            val safeEnd = if (rec.endDate > 0L) rec.endDate else System.currentTimeMillis()
                            val safeStart = if (rec.startDate in 1L..safeEnd) rec.startDate else safeEnd
                            val readingDays = ((safeEnd - safeStart) / (1000L * 60L * 60L * 24L)).coerceIn(1L, 3650L).toInt()
                            val endStr = SimpleDateFormat("dd.MM.yyyy", Locale("tr")).format(Date(safeEnd))
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
            items(classBooks.distinctBy { it.id }.sortedBy { it.registrationNo }, key = { "inventory_${it.id}" }) { b ->
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
// DETAY 5: DUYURU & ANKET & BİREBİR ANLIK MESAJLAŞMA
// =========================================================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ParentCommunicationDetailView(
    student: Student,
    linkedStudents: List<Student> = emptyList(),
    classRoom: ClassRoom?,
    announcements: List<Announcement>,
    chatMessages: List<ChatMessage>,
    currentUser: AuthenticatedUser,
    isDarkTheme: Boolean,
    isChatSubTabActive: Boolean = true,
    onSelectChatSubTab: (Boolean) -> Unit = {},
    onVotePoll: (String, String) -> Unit,
    onMarkAnnouncementRead: (String) -> Unit = {},
    onMarkChatAsRead: () -> Unit = {},
    onSendMessage: (String) -> Unit,
    onDeleteMessagesForMe: (Set<String>) -> Unit = {},
    onDeleteMessagesForEveryone: (Set<String>) -> Unit = {},
    onClearChatHistoryForMe: () -> Unit = {}
) {
    val context = LocalContext.current
    val primaryText = if (isDarkTheme) BirizDarkTextPrimary else BirizTextPrimary
    val secondaryText = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
    var chatText by remember { mutableStateOf("") }
    val chatListState = rememberLazyListState()

    var isSearchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedMessageIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showDeleteChoiceDialog by remember { mutableStateOf(false) }
    var selectedAnnouncementId by remember { mutableStateOf<String?>(null) }

    val selectedAnnouncement = remember(announcements, selectedAnnouncementId) {
        selectedAnnouncementId?.let { id -> announcements.firstOrNull { it.id == id } }
    }

    val displayedMessages = remember(chatMessages, isSearchOpen, searchQuery) {
        if (!isSearchOpen || searchQuery.isBlank()) {
            chatMessages
        } else {
            val q = searchQuery.trim()
            chatMessages.filter {
                !it.isDeletedForEveryone && it.text.contains(q, ignoreCase = true)
            }
        }
    }

    LaunchedEffect(student.id) {
        selectedMessageIds = emptySet()
        isSelectionMode = false
        isSearchOpen = false
        searchQuery = ""
        selectedAnnouncementId = null
    }

    LaunchedEffect(isChatSubTabActive) {
        if (isChatSubTabActive) {
            selectedAnnouncementId = null
        }
    }

    LaunchedEffect(selectedAnnouncementId, selectedAnnouncement) {
        if (selectedAnnouncementId != null && selectedAnnouncement == null) {
            selectedAnnouncementId = null
        }
    }

    BackHandler(enabled = isChatSubTabActive && (isSelectionMode || isSearchOpen)) {
        if (isSelectionMode) {
            selectedMessageIds = emptySet()
            isSelectionMode = false
        } else if (isSearchOpen) {
            isSearchOpen = false
            searchQuery = ""
        }
    }

    BackHandler(enabled = !isChatSubTabActive && selectedAnnouncement != null) {
        selectedAnnouncementId = null
    }

    val unreadFromTeacherCount = remember(chatMessages) {
        chatMessages.count { it.isFromTeacher && !it.isDeletedForEveryone && it.status != MessageStatus.READ }
    }

    val unreadAnnouncementsCount = remember(announcements, student) {
        announcements.count {
            AnnouncementIdentityHelper.findStudentReadTimestamp(student, it.readReceipts) == null
        }
    }

    val parentDisplayName = currentUser.fullName.ifBlank { "Veli" }

    // Veli duyuru satırına dokunup detayını açtığında yalnızca o duyuruyu okundu işaretle
    LaunchedEffect(selectedAnnouncement?.id, isChatSubTabActive, student.id) {
        val opened = selectedAnnouncement
        if (!isChatSubTabActive && opened != null) {
            if (AnnouncementIdentityHelper.findStudentReadTimestamp(student, opened.readReceipts) == null) {
                onMarkAnnouncementRead(opened.id)
            }
        }
    }

    // Sohbet sekmesi açıkken gelen öğretmen mesajlarını anında okundu işaretle ve en alta kaydır
    LaunchedEffect(student.id, chatMessages.size, isChatSubTabActive, isSearchOpen) {
        if (isChatSubTabActive) {
            onMarkChatAsRead()
            if (!isSearchOpen && displayedMessages.isNotEmpty()) {
                chatListState.animateScrollToItem(displayedMessages.size)
            }
        }
    }

    val quickReplies = listOf(
        "Bilgilendirme için teşekkür ederim 🙏",
        "Tamamdır öğretmenim, ilgileneceğiz 👍",
        "Ödevini kontrol ettik 📚"
    )

    val sendCurrentParentMessage = {
        val trimmed = chatText.trim()
        if (trimmed.isNotEmpty()) {
            onSendMessage(trimmed)
            chatText = ""
        }
    }

    val toggleMessageSelection: (String) -> Unit = { msgId ->
        val next = if (msgId in selectedMessageIds) selectedMessageIds - msgId else selectedMessageIds + msgId
        selectedMessageIds = next
        if (next.isEmpty()) isSelectionMode = false
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // --- 0. ÖĞRETMEN İLETİŞİM ÖZETİ & SEKME SEÇİCİ (veya WhatsApp Seçim Çubuğu) ---
        Surface(
            color = if (isSelectionMode && isChatSubTabActive) {
                if (isDarkTheme) Color(0xFF0F2F3C) else BirizNavyPrimary
            } else {
                if (isDarkTheme) Color(0xFF0C1829) else Color.White
            },
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isSelectionMode && isChatSubTabActive) {
                val selectedMessages = chatMessages.filter { it.id in selectedMessageIds }
                val copyableText = selectedMessages.filter { !it.isDeletedForEveryone && it.text.isNotBlank() }
                    .joinToString("\n") { it.text }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            selectedMessageIds = emptySet()
                            isSelectionMode = false
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Seçimi İptal Et", tint = Color.White)
                    }
                    Text(
                        text = "${selectedMessageIds.size} mesaj seçildi",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    if (copyableText.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("BİRİZ Mesaj", copyableText))
                                Toast.makeText(context, "Mesaj kopyalandı", Toast.LENGTH_SHORT).show()
                                selectedMessageIds = emptySet()
                                isSelectionMode = false
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Kopyala", tint = Color.White)
                        }
                    }
                    IconButton(
                        onClick = {
                            if (selectedMessageIds.isNotEmpty()) {
                                showDeleteChoiceDialog = true
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("btn_parent_delete_selected_messages")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Seçilenleri Sil", tint = Color(0xFFFCA5A5))
                    }
                }
            } else {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = BirizNavyPrimary,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("👨‍🏫", fontSize = 20.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = classRoom?.teacherName?.ifBlank { "Sınıf Öğretmeni" } ?: "Sınıf Öğretmeni",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) Color.White else BirizNavyPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${classRoom?.gradeLevel ?: 4}/${classRoom?.section ?: "D"} • ${student.studentNumber} ${student.fullName}",
                                fontSize = 11.sp,
                                color = secondaryText,
                                maxLines = 2
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.14f)
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
                                Text(
                                    text = "Anlık Hat",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                        if (isChatSubTabActive) {
                            IconButton(
                                onClick = {
                                    isSearchOpen = !isSearchOpen
                                    if (!isSearchOpen) searchQuery = ""
                                },
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Sohbette Ara",
                                    tint = primaryText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Box {
                                IconButton(
                                    onClick = { showOverflowMenu = true },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .testTag("btn_parent_chat_menu")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Sohbet Seçenekleri",
                                        tint = primaryText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = showOverflowMenu,
                                    onDismissRequest = { showOverflowMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Sohbette Ara") },
                                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                        onClick = {
                                            showOverflowMenu = false
                                            isSearchOpen = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Mesajları Seç ve Sil") },
                                        leadingIcon = { Icon(Icons.Default.SelectAll, contentDescription = null) },
                                        enabled = chatMessages.isNotEmpty(),
                                        onClick = {
                                            showOverflowMenu = false
                                            isSelectionMode = true
                                            selectedMessageIds = emptySet()
                                            Toast.makeText(context, "Silmek istediğiniz mesajlara dokunun veya uzun basın", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("Sohbet Geçmişini Temizle", color = Color(0xFFEF4444)) },
                                        leadingIcon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color(0xFFEF4444)) },
                                        enabled = chatMessages.isNotEmpty(),
                                        onClick = {
                                            showOverflowMenu = false
                                            showClearHistoryDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = isChatSubTabActive && isSearchOpen) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Mesajlarda ara...", fontSize = 12.5.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                trailingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (searchQuery.isNotBlank()) {
                                            Text(
                                                text = "${displayedMessages.size}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BirizTurquoise,
                                                modifier = Modifier.padding(end = 4.dp)
                                            )
                                        }
                                        IconButton(onClick = {
                                            isSearchOpen = false
                                            searchQuery = ""
                                        }) {
                                            Icon(Icons.Default.Close, contentDescription = "Aramayı Kapat", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // İletişim Alt Sekmeleri: Birebir Sohbet | Duyurular & Anketler
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = { onSelectChatSubTab(true) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isChatSubTabActive) BirizTurquoise else if (isDarkTheme) Color(0x251E293B) else Color(0xFFF1F5F9),
                            border = BorderStroke(
                                1.dp,
                                if (isChatSubTabActive) BirizTurquoise else if (isDarkTheme) BirizDarkCardBorder else BirizBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 42.dp)
                                .testTag("parent_tab_chat")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "💬 Birebir Sohbet",
                                    fontSize = 12.5.sp,
                                    fontWeight = if (isChatSubTabActive) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = if (isChatSubTabActive) BirizNavyDark else primaryText
                                )
                                if (unreadFromTeacherCount > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(shape = CircleShape, color = BirizAmber) {
                                        Text(
                                            text = "$unreadFromTeacherCount",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Surface(
                            onClick = { onSelectChatSubTab(false) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (!isChatSubTabActive) BirizNavyPrimary else if (isDarkTheme) Color(0x251E293B) else Color(0xFFF1F5F9),
                            border = BorderStroke(
                                1.dp,
                                if (!isChatSubTabActive) BirizNavyPrimary else if (isDarkTheme) BirizDarkCardBorder else BirizBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 42.dp)
                                .testTag("parent_tab_announcements")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📢 Duyurular (${announcements.size})",
                                    fontSize = 12.5.sp,
                                    fontWeight = if (!isChatSubTabActive) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = if (!isChatSubTabActive) Color.White else primaryText
                                )
                                if (unreadAnnouncementsCount > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(shape = CircleShape, color = BirizAmber) {
                                        Text(
                                            text = "$unreadAnnouncementsCount",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isChatSubTabActive) {
            // --- 1. WHATSAPP STİLİ BİREBİR SOHBET PANELİ ---
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(if (isDarkTheme) Color(0xFF091427) else Color(0xFFEFEAE2))
            ) {
                LazyColumn(
                    state = chatListState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDarkTheme) Color(0x401E293B) else Color(0x90FFFBEB)
                            ) {
                                Text(
                                    text = "🔒 Bu sohbet yalnızca sizin (${currentUser.email.ifBlank { "Doğrulanmış Veli" }}) ve sınıf öğretmeniniz arasındadır. Uzun basarak mesaj silebilirsiniz.",
                                    fontSize = 10.sp,
                                    color = if (isDarkTheme) BirizDarkTextMuted else Color(0xFF92400E),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    if (displayedMessages.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isDarkTheme) Color(0x401E293B) else Color.White.copy(alpha = 0.92f),
                                    border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder)
                                ) {
                                    Text(
                                        text = if (isSearchOpen && searchQuery.isNotBlank()) {
                                            "🔍 \"$searchQuery\" ile eşleşen mesaj bulunamadı."
                                        } else {
                                            "💬 Öğretmeniniz ile gösterilecek mesaj bulunmuyor.\nAşağıdan mesaj yazarak anlık sohbeti başlatabilirsiniz."
                                        },
                                        fontSize = 12.5.sp,
                                        color = secondaryText,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 18.sp,
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        items(displayedMessages.indices.toList(), key = { idx -> displayedMessages[idx].id }) { idx ->
                            val msg = displayedMessages[idx]
                            val prevDayKey = if (idx > 0) ChatVisibilityHelper.dayKeyForTimestamp(displayedMessages[idx - 1].timestamp) else null
                            val currDayKey = ChatVisibilityHelper.dayKeyForTimestamp(msg.timestamp)
                            if (idx == 0 || prevDayKey != currDayKey) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isDarkTheme) Color(0xFF182229) else Color(0xFFE1F3FB)
                                    ) {
                                        Text(
                                            text = ChatVisibilityHelper.formatChatDateDivider(msg.timestamp),
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF334155),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            val isMe = !msg.isFromTeacher
                            val isSelected = msg.id in selectedMessageIds
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) BirizTurquoise.copy(alpha = 0.22f) else Color.Transparent)
                                    .combinedClickable(
                                        onClick = {
                                            if (isSelectionMode) {
                                                toggleMessageSelection(msg.id)
                                            }
                                        },
                                        onLongClick = {
                                            isSelectionMode = true
                                            selectedMessageIds = selectedMessageIds + msg.id
                                        }
                                    )
                                    .padding(horizontal = 2.dp, vertical = 1.dp),
                                horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(
                                        topStart = 14.dp,
                                        topEnd = 14.dp,
                                        bottomStart = if (isMe) 14.dp else 2.dp,
                                        bottomEnd = if (isMe) 2.dp else 14.dp
                                    ),
                                    color = if (isMe) {
                                        if (isDarkTheme) Color(0xFF005C4B) else Color(0xFFD9FDD3)
                                    } else {
                                        if (isDarkTheme) Color(0xFF202C33) else Color.White
                                    },
                                    border = if (isSelected) BorderStroke(1.5.dp, BirizTurquoise) else null,
                                    shadowElevation = 1.dp,
                                    modifier = Modifier.widthIn(max = 290.dp)
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)) {
                                        if (!isMe) {
                                            Text(
                                                text = msg.senderName.ifBlank { classRoom?.teacherName ?: "Sınıf Öğretmeni" },
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0284C7)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                        }
                                        Text(
                                            text = if (msg.isDeletedForEveryone) "🚫 Bu mesaj silindi" else msg.text,
                                            fontSize = 13.sp,
                                            fontStyle = if (msg.isDeletedForEveryone) FontStyle.Italic else FontStyle.Normal,
                                            color = if (msg.isDeletedForEveryone) {
                                                if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
                                            } else {
                                                if (isDarkTheme) Color.White else Color(0xFF111B21)
                                            },
                                            lineHeight = 18.sp
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Row(
                                            modifier = Modifier.align(Alignment.End),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = CommunicationPersistenceHelper.formatTime(msg.timestamp),
                                                fontSize = 9.5.sp,
                                                color = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color.Gray
                                            )
                                            if (isMe && !msg.isDeletedForEveryone) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = if (msg.status == MessageStatus.SENT) Icons.Default.Check else Icons.Default.DoneAll,
                                                    contentDescription = when (msg.status) {
                                                        MessageStatus.READ -> "Okundu"
                                                        MessageStatus.DELIVERED -> "İletildi"
                                                        MessageStatus.SENT -> "Gönderildi"
                                                    },
                                                    tint = when (msg.status) {
                                                        MessageStatus.READ -> Color(0xFF53BDEB)
                                                        MessageStatus.DELIVERED -> Color.Gray
                                                        MessageStatus.SENT -> Color.LightGray
                                                    },
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Hızlı Yanıt Şablonları
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickReplies.forEach { reply ->
                        Surface(
                            onClick = { onSendMessage(reply) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDarkTheme) Color(0x401E293B) else Color.White,
                            border = BorderStroke(0.8.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder)
                        ) {
                            Text(
                                text = reply,
                                fontSize = 11.sp,
                                color = primaryText,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // Mesaj Yazma ve Gönderme Alanı (Tam yükseklikli, klavye gönderimi destekli)
                Surface(
                    color = if (isDarkTheme) BirizDarkSurfaceGlass else Color.White,
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = chatText,
                            onValueChange = { chatText = it },
                            placeholder = { Text("Öğretmene mesaj yazın...", fontSize = 13.sp) },
                            textStyle = TextStyle(
                                fontSize = 13.5.sp,
                                lineHeight = 19.sp,
                                color = if (isDarkTheme) Color.White else Color(0xFF111B21)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp)
                                .testTag("input_parent_chat_message"),
                            maxLines = 4,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = { sendCurrentParentMessage() }),
                            shape = RoundedCornerShape(22.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BirizTurquoise,
                                unfocusedBorderColor = if (isDarkTheme) BirizDarkCardBorder else BirizBorder
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val canSend = chatText.isNotBlank()
                        FloatingActionButton(
                            onClick = { if (canSend) sendCurrentParentMessage() },
                            shape = CircleShape,
                            containerColor = if (canSend) Color(0xFF00A884) else Color(0xFF94A3B8),
                            contentColor = Color.White,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("btn_parent_send_chat")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Gönder",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            if (showDeleteChoiceDialog && selectedMessageIds.isNotEmpty()) {
                val selectedMessages = chatMessages.filter { it.id in selectedMessageIds }
                val allSelectedAreMine = selectedMessages.isNotEmpty() && selectedMessages.all { !it.isFromTeacher && !it.isDeletedForEveryone }
                AlertDialog(
                    onDismissRequest = { showDeleteChoiceDialog = false },
                    title = {
                        Text(
                            text = if (selectedMessages.size == 1) "Mesajı Sil" else "${selectedMessages.size} Mesajı Sil",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = if (allSelectedAreMine) {
                                    "Mesajları yalnızca kendi sohbetinizden silebilirsiniz (öğretmeninizdeki mesaj kalır) veya her iki taraftan da kaldırabilirsiniz."
                                } else {
                                    "Seçilen mesajlar yalnızca sizin sohbet geçmişinizden silinecek. Öğretmeninizin sohbet geçmişi silinmez."
                                },
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    val ids = selectedMessageIds
                                    showDeleteChoiceDialog = false
                                    selectedMessageIds = emptySet()
                                    isSelectionMode = false
                                    onDeleteMessagesForMe(ids)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BirizNavyPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Benden Sil (Karşı Taraf Etkilenmez)", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            if (allSelectedAreMine) {
                                Button(
                                    onClick = {
                                        val ids = selectedMessageIds
                                        showDeleteChoiceDialog = false
                                        selectedMessageIds = emptySet()
                                        isSelectionMode = false
                                        onDeleteMessagesForEveryone(ids)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Herkesten Sil", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showDeleteChoiceDialog = false }) { Text("İptal") }
                    }
                )
            }

            if (showClearHistoryDialog) {
                AlertDialog(
                    onDismissRequest = { showClearHistoryDialog = false },
                    title = { Text("Sohbet Geçmişini Temizle", fontWeight = FontWeight.Bold) },
                    text = {
                        Text(
                            text = "Bu sohbetteki tüm mesaj geçmişi yalnızca sizin veli hesabınızdan temizlenecektir. Öğretmeninizin sohbet geçmişi silinmez ve yeni gelecek mesajlar görünmeye devam eder.",
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showClearHistoryDialog = false
                                selectedMessageIds = emptySet()
                                isSelectionMode = false
                                onClearChatHistoryForMe()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Benden Temizle", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearHistoryDialog = false }) { Text("Vazgeç") }
                    }
                )
            }
        } else {
            // --- 2. SINIF DUYURULARI & VELİ ANKETLERİ (Kompakt Satır Listesi + Detay Görünümü) ---
            if (selectedAnnouncement != null) {
                ParentAnnouncementDetailView(
                    announcement = selectedAnnouncement,
                    student = student,
                    linkedStudents = linkedStudents,
                    parentDisplayName = parentDisplayName,
                    isDarkTheme = isDarkTheme,
                    onBack = { selectedAnnouncementId = null },
                    onVotePoll = { optionId -> onVotePoll(selectedAnnouncement.id, optionId) }
                )
            } else {
                ParentAnnouncementsListView(
                    announcements = announcements,
                    student = student,
                    linkedStudents = linkedStudents,
                    parentDisplayName = parentDisplayName,
                    isDarkTheme = isDarkTheme,
                    onAnnouncementClick = { ann -> selectedAnnouncementId = ann.id }
                )
            }
        }
    }
}

@Composable
private fun ParentAnnouncementsListView(
    announcements: List<Announcement>,
    student: Student,
    linkedStudents: List<Student>,
    parentDisplayName: String,
    isDarkTheme: Boolean,
    onAnnouncementClick: (Announcement) -> Unit
) {
    val secondaryText = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
    var selectedFilter by remember { mutableStateOf("Tümü") }
    val filters = listOf("Tümü", "Okunmamış", "Anketli", "Özel")

    val voterAliases = remember(student, linkedStudents, parentDisplayName) {
        AnnouncementIdentityHelper.voterAliasesForParent(
            activeStudent = student,
            linkedStudents = linkedStudents,
            rawParentName = parentDisplayName
        )
    }

    val filteredList = remember(announcements, selectedFilter, student.id) {
        when (selectedFilter) {
            "Okunmamış" -> announcements.filter {
                AnnouncementIdentityHelper.findStudentReadTimestamp(student, it.readReceipts) == null
            }
            "Anketli" -> announcements.filter { it.poll != null && it.poll.options.isNotEmpty() }
            "Özel" -> announcements.filter { it.targetType == AnnouncementTargetType.SPECIFIC_STUDENTS }
            else -> announcements
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Üst Bilgi Kartı
        item {
            BentoCard(
                isDark = isDarkTheme,
                glowColor = BirizAmber,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = BirizAmber.copy(alpha = 0.16f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = null,
                                tint = BirizAmber,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(11.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "📢 Sınıf Duyuruları & Anketler",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                        )
                        Text(
                            text = "İçeriği okumak ve anketlere oy vermek için duyuru satırına dokunun.",
                            fontSize = 11.5.sp,
                            color = secondaryText,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // Filtre Çipleri
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                filters.forEach { filter ->
                    val isSelected = selectedFilter == filter
                    Surface(
                        onClick = { selectedFilter = filter },
                        shape = RoundedCornerShape(10.dp),
                        color = when {
                            isSelected -> if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                            else -> if (isDarkTheme) Color(0x351E293B) else Color(0xFFF1F5F9)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) (if (isDarkTheme) BirizTurquoise else BirizNavyPrimary) else BirizBorder
                        ),
                        modifier = Modifier.heightIn(min = 36.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = filter,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = when {
                                    isSelected -> if (isDarkTheme) BirizNavyDark else Color.White
                                    else -> if (isDarkTheme) Color.White else BirizNavyPrimary
                                }
                            )
                        }
                    }
                }
            }
        }

        // Kompakt Duyuru Başlık Satırları
        if (filteredList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (announcements.isEmpty()) {
                                "Henüz yayınlanmış bir sınıf duyurusu bulunmuyor."
                            } else {
                                "Bu filtreye uygun duyuru bulunmuyor."
                            },
                            fontSize = 13.sp,
                            color = secondaryText,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredList, key = { it.id }) { ann ->
                val isRead = AnnouncementIdentityHelper.findStudentReadTimestamp(student, ann.readReceipts) != null
                val hasVoted = ann.poll?.let { AnnouncementIdentityHelper.hasVotedInPoll(it, voterAliases) } ?: false
                ParentAnnouncementSummaryRowCard(
                    announcement = ann,
                    isRead = isRead,
                    hasVoted = hasVoted,
                    isDarkTheme = isDarkTheme,
                    onClick = { onAnnouncementClick(ann) }
                )
            }
        }
    }
}

@Composable
private fun ParentAnnouncementSummaryRowCard(
    announcement: Announcement,
    isRead: Boolean,
    hasVoted: Boolean,
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    val isAll = announcement.targetType == AnnouncementTargetType.ALL_CLASS
    val hasPoll = announcement.poll != null && announcement.poll.options.isNotEmpty()
    val accentColor = when {
        !isRead -> BirizAmber
        hasPoll -> Color(0xFF10B981)
        isAll -> BirizTurquoise
        else -> BirizAmber
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(15.dp),
        color = if (!isRead) {
            if (isDarkTheme) Color(0x38251C12) else Color(0xFFFFFBEB)
        } else {
            if (isDarkTheme) Color(0x30101F36) else Color.White
        },
        border = BorderStroke(
            if (!isRead) 1.4.dp else 1.dp,
            if (!isRead) BirizAmber.copy(alpha = 0.7f)
            else if (isDarkTheme) BirizDarkCardBorder
            else accentColor.copy(alpha = 0.35f)
        ),
        shadowElevation = if (!isRead) 2.dp else 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("parent_announcement_row_${announcement.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sol Tür İkonu
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accentColor.copy(alpha = if (isDarkTheme) 0.22f else 0.14f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (hasPoll) Icons.Default.Poll else Icons.Default.Campaign,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(11.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Üst satır: Hedef + Anket + Okunma rozeti + Tarih
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (!isRead) {
                            Surface(
                                shape = RoundedCornerShape(5.dp),
                                color = BirizAmber
                            ) {
                                Text(
                                    text = "YENİ",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = if (isAll) {
                                if (isDarkTheme) Color(0x300284C7) else Color(0xFFE0F2FE)
                            } else {
                                if (isDarkTheme) Color(0x30D97706) else Color(0xFFFEF3C7)
                            }
                        ) {
                            Text(
                                text = if (isAll) "Tüm Sınıf" else "Size Özel",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAll) Color(0xFF0284C7) else Color(0xFFD97706),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (hasPoll) {
                            Surface(
                                shape = RoundedCornerShape(5.dp),
                                color = if (isDarkTheme) Color(0x3010B981) else Color(0xFFDCFCE7)
                            ) {
                                Text(
                                    text = if (hasVoted) "Anket ✓" else "Anketli",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF059669),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (announcement.isEdited) {
                            Text(
                                text = "✏️",
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = CommunicationPersistenceHelper.formatDateTime(announcement.createdAt),
                        fontSize = 9.5.sp,
                        color = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Ana Duyuru Başlığı
                Text(
                    text = announcement.title,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Alt özet satırı
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = null,
                            tint = if (isRead) Color(0xFF0284C7) else BirizAmber,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (isRead) "Okundu" else "Okunmadı • Açmak için dokunun",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isRead) {
                                if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                            } else BirizAmber
                        )
                    }

                    announcement.poll?.takeIf { it.options.isNotEmpty() }?.let { poll ->
                        Text(
                            text = "• 🗳️ ${poll.totalVotes} Oy" + if (hasVoted) " (Oylandı)" else "",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF059669)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Duyuru Detayını Aç",
                tint = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun ParentAnnouncementDetailView(
    announcement: Announcement,
    student: Student,
    linkedStudents: List<Student>,
    parentDisplayName: String,
    isDarkTheme: Boolean,
    onBack: () -> Unit,
    onVotePoll: (String) -> Unit
) {
    val primaryText = if (isDarkTheme) BirizDarkTextPrimary else BirizTextPrimary
    val secondaryText = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
    val isAll = announcement.targetType == AnnouncementTargetType.ALL_CLASS
    val readTimestamp = AnnouncementIdentityHelper.findStudentReadTimestamp(student, announcement.readReceipts)

    Column(modifier = Modifier.fillMaxSize()) {
        // Üst Geri Dönüş Başlığı
        Surface(
            color = if (isDarkTheme) Color(0xFF0C1829) else Color.White,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("btn_parent_back_announcement_list")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Duyurulara Dön",
                            tint = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                        )
                    }
                    Column {
                        Text(
                            text = "Duyuru Detayı",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                        )
                        Text(
                            text = "Tüm duyurulara dönmek için geri tuşuna dokunun",
                            fontSize = 10.sp,
                            color = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDarkTheme) Color(0x3510B981) else Color(0xFFDCFCE7),
                    border = BorderStroke(0.8.dp, Color(0xFF10B981).copy(alpha = 0.6f)),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (readTimestamp != null) "Okundu" else "Okunuyor",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669)
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 14.dp)
                .padding(bottom = 96.dp)
        ) {
            BentoCard(
                isDark = isDarkTheme,
                glowColor = if (isAll) BirizTurquoise else BirizAmber,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // 1. Rozetler & Tarih
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isAll) {
                                    if (isDarkTheme) Color(0x350284C7) else Color(0xFFE0F2FE)
                                } else {
                                    if (isDarkTheme) Color(0x35D97706) else Color(0xFFFEF3C7)
                                },
                                border = BorderStroke(
                                    0.8.dp,
                                    if (isAll) Color(0xFF38BDF8) else Color(0xFFF59E0B)
                                )
                            ) {
                                Text(
                                    text = if (isAll) "Tüm Sınıf" else "Size Özel",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAll) Color(0xFF0284C7) else Color(0xFFD97706),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            if (announcement.isEdited) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isDarkTheme) Color(0x30F97316) else Color(0xFFFFEDD5),
                                    border = BorderStroke(0.8.dp, Color(0xFFF97316))
                                ) {
                                    Text(
                                        text = "✏️ Düzenlendi",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFC2410C),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = CommunicationPersistenceHelper.formatDateTime(announcement.createdAt),
                            fontSize = 10.5.sp,
                            color = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. Duyuru Başlığı
                    Text(
                        text = announcement.title,
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3. Duyuru Metni
                    Text(
                        text = announcement.content,
                        fontSize = 13.5.sp,
                        color = secondaryText,
                        lineHeight = 20.sp
                    )

                    // 4. Anket Bölümü (Tek Oy Kısıtlamalı & KVKK Korumalı)
                    val poll = announcement.poll
                    if (poll != null && poll.options.isNotEmpty()) {
                        val voterAliases = remember(student, linkedStudents, parentDisplayName) {
                            AnnouncementIdentityHelper.voterAliasesForParent(
                                activeStudent = student,
                                linkedStudents = linkedStudents,
                                rawParentName = parentDisplayName
                            )
                        }
                        val hasVotedInPoll = AnnouncementIdentityHelper.hasVotedInPoll(poll, voterAliases)

                        Spacer(modifier = Modifier.height(14.dp))

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDarkTheme) Color(0x40132238) else Color(0xFFF0FDF4),
                            border = BorderStroke(1.dp, if (isDarkTheme) Color(0x5010B981) else Color(0xFF86EFAC)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Poll,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "📊 ANKET",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF10B981)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (hasVotedInPoll) {
                                            Color(0xFF10B981).copy(alpha = 0.16f)
                                        } else {
                                            BirizTurquoise.copy(alpha = 0.14f)
                                        }
                                    ) {
                                        Text(
                                            text = if (hasVotedInPoll) "🔒 Oyunuz Kaydedildi" else "Tek Oy Hakkı",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (hasVotedInPoll) Color(0xFF059669) else BirizTurquoise,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = poll.question.ifBlank { "Veli Anketi" },
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryText
                                )

                                if (!hasVotedInPoll) {
                                    Text(
                                        text = "Tercihinizi seçin (oy verdikten sonra cevap değiştirilemez):",
                                        fontSize = 11.5.sp,
                                        color = secondaryText,
                                        modifier = Modifier.padding(top = 3.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                poll.options.forEach { opt ->
                                    val isVotedByMe = AnnouncementIdentityHelper.isOptionVotedByParent(opt, voterAliases)
                                    val percent = if (poll.totalVotes > 0) {
                                        (opt.voteCount * 100) / poll.totalVotes
                                    } else 0

                                    Surface(
                                        onClick = {
                                            if (!hasVotedInPoll) {
                                                onVotePoll(opt.id)
                                            }
                                        },
                                        enabled = !hasVotedInPoll,
                                        shape = RoundedCornerShape(10.dp),
                                        color = when {
                                            isVotedByMe -> if (isDarkTheme) Color(0x350D9488) else Color(0xFFCCFBF1)
                                            hasVotedInPoll -> if (isDarkTheme) Color(0x18334E68) else Color(0xFFF8FAFC)
                                            else -> if (isDarkTheme) Color(0x301E293B) else Color.White
                                        },
                                        border = BorderStroke(
                                            if (isVotedByMe) 1.5.dp else 1.dp,
                                            if (isVotedByMe) BirizTurquoise else if (isDarkTheme) BirizDarkCardBorder else Color(0xFFCBD5E1)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    if (isVotedByMe) {
                                                        Text("✓ ", color = BirizTurquoise, fontWeight = FontWeight.Black)
                                                    }
                                                    Text(
                                                        text = opt.text,
                                                        fontSize = 13.sp,
                                                        fontWeight = if (isVotedByMe) FontWeight.Bold else FontWeight.SemiBold,
                                                        color = if (hasVotedInPoll && !isVotedByMe) secondaryText else primaryText
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "%$percent (${opt.voteCount} Oy)",
                                                    fontSize = 11.5.sp,
                                                    color = if (isVotedByMe) BirizTurquoise else Color(0xFF10B981),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            LinearProgressIndicator(
                                                progress = { if (poll.totalVotes > 0) opt.voteCount.toFloat() / poll.totalVotes else 0f },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(6.dp)
                                                    .clip(RoundedCornerShape(3.dp)),
                                                color = if (isVotedByMe) BirizTurquoise else Color(0xFF10B981),
                                                trackColor = if (isDarkTheme) Color(0x3064748B) else Color(0xFFE2E8F0)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = if (hasVotedInPoll) {
                                        "🔒 Ankete oy kullandınız (Toplam ${poll.totalVotes} oy). Verilen oy değiştirilemez."
                                    } else {
                                        "Toplam ${poll.totalVotes} oy kullanıldı."
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = secondaryText
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
