package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClassRoom
import com.example.data.model.Student
import com.example.ui.theme.BirizAmber
import com.example.ui.theme.BirizBorder
import com.example.ui.theme.BirizDarkCardBorder
import com.example.ui.theme.BirizDarkSurfaceGlass
import com.example.ui.theme.BirizDarkTextPrimary
import com.example.ui.theme.BirizDarkTextSecondary
import com.example.ui.theme.BirizNavyDark
import com.example.ui.theme.BirizNavyPrimary
import com.example.ui.theme.BirizTextPrimary
import com.example.ui.theme.BirizTextSecondary
import com.example.ui.theme.BirizTurquoise
import com.example.ui.viewmodel.DashboardSubPage
import com.example.util.DilekceState
import com.example.util.DilekceType
import com.example.util.ExcelPdfHelper
import com.example.util.OkulGeziState
import com.example.util.SosyalKulupState
import com.example.util.TeacherDocumentHelper
import com.example.util.VeliToplantiState
import com.example.util.ZumreToplantiState

@Composable
fun TeacherRoomSubScreen(
    classRoom: ClassRoom,
    students: List<Student>,
    currentSubPage: DashboardSubPage,
    isDarkTheme: Boolean,
    onSelectSubPage: (DashboardSubPage) -> Unit,
    onBackToClassList: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    BackHandler {
        onBackToClassList()
    }

    // Modül State'leri
    var zumreState by remember {
        mutableStateOf(
            ZumreToplantiState(
                chairman = classRoom.teacherName,
                subjectName = "${classRoom.gradeLevel}. Sınıf Öğretmenleri Zümresi"
            )
        )
    }

    var veliToplantiState by remember {
        mutableStateOf(
            VeliToplantiState(
                classroomName = "${classRoom.gradeLevel}/${classRoom.section} Dersliği"
            )
        )
    }

    var sosyalKulupState by remember {
        mutableStateOf(
            SosyalKulupState(
                advisorTeacher = classRoom.teacherName,
                presidentStudentName = students.firstOrNull()?.fullName ?: "Kulüp Başkanı",
                vicePresidentStudentName = students.getOrNull(1)?.fullName ?: "Başkan Yrd.",
                secretaryStudentName = students.getOrNull(2)?.fullName ?: "Yazman"
            )
        )
    }

    var okulGeziState by remember {
        mutableStateOf(
            OkulGeziState(
                leaderTeacher = classRoom.teacherName,
                participatingStudentIds = students.map { it.id }.toSet()
            )
        )
    }

    var dilekceState by remember {
        mutableStateOf(
            DilekceState()
        )
    }

    var isShareMenuOpen by remember { mutableStateOf(false) }

    val copyToClipboard: (String, String) -> Unit = { title, text ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(title, text))
        Toast.makeText(context, "$title panoya kopyalandı! 📋", Toast.LENGTH_SHORT).show()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 88.dp) // Alt dock için boşluk
                .verticalScroll(scrollState)
        ) {
            // --- 1. ÜST HERO BANNER ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isDarkTheme) Color(0x900E1A38) else Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Coffee,
                                    contentDescription = "Öğretmen Odası",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Öğretmen Odası",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                                )
                                Text(
                                    text = "Resmî MEB Evrak & Tutanak Merkezi",
                                    fontSize = 12.sp,
                                    color = BirizTurquoise,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Paylaş Butonu
                        Box {
                            Button(
                                onClick = { isShareMenuOpen = true },
                                colors = ButtonDefaults.buttonColors(containerColor = BirizNavyPrimary),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("teacher_room_share_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Paylaş",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Paylaş", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            DropdownMenu(
                                expanded = isShareMenuOpen,
                                onDismissRequest = { isShareMenuOpen = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("📋 WhatsApp / SMS Metni Kopyala", fontSize = 12.sp) },
                                    onClick = {
                                        isShareMenuOpen = false
                                        when (currentSubPage) {
                                            DashboardSubPage.OGRETMEN_ODASI_ZUMRE -> {
                                                val text = TeacherDocumentHelper.generateZumreShareText(classRoom, zumreState)
                                                copyToClipboard("Zümre Toplantı Tutanağı", text)
                                            }
                                            DashboardSubPage.OGRETMEN_ODASI_VELI -> {
                                                val text = TeacherDocumentHelper.generateVeliToplantiShareText(classRoom, veliToplantiState, students)
                                                copyToClipboard("Veli Toplantı Tutanağı", text)
                                            }
                                            DashboardSubPage.OGRETMEN_ODASI_KULUP -> {
                                                val text = TeacherDocumentHelper.generateSosyalKulupShareText(classRoom, sosyalKulupState)
                                                copyToClipboard("Sosyal Kulüp Planı", text)
                                            }
                                            DashboardSubPage.OGRETMEN_ODASI_GEZI -> {
                                                val text = TeacherDocumentHelper.generateOkulGeziShareText(classRoom, okulGeziState, okulGeziState.participatingStudentIds.size)
                                                copyToClipboard("Okul Gezi Evrakları", text)
                                            }
                                            DashboardSubPage.OGRETMEN_ODASI_DILEKCE -> {
                                                val text = TeacherDocumentHelper.generateDilekceShareText(classRoom, dilekceState)
                                                copyToClipboard("Resmi Dilekçe Metni", text)
                                            }
                                            else -> {}
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("📊 Excel (.xlsx) İndir / Paylaş", fontSize = 12.sp) },
                                    onClick = {
                                        isShareMenuOpen = false
                                        try {
                                            val uri = when (currentSubPage) {
                                                DashboardSubPage.OGRETMEN_ODASI_ZUMRE ->
                                                    TeacherDocumentHelper.exportZumreExcel(context, classRoom, zumreState)
                                                DashboardSubPage.OGRETMEN_ODASI_VELI ->
                                                    TeacherDocumentHelper.exportVeliToplantiExcel(context, classRoom, veliToplantiState, students)
                                                DashboardSubPage.OGRETMEN_ODASI_KULUP ->
                                                    TeacherDocumentHelper.exportSosyalKulupExcel(context, classRoom, sosyalKulupState)
                                                DashboardSubPage.OGRETMEN_ODASI_GEZI -> {
                                                    val partStudents = students.filter { okulGeziState.participatingStudentIds.contains(it.id) }
                                                    TeacherDocumentHelper.exportOkulGeziExcel(context, classRoom, okulGeziState, partStudents)
                                                }
                                                else ->
                                                    TeacherDocumentHelper.exportZumreExcel(context, classRoom, zumreState)
                                            }
                                            ExcelPdfHelper.shareFile(
                                                context = context,
                                                uri = uri,
                                                mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                                chooserTitle = "Excel Evrakını Paylaş"
                                            )
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("📄 PDF İndir / Yazdır (Resmî MEB A4)", fontSize = 12.sp) },
                                    onClick = {
                                        isShareMenuOpen = false
                                        try {
                                            val uri = when (currentSubPage) {
                                                DashboardSubPage.OGRETMEN_ODASI_ZUMRE -> {
                                                    TeacherDocumentHelper.exportOfficialDocumentPdf(
                                                        context = context,
                                                        classRoom = classRoom,
                                                        documentTitle = "${zumreState.academicYear} ${zumreState.semester} ZÜMRE TUTANAĞI",
                                                        subTitle = zumreState.subjectName,
                                                        infoRows = listOf(
                                                            "Tarih & Saat" to "${zumreState.meetingDate} - ${zumreState.meetingTime}",
                                                            "Toplantı Yeri" to zumreState.meetingLocation,
                                                            "Zümre Başkanı" to zumreState.chairman.ifBlank { classRoom.teacherName },
                                                            "Katılımcılar" to zumreState.participants.joinToString(", ")
                                                        ),
                                                        numberedSections = listOf(
                                                            "Gündem Maddeleri" to zumreState.agendaItems,
                                                            "Alınan Kararlar" to zumreState.decisions
                                                        ),
                                                        signers = listOf(
                                                            zumreState.chairman.ifBlank { classRoom.teacherName } + " (Zümre Bşk.)",
                                                            "Katılımcı Öğretmen",
                                                            "Okul Müdürü (Onay)"
                                                        )
                                                    )
                                                }
                                                DashboardSubPage.OGRETMEN_ODASI_VELI -> {
                                                    TeacherDocumentHelper.exportOfficialDocumentPdf(
                                                        context = context,
                                                        classRoom = classRoom,
                                                        documentTitle = veliToplantiState.termName,
                                                        subTitle = "${classRoom.gradeLevel}/${classRoom.section} Sınıfı Veli Toplantı Tutanağı",
                                                        infoRows = listOf(
                                                            "Tarih & Saat" to "${veliToplantiState.meetingDate} - ${veliToplantiState.meetingTime}",
                                                            "Derslik" to veliToplantiState.classroomName,
                                                            "Sınıf Öğretmeni" to classRoom.teacherName,
                                                            "Kayıtlı Öğrenci" to "${students.size} Öğrenci"
                                                        ),
                                                        numberedSections = listOf(
                                                            "Toplantı Gündemi" to veliToplantiState.agendaItems,
                                                            "Alınan Kararlar ve Tavsiyeler" to veliToplantiState.decisions
                                                        ),
                                                        signers = listOf(
                                                            classRoom.teacherName + " (Sınıf Öğretmeni)",
                                                            "Veli Temsilcisi",
                                                            "Okul Müdürü"
                                                        )
                                                    )
                                                }
                                                DashboardSubPage.OGRETMEN_ODASI_KULUP -> {
                                                    TeacherDocumentHelper.exportOfficialDocumentPdf(
                                                        context = context,
                                                        classRoom = classRoom,
                                                        documentTitle = "${sosyalKulupState.clubName} Yıllık Planı",
                                                        subTitle = "Sosyal Etkinlikler ve Toplum Hizmeti Faaliyet Tutanağı",
                                                        infoRows = listOf(
                                                            "Danışman Öğretmen" to sosyalKulupState.advisorTeacher.ifBlank { classRoom.teacherName },
                                                            "Kulüp Başkanı" to sosyalKulupState.presidentStudentName,
                                                            "Başkan Yrd." to sosyalKulupState.vicePresidentStudentName,
                                                            "Toplum Hizmeti" to sosyalKulupState.communityServiceTopic
                                                        ),
                                                        numberedSections = listOf(
                                                            "Aylık Faaliyet Dağılımı" to sosyalKulupState.monthlyPlan.map { "${it.first}: ${it.second}" }
                                                        ),
                                                        signers = listOf(
                                                            sosyalKulupState.advisorTeacher.ifBlank { classRoom.teacherName } + " (Danışman)",
                                                            sosyalKulupState.presidentStudentName + " (Kulüp Bşk.)",
                                                            "Okul Müdürü"
                                                        )
                                                    )
                                                }
                                                DashboardSubPage.OGRETMEN_ODASI_GEZI -> {
                                                    TeacherDocumentHelper.exportOfficialDocumentPdf(
                                                        context = context,
                                                        classRoom = classRoom,
                                                        documentTitle = "MEB OKUL GEZİSİ BİLGİLENDİRME VE ONAY FORMU",
                                                        subTitle = "${okulGeziState.destination} Gezi Dosyası",
                                                        infoRows = listOf(
                                                            "Gezi Tarihi" to okulGeziState.tripDate,
                                                            "Hareket - Dönüş" to "${okulGeziState.departureTime} - ${okulGeziState.returnTime}",
                                                            "Kafile Başkanı" to okulGeziState.leaderTeacher.ifBlank { classRoom.teacherName },
                                                            "Ulaşım & Sürücü" to "${okulGeziState.transportType} (${okulGeziState.busPlateAndDriver})"
                                                        ),
                                                        numberedSections = listOf(
                                                            "Gezinin Amacı ve Kazanımları" to listOf(okulGeziState.purpose),
                                                            "Alınan Güvenlik ve Emniyet Tedbirleri" to listOf(
                                                                "Araç belgeleri, kasko ve D2 yetki belgesi kontrol edilmiştir.",
                                                                "Tüm öğrencilerin velilerinden yazılı izin belgesi (muvafakat) alınmıştır.",
                                                                "Öğrencilere ilk yardım çantası ve acil durum irtibat listesi temin edilmiştir."
                                                            )
                                                        ),
                                                        signers = listOf(
                                                            okulGeziState.leaderTeacher.ifBlank { classRoom.teacherName } + " (Kafile Bşk.)",
                                                            "Müdür Yardımcısı",
                                                            "Okul Müdürü (Uygundur)"
                                                        )
                                                    )
                                                }
                                                DashboardSubPage.OGRETMEN_ODASI_DILEKCE -> {
                                                    TeacherDocumentHelper.exportOfficialDocumentPdf(
                                                        context = context,
                                                        classRoom = classRoom,
                                                        documentTitle = "RESMÎ DİLEKÇE VE TALEP FORMU",
                                                        subTitle = dilekceState.type.title,
                                                        infoRows = listOf(
                                                            "Makam" to "${classRoom.schoolName} Müdürlüğüne",
                                                            "Talep Eden" to classRoom.teacherName,
                                                            "Görevi" to "${classRoom.gradeLevel}/${classRoom.section} Sınıf Öğretmeni",
                                                            "Tarih" to dilekceState.dateSpan
                                                        ),
                                                        numberedSections = listOf(
                                                            "Dilekçe Metni" to listOf(
                                                                "Konu: ${dilekceState.subject}",
                                                                dilekceState.detailText,
                                                                "Gereğini saygılarımla arz ederim."
                                                            )
                                                        ),
                                                        signers = listOf(
                                                            classRoom.teacherName + " (Öğretmen)",
                                                            "Okul Müdürü"
                                                        )
                                                    )
                                                }
                                                else -> {
                                                    TeacherDocumentHelper.exportOfficialDocumentPdf(
                                                        context = context,
                                                        classRoom = classRoom,
                                                        documentTitle = "RESMÎ EVRAK",
                                                        subTitle = "BİRİZ Öğretmen Odası",
                                                        infoRows = emptyList(),
                                                        numberedSections = emptyList(),
                                                        signers = listOf(classRoom.teacherName)
                                                    )
                                                }
                                            }
                                            ExcelPdfHelper.shareFile(
                                                context = context,
                                                uri = uri,
                                                mimeType = "application/pdf",
                                                chooserTitle = "PDF Belgesini Paylaş veya Yazdır"
                                            )
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Hızlı Araç Çubuğu (Zümre, Veli, Kulüp, Gezi, Dilekçe)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val tabs = listOf(
                            DashboardSubPage.OGRETMEN_ODASI_ZUMRE to "☕ Zümre Toplantısı",
                            DashboardSubPage.OGRETMEN_ODASI_VELI to "👨‍👩‍👧‍👦 Veli Toplantısı",
                            DashboardSubPage.OGRETMEN_ODASI_KULUP to "🤝 Sosyal Kulüp",
                            DashboardSubPage.OGRETMEN_ODASI_GEZI to "🚌 Okul Gezisi",
                            DashboardSubPage.OGRETMEN_ODASI_DILEKCE to "📝 Dilekçe & Formlar"
                        )

                        tabs.forEach { (subPage, label) ->
                            val isSelected = currentSubPage == subPage
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectSubPage(subPage) },
                                label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                                    selectedLabelColor = if (isDarkTheme) BirizNavyDark else Color.White
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- 2. AKTİF MODÜLE AİT FORM & BELGE SİHİRBAZI ---
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                when (currentSubPage) {
                    DashboardSubPage.OGRETMEN_ODASI_ZUMRE -> {
                        ZumreToplantiSection(
                            classRoom = classRoom,
                            state = zumreState,
                            isDark = isDarkTheme,
                            onUpdateState = { zumreState = it }
                        )
                    }

                    DashboardSubPage.OGRETMEN_ODASI_VELI -> {
                        VeliToplantiSection(
                            classRoom = classRoom,
                            students = students,
                            state = veliToplantiState,
                            isDark = isDarkTheme,
                            onUpdateState = { veliToplantiState = it }
                        )
                    }

                    DashboardSubPage.OGRETMEN_ODASI_KULUP -> {
                        SosyalKulupSection(
                            classRoom = classRoom,
                            students = students,
                            state = sosyalKulupState,
                            isDark = isDarkTheme,
                            onUpdateState = { sosyalKulupState = it }
                        )
                    }

                    DashboardSubPage.OGRETMEN_ODASI_GEZI -> {
                        OkulGeziSection(
                            classRoom = classRoom,
                            students = students,
                            state = okulGeziState,
                            isDark = isDarkTheme,
                            onUpdateState = { okulGeziState = it }
                        )
                    }

                    DashboardSubPage.OGRETMEN_ODASI_DILEKCE -> {
                        DilekceSection(
                            classRoom = classRoom,
                            state = dilekceState,
                            isDark = isDarkTheme,
                            onUpdateState = { dilekceState = it }
                        )
                    }

                    else -> {
                        ZumreToplantiSection(
                            classRoom = classRoom,
                            state = zumreState,
                            isDark = isDarkTheme,
                            onUpdateState = { zumreState = it }
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 1. ZÜMRE TOPLANTISI BÖLÜMÜ
// -------------------------------------------------------------
@Composable
private fun ZumreToplantiSection(
    classRoom: ClassRoom,
    state: ZumreToplantiState,
    isDark: Boolean,
    onUpdateState: (ZumreToplantiState) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) BirizDarkSurfaceGlass else Color.White),
        border = BorderStroke(1.dp, if (isDark) BirizDarkCardBorder else BirizBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Groups, contentDescription = null, tint = BirizTurquoise)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Zümre Öğretmenler Kurulu Tutanak Sihirbazı",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isDark) BirizDarkTextPrimary else BirizNavyPrimary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Form Alanları
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.academicYear,
                    onValueChange = { onUpdateState(state.copy(academicYear = it)) },
                    label = { Text("Eğitim Yılı", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.semester,
                    onValueChange = { onUpdateState(state.copy(semester = it)) },
                    label = { Text("Dönem", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = state.subjectName,
                onValueChange = { onUpdateState(state.copy(subjectName = it)) },
                label = { Text("Zümre Adı / Alanı", fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.meetingDate,
                    onValueChange = { onUpdateState(state.copy(meetingDate = it)) },
                    label = { Text("Tarih", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.meetingTime,
                    onValueChange = { onUpdateState(state.copy(meetingTime = it)) },
                    label = { Text("Saat", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Gündem Maddeleri (${state.agendaItems.size}):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))
            state.agendaItems.forEachIndexed { idx, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${idx + 1}.", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BirizTurquoise)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(item, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = {
                            val newItems = state.agendaItems.toMutableList().apply { removeAt(idx) }
                            onUpdateState(state.copy(agendaItems = newItems))
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Sil", modifier = Modifier.size(14.dp), tint = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("Alınan Kararlar (${state.decisions.size}):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))
            state.decisions.forEachIndexed { idx, dec ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("✓", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF10B981))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(dec, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = {
                            val newDecs = state.decisions.toMutableList().apply { removeAt(idx) }
                            onUpdateState(state.copy(decisions = newDecs))
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Sil", modifier = Modifier.size(14.dp), tint = Color.Gray)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 2. VELİ TOPLANTISI BÖLÜMÜ
// -------------------------------------------------------------
@Composable
private fun VeliToplantiSection(
    classRoom: ClassRoom,
    students: List<Student>,
    state: VeliToplantiState,
    isDark: Boolean,
    onUpdateState: (VeliToplantiState) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) BirizDarkSurfaceGlass else Color.White),
        border = BorderStroke(1.dp, if (isDark) BirizDarkCardBorder else BirizBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.People, contentDescription = null, tint = BirizTurquoise)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Veli Toplantı Tutanağı & İmza Sirküsü",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isDark) BirizDarkTextPrimary else BirizNavyPrimary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.termName,
                onValueChange = { onUpdateState(state.copy(termName = it)) },
                label = { Text("Toplantı Tanımı", fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.meetingDate,
                    onValueChange = { onUpdateState(state.copy(meetingDate = it)) },
                    label = { Text("Tarih", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.meetingTime,
                    onValueChange = { onUpdateState(state.copy(meetingTime = it)) },
                    label = { Text("Saat", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = state.classroomName,
                onValueChange = { onUpdateState(state.copy(classroomName = it)) },
                label = { Text("Toplantı Dersliği", fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Katılımcı Veli İmza Sirküsü (${students.size} Öğrenci):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(
                "Sınıf listeniz otomatik olarak A4 çıktı ve Excel sirküsüne eklenir.",
                fontSize = 11.sp,
                color = BirizTurquoise
            )
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                shape = RoundedCornerShape(8.dp),
                color = if (isDark) Color(0x301E293B) else Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, if (isDark) BirizDarkCardBorder else BirizBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (students.isEmpty()) {
                        Text(
                            "Henüz sınıfa öğrenci eklenmedi. Yine de boş şablon olarak yazdırabilirsiniz.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(12.dp)
                        )
                    } else {
                        students.sortedBy { it.studentNumber.toIntOrNull() ?: 9999 }.forEach { s ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${s.studentNumber} • ${s.fullName}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text("[ İmza Alanı ]", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 3. SOSYAL KULÜP BÖLÜMÜ
// -------------------------------------------------------------
@Composable
private fun SosyalKulupSection(
    classRoom: ClassRoom,
    students: List<Student>,
    state: SosyalKulupState,
    isDark: Boolean,
    onUpdateState: (SosyalKulupState) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) BirizDarkSurfaceGlass else Color.White),
        border = BorderStroke(1.dp, if (isDark) BirizDarkCardBorder else BirizBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Assignment, contentDescription = null, tint = BirizTurquoise)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Sosyal Kulüp & Toplum Hizmeti Planı",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isDark) BirizDarkTextPrimary else BirizNavyPrimary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            Text("Kulüp Seçimi:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))

            // Yatay Kulüp Seçim Çipleri
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TeacherDocumentHelper.AVAILABLE_CLUBS.forEach { club ->
                    val isSelected = state.clubName == club
                    FilterChip(
                        selected = isSelected,
                        onClick = { onUpdateState(state.copy(clubName = club)) },
                        label = { Text(club, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.advisorTeacher.ifBlank { classRoom.teacherName },
                onValueChange = { onUpdateState(state.copy(advisorTeacher = it)) },
                label = { Text("Danışman Öğretmen", fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.presidentStudentName,
                    onValueChange = { onUpdateState(state.copy(presidentStudentName = it)) },
                    label = { Text("Kulüp Başkanı", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = state.vicePresidentStudentName,
                    onValueChange = { onUpdateState(state.copy(vicePresidentStudentName = it)) },
                    label = { Text("Başkan Yrd.", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = state.communityServiceTopic,
                onValueChange = { onUpdateState(state.copy(communityServiceTopic = it)) },
                label = { Text("Toplum Hizmeti Çalışma Konusu", fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))
            Text("Yıllık Çalışma Çizelgesi (9 Ay MEB Takvimi):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(6.dp))
            state.monthlyPlan.take(4).forEach { (m, act) ->
                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                    Text("$m: ", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BirizTurquoise)
                    Text(act, fontSize = 12.sp)
                }
            }
            Text("...ve diğer tüm aylar resmi PDF ve Excel çıktısında yer alır.", fontSize = 11.sp, color = Color.Gray)
        }
    }
}

// -------------------------------------------------------------
// 4. OKUL GEZİSİ BÖLÜMÜ
// -------------------------------------------------------------
@Composable
private fun OkulGeziSection(
    classRoom: ClassRoom,
    students: List<Student>,
    state: OkulGeziState,
    isDark: Boolean,
    onUpdateState: (OkulGeziState) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) BirizDarkSurfaceGlass else Color.White),
        border = BorderStroke(1.dp, if (isDark) BirizDarkCardBorder else BirizBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DirectionsBus, contentDescription = null, tint = BirizTurquoise)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "MEB Okul Gezisi Evrakları & Veli İzin Belgesi",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isDark) BirizDarkTextPrimary else BirizNavyPrimary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.destination,
                onValueChange = { onUpdateState(state.copy(destination = it)) },
                label = { Text("Gezi Yeri / Hedefi", fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.tripDate,
                    onValueChange = { onUpdateState(state.copy(tripDate = it)) },
                    label = { Text("Tarih", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = "${state.departureTime} - ${state.returnTime}",
                    onValueChange = { },
                    label = { Text("Saat (Gidiş-Dönüş)", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = state.purpose,
                onValueChange = { onUpdateState(state.copy(purpose = it)) },
                label = { Text("Gezinin Eğitsel Amacı", fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = state.busPlateAndDriver,
                onValueChange = { onUpdateState(state.copy(busPlateAndDriver = it)) },
                label = { Text("Araç Plakası & Sürücü Bilgisi", fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Geziye Katılacak Öğrenciler (${state.participatingStudentIds.size}/${students.size}):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = if (state.participatingStudentIds.size == students.size) "Tümünü Kaldır" else "Tümünü Seç",
                    fontSize = 12.sp,
                    color = BirizTurquoise,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        if (state.participatingStudentIds.size == students.size) {
                            onUpdateState(state.copy(participatingStudentIds = emptySet()))
                        } else {
                            onUpdateState(state.copy(participatingStudentIds = students.map { it.id }.toSet()))
                        }
                    }
                )
            }
        }
    }
}

// -------------------------------------------------------------
// 5. RESMİ DİLEKÇE VE FORMLAR BÖLÜMÜ
// -------------------------------------------------------------
@Composable
private fun DilekceSection(
    classRoom: ClassRoom,
    state: DilekceState,
    isDark: Boolean,
    onUpdateState: (DilekceState) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) BirizDarkSurfaceGlass else Color.White),
        border = BorderStroke(1.dp, if (isDark) BirizDarkCardBorder else BirizBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Article, contentDescription = null, tint = BirizTurquoise)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Resmî Dilekçe & Form Şablonları",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (isDark) BirizDarkTextPrimary else BirizNavyPrimary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Dilekçe Türü Seçimi
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DilekceType.values().forEach { dType ->
                    val isSelected = state.type == dType
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            val newSubject = when (dType) {
                                DilekceType.MAZERET_IZNI -> "657 Sayılı Kanun Madde 104 Uyarınca Mazeret İzni Talebi"
                                DilekceType.SEVK_RAPOR -> "Hastalık Sevk / Sağlık Raporu Bildirimi"
                                DilekceType.SINAV_ANALIZ -> "Dönem Başı Sınav & Kazanım Değerlendirme Raporu"
                                DilekceType.REHBERLIK_VELI -> "Öğrenci Rehberlik ve Veli Görüşme Tutanağı"
                            }
                            onUpdateState(state.copy(type = dType, subject = newSubject))
                        },
                        label = { Text(dType.title, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = state.subject,
                onValueChange = { onUpdateState(state.copy(subject = it)) },
                label = { Text("Konu", fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = state.dateSpan,
                onValueChange = { onUpdateState(state.copy(dateSpan = it)) },
                label = { Text("Tarih", fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = state.detailText,
                onValueChange = { onUpdateState(state.copy(detailText = it)) },
                label = { Text("Dilekçe Açıklama & Talep Metni", fontSize = 11.sp) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Resmi Önizleme Kutusu
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = if (isDark) Color(0x301E293B) else Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, if (isDark) BirizDarkCardBorder else BirizBorder)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "${classRoom.schoolName.uppercase()} MÜDÜRLÜĞÜNE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (isDark) BirizDarkTextPrimary else BirizNavyPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(state.detailText, fontSize = 11.sp, color = if (isDark) BirizDarkTextSecondary else BirizTextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(classRoom.teacherName, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text("Sınıf Öğretmeni", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}
