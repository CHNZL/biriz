package com.example.util

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class OnlineParentPresence(
    val email: String,
    val name: String,
    val studentIds: List<String>,
    val lastActiveMillis: Long
)

data class OfflineParentPresence(
    val email: String,
    val studentIds: List<String>,
    val lastActiveMillis: Long
)

data class ClassroomPresenceSnapshot(
    val classId: String,
    val isConnected: Boolean,
    val serverTs: Long,
    val receivedAtLocalMs: Long,
    val teacherOnline: Boolean,
    val teacherLastActiveMillis: Long?,
    val onlineParents: List<OnlineParentPresence>,
    val offlineParents: List<OfflineParentPresence>
)

/**
 * Sıfır Firestore yazma/okuma kotası kullanan gerçek zamanlı WebSocket canlı bağlantı yöneticisi.
 * Mevcut Cloudflare Workers + Durable Objects altyapısı (/tahta/api/presence/ws) üzerinden çalışır.
 * Sunucu henüz yeni rotayı barındırmıyorsa veya geçici ağ kesintisi varsa sessizce geri çekilir;
 * uygulama otomatik olarak optimize edilmiş Firestore fallback katmanını kullanmaya devam eder.
 */
object PresenceSocketManager {
    private const val TAG = "PresenceSocketManager"
    private const val PRESENCE_WS_BASE = "wss://cihanogretmen.com/tahta/api/presence/ws"

    private data class PresenceTarget(
        val classId: String,
        val role: String,
        val email: String,
        val name: String,
        val studentIds: List<String>
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .pingInterval(20, TimeUnit.SECONDS)
            .build()
    }

    private val _snapshot = MutableStateFlow<ClassroomPresenceSnapshot?>(null)
    val snapshot: StateFlow<ClassroomPresenceSnapshot?> = _snapshot.asStateFlow()

    @Volatile
    private var currentTarget: PresenceTarget? = null

    @Volatile
    private var activeSocket: WebSocket? = null

    @Volatile
    private var socketOpened: Boolean = false

    @Volatile
    private var shouldStayConnected: Boolean = false

    @Volatile
    private var epoch: Long = 0L

    @Volatile
    private var backoffAttempt: Int = 0

    @Volatile
    private var endpointUnsupportedUntilMs: Long = 0L

    private var pingJob: Job? = null
    private var reconnectJob: Job? = null

    fun isLiveConnected(classId: String?): Boolean {
        val clean = cleanClassId(classId)
        if (clean.length < 2) return false
        val snap = _snapshot.value ?: return false
        return snap.isConnected && snap.classId == clean
    }

    fun connect(
        classId: String,
        role: String,
        email: String,
        name: String,
        studentIds: List<String>
    ) {
        val cleanId = cleanClassId(classId)
        if (cleanId.length < 2 || cleanId == "default") return

        val normalizedRole = if (role.equals("teacher", ignoreCase = true)) "teacher" else "parent"
        val normalizedEmail = email.trim().lowercase()
        val cleanName = name.trim()
        val normalizedStudentIds = studentIds.map { it.trim() }.filter { it.isNotBlank() }.distinct().sorted()
        val nextTarget = PresenceTarget(
            classId = cleanId,
            role = normalizedRole,
            email = normalizedEmail,
            name = cleanName,
            studentIds = normalizedStudentIds
        )

        synchronized(lock) {
            shouldStayConnected = true
            val prevTarget = currentTarget
            val sameBaseSession = prevTarget != null &&
                prevTarget.classId == nextTarget.classId &&
                prevTarget.role == nextTarget.role &&
                prevTarget.email == nextTarget.email

            if (sameBaseSession && activeSocket != null) {
                currentTarget = nextTarget
                if (prevTarget.studentIds != nextTarget.studentIds || prevTarget.name != nextTarget.name) {
                    if (socketOpened) {
                        sendPresenceUpdateLocked(nextTarget)
                    }
                }
                return
            }

            currentTarget = nextTarget
            backoffAttempt = 0
            openSocketLocked(nextTarget)
        }
    }

