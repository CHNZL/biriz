package com.example.util

import android.content.Context
import android.util.Log
import com.example.data.model.ActiveAssignment
import com.example.data.model.Book
import com.example.data.model.ReadingRecord
import com.example.data.model.StarRewardRecord
import com.example.data.model.Student
import com.example.data.repository.StarRewardPersistenceHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Akıllı Sınıf Kitaplığı & Okuma Takip Kalıcı Veri ve İş Mantığı Yöneticisi (Firestore SSOT)
 */
object LibraryPersistenceHelper {
    private const val TAG = "LibraryPersistence"
    private const val PREFS_NAME = "biriz_library_prefs"
    private const val KEY_BOOKS_PREFIX = "lib_books_"
    private const val KEY_RECORDS_PREFIX = "lib_records_"
    private const val KEY_USER_ADDED_IDS_PREFIX = "lib_user_book_ids_"
    private val syncScope = CoroutineScope(Dispatchers.IO)

    /**
     * Önceki sürümlerde otomatik üretilmiş sanal/örnek kitap adları (Büyük harf Türkçe normalize)
     */
    private val LEGACY_SAMPLE_BOOK_NAMES_UPPER = setOf(
        "KÜÇÜK PRENS",
        "ŞEKER PORTAKALI",
        "DEDEMİN BAKKALI",
        "PAL SOKAĞI ÇOCUKLARI",
        "KELOĞLAN MASALLARI",
        "PETER PAN",
        "MOMO",
        "PİNOKYO",
        "80 GÜNDE DEVRİALEM",
        "SEKSEN GÜNDE DEVRİALEM",
        "BEYAZ DİŞ",
        "ÇOCUK KALBİ",
        "HEİDİ",
        "HEIDI",
        "ALİCE HARİKALAR DİYARINDA",
        "DEFİNE ADASI",
        "GULLİVER'İN GEZİLERİ",
        "DON KİŞOT",
        "OLİVER TWİST",
        "ROBİN HOOD",
        "TOM SAWYER",
        "POLLYANNA",
        "NASREDDİN HOCA FIKRALARI",
        "LA FONTAİNE MASALLARI"
    )

    private val LEGACY_SAMPLE_ID_PREFIXES = listOf(
        "sample_", "mock_", "default_", "seed_", "init_", "demo_", "test_",
        "book_1", "book_2", "book_3", "book_4", "book_5", "book_6", "book_7", "book_8", "book_9", "book_10"
    )

    private fun getFirestore(context: Context): FirebaseFirestore? {
        return try {
            FirestoreProvider.get(context)
        } catch (_: Exception) {
            null
        }
    }

