package com.example.util

import com.example.data.model.Student
import com.example.data.model.Tournament
import com.example.data.model.TournamentMatch
import com.example.data.model.TournamentMatchType
import com.example.data.model.TournamentStanding
import com.example.data.model.TournamentStatus
import com.example.data.model.TournamentType

/**
 * Turnuva PDF üretimi ve Veli Portalı için ortak, KVKK uyumlu
 * katılımcı çözümleme, eleme şeması düğümleme, grup puan durumu ve çocuk sonuç değerlendirme yardımcısı.
 */
object TournamentOutcomeHelper {

    enum class OutcomeTone {
        GOLD, SILVER, BRONZE, SUCCESS, INFO, NEUTRAL
    }

    data class GroupStandingsReport(
        val groupId: String,
        val groupName: String,
        val standings: List<TournamentStanding>,
        val matches: List<TournamentMatch>,
        val completedCount: Int,
        val lastCompletedMatchText: String
    )

    data class BracketMatchupNode(
        val baseCode: String,
        val round: Int,
        val isThirdPlaceMatch: Boolean,
        val legs: List<TournamentMatch>,
        val player1Label: String,
        val player2Label: String,
        val player1Id: String,
        val player2Id: String,
        val p1SourceBaseCode: String?,
        val p2SourceBaseCode: String?,
        val scoreSummary: String,
        val statusLabel: String,
        val isCompleted: Boolean,
        val isBye: Boolean,
        val winnerId: String?,
        val isTieBreakerPlayed: Boolean
    )

    data class ChildTournamentOutcome(
        val isParticipant: Boolean,
        val isCompleted: Boolean,
        val tone: OutcomeTone,
        val medalEmoji: String,
        val badgeText: String,
        val headline: String,
        val detailText: String,
        val stageReached: String? = null,
        val groupName: String? = null,
        val groupRank: Int? = null,
        val groupTotal: Int? = null,
        val overallRank: Int? = null,
        val overallTotal: Int? = null,
        val standingRow: TournamentStanding? = null,
        val earnedStars: Int = 0,
        val earnedStarsLabel: String? = null
    )

    fun isPlaceholderPlayerId(playerId: String?): Boolean {
        if (playerId.isNullOrBlank()) return true
        return playerId.startsWith("WINNER_OF_") ||
            playerId.startsWith("LOSER_OF_") ||
            playerId.startsWith("GROUP_POS_") ||
            playerId == "TBD"
    }

    fun resolveParticipantName(
        id: String,
        matches: List<TournamentMatch>,
        students: List<Student> = emptyList()
    ): String {
        if (id == "BYE") return "BAY (Tur Atladı)"
        if (id.startsWith("extra:")) {
            return id.removePrefix("extra:").trim().ifBlank { "Misafir Katılımcı" }
        }
        students.firstOrNull { it.id == id }?.fullName?.takeIf { it.isNotBlank() }?.let { return it }
        for (m in matches) {
            if (m.player1Id == id && m.player1Name.isNotBlank() && !isPlaceholderPlayerId(m.player1Id)) {
                return m.player1Name
            }
            if (m.player2Id == id && m.player2Name.isNotBlank() && !isPlaceholderPlayerId(m.player2Id)) {
                return m.player2Name
            }
        }
        return id
    }

    fun resolveParticipantPairs(
        participantIds: List<String>,
        matches: List<TournamentMatch>,
        students: List<Student> = emptyList()
    ): List<Pair<String, String>> {
        return participantIds
            .filter { it.isNotBlank() && it != "BYE" }
            .distinct()
            .map { id -> Pair(id, resolveParticipantName(id, matches, students)) }
    }

    private fun extractSourceMatchCode(source: String?, placeholderId: String): String? {
        if (!source.isNullOrBlank()) {
            when {
                source.startsWith("WINNER:") -> return source.removePrefix("WINNER:")
                source.startsWith("LOSER:") -> return source.removePrefix("LOSER:")
            }
        }
        return when {
            placeholderId.startsWith("WINNER_OF_") -> placeholderId.removePrefix("WINNER_OF_")
            placeholderId.startsWith("LOSER_OF_") -> placeholderId.removePrefix("LOSER_OF_")
            else -> null
        }
    }

    private fun resolveDisplayMatchCode(rawCode: String, allMatches: List<TournamentMatch>): String {
        val base = TournamentEngine.getBaseMatchCode(rawCode)
        val matched = allMatches.firstOrNull {
            !it.isTieBreaker &&
                !it.matchCode.endsWith("-R") &&
                TournamentEngine.getBaseMatchCode(it.matchCode) == base
        }
        return matched?.matchCode ?: rawCode
    }

