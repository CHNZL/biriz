package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ActivityStarDraft
import com.example.data.model.ClassRoom
import com.example.data.model.StarCategory
import com.example.data.model.StarRewardRecord
import com.example.data.model.Student
import com.example.data.repository.StarRewardPersistenceHelper
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Yıldızlar Sınıfı Alt Ekranı (Star Rewards & Activity-Based Distribution)
 */
@Composable
fun StarsClassroomSubScreen(
    classRoom: ClassRoom,
    students: List<Student>,
    isDarkTheme: Boolean,
    onBackToClassList: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Geri tuşu kontrolü
    BackHandler {
        onBackToClassList()
    }

    // Kalıcı Yıldız Kayıtları (Yerel Hafıza + Firebase Firestore Canlı Senkronizasyon)
    var starRecords by remember {
        mutableStateOf(StarRewardPersistenceHelper.loadStarRecords(context, classRoom.id))
    }

    DisposableEffect(classRoom.id) {
        val listener = StarRewardPersistenceHelper.observeFirestoreStars(context, classRoom.id) { updated ->
            starRecords = updated
        }
        onDispose {
            listener?.remove()
        }
    }

    // Arama ve Filtreleme
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<String?>("ALL") } // ALL, ACADEMIC, VALUES
    var sortOrder by remember { mutableStateOf("STARS_DESC") } // STARS_DESC, NAME_ASC, NUMBER_ASC

    // Modal Durumları
    var studentForIndividualStar by remember { mutableStateOf<Student?>(null) }
    var studentForHistory by remember { mutableStateOf<Student?>(null) }
    var showActivityWizard by remember { mutableStateOf(false) }
    var showAllClassRankingDialog by remember { mutableStateOf(false) }

    // Son verilen işlem (Geri Al için Snackbar)
    var lastAwardedBatch by remember { mutableStateOf<Pair<String, List<StarRewardRecord>>?>(null) }

    // Öğrenci yıldız toplamları
    val studentStarMap = remember(starRecords) {
        students.associate { s ->
            s.id to starRecords.filter { it.studentId == s.id }.sumOf { it.starCount }
        }
    }
    val totalClassStars = remember(starRecords) { starRecords.sumOf { it.starCount } }

    // Sıralı ve Filtreli Öğrenci Listesi
    val filteredStudents = remember(students, searchQuery, studentStarMap, sortOrder) {
        students.filter { student ->
            if (searchQuery.isBlank()) true
            else {
                student.fullName.contains(searchQuery, ignoreCase = true) ||
                        student.studentNumber.contains(searchQuery, ignoreCase = true)
            }
        }.sortedWith { a, b ->
            when (sortOrder) {
                "STARS_DESC" -> (studentStarMap[b.id] ?: 0).compareTo(studentStarMap[a.id] ?: 0)
                "NAME_ASC" -> a.fullName.compareTo(b.fullName, ignoreCase = true)
                "NUMBER_ASC" -> (a.studentNumber.toIntOrNull() ?: 0).compareTo(b.studentNumber.toIntOrNull() ?: 0)
                else -> 0
            }
        }
    }

    // Lider Tablosu (İlk 3)
    val topStudents = remember(studentStarMap, students) {
        students
            .filter { (studentStarMap[it.id] ?: 0) > 0 }
            .sortedByDescending { studentStarMap[it.id] ?: 0 }
            .take(3)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 76.dp) // Alt dock payı
                .testTag("screen_stars_classroom")
        ) {
            // --- 1. ÜST KONTROL & ETKİNLİKLİ YILDIZ BARI ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isDarkTheme) Color(0x700E1A38) else Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Sol Bilgi Rozeti: Toplam Yıldız & Hızlı Sıralama Erişimi
                        Surface(
                            onClick = { showAllClassRankingDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDarkTheme) Color(0x35F59E0B) else Color(0xFFFEF3C7),
                            border = BorderStroke(1.dp, BirizAmber)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("⭐", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Sınıf Toplamı: $totalClassStars Yıldız",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDarkTheme) Color(0xFFFDE68A) else Color(0xFFB45309)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Leaderboard,
                                    contentDescription = "Sıralama",
                                    tint = if (isDarkTheme) Color(0xFFFDE68A) else Color(0xFFB45309),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }

                        // Sağ Ana Aksiyon: ETKİNLİKLİ YILDIZ BUTONU (Görseldeki gibi Mor Buton)
                        Button(
                            onClick = { showActivityWizard = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6366F1), // Canlı Mor renk
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("btn_activity_star_wizard")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ETKİNLİKLİ YILDIZ",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Arama & Sıralama Satırı (Metin kesilmesi düzeltilmiş, dikeyde kusursuz ortalanmış arama alanı)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Arama Çubuğu
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDarkTheme) Color(0x301E293B) else Color.White,
                            border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Öğrenci adı veya no ara...",
                                            fontSize = 12.5.sp,
                                            color = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted
                                        )
                                    }
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isDarkTheme) Color.White else BirizNavyPrimary
                                        ),
                                        cursorBrush = SolidColor(if (isDarkTheme) BirizTurquoise else BirizNavyPrimary),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchQuery = "" },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Temizle",
                                            modifier = Modifier.size(16.dp),
                                            tint = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else BirizTextMuted
                                        )
                                    }
                                }
                            }
                        }

                        // Sıralama Butonu
                        Surface(
                            onClick = {
                                sortOrder = when (sortOrder) {
                                    "STARS_DESC" -> "NAME_ASC"
                                    "NAME_ASC" -> "NUMBER_ASC"
                                    else -> "STARS_DESC"
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDarkTheme) Color(0x303B82F6) else Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, if (isDarkTheme) Color(0x503B82F6) else Color(0xFFBFDBFE)),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                Text(
                                    text = when (sortOrder) {
                                        "STARS_DESC" -> "⭐ Puana Göre"
                                        "NAME_ASC" -> "🔤 İsme Göre"
                                        else -> "🔢 Numaraya Göre"
                                    },
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkTheme) BirizTurquoise else Color(0xFF1D4ED8)
                                )
                            }
                        }
                    }
                }
            }

            // --- 2. PROFESYONEL LİDER KÜRSÜSÜ & TÜM SIRALAMA ERİŞİMİ ---
            if (searchQuery.isBlank()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDarkTheme) Color(0x280F172A) else Color(0xFFFAF5FF),
                    border = BorderStroke(1.dp, if (isDarkTheme) Color(0x40A855F7) else Color(0xFFE9D5FF)),
                    shadowElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Liderlik Kartı Başlığı & "Tüm Sıralama" Butonu
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = BirizAmber.copy(alpha = 0.18f),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.EmojiEvents,
                                            contentDescription = null,
                                            tint = BirizAmber,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Haftanın Yıldızları (Lider Kürsüsü)",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isDarkTheme) Color(0xFFF1F5F9) else BirizNavyPrimary
                                    )
                                    Text(
                                        text = "En çok yıldız toplayan şampiyonlar",
                                        fontSize = 10.sp,
                                        color = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted
                                    )
                                }
                            }

                            // Tüm Sınıf Sıralaması Butonu
                            Surface(
                                onClick = { showAllClassRankingDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDarkTheme) Color(0x356366F1) else Color(0xFFEEF2FF),
                                border = BorderStroke(1.dp, Color(0xFF818CF8)),
                                modifier = Modifier.testTag("btn_all_class_ranking_podium")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Leaderboard,
                                        contentDescription = null,
                                        tint = Color(0xFF6366F1),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Tüm Sıralama",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF6366F1)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (topStudents.isEmpty()) {
                            // Henüz yıldız verilmemişse
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDarkTheme) Color(0x201E293B) else Color.White,
                                border = BorderStroke(0.8.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("✨", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Sınıfınızda henüz yıldız verilmemiş. Öğrencilerinize dokunarak veya 'Etkinlikli Yıldız' ile ilk yıldızlarını dağıtın!",
                                        fontSize = 11.sp,
                                        color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                                    )
                                }
                            }
                        } else {
                            // 3'lü Olimpiyat Kürsüsü: Sol: 2. (Gümüş), Orta: 1. (Altın - Yüksek), Sağ: 3. (Bronz)
                            val first = topStudents.getOrNull(0)
                            val second = topStudents.getOrNull(1)
                            val third = topStudents.getOrNull(2)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                // 2. SIRA (Gümüş Kürsü - Sol)
                                if (second != null) {
                                    PodiumPillarItem(
                                        rank = 2,
                                        medalEmoji = "🥈",
                                        student = second,
                                        stars = studentStarMap[second.id] ?: 0,
                                        isDarkTheme = isDarkTheme,
                                        modifier = Modifier.weight(1f),
                                        onClick = { studentForIndividualStar = second }
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }

                                // 1. SIRA (Altın Kürsü - Orta / Yüksek & Taçlı)
                                if (first != null) {
                                    PodiumPillarItem(
                                        rank = 1,
                                        medalEmoji = "🥇",
                                        student = first,
                                        stars = studentStarMap[first.id] ?: 0,
                                        isDarkTheme = isDarkTheme,
                                        isCenterChampion = true,
                                        modifier = Modifier.weight(1.15f),
                                        onClick = { studentForIndividualStar = first }
                                    )
                                }

                                // 3. SIRA (Bronz Kürsü - Sağ)
                                if (third != null) {
                                    PodiumPillarItem(
                                        rank = 3,
                                        medalEmoji = "🥉",
                                        student = third,
                                        stars = studentStarMap[third.id] ?: 0,
                                        isDarkTheme = isDarkTheme,
                                        modifier = Modifier.weight(1f),
                                        onClick = { studentForIndividualStar = third }
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // --- 3. ÖĞRENCİ LİSTESİ VE YILDIZ KARTLARI ---
            if (filteredStudents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aranan kriterlere uygun öğrenci bulunamadı.",
                        fontSize = 13.sp,
                        color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredStudents, key = { it.id }) { student ->
                        val studentStars = studentStarMap[student.id] ?: 0
                        val studentRecords = remember(starRecords) {
                            starRecords.filter { it.studentId == student.id }
                        }

                        StudentStarCardItem(
                            student = student,
                            totalStars = studentStars,
                            records = studentRecords,
                            isDarkTheme = isDarkTheme,
                            onGiveStarClick = { studentForIndividualStar = student },
                            onHistoryClick = { studentForHistory = student }
                        )
                    }
                }
            }
        }

        // --- 4. GERİ AL (UNDO) BİLDİRİM BANTI ---
        lastAwardedBatch?.let { (message, batchRecords) ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFF1E293B),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 86.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text("✨", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = message,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = {
                                val recordIds = batchRecords.map { it.id }.toSet()
                                starRecords = StarRewardPersistenceHelper.deleteRecordsByIds(context, classRoom.id, recordIds)
                                Toast.makeText(context, "İşlem geri alındı ↩️", Toast.LENGTH_SHORT).show()
                                lastAwardedBatch = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BirizAmber),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Geri Al", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }

                        IconButton(
                            onClick = { lastAwardedBatch = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }

    // --- MODAL DİYALOGLAR ---

    // 1. Bireysel Yıldız Verme Modalı
    studentForIndividualStar?.let { student ->
        IndividualStarAwardDialog(
            student = student,
            isDarkTheme = isDarkTheme,
            onDismiss = { studentForIndividualStar = null },
            onAwardStar = { category, starCount, note ->
                val newRecord = StarRewardRecord(
                    id = UUID.randomUUID().toString(),
                    classId = classRoom.id,
                    studentId = student.id,
                    studentName = student.fullName,
                    studentNumber = student.studentNumber,
                    categoryId = category.id,
                    categoryName = category.name,
                    starCount = starCount,
                    note = note,
                    isLuckyBonus = false,
                    timestamp = System.currentTimeMillis()
                )
                starRecords = StarRewardPersistenceHelper.addStarRecord(context, classRoom.id, newRecord)
                lastAwardedBatch = Pair("${student.fullName}: +$starCount Yıldız (${category.name})", listOf(newRecord))
                studentForIndividualStar = null
                Toast.makeText(context, "${student.fullName} öğrencisine +$starCount yıldız verildi! ⭐", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 2. Öğrenci Yıldız Geçmişi & Düzenleme Modalı
    studentForHistory?.let { student ->
        val historyRecords = starRecords.filter { it.studentId == student.id }
        StudentStarHistoryDialog(
            student = student,
            records = historyRecords,
            isDarkTheme = isDarkTheme,
            onDismiss = { studentForHistory = null },
            onDeleteRecord = { recordId ->
                starRecords = StarRewardPersistenceHelper.deleteStarRecord(context, classRoom.id, recordId)
                Toast.makeText(context, "Yıldız kaydı silindi.", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 3. Etkinlikli Yıldız Dağıtımı Sihirbazı (2 Adımlı Akıllı Dağıtım)
    if (showActivityWizard) {
        ActivityStarWizardDialog(
            classRoom = classRoom,
            students = students,
            isDarkTheme = isDarkTheme,
            onDismiss = { showActivityWizard = false },
            onAwardBatch = { category, starCount, note, awardedStudents, luckyStudent ->
                val newRecords = mutableListOf<StarRewardRecord>()
                val timestamp = System.currentTimeMillis()

                awardedStudents.forEach { student ->
                    newRecords.add(
                        StarRewardRecord(
                            id = UUID.randomUUID().toString(),
                            classId = classRoom.id,
                            studentId = student.id,
                            studentName = student.fullName,
                            studentNumber = student.studentNumber,
                            categoryId = category.id,
                            categoryName = category.name,
                            starCount = starCount,
                            note = note,
                            isLuckyBonus = false,
                            timestamp = timestamp
                        )
                    )
                }

                // Şanslı Öğrenci Ödülü Eklendi mi?
                luckyStudent?.let { lucky ->
                    newRecords.add(
                        StarRewardRecord(
                            id = UUID.randomUUID().toString(),
                            classId = classRoom.id,
                            studentId = lucky.id,
                            studentName = lucky.fullName,
                            studentNumber = lucky.studentNumber,
                            categoryId = category.id,
                            categoryName = category.name,
                            starCount = starCount,
                            note = if (note.isBlank()) "Şanslı Öğrenci Ödülü 👑" else "$note (Şanslı Öğrenci Ödülü 👑)",
                            isLuckyBonus = true,
                            timestamp = timestamp + 1
                        )
                    )
                }

                starRecords = StarRewardPersistenceHelper.addStarRecords(context, classRoom.id, newRecords)
                val summaryText = "${awardedStudents.size} Öğrenciye +$starCount Yıldız Verildi (${category.name})"
                lastAwardedBatch = Pair(summaryText, newRecords)
                showActivityWizard = false
                Toast.makeText(context, "$summaryText başarıyla kaydedildi! 🎉", Toast.LENGTH_LONG).show()
            }
        )
    }

    // 4. Tüm Sınıf Yıldız Sıralaması Penceresi (Öğretmenin sınıfın tüm sıralamasını ve öğrencilerin kaç yıldızı olduğunu anında görmesi için)
    if (showAllClassRankingDialog) {
        AllClassStarRankingDialog(
            students = students,
            studentStarMap = studentStarMap,
            totalClassStars = totalClassStars,
            isDarkTheme = isDarkTheme,
            onDismiss = { showAllClassRankingDialog = false },
            onSelectStudent = { student ->
                studentForIndividualStar = student
            }
        )
    }
}

/**
 * Profesyonel Olimpiyat Kürsüsü Sütunu (1., 2., 3. Sıra)
 */
@Composable
private fun PodiumPillarItem(
    rank: Int,
    medalEmoji: String,
    student: Student,
    stars: Int,
    isDarkTheme: Boolean,
    isCenterChampion: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isGirl = student.gender.equals("Kız", ignoreCase = true)
    val ringColor = when (rank) {
        1 -> BirizAmber
        2 -> Color(0xFF94A3B8)
        else -> Color(0xFFD97706)
    }
    val cardBg = when (rank) {
        1 -> if (isDarkTheme) Color(0x35F59E0B) else Color(0xFFFFFBEB)
        2 -> if (isDarkTheme) Color(0x2594A3B8) else Color(0xFFF8FAFC)
        else -> if (isDarkTheme) Color(0x25D97706) else Color(0xFFFFF7ED)
    }
    val cardBorder = when (rank) {
        1 -> BirizAmber
        2 -> Color(0xFF94A3B8)
        else -> Color(0xFFFB923C)
    }
    val pillarHeight = if (isCenterChampion) 124.dp else 108.dp

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        border = BorderStroke(if (isCenterChampion) 1.5.dp else 1.dp, cardBorder),
        shadowElevation = if (isCenterChampion) 4.dp else 1.dp,
        modifier = modifier.height(pillarHeight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Üst Rozet: Taç / Madalya
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isCenterChampion) {
                    Text("👑", fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(2.dp))
                }
                Text(
                    text = "$medalEmoji $rank.",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = when (rank) {
                        1 -> if (isDarkTheme) Color(0xFFFDE68A) else Color(0xFFB45309)
                        2 -> if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF475569)
                        else -> if (isDarkTheme) Color(0xFFFED7AA) else Color(0xFFC2410C)
                    }
                )
            }

            // Avatar / Baş Harf Çemberi
            Surface(
                shape = CircleShape,
                color = if (isGirl) Color(0xFFFDF2F8) else Color(0xFFEFF6FF),
                border = BorderStroke(1.5.dp, ringColor),
                modifier = Modifier.size(if (isCenterChampion) 34.dp else 28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = student.fullName.take(2).uppercase(),
                        fontSize = if (isCenterChampion) 11.sp else 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isGirl) Color(0xFFDB2777) else Color(0xFF2563EB)
                    )
                }
            }

            // Öğrenci İsmi
            Text(
                text = student.fullName.split(" ").firstOrNull() ?: student.fullName,
                fontSize = if (isCenterChampion) 11.5.sp else 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDarkTheme) Color.White else BirizNavyPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            // Yıldız Kutucuğu
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (rank == 1) BirizAmber else if (isDarkTheme) Color(0x351E293B) else Color.White,
                border = BorderStroke(0.8.dp, cardBorder)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "$stars ⭐",
                        fontSize = if (isCenterChampion) 11.sp else 10.sp,
                        fontWeight = FontWeight.Black,
                        color = if (rank == 1) Color.Black else if (isDarkTheme) Color(0xFFFDE68A) else Color(0xFFB45309)
                    )
                }
            }
        }
    }
}

/**
 * Tüm Sınıf Yıldız Sıralaması Penceresi (Leaderboard Dialog)
 */
@Composable
fun AllClassStarRankingDialog(
    students: List<Student>,
    studentStarMap: Map<String, Int>,
    totalClassStars: Int,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onSelectStudent: (Student) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    // Tüm sınıf sıralaması: yıldız sayısına göre azalan, eşitlikte numara
    val rankedStudents = remember(students, studentStarMap) {
        students.sortedWith { a, b ->
            val starA = studentStarMap[a.id] ?: 0
            val starB = studentStarMap[b.id] ?: 0
            if (starA != starB) {
                starB.compareTo(starA)
            } else {
                (a.studentNumber.toIntOrNull() ?: 0).compareTo(b.studentNumber.toIntOrNull() ?: 0)
            }
        }
    }

    // Arama filtreli liste
    val displayList = remember(rankedStudents, searchQuery) {
        if (searchQuery.isBlank()) rankedStudents
        else {
            rankedStudents.filter { s ->
                s.fullName.contains(searchQuery, ignoreCase = true) ||
                s.studentNumber.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val classAverage = remember(students, totalClassStars) {
        if (students.isNotEmpty()) {
            val avg = totalClassStars.toFloat() / students.size
            String.format(java.util.Locale.US, "%.1f", avg)
        } else "0"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = if (isDarkTheme) Color(0xFF0F172A) else Color.White,
            border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .testTag("dialog_all_class_ranking")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Başlık Satırı
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = BirizAmber.copy(alpha = 0.18f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Leaderboard,
                                    contentDescription = null,
                                    tint = BirizAmber,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Sınıf Yıldız Sıralaması",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isDarkTheme) Color(0xFFF1F5F9) else BirizNavyPrimary
                            )
                            Text(
                                text = "${students.size} Öğrenci • Toplam $totalClassStars ⭐ • Ort: $classAverage ⭐",
                                fontSize = 11.sp,
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

                Spacer(modifier = Modifier.height(12.dp))

                // Hızlı Arama Çubuğu (Öğrenci sorunca anında bulabilmek için)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDarkTheme) Color(0x301E293B) else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Sıralamada öğrenci ara...",
                                    fontSize = 12.sp,
                                    color = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDarkTheme) Color.White else BirizNavyPrimary
                                ),
                                cursorBrush = SolidColor(if (isDarkTheme) BirizTurquoise else BirizNavyPrimary),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Temizle", modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Sıralı Liste
                if (displayList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Aramaya uygun öğrenci bulunamadı.",
                            fontSize = 12.5.sp,
                            color = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(displayList.size) { index ->
                            val student = displayList[index]
                            val actualRank = rankedStudents.indexOfFirst { it.id == student.id } + 1
                            val stars = studentStarMap[student.id] ?: 0

                            Surface(
                                onClick = {
                                    onSelectStudent(student)
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = when (actualRank) {
                                    1 -> if (isDarkTheme) Color(0x35F59E0B) else Color(0xFFFFFBEB)
                                    2 -> if (isDarkTheme) Color(0x2594A3B8) else Color(0xFFF8FAFC)
                                    3 -> if (isDarkTheme) Color(0x30D97706) else Color(0xFFFFF7ED)
                                    else -> if (isDarkTheme) Color(0x201E293B) else Color.White
                                },
                                border = BorderStroke(
                                    1.dp,
                                    when (actualRank) {
                                        1 -> BirizAmber
                                        2 -> Color(0xFF94A3B8)
                                        3 -> Color(0xFFFB923C)
                                        else -> if (isDarkTheme) BirizDarkCardBorder else BirizBorder
                                    }
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Sıralama Rozeti
                                    Box(
                                        modifier = Modifier.size(30.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        when (actualRank) {
                                            1 -> Text("🥇", fontSize = 18.sp)
                                            2 -> Text("🥈", fontSize = 18.sp)
                                            3 -> Text("🥉", fontSize = 18.sp)
                                            else -> {
                                                Surface(
                                                    shape = CircleShape,
                                                    color = if (isDarkTheme) Color(0x35334E68) else Color(0xFFE2E8F0),
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(
                                                            text = "$actualRank",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF475569)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    // Cinsiyet / Baş Harf Çemberi
                                    val isGirl = student.gender.equals("Kız", ignoreCase = true)
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isGirl) Color(0xFFFDF2F8) else Color(0xFFEFF6FF),
                                        border = BorderStroke(1.dp, if (isGirl) Color(0xFFF472B6) else Color(0xFF60A5FA)),
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = student.fullName.take(2).uppercase(),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isGirl) Color(0xFFDB2777) else Color(0xFF2563EB)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    // İsim ve Numara
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = student.fullName,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "#${student.studentNumber}",
                                            fontSize = 10.5.sp,
                                            color = if (isGirl) Color(0xFFDB2777) else Color(0xFF2563EB),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    // Yıldız Rozeti
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (stars > 0) {
                                            if (isDarkTheme) Color(0x35F59E0B) else Color(0xFFFEF3C7)
                                        } else {
                                            if (isDarkTheme) Color(0x201E293B) else Color(0xFFF1F5F9)
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            if (stars > 0) BirizAmber else if (isDarkTheme) BirizDarkCardBorder else Color(0xFFCBD5E1)
                                        )
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("⭐", fontSize = 12.sp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "$stars Yıldız",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (stars > 0) {
                                                    if (isDarkTheme) Color(0xFFFDE68A) else Color(0xFFB45309)
                                                } else {
                                                    if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Alt Kapat Butonu
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDarkTheme) Color(0xFF334155) else Color(0xFFF1F5F9),
                        contentColor = if (isDarkTheme) Color.White else BirizNavyPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                    Text("Kapat", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Öğrenci Yıldız Kartı
 */
@Composable
private fun StudentStarCardItem(
    student: Student,
    totalStars: Int,
    records: List<StarRewardRecord>,
    isDarkTheme: Boolean,
    onGiveStarClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
    val isKiz = student.gender.equals("Kız", ignoreCase = true)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isDarkTheme) Color(0x3513233F) else Color.White,
        border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Numarası / Profil Rozeti
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

            // İsim ve Yıldız Detayı
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = student.fullName,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Son Kazanılan Rozetler / Rozet Etiketleri
                if (records.isNotEmpty()) {
                    val recentCategories = records.take(2).map { it.categoryName }.distinct()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        recentCategories.forEach { catName ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isDarkTheme) Color(0x2564748B) else Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = catName,
                                    fontSize = 9.5.sp,
                                    color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Henüz yıldız kazanmadı",
                        fontSize = 10.sp,
                        color = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Toplam Yıldız Sayısı Rozeti
            Surface(
                onClick = onHistoryClick,
                shape = RoundedCornerShape(10.dp),
                color = if (totalStars > 0) {
                    if (isDarkTheme) Color(0x35F59E0B) else Color(0xFFFEF3C7)
                } else {
                    if (isDarkTheme) Color(0x2064748B) else Color(0xFFF1F5F9)
                },
                border = BorderStroke(1.dp, if (totalStars > 0) BirizAmber else Color(0xFFCBD5E1)),
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text("⭐", fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$totalStars",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = if (totalStars > 0) {
                            if (isDarkTheme) Color(0xFFFDE68A) else Color(0xFFB45309)
                        } else {
                            if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
                        }
                    )
                }
            }

            // Hızlı Yıldız Ekle Butonu
            Button(
                onClick = onGiveStarClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                    contentColor = if (isDarkTheme) BirizNavyDark else Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text("Yıldız Ver", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Kompakt Seçili Kategori Kartı (Tıklanınca 3 Sütunlu Açılır Kapanır Pencereyi Açar)
 */
@Composable
private fun SelectedCategorySummaryCard(
    category: StarCategory,
    isDarkTheme: Boolean,
    onOpenPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val catColor = Color(category.colorHex)

    Surface(
        onClick = onOpenPicker,
        shape = RoundedCornerShape(14.dp),
        color = if (isDarkTheme) Color(0x351E293B) else Color(0xFFF8FAFC),
        border = BorderStroke(1.5.dp, catColor.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
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
                    shape = RoundedCornerShape(10.dp),
                    color = catColor.copy(alpha = 0.16f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = null,
                            tint = catColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = category.name,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) Color.White else BirizNavyPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (category.isAcademic) "Ders / Akademik Başarı" else "Örnek Davranış & Değerler",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = catColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = catColor.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, catColor.copy(alpha = 0.4f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Kategori Değiştir ▾",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = catColor
                    )
                }
            }
        }
    }
}

/**
 * 3 Sütunlu Kutu/Kare Formatında Açılır Kapanır Kategori Seçim Penceresi
 */
@Composable
private fun CategoryGridPickerDialog(
    selectedCategoryId: String,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onCategorySelected: (StarCategory) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, ACADEMIC, VALUES

    val filteredCategories = remember(selectedFilter) {
        when (selectedFilter) {
            "ACADEMIC" -> StarCategory.ALL_CATEGORIES.filter { it.isAcademic }
            "VALUES" -> StarCategory.ALL_CATEGORIES.filter { !it.isAcademic }
            else -> StarCategory.ALL_CATEGORIES
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = if (isDarkTheme) Color(0xFF0F172A) else Color.White,
            border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
            shadowElevation = 14.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.82f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Başlık & Kapat Butonu
                Surface(
                    color = if (isDarkTheme) Color(0x351E293B) else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFEEF2FF),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFF6366F1),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Yıldız Kategorisi Seçin",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDarkTheme) Color.White else BirizNavyPrimary
                                )
                                Text(
                                    text = "Kategori kutusuna dokunarak hemen seçebilirsiniz",
                                    fontSize = 11.sp,
                                    color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                                )
                            }
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Kapat")
                        }
                    }
                }

                // Filtreleme Sekmeleri: [Tümü (17)] [Dersler (9)] [Değerler (8)]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabs = listOf(
                        "ALL" to "Tümü (17)",
                        "ACADEMIC" to "Dersler (9)",
                        "VALUES" to "Değerler (8)"
                    )
                    tabs.forEach { (key, label) ->
                        val isSel = selectedFilter == key
                        Surface(
                            onClick = { selectedFilter = key },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) Color(0xFF6366F1) else (if (isDarkTheme) Color(0x301E293B) else Color(0xFFF1F5F9)),
                            border = BorderStroke(
                                1.dp,
                                if (isSel) Color(0xFF6366F1) else (if (isDarkTheme) Color(0x3064748B) else Color(0xFFE2E8F0))
                            )
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) Color.White else (if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // 3 Sütunlu Kutu/Kare Grid Düzeni (Oturma Planı Akıllı Dağıtım Formatında)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 14.dp)
                ) {
                    items(filteredCategories, key = { it.id }) { cat ->
                        val isSelected = cat.id == selectedCategoryId
                        val catColor = Color(cat.colorHex)

                        Surface(
                            onClick = {
                                onCategorySelected(cat)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) {
                                catColor.copy(alpha = 0.16f)
                            } else {
                                if (isDarkTheme) Color(0x301E293B) else Color(0xFFF8FAFC)
                            },
                            border = BorderStroke(
                                if (isSelected) 2.dp else 1.dp,
                                if (isSelected) catColor else (if (isDarkTheme) Color(0x4064748B) else Color(0xFFE2E8F0))
                            ),
                            shadowElevation = if (isSelected) 3.dp else 0.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(82.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 6.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = catColor.copy(alpha = 0.18f),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = cat.icon,
                                                contentDescription = null,
                                                tint = catColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = cat.name,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                        color = if (isSelected) catColor else (if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary),
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 13.sp
                                    )
                                }

                                if (isSelected) {
                                    Surface(
                                        shape = CircleShape,
                                        color = catColor,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(5.dp)
                                            .size(16.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(11.dp)
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

/**
 * Özel Tasarımlı, Gamified ve Çok Şık Yıldız Sayısı Seçici Kutusu
 */
@Composable
private fun StarRewardSelectorCard(
    starCount: Int,
    onStarCountChange: (Int) -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isDarkTheme) Color(0x3578350F) else Color(0xFFFFFBEB),
        border = BorderStroke(1.5.dp, if (isDarkTheme) Color(0xFFD97706).copy(alpha = 0.5f) else Color(0xFFFDE68A)),
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Üst Satır: İkon Rozeti + Büyük Miktar + Stepper Butonları
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Sol: Parlayan Yıldız & Miktar
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFEF3C7),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("⭐", fontSize = 20.sp)
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "+$starCount YILDIZ",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFD97706),
                            letterSpacing = 0.3.sp
                        )
                        Text(
                            text = "Kişi başı verilecek ödül miktarı",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF64748B)
                        )
                    }
                }

                // Sağ: Stepper (- / +)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        onClick = { if (starCount > 1) onStarCountChange(starCount - 1) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (starCount > 1) {
                            if (isDarkTheme) Color(0x4064748B) else Color(0xFFF1F5F9)
                        } else {
                            if (isDarkTheme) Color(0x1564748B) else Color(0xFFF8FAFC)
                        },
                        border = BorderStroke(1.dp, if (isDarkTheme) Color(0x4064748B) else Color(0xFFCBD5E1)),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Azalt",
                                tint = if (starCount > 1) Color(0xFFD97706) else Color.Gray.copy(alpha = 0.4f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Surface(
                        onClick = { if (starCount < 25) onStarCountChange(starCount + 1) },
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF59E0B),
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Arttır",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Alt Satır: Hızlı Önayarlar (+1, +2, +3, +5, +10)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(1, 2, 3, 5, 10).forEach { amount ->
                    val isSel = starCount == amount
                    Surface(
                        onClick = { onStarCountChange(amount) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSel) Color(0xFFF59E0B) else (if (isDarkTheme) Color(0x25D97706) else Color.White),
                        border = BorderStroke(
                            if (isSel) 1.5.dp else 1.dp,
                            if (isSel) Color(0xFFD97706) else (if (isDarkTheme) Color(0x40D97706) else Color(0xFFFDE68A))
                        ),
                        shadowElevation = if (isSel) 2.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .height(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "⭐ +$amount",
                                fontSize = 11.5.sp,
                                fontWeight = if (isSel) FontWeight.Black else FontWeight.Bold,
                                color = if (isSel) Color.White else Color(0xFFB45309)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Bireysel Yıldız Verme Modalı
 * Sırası ile:
 * 1. Önce kategori seçimi (Açılır kapanır 3 sütunlu pencere)
 * 2. Sonra dilersek açıklama yazma (isteğe bağlı)
 * 3. Sonra kaç yıldız vereceğimizi belirleme
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IndividualStarAwardDialog(
    student: Student,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onAwardStar: (StarCategory, Int, String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(StarCategory.ALL_CATEGORIES.first()) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf("") }
    var starCount by remember { mutableIntStateOf(1) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isDarkTheme) BirizDarkSurfaceGlass else Color.White,
            border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
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
                            color = Color(0xFFFEF3C7),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("⭐", fontSize = 18.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Yıldız Ver",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isDarkTheme) Color.White else Color(0xFF0F172A)
                            )
                            Text(
                                text = "#${student.studentNumber} ${student.fullName}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) BirizTurquoise else Color(0xFF0284C7)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // ADIM 1: KATEGORİ SEÇİN (Açılır Kapanır 3 Sütunlu Pencere Butonu)
                    Text(
                        text = "1. KATEGORİ SEÇİN:",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDarkTheme) Color(0xFFF1F5F9) else Color(0xFF0F172A),
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SelectedCategorySummaryCard(
                        category = selectedCategory,
                        isDarkTheme = isDarkTheme,
                        onOpenPicker = { showCategoryPicker = true }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // ADIM 2: AÇIKLAMA / BAŞARI NOTU (İSTEĞE BAĞLI)
                    Text(
                        text = "2. AÇIKLAMA / BAŞARI NOTU (İSTEĞE BAĞLI):",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDarkTheme) Color(0xFFF1F5F9) else Color(0xFF0F172A),
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        placeholder = { Text("Örn: Hafta boyunca örnek davranış sergiledi...", fontSize = 12.5.sp, color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)) },
                        shape = RoundedCornerShape(10.dp),
                        textStyle = TextStyle(fontSize = 13.sp, color = if (isDarkTheme) Color.White else BirizNavyPrimary),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                            unfocusedBorderColor = if (isDarkTheme) BirizDarkCardBorder else BirizBorder
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // ADIM 3: VERİLECEK YILDIZ MİKTARI
                    Text(
                        text = "3. VERİLECEK YILDIZ MİKTARI:",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDarkTheme) Color(0xFFF1F5F9) else Color(0xFF0F172A),
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    StarRewardSelectorCard(
                        starCount = starCount,
                        onStarCountChange = { starCount = it },
                        isDarkTheme = isDarkTheme
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Kaydet Butonları
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text("Vazgeç")
                    }

                    Button(
                        onClick = { onAwardStar(selectedCategory, starCount, noteText.trim()) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6366F1),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(44.dp)
                    ) {
                        Text("⭐ +$starCount Yıldız Ver", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showCategoryPicker) {
        CategoryGridPickerDialog(
            selectedCategoryId = selectedCategory.id,
            isDarkTheme = isDarkTheme,
            onDismiss = { showCategoryPicker = false },
            onCategorySelected = {
                selectedCategory = it
            }
        )
    }
}

/**
 * Etkinlikli Yıldız Dağıtım Sihirbazı Modalı
 * Adım 1: Kazanım & Kurallar (Kategori, Açıklama, Yıldız Miktarı, Süre Limiti, Hedef Kişi Limiti)
 * Adım 2: Öğrenci Seçimi (Hızlı Filtreler, Süre Sayacı, Hedef Sayıya Ulaşınca Otomatik Dağıtım)
 * Adım 3: Şanslı Öğrenci Seçilsin Mi? -> Doğru / Yanlış Sorusu ile Ödül Verme
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActivityStarWizardDialog(
    classRoom: ClassRoom,
    students: List<Student>,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onAwardBatch: (StarCategory, Int, String, List<Student>, Student?) -> Unit
) {
    // Adım: 1 = Kazanım & Kurallar, 2 = Öğrenci Seçimi
    var currentStep by remember { mutableIntStateOf(1) }

    // Adım 1 Konfigürasyonu
    var selectedCategory by remember { mutableStateOf(StarCategory.ALL_CATEGORIES.first()) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var activityNote by remember { mutableStateOf("") }
    var starCount by remember { mutableIntStateOf(1) }
    var timeLimitSeconds by remember { mutableIntStateOf(0) } // 0 = Süresiz
    var targetStudentLimit by remember { mutableIntStateOf(0) } // 0 = Sınırsız

    // Adım 2 Seçilen Öğrenciler
    val selectedStudentIds = remember { mutableStateListOf<String>() }
    var studentSearch by remember { mutableStateOf("") }

    // Süre Sayacı Durumu (Adım 2)
    var remainingSeconds by remember(timeLimitSeconds) { mutableIntStateOf(timeLimitSeconds) }
    var isTimerRunning by remember { mutableStateOf(timeLimitSeconds > 0) }

    // Şanslı Öğrenci Modalları (Adım 3)
    var showLuckyPromptDialog by remember { mutableStateOf(false) }
    var selectedLuckyStudent by remember { mutableStateOf<Student?>(null) }
    var showLuckyQuestionDialog by remember { mutableStateOf(false) }

    // Süre Geri Sayımı Efekti
    LaunchedEffect(isTimerRunning, remainingSeconds) {
        if (isTimerRunning && remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds--
            if (remainingSeconds == 0) {
                isTimerRunning = false
                // Süre bittiğinde seçili öğrenciler varsa otomatik yıldız ver
                if (selectedStudentIds.isNotEmpty()) {
                    showLuckyPromptDialog = true
                }
            }
        }
    }

    // Hedef Kişi Sayısına Ulaşılınca Otomatik Yıldız Ver
    fun checkTargetPersonLimitAndProceed() {
        if (targetStudentLimit > 0 && selectedStudentIds.size >= targetStudentLimit) {
            showLuckyPromptDialog = true
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isDarkTheme) Color(0xFF0F172A) else Color.White,
            border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.90f)
                .padding(vertical = 4.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // --- ÜST BAR (Adım Göstergesi & Kapat Butonu) ---
                Surface(
                    color = if (isDarkTheme) Color(0x351E293B) else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFEEF2FF),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFF6366F1),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "ETKİNLİKLİ YILDIZ DAĞITIMI",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isDarkTheme) Color.White else Color(0xFF0F172A),
                                        letterSpacing = 0.3.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF6366F1)
                                    ) {
                                        Text(
                                            text = if (currentStep == 1) "Adım 1/2: Kazanım & Kurallar" else "Adım 2/2: Öğrenci Seçimi",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (currentStep == 1) "Kazanım & Kurallar: Kategori, açıklama, süre ve limit belirleyin"
                                    else "Öğrenci Seçimi (${students.size} Öğrenci) - Tek dokunuşla seçebilirsiniz",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF475569),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Kapat", tint = if (isDarkTheme) Color.White else Color(0xFF0F172A), modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // --- ADIM 1: KAZANIM VE KURALLAR ---
                if (currentStep == 1) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        // 1. KATEGORİ SEÇİN
                        Text(
                            text = "YILDIZ KATEGORİSİ",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isDarkTheme) Color(0xFFF1F5F9) else Color(0xFF0F172A),
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        SelectedCategorySummaryCard(
                            category = selectedCategory,
                            isDarkTheme = isDarkTheme,
                            onOpenPicker = { showCategoryPicker = true }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // 2. ETKİNLİK / KAZANIM AÇIKLAMASI
                        Text(
                            text = "ETKİNLİK / KAZANIM AÇIKLAMASI (İSTEĞE BAĞLI)",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isDarkTheme) Color(0xFFF1F5F9) else Color(0xFF0F172A),
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = activityNote,
                            onValueChange = { activityNote = it },
                            placeholder = { Text("Örn: Ders içi aktif katılım, Matematik yarışması...", fontSize = 12.5.sp, color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            textStyle = TextStyle(fontSize = 13.sp, color = if (isDarkTheme) Color.White else BirizNavyPrimary),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isDarkTheme) BirizTurquoise else Color(0xFF6366F1),
                                unfocusedBorderColor = if (isDarkTheme) BirizDarkCardBorder else BirizBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // 3. VERİLECEK YILDIZ MİKTARI (ÖZEL TASARIMLI GÖZ ALICI KUTU)
                        Text(
                            text = "VERİLECEK YILDIZ MİKTARI",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isDarkTheme) Color(0xFFF1F5F9) else Color(0xFF0F172A),
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        StarRewardSelectorCard(
                            starCount = starCount,
                            onStarCountChange = { starCount = it },
                            isDarkTheme = isDarkTheme
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // 4. EŞİT ÖLÇÜLERDE VE SİMETRİK KUTUCUKLAR: ZAMAN SINIRI & HEDEF KİŞİ SINIRI
                        Text(
                            text = "SÜRE VE HEDEF KİŞİ SINIRI (İSTEĞE BAĞLI)",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isDarkTheme) Color(0xFFF1F5F9) else Color(0xFF0F172A),
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // --- 1. ZAMAN SINIRI KUTUSU ---
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isDarkTheme) Color(0x351E293B) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFFCBD5E1)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(116.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Başlık & Rozet
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "⏱ Süre Sınırı",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (isDarkTheme) Color.White else Color(0xFF0F172A)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (timeLimitSeconds > 0) Color(0xFFFEF3C7) else (if (isDarkTheme) Color(0x301E293B) else Color(0xFFF1F5F9))
                                        ) {
                                            Text(
                                                text = if (timeLimitSeconds == 0) "Süresiz" else "${timeLimitSeconds}s",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (timeLimitSeconds > 0) Color(0xFFB45309) else Color(0xFF64748B),
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    // Stepper: [-10s]  XX sn  [+10s]
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            onClick = { if (timeLimitSeconds >= 10) timeLimitSeconds -= 10 },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isDarkTheme) Color(0x3064748B) else Color(0xFFE2E8F0),
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("-10s", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Text(
                                            text = "$timeLimitSeconds sn",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (timeLimitSeconds > 0) Color(0xFFD97706) else (if (isDarkTheme) Color.White else Color(0xFF0F172A))
                                        )

                                        Surface(
                                            onClick = { timeLimitSeconds += 10 },
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFFEF3C7),
                                            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("+10s", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                                            }
                                        }
                                    }

                                    // Önayarlar: [Yok] [30s] [60s] [120s]
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        val timePresets = listOf(0 to "Yok", 30 to "30s", 60 to "60s", 120 to "120s")
                                        timePresets.forEach { (sec, label) ->
                                            val isSel = timeLimitSeconds == sec
                                            Surface(
                                                onClick = { timeLimitSeconds = sec },
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isSel) Color(0xFFD97706) else (if (isDarkTheme) Color(0x2564748B) else Color(0xFFF1F5F9)),
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (isSel) Color(0xFFD97706) else (if (isDarkTheme) Color(0x3064748B) else Color(0xFFCBD5E1))
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(26.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = label,
                                                        fontSize = 10.sp,
                                                        fontWeight = if (isSel) FontWeight.Black else FontWeight.Bold,
                                                        color = if (isSel) Color.White else (if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF475569))
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // --- 2. HEDEF KİŞİ SINIRI KUTUSU (BİREBİR EŞİT ÖLÇÜ) ---
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isDarkTheme) Color(0x351E293B) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFFCBD5E1)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(116.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Başlık & Rozet
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "🎯 Hedef Limit",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (isDarkTheme) Color.White else Color(0xFF0F172A)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (targetStudentLimit > 0) Color(0xFFEEF2FF) else (if (isDarkTheme) Color(0x301E293B) else Color(0xFFF1F5F9))
                                        ) {
                                            Text(
                                                text = if (targetStudentLimit == 0) "Sınırsız" else "$targetStudentLimit Kişi",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (targetStudentLimit > 0) Color(0xFF4338CA) else Color(0xFF64748B),
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    // Stepper: [-1]  XX kişi  [+1]
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            onClick = { if (targetStudentLimit > 0) targetStudentLimit-- },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isDarkTheme) Color(0x3064748B) else Color(0xFFE2E8F0),
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("-1", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Text(
                                            text = if (targetStudentLimit == 0) "Sınırsız" else "$targetStudentLimit kişi",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (targetStudentLimit > 0) Color(0xFF4F46E5) else (if (isDarkTheme) Color.White else Color(0xFF0F172A))
                                        )

                                        Surface(
                                            onClick = { targetStudentLimit++ },
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFEEF2FF),
                                            border = BorderStroke(1.dp, Color(0xFFC7D2FE)),
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("+1", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4338CA))
                                            }
                                        }
                                    }

                                    // Önayarlar: [Yok] [3] [5] [10]
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        val targetPresets = listOf(0 to "Yok", 3 to "İlk 3", 5 to "İlk 5", 10 to "İlk 10")
                                        targetPresets.forEach { (limit, label) ->
                                            val isSel = targetStudentLimit == limit
                                            Surface(
                                                onClick = { targetStudentLimit = limit },
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isSel) Color(0xFF4F46E5) else (if (isDarkTheme) Color(0x2564748B) else Color(0xFFF1F5F9)),
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (isSel) Color(0xFF4F46E5) else (if (isDarkTheme) Color(0x3064748B) else Color(0xFFCBD5E1))
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(26.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = label,
                                                        fontSize = 10.sp,
                                                        fontWeight = if (isSel) FontWeight.Black else FontWeight.Bold,
                                                        color = if (isSel) Color.White else (if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF475569))
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Adım 1 Alt Butonları (Sabit ve Asla Kesilmez)
                    Surface(
                        color = if (isDarkTheme) Color(0x401E293B) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text("VAZGEÇ", fontWeight = FontWeight.Bold, color = Color(0xFF64748B), fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    remainingSeconds = timeLimitSeconds
                                    isTimerRunning = timeLimitSeconds > 0
                                    currentStep = 2
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(42.dp)
                            ) {
                                Text("ÖĞRENCİ SEÇİMİNE GEÇ", fontWeight = FontWeight.Black, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // --- ADIM 2: ÖĞRENCİ SEÇİMİ (KAYDIRMASIZ, HER ŞEY TEK EKRANDA) ---
                if (currentStep == 2) {
                    val hasTimer = timeLimitSeconds > 0 || remainingSeconds > 0
                    val hasQuota = targetStudentLimit > 0

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        // --- SAYAÇLAR VE CANLI BİLGİ BANDI (HERO STATUS BAR) ---
                        // 1. Durum: Hem Süre Hem Hedef Kişi Sınırı Varsa (İki Kart Yan Yana)
                        if (hasTimer && hasQuota) {
                            val remainingSlots = (targetStudentLimit - selectedStudentIds.size).coerceAtLeast(0)
                            val isWarning = remainingSeconds in 1..10

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Sol Kart: Süre Sayacı
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isWarning) Color(0xFFFEF2F2) else Color(0xFFFFFBEB),
                                    border = BorderStroke(1.5.dp, if (isWarning) Color(0xFFEF4444) else Color(0xFFF59E0B)),
                                    shadowElevation = 2.dp,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = if (isWarning) "⚠️ KALAN SÜRE" else "⏱ KALAN SÜRE",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (isWarning) Color(0xFFDC2626) else Color(0xFFB45309)
                                            )
                                            Text(
                                                text = if (remainingSeconds > 0) "$remainingSeconds sn" else "SÜRE BİTTİ",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (isWarning) Color(0xFFDC2626) else Color(0xFFD97706)
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (remainingSeconds > 0) {
                                                IconButton(
                                                    onClick = { isTimerRunning = !isTimerRunning },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                        contentDescription = null,
                                                        tint = Color(0xFF15803D),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            Surface(
                                                onClick = {
                                                    remainingSeconds += 10
                                                    isTimerRunning = true
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFFEF3C7),
                                                border = BorderStroke(1.dp, Color(0xFFFDE68A))
                                            ) {
                                                Text(
                                                    text = "+10s",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFB45309),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Sağ Kart: Kalan Kişi Sayacı
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (remainingSlots == 0) Color(0xFFF0FDF4) else Color(0xFFEEF2FF),
                                    border = BorderStroke(1.5.dp, if (remainingSlots == 0) Color(0xFF22C55E) else Color(0xFF6366F1)),
                                    shadowElevation = 2.dp,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = if (remainingSlots == 0) "🎉 HEDEF DOLDU" else "🎯 KALAN KİŞİ",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (remainingSlots == 0) Color(0xFF15803D) else Color(0xFF4338CA)
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (remainingSlots > 0) "$remainingSlots Kişi Kaldı" else "Doldu! ($targetStudentLimit)",
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (remainingSlots == 0) Color(0xFF15803D) else Color(0xFF4338CA)
                                            )
                                            Text(
                                                text = "${selectedStudentIds.size}/$targetStudentLimit",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        // 2. Durum: Sadece Süre Sınırı Varsa (Geniş Tek Hero Kart)
                        else if (hasTimer && !hasQuota) {
                            val isWarning = remainingSeconds in 1..10
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isWarning) Color(0xFFFEF2F2) else Color(0xFFFFFBEB),
                                border = BorderStroke(1.5.dp, if (isWarning) Color(0xFFEF4444) else Color(0xFFF59E0B)),
                                shadowElevation = 3.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isWarning) Color(0xFFFEE2E2) else Color(0xFFFEF3C7),
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(if (isWarning) "⚠️" else "⏱", fontSize = 18.sp)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = if (isWarning) "SON SANİYELER (SÜRE BİTİYOR)" else "GERİ SAYIM SÜRESİ",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (isWarning) Color(0xFFDC2626) else Color(0xFFB45309)
                                            )
                                            Text(
                                                text = if (remainingSeconds > 0) "$remainingSeconds SANİYE" else "⌛ SÜRE DOLDU!",
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (isWarning) Color(0xFFDC2626) else (if (remainingSeconds == 0) Color(0xFFEF4444) else Color(0xFFD97706))
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        if (remainingSeconds > 0) {
                                            Surface(
                                                onClick = { isTimerRunning = !isTimerRunning },
                                                shape = CircleShape,
                                                color = Color(0xFFDCFCE7),
                                                border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                        contentDescription = null,
                                                        tint = Color(0xFF15803D),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }

                                        listOf(10, 30).forEach { sec ->
                                            Surface(
                                                onClick = {
                                                    remainingSeconds += sec
                                                    isTimerRunning = true
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFFFEF3C7),
                                                border = BorderStroke(1.dp, Color(0xFFFDE68A))
                                            ) {
                                                Text(
                                                    text = "+${sec}s",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFB45309),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        // 3. Durum: Sadece Hedef Kişi Sınırı Varsa (Geniş Tek Hero Kart)
                        else if (hasQuota && !hasTimer) {
                            val remainingSlots = (targetStudentLimit - selectedStudentIds.size).coerceAtLeast(0)
                            val isCompleted = remainingSlots == 0
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isCompleted) Color(0xFFF0FDF4) else Color(0xFFEEF2FF),
                                border = BorderStroke(1.5.dp, if (isCompleted) Color(0xFF22C55E) else Color(0xFF6366F1)),
                                shadowElevation = 3.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isCompleted) Color(0xFFDCFCE7) else Color(0xFFE0E7FF),
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(if (isCompleted) "🎉" else "🎯", fontSize = 18.sp)
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = if (isCompleted) "HEDEF BAŞARIYLA TAMAMLANDI" else "HEDEF KİŞİ SINIRI SAYACI",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (isCompleted) Color(0xFF15803D) else Color(0xFF4338CA)
                                            )
                                            Text(
                                                text = if (!isCompleted) "👥 $remainingSlots KİŞİ KALDI" else "🎉 KOTA DOLDU! ($targetStudentLimit Kişi)",
                                                fontSize = 21.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (isCompleted) Color(0xFF15803D) else Color(0xFF4338CA)
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isCompleted) Color(0xFF15803D) else Color(0xFF4F46E5)
                                    ) {
                                        Text(
                                            text = "${selectedStudentIds.size} / $targetStudentLimit",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                        // 4. Durum: Hiçbiri Yoksa (Klasik Üst Bar)
                        else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Kategori Rozeti
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(selectedCategory.colorHex).copy(alpha = 0.16f),
                                    border = BorderStroke(1.dp, Color(selectedCategory.colorHex))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    ) {
                                        Icon(
                                            imageVector = selectedCategory.icon,
                                            contentDescription = null,
                                            tint = Color(selectedCategory.colorHex),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${selectedCategory.name} +$starCount⭐",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(selectedCategory.colorHex)
                                        )
                                    }
                                }

                                // İsteğe Bağlı Hızlı Süre Başlatma
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isDarkTheme) Color(0x301E293B) else Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "⏱ Hızlı:",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF64748B)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        listOf(10, 20, 30).forEach { sec ->
                                            Text(
                                                text = "${sec}s",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF4F46E5),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .clickable {
                                                        remainingSeconds = sec
                                                        isTimerRunning = true
                                                    }
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                // Canlı Seçilen Kişi Sayısı Rozeti
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selectedStudentIds.isNotEmpty()) Color(0xFFEEF2FF) else (if (isDarkTheme) Color(0x301E293B) else Color(0xFFF8FAFC)),
                                    border = BorderStroke(1.2.dp, if (selectedStudentIds.isNotEmpty()) Color(0xFF6366F1) else Color(0xFFCBD5E1))
                                ) {
                                    Text(
                                        text = "👥 ${selectedStudentIds.size}/${students.size} Seçili",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (selectedStudentIds.isNotEmpty()) Color(0xFF4338CA) else Color(0xFF64748B),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        // Eğer Sayaç Hero Paneli Gösterildiyse, Altında Kompakt Özet Çizgisi
                        if (hasTimer || hasQuota) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(selectedCategory.colorHex).copy(alpha = 0.14f)
                                ) {
                                    Text(
                                        text = "${selectedCategory.name} (+${starCount}⭐)",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(selectedCategory.colorHex),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Text(
                                    text = "👥 ${selectedStudentIds.size}/${students.size} Öğrenci Seçildi",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (selectedStudentIds.isNotEmpty()) Color(0xFF4338CA) else Color(0xFF64748B)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // ÜST KONTROL BANDI: Hızlı Filtre Butonları
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val erkekSayisi = students.count { it.gender.equals("Erkek", ignoreCase = true) }
                            val kizSayisi = students.count { it.gender.equals("Kız", ignoreCase = true) }

                            // Tümünü Seç
                            Surface(
                                onClick = {
                                    selectedStudentIds.clear()
                                    selectedStudentIds.addAll(students.map { it.id })
                                    checkTargetPersonLimitAndProceed()
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedStudentIds.size == students.size) Color(0xFF6366F1) else (if (isDarkTheme) Color(0x301E293B) else Color(0xFFF1F5F9)),
                                border = BorderStroke(1.dp, if (selectedStudentIds.size == students.size) Color(0xFF6366F1) else Color(0xFFCBD5E1)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 5.dp)) {
                                    Text(
                                        text = "Tümü",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (selectedStudentIds.size == students.size) Color.White else (if (isDarkTheme) Color.White else Color(0xFF0F172A))
                                    )
                                }
                            }

                            // Erkekler
                            Surface(
                                onClick = {
                                    selectedStudentIds.clear()
                                    selectedStudentIds.addAll(students.filter { it.gender.equals("Erkek", ignoreCase = true) }.map { it.id })
                                    checkTargetPersonLimitAndProceed()
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDarkTheme) Color(0x200284C7) else Color(0xFFF0F9FF),
                                border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 5.dp)) {
                                    Text(
                                        text = "Erkek ($erkekSayisi)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF0284C7)
                                    )
                                }
                            }

                            // Kızlar
                            Surface(
                                onClick = {
                                    selectedStudentIds.clear()
                                    selectedStudentIds.addAll(students.filter { it.gender.equals("Kız", ignoreCase = true) }.map { it.id })
                                    checkTargetPersonLimitAndProceed()
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDarkTheme) Color(0x20EC4899) else Color(0xFFFDF2F8),
                                border = BorderStroke(1.dp, Color(0xFFF472B6)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 5.dp)) {
                                    Text(
                                        text = "Kız ($kizSayisi)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFDB2777)
                                    )
                                }
                            }

                            // Tersine Çevir
                            Surface(
                                onClick = {
                                    val current = selectedStudentIds.toSet()
                                    selectedStudentIds.clear()
                                    selectedStudentIds.addAll(students.filter { !current.contains(it.id) }.map { it.id })
                                    checkTargetPersonLimitAndProceed()
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDarkTheme) Color(0x301E293B) else Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 5.dp)) {
                                    Text(
                                        text = "Tersine",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF334155)
                                    )
                                }
                            }

                            // Temizle
                            Surface(
                                onClick = { selectedStudentIds.clear() },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDarkTheme) Color(0x20EF4444) else Color(0xFFFEF2F2),
                                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 5.dp)) {
                                    Text(
                                        text = "Temizle",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = BirizError
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // ÖĞRENCİ GRID'İ - TÜM ÖĞRENCİLER KAYDIRMA OLMADAN EKRANDA!
                        val colCount = if (students.size <= 18) 2 else if (students.size <= 27) 3 else 4

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(colCount),
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            items(students, key = { it.id }) { student ->
                                val isSelected = selectedStudentIds.contains(student.id)
                                val isKiz = student.gender.equals("Kız", ignoreCase = true)

                                Surface(
                                    onClick = {
                                        if (isSelected) {
                                            selectedStudentIds.remove(student.id)
                                        } else {
                                            selectedStudentIds.add(student.id)
                                            checkTargetPersonLimitAndProceed()
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFF4338CA) else (if (isDarkTheme) Color(0x351E293B) else Color.White),
                                    border = BorderStroke(
                                        if (isSelected) 1.5.dp else 1.dp,
                                        if (isSelected) Color(0xFF6366F1) else (if (isKiz) Color(0x80F472B6) else Color(0x8038BDF8))
                                    ),
                                    shadowElevation = if (isSelected) 2.dp else 0.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(36.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Numara & İsim
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = "#${student.studentNumber}",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (isSelected) Color(0xFFC7D2FE) else (if (isKiz) Color(0xFFDB2777) else Color(0xFF0284C7))
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = student.fullName,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                                color = if (isSelected) Color.White else (if (isDarkTheme) Color.White else Color(0xFF0F172A)),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(3.dp))

                                        // Seçim Göstergesi (Seçiliyse Beyaz Dolu Tik, Seçili Değilse İnce Halka)
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isSelected) Color.White else Color.Transparent,
                                            border = BorderStroke(
                                                1.2.dp,
                                                if (isSelected) Color.White else (if (isKiz) Color(0x80F472B6) else Color(0x8038BDF8))
                                            ),
                                            modifier = Modifier.size(16.dp)
                                        ) {
                                            if (isSelected) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Seçildi",
                                                        tint = Color(0xFF4338CA),
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Adım 2 Alt Butonları (Sabit ve Asla Kesilmez)
                        Surface(
                            color = if (isDarkTheme) Color(0x401E293B) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { currentStep = 1 },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("GERİ", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }

                                TextButton(onClick = onDismiss) {
                                    Text("VAZGEÇ", fontSize = 11.5.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        if (selectedStudentIds.isNotEmpty()) {
                                            showLuckyPromptDialog = true
                                        }
                                    },
                                    enabled = selectedStudentIds.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${selectedStudentIds.size} ÖĞRENCİYE VER (+${starCount * selectedStudentIds.size} ⭐)",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- ADIM 3A: ŞANSLI ÖĞRENCİ SEÇİLSİN Mİ? DİYALOĞU (Görsel 8) ---
    if (showLuckyPromptDialog) {
        Dialog(onDismissRequest = { /* Modal dışına basınca kapatma */ }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isDarkTheme) Color(0xFF1E293B) else Color.White,
                border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
                shadowElevation = 10.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // İkon Rozeti
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFEEF2FF),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF6366F1),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "ŞANSLI ÖĞRENCİ SEÇİLSİN Mİ?",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDarkTheme) Color.White else BirizNavyPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Etkinlik sonunda sınıf içerisinden rastgele 1 şanslı öğrenci seçip ekstra ödül vermek ister misiniz?",
                        fontSize = 12.sp,
                        color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // EVET, RASTGELE SEÇ! Butonu
                    Button(
                        onClick = {
                            selectedLuckyStudent = students.randomOrNull()
                            showLuckyPromptDialog = false
                            showLuckyQuestionDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text("EVET, RASTGELE SEÇ!", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // HAYIR, BİTİR Butonu
                    OutlinedButton(
                        onClick = {
                            showLuckyPromptDialog = false
                            val awardedStudents = students.filter { selectedStudentIds.contains(it.id) }
                            onAwardBatch(selectedCategory, starCount, activityNote.trim(), awardedStudents, null)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text("HAYIR, BİTİR", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }
    }

    // --- ADIM 3B: ŞANSLI ÖĞRENCİ SORU & ÖDÜL DİYALOĞU (Görsel 9) ---
    if (showLuckyQuestionDialog && selectedLuckyStudent != null) {
        val lucky = selectedLuckyStudent!!

        Dialog(onDismissRequest = { /* Modal dışına basınca kapatma */ }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isDarkTheme) Color(0xFF1E293B) else Color.White,
                border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
                shadowElevation = 10.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Turuncu Taç Simgesi (Görsel 9)
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFFF97316),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("👑", fontSize = 32.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "ŞANSLI ÖĞRENCİ!",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFEA580C),
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = lucky.fullName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDarkTheme) Color.White else BirizNavyPrimary,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Okul No: #${lucky.studentNumber}",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Soruya veya etkinliğe doğru cevap verdi mi?",
                        fontSize = 13.sp,
                        color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // DOĞRU (+ÖDÜL) Butonu
                        OutlinedButton(
                            onClick = {
                                showLuckyQuestionDialog = false
                                val awardedStudents = students.filter { selectedStudentIds.contains(it.id) }
                                onAwardBatch(selectedCategory, starCount, activityNote.trim(), awardedStudents, lucky)
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, Color(0xFF10B981)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFFECFDF5),
                                contentColor = Color(0xFF047857)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("DOĞRU (+ÖDÜL)", fontWeight = FontWeight.Black, fontSize = 11.5.sp)
                        }

                        // YANLIŞ Butonu
                        Button(
                            onClick = {
                                showLuckyQuestionDialog = false
                                val awardedStudents = students.filter { selectedStudentIds.contains(it.id) }
                                onAwardBatch(selectedCategory, starCount, activityNote.trim(), awardedStudents, null)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDarkTheme) Color(0x3564748B) else Color(0xFFF1F5F9),
                                contentColor = Color(0xFF64748B)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("YANLIŞ", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                        }
                    }
                }
            }
        }
    }

    if (showCategoryPicker) {
        CategoryGridPickerDialog(
            selectedCategoryId = selectedCategory.id,
            isDarkTheme = isDarkTheme,
            onDismiss = { showCategoryPicker = false },
            onCategorySelected = {
                selectedCategory = it
            }
        )
    }
}

/**
 * Öğrencinin Yıldız Geçmişi ve Silme Modalı
 */
@Composable
private fun StudentStarHistoryDialog(
    student: Student,
    records: List<StarRewardRecord>,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onDeleteRecord: (String) -> Unit
) {
    val totalStars = records.sumOf { it.starCount }
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr")) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isDarkTheme) BirizDarkSurfaceGlass else Color.White,
            border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.80f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
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
                            color = Color(0xFFFEF3C7),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("⭐", fontSize = 18.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = student.fullName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                            )
                            Text(
                                text = "Toplam: $totalStars Yıldız • ${records.size} İşlem",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDarkTheme) BirizTurquoise else Color(0xFF0284C7)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (records.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Bu öğrencinin henüz kayıtlı yıldızı bulunmamaktadır.",
                            fontSize = 12.5.sp,
                            color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(records, key = { it.id }) { record ->
                            val category = StarCategory.findById(record.categoryId)

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDarkTheme) Color(0x351E293B) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = category.icon,
                                        contentDescription = null,
                                        tint = Color(category.colorHex),
                                        modifier = Modifier.size(18.dp)
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = record.categoryName,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                                            )
                                            if (record.isLuckyBonus) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("👑 Şanslı", fontSize = 10.sp, color = Color(0xFFEA580C))
                                            }
                                        }
                                        if (record.note.isNotBlank()) {
                                            Text(
                                                text = record.note,
                                                fontSize = 11.sp,
                                                color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                                            )
                                        }
                                        Text(
                                            text = dateFormat.format(Date(record.timestamp)),
                                            fontSize = 9.5.sp,
                                            color = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted
                                        )
                                    }

                                    Text(
                                        text = "+${record.starCount} ⭐",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = BirizAmber
                                    )

                                    Spacer(modifier = Modifier.width(6.dp))

                                    IconButton(
                                        onClick = { onDeleteRecord(record.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Sil",
                                            tint = BirizError,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Kapat")
                }
            }
        }
    }
}
