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
    PARENT_SECURITY(
        key = "parent_security",
        title = "Güvenlik & İkinci Veli",
        description = "Çocuğunuza davet koduyla başka bir veli hesabı bağlandığında güvenlik uyarısı",
        emoji = "🔐",
        forParent = true,
        forTeacher = false
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

data class QuietHoursConfig(
    val enabled: Boolean = false,
    val startHour: Int = 22,
    val startMinute: Int = 0,
    val endHour: Int = 8,
    val endMinute: Int = 0
)

/**
 * Öğretmenin gece saatlerinde yaptığı sınıf veri girişlerinin velilere anında bildirim
 * olarak gitmesini engelleyip belirlediği sabah saatinde (örn. 10:00) topluca iletilmesini
 * sağlayan sınıf geneli bildirim erteleme ayarı.
 */
data class ParentNotificationDeferralConfig(
    val enabled: Boolean = false,
    val startHour: Int = 21,
    val startMinute: Int = 0,
    val deliverHour: Int = 10,
    val deliverMinute: Int = 0
) {
    fun toQuietHoursConfig(): QuietHoursConfig = QuietHoursConfig(
        enabled = enabled,
        startHour = startHour.coerceIn(0, 23),
        startMinute = startMinute.coerceIn(0, 59),
        endHour = deliverHour.coerceIn(0, 23),
        endMinute = deliverMinute.coerceIn(0, 59)
    )
}

data class DeferredSystemNotification(
    val notification: BirizNotification,
    val isRecipientTeacher: Boolean
)

object NotificationPreferencesHelper {

    private const val PREFS_NAME = "biriz_notification_prefs"
    private const val KEY_PREFIX = "notif_pref_"
    private const val KEY_QUIET_PREFIX = "quiet_hours_"
    private const val KEY_DEFERRED_PREFIX = "quiet_deferred_"
    private const val KEY_TEACHER_DEFERRAL_PREFIX = "teacher_parent_deferral_"

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
        if (notif.id.startsWith("notif_parent_sec_link_") || notif.id.startsWith("parent_sec_link_")) {
            return NotificationCategory.PARENT_SECURITY
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

    // --- Sessiz Saatler (Gece Koruması) ---

    fun loadQuietHours(context: Context, viewerKey: String): QuietHoursConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val scope = viewerKey.ifBlank { "default" }
        return QuietHoursConfig(
            enabled = prefs.getBoolean("${KEY_QUIET_PREFIX}${scope}_enabled", false),
            startHour = prefs.getInt("${KEY_QUIET_PREFIX}${scope}_start_h", 22).coerceIn(0, 23),
            startMinute = prefs.getInt("${KEY_QUIET_PREFIX}${scope}_start_m", 0).coerceIn(0, 59),
            endHour = prefs.getInt("${KEY_QUIET_PREFIX}${scope}_end_h", 8).coerceIn(0, 23),
            endMinute = prefs.getInt("${KEY_QUIET_PREFIX}${scope}_end_m", 0).coerceIn(0, 59)
        )
    }

    fun saveQuietHours(context: Context, viewerKey: String, config: QuietHoursConfig) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val scope = viewerKey.ifBlank { "default" }
        prefs.edit()
            .putBoolean("${KEY_QUIET_PREFIX}${scope}_enabled", config.enabled)
            .putInt("${KEY_QUIET_PREFIX}${scope}_start_h", config.startHour.coerceIn(0, 23))
            .putInt("${KEY_QUIET_PREFIX}${scope}_start_m", config.startMinute.coerceIn(0, 59))
            .putInt("${KEY_QUIET_PREFIX}${scope}_end_h", config.endHour.coerceIn(0, 23))
            .putInt("${KEY_QUIET_PREFIX}${scope}_end_m", config.endMinute.coerceIn(0, 59))
            .apply()
    }

    fun formatTime(hour: Int, minute: Int): String {
        val hh = hour.coerceIn(0, 23).toString().padStart(2, '0')
        val mm = minute.coerceIn(0, 59).toString().padStart(2, '0')
        return "$hh:$mm"
    }

    fun stepTimeByMinutes(hour: Int, minute: Int, deltaMinutes: Int): Pair<Int, Int> {
        val total = (hour.coerceIn(0, 23) * 60 + minute.coerceIn(0, 59) + deltaMinutes)
            .mod(24 * 60)
        return (total / 60) to (total % 60)
    }

    fun formatQuietHoursRange(config: QuietHoursConfig): String {
        return "${formatTime(config.startHour, config.startMinute)} – ${formatTime(config.endHour, config.endMinute)}"
    }

    fun formatDeferralRange(config: ParentNotificationDeferralConfig): String {
        return "${formatTime(config.startHour, config.startMinute)} – ${formatTime(config.deliverHour, config.deliverMinute)}"
    }

    fun loadTeacherParentDeferral(context: Context, classId: String): ParentNotificationDeferralConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val scope = classId.trim().ifBlank { "default" }
        return ParentNotificationDeferralConfig(
            enabled = prefs.getBoolean("${KEY_TEACHER_DEFERRAL_PREFIX}${scope}_enabled", false),
            startHour = prefs.getInt("${KEY_TEACHER_DEFERRAL_PREFIX}${scope}_start_h", 21).coerceIn(0, 23),
            startMinute = prefs.getInt("${KEY_TEACHER_DEFERRAL_PREFIX}${scope}_start_m", 0).coerceIn(0, 59),
            deliverHour = prefs.getInt("${KEY_TEACHER_DEFERRAL_PREFIX}${scope}_deliver_h", 10).coerceIn(0, 23),
            deliverMinute = prefs.getInt("${KEY_TEACHER_DEFERRAL_PREFIX}${scope}_deliver_m", 0).coerceIn(0, 59)
        )
    }

    fun saveTeacherParentDeferral(
        context: Context,
        classId: String,
        config: ParentNotificationDeferralConfig
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val scope = classId.trim().ifBlank { "default" }
        prefs.edit()
            .putBoolean("${KEY_TEACHER_DEFERRAL_PREFIX}${scope}_enabled", config.enabled)
            .putInt("${KEY_TEACHER_DEFERRAL_PREFIX}${scope}_start_h", config.startHour.coerceIn(0, 23))
            .putInt("${KEY_TEACHER_DEFERRAL_PREFIX}${scope}_start_m", config.startMinute.coerceIn(0, 59))
            .putInt("${KEY_TEACHER_DEFERRAL_PREFIX}${scope}_deliver_h", config.deliverHour.coerceIn(0, 23))
            .putInt("${KEY_TEACHER_DEFERRAL_PREFIX}${scope}_deliver_m", config.deliverMinute.coerceIn(0, 59))
            .apply()
    }

    fun isWithinDeferralWindow(
        config: ParentNotificationDeferralConfig,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (!config.enabled) return false
        return isWithinQuietHours(config.toQuietHoursConfig(), nowMillis)
    }

    fun isWithinQuietHours(
        config: QuietHoursConfig,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (!config.enabled) return false
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = nowMillis }
        val currentMinutes = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
        val startMinutes = config.startHour.coerceIn(0, 23) * 60 + config.startMinute.coerceIn(0, 59)
        val endMinutes = config.endHour.coerceIn(0, 23) * 60 + config.endMinute.coerceIn(0, 59)

        if (startMinutes == endMinutes) return false
        return if (startMinutes < endMinutes) {
            currentMinutes in startMinutes until endMinutes
        } else {
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        }
    }

    fun nextQuietHoursEndMillis(
        config: QuietHoursConfig,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        val endMinutes = config.endHour.coerceIn(0, 23) * 60 + config.endMinute.coerceIn(0, 59)
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(java.util.Calendar.HOUR_OF_DAY, config.endHour.coerceIn(0, 23))
            set(java.util.Calendar.MINUTE, config.endMinute.coerceIn(0, 59))
            set(java.util.Calendar.SECOND, 5)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val currentCal = java.util.Calendar.getInstance().apply { timeInMillis = nowMillis }
        val currentMinutes = currentCal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + currentCal.get(java.util.Calendar.MINUTE)
        if (currentMinutes >= endMinutes) {
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    fun isSecurityCriticalNotification(notif: BirizNotification): Boolean {
        return notif.id.startsWith("notif_parent_sec_link_") ||
            notif.id.startsWith("parent_sec_link_")
    }

    fun shouldDeferForQuietHours(
        config: QuietHoursConfig,
        notif: BirizNotification,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (!config.enabled) return false
        if (notif.type == NotificationType.APP_UPDATE) return false
        if (isSecurityCriticalNotification(notif)) return false
        return isWithinQuietHours(config, nowMillis)
    }

    /**
     * Veli cihazında hem velinin kendi Sessiz Saatler ayarını hem de öğretmenin
     * sınıf geneli Veli Bildirim Erteleme (Gece Çalışma Modu) ayarını birlikte değerlendirir.
     * İkisinden biri aktifse (güvenlik uyarıları ve uygulama güncellemeleri hariç) bildirimi erteler.
     */
    fun shouldDeferForParent(
        personalQuiet: QuietHoursConfig,
        teacherDeferral: ParentNotificationDeferralConfig,
        notif: BirizNotification,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (notif.type == NotificationType.APP_UPDATE) return false
        if (isSecurityCriticalNotification(notif)) return false
        return isWithinQuietHours(personalQuiet, nowMillis) ||
            isWithinDeferralWindow(teacherDeferral, nowMillis)
    }

    /**
     * Velinin kişisel Sessiz Saatleri ile öğretmenin Veli Bildirim Erteleme penceresi
     * çakıştığında, iki korumanın da bittiği en erken teslim zamanını hesaplar.
     */
    fun nextEffectiveParentDeliveryMillis(
        personalQuiet: QuietHoursConfig,
        teacherDeferral: ParentNotificationDeferralConfig,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        val teacherAsQuiet = teacherDeferral.toQuietHoursConfig()
        val hasPersonal = personalQuiet.enabled
        val hasTeacher = teacherAsQuiet.enabled
        if (!hasPersonal && !hasTeacher) return nowMillis

        var candidate = nowMillis
        // Eğer şu an aktif olan bir pencere varsa, her iki pencere de bitene dek ileri al
        var advanced = false
        for (step in 0 until 4) {
            val inPersonal = hasPersonal && isWithinQuietHours(personalQuiet, candidate)
            val inTeacher = hasTeacher && isWithinQuietHours(teacherAsQuiet, candidate)
            if (!inPersonal && !inTeacher) break
            var nextCand = candidate
            if (inPersonal) {
                nextCand = maxOf(nextCand, nextQuietHoursEndMillis(personalQuiet, candidate))
            }
            if (inTeacher) {
                nextCand = maxOf(nextCand, nextQuietHoursEndMillis(teacherAsQuiet, candidate))
            }
            if (nextCand <= candidate) break
            candidate = nextCand
            advanced = true
        }
        if (advanced) return candidate

        // Şu an iki pencere de aktif değilse, bir sonraki planlı bitiş saatini döndür
        val candidates = buildList {
            if (hasPersonal) add(nextQuietHoursEndMillis(personalQuiet, nowMillis))
            if (hasTeacher) add(nextQuietHoursEndMillis(teacherAsQuiet, nowMillis))
        }
        return candidates.minOrNull() ?: nowMillis
    }

    fun enqueueDeferredNotifications(
        context: Context,
        viewerKey: String,
        items: List<DeferredSystemNotification>
    ): Int {
        if (items.isEmpty()) return peekDeferredCount(context, viewerKey)
        val scope = viewerKey.ifBlank { "default" }
        val existing = loadDeferredNotifications(context, scope).toMutableList()
        items.forEach { entry ->
            if (existing.none { it.notification.id == entry.notification.id }) {
                existing.add(entry)
            }
        }
        val capped = existing.takeLast(80)
        saveDeferredNotifications(context, scope, capped)
        return capped.size
    }

    fun peekDeferredCount(context: Context, viewerKey: String): Int {
        return loadDeferredNotifications(context, viewerKey).size
    }

    fun drainDeferredNotifications(
        context: Context,
        viewerKey: String
    ): List<DeferredSystemNotification> {
        val scope = viewerKey.ifBlank { "default" }
        val items = loadDeferredNotifications(context, scope)
        if (items.isNotEmpty()) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().remove("${KEY_DEFERRED_PREFIX}$scope").apply()
        }
        return items
    }

    private fun loadDeferredNotifications(
        context: Context,
        viewerKey: String
    ): List<DeferredSystemNotification> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val scope = viewerKey.ifBlank { "default" }
        val raw = prefs.getString("${KEY_DEFERRED_PREFIX}$scope", null) ?: return emptyList()
        return try {
            val arr = org.json.JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val id = obj.optString("id", "").trim()
                    if (id.isBlank()) continue
                    val parsedType = try {
                        NotificationType.valueOf(obj.optString("type", NotificationType.ANNOUNCEMENT.name))
                    } catch (_: Exception) {
                        NotificationType.ANNOUNCEMENT
                    }
                    add(
                        DeferredSystemNotification(
                            notification = BirizNotification(
                                id = id,
                                title = obj.optString("title", ""),
                                message = obj.optString("message", ""),
                                timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                                type = parsedType,
                                targetSubPageName = obj.optString("targetSubPageName", "HOME"),
                                targetId = obj.optString("targetId", "").takeIf { it.isNotBlank() },
                                isRead = false
                            ),
                            isRecipientTeacher = obj.optBoolean("isRecipientTeacher", false)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveDeferredNotifications(
        context: Context,
        viewerKey: String,
        items: List<DeferredSystemNotification>
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val scope = viewerKey.ifBlank { "default" }
        val arr = org.json.JSONArray()
        items.forEach { item ->
            val obj = org.json.JSONObject().apply {
                put("id", item.notification.id)
                put("title", item.notification.title)
                put("message", item.notification.message)
                put("timestamp", item.notification.timestamp)
                put("type", item.notification.type.name)
                put("targetSubPageName", item.notification.targetSubPageName)
                if (!item.notification.targetId.isNullOrBlank()) {
                    put("targetId", item.notification.targetId)
                }
                put("isRecipientTeacher", item.isRecipientTeacher)
            }
            arr.put(obj)
        }
        prefs.edit().putString("${KEY_DEFERRED_PREFIX}$scope", arr.toString()).apply()
    }
}
