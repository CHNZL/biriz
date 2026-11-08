package com.example.util

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.StarRewardRecord
import com.example.data.model.Tournament
import com.example.data.model.TournamentMatch
import com.example.data.model.TournamentMatchType
import com.example.data.model.TournamentStarRewards
import com.example.data.model.TournamentStatus
import com.example.data.model.TournamentStanding
import com.example.data.model.TournamentType
import com.example.data.model.WinnerSelectionMethod
import com.example.data.repository.StarRewardPersistenceHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Turnuva ve Maç Kalıcı Veri Yöneticisi (Persistence & Firestore Sync)
 * - SharedPreferences (Hızlı, sıfır gecikmeli yerel depolama)
 * - Firestore (Bulut yedekleme ve senkronizasyon)
 * - Otomatik Yıldız Ödülü Dağıtımı ve Turnuva Silindiğinde Güvenli Geri Alma (Rollback)
 */
object TournamentPersistenceHelper {
    private const val TAG = "TournamentPersistence"
    private const val PREFS_NAME = "biriz_tournaments_prefs"
    private const val KEY_TOURNAMENTS_PREFIX = "tournaments_"
    private const val KEY_MATCHES_PREFIX = "matches_"
    private val syncScope = CoroutineScope(Dispatchers.IO)

    private fun getFirestore(context: Context): FirebaseFirestore? {
        return try {
            val dbId = context.getString(R.string.firestore_database_id)
            FirebaseFirestore.getInstance(dbId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Firestore instance", e)
            null
        }
    }

    // -------------------------------------------------------------
    // TURNUVALAR CRUD
    // -------------------------------------------------------------
    fun loadTournaments(context: Context, classId: String): List<Tournament> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("$KEY_TOURNAMENTS_PREFIX$classId", null) ?: return emptyList()

        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<Tournament>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val starObj = obj.optJSONObject("starRewards")
                val starRewards = TournamentStarRewards(
                    enabled = starObj?.optBoolean("enabled", true) ?: true,
                    firstPlaceStars = starObj?.optInt("firstPlaceStars", 10) ?: 10,
                    secondPlaceStars = starObj?.optInt("secondPlaceStars", 6) ?: 6,
                    thirdPlaceStars = starObj?.optInt("thirdPlaceStars", 4) ?: 4,
                    participationStars = starObj?.optInt("participationStars", 1) ?: 1,
                    winBonusStars = starObj?.optInt("winBonusStars", 0) ?: 0,
                    distributed = starObj?.optBoolean("distributed", false) ?: false,
                    distributedAt = if (starObj?.has("distributedAt") == true) starObj.optLong("distributedAt") else null
                )

                val participantsList = mutableListOf<String>()
                val partArray = obj.optJSONArray("participants")
                if (partArray != null) {
                    for (j in 0 until partArray.length()) {
                        participantsList.add(partArray.getString(j))
                    }
                }

                list.add(
                    Tournament(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        classRoomId = obj.optString("classRoomId", classId),
                        name = obj.optString("name", "Turnuva"),
                        description = obj.optString("description", ""),
                        type = try { TournamentType.valueOf(obj.optString("type", "ELEME")) } catch (_: Exception) { TournamentType.ELEME },
                        matchType = try { TournamentMatchType.valueOf(obj.optString("matchType", "TEK_MAC")) } catch (_: Exception) { TournamentMatchType.TEK_MAC },
                        fixtureType = obj.optString("fixtureType", "all"),
                        winnerSelectionMethod = try { WinnerSelectionMethod.valueOf(obj.optString("winnerSelectionMethod", "WINNER")) } catch (_: Exception) { WinnerSelectionMethod.WINNER },
                        currentRound = obj.optInt("currentRound", 1),
                        participants = participantsList,
                        status = try { TournamentStatus.valueOf(obj.optString("status", "DEVAM_EDIYOR")) } catch (_: Exception) { TournamentStatus.DEVAM_EDIYOR },
                        winnerName = obj.optString("winnerName", "").takeIf { it.isNotEmpty() },
                        winnerId = obj.optString("winnerId", "").takeIf { it.isNotEmpty() },
                        runnerUpName = obj.optString("runnerUpName", "").takeIf { it.isNotEmpty() },
                        runnerUpId = obj.optString("runnerUpId", "").takeIf { it.isNotEmpty() },
                        thirdPlaceName = obj.optString("thirdPlaceName", "").takeIf { it.isNotEmpty() },
                        thirdPlaceId = obj.optString("thirdPlaceId", "").takeIf { it.isNotEmpty() },
                        pointsWin = obj.optInt("pointsWin", 3),
                        pointsDraw = obj.optInt("pointsDraw", 1),
                        pointsLoss = obj.optInt("pointsLoss", 0),
                        groupCount = obj.optInt("groupCount", 2),
                        groupNaming = obj.optString("groupNaming", "letters"),
                        advancingPerGroup = obj.optInt("advancingPerGroup", 2),
                        currentStage = obj.optString("currentStage", "group"),
                        starRewards = starRewards,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }

            // Tamamlanmış turnuvalarda podyum derecelerinin doğruluğunu otomatik kontrol et ve onar
            var hasPodiumRepair = false
            for (idx in list.indices) {
                val t = list[idx]
                if (t.status == TournamentStatus.TAMAMLANDI) {
                    val mList = loadMatches(context, t.id)
                    if (mList.isNotEmpty()) {
                        val standings = TournamentEngine.calculateStandings(
                            participants = t.participants.map { Pair(it, it) },
                            matches = mList,
                            pointsWin = t.pointsWin,
                            pointsDraw = t.pointsDraw,
                            pointsLoss = t.pointsLoss
                        )
                        val podium = TournamentEngine.determinePodium(t, mList, standings)
                        if (podium.first != null && (t.winnerId != podium.first?.first || t.runnerUpId != podium.second?.first || t.thirdPlaceId != podium.third?.first)) {
                            val corrected = t.copy(
                                winnerId = podium.first?.first,
                                winnerName = podium.first?.second,
                                runnerUpId = podium.second?.first,
                                runnerUpName = podium.second?.second,
                                thirdPlaceId = podium.third?.first,
                                thirdPlaceName = podium.third?.second,
                                starRewards = t.starRewards.copy(distributed = true)
                            )
                            list[idx] = corrected
                            hasPodiumRepair = true
                            if (corrected.starRewards.enabled) {
                                distributeTournamentStars(context, classId, corrected, forceUpdate = true)
                            }
                        }
                    }
                }
            }
            if (hasPodiumRepair) {
                saveTournaments(context, classId, list)
            }

            list.sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading tournaments", e)
            emptyList()
        }
    }

