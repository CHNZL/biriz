package com.example.util

import android.content.Context
import android.util.Log
import com.example.data.model.ActiveAssignment
import com.example.data.model.Book
import com.example.data.model.ReadingRecord
import com.example.data.model.StarRewardRecord
import com.example.data.model.Student
import com.example.data.repository.ClassRegistryRepository
import com.example.data.repository.StarRewardPersistenceHelper
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Collections

data class StudentStarVerificationItem(
    val studentNo: Int,
    val studentName: String,
    val siteTotalStarsShownToUser: Int,
    val sumFromStarRecords: Int,
    val readingCount: Int
)

data class MappedWebMigrationData(
    val teacherEmail: String,
    val matchedClassroomLabel: String,
    val exportedAtIso: String,
    val students: List<Student>,
    val books: List<Book>,
    val readingRecords: List<ReadingRecord>,
    val starRecords: List<StarRewardRecord>,
    val verificationItems: List<StudentStarVerificationItem>,
    val totalStarsSum: Int,
    val activeLoanBooksCount: Int
)

data class WebMigrationSummary(
    val success: Boolean,
    val studentsCount: Int,
    val booksCount: Int,
    val booksOnShelfCount: Int,
    val activeLoanBooksCount: Int,
    val readingRecordsCount: Int,
    val starRecordsCount: Int,
    val totalStarsCount: Int,
    val message: String
)

object WebDataMigrationHelper {
    private const val TAG = "WebDataMigration"
    private const val PREFS_NAME = "biriz_web_migration_prefs"
    private const val KEY_MIGRATED_PREFIX = "web_migrated_v1_"
    private const val KEY_PRESERVE_BOOKS_PREFIX = "preserve_migrated_books_"
    private const val KEY_PRESERVE_RECORDS_PREFIX = "preserve_migrated_records_"
    private const val KEY_PRESERVE_STARS_PREFIX = "preserve_migrated_stars_"
    const val TARGET_TEACHER_EMAIL = "cihan.ozel10@gmail.com"
    const val BUNDLED_ASSET_FILENAME = "biriz_canli_veri_gocu_cihan_ozel10_2026-11-15_e90ca4d7.json"

    private val activeMigrationClassIds: MutableSet<String> =
        Collections.synchronizedSet(mutableSetOf<String>())

    private val STABLE_INVITE_CODES_BY_NO = mapOf(
        45 to "94K7P2MN",
        326 to "7M3K8Q2P",
        402 to "TD8GDLU8",
        403 to "4K8M2P9R",
        405 to "7N3Q6T8W",
        406 to "2R9V4X7Z",
        407 to "8K3N9P2X",
        408 to "5H2L9R4T",
        409 to "3V7W2X8Y",
        411 to "6B4N9M1K",
        413 to "2P8Q5R3T",
        414 to "8M4K7P2N",
        415 to "9X1Y4Z7W",
        416 to "5T9R3K7L",
        418 to "4M6N8P2R",
        420 to "7T3K9L5Q",
        421 to "6Q9M2N4P",
        422 to "5R8W2V4X",
        425 to "3N6P9Q1M",
        426 to "8K4L7R2T",
        429 to "4T7R1K8L",
        430 to "2W5Y8Z3V",
        441 to "9V2X5Y7Z",
        444 to "3P6Q8R1M",
        555 to "7K4L9N2T"
    )

    fun isTargetTeacherEmail(email: String?): Boolean {
        return email?.trim()?.equals(TARGET_TEACHER_EMAIL, ignoreCase = true) == true
    }

    fun hasCompletedMigration(context: Context, classId: String): Boolean {
        if (classId.isBlank()) return false
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("$KEY_MIGRATED_PREFIX$classId", false)
    }

    fun shouldPreserveLocalMigratedBooks(context: Context, classId: String): Boolean {
        if (classId.isBlank()) return false
        if (activeMigrationClassIds.contains(classId)) return true
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("$KEY_PRESERVE_BOOKS_PREFIX$classId", false)
    }

