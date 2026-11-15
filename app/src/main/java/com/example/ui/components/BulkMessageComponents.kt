package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ChatMessage
import com.example.data.model.Student
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
import com.example.util.BulkMessageHelper
import com.example.util.BulkMessageRecipient
import com.example.util.ChatEmojiCategory
import com.example.util.ChatEmojiHelper

/**
 * Öğretmen Modu: Tüm Velilere veya Seçili Birden Fazla Veliye Toplu Mesaj Penceresi.
 * - Her veliye kendi özel sohbeti üzerinden gizlilik (KVKK) korunarak iletilir.
 * - {veli} ve {ogrenci} yer tutucuları ile kişisel hitap, emoji çekmecesi ve zamanlanmış iletim destekler.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BulkMessageDialog(
    students: List<Student>,
    scheduledMessages: List<ChatMessage> = emptyList(),
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onSendBulk: (recipients: List<BulkMessageRecipient>, text: String, scheduledFor: Long?) -> Unit,
    onUpdateScheduledMessage: (messageId: String, newText: String, newScheduledFor: Long) -> Unit = { _, _, _ -> },
    onDeleteScheduledMessage: (messageId: String) -> Unit = {},
    onDeliverScheduledNow: (messageId: String) -> Unit = {}
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val primaryText = if (isDarkTheme) BirizDarkTextPrimary else BirizTextPrimary
    val secondaryText = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
    val mutedText = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted
    val cardBg = if (isDarkTheme) Color(0xFF1F2C34) else Color.White
    val surfaceBg = if (isDarkTheme) Color(0xFF182229) else Color(0xFFF8FAFC)
    val borderColor = if (isDarkTheme) BirizDarkCardBorder else BirizBorder

    val allRecipients = remember(students) { BulkMessageHelper.collectAllLinkedRecipients(students) }
    val primaryRecipients = remember(students) { BulkMessageHelper.collectPrimaryParentRecipients(students) }
    val unlinkedStudentCount = remember(students) { students.count { !it.isParentLinked } }

    // Varsayılan: 0 = Alıcı Seçimi, 1 = Mesaj Yaz & Gönder
    var activeStep by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedKeys by remember {
        mutableStateOf(allRecipients.map { it.key }.toSet())
    }
    var messageValue by remember { mutableStateOf(TextFieldValue("")) }
    var isEmojiDrawerOpen by remember { mutableStateOf(false) }
    var selectedEmojiCategory by remember { mutableStateOf(ChatEmojiCategory.SMILEYS) }
    var recentEmojis by remember { mutableStateOf(ChatEmojiHelper.loadRecentEmojis(context)) }
    var showBulkScheduleDialog by remember { mutableStateOf(false) }

    val selectedRecipients = remember(allRecipients, selectedKeys) {
        allRecipients.filter { it.key in selectedKeys }
    }
    val filteredStudents = remember(students, searchQuery) {
        BulkMessageHelper.filterLinkedStudents(students, searchQuery)
    }

    val insertTokenAtCursor: (String) -> Unit = { token ->
        val start = messageValue.selection.min.coerceIn(0, messageValue.text.length)
        val end = messageValue.selection.max.coerceIn(0, messageValue.text.length)
        val updatedText = buildString {
            append(messageValue.text.substring(0, start))
            append(token)
            append(messageValue.text.substring(end))
        }
        val nextCursor = start + token.length
        messageValue = TextFieldValue(
            text = updatedText,
            selection = TextRange(nextCursor)
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = cardBg,
            border = BorderStroke(1.dp, borderColor),
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .widthIn(max = 560.dp)
                .heightIn(min = 420.dp, max = 680.dp)
                .testTag("dialog_bulk_message")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Üst Başlık
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isDarkTheme) Color(0x3028B8C2) else Color(0xFFE0F2FE),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = null,
                                tint = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Toplu Mesaj Gönder",
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = primaryText
                        )
                        Text(
                            text = "Her veliye kendi özel sohbetinden KVKK uyumlu iletilir",
                            fontSize = 11.sp,
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

                // Adım Göstergesi (1. Alıcılar <-> 2. Mesaj Yaz)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BulkStepTabChip(
                        stepNumber = "1",
                        label = "Alıcılar (${selectedRecipients.size})",
                        active = activeStep == 0,
                        isDarkTheme = isDarkTheme,
                        primaryText = primaryText,
                        onClick = {
                            isEmojiDrawerOpen = false
                            activeStep = 0
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_bulk_step_recipients")
                    )
                    BulkStepTabChip(
                        stepNumber = "2",
                        label = "Mesaj Yaz",
                        active = activeStep == 1,
                        enabled = selectedRecipients.isNotEmpty(),
                        isDarkTheme = isDarkTheme,
                        primaryText = primaryText,
                        onClick = {
                            if (selectedRecipients.isNotEmpty()) {
                                activeStep = 1
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_bulk_step_compose")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = borderColor)

                if (activeStep == 0) {
                    // --- ADIM 1: ALICI SEÇİMİ ---
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (allRecipients.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = surfaceBg,
                                border = BorderStroke(0.8.dp, borderColor),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = mutedText,
                                        modifier = Modifier.size(34.dp)
                                    )
                                    Text(
                                        text = "Henüz sınıfa bağlanmış veli hesabı bulunmuyor.",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryText,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "Veliler katılım kodlarıyla sınıfa bağlandığında toplu mesaj gönderebilirsiniz.",
                                        fontSize = 11.5.sp,
                                        color = secondaryText,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            // Hızlı Seçim Çipleri
                            val allSelected = selectedKeys.size == allRecipients.size && allRecipients.isNotEmpty()
                            val primaryKeys = remember(primaryRecipients) { primaryRecipients.map { it.key }.toSet() }
                            val primaryOnlySelected = selectedKeys == primaryKeys && primaryKeys.isNotEmpty()

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                BulkPresetChip(
                                    label = "Tüm Veliler (${allRecipients.size})",
                                    selected = allSelected,
                                    isDarkTheme = isDarkTheme,
                                    testTag = "btn_bulk_select_all",
                                    onClick = {
                                        selectedKeys = allRecipients.map { it.key }.toSet()
                                    }
                                )
                                if (primaryRecipients.size < allRecipients.size) {
                                    BulkPresetChip(
                                        label = "Öğrenci Başına 1 Veli (${primaryRecipients.size})",
                                        selected = primaryOnlySelected,
                                        isDarkTheme = isDarkTheme,
                                        testTag = "btn_bulk_select_primary",
                                        onClick = {
                                            selectedKeys = primaryKeys
                                        }
                                    )
                                }
                                BulkPresetChip(
                                    label = "Seçimi Temizle",
                                    selected = false,
                                    enabled = selectedKeys.isNotEmpty(),
                                    isDarkTheme = isDarkTheme,
                                    testTag = "btn_bulk_clear_selection",
                                    onClick = {
                                        selectedKeys = emptySet()
                                    }
                                )
                            }

                            // Arama Alanı
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text("Öğrenci adı, no veya veli ara...", fontSize = 12.sp)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = BirizTurquoise,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                trailingIcon = if (searchQuery.isNotEmpty()) {
                                    {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Aramayı temizle",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                } else null,
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BirizTurquoise,
                                    unfocusedBorderColor = borderColor
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_bulk_search_recipients")
                            )

                            // Öğrenci & Veli Listesi
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 310.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 4.dp)
                            ) {
                                if (filteredStudents.isEmpty()) {
                                    item {
                                        Text(
                                            text = "Aramayla eşleşen bağlı veli bulunamadı.",
                                            fontSize = 12.sp,
                                            color = mutedText,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 20.dp)
                                        )
                                    }
                                } else {
                                    items(filteredStudents, key = { it.id }) { student ->
                                        val studentParents = student.allLinkedParents
                                        val studentKeys = studentParents.map {
                                            BulkMessageHelper.recipientKey(student.id, it.email)
                                        }
                                        val selectedForStudentCount = studentKeys.count { it in selectedKeys }
                                        val allStudentParentsSelected =
                                            studentKeys.isNotEmpty() && selectedForStudentCount == studentKeys.size

                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (selectedForStudentCount > 0) {
                                                if (isDarkTheme) Color(0xFF10313A) else Color(0xFFF0FDFA)
                                            } else {
                                                surfaceBg
                                            },
                                            border = BorderStroke(
                                                width = if (selectedForStudentCount > 0) 1.2.dp else 0.8.dp,
                                                color = if (selectedForStudentCount > 0) Color(0xFF00A884) else borderColor
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedKeys = if (allStudentParentsSelected) {
                                                        selectedKeys - studentKeys.toSet()
                                                    } else {
                                                        selectedKeys + studentKeys
                                                    }
                                                }
                                                .testTag("bulk_student_card_${student.id}")
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Surface(
                                                        shape = CircleShape,
                                                        color = if (isDarkTheme) Color(0x3028B8C2) else Color(0xFFE0F2FE),
                                                        modifier = Modifier.size(30.dp)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Text(
                                                                text = student.studentNumber,
                                                                fontSize = 10.5.sp,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = student.fullName,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = primaryText,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Text(
                                                        text = "$selectedForStudentCount/${studentKeys.size} Seçili",
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (selectedForStudentCount > 0) Color(0xFF00A884) else mutedText
                                                    )
                                                }

                                                FlowRow(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    studentParents.forEach { parent ->
                                                        val key = BulkMessageHelper.recipientKey(student.id, parent.email)
                                                        val isChecked = key in selectedKeys
                                                        Surface(
                                                            onClick = {
                                                                selectedKeys = if (isChecked) {
                                                                    selectedKeys - key
                                                                } else {
                                                                    selectedKeys + key
                                                                }
                                                            },
                                                            shape = RoundedCornerShape(10.dp),
                                                            color = if (isChecked) {
                                                                Color(0xFF00A884)
                                                            } else if (isDarkTheme) {
                                                                Color(0xFF1F2C34)
                                                            } else {
                                                                Color.White
                                                            },
                                                            border = BorderStroke(
                                                                0.9.dp,
                                                                if (isChecked) Color(0xFF00A884) else borderColor
                                                            ),
                                                            modifier = Modifier
                                                                .minimumInteractiveComponentSize()
                                                                .heightIn(min = 38.dp)
                                                                .testTag("bulk_parent_chip_$key")
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = if (isChecked) Icons.Default.CheckCircle else Icons.Default.Person,
                                                                    contentDescription = null,
                                                                    tint = if (isChecked) Color.White else secondaryText,
                                                                    modifier = Modifier.size(14.dp)
                                                                )
                                                                Text(
                                                                    text = "${parent.relationship}: ${parent.name.ifBlank { parent.email.substringBefore("@") }}",
                                                                    fontSize = 11.5.sp,
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    color = if (isChecked) Color.White else primaryText,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
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

                            if (unlinkedStudentCount > 0) {
                                Text(
                                    text = "ℹ️ Henüz velisi bağlanmamış $unlinkedStudentCount öğrenci listelenmedi.",
                                    fontSize = 10.5.sp,
                                    color = mutedText
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = borderColor)

                    // Adım 1 Alt Çubuk
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
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
                            onClick = { activeStep = 1 },
                            enabled = selectedRecipients.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00A884),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("btn_bulk_next_to_compose")
                        ) {
                            Text(
                                text = "Mesaj Yaz (${selectedRecipients.size})",
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else {
                    // --- ADIM 2: MESAJ YAZ & GÖNDER ---
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Seçili Alıcı Özeti
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isDarkTheme) Color(0xFF10313A) else Color(0xFFECFDF5),
                            border = BorderStroke(0.9.dp, Color(0xFF00A884)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DoneAll,
                                        contentDescription = null,
                                        tint = Color(0xFF00A884),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "${selectedRecipients.size} Veli Hesabına Gönderilecek",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = primaryText
                                        )
                                        Text(
                                            text = selectedRecipients.take(3).joinToString(", ") {
                                                it.parent.name.ifBlank { it.student.fullName }
                                            } + if (selectedRecipients.size > 3) " ve +${selectedRecipients.size - 3} kişi" else "",
                                            fontSize = 10.5.sp,
                                            color = secondaryText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                TextButton(
                                    onClick = {
                                        isEmojiDrawerOpen = false
                                        activeStep = 0
                                    },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Text("Değiştir", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Kişiselleştirme Etiketleri
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Otomatik İsim:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = secondaryText
                            )
                            Surface(
                                onClick = { insertTokenAtCursor("{veli}") },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDarkTheme) Color(0x2838BDF8) else Color(0xFFE0F2FE),
                                border = BorderStroke(0.8.dp, if (isDarkTheme) BirizTurquoise else Color(0xFF93C5FD)),
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Text(
                                    text = "+ {veli}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkTheme) BirizTurquoise else Color(0xFF0369A1),
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }
                            Surface(
                                onClick = { insertTokenAtCursor("{ogrenci}") },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDarkTheme) Color(0x2838BDF8) else Color(0xFFE0F2FE),
                                border = BorderStroke(0.8.dp, if (isDarkTheme) BirizTurquoise else Color(0xFF93C5FD)),
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Text(
                                    text = "+ {ogrenci}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkTheme) BirizTurquoise else Color(0xFF0369A1),
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }
                        }

                        // Mesaj Giriş Kutusu + Emoji Düğmesi
                        OutlinedTextField(
                            value = messageValue,
                            onValueChange = { messageValue = it },
                            label = { Text("Toplu Mesajınız", fontSize = 12.sp) },
                            placeholder = {
                                Text(
                                    text = "Örn: Sayın {veli}, {ogrenci} öğrencimiz için yarın...",
                                    fontSize = 12.5.sp
                                )
                            },
                            leadingIcon = {
                                IconButton(
                                    onClick = {
                                        if (isEmojiDrawerOpen) {
                                            isEmojiDrawerOpen = false
                                            keyboardController?.show()
                                        } else {
                                            keyboardController?.hide()
                                            isEmojiDrawerOpen = true
                                        }
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("btn_bulk_emoji_toggle")
                                ) {
                                    Icon(
                                        imageVector = if (isEmojiDrawerOpen) Icons.Default.Keyboard else Icons.Default.SentimentSatisfiedAlt,
                                        contentDescription = if (isEmojiDrawerOpen) "Klavyeye geç" else "Emoji ekle",
                                        tint = if (isEmojiDrawerOpen) Color(0xFF00A884) else secondaryText
                                    )
                                }
                            },
                            minLines = 3,
                            maxLines = 6,
                            shape = RoundedCornerShape(14.dp),
                            textStyle = TextStyle(fontSize = 13.5.sp, color = primaryText),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00A884),
                                unfocusedBorderColor = borderColor,
                                focusedLabelColor = Color(0xFF00A884),
                                cursorColor = Color(0xFF00A884)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_bulk_message_text")
                        )

                        // Emoji Seçici (WhatsApp Stili Kompakt Çekmece)
                        AnimatedVisibility(
                            visible = isEmojiDrawerOpen,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = surfaceBg,
                                border = BorderStroke(0.8.dp, borderColor),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        ChatEmojiCategory.entries.forEach { cat ->
                                            val active = selectedEmojiCategory == cat
                                            Surface(
                                                onClick = { selectedEmojiCategory = cat },
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (active) Color(0x2800A884) else Color.Transparent,
                                                modifier = Modifier.minimumInteractiveComponentSize()
                                            ) {
                                                Text(
                                                    text = cat.icon,
                                                    fontSize = 18.sp,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val emojis = if (selectedEmojiCategory == ChatEmojiCategory.RECENT) {
                                        recentEmojis
                                    } else {
                                        ChatEmojiHelper.categoryEmojis[selectedEmojiCategory].orEmpty()
                                    }
                                    LazyVerticalGrid(
                                        columns = GridCells.Adaptive(minSize = 40.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(132.dp)
                                    ) {
                                        items(emojis) { emoji ->
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clickable {
                                                        val (nextText, nextCursor) = ChatEmojiHelper.insertEmojiAtSelection(
                                                            text = messageValue.text,
                                                            selectionStart = messageValue.selection.start,
                                                            selectionEnd = messageValue.selection.end,
                                                            emoji = emoji
                                                        )
                                                        messageValue = TextFieldValue(
                                                            text = nextText,
                                                            selection = TextRange(nextCursor)
                                                        )
                                                        recentEmojis = ChatEmojiHelper.recordRecentEmoji(context, emoji)
                                                    }
                                            ) {
                                                Text(text = emoji, fontSize = 21.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Etiket Kullanıldıysa Canlı Kişiselleştirme Önizlemesi
                        val hasTokens = messageValue.text.contains("{veli}", ignoreCase = true) ||
                            messageValue.text.contains("{ogrenci}", ignoreCase = true) ||
                            messageValue.text.contains("{öğrenci}", ignoreCase = true)
                        val firstSample = selectedRecipients.firstOrNull()
                        if (hasTokens && firstSample != null && messageValue.text.isNotBlank()) {
                            val previewText = BulkMessageHelper.personalizeBulkMessageText(
                                template = messageValue.text,
                                studentName = firstSample.student.fullName,
                                parentName = firstSample.parent.name
                            )
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = surfaceBg,
                                border = BorderStroke(0.8.dp, borderColor),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = "Önizleme (${firstSample.student.fullName} • ${firstSample.parent.name.ifBlank { firstSample.parent.email }})",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00A884)
                                    )
                                    Text(
                                        text = previewText,
                                        fontSize = 12.sp,
                                        color = primaryText
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = borderColor)

                    val canSendBulk = selectedRecipients.isNotEmpty() && messageValue.text.isNotBlank()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                isEmojiDrawerOpen = false
                                activeStep = 0
                            },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Geri")
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    keyboardController?.hide()
                                    isEmojiDrawerOpen = false
                                    showBulkScheduleDialog = true
                                },
                                enabled = canSendBulk,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_bulk_schedule_send")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Zamanla", fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = {
                                    if (canSendBulk) {
                                        onSendBulk(selectedRecipients, messageValue.text.trim(), null)
                                        onDismiss()
                                    }
                                },
                                enabled = canSendBulk,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00A884),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("btn_bulk_send_now")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Gönder (${selectedRecipients.size})", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBulkScheduleDialog) {
        ScheduleDeliveryDialog(
            initialMessageText = messageValue.text.trim(),
            scheduledMessages = scheduledMessages,
            isDarkTheme = isDarkTheme,
            onDismiss = { showBulkScheduleDialog = false },
            onSendNow = { finalText ->
                val clean = finalText.trim()
                if (clean.isNotEmpty() && selectedRecipients.isNotEmpty()) {
                    onSendBulk(selectedRecipients, clean, null)
                    showBulkScheduleDialog = false
                    onDismiss()
                }
            },
            onScheduleAt = { finalText, targetMillis ->
                val clean = finalText.trim()
                if (clean.isNotEmpty() && selectedRecipients.isNotEmpty()) {
                    onSendBulk(selectedRecipients, clean, targetMillis)
                    showBulkScheduleDialog = false
                    onDismiss()
                }
            },
            onUpdateScheduledMessage = onUpdateScheduledMessage,
            onDeleteScheduledMessage = onDeleteScheduledMessage,
            onDeliverScheduledNow = onDeliverScheduledNow
        )
    }
}

@Composable
private fun BulkStepTabChip(
    stepNumber: String,
    label: String,
    active: Boolean,
    enabled: Boolean = true,
    isDarkTheme: Boolean,
    primaryText: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        color = when {
            active -> if (isDarkTheme) Color(0xFF005C4B) else Color(0xFFECFDF5)
            isDarkTheme -> Color(0xFF182229)
            else -> Color(0xFFF1F5F9)
        },
        border = BorderStroke(
            width = if (active) 1.3.dp else 0.8.dp,
            color = if (active) Color(0xFF00A884) else if (isDarkTheme) BirizDarkCardBorder else BirizBorder
        ),
        modifier = modifier.heightIn(min = 44.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (active) Color(0xFF00A884) else if (isDarkTheme) Color(0xFF334155) else Color(0xFFCBD5E1),
                modifier = Modifier.size(20.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = stepNumber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                color = if (enabled) primaryText else primaryText.copy(alpha = 0.45f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BulkPresetChip(
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    isDarkTheme: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(20.dp),
        color = when {
            selected -> Color(0xFF00A884)
            isDarkTheme -> Color(0xFF182229)
            else -> Color(0xFFF1F5F9)
        },
        border = BorderStroke(
            1.dp,
            if (selected) Color(0xFF00A884) else if (isDarkTheme) BirizDarkCardBorder else BirizBorder
        ),
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = label,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = when {
                    !enabled -> if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted
                    selected -> Color.White
                    isDarkTheme -> BirizDarkTextPrimary
                    else -> BirizTextPrimary
                }
            )
        }
    }
}
