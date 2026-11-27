package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PersonOutline
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Book
import com.example.data.model.LibraryBookRequest
import com.example.data.model.Student
import com.example.ui.theme.BirizAmber
import com.example.ui.theme.BirizDarkCardBorder
import com.example.ui.theme.BirizDarkTextPrimary
import com.example.ui.theme.BirizDarkTextSecondary
import com.example.ui.theme.BirizError
import com.example.ui.theme.BirizGold
import com.example.ui.theme.BirizNavyPrimary
import com.example.ui.theme.BirizSuccess
import com.example.ui.theme.BirizTextPrimary
import com.example.ui.theme.BirizTextSecondary
import com.example.ui.theme.BirizTurquoise
import com.example.util.LibraryPersistenceHelper
import com.example.util.LibraryRequestPersistenceHelper
import com.example.util.TurkishTextHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun parseTurkishDateToMillis(dateStr: String): Long? {
    val trimmed = dateStr.trim()
    if (trimmed.isBlank()) return null
    return try {
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale("tr")).apply { isLenient = false }
        sdf.parse(trimmed)?.time
    } catch (_: Exception) {
        null
    }
}

/**
 * Veli > Öğrenci Yeni Kitap Girişi & Okumaya Başlama Talebi Diyaloğu.
 * Kitap sınıf kitaplığında zaten kayıtlıysa adı, yazarı ve sayfa sayısı otomatik dolar
 * ve sayfa sayısı veli tarafından değiştirilemez (salt okunur / kilitli).
 */