    private fun getUserAddedBookIds(context: Context, classId: String): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet("$KEY_USER_ADDED_IDS_PREFIX$classId", emptySet()) ?: emptySet()
    }

    private fun markBookAsUserAdded(context: Context, classId: String, bookId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = getUserAddedBookIds(context, classId).toMutableSet()
        current.add(bookId)
        prefs.edit().putStringSet("$KEY_USER_ADDED_IDS_PREFIX$classId", current).apply()
    }

    /**
     * Bir kitabın sistem tarafından önceden eklenmiş sanal/örnek veya bozuk bir kayıt olup olmadığını tespit eder.
     */
    fun isVirtualOrCorruptBook(
        book: Book,
        explicitlyUserCreated: Boolean = false,
        knownUserIds: Set<String> = emptySet()
    ): Boolean {
        val trimmedName = book.name.trim()
        if (trimmedName.isBlank() || trimmedName.equals("Kitap", ignoreCase = true)) return true
        if (book.pageCount <= 0) return true
        val lowerId = book.id.lowercase()
        if (LEGACY_SAMPLE_ID_PREFIXES.any { lowerId == it || lowerId.startsWith(it) }) return true

        if (explicitlyUserCreated || knownUserIds.contains(book.id)) {
            return false
        }

        val upperName = TurkishTextHelper.toTurkishUpperCase(trimmedName)
        if (upperName in LEGACY_SAMPLE_BOOK_NAMES_UPPER) {
            return true
        }
        return false
    }

    /**
     * Kitap nesnesinin ödünç / raf durumunu iç tutarlılığa kavuşturur (Hayalet "Okunuyor" kayıtlarını temizler).
     */
    fun normalizeBookState(book: Book, validStudentIds: Set<String>? = null): Book {
        val cleanAssignments = book.activeAssignments
            .filter { a ->
                a.studentId.isNotBlank() &&
                    a.studentName.isNotBlank() &&
                    (validStudentIds == null || validStudentIds.isEmpty() || validStudentIds.contains(a.studentId))
            }
            .distinctBy { it.studentId }

        return if (cleanAssignments.isNotEmpty()) {
            val first = cleanAssignments.first()
            book.copy(
                name = book.name.trim(),
                author = book.author.trim(),
                status = "Okunuyor",
                activeAssignments = cleanAssignments,
                currentStudentId = first.studentId,
                currentStudentName = first.studentName,
                assignmentDate = first.assignmentDate
            )
        } else {
            book.copy(
                name = book.name.trim(),
                author = book.author.trim(),
                status = "Rafta",
                activeAssignments = emptyList(),
                currentStudentId = null,
                currentStudentName = null,
                assignmentDate = null
            )
        }
    }

    /**
     * Akıllı & Otomatik Demirbaş Numarası Bulucu
     * Mevcut kitapların registrationNo değerlerini inceler.
     * 1'den başlayarak ilk boşta olan en küçük sayıyı döndürür.
     */
    fun findNextAvailableRegistrationNo(books: List<Book>): Int {
        val existingNumbers = books.map { it.registrationNo }.filter { it > 0 }.toSet()
        var candidate = 1
        while (existingNumbers.contains(candidate)) {
            candidate++
        }
        return candidate
    }

    /**
     * Mükerrer Kitap Adı Kontrolü (Büyük/Küçük harf duyarsız Türkçe kontrol)
     */
    fun isDuplicateBookName(books: List<Book>, name: String, excludeBookId: String? = null): Boolean {
        val cleanName = TurkishTextHelper.toTurkishUpperCase(name.trim())
        if (cleanName.isBlank()) return false
        return books.any {
            it.id != excludeBookId && TurkishTextHelper.toTurkishUpperCase(it.name.trim()) == cleanName
        }
    }

    fun isBookNameDuplicate(books: List<Book>, name: String, excludeCurrentBookId: String? = null): Boolean {
        return isDuplicateBookName(books, name, excludeCurrentBookId)
    }

    /**
     * Sayfa Hacmine Göre Kitap Kurdu Yıldız Hesabı (Tavan Yuvarlama / Ceil):
     * Her 10 sayfaya 1 yıldız; 1-10 sayfa -> 1, 11-20 sayfa -> 2, 21-30 sayfa -> 3 vb.
     */
    fun calculateReadingStars(pageCount: Int): Int {
        if (pageCount <= 0) return 1
        return ((pageCount + 9) / 10).coerceAtLeast(1)
    }

    /**
     * Kitapları Yerel Hafızadan Yükle (Sanal/örnek ve mükerrer kitapları otomatik temizler)
     */
    fun loadBooks(context: Context, classId: String): List<Book> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("$KEY_BOOKS_PREFIX$classId", null)
        if (jsonStr.isNullOrBlank()) {
            return emptyList()
        }

        val knownUserIds = getUserAddedBookIds(context, classId)

        return try {
            val array = JSONArray(jsonStr)
            val rawList = mutableListOf<Pair<Book, Boolean>>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val userCreated = obj.optBoolean("userCreated", false)
                val book = deserializeBook(obj, defaultRegNo = i + 1)
                rawList.add(book to userCreated)
            }

            val purgedBookIds = mutableSetOf<String>()
            val validList = mutableListOf<Book>()
            val seenNames = mutableSetOf<String>()

            // Aktif ödüncü olan veya en son güncellenen gerçek kitaplara öncelik ver
            val sortedCandidates = rawList.sortedWith(
                compareByDescending<Pair<Book, Boolean>> { it.second || knownUserIds.contains(it.first.id) }
                    .thenByDescending { it.first.activeAssignments.isNotEmpty() }
                    .thenByDescending { it.first.updatedAt }
            )

            for ((book, userCreated) in sortedCandidates) {
                if (isVirtualOrCorruptBook(book, userCreated, knownUserIds)) {
                    purgedBookIds.add(book.id)
                    continue
                }
                val normName = TurkishTextHelper.toTurkishUpperCase(book.name.trim())
                if (!seenNames.add(normName)) {
                    purgedBookIds.add(book.id)
                    continue
                }
                validList.add(normalizeBookState(book))
            }

            val finalSorted = validList.sortedBy { it.registrationNo }
            if (purgedBookIds.isNotEmpty() || finalSorted.size != rawList.size) {
                saveBooksToLocalPrefsOnly(context, classId, finalSorted)
                if (classId.isNotBlank() && classId != "default" && purgedBookIds.isNotEmpty()) {
                    deleteBookDocsFromFirestore(context, classId, purgedBookIds)
                }
            }
            finalSorted
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveBooksToLocalPrefsOnly(context: Context, classId: String, books: List<Book>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val knownUserIds = getUserAddedBookIds(context, classId)
        val array = JSONArray()
        books.forEach { book ->
            array.put(serializeBook(book, userCreated = knownUserIds.contains(book.id)))
        }
        prefs.edit().putString("$KEY_BOOKS_PREFIX$classId", array.toString()).apply()
    }

    private fun saveRecordsToLocalPrefsOnly(context: Context, classId: String, records: List<ReadingRecord>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        records.forEach { r ->
            array.put(serializeReadingRecord(r))
        }
        prefs.edit().putString("$KEY_RECORDS_PREFIX$classId", array.toString()).apply()
    }

    /**
     * Kitapları Yerel Hafızaya ve Firestore'a Kaydet
     */
    fun saveBooks(context: Context, classId: String, books: List<Book>) {
        val normalized = books.map { normalizeBookState(it) }.sortedBy { it.registrationNo }
        saveBooksToLocalPrefsOnly(context, classId, normalized)

        // Firestore bulut yedeklemesi
        syncScope.launch {
            uploadBooksToFirestore(context, classId, normalized)
        }
    }

    /**
     * Tamamlanan Okuma Geçmişi Kayıtlarını Yükle
     */
    fun loadReadingRecords(context: Context, classId: String): List<ReadingRecord> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("$KEY_RECORDS_PREFIX$classId", null) ?: return emptyList()

        return try {
            val knownUserIds = getUserAddedBookIds(context, classId)
            val array = JSONArray(jsonStr)
            val list = mutableListOf<ReadingRecord>()
            val removedIds = mutableSetOf<String>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val rec = deserializeReadingRecord(obj)
                val upperBookName = TurkishTextHelper.toTurkishUpperCase(rec.bookName.trim())
                val isLegacySample = upperBookName in LEGACY_SAMPLE_BOOK_NAMES_UPPER && rec.bookId !in knownUserIds
                if (rec.id.isBlank() || rec.bookName.isBlank() || rec.studentId.isBlank() || isLegacySample) {
                    if (rec.id.isNotBlank()) removedIds.add(rec.id)
                    continue
                }
                list.add(rec)
            }
            val deduped = list.distinctBy { it.id }.sortedByDescending { it.endDate }
            if (removedIds.isNotEmpty() || deduped.size != list.size) {
                saveRecordsToLocalPrefsOnly(context, classId, deduped)
                if (classId.isNotBlank() && classId != "default" && removedIds.isNotEmpty()) {
                    deleteRecordDocsFromFirestore(context, classId, removedIds)
                }
            }
            deduped
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Okuma Geçmişi Kayıtlarını Kaydet
     */
    fun saveReadingRecords(context: Context, classId: String, records: List<ReadingRecord>) {
        val sorted = records.distinctBy { it.id }.sortedByDescending { it.endDate }
        saveRecordsToLocalPrefsOnly(context, classId, sorted)

        syncScope.launch {
            uploadRecordsToFirestore(context, classId, sorted)
        }
    }

    /**
     * Yeni Kitap Ekle (Öğretmen tarafından eklenen gerçek kitap olarak işaretlenir)
     */
    fun addBook(
        context: Context,
        classRoomId: String,
        name: String,
        author: String = "",
        pageCount: Int = 0,
        registrationNo: Int = 0,
        initialStudentId: String? = null,
        initialStudentName: String? = null,
        assignmentDate: String? = null
    ): Book {
        val books = loadBooks(context, classRoomId).toMutableList()
        val regNo = if (registrationNo > 0) registrationNo else findNextAvailableRegistrationNo(books)

        val assignDateMs = if (!assignmentDate.isNullOrBlank()) {
            try {
                java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale("tr")).parse(assignmentDate)?.time ?: System.currentTimeMillis()
            } catch (_: Exception) { System.currentTimeMillis() }
        } else System.currentTimeMillis()

        val assignments = mutableListOf<ActiveAssignment>()
        if (!initialStudentId.isNullOrBlank() && !initialStudentName.isNullOrBlank()) {
            assignments.add(
                ActiveAssignment(
                    studentId = initialStudentId,
                    studentName = initialStudentName,
                    assignmentDate = assignDateMs
                )
            )
        }

        val newBookId = UUID.randomUUID().toString()
        markBookAsUserAdded(context, classRoomId, newBookId)

        val newBook = normalizeBookState(
            Book(
                id = newBookId,
                registrationNo = regNo,
                name = name.trim(),
                author = author.trim(),
                pageCount = pageCount,
                status = if (assignments.isNotEmpty()) "Okunuyor" else "Rafta",
                activeAssignments = assignments,
                currentStudentId = initialStudentId,
                currentStudentName = initialStudentName,
                assignmentDate = if (assignments.isNotEmpty()) assignDateMs else null,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
        books.add(newBook)
        books.sortBy { it.registrationNo }
        saveBooks(context, classRoomId, books)
        return newBook
    }

    /**
     * Kitap Güncelle
     */
    fun updateBook(context: Context, classRoomId: String, book: Book) {
        markBookAsUserAdded(context, classRoomId, book.id)
        val books = loadBooks(context, classRoomId).toMutableList()
        val idx = books.indexOfFirst { it.id == book.id }
        if (idx != -1) {
            books[idx] = normalizeBookState(book.copy(updatedAt = System.currentTimeMillis()))
            books.sortBy { it.registrationNo }
            saveBooks(context, classRoomId, books)
        }
    }

    data class BookDeletionSummary(
        val deletedBookName: String,
        val deletedRecordsCount: Int,
        val revokedStarsCount: Int,
        val affectedStudentsCount: Int
    )

    data class ReadingHistoryDeletionSummary(
        val deletedRecordsCount: Int,
        val revokedStarsCount: Int,
        val affectedStudentsCount: Int,
        val cancelledActiveLoansCount: Int = 0
    )

    data class LibraryCleanupReport(
        val removedBooksCount: Int,
        val removedRecordsCount: Int,
        val fixedGhostLoansCount: Int,
        val revokedStarsCount: Int,
        val resequencedBooksCount: Int = 0
    ) {
        val hasChanges: Boolean
            get() = removedBooksCount > 0 || removedRecordsCount > 0 || fixedGhostLoansCount > 0 || revokedStarsCount > 0 || resequencedBooksCount > 0
    }

    /**
     * Kitap Sil ve İlgili Okuma Geçmişi ile Kazanılan Yıldızları Temizle (Cascade / Kademeli Silme)
     */
    fun deleteBook(context: Context, classRoomId: String, bookId: String): BookDeletionSummary {
        val books = loadBooks(context, classRoomId).toMutableList()
        val book = books.firstOrNull { it.id == bookId }
        val bookName = book?.name ?: "Kitap"
        books.removeAll { it.id == bookId }
        saveBooks(context, classRoomId, books)

        // 2. Bu kitaba ait tüm tamamlanmış okuma kayıtlarını (ReadingRecord) bul ve sil
        val records = loadReadingRecords(context, classRoomId).toMutableList()
        val relatedRecords = records.filter {
            it.bookId == bookId || (book != null && it.bookName.trim().equals(bookName.trim(), ignoreCase = true))
        }
        val relatedRecordIds = relatedRecords.map { it.id }.toSet()
        records.removeAll { it.id in relatedRecordIds }
        saveReadingRecords(context, classRoomId, records)

        if (classRoomId.isNotBlank() && classRoomId != "default") {
            deleteBookDocsFromFirestore(context, classRoomId, setOf(bookId))
            if (relatedRecordIds.isNotEmpty()) {
                deleteRecordDocsFromFirestore(context, classRoomId, relatedRecordIds)
            }
        }

        // 3. Yıldızlar Sınıfı kayıtlarından (StarRewardRecord) bu kitaba ait okuma yıldızlarını sil
        val starRecords = StarRewardPersistenceHelper.loadStarRecords(context, classRoomId)
        val starIdsToRemove = mutableSetOf<String>()
        var revokedStars = 0
        val affectedStudentIds = mutableSetOf<String>()

        relatedRecords.forEach { rec ->
            affectedStudentIds.add(rec.studentId)
            val stars = if (rec.earnedStars > 0) rec.earnedStars else calculateReadingStars(rec.pageCount)
            revokedStars += stars

            val matchingStar = starRecords.firstOrNull { sr ->
                !starIdsToRemove.contains(sr.id) &&
                sr.studentId == rec.studentId &&
                sr.categoryId == "cat_kitap_kurdu" &&
                (sr.note.contains(bookName, ignoreCase = true) || Math.abs(sr.timestamp - rec.endDate) < 60000L)
            }
            if (matchingStar != null) {
                starIdsToRemove.add(matchingStar.id)
            }
        }

        starRecords.filter { sr ->
            !starIdsToRemove.contains(sr.id) &&
            sr.categoryId == "cat_kitap_kurdu" &&
            sr.note.contains(bookName, ignoreCase = true)
        }.forEach { sr ->
            starIdsToRemove.add(sr.id)
        }

        if (starIdsToRemove.isNotEmpty()) {
            StarRewardPersistenceHelper.deleteRecordsByIds(context, classRoomId, starIdsToRemove)
        }

        return BookDeletionSummary(
            deletedBookName = bookName,
            deletedRecordsCount = relatedRecords.size,
            revokedStarsCount = revokedStars,
            affectedStudentsCount = affectedStudentIds.size
        )
    }

    /**
     * Çoklu Kitap Silme (Öğretmenin seçtiği birden fazla kitabı ve bağlı kayıtlarını tek seferde temizler)
     */
    fun deleteBooksBatch(context: Context, classRoomId: String, bookIds: Set<String>): BookDeletionSummary {
        if (bookIds.isEmpty()) return BookDeletionSummary("", 0, 0, 0)
        var totalRecords = 0
        var totalStars = 0
        val affectedStudents = mutableSetOf<String>()
        var firstBookName = ""

        val currentRecords = loadReadingRecords(context, classRoomId)
        currentRecords.filter { it.bookId in bookIds }.forEach { affectedStudents.add(it.studentId) }

        bookIds.forEachIndexed { idx, id ->
            val summary = deleteBook(context, classRoomId, id)
            if (idx == 0) firstBookName = summary.deletedBookName
            totalRecords += summary.deletedRecordsCount
            totalStars += summary.revokedStarsCount
        }

        return BookDeletionSummary(
            deletedBookName = if (bookIds.size == 1) firstBookName else "${bookIds.size} Kitap",
            deletedRecordsCount = totalRecords,
            revokedStarsCount = totalStars,
            affectedStudentsCount = affectedStudents.size
        )
    }

    /**
     * Tüm Kitaplık Listesini Tek Seferde Kalıcı Olarak Sil (Kitaplar, Aktif Ödünçler, Okuma Kayıtları ve Kitap Kurdu Yıldızları)
     */
    fun deleteAllBooks(context: Context, classRoomId: String): BookDeletionSummary {
        val books = loadBooks(context, classRoomId)
        val records = loadReadingRecords(context, classRoomId)
        val starRecords = StarRewardPersistenceHelper.loadStarRecords(context, classRoomId)

        val readingStarIds = starRecords.filter { it.categoryId == "cat_kitap_kurdu" }.map { it.id }.toSet()
        val revokedStars = starRecords.filter { it.id in readingStarIds }.sumOf { it.starCount }
            .coerceAtLeast(records.sumOf { if (it.earnedStars > 0) it.earnedStars else calculateReadingStars(it.pageCount) })
        val affectedStudents = records.map { it.studentId }.toSet().size

        // 1. Yerel tercihleri sıfırla (boş dizi yazarak senkron durumunu koru)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString("$KEY_BOOKS_PREFIX$classRoomId", "[]")
            .putString("$KEY_RECORDS_PREFIX$classRoomId", "[]")
            .remove("$KEY_USER_ADDED_IDS_PREFIX$classRoomId")
            .apply()

        // 2. İlgili Kitap Kurdu yıldızlarını yerelden ve Firestore'dan temizle
        if (readingStarIds.isNotEmpty()) {
            StarRewardPersistenceHelper.deleteRecordsByIds(context, classRoomId, readingStarIds)
        }
        StarRewardPersistenceHelper.deleteRecordsByCategory(context, classRoomId, "cat_kitap_kurdu")

        // 3. Firestore'daki tüm library_books ve reading_records belgelerini sorgulayarak topluca sil
        if (classRoomId.isNotBlank() && classRoomId != "default") {
            deleteAllCollectionDocsFromFirestore(context, classRoomId, "library_books", books.map { it.id }.toSet())
            deleteAllCollectionDocsFromFirestore(context, classRoomId, "reading_records", records.map { it.id }.toSet())
        }

        return BookDeletionSummary(
            deletedBookName = "${books.size} Kitap",
            deletedRecordsCount = records.size,
            revokedStarsCount = revokedStars,
            affectedStudentsCount = affectedStudents
        )
    }

    /**
     * Tüm Sınıfın Okuma Geçmişini Güvenlikli Olarak Sıfırla (Okuma Kayıtları + Kitap Kurdu Yıldızları)
     * Kitaplıktaki kitap envanteri korunur; isteğe bağlı olarak aktif ödünç kayıtları da rafa döndürülür.
     */
    fun deleteAllReadingRecords(
        context: Context,
        classRoomId: String,
        alsoCancelActiveLoans: Boolean = false
    ): ReadingHistoryDeletionSummary {
        val records = loadReadingRecords(context, classRoomId)
        val starRecords = StarRewardPersistenceHelper.loadStarRecords(context, classRoomId)
        val readingStarIds = starRecords.filter { it.categoryId == "cat_kitap_kurdu" }.map { it.id }.toSet()
        val revokedStars = starRecords.filter { it.id in readingStarIds }.sumOf { it.starCount }
            .coerceAtLeast(records.sumOf { if (it.earnedStars > 0) it.earnedStars else calculateReadingStars(it.pageCount) })
        val affectedStudents = records.map { it.studentId }.toSet().size

        // 1. Yerel okuma kayıtlarını sıfırla
        saveRecordsToLocalPrefsOnly(context, classRoomId, emptyList())

        // 2. Kitap Kurdu yıldızlarını yerelden ve Firestore'dan temizle
        if (readingStarIds.isNotEmpty()) {
            StarRewardPersistenceHelper.deleteRecordsByIds(context, classRoomId, readingStarIds)
        }
        StarRewardPersistenceHelper.deleteRecordsByCategory(context, classRoomId, "cat_kitap_kurdu")

        // 3. Firestore reading_records koleksiyonundaki tüm belgeleri temizle
        if (classRoomId.isNotBlank() && classRoomId != "default") {
            deleteAllCollectionDocsFromFirestore(context, classRoomId, "reading_records", records.map { it.id }.toSet())
        }

        // 4. İsteğe bağlı olarak aktif okunmakta olan ödünçleri rafa döndür
        var cancelledLoans = 0
        if (alsoCancelActiveLoans) {
            val currentBooks = loadBooks(context, classRoomId)
            val updatedBooks = currentBooks.map { book ->
                if (book.activeAssignments.isNotEmpty()) {
                    cancelledLoans += book.activeAssignments.size
                    normalizeBookState(
                        book.copy(
                            status = "Rafta",
                            activeAssignments = emptyList(),
                            currentStudentId = null,
                            currentStudentName = null,
                            assignmentDate = null,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                } else book
            }
            if (cancelledLoans > 0) {
                saveBooks(context, classRoomId, updatedBooks)
            }
        }

        return ReadingHistoryDeletionSummary(
            deletedRecordsCount = records.size,
            revokedStarsCount = revokedStars,
            affectedStudentsCount = affectedStudents,
            cancelledActiveLoansCount = cancelledLoans
        )
    }

    /**
     * Kitaplıktaki eski, sahte/sanal, mükerrer kitapları, silinmiş öğrencilere ait askıda kalmış ödünçleri
     * ve karşılığı olmayan okuma kayıtlarını otomatik temizler ve senkronize eder.
     */
    fun cleanupStaleAndOrphanedLibraryData(
        context: Context,
        classId: String,
        validStudents: List<Student>,
        resequenceNumbers: Boolean = false
    ): LibraryCleanupReport {
        if (classId.isBlank() || classId == "default") {
            return LibraryCleanupReport(0, 0, 0, 0, 0)
        }

        val validStudentIds = validStudents.map { it.id }.toSet()
        val knownUserIds = getUserAddedBookIds(context, classId)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val libraryKnownSynced = prefs.contains("$KEY_BOOKS_PREFIX$classId")
        val recordsKnownSynced = prefs.contains("$KEY_RECORDS_PREFIX$classId")

        // 1. Ham kitapları incele
        val rawBooksJson = prefs.getString("$KEY_BOOKS_PREFIX$classId", null)
        val rawBooks = mutableListOf<Pair<Book, Boolean>>()
        if (!rawBooksJson.isNullOrBlank()) {
            try {
                val arr = JSONArray(rawBooksJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    rawBooks.add(deserializeBook(obj, i + 1) to obj.optBoolean("userCreated", false))
                }
            } catch (_: Exception) {}
        }

        var removedBooksCount = 0
        var fixedGhostLoansCount = 0
        var resequencedCount = 0
        val bookIdsToDeleteFromCloud = mutableSetOf<String>()
        val keptBooks = mutableListOf<Book>()
        val seenNames = mutableSetOf<String>()

        val sortedCandidates = rawBooks.sortedWith(
            compareByDescending<Pair<Book, Boolean>> { it.second || knownUserIds.contains(it.first.id) }
                .thenByDescending { it.first.activeAssignments.isNotEmpty() }
                .thenByDescending { it.first.updatedAt }
        )

        for ((book, userCreated) in sortedCandidates) {
            if (isVirtualOrCorruptBook(book, userCreated, knownUserIds)) {
                removedBooksCount++
                bookIdsToDeleteFromCloud.add(book.id)
                continue
            }
            val normName = TurkishTextHelper.toTurkishUpperCase(book.name.trim())
            if (!seenNames.add(normName)) {
                removedBooksCount++
                bookIdsToDeleteFromCloud.add(book.id)
                continue
            }

            val normalized = normalizeBookState(book, if (validStudentIds.isNotEmpty()) validStudentIds else null)
            if (normalized.status != book.status ||
                normalized.activeAssignments.size != book.activeAssignments.size ||
                normalized.currentStudentId != book.currentStudentId
            ) {
                fixedGhostLoansCount++
            }
            keptBooks.add(normalized)
        }

        var finalBooks = keptBooks.sortedBy { it.registrationNo }
        if (resequenceNumbers) {
            finalBooks = finalBooks.mapIndexed { index, b ->
                val targetNo = index + 1
                if (b.registrationNo != targetNo) {
                    resequencedCount++
                    b.copy(registrationNo = targetNo, updatedAt = System.currentTimeMillis())
                } else b
            }
        }

        // 2. Okuma kayıtlarını incele: Silinmiş kitaplara, sanal kitaplara veya silinmiş öğrencilere ait kayıtları temizle
        val validBookIds = finalBooks.map { it.id }.toSet()
        val validBookNamesUpper = finalBooks.map { TurkishTextHelper.toTurkishUpperCase(it.name.trim()) }.toSet()

        val rawRecords = loadReadingRecords(context, classId)
        val keptRecords = mutableListOf<ReadingRecord>()
        val removedRecords = mutableListOf<ReadingRecord>()
        val recordIdsToDeleteFromCloud = mutableSetOf<String>()

        for (rec in rawRecords) {
            val upperBookName = TurkishTextHelper.toTurkishUpperCase(rec.bookName.trim())
            val isCorruptRecord = rec.id.isBlank() || rec.bookName.isBlank() || rec.studentId.isBlank()
            val isSampleBook = upperBookName in LEGACY_SAMPLE_BOOK_NAMES_UPPER && upperBookName !in validBookNamesUpper
            val isStudentInvalid = validStudentIds.isNotEmpty() && rec.studentId !in validStudentIds
            val isBookOrphaned = (finalBooks.isNotEmpty() || libraryKnownSynced) && rec.bookId !in validBookIds && upperBookName !in validBookNamesUpper

            if (isCorruptRecord || isSampleBook || isStudentInvalid || isBookOrphaned) {
                removedRecords.add(rec)
                if (rec.id.isNotBlank()) recordIdsToDeleteFromCloud.add(rec.id)
            } else {
                // Öğrenci adını güncel sınıf listesinden senkronize et
                val matchedStudent = validStudents.firstOrNull { it.id == rec.studentId }
                val updatedRec = if (matchedStudent != null && (rec.studentName != matchedStudent.fullName || rec.studentNumber != matchedStudent.studentNumber)) {
                    rec.copy(studentName = matchedStudent.fullName, studentNumber = matchedStudent.studentNumber)
                } else rec
                keptRecords.add(updatedRec)
            }
        }

        // 3. Silinen okuma kayıtlarına, sanal kitaplara veya karşılıksız kalan okuma işlemlerine ait Kitap Kurdu yıldızlarını temizle
        var revokedStarsCount = 0
        val starRecords = StarRewardPersistenceHelper.loadStarRecords(context, classId)
        if (starRecords.isNotEmpty()) {
            val starIdsToRemove = mutableSetOf<String>()

            removedRecords.forEach { rec ->
                val matching = starRecords.firstOrNull { sr ->
                    !starIdsToRemove.contains(sr.id) &&
                    sr.studentId == rec.studentId &&
                    sr.categoryId == "cat_kitap_kurdu" &&
                    (sr.note.contains(rec.bookName, ignoreCase = true) || kotlin.math.abs(sr.timestamp - rec.endDate) < 60000L)
                }
                if (matching != null) {
                    starIdsToRemove.add(matching.id)
                    revokedStarsCount += matching.starCount
                }
            }

            // Okuma geçmişi senkronize ise ve hala karşılığı olmayan cat_kitap_kurdu yıldızları varsa temizle
            if (recordsKnownSynced) {
                starRecords.filter { sr ->
                    !starIdsToRemove.contains(sr.id) && sr.categoryId == "cat_kitap_kurdu"
                }.forEach { sr ->
                    val hasMatchingRecord = keptRecords.any { rec ->
                        rec.studentId == sr.studentId &&
                            (sr.note.contains(rec.bookName, ignoreCase = true) || kotlin.math.abs(sr.timestamp - rec.endDate) < 60000L)
                    }
                    if (!hasMatchingRecord) {
                        starIdsToRemove.add(sr.id)
                        revokedStarsCount += sr.starCount
                    }
                }
            }

            // Sanal kitap adlarını içeren tüm eski Kitap Kurdu yıldızlarını da temizle
            starRecords.filter { sr ->
                !starIdsToRemove.contains(sr.id) &&
                sr.categoryId == "cat_kitap_kurdu" &&
                LEGACY_SAMPLE_BOOK_NAMES_UPPER.any { sample ->
                    sample !in validBookNamesUpper && TurkishTextHelper.toTurkishUpperCase(sr.note).contains(sample)
                }
            }.forEach { sr ->
                starIdsToRemove.add(sr.id)
                revokedStarsCount += sr.starCount
            }

            if (starIdsToRemove.isNotEmpty()) {
                StarRewardPersistenceHelper.deleteRecordsByIds(context, classId, starIdsToRemove)
            }
        }

        if (removedBooksCount > 0 || fixedGhostLoansCount > 0 || resequencedCount > 0) {
            saveBooks(context, classId, finalBooks)
        }
        if (bookIdsToDeleteFromCloud.isNotEmpty()) {
            deleteBookDocsFromFirestore(context, classId, bookIdsToDeleteFromCloud)
        }
        if (removedRecords.isNotEmpty() || keptRecords.size != rawRecords.size) {
            saveReadingRecords(context, classId, keptRecords)
        }
        if (recordIdsToDeleteFromCloud.isNotEmpty()) {
            deleteRecordDocsFromFirestore(context, classId, recordIdsToDeleteFromCloud)
        }

        return LibraryCleanupReport(
            removedBooksCount = removedBooksCount,
            removedRecordsCount = removedRecords.size,
            fixedGhostLoansCount = fixedGhostLoansCount,
            revokedStarsCount = revokedStarsCount,
            resequencedBooksCount = resequencedCount
        )
    }

    /**
     * Okuma Kaydı Sil ve İlgili Kitap Kurdu Yıldızını Geri Al
     */
    fun deleteReadingRecord(context: Context, classRoomId: String, recordId: String): Int {
        val records = loadReadingRecords(context, classRoomId).toMutableList()
        val record = records.firstOrNull { it.id == recordId }
        records.removeAll { it.id == recordId }
        saveReadingRecords(context, classRoomId, records)

        if (classRoomId.isNotBlank() && classRoomId != "default") {
            deleteRecordDocsFromFirestore(context, classRoomId, setOf(recordId))
        }

        var revokedStars = 0
        if (record != null) {
            revokedStars = if (record.earnedStars > 0) record.earnedStars else calculateReadingStars(record.pageCount)
            val starRecords = StarRewardPersistenceHelper.loadStarRecords(context, classRoomId)
            val matching = starRecords.firstOrNull { sr ->
                sr.studentId == record.studentId &&
                sr.categoryId == "cat_kitap_kurdu" &&
                (sr.note.contains(record.bookName, ignoreCase = true) || Math.abs(sr.timestamp - record.endDate) < 60000L)
            }
            if (matching != null) {
                StarRewardPersistenceHelper.deleteStarRecord(context, classRoomId, matching.id)
            }
        }
        return revokedStars
    }

    /**
     * Kitap Ödünç Verme (Tek Öğrenci / Çoklu Öğrenci desteği)
     */
    fun borrowBook(
        context: Context,
        classRoomId: String,
        bookId: String,
        studentId: String,
        studentName: String,
        date: String? = null
    ): List<Book> {
        val assignDateMs = if (!date.isNullOrBlank()) {
            try {
                java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale("tr")).parse(date)?.time ?: System.currentTimeMillis()
            } catch (_: Exception) { System.currentTimeMillis() }
        } else System.currentTimeMillis()

        val currentBooks = loadBooks(context, classRoomId).toMutableList()
        val index = currentBooks.indexOfFirst { it.id == bookId }
        if (index != -1) {
            val book = currentBooks[index]
            val newAssignments = book.activeAssignments.toMutableList()
            if (newAssignments.none { it.studentId == studentId }) {
                newAssignments.add(
                    ActiveAssignment(
                        studentId = studentId,
                        studentName = studentName,
                        assignmentDate = assignDateMs
                    )
                )
            }
            val updated = normalizeBookState(
                book.copy(
                    status = "Okunuyor",
                    activeAssignments = newAssignments,
                    currentStudentId = newAssignments.firstOrNull()?.studentId,
                    currentStudentName = newAssignments.firstOrNull()?.studentName,
                    assignmentDate = newAssignments.firstOrNull()?.assignmentDate ?: assignDateMs,
                    updatedAt = System.currentTimeMillis()
                )
            )
            currentBooks[index] = updated
            saveBooks(context, classRoomId, currentBooks)
        }
        return currentBooks
    }

    fun borrowBook(
        context: Context,
        classId: String,
        bookId: String,
        students: List<Student>,
        assignmentDate: Long = System.currentTimeMillis()
    ): List<Book> {
        val currentBooks = loadBooks(context, classId).toMutableList()
        val index = currentBooks.indexOfFirst { it.id == bookId }
        if (index != -1) {
            val book = currentBooks[index]
            val newAssignments = book.activeAssignments.toMutableList()
            students.forEach { s ->
                if (newAssignments.none { it.studentId == s.id }) {
                    newAssignments.add(
                        ActiveAssignment(
                            studentId = s.id,
                            studentName = s.fullName,
                            assignmentDate = assignmentDate
                        )
                    )
                }
            }
            val updated = normalizeBookState(
                book.copy(
                    status = "Okunuyor",
                    activeAssignments = newAssignments,
                    currentStudentId = newAssignments.firstOrNull()?.studentId,
                    currentStudentName = newAssignments.firstOrNull()?.studentName,
                    assignmentDate = newAssignments.firstOrNull()?.assignmentDate ?: assignmentDate,
                    updatedAt = System.currentTimeMillis()
                )
            )
            currentBooks[index] = updated
            saveBooks(context, classId, currentBooks)
        }
        return currentBooks
    }

    /**
     * Kitap İade Alma & Otomatik Motivasyon Yıldızı Verme
     */
    fun returnBook(
        context: Context,
        classRoomId: String,
        bookId: String,
        studentId: String,
        studentName: String? = null,
        startDate: Long = 0L,
        returnDate: Long = System.currentTimeMillis()
    ): Int {
        val currentBooks = loadBooks(context, classRoomId).toMutableList()
        val currentRecords = loadReadingRecords(context, classRoomId).toMutableList()
        var starsEarned = 1

        val bookIndex = currentBooks.indexOfFirst { it.id == bookId }
        if (bookIndex != -1) {
            val book = currentBooks[bookIndex]
            val matchingAssignment = book.activeAssignments.firstOrNull { it.studentId == studentId }
            val realStartDate = if (startDate > 0) startDate else (matchingAssignment?.assignmentDate ?: book.assignmentDate ?: (returnDate - 86400000L * 3))
            val realStudentName = studentName ?: matchingAssignment?.studentName ?: book.currentStudentName ?: "Öğrenci"

            starsEarned = calculateReadingStars(book.pageCount)
            val starRecord = StarRewardRecord(
                id = UUID.randomUUID().toString(),
                classId = classRoomId,
                studentId = studentId,
                studentName = realStudentName,
                studentNumber = "",
                categoryId = "cat_kitap_kurdu",
                categoryName = "Kitap Kurdu 📚",
                starCount = starsEarned,
                note = "${book.name} okundu (${book.pageCount} sayfa)",
                isLuckyBonus = false,
                timestamp = returnDate
            )
            StarRewardPersistenceHelper.addStarRecord(context, classRoomId, starRecord)

            val readingRecord = ReadingRecord(
                id = UUID.randomUUID().toString(),
                bookId = book.id,
                bookName = book.name,
                pageCount = book.pageCount,
                studentId = studentId,
                studentName = realStudentName,
                studentNumber = "",
                startDate = realStartDate,
                endDate = returnDate,
                earnedStars = starsEarned,
                teacherUid = Firebase.auth.currentUser?.uid ?: "",
                createdAt = System.currentTimeMillis()
            )
            currentRecords.add(0, readingRecord)
            saveReadingRecords(context, classRoomId, currentRecords)

            val remainingAssignments = book.activeAssignments.filter { it.studentId != studentId }
            val updatedBook = normalizeBookState(
                book.copy(
                    status = if (remainingAssignments.isEmpty()) "Rafta" else "Okunuyor",
                    activeAssignments = remainingAssignments,
                    currentStudentId = remainingAssignments.firstOrNull()?.studentId,
                    currentStudentName = remainingAssignments.firstOrNull()?.studentName,
                    assignmentDate = remainingAssignments.firstOrNull()?.assignmentDate,
                    updatedAt = System.currentTimeMillis()
                )
            )
            currentBooks[bookIndex] = updatedBook
            saveBooks(context, classRoomId, currentBooks)
        }
        return starsEarned
    }

    /**
     * Kitap İade Alma & Otomatik Motivasyon Yıldızı Verme
     */
    fun returnBook(
        context: Context,
        classId: String,
        bookId: String,
        studentId: String,
        returnDate: Long = System.currentTimeMillis()
    ): Pair<List<Book>, List<ReadingRecord>> {
        returnBook(
            context = context,
            classRoomId = classId,
            bookId = bookId,
            studentId = studentId,
            returnDate = returnDate
        )
        return Pair(loadBooks(context, classId), loadReadingRecords(context, classId))
    }

    /**
     * Teslimatı İptal Et (Öğrenci okumadan hemen iade ettiyse veya yanlış atandıysa)
     */
    fun cancelAssignment(
        context: Context,
        classId: String,
        bookId: String,
        studentId: String? = null
    ): List<Book> {
        val currentBooks = loadBooks(context, classId).toMutableList()
        val bookIndex = currentBooks.indexOfFirst { it.id == bookId }
        if (bookIndex != -1) {
            val book = currentBooks[bookIndex]
            val remainingAssignments = if (!studentId.isNullOrBlank()) {
                book.activeAssignments.filter { it.studentId != studentId }
            } else emptyList()

            val updatedBook = normalizeBookState(
                book.copy(
                    status = if (remainingAssignments.isEmpty()) "Rafta" else "Okunuyor",
                    activeAssignments = remainingAssignments,
                    currentStudentId = remainingAssignments.firstOrNull()?.studentId,
                    currentStudentName = remainingAssignments.firstOrNull()?.studentName,
                    assignmentDate = remainingAssignments.firstOrNull()?.assignmentDate,
                    updatedAt = System.currentTimeMillis()
                )
            )
            currentBooks[bookIndex] = updatedBook
            saveBooks(context, classId, currentBooks)
        }
        return currentBooks
    }

    /**
     * "Tüm Sınıf Okudu" Toplu Eylemi
     */
    fun recordClassRead(
        context: Context,
        classId: String,
        bookId: String,
        students: List<Student>,
        readDate: Long = System.currentTimeMillis()
    ): List<ReadingRecord> {
        val books = loadBooks(context, classId)
        val book = books.firstOrNull { it.id == bookId } ?: return loadReadingRecords(context, classId)
        val currentRecords = loadReadingRecords(context, classId).toMutableList()
        val stars = calculateReadingStars(book.pageCount)

        students.forEach { s ->
            val starRecord = StarRewardRecord(
                id = UUID.randomUUID().toString(),
                classId = classId,
                studentId = s.id,
                studentName = s.fullName,
                studentNumber = s.studentNumber,
                categoryId = "cat_kitap_kurdu",
                categoryName = "Kitap Kurdu 📚",
                starCount = stars,
                note = "${book.name} (Ortak Sınıf Okuması)",
                isLuckyBonus = false,
                timestamp = readDate
            )
            StarRewardPersistenceHelper.addStarRecord(context, classId, starRecord)

            currentRecords.add(
                0,
                ReadingRecord(
                    id = UUID.randomUUID().toString(),
                    bookId = book.id,
                    bookName = book.name,
                    pageCount = book.pageCount,
                    studentId = s.id,
                    studentName = s.fullName,
                    studentNumber = s.studentNumber,
                    startDate = readDate - 86400000L * 7,
                    endDate = readDate,
                    earnedStars = stars,
                    teacherUid = Firebase.auth.currentUser?.uid ?: "",
                    createdAt = System.currentTimeMillis()
                )
            )
        }
        saveReadingRecords(context, classId, currentRecords)
        return currentRecords
    }

    fun recordBatchClassReading(
        context: Context,
        classRoomId: String,
        book: Book,
        students: List<Student>,
        date: String? = null
    ): Int {
        val dateMs = if (!date.isNullOrBlank()) {
            try {
                java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale("tr")).parse(date)?.time ?: System.currentTimeMillis()
            } catch (_: Exception) { System.currentTimeMillis() }
        } else System.currentTimeMillis()
        val records = recordClassRead(context, classRoomId, book.id, students, dateMs)
        return records.take(students.size).sumOf { it.earnedStars }
    }

    private fun serializeBook(book: Book, userCreated: Boolean = true): JSONObject {
        val assignArray = JSONArray()
        book.activeAssignments.forEach { a ->
            val aObj = JSONObject().apply {
                put("studentId", a.studentId)
                put("studentName", a.studentName)
                put("assignmentDate", a.assignmentDate)
            }
            assignArray.put(aObj)
        }
        return JSONObject().apply {
            put("id", book.id)
            put("registrationNo", book.registrationNo)
            put("name", book.name)
            put("author", book.author)
            put("pageCount", book.pageCount)
            put("status", book.status)
            put("activeAssignments", assignArray)
            put("userCreated", userCreated)
            book.currentStudentId?.let { put("currentStudentId", it) }
            book.currentStudentName?.let { put("currentStudentName", it) }
            book.assignmentDate?.let { put("assignmentDate", it) }
            put("createdAt", book.createdAt)
            put("updatedAt", book.updatedAt)
        }
    }

    private fun deserializeBook(obj: JSONObject, defaultRegNo: Int = 1): Book {
        val assignmentsList = mutableListOf<ActiveAssignment>()
        val assignArray = obj.optJSONArray("activeAssignments")
        if (assignArray != null) {
            for (j in 0 until assignArray.length()) {
                val aObj = assignArray.getJSONObject(j)
                val sId = aObj.optString("studentId", "").trim()
                val sName = aObj.optString("studentName", "").trim()
                if (sId.isNotEmpty() && sName.isNotEmpty()) {
                    assignmentsList.add(
                        ActiveAssignment(
                            studentId = sId,
                            studentName = sName,
                            assignmentDate = aObj.optLong("assignmentDate", System.currentTimeMillis())
                        )
                    )
                }
            }
        }
        // Eğer eski kayıtlarda activeAssignments boş ama currentStudentId dolu ise dönüştür
        val legacyStudentId = obj.optString("currentStudentId", "").trim()
        val legacyStudentName = obj.optString("currentStudentName", "").trim()
        val legacyAssignDate = if (obj.has("assignmentDate")) obj.optLong("assignmentDate", System.currentTimeMillis()) else System.currentTimeMillis()
        if (assignmentsList.isEmpty() && legacyStudentId.isNotEmpty() && legacyStudentName.isNotEmpty()) {
            assignmentsList.add(
                ActiveAssignment(
                    studentId = legacyStudentId,
                    studentName = legacyStudentName,
                    assignmentDate = legacyAssignDate
                )
            )
        }

        return normalizeBookState(
            Book(
                id = obj.optString("id", UUID.randomUUID().toString()),
                registrationNo = obj.optInt("registrationNo", defaultRegNo),
                name = obj.optString("name", "Kitap"),
                author = obj.optString("author", ""),
                pageCount = obj.optInt("pageCount", 0),
                status = if (assignmentsList.isNotEmpty()) "Okunuyor" else "Rafta",
                activeAssignments = assignmentsList,
                currentStudentId = assignmentsList.firstOrNull()?.studentId,
                currentStudentName = assignmentsList.firstOrNull()?.studentName,
                assignmentDate = assignmentsList.firstOrNull()?.assignmentDate,
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
            )
        )
    }

    private fun serializeReadingRecord(r: ReadingRecord): JSONObject {
        return JSONObject().apply {
            put("id", r.id)
            put("bookId", r.bookId)
            put("bookName", r.bookName)
            put("pageCount", r.pageCount)
            put("studentId", r.studentId)
            put("studentName", r.studentName)
            put("studentNumber", r.studentNumber)
            put("startDate", r.startDate)
            put("endDate", r.endDate)
            put("earnedStars", r.earnedStars)
            put("teacherUid", r.teacherUid)
            put("createdAt", r.createdAt)
        }
    }

    private fun deserializeReadingRecord(obj: JSONObject, fallbackId: String? = null): ReadingRecord {
        return ReadingRecord(
            id = obj.optString("id", fallbackId ?: UUID.randomUUID().toString()),
            bookId = obj.optString("bookId", ""),
            bookName = obj.optString("bookName", ""),
            pageCount = obj.optInt("pageCount", 0),
            studentId = obj.optString("studentId", ""),
            studentName = obj.optString("studentName", ""),
            studentNumber = obj.optString("studentNumber", ""),
            startDate = obj.optLong("startDate", System.currentTimeMillis()),
            endDate = obj.optLong("endDate", System.currentTimeMillis()),
            earnedStars = obj.optInt("earnedStars", 1),
            teacherUid = obj.optString("teacherUid", ""),
            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
        )
    }

    private fun deleteBookDocsFromFirestore(context: Context, classId: String, bookIds: Set<String>) {
        if (classId.isBlank() || classId == "default" || bookIds.isEmpty()) return
        val db = getFirestore(context) ?: return
        syncScope.launch {
            try {
                val col = db.collection("classrooms").document(classId).collection("library_books")
                bookIds.chunked(400).forEach { chunk ->
                    val batch = db.batch()
                    chunk.forEach { id ->
                        if (id.isNotBlank()) batch.delete(col.document(id))
                    }
                    batch.commit()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed deleting books from Firestore", e)
            }
        }
    }

    private fun deleteRecordDocsFromFirestore(context: Context, classId: String, recordIds: Set<String>) {
        if (classId.isBlank() || classId == "default" || recordIds.isEmpty()) return
        val db = getFirestore(context) ?: return
        syncScope.launch {
            try {
                val col = db.collection("classrooms").document(classId).collection("reading_records")
                recordIds.chunked(400).forEach { chunk ->
                    val batch = db.batch()
                    chunk.forEach { id ->
                        if (id.isNotBlank()) batch.delete(col.document(id))
                    }
                    batch.commit()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed deleting reading records from Firestore", e)
            }
        }
    }

    private fun deleteAllCollectionDocsFromFirestore(
        context: Context,
        classId: String,
        collectionName: String,
        knownIds: Set<String> = emptySet()
    ) {
        if (classId.isBlank() || classId == "default") return
        val db = getFirestore(context) ?: return
        syncScope.launch {
            try {
                val col = db.collection("classrooms").document(classId).collection(collectionName)
                if (knownIds.isNotEmpty()) {
                    knownIds.filter { it.isNotBlank() }.chunked(400).forEach { chunk ->
                        val batch = db.batch()
                        chunk.forEach { id -> batch.delete(col.document(id)) }
                        batch.commit()
                    }
                }
                col.get().addOnSuccessListener { snapshot ->
                    if (snapshot != null && !snapshot.isEmpty) {
                        snapshot.documents.chunked(400).forEach { chunk ->
                            val batch = db.batch()
                            chunk.forEach { doc -> batch.delete(doc.reference) }
                            batch.commit()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed clearing collection $collectionName from Firestore", e)
            }
        }
    }

    fun uploadBooksToFirestore(context: Context, classId: String, books: List<Book>) {
        if (classId.isBlank() || classId == "default" || books.isEmpty()) return
        val db = getFirestore(context) ?: return
        val knownUserIds = getUserAddedBookIds(context, classId)
        syncScope.launch {
            try {
                val col = db.collection("classrooms").document(classId).collection("library_books")
                books.chunked(400).forEach { chunk ->
                    val batch = db.batch()
                    chunk.forEach { rawBook ->
                        val book = normalizeBookState(rawBook)
                        val isExplicitTeacherBook = knownUserIds.contains(book.id)
                        val doc = col.document(book.id)
                        batch.set(
                            doc,
                            mapOf(
                                "id" to book.id,
                                "registrationNo" to book.registrationNo,
                                "name" to book.name,
                                "author" to book.author,
                                "pageCount" to book.pageCount,
                                "status" to book.status,
                                "currentStudentId" to (book.currentStudentId ?: ""),
                                "currentStudentName" to (book.currentStudentName ?: ""),
                                "userCreated" to isExplicitTeacherBook,
                                "explicitlyAddedByTeacher" to isExplicitTeacherBook,
                                "json" to serializeBook(book, userCreated = isExplicitTeacherBook).toString(),
                                "updatedAt" to System.currentTimeMillis()
                            ),
                            SetOptions.merge()
                        )
                    }
                    batch.commit()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed uploading books to Firestore", e)
            }
        }
    }

    fun uploadRecordsToFirestore(context: Context, classId: String, records: List<ReadingRecord>) {
        if (classId.isBlank() || classId == "default" || records.isEmpty()) return
        val db = getFirestore(context) ?: return
        syncScope.launch {
            try {
                val col = db.collection("classrooms").document(classId).collection("reading_records")
                records.chunked(400).forEach { chunk ->
                    val batch = db.batch()
                    chunk.forEach { r ->
                        val doc = col.document(r.id)
                        batch.set(
                            doc,
                            mapOf(
                                "id" to r.id,
                                "bookId" to r.bookId,
                                "bookName" to r.bookName,
                                "pageCount" to r.pageCount,
                                "studentId" to r.studentId,
                                "studentName" to r.studentName,
                                "studentNumber" to r.studentNumber,
                                "startDate" to r.startDate,
                                "endDate" to r.endDate,
                                "earnedStars" to r.earnedStars,
                                "teacherUid" to r.teacherUid,
                                "createdAt" to r.createdAt,
                                "json" to serializeReadingRecord(r).toString(),
                                "updatedAt" to System.currentTimeMillis()
                            ),
                            SetOptions.merge()
                        )
                    }
                    batch.commit()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed uploading reading records to Firestore", e)
            }
        }
    }

    fun observeFirestoreBooks(
        context: Context,
        classId: String,
        onUpdate: (List<Book>) -> Unit
    ): ListenerRegistration? {
        if (classId.isBlank() || classId == "default") return null
        val db = getFirestore(context) ?: return null
        return try {
            db.collection("classrooms").document(classId).collection("library_books")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    try {
                        val knownUserIds = getUserAddedBookIds(context, classId)
                        val rawDocs = mutableListOf<Pair<Book, Boolean>>()
                        val invalidDocIds = mutableSetOf<String>()

                        for (doc in snapshot.documents) {
                            val docExplicitlyAdded = doc.getBoolean("explicitlyAddedByTeacher") == true
                            val jsonStr = doc.getString("json")
                            if (!jsonStr.isNullOrBlank()) {
                                try {
                                    val obj = JSONObject(jsonStr)
                                    val book = deserializeBook(obj)
                                    val isUserCreated = docExplicitlyAdded || knownUserIds.contains(book.id)
                                    rawDocs.add(book to isUserCreated)
                                } catch (_: Exception) {
                                    invalidDocIds.add(doc.id)
                                }
                            } else {
                                val regNo = (doc.getLong("registrationNo") ?: 1L).toInt()
                                val name = doc.getString("name") ?: "Kitap"
                                val author = doc.getString("author") ?: ""
                                val pageCount = (doc.getLong("pageCount") ?: 0L).toInt()
                                val status = doc.getString("status") ?: "Rafta"
                                val sId = doc.getString("currentStudentId")?.takeIf { it.isNotEmpty() }
                                val sName = doc.getString("currentStudentName")?.takeIf { it.isNotEmpty() }
                                val assignments = if (sId != null && sName != null) {
                                    listOf(ActiveAssignment(sId, sName, System.currentTimeMillis()))
                                } else emptyList()
                                val book = normalizeBookState(
                                    Book(
                                        id = doc.id,
                                        registrationNo = regNo,
                                        name = name,
                                        author = author,
                                        pageCount = pageCount,
                                        status = status,
                                        activeAssignments = assignments,
                                        currentStudentId = sId,
                                        currentStudentName = sName
                                    )
                                )
                                val isUserCreated = docExplicitlyAdded || knownUserIds.contains(book.id)
                                rawDocs.add(book to isUserCreated)
                            }
                        }

                        val cleanList = mutableListOf<Book>()
                        val seenNames = mutableSetOf<String>()

                        val sortedCandidates = rawDocs.sortedWith(
                            compareByDescending<Pair<Book, Boolean>> { it.second || knownUserIds.contains(it.first.id) }
                                .thenByDescending { it.first.activeAssignments.isNotEmpty() }
                                .thenByDescending { it.first.updatedAt }
                        )

                        for ((book, isUserCreated) in sortedCandidates) {
                            if (isVirtualOrCorruptBook(book, isUserCreated, knownUserIds)) {
                                invalidDocIds.add(book.id)
                                continue
                            }
                            val normName = TurkishTextHelper.toTurkishUpperCase(book.name.trim())
                            if (!seenNames.add(normName)) {
                                invalidDocIds.add(book.id)
                                continue
                            }
                            if (isUserCreated && !knownUserIds.contains(book.id)) {
                                markBookAsUserAdded(context, classId, book.id)
                            }
                            cleanList.add(normalizeBookState(book))
                        }

                        if (invalidDocIds.isNotEmpty()) {
                            deleteBookDocsFromFirestore(context, classId, invalidDocIds)
                        }

                        cleanList.sortBy { it.registrationNo }
                        // Firestore SSOT: Boş olsa bile yerel önbelleği Firestore ile birebir eşitle (eski silinmiş kitapları geri yükleme!)
                        saveBooksToLocalPrefsOnly(context, classId, cleanList)
                        onUpdate(cleanList)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in observeFirestoreBooks", e)
                    }
                }
        } catch (_: Exception) {
            null
        }
    }

    fun observeFirestoreReadingRecords(
        context: Context,
        classId: String,
        onUpdate: (List<ReadingRecord>) -> Unit
    ): ListenerRegistration? {
        if (classId.isBlank() || classId == "default") return null
        val db = getFirestore(context) ?: return null
        return try {
            db.collection("classrooms").document(classId).collection("reading_records")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    try {
                        val knownUserIds = getUserAddedBookIds(context, classId)
                        val validBookIds = loadBooks(context, classId).map { it.id }.toSet()
                        val list = mutableListOf<ReadingRecord>()
                        val invalidRecordIds = mutableSetOf<String>()

                        for (doc in snapshot.documents) {
                            val jsonStr = doc.getString("json")
                            val record = if (!jsonStr.isNullOrBlank()) {
                                try {
                                    val obj = JSONObject(jsonStr)
                                    deserializeReadingRecord(obj, fallbackId = doc.id).copy(
                                        studentNumber = doc.getString("studentNumber") ?: obj.optString("studentNumber", ""),
                                        teacherUid = doc.getString("teacherUid") ?: obj.optString("teacherUid", "")
                                    )
                                } catch (_: Exception) {
                                    null
                                }
                            } else {
                                val bId = doc.getString("bookId") ?: ""
                                val bName = doc.getString("bookName") ?: ""
                                val page = (doc.getLong("pageCount") ?: 0L).toInt()
                                val sId = doc.getString("studentId") ?: ""
                                val sName = doc.getString("studentName") ?: ""
                                val sNumber = doc.getString("studentNumber") ?: ""
                                val sDate = doc.getLong("startDate") ?: System.currentTimeMillis()
                                val eDate = doc.getLong("endDate") ?: System.currentTimeMillis()
                                val stars = (doc.getLong("earnedStars") ?: 1L).toInt()
                                ReadingRecord(
                                    id = doc.id,
                                    bookId = bId,
                                    bookName = bName,
                                    pageCount = page,
                                    studentId = sId,
                                    studentName = sName,
                                    studentNumber = sNumber,
                                    startDate = sDate,
                                    endDate = eDate,
                                    earnedStars = stars
                                )
                            }

                            if (record == null || record.bookName.isBlank() || record.studentId.isBlank()) {
                                invalidRecordIds.add(doc.id)
                                continue
                            }

                            val upperBookName = TurkishTextHelper.toTurkishUpperCase(record.bookName.trim())
                            val isTeacherBook = record.bookId in knownUserIds || record.bookId in validBookIds
                            if (upperBookName in LEGACY_SAMPLE_BOOK_NAMES_UPPER && !isTeacherBook) {
                                invalidRecordIds.add(doc.id)
                                continue
                            }

                            list.add(record)
                        }

                        if (invalidRecordIds.isNotEmpty()) {
                            deleteRecordDocsFromFirestore(context, classId, invalidRecordIds)
                        }

                        val cleanSorted = list.distinctBy { it.id }.sortedByDescending { it.endDate }
                        // Firestore SSOT: Yerel önbelleği Firestore ile birebir eşitle
                        saveRecordsToLocalPrefsOnly(context, classId, cleanSorted)
                        onUpdate(cleanSorted)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in observeFirestoreReadingRecords", e)
                    }
                }
        } catch (_: Exception) {
            null
        }
    }
}
