package com.example.util

import com.example.data.model.Announcement
import com.example.data.model.AnnouncementTargetType
import com.example.data.model.BirizNotification
import com.example.data.model.Book
import com.example.data.model.NotificationType
import com.example.data.model.ReadingRecord
import com.example.data.model.StarRewardRecord
import com.example.data.model.Student
import com.example.data.model.Tournament
import com.example.data.model.TournamentMatch
import com.example.data.model.TournamentStatus
import com.example.data.model.TournamentType

/**
 * Duyuru, yıldız, kitaplık (ödünç / iade) ve turnuva (katılım / maç / tur atlama / derece-sıra sonucu)
 * olaylarından üst menü bildirim zilinde gösterilecek bildirimleri deterministik ve mükerrersiz üreten katman.
 */
object ActivityNotificationHelper {

    data class ActivitySnapshot(
        val announcements: List<Announcement> = emptyList(),
        val starRecords: List<StarRewardRecord> = emptyList(),
        val books: List<Book> = emptyList(),
        val readingRecords: List<ReadingRecord> = emptyList(),
        val tournaments: List<Tournament> = emptyList(),
        val matchesByTournament: Map<String, List<TournamentMatch>> = emptyMap(),
        val students: List<Student> = emptyList(),
        val hasAnnouncements: Boolean = true,
        val hasStars: Boolean = true,
        val hasBooks: Boolean = true,
        val hasReadingRecords: Boolean = true,
        val hasTournaments: Boolean = true,
        val loadedMatchTournamentIds: Set<String> = matchesByTournament.keys
    )

    data class EvaluationResult(
        val newNotifications: List<BirizNotification>,
        val updatedSeenKeys: Set<String>
    )

    private fun resolveStudentName(studentId: String, students: List<Student>, fallback: String = "Öğrenciniz"): String {
        return students.firstOrNull { it.id == studentId }?.fullName?.takeIf { it.isNotBlank() }
            ?: fallback.takeIf { it.isNotBlank() }
            ?: "Öğrenciniz"
    }

    fun knockoutRoundLabel(round: Int, maxKnockoutRound: Int): String {
        val effectiveMax = maxOf(round, maxKnockoutRound)
        return when (effectiveMax - round) {
            0 -> "Final"
            1 -> "Yarı Final"
            2 -> "Çeyrek Final"
            else -> "$round. Tur"
        }
    }

