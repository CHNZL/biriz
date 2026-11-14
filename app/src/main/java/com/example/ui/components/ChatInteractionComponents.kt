package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.ChatMessage
import com.example.data.model.MessageStatus
import com.example.ui.theme.BirizAmber
import com.example.ui.theme.BirizBorder
import com.example.ui.theme.BirizDarkCardBorder
import com.example.ui.theme.BirizDarkTextMuted
import com.example.ui.theme.BirizDarkTextPrimary
import com.example.ui.theme.BirizDarkTextSecondary
import com.example.ui.theme.BirizNavyDark
import com.example.ui.theme.BirizNavyPrimary
import com.example.ui.theme.BirizTextMuted
import com.example.ui.theme.BirizTextPrimary
import com.example.ui.theme.BirizTextSecondary
import com.example.ui.theme.BirizTurquoise
import com.example.util.ChatFeatureHelper
import com.example.util.CommunicationPersistenceHelper
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

/**
 * WhatsApp stili çift yönlü mesaj balonu:
 * - Sağa kaydırarak alıntılı yanıt başlatma (Swipe-to-Reply)
 * - İç içe alıntı önizleme kartı (dokununca kaynak mesaja kaydırır)
 * - Zamanlanmış iletim rozeti (Öğretmen tarafında beklerken)
 * - Balon altı emoji tepki rozetleri (👍, ❤️, 👀 ...)
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun WhatsAppChatBubble(
    message: ChatMessage,
    isMe: Boolean,
    isDarkTheme: Boolean,
    fontOffset: Int = 0,
    isSelected: Boolean = false,
    isHighlighted: Boolean = false,
    isSelectionMode: Boolean = false,
    showEmailInHeader: Boolean = false,
    fallbackIncomingSenderName: String = "Sınıf Öğretmeni",
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSwipeReply: () -> Unit,
    onQuotedMessageClick: (String) -> Unit,
    onReactionBadgeClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val dragOffsetPx = remember { Animatable(0f) }
    val maxSwipePx = with(density) { 64.dp.toPx() }
    val triggerThresholdPx = with(density) { 42.dp.toPx() }
    var triggeredThisDrag by remember { mutableStateOf(false) }

    val now = System.currentTimeMillis()
    val isPendingScheduled = message.isPendingScheduled(now)
    val canSwipeReply = !isSelectionMode && !message.isDeletedForEveryone && !isPendingScheduled
    val reactionSummary = remember(message.reactions) {
        ChatFeatureHelper.summarizeReactions(message.reactions)
    }

    val highlightColor by animateColorAsState(
        targetValue = when {
            isSelected -> BirizTurquoise.copy(alpha = 0.24f)
            isHighlighted -> Color(0xFF00A884).copy(alpha = 0.26f)
            else -> Color.Transparent
        },
        label = "bubble_highlight"
    )

    val swipeModifier = if (canSwipeReply) {
        Modifier.pointerInput(message.id, canSwipeReply) {
            detectHorizontalDragGestures(
                onDragStart = {
                    triggeredThisDrag = false
                },
                onDragEnd = {
                    if (dragOffsetPx.value >= triggerThresholdPx) {
                        onSwipeReply()
                    }
                    scope.launch {
                        dragOffsetPx.animateTo(0f, spring())
                    }
                },
                onDragCancel = {
                    scope.launch {
                        dragOffsetPx.animateTo(0f, spring())
                    }
                },
                onHorizontalDrag = { change, dragAmount ->
                    val current = dragOffsetPx.value
                    val next = (current + dragAmount).coerceIn(0f, maxSwipePx)
                    if (next > 0f || current > 0f) {
                        change.consume()
                        scope.launch { dragOffsetPx.snapTo(next) }
                        if (!triggeredThisDrag && next >= triggerThresholdPx) {
                            triggeredThisDrag = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }
                }
            )
        }
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(highlightColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .then(swipeModifier)
            .padding(horizontal = 2.dp, vertical = 2.dp)
            .testTag("chat_bubble_${message.id}")
    ) {
        // Sürükleme sırasında solda beliren Yanıtla ikonu
        val swipeFraction = (dragOffsetPx.value / triggerThresholdPx).coerceIn(0f, 1f)
        if (swipeFraction > 0.06f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
                    .size(34.dp)
                    .alpha(swipeFraction)
                    .clip(CircleShape)
                    .background(
                        if (swipeFraction >= 1f) Color(0xFF00A884)
                        else if (isDarkTheme) Color(0xFF1F2C34) else Color(0xFFD9DEE2)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Reply,
                    contentDescription = "Yanıtla",
                    tint = if (swipeFraction >= 1f) Color.White else if (isDarkTheme) Color.White else Color(0xFF54656F),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(dragOffsetPx.value.roundToInt(), 0) },
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 14.dp,
                    topEnd = 14.dp,
                    bottomStart = if (isMe) 14.dp else 2.dp,
                    bottomEnd = if (isMe) 2.dp else 14.dp
                ),
                color = when {
                    isPendingScheduled && isMe -> if (isDarkTheme) Color(0xFF15384B) else Color(0xFFE0F2FE)
                    isMe -> if (isDarkTheme) Color(0xFF005C4B) else Color(0xFFD9FDD3)
                    else -> if (isDarkTheme) Color(0xFF202C33) else Color.White
                },
                border = when {
                    isSelected -> BorderStroke(1.5.dp, BirizTurquoise)
                    isPendingScheduled -> BorderStroke(1.dp, if (isDarkTheme) BirizTurquoise.copy(alpha = 0.6f) else Color(0xFF38BDF8))
                    else -> null
                },
                shadowElevation = 1.dp,
                modifier = Modifier.widthIn(min = 96.dp, max = 290.dp + (fontOffset * 12).dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                    // Zamanlanmış İletim Rozeti (Öğretmen önizlemesi)
                    if (isPendingScheduled && message.scheduledFor != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDarkTheme) Color(0x450284C7) else Color(0xFFBAE6FD).copy(alpha = 0.65f),
                            modifier = Modifier.padding(bottom = 5.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = "Zamanlanmış mesaj",
                                    tint = if (isDarkTheme) Color(0xFF7DD3FC) else Color(0xFF0369A1),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Zamanlandı • ${ChatFeatureHelper.formatScheduledLabel(message.scheduledFor, now)}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkTheme) Color(0xFFE0F2FE) else Color(0xFF0C4A6E)
                                )
                            }
                        }
                    }

                    // Gelen mesaj gönderen bilgisi
                    if (!isMe) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = message.senderName.ifBlank { fallbackIncomingSenderName },
                                fontSize = (10.5 + (fontOffset / 2)).sp,
                                fontWeight = FontWeight.Bold,
                                color = if (showEmailInHeader) Color(0xFF25D366) else Color(0xFF0284C7)
                            )
                            if (showEmailInHeader && message.parentEmail.isNotBlank()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "• ${message.parentEmail}",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    // Alıntılanan Mesaj Önizleme Kutusu
                    if (!message.isDeletedForEveryone && !message.replyToMessageId.isNullOrBlank()) {
                        val accentColor = if (message.replyToIsFromTeacher == true) {
                            Color(0xFF00A884)
                        } else {
                            Color(0xFF0284C7)
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDarkTheme) Color(0x350B141A) else Color(0x120F172A),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 5.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onQuotedMessageClick(message.replyToMessageId) }
                                .testTag("quoted_reply_preview_${message.id}")
                        ) {
                            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .fillMaxHeight()
                                        .background(accentColor)
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = message.replyToSenderName?.ifBlank { "Mesaj" } ?: "Mesaj",
                                        fontSize = (10.5 + (fontOffset / 2)).sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = message.replyToText.orEmpty(),
                                        fontSize = (11 + (fontOffset / 2)).sp,
                                        color = if (isDarkTheme) Color(0xFFCBD5E1) else Color(0xFF475569),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // Ana Mesaj Metni
                    Text(
                        text = if (message.isDeletedForEveryone) "🚫 Bu mesaj silindi" else message.text,
                        fontSize = (12.5 + fontOffset).sp,
                        fontStyle = if (message.isDeletedForEveryone) FontStyle.Italic else FontStyle.Normal,
                        color = if (message.isDeletedForEveryone) {
                            if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
                        } else {
                            if (isDarkTheme) Color.White else Color(0xFF111B21)
                        },
                        lineHeight = (17 + fontOffset).sp
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    // Saat & Tik / Zamanlama İkonu
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = CommunicationPersistenceHelper.formatTime(message.timestamp),
                            fontSize = (9.5 + (fontOffset / 2)).sp,
                            color = if (isDarkTheme) Color.White.copy(alpha = 0.65f) else Color.Gray
                        )

                        if (isMe && !message.isDeletedForEveryone) {
                            Spacer(modifier = Modifier.width(4.dp))
                            if (isPendingScheduled) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = "Zamanlandı",
                                    tint = if (isDarkTheme) Color(0xFF7DD3FC) else Color(0xFF0284C7),
                                    modifier = Modifier.size(13.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = if (message.status == MessageStatus.SENT) Icons.Default.Check else Icons.Default.DoneAll,
                                    contentDescription = when (message.status) {
                                        MessageStatus.READ -> "Okundu"
                                        MessageStatus.DELIVERED -> "İletildi"
                                        MessageStatus.SENT -> "Gönderildi"
                                    },
                                    tint = when (message.status) {
                                        MessageStatus.READ -> Color(0xFF53BDEB)
                                        MessageStatus.DELIVERED -> if (isDarkTheme) Color(0xFFCBD5E1) else Color.Gray
                                        MessageStatus.SENT -> Color.LightGray
                                    },
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Balon altı Emoji Tepki Rozetleri
            if (reactionSummary.isNotEmpty() && !message.isDeletedForEveryone) {
                Surface(
                    onClick = onReactionBadgeClick,
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDarkTheme) Color(0xFF233138) else Color.White,
                    border = BorderStroke(0.8.dp, if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0)),
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .offset(y = (-4).dp)
                        .padding(horizontal = 6.dp)
                        .testTag("reaction_badge_${message.id}")
                ) {
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        reactionSummary.forEach { (emoji, count) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = emoji, fontSize = 13.sp)
                                if (count > 1) {
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "$count",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkTheme) Color(0xFFE2E8F0) else Color(0xFF334155)
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

/**
 * Mesaj kutusu üzerinde gösterilen "Yanıtlanıyor" önizleme şeridi.
 */
