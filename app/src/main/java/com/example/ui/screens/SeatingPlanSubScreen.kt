package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AirlineSeatReclineNormal
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoorBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Window
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ArchivedSeatingPlan
import com.example.data.model.ClassRoom
import com.example.data.model.DeskPosition
import com.example.data.model.DoorPosition
import com.example.data.model.SeatingConfig
import com.example.data.model.SeatingPlacementMode
import com.example.data.model.SeatingPlanState
import com.example.data.model.Student
import com.example.data.model.WindowPosition
import com.example.ui.theme.BirizAmber
import com.example.ui.theme.BirizBorder
import com.example.ui.theme.BirizDarkCardBorder
import com.example.ui.theme.BirizDarkCardGlass
import com.example.ui.theme.BirizDarkSurfaceGlass
import com.example.ui.theme.BirizDarkTextPrimary
import com.example.ui.theme.BirizNavyDark
import com.example.ui.theme.BirizNavyLight
import com.example.ui.theme.BirizNavyPrimary
import com.example.ui.theme.BirizTurquoise
import com.example.util.ExcelPdfHelper
import com.example.util.SeatingPlanHelper
import com.example.util.TurkishTextHelper

/**
 * Akıllı Sınıf Oturma Planı (Seating Plan) Ekranı
 *
 * Mobil uyumlu, dokunmatik kontrollü, esnek öbek ve sıra mimarili,
 * akıllı algoritmik yerleştirme ve mürekkep dostu A4 PDF çıktılı sınıf oturma planı.
 */
