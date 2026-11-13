package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.StarRewardRecord
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Yıldızlar Sınıfı kalıcı veri saklama ve bulut senkronizasyon yöneticisi
 * - Yerel (SharedPreferences): Anında tepki, internetsiz çalışma, sıfır gecikme
 * - Bulut (Firebase Firestore): Cihazlar arası senkronizasyon, veli/öğrenci paylaşımı, kalıcı yedek
 */
object StarRewardPersistenceHelper {
    private const val TAG = "StarRewardPersistence"
    private const val PREFS_NAME = "biriz_star_rewards_prefs"
    private const val KEY_RECORDS_PREFIX = "star_records_"
    private val syncScope = CoroutineScope(Dispatchers.IO)

    private fun getFirestore(context: Context): FirebaseFirestore? {
        return try {
            com.example.util.FirestoreProvider.get(context)
        } catch (_: Exception) {
            null
        }
    }

    fun loadStarRecords(context: Context, classId: String): List<StarRewardRecord> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("$KEY_RECORDS_PREFIX$classId", null) ?: return emptyList()

        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<StarRewardRecord>()
            var hadCorrupt = false
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id", "").trim()
                val studentId = obj.optString("studentId", "").trim()
                val starCount = obj.optInt("starCount", 1)
                if (id.isBlank() || studentId.isBlank() || starCount <= 0) {
                    hadCorrupt = true
                    continue
                }
                list.add(
                    StarRewardRecord(
                        id = id,
                        classId = obj.optString("classId", classId),
                        studentId = studentId,
                        studentName = obj.optString("studentName", ""),
                        studentNumber = obj.optString("studentNumber", ""),
                        categoryId = obj.optString("categoryId", ""),
                        categoryName = obj.optString("categoryName", ""),
                        starCount = starCount,
                        note = obj.optString("note", ""),
                        isLuckyBonus = obj.optBoolean("isLuckyBonus", false),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            val cleaned = list.distinctBy { it.id }.sortedByDescending { it.timestamp }
            if (hadCorrupt || cleaned.size != list.size) {
                saveStarRecords(context, classId, cleaned)
            }
            cleaned
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun hasSyncedStarCache(context: Context, classId: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.contains("$KEY_RECORDS_PREFIX$classId")
    }

    fun saveStarRecords(context: Context, classId: String, records: List<StarRewardRecord>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        records.forEach { record ->
            val obj = JSONObject().apply {
                put("id", record.id)
                put("classId", record.classId)
                put("studentId", record.studentId)
                put("studentName", record.studentName)
                put("studentNumber", record.studentNumber)
                put("categoryId", record.categoryId)
                put("categoryName", record.categoryName)
                put("starCount", record.starCount)
                put("note", record.note)
                put("isLuckyBonus", record.isLuckyBonus)
                put("timestamp", record.timestamp)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString("$KEY_RECORDS_PREFIX$classId", jsonArray.toString()).apply()
    }

    fun addStarRecord(context: Context, classId: String, record: StarRewardRecord): List<StarRewardRecord> {
        val current = loadStarRecords(context, classId).toMutableList()
        current.removeAll { it.id == record.id }
        current.add(0, record)
        saveStarRecords(context, classId, current)

        // Asynchronously persist to Firebase Firestore
        syncScope.launch {
            uploadRecordToFirestore(context, classId, record)
        }
        return current
    }

    fun addStarRecords(context: Context, classId: String, newRecords: List<StarRewardRecord>): List<StarRewardRecord> {
        val current = loadStarRecords(context, classId).toMutableList()
        val newIds = newRecords.map { it.id }.toSet()
        current.removeAll { newIds.contains(it.id) }
        current.addAll(0, newRecords)
        saveStarRecords(context, classId, current)

        // Asynchronously persist batch to Firebase Firestore
        syncScope.launch {
            uploadRecordsBatchToFirestore(context, classId, newRecords)
        }
        return current
    }

    fun deleteStarRecord(context: Context, classId: String, recordId: String): List<StarRewardRecord> {
        val current = loadStarRecords(context, classId).filter { it.id != recordId }
        saveStarRecords(context, classId, current)

        syncScope.launch {
            deleteRecordFromFirestore(context, classId, recordId)
        }
        return current
    }

    fun deleteRecordsByIds(context: Context, classId: String, recordIds: Set<String>): List<StarRewardRecord> {
        if (recordIds.isEmpty()) return loadStarRecords(context, classId)
        val current = loadStarRecords(context, classId).filter { !recordIds.contains(it.id) }
        saveStarRecords(context, classId, current)

        syncScope.launch {
            deleteRecordIdsBatchFromFirestore(context, classId, recordIds)
        }
        return current
    }

    /**
     * Belirli bir kategoriye (örn. "cat_kitap_kurdu") ait tüm yıldız kayıtlarını hem yerelden hem Firestore'dan siler.
     */
    fun deleteRecordsByCategory(context: Context, classId: String, categoryId: String): Int {
        val current = loadStarRecords(context, classId)
        val toRemove = current.filter { it.categoryId == categoryId }
        val removedStars = toRemove.sumOf { it.starCount }
        val remaining = current.filter { it.categoryId != categoryId }
        if (remaining.size != current.size) {
            saveStarRecords(context, classId, remaining)
        }

        syncScope.launch {
            if (classId.isBlank() || classId == "default") return@launch
            val db = getFirestore(context) ?: return@launch
            val collRef = db.collection("classrooms").document(classId).collection("stars")
            collRef.whereEqualTo("categoryId", categoryId).get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot != null && !snapshot.isEmpty) {
                        snapshot.documents.chunked(400).forEach { chunk ->
                            val batch = db.batch()
                            chunk.forEach { doc -> batch.delete(doc.reference) }
                            batch.commit().addOnFailureListener { e ->
                                handleFirestoreError(e, OperationType.DELETE, collRef.path)
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    handleFirestoreError(e, OperationType.LIST, collRef.path)
                }
        }
        return removedStars
    }

    fun clearAllRecords(context: Context, classId: String): List<StarRewardRecord> {
        saveStarRecords(context, classId, emptyList())

        syncScope.launch {
            if (classId.isBlank() || classId == "default") return@launch
            val db = getFirestore(context) ?: return@launch
            val collRef = db.collection("classrooms").document(classId).collection("stars")
            collRef.get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot != null && !snapshot.isEmpty) {
                        snapshot.documents.chunked(400).forEach { chunk ->
                            val batch = db.batch()
                            chunk.forEach { doc ->
                                batch.delete(doc.reference)
                            }
                            batch.commit().addOnFailureListener { e ->
                                handleFirestoreError(e, OperationType.DELETE, collRef.path)
                            }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    handleFirestoreError(e, OperationType.LIST, collRef.path)
                }
        }
        return emptyList()
    }

    /**
     * Sınıftan ayrılmış öğrencilere, bozuk/eksik alanlara veya artık var olmayan okuma/turnuva kayıtlarına ait
     * yetim/hatalı yıldızları yerelden ve Firestore'dan otomatik temizler.
     */
    fun cleanupStaleAndOrphanedStarRecords(
        context: Context,
        classId: String,
        validStudentIds: Set<String> = emptySet(),
        validTournamentNames: Set<String>? = null,
        readingRecordsSynced: Boolean = false,
        validReadingRecords: List<com.example.data.model.ReadingRecord> = emptyList()
    ): Int {
        if (classId.isBlank() || classId == "default") return 0
        val current = loadStarRecords(context, classId)
        if (current.isEmpty()) return 0

        val idsToRemove = mutableSetOf<String>()
        current.forEach { sr ->
            val isCorrupt = sr.id.isBlank() || sr.studentId.isBlank() || sr.starCount <= 0
            val isInvalidStudent = validStudentIds.isNotEmpty() && sr.studentId !in validStudentIds

            val isOrphanTournamentStar = if (validTournamentNames != null &&
                (sr.note.startsWith("Turnuva Ödülü: [") || sr.note.startsWith("Turnuva Katılım Ödülü: ["))
            ) {
                val startIdx = sr.note.indexOf('[')
                val endIdx = sr.note.indexOf(']', startIdx + 1)
                if (startIdx != -1 && endIdx > startIdx) {
                    val tName = sr.note.substring(startIdx + 1, endIdx)
                    tName !in validTournamentNames
                } else {
                    validTournamentNames.isEmpty()
                }
            } else false

            val isOrphanReadingStar = if (readingRecordsSynced && sr.categoryId == "cat_kitap_kurdu") {
                validReadingRecords.none { rec ->
                    rec.studentId == sr.studentId &&
                        (sr.note.contains(rec.bookName, ignoreCase = true) || kotlin.math.abs(sr.timestamp - rec.endDate) < 60000L)
                }
            } else false

            if (isCorrupt || isInvalidStudent || isOrphanTournamentStar || isOrphanReadingStar) {
                idsToRemove.add(sr.id)
            }
        }

        if (idsToRemove.isNotEmpty()) {
            deleteRecordsByIds(context, classId, idsToRemove)
        }
        return idsToRemove.size
    }

    fun getStudentTotalStars(records: List<StarRewardRecord>, studentId: String): Int {
        return records.filter { it.studentId == studentId }.sumOf { it.starCount }
    }

    /**
     * Firebase Firestore gerçek zamanlı senkronizasyon dinleyicisi (Firestore SSOT)
     */
    fun observeFirestoreStars(
        context: Context,
        classId: String,
        onUpdate: (List<StarRewardRecord>) -> Unit
    ): ListenerRegistration? {
        if (classId.isBlank() || classId == "default") return null
        val db = getFirestore(context) ?: return null

        val path = "classrooms/$classId/stars"
        return try {
            db.collection("classrooms").document(classId).collection("stars")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        handleFirestoreError(error, OperationType.LIST, path)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val invalidDocIds = mutableSetOf<String>()
                        val cloudRecords = snapshot.documents.mapNotNull { doc ->
                            val sId = doc.getString("studentId")?.trim()
                            val sName = doc.getString("studentName") ?: ""
                            val sNum = doc.getString("studentNumber") ?: ""
                            val catId = doc.getString("categoryId") ?: ""
                            val catName = doc.getString("categoryName") ?: ""
                            val count = (doc.getLong("starCount") ?: 0L).toInt()
                            val note = doc.getString("note") ?: ""
                            val isLucky = doc.getBoolean("isLuckyBonus") ?: false
                            val ts = doc.getLong("timestamp") ?: System.currentTimeMillis()

                            if (sId.isNullOrBlank() || count <= 0) {
                                invalidDocIds.add(doc.id)
                                return@mapNotNull null
                            }

                            StarRewardRecord(
                                id = doc.id,
                                classId = classId,
                                studentId = sId,
                                studentName = sName,
                                studentNumber = sNum,
                                categoryId = catId,
                                categoryName = catName,
                                starCount = count,
                                note = note,
                                isLuckyBonus = isLucky,
                                timestamp = ts
                            )
                        }.sortedByDescending { it.timestamp }

                        if (invalidDocIds.isNotEmpty()) {
                            syncScope.launch {
                                deleteRecordIdsBatchFromFirestore(context, classId, invalidDocIds)
                            }
                        }

                        // Firestore SSOT: Boş olsa bile yerel önbelleği Firestore ile birebir eşitle (sıfırlanan yıldızları geri yükleme!)
                        saveStarRecords(context, classId, cloudRecords)
                        onUpdate(cloudRecords)
                    }
                }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            null
        }
    }

    private fun uploadRecordToFirestore(context: Context, classId: String, record: StarRewardRecord) {
        if (classId.isBlank() || classId == "default") return
        val db = getFirestore(context) ?: return
        val docRef = db.collection("classrooms").document(classId).collection("stars").document(record.id)
        val payload = mapOf(
            "classId" to classId,
            "studentId" to record.studentId,
            "studentName" to record.studentName,
            "studentNumber" to record.studentNumber,
            "categoryId" to record.categoryId,
            "categoryName" to record.categoryName,
            "starCount" to record.starCount,
            "note" to record.note,
            "isLuckyBonus" to record.isLuckyBonus,
            "timestamp" to record.timestamp,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload)
            .addOnFailureListener { e ->
                handleFirestoreError(e, OperationType.CREATE, docRef.path)
            }
    }

    private fun uploadRecordsBatchToFirestore(context: Context, classId: String, records: List<StarRewardRecord>) {
        if (classId.isBlank() || classId == "default" || records.isEmpty()) return
        val db = getFirestore(context) ?: return
        records.chunked(400).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { record ->
                val docRef = db.collection("classrooms").document(classId).collection("stars").document(record.id)
                val payload = mapOf(
                    "classId" to classId,
                    "studentId" to record.studentId,
                    "studentName" to record.studentName,
                    "studentNumber" to record.studentNumber,
                    "categoryId" to record.categoryId,
                    "categoryName" to record.categoryName,
                    "starCount" to record.starCount,
                    "note" to record.note,
                    "isLuckyBonus" to record.isLuckyBonus,
                    "timestamp" to record.timestamp,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(docRef, payload)
            }
            batch.commit()
                .addOnFailureListener { e ->
                    handleFirestoreError(e, OperationType.WRITE, "classrooms/$classId/stars/batch")
                }
        }
    }

    private fun deleteRecordFromFirestore(context: Context, classId: String, recordId: String) {
        if (classId.isBlank() || classId == "default" || recordId.isBlank()) return
        val db = getFirestore(context) ?: return
        val docRef = db.collection("classrooms").document(classId).collection("stars").document(recordId)
        docRef.delete()
            .addOnFailureListener { e ->
                handleFirestoreError(e, OperationType.DELETE, docRef.path)
            }
    }

    private fun deleteRecordIdsBatchFromFirestore(context: Context, classId: String, recordIds: Set<String>) {
        if (classId.isBlank() || classId == "default" || recordIds.isEmpty()) return
        val db = getFirestore(context) ?: return
        val collRef = db.collection("classrooms").document(classId).collection("stars")
        recordIds.filter { it.isNotBlank() }.chunked(400).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { id ->
                batch.delete(collRef.document(id))
            }
            batch.commit()
                .addOnFailureListener { e ->
                    handleFirestoreError(e, OperationType.DELETE, collRef.path)
                }
        }
    }
}