    fun computeStudentTournamentPlacement(
        tournament: Tournament,
        matches: List<TournamentMatch>,
        studentId: String,
        students: List<Student> = emptyList()
    ): String {
        if (tournament.winnerId == studentId) return "1. sırada (Şampiyon 🥇)"
        if (tournament.runnerUpId == studentId) return "2. sırada (İkinci / Finalist 🥈)"
        if (tournament.thirdPlaceId == studentId) return "3. sırada (Üçüncü 🥉)"

        val participantPairs = tournament.participants.map { pId ->
            pId to resolveStudentName(pId, students, fallback = pId.removePrefix("extra:"))
        }

        if (tournament.type == TournamentType.LIG || tournament.type == TournamentType.GRUP) {
            val overallStandings = TournamentEngine.calculateStandings(
                participants = participantPairs,
                matches = matches,
                pointsWin = tournament.pointsWin,
                pointsDraw = tournament.pointsDraw,
                pointsLoss = tournament.pointsLoss
            )
            val overallIdx = overallStandings.indexOfFirst { it.playerId == studentId }
            val overallRank = if (overallIdx >= 0) overallIdx + 1 else null

            if (tournament.type == TournamentType.GRUP) {
                val studentGroupMatch = matches.firstOrNull {
                    !it.groupName.isNullOrBlank() && (it.player1Id == studentId || it.player2Id == studentId)
                }
                val groupName = studentGroupMatch?.groupName
                if (groupName != null) {
                    val gMatches = matches.filter { it.groupName == groupName }
                    val gParticipants = gMatches.flatMap {
                        listOf(it.player1Id to it.player1Name, it.player2Id to it.player2Name)
                    }.filter { it.first.isNotBlank() && it.first != "BYE" }.distinctBy { it.first }
                    val gStandings = TournamentEngine.calculateStandings(
                        participants = gParticipants,
                        matches = gMatches,
                        pointsWin = tournament.pointsWin,
                        pointsDraw = tournament.pointsDraw,
                        pointsLoss = tournament.pointsLoss
                    )
                    val gIdx = gStandings.indexOfFirst { it.playerId == studentId }
                    if (gIdx >= 0) {
                        val pts = gStandings[gIdx].points
                        val overallSuffix = if (overallRank != null) " • Genel $overallRank." else ""
                        return "$groupName ${gIdx + 1}.si ($pts Puan$overallSuffix)"
                    }
                }
            }

            if (overallRank != null) {
                val pts = overallStandings[overallIdx].points
                return "$overallRank. sırada ($pts Puan)"
            }
            return "Tamamlandı"
        }

        // ELEME veya GRUP_ELEME için podyum dışı kesin sıra & tur unvanı
        val thirdPlaceLoserId = matches.firstOrNull {
            (it.isThirdPlaceMatch || it.matchCode.contains("3RD", ignoreCase = true)) &&
                it.status == "completed" &&
                it.winnerId != null &&
                (it.player1Id == studentId || it.player2Id == studentId) &&
                it.winnerId != studentId
        }?.let { studentId }
        if (thirdPlaceLoserId != null) {
            return "4. sırada (Yarı Finalist)"
        }

        val knockoutMatches = matches.filter { it.stage == "knockout" }
        val maxKnockoutRound = knockoutMatches.maxOfOrNull { it.round } ?: 1
        val reachedRoundFor = { pId: String ->
            knockoutMatches
                .filter { !it.isThirdPlaceMatch && (it.player1Id == pId || it.player2Id == pId) }
                .maxOfOrNull { it.round } ?: 0
        }

        val overallStandings = TournamentEngine.calculateStandings(
            participants = participantPairs,
            matches = matches,
            pointsWin = tournament.pointsWin,
            pointsDraw = tournament.pointsDraw,
            pointsLoss = tournament.pointsLoss
        )
        val standingMap = overallStandings.associateBy { it.playerId }

        val thirdMatchLoserIds = matches.filter {
            (it.isThirdPlaceMatch || it.matchCode.contains("3RD", ignoreCase = true)) &&
                it.status == "completed" &&
                it.winnerId != null
        }.mapNotNull { m ->
            when (m.winnerId) {
                m.player1Id -> m.player2Id
                m.player2Id -> m.player1Id
                else -> null
            }
        }.toSet()

        val sortedParticipants = tournament.participants.sortedWith(
            compareBy<String>(
                { pId ->
                    when {
                        tournament.winnerId == pId -> 1
                        tournament.runnerUpId == pId -> 2
                        tournament.thirdPlaceId == pId -> 3
                        pId in thirdMatchLoserIds -> 4
                        else -> 5
                    }
                },
                { pId -> -reachedRoundFor(pId) },
                { pId -> -(standingMap[pId]?.won ?: 0) },
                { pId -> -(standingMap[pId]?.points ?: 0) },
                { pId -> -(standingMap[pId]?.goalDiff ?: 0) },
                { pId -> -(standingMap[pId]?.goalsFor ?: 0) },
                { pId -> resolveStudentName(pId, students) }
            )
        )

        val rankIndex = sortedParticipants.indexOf(studentId)
        val ordinalText = if (rankIndex >= 0) "${rankIndex + 1}. sırada" else "Tamamlandı"
        val studentReachedRound = reachedRoundFor(studentId)

        val stageTitle = when {
            studentReachedRound <= 0 && tournament.type == TournamentType.GRUP_ELEME -> "Grup Aşaması"
            studentReachedRound <= 0 -> "Katılımcı"
            maxKnockoutRound - studentReachedRound == 1 -> "Yarı Finalist"
            maxKnockoutRound - studentReachedRound == 2 -> "Çeyrek Finalist"
            else -> "$studentReachedRound. Tur"
        }

        return if (rankIndex >= 0) "$stageTitle ($ordinalText)" else stageTitle
    }

