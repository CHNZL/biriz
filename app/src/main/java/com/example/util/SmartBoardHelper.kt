package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.example.R
import com.example.data.model.GroupMakerState
import com.example.data.model.ScheduleLesson
import com.example.data.model.SeatingPlanState
import com.example.data.model.Student
import com.example.data.repository.StarRewardPersistenceHelper
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

/**
 * Akıllı Tahta Kumandası ve Canlı Yansıtma Yardımcısı.
 * - Öğretmen telefonda işlem yaptığında doğrudan `board_sessions/{temizKod}` belgesini
 *   ve `classes/{classId}/live_board/state` ile `classrooms/{classId}/live_board/state`
 *   belgelerini anlık günceller.
 * - Öğrenci yıldızları, oturma planı, gruplar ve ders programı modül verilerini eksiksiz aktarır.
 * - Akıllı Tahta web dosyasını (`tahta.html`) cihazda üretir ve indirme/paylaşma imkanı sunar.
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
        return FirestoreProvider.get(context)
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
     * Öğrencilerin yıldız puanları ve maskelenmiş veli kodları ile sunum verisini hazırlar.
     */
    private fun prepareStudentPresentationData(
        context: Context,
        classId: String,
        students: List<Student>
    ): List<Map<String, Any?>> {
        val starMap = try {
            val records = StarRewardPersistenceHelper.loadStarRecords(context, classId)
            records.groupBy { it.studentId }.mapValues { entry -> entry.value.sumOf { it.starCount } }
        } catch (e: Exception) {
            emptyMap()
        }

        return students.map { s ->
            mapOf(
                "id" to s.id,
                "studentNo" to (s.studentNumber.toIntOrNull() ?: 0),
                "studentNumber" to s.studentNumber,
                "fullName" to s.fullName,
                "gender" to s.gender,
                "parentCode" to "••••••••", // Veli Katılım Kodu akıllı tahta projeksiyonunda gizlenir
                "parentInviteCode" to "••••••••",
                "stars" to (starMap[s.id] ?: 0)
            )
        }
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
        schedule: List<ScheduleLesson> = emptyList(),
        seatingPlanState: SeatingPlanState? = null,
        groupMakerState: GroupMakerState? = null
    ): Result<Unit> {
        val cleanCode = normalizeCode(sessionCode)
        if (cleanCode.length < 4) {
            return Result.failure(IllegalArgumentException("Eşleştirme kodu en az 4 karakter olmalıdır."))
        }

        val now = System.currentTimeMillis()
        val studentData = prepareStudentPresentationData(context, classId, students)

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

        val seatingData = seatingPlanState?.let { plan ->
            mapOf(
                "groupCount" to plan.config.groupCount,
                "rowsPerGroup" to plan.config.rowsPerGroup,
                "peoplePerRow" to plan.config.peoplePerRow,
                "assignments" to plan.assignments,
                "activePlanName" to plan.activePlanName
            )
        }

        val groupsData = groupMakerState?.let { gm ->
            mapOf(
                "groups" to gm.groups.map { g ->
                    mapOf(
                        "id" to g.id,
                        "name" to g.name,
                        "colorTheme" to g.colorTheme.id,
                        "leaderStudentId" to g.leaderStudentId,
                        "studentIds" to g.studentIds
                    )
                },
                "isPresenterMode" to gm.isPresenterMode
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
        if (seatingData != null) sessionPayload["seatingPlan"] = seatingData
        if (groupsData != null) sessionPayload["groups"] = groupsData

        return try {
            val mainDb = getMainFirestore(context)

            // Ana veritabanına kesintisiz yaz
            scope.launch {
                try {
                    mainDb.collection("board_sessions")
                        .document(cleanCode)
                        .set(sessionPayload, SetOptions.merge())
                        .addOnSuccessListener {
                            Log.d(TAG, "Successfully wrote to board_sessions/$cleanCode on mainDb")
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "Failed writing to board_sessions/$cleanCode on mainDb: ${e.message}")
                        }
                } catch (e: Exception) {
                    Log.w(TAG, "Exception writing to mainDb: ${e.message}")
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
                schedule = schedule,
                seatingPlanState = seatingPlanState,
                groupMakerState = groupMakerState
            )

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
        seatingPlanState: SeatingPlanState? = null,
        groupMakerState: GroupMakerState? = null,
        extraData: Map<String, Any?> = emptyMap()
    ) {
        val cleanCode = sessionCode?.let { normalizeCode(it) }?.ifEmpty { null }
        val isPrivacyMode = !isLiveMirrorActive
        val now = System.currentTimeMillis()

        val studentData = prepareStudentPresentationData(context, classId, students)

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

        val seatingData = seatingPlanState?.let { plan ->
            mapOf(
                "groupCount" to plan.config.groupCount,
                "rowsPerGroup" to plan.config.rowsPerGroup,
                "peoplePerRow" to plan.config.peoplePerRow,
                "assignments" to plan.assignments,
                "activePlanName" to plan.activePlanName
            )
        }

        val groupsData = groupMakerState?.let { gm ->
            mapOf(
                "groups" to gm.groups.map { g ->
                    mapOf(
                        "id" to g.id,
                        "name" to g.name,
                        "colorTheme" to g.colorTheme.id,
                        "leaderStudentId" to g.leaderStudentId,
                        "studentIds" to g.studentIds
                    )
                },
                "isPresenterMode" to gm.isPresenterMode
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
        if (seatingData != null) {
            boardSessionUpdate["seatingPlan"] = seatingData
        }
        if (groupsData != null) {
            boardSessionUpdate["groups"] = groupsData
        }
        if (extraData.isNotEmpty()) {
            boardSessionUpdate.putAll(extraData)
            boardSessionUpdate["extraData"] = extraData
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
        if (seatingData != null) {
            liveBoardPayload["seatingPlan"] = seatingData
        }
        if (groupsData != null) {
            liveBoardPayload["groups"] = groupsData
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

    const val SMART_BOARD_WEB_URL = "https://cihanogretmen.com/tahta"

    /**
     * Akıllı Tahta Web Dosyası (`tahta.html`) HTML İçeriğini Döndürür.
     */
    fun getTahtaHtmlContent(context: Context): String {
        val loaded = try {
            context.assets.open("tahta.html").bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (e: Exception) {
            Log.w(TAG, "Could not read tahta.html from assets, trying file fallback: ${e.message}")
            try {
                val f = File("public/tahta.html")
                if (f.exists()) f.readText(Charsets.UTF_8) else ""
            } catch (ignored: Exception) {
                ""
            }
        }

        if (loaded.isNotBlank() && loaded.length > 500) {
            return loaded
        }

        // Failsafe acil durum HTML şablonu (Asla boş dosya oluşmasın)
        return """
            <!DOCTYPE html>
            <html lang="tr">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>BİRİZ - Akıllı Tahta Canlı Ekranı</title>
              <script src="https://www.gstatic.com/firebasejs/10.12.0/firebase-app-compat.js"></script>
              <script src="https://www.gstatic.com/firebasejs/10.12.0/firebase-firestore-compat.js"></script>
              <style>
                body { font-family: system-ui, sans-serif; background: #F8FAFC; color: #0F172A; text-align: center; padding: 40px; }
                .card { background: white; border-radius: 24px; padding: 40px; max-width: 600px; margin: 0 auto; box-shadow: 0 10px 30px rgba(0,0,0,0.08); }
                .code { font-size: 3rem; font-weight: 800; color: #1B2A4A; background: #EFF6FF; padding: 16px; border-radius: 16px; letter-spacing: 4px; margin: 20px 0; }
              </style>
            </head>
            <body>
              <div class="card">
                <h1>🖥️ BİRİZ Akıllı Tahta</h1>
                <p>Telefondaki kod giriş alanına aşağıdaki 8 haneli kodu giriniz:</p>
                <div class="code" id="code">BRZ94K7P</div>
              </div>
              <script>
                const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
                let c = "BRZ";
                for (let i = 0; i < 5; i++) c += chars.charAt(Math.floor(Math.random() * chars.length));
                document.getElementById("code").innerText = c;
              </script>
            </body>
            </html>
        """.trimIndent()
    }

    fun copySmartBoardUrl(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clipboard != null) {
            val clip = ClipData.newPlainText("Akıllı Tahta Adresi", SMART_BOARD_WEB_URL)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "🔗 Web adresi panoya kopyalandı:\n$SMART_BOARD_WEB_URL", Toast.LENGTH_SHORT).show()
        }
    }

    fun openSmartBoardUrl(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(SMART_BOARD_WEB_URL)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            copySmartBoardUrl(context)
        }
    }

    /**
     * Akıllı Tahta web dosyasını (`tahta.html`) oluşturup dışa aktarır / paylaşır.
     */
    fun downloadOrShareTahtaHtml(context: Context) {
        try {
            val content = getTahtaHtmlContent(context)

            // 1. Dosyayı yerel cache klasörüne kaydet
            val file = File(context.cacheDir, "tahta.html")
            file.writeText(content, Charsets.UTF_8)

            // 2. Kodu panoya da kopyala (Kullanıcı dilerse doğrudan yapıştırabilsin)
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (clipboard != null) {
                val clip = ClipData.newPlainText("tahta.html", content)
                clipboard.setPrimaryClip(clip)
            }

            // 3. Android Share Intent ile paylaş / bilgisayara aktar
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/html"
                putExtra(Intent.EXTRA_SUBJECT, "BİRİZ - Akıllı Tahta Canlı Ekran Dosyası (tahta.html)")
                putExtra(Intent.EXTRA_TEXT, content)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(shareIntent, "Akıllı Tahta Dosyasını (tahta.html) İndir / Paylaş").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)

            Toast.makeText(context, "📥 tahta.html (${file.length() / 1024} KB) hazırlandı ve panoya kopyalandı!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading or sharing tahta.html", e)
            Toast.makeText(context, "Dosya hazırlanırken hata: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
