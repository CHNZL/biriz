package com.example.util

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.model.Announcement
import com.example.data.model.BirizNotification
import com.example.data.model.Book
import com.example.data.model.ChatMessage
import com.example.data.model.GroupMakerState
import com.example.data.model.MessageStatus
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
import com.example.data.model.UserRole
import com.example.data.repository.ClassRegistryRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Uygulama kapalıyken dahi WorkManager üzerinden dönemsel bulut taraması yapan,
 * Sessiz Saatler (Gece Koruması) sonrası sabah özeti çıkaran ve
 * üretilen bildirimleri akıllı gruplama ile sisteme ileten arka plan yöneticisi.
 */
object BackgroundNotificationSyncHelper {

    private const val TAG = "BirizBgNotifSync"
    private const val PERIODIC_WORK_NAME = "biriz_bg_notification_sync"
    private const val QUIET_FLUSH_WORK_NAME = "biriz_quiet_hours_digest_flush"
    private const val PERIODIC_INTERVAL_MINUTES = 60L
    internal const val INPUT_VIEWER_KEY = "input_viewer_key"

    fun schedulePeriodicSync(context: Context) {
        val appCtx = context.applicationContext ?: context
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<BirizBackgroundNotificationWorker>(
            PERIODIC_INTERVAL_MINUTES, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 2, TimeUnit.MINUTES)
            .build()

        try {
            WorkManager.getInstance(appCtx).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to schedule periodic notification sync", e)
        }
    }

