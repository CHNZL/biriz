package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Student
import com.example.data.model.Tournament
import com.example.data.model.TournamentMatch
import com.example.data.model.TournamentStanding
import com.example.data.model.TournamentStatus
import com.example.data.model.TournamentType
import com.example.ui.theme.BirizAmber
import com.example.ui.theme.BirizBorder
import com.example.ui.theme.BirizDarkCardBorder
import com.example.ui.theme.BirizDarkCardGlass
import com.example.ui.theme.BirizDarkTextPrimary
import com.example.ui.theme.BirizDarkTextSecondary
import com.example.ui.theme.BirizGold
import com.example.ui.theme.BirizNavyPrimary
import com.example.ui.theme.BirizTextPrimary
import com.example.ui.theme.BirizTextSecondary
import com.example.ui.theme.BirizTurquoise
import com.example.util.ParentPrivacyHelper
import com.example.util.TournamentEngine
import com.example.util.TournamentOutcomeHelper

/**
 * Veli Portalı Turnuvalar Sekmesi:
 * - Turnuva türüne göre (Eleme, Lig, Grup, Grup+Eleme) çocuğun güncel/nihai turnuva sonucunu
 *   ve kazanılan yıldız ödülünü açıkça gösterir.
 * - Sınıf arkadaşlarının kişisel verilerini KVKK kurallarına uygun olarak "Öğrenci" şeklinde maskeler.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ParentTournamentDetailView(
    student: Student,
    tournaments: List<Tournament>,
    selectedTournament: Tournament?,
    matches: List<TournamentMatch>,
    allStudents: List<Student>,
    linkedStudentIds: Set<String>,
    isDarkTheme: Boolean,
    onSelectTournament: (Tournament) -> Unit
) {
    val primaryText = if (isDarkTheme) BirizDarkTextPrimary else BirizTextPrimary
    val secondaryText = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary

    if (selectedTournament == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Sınıfta şu an kayıtlı turnuva bulunmuyor.",
                fontSize = 13.sp,
                color = secondaryText
            )
        }
        return
    }

    val outcome = remember(selectedTournament, matches, allStudents, student.id) {
        TournamentOutcomeHelper.evaluateChildTournamentOutcome(
            tournament = selectedTournament,
            matches = matches,
            students = allStudents,
            childId = student.id
        )
    }

    val childMatches = remember(matches, student.id) {
        matches.filter { it.player1Id == student.id || it.player2Id == student.id }
            .sortedWith(compareBy<TournamentMatch> { if (it.stage == "group") 0 else 1 }.thenBy { it.round }.thenBy { it.matchCode })
    }

    val resolvedPairs = remember(selectedTournament, matches, allStudents) {
        TournamentOutcomeHelper.resolveParticipantPairs(selectedTournament.participants, matches, allStudents)
    }

    val participantCount = remember(selectedTournament, matches) {
        TournamentOutcomeHelper.resolveParticipantCount(selectedTournament, matches)
    }

    val maskedLeagueStandings = remember(selectedTournament, matches, resolvedPairs, linkedStudentIds) {
        if (selectedTournament.type == TournamentType.LIG) {
            val raw = TournamentEngine.calculateStandings(
                participants = resolvedPairs,
                matches = matches,
                pointsWin = selectedTournament.pointsWin,
                pointsDraw = selectedTournament.pointsDraw,
                pointsLoss = selectedTournament.pointsLoss
            )
            ParentPrivacyHelper.maskStandings(raw, linkedStudentIds)
        } else emptyList()
    }

    val groupReports = remember(selectedTournament, matches, allStudents) {
        if (selectedTournament.type == TournamentType.GRUP || selectedTournament.type == TournamentType.GRUP_ELEME) {
            TournamentOutcomeHelper.buildGroupReports(selectedTournament, matches, allStudents)
        } else emptyList()
    }

    var selectedGroupId by remember(selectedTournament.id, outcome.groupName, groupReports) {
        val childGroup = groupReports.firstOrNull { g -> g.standings.any { it.playerId == student.id } }
        mutableStateOf(childGroup?.groupId ?: groupReports.firstOrNull()?.groupId)
    }

    val isTournamentDone = selectedTournament.status == TournamentStatus.TAMAMLANDI

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("parent_tournament_detail_list"),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Birden fazla turnuva varsa seçim şeridi
        if (tournaments.size > 1) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tournaments.forEach { t ->
                        val isSelected = t.id == selectedTournament.id
                        val isDone = t.status == TournamentStatus.TAMAMLANDI
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectTournament(t) },
                            label = {
                                Text(
                                    text = "${if (isDone) "🏁" else "⚡"} ${t.name}",
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        }

        // 1. Turnuva Başlık Kartı
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isTournamentDone) BirizGold.copy(alpha = 0.14f) else if (isDarkTheme) Color(0x280EA5E9) else Color(0xFFE0F2FE),
                border = BorderStroke(1.dp, if (isTournamentDone) BirizGold else BirizTurquoise.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${selectedTournament.type.icon} Sınıf Turnuvası",
                                fontSize = 11.sp,
                                color = if (isTournamentDone) Color(0xFFB45309) else BirizTurquoise,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isTournamentDone) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                                modifier = Modifier.align(Alignment.CenterVertically)
                            ) {
                                Text(
                                    text = if (isTournamentDone) "✓ Tamamlandı" else "Devam Ediyor",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isTournamentDone) Color(0xFF065F46) else Color(0xFF92400E),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            if (participantCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isDarkTheme) Color(0x35334E68) else Color(0xFFF1F5F9),
                                    border = BorderStroke(0.8.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                ) {
                                    Text(
                                        text = "👥 $participantCount Katılımcı",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryText,
                                        modifier = Modifier
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                            .testTag("parent_tournament_participant_count")
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = selectedTournament.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = primaryText
                        )
                        Text(
                            text = "${selectedTournament.type.displayName} • ${selectedTournament.matchType.displayName} • ${matches.count { it.status == "completed" }}/${matches.size} Maç",
                            fontSize = 11.sp,
                            color = secondaryText
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = if (isTournamentDone) "🏆" else selectedTournament.type.icon, fontSize = 32.sp)
                }
            }
        }

        // 2. ÇOCUĞUN TURNUVA SONUÇ / DURUM ÖZET KARTI (Turnuva Türüne Özel)
        item {
            ChildTournamentOutcomeCard(
                studentName = student.fullName,
                tournamentType = selectedTournament.type,
                outcome = outcome,
                isDarkTheme = isDarkTheme
            )
        }

        // 3. TURNUVA TÜRÜNE GÖRE DURUM TABLOSU / AŞAMA GÖRÜNÜMÜ (KVKK MASKELİ)
        when (selectedTournament.type) {
            TournamentType.LIG -> {
                if (maskedLeagueStandings.isNotEmpty()) {
                    item {
                        ParentMaskedStandingsCard(
                            title = "📊 Lig Puan Durumu",
                            subtitle = "Diğer sınıf arkadaşları KVKK gereği gizlenmiştir",
                            standings = maskedLeagueStandings,
                            childId = student.id,
                            advancingCutoff = 0,
                            isDarkTheme = isDarkTheme
                        )
                    }
                }
            }

            TournamentType.GRUP, TournamentType.GRUP_ELEME -> {
                if (groupReports.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (groupReports.size > 1) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    groupReports.forEach { g ->
                                        val isMyGroup = g.standings.any { it.playerId == student.id }
                                        FilterChip(
                                            selected = selectedGroupId == g.groupId,
                                            onClick = { selectedGroupId = g.groupId },
                                            label = {
                                                Text(
                                                    text = if (isMyGroup) "⭐ ${g.groupName} (Grubunuz)" else g.groupName,
                                                    fontSize = 11.sp
                                                )
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                }
                            }

                            val activeGroup = groupReports.firstOrNull { it.groupId == selectedGroupId } ?: groupReports.first()
                            val maskedGroupRows = ParentPrivacyHelper.maskStandings(activeGroup.standings, linkedStudentIds)
                            val cutoff = if (selectedTournament.type == TournamentType.GRUP_ELEME) selectedTournament.advancingPerGroup else 0
                            ParentMaskedStandingsCard(
                                title = "👥 ${activeGroup.groupName} Puan Durumu",
                                subtitle = if (cutoff > 0) "İlk $cutoff Play-Off'a çıkar • ${activeGroup.completedCount}/${activeGroup.matches.size} maç oynandı"
                                else "${activeGroup.completedCount}/${activeGroup.matches.size} maç tamamlandı",
                                standings = maskedGroupRows,
                                childId = student.id,
                                advancingCutoff = cutoff,
                                isDarkTheme = isDarkTheme
                            )
                        }
                    }
                }
            }

            TournamentType.ELEME -> Unit
        }

        // 4. ÇOCUĞUN MAÇLARI VE KARŞILAŞMALARI
        item {
            Column {
                Text(
                    text = "⚔️ Çocuğunuzun Maçları & Karşılaşmaları",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = primaryText
                )
                Text(
                    text = "Oynanan skorlar, tur bilgileri ve sıradaki eşleşmeler (rakipler KVKK gereği gizlidir)",
                    fontSize = 11.sp,
                    color = secondaryText
                )
            }
        }

        if (childMatches.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDarkTheme) BirizDarkCardGlass else Color.White,
                    border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Çocuğunuzun bu turnuvada henüz planlanmış maçı bulunmuyor.",
                        fontSize = 12.sp,
                        color = secondaryText,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            val allKoMatches = matches.filter { it.stage != "group" }
            val maxKoRound = allKoMatches.maxOfOrNull { it.round } ?: 1
            val hasPreElim = TournamentOutcomeHelper.hasPreEliminationRound(allKoMatches)

            items(childMatches, key = { it.id }) { m ->
                val isGroupMatch = m.stage == "group"
                val isKnockoutMatch = !isGroupMatch && (selectedTournament.type == TournamentType.ELEME || selectedTournament.type == TournamentType.GRUP_ELEME)
                val roundLabel = when {
                    m.isThirdPlaceMatch -> "🥉 3.lük Maçı"
                    isKnockoutMatch -> TournamentOutcomeHelper.getRoundDisplayName(m.round, maxKoRound, true, hasPreElim)
                    isGroupMatch -> "${m.groupName ?: "Grup"} • ${m.round}. Hafta"
                    else -> "${m.round}. Hafta"
                }

                val p1Display = formatParentSafeSlot(m, true, matches, linkedStudentIds)
                val p2Display = formatParentSafeSlot(m, false, matches, linkedStudentIds)
                val isCompleted = m.status == "completed"
                val isBye = m.player1Id == "BYE" || m.player2Id == "BYE"
                val isDraw = isCompleted && !isBye && m.winnerId == null && m.score1 != null && m.score1 == m.score2
                val isWon = isCompleted && (m.winnerId == student.id || isBye)

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDarkTheme) BirizDarkCardGlass else Color.White,
                    border = BorderStroke(
                        1.dp,
                        when {
                            isWon -> Color(0xFF10B981).copy(alpha = 0.6f)
                            isCompleted -> if (isDarkTheme) BirizDarkCardBorder else BirizBorder
                            else -> if (isDarkTheme) BirizDarkCardBorder else BirizBorder
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$roundLabel • ${m.matchCode}${if (m.isTieBreaker) " (Karar)" else if (m.matchCode.endsWith("-R")) " (Rövanş)" else ""}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = secondaryText
                            )
                            if (isCompleted) {
                                val (badgeBg, badgeFg, badgeTxt) = when {
                                    isBye -> Triple(Color(0xFFE0F2FE), Color(0xFF0369A1), "BAY Geçti ✓")
                                    isWon -> Triple(Color(0xFFD1FAE5), Color(0xFF065F46), "Kazandı ✓")
                                    isDraw -> Triple(Color(0xFFFEF3C7), Color(0xFF92400E), "Berabere 🤝")
                                    else -> Triple(Color(0xFFFEE2E2), Color(0xFF991B1B), "Kaybetti")
                                }
                                Surface(shape = RoundedCornerShape(5.dp), color = badgeBg) {
                                    Text(
                                        text = badgeTxt,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeFg,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(5.dp),
                                    color = if (isDarkTheme) Color(0x30334E68) else Color(0xFFF1F5F9)
                                ) {
                                    Text(
                                        text = "Oynanacak",
                                        fontSize = 10.sp,
                                        color = secondaryText,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$p1Display  vs  $p2Display",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = primaryText,
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (m.score1 != null && m.score2 != null) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${m.score1} - ${m.score2}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
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

private fun formatParentSafeSlot(
    match: TournamentMatch,
    isPlayer1: Boolean,
    allMatches: List<TournamentMatch>,
    linkedStudentIds: Set<String>
): String {
    val pId = if (isPlayer1) match.player1Id else match.player2Id
    if (pId == "BYE") return "BAY (Tur Atladı)"
    if (TournamentOutcomeHelper.isPlaceholderPlayerId(pId)) {
        return TournamentOutcomeHelper.formatMatchSlotLabel(match, isPlayer1, allMatches, emptyList(), compact = false)
    }
    val pName = if (isPlayer1) match.player1Name else match.player2Name
    return ParentPrivacyHelper.maskStudentName(pId, pName, linkedStudentIds)
}

@Composable
private fun ChildTournamentOutcomeCard(
    studentName: String,
    tournamentType: TournamentType,
    outcome: TournamentOutcomeHelper.ChildTournamentOutcome,
    isDarkTheme: Boolean
) {
    val primaryText = if (isDarkTheme) BirizDarkTextPrimary else BirizTextPrimary
    val secondaryText = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary

    val (gradientColors, borderColor, accentColor) = when (outcome.tone) {
        TournamentOutcomeHelper.OutcomeTone.GOLD -> Triple(
            if (isDarkTheme) listOf(Color(0x45F59E0B), Color(0x25B45309)) else listOf(Color(0xFFFFFBEB), Color(0xFFFEF3C7)),
            BirizGold,
            Color(0xFFB45309)
        )
        TournamentOutcomeHelper.OutcomeTone.SILVER -> Triple(
            if (isDarkTheme) listOf(Color(0x4064748B), Color(0x20334155)) else listOf(Color(0xFFF8FAFC), Color(0xFFE2E8F0)),
            Color(0xFF94A3B8),
            if (isDarkTheme) Color(0xFFE2E8F0) else Color(0xFF334155)
        )
        TournamentOutcomeHelper.OutcomeTone.BRONZE -> Triple(
            if (isDarkTheme) listOf(Color(0x40B45309), Color(0x2078350F)) else listOf(Color(0xFFFFF7ED), Color(0xFFFED7AA)),
            Color(0xFFD97706),
            if (isDarkTheme) Color(0xFFFBBF24) else Color(0xFF9A3412)
        )
        TournamentOutcomeHelper.OutcomeTone.SUCCESS -> Triple(
            if (isDarkTheme) listOf(Color(0x3510B981), Color(0x18065F46)) else listOf(Color(0xFFECFDF5), Color(0xFFD1FAE5)),
            Color(0xFF10B981),
            if (isDarkTheme) Color(0xFF34D399) else Color(0xFF065F46)
        )
        TournamentOutcomeHelper.OutcomeTone.INFO -> Triple(
            if (isDarkTheme) listOf(Color(0x350EA5E9), Color(0x181E3A8A)) else listOf(Color(0xFFF0F9FF), Color(0xFFE0F2FE)),
            BirizTurquoise,
            if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
        )
        TournamentOutcomeHelper.OutcomeTone.NEUTRAL -> Triple(
            if (isDarkTheme) listOf(Color(0x30334E68), Color(0x181E293B)) else listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9)),
            if (isDarkTheme) BirizDarkCardBorder else BirizBorder,
            secondaryText
        )
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.3.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("parent_child_tournament_outcome_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(gradientColors))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${outcome.medalEmoji} ${studentName.uppercase()} • ${tournamentType.displayName.uppercase()}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = borderColor.copy(alpha = 0.22f),
                    border = BorderStroke(0.8.dp, borderColor)
                ) {
                    Text(
                        text = outcome.badgeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = outcome.headline,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = primaryText,
                lineHeight = 20.sp
            )

            Text(
                text = outcome.detailText,
                fontSize = 11.5.sp,
                color = secondaryText,
                lineHeight = 16.sp
            )

            // Lig veya Grup İstatistik Hapları
            outcome.standingRow?.let { st ->
                val displayRank = outcome.groupRank ?: outcome.overallRank
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (displayRank != null) {
                        OutcomeStatChip("Sıra", "#$displayRank", isDarkTheme, Modifier.weight(1f))
                    }
                    OutcomeStatChip("Maç", "${st.played}", isDarkTheme, Modifier.weight(1f))
                    OutcomeStatChip("G/B/M", "${st.won}/${st.drawn}/${st.lost}", isDarkTheme, Modifier.weight(1.3f))
                    OutcomeStatChip("AV", if (st.goalDiff > 0) "+${st.goalDiff}" else "${st.goalDiff}", isDarkTheme, Modifier.weight(1f))
                    OutcomeStatChip("Puan", "${st.points}", isDarkTheme, Modifier.weight(1f))
                }
            }

            // Kazanılan Yıldızlar Sınıfı Ödülü
            outcome.earnedStarsLabel?.let { starLabel ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDarkTheme) Color(0x35F59E0B) else Color(0xFFFEF3C7),
                    border = BorderStroke(1.dp, BirizGold.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "⭐ Turnuva Başarı Ödülü",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) Color(0xFFFBBF24) else Color(0xFF92400E)
                        )
                        Text(
                            text = starLabel,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDarkTheme) Color(0xFFFBBF24) else Color(0xFFB45309)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OutcomeStatChip(
    label: String,
    value: String,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (isDarkTheme) Color(0x451E293B) else Color.White.copy(alpha = 0.85f),
        border = BorderStroke(0.8.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 5.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, fontSize = 9.sp, color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary)
            Text(
                text = value,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
            )
        }
    }
}

@Composable
private fun ParentMaskedStandingsCard(
    title: String,
    subtitle: String,
    standings: List<TournamentStanding>,
    childId: String,
    advancingCutoff: Int,
    isDarkTheme: Boolean
) {
    val primaryText = if (isDarkTheme) BirizDarkTextPrimary else BirizTextPrimary
    val secondaryText = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isDarkTheme) BirizDarkCardGlass else Color.White,
        border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = primaryText)
            Text(text = subtitle, fontSize = 10.5.sp, color = secondaryText)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDarkTheme) Color(0x25334E68) else Color(0xFFF1F5F9), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("#", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = secondaryText, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center)
                Text("Katılımcı", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = secondaryText, modifier = Modifier.weight(1f))
                Text("O", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = secondaryText, modifier = Modifier.width(22.dp), textAlign = TextAlign.Center)
                Text("G", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = secondaryText, modifier = Modifier.width(22.dp), textAlign = TextAlign.Center)
                Text("B", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = secondaryText, modifier = Modifier.width(22.dp), textAlign = TextAlign.Center)
                Text("M", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = secondaryText, modifier = Modifier.width(22.dp), textAlign = TextAlign.Center)
                Text("AV", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = secondaryText, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                Text("P", fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
            }

            standings.forEachIndexed { idx, row ->
                val isChild = row.playerId == childId
                val isAdvancing = advancingCutoff > 0 && idx < advancingCutoff
                val rowBg = when {
                    isChild -> if (isDarkTheme) Color(0x350EA5E9) else Color(0xFFE0F2FE)
                    isAdvancing -> if (isDarkTheme) Color(0x1810B981) else Color(0xFFECFDF5)
                    else -> Color.Transparent
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(rowBg, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isAdvancing) "⬆️${idx + 1}" else "${idx + 1}",
                        fontSize = 10.5.sp,
                        fontWeight = if (isChild || idx == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (isChild) BirizTurquoise else if (isAdvancing) Color(0xFF10B981) else secondaryText,
                        modifier = Modifier.width(24.dp),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (isChild) "${row.playerName} (Çocuğunuz)" else row.playerName,
                        fontSize = 11.sp,
                        fontWeight = if (isChild) FontWeight.ExtraBold else FontWeight.Normal,
                        color = if (isChild) (if (isDarkTheme) BirizTurquoise else BirizNavyPrimary) else secondaryText,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("${row.played}", fontSize = 10.5.sp, color = secondaryText, modifier = Modifier.width(22.dp), textAlign = TextAlign.Center)
                    Text("${row.won}", fontSize = 10.5.sp, color = secondaryText, modifier = Modifier.width(22.dp), textAlign = TextAlign.Center)
                    Text("${row.drawn}", fontSize = 10.5.sp, color = secondaryText, modifier = Modifier.width(22.dp), textAlign = TextAlign.Center)
                    Text("${row.lost}", fontSize = 10.5.sp, color = secondaryText, modifier = Modifier.width(22.dp), textAlign = TextAlign.Center)
                    Text(
                        text = if (row.goalDiff > 0) "+${row.goalDiff}" else "${row.goalDiff}",
                        fontSize = 10.5.sp,
                        color = secondaryText,
                        modifier = Modifier.width(28.dp),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "${row.points}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isChild) (if (isDarkTheme) BirizTurquoise else BirizNavyPrimary) else primaryText,
                        modifier = Modifier.width(28.dp),
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
