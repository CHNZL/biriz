package com.example.util

import com.example.data.model.ScheduleConfig
import com.example.data.model.ScheduleLesson
import com.example.data.model.ScheduleTimelineItem
import java.util.Locale

data class LessonColorTheme(
    val key: String,
    val name: String,
    val primaryHex: String,
    val secondaryHex: String
)

object ScheduleHelper {

    val DAYS_OF_WEEK = listOf("PZT", "SAL", "ÇAR", "PER", "CUM")
    val DAY_NAMES = mapOf(
        "PZT" to "Pazartesi",
        "SAL" to "Salı",
        "ÇAR" to "Çarşamba",
        "PER" to "Perşembe",
        "CUM" to "Cuma"
    )

    /**
     * Ders düzenleme kartları için en az 2 satırda 20 zengin renk seçeneği
     */
    val LESSON_COLOR_PALETTE = listOf(
        // 1. Satır: Mavi, Turkuaz ve Sıcak Tonlar (10 Renk)
        LessonColorTheme("NAVY", "Lacivert", "#13294B", "#1E3A8A"),
        LessonColorTheme("BLUE", "Mavi", "#1D4ED8", "#3B82F6"),
        LessonColorTheme("TURQUOISE", "Turkuaz", "#0D9488", "#14B8A6"),
        LessonColorTheme("CYAN", "Camgöbeği", "#0891B2", "#06B6D4"),
        LessonColorTheme("EMERALD", "Zümrüt", "#047857", "#10B981"),
        LessonColorTheme("GREEN", "Yeşil", "#15803D", "#22C55E"),
        LessonColorTheme("LIME", "Fıstık Yeşili", "#4D7C0F", "#65A30D"),
        LessonColorTheme("AMBER", "Kehribar", "#D97706", "#F59E0B"),
        LessonColorTheme("ORANGE", "Turuncu", "#C2410C", "#EA580C"),
        LessonColorTheme("CORAL", "Mercan", "#BE123C", "#F43F5E"),
        // 2. Satır: Kırmızı, Pembe, Mor ve Toprak Tonları (10 Renk)
        LessonColorTheme("RED", "Kırmızı", "#B91C1C", "#EF4444"),
        LessonColorTheme("CRIMSON", "Vişne", "#7F1D1D", "#991B1B"),
        LessonColorTheme("PINK", "Pembe", "#BE185D", "#EC4899"),
        LessonColorTheme("ROSE", "Gül Kurusu", "#831843", "#BE185D"),
        LessonColorTheme("PURPLE", "Mor", "#5B21B6", "#7C3AED"),
        LessonColorTheme("VIOLET", "Menekşe", "#6D28D9", "#8B5CF6"),
        LessonColorTheme("INDIGO", "İndigo", "#3730A3", "#4F46E5"),
        LessonColorTheme("TEAL", "Petrol Mavisi", "#115E59", "#0F766E"),
        LessonColorTheme("BROWN", "Kahverengi", "#92400E", "#B45309"),
        LessonColorTheme("SLATE", "Arduvaz", "#1E293B", "#475569")
    )

    fun getColorsForTheme(themeKey: String?): Pair<String, String> {
        val found = LESSON_COLOR_PALETTE.firstOrNull { it.key.equals(themeKey, ignoreCase = true) }
        return if (found != null) {
            Pair(found.primaryHex, found.secondaryHex)
        } else {
            Pair("#13294B", "#1E3A8A")
        }
    }

    val PRESET_SUBJECTS = listOf(
        "TÜRKÇE",
        "MATEMATİK",
        "FEN BİLİMLERİ",
        "HAYAT BİLGİSİ",
        "SOSYAL BİLGİLER",
        "İNGİLİZCE",
        "GÖRSEL SANATLAR",
        "MÜZİK",
        "BEDEN EĞİTİMİ VE OYUN",
        "DİN KÜLTÜRÜ",
        "SERBEST ETKİNLİK",
        "BİLİŞİM VE KODLAMA",
        "REHBERLİK"
    )

    fun addMinutes(timeStr: String, minutes: Int): String {
        val parts = timeStr.trim().split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 30
        val totalMinutes = h * 60 + m + minutes
        val newH = (totalMinutes / 60) % 24
        val newM = totalMinutes % 60
        return String.format(Locale.US, "%02d:%02d", newH, newM)
    }

