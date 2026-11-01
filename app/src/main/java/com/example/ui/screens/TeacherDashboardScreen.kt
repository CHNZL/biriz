package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import com.example.data.model.Announcement
import com.example.data.model.AnnouncementPoll
import com.example.data.model.AnnouncementTargetType
import com.example.data.model.BirizNotification
import com.example.data.model.ChatMessage
import com.example.ui.components.NotificationCenterDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.ArchivedSeatingPlan
import com.example.data.model.ClassRoom
import com.example.data.model.GroupColorTheme
import com.example.data.model.GroupDistributionConfig
import com.example.data.model.GroupMakerState
import com.example.data.model.NotificationType
import com.example.data.model.ScheduleConfig
import com.example.data.model.ScheduleLesson
import com.example.data.model.SeatingConfig
import com.example.data.model.SeatingPlacementMode
import com.example.data.model.SeatingPlanState
import com.example.data.model.Student
import com.example.ui.components.DockNavItem
import com.example.ui.components.FloatingDockBar
import com.example.ui.components.ThemeToggleIconButton
import com.example.ui.theme.BirizAmber
import com.example.ui.theme.BirizBorder
import com.example.ui.theme.BirizDarkBackground
import com.example.ui.theme.BirizDarkBgBottom
import com.example.ui.theme.BirizDarkBgTop
import com.example.ui.theme.BirizDarkCardBorder
import com.example.ui.theme.BirizDarkCardGlass
import com.example.ui.theme.BirizDarkSurfaceGlass
import com.example.ui.theme.BirizDarkTextMuted
import com.example.ui.theme.BirizDarkTextPrimary
import com.example.ui.theme.BirizDarkTextSecondary
import com.example.ui.theme.BirizError
import com.example.ui.theme.BirizNavyDark
import com.example.ui.theme.BirizNavyLight
import com.example.ui.theme.BirizNavyPrimary
import com.example.ui.theme.BirizTextPrimary
import com.example.ui.theme.BirizTextSecondary
import com.example.ui.theme.BirizTurquoise
import com.example.ui.viewmodel.DashboardSubPage

