package com.example.data.model

/**
 * BİRİZ Kullanıcı Rolleri:
 * - UYE: Sisteme giriş yapmış ancak henüz rol seçmemiş varsayılan kullanıcı
 * - TEACHER: Sınıf kurmuş veya kurmakta olan yetkili sınıf öğretmeni
 * - PARENT: Veli rolü (Şu an kilitli / yakında aktif)
 */
enum class UserRole(val code: String, val displayName: String, val description: String) {
    MEMBER("UYE", "Üye", "Henüz bir rol seçilmemiş başlangıç hesabı"),
    TEACHER("OGRETMEN", "Öğretmen", "Sınıf yöneticisi, ödev ve veli iletişim koordinatörü"),
    PARENT("VELI", "Veli (Yakında)", "Öğrenci gelişim takibi ve öğretmen iletişimi")
}

data class City(
    val id: String, // Plaka kodu, örn: "58"
    val name: String, // İl adı, örn: "Sivas"
    val districts: List<String>
) {
    val plateCode: String get() = id
}

data class School(
    val id: String,
    val name: String, // Her zaman Türkçe BÜYÜK HARFLERLE saklanır
    val cityName: String,
    val districtName: String,
    val cityId: String = "",
    val isCustomAdded: Boolean = false
)

data class ClassRoom(
    val id: String,
    val cityName: String,
    val districtName: String,
    val schoolName: String,
    val gradeLevel: Int, // 1, 2, 3, 4
    val section: String, // "A" .. "Z"
    val teacherId: String = "",
    val teacherName: String,
    val teacherEmail: String = "",
    val inviteCode: String = "",
    val studentCount: Int = 0,
    val isOccupied: Boolean = true
)

data class Student(
    val id: String,
    val studentNumber: String,
    val fullName: String, // Her zaman Türkçe BÜYÜK HARFLERLE
    val gender: String, // "KIZ" veya "ERKEK"
    val parentInviteCode: String = "",
    val isPriority: Boolean = false, // Ön sıra önceliği (gözlük, dikkat, işitme, boy)
    val isFixed: Boolean = false,    // Sıraya sabitlenmiş mi?
    val createdAt: Long = System.currentTimeMillis(),
    val parentName: String? = null,
    val parentEmail: String? = null,
    val parentLinkedAt: String? = null,
    val parentLastLoginAt: String? = null
) {
    val isParentLinked: Boolean get() = !parentName.isNullOrBlank() || !parentEmail.isNullOrBlank()
}

data class BranchStatus(
    val section: String,
    val isOccupied: Boolean,
    val assignedTeacherName: String? = null,
    val assignedTeacherEmail: String? = null
)

/**
 * Esnek Ders Programı Zaman ve Ders Ayarları
 */
data class ScheduleConfig(
    val dailyLessonCount: Int = 6, // 1..10
    val firstLessonStartTime: String = "08:30",
    val lessonDurationMinutes: Int = 40,
    val recessDurations: List<Int> = listOf(15, 15, 10, 15, 15), // Her teneffüs için ayrı esnek süre
    val isLunchBreakActive: Boolean = true,
    val lunchAfterLessonIndex: Int = 4, // Kaçıncı dersten sonra öğle arası (Örn: 4. veya 3. ders)
    val lunchDurationMinutes: Int = 50,
    val isConfigured: Boolean = true
)

/**
 * Haftalık Ders Programındaki Tekil Ders Kaydı
 */
data class ScheduleLesson(
    val dayOfWeek: String, // "PZT", "SAL", "ÇAR", "PER", "CUM"
    val lessonIndex: Int, // 1..dailyLessonCount
    val subjectName: String,
    val topicOrNotes: String = "",
    val teacherName: String = "",
    val colorTheme: String = "NAVY" // "NAVY", "AMBER", "TURQUOISE", "PURPLE"
)

sealed class ScheduleTimelineItem {
    data class LessonSlot(
        val lessonIndex: Int,
        val startTime: String,
        val endTime: String,
        val lesson: ScheduleLesson?
    ) : ScheduleTimelineItem()

