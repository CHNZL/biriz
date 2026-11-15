package com.example.util

import android.content.Context
import com.example.data.model.BirizNotification
import com.example.data.model.NotificationType
import com.example.data.model.UserRole

/**
 * Kullanıcıların üst bildirim zili ve sistem bildirim çubuğu için
 * kategori bazlı açma/kapama tercihlerini yönetir.
 */
enum class NotificationCategory(
    val key: String,
    val title: String,
    val description: String,
    val emoji: String,
    val forParent: Boolean,
    val forTeacher: Boolean
) {
    MESSAGES(
        key = "messages",
        title = "Mesajlar",
        description = "Doğrudan gelen sohbet mesajları",
        emoji = "💬",
        forParent = true,
        forTeacher = true
    ),
    MESSAGE_REACTIONS(
        key = "reactions",
        title = "Mesaj Emoji Tepkileri",
        description = "Mesajlara bırakılan 👍, ❤️, 👀 tepkileri",
        emoji = "❤️",
        forParent = true,
        forTeacher = true
    ),
    ANNOUNCEMENTS(
        key = "announcements",
        title = "Duyurular ve Anketler",
        description = "Sınıf duyuruları, güncellemeler ve okuma/anket tamamlanma özetleri",
        emoji = "📢",
        forParent = true,
        forTeacher = true
    ),
    STAR_REWARDS(
        key = "stars",
        title = "Yıldızlar ve Başarı Eşikleri",
        description = "Kazanılan yıldızlar, uyarı puanları ve 25/50/100 barajları",
        emoji = "⭐",
        forParent = true,
        forTeacher = false
    ),
    LIBRARY(
        key = "library",
        title = "Sınıf Kitaplığı & Gecikme",
        description = "Kitap ödünç/iade hareketleri ve 14 günlük iade hatırlatmaları",
        emoji = "📚",
        forParent = true,
        forTeacher = true
    ),
    TOURNAMENTS(
        key = "tournaments",
        title = "Turnuvalar ve Eşleşmeler",
        description = "Katılım, yeni maç eşleşmesi, skor, tur atlama ve sonuçlar",
        emoji = "🏆",
        forParent = true,
        forTeacher = false
    ),
    SCHEDULE_AND_CLASSROOM(
        key = "schedule_classroom",
        title = "Ders Programı, Sıra ve Gruplar",
        description = "Program değişiklikleri, oturma düzeni ve grup/lider atamaları",
        emoji = "📅",
        forParent = true,
        forTeacher = false
    ),
    PARENT_REGISTRATIONS(
        key = "parent_reg",
        title = "Veli Katılım ve Ayrılma",
        description = "Sınıfa katılan veya bağlantısını kaldıran veli bildirimleri",
        emoji = "👨‍👩‍👧",
        forParent = false,
        forTeacher = true
    ),
    SCHEDULED_DELIVERY_CONFIRM(
        key = "scheduled_confirm",
        title = "Planlanan Mesaj İletim Onayı",
        description = "Zamanlanmış mesajlar hedef saatte iletildiğinde onay bildirimi",
        emoji = "⏰",
        forParent = false,
        forTeacher = true
    ),
    APP_UPDATES(
        key = "app_updates",
        title = "Uygulama Güncellemeleri",
        description = "Yeni uygulama sürümü yayınlandığında bilgilendir",
        emoji = "🚀",
        forParent = true,
        forTeacher = true
    );

    companion object {
        fun categoriesForRole(role: UserRole): List<NotificationCategory> {
            return when (role) {
                UserRole.TEACHER -> entries.filter { it.forTeacher }
                UserRole.PARENT -> entries.filter { it.forParent }
                else -> entries.toList()
            }
        }
    }
}

object NotificationPreferencesHelper {

    private const val PREFS_NAME = "biriz_notification_prefs"
    private const val KEY_PREFIX = "notif_pref_"

    fun defaultPreferences(): Map<String, Boolean> {
        return NotificationCategory.entries.associate { it.key to true }
    }

    fun loadPreferences(context: Context, viewerKey: String): Map<String, Boolean> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val scope = viewerKey.ifBlank { "default" }
        return NotificationCategory.entries.associate { cat ->
            cat.key to prefs.getBoolean("${KEY_PREFIX}${scope}_${cat.key}", true)
        }
    }

    fun saveCategoryPreference(
        context: Context,
        viewerKey: String,
        category: NotificationCategory,
        enabled: Boolean
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val scope = viewerKey.ifBlank { "default" }
        prefs.edit().putBoolean("${KEY_PREFIX}${scope}_${category.key}", enabled).apply()
    }

    fun saveAllPreferences(
        context: Context,
        viewerKey: String,
        preferences: Map<String, Boolean>
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val scope = viewerKey.ifBlank { "default" }
        val editor = prefs.edit()
        NotificationCategory.entries.forEach { cat ->
            editor.putBoolean("${KEY_PREFIX}${scope}_${cat.key}", preferences[cat.key] ?: true)
        }
        editor.apply()
    }

    fun isCategoryEnabled(
        preferences: Map<String, Boolean>,
        category: NotificationCategory
    ): Boolean {
        return preferences[category.key] ?: true
    }

    fun categoryForNotification(notif: BirizNotification): NotificationCategory {
        if (notif.id.startsWith("msg_react_")) {
            return NotificationCategory.MESSAGE_REACTIONS
        }
        if (notif.id.startsWith("sched_confirm_")) {
            return NotificationCategory.SCHEDULED_DELIVERY_CONFIRM
        }
        return when (notif.type) {
            NotificationType.MESSAGE -> NotificationCategory.MESSAGES
            NotificationType.ANNOUNCEMENT -> NotificationCategory.ANNOUNCEMENTS
            NotificationType.STAR_REWARD -> NotificationCategory.STAR_REWARDS
            NotificationType.LIBRARY -> NotificationCategory.LIBRARY
            NotificationType.TOURNAMENT -> NotificationCategory.TOURNAMENTS
            NotificationType.SCHEDULE,
            NotificationType.SEATING,
            NotificationType.GROUP -> NotificationCategory.SCHEDULE_AND_CLASSROOM
            NotificationType.PARENT_REGISTRATION -> NotificationCategory.PARENT_REGISTRATIONS
            NotificationType.APP_UPDATE -> NotificationCategory.APP_UPDATES
        }
    }

    fun isNotificationEnabled(
        preferences: Map<String, Boolean>,
        notif: BirizNotification
    ): Boolean {
        return isCategoryEnabled(preferences, categoryForNotification(notif))
    }
}
