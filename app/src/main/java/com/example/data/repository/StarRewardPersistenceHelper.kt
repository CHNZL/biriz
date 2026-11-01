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
            val dbId = context.getString(R.string.firestore_database_id)
            FirebaseFirestore.getInstance(dbId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get Firestore instance with custom database ID", e)
            null
        }
    }

    fun loadStarRecords(context: Context, classId: String): List<StarRewardRecord> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("$KEY_RECORDS_PREFIX$classId", null) ?: return emptyList()

        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<StarRewardRecord>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    StarRewardRecord(
                        id = obj.optString("id", ""),
                        classId = obj.optString("classId", classId),
                        studentId = obj.optString("studentId", ""),
                        studentName = obj.optString("studentName", ""),
                        studentNumber = obj.optString("studentNumber", ""),
                        categoryId = obj.optString("categoryId", ""),
                        categoryName = obj.optString("categoryName", ""),
                        starCount = obj.optInt("starCount", 1),
                        note = obj.optString("note", ""),
                        isLuckyBonus = obj.optBoolean("isLuckyBonus", false),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
            list.sortedByDescending { it.timestamp }
        } catch (_: Exception) {
            emptyList()
        }
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
        val current = loadStarRecords(context, classId).filter { !recordIds.contains(it.id) }
        saveStarRecords(context, classId, current)

        syncScope.launch {
            recordIds.forEach { recordId ->
                deleteRecordFromFirestore(context, classId, recordId)
            }
        }
        return current
    }

    fun getStudentTotalStars(records: List<StarRewardRecord>, studentId: String): Int {
        return records.filter { it.studentId == studentId }.sumOf { it.starCount }
    }

    /**
     * Firebase Firestore gerçek zamanlı senkronizasyon dinleyicisi
     */
    fun observeFirestoreStars(
        context: Context,
        classId: String,
        onUpdate: (List<StarRewardRecord>) -> Unit
    ): ListenerRegistration? {
        if (Firebase.auth.currentUser == null) return null
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
                        val cloudRecords = snapshot.documents.mapNotNull { doc ->
                            val sId = doc.getString("studentId") ?: return@mapNotNull null
                            val sName = doc.getString("studentName") ?: ""
                            val sNum = doc.getString("studentNumber") ?: ""
                            val catId = doc.getString("categoryId") ?: ""
                            val catName = doc.getString("categoryName") ?: ""
                            val count = (doc.getLong("starCount") ?: 1L).toInt()
                            val note = doc.getString("note") ?: ""
                            val isLucky = doc.getBoolean("isLuckyBonus") ?: false
                            val ts = doc.getLong("timestamp") ?: System.currentTimeMillis()

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

                        if (cloudRecords.isNotEmpty()) {
                            saveStarRecords(context, classId, cloudRecords)
                            onUpdate(cloudRecords)
                        }
                    }
                }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            null
        }
    }

    private fun uploadRecordToFirestore(context: Context, classId: String, record: StarRewardRecord) {
        if (Firebase.auth.currentUser == null) return
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
        if (Firebase.auth.currentUser == null || records.isEmpty()) return
        val db = getFirestore(context) ?: return
        val batch = db.batch()
        records.forEach { record ->
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

    private fun deleteRecordFromFirestore(context: Context, classId: String, recordId: String) {
        if (Firebase.auth.currentUser == null) return
        val db = getFirestore(context) ?: return
        val docRef = db.collection("classrooms").document(classId).collection("stars").document(recordId)
        docRef.delete()
            .addOnFailureListener { e ->
                handleFirestoreError(e, OperationType.DELETE, docRef.path)
            }
    }
}
