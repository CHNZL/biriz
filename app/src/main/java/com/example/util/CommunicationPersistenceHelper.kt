package com.example.util

import android.content.Context
import android.content.Intent
import com.example.data.model.Announcement
import com.example.data.model.AnnouncementPoll
import com.example.data.model.AnnouncementTargetType
import com.example.data.model.BirizNotification
import com.example.data.model.ChatMessage
import com.example.data.model.MessageStatus
import com.example.data.model.NotificationType
import com.example.data.model.PollOption
import com.example.data.model.Student
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CommunicationPersistenceHelper {

    private const val PREFS_NAME = "biriz_communication_storage"
    private const val KEY_ANNOUNCEMENTS_PREFIX = "announcements_"
    private const val KEY_CHAT_MESSAGES_PREFIX = "chat_messages_"
    private const val KEY_NOTIFICATIONS_PREFIX = "notifications_"
    private const val KEY_FONT_OFFSET = "key_communication_font_offset"

    private val timeFormat = SimpleDateFormat("HH:mm", Locale("tr", "TR"))
    private val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("tr", "TR"))

    fun loadFontOffset(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_FONT_OFFSET, 0)
    }

    fun saveFontOffset(context: Context, offset: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_FONT_OFFSET, offset).apply()
    }

    fun formatTime(timestamp: Long): String = timeFormat.format(Date(timestamp))
    fun formatDateTime(timestamp: Long): String = dateFormat.format(Date(timestamp))

    /**
     * Sınıfa ait duyuruları yükler; sadece gerçek duyuruları getirir, sahte/örnek veri tohumlamaz.
     */
    fun loadAnnouncements(context: Context, classId: String, students: List<Student>): List<Announcement> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rawJson = prefs.getString(KEY_ANNOUNCEMENTS_PREFIX + classId, null)

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
     * Sınıfa ait duyuruları kaydeder.
     */
    fun saveAnnouncements(context: Context, classId: String, announcements: List<Announcement>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        for (announcement in announcements) {
            jsonArray.put(serializeAnnouncementJson(announcement))
        }
        prefs.edit().putString(KEY_ANNOUNCEMENTS_PREFIX + classId, jsonArray.toString()).apply()
    }

    /**
     * Sınıfa ait sohbet mesajlarını yükler; sadece gerçek mesajları getirir.
     */
    fun loadChatMessages(context: Context, classId: String, students: List<Student>): List<ChatMessage> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rawJson = prefs.getString(KEY_CHAT_MESSAGES_PREFIX + classId, null)

        if (!rawJson.isNullOrBlank()) {
            try {
                val list = mutableListOf<ChatMessage>()
                val jsonArray = JSONArray(rawJson)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val msg = parseChatMessageJson(obj)
                    // Örnek / sahte mesajları filtrele
                    val isSample = msg.id.startsWith("msg_1") || msg.id.startsWith("msg_2") ||
                            msg.id.startsWith("msg_3") || msg.id.startsWith("msg_4") ||
                            msg.id.startsWith("msg_5") || msg.studentId.startsWith("student_test_")
                    if (!isSample) {
                        list.add(msg)
                    }
                }
                return list
            } catch (_: Exception) {
            }
        }

        return emptyList()
    }

    /**
     * Sohbet mesajlarını kaydeder.
     */
    fun saveChatMessages(context: Context, classId: String, messages: List<ChatMessage>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        for (msg in messages) {
            jsonArray.put(serializeChatMessageJson(msg))
        }
        prefs.edit().putString(KEY_CHAT_MESSAGES_PREFIX + classId, jsonArray.toString()).apply()
    }

    /**
     * Bildirimleri yükler; sadece gerçek bildirimleri getirir.
     */
    fun loadNotifications(context: Context, classId: String): List<BirizNotification> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rawJson = prefs.getString(KEY_NOTIFICATIONS_PREFIX + classId, null)

        if (!rawJson.isNullOrBlank()) {
            try {
                val list = mutableListOf<BirizNotification>()
                val jsonArray = JSONArray(rawJson)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val id = obj.getString("id")
                    // Örnek / sahte bildirimleri filtrele
                    if (id != "notif_1" && id != "notif_2" && id != "notif_3") {
                        list.add(
                            BirizNotification(
                                id = id,
                                title = obj.getString("title"),
                                message = obj.getString("message"),
                                timestamp = obj.getLong("timestamp"),
                                type = NotificationType.valueOf(obj.optString("type", "ANNOUNCEMENT")),
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
     * Bildirimleri kaydeder.
     */
    fun saveNotifications(context: Context, classId: String, notifications: List<BirizNotification>) {
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
        prefs.edit().putString(KEY_NOTIFICATIONS_PREFIX + classId, jsonArray.toString()).apply()
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
        }
    }

    private fun parseChatMessageJson(obj: JSONObject): ChatMessage {
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
            text = obj.getString("text"),
            timestamp = obj.getLong("timestamp"),
            status = MessageStatus.valueOf(obj.optString("status", "READ"))
        )
    }
}