@Composable
internal fun ParentAddBookRequestDialog(
    student: Student,
    classBooks: List<Book>,
    existingRequests: List<LibraryBookRequest>,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (bookName: String, author: String, pageCount: Int, startDateMs: Long) -> Unit
) {
    val todayDateStr = remember { SimpleDateFormat("dd.MM.yyyy", Locale("tr")).format(Date()) }
    var bookName by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var pageCountStr by remember { mutableStateOf("") }
    var startDateStr by remember { mutableStateOf(todayDateStr) }

    // Sınıf kitaplığında birebir eşleşen kayıtlı kitap var mı?
    val matchedBook = remember(bookName, classBooks) {
        LibraryRequestPersistenceHelper.findMatchingLibraryBook(classBooks, bookName)
    }

    // Eşleşen kitap değiştiğinde kayıtlı kitap bilgilerini otomatik doldur ve kilitle
    LaunchedEffect(matchedBook?.id) {
        if (matchedBook != null) {
            author = matchedBook.author
            pageCountStr = matchedBook.pageCount.toString()
        }
    }

    // Akıllı Tamamlama Önerileri (Kitaplıktaki kayıtlı kitaplardan)
    val bookSuggestions = remember(bookName, classBooks, matchedBook) {
        val query = bookName.trim()
        if (query.length < 2) emptyList() else {
            classBooks
                .filter { b ->
                    b.id != matchedBook?.id &&
                        TurkishTextHelper.toTurkishUpperCase(b.name).contains(TurkishTextHelper.toTurkishUpperCase(query))
                }
                .distinctBy { TurkishTextHelper.toTurkishUpperCase(it.name.trim()) }
                .take(4)
        }
    }

    val startDateMs = remember(startDateStr) { parseTurkishDateToMillis(startDateStr) }
    val effectivePageCount = matchedBook?.pageCount ?: (pageCountStr.toIntOrNull() ?: 0)
    val validation = remember(student.id, bookName, author, effectivePageCount, startDateMs, classBooks, existingRequests) {
        if (startDateMs == null) {
            null
        } else {
            LibraryRequestPersistenceHelper.validateStartBookRequest(
                studentId = student.id,
                rawBookName = bookName,
                rawAuthor = author,
                enteredPageCount = effectivePageCount,
                startDateMs = startDateMs,
                classBooks = classBooks,
                existingRequests = existingRequests
            )
        }
    }

    val canSubmit = validation?.isValid == true && startDateMs != null
    val primaryText = if (isDarkTheme) BirizDarkTextPrimary else BirizTextPrimary
    val secondaryText = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            val maxDialogHeight = maxHeight * 0.90f

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxDialogHeight),
                shape = RoundedCornerShape(22.dp),
                color = if (isDarkTheme) Color(0xFF0F1E38) else Color.White,
                border = BorderStroke(1.2.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFFE2E8F0)),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    // Başlık
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDarkTheme) Color(0x357C3AED) else Color(0xFFF3E8FF),
                                border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.5f)),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = if (isDarkTheme) Color(0xFFC4B5FD) else Color(0xFF7C3AED),
                                        modifier = Modifier.size(21.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Yeni Kitap Girişi",
                                    fontSize = 16.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = primaryText
                                )
                                Text(
                                    text = "${student.fullName} için öğretmene onay talebi gönderin",
                                    fontSize = 11.sp,
                                    color = secondaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Kapat",
                                tint = secondaryText
                            )
                        }
                    }

                    HorizontalDivider(
                        color = if (isDarkTheme) Color(0x3064748B) else Color(0xFFF1F5F9),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )

                    // Form Alanları
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. Kitap Adı + Kitaplık Önerileri
                        Column {
                            OutlinedTextField(
                                value = bookName,
                                onValueChange = { bookName = it },
                                label = { Text("Kitabın Adı * (Zorunlu)", fontWeight = FontWeight.SemiBold) },
                                placeholder = { Text("Örn: Küçük Prens, Şeker Portakalı", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.Book, contentDescription = null, tint = BirizTurquoise, modifier = Modifier.size(19.dp))
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(11.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BirizTurquoise,
                                    unfocusedBorderColor = if (isDarkTheme) Color(0xFF334155) else Color(0xFFCBD5E1),
                                    focusedLabelColor = BirizTurquoise
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("parent_book_request_name_input")
                            )

                            // Kitaplıktan Akıllı Öneriler
                            if (bookSuggestions.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(5.dp))
                                Text(
                                    text = "Sınıf kitaplığında eşleşen kayıtlar:",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = secondaryText,
                                    modifier = Modifier.padding(start = 4.dp, bottom = 3.dp)
                                )
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    bookSuggestions.forEach { suggestedBook ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (isDarkTheme) Color(0x300284C7) else Color(0xFFE0F2FE),
                                            border = BorderStroke(1.dp, if (isDarkTheme) Color(0x5538BDF8) else Color(0xFF7DD3FC)),
                                            modifier = Modifier.clickable {
                                                bookName = suggestedBook.name
                                                author = suggestedBook.author
                                                pageCountStr = suggestedBook.pageCount.toString()
                                            }
                                        ) {
                                            Text(
                                                text = "💡 ${suggestedBook.name} • ${suggestedBook.pageCount} s.",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isDarkTheme) Color(0xFF7DD3FC) else Color(0xFF0369A1),
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Kitap Kütüphanede Kayıtlı Bilgilendirme Bandı
                            if (matchedBook != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isDarkTheme) Color(0x300D9488) else Color(0xFFF0FDFA),
                                    border = BorderStroke(1.dp, BirizTurquoise.copy(alpha = 0.7f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Kitaplıktan kilitli bilgi",
                                            tint = if (isDarkTheme) BirizTurquoise else Color(0xFF0F766E),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Bu kitap sınıf kitaplığında kayıtlı (#${matchedBook.registrationNo}). Sayfa sayısı kitaplıktan otomatik alındı ve değiştirilemez.",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isDarkTheme) Color(0xFF99F6E4) else Color(0xFF115E59),
                                            lineHeight = 14.5.sp
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Sayfa Sayısı (Kayıtlı kitapta KİLİTLİ / Salt Okunur!)
                        val isLockedFromLibrary = matchedBook != null
                        Column {
                            OutlinedTextField(
                                value = if (isLockedFromLibrary) matchedBook!!.pageCount.toString() else pageCountStr,
                                onValueChange = { input ->
                                    if (!isLockedFromLibrary) {
                                        pageCountStr = input.filter { ch -> ch.isDigit() }
                                    }
                                },
                                readOnly = isLockedFromLibrary,
                                label = {
                                    Text(
                                        text = if (isLockedFromLibrary) "Sayfa Sayısı (Kitaplıktan Kilitli)" else "Sayfa Sayısı * (Zorunlu)",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                },
                                placeholder = { Text("Örn: 80", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.AutoStories, contentDescription = null, tint = BirizAmber, modifier = Modifier.size(19.dp))
                                },
                                trailingIcon = if (isLockedFromLibrary) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Değiştirilemez",
                                            tint = if (isDarkTheme) BirizTurquoise else Color(0xFF0F766E),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                } else null,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(11.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = if (isLockedFromLibrary) BirizTurquoise else BirizTurquoise,
                                    unfocusedBorderColor = if (isLockedFromLibrary) BirizTurquoise.copy(alpha = 0.5f) else if (isDarkTheme) Color(0xFF334155) else Color(0xFFCBD5E1),
                                    unfocusedContainerColor = if (isLockedFromLibrary) (if (isDarkTheme) Color(0x220D9488) else Color(0xFFF0FDFA)) else Color.Transparent,
                                    focusedContainerColor = if (isLockedFromLibrary) (if (isDarkTheme) Color(0x220D9488) else Color(0xFFF0FDFA)) else Color.Transparent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("parent_book_request_pages_input")
                            )

                            if (effectivePageCount > 0) {
                                val stars = LibraryPersistenceHelper.calculateReadingStars(effectivePageCount)
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isDarkTheme) Color(0x30F59E0B) else Color(0xFFFFFBEB),
                                    border = BorderStroke(1.dp, if (isDarkTheme) Color(0x55F59E0B) else Color(0xFFFDE68A)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "⭐ Bu kitap tamamlandığında +$stars Kitap Kurdu yıldızı kazandırır.",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkTheme) Color(0xFFFDE68A) else Color(0xFFB45309),
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }

                        // 3. Yazar Adı (İsteğe bağlı; kayıtlı kitapta kilitli)
                        OutlinedTextField(
                            value = if (isLockedFromLibrary) matchedBook!!.author else author,
                            onValueChange = { if (!isLockedFromLibrary) author = it },
                            readOnly = isLockedFromLibrary,
                            label = {
                                Text(
                                    text = if (isLockedFromLibrary) "Yazar (Kitaplıktan Alındı)" else "Yazar Adı (İsteğe bağlı)",
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            placeholder = { Text("Örn: Antoine de Saint-Exupéry", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.PersonOutline, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(19.dp))
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(11.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BirizTurquoise,
                                unfocusedBorderColor = if (isDarkTheme) Color(0xFF334155) else Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 4. Okumaya Başlama Tarihi
                        BirizDatePickerField(
                            label = "Okumaya Başlama Tarihi *",
                            dateStr = startDateStr,
                            onDateChange = { startDateStr = it },
                            isDarkTheme = isDarkTheme
                        )

                        // Doğrulama Hata Mesajı (varsa ve kullanıcı giriş yaptıysa)
                        val showError = when {
                            startDateMs == null -> "Lütfen gg.aa.yyyy biçiminde geçerli bir başlama tarihi seçin."
                            bookName.isNotBlank() && validation != null && !validation.isValid &&
                                (matchedBook != null || pageCountStr.isNotBlank()) -> validation.errorMessage
                            else -> null
                        }
                        if (!showError.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDarkTheme) Color(0x35DC2626) else Color(0xFFFEF2F2),
                                border = BorderStroke(1.dp, BirizError.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "⚠️ $showError",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFFB91C1C),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDarkTheme) Color(0x251E293B) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isLockedFromLibrary) {
                                        "ℹ️ Öğretmeniniz onayladığında öğrencinizin okuma kaydı seçtiğiniz başlama tarihiyle açılacaktır."
                                    } else {
                                        "ℹ️ Öğretmeniniz onayladığında bu kitap sınıf kitaplığına eklenecek ve öğrencinizin okuma kaydı başlayacaktır."
                                    },
                                    fontSize = 11.sp,
                                    color = secondaryText,
                                    lineHeight = 15.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        color = if (isDarkTheme) Color(0x3064748B) else Color(0xFFF1F5F9),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )

                    // Alt Butonlar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Text("Vazgeç", fontWeight = FontWeight.Bold, color = secondaryText)
                        }

                        Button(
                            onClick = {
                                if (canSubmit && startDateMs != null) {
                                    onSubmit(
                                        matchedBook?.name ?: bookName.trim(),
                                        matchedBook?.author ?: author.trim(),
                                        effectivePageCount,
                                        startDateMs
                                    )
                                }
                            },
                            enabled = canSubmit,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                                disabledContainerColor = if (isDarkTheme) Color(0xFF334155) else Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier
                                .weight(1.5f)
                                .heightIn(min = 48.dp)
                                .testTag("parent_submit_book_request_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Öğretmene Gönder",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Veli > Okumakta Olunan Kitap İçin 'Kitap Bitti' Talebi Diyaloğu.
 * Bitiş tarihini seçtirir, başlama tarihinden önce olmasını engeller ve öğretmene onay talebi gönderir.
 */
@Composable
internal fun ParentFinishBookRequestDialog(
    student: Student,
    book: Book,
    startDateMs: Long,
    existingRequests: List<LibraryBookRequest>,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (endDateMs: Long) -> Unit
) {
    val dateFmt = remember { SimpleDateFormat("dd.MM.yyyy", Locale("tr")) }
    val todayDateStr = remember { dateFmt.format(Date()) }
    var endDateStr by remember { mutableStateOf(todayDateStr) }

    val endDateMs = remember(endDateStr) { parseTurkishDateToMillis(endDateStr) }
    val validation = remember(student.id, book.id, startDateMs, endDateMs, existingRequests) {
        if (endDateMs == null) null else {
            LibraryRequestPersistenceHelper.validateFinishBookRequest(
                studentId = student.id,
                book = book,
                startDateMs = startDateMs,
                endDateMs = endDateMs,
                existingRequests = existingRequests
            )
        }
    }

    val expectedStars = remember(book.pageCount) {
        LibraryPersistenceHelper.calculateReadingStars(book.pageCount)
    }
    val canSubmit = validation?.isValid == true && endDateMs != null
    val primaryText = if (isDarkTheme) BirizDarkTextPrimary else BirizTextPrimary
    val secondaryText = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight * 0.85f),
                shape = RoundedCornerShape(22.dp),
                color = if (isDarkTheme) Color(0xFF0F1E38) else Color.White,
                border = BorderStroke(1.2.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFFE2E8F0)),
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Başlık
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDarkTheme) Color(0x3510B981) else Color(0xFFECFDF5),
                                border = BorderStroke(1.dp, BirizSuccess.copy(alpha = 0.5f)),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = BirizSuccess,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Kitap Bitti Bildirimi",
                                    fontSize = 16.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = primaryText
                                )
                                Text(
                                    text = "Okuma bitiş tarihini girip öğretmene gönderin",
                                    fontSize = 11.sp,
                                    color = secondaryText
                                )
                            }
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Kapat", tint = secondaryText)
                        }
                    }

                    // Kitap Özet Kartı
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDarkTheme) Color(0x281E293B) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = TurkishTextHelper.toTurkishTitleCase(book.name),
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = primaryText
                            )
                            Text(
                                text = "📄 ${book.pageCount} Sayfa • Başlama: ${dateFmt.format(Date(startDateMs))}",
                                fontSize = 11.5.sp,
                                color = secondaryText
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = BirizGold.copy(alpha = 0.18f),
                                border = BorderStroke(0.8.dp, BirizGold)
                            ) {
                                Text(
                                    text = "⭐ Öğretmen onaylayınca +$expectedStars Kitap Kurdu yıldızı eklenecek",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkTheme) Color(0xFFFCD34D) else Color(0xFFB45309),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Bitiş Tarihi Seçici
                    BirizDatePickerField(
                        label = "Okuma Bitiş Tarihi *",
                        dateStr = endDateStr,
                        onDateChange = { endDateStr = it },
                        isDarkTheme = isDarkTheme
                    )

                    val errorText = when {
                        endDateMs == null -> "Lütfen gg.aa.yyyy biçiminde geçerli bir bitiş tarihi girin."
                        validation != null && !validation.isValid -> validation.errorMessage
                        else -> null
                    }
                    if (!errorText.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isDarkTheme) Color(0x35DC2626) else Color(0xFFFEF2F2),
                            border = BorderStroke(1.dp, BirizError.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "⚠️ $errorText",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFFB91C1C),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }
                    }

                    // Butonlar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Text("Vazgeç", fontWeight = FontWeight.Bold, color = secondaryText)
                        }

                        Button(
                            onClick = {
                                if (canSubmit && endDateMs != null) {
                                    onSubmit(endDateMs)
                                }
                            },
                            enabled = canSubmit,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BirizSuccess,
                                disabledContainerColor = if (isDarkTheme) Color(0xFF334155) else Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier
                                .weight(1.5f)
                                .heightIn(min = 48.dp)
                                .testTag("parent_submit_finish_request_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Öğretmene Gönder",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
