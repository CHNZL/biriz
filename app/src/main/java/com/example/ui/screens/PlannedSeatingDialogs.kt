package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ArchivedSeatingPlan
import com.example.data.model.ClassRoom
import com.example.data.model.FixedStudentPlacement
import com.example.data.model.PlannedSeatingRules
import com.example.data.model.SeatingConfig
import com.example.data.model.SeatingPlanState
import com.example.data.model.Student
import com.example.ui.components.BentoCard
import com.example.ui.theme.BirizAmber
import com.example.ui.theme.BirizBorder
import com.example.ui.theme.BirizDarkCardBorder
import com.example.ui.theme.BirizDarkSurfaceGlass
import com.example.ui.theme.BirizDarkTextPrimary
import com.example.ui.theme.BirizDarkTextSecondary
import com.example.ui.theme.BirizError
import com.example.ui.theme.BirizNavyDark
import com.example.ui.theme.BirizNavyPrimary
import com.example.ui.theme.BirizTurquoise
import com.example.util.SeatingPlanHelper
import com.example.util.SeatingPlanPersistenceHelper
import com.example.util.TurkishTextHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Planlı Yerleştirme Sihirbazı (4 Adımlı Pencere):
 * 1. Adım: Referans Plan & Kurallar (Kız/Erkek karışık, farklı grup, farklı sıra, farklı arkadaş)
 * 2. Adım: Sabit Öğrenci Seçimi (Grup, sıra, koltuk kilitleme - Opsiyonel)
 * 3. Adım: Öncelikli Öğrenciler Seçimi (Öğretmen masasına en yakından öne yerleştirme - Opsiyonel)
 * 4. Adım: Özet & Onay Penceresi (Plan adı, kurallar özeti, onay veya düzenleme için geri dönme)
 */
