package com.example.util

import com.example.data.model.ChatMessage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class SchedulePresetOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val targetMillis: Long,
    val isRecommended: Boolean = false
)

/**
 * WhatsApp stili mesajlaşma özellikleri için saf iş mantığı yardımcısı:
 * - Hızlı emoji tepkileri (reactions) & katılımcı anahtarı
 * - Sessiz saatler algılama ve zamanlanmış iletim zamanı hesaplama
 * - Detaylı iletildi/okundu zaman damgası biçimlendirme
 */
object ChatFeatureHelper {

    val QUICK_REACTIONS: List<String> = listOf("👍", "❤️", "👀", "😂", "🙏", "👏")

    const val QUIET_EVENING_START_HOUR = 19
    const val QUIET_MORNING_END_HOUR = 8

    fun isQuietHours(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        return hour >= QUIET_EVENING_START_HOUR || hour < QUIET_MORNING_END_HOUR
    }

    /**
     * Bir sonraki uygun sabah (varsayılan 08:30) veya seçili saat için zaman damgası döndürür.
     * Eğer o günün hedef saati henüz geçmemişse (en az 2 dk payla) bugüne, geçmişse yarına kurar.
     */
    fun nextTargetSlotMillis(
        hour: Int,
        minute: Int,
        nowMillis: Long = System.currentTimeMillis(),
        forceTomorrow: Boolean? = null
    ): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val shouldMoveTomorrow = when (forceTomorrow) {
            true -> true
            false -> false
            null -> cal.timeInMillis <= nowMillis + 120_000L
        }
        if (shouldMoveTomorrow) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    fun buildSchedulePresets(nowMillis: Long = System.currentTimeMillis()): List<SchedulePresetOption> {
        val trLocale = Locale("tr", "TR")
        val slot0830 = nextTargetSlotMillis(8, 30, nowMillis)
        val slot0900 = nextTargetSlotMillis(9, 0, nowMillis)
        val slot1230 = nextTargetSlotMillis(12, 30, nowMillis)

        fun dayPrefix(target: Long): String {
            val cNow = Calendar.getInstance().apply { timeInMillis = nowMillis }
            val cTarget = Calendar.getInstance().apply { timeInMillis = target }
            val sameDay = cNow.get(Calendar.YEAR) == cTarget.get(Calendar.YEAR) &&
                cNow.get(Calendar.DAY_OF_YEAR) == cTarget.get(Calendar.DAY_OF_YEAR)
            return if (sameDay) "Bugün" else "Yarın"
        }

        val timeFmt = SimpleDateFormat("HH:mm", trLocale)
        val dateFmt = SimpleDateFormat("d MMM EEEE", trLocale)

        return listOf(
            SchedulePresetOption(
                id = "morning_0830",
                title = "${dayPrefix(slot0830)} 08:30'da İlet",
                subtitle = "${dateFmt.format(Date(slot0830))} • İlk ders zili",
                targetMillis = slot0830,
                isRecommended = true
            ),
            SchedulePresetOption(
                id = "morning_0900",
                title = "${dayPrefix(slot0900)} ${timeFmt.format(Date(slot0900))}'da İlet",
                subtitle = dateFmt.format(Date(slot0900)),
                targetMillis = slot0900,
                isRecommended = false
            ),
            SchedulePresetOption(
                id = "noon_1230",
                title = "${dayPrefix(slot1230)} ${timeFmt.format(Date(slot1230))}'da İlet",
                subtitle = "${dateFmt.format(Date(slot1230))} • Öğle arası",
                targetMillis = slot1230,
                isRecommended = false
            )
        )
    }