    /**
     * Zaman ve ders parametrelerine göre dikey zaman akışını (Timeline) hesaplar.
     */
    fun calculateTimeline(
        config: ScheduleConfig,
        lessonsForDay: List<ScheduleLesson>
    ): List<ScheduleTimelineItem> {
        var currentStart = config.firstLessonStartTime
        val items = mutableListOf<ScheduleTimelineItem>()
        var recessCounter = 1

        for (i in 1..config.dailyLessonCount) {
            // 1. Ders Yuvası (Lesson Slot)
            val lessonEnd = addMinutes(currentStart, config.lessonDurationMinutes)
            val lesson = lessonsForDay.firstOrNull { it.lessonIndex == i }
            items.add(
                ScheduleTimelineItem.LessonSlot(
                    lessonIndex = i,
                    startTime = currentStart,
                    endTime = lessonEnd,
                    lesson = lesson
                )
            )
            currentStart = lessonEnd

            // 2. Öğle Arası Kontrolü (Belirlenen dersten hemen sonra)
            if (config.isLunchBreakActive && i == config.lunchAfterLessonIndex) {
                val lunchEnd = addMinutes(currentStart, config.lunchDurationMinutes)
                items.add(
                    ScheduleTimelineItem.LunchBreakSlot(
                        afterLessonIndex = i,
                        startTime = currentStart,
                        endTime = lunchEnd,
                        durationMinutes = config.lunchDurationMinutes
                    )
                )
                currentStart = lunchEnd
            } else if (i < config.dailyLessonCount) {
                // 3. Teneffüs Yuvası (Recess Slot)
                val recessDur = config.recessDurations.getOrElse(recessCounter - 1) { 15 }
                val recessEnd = addMinutes(currentStart, recessDur)
                items.add(
                    ScheduleTimelineItem.RecessSlot(
                        recessIndex = recessCounter,
                        startTime = currentStart,
                        endTime = recessEnd,
                        durationMinutes = recessDur
                    )
                )
                currentStart = recessEnd
                recessCounter++
            }
        }

        return items
    }

    /**
     * Ayarlar ekranı için Öğle Arası başlangıç ve bitiş saatini hesaplar.
     */
    fun calculateLunchTimesPreview(config: ScheduleConfig): Pair<String, String> {
        var currentStart = config.firstLessonStartTime
        var recessCounter = 1

        for (i in 1..config.lunchAfterLessonIndex) {
            val lessonEnd = addMinutes(currentStart, config.lessonDurationMinutes)
            currentStart = lessonEnd

            if (i < config.lunchAfterLessonIndex) {
                val recessDur = config.recessDurations.getOrElse(recessCounter - 1) { 15 }
                currentStart = addMinutes(currentStart, recessDur)
                recessCounter++
            }
        }

        val lunchEnd = addMinutes(currentStart, config.lunchDurationMinutes)
        return Pair(currentStart, lunchEnd)
    }

    /**
     * Ders adına göre otomatik renk teması seçer
     */
    fun getColorThemeForSubject(subjectName: String): String {
        return when (subjectName.trim().lowercase(TurkishTextHelper.TR_LOCALE)) {
            "türkçe", "sosyal bilgiler", "türk dili" -> "NAVY"
            "matematik", "görsel sanatlar", "geometri" -> "AMBER"
            "fen bilimleri", "müzik", "bilişim ve kodlama" -> "TURQUOISE"
            "hayat bilgisi", "din kültürü", "rehberlik" -> "EMERALD"
            "beden eğitimi ve oyun", "yabancı dil", "ingilizce" -> "PURPLE"
            else -> "NAVY"
        }
    }