    fun formatMatchSlotLabel(
        match: TournamentMatch,
        isPlayer1: Boolean,
        allMatches: List<TournamentMatch>,
        students: List<Student> = emptyList(),
        compact: Boolean = false
    ): String {
        val playerId = if (isPlayer1) match.player1Id else match.player2Id
        val playerName = if (isPlayer1) match.player1Name else match.player2Name
        val source = if (isPlayer1) match.player1Source else match.player2Source

        if (playerId == "BYE") {
            return if (compact) "BAY" else "BAY (Tur Atladı)"
        }
        if (playerId.startsWith("GROUP_POS_") || source?.startsWith("GROUP:") == true) {
            return playerName.ifBlank { "Grup Sıra Galibi" }
        }
        if (!isPlaceholderPlayerId(playerId)) {
            val resolved = resolveParticipantName(playerId, allMatches, students)
            return if (resolved != playerId) resolved else playerName.ifBlank { resolved }
        }

        val isLoser = source?.startsWith("LOSER:") == true || playerId.startsWith("LOSER_OF_")
        val rawSourceCode = extractSourceMatchCode(source, playerId)
        if (!rawSourceCode.isNullOrBlank()) {
            val displayCode = resolveDisplayMatchCode(rawSourceCode, allMatches)
            return if (isLoser) {
                if (compact) "$displayCode Kaybedeni" else "$displayCode Maçının Kaybedeni"
            } else {
                if (compact) "$displayCode Kazananı" else "$displayCode Maçının Kazananı"
            }
        }

        return playerName.ifBlank { "Belirlenecek" }
    }

    fun hasPreEliminationRound(knockoutMatches: List<TournamentMatch>): Boolean {
        val r1Count = knockoutMatches.filter { it.round == 1 && !it.isThirdPlaceMatch }
            .map { TournamentEngine.getBaseMatchCode(it.matchCode) }.distinct().size
        val r2Count = knockoutMatches.filter { it.round == 2 && !it.isThirdPlaceMatch }
            .map { TournamentEngine.getBaseMatchCode(it.matchCode) }.distinct().size
        return r2Count > 0 && r1Count != r2Count * 2
    }

    fun getRoundDisplayName(
        round: Int,
        maxRound: Int,
        isKnockout: Boolean,
        hasPreElimination: Boolean = false
    ): String {
        if (!isKnockout) return "$round. Hafta"
        return when {
            round == maxRound -> "Final"
            round == maxRound - 1 && maxRound >= 2 -> "Yarı Final"
            round == maxRound - 2 && maxRound >= 3 -> "Çeyrek Final"
            round == maxRound - 3 && maxRound >= 4 -> "Son 16"
            round == maxRound - 4 && maxRound >= 5 -> "Son 32"
            round == 1 && hasPreElimination -> "Ön Eleme"
            else -> "$round. Tur"
        }
    }

    fun buildGroupReports(
        tournament: Tournament,
        matches: List<TournamentMatch>,
        students: List<Student> = emptyList()
    ): List<GroupStandingsReport> {
        val groupMatches = matches.filter { it.stage != "knockout" }
        if (groupMatches.isEmpty()) return emptyList()

        val grouped = groupMatches.groupBy { it.groupId ?: it.groupName ?: "G1" }
        return grouped.entries.sortedBy { it.key }.map { (gKey, gMatches) ->
            val gName = gMatches.firstNotNullOfOrNull { it.groupName } ?: gKey
            val gParticipants = gMatches.flatMap {
                listOf(it.player1Id, it.player2Id)
            }.filter { it.isNotBlank() && it != "BYE" && !isPlaceholderPlayerId(it) }
                .distinct()
                .map { id -> Pair(id, resolveParticipantName(id, matches, students)) }

            val standings = TournamentEngine.calculateStandings(
                participants = gParticipants,
                matches = gMatches,
                pointsWin = tournament.pointsWin,
                pointsDraw = tournament.pointsDraw,
                pointsLoss = tournament.pointsLoss
            )
            val completed = gMatches.filter { it.status == "completed" }
            val lastMatch = completed.maxWithOrNull(compareBy<TournamentMatch> { it.round }.thenBy { it.matchCode })
            val lastSummary = if (lastMatch != null) {
                val p1 = resolveParticipantName(lastMatch.player1Id, matches, students)
                val p2 = resolveParticipantName(lastMatch.player2Id, matches, students)
                val sc = if (lastMatch.score1 != null && lastMatch.score2 != null) {
                    "${lastMatch.score1}-${lastMatch.score2}"
                } else {
                    val wName = lastMatch.winnerId?.let { resolveParticipantName(it, matches, students) }
                    if (wName != null) "Kazanan: $wName" else "Berabere"
                }
                "Son Oynanan (${lastMatch.matchCode}): $p1 vs $p2 ($sc)"
            } else {
                "Henüz tamamlanan maç yok (Başlangıç Fikstürü)"
            }

            GroupStandingsReport(
                groupId = gKey,
                groupName = gName,
                standings = standings,
                matches = gMatches.sortedWith(compareBy({ it.round }, { it.matchCode })),
                completedCount = completed.size,
                lastCompletedMatchText = lastSummary
            )
        }
    }

