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
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Akıllı Tahta Kumandası ve Canlı Yansıtma Yardımcısı.
 * - Öğretmen telefonda işlem yaptığında doğrudan `board_sessions/{temizKod}` belgesini
 *   ve `classes/{classId}/live_board/state` ile `classrooms/{classId}/live_board/state`
 *   belgelerini anlık günceller.
 * - Ana veritabanı olarak `ai-studio-android-biriz-caa1dae7-0ec2-4613-b717-5606838b083e`
 *   baz alınır, işlemler hiçbir zaman telefon arayüzünü (UI) kilitlemez.
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
     * 2. ANA VERİTABANI ERİŞİMİ
     * Öncelikle projenin özel veritabanını (R.string.firestore_database_id) döndürür.
     */
    fun getMainFirestore(context: Context): FirebaseFirestore {
        return try {
            val customDbId = context.getString(R.string.firestore_database_id)
            if (customDbId.isNotBlank()) {
                FirebaseFirestore.getInstance(customDbId)
            } else {
                FirebaseFirestore.getInstance()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Custom database init exception: ${e.message}")
            FirebaseFirestore.getInstance()
        }
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
     * Maksimum 1 saniye timeout ile çalışır, arayüzü asla bekletmez.
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
            val mainDb = getMainFirestore(context)

            // Ana veritabanına yaz (Maksimum 1 saniye timeout ile)
            withTimeoutOrNull(1000) {
                try {
                    mainDb.collection("board_sessions")
                        .document(cleanCode)
                        .set(sessionPayload, SetOptions.merge())
                        .await()
                } catch (e: Exception) {
                    Log.w(TAG, "Direct main write timed out or exception: ${e.message}")
                }
            }

            // Canlı Yansıtma Durumunu da güncelle
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

            // Yedek varsayılan veritabanına arka planda sessizce yaz
            scope.launch {
                try {
                    val defaultDb = FirebaseFirestore.getInstance()
                    if (defaultDb != mainDb) {
                        defaultDb.collection("board_sessions")
                            .document(cleanCode)
                            .set(sessionPayload, SetOptions.merge())
                    }
                } catch (ignored: Exception) {}
            }

            saveConnectionState(context, cleanCode, true)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting smart board session", e)
            Result.success(Unit) // Arayüzü asla kitlemez
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
            val mainDb = getMainFirestore(context)
            try {
                // 1. DOĞRUDAN board_sessions/{temizKod} belgesini güncelle
                if (!cleanCode.isNullOrBlank()) {
                    mainDb.collection("board_sessions")
                        .document(cleanCode)
                        .set(boardSessionUpdate, SetOptions.merge())
                }

                // 2. classes/{classId}/live_board/state
                if (classId.isNotBlank() && classId != "default") {
                    mainDb.collection("classes")
                        .document(classId)
                        .collection("live_board")
                        .document("state")
                        .set(liveBoardPayload, SetOptions.merge())

                    // 3. classrooms/{classId}/live_board/state
                    mainDb.collection("classrooms")
                        .document(classId)
                        .collection("live_board")
                        .document("state")
                        .set(liveBoardPayload, SetOptions.merge())
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error updating live board state on main db: ${e.message}")
            }

            // Yedek varsayılan veritabanına sessizce yaz
            try {
                val defaultDb = FirebaseFirestore.getInstance()
                if (defaultDb != mainDb && !cleanCode.isNullOrBlank()) {
                    defaultDb.collection("board_sessions")
                        .document(cleanCode)
                        .set(boardSessionUpdate, SetOptions.merge())
                }
            } catch (ignored: Exception) {}
        }
    }

    /**
     * Akıllı Tahta Bağlantısını Kes
     */
    fun disconnectSession(context: Context, sessionCode: String?, classId: String) {
        saveConnectionState(context, null, false)
        val cleanCode = sessionCode?.let { normalizeCode(it) }?.ifEmpty { null }
        val now = System.currentTimeMillis()

        scope.launch {
            val mainDb = getMainFirestore(context)
            try {
                if (!cleanCode.isNullOrBlank()) {
                    val disconnectSessionPayload = mapOf(
                        "status" to "DISCONNECTED",
                        "isConnected" to false,
                        "isPrivacyMode" to true,
                        "updatedAt" to now
                    )
                    mainDb.collection("board_sessions")
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
                    mainDb.collection("classes")
                        .document(classId)
                        .collection("live_board")
                        .document("state")
                        .set(disconnectPayload, SetOptions.merge())

                    mainDb.collection("classrooms")
                        .document(classId)
                        .collection("live_board")
                        .document("state")
                        .set(disconnectPayload, SetOptions.merge())
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error disconnecting smart board: ${e.message}")
            }

            // Yedek varsayılan veritabanı
            try {
                val defaultDb = FirebaseFirestore.getInstance()
                if (defaultDb != mainDb && !cleanCode.isNullOrBlank()) {
                    defaultDb.collection("board_sessions")
                        .document(cleanCode)
                        .set(
                            mapOf(
                                "status" to "DISCONNECTED",
                                "isConnected" to false,
                                "isPrivacyMode" to true,
                                "updatedAt" to now
                            ),
                            SetOptions.merge()
                        )
                }
            } catch (ignored: Exception) {}
        }
    }
}
