package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.BuildConfig
import com.example.auth.GoogleAuthHelper
import com.example.ui.components.AppUpdateDialog
import com.example.ui.components.BentoCard
import com.example.ui.components.BirizOfficialLogo
import com.example.ui.components.KvkkDialog
import com.example.ui.components.TermsOfServiceDialog
import com.example.ui.components.ThemeToggleIconButton
import com.example.ui.theme.BirizAmber
import com.example.ui.theme.BirizBorder
import com.example.ui.theme.BirizDarkBgBottom
import com.example.ui.theme.BirizDarkBgTop
import com.example.ui.theme.BirizDarkCardBorder
import com.example.ui.theme.BirizDarkCardGlass
import com.example.ui.theme.BirizDarkSurfaceGlass
import com.example.ui.theme.BirizDarkTextPrimary
import com.example.ui.theme.BirizDarkTextSecondary
import com.example.ui.theme.BirizError
import com.example.ui.theme.BirizNavyDark
import com.example.ui.theme.BirizNavyLight
import com.example.ui.theme.BirizNavyPrimary
import com.example.ui.theme.BirizSurface
import com.example.ui.theme.BirizTextMuted
import com.example.ui.theme.BirizTextPrimary
import com.example.ui.theme.BirizTextSecondary
import com.example.ui.theme.BirizTurquoise
import com.example.ui.theme.BirizTurquoiseSoft
import com.example.util.AppUpdateManager

