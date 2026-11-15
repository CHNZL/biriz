package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.ui.theme.BirizAmber
import com.example.ui.theme.BirizDarkCardBorder
import com.example.ui.theme.BirizDarkTextMuted
import com.example.ui.theme.BirizDarkTextPrimary
import com.example.ui.theme.BirizDarkTextSecondary
import com.example.ui.theme.BirizNavyPrimary
import com.example.ui.theme.BirizTextSecondary
import com.example.ui.theme.BirizTurquoise
import com.example.util.NotificationCategory
import com.example.util.NotificationPreferencesHelper

@Composable
fun NotificationPreferencesSection(
    role: UserRole,
    preferences: Map<String, Boolean>,
    isDarkTheme: Boolean,
    onToggleCategory: (NotificationCategory, Boolean) -> Unit,
    onSetAllCategories: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = NotificationCategory.categoriesForRole(role)
    val enabledCount = categories.count { NotificationPreferencesHelper.isCategoryEnabled(preferences, it) }
    val allEnabled = enabledCount == categories.size && categories.isNotEmpty()
    val noneEnabled = enabledCount == 0

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = BirizTurquoise.copy(alpha = 0.14f)
            ) {
                Text(
                    text = "$enabledCount / ${categories.size} Kategori Açık",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = { onSetAllCategories(true) },
                    enabled = !allEnabled,
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .testTag("notif_prefs_enable_all")
                ) {
                    Text(
                        text = "Tümünü Aç",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (!allEnabled) BirizTurquoise else Color.Gray
                    )
                }
                TextButton(
                    onClick = { onSetAllCategories(false) },
                    enabled = !noneEnabled,
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .testTag("notif_prefs_disable_all")
                ) {
                    Text(
                        text = "Tümünü Kapat",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (!noneEnabled) BirizAmber else Color.Gray
                    )
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categories.forEachIndexed { index, category ->
                val isChecked = NotificationPreferencesHelper.isCategoryEnabled(preferences, category)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDarkTheme) {
                        if (isChecked) Color(0xFF172536) else Color(0xFF111827)
                    } else {
                        if (isChecked) Color(0xFFF8FAFC) else Color(0xFFF1F5F9)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (isChecked) {
                            if (isDarkTheme) BirizTurquoise.copy(alpha = 0.35f) else Color(0xFFCBD5E1)
                        } else {
                            if (isDarkTheme) BirizDarkCardBorder else Color(0xFFE2E8F0)
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onToggleCategory(category, !isChecked) }
                        .testTag("notif_pref_row_${category.key}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isChecked) {
                                    BirizTurquoise.copy(alpha = 0.16f)
                                } else {
                                    Color.Gray.copy(alpha = 0.12f)
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = category.emoji, fontSize = 16.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.padding(end = 8.dp)) {
                                Text(
                                    text = category.title,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkTheme) {
                                        if (isChecked) BirizDarkTextPrimary else BirizDarkTextMuted
                                    } else {
                                        if (isChecked) BirizNavyPrimary else Color(0xFF64748B)
                                    }
                                )
                                Text(
                                    text = category.description,
                                    fontSize = 10.5.sp,
                                    lineHeight = 14.sp,
                                    color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                                )
                            }
                        }

                        Switch(
                            checked = isChecked,
                            onCheckedChange = { onToggleCategory(category, it) },
                            modifier = Modifier
                                .testTag("notif_pref_${category.key}")
                                .semantics {
                                    contentDescription = "${category.title} bildirimlerini ${if (isChecked) "kapat" else "aç"}"
                                },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = BirizTurquoise,
                                uncheckedThumbColor = if (isDarkTheme) Color(0xFFCBD5E1) else BirizNavyPrimary,
                                uncheckedTrackColor = if (isDarkTheme) Color(0xFF334155) else Color(0xFFCBD5E1)
                            )
                        )
                    }
                }
                if (index == categories.lastIndex) {
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
        }

        HorizontalDivider(
            color = if (isDarkTheme) BirizDarkCardBorder else Color(0xFFE2E8F0),
            thickness = 1.dp
        )
        Text(
            text = "Kapatılan kategoriler hem üst zil simgesinde hem de telefon bildirim çubuğunda gösterilmez.",
            fontSize = 10.5.sp,
            lineHeight = 14.sp,
            color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
        )
    }
}
