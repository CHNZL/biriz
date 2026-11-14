package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Book
import com.example.data.model.ReadingRecord
import com.example.data.model.Student
import com.example.ui.theme.BirizAmber
import com.example.ui.theme.BirizBorder
import com.example.ui.theme.BirizDarkCardBorder
import com.example.ui.theme.BirizDarkCardGlass
import com.example.ui.theme.BirizDarkTextMuted
import com.example.ui.theme.BirizDarkTextPrimary
import com.example.ui.theme.BirizDarkTextSecondary
import com.example.ui.theme.BirizGold
import com.example.ui.theme.BirizNavyDark
import com.example.ui.theme.BirizNavyPrimary
import com.example.ui.theme.BirizTextMuted
import com.example.ui.theme.BirizTextPrimary
import com.example.ui.theme.BirizTextSecondary
import com.example.ui.theme.BirizTurquoise
import com.example.util.LibraryPersistenceHelper
import com.example.util.ParentBookStatus
import com.example.util.ParentHistorySortOrder
import com.example.util.ParentInventoryCounts
import com.example.util.ParentInventoryFilter
import com.example.util.ParentLibraryHelper
import com.example.util.ParentLibraryTab
import com.example.util.TurkishTextHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val LibraryPurpleAccent = Color(0xFF7C3AED)
private val LibraryPurpleSoft = Color(0xFF8B5CF6)

/**
 * Veli Portalı > Kitaplık ekranı.
 * Okuyor, Okuma Geçmişi ve Sınıf Kitaplığı sekmeleriyle çok sayıda kayıtta bile
 * aranan kitaba anında ulaşılmasını sağlar (KVKK uyumlu anonim gösterim).
 */
