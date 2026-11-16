package com.example.ui.components

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.example.BuildConfig
import com.example.data.model.ClassRoom
import com.example.data.model.Student
import com.example.data.model.UserRole
import com.example.ui.theme.BirizAmber
import com.example.ui.theme.BirizDarkCardBorder
import com.example.ui.theme.BirizError
import com.example.ui.theme.BirizNavyDark
import com.example.ui.theme.BirizNavyLight
import com.example.ui.theme.BirizNavyPrimary
import com.example.ui.theme.BirizSuccess
import com.example.ui.theme.BirizTurquoise
import com.example.ui.viewmodel.AuthenticatedUser
import com.example.util.AppUpdateManager
import com.example.util.NotificationCategory
import com.example.util.NotificationPreferencesHelper
import com.example.util.QuietHoursConfig
import com.example.util.UpdateInfo
import kotlinx.coroutines.launch

private enum class ProfileMenuScreen {
    DROPDOWN_MENU,
    PROFILE_INFO,
    NOTIFICATION_PREFS,
    VERSION_INFO,
    EXIT_OPTIONS,
    CONFIRM_DELETE_ROLE,
    CONFIRM_DELETE_ACCOUNT
}

private fun exitAppGracefully(context: Context) {
    var ctx: Context? = context
    while (ctx is ContextWrapper) {
        if (ctx is Activity) {
            ctx.finishAffinity()
            return
        }
        ctx = ctx.baseContext
    }
}

@Composable
fun UserAvatarCircle(
    user: AuthenticatedUser,
    isDarkTheme: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 34.dp
) {
    Surface(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable { onClick() }
            .testTag("user_avatar_circle"),
        shape = CircleShape,
        color = if (isDarkTheme) Color(0x35334E68) else Color(0x20FFFFFF),
        border = BorderStroke(1.5.dp, if (isDarkTheme) BirizTurquoise.copy(alpha = 0.7f) else Color(0x66FFFFFF))
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (!user.photoUrl.isNullOrBlank()) {
                SubcomposeAsyncImage(
                    model = user.photoUrl,
                    contentDescription = "Kullanıcı Profil Resmi",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    loading = {
                        AvatarInitialsFallback(initials = user.avatarInitials, size = size)
                    },
                    error = {
                        AvatarInitialsFallback(initials = user.avatarInitials, size = size)
                    }
                )
            } else {
                AvatarInitialsFallback(initials = user.avatarInitials, size = size)
            }
        }
    }
}

@Composable
private fun AvatarInitialsFallback(initials: String, size: Dp) {
    Surface(
        shape = CircleShape,
        color = BirizAmber,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = initials.ifBlank { "B" },
                fontSize = (size.value * 0.4).sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
    }
}

