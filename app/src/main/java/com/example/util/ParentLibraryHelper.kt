package com.example.util

import com.example.data.model.Book
import com.example.data.model.ReadingRecord
import java.util.Locale

enum class ParentLibraryTab(
    val title: String,
    val emoji: String,
    val testTag: String
) {
    READING("Okuyor", "📖", "parent_library_tab_reading"),
    HISTORY("Okuma Geçmişi", "📕", "parent_library_tab_history"),
    CLASS_LIBRARY("Sınıf Kitaplığı", "📚", "parent_library_tab_inventory")
}

enum class ParentHistorySortOrder(val label: String) {
    NEWEST("En Yeni"),
    MOST_STARS("En Çok Yıldız"),
    MOST_PAGES("En Çok Sayfa")
}

enum class ParentInventoryFilter(val label: String) {
    ALL("Tümü"),
    ON_SHELF("Rafta"),
    COMPLETED_BY_CHILD("Okundu"),
    BORROWED("Ödünçte")
}

enum class ParentBookStatus {
    MY_CHILD_READING,
    COMPLETED_BY_CHILD,
    BORROWED_BY_OTHER,
    ON_SHELF
}

data class ParentInventoryCounts(
    val all: Int,
    val onShelf: Int,
    val completedByChild: Int,
    val borrowed: Int
)

/**
 * Veli Kitaplık ekranındaki sekmeler (Okuyor, Okuma Geçmişi, Sınıf Kitaplığı) için
 * KVKK uyumlu durum hesaplama, Türkçe duyarlı arama, filtreleme ve sıralama yardımcısı.
 */
object ParentLibraryHelper {

    private val trLocale = Locale("tr")

    fun resolveActiveReadingBooks(classBooks: List<Book>, studentId: String): List<Book> {
        if (studentId.isBlank()) return emptyList()
        return classBooks
            .distinctBy { it.id }
            .filter { book ->
                book.activeAssignments.any { it.studentId == studentId } || book.currentStudentId == studentId
            }
            .sortedWith(
                compareByDescending<Book> { book ->
                    val assignDate = book.activeAssignments.firstOrNull { it.studentId == studentId }?.assignmentDate
                        ?: book.assignmentDate
                        ?: 0L
                    assignDate
                }.thenBy { it.registrationNo }
            )
    }

    fun isBookCompletedByStudent(book: Book, studentRecords: List<ReadingRecord>): Boolean {
        val cleanBookName = book.name.trim().lowercase(trLocale)
        return studentRecords.any { rec ->
            rec.bookId == book.id ||
                (cleanBookName.isNotEmpty() && rec.bookName.trim().lowercase(trLocale) == cleanBookName)
        }
    }

    fun resolveBookStatus(
        book: Book,
        studentId: String,
        studentRecords: List<ReadingRecord>
    ): ParentBookStatus {
        val isMyChildReading = studentId.isNotBlank() &&
            (book.activeAssignments.any { it.studentId == studentId } || book.currentStudentId == studentId)
        if (isMyChildReading) return ParentBookStatus.MY_CHILD_READING

        if (isBookCompletedByStudent(book, studentRecords)) {
            return ParentBookStatus.COMPLETED_BY_CHILD
        }

        val isAnotherChildReading = book.activeAssignments.any { it.studentId != studentId } ||
            (!book.currentStudentId.isNullOrBlank() && book.currentStudentId != studentId) ||
            book.status.trim().equals("Okunuyor", ignoreCase = true)

        return if (isAnotherChildReading) {
            ParentBookStatus.BORROWED_BY_OTHER
        } else {
            ParentBookStatus.ON_SHELF
        }
    }

