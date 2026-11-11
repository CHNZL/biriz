package com.example.util

import android.content.Context
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Şanslı Öğrenci Modülü Kalıcı Durum ve Bulut Senkronizasyon Yardımcısı
 *
 * Öğretmenin seçtiği şanslı öğrencileri, aday havuzundan çıkarılanları,
 * çekiliş modunu ve geçmiş sırasını hem yerel önbellekte hem de Firebase Firestore üzerinde canlı olarak saklar.
 */
object LuckyStudentPersistenceHelper {
    private const val PREFS_NAME = "biriz_lucky_student_storage"
    private const val KEY_PREFIX = "lucky_state_"
    private val syncScope = CoroutineScope(Dispatchers.IO)

    private fun getFirestore(context: Context): FirebaseFirestore? {
        return try {
            FirestoreProvider.get(context)
        } catch (_: Exception) {
            null
        }
    }

    data class SavedLuckyState(
        val drawnStudentIds: List<String>,
        val excludedStudentIds: Set<String>,
        val selectedModeName: String
    )

    private fun deserializeJson(jsonStr: String): SavedLuckyState {
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
    }

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
            val jsonStr = json.toString()
            val editor = prefs.edit()
            if (classId.isNotBlank()) {
                editor.putString(KEY_PREFIX + classId, jsonStr)
            }
            editor.putString(KEY_PREFIX + "default", jsonStr)
            editor.apply()

            if (classId.isNotBlank() && classId != "default") {
                syncScope.launch {
                    try {
                        val db = getFirestore(context) ?: return@launch
                        db.collection("classrooms").document(classId)
                            .collection("lucky_student").document("current")
                            .set(
                                mapOf(
                                    "classId" to classId,
                                    "luckyJson" to jsonStr,
                                    "updatedAt" to FieldValue.serverTimestamp()
                                ),
                                SetOptions.merge()
                            )
                    } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadState(context: Context, classId: String): SavedLuckyState? {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonStr = (if (classId.isNotBlank()) prefs.getString(KEY_PREFIX + classId, null) else null)
                ?: prefs.getString(KEY_PREFIX + "default", null)
                ?: return null
            return deserializeJson(jsonStr)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun clearState(context: Context, classId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        if (classId.isNotBlank()) {
            editor.remove(KEY_PREFIX + classId)
        }
        editor.remove(KEY_PREFIX + "default").apply()

        if (classId.isNotBlank() && classId != "default") {
            syncScope.launch {
                try {
                    val db = getFirestore(context) ?: return@launch
                    db.collection("classrooms").document(classId)
                        .collection("lucky_student").document("current")
                        .delete()
                } catch (_: Exception) {}
            }
        }
    }

    fun observeFirestoreLuckyState(
        context: Context,
        classId: String,
        onUpdate: (SavedLuckyState) -> Unit
    ): ListenerRegistration? {
        if (classId.isBlank() || classId == "default") return null
        val db = getFirestore(context) ?: return null
        return try {
            db.collection("classrooms").document(classId)
                .collection("lucky_student").document("current")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    if (!snapshot.exists()) {
                        val local = loadState(context, classId)
                        if (local != null && (local.drawnStudentIds.isNotEmpty() || local.excludedStudentIds.isNotEmpty())) {
                            saveState(context, classId, local.drawnStudentIds, local.excludedStudentIds, local.selectedModeName)
                        }
                        return@addSnapshotListener
                    }
                    try {
                        val luckyJson = snapshot.getString("luckyJson")
                        if (!luckyJson.isNullOrBlank()) {
                            val parsed = deserializeJson(luckyJson)
                            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            prefs.edit()
                                .putString(KEY_PREFIX + classId, luckyJson)
                                .putString(KEY_PREFIX + "default", luckyJson)
                                .apply()
                            onUpdate(parsed)
                        }
                    } catch (_: Exception) {}
                }
        } catch (_: Exception) {
            null
        }
    }
}
