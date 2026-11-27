package com.example

import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.data.repository.ClassRegistryRepository
import com.example.ui.components.AppUpdateDialog
import com.example.ui.screens.GoogleSignInScreen
import com.example.ui.screens.ParentPortalSubScreen
import com.example.ui.screens.ParentPortalTab
import com.example.ui.screens.RoleSelectionScreen
import com.example.ui.screens.SetupSuccessScreen
import com.example.ui.screens.TeacherDashboardScreen
import com.example.ui.screens.TeacherSetupWizardScreen
import com.example.ui.theme.BirizNavyPrimary
import com.example.ui.theme.BirizTheme
import androidx.compose.material3.LinearProgressIndicator
import com.example.ui.viewmodel.BirizViewModel
import com.example.ui.viewmodel.BirizViewModelFactory
import com.example.ui.viewmodel.ScreenState
import com.example.util.AppUpdateDownloadController
import com.example.util.AppUpdateManager
import com.example.util.AppUpdateNotificationHelper
import com.example.util.BackgroundUpdateState
import com.example.util.ChatNotificationHelper
import com.example.util.SystemActivityNotificationHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import java.io.File

class MainActivity : ComponentActivity() {

    private val viewModel: BirizViewModel by viewModels {
        BirizViewModelFactory(ClassRegistryRepository(applicationContext), applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()

        // Android 8.0+ bildirim kanallarını açılışta garantiye al
        AppUpdateNotificationHelper.createNotificationChannel(applicationContext)
        ChatNotificationHelper.createNotificationChannel(applicationContext)
        SystemActivityNotificationHelper.createNotificationChannels(applicationContext)

        // Cihaz hafızasında eski APK kalıntılarının birikmesini önlemek için temizle
        com.example.util.AppUpdateManager.cleanOldApkFiles(applicationContext)

        // Bildirim çubuğundan tıklandıysa güncelleme veya sohbet ekranını doğrudan aç
        handleNotificationIntents(intent)

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            BirizTheme(darkTheme = uiState.isDarkTheme) {
                BirizApp(viewModel = viewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        AppUpdateDownloadController.isAppInForeground = true
        viewModel.onAppForegrounded()
    }

    override fun onResume() {
        super.onResume()
        AppUpdateDownloadController.isAppInForeground = true
        viewModel.onAppForegrounded()
        viewModel.triggerLiveUpdateCheck()
        AppUpdateDownloadController.restoreCachedUpdateIfReady(
            applicationContext,
            viewModel.uiState.value.availableUpdateInfo
        )
        val readyAfterResume = AppUpdateDownloadController.state.value as? BackgroundUpdateState.ReadyToInstall
        if (readyAfterResume != null &&
            AppUpdateDownloadController.lastAutoPromptedVersionCode != readyAfterResume.info.latestVersionCode &&
            AppUpdateManager.canInstallPackages(this)
        ) {
            AppUpdateDownloadController.lastAutoPromptedVersionCode = readyAfterResume.info.latestVersionCode
            AppUpdateNotificationHelper.cancelUpdateNotification(applicationContext)
            AppUpdateDownloadController.markInstalling(readyAfterResume.info, readyAfterResume.apkFile)
            AppUpdateManager.installApk(this, readyAfterResume.apkFile, readyAfterResume.info.latestVersionCode)
        }
    }

    override fun onPause() {
        viewModel.onAppBackgrounded()
        super.onPause()
    }

    override fun onStop() {
        AppUpdateDownloadController.isAppInForeground = false
        viewModel.onAppBackgrounded()
        super.onStop()
    }

    override fun onDestroy() {
        viewModel.onAppBackgrounded()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntents(intent)
    }

    private fun handleNotificationIntents(intent: Intent?) {
        if (intent == null) return
        if (intent.getBooleanExtra(AppUpdateNotificationHelper.EXTRA_INSTALL_READY_APK, false)) {
            val pathFromIntent = intent.getStringExtra(AppUpdateNotificationHelper.EXTRA_INSTALL_APK_PATH)
            val readyFile = pathFromIntent?.let { File(it) }?.takeIf { it.exists() }
                ?: (AppUpdateDownloadController.state.value as? BackgroundUpdateState.ReadyToInstall)?.apkFile?.takeIf { it.exists() }
                ?: viewModel.uiState.value.availableUpdateInfo?.let {
                    AppUpdateManager.getDownloadedApkForVersion(applicationContext, it.latestVersionCode)
                }
            if (readyFile != null && readyFile.exists()) {
                AppUpdateNotificationHelper.cancelUpdateNotification(applicationContext)
                val availableInfo = viewModel.uiState.value.availableUpdateInfo
                val targetCode = availableInfo?.latestVersionCode ?: -1
                if (availableInfo != null && AppUpdateManager.canInstallPackages(this)) {
                    AppUpdateDownloadController.lastAutoPromptedVersionCode = targetCode
                    AppUpdateDownloadController.markInstalling(availableInfo, readyFile)
                }
                AppUpdateManager.installApk(this, readyFile, targetCode)
            } else {
                viewModel.startDirectUpdate(this)
            }
            intent.removeExtra(AppUpdateNotificationHelper.EXTRA_INSTALL_READY_APK)
            intent.removeExtra(AppUpdateNotificationHelper.EXTRA_INSTALL_APK_PATH)
        } else if (intent.getBooleanExtra(AppUpdateNotificationHelper.EXTRA_DIRECT_INSTALL_UPDATE, false)) {
            AppUpdateNotificationHelper.cancelUpdateNotification(applicationContext)
            viewModel.startDirectUpdate(this)
            intent.removeExtra(AppUpdateNotificationHelper.EXTRA_DIRECT_INSTALL_UPDATE)
        } else if (intent.getBooleanExtra(AppUpdateNotificationHelper.EXTRA_OPEN_UPDATE_DIALOG, false)) {
            viewModel.openGlobalUpdateDialog()
            intent.removeExtra(AppUpdateNotificationHelper.EXTRA_OPEN_UPDATE_DIALOG)
        }
        val chatStudentId = intent.getStringExtra(ChatNotificationHelper.EXTRA_OPEN_CHAT_STUDENT_ID)
        if (!chatStudentId.isNullOrBlank()) {
            val isTeacherTarget = if (intent.hasExtra(ChatNotificationHelper.EXTRA_OPEN_CHAT_IS_TEACHER)) {
                intent.getBooleanExtra(ChatNotificationHelper.EXTRA_OPEN_CHAT_IS_TEACHER, false)
            } else null
            viewModel.openChatFromNotification(chatStudentId, isTeacherTarget)
            intent.removeExtra(ChatNotificationHelper.EXTRA_OPEN_CHAT_STUDENT_ID)
            intent.removeExtra(ChatNotificationHelper.EXTRA_OPEN_CHAT_IS_TEACHER)
        }
        val sysNotifType = intent.getStringExtra(SystemActivityNotificationHelper.EXTRA_OPEN_NOTIF_TYPE)
        if (!sysNotifType.isNullOrBlank()) {
            val sysNotifId = intent.getStringExtra(SystemActivityNotificationHelper.EXTRA_OPEN_NOTIF_ID)
            val sysSubPage = intent.getStringExtra(SystemActivityNotificationHelper.EXTRA_OPEN_NOTIF_SUBPAGE)
            val sysTargetId = intent.getStringExtra(SystemActivityNotificationHelper.EXTRA_OPEN_NOTIF_TARGET_ID)
            val sysIsTeacher = if (intent.hasExtra(SystemActivityNotificationHelper.EXTRA_OPEN_NOTIF_IS_TEACHER)) {
                intent.getBooleanExtra(SystemActivityNotificationHelper.EXTRA_OPEN_NOTIF_IS_TEACHER, false)
            } else null
            viewModel.openNotificationFromSystemBar(
                notifId = sysNotifId,
                typeName = sysNotifType,
                targetSubPageName = sysSubPage,
                targetId = sysTargetId,
                isTeacherTarget = sysIsTeacher
            )
            intent.removeExtra(SystemActivityNotificationHelper.EXTRA_OPEN_NOTIF_ID)
            intent.removeExtra(SystemActivityNotificationHelper.EXTRA_OPEN_NOTIF_TYPE)
            intent.removeExtra(SystemActivityNotificationHelper.EXTRA_OPEN_NOTIF_SUBPAGE)
            intent.removeExtra(SystemActivityNotificationHelper.EXTRA_OPEN_NOTIF_TARGET_ID)
            intent.removeExtra(SystemActivityNotificationHelper.EXTRA_OPEN_NOTIF_IS_TEACHER)
        }
        if (intent.getBooleanExtra(SystemActivityNotificationHelper.EXTRA_OPEN_NOTIFICATION_CENTER, false)) {
            viewModel.setNotificationCenterOpen(true)
            intent.removeExtra(SystemActivityNotificationHelper.EXTRA_OPEN_NOTIFICATION_CENTER)
        }
    }
}

@Composable
fun BirizApp(viewModel: BirizViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val matchingSchools by viewModel.matchingSchools.collectAsStateWithLifecycle()
    val branchStatuses by viewModel.branchStatuses.collectAsStateWithLifecycle()
    val bgUpdateState by AppUpdateDownloadController.state.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.availableUpdateInfo) {
        AppUpdateDownloadController.restoreCachedUpdateIfReady(context, uiState.availableUpdateInfo)
    }

    // Android 13+ (API 33) için Bildirim İzni İsteme (Güncelleme bildirimlerinin çubuğa düşmesi için)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                uiState.availableUpdateInfo?.let { update ->
                    AppUpdateNotificationHelper.showUpdateNotification(context, update)
                }
            }
        }
        LaunchedEffect(Unit) {
            val currentPerm = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
            if (currentPerm != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { _ ->
            viewModel.refreshCurrentUser()
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    // Geri tuşu yönetimi (BackHandler)
    val canGoBack = uiState.currentScreen != ScreenState.GOOGLE_SIGN_IN &&
                    uiState.currentScreen != ScreenState.ROLE_AND_KVKK &&
                    !(uiState.currentScreen == ScreenState.TEACHER_DASHBOARD && uiState.currentSubPage == com.example.ui.viewmodel.DashboardSubPage.ANA_SAYFA) &&
                    uiState.currentScreen != ScreenState.PARENT_PORTAL
    BackHandler(enabled = canGoBack) {
        when (uiState.currentScreen) {
            ScreenState.TEACHER_SETUP -> viewModel.resetToRoleSelection()
            ScreenState.SETUP_SUCCESS -> viewModel.navigateToTeacherDashboard()
            ScreenState.TEACHER_DASHBOARD -> {
                if (uiState.currentSubPage != com.example.ui.viewmodel.DashboardSubPage.ANA_SAYFA) {
                    viewModel.selectSubPage(com.example.ui.viewmodel.DashboardSubPage.ANA_SAYFA)
                }
            }
            else -> {}
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            when (uiState.currentScreen) {
                ScreenState.GOOGLE_SIGN_IN -> {
                    GoogleSignInScreen(
                        onSignInSuccess = {
                            viewModel.refreshCurrentUser()
                        },
                        onTestSignIn = {
                            viewModel.launchTestSignIn()
                        },
                        isDarkTheme = uiState.isDarkTheme,
                        onToggleTheme = { viewModel.toggleTheme() },
                        isAuthResolving = uiState.isAuthResolving
                    )
                }

                ScreenState.ROLE_AND_KVKK -> {
                    RoleSelectionScreen(
                        currentRole = uiState.currentRole,
                        isKvkkAccepted = uiState.isKvkkAccepted,
                        isKvkkDialogVisible = uiState.isKvkkDialogVisible,
                        onKvkkCheckedChanged = { viewModel.onKvkkCheckedChanged(it) },
                        onOpenKvkkDialog = { viewModel.openKvkkDialog() },
                        onCloseKvkkDialog = { viewModel.dismissKvkkDialog() },
                        onSelectTeacher = { viewModel.selectTeacherRole() },
                        onSelectParentAttempt = { viewModel.onParentRoleAttempt() },
                        currentUser = uiState.currentUser,
                        onLogout = { viewModel.signOut(context) },
                        onDeleteEntireAccount = { viewModel.deleteEntireAccount(context) },
                        isDarkTheme = uiState.isDarkTheme,
                        onToggleTheme = { viewModel.toggleTheme() }
                    )
                }

                ScreenState.TEACHER_SETUP -> {
                    TeacherSetupWizardScreen(
                        cities = viewModel.cities,
                        selectedCity = uiState.selectedCity,
                        selectedDistrict = uiState.selectedDistrict,
                        rawSchoolInput = uiState.rawSchoolInput,
                        selectedSchool = uiState.selectedSchool,
                        matchingSchools = matchingSchools,
                        selectedGradeLevel = uiState.selectedGradeLevel,
                        selectedSection = uiState.selectedSection,
                        branchStatuses = branchStatuses,
                        currentUser = uiState.currentUser,
                        conflictError = uiState.conflictError,
                        occupiedClassRoom = uiState.occupiedClassRoom,
                        onCitySelected = { viewModel.onCitySelected(it) },
                        onDistrictSelected = { viewModel.onDistrictSelected(it) },
                        onSchoolInputChanged = { viewModel.onSchoolInputChanged(it) },
                        onSchoolSelectedFromList = { viewModel.onSchoolSelected(it) },
                        onAddNewSchool = { viewModel.onSchoolInputChanged(it) },
                        onGradeLevelSelected = { viewModel.onGradeLevelSelected(it) },
                        onSectionSelected = { viewModel.onSectionSelected(it) },
                        onSubmit = { viewModel.submitTeacherRegistration() },
                        onBackToRoles = { viewModel.resetToRoleSelection() },
                        isDarkTheme = uiState.isDarkTheme,
                        onToggleTheme = { viewModel.toggleTheme() }
                    )
                }

                ScreenState.SETUP_SUCCESS -> {
                    val classRoom = uiState.activeClassRoom
                    if (classRoom != null) {
                        SetupSuccessScreen(
                            classRoom = classRoom,
                            onNavigateToDashboard = { viewModel.navigateToTeacherDashboard() },
                            onTestAnotherClass = { viewModel.testAnotherClass() },
                            isDarkTheme = uiState.isDarkTheme,
                            onToggleTheme = { viewModel.toggleTheme() }
                        )
                    } else {
                        viewModel.resetToRoleSelection()
                    }
                }

                ScreenState.TEACHER_DASHBOARD -> {
                    val classRoom = uiState.activeClassRoom
                    val students by viewModel.students.collectAsStateWithLifecycle()
                    if (classRoom != null) {
                        val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                        DisposableEffect(classRoom.id) {
                            viewModel.syncLivePresenceSocket()
                            onDispose {
                                viewModel.updateTeacherPresence(isOnline = false)
                            }
                        }

                        LaunchedEffect(lifecycleOwner, classRoom.id) {
                            lifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
                                viewModel.syncLivePresenceSocket()
                                while (true) {
                                    kotlinx.coroutines.delay(75_000L)
                                    viewModel.sendTeacherPresenceHeartbeatIfNeeded()
                                }
                            }
                        }

                        TeacherDashboardScreen(
                            classRoom = classRoom,
                            students = students,
                            currentSubPage = uiState.currentSubPage,
                            isAddStudentDialogOpen = uiState.isAddStudentDialogOpen,
                            isDeleting = uiState.isDeleting,
                            isDarkTheme = uiState.isDarkTheme,
                            scheduleConfig = uiState.scheduleConfig,
                            scheduleLessons = uiState.scheduleLessons,
                            isScheduleSettingsActive = uiState.isScheduleSettingsStepActive,
                            activeScheduleDay = uiState.activeScheduleDay,
                            seatingPlanState = uiState.seatingPlanState,
                            isSeatingConfigOpen = uiState.isSeatingConfigOpen,
                            groupMakerState = uiState.groupMakerState,
                            onToggleTheme = { viewModel.toggleTheme() },
                            onSelectSubPage = { viewModel.selectSubPage(it) },
                            onOpenAddStudentDialog = { viewModel.openAddStudentDialog() },
                            onDismissAddStudentDialog = { viewModel.dismissAddStudentDialog() },
                            onAddStudent = { no, name, gender -> viewModel.addStudent(no, name, gender) },
                            onEditStudent = { id, no, name, gender -> viewModel.editStudent(id, no, name, gender) },
                            onImportStudents = { list -> viewModel.importStudents(list) },
                            onUpdateStudentParent = { id, name, email, linkedAt, lastLoginAt -> viewModel.updateStudentParent(id, name, email, linkedAt, lastLoginAt) },
                            onSaveScheduleConfig = { viewModel.updateScheduleConfig(it) },
                            onSaveScheduleLesson = { viewModel.updateScheduleLesson(it) },
                            onToggleScheduleSettings = { viewModel.setScheduleSettingsStepActive(it) },
                            onSelectScheduleDay = { viewModel.setActiveScheduleDay(it) },
                            onOpenSeatingConfig = { viewModel.setSeatingConfigDialogOpen(true) },
                            onCloseSeatingConfig = { viewModel.setSeatingConfigDialogOpen(false) },
                            onSaveSeatingConfig = { viewModel.updateSeatingConfig(it) },
                            onAutoAssignSeating = { viewModel.autoAssignSeating(it) },
                            onApplyPlannedSeating = { viewModel.applyPlannedSeating(it) },
                            onApplyArchivedPlan = { viewModel.applyArchivedPlan(it) },
                            onAssignStudentToSeat = { seatId, studentId -> viewModel.assignStudentToSeat(seatId, studentId) },
                            onSwapSeats = { s1, s2 -> viewModel.swapSeats(s1, s2) },
                            onTogglePinStudent = { viewModel.togglePinStudent(it) },
                            onTogglePriorityStudent = { viewModel.togglePriorityStudent(it) },
                            onClearSeatingPlan = { viewModel.clearSeatingPlan(it) },
                            onOpenGroupWizard = { viewModel.openGroupWizard() },
                            onCloseGroupWizard = { viewModel.closeGroupWizard() },
                            onSaveGroupConfig = { viewModel.saveGroupConfig(it) },
                            onDistributeGroups = { viewModel.distributeGroups(it) },
                            onReshuffleGroups = { viewModel.reshuffleGroups() },
                            onDistributeUnassignedGroups = { viewModel.distributeUnassignedGroups() },
                            onPickRandomLeadersForAllGroups = { viewModel.pickRandomLeadersForAllGroups() },
                            onPickRandomLeaderForGroup = { viewModel.pickRandomLeaderForSingleGroup(it) },
                            onToggleGroupLeader = { gId, sId -> viewModel.toggleStudentLeader(gId, sId) },
                            onRenameGroup = { gId, name -> viewModel.renameGroup(gId, name) },
                            onChangeGroupColor = { gId, theme -> viewModel.changeGroupColorTheme(gId, theme) },
                            onAddNewGroup = { viewModel.addNewGroup() },
                            onDeleteGroup = { viewModel.deleteGroup(it) },
                            onOpenGroupSwapModal = { viewModel.openSwapModal(it) },
                            onCloseGroupSwapModal = { viewModel.closeSwapModal() },
                            onSwapGroupStudents = { s1, s2 -> viewModel.swapStudents(s1, s2) },
                            onMoveGroupStudent = { sId, gId -> viewModel.moveStudentToGroup(sId, gId) },
                            onRemoveGroupStudent = { viewModel.removeStudentFromGroup(it) },
                            onToggleGroupPresenterMode = { viewModel.togglePresenterMode() },
                            announcements = uiState.announcements,
                            chatMessages = uiState.chatMessages,
                            notifications = uiState.notifications,
                            activeChatStudentId = uiState.activeChatStudentId,
                            isNotificationCenterOpen = uiState.isNotificationCenterOpen,
                            onSaveAnnouncement = { viewModel.saveAnnouncement(it) },
                            onEditAnnouncement = { id, title, content, poll, type, sIds, sNames ->
                                viewModel.editAnnouncement(id, title, content, poll, type, sIds, sNames)
                            },
                            onDeleteAnnouncement = { viewModel.deleteAnnouncement(it) },
                            onVotePollOption = { aId, oId, voter -> viewModel.votePollOption(aId, oId, voter) },
                            onSendChatMessage = { sId, text, isTeacher, email, name, replyTo, scheduledFor ->
                                viewModel.sendChatMessage(sId, text, isTeacher, email, name, replyTo, scheduledFor)
                            },
                            onSendBulkChatMessage = { recipients, text, scheduledFor ->
                                viewModel.sendBulkChatMessages(recipients, text, scheduledFor)
                            },
                            onToggleChatReaction = { msgId, emoji -> viewModel.toggleChatMessageReaction(msgId, emoji) },
                            onDeliverScheduledMessageNow = { msgId -> viewModel.deliverScheduledMessageNow(msgId) },
                            onUpdateScheduledMessage = { msgId, text, scheduledFor ->
                                viewModel.updateScheduledMessage(msgId, text, scheduledFor)
                            },
                            onCancelScheduledMessage = { msgId -> viewModel.cancelScheduledMessage(msgId) },
                            onSimulateParentReply = { sId, text -> viewModel.simulateParentReply(sId, text) },
                            onMarkChatAsRead = { sId, email -> viewModel.markChatMessagesAsRead(sId, email) },
                            onDeleteChatMessagesForMe = { viewModel.deleteChatMessagesForMe(it) },
                            onDeleteChatMessagesForEveryone = { viewModel.deleteChatMessagesForEveryone(it) },
                            onClearChatHistoryForMe = { sId, email -> viewModel.clearChatHistoryForMe(sId, email) },
                            onOpenNotificationCenter = { viewModel.setNotificationCenterOpen(true) },
                            onCloseNotificationCenter = { viewModel.setNotificationCenterOpen(false) },
                            onMarkNotificationAsRead = { viewModel.markNotificationAsRead(it) },
                            onMarkAllNotificationsAsRead = { viewModel.markAllNotificationsAsRead() },
                            onSetActiveChatStudent = { viewModel.setActiveChatStudent(it) },
                            onOpenUpdateDialog = { viewModel.openGlobalUpdateDialog(forceCheck = true) },
                            connectedBoardSessionCode = uiState.connectedBoardSessionCode,
                            isLiveBoardMirrorActive = uiState.isLiveBoardMirrorActive,
                            isSmartBoardDialogOpen = uiState.isSmartBoardDialogOpen,
                            isConnectingBoard = uiState.isConnectingBoard,
                            smartBoardMessage = uiState.smartBoardMessage,
                            activeBoardCastTitle = uiState.activeBoardCastTitle,
                            activeBoardCastType = uiState.activeBoardCastType,
                            isBroadcastingToBoard = uiState.isBroadcastingToBoard,
                            boardCastProgress = uiState.boardCastProgress,
                            isScreenMirrorPaused = uiState.isScreenMirrorPaused,
                            screenMirrorPauseReason = uiState.screenMirrorPauseReason,
                            screenMirrorPackageName = uiState.screenMirrorPackageName,
                            smartBoardDailyNote = uiState.smartBoardDailyNote,
                            smartBoardDailyNoteLabel = uiState.smartBoardDailyNoteLabel,
                            onOpenSmartBoardDialog = { viewModel.openSmartBoardDialog() },
                            onDismissSmartBoardDialog = { viewModel.dismissSmartBoardDialog() },
                            onConnectSmartBoard = { viewModel.connectSmartBoard(it) },
                            onToggleLiveBoardMirror = { viewModel.toggleLiveBoardMirror(it) },
                            onDisconnectSmartBoard = { viewModel.disconnectSmartBoard() },
                            onSaveSmartBoardDailyNote = { label, text ->
                                viewModel.saveSmartBoardDailyNote(label, text)
                            },
                            onClearSmartBoardDailyNote = { viewModel.clearSmartBoardDailyNote() },
                            onCastFileToBoard = { viewModel.castFileToSmartBoard(it) },
                            onCastUrlOrProgramToBoard = { url, title, isProgram ->
                                viewModel.castUrlToSmartBoard(url, title, isProgram)
                            },
                            onStartAppMirror = { pkg, appName, resCode, resData ->
                                viewModel.startAppScreenMirror(pkg, appName, resCode, resData)
                            },
                            onStopBoardCast = { viewModel.stopSmartBoardCast() },
                            onDeleteStudent = { viewModel.deleteStudent(it) },
                            onDeleteTeacherRoleAndClass = { viewModel.deleteTeacherRoleAndClassroom() },
                            onDeleteEntireAccount = { viewModel.deleteEntireAccount(context) },
                            onLogout = { viewModel.signOut(context) },
                            currentUser = uiState.currentUser,
                            notificationPreferences = uiState.notificationPreferences,
                            onToggleNotificationCategory = { category, enabled ->
                                viewModel.toggleNotificationCategory(category, enabled)
                            },
                            onSetAllNotificationCategories = { enabled ->
                                viewModel.setAllNotificationCategories(enabled)
                            },
                            quietHoursConfig = uiState.quietHoursConfig,
                            deferredQuietCount = uiState.deferredQuietCount,
                            onToggleQuietHours = { viewModel.toggleQuietHours(it) },
                            onUpdateQuietHoursWindow = { sH, sM, eH, eM ->
                                viewModel.updateQuietHoursWindow(sH, sM, eH, eM)
                            },
                            teacherParentDeferralConfig = uiState.teacherParentDeferralConfig,
                            onToggleTeacherParentDeferral = { viewModel.toggleTeacherParentDeferral(it) },
                            onUpdateTeacherParentDeferralWindow = { sH, sM, dH, dM ->
                                viewModel.updateTeacherParentDeferralWindow(sH, sM, dH, dM)
                            },
                            onSyncWebMigrationData = { customJson ->
                                viewModel.syncWebMigrationDataNow(customJson)
                            },
                            pendingLibraryRequestsCount = uiState.pendingLibraryRequestsCount
                        )
                    } else {
                        viewModel.resetToRoleSelection()
                    }
                }

                ScreenState.PARENT_PORTAL -> {
                    val students by viewModel.students.collectAsStateWithLifecycle()
                    val linkedStudents = students.filter { it.id in uiState.linkedParentStudentIds && it.isParentLinked }
                    val activeStudent = linkedStudents.firstOrNull { it.id == uiState.activeParentStudentId } ?: linkedStudents.firstOrNull()

                    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                    DisposableEffect(uiState.linkedParentStudentIds) {
                        viewModel.syncLivePresenceSocket()
                        onDispose {
                            viewModel.updateParentPresence(isOnline = false)
                        }
                    }

                    LaunchedEffect(lifecycleOwner, uiState.linkedParentStudentIds) {
                        lifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
                            viewModel.syncLivePresenceSocket()
                            while (true) {
                                kotlinx.coroutines.delay(75_000L)
                                viewModel.sendParentPresenceHeartbeatIfNeeded()
                            }
                        }
                    }

                    ParentPortalSubScreen(
                        classRoom = uiState.activeClassRoom,
                        allStudents = students,
                        linkedStudents = linkedStudents,
                        activeStudent = activeStudent,
                        announcements = uiState.announcements,
                        chatMessages = uiState.chatMessages,
                        scheduleConfig = uiState.scheduleConfig,
                        scheduleLessons = uiState.scheduleLessons,
                        seatingPlanState = uiState.seatingPlanState,
                        currentUser = uiState.currentUser,
                        notifications = uiState.notifications,
                        isNotificationCenterOpen = uiState.isNotificationCenterOpen,
                        isDarkTheme = uiState.isDarkTheme,
                        onToggleTheme = { viewModel.toggleTheme() },
                        onLinkStudentByCode = { viewModel.linkStudentByParentCode(it) },
                        onUnlinkStudent = { viewModel.unlinkStudentFromParent(it) },
                        onSelectActiveStudent = { viewModel.selectActiveParentStudent(it) },
                        onDeleteRole = { viewModel.deleteParentRole() },
                        onDeleteEntireAccount = { viewModel.deleteEntireAccount(context) },
                        onOpenNotificationCenter = { viewModel.setNotificationCenterOpen(true) },
                        onCloseNotificationCenter = { viewModel.setNotificationCenterOpen(false) },
                        onMarkNotificationAsRead = { viewModel.markNotificationAsRead(it) },
                        onMarkAllNotificationsAsRead = { viewModel.markAllNotificationsAsRead() },
                        onOpenUpdateDialog = { viewModel.openGlobalUpdateDialog(forceCheck = true) },
                        onLogout = { viewModel.signOut(context) },
                        onVotePoll = { aId, oId -> viewModel.votePollOption(aId, oId, uiState.currentUser.fullName.ifBlank { "Veli" }) },
                        onSendChatMessage = { sId, text, replyTo ->
                            viewModel.sendChatMessage(sId, text, false, uiState.currentUser.email, uiState.currentUser.fullName, replyTo = replyTo)
                        },
                        onToggleChatReaction = { msgId, emoji -> viewModel.toggleChatMessageReaction(msgId, emoji) },
                        onMarkAnnouncementRead = { aId -> viewModel.markAnnouncementReadByParent(aId, uiState.currentUser.fullName) },
                        requestedTab = uiState.requestedParentTabName?.let { tabName ->
                            try { ParentPortalTab.valueOf(tabName) } catch (_: Exception) { null }
                        },
                        requestedCommIsChat = uiState.requestedParentCommIsChat,
                        onRequestedTabConsumed = { viewModel.consumeRequestedParentTab() },
                        onCommunicationTabStateChanged = { isComm, isChat ->
                            viewModel.setParentCommunicationScreenState(isComm, isChat)
                        },
                        onMarkChatAsRead = { sId ->
                            viewModel.markChatMessagesAsRead(sId, viewerIsTeacher = false)
                        },
                        onDeleteChatMessagesForMe = { viewModel.deleteChatMessagesForMe(it) },
                        onDeleteChatMessagesForEveryone = { viewModel.deleteChatMessagesForEveryone(it) },
                        onClearChatHistoryForMe = { sId -> viewModel.clearChatHistoryForMe(sId) },
                        notificationPreferences = uiState.notificationPreferences,
                        onToggleNotificationCategory = { category, enabled ->
                            viewModel.toggleNotificationCategory(category, enabled)
                        },
                        onSetAllNotificationCategories = { enabled ->
                            viewModel.setAllNotificationCategories(enabled)
                        },
                        quietHoursConfig = uiState.quietHoursConfig,
                        deferredQuietCount = uiState.deferredQuietCount,
                        onToggleQuietHours = { viewModel.toggleQuietHours(it) },
                        onUpdateQuietHoursWindow = { sH, sM, eH, eM ->
                            viewModel.updateQuietHoursWindow(sH, sM, eH, eM)
                        }
                    )
                }
            }

            // 🚀 Canlı Yayınlanan / İndirilen / Doğrudan Kurulan Güncelleme Banner'ı (tek pencere, sıfır diyalog yağmuru)
            val isBgDownloading = bgUpdateState is BackgroundUpdateState.Downloading && !uiState.isGlobalUpdateDialogOpen
            val isBgInstalling = bgUpdateState is BackgroundUpdateState.Installing && !uiState.isGlobalUpdateDialogOpen
            val isBgReady = bgUpdateState is BackgroundUpdateState.ReadyToInstall && !uiState.isGlobalUpdateDialogOpen
            val showBanner = isBgDownloading || isBgInstalling || isBgReady ||
                (!uiState.isGlobalUpdateDialogOpen && uiState.availableUpdateInfo?.isUpdateAvailable == true && !uiState.isUpdateBannerDismissed)

            AnimatedVisibility(
                visible = showBanner,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                val readyState = bgUpdateState as? BackgroundUpdateState.ReadyToInstall
                val installingState = bgUpdateState as? BackgroundUpdateState.Installing
                val downloadingState = bgUpdateState as? BackgroundUpdateState.Downloading
                val updateInfo = installingState?.info ?: readyState?.info ?: downloadingState?.info ?: uiState.availableUpdateInfo

                updateInfo?.let { info ->
                    val borderAccent = if (readyState != null || installingState != null) com.example.ui.theme.BirizSuccess else com.example.ui.theme.BirizTurquoise
                    var isNotesExpanded by remember(info.latestVersionCode) { mutableStateOf(false) }
                    val releaseItems = remember(info) { info.parsedReleaseItems() }
                    val isIdleAnnouncement = installingState == null && downloadingState == null && readyState == null

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (uiState.isDarkTheme) Color(0xFF0F2942) else Color(0xFF0F172A),
                        border = BorderStroke(1.2.dp, borderAccent),
                        shadowElevation = 10.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = borderAccent.copy(alpha = 0.2f),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.SystemUpdate,
                                                contentDescription = null,
                                                tint = borderAccent,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        val titleStr = when {
                                            installingState != null -> "⚙️ BİRİZ v${info.latestVersionName} Kuruluyor..."
                                            readyState != null -> "✅ BİRİZ v${info.latestVersionName} Hazır!"
                                            downloadingState != null -> "⬇️ v${info.latestVersionName} Yükleniyor (%${(downloadingState.progress * 100).toInt()})"
                                            else -> "🚀 Yeni BİRİZ v${info.latestVersionName} Yayında!"
                                        }
                                        val subStr = when {
                                            installingState != null -> "Otomatik kurulum başlatılıyor, tamamlandığında uygulama kapanır"
                                            readyState != null -> "Kurulumu tamamlamak için dokunun"
                                            downloadingState != null -> if (downloadingState.totalBytes > 0L) {
                                                "${AppUpdateDownloadController.formatBytesMb(downloadingState.downloadedBytes)} / ${AppUpdateDownloadController.formatBytesMb(downloadingState.totalBytes)} • İndikten sonra kurulur"
                                            } else {
                                                "İndirme tamamlanınca otomatik kurulur"
                                            }
                                            else -> info.releaseTitle.ifBlank { "Tek dokunuşla indir ve otomatik kur" }
                                        }
                                        Text(
                                            text = titleStr,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = subStr,
                                            fontSize = 10.5.sp,
                                            color = Color(0xFF94A3B8),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    when {
                                        installingState != null -> {
                                            // Kurulum sistem tarafından devam ederken ek pencere veya buton açma
                                        }

                                        readyState != null -> {
                                            Button(
                                                onClick = {
                                                    AppUpdateNotificationHelper.cancelUpdateNotification(context)
                                                    if (AppUpdateManager.canInstallPackages(context)) {
                                                        AppUpdateDownloadController.lastAutoPromptedVersionCode = readyState.info.latestVersionCode
                                                        AppUpdateDownloadController.markInstalling(readyState.info, readyState.apkFile)
                                                    }
                                                    AppUpdateManager.installApk(context, readyState.apkFile, readyState.info.latestVersionCode)
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.BirizSuccess),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text("Şimdi Kur", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }

                                        downloadingState != null -> {
                                            IconButton(
                                                onClick = { AppUpdateDownloadController.cancelDownload(context) },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "İndirmeyi İptal Et",
                                                    tint = Color(0xFF94A3B8),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        else -> {
                                            Button(
                                                onClick = {
                                                    viewModel.startDirectUpdate(context, info)
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.BirizTurquoise),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text("Yükle", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }

                                            IconButton(
                                                onClick = { viewModel.dismissUpdateBanner() },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Kapat",
                                                    tint = Color(0xFF94A3B8),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (isIdleAnnouncement && releaseItems.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                val visibleItems = if (isNotesExpanded) releaseItems else releaseItems.take(2)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0x16FFFFFF),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        visibleItems.forEach { item ->
                                            Text(
                                                text = buildAnnotatedString {
                                                    withStyle(SpanStyle(color = com.example.ui.theme.BirizTurquoise, fontWeight = FontWeight.Bold)) {
                                                        append("• ${item.title}: ")
                                                    }
                                                    withStyle(SpanStyle(color = Color(0xFFE2E8F0))) {
                                                        append(item.summary)
                                                    }
                                                },
                                                fontSize = 10.8.sp,
                                                lineHeight = 14.5.sp,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (releaseItems.size > 2) {
                                            Text(
                                                text = if (isNotesExpanded) "▲ Daralt" else "▼ Tüm yenilikler (+${releaseItems.size - 2})",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = com.example.ui.theme.BirizTurquoise,
                                                modifier = Modifier
                                                    .padding(top = 2.dp)
                                                    .clickable { isNotesExpanded = !isNotesExpanded }
                                            )
                                        }
                                    }
                                }
                            }

                            if (downloadingState != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                if (downloadingState.totalBytes > 0L) {
                                    LinearProgressIndicator(
                                        progress = { downloadingState.progress },
                                        modifier = Modifier.fillMaxWidth().height(4.dp),
                                        color = com.example.ui.theme.BirizTurquoise,
                                        trackColor = Color(0x30FFFFFF)
                                    )
                                } else {
                                    LinearProgressIndicator(
                                        modifier = Modifier.fillMaxWidth().height(4.dp),
                                        color = com.example.ui.theme.BirizTurquoise,
                                        trackColor = Color(0x30FFFFFF)
                                    )
                                }
                            } else if (installingState != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth().height(4.dp),
                                    color = com.example.ui.theme.BirizSuccess,
                                    trackColor = Color(0x30FFFFFF)
                                )
                            }
                        }
                    }
                }
            }

            // Global Güncelleme Yükleme Penceresi (Bildirimden tıklandığında veya butonla açılır)
            if (uiState.isGlobalUpdateDialogOpen) {
                AppUpdateDialog(
                    isDarkTheme = uiState.isDarkTheme,
                    initialInfo = uiState.availableUpdateInfo,
                    autoStartDownload = uiState.isAutoDownloadOnOpen,
                    onUpdateDiscovered = { viewModel.onManualUpdateDiscovered(it) },
                    onDismiss = { viewModel.dismissGlobalUpdateDialog() }
                )
            }

            // Global Kalıcı Hesap Silme İlerleme Kaplaması (tüm ekranlarda görünür, dokunuşları engeller)
            if (uiState.isDeleting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x99020617))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (uiState.isDarkTheme) Color(0xFF0F172A) else Color.White,
                        border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.5f)),
                        shadowElevation = 16.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFFDC2626),
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "Hesap Kalıcı Olarak Siliniyor",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (uiState.isDarkTheme) Color.White else BirizNavyPrimary
                            )
                            Text(
                                text = uiState.deletingStatusMessage,
                                fontSize = 12.5.sp,
                                color = if (uiState.isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF475569),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
