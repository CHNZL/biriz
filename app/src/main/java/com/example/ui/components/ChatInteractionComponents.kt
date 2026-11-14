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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import kotlinx.coroutines.delay
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
 * Öğretmen Modu: Sessiz Saatler & Zamanlanmış İletim Seçim, Tarih/Saat Klavye Girişi
 * ve Planlı Mesaj Yönetim Penceresi
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleDeliveryDialog(
    initialMessageText: String,
    scheduledMessages: List<ChatMessage> = emptyList(),
    activeStudentId: String = "",
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onSendNow: (text: String) -> Unit,
    onScheduleAt: (text: String, targetMillis: Long) -> Unit,
    onUpdateScheduledMessage: (messageId: String, newText: String, newScheduledFor: Long) -> Unit = { _, _, _ -> },
    onDeleteScheduledMessage: (messageId: String) -> Unit = {},
    onDeliverScheduledNow: (messageId: String) -> Unit = {}
) {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000L)
            nowMillis = System.currentTimeMillis()
        }
    }

    val presets = remember(nowMillis) { ChatFeatureHelper.buildSchedulePresets(nowMillis) }
    val primaryText = if (isDarkTheme) BirizDarkTextPrimary else BirizTextPrimary
    val secondaryText = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
    val cardBg = if (isDarkTheme) Color(0xFF1F2C34) else Color.White
    val surfaceBg = if (isDarkTheme) Color(0xFF182229) else Color(0xFFF8FAFC)
    val borderColor = if (isDarkTheme) BirizDarkCardBorder else BirizBorder

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val hourFocusRequester = remember { FocusRequester() }
    val minuteFocusRequester = remember { FocusRequester() }

    var draftText by remember(initialMessageText) { mutableStateOf(initialMessageText) }
    var editingMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var messagePendingDelete by remember { mutableStateOf<ChatMessage?>(null) }

    val defaultSlot = presets.firstOrNull()?.targetMillis
        ?: ChatFeatureHelper.nextTargetSlotMillis(8, 30, nowMillis)
    val defaultCal = remember(defaultSlot) { Calendar.getInstance().apply { timeInMillis = defaultSlot } }

    var showCustomPicker by remember { mutableStateOf(initialMessageText.isBlank()) }
    // dayMode: 0 = Bugün, 1 = Yarın, 2 = Özel Tarih
    var dayMode by remember {
        mutableIntStateOf(ChatFeatureHelper.detectDayOption(defaultSlot, nowMillis))
    }
    var selectedUtcDateMillis by remember {
        mutableLongStateOf(ChatFeatureHelper.localDateToUtcMidnight(defaultSlot))
    }
    var customHour by remember { mutableIntStateOf(defaultCal.get(Calendar.HOUR_OF_DAY)) }
    var customMinute by remember { mutableIntStateOf(defaultCal.get(Calendar.MINUTE)) }
    var showDatePickerModal by remember { mutableStateOf(false) }

    val todayUtcMidnight = remember(nowMillis) {
        ChatFeatureHelper.localDateToUtcMidnight(nowMillis)
    }
    val tomorrowUtcMidnight = remember(nowMillis) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_YEAR, 1)
        }
        ChatFeatureHelper.localDateToUtcMidnight(cal.timeInMillis)
    }
    val currentYear = remember(nowMillis) {
        Calendar.getInstance().apply { timeInMillis = nowMillis }.get(Calendar.YEAR)
    }

    val activeUtcDateMillis = when (dayMode) {
        0 -> todayUtcMidnight
        1 -> tomorrowUtcMidnight
        else -> selectedUtcDateMillis.coerceAtLeast(todayUtcMidnight)
    }

    val candidateMillis = ChatFeatureHelper.combineUtcDateWithLocalTime(
        utcDateMillis = activeUtcDateMillis,
        hour = customHour,
        minute = customMinute
    )
    val isPastTime = candidateMillis <= nowMillis + 30_000L

    val sortedScheduled = remember(scheduledMessages, nowMillis) {
        ChatFeatureHelper.sortScheduledMessagesChronologically(scheduledMessages, nowMillis)
    }

    val startEditingMessage: (ChatMessage) -> Unit = { msg ->
        val target = msg.scheduledFor ?: (nowMillis + 3_600_000L)
        val cal = Calendar.getInstance().apply { timeInMillis = target }
        editingMessage = msg
        draftText = msg.text
        selectedUtcDateMillis = ChatFeatureHelper.localDateToUtcMidnight(target)
        dayMode = ChatFeatureHelper.detectDayOption(target, nowMillis)
        customHour = cal.get(Calendar.HOUR_OF_DAY)
        customMinute = cal.get(Calendar.MINUTE)
        showCustomPicker = true
        coroutineScope.launch {
            scrollState.animateScrollTo(0)
        }
    }

    val cancelEditing: () -> Unit = {
        editingMessage = null
        draftText = initialMessageText
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = cardBg,
            border = BorderStroke(1.dp, borderColor),
            shadowElevation = 10.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .widthIn(max = 520.dp)
                .heightIn(max = 680.dp)
                .testTag("dialog_schedule_delivery")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Başlık
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = if (isDarkTheme) Color(0x3038BDF8) else Color(0xFFE0F2FE),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (editingMessage != null) Icons.Default.Edit else Icons.Default.NightsStay,
                                contentDescription = null,
                                tint = if (isDarkTheme) BirizTurquoise else Color(0xFF0284C7),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (editingMessage != null) "Planlı Mesajı Düzenle" else "Zamanlanmış İletim",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = primaryText
                        )
                        Text(
                            text = if (editingMessage != null) {
                                "${editingMessage!!.studentName.ifBlank { "Öğrenci" }} • Tarih, saat veya metni güncelleyin"
                            } else {
                                "Veliyi gece rahatsız etmeden istediğiniz tarih ve saatte iletin"
                            },
                            fontSize = 11.5.sp,
                            color = secondaryText
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = secondaryText
                        )
                    }
                }

                // Düzenlenebilir Mesaj Metni Alanı
                OutlinedTextField(
                    value = draftText,
                    onValueChange = { draftText = it },
                    label = {
                        Text(
                            text = if (editingMessage != null) "Düzenlenen Mesaj" else "Planlanacak Mesaj",
                            fontSize = 12.sp
                        )
                    },
                    placeholder = {
                        Text(
                            text = "Planlamak istediğiniz mesajı yazınız...",
                            fontSize = 12.5.sp
                        )
                    },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(fontSize = 13.sp, color = primaryText),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00A884),
                        unfocusedBorderColor = borderColor,
                        focusedLabelColor = Color(0xFF00A884),
                        unfocusedLabelColor = secondaryText,
                        cursorColor = Color(0xFF00A884)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_schedule_message_draft")
                )

                val hasDraftText = draftText.isNotBlank()

                if (!showCustomPicker && editingMessage == null) {
                    presets.forEach { preset ->
                        Surface(
                            onClick = {
                                if (hasDraftText) {
                                    onScheduleAt(draftText.trim(), preset.targetMillis)
                                }
                            },
                            enabled = hasDraftText,
                            shape = RoundedCornerShape(12.dp),
                            color = if (preset.isRecommended) {
                                if (isDarkTheme) Color(0xFF005C4B) else Color(0xFFECFDF5)
                            } else {
                                surfaceBg
                            },
                            border = BorderStroke(
                                width = if (preset.isRecommended) 1.4.dp else 0.8.dp,
                                color = if (preset.isRecommended) Color(0xFF00A884) else borderColor
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
                                        color = if (hasDraftText) primaryText else secondaryText
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
                            val firstTarget = presets.firstOrNull()?.targetMillis ?: defaultSlot
                            val cal = Calendar.getInstance().apply { timeInMillis = firstTarget }
                            customHour = cal.get(Calendar.HOUR_OF_DAY)
                            customMinute = cal.get(Calendar.MINUTE)
                            selectedUtcDateMillis = ChatFeatureHelper.localDateToUtcMidnight(firstTarget)
                            dayMode = ChatFeatureHelper.detectDayOption(firstTarget, nowMillis)
                            showCustomPicker = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("btn_schedule_custom_time")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Farklı Tarih ve Saat Seç...", fontWeight = FontWeight.SemiBold)
                    }

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
                            onClick = {
                                if (hasDraftText) {
                                    onSendNow(draftText.trim())
                                }
                            },
                            enabled = hasDraftText,
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
                    // Gün / Tarih Seçimi: Bugün | Yarın | Tarih Seç
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ScheduleDayChip(
                            label = "Bugün",
                            selected = dayMode == 0,
                            isDarkTheme = isDarkTheme,
                            primaryText = primaryText,
                            testTag = "btn_schedule_day_today",
                            onClick = {
                                dayMode = 0
                                selectedUtcDateMillis = todayUtcMidnight
                            },
                            modifier = Modifier.weight(1f)
                        )
                        ScheduleDayChip(
                            label = "Yarın",
                            selected = dayMode == 1,
                            isDarkTheme = isDarkTheme,
                            primaryText = primaryText,
                            testTag = "btn_schedule_day_tomorrow",
                            onClick = {
                                dayMode = 1
                                selectedUtcDateMillis = tomorrowUtcMidnight
                            },
                            modifier = Modifier.weight(1f)
                        )
                        val customDateLabel = if (dayMode == 2) {
                            ChatFeatureHelper.formatShortDateChip(selectedUtcDateMillis)
                        } else {
                            "Tarih Seç"
                        }
                        ScheduleDayChip(
                            label = customDateLabel,
                            selected = dayMode == 2,
                            isDarkTheme = isDarkTheme,
                            primaryText = primaryText,
                            showCalendarIcon = true,
                            testTag = "btn_schedule_day_pick",
                            onClick = {
                                showDatePickerModal = true
                            },
                            modifier = Modifier.weight(1.15f)
                        )
                    }

                    // Doğrudan Klavyeden Yazılabilir + Stepper Saat/Dakika Seçici
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = surfaceBg,
                        border = BorderStroke(0.8.dp, borderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                EditableTimeUnitColumn(
                                    label = "Saat",
                                    value = customHour,
                                    maxValue = 23,
                                    step = 1,
                                    isDarkTheme = isDarkTheme,
                                    primaryText = primaryText,
                                    secondaryText = secondaryText,
                                    focusRequester = hourFocusRequester,
                                    imeAction = ImeAction.Next,
                                    fieldTestTag = "input_schedule_hour",
                                    onImeAction = { minuteFocusRequester.requestFocus() },
                                    onValueChange = { customHour = it }
                                )
                                Text(
                                    text = ":",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = primaryText,
                                    modifier = Modifier.padding(top = 14.dp)
                                )
                                EditableTimeUnitColumn(
                                    label = "Dakika",
                                    value = customMinute,
                                    maxValue = 59,
                                    step = 5,
                                    isDarkTheme = isDarkTheme,
                                    primaryText = primaryText,
                                    secondaryText = secondaryText,
                                    focusRequester = minuteFocusRequester,
                                    imeAction = ImeAction.Done,
                                    fieldTestTag = "input_schedule_minute",
                                    onImeAction = {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                    },
                                    onValueChange = { customMinute = it }
                                )
                            }
                            Text(
                                text = "Rakamların üzerine dokunup klavyeden sayı girebilirsiniz",
                                fontSize = 10.5.sp,
                                color = secondaryText,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    if (isPastTime) {
                        Text(
                            text = if (dayMode == 0) {
                                "⚠️ Seçtiğiniz saat bugün için geçmiş görünüyor; 'Yarın', 'Tarih Seç' veya daha ileri bir saat seçiniz."
                            } else {
                                "⚠️ Lütfen şu andan ileri bir tarih ve saat seçiniz."
                            },
                            fontSize = 11.sp,
                            color = BirizAmber,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (!hasDraftText) {
                        Text(
                            text = "💡 Mesaj planlamak için yukarıya mesaj yazın ya da aşağıdan kayıtlı mesajlarınızı yönetin.",
                            fontSize = 11.sp,
                            color = secondaryText,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    val targetPreviewLabel = ChatFeatureHelper.formatScheduledLabel(candidateMillis, nowMillis)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                if (editingMessage != null) {
                                    cancelEditing()
                                } else {
                                    showCustomPicker = false
                                }
                            },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text(if (editingMessage != null) "Düzenlemeyi İptal" else "Hazır Saatler")
                        }

                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                val targetEdit = editingMessage
                                if (targetEdit != null) {
                                    onUpdateScheduledMessage(targetEdit.id, draftText.trim(), candidateMillis)
                                    cancelEditing()
                                } else {
                                    onScheduleAt(draftText.trim(), candidateMillis)
                                }
                            },
                            enabled = hasDraftText && !isPastTime,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00A884),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("btn_confirm_custom_schedule")
                        ) {
                            Text(
                                text = if (editingMessage != null) {
                                    "Güncelle ($targetPreviewLabel)"
                                } else {
                                    "$targetPreviewLabel'da İlet"
                                },
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Kronolojik Planlanmış Mesajlar Listesi (En Yakından En Uzağa)
                HorizontalDivider(color = borderColor)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color(0xFF00A884),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Planlanmış Mesajlar (${sortedScheduled.size})",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryText
                        )
                    }
                    if (sortedScheduled.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDarkTheme) Color(0xFF111B21) else Color(0xFFE0F2FE)
                        ) {
                            Text(
                                text = "En yakından en uzağa",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDarkTheme) BirizTurquoise else Color(0xFF0369A1),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                if (sortedScheduled.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = surfaceBg,
                        border = BorderStroke(0.8.dp, borderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Henüz bekleyen planlanmış mesajınız bulunmuyor.",
                            fontSize = 11.5.sp,
                            color = secondaryText,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 14.dp)
                        )
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.testTag("list_scheduled_messages")
                    ) {
                        sortedScheduled.forEachIndexed { index, msg ->
                            val isCurrentlyEditing = editingMessage?.id == msg.id
                            val schedTime = msg.scheduledFor ?: msg.timestamp
                            val timeLabel = ChatFeatureHelper.formatScheduledLabel(schedTime, nowMillis)
                            val recipientLabel = buildString {
                                if (msg.studentName.isNotBlank()) append(msg.studentName)
                                if (msg.parentName.isNotBlank()) {
                                    if (isNotEmpty()) append(" • ")
                                    append(msg.parentName)
                                }
                            }.ifBlank { "Veli" }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isCurrentlyEditing) {
                                    if (isDarkTheme) Color(0xFF0B3B36) else Color(0xFFECFDF5)
                                } else {
                                    surfaceBg
                                },
                                border = BorderStroke(
                                    width = if (isCurrentlyEditing) 1.4.dp else 0.8.dp,
                                    color = if (isCurrentlyEditing) Color(0xFF00A884) else borderColor
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("scheduled_message_item_${msg.id}")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (index == 0) Color(0xFF00A884) else if (isDarkTheme) Color(0xFF263540) else Color(0xFFE2E8F0)
                                            ) {
                                                Text(
                                                    text = timeLabel,
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (index == 0) Color.White else primaryText,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                )
                                            }
                                            if (index == 0) {
                                                Text(
                                                    text = "Sıradaki",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF00A884)
                                                )
                                            }
                                        }

                                        if (activeStudentId.isNotBlank() && msg.studentId == activeStudentId) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (isDarkTheme) Color(0x3038BDF8) else Color(0xFFE0F2FE)
                                            ) {
                                                Text(
                                                    text = "Bu sohbet",
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (isDarkTheme) BirizTurquoise else Color(0xFF0284C7),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = recipientLabel,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = secondaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Text(
                                        text = msg.text,
                                        fontSize = 12.5.sp,
                                        color = primaryText,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(
                                            onClick = { onDeliverScheduledNow(msg.id) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.heightIn(min = 48.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Send,
                                                contentDescription = "Şimdi İlet",
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Şimdi İlet", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                        }

                                        TextButton(
                                            onClick = { startEditingMessage(msg) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .testTag("btn_edit_scheduled_${msg.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Düzenle",
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Düzenle", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                        }

                                        TextButton(
                                            onClick = { messagePendingDelete = msg },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444)),
                                            modifier = Modifier
                                                .heightIn(min = 48.dp)
                                                .testTag("btn_delete_scheduled_${msg.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Sil",
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Sil", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
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

    if (showDatePickerModal) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedUtcDateMillis.coerceAtLeast(todayUtcMidnight),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis >= todayUtcMidnight
                }

                override fun isSelectableYear(year: Int): Boolean {
                    return year >= currentYear
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePickerModal = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val pickedUtc = datePickerState.selectedDateMillis
                        if (pickedUtc != null) {
                            selectedUtcDateMillis = pickedUtc
                            val combined = ChatFeatureHelper.combineUtcDateWithLocalTime(pickedUtc, customHour, customMinute)
                            dayMode = ChatFeatureHelper.detectDayOption(combined, System.currentTimeMillis())
                        }
                        showDatePickerModal = false
                    },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Seç", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePickerModal = false },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Vazgeç")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    messagePendingDelete?.let { deleteTarget ->
        AlertDialog(
            onDismissRequest = { messagePendingDelete = null },
            title = {
                Text("Planlı Mesajı Sil", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "\"${deleteTarget.text.take(80)}${if (deleteTarget.text.length > 80) "…" else ""}\" mesajının planlanmış gönderimi iptal edilip silinsin mi?",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editingMessage?.id == deleteTarget.id) {
                            cancelEditing()
                        }
                        onDeleteScheduledMessage(deleteTarget.id)
                        messagePendingDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444)),
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Sil", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { messagePendingDelete = null },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text("Vazgeç")
                }
            }
        )
    }
}

@Composable
private fun ScheduleDayChip(
    label: String,
    selected: Boolean,
    isDarkTheme: Boolean,
    primaryText: Color,
    showCalendarIcon: Boolean = false,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) {
            Color(0xFF00A884)
        } else if (isDarkTheme) {
            Color(0xFF182229)
        } else {
            Color(0xFFF1F5F9)
        },
        border = if (!selected) {
            BorderStroke(0.8.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder)
        } else {
            null
        },
        modifier = modifier
            .heightIn(min = 48.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showCalendarIcon) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = if (selected) Color.White else primaryText,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else primaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EditableTimeUnitColumn(
    label: String,
    value: Int,
    maxValue: Int,
    step: Int,
    isDarkTheme: Boolean,
    primaryText: Color,
    secondaryText: Color,
    focusRequester: FocusRequester,
    imeAction: ImeAction,
    fieldTestTag: String,
    onImeAction: () -> Unit,
    onValueChange: (Int) -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var isFocused by remember { mutableStateOf(false) }
    val formatted = remember(value) { String.format(Locale.US, "%02d", value) }
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = formatted, selection = TextRange(formatted.length)))
    }

    LaunchedEffect(value, isFocused) {
        if (!isFocused && textFieldValue.text != formatted) {
            textFieldValue = TextFieldValue(
                text = formatted,
                selection = TextRange(formatted.length)
            )
        }
    }

    val cycleValue: (Int) -> Unit = { delta ->
        val mod = maxValue + 1
        val next = ((value + delta) % mod + mod) % mod
        val nextText = String.format(Locale.US, "%02d", next)
        textFieldValue = TextFieldValue(text = nextText, selection = TextRange(0, nextText.length))
        onValueChange(next)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = secondaryText)
        IconButton(
            onClick = { cycleValue(step) },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "$label artır", tint = primaryText)
        }
        Surface(
            onClick = {
                focusRequester.requestFocus()
                keyboardController?.show()
            },
            shape = RoundedCornerShape(12.dp),
            color = if (isDarkTheme) Color(0xFF111B21) else Color.White,
            border = BorderStroke(
                width = if (isFocused) 1.8.dp else 1.dp,
                color = if (isFocused) Color(0xFF00A884) else if (isDarkTheme) BirizDarkCardBorder else BirizBorder
            ),
            modifier = Modifier
                .width(78.dp)
                .height(50.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { incoming ->
                        val digits = incoming.text.filter { it.isDigit() }.take(2)
                        if (digits.isEmpty()) {
                            textFieldValue = incoming.copy(text = "", selection = TextRange(0))
                        } else {
                            val parsed = digits.toIntOrNull() ?: 0
                            val clamped = parsed.coerceIn(0, maxValue)
                            val displayText = if (parsed > maxValue) {
                                String.format(Locale.US, "%02d", clamped)
                            } else {
                                digits
                            }
                            textFieldValue = TextFieldValue(
                                text = displayText,
                                selection = TextRange(displayText.length)
                            )
                            onValueChange(clamped)
                        }
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = primaryText,
                        textAlign = TextAlign.Center
                    ),
                    cursorBrush = SolidColor(Color(0xFF00A884)),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = imeAction
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { onImeAction() },
                        onDone = { onImeAction() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            val nowFocused = focusState.isFocused
                            if (nowFocused && !isFocused) {
                                textFieldValue = TextFieldValue(
                                    text = formatted,
                                    selection = TextRange(0, formatted.length)
                                )
                            } else if (!nowFocused && isFocused) {
                                val safe = textFieldValue.text.toIntOrNull()?.coerceIn(0, maxValue) ?: value
                                onValueChange(safe)
                                val safeStr = String.format(Locale.US, "%02d", safe)
                                textFieldValue = TextFieldValue(
                                    text = safeStr,
                                    selection = TextRange(safeStr.length)
                                )
                            }
                            isFocused = nowFocused
                        }
                        .testTag(fieldTestTag)
                )
            }
        }
        IconButton(
            onClick = { cycleValue(-step) },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(Icons.Default.Remove, contentDescription = "$label azalt", tint = primaryText)
        }
    }
}
