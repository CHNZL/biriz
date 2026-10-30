package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.model.ClassRoom
import com.example.ui.components.AppUpdateDialog
import com.example.ui.components.BentoCard
import com.example.ui.components.ParentDownloadQrDialog
import com.example.util.AppUpdateManager
import com.example.ui.theme.BirizAmber
import com.example.ui.theme.BirizBorder
import com.example.ui.theme.BirizDarkCardBorder
import com.example.ui.theme.BirizDarkTextMuted
import com.example.ui.theme.BirizDarkTextPrimary
import com.example.ui.theme.BirizDarkTextSecondary
import com.example.ui.theme.BirizError
import com.example.ui.theme.BirizNavyDark
import com.example.ui.theme.BirizNavyPrimary
import com.example.ui.theme.BirizSuccess
import com.example.ui.theme.BirizTextPrimary
import com.example.ui.theme.BirizTextSecondary
import com.example.ui.theme.BirizTurquoise

@Composable
fun ProfileSettingsSubScreen(
    classRoom: ClassRoom,
    studentCount: Int,
    isDarkTheme: Boolean,
    onDeleteTeacherRoleAndClass: () -> Unit,
    onDeleteEntireAccount: () -> Unit,
    onBackToClassList: () -> Unit,
    onCopyCode: (String) -> Unit
) {
    val context = LocalContext.current
    var showUpdateDialog by remember { mutableStateOf(false) }
    var showParentQrDialog by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Geri Dön Butonu
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onBackToClassList() }
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Geri",
                tint = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Sınıf Listesine Dön",
                fontWeight = FontWeight.Bold,
                color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                fontSize = 14.sp
            )
        }

        // 1. Profil ve Kimlik Bento Kartı
        BentoCard(
            isDark = isDarkTheme,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = if (isDarkTheme) Color(0x3528B8C2) else BirizNavyPrimary,
                    modifier = Modifier.size(52.dp),
                    border = BorderStroke(1.5.dp, BirizTurquoise)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = classRoom.teacherName.take(1).uppercase(),
                            color = if (isDarkTheme) BirizTurquoise else Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = classRoom.teacherName,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                    )
                    Text(
                        text = classRoom.teacherEmail,
                        fontSize = 13.sp,
                        color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = BirizTurquoise.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Yönetici Sınıf Öğretmeni • Aktif",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = BirizTurquoise,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            ProfileDetailRow(
                label = "Yetki Düzeyi",
                value = "Sınıf Sahibi & KVKK Veri Sorumlusu",
                isDark = isDarkTheme
            )
        }

        // 2. Okul ve Sınıf Detay Bento Kartı
        BentoCard(
            isDark = isDarkTheme,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tanımlı Okul ve Şube",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            ProfileDetailRow("Okul Adı", classRoom.schoolName, isDark = isDarkTheme)
            ProfileDetailRow("İl / İlçe", "${classRoom.cityName} / ${classRoom.districtName}", isDark = isDarkTheme)
            ProfileDetailRow("Sınıf ve Şube", "${classRoom.gradeLevel}/${classRoom.section} Şubesi", isDark = isDarkTheme)
            ProfileDetailRow("Kayıtlı Öğrenci", "$studentCount Öğrenci", isDark = isDarkTheme)
        }

        // 3. UYGULAMA SÜRÜMÜ VE OTOMATİK GÜNCELLEME (APK & GITHUB) BENTO KARTI
        BentoCard(
            isDark = isDarkTheme,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = BirizTurquoise,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Uygulama Sürümü & Güncelleme",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = BirizTurquoise.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "v${BuildConfig.VERSION_NAME}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BirizTurquoise,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            ProfileDetailRow(label = "Uygulama Adı", value = "BİRİZ", isDark = isDarkTheme)
            ProfileDetailRow(label = "Kurulu Sürüm", value = "v${BuildConfig.VERSION_NAME} (#${BuildConfig.VERSION_CODE})", isDark = isDarkTheme)
            
            val currentRepo = AppUpdateManager.getGitHubRepo(context).ifEmpty { "Otomatik / Yapılandırılmadı" }
            ProfileDetailRow(label = "GitHub Deposu", value = currentRepo, isDark = isDarkTheme)

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "GitHub'a her kod senkronizasyonu yaptığınızda otomatik olarak yeni bir Biriz.apk derlenir. Aşağıdaki butona dokunarak veya uygulamayı açtığınızda en yeni sürüme tek tıkla doğrudan güncelleyebilirsiniz.",
                fontSize = 11.5.sp,
                color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { showUpdateDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("check_updates_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BirizTurquoise,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "🚀 Sürümü ve Güncellemeleri Denetle",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                }

                OutlinedButton(
                    onClick = { showParentQrDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("show_parent_qr_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.2.dp, if (isDarkTheme) BirizTurquoise else BirizNavyPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = null,
                        tint = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "📲 Veli İndirme Karekodu ve Bağlantısı",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                    )
                }
            }
        }

        // 4. TEHLİKELİ ALAN & CASCADE VERİ SİLME BENTO KARTLARI
        BentoCard(
            isDark = isDarkTheme,
            glowColor = BirizAmber.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PersonRemove,
                    contentDescription = null,
                    tint = BirizAmber,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Öğretmen Rolümü ve Sınıfımı Sil",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = BirizAmber
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Bu işlem '${classRoom.schoolName}' ${classRoom.gradeLevel}/${classRoom.section} sınıfınızı ve tüm öğrencilerini veritabanından kalıcı olarak temizler. Rolünüz 'Üye' statüsüne döner ve şube diğer öğretmenlerin seçimine açılır.",
                fontSize = 12.sp,
                color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onDeleteTeacherRoleAndClass,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("delete_teacher_role_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD97706),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PersonRemove,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Öğretmen Rolümü ve Sınıfımı Sil", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        // 4. Hesabı Tamamen Sil Kartı
        BentoCard(
            isDark = isDarkTheme,
            glowColor = BirizError.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = BirizError,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Hesabımı Tamamen Sil",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = BirizError
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "KVKK gereği tüm hesap bilgileriniz, kimlik kayıtlarınız, sınıfınız ve tüm öğrenci listeleriniz veritabanından kalıcı olarak silinir. Bu işlem geri alınamaz.",
                fontSize = 12.sp,
                color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onDeleteEntireAccount,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("delete_entire_account_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BirizError,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Hesabımı Kalıcı Olarak Sil", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(80.dp)) // Floating dock payı
    }

    if (showUpdateDialog) {
        AppUpdateDialog(
            isDarkTheme = isDarkTheme,
            onDismiss = { showUpdateDialog = false }
        )
    }

    if (showParentQrDialog) {
        ParentDownloadQrDialog(
            isDarkTheme = isDarkTheme,
            onDismiss = { showParentQrDialog = false }
        )
    }
}

@Composable
private fun ProfileDetailRow(label: String, value: String, isDark: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = if (isDark) BirizDarkTextSecondary else BirizTextSecondary
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) BirizDarkTextPrimary else BirizNavyPrimary
        )
    }
}
