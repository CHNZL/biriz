package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BirizBorder
import com.example.ui.theme.BirizDarkCardBorder
import com.example.ui.theme.BirizDarkTextMuted
import com.example.ui.theme.BirizDarkTextPrimary
import com.example.ui.theme.BirizTextMuted
import com.example.ui.theme.BirizTurquoise
import com.example.util.ChatEmojiCategory
import com.example.util.ChatEmojiHelper

/**
 * WhatsApp stili mesaj giriş barı ve entegre kategorili emoji çekmecesi.
 * - Mesaj kutusunun baş kısmındaki emoji/klavye ikonu ile tek dokunuşla açılıp kapanır.
 * - Opsiyonel alıntı (Yanıtla) önizlemesi, sessiz saatler ipucu bandı ve zamanlanmış iletim (Öğretmen Modu) destekler.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WhatsAppChatComposer(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    placeholder: String,
    isDarkTheme: Boolean,
    fontOffset: Int = 0,
    inputTestTag: String,
    sendTestTag: String,
    emojiToggleTestTag: String = "btn_chat_emoji_toggle",
    onSend: () -> Unit,
    resetKey: Any? = Unit,
    enableScheduledSend: Boolean = false,
    scheduledMessageCount: Int = 0,
    onOpenScheduleOptions: (() -> Unit)? = null,
    topBannerContent: (@Composable () -> Unit)? = null,
    replyPreviewContent: (@Composable () -> Unit)? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val textFieldInteractionSource = remember { MutableInteractionSource() }

    var isEmojiPickerOpen by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(ChatEmojiCategory.SMILEYS) }
    var recentEmojis by remember { mutableStateOf(ChatEmojiHelper.loadRecentEmojis(context)) }

    // Konuşma değiştiğinde emoji panelini kapat
    LaunchedEffect(resetKey) {
        isEmojiPickerOpen = false
    }

    // Kullanıcı metin alanına dokunduğunda sistem klavyesi açılacağı için emoji çekmecesini kapat
    LaunchedEffect(textFieldInteractionSource) {
        textFieldInteractionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release && isEmojiPickerOpen) {
                isEmojiPickerOpen = false
                keyboardController?.show()
            }
        }
    }

    // Android Geri tuşuna basıldığında önce açık emoji penceresini kapat
    BackHandler(enabled = isEmojiPickerOpen) {
        isEmojiPickerOpen = false
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        topBannerContent?.invoke()

        // Mesaj Giriş Kapsülü (varsa üstte alıntı önizlemesi ile) + Gönder Butonu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = if (isDarkTheme) Color(0xFF1F2C34) else Color.White,
                border = BorderStroke(
                    0.8.dp,
                    if (isEmojiPickerOpen) {
                        Color(0xFF00A884)
                    } else if (isDarkTheme) {
                        BirizDarkCardBorder
                    } else {
                        BirizBorder.copy(alpha = 0.7f)
                    }
                ),
                shadowElevation = 2.dp,
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    replyPreviewContent?.invoke()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // WhatsApp stili: Mesaj kutusunun baş kısmında Emoji / Klavye geçiş ikonu
                        IconButton(
                            onClick = {
                                if (isEmojiPickerOpen) {
                                    isEmojiPickerOpen = false
                                    focusRequester.requestFocus()
                                    keyboardController?.show()
                                } else {
                                    keyboardController?.hide()
                                    isEmojiPickerOpen = true
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .padding(bottom = 1.dp)
                                .testTag(emojiToggleTestTag)
                        ) {
                            Icon(
                                imageVector = if (isEmojiPickerOpen) {
                                    Icons.Default.Keyboard
                                } else {
                                    Icons.Default.SentimentSatisfiedAlt
                                },
                                contentDescription = if (isEmojiPickerOpen) {
                                    "Klavyeye geç"
                                } else {
                                    "Emoji penceresini aç"
                                },
                                tint = if (isEmojiPickerOpen) {
                                    Color(0xFF00A884)
                                } else if (isDarkTheme) {
                                    Color(0xFF94A3B8)
                                } else {
                                    Color(0xFF54656F)
                                },
                                modifier = Modifier.size(23.dp)
                            )
                        }

                        TextField(
                            value = value,
                            onValueChange = onValueChange,
                            placeholder = {
                                Text(
                                    text = placeholder,
                                    fontSize = (13.5 + fontOffset).sp,
                                    color = if (isDarkTheme) BirizDarkTextMuted else Color(0xFF667781)
                                )
                            },
                            textStyle = TextStyle(
                                fontSize = (14 + fontOffset).sp,
                                lineHeight = (19 + fontOffset).sp,
                                color = if (isDarkTheme) BirizDarkTextPrimary else Color(0xFF111B21)
                            ),
                            maxLines = 5,
                            interactionSource = textFieldInteractionSource,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (value.text.isNotBlank()) {
                                        onSend()
                                    }
                                }
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                                cursorColor = Color(0xFF00A884)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .focusRequester(focusRequester)
                                .testTag(inputTestTag)
                        )

                        if (enableScheduledSend && onOpenScheduleOptions != null) {
                            IconButton(
                                onClick = {
                                    keyboardController?.hide()
                                    isEmojiPickerOpen = false
                                    onOpenScheduleOptions()
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .padding(bottom = 1.dp)
                                    .testTag("btn_schedule_send")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = if (scheduledMessageCount > 0) {
                                            "Zamanlanmış iletim ve $scheduledMessageCount planlı mesaj"
                                        } else {
                                            "Zamanlanmış iletim seçenekleri"
                                        },
                                        tint = if (value.text.isNotBlank() || scheduledMessageCount > 0) {
                                            if (isDarkTheme) BirizTurquoise else Color(0xFF0284C7)
                                        } else if (isDarkTheme) {
                                            Color(0xFF94A3B8)
                                        } else {
                                            Color(0xFF54656F)
                                        },
                                        modifier = Modifier.size(22.dp)
                                    )
                                    if (scheduledMessageCount > 0) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF00A884),
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .offset(x = 6.dp, y = (-4).dp)
                                                .size(16.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = if (scheduledMessageCount > 9) "9+" else "$scheduledMessageCount",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White,
                                                    textAlign = TextAlign.Center
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

            Spacer(modifier = Modifier.width(6.dp))

            val canSend = value.text.isNotBlank()
            Surface(
                shape = CircleShape,
                color = if (canSend) Color(0xFF00A884) else if (isDarkTheme) Color(0xFF334155) else Color(0xFF94A3B8),
                contentColor = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .combinedClickable(
                        onClick = {
                            if (canSend) {
                                onSend()
                            }
                        },
                        onLongClick = if (enableScheduledSend && canSend && onOpenScheduleOptions != null) {
                            {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                keyboardController?.hide()
                                isEmojiPickerOpen = false
                                onOpenScheduleOptions()
                            }
                        } else null
                    )
                    .testTag(sendTestTag)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = if (enableScheduledSend && canSend) {
                            "Gönder (Zamanlamak için basılı tutun)"
                        } else {
                            "Gönder"
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // WhatsApp Stili Kategorili Emoji Çekmecesi
        AnimatedVisibility(
            visible = isEmojiPickerOpen,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
        ) {
            Surface(
                color = if (isDarkTheme) Color(0xFF111B21) else Color(0xFFF0F2F5),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(254.dp)
                    .testTag("whatsapp_emoji_picker_panel")
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    HorizontalDivider(
                        thickness = 0.8.dp,
                        color = if (isDarkTheme) Color(0xFF222E35) else Color(0xFFD9DEE2)
                    )

                    // Üst Kategori Sekmeleri + Sağda Silme (Backspace) Butonu
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isDarkTheme) Color(0xFF182229) else Color(0xFFE7EAEE))
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ChatEmojiCategory.entries.forEach { category ->
                            val isSelected = selectedCategory == category
                            Surface(
                                onClick = { selectedCategory = category },
                                color = Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag(category.testTag)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = category.icon,
                                        fontSize = 18.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .fillMaxWidth(0.65f)
                                                .height(2.5.dp)
                                                .background(
                                                    color = if (isDarkTheme) BirizTurquoise else Color(0xFF00A884),
                                                    shape = RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)
                                                )
                                        )
                                    }
                                }
                            }
                        }

                        IconButton(
                            onClick = {
                                val (newText, newCursor) = ChatEmojiHelper.deleteGraphemeBeforeCursor(
                                    text = value.text,
                                    selectionStart = value.selection.start,
                                    selectionEnd = value.selection.end
                                )
                                onValueChange(
                                    value.copy(
                                        text = newText,
                                        selection = TextRange(newCursor)
                                    )
                                )
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("btn_emoji_backspace")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Sil",
                                tint = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF54656F),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    // Kategori Başlığı
                    Text(
                        text = selectedCategory.title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )

                    // Emoji Izgarası
                    val emojisToShow = if (selectedCategory == ChatEmojiCategory.RECENT) {
                        recentEmojis
                    } else {
                        ChatEmojiHelper.categoryEmojis[selectedCategory].orEmpty()
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 44.dp),
                        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(emojisToShow, key = { "${selectedCategory.name}_$it" }) { emoji ->
                            Surface(
                                onClick = {
                                    val (newText, newCursor) = ChatEmojiHelper.insertEmojiAtSelection(
                                        text = value.text,
                                        selectionStart = value.selection.start,
                                        selectionEnd = value.selection.end,
                                        emoji = emoji
                                    )
                                    onValueChange(
                                        value.copy(
                                            text = newText,
                                            selection = TextRange(newCursor)
                                        )
                                    )
                                    recentEmojis = ChatEmojiHelper.recordRecentEmoji(context, emoji)
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = Color.Transparent,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = emoji,
                                        fontSize = 23.sp,
                                        textAlign = TextAlign.Center
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
