package com.example.util

import com.example.data.model.Tournament
import com.example.data.model.TournamentMatch
import com.example.data.model.TournamentMatchType
import com.example.data.model.TournamentStanding
import com.example.data.model.TournamentType
import com.example.data.model.WinnerSelectionMethod
import java.util.UUID

/**
 * Turnuva ve Fikstür Matematiksel Motoru (Tournament Engine)
 * - Eleme Usulü (Single Elimination) & 2^k Matematiksel BAY Algoritması
 * - Lig Usulü (Round-Robin) & Berger Rotasyon Çember Algoritması
 * - Grup Dağıtımı ve Grup + Eleme Geçiş Motoru
 * - Çift Maç İkili Averaj ve Otomatik Beraberlik Bozma (Tie-Breaker)
 * - Canlı Puan Tablosu Sıralama ve Eşitlik Bozma Kuralları
 */
object TournamentEngine {

    /**
     * En küçük 2'nin kuvveti M >= N bulur.
     */
    fun findSmallestPowerOfTwo(n: Int): Int {
        if (n <= 1) return 1
        var p = 1
        while (p < n) {
            p = p shl 1
        }
        return p
    }

    /**
     * 1. ELEME USULÜ (KNOCKOUT) FİKSTÜR ÜRETİMİ (2^n VE ÖN ELEME ALGORİTMASI)
     * - Eğer N tam 2'nin kuvveti ise (Örn: 16, 8, 4): Ön elemesiz doğrudan N/2 maç oluşturulur.
     * - Eğer N tam 2'nin kuvveti değilse (Örn: N = 25):
     *   - En yakın alt 2'nin kuvveti P (Örn: P = 16).
     *   - Ön Eleme Maç Sayısı: M_pre = N - P (Örn: 25 - 16 = 9 maç).
     *   - Ön Elemeye Katılan Oyuncu Sayısı: 2 * M_pre = 18 öğrenci.
     *   - Ön Elemede BAY Geçen (Doğrudan Son 16'ya Yükselen) Öğrenci Sayısı: B = 2*P - N (Örn: 32 - 25 = 7 öğrenci).
     *   - 2. Tur (Son 16): 7 BAY geçen öğrenci + 9 Ön Eleme galibi = 16 Öğrenci (8 Maç).
     */
    fun generateKnockoutFixture(
        tournamentId: String,
        participants: List<Pair<String, String>>, // Pair(id, name)
        isDoubleMatch: Boolean = false,
        allTree: Boolean = true
    ): List<TournamentMatch> {
        val n = participants.size
        if (n < 2) return emptyList()

        val shuffled = participants.shuffled()
        val matches = mutableListOf<TournamentMatch>()

        // Tam 2'nin kuvveti kontrolü (2, 4, 8, 16, 32...)
        val isExactPowerOfTwo = (n and (n - 1)) == 0

        if (isExactPowerOfTwo) {
            val pairCount = n / 2
            for (i in 0 until pairCount) {
                val p1 = shuffled[i * 2]
                val p2 = shuffled[i * 2 + 1]
                val matchCode = "R1-M${i + 1}"
                val match = TournamentMatch(
                    tournamentId = tournamentId,
                    round = 1,
                    player1Id = p1.first,
                    player2Id = p2.first,
                    player1Name = p1.second,
                    player2Name = p2.second,
                    matchCode = matchCode,
                    isDoubleMatch = isDoubleMatch,
                    pairCode = if (isDoubleMatch) "PAIR-R1-M${i + 1}" else null,
                    stage = "knockout"
                )
                matches.add(match)

                if (isDoubleMatch) {
                    matches.add(
                        TournamentMatch(
                            tournamentId = tournamentId,
                            round = 1,
                            player1Id = p2.first,
                            player2Id = p1.first,
                            player1Name = p2.second,
                            player2Name = p1.second,
                            matchCode = "R1-M${i + 1}-R",
                            isDoubleMatch = true,
                            pairCode = "PAIR-R1-M${i + 1}",
                            stage = "knockout"
                        )
                    )
                }
            }

            if (allTree && pairCount > 1) {
                generateSubsequentKnockoutRounds(tournamentId, pairCount, isDoubleMatch, matches)
            }
        } else {
            // N 2'nin kuvveti değil: Ön Eleme oynatıp ana eleme ağacına eşitliyoruz
            var p = 1
            while (p * 2 < n) {
                p *= 2
            }

            val preMatchCount = n - p               // Ön eleme maç sayısı (Örn: 25 - 16 = 9)
            val prePlayerCount = preMatchCount * 2     // Ön elemeye katılanlar (Örn: 18)
            val byeCount = n - prePlayerCount       // Ön elemeyi BAY geçenler (Örn: 25 - 18 = 7)

            val prePlayers = shuffled.take(prePlayerCount)
            val byePlayers = shuffled.drop(prePlayerCount)

            // 1. TUR: Ön Eleme Maçları
            val preWinnerPlaceholders = mutableListOf<Pair<String, String>>()
            for (i in 0 until preMatchCount) {
                val p1 = prePlayers[i * 2]
                val p2 = prePlayers[i * 2 + 1]
                val matchCode = "R1-M${i + 1}"
                val match = TournamentMatch(
                    tournamentId = tournamentId,
                    round = 1,
                    player1Id = p1.first,
                    player2Id = p2.first,
                    player1Name = p1.second,
                    player2Name = p2.second,
                    matchCode = matchCode,
                    isDoubleMatch = isDoubleMatch,
                    pairCode = if (isDoubleMatch) "PAIR-R1-M${i + 1}" else null,
                    stage = "knockout"
                )
                matches.add(match)

                if (isDoubleMatch) {
                    matches.add(
                        TournamentMatch(
                            tournamentId = tournamentId,
                            round = 1,
                            player1Id = p2.first,
                            player2Id = p1.first,
                            player1Name = p2.second,
                            player2Name = p1.second,
                            matchCode = "R1-M${i + 1}-R",
                            isDoubleMatch = true,
                            pairCode = "PAIR-R1-M${i + 1}",
                            stage = "knockout"
                        )
                    )
                }

                preWinnerPlaceholders.add(Pair("WINNER_OF_R1-M${i + 1}", "Ön Eleme Galibi ${i + 1}"))
            }

            // 2. TUR: Ana Eleme Ağacı (P oyuncu -> P/2 maç)
            // BAY geçenler ile Ön Eleme Galiplerini dengeli birleştiriyoruz
            val r2Slots = mutableListOf<Pair<String, String>>()
            val minPairs = minOf(byePlayers.size, preWinnerPlaceholders.size)

            for (i in 0 until minPairs) {
                r2Slots.add(byePlayers[i])
                r2Slots.add(preWinnerPlaceholders[i])
            }
            if (byePlayers.size > minPairs) {
                for (i in minPairs until byePlayers.size) {
                    r2Slots.add(byePlayers[i])
                }
            }
            if (preWinnerPlaceholders.size > minPairs) {
                for (i in minPairs until preWinnerPlaceholders.size) {
                    r2Slots.add(preWinnerPlaceholders[i])
                }
            }

            val r2MatchCount = p / 2
            for (i in 0 until r2MatchCount) {
                val s1 = r2Slots.getOrElse(i * 2) { Pair("BYE", "BAY") }
                val s2 = r2Slots.getOrElse(i * 2 + 1) { Pair("BYE", "BAY") }
                val matchCode = "R2-M${i + 1}"
                val p1Source = if (s1.first.startsWith("WINNER_OF_")) "WINNER:" + s1.first.removePrefix("WINNER_OF_") else null
                val p2Source = if (s2.first.startsWith("WINNER_OF_")) "WINNER:" + s2.first.removePrefix("WINNER_OF_") else null

                val match = TournamentMatch(
                    tournamentId = tournamentId,
                    round = 2,
                    player1Id = s1.first,
                    player2Id = s2.first,
                    player1Name = s1.second,
                    player2Name = s2.second,
                    matchCode = matchCode,
                    isDoubleMatch = isDoubleMatch,
                    pairCode = if (isDoubleMatch) "PAIR-R2-M${i + 1}" else null,
                    player1Source = p1Source,
                    player2Source = p2Source,
                    stage = "knockout"
                )
                matches.add(match)

                if (isDoubleMatch) {
                    matches.add(
                        TournamentMatch(
                            tournamentId = tournamentId,
                            round = 2,
                            player1Id = s2.first,
                            player2Id = s1.first,
                            player1Name = s2.second,
                            player2Name = s1.second,
                            matchCode = "R2-M${i + 1}-R",
                            isDoubleMatch = true,
                            pairCode = "PAIR-R2-M${i + 1}",
                            player1Source = p2Source,
                            player2Source = p1Source,
                            stage = "knockout"
                        )
                    )
                }
            }

            if (allTree && r2MatchCount > 1) {
                // 3. Tur ve devamını türet
                generateSubsequentKnockoutRounds(
                    tournamentId = tournamentId,
                    startRoundSlots = r2MatchCount,
                    isDoubleMatch = isDoubleMatch,
                    matches = matches,
                    startRoundNumber = 3,
                    previousRoundNumber = 2
                )
            }
        }

        return matches
    }

