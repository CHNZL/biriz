package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.School
import com.example.ui.theme.BirizAmber
import com.example.ui.theme.BirizBorder
import com.example.ui.theme.BirizNavyDark
import com.example.ui.theme.BirizNavyPrimary
import com.example.ui.theme.BirizSuccess
import com.example.ui.theme.BirizTextMuted
import com.example.ui.theme.BirizTextPrimary
import com.example.ui.theme.BirizTextSecondary
import com.example.ui.theme.BirizTurquoise
import com.example.ui.theme.BirizTurquoiseSoft

@Composable
fun SchoolAutocompleteField(
    modifier: Modifier = Modifier,
    cityName: String?,
    districtName: String?,
    schoolInput: String,
    selectedSchool: School?,
    suggestions: List<School>,
    onInputChanged: (String) -> Unit,
    onSchoolSelected: (School) -> Unit,
    onAddNewSchool: (String) -> Unit,
    isDark: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    val isEnabled = !cityName.isNullOrBlank() && !districtName.isNullOrBlank()

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = schoolInput,
            onValueChange = { onInputChanged(com.example.util.TurkishTextHelper.toTurkishUpperCase(it)) },
            enabled = isEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused }
                .testTag("school_input_field"),
            label = {
                Text(
                    text = if (isEnabled) "Okul Adı" else "Önce İl ve İlçe Seçiniz",
                    fontWeight = FontWeight.Medium
                )
            },
            placeholder = { Text("Örn: Atatürk İlkokulu") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = if (isEnabled) (if (isDark) BirizTurquoise else BirizNavyPrimary) else BirizTextMuted
                )
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectedSchool != null) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Seçildi",
                            tint = BirizSuccess,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    if (schoolInput.isNotEmpty()) {
                        IconButton(onClick = { onInputChanged("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Temizle",
                                tint = BirizTextMuted
                            )
                        }
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = if (isDark) Color.White else BirizTextPrimary,
                unfocusedTextColor = if (isDark) Color.White else BirizTextPrimary,
                focusedBorderColor = BirizTurquoise,
                unfocusedBorderColor = if (isDark) Color(0xFF334E68) else BirizBorder,
                focusedLabelColor = if (isDark) BirizTurquoise else BirizNavyPrimary,
                unfocusedLabelColor = if (isDark) Color(0xFF94A3B8) else BirizTextSecondary,
                cursorColor = BirizTurquoise
            ),
            singleLine = true
        )

        // Açılır Tavsiye ve Ekleme Paneli (Yalnızca o ilçede kayıtlı okul varsa veya yeni okul ekleme gerekiyorsa)
        val shouldShowDropdown = isFocused && isEnabled && (suggestions.isNotEmpty() || schoolInput.trim().length >= 3)
        AnimatedVisibility(visible = shouldShowDropdown) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .testTag("school_suggestions_dropdown"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF13203E) else Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isDark) Color(0xFF334E68) else BirizBorder
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    if (suggestions.isNotEmpty()) {
                        Text(
                            text = "Kayıtlı Okullar (${suggestions.size}):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) BirizTurquoise else BirizNavyPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )

                        LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                            items(suggestions) { school ->
                                val isSelected = selectedSchool?.id == school.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) {
                                                if (isDark) Color(0x3528B8C2) else BirizTurquoiseSoft
                                            } else Color.Transparent
                                        )
                                        .clickable {
                                            onSchoolSelected(school)
                                            isFocused = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.School,
                                        contentDescription = null,
                                        tint = if (isSelected) BirizTurquoise else BirizTextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = school.name,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = if (isDark) Color.White else BirizTextPrimary
                                        )
                                        Text(
                                            text = "${school.cityName} / ${school.districtName}",
                                            fontSize = 11.sp,
                                            color = if (isDark) Color(0xFF94A3B8) else BirizTextSecondary
                                        )
                                    }
                                    if (isSelected) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = BirizTurquoise
                                        ) {
                                            Text(
                                                text = "Seçildi",
                                                color = BirizNavyDark,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Yeni Okul Ekleme Seçeneği
                    val exactMatchExists = suggestions.any { it.name.equals(schoolInput.trim(), ignoreCase = true) }
                    if (schoolInput.trim().length >= 3 && !exactMatchExists) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0x35E67E22) else Color(0xFFFFFBEB))
                                .border(
                                    1.dp,
                                    if (isDark) BirizAmber.copy(alpha = 0.7f) else BirizAmber.copy(alpha = 0.4f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    onAddNewSchool(schoolInput.trim())
                                    isFocused = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = BirizAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Yeni Okul Olarak Ekle:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BirizAmber
                                )
                                Text(
                                    text = schoolInput.trim(),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) Color.White else BirizNavyPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
