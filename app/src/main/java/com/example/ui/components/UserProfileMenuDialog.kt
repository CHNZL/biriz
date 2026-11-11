package com.example.ui.components

import com.example.data.model.UserRole
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.example.BuildConfig
import com.example.data.model.ClassRoom
import com.example.data.model.Student
import com.example.ui.theme.BirizAmber
import com.example.ui.theme.BirizDarkCardBorder
import com.example.ui.theme.BirizNavyPrimary
import com.example.ui.theme.BirizTurquoise
import com.example.ui.viewmodel.AuthenticatedUser

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
        border = BorderStroke(1.dp, if (isDarkTheme) Color(0x4064748B) else Color(0x30FFFFFF))
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
                        Surface(
                            shape = CircleShape,
                            color = BirizAmber,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = user.avatarInitials.ifBlank { "CÖ" },
                                    fontSize = (size.value * 0.4).sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                    },
                    error = {
                        Surface(
                            shape = CircleShape,
                            color = BirizAmber,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = user.avatarInitials.ifBlank { "CÖ" },
                                    fontSize = (size.value * 0.4).sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                    }
                )
            } else {
                Surface(
                    shape = CircleShape,
                    color = BirizAmber,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = user.avatarInitials.ifBlank { "CÖ" },
                            fontSize = (size.value * 0.4).sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            }
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
    onToggleTheme: () -> Unit,
    onDeleteRole: () -> Unit,
    onDeleteEntireAccount: () -> Unit,
    onCheckUpdate: () -> Unit = {},
    onLogout: () -> Unit,
    onDismissRequest: () -> Unit
) {
    var showDeleteRoleConfirm by remember { mutableStateOf(false) }
    var showDeleteAccountConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("user_profile_menu_dialog"),
        shape = RoundedCornerShape(24.dp),
        containerColor = if (isDarkTheme) Color(0xFF0F172A) else Color.White,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = BirizAmber,
                        modifier = Modifier.size(38.dp)
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
                                    text = currentUser.avatarInitials,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = currentUser.fullName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDarkTheme) Color.White else BirizNavyPrimary
                        )
                        Text(
                            text = if (currentRole == UserRole.TEACHER) "👨‍🏫 Sınıf Öğretmeni" else "👨‍👩‍👧‍👦 Öğrenci Velisi",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = BirizTurquoise
                        )
                    }
                }

                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Kapat",
                        tint = if (isDarkTheme) Color.LightGray else Color.Gray
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // --- 1. AÇIK / KOYU TEMA SEÇENEĞİ ---
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = if (isDarkTheme) BirizAmber else Color(0xFFD97706),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isDarkTheme) "🌙 Koyu Tema Aktif" else "☀️ Açık Tema Aktif",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkTheme) Color.White else BirizNavyPrimary
                                )
                                Text(
                                    text = "Uygulama görünüm temasını değiştir",
                                    fontSize = 10.5.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { onToggleTheme() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = BirizAmber,
                                uncheckedThumbColor = BirizNavyPrimary,
                                uncheckedTrackColor = Color(0xFFCBD5E1)
                            )
                        )
                    }
                }

                // --- 2. PROFİL BİLGİLERİ VE KUTULARI ---
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "📋 Profil & Tanımlı Bilgiler",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                    )

                    // 2a. Kullanıcı Profil Kutusu
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFFCBD5E1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = BirizAmber, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (currentUser.fullName == "Öğretmen" && currentUser.email.isNotBlank()) {
                                        currentUser.email.substringBefore("@").replace(".", " ").split(" ")
                                            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }
                                    } else currentUser.fullName,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkTheme) Color.White else BirizNavyPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Email, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = currentUser.email.ifBlank { "cihan.ozel10@gmail.com" },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF475569)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Google Hesabı Bağlı • KVKK Onaylı Güvenli Oturum", fontSize = 11.sp, color = Color(0xFF10B981), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // 2b. Tanımlı Okul / Şube Kutusu
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFFBFDBFE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.School, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (currentRole == UserRole.TEACHER) "🏫 Tanımlı Öğretmen Sınıfı" else "👧 Tanımlı Öğrenciler",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0284C7)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            if (currentRole == UserRole.TEACHER) {
                                if (classRoom != null) {
                                    Text(
                                        text = "${classRoom.cityName} / ${classRoom.districtName} - ${classRoom.schoolName}",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDarkTheme) Color.White else BirizNavyPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${classRoom.gradeLevel}/${classRoom.section} Sınıfı",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.Gray
                                    )
                                } else {
                                    Text("Henüz tanımlı sınıfınız bulunmamaktadır.", fontSize = 11.5.sp, color = Color.Gray)
                                }
                            } else {
                                if (linkedStudents.isNotEmpty()) {
                                    linkedStudents.forEach { s ->
                                        Text(
                                            text = "• ${s.fullName} (${s.studentNumber}) - ${classRoom?.let { "${it.gradeLevel}/${it.section}" } ?: "Sınıf"}",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                } else {
                                    Text("Henüz bağlı öğrenciniz bulunmamaktadır.", fontSize = 11.5.sp, color = Color.Gray)
                                }
                            }
                        }
                    }

                    if (currentRole == UserRole.PARENT) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDarkTheme) Color(0x3010B981) else Color(0xFFF0FDF4),
                            border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF10B981) else Color(0xFFBBF7D0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "BİRİZ Veri Güvenliği: Yalnızca bağlı olduğunuz öğrencinizin verileri görüntülenir. Sınıftaki diğer öğrencilerin kimlik bilgileri KVKK ve gizlilik standartlarına uygun olarak korunur.",
                                    fontSize = 10.5.sp,
                                    lineHeight = 14.5.sp,
                                    color = if (isDarkTheme) Color(0xFF86EFAC) else Color(0xFF166534)
                                )
                            }
                        }
                    }

                    // 2c. Rol Silme Kutusu
                    Surface(
                        onClick = { showDeleteRoleConfirm = true },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFFF7ED),
                        border = BorderStroke(1.dp, Color(0xFFFFEDD5)),
                        modifier = Modifier.fillMaxWidth()
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
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (currentRole == UserRole.TEACHER) "Öğretmen Rolünü ve Sınıfı Sil" else "Veli Rolünü ve Bağlantıları Sil",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFC2410C)
                                    )
                                    Text(
                                        text = "Rolünüz sıfırlanır, tekrar Rol Seçimi ekranına dönersiniz.",
                                        fontSize = 10.5.sp,
                                        color = Color(0xFF9A3412)
                                    )
                                }
                            }
                            Text("Sil ❯", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFEA580C))
                        }
                    }

                    // 2d. Hesabı Sil Kutusu
                    Surface(
                        onClick = { showDeleteAccountConfirm = true },
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFEE2E2)),
                        modifier = Modifier.fillMaxWidth()
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
                                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Hesabımı ve Tüm Verilerimi Sil",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB91C1C)
                                    )
                                    Text(
                                        text = "Veritabanındaki tüm kayıtlarınız kalıcı olarak silinir.",
                                        fontSize = 10.5.sp,
                                        color = Color(0xFF991B1B)
                                    )
                                }
                            }
                            Text("Sil ❯", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFDC2626))
                        }
                    }
                }

                // --- 3. SÜRÜM BİLGİSİ VE GÜNCELLEME KUTUSU ---
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "📱 Uygulama Sürümü",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) Color.White else BirizNavyPrimary
                            )
                            Text(
                                text = "BİRİZ Mobil v${BuildConfig.VERSION_NAME} (#${BuildConfig.VERSION_CODE})",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                onCheckUpdate()
                                onDismissRequest()
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Güncelleme Denetle", fontSize = 11.sp)
                        }
                    }
                }

                // --- 4. HESAPTAN ÇIKIŞ KUTUSU ---
                Button(
                    onClick = {
                        onDismissRequest()
                        onLogout()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hesaptan Çıkış Yap", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                }
            }
        },
        confirmButton = {}
    )

    // --- ROL SİLME ONAY DİYALOĞU ---
    if (showDeleteRoleConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteRoleConfirm = false },
            title = { Text("⚠️ Rolünüzü Silmek İstediğinize Emin Misiniz?", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Text(
                    text = if (currentRole == UserRole.TEACHER) {
                        "Öğretmen rolünüzü ve oluşturduğunuz sınıfı sildiğinizde Üye rolüne geçeceksiniz. Dilerseniz tekrar Veli veya Öğretmen rolünü seçebilirsiniz."
                    } else {
                        "Veli rolünüzü ve bağlı tüm öğrencilerinizi sildiğinizde Üye rolüne geçeceksiniz. Dilerseniz tekrar Veli veya Öğretmen rolünü seçebilirsiniz."
                    },
                    fontSize = 12.5.sp,
                    color = Color.Gray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteRoleConfirm = false
                        onDismissRequest()
                        onDeleteRole()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEA580C)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Evet, Rolümü Sil", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteRoleConfirm = false }) { Text("Vazgeç") }
            }
        )
    }

    // --- HESAP SİLME ONAY DİYALOĞU ---
    if (showDeleteAccountConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountConfirm = false },
            title = { Text("💥 Hesabınızı Kalıcı Olarak Silin", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFDC2626)) },
            text = {
                Text(
                    text = "Bu işlem geri alınamaz! Veritabanındaki hesabınız, verileriniz ve tüm bağlı kayıtlar tamamen silinecektir.",
                    fontSize = 12.5.sp,
                    color = Color.Gray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAccountConfirm = false
                        onDismissRequest()
                        onDeleteEntireAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Evet, Hesabımı Sil", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountConfirm = false }) { Text("Vazgeç") }
            }
        )
    }
}