@Composable
fun UserProfileMenuDialog(
    currentUser: AuthenticatedUser,
    currentRole: UserRole,
    classRoom: ClassRoom?,
    linkedStudents: List<Student> = emptyList(),
    isDarkTheme: Boolean,
    notificationPreferences: Map<String, Boolean> = NotificationPreferencesHelper.defaultPreferences(),
    onToggleNotificationCategory: (NotificationCategory, Boolean) -> Unit = { _, _ -> },
    onSetAllNotificationCategories: (Boolean) -> Unit = {},
    quietHoursConfig: QuietHoursConfig = QuietHoursConfig(),
    deferredQuietCount: Int = 0,
    onToggleQuietHours: (Boolean) -> Unit = {},
    onUpdateQuietHoursWindow: (Int, Int, Int, Int) -> Unit = { _, _, _, _ -> },
    onToggleTheme: () -> Unit,
    onDeleteRole: () -> Unit,
    onDeleteEntireAccount: () -> Unit,
    onCheckUpdate: () -> Unit = {},
    onLogout: () -> Unit,
    onDismissRequest: () -> Unit,
    activeParentStudentId: String? = null,
    onSelectParentStudent: ((String) -> Unit)? = null,
    onAddParentStudentClick: (() -> Unit)? = null,
    onUnlinkParentStudentClick: ((Student) -> Unit)? = null
) {
    val context = LocalContext.current
    var activeScreen by remember { mutableStateOf(ProfileMenuScreen.DROPDOWN_MENU) }
    var showParentQrDialog by remember { mutableStateOf(false) }
    var isExitQuickExpanded by remember { mutableStateOf(false) }

    val displayName = remember(currentUser, classRoom, currentRole) {
        if (currentRole == UserRole.TEACHER && !classRoom?.teacherName.isNullOrBlank() &&
            (currentUser.fullName.isBlank() || currentUser.fullName == "Öğretmen")
        ) {
            classRoom!!.teacherName
        } else if (currentUser.fullName.isBlank() || currentUser.fullName == "Öğretmen") {
            currentUser.email.substringBefore("@").replace(".", " ").split(" ")
                .filter { it.isNotBlank() }
                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }
                .ifBlank { if (currentRole == UserRole.TEACHER) "Sınıf Öğretmeni" else "Öğrenci Velisi" }
        } else {
            currentUser.fullName
        }
    }

    val displayEmail = remember(currentUser, classRoom) {
        currentUser.email.ifBlank {
            classRoom?.teacherEmail?.ifBlank { "cihan.ozel10@gmail.com" } ?: "cihan.ozel10@gmail.com"
        }
    }

    val roleCategories = remember(currentRole) { NotificationCategory.categoriesForRole(currentRole) }
    val activeCategoryCount = remember(notificationPreferences, roleCategories) {
        roleCategories.count { NotificationPreferencesHelper.isCategoryEnabled(notificationPreferences, it) }
    }

    when (activeScreen) {
        // =========================================================================
        // 1. PROFİL RESMİNE TIKLAYINCA SAĞ ÜSTTE AÇILAN PROFESYONEL MENÜ
        // =========================================================================
        ProfileMenuScreen.DROPDOWN_MENU -> {
            Dialog(
                onDismissRequest = onDismissRequest,
                properties = DialogProperties(
                    usePlatformDefaultWidth = false,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onDismissRequest() }
                        .padding(top = 54.dp, end = 12.dp, start = 12.dp, bottom = 16.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    Surface(
                        modifier = Modifier
                            .widthIn(min = 300.dp, max = 350.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { /* Menü içi tıklamalar arka planı kapatmasın */ }
                            .testTag("user_profile_menu_dialog"),
                        shape = RoundedCornerShape(24.dp),
                        color = if (isDarkTheme) Color(0xFF0F172A) else Color.White,
                        shadowElevation = 18.dp,
                        border = BorderStroke(
                            1.2.dp,
                            if (isDarkTheme) BirizTurquoise.copy(alpha = 0.45f) else Color(0xFFCBD5E1)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            // --- ÜST KULLANICI KARTI BAŞLIĞI ---
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isDarkTheme) {
                                            Brush.horizontalGradient(
                                                listOf(Color(0xFF162440), Color(0xFF0E1A32))
                                            )
                                        } else {
                                            Brush.horizontalGradient(
                                                listOf(BirizNavyPrimary, BirizNavyLight)
                                            )
                                        }
                                    )
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
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
                                        Box {
                                            Surface(
                                                shape = CircleShape,
                                                color = BirizAmber,
                                                border = BorderStroke(2.dp, Color.White.copy(alpha = 0.85f)),
                                                modifier = Modifier.size(44.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    if (!currentUser.photoUrl.isNullOrBlank()) {
                                                        AsyncImage(
                                                            model = currentUser.photoUrl,
                                                            contentDescription = "Profil Resmi",
                                                            modifier = Modifier
                                                                .fillMaxSize()
                                                                .clip(CircleShape),
                                                            contentScale = ContentScale.Crop,
                                                            error = rememberVectorPainter(Icons.Default.Person),
                                                            placeholder = rememberVectorPainter(Icons.Default.Person)
                                                        )
                                                    } else {
                                                        Text(
                                                            text = currentUser.avatarInitials.ifBlank { "B" },
                                                            fontSize = 16.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = Color.White
                                                        )
                                                    }
                                                }
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .size(12.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White)
                                                    .padding(2.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF10B981))
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column {
                                            Text(
                                                text = displayName,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = displayEmail,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color.White.copy(alpha = 0.8f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = BirizTurquoise.copy(alpha = 0.25f)
                                            ) {
                                                Text(
                                                    text = if (currentRole == UserRole.TEACHER) {
                                                        "👨‍🏫 Sınıf Öğretmeni" + (classRoom?.let { " • ${it.gradeLevel}/${it.section}" } ?: "")
                                                    } else {
                                                        "👨‍👩‍👧‍👦 Öğrenci Velisi" + (if (linkedStudents.isNotEmpty()) " • ${linkedStudents.size} Öğrenci" else "")
                                                    },
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF5EEAD4),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    IconButton(
                                        onClick = onDismissRequest,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Menüyü Kapat",
                                            tint = Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // --- 1. AÇIK / KOYU TEMA (TEK TIK GEÇİŞ) ---
                                Surface(
                                    onClick = { onToggleTheme() },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFFFFBEB),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isDarkTheme) BirizAmber.copy(alpha = 0.45f) else Color(0xFFFDE68A)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("profile_menu_theme_toggle")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (isDarkTheme) BirizAmber.copy(alpha = 0.2f) else Color(0xFFFEF3C7),
                                                modifier = Modifier.size(38.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                                                        contentDescription = "Tema Değiştir",
                                                        tint = if (isDarkTheme) BirizAmber else Color(0xFFD97706),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = if (isDarkTheme) "Koyu Tema Aktif" else "Açık Tema Aktif",
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (isDarkTheme) Color.White else BirizNavyPrimary
                                                )
                                                Text(
                                                    text = if (isDarkTheme) "Tek tıkla Açık Temaya geç ☀️" else "Tek tıkla Koyu Temaya geç 🌙",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (isDarkTheme) Color(0xFFFBBF24) else Color(0xFFB45309)
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = if (isDarkTheme) BirizAmber else BirizNavyPrimary
                                        ) {
                                            Text(
                                                text = if (isDarkTheme) "🌙 Koyu" else "☀️ Açık",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }

                                // --- 2. PROFİL BİLGİLERİ ---
                                DropdownMenuActionItem(
                                    icon = Icons.Default.Person,
                                    iconTint = BirizTurquoise,
                                    iconBg = BirizTurquoise.copy(alpha = 0.15f),
                                    title = "Profil Bilgileri",
                                    subtitle = if (currentRole == UserRole.TEACHER) {
                                        "Profil, ${classRoom?.let { "${it.gradeLevel}/${it.section} sınıfı" } ?: "sınıf bilgisi"}, rol ve hesap silme"
                                    } else {
                                        "Profil, tanımlı öğrenciler (${linkedStudents.size}), rol ve hesap silme"
                                    },
                                    isDarkTheme = isDarkTheme,
                                    testTag = "profile_menu_item_profile_info",
                                    onClick = { activeScreen = ProfileMenuScreen.PROFILE_INFO }
                                )

                                // --- 3. BİLDİRİM TERCİHLERİ ---
                                DropdownMenuActionItem(
                                    icon = Icons.Default.Notifications,
                                    iconTint = Color(0xFF0284C7),
                                    iconBg = Color(0xFF0284C7).copy(alpha = 0.15f),
                                    title = "Bildirim Tercihleri",
                                    subtitle = "$activeCategoryCount/${roleCategories.size} kategori açık" +
                                        if (quietHoursConfig.enabled) " • 🌙 Sessiz Saatler" else "",
                                    badgeText = "$activeCategoryCount/${roleCategories.size}",
                                    isDarkTheme = isDarkTheme,
                                    testTag = "profile_notification_prefs_toggle",
                                    onClick = { activeScreen = ProfileMenuScreen.NOTIFICATION_PREFS }
                                )

                                // --- 4. SÜRÜM BİLGİSİ (SÜRÜM BİLGİSİ VE GÜNCELLEME DENETLEME) ---
                                DropdownMenuActionItem(
                                    icon = Icons.Default.SystemUpdate,
                                    iconTint = Color(0xFF10B981),
                                    iconBg = Color(0xFF10B981).copy(alpha = 0.15f),
                                    title = "Sürüm Bilgisi",
                                    subtitle = "Kurulu: v${BuildConfig.VERSION_NAME} • Güncelleme denetle",
                                    badgeText = "v${BuildConfig.VERSION_NAME}",
                                    isDarkTheme = isDarkTheme,
                                    testTag = "profile_check_update_button",
                                    onClick = { activeScreen = ProfileMenuScreen.VERSION_INFO }
                                )

                                HorizontalDivider(
                                    color = if (isDarkTheme) BirizDarkCardBorder else Color(0xFFE2E8F0),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )

                                // --- 5. ÇIKIŞ (UYGULAMADAN ÇIKIŞ / HESABI KAPAT ÇIK) ---
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isDarkTheme) Color(0xFF2A1419) else Color(0xFFFEF2F2),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isDarkTheme) Color(0xFF7F1D1D) else Color(0xFFFECACA)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { isExitQuickExpanded = !isExitQuickExpanded }
                                                .padding(horizontal = 12.dp, vertical = 11.dp)
                                                .testTag("profile_menu_item_exit"),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = Color(0xFFDC2626).copy(alpha = 0.15f),
                                                    modifier = Modifier.size(38.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = Icons.AutoMirrored.Filled.Logout,
                                                            contentDescription = "Çıkış",
                                                            tint = Color(0xFFDC2626),
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = "Çıkış",
                                                        fontSize = 13.5.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
                                                    )
                                                    Text(
                                                        text = "Uygulamadan çıkış veya hesabı kapat çık",
                                                        fontSize = 11.sp,
                                                        color = if (isDarkTheme) Color(0xFFF87171) else Color(0xFF991B1B)
                                                    )
                                                }
                                            }

                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                contentDescription = null,
                                                tint = if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFFDC2626)
                                            )
                                        }

                                        AnimatedVisibility(
                                            visible = isExitQuickExpanded,
                                            enter = expandVertically() + fadeIn(),
                                            exit = shrinkVertically() + fadeOut()
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                // Seçenek A: Uygulamadan Çıkış (Oturum Açık Kalır)
                                                Button(
                                                    onClick = {
                                                        onDismissRequest()
                                                        exitAppGracefully(context)
                                                    },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(44.dp)
                                                        .testTag("profile_exit_app_button"),
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (isDarkTheme) Color(0xFF334155) else BirizNavyPrimary,
                                                        contentColor = Color.White
                                                    )
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.PowerSettingsNew,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(17.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "Uygulamadan Çıkış (Oturum Açık Kalır)",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }

                                                // Seçenek B: Hesabı Kapat Çık (Oturumu Sonlandır)
                                                Button(
                                                    onClick = {
                                                        onDismissRequest()
                                                        onLogout()
                                                    },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(44.dp)
                                                        .testTag("profile_logout_button"),
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = Color(0xFFDC2626),
                                                        contentColor = Color.White
                                                    )
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(17.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "Hesabı Kapat Çık (Oturumu Sonlandır)",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 2. PROFİL BİLGİLERİ PENCERESİ (PROFİL, SINIF/ÖĞRENCİLER, ROL SİL, HESAP SİL)
        // =========================================================================
        ProfileMenuScreen.PROFILE_INFO -> {
            ProfileSubModalScaffold(
                title = "Profil Bilgileri",
                subtitle = if (currentRole == UserRole.TEACHER) {
                    "Öğretmen Profili, Sınıf Bilgileri ve Hesap Yönetimi"
                } else {
                    "Veli Profili, Tanımlı Öğrenciler ve Hesap Yönetimi"
                },
                isDarkTheme = isDarkTheme,
                onBackToMenu = { activeScreen = ProfileMenuScreen.DROPDOWN_MENU },
                onCloseAll = onDismissRequest
            ) {
                // 2.1. Profil ve Tanımlı Bilgiler Kartı
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = BirizAmber,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (!currentUser.photoUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = currentUser.photoUrl,
                                                contentDescription = "Profil",
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Text(
                                                text = currentUser.avatarInitials.ifBlank { "B" },
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = displayName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isDarkTheme) Color.White else BirizNavyPrimary
                                    )
                                    Text(
                                        text = if (currentRole == UserRole.TEACHER) "👨‍🏫 Sınıf Öğretmeni" else "👨‍👩‍👧‍👦 Öğrenci Velisi",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BirizTurquoise
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Aktif",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = if (isDarkTheme) BirizDarkCardBorder else Color(0xFFE2E8F0))

                        ProfileInfoKeyValueRow(
                            label = "Ad Soyad",
                            value = displayName,
                            isDarkTheme = isDarkTheme
                        )
                        ProfileInfoKeyValueRow(
                            label = "E-posta Adresi",
                            value = displayEmail,
                            isDarkTheme = isDarkTheme,
                            onCopy = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("E-posta", displayEmail))
                                Toast.makeText(context, "E-posta kopyalandı", Toast.LENGTH_SHORT).show()
                            }
                        )
                        ProfileInfoKeyValueRow(
                            label = "Kullanıcı Rolü",
                            value = if (currentRole == UserRole.TEACHER) "Yönetici Sınıf Öğretmeni" else "Öğrenci Velisi",
                            isDarkTheme = isDarkTheme
                        )
                        ProfileInfoKeyValueRow(
                            label = "Güvenlik & KVKK",
                            value = "Google Hesabı Bağlı • Onaylı",
                            valueColor = Color(0xFF10B981),
                            isDarkTheme = isDarkTheme
                        )
                    }
                }

                // 2.2. Role Göre: Tanımlı Öğretmen Sınıfı VEYA Tanımlı Öğrenciler
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFFBFDBFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (currentRole == UserRole.TEACHER) {
                                        "🏫 Tanımlı Öğretmen Sınıfı"
                                    } else {
                                        "👧👦 Tanımlı Öğrenciler (${linkedStudents.size})"
                                    },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDarkTheme) Color(0xFF38BDF8) else Color(0xFF0369A1)
                                )
                            }

                            if (currentRole == UserRole.PARENT && onAddParentStudentClick != null) {
                                Surface(
                                    onClick = { onAddParentStudentClick() },
                                    shape = RoundedCornerShape(8.dp),
                                    color = BirizTurquoise
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Öğrenci Ekle",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        if (currentRole == UserRole.TEACHER) {
                            if (classRoom != null) {
                                val girlsCount = linkedStudents.count { it.gender.equals("Kız", ignoreCase = true) }
                                val boysCount = linkedStudents.size - girlsCount
                                ProfileInfoKeyValueRow("Okul Adı", classRoom.schoolName, isDarkTheme)
                                ProfileInfoKeyValueRow("İl / İlçe", "${classRoom.cityName} / ${classRoom.districtName}", isDarkTheme)
                                ProfileInfoKeyValueRow("Sınıf / Şube", "${classRoom.gradeLevel}/${classRoom.section} Sınıfı", isDarkTheme)
                                ProfileInfoKeyValueRow(
                                    label = "Sınıf Mevcudu",
                                    value = if (linkedStudents.isNotEmpty()) {
                                        "${linkedStudents.size} Öğrenci ($girlsCount Kız, $boysCount Erkek)"
                                    } else {
                                        "Henüz öğrenci eklenmedi"
                                    },
                                    isDarkTheme = isDarkTheme
                                )

                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedButton(
                                    onClick = { showParentQrDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, if (isDarkTheme) BirizTurquoise else BirizNavyPrimary)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCode2,
                                        contentDescription = null,
                                        tint = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "📲 Veli Uygulama İndirme Karekodu",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                                    )
                                }
                            } else {
                                Text(
                                    text = "Henüz tanımlı bir sınıfınız bulunmamaktadır.",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        } else {
                            // VELİ ROLÜ: TANIMLI ÖĞRENCİLER LİSTESİ
                            if (linkedStudents.isNotEmpty()) {
                                linkedStudents.forEach { student ->
                                    val isSelected = activeParentStudentId == student.id
                                    val isGirl = student.gender.equals("Kız", ignoreCase = true)
                                    Surface(
                                        onClick = {
                                            onSelectParentStudent?.invoke(student.id)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isDarkTheme) {
                                            if (isSelected) Color(0xFF16304B) else Color(0xFF0F172A)
                                        } else {
                                            if (isGirl) Color(0xFFFFF1F5) else Color(0xFFF0F9FF)
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) BirizTurquoise else if (isGirl) Color(0xFFFBCFE8) else Color(0xFFBAE6FD)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = if (isGirl) "👧" else "👦",
                                                    fontSize = 20.sp
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = "${student.fullName} (#${student.studentNumber})",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = if (isDarkTheme) Color.White else BirizNavyPrimary
                                                    )
                                                    Text(
                                                        text = classRoom?.let {
                                                            "${it.schoolName} • ${it.gradeLevel}/${it.section} Sınıfı (${it.teacherName})"
                                                        } ?: "Sınıf Bilgisi Kayıtlı",
                                                        fontSize = 11.sp,
                                                        color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF475569)
                                                    )
                                                }
                                            }

                                            if (onUnlinkParentStudentClick != null) {
                                                IconButton(
                                                    onClick = { onUnlinkParentStudentClick(student) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.DeleteOutline,
                                                        contentDescription = "Öğrenci Bağlantısını Kaldır",
                                                        tint = Color(0xFFEF4444),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = "Henüz bağlı öğrenciniz bulunmamaktadır. Öğretmeninizden aldığınız 8 haneli Veli Katılım Kodu ile öğrencinizi ekleyebilirsiniz.",
                                    fontSize = 12.sp,
                                    color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF475569)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDarkTheme) Color(0x2510B981) else Color(0xFFF0FDF4),
                                border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF10B981) else Color(0xFFBBF7D0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VerifiedUser,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "KVKK Güvencesi: Yalnızca bağlı olduğunuz öğrencinizin verileri görüntülenir.",
                                        fontSize = 10.5.sp,
                                        color = if (isDarkTheme) Color(0xFF86EFAC) else Color(0xFF166534)
                                    )
                                }
                            }
                        }
                    }
                }

                // 2.3. Rol Silme ve Hesap Silme İşlemleri
                Text(
                    text = "⚠️ Rol ve Hesap Yönetimi",
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDarkTheme) Color(0xFFFBBF24) else Color(0xFFB45309)
                )

                // Rol Sil Butonu (Öğretmen Rolünü Sil / Veli Rolünü Sil)
                Surface(
                    onClick = { activeScreen = ProfileMenuScreen.CONFIRM_DELETE_ROLE },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDarkTheme) Color(0xFF2B1D12) else Color(0xFFFFF7ED),
                    border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF9A3412) else Color(0xFFFED7AA)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_delete_role_button")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonRemove,
                                contentDescription = null,
                                tint = Color(0xFFEA580C),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (currentRole == UserRole.TEACHER) {
                                        "Öğretmen Rolünü Sil"
                                    } else {
                                        "Veli Rolünü Sil"
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDarkTheme) Color(0xFFFDBA74) else Color(0xFFC2410C)
                                )
                                Text(
                                    text = if (currentRole == UserRole.TEACHER) {
                                        "Öğretmen rolünüz ve tanımlı sınıfınız sıfırlanır, Rol Seçimi ekranına dönersiniz."
                                    } else {
                                        "Veli rolünüz ve öğrenci bağlantılarınız kaldırılır, Rol Seçimi ekranına dönersiniz."
                                    },
                                    fontSize = 11.sp,
                                    color = if (isDarkTheme) Color(0xFFFED7AA) else Color(0xFF9A3412)
                                )
                            }
                        }
                        Text(
                            text = "Sil ❯",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFEA580C)
                        )
                    }
                }

                // Hesabımı Sil Butonu
                Surface(
                    onClick = { activeScreen = ProfileMenuScreen.CONFIRM_DELETE_ACCOUNT },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDarkTheme) Color(0xFF2A1215) else Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF991B1B) else Color(0xFFFECACA)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_delete_account_button")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Hesabımı Sil",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
                                )
                                Text(
                                    text = "Hesabınız ve veritabanındaki tüm kayıtlarınız kalıcı olarak silinir.",
                                    fontSize = 11.sp,
                                    color = if (isDarkTheme) Color(0xFFFECACA) else Color(0xFF991B1B)
                                )
                            }
                        }
                        Text(
                            text = "Sil ❯",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFDC2626)
                        )
                    }
                }
            }
        }

        // =========================================================================
        // 3. BİLDİRİM TERCİHLERİ PENCERESİ
        // =========================================================================
        ProfileMenuScreen.NOTIFICATION_PREFS -> {
            ProfileSubModalScaffold(
                title = "Bildirim Tercihleri",
                subtitle = "$activeCategoryCount/${roleCategories.size} kategori aktif • Sessiz saatler ve bildirim filtreleri",
                isDarkTheme = isDarkTheme,
                onBackToMenu = { activeScreen = ProfileMenuScreen.DROPDOWN_MENU },
                onCloseAll = onDismissRequest
            ) {
                NotificationPreferencesSection(
                    role = currentRole,
                    preferences = notificationPreferences,
                    isDarkTheme = isDarkTheme,
                    onToggleCategory = onToggleNotificationCategory,
                    onSetAllCategories = onSetAllNotificationCategories,
                    quietHoursConfig = quietHoursConfig,
                    deferredQuietCount = deferredQuietCount,
                    onToggleQuietHours = onToggleQuietHours,
                    onUpdateQuietHoursWindow = onUpdateQuietHoursWindow
                )
            }
        }

        // =========================================================================
        // 4. SÜRÜM BİLGİSİ VE GÜNCELLEME DENETLEME PENCERESİ
        // =========================================================================
        ProfileMenuScreen.VERSION_INFO -> {
            val coroutineScope = rememberCoroutineScope()
            var isChecking by remember { mutableStateOf(false) }
            var checkedUpdateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
            var checkError by remember { mutableStateOf<String?>(null) }

            fun triggerInlineCheck() {
                isChecking = true
                checkError = null
                coroutineScope.launch {
                    val repo = AppUpdateManager.getGitHubRepo(context)
                    val info = AppUpdateManager.checkForUpdates(context, repo)
                    isChecking = false
                    if (info == null) {
                        checkError = "Sunucuya bağlanılamadı. İnternet bağlantınızı kontrol edip tekrar deneyebilirsiniz."
                    } else {
                        checkedUpdateInfo = info
                    }
                }
            }

            LaunchedEffect(Unit) {
                triggerInlineCheck()
            }

            ProfileSubModalScaffold(
                title = "Sürüm Bilgisi",
                subtitle = "Uygulama sürüm detayları ve canlı güncelleme denetimi",
                isDarkTheme = isDarkTheme,
                onBackToMenu = { activeScreen = ProfileMenuScreen.DROPDOWN_MENU },
                onCloseAll = onDismissRequest
            ) {
                // Kurulu Sürüm Bilgi Kartı
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFF0F9FF),
                    border = BorderStroke(1.dp, if (isDarkTheme) BirizTurquoise.copy(alpha = 0.5f) else Color(0xFFBAE6FD)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "📱 BİRİZ Eğitim & Sınıf Yönetimi",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDarkTheme) Color.White else BirizNavyPrimary
                                )
                                Text(
                                    text = "Kurulu Sürüm: v${BuildConfig.VERSION_NAME} (Yapı #${BuildConfig.VERSION_CODE})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF475569)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = BirizTurquoise.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "v${BuildConfig.VERSION_NAME}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = if (isDarkTheme) BirizDarkCardBorder else Color(0xFFBAE6FD))

                        ProfileInfoKeyValueRow(
                            label = "Yüklü Sürüm Kodu",
                            value = "#${BuildConfig.VERSION_CODE}",
                            isDarkTheme = isDarkTheme
                        )
                        ProfileInfoKeyValueRow(
                            label = "Güncelleme Kanalı",
                            value = AppUpdateManager.getGitHubRepo(context).ifBlank { "CHNZL/biriz" },
                            isDarkTheme = isDarkTheme
                        )
                    }
                }

                // Canlı Denetleme Sonucu Kartı
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        when {
                            isChecking -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.padding(vertical = 6.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = BirizTurquoise,
                                        strokeWidth = 2.5.dp
                                    )
                                    Column {
                                        Text(
                                            text = "Güncellemeler Denetleniyor...",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDarkTheme) Color.White else BirizNavyPrimary
                                        )
                                        Text(
                                            text = "GitHub deposundaki en son yayınlanan sürüm kontrol ediliyor.",
                                            fontSize = 11.5.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }

                            checkError != null -> {
                                Text(
                                    text = "⚠️ $checkError",
                                    fontSize = 12.sp,
                                    color = Color(0xFFEA580C),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            checkedUpdateInfo != null -> {
                                val info = checkedUpdateInfo!!
                                if (info.isUpdateAvailable) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, Color(0xFF10B981)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = "🎉 Yeni Sürüm Mevcut: v${info.latestVersionName}",
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF10B981)
                                            )
                                            if (info.releaseTitle.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = info.releaseTitle,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (isDarkTheme) Color.White else BirizNavyPrimary
                                                )
                                            }
                                            val releaseItems = remember(info) { info.parsedReleaseItems() }
                                            if (releaseItems.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                UpdateReleaseNotesCard(
                                                    items = releaseItems,
                                                    isDarkTheme = isDarkTheme
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Harika! Uygulamanız Güncel",
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF10B981)
                                            )
                                            Text(
                                                text = "En güncel sürümü (v${BuildConfig.VERSION_NAME}) kullanıyorsunuz.",
                                                fontSize = 11.5.sp,
                                                color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF475569)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { triggerInlineCheck() },
                                enabled = !isChecking,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (isDarkTheme) BirizTurquoise else BirizNavyPrimary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Tekrar Denetle",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                                )
                            }

                            Button(
                                onClick = {
                                    onDismissRequest()
                                    onCheckUpdate()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (checkedUpdateInfo?.isUpdateAvailable == true) Color(0xFF10B981) else BirizTurquoise,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp)
                            ) {
                                Icon(
                                    imageVector = if (checkedUpdateInfo?.isUpdateAvailable == true) Icons.Default.Download else Icons.Default.SystemUpdate,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (checkedUpdateInfo?.isUpdateAvailable == true) "İndir ve Kur" else "Güncelleme Ekranı",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // 5. ÇIKIŞ SEÇENEKLERİ PENCERESİ
        // =========================================================================
        ProfileMenuScreen.EXIT_OPTIONS -> {
            ProfileSubModalScaffold(
                title = "Çıkış Seçenekleri",
                subtitle = "Uygulamayı kapatabilir veya hesap oturumunuzu sonlandırabilirsiniz",
                isDarkTheme = isDarkTheme,
                onBackToMenu = { activeScreen = ProfileMenuScreen.DROPDOWN_MENU },
                onCloseAll = onDismissRequest
            ) {
                // Seçenek 1: Uygulamadan Çıkış
                Surface(
                    onClick = {
                        onDismissRequest()
                        exitAppGracefully(context)
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFF0F9FF),
                    border = BorderStroke(1.dp, if (isDarkTheme) BirizTurquoise.copy(alpha = 0.45f) else Color(0xFFBAE6FD)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = null,
                                tint = BirizTurquoise,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Uygulamadan Çıkış",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDarkTheme) Color.White else BirizNavyPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Hesap oturumunuz açık kalır, yalnızca uygulama kapatılır. Tekrar açtığınızda doğrudan kaldığınız yerden devam edersiniz.",
                            fontSize = 11.5.sp,
                            color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF475569)
                        )
                    }
                }

                // Seçenek 2: Hesabı Kapat Çık
                Surface(
                    onClick = {
                        onDismissRequest()
                        onLogout()
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDarkTheme) Color(0xFF2A1215) else Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF991B1B) else Color(0xFFFECACA)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Hesabı Kapat Çık",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Google ve BİRİZ hesap oturumunuz güvenli şekilde kapatılır ve giriş ekranına yönlendirilirsiniz.",
                            fontSize = 11.5.sp,
                            color = if (isDarkTheme) Color(0xFFFECACA) else Color(0xFF991B1B)
                        )
                    }
                }
            }
        }

        // =========================================================================
        // 6. ROL SİLME ONAY EKRANI (AYNI DİYALOG İÇİNDE)
        // =========================================================================
        ProfileMenuScreen.CONFIRM_DELETE_ROLE -> {
            ProfileSubModalScaffold(
                title = if (currentRole == UserRole.TEACHER) "Öğretmen Rolünü Sil" else "Veli Rolünü Sil",
                subtitle = "Onayladığınızda rolünüz sıfırlanır ve Rol Seçimine dönersiniz",
                isDarkTheme = isDarkTheme,
                onBackToMenu = { activeScreen = ProfileMenuScreen.PROFILE_INFO },
                onCloseAll = onDismissRequest
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDarkTheme) Color(0xFF2B1D12) else Color(0xFFFFF7ED),
                    border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF9A3412) else Color(0xFFFED7AA)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PersonRemove,
                                contentDescription = null,
                                tint = Color(0xFFEA580C),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (currentRole == UserRole.TEACHER) "⚠️ Öğretmen Rolü Silinecek" else "⚠️ Veli Rolü Silinecek",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.5.sp,
                                color = if (isDarkTheme) Color(0xFFFDBA74) else Color(0xFFC2410C)
                            )
                        }
                        Text(
                            text = if (currentRole == UserRole.TEACHER) {
                                "Öğretmen rolünüz ve oluşturduğunuz sınıf kalıcı olarak kaldırılır. Google oturumunuz açık kalır ve Rol Seçimi ekranına dönersiniz."
                            } else {
                                "Veli rolünüz ve bağlı tüm öğrenci bağlantılarınız kaldırılır. Google oturumunuz açık kalır ve Rol Seçimi ekranına dönersiniz."
                            },
                            fontSize = 12.5.sp,
                            color = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF475569)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { activeScreen = ProfileMenuScreen.PROFILE_INFO },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Vazgeç", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            onDismissRequest()
                            onDeleteRole()
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("confirm_delete_role_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Evet, Rolümü Sil",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // =========================================================================
        // 7. HESABIMI KALICI OLARAK SİLME ONAY EKRANI (AYNI DİYALOG İÇİNDE)
        // =========================================================================
        ProfileMenuScreen.CONFIRM_DELETE_ACCOUNT -> {
            ProfileSubModalScaffold(
                title = "Hesabımı Kalıcı Olarak Sil",
                subtitle = "Bu işlem geri alınamaz • Tüm bulut ve cihaz verileriniz temizlenir",
                isDarkTheme = isDarkTheme,
                onBackToMenu = { activeScreen = ProfileMenuScreen.PROFILE_INFO },
                onCloseAll = onDismissRequest
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDarkTheme) Color(0xFF2A1215) else Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF991B1B) else Color(0xFFFECACA)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "💥 Sistemdeki Tüm İzleriniz Silinecek",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.5.sp,
                                color = if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
                            )
                        }
                        Text(
                            text = if (currentRole == UserRole.TEACHER) {
                                "• Kullanıcı profiliniz, bildirim tokenınız ve Google/Firebase kimlik kaydınız\n• Sahip olduğunuz sınıf, öğrenciler, yıldızlar, kitaplık, turnuvalar, ders ve oturma planları\n• Duyuru, mesajlar ve aktif Akıllı Tahta oturumları\n• Cihazdaki tüm yerel BİRİZ dosyaları ve önbellek\n\nkalıcı olarak silinecektir."
                            } else {
                                "• Kullanıcı profiliniz, bildirim tokenınız ve Google/Firebase kimlik kaydınız\n• Tüm öğrenci bağlantılarınız, gönderdiğiniz veli mesajları ve sohbet geçmişiniz\n• Cihazdaki tüm yerel BİRİZ ayarları, bildirimler ve önbellek\n\nkalıcı olarak silinecektir."
                            },
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = if (isDarkTheme) Color(0xFFFECACA) else Color(0xFF7F1D1D)
                        )
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onDismissRequest()
                            onDeleteEntireAccount()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("confirm_delete_account_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Evet, Hesabımı ve Tüm Verilerimi Sil",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    OutlinedButton(
                        onClick = { activeScreen = ProfileMenuScreen.PROFILE_INFO },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Vazgeç ve Profilime Dön", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showParentQrDialog) {
        ParentDownloadQrDialog(
            isDarkTheme = isDarkTheme,
            onDismiss = { showParentQrDialog = false }
        )
    }
}