    fun formatScheduledLabel(
        scheduledFor: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        val calNow = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val calTarget = Calendar.getInstance().apply { timeInMillis = scheduledFor }
        val trLocale = Locale("tr", "TR")
        val timeText = SimpleDateFormat("HH:mm", trLocale).format(Date(scheduledFor))

        val sameDay = calNow.get(Calendar.YEAR) == calTarget.get(Calendar.YEAR) &&
            calNow.get(Calendar.DAY_OF_YEAR) == calTarget.get(Calendar.DAY_OF_YEAR)
        if (sameDay) return "Bugün $timeText"

        val calTomorrow = (calNow.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        val isTomorrow = calTomorrow.get(Calendar.YEAR) == calTarget.get(Calendar.YEAR) &&
            calTomorrow.get(Calendar.DAY_OF_YEAR) == calTarget.get(Calendar.DAY_OF_YEAR)
        if (isTomorrow) return "Yarın $timeText"

        val dateText = SimpleDateFormat("d MMM", trLocale).format(Date(scheduledFor))
        return "$dateText $timeText"
    }

    fun formatDetailedDateTime(
        timestamp: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        val calNow = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val calTarget = Calendar.getInstance().apply { timeInMillis = timestamp }
        val trLocale = Locale("tr", "TR")
        val timeText = SimpleDateFormat("HH:mm:ss", trLocale).format(Date(timestamp))

        val sameDay = calNow.get(Calendar.YEAR) == calTarget.get(Calendar.YEAR) &&
            calNow.get(Calendar.DAY_OF_YEAR) == calTarget.get(Calendar.DAY_OF_YEAR)
        if (sameDay) return "Bugün, $timeText"

        val calYesterday = (calNow.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        val isYesterday = calYesterday.get(Calendar.YEAR) == calTarget.get(Calendar.YEAR) &&
            calYesterday.get(Calendar.DAY_OF_YEAR) == calTarget.get(Calendar.DAY_OF_YEAR)
        if (isYesterday) return "Dün, $timeText"

        val dateText = SimpleDateFormat("d MMMM yyyy", trLocale).format(Date(timestamp))
        return "$dateText, $timeText"
    }

    fun reactionParticipantKey(
        viewerIsTeacher: Boolean,
        userEmail: String,
        studentId: String
    ): String {
        if (viewerIsTeacher) return "teacher"
        val clean = userEmail.trim().lowercase(Locale.ROOT)
        val suffix = if (clean.isNotBlank() && clean != "veli@biriz.app") {
            clean.replace(".", "_")
        } else {
            studentId.ifBlank { "default" }.replace(".", "_")
        }
        return "parent_$suffix"
    }

    fun reactionParticipantDisplayLabel(
        participantKey: String,
        message: ChatMessage,
        teacherDisplayName: String = "Sınıf Öğretmeni"
    ): String {
        if (participantKey == "teacher") {
            return "$teacherDisplayName (Öğretmen)"
        }
        val cleanKeyEmail = participantKey.removePrefix("parent_").replace("_", ".")
        if (message.parentName.isNotBlank() &&
            (message.parentEmail.isBlank() ||
                message.parentEmail.trim().lowercase(Locale.ROOT) == cleanKeyEmail)
        ) {
            return "${message.parentName} (Veli)"
        }
        return if (cleanKeyEmail.contains("@")) cleanKeyEmail else "Veli"
    }

    /**
     * Tepki haritasını günceller. Aynı emojiye tekrar basılırsa tepki kaldırılır ("" değeri döner).
     * @return Pair(yeni temizlenmiş harita, Firestore'a yazılacak değer - kaldırıldıysa "")
     */
    fun toggleReaction(
        currentReactions: Map<String, String>,
        participantKey: String,
        emoji: String
    ): Pair<Map<String, String>, String> {
        val cleanEmoji = emoji.trim()
        val activeOnly = currentReactions.filterValues { it.isNotBlank() }.toMutableMap()
        val existing = activeOnly[participantKey]
        return if (existing == cleanEmoji || cleanEmoji.isEmpty()) {
            activeOnly.remove(participantKey)
            activeOnly to ""
        } else {
            activeOnly[participantKey] = cleanEmoji
            activeOnly to cleanEmoji
        }
    }

    /**
     * Balon altında gösterilecek tepki özetini [(👍, 2), (❤️, 1)] şeklinde döndürür.
     */
    fun summarizeReactions(reactions: Map<String, String>): List<Pair<String, Int>> {
        val active = reactions.values.filter { it.isNotBlank() }
        if (active.isEmpty()) return emptyList()
        val counts = LinkedHashMap<String, Int>()
        active.forEach { emoji ->
            counts[emoji] = (counts[emoji] ?: 0) + 1
        }
        return counts.entries.map { it.key to it.value }
    }
}
