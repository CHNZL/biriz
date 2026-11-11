package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.ClassRoom
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
import com.example.ui.theme.BirizSuccess
import com.example.ui.theme.BirizTextMuted
import com.example.ui.theme.BirizTextSecondary
import com.example.ui.theme.BirizTurquoise
import com.example.util.SoundAlertHelper
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Zamanlayıcı Modları
 */
enum class TimerClassroomMode(
    val title: String,
    val iconEmoji: String,
    val themeColor: Color,
    val defaultMinutes: Int,
    val presetMinutes: List<Int>,
    val description: String
) {
    QUICK(
        title = "Hızlı Sayaç",
        iconEmoji = "⏱️",
        themeColor = BirizTurquoise,
        defaultMinutes = 5,
        presetMinutes = listOf(1, 2, 3, 5, 10, 15),
        description = "Pratik ders içi geri sayım"
    ),
    EXAM(
        title = "Sınav Sayacı",
        iconEmoji = "📝",
        themeColor = Color(0xFF8B5CF6),
        defaultMinutes = 30,
        presetMinutes = listOf(15, 20, 30, 40, 45, 60),
        description = "Sessiz, odaklayıcı sınav süresi"
    ),
    GROUP(
        title = "Grup Etkinliği",
        iconEmoji = "👥",
        themeColor = Color(0xFF10B981),
        defaultMinutes = 10,
        presetMinutes = listOf(3, 5, 7, 10, 15, 20),
        description = "Takım ve grup çalışmaları için"
    ),
    READING(
        title = "Okuma Saati",
        iconEmoji = "📖",
        themeColor = BirizAmber,
        defaultMinutes = 20,
        presetMinutes = listOf(10, 15, 20, 25, 30, 40),
        description = "Sessiz kitap okuma seansı"
    )
}

/**
 * Modern, Estetik ve Fonksiyonel Akıllı Sınıf Zamanlayıcısı
 */