@Composable
fun SeatingPlanSubScreen(
    classRoom: ClassRoom,
    students: List<Student>,
    seatingPlanState: SeatingPlanState,
    isConfigDialogOpen: Boolean,
    isDarkTheme: Boolean,
    onOpenConfigDialog: () -> Unit,
    onCloseConfigDialog: () -> Unit,
    onSaveConfig: (SeatingConfig) -> Unit,
    onAutoAssign: (SeatingPlacementMode) -> Unit,
    onApplyPlannedSeating: (SeatingPlanState) -> Unit = {},
    onApplyArchivedPlan: (ArchivedSeatingPlan) -> Unit = {},
    onAssignStudentToSeat: (seatId: String, studentId: String?) -> Unit,
    onSwapSeats: (seatId1: String, seatId2: String) -> Unit,
    onTogglePin: (studentId: String) -> Unit,
    onTogglePriority: (studentId: String) -> Unit,
    onClearPlan: (clearFixed: Boolean) -> Unit,
    onBackToClassList: () -> Unit
) {
    val context = LocalContext.current

    // Takas (Swap) modu durumu
    var swapSourceSeatId by remember { mutableStateOf<String?>(null) }

    // Ayaktaki öğrenci tepsisinden seçilip boş koltuğa dokunarak yerleştirme durumu
    var selectedStandingStudentId by remember { mutableStateOf<String?>(null) }

    // Seçilen koltuk detay diyaloğu
    var selectedSeatForDetail by remember { mutableStateOf<String?>(null) }

    // Boş koltuğa öğrenci atama diyaloğu
    var selectedSeatForAssignment by remember { mutableStateOf<String?>(null) }

    // Otomatik dağıtım mod diyaloğu (2 sütun x 4 satır kutu kutu - rotasyon modları dahil)
    var showAutoAssignDialog by remember { mutableStateOf(false) }

    // Elle yerleştirme başlangıç seçenekleri diyaloğu
    var showManualModeOptionsDialog by remember { mutableStateOf(false) }

    // Paylaşım menüsü
    var isShareMenuOpen by remember { mutableStateOf(false) }

    // Planlı Yerleştirme Sihirbazı
    var isPlannedWizardOpen by remember { mutableStateOf(false) }

    // Oturma Planı Arşiv Diyaloğu
    var isArchiveDialogOpen by remember { mutableStateOf(false) }

    // Planı Sıfırla Güvenlik Uyarısı Diyaloğu
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    // Koltuk atamalarını güncel öğrenci listesiyle uzlaştır (hayalet/eski ID'lerin boş koltukları kilitlemesini önler)
    val effectivePlanState = remember(seatingPlanState, students) {
        SeatingPlanHelper.reconcileAndSanitizePlan(seatingPlanState, students)
    }

    // Yerleşen ve Yerleşmemiş (Ayaktaki) Öğrenciler
    val assignedStudentIds = remember(effectivePlanState.assignments, students) {
        val validStudentIds = students.map { it.id }.toSet()
        effectivePlanState.assignments.values.filter { it.isNotBlank() && validStudentIds.contains(it) }.toSet()
    }
    val unassignedStudents = remember(students, assignedStudentIds) {
        students
            .filter { !assignedStudentIds.contains(it.id) }
            .sortedBy { it.studentNumber.toIntOrNull() ?: 9999 }
    }
    val seatedCount = assignedStudentIds.size
    val totalSeats = effectivePlanState.config.totalSeats

    // Zemin ve Kart Renkleri
    val cardBg = if (isDarkTheme) BirizDarkSurfaceGlass else Color.White
    val cardBorder = if (isDarkTheme) BirizDarkCardBorder else BirizBorder
    val primaryTextColor = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("seating_plan_sub_screen")
    ) {
        // --- 1. ÜST KONTROL VE İSTATİSTİK BAR ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = if (isDarkTheme) BirizDarkSurfaceGlass else Color(0xCCFFFFFF),
            border = BorderStroke(1.dp, cardBorder),
            shadowElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // İstatistik Rozetleri & Sıra Düzeni & Paylaş (Tam Eşit Yükseklikte)
                val topBarHeight = 42.dp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Birleştirilmiş İstatistik Kutusu: İnsan (Mevcut), Dolu Koltuk (Yerleşen), Ayakta İnsan (Yerleşmeyen), Boş Koltuk (Boşta Kalan)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDarkTheme) Color(0x301E293B) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .weight(1f)
                            .height(topBarHeight)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // İnsan figürü + sınıf mevcudu sayısı
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text("👥", fontSize = 13.sp)
                                Text(
                                    text = "${students.size}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryTextColor
                                )
                            }

                            Text("•", color = Color(0xFF94A3B8), fontSize = 10.sp)

                            // Dolu koltuk figürü (Sandalyede oturan kişi) + dolu koltuk sayısı
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AirlineSeatReclineNormal,
                                    contentDescription = "Dolu Koltuk",
                                    tint = if (isDarkTheme) BirizTurquoise else Color(0xFF0F766E),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "$seatedCount",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkTheme) BirizTurquoise else Color(0xFF0F766E)
                                )
                            }

                            Text("•", color = Color(0xFF94A3B8), fontSize = 10.sp)

                            // Ayakta insan figürü + henüz yerleşmeyen öğrenci sayısı
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text("🧍", fontSize = 13.sp)
                                Text(
                                    text = "${unassignedStudents.size}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (unassignedStudents.isNotEmpty()) Color(0xFFE11D48) else Color(0xFF64748B)
                                )
                            }

                            Text("•", color = Color(0xFF94A3B8), fontSize = 10.sp)

                            // Boş koltuk + boşta kalan koltuk sayısı
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text("🪑", fontSize = 13.sp)
                                val emptySeats = (totalSeats - seatedCount).coerceAtLeast(0)
                                Text(
                                    text = "$emptySeats",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkTheme) BirizAmber else Color(0xFFB45309)
                                )
                            }
                        }
                    }

                    // 2. Ayarlar Butonu (Simgesi ile - Eşit Yükseklikte)
                    Surface(
                        onClick = onOpenConfigDialog,
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDarkTheme) Color(0x303B82F6) else Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFFBFDBFE)),
                        modifier = Modifier
                            .size(topBarHeight)
                            .testTag("seating_config_open_button")
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Sıra Düzeni Ayarları",
                                tint = if (isDarkTheme) Color(0xFF93C5FD) else Color(0xFF1D4ED8),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    // 3. Paylaş Butonu (Simgesi ile - Eşit Yükseklikte)
                    Box {
                        Surface(
                            onClick = { isShareMenuOpen = true },
                            shape = RoundedCornerShape(10.dp),
                            color = BirizNavyPrimary,
                            modifier = Modifier
                                .size(topBarHeight)
                                .testTag("seating_share_button")
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Paylaş",
                                    modifier = Modifier.size(18.dp),
                                    tint = Color.White
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = isShareMenuOpen,
                            onDismissRequest = { isShareMenuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("📋 WhatsApp / SMS Metni Kopyala", fontSize = 12.sp) },
                                onClick = {
                                    isShareMenuOpen = false
                                    val shareText = ExcelPdfHelper.generateSeatingPlanShareText(classRoom, seatingPlanState, students)
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Oturma Planı", shareText))
                                    Toast.makeText(context, "Oturma planı panoya kopyalandı! 📋", Toast.LENGTH_SHORT).show()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("📊 Excel (.xlsx) İndir / Paylaş", fontSize = 12.sp) },
                                onClick = {
                                    isShareMenuOpen = false
                                    try {
                                        val uri = ExcelPdfHelper.exportSeatingPlanExcel(context, classRoom, seatingPlanState, students)
                                        ExcelPdfHelper.shareFile(
                                            context = context,
                                            uri = uri,
                                            mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                            chooserTitle = "Oturma Planı Excel Dosyasını Paylaş"
                                        )
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("🖨️ PDF Krokisi İndir / Yazdır", fontSize = 12.sp) },
                                onClick = {
                                    isShareMenuOpen = false
                                    try {
                                        val uri = ExcelPdfHelper.exportSeatingPlanPdf(
                                            context = context,
                                            classRoom = classRoom,
                                            seatingPlanState = seatingPlanState,
                                            students = students
                                        )
                                        ExcelPdfHelper.shareFile(
                                            context = context,
                                            uri = uri,
                                            mimeType = "application/pdf",
                                            chooserTitle = "Oturma Planı PDF Dosyasını Paylaş"
                                        )
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "PDF oluşturulamadı: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("🏷️ Sıra Üstü Kesilebilir İsimlikleri Yazdır (PDF)", fontSize = 12.sp) },
                                onClick = {
                                    isShareMenuOpen = false
                                    try {
                                        val uri = ExcelPdfHelper.exportDeskNameTagsPdf(
                                            context = context,
                                            classRoom = classRoom,
                                            seatingPlanState = seatingPlanState,
                                            students = students
                                        )
                                        ExcelPdfHelper.shareFile(
                                            context = context,
                                            uri = uri,
                                            mimeType = "application/pdf",
                                            chooserTitle = "Sıra Üstü İsimlikleri Paylaş / Yazdır"
                                        )
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "İsimlik PDF oluşturulamadı: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Alt Satır: Akıllı Dağıt, Arşiv ve Çöp Kutusu Simgesi (Tam Eşit Yükseklikte)
                val actionBarHeight = 38.dp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Akıllı Dağıt Butonu (Kutu kutu mod penceresi - Rotasyon modları dahil)
                    Button(
                        onClick = { showAutoAssignDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDarkTheme) BirizNavyLight else BirizNavyPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(actionBarHeight)
                            .testTag("seating_auto_assign_menu_button")
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Akıllı Dağıt", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }

                    // 2. Arşiv Butonu
                    OutlinedButton(
                        onClick = { isArchiveDialogOpen = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryTextColor),
                        border = BorderStroke(1.dp, cardBorder),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier
                            .height(actionBarHeight)
                            .testTag("seating_archive_button")
                    ) {
                        Icon(imageVector = Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Arşiv", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    // 3. Çöp Kutusu Simgesi (Sıfırla - Satırdaki Diğer Butonlarla Birebir Eşit Yükseklik ve Köşede)
                    OutlinedButton(
                        onClick = { showClearConfirmDialog = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isDarkTheme) Color(0x30EF4444) else Color(0xFFFEE2E2),
                            contentColor = if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFFDC2626)
                        ),
                        border = BorderStroke(1.dp, if (isDarkTheme) Color(0x60EF4444) else Color(0xFFFECACA)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .height(actionBarHeight)
                            .width(actionBarHeight)
                            .testTag("seating_clear_plan_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Planı Sıfırla",
                            tint = if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFFDC2626),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Görüntülenen Plan Adı Şeridi (Ekranda gösterilir, PDF çıktısında yer almaz)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDarkTheme) Color(0x250EA5E9) else Color(0xFFF0F9FF),
                    border = BorderStroke(1.dp, if (isDarkTheme) Color(0x400EA5E9) else Color(0xFFBAE6FD)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                            )
                            Text(
                                text = "Görüntülenen Plan:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDarkTheme) Color.LightGray else Color(0xFF475569)
                            )
                            Text(
                                text = seatingPlanState.activePlanName.ifBlank { "Varsayılan Plan" },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Text(
                            text = "📂 Arşivden Seç",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDarkTheme) BirizTurquoise else Color(0xFF0369A1),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { isArchiveDialogOpen = true }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // --- 2. TAKAS (SWAP) UYARI VE İPTAL BİLGİ ŞERİDİ ---
        AnimatedVisibility(
            visible = swapSourceSeatId != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val sourceStudentId = effectivePlanState.assignments[swapSourceSeatId]
            val sourceStudent = students.firstOrNull { it.id == sourceStudentId }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isDarkTheme) Color(0xD078350F) else Color(0xFFFEF3C7),
                border = BorderStroke(1.dp, Color(0xFFF59E0B))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = Color(0xFFB45309),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Koltuk Takası: ${sourceStudent?.fullName ?: "Seçilen koltuk"} için hedef sıraya dokunun.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) Color.White else Color(0xFF78350F)
                        )
                    }

                    TextButton(
                        onClick = { swapSourceSeatId = null }
                    ) {
                        Text("İptal", fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                    }
                }
            }
        }

        // --- 2B. AYAKTAKİ ÖĞRENCİYİ SEÇİP BOŞ KOLTUĞA YERLEŞTİRME ŞERİDİ ---
        AnimatedVisibility(
            visible = selectedStandingStudentId != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val selectedStudent = students.firstOrNull { it.id == selectedStandingStudentId }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isDarkTheme) Color(0xD00D4A56) else Color(0xFFE0F7F6),
                border = BorderStroke(1.dp, BirizTurquoise)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("👆", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Elle Yerleştirme: ${selectedStudent?.studentNumber ?: ""} ${selectedStudent?.fullName ?: ""}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDarkTheme) Color.White else Color(0xFF0F766E)
                            )
                            Text(
                                text = "Oturtmak istediğiniz boş (+ Boş) koltuğa dokunun.",
                                fontSize = 10.5.sp,
                                color = if (isDarkTheme) Color(0xFFBAE6FD) else Color(0xFF0D9488)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = {
                                val st = selectedStudent ?: return@TextButton
                                val allSeatIds = SeatingPlanHelper.generateAllSeatIdsFrontToBack(effectivePlanState.config)
                                val firstEmptySeatId = allSeatIds.firstOrNull { seatId ->
                                    val occId = effectivePlanState.assignments[seatId]
                                    occId.isNullOrBlank() || students.none { it.id == occId }
                                }
                                if (firstEmptySeatId != null) {
                                    onAssignStudentToSeat(firstEmptySeatId, st.id)
                                    selectedStandingStudentId = null
                                } else {
                                    Toast.makeText(context, "Sınıfta boş koltuk kalmadı.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text("İlk Boşa Oturt", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDarkTheme) BirizTurquoise else Color(0xFF0F766E))
                        }

                        TextButton(
                            onClick = { selectedStandingStudentId = null }
                        ) {
                            Text("İptal", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE11D48))
                        }
                    }
                }
            }
        }

        // --- 3. ANA İÇERİK: SINIF KROKİSİ VE OTURMA PLANI ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 72.dp)
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // 3.1. Sınıf Ön Cephesi (Akıllı Tahta, Öğretmen Kürsüsü, Kapı ve Pencere)
                ClassroomFrontArchBanner(
                    config = effectivePlanState.config,
                    isDarkTheme = isDarkTheme
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 3.2. Sıra ve Öbek Kolonları (Bento Düzeni - Yatay Kaydırılabilir)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        val config = effectivePlanState.config
                        for (groupIndex in 0 until config.groupCount) {
                            val rowsInGroup = config.rowsPerGroup.getOrElse(groupIndex) { 5 }

                            GroupColumn(
                                groupIndex = groupIndex,
                                totalRows = rowsInGroup,
                                peoplePerRow = config.peoplePerRow,
                                seatingPlanState = effectivePlanState,
                                students = students,
                                swapSourceSeatId = swapSourceSeatId,
                                isStandingPlacementActive = selectedStandingStudentId != null,
                                isDarkTheme = isDarkTheme,
                                onSeatClick = { seatId ->
                                    if (swapSourceSeatId != null) {
                                        if (swapSourceSeatId != seatId) {
                                            onSwapSeats(swapSourceSeatId!!, seatId)
                                        }
                                        swapSourceSeatId = null
                                    } else if (selectedStandingStudentId != null) {
                                        onAssignStudentToSeat(seatId, selectedStandingStudentId)
                                        selectedStandingStudentId = null
                                    } else {
                                        val occupantId = effectivePlanState.assignments[seatId]
                                        val occupantStudent = students.firstOrNull { it.id == occupantId }
                                        if (occupantStudent != null) {
                                            selectedSeatForDetail = seatId
                                        } else {
                                            selectedSeatForAssignment = seatId
                                        }
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3.3. Sınıf Arka Cephesi (Arka Pencereler, Arka Kapı ve Sınıf Panosu)
                ClassroomBackArchBanner(
                    config = effectivePlanState.config,
                    isDarkTheme = isDarkTheme
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 3.4. Yerleşmemiş (Ayaktaki) Öğrenciler Tepsisi (Varsa)
                if (unassignedStudents.isNotEmpty()) {
                    UnassignedStudentsSection(
                        unassignedStudents = unassignedStudents,
                        selectedStandingStudentId = selectedStandingStudentId,
                        isDarkTheme = isDarkTheme,
                        onStudentClick = { student ->
                            if (selectedStandingStudentId == student.id) {
                                selectedStandingStudentId = null
                            } else {
                                swapSourceSeatId = null
                                selectedStandingStudentId = student.id
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // --- DİYALOGLAR ---

    // 1. Koltuk Detayı & İşlem Menüsü
    selectedSeatForDetail?.let { seatId ->
        val studentId = effectivePlanState.assignments[seatId]
        val student = students.firstOrNull { it.id == studentId }

        if (student != null) {
            val isPinned = effectivePlanState.pinnedStudents.contains(student.id)
            val isPriority = effectivePlanState.priorityStudents.contains(student.id)

            SeatDetailDialog(
                seatId = seatId,
                student = student,
                isPinned = isPinned,
                isPriority = isPriority,
                isDarkTheme = isDarkTheme,
                onDismiss = { selectedSeatForDetail = null },
                onStartSwap = {
                    swapSourceSeatId = seatId
                    selectedStandingStudentId = null
                    selectedSeatForDetail = null
                },
                onChangeStudentForSeat = {
                    selectedSeatForDetail = null
                    selectedSeatForAssignment = seatId
                },
                onTogglePin = {
                    onTogglePin(student.id)
                },
                onTogglePriority = {
                    onTogglePriority(student.id)
                },
                onEmptySeat = {
                    onAssignStudentToSeat(seatId, null)
                    selectedSeatForDetail = null
                }
            )
        } else {
            selectedSeatForDetail = null
            selectedSeatForAssignment = seatId
        }
    }

    // 2. Boş (veya Seçilen) Koltuğa Öğrenci Seçme Diyaloğu
    selectedSeatForAssignment?.let { seatId ->
        AssignStudentDialog(
            seatId = seatId,
            unassignedStudents = unassignedStudents,
            allStudents = students,
            seatingPlanState = effectivePlanState,
            isDarkTheme = isDarkTheme,
            onDismiss = { selectedSeatForAssignment = null },
            onSelectStudent = { studentId, shouldPin ->
                onAssignStudentToSeat(seatId, studentId)
                if (shouldPin && !effectivePlanState.pinnedStudents.contains(studentId)) {
                    onTogglePin(studentId)
                }
                selectedStandingStudentId = null
                selectedSeatForAssignment = null
            }
        )
    }

    // 3. Sınıf Mimarisi & Ayarlar Diyaloğu
    if (isConfigDialogOpen) {
        SeatingConfigDialog(
            currentConfig = effectivePlanState.config,
            isDarkTheme = isDarkTheme,
            onDismiss = onCloseConfigDialog,
            onSave = { newConfig ->
                onSaveConfig(newConfig)
                onCloseConfigDialog()
            }
        )
    }

    // 4. Planlı Yerleştirme Sihirbazı
    if (isPlannedWizardOpen) {
        PlannedSeatingWizardDialog(
            classRoom = classRoom,
            students = students,
            currentState = effectivePlanState,
            isDarkTheme = isDarkTheme,
            onDismiss = { isPlannedWizardOpen = false },
            onApplyPlannedPlan = { newPlanState ->
                onApplyPlannedSeating(newPlanState)
                isPlannedWizardOpen = false
                Toast.makeText(context, "Yeni oturma planı başarıyla oluşturuldu ve uygulandı! 🎯", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 5. Oturma Planı Arşiv Diyaloğu
    if (isArchiveDialogOpen) {
        SeatingPlanArchiveDialog(
            classRoom = classRoom,
            students = students,
            isDarkTheme = isDarkTheme,
            onDismiss = { isArchiveDialogOpen = false },
            onApplyPlan = { archivedPlan ->
                onApplyArchivedPlan(archivedPlan)
                isArchiveDialogOpen = false
                Toast.makeText(context, "'${archivedPlan.name}' yüklendi ve uygulandı! 📂", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 6. Akıllı Dağıt Modları Diyaloğu (2 sütun x 4 satır kutu kutu)
    if (showAutoAssignDialog) {
        AutoAssignModalDialog(
            isDarkTheme = isDarkTheme,
            onDismiss = { showAutoAssignDialog = false },
            onSelectMode = { mode ->
                if (mode == SeatingPlacementMode.MANUAL) {
                    if (seatedCount > 0) {
                        showManualModeOptionsDialog = true
                    } else {
                        Toast.makeText(
                            context,
                            "👆 Elle Yerleştirme: Boş bir koltuğa dokunarak ayaktaki öğrencilerden dilediğinizi seçebilirsiniz.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    onAutoAssign(mode)
                }
            },
            onOpenPlannedWizard = {
                isPlannedWizardOpen = true
            }
        )
    }

    // 6B. Elle Yerleştirme Başlangıç Seçenekleri Diyaloğu
    if (showManualModeOptionsDialog) {
        AlertDialog(
            onDismissRequest = { showManualModeOptionsDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("👆", fontSize = 20.sp)
                    Column {
                        Text(
                            text = "Elle Yerleştirme Modu",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Nasıl devam etmek istediğinizi seçin",
                            fontSize = 11.5.sp,
                            color = Color.Gray
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Şu anda sınıfta $seatedCount öğrenci oturuyor, ${unassignedStudents.size} öğrenci ayakta. Boş koltuklara tıkladığınızda ayaktaki öğrencilerden dilediğinizi seçebilirsiniz.",
                        fontSize = 12.5.sp,
                        color = if (isDarkTheme) BirizDarkTextPrimary else Color(0xFF334155),
                        lineHeight = 17.sp
                    )
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            showManualModeOptionsDialog = false
                            onClearPlan(true)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BirizNavyPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_mode_clear_all_button")
                    ) {
                        Text(
                            text = "🧍 Tüm Sınıfı Ayağa Kaldır (Sıfırdan Elle Yerleştir)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    if (effectivePlanState.pinnedStudents.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                showManualModeOptionsDialog = false
                                onClearPlan(false)
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("manual_mode_keep_pinned_button")
                        ) {
                            Text(
                                text = "🔒 Kilitliler Kalsın, Diğerlerini Ayağa Kaldır",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            showManualModeOptionsDialog = false
                            Toast.makeText(
                                context,
                                "👆 Boş koltuklara dokunarak ayaktaki öğrencileri seçebilir, dolu koltuklara dokunarak değiştirebilirsiniz.",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_mode_keep_current_button")
                    ) {
                        Text(
                            text = "✏️ Mevcut Yerleşim Üzerinde Elle Düzenle",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualModeOptionsDialog = false }) {
                    Text("Kapat", fontSize = 12.sp)
                }
            }
        )
    }

    // 7. Planı Sıfırla Güvenlik Uyarısı Onay Diyaloğu
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Oturma Planını Sıfırla",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "Oturma planındaki yerleşimleri sıfırlamak istediğinize emin misiniz? Sabitlenmiş (🔒 kilitli) öğrencileri koruyabilir veya tüm koltukları tamamen boşaltabilirsiniz.",
                    fontSize = 13.sp,
                    color = if (isDarkTheme) BirizDarkTextPrimary else Color(0xFF334155),
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            showClearConfirmDialog = false
                            onClearPlan(false)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🔒 Kilitliler Kalsın, Diğerlerini Boşalt", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            showClearConfirmDialog = false
                            onClearPlan(true)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🗑️ Tüm Koltukları Boşalt (Tam Sıfırla)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    TextButton(
                        onClick = { showClearConfirmDialog = false },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Vazgeç", fontSize = 12.sp)
                    }
                }
            },
            dismissButton = null
        )
    }
}

/**
 * Sınıf Ön Cephesi: Ortalanmış Akıllı Tahta, Öğretmen Masası, Pencere ve Kapı Konumu (Kompakt Tek Satır)
 */
@Composable
private fun ClassroomFrontArchBanner(
    config: SeatingConfig,
    isDarkTheme: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isDarkTheme) Color(0x600E1A38) else Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Sol Duvar Elemanları (Ön Sol Kapı, Sol Pencere, Sol Öğretmen Masası)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (config.doorPosition == DoorPosition.ON_SOL || config.doorPosition == DoorPosition.SOL_ON) {
                    ArchitecturalBadge(label = "🚪 Kapı", isDarkTheme = isDarkTheme, isDoor = true)
                }
                if (config.windowPosition == WindowPosition.LEFT) {
                    ArchitecturalBadge(label = "🪟 Pencere", isDarkTheme = isDarkTheme)
                }
                if (config.deskPosition == DeskPosition.LEFT) {
                    TeacherDeskBadge(isDarkTheme = isDarkTheme)
                }
            }

            // Orta Duvar Elemanları (Ortalanmış Akıllı Tahta ve Orta Öğretmen Masası)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFF334155),
                    border = BorderStroke(1.dp, Color(0xFF475569))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "📋 AKILLI TAHTA",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }
                if (config.deskPosition == DeskPosition.CENTER) {
                    TeacherDeskBadge(isDarkTheme = isDarkTheme)
                }
            }

            // Sağ Duvar Elemanları (Sağ Öğretmen Masası, Sağ Pencere, Ön Sağ Kapı)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (config.deskPosition == DeskPosition.RIGHT) {
                    TeacherDeskBadge(isDarkTheme = isDarkTheme)
                }
                if (config.windowPosition == WindowPosition.RIGHT) {
                    ArchitecturalBadge(label = "🪟 Pencere", isDarkTheme = isDarkTheme)
                }
                if (config.doorPosition == DoorPosition.ON_SAG || config.doorPosition == DoorPosition.SAG_ON) {
                    ArchitecturalBadge(label = "🚪 Kapı", isDarkTheme = isDarkTheme, isDoor = true)
                }
            }
        }
    }
}

/**
 * Sınıf Arka Cephesi: Arka Pencereler, Arka Kapı ve Sınıf Panosu
 */
@Composable
private fun ClassroomBackArchBanner(
    config: SeatingConfig,
    isDarkTheme: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isDarkTheme) Color(0x400E1A38) else Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // Sol Arka Kapı
                if (config.doorPosition == DoorPosition.ARKA_SOL || config.doorPosition == DoorPosition.SOL_ARKA) {
                    Box(modifier = Modifier.align(Alignment.CenterStart)) {
                        ArchitecturalBadge(label = "🚪 Kapı (${config.doorPosition.positionOnFacade})", isDarkTheme = isDarkTheme, isDoor = true)
                    }
                }

                // Orta Alan: Arka Cephe ve Pencereler
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (config.windowPosition == WindowPosition.BACK) {
                        ArchitecturalBadge(label = "🪟 Pencereler (Arka Cephe Işık Alanı)", isDarkTheme = isDarkTheme)
                    }
                }

                // Sağ Arka Kapı
                if (config.doorPosition == DoorPosition.ARKA_SAG || config.doorPosition == DoorPosition.SAG_ARKA) {
                    Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                        ArchitecturalBadge(label = "🚪 Kapı (${config.doorPosition.positionOnFacade})", isDarkTheme = isDarkTheme, isDoor = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchitecturalBadge(label: String, isDarkTheme: Boolean, isDoor: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(7.dp),
        color = if (isDoor) {
            if (isDarkTheme) Color(0x40D97706) else Color(0xFFFEF3C7)
        } else {
            if (isDarkTheme) Color(0x400284C7) else Color(0xFFE0F2FE)
        },
        border = BorderStroke(1.dp, if (isDoor) Color(0xFFD97706) else Color(0xFF0284C7))
    ) {
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDoor) Color(0xFFB45309) else Color(0xFF0284C7),
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun TeacherDeskBadge(isDarkTheme: Boolean) {
    Surface(
        shape = RoundedCornerShape(7.dp),
        color = if (isDarkTheme) Color(0x40F59E0B) else Color(0xFFFFFBEB),
        border = BorderStroke(1.dp, Color(0xFFF59E0B))
    ) {
        Text(
            text = "👩‍🏫 Öğretmen Masası",
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFD97706),
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp)
        )
    }
}

/**
 * Tek bir Grup Kolonu (Örn: 1. Grup - 5 Sıra)
 */
@Composable
private fun GroupColumn(
    groupIndex: Int,
    totalRows: Int,
    peoplePerRow: Int,
    seatingPlanState: SeatingPlanState,
    students: List<Student>,
    swapSourceSeatId: String?,
    isStandingPlacementActive: Boolean = false,
    isDarkTheme: Boolean,
    onSeatClick: (String) -> Unit
) {
    val columnWidth = when (peoplePerRow) {
        1 -> 130.dp
        2 -> 230.dp
        3 -> 310.dp
        else -> 380.dp
    }

    Column(
        modifier = Modifier.width(columnWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Grup Başlığı
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isDarkTheme) Color(0x4013294B) else Color(0xFFE2E8F0),
            border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFFCBD5E1)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "${groupIndex + 1}. Grup • $totalRows Sıra",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 5.dp)
            )
        }

        // Sıralar (Önden Arkaya Doğru)
        for (rowIndex in 0 until totalRows) {
            DeskCard(
                groupIndex = groupIndex,
                rowIndex = rowIndex,
                peoplePerRow = peoplePerRow,
                seatingPlanState = seatingPlanState,
                students = students,
                swapSourceSeatId = swapSourceSeatId,
                isStandingPlacementActive = isStandingPlacementActive,
                isDarkTheme = isDarkTheme,
                onSeatClick = onSeatClick
            )
        }
    }
}

/**
 * Okul Sırası Bileşeni (Desk)
 */
@Composable
private fun DeskCard(
    groupIndex: Int,
    rowIndex: Int,
    peoplePerRow: Int,
    seatingPlanState: SeatingPlanState,
    students: List<Student>,
    swapSourceSeatId: String?,
    isStandingPlacementActive: Boolean = false,
    isDarkTheme: Boolean,
    onSeatClick: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) Color(0xFF13203E) else Color(0xFFF8FAFC)
        ),
        border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFFCBD5E1)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            // Sıra Sıralama Etiketi
            Text(
                text = if (rowIndex == 0) "1. Sıra (Ön)" else "${rowIndex + 1}. Sıra",
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (rowIndex == 0) BirizAmber else if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
                modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
            )

            // Koltuklar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (seatIndex in 0 until peoplePerRow) {
                    val seatId = "g$groupIndex-r$rowIndex-s$seatIndex"
                    val studentId = seatingPlanState.assignments[seatId]
                    val student = students.firstOrNull { it.id == studentId }
                    val isSelectedForSwap = swapSourceSeatId == seatId

                    SeatItem(
                        seatId = seatId,
                        student = student,
                        isPinned = student != null && seatingPlanState.pinnedStudents.contains(student.id),
                        isPriority = student != null && seatingPlanState.priorityStudents.contains(student.id),
                        isSelectedForSwap = isSelectedForSwap,
                        isStandingTarget = student == null && isStandingPlacementActive,
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier.weight(1f),
                        onClick = { onSeatClick(seatId) }
                    )
                }
            }
        }
    }
}