    /**
     * Grup + Eleme turnuvalarında grup maçları sürerken henüz Play-Off eşleşmeleri üretilmemişse
     * tüm turnuva fikstürünün şeması PDF'te tam görünsün diye planlanan Play-Off ağacını üretir.
     */
    fun buildProjectedPlayoffMatches(
        tournament: Tournament,
        groupReports: List<GroupStandingsReport>,
        existingKnockouts: List<TournamentMatch>
    ): List<TournamentMatch> {
        if (existingKnockouts.isNotEmpty()) return existingKnockouts
        if (tournament.type != TournamentType.GRUP_ELEME || groupReports.isEmpty()) return emptyList()

        val gCount = groupReports.size
        val pairs = mutableListOf<Pair<Pair<String, String>, Pair<String, String>>>()

        var i = 0
        while (i < gCount) {
            if (i + 1 < gCount) {
                val gA = groupReports[i]
                val gB = groupReports[i + 1]
                pairs.add(
                    Pair(
                        Pair("GROUP_POS_${gA.groupId}_1", "${gA.groupName} 1.si"),
                        Pair("GROUP_POS_${gB.groupId}_2", "${gB.groupName} 2.si")
                    )
                )
                if (tournament.advancingPerGroup >= 2) {
                    pairs.add(
                        Pair(
                            Pair("GROUP_POS_${gB.groupId}_1", "${gB.groupName} 1.si"),
                            Pair("GROUP_POS_${gA.groupId}_2", "${gA.groupName} 2.si")
                        )
                    )
                }
            } else {
                val gSingle = groupReports[i]
                if (tournament.advancingPerGroup >= 2) {
                    pairs.add(
                        Pair(
                            Pair("GROUP_POS_${gSingle.groupId}_1", "${gSingle.groupName} 1.si"),
                            Pair("GROUP_POS_${gSingle.groupId}_2", "${gSingle.groupName} 2.si")
                        )
                    )
                }
            }
            i += 2
        }

        if (pairs.isEmpty()) return emptyList()
        val isDouble = tournament.matchType == TournamentMatchType.CIFT_MAC
        val list = mutableListOf<TournamentMatch>()

        pairs.forEachIndexed { idx, (s1, s2) ->
            val mCode = "KO-R1-M${idx + 1}"
            list.add(
                TournamentMatch(
                    tournamentId = tournament.id,
                    round = 1,
                    player1Id = s1.first,
                    player2Id = s2.first,
                    player1Name = s1.second,
                    player2Name = s2.second,
                    matchCode = mCode,
                    isDoubleMatch = isDouble,
                    pairCode = if (isDouble) "PAIR-$mCode" else null,
                    stage = "knockout"
                )
            )
        }

        var prevRound = 1
        var curSlots = pairs.size
        var curRound = 2
        while (curSlots > 1) {
            val nextSlots = curSlots / 2
            if (curSlots == 2) {
                val thirdCode = "R$curRound-M-3RD"
                val l1Code = if (prevRound == 1) "KO-R1-M1" else "R$prevRound-M1"
                val l2Code = if (prevRound == 1) "KO-R1-M2" else "R$prevRound-M2"
                list.add(
                    TournamentMatch(
                        tournamentId = tournament.id,
                        round = curRound,
                        player1Id = "LOSER_OF_$l1Code",
                        player2Id = "LOSER_OF_$l2Code",
                        player1Name = "$l1Code Maçının Kaybedeni",
                        player2Name = "$l2Code Maçının Kaybedeni",
                        matchCode = thirdCode,
                        isDoubleMatch = isDouble,
                        isThirdPlaceMatch = true,
                        player1Source = "LOSER:$l1Code",
                        player2Source = "LOSER:$l2Code",
                        stage = "knockout"
                    )
                )
            }
            for (k in 0 until nextSlots) {
                val mCode = "R$curRound-M${k + 1}"
                val s1Code = if (prevRound == 1) "KO-R1-M${k * 2 + 1}" else "R$prevRound-M${k * 2 + 1}"
                val s2Code = if (prevRound == 1) "KO-R1-M${k * 2 + 2}" else "R$prevRound-M${k * 2 + 2}"
                list.add(
                    TournamentMatch(
                        tournamentId = tournament.id,
                        round = curRound,
                        player1Id = "WINNER_OF_$s1Code",
                        player2Id = "WINNER_OF_$s2Code",
                        player1Name = "$s1Code Maçının Kazananı",
                        player2Name = "$s2Code Maçının Kazananı",
                        matchCode = mCode,
                        isDoubleMatch = isDouble,
                        player1Source = "WINNER:$s1Code",
                        player2Source = "WINNER:$s2Code",
                        stage = "knockout"
                    )
                )
            }
            prevRound = curRound
            curRound++
            curSlots = nextSlots
        }

        return list
    }

