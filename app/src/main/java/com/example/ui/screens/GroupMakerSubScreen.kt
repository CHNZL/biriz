package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ClassRoom
import com.example.data.model.GroupColorTheme
import com.example.data.model.GroupDistributionConfig
import com.example.data.model.GroupDistributionMode
import com.example.data.model.GroupMakerState
import com.example.data.model.GroupTeam
import com.example.data.model.Student
import com.example.ui.theme.BirizAmber
import com.example.ui.theme.BirizBorder
import com.example.ui.theme.BirizDarkCardBorder
import com.example.ui.theme.BirizNavyDark
import com.example.ui.theme.BirizNavyPrimary
import com.example.ui.theme.BirizSuccess
import com.example.ui.theme.BirizTurquoise
import com.example.util.ExcelPdfHelper
import com.example.util.GroupMakerHelper
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GroupMakerSubScreen(
    classRoom: ClassRoom,
    students: List<Student>,
    groupMakerState: GroupMakerState,
    isDarkTheme: Boolean,
    onOpenWizard: () -> Unit,
    onCloseWizard: () -> Unit,
    onSaveConfig: (GroupDistributionConfig) -> Unit,
    onDistribute: (GroupDistributionConfig?) -> Unit,
    onReshuffle: () -> Unit,
    onDistributeUnassigned: () -> Unit,
    onPickRandomLeadersForAll: () -> Unit,
    onPickRandomLeaderForGroup: (String) -> Unit,
    onToggleLeader: (String, String) -> Unit,
    onRenameGroup: (String, String) -> Unit,
    onChangeGroupColor: (String, GroupColorTheme) -> Unit,
    onAddNewGroup: () -> Unit,
    onDeleteGroup: (String) -> Unit,
    onOpenSwapModal: (String) -> Unit,
    onCloseSwapModal: () -> Unit,
    onSwapStudents: (String, String) -> Unit,
    onMoveStudent: (String, String) -> Unit,
    onRemoveStudent: (String) -> Unit,
    onBackToClassList: () -> Unit
) {
    val context = LocalContext.current
    var isExportMenuOpen by remember { mutableStateOf(false) }
    var groupToRename by remember { mutableStateOf<GroupTeam?>(null) }
    var groupToChangeColor by remember { mutableStateOf<GroupTeam?>(null) }
    var showUnassignedDrawer by remember { mutableStateOf(false) }
    var selectedUnassignedStudentForGroup by remember { mutableStateOf<Student?>(null) }

    // Geri tuşu kontrolü
    BackHandler {
        onBackToClassList()
    }

    // Toplam İstatistikler
    val assignedCount = groupMakerState.groups.sumOf { it.studentIds.size }
    val unassignedCount = groupMakerState.unassignedStudentIds.size
    val totalStudents = students.size

    val activeSwapStudent = students.firstOrNull { it.id == groupMakerState.activeSwapStudentId }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp) // Alt dock payı
        ) {
            // 1. ÜST İSTATİSTİK & HIZLI EYLEM BARI
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isDarkTheme) Color(0x700E1A38) else Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Sol İstatistik Rozetleri
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDarkTheme) Color(0x350D9488) else Color(0xFFCCFBF1),
                                border = BorderStroke(1.dp, BirizTurquoise)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "👥 $assignedCount / $totalStudents Yerleşti",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkTheme) Color(0xFF5EEAD4) else Color(0xFF0F766E)
                                    )
                                }
                            }

                            if (unassignedCount > 0) {
                                Surface(
                                    onClick = { showUnassignedDrawer = true },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isDarkTheme) Color(0x35F59E0B) else Color(0xFFFEF3C7),
                                    border = BorderStroke(1.dp, BirizAmber)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = "📦 $unassignedCount Atanmamış",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDarkTheme) Color(0xFFFDE68A) else Color(0xFFB45309)
                                        )
                                    }
                                }
                            }
                        }

                        // Sağ Aksiyon Butonları (Dışa Aktar / Paylaş)
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Dışa Aktar Menüsü
                            Box {
                                Button(
                                    onClick = { isExportMenuOpen = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = BirizNavyPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("group_export_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Paylaş",
                                        modifier = Modifier.size(15.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Paylaş", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                DropdownMenu(
                                    expanded = isExportMenuOpen,
                                    onDismissRequest = { isExportMenuOpen = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("📋 WhatsApp / SMS Metni Kopyala", fontSize = 12.sp) },
                                        onClick = {
                                            isExportMenuOpen = false
                                            val shareText = GroupMakerHelper.generateShareText(classRoom, groupMakerState.groups, students)
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Grup Listesi", shareText))
                                            Toast.makeText(context, "Grup listesi panoya kopyalandı! 📋", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("📊 Excel (.xlsx) İndir / Paylaş", fontSize = 12.sp) },
                                        onClick = {
                                            isExportMenuOpen = false
                                            try {
                                                val uri = ExcelPdfHelper.exportGroupListExcel(context, classRoom, groupMakerState.groups, students)
                                                ExcelPdfHelper.shareFile(
                                                    context = context,
                                                    uri = uri,
                                                    mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                                    chooserTitle = "Grup Listesi Excel Dosyasını Paylaş"
                                                )
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("🖨️ PDF Krokisi İndir / Yazdır", fontSize = 12.sp) },
                                        onClick = {
                                            isExportMenuOpen = false
                                            try {
                                                val uri = ExcelPdfHelper.exportGroupListPdf(context, classRoom, groupMakerState.groups, students)
                                                ExcelPdfHelper.shareFile(
                                                    context = context,
                                                    uri = uri,
                                                    mimeType = "application/pdf",
                                                    chooserTitle = "Grup Listesi PDF Dosyasını Paylaş"
                                                )
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. YATAY ARAÇ ÇUBUĞU (Sihirbaz, Karıştır, Lider Seç, Yeni Grup)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onOpenWizard,
                            colors = ButtonDefaults.buttonColors(containerColor = BirizTurquoise),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("open_group_wizard_button")
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("✨ Dağıtım Sihirbazı", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onReshuffle,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("🎲 Yeniden Karıştır", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onPickRandomLeadersForAll,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("👑 Rastgele Sözcüler", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onAddNewGroup,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("➕ Grup Ekle", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        if (unassignedCount > 0) {
                            Button(
                                onClick = onDistributeUnassigned,
                                colors = ButtonDefaults.buttonColors(containerColor = BirizAmber),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("⚡ Kalanları Dağıt ($unassignedCount)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 3. ATANMAMIŞ ÖĞRENCİLER HAVUZU UYARI BANTI (Eğer varsa)
            if (unassignedCount > 0) {
                Surface(
                    onClick = { showUnassignedDrawer = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDarkTheme) Color(0x35F59E0B) else Color(0xFFFEF3C7),
                    border = BorderStroke(1.dp, BirizAmber)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "📦", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Atanmamış Öğrenciler Havuzu ($unassignedCount Kişi)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkTheme) Color(0xFFFDE68A) else Color(0xFF92400E)
                                )
                                Text(
                                    text = "Dokunarak listeyi açın veya kalanları gruplara dengeli paylaştırın",
                                    fontSize = 10.sp,
                                    color = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFFB45309)
                                )
                            }
                        }
                        Button(
                            onClick = onDistributeUnassigned,
                            colors = ButtonDefaults.buttonColors(containerColor = BirizAmber),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Dengeli Dağıt", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 4. GRUP KARTLARI LİSTESİ (Responsive Grid / Column)
            if (groupMakerState.groups.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "👥", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Henüz Grup Oluşturulmadı",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) Color.White else BirizNavyPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Yukarıdaki '✨ Dağıtım Sihirbazı' butonuna dokunarak sınıfınızı adil, dengeli ve eğlenceli çalışma gruplarına ayırın.",
                            fontSize = 12.sp,
                            color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.widthIn(max = 320.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onOpenWizard,
                            colors = ButtonDefaults.buttonColors(containerColor = BirizTurquoise),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Dağıtım Sihirbazını Başlat", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                ) {
                    items(groupMakerState.groups, key = { it.id }) { group ->
                        GroupCard(
                            group = group,
                            allStudents = students,
                            isDarkTheme = isDarkTheme,
                            onRenameClick = { groupToRename = group },
                            onChangeColorClick = { groupToChangeColor = group },
                            onPickLeaderClick = { onPickRandomLeaderForGroup(group.id) },
                            onDeleteClick = { onDeleteGroup(group.id) },
                            onStudentClick = { studentId -> onOpenSwapModal(studentId) },
                            onToggleLeaderClick = { studentId -> onToggleLeader(group.id, studentId) }
                        )
                    }
                }
            }
        }

        // --- 5. DİYALOGLAR VE MODALLAR ---

        // A. Dağıtım Sihirbazı Dialogu
        if (groupMakerState.isWizardOpen) {
            DistributionWizardDialog(
                students = students,
                currentConfig = groupMakerState.config,
                isDarkTheme = isDarkTheme,
                onDismiss = onCloseWizard,
                onApply = { newConfig ->
                    onSaveConfig(newConfig)
                    onDistribute(newConfig)
                }
            )
        }

        // B. Dokunmatik Öğrenci Takas / Taşıma Dialogu
        if (activeSwapStudent != null) {
            StudentTouchModal(
                student = activeSwapStudent,
                groups = groupMakerState.groups,
                allStudents = students,
                isDarkTheme = isDarkTheme,
                onDismiss = onCloseSwapModal,
                onMoveToGroup = { targetGroupId -> onMoveStudent(activeSwapStudent.id, targetGroupId) },
                onSwapWithStudent = { targetStudentId -> onSwapStudents(activeSwapStudent.id, targetStudentId) },
                onRemoveFromGroup = { onRemoveStudent(activeSwapStudent.id) },
                onToggleLeader = {
                    val currentGroup = groupMakerState.groups.firstOrNull { it.studentIds.contains(activeSwapStudent.id) }
                    if (currentGroup != null) {
                        onToggleLeader(currentGroup.id, activeSwapStudent.id)
                    }
                }
            )
        }

        // C. Grup İsmi Düzenleme Dialogu
        if (groupToRename != null) {
            val group = groupToRename!!
            RenameGroupDialog(
                currentName = group.name,
                isDarkTheme = isDarkTheme,
                onDismiss = { groupToRename = null },
                onConfirm = { newName ->
                    onRenameGroup(group.id, newName)
                    groupToRename = null
                }
            )
        }

        // D. Grup Renk Teması Değiştirme Dialogu
        if (groupToChangeColor != null) {
            val group = groupToChangeColor!!
            ChangeColorThemeDialog(
                currentTheme = group.colorTheme,
                isDarkTheme = isDarkTheme,
                onDismiss = { groupToChangeColor = null },
                onSelect = { newTheme ->
                    onChangeGroupColor(group.id, newTheme)
                    groupToChangeColor = null
                }
            )
        }

        // E. Atanmamış Öğrenciler Çekmecesi / Modalı
        if (showUnassignedDrawer) {
            UnassignedStudentsDialog(
                unassignedStudentIds = groupMakerState.unassignedStudentIds,
                allStudents = students,
                groups = groupMakerState.groups,
                isDarkTheme = isDarkTheme,
                onDismiss = { showUnassignedDrawer = false },
                onDistributeAll = {
                    onDistributeUnassigned()
                    showUnassignedDrawer = false
                },
                onSelectStudentToAssign = { student ->
                    selectedUnassignedStudentForGroup = student
                }
            )
        }

        // F. Atanmamış Tekil Öğrenciyi Bir Gruba Atama Dialogu
        if (selectedUnassignedStudentForGroup != null) {
            val st = selectedUnassignedStudentForGroup!!
            AssignToGroupDialog(
                student = st,
                groups = groupMakerState.groups,
                isDarkTheme = isDarkTheme,
                onDismiss = { selectedUnassignedStudentForGroup = null },
                onSelectGroup = { targetGroupId ->
                    onMoveStudent(st.id, targetGroupId)
                    selectedUnassignedStudentForGroup = null
                }
            )
        }
    }
}

/**
 * Tek bir Grup Kartı Bileşeni
 */
@Composable
private fun GroupCard(
    group: GroupTeam,
    allStudents: List<Student>,
    isDarkTheme: Boolean,
    onRenameClick: () -> Unit,
    onChangeColorClick: () -> Unit,
    onPickLeaderClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onStudentClick: (String) -> Unit,
    onToggleLeaderClick: (String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val members = group.studentIds.mapNotNull { sid -> allStudents.firstOrNull { it.id == sid } }
    val girlCount = members.count { it.gender.equals("KIZ", ignoreCase = true) }
    val boyCount = members.count { it.gender.equals("ERKEK", ignoreCase = true) }

    val theme = group.colorTheme
    val primaryColor = try { Color(android.graphics.Color.parseColor(theme.primaryHex)) } catch (e: Exception) { BirizNavyPrimary }
    val cardBg = if (isDarkTheme) Color(0xFF0F1E38) else try { Color(android.graphics.Color.parseColor(theme.bgHex)) } catch (e: Exception) { Color.White }
    val cardBorder = if (isDarkTheme) BirizDarkCardBorder else try { Color(android.graphics.Color.parseColor(theme.borderHex)) } catch (e: Exception) { BirizBorder }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("group_card_${group.name.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.2.dp, cardBorder)
    ) {
        Column {
            // GRUP BAŞLIĞI ŞERİDİ
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = primaryColor,
                shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Grup İsmi + Renk İkonu + Düzenleme
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onRenameClick() }
                    ) {
                        Text(
                            text = group.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Düzenle",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Grup İçi İstatistik Rozetleri
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "👥 ${members.size}  ♀$girlCount ♂$boyCount",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }

                        // Menü Butonu
                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Seçenekler",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("✏️ İsmi Değiştir", fontSize = 12.sp) },
                                    onClick = {
                                        showMenu = false
                                        onRenameClick()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🎨 Renk Temasını Değiştir", fontSize = 12.sp) },
                                    onClick = {
                                        showMenu = false
                                        onChangeColorClick()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("👑 Rastgele Lider Seç", fontSize = 12.sp) },
                                    onClick = {
                                        showMenu = false
                                        onPickLeaderClick()
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("🗑️ Grubu Sil", fontSize = 12.sp, color = Color.Red) },
                                    onClick = {
                                        showMenu = false
                                        onDeleteClick()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ÖĞRENCİ LİSTESİ BÖLÜMÜ
            if (members.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+ Bu grupta henüz öğrenci yok.\nÖğrenci kartlarına dokunarak buraya taşıyabilirsiniz.",
                        fontSize = 11.sp,
                        color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    members.forEachIndexed { index, student ->
                        val isLeader = student.id == group.leaderStudentId
                        val isGirl = student.gender.equals("KIZ", ignoreCase = true)

                        Surface(
                            onClick = { onStudentClick(student.id) },
                            shape = RoundedCornerShape(10.dp),
                            color = when {
                                isLeader -> if (isDarkTheme) Color(0x50F59E0B) else Color(0xFFFFFBEB)
                                isGirl -> if (isDarkTheme) Color(0x30F472B6) else Color.White
                                else -> if (isDarkTheme) Color(0x3038BDF8) else Color.White
                            },
                            border = BorderStroke(
                                1.dp,
                                when {
                                    isLeader -> BirizAmber
                                    isGirl -> if (isDarkTheme) Color(0xFFF472B6) else Color(0xFFFCE7F3)
                                    else -> if (isDarkTheme) Color(0xFF38BDF8) else Color(0xFFE0F2FE)
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("group_student_item_${student.studentNumber}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Sol: Sıra No + Öğrenci No + İsim
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "${index + 1}.",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
                                        modifier = Modifier.width(22.dp)
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isGirl) Color(0xFFFCE7F3) else Color(0xFFE0F2FE)
                                    ) {
                                        Text(
                                            text = "#${student.studentNumber}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isGirl) Color(0xFFDB2777) else Color(0xFF0284C7),
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = student.fullName,
                                        fontSize = 12.sp,
                                        fontWeight = if (isLeader) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        color = if (isDarkTheme) Color.White else Color(0xFF1E293B),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Sağ: Lider / Taç Butonu & Cinsiyet İkonu
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Taç Butonu (Sözcü Seçimi)
                                    Surface(
                                        onClick = { onToggleLeaderClick(student.id) },
                                        shape = CircleShape,
                                        color = if (isLeader) BirizAmber else Color.Transparent,
                                        border = if (isLeader) null else BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)),
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "👑",
                                                fontSize = 12.sp
                                            )
                                        }
                                    }

                                    Text(
                                        text = if (isGirl) "♀" else "♂",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isGirl) Color(0xFFEC4899) else Color(0xFF0EA5E9)
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

/**
 * Dokunmatik Öğrenci Takas & Taşıma Modalı (Tap-to-Swap / Move Modal)
 */
@Composable
private fun StudentTouchModal(
    student: Student,
    groups: List<GroupTeam>,
    allStudents: List<Student>,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onMoveToGroup: (String) -> Unit,
    onSwapWithStudent: (String) -> Unit,
    onRemoveFromGroup: () -> Unit,
    onToggleLeader: () -> Unit
) {
    var mode by remember { mutableStateOf("MAIN") } // "MAIN", "MOVE", "SWAP"
    val currentGroup = groups.firstOrNull { it.studentIds.contains(student.id) }
    val isLeader = currentGroup?.leaderStudentId == student.id

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isDarkTheme) Color(0xFF0F1E38) else Color.White,
            border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Başlık: Öğrenci Künyesi
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (student.gender.equals("KIZ", ignoreCase = true)) Color(0xFFFCE7F3) else Color(0xFFE0F2FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (student.gender.equals("KIZ", ignoreCase = true)) "👧" else "👦",
                                fontSize = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = student.fullName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) Color.White else Color(0xFF1E293B)
                            )
                            Text(
                                text = "No: ${student.studentNumber} • Grup: ${currentGroup?.name ?: "Atanmamış"}",
                                fontSize = 11.sp,
                                color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = if (isDarkTheme) BirizDarkCardBorder else BirizBorder)
                Spacer(modifier = Modifier.height(14.dp))

                when (mode) {
                    "MAIN" -> {
                        // 1. Liderlik Aç/Kapat
                        OutlinedButton(
                            onClick = {
                                onToggleLeader()
                                onDismiss()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isLeader) "👑 Grup Sözcülüğü Unvanını Kaldır" else "👑 Bu Öğrenciyi Grup Sözcüsü / Lideri Yap",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 2. Gruba Taşı
                        Button(
                            onClick = { mode = "MOVE" },
                            colors = ButtonDefaults.buttonColors(containerColor = BirizTurquoise),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("➡️ Başka Bir Gruba Taşı", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 3. Öğrenciyle Takas Et (Swap)
                        Button(
                            onClick = { mode = "SWAP" },
                            colors = ButtonDefaults.buttonColors(containerColor = BirizNavyPrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("⇄ Başka Bir Öğrenciyle Takas Et", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 4. Gruptan Çıkar
                        OutlinedButton(
                            onClick = {
                                onRemoveFromGroup()
                                onDismiss()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("📦 Gruptan Çıkar (Atanmamışlar Havuzuna Gönder)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    "MOVE" -> {
                        Text(
                            text = "Öğrenciyi taşımak istediğiniz hedef grubu seçin:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) Color.White else Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val otherGroups = groups.filter { it.id != currentGroup?.id }
                        if (otherGroups.isEmpty()) {
                            Text(
                                text = "Taşınabilecek başka bir grup bulunmuyor.",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        } else {
                            LazyColumn(modifier = Modifier.height(180.dp)) {
                                items(otherGroups) { targetGroup ->
                                    Surface(
                                        onClick = {
                                            onMoveToGroup(targetGroup.id)
                                            onDismiss()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isDarkTheme) Color(0xFF172649) else Color(0xFFF1F5F9),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "📌 ${targetGroup.name}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${targetGroup.studentIds.size} Öğrenci",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { mode = "MAIN" }, modifier = Modifier.align(Alignment.End)) {
                            Text("Geri")
                        }
                    }

                    "SWAP" -> {
                        Text(
                            text = "Yerini değiştirmek (takas etmek) istediğiniz öğrenciyi seçin:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) Color.White else Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Kendisi hariç diğer tüm gruplardaki öğrenciler
                        val otherStudents = allStudents.filter { st ->
                            st.id != student.id && groups.any { it.studentIds.contains(st.id) && it.id != currentGroup?.id }
                        }

                        if (otherStudents.isEmpty()) {
                            Text(
                                text = "Takas edilebilecek başka bir öğrenci bulunmuyor.",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        } else {
                            LazyColumn(modifier = Modifier.height(200.dp)) {
                                items(otherStudents) { targetStudent ->
                                    val targetG = groups.firstOrNull { it.studentIds.contains(targetStudent.id) }
                                    Surface(
                                        onClick = {
                                            onSwapWithStudent(targetStudent.id)
                                            onDismiss()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isDarkTheme) Color(0xFF172649) else Color(0xFFF1F5F9),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text(
                                                    text = "#${targetStudent.studentNumber} ${targetStudent.fullName}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "Grup: ${targetG?.name ?: "Bilinmiyor"}",
                                                    fontSize = 10.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                            Text(text = "⇄ Takas", fontSize = 11.sp, color = BirizTurquoise, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { mode = "MAIN" }, modifier = Modifier.align(Alignment.End)) {
                            Text("Geri")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Akıllı Dağıtım Sihirbazı Modalı (Distribution Wizard Dialog)
 */
@Composable
private fun DistributionWizardDialog(
    students: List<Student>,
    currentConfig: GroupDistributionConfig,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onApply: (GroupDistributionConfig) -> Unit
) {
    var mode by remember { mutableStateOf(currentConfig.mode) }
    var targetGroupCount by remember { mutableStateOf(currentConfig.targetGroupCount) }
    var targetStudentsPerGroup by remember { mutableStateOf(currentConfig.targetStudentsPerGroup) }
    var balanceGender by remember { mutableStateOf(currentConfig.balanceGender) }
    var excludedStudentIds by remember { mutableStateOf(currentConfig.excludedStudentIds.toMutableSet()) }
    var searchQuery by remember { mutableStateOf("") }

    val activeCount = students.size - excludedStudentIds.size

    val estimatedGroups = when (mode) {
        GroupDistributionMode.BY_GROUP_COUNT -> targetGroupCount
        GroupDistributionMode.BY_STUDENTS_PER_GROUP -> kotlin.math.ceil(activeCount.toDouble() / targetStudentsPerGroup).toInt().coerceAtLeast(1)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.86f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(18.dp),
            color = if (isDarkTheme) Color(0xFF0F1E38) else Color.White,
            border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // BAŞLIK BARI
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "✨", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Akıllı Dağıtım Sihirbazı",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) Color.White else Color(0xFF1E293B)
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.Gray, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = if (isDarkTheme) BirizDarkCardBorder else BirizBorder)
                Spacer(modifier = Modifier.height(8.dp))

                // 1. SIKIŞTIRILMIŞ ÜST AYARLAR (Yöntem, Stepper ve Cinsiyet Dengesi)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // A. Dağıtım Yöntemi (Kompakt 2 Seçenek)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = { mode = GroupDistributionMode.BY_GROUP_COUNT },
                            shape = RoundedCornerShape(8.dp),
                            color = if (mode == GroupDistributionMode.BY_GROUP_COUNT) BirizNavyPrimary else (if (isDarkTheme) Color(0xFF172649) else Color(0xFFF1F5F9)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Grup Sayısına Göre",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (mode == GroupDistributionMode.BY_GROUP_COUNT) Color.White else (if (isDarkTheme) Color.White else Color(0xFF1E293B))
                                )
                            }
                        }

                        Surface(
                            onClick = { mode = GroupDistributionMode.BY_STUDENTS_PER_GROUP },
                            shape = RoundedCornerShape(8.dp),
                            color = if (mode == GroupDistributionMode.BY_STUDENTS_PER_GROUP) BirizNavyPrimary else (if (isDarkTheme) Color(0xFF172649) else Color(0xFFF1F5F9)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Kişi Sayısına Göre",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (mode == GroupDistributionMode.BY_STUDENTS_PER_GROUP) Color.White else (if (isDarkTheme) Color.White else Color(0xFF1E293B))
                                )
                            }
                        }
                    }

                    // B. Stepper (- ve + Butonlu)
                    if (mode == GroupDistributionMode.BY_GROUP_COUNT) {
                        StepperCard(
                            label = "Toplam Grup Sayısı",
                            valueText = "$targetGroupCount Grup",
                            subText = "Grup başına ~${if (targetGroupCount > 0) activeCount / targetGroupCount else 0} kişi",
                            onMinus = { if (targetGroupCount > 2) targetGroupCount-- },
                            onPlus = { if (targetGroupCount < 12) targetGroupCount++ },
                            isDarkTheme = isDarkTheme
                        )
                    } else {
                        StepperCard(
                            label = "Grup Başına Kişi Sayısı",
                            valueText = "$targetStudentsPerGroup Öğrenci",
                            subText = "~$estimatedGroups grup oluşacak",
                            onMinus = { if (targetStudentsPerGroup > 2) targetStudentsPerGroup-- },
                            onPlus = { if (targetStudentsPerGroup < 15) targetStudentsPerGroup++ },
                            isDarkTheme = isDarkTheme
                        )
                    }

                    // C. Kız - Erkek Cinsiyet Dengesi (Kompakt Tek Satır)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDarkTheme) Color(0xFF172649) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "⚖️", fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "Kız - Erkek Cinsiyet Dengesi",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkTheme) Color.White else Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = if (balanceGender) "Gruplarda kız/erkek oranları eşitlenir" else "Rastgele karma dağıtım",
                                        fontSize = 9.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            Switch(
                                checked = balanceGender,
                                onCheckedChange = { balanceGender = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = BirizTurquoise, checkedTrackColor = Color(0xFFCCFBF1)),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. KATILIMCI SEÇİMİ BAŞLIĞI VE AKSİYONLARI (Tümünü Seç / Temizle)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Katılımcı Seçimi",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BirizTurquoise
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isDarkTheme) Color(0x350D9488) else Color(0xFFCCFBF1)
                        ) {
                            Text(
                                text = "$activeCount / ${students.size}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) Color(0xFF5EEAD4) else Color(0xFF0F766E),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Geniş, rahat tıklanabilir hap butonlar
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = { excludedStudentIds = mutableSetOf() },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, BirizBorder)
                        ) {
                            Text(
                                text = "Tümünü Seç",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = BirizTurquoise,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            onClick = { excludedStudentIds = students.map { it.id }.toMutableSet() },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDarkTheme) Color(0x30EF4444) else Color(0xFFFEE2E2),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                        ) {
                            Text(
                                text = "Temizle",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626),
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 3. GENİŞLETİLMİŞ ÖĞRENCİ LİSTESİ ALANI (weight(1f) ile kalan tüm alanı kaplar)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDarkTheme) Color(0xFF172649) else Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        items(students, key = { it.id }) { student ->
                            val isIncluded = !excludedStudentIds.contains(student.id)
                            val isGirl = student.gender.equals("KIZ", ignoreCase = true)

                            Surface(
                                onClick = {
                                    val next = excludedStudentIds.toMutableSet()
                                    if (isIncluded) next.add(student.id) else next.remove(student.id)
                                    excludedStudentIds = next
                                },
                                shape = RoundedCornerShape(6.dp),
                                color = if (isIncluded) (if (isDarkTheme) Color(0x400E1A38) else Color.White) else Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isIncluded,
                                        onCheckedChange = { checked ->
                                            val next = excludedStudentIds.toMutableSet()
                                            if (checked) next.remove(student.id) else next.add(student.id)
                                            excludedStudentIds = next
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = BirizTurquoise),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))

                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isGirl) Color(0xFFFCE7F3) else Color(0xFFE0F2FE)
                                    ) {
                                        Text(
                                            text = "#${student.studentNumber}",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isGirl) Color(0xFFDB2777) else Color(0xFF0284C7),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = student.fullName,
                                        fontSize = 11.5.sp,
                                        color = if (isIncluded) (if (isDarkTheme) Color.White else Color(0xFF1E293B)) else Color.Gray,
                                        fontWeight = if (isIncluded) FontWeight.SemiBold else FontWeight.Normal,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Text(
                                        text = if (isGirl) "♀" else "♂",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isGirl) Color(0xFFEC4899) else Color(0xFF0EA5E9)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = if (isDarkTheme) BirizDarkCardBorder else BirizBorder)
                Spacer(modifier = Modifier.height(10.dp))

                // 4. ALT AKSİYON BUTONLARI (Her zaman ekranın en altında sabit ve tam görünür)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Vazgeç", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val newConfig = GroupDistributionConfig(
                                mode = mode,
                                targetGroupCount = targetGroupCount,
                                targetStudentsPerGroup = targetStudentsPerGroup,
                                balanceGender = balanceGender,
                                excludedStudentIds = excludedStudentIds
                            )
                            onApply(newConfig)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BirizTurquoise),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 10.dp),
                        modifier = Modifier
                            .weight(2f)
                            .testTag("wizard_apply_distribution_button")
                    ) {
                        Text("✨ Grupları Oluştur ($activeCount Kişi)", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

/**
 * Stepper Kartı (- ve + Butonlu, Kompakt)
 */
@Composable
private fun StepperCard(
    label: String,
    valueText: String,
    subText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    isDarkTheme: Boolean
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isDarkTheme) Color(0xFF172649) else Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) Color.White else Color(0xFF1E293B)
                )
                Text(
                    text = subText,
                    fontSize = 9.sp,
                    color = Color.Gray
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = onMinus,
                    shape = CircleShape,
                    color = if (isDarkTheme) Color(0xFF334E68) else Color(0xFFE2E8F0),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "–", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    text = valueText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BirizTurquoise,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )

                Surface(
                    onClick = onPlus,
                    shape = CircleShape,
                    color = if (isDarkTheme) Color(0xFF334E68) else Color(0xFFE2E8F0),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "+", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Grup İsmi Değiştirme Modalı
 */
@Composable
private fun RenameGroupDialog(
    currentName: String,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Grup İsmini Düzenle", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Grup Adı") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text("Öneri İsimler:", fontSize = 11.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GroupMakerHelper.FUN_GROUP_NAMES.take(6).forEach { suggestion ->
                        Surface(
                            onClick = { text = suggestion },
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDarkTheme) Color(0xFF172649) else Color(0xFFF1F5F9)
                        ) {
                            Text(
                                text = suggestion,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onConfirm(text.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = BirizTurquoise)
            ) {
                Text("Kaydet")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Vazgeç") }
        }
    )
}

/**
 * 8 Canlı Preset Renk Teması Seçim Dialogu
 */
@Composable
private fun ChangeColorThemeDialog(
    currentTheme: GroupColorTheme,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onSelect: (GroupColorTheme) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Grup Renk Teması Seç", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(160.dp)
            ) {
                items(GroupColorTheme.values()) { theme ->
                    val color = try { Color(android.graphics.Color.parseColor(theme.primaryHex)) } catch (e: Exception) { Color.Gray }
                    val isSelected = theme == currentTheme

                    Surface(
                        onClick = { onSelect(theme) },
                        shape = RoundedCornerShape(10.dp),
                        color = color,
                        border = if (isSelected) BorderStroke(2.dp, Color.White) else null,
                        modifier = Modifier.size(50.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Kapat") } }
    )
}

/**
 * Atanmamış Öğrenciler Çekmecesi / Listesi Modalı
 */
@Composable
private fun UnassignedStudentsDialog(
    unassignedStudentIds: List<String>,
    allStudents: List<Student>,
    groups: List<GroupTeam>,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onDistributeAll: () -> Unit,
    onSelectStudentToAssign: (Student) -> Unit
) {
    val unassignedList = allStudents.filter { unassignedStudentIds.contains(it.id) }
    var search by remember { mutableStateOf("") }
    val filtered = unassignedList.filter {
        search.isEmpty() || it.fullName.contains(search, ignoreCase = true) || it.studentNumber.contains(search)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isDarkTheme) Color(0xFF0F1E38) else Color.White,
            border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "📦 Atanmamış Öğrenciler",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) Color.White else Color(0xFF1E293B)
                        )
                        Text(
                            text = "${unassignedList.size} öğrenci henüz bir grupta yer almıyor",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Öğrenci ara...", fontSize = 12.sp) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onDistributeAll,
                    colors = ButtonDefaults.buttonColors(containerColor = BirizAmber),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("⚡ Kalanları Mevcut Gruplara Dengeli Dağıt", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(modifier = Modifier.height(220.dp)) {
                    items(filtered) { student ->
                        Surface(
                            onClick = {
                                onSelectStudentToAssign(student)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDarkTheme) Color(0xFF172649) else Color(0xFFF1F5F9),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "#${student.studentNumber}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BirizTurquoise
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = student.fullName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(text = "Gruba Ata ➡️", fontSize = 10.sp, color = BirizNavyPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tekil Atanmamış Öğrenciyi Hedef Gruba Ekleme Modalı
 */
@Composable
private fun AssignToGroupDialog(
    student: Student,
    groups: List<GroupTeam>,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onSelectGroup: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "${student.fullName} Hangi Gruba Eklensin?",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(modifier = Modifier.height(200.dp)) {
                items(groups) { group ->
                    Surface(
                        onClick = { onSelectGroup(group.id) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDarkTheme) Color(0xFF172649) else Color(0xFFF1F5F9),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "📌 ${group.name}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(text = "${group.studentIds.size} Kişi", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } }
    )
}