    fun disconnect() {
        val socketToClose: WebSocket?
        synchronized(lock) {
            shouldStayConnected = false
            epoch++
            currentTarget = null
            socketOpened = false
            pingJob?.cancel()
            pingJob = null
            reconnectJob?.cancel()
            reconnectJob = null
            socketToClose = activeSocket
            activeSocket = null
        }
        _snapshot.value = null
        try {
            socketToClose?.close(1000, "app_backgrounded")
        } catch (_: Exception) {}
    }

    private fun openSocketLocked(target: PresenceTarget) {
        epoch++
        val myEpoch = epoch
        socketOpened = false
        pingJob?.cancel()
        pingJob = null
        reconnectJob?.cancel()
        reconnectJob = null

        val oldSocket = activeSocket
        activeSocket = null
        try {
            oldSocket?.close(1000, "reconnecting")
        } catch (_: Exception) {}

        val now = System.currentTimeMillis()
        if (now < endpointUnsupportedUntilMs) {
            scheduleReconnectLocked(myEpoch, customDelayMs = (endpointUnsupportedUntilMs - now).coerceAtLeast(15_000L))
            return
        }

        val url = Uri.parse(PRESENCE_WS_BASE).buildUpon()
            .appendQueryParameter("classId", target.classId)
            .appendQueryParameter("role", target.role)
            .appendQueryParameter("email", target.email)
            .appendQueryParameter("name", target.name)
            .appendQueryParameter("studentIds", target.studentIds.joinToString(","))
            .build()
            .toString()

        val request = Request.Builder().url(url).build()
        activeSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                synchronized(lock) {
                    if (myEpoch != epoch || !shouldStayConnected || activeSocket !== webSocket) {
                        try {
                            webSocket.close(1000, "stale")
                        } catch (_: Exception) {}
                        return
                    }
                    socketOpened = true
                    backoffAttempt = 0
                    endpointUnsupportedUntilMs = 0L
                    startPingLoopLocked(myEpoch, webSocket)
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (myEpoch != epoch) return
                handleIncomingMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                try {
                    webSocket.close(code, reason)
                } catch (_: Exception) {}
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                handleSocketTerminated(myEpoch, webSocket, httpCode = null, normalClose = (code == 1000 && reason == "app_backgrounded"))
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val httpCode = response?.code
                Log.d(TAG, "Presence WS failure (http=$httpCode): ${t.message}")
                handleSocketTerminated(myEpoch, webSocket, httpCode = httpCode, normalClose = false)
            }
        })
    }

    private fun handleSocketTerminated(
        myEpoch: Long,
        webSocket: WebSocket,
        httpCode: Int?,
        normalClose: Boolean
    ) {
        synchronized(lock) {
            if (myEpoch != epoch || activeSocket !== webSocket) return
            activeSocket = null
            socketOpened = false
            pingJob?.cancel()
            pingJob = null

            val existing = _snapshot.value
            if (existing != null) {
                _snapshot.value = existing.copy(isConnected = false)
            }

            if (!shouldStayConnected || normalClose) return

            if (httpCode == 400 || httpCode == 404 || httpCode == 426) {
                // Sunucu henüz /presence/ws uç noktasını yayına almadıysa 60 sn bekle (Firestore fallback aktif kalır)
                endpointUnsupportedUntilMs = System.currentTimeMillis() + 60_000L
                scheduleReconnectLocked(myEpoch, customDelayMs = 60_000L)
                return
            }

            val delayMs = when (backoffAttempt) {
                0 -> 2_000L
                1 -> 4_000L
                2 -> 8_000L
                3 -> 15_000L
                else -> 30_000L
            }
            backoffAttempt++
            scheduleReconnectLocked(myEpoch, customDelayMs = delayMs)
        }
    }

    private fun scheduleReconnectLocked(myEpoch: Long, customDelayMs: Long) {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(customDelayMs)
            synchronized(lock) {
                if (!shouldStayConnected || myEpoch != epoch) return@launch
                val target = currentTarget ?: return@launch
                openSocketLocked(target)
            }
        }
    }

    private fun startPingLoopLocked(myEpoch: Long, webSocket: WebSocket) {
        pingJob?.cancel()
        pingJob = scope.launch {
            while (isActive) {
                delay(25_000L)
                val stillValid = synchronized(lock) {
                    shouldStayConnected && myEpoch == epoch && activeSocket === webSocket && socketOpened
                }
                if (!stillValid) break
                try {
                    val sent = webSocket.send("""{"type":"ping","ts":${System.currentTimeMillis()}}""")
                    if (!sent) break
                } catch (_: Exception) {
                    break
                }
            }
        }
    }

    private fun sendPresenceUpdateLocked(target: PresenceTarget) {
        val ws = activeSocket ?: return
        try {
            val payload = JSONObject().apply {
                put("type", "presence_update")
                put("name", target.name)
                put("studentIds", JSONArray(target.studentIds))
            }.toString()
            ws.send(payload)
        } catch (_: Exception) {}
    }

    private fun handleIncomingMessage(raw: String) {
        try {
            val json = JSONObject(raw)
            val type = json.optString("type")
            if (type != "presence_snapshot") return

            val classId = json.optString("classId")
            val serverTs = json.optLong("serverTs", System.currentTimeMillis())
            val teacherOnline = json.optBoolean("teacherOnline", false)
            val teacherLastRaw = json.optLong("teacherLastActiveMillis", 0L)
            val teacherLastActiveMillis = if (teacherLastRaw > 0L) teacherLastRaw else null

            val onlineParents = mutableListOf<OnlineParentPresence>()
            val onlineArr = json.optJSONArray("onlineParents")
            if (onlineArr != null) {
                for (i in 0 until onlineArr.length()) {
                    val item = onlineArr.optJSONObject(i) ?: continue
                    val email = item.optString("email").trim().lowercase()
                    if (email.isBlank()) continue
                    val name = item.optString("name", "Öğrenci Velisi")
                    val lastActive = item.optLong("lastActiveMillis", serverTs)
                    val sIds = parseStringArray(item.optJSONArray("studentIds"))
                    onlineParents.add(
                        OnlineParentPresence(
                            email = email,
                            name = name,
                            studentIds = sIds,
                            lastActiveMillis = lastActive
                        )
                    )
                }
            }

            val offlineParents = mutableListOf<OfflineParentPresence>()
            val offlineArr = json.optJSONArray("recentOfflineParents")
                ?: json.optJSONArray("offlineParents")
            if (offlineArr != null) {
                for (i in 0 until offlineArr.length()) {
                    val item = offlineArr.optJSONObject(i) ?: continue
                    val email = item.optString("email").trim().lowercase()
                    if (email.isBlank()) continue
                    val lastActive = item.optLong("lastActiveMillis", serverTs)
                    val sIds = parseStringArray(item.optJSONArray("studentIds"))
                    offlineParents.add(
                        OfflineParentPresence(
                            email = email,
                            studentIds = sIds,
                            lastActiveMillis = lastActive
                        )
                    )
                }
            }

            _snapshot.value = ClassroomPresenceSnapshot(
                classId = classId,
                isConnected = true,
                serverTs = serverTs,
                receivedAtLocalMs = System.currentTimeMillis(),
                teacherOnline = teacherOnline,
                teacherLastActiveMillis = teacherLastActiveMillis,
                onlineParents = onlineParents,
                offlineParents = offlineParents
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse presence snapshot", e)
        }
    }

    private fun parseStringArray(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        val list = ArrayList<String>(arr.length())
        for (i in 0 until arr.length()) {
            val s = arr.optString(i).trim()
            if (s.isNotBlank()) list.add(s)
        }
        return list
    }

    private fun cleanClassId(raw: String?): String {
        return raw.orEmpty().trim().replace(Regex("[^A-Za-z0-9_-]"), "")
    }
}