    /**
     * Veli rolündeki kullanıcının takip ettiği çocuklar ve sınıf için tüm yeni etkinlik bildirimlerini üretir.
     */
    fun evaluateParentNotifications(
        previous: ActivitySnapshot?,
        current: ActivitySnapshot,
        linkedStudentIds: Set<String>,
        seenEventKeys: Set<String>,
        baselineTimestampMs: Long,
        nowMs: Long = System.currentTimeMillis()
    ): EvaluationResult {
        if (linkedStudentIds.isEmpty()) {
            return EvaluationResult(emptyList(), seenEventKeys)
        }

        val updatedSeen = seenEventKeys.toMutableSet()
        val generated = mutableListOf<BirizNotification>()

        fun processCandidate(
            eventKey: String,
            eventTimestamp: Long,
            isLiveTransition: Boolean,
            buildNotification: () -> BirizNotification
        ) {
            if (eventKey in updatedSeen) return
            updatedSeen.add(eventKey)
            val isAfterBaseline = eventTimestamp <= 0L || eventTimestamp >= baselineTimestampMs
            if (isLiveTransition || isAfterBaseline) {
                generated.add(buildNotification())
            }
        }

        // 1. Yeni / Güncellenen Duyurular
        val canDiffAnnouncements = previous != null && previous.hasAnnouncements
        val prevAnnIds = previous?.announcements?.associateBy { it.id }.orEmpty()
        current.announcements.forEach { ann ->
            val targetsViewer = ann.targetType == AnnouncementTargetType.ALL_CLASS ||
                ann.targetStudentIds.isEmpty() ||
                ann.targetStudentIds.any { it in linkedStudentIds }
            if (!targetsViewer) return@forEach

            val eventKey = "ann_${ann.id}_${ann.updatedAt}"
            val prevAnn = prevAnnIds[ann.id]
            val isLiveTransition = canDiffAnnouncements && (prevAnn == null || prevAnn.updatedAt < ann.updatedAt)
            val ts = maxOf(ann.updatedAt, ann.createdAt)
            processCandidate(eventKey, ts, isLiveTransition) {
                BirizNotification(
                    id = "notif_$eventKey",
                    title = if (ann.isEdited) "Duyuru Güncellendi 📢" else "Yeni Sınıf Duyurusu 📢",
                    message = "${ann.title}: ${ann.content.take(90)}",
                    timestamp = ts.takeIf { it > 0L } ?: nowMs,
                    type = NotificationType.ANNOUNCEMENT,
                    targetSubPageName = "COMMUNICATION",
                    targetId = ann.id,
                    isRead = false
                )
            }
        }

        // 2. Yeni Yıldız Ödülleri
        val canDiffStars = previous != null && previous.hasStars
        val prevStarIds = previous?.starRecords?.map { it.id }?.toSet().orEmpty()
        current.starRecords.forEach { sr ->
            if (sr.studentId !in linkedStudentIds || sr.starCount <= 0) return@forEach
            val eventKey = "star_${sr.id}"
            val isLiveTransition = canDiffStars && sr.id !in prevStarIds
            val sName = resolveStudentName(sr.studentId, current.students, sr.studentName)
            processCandidate(eventKey, sr.timestamp, isLiveTransition) {
                BirizNotification(
                    id = "notif_$eventKey",
                    title = "⭐ $sName +${sr.starCount} Yıldız Kazandı!",
                    message = if (sr.note.isNotBlank()) "${sr.categoryName} • ${sr.note}" else sr.categoryName,
                    timestamp = sr.timestamp.takeIf { it > 0L } ?: nowMs,
                    type = NotificationType.STAR_REWARD,
                    targetSubPageName = "STARS",
                    targetId = sr.studentId,
                    isRead = false
                )
            }
        }

        // 3 & 4. Kitaplık Olayları (Yalnızca ödünç alma ve iade; yeni kitap ekleme bildirim üretmez)
        val canDiffBooks = previous != null && previous.hasBooks
        val prevBooksById = previous?.books?.associateBy { it.id }.orEmpty()
        current.books.forEach { book ->
            // Öğrenci Kitap Ödünç Aldığında
            val currentAssignments = if (book.activeAssignments.isNotEmpty()) {
                book.activeAssignments.map { it.studentId to it.assignmentDate }
            } else if (!book.currentStudentId.isNullOrBlank()) {
                listOf(book.currentStudentId to (book.assignmentDate ?: book.updatedAt))
            } else emptyList()

            val prevBook = prevBooksById[book.id]
            val prevAssignmentKeys = if (prevBook != null) {
                val rawPrev = if (prevBook.activeAssignments.isNotEmpty()) {
                    prevBook.activeAssignments.map { it.studentId to it.assignmentDate }
                } else if (!prevBook.currentStudentId.isNullOrBlank()) {
                    listOf(prevBook.currentStudentId to (prevBook.assignmentDate ?: prevBook.updatedAt))
                } else emptyList()
                rawPrev.map { (sId, dt) -> "${sId}_$dt" }.toSet()
            } else emptySet()

            currentAssignments.forEach { (sId, assignDate) ->
                if (sId !in linkedStudentIds) return@forEach
                val normalizedDate = if (assignDate > 0L) assignDate else book.updatedAt
                val eventKeyBorrow = "book_borrow_${book.id}_${sId}_$normalizedDate"
                val isLiveBorrow = canDiffBooks && "${sId}_$assignDate" !in prevAssignmentKeys &&
                    (prevBook == null || prevBook.activeAssignments.none { it.studentId == sId } && prevBook.currentStudentId != sId)
                val sName = resolveStudentName(sId, current.students)
                val tsBorrow = maxOf(normalizedDate, book.updatedAt)
                processCandidate(eventKeyBorrow, tsBorrow, isLiveBorrow) {
                    BirizNotification(
                        id = "notif_$eventKeyBorrow",
                        title = "📖 Kitap Ödünç Alındı",
                        message = "$sName, \"${book.name}\" kitabını okumak üzere ödünç aldı.",
                        timestamp = tsBorrow.takeIf { it > 0L } ?: nowMs,
                        type = NotificationType.LIBRARY,
                        targetSubPageName = "READING",
                        targetId = sId,
                        isRead = false
                    )
                }
            }
        }

        // 5. Öğrenci Kitabı İade Ettiğinde
        val canDiffReadingRecords = previous != null && previous.hasReadingRecords
        val prevReadingIds = previous?.readingRecords?.map { it.id }?.toSet().orEmpty()
        current.readingRecords.forEach { rec ->
            if (rec.studentId !in linkedStudentIds) return@forEach
            val eventKey = "book_return_${rec.id}"
            val isLiveTransition = canDiffReadingRecords && rec.id !in prevReadingIds
            val sName = resolveStudentName(rec.studentId, current.students, rec.studentName)
            val ts = maxOf(rec.endDate, rec.createdAt)
            val starSuffix = if (rec.earnedStars > 0) " (+${rec.earnedStars} ⭐)" else ""
            processCandidate(eventKey, ts, isLiveTransition) {
                BirizNotification(
                    id = "notif_$eventKey",
                    title = "✅ Kitap İade Edildi",
                    message = "$sName, \"${rec.bookName}\" kitabını okuyup iade etti$starSuffix.",
                    timestamp = ts.takeIf { it > 0L } ?: nowMs,
                    type = NotificationType.LIBRARY,
                    targetSubPageName = "READING",
                    targetId = rec.studentId,
                    isRead = false
                )
            }
        }

        // 6, 7, 8, 9. Turnuva Olayları
        val canDiffTournaments = previous != null && previous.hasTournaments
        val prevTournamentsById = previous?.tournaments?.associateBy { it.id }.orEmpty()
        current.tournaments.forEach { tournament ->
            val prevTournament = prevTournamentsById[tournament.id]
            val canDiffMatches = previous != null && tournament.id in previous.loadedMatchTournamentIds
            val matches = current.matchesByTournament[tournament.id].orEmpty()
            val prevMatchesById = previous?.matchesByTournament?.get(tournament.id).orEmpty().associateBy { it.id }
            val maxKnockoutRound = matches.filter { it.stage == "knockout" }.maxOfOrNull { it.round } ?: 1
            val hasGroupStage = matches.any { it.stage == "group" } || tournament.type == TournamentType.GRUP_ELEME

            linkedStudentIds.forEach { sId ->
                if (sId !in tournament.participants) return@forEach
                val sName = resolveStudentName(sId, current.students)

                // 6. Turnuvaya Katıldığında
                val joinKey = "tourney_join_${tournament.id}_$sId"
                val isLiveJoin = canDiffTournaments && (prevTournament == null || sId !in prevTournament.participants)
                processCandidate(joinKey, tournament.createdAt, isLiveJoin) {
                    BirizNotification(
                        id = "notif_$joinKey",
                        title = "🏆 Turnuvaya Katıldı",
                        message = "$sName, \"${tournament.name}\" (${tournament.type.displayName}) turnuvasına katıldı!",
                        timestamp = tournament.createdAt.takeIf { it > 0L } ?: nowMs,
                        type = NotificationType.TOURNAMENT,
                        targetSubPageName = "TOURNAMENT",
                        targetId = sId,
                        isRead = false
                    )
                }

                // 7. Turnuvada Maç Yaptığında
                matches.filter {
                    it.status == "completed" &&
                        (it.player1Id == sId || it.player2Id == sId) &&
                        it.player1Id != "BYE" &&
                        it.player2Id != "BYE"
                }.forEach { match ->
                    val matchKey = "tourney_match_${tournament.id}_${match.id}_$sId"
                    val prevMatch = prevMatchesById[match.id]
                    val isLiveMatch = canDiffMatches && (prevMatch == null || prevMatch.status != "completed")
                    val opponentName = if (match.player1Id == sId) match.player2Name else match.player1Name
                    val roundDesc = when {
                        !match.groupName.isNullOrBlank() -> match.groupName
                        match.isThirdPlaceMatch || match.matchCode.contains("3RD", ignoreCase = true) -> "3.lük Maçı"
                        tournament.type == TournamentType.LIG -> "${match.round}. Hafta"
                        else -> knockoutRoundLabel(match.round, maxKnockoutRound)
                    }
                    val scorePart = if (match.score1 != null && match.score2 != null) {
                        if (match.player1Id == sId) " (${match.score1}-${match.score2})" else " (${match.score2}-${match.score1})"
                    } else ""
                    val outcomeText = when {
                        match.winnerId == sId -> "Kazandı 🎉"
                        match.winnerId == null && match.score1 != null && match.score1 == match.score2 -> "Berabere 🤝"
                        else -> "Tamamladı"
                    }
                    val tsMatch = maxOf(match.createdAt, tournament.updatedAt)
                    processCandidate(matchKey, tsMatch, isLiveMatch) {
                        BirizNotification(
                            id = "notif_$matchKey",
                            title = "⚔️ Turnuva Maçı Tamamlandı",
                            message = "\"${tournament.name}\" • $roundDesc: $sName vs $opponentName$scorePart — $outcomeText",
                            timestamp = tsMatch.takeIf { it > 0L } ?: nowMs,
                            type = NotificationType.TOURNAMENT,
                            targetSubPageName = "TOURNAMENT",
                            targetId = sId,
                            isRead = false
                        )
                    }
                }

                // 8. Turnuvada Tur Atladığında
                val advanceRounds = matches.filter { match ->
                    match.stage == "knockout" &&
                        !match.isThirdPlaceMatch &&
                        !match.matchCode.contains("3RD", ignoreCase = true) &&
                        (match.player1Id == sId || match.player2Id == sId)
                }.groupBy { it.round }

                advanceRounds.toSortedMap().forEach { (round, roundMatches) ->
                    val hasWonEarlierKnockout = matches.any { earlier ->
                        earlier.stage == "knockout" &&
                            earlier.round < round &&
                            earlier.status == "completed" &&
                            earlier.winnerId == sId
                    }
                    val advancedFromGroupToKo = (round == 1 && hasGroupStage && tournament.type == TournamentType.GRUP_ELEME)
                    val firstMatchInRound = roundMatches.first()
                    val prevHadRound = prevMatchesById.values.any { pm ->
                        pm.stage == "knockout" &&
                            !pm.isThirdPlaceMatch &&
                            pm.round == round &&
                            (pm.player1Id == sId || pm.player2Id == sId)
                    }
                    val isLiveAdvance = canDiffMatches && prevMatchesById.isNotEmpty() && !prevHadRound && (round > 1 || advancedFromGroupToKo)

                    val isRealAdvancement = hasWonEarlierKnockout || advancedFromGroupToKo || isLiveAdvance
                    if (isRealAdvancement) {
                        val advKey = "tourney_advance_${tournament.id}_r${round}_$sId"
                        val stageLabel = if (round == 1 && advancedFromGroupToKo && maxKnockoutRound == 1) {
                            "Final"
                        } else if (round == 1 && advancedFromGroupToKo) {
                            "Eleme (${knockoutRoundLabel(round, maxKnockoutRound)})"
                        } else {
                            knockoutRoundLabel(round, maxKnockoutRound)
                        }
                        val tsAdv = maxOf(firstMatchInRound.createdAt, tournament.updatedAt)
                        processCandidate(advKey, tsAdv, isLiveAdvance) {
                            BirizNotification(
                                id = "notif_$advKey",
                                title = "🚀 Turnuvada Tur Atladı!",
                                message = "$sName, \"${tournament.name}\" turnuvasında $stageLabel turuna yükseldi!",
                                timestamp = tsAdv.takeIf { it > 0L } ?: nowMs,
                                type = NotificationType.TOURNAMENT,
                                targetSubPageName = "TOURNAMENT",
                                targetId = sId,
                                isRead = false
                            )
                        }
                    }
                }

                // 9. Turnuva Sonucunda Ne / Kaçıncı Olarak Bitirdiğinde
                if (tournament.status == TournamentStatus.TAMAMLANDI) {
                    val finishKey = "tourney_finish_${tournament.id}_$sId"
                    val isLiveFinish = canDiffTournaments && (prevTournament == null || prevTournament.status != TournamentStatus.TAMAMLANDI)
                    val placementDesc = computeStudentTournamentPlacement(
                        tournament = tournament,
                        matches = matches,
                        studentId = sId,
                        students = current.students
                    )
                    processCandidate(finishKey, tournament.updatedAt, isLiveFinish) {
                        BirizNotification(
                            id = "notif_$finishKey",
                            title = "🏁 Turnuva Sonuçlandı",
                            message = "$sName, \"${tournament.name}\" turnuvasını $placementDesc olarak bitirdi!",
                            timestamp = tournament.updatedAt.takeIf { it > 0L } ?: nowMs,
                            type = NotificationType.TOURNAMENT,
                            targetSubPageName = "TOURNAMENT",
                            targetId = sId,
                            isRead = false
                        )
                    }
                }
            }
        }

        val sortedNew = generated.sortedByDescending { it.timestamp }
        return EvaluationResult(
            newNotifications = sortedNew,
            updatedSeenKeys = updatedSeen
        )
    }
}