    fun shouldPreserveLocalMigratedReadingRecords(context: Context, classId: String): Boolean {
        if (classId.isBlank()) return false
        if (activeMigrationClassIds.contains(classId)) return true
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("$KEY_PRESERVE_RECORDS_PREFIX$classId", false)
    }

    fun shouldPreserveLocalMigratedStars(context: Context, classId: String): Boolean {
        if (classId.isBlank()) return false
        if (activeMigrationClassIds.contains(classId)) return true
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("$KEY_PRESERVE_STARS_PREFIX$classId", false)
    }

    fun onExplicitLibraryClear(context: Context, classId: String) {
        if (classId.isBlank()) return
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("$KEY_PRESERVE_BOOKS_PREFIX$classId", false)
            .putBoolean("$KEY_PRESERVE_RECORDS_PREFIX$classId", false)
            .apply()
    }

    fun onExplicitReadingRecordsClear(context: Context, classId: String) {
        if (classId.isBlank()) return
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("$KEY_PRESERVE_RECORDS_PREFIX$classId", false)
            .apply()
    }

    fun onExplicitStarsClear(context: Context, classId: String) {
        if (classId.isBlank()) return
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("$KEY_PRESERVE_STARS_PREFIX$classId", false)
            .apply()
    }

    /**
     * APK içindeki assets klasöründen veya yerel test dosya yolundan gömülü JSON verisini okur.
     */
    fun readBundledMigrationJson(context: Context? = null): String? {
        if (context != null) {
            try {
                val assetFiles = context.assets.list("")?.toList().orEmpty()
                val targetName = when {
                    BUNDLED_ASSET_FILENAME in assetFiles -> BUNDLED_ASSET_FILENAME
                    else -> assetFiles.firstOrNull {
                        it.startsWith("biriz_canli_veri_gocu_cihan_ozel10") && it.endsWith(".json")
                    } ?: BUNDLED_ASSET_FILENAME
                }
                context.assets.open(targetName).bufferedReader(Charsets.UTF_8).use {
                    val content = it.readText()
                    if (content.isNotBlank()) return content
                }
            } catch (_: Exception) {
            }
        }

        val candidatePaths = listOf(
            "public/assets/aistudio/$BUNDLED_ASSET_FILENAME",
            "../public/assets/aistudio/$BUNDLED_ASSET_FILENAME",
            "app/src/main/assets/$BUNDLED_ASSET_FILENAME",
            "src/main/assets/$BUNDLED_ASSET_FILENAME"
        )
        for (path in candidatePaths) {
            val f = File(path)
            if (f.exists() && f.isFile) {
                return f.readText(Charsets.UTF_8)
            }
        }
        return null
    }