    /**
     * image_18.png ile birebir uyumlu başlangıç haftalık ders programı
     */
    fun generateDefaultWeeklySchedule(): List<ScheduleLesson> {
        val list = mutableListOf<ScheduleLesson>()

        // Pazartesi (image_18.png ile birebir uyumlu)
        list.add(ScheduleLesson("PZT", 1, "TÜRKÇE", "Heceleme ve Okuma", "", "NAVY"))
        list.add(ScheduleLesson("PZT", 2, "MATEMATİK", "Doğal Sayılar", "", "AMBER"))
        list.add(ScheduleLesson("PZT", 3, "FEN BİLİMLERİ", "Madde ve Özellikleri", "", "TURQUOISE"))
        list.add(ScheduleLesson("PZT", 4, "SOSYAL BİLGİLER", "Birey ve Toplum", "", "NAVY"))
        list.add(ScheduleLesson("PZT", 5, "MÜZİK", "Ritim Çalışması", "", "TURQUOISE"))
        list.add(ScheduleLesson("PZT", 6, "GÖRSEL SANATLAR", "Sulu Boya Çalışması", "", "AMBER"))

        // Salı
        list.add(ScheduleLesson("SAL", 1, "MATEMATİK", "Toplama ve Çıkarma", "", "AMBER"))
        list.add(ScheduleLesson("SAL", 2, "MATEMATİK", "Problem Çözme", "", "AMBER"))
        list.add(ScheduleLesson("SAL", 3, "TÜRKÇE", "Metin Anlama", "", "NAVY"))
        list.add(ScheduleLesson("SAL", 4, "HAYAT BİLGİSİ", "Okul Kuralları", "", "EMERALD"))
        list.add(ScheduleLesson("SAL", 5, "BEDEN EĞİTİMİ VE OYUN", "Bahçe Oyunları", "", "PURPLE"))
        list.add(ScheduleLesson("SAL", 6, "BEDEN EĞİTİMİ VE OYUN", "Koordinasyon", "", "PURPLE"))

        // Çarşamba
        list.add(ScheduleLesson("ÇAR", 1, "TÜRKÇE", "Gramer ve Yazım", "", "NAVY"))
        list.add(ScheduleLesson("ÇAR", 2, "FEN BİLİMLERİ", "Deney Saati", "", "TURQUOISE"))
        list.add(ScheduleLesson("ÇAR", 3, "SOSYAL BİLGİLER", "Kültürümüz", "", "NAVY"))
        list.add(ScheduleLesson("ÇAR", 4, "İNGİLİZCE", "Greetings & Numbers", "", "PURPLE"))
        list.add(ScheduleLesson("ÇAR", 5, "İNGİLİZCE", "Family Members", "", "PURPLE"))
        list.add(ScheduleLesson("ÇAR", 6, "SERBEST ETKİNLİK", "Akıl Oyunları", "", "AMBER"))

        // Perşembe
        list.add(ScheduleLesson("PER", 1, "TÜRKÇE", "Kitap Okuma", "", "NAVY"))
        list.add(ScheduleLesson("PER", 2, "TÜRKÇE", "Yaratıcı Yazarlık", "", "NAVY"))
        list.add(ScheduleLesson("PER", 3, "MATEMATİK", "Şekiller ve Geometri", "", "AMBER"))
        list.add(ScheduleLesson("PER", 4, "DİN KÜLTÜRÜ", "Ahlaki Değerler", "", "EMERALD"))
        list.add(ScheduleLesson("PER", 5, "GÖRSEL SANATLAR", "Origami", "", "AMBER"))
        list.add(ScheduleLesson("PER", 6, "MÜZİK", "Şarkı Söyleme", "", "TURQUOISE"))

        // Cuma
        list.add(ScheduleLesson("CUM", 1, "MATEMATİK", "Haftalık Tekrar", "", "AMBER"))
        list.add(ScheduleLesson("CUM", 2, "FEN BİLİMLERİ", "Çevre Bilinci", "", "TURQUOISE"))
        list.add(ScheduleLesson("CUM", 3, "HAYAT BİLGİSİ", "Sağlıklı Yaşam", "", "EMERALD"))
        list.add(ScheduleLesson("CUM", 4, "SOSYAL BİLGİLER", "Milli Değerler", "", "NAVY"))
        list.add(ScheduleLesson("CUM", 5, "SERBEST ETKİNLİK", "Haftalık Değerlendirme", "", "AMBER"))
        list.add(ScheduleLesson("CUM", 6, "REHBERLİK", "Grup Etkinliği", "", "EMERALD"))

        return list
    }

