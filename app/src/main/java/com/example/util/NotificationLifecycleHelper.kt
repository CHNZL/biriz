package com.example.util

import com.example.data.model.BirizNotification
import com.example.data.model.ChatMessage
import com.example.data.model.NotificationType

/**
 * Mesajlaşma ve uygulama güncelleme bildirimlerinin yaşam döngüsünü
 * (ekran açıkken bildirim engelleme, okununca silme, sürüme göre eski güncelleme
 * bildirimlerini otomatik temizleme) deterministik olarak yöneten yardımcı katman.
 */
object NotificationLifecycleHelper {

    private val VERSION_NAME_REGEX = Regex("""v?(\d+\.\d+\.\d+)""", RegexOption.IGNORE_CASE)

    fun isUpdateNotification(notif: BirizNotification): Boolean {
        return notif.type == NotificationType.APP_UPDATE ||
            notif.id.startsWith("app_update_") ||
            notif.targetSubPageName == "APP_UPDATE"
    }

    fun extractUpdateNotificationVersionCode(notif: BirizNotification): Int? {
        notif.targetId?.trim()?.toIntOrNull()?.let { return it }
        if (notif.id.startsWith("app_update_")) {
            notif.id.removePrefix("app_update_").trim().toIntOrNull()?.let { return it }
        }
        val versionName = extractUpdateNotificationVersionName(notif) ?: return null
        val parts = versionName.split(".").mapNotNull { part ->
            part.filter { it.isDigit() }.toIntOrNull()
        }
        return when {
            parts.size >= 3 -> parts[0] * 1000 + parts[1] * 100 + parts[2]
            parts.size == 2 -> parts[0] * 1000 + parts[1] * 100
            parts.size == 1 -> 1000 + parts[0]
            else -> null
        }
    }

    fun extractUpdateNotificationVersionName(notif: BirizNotification): String? {
        val fromTitle = VERSION_NAME_REGEX.find(notif.title)?.groupValues?.getOrNull(1)
        if (!fromTitle.isNullOrBlank()) return fromTitle
        val fromMessage = VERSION_NAME_REGEX.find(notif.message)?.groupValues?.getOrNull(1)
        if (!fromMessage.isNullOrBlank()) return fromMessage
        return null
    }

    fun compareVersionNames(first: String, second: String): Int {
        val fParts = first.trim().removePrefix("v").removePrefix("V").split(".").mapNotNull { it.toIntOrNull() }
        val sParts = second.trim().removePrefix("v").removePrefix("V").split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(fParts.size, sParts.size)
        for (i in 0 until maxLen) {
            val f = fParts.getOrElse(i) { 0 }
            val s = sParts.getOrElse(i) { 0 }
            if (f != s) return f.compareTo(s)
        }
        return 0
    }

    /**
     * Güncelleme bildiriminin yüklü sürüm veya son sunucu kontrolüne göre geçersizleşip
     * otomatik olarak silinmesi gerekip gerekmediğini belirler.
     */
    fun isStaleUpdateNotification(
        notif: BirizNotification,
        currentVersionCode: Int,
        currentVersionName: String,
        latestCheckInfo: UpdateInfo? = null
    ): Boolean {
        if (!isUpdateNotification(notif)) return false

        // Sunucu son kontrolü "güncelleme yok" diyorsa tüm güncelleme bildirimlerini temizle
        if (latestCheckInfo != null && !latestCheckInfo.isUpdateAvailable) {
            return true
        }

        val notifCode = extractUpdateNotificationVersionCode(notif)
        val notifName = extractUpdateNotificationVersionName(notif)

        // Yüklü sürüm kodu veya sürüm adı bildirimdeki sürüme eşit veya büyükse artık eskimiştir
        if (notifCode != null && notifCode <= currentVersionCode) {
            return true
        }
        if (notifName != null && compareVersionNames(notifName, currentVersionName) <= 0) {
            return true
        }

        // Sürüm bilgisi hiç çıkarılamayan eski/bozuk güncelleme bildirimlerini de temizle
        if (notifCode == null && notifName == null) {
            return true
        }

        // Daha yeni bir sürüm yayınlanmışsa ondan eski olan bildirimleri de temizle
        if (latestCheckInfo != null && latestCheckInfo.isUpdateAvailable) {
            if (notifCode != null && notifCode < latestCheckInfo.latestVersionCode) {
                return true
            }
            if (notifName != null && compareVersionNames(notifName, latestCheckInfo.latestVersionName) < 0) {
                return true
            }
        }

        return false
    }

    fun pruneStaleUpdateNotifications(
        notifications: List<BirizNotification>,
        currentVersionCode: Int,
        currentVersionName: String,
        latestCheckInfo: UpdateInfo? = null
    ): List<BirizNotification> {
        return notifications.filterNot {
            isStaleUpdateNotification(
                notif = it,
                currentVersionCode = currentVersionCode,
                currentVersionName = currentVersionName,
                latestCheckInfo = latestCheckInfo
            )
        }
    }

    /**
     * Gelen bir sohbet mesajının mevcut oturumdaki kullanıcıya ait yeni bir gelen mesaj olup olmadığını belirler.
     */
    fun isIncomingMessageForViewer(
        msg: ChatMessage,
        viewerIsTeacher: Boolean,
        viewerIsParent: Boolean,
        linkedParentStudentIds: List<String>,
        currentUserEmail: String
    ): Boolean {
        return when {
            viewerIsTeacher -> !msg.isFromTeacher
            viewerIsParent -> {
                if (!msg.isFromTeacher) return false
                val matchesStudent = linkedParentStudentIds.isEmpty() || msg.studentId in linkedParentStudentIds
                val matchesEmail = msg.parentEmail.isBlank() ||
                    currentUserEmail.isBlank() ||
                    currentUserEmail.equals("veli@biriz.app", ignoreCase = true) ||
                    msg.parentEmail.trim().equals(currentUserEmail.trim(), ignoreCase = true)
                matchesStudent && matchesEmail
            }
            else -> false
        }
    }

    /**
     * Kullanıcının tam o anda ilgili öğrencinin mesajlaşma ekranında aktif olup olmadığını belirler.
     * Aktif sohbet ekranındayken yeni mesaj için sesli/görsel bildirim üretilmez; mesaj anında okundu sayılır.
     */
    fun isChatScreenActivelyOpenForStudent(
        studentId: String,
        viewerIsTeacher: Boolean,
        viewerIsParent: Boolean,
        isAppInForeground: Boolean,
        isTeacherChatPageOpen: Boolean,
        activeTeacherChatStudentId: String?,
        isParentCommTabActive: Boolean,
        isParentChatSubTabActive: Boolean,
        activeParentStudentId: String?,
        isNotificationCenterOpen: Boolean
    ): Boolean {
        if (!isAppInForeground || isNotificationCenterOpen) return false
        return when {
            viewerIsTeacher -> {
                isTeacherChatPageOpen && activeTeacherChatStudentId == studentId
            }
            viewerIsParent -> {
                isParentCommTabActive && isParentChatSubTabActive && activeParentStudentId == studentId
            }
            else -> false
        }
    }

    fun removeMessageNotificationsForStudent(
        notifications: List<BirizNotification>,
        studentId: String
    ): List<BirizNotification> {
        return notifications.filterNot { notif ->
            notif.type == NotificationType.MESSAGE &&
                (notif.targetId == studentId || notif.id == "msg_notif_$studentId")
        }
    }
}