    fun saveTournaments(context: Context, classId: String, tournaments: List<Tournament>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        tournaments.forEach { t ->
            val starObj = JSONObject().apply {
                put("enabled", t.starRewards.enabled)
                put("firstPlaceStars", t.starRewards.firstPlaceStars)
                put("secondPlaceStars", t.starRewards.secondPlaceStars)
                put("thirdPlaceStars", t.starRewards.thirdPlaceStars)
                put("participationStars", t.starRewards.participationStars)
                put("winBonusStars", t.starRewards.winBonusStars)
                put("distributed", t.starRewards.distributed)
                t.starRewards.distributedAt?.let { put("distributedAt", it) }
            }

            val partArray = JSONArray()
            t.participants.forEach { partArray.put(it) }

            val obj = JSONObject().apply {
                put("id", t.id)
                put("classRoomId", t.classRoomId)
                put("name", t.name)
                put("description", t.description)
                put("type", t.type.name)
                put("matchType", t.matchType.name)
                put("fixtureType", t.fixtureType)
                put("winnerSelectionMethod", t.winnerSelectionMethod.name)
                put("currentRound", t.currentRound)
                put("participants", partArray)
                put("status", t.status.name)
                t.winnerName?.let { put("winnerName", it) }
                t.winnerId?.let { put("winnerId", it) }
                t.runnerUpName?.let { put("runnerUpName", it) }
                t.runnerUpId?.let { put("runnerUpId", it) }
                t.thirdPlaceName?.let { put("thirdPlaceName", it) }
                t.thirdPlaceId?.let { put("thirdPlaceId", it) }
                put("pointsWin", t.pointsWin)
                put("pointsDraw", t.pointsDraw)
                put("pointsLoss", t.pointsLoss)
                put("groupCount", t.groupCount)
                put("groupNaming", t.groupNaming)
                put("advancingPerGroup", t.advancingPerGroup)
                put("currentStage", t.currentStage)
                put("starRewards", starObj)
                put("createdAt", t.createdAt)
                put("updatedAt", t.updatedAt)
            }
            array.put(obj)
        }
        prefs.edit().putString("$KEY_TOURNAMENTS_PREFIX$classId", array.toString()).apply()

        syncScope.launch {
            uploadTournamentsToFirestore(context, classId, tournaments)
        }
    }