@Composable
fun TimerSubScreen(
    classRoom: ClassRoom,
    isDarkTheme: Boolean,
    onBackToClassList: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Geri tuşu basıldığında Sınıf Listesine dön
    BackHandler {
        onBackToClassList()
    }

    // Aktif Mod
    var selectedMode by remember { mutableStateOf(TimerClassroomMode.QUICK) }

    // Süre Durumları (Saniye cinsinden)
    var totalSeconds by remember { mutableIntStateOf(selectedMode.defaultMinutes * 60) }
    var remainingSeconds by remember { mutableIntStateOf(selectedMode.defaultMinutes * 60) }
    var isRunning by remember { mutableStateOf(false) }

    // Tercihler (Sesli Uyarı & Titreşim)
    var isSoundEnabled by remember { mutableStateOf(true) }
    var isVibrationEnabled by remember { mutableStateOf(true) }

    // Tam Ekran Sunum Modu
    var isFullscreenMode by remember { mutableStateOf(false) }

    // Özel Süre Ayarlama Penceresi
    var showCustomDurationDialog by remember { mutableStateOf(false) }

    // Süre Bittiğinde Kutlama Modalı
    var showTimeUpDialog by remember { mutableStateOf(false) }

    // Firestore Akıllı Tahta Oturumu Senkronizasyonu
    fun syncTimerToSmartBoard(isFinished: Boolean = false) {
        coroutineScope.launch {
            try {
                val db = com.example.util.FirestoreProvider.get(context)
                val prefs = context.getSharedPreferences("biriz_board_sync", Context.MODE_PRIVATE)
                val activeCode = prefs.getString("active_session_code", null)
                if (!activeCode.isNullOrBlank()) {
                    val payload = mapOf(
                        "timerMode" to selectedMode.title,
                        "timerModeEmoji" to selectedMode.iconEmoji,
                        "timerTotalSeconds" to totalSeconds,
                        "timerRemainingSeconds" to remainingSeconds,
                        "timerIsRunning" to isRunning,
                        "timerIsFinished" to isFinished,
                        "updatedAt" to System.currentTimeMillis()
                    )
                    db.collection("board_sessions").document(activeCode).set(payload, com.google.firebase.firestore.SetOptions.merge())
                }
            } catch (_: Exception) {}
        }
    }

    // Zamanlayıcı Motoru (1 saniye aralıklarla çalışır)
    LaunchedEffect(isRunning, remainingSeconds) {
        if (isRunning && remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds -= 1
            syncTimerToSmartBoard(isFinished = false)
        } else if (isRunning && remainingSeconds <= 0) {
            isRunning = false
            SoundAlertHelper.playTimerCompleteAlert(context, playSound = isSoundEnabled, vibrate = isVibrationEnabled)
            showTimeUpDialog = true
            syncTimerToSmartBoard(isFinished = true)
        }
    }

    // Kalan Süre Oranı (0.0f - 1.0f)
    val progress = remember(remainingSeconds, totalSeconds) {
        if (totalSeconds > 0) {
            (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
        } else 0f
    }

    // Pürüzsüz yay animasyonu
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 900, easing = LinearEasing),
        label = "timerProgress"
    )

    // Süre durumuna göre renk değişimi (Normal -> Son %20 Kehribar -> Son 10 saniye Kırmızı)
    val currentColor = when {
        remainingSeconds <= 10 -> BirizError
        progress <= 0.20f -> BirizAmber
        else -> selectedMode.themeColor
    }

    // Son 10 saniyede nabız (pulse) efekti
    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRunning && remainingSeconds in 1..10) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Formatlanmış Dijital Süre Metni (Örn: 05:00 veya 01:15:30)
    val formattedTime = remember(remainingSeconds) {
        val hours = remainingSeconds / 3600
        val minutes = (remainingSeconds % 3600) / 60
        val seconds = remainingSeconds % 60
        if (hours > 0) {
            String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    // =========================================================================
    // 1. TAM EKRAN SUNUM MODU (MASAYA YATIRILDIĞINDA DEVASA GÖRÜNÜM)
    // =========================================================================
    if (isFullscreenMode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDarkTheme) Color(0xFF030712) else Color(0xFF0F172A))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            // Sağ Üst: Tam Ekrandan Çık Butonu
            IconButton(
                onClick = { isFullscreenMode = false },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0x30FFFFFF))
            ) {
                Icon(
                    imageVector = Icons.Default.FullscreenExit,
                    contentDescription = "Tam Ekrandan Çık",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Sol Üst: Aktif Mod Bilgisi
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .clip(RoundedCornerShape(20.dp))
                    .background(currentColor.copy(alpha = 0.2f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(selectedMode.iconEmoji, fontSize = 18.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = selectedMode.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Merkez: Devasa Sayaç Çemberi
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(310.dp)
                        .scale(pulseScale),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 18.dp.toPx()
                        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                        val arcOffset = Offset(strokeWidth / 2, strokeWidth / 2)

                        // Arka plan soluk çember
                        drawArc(
                            color = currentColor.copy(alpha = 0.15f),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = arcOffset,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // İlerleyen parlak yay
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(currentColor.copy(alpha = 0.7f), currentColor)
                            ),
                            startAngle = -90f,
                            sweepAngle = 360f * animatedProgress,
                            useCenter = false,
                            topLeft = arcOffset,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formattedTime,
                            fontSize = if (formattedTime.length > 5) 44.sp else 58.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when {
                                remainingSeconds == 0 -> "SÜRE DOLDU! 🔔"
                                isRunning -> "GERİ SAYILIYOR 🟢"
                                else -> "DURAKLATILDI ⏸️"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = currentColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                // Tam Ekran Kontrolleri (Başlat / Duraklat / Sıfırla)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sıfırla
                    Button(
                        onClick = {
                            isRunning = false
                            remainingSeconds = totalSeconds
                            syncTimerToSmartBoard(isFinished = false)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x30FFFFFF)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(54.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sıfırla", tint = Color.White, modifier = Modifier.size(24.dp))
                    }

                    // Başlat / Duraklat
                    Button(
                        onClick = {
                            if (remainingSeconds == 0) {
                                remainingSeconds = totalSeconds
                            }
                            isRunning = !isRunning
                            syncTimerToSmartBoard(isFinished = false)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = currentColor),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .height(58.dp)
                            .width(180.dp)
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isRunning) "Duraklat" else "Başlat",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    // +1 Dk Ekle
                    Button(
                        onClick = {
                            remainingSeconds += 60
                            totalSeconds += 60
                            syncTimerToSmartBoard(isFinished = false)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x30FFFFFF)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(54.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("+1'", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
        return
    }

    // =========================================================================
    // 2. STANDART EKRAN DÜZENİ
    // =========================================================================
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkTheme) Color(0xFF090D16) else Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag("timer_sub_screen")
    ) {
        // --- 1. ÜST BAŞLIK VE HIZLI AYARLAR BARI ---
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isDarkTheme) BirizDarkSurfaceGlass else Color.White,
            border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Sol: Geri Dön & Başlık
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBackToClassList,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isDarkTheme) Color(0x301E293B) else Color(0xFFF1F5F9))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri Dön",
                            tint = if (isDarkTheme) Color.White else BirizNavyPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Akıllı Zamanlayıcı",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                        )
                        Text(
                            text = "${classRoom.schoolName} • ${classRoom.gradeLevel}/${classRoom.section}",
                            fontSize = 10.5.sp,
                            color = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted
                        )
                    }
                }

                // Sağ: Hızlı Araçlar (Ses, Titreşim, Tam Ekran)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Ses Aç/Kapa
                    Surface(
                        onClick = { isSoundEnabled = !isSoundEnabled },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSoundEnabled) {
                            if (isDarkTheme) Color(0x3010B981) else Color(0xFFECFDF5)
                        } else {
                            if (isDarkTheme) Color(0x20334E68) else Color(0xFFF1F5F9)
                        },
                        border = BorderStroke(1.dp, if (isSoundEnabled) Color(0xFF10B981) else Color(0xFFCBD5E1)),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isSoundEnabled) Icons.Default.VolumeUp else Icons.Default.NotificationsOff,
                                contentDescription = "Ses",
                                tint = if (isSoundEnabled) Color(0xFF10B981) else Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Titreşim Aç/Kapa
                    Surface(
                        onClick = { isVibrationEnabled = !isVibrationEnabled },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isVibrationEnabled) {
                            if (isDarkTheme) Color(0x303B82F6) else Color(0xFFEFF6FF)
                        } else {
                            if (isDarkTheme) Color(0x20334E68) else Color(0xFFF1F5F9)
                        },
                        border = BorderStroke(1.dp, if (isVibrationEnabled) Color(0xFF3B82F6) else Color(0xFFCBD5E1)),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Vibration,
                                contentDescription = "Titreşim",
                                tint = if (isVibrationEnabled) Color(0xFF3B82F6) else Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Tam Ekran Modu
                    Surface(
                        onClick = { isFullscreenMode = true },
                        shape = RoundedCornerShape(10.dp),
                        color = BirizNavyPrimary,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "Tam Ekran Sunum Modu",
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- 2. MOD SEÇİCİ SEKMELERİ (HIZLI SAYAÇ, SINAV, GRUP, OKUMA) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimerClassroomMode.values().forEach { mode ->
                val isSelected = selectedMode == mode
                Surface(
                    onClick = {
                        if (!isRunning) {
                            selectedMode = mode
                            totalSeconds = mode.defaultMinutes * 60
                            remainingSeconds = totalSeconds
                            syncTimerToSmartBoard(isFinished = false)
                        } else {
                            Toast.makeText(context, "Mod değiştirmek için önce sayacı duraklatın.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) {
                        mode.themeColor
                    } else {
                        if (isDarkTheme) Color(0x201E293B) else Color.White
                    },
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) mode.themeColor else if (isDarkTheme) BirizDarkCardBorder else BirizBorder
                    ),
                    modifier = Modifier.height(42.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(mode.iconEmoji, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = mode.title,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            color = if (isSelected) Color.White else if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 3. MERKEZİ DİJİTAL NEON DAİRESEL SAYAÇ (HERO CANVAS) ---
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = if (isDarkTheme) BirizDarkSurfaceGlass else Color.White,
            border = BorderStroke(1.2.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Mod Başlığı & Açıklaması
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = currentColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, currentColor.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(selectedMode.iconEmoji, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${selectedMode.title} • ${selectedMode.description}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = currentColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Dairesel Neon İlerleme Halkası
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .scale(pulseScale),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 14.dp.toPx()
                        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                        val arcOffset = Offset(strokeWidth / 2, strokeWidth / 2)

                        // Arka Plan Rayı
                        drawArc(
                            color = if (isDarkTheme) Color(0x20334E68) else Color(0xFFF1F5F9),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = arcOffset,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // Dinamik İlerleyen Neon Yay
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(currentColor.copy(alpha = 0.6f), currentColor)
                            ),
                            startAngle = -90f,
                            sweepAngle = 360f * animatedProgress,
                            useCenter = false,
                            topLeft = arcOffset,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    // Dairenin İçindeki Büyük Dijital Saat ve Durum
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = formattedTime,
                            fontSize = if (formattedTime.length > 5) 34.sp else 46.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isDarkTheme) Color.White else BirizNavyPrimary,
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // Durum Etiketi
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            remainingSeconds == 0 -> BirizError
                                            isRunning -> BirizSuccess
                                            else -> BirizAmber
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = when {
                                    remainingSeconds == 0 -> "Süre Tamamlandı"
                                    isRunning -> "Geri Sayılıyor"
                                    else -> "Duraklatıldı"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Özel Süre Ayarla Butonu (Saat / Dk düzenleme)
                        Surface(
                            onClick = { showCustomDurationDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDarkTheme) Color(0x301E293B) else Color(0xFFF8FAFC),
                            border = BorderStroke(0.8.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFFCBD5E1))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp), tint = currentColor)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Süre Ayarla", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = currentColor)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- 4. ANA KONTROL BUTONLARI (BAŞLAT/DURAKLAT, SIFIRLA, +1', +5') ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sıfırla Butonu
                    Surface(
                        onClick = {
                            isRunning = false
                            remainingSeconds = totalSeconds
                            syncTimerToSmartBoard(isFinished = false)
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDarkTheme) Color(0x301E293B) else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .size(50.dp)
                            .testTag("btn_timer_reset")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sıfırla",
                                tint = if (isDarkTheme) Color.White else BirizNavyPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // +1 Dk Ekle
                    Surface(
                        onClick = {
                            remainingSeconds += 60
                            totalSeconds += 60
                            syncTimerToSmartBoard(isFinished = false)
                            Toast.makeText(context, "+1 Dakika eklendi", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDarkTheme) Color(0x253B82F6) else Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, if (isDarkTheme) Color(0x403B82F6) else Color(0xFFBFDBFE)),
                        modifier = Modifier
                            .height(50.dp)
                            .padding(horizontal = 2.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp)) {
                            Text("+1 Dk", fontSize = 13.sp, fontWeight = FontWeight.Black, color = if (isDarkTheme) Color(0xFF93C5FD) else Color(0xFF1D4ED8))
                        }
                    }

                    // BÜYÜK BAŞLAT / DURAKLAT ANA BUTON
                    Button(
                        onClick = {
                            if (remainingSeconds <= 0) {
                                remainingSeconds = totalSeconds
                            }
                            isRunning = !isRunning
                            syncTimerToSmartBoard(isFinished = false)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRunning) BirizAmber else currentColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .width(136.dp)
                            .testTag("btn_timer_toggle")
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isRunning) "DURAKLAT" else "BAŞLAT",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    // +5 Dk Ekle
                    Surface(
                        onClick = {
                            remainingSeconds += 300
                            totalSeconds += 300
                            syncTimerToSmartBoard(isFinished = false)
                            Toast.makeText(context, "+5 Dakika eklendi", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDarkTheme) Color(0x2510B981) else Color(0xFFECFDF5),
                        border = BorderStroke(1.dp, if (isDarkTheme) Color(0x4010B981) else Color(0xFFA7F3D0)),
                        modifier = Modifier
                            .height(50.dp)
                            .padding(horizontal = 2.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp)) {
                            Text("+5 Dk", fontSize = 13.sp, fontWeight = FontWeight.Black, color = if (isDarkTheme) Color(0xFF6EE7B7) else Color(0xFF047857))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 5. HAZIR SÜRE SEÇENEKLERİ (PRESET CHIPS) ---
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isDarkTheme) BirizDarkSurfaceGlass else Color.White,
            border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "⚡ Hızlı Süre Seçenekleri (${selectedMode.title})",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                    )
                    Text(
                        text = "Dokunun ve Başlatın",
                        fontSize = 10.5.sp,
                        color = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    selectedMode.presetMinutes.forEach { mins ->
                        val isPresetActive = totalSeconds == mins * 60
                        Surface(
                            onClick = {
                                isRunning = false
                                totalSeconds = mins * 60
                                remainingSeconds = mins * 60
                                syncTimerToSmartBoard(isFinished = false)
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isPresetActive) {
                                currentColor
                            } else {
                                if (isDarkTheme) Color(0x201E293B) else Color(0xFFF8FAFC)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isPresetActive) currentColor else if (isDarkTheme) BirizDarkCardBorder else Color(0xFFE2E8F0)
                            ),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            ) {
                                Text(
                                    text = "$mins Dk",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isPresetActive) Color.White else if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 6. AKILLI TAHTA CANLI YANSITMA BİLGİ KARTI ---
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isDarkTheme) Color(0x203B82F6) else Color(0xFFEFF6FF),
            border = BorderStroke(1.dp, if (isDarkTheme) Color(0x403B82F6) else Color(0xFFBFDBFE)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isDarkTheme) Color(0x403B82F6) else Color(0xFFDBEAFE),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🖥️", fontSize = 18.sp)
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Akıllı Tahtaya Canlı Yansıtma",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDarkTheme) Color(0xFF93C5FD) else Color(0xFF1D4ED8)
                    )
                    Text(
                        text = "Telefondan başlattığınız veya duraklattığınız sayaç, bilgisayar ekranındaki tahtada anlık saniye saniye görünür.",
                        fontSize = 11.sp,
                        color = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF475569),
                        lineHeight = 15.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp)) // Floating dock payı
    }

    // =========================================================================
    // 3. ÖZEL SÜRE AYARLAMA DİYALOĞU
    // =========================================================================
    if (showCustomDurationDialog) {
        var inputMinutes by remember { mutableStateOf((totalSeconds / 60).toString()) }
        var inputSeconds by remember { mutableStateOf((totalSeconds % 60).toString()) }

        Dialog(
            onDismissRequest = { showCustomDurationDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isDarkTheme) Color(0xFF0F172A) else Color.White,
                border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
                shadowElevation = 16.dp,
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Özel Süre Belirleyin",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDarkTheme) Color.White else BirizNavyPrimary
                    )
                    Text(
                        text = "Dakika ve saniye değerini girin",
                        fontSize = 11.sp,
                        color = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Dakika
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("DAKİKA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = currentColor)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = inputMinutes,
                                onValueChange = { if (it.length <= 3 && it.all { ch -> ch.isDigit() }) inputMinutes = it },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center,
                                    color = if (isDarkTheme) Color.White else BirizNavyPrimary
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = currentColor,
                                    unfocusedBorderColor = if (isDarkTheme) BirizDarkCardBorder else Color(0xFFCBD5E1)
                                ),
                                modifier = Modifier.width(84.dp)
                            )
                        }

                        Text(":", fontSize = 28.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 12.dp))

                        // Saniye
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("SANİYE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = currentColor)
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = inputSeconds,
                                onValueChange = { if (it.length <= 2 && it.all { ch -> ch.isDigit() }) inputSeconds = it },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    textAlign = TextAlign.Center,
                                    color = if (isDarkTheme) Color.White else BirizNavyPrimary
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = currentColor,
                                    unfocusedBorderColor = if (isDarkTheme) BirizDarkCardBorder else Color(0xFFCBD5E1)
                                ),
                                modifier = Modifier.width(84.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showCustomDurationDialog = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Vazgeç")
                        }

                        Button(
                            onClick = {
                                val mins = inputMinutes.toIntOrNull() ?: 0
                                val secs = inputSeconds.toIntOrNull() ?: 0
                                val total = (mins * 60 + secs).coerceAtLeast(5)
                                isRunning = false
                                totalSeconds = total
                                remainingSeconds = total
                                syncTimerToSmartBoard(isFinished = false)
                                showCustomDurationDialog = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = currentColor),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Ayarla", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // 4. SÜRE BİTİŞ KUTLAMA MODALI
    // =========================================================================
    if (showTimeUpDialog) {
        AlertDialog(
            onDismissRequest = { showTimeUpDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔔", fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Süre Tamamlandı!", fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
            },
            text = {
                Column {
                    Text(
                        text = "${selectedMode.title} için belirlenen süre doldu. Öğrencileriniz çalışmayı tamamlayabilir.",
                        fontSize = 13.sp,
                        color = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "🎉 Tebrikler! Seans başarıyla sonlandırıldı.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = currentColor
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showTimeUpDialog = false
                        remainingSeconds = totalSeconds
                        isRunning = true
                        syncTimerToSmartBoard(isFinished = false)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = currentColor)
                ) {
                    Text("Yeniden Başlat", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimeUpDialog = false }) {
                    Text("Tamam")
                }
            }
        )
    }
}
