package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.BirizNotification
import com.example.data.model.NotificationType
import com.example.ui.theme.BirizAmber
import com.example.ui.theme.BirizBorder
import com.example.ui.theme.BirizDarkCardBorder
import com.example.ui.theme.BirizDarkSurfaceGlass
import com.example.ui.theme.BirizDarkTextMuted
import com.example.ui.theme.BirizDarkTextPrimary
import com.example.ui.theme.BirizDarkTextSecondary
import com.example.ui.theme.BirizNavyPrimary
import com.example.ui.theme.BirizTextMuted
import com.example.ui.theme.BirizTextPrimary
import com.example.ui.theme.BirizTextSecondary
import com.example.ui.theme.BirizTurquoise
import com.example.util.CommunicationPersistenceHelper

@Composable
fun NotificationCenterDialog(
    notifications: List<BirizNotification>,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onNotificationClick: (BirizNotification) -> Unit,
    onMarkAllAsRead: () -> Unit
) {
    val unreadCount = notifications.count { !it.isRead }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = if (isDarkTheme) BirizDarkSurfaceGlass else Color.White,
            border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("dialog_notification_center")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = BirizAmber.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = BirizAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Bildirim Merkezi",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                                )
                                if (unreadCount > 0) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = BirizAmber,
                                        modifier = Modifier.padding(2.dp)
                                    ) {
                                        Text(
                                            text = "$unreadCount Yeni",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Duyurular, veli mesajları ve bildirimler",
                                fontSize = 11.sp,
                                color = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else BirizTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bildirim Listesi
                if (notifications.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Henüz yeni bildiriminiz bulunmuyor.",
                            fontSize = 13.sp,
                            color = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(notifications) { notif ->
                            NotificationRowItem(
                                notification = notif,
                                isDarkTheme = isDarkTheme,
                                onClick = {
                                    onNotificationClick(notif)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Alt Aksiyonlar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (unreadCount > 0) {
                        Surface(
                            onClick = onMarkAllAsRead,
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDarkTheme) Color(0x35334E68) else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = BirizTurquoise,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Tümünü Okundu Say",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDarkTheme) Color.White else BirizNavyPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRowItem(
    notification: BirizNotification,
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    val (icon, iconColor, bgColor) = when (notification.type) {
        NotificationType.ANNOUNCEMENT -> Triple(
            Icons.Default.Campaign,
            Color(0xFFF59E0B),
            Color(0xFFFEF3C7)
        )
        NotificationType.MESSAGE -> Triple(
            Icons.Default.Chat,
            Color(0xFF10B981),
            Color(0xFFDCFCE7)
        )
        NotificationType.PARENT_REGISTRATION -> Triple(
            Icons.Default.PersonAdd,
            BirizTurquoise,
            Color(0xFFE0F2FE)
        )
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (notification.isRead) {
            if (isDarkTheme) Color(0x201E293B) else Color(0xFFF8FAFC)
        } else {
            if (isDarkTheme) Color(0x350F2746) else Color(0xFFEFF6FF)
        },
        border = BorderStroke(
            1.dp,
            if (!notification.isRead) {
                if (isDarkTheme) Color(0x5038BDF8) else Color(0xFFBFDBFE)
            } else {
                if (isDarkTheme) BirizDarkCardBorder else BirizBorder
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = CircleShape,
                color = if (isDarkTheme) iconColor.copy(alpha = 0.2f) else bgColor,
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        fontSize = 12.5.sp,
                        fontWeight = if (!notification.isRead) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                    )
                    Text(
                        text = CommunicationPersistenceHelper.formatTime(notification.timestamp),
                        fontSize = 10.sp,
                        color = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = notification.message,
                    fontSize = 11.5.sp,
                    color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )

                if (!notification.isRead) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Dokun ve sayfaya git →",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = BirizTurquoise
                    )
                }
            }

            if (!notification.isRead) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(BirizAmber)
                )
            }
        }
    }
}
