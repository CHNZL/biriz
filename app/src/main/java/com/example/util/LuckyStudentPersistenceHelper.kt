package com.example.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Şanslı Öğrenci Modülü Kalıcı Durum Yardımcısı
 *
 * Öğretmenin seçtiği şanslı öğrencileri, aday havuzundan çıkarılanları,
 * çekiliş modunu ve geçmiş sırasını SharedPreferences üzerinde kalıcı olarak saklar.
 * Böylece sayfa geçişlerinde, sekme değişiminde veya uygulama yeniden açıldığında
 * tüm seçimler ve havuz durumu korunur.
 */
object LuckyStudentPersistenceHelper {
    private const val PREFS_NAME = "biriz_lucky_student_storage"
    private const val KEY_PREFIX = "lucky_state_"

    data class SavedLuckyState(
        val drawnStudentIds: List<String>,
        val excludedStudentIds: Set<String>,
        val selectedModeName: String
    )

    fun saveState(
        context: Context,
        classId: String,
        drawnStudentIds: List<String>,
        excludedStudentIds: Set<String>,
        selectedModeName: String
    ) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = JSONObject().apply {
                val drawnArray = JSONArray()
                drawnStudentIds.forEach { drawnArray.put(it) }
                put("drawn", drawnArray)

                val excludedArray = JSONArray()
                excludedStudentIds.forEach { excludedArray.put(it) }
                put("excluded", excludedArray)

                put("mode", selectedModeName)
            }
            val key = if (classId.isNotBlank()) KEY_PREFIX + classId else KEY_PREFIX + "default"
            prefs.edit().putString(key, json.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadState(context: Context, classId: String): SavedLuckyState? {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val key = if (classId.isNotBlank()) KEY_PREFIX + classId else KEY_PREFIX + "default"
            val jsonStr = prefs.getString(key, null) ?: return null
            val json = JSONObject(jsonStr)

            val drawn = mutableListOf<String>()
            val drawnArray = json.optJSONArray("drawn")
            if (drawnArray != null) {
                for (i in 0 until drawnArray.length()) {
                    drawn.add(drawnArray.getString(i))
                }
            }

            val excluded = mutableSetOf<String>()
            val excludedArray = json.optJSONArray("excluded")
            if (excludedArray != null) {
                for (i in 0 until excludedArray.length()) {
                    excluded.add(excludedArray.getString(i))
                }
            }

            val mode = json.optString("mode", "ROULETTE")
            return SavedLuckyState(drawn, excluded, mode)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun clearState(context: Context, classId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = if (classId.isNotBlank()) KEY_PREFIX + classId else KEY_PREFIX + "default"
        prefs.edit().remove(key).apply()
    }
}
