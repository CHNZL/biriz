package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import com.example.R
import com.example.data.model.GroupMakerState
import com.example.data.model.ScheduleConfig
import com.example.data.model.ScheduleLesson
import com.example.data.model.SeatingPlanState
import com.example.data.model.Student
import com.example.data.repository.StarRewardPersistenceHelper
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Akıllı Tahta Kumandası ve Canlı Yansıtma Yardımcısı.
 * - Öğretmen telefonda işlem yaptığında yalnızca tek belgeyi (`board_sessions/{temizKod}`)
 *   delta (değişen alanlar) mantığıyla güncelleyerek Firestore yazma kotasını korur.
 * - Öğrenci yıldızları, okunan kitap sayıları, oturma planı, gruplar, ders programı,
 *   şanslı öğrenci, zamanlayıcı, kitaplık ve turnuva verilerini aktarır.
 */
object SmartBoardHelper {
    private const val TAG = "SmartBoardHelper"
    private const val PREFS_NAME = "biriz_smart_board_storage"
    private const val KEY_SESSION_CODE = "key_connected_session_code"
    private const val KEY_MIRROR_ACTIVE = "key_live_mirror_active"
    private const val KEY_TRANSPORT_MODE = "key_smart_board_transport_mode"
    private const val CLOUDFLARE_API_BASE = "https://cihanogretmen.com/tahta/api"
    private const val CLOUDFLARE_WS_BASE = "wss://cihanogretmen.com/tahta/api/ws"

    enum class TransportMode {
        NONE,
        CLOUDFLARE,
        FIRESTORE
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private val sessionWriteMutex = Mutex()
    private val wsLock = Any()

    @Volatile
    private var activeSessionCode: String? = null
    @Volatile
    private var activeConnectionId: String? = null
    @Volatile
    private var connectionEpoch: Long = 0L
    @Volatile
    private var activeTransportMode: TransportMode = TransportMode.NONE
    @Volatile
    private var phoneWebSocket: WebSocket? = null
    @Volatile
    private var phoneWsSessionCode: String? = null
    @Volatile
    private var phoneWsEpoch: Long = 0L

    // Son bilinen durum önbelleği (Yıldız, kitap, turnuva, şanslı öğrenci veya öğrenci verisi değiştiğinde anında senkronizasyon tetiklemek için)
    @Volatile
    private var lastClassId: String = ""
    @Volatile
    private var lastCurrentSubPage: String = "SINIF_LISTESI"
    @Volatile
    private var lastCurrentSubPageTitle: String = "Sınıf Listesi"
    @Volatile
    private var lastActiveDay: String = "PZT"
    @Volatile
    private var lastStudents: List<Student> = emptyList()
    @Volatile
    private var lastSchedule: List<ScheduleLesson> = emptyList()
    @Volatile
    private var lastScheduleConfig: ScheduleConfig = ScheduleConfig()
    @Volatile
    private var lastSeatingPlanState: SeatingPlanState? = null
    @Volatile
    private var lastGroupMakerState: GroupMakerState? = null
    @Volatile
    private var lastBoardCast: Map<String, Any?>? = null
    @Volatile
    private var quotaCooldownUntilMs: Long = 0L

    // Delta senkronizasyon önbelleği: Aynı ağır verilerin her sekme değişiminde tekrar yazılmasını önler
    @Volatile
    private var syncedConnectionId: String? = null
    @Volatile
    private var sentStudentsHash: Int = 0
    @Volatile
    private var sentRecentStarsHash: Int = 0
    @Volatile
    private var sentScheduleHash: Int = 0
    @Volatile
    private var sentScheduleConfigHash: Int = 0
    @Volatile
    private var sentSeatingHash: Int = 0
    @Volatile
    private var sentGroupsHash: Int = 0
    @Volatile
    private var sentLuckyHash: Int = 0
    @Volatile
    private var sentLibraryHash: Int = 0
    @Volatile
    private var sentTournamentHash: Int = 0
    @Volatile
    private var sentBoardCastHash: Int = 0

    fun isQuotaCooldownActive(): Boolean {
        if (activeTransportMode == TransportMode.CLOUDFLARE) return false
        return System.currentTimeMillis() < quotaCooldownUntilMs
    }

    fun getActiveConnectionId(): String? = activeConnectionId

    fun isCloudflareActive(context: Context): Boolean {
        return resolveTransportMode(context) == TransportMode.CLOUDFLARE
    }

    private fun clearDeltaSyncCache() {
        syncedConnectionId = null
        sentStudentsHash = 0
        sentRecentStarsHash = 0
        sentScheduleHash = 0
        sentScheduleConfigHash = 0
        sentSeatingHash = 0
        sentGroupsHash = 0
        sentLuckyHash = 0
        sentLibraryHash = 0
        sentTournamentHash = 0
        sentBoardCastHash = 0
    }

    /**
     * 1. KOD NORMALİZASYONU (Tire, boşluk ve yabancı karakter temizliği)
     * Örn: "BRZ9-4K7P" veya "brz9 4k7p" -> "BRZ94K7P"
     */
    fun normalizeCode(code: String): String {
        return code.replace(Regex("[^A-Za-z0-9]"), "").uppercase()
    }

    private fun isEpochStillActive(cleanCode: String, expectedEpoch: Long): Boolean {
        return connectionEpoch == expectedEpoch && activeSessionCode == cleanCode
    }

    /**
     * 2. ANA VERİTABANI ERİŞİMİ
     * Öncelikle projenin özel veritabanını (R.string.firestore_database_id) döndürür.
     */
    fun getMainFirestore(context: Context): FirebaseFirestore {
        return FirestoreProvider.get(context)
    }

    private val restHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(7, TimeUnit.SECONDS)
            .writeTimeout(7, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private sealed class RestWriteResult {
        object Success : RestWriteResult()
        data class HttpError(val code: Int, val message: String) : RestWriteResult()
        data class NetworkError(val exception: IOException) : RestWriteResult()
    }

    private sealed class RestGetResult {
        data class Found(val fields: JSONObject) : RestGetResult()
        object Missing : RestGetResult()
        data class Error(val detail: String) : RestGetResult()
    }

    private fun resolveFirestoreRestConfig(context: Context): Triple<String, String, String> {
        val options = try {
            FirebaseApp.getInstance().options
        } catch (_: Exception) {
            null
        }
        val projectId = options?.projectId?.takeIf { it.isNotBlank() } ?: "gen-lang-client-0343498023"
        val apiKey = options?.apiKey?.takeIf { it.isNotBlank() } ?: ""
        val dbId = try {
            context.applicationContext.getString(R.string.firestore_database_id).trim()
                .ifBlank { "(default)" }
        } catch (_: Exception) {
            "(default)"
        }
        return Triple(projectId, apiKey, dbId)
    }

    private fun buildRestDocumentUrl(
        context: Context,
        docPathSegments: List<String>,
        mergeFieldPaths: Collection<String>? = null
    ): String {
        val (projectId, apiKey, dbId) = resolveFirestoreRestConfig(context)
        val encodedPath = docPathSegments.joinToString("/") { Uri.encode(it) }
        val base = "https://firestore.googleapis.com/v1/projects/${Uri.encode(projectId)}/databases/${Uri.encode(dbId)}/documents/$encodedPath"
        val sb = StringBuilder(base).append("?key=").append(Uri.encode(apiKey))
        mergeFieldPaths?.forEach { field ->
            val cleanField = field.trim()
            if (cleanField.isNotEmpty()) {
                sb.append("&updateMask.fieldPaths=").append(Uri.encode(cleanField))
            }
        }
        return sb.toString()
    }

    private fun encodeFirestoreValue(value: Any?): JSONObject {
        return when (value) {
            null -> JSONObject().put("nullValue", JSONObject.NULL)
            is Boolean -> JSONObject().put("booleanValue", value)
            is Int -> JSONObject().put("integerValue", value.toString())
            is Long -> JSONObject().put("integerValue", value.toString())
            is Short -> JSONObject().put("integerValue", value.toString())
            is Byte -> JSONObject().put("integerValue", value.toString())
            is Float -> {
                val d = if (value.isFinite()) value.toDouble() else 0.0
                JSONObject().put("doubleValue", d)
            }
            is Double -> {
                val d = if (value.isFinite()) value else 0.0
                JSONObject().put("doubleValue", d)
            }
            is Number -> JSONObject().put("doubleValue", value.toDouble())
            is CharSequence -> JSONObject().put("stringValue", value.toString())
            is Enum<*> -> JSONObject().put("stringValue", value.name)
            is Map<*, *> -> {
                val fields = encodeFirestoreFields(value)
                JSONObject().put("mapValue", JSONObject().put("fields", fields))
            }
            is Iterable<*> -> {
                val arr = JSONArray()
                value.forEach { elem -> arr.put(encodeFirestoreValue(elem)) }
                JSONObject().put("arrayValue", JSONObject().put("values", arr))
            }
            is Array<*> -> {
                val arr = JSONArray()
                value.forEach { elem -> arr.put(encodeFirestoreValue(elem)) }
                JSONObject().put("arrayValue", JSONObject().put("values", arr))
            }
            else -> JSONObject().put("stringValue", value.toString())
        }
    }

    private fun encodeFirestoreFields(map: Map<*, *>): JSONObject {
        val fields = JSONObject()
        for ((k, v) in map) {
            val key = k?.toString()?.takeIf { it.isNotEmpty() } ?: continue
            if (v is FieldValue) continue
            fields.put(key, encodeFirestoreValue(v))
        }
        return fields
    }

    private fun restPatchDocument(
        context: Context,
        docPathSegments: List<String>,
        payload: Map<String, Any?>,
        merge: Boolean
    ): RestWriteResult {
        val mergeKeys = if (merge) {
            payload.entries
                .filter { it.key.isNotBlank() && it.value !is FieldValue }
                .map { it.key }
        } else {
            null
        }
        val url = buildRestDocumentUrl(context, docPathSegments, mergeKeys)
        val bodyObj = JSONObject().put("fields", encodeFirestoreFields(payload))
        val requestBody = bodyObj.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .patch(requestBody)
            .header("Accept", "application/json")
            .build()
        return try {
            restHttpClient.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) {
                    quotaCooldownUntilMs = 0L
                    RestWriteResult.Success
                } else {
                    if (resp.code == 429) {
                        quotaCooldownUntilMs = System.currentTimeMillis() + 15_000L
                    }
                    val errText = try {
                        resp.body?.string()?.take(240).orEmpty()
                    } catch (_: Exception) {
                        ""
                    }
                    Log.w(TAG, "REST PATCH HTTP ${resp.code} on ${docPathSegments.joinToString("/")}: $errText")
                    RestWriteResult.HttpError(resp.code, errText)
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "REST PATCH IO error on ${docPathSegments.joinToString("/")}: ${e.message}")
            RestWriteResult.NetworkError(e)
        } catch (e: Exception) {
            Log.w(TAG, "REST PATCH unexpected error on ${docPathSegments.joinToString("/")}: ${e.message}")
            RestWriteResult.NetworkError(IOException(e.message ?: "Ağ hatası", e))
        }
    }