@Composable
fun GoogleSignInScreen(
    onSignInSuccess: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onTestSignIn: () -> Unit = {},
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var showUpdateDialog by remember { mutableStateOf(false) }
    var hasUpdateAvailable by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showKvkkDialog by remember { mutableStateOf(false) }

    // Giriş yapmadan önce arka planda yeni GitHub sürümünü kontrol et
    LaunchedEffect(Unit) {
        try {
            val info = AppUpdateManager.checkForUpdates(context)
            if (info?.isUpdateAvailable == true) {
                hasUpdateAvailable = true
            }
        } catch (_: Exception) {}
    }

    val backgroundBrush = if (isDarkTheme) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF070E20),
                BirizDarkBgTop,
                Color(0xFF091228),
                BirizDarkBgBottom
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFFF8FAFC),
                Color(0xFFF1F5F9),
                Color(0xFFEEF2F6)
            )
        )
    }

    val textColorPrimary = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
    val textColorSecondary = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        // ÜST BAR: Sürüm Rozeti & Güncelleme Tuşu + Tema Değiştirici
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sürüm Rozeti ve Doğrudan Güncelleme Tuşu
            Surface(
                onClick = { showUpdateDialog = true },
                shape = RoundedCornerShape(14.dp),
                color = if (hasUpdateAvailable) {
                    BirizAmber.copy(alpha = 0.22f)
                } else if (isDarkTheme) {
                    Color(0x3528B8C2)
                } else {
                    Color(0x1813294B)
                },
                border = BorderStroke(
                    1.dp,
                    if (hasUpdateAvailable) BirizAmber
                    else if (isDarkTheme) Color(0x6028B8C2)
                    else Color(0x2513294B)
                ),
                modifier = Modifier.testTag("login_version_update_badge")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Uygulama Güncelle",
                        tint = if (hasUpdateAvailable) BirizAmber else if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "v${BuildConfig.VERSION_NAME}${if (hasUpdateAvailable) " • Güncelleme Var!" else " • Güncelle"}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (hasUpdateAvailable) BirizAmber else if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            ThemeToggleIconButton(
                isDark = isDarkTheme,
                onToggle = onToggleTheme
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 1. HERO ALANI: 3D Logo Amblemi & Işıltılı Halka
                Box(
                    modifier = Modifier
                        .size(124.dp)
                        .shadow(
                            elevation = if (isDarkTheme) 24.dp else 14.dp,
                            shape = CircleShape,
                            spotColor = if (isDarkTheme) BirizTurquoise else Color(0x3013294B)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .size(118.dp)
                            .testTag("biriz_logo_container"),
                        shape = CircleShape,
                        color = Color.White,
                        border = BorderStroke(
                            width = 3.dp,
                            brush = Brush.sweepGradient(
                                listOf(
                                    BirizNavyPrimary,
                                    BirizTurquoise,
                                    BirizAmber,
                                    BirizNavyPrimary
                                )
                            )
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            BirizOfficialLogo(
                                size = 104.dp,
                                modifier = Modifier.clip(CircleShape)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Marka Başlığı & Slogan
                Text(
                    text = "BİRİZ",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    color = textColorPrimary,
                    letterSpacing = 4.sp
                )

                Text(
                    text = "Birlikte Daha Güçlü",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) BirizTurquoise else BirizAmber,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDarkTheme) Color(0x2528B8C2) else Color(0x1513294B),
                    border = BorderStroke(1.dp, if (isDarkTheme) Color(0x4028B8C2) else Color(0x1813294B))
                ) {
                    Text(
                        text = "İlkokul Dijital Sınıf ve Veli Yönetim Sistemi",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColorSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))

                // Hata Bildirimi (Varsa)
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDarkTheme) Color(0x35EF4444) else Color(0xFFFEE2E2),
                        border = BorderStroke(1.dp, BirizError),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = if (isDarkTheme) Color(0xFFFCA5A5) else BirizError,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(14.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // 3. TEK VE ANA İŞLEM: GOOGLE İLE GİRİŞ YAP BUTONU
                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        GoogleAuthHelper.onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isLoading = false
                                onSignInSuccess()
                            },
                            onAuthError = { err ->
                                isLoading = false
                                errorMessage = err
                            },
                            scope = coroutineScope,
                            onAuthCancelled = {
                                isLoading = false
                            }
                        )
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(
                            elevation = if (isDarkTheme) 16.dp else 8.dp,
                            shape = RoundedCornerShape(28.dp),
                            spotColor = if (isDarkTheme) BirizTurquoise else Color(0x3013294B)
                        )
                        .testTag("google_sign_in_button"),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDarkTheme) Color(0xFF13294B) else BirizNavyPrimary,
                        contentColor = Color.White
                    ),
                    border = BorderStroke(
                        1.5.dp,
                        if (isDarkTheme) BirizTurquoise else Color(0x30FFFFFF)
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Google ile Güvenli Bağlanılıyor...",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp
                        )
                    } else {
                        // Google "G" Logosu
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "G",
                                    color = Color(0xFF4285F4),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Google ile Giriş Yap",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.5.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // GÜVENLİK VE YASAL BİLGİLENDİRME (ERİŞİLEBİLİR SÖZLEŞMELER)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Tıklanabilir Sözleşme Rozetleri / Butonları
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Kullanım Şartları Butonu
                        Surface(
                            onClick = { showTermsDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDarkTheme) Color(0x3013294B) else Color(0xFFF1F5F9),
                            border = BorderStroke(
                                1.dp,
                                if (isDarkTheme) BirizDarkCardBorder else BirizBorder
                            ),
                            modifier = Modifier.testTag("btn_open_terms_dialog")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Kullanım Şartları",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // 2. KVKK Sözleşmesi Butonu
                        Surface(
                            onClick = { showKvkkDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDarkTheme) Color(0x3013294B) else Color(0xFFF1F5F9),
                            border = BorderStroke(
                                1.dp,
                                if (isDarkTheme) BirizDarkCardBorder else BirizBorder
                            ),
                            modifier = Modifier.testTag("btn_open_kvkk_dialog")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "KVKK Sözleşmesi",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = if (isDarkTheme) BirizDarkTextSecondary else BirizTextMuted,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Giriş yaparak BİRİZ Kullanım Şartları ve KVKK Aydınlatma Metnini onaylamış sayılırsınız.",
                            fontSize = 10.5.sp,
                            color = textColorSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        if (showTermsDialog) {
            TermsOfServiceDialog(
                onDismissRequest = { showTermsDialog = false },
                isDarkTheme = isDarkTheme
            )
        }

        if (showKvkkDialog) {
            KvkkDialog(
                onDismissRequest = { showKvkkDialog = false },
                isDarkTheme = isDarkTheme,
                isReadOnly = true
            )
        }

        if (showUpdateDialog) {
            AppUpdateDialog(
                isDarkTheme = isDarkTheme,
                onDismiss = { showUpdateDialog = false }
            )
        }
    }
}
