package com.example.util

import com.example.data.model.Announcement
import com.example.data.model.AnnouncementTargetType
import com.example.data.model.BirizNotification
import com.example.data.model.Book
import com.example.data.model.GroupMakerState
import com.example.data.model.LibraryBookRequest
import com.example.data.model.LibraryRequestStatus
import com.example.data.model.LibraryRequestType
import com.example.data.model.NotificationType
import com.example.data.model.ReadingRecord
import com.example.data.model.ScheduleConfig
import com.example.data.model.ScheduleLesson
import com.example.data.model.SeatingPlanState
import com.example.data.model.StarRewardRecord
import com.example.data.model.Student
import com.example.data.model.Tournament
import com.example.data.model.TournamentMatch
import com.example.data.model.TournamentStatus
import com.example.data.model.TournamentType
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Duyuru, yıldız, kitaplık (ödünç / iade / gecikme), turnuva (katılım / maç / tur atlama / derece-sıra sonucu),
 * ders programı, oturma düzeni, grup ataması ve veli katılım/ayrılma olaylarından bildirim üreten katman.
 */
object ActivityNotificationHelper {

    private const val OVERDUE_BOOK_THRESHOLD_MS = 14L * 24L * 60L * 60_000L
    private const val DAY_MS = 24L * 60L * 60_000L
    private val STAR_MILESTONES = listOf(25, 50, 100, 150, 200, 300, 500)

    data class ActivitySnapshot(
        val announcements: List<Announcement> = emptyList(),
        val starRecords: List<StarRewardRecord> = emptyList(),
        val books: List<Book> = emptyList(),
        val readingRecords: List<ReadingRecord> = emptyList(),
        val libraryRequests: List<LibraryBookRequest> = emptyList(),
        val tournaments: List<Tournament> = emptyList(),
        val matchesByTournament: Map<String, List<TournamentMatch>> = emptyMap(),
        val students: List<Student> = emptyList(),
        val scheduleConfig: ScheduleConfig? = null,
        val scheduleLessons: List<ScheduleLesson> = emptyList(),
        val scheduleUpdatedAtMs: Long = 0L,
        val seatingPlanState: SeatingPlanState? = null,
        val seatingUpdatedAtMs: Long = 0L,
        val groupMakerState: GroupMakerState? = null,
        val groupsUpdatedAtMs: Long = 0L,
        val hasAnnouncements: Boolean = true,
        val hasStars: Boolean = true,
        val hasBooks: Boolean = true,
        val hasReadingRecords: Boolean = true,
        val hasLibraryRequests: Boolean = true,
        val hasTournaments: Boolean = true,
        val hasSchedule: Boolean = false,
        val hasSeating: Boolean = false,
        val hasGroups: Boolean = false,
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
        nowMs: Long = System.currentTimeMillis(),
        currentUserEmail: String = ""
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
            val isAfterBaseline = eventTimestamp > 0L && eventTimestamp >= baselineTimestampMs
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
            val pollHint = ann.poll?.question?.takeIf { it.isNotBlank() }?.let { " • 📊 Anket: $it" } ?: ""
            processCandidate(eventKey, ts, isLiveTransition) {
                BirizNotification(
                    id = "notif_$eventKey",
                    title = if (ann.isEdited) "Duyuru Güncellendi 📢" else "Yeni Sınıf Duyurusu 📢",
                    message = "${ann.title}: ${ann.content.take(90)}$pollHint",
                    timestamp = ts.takeIf { it > 0L } ?: nowMs,
                    type = NotificationType.ANNOUNCEMENT,
                    targetSubPageName = "COMMUNICATION",
                    targetId = ann.id,
                    isRead = false
                )
            }
        }

