package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.ui.components.BentoCard
import com.example.ui.components.BirizOfficialLogo
import com.example.ui.components.KvkkDialog
import com.example.ui.components.TermsOfServiceDialog
import com.example.ui.components.ThemeToggleIconButton
import com.example.ui.theme.BirizAmber
import com.example.ui.theme.BirizDarkBgBottom
import com.example.ui.theme.BirizDarkBgTop
import com.example.ui.theme.BirizDarkTextPrimary
import com.example.ui.theme.BirizDarkTextSecondary
import com.example.ui.theme.BirizNavyDark
import com.example.ui.theme.BirizNavyPrimary
import com.example.ui.theme.BirizSuccess
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
    onDeleteEntireAccount: () -> Unit = {},
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {}
) {
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            containerColor = if (isDarkTheme) Color(0xFF0F172A) else Color.White,
            title = {
                Text(
                    text = "Hesabımı Kalıcı Olarak Sil",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = Color(0xFFDC2626)
                )
            },
            text = {
                Text(
                    text = "BİRİZ hesabınız, Firebase Authentication kaydınız ve Firestore 'users' koleksiyonundaki tüm profil bilgileriniz kalıcı olarak silinecektir. Bu işlem geri alınamaz. Devam etmek istiyor musunuz?",
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAccountDialog = false
                        onDeleteEntireAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    modifier = Modifier.testTag("confirm_role_screen_delete_account_button")
                ) {
                    Text("Evet, Kalıcı Olarak Sil", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text(
                        "Vazgeç",
                        color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        )
    }

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

    if (showTermsDialog) {
        TermsOfServiceDialog(
            onDismissRequest = { showTermsDialog = false },
            onAcceptAndClose = {
                onKvkkCheckedChanged(true)
                showTermsDialog = false
            },
            isDarkTheme = isDarkTheme,
            isReadOnly = false
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
        // Üst Bar & Logo Header
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

            Spacer(modifier = Modifier.height(8.dp))

            // ZORUNLU KVKK & KULLANIM ŞARTLARI KARTI (Her iki rol için de zorunlu)
            BentoCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("kvkk_card"),
                isDark = isDarkTheme,
                glowColor = if (isKvkkAccepted) BirizSuccess else BirizAmber,
                shape = RoundedCornerShape(20.dp)
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
                        Icon(
                            imageVector = if (isKvkkAccepted) Icons.Default.CheckCircle else Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (isKvkkAccepted) BirizSuccess else BirizAmber,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "KVKK & Kullanım Şartları Güvencesi",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = textColorPrimary
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isKvkkAccepted) BirizSuccess.copy(alpha = 0.16f) else BirizAmber.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = if (isKvkkAccepted) "Onaylandı" else "Zorunlu",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isKvkkAccepted) BirizSuccess else BirizAmber,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onKvkkCheckedChanged(!isKvkkAccepted) },
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
                    Text(
                        text = "KVKK Aydınlatma Metnini ve BİRİZ Kullanım Şartlarını okudum, anladım ve kabul ediyorum.",
                        fontSize = 12.2.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColorPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // KVKK ve Kullanım Şartları İnceleme Butonları
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDarkTheme) Color(0x3013294B) else Color(0xFFF1F5F9),
                        border = BorderStroke(
                            1.dp,
                            if (isDarkTheme) BirizTurquoise.copy(alpha = 0.4f) else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenKvkkDialog() }
                            .testTag("open_kvkk_details_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "KVKK Metni →",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDarkTheme) Color(0x3013294B) else Color(0xFFF1F5F9),
                        border = BorderStroke(
                            1.dp,
                            if (isDarkTheme) BirizTurquoise.copy(alpha = 0.4f) else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showTermsDialog = true }
                            .testTag("open_terms_details_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Kullanım Şartları →",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                            )
                        }
                    }
                }

                if (!isKvkkAccepted) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚠️ Öğretmen veya Veli rolünü seçebilmek için yukarıdaki onay kutusunu işaretlemeniz zorunludur.",
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BirizAmber
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // ROL SEÇİMİ (BENTO GRID - YAN YANA İKİ KART, HER İKİSİ DE KVKK ONAYINA BAĞLI)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Rolünüzü Seçin",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColorPrimary
                )
                Text(
                    text = if (isKvkkAccepted) "Bir role dokunarak başlayın" else "Önce KVKK ve Şartları onaylayın",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isKvkkAccepted) BirizSuccess else BirizAmber
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. ÖĞRETMEN KARTI (KVKK Onayı Zorunlu)
                BentoCard(
                    modifier = Modifier
                        .weight(1f)
                        .alpha(if (isKvkkAccepted) 1f else 0.62f)
                        .testTag("teacher_role_card"),
                    isDark = isDarkTheme,
                    glowColor = if (isKvkkAccepted) BirizTurquoise else null,
                    shape = RoundedCornerShape(24.dp),
                    onClick = onSelectTeacher
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isKvkkAccepted) {
                                if (isDarkTheme) Color(0x3528B8C2) else BirizTurquoiseSoft
                            } else {
                                if (isDarkTheme) Color(0x2564748B) else Color(0xFFF1F5F9)
                            },
                            modifier = Modifier.size(56.dp),
                            border = BorderStroke(
                                1.5.dp,
                                if (isKvkkAccepted) BirizTurquoise else BirizAmber.copy(alpha = 0.55f)
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isKvkkAccepted) Icons.Default.School else Icons.Default.Lock,
                                    contentDescription = "Öğretmen",
                                    tint = if (isKvkkAccepted) {
                                        if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                                    } else {
                                        BirizAmber
                                    },
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Öğretmen",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = if (isKvkkAccepted) textColorPrimary else textColorSecondary
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
                            color = if (isKvkkAccepted) {
                                BirizTurquoise
                            } else {
                                if (isDarkTheme) Color(0x3564748B) else Color(0xFFE2E8F0)
                            }
                        ) {
                            Text(
                                text = if (isKvkkAccepted) "Aktif & Hazır" else "Onay Zorunlu 🔒",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isKvkkAccepted) {
                                    BirizNavyDark
                                } else {
                                    if (isDarkTheme) Color(0xFFE2E8F0) else Color(0xFF475569)
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // 2. VELİ KARTI (KVKK Onayı Zorunlu)
                BentoCard(
                    modifier = Modifier
                        .weight(1f)
                        .alpha(if (isKvkkAccepted) 1f else 0.62f)
                        .testTag("parent_role_card"),
                    isDark = isDarkTheme,
                    glowColor = if (isKvkkAccepted) BirizAmber else null,
                    shape = RoundedCornerShape(24.dp),
                    onClick = onSelectParentAttempt
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isKvkkAccepted) {
                                if (isDarkTheme) Color(0x28E67E22) else Color(0xFFFFF3E0)
                            } else {
                                if (isDarkTheme) Color(0x2564748B) else Color(0xFFF1F5F9)
                            },
                            modifier = Modifier.size(56.dp),
                            border = BorderStroke(
                                1.5.dp,
                                if (isKvkkAccepted) BirizAmber else BirizAmber.copy(alpha = 0.55f)
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isKvkkAccepted) Icons.Default.FamilyRestroom else Icons.Default.Lock,
                                    contentDescription = "Veli",
                                    tint = BirizAmber,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Veli",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = if (isKvkkAccepted) textColorPrimary else textColorSecondary
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
                            color = if (isKvkkAccepted) {
                                BirizAmber
                            } else {
                                if (isDarkTheme) Color(0x3564748B) else Color(0xFFE2E8F0)
                            }
                        ) {
                            Text(
                                text = if (isKvkkAccepted) "Aktif & Hazır" else "Onay Zorunlu 🔒",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isKvkkAccepted) {
                                    Color.White
                                } else {
                                    if (isDarkTheme) Color(0xFFE2E8F0) else Color(0xFF475569)
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // DEVAM ET BUTONLARI (Her ikisi de KVKK ve Kullanım Şartları onayına bağlıdır)
            Button(
                onClick = onSelectTeacher,
                enabled = isKvkkAccepted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .shadow(
                        elevation = if (isKvkkAccepted) 8.dp else 0.dp,
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
                Icon(
                    imageVector = if (isKvkkAccepted) Icons.Default.School else Icons.Default.Lock,
                    contentDescription = null,
                    tint = continueContentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isKvkkAccepted) {
                        "Öğretmen Olarak Kuruluma Başla"
                    } else {
                        "Önce KVKK ve Kullanım Şartlarını Onaylayın"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
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

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onSelectParentAttempt,
                enabled = isKvkkAccepted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("select_parent_button"),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isKvkkAccepted) {
                        if (isDarkTheme) Color(0x22E67E22) else Color(0xFFFFF8F1)
                    } else {
                        Color.Transparent
                    },
                    contentColor = if (isKvkkAccepted) BirizAmber else textColorSecondary,
                    disabledContentColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
                ),
                border = BorderStroke(
                    1.5.dp,
                    if (isKvkkAccepted) BirizAmber else Color(0x4564748B)
                )
            ) {
                val parentContentColor = if (isKvkkAccepted) {
                    BirizAmber
                } else {
                    if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
                }
                Icon(
                    imageVector = if (isKvkkAccepted) Icons.Default.FamilyRestroom else Icons.Default.Lock,
                    contentDescription = null,
                    tint = parentContentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isKvkkAccepted) {
                        "Veli Olarak Öğrenciye Bağlan"
                    } else {
                        "Veli Girişi İçin KVKK ve Şartlar Onayı Zorunludur"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = parentContentColor,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TextButton(
                    onClick = { showDeleteAccountDialog = true },
                    modifier = Modifier.testTag("role_screen_delete_account_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = "Hesabımı Kalıcı Olarak Sil",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Hesabımı ve Verilerimi Kalıcı Olarak Sil",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                    )
                }
            }
        }
    }
}
