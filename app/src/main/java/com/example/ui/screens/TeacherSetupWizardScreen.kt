package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BranchStatus
import com.example.data.model.City
import com.example.data.model.ClassRoom
import com.example.data.model.School
import com.example.ui.components.BentoCard
import com.example.ui.components.ConflictAlertCard
import com.example.ui.components.SchoolAutocompleteField
import com.example.ui.components.SearchableSelectionDialog
import com.example.ui.components.ThemeToggleIconButton
import com.example.ui.theme.BirizAmber
import com.example.ui.theme.BirizBorder
import com.example.ui.theme.BirizDarkBgBottom
import com.example.ui.theme.BirizDarkBgTop
import com.example.ui.theme.BirizDarkCardBorder
import com.example.ui.theme.BirizDarkTextPrimary
import com.example.ui.theme.BirizDarkTextSecondary
import com.example.ui.theme.BirizError
import com.example.ui.theme.BirizNavyDark
import com.example.ui.theme.BirizNavyLight
import com.example.ui.theme.BirizNavyPrimary
import com.example.ui.theme.BirizSuccess
import com.example.ui.theme.BirizTextPrimary
import com.example.ui.theme.BirizTextSecondary
import com.example.ui.theme.BirizTurquoise
import com.example.ui.theme.BirizTurquoiseSoft

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TeacherSetupWizardScreen(
    cities: List<City>,
    selectedCity: City?,
    selectedDistrict: String?,
    rawSchoolInput: String,
    selectedSchool: School?,
    matchingSchools: List<School>,
    selectedGradeLevel: Int?,
    selectedSection: String?,
    branchStatuses: List<BranchStatus>,
    currentUser: com.example.ui.viewmodel.AuthenticatedUser,
    conflictError: String?,
    occupiedClassRoom: ClassRoom?,
    onCitySelected: (City) -> Unit,
    onDistrictSelected: (String) -> Unit,
    onSchoolInputChanged: (String) -> Unit,
    onSchoolSelectedFromList: (School) -> Unit,
    onAddNewSchool: (String) -> Unit,
    onGradeLevelSelected: (Int) -> Unit,
    onSectionSelected: (String) -> Unit,
    onSubmit: () -> Unit,
    onBackToRoles: () -> Unit,
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {}
) {
    var showCityDialog by remember { mutableStateOf(false) }
    var showDistrictDialog by remember { mutableStateOf(false) }
    var gradeDropdownExpanded by remember { mutableStateOf(false) }

    val hasConflict = conflictError != null

    // 81 İl Modalı
    if (showCityDialog) {
        SearchableSelectionDialog(
            title = "İl Seçiniz (81 İl)",
            items = cities.map { it.name },
            selectedItem = selectedCity?.name,
            onItemSelected = { cityName ->
                val found = cities.firstOrNull { it.name.equals(cityName, ignoreCase = true) }
                if (found != null) {
                    onCitySelected(found)
                }
            },
            onDismissRequest = { showCityDialog = false },
            isDarkTheme = isDarkTheme
        )
    }

    // Seçilen İlin İlçeleri Modalı
    if (showDistrictDialog && selectedCity != null) {
        val sortedDistricts = remember(selectedCity) {
            val collator = java.text.Collator.getInstance(java.util.Locale("tr", "TR"))
            selectedCity.districts.sortedWith(collator)
        }
        SearchableSelectionDialog(
            title = "${selectedCity.name} İlçeleri",
            items = sortedDistricts,
            selectedItem = selectedDistrict,
            onItemSelected = { distName ->
                onDistrictSelected(distName)
            },
            onDismissRequest = { showDistrictDialog = false },
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
        // Üst Bar & Navigasyon (Sabit, dokunma engellenemez)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = if (isDarkTheme) Color(0xD00D1B2A) else BirizNavyPrimary,
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBackToRoles,
                        modifier = Modifier.testTag("back_to_roles_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Sınıf Kurulumu",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Hiyerarşik Sınıf ve Şube Tanımlama",
                            fontSize = 11.sp,
                            color = BirizTurquoise
                        )
                    }
                }

                ThemeToggleIconButton(
                    isDark = isDarkTheme,
                    onToggle = onToggleTheme
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Adım Göstergesi ve İlerleme Çubuğu (Görseldeki Gibi "Adım 1/2")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Adım 1/2",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) BirizTurquoise else BirizAmber
                )
                Text(
                    text = "Temel Bilgiler",
                    fontSize = 12.sp,
                    color = textColorSecondary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { 0.65f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = BirizTurquoise,
                trackColor = if (isDarkTheme) Color(0x3564748B) else Color(0xFFE2E8F0)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ADIM 1 & 2: İL VE İLÇE SEÇİMİ (Görseldeki Gibi Yan Yana 2 Bento Kartı)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // İl Bento Kartı
                BentoCard(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("city_select_box"),
                    isDark = isDarkTheme,
                    shape = RoundedCornerShape(18.dp),
                    onClick = { showCityDialog = true }
                ) {
                    Text(
                        text = "İl:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColorSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedCity?.name ?: "Seçiniz",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedCity != null) textColorPrimary else textColorSecondary
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                        )
                    }
                }

                // İlçe Bento Kartı
                val isDistrictEnabled = selectedCity != null
                BentoCard(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("district_select_box"),
                    isDark = isDarkTheme,
                    shape = RoundedCornerShape(18.dp),
                    onClick = if (isDistrictEnabled) { { showDistrictDialog = true } } else null
                ) {
                    Text(
                        text = "İlçe:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColorSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedDistrict ?: if (isDistrictEnabled) "Seçiniz" else "-",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedDistrict != null) textColorPrimary else textColorSecondary
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = if (isDistrictEnabled) (if (isDarkTheme) BirizTurquoise else BirizNavyPrimary) else textColorSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ADIM 3: OKUL ADI (Bento Kartı içinde Autocomplete & Otomatik Türkçe Büyük Harf)
            BentoCard(
                modifier = Modifier.fillMaxWidth(),
                isDark = isDarkTheme,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = "Okul Adı",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColorSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                SchoolAutocompleteField(
                    cityName = selectedCity?.name,
                    districtName = selectedDistrict,
                    schoolInput = rawSchoolInput,
                    selectedSchool = selectedSchool,
                    suggestions = matchingSchools,
                    onInputChanged = onSchoolInputChanged,
                    onSchoolSelected = onSchoolSelectedFromList,
                    onAddNewSchool = onAddNewSchool,
                    isDark = isDarkTheme
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ADIM 4: SINIF SEVİYESİ (4 Adet Doğrudan Seçilebilir Kutu - Açılır Liste Yerine)
            Text(
                text = "Sınıf Seviyesi",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = textColorPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("grade_level_select_box"),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(1, 2, 3, 4).forEach { grade ->
                    val isSelected = selectedGradeLevel == grade
                    Surface(
                        onClick = { onGradeLevelSelected(grade) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) {
                            if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                        } else {
                            if (isDarkTheme) Color(0x351E293B) else Color(0xFFF8FAFC)
                        },
                        border = BorderStroke(
                            width = 1.5.dp,
                            color = if (isSelected) {
                                BirizTurquoise
                            } else {
                                if (isDarkTheme) BirizDarkCardBorder else BirizBorder
                            }
                        ),
                        shadowElevation = if (isSelected) 4.dp else 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("grade_level_pill_$grade")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "$grade. Sınıf",
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (isSelected) {
                                    if (isDarkTheme) BirizNavyDark else Color.White
                                } else {
                                    if (isDarkTheme) BirizDarkTextSecondary else BirizTextPrimary
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ADIM 5: ŞUBE SEÇİM MATRİSİ (Görsel 2'deki gibi Pill / Circular Grid Şeklinde)
            Text(
                text = "Şube",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = textColorPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Yatay Şube Hapları (Pills)
            val currentGrade = selectedGradeLevel ?: 4
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                branchStatuses.take(12).forEach { branch ->
                    val isSelected = selectedSection == branch.section
                    val isOccupied = branch.isOccupied
                    val label = "${currentGrade}/${branch.section}"

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(22.dp))
                            .clickable { onSectionSelected(branch.section) }
                            .testTag("branch_pill_${branch.section}"),
                        shape = RoundedCornerShape(22.dp),
                        color = when {
                            isOccupied -> if (isDarkTheme) Color(0x35EF4444) else Color(0xFFFEE2E2)
                            isSelected -> BirizTurquoise
                            else -> if (isDarkTheme) Color(0x30334E68) else Color(0xFFF1F5F9)
                        },
                        border = BorderStroke(
                            1.5.dp,
                            when {
                                isOccupied -> BirizError
                                isSelected -> if (isDarkTheme) Color.White else BirizTurquoise
                                else -> if (isDarkTheme) Color(0x4064748B) else Color(0xFFCBD5E1)
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = label,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        isOccupied -> if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFF991B1B)
                                        isSelected -> BirizNavyDark
                                        else -> if (isDarkTheme) Color.White else BirizNavyPrimary
                                    }
                                )
                                if (isOccupied) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Dolu",
                                        tint = BirizError,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                            if (isOccupied) {
                                Text(
                                    text = "Dolu",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFF991B1B)
                                )
                            }
                        }
                    }
                }
            }

            // Çakışma Uyarısı (Görseldeki "Bu şubede başka öğretmen var" kartı)
            AnimatedVisibility(visible = hasConflict && selectedSection != null) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    ConflictAlertCard(
                        occupiedClass = occupiedClassRoom,
                        selectedSection = selectedSection ?: "",
                        onSelectAlternativeSection = { onSectionSelected(it) },
                        isDark = isDarkTheme
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // KAYDET VE DEVAM ET BUTONU (Görsel 2'deki gibi Parlamalı ve Yuvarlatılmış)
            val isSubmitEnabled = !hasConflict && selectedSection != null && rawSchoolInput.isNotBlank()
            Button(
                onClick = onSubmit,
                enabled = isSubmitEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .shadow(
                        elevation = if (isSubmitEnabled) 12.dp else 0.dp,
                        shape = RoundedCornerShape(26.dp),
                        spotColor = if (isDarkTheme) BirizTurquoise else Color(0x3013294B)
                    )
                    .testTag("submit_registration_button"),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                    contentColor = if (isDarkTheme) BirizNavyDark else Color.White,
                    disabledContainerColor = if (isDarkTheme) Color(0x35334E68) else Color(0xFFE2E8F0),
                    disabledContentColor = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF475569)
                ),
                border = BorderStroke(
                    1.5.dp,
                    if (isSubmitEnabled && isDarkTheme) Color.White.copy(alpha = 0.5f) else Color.Transparent
                )
            ) {
                Text(
                    text = if (hasConflict) "Şube Dolu (Kayıt Engellendi)" else "Kaydet ve Devam Et",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = if (isSubmitEnabled) {
                        if (isDarkTheme) BirizNavyDark else Color.White
                    } else {
                        if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF475569)
                    }
                )
            }
        }
    }
}
