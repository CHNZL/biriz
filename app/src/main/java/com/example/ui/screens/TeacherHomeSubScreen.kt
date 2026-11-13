package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Announcement
import com.example.data.model.Book
import com.example.data.model.ChatMessage
import com.example.data.model.ClassRoom
import com.example.data.model.GroupMakerState
import com.example.data.model.MessageStatus
import com.example.data.model.ReadingRecord
import com.example.data.model.ScheduleConfig
import com.example.data.model.ScheduleLesson
import com.example.data.model.SeatingPlanState
import com.example.data.model.Student
import com.example.data.model.Tournament
import com.example.data.model.TournamentStatus
import com.example.data.repository.StarRewardPersistenceHelper
import com.example.ui.theme.BirizAmber
import com.example.ui.theme.BirizDarkCardBorder
import com.example.ui.theme.BirizDarkTextMuted
import com.example.ui.theme.BirizDarkTextPrimary
import com.example.ui.theme.BirizDarkTextSecondary
import com.example.ui.theme.BirizNavyDark
import com.example.ui.theme.BirizNavyLight
import com.example.ui.theme.BirizNavyPrimary
import com.example.ui.theme.BirizTextMuted
import com.example.ui.theme.BirizTextPrimary
import com.example.ui.theme.BirizTextSecondary
import com.example.ui.theme.BirizTurquoise
import com.example.ui.viewmodel.DashboardSubPage
import com.example.util.LibraryPersistenceHelper
import com.example.util.ScheduleHelper
import com.example.util.TournamentPersistenceHelper
import kotlinx.coroutines.delay
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TeacherHomeSubScreen(
    classRoom: ClassRoom,
    students: List<Student>,
    scheduleConfig: ScheduleConfig,
    scheduleLessons: List<ScheduleLesson>,
    seatingPlanState: SeatingPlanState,
    groupMakerState: GroupMakerState,
    announcements: List<Announcement>,
    chatMessages: List<ChatMessage>,
    isDarkTheme: Boolean,
    onSelectSubPage: (DashboardSubPage) -> Unit,
    onOpenAddStudentDialog: () -> Unit
) {
    val context = LocalContext.current
    val classId = classRoom.id

    // Canlı saat (Veli aktiflik durumunu anlık değerlendirmek için)
    val nowMillis by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            delay(10_000L)
            value = System.currentTimeMillis()
        }
    }

    // Firestore Canlı Dinleyicileri: Yıldızlar, Kitaplık, Turnuva
    var allClassStars by remember(classId) {
        mutableStateOf(StarRewardPersistenceHelper.loadStarRecords(context, classId))
    }
    var classBooks by remember(classId) {
        mutableStateOf<List<Book>>(emptyList())
    }
    var readingRecords by remember(classId) {
        mutableStateOf<List<ReadingRecord>>(emptyList())
    }
    var classTournaments by remember(classId) {
        mutableStateOf<List<Tournament>>(emptyList())
    }

    LaunchedEffect(classId, students.size) {
        if (classId.isNotBlank() && classId != "default") {
            val libReport = LibraryPersistenceHelper.cleanupStaleAndOrphanedLibraryData(
                context = context,
                classId = classId,
                validStudents = students,
                resequenceNumbers = false
            )
            val tourneyCleaned = TournamentPersistenceHelper.cleanupStaleAndOrphanedTournamentData(
                context = context,
                classId = classId
            )
            val validStudentIds = students.map { it.id }.toSet()
            val starCleaned = StarRewardPersistenceHelper.cleanupStaleAndOrphanedStarRecords(
                context = context,
                classId = classId,
                validStudentIds = validStudentIds
            )
            if (libReport.hasChanges || tourneyCleaned > 0 || starCleaned > 0) {
                classBooks = LibraryPersistenceHelper.loadBooks(context, classId)
                readingRecords = LibraryPersistenceHelper.loadReadingRecords(context, classId)
                classTournaments = TournamentPersistenceHelper.loadTournaments(context, classId)
                allClassStars = StarRewardPersistenceHelper.loadStarRecords(context, classId)
            }
        }
    }

    DisposableEffect(classId) {
        classBooks = LibraryPersistenceHelper.loadBooks(context, classId)
        readingRecords = LibraryPersistenceHelper.loadReadingRecords(context, classId)
        classTournaments = TournamentPersistenceHelper.loadTournaments(context, classId)

        val starsReg = StarRewardPersistenceHelper.observeFirestoreStars(context, classId) { updated ->
            allClassStars = updated
        }
        val booksReg = LibraryPersistenceHelper.observeFirestoreBooks(context, classId) { updated ->
            classBooks = updated
        }
        val recordsReg = LibraryPersistenceHelper.observeFirestoreReadingRecords(context, classId) { updated ->
            readingRecords = updated
        }
        val tourneysReg = TournamentPersistenceHelper.observeFirestoreTournaments(context, classId) { updated ->
            classTournaments = updated
        }
        onDispose {
            starsReg?.remove()
            booksReg?.remove()
            recordsReg?.remove()
            tourneysReg?.remove()
        }
    }

    // Öğrenci & Veli İstatistikleri
    val girlsCount = remember(students) { students.count { it.gender.equals("Kız", ignoreCase = true) } }
    val boysCount = remember(students) { students.size - girlsCount }
    val linkedStudentsCount = remember(students) { students.count { it.isParentLinked } }
    val unlinkedStudentsCount = remember(students, linkedStudentsCount) { (students.size - linkedStudentsCount).coerceAtLeast(0) }
    val activeOnlineParentsCount = remember(students, nowMillis) {
        students.count { it.isParentCurrentlyActive(nowMillis) }
    }

    // Okunmamış Veli Mesajı Sayısı
    val unreadParentMessagesCount = remember(chatMessages) {
        chatMessages.count { !it.isFromTeacher && it.status != MessageStatus.READ }
    }

    // Yıldız İstatistikleri
    val totalClassStars = remember(allClassStars) { allClassStars.sumOf { it.starCount } }

    // Aktif Turnuva
    val activeTournament = remember(classTournaments) {
        classTournaments.firstOrNull { it.status == TournamentStatus.DEVAM_EDIYOR }
            ?: classTournaments.firstOrNull()
    }

    // Günün Ders Programı
    val todayCode = remember {
        when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "PZT"
            Calendar.TUESDAY -> "SAL"
            Calendar.WEDNESDAY -> "ÇAR"
            Calendar.THURSDAY -> "PER"
            Calendar.FRIDAY -> "CUM"
            else -> "PZT"
        }
    }
    val todayTitle = remember(todayCode) {
        when (todayCode) {
            "PZT" -> "Pazartesi"
            "SAL" -> "Salı"
            "ÇAR" -> "Çarşamba"
            "PER" -> "Perşembe"
            "CUM" -> "Cuma"
            else -> "Pazartesi"
        }
    }
    val todayLessons = remember(scheduleLessons, todayCode, scheduleConfig.dailyLessonCount) {
        scheduleLessons.filter {
            (it.dayOfWeek == todayCode || (todayCode == "ÇAR" && it.dayOfWeek == "CAR")) &&
                    it.lessonIndex <= scheduleConfig.dailyLessonCount
        }.sortedBy { it.lessonIndex }
    }

    val canvasBackground = if (isDarkTheme) Color(0xFF070E1B) else Color(0xFFEAF0F6)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(canvasBackground)
            .padding(horizontal = 14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- 1. HERO ÖĞRETMEN & SINIF ÖZET KARTI ---
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color.Transparent,
                shadowElevation = if (isDarkTheme) 0.dp else 8.dp,
                border = BorderStroke(
                    1.5.dp,
                    if (isDarkTheme) BirizTurquoise.copy(alpha = 0.5f) else Color(0x45FFFFFF)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("teacher_home_hero_card")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                if (isDarkTheme) {
                                    listOf(Color(0xFF0F2447), Color(0xFF09152B), Color(0xFF0B3B49))
                                } else {
                                    listOf(BirizNavyPrimary, BirizNavyLight, Color(0xFF0F4C5C))
                                }
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Üst Satır: Şube Rozeti & Canlı Aktif Veli Rozeti
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = BirizTurquoise.copy(alpha = 0.22f),
                                border = BorderStroke(1.dp, BirizTurquoise.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = "🏫 ${classRoom.gradeLevel}/${classRoom.section} Şubesi • ${classRoom.cityName}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF5EEAD4),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (activeOnlineParentsCount > 0) {
                                    Color(0x3510B981)
                                } else {
                                    Color.White.copy(alpha = 0.14f)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (activeOnlineParentsCount > 0) Color(0xFF34D399) else Color.White.copy(alpha = 0.28f)
                                )
                            ) {
                                Text(
                                    text = if (activeOnlineParentsCount > 0) {
                                        "● $activeOnlineParentsCount Veli Aktif"
                                    } else {
                                        "👨‍👩‍👧 $linkedStudentsCount/${students.size} Veli Bağlı"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeOnlineParentsCount > 0) Color(0xFF6EE7B7) else Color.White,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = classRoom.schoolName,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Sınıf Öğretmeni: ${classRoom.teacherName} • ${classRoom.districtName}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.84f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 3'lü Canlı İstatistik Kutusu
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TeacherHeroStatPill(
                                title = "Öğrenci",
                                value = "${students.size}",
                                subtitle = "$girlsCount Kız • $boysCount Erkek",
                                accentColor = Color(0xFF38BDF8),
                                modifier = Modifier.weight(1f),
                                onClick = { onSelectSubPage(DashboardSubPage.SINIF_LISTESI) }
                            )
                            TeacherHeroStatPill(
                                title = "Veli Bağlantısı",
                                value = "$linkedStudentsCount Bağlı",
                                subtitle = if (unlinkedStudentsCount > 0) "$unlinkedStudentsCount Bekleyen" else "Tamamı Bağlı ✓",
                                accentColor = Color(0xFF34D399),
                                modifier = Modifier.weight(1f),
                                onClick = { onSelectSubPage(DashboardSubPage.SINIF_LISTESI) }
                            )
                            TeacherHeroStatPill(
                                title = "İletişim",
                                value = if (unreadParentMessagesCount > 0) "$unreadParentMessagesCount Yeni" else "${announcements.size} Duyuru",
                                subtitle = if (unreadParentMessagesCount > 0) "Okunmamış Mesaj" else "Mesaj & Anket",
                                accentColor = if (unreadParentMessagesCount > 0) BirizAmber else Color(0xFFC084FC),
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (unreadParentMessagesCount > 0) {
                                        onSelectSubPage(DashboardSubPage.ILETISIM_MESAJLAR)
                                    } else {
                                        onSelectSubPage(DashboardSubPage.ILETISIM_DUYURULAR)
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Hızlı Eylem Butonları: Sınıf Listesi & Yeni Öğrenci Ekle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { onSelectSubPage(DashboardSubPage.SINIF_LISTESI) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BirizTurquoise,
                                    contentColor = BirizNavyDark
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("teacher_home_btn_class_list")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.People,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Sınıf Listesi (${students.size})",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            OutlinedButton(
                                onClick = onOpenAddStudentDialog,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.55f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.White.copy(alpha = 0.12f),
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("teacher_home_btn_add_student")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Öğrenci Ekle",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 2. GÜNÜN DERS PROGRAMI KARTI ---
        item {
            val scheduleAccent = Color(0xFF0284C7)
            DistinctTeacherCard(
                accentColor = scheduleAccent,
                isDark = isDarkTheme,
                onClick = { onSelectSubPage(DashboardSubPage.DERS_PROGRAMI) },
                testTag = "teacher_home_schedule_card"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = scheduleAccent.copy(alpha = if (isDarkTheme) 0.25f else 0.14f),
                            border = BorderStroke(1.dp, scheduleAccent.copy(alpha = 0.45f)),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = "📅", fontSize = 20.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Günün Ders Programı",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$todayTitle • İlk Ders ${scheduleConfig.firstLessonStartTime} (${scheduleConfig.dailyLessonCount} Ders)",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = scheduleAccent.copy(alpha = if (isDarkTheme) 0.28f else 0.14f)
                    ) {
                        Text(
                            text = "Program →",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDarkTheme) Color(0xFF7DD3FC) else scheduleAccent,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (todayLessons.isEmpty()) {
                    Text(
                        text = "Bugün için henüz ders tanımlanmamış. Dokunarak ders programını düzenleyin.",
                        fontSize = 12.5.sp,
                        color = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted
                    )
                } else {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        todayLessons.forEach { lesson ->
                            val (bgHex, _) = ScheduleHelper.getColorsForTheme(lesson.colorTheme)
                            val pillColor = try {
                                Color(android.graphics.Color.parseColor(bgHex))
                            } catch (_: Exception) {
                                BirizNavyPrimary
                            }
                            Surface(
                                shape = RoundedCornerShape(9.dp),
                                color = pillColor.copy(alpha = if (isDarkTheme) 0.28f else 0.14f),
                                border = BorderStroke(1.dp, pillColor.copy(alpha = 0.55f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = pillColor,
                                        modifier = Modifier.size(17.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${lesson.lessonIndex}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = lesson.subjectName,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkTheme) Color.White else BirizNavyPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 3. SINIF & DERS YÖNETİM MODÜLLERİ (2x3 BELİRGİN KUTULAR) ---
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TeacherSectionHeader(
                    title = "SINIF & DERS MODÜLLERİ",
                    isDark = isDarkTheme
                )

                // 1. Satır: Sınıf Listesi & Oturma Planı
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val classSubtitle = if (students.isEmpty()) {
                        "Henüz öğrenci yok"
                    } else {
                        "${students.size} Kayıtlı Öğrenci • $linkedStudentsCount Veli"
                    }
                    TeacherModuleBentoTile(
                        emoji = "👥",
                        title = "Sınıf Listesi",
                        subtitle = classSubtitle,
                        badgeText = if (activeOnlineParentsCount > 0) "● $activeOnlineParentsCount Aktif" else "Aç →",
                        accentColor = Color(0xFF0284C7),
                        isDark = isDarkTheme,
                        onClick = { onSelectSubPage(DashboardSubPage.SINIF_LISTESI) },
                        modifier = Modifier.weight(1f),
                        testTag = "teacher_bento_class_list"
                    )

                    val seatedCount = seatingPlanState.assignments.size
                    TeacherModuleBentoTile(
                        emoji = "🪑",
                        title = "Oturma Planı",
                        subtitle = "$seatedCount / ${students.size} Öğrenci Yerleşti",
                        badgeText = "Planla →",
                        accentColor = Color(0xFF0D9488),
                        isDark = isDarkTheme,
                        onClick = { onSelectSubPage(DashboardSubPage.OTURMA_PLANI) },
                        modifier = Modifier.weight(1f),
                        testTag = "teacher_bento_seating_plan"
                    )
                }

                // 2. Satır: Yıldızlar Sınıfı & Sınıf Kitaplığı
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TeacherModuleBentoTile(
                        emoji = "⭐",
                        title = "Yıldızlar Sınıfı",
                        subtitle = "$totalClassStars Toplam Yıldız ★",
                        badgeText = "Ödül →",
                        accentColor = Color(0xFFD97706),
                        isDark = isDarkTheme,
                        onClick = { onSelectSubPage(DashboardSubPage.YILDIZLAR_SINIFI) },
                        modifier = Modifier.weight(1f),
                        testTag = "teacher_bento_stars"
                    )

                    val activeLoansCount = classBooks.count { it.activeAssignments.isNotEmpty() }
                    TeacherModuleBentoTile(
                        emoji = "📚",
                        title = "Sınıf Kitaplığı",
                        subtitle = "${classBooks.size} Kitap • ${readingRecords.size} Okuma",
                        badgeText = if (activeLoansCount > 0) "$activeLoansCount Okunuyor" else "Aç →",
                        accentColor = Color(0xFF7C3AED),
                        isDark = isDarkTheme,
                        onClick = { onSelectSubPage(DashboardSubPage.KITAPLIK_LISTE) },
                        modifier = Modifier.weight(1f),
                        testTag = "teacher_bento_library"
                    )
                }

                // 3. Satır: Grup Oluşturma & Turnuvalar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TeacherModuleBentoTile(
                        emoji = "🎲",
                        title = "Grup Oluşturma",
                        subtitle = if (groupMakerState.groups.isNotEmpty()) {
                            "${groupMakerState.groups.size} Çalışma Grubu"
                        } else {
                            "Dengeli Takım Kur"
                        },
                        badgeText = "Gruplar →",
                        accentColor = Color(0xFFEA580C),
                        isDark = isDarkTheme,
                        onClick = { onSelectSubPage(DashboardSubPage.GRUP_OLUSTURMA) },
                        modifier = Modifier.weight(1f),
                        testTag = "teacher_bento_groups"
                    )

                    TeacherModuleBentoTile(
                        emoji = "🏆",
                        title = "Turnuvalar",
                        subtitle = activeTournament?.name ?: "${classTournaments.size} Turnuva Kaydı",
                        badgeText = "Fikstür →",
                        accentColor = Color(0xFFE11D48),
                        isDark = isDarkTheme,
                        onClick = { onSelectSubPage(DashboardSubPage.TURNUVA_DEVAM_EDENLER) },
                        modifier = Modifier.weight(1f),
                        testTag = "teacher_bento_tournaments"
                    )
                }
            }
        }

        // --- 4. DERS İÇİ HIZLI ARAÇLAR (ŞANSLI ÖĞRENCİ & ZAMANLAYICI) ---
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TeacherSectionHeader(
                    title = "HIZLI DERS ARAÇLARI",
                    isDark = isDarkTheme
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val luckyAccent = Color(0xFF059669)
                    DistinctTeacherCard(
                        accentColor = luckyAccent,
                        isDark = isDarkTheme,
                        onClick = { onSelectSubPage(DashboardSubPage.SANSLI_OGRENCI) },
                        testTag = "teacher_bento_lucky_student",
                        contentPadding = PaddingValues(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = luckyAccent.copy(alpha = if (isDarkTheme) 0.28f else 0.14f),
                                border = BorderStroke(1.dp, luckyAccent.copy(alpha = 0.45f)),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = "🎯", fontSize = 20.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Şanslı Öğrenci",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDarkTheme) Color.White else BirizNavyPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Adil Kura Çek",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    val timerAccent = Color(0xFF2563EB)
                    DistinctTeacherCard(
                        accentColor = timerAccent,
                        isDark = isDarkTheme,
                        onClick = { onSelectSubPage(DashboardSubPage.ZAMANLAYICI) },
                        testTag = "teacher_bento_timer",
                        contentPadding = PaddingValues(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = timerAccent.copy(alpha = if (isDarkTheme) 0.28f else 0.14f),
                                border = BorderStroke(1.dp, timerAccent.copy(alpha = 0.45f)),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = if (isDarkTheme) Color(0xFF93C5FD) else timerAccent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Zamanlayıcı",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDarkTheme) Color.White else BirizNavyPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Etkinlik Sayacı",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 5. VELİ İLETİŞİM & DUYURU MERKEZİ KARTI ---
        item {
            val commAccent = Color(0xFFDB2777)
            DistinctTeacherCard(
                accentColor = commAccent,
                isDark = isDarkTheme,
                testTag = "teacher_home_communication_card"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = commAccent.copy(alpha = if (isDarkTheme) 0.26f else 0.14f),
                            border = BorderStroke(1.dp, commAccent.copy(alpha = 0.45f)),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = "💬", fontSize = 20.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Veli İletişim & Duyurular",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${announcements.size} Duyuru • $unreadParentMessagesCount Okunmamış Mesaj",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                            )
                        }
                    }

                    if (unreadParentMessagesCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = BirizAmber
                        ) {
                            Text(
                                text = "$unreadParentMessagesCount Yeni",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                val latestAnnouncement = announcements.firstOrNull()
                if (latestAnnouncement != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDarkTheme) Color(0x450F172A) else Color.White.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else commAccent.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectSubPage(DashboardSubPage.ILETISIM_DUYURULAR) }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📢 ${latestAnnouncement.title}",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkTheme) Color.White else BirizNavyPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${latestAnnouncement.readReceipts.size} Okundu",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = latestAnnouncement.content,
                                fontSize = 12.sp,
                                color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onSelectSubPage(DashboardSubPage.ILETISIM_DUYURULAR) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                            contentColor = if (isDarkTheme) BirizNavyDark else Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Duyurular (${announcements.size})",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = { onSelectSubPage(DashboardSubPage.ILETISIM_MESAJLAR) },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(
                            1.2.dp,
                            if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 9.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChatBubble,
                            contentDescription = null,
                            tint = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Veli Mesajları",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                        )
                    }
                }
            }
        }

        // --- 6. ÖĞRETMEN ODASI & RESMİ EVRAK MERKEZİ ---
        item {
            val roomAccent = Color(0xFF4F46E5)
            DistinctTeacherCard(
                accentColor = roomAccent,
                isDark = isDarkTheme,
                testTag = "teacher_home_teacher_room_card"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = roomAccent.copy(alpha = if (isDarkTheme) 0.28f else 0.14f),
                        border = BorderStroke(1.dp, roomAccent.copy(alpha = 0.45f)),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = "☕", fontSize = 20.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Öğretmen Odası & Evraklar",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Tutanak, gezi planı ve hazır dilekçeler",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TeacherDocQuickChip("📋 Zümre Toplantısı", roomAccent, isDarkTheme) {
                        onSelectSubPage(DashboardSubPage.OGRETMEN_ODASI_ZUMRE)
                    }
                    TeacherDocQuickChip("👨‍👩‍👧 Veli Toplantısı", roomAccent, isDarkTheme) {
                        onSelectSubPage(DashboardSubPage.OGRETMEN_ODASI_VELI)
                    }
                    TeacherDocQuickChip("🎭 Sosyal Kulüp", roomAccent, isDarkTheme) {
                        onSelectSubPage(DashboardSubPage.OGRETMEN_ODASI_KULUP)
                    }
                    TeacherDocQuickChip("🚌 Okul Gezisi", roomAccent, isDarkTheme) {
                        onSelectSubPage(DashboardSubPage.OGRETMEN_ODASI_GEZI)
                    }
                    TeacherDocQuickChip("📄 Dilekçe & Formlar", roomAccent, isDarkTheme) {
                        onSelectSubPage(DashboardSubPage.OGRETMEN_ODASI_DILEKCE)
                    }
                }
            }
        }
    }
}

@Composable
private fun TeacherSectionHeader(
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
private fun DistinctTeacherCard(
    accentColor: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    testTag: String? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val cardBrush = if (isDark) {
        Brush.linearGradient(
            listOf(
                Color(0xFF132240),
                accentColor.copy(alpha = 0.14f)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color.White,
                accentColor.copy(alpha = 0.08f)
            )
        )
    }
    val borderColor = if (isDark) {
        accentColor.copy(alpha = 0.48f)
    } else {
        accentColor.copy(alpha = 0.36f)
    }

    val taggedModifier = if (testTag != null) modifier.testTag(testTag) else modifier

    Surface(
        modifier = taggedModifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 0.dp else 5.dp,
                shape = shape,
                ambientColor = accentColor.copy(alpha = 0.22f),
                spotColor = accentColor.copy(alpha = 0.28f)
            )
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(1.5.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBrush)
        ) {
            // Üstte ince vurgulu renk şeridi — her kartı anında birbirinden ayırır
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(accentColor.copy(alpha = if (isDark) 0.85f else 0.78f))
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(contentPadding)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun TeacherHeroStatPill(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.11f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.50f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text(
                text = title,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.82f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.86f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TeacherModuleBentoTile(
    emoji: String,
    title: String,
    subtitle: String,
    badgeText: String,
    accentColor: Color,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    val shape = RoundedCornerShape(20.dp)
    val tileBrush = if (isDark) {
        Brush.linearGradient(
            listOf(
                Color(0xFF132240),
                accentColor.copy(alpha = 0.16f)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color.White,
                accentColor.copy(alpha = 0.10f)
            )
        )
    }
    val borderColor = if (isDark) {
        accentColor.copy(alpha = 0.52f)
    } else {
        accentColor.copy(alpha = 0.38f)
    }

    val taggedModifier = if (testTag != null) modifier.testTag(testTag) else modifier

    Surface(
        modifier = taggedModifier
            .heightIn(min = 128.dp)
            .shadow(
                elevation = if (isDark) 0.dp else 5.dp,
                shape = shape,
                ambientColor = accentColor.copy(alpha = 0.22f),
                spotColor = accentColor.copy(alpha = 0.30f)
            )
            .clip(shape)
            .clickable { onClick() },
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(1.5.dp, borderColor)
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
                    .height(4.dp)
                    .background(accentColor.copy(alpha = if (isDark) 0.90f else 0.82f))
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = accentColor.copy(alpha = if (isDark) 0.28f else 0.14f),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = emoji, fontSize = 20.sp)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accentColor.copy(alpha = if (isDark) 0.28f else 0.14f)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color.White else accentColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 1. Büyük ve dikkat çekici başlık
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isDark) Color.White else BirizNavyPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                // 2. Mevcut bilgi / özet (tek temiz satır, 3. küçük satır yok)
                Text(
                    text = subtitle,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) BirizDarkTextSecondary else Color(0xFF475569),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TeacherDocQuickChip(
    label: String,
    accentColor: Color,
    isDark: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isDark) Color(0x451E293B) else Color.White,
        border = BorderStroke(1.dp, if (isDark) accentColor.copy(alpha = 0.45f) else accentColor.copy(alpha = 0.30f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else BirizNavyPrimary
            )
            Spacer(modifier = Modifier.width(5.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = if (isDark) BirizTurquoise else accentColor,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}
