package com.example.util

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.ScheduleConfig
import com.example.data.model.ScheduleLesson
import com.google.firebase.FirebaseApp
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
 * Esnek Ders Programı Ayarları ve Haftalık Ders Listesi için
 * Hibrit Depolama: Yerel (SharedPreferences) + Bulut Canlı Senkronizasyon (Firebase Firestore).
 *
 * Öğretmen herhangi bir dersi, saati veya teneffüsü değiştirdiğinde;
 * hem cihaza yerel kaydedilir hem de Firestore üzerinden veli portalına ve diğer cihazlara
 * ANINDA gerçek zamanlı yansıtılır.
 */
object SchedulePersistenceHelper {

    private const val TAG = "SchedulePersistence"
    private const val PREFS_NAME = "biriz_schedule_storage"
    private const val KEY_CONFIG_PREFIX = "config_"
    private const val KEY_LESSONS_PREFIX = "lessons_"
    private const val KEY_ACTIVE_DAY_PREFIX = "active_day_"
    private val syncScope = CoroutineScope(Dispatchers.IO)

    private fun getFirestore(context: Context): FirebaseFirestore? {
        return try {
            FirestoreProvider.get(context)
        } catch (_: Exception) {
            null
        }
    }

    fun saveScheduleConfig(context: Context, classId: String, config: ScheduleConfig) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = JSONObject().apply {
                put("dailyLessonCount", config.dailyLessonCount)
                put("firstLessonStartTime", config.firstLessonStartTime)
                put("lessonDurationMinutes", config.lessonDurationMinutes)
                val recessArray = JSONArray()
                config.recessDurations.forEach { recessArray.put(it) }
                put("recessDurations", recessArray)
                put("isLunchBreakActive", config.isLunchBreakActive)
                put("lunchAfterLessonIndex", config.lunchAfterLessonIndex)
                put("lunchDurationMinutes", config.lunchDurationMinutes)
                put("isConfigured", true)
            }
            val jsonStr = json.toString()
            val editor = prefs.edit()
            if (classId.isNotBlank()) {
                editor.putString(KEY_CONFIG_PREFIX + classId, jsonStr)
            }
            editor.putString(KEY_CONFIG_PREFIX + "default", jsonStr)
            editor.apply()

