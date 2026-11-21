package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BirizBorder
import com.example.ui.theme.BirizDarkCardBorder
import com.example.ui.theme.BirizDarkCardGlass
import com.example.ui.theme.BirizDarkTextPrimary
import com.example.ui.theme.BirizDarkTextSecondary
import com.example.ui.theme.BirizError
import com.example.ui.theme.BirizNavyPrimary
import com.example.ui.theme.BirizTextSecondary
import com.example.util.SmartBoardHelper

private data class DailyNotePreset(
    val icon: String,
    val label: String
)

private val DAILY_NOTE_PRESETS = listOf(
    DailyNotePreset("📌", "Günün Notu"),
    DailyNotePreset("📝", "Ödev"),
    DailyNotePreset("🎒", "Yarın Getirilecekler"),
    DailyNotePreset("📢", "Hatırlatma")
)

/**
 * Akıllı Tahta Kumandası içinde öğretmenin "Günün Sabit Notu / Ödev Panosu"
 * metnini tek adımda yazıp tahtaya sabitlemesini veya kaldırmasını sağlayan kart.
 */
@Composable
fun SmartBoardDailyNoteCard(
    savedNote: String,
    savedLabel: String,
    isConnected: Boolean,
    isDarkTheme: Boolean,
    onSave: (label: String, text: String) -> Unit,
    onClear: () -> Unit
) {
    var noteInput by remember { mutableStateOf(savedNote) }
    var selectedLabel by remember {
        mutableStateOf(savedLabel.ifBlank { SmartBoardHelper.DEFAULT_DAILY_NOTE_LABEL })
    }

    LaunchedEffect(savedNote, savedLabel) {
        noteInput = savedNote
        selectedLabel = savedLabel.ifBlank { SmartBoardHelper.DEFAULT_DAILY_NOTE_LABEL }
    }

    val amberAccent = Color(0xFFD97706)
    val amberSoftBg = if (isDarkTheme) Color(0x28F59E0B) else Color(0xFFFFFBEB)
    val amberBorder = if (isDarkTheme) Color(0x66F59E0B) else Color(0xFFFCD34D)
    val hasSavedNote = savedNote.isNotBlank()
    val trimmedInput = noteInput.trim()
    val hasChanges = trimmedInput != savedNote.trim() ||
        selectedLabel != savedLabel.ifBlank { SmartBoardHelper.DEFAULT_DAILY_NOTE_LABEL }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("smart_board_daily_note_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) BirizDarkCardGlass else amberSoftBg
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (hasSavedNote) amberBorder else if (isDarkTheme) BirizDarkCardBorder else BirizBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "📌", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Günün Notu / Ödev Panosu",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                    )
                }

                if (hasSavedNote) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (isConnected) Color(0xFF059669).copy(alpha = 0.16f) else amberAccent.copy(alpha = 0.16f),
                        border = BorderStroke(
                            1.dp,
                            if (isConnected) Color(0xFF059669).copy(alpha = 0.5f) else amberAccent.copy(alpha = 0.5f)
                        )
                    ) {
                        Text(
                            text = if (isConnected) "Tahtada" else "Kayıtlı",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isConnected) {
                                if (isDarkTheme) Color(0xFF6EE7B7) else Color(0xFF065F46)
                            } else {
                                if (isDarkTheme) Color(0xFFFCD34D) else Color(0xFF92400E)
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Akıllı tahta ana sayfasının üst şeridinde sabit olarak görünür.",
                fontSize = 11.sp,
                lineHeight = 14.5.sp,
                color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Başlık türü seçici
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DAILY_NOTE_PRESETS.forEach { preset ->
                    val isSelected = selectedLabel == preset.label
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                color = if (isSelected) {
                                    amberAccent
                                } else if (isDarkTheme) {
                                    Color(0x22FFFFFF)
                                } else {
                                    Color.White
                                }
                            )
                            .clickable { selectedLabel = preset.label }
                            .padding(horizontal = 9.dp, vertical = 6.dp)
                            .testTag("smart_board_note_preset_${preset.label}")
                    ) {
                        Text(
                            text = "${preset.icon} ${preset.label}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
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

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = noteInput,
                onValueChange = { updated ->
                    if (updated.length <= SmartBoardHelper.MAX_DAILY_NOTE_LENGTH) {
                        noteInput = updated
                    }
                },
                placeholder = {
                    Text(
                        text = "Örn: Yarın resim defteri ve sulu boya getirilecek",
                        fontSize = 12.sp
                    )
                },
                minLines = 2,
                maxLines = 3,
                supportingText = {
                    Text(
                        text = "${noteInput.length}/${SmartBoardHelper.MAX_DAILY_NOTE_LENGTH}",
                        fontSize = 10.5.sp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("smart_board_daily_note_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { onSave(selectedLabel, noteInput) },
                    enabled = trimmedInput.isNotEmpty() && (!hasSavedNote || hasChanges),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("smart_board_daily_note_save_button"),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = amberAccent,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (hasSavedNote) {
                            if (isConnected) "Tahtada Güncelle" else "Notu Güncelle"
                        } else {
                            if (isConnected) "Tahtaya Sabitle" else "Notu Kaydet"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                if (hasSavedNote || trimmedInput.isNotEmpty()) {
                    OutlinedButton(
                        onClick = {
                            noteInput = ""
                            onClear()
                        },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("smart_board_daily_note_clear_button"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = BirizError
                        ),
                        border = BorderStroke(1.dp, BirizError.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Günün notunu kaldır",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Kaldır",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
