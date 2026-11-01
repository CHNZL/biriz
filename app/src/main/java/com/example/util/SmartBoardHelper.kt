package com.example.util

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Akıllı Tahta Kumandası ve Canlı Yansıtma Yardımcısı.
 * - Firestore `board_sessions/{sessionCode}` eşleştirmesini yönetir.
 * - `classes/{classId}/live_board/state` ve `classrooms/{classId}/live_board/state`
 *   belgelerine öğretmenin seçtiği ekranı ve canlı veriyi gerçek zamanlı yansıtır.
 */
object SmartBoardHelper {
    private const val TAG = "SmartBoardHelper"
    private const val PREFS_NAME = "biriz_smart_board_storage"
    private const val KEY_SESSION_CODE = "key_connected_session_code"
    private const val KEY_MIRROR_ACTIVE = "key_live_mirror_active"
    private val scope = CoroutineScope(Dispatchers.IO)

    private fun getFirestore(context: Context): FirebaseFirestore? {
        return try {
            val dbId = context.getString(R.string.firestore_database_id)
            FirebaseFirestore.getInstance(dbId)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting Firestore with custom DB ID", e)
            null
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
     * `board_sessions/{sessionCode}` belgesine CONNECTED durumu yazar.
     */
    suspend fun connectSession(
        context: Context,
        sessionCode: String,
        classId: String,
        teacherUid: String,
        teacherName: String,
        currentSubPage: String,
        currentSubPageTitle: String,
        activeDay: String = "PZT"
    ): Result<Unit> {
        val cleanCode = sessionCode.trim().uppercase()
        if (cleanCode.length < 4) {
            return Result.failure(IllegalArgumentException("Eşleştirme kodu en az 4 karakter olmalıdır."))
        }

        val db = getFirestore(context)
            ?: return Result.failure(IllegalStateException("Bulut veritabanına bağlanılamadı."))

        return try {
            // 1. board_sessions/{sessionCode} güncelle
            val sessionRef = db.collection("board_sessions").document(cleanCode)
            val sessionPayload = mapOf(
                "sessionCode" to cleanCode,
                "status" to "CONNECTED",
                "teacherUid" to teacherUid,
                "teacherName" to teacherName,
                "classId" to classId,
                "connectedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            sessionRef.set(sessionPayload).await()

            // 2. Canlı Yansıtma Başlangıç Durumunu Yaz
            updateLiveBoardState(
                context = context,
                classId = classId,
                sessionCode = cleanCode,
                isLiveMirrorActive = true,
                currentSubPage = currentSubPage,
                currentSubPageTitle = currentSubPageTitle,
                activeDay = activeDay
            )

            saveConnectionState(context, cleanCode, true)
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "board_sessions/$cleanCode")
            Result.failure(e)
        }
    }

    /**
     * Akıllı tahtaya canlı ekran durumunu yansıtır.
     * `classes/{classId}/live_board/state` ve `classrooms/{classId}/live_board/state`
     */
    fun updateLiveBoardState(
        context: Context,
        classId: String,
        sessionCode: String?,
        isLiveMirrorActive: Boolean,
        currentSubPage: String,
        currentSubPageTitle: String,
        activeDay: String = "PZT",
        extraData: Map<String, Any?> = emptyMap()
    ) {
        if (classId.isBlank() || classId == "default") return
        val db = getFirestore(context) ?: return

        scope.launch {
            try {
                val payload = mutableMapOf<String, Any?>(
                    "classId" to classId,
                    "sessionCode" to (sessionCode ?: ""),
                    "status" to if (sessionCode != null) "CONNECTED" else "DISCONNECTED",
                    "isLiveMirrorActive" to isLiveMirrorActive,
                    "currentSubPage" to currentSubPage,
                    "currentSubPageTitle" to currentSubPageTitle,
                    "activeScheduleDay" to activeDay,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (extraData.isNotEmpty()) {
                    payload["extraData"] = extraData
                }

                // Hem `classes` hem de `classrooms` yollarına yazılır
                db.collection("classes").document(classId).collection("live_board").document("state").set(payload)
                db.collection("classrooms").document(classId).collection("live_board").document("state").set(payload)
            } catch (e: Exception) {
                Log.w(TAG, "Live board state update error: ${e.message}")
            }
        }
    }

    /**
     * Akıllı Tahta Bağlantısını Kes
     */
    fun disconnectSession(context: Context, sessionCode: String?, classId: String) {
        saveConnectionState(context, null, false)
        if (sessionCode.isNullOrBlank()) return

        val db = getFirestore(context) ?: return
        scope.launch {
            try {
                db.collection("board_sessions").document(sessionCode.uppercase()).update(
                    mapOf(
                        "status" to "DISCONNECTED",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                if (classId.isNotBlank()) {
                    val disconnectPayload = mapOf(
                        "classId" to classId,
                        "sessionCode" to "",
                        "status" to "DISCONNECTED",
                        "isLiveMirrorActive" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    db.collection("classes").document(classId).collection("live_board").document("state").set(disconnectPayload)
                    db.collection("classrooms").document(classId).collection("live_board").document("state").set(disconnectPayload)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error disconnecting smart board: ${e.message}")
            }
        }
    }
}
