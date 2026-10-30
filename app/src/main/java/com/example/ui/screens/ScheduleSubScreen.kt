package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ClassRoom
import com.example.data.model.ScheduleConfig
import com.example.data.model.ScheduleLesson
import com.example.data.model.ScheduleTimelineItem
import com.example.ui.components.BentoCard
import com.example.util.TurkishTextHelper
import com.example.ui.theme.BirizAmber
import com.example.ui.theme.BirizBorder
import com.example.ui.theme.BirizDarkCardBorder
import com.example.ui.theme.BirizDarkTextMuted
import com.example.ui.theme.BirizDarkTextPrimary
import com.example.ui.theme.BirizDarkTextSecondary
import com.example.ui.theme.BirizError
import com.example.ui.theme.BirizNavyDark
import com.example.ui.theme.BirizNavyLight
import com.example.ui.theme.BirizNavyPrimary
import com.example.ui.theme.BirizTextMuted
import com.example.ui.theme.BirizTextPrimary
import com.example.ui.theme.BirizTextSecondary
import com.example.ui.theme.BirizTurquoise
import com.example.util.ExcelPdfHelper
import com.example.util.ScheduleHelper
import com.example.util.SchedulePersistenceHelper

@Composable
fun ScheduleSubScreen(
    classRoom: ClassRoom,
    scheduleConfig: ScheduleConfig,
    scheduleLessons: List<ScheduleLesson>,
    isSettingsActive: Boolean,
    activeDay: String,
    isDarkTheme: Boolean,
    onSaveConfig: (ScheduleConfig) -> Unit,
    onSaveLesson: (ScheduleLesson) -> Unit,
    onToggleSettingsView: (Boolean) -> Unit,
    onSelectDay: (String) -> Unit
) {
    val context = LocalContext.current

    if (isSettingsActive) {
        // --- 1. EKRAN: SOL - ESNEK PROGRAM AYARLARI ---
        ScheduleSettingsScreen(
            classRoom = classRoom,
            currentConfig = scheduleConfig,
            isDarkTheme = isDarkTheme,
            onBackToTable = { onToggleSettingsView(false) },
            onApplyConfig = { newConfig ->
                SchedulePersistenceHelper.saveScheduleConfig(context, classRoom.id, newConfig)
                onSaveConfig(newConfig)
                onToggleSettingsView(false)
            }
        )
    } else {
        // --- 2. EKRAN: SAĞ - DERS PROGRAMI ZAMAN ÇİZELGESİ (TABLOSU) ---
        ScheduleTableScreen(
            classRoom = classRoom,
            config = scheduleConfig,
            lessons = scheduleLessons,
            activeDay = activeDay,
            isDarkTheme = isDarkTheme,
            onOpenSettings = { onToggleSettingsView(true) },
            onSelectDay = { day ->
                SchedulePersistenceHelper.saveActiveDay(context, classRoom.id, day)
                onSelectDay(day)
            },
            onSaveLesson = { lesson ->
                onSaveLesson(lesson)
                val updatedLessons = scheduleLessons.toMutableList()
                val idx = updatedLessons.indexOfFirst { it.dayOfWeek == lesson.dayOfWeek && it.lessonIndex == lesson.lessonIndex }
                if (idx >= 0) {
                    updatedLessons[idx] = lesson
                } else {
                    updatedLessons.add(lesson)
                }
                SchedulePersistenceHelper.saveScheduleLessons(context, classRoom.id, updatedLessons)
            }
        )
    }
}

// ==============================================================================
// 1. EKRAN: PROGRAM AYARLARI (image_18.png Sol Telefon Görünümü)
// ==============================================================================