/**
 * Bireysel Koltuk / Öğrenci Kartı
 */
@Composable
private fun SeatItem(
    seatId: String,
    student: Student?,
    isPinned: Boolean,
    isPriority: Boolean,
    isSelectedForSwap: Boolean,
    isStandingTarget: Boolean = false,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isGirl = student?.gender.equals("Kız", ignoreCase = true)

    val backgroundColor = when {
        isSelectedForSwap -> if (isDarkTheme) Color(0xFF78350F) else Color(0xFFFEF3C7)
        isStandingTarget -> if (isDarkTheme) Color(0x3528B8C2) else Color(0xFFE0F7F6)
        student == null -> if (isDarkTheme) Color(0x20334E68) else Color.White
        isGirl -> if (isDarkTheme) Color(0x35BE185D) else Color(0xFFFDF2F8)
        else -> if (isDarkTheme) Color(0x350284C7) else Color(0xFFEFF6FF)
    }

    val borderColor = when {
        isSelectedForSwap -> BirizAmber
        isStandingTarget -> BirizTurquoise
        student == null -> if (isDarkTheme) Color(0x5564748B) else Color(0xFFCBD5E1)
        isGirl -> if (isDarkTheme) Color(0xFFF472B6) else Color(0xFFF472B6)
        else -> if (isDarkTheme) Color(0xFF60A5FA) else Color(0xFF93C5FD)
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        border = BorderStroke(if (isSelectedForSwap || isStandingTarget) 2.dp else 1.dp, borderColor),
        modifier = modifier
            .height(58.dp)
            .testTag("seat_item_$seatId")
    ) {
        if (student != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Üst Kısım: Okul No + Rozetler
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = student.studentNumber,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 11.sp,
                        color = if (isGirl) {
                            if (isDarkTheme) Color(0xFFF472B6) else Color(0xFFBE185D)
                        } else {
                            if (isDarkTheme) Color(0xFF93C5FD) else Color(0xFF0284C7)
                        }
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        if (isPinned) {
                            Text(text = "🔒", fontSize = 10.sp)
                        }
                        if (isPriority) {
                            Text(text = "👓", fontSize = 10.sp)
                        }
                    }
                }

                // Alt Kısım: Büyük Harfli Ad Soyad
                val upperFullName = TurkishTextHelper.toTurkishUpperCase(student.fullName)
                Text(
                    text = upperFullName,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) BirizDarkTextPrimary else Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            // Boş Koltuk
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = if (isStandingTarget) {
                            if (isDarkTheme) BirizTurquoise else Color(0xFF0F766E)
                        } else {
                            if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
                        },
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = if (isStandingTarget) "Buraya Oturt" else "Boş (Seç)",
                        fontSize = if (isStandingTarget) 10.sp else 10.5.sp,
                        color = if (isStandingTarget) {
                            if (isDarkTheme) BirizTurquoise else Color(0xFF0F766E)
                        } else {
                            if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Yerleşmemiş (Ayaktaki) Öğrenciler Yatay Tepsisi
 */
@Composable
private fun UnassignedStudentsSection(
    unassignedStudents: List<Student>,
    selectedStandingStudentId: String? = null,
    isDarkTheme: Boolean,
    onStudentClick: (Student) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isDarkTheme) Color(0x4013203E) else Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🧍 Ayaktaki Öğrenciler (${unassignedStudents.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                )
                Text(
                    text = if (selectedStandingStudentId != null) "Şimdi boş koltuğa dokunun" else "Boş koltuğa veya öğrenciye dokunun",
                    fontSize = 11.sp,
                    fontWeight = if (selectedStandingStudentId != null) FontWeight.Bold else FontWeight.Normal,
                    color = if (selectedStandingStudentId != null) BirizTurquoise else if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(unassignedStudents, key = { it.id }) { student ->
                    val isGirl = student.gender.equals("Kız", ignoreCase = true)
                    val isSelected = selectedStandingStudentId == student.id
                    Surface(
                        onClick = { onStudentClick(student) },
                        shape = RoundedCornerShape(10.dp),
                        color = when {
                            isSelected -> if (isDarkTheme) Color(0x4528B8C2) else Color(0xFFCCFBF1)
                            isGirl -> if (isDarkTheme) Color(0x30F472B6) else Color.White
                            else -> if (isDarkTheme) Color(0x3038BDF8) else Color.White
                        },
                        border = BorderStroke(
                            if (isSelected) 2.dp else 1.dp,
                            when {
                                isSelected -> BirizTurquoise
                                isGirl -> if (isDarkTheme) Color(0xFFF472B6) else Color(0xFFFCE7F3)
                                else -> if (isDarkTheme) Color(0xFF38BDF8) else Color(0xFFE0F2FE)
                            }
                        ),
                        modifier = Modifier.testTag("unassigned_student_${student.studentNumber}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isGirl) Color(0xFFFCE7F3) else Color(0xFFE0F2FE)
                            ) {
                                Text(
                                    text = student.studentNumber,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isGirl) Color(0xFFDB2777) else Color(0xFF0284C7),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = TurkishTextHelper.toTurkishUpperCase(student.fullName),
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = if (isDarkTheme) Color.White else Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSelected) "✓" else if (isGirl) "♀" else "♂",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFF0F766E) else if (isGirl) Color(0xFFEC4899) else Color(0xFF0EA5E9)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Koltuk Tıklandığında Açılan Detay ve İşlem Menüsü
 */
private data class SeatingModeOption(
    val id: String,
    val icon: String,
    val title: String,
    val description: String,
    val badgeColor: Color
)

/**
 * Koltuk Tıklandığında Açılan Detay ve Eylem Menüsü (Kutu Kutu Grid Tasarımı)
 */
@Composable
private fun SeatDetailDialog(
    seatId: String,
    student: Student,
    isPinned: Boolean,
    isPriority: Boolean,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onStartSwap: () -> Unit,
    onChangeStudentForSeat: () -> Unit = {},
    onTogglePin: () -> Unit,
    onTogglePriority: () -> Unit,
    onEmptySeat: () -> Unit
) {
    val isGirl = student.gender.equals("Kız", ignoreCase = true)
    val cardBg = if (isDarkTheme) BirizDarkCardGlass else Color(0xFFF8FAFC)
    val cardBorder = if (isDarkTheme) BirizDarkCardBorder else Color(0xFFE2E8F0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (isGirl) Color(0xFFFCE7F3) else Color(0xFFE0F2FE)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isGirl) "👧" else "👦",
                        fontSize = 22.sp
                    )
                }
                Column {
                    Text(
                        text = TurkishTextHelper.toTurkishUpperCase(student.fullName),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Okul No: ${student.studentNumber} • Cinsiyet: ${student.gender}",
                        fontSize = 11.5.sp,
                        color = Color.Gray
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Koltuk Konumu Rozeti
                val seatInfo = SeatingPlanHelper.parseSeatId(seatId)
                if (seatInfo != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDarkTheme) Color(0x30334E68) else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFFCBD5E1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "📍 Konum: ${seatInfo.groupIndex + 1}. Grup • ${seatInfo.rowIndex + 1}. Sıra • ${seatInfo.seatIndex + 1}. Koltuk",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDarkTheme) BirizTurquoise else Color(0xFF0F766E),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                // Hızlı Öğrenci Değiştir Butonu (Ayaktaki veya sınıftaki başka bir öğrenciyi doğrudan bu koltuğa seç)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDarkTheme) Color(0x2528B8C2) else Color(0xFFE0F7F6),
                    border = BorderStroke(1.dp, BirizTurquoise),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChangeStudentForSeat() }
                        .testTag("dialog_change_student_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BirizTurquoise.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🧍", fontSize = 16.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Bu Koltuğa Başka Öğrenci Seç",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isDarkTheme) Color.White else Color(0xFF0F766E)
                            )
                            Text(
                                text = "Ayaktaki öğrencilerden veya sınıftan bu koltuğa öğrenci ata",
                                fontSize = 10.sp,
                                color = if (isDarkTheme) Color(0xFFBAE6FD) else Color(0xFF0D9488)
                            )
                        }
                    }
                }

                // 2 Sütun x 2 Satır Eylem Kutuları (Bütünleşik & Uyumlu Tasarım)
                // 1. Satır: Yer Değiştir (Takas) & Sıraya Kilitle / Serbest Bırak
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Kutucuk 1: Yer Değiştir (Takas)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, cardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(68.dp)
                            .clickable { onStartSwap() }
                            .testTag("dialog_swap_seat_button")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF3B82F6).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🔀", fontSize = 16.sp)
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Yer Değiştir",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Başka koltukla takas",
                                    fontSize = 9.sp,
                                    color = Color.Gray,
                                    lineHeight = 11.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Kutucuk 2: Koltuk Kilidi (Pin)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, if (isPinned) BirizAmber else cardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(68.dp)
                            .clickable { onTogglePin() }
                            .testTag("dialog_toggle_pin_button")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF59E0B).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (isPinned) "🔓" else "🔒", fontSize = 16.sp)
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (isPinned) "Kilidi Kaldır" else "Koltuğa Kilitle",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = if (isPinned) BirizAmber else Color.Unspecified,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isPinned) "Serbest bırak" else "Dağıtımda sabit kalır",
                                    fontSize = 9.sp,
                                    color = Color.Gray,
                                    lineHeight = 11.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // 2. Satır: Ön Sıra Önceliği & Koltuğu Boşalt
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Kutucuk 3: Ön Sıra Önceliği (Gözlük/Boy)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, if (isPriority) Color(0xFF8B5CF6) else cardBorder),
                        modifier = Modifier
                            .weight(1f)
                            .height(68.dp)
                            .clickable { onTogglePriority() }
                            .testTag("dialog_toggle_priority_button")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF8B5CF6).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("👓", fontSize = 16.sp)
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (isPriority) "Önceliği Kaldır" else "Ön Sıra Önceliği",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = if (isPriority) Color(0xFF8B5CF6) else Color.Unspecified,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isPriority) "Öncelik aktif" else "Gözlük, boy, duyma",
                                    fontSize = 9.sp,
                                    color = Color.Gray,
                                    lineHeight = 11.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Kutucuk 4: Koltuğu Boşalt
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDarkTheme) Color(0x20EF4444) else Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, if (isDarkTheme) Color(0x50EF4444) else Color(0xFFFECACA)),
                        modifier = Modifier
                            .weight(1f)
                            .height(68.dp)
                            .clickable { onEmptySeat() }
                            .testTag("dialog_empty_seat_button")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFEF4444).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🗑️", fontSize = 16.sp)
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Koltuğu Boşalt",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    color = Color(0xFFDC2626),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Sıradan kaldır",
                                    fontSize = 9.sp,
                                    color = Color(0xFFB91C1C),
                                    lineHeight = 11.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat", fontSize = 12.5.sp)
            }
        }
    )
}

