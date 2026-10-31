package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Announcement
import com.example.data.model.AnnouncementPoll
import com.example.data.model.AnnouncementTargetType
import com.example.data.model.ChatMessage
import com.example.data.model.ClassRoom
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
    onSendMessage: (String, String, Boolean) -> Unit,
    onSimulateParentReply: (String, String) -> Unit,
    onMarkChatAsRead: (String) -> Unit
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

    // Aktif Sohbet Edilen Öğrenci/Veli (null ise sohbet listesi gösterilir)
    var selectedStudentForChat by remember(initialActiveStudentId) {
        mutableStateOf(students.find { it.id == initialActiveStudentId })
    }

    // Modal Durumları
    var isCreateOrEditDialogOpen by remember { mutableStateOf(false) }
    var editingAnnouncement by remember { mutableStateOf<Announcement?>(null) }
    var reportAnnouncement by remember { mutableStateOf<Announcement?>(null) }
    var announcementToDelete by remember { mutableStateOf<Announcement?>(null) }

    // Geri tuşu: Sohbet açıksa listeye döner
    BackHandler(enabled = selectedStudentForChat != null) {
        selectedStudentForChat = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag("screen_communication")
        ) {
            // --- 1. ÜST SEÇİM ÇUBUĞU (Duyurular & Mesajlar) ---
            if (selectedStudentForChat == null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (isDarkTheme) Color(0x600B162C) else Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. Duyurular Butonu
                        val isDuyuruSelected = activeTab == 0
                        Surface(
                            onClick = { activeTab = 0 },
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                isDuyuruSelected -> if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                                else -> if (isDarkTheme) Color(0x301E293B) else Color.White
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isDuyuruSelected) (if (isDarkTheme) BirizTurquoise else BirizNavyPrimary) else BirizBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tab_communication_announcements")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = null,
                                    tint = if (isDuyuruSelected) (if (isDarkTheme) BirizNavyDark else Color.White) else BirizAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Duyurular (${announcements.size})",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDuyuruSelected) (if (isDarkTheme) BirizNavyDark else Color.White) else (if (isDarkTheme) Color.White else BirizNavyPrimary)
                                )
                            }
                        }

                        // 2. Mesajlar Butonu
                        val isMesajSelected = activeTab == 1
                        val totalUnreadMessages = chatMessages.count { !it.isFromTeacher && it.status != MessageStatus.READ }

                        Surface(
                            onClick = { activeTab = 1 },
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                isMesajSelected -> if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                                else -> if (isDarkTheme) Color(0x301E293B) else Color.White
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isMesajSelected) (if (isDarkTheme) BirizTurquoise else BirizNavyPrimary) else BirizBorder
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tab_communication_messages")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Chat,
                                    contentDescription = null,
                                    tint = if (isMesajSelected) (if (isDarkTheme) BirizNavyDark else Color.White) else Color(0xFF10B981),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Mesajlar",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMesajSelected) (if (isDarkTheme) BirizNavyDark else Color.White) else (if (isDarkTheme) Color.White else BirizNavyPrimary)
                                )
                                if (totalUnreadMessages > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = BirizAmber,
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "$totalUnreadMessages",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // --- 1.1 PUNTO (YAZI BOYUTU) KONTROL ÇUBUĞU ---
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
            if (selectedStudentForChat != null) {
                // Veli ile Bireysel Sohbet Ekranı (WhatsApp Stili)
                OneOnOneChatView(
                    student = selectedStudentForChat!!,
                    teacherName = classRoom.teacherName,
                    messages = chatMessages.filter { it.studentId == selectedStudentForChat!!.id },
                    isDarkTheme = isDarkTheme,
                    fontOffset = fontOffset,
                    onFontOffsetChange = { next ->
                        fontOffset = next
                        CommunicationPersistenceHelper.saveFontOffset(context, next)
                    },
                    onBack = { selectedStudentForChat = null },
                    onSendMessage = { text ->
                        onSendMessage(selectedStudentForChat!!.id, text, true)
                    },
                    onSimulateParentReply = { text ->
                        onSimulateParentReply(selectedStudentForChat!!.id, text)
                    }
                )
            } else if (activeTab == 0) {
                // DUYURULAR GÖRÜNÜMÜ
                AnnouncementsListView(
                    classRoom = classRoom,
                    students = students,
                    announcements = announcements,
                    isDarkTheme = isDarkTheme,
                    fontOffset = fontOffset,
                    onNewAnnouncementClick = {
                        editingAnnouncement = null
                        isCreateOrEditDialogOpen = true
                    },
                    onEditClick = { ann ->
                        editingAnnouncement = ann
                        isCreateOrEditDialogOpen = true
                    },
                    onDeleteClick = { ann ->
                        announcementToDelete = ann
                    },
                    onReportClick = { ann ->
                        reportAnnouncement = ann
                    },
                    onVotePollOption = onVotePollOption
                )
            } else {
                // VELİ MESAJLARI LİSTESİ
                ParentMessagesListView(
                    students = students,
                    chatMessages = chatMessages,
                    isDarkTheme = isDarkTheme,
                    fontOffset = fontOffset,
                    onSelectStudent = { student ->
                        selectedStudentForChat = student
                        onMarkChatAsRead(student.id)
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
                    val allParentNames = if (targetType == AnnouncementTargetType.ALL_CLASS) {
                        students.map { it.parentName ?: "${it.fullName} Velisi" }
                    } else {
                        students.filter { targetStudentIds.contains(it.id) }
                            .map { it.parentName ?: "${it.fullName} Velisi" }
                    }

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
    classRoom: ClassRoom,
    students: List<Student>,
    announcements: List<Announcement>,
    isDarkTheme: Boolean,
    fontOffset: Int,
    onNewAnnouncementClick: () -> Unit,
    onEditClick: (Announcement) -> Unit,
    onDeleteClick: (Announcement) -> Unit,
    onReportClick: (Announcement) -> Unit,
    onVotePollOption: (String, String, String) -> Unit
) {
    val context = LocalContext.current
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
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                            text = "Tüm sınıfa veya özel velilere anketli/anketsiz duyuru yayınlayın.",
                            fontSize = 11.5.sp,
                            color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                        )
                    }

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

        // Duyuru Kartları
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
                AnnouncementItemCard(
                    announcement = ann,
                    classRoom = classRoom,
                    isDarkTheme = isDarkTheme,
                    fontOffset = fontOffset,
                    onEditClick = { onEditClick(ann) },
                    onDeleteClick = { onDeleteClick(ann) },
                    onReportClick = { onReportClick(ann) },
                    onShareWhatsApp = {
                        val shareText = CommunicationPersistenceHelper.generateAnnouncementWhatsAppShareText(
                            announcement = ann,
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
                        onVotePollOption(ann.id, optionId, "Öğretmen (Test)")
                    }
                )
            }
        }
    }
}

