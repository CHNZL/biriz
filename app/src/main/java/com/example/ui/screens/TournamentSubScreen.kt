package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActivityPreset
import com.example.data.model.ClassRoom
import com.example.data.model.Student
import com.example.data.model.Tournament
import com.example.data.model.TournamentMatch
import com.example.data.model.TournamentMatchType
import com.example.data.model.TournamentStanding
import com.example.data.model.TournamentStarRewards
import com.example.data.model.TournamentStatus
import com.example.data.model.TournamentType
import com.example.data.model.WinnerSelectionMethod
import com.example.ui.theme.BirizBorder
import com.example.ui.theme.BirizDarkBgBottom
import com.example.ui.theme.BirizDarkBgTop
import com.example.ui.theme.BirizDarkCardBorder
import com.example.ui.theme.BirizDarkCardGlass
import com.example.ui.theme.BirizDarkTextMuted
import com.example.ui.theme.BirizDarkTextPrimary
import com.example.ui.theme.BirizDarkTextSecondary
import com.example.ui.theme.BirizError
import com.example.ui.theme.BirizNavyDark
import com.example.ui.theme.BirizNavyPrimary
import com.example.ui.theme.BirizTextPrimary
import com.example.ui.theme.BirizTextSecondary
import com.example.ui.theme.BirizTurquoise
import com.example.ui.viewmodel.DashboardSubPage
import com.example.util.ExcelPdfHelper
import com.example.util.TournamentEngine
import com.example.util.TournamentPersistenceHelper

private val BirizGold = Color(0xFFF59E0B)
private val BirizSilver = Color(0xFF94A3B8)
private val BirizBronze = Color(0xFFB45309)

