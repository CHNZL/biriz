package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import com.example.data.model.Announcement
import com.example.data.model.AnnouncementPoll
import com.example.data.model.AnnouncementTargetType
import com.example.data.model.ChatMessage
import com.example.data.model.ClassRoom
import com.example.data.model.LinkedParentAccount
import com.example.data.model.MessageStatus
import com.example.data.model.ParentChatSummary
import com.example.data.model.PollOption
import com.example.data.model.Student
import com.example.ui.components.BentoCard
import com.example.ui.theme.BirizAmber
import com.example.ui.theme.BirizBorder
import com.example.ui.theme.BirizDarkCardBorder
import com.example.ui.theme.BirizDarkSurfaceGlass
import com.example.ui.theme.BirizDarkTextMuted
import com.example.ui.theme.BirizDarkTextPrimary
import com.example.ui.theme.BirizDarkTextSecondary
import com.example.ui.theme.BirizError
import com.example.ui.theme.BirizNavyDark
import com.example.ui.theme.BirizNavyPrimary
import com.example.ui.theme.BirizTextMuted
import com.example.ui.theme.BirizTextPrimary
import com.example.ui.theme.BirizTextSecondary
import com.example.ui.theme.BirizTurquoise
import com.example.util.AnnouncementIdentityHelper
import com.example.util.ChatVisibilityHelper
import com.example.util.CommunicationPersistenceHelper
import java.util.UUID

@Composable
fun CommunicationSubScreen(
    classRoom: ClassRoom,
    students: List<Student>,
    announcements: List<Announcement>,
    chatMessages: List<ChatMessage>,
    activeSubPageName: String, // "ILETISIM_DUYURULAR" veya "ILETISIM_MESAJLAR"
    initialActiveStudentId: String? = null,
    isDarkTheme: Boolean,
    onSaveAnnouncement: (Announcement) -> Unit,
    onEditAnnouncement: (String, String, String, AnnouncementPoll?, AnnouncementTargetType, List<String>, List<String>) -> Unit,
    onDeleteAnnouncement: (String) -> Unit,
    onVotePollOption: (String, String, String) -> Unit,
    onSendMessage: (String, String, Boolean, String, String) -> Unit,
    onSimulateParentReply: (String, String) -> Unit,
    onMarkChatAsRead: (String, String) -> Unit,
    onActiveChatStudentChange: (String?) -> Unit = {},
    onDeleteMessagesForMe: (Set<String>) -> Unit = {},
    onDeleteMessagesForEveryone: (Set<String>) -> Unit = {},
    onClearChatHistoryForMe: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current

    // Punto Boyut Değişimi: SharedPreferences'tan yüklenir (-2, 0, +2, +4, +6)
    var fontOffset by remember {
        mutableIntStateOf(CommunicationPersistenceHelper.loadFontOffset(context))
    }

    // 0 = Duyurular, 1 = Bireysel Mesajlar
    var activeTab by remember(activeSubPageName) {
        mutableIntStateOf(if (activeSubPageName == "ILETISIM_MESAJLAR") 1 else 0)
    }

    // Aktif Sohbet Edilen Öğrenci ve Veli Google Hesabı (null ise sohbet listesi gösterilir)
    var selectedChatTarget by remember {
        val initStudent = students.find { it.id == initialActiveStudentId }
        val initParent = initStudent?.allLinkedParents?.firstOrNull()
        mutableStateOf(if (initStudent != null && initParent != null) Pair(initStudent, initParent) else null)
    }

    LaunchedEffect(initialActiveStudentId, students) {
        if (initialActiveStudentId == null) {
            if (selectedChatTarget != null) {
                selectedChatTarget = null
            }
        } else if (selectedChatTarget?.first?.id != initialActiveStudentId) {
            val s = students.find { it.id == initialActiveStudentId }
            val p = s?.allLinkedParents?.firstOrNull()
            if (s != null && p != null) {
                selectedChatTarget = Pair(s, p)
                onMarkChatAsRead(s.id, p.email)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            onActiveChatStudentChange(null)
        }
    }

    // Modal Durumları
    var isCreateOrEditDialogOpen by remember { mutableStateOf(false) }
    var editingAnnouncement by remember { mutableStateOf<Announcement?>(null) }
    var reportAnnouncement by remember { mutableStateOf<Announcement?>(null) }
    var announcementToDelete by remember { mutableStateOf<Announcement?>(null) }
    var selectedAnnouncementId by remember { mutableStateOf<String?>(null) }

    val selectedAnnouncement = remember(announcements, selectedAnnouncementId) {
        selectedAnnouncementId?.let { id -> announcements.firstOrNull { it.id == id } }
    }

    LaunchedEffect(activeSubPageName) {
        if (activeSubPageName != "ILETISIM_DUYURULAR") {
            selectedAnnouncementId = null
        }
    }

    LaunchedEffect(selectedAnnouncementId, selectedAnnouncement) {
        if (selectedAnnouncementId != null && selectedAnnouncement == null) {
            selectedAnnouncementId = null
        }
    }

    // Geri tuşu: Sohbet veya duyuru detayı açıksa listeye döner
    BackHandler(enabled = selectedChatTarget != null) {
        selectedChatTarget = null
        onActiveChatStudentChange(null)
    }
    BackHandler(enabled = selectedChatTarget == null && activeTab == 0 && selectedAnnouncement != null) {
        selectedAnnouncementId = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag("screen_communication")
        ) {
            // --- 1. PUNTO (YAZI BOYUTU) KONTROL ÇUBUĞU ---
            if (selectedChatTarget == null && !(activeTab == 0 && selectedAnnouncement != null)) {
                CommunicationPuntoBar(
                    fontOffset = fontOffset,
                    isDarkTheme = isDarkTheme,
                    onDecrease = {
                        if (fontOffset > -2) {
                            val next = fontOffset - 2
                            fontOffset = next
                            CommunicationPersistenceHelper.saveFontOffset(context, next)
                        }
                    },
                    onIncrease = {
                        if (fontOffset < 6) {
                            val next = fontOffset + 2
                            fontOffset = next
                            CommunicationPersistenceHelper.saveFontOffset(context, next)
                        }
                    }
                )
            }

            // --- 2. İÇERİK: DUYURULAR VEYA MESAJLAR ---
            if (selectedChatTarget != null) {
                val (chatStudent, chatParent) = selectedChatTarget!!
                val parentsCount = chatStudent.allLinkedParents.size
                // Veli ile Bireysel Sohbet Ekranı (WhatsApp Stili)
                OneOnOneChatView(
                    student = chatStudent,
                    parentAccount = chatParent,
                    teacherName = classRoom.teacherName,
                    messages = chatMessages.filter {
                        it.studentId == chatStudent.id &&
                            (parentsCount <= 1 || it.parentEmail.isBlank() || chatParent.email.isBlank() || it.parentEmail.trim().equals(chatParent.email.trim(), ignoreCase = true))
                    },
                    isDarkTheme = isDarkTheme,
                    fontOffset = fontOffset,
                    onFontOffsetChange = { next ->
                        fontOffset = next
                        CommunicationPersistenceHelper.saveFontOffset(context, next)
                    },
                    onBack = {
                        selectedChatTarget = null
                        onActiveChatStudentChange(null)
                    },
                    onMarkAsRead = {
                        onMarkChatAsRead(chatStudent.id, chatParent.email)
                    },
                    onSendMessage = { text ->
                        onSendMessage(chatStudent.id, text, true, chatParent.email, chatParent.name)
                    },
                    onSimulateParentReply = { text ->
                        onSimulateParentReply(chatStudent.id, text)
                    },
                    onDeleteMessagesForMe = onDeleteMessagesForMe,
                    onDeleteMessagesForEveryone = onDeleteMessagesForEveryone,
                    onClearChatHistoryForMe = {
                        onClearChatHistoryForMe(chatStudent.id, chatParent.email)
                    }
                )
            } else if (activeTab == 0) {
                if (selectedAnnouncement != null) {
                    AnnouncementDetailView(
                        announcement = selectedAnnouncement,
                        classRoom = classRoom,
                        students = students,
                        isDarkTheme = isDarkTheme,
                        fontOffset = fontOffset,
                        onBack = { selectedAnnouncementId = null },
                        onEditClick = {
                            editingAnnouncement = selectedAnnouncement
                            isCreateOrEditDialogOpen = true
                        },
                        onDeleteClick = { announcementToDelete = selectedAnnouncement },
                        onReportClick = { reportAnnouncement = selectedAnnouncement },
                        onVotePollOption = onVotePollOption
                    )
                } else {
                    // DUYURULAR SATIR LİSTESİ GÖRÜNÜMÜ
                    AnnouncementsListView(
                        students = students,
                        announcements = announcements,
                        isDarkTheme = isDarkTheme,
                        fontOffset = fontOffset,
                        onNewAnnouncementClick = {
                            editingAnnouncement = null
                            isCreateOrEditDialogOpen = true
                        },
                        onAnnouncementClick = { ann ->
                            selectedAnnouncementId = ann.id
                        }
                    )
                }
            } else {
                // VELİ MESAJLARI LİSTESİ (Öğrenci & Bağlı Veli Hesapları)
                ParentMessagesListView(
                    students = students,
                    chatMessages = chatMessages,
                    isDarkTheme = isDarkTheme,
                    fontOffset = fontOffset,
                    onSelectParentChat = { student, parentAccount ->
                        selectedChatTarget = Pair(student, parentAccount)
                        onActiveChatStudentChange(student.id)
                        onMarkChatAsRead(student.id, parentAccount.email)
                    }
                )
            }
        }
    }

    // --- MODAL DİYALOGLAR ---

    // 1. Yeni Duyuru / Duyuru Düzenle Modal
    if (isCreateOrEditDialogOpen) {
        CreateOrEditAnnouncementDialog(
            classRoom = classRoom,
            students = students,
            announcementToEdit = editingAnnouncement,
            isDarkTheme = isDarkTheme,
            fontOffset = fontOffset,
            onFontOffsetChange = { next ->
                fontOffset = next
                CommunicationPersistenceHelper.saveFontOffset(context, next)
            },
            onDismiss = { isCreateOrEditDialogOpen = false },
            onSave = { targetType, targetStudentIds, targetStudentNames, title, content, poll ->
                if (editingAnnouncement != null) {
                    onEditAnnouncement(
                        editingAnnouncement!!.id,
                        title,
                        content,
                        poll,
                        targetType,
                        targetStudentIds,
                        targetStudentNames
                    )
                } else {
                    val allParentNames = AnnouncementIdentityHelper.formatExpectedRecipientsForAnnouncement(
                        targetType = targetType,
                        targetStudentIds = targetStudentIds,
                        allStudents = students
                    )

                    val newAnn = Announcement(
                        id = UUID.randomUUID().toString(),
                        classId = classRoom.id,
                        title = title,
                        content = content,
                        targetType = targetType,
                        targetStudentIds = targetStudentIds,
                        targetStudentNames = targetStudentNames,
                        poll = poll,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        isEdited = false,
                        deliveredToParentNames = allParentNames,
                        readReceipts = emptyMap()
                    )
                    onSaveAnnouncement(newAnn)
                }
                isCreateOrEditDialogOpen = false
            }
        )
    }

    // 2. Okunma & Teslim Raporu Modalı (WhatsApp Tarzı)
    reportAnnouncement?.let { ann ->
        AnnouncementReadReportDialog(
            announcement = ann,
            students = students,
            isDarkTheme = isDarkTheme,
            onDismiss = { reportAnnouncement = null }
        )
    }

    // 3. Duyuru Silme Onayı
    announcementToDelete?.let { ann ->
        AlertDialog(
            onDismissRequest = { announcementToDelete = null },
            containerColor = if (isDarkTheme) BirizDarkSurfaceGlass else Color.White,
            title = {
                Text(
                    text = "Duyuruyu Sil",
                    fontWeight = FontWeight.Bold,
                    color = BirizError
                )
            },
            text = {
                Text(
                    text = "'${ann.title}' başlıklı duyuruyu silmek istediğinize emin misiniz? Bu işlem geri alınamaz.",
                    fontSize = 13.sp,
                    color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAnnouncement(ann.id)
                        announcementToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BirizError)
                ) {
                    Text("Sil", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { announcementToDelete = null }) {
                    Text("Vazgeç", color = if (isDarkTheme) Color.White else BirizNavyPrimary)
                }
            }
        )
    }
}