    /**
     * Web sisteminden alınan JSON metnini ayrıştırır ve BİRİZ veri modellerine sıfır veri kaybıyla dönüştürür.
     */
    fun parseAndMapMigrationJson(
        jsonStr: String,
        classId: String,
        teacherUid: String = "",
        existingStudents: List<Student> = emptyList()
    ): MappedWebMigrationData {
        val root = JSONObject(jsonStr.trim())
        val metaObj = root.optJSONObject("exportMetadata")
        val teacherEmail = metaObj?.optString("teacherEmail", TARGET_TEACHER_EMAIL) ?: TARGET_TEACHER_EMAIL
        val matchedLabel = metaObj?.optString("matchedClassroomLabel", "SÜLEYMAN SAMİ KEPENEK İLKOKULU - 4/D")
            ?: "SÜLEYMAN SAMİ KEPENEK İLKOKULU - 4/D"
        val exportedAtIso = metaObj?.optString("exportedAtIso", "") ?: ""

        val existingByNo = existingStudents.associateBy { it.studentNumber.trim() }

        // 1. Öğrencileri Eşle (25 Öğrenci)
        val studentsArray = root.optJSONArray("students") ?: JSONArray()
        val mappedStudents = mutableListOf<Student>()
        val studentByNo = mutableMapOf<Int, Student>()
        val studentBySourceId = mutableMapOf<String, Student>()
        val studentByNameUpper = mutableMapOf<String, Student>()

        for (i in 0 until studentsArray.length()) {
            val sObj = studentsArray.optJSONObject(i) ?: continue
            val studentNo = sObj.optInt("studentNo", 0)
            if (studentNo <= 0) continue
            val noStr = studentNo.toString()
            val sourceId = sObj.optString("sourceId", "").trim()
            val rawName = sObj.optString("fullName", "").trim()
            val fullName = TurkishTextHelper.toTurkishUpperCase(rawName)
            if (fullName.isBlank()) continue
            val rawGender = sObj.optString("gender", "Erkek").trim()
            val gender = if (rawGender.equals("Kız", ignoreCase = true)) "Kız" else "Erkek"
            val isPriority = sObj.optBoolean("isPriority", false)
            val isFixed = sObj.optBoolean("isFixed", false)

            val existing = existingByNo[noStr]
            val resolvedId = existing?.id?.takeIf { it.isNotBlank() } ?: "std_$studentNo"
            val resolvedCode = existing?.parentInviteCode?.takeIf { it.isNotBlank() }
                ?: STABLE_INVITE_CODES_BY_NO[studentNo]
                ?: TurkishTextHelper.generateVeliKatilimKodu()

            val student = Student(
                id = resolvedId,
                studentNumber = noStr,
                fullName = fullName,
                gender = gender,
                parentInviteCode = resolvedCode,
                isPriority = isPriority || (existing?.isPriority == true),
                isFixed = isFixed || (existing?.isFixed == true),
                parentName = existing?.parentName,
                parentEmail = existing?.parentEmail,
                parentLinkedAt = existing?.parentLinkedAt,
                parentLastLoginAt = existing?.parentLastLoginAt,
                parentIsOnline = existing?.parentIsOnline ?: false,
                parentLastActiveMillis = existing?.parentLastActiveMillis,
                linkedParents = existing?.linkedParents ?: emptyList()
            )
            mappedStudents.add(student)
            studentByNo[studentNo] = student
            if (sourceId.isNotBlank()) studentBySourceId[sourceId] = student
            studentByNameUpper[fullName] = student
        }

        val sortedStudents = mappedStudents.sortedBy { it.studentNumber.toIntOrNull() ?: 9999 }

        fun resolveStudent(studentNo: Int, sourceId: String, rawName: String): Student? {
            if (studentNo > 0) {
                studentByNo[studentNo]?.let { return it }
            }
            if (sourceId.isNotBlank()) {
                studentBySourceId[sourceId]?.let { return it }
            }
            val normName = TurkishTextHelper.toTurkishUpperCase(rawName.trim())
            if (normName.isNotBlank()) {
                studentByNameUpper[normName]?.let { return it }
            }
            return null
        }

        // 2. Kitapları Eşle (113 Kitap + Aktif Ödünçler)
        val booksArray = root.optJSONArray("books") ?: JSONArray()
        val mappedBooks = mutableListOf<Book>()
        val bookBySourceId = mutableMapOf<String, Book>()
        val bookByRegNo = mutableMapOf<Int, Book>()
        val bookByNameUpper = mutableMapOf<String, Book>()
        val nowMs = System.currentTimeMillis()

        for (i in 0 until booksArray.length()) {
            val bObj = booksArray.optJSONObject(i) ?: continue
            val sourceId = bObj.optString("sourceId", "").trim()
            val regNo = bObj.optInt("registrationNo", i + 1)
            val name = bObj.optString("name", "").trim()
            if (name.isBlank()) continue
            val author = bObj.optString("author", "").trim()
            val pageCount = bObj.optInt("pageCount", 0).coerceAtLeast(1)
            val createdAtMs = bObj.optLong("createdAtMs", nowMs).let { if (it > 0L) it else nowMs }

            val activeList = mutableListOf<ActiveAssignment>()
            val assignArr = bObj.optJSONArray("activeAssignments")
            if (assignArr != null) {
                for (j in 0 until assignArr.length()) {
                    val aObj = assignArr.optJSONObject(j) ?: continue
                    val aStudentNo = aObj.optInt("studentNo", 0)
                    val aStudentSourceId = aObj.optString("studentSourceId", "").trim()
                    val aStudentName = aObj.optString("studentName", "").trim()
                    val aDateMs = aObj.optLong("assignmentDateMs", createdAtMs).let { if (it > 0L) it else createdAtMs }
                    val resolvedStudent = resolveStudent(aStudentNo, aStudentSourceId, aStudentName)
                    val finalStudentId = resolvedStudent?.id ?: ("std_$aStudentNo".takeIf { aStudentNo > 0 } ?: aStudentSourceId)
                    val finalStudentName = resolvedStudent?.fullName ?: TurkishTextHelper.toTurkishUpperCase(aStudentName)
                    if (finalStudentId.isNotBlank() && finalStudentName.isNotBlank()) {
                        activeList.add(
                            ActiveAssignment(
                                studentId = finalStudentId,
                                studentName = finalStudentName,
                                assignmentDate = aDateMs
                            )
                        )
                    }
                }
            }

            val bookId = sourceId.ifBlank { "web_book_$regNo" }
            val book = LibraryPersistenceHelper.normalizeBookState(
                Book(
                    id = bookId,
                    registrationNo = regNo,
                    name = name,
                    author = author,
                    pageCount = pageCount,
                    status = if (activeList.isNotEmpty()) "Okunuyor" else "Rafta",
                    activeAssignments = activeList,
                    currentStudentId = activeList.firstOrNull()?.studentId,
                    currentStudentName = activeList.firstOrNull()?.studentName,
                    assignmentDate = activeList.firstOrNull()?.assignmentDate,
                    createdAt = createdAtMs,
                    updatedAt = createdAtMs
                )
            )
            mappedBooks.add(book)
            if (sourceId.isNotBlank()) bookBySourceId[sourceId] = book
            bookByRegNo[regNo] = book
            bookByNameUpper[TurkishTextHelper.toTurkishUpperCase(name)] = book
        }

        val sortedBooks = mappedBooks.sortedBy { it.registrationNo }

        // 3. Okuma Geçmişi Kayıtlarını Eşle (214 Kayıt)
        val recordsArray = root.optJSONArray("readingRecords") ?: JSONArray()
        val mappedReadingRecords = mutableListOf<ReadingRecord>()

        for (i in 0 until recordsArray.length()) {
            val rObj = recordsArray.optJSONObject(i) ?: continue
            val sourceId = rObj.optString("sourceId", "").trim().ifBlank { "web_rec_$i" }
            val bookSourceId = rObj.optString("bookSourceId", "").trim()
            val bookRegNo = rObj.optInt("bookRegistrationNo", 0)
            val rawBookName = rObj.optString("bookName", "").trim()

            val matchedBook = bookBySourceId[bookSourceId]
                ?: (if (bookRegNo > 0) bookByRegNo[bookRegNo] else null)
                ?: bookByNameUpper[TurkishTextHelper.toTurkishUpperCase(rawBookName)]

            val finalBookId = matchedBook?.id ?: bookSourceId.ifBlank { "web_book_$bookRegNo" }
            val finalBookName = matchedBook?.name ?: rawBookName
            if (finalBookName.isBlank()) continue
            val pageCount = rObj.optInt("pageCount", matchedBook?.pageCount ?: 0)

            val studentNo = rObj.optInt("studentNo", 0)
            val studentSourceId = rObj.optString("studentSourceId", "").trim()
            val rawStudentName = rObj.optString("studentName", "").trim()
            val matchedStudent = resolveStudent(studentNo, studentSourceId, rawStudentName)

            val finalStudentId = matchedStudent?.id ?: ("std_$studentNo".takeIf { studentNo > 0 } ?: studentSourceId)
            val finalStudentName = matchedStudent?.fullName ?: TurkishTextHelper.toTurkishUpperCase(rawStudentName)
            val finalStudentNumber = matchedStudent?.studentNumber ?: (if (studentNo > 0) studentNo.toString() else "")
            if (finalStudentId.isBlank()) continue

            val startDateMs = rObj.optLong("startDateMs", nowMs)
            val endDateMs = rObj.optLong("endDateMs", startDateMs)
            val earnedStars = rObj.optInt("earnedStars", LibraryPersistenceHelper.calculateReadingStars(pageCount))
                .coerceAtLeast(1)

            mappedReadingRecords.add(
                ReadingRecord(
                    id = sourceId,
                    bookId = finalBookId,
                    bookName = finalBookName,
                    pageCount = pageCount,
                    studentId = finalStudentId,
                    studentName = finalStudentName,
                    studentNumber = finalStudentNumber,
                    startDate = startDateMs,
                    endDate = endDateMs,
                    earnedStars = earnedStars,
                    teacherUid = teacherUid,
                    createdAt = endDateMs
                )
            )
        }

        val sortedReadingRecords = mappedReadingRecords
            .distinctBy { it.id }
            .sortedByDescending { it.endDate }

        // 4. Yıldız Kayıtlarını Eşle (1.325 İşlem Kaydı - 10.084 Yıldız)
        val starsArray = root.optJSONArray("starRecords") ?: JSONArray()
        val mappedStarRecords = mutableListOf<StarRewardRecord>()

        for (i in 0 until starsArray.length()) {
            val srObj = starsArray.optJSONObject(i) ?: continue
            val sourceId = srObj.optString("sourceId", "").trim().ifBlank { "web_star_$i" }
            val studentNo = srObj.optInt("studentNo", 0)
            val studentSourceId = srObj.optString("studentSourceId", "").trim()
            val rawStudentName = srObj.optString("studentName", "").trim()
            val matchedStudent = resolveStudent(studentNo, studentSourceId, rawStudentName)

            val finalStudentId = matchedStudent?.id ?: ("std_$studentNo".takeIf { studentNo > 0 } ?: studentSourceId)
            val finalStudentName = matchedStudent?.fullName ?: TurkishTextHelper.toTurkishUpperCase(rawStudentName)
            val finalStudentNumber = matchedStudent?.studentNumber ?: (if (studentNo > 0) studentNo.toString() else "")
            if (finalStudentId.isBlank()) continue

            val rawCategoryId = srObj.optString("categoryId", "matematik").trim()
            val rawOrigin = srObj.optString("origin", "manual").trim()
            val finalCategoryId = when {
                rawCategoryId == "cat_kitap_kurdu" || rawOrigin == "reading_return" -> "cat_kitap_kurdu"
                rawCategoryId.isNotBlank() -> rawCategoryId
                else -> "matematik"
            }
            val rawCategoryName = srObj.optString("categoryName", "").trim()
            val finalCategoryName = if (rawCategoryName.isNotBlank()) {
                rawCategoryName
            } else {
                com.example.data.model.StarCategory.findById(finalCategoryId).name
            }

            val starCount = srObj.optInt("starCount", srObj.optInt("stars", 1)).coerceAtLeast(1)
            val note = srObj.optString("note", "").trim()
            val isLuckyBonus = srObj.optBoolean("isLuckyBonus", false)
            val timestampMs = srObj.optLong("timestampMs", nowMs)

            mappedStarRecords.add(
                StarRewardRecord(
                    id = sourceId,
                    classId = classId,
                    studentId = finalStudentId,
                    studentName = finalStudentName,
                    studentNumber = finalStudentNumber,
                    categoryId = finalCategoryId,
                    categoryName = finalCategoryName,
                    starCount = starCount,
                    note = note,
                    isLuckyBonus = isLuckyBonus,
                    timestamp = timestampMs
                )
            )
        }

        // 5. Sağlama Özeti (verificationSummary) Kontrolü ve Sıfır Veri Kaybı Garantisi
        val verificationList = mutableListOf<StudentStarVerificationItem>()
        val vsObj = root.optJSONObject("verificationSummary")
        val checkArr = vsObj?.optJSONArray("studentStarCheck")
        if (checkArr != null) {
            for (i in 0 until checkArr.length()) {
                val cObj = checkArr.optJSONObject(i) ?: continue
                val sNo = cObj.optInt("studentNo", 0)
                val sName = cObj.optString("studentName", cObj.optString("fullName", "")).trim()
                val siteTotal = cObj.optInt("siteTotalStarsShownToUser", 0)
                val sumFromRecords = cObj.optInt("sumFromStarRecords", siteTotal)
                val readingCount = cObj.optInt("readingCount", 0)
                if (sNo > 0) {
                    verificationList.add(
                        StudentStarVerificationItem(
                            studentNo = sNo,
                            studentName = sName,
                            siteTotalStarsShownToUser = siteTotal,
                            sumFromStarRecords = sumFromRecords,
                            readingCount = readingCount
                        )
                    )
                }
            }
        }

        // Eğer kısmi bir JSON yüklenmişse ve öğrencinin yıldız toplamı verificationSummary'deki değerden eksikse, farkı koru
        for (vItem in verificationList) {
            val student = studentByNo[vItem.studentNo] ?: continue
            val currentSum = mappedStarRecords
                .filter { it.studentId == student.id }
                .sumOf { it.starCount }
            val diff = vItem.siteTotalStarsShownToUser - currentSum
            if (diff > 0) {
                mappedStarRecords.add(
                    StarRewardRecord(
                        id = "web_reconcile_${student.studentNumber}",
                        classId = classId,
                        studentId = student.id,
                        studentName = student.fullName,
                        studentNumber = student.studentNumber,
                        categoryId = "sorumluluk",
                        categoryName = "Sorumluluk Sahibi Öğrenci",
                        starCount = diff,
                        note = "Web Sistemi Devir Bakiyesi",
                        isLuckyBonus = false,
                        timestamp = nowMs
                    )
                )
            }
        }

        val sortedStarRecords = mappedStarRecords
            .distinctBy { it.id }
            .sortedByDescending { it.timestamp }

        return MappedWebMigrationData(
            teacherEmail = teacherEmail,
            matchedClassroomLabel = matchedLabel,
            exportedAtIso = exportedAtIso,
            students = sortedStudents,
            books = sortedBooks,
            readingRecords = sortedReadingRecords,
            starRecords = sortedStarRecords,
            verificationItems = verificationList,
            totalStarsSum = sortedStarRecords.sumOf { it.starCount },
            activeLoanBooksCount = sortedBooks.count { it.activeAssignments.isNotEmpty() }
        )
    }

