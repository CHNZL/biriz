package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Book
import com.example.data.model.LibraryBookRequest
import com.example.data.model.LibraryRequestStatus
import com.example.data.model.LibraryRequestType
import com.example.data.model.Student
import com.example.ui.theme.BirizAmber
import com.example.ui.theme.BirizBorder
import com.example.ui.theme.BirizDarkCardBorder
import com.example.ui.theme.BirizDarkCardGlass
import com.example.ui.theme.BirizDarkTextMuted
import com.example.ui.theme.BirizDarkTextPrimary
import com.example.ui.theme.BirizDarkTextSecondary
import com.example.ui.theme.BirizError
import com.example.ui.theme.BirizGold
import com.example.ui.theme.BirizNavyDark
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

private enum class LibraryRequestFilter(val label: String) {
    PENDING("Bekleyenler"),
    APPROVED("Onaylananlar"),
    REJECTED("Reddedilenler"),
    ALL("Tümü")
}

/**
 * Öğretmen Kitaplık > Veli Kitap İstekleri Onay Merkezi.
 * Velilerden gelen yeni kitap girişi / okumaya başlama ve kitap bitirme taleplerini
 * ayrıntılı inceleyip onaylama veya açıklama ile reddetme ekranı.
 */
@Composable
internal fun LibraryRequestsManagementView(
    requests: List<LibraryBookRequest>,
    books: List<Book>,
    students: List<Student>,
    isDarkTheme: Boolean,
    onApprove: (LibraryBookRequest) -> Unit,
    onReject: (LibraryBookRequest, String) -> Unit
) {
    var activeFilter by remember { mutableStateOf(LibraryRequestFilter.PENDING) }
    var requestToReject by remember { mutableStateOf<LibraryBookRequest?>(null) }

    val primaryText = if (isDarkTheme) BirizDarkTextPrimary else BirizTextPrimary
    val secondaryText = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
    val mutedText = if (isDarkTheme) BirizDarkTextMuted else Color(0xFF64748B)
    val cardBg = if (isDarkTheme) BirizDarkCardGlass else Color.White
    val cardBorder = if (isDarkTheme) BirizDarkCardBorder else BirizBorder

    val pendingCount = remember(requests) { requests.count { it.status == LibraryRequestStatus.PENDING } }
    val approvedCount = remember(requests) { requests.count { it.status == LibraryRequestStatus.APPROVED } }
    val rejectedCount = remember(requests) { requests.count { it.status == LibraryRequestStatus.REJECTED } }

    val filteredRequests = remember(requests, activeFilter) {
        val base = when (activeFilter) {
            LibraryRequestFilter.PENDING -> requests.filter { it.status == LibraryRequestStatus.PENDING }
            LibraryRequestFilter.APPROVED -> requests.filter { it.status == LibraryRequestStatus.APPROVED }
            LibraryRequestFilter.REJECTED -> requests.filter { it.status == LibraryRequestStatus.REJECTED }
            LibraryRequestFilter.ALL -> requests
        }
        base.sortedWith(
            compareBy<LibraryBookRequest> {
                when (it.status) {
                    LibraryRequestStatus.PENDING -> 0
                    LibraryRequestStatus.APPROVED -> 1
                    LibraryRequestStatus.REJECTED -> 2
                }
            }.thenByDescending { it.reviewedAt ?: it.createdAt }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("teacher_library_requests_root")
    ) {
        // 1. Özet Durum Kartları
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RequestSummaryChip(
                title = "Bekleyen",
                count = pendingCount,
                accentColor = BirizAmber,
                isDarkTheme = isDarkTheme,
                modifier = Modifier.weight(1f)
            )
            RequestSummaryChip(
                title = "Onaylanan",
                count = approvedCount,
                accentColor = BirizSuccess,
                isDarkTheme = isDarkTheme,
                modifier = Modifier.weight(1f)
            )
            RequestSummaryChip(
                title = "Reddedilen",
                count = rejectedCount,
                accentColor = BirizError,
                isDarkTheme = isDarkTheme,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Filtre Çipleri
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            LibraryRequestFilter.entries.forEach { filter ->
                val isSelected = activeFilter == filter
                val count = when (filter) {
                    LibraryRequestFilter.PENDING -> pendingCount
                    LibraryRequestFilter.APPROVED -> approvedCount
                    LibraryRequestFilter.REJECTED -> rejectedCount
                    LibraryRequestFilter.ALL -> requests.size
                }
                val accent = when (filter) {
                    LibraryRequestFilter.PENDING -> Color(0xFFD97706)
                    LibraryRequestFilter.APPROVED -> BirizSuccess
                    LibraryRequestFilter.REJECTED -> BirizError
                    LibraryRequestFilter.ALL -> if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                }

                Surface(
                    onClick = { activeFilter = filter },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) accent else if (isDarkTheme) Color(0x30334E68) else Color(0xFFF1F5F9),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) accent else if (isDarkTheme) Color(0x4064748B) else BirizBorder
                    ),
                    modifier = Modifier
                        .heightIn(min = 38.dp)
                        .testTag("library_request_filter_${filter.name.lowercase(Locale.ROOT)}")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = "${filter.label} ($count)",
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                            color = if (isSelected) Color.White else primaryText
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Talep Listesi
        if (filteredRequests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isDarkTheme) Color(0x3010B981) else Color(0xFFECFDF5),
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = null,
                                    tint = BirizSuccess,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = when (activeFilter) {
                                LibraryRequestFilter.PENDING -> "Onay bekleyen veli kitap isteği yok"
                                LibraryRequestFilter.APPROVED -> "Henüz onaylanan talep bulunmuyor"
                                LibraryRequestFilter.REJECTED -> "Reddedilen talep bulunmuyor"
                                LibraryRequestFilter.ALL -> "Henüz veli kitap isteği gönderilmedi"
                            },
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = primaryText,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = "Veliler kitaplık bölümünden yeni kitap girişi yaptığında veya 'Kitap Bitti' bildirdiğinde tüm talepler anında buraya düşer.",
                            fontSize = 12.sp,
                            color = secondaryText,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("teacher_library_requests_list"),
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredRequests, key = { it.id }) { req ->
                    val matchedBook = remember(books, req.existingBookId, req.bookName) {
                        books.firstOrNull { !req.existingBookId.isNullOrBlank() && it.id == req.existingBookId }
                            ?: LibraryRequestPersistenceHelper.findMatchingLibraryBook(books, req.bookName)
                    }
                    val matchedStudent = remember(students, req.studentId) {
                        students.firstOrNull { it.id == req.studentId }
                    }

                    TeacherLibraryRequestCard(
                        request = req,
                        matchedBook = matchedBook,
                        matchedStudent = matchedStudent,
                        isDarkTheme = isDarkTheme,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        mutedText = mutedText,
                        onApprove = { onApprove(req) },
                        onRejectClick = { requestToReject = req }
                    )
                }
            }
        }
    }

    if (requestToReject != null) {
        RejectLibraryRequestDialog(
            request = requestToReject!!,
            isDarkTheme = isDarkTheme,
            onDismiss = { requestToReject = null },
            onConfirmReject = { reason ->
                val target = requestToReject!!
                requestToReject = null
                onReject(target, reason)
            }
        )
    }
}

