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
     * Sınıfa ait duyuruları yükler; yoksa varsayılan örnek veriler üretip kaydeder.
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
                    list.add(parseAnnouncementJson(obj))
                }
                return list
            } catch (_: Exception) {
                // Hata durumunda varsayılan tohumlama yapılır
            }
        }

        // İlk açılışta zengin tohum verileri oluştur
        val defaultList = generateInitialAnnouncements(classId, students)
        saveAnnouncements(context, classId, defaultList)
        return defaultList
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
     * Sınıfa ait sohbet mesajlarını yükler; yoksa varsayılan veli diyalogları tohumlar.
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
                    list.add(parseChatMessageJson(obj))
                }
                return list
            } catch (_: Exception) {
                // Fallback
            }
        }

        val defaultList = generateInitialChatMessages(classId, students)
        saveChatMessages(context, classId, defaultList)
        return defaultList
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
     * Bildirimleri yükler.
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
                    list.add(
                        BirizNotification(
                            id = obj.getString("id"),
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
                return list
            } catch (_: Exception) {
                // Fallback
            }
        }

        val initialList = listOf(
            BirizNotification(
                id = "notif_1",
                title = "Yeni Veli Mesajı",
                message = "Fatma Kavakoğlu (#45 Semih): Öğretmenim iyi akşamlar...",
                timestamp = System.currentTimeMillis() - 15 * 60 * 1000,
                type = NotificationType.MESSAGE,
                targetSubPageName = "ILETISIM_MESAJLAR",
                targetId = "student_test_1",
                isRead = false
            ),
            BirizNotification(
                id = "notif_2",
                title = "Yeni Anket Oyu",
                message = "Güneş Demir'in velisi 'Müze Gezisi' anketinde 'Katılacak' seçeneğini işaretledi.",
                timestamp = System.currentTimeMillis() - 45 * 60 * 1000,
                type = NotificationType.ANNOUNCEMENT,
                targetSubPageName = "ILETISIM_DUYURULAR",
                targetId = "ann_1",
                isRead = false
            ),
            BirizNotification(
                id = "notif_3",
                title = "Veli Katılım Talebi",
                message = "Ayşe Demir (#326 Güneş Demir) BİRİZ veli katılım kodunu onayladı.",
                timestamp = System.currentTimeMillis() - 2 * 3600 * 1000,
                type = NotificationType.PARENT_REGISTRATION,
                targetSubPageName = "SINIF_LISTESI",
                targetId = null,
                isRead = true
            )
        )
        saveNotifications(context, classId, initialList)
        return initialList
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
            isFromTeacher = obj.getBoolean("isFromTeacher"),
            senderName = obj.getString("senderName"),
            text = obj.getString("text"),
            timestamp = obj.getLong("timestamp"),
            status = MessageStatus.valueOf(obj.optString("status", "READ"))
        )
    }

    // --- BAŞLANGIÇ TOHUM VERİLERİ (TEST MODU VE İLK KULLANIM İÇİN) ---

    private fun generateInitialAnnouncements(classId: String, students: List<Student>): List<Announcement> {
        val parentNames = if (students.isNotEmpty()) {
            students.map { it.parentName ?: "${it.fullName.split(" ").first()} Velisi" }
        } else {
            listOf(
                "Fatma Kavakoğlu", "Ayşe Demir", "Mustafa Işık", "Zeynep Akkaş", "Mehmet Kaya",
                "Hülya Çelik", "Ali Yıldız", "Emine Şahin", "Ahmet Koç", "Merve Arslan"
            )
        }

        val now = System.currentTimeMillis()
        val readReceipts1 = mutableMapOf<String, Long>()
        parentNames.take(21).forEachIndexed { index, name ->
            readReceipts1[name] = now - (3600 * 1000L * (index % 12 + 1))
        }

        val poll1 = AnnouncementPoll(
            id = "poll_1",
            question = "24 Mayıs Cuma günü planlanan 'Bilim ve Teknoloji Müzesi' gezisine öğrencinizin katılım durumu nedir?",
            options = listOf(
                PollOption(
                    id = "opt_1",
                    text = "Evet, katılacak (İzin belgesi imzalanacak)",
                    voterParentNames = parentNames.take(16)
                ),
                PollOption(
                    id = "opt_2",
                    text = "Hayır, katılamayacak",
                    voterParentNames = parentNames.drop(16).take(3)
                ),
                PollOption(
                    id = "opt_3",
                    text = "Kararsızım / Detaylı bilgi rica ediyorum",
                    voterParentNames = parentNames.drop(19).take(2)
                )
            )
        )

        val ann1 = Announcement(
            id = "ann_1",
            classId = classId,
            title = "🏛️ Bilim Müzesi Gezisi & İzin Formları",
            content = "Değerli Velilerimiz;\nSınıfımızla birlikte MEB müfredatı kapsamında 'Bilim ve Teknoloji Müzesi' ziyareti gerçekleştireceğiz. Ulaşım okul servislerimiz ile sağlanacak olup gezi ücreti bulunmamaktadır. Lütfen aşağıdaki anketi yanıtlayınız.",
            targetType = AnnouncementTargetType.ALL_CLASS,
            targetStudentIds = emptyList(),
            targetStudentNames = emptyList(),
            poll = poll1,
            createdAt = now - (2 * 86400 * 1000L),
            updatedAt = now - (2 * 86400 * 1000L),
            isEdited = false,
            deliveredToParentNames = parentNames,
            readReceipts = readReceipts1
        )

        // İkinci duyuru: Özel duyuru (3 öğrenciye)
        val specificStudents = students.take(3)
        val specificParentNames = if (specificStudents.isNotEmpty()) {
            specificStudents.map { it.parentName ?: "${it.fullName} Velisi" }
        } else {
            listOf("Fatma Kavakoğlu", "Ayşe Demir", "Mustafa Işık")
        }

        val readReceipts2 = mutableMapOf<String, Long>()
        if (specificParentNames.isNotEmpty()) {
            readReceipts2[specificParentNames.first()] = now - (4 * 3600 * 1000L)
        }

        val ann2 = Announcement(
            id = "ann_2",
            classId = classId,
            title = "⭐ Akıl Oyunları Turnuvası Hazırlık Çalışması",
            content = "Sayın Velimiz;\nÖğrencimiz okullar arası Akıl ve Zeka Oyunları turnuvasında sınıfımızı temsil edecektir. Perşembe günü ders bitiminde 15:10 - 15:50 saatleri arasında okul kütüphanesinde mini pratik çalışması yapılacaktır. Bilginize sunarız.",
            targetType = AnnouncementTargetType.SPECIFIC_STUDENTS,
            targetStudentIds = specificStudents.map { it.id },
            targetStudentNames = specificStudents.map { "#${it.studentNumber} ${it.fullName}" },
            poll = null,
            createdAt = now - (1 * 86400 * 1000L),
            updatedAt = now - (1 * 86400 * 1000L),
            isEdited = false,
            deliveredToParentNames = specificParentNames,
            readReceipts = readReceipts2
        )

        return listOf(ann1, ann2)
    }

    private fun generateInitialChatMessages(classId: String, students: List<Student>): List<ChatMessage> {
        val now = System.currentTimeMillis()
        val list = mutableListOf<ChatMessage>()

        val s1 = students.getOrNull(0)
        val s1Id = s1?.id ?: "student_test_1"
        val s1No = s1?.studentNumber ?: "45"
        val s1Name = s1?.fullName ?: "SEMİH AHMET KAVAKOĞLU"
        val p1Name = s1?.parentName ?: "Fatma Kavakoğlu"

        list.add(
            ChatMessage(
                id = "msg_1",
                classId = classId,
                studentId = s1Id,
                studentNumber = s1No,
                studentName = s1Name,
                parentName = p1Name,
                isFromTeacher = true,
                senderName = "Cihan Özel",
                text = "İyi günler Fatma Hanım, Semih Ahmet bu hafta matematik dersindeki grup çalışmalarında harika bir liderlik gösterdi. Tebrik ederim!",
                timestamp = now - (4 * 3600 * 1000L),
                status = MessageStatus.READ
            )
        )
        list.add(
            ChatMessage(
                id = "msg_2",
                classId = classId,
                studentId = s1Id,
                studentNumber = s1No,
                studentName = s1Name,
                parentName = p1Name,
                isFromTeacher = false,
                senderName = p1Name,
                text = "Çok teşekkür ederiz Cihan Öğretmenim, evde de büyük bir hevesle ödevlerini hazırlıyor. İlginiz için minnettarız.",
                timestamp = now - (3 * 3600 * 1000L),
                status = MessageStatus.READ
            )
        )
        list.add(
            ChatMessage(
                id = "msg_3",
                classId = classId,
                studentId = s1Id,
                studentNumber = s1No,
                studentName = s1Name,
                parentName = p1Name,
                isFromTeacher = false,
                senderName = p1Name,
                text = "Öğretmenim iyi akşamlar, yarınki fen bilimleri deneyi için gerekli büyüteci çantasına koyduk bilginiz olsun.",
                timestamp = now - (15 * 60 * 1000L),
                status = MessageStatus.DELIVERED
            )
        )

        val s2 = students.getOrNull(1)
        val s2Id = s2?.id ?: "student_test_2"
        val s2No = s2?.studentNumber ?: "326"
        val s2Name = s2?.fullName ?: "GÜNEŞ DEMİR"
        val p2Name = s2?.parentName ?: "Ayşe Demir"

        list.add(
            ChatMessage(
                id = "msg_4",
                classId = classId,
                studentId = s2Id,
                studentNumber = s2No,
                studentName = s2Name,
                parentName = p2Name,
                isFromTeacher = true,
                senderName = "Cihan Özel",
                text = "Merhaba Ayşe Hanım, Güneş'in gözlük kullanımını sınıfta ön sıraya alarak destekliyoruz. Tahtayı artık çok rahat takip edebiliyor.",
                timestamp = now - (24 * 3600 * 1000L),
                status = MessageStatus.READ
            )
        )
        list.add(
            ChatMessage(
                id = "msg_5",
                classId = classId,
                studentId = s2Id,
                studentNumber = s2No,
                studentName = s2Name,
                parentName = p2Name,
                isFromTeacher = false,
                senderName = p2Name,
                text = "Çok teşekkürler öğretmenim, biz de evde fark ettik baş ağrısı şikayeti tamamen geçti.",
                timestamp = now - (22 * 3600 * 1000L),
                status = MessageStatus.READ
            )
        )

        return list
    }
}