    fun buildBracketColumns(
        tournament: Tournament,
        knockoutMatches: List<TournamentMatch>,
        students: List<Student> = emptyList()
    ): Map<Int, List<BracketMatchupNode>> {
        if (knockoutMatches.isEmpty()) return emptyMap()
        val byRound = knockoutMatches.groupBy { it.round }.toSortedMap()

        return byRound.mapValues { (_, roundMatches) ->
            val byBaseCode = roundMatches.groupBy { TournamentEngine.getBaseMatchCode(it.matchCode) }
            val orderedCodes = byBaseCode.keys.sortedWith(
                compareBy<String> { code ->
                    val legs = byBaseCode[code].orEmpty()
                    if (legs.any { it.isThirdPlaceMatch }) 1 else 0
                }.thenBy { code ->
                    val matchNumber = Regex("""M(\d+)""").find(code)?.groupValues?.getOrNull(1)?.toIntOrNull()
                    matchNumber ?: Int.MAX_VALUE
                }.thenBy { it }
            )

            orderedCodes.map { baseCode ->
                val legs = byBaseCode[baseCode].orEmpty().sortedWith(
                    compareBy<TournamentMatch> {
                        when {
                            it.isTieBreaker -> 2
                            it.matchCode.endsWith("-R") -> 1
                            else -> 0
                        }
                    }
                )
                val primary = legs.first()
                val outcome = TournamentEngine.determineMatchupResult(
                    baseCode = baseCode,
                    allMatches = knockoutMatches,
                    selectionMethod = tournament.winnerSelectionMethod
                )
                val isBye = primary.player1Id == "BYE" || primary.player2Id == "BYE"
                val p1Label = formatMatchSlotLabel(primary, true, knockoutMatches, students, compact = false)
                val p2Label = formatMatchSlotLabel(primary, false, knockoutMatches, students, compact = false)

                val p1SourceRaw = extractSourceMatchCode(primary.player1Source, primary.player1Id)
                val p2SourceRaw = extractSourceMatchCode(primary.player2Source, primary.player2Id)

                val scoreText = buildBracketScoreSummary(legs, primary, outcome, isBye)
                val statusText = when {
                    isBye -> "BAY"
                    outcome.isCompleted -> "Tamamlandı"
                    legs.any { it.status == "completed" } -> "Devam Ediyor"
                    else -> "Oynanacak"
                }

                BracketMatchupNode(
                    baseCode = baseCode,
                    round = primary.round,
                    isThirdPlaceMatch = primary.isThirdPlaceMatch,
                    legs = legs,
                    player1Label = p1Label,
                    player2Label = p2Label,
                    player1Id = primary.player1Id,
                    player2Id = primary.player2Id,
                    p1SourceBaseCode = p1SourceRaw?.let { TournamentEngine.getBaseMatchCode(it) },
                    p2SourceBaseCode = p2SourceRaw?.let { TournamentEngine.getBaseMatchCode(it) },
                    scoreSummary = scoreText,
                    statusLabel = statusText,
                    isCompleted = outcome.isCompleted || isBye,
                    isBye = isBye,
                    winnerId = outcome.winnerId ?: if (isBye) {
                        if (primary.player1Id == "BYE") primary.player2Id else primary.player1Id
                    } else null,
                    isTieBreakerPlayed = legs.any { it.isTieBreaker && it.status == "completed" }
                )
            }
        }
    }

    private fun buildBracketScoreSummary(
        legs: List<TournamentMatch>,
        primary: TournamentMatch,
        outcome: TournamentEngine.MatchupResult,
        isBye: Boolean
    ): String {
        if (isBye) return "BAY"
        val leg1 = legs.firstOrNull { !it.isTieBreaker && !it.matchCode.endsWith("-R") } ?: primary
        val leg2 = legs.firstOrNull { !it.isTieBreaker && it.matchCode.endsWith("-R") }
        val tie = legs.firstOrNull { it.isTieBreaker }

        if (legs.none { it.status == "completed" }) return "-"

        if (leg2 == null) {
            return if (leg1.score1 != null && leg1.score2 != null) {
                "${leg1.score1} - ${leg1.score2}"
            } else if (outcome.isCompleted) {
                "✓"
            } else {
                "-"
            }
        }

        val l1Text = when {
            leg1.status != "completed" -> "?"
            leg1.score1 != null && leg1.score2 != null -> "${leg1.score1}-${leg1.score2}"
            leg1.winnerId == primary.player1Id -> "G-M"
            else -> "M-G"
        }
        val l2Text = when {
            leg2.status != "completed" -> "?"
            leg2.score1 != null && leg2.score2 != null -> "${leg2.score2}-${leg2.score1}"
            leg2.winnerId == primary.player1Id -> "G-M"
            else -> "M-G"
        }
        val tieText = if (tie != null && tie.status == "completed") {
            if (tie.score1 != null && tie.score2 != null) " | K:${tie.score1}-${tie.score2}" else " | K:✓"
        } else if (tie != null) {
            " | Karar"
        } else ""
        return "$l1Text / $l2Text$tieText"
    }