// =========================================================================
// 1. DUYURULAR LİSTESİ BÖLÜMÜ
// =========================================================================

@Composable
private fun AnnouncementsListView(
    students: List<Student>,
    announcements: List<Announcement>,
    isDarkTheme: Boolean,
    fontOffset: Int,
    onNewAnnouncementClick: () -> Unit,
    onAnnouncementClick: (Announcement) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("Tümü") } // Tümü, Tüm Sınıf, Özel, Anketli
    val filters = listOf("Tümü", "Tüm Sınıf", "Özel", "Anketli")

    val filteredList = announcements.filter { ann ->
        when (selectedFilter) {
            "Tüm Sınıf" -> ann.targetType == AnnouncementTargetType.ALL_CLASS
            "Özel" -> ann.targetType == AnnouncementTargetType.SPECIFIC_STUDENTS
            "Anketli" -> ann.poll != null
            else -> true
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp), // Dock bar altında kalmayı önler
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Hero Eylem Kartı
        item {
            BentoCard(
                isDark = isDarkTheme,
                glowColor = BirizAmber,
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "📢 Sınıf Duyuru Portalı",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Detay, anket ve raporları açmak için duyuru satırına dokunun.",
                            fontSize = 11.5.sp,
                            color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onNewAnnouncementClick,
                        colors = ButtonDefaults.buttonColors(containerColor = BirizNavyPrimary),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("btn_create_announcement")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Yeni Duyuru", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
                        )
                    ) {
                        Text(
                            text = filter,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = when {
                                isSelected -> if (isDarkTheme) BirizNavyDark else Color.White
                                else -> if (isDarkTheme) Color.White else BirizNavyPrimary
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
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
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = BirizTextMuted,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Bu filtreye uygun duyuru bulunmuyor.",
                            fontSize = 13.sp,
                            color = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted
                        )
                    }
                }
            }
        } else {
            items(filteredList, key = { it.id }) { ann ->
                AnnouncementSummaryRowCard(
                    announcement = ann,
                    students = students,
                    isDarkTheme = isDarkTheme,
                    fontOffset = fontOffset,
                    onClick = { onAnnouncementClick(ann) }
                )
            }
        }
    }
}

@Composable
private fun AnnouncementSummaryRowCard(
    announcement: Announcement,
    students: List<Student>,
    isDarkTheme: Boolean,
    fontOffset: Int,
    onClick: () -> Unit
) {
    val isAll = announcement.targetType == AnnouncementTargetType.ALL_CLASS
    val hasPoll = announcement.poll != null
    val (readCount, deliveredCount) = remember(announcement, students) {
        AnnouncementIdentityHelper.resolveAnnouncementReadSummary(announcement, students)
    }

    val accentColor = when {
        hasPoll -> Color(0xFF10B981)
        isAll -> BirizTurquoise
        else -> BirizAmber
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(15.dp),
        color = if (isDarkTheme) Color(0x30101F36) else Color.White,
        border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else accentColor.copy(alpha = 0.35f)),
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("announcement_row_${announcement.id}")
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
                // Üst satır: Hedef + Anket rozeti + Tarih
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
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = if (isAll) {
                                if (isDarkTheme) Color(0x300284C7) else Color(0xFFE0F2FE)
                            } else {
                                if (isDarkTheme) Color(0x30D97706) else Color(0xFFFEF3C7)
                            }
                        ) {
                            Text(
                                text = if (isAll) "Tüm Sınıf" else "Özel (${announcement.targetStudentNames.size})",
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
                                    text = "Anketli",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF059669),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (announcement.isEdited) {
                            Text(
                                text = "✏️ Düzenlendi",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFEA580C),
                                maxLines = 1
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
                    fontSize = (14.5 + (fontOffset / 2)).sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = (19 + (fontOffset / 2)).sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Alt özet satırı: Okundu + Oy sayısı
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = null,
                            tint = if (readCount > 0) Color(0xFF0284C7) else Color.Gray,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "$readCount / $deliveredCount Okudu",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                        )
                    }

                    announcement.poll?.let { poll ->
                        Text(
                            text = "• 🗳️ ${poll.totalVotes} Oy",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF059669)
                        )
                    }

                    if (!isAll && announcement.targetStudentNames.isNotEmpty()) {
                        val suffix = if (announcement.targetStudentNames.size > 1) {
                            " +${announcement.targetStudentNames.size - 1}"
                        } else ""
                        Text(
                            text = "• ${announcement.targetStudentNames.first()}$suffix",
                            fontSize = 10.sp,
                            color = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = "Duyuru Detayını Aç",
                tint = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun AnnouncementDetailView(
    announcement: Announcement,
    classRoom: ClassRoom,
    students: List<Student>,
    isDarkTheme: Boolean,
    fontOffset: Int,
    onBack: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onReportClick: () -> Unit,
    onVotePollOption: (String, String, String) -> Unit
) {
    val context = LocalContext.current
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
                            .testTag("btn_back_announcement_list")
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
                    onClick = onReportClick,
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDarkTheme) Color(0x40334E68) else Color(0xFFEFF6FF),
                    border = BorderStroke(0.8.dp, if (isDarkTheme) Color(0xFF38BDF8) else Color(0xFF93C5FD)),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = if (isDarkTheme) Color(0xFF38BDF8) else Color(0xFF1D4ED8),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Rapor",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) Color(0xFF38BDF8) else Color(0xFF1D4ED8)
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
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .padding(bottom = 110.dp)
        ) {
            AnnouncementItemCard(
                announcement = announcement,
                classRoom = classRoom,
                students = students,
                isDarkTheme = isDarkTheme,
                fontOffset = fontOffset,
                onEditClick = onEditClick,
                onDeleteClick = onDeleteClick,
                onReportClick = onReportClick,
                onShareWhatsApp = {
                    val shareText = CommunicationPersistenceHelper.generateAnnouncementWhatsAppShareText(
                        announcement = announcement,
                        className = "${classRoom.gradeLevel}/${classRoom.section} Sınıfı",
                        teacherName = classRoom.teacherName
                    )
                    CommunicationPersistenceHelper.shareText(
                        context = context,
                        text = shareText,
                        title = "Duyuruyu WhatsApp ile Paylaş"
                    )
                },
                onVotePollOption = { optionId ->
                    onVotePollOption(announcement.id, optionId, "Öğretmen (Test)")
                }
            )
        }
    }
}