    // -------------------------------------------------------------
    // MAÇLAR CRUD
    // -------------------------------------------------------------
    fun loadMatches(context: Context, tournamentId: String): List<TournamentMatch> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("$KEY_MATCHES_PREFIX$tournamentId", null) ?: return emptyList()

        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<TournamentMatch>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    TournamentMatch(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        tournamentId = obj.optString("tournamentId", tournamentId),
                        round = obj.optInt("round", 1),
                        player1Id = obj.optString("player1Id", ""),
                        player2Id = obj.optString("player2Id", ""),
                        player1Name = obj.optString("player1Name", ""),
                        player2Name = obj.optString("player2Name", ""),
                        winnerId = obj.optString("winnerId", "").takeIf { it.isNotEmpty() },
                        score1 = if (obj.has("score1")) obj.optInt("score1") else null,
                        score2 = if (obj.has("score2")) obj.optInt("score2") else null,
                        status = obj.optString("status", "pending"),
                        matchCode = obj.optString("matchCode", ""),
                        isDoubleMatch = obj.optBoolean("isDoubleMatch", false),
                        pairCode = obj.optString("pairCode", "").takeIf { it.isNotEmpty() },
                        isTieBreaker = obj.optBoolean("isTieBreaker", false),
                        isThirdPlaceMatch = obj.optBoolean("isThirdPlaceMatch", false),
                        player1Source = obj.optString("player1Source", "").takeIf { it.isNotEmpty() },
                        player2Source = obj.optString("player2Source", "").takeIf { it.isNotEmpty() },
                        groupId = obj.optString("groupId", "").takeIf { it.isNotEmpty() },
                        groupName = obj.optString("groupName", "").takeIf { it.isNotEmpty() },
                        stage = obj.optString("stage", "knockout"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            list.sortedWith(compareBy({ it.round }, { it.matchCode }))
        } catch (e: Exception) {
            Log.e(TAG, "Error loading matches", e)
            emptyList()
        }
    }

    fun saveMatches(context: Context, tournamentId: String, matches: List<TournamentMatch>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        matches.forEach { m ->
            val obj = JSONObject().apply {
                put("id", m.id)
                put("tournamentId", m.tournamentId)
                put("round", m.round)
                put("player1Id", m.player1Id)
                put("player2Id", m.player2Id)
                put("player1Name", m.player1Name)
                put("player2Name", m.player2Name)
                m.winnerId?.let { put("winnerId", it) }
                m.score1?.let { put("score1", it) }
                m.score2?.let { put("score2", it) }
                put("status", m.status)
                put("matchCode", m.matchCode)
                put("isDoubleMatch", m.isDoubleMatch)
                m.pairCode?.let { put("pairCode", it) }
                put("isTieBreaker", m.isTieBreaker)
                put("isThirdPlaceMatch", m.isThirdPlaceMatch)
                m.player1Source?.let { put("player1Source", it) }
                m.player2Source?.let { put("player2Source", it) }
                m.groupId?.let { put("groupId", it) }
                m.groupName?.let { put("groupName", it) }
                put("stage", m.stage)
                put("createdAt", m.createdAt)
            }
            array.put(obj)
        }
        prefs.edit().putString("$KEY_MATCHES_PREFIX$tournamentId", array.toString()).apply()

        syncScope.launch {
            uploadMatchesToFirestore(context, tournamentId, matches)
        }
    }