    private fun generateSubsequentKnockoutRounds(
        tournamentId: String,
        startRoundSlots: Int,
        isDoubleMatch: Boolean,
        matches: MutableList<TournamentMatch>,
        startRoundNumber: Int = 2,
        previousRoundNumber: Int = 1
    ) {
        var prevRound = previousRoundNumber
        var currentSlots = startRoundSlots
        var curRound = startRoundNumber

        while (currentSlots > 1) {
            val nextSlots = currentSlots / 2

            // Eğer yarı finalden finale geçiyorsak (currentSlots == 2, nextSlots == 1),
            // Kullanıcı isteği: Finalden önce 3.lük maçını ekle!
            if (currentSlots == 2) {
                val thirdMatchCode = "R$curRound-M-3RD"
                val p1LoserId = "LOSER_OF_R$prevRound-M1"
                val p2LoserId = "LOSER_OF_R$prevRound-M2"
                val p1LoserName = "Yarı Final 1 Mağlubu"
                val p2LoserName = "Yarı Final 2 Mağlubu"

                val thirdMatch = TournamentMatch(
                    tournamentId = tournamentId,
                    round = curRound,
                    player1Id = p1LoserId,
                    player2Id = p2LoserId,
                    player1Name = p1LoserName,
                    player2Name = p2LoserName,
                    matchCode = thirdMatchCode,
                    isDoubleMatch = isDoubleMatch,
                    pairCode = if (isDoubleMatch) "PAIR-$thirdMatchCode" else null,
                    isThirdPlaceMatch = true,
                    player1Source = "LOSER:R$prevRound-M1",
                    player2Source = "LOSER:R$prevRound-M2",
                    stage = "knockout"
                )
                matches.add(thirdMatch)

                if (isDoubleMatch) {
                    matches.add(
                        TournamentMatch(
                            tournamentId = tournamentId,
                            round = curRound,
                            player1Id = p2LoserId,
                            player2Id = p1LoserId,
                            player1Name = p2LoserName,
                            player2Name = p1LoserName,
                            matchCode = "$thirdMatchCode-R",
                            isDoubleMatch = true,
                            pairCode = "PAIR-$thirdMatchCode",
                            isThirdPlaceMatch = true,
                            player1Source = "LOSER:R$prevRound-M2",
                            player2Source = "LOSER:R$prevRound-M1",
                            stage = "knockout"
                        )
                    )
                }
            }

            for (i in 0 until nextSlots) {
                val mCode = "R$curRound-M${i + 1}"
                val p1Id = "WINNER_OF_R$prevRound-M${i * 2 + 1}"
                val p2Id = "WINNER_OF_R$prevRound-M${i * 2 + 2}"
                val p1Name = "R$prevRound Galibi ${i * 2 + 1}"
                val p2Name = "R$prevRound Galibi ${i * 2 + 2}"

                val match = TournamentMatch(
                    tournamentId = tournamentId,
                    round = curRound,
                    player1Id = p1Id,
                    player2Id = p2Id,
                    player1Name = p1Name,
                    player2Name = p2Name,
                    matchCode = mCode,
                    isDoubleMatch = isDoubleMatch,
                    pairCode = if (isDoubleMatch) "PAIR-R$curRound-M${i + 1}" else null,
                    isThirdPlaceMatch = false,
                    player1Source = "WINNER:R$prevRound-M${i * 2 + 1}",
                    player2Source = "WINNER:R$prevRound-M${i * 2 + 2}",
                    stage = "knockout"
                )
                matches.add(match)

                if (isDoubleMatch) {
                    matches.add(
                        TournamentMatch(
                            tournamentId = tournamentId,
                            round = curRound,
                            player1Id = p2Id,
                            player2Id = p1Id,
                            player1Name = p2Name,
                            player2Name = p1Name,
                            matchCode = "$mCode-R",
                            isDoubleMatch = true,
                            pairCode = "PAIR-R$curRound-M${i + 1}",
                            isThirdPlaceMatch = false,
                            player1Source = "WINNER:R$prevRound-M${i * 2 + 2}",
                            player2Source = "WINNER:R$prevRound-M${i * 2 + 1}",
                            stage = "knockout"
                        )
                    )
                }
            }
            prevRound = curRound
            curRound++
            currentSlots = nextSlots
        }
    }