@Composable
fun ParentReadingDetailView(
    student: Student,
    records: List<ReadingRecord>,
    classBooks: List<Book>,
    isDarkTheme: Boolean
) {
    val primaryText = if (isDarkTheme) BirizDarkTextPrimary else BirizTextPrimary
    val secondaryText = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
    val mutedText = if (isDarkTheme) BirizDarkTextMuted else BirizTextMuted

    var selectedTab by remember { mutableStateOf(ParentLibraryTab.READING) }
    var historySearchQuery by remember { mutableStateOf("") }
    var historySortOrder by remember { mutableStateOf(ParentHistorySortOrder.NEWEST) }
    var inventorySearchQuery by remember { mutableStateOf("") }
    var inventoryFilter by remember { mutableStateOf(ParentInventoryFilter.ALL) }

    val activeReadingBooks = remember(classBooks, student.id) {
        ParentLibraryHelper.resolveActiveReadingBooks(classBooks, student.id)
    }
    val totalPagesRead = remember(records) { records.sumOf { it.pageCount } }
    val totalReadingStars = remember(records) { records.sumOf { it.earnedStars } }
    val uniqueInventoryCount = remember(classBooks) { classBooks.distinctBy { it.id }.size }

    val filteredRecords = remember(records, historySearchQuery, historySortOrder) {
        ParentLibraryHelper.filterAndSortHistory(records, historySearchQuery, historySortOrder)
    }
    val inventoryCounts = remember(classBooks, student.id, records) {
        ParentLibraryHelper.computeInventoryCounts(classBooks, student.id, records)
    }
    val filteredInventoryBooks = remember(classBooks, student.id, records, inventorySearchQuery, inventoryFilter) {
        ParentLibraryHelper.filterInventoryBooks(
            classBooks = classBooks,
            studentId = student.id,
            studentRecords = records,
            query = inventorySearchQuery,
            filter = inventoryFilter
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("parent_library_root")
    ) {
        // Sabit Üst Alan: Kompakt Okuma Karnesi + 3 Sekmeli Geçiş Çubuğu
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Okuma Karnesi Hero Kartı
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Transparent,
                border = BorderStroke(
                    1.dp,
                    if (isDarkTheme) LibraryPurpleSoft.copy(alpha = 0.5f) else LibraryPurpleSoft.copy(alpha = 0.38f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = if (isDarkTheme) {
                                    listOf(Color(0x367C3AED), Color(0x2228B8C2))
                                } else {
                                    listOf(Color(0x187C3AED), Color(0x1428B8C2))
                                }
                            )
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "📚 Okuma Karnesi",
                                    fontSize = 12.sp,
                                    color = if (isDarkTheme) Color(0xFFC4B5FD) else LibraryPurpleAccent,
                                    fontWeight = FontWeight.Bold
                                )
                                if (activeReadingBooks.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isDarkTheme) Color(0x45D97706) else Color(0xFFFEF3C7),
                                        border = BorderStroke(0.8.dp, Color(0xFFF59E0B))
                                    ) {
                                        Text(
                                            text = "📖 ${activeReadingBooks.size} Okuyor",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDarkTheme) Color(0xFFFCD34D) else Color(0xFF92400E),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${records.distinctBy { it.id }.size} Kitap Tamamlandı",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = primaryText
                            )
                            Text(
                                text = "Toplam $totalPagesRead sayfa • +$totalReadingStars ⭐ kazanıldı",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = secondaryText
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDarkTheme) Color(0x28FFFFFF) else Color.White.copy(alpha = 0.85f),
                            border = BorderStroke(
                                1.dp,
                                if (isDarkTheme) Color(0x30FFFFFF) else LibraryPurpleSoft.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("📖", fontSize = 24.sp)
                            }
                        }
                    }
                }
            }

            // 3 Sekmeli Geçiş Çubuğu: Okuyor | Okuma Geçmişi | Sınıf Kitaplığı
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isDarkTheme) Color(0x301E293B) else Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tabCounts = mapOf(
                        ParentLibraryTab.READING to activeReadingBooks.size,
                        ParentLibraryTab.HISTORY to records.distinctBy { it.id }.size,
                        ParentLibraryTab.CLASS_LIBRARY to uniqueInventoryCount
                    )

                    ParentLibraryTab.entries.forEach { tab ->
                        val isSelected = selectedTab == tab
                        val count = tabCounts[tab] ?: 0
                        Surface(
                            onClick = { selectedTab = tab },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) {
                                if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                            } else {
                                Color.Transparent
                            },
                            shadowElevation = if (isSelected) 2.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag(tab.testTag)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(text = tab.emoji, fontSize = 12.sp)
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = when {
                                            isSelected && isDarkTheme -> BirizNavyDark.copy(alpha = 0.25f)
                                            isSelected -> Color.White.copy(alpha = 0.2f)
                                            isDarkTheme -> Color(0x35475569)
                                            else -> Color(0xFFE2E8F0)
                                        }
                                    ) {
                                        Text(
                                            text = count.toString(),
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = when {
                                                isSelected -> if (isDarkTheme) BirizNavyDark else Color.White
                                                else -> primaryText
                                            },
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = tab.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = when {
                                        isSelected -> if (isDarkTheme) BirizNavyDark else Color.White
                                        else -> secondaryText
                                    },
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // Seçili Sekme İçeriği
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = {
                fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(120))
            },
            label = "ParentLibraryTabTransition",
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { currentTab ->
            when (currentTab) {
                ParentLibraryTab.READING -> {
                    ParentActiveReadingTabContent(
                        student = student,
                        activeReadingBooks = activeReadingBooks,
                        isDarkTheme = isDarkTheme,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        onBrowseClassLibrary = { selectedTab = ParentLibraryTab.CLASS_LIBRARY }
                    )
                }

                ParentLibraryTab.HISTORY -> {
                    ParentReadingHistoryTabContent(
                        allRecordsCount = records.distinctBy { it.id }.size,
                        filteredRecords = filteredRecords,
                        searchQuery = historySearchQuery,
                        sortOrder = historySortOrder,
                        isDarkTheme = isDarkTheme,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        mutedText = mutedText,
                        onSearchQueryChange = { historySearchQuery = it },
                        onSortOrderChange = { historySortOrder = it }
                    )
                }

                ParentLibraryTab.CLASS_LIBRARY -> {
                    ParentClassLibraryTabContent(
                        studentId = student.id,
                        studentRecords = records,
                        counts = inventoryCounts,
                        filteredBooks = filteredInventoryBooks,
                        searchQuery = inventorySearchQuery,
                        activeFilter = inventoryFilter,
                        isDarkTheme = isDarkTheme,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        mutedText = mutedText,
                        onSearchQueryChange = { inventorySearchQuery = it },
                        onFilterChange = { inventoryFilter = it }
                    )
                }
            }
        }
    }
}