    // -------------------------------------------------------------
    // YENİ TURNUVA VE FİKSTÜR OLUŞTURMA
    // -------------------------------------------------------------
    fun createTournament(
        context: Context,
        classRoomId: String,
        tournament: Tournament,
        participantsPairs: List<Pair<String, String>> // Pair(id, name)
    ): Tournament {
        val currentTournaments = loadTournaments(context, classRoomId).toMutableList()
        currentTournaments.add(0, tournament)
        saveTournaments(context, classRoomId, currentTournaments)

        val isDouble = tournament.matchType == TournamentMatchType.CIFT_MAC

        // Fikstürü türet
        val generatedMatches = when (tournament.type) {
            TournamentType.ELEME -> {
                TournamentEngine.generateKnockoutFixture(
                    tournamentId = tournament.id,
                    participants = participantsPairs,
                    isDoubleMatch = isDouble,
                    allTree = tournament.fixtureType == "all"
                )
            }
            TournamentType.LIG -> {
                TournamentEngine.generateRoundRobinFixture(
                    tournamentId = tournament.id,
                    participants = participantsPairs,
                    isDoubleMatch = isDouble
                )
            }
            TournamentType.GRUP -> {
                TournamentEngine.generateGroupFixtures(
                    tournamentId = tournament.id,
                    participants = participantsPairs,
                    groupCount = tournament.groupCount,
                    groupNaming = tournament.groupNaming,
                    isDoubleMatch = isDouble
                )
            }
            TournamentType.GRUP_ELEME -> {
                TournamentEngine.generateGroupFixtures(
                    tournamentId = tournament.id,
                    participants = participantsPairs,
                    groupCount = tournament.groupCount,
                    groupNaming = tournament.groupNaming,
                    isDoubleMatch = isDouble
                )
            }
        }

        saveMatches(context, tournament.id, generatedMatches)
        return tournament
    }