@Composable
fun ReplyComposerBanner(
    replyingTo: ChatMessage?,
    isDarkTheme: Boolean,
    onCancelReply: () -> Unit
) {
    AnimatedVisibility(
        visible = replyingTo != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        replyingTo?.let { msg ->
            val accentColor = if (msg.isFromTeacher) Color(0xFF00A884) else Color(0xFF0284C7)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isDarkTheme) Color(0xFF162127) else Color(0xFFF1F5F9),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 2.dp)
                    .testTag("reply_composer_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .background(accentColor)
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Yanıtlanıyor • ${msg.senderName}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = msg.text,
                            fontSize = 12.sp,
                            color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = onCancelReply,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("btn_cancel_reply")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Alıntıyı iptal et",
                            tint = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Akşam/gece saatlerinde öğretmene "Yarın 08:30'da İlet" seçeneğini hatırlatan sessiz saatler bandı.
 */
@Composable
fun QuietHoursHintBanner(
    isDarkTheme: Boolean,
    hasText: Boolean,
    onQuickScheduleMorning: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isDarkTheme) Color(0xFF172554).copy(alpha = 0.72f) else Color(0xFFEEF2FF),
        border = BorderStroke(0.8.dp, if (isDarkTheme) Color(0xFF3B82F6).copy(alpha = 0.5f) else Color(0xFFC7D2FE)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .testTag("quiet_hours_hint_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.NightsStay,
                contentDescription = null,
                tint = if (isDarkTheme) Color(0xFF93C5FD) else Color(0xFF4338CA),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Sessiz saatler • Veliyi rahatsız etmemek için ⏰ ile sabah 08:30'a zamanlayabilirsiniz",
                fontSize = 10.5.sp,
                lineHeight = 14.sp,
                color = if (isDarkTheme) Color(0xFFDBEAFE) else Color(0xFF312E81),
                modifier = Modifier.weight(1f)
            )
            if (hasText) {
                TextButton(
                    onClick = onQuickScheduleMorning,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.heightIn(min = 36.dp)
                ) {
                    Text(
                        text = "08:30",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDarkTheme) Color(0xFF7DD3FC) else Color(0xFF4338CA)
                    )
                }
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Sessiz saat uyarısını kapat",
                    tint = if (isDarkTheme) Color(0xFF93C5FD) else Color(0xFF6366F1),
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

/**
 * Uzun basma ile açılan WhatsApp stili Mesaj İşlem Penceresi:
 * - Üstte tek dokunuşla 👍 ❤️ 👀 😂 🙏 👏 tepki şeridi
 * - Altta Yanıtla, Bilgi, Kopyala, (Zamanlanmışsa Şimdi İlet), Birden Fazla Seç ve Sil seçenekleri
 */
@Composable
fun MessageLongPressActionDialog(
    message: ChatMessage,
    currentReactionEmoji: String?,
    viewerIsTeacher: Boolean,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onSelectReaction: (String) -> Unit,
    onReply: () -> Unit,
    onShowInfo: () -> Unit,
    onCopyText: () -> Unit,
    onDeliverNow: () -> Unit,
    onEnterSelectionMode: () -> Unit,
    onDeleteSingle: () -> Unit
) {
    val isPendingScheduled = message.isPendingScheduled()
    val cardBg = if (isDarkTheme) Color(0xFF1F2C34) else Color.White
    val primaryText = if (isDarkTheme) BirizDarkTextPrimary else BirizTextPrimary
    val secondaryText = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("message_action_dialog"),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Tek Dokunuşla Emoji Tepki Şeridi
            if (!message.isDeletedForEveryone) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ChatFeatureHelper.QUICK_REACTIONS.forEach { emoji ->
                            val isChosen = currentReactionEmoji == emoji
                            Surface(
                                onClick = {
                                    onSelectReaction(emoji)
                                    onDismiss()
                                },
                                shape = CircleShape,
                                color = if (isChosen) {
                                    if (isDarkTheme) Color(0xFF005C4B) else Color(0xFFD1FAE5)
                                } else {
                                    Color.Transparent
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("btn_react_$emoji")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = emoji, fontSize = 22.sp)
                                }
                            }
                        }
                    }
                }
            }

            // 2. Mesaj Önizleme ve İşlemler Listesi
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = cardBg,
                border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
                shadowElevation = 10.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Kısa Mesaj Özeti
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isDarkTheme) Color(0xFF182229) else Color(0xFFF8FAFC))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = message.senderName,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00A884)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (message.isDeletedForEveryone) "🚫 Bu mesaj silindi" else message.text,
                            fontSize = 12.5.sp,
                            color = secondaryText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    HorizontalDivider(color = if (isDarkTheme) BirizDarkCardBorder else BirizBorder)

                    if (!message.isDeletedForEveryone) {
                        ActionMenuRow(
                            icon = Icons.AutoMirrored.Filled.Reply,
                            label = "Yanıtla",
                            tint = Color(0xFF00A884),
                            textColor = primaryText,
                            testTag = "action_reply_message",
                            onClick = {
                                onDismiss()
                                onReply()
                            }
                        )
                        ActionMenuRow(
                            icon = Icons.Default.Info,
                            label = "Mesaj Bilgisi (İletildi / Okundu)",
                            tint = if (isDarkTheme) Color(0xFF38BDF8) else Color(0xFF0284C7),
                            textColor = primaryText,
                            testTag = "action_info_message",
                            onClick = {
                                onDismiss()
                                onShowInfo()
                            }
                        )
                        if (message.text.isNotBlank()) {
                            ActionMenuRow(
                                icon = Icons.Default.ContentCopy,
                                label = "Kopyala",
                                tint = secondaryText,
                                textColor = primaryText,
                                testTag = "action_copy_message",
                                onClick = {
                                    onDismiss()
                                    onCopyText()
                                }
                            )
                        }
                        if (viewerIsTeacher && isPendingScheduled) {
                            ActionMenuRow(
                                icon = Icons.AutoMirrored.Filled.Send,
                                label = "Beklemeden Şimdi İlet",
                                tint = Color(0xFF10B981),
                                textColor = primaryText,
                                testTag = "action_deliver_now_message",
                                onClick = {
                                    onDismiss()
                                    onDeliverNow()
                                }
                            )
                        }
                    }

                    ActionMenuRow(
                        icon = Icons.Default.SelectAll,
                        label = "Birden Fazla Seç",
                        tint = secondaryText,
                        textColor = primaryText,
                        testTag = "action_multiselect_message",
                        onClick = {
                            onDismiss()
                            onEnterSelectionMode()
                        }
                    )

                    HorizontalDivider(color = if (isDarkTheme) BirizDarkCardBorder else BirizBorder)

                    ActionMenuRow(
                        icon = Icons.Default.Delete,
                        label = if (viewerIsTeacher && isPendingScheduled) "Zamanlamayı İptal Et / Sil" else "Sil",
                        tint = Color(0xFFEF4444),
                        textColor = Color(0xFFEF4444),
                        testTag = "action_delete_message",
                        onClick = {
                            onDismiss()
                            onDeleteSingle()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionMenuRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    textColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

/**
 * WhatsApp stili Mesaj Bilgi Ekranı: Gönderildi / İletildi / Okundu saatlerini tam gösterir.
 */
@Composable
fun MessageInfoDialog(
    message: ChatMessage,
    viewerIsMe: Boolean,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit
) {
    val cardBg = if (isDarkTheme) Color(0xFF1F2C34) else Color.White
    val primaryText = if (isDarkTheme) BirizDarkTextPrimary else BirizTextPrimary
    val secondaryText = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
    val now = System.currentTimeMillis()
    val isPendingScheduled = message.isPendingScheduled(now)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cardBg,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFF00A884),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Mesaj Bilgisi",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryText
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dialog_message_info"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Mesaj Balonu Özeti
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (viewerIsMe) {
                        if (isDarkTheme) Color(0xFF005C4B) else Color(0xFFD9FDD3)
                    } else {
                        if (isDarkTheme) Color(0xFF111B21) else Color(0xFFF1F5F9)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = message.text.ifBlank { "🚫 Bu mesaj silindi" },
                        fontSize = 13.sp,
                        color = if (isDarkTheme) Color.White else Color(0xFF111B21),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                // Durum Zaman Çizelgesi Kartı
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isDarkTheme) Color(0xFF111B21) else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isPendingScheduled && message.scheduledFor != null) {
                            MessageInfoStatusRow(
                                icon = Icons.Default.Schedule,
                                iconTint = Color(0xFF0284C7),
                                title = "Zamanlanmış İletim",
                                detail = ChatFeatureHelper.formatDetailedDateTime(message.scheduledFor, now),
                                primaryText = primaryText,
                                secondaryText = secondaryText
                            )
                        } else {
                            // Okundu Satırı
                            val readDetail = when {
                                message.status == MessageStatus.READ && message.readAt != null ->
                                    ChatFeatureHelper.formatDetailedDateTime(message.readAt, now)
                                message.status == MessageStatus.READ ->
                                    "Okundu (detaylı saat sunucu senkronu bekliyor)"
                                else -> "Henüz okunmadı"
                            }
                            MessageInfoStatusRow(
                                icon = Icons.Default.DoneAll,
                                iconTint = if (message.status == MessageStatus.READ) Color(0xFF53BDEB) else secondaryText.copy(alpha = 0.5f),
                                title = if (viewerIsMe) "Okundu" else "Okuma Zamanınız",
                                detail = readDetail,
                                primaryText = primaryText,
                                secondaryText = secondaryText
                            )

                            HorizontalDivider(color = if (isDarkTheme) BirizDarkCardBorder else BirizBorder)

                            // İletildi Satırı
                            val deliveredDetail = when {
                                message.status == MessageStatus.SENT -> "Sunucuya iletiliyor..."
                                message.deliveredAt != null -> ChatFeatureHelper.formatDetailedDateTime(message.deliveredAt, now)
                                else -> ChatFeatureHelper.formatDetailedDateTime(message.timestamp, now)
                            }
                            MessageInfoStatusRow(
                                icon = Icons.Default.DoneAll,
                                iconTint = if (message.status != MessageStatus.SENT) Color(0xFF64748B) else secondaryText.copy(alpha = 0.5f),
                                title = "İletildi",
                                detail = deliveredDetail,
                                primaryText = primaryText,
                                secondaryText = secondaryText
                            )

                            HorizontalDivider(color = if (isDarkTheme) BirizDarkCardBorder else BirizBorder)

                            // Gönderildi Satırı
                            MessageInfoStatusRow(
                                icon = Icons.Default.Check,
                                iconTint = secondaryText,
                                title = if (viewerIsMe) "Gönderildi" else "Gönderilme Zamanı",
                                detail = ChatFeatureHelper.formatDetailedDateTime(message.timestamp, now),
                                primaryText = primaryText,
                                secondaryText = secondaryText
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Kapat", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun MessageInfoStatusRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    detail: String,
    primaryText: Color,
    secondaryText: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = primaryText
            )
            Text(
                text = detail,
                fontSize = 12.sp,
                color = secondaryText
            )
        }
    }
}