    private fun restGetDocument(context: Context, docPathSegments: List<String>): RestGetResult {
        val url = buildRestDocumentUrl(context, docPathSegments, null)
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "application/json")
            .header("Cache-Control", "no-store")
            .build()
        return try {
            restHttpClient.newCall(request).execute().use { resp ->
                if (resp.code == 404) return RestGetResult.Missing
                if (!resp.isSuccessful) {
                    return RestGetResult.Error("HTTP ${resp.code}")
                }
                val bodyStr = resp.body?.string().orEmpty()
                val json = JSONObject(bodyStr)
                val fields = json.optJSONObject("fields") ?: return RestGetResult.Missing
                RestGetResult.Found(fields)
            }
        } catch (e: Exception) {
            RestGetResult.Error(e.message ?: "network")
        }
    }

    private sealed class CfGetResult {
        data class Found(val data: JSONObject) : CfGetResult()
        object Missing : CfGetResult()
        data class Error(val detail: String) : CfGetResult()
    }

    private fun anyToJsonValue(value: Any?): Any {
        return when (value) {
            null -> JSONObject.NULL
            is Boolean, is Int, is Long, is Short, is Byte, is String -> value
            is Float -> if (value.isFinite()) value.toDouble() else 0.0
            is Double -> if (value.isFinite()) value else 0.0
            is Number -> value.toDouble()
            is Enum<*> -> value.name
            is Map<*, *> -> mapToJsonObject(value)
            is Iterable<*> -> {
                val arr = JSONArray()
                value.forEach { elem -> arr.put(anyToJsonValue(elem)) }
                arr
            }
            is Array<*> -> {
                val arr = JSONArray()
                value.forEach { elem -> arr.put(anyToJsonValue(elem)) }
                arr
            }
            else -> value.toString()
        }
    }

    private fun mapToJsonObject(map: Map<*, *>): JSONObject {
        val obj = JSONObject()
        for ((k, v) in map) {
            val key = k?.toString()?.takeIf { it.isNotEmpty() } ?: continue
            if (v is FieldValue) continue
            obj.put(key, anyToJsonValue(v))
        }
        return obj
    }