@Composable
fun TournamentSubScreen(
    classRoom: ClassRoom,
    students: List<Student>,
    currentSubPage: DashboardSubPage = DashboardSubPage.TURNUVA_DEVAM_EDENLER,
    isDarkTheme: Boolean = false,
    onSelectSubPage: (DashboardSubPage) -> Unit = {},
    onBackToClassList: () -> Unit
) {
    val context = LocalContext.current

    var tournaments by remember { mutableStateOf<List<Tournament>>(emptyList()) }
    var selectedTournament by remember { mutableStateOf<Tournament?>(null) }
    var selectedTournamentMatches by remember { mutableStateOf<List<TournamentMatch>>(emptyList()) }

    var searchQuery by remember { mutableStateOf("") }
    var showCreateWizard by remember { mutableStateOf(false) }
    var matchToScore by remember { mutableStateOf<TournamentMatch?>(null) }
    var tournamentToDelete by remember { mutableStateOf<Tournament?>(null) }
    var showPodiumDialog by remember { mutableStateOf(false) }

    fun refreshData() {
        tournaments = TournamentPersistenceHelper.loadTournaments(context, classRoom.id)
        if (selectedTournament != null) {
            val updatedT = tournaments.find { it.id == selectedTournament!!.id }
            if (updatedT != null) {
                selectedTournament = updatedT
                selectedTournamentMatches = TournamentPersistenceHelper.loadMatches(context, updatedT.id)
            } else {
                selectedTournament = null
                selectedTournamentMatches = emptyList()
            }
        }
    }

    LaunchedEffect(classRoom.id) {
        refreshData()
    }

    // Geri tuşu yönetimi
    BackHandler {
        if (selectedTournament != null) {
            selectedTournament = null
        } else {
            onBackToClassList()
        }
    }

    val cardBg = if (isDarkTheme) BirizDarkCardGlass else Color.White
    val cardBorder = if (isDarkTheme) BirizDarkCardBorder else BirizBorder
    val primaryText = if (isDarkTheme) BirizDarkTextPrimary else BirizTextPrimary
    val secondaryText = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary

    val activeTournaments = tournaments.filter { it.status == TournamentStatus.DEVAM_EDIYOR }
    val completedTournaments = tournaments.filter { it.status == TournamentStatus.TAMAMLANDI }

    val displayTournaments = remember(tournaments, currentSubPage, searchQuery) {
        val baseList = if (currentSubPage == DashboardSubPage.TURNUVA_TAMAMLANANLAR) completedTournaments else activeTournaments
        if (searchQuery.isBlank()) baseList else {
            baseList.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.description.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isDarkTheme) listOf(BirizDarkBgTop, BirizDarkBgBottom) else listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9))
                )
            )
            .testTag("tournament_sub_screen_root")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 68.dp) // Space for bottom dock
        ) {
            if (selectedTournament == null) {
                // =========================================================
                // TURNUVA LİSTESİ VE ÜST AKSİYON ÇUBUĞU
                // =========================================================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    // Arama & Turnuva Oluştur Satırı
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("tournament_search_input"),
                            placeholder = { Text("Turnuva adı veya branş ara...", fontSize = 12.sp, maxLines = 1) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = "Ara", modifier = Modifier.size(17.dp))
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Clear, contentDescription = "Temizle", modifier = Modifier.size(15.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                                unfocusedBorderColor = if (isDarkTheme) Color(0x5064748B) else BirizBorder
                            )
                        )

                        // + Turnuva Oluştur Butonu
                        Button(
                            onClick = { showCreateWizard = true },
                            modifier = Modifier
                                .height(46.dp)
                                .testTag("tournament_create_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Turnuva Ekle", tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Turnuva Kur", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (displayTournaments.isEmpty()) {
                        // Boş Ekran
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isDarkTheme) Color(0x25334E68) else Color.White,
                                border = BorderStroke(1.dp, if (isDarkTheme) Color(0x40334E68) else BirizBorder),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isDarkTheme) Color(0x3506B6D4) else Color(0xFFE0F2FE),
                                        modifier = Modifier.size(64.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(text = "🏆", fontSize = 32.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = if (searchQuery.isNotEmpty()) "Arama kriterine uygun turnuva bulunamadı" else if (currentSubPage == DashboardSubPage.TURNUVA_TAMAMLANANLAR) "Henüz Tamamlanan Turnuva Yok" else "Sınıfta Henüz Aktif Turnuva Yok",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = primaryText
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (searchQuery.isNotEmpty())
                                            "Farklı bir turnuva adı ile tekrar arayabilirsiniz."
                                        else
                                            "Satranç, masa tenisi, mangala, futbol veya bilgi yarışması başlatın! Otomatik fikstür motoru, podyum ve yıldız ödülleri hazır.",
                                        fontSize = 12.sp,
                                        color = secondaryText,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 17.sp
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { showCreateWizard = true },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                                        )
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("İlk Turnuvayı Başlat", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        // Turnuva Kartları Listesi
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(displayTournaments, key = { it.id }) { t ->
                                TournamentCardItem(
                                    tournament = t,
                                    isDarkTheme = isDarkTheme,
                                    onClick = {
                                        selectedTournament = t
                                        selectedTournamentMatches = TournamentPersistenceHelper.loadMatches(context, t.id)
                                    },
                                    onDelete = { tournamentToDelete = t }
                                )
                            }
                        }
                    }
                }
            } else {
                // =========================================================
                // SEÇİLİ TURNUVA DETAY EKRANI (FİKSTÜR, PUAN DURUMU, PODYUM)
                // =========================================================
                TournamentDetailScreen(
                    tournament = selectedTournament!!,
                    matches = selectedTournamentMatches,
                    students = students,
                    isDarkTheme = isDarkTheme,
                    onBack = { selectedTournament = null },
                    onMatchClick = { match ->
                        if (TournamentEngine.isMatchLocked(match, selectedTournamentMatches)) {
                            Toast.makeText(
                                context,
                                "🔒 Sonraki turun maçları başladığı için bu turun sonuçları kilitlenmiştir.",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            matchToScore = match
                        }
                    },
                    onDelete = { tournamentToDelete = selectedTournament },
                    onOpenPodium = { showPodiumDialog = true },
                    onRefresh = { refreshData() }
                )
            }
        }
    }

    // -------------------------------------------------------------
    // DİYALOG: MAÇ SONUCU GİRİŞİ (SKOR / GALİP SEÇİMİ)
    // -------------------------------------------------------------
    if (matchToScore != null && selectedTournament != null) {
        MatchScoreDialog(
            tournament = selectedTournament!!,
            match = matchToScore!!,
            isDarkTheme = isDarkTheme,
            onDismiss = { matchToScore = null },
            onSave = { s1, s2, winnerId ->
                val (updatedT, updatedM) = TournamentPersistenceHelper.recordMatchResult(
                    context = context,
                    classRoomId = classRoom.id,
                    tournamentId = selectedTournament!!.id,
                    matchId = matchToScore!!.id,
                    score1 = s1,
                    score2 = s2,
                    winnerId = winnerId
                )
                selectedTournament = updatedT
                selectedTournamentMatches = updatedM
                refreshData()
                matchToScore = null
                Toast.makeText(context, "⚽ Maç sonucu kaydedildi.", Toast.LENGTH_SHORT).show()
                if (updatedT.status == TournamentStatus.TAMAMLANDI) {
                    showPodiumDialog = true
                }
            }
        )
    }

    // -------------------------------------------------------------
    // DİYALOG: ŞAMPİYONLUK PODYUMU & KUTLAMA
    // -------------------------------------------------------------
    if (showPodiumDialog && selectedTournament != null) {
        val standings = remember(selectedTournamentMatches, selectedTournament) {
            TournamentEngine.calculateStandings(
                participants = selectedTournament!!.participants.map { Pair(it, it) },
                matches = selectedTournamentMatches,
                pointsWin = selectedTournament!!.pointsWin,
                pointsDraw = selectedTournament!!.pointsDraw,
                pointsLoss = selectedTournament!!.pointsLoss
            )
        }
        val livePodium = remember(selectedTournament, selectedTournamentMatches, standings) {
            TournamentEngine.determinePodium(selectedTournament!!, selectedTournamentMatches, standings)
        }
        val podiumTournament = remember(selectedTournament, livePodium) {
            if (livePodium.first != null) {
                selectedTournament!!.copy(
                    winnerId = livePodium.first?.first,
                    winnerName = livePodium.first?.second,
                    runnerUpId = livePodium.second?.first,
                    runnerUpName = livePodium.second?.second,
                    thirdPlaceId = livePodium.third?.first,
                    thirdPlaceName = livePodium.third?.second
                )
            } else {
                selectedTournament!!
            }
        }
        TournamentPodiumDialog(
            tournament = podiumTournament,
            isDarkTheme = isDarkTheme,
            onDismiss = { showPodiumDialog = false }
        )
    }

    // -------------------------------------------------------------
    // DİYALOG: TURNUVA SİLME VE YILDIZ GERİ ALMA ONAYI
    // -------------------------------------------------------------
    if (tournamentToDelete != null) {
        AlertDialog(
            onDismissRequest = { tournamentToDelete = null },
            title = { Text("Turnuvayı Sil?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "\"${tournamentToDelete!!.name}\" turnuvasını ve tüm maç kayıtlarını silmek istediğinize emin misiniz?\n\n" +
                            "⚠️ Güvenlik Notu: Bu turnuva için öğrencilere dağıtılmış olan tüm şampiyonluk ve katılım yıldızları öğrenci bakiyelerinden hatasız geri çekilecektir.",
                    fontSize = 13.5.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        TournamentPersistenceHelper.deleteTournament(context, classRoom.id, tournamentToDelete!!.id)
                        if (selectedTournament?.id == tournamentToDelete!!.id) {
                            selectedTournament = null
                        }
                        tournamentToDelete = null
                        refreshData()
                        Toast.makeText(context, "Turnuva silindi ve dağıtılan yıldızlar geri çekildi.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BirizError)
                ) {
                    Text("Evet, Sil ve Yıldızları Geri Al", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { tournamentToDelete = null }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    // -------------------------------------------------------------
    // DİYALOG: TURNUVA OLUŞTURMA SİHİRBAZI
    // -------------------------------------------------------------
    if (showCreateWizard) {
        TournamentCreateWizardDialog(
            students = students,
            isDarkTheme = isDarkTheme,
            onDismiss = { showCreateWizard = false },
            onCreate = { newTournament, participantsList ->
                val created = TournamentPersistenceHelper.createTournament(
                    context = context,
                    classRoomId = classRoom.id,
                    tournament = newTournament,
                    participantsPairs = participantsList
                )
                refreshData()
                showCreateWizard = false
                selectedTournament = created
                selectedTournamentMatches = TournamentPersistenceHelper.loadMatches(context, created.id)
                Toast.makeText(context, "🏆 Turnuva ve fikstür başarıyla oluşturuldu!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

// -------------------------------------------------------------
// BİLEŞEN: TURNUVA LİSTE KARTI
// -------------------------------------------------------------
@Composable
private fun TournamentCardItem(
    tournament: Tournament,
    isDarkTheme: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val matches = remember(tournament.id) { TournamentPersistenceHelper.loadMatches(context, tournament.id) }
    val totalMatches = matches.size
    val completedMatches = matches.count { it.status == "completed" }
    val progress = if (totalMatches > 0) completedMatches.toFloat() / totalMatches else 0f

    val isCompleted = tournament.status == TournamentStatus.TAMAMLANDI
    val primaryText = if (isDarkTheme) BirizDarkTextPrimary else BirizTextPrimary
    val secondaryText = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("tournament_card_${tournament.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) BirizDarkCardGlass else Color.White
        ),
        border = BorderStroke(
            1.dp,
            if (isCompleted) BirizGold.copy(alpha = 0.6f) else if (isDarkTheme) BirizDarkCardBorder else BirizBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(
                        text = tournament.type.icon,
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = tournament.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = primaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${tournament.type.displayName} • ${tournament.matchType.displayName}",
                                fontSize = 11.sp,
                                color = secondaryText
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isCompleted) BirizGold.copy(alpha = 0.15f) else if (isDarkTheme) Color(0x3506B6D4) else Color(0xFFE0F2FE),
                    border = BorderStroke(1.dp, if (isCompleted) BirizGold else if (isDarkTheme) BirizTurquoise else Color(0xFFBAE6FD))
                ) {
                    Text(
                        text = if (isCompleted) "🏆 Tamamlandı" else "⚡ Devam Ediyor",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCompleted) Color(0xFFB45309) else if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // İlerleme Çubuğu ve Maç Sayısı
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "İlerleme: $completedMatches / $totalMatches Maç Tamamlandı",
                    fontSize = 11.sp,
                    color = secondaryText
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCompleted) BirizGold else BirizTurquoise
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (isCompleted) BirizGold else BirizTurquoise,
                trackColor = if (isDarkTheme) Color(0x30334E68) else Color(0xFFE2E8F0)
            )

            // Şampiyon Rozeti (Tamamlandıysa)
            if (isCompleted && tournament.winnerName != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = BirizGold.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, BirizGold.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🥇", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Şampiyon: ${tournament.winnerName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFFB45309)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "+${tournament.starRewards.firstPlaceStars} ⭐",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.5.sp,
                            color = BirizGold
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// BİLEŞEN: SEÇİLİ TURNUVA DETAY GÖRÜNÜMÜ
// -------------------------------------------------------------
@Composable
private fun TournamentDetailScreen(
    tournament: Tournament,
    matches: List<TournamentMatch>,
    students: List<Student>,
    isDarkTheme: Boolean,
    onBack: () -> Unit,
    onMatchClick: (TournamentMatch) -> Unit,
    onDelete: () -> Unit,
    onOpenPodium: () -> Unit,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    var selectedDetailTab by remember { mutableIntStateOf(0) } // 0: Fikstür & Maçlar, 1: Puan Durumu, 2: Podyum & Ödüller

    val primaryText = if (isDarkTheme) BirizDarkTextPrimary else BirizTextPrimary
    val secondaryText = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary

    val totalMatches = matches.size
    val completedMatches = matches.count { it.status == "completed" }
    val isCompleted = tournament.status == TournamentStatus.TAMAMLANDI

    // Turlar / Haftalar
    val allRounds = remember(matches) { matches.map { it.round }.distinct().sorted() }
    var selectedRound by remember(allRounds) { mutableIntStateOf(allRounds.firstOrNull() ?: 1) }

    val roundMatches = remember(matches, selectedRound) {
        matches.filter { it.round == selectedRound }
    }

    // Puan Durumu (Lig veya Gruplar için)
    val standings = remember(matches, tournament) {
        TournamentEngine.calculateStandings(
            participants = tournament.participants.map { id ->
                val name = if (id.startsWith("extra:")) id.removePrefix("extra:") else students.find { it.id == id }?.fullName ?: id
                Pair(id, name)
            },
            matches = matches,
            pointsWin = tournament.pointsWin,
            pointsDraw = tournament.pointsDraw,
            pointsLoss = tournament.pointsLoss
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Üst Başlık Kartı
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = if (isDarkTheme) Color(0x900E172A) else Color.White,
            border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        IconButton(onClick = onBack, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = primaryText)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "${tournament.type.icon} ${tournament.name}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = primaryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${tournament.type.displayName} • $completedMatches/$totalMatches Maç Tamamlandı",
                                fontSize = 11.sp,
                                color = secondaryText
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // PDF Rapor Butonu
                        OutlinedButton(
                            onClick = {
                                val uri = ExcelPdfHelper.exportTournamentPdf(context, tournament, matches, standings)
                                ExcelPdfHelper.shareFile(context, uri, "application/pdf", "${tournament.name} Resmi Fikstür ve Puan Tablosu")
                            },
                            modifier = Modifier.height(32.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            border = BorderStroke(1.dp, if (isDarkTheme) BirizTurquoise else BirizNavyPrimary)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(14.dp), tint = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("PDF", fontSize = 11.5.sp, color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary)
                        }

                        // Silme Butonu
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Sil", tint = BirizError, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Detay İçi Sekmeler
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDarkTheme) Color(0x35334E68) else Color(0xFFF1F5F9))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val secondTabTitle = when (tournament.type) {
                        TournamentType.ELEME -> "Elemeler"
                        TournamentType.LIG -> "Puan Durumu"
                        TournamentType.GRUP -> "Gruplar"
                        TournamentType.GRUP_ELEME -> "Grup + Eleme"
                    }
                    val tabs = listOf(
                        Pair(0, "Maçlar & Fikstür"),
                        Pair(1, secondTabTitle),
                        Pair(2, if (isCompleted) "🏆 Podyum" else "⭐ Ödüller")
                    )

                    tabs.forEach { (index, title) ->
                        val isSelected = selectedDetailTab == index
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { selectedDetailTab = index },
                            color = if (isSelected) (if (isDarkTheme) BirizTurquoise else BirizNavyPrimary) else Color.Transparent,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = title,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else secondaryText,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Sekme İçerikleri
        when (selectedDetailTab) {
            0 -> {
                // 1. FİKSTÜR & MAÇLAR
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    // Hafta / Tur Seçici Hapları
                    if (allRounds.size > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            allRounds.forEach { r ->
                                val isSelected = selectedRound == r
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedRound = r },
                                    label = {
                                        val maxR = allRounds.maxOrNull() ?: 1
                                        Text(
                                            text = if (tournament.type == TournamentType.ELEME) {
                                                when {
                                                    r == maxR -> "🏆 Final"
                                                    r == maxR - 1 && maxR >= 2 -> "🥈 Yarı Final"
                                                    r == maxR - 2 && maxR >= 3 -> "🥉 Çeyrek Final"
                                                    r == maxR - 3 && maxR >= 4 -> "Son 16"
                                                    r == 1 && allRounds.size > 1 -> "⚡ Ön Eleme"
                                                    else -> "$r. Tur"
                                                }
                                            } else {
                                                "$r. Hafta"
                                            },
                                            fontSize = 11.5.sp
                                        )
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (roundMatches.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Bu tur için planlanmış maç bulunmuyor.", color = secondaryText, fontSize = 12.sp)
                        }
                    } else {
                        val isRoundLocked = remember(roundMatches, matches) {
                            roundMatches.isNotEmpty() && roundMatches.all { TournamentEngine.isMatchLocked(it, matches) }
                        }

                        if (isRoundLocked) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDarkTheme) Color(0x3064748B) else Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF475569) else Color(0xFFCBD5E1)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🔒", fontSize = 13.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Sonraki turun maçları başladığı için bu turun maç sonuçları ve skor düzenlemeleri kilitlenmiştir.",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF475569),
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }

                        val sortedRoundMatches = remember(roundMatches) {
                            roundMatches.sortedWith(
                                compareBy<TournamentMatch> { if (it.isThirdPlaceMatch) 0 else 1 }
                                    .thenBy { it.matchCode }
                            )
                        }
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(sortedRoundMatches, key = { it.id }) { match ->
                                val isLocked = TournamentEngine.isMatchLocked(match, matches)
                                TournamentMatchCard(
                                    tournament = tournament,
                                    match = match,
                                    isLocked = isLocked,
                                    isDarkTheme = isDarkTheme,
                                    onClick = { onMatchClick(match) }
                                )
                            }
                        }
                    }
                }
            }

            1 -> {
                when (tournament.type) {
                    TournamentType.ELEME -> {
                        TournamentEliminationsView(
                            tournament = tournament,
                            matches = matches,
                            isDarkTheme = isDarkTheme
                        )
                    }
                    TournamentType.LIG -> {
                        TournamentStandingsView(
                            standings = standings,
                            isDarkTheme = isDarkTheme
                        )
                    }
                    TournamentType.GRUP -> {
                        TournamentGroupStandingsView(
                            tournament = tournament,
                            matches = matches,
                            students = students,
                            isDarkTheme = isDarkTheme
                        )
                    }
                    TournamentType.GRUP_ELEME -> {
                        TournamentGroupAndKnockoutView(
                            tournament = tournament,
                            matches = matches,
                            students = students,
                            isDarkTheme = isDarkTheme,
                            onGenerateKnockout = {
                                TournamentPersistenceHelper.generateAndSaveKnockoutFromGroups(
                                    context = context,
                                    classRoomId = tournament.classRoomId,
                                    tournamentId = tournament.id
                                )
                                onRefresh()
                            }
                        )
                    }
                }
            }

            2 -> {
                // 3. PODYUM & ÖDÜLLER (Canlı doğrulanmış dereceler ile)
                val livePodium = remember(tournament, matches, standings) {
                    TournamentEngine.determinePodium(tournament, matches, standings)
                }
                val syncedTournament = remember(tournament, livePodium) {
                    if (livePodium.first != null) {
                        tournament.copy(
                            winnerId = livePodium.first?.first,
                            winnerName = livePodium.first?.second,
                            runnerUpId = livePodium.second?.first,
                            runnerUpName = livePodium.second?.second,
                            thirdPlaceId = livePodium.third?.first,
                            thirdPlaceName = livePodium.third?.second
                        )
                    } else {
                        tournament
                    }
                }
                TournamentPodiumView(
                    tournament = syncedTournament,
                    isDarkTheme = isDarkTheme,
                    onOpenPodiumModal = onOpenPodium
                )
            }
        }
    }
}

// -------------------------------------------------------------
// BİLEŞEN: MAÇ KARTI
// -------------------------------------------------------------
@Composable
private fun TournamentMatchCard(
    tournament: Tournament,
    match: TournamentMatch,
    isLocked: Boolean = false,
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    val isCompleted = match.status == "completed"
    val isTieBreaker = match.isTieBreaker
    val primaryText = if (isDarkTheme) BirizDarkTextPrimary else BirizTextPrimary
    val secondaryText = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("match_card_${match.matchCode}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) {
                if (isLocked) Color(0x20334E68) else BirizDarkCardGlass
            } else {
                if (isLocked) Color(0xFFF8FAFC) else Color.White
            }
        ),
        border = BorderStroke(
            1.dp,
            if (isLocked) (if (isDarkTheme) Color(0xFF475569) else Color(0xFFCBD5E1))
            else if (isTieBreaker) Color(0xFFEF4444)
            else if (isCompleted) Color(0xFF10B981).copy(alpha = 0.4f)
            else if (isDarkTheme) BirizDarkCardBorder
            else BirizBorder
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Üst Satır: Maç Kodu & Grup/Tur Bilgisi & Rozetler
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${match.groupName?.let { "$it • " } ?: ""}${match.matchCode}${if (match.isDoubleMatch && match.matchCode.endsWith("-R")) " (Rövanş)" else ""}",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = secondaryText
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isLocked) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isDarkTheme) Color(0x3064748B) else Color(0xFFE2E8F0),
                            border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF475569) else Color(0xFFCBD5E1))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text("🔒", fontSize = 8.5.sp)
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "Kilitli",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF475569)
                                )
                            }
                        }
                    }

                    if (match.isThirdPlaceMatch) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFEF3C7),
                            border = BorderStroke(1.dp, Color(0xFFD97706))
                        ) {
                            Text(
                                text = "🥉 3.lük Maçı",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    } else if (isTieBreaker) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFEE2E2),
                            border = BorderStroke(1.dp, Color(0xFFEF4444))
                        ) {
                            Text(
                                text = "⚖️ Karar Maçı (Tie-Breaker)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    } else if (isCompleted) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFD1FAE5)
                        ) {
                            Text(
                                text = "✓ Tamamlandı",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isDarkTheme) Color(0x30334E68) else Color(0xFFF1F5F9)
                        ) {
                            Text(
                                text = "Oynanacak",
                                fontSize = 10.sp,
                                color = secondaryText,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 1. Oyuncu Satırı
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (match.winnerId == match.player1Id) "🏆" else "👤",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = match.player1Name,
                        fontWeight = if (match.winnerId == match.player1Id) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp,
                        color = if (match.winnerId == match.player1Id) (if (isDarkTheme) BirizTurquoise else BirizNavyPrimary) else primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (tournament.winnerSelectionMethod == WinnerSelectionMethod.SCORE && match.score1 != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isDarkTheme) Color(0x40334E68) else Color(0xFFF1F5F9),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = "${match.score1}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = primaryText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                } else if (match.winnerId == match.player1Id) {
                    Text("Kazandı ✓", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 2. Oyuncu Satırı
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (match.winnerId == match.player2Id) "🏆" else "👤",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = match.player2Name,
                        fontWeight = if (match.winnerId == match.player2Id) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp,
                        color = if (match.winnerId == match.player2Id) (if (isDarkTheme) BirizTurquoise else BirizNavyPrimary) else primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (tournament.winnerSelectionMethod == WinnerSelectionMethod.SCORE && match.score2 != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isDarkTheme) Color(0x40334E68) else Color(0xFFF1F5F9),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text(
                            text = "${match.score2}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = primaryText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                } else if (match.winnerId == match.player2Id) {
                    Text("Kazandı ✓", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                }
            }
        }
    }
}

// -------------------------------------------------------------
// BİLEŞEN: PUAN DURUMU TABLOSU
// -------------------------------------------------------------
@Composable
private fun TournamentStandingsView(
    standings: List<TournamentStanding>,
    isDarkTheme: Boolean
) {
    val primaryText = if (isDarkTheme) BirizDarkTextPrimary else BirizTextPrimary
    val secondaryText = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkTheme) BirizDarkCardGlass else Color.White
            ),
            border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Tablo Başlık Satırı
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("#", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = secondaryText, modifier = Modifier.width(22.dp), textAlign = TextAlign.Center)
                    Text("Oyuncu", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = secondaryText, modifier = Modifier.weight(1f))
                    Text("O", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = secondaryText, modifier = Modifier.width(26.dp), textAlign = TextAlign.Center)
                    Text("G", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = secondaryText, modifier = Modifier.width(26.dp), textAlign = TextAlign.Center)
                    Text("B", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = secondaryText, modifier = Modifier.width(26.dp), textAlign = TextAlign.Center)
                    Text("M", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = secondaryText, modifier = Modifier.width(26.dp), textAlign = TextAlign.Center)
                    Text("AV", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = secondaryText, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                    Text("P", fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                }

                HorizontalDivider(color = if (isDarkTheme) Color(0x30334E68) else Color(0xFFE2E8F0))

                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(standings.size) { idx ->
                        val item = standings[idx]
                        val isLeader = idx == 0

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${idx + 1}",
                                fontSize = 11.sp,
                                fontWeight = if (isLeader) FontWeight.Bold else FontWeight.Normal,
                                color = if (isLeader) BirizGold else secondaryText,
                                modifier = Modifier.width(22.dp),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = item.playerName,
                                fontSize = 12.sp,
                                fontWeight = if (isLeader) FontWeight.Bold else FontWeight.Medium,
                                color = primaryText,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text("${item.played}", fontSize = 11.sp, color = secondaryText, modifier = Modifier.width(26.dp), textAlign = TextAlign.Center)
                            Text("${item.won}", fontSize = 11.sp, color = secondaryText, modifier = Modifier.width(26.dp), textAlign = TextAlign.Center)
                            Text("${item.drawn}", fontSize = 11.sp, color = secondaryText, modifier = Modifier.width(26.dp), textAlign = TextAlign.Center)
                            Text("${item.lost}", fontSize = 11.sp, color = secondaryText, modifier = Modifier.width(26.dp), textAlign = TextAlign.Center)
                            Text("${if (item.goalDiff > 0) "+${item.goalDiff}" else item.goalDiff}", fontSize = 11.sp, color = secondaryText, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                            Text(
                                text = "${item.points}",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                                modifier = Modifier.width(32.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                        if (idx < standings.size - 1) {
                            HorizontalDivider(color = if (isDarkTheme) Color(0x20334E68) else Color(0xFFF1F5F9))
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// BİLEŞEN: ELEMELER GÖRÜNÜMÜ (Kategorize Edilmiş Eleme Ağacı)
// -------------------------------------------------------------
@Composable
private fun TournamentEliminationsView(
    tournament: Tournament,
    matches: List<TournamentMatch>,
    isDarkTheme: Boolean
) {
    val primaryText = if (isDarkTheme) BirizDarkTextPrimary else BirizTextPrimary
    val secondaryText = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary

    // Sadece eleme maçları (grup maçları hariç)
    val knockoutMatches = remember(matches) {
        matches.filter { it.stage != "group" }
    }

    if (knockoutMatches.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🏆", fontSize = 40.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Henüz eleme maçı bulunmuyor.", fontWeight = FontWeight.Bold, color = primaryText, fontSize = 14.sp)
                Text("Maçlar oluştukça eleme ağacı burada kategorize edilecektir.", color = secondaryText, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        }
        return
    }

    val rounds = remember(knockoutMatches) {
        knockoutMatches.map { it.round }.distinct().sorted()
    }
    val maxRound = rounds.maxOrNull() ?: 1

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Bilgilendirme Şeridi (Kullanıcı İsteği: Bu sekme sadece turnuva durumunu izlemek içindir)
        item {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isDarkTheme) Color(0x2506B6D4) else Color(0xFFF0FDF4),
                border = BorderStroke(1.dp, if (isDarkTheme) Color(0x5006B6D4) else Color(0xFF86EFAC)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ℹ️", fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Bu sekme turnuva durumunu canlı takip etmek içindir. Maç skorları ve galip seçimleri 'Maçlar & Fikstür' sekmesinden yapılır.",
                        fontSize = 11.sp,
                        color = if (isDarkTheme) BirizDarkTextSecondary else Color(0xFF166534),
                        lineHeight = 15.sp
                    )
                }
            }
        }

        items(rounds, key = { it }) { r ->
            val roundMatches = knockoutMatches.filter { it.round == r }
            val isFinalRound = (r == maxRound)
            val thirdPlaceMatches = roundMatches.filter { it.isThirdPlaceMatch }
            val regularMatches = roundMatches.filter { !it.isThirdPlaceMatch }

            if (isFinalRound && thirdPlaceMatches.isNotEmpty()) {
                // 1. Önce 3.lük Maçı Bloğu (Bronz Madalya)
                val thirdCompleted = thirdPlaceMatches.count { it.status == "completed" }
                Column(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDarkTheme) Color(0x3578350F) else Color(0xFFFEF3C7),
                        border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFFB45309) else Color(0xFFFCD34D)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "🥉 3.lük Maçı (Bronz Madalya)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = if (isDarkTheme) Color(0xFFFBBF24) else Color(0xFF92400E)
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (thirdCompleted == thirdPlaceMatches.size) Color(0xFFDCFCE7) else (if (isDarkTheme) Color(0xFF1E293B) else Color.White),
                                border = BorderStroke(0.8.dp, if (thirdCompleted == thirdPlaceMatches.size) Color(0xFF10B981) else Color(0xFFCBD5E1))
                            ) {
                                Text(
                                    text = if (thirdCompleted == thirdPlaceMatches.size) "Tamamlandı ($thirdCompleted/$thirdCompleted) ✅" else "$thirdCompleted/${thirdPlaceMatches.size} Maç",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (thirdCompleted == thirdPlaceMatches.size) Color(0xFF15803D) else secondaryText,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        thirdPlaceMatches.forEach { match ->
                            EliminationMatchCard(
                                tournament = tournament,
                                match = match,
                                isDarkTheme = isDarkTheme
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Ardından Final Maçı Bloğu (Şampiyonluk)
                val finalCompleted = regularMatches.count { it.status == "completed" }
                Column(modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDarkTheme) Color(0x35334E68) else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFFCBD5E1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "🏆 Final (Şampiyonluk Maçı)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = BirizAmber
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (finalCompleted == regularMatches.size) Color(0xFFDCFCE7) else (if (isDarkTheme) Color(0xFF1E293B) else Color.White),
                                border = BorderStroke(0.8.dp, if (finalCompleted == regularMatches.size) Color(0xFF10B981) else Color(0xFFCBD5E1))
                            ) {
                                Text(
                                    text = if (finalCompleted == regularMatches.size) "Tamamlandı ($finalCompleted/$finalCompleted) ✅" else "$finalCompleted/${regularMatches.size} Maç",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (finalCompleted == regularMatches.size) Color(0xFF15803D) else secondaryText,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        regularMatches.forEach { match ->
                            EliminationMatchCard(
                                tournament = tournament,
                                match = match,
                                isDarkTheme = isDarkTheme
                            )
                        }
                    }
                }
            } else {
                val roundTitle = when {
                    r == maxRound -> "🏆 Final"
                    r == maxRound - 1 && maxRound >= 2 -> "🥈 Yarı Final"
                    r == maxRound - 2 && maxRound >= 3 -> "🥉 Çeyrek Final"
                    r == maxRound - 3 && maxRound >= 4 -> "Son 16 (1. Eleme)"
                    r == 1 && rounds.size > 1 -> "⚡ Ön Eleme Turu"
                    else -> "$r. Eleme Turu"
                }
                val completedCount = roundMatches.count { it.status == "completed" }

                Column(modifier = Modifier.fillMaxWidth()) {
                    // Tur Başlık Şeridi
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDarkTheme) Color(0x35334E68) else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFFCBD5E1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = roundTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = if (r == maxRound) BirizAmber else primaryText
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (completedCount == roundMatches.size) Color(0xFFDCFCE7) else (if (isDarkTheme) Color(0xFF1E293B) else Color.White),
                                border = BorderStroke(0.8.dp, if (completedCount == roundMatches.size) Color(0xFF10B981) else Color(0xFFCBD5E1))
                            ) {
                                Text(
                                    text = if (completedCount == roundMatches.size) "Tamamlandı ($completedCount/$completedCount) ✅" else "$completedCount/${roundMatches.size} Maç",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (completedCount == roundMatches.size) Color(0xFF15803D) else secondaryText,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Maç Kartları
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        roundMatches.forEach { match ->
                            EliminationMatchCard(
                                tournament = tournament,
                                match = match,
                                isDarkTheme = isDarkTheme
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// BİLEŞEN: ELEME MAÇ KARTI (Salt Okunur - İzleme & Takip Amaçlı)
// -------------------------------------------------------------
@Composable
private fun EliminationMatchCard(
    tournament: Tournament,
    match: TournamentMatch,
    isDarkTheme: Boolean
) {
    val isCompleted = match.status == "completed"
    val isBye = match.player2Id == "BYE" || match.player1Id == "BYE"

    val winnerId = match.winnerId ?: when {
        match.score1 != null && match.score2 != null && match.score1 > match.score2 -> match.player1Id
        match.score1 != null && match.score2 != null && match.score2 > match.score1 -> match.player2Id
        else -> null
    }

    val isWinner1 = isCompleted && winnerId == match.player1Id
    val isWinner2 = isCompleted && winnerId == match.player2Id
    val isDraw = isCompleted && winnerId == null && match.score1 != null && match.score1 == match.score2

    val secondaryText = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) Color(0x351E293B) else Color.White
        ),
        border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Eşleşme Başlığı
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "${match.matchCode}${if (match.isDoubleMatch && match.matchCode.endsWith("-R")) " (Rövanş)" else ""}",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = secondaryText
                    )
                    if (match.isThirdPlaceMatch) {
                        Surface(shape = RoundedCornerShape(3.dp), color = Color(0xFFFEF3C7)) {
                            Text("🥉 3.lük", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E), modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                        }
                    } else if (match.isTieBreaker) {
                        Surface(shape = RoundedCornerShape(3.dp), color = Color(0xFFFEE2E2)) {
                            Text("⚖️ Karar", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF991B1B), modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                        }
                    }
                }
                if (isBye) {
                    Text("Bay Geçti", fontSize = 10.sp, color = BirizTurquoise, fontWeight = FontWeight.Bold)
                } else if (isCompleted) {
                    Text("Tamamlandı ✅", fontSize = 10.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                } else {
                    Text("Oynanmadı ⏱️", fontSize = 10.sp, color = secondaryText, fontWeight = FontWeight.Medium)
                }
            }

            // 1. Oyuncu Kutusu
            EliminationParticipantRow(
                playerName = match.player1Name.ifBlank { if (match.player1Id.startsWith("WINNER_OF")) "Önceki Tur Galibi" else "1. Oyuncu" },
                score = match.score1,
                isCompleted = isCompleted,
                isWinner = isWinner1,
                isLoser = isCompleted && !isWinner1 && !isDraw,
                isDraw = isDraw,
                isDarkTheme = isDarkTheme
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 2. Oyuncu Kutusu
            EliminationParticipantRow(
                playerName = if (match.player2Id == "BYE") "BAY (Eşleşme Yok)" else match.player2Name.ifBlank { if (match.player2Id.startsWith("WINNER_OF")) "Önceki Tur Galibi" else "2. Oyuncu" },
                score = match.score2,
                isCompleted = isCompleted,
                isWinner = isWinner2,
                isLoser = isCompleted && !isWinner2 && !isDraw,
                isDraw = isDraw,
                isDarkTheme = isDarkTheme
            )
        }
    }
}

@Composable
private fun EliminationParticipantRow(
    playerName: String,
    score: Int?,
    isCompleted: Boolean,
    isWinner: Boolean,
    isLoser: Boolean,
    isDraw: Boolean,
    isDarkTheme: Boolean
) {
    // Yenenler: yeşil fonlu kutu
    // Yenilenler: kırmızı fonlu kutu
    // Henüz oynanmayanlar: normal nötr fonlu kutu
    val bgColor = when {
        !isCompleted -> if (isDarkTheme) Color(0x20334E68) else Color(0xFFF8FAFC)
        isWinner -> if (isDarkTheme) Color(0x3510B981) else Color(0xFFDCFCE7)
        isLoser -> if (isDarkTheme) Color(0x35EF4444) else Color(0xFFFEE2E2)
        else -> if (isDarkTheme) Color(0x35F59E0B) else Color(0xFFFEF3C7) // Berabere
    }

    val borderColor = when {
        !isCompleted -> if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0)
        isWinner -> Color(0xFF10B981)
        isLoser -> Color(0xFFF87171)
        else -> Color(0xFFF59E0B)
    }

    val textColor = when {
        !isCompleted -> if (isDarkTheme) BirizDarkTextPrimary else Color(0xFF1E293B)
        isWinner -> if (isDarkTheme) Color(0xFF34D399) else Color(0xFF14532D)
        isLoser -> if (isDarkTheme) Color(0xFFF87171) else Color(0xFF7F1D1D)
        else -> if (isDarkTheme) Color(0xFFFBBF24) else Color(0xFF78350F)
    }

    Surface(
        shape = RoundedCornerShape(7.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth().height(32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                if (isCompleted) {
                    Text(
                        text = if (isWinner) "🏆" else if (isLoser) "❌" else "🤝",
                        fontSize = 11.sp,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
                Text(
                    text = playerName,
                    fontSize = 12.sp,
                    fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Medium,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (score != null) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isWinner) Color(0xFF10B981) else (if (isDarkTheme) Color(0xFF1E293B) else Color.White),
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    Text(
                        text = "$score",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isWinner) Color.White else textColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            } else if (isCompleted) {
                Text(
                    text = if (isWinner) "Galip" else "Elendi",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
        }
    }
}

// -------------------------------------------------------------
// BİLEŞEN: GRUPLAR PUAN DURUMU GÖRÜNÜMÜ
// -------------------------------------------------------------
@Composable
private fun TournamentGroupStandingsView(
    tournament: Tournament,
    matches: List<TournamentMatch>,
    students: List<Student>,
    isDarkTheme: Boolean
) {
    val primaryText = if (isDarkTheme) BirizDarkTextPrimary else BirizTextPrimary
    val secondaryText = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary

    // Grup maçları
    val groupMatches = remember(matches) {
        matches.filter { it.stage != "knockout" }
    }

    val groupsMap = remember(groupMatches) {
        groupMatches.groupBy { it.groupName ?: it.groupId ?: "A Grubu" }
    }

    if (groupsMap.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("👥", fontSize = 40.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Grup verisi bulunamadı.", fontWeight = FontWeight.Bold, color = primaryText, fontSize = 14.sp)
            }
        }
        return
    }

    val groupNames = remember(groupsMap) {
        if (groupsMap.size > 1) listOf("Tümü") + groupsMap.keys.sorted() else groupsMap.keys.toList()
    }
    var selectedGroupFilter by remember { mutableStateOf(groupNames.first()) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Grup Filtre Hapları (Birden fazla grup varsa)
        if (groupNames.size > 2) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                groupNames.forEach { name ->
                    val isSelected = selectedGroupFilter == name
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedGroupFilter = name },
                        label = { Text(name, fontSize = 11.5.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        val displayGroups = if (selectedGroupFilter == "Tümü") {
            groupsMap.keys.sorted()
        } else {
            listOf(selectedGroupFilter)
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(displayGroups, key = { it }) { gName ->
                val gMatches = groupsMap[gName] ?: emptyList()
                val gParticipants = remember(gMatches) {
                    gMatches.flatMap {
                        listOf(Pair(it.player1Id, it.player1Name), Pair(it.player2Id, it.player2Name))
                    }.filter { it.first.isNotBlank() && it.first != "BYE" }.distinctBy { it.first }
                }

                val gStandings = remember(gParticipants, gMatches) {
                    TournamentEngine.calculateStandings(
                        participants = gParticipants,
                        matches = gMatches,
                        pointsWin = tournament.pointsWin,
                        pointsDraw = tournament.pointsDraw,
                        pointsLoss = tournament.pointsLoss
                    )
                }

                val completedMatchesCount = gMatches.count { it.status == "completed" }

                // Grup Kartı
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkTheme) BirizDarkCardGlass else Color.White
                    ),
                    border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        // Grup Başlığı ve İlerleme
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "👥 $gName",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                                )
                                if (tournament.type == TournamentType.GRUP_ELEME) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isDarkTheme) Color(0x3010B981) else Color(0xFFDCFCE7)
                                    ) {
                                        Text(
                                            text = "⬆️ İlk ${tournament.advancingPerGroup} Üst Tura",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF15803D),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "$completedMatchesCount/${gMatches.size} Maç",
                                fontSize = 10.5.sp,
                                color = secondaryText,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Tablo Başlık Satırı
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isDarkTheme) Color(0x20334E68) else Color(0xFFF8FAFC), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("#", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = secondaryText, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                            Text("Oyuncu", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = secondaryText, modifier = Modifier.weight(1f))
                            Text("O", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = secondaryText, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                            Text("G", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = secondaryText, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                            Text("B", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = secondaryText, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                            Text("M", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = secondaryText, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                            Text("AV", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = secondaryText, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                            Text("P", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                        }

                        HorizontalDivider(color = if (isDarkTheme) Color(0x30334E68) else Color(0xFFE2E8F0), modifier = Modifier.padding(vertical = 4.dp))

                        // Tablo Satırları
                        Column {
                            gStandings.forEachIndexed { idx, item ->
                                val isAdvancing = tournament.type == TournamentType.GRUP_ELEME && idx < tournament.advancingPerGroup
                                val rowBg = if (isAdvancing) {
                                    if (isDarkTheme) Color(0x1810B981) else Color(0x1210B981)
                                } else Color.Transparent

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(rowBg, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(modifier = Modifier.width(24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                        if (isAdvancing) {
                                            Text("⬆️", fontSize = 9.sp)
                                        }
                                        Text(
                                            text = "${idx + 1}",
                                            fontSize = 11.sp,
                                            fontWeight = if (idx == 0 || isAdvancing) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isAdvancing) Color(0xFF10B981) else secondaryText,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    Text(
                                        text = item.playerName,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (idx == 0 || isAdvancing) FontWeight.Bold else FontWeight.Medium,
                                        color = primaryText,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text("${item.played}", fontSize = 10.5.sp, color = secondaryText, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                                    Text("${item.won}", fontSize = 10.5.sp, color = secondaryText, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                                    Text("${item.drawn}", fontSize = 10.5.sp, color = secondaryText, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                                    Text("${item.lost}", fontSize = 10.5.sp, color = secondaryText, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                                    Text("${if (item.goalDiff > 0) "+${item.goalDiff}" else item.goalDiff}", fontSize = 10.5.sp, color = secondaryText, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                                    Text(
                                        text = "${item.points}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                                        modifier = Modifier.width(30.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                                if (idx < gStandings.size - 1) {
                                    HorizontalDivider(color = if (isDarkTheme) Color(0x15334E68) else Color(0xFFF8FAFC))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// BİLEŞEN: GRUP + ELEME GÖRÜNÜMÜ (Gruplar ve Elemeler Alt Sekmeleri)
// -------------------------------------------------------------
@Composable
private fun TournamentGroupAndKnockoutView(
    tournament: Tournament,
    matches: List<TournamentMatch>,
    students: List<Student>,
    isDarkTheme: Boolean,
    onGenerateKnockout: () -> Unit
) {
    var subTab by remember { mutableIntStateOf(0) } // 0: Gruplar, 1: Elemeler

    val primaryText = if (isDarkTheme) BirizDarkTextPrimary else BirizTextPrimary
    val secondaryText = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary

    val knockoutMatches = remember(matches) { matches.filter { it.stage == "knockout" } }
    val groupMatches = remember(matches) { matches.filter { it.stage != "knockout" } }
    val allGroupMatchesCompleted = groupMatches.isNotEmpty() && groupMatches.all { it.status == "completed" }

    Column(modifier = Modifier.fillMaxSize()) {
        // Üst Alt Sekmeler (Pill Switcher)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            shape = RoundedCornerShape(8.dp),
            color = if (isDarkTheme) Color(0x25334E68) else Color(0xFFF1F5F9),
            border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFFE2E8F0))
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(3.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // 1. Gruplar Butonu
                Surface(
                    onClick = { subTab = 0 },
                    shape = RoundedCornerShape(6.dp),
                    color = if (subTab == 0) (if (isDarkTheme) BirizTurquoise else BirizNavyPrimary) else Color.Transparent,
                    modifier = Modifier.weight(1f).height(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "👥 Gruplar Puan Durumu",
                            fontSize = 11.5.sp,
                            fontWeight = if (subTab == 0) FontWeight.Bold else FontWeight.Medium,
                            color = if (subTab == 0) Color.White else secondaryText
                        )
                    }
                }

                // 2. Elemeler Butonu
                Surface(
                    onClick = { subTab = 1 },
                    shape = RoundedCornerShape(6.dp),
                    color = if (subTab == 1) (if (isDarkTheme) BirizTurquoise else BirizNavyPrimary) else Color.Transparent,
                    modifier = Modifier.weight(1f).height(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "🏆 Elemeler (Play-Off)",
                            fontSize = 11.5.sp,
                            fontWeight = if (subTab == 1) FontWeight.Bold else FontWeight.Medium,
                            color = if (subTab == 1) Color.White else secondaryText
                        )
                    }
                }
            }
        }

        when (subTab) {
            0 -> {
                // Gruplar Puan Durumu
                TournamentGroupStandingsView(
                    tournament = tournament,
                    matches = matches,
                    students = students,
                    isDarkTheme = isDarkTheme
                )
            }
            1 -> {
                // Elemeler (Play-Off)
                if (knockoutMatches.isNotEmpty()) {
                    TournamentEliminationsView(
                        tournament = tournament,
                        matches = matches,
                        isDarkTheme = isDarkTheme
                    )
                } else if (allGroupMatchesCompleted) {
                    // Grup maçları bitti, eleme ağacını başlatma çağrısı
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDarkTheme) Color(0x3510B981) else Color(0xFFF0FDF4)
                            ),
                            border = BorderStroke(1.2.dp, Color(0xFF10B981))
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🎉", fontSize = 36.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Grup Aşaması Tamamlandı!",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = if (isDarkTheme) Color.White else Color(0xFF14532D),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Tüm grup maçları oynandı ve lider/ikinciler belirlendi. Çapraz eşleşmeli eleme (play-off) ağacını oluşturup maçları başlatabilirsiniz.",
                                    fontSize = 12.sp,
                                    color = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF166534),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = onGenerateKnockout,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF10B981),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("🏆 Eleme Ağacını (Play-Off) Başlat", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                } else {
                    // Grup maçları devam ediyor
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDarkTheme) BirizDarkCardGlass else Color(0xFFFFFBEB)
                            ),
                            border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFFFDE68A))
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("⏳", fontSize = 32.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Grup Aşaması Devam Ediyor",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = primaryText
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${groupMatches.count { it.status == "completed" }}/${groupMatches.size} grup maçı tamamlandı.\nGruplardaki tüm maçlar bittiğinde lider ve ikinciler çapraz eşleştirilerek eleme ağacı burada açılacaktır.",
                                    fontSize = 11.5.sp,
                                    color = secondaryText,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// BİLEŞEN: PODYUM & ÖDÜLLER GÖRÜNÜMÜ
// -------------------------------------------------------------
@Composable
private fun TournamentPodiumView(
    tournament: Tournament,
    isDarkTheme: Boolean,
    onOpenPodiumModal: () -> Unit
) {
    val primaryText = if (isDarkTheme) BirizDarkTextPrimary else BirizTextPrimary
    val secondaryText = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
    val rewards = tournament.starRewards

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Podyum Kürsüsü
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = if (isDarkTheme) Color(0x30334E68) else Color.White,
            border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🏆 Turnuva Şampiyonluk Kürsüsü", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = primaryText)
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // 2. İkinci (Gümüş)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🥈", fontSize = 28.sp)
                        Text(
                            text = tournament.runnerUpName ?: "Henüz Belli Değil",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryText,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(75.dp),
                            color = BirizSilver.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("2", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = BirizSilver)
                            }
                        }
                    }

                    // 1. Şampiyon (Altın Kupa)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Text("👑", fontSize = 32.sp)
                        Text(
                            text = tournament.winnerName ?: "Henüz Belli Değil",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BirizGold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(105.dp),
                            color = BirizGold.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🏆", fontSize = 20.sp)
                                    Text("1", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFB45309))
                                }
                            }
                        }
                    }

                    // 3. Üçüncü (Bronz)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🥉", fontSize = 26.sp)
                        Text(
                            text = tournament.thirdPlaceName ?: "Henüz Belli Değil",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryText,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(55.dp),
                            color = BirizBronze.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("3", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = BirizBronze)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Yıldızlar Sınıfı Ödül Tablosu
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = if (isDarkTheme) Color(0x30334E68) else Color.White,
            border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⭐", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Yıldızlar Sınıfı Turnuva Ödülleri", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = primaryText)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("🥇 1. Şampiyon Ödülü:", fontSize = 12.sp, color = secondaryText)
                    Text("+${rewards.firstPlaceStars} ⭐", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = BirizGold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("🥈 2. İkincilik Ödülü:", fontSize = 12.sp, color = secondaryText)
                    Text("+${rewards.secondPlaceStars} ⭐", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = BirizSilver)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("🥉 3. Üçüncülük Ödülü:", fontSize = 12.sp, color = secondaryText)
                    Text("+${rewards.thirdPlaceStars} ⭐", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = BirizBronze)
                }
                if (rewards.participationStars > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("🎖️ Katılım Ödülü (Her Öğrenci):", fontSize = 12.sp, color = secondaryText)
                        Text("+${rewards.participationStars} ⭐", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (tournament.status == TournamentStatus.TAMAMLANDI)
                        "✅ Turnuva tamamlandığında tüm ödül yıldızları öğrencilerin hesaplarına otomatik olarak aktarılmıştır."
                    else
                        "💡 Final maçı tamamlandığı an kazanan öğrencilerin hesaplarına yukarıdaki yıldızlar otomatik işlenecektir.",
                    fontSize = 11.5.sp,
                    color = secondaryText,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

// -------------------------------------------------------------
// DİYALOG: MAÇ SONUCU GİRİŞİ (SKOR / GALİP)
// -------------------------------------------------------------
@Composable
private fun MatchScoreDialog(
    tournament: Tournament,
    match: TournamentMatch,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onSave: (score1: Int?, score2: Int?, winnerId: String?) -> Unit
) {
    var score1Str by remember { mutableStateOf(match.score1?.toString() ?: "") }
    var score2Str by remember { mutableStateOf(match.score2?.toString() ?: "") }
    var selectedWinnerId by remember { mutableStateOf(match.winnerId) }

    val isScoreMode = tournament.winnerSelectionMethod == WinnerSelectionMethod.SCORE
    val primaryText = if (isDarkTheme) BirizDarkTextPrimary else BirizTextPrimary

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Maç Sonucunu Gir", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(match.matchCode, fontSize = 12.sp, color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (isScoreMode) {
                    // Skor Girişi Modu
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(match.player1Name, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = score1Str,
                                onValueChange = { score1Str = it.filter { ch -> ch.isDigit() } },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.width(65.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            )
                        }

                        Text("—", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))

                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(match.player2Name, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = score2Str,
                                onValueChange = { score2Str = it.filter { ch -> ch.isDigit() } },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.width(65.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            )
                        }
                    }

                    val s1Val = score1Str.toIntOrNull()
                    val s2Val = score2Str.toIntOrNull()
                    val isTied = (s1Val != null && s2Val != null && s1Val == s2Val)
                    val isSingleKnockout = (match.stage == "knockout" && !match.isDoubleMatch)

                    if (isTied && isSingleKnockout) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDarkTheme) Color(0x35F59E0B) else Color(0xFFFEF3C7),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "⚠️ Skorlar Eşit! Eleme formatında üst tura çıkacak galibi seçin:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = if (isDarkTheme) Color.White else Color(0xFF92400E)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (selectedWinnerId == match.player1Id) Color(0xFF10B981) else (if (isDarkTheme) Color(0xFF1E293B) else Color.White),
                                        border = BorderStroke(1.dp, if (selectedWinnerId == match.player1Id) Color(0xFF10B981) else Color(0xFFCBD5E1)),
                                        modifier = Modifier.weight(1f).clickable { selectedWinnerId = match.player1Id }
                                    ) {
                                        Text(
                                            text = "🏆 ${match.player1Name}",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedWinnerId == match.player1Id) Color.White else primaryText,
                                            modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp),
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (selectedWinnerId == match.player2Id) Color(0xFF10B981) else (if (isDarkTheme) Color(0xFF1E293B) else Color.White),
                                        border = BorderStroke(1.dp, if (selectedWinnerId == match.player2Id) Color(0xFF10B981) else Color(0xFFCBD5E1)),
                                        modifier = Modifier.weight(1f).clickable { selectedWinnerId = match.player2Id }
                                    ) {
                                        Text(
                                            text = "🏆 ${match.player2Name}",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedWinnerId == match.player2Id) Color.White else primaryText,
                                            modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp),
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Galip Seçim Modu
                    Text("Kazanan oyuncuyu seçin:", fontSize = 12.sp)

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedWinnerId = match.player1Id },
                        color = if (selectedWinnerId == match.player1Id) (if (isDarkTheme) Color(0x3506B6D4) else Color(0xFFE0F2FE)) else Color.Transparent,
                        border = BorderStroke(1.dp, if (selectedWinnerId == match.player1Id) BirizTurquoise else BirizBorder)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (selectedWinnerId == match.player1Id) "🏆" else "👤", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(match.player1Name, fontWeight = if (selectedWinnerId == match.player1Id) FontWeight.Bold else FontWeight.Medium, fontSize = 13.sp)
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedWinnerId = match.player2Id },
                        color = if (selectedWinnerId == match.player2Id) (if (isDarkTheme) Color(0x3506B6D4) else Color(0xFFE0F2FE)) else Color.Transparent,
                        border = BorderStroke(1.dp, if (selectedWinnerId == match.player2Id) BirizTurquoise else BirizBorder)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (selectedWinnerId == match.player2Id) "🏆" else "👤", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(match.player2Name, fontWeight = if (selectedWinnerId == match.player2Id) FontWeight.Bold else FontWeight.Medium, fontSize = 13.sp)
                        }
                    }

                    if (tournament.type == TournamentType.LIG || tournament.type == TournamentType.GRUP) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedWinnerId = "DRAW" },
                            color = if (selectedWinnerId == "DRAW") (if (isDarkTheme) Color(0x3506B6D4) else Color(0xFFE0F2FE)) else Color.Transparent,
                            border = BorderStroke(1.dp, if (selectedWinnerId == "DRAW") BirizTurquoise else BirizBorder)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("🤝", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Berabere Bitti", fontWeight = if (selectedWinnerId == "DRAW") FontWeight.Bold else FontWeight.Medium, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isScoreMode) {
                        val s1 = score1Str.toIntOrNull()
                        val s2 = score2Str.toIntOrNull()
                        val wId = when {
                            s1 != null && s2 != null && s1 > s2 -> match.player1Id
                            s1 != null && s2 != null && s2 > s1 -> match.player2Id
                            s1 != null && s2 != null && s1 == s2 -> selectedWinnerId
                            else -> null
                        }
                        onSave(s1, s2, wId)
                    } else {
                        val wId = if (selectedWinnerId == "DRAW") null else selectedWinnerId
                        onSave(null, null, wId)
                    }
                },
                enabled = if (isScoreMode) {
                    val s1 = score1Str.toIntOrNull()
                    val s2 = score2Str.toIntOrNull()
                    score1Str.isNotBlank() && score2Str.isNotBlank() && (
                        s1 != s2 || match.stage != "knockout" || match.isDoubleMatch || selectedWinnerId != null
                    )
                } else selectedWinnerId != null
            ) {
                Text("Kaydet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Vazgeç")
            }
        }
    )
}

// -------------------------------------------------------------
// DİYALOG: ŞAMPİYONLUK PODYUMU & KUTLAMA MODALI
// -------------------------------------------------------------
@Composable
private fun TournamentPodiumDialog(
    tournament: Tournament,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("🎉 ŞAMPİYONLUK KÜRSÜSÜ 🏆", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = BirizGold)
                Text(tournament.name, fontSize = 12.sp, color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Tebrikler! Turnuva başarıyla tamamlandı.", fontSize = 12.5.sp, textAlign = TextAlign.Center)

                // 1. Şampiyon
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = BirizGold.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, BirizGold),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("🥇", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("1. Şampiyon", fontSize = 11.sp, color = Color(0xFFB45309))
                            Text(tournament.winnerName ?: "-", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                        }
                        Text("+${tournament.starRewards.firstPlaceStars} ⭐", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = BirizGold)
                    }
                }

                // 2. İkinci
                tournament.runnerUpName?.let { rName ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = BirizSilver.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, BirizSilver),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🥈", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("2. İkinci", fontSize = 11.sp, color = Color(0xFF475569))
                                Text(rName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Text("+${tournament.starRewards.secondPlaceStars} ⭐", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = BirizSilver)
                        }
                    }
                }

                // 3. Üçüncü
                tournament.thirdPlaceName?.let { thName ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = BirizBronze.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, BirizBronze),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🥉", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("3. Üçüncü", fontSize = 11.sp, color = Color(0xFF78350F))
                                Text(thName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Text("+${tournament.starRewards.thirdPlaceStars} ⭐", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = BirizBronze)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Harika!")
            }
        }
    )
}

