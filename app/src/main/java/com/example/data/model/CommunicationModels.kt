package com.example.data.model

/**
 * Duyuru Hedef Kitle Tipi
 */
enum class AnnouncementTargetType(val displayName: String) {
    ALL_CLASS("Tüm Sınıf"),
    SPECIFIC_STUDENTS("Belirli Öğrenciler")
}

/**
 * Duyuruya eklenen anket seçeneği
 */
data class PollOption(
    val id: String,
    val text: String,
    val voterParentNames: List<String> = emptyList()
) {
    val voteCount: Int get() = voterParentNames.size
}

/**
 * Duyuruya eklenen anket
 */
data class AnnouncementPoll(
    val id: String,
    val question: String,
    val options: List<PollOption> = emptyList()
) {
    val totalVotes: Int get() = options.sumOf { it.voteCount }
}

/**
 * Sınıf Duyurusu Modeli
 * WhatsApp mantığında teslim ve okundu raporu, anket ve düzenleme desteği içerir.
 */
data class Announcement(
    val id: String,
    val classId: String,
    val title: String,
    val content: String,
    val targetType: AnnouncementTargetType = AnnouncementTargetType.ALL_CLASS,
    val targetStudentIds: List<String> = emptyList(), // Boş ise tüm sınıf
    val targetStudentNames: List<String> = emptyList(),
    val poll: AnnouncementPoll? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isEdited: Boolean = false,
    val deliveredToParentNames: List<String> = emptyList(),
    // parentName -> Okuma Zaman Damgası (Long)
    val readReceipts: Map<String, Long> = emptyMap()
) {
    val readCount: Int get() = readReceipts.size
    val deliveredCount: Int get() = deliveredToParentNames.size
    val isReadByAll: Boolean get() = deliveredCount > 0 && readCount >= deliveredCount
}

/**
 * Mesaj Durumu (WhatsApp Mantığı: Gönderildi, İletildi, Okundu)
 */
enum class MessageStatus {
    SENT,       // Tek gri tik
    DELIVERED,  // Çift gri tik
    READ        // Çift mavi tik
}

/**
 * Veli - Öğretmen Bireysel Mesaj Kaydı
 */
data class ChatMessage(
    val id: String,
    val classId: String,
    val studentId: String,
    val studentNumber: String,
    val studentName: String,
    val parentName: String,
    val isFromTeacher: Boolean,
    val senderName: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.READ
)

/**
 * Veli Sohbet Özeti (Mesajlar Sekmesi Listesi)
 */
data class ParentChatSummary(
    val studentId: String,
    val studentNumber: String,
    val studentName: String,
    val parentName: String,
    val parentPhone: String,
    val isParentLinked: Boolean = true,
    val lastMessage: ChatMessage? = null,
    val unreadCount: Int = 0
)

/**
 * Bildirim Türleri
 */
enum class NotificationType {
    ANNOUNCEMENT,
    MESSAGE,
    PARENT_REGISTRATION
}

/**
 * Üst bardaki bildirim zilinde gösterilen bildirim modeli
 */
data class BirizNotification(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: NotificationType,
    val targetSubPageName: String, // DashboardSubPage name
    val targetId: String? = null,   // Duyuru ID veya Öğrenci ID
    val isRead: Boolean = false
)