@Composable
fun ScheduleSettingsScreen(
    classRoom: ClassRoom,
    currentConfig: ScheduleConfig,
    isDarkTheme: Boolean,
    onBackToTable: () -> Unit,
    onApplyConfig: (ScheduleConfig) -> Unit
) {
    val context = LocalContext.current

    // Ayar State'leri - remember(currentConfig) ile dışarıdan gelen güncel ayarlar anında yansıtılır
    var lessonCount by remember(currentConfig) { mutableIntStateOf(currentConfig.dailyLessonCount) }
    var startTime by remember(currentConfig) { mutableStateOf(currentConfig.firstLessonStartTime) }
    var lessonDuration by remember(currentConfig) { mutableIntStateOf(currentConfig.lessonDurationMinutes) }

    // Teneffüs süreleri (ders sayısına göre esnek liste)
    var recessList by remember(currentConfig) {
        val initial = currentConfig.recessDurations.toMutableList()
        val neededRecesses = maxOf(0, currentConfig.dailyLessonCount - 1)
        while (initial.size < neededRecesses) {
            initial.add(15)
        }
        mutableStateOf(initial.take(neededRecesses))
    }

    // Ders sayısı değiştikçe teneffüs listesini uyarla
    fun updateLessonCount(newCount: Int) {
        if (newCount in 1..10) {
            lessonCount = newCount
            val needed = maxOf(0, newCount - 1)
            val updated = recessList.toMutableList()
            while (updated.size < needed) {
                updated.add(15)
            }
            recessList = updated.take(needed)
        }
    }

    var isLunchActive by remember(currentConfig) { mutableStateOf(currentConfig.isLunchBreakActive) }
    var lunchAfterLesson by remember(currentConfig) { mutableIntStateOf(currentConfig.lunchAfterLessonIndex) }
    var lunchDuration by remember(currentConfig) { mutableIntStateOf(currentConfig.lunchDurationMinutes) }

    fun buildCurrentConfig(): ScheduleConfig {
        return ScheduleConfig(
            dailyLessonCount = lessonCount,
            firstLessonStartTime = startTime,
            lessonDurationMinutes = lessonDuration,
            recessDurations = recessList,
            isLunchBreakActive = isLunchActive,
            lunchAfterLessonIndex = minOf(lunchAfterLesson, maxOf(1, lessonCount - 1)),
            lunchDurationMinutes = lunchDuration,
            isConfigured = true
        )
    }

    // Canlı Öğle Arası Saat Önizlemesi
    val lunchPreviewTimes = remember(startTime, lessonCount, lessonDuration, recessList, isLunchActive, lunchAfterLesson, lunchDuration) {
        val previewConfig = ScheduleConfig(
            dailyLessonCount = lessonCount,
            firstLessonStartTime = startTime,
            lessonDurationMinutes = lessonDuration,
            recessDurations = recessList,
            isLunchBreakActive = isLunchActive,
            lunchAfterLessonIndex = minOf(lunchAfterLesson, maxOf(1, lessonCount - 1)),
            lunchDurationMinutes = lunchDuration
        )
        ScheduleHelper.calculateLunchTimesPreview(previewConfig)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- ÜST BREADCRUMB & HIZLI KAYDET BUTONU ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    // Kullanıcı geri dönse dahi yaptığı değişiklikler otomatik olarak kaydedilir
                    val saved = buildCurrentConfig()
                    SchedulePersistenceHelper.saveScheduleConfig(context, classRoom.id, saved)
                    onApplyConfig(saved)
                    onBackToTable()
                },
                modifier = Modifier
                    .size(34.dp)
                    .testTag("btn_back_to_schedule_table")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Geri ve Kaydet",
                    tint = if (isDarkTheme) Color.White else BirizNavyPrimary
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isDarkTheme) Color(0x3028B8C2) else Color(0xFFE0F2FE)
            ) {
                Text(
                    text = "Ders Programı",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Text(
                text = "›",
                fontSize = 14.sp,
                color = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
            Text(
                text = "Esnek Ayarlar",
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
            )

            Spacer(modifier = Modifier.weight(1f))

            // Üst Hızlı Kaydet Butonu (Kaydırma yapmadan anında kaydeder)
            Button(
                onClick = {
                    val newConfig = buildCurrentConfig()
                    SchedulePersistenceHelper.saveScheduleConfig(context, classRoom.id, newConfig)
                    onApplyConfig(newConfig)
                    Toast.makeText(context, "Ders programı ayarları kaydedildi.", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("btn_top_save_schedule_config")
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Kaydet",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // --- 1. KART: ZAMAN VE DERS AYARLARI ---
        BentoCard(
            isDark = isDarkTheme,
            glowColor = BirizTurquoise.copy(alpha = 0.25f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Zaman ve Ders Ayarları",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Günlük Ders Sayısı & Stepper (- 6 +)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Günlük Ders Sayısı",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                        )
                        Text(
                            text = "Günde kaç blok ders yapılacak",
                            fontSize = 11.sp,
                            color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                        )
                    }

                    // Stepper (- 6 +) Kapsülü
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDarkTheme) BirizNavyDark else BirizNavyPrimary,
                        modifier = Modifier.testTag("stepper_lesson_count")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            IconButton(
                                onClick = { updateLessonCount(lessonCount - 1) },
                                modifier = Modifier.size(32.dp),
                                enabled = lessonCount > 1
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Azalt",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Text(
                                text = "$lessonCount",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            )

                            IconButton(
                                onClick = { updateLessonCount(lessonCount + 1) },
                                modifier = Modifier.size(32.dp),
                                enabled = lessonCount < 10
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Artır",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // İlk Ders Başlangıcı ve Ders Süresi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // İlk Ders Başlangıcı (08:30)
                    var showTimeMenu by remember { mutableStateOf(false) }
                    val commonTimes = listOf("08:00", "08:15", "08:30", "08:45", "09:00", "09:15")

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "İlk Ders Başlangıcı",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box {
                            Surface(
                                onClick = { showTimeMenu = true },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDarkTheme) Color(0x350A162B) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("btn_select_start_time")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = startTime,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkTheme) Color.White else BirizNavyPrimary
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = BirizTurquoise,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showTimeMenu,
                                onDismissRequest = { showTimeMenu = false }
                            ) {
                                commonTimes.forEach { t ->
                                    DropdownMenuItem(
                                        text = { Text(t, fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            startTime = t
                                            showTimeMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Ders Süresi (40 dk)
                    var showDurationMenu by remember { mutableStateOf(false) }
                    val durationOptions = listOf(30, 35, 40, 45, 50)

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ders Süresi",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box {
                            Surface(
                                onClick = { showDurationMenu = true },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDarkTheme) Color(0x350A162B) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("btn_select_lesson_duration")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "$lessonDuration dk",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkTheme) Color.White else BirizNavyPrimary
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = BirizTurquoise,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showDurationMenu,
                                onDismissRequest = { showDurationMenu = false }
                            ) {
                                durationOptions.forEach { d ->
                                    DropdownMenuItem(
                                        text = { Text("$d dakika", fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            lessonDuration = d
                                            showDurationMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 2. KART: TENEFFÜS & ÖĞLE ARASI AYARLARI ---
        BentoCard(
            isDark = isDarkTheme,
            glowColor = BirizAmber.copy(alpha = 0.35f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Teneffüs & Öğle Arası Ayarları",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Teneffüs Süreleri",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) BirizAmber else Color(0xFFB45309)
                    )

                    // "Tümünü Eşitle" Bağlantısı
                    Text(
                        text = "Tümünü Eşitle",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BirizTurquoise,
                        modifier = Modifier
                            .clickable {
                                val firstDuration = recessList.firstOrNull() ?: 15
                                recessList = List(recessList.size) { firstDuration }
                                Toast.makeText(context, "Tüm teneffüsler $firstDuration dk olarak eşitlendi.", Toast.LENGTH_SHORT).show()
                            }
                            .padding(4.dp)
                            .testTag("btn_equalize_recesses")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Her Teneffüs İçin Ayrı Esnek Süre Seçici
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val recessOptions = listOf(10, 15, 20, 25)

                    recessList.forEachIndexed { index, duration ->
                        var isExpanded by remember { mutableStateOf(false) }

                        Surface(
                            onClick = { isExpanded = true },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDarkTheme) Color(0x351E293B) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Coffee,
                                            contentDescription = null,
                                            tint = BirizAmber,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${index + 1}. Teneffüs: $duration dk",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isDarkTheme) Color.White else BirizNavyPrimary
                                        )
                                        if (index == 2) {
                                            Text(
                                                text = " (Değiştirilebilir)",
                                                fontSize = 11.sp,
                                                color = BirizTurquoise,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Değiştir",
                                        tint = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = isExpanded,
                                    onDismissRequest = { isExpanded = false }
                                ) {
                                    recessOptions.forEach { opt ->
                                        DropdownMenuItem(
                                            text = { Text("$opt dakika") },
                                            onClick = {
                                                val copy = recessList.toMutableList()
                                                copy[index] = opt
                                                recessList = copy
                                                isExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = if (isDarkTheme) BirizDarkCardBorder else BirizBorder)
                Spacer(modifier = Modifier.height(14.dp))

                // --- ÖĞLE ARASI AYARLARI ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Öğle Arası",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                        )
                        Text(
                            text = if (isLunchActive) "Aktif (Dersler arasına yemek molası eklenir)" else "Kapalı (Sadece teneffüsler uygulanır)",
                            fontSize = 11.sp,
                            color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isLunchActive) "Öğle Arası Var" else "Yok",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isLunchActive) BirizTurquoise else if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Switch(
                            checked = isLunchActive,
                            onCheckedChange = { isLunchActive = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = BirizTurquoise,
                                uncheckedThumbColor = Color.LightGray,
                                uncheckedTrackColor = Color.DarkGray
                            ),
                            modifier = Modifier.testTag("switch_lunch_break")
                        )
                    }
                }

                AnimatedVisibility(visible = isLunchActive) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Kaçıncı dersten sonra?
                            var showTimingMenu by remember { mutableStateOf(false) }
                            val maxLunchLesson = maxOf(1, lessonCount - 1)
                            val timingOptions = (1..maxLunchLesson).toList()

                            Column(modifier = Modifier.weight(1.2f)) {
                                Text(
                                    text = "Zamanlama (Timing)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box {
                                    Surface(
                                        onClick = { showTimingMenu = true },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isDarkTheme) Color(0x350A162B) else Color(0xFFF8FAFC),
                                        border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "$lunchAfterLesson. Dersten Sonra",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDarkTheme) Color.White else BirizNavyPrimary
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = null,
                                                tint = BirizTurquoise
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = showTimingMenu,
                                        onDismissRequest = { showTimingMenu = false }
                                    ) {
                                        timingOptions.forEach { opt ->
                                            DropdownMenuItem(
                                                text = { Text("$opt. Dersten Sonra") },
                                                onClick = {
                                                    lunchAfterLesson = opt
                                                    showTimingMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Süre (20 dakikadan 90 dakikaya 5'er dakikalık aralıklarla seçenekler)
                            var showDurationMenu by remember { mutableStateOf(false) }
                            val lunchDurOptions = (20..90 step 5).toList()

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Öğle Arası Süresi",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box {
                                    Surface(
                                        onClick = { showDurationMenu = true },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isDarkTheme) Color(0x350A162B) else Color(0xFFF8FAFC),
                                        border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "$lunchDuration dk",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDarkTheme) Color.White else BirizNavyPrimary
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = null,
                                                tint = BirizTurquoise
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = showDurationMenu,
                                        onDismissRequest = { showDurationMenu = false }
                                    ) {
                                        lunchDurOptions.forEach { dur ->
                                            DropdownMenuItem(
                                                text = { Text("$dur dakika") },
                                                onClick = {
                                                    lunchDuration = dur
                                                    showDurationMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Canlı Saat Önizleme Kartı (image_18.png gibi)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDarkTheme) Color(0x3028B8C2) else Color(0xFFE0F2FE),
                            border = BorderStroke(1.dp, BirizTurquoise.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Restaurant,
                                    contentDescription = null,
                                    tint = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$lunchAfterLesson. Dersten Sonra / ${lunchPreviewTimes.first} - ${lunchPreviewTimes.second} ($lunchDuration dk)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkTheme) Color.White else BirizNavyPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 3. İŞLEM BUTONU: PROGRAMI OLUŞTUR VE DÜZENLE ---
        val createBtnColor = if (isDarkTheme) BirizNavyDark else Color.White
        Button(
            onClick = {
                val newConfig = buildCurrentConfig()
                SchedulePersistenceHelper.saveScheduleConfig(context, classRoom.id, newConfig)
                onApplyConfig(newConfig)
                Toast.makeText(context, "Ders programı zaman akışı hesaplandı ve kaydedildi.", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("btn_save_and_generate_schedule"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                contentColor = createBtnColor
            )
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = createBtnColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Programı Oluştur ve Düzenle",
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = createBtnColor
            )
        }

        Spacer(modifier = Modifier.height(70.dp)) // Alt gezinme çubuğu payı
    }
}

// ==============================================================================
// 2. EKRAN: DERS PROGRAMI TABLOSU (image_18.png Sağ Telefon Görünümü)
// ==============================================================================

@Composable
fun ScheduleTableScreen(
    classRoom: ClassRoom,
    config: ScheduleConfig,
    lessons: List<ScheduleLesson>,
    activeDay: String,
    isDarkTheme: Boolean,
    onOpenSettings: () -> Unit,
    onSelectDay: (String) -> Unit,
    onSaveLesson: (ScheduleLesson) -> Unit
) {
    val context = LocalContext.current
    var editingLessonSlot by remember { mutableStateOf<ScheduleTimelineItem.LessonSlot?>(null) }
    var isShareMenuOpen by remember { mutableStateOf(false) }

    // Seçili günün derslerini filtrele
    val currentDayLessons = remember(lessons, activeDay) {
        lessons.filter { it.dayOfWeek == activeDay }
    }

    // Zaman Akışını (Timeline) Hesapla
    val timelineItems = remember(config, currentDayLessons) {
        ScheduleHelper.calculateTimeline(config, currentDayLessons)
    }

    // Canlı "Şu Anki Ders" & Teneffüs Durumunu Hesapla
    val liveStatus = remember(config, lessons) {
        ScheduleHelper.calculateLiveScheduleStatus(config, lessons)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // --- 1. ÜST BAŞLIK, "PDF İNDİR" VE "AYARLAR ⚙️" BUTONLARI ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Ders Programı",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                    )
                    Text(
                        text = "${classRoom.gradeLevel}/${classRoom.section} • ${classRoom.schoolName}",
                        fontSize = 11.sp,
                        color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // "Ayarlar ⚙️" Butonu
                    Surface(
                        onClick = onOpenSettings,
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDarkTheme) Color(0x35334E68) else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
                        modifier = Modifier.testTag("btn_schedule_settings_top")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Ayarlar",
                                tint = BirizTurquoise,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Ayarlar",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) Color.White else BirizNavyPrimary
                            )
                        }
                    }

                    // Standart Paylaş Butonu ve Menüsü
                    Box {
                        Button(
                            onClick = { isShareMenuOpen = true },
                            colors = ButtonDefaults.buttonColors(containerColor = BirizNavyPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("schedule_share_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Paylaş",
                                modifier = Modifier.size(15.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Paylaş", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        DropdownMenu(
                            expanded = isShareMenuOpen,
                            onDismissRequest = { isShareMenuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("📋 WhatsApp / SMS Metni Kopyala", fontSize = 12.sp) },
                                onClick = {
                                    isShareMenuOpen = false
                                    val shareText = ExcelPdfHelper.generateWeeklyScheduleShareText(classRoom, config, lessons)
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Haftalık Ders Programı", shareText))
                                    Toast.makeText(context, "Ders programı panoya kopyalandı! 📋", Toast.LENGTH_SHORT).show()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("📊 Excel (.xlsx) İndir / Paylaş", fontSize = 12.sp) },
                                onClick = {
                                    isShareMenuOpen = false
                                    try {
                                        val uri = ExcelPdfHelper.exportWeeklyScheduleExcel(context, classRoom, config, lessons)
                                        ExcelPdfHelper.shareFile(
                                            context = context,
                                            uri = uri,
                                            mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                            chooserTitle = "Haftalık Ders Programı Excel Dosyasını Paylaş"
                                        )
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("🖨️ PDF (A4) İndir / Yazdır", fontSize = 12.sp) },
                                onClick = {
                                    isShareMenuOpen = false
                                    try {
                                        val uri = ExcelPdfHelper.exportWeeklySchedulePdf(
                                            context = context,
                                            classRoom = classRoom,
                                            config = config,
                                            lessons = lessons
                                        )
                                        ExcelPdfHelper.shareFile(
                                            context = context,
                                            uri = uri,
                                            mimeType = "application/pdf",
                                            chooserTitle = "Haftalık Ders Programı PDF Dosyasını Paylaş"
                                        )
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // --- 1.5. CANLI "ŞU ANKİ DERS" & TENEFFÜS GÖSTERGE BANDI ---
        item {
            val liveBgColor = when (liveStatus.iconType) {
                "LESSON" -> if (isDarkTheme) Color(0x3028B8C2) else Color(0xFFF0FDF4)
                "RECESS", "LUNCH" -> if (isDarkTheme) Color(0x35F59E0B) else Color(0xFFFEF3C7)
                else -> if (isDarkTheme) Color(0x25334E68) else Color(0xFFF1F5F9)
            }
            val liveBorderColor = when (liveStatus.iconType) {
                "LESSON" -> if (isDarkTheme) BirizTurquoise else Color(0xFF86EFAC)
                "RECESS", "LUNCH" -> if (isDarkTheme) BirizAmber else Color(0xFFFCD34D)
                else -> if (isDarkTheme) BirizDarkCardBorder else BirizBorder
            }
            val liveTitleColor = when (liveStatus.iconType) {
                "LESSON" -> if (isDarkTheme) BirizTurquoise else Color(0xFF15803D)
                "RECESS", "LUNCH" -> if (isDarkTheme) BirizAmber else Color(0xFFB45309)
                else -> if (isDarkTheme) Color.White else BirizNavyPrimary
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = liveBgColor,
                border = BorderStroke(1.2.dp, liveBorderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .testTag("schedule_live_status_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (liveStatus.isLive) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (liveStatus.iconType == "LESSON") Color(0xFF22C55E) else Color(0xFFF59E0B))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = liveStatus.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = liveTitleColor
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = liveStatus.subtitle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF475569)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDarkTheme) Color(0x40000000) else Color.White.copy(alpha = 0.8f),
                        border = BorderStroke(0.5.dp, liveBorderColor)
                    ) {
                        Text(
                            text = if (liveStatus.isLive) "CANLI" else "BİLGİ",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = liveTitleColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // --- 2. GÜN SEÇİCİ SEKMELERİ (PZT, SAL, ÇAR, PER, CUM) ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDarkTheme) Color(0x350A162B) else Color(0xFFF1F5F9))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                ScheduleHelper.DAYS_OF_WEEK.forEach { dayKey ->
                    val isSelected = activeDay == dayKey
                    Surface(
                        onClick = { onSelectDay(dayKey) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) {
                            BirizTurquoise
                        } else Color.Transparent,
                        shadowElevation = if (isSelected) 1.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .testTag("tab_day_$dayKey")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = dayKey,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                color = if (isSelected) {
                                    BirizNavyDark
                                } else if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                            )
                        }
                    }
                }
            }
        }

        // --- 3. DİNAMİK DİKEY ZAMAN AKIŞI (TIMELINE) ---
        items(timelineItems) { item ->
            when (item) {
                is ScheduleTimelineItem.LessonSlot -> {
                    LessonSlotCard(
                        slot = item,
                        isDark = isDarkTheme,
                        onEdit = { editingLessonSlot = item }
                    )
                }

                is ScheduleTimelineItem.RecessSlot -> {
                    RecessSlotBanner(
                        slot = item,
                        isDark = isDarkTheme
                    )
                }

                is ScheduleTimelineItem.LunchBreakSlot -> {
                    LunchBreakBanner(
                        slot = item,
                        isDark = isDarkTheme
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(120.dp)) // Alt menü yüksekliği kadar boş alan (6. ders rahatça okunabilir)
        }
    }

    // --- DERS DÜZENLEME MODAL DİYALOĞU ---
    editingLessonSlot?.let { slot ->
        LessonEditDialog(
            slot = slot,
            dayKey = activeDay,
            isDarkTheme = isDarkTheme,
            onDismiss = { editingLessonSlot = null },
            onSave = { updatedLesson ->
                onSaveLesson(updatedLesson)
                editingLessonSlot = null
            }
        )
    }
}

// ==============================================================================
// TIMELINE BİLEŞENLERİ: DERS KARTI, TENEFFÜS BANDI, ÖĞLE ARASI BANDI
// ==============================================================================

@Composable
fun LessonSlotCard(
    slot: ScheduleTimelineItem.LessonSlot,
    isDark: Boolean,
    onEdit: () -> Unit
) {
    val lesson = slot.lesson
    val subjectName = TurkishTextHelper.toTurkishUpperCase(lesson?.subjectName ?: "DERS SEÇİLMEDİ")
    val theme = lesson?.colorTheme ?: ScheduleHelper.getColorThemeForSubject(subjectName)

    // 20 Renk Seçeneğine Dinamik Gradyan Desteği
    val themeColors = ScheduleHelper.getColorsForTheme(theme)
    val cardGradient = Brush.horizontalGradient(
        listOf(
            Color(android.graphics.Color.parseColor(themeColors.first)),
            Color(android.graphics.Color.parseColor(themeColors.second))
        )
    )

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .testTag("lesson_card_${slot.lessonIndex}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardGradient)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sol: Saat Rozeti (Kompakt hap görünümü)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White.copy(alpha = 0.22f),
                    modifier = Modifier.padding(end = 10.dp)
                ) {
                    Text(
                        text = "${slot.startTime} - ${slot.endTime}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }

                // Orta: Ders Sırası ve Adı (Örn: "1. Ders: Türkçe")
                Text(
                    text = "${slot.lessonIndex}. Ders: $subjectName",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                // Sağ: Düzenleme İkonu
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("btn_edit_lesson_${slot.lessonIndex}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Dersi Düzenle",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RecessSlotBanner(
    slot: ScheduleTimelineItem.RecessSlot,
    isDark: Boolean
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isDark) Color(0x22E67E22) else Color(0xFFFEF3C7),
        border = BorderStroke(1.dp, BirizAmber.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Coffee,
                    contentDescription = null,
                    tint = BirizAmber,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Teneffüs (${slot.durationMinutes} dk)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFFFBBF24) else Color(0xFF92400E)
                )
            }
            Text(
                text = "${slot.startTime} - ${slot.endTime}",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDark) BirizDarkTextMuted else BirizTextMuted
            )
        }
    }
}

@Composable
fun LunchBreakBanner(
    slot: ScheduleTimelineItem.LunchBreakSlot,
    isDark: Boolean
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isDark) Color(0x3028B8C2) else Color(0xFFE0F2FE),
        border = BorderStroke(1.dp, BirizTurquoise.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = BirizTurquoise,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Öğle Arası (${slot.durationMinutes} dk)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color.White else BirizNavyPrimary
                )
            }
            Text(
                text = "${slot.startTime} - ${slot.endTime}",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) BirizTurquoise else BirizNavyPrimary
            )
        }
    }
}

// ==============================================================================
// DERS DÜZENLEME MODAL DİYALOĞU
// ==============================================================================

@Composable
fun LessonEditDialog(
    slot: ScheduleTimelineItem.LessonSlot,
    dayKey: String,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onSave: (ScheduleLesson) -> Unit
) {
    val existing = slot.lesson
    var subjectNameInput by remember {
        mutableStateOf(
            existing?.subjectName?.let { TurkishTextHelper.toTurkishUpperCase(it) } ?: "TÜRKÇE"
        )
    }
    var selectedColor by remember { mutableStateOf(existing?.colorTheme ?: ScheduleHelper.getColorThemeForSubject(subjectNameInput)) }

    val dayFullName = ScheduleHelper.DAY_NAMES[dayKey] ?: dayKey

    Dialog(onDismissRequest = onDismiss) {
        BentoCard(
            isDark = isDarkTheme,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "${slot.lessonIndex}. Ders Düzenle",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                )
                Text(
                    text = "$dayFullName • ${slot.startTime} - ${slot.endTime}",
                    fontSize = 12.sp,
                    color = BirizTurquoise,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Hızlı Ders Seçim Çipleri
                Text(
                    text = "Hızlı Ders Seçimi",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(ScheduleHelper.PRESET_SUBJECTS) { subject ->
                        val upperSub = TurkishTextHelper.toTurkishUpperCase(subject)
                        val isSel = subjectNameInput.equals(upperSub, ignoreCase = true)
                        Surface(
                            onClick = {
                                subjectNameInput = upperSub
                                selectedColor = ScheduleHelper.getColorThemeForSubject(upperSub)
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) BirizTurquoise else if (isDarkTheme) Color(0x35334E68) else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isSel) BirizTurquoise else Color.Transparent)
                        ) {
                            Text(
                                text = upperSub,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) BirizNavyDark else if (isDarkTheme) Color.White else BirizTextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Ders Adı Girişi (Büyük Harf algılama & gösterme)
                OutlinedTextField(
                    value = subjectNameInput,
                    onValueChange = { subjectNameInput = TurkishTextHelper.toTurkishUpperCase(it) },
                    label = { Text("Ders Adı (BÜYÜK HARF)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = if (isDarkTheme) Color.White else BirizTextPrimary,
                        unfocusedTextColor = if (isDarkTheme) Color.White else BirizTextPrimary,
                        focusedBorderColor = BirizTurquoise,
                        unfocusedBorderColor = if (isDarkTheme) BirizDarkCardBorder else BirizBorder
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 20 Zengin Renk Seçeneği (2 Satır x 10 Renk)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Kart Renk Teması (20 Renk)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                    )

                    val activeTheme = ScheduleHelper.LESSON_COLOR_PALETTE.firstOrNull { it.key.equals(selectedColor, ignoreCase = true) }
                    val activeThemeName = activeTheme?.name ?: selectedColor
                    val activeColorHex = activeTheme?.primaryHex ?: "#13294B"
                    Text(
                        text = activeThemeName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(android.graphics.Color.parseColor(activeColorHex))
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Satır: 10 Renk
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ScheduleHelper.LESSON_COLOR_PALETTE.take(10).forEach { item ->
                            val isSelected = selectedColor.equals(item.key, ignoreCase = true)
                            val clr = Color(android.graphics.Color.parseColor(item.primaryHex))
                            Surface(
                                onClick = { selectedColor = item.key },
                                shape = CircleShape,
                                color = clr,
                                border = if (isSelected) BorderStroke(2.dp, Color.White) else BorderStroke(0.6.dp, Color.Black.copy(alpha = 0.15f)),
                                modifier = Modifier
                                    .size(26.dp)
                                    .testTag("color_picker_${item.key}")
                            ) {
                                if (isSelected) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color.White,
                                            modifier = Modifier.size(7.dp)
                                        ) {}
                                    }
                                }
                            }
                        }
                    }

                    // 2. Satır: 10 Renk
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ScheduleHelper.LESSON_COLOR_PALETTE.drop(10).take(10).forEach { item ->
                            val isSelected = selectedColor.equals(item.key, ignoreCase = true)
                            val clr = Color(android.graphics.Color.parseColor(item.primaryHex))
                            Surface(
                                onClick = { selectedColor = item.key },
                                shape = CircleShape,
                                color = clr,
                                border = if (isSelected) BorderStroke(2.dp, Color.White) else BorderStroke(0.6.dp, Color.Black.copy(alpha = 0.15f)),
                                modifier = Modifier
                                    .size(26.dp)
                                    .testTag("color_picker_${item.key}")
                            ) {
                                if (isSelected) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color.White,
                                            modifier = Modifier.size(7.dp)
                                        ) {}
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Butonlar: İptal ve Kaydet
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("İptal", color = if (isDarkTheme) Color.White else BirizNavyPrimary)
                    }

                    val saveBtnContent = if (isDarkTheme) BirizNavyDark else Color.White
                    Button(
                        onClick = {
                            val upperName = TurkishTextHelper.toTurkishUpperCase(subjectNameInput.trim()).ifEmpty { "DERS" }
                            val updated = ScheduleLesson(
                                dayOfWeek = dayKey,
                                lessonIndex = slot.lessonIndex,
                                subjectName = upperName,
                                topicOrNotes = "",
                                colorTheme = selectedColor
                            )
                            onSave(updated)
                        },
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("btn_save_lesson_modal"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                            contentColor = saveBtnContent
                        )
                    ) {
                        Text("Kaydet", fontWeight = FontWeight.Bold, color = saveBtnContent)
                    }
                }
            }
        }
    }
}