    /**
     * 2. LİG USULÜ (ROUND-ROBIN) VE BERGER ÇEMBER ROTASYON ALGORİTMASI
     * - Katılımcı sayısı tek ise bir adet "BYE" sanal oyuncusu eklenir.
     * - Toplam hafta sayısı: N - 1 (Çift için N, teklik eklenince N).
     * - Her hafta maç sayısı: N / 2.
     * - 1. Oyuncu sabit tutulur, kalan N-1 oyuncu saat yönünde döner.
     * - Çift Maç ise 2. yarıda sahalar ters çevrilerek rövanş haftaları eklenir.
     */
    fun generateRoundRobinFixture(
        tournamentId: String,
        participants: List<Pair<String, String>>, // Pair(id, name)
        isDoubleMatch: Boolean = false,
        groupId: String? = null,
        groupName: String? = null
    ): List<TournamentMatch> {
        val list = participants.toMutableList()
        val isOdd = list.size % 2 != 0
        if (isOdd) {
            list.add(Pair("BYE", "BAY (Boşta)"))
        }

        val totalTeams = list.size
        val roundsCount = totalTeams - 1
        val matchesPerRound = totalTeams / 2
        val matches = mutableListOf<TournamentMatch>()

        val players = list.toMutableList()

        for (round in 1..roundsCount) {
            for (matchIdx in 0 until matchesPerRound) {
                val home = players[matchIdx]
                val away = players[totalTeams - 1 - matchIdx]

                // BYE maçlarını atla
                if (home.first == "BYE" || away.first == "BYE") {
                    continue
                }

                // Ev sahibi / Deplasman dengesi (round tek/çift kontrolü)
                val isSwapped = (matchIdx == 0 && round % 2 == 0)
                val finalHome = if (isSwapped) away else home
                val finalAway = if (isSwapped) home else away

                val code = if (groupId != null) "$groupId-W$round-M${matchIdx + 1}" else "W$round-M${matchIdx + 1}"
                matches.add(
                    TournamentMatch(
                        tournamentId = tournamentId,
                        round = round,
                        player1Id = finalHome.first,
                        player2Id = finalAway.first,
                        player1Name = finalHome.second,
                        player2Name = finalAway.second,
                        matchCode = code,
                        isDoubleMatch = isDoubleMatch,
                        groupId = groupId,
                        groupName = groupName,
                        stage = if (groupId != null) "group" else "league"
                    )
                )
            }

            // Berger Çember Rotasyonu: index 0 sabit kalır, 1..totalTeams-1 saat yönünde 1 adım kayar
            val last = players.removeAt(players.size - 1)
            players.add(1, last)
        }

        // Çift Maç (2. Devre / Rövanş Haftaları)
        if (isDoubleMatch) {
            val firstHalfMatches = matches.toList()
            firstHalfMatches.forEach { m ->
                val secondRound = m.round + roundsCount
                val code = "${m.matchCode}-R"
                matches.add(
                    TournamentMatch(
                        tournamentId = tournamentId,
                        round = secondRound,
                        player1Id = m.player2Id,
                        player2Id = m.player1Id,
                        player1Name = m.player2Name,
                        player2Name = m.player1Name,
                        matchCode = code,
                        isDoubleMatch = true,
                        pairCode = m.id,
                        groupId = groupId,
                        groupName = groupName,
                        stage = m.stage
                    )
                )
            }
        }

        return matches
    }

