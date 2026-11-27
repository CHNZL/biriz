package com.example.util

import android.content.Context
import android.util.Log
import com.example.data.model.Book
import com.example.data.model.LibraryBookRequest
import com.example.data.model.LibraryRequestStatus
import com.example.data.model.LibraryRequestType
import com.example.data.model.Student
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

data class StartBookRequestValidation(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val matchedExistingBook: Book? = null,
    val resolvedBookName: String = "",
    val resolvedAuthor: String = "",
    val resolvedPageCount: Int = 0
)

data class FinishBookRequestValidation(
    val isValid: Boolean,
    val errorMessage: String? = null
)

data class LibraryRequestActionResult(
    val success: Boolean,
    val message: String,
    val earnedStars: Int = 0,
    val request: LibraryBookRequest? = null
)

/**
 * Veli -> Öğretmen Kitap Ekleme / Okumaya Başlama ve Kitap Bitirme Talepleri
 * Kalıcı Veri & İş Mantığı Yöneticisi (Firestore SSOT + Yerel Önbellek)
 */
object LibraryRequestPersistenceHelper {
    private const val TAG = "LibraryRequestHelper"
    private const val PREFS_NAME = "biriz_library_requests_prefs"
    private const val KEY_REQUESTS_PREFIX = "lib_requests_"
    private const val COLLECTION_NAME = "library_requests"

    private val syncScope = CoroutineScope(Dispatchers.IO)
    private val prefsLock = Any()