/**
 * Akıllı Dağıtım Modları Penceresi (2 Sütun x 3 Satır Kutu Kutu Düzen)
 */
@Composable
private fun AutoAssignModalDialog(
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onSelectMode: (SeatingPlacementMode) -> Unit,
    onOpenPlannedWizard: () -> Unit
) {
    val modes = listOf(
        SeatingModeOption(
            id = "planned",
            icon = "🪄",
            title = "Planlı Yerleştirme",
            description = "Özel kurallı yerleştirme sihirbazı",
            badgeColor = Color(0xFF8B5CF6)
        ),
        SeatingModeOption(
            id = "random",
            icon = "🎲",
            title = "Rastgele Dağıt",
            description = "Tüm sınıfı karma yerleştir",
            badgeColor = Color(0xFF3B82F6)
        ),
        SeatingModeOption(
            id = "gender_balanced",
            icon = "👫",
            title = "Kız - Erkek Dengeli",
            description = "Sıralara dengeli cinsiyet",
            badgeColor = Color(0xFFEC4899)
        ),
        SeatingModeOption(
            id = "alphabetical",
            icon = "🔤",
            title = "Alfabetik (A-Z)",
            description = "Öğrenci adlarına göre sıra",
            badgeColor = Color(0xFF10B981)
        ),
        SeatingModeOption(
            id = "number_order",
            icon = "🔢",
            title = "Okul No Sıralı",
            description = "Numara sırasıyla yerleştir",
            badgeColor = Color(0xFFF59E0B)
        ),
        SeatingModeOption(
            id = "front_to_back",
            icon = "⬇️",
            title = "Önden Arkaya Kaydır",
            description = "Sıralar 1 kademe arkaya kayar",
            badgeColor = Color(0xFF0284C7)
        ),
        SeatingModeOption(
            id = "groups_shift",
            icon = "↔️",
            title = "Grupları Yana Kaydır",
            description = "Gruplar sırayla yana kayar",
            badgeColor = Color(0xFF0D9488)
        ),
        SeatingModeOption(
            id = "manual",
            icon = "👆",
            title = "Elle Yerleştirme",
            description = "Sıralara dokunarak kendiniz seçin",
            badgeColor = Color(0xFF06B6D4)
        )
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Column {
                    Text("Akıllı Dağıtım Modları", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Yerleştirme veya kaydırma modunu seçin", fontSize = 11.sp, color = Color.Gray)
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                for (row in 0 until 4) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (col in 0 until 2) {
                            val index = row * 2 + col
                            val item = modes[index]
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDarkTheme) BirizDarkCardGlass else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(68.dp)
                                    .clickable {
                                        onDismiss()
                                        when (item.id) {
                                            "planned" -> onOpenPlannedWizard()
                                            "random" -> onSelectMode(SeatingPlacementMode.RANDOM)
                                            "gender_balanced" -> onSelectMode(SeatingPlacementMode.GENDER_BALANCED)
                                            "alphabetical" -> onSelectMode(SeatingPlacementMode.ALPHABETICAL)
                                            "number_order" -> onSelectMode(SeatingPlacementMode.NUMBER_ORDER)
                                            "front_to_back" -> onSelectMode(SeatingPlacementMode.ROTATE_FRONT_TO_BACK)
                                            "groups_shift" -> onSelectMode(SeatingPlacementMode.ROTATE_GROUPS_SHIFT)
                                            "manual" -> onSelectMode(SeatingPlacementMode.MANUAL)
                                        }
                                    }
                                    .testTag("mode_${item.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(item.badgeColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(item.icon, fontSize = 16.sp)
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = item.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = item.description,
                                            fontSize = 9.sp,
                                            color = Color.Gray,
                                            lineHeight = 11.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat", fontSize = 12.sp)
            }
        }
    )
}

/**
 * Boş (veya Seçilen) Koltuğa Öğrenci Seçme Diyaloğu:
 * - Ayaktaki (yerleşmemiş) öğrencileri varsayılan sekmede gösterir; boş koltuğa tıklanır tıklanmaz
 *   öğretmenin ayaktaki öğrencilerden dilediğini seçip yerleştirmesine olanak tanır.
 * - İkinci sekmeden ("Oturanlardan Taşı") başka bir sırada oturan öğrencinin de bu koltuğa taşınmasını destekler.
 * - Arama, cinsiyet filtresi, yan sıra arkadaşı bilgisi ve "Bu koltuğa sabitle (kilitle)" seçeneği içerir.
 */
@Composable
private fun AssignStudentDialog(
    seatId: String,
    unassignedStudents: List<Student>,
    allStudents: List<Student>,
    seatingPlanState: SeatingPlanState,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onSelectStudent: (studentId: String, shouldPin: Boolean) -> Unit
) {
    val seatInfo = SeatingPlanHelper.parseSeatId(seatId)
    var searchQuery by remember { mutableStateOf("") }
    // 0 = Ayaktaki Öğrenciler, 1 = Oturanlar / Tüm Sınıf
    var activeTab by remember(unassignedStudents.size) {
        mutableIntStateOf(if (unassignedStudents.isNotEmpty()) 0 else 1)
    }
    // "ALL", "GIRL", "BOY"
    var genderFilter by remember { mutableStateOf("ALL") }
    var pinOnAssign by remember { mutableStateOf(false) }

    val seatedStudents = remember(allStudents, unassignedStudents) {
        val unassignedIds = unassignedStudents.map { it.id }.toSet()
        allStudents
            .filter { !unassignedIds.contains(it.id) }
            .sortedBy { it.studentNumber.toIntOrNull() ?: 9999 }
    }

    val deskNeighbors = remember(seatId, seatingPlanState, allStudents) {
        if (seatInfo == null) emptyList()
        else {
            (0 until seatingPlanState.config.peoplePerRow)
                .filter { it != seatInfo.seatIndex }
                .mapNotNull { sIdx ->
                    val neighborSeatId = "g${seatInfo.groupIndex}-r${seatInfo.rowIndex}-s$sIdx"
                    val neighborStudentId = seatingPlanState.assignments[neighborSeatId]
                    allStudents.firstOrNull { it.id == neighborStudentId }
                }
        }
    }

    val seatByStudentId = remember(seatingPlanState.assignments) {
        seatingPlanState.assignments.entries.associate { (sId, stId) -> stId to sId }
    }

    val baseList = if (activeTab == 0) unassignedStudents else seatedStudents
    val filteredList = remember(baseList, searchQuery, genderFilter) {
        val q = TurkishTextHelper.toTurkishUpperCase(searchQuery.trim())
        baseList.filter { st ->
            val matchesGender = when (genderFilter) {
                "GIRL" -> st.gender.equals("Kız", ignoreCase = true)
                "BOY" -> !st.gender.equals("Kız", ignoreCase = true)
                else -> true
            }
            val matchesQuery = q.isBlank() ||
                st.studentNumber.contains(q, ignoreCase = true) ||
                TurkishTextHelper.toTurkishUpperCase(st.fullName).contains(q)
            matchesGender && matchesQuery
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "🪑 Koltuğa Öğrenci Seç",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
                if (seatInfo != null) {
                    Text(
                        text = "📍 ${seatInfo.groupIndex + 1}. Grup • ${seatInfo.rowIndex + 1}. Sıra • ${seatInfo.seatIndex + 1}. Koltuk",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) BirizTurquoise else Color(0xFF0F766E)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Sıra arkadaşı bilgisi
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDarkTheme) Color(0x30334E68) else Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val neighborText = if (deskNeighbors.isEmpty()) {
                        "🪑 Bu sıradaki yan koltuk şu an boş"
                    } else {
                        "🤝 Sıra Arkadaşı: " + deskNeighbors.joinToString(", ") {
                            "${it.studentNumber} ${TurkishTextHelper.toTurkishUpperCase(it.fullName)} (${it.gender})"
                        }
                    }
                    Text(
                        text = neighborText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDarkTheme) Color.White else Color(0xFF334155),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                // Sekme Seçici: Ayaktaki Öğrenciler vs Oturanlardan Taşı
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        onClick = { activeTab = 0 },
                        shape = RoundedCornerShape(9.dp),
                        color = if (activeTab == 0) BirizNavyPrimary else if (isDarkTheme) Color(0x25334E68) else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, if (activeTab == 0) BirizNavyPrimary else if (isDarkTheme) BirizDarkCardBorder else Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("assign_tab_standing")
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = "🧍 Ayaktakiler (${unassignedStudents.size})",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeTab == 0) Color.White else if (isDarkTheme) Color.White else Color(0xFF334155)
                            )
                        }
                    }

                    Surface(
                        onClick = { activeTab = 1 },
                        shape = RoundedCornerShape(9.dp),
                        color = if (activeTab == 1) BirizNavyPrimary else if (isDarkTheme) Color(0x25334E68) else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, if (activeTab == 1) BirizNavyPrimary else if (isDarkTheme) BirizDarkCardBorder else Color(0xFFCBD5E1)),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("assign_tab_seated")
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = "🪑 Oturanlar (${seatedStudents.size})",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeTab == 1) Color.White else if (isDarkTheme) Color.White else Color(0xFF334155)
                            )
                        }
                    }
                }

                // Arama Çubuğu
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Öğrenci adı veya okul no ara...", fontSize = 11.5.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Temizle", modifier = Modifier.size(15.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BirizTurquoise,
                        unfocusedBorderColor = if (isDarkTheme) BirizDarkCardBorder else BirizBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("assign_student_search_input")
                )

                // Cinsiyet Filtresi + Sabitleme Seçeneği
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            "ALL" to "Tümü",
                            "GIRL" to "👧 Kız",
                            "BOY" to "👦 Erkek"
                        ).forEach { (code, label) ->
                            val selected = genderFilter == code
                            Surface(
                                onClick = { genderFilter = code },
                                shape = RoundedCornerShape(7.dp),
                                color = if (selected) BirizTurquoise.copy(alpha = 0.2f) else Color.Transparent,
                                border = BorderStroke(1.dp, if (selected) BirizTurquoise else if (isDarkTheme) BirizDarkCardBorder else Color(0xFFCBD5E1))
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) (if (isDarkTheme) BirizTurquoise else Color(0xFF0F766E)) else Color.Gray,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { pinOnAssign = !pinOnAssign }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Checkbox(
                            checked = pinOnAssign,
                            onCheckedChange = { pinOnAssign = it },
                            colors = CheckboxDefaults.colors(checkedColor = BirizAmber),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "🔒 Kilitle",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (pinOnAssign) BirizAmber else Color.Gray
                        )
                    }
                }

                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (activeTab == 0 && unassignedStudents.isEmpty()) {
                                    "🎉 Ayakta öğrenci kalmadı, tüm öğrenciler yerleşti!"
                                } else {
                                    "Arama kriterine uygun öğrenci bulunamadı."
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                            if (activeTab == 0 && unassignedStudents.isEmpty() && seatedStudents.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = { activeTab = 1 },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("🪑 Oturan Öğrencilerden Seç (${seatedStudents.size})", fontSize = 11.5.sp)
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredList, key = { it.id }) { student ->
                            val isGirl = student.gender.equals("Kız", ignoreCase = true)
                            val currentSeatId = seatByStudentId[student.id]
                            val currentCoord = currentSeatId?.let { SeatingPlanHelper.parseSeatId(it) }
                            val isPriority = seatingPlanState.priorityStudents.contains(student.id) || student.isPriority

                            Surface(
                                onClick = { onSelectStudent(student.id, pinOnAssign) },
                                shape = RoundedCornerShape(10.dp),
                                color = when {
                                    isGirl -> if (isDarkTheme) Color(0x30F472B6) else Color.White
                                    else -> if (isDarkTheme) Color(0x3038BDF8) else Color.White
                                },
                                border = BorderStroke(
                                    1.dp,
                                    when {
                                        isGirl -> if (isDarkTheme) Color(0xFFF472B6) else Color(0xFFFCE7F3)
                                        else -> if (isDarkTheme) Color(0xFF38BDF8) else Color(0xFFE0F2FE)
                                    }
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("assign_dialog_student_${student.studentNumber}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(5.dp),
                                            color = if (isGirl) Color(0xFFFCE7F3) else Color(0xFFE0F2FE)
                                        ) {
                                            Text(
                                                text = student.studentNumber,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isGirl) Color(0xFFDB2777) else Color(0xFF0284C7),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = TurkishTextHelper.toTurkishUpperCase(student.fullName),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = if (isDarkTheme) Color.White else Color(0xFF1E293B),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (isPriority) {
                                                    Text("👓", fontSize = 11.sp)
                                                }
                                            }
                                            if (currentCoord != null) {
                                                Text(
                                                    text = "Şu an: ${currentCoord.groupIndex + 1}. Grup • ${currentCoord.rowIndex + 1}. Sıra",
                                                    fontSize = 10.sp,
                                                    color = BirizAmber,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (currentCoord != null) {
                                            if (isDarkTheme) Color(0x35F59E0B) else Color(0xFFFEF3C7)
                                        } else {
                                            if (isDarkTheme) Color(0x3528B8C2) else Color(0xFFE0F7F6)
                                        }
                                    ) {
                                        Text(
                                            text = if (currentCoord != null) "↔️ Buraya Taşı" else "➕ Oturt",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (currentCoord != null) Color(0xFFB45309) else Color(0xFF0F766E),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Vazgeç")
            }
        }
    )
}

/**
 * Sınıf Mimarisi & Sıra Ayarları Diyaloğu
 */
@Composable
private fun SeatingConfigDialog(
    currentConfig: SeatingConfig,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onSave: (SeatingConfig) -> Unit
) {
    var groupCount by remember(currentConfig) { mutableIntStateOf(currentConfig.groupCount) }
    var peoplePerRow by remember(currentConfig) { mutableIntStateOf(currentConfig.peoplePerRow) }
    var rowsPerGroup by remember(currentConfig) {
        mutableStateOf(currentConfig.rowsPerGroup.toMutableList().also { list ->
            while (list.size < 6) list.add(5)
        })
    }
    var windowPosition by remember(currentConfig) { mutableStateOf(currentConfig.windowPosition) }
    var doorPosition by remember(currentConfig) { mutableStateOf(currentConfig.doorPosition) }
    var selectedDoorFacade by remember(currentConfig) { mutableStateOf(currentConfig.doorPosition.facade) }
    var deskPosition by remember(currentConfig) { mutableStateOf(currentConfig.deskPosition) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Sıra Düzeni Ayarları", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Grup Sayısı (Varsayılan 3 - Esnek Artırma / Azaltma)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Grup Sayısı:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Sınıftaki sıra kolonları", fontSize = 11.sp, color = Color.Gray)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            onClick = { if (groupCount > 1) groupCount-- },
                            enabled = groupCount > 1,
                            shape = CircleShape,
                            color = if (groupCount > 1) (if (isDarkTheme) Color(0x30334E68) else Color(0xFFF1F5F9)) else Color.Transparent,
                            border = BorderStroke(1.dp, if (groupCount > 1) (if (isDarkTheme) BirizDarkCardBorder else BirizBorder) else Color.Transparent),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Azalt",
                                    tint = if (groupCount > 1) (if (isDarkTheme) Color.White else Color(0xFF0F172A)) else Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Text(
                            text = "$groupCount Grup",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Surface(
                            onClick = {
                                if (groupCount < 6) {
                                    groupCount++
                                    while (rowsPerGroup.size < groupCount) {
                                        rowsPerGroup.add(5)
                                    }
                                }
                            },
                            enabled = groupCount < 6,
                            shape = CircleShape,
                            color = if (groupCount < 6) (if (isDarkTheme) Color(0x30334E68) else Color(0xFFF1F5F9)) else Color.Transparent,
                            border = BorderStroke(1.dp, if (groupCount < 6) (if (isDarkTheme) BirizDarkCardBorder else BirizBorder) else Color.Transparent),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Arttır",
                                    tint = if (groupCount < 6) (if (isDarkTheme) Color.White else Color(0xFF0F172A)) else Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // 2. Sıra Başına Oturan Öğrenci (Varsayılan 2 - İkili, Esnek + -)
                val peopleLabels = mapOf(1 to "1 (Tekli)", 2 to "2 (İkili)", 3 to "3 (Üçlü)", 4 to "4 (Dörtlü)")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Sıra Başına Öğrenci:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Sıradaki koltuk sayısı", fontSize = 11.sp, color = Color.Gray)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            onClick = { if (peoplePerRow > 1) peoplePerRow-- },
                            enabled = peoplePerRow > 1,
                            shape = CircleShape,
                            color = if (peoplePerRow > 1) (if (isDarkTheme) Color(0x30334E68) else Color(0xFFF1F5F9)) else Color.Transparent,
                            border = BorderStroke(1.dp, if (peoplePerRow > 1) (if (isDarkTheme) BirizDarkCardBorder else BirizBorder) else Color.Transparent),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Azalt",
                                    tint = if (peoplePerRow > 1) (if (isDarkTheme) Color.White else Color(0xFF0F172A)) else Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Text(
                            text = peopleLabels[peoplePerRow] ?: "$peoplePerRow Öğrenci",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Surface(
                            onClick = { if (peoplePerRow < 4) peoplePerRow++ },
                            enabled = peoplePerRow < 4,
                            shape = CircleShape,
                            color = if (peoplePerRow < 4) (if (isDarkTheme) Color(0x30334E68) else Color(0xFFF1F5F9)) else Color.Transparent,
                            border = BorderStroke(1.dp, if (peoplePerRow < 4) (if (isDarkTheme) BirizDarkCardBorder else BirizBorder) else Color.Transparent),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Arttır",
                                    tint = if (peoplePerRow < 4) (if (isDarkTheme) Color.White else Color(0xFF0F172A)) else Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // 3. Her Grup İçin Sıra Sayısı
                Column {
                    Text("Her Grup İçin Sıra Sayısı:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    for (g in 0 until groupCount) {
                        val currentRows = rowsPerGroup.getOrElse(g) { 5 }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${g + 1}. Grup Sıra Sayısı:", fontSize = 12.sp)

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (currentRows > 1) {
                                            val newList = rowsPerGroup.toMutableList()
                                            newList[g] = currentRows - 1
                                            rowsPerGroup = newList
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Remove, contentDescription = "Azalt")
                                }

                                Text(
                                    text = "$currentRows Sıra",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )

                                IconButton(
                                    onClick = {
                                        if (currentRows < 8) {
                                            val newList = rowsPerGroup.toMutableList()
                                            newList[g] = currentRows + 1
                                            rowsPerGroup = newList
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = "Arttır")
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                // 4. Pencere Konumu (Sağ, Sol, Arka, Penceresiz)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🪟 Pencere Konumu:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            WindowPosition.LEFT to "Sol Cephe",
                            WindowPosition.RIGHT to "Sağ Cephe",
                            WindowPosition.BACK to "Arka Cephe",
                            WindowPosition.NONE to "Penceresiz"
                        ).forEach { (pos, label) ->
                            FilterChip(
                                selected = windowPosition == pos,
                                onClick = { windowPosition = pos },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // 5. Kapı Konumu (Cephe ve Detaylı Konum Seçenekleri)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🚪 Kapı Cephesi ve Konumu:", fontWeight = FontWeight.Bold, fontSize = 13.sp)

                    // Aşama 1: Kapı Cephesi
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Sağ" to "Sağ Cephe", "Sol" to "Sol Cephe", "Ön" to "Ön Cephe", "Arka" to "Arka Cephe", "Yok" to "Kapısız").forEach { (facadeCode, label) ->
                            FilterChip(
                                selected = selectedDoorFacade == facadeCode,
                                onClick = {
                                    selectedDoorFacade = facadeCode
                                    doorPosition = when (facadeCode) {
                                        "Sağ" -> DoorPosition.SAG_ON
                                        "Sol" -> DoorPosition.SOL_ON
                                        "Ön" -> DoorPosition.ON_SAG
                                        "Arka" -> DoorPosition.ARKA_SAG
                                        else -> DoorPosition.NONE
                                    }
                                },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    // Aşama 2: Seçilen Cepheye Göre Konum (Sağ/Sol için Ön/Arka; Ön/Arka için Sol/Sağ)
                    if (selectedDoorFacade != "Yok") {
                        val subOptions = when (selectedDoorFacade) {
                            "Sağ" -> listOf(DoorPosition.SAG_ON to "Sağ Ön", DoorPosition.SAG_ARKA to "Sağ Arka")
                            "Sol" -> listOf(DoorPosition.SOL_ON to "Sol Ön", DoorPosition.SOL_ARKA to "Sol Arka")
                            "Ön" -> listOf(DoorPosition.ON_SOL to "Ön Sol", DoorPosition.ON_SAG to "Ön Sağ")
                            "Arka" -> listOf(DoorPosition.ARKA_SOL to "Arka Sol", DoorPosition.ARKA_SAG to "Arka Sağ")
                            else -> emptyList()
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            subOptions.forEach { (pos, label) ->
                                FilterChip(
                                    selected = doorPosition == pos,
                                    onClick = { doorPosition = pos },
                                    label = { Text("📍 $label", fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }

                // 6. Öğretmen Masası Konumu
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("👩‍🏫 Öğretmen Masası Konumu:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            DeskPosition.LEFT to "Sol Ön",
                            DeskPosition.CENTER to "Orta Ön",
                            DeskPosition.RIGHT to "Sağ Ön"
                        ).forEach { (pos, label) ->
                            FilterChip(
                                selected = deskPosition == pos,
                                onClick = { deskPosition = pos },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalRows = rowsPerGroup.take(groupCount)
                    onSave(
                        currentConfig.copy(
                            groupCount = groupCount,
                            peoplePerRow = peoplePerRow,
                            rowsPerGroup = finalRows,
                            windowPosition = windowPosition,
                            doorPosition = doorPosition,
                            deskPosition = deskPosition
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = BirizNavyPrimary,
                    contentColor = Color.White
                )
            ) {
                Text("Sıra Düzenini Güncelle")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Vazgeç")
            }
        }
    )
}