@Composable
private fun AnnouncementItemCard(
    announcement: Announcement,
    classRoom: ClassRoom,
    students: List<Student>,
    isDarkTheme: Boolean,
    fontOffset: Int,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onReportClick: () -> Unit,
    onShareWhatsApp: () -> Unit,
    onVotePollOption: (String) -> Unit
) {
    val (resolvedReadCount, resolvedDeliveredCount) = remember(announcement, students) {
        AnnouncementIdentityHelper.resolveAnnouncementReadSummary(announcement, students)
    }
    BentoCard(
        isDark = isDarkTheme,
        glowColor = if (announcement.targetType == AnnouncementTargetType.ALL_CLASS) BirizTurquoise else BirizAmber,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 1. Üst Başlık & Rozetler
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Hedef Rozeti
                    val isAll = announcement.targetType == AnnouncementTargetType.ALL_CLASS
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = if (isAll) Icons.Default.Group else Icons.Default.Person,
                                contentDescription = null,
                                tint = if (isAll) Color(0xFF0284C7) else Color(0xFFD97706),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isAll) "Tüm Sınıf (${resolvedDeliveredCount} Veli)" else "Özel (${announcement.targetStudentNames.size} Öğrenci)",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAll) Color(0xFF0284C7) else Color(0xFFD97706)
                            )
                        }
                    }

                    // Düzenlendi Rozeti
                    if (announcement.isEdited) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDarkTheme) Color(0x30F97316) else Color(0xFFFFEDD5),
                            border = BorderStroke(0.8.dp, Color(0xFFF97316))
                        ) {
                            Text(
                                text = "✏️ Düzenlendi (Okuma Sıfırlandı)",
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
                    fontSize = 10.sp,
                    color = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted
                )
            }

            // Özel Hedef Öğrenci İsimleri
            if (announcement.targetType == AnnouncementTargetType.SPECIFIC_STUDENTS && announcement.targetStudentNames.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "🎯 Hedef: ${announcement.targetStudentNames.joinToString(", ")}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDarkTheme) Color(0xFFFDE68A) else Color(0xFFB45309)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Duyuru Başlığı
            Text(
                text = announcement.title,
                fontSize = (15 + fontOffset).sp,
                fontWeight = FontWeight.Black,
                color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary,
                lineHeight = (20 + fontOffset).sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 3. Duyuru Metni
            Text(
                text = announcement.content,
                fontSize = (12.5 + fontOffset).sp,
                color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary,
                lineHeight = (18 + fontOffset).sp
            )

            // 4. Anket Bölümü (Eğer varsa)
            announcement.poll?.let { poll ->
                Spacer(modifier = Modifier.height(12.dp))
                PollCardComponent(
                    poll = poll,
                    students = students,
                    totalDelivered = resolvedDeliveredCount,
                    isDarkTheme = isDarkTheme,
                    fontOffset = fontOffset,
                    onVote = onVotePollOption
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. GÖRÜLDÜ / OKUNDU GÖSTERGE BANDI (WhatsApp Raporu)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDarkTheme) Color(0x350F243E) else Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // WhatsApp Tarzı Çift Tik
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Okundu",
                            tint = if (resolvedReadCount > 0) Color(0xFF38BDF8) else Color.Gray, // Mavi çift tik
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "$resolvedReadCount / $resolvedDeliveredCount Veli Okudu",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                            )
                            val percent = if (resolvedDeliveredCount > 0) {
                                (resolvedReadCount * 100) / resolvedDeliveredCount
                            } else 0
                            Text(
                                text = "%$percent Görüldü Oranı",
                                fontSize = 10.sp,
                                color = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted
                            )
                        }
                    }

                    // "Raporu Gör 📊" Butonu
                    Surface(
                        onClick = onReportClick,
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDarkTheme) Color(0x40334E68) else Color(0xFFEFF6FF),
                        border = BorderStroke(0.8.dp, if (isDarkTheme) Color(0xFF38BDF8) else Color(0xFF93C5FD))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = null,
                                tint = if (isDarkTheme) Color(0xFF38BDF8) else Color(0xFF1D4ED8),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Rapor 📊",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) Color(0xFF38BDF8) else Color(0xFF1D4ED8)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 6. EYLEM BUTONLARI: WhatsApp Paylaş + Düzenle + Sil
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // WhatsApp ile Paylaş Butonu (Yeşil WhatsApp Stili)
                Button(
                    onClick = onShareWhatsApp,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_share_announcement_whatsapp")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "WhatsApp",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "WhatsApp'ta Paylaş",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Düzenle Butonu
                    Surface(
                        onClick = onEditClick,
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDarkTheme) Color(0x35334E68) else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Düzenle",
                                tint = BirizTurquoise,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Düzenle",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDarkTheme) Color.White else BirizNavyPrimary
                            )
                        }
                    }

                    // Sil Butonu
                    Surface(
                        onClick = onDeleteClick,
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDarkTheme) Color(0x30EF4444) else Color(0xFFFEE2E2),
                        border = BorderStroke(1.dp, Color(0xFFF87171))
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Sil",
                                tint = BirizError,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Anket Bileşeni (Seçenekler, yüzdeler ve oy veren veli listesi genişleticisi)
 */
@Composable
private fun PollCardComponent(
    poll: AnnouncementPoll,
    students: List<Student>,
    totalDelivered: Int,
    isDarkTheme: Boolean,
    fontOffset: Int,
    onVote: (String) -> Unit
) {
    var expandedOptionId by remember { mutableStateOf<String?>(null) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isDarkTheme) Color(0x40132238) else Color(0xFFF0FDF4),
        border = BorderStroke(1.dp, if (isDarkTheme) Color(0x5010B981) else Color(0xFF86EFAC)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Poll,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "📊 ANKET (VELİ SONUÇLARI)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF10B981)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = poll.question,
                fontSize = (12.5 + fontOffset).sp,
                fontWeight = FontWeight.Bold,
                color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Seçenekler (Öğretmen görünümünde yalnızca sonuç özeti — veli oylarını kirletmez)
            poll.options.forEach { option ->
                val percent = if (poll.totalVotes > 0) {
                    (option.voteCount * 100) / poll.totalVotes
                } else 0

                val isExpanded = expandedOptionId == option.id

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDarkTheme) Color(0x301E293B) else Color.White,
                        border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = option.text,
                                    fontSize = (12 + fontOffset).sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "%$percent (${option.voteCount} Oy)",
                                    fontSize = (11 + (fontOffset / 2)).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // İlerleme çubuğu
                            LinearProgressIndicator(
                                progress = { if (poll.totalVotes > 0) option.voteCount.toFloat() / poll.totalVotes else 0f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = Color(0xFF10B981),
                                trackColor = if (isDarkTheme) Color(0x3064748B) else Color(0xFFE2E8F0)
                            )
                        }
                    }

                    // Oy Veren Velileri Görme
                    if (option.voterParentNames.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedOptionId = if (isExpanded) null else option.id
                                }
                                .padding(top = 2.dp, bottom = 4.dp, start = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isExpanded) "▾ Oy Verenleri Gizle" else "▸ Oy Veren Velileri Gör (${option.voterParentNames.size} Veli)",
                                fontSize = 10.sp,
                                color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (isExpanded) {
                            val resolvedVoters = option.voterParentNames.map { rawVoter ->
                                AnnouncementIdentityHelper.resolveDisplayLabelForTeacher(rawVoter, students)
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDarkTheme) Color(0x300F243E) else Color(0xFFF1F5F9),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 6.dp, end = 6.dp, bottom = 6.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    resolvedVoters.forEach { voterLabel ->
                                        Text(
                                            text = "• $voterLabel",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Toplam ${poll.totalVotes} veli oy kullandı (${totalDelivered} teslim edildi)",
                fontSize = 10.sp,
                color = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted
            )
        }
    }
}

// =========================================================================
// 2. OKUNMA & TESLİM RAPORU DİYALOĞU (WHATSAPP TARZI)
// =========================================================================

@Composable
private fun AnnouncementReadReportDialog(
    announcement: Announcement,
    students: List<Student>,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit
) {
    val (resolvedReadCount, resolvedDeliveredCount) = remember(announcement, students) {
        AnnouncementIdentityHelper.resolveAnnouncementReadSummary(announcement, students)
    }

    val reportRows = remember(announcement, students) {
        val targetStudents = if (announcement.targetType == AnnouncementTargetType.ALL_CLASS) {
            students
        } else {
            students.filter { it.id in announcement.targetStudentIds }
        }
        if (targetStudents.isNotEmpty()) {
            val studentRows = targetStudents.map { student ->
                val primaryParent = student.allLinkedParents.firstOrNull()?.name ?: student.parentName
                val formattedLabel = AnnouncementIdentityHelper.formatStudentParentLabel(student, primaryParent)
                val ts = AnnouncementIdentityHelper.findStudentReadTimestamp(student, announcement.readReceipts)
                Triple(formattedLabel, ts != null, ts)
            }
            val matchedStudentsSet = targetStudents.filter {
                AnnouncementIdentityHelper.findStudentReadTimestamp(it, announcement.readReceipts) != null
            }
            val unmatchedReceipts = announcement.readReceipts.entries.filter { (key, _) ->
                matchedStudentsSet.none { s ->
                    AnnouncementIdentityHelper.findStudentReadTimestamp(s, mapOf(key to 1L)) != null
                }
            }.map { (key, ts) ->
                Triple(AnnouncementIdentityHelper.resolveDisplayLabelForTeacher(key, students), true, ts as Long?)
            }
            (studentRows + unmatchedReceipts).sortedByDescending { it.second }
        } else {
            announcement.deliveredToParentNames.map { raw ->
                val resolved = AnnouncementIdentityHelper.resolveDisplayLabelForTeacher(raw, students)
                val ts = announcement.readReceipts[raw] ?: announcement.readReceipts[resolved]
                Triple(resolved, ts != null, ts)
            }.sortedByDescending { it.second }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = if (isDarkTheme) BirizDarkSurfaceGlass else Color.White,
            border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Başlık
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF38BDF8).copy(alpha = 0.15f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = null,
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Okunma ve Teslim Raporu",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                            )
                            Text(
                                text = announcement.title,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else BirizTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Özet İstatistik Çubukları
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDarkTheme) Color(0x3010B981) else Color(0xFFDCFCE7),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Okuyan Veliler", fontSize = 10.sp, color = Color(0xFF15803D))
                            Text(
                                "$resolvedReadCount",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF15803D)
                            )
                        }
                    }

                    val unreadCount = (resolvedDeliveredCount - resolvedReadCount).coerceAtLeast(0)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDarkTheme) Color(0x3064748B) else Color(0xFFF1F5F9),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Henüz Okumayan", fontSize = 10.sp, color = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF475569))
                            Text(
                                "$unreadCount",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF475569)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Veli Listesi (Okuyanlar üstte yeşil çift tik ile, okumayanlar gri ile)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(reportRows) { (displayName, isRead, readTimestamp) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isRead) {
                                if (isDarkTheme) Color(0x25064E3B) else Color(0xFFF0FDF4)
                            } else {
                                if (isDarkTheme) Color(0x201E293B) else Color(0xFFF8FAFC)
                            },
                            border = BorderStroke(
                                0.8.dp,
                                if (isRead) Color(0xFF86EFAC) else (if (isDarkTheme) BirizDarkCardBorder else BirizBorder)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DoneAll,
                                        contentDescription = null,
                                        tint = if (isRead) Color(0xFF0284C7) else Color.Gray,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = displayName,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isRead && readTimestamp != null) {
                                        CommunicationPersistenceHelper.formatTime(readTimestamp)
                                    } else {
                                        "İletildi"
                                    },
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isRead) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isRead) Color(0xFF15803D) else Color.Gray
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = BirizNavyPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Kapat", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

// =========================================================================
// 3. YENİ DUYURU / DUYURU DÜZENLEME MODALI
// =========================================================================