    private fun getFirestore(context: Context): FirebaseFirestore? {
        return try {
            FirestoreProvider.get(context)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Sınıf kitaplığında Türkçe büyük/küçük harf duyarsız tam ad eşleşmesi arar.
     */
    fun findMatchingLibraryBook(books: List<Book>, rawName: String): Book? {
        val cleanUpper = TurkishTextHelper.toTurkishUpperCase(rawName.trim())
        if (cleanUpper.isBlank()) return null
        return books.firstOrNull { b ->
            TurkishTextHelper.toTurkishUpperCase(b.name.trim()) == cleanUpper
        }
    }

    /**
     * Takvim günü başlangıcını (00:00:00.000) döndürerek tarih karşılaştırmalarını saat farkından bağımsız yapar.
     */
    fun normalizeCalendarDayStart(millis: Long): Long {
        if (millis <= 0L) return 0L
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    /**
     * Veli tarafından gönderilecek kitap ekleme / okumaya başlama talebini doğrular.
     * Kitap sınıf kütüphanesinde kayıtlıysa kitap adı, yazar ve sayfa sayısı kütüphanedeki kayıttan kilitli olarak alınır.
     */
    fun validateStartBookRequest(
        studentId: String,
        rawBookName: String,
        rawAuthor: String,
        enteredPageCount: Int,
        startDateMs: Long,
        classBooks: List<Book>,
        existingRequests: List<LibraryBookRequest>
    ): StartBookRequestValidation {
        if (studentId.isBlank()) {
            return StartBookRequestValidation(false, "Öğrenci bilgisi bulunamadı.")
        }
        val trimmedName = rawBookName.trim()
        if (trimmedName.isBlank()) {
            return StartBookRequestValidation(false, "Lütfen kitap adını girin.")
        }
        if (startDateMs <= 0L) {
            return StartBookRequestValidation(false, "Lütfen geçerli bir başlama tarihi seçin.")
        }

        val matched = findMatchingLibraryBook(classBooks, trimmedName)
        val resolvedName = matched?.name?.trim() ?: TurkishTextHelper.toTurkishTitleCase(trimmedName)
        val resolvedAuthor = matched?.author?.trim() ?: TurkishTextHelper.toTurkishTitleCase(rawAuthor.trim())
        val resolvedPageCount = matched?.pageCount ?: enteredPageCount

        if (resolvedPageCount <= 0) {
            return StartBookRequestValidation(
                isValid = false,
                errorMessage = "Sayfa sayısı 0'dan büyük olmalıdır.",
                matchedExistingBook = matched
            )
        }

        // Öğrenci bu kitabı zaten aktif olarak okuyorsa mükerrer başlama talebini engelle
        if (matched != null) {
            val alreadyReading = matched.activeAssignments.any { it.studentId == studentId } ||
                matched.currentStudentId == studentId
            if (alreadyReading) {
                return StartBookRequestValidation(
                    isValid = false,
                    errorMessage = "Öğrenciniz bu kitabı zaten okumaktadır. Bitirdiyseniz 'Kitap Bitti' tuşunu kullanabilirsiniz.",
                    matchedExistingBook = matched,
                    resolvedBookName = resolvedName,
                    resolvedAuthor = resolvedAuthor,
                    resolvedPageCount = resolvedPageCount
                )
            }
        }

        // Aynı öğrenci + aynı kitap için zaten bekleyen bir başlama talebi var mı?
        val normalizedUpper = TurkishTextHelper.toTurkishUpperCase(resolvedName)
        val hasPendingSame = existingRequests.any { req ->
            req.status == LibraryRequestStatus.PENDING &&
                req.requestType == LibraryRequestType.START_BOOK &&
                req.studentId == studentId &&
                ((matched != null && req.existingBookId == matched.id) ||
                    TurkishTextHelper.toTurkishUpperCase(req.bookName.trim()) == normalizedUpper)
        }
        if (hasPendingSame) {
            return StartBookRequestValidation(
                isValid = false,
                errorMessage = "Bu kitap için öğretmen onayı bekleyen bir talebiniz zaten bulunuyor.",
                matchedExistingBook = matched,
                resolvedBookName = resolvedName,
                resolvedAuthor = resolvedAuthor,
                resolvedPageCount = resolvedPageCount
            )
        }

        return StartBookRequestValidation(
            isValid = true,
            matchedExistingBook = matched,
            resolvedBookName = resolvedName,
            resolvedAuthor = resolvedAuthor,
            resolvedPageCount = resolvedPageCount
        )
    }

    /**
     * Veli tarafından gönderilecek 'Kitap Bitti' talebini doğrular.
     */
    fun validateFinishBookRequest(
        studentId: String,
        book: Book,
        startDateMs: Long,
        endDateMs: Long,
        existingRequests: List<LibraryBookRequest>
    ): FinishBookRequestValidation {
        if (studentId.isBlank() || book.id.isBlank()) {
            return FinishBookRequestValidation(false, "Kitap veya öğrenci bilgisi eksik.")
        }
        if (endDateMs <= 0L) {
            return FinishBookRequestValidation(false, "Lütfen geçerli bir bitiş tarihi seçin.")
        }
        if (startDateMs > 0L && normalizeCalendarDayStart(endDateMs) < normalizeCalendarDayStart(startDateMs)) {
            return FinishBookRequestValidation(false, "Bitiş tarihi, okumaya başlama tarihinden önce olamaz.")
        }

        val normalizedUpper = TurkishTextHelper.toTurkishUpperCase(book.name.trim())
        val hasPendingFinish = existingRequests.any { req ->
            req.status == LibraryRequestStatus.PENDING &&
                req.requestType == LibraryRequestType.FINISH_BOOK &&
                req.studentId == studentId &&
                (req.existingBookId == book.id ||
                    TurkishTextHelper.toTurkishUpperCase(req.bookName.trim()) == normalizedUpper)
        }
        if (hasPendingFinish) {
            return FinishBookRequestValidation(false, "Bu kitap için öğretmen onayı bekleyen bir bitirme talebiniz zaten var.")
        }

        return FinishBookRequestValidation(true)
    }

    fun loadRequests(context: Context, classId: String): List<LibraryBookRequest> {
        if (classId.isBlank()) return emptyList()
        val jsonStr = synchronized(prefsLock) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString("$KEY_REQUESTS_PREFIX$classId", null)
        } ?: return emptyList()

        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<LibraryBookRequest>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val req = deserializeRequest(obj)
                if (req.id.isNotBlank() && req.studentId.isNotBlank() && req.bookName.isNotBlank()) {
                    list.add(req)
                }
            }
            list.distinctBy { it.id }.sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse cached library requests for $classId", e)
            emptyList()
        }
    }

