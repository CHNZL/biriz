package com.example.data.repository

import android.content.Context
import com.example.data.model.StarRewardRecord
import org.json.JSONArray
import org.json.JSONObject

/**
 * Yıldızlar Sınıfı kalıcı veri saklama yardımcısı (SharedPreferences + JSON)
 */
object StarRewardPersistenceHelper {
    private const val PREFS_NAME = "biriz_star_rewards_prefs"
    private const val KEY_RECORDS_PREFIX = "star_records_"

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
        current.add(0, record)
        saveStarRecords(context, classId, current)
        return current
    }

    fun addStarRecords(context: Context, classId: String, newRecords: List<StarRewardRecord>): List<StarRewardRecord> {
        val current = loadStarRecords(context, classId).toMutableList()
        current.addAll(0, newRecords)
        saveStarRecords(context, classId, current)
        return current
    }

    fun deleteStarRecord(context: Context, classId: String, recordId: String): List<StarRewardRecord> {
        val current = loadStarRecords(context, classId).filter { it.id != recordId }
        saveStarRecords(context, classId, current)
        return current
    }

    fun deleteRecordsByIds(context: Context, classId: String, recordIds: Set<String>): List<StarRewardRecord> {
        val current = loadStarRecords(context, classId).filter { !recordIds.contains(it.id) }
        saveStarRecords(context, classId, current)
        return current
    }

    fun getStudentTotalStars(records: List<StarRewardRecord>, studentId: String): Int {
        return records.filter { it.studentId == studentId }.sumOf { it.starCount }
    }
}