    /**
     * Veli Portalı için çocuğun turnuva durumunu ve turnuva bittiğindeki nihai sonucunu
     * turnuvanın türüne (Eleme, Lig, Grup, Grup+Eleme) göre profesyonelce hesaplar.
     */
    fun evaluateChildTournamentOutcome(
        tournament: Tournament,
        matches: List<TournamentMatch>,
        students: List<Student>,
        childId: String
    ): ChildTournamentOutcome {
        val childMatches = matches.filter { it.player1Id == childId || it.player2Id == childId }
        val isParticipant = tournament.participants.contains(childId) || childMatches.isNotEmpty()
        val isTournamentCompleted = tournament.status == TournamentStatus.TAMAMLANDI

        if (!isParticipant) {
            return ChildTournamentOutcome(
                isParticipant = false,
                isCompleted = isTournamentCompleted,
                tone = OutcomeTone.NEUTRAL,
                medalEmoji = "ℹ️",
                badgeText = "Katılımcı Değil",
                headline = "Çocuğunuz Bu Turnuvada Yer Almıyor",
                detailText = "Bu sınıf turnuvasının katılımcı listesinde çocuğunuz bulunmuyor."
            )
        }

        val resolvedPairs = resolveParticipantPairs(tournament.participants, matches, students)
        val allStandings = TournamentEngine.calculateStandings(
            participants = resolvedPairs,
            matches = matches,
            pointsWin = tournament.pointsWin,
            pointsDraw = tournament.pointsDraw,
            pointsLoss = tournament.pointsLoss
        )
        val podium = TournamentEngine.determinePodium(tournament, matches, allStandings)
        val effectiveWinnerId = tournament.winnerId ?: podium.first?.first
        val effectiveRunnerUpId = tournament.runnerUpId ?: podium.second?.first
        val effectiveThirdId = tournament.thirdPlaceId ?: podium.third?.first

        fun calculateEarnedStars(rank: Int?): Pair<Int, String?> {
            if (!isTournamentCompleted || !tournament.starRewards.enabled) return Pair(0, null)
            val r = tournament.starRewards
            return when (rank) {
                1 -> Pair(r.firstPlaceStars, if (r.firstPlaceStars > 0) "+${r.firstPlaceStars} ⭐ Şampiyonluk Ödülü" else null)
                2 -> Pair(r.secondPlaceStars, if (r.secondPlaceStars > 0) "+${r.secondPlaceStars} ⭐ İkincilik Ödülü" else null)
                3 -> Pair(r.thirdPlaceStars, if (r.thirdPlaceStars > 0) "+${r.thirdPlaceStars} ⭐ Üçüncülük Ödülü" else null)
                else -> Pair(r.participationStars, if (r.participationStars > 0) "+${r.participationStars} ⭐ Katılım Ödülü" else null)
            }
        }

        return when (tournament.type) {
            TournamentType.LIG -> {
                val idx = allStandings.indexOfFirst { it.playerId == childId }
                val rank = if (idx >= 0) idx + 1 else null
                val row = allStandings.getOrNull(idx)
                val total = allStandings.size
                val statsLine = if (row != null) {
                    "${row.played} maç • ${row.won}G ${row.drawn}B ${row.lost}M • Averaj ${if (row.goalDiff > 0) "+${row.goalDiff}" else "${row.goalDiff}"} • ${row.points} Puan"
                } else {
                    "Henüz lig maçı tamamlanmadı"
                }

                if (isTournamentCompleted && rank != null) {
                    val (stars, starLabel) = calculateEarnedStars(rank)
                    val (tone, emoji, badge, headline) = when (rank) {
                        1 -> Quad(OutcomeTone.GOLD, "🥇", "LİG ŞAMPİYONU", "Turnuvayı Lig Şampiyonu (1.) Olarak Tamamladı!")
                        2 -> Quad(OutcomeTone.SILVER, "🥈", "LİG 2.'Sİ", "Turnuvayı 2. Sırada Tamamladı!")
                        3 -> Quad(OutcomeTone.BRONZE, "🥉", "LİG 3.'SÜ", "Turnuvayı 3. Sırada Tamamladı!")
                        else -> Quad(OutcomeTone.INFO, "📊", "$rank. SIRA / $total", "Lig Tablosunu $rank. Sırada Tamamladı")
                    }
                    ChildTournamentOutcome(
                        isParticipant = true,
                        isCompleted = true,
                        tone = tone,
                        medalEmoji = emoji,
                        badgeText = badge,
                        headline = headline,
                        detailText = statsLine,
                        overallRank = rank,
                        overallTotal = total,
                        standingRow = row,
                        earnedStars = stars,
                        earnedStarsLabel = starLabel
                    )
                } else {
                    val playedAny = (row?.played ?: 0) > 0
                    val rankText = if (rank != null && playedAny) "Şu An $rank. Sırada ($total Katılımcı)" else "Lig Mücadelesi Devam Ediyor"
                    ChildTournamentOutcome(
                        isParticipant = true,
                        isCompleted = false,
                        tone = OutcomeTone.INFO,
                        medalEmoji = "⚡",
                        badgeText = if (rank != null && playedAny) "Anlık $rank." else "Devam Ediyor",
                        headline = rankText,
                        detailText = statsLine,
                        overallRank = rank,
                        overallTotal = total,
                        standingRow = row
                    )
                }
            }

            TournamentType.GRUP -> {
                val groupReports = buildGroupReports(tournament, matches, students)
                val myGroup = groupReports.firstOrNull { g -> g.standings.any { it.playerId == childId } }
                val gIdx = myGroup?.standings?.indexOfFirst { it.playerId == childId } ?: -1
                val gRank = if (gIdx >= 0) gIdx + 1 else null
                val gRow = if (gIdx >= 0) myGroup?.standings?.getOrNull(gIdx) else null
                val overallIdx = allStandings.indexOfFirst { it.playerId == childId }
                val overallRank = if (overallIdx >= 0) overallIdx + 1 else null
                val podiumRank = when (childId) {
                    effectiveWinnerId -> 1
                    effectiveRunnerUpId -> 2
                    effectiveThirdId -> 3
                    else -> overallRank
                }

                val statsLine = if (gRow != null) {
                    "${gRow.played} maç • ${gRow.won}G ${gRow.drawn}B ${gRow.lost}M • Averaj ${if (gRow.goalDiff > 0) "+${gRow.goalDiff}" else "${gRow.goalDiff}"} • ${gRow.points} Puan"
                } else {
                    "Grup karşılaşmaları bekleniyor"
                }

                if (isTournamentCompleted) {
                    val (stars, starLabel) = calculateEarnedStars(podiumRank)
                    val gLabel = myGroup?.groupName ?: "Grubu"
                    val (tone, emoji, badge, headline) = when (podiumRank) {
                        1 -> Quad(OutcomeTone.GOLD, "🥇", "TURNUVA ŞAMPİYONU", "$gLabel 1.'si ve Turnuva Şampiyonu Oldu!")
                        2 -> Quad(OutcomeTone.SILVER, "🥈", "TURNUVA 2.'Sİ", "$gLabel'nu ${gRank ?: "-"}. Bitirdi • Genel 2. Oldu!")
                        3 -> Quad(OutcomeTone.BRONZE, "🥉", "TURNUVA 3.'SÜ", "$gLabel'nu ${gRank ?: "-"}. Bitirdi • Genel 3. Oldu!")
                        else -> Quad(OutcomeTone.INFO, "👥", "$gLabel ${gRank ?: "-"}.", "$gLabel'nu ${gRank ?: "-"}. Sırada Tamamladı")
                    }
                    ChildTournamentOutcome(
                        isParticipant = true,
                        isCompleted = true,
                        tone = tone,
                        medalEmoji = emoji,
                        badgeText = badge,
                        headline = headline,
                        detailText = statsLine,
                        groupName = myGroup?.groupName,
                        groupRank = gRank,
                        groupTotal = myGroup?.standings?.size,
                        overallRank = podiumRank,
                        overallTotal = allStandings.size,
                        standingRow = gRow,
                        earnedStars = stars,
                        earnedStarsLabel = starLabel
                    )
                } else {
                    val gLabel = myGroup?.groupName ?: "Grup"
                    ChildTournamentOutcome(
                        isParticipant = true,
                        isCompleted = false,
                        tone = OutcomeTone.INFO,
                        medalEmoji = "👥",
                        badgeText = if (gRank != null) "$gLabel • $gRank." else gLabel,
                        headline = if (gRank != null) "$gLabel'nda Şu An $gRank. Sırada" else "$gLabel Maçları Devam Ediyor",
                        detailText = statsLine,
                        groupName = myGroup?.groupName,
                        groupRank = gRank,
                        groupTotal = myGroup?.standings?.size,
                        standingRow = gRow
                    )
                }
            }

            TournamentType.ELEME -> {
                evaluateKnockoutOutcomeForChild(
                    tournament = tournament,
                    knockoutMatches = matches.filter { it.stage != "group" },
                    childId = childId,
                    isTournamentCompleted = isTournamentCompleted,
                    effectiveWinnerId = effectiveWinnerId,
                    effectiveRunnerUpId = effectiveRunnerUpId,
                    effectiveThirdId = effectiveThirdId,
                    groupPrefix = null,
                    groupName = null,
                    groupRank = null,
                    groupTotal = null,
                    calculateEarnedStars = ::calculateEarnedStars
                )
            }

            TournamentType.GRUP_ELEME -> {
                val groupReports = buildGroupReports(tournament, matches, students)
                val myGroup = groupReports.firstOrNull { g -> g.standings.any { it.playerId == childId } }
                val gIdx = myGroup?.standings?.indexOfFirst { it.playerId == childId } ?: -1
                val gRank = if (gIdx >= 0) gIdx + 1 else null
                val gRow = if (gIdx >= 0) myGroup?.standings?.getOrNull(gIdx) else null
                val gName = myGroup?.groupName ?: "Grubu"

                val knockoutMatches = matches.filter { it.stage == "knockout" }
                val playedKnockout = knockoutMatches.any { it.player1Id == childId || it.player2Id == childId }
                val groupStageComplete = myGroup != null && myGroup.matches.isNotEmpty() && myGroup.matches.all { it.status == "completed" }

                if (knockoutMatches.isEmpty() || !playedKnockout) {
                    val statsLine = if (gRow != null) {
                        "${gRow.played} grup maçı • ${gRow.won}G ${gRow.drawn}B ${gRow.lost}M • ${gRow.points} Puan"
                    } else ""
                    val qualified = gRank != null && gRank <= tournament.advancingPerGroup

                    if (isTournamentCompleted || (groupStageComplete && knockoutMatches.isNotEmpty() && !playedKnockout)) {
                        val (stars, starLabel) = calculateEarnedStars(null)
                        return ChildTournamentOutcome(
                            isParticipant = true,
                            isCompleted = isTournamentCompleted,
                            tone = OutcomeTone.NEUTRAL,
                            medalEmoji = "🏁",
                            badgeText = "$gName ${gRank ?: "-"}.",
                            headline = "$gName'nu ${gRank ?: "-"}. Sırada Tamamlayarak Grup Aşamasında Elendi",
                            detailText = "$statsLine • İlk ${tournament.advancingPerGroup} Play-Off'a yükseldi.",
                            stageReached = "Grup Aşaması",
                            groupName = myGroup?.groupName,
                            groupRank = gRank,
                            groupTotal = myGroup?.standings?.size,
                            standingRow = gRow,
                            earnedStars = stars,
                            earnedStarsLabel = starLabel
                        )
                    }

                    val liveHeadline = when {
                        groupStageComplete && qualified -> "$gName'nu $gRank. Tamamlayıp Play-Off'a Yükseldi!"
                        gRank != null && qualified -> "$gName'nda $gRank. Sırada (Play-Off Hattında ⬆️)"
                        gRank != null -> "$gName'nda $gRank. Sırada"
                        else -> "Grup Aşaması Devam Ediyor"
                    }
                    return ChildTournamentOutcome(
                        isParticipant = true,
                        isCompleted = false,
                        tone = if (qualified) OutcomeTone.SUCCESS else OutcomeTone.INFO,
                        medalEmoji = if (qualified) "⬆️" else "👥",
                        badgeText = if (gRank != null) "$gName • $gRank." else gName,
                        headline = liveHeadline,
                        detailText = statsLine,
                        stageReached = "Grup Aşaması",
                        groupName = myGroup?.groupName,
                        groupRank = gRank,
                        groupTotal = myGroup?.standings?.size,
                        standingRow = gRow
                    )
                }

                // Play-Off (eleme) aşamasına yükselmiş çocuk
                evaluateKnockoutOutcomeForChild(
                    tournament = tournament,
                    knockoutMatches = knockoutMatches,
                    childId = childId,
                    isTournamentCompleted = isTournamentCompleted,
                    effectiveWinnerId = effectiveWinnerId,
                    effectiveRunnerUpId = effectiveRunnerUpId,
                    effectiveThirdId = effectiveThirdId,
                    groupPrefix = "$gName (${gRank ?: "-"}.) → ",
                    groupName = myGroup?.groupName,
                    groupRank = gRank,
                    groupTotal = myGroup?.standings?.size,
                    calculateEarnedStars = ::calculateEarnedStars
                )
            }
        }
    }