    /**
     * 3. GRUP VE GRUP + ELEME AŞAMASI MOTORU
     * - Katılımcılar belirtilen grup sayısına (G) dengeli dağıtılır.
     * - Grup İsimlendirmeleri: Harfler (A, B, C...) veya Renkler (Kırmızı, Mavi, Yeşil, Sarı...).
     * - Her grup için Berger rotasyonu ile lig fikstürü üretilir.
     */
    fun generateGroupFixtures(
        tournamentId: String,
        participants: List<Pair<String, String>>,
        groupCount: Int,
        groupNaming: String = "letters", // 'letters' | 'colors'
        isDoubleMatch: Boolean = false
    ): List<TournamentMatch> {
        val count = groupCount.coerceIn(2, 8)
        val shuffled = participants.shuffled()

        val groupLetters = listOf("A", "B", "C", "D", "E", "F", "G", "H")
        val groupColors = listOf("Kırmızı", "Mavi", "Yeşil", "Sarı", "Mor", "Turuncu", "Turkuaz", "Pembe")

        val groups = List(count) { mutableListOf<Pair<String, String>>() }
        shuffled.forEachIndexed { index, participant ->
            groups[index % count].add(participant)
        }

        val allMatches = mutableListOf<TournamentMatch>()
        groups.forEachIndexed { gIdx, groupMembers ->
            val gId = "G${gIdx + 1}"
            val gName = if (groupNaming == "colors") {
                "${groupColors.getOrElse(gIdx) { "Grup ${gIdx + 1}" }} Grubu"
            } else {
                "${groupLetters.getOrElse(gIdx) { "${gIdx + 1}" }} Grubu"
            }

            val groupMatches = generateRoundRobinFixture(
                tournamentId = tournamentId,
                participants = groupMembers,
                isDoubleMatch = isDoubleMatch,
                groupId = gId,
                groupName = gName
            )
            allMatches.addAll(groupMatches)
        }

        return allMatches
    }

    /**
     * Grup Aşaması Tamamlandığında Çapraz Eşleşmeli Eleme Ağacı Üretimi (World Cup Formatı)
     * - Her gruptan çıkan ilk advancingPerGroup kişi sıralanır.
     * - Çapraz eşleştirme: 1. A vs 2. B, 1. B vs 2. A gibi.
     */
    fun generateKnockoutFromGroups(
        tournament: Tournament,
        groupStandingsMap: Map<String, List<TournamentStanding>>,
        isDoubleMatch: Boolean = false
    ): List<TournamentMatch> {
        val advancingPerGroup = tournament.advancingPerGroup.coerceAtLeast(1)
        val qualifiedPlayers = mutableListOf<Pair<String, String>>()

        val sortedGroups = groupStandingsMap.keys.sorted()
        // Gruplardan çıkanları topla
        val groupTopTeams = mutableMapOf<String, List<TournamentStanding>>()
        sortedGroups.forEach { gId ->
            val standings = groupStandingsMap[gId] ?: emptyList()
            groupTopTeams[gId] = standings.take(advancingPerGroup)
        }

        // Çapraz eşleme mantığı (A1 vs B2, B1 vs A2 vb.)
        val knockoutPairs = mutableListOf<Pair<TournamentStanding, TournamentStanding>>()
        val gList = sortedGroups.toList()
        for (i in 0 until gList.size step 2) {
            if (i + 1 < gList.size) {
                val gA = gList[i]
                val gB = gList[i + 1]
                val topA = groupTopTeams[gA] ?: emptyList()
                val topB = groupTopTeams[gB] ?: emptyList()

                if (topA.isNotEmpty() && topB.size > 1) {
                    knockoutPairs.add(Pair(topA[0], topB[1]))
                } else if (topA.isNotEmpty() && topB.isNotEmpty()) {
                    knockoutPairs.add(Pair(topA[0], topB[0]))
                }

                if (topB.isNotEmpty() && topA.size > 1) {
                    knockoutPairs.add(Pair(topB[0], topA[1]))
                }
            } else {
                // Tek kalan grup varsa doğrudan ilk 2 kişiyi eşle
                val gSingle = gList[i]
                val topSingle = groupTopTeams[gSingle] ?: emptyList()
                if (topSingle.size >= 2) {
                    knockoutPairs.add(Pair(topSingle[0], topSingle[1]))
                }
            }
        }

        val matches = mutableListOf<TournamentMatch>()
        knockoutPairs.forEachIndexed { index, (s1, s2) ->
            val matchCode = "KO-R1-M${index + 1}"
            val match = TournamentMatch(
                tournamentId = tournament.id,
                round = 1,
                player1Id = s1.playerId,
                player2Id = s2.playerId,
                player1Name = s1.playerName,
                player2Name = s2.playerName,
                matchCode = matchCode,
                isDoubleMatch = isDoubleMatch,
                pairCode = if (isDoubleMatch) "PAIR-$matchCode" else null,
                stage = "knockout"
            )
            matches.add(match)

            if (isDoubleMatch) {
                matches.add(
                    TournamentMatch(
                        tournamentId = tournament.id,
                        round = 1,
                        player1Id = s2.playerId,
                        player2Id = s1.playerId,
                        player1Name = s2.playerName,
                        player2Name = s1.playerName,
                        matchCode = "$matchCode-R",
                        isDoubleMatch = true,
                        pairCode = "PAIR-$matchCode",
                        stage = "knockout"
                    )
                )
            }
        }

        // Kalan eleme turlarını oluştur
        if (knockoutPairs.size > 1) {
            generateSubsequentKnockoutRounds(
                tournamentId = tournament.id,
                startRoundSlots = knockoutPairs.size,
                isDoubleMatch = isDoubleMatch,
                matches = matches,
                startRoundNumber = 2,
                previousRoundNumber = 1
            )
        }

        return matches
    }