    fun computeInventoryCounts(
        classBooks: List<Book>,
        studentId: String,
        studentRecords: List<ReadingRecord>
    ): ParentInventoryCounts {
        val uniqueBooks = classBooks.distinctBy { it.id }
        var onShelf = 0
        var completed = 0
        var borrowed = 0

        for (book in uniqueBooks) {
            val isMyChildReading = studentId.isNotBlank() &&
                (book.activeAssignments.any { it.studentId == studentId } || book.currentStudentId == studentId)
            val isBorrowed = isMyChildReading ||
                book.activeAssignments.isNotEmpty() ||
                !book.currentStudentId.isNullOrBlank() ||
                book.status.trim().equals("Okunuyor", ignoreCase = true)

            if (isBookCompletedByStudent(book, studentRecords)) {
                completed++
            }
            if (isBorrowed) {
                borrowed++
            } else {
                onShelf++
            }
        }

        return ParentInventoryCounts(
            all = uniqueBooks.size,
            onShelf = onShelf,
            completedByChild = completed,
            borrowed = borrowed
        )
    }

    fun filterAndSortHistory(
        records: List<ReadingRecord>,
        query: String,
        sortOrder: ParentHistorySortOrder
    ): List<ReadingRecord> {
        val cleanQuery = query.trim().lowercase(trLocale)
        val filtered = records
            .distinctBy { it.id }
            .filter { rec ->
                cleanQuery.isEmpty() || rec.bookName.lowercase(trLocale).contains(cleanQuery)
            }

        return when (sortOrder) {
            ParentHistorySortOrder.NEWEST -> filtered.sortedWith(
                compareByDescending<ReadingRecord> { maxOf(it.endDate, it.createdAt) }
                    .thenBy { it.bookName.lowercase(trLocale) }
            )
            ParentHistorySortOrder.MOST_STARS -> filtered.sortedWith(
                compareByDescending<ReadingRecord> { it.earnedStars }
                    .thenByDescending { maxOf(it.endDate, it.createdAt) }
            )
            ParentHistorySortOrder.MOST_PAGES -> filtered.sortedWith(
                compareByDescending<ReadingRecord> { it.pageCount }
                    .thenByDescending { maxOf(it.endDate, it.createdAt) }
            )
        }
    }

    fun filterInventoryBooks(
        classBooks: List<Book>,
        studentId: String,
        studentRecords: List<ReadingRecord>,
        query: String,
        filter: ParentInventoryFilter
    ): List<Book> {
        val cleanQuery = query.trim().lowercase(trLocale)
        val numericQuery = cleanQuery.removePrefix("#").trim()

        return classBooks
            .distinctBy { it.id }
            .filter { book ->
                val matchesQuery = if (cleanQuery.isEmpty()) {
                    true
                } else {
                    book.name.lowercase(trLocale).contains(cleanQuery) ||
                        book.author.lowercase(trLocale).contains(cleanQuery) ||
                        (numericQuery.isNotEmpty() && book.registrationNo.toString().contains(numericQuery))
                }
                if (!matchesQuery) return@filter false

                when (filter) {
                    ParentInventoryFilter.ALL -> true
                    ParentInventoryFilter.ON_SHELF -> {
                        val isMyChildReading = studentId.isNotBlank() &&
                            (book.activeAssignments.any { it.studentId == studentId } || book.currentStudentId == studentId)
                        val isBorrowed = isMyChildReading ||
                            book.activeAssignments.isNotEmpty() ||
                            !book.currentStudentId.isNullOrBlank() ||
                            book.status.trim().equals("Okunuyor", ignoreCase = true)
                        !isBorrowed
                    }
                    ParentInventoryFilter.COMPLETED_BY_CHILD -> isBookCompletedByStudent(book, studentRecords)
                    ParentInventoryFilter.BORROWED -> {
                        val isMyChildReading = studentId.isNotBlank() &&
                            (book.activeAssignments.any { it.studentId == studentId } || book.currentStudentId == studentId)
                        isMyChildReading ||
                            book.activeAssignments.isNotEmpty() ||
                            !book.currentStudentId.isNullOrBlank() ||
                            book.status.trim().equals("Okunuyor", ignoreCase = true)
                    }
                }
            }
            .sortedBy { it.registrationNo }
    }
}