    data class RecessSlot(
        val recessIndex: Int,
        val startTime: String,
        val endTime: String,
        val durationMinutes: Int
    ) : ScheduleTimelineItem()

    data class LunchBreakSlot(
        val afterLessonIndex: Int,
        val startTime: String,
        val endTime: String,
        val durationMinutes: Int
    ) : ScheduleTimelineItem()
}

/**
 * Sıra Düzeni ve Oturma Planı Konfigürasyonu
 */
enum class WindowPosition(val title: String) {
    LEFT("Sol Cephe"),
    RIGHT("Sağ Cephe"),
    BACK("Arka Cephe"),
    NONE("Penceresiz")
}

enum class DoorPosition(val title: String, val facade: String, val positionOnFacade: String) {
    SOL_ON("Sol Cephe - Ön", "Sol", "Ön"),
    SOL_ARKA("Sol Cephe - Arka", "Sol", "Arka"),
    SAG_ON("Sağ Cephe - Ön", "Sağ", "Ön"),
    SAG_ARKA("Sağ Cephe - Arka", "Sağ", "Arka"),
    ON_SOL("Ön Cephe - Sol", "Ön", "Sol"),
    ON_SAG("Ön Cephe - Sağ", "Ön", "Sağ"),
    ARKA_SOL("Arka Cephe - Sol", "Arka", "Sol"),
    ARKA_SAG("Arka Cephe - Sağ", "Arka", "Sağ"),
    NONE("Kapısız", "Yok", "Yok");

    companion object {
        val LEFT get() = SOL_ARKA
        val RIGHT get() = SAG_ON
    }
}

enum class DeskPosition(val title: String) {
    LEFT("Sol Ön"),
    CENTER("Orta Ön"),
    RIGHT("Sağ Ön")
}

enum class SeatingPlacementMode(val title: String, val description: String) {
    PLANNED("Planlı Yerleştirme", "Açılan pencerede gerekli ayarlar yapılarak yerleştirme yapılır."),
    RANDOM("Rastgele Dağıt", "Tüm Öğrenciler rastgele dağıtılır."),
    GENDER_BALANCED("Kız-Erkek Dengeli", "Kız ve erkek öğrencileri sıralara dengeli ve yan yana yerleştirir"),
    ALPHABETICAL("İsme Göre", "Öğrencileri ad ve soyadlarına göre alfabetik sırayla yerleştirir"),
    NUMBER_ORDER("Numaraya Göre", "Öğrencileri okul numarası sırasına göre yerleştirir"),
    MANUAL("Elle Yerleştirme", "Öğrenciler tek tek öğretmenin tercihine göre yerleştirilir."),
    PRIORITY_FRONT("Önceliklileri Öne Al", "Gözlük, dikkat veya işitme önceliği olan öğrencileri en ön sıralara yerleştirir")
}

data class PlannedSeatingRules(
    val genderMixed: Boolean = true,      // Kız / Erkek karışık otursun
    val differentGroup: Boolean = true,   // Her öğrenci farklı gruba yerleştirilsin
    val differentRow: Boolean = true,     // Her öğrenci farklı sıraya yerleştirilsin
    val differentNeighbor: Boolean = true, // Her öğrenci farklı kişi ile otursun
    val incompatiblePairs: List<Pair<String, String>> = emptyList() // Yan yana / aynı sıraya oturmaması gereken öğrenci çiftleri
)

data class FixedStudentPlacement(
    val studentId: String,
    val groupIndex: Int,
    val rowIndex: Int,
    val seatIndex: Int
) {
    val seatId: String get() = "g$groupIndex-r$rowIndex-s$seatIndex"
}

data class ArchivedSeatingPlan(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val planState: SeatingPlanState
)