@Composable
fun TeacherDashboardScreen(
    classRoom: ClassRoom,
    students: List<Student>,
    currentSubPage: DashboardSubPage,
    isAddStudentDialogOpen: Boolean,
    isDeleting: Boolean,
    isDarkTheme: Boolean = false,
    scheduleConfig: ScheduleConfig = ScheduleConfig(),
    scheduleLessons: List<ScheduleLesson> = emptyList(),
    isScheduleSettingsActive: Boolean = false,
    activeScheduleDay: String = "PZT",
    seatingPlanState: SeatingPlanState = SeatingPlanState(),
    isSeatingConfigOpen: Boolean = false,
    groupMakerState: GroupMakerState = GroupMakerState(),
    onToggleTheme: () -> Unit = {},
    onSelectSubPage: (DashboardSubPage) -> Unit,
    onOpenAddStudentDialog: () -> Unit,
    onDismissAddStudentDialog: () -> Unit,
    onAddStudent: (Int, String, String) -> Unit,
    onEditStudent: (String, Int, String, String) -> Unit = { _, _, _, _ -> },
    onImportStudents: (List<Triple<Int, String, String>>) -> Unit = {},
    onUpdateStudentParent: (String, String?, String?, String?, String?) -> Unit = { _, _, _, _, _ -> },
    onSaveScheduleConfig: (ScheduleConfig) -> Unit = {},
    onSaveScheduleLesson: (ScheduleLesson) -> Unit = {},
    onToggleScheduleSettings: (Boolean) -> Unit = {},
    onSelectScheduleDay: (String) -> Unit = {},
    onOpenSeatingConfig: () -> Unit = {},
    onCloseSeatingConfig: () -> Unit = {},
    onSaveSeatingConfig: (SeatingConfig) -> Unit = {},
    onAutoAssignSeating: (SeatingPlacementMode) -> Unit = {},
    onApplyPlannedSeating: (SeatingPlanState) -> Unit = {},
    onApplyArchivedPlan: (ArchivedSeatingPlan) -> Unit = {},
    onAssignStudentToSeat: (String, String?) -> Unit = { _, _ -> },
    onSwapSeats: (String, String) -> Unit = { _, _ -> },
    onTogglePinStudent: (String) -> Unit = {},
    onTogglePriorityStudent: (String) -> Unit = {},
    onClearSeatingPlan: (Boolean) -> Unit = {},
    onOpenGroupWizard: () -> Unit = {},
    onCloseGroupWizard: () -> Unit = {},
    onSaveGroupConfig: (GroupDistributionConfig) -> Unit = {},
    onDistributeGroups: (GroupDistributionConfig?) -> Unit = {},
    onReshuffleGroups: () -> Unit = {},
    onDistributeUnassignedGroups: () -> Unit = {},
    onPickRandomLeadersForAllGroups: () -> Unit = {},
    onPickRandomLeaderForGroup: (String) -> Unit = {},
    onToggleGroupLeader: (String, String) -> Unit = { _, _ -> },
    onRenameGroup: (String, String) -> Unit = { _, _ -> },
    onChangeGroupColor: (String, GroupColorTheme) -> Unit = { _, _ -> },
    onAddNewGroup: () -> Unit = {},
    onDeleteGroup: (String) -> Unit = {},
    onOpenGroupSwapModal: (String) -> Unit = {},
    onCloseGroupSwapModal: () -> Unit = {},
    onSwapGroupStudents: (String, String) -> Unit = { _, _ -> },
    onMoveGroupStudent: (String, String) -> Unit = { _, _ -> },
    onRemoveGroupStudent: (String) -> Unit = {},
    onToggleGroupPresenterMode: () -> Unit = {},
    announcements: List<Announcement> = emptyList(),
    chatMessages: List<ChatMessage> = emptyList(),
    notifications: List<BirizNotification> = emptyList(),
    activeChatStudentId: String? = null,
    isNotificationCenterOpen: Boolean = false,
    onSaveAnnouncement: (Announcement) -> Unit = {},
    onEditAnnouncement: (String, String, String, AnnouncementPoll?, AnnouncementTargetType, List<String>, List<String>) -> Unit = { _, _, _, _, _, _, _ -> },
    onDeleteAnnouncement: (String) -> Unit = {},
    onVotePollOption: (String, String, String) -> Unit = { _, _, _ -> },
    onSendChatMessage: (String, String, Boolean, String, String) -> Unit = { _, _, _, _, _ -> },
    onSimulateParentReply: (String, String) -> Unit = { _, _ -> },
    onMarkChatAsRead: (String, String) -> Unit = { _, _ -> },
    onOpenNotificationCenter: () -> Unit = {},
    onCloseNotificationCenter: () -> Unit = {},
    onMarkNotificationAsRead: (String) -> Unit = {},
    onMarkAllNotificationsAsRead: () -> Unit = {},
    onSetActiveChatStudent: (String?) -> Unit = {},
    onOpenUpdateDialog: () -> Unit = {},
    onDeleteStudent: (Student) -> Unit,
    onDeleteTeacherRoleAndClass: () -> Unit,
    onDeleteEntireAccount: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var studentToDelete by remember { mutableStateOf<Student?>(null) }
    var showDeleteRoleDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    val hasStudents = students.isNotEmpty()

    val copyCodeToClipboard: (String) -> Unit = { code ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("BİRİZ Kodu", code))
        Toast.makeText(context, "Kod kopyalandı: $code", Toast.LENGTH_SHORT).show()
    }

    // 6 Ana Dock Öğesi
    val dockItems = remember {
        listOf(
            DockNavItem("SINIF", "Sınıf", Icons.Default.School, "dock_item_sinif"),
            DockNavItem("DERS", "Ders", Icons.Default.AutoAwesome, "dock_item_ders"),
            DockNavItem("KITAPLIK", "Kitaplık", Icons.Default.MenuBook, "dock_item_kitaplik"),
            DockNavItem("TURNUVA", "Turnuva", Icons.Default.EmojiEvents, "dock_item_turnuva"),
            DockNavItem("OGRETMEN_ODASI", "Öğretmen Odası", Icons.Default.Coffee, "dock_item_ogretmen_odasi"),
            DockNavItem("ILETISIM", "İletişim", Icons.Default.ChatBubble, "dock_item_iletisim")
        )
    }

    val selectedDockId = when (currentSubPage) {
        DashboardSubPage.SINIF_LISTESI,
        DashboardSubPage.DERS_PROGRAMI,
        DashboardSubPage.OTURMA_PLANI -> "SINIF"

        DashboardSubPage.GRUP_OLUSTURMA,
        DashboardSubPage.SANSLI_OGRENCI,
        DashboardSubPage.YILDIZLAR_SINIFI,
        DashboardSubPage.ZAMANLAYICI -> "DERS"

        DashboardSubPage.ILETISIM_DUYURULAR,
        DashboardSubPage.ILETISIM_MESAJLAR -> "ILETISIM"

        DashboardSubPage.KITAPLIK_LISTE,
        DashboardSubPage.KITAPLIK_KAYITLAR,
        DashboardSubPage.KITAPLIK_DEGERLENDIRME -> "KITAPLIK"

        DashboardSubPage.TURNUVA_DEVAM_EDENLER,
        DashboardSubPage.TURNUVA_TAMAMLANANLAR -> "TURNUVA"

        DashboardSubPage.OGRETMEN_ODASI_ZUMRE,
        DashboardSubPage.OGRETMEN_ODASI_VELI,
        DashboardSubPage.OGRETMEN_ODASI_KULUP,
        DashboardSubPage.OGRETMEN_ODASI_GEZI,
        DashboardSubPage.OGRETMEN_ODASI_DILEKCE -> "OGRETMEN_ODASI"

        DashboardSubPage.PROFIL_AYARLARI -> "PROFIL"
    }

    val backgroundBrush = if (isDarkTheme) {
        Brush.verticalGradient(listOf(BirizDarkBgTop, BirizDarkBgBottom))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9)))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
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
                                Image(
                                    painter = painterResource(id = R.drawable.ic_biriz_emblem),
                                    contentDescription = "BİRİZ Amblem",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${classRoom.teacherName} 👋",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.5.sp,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Sağ İkon Butonları: Tema Değiştirici + Bildirim + Profil + Çıkış
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // Açık / Koyu Tema Butonu
                            ThemeToggleIconButton(
                                isDark = isDarkTheme,
                                onToggle = onToggleTheme,
                                size = 34.dp
                            )

                            // Bildirim Rozeti (Duyurular & Veli Mesajları)
                            val unreadNotificationsCount = notifications.count { !it.isRead }
                            Surface(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .clickable { onOpenNotificationCenter() }
                                    .testTag("dashboard_notification_bell"),
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

                            // Profil Ayarları Butonu
                            IconButton(
                                onClick = { onSelectSubPage(DashboardSubPage.PROFIL_AYARLARI) },
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("dashboard_profile_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Profil",
                                    tint = if (currentSubPage == DashboardSubPage.PROFIL_AYARLARI) BirizAmber else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Çıkış Butonu
                            IconButton(
                                onClick = onLogout,
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("dashboard_logout_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = "Çıkış Yap",
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // --- 2. ALT KATEGORİ PİLL ÇUBUĞU (Aktif Kategoriye Göre Hızlı Geçiş) ---
            val activeCategorySubPages = when (selectedDockId) {
                "SINIF" -> listOf(
                    DashboardSubPage.SINIF_LISTESI,
                    DashboardSubPage.DERS_PROGRAMI,
                    DashboardSubPage.OTURMA_PLANI
                )
                "DERS" -> listOf(
                    DashboardSubPage.GRUP_OLUSTURMA,
                    DashboardSubPage.SANSLI_OGRENCI,
                    DashboardSubPage.YILDIZLAR_SINIFI,
                    DashboardSubPage.ZAMANLAYICI
                )
                "ILETISIM" -> listOf(
                    DashboardSubPage.ILETISIM_DUYURULAR,
                    DashboardSubPage.ILETISIM_MESAJLAR
                )
                "KITAPLIK" -> listOf(
                    DashboardSubPage.KITAPLIK_LISTE,
                    DashboardSubPage.KITAPLIK_KAYITLAR,
                    DashboardSubPage.KITAPLIK_DEGERLENDIRME
                )
                "TURNUVA" -> listOf(
                    DashboardSubPage.TURNUVA_DEVAM_EDENLER,
                    DashboardSubPage.TURNUVA_TAMAMLANANLAR
                )
                "OGRETMEN_ODASI" -> listOf(
                    DashboardSubPage.OGRETMEN_ODASI_ZUMRE,
                    DashboardSubPage.OGRETMEN_ODASI_VELI,
                    DashboardSubPage.OGRETMEN_ODASI_KULUP,
                    DashboardSubPage.OGRETMEN_ODASI_GEZI,
                    DashboardSubPage.OGRETMEN_ODASI_DILEKCE
                )
                else -> listOf(DashboardSubPage.PROFIL_AYARLARI)
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isDarkTheme) Color(0x800B162C) else Color(0xCCFFFFFF),
                border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    activeCategorySubPages.forEach { subPage ->
                        val isSelected = currentSubPage == subPage
                        val isLocked = !hasStudents &&
                                subPage != DashboardSubPage.SINIF_LISTESI &&
                                subPage != DashboardSubPage.PROFIL_AYARLARI &&
                                subPage.category != "ÖĞRETMEN ODASI"

                        Surface(
                            onClick = { onSelectSubPage(subPage) },
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                isSelected -> if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                                isLocked -> if (isDarkTheme) Color(0x35334E68) else Color(0xFFE2E8F0)
                                else -> if (isDarkTheme) Color(0x40172649) else Color(0xFFF1F5F9)
                            },
                            border = BorderStroke(
                                1.dp,
                                when {
                                    isSelected -> if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                                    isLocked -> if (isDarkTheme) Color(0x3564748B) else Color(0xFFCBD5E1)
                                    else -> if (isDarkTheme) Color(0x5064748B) else BirizBorder
                                }
                            ),
                            modifier = Modifier.testTag("menu_tab_${subPage.name.lowercase()}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                if (isLocked) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Kilitli",
                                        tint = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF475569),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = subPage.title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = when {
                                        isSelected -> if (isDarkTheme) BirizNavyDark else Color.White
                                        isLocked -> if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF475569)
                                        else -> if (isDarkTheme) Color.White else BirizNavyPrimary
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // --- 3. ANA İÇERİK ALANI (Modüler Sayfalar) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (currentSubPage) {
                    DashboardSubPage.SINIF_LISTESI -> {
                        ClassListSubScreen(
                            classRoom = classRoom,
                            students = students,
                            isDarkTheme = isDarkTheme,
                            onAddStudent = onAddStudent,
                            onEditStudent = onEditStudent,
                            onDeleteStudent = { studentToDelete = it },
                            onImportStudents = onImportStudents,
                            onCopyParentCode = copyCodeToClipboard,
                            onUpdateParentInfo = onUpdateStudentParent,
                            onTogglePriority = onTogglePriorityStudent
                        )
                    }

                    DashboardSubPage.DERS_PROGRAMI -> {
                        ScheduleSubScreen(
                            classRoom = classRoom,
                            scheduleConfig = scheduleConfig,
                            scheduleLessons = scheduleLessons,
                            isSettingsActive = isScheduleSettingsActive,
                            activeDay = activeScheduleDay,
                            isDarkTheme = isDarkTheme,
                            onSaveConfig = onSaveScheduleConfig,
                            onSaveLesson = onSaveScheduleLesson,
                            onToggleSettingsView = onToggleScheduleSettings,
                            onSelectDay = onSelectScheduleDay
                        )
                    }

                    DashboardSubPage.OTURMA_PLANI -> {
                        SeatingPlanSubScreen(
                            classRoom = classRoom,
                            students = students,
                            seatingPlanState = seatingPlanState,
                            isConfigDialogOpen = isSeatingConfigOpen,
                            isDarkTheme = isDarkTheme,
                            onOpenConfigDialog = onOpenSeatingConfig,
                            onCloseConfigDialog = onCloseSeatingConfig,
                            onSaveConfig = onSaveSeatingConfig,
                            onAutoAssign = onAutoAssignSeating,
                            onApplyPlannedSeating = onApplyPlannedSeating,
                            onApplyArchivedPlan = onApplyArchivedPlan,
                            onAssignStudentToSeat = onAssignStudentToSeat,
                            onSwapSeats = onSwapSeats,
                            onTogglePin = onTogglePinStudent,
                            onTogglePriority = onTogglePriorityStudent,
                            onClearPlan = onClearSeatingPlan,
                            onBackToClassList = { onSelectSubPage(DashboardSubPage.SINIF_LISTESI) }
                        )
                    }

                    DashboardSubPage.GRUP_OLUSTURMA -> {
                        GroupMakerSubScreen(
                            classRoom = classRoom,
                            students = students,
                            groupMakerState = groupMakerState,
                            isDarkTheme = isDarkTheme,
                            onOpenWizard = onOpenGroupWizard,
                            onCloseWizard = onCloseGroupWizard,
                            onSaveConfig = onSaveGroupConfig,
                            onDistribute = onDistributeGroups,
                            onReshuffle = onReshuffleGroups,
                            onDistributeUnassigned = onDistributeUnassignedGroups,
                            onPickRandomLeadersForAll = onPickRandomLeadersForAllGroups,
                            onPickRandomLeaderForGroup = onPickRandomLeaderForGroup,
                            onToggleLeader = onToggleGroupLeader,
                            onRenameGroup = onRenameGroup,
                            onChangeGroupColor = onChangeGroupColor,
                            onAddNewGroup = onAddNewGroup,
                            onDeleteGroup = onDeleteGroup,
                            onOpenSwapModal = onOpenGroupSwapModal,
                            onCloseSwapModal = onCloseGroupSwapModal,
                            onSwapStudents = onSwapGroupStudents,
                            onMoveStudent = onMoveGroupStudent,
                            onRemoveStudent = onRemoveGroupStudent,
                            onBackToClassList = { onSelectSubPage(DashboardSubPage.SINIF_LISTESI) }
                        )
                    }

                    DashboardSubPage.SANSLI_OGRENCI -> {
                        LuckyStudentSubScreen(
                            classRoom = classRoom,
                            students = students,
                            isDarkTheme = isDarkTheme,
                            onBackToClassList = { onSelectSubPage(DashboardSubPage.SINIF_LISTESI) }
                        )
                    }

                    DashboardSubPage.YILDIZLAR_SINIFI -> {
                        StarsClassroomSubScreen(
                            classRoom = classRoom,
                            students = students,
                            isDarkTheme = isDarkTheme,
                            onBackToClassList = { onSelectSubPage(DashboardSubPage.SINIF_LISTESI) }
                        )
                    }

                    DashboardSubPage.ILETISIM_DUYURULAR,
                    DashboardSubPage.ILETISIM_MESAJLAR -> {
                        CommunicationSubScreen(
                            classRoom = classRoom,
                            students = students,
                            announcements = announcements,
                            chatMessages = chatMessages,
                            activeSubPageName = currentSubPage.name,
                            initialActiveStudentId = activeChatStudentId,
                            isDarkTheme = isDarkTheme,
                            onSaveAnnouncement = onSaveAnnouncement,
                            onEditAnnouncement = onEditAnnouncement,
                            onDeleteAnnouncement = onDeleteAnnouncement,
                            onVotePollOption = onVotePollOption,
                            onSendMessage = onSendChatMessage,
                            onSimulateParentReply = onSimulateParentReply,
                            onMarkChatAsRead = onMarkChatAsRead
                        )
                    }

                    DashboardSubPage.OGRETMEN_ODASI_ZUMRE,
                    DashboardSubPage.OGRETMEN_ODASI_VELI,
                    DashboardSubPage.OGRETMEN_ODASI_KULUP,
                    DashboardSubPage.OGRETMEN_ODASI_GEZI,
                    DashboardSubPage.OGRETMEN_ODASI_DILEKCE -> {
                        TeacherRoomSubScreen(
                            classRoom = classRoom,
                            students = students,
                            currentSubPage = currentSubPage,
                            isDarkTheme = isDarkTheme,
                            onSelectSubPage = onSelectSubPage,
                            onBackToClassList = { onSelectSubPage(DashboardSubPage.SINIF_LISTESI) }
                        )
                    }

                    DashboardSubPage.PROFIL_AYARLARI -> {
                        ProfileSettingsSubScreen(
                            classRoom = classRoom,
                            studentCount = students.size,
                            isDarkTheme = isDarkTheme,
                            onDeleteTeacherRoleAndClass = { showDeleteRoleDialog = true },
                            onDeleteEntireAccount = { showDeleteAccountDialog = true },
                            onBackToClassList = { onSelectSubPage(DashboardSubPage.SINIF_LISTESI) },
                            onCopyCode = copyCodeToClipboard
                        )
                    }

                    else -> {
                        // Diğer Alt Sayfalar (Zengin Bento Mikro-Önizleme)
                        ModulePreviewsSubScreen(
                            subPage = currentSubPage,
                            isDarkTheme = isDarkTheme,
                            onBackToClassList = { onSelectSubPage(DashboardSubPage.SINIF_LISTESI) }
                        )
                    }
                }

                // Havada Süzülen Gezinti Çubuğu (Floating Navigation Dock)
                FloatingDockBar(
                    items = dockItems,
                    selectedId = selectedDockId,
                    onItemSelected = { dockId ->
                        when (dockId) {
                            "SINIF" -> onSelectSubPage(DashboardSubPage.SINIF_LISTESI)
                            "DERS" -> onSelectSubPage(DashboardSubPage.GRUP_OLUSTURMA)
                            "ILETISIM" -> onSelectSubPage(DashboardSubPage.ILETISIM_DUYURULAR)
                            "KITAPLIK" -> onSelectSubPage(DashboardSubPage.KITAPLIK_LISTE)
                            "TURNUVA" -> onSelectSubPage(DashboardSubPage.TURNUVA_DEVAM_EDENLER)
                            "OGRETMEN_ODASI" -> onSelectSubPage(DashboardSubPage.OGRETMEN_ODASI_ZUMRE)
                            "PROFIL" -> onSelectSubPage(DashboardSubPage.PROFIL_AYARLARI)
                        }
                    },
                    isDark = isDarkTheme,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        // --- DİYALOGLAR ---

        // 0. Bildirim Merkezi Diyaloğu
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
                        val targetPage = try {
                            DashboardSubPage.valueOf(notif.targetSubPageName)
                        } catch (_: Exception) {
                            DashboardSubPage.ILETISIM_DUYURULAR
                        }
                        if (notif.targetId != null && targetPage == DashboardSubPage.ILETISIM_MESAJLAR) {
                            onSetActiveChatStudent(notif.targetId)
                        }
                        onSelectSubPage(targetPage)
                    }
                },
                onMarkAllAsRead = onMarkAllNotificationsAsRead
            )
        }

        // 1. Yeni Öğrenci Ekleme Diyaloğu
        if (isAddStudentDialogOpen) {
            AddStudentDialog(
                gradeLevel = classRoom.gradeLevel,
                section = classRoom.section,
                isDarkTheme = isDarkTheme,
                onDismiss = onDismissAddStudentDialog,
                onConfirm = { no, name, gender ->
                    onAddStudent(no, name, gender)
                }
            )
        }

        // 2. Öğrenci Silme Onay Diyaloğu
        studentToDelete?.let { student ->
            AlertDialog(
                onDismissRequest = { studentToDelete = null },
                containerColor = if (isDarkTheme) BirizDarkSurfaceGlass else Color.White,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = BirizError,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = "Öğrenciyi Sil",
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                    )
                },
                text = {
                    Text(
                        text = "${student.studentNumber} numaralı '${student.fullName}' adlı öğrenciyi sınıftan ve veritabanından kalıcı olarak silmek istediğinize emin misiniz?",
                        fontSize = 14.sp,
                        color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteStudent(student)
                            studentToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BirizError,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("confirm_delete_student_button")
                    ) {
                        Text("Evet, Sil", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { studentToDelete = null }) {
                        Text(
                            "Vazgeç",
                            color = if (isDarkTheme) Color.White else BirizNavyPrimary
                        )
                    }
                }
            )
        }

        // 3. Öğretmen Rolü ve Sınıfı Silme Diyaloğu
        if (showDeleteRoleDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteRoleDialog = false },
                containerColor = if (isDarkTheme) BirizDarkSurfaceGlass else Color.White,
                icon = {
                    Icon(
                        imageVector = Icons.Default.PersonRemove,
                        contentDescription = null,
                        tint = BirizAmber,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = "Öğretmen Rolünü ve Sınıfı Sil",
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                    )
                },
                text = {
                    Text(
                        text = "Öğretmen rolünüz, '${classRoom.schoolName}' ${classRoom.gradeLevel}/${classRoom.section} sınıfınız ve sınıfa eklediğiniz TÜM ÖĞRENCİLER (${students.size} öğrenci) veritabanından kalıcı olarak silinecektir.\n\nBoşalan ${classRoom.gradeLevel}/${classRoom.section} şubesi diğer öğretmenler için tekrar seçilebilir hale gelecektir. Rolünüz 'Üye' statüsüne dönecektir.",
                        fontSize = 13.sp,
                        color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteRoleDialog = false
                            onDeleteTeacherRoleAndClass()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BirizAmber,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("confirm_delete_role_button")
                    ) {
                        Text("Rolü ve Sınıfı Sil", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showDeleteRoleDialog = false }) {
                        Text(
                            "Vazgeç",
                            color = if (isDarkTheme) Color.White else BirizNavyPrimary
                        )
                    }
                }
            )
        }

        // 4. Hesabı Tamamen Silme Diyaloğu
        if (showDeleteAccountDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteAccountDialog = false },
                containerColor = if (isDarkTheme) BirizDarkSurfaceGlass else Color.White,
                icon = {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = BirizError,
                        modifier = Modifier.size(40.dp)
                    )
                },
                title = {
                    Text(
                        text = "Hesabımı Tamamen Sil",
                        fontWeight = FontWeight.Bold,
                        color = BirizError
                    )
                },
                text = {
                    Text(
                        text = "DİKKAT: Hesabınız, kimlik bilgileriniz, sınıfınız, öğrenci listeleriniz ve tüm KVKK onay kayıtlarınız kalıcı olarak veritabanından silinecektir!\n\nBu işlem kesinlikle geri alınamaz. Devam etmek istiyor musunuz?",
                        fontSize = 13.sp,
                        color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteAccountDialog = false
                            onDeleteEntireAccount()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BirizError,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("confirm_delete_account_button")
                    ) {
                        Text("Hesabımı Kalıcı Olarak Sil", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showDeleteAccountDialog = false }) {
                        Text(
                            "Vazgeç",
                            color = if (isDarkTheme) Color.White else BirizNavyPrimary
                        )
                    }
                }
            )
        }

        // Silme yükleme göstergesi
        if (isDeleting) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDarkTheme) BirizDarkSurfaceGlass else Color.White
                        ),
                        border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(26.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = BirizTurquoise)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Veriler temizleniyor...",
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
