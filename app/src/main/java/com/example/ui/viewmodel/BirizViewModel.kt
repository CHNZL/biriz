package com.example.ui.viewmodel

import android.content.Context
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
import com.example.data.model.ChatMessage
import com.example.data.model.MessageStatus
import com.example.data.model.NotificationType
import com.example.util.CommunicationPersistenceHelper
import com.example.util.AppUpdateManager
import com.example.util.AppUpdateNotificationHelper
import com.example.util.SmartBoardHelper
import com.example.util.UpdateInfo
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.ListenerRegistration
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
    val currentScreen: ScreenState = if (Firebase.auth.currentUser == null) ScreenState.GOOGLE_SIGN_IN else ScreenState.ROLE_AND_KVKK,
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
    val currentSubPage: DashboardSubPage = DashboardSubPage.SINIF_LISTESI,
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
    private var seatingPlanListenerRegistration: ListenerRegistration? = null

    init {
        loadScheduleFromStorage("default")
        loadSeatingPlanFromStorage("default")
        loadCommunicationFromStorage("default")
        loadGroupMakerFromStorage("default")
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

            if (!savedClassroomId.isNullOrBlank()) {
                repository.observeStudents(savedClassroomId)
                loadScheduleFromStorage(savedClassroomId)
                loadSeatingPlanFromStorage(savedClassroomId)
                loadCommunicationFromStorage(savedClassroomId)
            }

            val restoredRole = if (savedRoleCode == UserRole.PARENT.code) {
                UserRole.PARENT
            } else {
                _uiState.value.currentRole
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
                currentRole = restoredRole,
                currentScreen = restoredScreen,
                linkedParentStudentIds = linkedList,
                activeParentStudentId = if (savedActiveStudentId.isNullOrBlank()) linkedList.firstOrNull() else savedActiveStudentId
            )

            if (linkedList.isNotEmpty()) {
                repository.syncLinkedStudentsWithParent(
                    linkedList,
                    _uiState.value.currentUser.email.ifBlank { "veli@biriz.app" },
                    _uiState.value.currentUser.fullName.ifBlank { "Öğrenci Velisi" }
                )
            }
        }
        refreshCurrentUser()
        startPeriodicUpdateChecker()
    }

    private fun saveParentSessionState() {
        if (appContext == null) return
        val prefs = appContext.getSharedPreferences("biriz_app_settings", Context.MODE_PRIVATE)
        val state = _uiState.value
        prefs.edit()
            .putString("key_saved_role", state.currentRole.code)
            .putString("key_linked_student_ids", state.linkedParentStudentIds.joinToString(","))
            .putString("key_active_parent_student_id", state.activeParentStudentId ?: "")
            .putString("key_linked_classroom_id", state.activeClassRoom?.id ?: "")
            .apply()
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

    suspend fun checkUpdatesSilently(forceResendNotification: Boolean = false) {
        val ctx = appContext ?: return
        try {
            val update = AppUpdateManager.checkForUpdates(ctx)
            if (update != null && update.isUpdateAvailable) {
                _uiState.value = _uiState.value.copy(
                    availableUpdateInfo = update,
                    isUpdateBannerDismissed = false
                )
                syncUpdateNotification(update)
                AppUpdateNotificationHelper.showUpdateNotification(ctx, update, forceResend = forceResendNotification)
            }
        } catch (_: Exception) {}
    }

    private fun syncUpdateNotification(update: UpdateInfo) {
        if (!update.isUpdateAvailable) return
        val ctx = appContext ?: return
        val updateNotifId = "app_update_${update.latestVersionCode}"
        val currentNotifs = _uiState.value.notifications
        if (currentNotifs.none { it.id == updateNotifId }) {
            val updateNotif = BirizNotification(
                id = updateNotifId,
                title = "🚀 Yeni Güncelleme Yayında! (v${update.latestVersionName})",
                message = "Yeni sürüm kullanıma hazır. Dokunarak yenilikleri inceleyin ve güncelleyin.",
                timestamp = System.currentTimeMillis(),
                type = NotificationType.APP_UPDATE,
                targetSubPageName = "APP_UPDATE",
                isRead = false
            )
            val updatedList = listOf(updateNotif) + currentNotifs
            _uiState.value = _uiState.value.copy(notifications = updatedList)
            val classId = _uiState.value.activeClassRoom?.id ?: "default"
            CommunicationPersistenceHelper.saveNotifications(ctx, classId, updatedList)
        }
    }

    fun openGlobalUpdateDialog(info: UpdateInfo? = null, autoStartDownload: Boolean = false) {
        _uiState.value = _uiState.value.copy(
            isGlobalUpdateDialogOpen = true,
            isAutoDownloadOnOpen = autoStartDownload,
            availableUpdateInfo = info ?: _uiState.value.availableUpdateInfo
        )
    }

    fun dismissGlobalUpdateDialog() {
        _uiState.value = _uiState.value.copy(isGlobalUpdateDialogOpen = false, isAutoDownloadOnOpen = false)
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
                    syncUpdateNotification(update)
                    AppUpdateNotificationHelper.showUpdateNotification(ctx, update, forceResend = true)
                    onResult?.invoke(true)
                } else {
                    onResult?.invoke(false)
                }
            } catch (_: Exception) {
                onResult?.invoke(false)
            }
        }
    }

    fun loadScheduleFromStorage(classId: String = _uiState.value.activeClassRoom?.id ?: "default") {
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

        // Firestore üzerinden anlık canlı ders programı senkronizasyonu
        scheduleListenerRegistration?.remove()
        scheduleListenerRegistration = SchedulePersistenceHelper.observeFirestoreSchedule(ctx, classId) { cloudConfig, cloudLessons ->
            if (cloudConfig != null || cloudLessons != null) {
                _uiState.value = _uiState.value.copy(
                    scheduleConfig = cloudConfig ?: _uiState.value.scheduleConfig,
                    scheduleLessons = cloudLessons ?: _uiState.value.scheduleLessons
                )
            }
        }
    }

    fun loadSeatingPlanFromStorage(classId: String = _uiState.value.activeClassRoom?.id ?: "default") {
        val ctx = appContext ?: return
        val savedPlan = SeatingPlanPersistenceHelper.loadActivePlan(ctx, classId)
        val savedConfig = SeatingPlanPersistenceHelper.loadSeatingConfig(ctx, classId)
        if (savedPlan != null) {
            val finalPlan = if (savedConfig != null) savedPlan.copy(config = savedConfig) else savedPlan
            _uiState.value = _uiState.value.copy(seatingPlanState = finalPlan)
        } else if (savedConfig != null) {
            _uiState.value = _uiState.value.copy(
                seatingPlanState = _uiState.value.seatingPlanState.copy(config = savedConfig)
            )
        }

        seatingPlanListenerRegistration?.remove()
        seatingPlanListenerRegistration = SeatingPlanPersistenceHelper.observeActivePlanFromFirestore(ctx, classId) { cloudPlan ->
            val finalCloudPlan = if (savedConfig != null) cloudPlan.copy(config = savedConfig) else cloudPlan
            _uiState.value = _uiState.value.copy(seatingPlanState = finalCloudPlan)
        }
    }

    fun loadGroupMakerFromStorage(classId: String = _uiState.value.activeClassRoom?.id ?: "default") {
        val ctx = appContext ?: return
        val saved = GroupMakerPersistenceHelper.loadState(ctx, classId)
        if (saved != null && saved.groups.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(groupMakerState = saved)
        }
    }

    private fun persistGroupMakerState(state: GroupMakerState) {
        val ctx = appContext ?: return
        val classId = _uiState.value.activeClassRoom?.id ?: "default"
        GroupMakerPersistenceHelper.saveState(ctx, classId, state)
    }

    fun loadCommunicationFromStorage(classId: String = _uiState.value.activeClassRoom?.id ?: "default") {
        val ctx = appContext ?: return
        val currentStudents = repository.students.value
        val loadedAnnouncements = CommunicationPersistenceHelper.loadAnnouncements(ctx, classId, currentStudents)
        val loadedMessages = CommunicationPersistenceHelper.loadChatMessages(ctx, classId, currentStudents)
        val loadedNotifications = CommunicationPersistenceHelper.loadNotifications(ctx, classId)
        _uiState.value = _uiState.value.copy(
            announcements = loadedAnnouncements,
            chatMessages = loadedMessages,
            notifications = loadedNotifications
        )

        // Firestore üzerinden anlık canlı duyuru ve anket senkronizasyonu
        communicationAnnouncementsListener?.remove()
        communicationAnnouncementsListener = CommunicationPersistenceHelper.observeFirestoreAnnouncements(ctx, classId) { cloudAnnouncements ->
            _uiState.value = _uiState.value.copy(announcements = cloudAnnouncements)
        }

        // Firestore üzerinden anlık iki yönlü öğretmen-veli canlı mesajlaşma senkronizasyonu
        communicationMessagesListener?.remove()
        communicationMessagesListener = CommunicationPersistenceHelper.observeFirestoreChatMessages(ctx, classId) { cloudMessages ->
            _uiState.value = _uiState.value.copy(chatMessages = cloudMessages)
        }
    }

    fun saveAnnouncement(announcement: Announcement) {
        val classId = _uiState.value.activeClassRoom?.id ?: "default"
        val updatedList = listOf(announcement) + _uiState.value.announcements.filter { it.id != announcement.id }
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
        val updatedNotifs = listOf(newNotif) + _uiState.value.notifications
        _uiState.value = _uiState.value.copy(
            announcements = updatedList,
            notifications = updatedNotifs,
            snackbarMessage = "Duyuru başarıyla yayınlandı 📢"
        )
        appContext?.let {
            CommunicationPersistenceHelper.saveAnnouncements(it, classId, updatedList)
            CommunicationPersistenceHelper.saveNotifications(it, classId, updatedNotifs)
        }
    }

    fun markAnnouncementReadByParent(announcementId: String, parentName: String) {
        val classId = _uiState.value.activeClassRoom?.id ?: "default"
        val resolvedName = parentName.ifBlank { "Veli" }
        val updatedList = _uiState.value.announcements.map { ann ->
            if (ann.id == announcementId && !ann.readReceipts.containsKey(resolvedName)) {
                val updatedReceipts = ann.readReceipts + (resolvedName to System.currentTimeMillis())
                ann.copy(readReceipts = updatedReceipts)
            } else ann
        }
        _uiState.value = _uiState.value.copy(announcements = updatedList)
        appContext?.let { CommunicationPersistenceHelper.saveAnnouncements(it, classId, updatedList) }
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
        val classId = _uiState.value.activeClassRoom?.id ?: "default"
        val currentStudents = repository.students.value
        val deliveredParents = if (targetType == AnnouncementTargetType.ALL_CLASS) {
            currentStudents.map { it.parentName ?: "${it.fullName} Velisi" }
        } else {
            currentStudents.filter { targetStudentIds.contains(it.id) }
                .map { it.parentName ?: "${it.fullName} Velisi" }
        }

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
                )
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
        val updatedNotifs = listOf(newNotif) + _uiState.value.notifications

        _uiState.value = _uiState.value.copy(
            announcements = updatedList,
            notifications = updatedNotifs,
            snackbarMessage = "Duyuru güncellendi. Veli okuma durumu sıfırlandı."
        )
        appContext?.let {
            CommunicationPersistenceHelper.saveAnnouncements(it, classId, updatedList)
            CommunicationPersistenceHelper.saveNotifications(it, classId, updatedNotifs)
        }
    }

    fun deleteAnnouncement(id: String) {
        val classId = _uiState.value.activeClassRoom?.id ?: "default"
        val updatedList = _uiState.value.announcements.filter { it.id != id }
        _uiState.value = _uiState.value.copy(
            announcements = updatedList,
            snackbarMessage = "Duyuru silindi."
        )
        appContext?.let {
            CommunicationPersistenceHelper.saveAnnouncements(it, classId, updatedList)
            CommunicationPersistenceHelper.deleteAnnouncementFromFirestore(it, classId, id)
        }
    }

    fun votePollOption(announcementId: String, optionId: String, voterName: String) {
        val classId = _uiState.value.activeClassRoom?.id ?: "default"
        val updatedList = _uiState.value.announcements.map { ann ->
            if (ann.id == announcementId && ann.poll != null) {
                val updatedOptions = ann.poll.options.map { opt ->
                    if (opt.id == optionId) {
                        if (!opt.voterParentNames.contains(voterName)) {
                            opt.copy(voterParentNames = opt.voterParentNames + voterName)
                        } else opt
                    } else {
                        opt.copy(voterParentNames = opt.voterParentNames.filter { it != voterName })
                    }
                }
                ann.copy(poll = ann.poll.copy(options = updatedOptions))
            } else ann
        }
        _uiState.value = _uiState.value.copy(announcements = updatedList)
        appContext?.let { CommunicationPersistenceHelper.saveAnnouncements(it, classId, updatedList) }
    }

    fun sendChatMessage(
        studentId: String,
        text: String,
        isFromTeacher: Boolean = true,
        parentEmail: String = "",
        parentName: String = ""
    ) {
        val classId = _uiState.value.activeClassRoom?.id ?: "default"
        val student = repository.students.value.find { it.id == studentId }
        val studentNo = student?.studentNumber ?: ""
        val studentName = student?.fullName ?: "Öğrenci"

        val targetParent = student?.allLinkedParents?.find { it.email.equals(parentEmail, ignoreCase = true) }
            ?: student?.allLinkedParents?.firstOrNull()

        val resolvedEmail = if (parentEmail.isNotBlank()) parentEmail else (targetParent?.email ?: "")
        val resolvedParentName = if (parentName.isNotBlank()) parentName else (targetParent?.name ?: (student?.parentName ?: "${studentName.split(" ").first()} Velisi"))

        val newMsg = ChatMessage(
            id = "msg_${System.currentTimeMillis()}",
            classId = classId,
            studentId = studentId,
            studentNumber = studentNo,
            studentName = studentName,
            parentName = resolvedParentName,
            parentEmail = resolvedEmail,
            isFromTeacher = isFromTeacher,
            senderName = if (isFromTeacher) _uiState.value.activeClassRoom?.teacherName ?: "Cihan Özel" else resolvedParentName,
            text = text,
            timestamp = System.currentTimeMillis(),
            status = if (isFromTeacher) MessageStatus.READ else MessageStatus.DELIVERED
        )

        val updatedMessages = _uiState.value.chatMessages + newMsg
        val notif = if (!isFromTeacher) {
            BirizNotification(
                id = "notif_${System.currentTimeMillis()}",
                title = "Yeni Veli Mesajı 💬",
                message = "$resolvedParentName ($studentNo $studentName): $text",
                timestamp = System.currentTimeMillis(),
                type = NotificationType.MESSAGE,
                targetSubPageName = "ILETISIM_MESAJLAR",
                targetId = studentId,
                isRead = false
            )
        } else {
            BirizNotification(
                id = "notif_${System.currentTimeMillis()}",
                title = "Öğretmenden Yeni Mesaj 💬",
                message = "${_uiState.value.activeClassRoom?.teacherName ?: "Öğretmen"}: $text",
                timestamp = System.currentTimeMillis(),
                type = NotificationType.MESSAGE,
                targetSubPageName = "ILETISIM",
                targetId = studentId,
                isRead = false
            )
        }
        val updatedNotifs = listOf(notif) + _uiState.value.notifications
        _uiState.value = _uiState.value.copy(
            chatMessages = updatedMessages,
            notifications = updatedNotifs
        )
        appContext?.let {
            CommunicationPersistenceHelper.saveChatMessages(it, classId, updatedMessages)
            CommunicationPersistenceHelper.saveNotifications(it, classId, updatedNotifs)
        }
    }

    fun simulateParentReply(
        studentId: String,
        replyText: String,
        parentEmail: String = "",
        parentName: String = ""
    ) {
        sendChatMessage(studentId, replyText, isFromTeacher = false, parentEmail = parentEmail, parentName = parentName)
        val student = repository.students.value.find { it.id == studentId }
        val targetParent = student?.allLinkedParents?.find { it.email.equals(parentEmail, ignoreCase = true) }
        val resolvedParentName = if (parentName.isNotBlank()) parentName else (targetParent?.name ?: "Veli")

        val notif = BirizNotification(
            id = "notif_${System.currentTimeMillis()}",
            title = "Yeni Veli Mesajı 💬",
            message = "$resolvedParentName (${student?.studentNumber} ${student?.fullName}): $replyText",
            timestamp = System.currentTimeMillis(),
            type = NotificationType.MESSAGE,
            targetSubPageName = "ILETISIM_MESAJLAR",
            targetId = studentId,
            isRead = false
        )
        val updatedNotifs = listOf(notif) + _uiState.value.notifications
        _uiState.value = _uiState.value.copy(notifications = updatedNotifs)
        appContext?.let {
            CommunicationPersistenceHelper.saveNotifications(it, _uiState.value.activeClassRoom?.id ?: "default", updatedNotifs)
        }
    }

    fun markChatMessagesAsRead(studentId: String, parentEmail: String = "") {
        val classId = _uiState.value.activeClassRoom?.id ?: "default"
        val updatedMessages = _uiState.value.chatMessages.map { msg ->
            val matchesStudent = msg.studentId == studentId
            val matchesParent = parentEmail.isBlank() || msg.parentEmail.isBlank() || msg.parentEmail.equals(parentEmail, ignoreCase = true)
            if (matchesStudent && matchesParent && !msg.isFromTeacher && msg.status != MessageStatus.READ) {
                msg.copy(status = MessageStatus.READ)
            } else msg
        }
        _uiState.value = _uiState.value.copy(chatMessages = updatedMessages)
        appContext?.let {
            CommunicationPersistenceHelper.saveChatMessages(it, classId, updatedMessages)
            CommunicationPersistenceHelper.markChatMessagesReadInFirestore(it, classId, updatedMessages)
        }
    }

    fun markNotificationAsRead(notificationId: String) {
        val classId = _uiState.value.activeClassRoom?.id ?: "default"
        val updatedNotifs = _uiState.value.notifications.filterNot { it.id == notificationId }
        _uiState.value = _uiState.value.copy(notifications = updatedNotifs)
        appContext?.let { CommunicationPersistenceHelper.saveNotifications(it, classId, updatedNotifs) }
    }

    fun markAllNotificationsAsRead() {
        val classId = _uiState.value.activeClassRoom?.id ?: "default"
        _uiState.value = _uiState.value.copy(notifications = emptyList())
        appContext?.let { CommunicationPersistenceHelper.saveNotifications(it, classId, emptyList()) }
    }

    fun setNotificationCenterOpen(isOpen: Boolean) {
        _uiState.value = _uiState.value.copy(isNotificationCenterOpen = isOpen)
    }

    fun setActiveChatStudent(studentId: String?) {
        _uiState.value = _uiState.value.copy(activeChatStudentId = studentId)
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
            currentSubPage = DashboardSubPage.SINIF_LISTESI,
            snackbarMessage = "Önizleme Test Modu: 4/D Sınıfı (25 Öğrenci) hazır yüklendi."
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

                if (resolvedClass != null) {
                    repository.observeStudents(resolvedClass.id)
                    loadScheduleFromStorage(resolvedClass.id)
                    loadSeatingPlanFromStorage(resolvedClass.id)
                    loadCommunicationFromStorage(resolvedClass.id)
                    loadGroupMakerFromStorage(resolvedClass.id)

                    _uiState.value = _uiState.value.copy(
                        activeClassRoom = resolvedClass,
                        currentRole = UserRole.TEACHER,
                        isKvkkAccepted = true,
                        currentScreen = ScreenState.TEACHER_DASHBOARD,
                        currentSubPage = DashboardSubPage.SINIF_LISTESI
                    )
                    saveParentSessionState()
                    return@launch
                }

                // 2. Veli Rolü veya Öğrenci Bağlantısı Kontrolü
                // Veli her girişinde (mailine tıkladığında) daha önce veli olarak tanımlandıysa
                // veya sisteme kayıtlı öğrencisi varsa ASLA rol seçme ekranı (ROLE_AND_KVKK) gösterilmez,
                // doğrudan Veli Portalı ana ekranına (PARENT_PORTAL) yönlendirilir.
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
                    val combinedLinkedStudentIds = (_uiState.value.linkedParentStudentIds + savedLinkedIds + parentLinkedMatches.map { it.first.id })
                        .distinct()
                    val activeStudentId = _uiState.value.activeParentStudentId
                        ?: combinedLinkedStudentIds.firstOrNull()
                    val parentClass = parentLinkedMatches.firstOrNull()?.second
                        ?: _uiState.value.activeClassRoom
                        ?: repository.defaultSampleClass

                    if (_uiState.value.activeClassRoom == null || _uiState.value.activeClassRoom?.id != parentClass.id) {
                        _uiState.value = _uiState.value.copy(activeClassRoom = parentClass)
                    }

                    repository.observeStudents(parentClass.id)
                    loadScheduleFromStorage(parentClass.id)
                    loadSeatingPlanFromStorage(parentClass.id)
                    loadCommunicationFromStorage(parentClass.id)

                    if (combinedLinkedStudentIds.isNotEmpty()) {
                        repository.syncLinkedStudentsWithParent(
                            linkedStudentIds = combinedLinkedStudentIds,
                            parentEmail = userEmail,
                            parentName = name
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
                    saveParentSessionState()
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
                currentSubPage = DashboardSubPage.SINIF_LISTESI
            )
            return
        }
        _uiState.value = _uiState.value.copy(
            currentRole = UserRole.TEACHER,
            currentScreen = ScreenState.TEACHER_SETUP
        )
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
        loadCommunicationFromStorage(targetClass.id)

        _uiState.value = _uiState.value.copy(
            currentRole = UserRole.PARENT,
            currentScreen = ScreenState.PARENT_PORTAL,
            snackbarMessage = "BİRİZ Veli Modülüne Geçiş Yapıldı 👨‍👩‍👧‍👦"
        )
        saveParentSessionState()
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
        saveParentSessionState()
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
                saveParentSessionState()
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
        repository.observeStudents(targetClass.id)
        loadScheduleFromStorage(targetClass.id)
        loadSeatingPlanFromStorage(targetClass.id)
        loadCommunicationFromStorage(targetClass.id)
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

                _uiState.value = _uiState.value.copy(
                    activeClassRoom = createdClass,
                    currentScreen = ScreenState.SETUP_SUCCESS,
                    currentSubPage = DashboardSubPage.SINIF_LISTESI,
                    conflictError = null,
                    occupiedClassRoom = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    conflictError = e.message,
                    snackbarMessage = e.message ?: "Kayıt sırasında hata oluştu"
                )
            }
        }
    }

    fun navigateToTeacherDashboard() {
        val classRoom = _uiState.value.activeClassRoom
        if (classRoom != null) {
            repository.observeStudents(classRoom.id)
            loadScheduleFromStorage(classRoom.id)
            loadSeatingPlanFromStorage(classRoom.id)
            loadGroupMakerFromStorage(classRoom.id)
        } else {
            loadScheduleFromStorage("default")
            loadSeatingPlanFromStorage("default")
            loadGroupMakerFromStorage("default")
        }
        _uiState.value = _uiState.value.copy(
            currentScreen = ScreenState.TEACHER_DASHBOARD,
            currentSubPage = DashboardSubPage.SINIF_LISTESI
        )
    }

    fun selectSubPage(subPage: DashboardSubPage) {
        if (subPage == DashboardSubPage.DERS_PROGRAMI) {
            loadScheduleFromStorage(_uiState.value.activeClassRoom?.id ?: "default")
        }
        if (subPage == DashboardSubPage.OTURMA_PLANI) {
            loadSeatingPlanFromStorage(_uiState.value.activeClassRoom?.id ?: "default")
        }

        if (subPage == DashboardSubPage.SINIF_LISTESI || subPage == DashboardSubPage.PROFIL_AYARLARI || subPage.category == "ÖĞRETMEN ODASI") {
            _uiState.value = _uiState.value.copy(currentSubPage = subPage)
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

        _uiState.value = _uiState.value.copy(currentSubPage = subPage)

        // Akıllı Tahta Canlı Yansıtma Senkronizasyonu
        triggerSmartBoardSync(subPage = subPage)
    }

    // --- AKILLI TAHTA KUMANDASI VE CANLI YANSITMA METODLARI ---

    fun triggerSmartBoardSync(
        subPage: DashboardSubPage = _uiState.value.currentSubPage,
        day: String = _uiState.value.activeScheduleDay,
        isMirrorActive: Boolean = _uiState.value.isLiveBoardMirrorActive
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
            seatingPlanState = _uiState.value.seatingPlanState,
            groupMakerState = _uiState.value.groupMakerState
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
            SmartBoardHelper.disconnectSession(ctx, code, activeClass.id)
        }
        _uiState.value = _uiState.value.copy(
            connectedBoardSessionCode = null,
            isLiveBoardMirrorActive = false,
            isSmartBoardDialogOpen = false,
            smartBoardMessage = null,
            snackbarMessage = "Akıllı Tahta Bağlantısı Kesildi 🔌"
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
        val classId = _uiState.value.activeClassRoom?.id ?: "default"
        appContext?.let { ctx ->
            SchedulePersistenceHelper.saveScheduleConfig(ctx, classId, configured)
        }
        _uiState.value = _uiState.value.copy(
            scheduleConfig = configured,
            isScheduleSettingsStepActive = false,
            snackbarMessage = "Ders programı zaman çizelgesi ve teneffüs ayarları güncellendi."
        )
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

        val classId = _uiState.value.activeClassRoom?.id ?: "default"
        appContext?.let { ctx ->
            SchedulePersistenceHelper.saveScheduleLessons(ctx, classId, currentLessons)
        }

        _uiState.value = _uiState.value.copy(
            scheduleLessons = currentLessons,
            snackbarMessage = "${normalizedLesson.lessonIndex}. Ders ($upperSubjectName) ve aynı adlı tüm derslerin rengi eşitlendi."
        )
    }

    fun setScheduleSettingsStepActive(active: Boolean) {
        _uiState.value = _uiState.value.copy(isScheduleSettingsStepActive = active)
    }

    fun setActiveScheduleDay(day: String) {
        val classId = _uiState.value.activeClassRoom?.id ?: "default"
        appContext?.let { ctx ->
            SchedulePersistenceHelper.saveActiveDay(ctx, classId, day)
        }
        _uiState.value = _uiState.value.copy(activeScheduleDay = day)

        // Akıllı Tahta Canlı Gün Güncellemesi
        triggerSmartBoardSync(day = day)
    }

    // --- AKILLI SINIF OTURMA PLANI METODLARI ---

    private fun persistActiveSeatingPlan(state: SeatingPlanState) {
        val classId = _uiState.value.activeClassRoom?.id ?: "default"
        appContext?.let { ctx ->
            SeatingPlanPersistenceHelper.saveActivePlan(ctx, classId, state)
        }
    }

    fun updateSeatingConfig(newConfig: SeatingConfig) {
        val currentPlan = _uiState.value.seatingPlanState
        val validSeatIds = SeatingPlanHelper.generateAllSeatIdsFrontToBack(newConfig).toSet()
        val filteredAssignments = currentPlan.assignments.filterKeys { validSeatIds.contains(it) }

        val updated = currentPlan.copy(
            config = newConfig,
            assignments = filteredAssignments
        )
        val classId = _uiState.value.activeClassRoom?.id ?: "default"
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
        val currentPlan = _uiState.value.seatingPlanState
        val newAssignments = SeatingPlanHelper.assignStudent(currentPlan.assignments, seatId, studentId)
        val updated = currentPlan.copy(assignments = newAssignments)
        persistActiveSeatingPlan(updated)
        _uiState.value = _uiState.value.copy(seatingPlanState = updated)
    }

    fun swapSeats(seatId1: String, seatId2: String) {
        val currentPlan = _uiState.value.seatingPlanState
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
        val currentPlan = _uiState.value.seatingPlanState
        val newAssignments = if (clearFixed) {
            emptyMap()
        } else {
            currentPlan.assignments.filter { (_, studentId) ->
                currentPlan.pinnedStudents.contains(studentId)
            }
        }
        val updated = currentPlan.copy(assignments = newAssignments)
        persistActiveSeatingPlan(updated)
        _uiState.value = _uiState.value.copy(
            seatingPlanState = updated,
            snackbarMessage = if (clearFixed) "Tüm koltuklar boşaltıldı." else "Sabitlenenler hariç tüm koltuklar boşaltıldı."
        )
    }

    fun ensureSeatingPlanInitialized() {
        val currentPlan = _uiState.value.seatingPlanState
        val currentStudents = repository.students.value
        val classId = _uiState.value.activeClassRoom?.id ?: "default"
        val saved = appContext?.let { SeatingPlanPersistenceHelper.loadActivePlan(it, classId) }
        val savedConfig = appContext?.let { SeatingPlanPersistenceHelper.loadSeatingConfig(it, classId) }
        val effectiveConfig = savedConfig ?: saved?.config ?: currentPlan.config

        if (saved != null) {
            if (currentPlan.assignments.isEmpty() && currentStudents.isNotEmpty()) {
                if (saved.assignments.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(seatingPlanState = saved.copy(config = effectiveConfig))
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
            } else if (currentPlan.config != effectiveConfig) {
                _uiState.value = _uiState.value.copy(
                    seatingPlanState = currentPlan.copy(config = effectiveConfig)
                )
            }
        } else if (currentPlan.assignments.isEmpty() && currentStudents.isNotEmpty()) {
            val initial = SeatingPlanHelper.generateInitialAssignments(currentStudents, effectiveConfig)
            val initialPlan = currentPlan.copy(
                config = effectiveConfig,
                assignments = initial,
                activePlanName = "1. Başlangıç Planı"
            )
            persistActiveSeatingPlan(initialPlan)
            _uiState.value = _uiState.value.copy(seatingPlanState = initialPlan)
        } else if (currentPlan.config != effectiveConfig) {
            _uiState.value = _uiState.value.copy(
                seatingPlanState = currentPlan.copy(config = effectiveConfig)
            )
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
            snackbarMessage = "Tüm gruplar için rastgele birer sözcü/lider seçildi 👑"
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
            snackbarMessage = if (isNowLeader) "${student?.fullName ?: "Öğrenci"} grup sözcüsü yapıldı 👑"
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
        disconnectSmartBoard()
        Firebase.auth.signOut()
        repository.disableTestMode()
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
        communicationAnnouncementsListener?.remove()
        communicationMessagesListener?.remove()
        seatingPlanListenerRegistration?.remove()
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
