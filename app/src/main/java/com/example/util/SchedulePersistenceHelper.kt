package com.example.util

import android.content.Context
import com.example.data.model.ScheduleConfig
import com.example.data.model.ScheduleLesson
import org.json.JSONArray
import org.json.JSONObject

/**
 * Esnek Ders Programı Ayarları ve Haftalık Ders Listesi için
 * Yerel Kalıcı Depolama (SharedPreferences) Yardımcısı.
 *
 * Kullanıcının yaptığı tüm zaman çizelgesi, ders süresi, teneffüs, öğle arası
 * ve ders adı/rengi değişikliklerini cihazda kalıcı olarak saklar; böylece
 * sayfa geçişlerinde veya uygulama yeniden açıldığında varsayılana dönüş engellenir.
 */
object SchedulePersistenceHelper {

    private const val PREFS_NAME = "biriz_schedule_storage"
    private const val KEY_CONFIG_PREFIX = "config_"
    private const val KEY_LESSONS_PREFIX = "lessons_"
    private const val KEY_ACTIVE_DAY_PREFIX = "active_day_"

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
}