/**
 * Balon altındaki tepkiye basıldığında kimin hangi tepkiyi verdiğini gösteren ve tek dokunuşla
 * tepki değiştirmeye/kaldırmaya izin veren pencere.
 */
@Composable
fun ReactionDetailsDialog(
    message: ChatMessage,
    currentParticipantKey: String,
    teacherDisplayName: String,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onSelectEmoji: (String) -> Unit
) {
    val activeMap = message.activeReactions()
    val myCurrent = activeMap[currentParticipantKey]
    val cardBg = if (isDarkTheme) Color(0xFF1F2C34) else Color.White
    val primaryText = if (isDarkTheme) BirizDarkTextPrimary else BirizTextPrimary
    val secondaryText = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cardBg,
        title = {
            Text(
                text = "Mesaj Tepkileri (${activeMap.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = primaryText
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ChatFeatureHelper.QUICK_REACTIONS.forEach { emoji ->
                        val selected = myCurrent == emoji
                        Surface(
                            onClick = {
                                onSelectEmoji(emoji)
                                onDismiss()
                            },
                            shape = CircleShape,
                            color = if (selected) {
                                if (isDarkTheme) Color(0xFF005C4B) else Color(0xFFD1FAE5)
                            } else Color.Transparent,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = emoji, fontSize = 20.sp)
                            }
                        }
                    }
                }

                HorizontalDivider(color = if (isDarkTheme) BirizDarkCardBorder else BirizBorder)

                activeMap.forEach { (key, emoji) ->
                    val label = ChatFeatureHelper.reactionParticipantDisplayLabel(
                        participantKey = key,
                        message = message,
                        teacherDisplayName = teacherDisplayName
                    )
                    val isMine = key == currentParticipantKey
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(enabled = isMine) {
                                onSelectEmoji(emoji)
                                onDismiss()
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = if (isMine) FontWeight.Bold else FontWeight.Medium,
                                color = primaryText
                            )
                            if (isMine) {
                                Text(
                                    text = "Kaldırmak için dokunun",
                                    fontSize = 10.5.sp,
                                    color = secondaryText
                                )
                            }
                        }
                        Text(text = emoji, fontSize = 20.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat", fontWeight = FontWeight.Bold)
            }
        }
    )
}