// -------------------------------------------------------------
// DİYALOG: TURNUVA OLUŞTURMA SİHİRBAZI
// -------------------------------------------------------------
@Composable
private fun TournamentCreateWizardDialog(
    students: List<Student>,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onCreate: (Tournament, List<Pair<String, String>>) -> Unit
) {
    var step by remember { mutableIntStateOf(1) } // 1: Branş, 2: Format & Kurallar, 3: Katılımcılar, 4: Yıldızlar

    // Form Değerleri
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var tournamentType by remember { mutableStateOf(TournamentType.ELEME) }
    var matchType by remember { mutableStateOf(TournamentMatchType.TEK_MAC) }
    var winnerMethod by remember { mutableStateOf(WinnerSelectionMethod.WINNER) }

    var groupCount by remember { mutableIntStateOf(2) }
    var groupNaming by remember { mutableStateOf("letters") }
    var advancingPerGroup by remember { mutableIntStateOf(2) }

    val selectedStudentIds = remember { mutableStateOf(students.map { it.id }.toSet()) }
    var guestNameInput by remember { mutableStateOf("") }
    val extraParticipants = remember { mutableStateOf<List<String>>(emptyList()) }

    var starsEnabled by remember { mutableStateOf(true) }
    var firstPlaceStarsStr by remember { mutableStateOf("10") }
    var secondPlaceStarsStr by remember { mutableStateOf("6") }
    var thirdPlaceStarsStr by remember { mutableStateOf("4") }
    var participationStarsStr by remember { mutableStateOf("1") }

    val primaryTextColor = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
    val secondaryTextColor = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary

    Dialog(onDismissRequest = onDismiss) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 10.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            val maxDialogHeight = maxHeight * 0.92f

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxDialogHeight),
                shape = RoundedCornerShape(22.dp),
                color = if (isDarkTheme) Color(0xFF0F1E38) else Color.White,
                border = BorderStroke(1.2.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFFE2E8F0)),
                shadowElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // 1. SABİT ÜST BAŞLIK & ADIM BİLGİSİ
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDarkTheme) Color(0x35F59E0B) else Color(0xFFFEF3C7),
                                border = BorderStroke(1.dp, if (isDarkTheme) BirizAmber else Color(0xFFFDE68A)),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("🏆", fontSize = 20.sp)
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Turnuva Kur",
                                        fontSize = 15.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = primaryTextColor
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isDarkTheme) Color(0x350D9488) else Color(0xFFE0F2FE)
                                    ) {
                                        Text(
                                            text = "Adım $step/4",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = when (step) {
                                        1 -> "Popüler Branş veya Özel Turnuva Adı"
                                        2 -> "Format, Eşleşme & Kural Seçimi"
                                        3 -> "Katılımcı Öğrenci & Misafir Seçimi"
                                        else -> "Kazanılacak Yıldızlar Sınıfı Ödülleri"
                                    },
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = secondaryTextColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Kapat",
                                tint = secondaryTextColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // 4 Aşamalı İlerleme Çubuğu (Step Progress Indicator)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (i in 1..4) {
                            val isCompletedOrCurrent = i <= step
                            val barColor = when {
                                i == step -> BirizTurquoise
                                i < step -> if (isDarkTheme) Color(0xFF0284C7) else Color(0xFF0284C7)
                                else -> if (isDarkTheme) Color(0x3064748B) else Color(0xFFE2E8F0)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(3.5.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(barColor)
                            )
                        }
                    }

                    HorizontalDivider(
                        color = if (isDarkTheme) Color(0x3064748B) else Color(0xFFF1F5F9),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    // 2. ORTA FORMLAR (Minimum Dikey Boşluk, Kaydırılabilir İçerik)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        when (step) {
                            1 -> {
                                // ADIM 1: BRANŞ SEÇİMİ
                                Text(
                                    text = "1. Popüler Bir Branş Seçin:",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryTextColor
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    ActivityPreset.PRESETS.forEach { preset ->
                                        val isSelected = name.startsWith(preset.title)
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    name = "${preset.title} Turnuvası"
                                                    description = preset.description
                                                    tournamentType = preset.defaultType
                                                    matchType = preset.defaultMatchType
                                                    winnerMethod = preset.defaultWinnerMethod
                                                },
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isSelected) {
                                                if (isDarkTheme) Color(0x3506B6D4) else Color(0xFFE0F2FE)
                                            } else {
                                                if (isDarkTheme) Color(0x181E293B) else Color(0xFFF8FAFC)
                                            },
                                            border = BorderStroke(
                                                1.dp,
                                                if (isSelected) BirizTurquoise else (if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0))
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(if (isSelected) BirizTurquoise.copy(alpha = 0.2f) else Color.Transparent),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(preset.icon, fontSize = 20.sp)
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = preset.title,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.5.sp,
                                                        color = primaryTextColor
                                                    )
                                                    Text(
                                                        text = preset.description,
                                                        fontSize = 10.sp,
                                                        color = secondaryTextColor,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.CheckCircle,
                                                        contentDescription = null,
                                                        tint = BirizTurquoise,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = "2. Turnuva Adını Belirleyin / Özelleştirin:",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryTextColor
                                )

                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = { Text("Turnuva Adı * (Zorunlu)", fontSize = 11.5.sp) },
                                    placeholder = { Text("Örn: 4/D Bahar Satranç Turnuvası", fontSize = 12.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BirizTurquoise,
                                        unfocusedBorderColor = if (isDarkTheme) Color(0xFF334155) else Color(0xFFCBD5E1)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            2 -> {
                                // ADIM 2: FORMAT & KURALLAR (Kompakt ve profesyonel kart blokları)
                                // 1. Turnuva Formatı Kartı
                                SelectionGroupCard(
                                    title = "Turnuva Formatı",
                                    icon = "🏆",
                                    isDarkTheme = isDarkTheme
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        TournamentType.values().forEach { tType ->
                                            val isSelected = tournamentType == tType
                                            CompactOptionButton(
                                                title = "${tType.icon} ${tType.displayName}",
                                                isSelected = isSelected,
                                                isDarkTheme = isDarkTheme,
                                                modifier = Modifier.weight(1f),
                                                onClick = { tournamentType = tType }
                                            )
                                        }
                                    }
                                }

                                // Grup formatı seçilmişse: Grup Sayısı
                                if (tournamentType == TournamentType.GRUP || tournamentType == TournamentType.GRUP_ELEME) {
                                    SelectionGroupCard(
                                        title = "Grup Sayısı",
                                        icon = "👥",
                                        isDarkTheme = isDarkTheme
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf(2, 3, 4, 6).forEach { gc ->
                                                val isSelected = groupCount == gc
                                                CompactOptionButton(
                                                    title = "$gc Grup",
                                                    isSelected = isSelected,
                                                    isDarkTheme = isDarkTheme,
                                                    modifier = Modifier.weight(1f),
                                                    onClick = { groupCount = gc }
                                                )
                                            }
                                        }
                                    }
                                }

                                // 2. Karşılaşma Biçimi Kartı
                                SelectionGroupCard(
                                    title = "Karşılaşma Biçimi",
                                    icon = "⚔️",
                                    isDarkTheme = isDarkTheme
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        TournamentMatchType.values().forEach { mType ->
                                            val isSelected = matchType == mType
                                            CompactOptionButton(
                                                title = mType.displayName,
                                                isSelected = isSelected,
                                                isDarkTheme = isDarkTheme,
                                                modifier = Modifier.weight(1f),
                                                onClick = { matchType = mType }
                                            )
                                        }
                                    }
                                }

                                // 3. Kazanan Belirleme Yöntemi Kartı
                                SelectionGroupCard(
                                    title = "Kazanan Belirleme Yöntemi",
                                    icon = "🎯",
                                    isDarkTheme = isDarkTheme
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        WinnerSelectionMethod.values().forEach { wMethod ->
                                            val isSelected = winnerMethod == wMethod
                                            CompactOptionButton(
                                                title = wMethod.displayName,
                                                isSelected = isSelected,
                                                isDarkTheme = isDarkTheme,
                                                modifier = Modifier.weight(1f),
                                                onClick = { winnerMethod = wMethod }
                                            )
                                        }
                                    }
                                }
                            }

                            3 -> {
                                // ADIM 3: KATILIMCILAR
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "Katılımcı Listesi",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            color = primaryTextColor
                                        )
                                        Text(
                                            text = "Toplam: ${selectedStudentIds.value.size + extraParticipants.value.size} katılımcı seçili",
                                            fontSize = 10.5.sp,
                                            color = if (selectedStudentIds.value.size + extraParticipants.value.size >= 2) BirizTurquoise else BirizError
                                        )
                                    }

                                    TextButton(
                                        onClick = {
                                            selectedStudentIds.value = if (selectedStudentIds.value.size == students.size) emptySet() else students.map { it.id }.toSet()
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (selectedStudentIds.value.size == students.size) "Seçimi Kaldır" else "Tümünü Seç",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BirizTurquoise
                                        )
                                    }
                                }

                                // Sınıf Öğrenci Listesi
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isDarkTheme) Color(0x181E293B) else Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 90.dp, max = 150.dp)
                                ) {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(4.dp),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        items(students, key = { it.id }) { st ->
                                            val isSelected = selectedStudentIds.value.contains(st.id)
                                            val isGirl = st.gender.equals("KIZ", ignoreCase = true)
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        val current = selectedStudentIds.value.toMutableSet()
                                                        if (isSelected) current.remove(st.id) else current.add(st.id)
                                                        selectedStudentIds.value = current
                                                    },
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isSelected) {
                                                    if (isDarkTheme) Color(0x3506B6D4) else Color(0xFFE0F2FE)
                                                } else Color.Transparent,
                                                border = BorderStroke(
                                                    0.8.dp,
                                                    if (isSelected) BirizTurquoise else Color.Transparent
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Checkbox(
                                                        checked = isSelected,
                                                        onCheckedChange = { checked ->
                                                            val current = selectedStudentIds.value.toMutableSet()
                                                            if (checked) current.add(st.id) else current.remove(st.id)
                                                            selectedStudentIds.value = current
                                                        },
                                                        colors = CheckboxDefaults.colors(checkedColor = BirizTurquoise),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))

                                                    Surface(
                                                        shape = CircleShape,
                                                        color = if (isGirl) Color(0xFFFCE7F3) else Color(0xFFE0F2FE),
                                                        modifier = Modifier.size(20.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text(
                                                                text = st.studentNumber,
                                                                fontSize = 8.5.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isGirl) Color(0xFFDB2777) else Color(0xFF0284C7)
                                                            )
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = st.fullName,
                                                        fontSize = 11.5.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        color = primaryTextColor
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Misafir Katılımcı Ekleme
                                Text(
                                    text = "Misafir / Ek Katılımcı Ekle:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryTextColor
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedTextField(
                                        value = guestNameInput,
                                        onValueChange = { guestNameInput = it },
                                        placeholder = { Text("Misafir Adı (Örn: Ahmet Öğretmen)...", fontSize = 11.sp) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).height(42.dp)
                                    )
                                    Button(
                                        onClick = {
                                            if (guestNameInput.isNotBlank()) {
                                                extraParticipants.value = extraParticipants.value + guestNameInput.trim()
                                                guestNameInput = ""
                                            }
                                        },
                                        enabled = guestNameInput.isNotBlank(),
                                        colors = ButtonDefaults.buttonColors(containerColor = BirizTurquoise),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                        modifier = Modifier.height(42.dp)
                                    ) {
                                        Text("Ekle", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (extraParticipants.value.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        extraParticipants.value.forEach { extra ->
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isDarkTheme) Color(0x350D9488) else Color(0xFFCCFBF1),
                                                border = BorderStroke(1.dp, BirizTurquoise)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(text = "👤 $extra", fontSize = 10.5.sp, color = BirizTurquoise, fontWeight = FontWeight.SemiBold)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Sil",
                                                        tint = BirizTurquoise,
                                                        modifier = Modifier
                                                            .size(13.dp)
                                                            .clickable {
                                                                extraParticipants.value = extraParticipants.value.filter { it != extra }
                                                            }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            4 -> {
                                // ADIM 4: YILDIZLAR SINIFI ÖDÜL AYARLARI
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (starsEnabled) {
                                        if (isDarkTheme) Color(0x35F59E0B) else Color(0xFFFEF3C7)
                                    } else {
                                        if (isDarkTheme) Color(0x181E293B) else Color(0xFFF8FAFC)
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (starsEnabled) BirizAmber else (if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0))
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { starsEnabled = !starsEnabled }
                                            .padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("⭐", fontSize = 16.sp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = "Yıldızlar Sınıfı Başarı Puanları",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = primaryTextColor
                                                )
                                                Text(
                                                    text = "Turnuva bitiminde dereceye girenlere otomatik yıldız verir",
                                                    fontSize = 10.sp,
                                                    color = secondaryTextColor
                                                )
                                            }
                                        }
                                        Switch(
                                            checked = starsEnabled,
                                            onCheckedChange = { starsEnabled = it },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = BirizAmber
                                            )
                                        )
                                    }
                                }

                                if (starsEnabled) {
                                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        StarRewardInputField(
                                            label = "🥇 1. Şampiyon Yıldızı",
                                            value = firstPlaceStarsStr,
                                            onValueChange = { firstPlaceStarsStr = it },
                                            isDarkTheme = isDarkTheme
                                        )
                                        StarRewardInputField(
                                            label = "🥈 2. İkincilik Yıldızı",
                                            value = secondPlaceStarsStr,
                                            onValueChange = { secondPlaceStarsStr = it },
                                            isDarkTheme = isDarkTheme
                                        )
                                        StarRewardInputField(
                                            label = "🥉 3. Üçüncülük Yıldızı",
                                            value = thirdPlaceStarsStr,
                                            onValueChange = { thirdPlaceStarsStr = it },
                                            isDarkTheme = isDarkTheme
                                        )
                                        StarRewardInputField(
                                            label = "🎖️ Katılım Yıldızı (Tümüne)",
                                            value = participationStarsStr,
                                            onValueChange = { participationStarsStr = it },
                                            isDarkTheme = isDarkTheme
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. SABİT ALT AKSİYON ŞERİDİ (Her Zaman Görünür)
                    HorizontalDivider(
                        color = if (isDarkTheme) Color(0x3064748B) else Color(0xFFF1F5F9),
                        thickness = 1.dp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (step > 1) {
                            OutlinedButton(
                                onClick = { step-- },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Geri", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            OutlinedButton(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Text("Vazgeç", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        if (step < 4) {
                            val isStepValid = when (step) {
                                1 -> name.isNotBlank()
                                3 -> (selectedStudentIds.value.size + extraParticipants.value.size) >= 2
                                else -> true
                            }

                            Button(
                                onClick = { step++ },
                                enabled = isStepValid,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) {
                                Text("İleri", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        } else {
                            Button(
                                onClick = {
                                    val partPairs = mutableListOf<Pair<String, String>>()
                                    students.filter { selectedStudentIds.value.contains(it.id) }.forEach {
                                        partPairs.add(Pair(it.id, it.fullName))
                                    }
                                    extraParticipants.value.forEach { extra ->
                                        partPairs.add(Pair("extra:$extra", extra))
                                    }

                                    val starRewards = TournamentStarRewards(
                                        enabled = starsEnabled,
                                        firstPlaceStars = firstPlaceStarsStr.toIntOrNull() ?: 10,
                                        secondPlaceStars = secondPlaceStarsStr.toIntOrNull() ?: 6,
                                        thirdPlaceStars = thirdPlaceStarsStr.toIntOrNull() ?: 4,
                                        participationStars = participationStarsStr.toIntOrNull() ?: 1
                                    )

                                    val newT = Tournament(
                                        classRoomId = "",
                                        name = name.trim(),
                                        description = description.trim(),
                                        type = tournamentType,
                                        matchType = matchType,
                                        winnerSelectionMethod = winnerMethod,
                                        participants = partPairs.map { it.first },
                                        extraParticipants = extraParticipants.value,
                                        groupCount = groupCount,
                                        groupNaming = groupNaming,
                                        advancingPerGroup = advancingPerGroup,
                                        starRewards = starRewards
                                    )

                                    onCreate(newT, partPairs)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BirizAmber),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier.weight(1f).height(40.dp)
                            ) {
                                Text("Turnuvayı Başlat 🏆", fontSize = 12.5.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionGroupCard(
    title: String,
    icon: String,
    isDarkTheme: Boolean,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isDarkTheme) Color(0x181E293B) else Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 5.dp)
            ) {
                Text(icon, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                )
            }
            content()
        }
    }
}

@Composable
private fun CompactOptionButton(
    title: String,
    isSelected: Boolean,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) {
        if (isDarkTheme) Color(0x350D9488) else Color(0xFFCCFBF1)
    } else {
        if (isDarkTheme) Color(0x20334E68) else Color.White
    }
    val borderColor = if (isSelected) BirizTurquoise else (if (isDarkTheme) Color(0xFF334155) else Color(0xFFCBD5E1))
    val textColor = if (isSelected) {
        if (isDarkTheme) BirizTurquoise else Color(0xFF0F766E)
    } else {
        if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF475569)
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier.height(34.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 4.dp)) {
            Text(
                text = title,
                fontSize = 10.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StarRewardInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isDarkTheme: Boolean
) {
    val currentInt = value.toIntOrNull() ?: 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isDarkTheme) Color(0x221E293B) else Color(0xFFF8FAFC))
            .border(1.dp, if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isDarkTheme) Color.White else Color(0xFF1E293B),
            modifier = Modifier.weight(1f)
        )

        // Modern, son derece net Stepper (Eksi - Sayı/Yıldız - Artı)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Eksi Butonu
            Surface(
                onClick = {
                    val nextVal = (currentInt - 1).coerceAtLeast(0)
                    onValueChange(nextVal.toString())
                },
                shape = RoundedCornerShape(8.dp),
                color = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF475569) else Color(0xFFCBD5E1)),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "−",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) Color.White else Color(0xFF1E293B)
                    )
                }
            }

            // Sayı Alanı: Hem tıklanıp yazılabilir hem de net, büyük ve asla kırpılmayan değer
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isDarkTheme) Color(0xFF0F1E38) else Color.White,
                border = BorderStroke(1.2.dp, if (isDarkTheme) BirizTurquoise else BirizAmber),
                modifier = Modifier
                    .width(70.dp)
                    .height(34.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    BasicTextField(
                        value = value,
                        onValueChange = { newVal ->
                            val digitsOnly = newVal.filter { it.isDigit() }
                            onValueChange(digitsOnly)
                        },
                        textStyle = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDarkTheme) BirizAmberLight else BirizNavyPrimary,
                            textAlign = TextAlign.Center
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(if (isDarkTheme) BirizTurquoise else BirizAmber),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.widthIn(min = 18.dp, max = 36.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "⭐",
                        fontSize = 11.5.sp
                    )
                }
            }

            // Artı Butonu
            Surface(
                onClick = {
                    val nextVal = currentInt + 1
                    onValueChange(nextVal.toString())
                },
                shape = RoundedCornerShape(8.dp),
                color = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF475569) else Color(0xFFCBD5E1)),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "+",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) Color.White else Color(0xFF1E293B)
                    )
                }
            }
        }
    }
}
