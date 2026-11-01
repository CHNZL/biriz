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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
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

    // Kalıcı Yıldız Kayıtları
    var starRecords by remember {
        mutableStateOf(StarRewardPersistenceHelper.loadStarRecords(context, classRoom.id))
    }

    // Arama ve Filtreleme
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<String?>("ALL") } // ALL, ACADEMIC, VALUES
    var sortOrder by remember { mutableStateOf("STARS_DESC") } // STARS_DESC, NAME_ASC, NUMBER_ASC

    // Modal Durumları
    var studentForIndividualStar by remember { mutableStateOf<Student?>(null) }
    var studentForHistory by remember { mutableStateOf<Student?>(null) }
    var showActivityWizard by remember { mutableStateOf(false) }

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
                        // Sol Bilgi Rozeti: Toplam Yıldız
                        Surface(
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

                    // Arama & Sıralama Satırı
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Arama Çubuğu
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Öğrenci adı veya no ara...", fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Close, contentDescription = "Temizle", modifier = Modifier.size(14.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                                unfocusedBorderColor = if (isDarkTheme) BirizDarkCardBorder else BirizBorder,
                                focusedContainerColor = if (isDarkTheme) Color(0x301E293B) else Color.White,
                                unfocusedContainerColor = if (isDarkTheme) Color(0x201E293B) else Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        )

                        // Sıralama Butonu
                        Surface(
                            onClick = {
                                sortOrder = when (sortOrder) {
                                    "STARS_DESC" -> "NAME_ASC"
                                    "NAME_ASC" -> "NUMBER_ASC"
                                    else -> "STARS_DESC"
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDarkTheme) Color(0x303B82F6) else Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, if (isDarkTheme) Color(0x503B82F6) else Color(0xFFBFDBFE)),
                            modifier = Modifier.height(46.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            ) {
                                Text(
                                    text = when (sortOrder) {
                                        "STARS_DESC" -> "⭐ Puana Göre"
                                        "NAME_ASC" -> "🔤 İsme Göre"
                                        else -> "🔢 Numaraya Göre"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkTheme) BirizTurquoise else Color(0xFF1D4ED8)
                                )
                            }
                        }
                    }
                }
            }

            // --- 2. LİDERLİK KÜRSÜSÜ (Eğer Yıldız Varsa) ---
            if (topStudents.isNotEmpty() && searchQuery.isBlank()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDarkTheme) Color(0x250EA5E9) else Color(0xFFF0FDF4),
                    border = BorderStroke(1.dp, if (isDarkTheme) Color(0x400EA5E9) else Color(0xFFBBF7D0))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = BirizAmber,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Haftanın Yıldızları (Lider Tablosu)",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) BirizDarkTextPrimary else Color(0xFF15803D)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            topStudents.forEachIndexed { index, student ->
                                val medal = when (index) {
                                    0 -> "🥇"
                                    1 -> "🥈"
                                    else -> "🥉"
                                }
                                val stars = studentStarMap[student.id] ?: 0
                                Surface(
                                    onClick = { studentForIndividualStar = student },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isDarkTheme) Color(0x351E293B) else Color.White,
                                    border = BorderStroke(0.8.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFFE2E8F0)),
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(text = medal, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Column {
                                            Text(
                                                text = student.fullName.split(" ").firstOrNull() ?: student.fullName,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "$stars ⭐",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                color = BirizAmber
                                            )
                                        }
                                    }
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
 * Bireysel Yıldız Verme Modalı
 * Sırası ile:
 * 1. Önce kategori seçimi
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
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                            )
                            Text(
                                text = "#${student.studentNumber} ${student.fullName}",
                                fontSize = 12.sp,
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

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // ADIM 1: KATEGORİ SEÇİN (Görseldeki gibi Rozetler)
                    Text(
                        text = "1. KATEGORİ SEÇİN:",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF334155),
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StarCategory.ALL_CATEGORIES.forEach { category ->
                            val isSelected = category.id == selectedCategory.id
                            Surface(
                                onClick = { selectedCategory = category },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) {
                                    Color(category.colorHex).copy(alpha = 0.18f)
                                } else {
                                    if (isDarkTheme) Color(0x301E293B) else Color(0xFFF8FAFC)
                                },
                                border = BorderStroke(
                                    if (isSelected) 1.8.dp else 1.dp,
                                    if (isSelected) Color(category.colorHex) else (if (isDarkTheme) Color(0x4064748B) else Color(0xFFE2E8F0))
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp)
                                ) {
                                    Icon(
                                        imageVector = category.icon,
                                        contentDescription = null,
                                        tint = Color(category.colorHex),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = category.name,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        color = if (isSelected) Color(category.colorHex) else (if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ADIM 2: AÇIKLAMA / BAŞARI NOTU (İSTEĞE BAĞLI)
                    Text(
                        text = "2. AÇIKLAMA / BAŞARI NOTU (İSTEĞE BAĞLI):",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF334155),
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        placeholder = { Text("Örn: Hafta boyunca örnek davranış sergiledi...", fontSize = 12.sp) },
                        shape = RoundedCornerShape(10.dp),
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
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF334155),
                        letterSpacing = 0.5.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDarkTheme) Color(0x351E293B) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                IconButton(
                                    onClick = { if (starCount > 1) starCount-- },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Azalt", tint = BirizAmber)
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Text(
                                    text = "+$starCount YILDIZ",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFD97706)
                                )

                                Spacer(modifier = Modifier.width(16.dp))

                                IconButton(
                                    onClick = { if (starCount < 25) starCount++ },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Arttır", tint = BirizAmber)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Hızlı Önayarlar
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf(1, 2, 3, 5, 10).forEach { amount ->
                                    val isSelected = starCount == amount
                                    Surface(
                                        onClick = { starCount = amount },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) Color(0xFF6366F1) else (if (isDarkTheme) Color(0x306366F1) else Color(0xFFEEF2FF)),
                                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF6366F1) else Color(0xFFC7D2FE))
                                    ) {
                                        Text(
                                            text = "+$amount",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else Color(0xFF4F46E5),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
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
                .fillMaxHeight(0.94f)
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "ETKİNLİKLİ YILDIZ DAĞITIMI",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isDarkTheme) Color.White else BirizNavyPrimary,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF6366F1)
                                    ) {
                                        Text(
                                            text = if (currentStep == 1) "Adım 1/2: Kazanım & Kurallar" else "Adım 2/2: Öğrenci Seçimi",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (currentStep == 1) "Kategori, açıklama, yıldız adedi, zaman sınırı ve hedef öğrenci limitini belirleyin."
                                    else "${students.size} öğrencinin tamamı ekranda listelenmektedir. Tek dokunuşla seçebilirsiniz.",
                                    fontSize = 10.5.sp,
                                    color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                                )
                            }
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Kapat")
                        }
                    }
                }

                // --- ADIM 1: KAZANIM VE KURALLAR ---
                if (currentStep == 1) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        // 1. KATEGORİ SEÇİN (Görseldeki Gibi Rozetler)
                        Text(
                            text = "YILDIZ KATEGORİSİ SEÇİN",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF334155),
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            StarCategory.ALL_CATEGORIES.forEach { category ->
                                val isSelected = category.id == selectedCategory.id
                                Surface(
                                    onClick = { selectedCategory = category },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) {
                                        Color(category.colorHex).copy(alpha = 0.18f)
                                    } else {
                                        if (isDarkTheme) Color(0x301E293B) else Color(0xFFF8FAFC)
                                    },
                                    border = BorderStroke(
                                        if (isSelected) 1.8.dp else 1.dp,
                                        if (isSelected) Color(category.colorHex) else (if (isDarkTheme) Color(0x4064748B) else Color(0xFFE2E8F0))
                                    )
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp)
                                    ) {
                                        Icon(
                                            imageVector = category.icon,
                                            contentDescription = null,
                                            tint = Color(category.colorHex),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = category.name,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                            color = if (isSelected) Color(category.colorHex) else (if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 2. ETKİNLİK / KAZANIM AÇIKLAMASI
                        Text(
                            text = "ETKİNLİK / KAZANIM AÇIKLAMASI",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF334155),
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = activityNote,
                            onValueChange = { activityNote = it },
                            placeholder = { Text("Örn: Ders içi aktif katılım, Matematik problem çözme yarışması, Örnek davranış...", fontSize = 12.sp) },
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isDarkTheme) BirizTurquoise else Color(0xFF6366F1),
                                unfocusedBorderColor = if (isDarkTheme) BirizDarkCardBorder else BirizBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 3. VERİLECEK YILDIZ MİKTARI
                        Text(
                            text = "VERİLECEK YILDIZ MİKTARI",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF334155),
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDarkTheme) Color(0x351E293B) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFFE2E8F0)),
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
                                    IconButton(
                                        onClick = { if (starCount > 1) starCount-- },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "Azalt")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "+$starCount YILDIZ",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF6366F1)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = { if (starCount < 25) starCount++ },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Arttır")
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(1, 2, 3, 5, 10).forEach { amount ->
                                        val isSel = starCount == amount
                                        Surface(
                                            onClick = { starCount = amount },
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isSel) Color(0xFF6366F1) else (if (isDarkTheme) Color(0x306366F1) else Color(0xFFEEF2FF))
                                        ) {
                                            Text(
                                                text = "+$amount",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSel) Color.White else Color(0xFF4F46E5),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 4. EKSTRA KONTROLLER: ZAMAN SINIRI & HEDEF KİŞİ SINIRI (YAN YANA / BLOKLAR)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Zaman Sınırı (Saniye)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDarkTheme) Color(0x351E293B) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFFE2E8F0)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("⏱ ZAMAN SINIRI", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = if (timeLimitSeconds == 0) "0 = Süresiz" else "$timeLimitSeconds sn",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (timeLimitSeconds > 0) Color(0xFFD97706) else Color.Gray
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { if (timeLimitSeconds >= 10) timeLimitSeconds -= 10 },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Text("-10s", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Text(
                                            text = "$timeLimitSeconds sn",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        IconButton(
                                            onClick = { timeLimitSeconds += 10 },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Text("+10s", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        listOf(0, 30, 60, 120).forEach { secs ->
                                            Surface(
                                                onClick = { timeLimitSeconds = secs },
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (timeLimitSeconds == secs) Color(0xFF6366F1) else Color.Transparent
                                            ) {
                                                Text(
                                                    text = if (secs == 0) "Süresiz" else "${secs}s",
                                                    fontSize = 9.5.sp,
                                                    color = if (timeLimitSeconds == secs) Color.White else Color.Gray,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Hedef Kişi Sınırı
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDarkTheme) Color(0x351E293B) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFFE2E8F0)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("🎯 HEDEF KİŞİ SINIRI", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = if (targetStudentLimit == 0) "0 = Sınırsız" else "$targetStudentLimit kişi",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (targetStudentLimit > 0) Color(0xFF2563EB) else Color.Gray
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { if (targetStudentLimit > 0) targetStudentLimit-- },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Text("-1", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Text(
                                            text = if (targetStudentLimit == 0) "Sınırsız" else "$targetStudentLimit öğrenci",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        IconButton(
                                            onClick = { targetStudentLimit++ },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Text("+1", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        listOf(0, 3, 5, 10).forEach { limit ->
                                            Surface(
                                                onClick = { targetStudentLimit = limit },
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (targetStudentLimit == limit) Color(0xFF6366F1) else Color.Transparent
                                            ) {
                                                Text(
                                                    text = if (limit == 0) "Sınırsız" else "İlk $limit",
                                                    fontSize = 9.5.sp,
                                                    color = if (targetStudentLimit == limit) Color.White else Color.Gray,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Adım 1 Alt Butonları
                    Surface(
                        color = if (isDarkTheme) Color(0x401E293B) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text("VAZGEÇ", fontWeight = FontWeight.Bold, color = Color.Gray)
                            }

                            Button(
                                onClick = {
                                    remainingSeconds = timeLimitSeconds
                                    isTimerRunning = timeLimitSeconds > 0
                                    currentStep = 2
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("ÖĞRENCİ SEÇİMİNE GEÇ", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // --- ADIM 2: ÖĞRENCİ SEÇİMİ (Görsel 6 & 7) ---
                if (currentStep == 2) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        // Üst Bilgi Satırı: Seçili Kategori & Yıldız, Sayaç, Seçilen Sayısı
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Kategori & Yıldız Rozeti
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(selectedCategory.colorHex).copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, Color(selectedCategory.colorHex))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = selectedCategory.icon,
                                        contentDescription = null,
                                        tint = Color(selectedCategory.colorHex),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${selectedCategory.name} +$starCount Yıldız",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(selectedCategory.colorHex)
                                    )
                                }
                            }

                            // Süre Sayacı & Hızlı Butonlar (10s, 20s, 30s)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (remainingSeconds > 0) Color(0xFFFEF3C7) else (if (isDarkTheme) Color(0x301E293B) else Color(0xFFF1F5F9)),
                                border = BorderStroke(1.dp, if (remainingSeconds > 0) BirizAmber else Color(0xFFCBD5E1))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "⏱ ${remainingSeconds}s",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (remainingSeconds > 0) Color(0xFFB45309) else Color.Gray
                                    )

                                    Spacer(modifier = Modifier.width(4.dp))

                                    // Hızlı 10s 20s 30s butonları
                                    listOf(10, 20, 30).forEach { sec ->
                                        Text(
                                            text = "${sec}s",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
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

                                    Spacer(modifier = Modifier.width(4.dp))

                                    // Başlat / Duraklat
                                    if (remainingSeconds > 0) {
                                        IconButton(
                                            onClick = { isTimerRunning = !isTimerRunning },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = Color(0xFF15803D),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            // Seçilen Kişi Sayısı Rozeti
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDarkTheme) Color(0x356366F1) else Color(0xFFEEF2FF),
                                border = BorderStroke(1.dp, Color(0xFF6366F1))
                            ) {
                                Text(
                                    text = "👥 Seçilen: ${selectedStudentIds.size} / ${students.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4338CA),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Hızlı Filtre Butonları (Tümünü Seç, Erkekler, Kızlar, Tersine Çevir, Temizle)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    selectedStudentIds.clear()
                                    selectedStudentIds.addAll(students.map { it.id })
                                    checkTargetPersonLimitAndProceed()
                                }
                            ) { Text("Tümünü Seç", fontSize = 10.5.sp, fontWeight = FontWeight.Bold) }

                            TextButton(
                                onClick = {
                                    selectedStudentIds.clear()
                                    selectedStudentIds.addAll(students.filter { it.gender.equals("Erkek", ignoreCase = true) }.map { it.id })
                                    checkTargetPersonLimitAndProceed()
                                }
                            ) { Text("Erkekler (${students.count { it.gender.equals("Erkek", ignoreCase = true) }})", fontSize = 10.5.sp, fontWeight = FontWeight.Bold) }

                            TextButton(
                                onClick = {
                                    selectedStudentIds.clear()
                                    selectedStudentIds.addAll(students.filter { it.gender.equals("Kız", ignoreCase = true) }.map { it.id })
                                    checkTargetPersonLimitAndProceed()
                                }
                            ) { Text("Kızlar (${students.count { it.gender.equals("Kız", ignoreCase = true) }})", fontSize = 10.5.sp, fontWeight = FontWeight.Bold) }

                            TextButton(
                                onClick = {
                                    val current = selectedStudentIds.toSet()
                                    selectedStudentIds.clear()
                                    selectedStudentIds.addAll(students.filter { !current.contains(it.id) }.map { it.id })
                                    checkTargetPersonLimitAndProceed()
                                }
                            ) { Text("Tersine Çevir", fontSize = 10.5.sp, fontWeight = FontWeight.Bold) }

                            TextButton(
                                onClick = { selectedStudentIds.clear() }
                            ) { Text("Temizle", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = BirizError) }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Öğrenci Seçim Grid'i (Görseller 6 & 7 gibi mor butonlu kartlar)
                        val step2FilteredStudents = remember(students, studentSearch) {
                            if (studentSearch.isBlank()) students
                            else students.filter {
                                it.fullName.contains(studentSearch, ignoreCase = true) ||
                                        it.studentNumber.contains(studentSearch, ignoreCase = true)
                            }
                        }

                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 130.dp),
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(step2FilteredStudents, key = { it.id }) { student ->
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
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) Color(0xFF4F46E5) else (if (isDarkTheme) Color(0x351E293B) else Color.White),
                                    border = BorderStroke(
                                        if (isSelected) 2.dp else 1.dp,
                                        if (isSelected) Color(0xFF6366F1) else (if (isKiz) Color(0x40F472B6) else Color(0x4038BDF8))
                                    ),
                                    shadowElevation = if (isSelected) 3.dp else 1.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "#${student.studentNumber}",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color(0xFFE0E7FF) else (if (isKiz) Color(0xFFDB2777) else Color(0xFF0284C7))
                                            )
                                            Text(
                                                text = student.fullName,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else (if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Surface(
                                            shape = CircleShape,
                                            color = if (isSelected) Color.White else Color.Transparent,
                                            border = BorderStroke(1.2.dp, if (isSelected) Color.White else Color(0xFFCBD5E1)),
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            if (isSelected) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Seçildi",
                                                        tint = Color(0xFF4F46E5),
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Adım 2 Alt Butonları
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
                                OutlinedButton(
                                    onClick = { currentStep = 1 },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("GERİ", fontSize = 11.sp)
                                }

                                TextButton(onClick = onDismiss) {
                                    Text("VAZGEÇ", fontSize = 11.sp, color = Color.Gray)
                                }

                                Button(
                                    onClick = {
                                        if (selectedStudentIds.isNotEmpty()) {
                                            showLuckyPromptDialog = true
                                        }
                                    },
                                    enabled = selectedStudentIds.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${selectedStudentIds.size} ÖĞRENCİYE YILDIZ VER (+${starCount * selectedStudentIds.size})",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
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