    fun scheduleQuietHoursFlush(
        context: Context,
        config: QuietHoursConfig,
        viewerKey: String,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        if (!config.enabled) {
            cancelQuietHoursFlush(context)
            return
        }
        val targetAt = NotificationPreferencesHelper.nextQuietHoursEndMillis(config, nowMillis)
        val delayMs = (targetAt - nowMillis).coerceAtLeast(10_000L)
        val appCtx = context.applicationContext ?: context
        val inputData = Data.Builder()
            .putString(INPUT_VIEWER_KEY, viewerKey.ifBlank { "default" })
            .build()
        val request = OneTimeWorkRequestBuilder<BirizQuietHoursFlushWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .build()

        try {
            WorkManager.getInstance(appCtx).enqueueUniqueWork(
                QUIET_FLUSH_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to schedule quiet hours digest flush", e)
        }
    }

    fun cancelQuietHoursFlush(context: Context) {
        val appCtx = context.applicationContext ?: context
        try {
            WorkManager.getInstance(appCtx).cancelUniqueWork(QUIET_FLUSH_WORK_NAME)
        } catch (_: Exception) {
        }
    }

    fun cancelAllBackgroundSync(context: Context) {
        val appCtx = context.applicationContext ?: context
        try {
            val wm = WorkManager.getInstance(appCtx)
            wm.cancelUniqueWork(PERIODIC_WORK_NAME)
            wm.cancelUniqueWork(QUIET_FLUSH_WORK_NAME)
        } catch (_: Exception) {
        }
    }

    fun flushQuietHoursDigestIfDue(
        context: Context,
        viewerKey: String,
        forceFlushEvenIfWithinWindow: Boolean = false,
        nowMillis: Long = System.currentTimeMillis()
    ): Int {
        val appCtx = context.applicationContext ?: context
        val scope = viewerKey.ifBlank { "default" }
        val quietConfig = NotificationPreferencesHelper.loadQuietHours(appCtx, scope)
        if (!forceFlushEvenIfWithinWindow && NotificationPreferencesHelper.isWithinQuietHours(quietConfig, nowMillis)) {
            return 0
        }
        val deferred = NotificationPreferencesHelper.drainDeferredNotifications(appCtx, scope)
        if (deferred.isEmpty()) return 0

        val isTeacher = deferred.any { it.isRecipientTeacher }
        SystemActivityNotificationHelper.showNotificationsBatch(
            context = appCtx,
            notifications = deferred.map { it.notification },
            isRecipientTeacher = isTeacher,
            isMorningDigest = true
        )
        return deferred.size
    }

    suspend fun runSyncOnce(context: Context, allowWhenForeground: Boolean = false) {
        val appCtx = context.applicationContext ?: context
        if (!allowWhenForeground && AppUpdateDownloadController.isAppInForeground) {
            return
        }

        val fbUser = Firebase.auth.currentUser ?: return
        val appPrefs = appCtx.getSharedPreferences("biriz_app_settings", Context.MODE_PRIVATE)
        val savedRoleCode = appPrefs.getString("key_saved_role", "")?.trim().orEmpty()
        val classId = appPrefs.getString("key_linked_classroom_id", "")?.trim().orEmpty()
        if (classId.isBlank() || classId == "default") return

        val role = UserRole.fromCode(savedRoleCode)
        if (role != UserRole.TEACHER && role != UserRole.PARENT) return

        val userEmail = (appPrefs.getString("key_user_email", "")?.trim().orEmpty())
            .ifBlank { fbUser.email?.trim().orEmpty() }
        val linkedIds = appPrefs.getString("key_linked_student_ids", "")
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.toSet()
            .orEmpty()
        if (role == UserRole.PARENT && linkedIds.isEmpty()) return

        val viewerKey = when (role) {
            UserRole.TEACHER -> "teacher"
            UserRole.PARENT -> ChatVisibilityHelper.viewerDocumentKey(
                viewerIsTeacher = false,
                userEmail = userEmail
            )
            else -> return
        }

        // Sessiz saatler sona erdiyse gece biriken bildirimleri önce teslim et
        val nowMs = System.currentTimeMillis()
        flushQuietHoursDigestIfDue(appCtx, viewerKey, nowMillis = nowMs)

        // Gece 00:00 - 06:59 arasında arka plan Firestore sorgulaması yapma (kota ve pil tasarrufu)
        if (!allowWhenForeground) {
            val hour = Calendar.getInstance().apply { timeInMillis = nowMs }.get(Calendar.HOUR_OF_DAY)
            if (hour in 0..6) {
                return
            }
        }

        val db = try {
            FirestoreProvider.get(appCtx)
        } catch (_: Exception) {
            return
        }

        val notificationPrefs = NotificationPreferencesHelper.loadPreferences(appCtx, viewerKey)
        val quietConfig = NotificationPreferencesHelper.loadQuietHours(appCtx, viewerKey)
        val (baselineMs, seenKeysInitial) = CommunicationPersistenceHelper.loadActivityNotificationState(
            context = appCtx,
            classId = classId,
            viewerKey = viewerKey
        )
        var updatedSeenKeys = seenKeysInitial

        val students = if (role == UserRole.PARENT && linkedIds.isNotEmpty()) {
            fetchLinkedStudentsOnly(db, classId, linkedIds)
        } else {
            fetchStudents(db, classId)
        }
        val announcements = fetchAnnouncements(db, classId)
        val clearDocKey = ChatVisibilityHelper.viewerDocumentKey(
            viewerIsTeacher = role == UserRole.TEACHER,
            userEmail = userEmail
        )
        val clearMap = CommunicationPersistenceHelper.loadChatClearTimestamps(appCtx, classId, clearDocKey)
        val chatMessages = fetchChatMessages(db, classId)

        val generatedNotifications = mutableListOf<BirizNotification>()

        if (role == UserRole.TEACHER) {
            val books = fetchBorrowedBooks(db, classId)
            if (students.isNotEmpty()) {
                val teacherRes = ActivityNotificationHelper.evaluateTeacherNotifications(
                    previousStudents = null,
                    currentStudents = students,
                    seenEventKeys = updatedSeenKeys,
                    baselineTimestampMs = baselineMs,
                    nowMs = nowMs
                )
                generatedNotifications.addAll(teacherRes.newNotifications)
                updatedSeenKeys = teacherRes.updatedSeenKeys
            }
            val opRes = ActivityNotificationHelper.evaluateTeacherOperationalNotifications(
                previousAnnouncements = null,
                currentAnnouncements = announcements,
                books = books,
                seenEventKeys = updatedSeenKeys,
                baselineTimestampMs = baselineMs,
                nowMs = nowMs
            )
            generatedNotifications.addAll(opRes.newNotifications)
            updatedSeenKeys = opRes.updatedSeenKeys
        } else {
            val books = fetchBorrowedBooks(db, classId)
            val stars = fetchStarsForStudents(db, classId, linkedIds)
            val readingRecords = fetchReadingRecordsForStudents(db, classId, linkedIds)
            val tournaments = fetchTournaments(db, classId)
            val matchesByTournament = mutableMapOf<String, List<TournamentMatch>>()
            val tournamentsNeedingMatches = tournaments.filter { t ->
                val isOngoing = t.status == TournamentStatus.DEVAM_EDIYOR
                val needsFinishNotif = t.status == TournamentStatus.TAMAMLANDI &&
                    linkedIds.any { sId -> sId in t.participants && "tourney_finish_${t.id}_$sId" !in updatedSeenKeys }
                isOngoing || needsFinishNotif
            }.sortedByDescending { it.updatedAt }.take(4)
            tournamentsNeedingMatches.forEach { t ->
                matchesByTournament[t.id] = fetchTournamentMatches(db, t.id)
            }
            val (schedConfig, schedLessons, schedUpdatedAt) = fetchSchedule(db, classId)
            val (seatingState, seatingUpdatedAt) = fetchSeating(db, classId)
            val (groupState, groupsUpdatedAt) = fetchGroups(db, classId)

            val snapshot = ActivityNotificationHelper.ActivitySnapshot(
                announcements = announcements,
                starRecords = stars,
                books = books,
                readingRecords = readingRecords,
                tournaments = tournaments,
                matchesByTournament = matchesByTournament,
                students = students,
                scheduleConfig = schedConfig,
                scheduleLessons = schedLessons,
                scheduleUpdatedAtMs = schedUpdatedAt,
                seatingPlanState = seatingState,
                seatingUpdatedAtMs = seatingUpdatedAt,
                groupMakerState = groupState,
                groupsUpdatedAtMs = groupsUpdatedAt,
                hasAnnouncements = true,
                hasStars = true,
                hasBooks = true,
                hasReadingRecords = true,
                hasTournaments = true,
                hasSchedule = schedConfig != null || schedLessons.isNotEmpty(),
                hasSeating = seatingState != null,
                hasGroups = groupState != null
            )

            val parentRes = ActivityNotificationHelper.evaluateParentNotifications(
                previous = null,
                current = snapshot,
                linkedStudentIds = linkedIds,
                seenEventKeys = updatedSeenKeys,
                baselineTimestampMs = baselineMs,
                nowMs = nowMs,
                currentUserEmail = userEmail
            )
            generatedNotifications.addAll(parentRes.newNotifications)
            updatedSeenKeys = parentRes.updatedSeenKeys
        }

        if (updatedSeenKeys != seenKeysInitial) {
            CommunicationPersistenceHelper.saveActivityNotificationState(
                context = appCtx,
                classId = classId,
                viewerKey = viewerKey,
                baselineMs = baselineMs,
                seenKeys = updatedSeenKeys
            )
        }

        // Kapalı uygulama sırasında gelen okunmamış sohbet mesajlarını kontrol et
        val isTeacher = role == UserRole.TEACHER
        val visibleMessages = ChatVisibilityHelper.filterVisibleMessages(
            messages = chatMessages,
            viewerIsTeacher = isTeacher,
            viewerIsParent = !isTeacher,
            currentParentEmail = userEmail,
            clearTimestamps = clearMap,
            nowMillis = nowMs
        ).filter { msg ->
            isTeacher || linkedIds.isEmpty() || msg.studentId in linkedIds
        }

        val processedMsgIds = CommunicationPersistenceHelper.loadNotifiedMessageIds(appCtx, classId).toMutableSet()
        val incomingUnread = visibleMessages.filter { msg ->
            msg.status != MessageStatus.READ &&
                !msg.isDeletedForEveryone &&
                msg.id !in processedMsgIds &&
                NotificationLifecycleHelper.isIncomingMessageForViewer(
                    msg = msg,
                    viewerIsTeacher = isTeacher,
                    viewerIsParent = !isTeacher,
                    linkedParentStudentIds = linkedIds.toList(),
                    currentUserEmail = userEmail,
                    nowMillis = nowMs
                )
        }

        val chatAlertPairs = mutableListOf<Pair<ChatMessage, BirizNotification>>()
        if (incomingUnread.isNotEmpty()) {
            processedMsgIds.addAll(incomingUnread.map { it.id })
            CommunicationPersistenceHelper.saveNotifiedMessageIds(appCtx, classId, processedMsgIds)

            if (NotificationPreferencesHelper.isCategoryEnabled(notificationPrefs, NotificationCategory.MESSAGES)) {
                incomingUnread.groupBy { it.studentId }.forEach { (studentId, msgs) ->
                    val latestMsg = msgs.maxByOrNull { it.timestamp } ?: return@forEach
                    val totalUnread = visibleMessages.count {
                        it.studentId == studentId &&
                            it.status != MessageStatus.READ &&
                            !it.isDeletedForEveryone &&
                            NotificationLifecycleHelper.isIncomingMessageForViewer(
                                msg = it,
                                viewerIsTeacher = isTeacher,
                                viewerIsParent = !isTeacher,
                                linkedParentStudentIds = linkedIds.toList(),
                                currentUserEmail = userEmail,
                                nowMillis = nowMs
                            )
                    }.coerceAtLeast(msgs.size)
                    val notifTitle = if (isTeacher) {
                        if (totalUnread > 1) "Yeni Veli Mesajları ($totalUnread) 💬" else "Yeni Veli Mesajı 💬"
                    } else {
                        if (totalUnread > 1) "Öğretmenden Yeni Mesajlar ($totalUnread) 💬" else "Öğretmenden Yeni Mesaj 💬"
                    }
                    val notifBody = if (isTeacher) {
                        "${latestMsg.parentName} (${latestMsg.studentNumber} ${latestMsg.studentName}): ${latestMsg.text}"
                    } else {
                        "${latestMsg.senderName}: ${latestMsg.text}"
                    }
                    val chatNotif = BirizNotification(
                        id = "msg_notif_$studentId",
                        title = notifTitle,
                        message = notifBody,
                        timestamp = latestMsg.timestamp,
                        type = NotificationType.MESSAGE,
                        targetSubPageName = if (isTeacher) "ILETISIM_MESAJLAR" else "COMMUNICATION",
                        targetId = studentId,
                        isRead = false
                    )
                    chatAlertPairs.add(latestMsg to chatNotif)
                }
            }
        }

        val allowedActivityNew = generatedNotifications.filter {
            NotificationPreferencesHelper.isNotificationEnabled(notificationPrefs, it)
        }
        val existingStored = CommunicationPersistenceHelper.loadNotifications(appCtx, classId, viewerKey)
        val existingIds = existingStored.map { it.id }.toSet()
        val uniqueActivityNew = allowedActivityNew.filter { it.id !in existingIds }

        val allNewBellNotifs = (chatAlertPairs.map { it.second } + uniqueActivityNew)
        if (allNewBellNotifs.isNotEmpty()) {
            val chatStudentIdsUpdated = chatAlertPairs.mapNotNull { it.second.targetId }.toSet()
            val cleanedExisting = existingStored.filterNot { prev ->
                prev.type == NotificationType.MESSAGE &&
                    !prev.id.startsWith("msg_react_") &&
                    !prev.id.startsWith("sched_confirm_") &&
                    prev.targetId in chatStudentIdsUpdated
            }
            val mergedStored = (allNewBellNotifs + cleanedExisting)
                .distinctBy { it.id }
                .sortedByDescending { it.timestamp }
                .take(150)
            CommunicationPersistenceHelper.saveNotifications(appCtx, classId, mergedStored, viewerKey)
        }

        // Sessiz Saatler kontrolü ile sistem bildirim çubuğuna iletim
        val toDefer = mutableListOf<DeferredSystemNotification>()
        val immediateActivities = mutableListOf<BirizNotification>()
        val immediateChats = mutableListOf<Pair<ChatMessage, Int>>()

        uniqueActivityNew.forEach { notif ->
            if (NotificationPreferencesHelper.shouldDeferForQuietHours(quietConfig, notif, nowMs)) {
                toDefer.add(DeferredSystemNotification(notif, isTeacher))
            } else {
                immediateActivities.add(notif)
            }
        }

        chatAlertPairs.forEach { (msg, chatNotif) ->
            if (NotificationPreferencesHelper.shouldDeferForQuietHours(quietConfig, chatNotif, nowMs)) {
                toDefer.add(DeferredSystemNotification(chatNotif, isTeacher))
            } else {
                val countForStudent = incomingUnread.count { it.studentId == msg.studentId }.coerceAtLeast(1)
                immediateChats.add(msg to countForStudent)
            }
        }

        if (toDefer.isNotEmpty()) {
            NotificationPreferencesHelper.enqueueDeferredNotifications(appCtx, viewerKey, toDefer)
            scheduleQuietHoursFlush(appCtx, quietConfig, viewerKey, nowMs)
        }
        if (immediateActivities.isNotEmpty()) {
            SystemActivityNotificationHelper.showNotificationsBatch(
                context = appCtx,
                notifications = immediateActivities,
                isRecipientTeacher = isTeacher
            )
        }
        immediateChats.forEach { (msg, count) ->
            ChatNotificationHelper.showIncomingMessageNotification(
                context = appCtx,
                message = msg,
                isRecipientTeacher = isTeacher,
                unreadCountForStudent = count
            )
        }
    }

    private suspend fun fetchStudents(db: FirebaseFirestore, classId: String): List<Student> {
        return try {
            val snap = db.collection("classrooms").document(classId).collection("students").get().await()
            snap.documents.map { doc ->
                ClassRegistryRepository.parseStudentFromDoc(doc)
            }.sortedBy { it.studentNumber.toIntOrNull() ?: 9999 }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun fetchLinkedStudentsOnly(
        db: FirebaseFirestore,
        classId: String,
        linkedIds: Set<String>
    ): List<Student> {
        if (linkedIds.isEmpty()) return emptyList()
        return try {
            val studentsCol = db.collection("classrooms").document(classId).collection("students")
            linkedIds.mapNotNull { sId ->
                try {
                    val doc = studentsCol.document(sId).get().await()
                    if (doc.exists()) ClassRegistryRepository.parseStudentFromDoc(doc) else null
                } catch (_: Exception) {
                    null
                }
            }.sortedBy { it.studentNumber.toIntOrNull() ?: 9999 }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun fetchAnnouncements(db: FirebaseFirestore, classId: String): List<Announcement> {
        return try {
            val snap = db.collection("classrooms").document(classId).collection("announcements")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(15L)
                .get()
                .await()
            snap.documents.mapNotNull { doc ->
                val raw = doc.getString("json") ?: return@mapNotNull null
                try {
                    val ann = CommunicationPersistenceHelper.parseAnnouncementJson(JSONObject(raw))
                    if (ann.id == "ann_1" || ann.id == "ann_2") null else ann
                } catch (_: Exception) {
                    null
                }
            }.sortedByDescending { it.createdAt }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun fetchChatMessages(db: FirebaseFirestore, classId: String): List<ChatMessage> {
        return try {
            val snap = db.collection("classrooms").document(classId).collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(40L)
                .get()
                .await()
            snap.documents.mapNotNull { doc ->
                val raw = doc.getString("json") ?: return@mapNotNull null
                try {
                    val msg = CommunicationPersistenceHelper.parseChatMessageJson(JSONObject(raw))
                    val serverStatus = doc.getString("status")?.let {
                        try { MessageStatus.valueOf(it) } catch (_: Exception) { null }
                    }
                    val serverDeliveredAt = doc.getLong("deliveredAt")
                    val serverReadAt = doc.getLong("readAt")
                    val mergedStatus = when {
                        serverStatus == MessageStatus.READ || msg.status == MessageStatus.READ -> MessageStatus.READ
                        serverStatus == MessageStatus.DELIVERED || msg.status == MessageStatus.DELIVERED -> MessageStatus.DELIVERED
                        else -> msg.status
                    }
                    msg.copy(
                        status = mergedStatus,
                        deliveredAt = serverDeliveredAt ?: msg.deliveredAt,
                        readAt = serverReadAt ?: msg.readAt
                    )
                } catch (_: Exception) {
                    null
                }
            }.filterNot { CommunicationPersistenceHelper.isSampleChatMessage(it) }
                .sortedBy { it.timestamp }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun fetchBorrowedBooks(db: FirebaseFirestore, classId: String): List<Book> {
        return try {
            val snap = db.collection("classrooms").document(classId).collection("library_books")
                .whereEqualTo("status", "Okunuyor")
                .get()
                .await()
            snap.documents.mapNotNull { doc ->
                val raw = doc.getString("json")
                if (!raw.isNullOrBlank()) {
                    try {
                        LibraryPersistenceHelper.deserializeBook(JSONObject(raw), fallbackId = doc.id)
                    } catch (_: Exception) {
                        null
                    }
                } else null
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun fetchStarsForStudents(
        db: FirebaseFirestore,
        classId: String,
        linkedIds: Set<String>
    ): List<StarRewardRecord> {
        val col = db.collection("classrooms").document(classId).collection("stars")
        return try {
            val docs = if (linkedIds.isNotEmpty()) {
                linkedIds.toList().chunked(10).flatMap { chunk ->
                    col.whereIn("studentId", chunk).get().await().documents
                }
            } else {
                col.orderBy("timestamp", Query.Direction.DESCENDING).limit(40L).get().await().documents
            }
            docs.mapNotNull { doc ->
                val sId = doc.getString("studentId")?.trim() ?: return@mapNotNull null
                val count = (doc.getLong("starCount") ?: 0L).toInt()
                if (sId.isBlank() || count == 0) return@mapNotNull null
                StarRewardRecord(
                    id = doc.id,
                    classId = classId,
                    studentId = sId,
                    studentName = doc.getString("studentName") ?: "",
                    studentNumber = doc.getString("studentNumber") ?: "",
                    categoryId = doc.getString("categoryId") ?: "",
                    categoryName = doc.getString("categoryName") ?: "",
                    starCount = count,
                    note = doc.getString("note") ?: "",
                    isLuckyBonus = doc.getBoolean("isLuckyBonus") ?: false,
                    timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                )
            }.sortedByDescending { it.timestamp }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun fetchReadingRecordsForStudents(
        db: FirebaseFirestore,
        classId: String,
        linkedIds: Set<String>
    ): List<ReadingRecord> {
        val col = db.collection("classrooms").document(classId).collection("reading_records")
        return try {
            val docs = if (linkedIds.isNotEmpty()) {
                linkedIds.toList().chunked(10).flatMap { chunk ->
                    col.whereIn("studentId", chunk).get().await().documents
                }
            } else {
                col.orderBy("endDate", Query.Direction.DESCENDING).limit(30L).get().await().documents
            }
            docs.mapNotNull { doc ->
                val raw = doc.getString("json") ?: return@mapNotNull null
                try {
                    LibraryPersistenceHelper.deserializeReadingRecord(JSONObject(raw), fallbackId = doc.id)
                } catch (_: Exception) {
                    null
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun fetchTournaments(db: FirebaseFirestore, classId: String): List<Tournament> {
        return try {
            val snap = db.collection("classrooms").document(classId).collection("tournaments")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(10L)
                .get()
                .await()
            snap.documents.mapNotNull { doc ->
                val raw = doc.getString("json") ?: return@mapNotNull null
                try {
                    TournamentPersistenceHelper.deserializeTournament(JSONObject(raw), classId)
                } catch (_: Exception) {
                    null
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun fetchTournamentMatches(db: FirebaseFirestore, tournamentId: String): List<TournamentMatch> {
        return try {
            val snap = db.collection("tournaments").document(tournamentId).collection("matches").get().await()
            snap.documents.mapNotNull { doc ->
                val raw = doc.getString("json") ?: return@mapNotNull null
                try {
                    TournamentPersistenceHelper.deserializeMatch(JSONObject(raw), tournamentId)
                } catch (_: Exception) {
                    null
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun fetchSchedule(
        db: FirebaseFirestore,
        classId: String
    ): Triple<ScheduleConfig?, List<ScheduleLesson>, Long> {
        return try {
            val doc = db.collection("classrooms").document(classId)
                .collection("schedule").document("current").get().await()
            if (!doc.exists()) return Triple(null, emptyList(), 0L)
            val updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
            val cfgStr = doc.getString("configJson")
            val cfg = if (!cfgStr.isNullOrBlank()) {
                val json = JSONObject(cfgStr)
                val recessArr = json.optJSONArray("recessDurations")
                val recessList = mutableListOf<Int>()
                if (recessArr != null) {
                    for (i in 0 until recessArr.length()) recessList.add(recessArr.optInt(i, 10))
                } else {
                    recessList.addAll(listOf(15, 15, 10, 15, 15))
                }
                ScheduleConfig(
                    dailyLessonCount = json.optInt("dailyLessonCount", 6),
                    firstLessonStartTime = json.optString("firstLessonStartTime", "08:30"),
                    lessonDurationMinutes = json.optInt("lessonDurationMinutes", 40),
                    recessDurations = recessList,
                    isLunchBreakActive = json.optBoolean("isLunchBreakActive", true),
                    lunchAfterLessonIndex = json.optInt("lunchAfterLessonIndex", 4),
                    lunchDurationMinutes = json.optInt("lunchDurationMinutes", 50),
                    isConfigured = true
                )
            } else null

            val lessonsStr = doc.getString("lessonsJson")
            val lessons = mutableListOf<ScheduleLesson>()
            if (!lessonsStr.isNullOrBlank()) {
                val arr = JSONArray(lessonsStr)
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    lessons.add(
                        ScheduleLesson(
                            dayOfWeek = obj.optString("dayOfWeek", "PZT"),
                            lessonIndex = obj.optInt("lessonIndex", 1),
                            subjectName = obj.optString("subjectName", ""),
                            topicOrNotes = obj.optString("topicOrNotes", "")
                        )
                    )
                }
            }
            Triple(cfg, lessons, updatedAt)
        } catch (_: Exception) {
            Triple(null, emptyList(), 0L)
        }
    }

    private suspend fun fetchSeating(db: FirebaseFirestore, classId: String): Pair<SeatingPlanState?, Long> {
        return try {
            val doc = db.collection("classrooms").document(classId)
                .collection("seating").document("current").get().await()
            if (!doc.exists()) return null to 0L
            val seatsJson = doc.getString("seatsJson") ?: return null to 0L
            val state = SeatingPlanPersistenceHelper.deserializePlanState(JSONObject(seatsJson))
            val updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
            state to updatedAt
        } catch (_: Exception) {
            null to 0L
        }
    }

    private suspend fun fetchGroups(db: FirebaseFirestore, classId: String): Pair<GroupMakerState?, Long> {
        return try {
            val doc = db.collection("classrooms").document(classId)
                .collection("groups").document("current").get().await()
            if (!doc.exists()) return null to 0L
            val groupsJson = doc.getString("groupsJson") ?: return null to 0L
            val state = GroupMakerPersistenceHelper.deserializeState(JSONObject(groupsJson))
            val updatedAt = doc.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
            state to updatedAt
        } catch (_: Exception) {
            null to 0L
        }
    }
}

class BirizBackgroundNotificationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return try {
            BackgroundNotificationSyncHelper.runSyncOnce(applicationContext)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}

class BirizQuietHoursFlushWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return try {
            val viewerKey = inputData.getString(BackgroundNotificationSyncHelper.INPUT_VIEWER_KEY)
                ?.takeIf { it.isNotBlank() }
                ?: resolveSavedViewerKey(applicationContext)
            BackgroundNotificationSyncHelper.flushQuietHoursDigestIfDue(
                context = applicationContext,
                viewerKey = viewerKey
            )
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun resolveSavedViewerKey(context: Context): String {
        val prefs = context.getSharedPreferences("biriz_app_settings", Context.MODE_PRIVATE)
        val roleCode = prefs.getString("key_saved_role", "") ?: ""
        return when (UserRole.fromCode(roleCode)) {
            UserRole.TEACHER -> "teacher"
            UserRole.PARENT -> {
                val email = prefs.getString("key_user_email", "").orEmpty()
                ChatVisibilityHelper.viewerDocumentKey(viewerIsTeacher = false, userEmail = email)
            }
            else -> "default"
        }
    }
}