    // -------------------------------------------------------------
    // MAÇ SONUCU KAYDI VE AĞAÇ İLERLEMESİ
    // -------------------------------------------------------------
    fun recordMatchResult(
        context: Context,
        classRoomId: String,
        tournamentId: String,
        matchId: String,
        score1: Int?,
        score2: Int?,
        winnerId: String?
    ): Pair<Tournament, List<TournamentMatch>> {
        val tournaments = loadTournaments(context, classRoomId).toMutableList()
        val tIdx = tournaments.indexOfFirst { it.id == tournamentId }
        val tournament = if (tIdx != -1) tournaments[tIdx] else return Pair(Tournament(classRoomId = classRoomId, name = ""), emptyList())

        val currentMatches = loadMatches(context, tournamentId).toMutableList()
        val mIdx = currentMatches.indexOfFirst { it.id == matchId }
        if (mIdx == -1) return Pair(tournament, currentMatches)

        val match = currentMatches[mIdx]
        val updatedMatch = match.copy(
            score1 = score1,
            score2 = score2,
            winnerId = winnerId,
            status = "completed"
        )
        currentMatches[mIdx] = updatedMatch

        var refreshedMatches = currentMatches.toList()
        val isKnockoutStage = tournament.type == TournamentType.ELEME || (tournament.type == TournamentType.GRUP_ELEME && match.stage == "knockout")

        if (isKnockoutStage) {
            val baseCode = TournamentEngine.getBaseMatchCode(match.matchCode)

            if (match.isDoubleMatch) {
                // Çift maçlı (Rövanşlı) karşılaşmada beraberlik bozma maçı kontrolü
                val tieBreaker = TournamentEngine.checkDoubleMatchTieBreaker(tournament, refreshedMatches, updatedMatch)
                if (tieBreaker != null) {
                    refreshedMatches = refreshedMatches + tieBreaker
                }
            }

            // Eleme ağacını kesin ve hatasız senkronize et:
            // Kaybeden değil daima kazanan tur atlar, 3.lük maçına ise yarı final mağlupları yerleşir.
            refreshedMatches = TournamentEngine.syncKnockoutTree(
                matches = refreshedMatches,
                selectionMethod = tournament.winnerSelectionMethod
            )
        }

        saveMatches(context, tournamentId, refreshedMatches)

        // 3. Turnuva Tamamlandı mı?
        val allFinished = refreshedMatches.isNotEmpty() && refreshedMatches.all { it.status == "completed" }
        var updatedTournament = tournament.copy(updatedAt = System.currentTimeMillis())

        if (allFinished && tournament.status != TournamentStatus.TAMAMLANDI) {
            val standings = TournamentEngine.calculateStandings(
                participants = tournament.participants.map { Pair(it, it) },
                matches = refreshedMatches,
                pointsWin = tournament.pointsWin,
                pointsDraw = tournament.pointsDraw,
                pointsLoss = tournament.pointsLoss
            )
            val podium = TournamentEngine.determinePodium(tournament, refreshedMatches, standings)

            updatedTournament = updatedTournament.copy(
                status = TournamentStatus.TAMAMLANDI,
                winnerId = podium.first?.first,
                winnerName = podium.first?.second,
                runnerUpId = podium.second?.first,
                runnerUpName = podium.second?.second,
                thirdPlaceId = podium.third?.first,
                thirdPlaceName = podium.third?.second,
                starRewards = updatedTournament.starRewards.copy(
                    distributed = true,
                    distributedAt = System.currentTimeMillis()
                )
            )

            // Yıldızları Dağıt
            distributeTournamentStars(context, classRoomId, updatedTournament, forceUpdate = true)
        }

        tournaments[tIdx] = updatedTournament
        saveTournaments(context, classRoomId, tournaments)

        return Pair(updatedTournament, refreshedMatches)
    }

    /**
     * Grup + Eleme formatında, grup maçları tamamlandıktan sonra
     * lider ve ikincilerden eleme (play-off) ağacı maçlarını türetir ve kaydeder.
     */
    fun generateAndSaveKnockoutFromGroups(
        context: Context,
        classRoomId: String,
        tournamentId: String
    ): List<TournamentMatch> {
        val tournaments = loadTournaments(context, classRoomId)
        val tournament = tournaments.find { it.id == tournamentId } ?: return emptyList()
        val currentMatches = loadMatches(context, tournamentId)

        // Zaten knockout maçları varsa tekrar üretme
        val existingKnockouts = currentMatches.filter { it.stage == "knockout" }
        if (existingKnockouts.isNotEmpty()) {
            return currentMatches
        }

        // Grup maçlarını grupla
        val groupMatchesMap = currentMatches.filter { it.stage != "knockout" }
            .groupBy { it.groupId ?: it.groupName ?: "G1" }
        val groupStandingsMap = mutableMapOf<String, List<TournamentStanding>>()

        groupMatchesMap.forEach { (gId, gMatches) ->
            val groupParticipants = gMatches.flatMap {
                listOf(Pair(it.player1Id, it.player1Name), Pair(it.player2Id, it.player2Name))
            }.filter { it.first.isNotBlank() && it.first != "BYE" }.distinctBy { it.first }

            val standings = TournamentEngine.calculateStandings(
                participants = groupParticipants,
                matches = gMatches,
                pointsWin = tournament.pointsWin,
                pointsDraw = tournament.pointsDraw,
                pointsLoss = tournament.pointsLoss
            )
            groupStandingsMap[gId] = standings
        }

        val isDouble = tournament.matchType == TournamentMatchType.CIFT_MAC
        val knockoutMatches = TournamentEngine.generateKnockoutFromGroups(
            tournament = tournament,
            groupStandingsMap = groupStandingsMap,
            isDoubleMatch = isDouble
        )

        val updatedMatches = currentMatches + knockoutMatches
        saveMatches(context, tournamentId, updatedMatches)
        return updatedMatches
    }