    /**
     * 4. CANLI PUAN TABLOSU VE EŞİTLİK BOZMA MOTORU (STANDINGS ENGINE)
     * - O: Oynanan, G: Galibiyet, B: Beraberlik, M: Mağlubiyet
     * - AG: Atılan Gol/Puan, YG: Yenilen Gol/Puan, AV: Averaj, P: Puan
     * - Eşitlik Bozma Öncelikleri:
     *   1. Puan (Desc)
     *   2. Galibiyet Sayısı (Desc)
     *   3. Genel Averaj (Desc)
     *   4. Atılan Gol (Desc)
     *   5. Alfabetik İsim (Asc)
     */
    fun calculateStandings(
        participants: List<Pair<String, String>>, // Pair(id, name)
        matches: List<TournamentMatch>,
        pointsWin: Int = 3,
        pointsDraw: Int = 1,
        pointsLoss: Int = 0
    ): List<TournamentStanding> {
        val map = mutableMapOf<String, TournamentStanding>()
        participants.forEach { (id, name) ->
            if (id != "BYE") {
                map[id] = TournamentStanding(playerId = id, playerName = name)
            }
        }

        matches.filter { it.status == "completed" }.forEach { match ->
            val p1 = map[match.player1Id]
            val p2 = map[match.player2Id]

            if (p1 != null && p2 != null) {
                val s1 = match.score1 ?: 0
                val s2 = match.score2 ?: 0

                val isWinnerP1 = match.winnerId == p1.playerId || (match.score1 != null && s1 > s2)
                val isWinnerP2 = match.winnerId == p2.playerId || (match.score2 != null && s2 > s1)
                val isDraw = (match.winnerId == null && match.score1 != null && s1 == s2)

                val newP1 = when {
                    isWinnerP1 -> p1.copy(
                        played = p1.played + 1,
                        won = p1.won + 1,
                        goalsFor = p1.goalsFor + s1,
                        goalsAgainst = p1.goalsAgainst + s2,
                        goalDiff = (p1.goalsFor + s1) - (p1.goalsAgainst + s2),
                        points = p1.points + pointsWin
                    )
                    isWinnerP2 -> p1.copy(
                        played = p1.played + 1,
                        lost = p1.lost + 1,
                        goalsFor = p1.goalsFor + s1,
                        goalsAgainst = p1.goalsAgainst + s2,
                        goalDiff = (p1.goalsFor + s1) - (p1.goalsAgainst + s2),
                        points = p1.points + pointsLoss
                    )
                    else -> p1.copy(
                        played = p1.played + 1,
                        drawn = p1.drawn + 1,
                        goalsFor = p1.goalsFor + s1,
                        goalsAgainst = p1.goalsAgainst + s2,
                        goalDiff = (p1.goalsFor + s1) - (p1.goalsAgainst + s2),
                        points = p1.points + pointsDraw
                    )
                }

                val newP2 = when {
                    isWinnerP2 -> p2.copy(
                        played = p2.played + 1,
                        won = p2.won + 1,
                        goalsFor = p2.goalsFor + s2,
                        goalsAgainst = p2.goalsAgainst + s1,
                        goalDiff = (p2.goalsFor + s2) - (p2.goalsAgainst + s1),
                        points = p2.points + pointsWin
                    )
                    isWinnerP1 -> p2.copy(
                        played = p2.played + 1,
                        lost = p2.lost + 1,
                        goalsFor = p2.goalsFor + s2,
                        goalsAgainst = p2.goalsAgainst + s1,
                        goalDiff = (p2.goalsFor + s2) - (p2.goalsAgainst + s1),
                        points = p2.points + pointsLoss
                    )
                    else -> p2.copy(
                        played = p2.played + 1,
                        drawn = p2.drawn + 1,
                        goalsFor = p2.goalsFor + s2,
                        goalsAgainst = p2.goalsAgainst + s1,
                        goalDiff = (p2.goalsFor + s2) - (p2.goalsAgainst + s1),
                        points = p2.points + pointsDraw
                    )
                }

                map[p1.playerId] = newP1
                map[p2.playerId] = newP2
            }
        }

        return map.values.sortedWith(
            compareByDescending<TournamentStanding> { it.points }
                .thenByDescending { it.won }
                .thenByDescending { it.goalDiff }
                .thenByDescending { it.goalsFor }
                .thenBy { it.playerName }
        )
    }

    /**
     * 5. ÇİFT MAÇTA TOPLAM SKOR VE BERABERLİK BOZMA (TIE-BREAKER) TESPİTİ
     * - pairCode altındaki 1. ve 2. Maç completed olduğunda toplam skoru karşılaştırır.
     * - Eşitlik varsa ve henüz karar maçı üretilmemişse 3. Karar Maçı (isTieBreaker = true) üretir.
     */
    fun checkDoubleMatchTieBreaker(
        tournament: Tournament,
        matches: List<TournamentMatch>,
        completedMatch: TournamentMatch
    ): TournamentMatch? {
        val pairCode = completedMatch.pairCode ?: return null
        val legMatches = matches.filter { it.pairCode == pairCode && !it.isTieBreaker }
        if (legMatches.size != 2 || legMatches.any { it.status != "completed" }) {
            return null
        }

        // Halihazırda tie-breaker maçı var mı?
        val existingTieBreaker = matches.find { it.pairCode == pairCode && it.isTieBreaker }
        if (existingTieBreaker != null) return null

        val m1 = legMatches.find { !it.matchCode.endsWith("-R") } ?: legMatches[0]
        val m2 = legMatches.find { it.matchCode.endsWith("-R") } ?: legMatches[1]

        val p1Id = m1.player1Id
        val p2Id = m1.player2Id

        if (tournament.winnerSelectionMethod == WinnerSelectionMethod.SCORE) {
            val m1Score1 = m1.score1 ?: 0
            val m1Score2 = m1.score2 ?: 0
            val m2Score1 = m2.score1 ?: 0 // m2'de player1Id = m1'in player2Id'sidir
            val m2Score2 = m2.score2 ?: 0

            val p1TotalScore = m1Score1 + m2Score2
            val p2TotalScore = m1Score2 + m2Score1

            if (p1TotalScore == p2TotalScore) {
                // Beraberlik bozma maçı türet
                return TournamentMatch(
                    tournamentId = tournament.id,
                    round = m1.round,
                    player1Id = p1Id,
                    player2Id = p2Id,
                    player1Name = m1.player1Name,
                    player2Name = m1.player2Name,
                    matchCode = "$pairCode-T",
                    isDoubleMatch = true,
                    pairCode = pairCode,
                    isTieBreaker = true,
                    isThirdPlaceMatch = m1.isThirdPlaceMatch,
                    stage = m1.stage
                )
            }
        } else {
            // Galip seçimi: 1-1 eşitlik var mı?
            val m1Win = m1.winnerId
            val m2Win = m2.winnerId
            if (m1Win != null && m2Win != null && m1Win != m2Win) {
                // 1. maçı p1, 2. maçı p2 kazandı -> Karar Maçı
                return TournamentMatch(
                    tournamentId = tournament.id,
                    round = m1.round,
                    player1Id = p1Id,
                    player2Id = p2Id,
                    player1Name = m1.player1Name,
                    player2Name = m1.player2Name,
                    matchCode = "$pairCode-T",
                    isDoubleMatch = true,
                    pairCode = pairCode,
                    isTieBreaker = true,
                    isThirdPlaceMatch = m1.isThirdPlaceMatch,
                    stage = m1.stage
                )
            }
        }

        return null
    }