@Composable
fun PlannedSeatingWizardDialog(
    classRoom: ClassRoom,
    students: List<Student>,
    currentState: SeatingPlanState,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onApplyPlannedPlan: (SeatingPlanState) -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(1) }

    // Arşivdeki planlar
    val archivedPlans = remember {
        SeatingPlanPersistenceHelper.getArchivedPlans(context, classRoom.id)
    }

    // 1. ADIM STATE'LERİ: Referans Plan & Kurallar
    var selectedReferencePlan by remember {
        mutableStateOf<SeatingPlanState?>(if (currentState.assignments.isNotEmpty()) currentState else archivedPlans.firstOrNull()?.planState)
    }
    var selectedReferenceName by remember {
        mutableStateOf(
            if (currentState.assignments.isNotEmpty()) "Mevcut Oturma Planı (${currentState.activePlanName})"
            else archivedPlans.firstOrNull()?.name ?: "Referansız (Bağımsız Dağıtım)"
        )
    }
    var ruleGenderMixed by remember { mutableStateOf(true) }
    var ruleDifferentGroup by remember { mutableStateOf(true) }
    var ruleDifferentRow by remember { mutableStateOf(true) }
    var ruleDifferentNeighbor by remember { mutableStateOf(true) }
    val incompatiblePairs = remember { mutableStateListOf<Pair<String, String>>() }
    var selectedIncompatStudent1 by remember { mutableStateOf<Student?>(null) }
    var selectedIncompatStudent2 by remember { mutableStateOf<Student?>(null) }

    // 2. ADIM STATE'LERİ: Sabit Öğrenciler
    val fixedPlacements = remember { mutableStateListOf<FixedStudentPlacement>() }
    var selectedFixedStudent by remember { mutableStateOf<Student?>(null) }
    var selectedGroupIdx by remember { mutableIntStateOf(0) }
    var selectedRowIdx by remember { mutableIntStateOf(0) }
    var selectedSeatIdx by remember { mutableIntStateOf(0) }

    // 3. ADIM STATE'LERİ: Öncelikli Öğrenciler (Sınıf Listesinde 'isPriority / Ön Sıra' olanlar otomatik gelir)
    val priorityStudentIds = remember {
        mutableStateListOf<String>().apply {
            val autoPriorities = (currentState.priorityStudents + students.filter { it.isPriority }.map { it.id }).distinct()
            addAll(autoPriorities)
        }
    }

    // 4. ADIM STATE'LERİ: Özet & İsimlendirme
    var planNameInput by remember {
        mutableStateOf(SeatingPlanPersistenceHelper.generateDefaultPlanName())
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isDarkTheme) Color(0xFF0F1E36) else Color.White,
            border = BorderStroke(1.5.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // --- SİHİRBAZ BAŞLIĞI VE ADIM GÖSTERGESİ ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = BirizTurquoise,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Planlı Yerleştirme Sihirbazı",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDarkTheme) Color.White else BirizNavyPrimary
                            )
                        }
                        Text(
                            text = "Adım $currentStep / 4 • ${
                                when (currentStep) {
                                    1 -> "Referans Plan & Kurallar"
                                    2 -> "Sabit Öğrenciler"
                                    3 -> "Öncelikli Öğrenciler"
                                    else -> "Özet ve Onay"
                                }
                            }",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BirizTurquoise
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = if (isDarkTheme) Color.White else BirizNavyPrimary
                        )
                    }
                }

                // Adım Çubuğu (Progress Indicator)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (step in 1..4) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .background(
                                    color = if (step <= currentStep) BirizTurquoise else if (isDarkTheme) Color(0x30FFFFFF) else Color(0xFFE2E8F0),
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // --- ADIM İÇERİKLERİ ---
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (currentStep) {
                        // ==============================================================
                        // ADIM 1: REFERANS PLAN VE KURALLAR
                        // ==============================================================
                        1 -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "1. Referans Alınacak Planı Seçin:",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkTheme) Color.White else BirizNavyPrimary
                                )

                                var showRefMenu by remember { mutableStateOf(false) }
                                Box {
                                    Surface(
                                        onClick = { showRefMenu = true },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isDarkTheme) Color(0x350A162B) else Color(0xFFF8FAFC),
                                        border = BorderStroke(1.dp, BirizTurquoise),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(46.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Bookmark,
                                                    contentDescription = null,
                                                    tint = BirizTurquoise,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = selectedReferenceName,
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDarkTheme) Color.White else BirizNavyPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Text(
                                                text = "Değiştir ▾",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BirizTurquoise
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = showRefMenu,
                                        onDismissRequest = { showRefMenu = false }
                                    ) {
                                        if (currentState.assignments.isNotEmpty()) {
                                            DropdownMenuItem(
                                                text = { Text("📍 Mevcut Aktif Plan (${currentState.activePlanName})") },
                                                onClick = {
                                                    selectedReferencePlan = currentState
                                                    selectedReferenceName = "Mevcut Aktif Plan (${currentState.activePlanName})"
                                                    showRefMenu = false
                                                }
                                            )
                                        }

                                        archivedPlans.forEach { archived ->
                                            DropdownMenuItem(
                                                text = { Text("📁 ${archived.name}") },
                                                onClick = {
                                                    selectedReferencePlan = archived.planState
                                                    selectedReferenceName = archived.name
                                                    showRefMenu = false
                                                }
                                            )
                                        }

                                        DropdownMenuItem(
                                            text = { Text("⚪ Referansız / Bağımsız Dağıtım") },
                                            onClick = {
                                                selectedReferencePlan = null
                                                selectedReferenceName = "Referansız (Bağımsız Dağıtım)"
                                                showRefMenu = false
                                            }
                                        )
                                    }
                                }

                                HorizontalDivider(color = if (isDarkTheme) Color(0x30FFFFFF) else Color(0xFFE2E8F0))

                                Text(
                                    text = "2. Yerleştirme Kuralları (Aktif / Pasif):",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkTheme) Color.White else BirizNavyPrimary
                                )

                                // Kural 1: Kız / Erkek Karışık
                                WizardRuleItem(
                                    title = "👫 Kız / Erkek Karışık Otursun",
                                    desc = "Sıralarda yan yana oturan öğrencilerin kız-erkek dengeli olması hedeflenir.",
                                    checked = ruleGenderMixed,
                                    isDark = isDarkTheme,
                                    onCheckedChange = { ruleGenderMixed = it }
                                )

                                // Kural 2: Farklı Gruba Yerleştirilsin
                                WizardRuleItem(
                                    title = "🔀 Her Öğrenci Farklı Gruba Yerleştirilsin",
                                    desc = "Öğrenciler önceki planlarındaki gruptan/öbekten farklı bir gruba kaydırılır.",
                                    checked = ruleDifferentGroup,
                                    isDark = isDarkTheme,
                                    onCheckedChange = { ruleDifferentGroup = it }
                                )

                                // Kural 3: Farklı Sıraya Yerleştirilsin
                                WizardRuleItem(
                                    title = "⬆️⬇️ Her Öğrenci Farklı Sıraya Yerleştirilsin",
                                    desc = "Öğrenciler önceki oturdukları sıradan (ön/orta/arka) farklı bir satıra geçer.",
                                    checked = ruleDifferentRow,
                                    isDark = isDarkTheme,
                                    onCheckedChange = { ruleDifferentRow = it }
                                )

                                // Kural 4: Farklı Kişi ile Otursun
                                WizardRuleItem(
                                    title = "🤝 Her Öğrenci Farklı Kişi ile Otursun",
                                    desc = "Öğrencilerin önceki sıra arkadaşlarıyla tekrar yan yana eşleşmesi engellenir.",
                                    checked = ruleDifferentNeighbor,
                                    isDark = isDarkTheme,
                                    onCheckedChange = { ruleDifferentNeighbor = it }
                                )

                                HorizontalDivider(color = if (isDarkTheme) Color(0x30FFFFFF) else Color(0xFFE2E8F0))

                                // Kural 5: Yan Yana Oturmaması Gerekenler (Uyumsuz İkililer)
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "🚫 Yan Yana Oturmaması Gerekenler (Uyumsuz İkililer):",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkTheme) Color.White else BirizNavyPrimary
                                    )
                                    Text(
                                        text = "Birlikte veya aynı sırada oturmaması gereken öğrencileri ikili olarak eşleştirebilirsiniz.",
                                        fontSize = 11.sp,
                                        color = if (isDarkTheme) Color.LightGray else Color(0xFF64748B)
                                    )

                                    var showIncompatMenu1 by remember { mutableStateOf(false) }
                                    var showIncompatMenu2 by remember { mutableStateOf(false) }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            OutlinedButton(
                                                onClick = { showIncompatMenu1 = true },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                                modifier = Modifier.fillMaxWidth().height(36.dp)
                                            ) {
                                                Text(
                                                    text = selectedIncompatStudent1?.let { "#${it.studentNumber} ${TurkishTextHelper.toTurkishUpperCase(it.fullName)}" } ?: "1. Öğrenci",
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = showIncompatMenu1,
                                                onDismissRequest = { showIncompatMenu1 = false }
                                            ) {
                                                students.forEach { s ->
                                                    DropdownMenuItem(
                                                        text = { Text("#${s.studentNumber} • ${TurkishTextHelper.toTurkishUpperCase(s.fullName)}") },
                                                        onClick = {
                                                            selectedIncompatStudent1 = s
                                                            showIncompatMenu1 = false
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        Text("❌", fontSize = 12.sp)

                                        Box(modifier = Modifier.weight(1f)) {
                                            OutlinedButton(
                                                onClick = { showIncompatMenu2 = true },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                                modifier = Modifier.fillMaxWidth().height(36.dp)
                                            ) {
                                                Text(
                                                    text = selectedIncompatStudent2?.let { "#${it.studentNumber} ${TurkishTextHelper.toTurkishUpperCase(it.fullName)}" } ?: "2. Öğrenci",
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = showIncompatMenu2,
                                                onDismissRequest = { showIncompatMenu2 = false }
                                            ) {
                                                students.forEach { s ->
                                                    DropdownMenuItem(
                                                        text = { Text("#${s.studentNumber} • ${TurkishTextHelper.toTurkishUpperCase(s.fullName)}") },
                                                        onClick = {
                                                            selectedIncompatStudent2 = s
                                                            showIncompatMenu2 = false
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                val s1 = selectedIncompatStudent1
                                                val s2 = selectedIncompatStudent2
                                                if (s1 != null && s2 != null && s1.id != s2.id) {
                                                    val exists = incompatiblePairs.any { (a, b) -> (a == s1.id && b == s2.id) || (a == s2.id && b == s1.id) }
                                                    if (!exists) {
                                                        incompatiblePairs.add(Pair(s1.id, s2.id))
                                                        selectedIncompatStudent1 = null
                                                        selectedIncompatStudent2 = null
                                                    }
                                                }
                                            },
                                            enabled = selectedIncompatStudent1 != null && selectedIncompatStudent2 != null && selectedIncompatStudent1?.id != selectedIncompatStudent2?.id,
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.height(36.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                                        ) {
                                            Text("+ Ekle", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    if (incompatiblePairs.isNotEmpty()) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            incompatiblePairs.forEach { (id1, id2) ->
                                                val st1 = students.firstOrNull { it.id == id1 }
                                                val st2 = students.firstOrNull { it.id == id2 }
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isDarkTheme) Color(0x30EF4444) else Color(0xFFFEE2E2),
                                                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "🚫 ${st1?.fullName ?: id1}  ⚡  ${st2?.fullName ?: id2}",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = if (isDarkTheme) Color.White else Color(0xFF991B1B)
                                                        )
                                                        IconButton(
                                                            onClick = { incompatiblePairs.remove(Pair(id1, id2)) },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Close,
                                                                contentDescription = "Sil",
                                                                modifier = Modifier.size(14.dp),
                                                                tint = Color(0xFFEF4444)
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

                        // ==============================================================
                        // ADIM 2: SABİT ÖĞRENCİ SEÇİMİ
                        // ==============================================================
                        2 -> {
                            val availableForFix = students.filter { s -> fixedPlacements.none { it.studentId == s.id } }
                            val config = currentState.config

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isDarkTheme) Color(0x3028B8C2) else Color(0xFFE0F7F6),
                                    border = BorderStroke(1.dp, BirizTurquoise)
                                ) {
                                    Text(
                                        text = "ℹ️ Yeri değişmemesi gereken öğrencileri seçip sabitleyebilirsiniz. Bu öğrenciler yerinde kalır, diğerleri kurallara göre dağıtılır. Sabit öğrenci seçmek zorunlu değildir.",
                                        fontSize = 11.sp,
                                        color = if (isDarkTheme) Color.White else Color(0xFF0F766E),
                                        modifier = Modifier.padding(10.dp),
                                        lineHeight = 15.sp
                                    )
                                }

                                Text(
                                    text = "Sabitlenecek Öğrenciyi ve Koltuğu Seçin:",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkTheme) Color.White else BirizNavyPrimary
                                )

                                // Öğrenci Seçici Açılır Menü
                                var showStudentMenu by remember { mutableStateOf(false) }
                                Box {
                                    OutlinedButton(
                                        onClick = { showStudentMenu = true },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = selectedFixedStudent?.let { "#${it.studentNumber} - ${TurkishTextHelper.toTurkishUpperCase(it.fullName)}" }
                                                ?: "Öğrenci Seçin (${availableForFix.size} serbest)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = showStudentMenu,
                                        onDismissRequest = { showStudentMenu = false }
                                    ) {
                                        availableForFix.forEach { student ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text("#${student.studentNumber} • ${TurkishTextHelper.toTurkishUpperCase(student.fullName)} (${student.gender})")
                                                },
                                                onClick = {
                                                    selectedFixedStudent = student
                                                    showStudentMenu = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Koltuk Koordinat Seçicileri (Grup, Sıra, Koltuk)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // 1. Grup
                                    var showGroupMenu by remember { mutableStateOf(false) }
                                    Box(modifier = Modifier.weight(1f)) {
                                        OutlinedButton(
                                            onClick = { showGroupMenu = true },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("${selectedGroupIdx + 1}. Grup", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        DropdownMenu(expanded = showGroupMenu, onDismissRequest = { showGroupMenu = false }) {
                                            for (g in 0 until config.groupCount) {
                                                DropdownMenuItem(
                                                    text = { Text("${g + 1}. Grup") },
                                                    onClick = {
                                                        selectedGroupIdx = g
                                                        showGroupMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // 2. Sıra
                                    val rowCountForGroup = config.rowsPerGroup.getOrElse(selectedGroupIdx) { 5 }
                                    var showRowMenu by remember { mutableStateOf(false) }
                                    Box(modifier = Modifier.weight(1f)) {
                                        OutlinedButton(
                                            onClick = { showRowMenu = true },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("${selectedRowIdx + 1}. Sıra", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        DropdownMenu(expanded = showRowMenu, onDismissRequest = { showRowMenu = false }) {
                                            for (r in 0 until rowCountForGroup) {
                                                DropdownMenuItem(
                                                    text = { Text("${r + 1}. Sıra") },
                                                    onClick = {
                                                        selectedRowIdx = r
                                                        showRowMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // 3. Koltuk
                                    var showSeatMenu by remember { mutableStateOf(false) }
                                    Box(modifier = Modifier.weight(1f)) {
                                        OutlinedButton(
                                            onClick = { showSeatMenu = true },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("${selectedSeatIdx + 1}. Koltuk", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        DropdownMenu(expanded = showSeatMenu, onDismissRequest = { showSeatMenu = false }) {
                                            for (s in 0 until config.peoplePerRow) {
                                                DropdownMenuItem(
                                                    text = { Text("${s + 1}. Koltuk") },
                                                    onClick = {
                                                        selectedSeatIdx = s
                                                        showSeatMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // Ekle Butonu
                                Button(
                                    onClick = {
                                        val student = selectedFixedStudent
                                        if (student == null) {
                                            Toast.makeText(context, "Lütfen önce bir öğrenci seçin.", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        val targetSeatId = "g$selectedGroupIdx-r$selectedRowIdx-s$selectedSeatIdx"
                                        // Aynı koltukta başka biri sabitlenmiş mi?
                                        fixedPlacements.removeAll { it.seatId == targetSeatId || it.studentId == student.id }
                                        fixedPlacements.add(
                                            FixedStudentPlacement(
                                                studentId = student.id,
                                                groupIndex = selectedGroupIdx,
                                                rowIndex = selectedRowIdx,
                                                seatIndex = selectedSeatIdx
                                            )
                                        )
                                        selectedFixedStudent = null
                                        Toast.makeText(context, "${student.fullName} hedeflenen koltuğa sabitlendi.", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BirizTurquoise),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Bu Öğrenciyi Sabitle", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                // Eklenmiş Sabit Öğrenciler Listesi
                                if (fixedPlacements.isNotEmpty()) {
                                    Text(
                                        text = "Sabitlenen Öğrenciler (${fixedPlacements.size}):",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BirizTurquoise,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )

                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        fixedPlacements.forEach { placement ->
                                            val st = students.firstOrNull { it.id == placement.studentId }
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isDarkTheme) Color(0x350A162B) else Color(0xFFF1F5F9),
                                                border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Default.Lock,
                                                            contentDescription = null,
                                                            tint = BirizTurquoise,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = "${st?.fullName ?: "Öğrenci"} ➔ ${placement.groupIndex + 1}. Grup, ${placement.rowIndex + 1}. Sıra, ${placement.seatIndex + 1}. Koltuk",
                                                            fontSize = 11.5.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    }

                                                    IconButton(
                                                        onClick = { fixedPlacements.remove(placement) },
                                                        modifier = Modifier.size(26.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = "Sil",
                                                            tint = BirizError,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ==============================================================
                        // ADIM 3: ÖNCELİKLİ ÖĞRENCİ SEÇİMİ
                        // ==============================================================
                        3 -> {
                            var searchQuery by remember { mutableStateOf("") }
                            val filteredStudents = remember(searchQuery, students) {
                                if (searchQuery.isBlank()) students
                                else students.filter {
                                    it.fullName.contains(searchQuery, ignoreCase = true) || it.studentNumber.contains(searchQuery)
                                }
                            }

                            Column(modifier = Modifier.fillMaxSize()) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isDarkTheme) Color(0x30F59E0B) else Color(0xFFFEF3C7),
                                    border = BorderStroke(1.dp, Color(0xFFF59E0B))
                                ) {
                                    Text(
                                        text = "👓 Öncelikli öğrenciler (gözlük, boy, dikkat vb.) öğretmen masasına en yakın 1. sıralardan başlayarak öne yerleştirilir. Öğrencinin üzerine dokunarak seçebilirsiniz.",
                                        fontSize = 11.sp,
                                        color = if (isDarkTheme) Color.White else Color(0xFF78350F),
                                        modifier = Modifier.padding(10.dp),
                                        lineHeight = 15.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Arama Çubuğu
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Öğrenci adı veya no ara...", fontSize = 12.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BirizTurquoise,
                                        unfocusedBorderColor = if (isDarkTheme) BirizDarkCardBorder else BirizBorder
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Seçilen Öncelikli: ${priorityStudentIds.size}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BirizAmber
                                    )
                                    if (priorityStudentIds.isNotEmpty()) {
                                        TextButton(onClick = { priorityStudentIds.clear() }) {
                                            Text("Temizle", fontSize = 11.sp, color = BirizError)
                                        }
                                    }
                                }

                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(filteredStudents, key = { it.id }) { student ->
                                        val isPriority = priorityStudentIds.contains(student.id)
                                        val isGirl = student.gender.equals("Kız", ignoreCase = true)

                                            Surface(
                                                onClick = {
                                                    if (isPriority) priorityStudentIds.remove(student.id)
                                                    else priorityStudentIds.add(student.id)
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                color = when {
                                                    isPriority -> if (isDarkTheme) Color(0x35F59E0B) else Color(0xFFFEF3C7)
                                                    isGirl -> if (isDarkTheme) Color(0x30F472B6) else Color.White
                                                    else -> if (isDarkTheme) Color(0x3038BDF8) else Color.White
                                                },
                                                border = BorderStroke(
                                                    width = if (isPriority) 1.5.dp else 1.dp,
                                                    color = when {
                                                        isPriority -> BirizAmber
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
                                                            fontSize = 12.sp,
                                                            fontWeight = if (isPriority) FontWeight.Bold else FontWeight.SemiBold,
                                                            color = if (isDarkTheme) Color.White else Color(0xFF1E293B),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }

                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = if (isGirl) "♀" else "♂",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isGirl) Color(0xFFEC4899) else Color(0xFF0EA5E9)
                                                        )

                                                        Checkbox(
                                                            checked = isPriority,
                                                            onCheckedChange = { checked ->
                                                                if (checked) priorityStudentIds.add(student.id)
                                                                else priorityStudentIds.remove(student.id)
                                                            },
                                                            colors = CheckboxDefaults.colors(
                                                                checkedColor = BirizAmber
                                                            ),
                                                            modifier = Modifier.size(22.dp)
                                                        )
                                                    }
                                                }
                                            }
                                    }
                                }
                            }
                        }

                        // ==============================================================
                        // ADIM 4: ÖZET VE ONAY
                        // ==============================================================
                        4 -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Plan Özeti ve İsimlendirme:",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkTheme) Color.White else BirizNavyPrimary
                                )

                                // Plan İsmi Girişi
                                OutlinedTextField(
                                    value = planNameInput,
                                    onValueChange = { planNameInput = it },
                                    label = { Text("Plan Adı") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BirizTurquoise,
                                        unfocusedBorderColor = if (isDarkTheme) BirizDarkCardBorder else BirizBorder
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Özet Kartı
                                BentoCard(isDark = isDarkTheme, modifier = Modifier.fillMaxWidth()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        SummaryRow(
                                            title = "Referans Plan:",
                                            value = selectedReferenceName
                                        )
                                        SummaryRow(
                                            title = "Kız/Erkek Karışık:",
                                            value = if (ruleGenderMixed) "✅ Aktif" else "❌ Pasif"
                                        )
                                        SummaryRow(
                                            title = "Farklı Grup Kuralı:",
                                            value = if (ruleDifferentGroup) "✅ Aktif" else "❌ Pasif"
                                        )
                                        SummaryRow(
                                            title = "Farklı Sıra Kuralı:",
                                            value = if (ruleDifferentRow) "✅ Aktif" else "❌ Pasif"
                                        )
                                        SummaryRow(
                                            title = "Farklı Arkadaş Kuralı:",
                                            value = if (ruleDifferentNeighbor) "✅ Aktif" else "❌ Pasif"
                                        )
                                        SummaryRow(
                                            title = "Sabitlenen Öğrenciler:",
                                            value = "${fixedPlacements.size} Öğrenci"
                                        )
                                        SummaryRow(
                                            title = "Öncelikli Öğrenciler:",
                                            value = "${priorityStudentIds.size} Öğrenci (Öğretmen Masasına En Yakın)"
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isDarkTheme) Color(0x3028B8C2) else Color(0xFFE0F7F6),
                                    border = BorderStroke(1.dp, BirizTurquoise)
                                ) {
                                    Text(
                                        text = "🚀 'Planı Oluştur ve Uygula' butonuna tıkladığınızda oturma planı yukarıdaki kurallara göre dağıtılacak, sınıfa uygulanacak ve arşivinize otomatik kaydedilecektir.",
                                        fontSize = 11.5.sp,
                                        color = if (isDarkTheme) Color.White else Color(0xFF0F766E),
                                        modifier = Modifier.padding(10.dp),
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // --- ALT GEZİNME VE İŞLEM BUTONLARI ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = { currentStep-- },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("‹ Geri", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        TextButton(onClick = onDismiss) {
                            Text("İptal", color = BirizError)
                        }
                    }

                    if (currentStep < 4) {
                        Button(
                            onClick = { currentStep++ },
                            colors = ButtonDefaults.buttonColors(containerColor = BirizNavyPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("İleri ›", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Button(
                            onClick = {
                                val rules = PlannedSeatingRules(
                                    genderMixed = ruleGenderMixed,
                                    differentGroup = ruleDifferentGroup,
                                    differentRow = ruleDifferentRow,
                                    differentNeighbor = ruleDifferentNeighbor,
                                    incompatiblePairs = incompatiblePairs.toList()
                                )
                                val generatedPlan = SeatingPlanHelper.generatePlannedSeating(
                                    students = students,
                                    config = currentState.config,
                                    referencePlanState = selectedReferencePlan,
                                    rules = rules,
                                    fixedPlacements = fixedPlacements.toList(),
                                    priorityStudentIds = priorityStudentIds.toList(),
                                    planName = planNameInput.trim().ifBlank { SeatingPlanPersistenceHelper.generateDefaultPlanName() }
                                )
                                onApplyPlannedPlan(generatedPlan)
                                Toast.makeText(context, "Yeni oturma planı başarıyla oluşturuldu ve arşivlendi.", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BirizTurquoise),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Planı Oluştur ve Uygula", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WizardRuleItem(
    title: String,
    desc: String,
    checked: Boolean,
    isDark: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        onClick = { onCheckedChange(!checked) },
        shape = RoundedCornerShape(10.dp),
        color = if (checked) {
            if (isDark) Color(0x3028B8C2) else Color(0xFFF0FDF4)
        } else {
            if (isDark) Color(0x200A162B) else Color(0xFFF8FAFC)
        },
        border = BorderStroke(
            1.dp,
            if (checked) BirizTurquoise else if (isDark) BirizDarkCardBorder else BirizBorder
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else BirizNavyPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = desc,
                    fontSize = 10.5.sp,
                    color = if (isDark) BirizDarkTextSecondary else Color.Gray,
                    lineHeight = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = BirizTurquoise
                )
            )
        }
    }
}

@Composable
private fun SummaryRow(title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, fontSize = 11.5.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * Oturma Planı Arşiv Diyaloğu (Tüm Planları Görüntüleme, Tekli / Çoklu / Toplu Silme, Onay ile Uygulama)
 */
@Composable
fun SeatingPlanArchiveDialog(
    classRoom: ClassRoom,
    students: List<Student>,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onApplyPlan: (ArchivedSeatingPlan) -> Unit
) {
    val context = LocalContext.current
    var plansList by remember {
        mutableStateOf(SeatingPlanPersistenceHelper.getArchivedPlans(context, classRoom.id))
    }
    val selectedIds = remember { mutableStateListOf<String>() }

    // Silme ve Uygulama Onay Dialog State'leri
    var planToDeleteSingle by remember { mutableStateOf<ArchivedSeatingPlan?>(null) }
    var showDeleteMultipleConfirm by remember { mutableStateOf(false) }
    var showClearAllConfirm by remember { mutableStateOf(false) }
    var planToConfirmApply by remember { mutableStateOf<ArchivedSeatingPlan?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isDarkTheme) Color(0xFF0F1E36) else Color.White,
            border = BorderStroke(1.5.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Başlık
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = null,
                            tint = BirizTurquoise,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Oturma Planı Arşivi",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDarkTheme) Color.White else BirizNavyPrimary
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Kapat")
                    }
                }

                Text(
                    text = "Daha önce oluşturulmuş planları inceleyebilir, sınıfa uygulayabilir veya silebilirsiniz.",
                    fontSize = 11.sp,
                    color = if (isDarkTheme) BirizDarkTextSecondary else Color.Gray,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                )

                // Toplu İşlem Butonları (Tümünü Seç, Seçilenleri Sil, Tümünü Sil)
                if (plansList.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tümünü Seç / Seçimi Kaldır
                        val isAllSelected = selectedIds.size == plansList.size
                        TextButton(
                            onClick = {
                                if (isAllSelected) selectedIds.clear()
                                else {
                                    selectedIds.clear()
                                    selectedIds.addAll(plansList.map { it.id })
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Text(
                                text = if (isAllSelected) "Seçimi Kaldır" else "Tümünü Seç (${plansList.size})",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = BirizTurquoise
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Seçilenleri Sil (Birden çok silme)
                            if (selectedIds.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = { showDeleteMultipleConfirm = true },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BirizError),
                                    border = BorderStroke(1.dp, BirizError),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sil (${selectedIds.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Tümünü Temizle
                            OutlinedButton(
                                onClick = { showClearAllConfirm = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = BirizError),
                                border = BorderStroke(1.dp, BirizError.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Tümünü Sil", fontSize = 11.sp)
                            }
                        }
                    }
                }

                HorizontalDivider(color = if (isDarkTheme) Color(0x30FFFFFF) else Color(0xFFE2E8F0))

                Spacer(modifier = Modifier.height(8.dp))

                // Planlar Listesi
                if (plansList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Archive,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Henüz arşivlenmiş oturma planı bulunmuyor.",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Gray
                            )
                            Text(
                                text = "Akıllı Dağıt veya Planlı Yerleştirme ile yeni planlar oluşturabilirsiniz.",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(plansList, key = { it.id }) { plan ->
                            val isChecked = selectedIds.contains(plan.id)
                            val placedCount = plan.planState.assignments.values.filter { it.isNotBlank() }.distinct().size
                            val totalSeats = plan.planState.config.totalSeats

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDarkTheme) Color(0x350A162B) else Color(0xFFF8FAFC),
                                border = BorderStroke(
                                    width = if (isChecked) 1.5.dp else 1.dp,
                                    color = if (isChecked) BirizTurquoise else if (isDarkTheme) BirizDarkCardBorder else BirizBorder
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { checked ->
                                                if (checked) selectedIds.add(plan.id)
                                                else selectedIds.remove(plan.id)
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = BirizTurquoise),
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = plan.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDarkTheme) Color.White else BirizNavyPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr")).format(Date(plan.createdAtMillis))
                                            Text(
                                                text = "$dateStr • $placedCount / $totalSeats Yerleşik • ${plan.planState.config.groupCount} Grup",
                                                fontSize = 11.sp,
                                                color = if (isDarkTheme) BirizDarkTextSecondary else Color.Gray
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Uygula Butonu
                                        Button(
                                            onClick = { planToConfirmApply = plan },
                                            colors = ButtonDefaults.buttonColors(containerColor = BirizNavyPrimary),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Text("Uygula", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                        }

                                        // Tekli Sil Butonu
                                        IconButton(
                                            onClick = { planToDeleteSingle = plan },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Sil",
                                                tint = BirizError,
                                                modifier = Modifier.size(18.dp)
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
    }

    // --- ONAY PENCERELERİ ---

    // 1. Planı Uygulama Onayı ("Seçilen plan ([Plan Adı]) uygulansın mı?")
    planToConfirmApply?.let { plan ->
        AlertDialog(
            onDismissRequest = { planToConfirmApply = null },
            title = { Text("Planı Uygula", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Seçilen plan ('${plan.name}') sınıfa uygulansın mı? Mevcut oturma düzeni bu plana göre güncellenecektir.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onApplyPlan(plan)
                        planToConfirmApply = null
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BirizTurquoise)
                ) {
                    Text("Evet, Uygula", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { planToConfirmApply = null }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    // 2. Tek Plan Silme Onayı
    planToDeleteSingle?.let { plan ->
        AlertDialog(
            onDismissRequest = { planToDeleteSingle = null },
            title = { Text("Planı Sil", fontWeight = FontWeight.Bold) },
            text = { Text("'${plan.name}' arşivden kalıcı olarak silinsin mi?", fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        SeatingPlanPersistenceHelper.deleteArchivedPlan(context, classRoom.id, plan.id)
                        plansList = SeatingPlanPersistenceHelper.getArchivedPlans(context, classRoom.id)
                        selectedIds.remove(plan.id)
                        planToDeleteSingle = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BirizError)
                ) {
                    Text("Sil", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { planToDeleteSingle = null }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    // 3. Çoklu Plan Silme Onayı
    if (showDeleteMultipleConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteMultipleConfirm = false },
            title = { Text("Seçilen Planları Sil", fontWeight = FontWeight.Bold) },
            text = { Text("Seçilen ${selectedIds.size} adet oturma planı arşivden silinsin mi?", fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        SeatingPlanPersistenceHelper.deleteMultipleArchivedPlans(context, classRoom.id, selectedIds.toSet())
                        plansList = SeatingPlanPersistenceHelper.getArchivedPlans(context, classRoom.id)
                        selectedIds.clear()
                        showDeleteMultipleConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BirizError)
                ) {
                    Text("Evet, Sil", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteMultipleConfirm = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    // 4. Tüm Arşivi Temizleme Onayı
    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            title = { Text("Tüm Arşivi Temizle", fontWeight = FontWeight.Bold, color = BirizError) },
            text = { Text("Arşivdeki tüm kayıtlı planlar kalıcı olarak silinecektir. Emin misiniz?", fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        SeatingPlanPersistenceHelper.clearAllArchivedPlans(context, classRoom.id)
                        plansList = emptyList()
                        selectedIds.clear()
                        showClearAllConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BirizError)
                ) {
                    Text("Tümünü Temizle", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }
}