@Composable
private fun RequestSummaryChip(
    title: String,
    count: Int,
    accentColor: Color,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isDarkTheme) accentColor.copy(alpha = 0.16f) else accentColor.copy(alpha = 0.09f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = if (isDarkTheme) 0.45f else 0.28f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = if (isDarkTheme) accentColor.copy(alpha = 0.95f) else accentColor
            )
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
            )
        }
    }
}

@Composable
private fun TeacherLibraryRequestCard(
    request: LibraryBookRequest,
    matchedBook: Book?,
    matchedStudent: Student?,
    isDarkTheme: Boolean,
    cardBg: Color,
    cardBorder: Color,
    primaryText: Color,
    secondaryText: Color,
    mutedText: Color,
    onApprove: () -> Unit,
    onRejectClick: () -> Unit
) {
    val dateFmt = remember { SimpleDateFormat("dd.MM.yyyy", Locale("tr")) }
    val dateTimeFmt = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr")) }
    val isFinish = request.requestType == LibraryRequestType.FINISH_BOOK
    val isExistingLibraryBook = matchedBook != null || !request.existingBookId.isNullOrBlank()
    val resolvedStudentName = matchedStudent?.fullName?.takeIf { it.isNotBlank() } ?: request.studentName
    val resolvedStudentNumber = matchedStudent?.studentNumber?.takeIf { it.isNotBlank() } ?: request.studentNumber
    val isGirl = matchedStudent?.gender.equals("KIZ", ignoreCase = true)
    val resolvedPages = matchedBook?.pageCount?.takeIf { it > 0 } ?: request.pageCount
    val expectedStars = if (request.earnedStarsPreview > 0) {
        request.earnedStarsPreview
    } else {
        LibraryPersistenceHelper.calculateReadingStars(resolvedPages)
    }

    val typeBadgeColor = when {
        isFinish -> Color(0xFF059669)
        isExistingLibraryBook -> Color(0xFF0284C7)
        else -> Color(0xFF7C3AED)
    }
    val typeBadgeLabel = when {
        isFinish -> "🏁 Kitap Bitti Talebi"
        isExistingLibraryBook -> "📖 Kitaplıktan Okuma"
        else -> "📘 Yeni Kitap & Okuma"
    }

    val borderStrokeColor = when (request.status) {
        LibraryRequestStatus.PENDING -> typeBadgeColor.copy(alpha = if (isDarkTheme) 0.65f else 0.45f)
        LibraryRequestStatus.APPROVED -> BirizSuccess.copy(alpha = 0.35f)
        LibraryRequestStatus.REJECTED -> BirizError.copy(alpha = 0.35f)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(1.2.dp, borderStrokeColor),
        shadowElevation = if (request.status == LibraryRequestStatus.PENDING && !isDarkTheme) 2.dp else 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("teacher_library_request_card_${request.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Üst Satır: Talep Türü Rozeti + Durum Rozeti
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = typeBadgeColor.copy(alpha = if (isDarkTheme) 0.25f else 0.12f),
                    border = BorderStroke(0.8.dp, typeBadgeColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = typeBadgeLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDarkTheme) typeBadgeColor.copy(alpha = 0.95f) else typeBadgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                val (statusBg, statusFg, statusText) = when (request.status) {
                    LibraryRequestStatus.PENDING -> Triple(
                        if (isDarkTheme) Color(0x35F59E0B) else Color(0xFFFEF3C7),
                        if (isDarkTheme) Color(0xFFFCD34D) else Color(0xFFB45309),
                        "⏳ Onay Bekliyor"
                    )
                    LibraryRequestStatus.APPROVED -> Triple(
                        if (isDarkTheme) Color(0x3510B981) else Color(0xFFECFDF5),
                        if (isDarkTheme) Color(0xFF6EE7B7) else Color(0xFF047857),
                        "✅ Onaylandı"
                    )
                    LibraryRequestStatus.REJECTED -> Triple(
                        if (isDarkTheme) Color(0x35EF4444) else Color(0xFFFEF2F2),
                        if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFFB91C1C),
                        "❌ Reddedildi"
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusBg
                ) {
                    Text(
                        text = statusText,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusFg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Öğrenci & Veli Bilgisi
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isGirl) Color(0xFFFCE7F3) else Color(0xFFE0F2FE),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = resolvedStudentNumber.ifBlank { "•" },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isGirl) Color(0xFFDB2777) else Color(0xFF0284C7)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = resolvedStudentName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Gönderen Veli: ${request.parentName.ifBlank { "Veli" }} • ${dateTimeFmt.format(Date(request.createdAt))}",
                        fontSize = 11.sp,
                        color = secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Kitap Detay Kutusu
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDarkTheme) Color(0x281E293B) else Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(11.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
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
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = TurkishTextHelper.toTurkishTitleCase(request.bookName),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = primaryText,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (matchedBook != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isDarkTheme) Color(0x350284C7) else Color(0xFFE0F2FE)
                            ) {
                                Text(
                                    text = "Kitaplık #${matchedBook.registrationNo}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkTheme) Color(0xFF7DD3FC) else Color(0xFF0369A1),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (request.author.isNotBlank()) {
                        Text(
                            text = "Yazar: ${TurkishTextHelper.toTurkishTitleCase(request.author)}",
                            fontSize = 11.5.sp,
                            color = secondaryText
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val dateSummary = if (isFinish) {
                            val startText = if (request.startDate > 0L) dateFmt.format(Date(request.startDate)) else "-"
                            val endText = request.endDate?.takeIf { it > 0L }?.let { dateFmt.format(Date(it)) } ?: "-"
                            "📄 $resolvedPages s. • Başlama: $startText → Bitiş: $endText"
                        } else {
                            val startText = if (request.startDate > 0L) dateFmt.format(Date(request.startDate)) else "-"
                            "📄 $resolvedPages Sayfa • Başlama: $startText"
                        }
                        Text(
                            text = dateSummary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = secondaryText,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = BirizGold.copy(alpha = 0.18f),
                            border = BorderStroke(0.8.dp, BirizGold)
                        ) {
                            Text(
                                text = "+$expectedStars ⭐",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDarkTheme) Color(0xFFFCD34D) else Color(0xFFB45309),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Onaylandığında ne olacağını özetleyen mini açıklama (sadece bekleyenlerde)
                    if (request.status == LibraryRequestStatus.PENDING) {
                        val impactInfo = when {
                            isFinish -> "Onaylandığında okuma geçmişine eklenir ve öğrenciye +$expectedStars ⭐ Kitap Kurdu yıldızı verilir."
                            matchedBook != null -> "Kitap kütüphanede zaten kayıtlı. Onaylandığında belirtilen tarihte öğrencinin okuma kaydı başlar."
                            else -> "Onaylandığında kitap sınıf kitaplığına eklenir ve belirtilen tarihte öğrencinin okuma kaydı başlar."
                        }
                        Text(
                            text = "ℹ️ $impactInfo",
                            fontSize = 10.5.sp,
                            color = mutedText,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            // Reddedilme gerekçesi varsa göster
            if (request.status == LibraryRequestStatus.REJECTED && request.rejectionReason.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDarkTheme) Color(0x30DC2626) else Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, BirizError.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Reddetme Notu: ${request.rejectionReason}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFFB91C1C),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }
            }

            // Onay / Reddet İşlem Butonları (Bekleyen talepler için)
            if (request.status == LibraryRequestStatus.PENDING) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onRejectClick,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.2.dp, BirizError.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("reject_library_request_${request.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Reddet",
                            tint = BirizError,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Reddet",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = BirizError
                        )
                    }

                    Button(
                        onClick = onApprove,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BirizSuccess
                        ),
                        modifier = Modifier
                            .weight(1.35f)
                            .heightIn(min = 48.dp)
                            .testTag("approve_library_request_${request.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Onayla",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isFinish) "Onayla & Yıldız Ver" else "Onayla & Kaydet",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else if (request.reviewedAt != null && request.reviewedAt > 0L) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (request.status == LibraryRequestStatus.APPROVED) Icons.Default.CheckCircle else Icons.Default.Schedule,
                        contentDescription = null,
                        tint = mutedText,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "İşlem tarihi: ${dateTimeFmt.format(Date(request.reviewedAt))}",
                        fontSize = 10.5.sp,
                        color = mutedText
                    )
                }
            }
        }
    }
}

