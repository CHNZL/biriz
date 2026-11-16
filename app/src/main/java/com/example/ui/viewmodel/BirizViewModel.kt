package com.example.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.util.SchedulePersistenceHelper
import com.example.data.model.BranchStatus
import com.example.data.model.City
import com.example.data.model.ClassRoom
import com.example.data.model.GroupColorTheme
import com.example.data.model.GroupDistributionConfig
import com.example.data.model.GroupMakerState
import com.example.data.model.GroupTeam
import com.example.data.model.ScheduleConfig
import com.example.data.model.ScheduleLesson
import com.example.data.model.School
import com.example.data.model.ArchivedSeatingPlan
import com.example.data.model.SeatingConfig
import com.example.data.model.SeatingPlacementMode
import com.example.data.model.SeatingPlanState
import com.example.data.model.UserRole
import com.example.data.repository.ClassRegistryRepository
import com.example.util.GroupMakerHelper
import com.example.util.GroupMakerPersistenceHelper
import com.example.util.ScheduleHelper
import com.example.util.SeatingPlanHelper
import com.example.util.SeatingPlanPersistenceHelper
import com.example.util.TurkishTextHelper
import com.example.data.model.Announcement
import com.example.data.model.AnnouncementPoll
import com.example.data.model.AnnouncementTargetType
import com.example.data.model.BirizNotification
import com.example.data.model.Book
import com.example.data.model.ChatMessage
import com.example.data.model.MessageStatus
import com.example.data.model.NotificationType
import com.example.data.model.ReadingRecord
import com.example.data.model.StarRewardRecord
import com.example.data.model.Student
import com.example.data.model.Tournament
import com.example.data.model.TournamentMatch
import com.example.data.repository.StarRewardPersistenceHelper
import com.example.BuildConfig
import com.example.util.ActivityNotificationHelper
import com.example.util.AnnouncementIdentityHelper
import com.example.util.ChatFeatureHelper
import com.example.util.ChatNotificationHelper
import com.example.util.ChatVisibilityHelper
import com.example.util.CommunicationPersistenceHelper
import com.example.util.AppUpdateDownloadController
import com.example.util.AppUpdateManager
import com.example.util.AppUpdateNotificationHelper
import com.example.util.BackgroundNotificationSyncHelper
import com.example.util.BackgroundUpdateState
import com.example.util.DeferredSystemNotification
import com.example.util.LibraryPersistenceHelper
import com.example.util.NotificationCategory
import com.example.util.NotificationLifecycleHelper
import com.example.util.NotificationPreferencesHelper
import com.example.util.QuietHoursConfig
import com.example.util.SmartBoardHelper
import com.example.util.SmartBoardScreenCastService
import com.example.util.SystemActivityNotificationHelper
import com.example.util.TournamentPersistenceHelper
import com.example.util.UpdateInfo
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.messaging.FirebaseMessaging
import java.util.UUID
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

enum class ScreenState {
    GOOGLE_SIGN_IN,
    ROLE_AND_KVKK,
    TEACHER_SETUP,
    SETUP_SUCCESS,
    TEACHER_DASHBOARD,
    PARENT_PORTAL
}

enum class DashboardSubPage(val title: String, val category: String) {
    // 1. SINIF YÖNETİMİ
    ANA_SAYFA("Ana Sayfa", "SINIF YÖNETİMİ"),
    SINIF_LISTESI("Sınıf Listesi", "SINIF YÖNETİMİ"),
    DERS_PROGRAMI("Ders Programı", "SINIF YÖNETİMİ"),
    OTURMA_PLANI("Oturma Planı", "SINIF YÖNETİMİ"),

    // 2. DERS YÖNETİMİ
    GRUP_OLUSTURMA("Grup Oluşturma", "DERS YÖNETİMİ"),
    SANSLI_OGRENCI("Şanslı Öğrenci", "DERS YÖNETİMİ"),
    YILDIZLAR_SINIFI("Yıldızlar Sınıfı", "DERS YÖNETİMİ"),
    ZAMANLAYICI("Zamanlayıcı", "DERS YÖNETİMİ"),

    // 3. İLETİŞİM (DUYURULAR & BİREYSEL MESAJLAR)
    ILETISIM_DUYURULAR("Duyurular", "İLETİŞİM"),
    ILETISIM_MESAJLAR("Mesajlar", "İLETİŞİM"),

    // 4. KİTAPLIK YÖNETİMİ
    KITAPLIK_LISTE("Kitaplık Listesi", "KİTAPLIK YÖNETİMİ"),
    KITAPLIK_KAYITLAR("Ödünç & İade Geçmişi", "KİTAPLIK YÖNETİMİ"),
    KITAPLIK_DEGERLENDIRME("Okuma İstatistikleri", "KİTAPLIK YÖNETİMİ"),

    // 5. TURNUVA YÖNETİMİ
    TURNUVA_DEVAM_EDENLER("Devam Edenler", "TURNUVA YÖNETİMİ"),
    TURNUVA_TAMAMLANANLAR("Tamamlananlar", "TURNUVA YÖNETİMİ"),

    // 6. ÖĞRETMEN ODASI (RESMİ EVRAK & ARAÇLAR)
    OGRETMEN_ODASI_ZUMRE("Zümre Toplantısı", "ÖĞRETMEN ODASI"),
    OGRETMEN_ODASI_VELI("Veli Toplantısı", "ÖĞRETMEN ODASI"),
    OGRETMEN_ODASI_KULUP("Sosyal Kulüp", "ÖĞRETMEN ODASI"),
    OGRETMEN_ODASI_GEZI("Okul Gezisi", "ÖĞRETMEN ODASI"),
    OGRETMEN_ODASI_DILEKCE("Dilekçe & Formlar", "ÖĞRETMEN ODASI"),

    // 7. PROFİL VE HESAP AYARLARI
    PROFIL_AYARLARI("Profil ve Hesap Ayarları", "HESAP")
}

data class AuthenticatedUser(
    val fullName: String = "Öğretmen",
    val email: String = "",
    val provider: String = "Google Hesabı",
    val avatarInitials: String = "Ö",
    val photoUrl: String? = null
)

data class BirizUiState(
    val currentScreen: ScreenState = if (try { Firebase.auth.currentUser == null } catch (_: Throwable) { true }) ScreenState.GOOGLE_SIGN_IN else ScreenState.ROLE_AND_KVKK,
    val currentRole: UserRole = UserRole.MEMBER,
    val isKvkkAccepted: Boolean = false,
    val isKvkkDialogVisible: Boolean = false,
    val currentUser: AuthenticatedUser = AuthenticatedUser(),
    val isTestMode: Boolean = false,
    val isDarkTheme: Boolean = false,

    // Öğretmen Hiyerarşik Form Alanları (Varsayılan yok, tamamen temiz başlar)
    val selectedCity: City? = null,
    val selectedDistrict: String? = null,
    val rawSchoolInput: String = "",
    val uppercaseSchoolName: String = "",
    val selectedSchool: School? = null,
    val selectedGradeLevel: Int? = null,
    val selectedSection: String? = null,

    // Çakışma Durumu
    val conflictError: String? = null,
    val occupiedClassRoom: ClassRoom? = null,

    // Başarılı Kurulum Sonucu
    val activeClassRoom: ClassRoom? = null,

    // Veli Portalı Durumu
    val linkedParentStudentIds: List<String> = emptyList(),
    val activeParentStudentId: String? = null,

    // Öğretmen Paneli Alt Sayfa & Öğrenci Durumu
    val currentSubPage: DashboardSubPage = DashboardSubPage.ANA_SAYFA,
    val isAddStudentDialogOpen: Boolean = false,
    val isDeleting: Boolean = false,

    // Esnek Ders Programı Durumu
    val scheduleConfig: ScheduleConfig = ScheduleConfig(),
    val scheduleLessons: List<ScheduleLesson> = ScheduleHelper.generateDefaultWeeklySchedule(),
    val isScheduleSettingsStepActive: Boolean = false,
    val activeScheduleDay: String = "PZT",

    // Akıllı Sınıf Oturma Planı Durumu
    val seatingPlanState: SeatingPlanState = SeatingPlanState(),
    val isSeatingConfigOpen: Boolean = false,

    // Akıllı Grup Oluşturucu Durumu
    val groupMakerState: GroupMakerState = GroupMakerState(),

    // İletişim, Duyurular ve Mesajlaşma Durumu
    val announcements: List<Announcement> = emptyList(),
    val chatMessages: List<ChatMessage> = emptyList(),
    val notifications: List<BirizNotification> = emptyList(),
    val activeChatStudentId: String? = null,
    val isNotificationCenterOpen: Boolean = false,
    val requestedParentTabName: String? = null,
    val requestedParentCommIsChat: Boolean? = null,
    val notificationPreferences: Map<String, Boolean> = NotificationPreferencesHelper.defaultPreferences(),
    val quietHoursConfig: QuietHoursConfig = QuietHoursConfig(),
    val deferredQuietCount: Int = 0,

    // Uygulama Güncelleme Durumu (Arka planda & gezinirken otomatik tespit)
    val availableUpdateInfo: UpdateInfo? = null,
    val isGlobalUpdateDialogOpen: Boolean = false,
    val isAutoDownloadOnOpen: Boolean = false,
    val isUpdateBannerDismissed: Boolean = false,

    // Akıllı Tahta Kumandası & Canlı Yansıtma Durumu
    val connectedBoardSessionCode: String? = null,
    val isLiveBoardMirrorActive: Boolean = true,
    val isSmartBoardDialogOpen: Boolean = false,
    val isConnectingBoard: Boolean = false,
    val smartBoardMessage: String? = null,
    val activeBoardCastTitle: String? = null,
    val activeBoardCastType: String? = null,
    val isBroadcastingToBoard: Boolean = false,
    val boardCastProgress: Float = 0f,
    val isScreenMirrorPaused: Boolean = false,
    val screenMirrorPauseReason: String? = null,
    val screenMirrorPackageName: String? = null,

    // UI Bildirimleri
    val snackbarMessage: String? = null
)

