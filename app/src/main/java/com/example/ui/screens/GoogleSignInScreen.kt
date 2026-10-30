package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.GoogleAuthHelper
import com.example.ui.components.BentoCard
import com.example.ui.components.BirizOfficialLogo
import com.example.ui.components.ThemeToggleIconButton
import com.example.ui.theme.BirizAmber
import com.example.ui.theme.BirizAmberLight
import com.example.ui.theme.BirizBorder
import com.example.ui.theme.BirizDarkBgBottom
import com.example.ui.theme.BirizDarkBgTop
import com.example.ui.theme.BirizDarkCardBorder
import com.example.ui.theme.BirizDarkCardGlass
import com.example.ui.theme.BirizDarkSurfaceGlass
import com.example.ui.theme.BirizDarkTextPrimary
import com.example.ui.theme.BirizDarkTextSecondary
import com.example.ui.theme.BirizError
import com.example.ui.theme.BirizGlowCyan
import com.example.ui.theme.BirizNavyDark
import com.example.ui.theme.BirizNavyLight
import com.example.ui.theme.BirizNavyPrimary
import com.example.ui.theme.BirizSurface
import com.example.ui.theme.BirizTextPrimary
import com.example.ui.theme.BirizTextSecondary
import com.example.ui.theme.BirizTurquoise
import com.example.ui.theme.BirizTurquoiseSoft

@Composable
fun GoogleSignInScreen(
    onSignInSuccess: () -> Unit,
    onTestSignIn: () -> Unit = {},
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var isTestLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

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
                Color(0xFFEDF2F7)
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
        // Üst Bar & Tema Değiştirici (Sabit, dokunma engellenemez)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDarkTheme) Color(0x3028B8C2) else Color(0x1513294B),
                border = BorderStroke(1.dp, if (isDarkTheme) Color(0x5028B8C2) else Color(0x2013294B))
            ) {
                Text(
                    text = "FAZ 1 • CANLI",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    letterSpacing = 1.sp
                )
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
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
            // 3D Logo Amblemi (Işıltılı Çerçeve)
            Box(
                modifier = Modifier
                    .size(116.dp)
                    .shadow(
                        elevation = if (isDarkTheme) 20.dp else 12.dp,
                        shape = CircleShape,
                        spotColor = if (isDarkTheme) BirizTurquoise else Color(0x2513294B)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .size(112.dp)
                        .testTag("biriz_logo_container"),
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(
                        width = 3.dp,
                        brush = Brush.sweepGradient(
                            listOf(BirizNavyPrimary, BirizTurquoise, BirizAmber, BirizNavyPrimary)
                        )
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        BirizOfficialLogo(
                            size = 100.dp,
                            modifier = Modifier.clip(CircleShape)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "BİRİZ",
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColorPrimary,
                letterSpacing = 3.sp
            )

            Text(
                text = "Birlikte Daha Güçlü",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDarkTheme) BirizTurquoise else BirizAmber,
                textAlign = TextAlign.Center
            )

            Text(
                text = "İlkokul Dijital Sınıf ve Veli Asistanı",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = textColorSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Rol Önizleme Bento Kartları (Görseldeki Gibi Yan Yana)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Rolünüzü Seçin",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColorPrimary,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Öğretmen Bento Kartı (Aktif & Işıltılı)
                    BentoCard(
                        modifier = Modifier.weight(1f),
                        isDark = isDarkTheme,
                        glowColor = BirizTurquoise,
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isDarkTheme) Color(0x3028B8C2) else BirizTurquoiseSoft,
                                modifier = Modifier.size(46.dp),
                                border = BorderStroke(1.dp, BirizTurquoise)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.School,
                                        contentDescription = null,
                                        tint = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Öğretmen",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = textColorPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = BirizTurquoise
                            ) {
                                Text(
                                    text = "Aktif Rol",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BirizNavyDark,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Veli Bento Kartı (Pasif / Buzlu Cam Kilitli)
                    BentoCard(
                        modifier = Modifier.weight(1f),
                        isDark = isDarkTheme,
                        glowColor = if (isDarkTheme) Color(0x2564748B) else Color(0x1513294B),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isDarkTheme) Color(0x20E67E22) else Color(0xFFFFF3E0),
                                modifier = Modifier.size(46.dp),
                                border = BorderStroke(1.dp, BirizAmber.copy(alpha = 0.5f))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = BirizAmber,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Veli",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = textColorSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isDarkTheme) Color(0x3564748B) else Color(0xFFE2E8F0)
                            ) {
                                Text(
                                    text = "Yakında!",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF334155),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Hata Mesajı
            if (errorMessage != null) {
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
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(14.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 1. BUTON: GOOGLE İLE GİRİŞ YAP (Pill & Parlamalı)
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
                enabled = !isLoading && !isTestLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .shadow(
                        elevation = if (isDarkTheme) 14.dp else 6.dp,
                        shape = RoundedCornerShape(26.dp),
                        spotColor = if (isDarkTheme) BirizTurquoise else Color(0x2013294B)
                    )
                    .testTag("google_sign_in_button"),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDarkTheme) Color(0xFF13294B) else BirizNavyPrimary,
                    contentColor = Color.White
                ),
                border = BorderStroke(
                    1.5.dp,
                    if (isDarkTheme) BirizTurquoise else Color(0x20FFFFFF)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Google ile Bağlanılıyor...",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "G",
                            color = Color(0xFF4285F4),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Google ile Giriş Yap",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. BUTON: ÖNİZLEME TEST MODU İLE BAŞLA
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(enabled = !isLoading && !isTestLoading) {
                        isTestLoading = true
                        onTestSignIn()
                    }
                    .testTag("preview_test_sign_in_button"),
                shape = RoundedCornerShape(20.dp),
                color = if (isDarkTheme) Color(0x25E67E22) else Color(0xFFFFF7ED),
                border = BorderStroke(
                    1.dp,
                    if (isDarkTheme) Color(0x70E67E22) else Color(0x50E67E22)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = BirizAmber,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Önizleme Test Modu ile Başla",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = BirizAmber
                            ) {
                                Text(
                                    text = "1 TIKLA",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "Google hesabı bağlamadan tüm sınıf sihirbazını test edin",
                            fontSize = 11.sp,
                            color = textColorSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Güvenlik & KVKK Bilgilendirme Kartı
            BentoCard(
                modifier = Modifier.fillMaxWidth(),
                isDark = isDarkTheme,
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "KVKK Uyumlu & Şifrelenmiş Bulut Altyapısı",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColorPrimary
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Giriş yaparak BİRİZ Kullanım Koşulları ve KVKK Açık Rıza Metnini kabul etmiş sayılırsınız.",
                    fontSize = 11.sp,
                    color = textColorSecondary,
                    lineHeight = 16.sp
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
}
