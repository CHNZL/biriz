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
        currentUserEmail: String,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (msg.isPendingScheduled(nowMillis)) return false
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
                !notif.id.startsWith("msg_react_") &&
                !notif.id.startsWith("sched_confirm_") &&
                (notif.targetId == studentId || notif.id == "msg_notif_$studentId")
        }
    }

    private const val MAX_SYNCED_READ_IDS = 400
    private const val MAX_SYNCED_SEEN_KEYS = 800
    private const val MAX_SYNCED_MESSAGE_IDS = 400

    fun effectiveBaselineMs(state: NotificationReadSyncState): Long {
        return maxOf(state.baselineMs, state.readAllBeforeMs)
    }

    /**
     * İki cihaz arasındaki okundu/temizlendi durumunu kayıpsız ve tek yönlü (monotonic) olarak birleştirir.
     */
    fun mergeReadSyncState(
        local: NotificationReadSyncState,
        remote: NotificationReadSyncState
    ): NotificationReadSyncState {
        return NotificationReadSyncState(
            readAllBeforeMs = maxOf(local.readAllBeforeMs, remote.readAllBeforeMs),
            baselineMs = maxOf(local.baselineMs, remote.baselineMs),
            readNotificationIds = capPreservingOrder(local.readNotificationIds, remote.readNotificationIds, MAX_SYNCED_READ_IDS),
            seenEventKeys = capPreservingOrder(local.seenEventKeys, remote.seenEventKeys, MAX_SYNCED_SEEN_KEYS),
            dismissedMessageIds = capPreservingOrder(local.dismissedMessageIds, remote.dismissedMessageIds, MAX_SYNCED_MESSAGE_IDS)
        )
    }

    private fun capPreservingOrder(
        first: Collection<String>,
        second: Collection<String>,
        limit: Int
    ): Set<String> {
        val ordered = LinkedHashSet<String>()
        first.forEach { if (it.isNotBlank()) ordered.add(it) }
        second.forEach { if (it.isNotBlank()) ordered.add(it) }
        if (ordered.size <= limit) return ordered
        return ordered.toList().takeLast(limit).toCollection(LinkedHashSet())
    }

    /**
     * Bir bildirimin kullanıcı tarafından (bu cihazda veya başka bir cihazda)
     * daha önce okunmuş / kapatılmış olup olmadığını belirler.
     */
    fun isNotificationDismissed(
        notif: BirizNotification,
        state: NotificationReadSyncState
    ): Boolean {
        if (notif.id in state.readNotificationIds) return true
        if (notif.id.startsWith("notif_")) {
            val eventKey = notif.id.removePrefix("notif_")
            if (eventKey.isNotBlank() && eventKey in state.seenEventKeys) return true
        }
        if (isUpdateNotification(notif)) {
            return false
        }
        return state.readAllBeforeMs > 0L && notif.timestamp in 1L..state.readAllBeforeMs
    }

    fun isChatMessageNotificationDismissed(
        msg: ChatMessage,
        state: NotificationReadSyncState
    ): Boolean {
        if (msg.id in state.dismissedMessageIds) return true
        return state.readAllBeforeMs > 0L && msg.timestamp in 1L..state.readAllBeforeMs
    }

    fun pruneDismissedNotifications(
        notifications: List<BirizNotification>,
        state: NotificationReadSyncState
    ): List<BirizNotification> {
        return notifications.filterNot { isNotificationDismissed(it, state) }
    }

    fun recordSingleNotificationRead(
        state: NotificationReadSyncState,
        notificationId: String,
        extraDismissedMessageIds: Collection<String> = emptyList()
    ): NotificationReadSyncState {
        val extraSeen = if (notificationId.startsWith("notif_")) {
            listOf(notificationId.removePrefix("notif_")).filter { it.isNotBlank() }
        } else {
            emptyList()
        }
        return NotificationReadSyncState(
            readAllBeforeMs = state.readAllBeforeMs,
            baselineMs = state.baselineMs,
            readNotificationIds = capPreservingOrder(state.readNotificationIds, listOf(notificationId), MAX_SYNCED_READ_IDS),
            seenEventKeys = capPreservingOrder(state.seenEventKeys, extraSeen, MAX_SYNCED_SEEN_KEYS),
            dismissedMessageIds = capPreservingOrder(state.dismissedMessageIds, extraDismissedMessageIds, MAX_SYNCED_MESSAGE_IDS)
        )
    }

    fun recordAllNotificationsRead(
        state: NotificationReadSyncState,
        currentNotifications: List<BirizNotification>,
        currentUnreadMessageIds: Collection<String> = emptyList(),
        currentSeenEventKeys: Collection<String> = emptyList(),
        nowMillis: Long = System.currentTimeMillis()
    ): NotificationReadSyncState {
        val maxNotifTs = currentNotifications.maxOfOrNull { it.timestamp } ?: 0L
        val cutoffMs = maxOf(nowMillis, state.readAllBeforeMs, maxNotifTs)
        val notifIds = currentNotifications.map { it.id }
        val notifEventKeys = currentNotifications.mapNotNull { notif ->
            if (notif.id.startsWith("notif_")) notif.id.removePrefix("notif_").takeIf { it.isNotBlank() } else null
        }
        return NotificationReadSyncState(
            readAllBeforeMs = cutoffMs,
            baselineMs = maxOf(state.baselineMs, cutoffMs),
            readNotificationIds = capPreservingOrder(state.readNotificationIds, notifIds, MAX_SYNCED_READ_IDS),
            seenEventKeys = capPreservingOrder(state.seenEventKeys, notifEventKeys + currentSeenEventKeys, MAX_SYNCED_SEEN_KEYS),
            dismissedMessageIds = capPreservingOrder(state.dismissedMessageIds, currentUnreadMessageIds, MAX_SYNCED_MESSAGE_IDS)
        )
    }

    fun recordDismissedMessageIds(
        state: NotificationReadSyncState,
        messageIds: Collection<String>
    ): NotificationReadSyncState {
        if (messageIds.isEmpty()) return state
        return state.copy(
            dismissedMessageIds = capPreservingOrder(state.dismissedMessageIds, messageIds, MAX_SYNCED_MESSAGE_IDS)
        )
    }
}

/**
 * Aynı hesabın farklı cihazlarında bildirimlerin tekrar baştan gelmesini önlemek için
 * bulutta tutulan kişi ve sınıf bazlı okundu / temizlendi eşitleme durumu.
 */
data class NotificationReadSyncState(
    val readAllBeforeMs: Long = 0L,
    val baselineMs: Long = 0L,
    val readNotificationIds: Set<String> = emptySet(),
    val seenEventKeys: Set<String> = emptySet(),
    val dismissedMessageIds: Set<String> = emptySet()
)