/**
 * Öğretmen Modu: Sessiz Saatler & Zamanlanmış İletim Seçim Penceresi
 */
@Composable
fun ScheduleDeliveryDialog(
    messagePreview: String,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onSendNow: () -> Unit,
    onScheduleAt: (Long) -> Unit
) {
    val now = remember { System.currentTimeMillis() }
    val presets = remember(now) { ChatFeatureHelper.buildSchedulePresets(now) }
    val primaryText = if (isDarkTheme) BirizDarkTextPrimary else BirizTextPrimary
    val secondaryText = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
    val cardBg = if (isDarkTheme) Color(0xFF1F2C34) else Color.White

    var showCustomPicker by remember { mutableStateOf(false) }
    var customTomorrow by remember { mutableStateOf(true) }
    var customHour by remember { mutableIntStateOf(8) }
    var customMinute by remember { mutableIntStateOf(30) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = cardBg,
            border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
            shadowElevation = 10.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dialog_schedule_delivery")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = if (isDarkTheme) Color(0x3038BDF8) else Color(0xFFE0F2FE),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.NightsStay,
                                contentDescription = null,
                                tint = if (isDarkTheme) BirizTurquoise else Color(0xFF0284C7),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Zamanlanmış İletim",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = primaryText
                        )
                        Text(
                            text = "Veliyi gece rahatsız etmeden iletin",
                            fontSize = 11.5.sp,
                            color = secondaryText
                        )
                    }
                }

                // Mesaj önizleme
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDarkTheme) Color(0xFF111B21) else Color(0xFFF8FAFC),
                    border = BorderStroke(0.8.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = messagePreview,
                        fontSize = 12.sp,
                        color = secondaryText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                if (!showCustomPicker) {
                    presets.forEach { preset ->
                        Surface(
                            onClick = {
                                onScheduleAt(preset.targetMillis)
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (preset.isRecommended) {
                                if (isDarkTheme) Color(0xFF005C4B) else Color(0xFFECFDF5)
                            } else {
                                if (isDarkTheme) Color(0xFF182229) else Color(0xFFF8FAFC)
                            },
                            border = BorderStroke(
                                width = if (preset.isRecommended) 1.4.dp else 0.8.dp,
                                color = if (preset.isRecommended) Color(0xFF00A884)
                                else if (isDarkTheme) BirizDarkCardBorder else BirizBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag("schedule_preset_${preset.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = if (preset.isRecommended) Color(0xFF00A884) else secondaryText,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = preset.title,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryText
                                    )
                                    Text(
                                        text = preset.subtitle,
                                        fontSize = 11.sp,
                                        color = secondaryText
                                    )
                                }
                                if (preset.isRecommended) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF00A884)
                                    ) {
                                        Text(
                                            text = "Önerilen",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance().apply { timeInMillis = presets.first().targetMillis }
                            customHour = cal.get(Calendar.HOUR_OF_DAY)
                            customMinute = cal.get(Calendar.MINUTE)
                            customTomorrow = ChatFeatureHelper.formatScheduledLabel(presets.first().targetMillis, now).startsWith("Yarın")
                            showCustomPicker = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("btn_schedule_custom_time")
                    ) {
                        Text("Farklı Bir Saat Seç...", fontWeight = FontWeight.SemiBold)
                    }

                    HorizontalDivider(color = if (isDarkTheme) BirizDarkCardBorder else BirizBorder)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Vazgeç")
                        }

                        Button(
                            onClick = onSendNow,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                                contentColor = if (isDarkTheme) BirizNavyDark else Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("btn_schedule_send_now")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Hemen Gönder", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Özel Saat & Gün Seçici
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = { customTomorrow = false },
                            shape = RoundedCornerShape(10.dp),
                            color = if (!customTomorrow) Color(0xFF00A884) else if (isDarkTheme) Color(0xFF182229) else Color(0xFFF1F5F9),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Bugün",
                                    fontWeight = FontWeight.Bold,
                                    color = if (!customTomorrow) Color.White else primaryText
                                )
                            }
                        }
                        Surface(
                            onClick = { customTomorrow = true },
                            shape = RoundedCornerShape(10.dp),
                            color = if (customTomorrow) Color(0xFF00A884) else if (isDarkTheme) Color(0xFF182229) else Color(0xFFF1F5F9),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Yarın",
                                    fontWeight = FontWeight.Bold,
                                    color = if (customTomorrow) Color.White else primaryText
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TimeStepperColumn(
                            label = "Saat",
                            valueText = String.format(Locale.US, "%02d", customHour),
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                            onIncrement = { customHour = (customHour + 1) % 24 },
                            onDecrement = { customHour = (customHour + 23) % 24 }
                        )
                        Text(
                            text = ":",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = primaryText
                        )
                        TimeStepperColumn(
                            label = "Dakika",
                            valueText = String.format(Locale.US, "%02d", customMinute),
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                            onIncrement = { customMinute = (customMinute + 15) % 60 },
                            onDecrement = { customMinute = (customMinute + 45) % 60 }
                        )
                    }

                    val candidateMillis = ChatFeatureHelper.nextTargetSlotMillis(
                        hour = customHour,
                        minute = customMinute,
                        nowMillis = System.currentTimeMillis(),
                        forceTomorrow = customTomorrow
                    )
                    val isPastToday = !customTomorrow && candidateMillis <= System.currentTimeMillis() + 60_000L
                    if (isPastToday) {
                        Text(
                            text = "⚠️ Seçtiğiniz saat bugün için geçmiş görünüyor; 'Yarın' veya daha ileri bir saat seçiniz.",
                            fontSize = 11.sp,
                            color = BirizAmber,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = { showCustomPicker = false },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Geri")
                        }
                        Button(
                            onClick = { onScheduleAt(candidateMillis) },
                            enabled = !isPastToday,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A884)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text(
                                text = "${if (customTomorrow) "Yarın" else "Bugün"} ${String.format(Locale.US, "%02d:%02d", customHour, customMinute)}'da İlet",
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

@Composable
private fun TimeStepperColumn(
    label: String,
    valueText: String,
    primaryText: Color,
    secondaryText: Color,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 11.sp, color = secondaryText)
        IconButton(onClick = onIncrement, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.Add, contentDescription = "$label artır", tint = primaryText)
        }
        Text(
            text = valueText,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            color = primaryText
        )
        IconButton(onClick = onDecrement, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.Remove, contentDescription = "$label azalt", tint = primaryText)
        }
    }
}