        // 2. Yıldız Ödülleri, Uyarılar (Eksi Yıldız) ve Kilometre Taşları
        val canDiffStars = previous != null && previous.hasStars
        val prevStarIds = previous?.starRecords?.map { it.id }?.toSet().orEmpty()
        current.starRecords.forEach { sr ->
            if (sr.studentId !in linkedStudentIds || sr.starCount == 0) return@forEach
            val eventKey = "star_${sr.id}"
            val isLiveTransition = canDiffStars && sr.id !in prevStarIds
            val sName = resolveStudentName(sr.studentId, current.students, sr.studentName)
            val isPositive = sr.starCount > 0
            processCandidate(eventKey, sr.timestamp, isLiveTransition) {
                BirizNotification(
                    id = "notif_$eventKey",
                    title = if (isPositive) {
                        "⭐ $sName +${sr.starCount} Yıldız Kazandı!"
                    } else {
                        "⚠️ $sName ${sr.starCount} Yıldız (Uyarı)"
                    },
                    message = if (sr.note.isNotBlank()) "${sr.categoryName} • ${sr.note}" else sr.categoryName,
                    timestamp = sr.timestamp.takeIf { it > 0L } ?: nowMs,
                    type = NotificationType.STAR_REWARD,
                    targetSubPageName = "STARS",
                    targetId = sr.studentId,
                    isRead = false
                )
            }
        }

        if (current.hasStars) {
            linkedStudentIds.forEach { sId ->
                val studentStars = current.starRecords.filter { it.studentId == sId }
                val hasRecentTrigger = studentStars.any { sr ->
                    val isLive = canDiffStars && sr.id !in prevStarIds
                    val isPostBaseline = sr.timestamp > 0L && sr.timestamp >= baselineTimestampMs
                    sr.starCount > 0 && (isLive || isPostBaseline)
                }
                if (!hasRecentTrigger) return@forEach

                val totalStars = studentStars.sumOf { it.starCount }
                val prevTotalStars = previous?.starRecords?.filter { it.studentId == sId }?.sumOf { it.starCount } ?: 0
                val sName = resolveStudentName(sId, current.students)
                STAR_MILESTONES.forEach { milestone ->
                    if (totalStars >= milestone) {
                        val mKey = "star_milestone_${sId}_$milestone"
                        val crossedLive = canDiffStars && prevTotalStars < milestone
                        val latestTs = studentStars.maxOfOrNull { it.timestamp } ?: nowMs
                        processCandidate(mKey, latestTs, crossedLive) {
                            BirizNotification(
                                id = "notif_$mKey",
                                title = "🏅 $sName $milestone Yıldız Barajına Ulaştı!",
                                message = "Tebrikler! $sName toplamda $totalStars yıldıza ulaştı.",
                                timestamp = latestTs.takeIf { it > 0L } ?: nowMs,
                                type = NotificationType.STAR_REWARD,
                                targetSubPageName = "STARS",
                                targetId = sId,
                                isRead = false
                            )
                        }
                    }
                }
            }
        }

