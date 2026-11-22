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
import com.example.util.ParentNotificationDeferralConfig
import com.example.util.QuietHoursConfig

@Composable
fun NotificationPreferencesSection(
    role: UserRole,
    preferences: Map<String, Boolean>,
    isDarkTheme: Boolean,
    onToggleCategory: (NotificationCategory, Boolean) -> Unit,
    onSetAllCategories: (Boolean) -> Unit,
    quietHoursConfig: QuietHoursConfig = QuietHoursConfig(),
    deferredQuietCount: Int = 0,
    onToggleQuietHours: (Boolean) -> Unit = {},
    onUpdateQuietHoursWindow: (Int, Int, Int, Int) -> Unit = { _, _, _, _ -> },
    teacherParentDeferralConfig: ParentNotificationDeferralConfig = ParentNotificationDeferralConfig(),
    onToggleTeacherParentDeferral: (Boolean) -> Unit = {},
    onUpdateTeacherParentDeferralWindow: (Int, Int, Int, Int) -> Unit = { _, _, _, _ -> },
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
        // --- ÖĞRETMEN: VELİ BİLDİRİMLERİNİ ERTELEME (GECE ÇALIŞMA MODU) ---
        if (role == UserRole.TEACHER) {
            TeacherParentDeferralCard(
                config = teacherParentDeferralConfig,
                isDarkTheme = isDarkTheme,
                onToggle = onToggleTeacherParentDeferral,
                onUpdateWindow = onUpdateTeacherParentDeferralWindow
            )
        } else if (role == UserRole.PARENT && teacherParentDeferralConfig.enabled) {
            val rangeLabel = NotificationPreferencesHelper.formatDeferralRange(teacherParentDeferralConfig)
            val deliverLabel = NotificationPreferencesHelper.formatTime(
                teacherParentDeferralConfig.deliverHour,
                teacherParentDeferralConfig.deliverMinute
            )
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isDarkTheme) Color(0xFF1A2438) else Color(0xFFFFFBEB),
                border = BorderStroke(
                    1.dp,
                    if (isDarkTheme) BirizAmber.copy(alpha = 0.5f) else Color(0xFFFCD34D)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("parent_teacher_deferral_info_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "👨‍🏫 Öğretmen Gece Çalışma Koruması Aktif ($rangeLabel)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) Color(0xFFFDE68A) else Color(0xFF92400E)
                    )
                    Text(
                        text = "Öğretmeninizin bu saatlerde yaptığı sınıf veri girişlerinin bildirimleri sizi rahatsız etmemek için saat $deliverLabel'da topluca iletilir. Güvenlik uyarıları her zaman anında gelir.",
                        fontSize = 10.5.sp,
                        lineHeight = 14.5.sp,
                        color = if (isDarkTheme) BirizDarkTextSecondary else Color(0xFF78350F)
                    )
                }
            }
        }

        // --- SESSİZ SAATLER (GECE KORUMASI) KARTI ---
        val quietRangeLabel = NotificationPreferencesHelper.formatQuietHoursRange(quietHoursConfig)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isDarkTheme) {
                if (quietHoursConfig.enabled) Color(0xFF16273B) else Color(0xFF111827)
            } else {
                if (quietHoursConfig.enabled) Color(0xFFEFF6FF) else Color(0xFFF8FAFC)
            },
            border = BorderStroke(
                1.dp,
                if (quietHoursConfig.enabled) {
                    BirizTurquoise.copy(alpha = if (isDarkTheme) 0.55f else 0.45f)
                } else if (isDarkTheme) {
                    BirizDarkCardBorder
                } else {
                    Color(0xFFE2E8F0)
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { onToggleQuietHours(!quietHoursConfig.enabled) }
                .testTag("quiet_hours_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (role == UserRole.TEACHER) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = BirizTurquoise.copy(alpha = if (isDarkTheme) 0.18f else 0.12f)
                    ) {
                        Text(
                            text = "📥 Size Gelen Bildirimler",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (quietHoursConfig.enabled) {
                                BirizAmber.copy(alpha = 0.18f)
                            } else {
                                Color.Gray.copy(alpha = 0.12f)
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = "🌙", fontSize = 16.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.padding(end = 8.dp)) {
                            Text(
                                text = "Sessiz Saatler (Gece Koruması)",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                            )
                            Text(
                                text = "$quietRangeLabel • Rutin bildirimler sabah topluca iletilir; güvenlik uyarıları anında gelir",
                                fontSize = 10.5.sp,
                                lineHeight = 14.sp,
                                color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                            )
                        }
                    }

                    Switch(
                        checked = quietHoursConfig.enabled,
                        onCheckedChange = { onToggleQuietHours(it) },
                        modifier = Modifier
                            .testTag("quiet_hours_switch")
                            .semantics {
                                contentDescription = if (quietHoursConfig.enabled) {
                                    "Sessiz saatleri kapat"
                                } else {
                                    "Sessiz saatleri aç"
                                }
                            },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = BirizAmber,
                            uncheckedThumbColor = if (isDarkTheme) Color(0xFFCBD5E1) else BirizNavyPrimary,
                            uncheckedTrackColor = if (isDarkTheme) Color(0xFF334155) else Color(0xFFCBD5E1)
                        )
                    )
                }

                if (quietHoursConfig.enabled) {
                    val presets = listOf(
                        Triple("21:00 – 08:00", "quiet_hours_preset_21_08", listOf(21, 0, 8, 0)),
                        Triple("22:00 – 08:00", "quiet_hours_preset_22_08", listOf(22, 0, 8, 0)),
                        Triple("23:00 – 07:30", "quiet_hours_preset_23_0730", listOf(23, 0, 7, 30))
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.forEach { (label, tag, coords) ->
                            val isSelected = quietHoursConfig.startHour == coords[0] &&
                                quietHoursConfig.startMinute == coords[1] &&
                                quietHoursConfig.endHour == coords[2] &&
                                quietHoursConfig.endMinute == coords[3]
                            Surface(
                                onClick = {
                                    onUpdateQuietHoursWindow(coords[0], coords[1], coords[2], coords[3])
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) {
                                    BirizTurquoise
                                } else if (isDarkTheme) {
                                    Color(0xFF1E293B)
                                } else {
                                    Color.White
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) BirizTurquoise else if (isDarkTheme) BirizDarkCardBorder else Color(0xFFCBD5E1)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag(tag)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        color = if (isSelected) {
                                            Color.White
                                        } else if (isDarkTheme) {
                                            BirizDarkTextPrimary
                                        } else {
                                            BirizNavyPrimary
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (deferredQuietCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BirizAmber.copy(alpha = 0.16f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🌅 Sabah özeti için $deferredQuietCount bildirim bekliyor",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) Color(0xFFFDE68A) else Color(0xFFB45309),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

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
                        .heightIn(min = 48.dp)
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
                        .heightIn(min = 48.dp)
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

@Composable
private fun TeacherParentDeferralCard(
    config: ParentNotificationDeferralConfig,
    isDarkTheme: Boolean,
    onToggle: (Boolean) -> Unit,
    onUpdateWindow: (Int, Int, Int, Int) -> Unit
) {
    val startLabel = NotificationPreferencesHelper.formatTime(config.startHour, config.startMinute)
    val deliverLabel = NotificationPreferencesHelper.formatTime(config.deliverHour, config.deliverMinute)
    val rangeLabel = NotificationPreferencesHelper.formatDeferralRange(config)
    val isCurrentlyActive = NotificationPreferencesHelper.isWithinDeferralWindow(config)

    val cardBg = if (isDarkTheme) {
        if (config.enabled) Color(0xFF231F36) else Color(0xFF111827)
    } else {
        if (config.enabled) Color(0xFFFFFBEB) else Color(0xFFF8FAFC)
    }
    val cardBorder = if (config.enabled) {
        if (isDarkTheme) BirizAmber.copy(alpha = 0.65f) else Color(0xFFF59E0B)
    } else if (isDarkTheme) {
        BirizDarkCardBorder
    } else {
        Color(0xFFE2E8F0)
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        border = BorderStroke(1.2.dp, cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onToggle(!config.enabled) }
            .testTag("teacher_parent_deferral_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (config.enabled) {
                        BirizAmber.copy(alpha = if (isDarkTheme) 0.24f else 0.18f)
                    } else {
                        Color.Gray.copy(alpha = 0.14f)
                    }
                ) {
                    Text(
                        text = "👨‍🏫 → 👨‍👩‍👧 Velilere Giden Bildirimler",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (config.enabled) {
                            if (isDarkTheme) Color(0xFFFDE68A) else Color(0xFFB45309)
                        } else if (isDarkTheme) {
                            BirizDarkTextSecondary
                        } else {
                            BirizTextSecondary
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                if (config.enabled) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (isCurrentlyActive) {
                            Color(0xFF10B981).copy(alpha = 0.2f)
                        } else {
                            BirizTurquoise.copy(alpha = 0.16f)
                        }
                    ) {
                        Text(
                            text = if (isCurrentlyActive) "🟢 Şu An Aktif" else "⏰ Hazır",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrentlyActive) {
                                if (isDarkTheme) Color(0xFF6EE7B7) else Color(0xFF047857)
                            } else {
                                if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (config.enabled) {
                            BirizAmber.copy(alpha = 0.22f)
                        } else {
                            Color.Gray.copy(alpha = 0.12f)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = "🛌", fontSize = 17.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.padding(end = 8.dp)) {
                        Text(
                            text = "Veli Bildirimlerini Ertele (Gece Çalışma Modu)",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                        )
                        Text(
                            text = "Gece yaptığınız yıldız, kitaplık, duyuru veya program girişleri velilere o an gitmez; seçtiğiniz sabah saatinde topluca iletilir.",
                            fontSize = 10.5.sp,
                            lineHeight = 14.5.sp,
                            color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                        )
                    }
                }

                Switch(
                    checked = config.enabled,
                    onCheckedChange = { onToggle(it) },
                    modifier = Modifier
                        .testTag("teacher_parent_deferral_switch")
                        .semantics {
                            contentDescription = if (config.enabled) {
                                "Veli bildirim ertelemeyi kapat"
                            } else {
                                "Veli bildirim ertelemeyi aç"
                            }
                        },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = BirizAmber,
                        uncheckedThumbColor = if (isDarkTheme) Color(0xFFCBD5E1) else BirizNavyPrimary,
                        uncheckedTrackColor = if (isDarkTheme) Color(0xFF334155) else Color(0xFFCBD5E1)
                    )
                )
            }

            if (!config.enabled) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDarkTheme) Color(0xFF1E293B) else Color.White,
                    border = BorderStroke(
                        1.dp,
                        if (isDarkTheme) BirizDarkCardBorder else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "💡 Veliler kendi telefonlarında sessiz saatleri açmamış olsalar bile gece veri girişi yaparken rahatsızlık vermemek için bu özelliği açabilirsiniz.",
                        fontSize = 10.5.sp,
                        lineHeight = 14.5.sp,
                        color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }
            } else {
                // Canlı özet kutusu
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDarkTheme) Color(0xFF1B2838) else Color.White,
                    border = BorderStroke(
                        1.dp,
                        if (isDarkTheme) BirizAmber.copy(alpha = 0.35f) else Color(0xFFFDE68A)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isCurrentlyActive) {
                            "🛡️ Şu an yaptığınız tüm sınıf girişlerinin bildirimleri bekletiliyor ve velilere saat $deliverLabel'da topluca iletilecek."
                        } else {
                            "🛡️ $rangeLabel arasında yapacağınız tüm sınıf girişlerinin bildirimleri, veliler sessiz saat açmamış olsalar bile saat $deliverLabel'da topluca iletilir."
                        },
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDarkTheme) Color(0xFFFDE68A) else Color(0xFF92400E),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }

                // Hızlı Saat Seçenekleri
                val presets = listOf(
                    Triple("21:00 → 10:00", "deferral_preset_21_10", listOf(21, 0, 10, 0)),
                    Triple("22:00 → 09:00", "deferral_preset_22_09", listOf(22, 0, 9, 0)),
                    Triple("20:00 → 10:00", "deferral_preset_20_10", listOf(20, 0, 10, 0))
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { (label, tag, coords) ->
                        val isSelected = config.startHour == coords[0] &&
                            config.startMinute == coords[1] &&
                            config.deliverHour == coords[2] &&
                            config.deliverMinute == coords[3]
                        Surface(
                            onClick = {
                                onUpdateWindow(coords[0], coords[1], coords[2], coords[3])
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) {
                                BirizAmber
                            } else if (isDarkTheme) {
                                Color(0xFF1E293B)
                            } else {
                                Color.White
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) BirizAmber else if (isDarkTheme) BirizDarkCardBorder else Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag(tag)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = if (isSelected) {
                                        Color.White
                                    } else if (isDarkTheme) {
                                        BirizDarkTextPrimary
                                    } else {
                                        BirizNavyPrimary
                                    }
                                )
                            }
                        }
                    }
                }

                // Özel Saat Ayarlama Satırları (30 dk adımlarla)
                DeferralTimeStepperRow(
                    title = "Gece Erteleme Başlangıcı",
                    subtitle = "Bu saatten sonra yapılan işlemler bekletilir",
                    timeText = startLabel,
                    isDarkTheme = isDarkTheme,
                    minusTag = "deferral_start_minus",
                    plusTag = "deferral_start_plus",
                    onDecrement = {
                        val (h, m) = NotificationPreferencesHelper.stepTimeByMinutes(config.startHour, config.startMinute, -30)
                        onUpdateWindow(h, m, config.deliverHour, config.deliverMinute)
                    },
                    onIncrement = {
                        val (h, m) = NotificationPreferencesHelper.stepTimeByMinutes(config.startHour, config.startMinute, 30)
                        onUpdateWindow(h, m, config.deliverHour, config.deliverMinute)
                    }
                )

                DeferralTimeStepperRow(
                    title = "Velilere İletim Saati (Sabah)",
                    subtitle = "Bekleyen bildirimler bu saatte topluca gider",
                    timeText = deliverLabel,
                    isDarkTheme = isDarkTheme,
                    minusTag = "deferral_deliver_minus",
                    plusTag = "deferral_deliver_plus",
                    onDecrement = {
                        val (h, m) = NotificationPreferencesHelper.stepTimeByMinutes(config.deliverHour, config.deliverMinute, -30)
                        onUpdateWindow(config.startHour, config.startMinute, h, m)
                    },
                    onIncrement = {
                        val (h, m) = NotificationPreferencesHelper.stepTimeByMinutes(config.deliverHour, config.deliverMinute, 30)
                        onUpdateWindow(config.startHour, config.startMinute, h, m)
                    }
                )

                Text(
                    text = "ℹ️ Yalnızca velilere giden bildirimleri erteler; size gelen veli mesajlarını etkilemez. Güvenlik uyarıları her zaman anında iletilir.",
                    fontSize = 10.sp,
                    lineHeight = 13.5.sp,
                    color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                )
            }
        }
    }
}

@Composable
private fun DeferralTimeStepperRow(
    title: String,
    subtitle: String,
    timeText: String,
    isDarkTheme: Boolean,
    minusTag: String,
    plusTag: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isDarkTheme) Color(0xFF162033) else Color.White,
        border = BorderStroke(
            1.dp,
            if (isDarkTheme) BirizDarkCardBorder else Color(0xFFE2E8F0)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = title,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    onClick = onDecrement,
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDarkTheme) Color(0xFF243247) else Color(0xFFF1F5F9),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag(minusTag)
                        .semantics { contentDescription = "$title 30 dakika azalt" }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "−",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                        )
                    }
                }
                Text(
                    text = timeText,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDarkTheme) Color(0xFFFDE68A) else Color(0xFFB45309)
                )
                Surface(
                    onClick = onIncrement,
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDarkTheme) Color(0xFF243247) else Color(0xFFF1F5F9),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag(plusTag)
                        .semantics { contentDescription = "$title 30 dakika artır" }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "+",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                        )
                    }
                }
            }
        }
    }
}