    /**
     * Maç kodunun kök halini temizler (PAIR-, KO-, -R, -T eklerini kaldırır)
     */
    fun getBaseMatchCode(matchCode: String): String {
        return matchCode
            .removePrefix("PAIR-")
            .removePrefix("KO-")
            .removeSuffix("-R")
            .removeSuffix("-T")
    }

    /**
     * Eşleşme Sonucu Modeli (Kesin Galip ve Mağlup Tespiti)
     */
    data class MatchupResult(
        val baseCode: String,
        val isCompleted: Boolean,
        val winnerId: String?,
        val winnerName: String?,
        val loserId: String?,
        val loserName: String?
    )

    /**
     * Tek bir eleme eşleşmesinin (tek maç, çift maç ve karar maçı dahil)
     * kesin galibini ve mağlubunu tespit eder. Kaybeden değil kazananın tur atlamasını garanti eder.
     */
    fun determineMatchupResult(
        baseCode: String,
        allMatches: List<TournamentMatch>,
        selectionMethod: WinnerSelectionMethod
    ): MatchupResult {
        val related = allMatches.filter { getBaseMatchCode(it.matchCode) == baseCode }
        if (related.isEmpty()) {
            return MatchupResult(baseCode, false, null, null, null, null)
        }

        // 1. Karar Maçı (Tie-Breaker) var mı ve oynanmış mı?
        val tieBreaker = related.find { it.isTieBreaker }
        if (tieBreaker != null) {
            if (tieBreaker.status == "completed") {
                val wId = tieBreaker.winnerId ?: when {
                    tieBreaker.score1 != null && tieBreaker.score2 != null && tieBreaker.score1 > tieBreaker.score2 -> tieBreaker.player1Id
                    tieBreaker.score1 != null && tieBreaker.score2 != null && tieBreaker.score2 > tieBreaker.score1 -> tieBreaker.player2Id
                    else -> null
                }
                if (wId != null) {
                    val isP1Winner = (wId == tieBreaker.player1Id)
                    val wName = if (isP1Winner) tieBreaker.player1Name else tieBreaker.player2Name
                    val lId = if (isP1Winner) tieBreaker.player2Id else tieBreaker.player1Id
                    val lName = if (isP1Winner) tieBreaker.player2Name else tieBreaker.player1Name
                    return MatchupResult(baseCode, true, wId, wName, lId, lName)
                }
            }
            return MatchupResult(baseCode, false, null, null, null, null)
        }

        // 2. Çift Maç (Rövanşlı) Karşılaşma
        val isDouble = related.any { it.isDoubleMatch }
        if (isDouble) {
            val m1 = related.find { !it.matchCode.endsWith("-R") }
            val m2 = related.find { it.matchCode.endsWith("-R") }
            if (m1 != null && m2 != null && m1.status == "completed" && m2.status == "completed") {
                if (selectionMethod == WinnerSelectionMethod.SCORE) {
                    val p1Total = (m1.score1 ?: 0) + (m2.score2 ?: 0)
                    val p2Total = (m1.score2 ?: 0) + (m2.score1 ?: 0)
                    return when {
                        p1Total > p2Total -> MatchupResult(baseCode, true, m1.player1Id, m1.player1Name, m1.player2Id, m1.player2Name)
                        p2Total > p1Total -> MatchupResult(baseCode, true, m1.player2Id, m1.player2Name, m1.player1Id, m1.player1Name)
                        else -> MatchupResult(baseCode, false, null, null, null, null) // Eşitlik: Karar maçı bekleniyor
                    }
                } else {
                    val w1 = m1.winnerId
                    val w2 = m2.winnerId
                    return when {
                        w1 != null && w1 == w2 -> {
                            val isP1 = (w1 == m1.player1Id)
                            val wName = if (isP1) m1.player1Name else m1.player2Name
                            val lId = if (isP1) m1.player2Id else m1.player1Id
                            val lName = if (isP1) m1.player2Name else m1.player1Name
                            MatchupResult(baseCode, true, w1, wName, lId, lName)
                        }
                        else -> MatchupResult(baseCode, false, null, null, null, null) // 1-1 eşitlik: Karar maçı bekleniyor
                    }
                }
            }
            return MatchupResult(baseCode, false, null, null, null, null)
        }

        // 3. Tek Maçlı Eleme Karşılaşması
        val m = related.firstOrNull()
        if (m != null && m.status == "completed") {
            if (m.player2Id == "BYE") {
                return MatchupResult(baseCode, true, m.player1Id, m.player1Name, null, null)
            }
            if (m.player1Id == "BYE") {
                return MatchupResult(baseCode, true, m.player2Id, m.player2Name, null, null)
            }
            val wId = m.winnerId ?: when {
                m.score1 != null && m.score2 != null && m.score1 > m.score2 -> m.player1Id
                m.score1 != null && m.score2 != null && m.score2 > m.score1 -> m.player2Id
                else -> null
            }
            if (wId != null) {
                val isP1 = (wId == m.player1Id)
                val wName = if (isP1) m.player1Name else m.player2Name
                val lId = if (isP1) m.player2Id else m.player1Id
                val lName = if (isP1) m.player2Name else m.player1Name
                return MatchupResult(baseCode, true, wId, wName, lId, lName)
            }
        }

        return MatchupResult(baseCode, false, null, null, null, null)
    }

