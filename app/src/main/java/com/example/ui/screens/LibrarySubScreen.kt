package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActiveAssignment
import com.example.data.model.Book
import com.example.data.model.ClassRoom
import com.example.data.model.ReadingRecord
import com.example.data.model.Student
import com.example.ui.theme.BirizAmber
import com.example.ui.theme.BirizBorder
import com.example.ui.theme.BirizDarkBgBottom
import com.example.ui.theme.BirizDarkBgTop
import com.example.ui.theme.BirizDarkCardBorder
import com.example.ui.theme.BirizDarkCardGlass
import com.example.ui.theme.BirizDarkTextMuted
import com.example.ui.theme.BirizDarkTextPrimary
import com.example.ui.theme.BirizDarkTextSecondary
import com.example.ui.theme.BirizError
import com.example.ui.theme.BirizNavyDark
import com.example.ui.theme.BirizNavyPrimary
import com.example.ui.theme.BirizSuccess
import com.example.ui.theme.BirizTextPrimary
import com.example.ui.theme.BirizTextSecondary
import com.example.ui.theme.BirizTurquoise
import com.example.ui.viewmodel.DashboardSubPage
import com.example.util.ExcelPdfHelper
import com.example.util.LibraryPersistenceHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val BirizCardBg = Color.White
private val BirizGold = Color(0xFFF59E0B)

