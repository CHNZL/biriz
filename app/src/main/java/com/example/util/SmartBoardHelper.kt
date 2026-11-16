package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import com.example.data.model.GroupMakerState
import com.example.data.model.ScheduleConfig
import com.example.data.model.ScheduleLesson
import com.example.data.model.SeatingPlanState
import com.example.data.model.Student
import com.example.data.repository.StarRewardPersistenceHelper
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale
import java.util.UUID

/**
 * Akıllı Tahta Kumandası ve Canlı Yansıtma Yardımcısı.
 * - Öğretmen telefonda işlem yaptığında doğrudan `board_sessions/{temizKod}` belgesini
 *   ve `classes/{classId}/live_board/state` ile `classrooms/{classId}/live_board/state`
 *   belgelerini anlık günceller.
 * - Öğrenci yıldızları, okunan kitap sayıları, oturma planı, gruplar, ders programı,
 *   şanslı öğrenci, zamanlayıcı, kitaplık ve turnuva verilerini eksiksiz aktarır.
 */
object SmartBoardHelper {
    private const val TAG = "SmartBoardHelper"
    private const val PREFS_NAME = "biriz_smart_board_storage"
    private const val KEY_SESSION_CODE = "key_connected_session_code"
    private const val KEY_MIRROR_ACTIVE = "key_live_mirror_active"
    private val scope = CoroutineScope(Dispatchers.IO)
    private val sessionWriteMutex = Mutex()

    @Volatile
    private var activeSessionCode: String? = null
    @Volatile
    private var activeConnectionId: String? = null
    @Volatile
    private var connectionEpoch: Long = 0L

    // Son bilinen durum önbelleği (Yıldız, kitap, turnuva veya öğrenci verisi değiştiğinde anında senkronizasyon tetiklemek için)
    @Volatile
    private var lastClassId: String = ""
    @Volatile
    private var lastCurrentSubPage: String = "SINIF_LISTESI"
    @Volatile
    private var lastCurrentSubPageTitle: String = "Sınıf Listesi"
    @Volatile
    private var lastActiveDay: String = "PZT"
    @Volatile
    private var lastStudents: List<Student> = emptyList()
    @Volatile
    private var lastSchedule: List<ScheduleLesson> = emptyList()
    @Volatile
    private var lastScheduleConfig: ScheduleConfig = ScheduleConfig()
    @Volatile
    private var lastSeatingPlanState: SeatingPlanState? = null
    @Volatile
    private var lastGroupMakerState: GroupMakerState? = null
    @Volatile
    private var lastBoardCast: Map<String, Any?>? = null

    /**
     * 1. KOD NORMALİZASYONU (Tire, boşluk ve yabancı karakter temizliği)
     * Örn: "BRZ9-4K7P" veya "brz9 4k7p" -> "BRZ94K7P"
     */
    fun normalizeCode(code: String): String {
        return code.replace(Regex("[^A-Za-z0-9]"), "").uppercase()
    }

    private fun isEpochStillActive(cleanCode: String, expectedEpoch: Long): Boolean {
        return connectionEpoch == expectedEpoch && activeSessionCode == cleanCode
    }

    /**
     * 2. ANA VERİTABANI ERİŞİMİ
     * Öncelikle projenin özel veritabanını (R.string.firestore_database_id) döndürür.
     */
    fun getMainFirestore(context: Context): FirebaseFirestore {
        return FirestoreProvider.get(context)
    }