@Composable
private fun DropdownMenuActionItem(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    badgeText: String? = null,
    isDarkTheme: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = iconBg,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.padding(end = 6.dp)) {
                    Text(
                        text = title,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDarkTheme) Color.White else BirizNavyPrimary
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!badgeText.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = iconTint.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = iconTint,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
            }
        }
    }
}

@Composable
private fun ProfileSubModalScaffold(
    title: String,
    subtitle: String,
    isDarkTheme: Boolean,
    onBackToMenu: () -> Unit,
    onCloseAll: () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCloseAll,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("user_profile_menu_dialog"),
        shape = RoundedCornerShape(24.dp),
        containerColor = if (isDarkTheme) Color(0xFF0F172A) else Color.White,
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
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
                            onClick = onBackToMenu,
                            shape = CircleShape,
                            color = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("profile_submodal_back_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Menüye Dön",
                                    tint = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDarkTheme) Color.White else BirizNavyPrimary
                            )
                            Text(
                                text = subtitle,
                                fontSize = 11.sp,
                                color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                    }

                    IconButton(
                        onClick = onCloseAll,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = if (isDarkTheme) Color.LightGray else Color.Gray
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                content()
            }
        },
        confirmButton = {
            TextButton(onClick = onBackToMenu) {
                Text(
                    text = "← Açılır Menüye Dön",
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                )
            }
        }
    )
}

@Composable
private fun ProfileInfoKeyValueRow(
    label: String,
    value: String,
    isDarkTheme: Boolean,
    valueColor: Color? = null,
    onCopy: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor ?: if (isDarkTheme) Color.White else BirizNavyPrimary
            )
            if (onCopy != null) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Kopyala",
                    tint = BirizTurquoise,
                    modifier = Modifier
                        .size(14.dp)
                        .clickable { onCopy() }
                )
            }
        }
    }
}
