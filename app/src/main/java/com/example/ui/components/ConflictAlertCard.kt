package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ClassRoom
import com.example.ui.theme.BirizError
import com.example.ui.theme.BirizErrorSoft
import com.example.ui.theme.BirizNavyPrimary
import com.example.ui.theme.BirizTextPrimary
import com.example.ui.theme.BirizTextSecondary

@Composable
fun ConflictAlertCard(
    modifier: Modifier = Modifier,
    occupiedClass: ClassRoom?,
    selectedSection: String,
    onSelectAlternativeSection: (String) -> Unit,
    isDark: Boolean = false
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("conflict_alert_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0x357F1D1D) else Color(0xFFFFF1F2)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isDark) Color(0xFFEF4444) else Color(0xFFFCA5A5)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0x30EF4444) else BirizErrorSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Şube Kilitli",
                        tint = if (isDark) Color(0xFFFCA5A5) else BirizError,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Şube Dolu / Seçilemez ($selectedSection Şubesi)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Bu şubenin sınıf öğretmeni zaten kayıtlıdır. Her sınıfın yalnız bir yönetici öğretmeni olabilir.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color(0xFFFECACA) else Color(0xFF991B1B)
                    )

                    if (occupiedClass != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDark) Color(0x4013203E) else Color.White.copy(alpha = 0.9f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isDark) Color(0x40EF4444) else Color(0xFFFECACA)
                            )
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Mevcut Yönetici Öğretmen: ${occupiedClass.teacherName}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isDark) Color.White else BirizNavyPrimary
                                )
                                Text(
                                    text = "${occupiedClass.schoolName} - ${occupiedClass.gradeLevel}/${occupiedClass.section} Şubesi",
                                    fontSize = 12.sp,
                                    color = if (isDark) Color(0xFFCBD5E1) else BirizTextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Lütfen aşağıdaki müsait şubelerden birini seçiniz (Örn: B, C, D...)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E)
                        )
                    }
                }
            }
        }
    }
}
