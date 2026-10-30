package com.example.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.ui.components.BentoCard
import com.example.ui.components.BirizOfficialLogo
import com.example.ui.components.KvkkDialog
import com.example.ui.components.ThemeToggleIconButton
import com.example.ui.theme.BirizAmber
import com.example.ui.theme.BirizBorder
import com.example.ui.theme.BirizDarkBgBottom
import com.example.ui.theme.BirizDarkBgTop
import com.example.ui.theme.BirizDarkTextPrimary
import com.example.ui.theme.BirizDarkTextSecondary
import com.example.ui.theme.BirizNavyDark
import com.example.ui.theme.BirizNavyLight
import com.example.ui.theme.BirizNavyPrimary
import com.example.ui.theme.BirizSuccess
import com.example.ui.theme.BirizTextPrimary
import com.example.ui.theme.BirizTextSecondary
import com.example.ui.theme.BirizTurquoise
import com.example.ui.theme.BirizTurquoiseSoft

@Composable
fun RoleSelectionScreen(
    currentRole: UserRole,
    isKvkkAccepted: Boolean,
    isKvkkDialogVisible: Boolean,
    onKvkkCheckedChanged: (Boolean) -> Unit,
    onOpenKvkkDialog: () -> Unit,
    onCloseKvkkDialog: () -> Unit,
    onSelectTeacher: () -> Unit,
    onSelectParentAttempt: () -> Unit,
    currentUser: com.example.ui.viewmodel.AuthenticatedUser = com.example.ui.viewmodel.AuthenticatedUser(),
    onLogout: () -> Unit = {},
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {}
) {
    if (isKvkkDialogVisible) {
        KvkkDialog(
            onDismissRequest = onCloseKvkkDialog,
            onAcceptAndClose = {
                onKvkkCheckedChanged(true)
                onCloseKvkkDialog()
            },
            isDarkTheme = isDarkTheme
        )
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
                Color(0xFFEDF2F7)
            )
        )
    }

    val textColorPrimary = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
    val textColorSecondary = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        // Üst Bar & Logo Header (Sabit, dokunma engellenemez)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = if (isDarkTheme) Color(0xD00D1B2A) else BirizNavyPrimary,
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BirizOfficialLogo(size = 44.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "BİRİZ",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Rol Seçim Portali",
                            fontSize = 11.sp,
                            color = BirizTurquoise,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    ThemeToggleIconButton(
                        isDark = isDarkTheme,
                        onToggle = onToggleTheme
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("role_screen_logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Çıkış Yap / Hesap Değiştir",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Hoş Geldiniz Başlığı
            Text(
                text = "Hoş Geldiniz 👋",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColorPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "İlkokul sınıfınızı dijital dünyaya taşıyın, velilerinizle güvenli bir köprü kurun.",
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = textColorSecondary
            )

            // Aktif Giriş Yapan Hesap ve Hesap Değiştirme Rozeti
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isDarkTheme) Color(0x3513294B) else Color(0x1513294B),
                border = BorderStroke(1.dp, if (isDarkTheme) Color(0x6028B8C2) else Color(0x2013294B)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = BirizTurquoise,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = currentUser.avatarInitials,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = BirizNavyDark
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = currentUser.fullName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = textColorPrimary
                            )
                            if (currentUser.email.isNotBlank()) {
                                Text(
                                    text = currentUser.email,
                                    fontSize = 11.sp,
                                    color = textColorSecondary
                                )
                            }
                        }
                    }
                    TextButton(
                        onClick = onLogout,
                        modifier = Modifier.testTag("switch_account_button")
                    ) {
                        Text(
                            text = "Hesap Değiştir",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) BirizTurquoise else BirizAmber
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ZORUNLU KVKK KARTI (Bento Stili)
            BentoCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("kvkk_card"),
                isDark = isDarkTheme,
                glowColor = if (isKvkkAccepted) BirizSuccess else BirizAmber,
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (isKvkkAccepted) BirizSuccess else BirizAmber,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "KVKK & Açık Rıza Güvencesi",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = textColorPrimary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isKvkkAccepted,
                        onCheckedChange = onKvkkCheckedChanged,
                        colors = CheckboxDefaults.colors(
                            checkedColor = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                            uncheckedColor = textColorSecondary
                        ),
                        modifier = Modifier.testTag("kvkk_checkbox")
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "KVKK Aydınlatma Metnini okudum ve kabul ediyorum.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColorPrimary
                        )
                        Text(
                            text = "Aydınlatma Metnini İncele →",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) BirizTurquoise else BirizAmber,
                            modifier = Modifier
                                .clickable { onOpenKvkkDialog() }
                                .padding(vertical = 4.dp)
                                .testTag("open_kvkk_details_button")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ROL SEÇİMİ (BENTO GRID - YAN YANA İKİ KART)
            Text(
                text = "Rolünüzü Seçin",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = textColorPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. ÖĞRETMEN KARTI (AKTİF)
                BentoCard(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("teacher_role_card"),
                    isDark = isDarkTheme,
                    glowColor = if (isKvkkAccepted) BirizTurquoise else null,
                    shape = RoundedCornerShape(24.dp),
                    onClick = if (isKvkkAccepted) onSelectTeacher else null
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            shape = CircleShape,
                            color = if (isDarkTheme) Color(0x3528B8C2) else BirizTurquoiseSoft,
                            modifier = Modifier.size(56.dp),
                            border = BorderStroke(1.5.dp, BirizTurquoise)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = "Öğretmen",
                                    tint = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Öğretmen",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = textColorPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sınıf Yöneticisi",
                            fontSize = 11.sp,
                            color = textColorSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BirizTurquoise
                        ) {
                            Text(
                                text = "Aktif & Hazır",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = BirizNavyDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // 2. VELİ KARTI (PASİF & BUZLU CAM KİLİTLİ)
                BentoCard(
                    modifier = Modifier
                        .weight(1f)
                        .alpha(0.70f)
                        .testTag("parent_role_card"),
                    isDark = isDarkTheme,
                    glowColor = if (isDarkTheme) Color(0x3064748B) else Color(0x2013294B),
                    shape = RoundedCornerShape(24.dp),
                    onClick = onSelectParentAttempt
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            shape = CircleShape,
                            color = if (isDarkTheme) Color(0x25E67E22) else Color(0xFFFFF3E0),
                            modifier = Modifier.size(56.dp),
                            border = BorderStroke(1.5.dp, BirizAmber.copy(alpha = 0.5f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Veli Kilitli",
                                    tint = BirizAmber,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Veli",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = textColorSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Öğrenci Takibi",
                            fontSize = 11.sp,
                            color = textColorSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDarkTheme) Color(0x3564748B) else Color(0xFFE2E8F0)
                        ) {
                            Text(
                                text = "Yakında!",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) Color(0xFFE2E8F0) else Color(0xFF334155),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // DEVAM ET BUTONU
            Button(
                onClick = onSelectTeacher,
                enabled = isKvkkAccepted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .shadow(
                        elevation = if (isKvkkAccepted) 10.dp else 0.dp,
                        shape = RoundedCornerShape(26.dp),
                        spotColor = if (isDarkTheme) BirizTurquoise else Color(0x3013294B)
                    )
                    .testTag("select_teacher_button"),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                    contentColor = if (isDarkTheme) BirizNavyDark else Color.White,
                    disabledContainerColor = if (isDarkTheme) Color(0x35334E68) else Color(0xFFE2E8F0),
                    disabledContentColor = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF475569)
                ),
                border = BorderStroke(
                    1.5.dp,
                    if (isKvkkAccepted) BirizTurquoise else Color.Transparent
                )
            ) {
                val continueContentColor = if (isKvkkAccepted) {
                    if (isDarkTheme) BirizNavyDark else Color.White
                } else {
                    if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF475569)
                }
                Text(
                    text = if (isKvkkAccepted) "Öğretmen Olarak Kuruluma Başla" else "Önce KVKK Onayı Zorunludur",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = continueContentColor
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = continueContentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
