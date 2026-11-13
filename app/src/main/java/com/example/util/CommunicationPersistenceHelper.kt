package com.example.util

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.R
import com.example.data.model.Announcement
import com.example.data.model.AnnouncementPoll
import com.example.data.model.AnnouncementTargetType
import com.example.data.model.BirizNotification
import com.example.data.model.ChatMessage
import com.example.data.model.MessageStatus
import com.example.data.model.NotificationType
import com.example.data.model.PollOption
import com.example.data.model.Student
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CommunicationPersistenceHelper {

    private const val TAG = "CommunicationPersistence"
    private const val PREFS_NAME = "biriz_communication_storage"
    private const val KEY_ANNOUNCEMENTS_PREFIX = "announcements_"
    private const val KEY_CHAT_MESSAGES_PREFIX = "chat_messages_"
    private const val KEY_NOTIFICATIONS_PREFIX = "notifications_"
    private const val KEY_FONT_OFFSET = "key_communication_font_offset"
    private val syncScope = CoroutineScope(Dispatchers.IO)

    private fun getFirestore(context: Context): FirebaseFirestore? {
        return try {
            FirestoreProvider.get(context)
        } catch (_: Exception) {
            null
        }
    }

    private val timeFormat = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat = SimpleDateFormat("HH:mm", Locale("tr", "TR"))
    }
    private val dateFormat = object : ThreadLocal<SimpleDateFormat>() {
        override fun initialValue(): SimpleDateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("tr", "TR"))
    }

    fun loadFontOffset(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_FONT_OFFSET, 0)
    }

    fun saveFontOffset(context: Context, offset: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_FONT_OFFSET, offset).apply()
    }

    fun formatTime(timestamp: Long): String = (timeFormat.get() ?: SimpleDateFormat("HH:mm", Locale("tr", "TR"))).format(Date(timestamp))
    fun formatDateTime(timestamp: Long): String = (dateFormat.get() ?: SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("tr", "TR"))).format(Date(timestamp))

    /**
     * Sınıfa ait duyuruları yükler; sadece gerçek duyuruları getirir, sahte/örnek veri tohumlamaz.
     */
    fun loadAnnouncements(context: Context, classId: String, students: List<Student>): List<Announcement> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rawJson = (if (classId.isNotBlank()) prefs.getString(KEY_ANNOUNCEMENTS_PREFIX + classId, null) else null)
            ?: prefs.getString(KEY_ANNOUNCEMENTS_PREFIX + "default", null)

        if (!rawJson.isNullOrBlank()) {
            try {
                val list = mutableListOf<Announcement>()
                val jsonArray = JSONArray(rawJson)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val ann = parseAnnouncementJson(obj)
                    // Örnek / sahte tohum duyuruları filtrele
                    if (ann.id != "ann_1" && ann.id != "ann_2") {
                        list.add(ann)
                    }
                }
                return list
            } catch (_: Exception) {
            }
        }

        return emptyList()
    }

    /**
     * Sınıfa ait duyuruları yalnızca yerel önbelleğe kaydeder.
     */
    fun saveAnnouncementsLocalOnly(context: Context, classId: String, announcements: List<Announcement>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        for (announcement in announcements) {
            jsonArray.put(serializeAnnouncementJson(announcement))
        }
        val jsonStr = jsonArray.toString()
        val editor = prefs.edit()
        if (classId.isNotBlank()) {
            editor.putString(KEY_ANNOUNCEMENTS_PREFIX + classId, jsonStr)
        }
        editor.putString(KEY_ANNOUNCEMENTS_PREFIX + "default", jsonStr)
        editor.apply()
    }

    /**
     * Sınıfa ait duyuruları kaydeder ve buluta senkronize eder.
     */
    fun saveAnnouncements(context: Context, classId: String, announcements: List<Announcement>) {
        saveAnnouncementsLocalOnly(context, classId, announcements)
        for (announcement in announcements) {
            uploadAnnouncementToFirestore(context, classId, announcement)
        }
    }

    /**
     * Tek bir duyuruyu Firestore'a canlı yükler.
     */
    fun uploadAnnouncementToFirestore(context: Context, classId: String, announcement: Announcement) {
        if (classId.isBlank() || classId == "default") return
        val db = getFirestore(context) ?: return
        syncScope.launch {
            try {
                val json = serializeAnnouncementJson(announcement.copy(classId = classId)).toString()
                val data = mapOf(
                    "id" to announcement.id,
                    "classId" to classId,
                    "title" to announcement.title,
                    "createdAt" to announcement.createdAt,
                    "json" to json,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.collection("classrooms").document(classId)
                    .collection("announcements").document(announcement.id)
                    .set(data, SetOptions.merge())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload announcement to Firestore", e)
            }
        }
    }

    /**
     * Anket oyunu Firestore üzerinde atomik işlemle (mevcut oyları ezmeden ve tek oy kilidini koruyarak) kaydeder.
     */
    fun recordPollVoteInFirestore(
        context: Context,
        classId: String,
        announcementId: String,
        optionId: String,
        voterLabel: String,
        voterAliases: Set<String>,
        fallbackAnnouncement: Announcement
    ) {
        if (classId.isBlank() || classId == "default") return
        val db = getFirestore(context) ?: return
        val docRef = db.collection("classrooms").document(classId)
            .collection("announcements").document(announcementId)
        syncScope.launch {
            try {
                db.runTransaction { tx ->
                    val snap = tx.get(docRef)
                    val baseAnn = snap.getString("json")?.takeIf { it.isNotBlank() }?.let { raw ->
                        try {
                            parseAnnouncementJson(JSONObject(raw))
                        } catch (_: Exception) {
                            fallbackAnnouncement
                        }
                    } ?: fallbackAnnouncement

                    val currentPoll = baseAnn.poll ?: return@runTransaction null
                    val updatedPoll = AnnouncementIdentityHelper.applySingleVote(
                        poll = currentPoll,
                        optionId = optionId,
                        voterLabel = voterLabel,
                        voterAliases = voterAliases
                    ) ?: return@runTransaction null

                    val mergedAnn = baseAnn.copy(classId = classId, poll = updatedPoll)
                    val data = mapOf(
                        "id" to mergedAnn.id,
                        "classId" to classId,
                        "title" to mergedAnn.title,
                        "createdAt" to mergedAnn.createdAt,
                        "json" to serializeAnnouncementJson(mergedAnn).toString(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    tx.set(docRef, data, SetOptions.merge())
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed transactionally recording poll vote", e)
                uploadAnnouncementToFirestore(context, classId, fallbackAnnouncement)
            }
        }
    }

    /**
     * Duyuru okundu bilgisini Firestore üzerinde atomik işlemle (diğer velilerin okuma kayıtlarını ezmeden) kaydeder.
     */
    fun recordAnnouncementReadInFirestore(
        context: Context,
        classId: String,
        announcementId: String,
        readerLabel: String,
        readTimestamp: Long,
        fallbackAnnouncement: Announcement
    ) {
        if (classId.isBlank() || classId == "default") return
        val db = getFirestore(context) ?: return
        val docRef = db.collection("classrooms").document(classId)
            .collection("announcements").document(announcementId)
        syncScope.launch {
            try {
                db.runTransaction { tx ->
                    val snap = tx.get(docRef)
                    val baseAnn = snap.getString("json")?.takeIf { it.isNotBlank() }?.let { raw ->
                        try {
                            parseAnnouncementJson(JSONObject(raw))
                        } catch (_: Exception) {
                            fallbackAnnouncement
                        }
                    } ?: fallbackAnnouncement

                    if (baseAnn.readReceipts.containsKey(readerLabel)) return@runTransaction null
                    val mergedReceipts = baseAnn.readReceipts + (readerLabel to readTimestamp)
                    val mergedAnn = baseAnn.copy(classId = classId, readReceipts = mergedReceipts)
                    val data = mapOf(
                        "id" to mergedAnn.id,
                        "classId" to classId,
                        "title" to mergedAnn.title,
                        "createdAt" to mergedAnn.createdAt,
                        "json" to serializeAnnouncementJson(mergedAnn).toString(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    tx.set(docRef, data, SetOptions.merge())
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed transactionally recording read receipt", e)
                uploadAnnouncementToFirestore(context, classId, fallbackAnnouncement)
            }
        }
    }

    /**
     * Duyuruyu Firestore'dan siler.
     */
    fun deleteAnnouncementFromFirestore(context: Context, classId: String, announcementId: String) {
        if (classId.isBlank() || classId == "default") return
        val db = getFirestore(context) ?: return
        syncScope.launch {
            try {
                db.collection("classrooms").document(classId)
                    .collection("announcements").document(announcementId)
                    .delete()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete announcement from Firestore", e)
            }
        }
    }

    /**
     * Firestore üzerinden sınıfa ait duyuruları gerçek zamanlı dinler.
     * Öğretmen duyuru veya anket yayınladığında veli portalında ve diğer cihazlarda ANINDA görünür.
     */
    fun observeFirestoreAnnouncements(
        context: Context,
        classId: String,
        onUpdate: (List<Announcement>) -> Unit
    ): ListenerRegistration? {
        if (classId.isBlank() || classId == "default") return null
        val db = getFirestore(context) ?: return null
        return try {
            db.collection("classrooms").document(classId)
                .collection("announcements")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    try {
                        val list = mutableListOf<Announcement>()
                        for (doc in snapshot.documents) {
                            val jsonStr = doc.getString("json")
                            if (!jsonStr.isNullOrBlank()) {
                                try {
                                    val ann = parseAnnouncementJson(JSONObject(jsonStr))
                                    if (ann.id != "ann_1" && ann.id != "ann_2") {
                                        list.add(ann)
                                    }
                                } catch (_: Exception) {}
                            }
                        }
                        if (list.isEmpty()) {
                            val localList = loadAnnouncements(context, classId, emptyList())
                            if (localList.isNotEmpty()) {
                                localList.forEach { uploadAnnouncementToFirestore(context, classId, it) }
                                onUpdate(localList)
                                return@addSnapshotListener
                            }
                        }
                        list.sortByDescending { it.createdAt }
                        if (list.isNotEmpty()) {
                            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            val jsonArray = JSONArray()
                            for (item in list) {
                                jsonArray.put(serializeAnnouncementJson(item))
                            }
                            val str = jsonArray.toString()
                            prefs.edit()
                                .putString(KEY_ANNOUNCEMENTS_PREFIX + classId, str)
                                .putString(KEY_ANNOUNCEMENTS_PREFIX + "default", str)
                                .apply()
                        }
                        onUpdate(list)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed parsing announcements snapshot", e)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register announcements snapshot listener", e)
            null
        }
    }

    private val SAMPLE_MESSAGE_IDS = setOf("msg_1", "msg_2", "msg_3", "msg_4", "msg_5")
    private const val KEY_NOTIFIED_MESSAGE_IDS_PREFIX = "notified_msg_ids_"
    private const val KEY_CHAT_CLEAR_TIMESTAMPS_PREFIX = "chat_clear_ts_"
    private const val KEY_ACTIVITY_BASELINE_PREFIX = "activity_baseline_ts_"
    private const val KEY_SEEN_ACTIVITY_EVENTS_PREFIX = "seen_activity_events_"

    fun isSampleChatMessage(msg: ChatMessage): Boolean {
        return msg.id in SAMPLE_MESSAGE_IDS || msg.studentId.startsWith("student_test_")
    }

    /**
     * Sınıfa ait sohbet mesajlarını yükler; sadece gerçek mesajları getirir.
     */
    fun loadChatMessages(
        context: Context,
        classId: String,
        students: List<Student>,
        allowDefaultFallback: Boolean = true
    ): List<ChatMessage> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rawJson = (if (classId.isNotBlank()) prefs.getString(KEY_CHAT_MESSAGES_PREFIX + classId, null) else null)
            ?: if (allowDefaultFallback) prefs.getString(KEY_CHAT_MESSAGES_PREFIX + "default", null) else null

        if (!rawJson.isNullOrBlank()) {
            try {
                val list = mutableListOf<ChatMessage>()
                val jsonArray = JSONArray(rawJson)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val msg = parseChatMessageJson(obj)
                    if (!isSampleChatMessage(msg)) {
                        list.add(msg)
                    }
                }
                return list.sortedBy { it.timestamp }
            } catch (_: Exception) {
            }
        }

        return emptyList()
    }

    /**
     * Sohbet mesajlarını yalnızca yerel önbelleğe (SharedPreferences) hızlıca kaydeder.
     */
    fun saveChatMessagesLocal(context: Context, classId: String, messages: List<ChatMessage>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        for (msg in messages) {
            jsonArray.put(serializeChatMessageJson(msg))
        }
        val str = jsonArray.toString()
        val editor = prefs.edit()
        if (classId.isNotBlank()) {
            editor.putString(KEY_CHAT_MESSAGES_PREFIX + classId, str)
        }
        editor.putString(KEY_CHAT_MESSAGES_PREFIX + "default", str)
        editor.apply()
    }

    /**
     * Sohbet mesajlarını kaydeder ve buluta yükler.
     */
    fun saveChatMessages(context: Context, classId: String, messages: List<ChatMessage>) {
        saveChatMessagesLocal(context, classId, messages)
        messages.lastOrNull()?.let { latest ->
            uploadChatMessageToFirestore(context, classId, latest)
        }
    }

    /**
     * Tek bir sohbet mesajını Firestore'a canlı yükler ve teslim durumunu günceller.
     */
    fun uploadChatMessageToFirestore(
        context: Context,
        classId: String,
        message: ChatMessage,
        onFailure: ((Exception) -> Unit)? = null
    ) {
        if (classId.isBlank() || classId == "default") {
            onFailure?.invoke(IllegalStateException("Aktif sınıf bulunamadı"))
            return
        }
        val db = getFirestore(context)
        if (db == null) {
            onFailure?.invoke(IllegalStateException("Firestore bağlantısı kurulamadı"))
            return
        }
        syncScope.launch {
            try {
                val deliveredMessage = if (message.status == MessageStatus.SENT) {
                    message.copy(classId = classId, status = MessageStatus.DELIVERED)
                } else {
                    message.copy(classId = classId)
                }
                val json = serializeChatMessageJson(deliveredMessage).toString()
                val data = mapOf(
                    "id" to deliveredMessage.id,
                    "classId" to classId,
                    "studentId" to deliveredMessage.studentId,
                    "parentEmail" to deliveredMessage.parentEmail,
                    "isFromTeacher" to deliveredMessage.isFromTeacher,
                    "timestamp" to deliveredMessage.timestamp,
                    "status" to deliveredMessage.status.name,
                    "json" to json,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.collection("classrooms").document(classId)
                    .collection("messages").document(deliveredMessage.id)
                    .set(data, SetOptions.merge())
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to upload chat message ${deliveredMessage.id}", e)
                        onFailure?.invoke(e)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload chat message to Firestore", e)
                onFailure?.invoke(e)
            }
        }
    }

    /**
     * İki yönlü okundu durumlarını Firestore'a işler (yalnızca durumu READ olan mesajları yazar).
     */
    fun markChatMessagesReadInFirestore(context: Context, classId: String, readMessages: List<ChatMessage>) {
        if (classId.isBlank() || classId == "default" || readMessages.isEmpty()) return
        val db = getFirestore(context) ?: return
        syncScope.launch {
            try {
                readMessages.forEach { msg ->
                    val readMsg = msg.copy(classId = classId, status = MessageStatus.READ)
                    val data = mapOf(
                        "id" to readMsg.id,
                        "classId" to classId,
                        "studentId" to readMsg.studentId,
                        "parentEmail" to readMsg.parentEmail,
                        "isFromTeacher" to readMsg.isFromTeacher,
                        "timestamp" to readMsg.timestamp,
                        "status" to MessageStatus.READ.name,
                        "json" to serializeChatMessageJson(readMsg).toString(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    db.collection("classrooms").document(classId)
                        .collection("messages").document(readMsg.id)
                        .set(data, SetOptions.merge())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark chat messages read in Firestore", e)
            }
        }
    }

    /**
     * Firestore üzerinden sınıfa ait sohbet mesajlarını gerçek zamanlı dinler.
     * Veli öğretmenine, öğretmen veliye mesaj yazdığı anda ekranda anlık belirir.
     */
    fun observeFirestoreChatMessages(
        context: Context,
        classId: String,
        onUpdate: (List<ChatMessage>) -> Unit
    ): ListenerRegistration? {
        if (classId.isBlank() || classId == "default") return null
        val db = getFirestore(context) ?: return null
        return try {
            db.collection("classrooms").document(classId)
                .collection("messages")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) {
                        if (error != null) Log.e(TAG, "Chat messages snapshot error", error)
                        return@addSnapshotListener
                    }
                    try {
                        val list = mutableListOf<ChatMessage>()
                        for (doc in snapshot.documents) {
                            val jsonStr = doc.getString("json")
                            if (!jsonStr.isNullOrBlank()) {
                                try {
                                    val msg = parseChatMessageJson(JSONObject(jsonStr))
                                    val statusOverride = doc.getString("status")?.let {
                                        try { MessageStatus.valueOf(it) } catch (_: Exception) { null }
                                    }
                                    val hiddenForTeacherOverride = doc.getBoolean("hiddenForTeacher")
                                    val hiddenForParentsOverride = (doc.get("hiddenForParents") as? List<*>)
                                        ?.mapNotNull { it?.toString() }
                                    val deletedForEveryoneOverride = doc.getBoolean("isDeletedForEveryone")

                                    val mergedHiddenParents = if (hiddenForParentsOverride != null) {
                                        (msg.hiddenForParentEmails + hiddenForParentsOverride).distinct()
                                    } else {
                                        msg.hiddenForParentEmails
                                    }
                                    val resolvedDeleted = deletedForEveryoneOverride ?: msg.isDeletedForEveryone
                                    val resolved = msg.copy(
                                        status = statusOverride ?: msg.status,
                                        text = if (resolvedDeleted) "" else msg.text,
                                        hiddenForTeacher = hiddenForTeacherOverride ?: msg.hiddenForTeacher,
                                        hiddenForParentEmails = mergedHiddenParents,
                                        isDeletedForEveryone = resolvedDeleted
                                    )
                                    if (!isSampleChatMessage(resolved)) {
                                        list.add(resolved)
                                    }
                                } catch (_: Exception) {}
                            }
                        }
                        if (list.isEmpty() && !snapshot.metadata.isFromCache) {
                            val localMessages = loadChatMessages(context, classId, emptyList(), allowDefaultFallback = false)
                            if (localMessages.isNotEmpty()) {
                                localMessages.forEach { uploadChatMessageToFirestore(context, classId, it) }
                                onUpdate(localMessages)
                                return@addSnapshotListener
                            }
                        }
                        list.sortBy { it.timestamp }
                        saveChatMessagesLocal(context, classId, list)
                        onUpdate(list)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed parsing chat messages snapshot", e)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register chat messages snapshot listener", e)
            null
        }
    }

    /**
     * WhatsApp "Benden Sil": Seçilen mesajları yalnızca ilgili kullanıcı (öğretmen veya belirli veli) için gizler.
     * Karşı tarafın mesaj geçmişine ve belgedeki diğer kullanıcılara dokunmaz.
     */
    fun hideChatMessagesForUserInFirestore(
        context: Context,
        classId: String,
        messageIds: Set<String>,
        viewerIsTeacher: Boolean,
        parentKey: String
    ) {
        if (classId.isBlank() || classId == "default" || messageIds.isEmpty()) return
        val db = getFirestore(context) ?: return
        syncScope.launch {
            try {
                messageIds.forEach { msgId ->
                    val patch = if (viewerIsTeacher) {
                        mapOf<String, Any>(
                            "hiddenForTeacher" to true,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    } else {
                        mapOf<String, Any>(
                            "hiddenForParents" to FieldValue.arrayUnion(parentKey),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    }
                    db.collection("classrooms").document(classId)
                        .collection("messages").document(msgId)
                        .set(patch, SetOptions.merge())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to hide messages for user in Firestore", e)
            }
        }
    }

    /**
     * WhatsApp "Herkesten Sil": Kullanıcının kendi gönderdiği mesajı her iki tarafta "Bu mesaj silindi" olarak işaretler.
     */
    fun deleteChatMessagesForEveryoneInFirestore(
        context: Context,
        classId: String,
        messageIds: Set<String>
    ) {
        if (classId.isBlank() || classId == "default" || messageIds.isEmpty()) return
        val db = getFirestore(context) ?: return
        syncScope.launch {
            try {
                messageIds.forEach { msgId ->
                    val patch = mapOf<String, Any>(
                        "isDeletedForEveryone" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    db.collection("classrooms").document(classId)
                        .collection("messages").document(msgId)
                        .set(patch, SetOptions.merge())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete messages for everyone in Firestore", e)
            }
        }
    }

    fun loadChatClearTimestamps(context: Context, classId: String, viewerDocKey: String): Map<String, Long> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "${KEY_CHAT_CLEAR_TIMESTAMPS_PREFIX}${classId}_$viewerDocKey"
        val raw = prefs.getString(key, null) ?: return emptyMap()
        return try {
            val obj = JSONObject(raw)
            val result = mutableMapOf<String, Long>()
            val iter = obj.keys()
            while (iter.hasNext()) {
                val k = iter.next()
                result[k] = obj.optLong(k, 0L)
            }
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun saveChatClearTimestampsLocal(
        context: Context,
        classId: String,
        viewerDocKey: String,
        clearMap: Map<String, Long>
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "${KEY_CHAT_CLEAR_TIMESTAMPS_PREFIX}${classId}_$viewerDocKey"
        val obj = JSONObject()
        clearMap.forEach { (k, v) -> obj.put(k, v) }
        prefs.edit().putString(key, obj.toString()).apply()
    }

    fun updateChatClearTimestampsInFirestore(
        context: Context,
        classId: String,
        viewerDocKey: String,
        clearMap: Map<String, Long>
    ) {
        if (classId.isBlank() || classId == "default") return
        val db = getFirestore(context) ?: return
        syncScope.launch {
            try {
                val data = mapOf(
                    "clearedBeforeByKey" to clearMap,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.collection("classrooms").document(classId)
                    .collection("chat_clear_state").document(viewerDocKey)
                    .set(data, SetOptions.merge())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update chat clear state in Firestore", e)
            }
        }
    }

    fun observeFirestoreChatClearTimestamps(
        context: Context,
        classId: String,
        viewerDocKey: String,
        onUpdate: (Map<String, Long>) -> Unit
    ): ListenerRegistration? {
        if (classId.isBlank() || classId == "default") return null
        val db = getFirestore(context) ?: return null
        return try {
            db.collection("classrooms").document(classId)
                .collection("chat_clear_state").document(viewerDocKey)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                    try {
                        @Suppress("UNCHECKED_CAST")
                        val remoteRaw = snapshot.get("clearedBeforeByKey") as? Map<*, *> ?: emptyMap<Any?, Any?>()
                        val local = loadChatClearTimestamps(context, classId, viewerDocKey)
                        val merged = local.toMutableMap()
                        remoteRaw.forEach { (k, v) ->
                            val keyStr = k?.toString()
                            val ts = (v as? Number)?.toLong()
                            if (!keyStr.isNullOrBlank() && ts != null) {
                                merged[keyStr] = maxOf(merged[keyStr] ?: 0L, ts)
                            }
                        }
                        saveChatClearTimestampsLocal(context, classId, viewerDocKey, merged)
                        onUpdate(merged)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed parsing chat clear state snapshot", e)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed registering chat clear state listener", e)
            null
        }
    }

    fun loadNotifiedMessageIds(context: Context, classId: String): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_NOTIFIED_MESSAGE_IDS_PREFIX + classId, null) ?: return emptySet()
        return raw.split(",").filter { it.isNotBlank() }.toSet()
    }

    fun saveNotifiedMessageIds(context: Context, classId: String, ids: Set<String>) {
        val trimmed = if (ids.size > 400) ids.toList().takeLast(400) else ids.toList()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_NOTIFIED_MESSAGE_IDS_PREFIX + classId, trimmed.joinToString(",")).apply()
    }

    /**
     * Belirli sınıf + rol/kullanıcı (viewerKey) için etkinlik bildirim taban zamanını ve görülen olay anahtarlarını yükler.
     * İlk kurulumda varsayılan taban zamanını hemen kalıcı hale getirir ki eski kayıtlar yığılmasın, sonraki olaylar yakalansın.
     */
    fun loadActivityNotificationState(
        context: Context,
        classId: String,
        viewerKey: String,
        defaultBaselineMs: Long = System.currentTimeMillis() - 5 * 60_000L
    ): Pair<Long, Set<String>> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val scopedSuffix = if (viewerKey.isBlank()) classId else "${classId}_$viewerKey"
        val baselinePrefKey = KEY_ACTIVITY_BASELINE_PREFIX + scopedSuffix
        val seenPrefKey = KEY_SEEN_ACTIVITY_EVENTS_PREFIX + scopedSuffix

        val savedBaseline = if (prefs.contains(baselinePrefKey)) {
            prefs.getLong(baselinePrefKey, defaultBaselineMs)
        } else {
            prefs.edit().putLong(baselinePrefKey, defaultBaselineMs).apply()
            defaultBaselineMs
        }

        val rawSeen = prefs.getString(seenPrefKey, null)
        val seenSet = if (rawSeen.isNullOrBlank()) {
            emptySet()
        } else {
            try {
                val arr = JSONArray(rawSeen)
                buildSet {
                    for (i in 0 until arr.length()) {
                        val value = arr.optString(i, "")
                        if (value.isNotBlank()) add(value)
                    }
                }
            } catch (_: Exception) {
                emptySet()
            }
        }
        return savedBaseline to seenSet
    }

    fun saveActivityNotificationState(
        context: Context,
        classId: String,
        viewerKey: String,
        baselineMs: Long,
        seenKeys: Set<String>
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val scopedSuffix = if (viewerKey.isBlank()) classId else "${classId}_$viewerKey"
        val trimmed = if (seenKeys.size > 1500) seenKeys.toList().takeLast(1500) else seenKeys.toList()
        val arr = JSONArray()
        trimmed.forEach { arr.put(it) }
        prefs.edit()
            .putLong(KEY_ACTIVITY_BASELINE_PREFIX + scopedSuffix, baselineMs)
            .putString(KEY_SEEN_ACTIVITY_EVENTS_PREFIX + scopedSuffix, arr.toString())
            .apply()
    }

    /**
     * Bildirimleri rol/kullanıcıya özel yükler; eski sürüm anahtarından yalnızca öğretmen görünümü için geçiş yapar.
     */
    fun loadNotifications(
        context: Context,
        classId: String,
        viewerKey: String = ""
    ): List<BirizNotification> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val scopedKey = if (viewerKey.isNotBlank()) {
            "${KEY_NOTIFICATIONS_PREFIX}${classId}_$viewerKey"
        } else {
            KEY_NOTIFICATIONS_PREFIX + classId
        }
        val rawJson = prefs.getString(scopedKey, null)
            ?: if (viewerKey == "teacher" || viewerKey.isBlank()) {
                prefs.getString(KEY_NOTIFICATIONS_PREFIX + classId, null)
            } else null

        if (!rawJson.isNullOrBlank()) {
            try {
                val list = mutableListOf<BirizNotification>()
                val jsonArray = JSONArray(rawJson)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val id = obj.getString("id")
                    // Örnek / sahte bildirimleri filtrele
                    if (id != "notif_1" && id != "notif_2" && id != "notif_3") {
                        val parsedType = try {
                            NotificationType.valueOf(obj.optString("type", "ANNOUNCEMENT"))
                        } catch (_: Exception) {
                            NotificationType.ANNOUNCEMENT
                        }
                        list.add(
                            BirizNotification(
                                id = id,
                                title = obj.getString("title"),
                                message = obj.getString("message"),
                                timestamp = obj.getLong("timestamp"),
                                type = parsedType,
                                targetSubPageName = obj.optString("targetSubPageName", "ILETISIM_DUYURULAR"),
                                targetId = if (obj.has("targetId")) obj.getString("targetId") else null,
                                isRead = obj.optBoolean("isRead", false)
                            )
                        )
                    }
                }
                return list
            } catch (_: Exception) {
            }
        }

        return emptyList()
    }

    /**
     * Bildirimleri rol/kullanıcıya özel kaydeder.
     */
    fun saveNotifications(
        context: Context,
        classId: String,
        notifications: List<BirizNotification>,
        viewerKey: String = ""
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        for (item in notifications) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("message", item.message)
                put("timestamp", item.timestamp)
                put("type", item.type.name)
                put("targetSubPageName", item.targetSubPageName)
                if (item.targetId != null) put("targetId", item.targetId)
                put("isRead", item.isRead)
            }
            jsonArray.put(obj)
        }
        val storageKey = if (viewerKey.isNotBlank()) {
            "${KEY_NOTIFICATIONS_PREFIX}${classId}_$viewerKey"
        } else {
            KEY_NOTIFICATIONS_PREFIX + classId
        }
        prefs.edit().putString(storageKey, jsonArray.toString()).apply()
    }

    /**
     * WhatsApp / Sosyal Medya için formatlı duyuru metni üretir.
     */
    fun generateAnnouncementWhatsAppShareText(
        announcement: Announcement,
        className: String,
        teacherName: String
    ): String {
        val sb = StringBuilder()
        sb.append("📢 *BİRİZ SINIF DUYURUSU*\n")
        sb.append("🏫 *$className - $teacherName*\n")
        sb.append("🗓 ${formatDateTime(announcement.createdAt)}\n")
        if (announcement.isEdited) {
            sb.append("✏️ _(Duyuru güncellendi)_\n")
        }
        sb.append("━━━━━━━━━━━━━━━━━━━━\n\n")
        sb.append("📌 *${announcement.title}*\n\n")
        sb.append("${announcement.content}\n\n")

        announcement.poll?.let { poll ->
            sb.append("━━━━━━━━━━━━━━━━━━━━\n")
            sb.append("📊 *ANKET: ${poll.question}*\n")
            poll.options.forEachIndexed { index, option ->
                val percent = if (poll.totalVotes > 0) (option.voteCount * 100 / poll.totalVotes) else 0
                sb.append("${index + 1}. ${option.text} — %$percent (${option.voteCount} Oy)\n")
            }
            sb.append("\n_Velilerimiz BİRİZ Veli Portalından anket oyunu verebilir._\n")
        }

        sb.append("━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("BİRİZ İlkokul Eğitim & Sınıf Ekosistemi ✨")
        return sb.toString()
    }

    /**
     * Android Share Intent başlatır.
     */
    fun shareText(context: Context, text: String, title: String = "Duyuruyu Paylaş") {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(shareIntent)
    }

    // --- JSON SERİALİZASYON YARDIMCILARI ---

    private fun serializeAnnouncementJson(item: Announcement): JSONObject {
        val obj = JSONObject()
        obj.put("id", item.id)
        obj.put("classId", item.classId)
        obj.put("title", item.title)
        obj.put("content", item.content)
        obj.put("targetType", item.targetType.name)
        obj.put("createdAt", item.createdAt)
        obj.put("updatedAt", item.updatedAt)
        obj.put("isEdited", item.isEdited)

        val targetStudentIdsArr = JSONArray()
        item.targetStudentIds.forEach { targetStudentIdsArr.put(it) }
        obj.put("targetStudentIds", targetStudentIdsArr)

        val targetStudentNamesArr = JSONArray()
        item.targetStudentNames.forEach { targetStudentNamesArr.put(it) }
        obj.put("targetStudentNames", targetStudentNamesArr)

        val deliveredParentsArr = JSONArray()
        item.deliveredToParentNames.forEach { deliveredParentsArr.put(it) }
        obj.put("deliveredToParentNames", deliveredParentsArr)

        val readReceiptsObj = JSONObject()
        item.readReceipts.forEach { (parent, ts) -> readReceiptsObj.put(parent, ts) }
        obj.put("readReceipts", readReceiptsObj)

        item.poll?.let { poll ->
            val pollObj = JSONObject()
            pollObj.put("id", poll.id)
            pollObj.put("question", poll.question)

            val optionsArr = JSONArray()
            poll.options.forEach { opt ->
                val optObj = JSONObject()
                optObj.put("id", opt.id)
                optObj.put("text", opt.text)
                val votersArr = JSONArray()
                opt.voterParentNames.forEach { votersArr.put(it) }
                optObj.put("voterParentNames", votersArr)
                optionsArr.put(optObj)
            }
            pollObj.put("options", optionsArr)
            obj.put("poll", pollObj)
        }

        return obj
    }

    private fun parseAnnouncementJson(obj: JSONObject): Announcement {
        val targetStudentIds = mutableListOf<String>()
        val targetStudentIdsArr = obj.optJSONArray("targetStudentIds")
        if (targetStudentIdsArr != null) {
            for (i in 0 until targetStudentIdsArr.length()) {
                targetStudentIds.add(targetStudentIdsArr.getString(i))
            }
        }

        val targetStudentNames = mutableListOf<String>()
        val targetStudentNamesArr = obj.optJSONArray("targetStudentNames")
        if (targetStudentNamesArr != null) {
            for (i in 0 until targetStudentNamesArr.length()) {
                targetStudentNames.add(targetStudentNamesArr.getString(i))
            }
        }

        val deliveredParents = mutableListOf<String>()
        val deliveredArr = obj.optJSONArray("deliveredToParentNames")
        if (deliveredArr != null) {
            for (i in 0 until deliveredArr.length()) {
                deliveredParents.add(deliveredArr.getString(i))
            }
        }

        val readReceipts = mutableMapOf<String, Long>()
        val readReceiptsObj = obj.optJSONObject("readReceipts")
        if (readReceiptsObj != null) {
            val keys = readReceiptsObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                readReceipts[k] = readReceiptsObj.getLong(k)
            }
        }

        var poll: AnnouncementPoll? = null
        val pollObj = obj.optJSONObject("poll")
        if (pollObj != null) {
            val optList = mutableListOf<PollOption>()
            val optArr = pollObj.optJSONArray("options")
            if (optArr != null) {
                for (i in 0 until optArr.length()) {
                    val o = optArr.getJSONObject(i)
                    val voters = mutableListOf<String>()
                    val vArr = o.optJSONArray("voterParentNames")
                    if (vArr != null) {
                        for (j in 0 until vArr.length()) {
                            voters.add(vArr.getString(j))
                        }
                    }
                    optList.add(
                        PollOption(
                            id = o.getString("id"),
                            text = o.getString("text"),
                            voterParentNames = voters
                        )
                    )
                }
            }
            poll = AnnouncementPoll(
                id = pollObj.getString("id"),
                question = pollObj.getString("question"),
                options = optList
            )
        }

        return Announcement(
            id = obj.getString("id"),
            classId = obj.getString("classId"),
            title = obj.getString("title"),
            content = obj.getString("content"),
            targetType = AnnouncementTargetType.valueOf(obj.optString("targetType", "ALL_CLASS")),
            targetStudentIds = targetStudentIds,
            targetStudentNames = targetStudentNames,
            poll = poll,
            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
            isEdited = obj.optBoolean("isEdited", false),
            deliveredToParentNames = deliveredParents,
            readReceipts = readReceipts
        )
    }

    private fun serializeChatMessageJson(msg: ChatMessage): JSONObject {
        return JSONObject().apply {
            put("id", msg.id)
            put("classId", msg.classId)
            put("studentId", msg.studentId)
            put("studentNumber", msg.studentNumber)
            put("studentName", msg.studentName)
            put("parentName", msg.parentName)
            put("parentEmail", msg.parentEmail)
            put("isFromTeacher", msg.isFromTeacher)
            put("senderName", msg.senderName)
            put("text", msg.text)
            put("timestamp", msg.timestamp)
            put("status", msg.status.name)
            put("hiddenForTeacher", msg.hiddenForTeacher)
            val hiddenArr = JSONArray()
            msg.hiddenForParentEmails.forEach { hiddenArr.put(it) }
            put("hiddenForParentEmails", hiddenArr)
            put("isDeletedForEveryone", msg.isDeletedForEveryone)
        }
    }

    private fun parseChatMessageJson(obj: JSONObject): ChatMessage {
        val hiddenParents = mutableListOf<String>()
        val hiddenArr = obj.optJSONArray("hiddenForParentEmails")
        if (hiddenArr != null) {
            for (i in 0 until hiddenArr.length()) {
                val value = hiddenArr.optString(i, "")
                if (value.isNotBlank()) hiddenParents.add(value)
            }
        }
        val deletedForAll = obj.optBoolean("isDeletedForEveryone", false)
        return ChatMessage(
            id = obj.getString("id"),
            classId = obj.getString("classId"),
            studentId = obj.getString("studentId"),
            studentNumber = obj.getString("studentNumber"),
            studentName = obj.getString("studentName"),
            parentName = obj.getString("parentName"),
            parentEmail = obj.optString("parentEmail", ""),
            isFromTeacher = obj.getBoolean("isFromTeacher"),
            senderName = obj.getString("senderName"),
            text = if (deletedForAll) "" else obj.getString("text"),
            timestamp = obj.getLong("timestamp"),
            status = MessageStatus.valueOf(obj.optString("status", "READ")),
            hiddenForTeacher = obj.optBoolean("hiddenForTeacher", false),
            hiddenForParentEmails = hiddenParents,
            isDeletedForEveryone = deletedForAll
        )
    }
}
