package com.example.util

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.ScheduleLesson
import com.example.data.model.Student
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Akıllı Tahta Kumandası ve Canlı Yansıtma Yardımcısı.
 * - Öğretmen telefonda işlem yaptığında doğrudan `board_sessions/{temizKod}` belgesini
 *   ve `classes/{classId}/live_board/state` ile `classrooms/{classId}/live_board/state`
 *   belgelerini anlık günceller.
 * - Garanti eşleşme için hem özel `ai-studio-android-biriz-...` veritabanına
 *   hem de varsayılan `(default)` Firestore veritabanına aynı anda yazar.
 */
object SmartBoardHelper {
    private const val TAG = "SmartBoardHelper"
    private const val PREFS_NAME = "biriz_smart_board_storage"
    private const val KEY_SESSION_CODE = "key_connected_session_code"
    private const val KEY_MIRROR_ACTIVE = "key_live_mirror_active"
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * 1. KOD NORMALİZASYONU (Tire, boşluk ve yabancı karakter temizliği)
     * Örn: "BRZ9-4K7P" veya "brz9 4k7p" -> "BRZ94K7P"
     */
    fun normalizeCode(code: String): String {
        return code.replace(Regex("[^A-Za-z0-9]"), "").uppercase()
    }

    /**
     * 2. ÇİFT VERİTABANI YÖNETİMİ
     * Hem özel veritabanını hem de varsayılan (default) veritabanını döndürür.
     */
    fun getAllFirestores(context: Context): List<FirebaseFirestore> {
        val instances = mutableListOf<FirebaseFirestore>()
        // 1. Özel / İsimlendirilmiş Veritabanı
        try {
            val customDbId = context.getString(R.string.firestore_database_id)
            if (customDbId.isNotBlank()) {
                instances.add(FirebaseFirestore.getInstance(customDbId))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Custom database instance error: ${e.message}")
        }
        // 2. Varsayılan (default) Veritabanı
        try {
            val defaultDb = FirebaseFirestore.getInstance()
            if (!instances.contains(defaultDb)) {
                instances.add(defaultDb)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Default database instance error: ${e.message}")
        }
        return instances
    }

    fun getSavedSessionCode(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SESSION_CODE, null)
    }

    fun isMirrorActive(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_MIRROR_ACTIVE, true)
    }