    private fun evaluateKnockoutOutcomeForChild(
        tournament: Tournament,
        knockoutMatches: List<TournamentMatch>,
        childId: String,
        isTournamentCompleted: Boolean,
        effectiveWinnerId: String?,
        effectiveRunnerUpId: String?,
        effectiveThirdId: String?,
        groupPrefix: String?,
        groupName: String?,
        groupRank: Int?,
        groupTotal: Int?,
        calculateEarnedStars: (Int?) -> Pair<Int, String?>
    ): ChildTournamentOutcome {
        val prefix = groupPrefix ?: ""
        val rounds = knockoutMatches.map { it.round }.distinct().sorted()
        val maxRound = rounds.maxOrNull() ?: 1
        val hasPreElim = hasPreEliminationRound(knockoutMatches)

        val childKoMatches = knockoutMatches.filter { it.player1Id == childId || it.player2Id == childId }
        val completedCount = childKoMatches.count { it.status == "completed" && it.player1Id != "BYE" && it.player2Id != "BYE" }
        val baseCodes = childKoMatches.map { TournamentEngine.getBaseMatchCode(it.matchCode) }.distinct()

        var winMatchups = 0
        var lossMatchups = 0
        var lostRound: Int? = null
        var lostThirdPlace = false
        var wonThirdPlace = false
        var pendingRound: Int? = null
        var pendingThirdPlace = false

        baseCodes.forEach { bc ->
            val legs = childKoMatches.filter { TournamentEngine.getBaseMatchCode(it.matchCode) == bc }
            val is3rd = legs.any { it.isThirdPlaceMatch }
            val round = legs.first().round
            val outcome = TournamentEngine.determineMatchupResult(bc, knockoutMatches, tournament.winnerSelectionMethod)
            if (outcome.isCompleted) {
                if (outcome.winnerId == childId) {
                    winMatchups++
                    if (is3rd) wonThirdPlace = true
                } else if (outcome.loserId == childId) {
                    lossMatchups++
                    if (is3rd) lostThirdPlace = true
                    else lostRound = maxOf(lostRound ?: 0, round)
                }
            } else {
                if (is3rd) pendingThirdPlace = true
                else pendingRound = maxOf(pendingRound ?: 0, round)
            }
        }

        val statsSummary = "$completedCount eleme maçı • $winMatchups tur galibiyeti"

        if (childId == effectiveWinnerId) {
            val (stars, starLabel) = calculateEarnedStars(1)
            return ChildTournamentOutcome(
                isParticipant = true,
                isCompleted = isTournamentCompleted,
                tone = OutcomeTone.GOLD,
                medalEmoji = "🥇",
                badgeText = "TURNUVA ŞAMPİYONU",
                headline = "${prefix}Turnuva Şampiyonu Oldu!",
                detailText = "Final maçını kazanarak 1.lik kürsüsüne çıktı. ($statsSummary)",
                stageReached = "Final (Şampiyon)",
                groupName = groupName,
                groupRank = groupRank,
                groupTotal = groupTotal,
                overallRank = 1,
                earnedStars = stars,
                earnedStarsLabel = starLabel
            )
        }

        if (childId == effectiveRunnerUpId) {
            val (stars, starLabel) = calculateEarnedStars(2)
            return ChildTournamentOutcome(
                isParticipant = true,
                isCompleted = isTournamentCompleted,
                tone = OutcomeTone.SILVER,
                medalEmoji = "🥈",
                badgeText = "TURNUVA 2.'Sİ",
                headline = "${prefix}Turnuvayı 2. Sırada Tamamladı!",
                detailText = "Finale kadar yükselerek gümüş madalya derecesi elde etti. ($statsSummary)",
                stageReached = "Final (2.lik)",
                groupName = groupName,
                groupRank = groupRank,
                groupTotal = groupTotal,
                overallRank = 2,
                earnedStars = stars,
                earnedStarsLabel = starLabel
            )
        }

        if (childId == effectiveThirdId || wonThirdPlace) {
            val (stars, starLabel) = calculateEarnedStars(3)
            return ChildTournamentOutcome(
                isParticipant = true,
                isCompleted = isTournamentCompleted,
                tone = OutcomeTone.BRONZE,
                medalEmoji = "🥉",
                badgeText = "TURNUVA 3.'SÜ",
                headline = "${prefix}Turnuvayı 3. Sırada Tamamladı!",
                detailText = "Yarı final sonrası 3.lük maçını kazanarak bronz madalya derecesi elde etti.",
                stageReached = "3.lük Maçı (3.)",
                groupName = groupName,
                groupRank = groupRank,
                groupTotal = groupTotal,
                overallRank = 3,
                earnedStars = stars,
                earnedStarsLabel = starLabel
            )
        }

        if (lostThirdPlace) {
            val (stars, starLabel) = calculateEarnedStars(4)
            return ChildTournamentOutcome(
                isParticipant = true,
                isCompleted = isTournamentCompleted,
                tone = OutcomeTone.INFO,
                medalEmoji = "🏅",
                badgeText = "4. SIRA (YARI FİNALİST)",
                headline = "${prefix}Yarı Finale Yükseldi — Turnuvayı 4. Tamamladı",
                detailText = "Yarı final ve 3.lük karşılaşmalarında mücadele etti. ($statsSummary)",
                stageReached = "Yarı Final (4.)",
                groupName = groupName,
                groupRank = groupRank,
                groupTotal = groupTotal,
                overallRank = 4,
                earnedStars = stars,
                earnedStarsLabel = starLabel
            )
        }

        if (pendingRound != null || pendingThirdPlace) {
            val stageName = if (pendingThirdPlace) {
                "3.lük Maçı"
            } else {
                getRoundDisplayName(pendingRound!!, maxRound, isKnockout = true, hasPreElimination = hasPreElim)
            }
            return ChildTournamentOutcome(
                isParticipant = true,
                isCompleted = false,
                tone = OutcomeTone.SUCCESS,
                medalEmoji = "⚡",
                badgeText = stageName.uppercase(),
                headline = "${prefix}$stageName Aşamasında Mücadele Ediyor",
                detailText = "Sıradaki karşılaşma oynanmayı bekliyor. ($statsSummary)",
                stageReached = stageName,
                groupName = groupName,
                groupRank = groupRank,
                groupTotal = groupTotal
            )
        }

        if (lostRound != null) {
            val stageName = getRoundDisplayName(lostRound!!, maxRound, isKnockout = true, hasPreElimination = hasPreElim)
            // Yarı finalde kaybedip 3.lük maçının henüz tamamlanmadığı durumu kontrol et
            val isSemiFinalWaiting3rd = (lostRound == maxRound - 1 && maxRound >= 2 && !isTournamentCompleted)
            if (isSemiFinalWaiting3rd) {
                return ChildTournamentOutcome(
                    isParticipant = true,
                    isCompleted = false,
                    tone = OutcomeTone.INFO,
                    medalEmoji = "🥉",
                    badgeText = "3.LÜK MAÇI",
                    headline = "${prefix}Yarı Final Sonrası 3.lük Maçına Çıkacak",
                    detailText = "Diğer yarı final eşleşmesinin tamamlanması bekleniyor.",
                    stageReached = "3.lük Maçı",
                    groupName = groupName,
                    groupRank = groupRank,
                    groupTotal = groupTotal
                )
            }
            val (stars, starLabel) = calculateEarnedStars(null)
            return ChildTournamentOutcome(
                isParticipant = true,
                isCompleted = isTournamentCompleted,
                tone = OutcomeTone.NEUTRAL,
                medalEmoji = "🏁",
                badgeText = stageName,
                headline = "${prefix}$stageName Aşamasında Elendi",
                detailText = "$stageName turuna kadar yükseldi. ($statsSummary)",
                stageReached = stageName,
                groupName = groupName,
                groupRank = groupRank,
                groupTotal = groupTotal,
                earnedStars = stars,
                earnedStarsLabel = starLabel
            )
        }

        val (stars, starLabel) = calculateEarnedStars(null)
        return ChildTournamentOutcome(
            isParticipant = true,
            isCompleted = isTournamentCompleted,
            tone = OutcomeTone.INFO,
            medalEmoji = "🏆",
            badgeText = if (isTournamentCompleted) "KATILIMCI" else "DEVAM EDİYOR",
            headline = if (isTournamentCompleted) "${prefix}Turnuvayı Tamamladı" else "${prefix}Turnuva Devam Ediyor",
            detailText = statsSummary,
            groupName = groupName,
            groupRank = groupRank,
            groupTotal = groupTotal,
            earnedStars = stars,
            earnedStarsLabel = starLabel
        )
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
