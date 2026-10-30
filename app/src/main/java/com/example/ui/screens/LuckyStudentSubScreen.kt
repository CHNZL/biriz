package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ClassRoom
import com.example.data.model.Student
import com.example.ui.components.BentoCard
import com.example.ui.theme.BirizAmber
import com.example.ui.theme.BirizBorder
import com.example.ui.theme.BirizDarkCardBorder
import com.example.ui.theme.BirizDarkTextMuted
import com.example.ui.theme.BirizDarkTextPrimary
import com.example.ui.theme.BirizDarkTextSecondary
import com.example.ui.theme.BirizNavyDark
import com.example.ui.theme.BirizNavyPrimary
import com.example.ui.theme.BirizTextMuted
import com.example.ui.theme.BirizTextPrimary
import com.example.ui.theme.BirizTextSecondary
import com.example.ui.theme.BirizTurquoise
import com.example.util.LuckyStudentPersistenceHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class LuckyDrawMode(val title: String, val icon: String, val subtitle: String) {
    ROULETTE("Şans Çarkı", "🎡", "Animasyonlu hızlı isim çarkı"),
    MYSTERY_CARDS("Gizemli Kartlar", "🎴", "Dokunarak açılan gizli kartlar"),
    LIGHTNING("Şimşek Çekiliş", "⚡", "1 saniyede anında rastgele seçim")
}