        // 3 & 4. Kitaplık Olayları (Ödünç alma, 14 gün gecikme hatırlatması ve iade; yeni kitap ekleme bildirim üretmez)
        val canDiffBooks = previous != null && previous.hasBooks
        val prevBooksById = previous?.books?.associateBy { it.id }.orEmpty()
        current.books.forEach { book ->
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

                // 14 gün ve üzeri ödünç süresi geçen kitaplar için haftalık hatırlatma
                if (assignDate > 0L && (nowMs - assignDate) >= OVERDUE_BOOK_THRESHOLD_MS) {
                    val daysElapsed = ((nowMs - assignDate) / DAY_MS).toInt().coerceAtLeast(14)
                    val weekBucket = (daysElapsed - 14) / 7
                    val overdueKey = "book_overdue_${book.id}_${sId}_w$weekBucket"
                    if (overdueKey !in updatedSeen) {
                        updatedSeen.add(overdueKey)
                        if (nowMs > baselineTimestampMs) {
                            generated.add(
                                BirizNotification(
                                    id = "notif_$overdueKey",
                                    title = "⏰ Kitap İade Hatırlatması",
                                    message = "$sName, \"${book.name}\" kitabını ödünç alalı $daysElapsed gün oldu. Okuma tamamlandıysa iade edebilirsiniz.",
                                    timestamp = nowMs,
                                    type = NotificationType.LIBRARY,
                                    targetSubPageName = "READING",
                                    targetId = sId,
                                    isRead = false
                                )
                            )
                        }
                    }
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

        // 5b. Veli Kitap Talebi Sonuçları (Öğretmen Onayladı / Reddetti)
        val canDiffLibraryRequests = previous != null && previous.hasLibraryRequests
        val prevReqById = previous?.libraryRequests?.associateBy { it.id }.orEmpty()
        current.libraryRequests.forEach { req ->
            if (req.studentId !in linkedStudentIds) return@forEach
            if (req.status == LibraryRequestStatus.PENDING) return@forEach

            val eventKey = "lib_req_parent_${req.id}_${req.status.name.lowercase(Locale.ROOT)}"
            val prevStatus = prevReqById[req.id]?.status
            val isLiveTransition = canDiffLibraryRequests && prevStatus != req.status
            val sName = resolveStudentName(req.studentId, current.students, req.studentName)
            val ts = (req.reviewedAt ?: req.createdAt).takeIf { it > 0L } ?: nowMs
            val isFinish = req.requestType == LibraryRequestType.FINISH_BOOK

            processCandidate(eventKey, ts, isLiveTransition) {
                if (req.status == LibraryRequestStatus.APPROVED) {
                    val title = if (isFinish) "✅ Kitap Bitirme Talebi Onaylandı" else "✅ Kitap Talebi Onaylandı"
                    val starSuffix = if (isFinish && req.earnedStarsPreview > 0) " (+${req.earnedStarsPreview} ⭐ kazanıldı)" else ""
                    val msg = if (isFinish) {
                        "$sName için \"${req.bookName}\" kitap bitirme talebiniz onaylandı ve okuma geçmişine eklendi$starSuffix."
                    } else {
                        "$sName için \"${req.bookName}\" kitap talebiniz öğretmen tarafından onaylandı ve okuma kaydı başlatıldı."
                    }
                    BirizNotification(
                        id = "notif_$eventKey",
                        title = title,
                        message = msg,
                        timestamp = ts,
                        type = NotificationType.LIBRARY,
                        targetSubPageName = "READING",
                        targetId = req.studentId,
                        isRead = false
                    )
                } else {
                    val title = if (isFinish) "❌ Kitap Bitirme Talebi Reddedildi" else "❌ Kitap Talebi Reddedildi"
                    val reasonSuffix = if (req.rejectionReason.isNotBlank()) " Açıklama: ${req.rejectionReason}" else ""
                    val msg = if (isFinish) {
                        "$sName için \"${req.bookName}\" kitap bitirme talebiniz öğretmen tarafından reddedildi.$reasonSuffix"
                    } else {
                        "$sName için \"${req.bookName}\" kitap talebiniz öğretmen tarafından reddedildi.$reasonSuffix"
                    }
                    BirizNotification(
                        id = "notif_$eventKey",
                        title = title,
                        message = msg,
                        timestamp = ts,
                        type = NotificationType.LIBRARY,
                        targetSubPageName = "READING",
                        targetId = req.studentId,
                        isRead = false
                    )
                }
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

                // 7b. Sıradaki / Yaklaşan Turnuva Maçı Eşleşmesi
                matches.filter {
                    it.status != "completed" &&
                        (it.player1Id == sId || it.player2Id == sId) &&
                        it.player1Id != "BYE" &&
                        it.player2Id != "BYE"
                }.forEach { match ->
                    val upcomingKey = "tourney_upcoming_${tournament.id}_${match.id}_$sId"
                    val prevMatch = prevMatchesById[match.id]
                    val isLiveUpcoming = canDiffMatches && prevMatch == null
                    val opponentName = if (match.player1Id == sId) match.player2Name else match.player1Name
                    val roundDesc = when {
                        !match.groupName.isNullOrBlank() -> match.groupName
                        match.isThirdPlaceMatch || match.matchCode.contains("3RD", ignoreCase = true) -> "3.lük Maçı"
                        tournament.type == TournamentType.LIG -> "${match.round}. Hafta"
                        else -> knockoutRoundLabel(match.round, maxKnockoutRound)
                    }
                    val tsUpcoming = maxOf(match.createdAt, tournament.updatedAt)
                    processCandidate(upcomingKey, tsUpcoming, isLiveUpcoming) {
                        BirizNotification(
                            id = "notif_$upcomingKey",
                            title = "🗓️ Sıradaki Turnuva Maçı Belli Oldu",
                            message = "\"${tournament.name}\" • $roundDesc: $sName vs $opponentName eşleşmesi hazır.",
                            timestamp = tsUpcoming.takeIf { it > 0L } ?: nowMs,
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

        // 10. Ders Programı Güncellemeleri
        if (current.hasSchedule && (current.scheduleLessons.isNotEmpty() || current.scheduleConfig?.isConfigured == true)) {
            val currSig = scheduleSignature(current.scheduleConfig, current.scheduleLessons)
            val prevSig = if (previous != null && previous.hasSchedule) {
                scheduleSignature(previous.scheduleConfig, previous.scheduleLessons)
            } else null
            val isLiveSchedule = prevSig != null && prevSig != currSig
            val tsSched = current.scheduleUpdatedAtMs
            val schedKey = "schedule_${currSig}"
            if (isLiveSchedule || (tsSched > 0L && tsSched >= baselineTimestampMs)) {
                processCandidate(schedKey, tsSched.takeIf { it > 0L } ?: nowMs, isLiveSchedule) {
                    BirizNotification(
                        id = "notif_$schedKey",
                        title = "📅 Ders Programı Güncellendi",
                        message = "Sınıfın haftalık ders programı veya ders saatleri öğretmen tarafından güncellendi.",
                        timestamp = tsSched.takeIf { it > 0L } ?: nowMs,
                        type = NotificationType.SCHEDULE,
                        targetSubPageName = "SCHEDULE",
                        targetId = linkedStudentIds.firstOrNull(),
                        isRead = false
                    )
                }
            } else {
                updatedSeen.add(schedKey)
            }
        }

        // 11. Oturma (Sıra) Düzeni Güncellemeleri
        if (current.hasSeating && current.seatingPlanState != null) {
            val tsSeat = current.seatingUpdatedAtMs
            linkedStudentIds.forEach { sId ->
                val currSeatId = current.seatingPlanState.assignments.entries.firstOrNull { it.value == sId }?.key ?: return@forEach
                val prevSeatId = previous?.seatingPlanState?.assignments?.entries?.firstOrNull { it.value == sId }?.key
                val isLiveSeat = previous != null && previous.hasSeating && prevSeatId != currSeatId
                val seatKey = "seating_${sId}_${currSeatId}_${tsSeat.takeIf { it > 0L } ?: 0L}"
                if (isLiveSeat || (tsSeat > 0L && tsSeat >= baselineTimestampMs)) {
                    val sName = resolveStudentName(sId, current.students)
                    val seatDesc = formatSeatLabel(currSeatId)
                    processCandidate(seatKey, tsSeat.takeIf { it > 0L } ?: nowMs, isLiveSeat) {
                        BirizNotification(
                            id = "notif_$seatKey",
                            title = "🪑 Oturma Düzeni Güncellendi",
                            message = "$sName için sınıf oturma planı güncellendi ($seatDesc).",
                            timestamp = tsSeat.takeIf { it > 0L } ?: nowMs,
                            type = NotificationType.SEATING,
                            targetSubPageName = "SEATING",
                            targetId = sId,
                            isRead = false
                        )
                    }
                } else {
                    updatedSeen.add(seatKey)
                }
            }
        }

        // 12. Grup Çalışması & Grup Liderliği Atamaları
        if (current.hasGroups && current.groupMakerState != null && current.groupMakerState.groups.isNotEmpty()) {
            val tsGroup = current.groupsUpdatedAtMs
            linkedStudentIds.forEach { sId ->
                val currGroup = current.groupMakerState.groups.firstOrNull { sId in it.studentIds } ?: return@forEach
                val isLeader = currGroup.leaderStudentId == sId
                val prevGroup = previous?.groupMakerState?.groups?.firstOrNull { sId in it.studentIds }
                val prevIsLeader = prevGroup?.leaderStudentId == sId
                val isLiveGroup = previous != null && previous.hasGroups &&
                    (prevGroup?.id != currGroup.id || prevGroup.name != currGroup.name || prevIsLeader != isLeader)
                val groupKey = "group_${sId}_${currGroup.id}_${currGroup.name}_$isLeader"
                if (isLiveGroup || (tsGroup > 0L && tsGroup >= baselineTimestampMs)) {
                    val sName = resolveStudentName(sId, current.students)
                    processCandidate(groupKey, tsGroup.takeIf { it > 0L } ?: nowMs, isLiveGroup) {
                        BirizNotification(
                            id = "notif_$groupKey",
                            title = if (isLeader) "👑 Grup Lideri Seçildi!" else "👥 Yeni Çalışma Grubu",
                            message = if (isLeader) {
                                "$sName, \"${currGroup.name}\" grubunda Grup Lideri olarak görevlendirildi."
                            } else {
                                "$sName, \"${currGroup.name}\" çalışma grubuna yerleştirildi."
                            },
                            timestamp = tsGroup.takeIf { it > 0L } ?: nowMs,
                            type = NotificationType.GROUP,
                            targetSubPageName = "HOME",
                            targetId = sId,
                            isRead = false
                        )
                    }
                } else {
                    updatedSeen.add(groupKey)
                }
            }
        }

        // 13. Güvenlik Bildirimi: Aynı Öğrenciye İkinci Veli Bağlantısı Algılandı
        val cleanViewerEmail = currentUserEmail.trim().lowercase(Locale.ROOT)
        val prevStudentsById = previous?.students?.associateBy { it.id }.orEmpty()
        linkedStudentIds.forEach { sId ->
            val currStudent = current.students.firstOrNull { it.id == sId } ?: return@forEach
            val prevStudent = prevStudentsById[sId]

            val currOtherEmails = currStudent.allLinkedParents
                .map { it.email.trim().lowercase(Locale.ROOT) }
                .filter { it.isNotBlank() && it != "veli@biriz.app" && (cleanViewerEmail.isBlank() || it != cleanViewerEmail) }
                .toSet()

            if (prevStudent != null) {
                val prevOtherEmails = prevStudent.allLinkedParents
                    .map { it.email.trim().lowercase(Locale.ROOT) }
                    .filter { it.isNotBlank() && it != "veli@biriz.app" && (cleanViewerEmail.isBlank() || it != cleanViewerEmail) }
                    .toSet()
                (prevOtherEmails - currOtherEmails).forEach { removedEmail ->
                    updatedSeen.remove("parent_sec_link_${sId}_$removedEmail")
                }
            }

            currStudent.allLinkedParents.forEach { otherParent ->
                val pEmail = otherParent.email.trim().lowercase(Locale.ROOT)
                if (pEmail.isBlank() || pEmail == "veli@biriz.app") return@forEach
                if (cleanViewerEmail.isNotBlank() && pEmail == cleanViewerEmail) return@forEach

                val prevParentEmails = prevStudent?.allLinkedParents
                    ?.map { it.email.trim().lowercase(Locale.ROOT) }
                    ?.toSet()
                    .orEmpty()
                val isLiveTransition = previous != null &&
                    previous.students.isNotEmpty() &&
                    prevStudent != null &&
                    pEmail !in prevParentEmails
                val parsedLinkedAt = parseLinkedAtMillis(otherParent.linkedAt)
                val secKey = "parent_sec_link_${sId}_$pEmail"

                processCandidate(
                    eventKey = secKey,
                    eventTimestamp = parsedLinkedAt ?: 1L,
                    isLiveTransition = isLiveTransition
                ) {
                    val sName = resolveStudentName(sId, current.students, currStudent.fullName)
                    val emailLabel = if (otherParent.email.isNotBlank()) " (${otherParent.email})" else ""
                    BirizNotification(
                        id = "notif_$secKey",
                        title = "🔐 İkinci Veli Bağlantısı Algılandı",
                        message = "${otherParent.name}$emailLabel, $sName öğrencisine veli katılım koduyla bağlandı. Bu işlemi bilmiyorsanız öğretmeninizle iletişime geçiniz.",
                        timestamp = parsedLinkedAt ?: nowMs,
                        type = NotificationType.PARENT_REGISTRATION,
                        targetSubPageName = "HOME",
                        targetId = sId,
                        isRead = false
                    )
                }
            }
        }

        val sortedNew = generated.sortedByDescending { it.timestamp }
        return EvaluationResult(
            newNotifications = sortedNew,
            updatedSeenKeys = updatedSeen
        )
    }

    private fun scheduleSignature(config: ScheduleConfig?, lessons: List<ScheduleLesson>): String {
        val cPart = config?.let {
            "${it.dailyLessonCount}_${it.firstLessonStartTime}_${it.lessonDurationMinutes}_${it.recessDurations.joinToString("-")}_${it.isLunchBreakActive}_${it.lunchAfterLessonIndex}_${it.lunchDurationMinutes}"
        } ?: "nocfg"
        val lPart = lessons.sortedWith(compareBy({ it.dayOfWeek }, { it.lessonIndex }))
            .joinToString("|") { "${it.dayOfWeek}:${it.lessonIndex}:${it.subjectName}:${it.topicOrNotes}" }
        return "$cPart#$lPart".hashCode().toString()
    }

    private fun formatSeatLabel(seatId: String): String {
        val match = Regex("""g(\d+)-r(\d+)-s(\d+)""").matchEntire(seatId.trim()) ?: return "Yeni Konum"
        val g = (match.groupValues[1].toIntOrNull() ?: 0) + 1
        val r = (match.groupValues[2].toIntOrNull() ?: 0) + 1
        return "$g. Grup • $r. Sıra"
    }

    fun parseLinkedAtMillis(raw: String?): Long? {
        val text = raw?.trim().orEmpty()
        if (text.isBlank()) return null
        val formats = listOf(
            "dd MMMM yyyy, HH:mm",
            "dd MMMM yyyy HH:mm",
            "dd.MM.yyyy HH:mm",
            "dd.MM.yyyy"
        )
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale("tr")).apply { isLenient = false }
                val parsed = sdf.parse(text)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {
            }
        }
        return null
    }

    /**
     * Öğretmen rolündeki kullanıcı için yeni veli katılımlarını deterministik ve mükerrersiz biçimde değerlendirir.
     */
    fun evaluateTeacherNotifications(
        previousStudents: List<Student>?,
        currentStudents: List<Student>,
        seenEventKeys: Set<String>,
        baselineTimestampMs: Long,
        nowMs: Long = System.currentTimeMillis()
    ): EvaluationResult {
        if (currentStudents.isEmpty()) {
            return EvaluationResult(emptyList(), seenEventKeys)
        }

        val updatedSeen = seenEventKeys.toMutableSet()
        val generated = mutableListOf<BirizNotification>()

        fun parentKeyFor(email: String, name: String): String {
            val cleanEmail = email.trim().lowercase(Locale.ROOT)
            if (cleanEmail.isNotBlank()) return cleanEmail
            return name.trim().lowercase(Locale.ROOT)
        }

        val prevStudentsById = previousStudents?.associateBy { it.id }.orEmpty()
        val prevParentKeysByStudent = previousStudents?.associate { st ->
            st.id to st.allLinkedParents.mapNotNull { p ->
                parentKeyFor(p.email, p.name).takeIf { it.isNotBlank() }
            }.toSet()
        }.orEmpty()

        // Mevcut bir öğrenciden veli bağlantısı kaldırıldıysa ayrılma bildirimi üret ve ileride yeniden katılabilmesi için key temizle
        if (previousStudents != null) {
            currentStudents.forEach { st ->
                val prevStudent = prevStudentsById[st.id] ?: return@forEach
                val prevParentsByKey = prevStudent.allLinkedParents.associateBy { p ->
                    parentKeyFor(p.email, p.name)
                }.filterKeys { it.isNotBlank() }
                val currKeys = st.allLinkedParents.mapNotNull { p ->
                    parentKeyFor(p.email, p.name).takeIf { it.isNotBlank() }
                }.toSet()
                (prevParentsByKey.keys - currKeys).forEach { removedKey ->
                    updatedSeen.remove("parent_reg_${st.id}_$removedKey")
                    val prevParent = prevParentsByKey[removedKey]
                    val studentNoPrefix = if (st.studentNumber.isNotBlank()) "${st.studentNumber} " else ""
                    val unlinkKey = "parent_unlink_${st.id}_${removedKey}_${nowMs / 60_000L}"
                    if (unlinkKey !in updatedSeen) {
                        updatedSeen.add(unlinkKey)
                        generated.add(
                            BirizNotification(
                                id = "notif_$unlinkKey",
                                title = "🔗 Veli Bağlantısı Kaldırıldı",
                                message = "${prevParent?.name ?: "Veli"}, $studentNoPrefix${st.fullName} öğrencisiyle olan bağlantısını sonlandırdı.",
                                timestamp = nowMs,
                                type = NotificationType.PARENT_REGISTRATION,
                                targetSubPageName = "SINIF_LISTESI",
                                targetId = st.id,
                                isRead = false
                            )
                        )
                    }
                }
            }
        }

        currentStudents.forEach { student ->
            val prevKeys = prevParentKeysByStudent[student.id].orEmpty()
            val studentPresentBefore = student.id in prevStudentsById
            student.allLinkedParents.forEach { parent ->
                val pKey = parentKeyFor(parent.email, parent.name)
                if (pKey.isBlank()) return@forEach

                val eventKey = "parent_reg_${student.id}_$pKey"
                if (eventKey in updatedSeen) return@forEach
                updatedSeen.add(eventKey)

                val isLiveTransition = previousStudents != null &&
                    (!studentPresentBefore || pKey !in prevKeys)
                val parsedTs = parseLinkedAtMillis(parent.linkedAt)
                val isAfterBaseline = parsedTs != null && parsedTs >= baselineTimestampMs

                if (isLiveTransition || isAfterBaseline) {
                    val studentNoPrefix = if (student.studentNumber.isNotBlank()) "${student.studentNumber} " else ""
                    val emailSuffix = if (parent.email.isNotBlank() && !parent.email.equals("veli@biriz.app", ignoreCase = true)) {
                        " (${parent.email})"
                    } else ""
                    generated.add(
                        BirizNotification(
                            id = "notif_$eventKey",
                            title = "👨‍👩‍👧 Yeni Veli Katılımı",
                            message = "${parent.name}$emailSuffix, $studentNoPrefix${student.fullName} öğrencisi için sınıfa katıldı.",
                            timestamp = parsedTs ?: nowMs,
                            type = NotificationType.PARENT_REGISTRATION,
                            targetSubPageName = "SINIF_LISTESI",
                            targetId = student.id,
                            isRead = false
                        )
                    )
                }
            }
        }

        return EvaluationResult(
            newNotifications = generated.sortedByDescending { it.timestamp },
            updatedSeenKeys = updatedSeen
        )
    }

    /**
     * Öğretmen için duyuru/anket tamamlanma özetleri ve 14+ gün geciken kitaplık iade hatırlatmalarını değerlendirir.
     */
    fun evaluateTeacherOperationalNotifications(
        previousAnnouncements: List<Announcement>?,
        currentAnnouncements: List<Announcement>,
        books: List<Book>,
        seenEventKeys: Set<String>,
        baselineTimestampMs: Long,
        nowMs: Long = System.currentTimeMillis(),
        previousLibraryRequests: List<LibraryBookRequest>? = null,
        currentLibraryRequests: List<LibraryBookRequest> = emptyList()
    ): EvaluationResult {
        val updatedSeen = seenEventKeys.toMutableSet()
        val generated = mutableListOf<BirizNotification>()

        val prevAnnById = previousAnnouncements?.associateBy { it.id }.orEmpty()
        currentAnnouncements.forEach { ann ->
            val delivered = ann.deliveredCount
            if (delivered <= 0) return@forEach
            val prevAnn = prevAnnById[ann.id]

            // Tüm hedef veliler duyuruyu okuduğunda özet bildirim
            if (ann.readCount >= delivered) {
                val readAllKey = "teacher_ann_all_read_${ann.id}_${ann.updatedAt}"
                val becameAllReadLive = prevAnn != null && prevAnn.readCount < delivered
                val latestReadTs = ann.readReceipts.values.maxOrNull() ?: ann.updatedAt
                if (readAllKey !in updatedSeen) {
                    updatedSeen.add(readAllKey)
                    if (becameAllReadLive || (latestReadTs > 0L && latestReadTs >= baselineTimestampMs)) {
                        generated.add(
                            BirizNotification(
                                id = "notif_$readAllKey",
                                title = "✅ Duyuru Tüm Velilerce Okundu",
                                message = "\"${ann.title}\" duyurusu tüm hedef veliler (${ann.readCount}/$delivered) tarafından okundu.",
                                timestamp = latestReadTs.takeIf { it > 0L } ?: nowMs,
                                type = NotificationType.ANNOUNCEMENT,
                                targetSubPageName = "ILETISIM_DUYURULAR",
                                targetId = ann.id,
                                isRead = false
                            )
                        )
                    }
                }
            }

            // Ankete tüm hedef veliler oy verdiğinde özet bildirim
            val poll = ann.poll
            if (poll != null && poll.totalVotes >= delivered) {
                val pollAllKey = "teacher_poll_all_voted_${ann.id}_${poll.id}"
                val prevVotes = prevAnn?.poll?.totalVotes ?: 0
                val becameAllVotedLive = prevAnn != null && prevVotes < delivered
                if (pollAllKey !in updatedSeen) {
                    updatedSeen.add(pollAllKey)
                    if (becameAllVotedLive || (ann.updatedAt > 0L && ann.updatedAt >= baselineTimestampMs)) {
                        generated.add(
                            BirizNotification(
                                id = "notif_$pollAllKey",
                                title = "📊 Anket Oylaması Tamamlandı",
                                message = "\"${poll.question}\" anketinde tüm hedef veliler (${poll.totalVotes}/$delivered) oy kullandı.",
                                timestamp = nowMs,
                                type = NotificationType.ANNOUNCEMENT,
                                targetSubPageName = "ILETISIM_DUYURULAR",
                                targetId = ann.id,
                                isRead = false
                            )
                        )
                    }
                }
            }
        }

        // 14 gün ve üzeri iadesi geciken kitaplar için günlük tek özet bildirim
        val overdueItems = mutableListOf<String>()
        books.forEach { book ->
            val assignments = if (book.activeAssignments.isNotEmpty()) {
                book.activeAssignments.map { it.studentName to it.assignmentDate }
            } else if (!book.currentStudentId.isNullOrBlank()) {
                listOf((book.currentStudentName ?: "Öğrenci") to (book.assignmentDate ?: 0L))
            } else emptyList()

            assignments.forEach { (sName, assignDate) ->
                if (assignDate > 0L && (nowMs - assignDate) >= OVERDUE_BOOK_THRESHOLD_MS) {
                    val days = ((nowMs - assignDate) / DAY_MS).toInt().coerceAtLeast(14)
                    overdueItems.add("$sName — \"${book.name}\" ($days gün)")
                }
            }
        }
        if (overdueItems.isNotEmpty()) {
            val dayBucket = nowMs / DAY_MS
            val overdueSummaryKey = "teacher_books_overdue_d$dayBucket"
            if (overdueSummaryKey !in updatedSeen) {
                updatedSeen.add(overdueSummaryKey)
                if (nowMs > baselineTimestampMs) {
                    val preview = overdueItems.take(2).joinToString(", ")
                    val extraSuffix = if (overdueItems.size > 2) " ve ${overdueItems.size - 2} kitap daha" else ""
                    generated.add(
                        BirizNotification(
                            id = "notif_$overdueSummaryKey",
                            title = "📚 İadesi Geciken Kitaplar (${overdueItems.size})",
                            message = "$preview$extraSuffix 14 günü aşkın süredir öğrencilerde.",
                            timestamp = nowMs,
                            type = NotificationType.LIBRARY,
                            targetSubPageName = "KITAPLIK_LISTE",
                            targetId = null,
                            isRead = false
                        )
                    )
                }
            }
        }

        // Velilerden gelen yeni bekleyen kitap ekleme / kitap bitirme talepleri için öğretmen bildirimi
        val prevReqIds = previousLibraryRequests?.map { it.id }?.toSet()
        currentLibraryRequests.forEach { req ->
            if (req.status != LibraryRequestStatus.PENDING) return@forEach
            val reqKey = "lib_req_teacher_${req.id}"
            if (reqKey in updatedSeen) return@forEach
            updatedSeen.add(reqKey)

            val isLiveNew = prevReqIds != null && req.id !in prevReqIds
            val createdTs = req.createdAt.takeIf { it > 0L } ?: nowMs
            if (isLiveNew || createdTs >= baselineTimestampMs) {
                val isFinish = req.requestType == LibraryRequestType.FINISH_BOOK
                val title = if (isFinish) {
                    "🏁 Yeni Kitap Bitti Talebi"
                } else {
                    "📘 Yeni Veli Kitap İsteği"
                }
                val message = if (isFinish) {
                    "${req.studentName} için \"${req.bookName}\" kitap bitirme talebi onayınızı bekliyor."
                } else {
                    "${req.studentName} için \"${req.bookName}\" (${req.pageCount} s.) kitap ekleme talebi onayınızı bekliyor."
                }
                generated.add(
                    BirizNotification(
                        id = "notif_$reqKey",
                        title = title,
                        message = message,
                        timestamp = createdTs,
                        type = NotificationType.LIBRARY,
                        targetSubPageName = "KITAPLIK_ISTEKLER",
                        targetId = req.id,
                        isRead = false
                    )
                )
            }
        }

        return EvaluationResult(
            newNotifications = generated.sortedByDescending { it.timestamp },
            updatedSeenKeys = updatedSeen
        )
    }
}