class BirizViewModel(
    private val repository: ClassRegistryRepository,
    private val appContext: Context? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(BirizUiState())
    val uiState: StateFlow<BirizUiState> = _uiState.asStateFlow()

    val cities: List<City> = repository.cities
    val students: StateFlow<List<com.example.data.model.Student>> = repository.students

    private var updateCheckJob: Job? = null
    private var scheduleListenerRegistration: ListenerRegistration? = null
    private var communicationAnnouncementsListener: ListenerRegistration? = null
    private var communicationMessagesListener: ListenerRegistration? = null
    private var communicationClearStateListener: ListenerRegistration? = null
    private var starsNotificationListener: ListenerRegistration? = null
    private var libraryBooksNotificationListener: ListenerRegistration? = null
    private var libraryRecordsNotificationListener: ListenerRegistration? = null
    private var tournamentsNotificationListener: ListenerRegistration? = null
    private val tournamentMatchListeners = mutableMapOf<String, ListenerRegistration>()
    private var seatingPlanListenerRegistration: ListenerRegistration? = null
    private var groupMakerListenerRegistration: ListenerRegistration? = null
    private var parentPresenceJob: Job? = null
    private var teacherPresenceJob: Job? = null
    private val knownMessageIds = mutableSetOf<String>()
    private val processedNotifMessageIds = mutableSetOf<String>()
    private var loadedNotifiedClassId: String? = null
    private var observedNotificationClassId: String? = null
    private var rawChatMessages: List<ChatMessage> = emptyList()
    private var chatClearTimestamps: Map<String, Long> = emptyMap()
    private var loadedClearStateKey: String? = null

    private var latestAnnouncements: List<Announcement> = emptyList()
    private var latestStars: List<StarRewardRecord> = emptyList()
    private var latestBooks: List<Book> = emptyList()
    private var latestReadingRecords: List<ReadingRecord> = emptyList()
    private var latestTournaments: List<Tournament> = emptyList()
    private val latestMatchesByTournament = mutableMapOf<String, List<TournamentMatch>>()
    private var latestScheduleUpdatedAtMs: Long = 0L
    private var latestSeatingUpdatedAtMs: Long = 0L
    private var latestGroupsUpdatedAtMs: Long = 0L

    private var hasCloudAnnouncements: Boolean = false
    private var hasCloudStars: Boolean = false
    private var hasCloudBooks: Boolean = false
    private var hasCloudReadingRecords: Boolean = false
    private var hasCloudTournaments: Boolean = false
    private var hasCloudSchedule: Boolean = false
    private var hasCloudSeating: Boolean = false
    private var hasCloudGroups: Boolean = false
    private val loadedCloudMatchTournamentIds = mutableSetOf<String>()

    private var previousActivitySnapshot: ActivityNotificationHelper.ActivitySnapshot? = null
    private var previousTeacherStudentsSnapshot: List<Student>? = null
    private var previousTeacherAnnouncements: List<Announcement>? = null
    private var previousChatReactions: Map<String, Map<String, String>>? = null
    private var seenActivityEventKeys: Set<String> = emptySet()
    private var activityBaselineTimestampMs: Long = 0L
    private var loadedActivityStateKey: String? = null
    private data class PendingPollVote(
        val announcementId: String,
        val optionId: String,
        val voterLabel: String,
        val voterAliases: Set<String>,
        val voterAccountKeys: Set<String>
    )
    private val pendingPollVotes = mutableMapOf<String, PendingPollVote>()
    @Volatile
    private var isAppInForeground: Boolean = true
    @Volatile
    private var isParentCommunicationTabActive: Boolean = false
    @Volatile
    private var isParentChatSubTabActive: Boolean = true
    private var scheduledMessagesDueJob: kotlinx.coroutines.Job? = null

    private fun currentViewerKey(state: BirizUiState = _uiState.value): String {
        return when (state.currentRole) {
            UserRole.TEACHER -> "teacher"
            UserRole.PARENT -> ChatVisibilityHelper.viewerDocumentKey(
                viewerIsTeacher = false,
                userEmail = state.currentUser.email
            )
            else -> "member"
        }
    }

    private fun reloadNotificationPreferences(state: BirizUiState = _uiState.value) {
        val ctx = appContext ?: return
        val viewerKey = currentViewerKey(state)
        val loaded = NotificationPreferencesHelper.loadPreferences(ctx, viewerKey)
        val quietCfg = NotificationPreferencesHelper.loadQuietHours(ctx, viewerKey)
        val pendingCount = NotificationPreferencesHelper.peekDeferredCount(ctx, viewerKey)
        if (_uiState.value.notificationPreferences != loaded ||
            _uiState.value.quietHoursConfig != quietCfg ||
            _uiState.value.deferredQuietCount != pendingCount
        ) {
            _uiState.value = _uiState.value.copy(
                notificationPreferences = loaded,
                quietHoursConfig = quietCfg,
                deferredQuietCount = pendingCount
            )
        }
    }

    fun toggleNotificationCategory(category: NotificationCategory, enabled: Boolean) {
        val state = _uiState.value
        val updated = state.notificationPreferences.toMutableMap().apply {
            this[category.key] = enabled
        }
        _uiState.value = state.copy(notificationPreferences = updated)
        appContext?.let { ctx ->
            NotificationPreferencesHelper.saveCategoryPreference(ctx, currentViewerKey(state), category, enabled)
        }
    }

    fun setAllNotificationCategories(enabled: Boolean) {
        val state = _uiState.value
        val roleCategories = NotificationCategory.categoriesForRole(state.currentRole)
        val updated = state.notificationPreferences.toMutableMap().apply {
            roleCategories.forEach { cat -> this[cat.key] = enabled }
        }
        _uiState.value = state.copy(notificationPreferences = updated)
        appContext?.let { ctx ->
            NotificationPreferencesHelper.saveAllPreferences(ctx, currentViewerKey(state), updated)
        }
    }

    fun toggleQuietHours(enabled: Boolean) {
        val state = _uiState.value
        val updated = state.quietHoursConfig.copy(enabled = enabled)
        val viewerKey = currentViewerKey(state)
        _uiState.value = state.copy(
            quietHoursConfig = updated,
            snackbarMessage = if (enabled) {
                "🌙 Sessiz Saatler açıldı (${NotificationPreferencesHelper.formatQuietHoursRange(updated)})"
            } else {
                "🔔 Sessiz Saatler kapatıldı"
            }
        )
        appContext?.let { ctx ->
            NotificationPreferencesHelper.saveQuietHours(ctx, viewerKey, updated)
            if (enabled) {
                BackgroundNotificationSyncHelper.scheduleQuietHoursFlush(ctx, updated, viewerKey)
                if (!NotificationPreferencesHelper.isWithinQuietHours(updated)) {
                    flushPendingMorningDigest(ctx, viewerKey, forceEvenIfWithinWindow = false)
                }
            } else {
                BackgroundNotificationSyncHelper.cancelQuietHoursFlush(ctx)
                flushPendingMorningDigest(ctx, viewerKey, forceEvenIfWithinWindow = true)
            }
        }
    }

    fun updateQuietHoursWindow(
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int
    ) {
        val state = _uiState.value
        val updated = state.quietHoursConfig.copy(
            startHour = startHour.coerceIn(0, 23),
            startMinute = startMinute.coerceIn(0, 59),
            endHour = endHour.coerceIn(0, 23),
            endMinute = endMinute.coerceIn(0, 59)
        )
        val viewerKey = currentViewerKey(state)
        _uiState.value = state.copy(
            quietHoursConfig = updated,
            snackbarMessage = "🌙 Sessiz Saatler güncellendi: ${NotificationPreferencesHelper.formatQuietHoursRange(updated)}"
        )
        appContext?.let { ctx ->
            NotificationPreferencesHelper.saveQuietHours(ctx, viewerKey, updated)
            if (updated.enabled) {
                BackgroundNotificationSyncHelper.scheduleQuietHoursFlush(ctx, updated, viewerKey)
                if (!NotificationPreferencesHelper.isWithinQuietHours(updated)) {
                    flushPendingMorningDigest(ctx, viewerKey, forceEvenIfWithinWindow = false)
                }
            }
        }
    }

    private fun flushPendingMorningDigest(
        ctx: Context,
        viewerKey: String,
        forceEvenIfWithinWindow: Boolean = false
    ) {
        val flushed = BackgroundNotificationSyncHelper.flushQuietHoursDigestIfDue(
            context = ctx,
            viewerKey = viewerKey,
            forceFlushEvenIfWithinWindow = forceEvenIfWithinWindow
        )
        val remaining = NotificationPreferencesHelper.peekDeferredCount(ctx, viewerKey)
        if (flushed > 0 || remaining != _uiState.value.deferredQuietCount) {
            _uiState.value = _uiState.value.copy(deferredQuietCount = remaining)
        }
    }

    private fun dispatchSystemNotifications(
        ctx: Context,
        notifications: List<BirizNotification>,
        isRecipientTeacher: Boolean
    ) {
        if (notifications.isEmpty()) return
        val state = _uiState.value
        val viewerKey = currentViewerKey(state)
        val quietCfg = state.quietHoursConfig
        val now = System.currentTimeMillis()
        val (deferred, immediate) = notifications.partition {
            NotificationPreferencesHelper.shouldDeferForQuietHours(quietCfg, it, now)
        }
        if (deferred.isNotEmpty()) {
            val newCount = NotificationPreferencesHelper.enqueueDeferredNotifications(
                context = ctx,
                viewerKey = viewerKey,
                items = deferred.map { DeferredSystemNotification(it, isRecipientTeacher) }
            )
            _uiState.value = _uiState.value.copy(deferredQuietCount = newCount)
            BackgroundNotificationSyncHelper.scheduleQuietHoursFlush(ctx, quietCfg, viewerKey, now)
        }
        if (immediate.isNotEmpty()) {
            SystemActivityNotificationHelper.showNotificationsBatch(
                context = ctx,
                notifications = immediate,
                isRecipientTeacher = isRecipientTeacher
            )
        }
    }

    private fun syncBackgroundWorkersAndPushToken(state: BirizUiState = _uiState.value) {
        val ctx = appContext ?: return
        if (state.currentRole != UserRole.TEACHER && state.currentRole != UserRole.PARENT) return
        BackgroundNotificationSyncHelper.schedulePeriodicSync(ctx)
        val viewerKey = currentViewerKey(state)
        if (state.quietHoursConfig.enabled) {
            BackgroundNotificationSyncHelper.scheduleQuietHoursFlush(ctx, state.quietHoursConfig, viewerKey)
        }
        val uid = try { Firebase.auth.currentUser?.uid } catch (_: Throwable) { null } ?: return
        try {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                if (!token.isNullOrBlank()) {
                    viewModelScope.launch {
                        repository.saveFcmToken(uid, token)
                    }
                }
            }
        } catch (_: Throwable) {
        }
    }

    private fun clearActivityListeners() {
        communicationAnnouncementsListener?.remove()
        communicationAnnouncementsListener = null
        communicationMessagesListener?.remove()
        communicationMessagesListener = null
        communicationClearStateListener?.remove()
        communicationClearStateListener = null
        starsNotificationListener?.remove()
        starsNotificationListener = null
        libraryBooksNotificationListener?.remove()
        libraryBooksNotificationListener = null
        libraryRecordsNotificationListener?.remove()
        libraryRecordsNotificationListener = null
        tournamentsNotificationListener?.remove()
        tournamentsNotificationListener = null
        tournamentMatchListeners.values.forEach { it.remove() }
        tournamentMatchListeners.clear()
        scheduledMessagesDueJob?.cancel()
        scheduledMessagesDueJob = null
    }

    private fun seedLocalActivitySnapshotsIfNeeded(
        ctx: Context,
        classId: String,
        forceAllLocal: Boolean = false
    ) {
        if (forceAllLocal || !hasCloudAnnouncements) {
            latestAnnouncements = CommunicationPersistenceHelper.loadAnnouncements(ctx, classId, repository.students.value)
        }
        if (forceAllLocal || !hasCloudStars) {
            latestStars = StarRewardPersistenceHelper.loadStarRecords(ctx, classId)
        }
        if (forceAllLocal || !hasCloudBooks) {
            latestBooks = LibraryPersistenceHelper.loadBooks(ctx, classId)
        }
        if (forceAllLocal || !hasCloudReadingRecords) {
            latestReadingRecords = LibraryPersistenceHelper.loadReadingRecords(ctx, classId)
        }
        if (forceAllLocal || !hasCloudTournaments) {
            latestTournaments = TournamentPersistenceHelper.loadTournaments(ctx, classId)
        }
        latestTournaments.forEach { t ->
            if (forceAllLocal || t.id !in loadedCloudMatchTournamentIds) {
                latestMatchesByTournament[t.id] = TournamentPersistenceHelper.loadMatches(ctx, t.id)
            }
        }
    }

    private fun syncTournamentMatchListeners(
        ctx: Context,
        classId: String,
        cloudTournaments: List<Tournament>
    ) {
        val activeIds = cloudTournaments
            .sortedByDescending { it.updatedAt }
            .take(15)
            .map { it.id }
            .toSet()

        val staleIds = tournamentMatchListeners.keys.filter { it !in activeIds }
        staleIds.forEach { staleId ->
            tournamentMatchListeners.remove(staleId)?.remove()
            latestMatchesByTournament.remove(staleId)
            loadedCloudMatchTournamentIds.remove(staleId)
        }

        cloudTournaments
            .filter { it.id in activeIds && !tournamentMatchListeners.containsKey(it.id) }
            .forEach { tournament ->
                latestMatchesByTournament[tournament.id] = TournamentPersistenceHelper.loadMatches(ctx, tournament.id)
                val reg = TournamentPersistenceHelper.observeFirestoreMatches(ctx, tournament.id) { cloudMatches ->
                    latestMatchesByTournament[tournament.id] = cloudMatches
                    loadedCloudMatchTournamentIds.add(tournament.id)
                    evaluateActivityNotifications(classId)
                }
                if (reg != null) {
                    tournamentMatchListeners[tournament.id] = reg
                }
            }
    }

    private fun evaluateActivityNotifications(
        classId: String = resolveActiveClassId(),
        forceReloadViewerNotifications: Boolean = false
    ) {
        val ctx = appContext ?: return
        val state = _uiState.value
        val viewerKey = currentViewerKey(state)
        val stateKey = "$classId|$viewerKey"

        if (forceReloadViewerNotifications || loadedActivityStateKey != stateKey) {
            loadedActivityStateKey = stateKey
            previousActivitySnapshot = null
            previousTeacherStudentsSnapshot = null
            previousTeacherAnnouncements = null
            reloadNotificationPreferences(state)

            val existingUpdateNotifs = state.notifications.filter { NotificationLifecycleHelper.isUpdateNotification(it) }
            val loadedViewerNotifs = CommunicationPersistenceHelper.loadNotifications(ctx, classId, viewerKey)
            val merged = (existingUpdateNotifs + loadedViewerNotifs).distinctBy { it.id }
            val pruned = NotificationLifecycleHelper.pruneStaleUpdateNotifications(
                notifications = merged,
                currentVersionCode = BuildConfig.VERSION_CODE,
                currentVersionName = BuildConfig.VERSION_NAME,
                latestCheckInfo = state.availableUpdateInfo
            )
            if (pruned != state.notifications) {
                _uiState.value = _uiState.value.copy(notifications = pruned)
            }
            if (pruned.size != loadedViewerNotifs.size) {
                CommunicationPersistenceHelper.saveNotifications(ctx, classId, pruned, viewerKey)
                if (pruned.none { NotificationLifecycleHelper.isUpdateNotification(it) }) {
                    AppUpdateNotificationHelper.cancelUpdateNotification(ctx)
                }
            }

            val (loadedBaseline, loadedSeen) = CommunicationPersistenceHelper.loadActivityNotificationState(
                context = ctx,
                classId = classId,
                viewerKey = viewerKey
            )
            activityBaselineTimestampMs = loadedBaseline
            seenActivityEventKeys = loadedSeen
        }

        val freshState = _uiState.value
        val activePrefs = freshState.notificationPreferences
        if (freshState.currentRole == UserRole.TEACHER) {
            previousActivitySnapshot = null
            val generatedForTeacher = mutableListOf<BirizNotification>()
            var updatedTeacherSeen = seenActivityEventKeys

            val currentStudents = repository.students.value
            if (currentStudents.isNotEmpty()) {
                val teacherRes = ActivityNotificationHelper.evaluateTeacherNotifications(
                    previousStudents = previousTeacherStudentsSnapshot,
                    currentStudents = currentStudents,
                    seenEventKeys = updatedTeacherSeen,
                    baselineTimestampMs = activityBaselineTimestampMs
                )
                previousTeacherStudentsSnapshot = currentStudents
                generatedForTeacher.addAll(teacherRes.newNotifications)
                updatedTeacherSeen = teacherRes.updatedSeenKeys
            }

            if (hasCloudAnnouncements || hasCloudBooks || latestAnnouncements.isNotEmpty() || latestBooks.isNotEmpty()) {
                val opRes = ActivityNotificationHelper.evaluateTeacherOperationalNotifications(
                    previousAnnouncements = previousTeacherAnnouncements,
                    currentAnnouncements = latestAnnouncements,
                    books = latestBooks,
                    seenEventKeys = updatedTeacherSeen,
                    baselineTimestampMs = activityBaselineTimestampMs
                )
                if (hasCloudAnnouncements || latestAnnouncements.isNotEmpty()) {
                    previousTeacherAnnouncements = latestAnnouncements
                }
                generatedForTeacher.addAll(opRes.newNotifications)
                updatedTeacherSeen = opRes.updatedSeenKeys
            }

            if (updatedTeacherSeen != seenActivityEventKeys) {
                seenActivityEventKeys = updatedTeacherSeen
                CommunicationPersistenceHelper.saveActivityNotificationState(
                    context = ctx,
                    classId = classId,
                    viewerKey = viewerKey,
                    baselineMs = activityBaselineTimestampMs,
                    seenKeys = seenActivityEventKeys
                )
            }

            val allowedTeacherNew = generatedForTeacher.filter {
                NotificationPreferencesHelper.isNotificationEnabled(activePrefs, it)
            }
            if (allowedTeacherNew.isNotEmpty()) {
                val latestUi = _uiState.value
                val existingIds = latestUi.notifications.map { it.id }.toSet()
                val uniqueNew = allowedTeacherNew.filter { it.id !in existingIds }
                if (uniqueNew.isNotEmpty()) {
                    val updatedNotifs = (uniqueNew + latestUi.notifications)
                        .distinctBy { it.id }
                        .sortedByDescending { it.timestamp }
                        .take(150)
                    val bannerText = if (isAppInForeground) uniqueNew.firstOrNull()?.title else null
                    _uiState.value = latestUi.copy(
                        notifications = updatedNotifs,
                        snackbarMessage = bannerText ?: latestUi.snackbarMessage
                    )
                    CommunicationPersistenceHelper.saveNotifications(ctx, classId, updatedNotifs, viewerKey)
                    dispatchSystemNotifications(ctx, uniqueNew, isRecipientTeacher = true)
                }
            }
            return
        }

        previousTeacherStudentsSnapshot = null
        previousTeacherAnnouncements = null
        if (freshState.currentRole != UserRole.PARENT || freshState.linkedParentStudentIds.isEmpty()) {
            previousActivitySnapshot = null
            return
        }

        val currentSnapshot = ActivityNotificationHelper.ActivitySnapshot(
            announcements = latestAnnouncements,
            starRecords = latestStars,
            books = latestBooks,
            readingRecords = latestReadingRecords,
            tournaments = latestTournaments,
            matchesByTournament = latestMatchesByTournament.mapValues { it.value.toList() },
            students = repository.students.value,
            scheduleConfig = freshState.scheduleConfig,
            scheduleLessons = freshState.scheduleLessons,
            scheduleUpdatedAtMs = latestScheduleUpdatedAtMs,
            seatingPlanState = freshState.seatingPlanState,
            seatingUpdatedAtMs = latestSeatingUpdatedAtMs,
            groupMakerState = freshState.groupMakerState,
            groupsUpdatedAtMs = latestGroupsUpdatedAtMs,
            hasAnnouncements = hasCloudAnnouncements,
            hasStars = hasCloudStars,
            hasBooks = hasCloudBooks,
            hasReadingRecords = hasCloudReadingRecords,
            hasTournaments = hasCloudTournaments,
            hasSchedule = hasCloudSchedule,
            hasSeating = hasCloudSeating,
            hasGroups = hasCloudGroups,
            loadedMatchTournamentIds = loadedCloudMatchTournamentIds.toSet()
        )

        val result = ActivityNotificationHelper.evaluateParentNotifications(
            previous = previousActivitySnapshot,
            current = currentSnapshot,
            linkedStudentIds = freshState.linkedParentStudentIds.toSet(),
            seenEventKeys = seenActivityEventKeys,
            baselineTimestampMs = activityBaselineTimestampMs,
            currentUserEmail = freshState.currentUser.email
        )
        previousActivitySnapshot = currentSnapshot

        val seenChanged = result.updatedSeenKeys.size != seenActivityEventKeys.size ||
            result.updatedSeenKeys != seenActivityEventKeys
        if (seenChanged) {
            seenActivityEventKeys = result.updatedSeenKeys
            CommunicationPersistenceHelper.saveActivityNotificationState(
                context = ctx,
                classId = classId,
                viewerKey = viewerKey,
                baselineMs = activityBaselineTimestampMs,
                seenKeys = seenActivityEventKeys
            )
        }

        val allowedParentNew = result.newNotifications.filter {
            NotificationPreferencesHelper.isNotificationEnabled(activePrefs, it)
        }
        if (allowedParentNew.isNotEmpty()) {
            val latestUi = _uiState.value
            val existingIds = latestUi.notifications.map { it.id }.toSet()
            val uniqueNew = allowedParentNew.filter { it.id !in existingIds }
            if (uniqueNew.isNotEmpty()) {
                val updatedNotifs = (uniqueNew + latestUi.notifications)
                    .distinctBy { it.id }
                    .sortedByDescending { it.timestamp }
                    .take(150)
                val bannerText = if (isAppInForeground) uniqueNew.firstOrNull()?.title else null
                _uiState.value = latestUi.copy(
                    notifications = updatedNotifs,
                    snackbarMessage = bannerText ?: latestUi.snackbarMessage
                )
                CommunicationPersistenceHelper.saveNotifications(ctx, classId, updatedNotifs, viewerKey)
                dispatchSystemNotifications(ctx, uniqueNew, isRecipientTeacher = false)
            }
        }
    }

    private fun filterMessagesForCurrentViewer(
        messages: List<ChatMessage>,
        state: BirizUiState = _uiState.value,
        clearMap: Map<String, Long> = chatClearTimestamps
    ): List<ChatMessage> {
        val viewerIsTeacher = state.currentRole == UserRole.TEACHER
        val viewerIsParent = state.currentRole == UserRole.PARENT
        if (!viewerIsTeacher && !viewerIsParent) return messages
        return ChatVisibilityHelper.filterVisibleMessages(
            messages = messages,
            viewerIsTeacher = viewerIsTeacher,
            viewerIsParent = viewerIsParent,
            currentParentEmail = state.currentUser.email,
            clearTimestamps = clearMap
        )
    }

    private fun refreshChatClearStateForCurrentViewer(
        classId: String = resolveActiveClassId(),
        forceReload: Boolean = false
    ) {
        val state = _uiState.value
        val viewerIsTeacher = state.currentRole == UserRole.TEACHER
        val viewerDocKey = ChatVisibilityHelper.viewerDocumentKey(viewerIsTeacher, state.currentUser.email)
        val cacheKey = "$classId|$viewerDocKey"
        val ctx = appContext
        if (ctx != null && (forceReload || loadedClearStateKey != cacheKey || communicationClearStateListener == null)) {
            loadedClearStateKey = cacheKey
            chatClearTimestamps = CommunicationPersistenceHelper.loadChatClearTimestamps(ctx, classId, viewerDocKey)
            communicationClearStateListener?.remove()
            communicationClearStateListener = CommunicationPersistenceHelper.observeFirestoreChatClearTimestamps(
                context = ctx,
                classId = classId,
                viewerDocKey = viewerDocKey
            ) { mergedClearMap ->
                chatClearTimestamps = mergedClearMap
                val visible = filterMessagesForCurrentViewer(rawChatMessages, _uiState.value, mergedClearMap)
                _uiState.value = _uiState.value.copy(chatMessages = visible)
            }
        }
        val visible = filterMessagesForCurrentViewer(rawChatMessages, _uiState.value, chatClearTimestamps)
        if (_uiState.value.chatMessages != visible) {
            _uiState.value = _uiState.value.copy(chatMessages = visible)
        }
    }

    private fun resolveActiveClassId(): String {
        val activeId = _uiState.value.activeClassRoom?.id
        if (!activeId.isNullOrBlank() && activeId != "default") return activeId
        val savedId = appContext?.getSharedPreferences("biriz_app_settings", Context.MODE_PRIVATE)
            ?.getString("key_linked_classroom_id", null)
        if (!savedId.isNullOrBlank() && savedId != "default") return savedId
        return "default"
    }

    init {
        viewModelScope.launch {
            var hadScreenMirrorSession = false
            SmartBoardScreenCastService.status.collect { mirrorStatus ->
                if (mirrorStatus.isRunning || mirrorStatus.isStoppedKeepingFrozenFrame) {
                    hadScreenMirrorSession = true
                    _uiState.value = _uiState.value.copy(
                        activeBoardCastTitle = mirrorStatus.targetAppName.ifBlank { _uiState.value.activeBoardCastTitle },
                        activeBoardCastType = "SCREEN_MIRROR",
                        screenMirrorPackageName = mirrorStatus.targetPackageName.ifBlank { _uiState.value.screenMirrorPackageName },
                        isScreenMirrorPaused = mirrorStatus.isPaused,
                        screenMirrorPauseReason = mirrorStatus.pauseReason.ifBlank { null },
                        isLiveBoardMirrorActive = true
                    )
                } else if (hadScreenMirrorSession) {
                    hadScreenMirrorSession = false
                    if (_uiState.value.activeBoardCastType == "SCREEN_MIRROR") {
                        _uiState.value = _uiState.value.copy(
                            activeBoardCastTitle = null,
                            activeBoardCastType = null,
                            isScreenMirrorPaused = false,
                            screenMirrorPauseReason = null,
                            screenMirrorPackageName = null,
                            smartBoardMessage = null
                        )
                    }
                }
            }
        }
        if (appContext != null) {
            val prefs = appContext.getSharedPreferences("biriz_app_settings", Context.MODE_PRIVATE)
            val savedDark = prefs.getBoolean("key_is_dark_theme", false)
            val savedSession = SmartBoardHelper.getSavedSessionCode(appContext)
            val savedMirror = SmartBoardHelper.isMirrorActive(appContext)

            val savedRoleCode = prefs.getString("key_saved_role", "")
            val savedLinkedIdsStr = prefs.getString("key_linked_student_ids", "")
            val savedActiveStudentId = prefs.getString("key_active_parent_student_id", "")
            val savedClassroomId = prefs.getString("key_linked_classroom_id", "")

            val linkedList = if (!savedLinkedIdsStr.isNullOrBlank()) {
                savedLinkedIdsStr.split(",").filter { it.isNotBlank() }
            } else emptyList()

            val restoredClassRoom = if (!savedClassroomId.isNullOrBlank() && savedClassroomId != "default") {
                ClassRoom(
                    id = savedClassroomId,
                    cityName = prefs.getString("key_classroom_city", "") ?: "",
                    districtName = prefs.getString("key_classroom_district", "") ?: "",
                    schoolName = prefs.getString("key_classroom_school", "") ?: "",
                    gradeLevel = prefs.getInt("key_classroom_grade", 4),
                    section = prefs.getString("key_classroom_section", "A") ?: "A",
                    teacherName = prefs.getString("key_classroom_teacher_name", "") ?: "",
                    teacherEmail = prefs.getString("key_classroom_teacher_email", "") ?: "",
                    inviteCode = prefs.getString("key_classroom_invite_code", "") ?: "",
                    isOccupied = true
                )
            } else null

            val restoredRole = when (savedRoleCode) {
                UserRole.PARENT.code -> UserRole.PARENT
                UserRole.TEACHER.code -> UserRole.TEACHER
                else -> _uiState.value.currentRole
            }

            val restoredScreen = if (restoredRole == UserRole.PARENT) {
                ScreenState.PARENT_PORTAL
            } else {
                _uiState.value.currentScreen
            }

            _uiState.value = _uiState.value.copy(
                isDarkTheme = savedDark,
                connectedBoardSessionCode = savedSession,
                isLiveBoardMirrorActive = savedMirror,
                activeClassRoom = restoredClassRoom ?: _uiState.value.activeClassRoom,
                currentRole = restoredRole,
                currentScreen = restoredScreen,
                linkedParentStudentIds = linkedList,
                activeParentStudentId = if (savedActiveStudentId.isNullOrBlank()) linkedList.firstOrNull() else savedActiveStudentId
            )

            val targetClassId = if (!savedClassroomId.isNullOrBlank()) savedClassroomId else "default"
            if (targetClassId != "default") {
                repository.observeStudents(targetClassId)
            }
            loadScheduleFromStorage(targetClassId)
            loadSeatingPlanFromStorage(targetClassId)
            loadCommunicationFromStorage(targetClassId)
            loadGroupMakerFromStorage(targetClassId)
            reloadNotificationPreferences(_uiState.value)
            if (restoredRole == UserRole.TEACHER || restoredRole == UserRole.PARENT) {
                BackgroundNotificationSyncHelper.schedulePeriodicSync(appContext)
            }

            if (linkedList.isNotEmpty() && Firebase.auth.currentUser == null) {
                repository.syncLinkedStudentsWithParent(
                    linkedList,
                    _uiState.value.currentUser.email.ifBlank { "veli@biriz.app" },
                    _uiState.value.currentUser.fullName.ifBlank { "Öğrenci Velisi" }
                )
            }
        } else {
            loadScheduleFromStorage("default")
            loadSeatingPlanFromStorage("default")
            loadCommunicationFromStorage("default")
            loadGroupMakerFromStorage("default")
        }

        // Öğretmen bağlantıyı kestiğinde veli ekranında öğrencinin ANINDA düşmesini sağlayan canlı dinleyici
        viewModelScope.launch {
            repository.students.collect { currentStudents ->
                val state = _uiState.value
                if (state.currentRole == UserRole.PARENT && state.linkedParentStudentIds.isNotEmpty() && currentStudents.isNotEmpty()) {
                    val userEmail = state.currentUser.email.trim()
                    val stillValidIds = state.linkedParentStudentIds.filter { linkedId ->
                        val studentInClass = currentStudents.find { it.id == linkedId }
                        if (studentInClass == null) {
                            false
                        } else if (!studentInClass.isParentLinked) {
                            false
                        } else if (userEmail.isNotBlank() && userEmail != "veli@biriz.app") {
                            studentInClass.parentEmail?.trim().equals(userEmail, ignoreCase = true) ||
                                    studentInClass.allLinkedParents.any { it.email.trim().equals(userEmail, ignoreCase = true) }
                        } else {
                            true
                        }
                    }

                    if (stillValidIds.size != state.linkedParentStudentIds.size) {
                        val nextActive = if (state.activeParentStudentId in stillValidIds) {
                            state.activeParentStudentId
                        } else {
                            stillValidIds.firstOrNull()
                        }
                        _uiState.value = state.copy(
                            linkedParentStudentIds = stillValidIds,
                            activeParentStudentId = nextActive,
                            snackbarMessage = "ℹ️ Öğretmen tarafından öğrenci-veli bağlantısı sonlandırıldı."
                        )
                        saveParentSessionState()
                    }
                }
                if (currentStudents.isNotEmpty()) {
                    val currentSeating = _uiState.value.seatingPlanState
                    val reconciledSeating = SeatingPlanHelper.reconcileAndSanitizePlan(currentSeating, currentStudents)
                    if (reconciledSeating != currentSeating) {
                        _uiState.value = _uiState.value.copy(seatingPlanState = reconciledSeating)
                    }
                }
                triggerSmartBoardSync()
                evaluateActivityNotifications()
            }
        }

        // Aktif sınıftaki öğretmen canlı bağlantı durumunu anlık olarak yansıt
        viewModelScope.launch {
            repository.registeredClasses.collect { classes ->
                val currentActive = _uiState.value.activeClassRoom ?: return@collect
                val updated = classes.firstOrNull { it.id == currentActive.id } ?: return@collect
                if (updated != currentActive) {
                    _uiState.value = _uiState.value.copy(activeClassRoom = updated)
                }
            }
        }

        refreshCurrentUser()
        startPeriodicUpdateChecker()
    }

    fun onAppForegrounded() {
        isAppInForeground = true
        val state = _uiState.value
        appContext?.let { ctx ->
            flushPendingMorningDigest(ctx, currentViewerKey(state), forceEvenIfWithinWindow = false)
        }
        evaluateDueScheduledMessages()
        if (state.currentRole == UserRole.PARENT) {
            updateParentPresence(isOnline = true)
            if (isParentCommunicationTabActive && isParentChatSubTabActive && !state.activeParentStudentId.isNullOrBlank()) {
                markChatMessagesAsRead(state.activeParentStudentId, viewerIsTeacher = false)
            }
        } else if (state.currentRole == UserRole.TEACHER) {
            updateTeacherPresence(isOnline = true)
            if (state.currentScreen == ScreenState.TEACHER_DASHBOARD &&
                state.currentSubPage == DashboardSubPage.ILETISIM_MESAJLAR &&
                !state.activeChatStudentId.isNullOrBlank()
            ) {
                markChatMessagesAsRead(state.activeChatStudentId, viewerIsTeacher = true)
            }
        }
    }

    fun onAppBackgrounded() {
        isAppInForeground = false
        updateParentPresence(isOnline = false)
        updateTeacherPresence(isOnline = false)
    }

    fun updateTeacherPresence(isOnline: Boolean) {
        if (isOnline && !isAppInForeground) return
        val state = _uiState.value
        if (state.currentRole != UserRole.TEACHER && isOnline) return
        val activeClass = state.activeClassRoom ?: return
        val classId = activeClass.id
        if (classId.isBlank()) return

        val nowMillis = System.currentTimeMillis()
        _uiState.value = state.copy(
            activeClassRoom = activeClass.copy(
                teacherIsOnline = isOnline,
                teacherLastActiveMillis = nowMillis
            )
        )

        teacherPresenceJob?.cancel()
        teacherPresenceJob = viewModelScope.launch {
            if (!isOnline) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                    repository.updateTeacherPresence(
                        classroomId = classId,
                        isOnline = false
                    )
                }
            } else {
                if (!isAppInForeground) return@launch
                repository.updateTeacherPresence(
                    classroomId = classId,
                    isOnline = true
                )
            }
        }
    }

    fun updateParentPresence(isOnline: Boolean) {
        if (isOnline && !isAppInForeground) return
        val state = _uiState.value
        if (state.currentRole != UserRole.PARENT && isOnline) return
        val linkedIds = state.linkedParentStudentIds
        if (linkedIds.isEmpty()) return
        val classId = resolveActiveClassId()
        val email = state.currentUser.email.ifBlank { "veli@biriz.app" }
        val name = state.currentUser.fullName.ifBlank { "Öğrenci Velisi" }

        parentPresenceJob?.cancel()
        parentPresenceJob = viewModelScope.launch {
            if (!isOnline) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) {
                    repository.updateParentPresence(
                        classroomId = classId,
                        studentIds = linkedIds,
                        parentEmail = email,
                        parentName = name,
                        isOnline = false
                    )
                }
            } else {
                if (!isAppInForeground) return@launch
                repository.updateParentPresence(
                    classroomId = classId,
                    studentIds = linkedIds,
                    parentEmail = email,
                    parentName = name,
                    isOnline = true
                )
            }
        }
    }

    private fun saveParentSessionState() {
        if (appContext == null) return
        val prefs = appContext.getSharedPreferences("biriz_app_settings", Context.MODE_PRIVATE)
        val state = _uiState.value
        val activeClass = state.activeClassRoom
        val editor = prefs.edit()
            .putString("key_saved_role", state.currentRole.code)
            .putString("key_linked_student_ids", state.linkedParentStudentIds.joinToString(","))
            .putString("key_active_parent_student_id", state.activeParentStudentId ?: "")
            .putString("key_linked_classroom_id", activeClass?.id ?: "")
            .putString("key_user_email", state.currentUser.email)
            .putString("key_user_name", state.currentUser.fullName)
        if (activeClass != null) {
            editor
                .putString("key_classroom_city", activeClass.cityName)
                .putString("key_classroom_district", activeClass.districtName)
                .putString("key_classroom_school", activeClass.schoolName)
                .putInt("key_classroom_grade", activeClass.gradeLevel)
                .putString("key_classroom_section", activeClass.section)
                .putString("key_classroom_teacher_name", activeClass.teacherName)
                .putString("key_classroom_teacher_email", activeClass.teacherEmail)
                .putString("key_classroom_invite_code", activeClass.inviteCode)
        }
        editor.apply()
    }

    /**
     * Kullanıcı programda gezinirken yeni bir sürüm çıktığında
     * anında otomatik olarak algılar (30 saniyede bir periyodik kontrol)
     * hem sistem bildirim çubuğuna bildirim atar hem de uygulama içi bildirim alanını günceller.
     */
    fun startPeriodicUpdateChecker() {
        updateCheckJob?.cancel()
        updateCheckJob = viewModelScope.launch {
            // İlk açılışta 1.5 sn sonra hemen denetle
            delay(1500L)
            while (isActive) {
                checkUpdatesSilently()
                // Program açıkken her 30 saniyede bir otomatik denetle (kullanıcı uygulamadayken anında haberdar olsun)
                delay(30_000L)
            }
        }
    }

    fun triggerLiveUpdateCheck() {
        viewModelScope.launch {
            checkUpdatesSilently(forceResendNotification = false)
        }
    }

    fun pruneStaleUpdateNotifications(latestCheck: UpdateInfo? = _uiState.value.availableUpdateInfo) {
        val current = _uiState.value.notifications
        val pruned = NotificationLifecycleHelper.pruneStaleUpdateNotifications(
            notifications = current,
            currentVersionCode = BuildConfig.VERSION_CODE,
            currentVersionName = BuildConfig.VERSION_NAME,
            latestCheckInfo = latestCheck
        )
        if (pruned.size != current.size) {
            _uiState.value = _uiState.value.copy(notifications = pruned)
            appContext?.let { ctx ->
                val classId = resolveActiveClassId()
                CommunicationPersistenceHelper.saveNotifications(ctx, classId, pruned, currentViewerKey())
                if (pruned.none { NotificationLifecycleHelper.isUpdateNotification(it) }) {
                    AppUpdateNotificationHelper.cancelUpdateNotification(ctx)
                }
            }
        }
    }

    suspend fun checkUpdatesSilently(forceResendNotification: Boolean = false) {
        val ctx = appContext ?: return
        try {
            val update = AppUpdateManager.checkForUpdates(ctx)
            if (update != null && update.isUpdateAvailable) {
                _uiState.value = _uiState.value.copy(
                    availableUpdateInfo = update,
                    isUpdateBannerDismissed = false
                )
                pruneStaleUpdateNotifications(update)
                syncUpdateNotification(update)
                AppUpdateDownloadController.restoreCachedUpdateIfReady(ctx, update)
                val bg = AppUpdateDownloadController.state.value
                val isDownloadingOrReady = (bg is BackgroundUpdateState.Downloading && bg.info.latestVersionCode == update.latestVersionCode) ||
                    (bg is BackgroundUpdateState.ReadyToInstall && bg.info.latestVersionCode == update.latestVersionCode) ||
                    (bg is BackgroundUpdateState.Installing && bg.info.latestVersionCode == update.latestVersionCode)
                val isAppUpdateNotifEnabled = NotificationPreferencesHelper.isCategoryEnabled(
                    _uiState.value.notificationPreferences,
                    NotificationCategory.APP_UPDATES
                )
                // Uygulama ön plandayken zaten üst banner görünür; çift pencere/bildirim yağmuru yaratma
                val shouldPostSystemNotif = forceResendNotification ||
                    (!AppUpdateDownloadController.isAppInForeground && isAppUpdateNotifEnabled)
                if (!isDownloadingOrReady && shouldPostSystemNotif) {
                    AppUpdateNotificationHelper.showUpdateNotification(ctx, update, forceResend = forceResendNotification)
                }
            } else {
                if (update != null && !update.isUpdateAvailable) {
                    _uiState.value = _uiState.value.copy(availableUpdateInfo = null)
                }
                pruneStaleUpdateNotifications(update)
            }
        } catch (_: Exception) {
            pruneStaleUpdateNotifications(null)
        }
    }

    private fun syncUpdateNotification(update: UpdateInfo) {
        if (!update.isUpdateAvailable) {
            pruneStaleUpdateNotifications(update)
            return
        }
        val ctx = appContext ?: return
        val updateNotifId = "app_update_${update.latestVersionCode}"
        val cleanedNotifs = NotificationLifecycleHelper.pruneStaleUpdateNotifications(
            notifications = _uiState.value.notifications,
            currentVersionCode = BuildConfig.VERSION_CODE,
            currentVersionName = BuildConfig.VERSION_NAME,
            latestCheckInfo = update
        )
        val isAppUpdateNotifEnabled = NotificationPreferencesHelper.isCategoryEnabled(
            _uiState.value.notificationPreferences,
            NotificationCategory.APP_UPDATES
        )
        if (cleanedNotifs.none { it.id == updateNotifId } && isAppUpdateNotifEnabled) {
            val releaseItems = update.parsedReleaseItems().take(3)
            val bulletText = releaseItems.joinToString("\n") { "• ${it.title}: ${it.summary}" }
            val notifMessage = when {
                bulletText.isNotBlank() -> bulletText
                update.releaseTitle.isNotBlank() -> "${update.releaseTitle}. Dokunarak güncelleyebilirsiniz."
                else -> "Yeni sürüm kullanıma hazır. Dokunarak yenilikleri inceleyin ve güncelleyin."
            }
            val notifTitle = if (update.releaseTitle.isNotBlank()) {
                "🚀 v${update.latestVersionName} — ${update.releaseTitle}"
            } else {
                "🚀 Yeni Güncelleme Yayında! (v${update.latestVersionName})"
            }
            val updateNotif = BirizNotification(
                id = updateNotifId,
                title = notifTitle,
                message = notifMessage,
                timestamp = System.currentTimeMillis(),
                type = NotificationType.APP_UPDATE,
                targetSubPageName = "APP_UPDATE",
                targetId = update.latestVersionCode.toString(),
                isRead = false
            )
            val updatedList = listOf(updateNotif) + cleanedNotifs
            _uiState.value = _uiState.value.copy(notifications = updatedList)
            val classId = resolveActiveClassId()
            CommunicationPersistenceHelper.saveNotifications(ctx, classId, updatedList, currentViewerKey())
        } else if (cleanedNotifs.size != _uiState.value.notifications.size) {
            _uiState.value = _uiState.value.copy(notifications = cleanedNotifs)
            val classId = resolveActiveClassId()
            CommunicationPersistenceHelper.saveNotifications(ctx, classId, cleanedNotifs, currentViewerKey())
        }
    }

    fun onManualUpdateDiscovered(update: UpdateInfo) {
        if (update.isUpdateAvailable) {
            _uiState.value = _uiState.value.copy(
                availableUpdateInfo = update,
                isUpdateBannerDismissed = false
            )
            pruneStaleUpdateNotifications(update)
            syncUpdateNotification(update)
        } else {
            _uiState.value = _uiState.value.copy(availableUpdateInfo = null)
            pruneStaleUpdateNotifications(update)
        }
    }

    fun openGlobalUpdateDialog(
        info: UpdateInfo? = null,
        autoStartDownload: Boolean = false,
        forceCheck: Boolean = false
    ) {
        _uiState.value = _uiState.value.copy(
            isGlobalUpdateDialogOpen = true,
            isAutoDownloadOnOpen = autoStartDownload,
            availableUpdateInfo = if (forceCheck) null else (info ?: _uiState.value.availableUpdateInfo)
        )
    }

    fun dismissGlobalUpdateDialog() {
        _uiState.value = _uiState.value.copy(isGlobalUpdateDialogOpen = false, isAutoDownloadOnOpen = false)
    }

    fun startDirectUpdate(context: Context, info: UpdateInfo? = _uiState.value.availableUpdateInfo) {
        dismissGlobalUpdateDialog()
        if (info != null && info.isUpdateAvailable) {
            if (!AppUpdateManager.canInstallPackages(context)) {
                android.widget.Toast.makeText(
                    context,
                    "Lütfen BİRİZ uygulamasına güncelleme yükleme izni verin.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                AppUpdateManager.requestInstallPermission(context)
            } else {
                AppUpdateNotificationHelper.cancelUpdateNotification(context)
                AppUpdateDownloadController.startDownload(context, info)
            }
        } else {
            viewModelScope.launch {
                val ctx = appContext ?: context.applicationContext
                val update = AppUpdateManager.checkForUpdates(ctx)
                if (update != null && update.isUpdateAvailable) {
                    _uiState.value = _uiState.value.copy(
                        availableUpdateInfo = update,
                        isUpdateBannerDismissed = false
                    )
                    pruneStaleUpdateNotifications(update)
                    syncUpdateNotification(update)
                    if (!AppUpdateManager.canInstallPackages(ctx)) {
                        android.widget.Toast.makeText(
                            ctx,
                            "Lütfen BİRİZ uygulamasına güncelleme yükleme izni verin.",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                        AppUpdateManager.requestInstallPermission(ctx)
                    } else {
                        AppUpdateNotificationHelper.cancelUpdateNotification(ctx)
                        AppUpdateDownloadController.startDownload(ctx, update)
                    }
                }
            }
        }
    }

    fun dismissUpdateBanner() {
        _uiState.value = _uiState.value.copy(isUpdateBannerDismissed = true)
    }

    fun checkUpdatesNow(onResult: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val ctx = appContext ?: return@launch
            try {
                val update = AppUpdateManager.checkForUpdates(ctx)
                if (update != null && update.isUpdateAvailable) {
                    _uiState.value = _uiState.value.copy(
                        availableUpdateInfo = update,
                        isGlobalUpdateDialogOpen = true
                    )
                    pruneStaleUpdateNotifications(update)
                    syncUpdateNotification(update)
                    AppUpdateDownloadController.restoreCachedUpdateIfReady(ctx, update)
                    onResult?.invoke(true)
                } else {
                    if (update != null && !update.isUpdateAvailable) {
                        _uiState.value = _uiState.value.copy(availableUpdateInfo = null)
                    }
                    pruneStaleUpdateNotifications(update)
                    onResult?.invoke(false)
                }
            } catch (_: Exception) {
                pruneStaleUpdateNotifications(null)
                onResult?.invoke(false)
            }
        }
    }

    fun loadScheduleFromStorage(classId: String = resolveActiveClassId()) {
        val ctx = appContext ?: return
        val savedConfig = SchedulePersistenceHelper.loadScheduleConfig(ctx, classId)
        val savedLessons = SchedulePersistenceHelper.loadScheduleLessons(ctx, classId)
        val savedActiveDay = SchedulePersistenceHelper.loadActiveDay(ctx, classId)

        if (savedConfig != null || savedLessons != null || savedActiveDay != null) {
            _uiState.value = _uiState.value.copy(
                scheduleConfig = savedConfig ?: _uiState.value.scheduleConfig,
                scheduleLessons = savedLessons ?: _uiState.value.scheduleLessons,
                activeScheduleDay = savedActiveDay ?: _uiState.value.activeScheduleDay
            )
        }

        // Firestore üzerinden anlık canlı ders programı senkronizasyonu (SSOT)
        scheduleListenerRegistration?.remove()
        scheduleListenerRegistration = SchedulePersistenceHelper.observeFirestoreSchedule(
            context = ctx,
            classId = classId,
            onUpdatedAtMillis = { updatedAtMs ->
                if (updatedAtMs > 0L) latestScheduleUpdatedAtMs = updatedAtMs
            }
        ) { cloudConfig, cloudLessons ->
            if (cloudConfig != null || cloudLessons != null) {
                hasCloudSchedule = true
                _uiState.value = _uiState.value.copy(
                    scheduleConfig = cloudConfig ?: _uiState.value.scheduleConfig,
                    scheduleLessons = cloudLessons ?: _uiState.value.scheduleLessons
                )
                evaluateActivityNotifications(classId)
            }
        }
    }

    fun loadSeatingPlanFromStorage(classId: String = resolveActiveClassId()) {
        val ctx = appContext ?: return
        val currentStudents = repository.students.value
        val savedPlan = SeatingPlanPersistenceHelper.loadActivePlan(ctx, classId)
        val savedConfig = SeatingPlanPersistenceHelper.loadSeatingConfig(ctx, classId)
        if (savedPlan != null) {
            val reconciled = SeatingPlanHelper.reconcileAndSanitizePlan(savedPlan, currentStudents)
            _uiState.value = _uiState.value.copy(seatingPlanState = reconciled)
        } else if (savedConfig != null) {
            _uiState.value = _uiState.value.copy(
                seatingPlanState = _uiState.value.seatingPlanState.copy(config = savedConfig)
            )
        }

        // Firestore üzerinden anlık canlı oturma planı senkronizasyonu (SSOT - yerel config bulutu ezmez)
        seatingPlanListenerRegistration?.remove()
        seatingPlanListenerRegistration = SeatingPlanPersistenceHelper.observeActivePlanFromFirestore(
            context = ctx,
            classId = classId,
            onUpdatedAtMillis = { updatedAtMs ->
                if (updatedAtMs > 0L) latestSeatingUpdatedAtMs = updatedAtMs
            }
        ) { cloudPlan ->
            hasCloudSeating = true
            val reconciledCloud = SeatingPlanHelper.reconcileAndSanitizePlan(cloudPlan, repository.students.value)
            _uiState.value = _uiState.value.copy(seatingPlanState = reconciledCloud)
            evaluateActivityNotifications(classId)
        }
    }

    fun loadGroupMakerFromStorage(classId: String = resolveActiveClassId()) {
        val ctx = appContext ?: return
        val saved = GroupMakerPersistenceHelper.loadState(ctx, classId)
        if (saved != null && saved.groups.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(groupMakerState = saved)
        }

        groupMakerListenerRegistration?.remove()
        groupMakerListenerRegistration = GroupMakerPersistenceHelper.observeFirestoreGroups(
            context = ctx,
            classId = classId,
            onUpdatedAtMillis = { updatedAtMs ->
                if (updatedAtMs > 0L) latestGroupsUpdatedAtMs = updatedAtMs
            }
        ) { cloudGroups ->
            if (cloudGroups.groups.isNotEmpty()) {
                hasCloudGroups = true
                _uiState.value = _uiState.value.copy(groupMakerState = cloudGroups)
                evaluateActivityNotifications(classId)
            }
        }
    }

    private fun persistGroupMakerState(state: GroupMakerState) {
        val ctx = appContext ?: return
        val classId = resolveActiveClassId()
        GroupMakerPersistenceHelper.saveState(ctx, classId, state)
        triggerSmartBoardSync(groupMaker = state)
    }

    fun loadCommunicationFromStorage(classId: String = resolveActiveClassId()) {
        val ctx = appContext ?: return
        val currentStudents = repository.students.value
        val stateSnapshot = _uiState.value
        val viewerKey = currentViewerKey(stateSnapshot)
        val viewerIsTeacher = stateSnapshot.currentRole == UserRole.TEACHER
        val viewerDocKey = ChatVisibilityHelper.viewerDocumentKey(viewerIsTeacher, stateSnapshot.currentUser.email)

        val loadedAnnouncements = CommunicationPersistenceHelper.loadAnnouncements(ctx, classId, currentStudents)
        val loadedMessages = CommunicationPersistenceHelper.loadChatMessages(ctx, classId, currentStudents)
        val existingUpdateNotifs = stateSnapshot.notifications.filter { NotificationLifecycleHelper.isUpdateNotification(it) }
        val loadedNotifications = CommunicationPersistenceHelper.loadNotifications(ctx, classId, viewerKey)
        val rawMerged = (existingUpdateNotifs + loadedNotifications).distinctBy { it.id }
        val prunedNotifications = NotificationLifecycleHelper.pruneStaleUpdateNotifications(
            notifications = rawMerged,
            currentVersionCode = BuildConfig.VERSION_CODE,
            currentVersionName = BuildConfig.VERSION_NAME,
            latestCheckInfo = stateSnapshot.availableUpdateInfo
        )
        if (prunedNotifications.size != loadedNotifications.size && prunedNotifications != rawMerged) {
            CommunicationPersistenceHelper.saveNotifications(ctx, classId, prunedNotifications, viewerKey)
            if (prunedNotifications.none { NotificationLifecycleHelper.isUpdateNotification(it) }) {
                AppUpdateNotificationHelper.cancelUpdateNotification(ctx)
            }
        }

        if (loadedNotifiedClassId != classId) {
            loadedNotifiedClassId = classId
            processedNotifMessageIds.clear()
            processedNotifMessageIds.addAll(CommunicationPersistenceHelper.loadNotifiedMessageIds(ctx, classId))
            knownMessageIds.clear()
            previousChatReactions = null
        }
        rawChatMessages = loadedMessages
        knownMessageIds.addAll(loadedMessages.map { it.id })

        loadedClearStateKey = "$classId|$viewerDocKey"
        chatClearTimestamps = CommunicationPersistenceHelper.loadChatClearTimestamps(ctx, classId, viewerDocKey)

        _uiState.value = _uiState.value.copy(
            announcements = loadedAnnouncements,
            chatMessages = filterMessagesForCurrentViewer(rawChatMessages, _uiState.value, chatClearTimestamps),
            notifications = prunedNotifications
        )
        evaluateDueScheduledMessages()
        _uiState.value.availableUpdateInfo?.let { syncUpdateNotification(it) }

        val classChanged = observedNotificationClassId != classId
        if (classChanged) {
            observedNotificationClassId = classId
            hasCloudAnnouncements = false
            hasCloudStars = false
            hasCloudBooks = false
            hasCloudReadingRecords = false
            hasCloudTournaments = false
            hasCloudSchedule = false
            hasCloudSeating = false
            hasCloudGroups = false
            latestScheduleUpdatedAtMs = 0L
            latestSeatingUpdatedAtMs = 0L
            latestGroupsUpdatedAtMs = 0L
            loadedCloudMatchTournamentIds.clear()
            latestMatchesByTournament.clear()
            pendingPollVotes.clear()
            previousActivitySnapshot = null
            previousTeacherStudentsSnapshot = null
            previousTeacherAnnouncements = null
            previousChatReactions = null
            clearActivityListeners()
        }

        seedLocalActivitySnapshotsIfNeeded(ctx, classId, forceAllLocal = classChanged)
        evaluateActivityNotifications(classId, forceReloadViewerNotifications = true)

        if (classChanged || communicationClearStateListener == null) {
            communicationClearStateListener?.remove()
            communicationClearStateListener = CommunicationPersistenceHelper.observeFirestoreChatClearTimestamps(
                context = ctx,
                classId = classId,
                viewerDocKey = viewerDocKey
            ) { mergedClearMap ->
                chatClearTimestamps = mergedClearMap
                val visible = filterMessagesForCurrentViewer(rawChatMessages, _uiState.value, mergedClearMap)
                _uiState.value = _uiState.value.copy(chatMessages = visible)
            }
        }

        if (classChanged || communicationAnnouncementsListener == null) {
            // Firestore üzerinden anlık canlı duyuru ve anket senkronizasyonu
            communicationAnnouncementsListener?.remove()
            communicationAnnouncementsListener = CommunicationPersistenceHelper.observeFirestoreAnnouncements(ctx, classId) { cloudAnnouncements ->
                val cloudIds = cloudAnnouncements.map { it.id }.toSet()
                pendingPollVotes.keys.retainAll(cloudIds)
                val reconciled = cloudAnnouncements.map { ann ->
                    val pending = pendingPollVotes[ann.id]
                    val poll = ann.poll
                    if (pending != null && poll != null) {
                        val alreadyInCloud = AnnouncementIdentityHelper.hasVotedInPoll(
                            poll = poll,
                            voterAliases = pending.voterAliases + pending.voterLabel,
                            voterAccountKeys = pending.voterAccountKeys
                        )
                        if (alreadyInCloud) {
                            pendingPollVotes.remove(ann.id)
                            ann
                        } else {
                            val mergedPoll = AnnouncementIdentityHelper.applySingleVote(
                                poll = poll,
                                optionId = pending.optionId,
                                voterLabel = pending.voterLabel,
                                voterAliases = pending.voterAliases,
                                voterAccountKeys = pending.voterAccountKeys
                            )
                            if (mergedPoll != null) ann.copy(poll = mergedPoll) else ann
                        }
                    } else {
                        ann
                    }
                }
                latestAnnouncements = reconciled
                hasCloudAnnouncements = true
                _uiState.value = _uiState.value.copy(announcements = reconciled)
                evaluateActivityNotifications(classId)
            }
        }

        if (classChanged || communicationMessagesListener == null) {
            // Firestore üzerinden anlık iki yönlü öğretmen-veli canlı mesajlaşma senkronizasyonu
            communicationMessagesListener?.remove()
            communicationMessagesListener = CommunicationPersistenceHelper.observeFirestoreChatMessages(ctx, classId) { cloudMessages ->
                handleIncomingChatMessagesSnapshot(classId, cloudMessages)
            }
        }

        if (classChanged || starsNotificationListener == null) {
            starsNotificationListener?.remove()
            starsNotificationListener = StarRewardPersistenceHelper.observeFirestoreStars(ctx, classId) { cloudStars ->
                latestStars = cloudStars
                hasCloudStars = true
                triggerSmartBoardSync()
                evaluateActivityNotifications(classId)
            }
        }

        if (classChanged || libraryBooksNotificationListener == null) {
            libraryBooksNotificationListener?.remove()
            libraryBooksNotificationListener = LibraryPersistenceHelper.observeFirestoreBooks(ctx, classId) { cloudBooks ->
                latestBooks = cloudBooks
                hasCloudBooks = true
                triggerSmartBoardSync()
                evaluateActivityNotifications(classId)
            }
        }

        if (classChanged || libraryRecordsNotificationListener == null) {
            libraryRecordsNotificationListener?.remove()
            libraryRecordsNotificationListener = LibraryPersistenceHelper.observeFirestoreReadingRecords(ctx, classId) { cloudRecords ->
                latestReadingRecords = cloudRecords
                hasCloudReadingRecords = true
                triggerSmartBoardSync()
                evaluateActivityNotifications(classId)
            }
        }

        if (classChanged || tournamentsNotificationListener == null) {
            tournamentsNotificationListener?.remove()
            tournamentsNotificationListener = TournamentPersistenceHelper.observeFirestoreTournaments(ctx, classId) { cloudTournaments ->
                latestTournaments = cloudTournaments
                hasCloudTournaments = true
                syncTournamentMatchListeners(ctx, classId, cloudTournaments)
                evaluateActivityNotifications(classId)
            }
        }
    }

    fun saveAnnouncement(announcement: Announcement) {
        if (_uiState.value.currentRole != UserRole.TEACHER) return
        val classId = resolveActiveClassId()
        val savedAnnouncement = announcement.copy(classId = classId)
        val updatedList = listOf(savedAnnouncement) + _uiState.value.announcements.filter { it.id != savedAnnouncement.id }
        val newNotif = BirizNotification(
            id = "notif_${System.currentTimeMillis()}",
            title = "Yeni Sınıf Duyurusu 📢",
            message = "${announcement.title}: ${announcement.content.take(80)}",
            timestamp = System.currentTimeMillis(),
            type = NotificationType.ANNOUNCEMENT,
            targetSubPageName = "ILETISIM_DUYURULAR",
            targetId = announcement.id,
            isRead = false
        )
        val allowAnnNotif = NotificationPreferencesHelper.isNotificationEnabled(
            _uiState.value.notificationPreferences,
            newNotif
        )
        val updatedNotifs = if (allowAnnNotif) {
            listOf(newNotif) + _uiState.value.notifications
        } else {
            _uiState.value.notifications
        }
        latestAnnouncements = updatedList
        _uiState.value = _uiState.value.copy(
            announcements = updatedList,
            notifications = updatedNotifs,
            snackbarMessage = "Duyuru başarıyla yayınlandı 📢"
        )
        appContext?.let {
            CommunicationPersistenceHelper.unmarkAnnouncementDeleted(it, classId, savedAnnouncement.id)
            CommunicationPersistenceHelper.saveAnnouncementsLocalOnly(it, classId, updatedList)
            CommunicationPersistenceHelper.uploadAnnouncementToFirestore(it, classId, savedAnnouncement)
            if (allowAnnNotif) {
                CommunicationPersistenceHelper.saveNotifications(it, classId, updatedNotifs, currentViewerKey())
                dispatchSystemNotifications(it, listOf(newNotif), isRecipientTeacher = true)
            }
        }
    }

    fun markAnnouncementReadByParent(announcementId: String, parentName: String) {
        val state = _uiState.value
        val classId = resolveActiveClassId()
        val rawName = parentName.ifBlank { state.currentUser.fullName }.ifBlank { "Veli" }
        val allStudents = repository.students.value
        val linkedStudents = allStudents.filter { it.id in state.linkedParentStudentIds }
        val activeStudent = allStudents.firstOrNull { it.id == state.activeParentStudentId } ?: linkedStudents.firstOrNull()
        val resolvedLabel = AnnouncementIdentityHelper.formatParentActionLabel(
            activeStudent = activeStudent,
            linkedStudents = linkedStudents,
            rawParentName = rawName
        )
        val readTs = System.currentTimeMillis()
        var changedAnnouncement: Announcement? = null
        val updatedList = state.announcements.map { ann ->
            if (ann.id == announcementId) {
                val alreadyReadForActive = activeStudent != null &&
                    AnnouncementIdentityHelper.findStudentReadTimestamp(activeStudent, ann.readReceipts) != null
                if (!alreadyReadForActive && !ann.readReceipts.containsKey(resolvedLabel)) {
                    val updatedReceipts = ann.readReceipts + (resolvedLabel to readTs)
                    ann.copy(readReceipts = updatedReceipts).also { changedAnnouncement = it }
                } else ann
            } else ann
        }
        val targetUpdated = changedAnnouncement ?: return
        _uiState.value = _uiState.value.copy(announcements = updatedList)
        appContext?.let { ctx ->
            CommunicationPersistenceHelper.saveAnnouncementsLocalOnly(ctx, classId, updatedList)
            CommunicationPersistenceHelper.recordAnnouncementReadInFirestore(
                context = ctx,
                classId = classId,
                announcementId = announcementId,
                readerLabel = resolvedLabel,
                readTimestamp = readTs,
                fallbackAnnouncement = targetUpdated
            )
        }
    }

    fun editAnnouncement(
        id: String,
        newTitle: String,
        newContent: String,
        newPoll: AnnouncementPoll?,
        targetType: AnnouncementTargetType,
        targetStudentIds: List<String>,
        targetStudentNames: List<String>
    ) {
        if (_uiState.value.currentRole != UserRole.TEACHER) return
        val classId = resolveActiveClassId()
        val currentStudents = repository.students.value
        val deliveredParents = AnnouncementIdentityHelper.formatExpectedRecipientsForAnnouncement(
            targetType = targetType,
            targetStudentIds = targetStudentIds,
            allStudents = currentStudents
        )

        var editedAnnouncement: Announcement? = null
        val updatedList = _uiState.value.announcements.map { ann ->
            if (ann.id == id) {
                ann.copy(
                    title = newTitle,
                    content = newContent,
                    poll = newPoll,
                    targetType = targetType,
                    targetStudentIds = targetStudentIds,
                    targetStudentNames = targetStudentNames,
                    updatedAt = System.currentTimeMillis(),
                    isEdited = true,
                    deliveredToParentNames = deliveredParents,
                    readReceipts = emptyMap() // OKUMA VE GÖRÜLDÜ BİLGİSİ YENİ DUYURU GİBİ SIFIRLANDI!
                ).also { editedAnnouncement = it }
            } else ann
        }

        val newNotif = BirizNotification(
            id = "notif_${System.currentTimeMillis()}",
            title = "Duyuru Güncellendi ✏️",
            message = "$newTitle güncellendi. Veli okuma/görüldü bilgisi sıfırlandı.",
            timestamp = System.currentTimeMillis(),
            type = NotificationType.ANNOUNCEMENT,
            targetSubPageName = "ILETISIM_DUYURULAR",
            targetId = id,
            isRead = false
        )
        val allowAnnNotif = NotificationPreferencesHelper.isNotificationEnabled(
            _uiState.value.notificationPreferences,
            newNotif
        )
        val updatedNotifs = if (allowAnnNotif) {
            listOf(newNotif) + _uiState.value.notifications
        } else {
            _uiState.value.notifications
        }
        latestAnnouncements = updatedList

        _uiState.value = _uiState.value.copy(
            announcements = updatedList,
            notifications = updatedNotifs,
            snackbarMessage = "Duyuru güncellendi. Veli okuma durumu sıfırlandı."
        )
        appContext?.let { ctx ->
            CommunicationPersistenceHelper.saveAnnouncementsLocalOnly(ctx, classId, updatedList)
            editedAnnouncement?.let { edited ->
                CommunicationPersistenceHelper.unmarkAnnouncementDeleted(ctx, classId, edited.id)
                CommunicationPersistenceHelper.uploadAnnouncementToFirestore(ctx, classId, edited)
            }
            if (allowAnnNotif) {
                CommunicationPersistenceHelper.saveNotifications(ctx, classId, updatedNotifs, currentViewerKey())
                dispatchSystemNotifications(ctx, listOf(newNotif), isRecipientTeacher = true)
            }
        }
    }

    fun deleteAnnouncement(id: String) {
        if (_uiState.value.currentRole != UserRole.TEACHER) return
        val classId = resolveActiveClassId()
        pendingPollVotes.remove(id)
        val updatedList = _uiState.value.announcements.filter { it.id != id }
        latestAnnouncements = updatedList
        _uiState.value = _uiState.value.copy(
            announcements = updatedList,
            snackbarMessage = "Duyuru kalıcı olarak silindi."
        )
        appContext?.let { ctx ->
            CommunicationPersistenceHelper.markAnnouncementDeleted(ctx, classId, id)
            CommunicationPersistenceHelper.saveAnnouncementsLocalOnly(ctx, classId, updatedList)
            CommunicationPersistenceHelper.deleteAnnouncementFromFirestore(ctx, classId, id)
        }
    }

    fun votePollOption(announcementId: String, optionId: String, voterName: String) {
        val state = _uiState.value
        if (state.currentRole != UserRole.PARENT) {
            _uiState.value = state.copy(
                snackbarMessage = "Anket oylamaları yalnızca veli hesapları tarafından yapılabilir."
            )
            return
        }
        val classId = resolveActiveClassId()
        val rawVoter = voterName.ifBlank { state.currentUser.fullName }.ifBlank { "Veli" }
        val allStudents = repository.students.value
        val linkedStudents = allStudents.filter { it.id in state.linkedParentStudentIds }
        val activeStudent = allStudents.firstOrNull { it.id == state.activeParentStudentId } ?: linkedStudents.firstOrNull()

        val resolvedVoterLabel = AnnouncementIdentityHelper.formatParentActionLabel(
            activeStudent = activeStudent,
            linkedStudents = linkedStudents,
            rawParentName = rawVoter
        )
        val voterAliases = AnnouncementIdentityHelper.voterAliasesForParent(
            activeStudent = activeStudent,
            linkedStudents = linkedStudents,
            rawParentName = rawVoter
        )
        val firebaseUid = try { Firebase.auth.currentUser?.uid ?: "" } catch (_: Throwable) { "" }
        val voterAccountKeys = AnnouncementIdentityHelper.buildVoterAccountKeys(
            userId = firebaseUid,
            userEmail = state.currentUser.email
        )

        if (pendingPollVotes.containsKey(announcementId)) {
            _uiState.value = state.copy(
                snackbarMessage = "🔒 Bu ankete zaten oy kullandınız. Verilen oy değiştirilemez."
            )
            return
        }

        val targetAnn = state.announcements.firstOrNull { it.id == announcementId } ?: return
        val currentPoll = targetAnn.poll ?: return
        val updatedPoll = AnnouncementIdentityHelper.applySingleVote(
            poll = currentPoll,
            optionId = optionId,
            voterLabel = resolvedVoterLabel,
            voterAliases = voterAliases,
            voterAccountKeys = voterAccountKeys
        )
        if (updatedPoll == null) {
            _uiState.value = state.copy(
                snackbarMessage = "🔒 Bu ankete zaten oy kullandınız. Verilen oy değiştirilemez."
            )
            return
        }

        pendingPollVotes[announcementId] = PendingPollVote(
            announcementId = announcementId,
            optionId = optionId,
            voterLabel = resolvedVoterLabel,
            voterAliases = voterAliases,
            voterAccountKeys = voterAccountKeys
        )

        var updatedAnnouncement: Announcement? = null
        val updatedList = state.announcements.map { ann ->
            if (ann.id == announcementId) {
                ann.copy(poll = updatedPoll).also { updatedAnnouncement = it }
            } else ann
        }
        val savedAnn = updatedAnnouncement ?: return
        latestAnnouncements = updatedList
        _uiState.value = state.copy(
            announcements = updatedList,
            snackbarMessage = "✅ Anket oyunuz kaydedildi (değiştirilemez)."
        )
        appContext?.let { ctx ->
            CommunicationPersistenceHelper.saveAnnouncementsLocalOnly(ctx, classId, updatedList)
            CommunicationPersistenceHelper.recordPollVoteInFirestore(
                context = ctx,
                classId = classId,
                announcementId = announcementId,
                optionId = optionId,
                voterLabel = resolvedVoterLabel,
                voterAliases = voterAliases,
                voterAccountKeys = voterAccountKeys,
                fallbackAnnouncement = savedAnn
            )
        }
    }

    fun sendChatMessage(
        studentId: String,
        text: String,
        isFromTeacher: Boolean = true,
        parentEmail: String = "",
        parentName: String = "",
        replyTo: ChatMessage? = null,
        scheduledFor: Long? = null
    ) {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return

        val classId = resolveActiveClassId()
        if (classId == "default" && !_uiState.value.isTestMode) {
            _uiState.value = _uiState.value.copy(
                snackbarMessage = "⚠️ Mesaj gönderebilmek için aktif bir sınıf bağlantısı gereklidir."
            )
            return
        }

        val student = repository.students.value.find { it.id == studentId }
        val studentNo = student?.studentNumber ?: ""
        val studentName = student?.fullName ?: "Öğrenci"

        val targetParent = student?.allLinkedParents?.find { it.email.equals(parentEmail, ignoreCase = true) }
            ?: student?.allLinkedParents?.firstOrNull()

        val resolvedEmail = if (parentEmail.isNotBlank()) parentEmail.trim() else (targetParent?.email ?: "")
        val resolvedParentName = when {
            parentName.isNotBlank() -> parentName.trim()
            targetParent != null && targetParent.name.isNotBlank() -> targetParent.name
            !student?.parentName.isNullOrBlank() -> student!!.parentName!!
            else -> "${studentName.split(" ").first()} Velisi"
        }

        val resolvedTeacherName = _uiState.value.activeClassRoom?.teacherName?.takeIf { it.isNotBlank() }
            ?: _uiState.value.currentUser.fullName.takeIf { it.isNotBlank() && isFromTeacher }
            ?: "Sınıf Öğretmeni"

        val now = System.currentTimeMillis()
        val effectiveScheduledFor = if (isFromTeacher && scheduledFor != null && scheduledFor > now + 5_000L) {
            scheduledFor
        } else {
            null
        }

        val clearWatermark = if (isFromTeacher) {
            ChatVisibilityHelper.resolvedTeacherClearTimestamp(studentId, resolvedEmail, chatClearTimestamps)
        } else {
            chatClearTimestamps[studentId] ?: 0L
        }
        val validReply = replyTo?.takeIf { !it.isDeletedForEveryone && it.text.isNotBlank() }
        val newMsg = ChatMessage(
            id = "chat_${UUID.randomUUID()}",
            classId = classId,
            studentId = studentId,
            studentNumber = studentNo,
            studentName = studentName,
            parentName = resolvedParentName,
            parentEmail = resolvedEmail,
            isFromTeacher = isFromTeacher,
            senderName = if (isFromTeacher) resolvedTeacherName else resolvedParentName,
            text = cleanText,
            timestamp = effectiveScheduledFor ?: maxOf(now, clearWatermark + 1L),
            status = MessageStatus.SENT,
            replyToMessageId = validReply?.id,
            replyToSenderName = validReply?.senderName,
            replyToText = validReply?.text,
            replyToIsFromTeacher = validReply?.isFromTeacher,
            scheduledFor = effectiveScheduledFor
        )

        knownMessageIds.add(newMsg.id)
        processedNotifMessageIds.add(newMsg.id)

        rawChatMessages = (rawChatMessages + newMsg).sortedBy { it.timestamp }
        val visibleMessages = filterMessagesForCurrentViewer(rawChatMessages)
        _uiState.value = _uiState.value.copy(
            chatMessages = visibleMessages,
            snackbarMessage = if (effectiveScheduledFor != null) {
                "⏰ Mesaj ${ChatFeatureHelper.formatScheduledLabel(effectiveScheduledFor, now)} için zamanlandı"
            } else {
                _uiState.value.snackbarMessage
            }
        )
        scheduleNextDueEvaluation()

        appContext?.let { ctx ->
            CommunicationPersistenceHelper.saveChatMessagesLocal(ctx, classId, rawChatMessages)
            CommunicationPersistenceHelper.saveNotifiedMessageIds(ctx, classId, processedNotifMessageIds)
            if (classId != "default") {
                CommunicationPersistenceHelper.uploadChatMessageToFirestore(ctx, classId, newMsg) { err ->
                    _uiState.value = _uiState.value.copy(
                        snackbarMessage = "⚠️ Mesaj buluta gönderilemedi: ${err.localizedMessage ?: "Bağlantı hatası"}"
                    )
                }
            }
        }
    }

    fun sendBulkChatMessages(
        recipients: List<com.example.util.BulkMessageRecipient>,
        text: String,
        scheduledFor: Long? = null
    ) {
        val cleanTemplate = text.trim()
        if (cleanTemplate.isEmpty()) return

        val state = _uiState.value
        if (state.currentRole != UserRole.TEACHER) return

        val uniqueRecipients = com.example.util.BulkMessageHelper.deduplicateRecipients(recipients)
        if (uniqueRecipients.isEmpty()) {
            _uiState.value = state.copy(snackbarMessage = "⚠️ Lütfen en az bir veli seçin.")
            return
        }

        val classId = resolveActiveClassId()
        if (classId == "default" && !state.isTestMode) {
            _uiState.value = state.copy(
                snackbarMessage = "⚠️ Toplu mesaj gönderebilmek için aktif bir sınıf bağlantısı gereklidir."
            )
            return
        }

        val resolvedTeacherName = state.activeClassRoom?.teacherName?.takeIf { it.isNotBlank() }
            ?: state.currentUser.fullName.takeIf { it.isNotBlank() }
            ?: "Sınıf Öğretmeni"

        val now = System.currentTimeMillis()
        val effectiveScheduledFor = if (scheduledFor != null && scheduledFor > now + 5_000L) {
            scheduledFor
        } else {
            null
        }

        val createdMessages = uniqueRecipients.mapIndexed { idx, recipient ->
            val student = recipient.student
            val parent = recipient.parent
            val resolvedEmail = parent.email.trim()
            val resolvedParentName = parent.name.trim().ifBlank {
                student.parentName?.takeIf { it.isNotBlank() }
                    ?: "${student.fullName.split(" ").firstOrNull().orEmpty()} Velisi"
            }
            val personalizedText = com.example.util.BulkMessageHelper.personalizeBulkMessageText(
                template = cleanTemplate,
                studentName = student.fullName,
                parentName = resolvedParentName
            ).ifBlank { cleanTemplate }

            val clearWatermark = ChatVisibilityHelper.resolvedTeacherClearTimestamp(
                student.id,
                resolvedEmail,
                chatClearTimestamps
            )
            val baseTs = effectiveScheduledFor ?: maxOf(now + idx.toLong(), clearWatermark + 1L)

            ChatMessage(
                id = "chat_${UUID.randomUUID()}",
                classId = classId,
                studentId = student.id,
                studentNumber = student.studentNumber,
                studentName = student.fullName,
                parentName = resolvedParentName,
                parentEmail = resolvedEmail,
                isFromTeacher = true,
                senderName = resolvedTeacherName,
                text = personalizedText,
                timestamp = baseTs,
                status = MessageStatus.SENT,
                scheduledFor = effectiveScheduledFor
            )
        }

        createdMessages.forEach { msg ->
            knownMessageIds.add(msg.id)
            processedNotifMessageIds.add(msg.id)
        }

        rawChatMessages = (rawChatMessages + createdMessages).sortedBy { it.timestamp }
        val visibleMessages = filterMessagesForCurrentViewer(rawChatMessages)
        val count = createdMessages.size
        _uiState.value = _uiState.value.copy(
            chatMessages = visibleMessages,
            snackbarMessage = if (effectiveScheduledFor != null) {
                "⏰ $count veli için toplu mesaj ${ChatFeatureHelper.formatScheduledLabel(effectiveScheduledFor, now)} için zamanlandı"
            } else {
                "📢 $count veliye toplu mesaj başarıyla gönderildi"
            }
        )
        scheduleNextDueEvaluation()

        appContext?.let { ctx ->
            CommunicationPersistenceHelper.saveChatMessagesLocal(ctx, classId, rawChatMessages)
            CommunicationPersistenceHelper.saveNotifiedMessageIds(ctx, classId, processedNotifMessageIds)
            if (classId != "default") {
                CommunicationPersistenceHelper.uploadChatMessagesBatchToFirestore(ctx, classId, createdMessages) { err ->
                    _uiState.value = _uiState.value.copy(
                        snackbarMessage = "⚠️ Bazı toplu mesajlar buluta gönderilemedi: ${err.localizedMessage ?: "Bağlantı hatası"}"
                    )
                }
            }
        }
    }

    fun toggleChatMessageReaction(messageId: String, emoji: String) {
        if (messageId.isBlank() || emoji.isBlank()) return
        val state = _uiState.value
        val classId = resolveActiveClassId()
        val viewerIsTeacher = state.currentRole == UserRole.TEACHER
        var targetMsg: ChatMessage? = null
        var updatedValueForCloud = ""
        var participantKeyUsed = ""

        val updatedRaw = rawChatMessages.map { msg ->
            if (msg.id == messageId && !msg.isDeletedForEveryone) {
                val pKey = ChatFeatureHelper.reactionParticipantKey(
                    viewerIsTeacher = viewerIsTeacher,
                    userEmail = state.currentUser.email,
                    studentId = msg.studentId
                )
                val (newReactions, cloudVal) = ChatFeatureHelper.toggleReaction(msg.reactions, pKey, emoji)
                participantKeyUsed = pKey
                updatedValueForCloud = cloudVal
                msg.copy(reactions = newReactions).also { targetMsg = it }
            } else msg
        }
        if (targetMsg == null) return

        rawChatMessages = updatedRaw
        _uiState.value = state.copy(chatMessages = filterMessagesForCurrentViewer(updatedRaw, state))

        appContext?.let { ctx ->
            CommunicationPersistenceHelper.saveChatMessagesLocal(ctx, classId, updatedRaw)
            if (classId != "default" && participantKeyUsed.isNotBlank()) {
                CommunicationPersistenceHelper.updateChatMessageReactionInFirestore(
                    context = ctx,
                    classId = classId,
                    messageId = messageId,
                    participantKey = participantKeyUsed,
                    emojiOrEmpty = updatedValueForCloud
                )
            }
        }
    }

    fun deliverScheduledMessageNow(messageId: String) {
        val state = _uiState.value
        if (state.currentRole != UserRole.TEACHER) return
        val classId = resolveActiveClassId()
        val now = System.currentTimeMillis()
        var updatedMsg: ChatMessage? = null
        val updatedRaw = rawChatMessages.map { msg ->
            if (msg.id == messageId && msg.isFromTeacher && msg.scheduledFor != null) {
                msg.copy(
                    scheduledFor = null,
                    timestamp = now,
                    status = MessageStatus.DELIVERED,
                    deliveredAt = now
                ).also { updatedMsg = it }
            } else msg
        }.sortedBy { it.timestamp }
        val delivered = updatedMsg ?: return

        rawChatMessages = updatedRaw
        _uiState.value = state.copy(
            chatMessages = filterMessagesForCurrentViewer(updatedRaw, state),
            snackbarMessage = "Mesaj hemen iletildi ✅"
        )
        scheduleNextDueEvaluation()

        appContext?.let { ctx ->
            CommunicationPersistenceHelper.saveChatMessagesLocal(ctx, classId, updatedRaw)
            if (classId != "default") {
                CommunicationPersistenceHelper.uploadChatMessageToFirestore(ctx, classId, delivered)
            }
        }
    }

    fun updateScheduledMessage(
        messageId: String,
        newText: String,
        newScheduledFor: Long
    ) {
        val state = _uiState.value
        if (state.currentRole != UserRole.TEACHER) return
        val cleanText = newText.trim()
        val now = System.currentTimeMillis()
        if (cleanText.isEmpty() || newScheduledFor <= now + 5_000L) return

        val classId = resolveActiveClassId()
        var updatedMsg: ChatMessage? = null
        val updatedRaw = rawChatMessages.map { msg ->
            if (msg.id == messageId && msg.isFromTeacher && msg.isPendingScheduled(now)) {
                msg.copy(
                    text = cleanText,
                    scheduledFor = newScheduledFor,
                    timestamp = newScheduledFor,
                    status = MessageStatus.SENT,
                    deliveredAt = null
                ).also { updatedMsg = it }
            } else msg
        }.sortedBy { it.timestamp }
        val edited = updatedMsg ?: return

        rawChatMessages = updatedRaw
        _uiState.value = state.copy(
            chatMessages = filterMessagesForCurrentViewer(updatedRaw, state),
            snackbarMessage = "⏰ Planlı mesaj güncellendi: ${ChatFeatureHelper.formatScheduledLabel(newScheduledFor, now)}"
        )
        scheduleNextDueEvaluation()

        appContext?.let { ctx ->
            CommunicationPersistenceHelper.saveChatMessagesLocal(ctx, classId, updatedRaw)
            if (classId != "default") {
                CommunicationPersistenceHelper.uploadChatMessageToFirestore(ctx, classId, edited)
            }
        }
    }

    fun cancelScheduledMessage(messageId: String) {
        val state = _uiState.value
        if (state.currentRole != UserRole.TEACHER) return
        val target = rawChatMessages.firstOrNull {
            it.id == messageId && it.isFromTeacher && it.isPendingScheduled()
        } ?: return

        val classId = resolveActiveClassId()
        val updatedRaw = rawChatMessages.filterNot { it.id == target.id }
        knownMessageIds.remove(target.id)
        processedNotifMessageIds.remove(target.id)
        rawChatMessages = updatedRaw

        _uiState.value = state.copy(
            chatMessages = filterMessagesForCurrentViewer(updatedRaw, state),
            snackbarMessage = "🗑️ Planlanmış mesaj silindi"
        )
        scheduleNextDueEvaluation()

        appContext?.let { ctx ->
            CommunicationPersistenceHelper.saveChatMessagesLocal(ctx, classId, updatedRaw)
            CommunicationPersistenceHelper.saveNotifiedMessageIds(ctx, classId, processedNotifMessageIds)
            if (classId != "default") {
                CommunicationPersistenceHelper.deleteChatMessageFromFirestore(ctx, classId, target.id)
            }
        }
    }

    private fun scheduleNextDueEvaluation() {
        scheduledMessagesDueJob?.cancel()
        val now = System.currentTimeMillis()
        val nextScheduledAt = rawChatMessages
            .mapNotNull { it.scheduledFor?.takeIf { ts -> ts > now && !it.isDeletedForEveryone } }
            .minOrNull() ?: return

        val waitMs = (nextScheduledAt - now + 250L).coerceAtLeast(250L)
        scheduledMessagesDueJob = viewModelScope.launch {
            kotlinx.coroutines.delay(waitMs)
            evaluateDueScheduledMessages()
        }
    }

    private fun evaluateDueScheduledMessages() {
        val classId = resolveActiveClassId()
        val state = _uiState.value
        val now = System.currentTimeMillis()
        val ctx = appContext

        val dueTeacherMessages = if (state.currentRole == UserRole.TEACHER) {
            rawChatMessages.filter {
                it.isFromTeacher &&
                    !it.isDeletedForEveryone &&
                    it.scheduledFor != null &&
                    it.scheduledFor <= now &&
                    it.status == MessageStatus.SENT
            }
        } else {
            emptyList()
        }

        if (dueTeacherMessages.isNotEmpty()) {
            val promotedById = dueTeacherMessages.associate { msg ->
                msg.id to msg.copy(
                    status = MessageStatus.DELIVERED,
                    deliveredAt = msg.deliveredAt ?: msg.scheduledFor ?: now
                )
            }
            rawChatMessages = rawChatMessages.map { promotedById[it.id] ?: it }
            if (ctx != null) {
                CommunicationPersistenceHelper.saveChatMessagesLocal(ctx, classId, rawChatMessages)
                if (classId != "default") {
                    promotedById.values.forEach { promoted ->
                        CommunicationPersistenceHelper.uploadChatMessageToFirestore(ctx, classId, promoted)
                    }
                }
            }

            val firstDue = dueTeacherMessages.first()
            val confirmNotif = BirizNotification(
                id = "sched_confirm_${now}_${dueTeacherMessages.size}",
                title = if (dueTeacherMessages.size == 1) "⏰ Planlanan Mesaj İletildi" else "⏰ Planlanan ${dueTeacherMessages.size} Mesaj İletildi",
                message = if (dueTeacherMessages.size == 1) {
                    "${firstDue.parentName} (${firstDue.studentName}) için zamanlanan mesaj iletildi: \"${firstDue.text.take(65)}\""
                } else {
                    "${dueTeacherMessages.size} veli için zamanlanan mesajlar hedef saatte iletildi."
                },
                timestamp = now,
                type = NotificationType.MESSAGE,
                targetSubPageName = "ILETISIM_MESAJLAR",
                targetId = firstDue.studentId,
                isRead = false
            )
            val latestUi = _uiState.value
            if (NotificationPreferencesHelper.isNotificationEnabled(latestUi.notificationPreferences, confirmNotif) &&
                latestUi.notifications.none { it.id == confirmNotif.id }
            ) {
                val updatedNotifs = (listOf(confirmNotif) + latestUi.notifications).distinctBy { it.id }.take(150)
                _uiState.value = latestUi.copy(
                    notifications = updatedNotifs,
                    snackbarMessage = if (isAppInForeground) confirmNotif.title else latestUi.snackbarMessage
                )
                if (ctx != null) {
                    CommunicationPersistenceHelper.saveNotifications(ctx, classId, updatedNotifs, currentViewerKey(latestUi))
                    dispatchSystemNotifications(ctx, listOf(confirmNotif), isRecipientTeacher = true)
                }
            }
        }

        if (rawChatMessages.any { it.scheduledFor != null }) {
            handleIncomingChatMessagesSnapshot(classId, rawChatMessages)
        } else {
            scheduleNextDueEvaluation()
        }
    }

    fun simulateParentReply(
        studentId: String,
        replyText: String,
        parentEmail: String = "",
        parentName: String = ""
    ) {
        val cleanText = replyText.trim()
        if (cleanText.isEmpty()) return
        val classId = resolveActiveClassId()
        val student = repository.students.value.find { it.id == studentId }
        val targetParent = student?.allLinkedParents?.find { it.email.equals(parentEmail, ignoreCase = true) }
            ?: student?.allLinkedParents?.firstOrNull()
        val resolvedParentName = parentName.ifBlank { targetParent?.name ?: "Veli" }
        val resolvedEmail = parentEmail.ifBlank { targetParent?.email ?: "" }
        val simMsg = ChatMessage(
            id = "chat_${UUID.randomUUID()}",
            classId = classId,
            studentId = studentId,
            studentNumber = student?.studentNumber ?: "",
            studentName = student?.fullName ?: "Öğrenci",
            parentName = resolvedParentName,
            parentEmail = resolvedEmail,
            isFromTeacher = false,
            senderName = resolvedParentName,
            text = cleanText,
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.DELIVERED
        )
        handleIncomingChatMessagesSnapshot(classId, rawChatMessages + simMsg)
    }

    private fun handleIncomingChatMessagesSnapshot(classId: String, cloudMessages: List<ChatMessage>) {
        val ctx = appContext
        val state = _uiState.value
        val now = System.currentTimeMillis()
        val cloudIds = cloudMessages.map { it.id }.toSet()
        val pendingLocalMessages = rawChatMessages.filter { local ->
            local.id !in cloudIds &&
                local.status == MessageStatus.SENT &&
                (now - local.timestamp) in -5_000L..12_000L
        }
        val mergedMessages = (cloudMessages + pendingLocalMessages)
            .distinctBy { it.id }
            .sortedBy { it.timestamp }

        rawChatMessages = mergedMessages
        knownMessageIds.addAll(mergedMessages.map { it.id })

        val viewerIsTeacher = state.currentRole == UserRole.TEACHER
        val viewerIsParent = state.currentRole == UserRole.PARENT
        if (!viewerIsTeacher && !viewerIsParent) {
            _uiState.value = state.copy(chatMessages = mergedMessages)
            return
        }

        val visibleMessages = filterMessagesForCurrentViewer(mergedMessages, state)
        val incomingUnread = visibleMessages.filter { msg ->
            msg.status != MessageStatus.READ &&
                !msg.isDeletedForEveryone &&
                NotificationLifecycleHelper.isIncomingMessageForViewer(
                    msg = msg,
                    viewerIsTeacher = viewerIsTeacher,
                    viewerIsParent = viewerIsParent,
                    linkedParentStudentIds = state.linkedParentStudentIds,
                    currentUserEmail = state.currentUser.email
                )
        }

        val isTeacherChatPageOpen = state.currentScreen == ScreenState.TEACHER_DASHBOARD &&
            state.currentSubPage == DashboardSubPage.ILETISIM_MESAJLAR

        val openChatUnread = incomingUnread.filter { msg ->
            NotificationLifecycleHelper.isChatScreenActivelyOpenForStudent(
                studentId = msg.studentId,
                viewerIsTeacher = viewerIsTeacher,
                viewerIsParent = viewerIsParent,
                isAppInForeground = isAppInForeground,
                isTeacherChatPageOpen = isTeacherChatPageOpen,
                activeTeacherChatStudentId = state.activeChatStudentId,
                isParentCommTabActive = isParentCommunicationTabActive,
                isParentChatSubTabActive = isParentChatSubTabActive,
                activeParentStudentId = state.activeParentStudentId,
                isNotificationCenterOpen = state.isNotificationCenterOpen
            )
        }

        var updatedRaw = mergedMessages
        var updatedNotifs = state.notifications
        var notifsChanged = false

        // Açık sohbet ekranına gelen mesajları anında OKUNDU say ve bildirim üretme
        if (openChatUnread.isNotEmpty()) {
            val openIds = openChatUnread.map { it.id }.toSet()
            val newlyReadList = openChatUnread.map {
                it.copy(
                    status = MessageStatus.READ,
                    deliveredAt = it.deliveredAt ?: it.timestamp,
                    readAt = it.readAt ?: now
                )
            }
            updatedRaw = mergedMessages.map { msg ->
                if (msg.id in openIds) {
                    msg.copy(
                        status = MessageStatus.READ,
                        deliveredAt = msg.deliveredAt ?: msg.timestamp,
                        readAt = msg.readAt ?: now
                    )
                } else msg
            }
            rawChatMessages = updatedRaw
            processedNotifMessageIds.addAll(openIds)

            val openStudentIds = openChatUnread.map { it.studentId }.distinct()
            openStudentIds.forEach { sId ->
                val beforeCount = updatedNotifs.size
                updatedNotifs = NotificationLifecycleHelper.removeMessageNotificationsForStudent(updatedNotifs, sId)
                if (updatedNotifs.size != beforeCount) notifsChanged = true
                if (ctx != null) ChatNotificationHelper.cancelMessageNotification(ctx, sId)
            }

            if (ctx != null) {
                CommunicationPersistenceHelper.saveChatMessagesLocal(ctx, classId, updatedRaw)
                CommunicationPersistenceHelper.markChatMessagesReadInFirestore(ctx, classId, newlyReadList)
                CommunicationPersistenceHelper.saveNotifiedMessageIds(ctx, classId, processedNotifMessageIds)
            }
        }

        // Başka cihazda, silinerek veya daha önce okunmuş öğrencilerin mesaj bildirimlerini temizle
        val unreadByStudent = incomingUnread.filterNot { msg -> msg.id in openChatUnread.map { it.id }.toSet() }
            .groupBy { it.studentId }
        val staleMsgNotifStudentIds = updatedNotifs
            .filter {
                it.type == NotificationType.MESSAGE &&
                    !it.id.startsWith("msg_react_") &&
                    !it.id.startsWith("sched_confirm_") &&
                    !it.targetId.isNullOrBlank()
            }
            .mapNotNull { it.targetId }
            .filter { sId -> unreadByStudent[sId].isNullOrEmpty() }
            .distinct()
        staleMsgNotifStudentIds.forEach { sId ->
            val beforeCount = updatedNotifs.size
            updatedNotifs = NotificationLifecycleHelper.removeMessageNotificationsForStudent(updatedNotifs, sId)
            if (updatedNotifs.size != beforeCount) notifsChanged = true
            if (ctx != null) ChatNotificationHelper.cancelMessageNotification(ctx, sId)
        }

        // Kapalı sohbetlere gelen YENİ okunmamış mesajlar için alıcıya bildirim üret
        val closedChatUnread = incomingUnread.filterNot { msg -> openChatUnread.any { it.id == msg.id } }
        val newlyArrived = closedChatUnread.filter { it.id !in processedNotifMessageIds }
        var bannerText: String? = null
        val messagesNotifEnabled = NotificationPreferencesHelper.isCategoryEnabled(
            state.notificationPreferences,
            NotificationCategory.MESSAGES
        )

        if (newlyArrived.isNotEmpty()) {
            processedNotifMessageIds.addAll(newlyArrived.map { it.id })
            if (ctx != null) {
                CommunicationPersistenceHelper.saveNotifiedMessageIds(ctx, classId, processedNotifMessageIds)
            }

            if (messagesNotifEnabled) {
                val grouped = newlyArrived.groupBy { it.studentId }
                grouped.forEach { (studentId, msgs) ->
                    val latestMsg = msgs.maxByOrNull { it.timestamp } ?: return@forEach
                    val totalUnreadForStudent = closedChatUnread.count { it.studentId == studentId }
                    val notifId = "msg_notif_$studentId"
                    val notifTitle = if (viewerIsTeacher) {
                        if (totalUnreadForStudent > 1) "Yeni Veli Mesajları ($totalUnreadForStudent) 💬" else "Yeni Veli Mesajı 💬"
                    } else {
                        if (totalUnreadForStudent > 1) "Öğretmenden Yeni Mesajlar ($totalUnreadForStudent) 💬" else "Öğretmenden Yeni Mesaj 💬"
                    }
                    val notifBody = if (viewerIsTeacher) {
                        "${latestMsg.parentName} (${latestMsg.studentNumber} ${latestMsg.studentName}): ${latestMsg.text}"
                    } else {
                        "${latestMsg.senderName}: ${latestMsg.text}"
                    }

                    val notif = BirizNotification(
                        id = notifId,
                        title = notifTitle,
                        message = notifBody,
                        timestamp = latestMsg.timestamp,
                        type = NotificationType.MESSAGE,
                        targetSubPageName = if (viewerIsTeacher) "ILETISIM_MESAJLAR" else "COMMUNICATION",
                        targetId = studentId,
                        isRead = false
                    )
                    updatedNotifs = listOf(notif) + NotificationLifecycleHelper.removeMessageNotificationsForStudent(updatedNotifs, studentId)
                    notifsChanged = true

                    if (ctx != null) {
                        if (NotificationPreferencesHelper.shouldDeferForQuietHours(state.quietHoursConfig, notif, now)) {
                            val viewerKey = currentViewerKey(state)
                            val deferredCount = NotificationPreferencesHelper.enqueueDeferredNotifications(
                                context = ctx,
                                viewerKey = viewerKey,
                                items = listOf(DeferredSystemNotification(notif, viewerIsTeacher))
                            )
                            _uiState.value = _uiState.value.copy(deferredQuietCount = deferredCount)
                            BackgroundNotificationSyncHelper.scheduleQuietHoursFlush(ctx, state.quietHoursConfig, viewerKey, now)
                        } else {
                            ChatNotificationHelper.showIncomingMessageNotification(
                                context = ctx,
                                message = latestMsg,
                                isRecipientTeacher = viewerIsTeacher,
                                unreadCountForStudent = totalUnreadForStudent
                            )
                        }
                    }
                }

                if (isAppInForeground) {
                    val newestOverall = newlyArrived.maxByOrNull { it.timestamp }
                    if (newestOverall != null) {
                        bannerText = "💬 ${newestOverall.senderName}: ${newestOverall.text.take(65)}"
                    }
                }
            }
        }

        // Mesaj emoji tepkileri (👍, ❤️, 👀 vb.) eklendiğinde veya değiştiğinde alıcıya bildirim üret
        val prevReactionsSnapshot = previousChatReactions
        val reactionsNotifEnabled = NotificationPreferencesHelper.isCategoryEnabled(
            state.notificationPreferences,
            NotificationCategory.MESSAGE_REACTIONS
        )
        if (prevReactionsSnapshot != null) {
            visibleMessages
                .filter { !it.isDeletedForEveryone && !it.isPendingScheduled(now) }
                .forEach { msg ->
                    val oldReactions = prevReactionsSnapshot[msg.id].orEmpty()
                    val currReactions = msg.activeReactions()
                    currReactions.forEach { (pKey, emoji) ->
                        val ownParticipantKey = ChatFeatureHelper.reactionParticipantKey(
                            viewerIsTeacher = viewerIsTeacher,
                            userEmail = state.currentUser.email,
                            studentId = msg.studentId
                        )
                        val isOwn = pKey == ownParticipantKey || (viewerIsTeacher && pKey == "teacher")
                        if (!isOwn && oldReactions[pKey] != emoji) {
                            val isRelevantStudent = viewerIsTeacher ||
                                state.linkedParentStudentIds.isEmpty() ||
                                msg.studentId in state.linkedParentStudentIds
                            val isChatOpen = NotificationLifecycleHelper.isChatScreenActivelyOpenForStudent(
                                studentId = msg.studentId,
                                viewerIsTeacher = viewerIsTeacher,
                                viewerIsParent = viewerIsParent,
                                isAppInForeground = isAppInForeground,
                                isTeacherChatPageOpen = isTeacherChatPageOpen,
                                activeTeacherChatStudentId = state.activeChatStudentId,
                                isParentCommTabActive = isParentCommunicationTabActive,
                                isParentChatSubTabActive = isParentChatSubTabActive,
                                activeParentStudentId = state.activeParentStudentId,
                                isNotificationCenterOpen = state.isNotificationCenterOpen
                            )
                            if (isRelevantStudent && !isChatOpen && reactionsNotifEnabled) {
                                val reactorLabel = if (pKey == "teacher") {
                                    state.activeClassRoom?.teacherName?.takeIf { it.isNotBlank() } ?: "Öğretmen"
                                } else {
                                    msg.parentName.ifBlank { "${msg.studentName} Velisi" }
                                }
                                val reactNotifId = "msg_react_${msg.id}_${pKey}_${emoji.hashCode()}"
                                val reactNotif = BirizNotification(
                                    id = reactNotifId,
                                    title = "$emoji Mesajınıza Tepki Bıraktı",
                                    message = "$reactorLabel (${msg.studentName}): \"${msg.text.take(55)}\" mesajına $emoji tepkisi verdi.",
                                    timestamp = now,
                                    type = NotificationType.MESSAGE,
                                    targetSubPageName = if (viewerIsTeacher) "ILETISIM_MESAJLAR" else "COMMUNICATION",
                                    targetId = msg.studentId,
                                    isRead = false
                                )
                                if (updatedNotifs.none { it.id == reactNotifId }) {
                                    updatedNotifs = listOf(reactNotif) + updatedNotifs
                                    notifsChanged = true
                                    if (ctx != null) {
                                        dispatchSystemNotifications(ctx, listOf(reactNotif), isRecipientTeacher = viewerIsTeacher)
                                    }
                                    if (isAppInForeground && bannerText == null) {
                                        bannerText = "$emoji $reactorLabel mesajınıza tepki verdi"
                                    }
                                }
                            }
                        }
                    }
                }
        }
        previousChatReactions = mergedMessages.associate { it.id to it.activeReactions() }

        if (notifsChanged && ctx != null) {
            CommunicationPersistenceHelper.saveNotifications(ctx, classId, updatedNotifs, currentViewerKey(state))
        }

        _uiState.value = _uiState.value.copy(
            chatMessages = filterMessagesForCurrentViewer(updatedRaw, state),
            notifications = updatedNotifs,
            snackbarMessage = bannerText ?: _uiState.value.snackbarMessage
        )
        scheduleNextDueEvaluation()
    }

    fun markChatMessagesAsRead(
        studentId: String,
        parentEmail: String = "",
        viewerIsTeacher: Boolean = (_uiState.value.currentRole == UserRole.TEACHER)
    ) {
        val classId = resolveActiveClassId()
        val now = System.currentTimeMillis()
        val newlyReadMessages = mutableListOf<ChatMessage>()
        val updatedRaw = rawChatMessages.map { msg ->
            val matchesStudent = msg.studentId == studentId
            val matchesDirection = if (viewerIsTeacher) !msg.isFromTeacher else msg.isFromTeacher
            val matchesParent = if (!viewerIsTeacher || parentEmail.isBlank()) {
                true
            } else {
                msg.parentEmail.isBlank() || msg.parentEmail.equals(parentEmail, ignoreCase = true)
            }
            if (matchesStudent &&
                matchesDirection &&
                matchesParent &&
                !msg.isPendingScheduled(now) &&
                msg.status != MessageStatus.READ
            ) {
                val readMsg = msg.copy(
                    status = MessageStatus.READ,
                    deliveredAt = msg.deliveredAt ?: msg.timestamp,
                    readAt = msg.readAt ?: now
                )
                newlyReadMessages.add(readMsg)
                processedNotifMessageIds.add(msg.id)
                readMsg
            } else msg
        }
        rawChatMessages = updatedRaw

        val currentNotifs = _uiState.value.notifications
        val reactIdsToCancel = currentNotifs
            .filter { it.id.startsWith("msg_react_") && it.targetId == studentId }
            .map { it.id }
        val updatedNotifs = NotificationLifecycleHelper.removeMessageNotificationsForStudent(currentNotifs, studentId)
            .filterNot { it.id in reactIdsToCancel }
        val notifsChanged = updatedNotifs.size != currentNotifs.size

        if (newlyReadMessages.isNotEmpty() || notifsChanged) {
            _uiState.value = _uiState.value.copy(
                chatMessages = filterMessagesForCurrentViewer(updatedRaw),
                notifications = updatedNotifs
            )
            appContext?.let { ctx ->
                if (newlyReadMessages.isNotEmpty()) {
                    CommunicationPersistenceHelper.saveChatMessagesLocal(ctx, classId, updatedRaw)
                    CommunicationPersistenceHelper.markChatMessagesReadInFirestore(ctx, classId, newlyReadMessages)
                    CommunicationPersistenceHelper.saveNotifiedMessageIds(ctx, classId, processedNotifMessageIds)
                }
                if (notifsChanged) {
                    CommunicationPersistenceHelper.saveNotifications(ctx, classId, updatedNotifs, currentViewerKey())
                }
                ChatNotificationHelper.cancelMessageNotification(ctx, studentId)
                if (reactIdsToCancel.isNotEmpty()) {
                    SystemActivityNotificationHelper.cancelNotifications(ctx, reactIdsToCancel)
                }
            }
        } else {
            appContext?.let { ctx ->
                ChatNotificationHelper.cancelMessageNotification(ctx, studentId)
            }
        }
    }

    /**
     * WhatsApp "Benden Sil": Seçili mesajları yalnızca mevcut kullanıcının kendi görünümünden siler.
     * Karşı tarafın sohbet geçmişi kesinlikle silinmez.
     */
    fun deleteChatMessagesForMe(messageIds: Set<String>) {
        if (messageIds.isEmpty()) return
        val state = _uiState.value
        val classId = resolveActiveClassId()
        val viewerIsTeacher = state.currentRole == UserRole.TEACHER
        val affectedStudentIds = mutableSetOf<String>()

        val updatedRaw = rawChatMessages.map { msg ->
            if (msg.id in messageIds) {
                affectedStudentIds.add(msg.studentId)
                processedNotifMessageIds.add(msg.id)
                if (viewerIsTeacher) {
                    msg.copy(hiddenForTeacher = true)
                } else {
                    val parentKey = ChatVisibilityHelper.normalizeParentKey(state.currentUser.email, msg.studentId)
                    msg.copy(hiddenForParentEmails = (msg.hiddenForParentEmails + parentKey).distinct())
                }
            } else {
                msg
            }
        }
        rawChatMessages = updatedRaw
        val visibleAfter = filterMessagesForCurrentViewer(updatedRaw, state)

        var updatedNotifs = state.notifications
        var notifsChanged = false
        affectedStudentIds.forEach { sId ->
            val hasRemainingUnread = visibleAfter.any { msg ->
                msg.studentId == sId &&
                    msg.status != MessageStatus.READ &&
                    NotificationLifecycleHelper.isIncomingMessageForViewer(
                        msg = msg,
                        viewerIsTeacher = viewerIsTeacher,
                        viewerIsParent = !viewerIsTeacher,
                        linkedParentStudentIds = state.linkedParentStudentIds,
                        currentUserEmail = state.currentUser.email
                    )
            }
            if (!hasRemainingUnread) {
                val before = updatedNotifs.size
                updatedNotifs = NotificationLifecycleHelper.removeMessageNotificationsForStudent(updatedNotifs, sId)
                if (updatedNotifs.size != before) notifsChanged = true
                appContext?.let { ChatNotificationHelper.cancelMessageNotification(it, sId) }
            }
        }

        val deletedCount = messageIds.size
        _uiState.value = state.copy(
            chatMessages = visibleAfter,
            notifications = updatedNotifs,
            snackbarMessage = if (deletedCount == 1) {
                "Mesaj yalnızca sizin sohbetinizden silindi"
            } else {
                "$deletedCount mesaj yalnızca sizin sohbetinizden silindi"
            }
        )

        appContext?.let { ctx ->
            CommunicationPersistenceHelper.saveChatMessagesLocal(ctx, classId, updatedRaw)
            CommunicationPersistenceHelper.saveNotifiedMessageIds(ctx, classId, processedNotifMessageIds)
            if (notifsChanged) {
                CommunicationPersistenceHelper.saveNotifications(ctx, classId, updatedNotifs, currentViewerKey(state))
            }
            val parentKey = ChatVisibilityHelper.normalizeParentKey(
                state.currentUser.email,
                affectedStudentIds.firstOrNull().orEmpty()
            )
            CommunicationPersistenceHelper.hideChatMessagesForUserInFirestore(
                context = ctx,
                classId = classId,
                messageIds = messageIds,
                viewerIsTeacher = viewerIsTeacher,
                parentKey = parentKey
            )
        }
    }

    /**
     * WhatsApp "Herkesten Sil": Yalnızca kullanıcının kendi gönderdiği mesajı her iki tarafta silinmiş olarak işaretler.
     */
    fun deleteChatMessagesForEveryone(messageIds: Set<String>) {
        if (messageIds.isEmpty()) return
        val state = _uiState.value
        val classId = resolveActiveClassId()
        val viewerIsTeacher = state.currentRole == UserRole.TEACHER

        val allowedIds = rawChatMessages.filter { msg ->
            msg.id in messageIds &&
                !msg.isDeletedForEveryone &&
                ((viewerIsTeacher && msg.isFromTeacher) || (!viewerIsTeacher && !msg.isFromTeacher))
        }.map { it.id }.toSet()
        if (allowedIds.isEmpty()) return

        val updatedRaw = rawChatMessages.map { msg ->
            if (msg.id in allowedIds) {
                msg.copy(isDeletedForEveryone = true, text = "")
            } else msg
        }
        rawChatMessages = updatedRaw
        _uiState.value = state.copy(
            chatMessages = filterMessagesForCurrentViewer(updatedRaw, state),
            snackbarMessage = if (allowedIds.size == 1) {
                "Mesaj herkes için silindi"
            } else {
                "${allowedIds.size} mesaj herkes için silindi"
            }
        )
        appContext?.let { ctx ->
            CommunicationPersistenceHelper.saveChatMessagesLocal(ctx, classId, updatedRaw)
            CommunicationPersistenceHelper.deleteChatMessagesForEveryoneInFirestore(ctx, classId, allowedIds)
        }
    }

    /**
     * WhatsApp "Sohbet Geçmişini Temizle":
     * Seçili öğrenci sohbetindeki tüm geçmişi YALNIZCA mevcut kullanıcı için temizler.
     * Karşı taraf kendi geçmişini görmeye devam eder; bu saatten sonra gelen yeni mesajlar yine görünür.
     */
    fun clearChatHistoryForMe(studentId: String, parentEmail: String = "") {
        val state = _uiState.value
        val classId = resolveActiveClassId()
        val viewerIsTeacher = state.currentRole == UserRole.TEACHER
        val student = repository.students.value.find { it.id == studentId }
        val isSingleParent = (student?.allLinkedParents?.size ?: 0) <= 1

        val maxMsgTimestamp = rawChatMessages
            .filter { it.studentId == studentId }
            .maxOfOrNull { it.timestamp } ?: 0L
        val clearTs = maxOf(System.currentTimeMillis(), maxMsgTimestamp)

        val keysToSet = if (viewerIsTeacher) {
            ChatVisibilityHelper.teacherConversationClearKeys(studentId, parentEmail, isSingleParent)
        } else {
            ChatVisibilityHelper.parentConversationClearKeys(studentId)
        }

        val updatedMap = chatClearTimestamps.toMutableMap()
        keysToSet.forEach { k ->
            updatedMap[k] = maxOf(updatedMap[k] ?: 0L, clearTs)
        }
        chatClearTimestamps = updatedMap

        val updatedRaw = rawChatMessages.map { msg ->
            val matchesStudent = msg.studentId == studentId
            val matchesDirection = if (viewerIsTeacher) !msg.isFromTeacher else msg.isFromTeacher
            val matchesParent = if (!viewerIsTeacher || parentEmail.isBlank()) {
                true
            } else {
                msg.parentEmail.isBlank() || msg.parentEmail.equals(parentEmail, ignoreCase = true)
            }
            if (matchesStudent && matchesDirection && matchesParent && msg.status != MessageStatus.READ) {
                processedNotifMessageIds.add(msg.id)
                msg.copy(status = MessageStatus.READ)
            } else msg
        }
        rawChatMessages = updatedRaw

        val visibleAfter = filterMessagesForCurrentViewer(updatedRaw, state, updatedMap)
        val updatedNotifs = NotificationLifecycleHelper.removeMessageNotificationsForStudent(state.notifications, studentId)
        val notifsChanged = updatedNotifs.size != state.notifications.size

        _uiState.value = state.copy(
            chatMessages = visibleAfter,
            notifications = updatedNotifs,
            snackbarMessage = "Sohbet geçmişi yalnızca sizin için temizlendi 🧹"
        )

        appContext?.let { ctx ->
            val viewerDocKey = ChatVisibilityHelper.viewerDocumentKey(viewerIsTeacher, state.currentUser.email)
            CommunicationPersistenceHelper.saveChatClearTimestampsLocal(ctx, classId, viewerDocKey, updatedMap)
            CommunicationPersistenceHelper.updateChatClearTimestampsInFirestore(ctx, classId, viewerDocKey, updatedMap)
            CommunicationPersistenceHelper.saveChatMessagesLocal(ctx, classId, updatedRaw)
            CommunicationPersistenceHelper.saveNotifiedMessageIds(ctx, classId, processedNotifMessageIds)
            if (notifsChanged) {
                CommunicationPersistenceHelper.saveNotifications(ctx, classId, updatedNotifs, currentViewerKey(state))
            }
            ChatNotificationHelper.cancelMessageNotification(ctx, studentId)
        }
    }

    fun markNotificationAsRead(notificationId: String) {
        val classId = resolveActiveClassId()
        val removed = _uiState.value.notifications.firstOrNull { it.id == notificationId }
        val updatedNotifs = _uiState.value.notifications.filterNot { it.id == notificationId }
        _uiState.value = _uiState.value.copy(notifications = updatedNotifs)
        appContext?.let { ctx ->
            CommunicationPersistenceHelper.saveNotifications(ctx, classId, updatedNotifs, currentViewerKey())
            val isSpecialMessageAlert = removed != null &&
                (removed.id.startsWith("msg_react_") || removed.id.startsWith("sched_confirm_"))
            if (removed?.type == NotificationType.MESSAGE && !isSpecialMessageAlert && !removed.targetId.isNullOrBlank()) {
                ChatNotificationHelper.cancelMessageNotification(ctx, removed.targetId)
            } else if (removed != null && NotificationLifecycleHelper.isUpdateNotification(removed)) {
                AppUpdateNotificationHelper.cancelUpdateNotification(ctx)
            } else {
                SystemActivityNotificationHelper.cancelNotification(ctx, notificationId)
            }
        }
    }

    fun markAllNotificationsAsRead() {
        val classId = resolveActiveClassId()
        val current = _uiState.value.notifications
        val messageStudentIds = current
            .filter { it.type == NotificationType.MESSAGE && !it.targetId.isNullOrBlank() }
            .mapNotNull { it.targetId }
            .distinct()
        _uiState.value = _uiState.value.copy(notifications = emptyList())
        appContext?.let { ctx ->
            CommunicationPersistenceHelper.saveNotifications(ctx, classId, emptyList(), currentViewerKey())
            ChatNotificationHelper.cancelAllMessageNotifications(ctx, messageStudentIds)
            AppUpdateNotificationHelper.cancelUpdateNotification(ctx)
            SystemActivityNotificationHelper.cancelNotifications(ctx, current.map { it.id })
        }
    }

    fun setNotificationCenterOpen(isOpen: Boolean) {
        _uiState.value = _uiState.value.copy(isNotificationCenterOpen = isOpen)
    }

    fun setActiveChatStudent(studentId: String?) {
        _uiState.value = _uiState.value.copy(activeChatStudentId = studentId)
        if (!studentId.isNullOrBlank() &&
            isAppInForeground &&
            _uiState.value.currentRole == UserRole.TEACHER &&
            _uiState.value.currentSubPage == DashboardSubPage.ILETISIM_MESAJLAR
        ) {
            markChatMessagesAsRead(studentId, viewerIsTeacher = true)
        }
    }

    fun setParentCommunicationScreenState(isCommTabActive: Boolean, isChatSubTabActive: Boolean) {
        isParentCommunicationTabActive = isCommTabActive
        isParentChatSubTabActive = isChatSubTabActive
        val state = _uiState.value
        val activeStudentId = state.activeParentStudentId
        if (isCommTabActive && isChatSubTabActive && isAppInForeground && state.currentRole == UserRole.PARENT && !activeStudentId.isNullOrBlank()) {
            markChatMessagesAsRead(activeStudentId, viewerIsTeacher = false)
        }
    }

    fun openChatFromNotification(studentId: String, isTeacherTarget: Boolean? = null) {
        val state = _uiState.value
        val openAsTeacher = isTeacherTarget ?: (state.currentRole == UserRole.TEACHER)
        if (openAsTeacher && state.currentRole == UserRole.TEACHER) {
            _uiState.value = state.copy(
                currentScreen = ScreenState.TEACHER_DASHBOARD,
                currentSubPage = DashboardSubPage.ILETISIM_MESAJLAR,
                activeChatStudentId = studentId
            )
            markChatMessagesAsRead(studentId, viewerIsTeacher = true)
        } else if (state.currentRole == UserRole.PARENT) {
            val resolvedStudentId = if (studentId in state.linkedParentStudentIds) studentId else state.activeParentStudentId
            _uiState.value = state.copy(
                currentScreen = ScreenState.PARENT_PORTAL,
                activeParentStudentId = resolvedStudentId,
                requestedParentTabName = "COMMUNICATION",
                requestedParentCommIsChat = true
            )
            if (!resolvedStudentId.isNullOrBlank()) {
                markChatMessagesAsRead(resolvedStudentId, viewerIsTeacher = false)
            }
        }
    }

    fun openNotificationFromSystemBar(
        notifId: String?,
        typeName: String?,
        targetSubPageName: String?,
        targetId: String?,
        isTeacherTarget: Boolean?
    ) {
        if (!notifId.isNullOrBlank()) {
            markNotificationAsRead(notifId)
        }
        val parsedType = typeName?.let {
            try { NotificationType.valueOf(it) } catch (_: Exception) { null }
        } ?: return

        if (parsedType == NotificationType.APP_UPDATE) {
            openGlobalUpdateDialog()
            return
        }
        if (parsedType == NotificationType.MESSAGE && !targetId.isNullOrBlank()) {
            openChatFromNotification(targetId, isTeacherTarget)
            return
        }

        val state = _uiState.value
        val openAsTeacher = isTeacherTarget ?: (state.currentRole == UserRole.TEACHER)
        if (openAsTeacher && state.currentRole == UserRole.TEACHER) {
            val targetPage = targetSubPageName?.let {
                try { DashboardSubPage.valueOf(it) } catch (_: Exception) { null }
            } ?: when (parsedType) {
                NotificationType.PARENT_REGISTRATION -> DashboardSubPage.SINIF_LISTESI
                NotificationType.STAR_REWARD -> DashboardSubPage.YILDIZLAR_SINIFI
                NotificationType.LIBRARY -> DashboardSubPage.KITAPLIK_LISTE
                NotificationType.TOURNAMENT -> DashboardSubPage.TURNUVA_DEVAM_EDENLER
                NotificationType.MESSAGE -> DashboardSubPage.ILETISIM_MESAJLAR
                NotificationType.SCHEDULE -> DashboardSubPage.DERS_PROGRAMI
                NotificationType.SEATING -> DashboardSubPage.OTURMA_PLANI
                NotificationType.GROUP -> DashboardSubPage.GRUP_OLUSTURMA
                NotificationType.ANNOUNCEMENT,
                NotificationType.APP_UPDATE -> DashboardSubPage.ILETISIM_DUYURULAR
            }
            _uiState.value = state.copy(
                currentScreen = ScreenState.TEACHER_DASHBOARD,
                currentSubPage = targetPage,
                activeChatStudentId = if (targetPage == DashboardSubPage.ILETISIM_MESAJLAR) targetId else state.activeChatStudentId
            )
        } else if (state.currentRole == UserRole.PARENT) {
            val resolvedStudentId = if (!targetId.isNullOrBlank() && targetId in state.linkedParentStudentIds) {
                targetId
            } else {
                state.activeParentStudentId
            }
            val (tabName, isChatSubTab) = when (parsedType) {
                NotificationType.STAR_REWARD -> "STARS" to null
                NotificationType.LIBRARY -> "READING" to null
                NotificationType.TOURNAMENT -> "TOURNAMENT" to null
                NotificationType.ANNOUNCEMENT -> "COMMUNICATION" to false
                NotificationType.MESSAGE -> "COMMUNICATION" to true
                NotificationType.SCHEDULE -> "SCHEDULE" to null
                NotificationType.SEATING -> "SEATING" to null
                NotificationType.GROUP,
                NotificationType.PARENT_REGISTRATION,
                NotificationType.APP_UPDATE -> "HOME" to null
            }
            _uiState.value = state.copy(
                currentScreen = ScreenState.PARENT_PORTAL,
                activeParentStudentId = resolvedStudentId,
                requestedParentTabName = tabName,
                requestedParentCommIsChat = isChatSubTab
            )
        }
    }

    fun consumeRequestedParentTab() {
        if (_uiState.value.requestedParentTabName != null || _uiState.value.requestedParentCommIsChat != null) {
            _uiState.value = _uiState.value.copy(
                requestedParentTabName = null,
                requestedParentCommIsChat = null
            )
        }
    }

    fun launchTestSignIn(onComplete: () -> Unit = {}) {
        enterTestMode()
        onComplete()
    }

    fun enterTestMode() {
        val testUid = "test_ogretmen_preview_id"
        repository.enableTestMode(testUid)
        val defaultClass = repository.seedDefaultTestClassroom(testUid)
        loadScheduleFromStorage(defaultClass.id)
        loadSeatingPlanFromStorage(defaultClass.id)
        loadCommunicationFromStorage(defaultClass.id)
        _uiState.value = _uiState.value.copy(
            currentUser = AuthenticatedUser(
                fullName = "Cihan Özel",
                email = "cihan.ozel10@gmail.com",
                provider = "Önizleme Test Modu",
                avatarInitials = "CÖ"
            ),
            currentRole = UserRole.TEACHER,
            isKvkkAccepted = true,
            isTestMode = true,
            activeClassRoom = defaultClass,
            currentScreen = ScreenState.TEACHER_DASHBOARD,
            currentSubPage = DashboardSubPage.ANA_SAYFA,
            snackbarMessage = "Önizleme Test Modu: 4/D Sınıfı (25 Öğrenci) hazır yüklendi."
        )
        refreshChatClearStateForCurrentViewer(defaultClass.id, forceReload = true)
        ensureWebDataMigrationIfNeeded(
            classId = defaultClass.id,
            teacherUid = testUid,
            teacherEmail = "cihan.ozel10@gmail.com"
        )
    }

    private fun ensureWebDataMigrationIfNeeded(
        classId: String,
        teacherUid: String,
        teacherEmail: String,
        force: Boolean = false,
        customJsonStr: String? = null
    ) {
        val ctx = appContext ?: return
        if (!force && customJsonStr.isNullOrBlank() && !com.example.util.WebDataMigrationHelper.isTargetTeacherEmail(teacherEmail)) {
            return
        }
        val wasAlreadyMigrated = com.example.util.WebDataMigrationHelper.hasCompletedMigration(ctx, classId)
        viewModelScope.launch {
            val summary = com.example.util.WebDataMigrationHelper.migrateClassroomData(
                context = ctx,
                repository = repository,
                classId = classId,
                teacherUid = teacherUid,
                customJsonStr = customJsonStr,
                force = force
            )
            if (summary.success) {
                previousActivitySnapshot = null
                seedLocalActivitySnapshotsIfNeeded(ctx, classId, forceAllLocal = true)
                if (force || !customJsonStr.isNullOrBlank() || !wasAlreadyMigrated) {
                    _uiState.value = _uiState.value.copy(snackbarMessage = summary.message)
                }
            } else if (force || !customJsonStr.isNullOrBlank()) {
                _uiState.value = _uiState.value.copy(snackbarMessage = "⚠️ ${summary.message}")
            }
        }
    }

    fun syncWebMigrationDataNow(customJsonStr: String? = null) {
        val state = _uiState.value
        val classId = resolveActiveClassId()
        val uid = try { Firebase.auth.currentUser?.uid } catch (_: Throwable) { null } ?: "teacher_uid"
        ensureWebDataMigrationIfNeeded(
            classId = classId,
            teacherUid = uid,
            teacherEmail = state.currentUser.email,
            force = true,
            customJsonStr = customJsonStr
        )
    }

    fun refreshCurrentUser() {
        if (_uiState.value.isTestMode && Firebase.auth.currentUser == null) {
            return
        }

        val fbUser = Firebase.auth.currentUser
        if (fbUser != null) {
            val userEmail = (fbUser.email ?: "").ifBlank { "cihan.ozel10@gmail.com" }
            var name = fbUser.displayName
            if (name.isNullOrBlank() || name == "Öğretmen") {
                name = userEmail.substringBefore("@")
                    .replace(".", " ")
                    .replace("_", " ")
                    .split(" ")
                    .filter { it.isNotBlank() }
                    .joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }
                    .ifBlank { "Cihan Özel" }
            }

            val initials = name.split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .map { it.first().uppercaseChar() }
                .joinToString("")
                .ifEmpty { "CÖ" }

            val photoUrl = fbUser.photoUrl?.toString()
            _uiState.value = _uiState.value.copy(
                currentUser = AuthenticatedUser(
                    fullName = name,
                    email = userEmail,
                    provider = if (fbUser.providerData.any { it.providerId == "google.com" }) "Google Hesabı" else "E-posta",
                    avatarInitials = initials,
                    photoUrl = photoUrl
                )
            )

            viewModelScope.launch {
                val profile = repository.ensureUserProfile(fbUser.uid, name, userEmail)
                val isTeacher = profile.role == "TEACHER" || profile.role == "OGRETMEN"
                val assignedClassId = profile.assignedClassId

                // 1. Öğretmen rolü veya tanımlı sınıf kontrolü (Firestore'dan öncelikli geri yükleme)
                var resolvedClass: ClassRoom? = null
                if (!assignedClassId.isNullOrBlank()) {
                    resolvedClass = repository.fetchClassroomById(assignedClassId)
                }

                if (resolvedClass == null) {
                    resolvedClass = repository.findTeacherClassroom(fbUser.uid, userEmail)
                }

                if (resolvedClass == null && com.example.util.WebDataMigrationHelper.isTargetTeacherEmail(userEmail)) {
                    resolvedClass = repository.fetchClassroomById(repository.defaultSampleClass.id)
                    if (resolvedClass == null) {
                        val sivasCity = repository.cities.firstOrNull { it.id == "58" }
                            ?: City("58", "SİVAS", listOf("MERKEZ"))
                        try {
                            repository.addSchool(sivasCity.id, "MERKEZ", "SÜLEYMAN SAMİ KEPENEK İLKOKULU")
                            resolvedClass = repository.registerClass(
                                city = sivasCity,
                                districtName = "MERKEZ",
                                schoolName = "SÜLEYMAN SAMİ KEPENEK İLKOKULU",
                                gradeLevel = 4,
                                section = "D",
                                teacherName = name,
                                teacherEmail = userEmail
                            )
                        } catch (_: Exception) {
                            resolvedClass = repository.fetchClassroomById(repository.defaultSampleClass.id)
                        }
                    }
                    if (resolvedClass == null) {
                        resolvedClass = repository.defaultSampleClass.copy(
                            teacherId = fbUser.uid,
                            teacherName = name,
                            teacherEmail = userEmail
                        )
                    }
                }

                if (resolvedClass != null) {
                    _uiState.value = _uiState.value.copy(
                        activeClassRoom = resolvedClass,
                        currentRole = UserRole.TEACHER,
                        isKvkkAccepted = true,
                        currentScreen = ScreenState.TEACHER_DASHBOARD,
                        currentSubPage = DashboardSubPage.ANA_SAYFA
                    )
                    saveParentSessionState()

                    repository.observeStudents(resolvedClass.id)
                    ensureWebDataMigrationIfNeeded(
                        classId = resolvedClass.id,
                        teacherUid = fbUser.uid,
                        teacherEmail = userEmail
                    )
                    loadScheduleFromStorage(resolvedClass.id)
                    loadSeatingPlanFromStorage(resolvedClass.id)
                    loadCommunicationFromStorage(resolvedClass.id)
                    loadGroupMakerFromStorage(resolvedClass.id)
                    syncBackgroundWorkersAndPushToken(_uiState.value)
                    return@launch
                }

                // 2. Veli Rolü veya Öğrenci Bağlantısı Kontrolü
                val prefs = appContext?.getSharedPreferences("biriz_app_settings", Context.MODE_PRIVATE)
                val savedRole = prefs?.getString("key_saved_role", "") ?: ""
                val savedLinkedIdsStr = prefs?.getString("key_linked_student_ids", "") ?: ""
                val savedLinkedIds = if (savedLinkedIdsStr.isNotBlank()) {
                    savedLinkedIdsStr.split(",").filter { it.isNotBlank() }
                } else emptyList()

                val hasLocalParentRole = savedRole == UserRole.PARENT.code || _uiState.value.currentRole == UserRole.PARENT
                val hasLocalLinkedStudents = _uiState.value.linkedParentStudentIds.isNotEmpty() || savedLinkedIds.isNotEmpty()
                val isProfileParent = profile.role.uppercase() in listOf("PARENT", "VELI")

                // Firestore ve hafızadan e-postaya bağlı öğrenci sorgusu
                val parentLinkedMatches = repository.findStudentsLinkedToParent(userEmail)
                val hasParentLinkedStudents = parentLinkedMatches.isNotEmpty()

                if (isProfileParent || hasLocalParentRole || hasLocalLinkedStudents || hasParentLinkedStudents) {
                    val combinedLinkedStudentIds = if (hasParentLinkedStudents) {
                        parentLinkedMatches.map { it.first.id }.distinct()
                    } else {
                        (_uiState.value.linkedParentStudentIds + savedLinkedIds)
                            .distinct()
                            .filter { id ->
                                val s = repository.students.value.find { it.id == id }
                                s == null || s.isParentLinked
                            }
                    }
                    val activeStudentId = if (_uiState.value.activeParentStudentId in combinedLinkedStudentIds) {
                        _uiState.value.activeParentStudentId
                    } else {
                        combinedLinkedStudentIds.firstOrNull()
                    }
                    val parentClass = parentLinkedMatches.firstOrNull()?.second
                        ?: _uiState.value.activeClassRoom
                        ?: repository.defaultSampleClass

                    if (_uiState.value.activeClassRoom == null || _uiState.value.activeClassRoom?.id != parentClass.id) {
                        _uiState.value = _uiState.value.copy(activeClassRoom = parentClass)
                    }

                    repository.observeStudents(parentClass.id)
                    loadScheduleFromStorage(parentClass.id)
                    loadSeatingPlanFromStorage(parentClass.id)
                    loadGroupMakerFromStorage(parentClass.id)
                    loadCommunicationFromStorage(parentClass.id)

                    if (combinedLinkedStudentIds.isNotEmpty()) {
                        repository.updateParentPresence(
                            classroomId = parentClass.id,
                            studentIds = combinedLinkedStudentIds,
                            parentEmail = userEmail,
                            parentName = name,
                            isOnline = true
                        )
                    }

                    if (profile.role != "PARENT" && profile.role != "VELI") {
                        repository.updateUserRole(fbUser.uid, "PARENT")
                    }

                    _uiState.value = _uiState.value.copy(
                        currentRole = UserRole.PARENT,
                        isKvkkAccepted = true,
                        currentScreen = ScreenState.PARENT_PORTAL,
                        linkedParentStudentIds = combinedLinkedStudentIds,
                        activeParentStudentId = activeStudentId
                    )
                    reloadNotificationPreferences(_uiState.value)
                    refreshChatClearStateForCurrentViewer(parentClass.id, forceReload = true)
                    saveParentSessionState()
                    syncBackgroundWorkersAndPushToken(_uiState.value)
                    return@launch
                }

                // 3. Öğretmen olarak kayıtlı olup henüz sınıf oluşturmamış olanlar
                if (isTeacher || savedRole == UserRole.TEACHER.code) {
                    _uiState.value = _uiState.value.copy(
                        currentRole = UserRole.TEACHER,
                        isKvkkAccepted = true,
                        currentScreen = ScreenState.TEACHER_SETUP
                    )
                    saveParentSessionState()
                    return@launch
                }

                // 4. Henüz rol seçimi yapmamış yeni kullanıcılar için Rol ve KVKK ekranı
                _uiState.value = _uiState.value.copy(
                    currentRole = UserRole.MEMBER,
                    isKvkkAccepted = profile.kvkkAccepted,
                    currentScreen = if (_uiState.value.currentScreen == ScreenState.GOOGLE_SIGN_IN) {
                        ScreenState.ROLE_AND_KVKK
                    } else {
                        _uiState.value.currentScreen
                    }
                )
            }
        } else {
            _uiState.value = _uiState.value.copy(
                currentScreen = ScreenState.GOOGLE_SIGN_IN,
                activeClassRoom = null
            )
        }
    }

    // Seçilen il ve ilçeye göre Firestore'daki kayıtlı okullar
    val matchingSchools: StateFlow<List<School>> = combine(
        repository.schools,
        _uiState
    ) { allSchools, state ->
        val city = state.selectedCity ?: return@combine emptyList<School>()
        val district = state.selectedDistrict ?: return@combine emptyList<School>()
        val query = state.uppercaseSchoolName.trim()

        allSchools.filter { sch ->
            (sch.cityId == city.id || sch.cityName.equals(city.name, ignoreCase = true)) &&
            sch.districtName.equals(district, ignoreCase = true) &&
            (query.isEmpty() || sch.name.contains(query, ignoreCase = true))
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Seçilen il, ilçe, okul ve seviyeye göre şube doluluk durumları
    val branchStatuses: StateFlow<List<BranchStatus>> = combine(
        repository.registeredClasses,
        _uiState
    ) { _, state ->
        val city = state.selectedCity?.name ?: return@combine repository.allSections.map { BranchStatus(it, false) }
        val district = state.selectedDistrict ?: return@combine repository.allSections.map { BranchStatus(it, false) }
        val school = state.selectedSchool?.name ?: state.uppercaseSchoolName
        val grade = state.selectedGradeLevel ?: return@combine repository.allSections.map { BranchStatus(it, false) }

        if (school.isBlank()) {
            return@combine repository.allSections.map { BranchStatus(it, false) }
        }

        repository.getBranchStatuses(city, district, school, grade)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = repository.allSections.map { BranchStatus(it, false) }
    )

    // --- KVKK ve Rol İşlemleri ---

    fun onKvkkCheckedChanged(accepted: Boolean) {
        _uiState.value = _uiState.value.copy(
            isKvkkAccepted = accepted,
            currentRole = if (accepted) UserRole.TEACHER else UserRole.MEMBER
        )
    }

    fun openKvkkDialog() {
        _uiState.value = _uiState.value.copy(isKvkkDialogVisible = true)
    }

    fun dismissKvkkDialog() {
        _uiState.value = _uiState.value.copy(isKvkkDialogVisible = false)
    }

    fun acceptKvkkFromDialog() {
        _uiState.value = _uiState.value.copy(
            isKvkkAccepted = true,
            isKvkkDialogVisible = false,
            currentRole = UserRole.TEACHER
        )
    }

    fun selectTeacherRole() {
        if (!_uiState.value.isKvkkAccepted) {
            _uiState.value = _uiState.value.copy(
                snackbarMessage = "Devam etmek için KVKK Aydınlatma Metnini onaylamanız gerekmektedir."
            )
            return
        }
        val uid = Firebase.auth.currentUser?.uid
        if (uid != null) {
            viewModelScope.launch {
                repository.updateUserRole(uid, "TEACHER")
            }
        }
        if (_uiState.value.isTestMode && _uiState.value.activeClassRoom != null) {
            _uiState.value = _uiState.value.copy(
                currentRole = UserRole.TEACHER,
                currentScreen = ScreenState.TEACHER_DASHBOARD,
                currentSubPage = DashboardSubPage.ANA_SAYFA
            )
            refreshChatClearStateForCurrentViewer(forceReload = true)
            return
        }
        _uiState.value = _uiState.value.copy(
            currentRole = UserRole.TEACHER,
            currentScreen = ScreenState.TEACHER_SETUP
        )
        refreshChatClearStateForCurrentViewer(forceReload = true)
    }

    fun onParentRoleAttempt() {
        switchToParentRole()
    }

    fun switchToParentRole() {
        val uid = Firebase.auth.currentUser?.uid
        if (uid != null) {
            viewModelScope.launch {
                repository.updateUserRole(uid, "PARENT")
            }
        }
        val targetClass = _uiState.value.activeClassRoom ?: repository.defaultSampleClass
        if (_uiState.value.activeClassRoom == null) {
            _uiState.value = _uiState.value.copy(activeClassRoom = targetClass)
        }
        repository.observeStudents(targetClass.id)
        loadScheduleFromStorage(targetClass.id)
        loadSeatingPlanFromStorage(targetClass.id)
        loadGroupMakerFromStorage(targetClass.id)
        loadCommunicationFromStorage(targetClass.id)

        _uiState.value = _uiState.value.copy(
            currentRole = UserRole.PARENT,
            currentScreen = ScreenState.PARENT_PORTAL,
            snackbarMessage = "BİRİZ Veli Modülüne Geçiş Yapıldı 👨‍👩‍👧‍👦"
        )
        reloadNotificationPreferences(_uiState.value)
        appContext?.let { ctx ->
            seedLocalActivitySnapshotsIfNeeded(ctx, targetClass.id, forceAllLocal = !hasCloudTournaments && !hasCloudBooks)
        }
        evaluateActivityNotifications(targetClass.id, forceReloadViewerNotifications = true)
        refreshChatClearStateForCurrentViewer(targetClass.id, forceReload = true)
        saveParentSessionState()
        syncBackgroundWorkersAndPushToken(_uiState.value)
    }

    fun switchToTeacherRole() {
        if (_uiState.value.linkedParentStudentIds.isNotEmpty()) {
            repository.syncLinkedStudentsWithParent(
                _uiState.value.linkedParentStudentIds,
                _uiState.value.currentUser.email.ifBlank { "veli@biriz.app" },
                _uiState.value.currentUser.fullName.ifBlank { "Öğrenci Velisi" }
            )
        }
        if (_uiState.value.activeClassRoom != null) {
            _uiState.value = _uiState.value.copy(
                currentRole = UserRole.TEACHER,
                currentScreen = ScreenState.TEACHER_DASHBOARD,
                snackbarMessage = "Sınıf Öğretmeni Moduna Geçildi 👨‍🏫"
            )
        } else {
            _uiState.value = _uiState.value.copy(
                currentRole = UserRole.TEACHER,
                currentScreen = ScreenState.ROLE_AND_KVKK,
                snackbarMessage = "Sınıf Öğretmeni Kurulum Modu"
            )
        }
        evaluateActivityNotifications(forceReloadViewerNotifications = true)
        refreshChatClearStateForCurrentViewer(forceReload = true)
        saveParentSessionState()
        syncBackgroundWorkersAndPushToken(_uiState.value)
    }

    fun linkStudentByParentCode(codeInput: String) {
        val sanitizedInput = com.example.util.TurkishTextHelper.normalizeVeliKatilimKodu(codeInput)
        if (sanitizedInput.isBlank()) {
            _uiState.value = _uiState.value.copy(
                snackbarMessage = "⚠️ Lütfen 8 haneli veli katılım kodunu giriniz."
            )
            return
        }

        viewModelScope.launch {
            val user = _uiState.value.currentUser
            val result = repository.findAndLinkStudentByCode(
                sanitizedCode = sanitizedInput,
                currentUserEmail = user.email.ifBlank { "veli@biriz.app" },
                currentUserFullName = user.fullName.ifBlank { "Öğrenci Velisi" }
            )

            if (result != null) {
                val (matchedStudent, matchedClass) = result
                val currentList = _uiState.value.linkedParentStudentIds.toMutableList()
                if (!currentList.contains(matchedStudent.id)) {
                    currentList.add(matchedStudent.id)
                }

                val activeClass = matchedClass ?: _uiState.value.activeClassRoom ?: repository.defaultSampleClass
                if (_uiState.value.activeClassRoom == null || _uiState.value.activeClassRoom?.id != activeClass.id) {
                    _uiState.value = _uiState.value.copy(activeClassRoom = activeClass)
                }
                repository.observeStudents(activeClass.id)
                loadScheduleFromStorage(activeClass.id)
                loadSeatingPlanFromStorage(activeClass.id)
                loadGroupMakerFromStorage(activeClass.id)
                loadCommunicationFromStorage(activeClass.id)

                val uid = Firebase.auth.currentUser?.uid
                if (uid != null) {
                    repository.updateUserRole(uid, "PARENT")
                }

                _uiState.value = _uiState.value.copy(
                    linkedParentStudentIds = currentList,
                    activeParentStudentId = matchedStudent.id,
                    currentRole = UserRole.PARENT,
                    currentScreen = ScreenState.PARENT_PORTAL,
                    snackbarMessage = "🎉 ${matchedStudent.fullName} (${activeClass.gradeLevel}/${activeClass.section} - Öğr: ${activeClass.teacherName}) Veli Hesabınıza Başarıyla Bağlandı!"
                )
                reloadNotificationPreferences(_uiState.value)
                evaluateActivityNotifications(activeClass.id, forceReloadViewerNotifications = true)
                refreshChatClearStateForCurrentViewer(activeClass.id, forceReload = true)
                saveParentSessionState()
                syncBackgroundWorkersAndPushToken(_uiState.value)
            } else {
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "❌ Girilen '$sanitizedInput' kodu ile eşleşen öğrenci bulunamadı. Lütfen 8 haneli katılım kodunu kontrol ediniz."
                )
            }
        }
    }

    fun selectActiveParentStudent(studentId: String) {
        _uiState.value = _uiState.value.copy(
            activeParentStudentId = studentId
        )
        val registeredClass = repository.registeredClasses.value.firstOrNull { cls ->
            cls.id == _uiState.value.activeClassRoom?.id
        }
        val targetClass = registeredClass ?: _uiState.value.activeClassRoom ?: repository.defaultSampleClass
        if (_uiState.value.activeClassRoom?.id != targetClass.id) {
            _uiState.value = _uiState.value.copy(activeClassRoom = targetClass)
        }
        if (loadedNotifiedClassId != targetClass.id || communicationMessagesListener == null) {
            repository.observeStudents(targetClass.id)
            loadScheduleFromStorage(targetClass.id)
            loadSeatingPlanFromStorage(targetClass.id)
            loadCommunicationFromStorage(targetClass.id)
        }
        if (isParentCommunicationTabActive && isParentChatSubTabActive && isAppInForeground) {
            markChatMessagesAsRead(studentId, viewerIsTeacher = false)
        }
        saveParentSessionState()
    }

    fun unlinkStudentFromParent(studentId: String) {
        val currentList = _uiState.value.linkedParentStudentIds.toMutableList()
        currentList.remove(studentId)

        val nextActiveStudentId = if (_uiState.value.activeParentStudentId == studentId) {
            currentList.firstOrNull()
        } else {
            _uiState.value.activeParentStudentId
        }

        val studentName = repository.students.value.firstOrNull { it.id == studentId }?.fullName ?: "Öğrenci"

        _uiState.value = _uiState.value.copy(
            linkedParentStudentIds = currentList,
            activeParentStudentId = nextActiveStudentId,
            snackbarMessage = "🗑️ $studentName veli profilinizden başarıyla kaldırıldı."
        )

        saveParentSessionState()

        viewModelScope.launch {
            val classId = _uiState.value.activeClassRoom?.id
            repository.unlinkParentFromStudent(
                classroomId = classId,
                studentId = studentId,
                parentEmail = _uiState.value.currentUser.email
            )
        }
    }

    // --- Öğretmen Form Adımları ---

    fun onCitySelected(city: City) {
        _uiState.value = _uiState.value.copy(
            selectedCity = city,
            selectedDistrict = null,
            rawSchoolInput = "",
            uppercaseSchoolName = "",
            selectedSchool = null,
            selectedGradeLevel = null,
            selectedSection = null,
            conflictError = null,
            occupiedClassRoom = null
        )
    }

    fun onDistrictSelected(district: String) {
        _uiState.value = _uiState.value.copy(
            selectedDistrict = district,
            rawSchoolInput = "",
            uppercaseSchoolName = "",
            selectedSchool = null,
            selectedGradeLevel = null,
            selectedSection = null,
            conflictError = null,
            occupiedClassRoom = null
        )
    }

    fun onSchoolInputChanged(input: String) {
        val uppercase = TurkishTextHelper.toTurkishUpperCase(input)
        _uiState.value = _uiState.value.copy(
            rawSchoolInput = uppercase,
            uppercaseSchoolName = uppercase,
            selectedSchool = null,
            selectedGradeLevel = null,
            selectedSection = null,
            conflictError = null,
            occupiedClassRoom = null
        )
    }

    fun onSchoolSelected(school: School) {
        _uiState.value = _uiState.value.copy(
            selectedSchool = school,
            rawSchoolInput = school.name,
            uppercaseSchoolName = school.name,
            selectedGradeLevel = null,
            selectedSection = null,
            conflictError = null,
            occupiedClassRoom = null
        )
    }

    fun onGradeLevelSelected(level: Int) {
        _uiState.value = _uiState.value.copy(
            selectedGradeLevel = level,
            selectedSection = null,
            conflictError = null,
            occupiedClassRoom = null
        )
    }

    fun onSectionSelected(section: String) {
        val state = _uiState.value
        val city = state.selectedCity?.name ?: return
        val district = state.selectedDistrict ?: return
        val school = state.selectedSchool?.name ?: state.uppercaseSchoolName
        val grade = state.selectedGradeLevel ?: return

        val conflict = repository.checkConflict(city, district, school, grade, section)
        _uiState.value = state.copy(
            selectedSection = section,
            conflictError = if (conflict != null) {
                "Bu şubenin sınıf öğretmeni zaten kayıtlıdır (${conflict.teacherName}). Her sınıfın yalnız bir yönetici öğretmeni olabilir."
            } else null,
            occupiedClassRoom = conflict
        )
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }

    // --- Kayıt ve Tamamlama (Firebase Firestore Entegre) ---

    fun submitTeacherRegistration() {
        val state = _uiState.value
        val city = state.selectedCity
        val district = state.selectedDistrict
        val school = state.selectedSchool?.name ?: state.uppercaseSchoolName
        val grade = state.selectedGradeLevel
        val section = state.selectedSection
        val teacherName = state.currentUser.fullName
        val teacherEmail = state.currentUser.email

        if (city == null || district.isNullOrBlank()) {
            _uiState.value = state.copy(snackbarMessage = "Lütfen İl ve İlçe seçiniz.")
            return
        }
        if (school.isBlank()) {
            _uiState.value = state.copy(snackbarMessage = "Lütfen Okul Adını giriniz veya listeden seçiniz.")
            return
        }
        if (grade == null) {
            _uiState.value = state.copy(snackbarMessage = "Lütfen Sınıf Seviyesini (1-4) seçiniz.")
            return
        }
        if (section.isNullOrBlank()) {
            _uiState.value = state.copy(snackbarMessage = "Lütfen bir Şube seçiniz.")
            return
        }

        val conflict = repository.checkConflict(city.name, district, school, grade, section)
        if (conflict != null) {
            _uiState.value = state.copy(
                conflictError = "Bu şubenin sınıf öğretmeni zaten kayıtlıdır (${conflict.teacherName}). Her sınıfın yalnız bir yönetici öğretmeni olabilir.",
                occupiedClassRoom = conflict,
                snackbarMessage = "Seçilen şube dolu olduğu için kayıt yapılamaz."
            )
            return
        }

        viewModelScope.launch {
            try {
                if (state.selectedSchool == null) {
                    repository.addSchool(city.id, district, school)
                }

                val createdClass = repository.registerClass(
                    city = city,
                    districtName = district,
                    schoolName = school,
                    gradeLevel = grade,
                    section = section,
                    teacherName = teacherName,
                    teacherEmail = teacherEmail
                )

                repository.observeStudents(createdClass.id)
                val currentUid = try { Firebase.auth.currentUser?.uid } catch (_: Throwable) { null } ?: ""
                ensureWebDataMigrationIfNeeded(
                    classId = createdClass.id,
                    teacherUid = currentUid,
                    teacherEmail = teacherEmail
                )

                _uiState.value = _uiState.value.copy(
                    activeClassRoom = createdClass,
                    currentRole = UserRole.TEACHER,
                    currentScreen = ScreenState.SETUP_SUCCESS,
                    currentSubPage = DashboardSubPage.ANA_SAYFA,
                    conflictError = null,
                    occupiedClassRoom = null
                )
                saveParentSessionState()
                syncBackgroundWorkersAndPushToken(_uiState.value)
                loadScheduleFromStorage(createdClass.id)
                loadSeatingPlanFromStorage(createdClass.id)
                loadCommunicationFromStorage(createdClass.id)
                loadGroupMakerFromStorage(createdClass.id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    conflictError = e.message,
                    snackbarMessage = e.message ?: "Kayıt sırasında hata oluştu"
                )
            }
        }
    }

    fun navigateToTeacherDashboard() {
        val classId = resolveActiveClassId()
        if (classId != "default") {
            repository.observeStudents(classId)
            loadScheduleFromStorage(classId)
            loadSeatingPlanFromStorage(classId)
            loadCommunicationFromStorage(classId)
            loadGroupMakerFromStorage(classId)
        } else {
            loadScheduleFromStorage("default")
            loadSeatingPlanFromStorage("default")
            loadGroupMakerFromStorage("default")
        }
        _uiState.value = _uiState.value.copy(
            currentRole = UserRole.TEACHER,
            currentScreen = ScreenState.TEACHER_DASHBOARD,
            currentSubPage = DashboardSubPage.ANA_SAYFA
        )
        saveParentSessionState()
    }

    fun selectSubPage(subPage: DashboardSubPage) {
        val activeClassId = resolveActiveClassId()
        if (subPage == DashboardSubPage.ANA_SAYFA || subPage == DashboardSubPage.DERS_PROGRAMI) {
            loadScheduleFromStorage(activeClassId)
        }
        if (subPage == DashboardSubPage.ANA_SAYFA || subPage == DashboardSubPage.OTURMA_PLANI) {
            loadSeatingPlanFromStorage(activeClassId)
        }
        if (subPage == DashboardSubPage.ANA_SAYFA || subPage == DashboardSubPage.GRUP_OLUSTURMA) {
            loadGroupMakerFromStorage(activeClassId)
        }
        if (subPage == DashboardSubPage.ANA_SAYFA || subPage == DashboardSubPage.ILETISIM_DUYURULAR || subPage == DashboardSubPage.ILETISIM_MESAJLAR) {
            if (loadedNotifiedClassId != activeClassId || communicationMessagesListener == null) {
                loadCommunicationFromStorage(activeClassId)
            }
        }

        val nextActiveChatStudentId = if (subPage == DashboardSubPage.ILETISIM_MESAJLAR) {
            _uiState.value.activeChatStudentId
        } else {
            null
        }

        if (subPage == DashboardSubPage.ANA_SAYFA ||
            subPage == DashboardSubPage.SINIF_LISTESI ||
            subPage == DashboardSubPage.PROFIL_AYARLARI ||
            subPage.category == "ÖĞRETMEN ODASI"
        ) {
            _uiState.value = _uiState.value.copy(
                currentSubPage = subPage,
                activeChatStudentId = nextActiveChatStudentId
            )
            if (subPage == DashboardSubPage.ANA_SAYFA || subPage == DashboardSubPage.SINIF_LISTESI) {
                triggerSmartBoardSync(subPage = subPage)
            }
            return
        }

        // Kilit Kuralı: En az 1 öğrenci eklenmeden diğer 11 alt sayfaya geçilemez
        val count = repository.students.value.size
        if (count == 0) {
            _uiState.value = _uiState.value.copy(
                snackbarMessage = "Diğer modülleri kullanabilmek için lütfen önce Sınıf Listenizi oluşturun."
            )
            return
        }

        if (subPage == DashboardSubPage.OTURMA_PLANI) {
            ensureSeatingPlanInitialized()
        }
        if (subPage == DashboardSubPage.GRUP_OLUSTURMA) {
            ensureGroupMakerInitialized()
        }

        _uiState.value = _uiState.value.copy(
            currentSubPage = subPage,
            activeChatStudentId = nextActiveChatStudentId
        )

        // Akıllı Tahta Canlı Yansıtma Senkronizasyonu
        triggerSmartBoardSync(subPage = subPage)
    }

    // --- AKILLI TAHTA KUMANDASI VE CANLI YANSITMA METODLARI ---

    fun triggerSmartBoardSync(
        subPage: DashboardSubPage = _uiState.value.currentSubPage,
        day: String = _uiState.value.activeScheduleDay,
        isMirrorActive: Boolean = _uiState.value.isLiveBoardMirrorActive,
        seatingPlan: SeatingPlanState? = _uiState.value.seatingPlanState,
        groupMaker: GroupMakerState? = _uiState.value.groupMakerState,
        scheduleCfg: ScheduleConfig = _uiState.value.scheduleConfig
    ) {
        val activeCode = _uiState.value.connectedBoardSessionCode ?: return
        val ctx = appContext ?: return
        val activeClass = _uiState.value.activeClassRoom ?: return

        SmartBoardHelper.updateLiveBoardState(
            context = ctx,
            classId = activeClass.id,
            sessionCode = activeCode,
            isLiveMirrorActive = isMirrorActive,
            currentSubPage = subPage.name,
            currentSubPageTitle = subPage.title,
            activeDay = day,
            students = repository.students.value,
            schedule = _uiState.value.scheduleLessons,
            scheduleConfig = scheduleCfg,
            seatingPlanState = seatingPlan,
            groupMakerState = groupMaker
        )
    }

    fun openSmartBoardDialog() {
        _uiState.value = _uiState.value.copy(isSmartBoardDialogOpen = true, smartBoardMessage = null)
    }

    fun dismissSmartBoardDialog() {
        _uiState.value = _uiState.value.copy(isSmartBoardDialogOpen = false, smartBoardMessage = null)
    }

    fun connectSmartBoard(sessionCode: String) {
        val ctx = appContext ?: return
        val activeClass = _uiState.value.activeClassRoom ?: return
        val teacherUid = Firebase.auth.currentUser?.uid ?: "preview_teacher"
        val teacherName = _uiState.value.currentUser.fullName
        val cleanCode = SmartBoardHelper.normalizeCode(sessionCode)

        if (cleanCode.length < 4) {
            _uiState.value = _uiState.value.copy(
                smartBoardMessage = "Eşleştirme kodu en az 4 karakter olmalıdır."
            )
            return
        }

        // 1. OPTIMISTIC UI: Beklemeden ANINDA bağlan ve modalı kapat
        _uiState.value = _uiState.value.copy(
            connectedBoardSessionCode = cleanCode,
            isLiveBoardMirrorActive = true,
            isConnectingBoard = false,
            isSmartBoardDialogOpen = false,
            smartBoardMessage = null,
            snackbarMessage = "Akıllı Tahta Bağlandı: $cleanCode 🖥️"
        )
        SmartBoardHelper.saveConnectionState(ctx, cleanCode, true)

        // 2. Arka Planda Asenkron Yazma (Telefon arayüzünü ASLA kilitlemez)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                SmartBoardHelper.connectSession(
                    context = ctx,
                    sessionCode = cleanCode,
                    classId = activeClass.id,
                    teacherUid = teacherUid,
                    teacherName = teacherName,
                    schoolName = activeClass.schoolName,
                    grade = activeClass.gradeLevel,
                    branch = activeClass.section,
                    currentSubPage = _uiState.value.currentSubPage.name,
                    currentSubPageTitle = _uiState.value.currentSubPage.title,
                    activeDay = _uiState.value.activeScheduleDay,
                    students = repository.students.value,
                    schedule = _uiState.value.scheduleLessons,
                    scheduleConfig = _uiState.value.scheduleConfig,
                    seatingPlanState = _uiState.value.seatingPlanState,
                    groupMakerState = _uiState.value.groupMakerState
                )
            } catch (e: Exception) {
                Log.w("BirizViewModel", "Background board connect warning: ${e.message}")
            }
        }
    }

    fun toggleLiveBoardMirror(active: Boolean) {
        val ctx = appContext ?: return
        val code = _uiState.value.connectedBoardSessionCode
        _uiState.value = _uiState.value.copy(
            isLiveBoardMirrorActive = active,
            snackbarMessage = if (active) "Canlı Yansıtma Açık 🟢" else "Gizlilik Modu Aktif (Yayın Duraklatıldı) 🔒"
        )
        SmartBoardHelper.saveConnectionState(ctx, code, active)
        triggerSmartBoardSync(isMirrorActive = active)
    }

    fun disconnectSmartBoard() {
        val ctx = appContext ?: return
        val activeClass = _uiState.value.activeClassRoom
        val code = _uiState.value.connectedBoardSessionCode
        if (activeClass != null) {
            SmartBoardScreenCastService.stopCast(ctx, activeClass.id, code)
            SmartBoardHelper.disconnectSession(ctx, code, activeClass.id)
        }
        _uiState.value = _uiState.value.copy(
            connectedBoardSessionCode = null,
            isLiveBoardMirrorActive = false,
            isSmartBoardDialogOpen = false,
            smartBoardMessage = null,
            activeBoardCastTitle = null,
            activeBoardCastType = null,
            isBroadcastingToBoard = false,
            boardCastProgress = 0f,
            isScreenMirrorPaused = false,
            screenMirrorPauseReason = null,
            screenMirrorPackageName = null,
            snackbarMessage = "Akıllı Tahta Bağlantısı Kesildi 🔌"
        )
    }

    fun castFileToSmartBoard(uri: Uri) {
        val ctx = appContext ?: return
        val code = _uiState.value.connectedBoardSessionCode
        val activeClass = _uiState.value.activeClassRoom
        if (code.isNullOrBlank() || activeClass == null) {
            _uiState.value = _uiState.value.copy(
                smartBoardMessage = "Önce Akıllı Tahta bağlantısı kurun."
            )
            return
        }
        _uiState.value = _uiState.value.copy(
            isBroadcastingToBoard = true,
            boardCastProgress = 0.05f,
            smartBoardMessage = null
        )
        viewModelScope.launch(Dispatchers.IO) {
            val result = SmartBoardHelper.castFileToBoard(
                context = ctx,
                classId = activeClass.id,
                sessionCode = code,
                uri = uri,
                onProgress = { prog ->
                    _uiState.value = _uiState.value.copy(boardCastProgress = prog)
                }
            )
            result.fold(
                onSuccess = { name ->
                    _uiState.value = _uiState.value.copy(
                        isBroadcastingToBoard = false,
                        boardCastProgress = 1f,
                        isLiveBoardMirrorActive = true,
                        activeBoardCastTitle = name,
                        activeBoardCastType = "FILE",
                        smartBoardMessage = "✅ Tahtada yayınlandı: $name",
                        snackbarMessage = "📡 Dosya tahtada yayınlandı: $name"
                    )
                    SmartBoardHelper.saveConnectionState(ctx, code, true)
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isBroadcastingToBoard = false,
                        boardCastProgress = 0f,
                        smartBoardMessage = err.message ?: "Dosya tahtaya gönderilemedi."
                    )
                }
            )
        }
    }

    fun castUrlToSmartBoard(url: String, title: String = "", isProgram: Boolean = false) {
        val ctx = appContext ?: return
        val code = _uiState.value.connectedBoardSessionCode
        val activeClass = _uiState.value.activeClassRoom
        if (code.isNullOrBlank() || activeClass == null) {
            _uiState.value = _uiState.value.copy(
                smartBoardMessage = "Önce Akıllı Tahta bağlantısı kurun."
            )
            return
        }
        val castType = if (isProgram) "PROGRAM" else "URL"
        _uiState.value = _uiState.value.copy(
            isBroadcastingToBoard = true,
            boardCastProgress = 0.5f,
            smartBoardMessage = null
        )
        viewModelScope.launch(Dispatchers.IO) {
            val result = SmartBoardHelper.castUrlOrProgramToBoard(
                context = ctx,
                classId = activeClass.id,
                sessionCode = code,
                type = castType,
                rawUrl = url,
                title = title
            )
            result.fold(
                onSuccess = { resolvedTitle ->
                    _uiState.value = _uiState.value.copy(
                        isBroadcastingToBoard = false,
                        boardCastProgress = 1f,
                        isLiveBoardMirrorActive = true,
                        activeBoardCastTitle = resolvedTitle,
                        activeBoardCastType = castType,
                        smartBoardMessage = "✅ Tahtada yayınlandı: $resolvedTitle",
                        snackbarMessage = "📡 Tahtada açıldı: $resolvedTitle"
                    )
                    SmartBoardHelper.saveConnectionState(ctx, code, true)
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isBroadcastingToBoard = false,
                        boardCastProgress = 0f,
                        smartBoardMessage = err.message ?: "Yayın başlatılamadı."
                    )
                }
            )
        }
    }

    fun startAppScreenMirror(
        packageName: String,
        appName: String,
        resultCode: Int,
        resultData: Intent
    ) {
        val ctx = appContext ?: return
        val code = _uiState.value.connectedBoardSessionCode
        val activeClass = _uiState.value.activeClassRoom
        if (code.isNullOrBlank() || activeClass == null) {
            _uiState.value = _uiState.value.copy(
                smartBoardMessage = "Önce Akıllı Tahta bağlantısı kurun."
            )
            return
        }
        val cleanCode = SmartBoardHelper.normalizeCode(code)
        val serviceIntent = Intent(ctx, SmartBoardScreenCastService::class.java).apply {
            action = SmartBoardScreenCastService.ACTION_START
            putExtra(SmartBoardScreenCastService.EXTRA_RESULT_CODE, resultCode)
            putExtra(SmartBoardScreenCastService.EXTRA_RESULT_DATA, resultData)
            putExtra(SmartBoardScreenCastService.EXTRA_SESSION_CODE, cleanCode)
            putExtra(SmartBoardScreenCastService.EXTRA_CLASS_ID, activeClass.id)
            putExtra(SmartBoardScreenCastService.EXTRA_TARGET_PACKAGE, packageName)
            putExtra(SmartBoardScreenCastService.EXTRA_TARGET_APP_NAME, appName)
        }
        ContextCompat.startForegroundService(ctx, serviceIntent)
        SmartBoardHelper.saveConnectionState(ctx, cleanCode, true)
        _uiState.value = _uiState.value.copy(
            isLiveBoardMirrorActive = true,
            isSmartBoardDialogOpen = false,
            activeBoardCastTitle = appName,
            activeBoardCastType = "SCREEN_MIRROR",
            screenMirrorPackageName = packageName,
            isScreenMirrorPaused = false,
            screenMirrorPauseReason = null,
            smartBoardMessage = null,
            snackbarMessage = "📡 $appName açılıyor ve Tahta 'Paylaşım' sayfasına yansıtılıyor..."
        )
    }

    fun stopSmartBoardCast() {
        val ctx = appContext ?: return
        val code = _uiState.value.connectedBoardSessionCode
        val activeClass = _uiState.value.activeClassRoom
        if (activeClass != null) {
            SmartBoardScreenCastService.stopCast(ctx, activeClass.id, code)
        } else {
            SmartBoardHelper.stopBoardCast(ctx, "default", code)
        }
        _uiState.value = _uiState.value.copy(
            activeBoardCastTitle = null,
            activeBoardCastType = null,
            isBroadcastingToBoard = false,
            boardCastProgress = 0f,
            isScreenMirrorPaused = false,
            screenMirrorPauseReason = null,
            screenMirrorPackageName = null,
            smartBoardMessage = null,
            snackbarMessage = "⏹️ Paylaşım durduruldu, tahta BİRİZ menülerine döndü."
        )
    }

    fun openAddStudentDialog() {
        _uiState.value = _uiState.value.copy(isAddStudentDialogOpen = true)
    }

    fun dismissAddStudentDialog() {
        _uiState.value = _uiState.value.copy(isAddStudentDialogOpen = false)
    }

    fun addStudent(studentNo: Int, fullName: String, gender: String) {
        val classRoom = _uiState.value.activeClassRoom ?: return
        if (studentNo <= 0) {
            _uiState.value = _uiState.value.copy(snackbarMessage = "Lütfen geçerli bir okul numarası giriniz.")
            return
        }
        if (fullName.trim().length < 2) {
            _uiState.value = _uiState.value.copy(snackbarMessage = "Öğrenci adı soyadı en az 2 karakter olmalıdır.")
            return
        }

        viewModelScope.launch {
            try {
                repository.addStudent(classRoom.id, studentNo, fullName, gender)
                _uiState.value = _uiState.value.copy(
                    isAddStudentDialogOpen = false,
                    snackbarMessage = "${TurkishTextHelper.toTurkishUpperCase(fullName)} başarıyla sınıfa eklendi."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = e.message ?: "Öğrenci ekleme başarısız oldu"
                )
            }
        }
    }

    fun editStudent(studentId: String, newStudentNo: Int, newFullName: String, newGender: String) {
        val classRoom = _uiState.value.activeClassRoom ?: return
        if (newStudentNo <= 0) {
            _uiState.value = _uiState.value.copy(snackbarMessage = "Lütfen geçerli bir okul numarası giriniz.")
            return
        }
        if (newFullName.trim().length < 2) {
            _uiState.value = _uiState.value.copy(snackbarMessage = "Öğrenci adı soyadı en az 2 karakter olmalıdır.")
            return
        }

        viewModelScope.launch {
            try {
                repository.editStudent(classRoom.id, studentId, newStudentNo, newFullName, newGender)
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "${TurkishTextHelper.toTurkishUpperCase(newFullName)} bilgileri güncellendi."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = e.message ?: "Öğrenci güncelleme başarısız oldu"
                )
            }
        }
    }

    fun importStudents(studentsToAdd: List<Triple<Int, String, String>>, onComplete: (Int) -> Unit = {}) {
        val classRoom = _uiState.value.activeClassRoom ?: return
        if (studentsToAdd.isEmpty()) {
            _uiState.value = _uiState.value.copy(snackbarMessage = "Yüklenecek geçerli öğrenci verisi bulunamadı.")
            return
        }

        viewModelScope.launch {
            try {
                val added = repository.addStudentsBatch(classRoom.id, studentsToAdd)
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "$added öğrenci başarıyla sınıfa aktarıldı ve veli kodları üretildi."
                )
                onComplete(added)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = e.message ?: "Toplu yükleme sırasında hata oluştu"
                )
            }
        }
    }

    fun deleteStudent(student: com.example.data.model.Student) {
        val classRoom = _uiState.value.activeClassRoom ?: return
        viewModelScope.launch {
            try {
                repository.deleteStudent(classRoom.id, student.id)
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "${student.fullName} sınıftan silindi."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = e.message ?: "Öğrenci silinemedi"
                )
            }
        }
    }

    fun updateStudentParent(
        studentId: String,
        parentName: String?,
        parentEmail: String?,
        parentLinkedAt: String? = null,
        parentLastLoginAt: String? = null
    ) {
        val classRoom = _uiState.value.activeClassRoom ?: return
        viewModelScope.launch {
            try {
                repository.updateStudentParentInfo(
                    classroomId = classRoom.id,
                    studentId = studentId,
                    parentName = parentName,
                    parentEmail = parentEmail,
                    parentLinkedAt = parentLinkedAt,
                    parentLastLoginAt = parentLastLoginAt
                )
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = if (parentName != null) "Veli bağlantısı güncellendi: $parentName" else "Veli bağlantısı kaldırıldı."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "Veli bilgisi güncellenemedi: ${e.message}"
                )
            }
        }
    }

    // --- Esnek Ders Programı İşlemleri ---

    fun updateScheduleConfig(newConfig: ScheduleConfig) {
        val configured = newConfig.copy(isConfigured = true)
        val classId = resolveActiveClassId()
        appContext?.let { ctx ->
            SchedulePersistenceHelper.saveScheduleConfig(ctx, classId, configured)
            SchedulePersistenceHelper.uploadScheduleToFirestore(ctx, classId, configured, _uiState.value.scheduleLessons)
        }
        _uiState.value = _uiState.value.copy(
            scheduleConfig = configured,
            isScheduleSettingsStepActive = false,
            snackbarMessage = "Ders programı zaman çizelgesi ve teneffüs ayarları güncellendi."
        )
        triggerSmartBoardSync()
    }

    fun updateScheduleLesson(lesson: ScheduleLesson) {
        val upperSubjectName = TurkishTextHelper.toTurkishUpperCase(lesson.subjectName.trim()).ifEmpty { "DERS" }
        val normalizedLesson = lesson.copy(
            subjectName = upperSubjectName,
            colorTheme = lesson.colorTheme
        )

        val currentLessons = _uiState.value.scheduleLessons.toMutableList()
        val index = currentLessons.indexOfFirst {
            it.dayOfWeek == normalizedLesson.dayOfWeek && it.lessonIndex == normalizedLesson.lessonIndex
        }

        if (index >= 0) {
            currentLessons[index] = normalizedLesson
        } else {
            currentLessons.add(normalizedLesson)
        }

        // Kullanıcı İsteği: İsmi aynı olan tüm derslerin rengi birbiriyle eşitlenir.
        // Büyük/küçük harf karmaşasını önlemek için tamamı büyük harf olarak eşleştirilir ve güncellenir.
        for (i in 0 until currentLessons.size) {
            val item = currentLessons[i]
            val itemUpperName = TurkishTextHelper.toTurkishUpperCase(item.subjectName.trim())
            if (itemUpperName == upperSubjectName) {
                currentLessons[i] = item.copy(
                    subjectName = upperSubjectName,
                    colorTheme = normalizedLesson.colorTheme
                )
            }
        }

        val classId = resolveActiveClassId()
        appContext?.let { ctx ->
            SchedulePersistenceHelper.saveScheduleLessons(ctx, classId, currentLessons)
            SchedulePersistenceHelper.uploadScheduleToFirestore(ctx, classId, _uiState.value.scheduleConfig, currentLessons)
        }

        _uiState.value = _uiState.value.copy(
            scheduleLessons = currentLessons,
            snackbarMessage = "${normalizedLesson.lessonIndex}. Ders ($upperSubjectName) ve aynı adlı tüm derslerin rengi eşitlendi."
        )
        triggerSmartBoardSync()
    }

    fun setScheduleSettingsStepActive(active: Boolean) {
        _uiState.value = _uiState.value.copy(isScheduleSettingsStepActive = active)
    }

    fun setActiveScheduleDay(day: String) {
        val classId = resolveActiveClassId()
        appContext?.let { ctx ->
            SchedulePersistenceHelper.saveActiveDay(ctx, classId, day)
        }
        _uiState.value = _uiState.value.copy(activeScheduleDay = day)

        // Akıllı Tahta Canlı Gün Güncellemesi
        triggerSmartBoardSync(day = day)
    }

    // --- AKILLI SINIF OTURMA PLANI METODLARI ---

    private fun persistActiveSeatingPlan(state: SeatingPlanState) {
        val classId = resolveActiveClassId()
        appContext?.let { ctx ->
            SeatingPlanPersistenceHelper.saveActivePlan(ctx, classId, state)
        }
        triggerSmartBoardSync(seatingPlan = state)
    }

    fun updateSeatingConfig(newConfig: SeatingConfig) {
        val currentPlan = _uiState.value.seatingPlanState
        val validSeatIds = SeatingPlanHelper.generateAllSeatIdsFrontToBack(newConfig).toSet()
        val filteredAssignments = currentPlan.assignments.filterKeys { validSeatIds.contains(it) }

        val updated = currentPlan.copy(
            config = newConfig,
            assignments = filteredAssignments
        )
        val classId = resolveActiveClassId()
        appContext?.let { ctx ->
            SeatingPlanPersistenceHelper.saveSeatingConfig(ctx, classId, newConfig)
        }
        persistActiveSeatingPlan(updated)
        _uiState.value = _uiState.value.copy(
            seatingPlanState = updated,
            isSeatingConfigOpen = false,
            snackbarMessage = "Sınıf sıra düzeni (${newConfig.groupCount} grup, ${newConfig.totalSeats} koltuk) güncellendi."
        )
    }

    fun setSeatingConfigDialogOpen(isOpen: Boolean) {
        _uiState.value = _uiState.value.copy(isSeatingConfigOpen = isOpen)
    }

    fun autoAssignSeating(mode: SeatingPlacementMode) {
        if (mode == SeatingPlacementMode.MANUAL || mode == SeatingPlacementMode.PLANNED) {
            return
        }
        val currentStudents = repository.students.value
        val updatedPlan = SeatingPlanHelper.autoAssignSeats(
            students = currentStudents,
            currentState = _uiState.value.seatingPlanState,
            mode = mode
        )
        val planName = "${mode.title} (${SeatingPlanPersistenceHelper.generateDefaultPlanName()})"
        val namedPlan = updatedPlan.copy(activePlanName = planName)
        persistActiveSeatingPlan(namedPlan)

        // Otomatik dağıtılan planı arşive kaydet
        val classId = _uiState.value.activeClassRoom?.id ?: "default"
        appContext?.let { ctx ->
            SeatingPlanPersistenceHelper.saveArchivedPlan(
                ctx,
                classId,
                ArchivedSeatingPlan(name = planName, planState = namedPlan)
            )
        }

        _uiState.value = _uiState.value.copy(
            seatingPlanState = namedPlan,
            snackbarMessage = "Oturma planı '${mode.title}' kuralına göre akıllıca dağıtıldı ve arşivlendi."
        )
    }

    fun applyPlannedSeating(newState: SeatingPlanState) {
        persistActiveSeatingPlan(newState)
        val classId = _uiState.value.activeClassRoom?.id ?: "default"
        appContext?.let { ctx ->
            SeatingPlanPersistenceHelper.saveArchivedPlan(
                ctx,
                classId,
                ArchivedSeatingPlan(name = newState.activePlanName, planState = newState)
            )
        }
        _uiState.value = _uiState.value.copy(
            seatingPlanState = newState,
            snackbarMessage = "'${newState.activePlanName}' başarıyla oluşturuldu ve arşive kaydedildi."
        )
    }

    fun applyArchivedPlan(archivedPlan: ArchivedSeatingPlan) {
        val newState = archivedPlan.planState.copy(activePlanName = archivedPlan.name)
        persistActiveSeatingPlan(newState)
        _uiState.value = _uiState.value.copy(
            seatingPlanState = newState,
            snackbarMessage = "'${archivedPlan.name}' başarıyla uygulandı."
        )
    }

    fun assignStudentToSeat(seatId: String, studentId: String?) {
        val currentStudents = repository.students.value
        val currentPlan = SeatingPlanHelper.reconcileAndSanitizePlan(_uiState.value.seatingPlanState, currentStudents)
        val newAssignments = SeatingPlanHelper.assignStudent(currentPlan.assignments, seatId, studentId)
        val updated = currentPlan.copy(assignments = newAssignments)
        persistActiveSeatingPlan(updated)
        val placedStudent = if (!studentId.isNullOrBlank()) currentStudents.firstOrNull { it.id == studentId } else null
        val coord = SeatingPlanHelper.parseSeatId(seatId)
        val seatLabel = if (coord != null) "${coord.groupIndex + 1}. Grup ${coord.rowIndex + 1}. Sıra ${coord.seatIndex + 1}. Koltuk" else "koltuk"
        _uiState.value = _uiState.value.copy(
            seatingPlanState = updated,
            snackbarMessage = if (placedStudent != null) {
                "✅ ${placedStudent.fullName} → $seatLabel konumuna yerleştirildi."
            } else {
                "🪑 $seatLabel boşaltıldı."
            }
        )
    }

    fun swapSeats(seatId1: String, seatId2: String) {
        val currentStudents = repository.students.value
        val currentPlan = SeatingPlanHelper.reconcileAndSanitizePlan(_uiState.value.seatingPlanState, currentStudents)
        val newAssignments = SeatingPlanHelper.swapSeats(currentPlan.assignments, seatId1, seatId2)
        val updated = currentPlan.copy(assignments = newAssignments)
        persistActiveSeatingPlan(updated)
        _uiState.value = _uiState.value.copy(
            seatingPlanState = updated,
            snackbarMessage = "Koltuklar başarıyla takas edildi."
        )
    }

    fun togglePinStudent(studentId: String) {
        val currentPlan = _uiState.value.seatingPlanState
        val newPinned = if (currentPlan.pinnedStudents.contains(studentId)) {
            currentPlan.pinnedStudents - studentId
        } else {
            currentPlan.pinnedStudents + studentId
        }
        val student = repository.students.value.firstOrNull { it.id == studentId }
        val isNowPinned = newPinned.contains(studentId)
        val updated = currentPlan.copy(pinnedStudents = newPinned)
        persistActiveSeatingPlan(updated)
        _uiState.value = _uiState.value.copy(
            seatingPlanState = updated,
            snackbarMessage = if (isNowPinned) "${student?.fullName ?: "Öğrenci"} bu sıraya sabitlendi (🔒 Kilitli)."
            else "${student?.fullName ?: "Öğrenci"} sırası serbest bırakıldı."
        )
    }

    fun togglePriorityStudent(studentId: String) {
        val currentPlan = _uiState.value.seatingPlanState
        val isNowPriority = repository.toggleStudentPriority(studentId)
        val newPriority = if (isNowPriority) {
            currentPlan.priorityStudents + studentId
        } else {
            currentPlan.priorityStudents - studentId
        }
        val student = repository.students.value.firstOrNull { it.id == studentId }
        val updated = currentPlan.copy(priorityStudents = newPriority)
        persistActiveSeatingPlan(updated)
        _uiState.value = _uiState.value.copy(
            seatingPlanState = updated,
            snackbarMessage = if (isNowPriority) "${student?.fullName ?: "Öğrenci"} ön sıra önceliğine (👓) alındı."
            else "${student?.fullName ?: "Öğrenci"} öncelik listesinden çıkarıldı."
        )
    }

    fun clearSeatingPlan(clearFixed: Boolean = false) {
        val currentStudents = repository.students.value
        val currentPlan = SeatingPlanHelper.reconcileAndSanitizePlan(_uiState.value.seatingPlanState, currentStudents)
        val newAssignments = if (clearFixed) {
            emptyMap()
        } else {
            currentPlan.assignments.filter { (_, studentId) ->
                currentPlan.pinnedStudents.contains(studentId)
            }
        }
        val newPinned = if (clearFixed) emptySet() else currentPlan.pinnedStudents
        val updated = currentPlan.copy(
            assignments = newAssignments,
            pinnedStudents = newPinned,
            activePlanName = "Elle Yerleştirme Planı"
        )
        persistActiveSeatingPlan(updated)
        _uiState.value = _uiState.value.copy(
            seatingPlanState = updated,
            snackbarMessage = if (clearFixed) "Tüm koltuklar boşaltıldı. Boş koltuklara dokunarak öğrencileri elle yerleştirebilirsiniz."
            else "Sabitlenenler hariç tüm koltuklar boşaltıldı. Boş koltuklara dokunarak elle yerleştirebilirsiniz."
        )
    }

    fun ensureSeatingPlanInitialized() {
        val currentStudents = repository.students.value
        val currentPlan = SeatingPlanHelper.reconcileAndSanitizePlan(_uiState.value.seatingPlanState, currentStudents)
        if (currentPlan != _uiState.value.seatingPlanState) {
            _uiState.value = _uiState.value.copy(seatingPlanState = currentPlan)
        }
        val classId = resolveActiveClassId()
        val rawSaved = appContext?.let { SeatingPlanPersistenceHelper.loadActivePlan(it, classId) }
        val saved = rawSaved?.let { SeatingPlanHelper.reconcileAndSanitizePlan(it, currentStudents) }
        val savedConfig = appContext?.let { SeatingPlanPersistenceHelper.loadSeatingConfig(it, classId) }
        val effectiveConfig = saved?.config ?: savedConfig ?: currentPlan.config

        if (saved != null) {
            if (currentPlan.assignments.isEmpty() && currentStudents.isNotEmpty()) {
                val wasExplicitlyClearedForManual = saved.activePlanName.contains("Elle", ignoreCase = true) ||
                    currentPlan.activePlanName.contains("Elle", ignoreCase = true)
                if (saved.assignments.isNotEmpty() || wasExplicitlyClearedForManual) {
                    _uiState.value = _uiState.value.copy(seatingPlanState = saved)
                } else {
                    val initial = SeatingPlanHelper.generateInitialAssignments(currentStudents, effectiveConfig)
                    val initialPlan = saved.copy(
                        config = effectiveConfig,
                        assignments = initial,
                        activePlanName = "1. Başlangıç Planı"
                    )
                    persistActiveSeatingPlan(initialPlan)
                    _uiState.value = _uiState.value.copy(seatingPlanState = initialPlan)
                }
            }
        } else if (currentPlan.assignments.isEmpty() && currentStudents.isNotEmpty()) {
            val initial = SeatingPlanHelper.generateInitialAssignments(currentStudents, effectiveConfig)
            val initialPlan = currentPlan.copy(
                config = effectiveConfig,
                assignments = initial,
                activePlanName = "1. Başlangıç Planı"
            )
            _uiState.value = _uiState.value.copy(seatingPlanState = initialPlan)
            val ctx = appContext
            if (ctx != null && classId != "default") {
                SeatingPlanPersistenceHelper.syncActivePlanFromFirestore(ctx, classId) { cloudState ->
                    val reconciledCloud = SeatingPlanHelper.reconcileAndSanitizePlan(cloudState, repository.students.value)
                    if (reconciledCloud.assignments.isNotEmpty()) {
                        _uiState.value = _uiState.value.copy(seatingPlanState = reconciledCloud)
                    } else {
                        persistActiveSeatingPlan(initialPlan)
                    }
                }
            } else {
                persistActiveSeatingPlan(initialPlan)
            }
        }
    }

    // --- AKILLI GRUP OLUŞTURUCU METOTLARI ---

    fun openGroupWizard() {
        _uiState.value = _uiState.value.copy(
            groupMakerState = _uiState.value.groupMakerState.copy(isWizardOpen = true)
        )
    }

    fun closeGroupWizard() {
        _uiState.value = _uiState.value.copy(
            groupMakerState = _uiState.value.groupMakerState.copy(isWizardOpen = false)
        )
    }

    fun saveGroupConfig(config: GroupDistributionConfig) {
        val newState = _uiState.value.groupMakerState.copy(config = config)
        _uiState.value = _uiState.value.copy(groupMakerState = newState)
        persistGroupMakerState(newState)
    }

    fun distributeGroups(config: GroupDistributionConfig? = null) {
        val activeConfig = config ?: _uiState.value.groupMakerState.config
        val currentStudents = repository.students.value
        val (newGroups, unassigned) = GroupMakerHelper.distributeStudents(
            allStudents = currentStudents,
            config = activeConfig,
            existingGroups = _uiState.value.groupMakerState.groups
        )
        val newState = _uiState.value.groupMakerState.copy(
            groups = newGroups,
            unassignedStudentIds = unassigned,
            config = activeConfig,
            isWizardOpen = false
        )
        _uiState.value = _uiState.value.copy(
            groupMakerState = newState,
            snackbarMessage = "${newGroups.size} adet çalışma grubu başarıyla oluşturuldu! 🎉"
        )
        persistGroupMakerState(newState)
    }

    fun reshuffleGroups() {
        val current = _uiState.value.groupMakerState
        val currentStudents = repository.students.value
        val reshuffled = GroupMakerHelper.reshuffleStudents(
            allStudents = currentStudents,
            currentGroups = current.groups,
            balanceGender = current.config.balanceGender
        )
        val newState = current.copy(groups = reshuffled)
        _uiState.value = _uiState.value.copy(
            groupMakerState = newState,
            snackbarMessage = "Öğrenciler mevcut gruplar arasında yeniden karıştırıldı 🎲"
        )
        persistGroupMakerState(newState)
    }

    fun distributeUnassignedGroups() {
        val current = _uiState.value.groupMakerState
        val currentStudents = repository.students.value
        val (updatedGroups, newUnassigned) = GroupMakerHelper.distributeUnassigned(
            unassignedIds = current.unassignedStudentIds,
            allStudents = currentStudents,
            currentGroups = current.groups,
            balanceGender = current.config.balanceGender
        )
        val newState = current.copy(
            groups = updatedGroups,
            unassignedStudentIds = newUnassigned
        )
        _uiState.value = _uiState.value.copy(
            groupMakerState = newState,
            snackbarMessage = "Kalan öğrenciler mevcut gruplara dengeli olarak dağıtıldı ✨"
        )
        persistGroupMakerState(newState)
    }

    fun pickRandomLeadersForAllGroups() {
        val current = _uiState.value.groupMakerState
        val updated = GroupMakerHelper.pickRandomLeadersForAll(current.groups)
        val newState = current.copy(groups = updated)
        _uiState.value = _uiState.value.copy(
            groupMakerState = newState,
            snackbarMessage = "Tüm gruplar için rastgele birer lider seçildi 👑"
        )
        persistGroupMakerState(newState)
    }

    fun pickRandomLeaderForSingleGroup(groupId: String) {
        val current = _uiState.value.groupMakerState
        val updated = current.groups.map { group ->
            if (group.id == groupId) GroupMakerHelper.pickRandomLeaderForGroup(group) else group
        }
        val target = updated.firstOrNull { it.id == groupId }
        val leader = repository.students.value.firstOrNull { it.id == target?.leaderStudentId }
        val newState = current.copy(groups = updated)
        _uiState.value = _uiState.value.copy(
            groupMakerState = newState,
            snackbarMessage = if (leader != null) "${target?.name} lideri olarak ${leader.fullName} seçildi 👑" else "Lider seçilemedi."
        )
        persistGroupMakerState(newState)
    }

    fun toggleStudentLeader(groupId: String, studentId: String) {
        val current = _uiState.value.groupMakerState
        val student = repository.students.value.firstOrNull { it.id == studentId }
        val updated = current.groups.map { group ->
            if (group.id == groupId) {
                val newLeader = if (group.leaderStudentId == studentId) null else studentId
                group.copy(leaderStudentId = newLeader)
            } else group
        }
        val isNowLeader = updated.firstOrNull { it.id == groupId }?.leaderStudentId == studentId
        val newState = current.copy(groups = updated)
        _uiState.value = _uiState.value.copy(
            groupMakerState = newState,
            snackbarMessage = if (isNowLeader) "${student?.fullName ?: "Öğrenci"} grup lideri yapıldı 👑"
            else "${student?.fullName ?: "Öğrenci"} liderlik unvanı kaldırıldı."
        )
        persistGroupMakerState(newState)
    }

    fun renameGroup(groupId: String, newName: String) {
        val current = _uiState.value.groupMakerState
        val updated = current.groups.map { group ->
            if (group.id == groupId) group.copy(name = newName) else group
        }
        val newState = current.copy(groups = updated)
        _uiState.value = _uiState.value.copy(
            groupMakerState = newState,
            snackbarMessage = "Grup adı güncellendi: $newName ✏️"
        )
        persistGroupMakerState(newState)
    }

    fun changeGroupColorTheme(groupId: String, theme: GroupColorTheme) {
        val current = _uiState.value.groupMakerState
        val updated = current.groups.map { group ->
            if (group.id == groupId) group.copy(colorTheme = theme) else group
        }
        val newState = current.copy(groups = updated)
        _uiState.value = _uiState.value.copy(
            groupMakerState = newState,
            snackbarMessage = "Grup teması değiştirildi: ${theme.title} 🎨"
        )
        persistGroupMakerState(newState)
    }

    fun addNewGroup() {
        val current = _uiState.value.groupMakerState
        val colorThemes = GroupColorTheme.values()
        val nextIdx = current.groups.size
        val presetName = GroupMakerHelper.FUN_GROUP_NAMES.getOrNull(nextIdx) ?: "${nextIdx + 1}. Grup"
        val newGroup = GroupTeam(
            id = java.util.UUID.randomUUID().toString(),
            name = presetName,
            colorTheme = colorThemes[nextIdx % colorThemes.size],
            studentIds = emptyList()
        )
        val newState = current.copy(groups = current.groups + newGroup)
        _uiState.value = _uiState.value.copy(
            groupMakerState = newState,
            snackbarMessage = "Yeni boş grup eklendi: $presetName ➕"
        )
        persistGroupMakerState(newState)
    }

    fun deleteGroup(groupId: String) {
        val current = _uiState.value.groupMakerState
        val groupToDelete = current.groups.firstOrNull { it.id == groupId }
        val (updatedGroups, newUnassigned) = GroupMakerHelper.deleteGroup(
            groups = current.groups,
            unassigned = current.unassignedStudentIds,
            groupId = groupId
        )
        val newState = current.copy(
            groups = updatedGroups,
            unassignedStudentIds = newUnassigned
        )
        _uiState.value = _uiState.value.copy(
            groupMakerState = newState,
            snackbarMessage = "${groupToDelete?.name ?: "Grup"} silindi. Öğrenciler Atanmamışlar Havuzuna aktarıldı 🗑️"
        )
        persistGroupMakerState(newState)
    }

    fun openSwapModal(studentId: String) {
        _uiState.value = _uiState.value.copy(
            groupMakerState = _uiState.value.groupMakerState.copy(activeSwapStudentId = studentId)
        )
    }

    fun closeSwapModal() {
        _uiState.value = _uiState.value.copy(
            groupMakerState = _uiState.value.groupMakerState.copy(activeSwapStudentId = null)
        )
    }

    fun swapStudents(studentAId: String, studentBId: String) {
        val current = _uiState.value.groupMakerState
        val updated = GroupMakerHelper.swapStudents(current.groups, studentAId, studentBId)
        val studentA = repository.students.value.firstOrNull { it.id == studentAId }
        val studentB = repository.students.value.firstOrNull { it.id == studentBId }
        val newState = current.copy(groups = updated, activeSwapStudentId = null)
        _uiState.value = _uiState.value.copy(
            groupMakerState = newState,
            snackbarMessage = "${studentA?.fullName ?: "Öğrenci 1"} ve ${studentB?.fullName ?: "Öğrenci 2"} karşılıklı takas edildi ⇄"
        )
        persistGroupMakerState(newState)
    }

    fun moveStudentToGroup(studentId: String, targetGroupId: String) {
        val current = _uiState.value.groupMakerState
        val (updatedGroups, newUnassigned) = GroupMakerHelper.moveStudentToGroup(
            groups = current.groups,
            unassigned = current.unassignedStudentIds,
            studentId = studentId,
            targetGroupId = targetGroupId
        )
        val student = repository.students.value.firstOrNull { it.id == studentId }
        val targetGroup = updatedGroups.firstOrNull { it.id == targetGroupId }
        val newState = current.copy(
            groups = updatedGroups,
            unassignedStudentIds = newUnassigned,
            activeSwapStudentId = null
        )
        _uiState.value = _uiState.value.copy(
            groupMakerState = newState,
            snackbarMessage = "${student?.fullName ?: "Öğrenci"} -> ${targetGroup?.name ?: "Yeni Grup"} grubuna taşındı ➡️"
        )
        persistGroupMakerState(newState)
    }

    fun removeStudentFromGroup(studentId: String) {
        val current = _uiState.value.groupMakerState
        val (updatedGroups, newUnassigned) = GroupMakerHelper.removeStudentFromGroup(
            groups = current.groups,
            unassigned = current.unassignedStudentIds,
            studentId = studentId
        )
        val student = repository.students.value.firstOrNull { it.id == studentId }
        val newState = current.copy(
            groups = updatedGroups,
            unassignedStudentIds = newUnassigned,
            activeSwapStudentId = null
        )
        _uiState.value = _uiState.value.copy(
            groupMakerState = newState,
            snackbarMessage = "${student?.fullName ?: "Öğrenci"} gruptan çıkarıldı ve Atanmamışlar Havuzuna alındı 📦"
        )
        persistGroupMakerState(newState)
    }

    fun togglePresenterMode() {
        val current = _uiState.value.groupMakerState
        val newMode = !current.isPresenterMode
        val newState = current.copy(isPresenterMode = newMode)
        _uiState.value = _uiState.value.copy(
            groupMakerState = newState,
            snackbarMessage = if (newMode) "Akıllı Tahta / Sunum Modu Açıldı 📺" else "Sunum Modundan Çıkıldı ↩️"
        )
        persistGroupMakerState(newState)
    }

    fun ensureGroupMakerInitialized() {
        val classId = _uiState.value.activeClassRoom?.id ?: "default"
        if (appContext != null) {
            val saved = GroupMakerPersistenceHelper.loadState(appContext, classId)
            if (saved != null && saved.groups.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(groupMakerState = saved)
                return
            }
        }
        val current = _uiState.value.groupMakerState
        val currentStudents = repository.students.value
        if (current.groups.isEmpty() && currentStudents.isNotEmpty()) {
            val (initialGroups, unassigned) = GroupMakerHelper.distributeStudents(
                allStudents = currentStudents,
                config = current.config
            )
            val newState = current.copy(
                groups = initialGroups,
                unassignedStudentIds = unassigned
            )
            _uiState.value = _uiState.value.copy(
                groupMakerState = newState
            )
            persistGroupMakerState(newState)
        }
    }

    fun deleteTeacherRoleAndClassroom() {
        val classRoom = _uiState.value.activeClassRoom ?: return
        _uiState.value = _uiState.value.copy(isDeleting = true)
        viewModelScope.launch {
            try {
                repository.deleteTeacherRoleAndClassroom(classRoom.id)
                _uiState.value = _uiState.value.copy(
                    activeClassRoom = null,
                    currentRole = UserRole.MEMBER,
                    currentScreen = ScreenState.ROLE_AND_KVKK,
                    currentSubPage = DashboardSubPage.SINIF_LISTESI,
                    isDeleting = false,
                    selectedCity = null,
                    selectedDistrict = null,
                    rawSchoolInput = "",
                    uppercaseSchoolName = "",
                    selectedSchool = null,
                    selectedGradeLevel = null,
                    selectedSection = null,
                    snackbarMessage = "Öğretmen rolünüz ve sınıf verileriniz veritabanından kalıcı olarak silindi. Rol Seçimine dönüldü."
                )
                saveParentSessionState()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDeleting = false,
                    snackbarMessage = e.message ?: "Silme sırasında hata oluştu"
                )
            }
        }
    }

    fun deleteParentRole() {
        val uid = Firebase.auth.currentUser?.uid
        if (uid != null) {
            viewModelScope.launch {
                repository.updateUserRole(uid, "UYE")
            }
        }
        _uiState.value = _uiState.value.copy(
            currentRole = UserRole.MEMBER,
            currentScreen = ScreenState.ROLE_AND_KVKK,
            linkedParentStudentIds = emptyList(),
            activeParentStudentId = null,
            snackbarMessage = "Veli rolünüz ve bağlı tüm öğrenciler başarıyla silindi. Rol Seçimine dönüldü."
        )
        saveParentSessionState()
    }

    fun deleteEntireAccount(context: android.content.Context? = null) {
        val classRoom = _uiState.value.activeClassRoom
        _uiState.value = _uiState.value.copy(isDeleting = true)
        viewModelScope.launch {
            try {
                repository.deleteEntireAccount(classRoom?.id)
                signOut(context)
                _uiState.value = _uiState.value.copy(
                    isDeleting = false,
                    snackbarMessage = "Hesabınız ve tüm verileriniz kalıcı olarak silindi."
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDeleting = false,
                    snackbarMessage = e.message ?: "Hesap silinirken hata oluştu"
                )
            }
        }
    }

    fun resetToRoleSelection() {
        _uiState.value = _uiState.value.copy(
            currentScreen = ScreenState.ROLE_AND_KVKK,
            selectedSection = null,
            conflictError = null,
            occupiedClassRoom = null
        )
    }

    fun testAnotherClass() {
        _uiState.value = _uiState.value.copy(
            currentScreen = ScreenState.TEACHER_SETUP,
            selectedSection = null,
            conflictError = null,
            occupiedClassRoom = null
        )
    }

    fun toggleTheme() {
        val newTheme = !_uiState.value.isDarkTheme
        _uiState.value = _uiState.value.copy(
            isDarkTheme = newTheme,
            snackbarMessage = if (newTheme) "Koyu Tema Aktif Edildi 🌙" else "Açık Tema Aktif Edildi ☀️"
        )
        appContext?.let { ctx ->
            val prefs = ctx.getSharedPreferences("biriz_app_settings", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("key_is_dark_theme", newTheme).apply()
        }
    }

    fun signOut(context: android.content.Context? = null) {
        val prevState = _uiState.value
        if (prevState.currentRole == UserRole.PARENT && prevState.linkedParentStudentIds.isNotEmpty()) {
            val cid = resolveActiveClassId()
            val email = prevState.currentUser.email.ifBlank { "veli@biriz.app" }
            val name = prevState.currentUser.fullName.ifBlank { "Öğrenci Velisi" }
            viewModelScope.launch {
                try {
                    repository.updateParentPresence(
                        classroomId = cid,
                        studentIds = prevState.linkedParentStudentIds,
                        parentEmail = email,
                        parentName = name,
                        isOnline = false
                    )
                } catch (_: Exception) {}
                Firebase.auth.signOut()
            }
        } else if (prevState.currentRole == UserRole.TEACHER && prevState.activeClassRoom != null) {
            val cid = prevState.activeClassRoom.id
            viewModelScope.launch {
                try {
                    repository.updateTeacherPresence(
                        classroomId = cid,
                        isOnline = false
                    )
                } catch (_: Exception) {}
                Firebase.auth.signOut()
            }
        } else {
            Firebase.auth.signOut()
        }
        disconnectSmartBoard()
        repository.disableTestMode()
        clearActivityListeners()
        observedNotificationClassId = null
        loadedActivityStateKey = null
        previousActivitySnapshot = null
        val cleanupCtx = context ?: appContext
        cleanupCtx?.let { ctx ->
            ctx.getSharedPreferences("biriz_app_settings", Context.MODE_PRIVATE).edit()
                .remove("key_saved_role")
                .remove("key_linked_classroom_id")
                .remove("key_linked_student_ids")
                .remove("key_active_parent_student_id")
                .remove("key_user_email")
                .remove("key_user_name")
                .apply()
            BackgroundNotificationSyncHelper.cancelAllBackgroundSync(ctx)
        }
        context?.let { ctx ->
            viewModelScope.launch {
                com.example.auth.GoogleAuthHelper.clearCredentialState(ctx)
            }
        }
        _uiState.value = _uiState.value.copy(
            currentScreen = ScreenState.GOOGLE_SIGN_IN,
            isTestMode = false,
            isKvkkAccepted = false,
            activeClassRoom = null,
            currentRole = UserRole.MEMBER,
            selectedCity = null,
            selectedDistrict = null,
            rawSchoolInput = "",
            uppercaseSchoolName = "",
            selectedSchool = null,
            selectedGradeLevel = null,
            selectedSection = null
        )
    }

    override fun onCleared() {
        super.onCleared()
        scheduleListenerRegistration?.remove()
        clearActivityListeners()
        seatingPlanListenerRegistration?.remove()
        groupMakerListenerRegistration?.remove()
    }
}

class BirizViewModelFactory(
    private val repository: ClassRegistryRepository,
    private val context: Context? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BirizViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BirizViewModel(repository, context?.applicationContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