    /**
     * Çift maçlı (Rövanşlı) karşılaşmalarda iki ayağın genel galibini tespit eder.
     */
    fun getDoubleMatchOverallWinner(
        tournament: Tournament,
        legMatches: List<TournamentMatch>
    ): Pair<String, String>? {
        val baseCode = legMatches.firstOrNull()?.let { getBaseMatchCode(it.matchCode) } ?: return null
        val outcome = determineMatchupResult(baseCode, legMatches, tournament.winnerSelectionMethod)
        return if (outcome.isCompleted && outcome.winnerId != null) {
            Pair(outcome.winnerId, outcome.winnerName ?: "")
        } else null
    }

    /**
     * 6. ELEME AĞACINI KESİN VE SENKRONİZE GÜNCELLEME
     * - Tüm turları sırayla tarar.
     * - Karar maçları ve normal maçlar bittiğinde YALNIZCA KAZANANI üst tura ilerletir.
     * - Yarı final mağluplarını ise otomatik olarak 3.lük maçına yerleştirir.
     * - Önceden yanlış yerleşmiş oyuncuları daima güncel galip ile ezerek düzeltir.
     */
    fun syncKnockoutTree(
        matches: List<TournamentMatch>,
        selectionMethod: WinnerSelectionMethod
    ): List<TournamentMatch> {
        val updated = matches.toMutableList()
        val knockoutMatches = updated.filter { it.stage == "knockout" }
        val rounds = knockoutMatches.map { it.round }.distinct().sorted()

        for (r in rounds) {
            val roundMatches = updated.filter { it.stage == "knockout" && it.round == r }
            val baseCodes = roundMatches.map { getBaseMatchCode(it.matchCode) }.distinct()

            for (baseCode in baseCodes) {
                val outcome = determineMatchupResult(baseCode, updated, selectionMethod)
                if (outcome.isCompleted && outcome.winnerId != null) {
                    val winPlaceholder = "WINNER_OF_$baseCode"
                    val losePlaceholder = "LOSER_OF_$baseCode"
                    val winSource = "WINNER:$baseCode"
                    val loseSource = "LOSER:$baseCode"

                    for (i in updated.indices) {
                        val m = updated[i]
                        if (m.stage == "knockout" && m.round > r) {
                            var p1Id = m.player1Id
                            var p1Name = m.player1Name
                            var p2Id = m.player2Id
                            var p2Name = m.player2Name

                            // 1. Oyuncu Güncelleme: Galip veya Mağlup Kaynağı
                            if (m.player1Source == winSource || m.player1Id == winPlaceholder || (m.player1Source == null && m.player1Id.startsWith("WINNER_OF_$baseCode"))) {
                                p1Id = outcome.winnerId
                                p1Name = outcome.winnerName ?: "Galip"
                            } else if (m.player1Source == loseSource || m.player1Id == losePlaceholder || (m.player1Source == null && m.player1Id.startsWith("LOSER_OF_$baseCode"))) {
                                if (outcome.loserId != null) {
                                    p1Id = outcome.loserId
                                    p1Name = outcome.loserName ?: "Mağlup"
                                }
                            }

                            // 2. Oyuncu Güncelleme: Galip veya Mağlup Kaynağı
                            if (m.player2Source == winSource || m.player2Id == winPlaceholder || (m.player2Source == null && m.player2Id.startsWith("WINNER_OF_$baseCode"))) {
                                p2Id = outcome.winnerId
                                p2Name = outcome.winnerName ?: "Galip"
                            } else if (m.player2Source == loseSource || m.player2Id == losePlaceholder || (m.player2Source == null && m.player2Id.startsWith("LOSER_OF_$baseCode"))) {
                                if (outcome.loserId != null) {
                                    p2Id = outcome.loserId
                                    p2Name = outcome.loserName ?: "Mağlup"
                                }
                            }

                            if (p1Id != m.player1Id || p1Name != m.player1Name || p2Id != m.player2Id || p2Name != m.player2Name) {
                                updated[i] = m.copy(
                                    player1Id = p1Id,
                                    player1Name = p1Name,
                                    player2Id = p2Id,
                                    player2Name = p2Name
                                )
                            }
                        }
                    }
                }
            }
        }
        return updated
    }

