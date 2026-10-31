package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ClassRoom
import com.example.data.model.Student
import com.example.ui.components.BentoCard
import com.example.ui.components.ParentDownloadQrDialog
import com.example.ui.theme.BirizAmber
import com.example.ui.theme.BirizBorder
import com.example.ui.theme.BirizDarkCardBorder
import com.example.ui.theme.BirizDarkSurfaceGlass
import com.example.ui.theme.BirizDarkTextMuted
import com.example.ui.theme.BirizDarkTextPrimary
import com.example.ui.theme.BirizDarkTextSecondary
import com.example.ui.theme.BirizError
import com.example.ui.theme.BirizNavyDark
import com.example.ui.theme.BirizNavyPrimary
import com.example.ui.theme.BirizTextMuted
import com.example.ui.theme.BirizTextPrimary
import com.example.ui.theme.BirizTextSecondary
import com.example.ui.theme.BirizTurquoise
import com.example.util.ExcelPdfHelper
import com.example.util.TurkishTextHelper

enum class ClassListTopTab {
    NONE,
    CLASS_SUMMARY,
    STUDENT_REGISTRATION
}

enum class StudentEntryMode {
    SINGLE_ENTRY,
    BATCH_EXCEL
}

enum class StudentSortFilter {
    BY_NUMBER,
    ALPHABETICAL,
    UNLINKED_ONLY
}

/**
 * Kompakt Tek Satır Giriş Kutucuğu (Yükseklik: 42dp)
 * - Gereksiz dikey boşlukları ve kaydırmayı önler.
 * - Odaklanıldığında Turkuaz çerçeve ile belirginleşir.
 */
@Composable
fun BirizCompactInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    testTag: String = ""
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isDarkTheme) Color(0x350A162B) else Color.White,
        border = BorderStroke(
            width = if (isFocused) 1.5.dp else 1.dp,
            color = if (isFocused) BirizTurquoise else if (isDarkTheme) BirizDarkCardBorder else BirizBorder
        ),
        modifier = modifier
            .height(42.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted,
                        maxLines = 1
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDarkTheme) Color.White else BirizTextPrimary
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(BirizTurquoise),
                    keyboardOptions = keyboardOptions,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused }
                )
            }
        }
    }
}