@Composable
private fun CreateOrEditAnnouncementDialog(
    classRoom: ClassRoom,
    students: List<Student>,
    announcementToEdit: Announcement?,
    isDarkTheme: Boolean,
    fontOffset: Int,
    onFontOffsetChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onSave: (AnnouncementTargetType, List<String>, List<String>, String, String, AnnouncementPoll?) -> Unit
) {
    var targetType by remember {
        mutableStateOf(announcementToEdit?.targetType ?: AnnouncementTargetType.ALL_CLASS)
    }

    val selectedStudentIds = remember {
        mutableStateListOf<String>().apply {
            if (announcementToEdit != null) {
                addAll(announcementToEdit.targetStudentIds)
            }
        }
    }

    var title by remember { mutableStateOf(announcementToEdit?.title ?: "") }
    var content by remember { mutableStateOf(announcementToEdit?.content ?: "") }

    // Anket Alanları
    var isPollEnabled by remember { mutableStateOf(announcementToEdit?.poll != null) }
    var pollQuestion by remember { mutableStateOf(announcementToEdit?.poll?.question ?: "") }
    val pollOptions = remember {
        mutableStateListOf<String>().apply {
            if (announcementToEdit?.poll != null) {
                addAll(announcementToEdit.poll.options.map { it.text })
            } else {
                add("Evet, katılacağız")
                add("Hayır, katılamayacağız")
            }
        }
    }

    // Belirli Öğrencileri Seçme Modal Alt Bölümü Açık/Kapalı
    var isStudentSelectorOpen by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = if (isDarkTheme) BirizDarkSurfaceGlass else Color.White,
            border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Başlık
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (announcementToEdit != null) "Duyuruyu Düzenle ✏️" else "Yeni Sınıf Duyurusu 📢",
                        fontSize = (15.5 + (fontOffset / 2)).sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Modal içi Punto Küçült
                        Surface(
                            onClick = { if (fontOffset > -2) onFontOffsetChange(fontOffset - 2) },
                            enabled = fontOffset > -2,
                            shape = RoundedCornerShape(8.dp),
                            color = if (fontOffset > -2) (if (isDarkTheme) Color(0x501E293B) else Color(0xFFF1F5F9)) else Color.Transparent,
                            border = BorderStroke(0.8.dp, if (fontOffset > -2) (if (isDarkTheme) BirizDarkCardBorder else BirizBorder) else Color.Transparent),
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("dialog_font_decrease")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "A⁻",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (fontOffset > -2) (if (isDarkTheme) Color.White else BirizNavyPrimary) else Color.Gray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Modal içi Punto Büyüt
                        Surface(
                            onClick = { if (fontOffset < 6) onFontOffsetChange(fontOffset + 2) },
                            enabled = fontOffset < 6,
                            shape = RoundedCornerShape(8.dp),
                            color = if (fontOffset < 6) (if (isDarkTheme) Color(0x501E293B) else Color(0xFFF1F5F9)) else Color.Transparent,
                            border = BorderStroke(0.8.dp, if (fontOffset < 6) (if (isDarkTheme) BirizDarkCardBorder else BirizBorder) else Color.Transparent),
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("dialog_font_increase")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "A⁺",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (fontOffset < 6) (if (isDarkTheme) Color.White else BirizNavyPrimary) else Color.Gray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Kapat",
                                tint = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else BirizTextSecondary
                            )
                        }
                    }
                }

                if (announcementToEdit != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDarkTheme) Color(0x30F97316) else Color(0xFFFFEDD5),
                        border = BorderStroke(0.8.dp, Color(0xFFF97316)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⚠️ Düzenleme yapıldığında okundu/görüldü sayaçları sıfırlanır ve velilere yeni bildirim iletilir.",
                            fontSize = 10.5.sp,
                            color = Color(0xFFC2410C),
                            lineHeight = 14.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Hedef Kitle Seçimi
                    item {
                        Text(
                            text = "Hedef Kitle",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                onClick = { targetType = AnnouncementTargetType.ALL_CLASS },
                                shape = RoundedCornerShape(10.dp),
                                color = if (targetType == AnnouncementTargetType.ALL_CLASS) {
                                    if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                                } else {
                                    if (isDarkTheme) Color(0x301E293B) else Color(0xFFF1F5F9)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "👥 Tüm Sınıf",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (targetType == AnnouncementTargetType.ALL_CLASS) {
                                        if (isDarkTheme) BirizNavyDark else Color.White
                                    } else {
                                        if (isDarkTheme) Color.White else BirizNavyPrimary
                                    },
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp)
                                )
                            }

                            Surface(
                                onClick = { targetType = AnnouncementTargetType.SPECIFIC_STUDENTS },
                                shape = RoundedCornerShape(10.dp),
                                color = if (targetType == AnnouncementTargetType.SPECIFIC_STUDENTS) {
                                    if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                                } else {
                                    if (isDarkTheme) Color(0x301E293B) else Color(0xFFF1F5F9)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "🎯 Özel (${selectedStudentIds.size})",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (targetType == AnnouncementTargetType.SPECIFIC_STUDENTS) {
                                        if (isDarkTheme) BirizNavyDark else Color.White
                                    } else {
                                        if (isDarkTheme) Color.White else BirizNavyPrimary
                                    },
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp)
                                )
                            }
                        }

                        // Eğer özel seçildiyse öğrenci seçme butonu
                        if (targetType == AnnouncementTargetType.SPECIFIC_STUDENTS) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                onClick = { isStudentSelectorOpen = !isStudentSelectorOpen },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDarkTheme) Color(0x30334E68) else Color(0xFFEFF6FF),
                                border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF38BDF8) else Color(0xFF93C5FD)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (selectedStudentIds.isEmpty()) "Öğrenci Seçiniz (Şu an boş)" else "${selectedStudentIds.size} Öğrenci Seçildi",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkTheme) Color(0xFF38BDF8) else Color(0xFF1D4ED8)
                                    )
                                    Icon(
                                        imageVector = if (isStudentSelectorOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = if (isDarkTheme) Color(0xFF38BDF8) else Color(0xFF1D4ED8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            if (isStudentSelectorOpen) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 140.dp)
                                        .background(if (isDarkTheme) Color(0x400B162C) else Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                                        .padding(6.dp)
                                ) {
                                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        items(students) { student ->
                                            val isChecked = selectedStudentIds.contains(student.id)
                                            val isKiz = student.gender.equals("Kız", ignoreCase = true)
                                            Surface(
                                                onClick = {
                                                    if (isChecked) selectedStudentIds.remove(student.id)
                                                    else selectedStudentIds.add(student.id)
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                color = when {
                                                    isKiz -> if (isDarkTheme) Color(0x30F472B6) else Color.White
                                                    else -> if (isDarkTheme) Color(0x3038BDF8) else Color.White
                                                },
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (isChecked) BirizTurquoise else when {
                                                        isKiz -> if (isDarkTheme) Color(0xFFF472B6) else Color(0xFFFCE7F3)
                                                        else -> if (isDarkTheme) Color(0xFF38BDF8) else Color(0xFFE0F2FE)
                                                    }
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 8.dp, vertical = 5.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Checkbox(
                                                        checked = isChecked,
                                                        onCheckedChange = { check ->
                                                            if (check) selectedStudentIds.add(student.id)
                                                            else selectedStudentIds.remove(student.id)
                                                        },
                                                        colors = CheckboxDefaults.colors(checkedColor = BirizTurquoise),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = if (isKiz) Color(0xFFFCE7F3) else Color(0xFFE0F2FE)
                                                    ) {
                                                        Text(
                                                            text = student.studentNumber,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isKiz) Color(0xFFDB2777) else Color(0xFF0284C7),
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = student.fullName,
                                                        fontSize = 11.5.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (isDarkTheme) Color.White else Color(0xFF1E293B),
                                                        modifier = Modifier.weight(1f),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = if (isKiz) "♀" else "♂",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isKiz) Color(0xFFEC4899) else Color(0xFF0EA5E9)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Duyuru Başlığı
                    item {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Duyuru Başlığı", fontSize = (12 + (fontOffset / 2)).sp) },
                            placeholder = { Text("Örn: Müze Gezisi & İzin Formları", fontSize = (12 + (fontOffset / 2)).sp) },
                            textStyle = TextStyle(
                                fontSize = (14 + fontOffset).sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_announcement_title"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BirizTurquoise,
                                unfocusedBorderColor = if (isDarkTheme) BirizDarkCardBorder else BirizBorder
                            )
                        )
                    }

                    // 3. Duyuru Metni
                    item {
                        OutlinedTextField(
                            value = content,
                            onValueChange = { content = it },
                            label = { Text("Duyuru Metni", fontSize = (12 + (fontOffset / 2)).sp) },
                            placeholder = { Text("Duyuru detaylarını ve açıklamaları buraya yazınız...", fontSize = (12 + (fontOffset / 2)).sp) },
                            textStyle = TextStyle(
                                fontSize = (13 + fontOffset).sp,
                                lineHeight = (18 + fontOffset).sp,
                                color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                            ),
                            minLines = 3,
                            maxLines = 6,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_announcement_content"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BirizTurquoise,
                                unfocusedBorderColor = if (isDarkTheme) BirizDarkCardBorder else BirizBorder
                            )
                        )
                    }

                    // 4. Anket Ekleme Alanı
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDarkTheme) Color(0x3010B981) else Color(0xFFF0FDF4),
                            border = BorderStroke(1.dp, if (isDarkTheme) Color(0x5010B981) else Color(0xFF86EFAC)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.HowToVote,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Bu Duyuruya Anket Ekle",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981)
                                        )
                                    }

                                    Switch(
                                        checked = isPollEnabled,
                                        onCheckedChange = { isPollEnabled = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF10B981))
                                    )
                                }

                                if (isPollEnabled) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = pollQuestion,
                                        onValueChange = { pollQuestion = it },
                                        label = { Text("Anket Sorusu", fontSize = (11 + (fontOffset / 2)).sp) },
                                        placeholder = { Text("Örn: Geziye katılım durumunuz nedir?", fontSize = (11 + (fontOffset / 2)).sp) },
                                        textStyle = TextStyle(
                                            fontSize = (12.5 + fontOffset).sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                                        ),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF10B981)
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Seçenekler:", fontSize = (11 + (fontOffset / 2)).sp, fontWeight = FontWeight.Bold)

                                    pollOptions.forEachIndexed { index, opt ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp),
                                             verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = opt,
                                                onValueChange = { pollOptions[index] = it },
                                                placeholder = { Text("Seçenek ${index + 1}", fontSize = (11 + (fontOffset / 2)).sp) },
                                                textStyle = TextStyle(
                                                    fontSize = (12 + fontOffset).sp,
                                                    color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                                                ),
                                                singleLine = true,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (pollOptions.size > 2) {
                                                IconButton(
                                                    onClick = { pollOptions.removeAt(index) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Sil",
                                                        tint = BirizError,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (pollOptions.size < 5) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        OutlinedButton(
                                            onClick = { pollOptions.add("") },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("+ Yeni Seçenek Ekle", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Alt Butonlar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Vazgeç", color = if (isDarkTheme) Color.White else BirizNavyPrimary)
                    }

                    Button(
                        onClick = {
                            if (title.isBlank() || content.isBlank()) return@Button

                            val targetStudentNames = if (targetType == AnnouncementTargetType.SPECIFIC_STUDENTS) {
                                students.filter { selectedStudentIds.contains(it.id) }
                                    .map { "${it.studentNumber} ${it.fullName}" }
                            } else emptyList()

                            val poll = if (isPollEnabled && pollQuestion.isNotBlank()) {
                                val cleanOptionTexts = pollOptions.filter { it.isNotBlank() }
                                AnnouncementPoll(
                                    id = announcementToEdit?.poll?.id ?: UUID.randomUUID().toString(),
                                    question = pollQuestion,
                                    options = cleanOptionTexts.mapIndexed { idx, optText ->
                                        val existingOpt = announcementToEdit?.poll?.options?.find { it.text == optText }
                                            ?: announcementToEdit?.poll?.options?.getOrNull(idx)
                                        PollOption(
                                            id = existingOpt?.id ?: UUID.randomUUID().toString(),
                                            text = optText,
                                            voterParentNames = existingOpt?.voterParentNames ?: emptyList(),
                                            voterIds = existingOpt?.voterIds ?: emptyList()
                                        )
                                    }
                                )
                            } else null

                            onSave(
                                targetType,
                                selectedStudentIds.toList(),
                                targetStudentNames,
                                title.trim(),
                                content.trim(),
                                poll
                            )
                        },
                        enabled = title.isNotBlank() && content.isNotBlank() && (targetType == AnnouncementTargetType.ALL_CLASS || selectedStudentIds.isNotEmpty()),
                        colors = ButtonDefaults.buttonColors(containerColor = BirizNavyPrimary),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("btn_save_announcement")
                    ) {
                        Text(
                            text = if (announcementToEdit != null) "Değişiklikleri Kaydet" else "Duyuruyu Yayınla",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// 4. VELİ MESAJLARI LİSTESİ BÖLÜMÜ (MESAJLAR SEKMESİ)
// =========================================================================

private data class HoveredParentTooltipState(
    val student: Student,
    val parentAccount: LinkedParentAccount,
    val unreadCount: Int,
    val lastMessage: ChatMessage?,
    val anchorRectInWindow: Rect,
    val isPinned: Boolean = false
)

@Composable
private fun ParentMessagesListView(
    students: List<Student>,
    chatMessages: List<ChatMessage>,
    isDarkTheme: Boolean,
    fontOffset: Int,
    onSelectParentChat: (Student, LinkedParentAccount) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    // 0 = Tümü, 1 = Bağlı Veliler (Aktif), 2 = Henüz Bağlanmayanlar (Pasif)
    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    var inactiveStudentForDialog by remember { mutableStateOf<Student?>(null) }

    val listState = rememberLazyListState()
    var rootBoundsInWindow by remember { mutableStateOf(Rect.Zero) }
    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    var activeTooltip by remember { mutableStateOf<HoveredParentTooltipState?>(null) }
    var isAnchorHovered by remember { mutableStateOf(false) }
    var isBubbleHovered by remember { mutableStateOf(false) }
    var bubbleSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            activeTooltip = null
            isAnchorHovered = false
            isBubbleHovered = false
        }
    }

    LaunchedEffect(isAnchorHovered, isBubbleHovered, activeTooltip?.isPinned, activeTooltip?.parentAccount?.email) {
        val current = activeTooltip
        if (current != null && !current.isPinned && !isAnchorHovered && !isBubbleHovered) {
            delay(160)
            if (!isAnchorHovered && !isBubbleHovered) {
                activeTooltip = null
            }
        }
    }

    val activeCount = students.count { it.isParentLinked }
    val inactiveCount = students.count { !it.isParentLinked }

    val filteredStudents = students.filter { student ->
        val matchesSearch = searchQuery.isBlank() ||
                student.fullName.contains(searchQuery, ignoreCase = true) ||
                student.studentNumber.contains(searchQuery) ||
                student.allLinkedParents.any {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                            it.email.contains(searchQuery, ignoreCase = true)
                }
        val matchesFilter = when (selectedFilterIndex) {
            1 -> student.isParentLinked
            2 -> !student.isParentLinked
            else -> true
        }
        matchesSearch && matchesFilter
    }.sortedWith(compareBy({ !it.isParentLinked }, { it.studentNumber.toIntOrNull() ?: Int.MAX_VALUE }))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                rootBoundsInWindow = coords.boundsInWindow()
                rootSize = coords.size
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Güvenlik & Rol Bilgilendirme Kartı (Öğretmen - Veli Özel Hat)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDarkTheme) Color(0x350369A1) else Color(0xFFF0FDF4),
                border = BorderStroke(1.dp, if (isDarkTheme) Color(0x600284C7) else Color(0xFFBBF7D0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = if (isDarkTheme) Color(0xFF38BDF8) else Color(0xFF16A34A),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Öğretmen ↔ Veli Güvenli Özel İletişim Hattı",
                            fontSize = (11.5 + (fontOffset / 2)).sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) BirizDarkTextPrimary else Color(0xFF14532D)
                        )
                        Text(
                            text = "Veli ikonunun üzerine gelerek ad ve Gmail kullanıcı adını görebilir, dokunarak doğrudan mesajlaşabilirsiniz.",
                            fontSize = (10 + (fontOffset / 2)).sp,
                            color = if (isDarkTheme) BirizDarkTextSecondary else Color(0xFF166534),
                            lineHeight = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Arama Çubuğu
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Öğrenci adı, no veya veli e-postası ara...", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = BirizTurquoise,
                        modifier = Modifier.size(18.dp)
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BirizTurquoise,
                    unfocusedBorderColor = if (isDarkTheme) BirizDarkCardBorder else BirizBorder
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Filtre Sekmeleri (Tümü / Bağlı Veliler / Henüz Bağlanmayanlar)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "Tümü (${students.size})",
                    "Bağlı Veliler ($activeCount)",
                    "Bağlı Olmayan ($inactiveCount)"
                ).forEachIndexed { index, label ->
                    val isSelected = selectedFilterIndex == index
                    Surface(
                        onClick = { selectedFilterIndex = index },
                        shape = RoundedCornerShape(20.dp),
                        color = when {
                            isSelected -> if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                            else -> if (isDarkTheme) Color(0x250B162C) else Color(0xFFF1F5F9)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) (if (isDarkTheme) BirizTurquoise else BirizNavyPrimary) else (if (isDarkTheme) BirizDarkCardBorder else BirizBorder)
                        )
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = when {
                                isSelected -> if (isDarkTheme) BirizNavyDark else Color.White
                                else -> if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Öğrenci & Veli İkonları Listesi (Açılır pencere yok — doğrudan etkileşimli ikonlar)
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (filteredStudents.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Aramanıza uygun öğrenci veya veli hesabı bulunamadı.",
                                fontSize = 12.sp,
                                color = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted
                            )
                        }
                    }
                } else {
                    items(filteredStudents, key = { it.id }) { student ->
                        if (!student.isParentLinked) {
                            // PASİF ÖĞRENCİ (Bağlı Veli Yok)
                            InactiveStudentRowItem(
                                student = student,
                                isDarkTheme = isDarkTheme,
                                fontOffset = fontOffset,
                                onClick = { inactiveStudentForDialog = student }
                            )
                        } else {
                            // AKTİF ÖĞRENCİ (Açılır pencere olmadan doğrudan bağlı veli ikonları)
                            val studentMessages = chatMessages.filter { it.studentId == student.id }
                            val totalUnread = studentMessages.count { !it.isFromTeacher && it.status != MessageStatus.READ }

                            ActiveStudentChatCard(
                                student = student,
                                studentMessages = studentMessages,
                                totalUnread = totalUnread,
                                isDarkTheme = isDarkTheme,
                                fontOffset = fontOffset,
                                activeTooltipEmail = if (activeTooltip?.student?.id == student.id) {
                                    activeTooltip?.parentAccount?.email
                                } else null,
                                onHoverParentIcon = { parentAccount, unread, lastMsg, hovered, anchorRect ->
                                    if (hovered) {
                                        isAnchorHovered = true
                                        activeTooltip = HoveredParentTooltipState(
                                            student = student,
                                            parentAccount = parentAccount,
                                            unreadCount = unread,
                                            lastMessage = lastMsg,
                                            anchorRectInWindow = anchorRect,
                                            isPinned = false
                                        )
                                    } else {
                                        if (activeTooltip?.student?.id == student.id &&
                                            activeTooltip?.parentAccount?.email.equals(parentAccount.email, ignoreCase = true)
                                        ) {
                                            isAnchorHovered = false
                                        }
                                    }
                                },
                                onPinParentTooltip = { parentAccount, unread, lastMsg, anchorRect ->
                                    isAnchorHovered = true
                                    activeTooltip = HoveredParentTooltipState(
                                        student = student,
                                        parentAccount = parentAccount,
                                        unreadCount = unread,
                                        lastMessage = lastMsg,
                                        anchorRectInWindow = anchorRect,
                                        isPinned = true
                                    )
                                },
                                onClearTooltip = {
                                    isAnchorHovered = false
                                    isBubbleHovered = false
                                    activeTooltip = null
                                },
                                onSelectParent = { parentAccount ->
                                    isAnchorHovered = false
                                    isBubbleHovered = false
                                    activeTooltip = null
                                    onSelectParentChat(student, parentAccount)
                                }
                            )
                        }
                    }
                }
            }
        }

        // Aynı Pencere İçi Yüzen Veli Bilgi Baloncuğu (Hover veya Uzun Basma İle)
        activeTooltip?.let { tooltip ->
            if (tooltip.isPinned) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(40f)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            activeTooltip = null
                            isAnchorHovered = false
                            isBubbleHovered = false
                        }
                )
            }

            val density = LocalDensity.current
            val gapPx = with(density) { 8.dp.roundToPx() }
            val sidePadPx = with(density) { 10.dp.roundToPx() }
            val fallbackWidthPx = with(density) { 248.dp.roundToPx() }
            val fallbackHeightPx = with(density) { 112.dp.roundToPx() }

            val effectiveW = if (bubbleSize.width > 0) bubbleSize.width else fallbackWidthPx
            val effectiveH = if (bubbleSize.height > 0) bubbleSize.height else fallbackHeightPx

            val relRight = (tooltip.anchorRectInWindow.right - rootBoundsInWindow.left).toInt()
            val relTop = (tooltip.anchorRectInWindow.top - rootBoundsInWindow.top).toInt()
            val relBottom = (tooltip.anchorRectInWindow.bottom - rootBoundsInWindow.top).toInt()

            val maxX = (rootSize.width - effectiveW - sidePadPx).coerceAtLeast(sidePadPx)
            val x = (relRight - effectiveW).coerceIn(sidePadPx, maxX)
            val aboveY = relTop - effectiveH - gapPx
            val belowY = relBottom + gapPx
            val maxY = (rootSize.height - effectiveH - sidePadPx).coerceAtLeast(sidePadPx)
            val y = if (aboveY >= sidePadPx) aboveY else belowY.coerceAtMost(maxY)

            ParentHoverInfoBubble(
                tooltip = tooltip,
                isDarkTheme = isDarkTheme,
                modifier = Modifier
                    .zIndex(50f)
                    .offset { IntOffset(x, y) }
                    .onSizeChanged { bubbleSize = it },
                onBubbleHoverChange = { hovered ->
                    isBubbleHovered = hovered
                },
                onClick = {
                    val targetStudent = tooltip.student
                    val targetParent = tooltip.parentAccount
                    isAnchorHovered = false
                    isBubbleHovered = false
                    activeTooltip = null
                    onSelectParentChat(targetStudent, targetParent)
                }
            )
        }
    }

    // Pasif Öğrenci Veli Katılım Kodu Paylaşım Modalı
    if (inactiveStudentForDialog != null) {
        val student = inactiveStudentForDialog!!
        InactiveStudentCodeDialog(
            student = student,
            isDarkTheme = isDarkTheme,
            onDismiss = { inactiveStudentForDialog = null }
        )
    }
}