    fun saveConnectionState(context: Context, sessionCode: String?, isMirrorActive: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_SESSION_CODE, sessionCode)
            .putBoolean(KEY_MIRROR_ACTIVE, isMirrorActive)
            .apply()
    }

    /**
     * Akıllı tahtada görünen 8 haneli kod ile eşleştirme yapar.
     * `board_sessions/{temizKod}` belgesine CONNECTED durumu yazar (SetOptions.merge() ile).
     */
    suspend fun connectSession(
        context: Context,
        sessionCode: String,
        classId: String,
        teacherUid: String,
        teacherName: String,
        schoolName: String,
        grade: Int,
        branch: String,
        currentSubPage: String,
        currentSubPageTitle: String,
        activeDay: String = "PZT",
        students: List<Student> = emptyList(),
        schedule: List<ScheduleLesson> = emptyList()
    ): Result<Unit> {
        val cleanCode = normalizeCode(sessionCode)
        if (cleanCode.length < 4) {
            return Result.failure(IllegalArgumentException("Eşleştirme kodu en az 4 karakter olmalıdır."))
        }

        val databases = getAllFirestores(context)
        if (databases.isEmpty()) {
            return Result.failure(IllegalStateException("Bulut veritabanına bağlanılamadı."))
        }

        val now = System.currentTimeMillis()

        val studentData = students.map { s ->
            mapOf(
                "id" to s.id,
                "studentNo" to (s.studentNumber.toIntOrNull() ?: 0),
                "studentNumber" to s.studentNumber,
                "fullName" to s.fullName,
                "gender" to s.gender,
                "parentCode" to s.parentInviteCode,
                "parentInviteCode" to s.parentInviteCode
            )
        }

        val scheduleData = schedule.map { l ->
            mapOf(
                "dayOfWeek" to l.dayOfWeek,
                "lessonIndex" to l.lessonIndex,
                "subjectName" to l.subjectName,
                "topicOrNotes" to l.topicOrNotes,
                "teacherName" to l.teacherName,
                "colorTheme" to l.colorTheme
            )
        }

        val sessionPayload = hashMapOf<String, Any?>(
            "sessionCode" to cleanCode,
            "status" to "CONNECTED",
            "isConnected" to true,
            "teacherUid" to teacherUid,
            "teacherName" to teacherName,
            "classId" to classId,
            "schoolName" to schoolName,
            "grade" to grade,
            "branch" to branch,
            "activeModule" to currentSubPage,
            "activeModuleTitle" to currentSubPageTitle,
            "isPrivacyMode" to false,
            "selectedDay" to activeDay,
            "students" to studentData,
            "schedule" to scheduleData,
            "updatedAt" to now,
            "connectedAt" to now,
            "serverTimestamp" to FieldValue.serverTimestamp()
        )

        return try {
            // Hem isimlendirilmiş hem de default veritabanına merge ile yaz
            for (db in databases) {
                try {
                    db.collection("board_sessions")
                        .document(cleanCode)
                        .set(sessionPayload, SetOptions.merge())
                        .await()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed writing session to db: ${e.message}")
                }
            }

            // Canlı Yansıtma Başlangıç Durumunu da tetikle
            updateLiveBoardState(
                context = context,
                classId = classId,
                sessionCode = cleanCode,
                isLiveMirrorActive = true,
                currentSubPage = currentSubPage,
                currentSubPageTitle = currentSubPageTitle,
                activeDay = activeDay,
                students = students,
                schedule = schedule
            )

            saveConnectionState(context, cleanCode, true)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting smart board session", e)
            Result.failure(e)
        }
    }

    /**
     * Akıllı tahtaya canlı ekran durumunu yansıtır.
     * DOĞRUDAN `board_sessions/{temizKod}` belgesini ve
     * `classes/{classId}/live_board/state` ile `classrooms/{classId}/live_board/state` belgelerini günceller.
     */
    fun updateLiveBoardState(
        context: Context,
        classId: String,
        sessionCode: String?,
        isLiveMirrorActive: Boolean,
        currentSubPage: String,
        currentSubPageTitle: String,
        activeDay: String = "PZT",
        students: List<Student> = emptyList(),
        schedule: List<ScheduleLesson> = emptyList(),
        extraData: Map<String, Any?> = emptyMap()
    ) {
        val cleanCode = sessionCode?.let { normalizeCode(it) }?.ifEmpty { null }
        val databases = getAllFirestores(context)
        if (databases.isEmpty()) return

        val isPrivacyMode = !isLiveMirrorActive
        val now = System.currentTimeMillis()

        val studentData = students.map { s ->
            mapOf(
                "id" to s.id,
                "studentNo" to (s.studentNumber.toIntOrNull() ?: 0),
                "studentNumber" to s.studentNumber,
                "fullName" to s.fullName,
                "gender" to s.gender,
                "parentCode" to s.parentInviteCode,
                "parentInviteCode" to s.parentInviteCode
            )
        }

        val scheduleData = schedule.map { l ->
            mapOf(
                "dayOfWeek" to l.dayOfWeek,
                "lessonIndex" to l.lessonIndex,
                "subjectName" to l.subjectName,
                "topicOrNotes" to l.topicOrNotes,
                "teacherName" to l.teacherName,
                "colorTheme" to l.colorTheme
            )
        }

        val boardSessionUpdate = hashMapOf<String, Any?>(
            "activeModule" to currentSubPage,
            "activeModuleTitle" to currentSubPageTitle,
            "isPrivacyMode" to isPrivacyMode,
            "selectedDay" to activeDay,
            "updatedAt" to now,
            "serverTimestamp" to FieldValue.serverTimestamp()
        )
        if (studentData.isNotEmpty()) {
            boardSessionUpdate["students"] = studentData
        }
        if (scheduleData.isNotEmpty()) {
            boardSessionUpdate["schedule"] = scheduleData
        }
        if (cleanCode != null) {
            boardSessionUpdate["sessionCode"] = cleanCode
            boardSessionUpdate["status"] = "CONNECTED"
            boardSessionUpdate["isConnected"] = true
        }

        val liveBoardPayload = hashMapOf<String, Any?>(
            "classId" to classId,
            "sessionCode" to (cleanCode ?: ""),
            "status" to if (cleanCode != null) "CONNECTED" else "DISCONNECTED",
            "isLiveMirrorActive" to isLiveMirrorActive,
            "isPrivacyMode" to isPrivacyMode,
            "currentSubPage" to currentSubPage,
            "currentSubPageTitle" to currentSubPageTitle,
            "activeScheduleDay" to activeDay,
            "updatedAt" to now,
            "serverTimestamp" to FieldValue.serverTimestamp()
        )
        if (studentData.isNotEmpty()) {
            liveBoardPayload["students"] = studentData
        }
        if (scheduleData.isNotEmpty()) {
            liveBoardPayload["schedule"] = scheduleData
        }
        if (extraData.isNotEmpty()) {
            liveBoardPayload["extraData"] = extraData
        }

        scope.launch {
            for (db in databases) {
                try {
                    // 1. DOĞRUDAN board_sessions/{temizKod} belgesini güncelle
                    if (!cleanCode.isNullOrBlank()) {
                        db.collection("board_sessions")
                            .document(cleanCode)
                            .set(boardSessionUpdate, SetOptions.merge())
                    }

                    // 2. classes/{classId}/live_board/state
                    if (classId.isNotBlank() && classId != "default") {
                        db.collection("classes")
                            .document(classId)
                            .collection("live_board")
                            .document("state")
                            .set(liveBoardPayload, SetOptions.merge())

                        // 3. classrooms/{classId}/live_board/state
                        db.collection("classrooms")
                            .document(classId)
                            .collection("live_board")
                            .document("state")
                            .set(liveBoardPayload, SetOptions.merge())
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error updating live board state: ${e.message}")
                }
            }
        }
    }

    /**
     * Akıllı Tahta Bağlantısını Kes
     */
    fun disconnectSession(context: Context, sessionCode: String?, classId: String) {
        saveConnectionState(context, null, false)
        val cleanCode = sessionCode?.let { normalizeCode(it) }?.ifEmpty { null }
        val databases = getAllFirestores(context)
        if (databases.isEmpty()) return

        val now = System.currentTimeMillis()
        scope.launch {
            for (db in databases) {
                try {
                    if (!cleanCode.isNullOrBlank()) {
                        val disconnectSessionPayload = mapOf(
                            "status" to "DISCONNECTED",
                            "isConnected" to false,
                            "isPrivacyMode" to true,
                            "updatedAt" to now
                        )
                        db.collection("board_sessions")
                            .document(cleanCode)
                            .set(disconnectSessionPayload, SetOptions.merge())
                    }

                    if (classId.isNotBlank() && classId != "default") {
                        val disconnectPayload = mapOf(
                            "classId" to classId,
                            "sessionCode" to "",
                            "status" to "DISCONNECTED",
                            "isLiveMirrorActive" to false,
                            "isPrivacyMode" to true,
                            "updatedAt" to now
                        )
                        db.collection("classes")
                            .document(classId)
                            .collection("live_board")
                            .document("state")
                            .set(disconnectPayload, SetOptions.merge())

                        db.collection("classrooms")
                            .document(classId)
                            .collection("live_board")
                            .document("state")
                            .set(disconnectPayload, SetOptions.merge())
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error disconnecting smart board: ${e.message}")
                }
            }
        }
    }
}