    private fun saveRequestsLocalOnly(context: Context, classId: String, requests: List<LibraryBookRequest>) {
        if (classId.isBlank()) return
        val sorted = requests.distinctBy { it.id }.sortedByDescending { it.createdAt }
        val arr = JSONArray()
        sorted.forEach { arr.put(serializeRequest(it)) }
        synchronized(prefsLock) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString("$KEY_REQUESTS_PREFIX$classId", arr.toString())
                .apply()
        }
    }

    /**
     * Veli yeni kitap girişi / okumaya başlama talebi gönderir.
     */
    fun submitStartBookRequest(
        context: Context,
        classId: String,
        student: Student,
        parentName: String,
        parentEmail: String,
        rawBookName: String,
        rawAuthor: String,
        enteredPageCount: Int,
        startDateMs: Long,
        classBooks: List<Book>
    ): LibraryRequestActionResult {
        val currentRequests = loadRequests(context, classId)
        val validation = validateStartBookRequest(
            studentId = student.id,
            rawBookName = rawBookName,
            rawAuthor = rawAuthor,
            enteredPageCount = enteredPageCount,
            startDateMs = startDateMs,
            classBooks = classBooks,
            existingRequests = currentRequests
        )
        if (!validation.isValid) {
            return LibraryRequestActionResult(false, validation.errorMessage ?: "Talep doğrulanamadı.")
        }

        val nowMs = System.currentTimeMillis()
        val request = LibraryBookRequest(
            id = "lreq_${UUID.randomUUID()}",
            classId = classId,
            studentId = student.id,
            studentName = student.fullName,
            studentNumber = student.studentNumber,
            parentEmail = parentEmail.trim(),
            parentName = parentName.trim().ifBlank { "Veli" },
            requestType = LibraryRequestType.START_BOOK,
            status = LibraryRequestStatus.PENDING,
            existingBookId = validation.matchedExistingBook?.id,
            bookName = validation.resolvedBookName,
            author = validation.resolvedAuthor,
            pageCount = validation.resolvedPageCount,
            startDate = startDateMs,
            endDate = null,
            earnedStarsPreview = LibraryPersistenceHelper.calculateReadingStars(validation.resolvedPageCount),
            rejectionReason = "",
            createdAt = nowMs,
            reviewedAt = null,
            reviewedByTeacherUid = ""
        )

        val updatedList = listOf(request) + currentRequests.filter { it.id != request.id }
        saveRequestsLocalOnly(context, classId, updatedList)
        uploadRequestToFirestore(context, classId, request)

        return LibraryRequestActionResult(
            success = true,
            message = "📚 Kitap talebiniz öğretmeninize gönderildi. Onaylandığında okuma kaydınız başlayacaktır.",
            request = request
        )
    }

    /**
     * Veli aktif okunan bir kitap için 'Kitap Bitti' talebi gönderir.
     */
    fun submitFinishBookRequest(
        context: Context,
        classId: String,
        student: Student,
        parentName: String,
        parentEmail: String,
        book: Book,
        startDateMs: Long,
        endDateMs: Long
    ): LibraryRequestActionResult {
        val currentRequests = loadRequests(context, classId)
        val validation = validateFinishBookRequest(
            studentId = student.id,
            book = book,
            startDateMs = startDateMs,
            endDateMs = endDateMs,
            existingRequests = currentRequests
        )
        if (!validation.isValid) {
            return LibraryRequestActionResult(false, validation.errorMessage ?: "Talep doğrulanamadı.")
        }

        val nowMs = System.currentTimeMillis()
        val resolvedStartMs = if (startDateMs > 0L) startDateMs else endDateMs
        val expectedStars = LibraryPersistenceHelper.calculateReadingStars(book.pageCount)
        val request = LibraryBookRequest(
            id = "lreq_${UUID.randomUUID()}",
            classId = classId,
            studentId = student.id,
            studentName = student.fullName,
            studentNumber = student.studentNumber,
            parentEmail = parentEmail.trim(),
            parentName = parentName.trim().ifBlank { "Veli" },
            requestType = LibraryRequestType.FINISH_BOOK,
            status = LibraryRequestStatus.PENDING,
            existingBookId = book.id,
            bookName = book.name.trim(),
            author = book.author.trim(),
            pageCount = book.pageCount,
            startDate = resolvedStartMs,
            endDate = endDateMs,
            earnedStarsPreview = expectedStars,
            rejectionReason = "",
            createdAt = nowMs,
            reviewedAt = null,
            reviewedByTeacherUid = ""
        )

        val updatedList = listOf(request) + currentRequests.filter { it.id != request.id }
        saveRequestsLocalOnly(context, classId, updatedList)
        uploadRequestToFirestore(context, classId, request)

        return LibraryRequestActionResult(
            success = true,
            message = "🏁 Kitap bitirme talebiniz öğretmeninize gönderildi.",
            earnedStars = expectedStars,
            request = request
        )
    }

    /**
     * Öğretmen talebi onaylar:
     * - START_BOOK: Kitap kütüphanede yoksa ekler + öğrencinin belirttiği başlama tarihinde ödünç başlatır;
     *               kütüphanede varsa o kitabı öğrencinin belirttiği başlama tarihinde ödünç başlatır.
     * - FINISH_BOOK: Kitabı öğrenciden iade alır, okuma geçmişine kaydeder ve Kitap Kurdu yıldızını verir.
     */
    fun approveRequest(
        context: Context,
        classId: String,
        request: LibraryBookRequest,
        students: List<Student>
    ): LibraryRequestActionResult {
        if (request.status != LibraryRequestStatus.PENDING) {
            return LibraryRequestActionResult(false, "Bu talep daha önce sonuçlandırılmış.")
        }

        val currentBooks = LibraryPersistenceHelper.loadBooks(context, classId)
        val matchedStudent = students.firstOrNull { it.id == request.studentId }
        val resolvedStudentName = matchedStudent?.fullName?.takeIf { it.isNotBlank() } ?: request.studentName
        val resolvedStudentNumber = matchedStudent?.studentNumber?.takeIf { it.isNotBlank() } ?: request.studentNumber
        val teacherUid = try { Firebase.auth.currentUser?.uid ?: "" } catch (_: Throwable) { "" }
        val nowMs = System.currentTimeMillis()

        return when (request.requestType) {
            LibraryRequestType.START_BOOK -> {
                val existingBook = currentBooks.firstOrNull { !request.existingBookId.isNullOrBlank() && it.id == request.existingBookId }
                    ?: findMatchingLibraryBook(currentBooks, request.bookName)

                val resolvedBookId: String
                val resolvedPageCount: Int
                val startMs = if (request.startDate > 0L) request.startDate else nowMs
                val startStr = SimpleDateFormat("dd.MM.yyyy", Locale("tr")).format(Date(startMs))

                if (existingBook != null) {
                    val targetStudent = matchedStudent ?: Student(
                        id = request.studentId,
                        fullName = resolvedStudentName,
                        studentNumber = resolvedStudentNumber,
                        gender = ""
                    )
                    LibraryPersistenceHelper.borrowBook(
                        context = context,
                        classId = classId,
                        bookId = existingBook.id,
                        students = listOf(targetStudent),
                        assignmentDate = startMs
                    )
                    resolvedBookId = existingBook.id
                    resolvedPageCount = existingBook.pageCount
                } else {
                    val titleCasedName = TurkishTextHelper.toTurkishTitleCase(request.bookName)
                    val titleCasedAuthor = TurkishTextHelper.toTurkishTitleCase(request.author)
                    val createdBook = LibraryPersistenceHelper.addBook(
                        context = context,
                        classRoomId = classId,
                        name = titleCasedName,
                        author = titleCasedAuthor,
                        pageCount = request.pageCount.coerceAtLeast(1),
                        registrationNo = 0,
                        initialStudentId = request.studentId,
                        initialStudentName = resolvedStudentName,
                        assignmentDate = startStr
                    )
                    resolvedBookId = createdBook.id
                    resolvedPageCount = createdBook.pageCount
                }

                val updatedReq = request.copy(
                    status = LibraryRequestStatus.APPROVED,
                    existingBookId = resolvedBookId,
                    pageCount = resolvedPageCount,
                    earnedStarsPreview = LibraryPersistenceHelper.calculateReadingStars(resolvedPageCount),
                    reviewedAt = nowMs,
                    reviewedByTeacherUid = teacherUid
                )
                persistUpdatedRequest(context, classId, updatedReq)

                val actionMsg = if (existingBook != null) {
                    "✅ Onaylandı! \"${request.bookName}\" kitabı $resolvedStudentName için okuma kaydına alındı."
                } else {
                    "✅ Onaylandı! \"${request.bookName}\" kitaplığa eklendi ve $resolvedStudentName için okuma kaydı başlatıldı."
                }
                LibraryRequestActionResult(true, actionMsg, request = updatedReq)
            }

            LibraryRequestType.FINISH_BOOK -> {
                val targetBook = currentBooks.firstOrNull { !request.existingBookId.isNullOrBlank() && it.id == request.existingBookId }
                    ?: findMatchingLibraryBook(currentBooks, request.bookName)

                if (targetBook == null) {
                    return LibraryRequestActionResult(
                        false,
                        "İlgili kitap kütüphanede bulunamadı. Kitap silinmiş olabilir; talebi reddedebilirsiniz."
                    )
                }

                val finishMs = request.endDate?.takeIf { it > 0L } ?: nowMs
                val earnedStars = LibraryPersistenceHelper.returnBook(
                    context = context,
                    classRoomId = classId,
                    bookId = targetBook.id,
                    studentId = request.studentId,
                    studentName = resolvedStudentName,
                    startDate = request.startDate,
                    returnDate = finishMs
                )

                val updatedReq = request.copy(
                    status = LibraryRequestStatus.APPROVED,
                    existingBookId = targetBook.id,
                    pageCount = targetBook.pageCount,
                    earnedStarsPreview = earnedStars,
                    reviewedAt = nowMs,
                    reviewedByTeacherUid = teacherUid
                )
                persistUpdatedRequest(context, classId, updatedReq)

                LibraryRequestActionResult(
                    success = true,
                    message = "🎉 Onaylandı! $resolvedStudentName \"${targetBook.name}\" kitabını tamamladı (+$earnedStars ⭐).",
                    earnedStars = earnedStars,
                    request = updatedReq
                )
            }
        }
    }

    /**
     * Öğretmen talebi reddeder (isteğe bağlı açıklama ile).
     */
    fun rejectRequest(
        context: Context,
        classId: String,
        request: LibraryBookRequest,
        reason: String = ""
    ): LibraryRequestActionResult {
        if (request.status != LibraryRequestStatus.PENDING) {
            return LibraryRequestActionResult(false, "Bu talep daha önce sonuçlandırılmış.")
        }
        val teacherUid = try { Firebase.auth.currentUser?.uid ?: "" } catch (_: Throwable) { "" }
        val updatedReq = request.copy(
            status = LibraryRequestStatus.REJECTED,
            rejectionReason = reason.trim(),
            reviewedAt = System.currentTimeMillis(),
            reviewedByTeacherUid = teacherUid
        )
        persistUpdatedRequest(context, classId, updatedReq)
        return LibraryRequestActionResult(
            success = true,
            message = "Talep reddedildi ve veliye bilgi iletildi.",
            request = updatedReq
        )
    }

    /**
     * Veli, henüz öğretmen tarafından incelenmemiş bekleyen talebini geri çeker.
     */
    fun cancelPendingRequest(context: Context, classId: String, requestId: String) {
        if (classId.isBlank() || requestId.isBlank()) return
        val current = loadRequests(context, classId)
        val filtered = current.filter { it.id != requestId }
        saveRequestsLocalOnly(context, classId, filtered)

        if (classId != "default") {
            syncScope.launch {
                try {
                    getFirestore(context)
                        ?.collection("classrooms")?.document(classId)
                        ?.collection(COLLECTION_NAME)?.document(requestId)
                        ?.delete()
                } catch (e: Exception) {
                    Log.e(TAG, "Error deleting pending library request $requestId", e)
                }
            }
        }
    }

    private fun persistUpdatedRequest(context: Context, classId: String, updated: LibraryBookRequest) {
        val current = loadRequests(context, classId).toMutableList()
        val idx = current.indexOfFirst { it.id == updated.id }
        if (idx >= 0) {
            current[idx] = updated
        } else {
            current.add(0, updated)
        }
        saveRequestsLocalOnly(context, classId, current)
        uploadRequestToFirestore(context, classId, updated)
    }

    private fun uploadRequestToFirestore(context: Context, classId: String, request: LibraryBookRequest) {
        if (classId.isBlank() || classId == "default") return
        syncScope.launch {
            try {
                val db = getFirestore(context) ?: return@launch
                val data = hashMapOf<String, Any?>(
                    "id" to request.id,
                    "classId" to classId,
                    "studentId" to request.studentId,
                    "studentName" to request.studentName,
                    "studentNumber" to request.studentNumber,
                    "parentEmail" to request.parentEmail,
                    "parentName" to request.parentName,
                    "requestType" to request.requestType.name,
                    "status" to request.status.name,
                    "existingBookId" to request.existingBookId,
                    "bookName" to request.bookName,
                    "author" to request.author,
                    "pageCount" to request.pageCount,
                    "startDate" to request.startDate,
                    "endDate" to request.endDate,
                    "earnedStarsPreview" to request.earnedStarsPreview,
                    "rejectionReason" to request.rejectionReason,
                    "createdAt" to request.createdAt,
                    "reviewedAt" to request.reviewedAt,
                    "reviewedByTeacherUid" to request.reviewedByTeacherUid
                )
                db.collection("classrooms").document(classId)
                    .collection(COLLECTION_NAME).document(request.id)
                    .set(data, SetOptions.merge())
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading library request ${request.id} to Firestore", e)
            }
        }
    }

    fun observeFirestoreRequests(
        context: Context,
        classId: String,
        onUpdate: (List<LibraryBookRequest>) -> Unit
    ): ListenerRegistration? {
        if (classId.isBlank() || classId == "default") return null
        val db = getFirestore(context) ?: return null
        return db.collection("classrooms").document(classId)
            .collection(COLLECTION_NAME)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                try {
                    val list = snapshot.documents.mapNotNull { parseDocument(it, classId) }
                        .distinctBy { it.id }
                        .sortedByDescending { it.createdAt }
                    saveRequestsLocalOnly(context, classId, list)
                    onUpdate(list)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing library requests snapshot", e)
                }
            }
    }

    suspend fun fetchFirestoreRequestsOnce(
        db: FirebaseFirestore,
        classId: String
    ): List<LibraryBookRequest> {
        if (classId.isBlank() || classId == "default") return emptyList()
        return try {
            val snap = db.collection("classrooms").document(classId)
                .collection(COLLECTION_NAME)
                .get()
                .await()
            snap.documents.mapNotNull { parseDocument(it, classId) }
                .distinctBy { it.id }
                .sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching library requests once for $classId", e)
            emptyList()
        }
    }

    private fun safeDocString(doc: DocumentSnapshot, field: String, fallback: String = ""): String {
        return try {
            when (val v = doc.get(field)) {
                is String -> v
                is Number, is Boolean -> v.toString()
                else -> fallback
            }
        } catch (_: Exception) {
            fallback
        }
    }

    private fun safeDocLong(doc: DocumentSnapshot, field: String, fallback: Long = 0L): Long {
        return try {
            when (val v = doc.get(field)) {
                is Number -> v.toLong()
                is String -> v.trim().toLongOrNull() ?: fallback
                else -> fallback
            }
        } catch (_: Exception) {
            fallback
        }
    }

    private fun parseDocument(doc: DocumentSnapshot, classId: String): LibraryBookRequest? {
        val id = safeDocString(doc, "id", doc.id).ifBlank { doc.id }
        val studentId = safeDocString(doc, "studentId")
        val bookName = safeDocString(doc, "bookName")
        if (id.isBlank() || studentId.isBlank() || bookName.isBlank()) return null

        val typeStr = safeDocString(doc, "requestType", LibraryRequestType.START_BOOK.name)
        val statusStr = safeDocString(doc, "status", LibraryRequestStatus.PENDING.name)
        val reqType = LibraryRequestType.entries.firstOrNull { it.name == typeStr } ?: LibraryRequestType.START_BOOK
        val reqStatus = LibraryRequestStatus.entries.firstOrNull { it.name == statusStr } ?: LibraryRequestStatus.PENDING
        val existingBookId = safeDocString(doc, "existingBookId").takeIf { it.isNotBlank() }
        val endDateRaw = safeDocLong(doc, "endDate", 0L)
        val reviewedAtRaw = safeDocLong(doc, "reviewedAt", 0L)

        return LibraryBookRequest(
            id = id,
            classId = safeDocString(doc, "classId", classId).ifBlank { classId },
            studentId = studentId,
            studentName = safeDocString(doc, "studentName"),
            studentNumber = safeDocString(doc, "studentNumber"),
            parentEmail = safeDocString(doc, "parentEmail"),
            parentName = safeDocString(doc, "parentName"),
            requestType = reqType,
            status = reqStatus,
            existingBookId = existingBookId,
            bookName = bookName,
            author = safeDocString(doc, "author"),
            pageCount = safeDocLong(doc, "pageCount", 0L).toInt().coerceAtLeast(0),
            startDate = safeDocLong(doc, "startDate", 0L),
            endDate = endDateRaw.takeIf { it > 0L },
            earnedStarsPreview = safeDocLong(doc, "earnedStarsPreview", 0L).toInt().coerceAtLeast(0),
            rejectionReason = safeDocString(doc, "rejectionReason"),
            createdAt = safeDocLong(doc, "createdAt", System.currentTimeMillis()),
            reviewedAt = reviewedAtRaw.takeIf { it > 0L },
            reviewedByTeacherUid = safeDocString(doc, "reviewedByTeacherUid")
        )
    }

    private fun serializeRequest(req: LibraryBookRequest): JSONObject {
        return JSONObject().apply {
            put("id", req.id)
            put("classId", req.classId)
            put("studentId", req.studentId)
            put("studentName", req.studentName)
            put("studentNumber", req.studentNumber)
            put("parentEmail", req.parentEmail)
            put("parentName", req.parentName)
            put("requestType", req.requestType.name)
            put("status", req.status.name)
            req.existingBookId?.let { put("existingBookId", it) }
            put("bookName", req.bookName)
            put("author", req.author)
            put("pageCount", req.pageCount)
            put("startDate", req.startDate)
            req.endDate?.let { put("endDate", it) }
            put("earnedStarsPreview", req.earnedStarsPreview)
            put("rejectionReason", req.rejectionReason)
            put("createdAt", req.createdAt)
            req.reviewedAt?.let { put("reviewedAt", it) }
            put("reviewedByTeacherUid", req.reviewedByTeacherUid)
        }
    }

    private fun deserializeRequest(obj: JSONObject): LibraryBookRequest {
        val typeStr = obj.optString("requestType", LibraryRequestType.START_BOOK.name)
        val statusStr = obj.optString("status", LibraryRequestStatus.PENDING.name)
        val reqType = LibraryRequestType.entries.firstOrNull { it.name == typeStr } ?: LibraryRequestType.START_BOOK
        val reqStatus = LibraryRequestStatus.entries.firstOrNull { it.name == statusStr } ?: LibraryRequestStatus.PENDING
        val existingBookId = if (obj.has("existingBookId") && !obj.isNull("existingBookId")) {
            obj.optString("existingBookId", "").takeIf { it.isNotBlank() }
        } else null
        val endDate = if (obj.has("endDate") && !obj.isNull("endDate")) {
            obj.optLong("endDate", 0L).takeIf { it > 0L }
        } else null
        val reviewedAt = if (obj.has("reviewedAt") && !obj.isNull("reviewedAt")) {
            obj.optLong("reviewedAt", 0L).takeIf { it > 0L }
        } else null

        return LibraryBookRequest(
            id = obj.optString("id", ""),
            classId = obj.optString("classId", ""),
            studentId = obj.optString("studentId", ""),
            studentName = obj.optString("studentName", ""),
            studentNumber = obj.optString("studentNumber", ""),
            parentEmail = obj.optString("parentEmail", ""),
            parentName = obj.optString("parentName", ""),
            requestType = reqType,
            status = reqStatus,
            existingBookId = existingBookId,
            bookName = obj.optString("bookName", ""),
            author = obj.optString("author", ""),
            pageCount = obj.optInt("pageCount", 0).coerceAtLeast(0),
            startDate = obj.optLong("startDate", 0L),
            endDate = endDate,
            earnedStarsPreview = obj.optInt("earnedStarsPreview", 0).coerceAtLeast(0),
            rejectionReason = obj.optString("rejectionReason", ""),
            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
            reviewedAt = reviewedAt,
            reviewedByTeacherUid = obj.optString("reviewedByTeacherUid", "")
        )
    }
}