@Composable
private fun InactiveStudentRowItem(
    student: Student,
    isDarkTheme: Boolean,
    fontOffset: Int,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isDarkTheme) Color(0x181E293B) else Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, if (isDarkTheme) Color(0x35334155) else Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pasif Profil Rozeti (Muted Gri)
            Surface(
                shape = CircleShape,
                color = if (isDarkTheme) Color(0x30334155) else Color(0xFFE2E8F0),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = student.studentNumber,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // Öğrenci Adı Soyadı (Tam genişlik, sıkışmaz)
                Text(
                    text = student.fullName,
                    fontSize = (13.5 + (fontOffset / 2)).sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) BirizDarkTextSecondary else Color(0xFF334155),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                // İsmin Altında: Bağlı Veli Yok (Pasif) Rozeti ve Bilgisi
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isDarkTheme) Color(0x2564748B) else Color(0xFFF1F5F9),
                        border = BorderStroke(0.8.dp, if (isDarkTheme) Color(0x4064748B) else Color(0xFFCBD5E1))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LinkOff,
                                contentDescription = null,
                                tint = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Bağlı Veli Yok (Pasif)",
                                fontSize = (10 + (fontOffset / 2)).sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                    }

                    Text(
                        text = "• Katılım kodu bekliyor",
                        fontSize = (10 + (fontOffset / 2)).sp,
                        color = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Kodu Gör Butonu
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isDarkTheme) Color(0x300284C7) else Color(0xFFEFF6FF),
                border = BorderStroke(1.dp, if (isDarkTheme) Color(0x500284C7) else Color(0xFFBFDBFE))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Kodu Gör",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveStudentChatCard(
    student: Student,
    studentMessages: List<ChatMessage>,
    totalUnread: Int,
    isDarkTheme: Boolean,
    fontOffset: Int,
    activeTooltipEmail: String?,
    onHoverParentIcon: (LinkedParentAccount, Int, ChatMessage?, Boolean, Rect) -> Unit,
    onPinParentTooltip: (LinkedParentAccount, Int, ChatMessage?, Rect) -> Unit,
    onClearTooltip: () -> Unit,
    onSelectParent: (LinkedParentAccount) -> Unit
) {
    val isKiz = student.gender.equals("Kız", ignoreCase = true)
    val parents = student.allLinkedParents
    val latestStudentMessage = remember(studentMessages) {
        studentMessages.filter { !it.isDeletedForEveryone }.maxByOrNull { it.timestamp }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (totalUnread > 0) {
            if (isDarkTheme) Color(0x300F2746) else Color(0xFFF8FAFC)
        } else {
            if (isDarkTheme) Color(0x250B162C) else Color.White
        },
        border = BorderStroke(
            1.2.dp,
            if (totalUnread > 0) {
                if (isDarkTheme) Color(0x8038BDF8) else Color(0xFF93C5FD)
            } else {
                if (isDarkTheme) BirizDarkCardBorder else BirizBorder
            }
        ),
        shadowElevation = if (totalUnread > 0) 2.dp else 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Öğrenci Profil Numarası
            Surface(
                shape = CircleShape,
                color = if (isDarkTheme) {
                    if (isKiz) Color(0x30DB2777) else Color(0x300284C7)
                } else {
                    if (isKiz) Color(0xFFFCE7F3) else Color(0xFFE0F2FE)
                },
                border = BorderStroke(1.dp, if (isKiz) Color(0xFFF472B6) else Color(0xFF38BDF8)),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = student.studentNumber,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isKiz) Color(0xFFDB2777) else Color(0xFF0284C7)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Öğrenci Adı + Altında Bağlı Veli Sayısı & Son Mesaj Özeti
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.fullName,
                    fontSize = (13.5 + (fontOffset / 2)).sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isDarkTheme) Color(0x300284C7) else Color(0xFFE0F2FE)
                    ) {
                        Text(
                            text = "${parents.size} Veli",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (totalUnread > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = BirizAmber
                        ) {
                            Text(
                                text = "$totalUnread Yeni",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = latestStudentMessage?.text ?: "Sohbet için veli ikonuna dokunun",
                        fontSize = 10.5.sp,
                        color = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Sağda: Bağlı Veli Sayısı Kadar İkon (Açılır pencere yok — hover ile Ad & Gmail kullanıcı adı baloncuğu)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.widthIn(max = 152.dp)
            ) {
                parents.forEach { parentAccount ->
                    val parentMessages = studentMessages.filter {
                        parents.size <= 1 ||
                            it.parentEmail.isBlank() ||
                            it.parentEmail.equals(parentAccount.email, ignoreCase = true)
                    }
                    val parentLastMsg = parentMessages.filter { !it.isDeletedForEveryone }.maxByOrNull { it.timestamp }
                    val parentUnread = parentMessages.count {
                        !it.isFromTeacher && !it.isDeletedForEveryone && it.status != MessageStatus.READ
                    }
                    val isThisTooltipOpen = activeTooltipEmail != null &&
                        activeTooltipEmail.equals(parentAccount.email, ignoreCase = true)

                    ParentQuickChatIcon(
                        parentAccount = parentAccount,
                        unreadCount = parentUnread,
                        lastMessage = parentLastMsg,
                        isTooltipHighlighted = isThisTooltipOpen,
                        isDarkTheme = isDarkTheme,
                        onHoverChanged = { hovered, anchorRect ->
                            onHoverParentIcon(parentAccount, parentUnread, parentLastMsg, hovered, anchorRect)
                        },
                        onLongPressPin = { anchorRect ->
                            onPinParentTooltip(parentAccount, parentUnread, parentLastMsg, anchorRect)
                        },
                        onClearTooltip = onClearTooltip,
                        onOpenChat = { onSelectParent(parentAccount) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ParentQuickChatIcon(
    parentAccount: LinkedParentAccount,
    unreadCount: Int,
    lastMessage: ChatMessage?,
    isTooltipHighlighted: Boolean,
    isDarkTheme: Boolean,
    onHoverChanged: (Boolean, Rect) -> Unit,
    onLongPressPin: (Rect) -> Unit,
    onClearTooltip: () -> Unit,
    onOpenChat: () -> Unit
) {
    val hoverInteractionSource = remember { MutableInteractionSource() }
    val isSystemHovered by hoverInteractionSource.collectIsHoveredAsState()
    var anchorBounds by remember { mutableStateOf(Rect.Zero) }

    LaunchedEffect(isSystemHovered, anchorBounds) {
        if (anchorBounds != Rect.Zero) {
            onHoverChanged(isSystemHovered, anchorBounds)
        }
    }

    val accentColor = when (parentAccount.relationship) {
        "Anne" -> Color(0xFFDB2777)
        "Baba" -> Color(0xFF0284C7)
        else -> Color(0xFF0D9488)
    }
    val fillColor = when (parentAccount.relationship) {
        "Anne" -> if (isDarkTheme) Color(0x35DB2777) else Color(0xFFFCE7F3)
        "Baba" -> if (isDarkTheme) Color(0x350284C7) else Color(0xFFE0F2FE)
        else -> if (isDarkTheme) Color(0x350D9488) else Color(0xFFCCFBF1)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .onGloballyPositioned { coords ->
                anchorBounds = coords.boundsInWindow()
            }
            .hoverable(hoverInteractionSource)
            .pointerInput(parentAccount.email, parentAccount.name) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        when (event.type) {
                            PointerEventType.Enter -> {
                                onHoverChanged(true, anchorBounds)
                            }
                            PointerEventType.Move -> {
                                if (event.changes.any { !it.pressed }) {
                                    onHoverChanged(true, anchorBounds)
                                }
                            }
                            PointerEventType.Exit -> {
                                onHoverChanged(false, anchorBounds)
                            }
                            else -> Unit
                        }
                    }
                }
            }
            .combinedClickable(
                onClick = {
                    onClearTooltip()
                    onOpenChat()
                },
                onLongClick = {
                    onLongPressPin(anchorBounds)
                }
            )
            .testTag("parent_icon_${parentAccount.email}")
    ) {
        // Veli Profil + Sohbet İkonu
        Surface(
            shape = CircleShape,
            color = fillColor,
            border = BorderStroke(
                width = if (isTooltipHighlighted || unreadCount > 0) 2.dp else 1.2.dp,
                color = if (unreadCount > 0) BirizAmber else accentColor
            ),
            modifier = Modifier.size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "${parentAccount.name} (${parentAccount.email}) ile mesajlaş",
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Sağ alt köşede sohbet simgesi
        Surface(
            shape = CircleShape,
            color = accentColor,
            border = BorderStroke(1.dp, if (isDarkTheme) BirizNavyDark else Color.White),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 4.dp, bottom = 4.dp)
                .size(16.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(9.dp)
                )
            }
        }

        // Okunmamış mesaj sayısı rozeti
        if (unreadCount > 0) {
            Surface(
                shape = CircleShape,
                color = BirizAmber,
                border = BorderStroke(1.dp, if (isDarkTheme) BirizNavyDark else Color.White),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 3.dp, end = 3.dp)
                    .size(17.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (unreadCount > 9) "9+" else "$unreadCount",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ParentHoverInfoBubble(
    tooltip: HoveredParentTooltipState,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    onBubbleHoverChange: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val parentAccount = tooltip.parentAccount
    val unreadCount = tooltip.unreadCount
    val lastMessage = tooltip.lastMessage
    val emailTrimmed = parentAccount.email.trim()
    val gmailUsername = emailTrimmed.substringBefore("@").ifBlank { emailTrimmed }

    val accentColor = when (parentAccount.relationship) {
        "Anne" -> Color(0xFFDB2777)
        "Baba" -> Color(0xFF0284C7)
        else -> Color(0xFF0D9488)
    }

    val hoverSource = remember { MutableInteractionSource() }
    val isBubbleSystemHovered by hoverSource.collectIsHoveredAsState()
    LaunchedEffect(isBubbleSystemHovered) {
        onBubbleHoverChange(isBubbleSystemHovered)
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isDarkTheme) Color(0xFF0F1E33) else Color.White,
        border = BorderStroke(1.4.dp, accentColor.copy(alpha = 0.8f)),
        shadowElevation = 12.dp,
        modifier = modifier
            .widthIn(min = 215.dp, max = 285.dp)
            .hoverable(hoverSource)
            .pointerInput(emailTrimmed) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        when (event.type) {
                            PointerEventType.Enter -> onBubbleHoverChange(true)
                            PointerEventType.Move -> {
                                if (event.changes.any { !it.pressed }) {
                                    onBubbleHoverChange(true)
                                }
                            }
                            PointerEventType.Exit -> onBubbleHoverChange(false)
                            else -> Unit
                        }
                    }
                }
            }
            .testTag("parent_hover_bubble_${emailTrimmed}")
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            // 1. Veli Adı Soyadı ve Yakınlık Rozeti
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(5.dp),
                    color = accentColor.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = parentAccount.relationship,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = parentAccount.name.ifBlank { "Doğrulanmış Veli" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 2. Gmail Kullanıcı Adı & E-posta Kutusu
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isDarkTheme) Color(0x280284C7) else Color(0xFFF1F5F9),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (gmailUsername.isNotBlank()) gmailUsername else "E-posta kayıtlı değil",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isDarkTheme) Color(0xFF38BDF8) else BirizNavyPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (emailTrimmed.contains("@")) {
                        Text(
                            text = emailTrimmed,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = when {
                    unreadCount > 0 -> "🔔 $unreadCount okunmamış mesaj • Açmak için tıklayın"
                    lastMessage != null && !lastMessage.isDeletedForEveryone -> "💬 Son: ${lastMessage.text}"
                    else -> "💬 Mesajlaşmak için tıklayın"
                },
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = if (unreadCount > 0) BirizAmber else accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun InactiveStudentCodeDialog(
    student: Student,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val spacedCode = student.parentInviteCode.map { "$it " }.joinToString("").trim()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isDarkTheme) BirizDarkSurfaceGlass else Color.White,
            border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isDarkTheme) Color(0x30D97706) else Color(0xFFFEF3C7),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.LinkOff,
                            contentDescription = null,
                            tint = BirizAmber,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Bağlı Veli Hesabı Yok (Pasif)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${student.studentNumber} ${student.fullName}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDarkTheme) BirizTurquoise else Color(0xFF0284C7)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Bu öğrenci adına sisteme henüz bir veli Google hesabı bağlanmamıştır. Öğretmen-veli mesajlaşması resmi tarafı belirli olan Google hesapları ile yapılır. Velinizin sisteme katılabilmesi için katılım kodunu paylaşabilirsiniz.",
                    fontSize = 12.sp,
                    color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Veli Katılım Kodu Kutusu
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDarkTheme) Color(0x35D97706) else Color(0xFFFEF9C3),
                    border = BorderStroke(1.2.dp, Color(0xFFFBBF24)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "VELİ KATILIM KODU",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB45309)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = spacedCode,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF92400E)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Kodu Kopyala & Veliye Paylaş Butonu
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Veli Katılım Kodu", student.parentInviteCode))

                        val shareText = "BİRİZ İlkokul Sınıf İletişim Sistemi\n\n" +
                                "${student.studentNumber} numaralı öğrencimiz ${student.fullName} için Veli Katılım Kodunuz:\n" +
                                "${student.parentInviteCode}\n\n" +
                                "BİRİZ uygulamasına kendi Google hesabınızla giriş yaparak bu kod ile sınıfımıza katılabilir ve sınıf öğretmenimiz ile doğrudan mesajlaşabilirsiniz."

                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Veli Katılım Kodunu Paylaş"))
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                        contentColor = if (isDarkTheme) BirizNavyDark else Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Kodu Kopyala & Veliye Paylaş", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Kapat", fontSize = 12.sp)
                }
            }
        }
    }
}