            // Buluta canlı yükle
            uploadScheduleToFirestore(context, classId, config, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadScheduleConfig(context: Context, classId: String): ScheduleConfig? {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonStr = (if (classId.isNotBlank()) prefs.getString(KEY_CONFIG_PREFIX + classId, null) else null)
                ?: prefs.getString(KEY_CONFIG_PREFIX + "default", null)
                ?: return null

            val json = JSONObject(jsonStr)
            val recessList = mutableListOf<Int>()
            val recessArray = json.optJSONArray("recessDurations")
            if (recessArray != null) {
                for (i in 0 until recessArray.length()) {
                    recessList.add(recessArray.getInt(i))
                }
            } else {
                recessList.addAll(listOf(15, 15, 10, 15, 15))
            }

            return ScheduleConfig(
                dailyLessonCount = json.optInt("dailyLessonCount", 6),
                firstLessonStartTime = json.optString("firstLessonStartTime", "08:30"),
                lessonDurationMinutes = json.optInt("lessonDurationMinutes", 40),
                recessDurations = recessList,
                isLunchBreakActive = json.optBoolean("isLunchBreakActive", true),
                lunchAfterLessonIndex = json.optInt("lunchAfterLessonIndex", 4),
                lunchDurationMinutes = json.optInt("lunchDurationMinutes", 50),
                isConfigured = true
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun saveScheduleLessons(context: Context, classId: String, lessons: List<ScheduleLesson>) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val array = JSONArray()
            lessons.forEach { lesson ->
                val obj = JSONObject().apply {
                    put("dayOfWeek", lesson.dayOfWeek)
                    put("lessonIndex", lesson.lessonIndex)
                    put("subjectName", lesson.subjectName)
                    put("topicOrNotes", lesson.topicOrNotes)
                    put("teacherName", lesson.teacherName)
                    put("colorTheme", lesson.colorTheme)
                }
                array.put(obj)
            }
            val arrayStr = array.toString()
            val editor = prefs.edit()
            if (classId.isNotBlank()) {
                editor.putString(KEY_LESSONS_PREFIX + classId, arrayStr)
            }
            editor.putString(KEY_LESSONS_PREFIX + "default", arrayStr)
            editor.apply()

            // Buluta canlı yükle
            uploadScheduleToFirestore(context, classId, null, lessons)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadScheduleLessons(context: Context, classId: String): List<ScheduleLesson>? {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val jsonStr = (if (classId.isNotBlank()) prefs.getString(KEY_LESSONS_PREFIX + classId, null) else null)
                ?: prefs.getString(KEY_LESSONS_PREFIX + "default", null)
                ?: return null

            val array = JSONArray(jsonStr)
            val list = mutableListOf<ScheduleLesson>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ScheduleLesson(
                        dayOfWeek = obj.getString("dayOfWeek"),
                        lessonIndex = obj.getInt("lessonIndex"),
                        subjectName = obj.getString("subjectName"),
                        topicOrNotes = obj.optString("topicOrNotes", ""),
                        teacherName = obj.optString("teacherName", ""),
                        colorTheme = obj.optString("colorTheme", "NAVY")
                    )
                )
            }
            return if (list.isNotEmpty()) list else null
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun saveActiveDay(context: Context, classId: String, day: String) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            if (classId.isNotBlank()) {
                editor.putString(KEY_ACTIVE_DAY_PREFIX + classId, day)
            }
            editor.putString(KEY_ACTIVE_DAY_PREFIX + "default", day)
            editor.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadActiveDay(context: Context, classId: String): String? {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return (if (classId.isNotBlank()) prefs.getString(KEY_ACTIVE_DAY_PREFIX + classId, null) else null)
                ?: prefs.getString(KEY_ACTIVE_DAY_PREFIX + "default", null)
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Ders programı ayarlarını ve haftalık dersleri Firestore'a kaydeder.
     */
    fun uploadScheduleToFirestore(
        context: Context,
        classId: String,
        config: ScheduleConfig?,
        lessons: List<ScheduleLesson>?
    ) {
        if (classId.isBlank() || classId == "default") return
        val db = getFirestore(context) ?: return
        val effectiveConfig = config ?: loadScheduleConfig(context, classId)
        val effectiveLessons = lessons ?: loadScheduleLessons(context, classId)
        syncScope.launch {
            try {
                val docRef = db.collection("classrooms").document(classId).collection("schedule").document("current")
                val payload = mutableMapOf<String, Any>(
                    "classId" to classId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (effectiveConfig != null) {
                    val configJson = JSONObject().apply {
                        put("dailyLessonCount", effectiveConfig.dailyLessonCount)
                        put("firstLessonStartTime", effectiveConfig.firstLessonStartTime)
                        put("lessonDurationMinutes", effectiveConfig.lessonDurationMinutes)
                        val recessArray = JSONArray()
                        effectiveConfig.recessDurations.forEach { recessArray.put(it) }
                        put("recessDurations", recessArray)
                        put("isLunchBreakActive", effectiveConfig.isLunchBreakActive)
                        put("lunchAfterLessonIndex", effectiveConfig.lunchAfterLessonIndex)
                        put("lunchDurationMinutes", effectiveConfig.lunchDurationMinutes)
                        put("isConfigured", true)
                    }.toString()
                    payload["configJson"] = configJson
                }
                if (effectiveLessons != null) {
                    val array = JSONArray()
                    effectiveLessons.forEach { lesson ->
                        array.put(JSONObject().apply {
                            put("dayOfWeek", lesson.dayOfWeek)
                            put("lessonIndex", lesson.lessonIndex)
                            put("subjectName", lesson.subjectName)
                            put("topicOrNotes", lesson.topicOrNotes)
                            put("teacherName", lesson.teacherName)
                            put("colorTheme", lesson.colorTheme)
                        })
                    }
                    payload["lessonsJson"] = array.toString()
                }
                docRef.set(payload, SetOptions.merge())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to upload schedule to Firestore", e)
            }
        }
    }

    /**
     * Firestore üzerinden ders programındaki anlık değişiklikleri dinler.
     * Öğretmen ders veya zaman çizelgesi değiştirdiği anda veli ekranı canlı güncellenir.
     */
    fun observeFirestoreSchedule(
        context: Context,
        classId: String,
        onUpdate: (ScheduleConfig?, List<ScheduleLesson>?) -> Unit
    ): ListenerRegistration? {
        if (classId.isBlank() || classId == "default") return null
        val db = getFirestore(context) ?: return null
        return try {
            db.collection("classrooms").document(classId).collection("schedule").document("current")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                    try {
                        var parsedConfig: ScheduleConfig? = null
                        val configJsonStr = snapshot.getString("configJson")
                        if (!configJsonStr.isNullOrBlank()) {
                            val json = JSONObject(configJsonStr)
                            val recessList = mutableListOf<Int>()
                            val recessArray = json.optJSONArray("recessDurations")
                            if (recessArray != null) {
                                for (i in 0 until recessArray.length()) recessList.add(recessArray.getInt(i))
                            } else {
                                recessList.addAll(listOf(15, 15, 10, 15, 15))
                            }
                            parsedConfig = ScheduleConfig(
                                dailyLessonCount = json.optInt("dailyLessonCount", 6),
                                firstLessonStartTime = json.optString("firstLessonStartTime", "08:30"),
                                lessonDurationMinutes = json.optInt("lessonDurationMinutes", 40),
                                recessDurations = recessList,
                                isLunchBreakActive = json.optBoolean("isLunchBreakActive", true),
                                lunchAfterLessonIndex = json.optInt("lunchAfterLessonIndex", 4),
                                lunchDurationMinutes = json.optInt("lunchDurationMinutes", 50),
                                isConfigured = true
                            )
                            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            prefs.edit().putString(KEY_CONFIG_PREFIX + classId, configJsonStr).apply()
                        }

                        var parsedLessons: List<ScheduleLesson>? = null
                        val lessonsJsonStr = snapshot.getString("lessonsJson")
                        if (!lessonsJsonStr.isNullOrBlank()) {
                            val array = JSONArray(lessonsJsonStr)
                            val list = mutableListOf<ScheduleLesson>()
                            for (i in 0 until array.length()) {
                                val obj = array.getJSONObject(i)
                                list.add(
                                    ScheduleLesson(
                                        dayOfWeek = obj.getString("dayOfWeek"),
                                        lessonIndex = obj.getInt("lessonIndex"),
                                        subjectName = obj.getString("subjectName"),
                                        topicOrNotes = obj.optString("topicOrNotes", ""),
                                        teacherName = obj.optString("teacherName", ""),
                                        colorTheme = obj.optString("colorTheme", "NAVY")
                                    )
                                )
                            }
                            if (list.isNotEmpty()) {
                                parsedLessons = list
                                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                                prefs.edit().putString(KEY_LESSONS_PREFIX + classId, lessonsJsonStr).apply()
                            }
                        }

                        onUpdate(parsedConfig, parsedLessons)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse schedule snapshot", e)
                    }
                }
        } catch (_: Exception) {
            null
        }
    }
}