    fun getSavedSessionCode(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SESSION_CODE, null)
    }

    fun isMirrorActive(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_MIRROR_ACTIVE, true)
    }

    fun saveConnectionState(context: Context, sessionCode: String?, isMirrorActive: Boolean) {
        val clean = sessionCode?.let { normalizeCode(it) }?.ifEmpty { null }
        if (clean == null) {
            if (activeSessionCode != null) {
                connectionEpoch += 1L
            }
            activeSessionCode = null
            activeConnectionId = null
        } else {
            activeSessionCode = clean
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_SESSION_CODE, clean)
            .putBoolean(KEY_MIRROR_ACTIVE, isMirrorActive)
            .apply()
    }

    /**
     * Öğrencilerin yıldız puanları ve okudukları kitap sayıları ile sunum verisini hazırlar.
     * Veli kodları akıllı tahta projeksiyonuna gönderilmez.
     */
    private fun prepareStudentPresentationData(
        context: Context,
        classId: String,
        students: List<Student>
    ): List<Map<String, Any?>> {
        val starRecords = try {
            StarRewardPersistenceHelper.loadStarRecords(context, classId)
        } catch (e: Exception) {
            emptyList()
        }

        val readingRecords = try {
            LibraryPersistenceHelper.loadReadingRecords(context, classId)
        } catch (e: Exception) {
            emptyList()
        }

        val studentById = students.associateBy { it.id }
        val studentByNo = students.associateBy { it.studentNumber.trim() }
        val studentByName = students.associateBy { TurkishTextHelper.toTurkishUpperCase(it.fullName.trim()) }

        fun resolveStudentId(rawId: String, rawNo: String, rawName: String): String? {
            val cleanId = rawId.trim()
            if (cleanId.isNotEmpty() && studentById.containsKey(cleanId)) return cleanId
            val strippedNo = cleanId.removePrefix("std_").removePrefix("student_").trim()
            if (strippedNo.isNotEmpty() && studentByNo.containsKey(strippedNo)) {
                return studentByNo[strippedNo]?.id
            }
            val cleanNo = rawNo.trim()
            if (cleanNo.isNotEmpty() && studentByNo.containsKey(cleanNo)) {
                return studentByNo[cleanNo]?.id
            }
            val cleanName = TurkishTextHelper.toTurkishUpperCase(rawName.trim())
            if (cleanName.isNotEmpty() && studentByName.containsKey(cleanName)) {
                return studentByName[cleanName]?.id
            }
            return null
        }

        val starsPerStudent = mutableMapOf<String, Int>()
        for (record in starRecords) {
            val resolvedId = resolveStudentId(record.studentId, record.studentNumber, record.studentName) ?: record.studentId
            starsPerStudent[resolvedId] = (starsPerStudent[resolvedId] ?: 0) + record.starCount
        }

        val booksPerStudent = mutableMapOf<String, Int>()
        val pagesPerStudent = mutableMapOf<String, Int>()
        for (rec in readingRecords) {
            val resolvedId = resolveStudentId(rec.studentId, rec.studentNumber, rec.studentName) ?: rec.studentId
            booksPerStudent[resolvedId] = (booksPerStudent[resolvedId] ?: 0) + 1
            pagesPerStudent[resolvedId] = (pagesPerStudent[resolvedId] ?: 0) + rec.pageCount
        }

        return students.map { s ->
            mapOf(
                "id" to s.id,
                "studentNo" to (s.studentNumber.toIntOrNull() ?: 0),
                "studentNumber" to s.studentNumber,
                "fullName" to s.fullName,
                "gender" to s.gender,
                "stars" to (starsPerStudent[s.id] ?: 0),
                "booksRead" to (booksPerStudent[s.id] ?: 0),
                "pagesRead" to (pagesPerStudent[s.id] ?: 0)
            )
        }
    }

    private fun prepareRecentStarsData(context: Context, classId: String): List<Map<String, Any?>> {
        return try {
            StarRewardPersistenceHelper.loadStarRecords(context, classId)
                .take(15)
                .map { r ->
                    mapOf(
                        "id" to r.id,
                        "studentId" to r.studentId,
                        "studentName" to r.studentName,
                        "studentNumber" to r.studentNumber,
                        "categoryName" to r.categoryName,
                        "starCount" to r.starCount,
                        "note" to r.note,
                        "isLuckyBonus" to r.isLuckyBonus,
                        "timestamp" to r.timestamp
                    )
                }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun prepareLuckyStudentData(context: Context, classId: String): Map<String, Any?> {
        return try {
            val state = LuckyStudentPersistenceHelper.loadState(context, classId)
            mapOf(
                "selectedModeName" to (state?.selectedModeName ?: "ROULETTE"),
                "drawnStudentIds" to (state?.drawnStudentIds ?: emptyList<String>()),
                "excludedStudentIds" to (state?.excludedStudentIds?.toList() ?: emptyList<String>())
            )
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun prepareLibraryData(context: Context, classId: String): Map<String, Any?> {
        return try {
            val books = LibraryPersistenceHelper.loadBooks(context, classId)
            val records = LibraryPersistenceHelper.loadReadingRecords(context, classId)
            val activeLoans = books.count { it.status.equals("Okunuyor", ignoreCase = true) || it.activeAssignments.isNotEmpty() }
            mapOf(
                "totalBooksCount" to books.size,
                "activeLoansCount" to activeLoans,
                "totalCompletedCount" to records.size,
                "books" to books.take(80).map { b ->
                    mapOf(
                        "id" to b.id,
                        "registrationNo" to b.registrationNo,
                        "name" to b.name,
                        "author" to b.author,
                        "pageCount" to b.pageCount,
                        "status" to b.status,
                        "currentStudentId" to b.currentStudentId,
                        "currentStudentName" to b.currentStudentName,
                        "assignmentDate" to b.assignmentDate,
                        "activeAssignments" to b.activeAssignments.take(12).map { a ->
                            mapOf(
                                "studentId" to a.studentId,
                                "studentName" to a.studentName,
                                "assignmentDate" to a.assignmentDate
                            )
                        }
                    )
                },
                "recentRecords" to records.take(20).map { r ->
                    mapOf(
                        "id" to r.id,
                        "bookId" to r.bookId,
                        "bookName" to r.bookName,
                        "pageCount" to r.pageCount,
                        "studentId" to r.studentId,
                        "studentName" to r.studentName,
                        "studentNumber" to r.studentNumber,
                        "startDate" to r.startDate,
                        "endDate" to r.endDate,
                        "earnedStars" to r.earnedStars
                    )
                }
            )
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun prepareTournamentData(context: Context, classId: String): Map<String, Any?> {
        return try {
            val tournaments = TournamentPersistenceHelper.loadTournaments(context, classId)
            val sortedTournaments = tournaments.sortedBy { if (it.status.name == "DEVAM_EDIYOR") 0 else 1 }.take(8)
            mapOf(
                "tournaments" to sortedTournaments.map { t ->
                    val matches = TournamentPersistenceHelper.loadMatches(context, t.id).take(40)
                    mapOf(
                        "id" to t.id,
                        "name" to t.name,
                        "description" to t.description,
                        "type" to t.type.name,
                        "matchType" to t.matchType.name,
                        "status" to t.status.name,
                        "currentRound" to t.currentRound,
                        "currentStage" to t.currentStage,
                        "participantsCount" to t.participants.size,
                        "winnerId" to t.winnerId,
                        "winnerName" to t.winnerName,
                        "runnerUpId" to t.runnerUpId,
                        "runnerUpName" to t.runnerUpName,
                        "thirdPlaceId" to t.thirdPlaceId,
                        "thirdPlaceName" to t.thirdPlaceName,
                        "matches" to matches.map { m ->
                            mapOf(
                                "id" to m.id,
                                "round" to m.round,
                                "matchCode" to m.matchCode,
                                "player1Id" to m.player1Id,
                                "player2Id" to m.player2Id,
                                "player1Name" to m.player1Name,
                                "player2Name" to m.player2Name,
                                "score1" to m.score1,
                                "score2" to m.score2,
                                "winnerId" to m.winnerId,
                                "status" to m.status,
                                "groupName" to m.groupName,
                                "stage" to m.stage,
                                "isThirdPlaceMatch" to m.isThirdPlaceMatch
                            )
                        }
                    )
                }
            )
        } catch (_: Exception) {
            emptyMap()
        }
    }

    /**
     * Yıldız, kitap, turnuva, şanslı öğrenci veya öğrenci verisi değiştiğinde bağlı akıllı tahtayı anında günceller.
     */
    fun notifyDataChanged(context: Context, classId: String) {
        val activeCode = getSavedSessionCode(context)
        if (activeCode.isNullOrBlank()) return
        val targetClassId = classId.ifBlank { lastClassId }
        if (targetClassId.isBlank() || targetClassId == "default") return
        val studentsToUse = lastStudents
        if (studentsToUse.isEmpty()) return

        updateLiveBoardState(
            context = context,
            classId = targetClassId,
            sessionCode = activeCode,
            isLiveMirrorActive = isMirrorActive(context),
            currentSubPage = lastCurrentSubPage,
            currentSubPageTitle = lastCurrentSubPageTitle,
            activeDay = lastActiveDay,
            students = studentsToUse,
            schedule = lastSchedule,
            scheduleConfig = lastScheduleConfig,
            seatingPlanState = lastSeatingPlanState,
            groupMakerState = lastGroupMakerState
        )
    }

    /**
     * Zamanlayıcı (Timer) durumunu doğrudan bağlı akıllı tahtaya ve canlı sınıf durumuna senkronize eder.
     */
    fun syncTimerState(
        context: Context,
        classId: String,
        modeTitle: String,
        modeEmoji: String,
        modeDescription: String,
        totalSeconds: Int,
        remainingSeconds: Int,
        isRunning: Boolean,
        isFinished: Boolean
    ) {
        val activeCode = getSavedSessionCode(context)?.let { normalizeCode(it) }?.ifEmpty { null } ?: return
        if (activeSessionCode != activeCode) return
        val expectedEpoch = connectionEpoch
        val targetClassId = classId.ifBlank { lastClassId }
        val now = System.currentTimeMillis()
        val payload = hashMapOf<String, Any?>(
            "timerMode" to modeTitle,
            "timerModeEmoji" to modeEmoji,
            "timerModeDescription" to modeDescription,
            "timerTotalSeconds" to totalSeconds,
            "timerRemainingSeconds" to remainingSeconds,
            "timerIsRunning" to isRunning,
            "timerIsFinished" to isFinished,
            "updatedAt" to now,
            "serverTimestamp" to FieldValue.serverTimestamp()
        )
        scope.launch {
            sessionWriteMutex.withLock {
                if (!isEpochStillActive(activeCode, expectedEpoch)) return@withLock
                val mainDb = getMainFirestore(context)
                try {
                    withTimeoutOrNull(3500L) {
                        mainDb.collection("board_sessions")
                            .document(activeCode)
                            .set(payload, SetOptions.merge())
                            .await()
                    }
                    if (!isEpochStillActive(activeCode, expectedEpoch)) return@withLock
                    if (targetClassId.isNotBlank() && targetClassId != "default") {
                        withTimeoutOrNull(2500L) {
                            mainDb.collection("classrooms")
                                .document(targetClassId)
                                .collection("live_board")
                                .document("state")
                                .set(payload, SetOptions.merge())
                                .await()
                        }
                        if (!isEpochStillActive(activeCode, expectedEpoch)) return@withLock
                        withTimeoutOrNull(2500L) {
                            mainDb.collection("classes")
                                .document(targetClassId)
                                .collection("live_board")
                                .document("state")
                                .set(payload, SetOptions.merge())
                                .await()
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error syncing timer state: ${e.message}")
                }
            }
        }
    }

    /**
     * Şanslı Öğrenci çarkı dönerken o anki adayı canlı olarak akıllı tahtaya iletir.
     */
    fun syncLuckyRollingCandidate(
        context: Context,
        classId: String,
        rollingName: String,
        rollingNo: String,
        rollingGender: String
    ) {
        val activeCode = getSavedSessionCode(context)?.let { normalizeCode(it) }?.ifEmpty { null } ?: return
        if (activeSessionCode != activeCode) return
        val expectedEpoch = connectionEpoch
        val targetClassId = classId.ifBlank { lastClassId }
        val now = System.currentTimeMillis()
        val payload = mapOf(
            "isRolling" to true,
            "rollingCandidate" to mapOf(
                "fullName" to rollingName,
                "studentNumber" to rollingNo,
                "gender" to rollingGender
            ),
            "updatedAt" to now
        )
        scope.launch {
            sessionWriteMutex.withLock {
                if (!isEpochStillActive(activeCode, expectedEpoch)) return@withLock
                val mainDb = getMainFirestore(context)
                try {
                    withTimeoutOrNull(2500L) {
                        mainDb.collection("board_sessions")
                            .document(activeCode)
                            .set(payload, SetOptions.merge())
                            .await()
                    }
                    if (!isEpochStillActive(activeCode, expectedEpoch)) return@withLock
                    if (targetClassId.isNotBlank() && targetClassId != "default") {
                        withTimeoutOrNull(2500L) {
                            mainDb.collection("classrooms")
                                .document(targetClassId)
                                .collection("live_board")
                                .document("state")
                                .set(payload, SetOptions.merge())
                                .await()
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Akıllı tahtada görünen 8 haneli kod ile sunucu onaylı eşleştirme yapar.
     * 1. Aşama: Hızlı (<1 KB) CONNECTED el sıkışmasını Firestore'a yazar ve sunucu onayını bekler.
     * 2. Aşama: El sıkışma onaylandıktan hemen sonra sınıf verilerini arka planda aktarır.
     */
    suspend fun connectSession(
        context: Context,
        sessionCode: String,
        classId: String,
        teacherUid: String,
        teacherName: String,
        schoolName: String,
        grade: Int,
        branch: String,
        currentSubPage: String,
        currentSubPageTitle: String,
        activeDay: String = "PZT",
        students: List<Student> = emptyList(),
        schedule: List<ScheduleLesson> = emptyList(),
        scheduleConfig: ScheduleConfig = ScheduleConfig(),
        seatingPlanState: SeatingPlanState? = null,
        groupMakerState: GroupMakerState? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val cleanCode = normalizeCode(sessionCode)
        if (cleanCode.length < 4) {
            return@withContext Result.failure(IllegalArgumentException("Eşleştirme kodu en az 4 karakter olmalıdır."))
        }

        val myEpoch = ++connectionEpoch
        val now = System.currentTimeMillis()
        val newConnectionId = "conn_${now}_${UUID.randomUUID().toString().replace("-", "").take(8)}"
        activeSessionCode = cleanCode
        activeConnectionId = newConnectionId
        lastBoardCast = null

        lastClassId = classId
        lastCurrentSubPage = currentSubPage
        lastCurrentSubPageTitle = currentSubPageTitle
        lastActiveDay = activeDay
        if (students.isNotEmpty()) lastStudents = students
        if (schedule.isNotEmpty()) lastSchedule = schedule
        lastScheduleConfig = scheduleConfig
        if (seatingPlanState != null) lastSeatingPlanState = seatingPlanState
        if (groupMakerState != null) lastGroupMakerState = groupMakerState

        val handshakePayload = hashMapOf<String, Any?>(
            "sessionCode" to cleanCode,
            "connectionId" to newConnectionId,
            "status" to "CONNECTED",
            "isConnected" to true,
            "isLiveMirrorActive" to true,
            "isPrivacyMode" to false,
            "teacherUid" to teacherUid,
            "teacherName" to teacherName,
            "classId" to classId,
            "schoolName" to schoolName,
            "grade" to grade,
            "branch" to branch,
            "activeModule" to currentSubPage,
            "activeModuleTitle" to currentSubPageTitle,
            "currentSubPage" to currentSubPage,
            "currentSubPageTitle" to currentSubPageTitle,
            "selectedDay" to activeDay,
            "activeScheduleDay" to activeDay,
            "connectedAt" to now,
            "updatedAt" to now,
            "disconnectedAt" to FieldValue.delete(),
            "disconnectedSessionCode" to FieldValue.delete(),
            "disconnectedConnectionId" to FieldValue.delete(),
            "boardCast" to FieldValue.delete(),
            "serverTimestamp" to FieldValue.serverTimestamp()
        )

        val mainDb = try {
            getMainFirestore(context)
        } catch (e: Exception) {
            connectionEpoch += 1L
            activeSessionCode = null
            activeConnectionId = null
            saveConnectionState(context, null, false)
            return@withContext Result.failure(Exception("Veritabanı başlatılamadı: ${e.message}"))
        }

        val handshakeResult = sessionWriteMutex.withLock {
            if (!isEpochStillActive(cleanCode, myEpoch)) {
                return@withLock Result.failure(IllegalStateException("Bağlantı işlemi iptal edildi."))
            }
            try {
                withTimeout(10_000L) {
                    mainDb.collection("board_sessions")
                        .document(cleanCode)
                        .set(handshakePayload, SetOptions.merge())
                        .await()
                }

                if (!isEpochStillActive(cleanCode, myEpoch)) {
                    return@withLock Result.failure(IllegalStateException("Bağlantı işlemi iptal edildi."))
                }

                saveConnectionState(context, cleanCode, true)

                if (classId.isNotBlank() && classId != "default") {
                    withTimeoutOrNull(3500L) {
                        mainDb.collection("classes")
                            .document(classId)
                            .collection("live_board")
                            .document("state")
                            .set(handshakePayload, SetOptions.merge())
                            .await()
                    }
                    if (!isEpochStillActive(cleanCode, myEpoch)) {
                        return@withLock Result.failure(IllegalStateException("Bağlantı işlemi iptal edildi."))
                    }
                    withTimeoutOrNull(3500L) {
                        mainDb.collection("classrooms")
                            .document(classId)
                            .collection("live_board")
                            .document("state")
                            .set(handshakePayload, SetOptions.merge())
                            .await()
                    }
                }

                Result.success(Unit)
            } catch (e: TimeoutCancellationException) {
                Log.w(TAG, "Handshake timed out for $cleanCode")
                Result.failure(Exception("Tahta sunucusuna bağlanılamadı (zaman aşımı). İnternet bağlantınızı kontrol edip tekrar deneyin."))
            } catch (e: Exception) {
                Log.e(TAG, "Handshake failed for $cleanCode", e)
                Result.failure(Exception("Tahta bağlantısı sunucuya kaydedilemedi: ${e.localizedMessage ?: "Ağ hatası"}"))
            }
        }

        if (handshakeResult.isFailure) {
            if (isEpochStillActive(cleanCode, myEpoch)) {
                connectionEpoch += 1L
                activeSessionCode = null
                activeConnectionId = null
                saveConnectionState(context, null, false)
            }
            return@withContext handshakeResult
        }

        // 2. AŞAMA: Ağır sınıf modül verilerini arka planda güvenli şekilde gönder
        scope.launch {
            val dataNow = System.currentTimeMillis()
            val studentData = prepareStudentPresentationData(context, classId, students)
            val recentStars = prepareRecentStarsData(context, classId)
            val luckyData = prepareLuckyStudentData(context, classId)
            val libraryData = prepareLibraryData(context, classId)
            val tournamentData = prepareTournamentData(context, classId)

            val scheduleData = schedule.map { l ->
                mapOf(
                    "dayOfWeek" to l.dayOfWeek,
                    "lessonIndex" to l.lessonIndex,
                    "subjectName" to l.subjectName,
                    "topicOrNotes" to l.topicOrNotes,
                    "teacherName" to l.teacherName,
                    "colorTheme" to l.colorTheme
                )
            }

            val scheduleConfigData = mapOf(
                "dailyLessonCount" to scheduleConfig.dailyLessonCount,
                "firstLessonStartTime" to scheduleConfig.firstLessonStartTime,
                "lessonDurationMinutes" to scheduleConfig.lessonDurationMinutes,
                "recessDurations" to scheduleConfig.recessDurations,
                "isLunchBreakActive" to scheduleConfig.isLunchBreakActive,
                "lunchAfterLessonIndex" to scheduleConfig.lunchAfterLessonIndex,
                "lunchDurationMinutes" to scheduleConfig.lunchDurationMinutes
            )

            val seatingData = seatingPlanState?.let { plan ->
                mapOf(
                    "groupCount" to plan.config.groupCount,
                    "rowsPerGroup" to plan.config.rowsPerGroup,
                    "peoplePerRow" to plan.config.peoplePerRow,
                    "windowPosition" to plan.config.windowPosition.name,
                    "doorPosition" to plan.config.doorPosition.name,
                    "deskPosition" to plan.config.deskPosition.name,
                    "assignments" to plan.assignments,
                    "activePlanName" to plan.activePlanName
                )
            }

            val groupsData = groupMakerState?.let { gm ->
                mapOf(
                    "groups" to gm.groups.map { g ->
                        mapOf(
                            "id" to g.id,
                            "name" to g.name,
                            "colorTheme" to g.colorTheme.id,
                            "leaderStudentId" to g.leaderStudentId,
                            "studentIds" to g.studentIds
                        )
                    },
                    "unassignedStudentIds" to gm.unassignedStudentIds,
                    "isPresenterMode" to gm.isPresenterMode
                )
            }

            val fullPayload = hashMapOf<String, Any?>(
                "sessionCode" to cleanCode,
                "connectionId" to newConnectionId,
                "status" to "CONNECTED",
                "isConnected" to true,
                "isLiveMirrorActive" to true,
                "isPrivacyMode" to false,
                "classId" to classId,
                "students" to studentData,
                "recentStars" to recentStars,
                "schedule" to scheduleData,
                "scheduleConfig" to scheduleConfigData,
                "luckyData" to luckyData,
                "libraryData" to libraryData,
                "tournamentData" to tournamentData,
                "updatedAt" to dataNow,
                "serverTimestamp" to FieldValue.serverTimestamp()
            )
            if (seatingData != null) fullPayload["seatingPlan"] = seatingData
            if (groupsData != null) fullPayload["groups"] = groupsData

            sessionWriteMutex.withLock {
                if (!isEpochStillActive(cleanCode, myEpoch)) return@withLock
                try {
                    withTimeoutOrNull(8000L) {
                        mainDb.collection("board_sessions")
                            .document(cleanCode)
                            .set(fullPayload, SetOptions.merge())
                            .await()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Full payload write warning, retrying slim payload: ${e.message}")
                    if (isEpochStillActive(cleanCode, myEpoch)) {
                        val slimPayload = HashMap(fullPayload).apply {
                            remove("libraryData")
                            remove("tournamentData")
                        }
                        try {
                            withTimeoutOrNull(6000L) {
                                mainDb.collection("board_sessions")
                                    .document(cleanCode)
                                    .set(slimPayload, SetOptions.merge())
                                    .await()
                            }
                        } catch (_: Exception) {}
                    }
                }

                if (!isEpochStillActive(cleanCode, myEpoch)) return@withLock

                if (classId.isNotBlank() && classId != "default") {
                    try {
                        withTimeoutOrNull(4500L) {
                            mainDb.collection("classes")
                                .document(classId)
                                .collection("live_board")
                                .document("state")
                                .set(fullPayload, SetOptions.merge())
                                .await()
                        }
                        if (!isEpochStillActive(cleanCode, myEpoch)) return@withLock
                        withTimeoutOrNull(4500L) {
                            mainDb.collection("classrooms")
                                .document(classId)
                                .collection("live_board")
                                .document("state")
                                .set(fullPayload, SetOptions.merge())
                                .await()
                        }
                    } catch (_: Exception) {}
                }

                try {
                    if (!isEpochStillActive(cleanCode, myEpoch)) return@withLock
                    val defaultDb = FirebaseFirestore.getInstance()
                    if (defaultDb != mainDb) {
                        val mergedBackup = HashMap(handshakePayload).apply {
                            putAll(fullPayload)
                        }
                        withTimeoutOrNull(4000L) {
                            defaultDb.collection("board_sessions")
                                .document(cleanCode)
                                .set(mergedBackup, SetOptions.merge())
                                .await()
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        return@withContext Result.success(Unit)
    }

    /**
     * Akıllı tahtaya canlı ekran durumunu yansıtır.
     * DOĞRUDAN `board_sessions/{temizKod}` belgesini ve
     * `classes/{classId}/live_board/state` ile `classrooms/{classId}/live_board/state` belgelerini günceller.
     */
    fun updateLiveBoardState(
        context: Context,
        classId: String,
        sessionCode: String?,
        isLiveMirrorActive: Boolean,
        currentSubPage: String,
        currentSubPageTitle: String,
        activeDay: String = "PZT",
        students: List<Student> = emptyList(),
        schedule: List<ScheduleLesson> = emptyList(),
        scheduleConfig: ScheduleConfig = lastScheduleConfig,
        seatingPlanState: SeatingPlanState? = null,
        groupMakerState: GroupMakerState? = null,
        extraData: Map<String, Any?> = emptyMap()
    ) {
        if (classId.isNotBlank() && classId != "default") lastClassId = classId
        lastCurrentSubPage = currentSubPage
        lastCurrentSubPageTitle = currentSubPageTitle
        lastActiveDay = activeDay
        if (students.isNotEmpty()) lastStudents = students
        if (schedule.isNotEmpty()) lastSchedule = schedule
        lastScheduleConfig = scheduleConfig
        if (seatingPlanState != null) lastSeatingPlanState = seatingPlanState
        if (groupMakerState != null) lastGroupMakerState = groupMakerState

        val cleanCode = sessionCode?.let { normalizeCode(it) }?.ifEmpty { null } ?: return
        if (activeSessionCode == null) {
            val saved = getSavedSessionCode(context)?.let { normalizeCode(it) }?.ifEmpty { null }
            if (saved == cleanCode) {
                activeSessionCode = saved
            }
        }
        if (activeSessionCode != cleanCode) return
        val expectedEpoch = connectionEpoch

        val isPrivacyMode = !isLiveMirrorActive
        val now = System.currentTimeMillis()

        val effectiveStudents = if (students.isNotEmpty()) students else lastStudents
        val effectiveSchedule = if (schedule.isNotEmpty()) schedule else lastSchedule
        val effectiveSeating = seatingPlanState ?: lastSeatingPlanState
        val effectiveGroups = groupMakerState ?: lastGroupMakerState

        val studentData = prepareStudentPresentationData(context, classId, effectiveStudents)
        val recentStars = prepareRecentStarsData(context, classId)
        val luckyData = prepareLuckyStudentData(context, classId)
        val libraryData = prepareLibraryData(context, classId)
        val tournamentData = prepareTournamentData(context, classId)

        val scheduleData = effectiveSchedule.map { l ->
            mapOf(
                "dayOfWeek" to l.dayOfWeek,
                "lessonIndex" to l.lessonIndex,
                "subjectName" to l.subjectName,
                "topicOrNotes" to l.topicOrNotes,
                "teacherName" to l.teacherName,
                "colorTheme" to l.colorTheme
            )
        }

        val scheduleConfigData = mapOf(
            "dailyLessonCount" to scheduleConfig.dailyLessonCount,
            "firstLessonStartTime" to scheduleConfig.firstLessonStartTime,
            "lessonDurationMinutes" to scheduleConfig.lessonDurationMinutes,
            "recessDurations" to scheduleConfig.recessDurations,
            "isLunchBreakActive" to scheduleConfig.isLunchBreakActive,
            "lunchAfterLessonIndex" to scheduleConfig.lunchAfterLessonIndex,
            "lunchDurationMinutes" to scheduleConfig.lunchDurationMinutes
        )

        val seatingData = effectiveSeating?.let { plan ->
            mapOf(
                "groupCount" to plan.config.groupCount,
                "rowsPerGroup" to plan.config.rowsPerGroup,
                "peoplePerRow" to plan.config.peoplePerRow,
                "windowPosition" to plan.config.windowPosition.name,
                "doorPosition" to plan.config.doorPosition.name,
                "deskPosition" to plan.config.deskPosition.name,
                "assignments" to plan.assignments,
                "activePlanName" to plan.activePlanName
            )
        }

        val groupsData = effectiveGroups?.let { gm ->
            mapOf(
                "groups" to gm.groups.map { g ->
                    mapOf(
                        "id" to g.id,
                        "name" to g.name,
                        "colorTheme" to g.colorTheme.id,
                        "leaderStudentId" to g.leaderStudentId,
                        "studentIds" to g.studentIds
                    )
                },
                "unassignedStudentIds" to gm.unassignedStudentIds,
                "isPresenterMode" to gm.isPresenterMode
            )
        }

        val activeCast = lastBoardCast
        val isCastCurrentlyActive = (activeCast?.get("active") as? Boolean) == true
        val effectiveBoardSubPage = if (isCastCurrentlyActive) "PAYLASIM" else currentSubPage
        val effectiveBoardSubPageTitle = if (isCastCurrentlyActive) "📡 Paylaşım" else currentSubPageTitle

        val boardSessionUpdate = hashMapOf<String, Any?>(
            "classId" to classId,
            "activeModule" to effectiveBoardSubPage,
            "activeModuleTitle" to effectiveBoardSubPageTitle,
            "currentSubPage" to effectiveBoardSubPage,
            "currentSubPageTitle" to effectiveBoardSubPageTitle,
            "isLiveMirrorActive" to isLiveMirrorActive,
            "isPrivacyMode" to isPrivacyMode,
            "selectedDay" to activeDay,
            "activeScheduleDay" to activeDay,
            "recentStars" to recentStars,
            "scheduleConfig" to scheduleConfigData,
            "luckyData" to luckyData,
            "libraryData" to libraryData,
            "tournamentData" to tournamentData,
            "updatedAt" to now,
            "serverTimestamp" to FieldValue.serverTimestamp(),
            "sessionCode" to cleanCode,
            "status" to "CONNECTED",
            "isConnected" to true
        )
        if (studentData.isNotEmpty()) {
            boardSessionUpdate["students"] = studentData
        }
        if (scheduleData.isNotEmpty()) {
            boardSessionUpdate["schedule"] = scheduleData
        }
        if (seatingData != null) {
            boardSessionUpdate["seatingPlan"] = seatingData
        }
        if (groupsData != null) {
            boardSessionUpdate["groups"] = groupsData
        }
        if (activeCast != null) {
            boardSessionUpdate["boardCast"] = activeCast
        }
        if (extraData.isNotEmpty()) {
            boardSessionUpdate.putAll(extraData)
            boardSessionUpdate["extraData"] = extraData
        }

        val liveBoardPayload = hashMapOf<String, Any?>(
            "classId" to classId,
            "sessionCode" to cleanCode,
            "status" to "CONNECTED",
            "isConnected" to true,
            "isLiveMirrorActive" to isLiveMirrorActive,
            "isPrivacyMode" to isPrivacyMode,
            "currentSubPage" to effectiveBoardSubPage,
            "currentSubPageTitle" to effectiveBoardSubPageTitle,
            "activeModule" to effectiveBoardSubPage,
            "activeModuleTitle" to effectiveBoardSubPageTitle,
            "activeScheduleDay" to activeDay,
            "selectedDay" to activeDay,
            "recentStars" to recentStars,
            "scheduleConfig" to scheduleConfigData,
            "luckyData" to luckyData,
            "libraryData" to libraryData,
            "tournamentData" to tournamentData,
            "updatedAt" to now,
            "serverTimestamp" to FieldValue.serverTimestamp()
        )
        if (studentData.isNotEmpty()) {
            liveBoardPayload["students"] = studentData
        }
        if (scheduleData.isNotEmpty()) {
            liveBoardPayload["schedule"] = scheduleData
        }
        if (seatingData != null) {
            liveBoardPayload["seatingPlan"] = seatingData
        }
        if (groupsData != null) {
            liveBoardPayload["groups"] = groupsData
        }
        if (activeCast != null) {
            liveBoardPayload["boardCast"] = activeCast
        }
        if (extraData.isNotEmpty()) {
            liveBoardPayload.putAll(extraData)
            liveBoardPayload["extraData"] = extraData
        }

        scope.launch {
            sessionWriteMutex.withLock {
                if (!isEpochStillActive(cleanCode, expectedEpoch)) return@withLock
                val mainDb = getMainFirestore(context)
                try {
                    activeConnectionId?.let { cid ->
                        boardSessionUpdate["connectionId"] = cid
                        liveBoardPayload["connectionId"] = cid
                    }
                    // 1. DOĞRUDAN board_sessions/{temizKod} belgesini güncelle
                    withTimeoutOrNull(5000L) {
                        mainDb.collection("board_sessions")
                            .document(cleanCode)
                            .set(boardSessionUpdate, SetOptions.merge())
                            .await()
                    }

                    if (!isEpochStillActive(cleanCode, expectedEpoch)) return@withLock

                    // 2. classes/{classId}/live_board/state & classrooms/{classId}/live_board/state
                    if (classId.isNotBlank() && classId != "default") {
                        withTimeoutOrNull(4000L) {
                            mainDb.collection("classes")
                                .document(classId)
                                .collection("live_board")
                                .document("state")
                                .set(liveBoardPayload, SetOptions.merge())
                                .await()
                        }
                        if (!isEpochStillActive(cleanCode, expectedEpoch)) return@withLock
                        withTimeoutOrNull(4000L) {
                            mainDb.collection("classrooms")
                                .document(classId)
                                .collection("live_board")
                                .document("state")
                                .set(liveBoardPayload, SetOptions.merge())
                                .await()
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error updating live board state on main db: ${e.message}")
                }

                // Yedek varsayılan veritabanına sessizce yaz
                try {
                    if (!isEpochStillActive(cleanCode, expectedEpoch)) return@withLock
                    val defaultDb = FirebaseFirestore.getInstance()
                    if (defaultDb != mainDb) {
                        withTimeoutOrNull(3500L) {
                            defaultDb.collection("board_sessions")
                                .document(cleanCode)
                                .set(boardSessionUpdate, SetOptions.merge())
                                .await()
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Akıllı Tahta Bağlantısını Kes
     */
    fun disconnectSession(context: Context, sessionCode: String?, classId: String) {
        val targetCode = sessionCode?.let { normalizeCode(it) }?.ifEmpty { null }
            ?: activeSessionCode
            ?: getSavedSessionCode(context)?.let { normalizeCode(it) }?.ifEmpty { null }
        val prevConnId = activeConnectionId ?: ""
        connectionEpoch += 1L
        activeSessionCode = null
        activeConnectionId = null
        lastBoardCast = null
        saveConnectionState(context, null, false)

        val now = System.currentTimeMillis()
        val disconnectSessionPayload = hashMapOf<String, Any?>(
            "sessionCode" to (targetCode ?: ""),
            "connectionId" to "",
            "disconnectedConnectionId" to prevConnId,
            "status" to "DISCONNECTED",
            "isConnected" to false,
            "isLiveMirrorActive" to false,
            "isPrivacyMode" to true,
            "classId" to "",
            "boardCast" to mapOf(
                "active" to false,
                "castId" to "",
                "isPaused" to false,
                "inlineBase64" to "",
                "updatedAt" to now
            ),
            "disconnectedAt" to now,
            "updatedAt" to now,
            "serverTimestamp" to FieldValue.serverTimestamp()
        )
        val disconnectLivePayload = hashMapOf<String, Any?>(
            "classId" to classId,
            "sessionCode" to "",
            "connectionId" to "",
            "disconnectedConnectionId" to prevConnId,
            "disconnectedSessionCode" to (targetCode ?: ""),
            "status" to "DISCONNECTED",
            "isConnected" to false,
            "isLiveMirrorActive" to false,
            "isPrivacyMode" to true,
            "boardCast" to mapOf(
                "active" to false,
                "castId" to "",
                "isPaused" to false,
                "inlineBase64" to "",
                "updatedAt" to now
            ),
            "disconnectedAt" to now,
            "updatedAt" to now,
            "serverTimestamp" to FieldValue.serverTimestamp()
        )

        scope.launch {
            withContext(NonCancellable) {
                sessionWriteMutex.withLock {
                    val mainDb = getMainFirestore(context)
                    if (!targetCode.isNullOrBlank()) {
                        var mainWritten = false
                        for (attempt in 1..2) {
                            try {
                                withTimeout(8000L) {
                                    mainDb.collection("board_sessions")
                                        .document(targetCode)
                                        .set(disconnectSessionPayload, SetOptions.merge())
                                        .await()
                                }
                                mainWritten = true
                                break
                            } catch (e: Exception) {
                                Log.w(TAG, "Disconnect attempt $attempt failed for $targetCode: ${e.message}")
                                if (attempt == 1) delay(450L)
                            }
                        }
                        if (!mainWritten) {
                            try {
                                mainDb.collection("board_sessions")
                                    .document(targetCode)
                                    .set(disconnectSessionPayload, SetOptions.merge())
                            } catch (_: Exception) {}
                        }
                    }

                    if (classId.isNotBlank() && classId != "default") {
                        try {
                            withTimeoutOrNull(4500L) {
                                mainDb.collection("classes")
                                    .document(classId)
                                    .collection("live_board")
                                    .document("state")
                                    .set(disconnectLivePayload, SetOptions.merge())
                                    .await()
                            }
                            withTimeoutOrNull(4500L) {
                                mainDb.collection("classrooms")
                                    .document(classId)
                                    .collection("live_board")
                                    .document("state")
                                    .set(disconnectLivePayload, SetOptions.merge())
                                    .await()
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Error disconnecting live_board: ${e.message}")
                        }
                    }

                    // Yedek varsayılan veritabanı
                    try {
                        val defaultDb = FirebaseFirestore.getInstance()
                        if (defaultDb != mainDb && !targetCode.isNullOrBlank()) {
                            withTimeoutOrNull(4500L) {
                                defaultDb.collection("board_sessions")
                                    .document(targetCode)
                                    .set(disconnectSessionPayload, SetOptions.merge())
                                    .await()
                            }
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    /**
     * Uygulama açılışında yerelde kayıtlı tahta oturumunun sunucuda hâlâ aktif olup olmadığını doğrular.
     */
    suspend fun verifySessionStillConnected(context: Context, sessionCode: String): Boolean = withContext(Dispatchers.IO) {
        val clean = normalizeCode(sessionCode)
        if (clean.length < 4) return@withContext false
        return@withContext try {
            val mainDb = getMainFirestore(context)
            val snap = withTimeoutOrNull(5000L) {
                mainDb.collection("board_sessions").document(clean).get().await()
            } ?: return@withContext true // Zaman aşımı/çevrimdışıysa yerel oturumu koru
            if (!snap.exists()) return@withContext false
            val st = snap.getString("status")?.uppercase(Locale.ROOT) ?: ""
            val isConn = snap.getBoolean("isConnected")
            !(st == "DISCONNECTED" || st == "WAITING" || (isConn == false && st != "CONNECTED"))
        } catch (_: Exception) {
            true
        }
    }

    private fun resolveMimeType(context: Context, uri: Uri, fileName: String): String {
        val rawMime = context.contentResolver.getType(uri)?.trim()?.lowercase(Locale.ROOT)
        if (!rawMime.isNullOrBlank() && rawMime != "application/octet-stream") {
            return rawMime
        }
        val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        if (ext.isNotBlank()) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)?.let { return it }
            return when (ext) {
                "pdf" -> "application/pdf"
                "png" -> "image/png"
                "jpg", "jpeg" -> "image/jpeg"
                "webp" -> "image/webp"
                "gif" -> "image/gif"
                "svg" -> "image/svg+xml"
                "mp4" -> "video/mp4"
                "webm" -> "video/webm"
                "mp3" -> "audio/mpeg"
                "wav" -> "audio/wav"
                "m4a" -> "audio/mp4"
                "html", "htm" -> "text/html"
                "txt", "csv" -> "text/plain"
                "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                "ppt" -> "application/vnd.ms-powerpoint"
                "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                "doc" -> "application/msword"
                "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                "apk" -> "application/vnd.android.package-archive"
                "exe" -> "application/x-msdownload"
                "zip" -> "application/zip"
                else -> "application/octet-stream"
            }
        }
        return "application/octet-stream"
    }

    private fun resolveFileName(context: Context, uri: Uri): String {
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) {
                    val name = cursor.getString(idx)
                    if (!name.isNullOrBlank()) return name
                }
            }
        } catch (_: Exception) {}
        return uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { "dosya" } ?: "dosya"
    }

    private fun optimizeLargeImageIfPossible(rawBytes: ByteArray, mimeType: String): Pair<ByteArray, String> {
        if (!mimeType.startsWith("image/") || mimeType == "image/gif" || mimeType == "image/svg+xml") {
            return rawBytes to mimeType
        }
        if (rawBytes.size <= 380 * 1024) {
            return rawBytes to mimeType
        }
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, bounds)
            val maxSide = maxOf(bounds.outWidth, bounds.outHeight)
            var sampleSize = 1
            while (maxSide / sampleSize > 1920) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bmp = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOpts)
                ?: return rawBytes to mimeType
            val out = ByteArrayOutputStream()
            val quality = if (rawBytes.size > 3 * 1024 * 1024) 80 else 86
            bmp.compress(Bitmap.CompressFormat.JPEG, quality, out)
            bmp.recycle()
            val compressed = out.toByteArray()
            if (compressed.size < rawBytes.size) compressed to "image/jpeg" else rawBytes to mimeType
        } catch (_: Exception) {
            rawBytes to mimeType
        }
    }

    private suspend fun publishBoardCastMap(
        context: Context,
        classId: String,
        cleanCode: String,
        castMap: Map<String, Any?>
    ) {
        if (activeSessionCode != cleanCode) return
        val expectedEpoch = connectionEpoch
        val now = System.currentTimeMillis()
        lastBoardCast = castMap
        val payload = hashMapOf<String, Any?>(
            "classId" to classId,
            "sessionCode" to cleanCode,
            "status" to "CONNECTED",
            "isConnected" to true,
            "isLiveMirrorActive" to true,
            "isPrivacyMode" to false,
            "activeModule" to "PAYLASIM",
            "activeModuleTitle" to "📡 Paylaşım",
            "currentSubPage" to "PAYLASIM",
            "currentSubPageTitle" to "📡 Paylaşım",
            "boardCast" to castMap,
            "updatedAt" to now,
            "serverTimestamp" to FieldValue.serverTimestamp()
        )
        sessionWriteMutex.withLock {
            if (!isEpochStillActive(cleanCode, expectedEpoch)) return@withLock
            val mainDb = getMainFirestore(context)
            mainDb.collection("board_sessions")
                .document(cleanCode)
                .set(payload, SetOptions.merge())
                .await()

            if (!isEpochStillActive(cleanCode, expectedEpoch)) return@withLock

            if (classId.isNotBlank() && classId != "default") {
                mainDb.collection("classes")
                    .document(classId)
                    .collection("live_board")
                    .document("state")
                    .set(payload, SetOptions.merge())

                mainDb.collection("classrooms")
                    .document(classId)
                    .collection("live_board")
                    .document("state")
                    .set(payload, SetOptions.merge())
            }

            try {
                if (!isEpochStillActive(cleanCode, expectedEpoch)) return@withLock
                val defaultDb = FirebaseFirestore.getInstance()
                if (defaultDb != mainDb) {
                    defaultDb.collection("board_sessions")
                        .document(cleanCode)
                        .set(payload, SetOptions.merge())
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Seçilen telefon uygulamasının canlı ekran karesini Akıllı Tahta 'Paylaşım' sayfasına aktarır.
     */
    suspend fun publishScreenMirrorFrame(
        context: Context,
        classId: String,
        sessionCode: String,
        castId: String,
        packageName: String,
        appName: String,
        frameSeq: Long,
        base64Jpeg: String
    ) {
        val cleanCode = normalizeCode(sessionCode)
        if (cleanCode.length < 4 || activeSessionCode != cleanCode) return
        val now = System.currentTimeMillis()
        val castMap = hashMapOf<String, Any?>(
            "active" to true,
            "castId" to castId,
            "type" to "SCREEN_MIRROR",
            "title" to appName,
            "packageName" to packageName,
            "mimeType" to "image/jpeg",
            "isPaused" to false,
            "pauseReason" to "",
            "frameSeq" to frameSeq,
            "inlineBase64" to base64Jpeg,
            "updatedAt" to now
        )
        publishBoardCastMap(context, classId, cleanCode, castMap)
    }

    /**
     * Uygulamadan çıkıldığında veya ekran kapatıldığında tahtadaki son görüntüyü silmeden dondurur / tekrar açar.
     */
    suspend fun updateScreenMirrorPauseState(
        context: Context,
        classId: String,
        sessionCode: String,
        castId: String,
        appName: String,
        packageName: String,
        isPaused: Boolean,
        pauseReason: String
    ) {
        val cleanCode = normalizeCode(sessionCode)
        if (cleanCode.length < 4 || activeSessionCode != cleanCode) return
        val expectedEpoch = connectionEpoch
        val now = System.currentTimeMillis()

        val updatedCached = HashMap(lastBoardCast ?: emptyMap()).apply {
            put("active", true)
            put("castId", castId)
            put("type", "SCREEN_MIRROR")
            put("title", appName)
            put("packageName", packageName)
            put("isPaused", isPaused)
            put("pauseReason", pauseReason)
            put("updatedAt", now)
        }
        lastBoardCast = updatedCached

        val partialBoardCast = mapOf(
            "active" to true,
            "castId" to castId,
            "type" to "SCREEN_MIRROR",
            "title" to appName,
            "packageName" to packageName,
            "isPaused" to isPaused,
            "pauseReason" to pauseReason,
            "updatedAt" to now
        )
        val payload = hashMapOf<String, Any?>(
            "status" to "CONNECTED",
            "isConnected" to true,
            "isLiveMirrorActive" to true,
            "isPrivacyMode" to false,
            "activeModule" to "PAYLASIM",
            "activeModuleTitle" to "📡 Paylaşım",
            "currentSubPage" to "PAYLASIM",
            "currentSubPageTitle" to "📡 Paylaşım",
            "boardCast" to partialBoardCast,
            "updatedAt" to now
        )

        try {
            sessionWriteMutex.withLock {
                if (!isEpochStillActive(cleanCode, expectedEpoch)) return@withLock
                val mainDb = getMainFirestore(context)
                mainDb.collection("board_sessions")
                    .document(cleanCode)
                    .set(payload, SetOptions.merge())
                    .await()

                if (!isEpochStillActive(cleanCode, expectedEpoch)) return@withLock

                if (classId.isNotBlank() && classId != "default") {
                    mainDb.collection("classes")
                        .document(classId)
                        .collection("live_board")
                        .document("state")
                        .set(payload, SetOptions.merge())

                    mainDb.collection("classrooms")
                        .document(classId)
                        .collection("live_board")
                        .document("state")
                        .set(payload, SetOptions.merge())
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error updating screen mirror pause state: ${e.message}")
        }
    }

    /**
     * Cihazdan seçilen dosyayı (PDF, görsel, video, ses, sunum, program vb.) Akıllı Tahtada yayınlar.
     * Küçük dosyaları doğrudan, büyük dosyaları 300 KB parçalar halinde Firestore üzerinden iletir.
     */
    suspend fun castFileToBoard(
        context: Context,
        classId: String,
        sessionCode: String,
        uri: Uri,
        onProgress: (Float) -> Unit = {}
    ): Result<String> {
        return try {
            val cleanCode = normalizeCode(sessionCode)
            if (cleanCode.length < 4) {
                return Result.failure(IllegalStateException("Önce Akıllı Tahta bağlantısı kurun."))
            }

            val fileName = resolveFileName(context, uri)
            val initialMime = resolveMimeType(context, uri, fileName)

            val rawRead = context.contentResolver.openInputStream(uri)?.use { input ->
                val maxPreReadBytes = 25 * 1024 * 1024
                val buffer = ByteArray(16 * 1024)
                val out = ByteArrayOutputStream()
                var total = 0
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    total += read
                    if (total > maxPreReadBytes) {
                        return Result.failure(
                            IllegalArgumentException("Seçilen dosya çok büyük (en fazla 12 MB desteklenir). Büyük videoları URL ile yayınlayabilirsiniz.")
                        )
                    }
                    out.write(buffer, 0, read)
                }
                out.toByteArray()
            } ?: return Result.failure(IllegalStateException("Seçilen dosya okunamadı."))

            val (finalBytes, finalMime) = optimizeLargeImageIfPossible(rawRead, initialMime)
            val maxFinalBytes = 12 * 1024 * 1024
            if (finalBytes.size > maxFinalBytes) {
                return Result.failure(
                    IllegalArgumentException("Dosya boyutu (${finalBytes.size / (1024 * 1024)} MB) 12 MB sınırını aşıyor. Büyük dosyalar için URL sekmesini kullanın.")
                )
            }

            val now = System.currentTimeMillis()
            val castId = "cast_$now"
            val inlineThreshold = 320 * 1024

            if (finalBytes.size <= inlineThreshold) {
                onProgress(0.6f)
                val b64 = Base64.encodeToString(finalBytes, Base64.NO_WRAP)
                val castMap = hashMapOf<String, Any?>(
                    "active" to true,
                    "castId" to castId,
                    "type" to "FILE",
                    "title" to fileName,
                    "fileName" to fileName,
                    "mimeType" to finalMime,
                    "fileSize" to finalBytes.size.toLong(),
                    "inlineBase64" to b64,
                    "totalChunks" to 0,
                    "updatedAt" to now
                )
                publishBoardCastMap(context, classId, cleanCode, castMap)
                onProgress(1f)
            } else {
                val chunkSize = 300 * 1024
                val totalChunks = (finalBytes.size + chunkSize - 1) / chunkSize
                val mainDb = getMainFirestore(context)
                val chunksCol = mainDb.collection("board_sessions")
                    .document(cleanCode)
                    .collection("cast_chunks")

                for (i in 0 until totalChunks) {
                    val start = i * chunkSize
                    val end = minOf(finalBytes.size, start + chunkSize)
                    val slice = finalBytes.copyOfRange(start, end)
                    val b64Chunk = Base64.encodeToString(slice, Base64.NO_WRAP)
                    chunksCol.document("${castId}_$i")
                        .set(
                            mapOf(
                                "castId" to castId,
                                "chunkIndex" to i,
                                "totalChunks" to totalChunks,
                                "data" to b64Chunk,
                                "updatedAt" to now
                            )
                        )
                        .await()
                    onProgress((i + 1).toFloat() / (totalChunks + 1).toFloat())
                }

                val castMap = hashMapOf<String, Any?>(
                    "active" to true,
                    "castId" to castId,
                    "type" to "FILE",
                    "title" to fileName,
                    "fileName" to fileName,
                    "mimeType" to finalMime,
                    "fileSize" to finalBytes.size.toLong(),
                    "inlineBase64" to "",
                    "totalChunks" to totalChunks,
                    "updatedAt" to now
                )
                publishBoardCastMap(context, classId, cleanCode, castMap)
                onProgress(1f)
            }

            Result.success(fileName)
        } catch (e: Exception) {
            Log.e(TAG, "Error casting file to smart board", e)
            Result.failure(e)
        }
    }

    /**
     * Web adresi (URL / YouTube / PDF linki) veya eğitim programı bağlantısını Akıllı Tahtada yayınlar.
     */
    suspend fun castUrlOrProgramToBoard(
        context: Context,
        classId: String,
        sessionCode: String,
        type: String,
        rawUrl: String,
        title: String = ""
    ): Result<String> {
        return try {
            val cleanCode = normalizeCode(sessionCode)
            if (cleanCode.length < 4) {
                return Result.failure(IllegalStateException("Önce Akıllı Tahta bağlantısı kurun."))
            }
            val trimmed = rawUrl.trim()
            if (trimmed.isBlank()) {
                return Result.failure(IllegalArgumentException("Lütfen geçerli bir URL veya program adresi girin."))
            }
            val lower = trimmed.lowercase(Locale.ROOT)
            if (lower.startsWith("javascript:") || lower.startsWith("vbscript:")) {
                return Result.failure(IllegalArgumentException("Güvenlik nedeniyle bu adres desteklenmiyor."))
            }
            val hasScheme = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:").containsMatchIn(trimmed)
            val normalizedUrl = if (hasScheme) trimmed else "https://$trimmed"
            val parsedHost = try {
                Uri.parse(normalizedUrl).host?.removePrefix("www.")
            } catch (_: Exception) { null }
            val effectiveTitle = title.trim().ifBlank { parsedHost ?: normalizedUrl }
            val now = System.currentTimeMillis()

            val castMap = hashMapOf<String, Any?>(
                "active" to true,
                "castId" to "cast_$now",
                "type" to if (type == "PROGRAM") "PROGRAM" else "URL",
                "title" to effectiveTitle,
                "url" to normalizedUrl,
                "updatedAt" to now
            )
            publishBoardCastMap(context, classId, cleanCode, castMap)
            Result.success(effectiveTitle)
        } catch (e: Exception) {
            Log.e(TAG, "Error casting URL/Program to smart board", e)
            Result.failure(e)
        }
    }

    /**
     * Tahtadaki dosya/URL/program/ekran yayınını durdurur ve tahtayı tekrar BİRİZ uygulama menülerine döndürür.
     */
    fun stopBoardCast(context: Context, classId: String, sessionCode: String?) {
        lastBoardCast = null
        val cleanCode = sessionCode?.let { normalizeCode(it) }?.ifEmpty { null } ?: return
        if (activeSessionCode != cleanCode) return
        val expectedEpoch = connectionEpoch
        val now = System.currentTimeMillis()
        val restoreSubPage = lastCurrentSubPage.ifBlank { "SINIF_LISTESI" }
        val restoreSubPageTitle = lastCurrentSubPageTitle.ifBlank { "Sınıf Listesi" }
        val stopMap = mapOf(
            "active" to false,
            "castId" to "",
            "isPaused" to false,
            "inlineBase64" to "",
            "updatedAt" to now
        )

        scope.launch {
            sessionWriteMutex.withLock {
                if (!isEpochStillActive(cleanCode, expectedEpoch)) return@withLock
                try {
                    val mainDb = getMainFirestore(context)
                    val payload = mapOf(
                        "boardCast" to stopMap,
                        "activeModule" to restoreSubPage,
                        "activeModuleTitle" to restoreSubPageTitle,
                        "currentSubPage" to restoreSubPage,
                        "currentSubPageTitle" to restoreSubPageTitle,
                        "updatedAt" to now,
                        "serverTimestamp" to FieldValue.serverTimestamp()
                    )
                    mainDb.collection("board_sessions")
                        .document(cleanCode)
                        .set(payload, SetOptions.merge())

                    if (classId.isNotBlank() && classId != "default") {
                        mainDb.collection("classes")
                            .document(classId)
                            .collection("live_board")
                            .document("state")
                            .set(payload, SetOptions.merge())

                        mainDb.collection("classrooms")
                            .document(classId)
                            .collection("live_board")
                            .document("state")
                            .set(payload, SetOptions.merge())
                    }

                    try {
                        val defaultDb = FirebaseFirestore.getInstance()
                        if (defaultDb != mainDb) {
                            defaultDb.collection("board_sessions")
                                .document(cleanCode)
                                .set(payload, SetOptions.merge())
                        }
                    } catch (_: Exception) {}
                } catch (e: Exception) {
                    Log.w(TAG, "Error stopping board cast: ${e.message}")
                }
            }
        }
    }

    /**
     * Çıkış yapıldığında veya hesap silindiğinde bellek içi tahta önbelleğini temizler.
     */
    fun resetLocalState(context: Context? = null) {
        connectionEpoch += 1L
        activeSessionCode = null
        activeConnectionId = null
        lastClassId = ""
        lastCurrentSubPage = "SINIF_LISTESI"
        lastCurrentSubPageTitle = "Sınıf Listesi"
        lastActiveDay = "PZT"
        lastStudents = emptyList()
        lastSchedule = emptyList()
        lastScheduleConfig = ScheduleConfig()
        lastSeatingPlanState = null
        lastGroupMakerState = null
        lastBoardCast = null
        if (context != null) {
            try {
                saveConnectionState(context, null, false)
            } catch (_: Exception) {}
        }
    }

    /**
     * Hesap silinirken kullanıcıya/sınıflarına ait tüm Akıllı Tahta oturumlarını
     * ve `cast_chunks` alt koleksiyonlarını Firestore'dan temizler.
     */
    suspend fun deleteUserSmartBoardTraces(
        context: Context,
        ownedClassIds: Set<String>,
        teacherEmail: String,
        activeSessionCode: String?
    ) {
        val mainDb = getMainFirestore(context)
        val defaultDb = try {
            FirebaseFirestore.getInstance()
        } catch (_: Exception) {
            null
        }
        val dbs = listOfNotNull(mainDb, defaultDb?.takeIf { it != mainDb })

        val sessionIdsToDelete = mutableSetOf<String>()
        activeSessionCode?.let { normalizeCode(it) }?.takeIf { it.isNotBlank() }?.let { sessionIdsToDelete.add(it) }
        try {
            getSavedSessionCode(context)?.let { normalizeCode(it) }?.takeIf { it.isNotBlank() }?.let { sessionIdsToDelete.add(it) }
        } catch (_: Exception) {}

        val cleanEmail = teacherEmail.trim()
        for (db in dbs) {
            try {
                ownedClassIds.filter { it.isNotBlank() && it != "default" }.forEach { cid ->
                    try {
                        val byClass = db.collection("board_sessions")
                            .whereEqualTo("classId", cid)
                            .get()
                            .await()
                        byClass.documents.forEach { sessionIdsToDelete.add(it.id) }
                    } catch (_: Exception) {}
                }
                if (cleanEmail.isNotBlank()) {
                    try {
                        val byEmail = db.collection("board_sessions")
                            .whereEqualTo("teacherEmail", cleanEmail)
                            .get()
                            .await()
                        byEmail.documents.forEach { sessionIdsToDelete.add(it.id) }
                    } catch (_: Exception) {}
                }

                for (sid in sessionIdsToDelete) {
                    try {
                        val sessionRef = db.collection("board_sessions").document(sid)
                        val chunksSnap = sessionRef.collection("cast_chunks").get().await()
                        chunksSnap.documents.chunked(400).forEach { chunk ->
                            val batch = db.batch()
                            chunk.forEach { batch.delete(it.reference) }
                            batch.commit().await()
                        }
                        sessionRef.delete().await()
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }
        resetLocalState(context)
    }

    const val SMART_BOARD_WEB_URL = "https://cihanogretmen.com/tahta"

    /**
     * Akıllı Tahta Web Dosyası (`tahta.html`) HTML İçeriğini Döndürür.
     */
    fun getTahtaHtmlContent(context: Context): String {
        val loaded = try {
            context.assets.open("tahta.html").bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (e: Exception) {
            Log.w(TAG, "Could not read tahta.html from assets, trying file fallback: ${e.message}")
            try {
                val f = File("public/tahta.html")
                if (f.exists()) f.readText(Charsets.UTF_8) else ""
            } catch (ignored: Exception) {
                ""
            }
        }

        if (loaded.isNotBlank() && loaded.length > 500) {
            return loaded
        }

        // Failsafe acil durum HTML şablonu (Asla boş dosya oluşmasın)
        return """
            <!DOCTYPE html>
            <html lang="tr">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>BİRİZ - Akıllı Tahta Canlı Ekranı</title>
              <script src="https://www.gstatic.com/firebasejs/10.12.0/firebase-app-compat.js"></script>
              <script src="https://www.gstatic.com/firebasejs/10.12.0/firebase-firestore-compat.js"></script>
              <style>
                body { font-family: system-ui, sans-serif; background: #F8FAFC; color: #0F172A; text-align: center; padding: 40px; }
                .card { background: white; border-radius: 24px; padding: 40px; max-width: 600px; margin: 0 auto; box-shadow: 0 10px 30px rgba(0,0,0,0.08); }
                .code { font-size: 3rem; font-weight: 800; color: #1B2A4A; background: #EFF6FF; padding: 16px; border-radius: 16px; letter-spacing: 4px; margin: 20px 0; }
              </style>
            </head>
            <body>
              <div class="card">
                <h1>🖥️ BİRİZ Akıllı Tahta</h1>
                <p>Telefondaki kod giriş alanına aşağıdaki 8 haneli kodu giriniz:</p>
                <div class="code" id="code">--------</div>
              </div>
              <script>
                const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
                let c = "";
                for (let i = 0; i < 8; i++) c += chars.charAt(Math.floor(Math.random() * chars.length));
                document.getElementById("code").innerText = c;
              </script>
            </body>
            </html>
        """.trimIndent()
    }

    fun copySmartBoardUrl(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clipboard != null) {
            val clip = ClipData.newPlainText("Akıllı Tahta Adresi", SMART_BOARD_WEB_URL)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "🔗 Web adresi panoya kopyalandı:\n$SMART_BOARD_WEB_URL", Toast.LENGTH_SHORT).show()
        }
    }

    fun openSmartBoardUrl(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(SMART_BOARD_WEB_URL)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            copySmartBoardUrl(context)
        }
    }

    /**
     * Akıllı Tahta web dosyasını (`tahta.html`) oluşturup dışa aktarır / paylaşır.
     */
    fun downloadOrShareTahtaHtml(context: Context) {
        try {
            val content = getTahtaHtmlContent(context)

            // 1. Dosyayı yerel cache klasörüne kaydet
            val file = File(context.cacheDir, "tahta.html")
            file.writeText(content, Charsets.UTF_8)

            // 2. Kodu panoya da kopyala (Kullanıcı dilerse doğrudan yapıştırabilsin)
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (clipboard != null) {
                val clip = ClipData.newPlainText("tahta.html", content)
                clipboard.setPrimaryClip(clip)
            }

            // 3. Android Share Intent ile paylaş / bilgisayara aktar
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/html"
                putExtra(Intent.EXTRA_SUBJECT, "BİRİZ - Akıllı Tahta Canlı Ekran Dosyası (tahta.html)")
                putExtra(Intent.EXTRA_TEXT, content)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(shareIntent, "Akıllı Tahta Dosyasını (tahta.html) İndir / Paylaş").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)

            Toast.makeText(context, "📥 tahta.html (${file.length() / 1024} KB) hazırlandı ve panoya kopyalandı!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading or sharing tahta.html", e)
            Toast.makeText(context, "Dosya hazırlanırken hata: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