    private fun cfPatchSession(
        cleanCode: String,
        payload: Map<String, Any?>,
        replace: Boolean
    ): RestWriteResult {
        val encodedCode = Uri.encode(cleanCode)
        val suffix = if (replace) "?replace=1" else ""
        val url = "$CLOUDFLARE_API_BASE/session/$encodedCode$suffix"
        val bodyStr = mapToJsonObject(payload).toString()
        val requestBody = bodyStr.toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .header("Accept", "application/json")
            .build()
        return try {
            restHttpClient.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) {
                    quotaCooldownUntilMs = 0L
                    RestWriteResult.Success
                } else {
                    val errText = try {
                        resp.body?.string()?.take(240).orEmpty()
                    } catch (_: Exception) {
                        ""
                    }
                    Log.w(TAG, "Cloudflare POST HTTP ${resp.code} on $cleanCode: $errText")
                    RestWriteResult.HttpError(resp.code, errText)
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "Cloudflare POST IO error on $cleanCode: ${e.message}")
            RestWriteResult.NetworkError(e)
        } catch (e: Exception) {
            Log.w(TAG, "Cloudflare POST unexpected error on $cleanCode: ${e.message}")
            RestWriteResult.NetworkError(IOException(e.message ?: "Cloudflare ağ hatası", e))
        }
    }

    private fun cfGetSession(cleanCode: String): CfGetResult {
        val url = "$CLOUDFLARE_API_BASE/session/${Uri.encode(cleanCode)}"
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "application/json")
            .header("Cache-Control", "no-store")
            .build()
        return try {
            restHttpClient.newCall(request).execute().use { resp ->
                if (resp.code == 404) return CfGetResult.Missing
                if (!resp.isSuccessful) {
                    return CfGetResult.Error("HTTP ${resp.code}")
                }
                val bodyStr = resp.body?.string().orEmpty()
                val json = JSONObject(bodyStr)
                if (!json.optBoolean("exists", true)) {
                    return CfGetResult.Missing
                }
                val data = json.optJSONObject("data") ?: return CfGetResult.Missing
                CfGetResult.Found(data)
            }
        } catch (e: Exception) {
            CfGetResult.Error(e.message ?: "network")
        }
    }

    private fun cfUploadChunk(
        cleanCode: String,
        chunkId: String,
        payload: Map<String, Any?>
    ): RestWriteResult {
        val url = "$CLOUDFLARE_API_BASE/session/${Uri.encode(cleanCode)}/chunk/${Uri.encode(chunkId)}"
        val bodyStr = mapToJsonObject(payload).toString()
        val requestBody = bodyStr.toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .put(requestBody)
            .header("Accept", "application/json")
            .build()
        return try {
            restHttpClient.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) {
                    RestWriteResult.Success
                } else {
                    val errText = try {
                        resp.body?.string()?.take(240).orEmpty()
                    } catch (_: Exception) {
                        ""
                    }
                    RestWriteResult.HttpError(resp.code, errText)
                }
            }
        } catch (e: IOException) {
            RestWriteResult.NetworkError(e)
        } catch (e: Exception) {
            RestWriteResult.NetworkError(IOException(e.message ?: "Chunk upload error", e))
        }
    }

    private fun closePhoneWebSocket() {
        synchronized(wsLock) {
            val existing = phoneWebSocket
            phoneWebSocket = null
            phoneWsSessionCode = null
            phoneWsEpoch = 0L
            try {
                existing?.close(1000, "session_closed")
            } catch (_: Exception) {}
        }
    }

    private fun ensureCloudflareWebSocket(cleanCode: String, expectedEpoch: Long) {
        if (!isEpochStillActive(cleanCode, expectedEpoch)) return
        synchronized(wsLock) {
            if (phoneWebSocket != null &&
                phoneWsSessionCode == cleanCode &&
                phoneWsEpoch == expectedEpoch
            ) {
                return
            }
            try {
                phoneWebSocket?.close(1000, "reconnecting")
            } catch (_: Exception) {}
            phoneWebSocket = null
            phoneWsSessionCode = cleanCode
            phoneWsEpoch = expectedEpoch

            val wsUrl = "$CLOUDFLARE_WS_BASE?code=${Uri.encode(cleanCode)}&role=phone"
            val request = Request.Builder().url(wsUrl).build()
            val createdSocket = restHttpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d(TAG, "Cloudflare phone WebSocket opened for $cleanCode")
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val json = JSONObject(text)
                        val msgType = json.optString("type", "")
                        if (msgType == "rtc_answer" || msgType == "rtc_ice") {
                            SmartBoardRtcManager.onRemoteSignal(json)
                        }
                    } catch (_: Exception) {}
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    synchronized(wsLock) {
                        if (phoneWebSocket === webSocket) {
                            phoneWebSocket = null
                        }
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.w(TAG, "Cloudflare phone WebSocket warning: ${t.message}")
                    synchronized(wsLock) {
                        if (phoneWebSocket === webSocket) {
                            phoneWebSocket = null
                        }
                    }
                }
            })
            phoneWebSocket = createdSocket
        }
    }

    private fun sendOverPhoneWebSocket(envelope: JSONObject, expectedCode: String): Boolean {
        val ws = synchronized(wsLock) {
            if (phoneWsSessionCode == expectedCode) phoneWebSocket else null
        } ?: return false
        return try {
            ws.send(envelope.toString())
        } catch (_: Exception) {
            false
        }
    }

    fun sendRtcSignal(signalJson: JSONObject) {
        val code = activeSessionCode ?: return
        if (activeTransportMode != TransportMode.CLOUDFLARE) return
        ensureCloudflareWebSocket(code, connectionEpoch)
        if (sendOverPhoneWebSocket(signalJson, code)) {
            return
        }
        scope.launch {
            try {
                val url = "$CLOUDFLARE_API_BASE/signal/${Uri.encode(code)}"
                val reqBody = signalJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(url)
                    .post(reqBody)
                    .header("Accept", "application/json")
                    .build()
                restHttpClient.newCall(request).execute().close()
            } catch (e: Exception) {
                Log.w(TAG, "HTTP signal relay warning: ${e.message}")
            }
        }
    }

    private fun resolveTransportMode(context: Context): TransportMode {
        val cur = activeTransportMode
        if (cur != TransportMode.NONE) return cur
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedName = prefs.getString(KEY_TRANSPORT_MODE, null)
            val parsed = savedName?.let {
                try {
                    TransportMode.valueOf(it)
                } catch (_: Exception) {
                    TransportMode.NONE
                }
            } ?: TransportMode.NONE
            if (parsed != TransportMode.NONE && !getSavedSessionCode(context).isNullOrBlank()) {
                activeTransportMode = parsed
            }
            parsed
        } catch (_: Exception) {
            TransportMode.NONE
        }
    }

    private suspend fun writeDocumentRestFirst(
        context: Context,
        docPathSegments: List<String>,
        payload: Map<String, Any?>,
        merge: Boolean,
        sdkTimeoutMs: Long = 3000L
    ): RestWriteResult {
        val restRes = restPatchDocument(context, docPathSegments, payload, merge)
        if (restRes is RestWriteResult.Success || restRes is RestWriteResult.HttpError) {
            return restRes
        }
        // Ağ hatası durumunda yerel Firestore SDK ile bir kez hızlıca dene
        return try {
            val mainDb = getMainFirestore(context)
            var ref = mainDb.collection(docPathSegments[0]).document(docPathSegments[1])
            var idx = 2
            while (idx + 1 < docPathSegments.size) {
                ref = ref.collection(docPathSegments[idx]).document(docPathSegments[idx + 1])
                idx += 2
            }
            withTimeout(sdkTimeoutMs) {
                if (merge) {
                    ref.set(payload, SetOptions.merge()).await()
                } else {
                    ref.set(payload).await()
                }
            }
            RestWriteResult.Success
        } catch (e: Exception) {
            Log.w(TAG, "SDK fallback write failed on ${docPathSegments.joinToString("/")}: ${e.message}")
            restRes
        }
    }

    private suspend fun writeSessionStateUnified(
        context: Context,
        cleanCode: String,
        payload: Map<String, Any?>,
        merge: Boolean,
        sdkTimeoutMs: Long = 3000L
    ): RestWriteResult {
        return if (resolveTransportMode(context) == TransportMode.CLOUDFLARE) {
            val cfRes = cfPatchSession(cleanCode, payload, replace = !merge)
            if (cfRes is RestWriteResult.NetworkError) {
                delay(200L)
                cfPatchSession(cleanCode, payload, replace = !merge)
            } else {
                cfRes
            }
        } else {
            writeDocumentRestFirst(
                context = context,
                docPathSegments = listOf("board_sessions", cleanCode),
                payload = payload,
                merge = merge,
                sdkTimeoutMs = sdkTimeoutMs
            )
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

    fun saveConnectionState(
        context: Context,
        sessionCode: String?,
        isMirrorActive: Boolean,
        transportMode: TransportMode? = null
    ) {
        val clean = sessionCode?.let { normalizeCode(it) }?.ifEmpty { null }
        if (clean == null) {
            if (activeSessionCode != null) {
                connectionEpoch += 1L
            }
            activeSessionCode = null
            activeConnectionId = null
            activeTransportMode = TransportMode.NONE
            closePhoneWebSocket()
            SmartBoardRtcManager.stopSession()
            clearDeltaSyncCache()
        } else {
            activeSessionCode = clean
            if (transportMode != null) {
                activeTransportMode = transportMode
            }
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
            .putString(KEY_SESSION_CODE, clean)
            .putBoolean(KEY_MIRROR_ACTIVE, isMirrorActive)
        if (clean == null) {
            editor.remove(KEY_TRANSPORT_MODE)
        } else if (transportMode != null) {
            editor.putString(KEY_TRANSPORT_MODE, transportMode.name)
        }
        editor.apply()
    }

    /**
     * Öğrencilerin yıldız puanları ve okudukları kitap sayıları ile sunum verisini hazırlar.
     * Veli kodları akıllı tahta projeksiyonuna gönderilmez.
     */
    private fun prepareStudentPresentationData(
        context: Context,
        classId: String,
        students: List<Student>
    ): List<Map<String, Any?>> {
        val starRecords = try {
            StarRewardPersistenceHelper.loadStarRecords(context, classId)
        } catch (e: Exception) {
            emptyList()
        }

        val readingRecords = try {
            LibraryPersistenceHelper.loadReadingRecords(context, classId)
        } catch (e: Exception) {
            emptyList()
        }

        val studentById = students.associateBy { it.id }
        val studentByNo = students.associateBy { it.studentNumber.trim() }
        val studentByName = students.associateBy { TurkishTextHelper.toTurkishUpperCase(it.fullName.trim()) }

        fun resolveStudentId(rawId: String, rawNo: String, rawName: String): String? {
            val cleanId = rawId.trim()
            if (cleanId.isNotEmpty() && studentById.containsKey(cleanId)) return cleanId
            val strippedNo = cleanId.removePrefix("std_").removePrefix("student_").trim()
            if (strippedNo.isNotEmpty() && studentByNo.containsKey(strippedNo)) {
                return studentByNo[strippedNo]?.id
            }
            val cleanNo = rawNo.trim()
            if (cleanNo.isNotEmpty() && studentByNo.containsKey(cleanNo)) {
                return studentByNo[cleanNo]?.id
            }
            val cleanName = TurkishTextHelper.toTurkishUpperCase(rawName.trim())
            if (cleanName.isNotEmpty() && studentByName.containsKey(cleanName)) {
                return studentByName[cleanName]?.id
            }
            return null
        }

        val starsPerStudent = mutableMapOf<String, Int>()
        for (record in starRecords) {
            val resolvedId = resolveStudentId(record.studentId, record.studentNumber, record.studentName) ?: record.studentId
            starsPerStudent[resolvedId] = (starsPerStudent[resolvedId] ?: 0) + record.starCount
        }

        val booksPerStudent = mutableMapOf<String, Int>()
        val pagesPerStudent = mutableMapOf<String, Int>()
        for (rec in readingRecords) {
            val resolvedId = resolveStudentId(rec.studentId, rec.studentNumber, rec.studentName) ?: rec.studentId
            booksPerStudent[resolvedId] = (booksPerStudent[resolvedId] ?: 0) + 1
            pagesPerStudent[resolvedId] = (pagesPerStudent[resolvedId] ?: 0) + rec.pageCount
        }

        return students.map { s ->
            mapOf(
                "id" to s.id,
                "studentNo" to (s.studentNumber.toIntOrNull() ?: 0),
                "studentNumber" to s.studentNumber,
                "fullName" to s.fullName,
                "gender" to s.gender,
                "stars" to (starsPerStudent[s.id] ?: 0),
                "booksRead" to (booksPerStudent[s.id] ?: 0),
                "pagesRead" to (pagesPerStudent[s.id] ?: 0)
            )
        }
    }

    private fun prepareRecentStarsData(context: Context, classId: String): List<Map<String, Any?>> {
        return try {
            StarRewardPersistenceHelper.loadStarRecords(context, classId)
                .take(15)
                .map { r ->
                    mapOf(
                        "id" to r.id,
                        "studentId" to r.studentId,
                        "studentName" to r.studentName,
                        "studentNumber" to r.studentNumber,
                        "categoryName" to r.categoryName,
                        "starCount" to r.starCount,
                        "note" to r.note,
                        "isLuckyBonus" to r.isLuckyBonus,
                        "timestamp" to r.timestamp
                    )
                }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun prepareLuckyStudentData(context: Context, classId: String): Map<String, Any?> {
        return try {
            val state = LuckyStudentPersistenceHelper.loadState(context, classId)
            mapOf(
                "selectedModeName" to (state?.selectedModeName ?: "ROULETTE"),
                "drawnStudentIds" to (state?.drawnStudentIds ?: emptyList<String>()),
                "excludedStudentIds" to (state?.excludedStudentIds?.toList() ?: emptyList<String>())
            )
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun prepareLibraryData(context: Context, classId: String): Map<String, Any?> {
        return try {
            val books = LibraryPersistenceHelper.loadBooks(context, classId)
            val records = LibraryPersistenceHelper.loadReadingRecords(context, classId)
            val activeLoans = books.count { it.status.equals("Okunuyor", ignoreCase = true) || it.activeAssignments.isNotEmpty() }
            mapOf(
                "totalBooksCount" to books.size,
                "activeLoansCount" to activeLoans,
                "totalCompletedCount" to records.size,
                "books" to books.take(80).map { b ->
                    mapOf(
                        "id" to b.id,
                        "registrationNo" to b.registrationNo,
                        "name" to b.name,
                        "author" to b.author,
                        "pageCount" to b.pageCount,
                        "status" to b.status,
                        "currentStudentId" to b.currentStudentId,
                        "currentStudentName" to b.currentStudentName,
                        "assignmentDate" to b.assignmentDate,
                        "activeAssignments" to b.activeAssignments.take(12).map { a ->
                            mapOf(
                                "studentId" to a.studentId,
                                "studentName" to a.studentName,
                                "assignmentDate" to a.assignmentDate
                            )
                        }
                    )
                },
                "recentRecords" to records.take(20).map { r ->
                    mapOf(
                        "id" to r.id,
                        "bookId" to r.bookId,
                        "bookName" to r.bookName,
                        "pageCount" to r.pageCount,
                        "studentId" to r.studentId,
                        "studentName" to r.studentName,
                        "studentNumber" to r.studentNumber,
                        "startDate" to r.startDate,
                        "endDate" to r.endDate,
                        "earnedStars" to r.earnedStars
                    )
                }
            )
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun prepareTournamentData(context: Context, classId: String): Map<String, Any?> {
        return try {
            val tournaments = TournamentPersistenceHelper.loadTournaments(context, classId)
            val sortedTournaments = tournaments.sortedBy { if (it.status.name == "DEVAM_EDIYOR") 0 else 1 }.take(8)
            mapOf(
                "tournaments" to sortedTournaments.map { t ->
                    val matches = TournamentPersistenceHelper.loadMatches(context, t.id).take(40)
                    mapOf(
                        "id" to t.id,
                        "name" to t.name,
                        "description" to t.description,
                        "type" to t.type.name,
                        "matchType" to t.matchType.name,
                        "status" to t.status.name,
                        "currentRound" to t.currentRound,
                        "currentStage" to t.currentStage,
                        "participantsCount" to t.participants.size,
                        "winnerId" to t.winnerId,
                        "winnerName" to t.winnerName,
                        "runnerUpId" to t.runnerUpId,
                        "runnerUpName" to t.runnerUpName,
                        "thirdPlaceId" to t.thirdPlaceId,
                        "thirdPlaceName" to t.thirdPlaceName,
                        "matches" to matches.map { m ->
                            mapOf(
                                "id" to m.id,
                                "round" to m.round,
                                "matchCode" to m.matchCode,
                                "player1Id" to m.player1Id,
                                "player2Id" to m.player2Id,
                                "player1Name" to m.player1Name,
                                "player2Name" to m.player2Name,
                                "score1" to m.score1,
                                "score2" to m.score2,
                                "winnerId" to m.winnerId,
                                "status" to m.status,
                                "groupName" to m.groupName,
                                "stage" to m.stage,
                                "isThirdPlaceMatch" to m.isThirdPlaceMatch
                            )
                        }
                    )
                }
            )
        } catch (_: Exception) {
            emptyMap()
        }
    }

    /**
     * Yıldız, kitap, turnuva, şanslı öğrenci veya öğrenci verisi değiştiğinde bağlı akıllı tahtayı anında günceller.
     */
    fun notifyDataChanged(context: Context, classId: String) {
        val activeCode = getSavedSessionCode(context)
        if (activeCode.isNullOrBlank()) return
        val targetClassId = classId.ifBlank { lastClassId }
        if (targetClassId.isBlank() || targetClassId == "default") return
        val studentsToUse = lastStudents
        if (studentsToUse.isEmpty()) return

        updateLiveBoardState(
            context = context,
            classId = targetClassId,
            sessionCode = activeCode,
            isLiveMirrorActive = isMirrorActive(context),
            currentSubPage = lastCurrentSubPage,
            currentSubPageTitle = lastCurrentSubPageTitle,
            activeDay = lastActiveDay,
            students = studentsToUse,
            schedule = lastSchedule,
            scheduleConfig = lastScheduleConfig,
            seatingPlanState = lastSeatingPlanState,
            groupMakerState = lastGroupMakerState
        )
    }

    /**
     * Zamanlayıcı (Timer) durumunu doğrudan bağlı akıllı tahtaya senkronize eder.
     */
    fun syncTimerState(
        context: Context,
        classId: String,
        modeTitle: String,
        modeEmoji: String,
        modeDescription: String,
        totalSeconds: Int,
        remainingSeconds: Int,
        isRunning: Boolean,
        isFinished: Boolean
    ) {
        val activeCode = getSavedSessionCode(context)?.let { normalizeCode(it) }?.ifEmpty { null } ?: return
        if (activeSessionCode != activeCode) return
        val mode = resolveTransportMode(context)
        if (mode != TransportMode.CLOUDFLARE && isQuotaCooldownActive()) return
        val expectedEpoch = connectionEpoch
        val now = System.currentTimeMillis()
        val payload = hashMapOf<String, Any?>(
            "timerMode" to modeTitle,
            "timerModeEmoji" to modeEmoji,
            "timerModeDescription" to modeDescription,
            "timerTotalSeconds" to totalSeconds,
            "timerRemainingSeconds" to remainingSeconds,
            "timerIsRunning" to isRunning,
            "timerIsFinished" to isFinished,
            "updatedAt" to now
        )
        scope.launch {
            sessionWriteMutex.withLock {
                if (!isEpochStillActive(activeCode, expectedEpoch)) return@withLock
                if (mode != TransportMode.CLOUDFLARE && isQuotaCooldownActive()) return@withLock
                try {
                    if (mode == TransportMode.CLOUDFLARE) {
                        ensureCloudflareWebSocket(activeCode, expectedEpoch)
                        val wsMsg = JSONObject()
                            .put("type", "state_patch")
                            .put("replace", false)
                            .put("data", mapToJsonObject(payload))
                        if (!sendOverPhoneWebSocket(wsMsg, activeCode)) {
                            cfPatchSession(activeCode, payload, replace = false)
                        }
                    } else {
                        restPatchDocument(context, listOf("board_sessions", activeCode), payload, merge = true)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error syncing timer state: ${e.message}")
                }
            }
        }
    }

    /**
     * Şanslı Öğrenci çarkı dönerken o anki adayı canlı olarak akıllı tahtaya iletir.
     */
    fun syncLuckyRollingCandidate(
        context: Context,
        classId: String,
        rollingName: String,
        rollingNo: String,
        rollingGender: String
    ) {
        val activeCode = getSavedSessionCode(context)?.let { normalizeCode(it) }?.ifEmpty { null } ?: return
        if (activeSessionCode != activeCode) return
        val mode = resolveTransportMode(context)
        if (mode != TransportMode.CLOUDFLARE && isQuotaCooldownActive()) return
        val expectedEpoch = connectionEpoch
        val now = System.currentTimeMillis()
        val payload = mapOf(
            "isRolling" to true,
            "rollingCandidate" to mapOf(
                "fullName" to rollingName,
                "studentNumber" to rollingNo,
                "gender" to rollingGender
            ),
            "updatedAt" to now
        )
        scope.launch {
            sessionWriteMutex.withLock {
                if (!isEpochStillActive(activeCode, expectedEpoch)) return@withLock
                if (mode != TransportMode.CLOUDFLARE && isQuotaCooldownActive()) return@withLock
                try {
                    if (mode == TransportMode.CLOUDFLARE) {
                        ensureCloudflareWebSocket(activeCode, expectedEpoch)
                        val wsMsg = JSONObject()
                            .put("type", "state_patch")
                            .put("replace", false)
                            .put("data", mapToJsonObject(payload))
                        if (!sendOverPhoneWebSocket(wsMsg, activeCode)) {
                            cfPatchSession(activeCode, payload, replace = false)
                        }
                    } else {
                        restPatchDocument(context, listOf("board_sessions", activeCode), payload, merge = true)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Akıllı tahtada görünen 8 haneli kod ile sunucu onaylı eşleştirme yapar.
     * 1. Aşama: Öncelikle Cloudflare (`cihanogretmen.com/tahta/api`) üzerinden kotasız el sıkışma yapar.
     *    Cloudflare geçici olarak erişilemezse şeffaf şekilde Firestore yedek kanalına geçer.
     * 2. Aşama: El sıkışma onaylandıktan hemen sonra sınıf verilerini arka planda aktarır.
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
        scheduleConfig: ScheduleConfig = ScheduleConfig(),
        seatingPlanState: SeatingPlanState? = null,
        groupMakerState: GroupMakerState? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val cleanCode = normalizeCode(sessionCode)
        if (cleanCode.length < 4) {
            return@withContext Result.failure(IllegalArgumentException("Eşleştirme kodu en az 4 karakter olmalıdır."))
        }

        val myEpoch = ++connectionEpoch
        val now = System.currentTimeMillis()
        val newConnectionId = "conn_${now}_${UUID.randomUUID().toString().replace("-", "").take(8)}"
        activeSessionCode = cleanCode
        activeConnectionId = newConnectionId
        activeTransportMode = TransportMode.NONE
        closePhoneWebSocket()
        SmartBoardRtcManager.stopSession()
        lastBoardCast = null
        quotaCooldownUntilMs = 0L
        clearDeltaSyncCache()

        lastClassId = classId
        lastCurrentSubPage = currentSubPage
        lastCurrentSubPageTitle = currentSubPageTitle
        lastActiveDay = activeDay
        if (students.isNotEmpty()) lastStudents = students
        if (schedule.isNotEmpty()) lastSchedule = schedule
        lastScheduleConfig = scheduleConfig
        if (seatingPlanState != null) lastSeatingPlanState = seatingPlanState
        if (groupMakerState != null) lastGroupMakerState = groupMakerState

        val handshakePayload = hashMapOf<String, Any?>(
            "sessionCode" to cleanCode,
            "connectionId" to newConnectionId,
            "status" to "CONNECTED",
            "isConnected" to true,
            "isLiveMirrorActive" to true,
            "isPrivacyMode" to false,
            "teacherUid" to teacherUid,
            "teacherName" to teacherName,
            "classId" to classId,
            "schoolName" to schoolName,
            "grade" to grade,
            "branch" to branch,
            "activeModule" to currentSubPage,
            "activeModuleTitle" to currentSubPageTitle,
            "currentSubPage" to currentSubPage,
            "currentSubPageTitle" to currentSubPageTitle,
            "selectedDay" to activeDay,
            "activeScheduleDay" to activeDay,
            "connectedAt" to now,
            "updatedAt" to now
        )

        var chosenTransport = TransportMode.NONE
        val handshakeResult = sessionWriteMutex.withLock {
            if (!isEpochStillActive(cleanCode, myEpoch)) {
                return@withLock Result.failure(IllegalStateException("Bağlantı işlemi iptal edildi."))
            }

            // 1. ÖNCELİKLİ KANAL: Cloudflare Durable Object (Firebase kotası harcamaz, MEB ağında sorunsuz çalışır)
            var cfResult = cfPatchSession(cleanCode, handshakePayload, replace = true)
            if (cfResult is RestWriteResult.NetworkError && isEpochStillActive(cleanCode, myEpoch)) {
                delay(250L)
                if (isEpochStillActive(cleanCode, myEpoch)) {
                    cfResult = cfPatchSession(cleanCode, handshakePayload, replace = true)
                }
            }

            if (cfResult is RestWriteResult.Success) {
                chosenTransport = TransportMode.CLOUDFLARE
                if (!isEpochStillActive(cleanCode, myEpoch)) {
                    return@withLock Result.failure(IllegalStateException("Bağlantı işlemi iptal edildi."))
                }
                saveConnectionState(context, cleanCode, true, TransportMode.CLOUDFLARE)
                ensureCloudflareWebSocket(cleanCode, myEpoch)
                return@withLock Result.success(Unit)
            }

            Log.w(TAG, "Cloudflare handshake unavailable for $cleanCode ($cfResult), falling back to Firestore")

            // 2. YEDEK KANAL: Firestore REST PATCH -> SDK fallback
            var writeResult = restPatchDocument(
                context = context,
                docPathSegments = listOf("board_sessions", cleanCode),
                payload = handshakePayload,
                merge = false
            )

            if (writeResult is RestWriteResult.NetworkError && isEpochStillActive(cleanCode, myEpoch)) {
                delay(300L)
                if (isEpochStillActive(cleanCode, myEpoch)) {
                    writeResult = restPatchDocument(
                        context = context,
                        docPathSegments = listOf("board_sessions", cleanCode),
                        payload = handshakePayload,
                        merge = false
                    )
                }
            }

            if (writeResult is RestWriteResult.NetworkError && isEpochStillActive(cleanCode, myEpoch)) {
                try {
                    val mainDb = getMainFirestore(context)
                    val sdkHandshake = HashMap(handshakePayload).apply {
                        put("disconnectedAt", FieldValue.delete())
                        put("disconnectedSessionCode", FieldValue.delete())
                        put("disconnectedConnectionId", FieldValue.delete())
                        put("boardCast", FieldValue.delete())
                    }
                    withTimeout(4000L) {
                        mainDb.collection("board_sessions")
                            .document(cleanCode)
                            .set(sdkHandshake, SetOptions.merge())
                            .await()
                    }
                    writeResult = RestWriteResult.Success
                } catch (e: Exception) {
                    Log.w(TAG, "SDK handshake fallback failed for $cleanCode: ${e.message}")
                }
            }

            if (!isEpochStillActive(cleanCode, myEpoch)) {
                return@withLock Result.failure(IllegalStateException("Bağlantı işlemi iptal edildi."))
            }

            when (writeResult) {
                is RestWriteResult.Success -> {
                    chosenTransport = TransportMode.FIRESTORE
                    saveConnectionState(context, cleanCode, true, TransportMode.FIRESTORE)
                    Result.success(Unit)
                }
                is RestWriteResult.HttpError -> {
                    val detail = when (writeResult.code) {
                        429 -> "Tahta sunucusu erişilemiyor ve Firebase günlük yazma kotası dolu (HTTP 429)."
                        403 -> "Tahta sunucusu yazma izni vermedi (HTTP 403)."
                        400 -> "Tahta isteği geçersiz biçimde reddedildi (HTTP 400)."
                        else -> "Tahta sunucusu yanıt vermedi (HTTP ${writeResult.code})."
                    }
                    Result.failure(Exception(detail))
                }
                is RestWriteResult.NetworkError -> {
                    Result.failure(
                        Exception("Tahta sunucusuna ulaşılamadı. İnternet bağlantınızı kontrol edip tekrar deneyin.")
                    )
                }
            }
        }

        if (handshakeResult.isFailure) {
            if (isEpochStillActive(cleanCode, myEpoch)) {
                connectionEpoch += 1L
                activeSessionCode = null
                activeConnectionId = null
                activeTransportMode = TransportMode.NONE
                saveConnectionState(context, null, false)
            }
            return@withContext handshakeResult
        }

        // 2. AŞAMA: Ağır sınıf modül verilerini arka planda güvenli şekilde gönder
        scope.launch {
            val dataNow = System.currentTimeMillis()
            val studentData = prepareStudentPresentationData(context, classId, students)
            val recentStars = prepareRecentStarsData(context, classId)
            val luckyData = prepareLuckyStudentData(context, classId)
            val libraryData = prepareLibraryData(context, classId)
            val tournamentData = prepareTournamentData(context, classId)

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

            val scheduleConfigData = mapOf(
                "dailyLessonCount" to scheduleConfig.dailyLessonCount,
                "firstLessonStartTime" to scheduleConfig.firstLessonStartTime,
                "lessonDurationMinutes" to scheduleConfig.lessonDurationMinutes,
                "recessDurations" to scheduleConfig.recessDurations,
                "isLunchBreakActive" to scheduleConfig.isLunchBreakActive,
                "lunchAfterLessonIndex" to scheduleConfig.lunchAfterLessonIndex,
                "lunchDurationMinutes" to scheduleConfig.lunchDurationMinutes
            )

            val seatingData = seatingPlanState?.let { plan ->
                mapOf(
                    "groupCount" to plan.config.groupCount,
                    "rowsPerGroup" to plan.config.rowsPerGroup,
                    "peoplePerRow" to plan.config.peoplePerRow,
                    "windowPosition" to plan.config.windowPosition.name,
                    "doorPosition" to plan.config.doorPosition.name,
                    "deskPosition" to plan.config.deskPosition.name,
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
                    "unassignedStudentIds" to gm.unassignedStudentIds,
                    "isPresenterMode" to gm.isPresenterMode
                )
            }

            val fullPayload = hashMapOf<String, Any?>(
                "sessionCode" to cleanCode,
                "connectionId" to newConnectionId,
                "status" to "CONNECTED",
                "isConnected" to true,
                "isLiveMirrorActive" to true,
                "isPrivacyMode" to false,
                "classId" to classId,
                "students" to studentData,
                "recentStars" to recentStars,
                "schedule" to scheduleData,
                "scheduleConfig" to scheduleConfigData,
                "luckyData" to luckyData,
                "libraryData" to libraryData,
                "tournamentData" to tournamentData,
                "updatedAt" to dataNow
            )
            if (seatingData != null) fullPayload["seatingPlan"] = seatingData
            if (groupsData != null) fullPayload["groups"] = groupsData

            sessionWriteMutex.withLock {
                if (!isEpochStillActive(cleanCode, myEpoch)) return@withLock
                var finalWriteRes: RestWriteResult
                var syncedLibraryAndTournament: Boolean
                if (chosenTransport == TransportMode.CLOUDFLARE) {
                    finalWriteRes = cfPatchSession(cleanCode, fullPayload, replace = false)
                    if (finalWriteRes is RestWriteResult.NetworkError && isEpochStillActive(cleanCode, myEpoch)) {
                        delay(250L)
                        if (isEpochStillActive(cleanCode, myEpoch)) {
                            finalWriteRes = cfPatchSession(cleanCode, fullPayload, replace = false)
                        }
                    }
                    syncedLibraryAndTournament = (finalWriteRes is RestWriteResult.Success)
                } else {
                    finalWriteRes = restPatchDocument(
                        context = context,
                        docPathSegments = listOf("board_sessions", cleanCode),
                        payload = fullPayload,
                        merge = true
                    )
                    syncedLibraryAndTournament = (finalWriteRes is RestWriteResult.Success)
                    if (finalWriteRes !is RestWriteResult.Success &&
                        !(finalWriteRes is RestWriteResult.HttpError && finalWriteRes.code == 429) &&
                        isEpochStillActive(cleanCode, myEpoch)
                    ) {
                        val slimPayload = HashMap(fullPayload).apply {
                            remove("libraryData")
                            remove("tournamentData")
                        }
                        finalWriteRes = writeDocumentRestFirst(
                            context = context,
                            docPathSegments = listOf("board_sessions", cleanCode),
                            payload = slimPayload,
                            merge = true
                        )
                        syncedLibraryAndTournament = false
                    }
                }

                if (finalWriteRes is RestWriteResult.Success && isEpochStillActive(cleanCode, myEpoch)) {
                    syncedConnectionId = newConnectionId
                    sentStudentsHash = studentData.hashCode()
                    sentRecentStarsHash = recentStars.hashCode()
                    sentScheduleHash = scheduleData.hashCode()
                    sentScheduleConfigHash = scheduleConfigData.hashCode()
                    sentSeatingHash = seatingData?.hashCode() ?: 0
                    sentGroupsHash = groupsData?.hashCode() ?: 0
                    sentLuckyHash = luckyData.hashCode()
                    if (syncedLibraryAndTournament) {
                        sentLibraryHash = libraryData.hashCode()
                        sentTournamentHash = tournamentData.hashCode()
                    }
                }
            }
        }

        return@withContext Result.success(Unit)
    }

    /**
     * Akıllı tahtaya canlı ekran durumunu yansıtır.
     * Yalnızca `board_sessions/{temizKod}` belgesine delta (değişen alanlar) güncellemeleri yazar;
     * sekme geçişlerinde aynı ağır modül verilerini tekrar göndermeyerek kota kullanımını en aza indirir.
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
        scheduleConfig: ScheduleConfig = lastScheduleConfig,
        seatingPlanState: SeatingPlanState? = null,
        groupMakerState: GroupMakerState? = null,
        extraData: Map<String, Any?> = emptyMap()
    ) {
        if (classId.isNotBlank() && classId != "default") lastClassId = classId
        lastCurrentSubPage = currentSubPage
        lastCurrentSubPageTitle = currentSubPageTitle
        lastActiveDay = activeDay
        if (students.isNotEmpty()) lastStudents = students
        if (schedule.isNotEmpty()) lastSchedule = schedule
        lastScheduleConfig = scheduleConfig
        if (seatingPlanState != null) lastSeatingPlanState = seatingPlanState
        if (groupMakerState != null) lastGroupMakerState = groupMakerState

        val cleanCode = sessionCode?.let { normalizeCode(it) }?.ifEmpty { null } ?: return
        if (activeSessionCode == null) {
            val saved = getSavedSessionCode(context)?.let { normalizeCode(it) }?.ifEmpty { null }
            if (saved == cleanCode) {
                activeSessionCode = saved
            }
        }
        if (activeSessionCode != cleanCode) return
        val mode = resolveTransportMode(context)
        if (mode != TransportMode.CLOUDFLARE && isQuotaCooldownActive()) return
        val expectedEpoch = connectionEpoch

        val isPrivacyMode = !isLiveMirrorActive
        val now = System.currentTimeMillis()
        val currentConnId = activeConnectionId
        val forceFullSync = currentConnId.isNullOrBlank() || syncedConnectionId != currentConnId

        val effectiveStudents = if (students.isNotEmpty()) students else lastStudents
        val effectiveSchedule = if (schedule.isNotEmpty()) schedule else lastSchedule
        val effectiveSeating = seatingPlanState ?: lastSeatingPlanState
        val effectiveGroups = groupMakerState ?: lastGroupMakerState

        val studentData = prepareStudentPresentationData(context, classId, effectiveStudents)
        val recentStars = prepareRecentStarsData(context, classId)
        val luckyData = prepareLuckyStudentData(context, classId)
        val libraryData = prepareLibraryData(context, classId)
        val tournamentData = prepareTournamentData(context, classId)

        val scheduleData = effectiveSchedule.map { l ->
            mapOf(
                "dayOfWeek" to l.dayOfWeek,
                "lessonIndex" to l.lessonIndex,
                "subjectName" to l.subjectName,
                "topicOrNotes" to l.topicOrNotes,
                "teacherName" to l.teacherName,
                "colorTheme" to l.colorTheme
            )
        }

        val scheduleConfigData = mapOf(
            "dailyLessonCount" to scheduleConfig.dailyLessonCount,
            "firstLessonStartTime" to scheduleConfig.firstLessonStartTime,
            "lessonDurationMinutes" to scheduleConfig.lessonDurationMinutes,
            "recessDurations" to scheduleConfig.recessDurations,
            "isLunchBreakActive" to scheduleConfig.isLunchBreakActive,
            "lunchAfterLessonIndex" to scheduleConfig.lunchAfterLessonIndex,
            "lunchDurationMinutes" to scheduleConfig.lunchDurationMinutes
        )

        val seatingData = effectiveSeating?.let { plan ->
            mapOf(
                "groupCount" to plan.config.groupCount,
                "rowsPerGroup" to plan.config.rowsPerGroup,
                "peoplePerRow" to plan.config.peoplePerRow,
                "windowPosition" to plan.config.windowPosition.name,
                "doorPosition" to plan.config.doorPosition.name,
                "deskPosition" to plan.config.deskPosition.name,
                "assignments" to plan.assignments,
                "activePlanName" to plan.activePlanName
            )
        }

        val groupsData = effectiveGroups?.let { gm ->
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
                "unassignedStudentIds" to gm.unassignedStudentIds,
                "isPresenterMode" to gm.isPresenterMode
            )
        }

        val studentsHash = studentData.hashCode()
        val recentStarsHash = recentStars.hashCode()
        val scheduleHash = scheduleData.hashCode()
        val scheduleConfigHash = scheduleConfigData.hashCode()
        val seatingHash = seatingData?.hashCode() ?: 0
        val groupsHash = groupsData?.hashCode() ?: 0
        val luckyHash = luckyData.hashCode()
        val libraryHash = libraryData.hashCode()
        val tournamentHash = tournamentData.hashCode()

        val activeCast = lastBoardCast
        val boardCastHash = activeCast?.hashCode() ?: 0
        val isCastCurrentlyActive = (activeCast?.get("active") as? Boolean) == true
        val effectiveBoardSubPage = if (isCastCurrentlyActive) "PAYLASIM" else currentSubPage
        val effectiveBoardSubPageTitle = if (isCastCurrentlyActive) "📡 Paylaşım" else currentSubPageTitle

        val boardSessionUpdate = hashMapOf<String, Any?>(
            "classId" to classId,
            "activeModule" to effectiveBoardSubPage,
            "activeModuleTitle" to effectiveBoardSubPageTitle,
            "currentSubPage" to effectiveBoardSubPage,
            "currentSubPageTitle" to effectiveBoardSubPageTitle,
            "isLiveMirrorActive" to isLiveMirrorActive,
            "isPrivacyMode" to isPrivacyMode,
            "selectedDay" to activeDay,
            "activeScheduleDay" to activeDay,
            "updatedAt" to now,
            "sessionCode" to cleanCode,
            "status" to "CONNECTED",
            "isConnected" to true
        )
        if (!currentConnId.isNullOrBlank()) {
            boardSessionUpdate["connectionId"] = currentConnId
        }

        val includedStudents = studentData.isNotEmpty() && (forceFullSync || studentsHash != sentStudentsHash)
        if (includedStudents) boardSessionUpdate["students"] = studentData

        val includedRecentStars = forceFullSync || recentStarsHash != sentRecentStarsHash
        if (includedRecentStars) boardSessionUpdate["recentStars"] = recentStars

        val includedSchedule = scheduleData.isNotEmpty() && (forceFullSync || scheduleHash != sentScheduleHash)
        if (includedSchedule) boardSessionUpdate["schedule"] = scheduleData

        val includedScheduleConfig = forceFullSync || scheduleConfigHash != sentScheduleConfigHash
        if (includedScheduleConfig) boardSessionUpdate["scheduleConfig"] = scheduleConfigData

        val includedSeating = seatingData != null && (forceFullSync || seatingHash != sentSeatingHash)
        if (includedSeating) boardSessionUpdate["seatingPlan"] = seatingData

        val includedGroups = groupsData != null && (forceFullSync || groupsHash != sentGroupsHash)
        if (includedGroups) boardSessionUpdate["groups"] = groupsData

        val includedLucky = forceFullSync || luckyHash != sentLuckyHash
        if (includedLucky) boardSessionUpdate["luckyData"] = luckyData

        val includedLibrary = forceFullSync || libraryHash != sentLibraryHash
        if (includedLibrary) boardSessionUpdate["libraryData"] = libraryData

        val includedTournament = forceFullSync || tournamentHash != sentTournamentHash
        if (includedTournament) boardSessionUpdate["tournamentData"] = tournamentData

        val includedBoardCast = activeCast != null && (forceFullSync || boardCastHash != sentBoardCastHash)
        if (includedBoardCast) boardSessionUpdate["boardCast"] = activeCast

        if (extraData.isNotEmpty()) {
            boardSessionUpdate.putAll(extraData)
            boardSessionUpdate["extraData"] = extraData
        }

        scope.launch {
            sessionWriteMutex.withLock {
                if (!isEpochStillActive(cleanCode, expectedEpoch)) return@withLock
                if (mode != TransportMode.CLOUDFLARE && isQuotaCooldownActive()) return@withLock
                try {
                    if (mode == TransportMode.CLOUDFLARE) {
                        ensureCloudflareWebSocket(cleanCode, expectedEpoch)
                    }
                    val res = writeSessionStateUnified(
                        context = context,
                        cleanCode = cleanCode,
                        payload = boardSessionUpdate,
                        merge = true
                    )
                    if (res is RestWriteResult.Success && isEpochStillActive(cleanCode, expectedEpoch)) {
                        if (!currentConnId.isNullOrBlank()) {
                            syncedConnectionId = currentConnId
                        }
                        if (includedStudents) sentStudentsHash = studentsHash
                        if (includedRecentStars) sentRecentStarsHash = recentStarsHash
                        if (includedSchedule) sentScheduleHash = scheduleHash
                        if (includedScheduleConfig) sentScheduleConfigHash = scheduleConfigHash
                        if (includedSeating) sentSeatingHash = seatingHash
                        if (includedGroups) sentGroupsHash = groupsHash
                        if (includedLucky) sentLuckyHash = luckyHash
                        if (includedLibrary) sentLibraryHash = libraryHash
                        if (includedTournament) sentTournamentHash = tournamentHash
                        if (includedBoardCast) sentBoardCastHash = boardCastHash
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
        val targetCode = sessionCode?.let { normalizeCode(it) }?.ifEmpty { null }
            ?: activeSessionCode
            ?: getSavedSessionCode(context)?.let { normalizeCode(it) }?.ifEmpty { null }
        val prevConnId = activeConnectionId ?: ""
        val prevTransportMode = resolveTransportMode(context)
        connectionEpoch += 1L
        activeSessionCode = null
        activeConnectionId = null
        lastBoardCast = null
        clearDeltaSyncCache()
        saveConnectionState(context, null, false)

        val now = System.currentTimeMillis()
        val disconnectSessionPayload = hashMapOf<String, Any?>(
            "sessionCode" to (targetCode ?: ""),
            "connectionId" to "",
            "disconnectedConnectionId" to prevConnId,
            "status" to "DISCONNECTED",
            "isConnected" to false,
            "isLiveMirrorActive" to false,
            "isPrivacyMode" to true,
            "classId" to "",
            "boardCast" to mapOf(
                "active" to false,
                "castId" to "",
                "isPaused" to false,
                "inlineBase64" to "",
                "updatedAt" to now
            ),
            "disconnectedAt" to now,
            "updatedAt" to now
        )

        scope.launch {
            withContext(NonCancellable) {
                sessionWriteMutex.withLock {
                    if (!targetCode.isNullOrBlank()) {
                        if (prevTransportMode == TransportMode.CLOUDFLARE || prevTransportMode == TransportMode.NONE) {
                            var cfRes = cfPatchSession(targetCode, disconnectSessionPayload, replace = false)
                            if (cfRes !is RestWriteResult.Success) {
                                delay(300L)
                                cfRes = cfPatchSession(targetCode, disconnectSessionPayload, replace = false)
                            }
                            if (prevTransportMode == TransportMode.CLOUDFLARE || cfRes is RestWriteResult.Success) {
                                return@withLock
                            }
                        }
                        val res = writeDocumentRestFirst(
                            context = context,
                            docPathSegments = listOf("board_sessions", targetCode),
                            payload = disconnectSessionPayload,
                            merge = true,
                            sdkTimeoutMs = 3000L
                        )
                        if (res !is RestWriteResult.Success &&
                            !(res is RestWriteResult.HttpError && res.code == 429)
                        ) {
                            delay(350L)
                            writeDocumentRestFirst(
                                context = context,
                                docPathSegments = listOf("board_sessions", targetCode),
                                payload = disconnectSessionPayload,
                                merge = true,
                                sdkTimeoutMs = 3000L
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Uygulama açılışında yerelde kayıtlı tahta oturumunun sunucuda hâlâ aktif olup olmadığını doğrular.
     */
    suspend fun verifySessionStillConnected(context: Context, sessionCode: String): Boolean = withContext(Dispatchers.IO) {
        val clean = normalizeCode(sessionCode)
        if (clean.length < 4) return@withContext false
        val savedMode = resolveTransportMode(context)

        if (savedMode == TransportMode.CLOUDFLARE || savedMode == TransportMode.NONE) {
            when (val cfRes = cfGetSession(clean)) {
                is CfGetResult.Found -> {
                    val st = cfRes.data.optString("status", "").uppercase(Locale.ROOT)
                    val isConn = cfRes.data.optBoolean("isConnected", true)
                    val alive = !(st == "DISCONNECTED" || st == "WAITING" || (!isConn && st != "CONNECTED"))
                    if (alive) {
                        activeSessionCode = clean
                        activeTransportMode = TransportMode.CLOUDFLARE
                        val existingConnId = cfRes.data.optString("connectionId", "")
                        if (existingConnId.isNotBlank()) {
                            activeConnectionId = existingConnId
                        }
                        ensureCloudflareWebSocket(clean, connectionEpoch)
                    }
                    return@withContext alive
                }
                is CfGetResult.Missing -> {
                    if (savedMode == TransportMode.CLOUDFLARE) return@withContext false
                }
                is CfGetResult.Error -> {
                    if (savedMode == TransportMode.CLOUDFLARE) return@withContext true
                }
            }
        }

        return@withContext when (val res = restGetDocument(context, listOf("board_sessions", clean))) {
            is RestGetResult.Missing -> false
            is RestGetResult.Error -> true // Çevrimdışıysa yerel oturumu koru
            is RestGetResult.Found -> {
                val st = res.fields.optJSONObject("status")?.optString("stringValue", "").orEmpty().uppercase(Locale.ROOT)
                val isConnObj = res.fields.optJSONObject("isConnected")
                val isConn = if (isConnObj != null && isConnObj.has("booleanValue")) {
                    isConnObj.optBoolean("booleanValue", true)
                } else {
                    null
                }
                !(st == "DISCONNECTED" || st == "WAITING" || (isConn == false && st != "CONNECTED"))
            }
        }
    }

    private fun resolveMimeType(context: Context, uri: Uri, fileName: String): String {
        val rawMime = context.contentResolver.getType(uri)?.trim()?.lowercase(Locale.ROOT)
        if (!rawMime.isNullOrBlank() && rawMime != "application/octet-stream") {
            return rawMime
        }
        val ext = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        if (ext.isNotBlank()) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)?.let { return it }
            return when (ext) {
                "pdf" -> "application/pdf"
                "png" -> "image/png"
                "jpg", "jpeg" -> "image/jpeg"
                "webp" -> "image/webp"
                "gif" -> "image/gif"
                "svg" -> "image/svg+xml"
                "mp4" -> "video/mp4"
                "webm" -> "video/webm"
                "mp3" -> "audio/mpeg"
                "wav" -> "audio/wav"
                "m4a" -> "audio/mp4"
                "html", "htm" -> "text/html"
                "txt", "csv" -> "text/plain"
                "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                "ppt" -> "application/vnd.ms-powerpoint"
                "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                "doc" -> "application/msword"
                "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                "apk" -> "application/vnd.android.package-archive"
                "exe" -> "application/x-msdownload"
                "zip" -> "application/zip"
                else -> "application/octet-stream"
            }
        }
        return "application/octet-stream"
    }

    private fun resolveFileName(context: Context, uri: Uri): String {
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) {
                    val name = cursor.getString(idx)
                    if (!name.isNullOrBlank()) return name
                }
            }
        } catch (_: Exception) {}
        return uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { "dosya" } ?: "dosya"
    }

    private fun optimizeLargeImageIfPossible(rawBytes: ByteArray, mimeType: String): Pair<ByteArray, String> {
        if (!mimeType.startsWith("image/") || mimeType == "image/gif" || mimeType == "image/svg+xml") {
            return rawBytes to mimeType
        }
        if (rawBytes.size <= 380 * 1024) {
            return rawBytes to mimeType
        }
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, bounds)
            val maxSide = maxOf(bounds.outWidth, bounds.outHeight)
            var sampleSize = 1
            while (maxSide / sampleSize > 1920) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bmp = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOpts)
                ?: return rawBytes to mimeType
            val out = ByteArrayOutputStream()
            val quality = if (rawBytes.size > 3 * 1024 * 1024) 80 else 86
            bmp.compress(Bitmap.CompressFormat.JPEG, quality, out)
            bmp.recycle()
            val compressed = out.toByteArray()
            if (compressed.size < rawBytes.size) compressed to "image/jpeg" else rawBytes to mimeType
        } catch (_: Exception) {
            rawBytes to mimeType
        }
    }

    private suspend fun publishBoardCastMap(
        context: Context,
        classId: String,
        cleanCode: String,
        castMap: Map<String, Any?>
    ): RestWriteResult = withContext(Dispatchers.IO) {
        if (activeSessionCode != cleanCode) {
            return@withContext RestWriteResult.HttpError(409, "Oturum değişti")
        }
        val expectedEpoch = connectionEpoch
        val now = System.currentTimeMillis()
        lastBoardCast = castMap
        val payload = hashMapOf<String, Any?>(
            "classId" to classId,
            "sessionCode" to cleanCode,
            "status" to "CONNECTED",
            "isConnected" to true,
            "isLiveMirrorActive" to true,
            "isPrivacyMode" to false,
            "activeModule" to "PAYLASIM",
            "activeModuleTitle" to "📡 Paylaşım",
            "currentSubPage" to "PAYLASIM",
            "currentSubPageTitle" to "📡 Paylaşım",
            "boardCast" to castMap,
            "updatedAt" to now
        )
        sessionWriteMutex.withLock {
            if (!isEpochStillActive(cleanCode, expectedEpoch)) {
                return@withLock RestWriteResult.HttpError(409, "Oturum değişti")
            }
            val res = writeSessionStateUnified(
                context = context,
                cleanCode = cleanCode,
                payload = payload,
                merge = true
            )
            if (res is RestWriteResult.Success) {
                sentBoardCastHash = castMap.hashCode()
            }
            res
        }
    }

    /**
     * Seçilen telefon uygulamasının canlı ekran karesini Akıllı Tahta 'Paylaşım' sayfasına aktarır.
     */
    suspend fun publishScreenMirrorFrame(
        context: Context,
        classId: String,
        sessionCode: String,
        castId: String,
        packageName: String,
        appName: String,
        frameSeq: Long,
        base64Jpeg: String
    ) {
        val cleanCode = normalizeCode(sessionCode)
        if (cleanCode.length < 4 || activeSessionCode != cleanCode) return
        val mode = resolveTransportMode(context)
        if (mode != TransportMode.CLOUDFLARE && isQuotaCooldownActive()) return
        val expectedEpoch = connectionEpoch
        val now = System.currentTimeMillis()
        val castMap = hashMapOf<String, Any?>(
            "active" to true,
            "castId" to castId,
            "type" to "SCREEN_MIRROR",
            "title" to appName,
            "packageName" to packageName,
            "mimeType" to "image/jpeg",
            "isPaused" to false,
            "pauseReason" to "",
            "frameSeq" to frameSeq,
            "inlineBase64" to base64Jpeg,
            "updatedAt" to now
        )
        if (mode == TransportMode.CLOUDFLARE && base64Jpeg.isNotEmpty()) {
            ensureCloudflareWebSocket(cleanCode, expectedEpoch)
            val wsEnvelope = JSONObject()
                .put("type", "mirror_frame")
                .put("data", mapToJsonObject(castMap))
            if (sendOverPhoneWebSocket(wsEnvelope, cleanCode)) {
                lastBoardCast = castMap
                sentBoardCastHash = castMap.hashCode()
                return
            }
        }
        publishBoardCastMap(context, classId, cleanCode, castMap)
    }

    /**
     * Uygulamadan çıkıldığında veya ekran kapatıldığında tahtadaki son görüntüyü silmeden dondurur / tekrar açar.
     */
    suspend fun updateScreenMirrorPauseState(
        context: Context,
        classId: String,
        sessionCode: String,
        castId: String,
        appName: String,
        packageName: String,
        isPaused: Boolean,
        pauseReason: String
    ) = withContext(Dispatchers.IO) {
        val cleanCode = normalizeCode(sessionCode)
        if (cleanCode.length < 4 || activeSessionCode != cleanCode) return@withContext
        val expectedEpoch = connectionEpoch
        val now = System.currentTimeMillis()

        val updatedCached = HashMap(lastBoardCast ?: emptyMap()).apply {
            put("active", true)
            put("castId", castId)
            put("type", "SCREEN_MIRROR")
            put("title", appName)
            put("packageName", packageName)
            put("isPaused", isPaused)
            put("pauseReason", pauseReason)
            put("updatedAt", now)
        }
        lastBoardCast = updatedCached

        val partialBoardCast = mapOf(
            "active" to true,
            "castId" to castId,
            "type" to "SCREEN_MIRROR",
            "title" to appName,
            "packageName" to packageName,
            "isPaused" to isPaused,
            "pauseReason" to pauseReason,
            "updatedAt" to now
        )
        val payload = hashMapOf<String, Any?>(
            "status" to "CONNECTED",
            "isConnected" to true,
            "isLiveMirrorActive" to true,
            "isPrivacyMode" to false,
            "activeModule" to "PAYLASIM",
            "activeModuleTitle" to "📡 Paylaşım",
            "currentSubPage" to "PAYLASIM",
            "currentSubPageTitle" to "📡 Paylaşım",
            "boardCast" to partialBoardCast,
            "updatedAt" to now
        )

        try {
            sessionWriteMutex.withLock {
                if (!isEpochStillActive(cleanCode, expectedEpoch)) return@withLock
                val res = writeSessionStateUnified(
                    context = context,
                    cleanCode = cleanCode,
                    payload = payload,
                    merge = true
                )
                if (res is RestWriteResult.Success) {
                    sentBoardCastHash = updatedCached.hashCode()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error updating screen mirror pause state: ${e.message}")
        }
    }

    /**
     * Cihazdan seçilen dosyayı (PDF, görsel, video, ses, sunum, program vb.) Akıllı Tahtada yayınlar.
     * Küçük dosyaları doğrudan, büyük dosyaları 300 KB parçalar halinde Firestore üzerinden iletir.
     */
    suspend fun castFileToBoard(
        context: Context,
        classId: String,
        sessionCode: String,
        uri: Uri,
        onProgress: (Float) -> Unit = {}
    ): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val cleanCode = normalizeCode(sessionCode)
            if (cleanCode.length < 4) {
                return@withContext Result.failure(IllegalStateException("Önce Akıllı Tahta bağlantısı kurun."))
            }

            val fileName = resolveFileName(context, uri)
            val initialMime = resolveMimeType(context, uri, fileName)

            val rawRead = context.contentResolver.openInputStream(uri)?.use { input ->
                val maxPreReadBytes = 25 * 1024 * 1024
                val buffer = ByteArray(16 * 1024)
                val out = ByteArrayOutputStream()
                var total = 0
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    total += read
                    if (total > maxPreReadBytes) {
                        return@withContext Result.failure(
                            IllegalArgumentException("Seçilen dosya çok büyük (en fazla 12 MB desteklenir). Büyük videoları URL ile yayınlayabilirsiniz.")
                        )
                    }
                    out.write(buffer, 0, read)
                }
                out.toByteArray()
            } ?: return@withContext Result.failure(IllegalStateException("Seçilen dosya okunamadı."))

            val (finalBytes, finalMime) = optimizeLargeImageIfPossible(rawRead, initialMime)
            val maxFinalBytes = 12 * 1024 * 1024
            if (finalBytes.size > maxFinalBytes) {
                return@withContext Result.failure(
                    IllegalArgumentException("Dosya boyutu (${finalBytes.size / (1024 * 1024)} MB) 12 MB sınırını aşıyor. Büyük dosyalar için URL sekmesini kullanın.")
                )
            }

            val now = System.currentTimeMillis()
            val castId = "cast_$now"
            val inlineThreshold = 320 * 1024

            if (finalBytes.size <= inlineThreshold) {
                onProgress(0.6f)
                val b64 = Base64.encodeToString(finalBytes, Base64.NO_WRAP)
                val castMap = hashMapOf<String, Any?>(
                    "active" to true,
                    "castId" to castId,
                    "type" to "FILE",
                    "title" to fileName,
                    "fileName" to fileName,
                    "mimeType" to finalMime,
                    "fileSize" to finalBytes.size.toLong(),
                    "inlineBase64" to b64,
                    "totalChunks" to 0,
                    "updatedAt" to now
                )
                val pubRes = publishBoardCastMap(context, classId, cleanCode, castMap)
                if (pubRes is RestWriteResult.HttpError && pubRes.code == 429) {
                    throw IllegalStateException("Firebase günlük ücretsiz yazma kotası doldu (HTTP 429). Kota gece sıfırlandığında tekrar deneyin.")
                }
                onProgress(1f)
            } else {
                val chunkSize = 300 * 1024
                val totalChunks = (finalBytes.size + chunkSize - 1) / chunkSize

                for (i in 0 until totalChunks) {
                    val start = i * chunkSize
                    val end = minOf(finalBytes.size, start + chunkSize)
                    val slice = finalBytes.copyOfRange(start, end)
                    val b64Chunk = Base64.encodeToString(slice, Base64.NO_WRAP)
                    val chunkPayload = mapOf(
                        "castId" to castId,
                        "chunkIndex" to i,
                        "totalChunks" to totalChunks,
                        "data" to b64Chunk,
                        "updatedAt" to now
                    )
                    val chunkRes = if (resolveTransportMode(context) == TransportMode.CLOUDFLARE) {
                        cfUploadChunk(cleanCode, "${castId}_$i", chunkPayload)
                    } else {
                        writeDocumentRestFirst(
                            context = context,
                            docPathSegments = listOf("board_sessions", cleanCode, "cast_chunks", "${castId}_$i"),
                            payload = chunkPayload,
                            merge = false,
                            sdkTimeoutMs = 6000L
                        )
                    }
                    if (chunkRes is RestWriteResult.HttpError && chunkRes.code == 429) {
                        throw IllegalStateException("Firebase günlük ücretsiz yazma kotası doldu (HTTP 429). Kota gece sıfırlandığında tekrar deneyin.")
                    }
                    if (chunkRes !is RestWriteResult.Success) {
                        throw IllegalStateException("Dosya parçası yüklenemedi ($i/$totalChunks).")
                    }
                    onProgress((i + 1).toFloat() / (totalChunks + 1).toFloat())
                }

                val castMap = hashMapOf<String, Any?>(
                    "active" to true,
                    "castId" to castId,
                    "type" to "FILE",
                    "title" to fileName,
                    "fileName" to fileName,
                    "mimeType" to finalMime,
                    "fileSize" to finalBytes.size.toLong(),
                    "inlineBase64" to "",
                    "totalChunks" to totalChunks,
                    "updatedAt" to now
                )
                val pubRes = publishBoardCastMap(context, classId, cleanCode, castMap)
                if (pubRes is RestWriteResult.HttpError && pubRes.code == 429) {
                    throw IllegalStateException("Firebase günlük ücretsiz yazma kotası doldu (HTTP 429). Kota gece sıfırlandığında tekrar deneyin.")
                }
                onProgress(1f)
            }

            Result.success(fileName)
        } catch (e: Exception) {
            Log.e(TAG, "Error casting file to smart board", e)
            Result.failure(e)
        }
    }

    /**
     * Web adresi (URL / YouTube / PDF linki) veya eğitim programı bağlantısını Akıllı Tahtada yayınlar.
     */
    suspend fun castUrlOrProgramToBoard(
        context: Context,
        classId: String,
        sessionCode: String,
        type: String,
        rawUrl: String,
        title: String = ""
    ): Result<String> {
        return try {
            val cleanCode = normalizeCode(sessionCode)
            if (cleanCode.length < 4) {
                return Result.failure(IllegalStateException("Önce Akıllı Tahta bağlantısı kurun."))
            }
            val trimmed = rawUrl.trim()
            if (trimmed.isBlank()) {
                return Result.failure(IllegalArgumentException("Lütfen geçerli bir URL veya program adresi girin."))
            }
            val lower = trimmed.lowercase(Locale.ROOT)
            if (lower.startsWith("javascript:") || lower.startsWith("vbscript:")) {
                return Result.failure(IllegalArgumentException("Güvenlik nedeniyle bu adres desteklenmiyor."))
            }
            val hasScheme = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:").containsMatchIn(trimmed)
            val normalizedUrl = if (hasScheme) trimmed else "https://$trimmed"
            val parsedHost = try {
                Uri.parse(normalizedUrl).host?.removePrefix("www.")
            } catch (_: Exception) { null }
            val effectiveTitle = title.trim().ifBlank { parsedHost ?: normalizedUrl }
            val now = System.currentTimeMillis()

            val castMap = hashMapOf<String, Any?>(
                "active" to true,
                "castId" to "cast_$now",
                "type" to if (type == "PROGRAM") "PROGRAM" else "URL",
                "title" to effectiveTitle,
                "url" to normalizedUrl,
                "updatedAt" to now
            )
            val pubRes = publishBoardCastMap(context, classId, cleanCode, castMap)
            if (pubRes is RestWriteResult.HttpError && pubRes.code == 429) {
                return Result.failure(IllegalStateException("Firebase günlük ücretsiz yazma kotası doldu (HTTP 429). Kota gece sıfırlandığında tekrar deneyin."))
            }
            Result.success(effectiveTitle)
        } catch (e: Exception) {
            Log.e(TAG, "Error casting URL/Program to smart board", e)
            Result.failure(e)
        }
    }

    /**
     * Tahtadaki dosya/URL/program/ekran yayınını durdurur ve tahtayı tekrar BİRİZ uygulama menülerine döndürür.
     */
    fun stopBoardCast(context: Context, classId: String, sessionCode: String?) {
        lastBoardCast = null
        sentBoardCastHash = 0
        val cleanCode = sessionCode?.let { normalizeCode(it) }?.ifEmpty { null } ?: return
        if (activeSessionCode != cleanCode) return
        val expectedEpoch = connectionEpoch
        val now = System.currentTimeMillis()
        val restoreSubPage = lastCurrentSubPage.ifBlank { "SINIF_LISTESI" }
        val restoreSubPageTitle = lastCurrentSubPageTitle.ifBlank { "Sınıf Listesi" }
        val stopMap = mapOf(
            "active" to false,
            "castId" to "",
            "isPaused" to false,
            "inlineBase64" to "",
            "updatedAt" to now
        )

        scope.launch {
            sessionWriteMutex.withLock {
                if (!isEpochStillActive(cleanCode, expectedEpoch)) return@withLock
                try {
                    val payload = mapOf(
                        "boardCast" to stopMap,
                        "activeModule" to restoreSubPage,
                        "activeModuleTitle" to restoreSubPageTitle,
                        "currentSubPage" to restoreSubPage,
                        "currentSubPageTitle" to restoreSubPageTitle,
                        "updatedAt" to now
                    )
                    writeSessionStateUnified(
                        context = context,
                        cleanCode = cleanCode,
                        payload = payload,
                        merge = true
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Error stopping board cast: ${e.message}")
                }
            }
        }
    }

    /**
     * Çıkış yapıldığında veya hesap silindiğinde bellek içi tahta önbelleğini temizler.
     */
    fun resetLocalState(context: Context? = null) {
        connectionEpoch += 1L
        activeSessionCode = null
        activeConnectionId = null
        activeTransportMode = TransportMode.NONE
        closePhoneWebSocket()
        SmartBoardRtcManager.stopSession()
        quotaCooldownUntilMs = 0L
        clearDeltaSyncCache()
        lastClassId = ""
        lastCurrentSubPage = "SINIF_LISTESI"
        lastCurrentSubPageTitle = "Sınıf Listesi"
        lastActiveDay = "PZT"
        lastStudents = emptyList()
        lastSchedule = emptyList()
        lastScheduleConfig = ScheduleConfig()
        lastSeatingPlanState = null
        lastGroupMakerState = null
        lastBoardCast = null
        if (context != null) {
            try {
                saveConnectionState(context, null, false)
            } catch (_: Exception) {}
        }
    }

    /**
     * Hesap silinirken kullanıcıya/sınıflarına ait tüm Akıllı Tahta oturumlarını
     * ve `cast_chunks` alt koleksiyonlarını Firestore'dan temizler.
     */
    suspend fun deleteUserSmartBoardTraces(
        context: Context,
        ownedClassIds: Set<String>,
        teacherEmail: String,
        activeSessionCode: String?
    ) {
        val mainDb = getMainFirestore(context)
        val defaultDb = try {
            FirebaseFirestore.getInstance()
        } catch (_: Exception) {
            null
        }
        val dbs = listOfNotNull(mainDb, defaultDb?.takeIf { it != mainDb })

        val sessionIdsToDelete = mutableSetOf<String>()
        activeSessionCode?.let { normalizeCode(it) }?.takeIf { it.isNotBlank() }?.let { sessionIdsToDelete.add(it) }
        try {
            getSavedSessionCode(context)?.let { normalizeCode(it) }?.takeIf { it.isNotBlank() }?.let { sessionIdsToDelete.add(it) }
        } catch (_: Exception) {}

        val cleanEmail = teacherEmail.trim()
        for (db in dbs) {
            try {
                ownedClassIds.filter { it.isNotBlank() && it != "default" }.forEach { cid ->
                    try {
                        val byClass = db.collection("board_sessions")
                            .whereEqualTo("classId", cid)
                            .get()
                            .await()
                        byClass.documents.forEach { sessionIdsToDelete.add(it.id) }
                    } catch (_: Exception) {}
                }
                if (cleanEmail.isNotBlank()) {
                    try {
                        val byEmail = db.collection("board_sessions")
                            .whereEqualTo("teacherEmail", cleanEmail)
                            .get()
                            .await()
                        byEmail.documents.forEach { sessionIdsToDelete.add(it.id) }
                    } catch (_: Exception) {}
                }

                for (sid in sessionIdsToDelete) {
                    try {
                        val sessionRef = db.collection("board_sessions").document(sid)
                        val chunksSnap = sessionRef.collection("cast_chunks").get().await()
                        chunksSnap.documents.chunked(400).forEach { chunk ->
                            val batch = db.batch()
                            chunk.forEach { batch.delete(it.reference) }
                            batch.commit().await()
                        }
                        sessionRef.delete().await()
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }
        resetLocalState(context)
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
                <div class="code" id="code">--------</div>
              </div>
              <script>
                const chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
                let c = "";
                for (let i = 0; i < 8; i++) c += chars.charAt(Math.floor(Math.random() * chars.length));
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
