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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.ClassRegistryRepository
import com.example.ui.components.AppUpdateDialog
import com.example.ui.screens.GoogleSignInScreen
import com.example.ui.screens.RoleSelectionScreen
import com.example.ui.screens.SetupSuccessScreen
import com.example.ui.screens.TeacherDashboardScreen
import com.example.ui.screens.TeacherSetupWizardScreen
import com.example.ui.theme.BirizNavyPrimary
import com.example.ui.theme.BirizTheme
import com.example.ui.viewmodel.BirizViewModel
import com.example.ui.viewmodel.BirizViewModelFactory
import com.example.ui.viewmodel.ScreenState
import com.example.util.AppUpdateNotificationHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {

    private val viewModel: BirizViewModel by viewModels {
        BirizViewModelFactory(ClassRegistryRepository(applicationContext), applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Android 8.0+ bildirim kanalını açılışta garantiye al
        AppUpdateNotificationHelper.createNotificationChannel(applicationContext)

        // Cihaz hafızasında eski APK kalıntılarının birikmesini önlemek için temizle
        com.example.util.AppUpdateManager.cleanOldApkFiles(applicationContext)

        // Bildirim çubuğundan tıklandıysa güncelleme ekranını doğrudan aç
        handleUpdateIntent(intent)

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            BirizTheme(darkTheme = uiState.isDarkTheme) {
                BirizApp(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleUpdateIntent(intent)
    }

    private fun handleUpdateIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(AppUpdateNotificationHelper.EXTRA_OPEN_UPDATE_DIALOG, false) == true) {
            viewModel.openGlobalUpdateDialog()
            intent.removeExtra(AppUpdateNotificationHelper.EXTRA_OPEN_UPDATE_DIALOG)
        }
    }
}

@Composable
fun BirizApp(viewModel: BirizViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val matchingSchools by viewModel.matchingSchools.collectAsStateWithLifecycle()
    val branchStatuses by viewModel.branchStatuses.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

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
                    uiState.currentScreen != ScreenState.ROLE_AND_KVKK
    BackHandler(enabled = canGoBack) {
        when (uiState.currentScreen) {
            ScreenState.TEACHER_SETUP -> viewModel.resetToRoleSelection()
            ScreenState.SETUP_SUCCESS -> viewModel.navigateToTeacherDashboard()
            ScreenState.TEACHER_DASHBOARD -> {
                if (uiState.currentSubPage != com.example.ui.viewmodel.DashboardSubPage.SINIF_LISTESI) {
                    viewModel.selectSubPage(com.example.ui.viewmodel.DashboardSubPage.SINIF_LISTESI)
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
                        onToggleTheme = { viewModel.toggleTheme() }
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
                            onSendChatMessage = { sId, text, isTeacher, email, name -> viewModel.sendChatMessage(sId, text, isTeacher, email, name) },
                            onSimulateParentReply = { sId, text -> viewModel.simulateParentReply(sId, text) },
                            onMarkChatAsRead = { sId, email -> viewModel.markChatMessagesAsRead(sId, email) },
                            onOpenNotificationCenter = { viewModel.setNotificationCenterOpen(true) },
                            onCloseNotificationCenter = { viewModel.setNotificationCenterOpen(false) },
                            onMarkNotificationAsRead = { viewModel.markNotificationAsRead(it) },
                            onMarkAllNotificationsAsRead = { viewModel.markAllNotificationsAsRead() },
                            onSetActiveChatStudent = { viewModel.setActiveChatStudent(it) },
                            onDeleteStudent = { viewModel.deleteStudent(it) },
                            onDeleteTeacherRoleAndClass = { viewModel.deleteTeacherRoleAndClassroom() },
                            onDeleteEntireAccount = { viewModel.deleteEntireAccount(context) },
                            onLogout = { viewModel.signOut(context) }
                        )
                    } else {
                        viewModel.resetToRoleSelection()
                    }
                }
            }

            // Programda gezinirken yeni bir güncelleme çıktığında gösterilen üst afiş
            val updateAvailable = uiState.availableUpdateInfo?.isUpdateAvailable == true
            val showBanner = updateAvailable && !uiState.isUpdateBannerDismissed && !uiState.isGlobalUpdateDialogOpen

            AnimatedVisibility(
                visible = showBanner,
                enter = slideInVertically(initialOffsetY = { -it }),
                exit = slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (uiState.isDarkTheme) Color(0xFF1E293B) else Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFF6366F1)),
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.openGlobalUpdateDialog() }
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF6366F1),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.SystemUpdate,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Yeni Sürüm Hazır: v${uiState.availableUpdateInfo?.latestVersionName} 🚀",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.isDarkTheme) Color.White else BirizNavyPrimary
                                )
                                Text(
                                    text = "Yükleme ekranına gitmek için dokunun.",
                                    fontSize = 10.sp,
                                    color = if (uiState.isDarkTheme) Color(0xFF94A3B8) else Color(0xFF475569)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = { viewModel.openGlobalUpdateDialog() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Yükle", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(
                                onClick = { viewModel.dismissUpdateBanner() },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Gizle",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(15.dp)
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
                    onDismiss = { viewModel.dismissGlobalUpdateDialog() }
                )
            }
        }
    }
}