// =========================================================================
// 5. BİREYSEL SOHBET GÖRÜNÜMÜ (WHATSAPP STİLİ DİYALOG EKRANI)
// =========================================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OneOnOneChatView(
    student: Student,
    parentAccount: LinkedParentAccount,
    teacherName: String,
    messages: List<ChatMessage>,
    isDarkTheme: Boolean,
    fontOffset: Int,
    onFontOffsetChange: (Int) -> Unit,
    onBack: () -> Unit,
    onMarkAsRead: () -> Unit,
    onSendMessage: (String) -> Unit,
    onSimulateParentReply: (String) -> Unit,
    onDeleteMessagesForMe: (Set<String>) -> Unit = {},
    onDeleteMessagesForEveryone: (Set<String>) -> Unit = {},
    onClearChatHistoryForMe: () -> Unit = {}
) {
    val context = LocalContext.current
    var messageInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val isKiz = student.gender.equals("Kız", ignoreCase = true)
    val isParentOnline = student.isParentCurrentlyActive()

    var isSearchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedMessageIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showDeleteChoiceDialog by remember { mutableStateOf(false) }

    val displayedMessages = remember(messages, isSearchOpen, searchQuery) {
        if (!isSearchOpen || searchQuery.isBlank()) {
            messages
        } else {
            val q = searchQuery.trim()
            messages.filter { !it.isDeletedForEveryone && it.text.contains(q, ignoreCase = true) }
        }
    }

    LaunchedEffect(student.id, parentAccount.email) {
        selectedMessageIds = emptySet()
        isSelectionMode = false
        isSearchOpen = false
        searchQuery = ""
    }

    BackHandler(enabled = isSelectionMode || isSearchOpen) {
        if (isSelectionMode) {
            selectedMessageIds = emptySet()
            isSelectionMode = false
        } else if (isSearchOpen) {
            isSearchOpen = false
            searchQuery = ""
        }
    }

    // Hazır Hızlı Yanıt Şablonları
    val quickTemplates = listOf(
        "Bilgi için teşekkür ederiz 👍",
        "${student.fullName.split(" ").first()} derste çok aktifti ⭐",
        "Ödev kontrolü tamamlandı 📚",
        "Yarın teneffüste görüşelim 🤝"
    )

    val sendCurrentMessage = {
        val trimmed = messageInput.trim()
        if (trimmed.isNotEmpty()) {
            onSendMessage(trimmed)
            messageInput = ""
        }
    }

    val toggleSelection: (String) -> Unit = { msgId ->
        val next = if (msgId in selectedMessageIds) selectedMessageIds - msgId else selectedMessageIds + msgId
        selectedMessageIds = next
        if (next.isEmpty()) isSelectionMode = false
    }

    LaunchedEffect(student.id, messages.size, isSearchOpen) {
        onMarkAsRead()
        if (!isSearchOpen && displayedMessages.isNotEmpty()) {
            listState.animateScrollToItem(displayedMessages.size)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkTheme) Color(0xFF091427) else Color(0xFFEFEAE2))
            .imePadding()
            .navigationBarsPadding()
    ) {
        // WhatsApp Tarzı Üst Başlık Barı (veya Seçim Modu İşlem Barı)
        Surface(
            color = if (isSelectionMode) {
                if (isDarkTheme) Color(0xFF0F2F3C) else BirizNavyDark
            } else {
                if (isDarkTheme) BirizDarkSurfaceGlass else BirizNavyPrimary
            },
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 4.dp
        ) {
            if (isSelectionMode) {
                val selectedMsgs = messages.filter { it.id in selectedMessageIds }
                val copyableText = selectedMsgs
                    .filter { !it.isDeletedForEveryone && it.text.isNotBlank() }
                    .joinToString("\n") { it.text }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            selectedMessageIds = emptySet()
                            isSelectionMode = false
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Seçimi Kapat",
                            tint = Color.White
                        )
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
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Kopyala",
                                tint = Color.White
                            )
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
                            .testTag("btn_teacher_delete_selected_messages")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Seçilenleri Sil",
                            tint = Color(0xFFFCA5A5)
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Geri",
                                tint = Color.White
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = if (isKiz) Color(0xFFF472B6) else Color(0xFF38BDF8),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = student.studentNumber,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${parentAccount.name} (${parentAccount.relationship})",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Doğrulanmış Veli",
                                    tint = BirizTurquoise,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Text(
                                text = if (isParentOnline) {
                                    "● Çevrimiçi • ${student.studentNumber} ${student.fullName}"
                                } else {
                                    "${parentAccount.email} • ${student.studentNumber} ${student.fullName}"
                                },
                                fontSize = 10.sp,
                                fontFamily = if (isParentOnline) FontFamily.SansSerif else FontFamily.Monospace,
                                color = if (isParentOnline) Color(0xFF6EE7B7) else Color.White.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Arama Butonu
                        IconButton(
                            onClick = {
                                isSearchOpen = !isSearchOpen
                                if (!isSearchOpen) searchQuery = ""
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Sohbette Ara",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Punto Boyutu Küçült (A⁻)
                        Surface(
                            onClick = { if (fontOffset > -2) onFontOffsetChange(fontOffset - 2) },
                            enabled = fontOffset > -2,
                            shape = RoundedCornerShape(8.dp),
                            color = if (fontOffset > -2) Color(0x35FFFFFF) else Color(0x15FFFFFF),
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("chat_font_decrease")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "A⁻",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (fontOffset > -2) Color.White else Color.White.copy(alpha = 0.4f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(3.dp))

                        // Punto Boyutu Büyüt (A⁺)
                        Surface(
                            onClick = { if (fontOffset < 6) onFontOffsetChange(fontOffset + 2) },
                            enabled = fontOffset < 6,
                            shape = RoundedCornerShape(8.dp),
                            color = if (fontOffset < 6) Color(0x35FFFFFF) else Color(0x15FFFFFF),
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("chat_font_increase")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "A⁺",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (fontOffset < 6) Color.White else Color.White.copy(alpha = 0.4f)
                                )
                            }
                        }

                        // Sohbet Seçenekleri Menüsü (⋮)
                        Box {
                            IconButton(
                                onClick = { showOverflowMenu = true },
                                modifier = Modifier
                                    .size(40.dp)
                                    .testTag("btn_teacher_chat_menu")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Sohbet Seçenekleri",
                                    tint = Color.White,
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
                                    enabled = messages.isNotEmpty(),
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
                                    enabled = messages.isNotEmpty(),
                                    onClick = {
                                        showOverflowMenu = false
                                        showClearHistoryDialog = true
                                    }
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = isSearchOpen) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text("Mesajlarda ara...", fontSize = 12.5.sp, color = Color.White.copy(alpha = 0.7f))
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                },
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
                                            Icon(Icons.Default.Close, contentDescription = "Aramayı Kapat", tint = Color.White, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                },
                                singleLine = true,
                                textStyle = TextStyle(color = Color.White, fontSize = 13.sp),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BirizTurquoise,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.35f),
                                    cursorColor = BirizTurquoise
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        // Mesaj Listesi
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Şifreleme ve Güvenlik Bilgi Kartı
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDarkTheme) Color(0x401E293B) else Color(0x90FFFBEB),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "🔒 Bu görüşme yalnızca sınıf öğretmeni ve veli (${parentAccount.email}) arasında anlık olarak senkronize edilir. Uzun basarak mesaj silebilirsiniz.",
                            fontSize = 10.sp,
                            color = if (isDarkTheme) BirizDarkTextMuted else Color(0xFF92400E),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            textAlign = TextAlign.Center
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
                            color = if (isDarkTheme) Color(0x401E293B) else Color.White.copy(alpha = 0.9f),
                            border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder)
                        ) {
                            Text(
                                text = if (isSearchOpen && searchQuery.isNotBlank()) {
                                    "🔍 \"$searchQuery\" ile eşleşen mesaj bulunamadı."
                                } else {
                                    "💬 Gösterilecek mesaj bulunmuyor.\nAşağıdan mesaj yazarak veli ile anlık sohbeti başlatabilirsiniz."
                                },
                                fontSize = 12.sp,
                                color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary,
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

                    ChatMessageBubble(
                        message = msg,
                        isDarkTheme = isDarkTheme,
                        fontOffset = fontOffset,
                        isSelected = msg.id in selectedMessageIds,
                        onClick = {
                            if (isSelectionMode) {
                                toggleSelection(msg.id)
                            }
                        },
                        onLongClick = {
                            isSelectionMode = true
                            selectedMessageIds = selectedMessageIds + msg.id
                        }
                    )
                }
            }
        }

        // Hızlı Şablonlar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickTemplates.forEach { template ->
                Surface(
                    onClick = { onSendMessage(template) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDarkTheme) Color(0x401E293B) else Color.White,
                    border = BorderStroke(0.8.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder)
                ) {
                    Text(
                        text = template,
                        fontSize = 11.sp,
                        color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // Mesaj Giriş Barı (WhatsApp Stili - Dock Bar gizlendiği için tam erişilebilir)
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
                    value = messageInput,
                    onValueChange = { messageInput = it },
                    placeholder = { Text("Mesajınızı yazınız...", fontSize = (12.5 + fontOffset).sp) },
                    textStyle = TextStyle(
                        fontSize = (13 + fontOffset).sp,
                        lineHeight = (18 + fontOffset).sp,
                        color = if (isDarkTheme) Color.White else Color(0xFF111B21)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp)
                        .testTag("input_chat_message"),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { sendCurrentMessage() }),
                    shape = RoundedCornerShape(22.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BirizTurquoise,
                        unfocusedBorderColor = if (isDarkTheme) BirizDarkCardBorder else BirizBorder
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                val canSend = messageInput.isNotBlank()
                FloatingActionButton(
                    onClick = { if (canSend) sendCurrentMessage() },
                    shape = CircleShape,
                    containerColor = if (canSend) Color(0xFF00A884) else Color(0xFF94A3B8),
                    contentColor = Color.White,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("btn_send_chat_message")
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
        val selectedMessages = messages.filter { it.id in selectedMessageIds }
        val allSelectedAreMine = selectedMessages.isNotEmpty() && selectedMessages.all { it.isFromTeacher && !it.isDeletedForEveryone }
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
                            "Mesajları yalnızca kendi ekranınızdan silebilirsiniz (velideki mesaj kalır) veya her iki taraftan da kaldırabilirsiniz."
                        } else {
                            "Seçilen mesajlar yalnızca sizin öğretmen sohbet geçmişinizden silinecek. Velinin sohbet geçmişi silinmez."
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
                    text = "Bu veliyle olan sohbet geçmişiniz yalnızca sizin öğretmen hesabınızdan temizlenecektir. Velinin sohbet geçmişi silinmez ve yeni gelecek mesajlar görünmeye devam eder.",
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
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    isDarkTheme: Boolean,
    fontOffset: Int,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val isMe = message.isFromTeacher

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) BirizTurquoise.copy(alpha = 0.22f) else Color.Transparent)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 2.dp, vertical = 1.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isMe) 14.dp else 2.dp,
                bottomEnd = if (isMe) 2.dp else 14.dp
            ),
            color = if (isMe) {
                // Öğretmen mesajı (Yeşilimsi WhatsApp rengi)
                if (isDarkTheme) Color(0xFF005C4B) else Color(0xFFD9FDD3)
            } else {
                // Veli mesajı (Koyu modda lacivert, açık modda beyaz)
                if (isDarkTheme) Color(0xFF202C33) else Color.White
            },
            border = if (isSelected) BorderStroke(1.5.dp, BirizTurquoise) else null,
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 280.dp + (fontOffset * 12).dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                if (!isMe) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = message.senderName,
                            fontSize = (10.5 + (fontOffset / 2)).sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF25D366)
                        )
                        if (message.parentEmail.isNotBlank()) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "• ${message.parentEmail}",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                }

                Text(
                    text = if (message.isDeletedForEveryone) "🚫 Bu mesaj silindi" else message.text,
                    fontSize = (12.5 + fontOffset).sp,
                    fontStyle = if (message.isDeletedForEveryone) FontStyle.Italic else FontStyle.Normal,
                    color = if (message.isDeletedForEveryone) {
                        if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
                    } else {
                        if (isDarkTheme) Color.White else Color(0xFF111B21)
                    },
                    lineHeight = (17 + fontOffset).sp
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = CommunicationPersistenceHelper.formatTime(message.timestamp),
                        fontSize = (9.5 + (fontOffset / 2)).sp,
                        color = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color.Gray
                    )

                    if (isMe && !message.isDeletedForEveryone) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (message.status == MessageStatus.SENT) Icons.Default.Check else Icons.Default.DoneAll,
                            contentDescription = when (message.status) {
                                MessageStatus.READ -> "Okundu"
                                MessageStatus.DELIVERED -> "İletildi"
                                MessageStatus.SENT -> "Gönderildi"
                            },
                            tint = when (message.status) {
                                MessageStatus.READ -> Color(0xFF53BDEB) // WhatsApp Mavi Çift Tik
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

/**
 * İletişim sayfalarına özel Punto Büyütme/Küçültme kontrol çubuğu
 */
@Composable
private fun CommunicationPuntoBar(
    fontOffset: Int,
    isDarkTheme: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isDarkTheme) Color(0x350F243E) else Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🔍 Punto (Yazı Boyutu):",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Küçült Tuşu (A⁻)
                Surface(
                    onClick = onDecrease,
                    enabled = fontOffset > -2,
                    shape = RoundedCornerShape(8.dp),
                    color = if (fontOffset > -2) {
                        if (isDarkTheme) Color(0x501E293B) else Color.White
                    } else {
                        if (isDarkTheme) Color(0x201E293B) else Color(0xFFE2E8F0)
                    },
                    border = BorderStroke(
                        0.8.dp,
                        if (fontOffset > -2) (if (isDarkTheme) BirizDarkCardBorder else BirizBorder) else Color.Transparent
                    ),
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("btn_font_decrease")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "A⁻",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (fontOffset > -2) (if (isDarkTheme) Color.White else BirizNavyPrimary) else Color.Gray
                        )
                    }
                }

                // Seviye Göstergesi
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDarkTheme) Color(0x400284C7) else Color(0xFFE0F2FE),
                    border = BorderStroke(0.8.dp, if (isDarkTheme) Color(0xFF38BDF8) else Color(0xFF7DD3FC)),
                    modifier = Modifier.testTag("badge_font_level")
                ) {
                    Text(
                        text = when (fontOffset) {
                            -2 -> "Küçük (-2)"
                            0 -> "Standart"
                            2 -> "Büyük (+2)"
                            4 -> "Çok Büyük (+4)"
                            else -> "Ekstra (+$fontOffset)"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) Color(0xFF38BDF8) else Color(0xFF0369A1),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Büyüt Tuşu (A⁺)
                Surface(
                    onClick = onIncrease,
                    enabled = fontOffset < 6,
                    shape = RoundedCornerShape(8.dp),
                    color = if (fontOffset < 6) {
                        if (isDarkTheme) Color(0x501E293B) else Color.White
                    } else {
                        if (isDarkTheme) Color(0x201E293B) else Color(0xFFE2E8F0)
                    },
                    border = BorderStroke(
                        0.8.dp,
                        if (fontOffset < 6) (if (isDarkTheme) BirizDarkCardBorder else BirizBorder) else Color.Transparent
                    ),
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("btn_font_increase")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "A⁺",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = if (fontOffset < 6) (if (isDarkTheme) Color.White else BirizNavyPrimary) else Color.Gray
                        )
                    }
                }
            }
        }
    }
}
