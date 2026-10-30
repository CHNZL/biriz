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
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoorBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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

    // Seçilen koltuk detay diyaloğu
    var selectedSeatForDetail by remember { mutableStateOf<String?>(null) }

    // Boş koltuğa öğrenci atama diyaloğu
    var selectedSeatForAssignment by remember { mutableStateOf<String?>(null) }

    // Otomatik dağıtım menüsü
    var showAutoAssignMenu by remember { mutableStateOf(false) }

    // Paylaşım menüsü
    var isShareMenuOpen by remember { mutableStateOf(false) }

    // Planlı Yerleştirme Sihirbazı
    var isPlannedWizardOpen by remember { mutableStateOf(false) }

    // Oturma Planı Arşiv Diyaloğu
    var isArchiveDialogOpen by remember { mutableStateOf(false) }

    // Boşaltma menüsü
    var showClearMenu by remember { mutableStateOf(false) }

    // Yerleşen ve Yerleşmemiş Öğrenciler
    val assignedStudentIds = remember(seatingPlanState.assignments) {
        seatingPlanState.assignments.values.filter { it.isNotBlank() }.toSet()
    }
    val unassignedStudents = remember(students, assignedStudentIds) {
        students.filter { !assignedStudentIds.contains(it.id) }
    }
    val seatedCount = assignedStudentIds.size
    val totalSeats = seatingPlanState.config.totalSeats

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
                // İstatistik Rozetleri & Sıra Düzeni & Paylaş
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDarkTheme) Color(0x3028B8C2) else Color(0xFFE0F7F6),
                            border = BorderStroke(1.dp, BirizTurquoise)
                        ) {
                            Text(
                                text = "💺 Yerleşen: $seatedCount / ${students.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) BirizTurquoise else Color(0xFF0F766E),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDarkTheme) Color(0x30F59E0B) else Color(0xFFFEF3C7),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B))
                        ) {
                            val emptySeats = (totalSeats - seatedCount).coerceAtLeast(0)
                            Text(
                                text = "Boş: $emptySeats",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) BirizAmber else Color(0xFFB45309),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            )
                        }

                        // Sıra Düzeni (Boş kutusunun hemen yanında)
                        OutlinedButton(
                            onClick = onOpenConfigDialog,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = primaryTextColor
                            ),
                            border = BorderStroke(1.dp, cardBorder),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier
                                .height(28.dp)
                                .testTag("seating_config_open_button")
                        ) {
                            Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sıra Düzeni", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Standart Paylaş Butonu ve Menüsü
                    Box {
                        Button(
                            onClick = { isShareMenuOpen = true },
                            colors = ButtonDefaults.buttonColors(containerColor = BirizNavyPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("seating_share_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Paylaş",
                                modifier = Modifier.size(14.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Paylaş", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

                // Aksiyon Butonları (Akıllı Dağıt, Arşiv, Boşalt)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Akıllı Dağıt Açılır Menüsü
                    Box {
                        Button(
                            onClick = { showAutoAssignMenu = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDarkTheme) BirizNavyLight else BirizNavyPrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("seating_auto_assign_menu_button")
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Akıllı Dağıt", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                        }

                        DropdownMenu(
                            expanded = showAutoAssignMenu,
                            onDismissRequest = { showAutoAssignMenu = false }
                        ) {
                            // 1) Planlı Yerleştirme
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Planlı Yerleştirme", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Açılan pencerede gerekli ayarlar yapılarak yerleştirme yapılır.", fontSize = 11.sp, color = Color.Gray)
                                    }
                                },
                                onClick = {
                                    showAutoAssignMenu = false
                                    isPlannedWizardOpen = true
                                },
                                modifier = Modifier.testTag("mode_planned")
                            )

                            // 2) Rastgele Dağıt
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Rastgele Dağıt", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Tüm Öğrenciler rastgele dağıtılır.", fontSize = 11.sp, color = Color.Gray)
                                    }
                                },
                                onClick = {
                                    showAutoAssignMenu = false
                                    onAutoAssign(SeatingPlacementMode.RANDOM)
                                },
                                modifier = Modifier.testTag("mode_random")
                            )

                            // 3) Kız Erkek Dengeli
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Kız Erkek Dengeli", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Kız ve erkek öğrenciler sıralara dengeli şekilde dağıtılır.", fontSize = 11.sp, color = Color.Gray)
                                    }
                                },
                                onClick = {
                                    showAutoAssignMenu = false
                                    onAutoAssign(SeatingPlacementMode.GENDER_BALANCED)
                                },
                                modifier = Modifier.testTag("mode_gender_balanced")
                            )

                            // 4) İsme Göre
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("İsme Göre", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Öğrenciler alfabetik (A-Z) sırayla yerleştirilir.", fontSize = 11.sp, color = Color.Gray)
                                    }
                                },
                                onClick = {
                                    showAutoAssignMenu = false
                                    onAutoAssign(SeatingPlacementMode.ALPHABETICAL)
                                },
                                modifier = Modifier.testTag("mode_alphabetical")
                            )

                            // 5) Numaraya Göre
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Numaraya Göre", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Öğrenciler okul numarası sırasıyla yerleştirilir.", fontSize = 11.sp, color = Color.Gray)
                                    }
                                },
                                onClick = {
                                    showAutoAssignMenu = false
                                    onAutoAssign(SeatingPlacementMode.NUMBER_ORDER)
                                },
                                modifier = Modifier.testTag("mode_number_order")
                            )

                            // 6) Elle Yerleştirme
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Elle Yerleştirme", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Öğrenciler tek tek öğretmenin tercihine göre yerleştirilir.", fontSize = 11.sp, color = Color.Gray)
                                    }
                                },
                                onClick = {
                                    showAutoAssignMenu = false
                                    Toast.makeText(
                                        context,
                                        "Elle Yerleştirme: Boş bir sıraya tıklayarak öğrenci yerleştirebilir, oturan öğrenciye tıklayarak sırayı boşaltabilirsiniz.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                },
                                modifier = Modifier.testTag("mode_manual")
                            )
                        }
                    }

                    // 2. Arşiv Butonu
                    OutlinedButton(
                        onClick = { isArchiveDialogOpen = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = primaryTextColor
                        ),
                        border = BorderStroke(1.dp, cardBorder),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("seating_archive_button")
                    ) {
                        Icon(imageVector = Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Arşiv", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // 3. Rotasyon Butonu (Önden Arkaya Kaydır & Grupları Çapraz Döndür)
                    var showRotationMenu by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { showRotationMenu = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = primaryTextColor
                            ),
                            border = BorderStroke(1.dp, cardBorder),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("seating_rotation_menu_button")
                        ) {
                            Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rotasyon", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                        }

                        DropdownMenu(
                            expanded = showRotationMenu,
                            onDismissRequest = { showRotationMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("⬇️ Önden Arkaya Kaydır", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Sıralar 1 arkaya kayar, en arkadakiler en öne geçer.", fontSize = 11.sp, color = Color.Gray)
                                    }
                                },
                                onClick = {
                                    showRotationMenu = false
                                    val newAssignments = SeatingPlanHelper.rotateSeatsFrontToBack(
                                        config = seatingPlanState.config,
                                        currentAssignments = seatingPlanState.assignments,
                                        pinnedStudents = seatingPlanState.pinnedStudents
                                    )
                                    val updated = seatingPlanState.copy(assignments = newAssignments)
                                    onApplyPlannedSeating(updated)
                                    Toast.makeText(context, "Sıralar önden arkaya kaydırıldı! 🔄", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.testTag("rotation_front_to_back")
                            )
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("🔀 Grupları Döndür (Sol → Orta → Sağ)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Gruplar yana kayar, adil oturma dengesi sağlanır.", fontSize = 11.sp, color = Color.Gray)
                                    }
                                },
                                onClick = {
                                    showRotationMenu = false
                                    val newAssignments = SeatingPlanHelper.rotateGroups(
                                        config = seatingPlanState.config,
                                        currentAssignments = seatingPlanState.assignments,
                                        pinnedStudents = seatingPlanState.pinnedStudents
                                    )
                                    val updated = seatingPlanState.copy(assignments = newAssignments)
                                    onApplyPlannedSeating(updated)
                                    Toast.makeText(context, "Gruplar yana döndürüldü! 🔀", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.testTag("rotation_groups")
                            )
                        }
                    }

                    // 4. Planı Boşalt Butonu
                    Box {
                        OutlinedButton(
                            onClick = { showClearMenu = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFFDC2626)
                            ),
                            border = BorderStroke(1.dp, if (isDarkTheme) Color(0x60EF4444) else Color(0xFFFECACA)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("seating_clear_plan_menu_button")
                        ) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Boşalt", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        DropdownMenu(
                            expanded = showClearMenu,
                            onDismissRequest = { showClearMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Sabitlenenler Hariç Boşalt (🔒 Kilitliler Kalır)") },
                                onClick = {
                                    showClearMenu = false
                                    onClearPlan(false)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Tüm Koltukları Boşalt", color = Color.Red, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    showClearMenu = false
                                    onClearPlan(true)
                                }
                            )
                        }
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
            val sourceStudentId = seatingPlanState.assignments[swapSourceSeatId]
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
                    config = seatingPlanState.config,
                    isDarkTheme = isDarkTheme
                )

                Spacer(modifier = Modifier.height(14.dp))

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
                        val config = seatingPlanState.config
                        for (groupIndex in 0 until config.groupCount) {
                            val rowsInGroup = config.rowsPerGroup.getOrElse(groupIndex) { 5 }

                            GroupColumn(
                                groupIndex = groupIndex,
                                totalRows = rowsInGroup,
                                peoplePerRow = config.peoplePerRow,
                                seatingPlanState = seatingPlanState,
                                students = students,
                                swapSourceSeatId = swapSourceSeatId,
                                isDarkTheme = isDarkTheme,
                                onSeatClick = { seatId ->
                                    if (swapSourceSeatId != null) {
                                        if (swapSourceSeatId != seatId) {
                                            onSwapSeats(swapSourceSeatId!!, seatId)
                                        }
                                        swapSourceSeatId = null
                                    } else {
                                        val studentId = seatingPlanState.assignments[seatId]
                                        if (studentId != null && studentId.isNotBlank()) {
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
                    config = seatingPlanState.config,
                    isDarkTheme = isDarkTheme
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 3.3. Yerleşmemiş Öğrenciler Tepsisi (Varsa)
                if (unassignedStudents.isNotEmpty()) {
                    UnassignedStudentsSection(
                        unassignedStudents = unassignedStudents,
                        isDarkTheme = isDarkTheme,
                        onStudentClick = { student ->
                            // İlk boş koltuğu bul ve oturt
                            val allSeatIds = SeatingPlanHelper.generateAllSeatIdsFrontToBack(seatingPlanState.config)
                            val firstEmptySeatId = allSeatIds.firstOrNull { seatId ->
                                seatingPlanState.assignments[seatId].isNullOrBlank()
                            }
                            if (firstEmptySeatId != null) {
                                onAssignStudentToSeat(firstEmptySeatId, student.id)
                                Toast.makeText(context, "${student.fullName} boş koltuğa yerleştirildi.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Sınıfta boş koltuk kalmadı. Lütfen sıra düzenini genişletin.", Toast.LENGTH_SHORT).show()
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
        val studentId = seatingPlanState.assignments[seatId]
        val student = students.firstOrNull { it.id == studentId }

        if (student != null) {
            val isPinned = seatingPlanState.pinnedStudents.contains(student.id)
            val isPriority = seatingPlanState.priorityStudents.contains(student.id)

            SeatDetailDialog(
                seatId = seatId,
                student = student,
                isPinned = isPinned,
                isPriority = isPriority,
                isDarkTheme = isDarkTheme,
                onDismiss = { selectedSeatForDetail = null },
                onStartSwap = {
                    swapSourceSeatId = seatId
                    selectedSeatForDetail = null
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
        }
    }

    // 2. Boş Koltuğa Öğrenci Seçme Diyaloğu
    selectedSeatForAssignment?.let { seatId ->
        AssignStudentDialog(
            seatId = seatId,
            unassignedStudents = unassignedStudents,
            allStudents = students,
            seatingPlanState = seatingPlanState,
            isDarkTheme = isDarkTheme,
            onDismiss = { selectedSeatForAssignment = null },
            onSelectStudent = { studentId ->
                onAssignStudentToSeat(seatId, studentId)
                selectedSeatForAssignment = null
            }
        )
    }

    // 3. Sınıf Mimarisi & Ayarlar Diyaloğu
    if (isConfigDialogOpen) {
        SeatingConfigDialog(
            currentConfig = seatingPlanState.config,
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
            currentState = seatingPlanState,
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
}

/**
 * Sınıf Ön Cephesi: Ortalanmış Akıllı Tahta, Öğretmen Masası, Pencere ve Kapı Konumu
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
        shape = RoundedCornerShape(14.dp),
        color = if (isDarkTheme) Color(0x600E1A38) else Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Sınıf Oturma Planı",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 1. SATIR: ÖN DUVAR MİMARİSİ (Sol Kapı/Pencere, Ortalanmış Akıllı Tahta, Sağ Kapı/Pencere)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Sol Duvar Elemanları (Pencere ve/veya Ön Sol Kapı)
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (config.doorPosition == DoorPosition.ON_SOL || config.doorPosition == DoorPosition.SOL_ON) {
                            ArchitecturalBadge(label = "🚪 Kapı (${config.doorPosition.positionOnFacade})", isDarkTheme = isDarkTheme, isDoor = true)
                        }
                        if (config.windowPosition == WindowPosition.LEFT) {
                            ArchitecturalBadge(label = "🪟 Pencere", isDarkTheme = isDarkTheme)
                        }
                    }
                }

                // Ortalanmış Akıllı Tahta
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFF334155),
                    border = BorderStroke(1.dp, Color(0xFF475569))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "📋 AKILLI TAHTA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }

                // Sağ Duvar Elemanları (Pencere ve/veya Ön Sağ Kapı)
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (config.windowPosition == WindowPosition.RIGHT) {
                            ArchitecturalBadge(label = "🪟 Pencere", isDarkTheme = isDarkTheme)
                        }
                        if (config.doorPosition == DoorPosition.ON_SAG || config.doorPosition == DoorPosition.SAG_ON) {
                            ArchitecturalBadge(label = "🚪 Kapı (${config.doorPosition.positionOnFacade})", isDarkTheme = isDarkTheme, isDoor = true)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. SATIR: ÖĞRETMEN MASASI (Sol Ön, Orta Ön veya Sağ Ön Konumu - Asla Tahta Altında Kalmaz)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = when (config.deskPosition) {
                    DeskPosition.LEFT -> Arrangement.Start
                    DeskPosition.CENTER -> Arrangement.Center
                    DeskPosition.RIGHT -> Arrangement.End
                }
            ) {
                TeacherDeskBadge(isDarkTheme = isDarkTheme)
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
        shape = RoundedCornerShape(8.dp),
        color = if (isDoor) {
            if (isDarkTheme) Color(0x40D97706) else Color(0xFFFEF3C7)
        } else {
            if (isDarkTheme) Color(0x400284C7) else Color(0xFFE0F2FE)
        },
        border = BorderStroke(1.dp, if (isDoor) Color(0xFFD97706) else Color(0xFF0284C7))
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDoor) Color(0xFFB45309) else Color(0xFF0284C7),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun TeacherDeskBadge(isDarkTheme: Boolean) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isDarkTheme) Color(0x40F59E0B) else Color(0xFFFFFBEB),
        border = BorderStroke(1.dp, Color(0xFFF59E0B))
    ) {
        Text(
            text = "👩‍🏫 Öğretmen Masası",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFD97706),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
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
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isGirl = student?.gender.equals("Kız", ignoreCase = true)

    val backgroundColor = when {
        isSelectedForSwap -> if (isDarkTheme) Color(0xFF78350F) else Color(0xFFFEF3C7)
        student == null -> if (isDarkTheme) Color(0x20334E68) else Color.White
        isGirl -> if (isDarkTheme) Color(0x35BE185D) else Color(0xFFFDF2F8)
        else -> if (isDarkTheme) Color(0x350284C7) else Color(0xFFEFF6FF)
    }

    val borderColor = when {
        isSelectedForSwap -> BirizAmber
        student == null -> if (isDarkTheme) Color(0x4064748B) else Color(0xFFE2E8F0)
        isGirl -> if (isDarkTheme) Color(0xFFF472B6) else Color(0xFFF472B6)
        else -> if (isDarkTheme) Color(0xFF60A5FA) else Color(0xFF93C5FD)
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = backgroundColor,
        border = BorderStroke(if (isSelectedForSwap) 2.dp else 1.dp, borderColor),
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
                        text = "#${student.studentNumber}",
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
                        tint = if (isDarkTheme) Color(0xFF64748B) else Color(0xFF94A3B8),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "Boş",
                        fontSize = 11.sp,
                        color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Yerleşmemiş Öğrenciler Yatay Tepsisi
 */
@Composable
private fun UnassignedStudentsSection(
    unassignedStudents: List<Student>,
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
                    text = "Yerleşmemiş Öğrenciler (${unassignedStudents.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                )
                Text(
                    text = "Dokunarak boş koltuğa ata",
                    fontSize = 11.sp,
                    color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(unassignedStudents, key = { it.id }) { student ->
                    val isGirl = student.gender.equals("Kız", ignoreCase = true)
                    Surface(
                        onClick = { onStudentClick(student) },
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
                                    text = "#${student.studentNumber}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isGirl) Color(0xFFDB2777) else Color(0xFF0284C7),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = TurkishTextHelper.toTurkishUpperCase(student.fullName),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = if (isDarkTheme) Color.White else Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
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
    }
}

/**
 * Koltuk Tıklandığında Açılan Detay ve İşlem Menüsü
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
    onTogglePin: () -> Unit,
    onTogglePriority: () -> Unit,
    onEmptySeat: () -> Unit
) {
    val isGirl = student.gender.equals("Kız", ignoreCase = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isGirl) Color(0xFFFCE7F3) else Color(0xFFE0F2FE)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isGirl) "👧" else "👦",
                        fontSize = 18.sp
                    )
                }
                Column {
                    Text(
                        text = TurkishTextHelper.toTurkishUpperCase(student.fullName),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Okul No: ${student.studentNumber} • Cinsiyet: ${student.gender}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Koltuk Konumu
                val seatInfo = SeatingPlanHelper.parseSeatId(seatId)
                if (seatInfo != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDarkTheme) Color(0x30334E68) else Color(0xFFF1F5F9),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "📍 Konum: ${seatInfo.groupIndex + 1}. Grup, ${seatInfo.rowIndex + 1}. Sıra, ${seatInfo.seatIndex + 1}. Koltuk",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                HorizontalDivider()

                // 1. Yer Değiştirme (Swap) Aksiyonu
                OutlinedButton(
                    onClick = onStartSwap,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_swap_seat_button")
                ) {
                    Icon(imageVector = Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Başka Koltukla Yer Değiştir (Takas)")
                }

                // 2. Sıraya Sabitle / Kilitle (Pin)
                OutlinedButton(
                    onClick = onTogglePin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_toggle_pin_button")
                ) {
                    Icon(
                        imageVector = if (isPinned) Icons.Default.LockOpen else Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isPinned) "Sabitlemeyi Kaldır (Serbest Bırak)" else "Bu Koltuğa Sabitle (🔒 Kilit)")
                }

                // 3. Ön Sıra Önceliği (Gözlük/Boy/İşitme)
                OutlinedButton(
                    onClick = onTogglePriority,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_toggle_priority_button")
                ) {
                    Text(
                        text = if (isPriority) "👓 Ön Sıra Önceliğini Kaldır" else "👓 Ön Sıra Önceliği Ver (Gözlük/Boy)",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // 4. Koltuğu Boşalt
                OutlinedButton(
                    onClick = onEmptySeat,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.Red
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_empty_seat_button")
                ) {
                    Icon(imageVector = Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bu Koltuğu Boşalt")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat")
            }
        }
    )
}

/**
 * Boş Koltuğa Öğrenci Seçme Diyaloğu
 */
@Composable
private fun AssignStudentDialog(
    seatId: String,
    unassignedStudents: List<Student>,
    allStudents: List<Student>,
    seatingPlanState: SeatingPlanState,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onSelectStudent: (String) -> Unit
) {
    val seatInfo = SeatingPlanHelper.parseSeatId(seatId)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Koltuğa Öğrenci Yerleştir",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(modifier = Modifier.height(380.dp)) {
                if (seatInfo != null) {
                    Text(
                        text = "Hedef: ${seatInfo.groupIndex + 1}. Grup, ${seatInfo.rowIndex + 1}. Sıra, ${seatInfo.seatIndex + 1}. Koltuk",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Text(
                    text = if (unassignedStudents.isNotEmpty()) "Yerleşmemiş Öğrenciler:" else "Tüm Sınıf Öğrencileri:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = BirizTurquoise,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                val displayList = if (unassignedStudents.isNotEmpty()) unassignedStudents else allStudents

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(displayList, key = { it.id }) { student ->
                        val isGirl = student.gender.equals("Kız", ignoreCase = true)
                        val isAssigned = seatingPlanState.assignments.values.contains(student.id)

                        Surface(
                            onClick = { onSelectStudent(student.id) },
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
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
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
                                        text = TurkishTextHelper.toTurkishUpperCase(student.fullName),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = if (isDarkTheme) Color.White else Color(0xFF1E293B),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isAssigned) {
                                        Text(
                                            text = "Mevcut Koltuktan Taşı",
                                            fontSize = 10.sp,
                                            color = BirizAmber,
                                            fontWeight = FontWeight.Bold
                                        )
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
    var groupCount by remember { mutableIntStateOf(currentConfig.groupCount) }
    var peoplePerRow by remember { mutableIntStateOf(currentConfig.peoplePerRow) }
    var rowsPerGroup by remember {
        mutableStateOf(currentConfig.rowsPerGroup.toMutableList().also { list ->
            while (list.size < 6) list.add(5)
        })
    }
    var windowPosition by remember { mutableStateOf(currentConfig.windowPosition) }
    var doorPosition by remember { mutableStateOf(currentConfig.doorPosition) }
    var selectedDoorFacade by remember { mutableStateOf(currentConfig.doorPosition.facade) }
    var deskPosition by remember { mutableStateOf(currentConfig.deskPosition) }

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