@Composable
fun LuckyStudentSubScreen(
    classRoom: ClassRoom,
    students: List<Student>,
    isDarkTheme: Boolean = false,
    onBackToClassList: () -> Unit
) {
    BackHandler { onBackToClassList() }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 1. Kalıcı Durumu Yükle (SharedPreferences)
    val savedState = remember(classRoom.id) {
        LuckyStudentPersistenceHelper.loadState(context, classRoom.id)
    }

    var selectedMode by remember {
        mutableStateOf(
            savedState?.selectedModeName?.let { name ->
                try { LuckyDrawMode.valueOf(name) } catch (e: Exception) { LuckyDrawMode.ROULETTE }
            } ?: LuckyDrawMode.ROULETTE
        )
    }

    // Çekilen ve havuzdan çıkarılan öğrencilerin listesi (Geçmiş)
    val drawnStudentsHistory = remember {
        mutableStateListOf<Student>().apply {
            if (savedState != null) {
                val studentMap = students.associateBy { it.id }
                savedState.drawnStudentIds.mapNotNull { studentMap[it] }.forEach { add(it) }
            }
        }
    }

    // Havuzdan çıkarılan öğrenci ID'leri (Çekilenler + devamsız/muaf olanlar)
    val excludedStudentIds = remember {
        mutableStateListOf<String>().apply {
            if (savedState != null) {
                addAll(savedState.excludedStudentIds)
            }
        }
    }

    // Durumu diske kaydetme yardımcı fonksiyonu
    fun persistCurrentState() {
        LuckyStudentPersistenceHelper.saveState(
            context = context,
            classId = classRoom.id,
            drawnStudentIds = drawnStudentsHistory.map { it.id },
            excludedStudentIds = excludedStudentIds.toSet(),
            selectedModeName = selectedMode.name
        )
    }

    // Aktif aday havuzu (Henüz çekilmemiş ve devamsız olmayan öğrenciler)
    val candidatePool = remember(students, excludedStudentIds.toList()) {
        students.filter { it.id !in excludedStudentIds }
    }

    // Doğal sınıf listesi sırasına göre sıralı öğrenciler (Numaraya göre)
    val sortedStudentsByNumber = remember(students) {
        students.sortedBy { it.studentNumber.toIntOrNull() ?: Int.MAX_VALUE }
    }

    // Çekiliş animasyon durumları
    var isSpinning by remember { mutableStateOf(false) }
    var displayedRollingName by remember { mutableStateOf<String?>(null) }
    var displayedRollingNo by remember { mutableStateOf<String?>(null) }

    // Kazanan diyalog state'i
    var currentWinner by remember { mutableStateOf<Student?>(null) }
    var showWinnerDialog by remember { mutableStateOf(false) }

    // Alt Tab: 0 = Seçilenler (Kürsü), 1 = Kalan Adaylar
    var bottomActiveTab by remember { mutableIntStateOf(0) }

    // Kart seçim efekti
    var activeFlippingCardIndex by remember { mutableStateOf<Int?>(null) }

    // Çekiliş Çalıştırma Fonksiyonu
    fun pickWinner(
        isInstant: Boolean = false,
        onFinished: (Student) -> Unit = {}
    ) {
        if (candidatePool.isEmpty()) {
            Toast.makeText(context, "Aday havuzunda öğrenci kalmadı! Havuzu sıfırlayabilirsiniz.", Toast.LENGTH_SHORT).show()
            return
        }

        if (isSpinning) return

        isSpinning = true
        coroutineScope.launch {
            if (isInstant) {
                // Gizemli kartlar için anında, akıcı 250ms efekt
                delay(200)
            } else {
                val totalSteps = if (selectedMode == LuckyDrawMode.LIGHTNING) 7 else 20
                val delayBase = if (selectedMode == LuckyDrawMode.LIGHTNING) 50L else 45L

                for (i in 0 until totalSteps) {
                    val candidate = candidatePool.random()
                    displayedRollingName = candidate.fullName
                    displayedRollingNo = candidate.studentNumber
                    val currentDelay = if (selectedMode == LuckyDrawMode.LIGHTNING) delayBase else delayBase + (i * i * 0.4f).toLong()
                    delay(currentDelay)
                }
            }

            // Kazananı belirle
            val winner = candidatePool.random()
            displayedRollingName = winner.fullName
            displayedRollingNo = winner.studentNumber

            isSpinning = false
            activeFlippingCardIndex = null

            // Otomatik havuzdan çıkar ve geçmişe ekle
            excludedStudentIds.add(winner.id)
            drawnStudentsHistory.add(0, winner)
            persistCurrentState()

            currentWinner = winner
            showWinnerDialog = true
            onFinished(winner)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("screen_lucky_student")
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp), // Dock bar altında kalmayı kesinlikle önler
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // --- 1. ÇEKİLİŞ YÖNTEMİ SEÇİCİSİ (SEGMENTED BAR) ---
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isDarkTheme) Color(0x50061224) else Color(0xFFE2E8F0),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    LuckyDrawMode.entries.forEach { mode ->
                        val isSelected = selectedMode == mode
                        Surface(
                            onClick = {
                                if (!isSpinning) {
                                    selectedMode = mode
                                    persistCurrentState()
                                }
                            },
                            shape = RoundedCornerShape(11.dp),
                            color = if (isSelected) {
                                if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                            } else Color.Transparent,
                            shadowElevation = if (isSelected) 2.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = mode.icon,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = mode.title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (isSelected) {
                                        if (isDarkTheme) BirizNavyDark else Color.White
                                    } else {
                                        if (isDarkTheme) Color.LightGray else Color(0xFF475569)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 3. ANA ÇEKİLİŞ SAHNESİ (MODALARA GÖRE GELİŞMİŞ GÖRSEL TASARIM) ---
        item {
            BentoCard(
                isDark = isDarkTheme,
                glowColor = when (selectedMode) {
                    LuckyDrawMode.ROULETTE -> BirizAmber.copy(alpha = 0.35f)
                    LuckyDrawMode.MYSTERY_CARDS -> BirizTurquoise.copy(alpha = 0.4f)
                    LuckyDrawMode.LIGHTNING -> Color(0xFFE11D48).copy(alpha = 0.35f)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (selectedMode) {
                        // --- MOD 1: ŞANS ÇARKI (ROULETTE) ---
                        LuckyDrawMode.ROULETTE -> {
                            Text(
                                text = "🎡 Dijital İsim Çarkı",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDarkTheme) Color.White else BirizNavyPrimary
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Çark dönerken adaylar arasında hızla gezinir ve kazananı kilitler.",
                                fontSize = 11.sp,
                                color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Dönen İsim Vitrini
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = if (isDarkTheme) Color(0xFF0F243E) else Color(0xFFF1F5F9),
                                border = BorderStroke(
                                    2.dp,
                                    if (isSpinning) BirizAmber else if (isDarkTheme) BirizDarkCardBorder else BirizBorder
                                ),
                                shadowElevation = if (isSpinning) 8.dp else 2.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (candidatePool.isEmpty()) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "🎉 Tüm Sınıf Seçildi!",
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = BirizAmber
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Aday kutusundan 'Tümünü Seç' yaparak yeniden başlatabilirsiniz.",
                                                fontSize = 11.sp,
                                                color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                                            )
                                        }
                                    } else if (displayedRollingName != null) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        ) {
                                            Text(
                                                text = "NO: #${displayedRollingNo ?: ""}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BirizTurquoise
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = displayedRollingName ?: "",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (isDarkTheme) Color.White else BirizNavyPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    } else {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = BirizAmber,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Başlatmak için butona dokunun",
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isDarkTheme) BirizDarkTextMuted else BirizTextSecondary
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = { pickWinner(isInstant = false) },
                                enabled = !isSpinning && candidatePool.isNotEmpty(),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BirizAmber,
                                    contentColor = Color.White,
                                    disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("btn_start_roulette_draw")
                            ) {
                                Icon(
                                    imageVector = if (isSpinning) Icons.Default.Refresh else Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isSpinning) "Çark Dönüyor..." else "🎲 Şanslı Öğrenciyi Seç!",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // --- MOD 2: GİZEMLİ KARTLAR (MYSTERY CARDS - ANINDA TEPKİ & PRESTİJLİ TASARIM) ---
                        LuckyDrawMode.MYSTERY_CARDS -> {
                            Text(
                                text = "🎴 Gizemli Kartlar",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDarkTheme) Color.White else BirizNavyPrimary
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Tahtadaki kartlardan birine dokun; öğrenci kartın arkasından anında açılsın!",
                                fontSize = 11.sp,
                                color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            if (candidatePool.isEmpty()) {
                                Text(
                                    text = "🎉 Havuzdaki tüm öğrenciler seçildi!",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BirizAmber
                                )
                            } else {
                                // 6 Prestijli Gizemli Kart (2x3 Grid) - Altın Varaklı & BİRİZ Yıldızlı
                                val cardLabels = listOf("BİRİZ", "ŞANS", "YILDIZ", "BİLGİ", "CESARET", "ZAFER")
                                val cardGradients = listOf(
                                    listOf(Color(0xFF1E3A8A), Color(0xFF0F172A)),
                                    listOf(Color(0xFF065F46), Color(0xFF022C22)),
                                    listOf(Color(0xFF7C2D12), Color(0xFF451A03)),
                                    listOf(Color(0xFF581C87), Color(0xFF3B0764)),
                                    listOf(Color(0xFF831843), Color(0xFF500724)),
                                    listOf(Color(0xFF134E4A), Color(0xFF042F2E))
                                )

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    for (row in 0 until 2) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            for (col in 0 until 3) {
                                                val cardIndex = row * 3 + col
                                                val isCardActive = activeFlippingCardIndex == cardIndex

                                                Surface(
                                                    onClick = {
                                                        if (!isSpinning && candidatePool.isNotEmpty()) {
                                                            activeFlippingCardIndex = cardIndex
                                                            // Anında ve akıcı kart açılma
                                                            pickWinner(isInstant = true)
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(14.dp),
                                                    border = BorderStroke(
                                                        1.8.dp,
                                                        if (isCardActive) SolidColor(BirizAmber) else Brush.linearGradient(
                                                            listOf(BirizAmber.copy(alpha = 0.85f), BirizTurquoise.copy(alpha = 0.85f))
                                                        )
                                                    ),
                                                    shadowElevation = if (isCardActive) 6.dp else 3.dp,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(78.dp)
                                                        .testTag("mystery_card_$cardIndex")
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(
                                                                Brush.verticalGradient(cardGradients[cardIndex % cardGradients.size])
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Column(
                                                            horizontalAlignment = Alignment.CenterHorizontally,
                                                            verticalArrangement = Arrangement.Center
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Star,
                                                                contentDescription = null,
                                                                tint = BirizAmber,
                                                                modifier = Modifier.size(24.dp)
                                                            )
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Text(
                                                                text = cardLabels[cardIndex],
                                                                fontSize = 10.5.sp,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = Color(0xFFFDE68A),
                                                                letterSpacing = 1.sp
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

                        // --- MOD 3: ŞİMŞEK ÇEKİLİŞ (LIGHTNING DRAW) ---
                        LuckyDrawMode.LIGHTNING -> {
                            Text(
                                text = "⚡ Şimşek Hızlı Çekiliş",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDarkTheme) Color.White else BirizNavyPrimary
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Beklemeden tek dokunuşla doğrudan bir öğrenciyi tahtaya çağırır.",
                                fontSize = 11.sp,
                                color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Surface(
                                onClick = {
                                    if (!isSpinning && candidatePool.isNotEmpty()) {
                                        pickWinner(isInstant = false)
                                    }
                                },
                                shape = CircleShape,
                                color = if (isDarkTheme) Color(0x35E11D48) else Color(0xFFFFE4E6),
                                border = BorderStroke(2.dp, Color(0xFFE11D48)),
                                modifier = Modifier
                                    .size(76.dp)
                                    .testTag("btn_lightning_draw")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.FlashOn,
                                        contentDescription = "Şimşek Çekiliş",
                                        tint = Color(0xFFE11D48),
                                        modifier = Modifier.size(38.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isSpinning) "Belirleniyor..." else "Dokun ve Anında Seç",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE11D48)
                            )
                        }
                    }
                }
            }
        }

        // --- 4. ALT SEKMELER: [ 🏆 SEÇİLENLER ] | [ 👥 KALAN ADAYLAR ] ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDarkTheme) Color(0x50061224) else Color(0xFFE2E8F0))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    onClick = { bottomActiveTab = 0 },
                    shape = RoundedCornerShape(9.dp),
                    color = if (bottomActiveTab == 0) {
                        if (isDarkTheme) BirizNavyDark else Color.White
                    } else Color.Transparent,
                    shadowElevation = if (bottomActiveTab == 0) 2.dp else 0.dp,
                    modifier = Modifier.weight(1f).height(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "🏆 Seçilenler (${drawnStudentsHistory.size})",
                            fontSize = 12.sp,
                            fontWeight = if (bottomActiveTab == 0) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (bottomActiveTab == 0) {
                                if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                            } else {
                                if (isDarkTheme) Color.LightGray else Color(0xFF475569)
                            }
                        )
                    }
                }

                Surface(
                    onClick = { bottomActiveTab = 1 },
                    shape = RoundedCornerShape(9.dp),
                    color = if (bottomActiveTab == 1) {
                        if (isDarkTheme) BirizNavyDark else Color.White
                    } else Color.Transparent,
                    shadowElevation = if (bottomActiveTab == 1) 2.dp else 0.dp,
                    modifier = Modifier.weight(1f).height(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "👥 Kalan Adaylar (${candidatePool.size})",
                            fontSize = 12.sp,
                            fontWeight = if (bottomActiveTab == 1) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (bottomActiveTab == 1) {
                                if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                            } else {
                                if (isDarkTheme) Color.LightGray else Color(0xFF475569)
                            }
                        )
                    }
                }
            }
        }

        // --- 5. SEKME İÇERİĞİ ---
        if (bottomActiveTab == 0) {
            // SEKME 1: SEÇİLENLER (KÜRSÜ)
            if (drawnStudentsHistory.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDarkTheme) Color(0x2038BDF8) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted,
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Henüz Öğrenci Seçilmedi",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Çekiliş yaptığınızda seçilen öğrenciler sırayla burada kürsüye çıkar.",
                                fontSize = 11.sp,
                                color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(drawnStudentsHistory, key = { _, s -> "drawn_${s.id}" }) { index, student ->
                    val isKiz = student.gender.equals("Kız", ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = when {
                            isKiz -> if (isDarkTheme) Color(0x30F472B6) else Color.White
                            else -> if (isDarkTheme) Color(0x3038BDF8) else Color.White
                        },
                        border = BorderStroke(
                            1.dp,
                            when {
                                isKiz -> if (isDarkTheme) Color(0xFFF472B6) else Color(0xFFFCE7F3)
                                else -> if (isDarkTheme) Color(0xFF38BDF8) else Color(0xFFE0F2FE)
                            }
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
                                // Sıra Rozeti (1., 2., 3...)
                                Text(
                                    text = "${index + 1}.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    modifier = Modifier.width(22.dp)
                                )

                                // Okul No Rozeti
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

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = student.fullName,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDarkTheme) Color.White else Color(0xFF1E293B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Sağ: Cinsiyet Simgesi & Tekrar Havuza Geri Ekle Butonu
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (isKiz) "♀" else "♂",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isKiz) Color(0xFFEC4899) else Color(0xFF0284C7)
                                )

                                IconButton(
                                    onClick = {
                                        drawnStudentsHistory.remove(student)
                                        excludedStudentIds.remove(student.id)
                                        persistCurrentState()
                                        Toast.makeText(context, "${student.fullName} tekrar aday havuzuna eklendi.", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Undo,
                                        contentDescription = "Havuza Geri Al",
                                        tint = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // SEKME 2: KALAN ADAYLAR (SEÇİLENLER KARTI TARZINDA, DÜZENLİ VE SINIF SIRASINA GÖRE)
            item {
                // Üst Araç Çubuğu: [ Tümünü Seç ] | [ Tümünü Sil / Kaldır ]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            excludedStudentIds.clear()
                            persistCurrentState()
                            Toast.makeText(context, "Tüm öğrenciler aday havuzuna dahil edildi.", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDarkTheme) Color(0x3010B981) else Color(0xFFDCFCE7),
                            contentColor = if (isDarkTheme) Color(0xFF86EFAC) else Color(0xFF15803D)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF10B981)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Icon(Icons.Default.SelectAll, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tümünü Seç", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            excludedStudentIds.clear()
                            excludedStudentIds.addAll(students.map { it.id })
                            persistCurrentState()
                            Toast.makeText(context, "Tüm öğrenciler aday havuzundan çıkarıldı.", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDarkTheme) Color(0x30EF4444) else Color(0xFFFEE2E2),
                            contentColor = if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFEF4444)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f).height(36.dp)
                    ) {
                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tümünü Kaldır", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Sınıf Numarası Sırasına Göre Düzenli Kart Listesi
            itemsIndexed(sortedStudentsByNumber, key = { _, s -> "candidate_${s.id}" }) { _, student ->
                val isKiz = student.gender.equals("Kız", ignoreCase = true)
                val isInPool = student.id !in excludedStudentIds

                Surface(
                    onClick = {
                        if (isInPool) {
                            excludedStudentIds.add(student.id)
                            Toast.makeText(context, "${student.fullName} havuzdan çıkarıldı.", Toast.LENGTH_SHORT).show()
                        } else {
                            excludedStudentIds.remove(student.id)
                            drawnStudentsHistory.removeAll { it.id == student.id }
                            Toast.makeText(context, "${student.fullName} havuza eklendi.", Toast.LENGTH_SHORT).show()
                        }
                        persistCurrentState()
                    },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isInPool) {
                        if (isKiz) (if (isDarkTheme) Color(0x30F472B6) else Color.White) else (if (isDarkTheme) Color(0x3038BDF8) else Color.White)
                    } else {
                        if (isDarkTheme) Color(0x20EF4444) else Color(0xFFFFF1F2)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (isInPool) {
                            if (isKiz) (if (isDarkTheme) Color(0xFFF472B6) else Color(0xFFFCE7F3)) else (if (isDarkTheme) Color(0xFF38BDF8) else Color(0xFFE0F2FE))
                        } else {
                            Color(0x60EF4444)
                        }
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
                            // Okul No Rozeti
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

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = student.fullName,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isInPool) {
                                    if (isDarkTheme) Color.White else Color(0xFF1E293B)
                                } else {
                                    if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Cinsiyet Simgesi
                            Text(
                                text = if (isKiz) "♀" else "♂",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isKiz) Color(0xFFEC4899) else Color(0xFF0EA5E9)
                            )

                            // Aktiflik Durum Rozeti (Havuzda / Çıkarıldı)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isInPool) {
                                    if (isDarkTheme) Color(0x3010B981) else Color(0xFFDCFCE7)
                                } else {
                                    if (isDarkTheme) Color(0x20EF4444) else Color(0xFFFEE2E2)
                                },
                                border = BorderStroke(
                                    0.8.dp,
                                    if (isInPool) Color(0xFF10B981) else Color(0xFFEF4444)
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isInPool) Icons.Default.Check else Icons.Default.Close,
                                        contentDescription = null,
                                        tint = if (isInPool) (if (isDarkTheme) Color(0xFF86EFAC) else Color(0xFF15803D)) else Color(0xFFEF4444),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isInPool) "Havuzda" else "Çıkarıldı",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isInPool) {
                                            if (isDarkTheme) Color(0xFF86EFAC) else Color(0xFF15803D)
                                        } else {
                                            if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- 6. KAZANAN ÖĞRENCİ KUTLAMA MODAL DİYALOĞU ---
    if (showWinnerDialog && currentWinner != null) {
        val winner = currentWinner!!
        val isKiz = winner.gender.equals("Kız", ignoreCase = true)

        Dialog(onDismissRequest = { showWinnerDialog = false }) {
            BentoCard(
                isDark = isDarkTheme,
                glowColor = BirizAmber,
                shape = RoundedCornerShape(24.dp),
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
                        color = if (isDarkTheme) Color(0x30F59E0B) else Color(0xFFFEF3C7),
                        border = BorderStroke(2.dp, BirizAmber),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = BirizAmber,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "🎉 TEBRİKLER!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = BirizAmber
                    )
                    Text(
                        text = "Günün Şanslı Öğrencisi Belirlendi",
                        fontSize = 12.sp,
                        color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDarkTheme) Color(0xFF0F243E) else Color(0xFFF1F5F9),
                        border = BorderStroke(
                            1.5.dp,
                            if (isKiz) Color(0xFFF472B6) else Color(0xFF38BDF8)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isKiz) Color(0xFFFCE7F3) else Color(0xFFE0F2FE)
                            ) {
                                Text(
                                    text = "OKUL NO: #${winner.studentNumber}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isKiz) Color(0xFFBE185D) else Color(0xFF0284C7),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = winner.fullName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isDarkTheme) Color.White else BirizNavyPrimary,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "${classRoom.gradeLevel}/${classRoom.section} • ${winner.gender}",
                                fontSize = 12.sp,
                                color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDarkTheme) Color(0x2510B981) else Color(0xFFDCFCE7),
                        border = BorderStroke(1.dp, Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Bu öğrenci otomatik olarak havuzdan çıkarıldı. Sıradaki çekilişte diğer öğrenciler yarışacak.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDarkTheme) Color(0xFF86EFAC) else Color(0xFF15803D),
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = {
                                excludedStudentIds.remove(winner.id)
                                drawnStudentsHistory.remove(winner)
                                persistCurrentState()
                                showWinnerDialog = false
                                Toast.makeText(context, "${winner.fullName} tekrar havuza alındı.", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDarkTheme) Color(0x3038BDF8) else Color(0xFFE0F2FE),
                            border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF0284C7) else Color(0xFFBAE6FD)),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Havuza Geri Al",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkTheme) Color(0xFF7DD3FC) else Color(0xFF0369A1)
                                )
                            }
                        }

                        Button(
                            onClick = { showWinnerDialog = false },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BirizTurquoise,
                                contentColor = BirizNavyDark
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Text(
                                text = "Harika, Devam!",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}