@Composable
fun LibrarySubScreen(
    classRoom: ClassRoom,
    students: List<Student>,
    currentSubPage: DashboardSubPage = DashboardSubPage.KITAPLIK_LISTE,
    isDarkTheme: Boolean = false,
    onSelectSubPage: (DashboardSubPage) -> Unit = {},
    onBackToClassList: () -> Unit
) {
    BackHandler { onBackToClassList() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State
    var books by remember { mutableStateOf<List<Book>>(emptyList()) }
    var records by remember { mutableStateOf<List<ReadingRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Active tab (0: Envanter, 1: Ödünç & İade Geçmişi, 2: Okuma Analitiği)
    var selectedTab by remember {
        mutableIntStateOf(
            when (currentSubPage) {
                DashboardSubPage.KITAPLIK_KAYITLAR -> 1
                DashboardSubPage.KITAPLIK_DEGERLENDIRME -> 2
                else -> 0
            }
        )
    }

    // Sync tab with external subpage selection if provided
    LaunchedEffect(currentSubPage) {
        val targetTab = when (currentSubPage) {
            DashboardSubPage.KITAPLIK_KAYITLAR -> 1
            DashboardSubPage.KITAPLIK_DEGERLENDIRME -> 2
            else -> 0
        }
        if (selectedTab != targetTab) {
            selectedTab = targetTab
        }
    }

    // Filters and search
    var searchQuery by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("TÜMÜ") } // TÜMÜ, RAFTA, OKUNUYOR
    var viewMode by remember { mutableStateOf("GRID") } // GRID, LIST
    var selectedStudentFilterId by remember { mutableStateOf<String?>(null) } // null = Tüm Sınıf

    // Dialog states
    var showAddEditDialog by remember { mutableStateOf(false) }
    var bookToEdit by remember { mutableStateOf<Book?>(null) }

    var showBorrowDialog by remember { mutableStateOf(false) }
    var bookToBorrow by remember { mutableStateOf<Book?>(null) }

    var showReturnDialog by remember { mutableStateOf(false) }
    var bookToReturn by remember { mutableStateOf<Book?>(null) }

    var showBatchClassReadDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var bookToDelete by remember { mutableStateOf<Book?>(null) }
    var recordToDelete by remember { mutableStateOf<ReadingRecord?>(null) }

    // Load data
    fun refreshData() {
        books = LibraryPersistenceHelper.loadBooks(context, classRoom.id)
        records = LibraryPersistenceHelper.loadReadingRecords(context, classRoom.id)
        isLoading = false
    }

    LaunchedEffect(classRoom.id) {
        refreshData()
    }

    // Colors
    val cardBg = if (isDarkTheme) BirizDarkCardGlass else BirizCardBg
    val cardBorder = if (isDarkTheme) BirizDarkCardBorder else BirizBorder
    val primaryText = if (isDarkTheme) BirizDarkTextPrimary else BirizTextPrimary
    val secondaryText = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isDarkTheme) {
                        listOf(BirizDarkBgTop, BirizDarkBgBottom)
                    } else {
                        listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9))
                    }
                )
            )
            .testTag("library_screen_root")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 68.dp) // space for floating dock
        ) {
            // İçerik: currentSubPage'e göre gösterilir (Envanter, Ödünç & İade, Analitik)
            when (selectedTab) {
                0 -> {
                    // TAB 0: KİTAPLIK ENVANTERİ
                    LibraryInventoryView(
                        books = books,
                        students = students,
                        searchQuery = searchQuery,
                        statusFilter = statusFilter,
                        viewMode = viewMode,
                        isDarkTheme = isDarkTheme,
                        onSearchChange = { searchQuery = it },
                        onStatusFilterChange = { statusFilter = it },
                        onViewModeToggle = { viewMode = if (viewMode == "GRID") "LIST" else "GRID" },
                        onExportClick = { showExportDialog = true },
                        onAddBookClick = {
                            bookToEdit = null
                            showAddEditDialog = true
                        },
                        onBorrowClick = { book ->
                            bookToBorrow = book
                            showBorrowDialog = true
                        },
                        onReturnClick = { book ->
                            bookToReturn = book
                            showReturnDialog = true
                        },
                        onEditClick = { book ->
                            bookToEdit = book
                            showAddEditDialog = true
                        },
                        onDeleteClick = { book ->
                            bookToDelete = book
                        },
                        onCancelAssignment = { book, studentId ->
                            LibraryPersistenceHelper.cancelAssignment(context, classRoom.id, book.id, studentId)
                            refreshData()
                            Toast.makeText(context, "Teslimat iptal edildi, kitap rafa döndü.", Toast.LENGTH_SHORT).show()
                        },
                        onBatchClassReadClick = {
                            showBatchClassReadDialog = true
                        }
                    )
                }

                1 -> {
                    // TAB 1: ÖDÜNÇ & İADE GEÇMİŞİ
                    ReadingMovementsView(
                        books = books,
                        records = records,
                        students = students,
                        isDarkTheme = isDarkTheme,
                        onReturnBook = { book ->
                            bookToReturn = book
                            showReturnDialog = true
                        },
                        onCancelAssignment = { book, studentId ->
                            LibraryPersistenceHelper.cancelAssignment(context, classRoom.id, book.id, studentId)
                            refreshData()
                            Toast.makeText(context, "Teslimat iptal edildi.", Toast.LENGTH_SHORT).show()
                        },
                        onDeleteRecord = { record ->
                            recordToDelete = record
                        }
                    )
                }

                2 -> {
                    // TAB 2: OKUMA ANALİTİĞİ & ÖĞRENCİ KARNESİ
                    ReadingAnalyticsView(
                        books = books,
                        records = records,
                        students = students,
                        selectedStudentId = selectedStudentFilterId,
                        isDarkTheme = isDarkTheme,
                        onSelectStudent = { selectedStudentFilterId = it }
                    )
                }
            }
        }

        // --- DİYALOGLAR ---

        // 1. Kitap Ekle / Düzenle Diyaloğu
        if (showAddEditDialog) {
            AddEditBookDialog(
                existingBooks = books,
                students = students,
                bookToEdit = bookToEdit,
                isDarkTheme = isDarkTheme,
                onDismiss = { showAddEditDialog = false },
                onSave = { name, author, pageCount, registrationNo, assignStudentId, assignDate ->
                    if (bookToEdit == null) {
                        LibraryPersistenceHelper.addBook(
                            context = context,
                            classRoomId = classRoom.id,
                            name = name,
                            author = author,
                            pageCount = pageCount,
                            registrationNo = registrationNo,
                            initialStudentId = assignStudentId,
                            initialStudentName = students.firstOrNull { it.id == assignStudentId }?.fullName,
                            assignmentDate = assignDate
                        )
                        Toast.makeText(context, "✨ Kitap başarıyla kaydedildi.", Toast.LENGTH_SHORT).show()
                    } else {
                        val updated = bookToEdit!!.copy(
                            name = name.trim(),
                            author = author.trim(),
                            pageCount = pageCount,
                            registrationNo = registrationNo
                        )
                        LibraryPersistenceHelper.updateBook(context, classRoom.id, updated)
                        Toast.makeText(context, "Kitap bilgileri güncellendi.", Toast.LENGTH_SHORT).show()
                    }
                    refreshData()
                    showAddEditDialog = false
                }
            )
        }

        // 2. Kitap Ödünç Verme (Teslim Et) Diyaloğu
        if (showBorrowDialog && bookToBorrow != null) {
            BorrowBookDialog(
                book = bookToBorrow!!,
                students = students,
                isDarkTheme = isDarkTheme,
                onDismiss = {
                    showBorrowDialog = false
                    bookToBorrow = null
                },
                onConfirm = { selectedStudents, dateStr ->
                    selectedStudents.forEach { student ->
                        LibraryPersistenceHelper.borrowBook(
                            context = context,
                            classRoomId = classRoom.id,
                            bookId = bookToBorrow!!.id,
                            studentId = student.id,
                            studentName = student.fullName,
                            date = dateStr
                        )
                    }
                    refreshData()
                    showBorrowDialog = false
                    bookToBorrow = null
                    Toast.makeText(context, "📖 Kitap teslim edildi.", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // 3. Kitap İade Alma ve Motivasyon Yıldızı Diyaloğu
        if (showReturnDialog && bookToReturn != null) {
            ReturnBookDialog(
                book = bookToReturn!!,
                isDarkTheme = isDarkTheme,
                onDismiss = {
                    showReturnDialog = false
                    bookToReturn = null
                },
                onConfirm = { studentId, studentName, startDate, returnDateStr ->
                    val returnDateMs = try {
                        SimpleDateFormat("dd.MM.yyyy", Locale("tr")).parse(returnDateStr)?.time ?: System.currentTimeMillis()
                    } catch (e: Exception) { System.currentTimeMillis() }
                    val earnedStars = LibraryPersistenceHelper.returnBook(
                        context = context,
                        classRoomId = classRoom.id,
                        bookId = bookToReturn!!.id,
                        studentId = studentId,
                        studentName = studentName,
                        startDate = startDate,
                        returnDate = returnDateMs
                    )
                    refreshData()
                    showReturnDialog = false
                    bookToReturn = null
                    Toast.makeText(context, "🎉 Kitap iade alındı! $studentName +$earnedStars ⭐ kazandı!", Toast.LENGTH_LONG).show()
                }
            )
        }

        // 4. Tüm Sınıf Okudu Toplu İşlem Diyaloğu
        if (showBatchClassReadDialog) {
            BatchClassReadDialog(
                books = books,
                students = students,
                isDarkTheme = isDarkTheme,
                onDismiss = { showBatchClassReadDialog = false },
                onConfirm = { selectedBook, dateStr ->
                    val totalAwarded = LibraryPersistenceHelper.recordBatchClassReading(
                        context = context,
                        classRoomId = classRoom.id,
                        book = selectedBook,
                        students = students,
                        date = dateStr
                    )
                    refreshData()
                    showBatchClassReadDialog = false
                    Toast.makeText(context, "👏 Tüm sınıfa (${students.size} öğrenci) $totalAwarded yıldız dağıtıldı!", Toast.LENGTH_LONG).show()
                }
            )
        }

        // 5. Kitap Silme Onay Diyaloğu
        if (bookToDelete != null) {
            AlertDialog(
                onDismissRequest = { bookToDelete = null },
                title = { Text("Kitabı Sil?", fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "\"${bookToDelete!!.name}\" adlı kitabı envanterden silmek istediğinize emin misiniz?\n\n" +
                                "Not: Daha önce tamamlanan okuma kayıtları arşivde saklanmaya devam edecektir.",
                        fontSize = 13.5.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            LibraryPersistenceHelper.deleteBook(context, classRoom.id, bookToDelete!!.id)
                            refreshData()
                            bookToDelete = null
                            Toast.makeText(context, "Kitap silindi.", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BirizError)
                    ) {
                        Text("Evet, Sil", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { bookToDelete = null }) {
                        Text("İptal")
                    }
                }
            )
        }

        // 6. Okuma Kaydı Silme Onay Diyaloğu
        if (recordToDelete != null) {
            AlertDialog(
                onDismissRequest = { recordToDelete = null },
                title = { Text("Okuma Kaydını Sil?", fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "${recordToDelete!!.studentName} öğrencisinin \"${recordToDelete!!.bookName}\" okuma geçmişi kaydını silmek istiyor musunuz?",
                        fontSize = 13.5.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            LibraryPersistenceHelper.deleteReadingRecord(context, classRoom.id, recordToDelete!!.id)
                            refreshData()
                            recordToDelete = null
                            Toast.makeText(context, "Kayıt silindi.", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BirizError)
                    ) {
                        Text("Sil", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { recordToDelete = null }) {
                        Text("İptal")
                    }
                }
            )
        }

        // 7. Raporlama ve Dışa Aktarma Diyaloğu
        if (showExportDialog) {
            ExportLibraryDialog(
                classRoom = classRoom,
                books = books,
                records = records,
                isDarkTheme = isDarkTheme,
                onDismiss = { showExportDialog = false }
            )
        }
    }
}

// -------------------------------------------------------------
// TAB 0: KİTAPLIK ENVANTERİ BİLEŞENİ
// -------------------------------------------------------------
@Composable
private fun LibraryInventoryView(
    books: List<Book>,
    students: List<Student>,
    searchQuery: String,
    statusFilter: String,
    viewMode: String,
    isDarkTheme: Boolean,
    onSearchChange: (String) -> Unit,
    onStatusFilterChange: (String) -> Unit,
    onViewModeToggle: () -> Unit,
    onExportClick: () -> Unit,
    onAddBookClick: () -> Unit,
    onBorrowClick: (Book) -> Unit,
    onReturnClick: (Book) -> Unit,
    onEditClick: (Book) -> Unit,
    onDeleteClick: (Book) -> Unit,
    onCancelAssignment: (Book, String) -> Unit,
    onBatchClassReadClick: () -> Unit
) {
    val filteredBooks = remember(books, searchQuery, statusFilter) {
        books.filter { book ->
            val matchesSearch = searchQuery.isBlank() ||
                    book.name.contains(searchQuery, ignoreCase = true) ||
                    book.author.contains(searchQuery, ignoreCase = true) ||
                    book.registrationNo.toString() == searchQuery.trim()

            val isReading = book.status == "Okunuyor" || book.activeAssignments.isNotEmpty()
            val matchesStatus = when (statusFilter) {
                "RAFTA" -> !isReading
                "OKUNUYOR" -> isReading
                else -> true
            }
            matchesSearch && matchesStatus
        }.sortedBy { it.registrationNo }
    }

    val shelfCount = remember(books) { books.count { it.status != "Okunuyor" && it.activeAssignments.isEmpty() } }
    val readingCount = remember(books) { books.size - shelfCount }

    val primaryText = if (isDarkTheme) BirizDarkTextPrimary else BirizTextPrimary
    val secondaryText = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        // Arama Kutucuğu, Rapor, Kitap Ekle ve Görünüm Değiştirici
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .testTag("library_search_input"),
                placeholder = { Text("Kitap adı, yazar...", fontSize = 12.sp, maxLines = 1) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Ara", modifier = Modifier.size(17.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }, modifier = Modifier.size(24.dp)) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Temizle", modifier = Modifier.size(15.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                    unfocusedBorderColor = if (isDarkTheme) Color(0x5064748B) else BirizBorder
                )
            )

            // Rapor Butonu
            OutlinedButton(
                onClick = onExportClick,
                modifier = Modifier
                    .height(46.dp)
                    .testTag("library_export_button"),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (isDarkTheme) BirizTurquoise else BirizNavyPrimary),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FileDownload,
                    contentDescription = "Rapor",
                    tint = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "Rapor",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                )
            }

            // + Kitap Ekle Butonu
            Button(
                onClick = onAddBookClick,
                modifier = Modifier
                    .height(46.dp)
                    .testTag("library_add_book_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Kitap Ekle",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "Kitap",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Görünüm Modu Değiştir (Grid / Liste)
            Surface(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onViewModeToggle() }
                    .testTag("library_view_mode_toggle"),
                shape = RoundedCornerShape(10.dp),
                color = if (isDarkTheme) Color(0x35334E68) else Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, if (isDarkTheme) Color(0x5064748B) else BirizBorder)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (viewMode == "GRID") Icons.Default.ViewList else Icons.Default.GridView,
                        contentDescription = "Görünüm Değiştir",
                        tint = primaryText,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Durum Filtre Çipleri & Toplu Dağıt Butonu
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val filters = listOf(
                    Triple("TÜMÜ", "Tümü (${books.size})", Color(0xFF64748B)),
                    Triple("RAFTA", "🟢 Rafta ($shelfCount)", BirizSuccess),
                    Triple("OKUNUYOR", "🟠 Okunuyor ($readingCount)", Color(0xFFD97706))
                )

                filters.forEach { (key, label, color) ->
                    val isSelected = statusFilter == key
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onStatusFilterChange(key) },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) color else if (isDarkTheme) Color(0x30334E68) else Color(0xFFF1F5F9),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) color else if (isDarkTheme) Color(0x4064748B) else BirizBorder
                        )
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else primaryText,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Toplu Dağıt Butonu
            TextButton(
                onClick = onBatchClassReadClick,
                modifier = Modifier.testTag("batch_class_read_button"),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
            ) {
                Text(
                    text = "👥 Sınıfça Okuduk",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Kitaplar Listesi
        if (filteredBooks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDarkTheme) Color(0x25334E68) else Color.White,
                    border = BorderStroke(1.dp, if (isDarkTheme) Color(0x40334E68) else BirizBorder),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isDarkTheme) Color(0x3506B6D4) else Color(0xFFE0F2FE),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = "📚", fontSize = 30.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Arama kriterine uygun kitap bulunamadı" else "Kitaplıkta Henüz Kitap Yok",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = primaryText
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty())
                                "Farklı bir kitap adı, yazar veya demirbaş numarası ile tekrar arayabilirsiniz."
                            else
                                "Sınıf kitaplığınızı oluşturmak için ilk kitabınızı ekleyin. Sıra numarası otomatik atanır ve sayfa hacmine göre yıldızlar hazır!",
                            fontSize = 12.sp,
                            color = secondaryText,
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onAddBookClick,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("İlk Kitabı Ekle", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        } else if (viewMode == "GRID") {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredBooks, key = { it.id }) { book ->
                    BookCardGridItem(
                        book = book,
                        isDarkTheme = isDarkTheme,
                        onBorrowClick = { onBorrowClick(book) },
                        onReturnClick = { onReturnClick(book) },
                        onEditClick = { onEditClick(book) },
                        onDeleteClick = { onDeleteClick(book) },
                        onCancelAssignment = { studentId -> onCancelAssignment(book, studentId) }
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredBooks, key = { it.id }) { book ->
                    BookListItem(
                        book = book,
                        isDarkTheme = isDarkTheme,
                        onBorrowClick = { onBorrowClick(book) },
                        onReturnClick = { onReturnClick(book) },
                        onEditClick = { onEditClick(book) },
                        onDeleteClick = { onDeleteClick(book) },
                        onCancelAssignment = { studentId -> onCancelAssignment(book, studentId) }
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// GRID CARD BİLEŞENİ
// -------------------------------------------------------------
@Composable
private fun BookCardGridItem(
    book: Book,
    isDarkTheme: Boolean,
    onBorrowClick: () -> Unit,
    onReturnClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onCancelAssignment: (String) -> Unit
) {
    val cardBg = if (isDarkTheme) BirizDarkCardGlass else BirizCardBg
    val cardBorder = if (isDarkTheme) BirizDarkCardBorder else BirizBorder
    val primaryText = if (isDarkTheme) BirizDarkTextPrimary else BirizTextPrimary
    val secondaryText = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary

    val isReading = book.status == "Okunuyor" || book.activeAssignments.isNotEmpty()
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("book_card_${book.registrationNo}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(
            1.dp,
            if (isReading) Color(0xFFF59E0B).copy(alpha = 0.5f) else cardBorder
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Üst Satır: Demirbaş No + Menü
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isDarkTheme) Color(0x40334E68) else Color(0xFFE2E8F0)
                ) {
                    Text(
                        text = "#${book.registrationNo}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Text("⋮", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = secondaryText)
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Düzenle") },
                            onClick = {
                                showMenu = false
                                onEditClick()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                        if (isReading && book.activeAssignments.isNotEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Teslimatı İptal Et (Rafa Al)") },
                                onClick = {
                                    showMenu = false
                                    val firstStudentId = book.activeAssignments.firstOrNull()?.studentId ?: book.currentStudentId.orEmpty()
                                    onCancelAssignment(firstStudentId)
                                }
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Kitabı Sil", color = BirizError) },
                            onClick = {
                                showMenu = false
                                onDeleteClick()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = BirizError, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Kitap Başlığı & Yazar
            Text(
                text = book.name,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = primaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 17.sp
            )

            if (book.author.isNotBlank()) {
                Text(
                    text = book.author,
                    fontSize = 11.sp,
                    color = secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Sayfa Sayısı & Yıldız Değeri
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (book.pageCount > 0) {
                    Text(
                        text = "📄 ${book.pageCount} s.",
                        fontSize = 10.5.sp,
                        color = secondaryText
                    )
                    Text(
                        text = "• +${LibraryPersistenceHelper.calculateReadingStars(book.pageCount)} ⭐",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BirizGold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Durum Rozeti ve Okuyan Bilgisi
            if (isReading) {
                val readers = book.activeAssignments
                val readerText = when {
                    readers.isNotEmpty() -> {
                        val first = readers.first()
                        val days = calculateDaysAgo(first.assignmentDate)
                        "${first.studentName} ($days. gün)"
                    }
                    !book.currentStudentName.isNullOrBlank() -> book.currentStudentName
                    else -> "Öğrencide"
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFEF3C7),
                    border = BorderStroke(1.dp, Color(0xFFFCD34D))
                ) {
                    Column(modifier = Modifier.padding(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("👤", fontSize = 10.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = readerText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF92400E),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // İade Al Butonu
                Button(
                    onClick = onReturnClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("📥 İade Al", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDarkTheme) Color(0x30059669) else Color(0xFFD1FAE5),
                    border = BorderStroke(1.dp, if (isDarkTheme) Color(0x50059669) else Color(0xFFA7F3D0))
                ) {
                    Text(
                        text = "🟢 Rafta (Müsait)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDarkTheme) Color(0xFF6EE7B7) else Color(0xFF065F46),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Ödünç Ver Butonu
                OutlinedButton(
                    onClick = onBorrowClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (isDarkTheme) BirizTurquoise else BirizNavyPrimary),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "📖 Ödünç Ver",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// LIST ROW BİLEŞENİ
// -------------------------------------------------------------
@Composable
private fun BookListItem(
    book: Book,
    isDarkTheme: Boolean,
    onBorrowClick: () -> Unit,
    onReturnClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onCancelAssignment: (String) -> Unit
) {
    val cardBg = if (isDarkTheme) BirizDarkCardGlass else BirizCardBg
    val cardBorder = if (isDarkTheme) BirizDarkCardBorder else BirizBorder
    val primaryText = if (isDarkTheme) BirizDarkTextPrimary else BirizTextPrimary
    val secondaryText = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary

    val isReading = book.status == "Okunuyor" || book.activeAssignments.isNotEmpty()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Demirbaş & Detaylar
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
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = book.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = primaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (book.author.isNotBlank()) {
                            Text(
                                text = book.author,
                                fontSize = 11.sp,
                                color = secondaryText
                            )
                            Text(" • ", fontSize = 11.sp, color = secondaryText)
                        }
                        if (book.pageCount > 0) {
                            Text(
                                text = "${book.pageCount} s. (+${LibraryPersistenceHelper.calculateReadingStars(book.pageCount)} ⭐)",
                                fontSize = 11.sp,
                                color = secondaryText
                            )
                        }
                        if (isReading) {
                            val reader = book.activeAssignments.firstOrNull()?.studentName ?: book.currentStudentName.orEmpty()
                            Text(" • 👤 $reader", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFD97706))
                        }
                    }
                }
            }

            // Aksiyon Butonları
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isReading) {
                    Button(
                        onClick = onReturnClick,
                        modifier = Modifier.height(30.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text("İade Al", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedButton(
                        onClick = onBorrowClick,
                        modifier = Modifier.height(30.dp),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, if (isDarkTheme) BirizTurquoise else BirizNavyPrimary),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            text = "Ver",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                        )
                    }
                }

                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Düzenle", modifier = Modifier.size(16.dp), tint = secondaryText)
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Sil", modifier = Modifier.size(16.dp), tint = BirizError)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 1: ÖDÜNÇ & İADE HAREKETLERİ LİSTESİ
// -------------------------------------------------------------
@Composable
private fun ReadingMovementsView(
    books: List<Book>,
    records: List<ReadingRecord>,
    students: List<Student>,
    isDarkTheme: Boolean,
    onReturnBook: (Book) -> Unit,
    onCancelAssignment: (Book, String) -> Unit,
    onDeleteRecord: (ReadingRecord) -> Unit
) {
    var subTab by remember { mutableStateOf("TUMU") } // TUMU, AKTIF, TAMAMLANAN
    var movementSearch by remember { mutableStateOf("") }

    val primaryText = if (isDarkTheme) BirizDarkTextPrimary else BirizTextPrimary
    val secondaryText = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary

    // Aktif Okumalar (Kitaplar üzerinden)
    val activeAssignmentsList = remember(books) {
        books.flatMap { book ->
            book.activeAssignments.map { assignment ->
                Pair(book, assignment)
            }
        }
    }

    val filteredRecords = remember(records, movementSearch) {
        if (movementSearch.isBlank()) records else {
            records.filter {
                it.studentName.contains(movementSearch, ignoreCase = true) ||
                        it.bookName.contains(movementSearch, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        // Arama ve Alt Filtreler
        OutlinedTextField(
            value = movementSearch,
            onValueChange = { movementSearch = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            placeholder = { Text("Öğrenci veya kitap adı ara...", fontSize = 12.5.sp) },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = "Ara", modifier = Modifier.size(18.dp))
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val tabs = listOf(
                Pair("TUMU", "Tüm Hareketler (${activeAssignmentsList.size + records.size})"),
                Pair("AKTIF", "🟠 Okunuyor (${activeAssignmentsList.size})"),
                Pair("TAMAMLANAN", "🟢 İade Edilen (${records.size})")
            )

            tabs.forEach { (key, title) ->
                val isSelected = subTab == key
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { subTab = key },
                    color = if (isSelected) {
                        if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                    } else if (isDarkTheme) Color(0x30334E68) else Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else primaryText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 7.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Aktif Okumalar
            if (subTab == "TUMU" || subTab == "AKTIF") {
                if (activeAssignmentsList.isNotEmpty()) {
                    item {
                        Text(
                            text = "🟠 ŞU AN OKUNMAKTA OLAN KİTAPLAR",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706)
                        )
                    }

                    items(activeAssignmentsList) { (book, assignment) ->
                        val days = calculateDaysAgo(assignment.assignmentDate)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDarkTheme) Color(0x4078350F) else Color(0xFFFEF3C7)
                            ),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = assignment.studentName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = primaryText
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFD97706)
                                        ) {
                                            Text(
                                                text = "$days. gün",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "📖 ${book.name} (#${book.registrationNo})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                                    )
                                    Text(
                                        text = "Veriliş: ${formatTimestamp(assignment.assignmentDate)}",
                                        fontSize = 10.5.sp,
                                        color = secondaryText
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Button(
                                        onClick = { onReturnBook(book) },
                                        shape = RoundedCornerShape(6.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("İade Al", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { onCancelAssignment(book, assignment.studentId) },
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("İptal", fontSize = 11.sp, color = BirizError)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Tamamlanan Okumalar
            if (subTab == "TUMU" || subTab == "TAMAMLANAN") {
                if (filteredRecords.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "🟢 TAMAMLANAN OKUMA VE İADE KAYITLARI",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = BirizSuccess
                        )
                    }

                    items(filteredRecords, key = { it.id }) { record ->
                        val stars = LibraryPersistenceHelper.calculateReadingStars(record.pageCount)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDarkTheme) BirizDarkCardGlass else BirizCardBg
                            ),
                            border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = record.studentName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = primaryText
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = BirizGold.copy(alpha = 0.2f),
                                            border = BorderStroke(1.dp, BirizGold)
                                        ) {
                                            Text(
                                                text = "+$stars ⭐",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BirizGold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "📘 ${record.bookName} (${record.pageCount} Sayfa)",
                                        fontSize = 12.sp,
                                        color = primaryText
                                    )
                                    Text(
                                        text = "Süreç: ${formatTimestamp(record.startDate)} ➔ ${formatTimestamp(record.endDate)}",
                                        fontSize = 10.5.sp,
                                        color = secondaryText
                                    )
                                }

                                IconButton(
                                    onClick = { onDeleteRecord(record) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Kaydı Sil",
                                        tint = BirizError.copy(alpha = 0.8f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (activeAssignmentsList.isEmpty() && filteredRecords.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Henüz bir okuma hareketi gerçekleşmedi.",
                            fontSize = 13.sp,
                            color = secondaryText
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 2: OKUMA ANALİTİĞİ & ÖĞRENCİ KARNESİ
// -------------------------------------------------------------
@Composable
private fun ReadingAnalyticsView(
    books: List<Book>,
    records: List<ReadingRecord>,
    students: List<Student>,
    selectedStudentId: String?,
    isDarkTheme: Boolean,
    onSelectStudent: (String?) -> Unit
) {
    val primaryText = if (isDarkTheme) BirizDarkTextPrimary else BirizTextPrimary
    val secondaryText = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary

    // Filtrelenmiş veya Tüm Sınıf Kayıtları
    val displayRecords = remember(records, selectedStudentId) {
        if (selectedStudentId == null) records else records.filter { it.studentId == selectedStudentId }
    }

    // 4 Bento KPI
    val totalBooksRead = displayRecords.size
    val totalPagesRead = displayRecords.sumOf { it.pageCount }
    val totalReadingDays = remember(displayRecords) {
        displayRecords.sumOf { rec ->
            calculateDateDiffDays(rec.startDate, rec.endDate).coerceAtLeast(1)
        }
    }
    val avgPagesPerDay = remember(totalPagesRead, totalReadingDays) {
        if (totalReadingDays > 0) totalPagesRead / totalReadingDays else 0
    }

    // En Çok Okuyan Öğrenciler Sıralaması (Leaderboard)
    val readerRankings = remember(records, students) {
        students.map { student ->
            val stRecords = records.filter { it.studentId == student.id }
            val readCount = stRecords.size
            val pages = stRecords.sumOf { it.pageCount }
            val stars = stRecords.sumOf { LibraryPersistenceHelper.calculateReadingStars(it.pageCount) }
            val isCurrentlyReading = books.any { b -> b.activeAssignments.any { it.studentId == student.id } }
            Triple(student, readCount, Pair(pages, stars)) to isCurrentlyReading
        }.sortedByDescending { it.first.second } // okunan kitap sayısına göre sırala
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        // Öğrenci Filtreleme Şeridi
        Text(
            text = "👤 ÖĞRENCİ SEÇİMİ VE ANALİZ FİLTRESİ",
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = secondaryText
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tüm Sınıf Seçeneği
            val isAllSelected = selectedStudentId == null
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSelectStudent(null) },
                color = if (isAllSelected) {
                    if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                } else if (isDarkTheme) Color(0x35334E68) else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "🏫 Tüm Sınıf (${students.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isAllSelected) Color.White else primaryText,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }

            // Öğrenci Butonları
            students.sortedBy { it.studentNumber }.forEach { student ->
                val isSelected = selectedStudentId == student.id
                val readCount = records.count { it.studentId == student.id }
                val isCurrentlyReading = books.any { b -> b.activeAssignments.any { it.studentId == student.id } }

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelectStudent(student.id) },
                    color = if (isSelected) {
                        if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                    } else if (isDarkTheme) Color(0x25334E68) else Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) Color.Transparent else if (isDarkTheme) Color(0x4064748B) else BirizBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isCurrentlyReading) {
                            Text("🟡", fontSize = 9.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = "${student.studentNumber} ${student.fullName}",
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else primaryText
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) Color.White.copy(alpha = 0.25f) else BirizSuccess.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "$readCount",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else BirizSuccess,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4 Bento KPI Kartı (2x2 Grid)
        val selectedStudentObj = students.firstOrNull { it.id == selectedStudentId }
        Text(
            text = if (selectedStudentObj != null) "📊 ${selectedStudentObj.fullName} - Okuma Karnesi" else "📊 SINIF GENEL OKUMA PERFORMANSI",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KpiBentoCard(
                title = "Okunan Kitap",
                value = "$totalBooksRead",
                subtext = "Tamamlanan eser",
                icon = "📚",
                accentColor = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                isDark = isDarkTheme,
                modifier = Modifier.weight(1f)
            )
            KpiBentoCard(
                title = "Okunan Sayfa",
                value = "$totalPagesRead",
                subtext = "Toplam hacim",
                icon = "📄",
                accentColor = Color(0xFF7C3AED),
                isDark = isDarkTheme,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KpiBentoCard(
                title = "Toplam Süre",
                value = "$totalReadingDays Gün",
                subtext = "Kitap başında geçen",
                icon = "⏱️",
                accentColor = Color(0xFF059669),
                isDark = isDarkTheme,
                modifier = Modifier.weight(1f)
            )
            KpiBentoCard(
                title = "Günlük Hacim",
                value = "$avgPagesPerDay s/gün",
                subtext = "Ortalama tempo",
                icon = "📈",
                accentColor = Color(0xFFD97706),
                isDark = isDarkTheme,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sınıf Kitap Kurdu Liderlik Sıralaması
        Text(
            text = "🏆 SINIF KİTAP KURDU LİDERLERİ",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = primaryText
        )
        Spacer(modifier = Modifier.height(6.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkTheme) BirizDarkCardGlass else BirizCardBg
            ),
            border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                readerRankings.take(8).forEachIndexed { index, (data, isReading) ->
                    val (student, count, pageStar) = data
                    val (pages, stars) = pageStar

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = when (index) {
                                    0 -> "🥇"
                                    1 -> "🥈"
                                    2 -> "🥉"
                                    else -> "  ${index + 1}."
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = student.fullName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = primaryText
                                    )
                                    if (isReading) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("🟡", fontSize = 8.sp)
                                    }
                                }
                                Text(
                                    text = "No: ${student.studentNumber} • $pages Sayfa",
                                    fontSize = 10.5.sp,
                                    color = secondaryText
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = BirizSuccess.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "$count Kitap",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BirizSuccess,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = BirizGold.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "+$stars ⭐",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BirizGold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (index < 7 && index < readerRankings.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 2.dp),
                            color = if (isDarkTheme) Color(0x20FFFFFF) else Color(0xFFF1F5F9)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// BENTO KPI KART BİLEŞENİ
// -------------------------------------------------------------
@Composable
private fun KpiBentoCard(
    title: String,
    value: String,
    subtext: String,
    icon: String,
    accentColor: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) BirizDarkCardGlass else BirizCardBg
        ),
        border = BorderStroke(1.dp, if (isDark) BirizDarkCardBorder else BirizBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = if (isDark) BirizDarkTextSecondary else BirizTextSecondary
                )
                Text(text = icon, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                fontSize = 10.sp,
                color = if (isDark) BirizDarkTextMuted else BirizTextSecondary
            )
        }
    }
}

// -------------------------------------------------------------
// BİLEŞEN: KULLANIŞLI & ŞIK TARİH SEÇİCİ (BUGÜN / DÜN / TAKVİM)
// -------------------------------------------------------------
@Composable
private fun BirizDatePickerField(
    label: String,
    dateStr: String,
    onDateChange: (String) -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val todayStr = remember { SimpleDateFormat("dd.MM.yyyy", Locale("tr")).format(Date()) }
    val yesterdayStr = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, -1)
        SimpleDateFormat("dd.MM.yyyy", Locale("tr")).format(cal.time)
    }

    val openCalendar = {
        val cal = Calendar.getInstance()
        try {
            val parsed = SimpleDateFormat("dd.MM.yyyy", Locale("tr")).parse(dateStr)
            if (parsed != null) cal.time = parsed
        } catch (_: Exception) {}
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedCal = Calendar.getInstance()
                selectedCal.set(year, month, dayOfMonth)
                onDateChange(SimpleDateFormat("dd.MM.yyyy", Locale("tr")).format(selectedCal.time))
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = dateStr,
            onValueChange = onDateChange,
            label = { Text(label, fontSize = 12.sp) },
            trailingIcon = {
                IconButton(onClick = openCalendar) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Takvimden Seç",
                        tint = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        )

        // Hızlı Tarih Seçim Butonları
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isToday = dateStr == todayStr
            val isYesterday = dateStr == yesterdayStr

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isToday) (if (isDarkTheme) BirizTurquoise else BirizNavyPrimary) else (if (isDarkTheme) Color(0x35334E68) else Color(0xFFF1F5F9)),
                border = BorderStroke(1.dp, if (isToday) Color.Transparent else if (isDarkTheme) Color(0x5064748B) else BirizBorder),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onDateChange(todayStr) }
            ) {
                Text(
                    text = "📍 Bugün",
                    fontSize = 11.5.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                    color = if (isToday) Color.White else if (isDarkTheme) BirizDarkTextPrimary else BirizTextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isYesterday) (if (isDarkTheme) BirizTurquoise else BirizNavyPrimary) else (if (isDarkTheme) Color(0x35334E68) else Color(0xFFF1F5F9)),
                border = BorderStroke(1.dp, if (isYesterday) Color.Transparent else if (isDarkTheme) Color(0x5064748B) else BirizBorder),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onDateChange(yesterdayStr) }
            ) {
                Text(
                    text = "⏪ Dün",
                    fontSize = 11.5.sp,
                    fontWeight = if (isYesterday) FontWeight.Bold else FontWeight.Medium,
                    color = if (isYesterday) Color.White else if (isDarkTheme) BirizDarkTextPrimary else BirizTextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isDarkTheme) Color(0x2506B6D4) else Color(0xFFE0F2FE),
                border = BorderStroke(1.dp, if (isDarkTheme) BirizTurquoise.copy(alpha = 0.5f) else Color(0xFFBAE6FD)),
                modifier = Modifier
                    .weight(1.2f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = openCalendar)
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "🗓️ Takvim",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// DİYALOG: KİTAP EKLE / DÜZENLE
// -------------------------------------------------------------
@Composable
private fun AddEditBookDialog(
    existingBooks: List<Book>,
    students: List<Student>,
    bookToEdit: Book?,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, author: String, pageCount: Int, registrationNo: Int, assignStudentId: String?, assignDate: String?) -> Unit
) {
    var name by remember { mutableStateOf(bookToEdit?.name ?: "") }
    var author by remember { mutableStateOf(bookToEdit?.author ?: "") }
    var pageCountStr by remember { mutableStateOf(if ((bookToEdit?.pageCount ?: 0) > 0) bookToEdit!!.pageCount.toString() else "") }

    // Otomatik Demirbaş No (Boşlukları arar ve en küçük boştaki numarayı atar)
    val autoRegNo = remember(existingBooks, bookToEdit) {
        bookToEdit?.registrationNo ?: LibraryPersistenceHelper.findNextAvailableRegistrationNo(existingBooks)
    }

    // Sayfa sayısı zorunlu kontrolü
    val pageCount = pageCountStr.toIntOrNull() ?: 0
    val isPageValid = pageCount > 0

    // Öğrenciye Hemen Ver Seçeneği
    var assignImmediately by remember { mutableStateOf(false) }
    var selectedStudentId by remember { mutableStateOf<String?>(null) }
    val todayDateStr = remember { SimpleDateFormat("dd.MM.yyyy", Locale("tr")).format(Date()) }
    var assignmentDateStr by remember { mutableStateOf(todayDateStr) }

    // Mükerrer Kontrolü (Büyük-küçük harf duyarsız Türkçe)
    val isDuplicate = remember(name, bookToEdit, existingBooks) {
        val trimmed = name.trim()
        trimmed.isNotEmpty() && existingBooks.any { b ->
            b.id != bookToEdit?.id && b.name.trim().equals(trimmed, ignoreCase = true)
        }
    }

    // Akıllı Tamamlama Önerileri
    val nameSuggestions = remember(name, existingBooks) {
        if (name.length < 2) emptyList() else {
            existingBooks
                .map { it.name }
                .distinct()
                .filter { it.contains(name, ignoreCase = true) && !it.equals(name.trim(), ignoreCase = true) }
                .take(3)
        }
    }

    val primaryText = if (isDarkTheme) BirizDarkTextPrimary else BirizTextPrimary
    val secondaryText = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
                .padding(vertical = 10.dp)
                .clip(RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            color = if (isDarkTheme) Color(0xFF0F1E38) else Color.White,
            border = BorderStroke(1.2.dp, if (isDarkTheme) BirizDarkCardBorder else Color(0xFFE2E8F0)),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                // 1. SABİT BAŞLIK (Her zaman en üstte sabit)
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
                            color = if (isDarkTheme) Color(0x350D9488) else Color(0xFFCCFBF1),
                            border = BorderStroke(1.dp, if (isDarkTheme) BirizTurquoise else Color(0xFF5EEAD4)),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = null,
                                    tint = if (isDarkTheme) BirizTurquoise else Color(0xFF0F766E),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = if (bookToEdit == null) "Yeni Kitap Ekle" else "Kitabı Düzenle",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = primaryText
                            )
                            Text(
                                text = if (bookToEdit == null) "Sınıf kütüphanesine yeni bir eser kazandırın" else "Kitap bilgilerini güncelleyin",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = secondaryText
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
                            tint = secondaryText,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                HorizontalDivider(
                    color = if (isDarkTheme) Color(0x3064748B) else Color(0xFFF1F5F9),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 10.dp)
                )

                // 2. KAYDIRILABİLİR FORM GÖVDESİ (Ortada akıcı kaydırma)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Kitap Adı
                    Column {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Kitap Adı * (Zorunlu)", fontWeight = FontWeight.SemiBold) },
                            placeholder = { Text("Örn: Küçük Prens, Momo, Nutuk", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Book, contentDescription = null, tint = BirizTurquoise, modifier = Modifier.size(19.dp))
                            },
                            isError = isDuplicate,
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BirizTurquoise,
                                unfocusedBorderColor = if (isDarkTheme) Color(0xFF334155) else Color(0xFFCBD5E1),
                                focusedLabelColor = BirizTurquoise
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (isDuplicate) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "⚠️ Bu isimde bir kitap kitaplığınızda zaten kayıtlı!",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BirizError,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }

                        // Öneri çipleri
                        if (nameSuggestions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                nameSuggestions.forEach { suggestion ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isDarkTheme) Color(0x3038BDF8) else Color(0xFFE0F2FE),
                                        border = BorderStroke(1.dp, if (isDarkTheme) Color(0x4038BDF8) else Color(0xFFBAE6FD)),
                                        modifier = Modifier.clickable { name = suggestion }
                                    ) {
                                        Text(
                                            text = "💡 $suggestion",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isDarkTheme) Color(0xFF7DD3FC) else Color(0xFF0369A1),
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Yazar Adı
                    OutlinedTextField(
                        value = author,
                        onValueChange = { author = it },
                        label = { Text("Yazar Adı (İsteğe bağlı)", fontWeight = FontWeight.SemiBold) },
                        placeholder = { Text("Örn: Antoine de Saint-Exupéry", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.PersonOutline, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(19.dp))
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BirizTurquoise,
                            unfocusedBorderColor = if (isDarkTheme) Color(0xFF334155) else Color(0xFFCBD5E1),
                            focusedLabelColor = BirizTurquoise
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Sayfa Sayısı (ZORUNLU)
                    Column {
                        OutlinedTextField(
                            value = pageCountStr,
                            onValueChange = { pageCountStr = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Sayfa Sayısı * (Zorunlu)", fontWeight = FontWeight.SemiBold) },
                            placeholder = { Text("Örn: 41", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.AutoStories, contentDescription = null, tint = BirizAmber, modifier = Modifier.size(19.dp))
                            },
                            isError = pageCountStr.isNotBlank() && !isPageValid,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BirizTurquoise,
                                unfocusedBorderColor = if (isDarkTheme) Color(0xFF334155) else Color(0xFFCBD5E1),
                                focusedLabelColor = BirizTurquoise
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        if (isPageValid) {
                            val stars = LibraryPersistenceHelper.calculateReadingStars(pageCount)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDarkTheme) Color(0x35F59E0B) else Color(0xFFFFFBEB),
                                border = BorderStroke(1.dp, if (isDarkTheme) Color(0x60F59E0B) else Color(0xFFFDE68A)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("⭐", fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "$pageCount sayfa için +$stars Kitap Kurdu yıldızı verilecektir.",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkTheme) Color(0xFFFDE68A) else Color(0xFFB45309)
                                    )
                                }
                            }
                        } else if (pageCountStr.isNotBlank()) {
                            Text(
                                text = "⚠️ Sayfa sayısı 0'dan büyük bir tam sayı olmalıdır.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BirizError,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        } else {
                            Text(
                                text = "Zorunlu alan • Her 10 sayfaya 1 yıldız yukarı yuvarlanarak hesaplanır.",
                                fontSize = 10.5.sp,
                                color = secondaryText,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }

                    // Kitap Ekleme Anında Öğrenciye Hemen Ver (Ödünç Başlat)
                    if (bookToEdit == null && students.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (assignImmediately) {
                                if (isDarkTheme) Color(0x300D9488) else Color(0xFFF0FDFA)
                            } else {
                                if (isDarkTheme) Color(0x18FFFFFF) else Color(0xFFF8FAFC)
                            },
                            border = BorderStroke(
                                1.dp,
                                if (assignImmediately) BirizTurquoise else (if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0))
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { assignImmediately = !assignImmediately }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
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
                                            shape = CircleShape,
                                            color = if (assignImmediately) BirizTurquoise else Color(0xFF94A3B8).copy(alpha = 0.2f),
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.BookmarkAdd,
                                                    contentDescription = null,
                                                    tint = if (assignImmediately) Color.White else Color.Gray,
                                                    modifier = Modifier.size(17.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column {
                                            Text(
                                                text = "Öğrenciye Hemen Ödünç Ver",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = primaryText
                                            )
                                            Text(
                                                text = "Kitap eklenirken ilk teslim kaydını başlatın",
                                                fontSize = 10.5.sp,
                                                color = secondaryText
                                            )
                                        }
                                    }

                                    Switch(
                                        checked = assignImmediately,
                                        onCheckedChange = { assignImmediately = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = BirizTurquoise
                                        )
                                    )
                                }

                                if (assignImmediately) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(
                                        color = if (isDarkTheme) Color(0x2564748B) else Color(0xFFE2E8F0),
                                        thickness = 0.8.dp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    var studentFilterText by remember { mutableStateOf("") }
                                    OutlinedTextField(
                                        value = studentFilterText,
                                        onValueChange = { studentFilterText = it },
                                        placeholder = { Text("Öğrenci adı veya no ara...", fontSize = 11.5.sp) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp), tint = secondaryText)
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    val filteredSt = students.filter {
                                        studentFilterText.isBlank() ||
                                                it.fullName.contains(studentFilterText, ignoreCase = true) ||
                                                it.studentNumber.toString() == studentFilterText
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isDarkTheme) Color(0x20000000) else Color.White,
                                        border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 70.dp, max = 110.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .verticalScroll(rememberScrollState())
                                        ) {
                                            filteredSt.forEachIndexed { stIndex, st ->
                                                val isSelected = selectedStudentId == st.id
                                                val isGirl = st.gender.equals("KIZ", ignoreCase = true)

                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(if (isSelected) BirizTurquoise.copy(alpha = 0.15f) else Color.Transparent)
                                                        .clickable { selectedStudentId = st.id }
                                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Surface(
                                                                shape = CircleShape,
                                                                color = if (isGirl) Color(0xFFFCE7F3) else Color(0xFFE0F2FE),
                                                                modifier = Modifier.size(24.dp)
                                                            ) {
                                                                Box(contentAlignment = Alignment.Center) {
                                                                    Text(
                                                                        text = "#${st.studentNumber}",
                                                                        fontSize = 9.5.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = if (isGirl) Color(0xFFDB2777) else Color(0xFF0284C7)
                                                                    )
                                                                }
                                                            }
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text(
                                                                text = st.fullName,
                                                                fontSize = 12.sp,
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                                color = primaryText
                                                            )
                                                        }

                                                        if (isSelected) {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = "Seçildi",
                                                                tint = BirizTurquoise,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    }
                                                }

                                                if (stIndex < filteredSt.size - 1) {
                                                    HorizontalDivider(
                                                        color = if (isDarkTheme) Color(0x15FFFFFF) else Color(0xFFF1F5F9),
                                                        thickness = 0.6.dp
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (selectedStudentId == null) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "👆 Lütfen yukarıdaki listeden kitabı teslim edeceğiniz öğrenciye dokunun.",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BirizAmber,
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Teslim Tarihi Seçici
                                    BirizDatePickerField(
                                        label = "Teslim Tarihi",
                                        dateStr = assignmentDateStr,
                                        onDateChange = { assignmentDateStr = it },
                                        isDarkTheme = isDarkTheme
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. HER ZAMAN GÖRÜNÜR SABİT ALT BUTON ŞERİDİ (Pinned Bottom Actions)
                HorizontalDivider(
                    color = if (isDarkTheme) Color(0x3064748B) else Color(0xFFF1F5F9),
                    thickness = 1.dp,
                    modifier = Modifier.padding(top = 10.dp, bottom = 10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text(
                            text = "Vazgeç",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = secondaryText
                        )
                    }

                    val canSave = name.isNotBlank() && !isDuplicate && isPageValid && (!assignImmediately || selectedStudentId != null)

                    Button(
                        onClick = {
                            val assignId = if (assignImmediately) selectedStudentId else null
                            val assignDate = if (assignImmediately && assignId != null) assignmentDateStr else null
                            onSave(name.trim(), author.trim(), pageCount, autoRegNo, assignId, assignDate)
                        },
                        enabled = canSave,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                            disabledContainerColor = if (isDarkTheme) Color(0xFF334155) else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier
                            .weight(1.4f)
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (bookToEdit == null) "Kitabı Kaydet" else "Güncelle",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// DİYALOG: KİTAP ÖDÜNÇ VER (TESLİM ET)
// -------------------------------------------------------------
@Composable
private fun BorrowBookDialog(
    book: Book,
    students: List<Student>,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (selectedStudents: List<Student>, date: String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val selectedStudents = remember { mutableStateOf<Set<String>>(emptySet()) }
    val todayDateStr = remember { SimpleDateFormat("dd.MM.yyyy", Locale("tr")).format(Date()) }
    var assignmentDate by remember { mutableStateOf(todayDateStr) }

    val filteredStudents = remember(students, searchQuery) {
        if (searchQuery.isBlank()) students else {
            students.filter {
                it.fullName.contains(searchQuery, ignoreCase = true) ||
                        it.studentNumber.toString() == searchQuery
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "📖 Kitap Teslim Et", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        text = "${book.name} (${book.pageCount} Sayfa)",
                        fontSize = 12.sp,
                        color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isDarkTheme) Color(0x40334E68) else Color(0xFFE2E8F0)
                ) {
                    Text(
                        text = "#${book.registrationNo}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Tarih Seçici Bileşeni
                BirizDatePickerField(
                    label = "Teslim / Veriliş Tarihi",
                    dateStr = assignmentDate,
                    onDateChange = { assignmentDate = it },
                    isDarkTheme = isDarkTheme
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                Text(
                    text = "Kitabı teslim alacak öğrenciyi seçin (Grup için birden fazla seçilebilir):",
                    fontSize = 11.5.sp
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Öğrenci adı veya numarası...", fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Öğrenci Listesi
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredStudents, key = { it.id }) { st ->
                        val isChecked = selectedStudents.value.contains(st.id)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val current = selectedStudents.value.toMutableSet()
                                    if (isChecked) current.remove(st.id) else current.add(st.id)
                                    selectedStudents.value = current
                                },
                            color = if (isChecked) BirizTurquoise.copy(alpha = 0.15f) else Color.Transparent,
                            border = BorderStroke(1.dp, if (isChecked) BirizTurquoise else Color(0x3064748B))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        val current = selectedStudents.value.toMutableSet()
                                        if (checked) current.add(st.id) else current.remove(st.id)
                                        selectedStudents.value = current
                                    }
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${st.studentNumber} - ${st.fullName}",
                                    fontSize = 12.5.sp,
                                    fontWeight = if (isChecked) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val chosen = students.filter { selectedStudents.value.contains(it.id) }
                    onConfirm(chosen, assignmentDate)
                },
                enabled = selectedStudents.value.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                )
            ) {
                Text("Teslim Et (${selectedStudents.value.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

// -------------------------------------------------------------
// DİYALOG: KİTAP İADE ALMA VE YILDIZ KAZANDIRMA
// -------------------------------------------------------------
@Composable
private fun ReturnBookDialog(
    book: Book,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (studentId: String, studentName: String, startDate: Long, returnDate: String) -> Unit
) {
    val activeList = book.activeAssignments
    var selectedStudentId by remember {
        mutableStateOf(activeList.firstOrNull()?.studentId ?: book.currentStudentId.orEmpty())
    }

    val todayDateStr = remember { SimpleDateFormat("dd.MM.yyyy", Locale("tr")).format(Date()) }
    var returnDateStr by remember { mutableStateOf(todayDateStr) }

    val activeItem = activeList.firstOrNull { it.studentId == selectedStudentId }
    val studentName = activeItem?.studentName ?: book.currentStudentName ?: "Öğrenci"
    val startDate: Long = activeItem?.assignmentDate ?: book.assignmentDate ?: System.currentTimeMillis()

    val pageCount = book.pageCount
    val earnedStars = LibraryPersistenceHelper.calculateReadingStars(pageCount)

    val returnDateMs = remember(returnDateStr) {
        try {
            SimpleDateFormat("dd.MM.yyyy", Locale("tr")).parse(returnDateStr)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) { System.currentTimeMillis() }
    }
    val readingDays = remember(startDate, returnDateMs) {
        calculateDateDiffDays(startDate, returnDateMs).coerceAtLeast(1)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "📥 Kitap İade Al", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        text = "${book.name} (${book.pageCount} Sayfa)",
                        fontSize = 12.sp,
                        color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isDarkTheme) Color(0x40334E68) else Color(0xFFE2E8F0)
                ) {
                    Text(
                        text = "#${book.registrationNo}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (activeList.size > 1) {
                    Text("İade eden öğrenciyi seçin:", fontSize = 12.sp)
                    activeList.forEach { item ->
                        val isSelected = selectedStudentId == item.studentId
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { selectedStudentId = item.studentId },
                            color = if (isSelected) BirizTurquoise.copy(alpha = 0.2f) else Color.Transparent,
                            border = BorderStroke(1.dp, if (isSelected) BirizTurquoise else Color(0x3064748B))
                        ) {
                            Text(
                                text = item.studentName,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDarkTheme) Color(0x30334E68) else Color(0xFFF1F5F9),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "👤 Okuyan Öğrenci: $studentName",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                // İade Alınış Tarihi Seçici
                BirizDatePickerField(
                    label = "İade Alınış Tarihi",
                    dateStr = returnDateStr,
                    onDateChange = { returnDateStr = it },
                    isDarkTheme = isDarkTheme
                )

                // Süreç ve Yıldız Teşvik Kutusu
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = BirizGold.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, BirizGold)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⭐", fontSize = 28.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Kitap Kurdu Motivasyon Yıldızı",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                color = Color(0xFFB45309)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Süreç: ${formatTimestamp(startDate)} ➔ $returnDateStr ($readingDays gün)",
                                fontSize = 11.sp,
                                color = Color(0xFF92400E)
                            )
                            Text(
                                text = "Tebrikler! Öğrencinin hanesine +$earnedStars ⭐ eklenecektir.",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFB45309)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedStudentId, studentName, startDate, returnDateStr)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706))
            ) {
                Text("İade Al ve +$earnedStars ⭐ Ver", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Vazgeç")
            }
        }
    )
}

// -------------------------------------------------------------
// DİYALOG: TÜM SINIF OKUDU TOPLU OKUMA İŞLEMİ
// -------------------------------------------------------------
@Composable
private fun BatchClassReadDialog(
    books: List<Book>,
    students: List<Student>,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Book, String) -> Unit
) {
    var selectedBookId by remember { mutableStateOf(books.firstOrNull()?.id.orEmpty()) }
    val todayDateStr = remember { SimpleDateFormat("dd.MM.yyyy", Locale("tr")).format(Date()) }
    var readingDate by remember { mutableStateOf(todayDateStr) }

    val selectedBook = books.firstOrNull { it.id == selectedBookId }
    val earnedStars = LibraryPersistenceHelper.calculateReadingStars(selectedBook?.pageCount ?: 0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "👥 Sınıfça Okuma Kaydı", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Bütün sınıfın birlikte okuduğu bir eseri tek tıkla tüm öğrencilerin (${students.size} öğrenci) karnesine işleyebilirsiniz.",
                    fontSize = 12.sp
                )

                Text(text = "Okunan Kitap:", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(books) { book ->
                        val isSelected = selectedBookId == book.id
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { selectedBookId = book.id },
                            color = if (isSelected) BirizTurquoise.copy(alpha = 0.2f) else Color.Transparent,
                            border = BorderStroke(1.dp, if (isSelected) BirizTurquoise else Color(0x3064748B))
                        ) {
                            Text(
                                text = "#${book.registrationNo} - ${book.name} (${book.pageCount} Sayfa)",
                                fontSize = 12.sp,
                                modifier = Modifier.padding(8.dp),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Tarih Seçici Bileşeni
                BirizDatePickerField(
                    label = "Okunma Tarihi",
                    dateStr = readingDate,
                    onDateChange = { readingDate = it },
                    isDarkTheme = isDarkTheme
                )

                if (selectedBook != null) {
                    Text(
                        text = "💡 Her öğrenci +$earnedStars ⭐ kazanacaktır.",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BirizGold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedBook != null) {
                        onConfirm(selectedBook, readingDate)
                    }
                },
                enabled = selectedBook != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDarkTheme) BirizTurquoise else BirizNavyPrimary
                )
            ) {
                Text("Tüm Sınıfa Dağıt")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
}

// -------------------------------------------------------------
// DİYALOG: RAPORLAMA VE DIŞA AKTARMA (EXCEL / PDF)
// -------------------------------------------------------------
@Composable
private fun ExportLibraryDialog(
    classRoom: ClassRoom,
    books: List<Book>,
    records: List<ReadingRecord>,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "📤 Kitaplık Raporu & Dışa Aktarma", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Kitaplık listesini ve okuma hareketlerini resmi Excel tablosu veya yazdırılabilir A4 PDF belgesi olarak indirin:",
                    fontSize = 12.5.sp
                )

                // 1. Excel Kitaplık Listesi
                OutlinedButton(
                    onClick = {
                        val uri = ExcelPdfHelper.exportBooksExcel(context, classRoom, books)
                        ExcelPdfHelper.shareFile(
                            context,
                            uri,
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            "Kitaplık Listesi Excel Tablosu"
                        )
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("📊 Kitaplık Listesi (Excel .xlsx)")
                }

                // 2. Excel Okuma Geçmişi
                OutlinedButton(
                    onClick = {
                        val uri = ExcelPdfHelper.exportReadingRecordsExcel(context, classRoom, records)
                        ExcelPdfHelper.shareFile(
                            context,
                            uri,
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            "Okuma Geçmişi Excel Tablosu"
                        )
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("📋 Okuma & İade Geçmişi (Excel .xlsx)")
                }

                // 3. Resmi A4 PDF Raporu
                Button(
                    onClick = {
                        val uri = ExcelPdfHelper.exportLibraryPdf(context, classRoom, books, records)
                        ExcelPdfHelper.shareFile(
                            context,
                            uri,
                            "application/pdf",
                            "Sınıf Kitaplığı ve Okuma Raporu (PDF)"
                        )
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BirizNavyPrimary)
                ) {
                    Text("📄 Resmi A4 Kitaplık Raporu (PDF Yazdır)", color = Color.White)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat")
            }
        }
    )
}

// -------------------------------------------------------------
// YARDIMCI TARİH FONKSİYONLARI
// -------------------------------------------------------------
private fun calculateDaysAgo(timestamp: Long?): Long {
    if (timestamp == null || timestamp <= 0L) return 1
    val diffMs = System.currentTimeMillis() - timestamp
    return (diffMs / (1000 * 60 * 60 * 24)).coerceAtLeast(1)
}

private fun calculateDateDiffDays(startDate: Long, endDate: Long): Int {
    val diffMs = endDate - startDate
    return ((diffMs / (1000 * 60 * 60 * 24)).toInt()).coerceAtLeast(1)
}

private fun formatTimestamp(timestamp: Long?): String {
    if (timestamp == null || timestamp <= 0L) return "-"
    return SimpleDateFormat("dd.MM.yyyy", Locale("tr")).format(Date(timestamp))
}