@Composable
private fun AnnouncementItemCard(
    announcement: Announcement,
    classRoom: ClassRoom,
    isDarkTheme: Boolean,
    fontOffset: Int,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onReportClick: () -> Unit,
    onShareWhatsApp: () -> Unit,
    onVotePollOption: (String) -> Unit
) {
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
                                text = if (isAll) "Tüm Sınıf (${announcement.deliveredCount} Veli)" else "Özel (${announcement.targetStudentNames.size} Öğrenci)",
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
                    totalDelivered = announcement.deliveredCount,
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
                            tint = if (announcement.readCount > 0) Color(0xFF38BDF8) else Color.Gray, // Mavi çift tik
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "${announcement.readCount} / ${announcement.deliveredCount} Veli Okudu",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                            )
                            val percent = if (announcement.deliveredCount > 0) {
                                (announcement.readCount * 100) / announcement.deliveredCount
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
                    text = "📊 ANKET",
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

            // Seçenekler
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
                        onClick = { onVote(option.id) },
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
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isDarkTheme) Color(0x300F243E) else Color(0xFFF1F5F9),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 6.dp, end = 6.dp, bottom = 6.dp)
                            ) {
                                Text(
                                    text = option.voterParentNames.joinToString(", "),
                                    fontSize = 10.5.sp,
                                    color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary,
                                    modifier = Modifier.padding(6.dp)
                                )
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
    isDarkTheme: Boolean,
    onDismiss: () -> Unit
) {
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
                                "${announcement.readCount}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF15803D)
                            )
                        }
                    }

                    val unreadCount = (announcement.deliveredCount - announcement.readCount).coerceAtLeast(0)
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
                    val sortedParents = announcement.deliveredToParentNames.sortedByDescending {
                        announcement.readReceipts.containsKey(it)
                    }

                    items(sortedParents) { parentName ->
                        val isRead = announcement.readReceipts.containsKey(parentName)
                        val readTimestamp = announcement.readReceipts[parentName]

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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.DoneAll,
                                        contentDescription = null,
                                        tint = if (isRead) Color(0xFF0284C7) else Color.Gray,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = parentName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                                    )
                                }

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
                                                            text = "#${student.studentNumber}",
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
                                    .map { "#${it.studentNumber} ${it.fullName}" }
                            } else emptyList()

                            val poll = if (isPollEnabled && pollQuestion.isNotBlank()) {
                                AnnouncementPoll(
                                    id = announcementToEdit?.poll?.id ?: UUID.randomUUID().toString(),
                                    question = pollQuestion,
                                    options = pollOptions.filter { it.isNotBlank() }.map { optText ->
                                        val existingOpt = announcementToEdit?.poll?.options?.find { it.text == optText }
                                        PollOption(
                                            id = existingOpt?.id ?: UUID.randomUUID().toString(),
                                            text = optText,
                                            voterParentNames = existingOpt?.voterParentNames ?: emptyList()
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

@Composable
private fun ParentMessagesListView(
    students: List<Student>,
    chatMessages: List<ChatMessage>,
    isDarkTheme: Boolean,
    fontOffset: Int,
    onSelectStudent: (Student) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredStudents = students.filter { student ->
        searchQuery.isBlank() ||
                student.fullName.contains(searchQuery, ignoreCase = true) ||
                student.studentNumber.contains(searchQuery) ||
                (student.parentName?.contains(searchQuery, ignoreCase = true) == true)
    }.sortedBy { it.studentNumber.toIntOrNull() ?: Int.MAX_VALUE }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Arama Çubuğu
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Veli veya öğrenci adı / no ara...", fontSize = 12.sp) },
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

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredStudents, key = { it.id }) { student ->
                val studentMessages = chatMessages.filter { it.studentId == student.id }
                val lastMessage = studentMessages.maxByOrNull { it.timestamp }
                val unreadCount = studentMessages.count { !it.isFromTeacher && it.status != MessageStatus.READ }

                ParentChatRowItem(
                    student = student,
                    lastMessage = lastMessage,
                    unreadCount = unreadCount,
                    isDarkTheme = isDarkTheme,
                    fontOffset = fontOffset,
                    onClick = { onSelectStudent(student) }
                )
            }
        }
    }
}

@Composable
private fun ParentChatRowItem(
    student: Student,
    lastMessage: ChatMessage?,
    unreadCount: Int,
    isDarkTheme: Boolean,
    fontOffset: Int,
    onClick: () -> Unit
) {
    val isKiz = student.gender.equals("Kız", ignoreCase = true)
    val parentName = student.parentName ?: "${student.fullName.split(" ").first()} Velisi"

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (unreadCount > 0) {
            if (isDarkTheme) Color(0x300F2746) else Color(0xFFEFF6FF)
        } else {
            if (isDarkTheme) Color(0x250B162C) else Color.White
        },
        border = BorderStroke(
            1.dp,
            if (unreadCount > 0) {
                if (isDarkTheme) Color(0x6038BDF8) else Color(0xFFBFDBFE)
            } else {
                if (isDarkTheme) BirizDarkCardBorder else BirizBorder
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profil Rozeti (No & İsim Baş Harfi)
            Surface(
                shape = CircleShape,
                color = if (isDarkTheme) {
                    if (isKiz) Color(0x30DB2777) else Color(0x300284C7)
                } else {
                    if (isKiz) Color(0xFFFCE7F3) else Color(0xFFE0F2FE)
                },
                border = BorderStroke(1.dp, if (isKiz) Color(0xFFF472B6) else Color(0xFF38BDF8)),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "#${student.studentNumber}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isKiz) Color(0xFFDB2777) else Color(0xFF0284C7)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = parentName,
                        fontSize = (13 + (fontOffset / 2)).sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                    )

                    Text(
                        text = if (lastMessage != null) CommunicationPersistenceHelper.formatTime(lastMessage.timestamp) else "Yeni",
                        fontSize = (10 + (fontOffset / 2)).sp,
                        color = if (unreadCount > 0) BirizTurquoise else (if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted),
                        fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.Normal
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${student.fullName} Velisi",
                    fontSize = 10.5.sp,
                    color = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted
                )

                Spacer(modifier = Modifier.height(3.dp))

                // Son Mesaj Önizlemesi & Çift Tik
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (lastMessage != null && lastMessage.isFromTeacher) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = null,
                            tint = when (lastMessage.status) {
                                MessageStatus.READ -> Color(0xFF38BDF8) // Mavi çift tik
                                MessageStatus.DELIVERED -> Color.Gray
                                MessageStatus.SENT -> Color.LightGray
                            },
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                    }

                    Text(
                        text = lastMessage?.text ?: "Sohbeti başlatmak için dokunun...",
                        fontSize = (11.5 + fontOffset).sp,
                        color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (unreadCount > 0) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = CircleShape,
                    color = BirizAmber,
                    modifier = Modifier.size(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "$unreadCount",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            } else {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = if (isDarkTheme) Color.White.copy(alpha = 0.5f) else BirizBorder,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// =========================================================================
// 5. BİREYSEL SOHBET GÖRÜNÜMÜ (WHATSAPP STİLİ DİYALOG EKRANI)
// =========================================================================

@Composable
private fun OneOnOneChatView(
    student: Student,
    teacherName: String,
    messages: List<ChatMessage>,
    isDarkTheme: Boolean,
    fontOffset: Int,
    onFontOffsetChange: (Int) -> Unit,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onSimulateParentReply: (String) -> Unit
) {
    var messageInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val isKiz = student.gender.equals("Kız", ignoreCase = true)
    val parentName = student.parentName ?: "${student.fullName.split(" ").first()} Velisi"

    // Hazır Hızlı Yanıt Şablonları
    val quickTemplates = listOf(
        "Bilgi için teşekkür ederiz 👍",
        "Semih derste çok aktifti ⭐",
        "Ödev kontrolü tamamlandı 📚",
        "Yarın teneffüste görüşelim 🤝"
    )

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkTheme) Color(0xFF091427) else Color(0xFFEFEAE2))
    ) {
        // WhatsApp Tarzı Üst Başlık Barı
        Surface(
            color = if (isDarkTheme) BirizDarkSurfaceGlass else BirizNavyPrimary,
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Geri",
                        tint = Color.White
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = if (isKiz) Color(0xFFF472B6) else Color(0xFF38BDF8),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "#${student.studentNumber}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = parentName,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${student.fullName} Velisi • 🟢 Çevrimiçi",
                        fontSize = 10.sp,
                        color = BirizTurquoise
                    )
                }

                // Punto Boyutu Küçült (A⁻)
                Surface(
                    onClick = { if (fontOffset > -2) onFontOffsetChange(fontOffset - 2) },
                    enabled = fontOffset > -2,
                    shape = RoundedCornerShape(8.dp),
                    color = if (fontOffset > -2) Color(0x35FFFFFF) else Color(0x15FFFFFF),
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("chat_font_decrease")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "A⁻",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (fontOffset > -2) Color.White else Color.White.copy(alpha = 0.4f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Punto Boyutu Büyüt (A⁺)
                Surface(
                    onClick = { if (fontOffset < 6) onFontOffsetChange(fontOffset + 2) },
                    enabled = fontOffset < 6,
                    shape = RoundedCornerShape(8.dp),
                    color = if (fontOffset < 6) Color(0x35FFFFFF) else Color(0x15FFFFFF),
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("chat_font_increase")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "A⁺",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = if (fontOffset < 6) Color.White else Color.White.copy(alpha = 0.4f)
                        )
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
                            text = "🔒 Mesajlar BİRİZ Güvenli Eğitim Protokolü ile korunmaktadır.",
                            fontSize = 10.sp,
                            color = if (isDarkTheme) BirizDarkTextMuted else Color(0xFF92400E),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            items(messages, key = { it.id }) { msg ->
                ChatMessageBubble(
                    message = msg,
                    isDarkTheme = isDarkTheme,
                    fontOffset = fontOffset
                )
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

        // Mesaj Giriş Barı (WhatsApp Stili)
        Surface(
            color = if (isDarkTheme) BirizDarkSurfaceGlass else Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 60.dp) // Dock bar üstünde rahat kalır
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
                        .testTag("input_chat_message"),
                    maxLines = 4,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BirizTurquoise,
                        unfocusedBorderColor = if (isDarkTheme) BirizDarkCardBorder else BirizBorder
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                FloatingActionButton(
                    onClick = {
                        if (messageInput.isNotBlank()) {
                            onSendMessage(messageInput.trim())
                            messageInput = ""
                        }
                    },
                    shape = CircleShape,
                    containerColor = Color(0xFF00A884), // WhatsApp Yeşili
                    contentColor = Color.White,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("btn_send_chat_message")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Gönder",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    isDarkTheme: Boolean,
    fontOffset: Int
) {
    val isMe = message.isFromTeacher

    Row(
        modifier = Modifier.fillMaxWidth(),
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
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 280.dp + (fontOffset * 12).dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                if (!isMe) {
                    Text(
                        text = message.senderName,
                        fontSize = (10.5 + (fontOffset / 2)).sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF25D366)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }

                Text(
                    text = message.text,
                    fontSize = (12.5 + fontOffset).sp,
                    color = if (isDarkTheme) Color.White else Color(0xFF111B21),
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

                    if (isMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Okundu",
                            tint = when (message.status) {
                                MessageStatus.READ -> Color(0xFF53BDEB) // WhatsApp Mavi Çift Tik
                                MessageStatus.DELIVERED -> Color.Gray
                                MessageStatus.SENT -> Color.LightGray
                            },
                            modifier = Modifier.size(13.dp)
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