data class SeatingConfig(
    val groupCount: Int = 3, // Kolon / Grup sayısı (Varsayılan 3)
    val peoplePerRow: Int = 2, // Sıra başına oturan öğrenci sayısı (1: Tekli, 2: İkili, 3: Üçlü, 4: Dörtlü)
    val rowsPerGroup: List<Int> = listOf(5, 5, 5), // Her grubun satır/sıra sayısı
    val windowPosition: WindowPosition = WindowPosition.LEFT,
    val doorPosition: DoorPosition = DoorPosition.SAG_ON,
    val deskPosition: DeskPosition = DeskPosition.LEFT
) {
    val totalSeats: Int
        get() = rowsPerGroup.take(groupCount).sum() * peoplePerRow
}

data class SeatingPlanState(
    val config: SeatingConfig = SeatingConfig(),
    // Map of seatId ("g{group}-r{row}-s{seat}") -> studentId
    val assignments: Map<String, String> = emptyMap(),
    // Sabitlenmiş öğrenci kimlikleri (otomatik dağıtımlarda yeri değişmez)
    val pinnedStudents: Set<String> = emptySet(),
    // Öncelikli öğrenci kimlikleri (gözlük, dikkat, işitme vb.)
    val priorityStudents: Set<String> = emptySet(),
    // Aktif Planın Adı (Ekranda gösterilir, yazdırılan PDF krokisinde yer almaz)
    val activePlanName: String = "Varsayılan Plan"
)

/**
 * Akıllı Grup Oluşturucu (Team & Group Maker) Veri Modelleri
 */
enum class GroupColorTheme(
    val id: String,
    val title: String,
    val primaryHex: String,
    val bgHex: String,
    val borderHex: String,
    val badgeHex: String
) {
    INDIGO("indigo", "İndigo", "#4F46E5", "#EEF2FF", "#C7D2FE", "#3730A3"),
    EMERALD("emerald", "Zümrüt", "#10B981", "#ECFDF5", "#A7F3D0", "#065F46"),
    SKY("sky", "Gökyüzü", "#0284C7", "#E0F2FE", "#BAE6FD", "#075985"),
    PURPLE("purple", "Mor", "#9333EA", "#FAF5FF", "#E9D5FF", "#6B21A8"),
    AMBER("amber", "Kehribar", "#D97706", "#FFFBEB", "#FDE68A", "#92400E"),
    ROSE("rose", "Gül", "#E11D48", "#FFF1F2", "#FECDD3", "#9F1239"),
    TEAL("teal", "Turkuaz", "#0D9488", "#F0FDFA", "#99F6E4", "#115E59"),
    ORANGE("orange", "Turuncu", "#EA580C", "#FFF7ED", "#FED7AA", "#9A3412");

    companion object {
        fun fromId(id: String): GroupColorTheme = values().firstOrNull { it.id.equals(id, ignoreCase = true) } ?: INDIGO
    }
}

enum class GroupDistributionMode(val title: String, val description: String) {
    BY_GROUP_COUNT("Grup Sayısına Göre", "Toplam grup sayısını belirlersiniz, öğrenciler eşit bölünür."),
    BY_STUDENTS_PER_GROUP("Kişi Sayısına Göre", "Her grupta kaç öğrenci olacağını belirlersiniz, grup adedi hesaplanır.")
}

data class GroupDistributionConfig(
    val mode: GroupDistributionMode = GroupDistributionMode.BY_GROUP_COUNT,
    val targetGroupCount: Int = 4,
    val targetStudentsPerGroup: Int = 5,
    val balanceGender: Boolean = true,
    val excludedStudentIds: Set<String> = emptySet()
)

data class GroupTeam(
    val id: String,
    val name: String,
    val colorTheme: GroupColorTheme = GroupColorTheme.INDIGO,
    val studentIds: List<String> = emptyList(),
    val leaderStudentId: String? = null
)

data class GroupMakerState(
    val groups: List<GroupTeam> = emptyList(),
    val unassignedStudentIds: List<String> = emptyList(),
    val config: GroupDistributionConfig = GroupDistributionConfig(),
    val isPresenterMode: Boolean = false,
    val isWizardOpen: Boolean = false,
    val activeSwapStudentId: String? = null
)