    /**
     * Eleme ağacında galibi bir sonraki tura ilerletme (syncKnockoutTree'ye yönlendirir)
     */
    fun advanceWinnerToNextMatch(
        matches: List<TournamentMatch>,
        completedMatchCode: String,
        winnerId: String,
        winnerName: String
    ): List<TournamentMatch> {
        val updated = matches.toMutableList()
        val baseCode = getBaseMatchCode(completedMatchCode)
        val placeholder = "WINNER_OF_$baseCode"
        val winSource = "WINNER:$baseCode"

        for (i in updated.indices) {
            val m = updated[i]
            if (m.player1Source == winSource || m.player1Id == placeholder || getBaseMatchCode(m.player1Id) == placeholder) {
                updated[i] = updated[i].copy(player1Id = winnerId, player1Name = winnerName)
            }
            if (m.player2Source == winSource || m.player2Id == placeholder || getBaseMatchCode(m.player2Id) == placeholder) {
                updated[i] = updated[i].copy(player2Id = winnerId, player2Name = winnerName)
            }
        }
        return updated
    }

    /**
     * 7. PODYUM TESPİTİ (1. Şampiyon, 2. İkinci, 3. Üçüncü)
     * - Final Maçı Galibi: 1. Şampiyon
     * - Final Maçı Mağlubu: 2. İkinci
     * - 3.lük Maçı Galibi: 3. Üçüncü (Karar maçları dahil kesin galip)
     */
    fun determinePodium(
        tournament: Tournament,
        matches: List<TournamentMatch>,
        standings: List<TournamentStanding>
    ): Triple<Pair<String, String>?, Pair<String, String>?, Pair<String, String>?> {
        return when (tournament.type) {
            TournamentType.LIG, TournamentType.GRUP -> {
                // Puan durumuna göre ilk 3
                val first = standings.getOrNull(0)?.let { Pair(it.playerId, it.playerName) }
                val second = standings.getOrNull(1)?.let { Pair(it.playerId, it.playerName) }
                val third = standings.getOrNull(2)?.let { Pair(it.playerId, it.playerName) }
                Triple(first, second, third)
            }

            TournamentType.ELEME, TournamentType.GRUP_ELEME -> {
                val knockoutMatches = matches.filter { it.stage == "knockout" }
                val maxRound = knockoutMatches.maxOfOrNull { it.round } ?: 1

                // Final Maçı (isThirdPlaceMatch == false ve maç kodunda "3RD" geçmeyen en üst round'daki maçlar)
                val finalMatches = knockoutMatches.filter { 
                    it.round == maxRound && !it.isThirdPlaceMatch && !it.matchCode.contains("3RD", ignoreCase = true) && it.pairCode?.contains("3RD", ignoreCase = true) != true 
                }
                val finalBaseCode = finalMatches.firstOrNull()?.let { getBaseMatchCode(it.matchCode) } ?: "R$maxRound-M1"
                val finalOutcome = determineMatchupResult(finalBaseCode, knockoutMatches, tournament.winnerSelectionMethod)

                // 3.lük Maçı (isThirdPlaceMatch == true olan veya maç kodunda "3RD" geçen maçlar)
                val thirdPlaceMatches = knockoutMatches.filter { 
                    it.round == maxRound && (it.isThirdPlaceMatch || it.matchCode.contains("3RD", ignoreCase = true) || it.pairCode?.contains("3RD", ignoreCase = true) == true)
                }
                val thirdBaseCode = thirdPlaceMatches.firstOrNull()?.let { getBaseMatchCode(it.matchCode) } ?: "R$maxRound-M-3RD"
                val thirdOutcome = if (thirdPlaceMatches.isNotEmpty()) determineMatchupResult(thirdBaseCode, knockoutMatches, tournament.winnerSelectionMethod) else null

                if (!finalOutcome.isCompleted || finalOutcome.winnerId == null) {
                    Triple(null, null, null)
                } else {
                    // 1. Şampiyon: Final maçı galibi
                    val first = Pair(finalOutcome.winnerId, finalOutcome.winnerName ?: "Şampiyon")
                    // 2. İkinci: Final maçı mağlubu
                    val second = if (finalOutcome.loserId != null) Pair(finalOutcome.loserId, finalOutcome.loserName ?: "İkinci") else null

                    // 3. Üçüncü: 3.lük maçı galibi (3.lük maçını kaybeden kürsüye çıkmaz!)
                    val third = if (thirdOutcome != null && thirdOutcome.isCompleted && thirdOutcome.winnerId != null) {
                        Pair(thirdOutcome.winnerId, thirdOutcome.winnerName ?: "Üçüncü")
                    } else {
                        // 3.lük maçı henüz sonuçlanmamış veya bulunmuyorsa yarı final mağlubu fallback
                        val semiRound = maxRound - 1
                        val semiMatches = knockoutMatches.filter { it.round == semiRound }
                        val semiBaseCodes = semiMatches.map { getBaseMatchCode(it.matchCode) }.distinct()
                        val semiLosers = semiBaseCodes.mapNotNull { bc ->
                            val outcome = determineMatchupResult(bc, knockoutMatches, tournament.winnerSelectionMethod)
                            if (outcome.isCompleted && outcome.loserId != null) Pair(outcome.loserId, outcome.loserName ?: "") else null
                        }.distinctBy { it.first }
                        semiLosers.firstOrNull()
                    }

                    Triple(first, second, third)
                }
            }
        }
    }

    /**
     * Mevcut turda herhangi bir oyun oynandığı (tamamlandığı) anda,
     * önceki turlardaki maçların skor ve sonuç düzenlemesini kilitler.
     */
    fun isMatchLocked(match: TournamentMatch, allMatches: List<TournamentMatch>): Boolean {
        // Kendisinden sonraki bir turda herhangi bir maç tamamlanmışsa bu maç kilitlidir
        return allMatches.any { other ->
            other.id != match.id &&
            other.round > match.round &&
            other.stage == match.stage &&
            (match.groupName == null || other.groupName == match.groupName) &&
            other.status == "completed"
        }
    }
}