    /**
     * Telefonun o anki saatine ve haftanın gününe göre dinamik ders/teneffüs durumunu hesaplar.
     */
    fun calculateLiveScheduleStatus(
        config: ScheduleConfig,
        allLessons: List<ScheduleLesson>
    ): LiveLessonStatus {
        val cal = java.util.Calendar.getInstance()
        val dayOfWeekInt = cal.get(java.util.Calendar.DAY_OF_WEEK)
        val currentDayKey = when (dayOfWeekInt) {
            java.util.Calendar.MONDAY -> "PZT"
            java.util.Calendar.TUESDAY -> "SAL"
            java.util.Calendar.WEDNESDAY -> "ÇAR"
            java.util.Calendar.THURSDAY -> "PER"
            java.util.Calendar.FRIDAY -> "CUM"
            else -> null
        }

        if (currentDayKey == null) {
            return LiveLessonStatus(
                title = "📅 Hafta Sonu Dinlenme Zamanı",
                subtitle = "Pazartesi günü dersler saat ${config.firstLessonStartTime}'da başlayacaktır.",
                iconType = "WEEKEND",
                isLive = false
            )
        }

        val currentMinutes = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
        val todayLessons = allLessons.filter { it.dayOfWeek == currentDayKey }
        val timeline = calculateTimeline(config, todayLessons)

        fun parseTimeToMinutes(timeStr: String): Int {
            val parts = timeStr.split(":")
            return (parts.getOrNull(0)?.toIntOrNull() ?: 0) * 60 + (parts.getOrNull(1)?.toIntOrNull() ?: 0)
        }

        val lessonSlots = timeline.filterIsInstance<ScheduleTimelineItem.LessonSlot>()
        val firstSlotStart = lessonSlots.firstOrNull()?.let { parseTimeToMinutes(it.startTime) } ?: (8 * 60 + 30)
        val lastSlotEnd = lessonSlots.lastOrNull()?.let { parseTimeToMinutes(it.endTime) } ?: (14 * 60 + 30)

        if (currentMinutes < firstSlotStart) {
            val remaining = firstSlotStart - currentMinutes
            val firstLessonName = lessonSlots.firstOrNull()?.lesson?.subjectName ?: "1. Ders"
            return LiveLessonStatus(
                title = "⏰ Derslerin Başlamasına $remaining dk Kaldı",
                subtitle = "İlk Ders: 1. Ders - $firstLessonName (${config.firstLessonStartTime})",
                iconType = "LESSON",
                isLive = true
            )
        }

        if (currentMinutes >= lastSlotEnd) {
            return LiveLessonStatus(
                title = "🌙 Bugünlük Dersler Tamamlandı",
                subtitle = "İyi dinlenmeler dileriz!",
                iconType = "OUT_OF_SCHOOL",
                isLive = false
            )
        }

        for (item in timeline) {
            when (item) {
                is ScheduleTimelineItem.LessonSlot -> {
                    val start = parseTimeToMinutes(item.startTime)
                    val end = parseTimeToMinutes(item.endTime)
                    if (currentMinutes in start until end) {
                        val remaining = maxOf(1, end - currentMinutes)
                        val subject = item.lesson?.subjectName?.ifBlank { "Ders" } ?: "Ders"
                        return LiveLessonStatus(
                            title = "🔔 Şu An: ${item.lessonIndex}. Ders - $subject (Bitişe $remaining dk kaldı)",
                            subtitle = "Ders Aralığı: ${item.startTime} - ${item.endTime}",
                            iconType = "LESSON",
                            isLive = true
                        )
                    }
                }
                is ScheduleTimelineItem.RecessSlot -> {
                    val start = parseTimeToMinutes(item.startTime)
                    val end = parseTimeToMinutes(item.endTime)
                    if (currentMinutes in start until end) {
                        val remaining = maxOf(1, end - currentMinutes)
                        val nextLessonIdx = item.recessIndex + 1
                        val nextLesson = lessonSlots.firstOrNull { it.lessonIndex == nextLessonIdx }?.lesson?.subjectName ?: "$nextLessonIdx. Ders"
                        return LiveLessonStatus(
                            title = "☕ Teneffüstesiniz ($nextLessonIdx. Dersin başlamasına $remaining dk kaldı)",
                            subtitle = "Gelecek Ders: $nextLesson (Giriş Zili: ${item.endTime})",
                            iconType = "RECESS",
                            isLive = true
                        )
                    }
                }
                is ScheduleTimelineItem.LunchBreakSlot -> {
                    val start = parseTimeToMinutes(item.startTime)
                    val end = parseTimeToMinutes(item.endTime)
                    if (currentMinutes in start until end) {
                        val remaining = maxOf(1, end - currentMinutes)
                        val nextLessonIdx = item.afterLessonIndex + 1
                        val nextLesson = lessonSlots.firstOrNull { it.lessonIndex == nextLessonIdx }?.lesson?.subjectName ?: "$nextLessonIdx. Ders"
                        return LiveLessonStatus(
                            title = "🍽️ Öğle Arasındasınız ($nextLessonIdx. Dersin başlamasına $remaining dk kaldı)",
                            subtitle = "Gelecek Ders: $nextLesson (Giriş Zili: ${item.endTime})",
                            iconType = "LUNCH",
                            isLive = true
                        )
                    }
                }
            }
        }

        return LiveLessonStatus(
            title = "📚 Ders Programı",
            subtitle = "Günlük ders akışı aktif.",
            iconType = "LESSON",
            isLive = true
        )
    }
}

data class LiveLessonStatus(
    val title: String,
    val subtitle: String,
    val iconType: String,
    val isLive: Boolean
)