@Composable
private fun ParentActiveReadingTabContent(
    student: Student,
    activeReadingBooks: List<Book>,
    isDarkTheme: Boolean,
    primaryText: Color,
    secondaryText: Color,
    onBrowseClassLibrary: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("parent_library_reading_list"),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 2.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "📖 Şu An Okuduğu Kitaplar",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = primaryText
                )
                if (activeReadingBooks.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDarkTheme) Color(0x35D97706) else Color(0xFFFEF3C7),
                        border = BorderStroke(1.dp, Color(0xFFFCD34D))
                    ) {
                        Text(
                            text = "${activeReadingBooks.size} Kitap Ödünçte",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) Color(0xFFFCD34D) else Color(0xFF92400E),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        if (activeReadingBooks.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDarkTheme) BirizDarkCardGlass else Color.White,
                    border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📗", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Şu an ödünç alınmış aktif bir kitap bulunmuyor.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryText,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Öğretmeniniz sınıftan kitap verdiğinde veriliş tarihi ve kazanılacak yıldız bilgisi burada anlık görünür.",
                            fontSize = 11.5.sp,
                            color = secondaryText,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onBrowseClassLibrary,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("parent_library_browse_inventory_button")
                        ) {
                            Text(
                                text = "📚 Sınıf Kitaplığına Göz At",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                            )
                        }
                    }
                }
            }
        } else {
            items(activeReadingBooks, key = { "active_${it.id}" }) { book ->
                val myAssignment = book.activeAssignments.firstOrNull { it.studentId == student.id }
                val rawAssignMs = myAssignment?.assignmentDate ?: book.assignmentDate ?: System.currentTimeMillis()
                val assignDateMs = if (rawAssignMs > 0L) rawAssignMs else System.currentTimeMillis()
                val daysAgo = ((System.currentTimeMillis() - assignDateMs) / (1000L * 60L * 60L * 24L)).coerceAtLeast(1L)
                val targetStars = LibraryPersistenceHelper.calculateReadingStars(book.pageCount)

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDarkTheme) Color(0x3578350F) else Color(0xFFFEF3C7),
                    border = BorderStroke(1.2.dp, Color(0xFFF59E0B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(13.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFD97706)
                                ) {
                                    Text(
                                        text = "#${book.registrationNo}",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = TurkishTextHelper.toTurkishTitleCase(book.name),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = primaryText,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFD97706)
                            ) {
                                Text(
                                    text = "$daysAgo. Gününde",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }

                        if (book.author.isNotBlank()) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Yazar: ${book.author}",
                                fontSize = 11.5.sp,
                                color = secondaryText
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📄 ${book.pageCount} Sayfa • Veriliş: ${SimpleDateFormat("dd.MM.yyyy", Locale("tr")).format(Date(assignDateMs))}",
                                fontSize = 11.5.sp,
                                color = if (isDarkTheme) Color(0xFFFDE68A) else Color(0xFF92400E),
                                fontWeight = FontWeight.Medium
                            )

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = BirizGold.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, BirizGold)
                            ) {
                                Text(
                                    text = "İade edilince +$targetStars ⭐",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkTheme) Color(0xFFFCD34D) else Color(0xFFB45309),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
private fun ParentReadingHistoryTabContent(
    allRecordsCount: Int,
    filteredRecords: List<ReadingRecord>,
    searchQuery: String,
    sortOrder: ParentHistorySortOrder,
    isDarkTheme: Boolean,
    primaryText: Color,
    secondaryText: Color,
    mutedText: Color,
    onSearchQueryChange: (String) -> Unit,
    onSortOrderChange: (ParentHistorySortOrder) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (allRecordsCount > 0) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ParentLibrarySearchBar(
                    query = searchQuery,
                    placeholder = "Okunan kitap adı ara...",
                    isDarkTheme = isDarkTheme,
                    primaryText = primaryText,
                    mutedText = mutedText,
                    testTag = "parent_reading_history_search",
                    onQueryChange = onSearchQueryChange
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(ParentHistorySortOrder.entries, key = { it.name }) { order ->
                            val isSelected = sortOrder == order
                            Surface(
                                onClick = { onSortOrderChange(order) },
                                shape = RoundedCornerShape(18.dp),
                                color = if (isSelected) {
                                    if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                                } else if (isDarkTheme) {
                                    Color(0x30334E68)
                                } else {
                                    Color(0xFFF1F5F9)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) {
                                        if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                                    } else if (isDarkTheme) {
                                        BirizDarkCardBorder
                                    } else {
                                        BirizBorder
                                    }
                                ),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .heightIn(min = 36.dp)
                                    .testTag("history_sort_${order.name.lowercase(Locale.ROOT)}")
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = order.label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) {
                                            if (isDarkTheme) BirizNavyDark else Color.White
                                        } else {
                                            primaryText
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "${filteredRecords.size}/$allRecordsCount" else "$allRecordsCount Kitap",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = secondaryText
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("parent_library_history_list"),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (allRecordsCount == 0) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDarkTheme) BirizDarkCardGlass else Color.White,
                        border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("📕", fontSize = 32.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Henüz tamamlanmış kitap okuma kaydı bulunmuyor.",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryText,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Öğretmeniniz okunan kitap iadesini onayladığında tamamlama tarihleri ve kazanılan yıldızlar burada listelenir.",
                                fontSize = 11.5.sp,
                                color = secondaryText,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else if (filteredRecords.isEmpty()) {
                item {
                    ParentLibraryEmptySearchCard(
                        title = "Aranan kitap geçmişte bulunamadı",
                        subtitle = "\"$searchQuery\" ile eşleşen bir okuma kaydı yok.",
                        isDarkTheme = isDarkTheme,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        onClear = { onSearchQueryChange("") }
                    )
                }
            } else {
                items(filteredRecords, key = { "record_${it.id}" }) { rec ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDarkTheme) BirizDarkCardGlass else Color.White,
                        border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = TurkishTextHelper.toTurkishTitleCase(rec.bookName),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = primaryText
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                val safeEnd = if (rec.endDate > 0L) rec.endDate else System.currentTimeMillis()
                                val safeStart = if (rec.startDate in 1L..safeEnd) rec.startDate else safeEnd
                                val readingDays = ((safeEnd - safeStart) / (1000L * 60L * 60L * 24L)).coerceIn(1L, 3650L).toInt()
                                val endStr = SimpleDateFormat("dd.MM.yyyy", Locale("tr")).format(Date(safeEnd))
                                Text(
                                    text = "${rec.pageCount} Sayfa • $readingDays günde okundu • Bitiş: $endStr",
                                    fontSize = 11.5.sp,
                                    color = secondaryText
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = BirizAmber.copy(alpha = 0.18f),
                                border = BorderStroke(1.dp, BirizAmber.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "+${rec.earnedStars} ⭐",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = BirizAmber,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
private fun ParentClassLibraryTabContent(
    studentId: String,
    studentRecords: List<ReadingRecord>,
    counts: ParentInventoryCounts,
    filteredBooks: List<Book>,
    searchQuery: String,
    activeFilter: ParentInventoryFilter,
    isDarkTheme: Boolean,
    primaryText: Color,
    secondaryText: Color,
    mutedText: Color,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (ParentInventoryFilter) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (counts.all > 0) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ParentLibrarySearchBar(
                    query = searchQuery,
                    placeholder = "Kitap adı, yazar veya #no ara...",
                    isDarkTheme = isDarkTheme,
                    primaryText = primaryText,
                    mutedText = mutedText,
                    testTag = "parent_class_library_search",
                    onQueryChange = onSearchQueryChange
                )

                val chipItems = listOf(
                    Triple(ParentInventoryFilter.ALL, "Tümü (${counts.all})", Color(0xFF475569)),
                    Triple(ParentInventoryFilter.ON_SHELF, "🟢 Rafta (${counts.onShelf})", Color(0xFF059669)),
                    Triple(ParentInventoryFilter.COMPLETED_BY_CHILD, "✓ Okundu (${counts.completedByChild})", Color(0xFF0284C7)),
                    Triple(ParentInventoryFilter.BORROWED, "🟠 Ödünçte (${counts.borrowed})", Color(0xFFD97706))
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(chipItems, key = { it.first.name }) { (filterKey, label, activeColor) ->
                        val isSelected = activeFilter == filterKey
                        Surface(
                            onClick = { onFilterChange(filterKey) },
                            shape = RoundedCornerShape(18.dp),
                            color = if (isSelected) {
                                activeColor
                            } else if (isDarkTheme) {
                                Color(0x30334E68)
                            } else {
                                Color(0xFFF1F5F9)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) activeColor else if (isDarkTheme) BirizDarkCardBorder else BirizBorder
                            ),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .heightIn(min = 36.dp)
                                .testTag("inventory_filter_${filterKey.name.lowercase(Locale.ROOT)}")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else primaryText
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("parent_library_inventory_list"),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (counts.all == 0) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDarkTheme) BirizDarkCardGlass else Color.White,
                        border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("📚", fontSize = 32.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Sınıf kitaplığına henüz kitap eklenmedi.",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryText,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else if (filteredBooks.isEmpty()) {
                item {
                    ParentLibraryEmptySearchCard(
                        title = "Eşleşen kitap bulunamadı",
                        subtitle = "Arama kelimesini veya seçili durum filtresini değiştirerek tekrar deneyebilirsiniz.",
                        isDarkTheme = isDarkTheme,
                        primaryText = primaryText,
                        secondaryText = secondaryText,
                        buttonText = "Filtreleri Sıfırla",
                        onClear = {
                            onSearchQueryChange("")
                            onFilterChange(ParentInventoryFilter.ALL)
                        }
                    )
                }
            } else {
                items(filteredBooks, key = { "inventory_${it.id}" }) { book ->
                    val status = ParentLibraryHelper.resolveBookStatus(
                        book = book,
                        studentId = studentId,
                        studentRecords = studentRecords
                    )
                    val isMyChildReading = status == ParentBookStatus.MY_CHILD_READING

                    // Durum etiketi (KVKK kurallarına uygun: Başka bir öğrencinin adı ASLA veliye gösterilmez)
                    val (statusText, statusColor, statusBg) = when (status) {
                        ParentBookStatus.MY_CHILD_READING -> Triple("Çocuğunuzda 📖", Color(0xFF92400E), Color(0xFFFDE68A))
                        ParentBookStatus.COMPLETED_BY_CHILD -> Triple("Okundu ✓", Color(0xFF047857), Color(0xFFD1FAE5))
                        ParentBookStatus.BORROWED_BY_OTHER -> Triple("Ödünçte", Color(0xFFB45309), Color(0xFFFEF3C7))
                        ParentBookStatus.ON_SHELF -> Triple("Rafta", Color(0xFF065F46), Color(0xFFD1FAE5))
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isMyChildReading) {
                            if (isDarkTheme) Color(0x30D97706) else Color(0xFFFEF3C7)
                        } else if (isDarkTheme) {
                            BirizDarkCardGlass
                        } else {
                            Color.White
                        },
                        border = BorderStroke(
                            if (isMyChildReading) 1.2.dp else 1.dp,
                            if (isMyChildReading) Color(0xFFF59E0B) else (if (isDarkTheme) BirizDarkCardBorder else BirizBorder)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isDarkTheme) Color(0x40334E68) else Color(0xFFE2E8F0)
                                ) {
                                    Text(
                                        text = "#${book.registrationNo}",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = TurkishTextHelper.toTurkishTitleCase(book.name),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = primaryText,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${book.pageCount} Sayfa" + if (book.author.isNotBlank()) " • ${book.author}" else "",
                                        fontSize = 11.sp,
                                        color = secondaryText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = statusBg
                            ) {
                                Text(
                                    text = statusText,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
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
private fun ParentLibrarySearchBar(
    query: String,
    placeholder: String,
    isDarkTheme: Boolean,
    primaryText: Color,
    mutedText: Color,
    testTag: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                text = placeholder,
                fontSize = 12.5.sp,
                color = mutedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Ara",
                tint = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                modifier = Modifier.size(18.dp)
            )
        },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.testTag("${testTag}_clear")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Aramayı Temizle",
                        tint = mutedText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        } else {
            null
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = primaryText,
            unfocusedTextColor = primaryText,
            focusedBorderColor = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
            unfocusedBorderColor = if (isDarkTheme) BirizDarkCardBorder else BirizBorder,
            focusedContainerColor = if (isDarkTheme) Color(0x281E293B) else Color.White,
            unfocusedContainerColor = if (isDarkTheme) Color(0x201E293B) else Color.White
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    )
}

@Composable
private fun ParentLibraryEmptySearchCard(
    title: String,
    subtitle: String,
    isDarkTheme: Boolean,
    primaryText: Color,
    secondaryText: Color,
    buttonText: String = "Aramayı Temizle",
    onClear: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isDarkTheme) BirizDarkCardGlass else Color.White,
        border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🔍", fontSize = 28.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = primaryText,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                fontSize = 11.5.sp,
                color = secondaryText,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onClear,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(
                    text = buttonText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                )
            }
        }
    }
}