    // -------------------------------------------------------------
    // YILDIZ ÖDÜLLERİNİ DAĞITMA & SİLME GERİ ALMA (ROLLBACK)
    // -------------------------------------------------------------
    fun distributeTournamentStars(
        context: Context,
        classRoomId: String,
        tournament: Tournament,
        forceUpdate: Boolean = false
    ) {
        if (!tournament.starRewards.enabled) return
        if (tournament.starRewards.distributed && !forceUpdate) return

        if (forceUpdate) {
            // Önceki ödül kayıtlarını temizle (derece güncellenirken mükerrer yıldız olmaması için)
            val existing = StarRewardPersistenceHelper.loadStarRecords(context, classRoomId).toMutableList()
            val prefix = "Turnuva Ödülü: [${tournament.name}]"
            val participationPrefix = "Turnuva Katılım Ödülü: [${tournament.name}]"
            val countBefore = existing.size
            existing.removeAll { it.note.startsWith(prefix) || it.note.startsWith(participationPrefix) }
            if (existing.size != countBefore) {
                StarRewardPersistenceHelper.saveStarRecords(context, classRoomId, existing)
            }
        }

        val rewards = tournament.starRewards
        val starRecords = mutableListOf<StarRewardRecord>()
        val now = System.currentTimeMillis()

        // 1. Şampiyon Yıldızı
        tournament.winnerId?.let { wId ->
            if (rewards.firstPlaceStars > 0) {
                starRecords.add(
                    StarRewardRecord(
                        id = UUID.randomUUID().toString(),
                        classId = classRoomId,
                        studentId = wId,
                        studentName = tournament.winnerName ?: "Şampiyon",
                        studentNumber = "",
                        categoryId = "beden_egitimi",
                        categoryName = "Turnuva Şampiyonu 🏆",
                        starCount = rewards.firstPlaceStars,
                        note = "Turnuva Ödülü: [${tournament.name}] Şampiyonluğu 🥇 (+${rewards.firstPlaceStars}⭐)",
                        timestamp = now
                    )
                )
            }
        }

        // 2. İkinci Yıldızı
        tournament.runnerUpId?.let { rId ->
            if (rewards.secondPlaceStars > 0) {
                starRecords.add(
                    StarRewardRecord(
                        id = UUID.randomUUID().toString(),
                        classId = classRoomId,
                        studentId = rId,
                        studentName = tournament.runnerUpName ?: "2. Oyuncu",
                        studentNumber = "",
                        categoryId = "beden_egitimi",
                        categoryName = "Turnuva İkincisi 🥈",
                        starCount = rewards.secondPlaceStars,
                        note = "Turnuva Ödülü: [${tournament.name}] İkinciliği 🥈 (+${rewards.secondPlaceStars}⭐)",
                        timestamp = now
                    )
                )
            }
        }

        // 3. Üçüncü Yıldızı
        tournament.thirdPlaceId?.let { thId ->
            if (rewards.thirdPlaceStars > 0) {
                starRecords.add(
                    StarRewardRecord(
                        id = UUID.randomUUID().toString(),
                        classId = classRoomId,
                        studentId = thId,
                        studentName = tournament.thirdPlaceName ?: "3. Oyuncu",
                        studentNumber = "",
                        categoryId = "beden_egitimi",
                        categoryName = "Turnuva Üçüncüsü 🥉",
                        starCount = rewards.thirdPlaceStars,
                        note = "Turnuva Ödülü: [${tournament.name}] Üçüncülüğü 🥉 (+${rewards.thirdPlaceStars}⭐)",
                        timestamp = now
                    )
                )
            }
        }

        // 4. Katılım Yıldızı (Tüm katılımcılara)
        if (rewards.participationStars > 0) {
            val podiumIds = setOfNotNull(tournament.winnerId, tournament.runnerUpId, tournament.thirdPlaceId)
            tournament.participants.forEach { partId ->
                if (!partId.startsWith("extra:") && !podiumIds.contains(partId)) {
                    starRecords.add(
                        StarRewardRecord(
                            id = UUID.randomUUID().toString(),
                            classId = classRoomId,
                            studentId = partId,
                            studentName = "Öğrenci",
                            studentNumber = "",
                            categoryId = "beden_egitimi",
                            categoryName = "Turnuva Katılımı 🎖️",
                            starCount = rewards.participationStars,
                            note = "Turnuva Katılım Ödülü: [${tournament.name}] (+${rewards.participationStars}⭐)",
                            timestamp = now
                        )
                    )
                }
            }
        }

        if (starRecords.isNotEmpty()) {
            StarRewardPersistenceHelper.addStarRecords(context, classRoomId, starRecords)
        }
    }