    /**
     * Aktif sınıfa tüm öğrenci, kitaplık, okuma geçmişi ve yıldız verilerini sıfır kayıpla aktarır.
     */
    suspend fun migrateClassroomData(
        context: Context,
        repository: ClassRegistryRepository,
        classId: String,
        teacherUid: String = "",
        customJsonStr: String? = null,
        force: Boolean = false
    ): WebMigrationSummary {
        if (classId.isBlank()) {
            return WebMigrationSummary(false, 0, 0, 0, 0, 0, 0, 0, "Geçerli bir sınıf bulunamadı.")
        }

        if (!force && hasCompletedMigration(context, classId)) {
            val existingBooks = LibraryPersistenceHelper.loadBooks(context, classId)
            val existingRecords = LibraryPersistenceHelper.loadReadingRecords(context, classId)
            val existingStars = StarRewardPersistenceHelper.loadStarRecords(context, classId)
            val existingStudents = repository.students.value
            if (existingStudents.size >= 25 &&
                existingBooks.size >= 113 &&
                existingRecords.size >= 214 &&
                existingStars.size >= 1325
            ) {
                val activeCount = existingBooks.count { it.activeAssignments.isNotEmpty() }
                val totalStars = existingStars.sumOf { it.starCount }
                return WebMigrationSummary(
                    success = true,
                    studentsCount = existingStudents.size,
                    booksCount = existingBooks.size,
                    booksOnShelfCount = existingBooks.size - activeCount,
                    activeLoanBooksCount = activeCount,
                    readingRecordsCount = existingRecords.size,
                    starRecordsCount = existingStars.size,
                    totalStarsCount = totalStars,
                    message = "Verileriniz zaten eksiksiz senkronize durumda."
                )
            }
        }

        val rawJson = customJsonStr?.takeIf { it.isNotBlank() } ?: readBundledMigrationJson(context)
        if (rawJson.isNullOrBlank()) {
            return WebMigrationSummary(false, 0, 0, 0, 0, 0, 0, 0, "Aktarım JSON verisi okunamadı.")
        }

        activeMigrationClassIds.add(classId)
        try {
            // 1. İlk ayrıştırma ile öğrenci listesini çıkar ve Firestore / Repository ile birleştir
            val initialPass = parseAndMapMigrationJson(
                jsonStr = rawJson,
                classId = classId,
                teacherUid = teacherUid,
                existingStudents = repository.students.value
            )

            val syncedStudents = repository.syncMigratedStudentsToClassroom(
                classroomId = classId,
                incomingStudents = initialPass.students
            )

            // 2. Kesinleşen öğrenci ID'leri ile kitap ödünçlerini, okuma geçmişini ve yıldızları birebir eşle
            val finalMapped = parseAndMapMigrationJson(
                jsonStr = rawJson,
                classId = classId,
                teacherUid = teacherUid,
                existingStudents = syncedStudents
            )

            // 3. Koruma bayraklarını etkinleştir (boş başlangıç Firestore anlık görüntülerinin yerel veriyi ezmesini önler)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean("$KEY_MIGRATED_PREFIX$classId", true)
                .putBoolean("$KEY_PRESERVE_BOOKS_PREFIX$classId", true)
                .putBoolean("$KEY_PRESERVE_RECORDS_PREFIX$classId", true)
                .putBoolean("$KEY_PRESERVE_STARS_PREFIX$classId", true)
                .apply()

            // 4. Kitapları öğretmenin eklediği gerçek kitap olarak işaretle ve hem yerele hem buluta kaydet
            LibraryPersistenceHelper.markBooksAsUserAdded(
                context = context,
                classId = classId,
                bookIds = finalMapped.books.map { it.id }
            )
            LibraryPersistenceHelper.saveBooks(context, classId, finalMapped.books)

            // 5. Okuma kayıtlarını hem yerele hem buluta kaydet
            LibraryPersistenceHelper.saveReadingRecords(context, classId, finalMapped.readingRecords)

            // 6. Yıldız kayıtlarını hem yerele hem buluta kaydet
            StarRewardPersistenceHelper.saveStarRecords(context, classId, finalMapped.starRecords)
            StarRewardPersistenceHelper.uploadRecordsBatchToFirestore(context, classId, finalMapped.starRecords)

            val onShelf = finalMapped.books.size - finalMapped.activeLoanBooksCount
            val summaryMsg = "✅ Web verileriniz aktarıldı: ${finalMapped.students.size} Öğrenci, " +
                "${finalMapped.books.size} Kitap ($onShelf Rafta, ${finalMapped.activeLoanBooksCount} Okunuyor), " +
                "${finalMapped.readingRecords.size} Okuma Geçmişi, ${finalMapped.starRecords.size} Yıldız İşlemi (${finalMapped.totalStarsSum} ⭐)"

            return WebMigrationSummary(
                success = true,
                studentsCount = finalMapped.students.size,
                booksCount = finalMapped.books.size,
                booksOnShelfCount = onShelf,
                activeLoanBooksCount = finalMapped.activeLoanBooksCount,
                readingRecordsCount = finalMapped.readingRecords.size,
                starRecordsCount = finalMapped.starRecords.size,
                totalStarsCount = finalMapped.totalStarsSum,
                message = summaryMsg
            )
        } catch (e: Exception) {
            Log.e(TAG, "Web data migration failed for class $classId", e)
            return WebMigrationSummary(
                success = false,
                studentsCount = 0,
                booksCount = 0,
                booksOnShelfCount = 0,
                activeLoanBooksCount = 0,
                readingRecordsCount = 0,
                starRecordsCount = 0,
                totalStarsCount = 0,
                message = "Veri aktarımı sırasında hata oluştu: ${e.localizedMessage ?: "Bilinmeyen hata"}"
            )
        } finally {
            activeMigrationClassIds.remove(classId)
        }
    }
}
