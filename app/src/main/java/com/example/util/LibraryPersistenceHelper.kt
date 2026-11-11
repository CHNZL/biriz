package com.example.util

import android.content.Context
import android.util.Log
import com.example.R
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
 * Akıllı Sınıf Kitaplığı & Okuma Takip Kalıcı Veri ve İş Mantığı Yöneticisi
 */
object LibraryPersistenceHelper {
    private const val TAG = "LibraryPersistence"
    private const val PREFS_NAME = "biriz_library_prefs"
    private const val KEY_BOOKS_PREFIX = "lib_books_"
    private const val KEY_RECORDS_PREFIX = "lib_records_"
    private val syncScope = CoroutineScope(Dispatchers.IO)

    private fun getFirestore(context: Context): FirebaseFirestore? {
        return try {
            FirestoreProvider.get(context)
        } catch (_: Exception) {
            null
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
     * Kitapları Yerel Hafızadan Yükle (İlk açılışta zengin örnek veri ile başlar)
     */
    fun loadBooks(context: Context, classId: String): List<Book> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("$KEY_BOOKS_PREFIX$classId", null)
        if (jsonStr.isNullOrBlank()) {
            return emptyList()
        }

        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<Book>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val assignmentsList = mutableListOf<ActiveAssignment>()
                val assignArray = obj.optJSONArray("activeAssignments")
                if (assignArray != null) {
                    for (j in 0 until assignArray.length()) {
                        val aObj = assignArray.getJSONObject(j)
                        assignmentsList.add(
                            ActiveAssignment(
                                studentId = aObj.optString("studentId", ""),
                                studentName = aObj.optString("studentName", ""),
                                assignmentDate = aObj.optLong("assignmentDate", System.currentTimeMillis())
                            )
                        )
                    }
                }

                list.add(
                    Book(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        registrationNo = obj.optInt("registrationNo", i + 1),
                        name = obj.optString("name", "Kitap"),
                        author = obj.optString("author", ""),
                        pageCount = obj.optInt("pageCount", 0),
                        status = obj.optString("status", if (assignmentsList.isNotEmpty()) "Okunuyor" else "Rafta"),
                        activeAssignments = assignmentsList,
                        currentStudentId = obj.optString("currentStudentId", "").takeIf { it.isNotEmpty() },
                        currentStudentName = obj.optString("currentStudentName", "").takeIf { it.isNotEmpty() },
                        assignmentDate = if (obj.has("assignmentDate")) obj.optLong("assignmentDate") else null,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
            val sampleNames = setOf(
                "Küçük Prens", "Şeker Portakalı", "Dedemin Bakkalı",
                "Pal Sokağı Çocukları", "Keloğlan Masalları", "Peter Pan", "Momo", "Pinokyo"
            )
            val cleanedList = list.filterNot { it.name.trim() in sampleNames }
            if (cleanedList.size != list.size) {
                saveBooks(context, classId, cleanedList)
            }
            cleanedList.sortedBy { it.registrationNo }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Kitapları Yerel Hafızaya ve Firestore'a Kaydet
     */
    fun saveBooks(context: Context, classId: String, books: List<Book>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        books.forEach { book ->
            val obj = JSONObject().apply {
                put("id", book.id)
                put("registrationNo", book.registrationNo)
                put("name", book.name)
                put("author", book.author)
                put("pageCount", book.pageCount)
                put("status", book.status)
                put("currentStudentId", book.currentStudentId ?: "")
                put("currentStudentName", book.currentStudentName ?: "")
                book.assignmentDate?.let { put("assignmentDate", it) }
                put("createdAt", book.createdAt)
                put("updatedAt", book.updatedAt)

                val assignArray = JSONArray()
                book.activeAssignments.forEach { a ->
                    assignArray.put(
                        JSONObject().apply {
                            put("studentId", a.studentId)
                            put("studentName", a.studentName)
                            put("assignmentDate", a.assignmentDate)
                        }
                    )
                }
                put("activeAssignments", assignArray)
            }
            array.put(obj)
        }
        prefs.edit().putString("$KEY_BOOKS_PREFIX$classId", array.toString()).apply()

        // Firestore bulut yedeklemesi
        syncScope.launch {
            uploadBooksToFirestore(context, classId, books)
        }
    }

    /**
     * Tamamlanan Okuma Geçmişi Kayıtlarını Yükle
     */
    fun loadReadingRecords(context: Context, classId: String): List<ReadingRecord> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("$KEY_RECORDS_PREFIX$classId", null) ?: return emptyList()

        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<ReadingRecord>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ReadingRecord(
                        id = obj.optString("id", UUID.randomUUID().toString()),
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
                )
            }
            list.sortedByDescending { it.endDate }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Okuma Geçmişi Kayıtlarını Kaydet
     */
    fun saveReadingRecords(context: Context, classId: String, records: List<ReadingRecord>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        records.forEach { r ->
            val obj = JSONObject().apply {
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
            array.put(obj)
        }
        prefs.edit().putString("$KEY_RECORDS_PREFIX$classId", array.toString()).apply()

        syncScope.launch {
            uploadRecordsToFirestore(context, classId, records)
        }
    }

    /**
     * Yeni Kitap Ekle
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
            } catch (e: Exception) { System.currentTimeMillis() }
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

        val newBook = Book(
            id = UUID.randomUUID().toString(),
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
        books.add(newBook)
        books.sortBy { it.registrationNo }
        saveBooks(context, classRoomId, books)
        return newBook
    }

    /**
     * Kitap Güncelle
     */
    fun updateBook(context: Context, classRoomId: String, book: Book) {
        val books = loadBooks(context, classRoomId).toMutableList()
        val idx = books.indexOfFirst { it.id == book.id }
        if (idx != -1) {
            books[idx] = book.copy(updatedAt = System.currentTimeMillis())
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
        val relatedRecords = records.filter { it.bookId == bookId }
        records.removeAll { it.bookId == bookId }
        saveReadingRecords(context, classRoomId, records)

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

        // Eğer notunda doğrudan bookName geçen diğer Kitap Kurdu yıldızları varsa onları da ekle
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
     * Okuma Kaydı Sil ve İlgili Kitap Kurdu Yıldızını Geri Al
     */
    fun deleteReadingRecord(context: Context, classRoomId: String, recordId: String): Int {
        val records = loadReadingRecords(context, classRoomId).toMutableList()
        val record = records.firstOrNull { it.id == recordId }
        records.removeAll { it.id == recordId }
        saveReadingRecords(context, classRoomId, records)

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
            } catch (e: Exception) { System.currentTimeMillis() }
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
            val updated = book.copy(
                status = "Okunuyor",
                activeAssignments = newAssignments,
                currentStudentId = newAssignments.firstOrNull()?.studentId,
                currentStudentName = newAssignments.firstOrNull()?.studentName,
                assignmentDate = newAssignments.firstOrNull()?.assignmentDate ?: assignDateMs,
                updatedAt = System.currentTimeMillis()
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
            val updated = book.copy(
                status = "Okunuyor",
                activeAssignments = newAssignments,
                currentStudentId = newAssignments.firstOrNull()?.studentId,
                currentStudentName = newAssignments.firstOrNull()?.studentName,
                assignmentDate = newAssignments.firstOrNull()?.assignmentDate ?: assignmentDate,
                updatedAt = System.currentTimeMillis()
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
            val newStatus = if (remainingAssignments.isEmpty()) "Rafta" else "Okunuyor"
            val updatedBook = book.copy(
                status = newStatus,
                activeAssignments = remainingAssignments,
                currentStudentId = remainingAssignments.firstOrNull()?.studentId,
                currentStudentName = remainingAssignments.firstOrNull()?.studentName,
                assignmentDate = remainingAssignments.firstOrNull()?.assignmentDate,
                updatedAt = System.currentTimeMillis()
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
        val currentBooks = loadBooks(context, classId).toMutableList()
        val currentRecords = loadReadingRecords(context, classId).toMutableList()

        val bookIndex = currentBooks.indexOfFirst { it.id == bookId }
        if (bookIndex != -1) {
            val book = currentBooks[bookIndex]
            val matchingAssignment = book.activeAssignments.firstOrNull { it.studentId == studentId }
            val startDate = matchingAssignment?.assignmentDate ?: book.assignmentDate ?: (returnDate - 86400000L * 3)
            val studentName = matchingAssignment?.studentName ?: book.currentStudentName ?: "Öğrenci"

            // 1. Motivasyon Yıldızı Hesabı & Yıldızlar Sınıfına Ekleme
            val starsEarned = calculateReadingStars(book.pageCount)
            val starRecord = StarRewardRecord(
                id = UUID.randomUUID().toString(),
                classId = classId,
                studentId = studentId,
                studentName = studentName,
                studentNumber = "",
                categoryId = "cat_kitap_kurdu",
                categoryName = "Kitap Kurdu 📚",
                starCount = starsEarned,
                note = "${book.name} okundu (${book.pageCount} sayfa)",
                isLuckyBonus = false,
                timestamp = returnDate
            )
            StarRewardPersistenceHelper.addStarRecord(context, classId, starRecord)

            // 2. Okuma Geçmişi Kaydı Oluşturma
            val readingRecord = ReadingRecord(
                id = UUID.randomUUID().toString(),
                bookId = book.id,
                bookName = book.name,
                pageCount = book.pageCount,
                studentId = studentId,
                studentName = studentName,
                startDate = startDate,
                endDate = returnDate,
                earnedStars = starsEarned,
                teacherUid = Firebase.auth.currentUser?.uid ?: "",
                createdAt = System.currentTimeMillis()
            )
            currentRecords.add(0, readingRecord)
            saveReadingRecords(context, classId, currentRecords)

            // 3. Kitabın Aktif Ödünç Durumunu Güncelleme
            val remainingAssignments = book.activeAssignments.filter { it.studentId != studentId }
            val newStatus = if (remainingAssignments.isEmpty()) "Rafta" else "Okunuyor"
            val updatedBook = book.copy(
                status = newStatus,
                activeAssignments = remainingAssignments,
                currentStudentId = remainingAssignments.firstOrNull()?.studentId,
                currentStudentName = remainingAssignments.firstOrNull()?.studentName,
                assignmentDate = remainingAssignments.firstOrNull()?.assignmentDate,
                updatedAt = System.currentTimeMillis()
            )
            currentBooks[bookIndex] = updatedBook
            saveBooks(context, classId, currentBooks)
        }

        return Pair(currentBooks, currentRecords)
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
            val remainingAssignments = if (studentId != null) {
                book.activeAssignments.filter { it.studentId != studentId }
            } else emptyList()

            val updatedBook = book.copy(
                status = if (remainingAssignments.isEmpty()) "Rafta" else "Okunuyor",
                activeAssignments = remainingAssignments,
                currentStudentId = remainingAssignments.firstOrNull()?.studentId,
                currentStudentName = remainingAssignments.firstOrNull()?.studentName,
                assignmentDate = remainingAssignments.firstOrNull()?.assignmentDate,
                updatedAt = System.currentTimeMillis()
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
            } catch (e: Exception) { System.currentTimeMillis() }
        } else System.currentTimeMillis()
        val records = recordClassRead(context, classRoomId, book.id, students, dateMs)
        return records.take(students.size).sumOf { it.earnedStars }
    }

    private fun getInitialSampleBooks(): List<Book> {
        return emptyList()
    }

    private fun serializeBook(book: Book): JSONObject {
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
            book.currentStudentId?.let { put("currentStudentId", it) }
            book.currentStudentName?.let { put("currentStudentName", it) }
            book.assignmentDate?.let { put("assignmentDate", it) }
            put("createdAt", book.createdAt)
            put("updatedAt", book.updatedAt)
        }
    }

    private fun deserializeBook(obj: JSONObject): Book {
        val assignmentsList = mutableListOf<ActiveAssignment>()
        val assignArray = obj.optJSONArray("activeAssignments")
        if (assignArray != null) {
            for (j in 0 until assignArray.length()) {
                val aObj = assignArray.getJSONObject(j)
                assignmentsList.add(
                    ActiveAssignment(
                        studentId = aObj.optString("studentId", ""),
                        studentName = aObj.optString("studentName", ""),
                        assignmentDate = aObj.optLong("assignmentDate", System.currentTimeMillis())
                    )
                )
            }
        }
        return Book(
            id = obj.optString("id", UUID.randomUUID().toString()),
            registrationNo = obj.optInt("registrationNo", 1),
            name = obj.optString("name", "Kitap"),
            author = obj.optString("author", ""),
            pageCount = obj.optInt("pageCount", 0),
            status = obj.optString("status", if (assignmentsList.isNotEmpty()) "Okunuyor" else "Rafta"),
            activeAssignments = assignmentsList,
            currentStudentId = obj.optString("currentStudentId", "").takeIf { it.isNotEmpty() },
            currentStudentName = obj.optString("currentStudentName", "").takeIf { it.isNotEmpty() },
            assignmentDate = if (obj.has("assignmentDate")) obj.optLong("assignmentDate") else null,
            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
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
            put("startDate", r.startDate)
            put("endDate", r.endDate)
            put("earnedStars", r.earnedStars)
        }
    }

    fun uploadBooksToFirestore(context: Context, classId: String, books: List<Book>) {
        if (classId.isBlank() || classId == "default") return
        val db = getFirestore(context) ?: return
        syncScope.launch {
            try {
                val batch = db.batch()
                books.forEach { book ->
                    val doc = db.collection("classrooms").document(classId).collection("library_books").document(book.id)
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
                            "json" to serializeBook(book).toString(),
                            "updatedAt" to System.currentTimeMillis()
                        ),
                        SetOptions.merge()
                    )
                }
                batch.commit()
            } catch (e: Exception) {
                Log.e(TAG, "Failed uploading books to Firestore", e)
            }
        }
    }

    fun uploadRecordsToFirestore(context: Context, classId: String, records: List<ReadingRecord>) {
        if (classId.isBlank() || classId == "default") return
        val db = getFirestore(context) ?: return
        syncScope.launch {
            try {
                val batch = db.batch()
                records.take(50).forEach { r ->
                    val doc = db.collection("classrooms").document(classId).collection("reading_records").document(r.id)
                    batch.set(
                        doc,
                        mapOf(
                            "id" to r.id,
                            "bookId" to r.bookId,
                            "bookName" to r.bookName,
                            "pageCount" to r.pageCount,
                            "studentId" to r.studentId,
                            "studentName" to r.studentName,
                            "startDate" to r.startDate,
                            "endDate" to r.endDate,
                            "earnedStars" to r.earnedStars,
                            "json" to serializeReadingRecord(r).toString(),
                            "updatedAt" to System.currentTimeMillis()
                        ),
                        SetOptions.merge()
                    )
                }
                batch.commit()
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
                        val list = mutableListOf<Book>()
                        for (doc in snapshot.documents) {
                            val jsonStr = doc.getString("json")
                            if (!jsonStr.isNullOrBlank()) {
                                try {
                                    list.add(deserializeBook(JSONObject(jsonStr)))
                                } catch (_: Exception) {}
                            } else {
                                val regNo = (doc.getLong("registrationNo") ?: 1L).toInt()
                                val name = doc.getString("name") ?: "Kitap"
                                val author = doc.getString("author") ?: ""
                                val pageCount = (doc.getLong("pageCount") ?: 0L).toInt()
                                val status = doc.getString("status") ?: "Rafta"
                                val sId = doc.getString("currentStudentId")?.takeIf { it.isNotEmpty() }
                                val sName = doc.getString("currentStudentName")?.takeIf { it.isNotEmpty() }
                                list.add(
                                    Book(
                                        id = doc.id,
                                        registrationNo = regNo,
                                        name = name,
                                        author = author,
                                        pageCount = pageCount,
                                        status = status,
                                        currentStudentId = sId,
                                        currentStudentName = sName
                                    )
                                )
                            }
                        }
                        list.sortBy { it.registrationNo }
                        if (list.isNotEmpty()) {
                            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            val array = JSONArray()
                            list.forEach { array.put(serializeBook(it)) }
                            prefs.edit().putString("$KEY_BOOKS_PREFIX$classId", array.toString()).apply()
                        }
                        onUpdate(list)
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
                        val list = mutableListOf<ReadingRecord>()
                        for (doc in snapshot.documents) {
                            val bId = doc.getString("bookId") ?: ""
                            val bName = doc.getString("bookName") ?: ""
                            val page = (doc.getLong("pageCount") ?: 0L).toInt()
                            val sId = doc.getString("studentId") ?: ""
                            val sName = doc.getString("studentName") ?: ""
                            val sDate = doc.getLong("startDate") ?: System.currentTimeMillis()
                            val eDate = doc.getLong("endDate") ?: System.currentTimeMillis()
                            val stars = (doc.getLong("earnedStars") ?: 1L).toInt()
                            list.add(
                                ReadingRecord(
                                    id = doc.id,
                                    bookId = bId,
                                    bookName = bName,
                                    pageCount = page,
                                    studentId = sId,
                                    studentName = sName,
                                    startDate = sDate,
                                    endDate = eDate,
                                    earnedStars = stars
                                )
                            )
                        }
                        list.sortByDescending { it.endDate }
                        if (list.isNotEmpty()) {
                            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            val array = JSONArray()
                            list.forEach { array.put(serializeReadingRecord(it)) }
                            prefs.edit().putString("$KEY_RECORDS_PREFIX$classId", array.toString()).apply()
                        }
                        onUpdate(list)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in observeFirestoreReadingRecords", e)
                    }
                }
        } catch (_: Exception) {
            null
        }
    }
}