@Composable
private fun RejectLibraryRequestDialog(
    request: LibraryBookRequest,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onConfirmReject: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    val quickReasons = remember(request.requestType) {
        if (request.requestType == LibraryRequestType.FINISH_BOOK) {
            listOf(
                "Bitiş tarihi hatalı girilmiş",
                "Kitap henüz tamamlanmamış",
                "Öğrenciyle kontrol edilecek"
            )
        } else {
            listOf(
                "Sayfa sayısı hatalı girilmiş",
                "Kitap adı eksik veya belirsiz",
                "Başlama tarihi hatalı"
            )
        }
    }

    val primaryText = if (isDarkTheme) BirizDarkTextPrimary else BirizTextPrimary
    val secondaryText = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Talebi Reddet",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp,
                color = primaryText
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${request.studentName} için gönderilen \"${TurkishTextHelper.toTurkishTitleCase(request.bookName)}\" talebini reddetmek üzeresiniz. Veliye bildirim gidecektir.",
                    fontSize = 12.5.sp,
                    color = secondaryText
                )

                Text(
                    text = "Hızlı Gerekçe (İsteğe bağlı)",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryText
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    quickReasons.forEach { quick ->
                        val selected = reason == quick
                        Surface(
                            onClick = { reason = if (selected) "" else quick },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) {
                                if (isDarkTheme) Color(0x40EF4444) else Color(0xFFFEE2E2)
                            } else {
                                if (isDarkTheme) Color(0x28334E68) else Color(0xFFF8FAFC)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (selected) BirizError else if (isDarkTheme) BirizDarkCardBorder else BirizBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = quick,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) {
                                    if (isDarkTheme) Color(0xFFFCA5A5) else Color(0xFFB91C1C)
                                } else {
                                    primaryText
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = if (isDarkTheme) BirizDarkCardBorder else BirizBorder)

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Veliye Açıklama (İsteğe bağlı)", fontSize = 12.sp) },
                    placeholder = { Text("Örn: Sayfa sayısını kontrol edip tekrar gönderir misiniz?", fontSize = 11.5.sp) },
                    maxLines = 3,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BirizError,
                        unfocusedBorderColor = if (isDarkTheme) Color(0xFF334155) else Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reject_library_request_reason_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmReject(reason.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = BirizError),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("confirm_reject_library_request_button")
            ) {
                Text("Reddet ve Bildir", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Vazgeç", fontWeight = FontWeight.Bold, color = if (isDarkTheme) BirizDarkTextSecondary else BirizNavyDark)
            }
        }
    )
}