@Composable
fun ClassListSubScreen(
    classRoom: ClassRoom,
    students: List<Student>,
    isDarkTheme: Boolean,
    onAddStudent: (Int, String, String) -> Unit,
    onEditStudent: (String, Int, String, String) -> Unit,
    onDeleteStudent: (Student) -> Unit,
    onImportStudents: (List<Triple<Int, String, String>>) -> Unit,
    onCopyParentCode: (String) -> Unit,
    onUpdateParentInfo: ((studentId: String, name: String?, email: String?, linkedAt: String?, lastLoginAt: String?) -> Unit)? = null,
    onTogglePriority: ((studentId: String) -> Unit)? = null
) {
    val context = LocalContext.current

    // Giriş Modu: "Tek Öğrenci Ekle" veya "Toplu Yükleme (Excel)"
    var entryMode by remember { mutableStateOf(StudentEntryMode.SINGLE_ENTRY) }

    // Tek Öğrenci Giriş Formu State'leri
    var inputStudentNo by remember { mutableStateOf("") }
    var inputFirstName by remember { mutableStateOf("") }
    var inputLastName by remember { mutableStateOf("") }
    var inputGender by remember { mutableStateOf("Erkek") }
    var formError by remember { mutableStateOf<String?>(null) }

    // Öğrenci Düzenleme Diyaloğu State'i
    var editingStudent by remember { mutableStateOf<Student?>(null) }
    // Öğrenci Detay Penceresi State'i (Veri gizliliği & kazara dokunmayı önleme)
    var selectedStudentForDetail by remember { mutableStateOf<Student?>(null) }
    // Silme Onay Penceresi State'i
    var studentToDelete by remember { mutableStateOf<Student?>(null) }
    // Paylaşım Menüsü State'i
    var isShareMenuOpen by remember { mutableStateOf(false) }
    // Veli İndirme Karekodu Diyaloğu State'i
    var showParentQrDialog by remember { mutableStateOf(false) }

    // Üst Panel Sekme Tercihi: Tek satırda [4/D Sınıfı] ve [+ Öğrenci Ekle] sekmeleri
    var activeTopTab by remember {
        mutableStateOf(
            if (students.isEmpty()) ClassListTopTab.STUDENT_REGISTRATION else ClassListTopTab.NONE
        )
    }

    // Akıllı Sıralama & Filtreleme State'i
    var activeFilter by remember { mutableStateOf(StudentSortFilter.BY_NUMBER) }
    val turkishCollator = remember { java.text.Collator.getInstance(java.util.Locale("tr", "TR")) }
    val displayedStudents = remember(students, activeFilter) {
        when (activeFilter) {
            StudentSortFilter.BY_NUMBER -> students.sortedBy { it.studentNumber.toIntOrNull() ?: Int.MAX_VALUE }
            StudentSortFilter.ALPHABETICAL -> students.sortedWith(compareBy(turkishCollator) { it.fullName })
            StudentSortFilter.UNLINKED_ONLY -> students.filter { !it.isParentLinked }.sortedBy { it.studentNumber.toIntOrNull() ?: Int.MAX_VALUE }
        }
    }

    val totalStudents = students.size
    val femaleCount = students.count { it.gender.equals("Kız", ignoreCase = true) }
    val maleCount = students.count { it.gender.equals("Erkek", ignoreCase = true) }
    val parentLinkedCount = students.count { it.isParentLinked }
    val parentLinkedPercent = if (totalStudents > 0) (parentLinkedCount * 100) / totalStudents else 0
    val unlinkedCount = totalStudents - parentLinkedCount

    // Dosya Seçici Launcher (Excel / CSV yükleme)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val parseResult = ExcelPdfHelper.parseStudentsFile(context, uri)
            parseResult.onSuccess { parsedList ->
                onImportStudents(parsedList)
                Toast.makeText(context, "${parsedList.size} öğrenci dosyadan aktarılıyor...", Toast.LENGTH_SHORT).show()
            }.onFailure { err ->
                Toast.makeText(context, "Hata: ${err.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val isListComplete = students.isNotEmpty()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // --- 1. TEK SATIRDA YAN YANA İKİ SEKME: [ 4/D Sınıfı ] & [ + Öğrenci Ekle ] ---
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp)
            ) {
                // Tek Satır Yan Yana 2 Sekme Barı (Yükseklik: 44dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isClassActive = activeTopTab == ClassListTopTab.CLASS_SUMMARY
                    val isRegActive = activeTopTab == ClassListTopTab.STUDENT_REGISTRATION

                    // 1. SEKME: 4/D Sınıfı (Özet & İşlemler)
                    Surface(
                        onClick = {
                            activeTopTab = if (isClassActive) ClassListTopTab.NONE else ClassListTopTab.CLASS_SUMMARY
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = when {
                            isClassActive -> if (isDarkTheme) Color(0xFF13294B) else Color(0xFFE0F2FE)
                            else -> if (isDarkTheme) Color(0x350A162B) else Color.White
                        },
                        border = BorderStroke(
                            width = if (isClassActive) 1.5.dp else 1.dp,
                            color = if (isClassActive) BirizTurquoise else if (isDarkTheme) BirizDarkCardBorder else BirizBorder
                        ),
                        shadowElevation = if (isClassActive) 2.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag("tab_class_summary")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isListComplete) BirizTurquoise else BirizAmber,
                                    modifier = Modifier.size(8.dp)
                                ) {}
                                Spacer(modifier = Modifier.width(7.dp))
                                Text(
                                    text = "${classRoom.gradeLevel}/${classRoom.section} Sınıfı",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isClassActive) {
                                        if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                                    } else {
                                        if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = if (isClassActive) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isClassActive) "Kapat" else "Aç",
                                tint = if (isClassActive) BirizTurquoise else if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // 2. SEKME: Öğrenci Kayıt Alanı (+ Öğrenci Ekle)
                    Surface(
                        onClick = {
                            activeTopTab = if (isRegActive) ClassListTopTab.NONE else ClassListTopTab.STUDENT_REGISTRATION
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = when {
                            isRegActive -> if (isDarkTheme) Color(0xFF0C1F38) else Color(0xFFF0F7FF)
                            else -> if (isDarkTheme) Color(0x350A162B) else Color.White
                        },
                        border = BorderStroke(
                            width = if (isRegActive) 1.5.dp else 1.dp,
                            color = if (isRegActive) BirizTurquoise else if (isDarkTheme) BirizDarkCardBorder else BirizBorder
                        ),
                        shadowElevation = if (isRegActive) 2.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag("tab_student_registration")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    tint = if (isRegActive) BirizTurquoise else if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "+ Öğrenci Ekle",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isRegActive) {
                                        if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                                    } else {
                                        if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = if (isRegActive) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isRegActive) "Kapat" else "Aç",
                                tint = if (isRegActive) BirizTurquoise else if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // AÇILIR İÇERİK 2: Öğrenci Kayıt Paneli
                AnimatedVisibility(visible = activeTopTab == ClassListTopTab.STUDENT_REGISTRATION) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (isDarkTheme) Color(0xFF0C1F38) else Color(0xFFF0F7FF),
                            border = BorderStroke(
                                1.5.dp,
                                if (isDarkTheme) BirizTurquoise.copy(alpha = 0.6f) else Color(0xFF93C5FD)
                            ),
                            shadowElevation = if (isDarkTheme) 0.dp else 2.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("student_entry_panel")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {

                    // Segmented Controller: [Tek Öğrenci Ekle] | [Toplu Yükleme (Excel)] - Kompakt Yükseklik (34dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDarkTheme) Color(0x50061224) else Color(0xFFE2E8F0))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val singleSelected = entryMode == StudentEntryMode.SINGLE_ENTRY
                        Surface(
                            onClick = {
                                entryMode = StudentEntryMode.SINGLE_ENTRY
                                formError = null
                            },
                            shape = RoundedCornerShape(9.dp),
                            color = if (singleSelected) {
                                if (isDarkTheme) BirizNavyDark else Color.White
                            } else Color.Transparent,
                            shadowElevation = if (singleSelected) 2.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .testTag("tab_single_student_entry")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = if (singleSelected) {
                                            if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                                        } else if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Tek Öğrenci Ekle",
                                        fontSize = 11.5.sp,
                                        fontWeight = if (singleSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (singleSelected) {
                                            if (isDarkTheme) Color.White else BirizNavyPrimary
                                        } else if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                                    )
                                }
                            }
                        }

                        val batchSelected = entryMode == StudentEntryMode.BATCH_EXCEL
                        Surface(
                            onClick = {
                                entryMode = StudentEntryMode.BATCH_EXCEL
                                formError = null
                            },
                            shape = RoundedCornerShape(9.dp),
                            color = if (batchSelected) {
                                if (isDarkTheme) BirizNavyDark else Color.White
                            } else Color.Transparent,
                            shadowElevation = if (batchSelected) 2.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .testTag("tab_batch_excel_entry")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.TableChart,
                                        contentDescription = null,
                                        tint = if (batchSelected) {
                                            if (isDarkTheme) BirizAmber else BirizNavyPrimary
                                        } else if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Toplu Yükleme (Excel)",
                                        fontSize = 11.5.sp,
                                        fontWeight = if (batchSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (batchSelected) {
                                            if (isDarkTheme) Color.White else BirizNavyPrimary
                                        } else if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // --- MOD 1: TEK ÖĞRENCİ EKLE FORMU (KOMPAKT 42DP TEK SATIR) ---
                    if (entryMode == StudentEntryMode.SINGLE_ENTRY) {
                        val uppercaseFirstName = TurkishTextHelper.toTurkishUpperCase(inputFirstName)
                        val uppercaseLastName = TurkishTextHelper.toTurkishUpperCase(inputLastName)

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Satır 1: Kız/Erkek butonu ve Okul No yan yana (Tam 42.dp tek satır yükseklik)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. Cinsiyet Butonu (Kız / Erkek - 42.dp tek satır)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isDarkTheme) Color(0x350A162B) else Color.White,
                                    border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
                                    modifier = Modifier
                                        .weight(1.1f)
                                        .fillMaxHeight()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(3.dp),
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val isKiz = inputGender == "Kız"
                                        Surface(
                                            onClick = { inputGender = "Kız" },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isKiz) Color(0xFFDB2777) else Color.Transparent,
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .testTag("gender_select_kiz")
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "Kız ♀",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isKiz) Color.White else if (isDarkTheme) Color(0xFFF472B6) else Color(0xFF9D174D)
                                                )
                                            }
                                        }

                                        val isErkek = inputGender == "Erkek"
                                        Surface(
                                            onClick = { inputGender = "Erkek" },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isErkek) Color(0xFF0284C7) else Color.Transparent,
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .testTag("gender_select_erkek")
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "Erkek ♂",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isErkek) Color.White else if (isDarkTheme) Color(0xFF38BDF8) else Color(0xFF0369A1)
                                                )
                                            }
                                        }
                                    }
                                }

                                // 2. Okul No (42.dp tek satır kutucuk, örnek açıklamalar kaldırıldı)
                                BirizCompactInputField(
                                    value = inputStudentNo,
                                    onValueChange = {
                                        if (it.all { c -> c.isDigit() } && it.length <= 6) {
                                            inputStudentNo = it
                                            formError = null
                                        }
                                    },
                                    placeholder = "Okul No",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    isDarkTheme = isDarkTheme,
                                    testTag = "input_student_no",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Satır 2: Öğrenci Adı ve Soyadı yan yana (Tam 42.dp tek satır yükseklik, örnek açıklamalar kaldırıldı)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 3. Öğrenci Adı (örnek açıklama kaldırıldı)
                                BirizCompactInputField(
                                    value = uppercaseFirstName,
                                    onValueChange = {
                                        inputFirstName = it
                                        formError = null
                                    },
                                    placeholder = "Öğrenci Adı",
                                    isDarkTheme = isDarkTheme,
                                    testTag = "input_student_first_name",
                                    modifier = Modifier.weight(1f)
                                )

                                // 4. Soyadı (örnek açıklama kaldırıldı)
                                BirizCompactInputField(
                                    value = uppercaseLastName,
                                    onValueChange = {
                                        inputLastName = it
                                        formError = null
                                    },
                                    placeholder = "Soyadı",
                                    isDarkTheme = isDarkTheme,
                                    testTag = "input_student_last_name",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (formError != null) {
                                Text(
                                    text = formError ?: "",
                                    color = BirizError,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Ekle Butonu - Kompakt (40dp)
                            val addBtnContentColor = if (isDarkTheme) BirizNavyDark else Color.White
                            Button(
                                onClick = {
                                    val no = inputStudentNo.toIntOrNull()
                                    if (no == null || no <= 0) {
                                        formError = "Lütfen geçerli ve sayısal bir Okul No giriniz."
                                        return@Button
                                    }
                                    if (uppercaseFirstName.trim().length < 2) {
                                        formError = "Lütfen öğrencinin Adını giriniz."
                                        return@Button
                                    }
                                    if (uppercaseLastName.trim().length < 2) {
                                        formError = "Lütfen öğrencinin Soyadını giriniz."
                                        return@Button
                                    }
                                    val fullStudentName = "${uppercaseFirstName.trim()} ${uppercaseLastName.trim()}"
                                    if (students.any { it.studentNumber == no.toString() }) {
                                        formError = "$no numaralı öğrenci sınıf listesinde zaten kayıtlıdır!"
                                        return@Button
                                    }

                                    onAddStudent(no, fullStudentName, inputGender)
                                    inputStudentNo = ""
                                    inputFirstName = ""
                                    inputLastName = ""
                                    formError = null
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp)
                                    .testTag("btn_submit_student"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                                    contentColor = addBtnContentColor
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = addBtnContentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "+ Öğrenciyi Listeye Ekle",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = addBtnContentColor
                                )
                            }
                        }
                    } else {
                        // --- MOD 2: TOPLU YÜKLEME (EXCEL) PANELİ ---
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .border(
                                    BorderStroke(1.5.dp, BirizAmber.copy(alpha = 0.5f)),
                                    RoundedCornerShape(16.dp)
                                )
                                .background(if (isDarkTheme) Color(0x25E67E22) else Color(0xFFFFFBEB))
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = null,
                                tint = BirizAmber,
                                modifier = Modifier.size(36.dp)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Excel / CSV ile Toplu Öğrenci Aktarımı",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) Color.White else BirizNavyPrimary,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Standart format sütunları: 1. Okul No | 2. Adı | 3. Soyadı | 4. Cinsiyet\nExcel (.xlsx) şablonunu indirip öğrencilerinizi tek tıkla topluca aktarabilirsiniz.",
                                fontSize = 12.sp,
                                color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 1. Örnek Şablon İndir Butonu (.xlsx)
                                OutlinedButton(
                                    onClick = {
                                        val templateUri = ExcelPdfHelper.createSampleTemplateFile(context)
                                        ExcelPdfHelper.shareFile(
                                            context = context,
                                            uri = templateUri,
                                            mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                            chooserTitle = "Örnek Excel Şablonunu Kaydet / Paylaş"
                                        )
                                        Toast.makeText(context, "Örnek Excel şablonu (.xlsx) hazırlandı.", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_download_template"),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, BirizAmber)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FileDownload,
                                        contentDescription = null,
                                        tint = BirizAmber,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "📥 Şablon İndir (.xlsx)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BirizAmber
                                    )
                                }

                                // 2. Dosya Seç / Yükle Butonu
                                Button(
                                    onClick = {
                                        filePickerLauncher.launch("*/*")
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_pick_excel_file"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BirizAmber,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FileUpload,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Liste Yükle (Excel)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

                // AÇILIR İÇERİK 1: 4/D Sınıfı Bilgi ve Özet Paneli
                AnimatedVisibility(visible = activeTopTab == ClassListTopTab.CLASS_SUMMARY) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        BentoCard(
                            isDark = isDarkTheme,
                            glowColor = BirizNavyPrimary.copy(alpha = 0.2f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // 1. Satır: Sınıf Bilgisi & Paylaş Butonu (Ferah ve Kesintisiz)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 8.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isListComplete) BirizTurquoise else BirizAmber,
                                            modifier = Modifier.size(10.dp)
                                        ) {}
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "${classRoom.gradeLevel}/${classRoom.section} Sınıfı",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                                            )
                                            Text(
                                                text = classRoom.schoolName,
                                                fontSize = 11.5.sp,
                                                color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    // Sağ 1: Standart Paylaş Butonu ve Menüsü
                                    Box {
                                        Button(
                                            onClick = { isShareMenuOpen = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = BirizNavyPrimary),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                            modifier = Modifier.testTag("classlist_share_button")
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
                                            expanded = isShareMenuOpen,
                                            onDismissRequest = { isShareMenuOpen = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("📋 WhatsApp / SMS Metni Kopyala", fontSize = 12.sp) },
                                                onClick = {
                                                    isShareMenuOpen = false
                                                    if (students.isEmpty()) {
                                                        Toast.makeText(context, "Önce listeye en az 1 öğrenci eklemelisiniz.", Toast.LENGTH_SHORT).show()
                                                        return@DropdownMenuItem
                                                    }
                                                    val shareText = ExcelPdfHelper.generateClassListShareText(classRoom, students)
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                    clipboard.setPrimaryClip(ClipData.newPlainText("Sınıf Listesi", shareText))
                                                    Toast.makeText(context, "Sınıf listesi panoya kopyalandı! 📋", Toast.LENGTH_SHORT).show()
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("📊 Excel (.xlsx) İndir / Paylaş", fontSize = 12.sp) },
                                                onClick = {
                                                    isShareMenuOpen = false
                                                    if (students.isEmpty()) {
                                                        Toast.makeText(context, "Önce listeye en az 1 öğrenci eklemelisiniz.", Toast.LENGTH_SHORT).show()
                                                        return@DropdownMenuItem
                                                    }
                                                    try {
                                                        val excelUri = ExcelPdfHelper.exportStudentsToExcel(context, classRoom, students)
                                                        ExcelPdfHelper.shareFile(
                                                            context = context,
                                                            uri = excelUri,
                                                            mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                                            chooserTitle = "Sınıf Listesi Excel Belgesini Aç / Paylaş"
                                                        )
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("🖨️ PDF İndir / Yazdır", fontSize = 12.sp) },
                                                onClick = {
                                                    isShareMenuOpen = false
                                                    if (students.isEmpty()) {
                                                        Toast.makeText(context, "Önce listeye en az 1 öğrenci eklemelisiniz.", Toast.LENGTH_SHORT).show()
                                                        return@DropdownMenuItem
                                                    }
                                                    try {
                                                        val pdfUri = ExcelPdfHelper.exportStudentsToPdf(context, classRoom, students)
                                                        ExcelPdfHelper.shareFile(
                                                            context = context,
                                                            uri = pdfUri,
                                                            mimeType = "application/pdf",
                                                            chooserTitle = "Sınıf Listesi PDF Belgesini Aç / Paylaş"
                                                        )
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("🎟️ Veli Katılım Kartları (PDF)", fontSize = 12.sp) },
                                                onClick = {
                                                    isShareMenuOpen = false
                                                    if (students.isEmpty()) {
                                                        Toast.makeText(context, "Kupon oluşturmak için listeye en az 1 öğrenci eklemelisiniz.", Toast.LENGTH_SHORT).show()
                                                        return@DropdownMenuItem
                                                    }
                                                    try {
                                                        val cardsUri = ExcelPdfHelper.exportParentInviteCardsPdf(context, classRoom, students)
                                                        ExcelPdfHelper.shareFile(
                                                            context = context,
                                                            uri = cardsUri,
                                                            mimeType = "application/pdf",
                                                            chooserTitle = "Veli Katılım Kartlarını (PDF) Aç / Yazdır / Paylaş"
                                                        )
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("📲 Veli İndirme Karekodu (QR)", fontSize = 12.sp) },
                                                onClick = {
                                                    isShareMenuOpen = false
                                                    showParentQrDialog = true
                                                }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // 2. Satır: Yan Yana İki Şık İstatistik Rozeti (Ferah, Net ve Kesintisiz Okunabilir)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("student_stats_summary_bar"),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Sol Rozet: Öğrenci Sayısı ve Kız/Erkek Dağılımı
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isDarkTheme) Color(0x350A162B) else Color(0xFFF1F5F9),
                                        border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = if (isDarkTheme) BirizTurquoise.copy(alpha = 0.2f) else Color(0xFFDBEAFE),
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Group,
                                                        contentDescription = null,
                                                        tint = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = "$totalStudents Öğrenci",
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDarkTheme) Color.White else BirizNavyPrimary
                                                )
                                                Text(
                                                    text = "$femaleCount Kız, $maleCount Erkek",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                                                )
                                            }
                                        }
                                    }

                                    // Sağ Rozet: Veli Bağlantısı ve Katılım Oranı
                                    val hasConnectedParents = parentLinkedCount > 0
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (hasConnectedParents) {
                                            if (isDarkTheme) Color(0x2510B981) else Color(0xFFF0FDF4)
                                        } else {
                                            if (isDarkTheme) Color(0x25E67E22) else Color(0xFFFFFBEB)
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            if (hasConnectedParents) Color(0xFF86EFAC) else Color(0xFFFDE68A)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = if (hasConnectedParents) {
                                                    if (isDarkTheme) Color(0x4010B981) else Color(0xFFDCFCE7)
                                                } else {
                                                    if (isDarkTheme) Color(0x40E67E22) else Color(0xFFFEF3C7)
                                                },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = if (hasConnectedParents) Icons.Default.CheckCircle else Icons.Default.AccessTime,
                                                        contentDescription = null,
                                                        tint = if (hasConnectedParents) Color(0xFF10B981) else BirizAmber,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = "$parentLinkedCount Veli Bağlı",
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (hasConnectedParents) {
                                                        if (isDarkTheme) Color(0xFF4ADE80) else Color(0xFF166534)
                                                    } else {
                                                        if (isDarkTheme) Color(0xFFFBBF24) else Color(0xFFB45309)
                                                    }
                                                )
                                                Text(
                                                    text = if (totalStudents > 0) "%$parentLinkedPercent Katılım" else "%0 Katılım",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (hasConnectedParents) {
                                                        if (isDarkTheme) Color(0xFF86EFAC) else Color(0xFF15803D)
                                                    } else {
                                                        if (isDarkTheme) Color(0xFFFDE68A) else Color(0xFFD97706)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // --- KESİLEBİLİR VELİ KATILIM KARTLARI / KUPONLARI BUTONU (PDF) ---
                                Spacer(modifier = Modifier.height(10.dp))

                                Surface(
                                    onClick = {
                                        if (students.isEmpty()) {
                                            Toast.makeText(context, "Kupon oluşturmak için listeye en az 1 öğrenci eklemelisiniz.", Toast.LENGTH_SHORT).show()
                                            return@Surface
                                        }
                                        val cardsUri = ExcelPdfHelper.exportParentInviteCardsPdf(context, classRoom, students)
                                        ExcelPdfHelper.shareFile(
                                            context = context,
                                            uri = cardsUri,
                                            mimeType = "application/pdf",
                                            chooserTitle = "Veli Katılım Kartlarını (PDF) Aç / Yazdır / Paylaş"
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isDarkTheme) Color(0x30E67E22) else Color(0xFFFEF3C7),
                                    border = BorderStroke(1.dp, BirizAmber.copy(alpha = 0.65f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("btn_export_invite_cards_pdf")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCut,
                                            contentDescription = null,
                                            tint = if (isDarkTheme) Color(0xFFFBBF24) else Color(0xFFB45309),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "✂️ Veli Katılım Kartlarını Yazdır (Kesilebilir PDF)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isDarkTheme) Color(0xFFFBBF24) else Color(0xFFB45309)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 3. AKILLI SIRALAMA & FİLTRELEME ÇİPLERİ ---
        if (students.isNotEmpty()) {
            item {
                // Akıllı Sıralama & Filtreleme Çipleri (Gereksiz alan ve bilgi tekrarı önlendi)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Çip 1: Numaraya Göre
                    val isNumSelected = activeFilter == StudentSortFilter.BY_NUMBER
                    Surface(
                        onClick = { activeFilter = StudentSortFilter.BY_NUMBER },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isNumSelected) {
                            if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                        } else {
                            if (isDarkTheme) Color(0x250A162B) else Color(0xFFF1F5F9)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isNumSelected) Color.Transparent else if (isDarkTheme) BirizDarkCardBorder else BirizBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("filter_chip_number")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = "Numaraya Göre",
                                fontSize = 11.5.sp,
                                fontWeight = if (isNumSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isNumSelected) {
                                    if (isDarkTheme) BirizNavyDark else Color.White
                                } else {
                                    if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Çip 2: Alfabetik (A-Z)
                    val isAlphaSelected = activeFilter == StudentSortFilter.ALPHABETICAL
                    Surface(
                        onClick = { activeFilter = StudentSortFilter.ALPHABETICAL },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isAlphaSelected) {
                            if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                        } else {
                            if (isDarkTheme) Color(0x250A162B) else Color(0xFFF1F5F9)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isAlphaSelected) Color.Transparent else if (isDarkTheme) BirizDarkCardBorder else BirizBorder
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("filter_chip_alpha")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = "A-Z Alfabetik",
                                fontSize = 11.5.sp,
                                fontWeight = if (isAlphaSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isAlphaSelected) {
                                    if (isDarkTheme) BirizNavyDark else Color.White
                                } else {
                                    if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Çip 3: Velisi Bağlanmayanlar
                    val isUnlinkedSelected = activeFilter == StudentSortFilter.UNLINKED_ONLY
                    Surface(
                        onClick = { activeFilter = StudentSortFilter.UNLINKED_ONLY },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isUnlinkedSelected) {
                            BirizAmber
                        } else {
                            if (isDarkTheme) Color(0x25E67E22) else Color(0xFFFEF3C7)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isUnlinkedSelected) BirizAmber else if (isDarkTheme) BirizAmber.copy(alpha = 0.4f) else Color(0xFFFDE68A)
                        ),
                        modifier = Modifier
                            .weight(1.35f)
                            .height(36.dp)
                            .testTag("filter_chip_unlinked")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Veli Bekleyenler ($unlinkedCount)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isUnlinkedSelected) {
                                    Color.White
                                } else {
                                    if (isDarkTheme) Color(0xFFFBBF24) else Color(0xFFB45309)
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // --- 4. ÖĞRENCİ LİSTESİ BENTO KARTLARI ---
        if (students.isEmpty()) {
            item {
                BentoCard(
                    isDark = isDarkTheme,
                    shape = RoundedCornerShape(18.dp),
                    glowColor = BirizAmber.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 18.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isDarkTheme) Color(0x30E67E22) else Color(0xFFFFF3E0),
                            modifier = Modifier.size(54.dp),
                            border = BorderStroke(1.5.dp, BirizAmber)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Group,
                                    contentDescription = null,
                                    tint = BirizAmber,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Henüz Öğrenci Eklenmedi",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Yukarıdaki formu kullanarak tek tek veya Excel yüklemesiyle topluca sınıf listenizi oluşturabilirsiniz. İlk öğrenci kaydedildiğinde diğer 11 modül otomatik olarak açılacaktır.",
                            fontSize = 12.sp,
                            color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        } else if (displayedStudents.isEmpty() && activeFilter == StudentSortFilter.UNLINKED_ONLY) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDarkTheme) Color(0x2510B981) else Color(0xFFDCFCE7),
                    border = BorderStroke(1.dp, Color(0xFF10B981)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .testTag("all_parents_linked_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tebrikler! Tüm Veliler Sisteme Bağlı 🎉",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) Color.White else Color(0xFF14532D)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Sınıfınızdaki tüm öğrencilerin veli bağlantısı tamamlanmış durumda. Kupon verilecek veli kalmadı.",
                            fontSize = 11.5.sp,
                            color = if (isDarkTheme) BirizDarkTextSecondary else Color(0xFF166534),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(displayedStudents, key = { it.id }) { student ->
                StudentBentoCardItem(
                    student = student,
                    isDark = isDarkTheme,
                    onClick = { selectedStudentForDetail = student }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(84.dp)) // Dock bar payı
        }
    }

    // --- 1. ÖĞRENCİ DETAY PENCERESİ (MODAL) ---
    selectedStudentForDetail?.let { currentModalStudent ->
        val student = students.find { it.id == currentModalStudent.id } ?: currentModalStudent
        StudentDetailDialog(
            student = student,
            classRoom = classRoom,
            isDarkTheme = isDarkTheme,
            onDismiss = { selectedStudentForDetail = null },
            onEdit = {
                editingStudent = student
                selectedStudentForDetail = null
            },
            onDelete = {
                studentToDelete = student
                selectedStudentForDetail = null
            },
            onCopyCode = {
                val inviteMessage = buildString {
                    appendLine("Sayın Velimiz,")
                    appendLine()
                    appendLine("BİRİZ İlkokul Dijital Sınıf ve Veli Yönetim Sistemi üzerinden öğrencimiz ${student.fullName} (${classRoom.gradeLevel}/${classRoom.section} Sınıfı - ${classRoom.schoolName}) için veli katılım kodunuz:")
                    appendLine()
                    appendLine("🔑 Veli Katılım Kodu: ${student.parentInviteCode}")
                    appendLine()
                    appendLine("BİRİZ uygulamasını indirip bu kod ile giriş yaparak öğrencimizin ders programı, oturma planı ve sınıf duyurularını anlık takip edebilirsiniz.")
                    appendLine("📲 Uygulama İndirme Linki: https://github.com/CHNZL/biriz/releases/latest/download/Biriz.apk")
                }
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Veli Katılım Kodu", inviteMessage))
                Toast.makeText(context, "${student.fullName} için Veli Kodu ve davet mesajı kopyalandı! 📋 Paylaşılıyor...", Toast.LENGTH_SHORT).show()

                try {
                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_SUBJECT, "${student.fullName} - BİRİZ Veli Katılım Kodu")
                        putExtra(android.content.Intent.EXTRA_TEXT, inviteMessage)
                    }
                    val chooser = android.content.Intent.createChooser(shareIntent, "${student.fullName} Veli Katılım Kodunu Paylaş")
                    context.startActivity(chooser)
                } catch (_: Exception) {}
            },
            onTogglePriority = {
                onTogglePriority?.invoke(student.id)
                selectedStudentForDetail = student.copy(isPriority = !student.isPriority)
            },
            onSimulateOrUpdateParent = { name, email ->
                val nowTime = java.text.SimpleDateFormat("dd MMMM yyyy, HH:mm", java.util.Locale("tr")).format(java.util.Date())
                val lastLogin = "Bugün, " + java.text.SimpleDateFormat("HH:mm", java.util.Locale("tr")).format(java.util.Date())
                onUpdateParentInfo?.invoke(student.id, name, email, nowTime, lastLogin)
                selectedStudentForDetail = student.copy(
                    parentName = name,
                    parentEmail = email,
                    parentLinkedAt = nowTime,
                    parentLastLoginAt = lastLogin
                )
                Toast.makeText(context, "$name sisteme başarıyla bağlandı.", Toast.LENGTH_SHORT).show()
            },
            onDisconnectParent = {
                onUpdateParentInfo?.invoke(student.id, null, null, null, null)
                selectedStudentForDetail = student.copy(
                    parentName = null,
                    parentEmail = null,
                    parentLinkedAt = null,
                    parentLastLoginAt = null
                )
                Toast.makeText(context, "Veli bağlantısı kaldırıldı.", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // --- 2. SİLME ONAY DİYALOĞU ---
    studentToDelete?.let { student ->
        DeleteStudentConfirmDialog(
            student = student,
            isDarkTheme = isDarkTheme,
            onDismiss = { studentToDelete = null },
            onConfirm = {
                onDeleteStudent(student)
                studentToDelete = null
                Toast.makeText(context, "${student.fullName} listeden silindi.", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // --- 3. ÖĞRENCİ DÜZENLEME DİYALOĞU (MODAL) ---
    editingStudent?.let { student ->
        EditStudentDialog(
            student = student,
            isDarkTheme = isDarkTheme,
            onDismiss = { editingStudent = null },
            onConfirm = { newNo, newName, newGender ->
                onEditStudent(student.id, newNo, newName, newGender)
                editingStudent = null
                Toast.makeText(context, "Öğrenci bilgileri güncellendi.", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // --- 4. VELİ UYGULAMA İNDİRME KAREKODU DİYALOĞU (QR MODAL) ---
    if (showParentQrDialog) {
        ParentDownloadQrDialog(
            isDarkTheme = isDarkTheme,
            onDismiss = { showParentQrDialog = false },
            onPrintParentCards = {
                if (students.isNotEmpty()) {
                    try {
                        val cardsUri = ExcelPdfHelper.exportParentInviteCardsPdf(context, classRoom, students)
                        ExcelPdfHelper.shareFile(
                            context = context,
                            uri = cardsUri,
                            mimeType = "application/pdf",
                            chooserTitle = "Veli Katılım Kartlarını (PDF) Aç / Yazdır / Paylaş"
                        )
                    } catch (e: Exception) {
                        Toast.makeText(context, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "Kupon oluşturmak için listeye en az 1 öğrenci eklemelisiniz.", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

/**
 * Bento Izgara Standartlarına Uygun Sade ve Liste Görünümlü Öğrenci Kartı
 * - Sol: Cinsiyete Göre Renklendirilmiş Okul No Rozeti (Kız: Pembe, Erkek: Mavi)
 * - Orta: Öğrenci Adı Soyadı + Bağlı Veli Durum Rozeti
 * - Sağ: Şık ve kompakt Detay göstergesi
 */
@Composable
fun StudentBentoCardItem(
    student: Student,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val isKiz = student.gender.equals("Kız", ignoreCase = true)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = when {
            isKiz -> if (isDark) Color(0x30F472B6) else Color.White
            else -> if (isDark) Color(0x3038BDF8) else Color.White
        },
        border = BorderStroke(
            1.dp,
            when {
                isKiz -> if (isDark) Color(0xFFF472B6) else Color(0xFFFCE7F3)
                else -> if (isDark) Color(0xFF38BDF8) else Color(0xFFE0F2FE)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("student_card_${student.studentNumber}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Sol: Okul No Rozeti + Ad Soyad + Rozetler
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Okul No Rozeti (Grup Listeleriyle Birebir: Kız -> Pembe, Erkek -> Mavi)
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isKiz) Color(0xFFFCE7F3) else Color(0xFFE0F2FE)
                ) {
                    Text(
                        text = "#${student.studentNumber}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isKiz) Color(0xFFDB2777) else Color(0xFF0284C7),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Adı Soyadı & Veli Bağlantı Rozeti
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = student.fullName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color.White else Color(0xFF1E293B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (student.isParentLinked) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDark) Color(0x3010B981) else Color(0xFFDCFCE7),
                            border = BorderStroke(0.8.dp, if (isDark) Color(0xFF10B981) else Color(0xFF86EFAC))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF10B981),
                                    modifier = Modifier.size(5.dp)
                                ) {}
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Veli Bağlı",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF15803D)
                                )
                            }
                        }
                    }

                    if (student.isPriority) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDark) Color(0x35F59E0B) else Color(0xFFFEF3C7),
                            border = BorderStroke(0.8.dp, if (isDark) Color(0xFFF59E0B) else Color(0xFFFCD34D))
                        ) {
                            Text(
                                text = "👓 Ön Sıra",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) BirizAmber else Color(0xFFB45309),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Sağ: Cinsiyet Simgesi (♀ / ♂) + Kompakt Detay Gör Rozeti
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isKiz) "♀" else "♂",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isKiz) Color(0xFFEC4899) else Color(0xFF0284C7)
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isDark) Color(0x25FFFFFF) else Color(0xFFF1F5F9),
                    border = BorderStroke(0.8.dp, if (isDark) Color(0x30FFFFFF) else Color(0xFFCBD5E1))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Detay",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) Color.White.copy(alpha = 0.9f) else BirizNavyPrimary
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Öğrenci Detayı",
                            tint = if (isDark) BirizTurquoise else BirizNavyPrimary,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Veli Bilgi Satırı Elemanı
 */
@Composable
fun ParentDetailRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    isDarkTheme: Boolean,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onAction: (() -> Unit)? = null,
    valueHighlight: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isDarkTheme) Color(0x350A162B) else Color.White,
        border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted
                    )
                    Text(
                        text = value,
                        fontSize = 12.5.sp,
                        fontWeight = if (valueHighlight) FontWeight.ExtraBold else FontWeight.Bold,
                        color = if (valueHighlight) {
                            if (isDarkTheme) BirizTurquoise else Color(0xFF0284C7)
                        } else {
                            if (isDarkTheme) Color.White else BirizTextPrimary
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (actionIcon != null && onAction != null) {
                IconButton(onClick = onAction, modifier = Modifier.size(26.dp)) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = label,
                        tint = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Öğrenci Detay Penceresi (Modal / Dialog)
 * - Öğrenciye bağlanan bir veli varsa: Ad Soyad, E-posta, Bağlanma Tarihi, Sisteme Son Giriş gösterilir.
 * - Veli bağlanmamışsa: 8 Haneli Veli Katılım Kodu kopyalama butonu ile birlikte gösterilir.
 * - Öğrenci düzenleme ve silme işlemleri de bu pencerede yer alır.
 */
@Composable
fun StudentDetailDialog(
    student: Student,
    classRoom: ClassRoom,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCopyCode: () -> Unit,
    onTogglePriority: (() -> Unit)? = null,
    onSimulateOrUpdateParent: ((String, String) -> Unit)? = null,
    onDisconnectParent: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isKiz = student.gender.equals("Kız", ignoreCase = true)

    Dialog(onDismissRequest = onDismiss) {
        BentoCard(
            isDark = isDarkTheme,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Üst Çubuk: Başlık & Kapat İkonu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Öğrenci Detayı",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                    )

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Öğrenci Kimlik Kartı
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDarkTheme) Color(0x350F172A) else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDarkTheme) {
                                if (isKiz) Color(0x30DB2777) else Color(0x300284C7)
                            } else {
                                if (isKiz) Color(0xFFFCE7F3) else Color(0xFFE0F2FE)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isKiz) Color(0xFFF472B6) else Color(0xFF38BDF8)
                            ),
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = student.studentNumber,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isKiz) {
                                        if (isDarkTheme) Color(0xFFF472B6) else Color(0xFF9D174D)
                                    } else {
                                        if (isDarkTheme) Color(0xFF38BDF8) else Color(0xFF0369A1)
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = student.fullName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isDarkTheme) {
                                        if (isKiz) Color(0x25DB2777) else Color(0x250284C7)
                                    } else {
                                        if (isKiz) Color(0xFFFCE7F3) else Color(0xFFE0F2FE)
                                    }
                                ) {
                                    Text(
                                        text = if (isKiz) "Kız ♀" else "Erkek ♂",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isKiz) {
                                            if (isDarkTheme) Color(0xFFF472B6) else Color(0xFFBE185D)
                                        } else {
                                            if (isDarkTheme) Color(0xFF38BDF8) else Color(0xFF0284C7)
                                        },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${classRoom.gradeLevel}/${classRoom.section} Şubesi",
                                    fontSize = 11.sp,
                                    color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                onClick = { onTogglePriority?.invoke() },
                                shape = RoundedCornerShape(8.dp),
                                color = if (student.isPriority) {
                                    if (isDarkTheme) Color(0x35F59E0B) else Color(0xFFFEF3C7)
                                } else {
                                    if (isDarkTheme) Color(0x20334E68) else Color(0xFFF1F5F9)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (student.isPriority) Color(0xFFF59E0B) else if (isDarkTheme) BirizDarkCardBorder else BirizBorder
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = if (student.isPriority) "👓 Ön Sıra Önceliği Aktif (Gözlük/Boy)" else "+ 👓 Ön Sıra İhtiyacı Ekle",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (student.isPriority) {
                                            if (isDarkTheme) BirizAmber else Color(0xFFB45309)
                                        } else {
                                            if (isDarkTheme) Color.LightGray else Color(0xFF475569)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // --- VELİ BİLGİ ALANI: BAĞLI VELİ MEVCUTSA BİLGİLER, DEĞİLSE KATILIM KODU ---
                if (student.isParentLinked) {
                    // 1. BAĞLI VELİ BİLGİLERİ KARTI
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDarkTheme) Color(0x2510B981) else Color(0xFFF0FDF4),
                        border = BorderStroke(1.2.dp, if (isDarkTheme) Color(0xFF10B981).copy(alpha = 0.6f) else Color(0xFF86EFAC)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            // Üst Başlık & Aktif Rozeti
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isDarkTheme) Color(0x3010B981) else Color(0xFFDCFCE7),
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Bağlı Veli Bilgileri",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isDarkTheme) Color(0xFF6EE7B7) else Color(0xFF166534)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isDarkTheme) Color(0x4010B981) else Color(0xFFDCFCE7)
                                ) {
                                    Text(
                                        text = "● Sistemde Aktif",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkTheme) Color(0xFF34D399) else Color(0xFF15803D),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // 1. Veli Ad Soyad
                            ParentDetailRowItem(
                                icon = Icons.Default.Person,
                                label = "Veli Adı Soyadı",
                                value = student.parentName ?: "Belirtilmemiş",
                                isDarkTheme = isDarkTheme
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // 2. Veli E-posta
                            ParentDetailRowItem(
                                icon = Icons.Default.Email,
                                label = "E-posta Adresi",
                                value = student.parentEmail ?: "Belirtilmemiş",
                                isDarkTheme = isDarkTheme,
                                actionIcon = Icons.Default.ContentCopy,
                                onAction = {
                                    student.parentEmail?.let { email ->
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Veli E-posta", email))
                                        Toast.makeText(context, "E-posta kopyalandı: $email", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // 3. Bağlanma Tarihi
                            ParentDetailRowItem(
                                icon = Icons.Default.CalendarToday,
                                label = "Bağlanma Tarihi",
                                value = student.parentLinkedAt ?: "Belirtilmemiş",
                                isDarkTheme = isDarkTheme
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // 4. Sisteme Son Giriş
                            ParentDetailRowItem(
                                icon = Icons.Default.AccessTime,
                                label = "Sisteme Son Giriş",
                                value = student.parentLastLoginAt ?: "Giriş bilgisi bulunmuyor",
                                isDarkTheme = isDarkTheme,
                                valueHighlight = true
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Veli Katılım Kodu Referans Satırı
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDarkTheme) Color(0x300A162B) else Color(0xFFF1F5F9),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Veli Katılım Kodu:",
                                        fontSize = 11.sp,
                                        color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = student.parentInviteCode,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                                        )
                                        IconButton(
                                            onClick = onCopyCode,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Kodu Kopyala",
                                                tint = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Veli Bağlantısını Kaldır Butonu
                            if (onDisconnectParent != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Surface(
                                        onClick = onDisconnectParent,
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color.Transparent
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LinkOff,
                                                contentDescription = null,
                                                tint = BirizError,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Bağlantıyı Kaldır",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = BirizError
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // 2. VELİ BAĞLANTISI BEKLENİYOR KARTI (8 HANELİ KOD & TEST AKSİYONU)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDarkTheme) Color(0x35E67E22) else Color(0xFFFEF3C7),
                        border = BorderStroke(1.2.dp, BirizAmber.copy(alpha = 0.7f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "VELİ BAĞLANTI DURUMU",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkTheme) Color(0xFFFBBF24) else Color(0xFFB45309)
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isDarkTheme) Color(0x40E67E22) else Color(0xFFFDE68A)
                                ) {
                                    Text(
                                        text = "⏳ Henüz Bağlanmadı",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkTheme) Color(0xFFFBBF24) else Color(0xFF92400E),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "VELİ KATILIM KODU (8 HANELİ)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) Color(0xFFFBBF24) else Color(0xFFB45309)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Aralıklı 8 Haneli Kod
                            val spacedCode = student.parentInviteCode.map { "$it " }.joinToString("").trim()
                            Text(
                                text = spacedCode,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = if (isDarkTheme) BirizAmber else Color(0xFF92400E)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Kodu Kopyala & Veliye Paylaş Butonu
                            Button(
                                onClick = onCopyCode,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BirizAmber,
                                    contentColor = Color.White
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_detail_copy_code")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Paylaş",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Kodu Kopyala & Veliye Paylaş (WhatsApp vb.)",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Veli bu 8 haneli kod ile BİRİZ Veli uygulamasına giriş yaptığında; velinin Adı Soyadı, E-postası, Bağlanma Tarihi ve Sisteme Son Giriş saati burada otomatik olarak görüntülenecektir.",
                                fontSize = 10.5.sp,
                                color = if (isDarkTheme) BirizDarkTextSecondary else Color(0xFF78350F),
                                textAlign = TextAlign.Center,
                                lineHeight = 14.5.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Alt Eylem Butonları: Sil (🗑️) ve Düzenle (✏️)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Sil Butonu (Kırmızı)
                    OutlinedButton(
                        onClick = onDelete,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFEF4444)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isDarkTheme) Color(0x20EF4444) else Color(0xFFFEF2F2)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_detail_delete_student")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sil",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444)
                        )
                    }

                    // Düzenle Butonu (Lacivert / Turkuaz)
                    Button(
                        onClick = onEdit,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                            contentColor = if (isDarkTheme) BirizNavyDark else Color.White
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("btn_detail_edit_student")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = if (isDarkTheme) BirizNavyDark else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Düzenle",
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) BirizNavyDark else Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Öğrenci Silme Onay Diyaloğu (Kazara Silmeyi Önler)
 */
@Composable
fun DeleteStudentConfirmDialog(
    student: Student,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        BentoCard(
            isDark = isDarkTheme,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
            ) {
                Text(
                    text = "Öğrenciyi Silmek İstediğinize Emin misiniz?",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "${student.studentNumber} numaralı ${student.fullName} sınıf listesinden silinecektir. Bu işlem geri alınamaz.",
                    fontSize = 13.sp,
                    color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("İptal", color = if (isDarkTheme) Color.White else BirizNavyPrimary)
                    }

                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF4444),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("btn_confirm_delete_student")
                    ) {
                        Text("Evet, Sil", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * Öğrenci Bilgilerini Düzenleme Modal Diyaloğu
 */
@Composable
fun EditStudentDialog(
    student: Student,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int, String, String) -> Unit
) {
    val initialParts = student.fullName.trim().split(" ")
    val initialFirst = if (initialParts.size > 1) initialParts.dropLast(1).joinToString(" ") else initialParts.firstOrNull() ?: ""
    val initialLast = if (initialParts.size > 1) initialParts.last() else ""

    var editNoInput by remember { mutableStateOf(student.studentNumber) }
    var editFirstNameInput by remember { mutableStateOf(initialFirst) }
    var editLastNameInput by remember { mutableStateOf(initialLast) }
    var editGender by remember { mutableStateOf(student.gender) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val uppercaseFirstName = TurkishTextHelper.toTurkishUpperCase(editFirstNameInput)
    val uppercaseLastName = TurkishTextHelper.toTurkishUpperCase(editLastNameInput)

    Dialog(onDismissRequest = onDismiss) {
        BentoCard(
            isDark = isDarkTheme,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Öğrenci Bilgilerini Düzenle",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                    )

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Satır 1: Cinsiyet ve Okul No yan yana (Aynı Yükseklikte)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cinsiyet Seçimi
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDarkTheme) Color(0x350F172A) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isKiz = editGender.equals("Kız", ignoreCase = true)
                            Surface(
                                onClick = { editGender = "Kız" },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isKiz) Color(0xFFDB2777) else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Kız ♀",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isKiz) Color.White else if (isDarkTheme) Color(0xFFF472B6) else Color(0xFF9D174D)
                                    )
                                }
                            }

                            val isErkek = editGender.equals("Erkek", ignoreCase = true)
                            Surface(
                                onClick = { editGender = "Erkek" },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isErkek) Color(0xFF0284C7) else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Erkek ♂",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isErkek) Color.White else if (isDarkTheme) Color(0xFF38BDF8) else Color(0xFF0369A1)
                                    )
                                }
                            }
                        }
                    }

                    // Okul No
                    OutlinedTextField(
                        value = editNoInput,
                        onValueChange = {
                            if (it.all { c -> c.isDigit() } && it.length <= 6) {
                                editNoInput = it
                                errorMessage = null
                            }
                        },
                        label = { Text("Okul No") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = if (isDarkTheme) Color.White else BirizTextPrimary,
                            unfocusedTextColor = if (isDarkTheme) Color.White else BirizTextPrimary,
                            focusedBorderColor = BirizTurquoise,
                            unfocusedBorderColor = if (isDarkTheme) BirizDarkCardBorder else BirizBorder
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Satır 2: Adı ve Soyadı yan yana
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = uppercaseFirstName,
                        onValueChange = {
                            editFirstNameInput = it
                            errorMessage = null
                        },
                        label = { Text("Adı") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = if (isDarkTheme) Color.White else BirizTextPrimary,
                            unfocusedTextColor = if (isDarkTheme) Color.White else BirizTextPrimary,
                            focusedBorderColor = BirizTurquoise,
                            unfocusedBorderColor = if (isDarkTheme) BirizDarkCardBorder else BirizBorder
                        )
                    )

                    OutlinedTextField(
                        value = uppercaseLastName,
                        onValueChange = {
                            editLastNameInput = it
                            errorMessage = null
                        },
                        label = { Text("Soyadı") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = if (isDarkTheme) Color.White else BirizTextPrimary,
                            unfocusedTextColor = if (isDarkTheme) Color.White else BirizTextPrimary,
                            focusedBorderColor = BirizTurquoise,
                            unfocusedBorderColor = if (isDarkTheme) BirizDarkCardBorder else BirizBorder
                        )
                    )
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = BirizError,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("İptal", color = if (isDarkTheme) Color.White else BirizNavyPrimary)
                    }

                    val saveBtnContentColor = if (isDarkTheme) BirizNavyDark else Color.White
                    Button(
                        onClick = {
                            val no = editNoInput.toIntOrNull()
                            if (no == null || no <= 0) {
                                errorMessage = "Lütfen geçerli bir numara giriniz."
                                return@Button
                            }
                            if (uppercaseFirstName.trim().length < 2) {
                                errorMessage = "Lütfen öğrenci adını giriniz."
                                return@Button
                            }
                            if (uppercaseLastName.trim().length < 2) {
                                errorMessage = "Lütfen öğrenci soyadını giriniz."
                                return@Button
                            }
                            val updatedFullName = "${uppercaseFirstName.trim()} ${uppercaseLastName.trim()}"
                            onConfirm(no, updatedFullName, editGender)
                        },
                        modifier = Modifier.weight(1.4f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                            contentColor = saveBtnContentColor
                        )
                    ) {
                        Text("Güncelle", fontWeight = FontWeight.Bold, color = saveBtnContentColor)
                    }
                }
            }
        }
    }
}

/**
 * FAB ve harici tetikleyiciler için Açılır Öğrenci Ekleme Diyaloğu
 */
@Composable
fun AddStudentDialog(
    gradeLevel: Int,
    section: String,
    isDarkTheme: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (Int, String, String) -> Unit
) {
    var studentNoInput by remember { mutableStateOf("") }
    var firstNameInput by remember { mutableStateOf("") }
    var lastNameInput by remember { mutableStateOf("") }
    var selectedGender by remember { mutableStateOf("Erkek") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val uppercaseFirstName = TurkishTextHelper.toTurkishUpperCase(firstNameInput)
    val uppercaseLastName = TurkishTextHelper.toTurkishUpperCase(lastNameInput)

    Dialog(onDismissRequest = onDismiss) {
        BentoCard(
            isDark = isDarkTheme,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Yeni Öğrenci Ekle",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                        )
                        Text(
                            text = "$gradeLevel/$section Şubesi Sınıf Listesi",
                            fontSize = 12.sp,
                            color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Satır 1: Cinsiyet ve Okul No yan yana (Aynı Yükseklikte)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cinsiyet
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDarkTheme) Color(0x350F172A) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isKiz = selectedGender == "Kız"
                            Surface(
                                onClick = { selectedGender = "Kız" },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isKiz) Color(0xFFDB2777) else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Kız ♀",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isKiz) Color.White else if (isDarkTheme) Color(0xFFF472B6) else Color(0xFF9D174D)
                                    )
                                }
                            }

                            val isErkek = selectedGender == "Erkek"
                            Surface(
                                onClick = { selectedGender = "Erkek" },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isErkek) Color(0xFF0284C7) else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Erkek ♂",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isErkek) Color.White else if (isDarkTheme) Color(0xFF38BDF8) else Color(0xFF0369A1)
                                    )
                                }
                            }
                        }
                    }

                    // Okul No
                    OutlinedTextField(
                        value = studentNoInput,
                        onValueChange = {
                            if (it.all { c -> c.isDigit() } && it.length <= 6) {
                                studentNoInput = it
                                validationError = null
                            }
                        },
                        label = { Text("Okul No") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = if (isDarkTheme) Color.White else BirizTextPrimary,
                            unfocusedTextColor = if (isDarkTheme) Color.White else BirizTextPrimary,
                            focusedBorderColor = BirizTurquoise,
                            unfocusedBorderColor = if (isDarkTheme) BirizDarkCardBorder else BirizBorder
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Satır 2: Adı ve Soyadı yan yana
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = uppercaseFirstName,
                        onValueChange = {
                            firstNameInput = it
                            validationError = null
                        },
                        label = { Text("Adı") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = if (isDarkTheme) Color.White else BirizTextPrimary,
                            unfocusedTextColor = if (isDarkTheme) Color.White else BirizTextPrimary,
                            focusedBorderColor = BirizTurquoise,
                            unfocusedBorderColor = if (isDarkTheme) BirizDarkCardBorder else BirizBorder
                        )
                    )

                    OutlinedTextField(
                        value = uppercaseLastName,
                        onValueChange = {
                            lastNameInput = it
                            validationError = null
                        },
                        label = { Text("Soyadı") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = if (isDarkTheme) Color.White else BirizTextPrimary,
                            unfocusedTextColor = if (isDarkTheme) Color.White else BirizTextPrimary,
                            focusedBorderColor = BirizTurquoise,
                            unfocusedBorderColor = if (isDarkTheme) BirizDarkCardBorder else BirizBorder
                        )
                    )
                }

                if (validationError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = validationError ?: "",
                        color = BirizError,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("İptal", color = if (isDarkTheme) Color.White else BirizNavyPrimary)
                    }

                    val saveBtnContentColor = if (isDarkTheme) BirizNavyDark else Color.White
                    Button(
                        onClick = {
                            val no = studentNoInput.toIntOrNull()
                            if (no == null || no <= 0) {
                                validationError = "Lütfen geçerli bir okul numarası giriniz."
                                return@Button
                            }
                            if (uppercaseFirstName.trim().length < 2) {
                                validationError = "Lütfen ad giriniz."
                                return@Button
                            }
                            if (uppercaseLastName.trim().length < 2) {
                                validationError = "Lütfen soyad giriniz."
                                return@Button
                            }
                            val full = "${uppercaseFirstName.trim()} ${uppercaseLastName.trim()}"
                            onConfirm(no, full, selectedGender)
                        },
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                            contentColor = saveBtnContentColor
                        )
                    ) {
                        Text("Kaydet", fontWeight = FontWeight.Bold, color = saveBtnContentColor)
                    }
                }
            }
        }
    }
}