    /**
     * Turnuva Silme ve Güvenli Geri Alma (Rollback)
     * Turnuva silinirse o turnuvadan dağıtılan tüm yıldızlar hatasız silinir.
     */
    fun deleteTournament(context: Context, classRoomId: String, tournamentId: String) {
        val tournaments = loadTournaments(context, classRoomId).toMutableList()
        val tournament = tournaments.find { it.id == tournamentId }

        tournaments.removeAll { it.id == tournamentId }
        saveTournaments(context, classRoomId, tournaments)

        // Maçları temizle
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove("$KEY_MATCHES_PREFIX$tournamentId").apply()

        // Yıldızları Geri Al (Rollback)
        if (tournament != null) {
            val starRecords = StarRewardPersistenceHelper.loadStarRecords(context, classRoomId).toMutableList()
            val prefix = "Turnuva Ödülü: [${tournament.name}]"
            val participationPrefix = "Turnuva Katılım Ödülü: [${tournament.name}]"

            val countBefore = starRecords.size
            starRecords.removeAll { it.note.startsWith(prefix) || it.note.startsWith(participationPrefix) }

            if (starRecords.size != countBefore) {
                StarRewardPersistenceHelper.saveStarRecords(context, classRoomId, starRecords)
                Log.d(TAG, "Turnuva silindi ve dağıtılan yıldızlar geri çekildi.")
            }
        }
    }

    // -------------------------------------------------------------
    // FIRESTORE SYNC
    // -------------------------------------------------------------
    private fun uploadTournamentsToFirestore(context: Context, classId: String, tournaments: List<Tournament>) {
        if (Firebase.auth.currentUser == null) return
        val db = getFirestore(context) ?: return
        val batch = db.batch()
        tournaments.forEach { t ->
            val doc = db.collection("classrooms").document(classId).collection("tournaments").document(t.id)
            batch.set(
                doc,
                mapOf(
                    "name" to t.name,
                    "type" to t.type.name,
                    "status" to t.status.name,
                    "winnerName" to (t.winnerName ?: ""),
                    "participantCount" to t.participants.size,
                    "updatedAt" to System.currentTimeMillis()
                )
            )
        }
        batch.commit()
    }

    private fun uploadMatchesToFirestore(context: Context, tournamentId: String, matches: List<TournamentMatch>) {
        if (Firebase.auth.currentUser == null) return
        val db = getFirestore(context) ?: return
        val batch = db.batch()
        matches.take(50).forEach { m ->
            val doc = db.collection("tournaments").document(tournamentId).collection("matches").document(m.id)
            batch.set(
                doc,
                mapOf(
                    "round" to m.round,
                    "player1Name" to m.player1Name,
                    "player2Name" to m.player2Name,
                    "status" to m.status,
                    "score1" to (m.score1 ?: -1),
                    "score2" to (m.score2 ?: -1),
                    "winnerId" to (m.winnerId ?: "")
                )
            )
        }
        batch.commit()
    }
}
