package com.example.util

import android.content.Context
import android.util.Log
import org.json.JSONObject
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executors

/**
 * Akıllı Tahta ile telefon arasında doğrudan cihazdan cihaza (P2P WebRTC DataChannel)
 * düşük gecikmeli ekran yansıtma kanalı kurar.
 * Okul ağı (MEB/Fatih vb.) P2P'yi engellerse otomatik olarak Cloudflare aktarımına düşer.
 */
object SmartBoardRtcManager {
    private const val TAG = "SmartBoardRtcManager"
    private const val DATA_CHANNEL_LABEL = "biriz-mirror"
    private const val MAX_CHUNK_BYTES = 28 * 1024
    private const val MAX_BUFFERED_AMOUNT = 512 * 1024L

    private val rtcExecutor = Executors.newSingleThreadExecutor()
    private val lock = Any()

    @Volatile
    private var factoryInitialized = false
    private var peerConnectionFactory: PeerConnectionFactory? = null

    private var peerConnection: PeerConnection? = null
    private var dataChannel: DataChannel? = null
    private var activeSessionCode: String? = null
    private var activeConnectionId: String? = null
    private var signalCallback: ((JSONObject) -> Unit)? = null
    private var remoteDescriptionSet = false
    private val pendingIceCandidates = mutableListOf<IceCandidate>()

    @Volatile
    private var channelOpen = false

    @Volatile
    private var connectionStateLabel = "KAPALI"

    fun isChannelReady(): Boolean {
        val dc = dataChannel ?: return false
        return channelOpen && dc.state() == DataChannel.State.OPEN && dc.bufferedAmount() < MAX_BUFFERED_AMOUNT
    }

    fun getConnectionStateLabel(): String = connectionStateLabel

    private fun ensureFactory(appContext: Context): PeerConnectionFactory? {
        synchronized(lock) {
            if (!factoryInitialized) {
                try {
                    PeerConnectionFactory.initialize(
                        PeerConnectionFactory.InitializationOptions.builder(appContext)
                            .setEnableInternalTracer(false)
                            .createInitializationOptions()
                    )
                    factoryInitialized = true
                } catch (e: Exception) {
                    Log.w(TAG, "PeerConnectionFactory init warning: ${e.message}")
                }
            }
            if (peerConnectionFactory == null && factoryInitialized) {
                try {
                    peerConnectionFactory = PeerConnectionFactory.builder()
                        .createPeerConnectionFactory()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed creating PeerConnectionFactory: ${e.message}")
                }
            }
            return peerConnectionFactory
        }
    }

    fun startSession(
        context: Context,
        sessionCode: String,
        connectionId: String,
        onSignal: (JSONObject) -> Unit
    ) {
        val appCtx = context.applicationContext ?: context
        rtcExecutor.execute {
            synchronized(lock) {
                if (activeSessionCode == sessionCode &&
                    activeConnectionId == connectionId &&
                    peerConnection != null &&
                    channelOpen
                ) {
                    return@execute
                }
                closeInternalLocked()

                val factory = ensureFactory(appCtx) ?: return@execute
                activeSessionCode = sessionCode
                activeConnectionId = connectionId
                signalCallback = onSignal
                remoteDescriptionSet = false
                pendingIceCandidates.clear()
                connectionStateLabel = "BAGLANIYOR"

                val iceServers = listOf(
                    PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
                    PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
                )
                val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
                    sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
                    continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_ONCE
                }

                val pc = factory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
                    override fun onSignalingChange(newState: PeerConnection.SignalingState?) {}

                    override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState?) {
                        Log.d(TAG, "ICE connection state: $newState")
                        when (newState) {
                            PeerConnection.IceConnectionState.CONNECTED,
                            PeerConnection.IceConnectionState.COMPLETED -> {
                                connectionStateLabel = "P2P_BAGLI"
                            }
                            PeerConnection.IceConnectionState.FAILED,
                            PeerConnection.IceConnectionState.DISCONNECTED,
                            PeerConnection.IceConnectionState.CLOSED -> {
                                channelOpen = false
                                connectionStateLabel = "YEDEK_MOD"
                            }
                            else -> {}
                        }
                    }

                    override fun onIceConnectionReceivingChange(receiving: Boolean) {}
                    override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState?) {}

                    override fun onIceCandidate(candidate: IceCandidate?) {
                        candidate ?: return
                        val cb = signalCallback ?: return
                        val conn = activeConnectionId ?: return
                        val msg = JSONObject()
                            .put("type", "rtc_ice")
                            .put("role", "phone")
                            .put("connectionId", conn)
                            .put("candidate", JSONObject()
                                .put("candidate", candidate.sdp)
                                .put("sdpMid", candidate.sdpMid ?: "")
                                .put("sdpMLineIndex", candidate.sdpMLineIndex)
                            )
                        try {
                            cb(msg)
                        } catch (_: Exception) {}
                    }

                    override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
                    override fun onAddStream(stream: MediaStream?) {}
                    override fun onRemoveStream(stream: MediaStream?) {}
                    override fun onDataChannel(dc: DataChannel?) {}
                    override fun onRenegotiationNeeded() {}
                    override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {}
                }) ?: return@execute

                peerConnection = pc

                val dcInit = DataChannel.Init().apply {
                    ordered = true
                }
                val dc = pc.createDataChannel(DATA_CHANNEL_LABEL, dcInit)
                dataChannel = dc
                dc?.registerObserver(object : DataChannel.Observer {
                    override fun onBufferedAmountChange(previousAmount: Long) {}
                    override fun onStateChange() {
                        val st = dc.state()
                        channelOpen = (st == DataChannel.State.OPEN)
                        if (channelOpen) {
                            connectionStateLabel = "P2P_ACIK"
                        } else if (st == DataChannel.State.CLOSED || st == DataChannel.State.CLOSING) {
                            connectionStateLabel = "KAPALI"
                        }
                        Log.d(TAG, "DataChannel state: $st")
                    }

                    override fun onMessage(buffer: DataChannel.Buffer?) {}
                })

                val constraints = MediaConstraints()
                pc.createOffer(object : SdpObserver {
                    override fun onCreateSuccess(sdp: SessionDescription?) {
                        sdp ?: return
                        rtcExecutor.execute {
                            synchronized(lock) {
                                val currentPc = peerConnection ?: return@execute
                                currentPc.setLocalDescription(object : SdpObserver {
                                    override fun onCreateSuccess(p0: SessionDescription?) {}
                                    override fun onSetSuccess() {
                                        val cb = signalCallback ?: return
                                        val conn = activeConnectionId ?: return
                                        val offerMsg = JSONObject()
                                            .put("type", "rtc_offer")
                                            .put("connectionId", conn)
                                            .put("sdp", sdp.description)
                                        try {
                                            cb(offerMsg)
                                        } catch (_: Exception) {}
                                    }

                                    override fun onCreateFailure(err: String?) {}
                                    override fun onSetFailure(err: String?) {
                                        Log.w(TAG, "setLocalDescription failed: $err")
                                    }
                                }, sdp)
                            }
                        }
                    }

                    override fun onSetSuccess() {}
                    override fun onCreateFailure(err: String?) {
                        Log.w(TAG, "createOffer failed: $err")
                    }

                    override fun onSetFailure(err: String?) {}
                }, constraints)
            }
        }
    }

    fun onRemoteSignal(msg: JSONObject) {
        val type = msg.optString("type", "")
        val msgConnId = msg.optString("connectionId", "")
        rtcExecutor.execute {
            synchronized(lock) {
                val pc = peerConnection ?: return@execute
                val curConn = activeConnectionId
                if (!curConn.isNullOrBlank() && msgConnId.isNotBlank() && msgConnId != curConn) {
                    return@execute
                }

                when (type) {
                    "rtc_answer" -> {
                        val sdpDesc = msg.optString("sdp", "")
                        if (sdpDesc.isBlank()) return@execute
                        val remoteSdp = SessionDescription(SessionDescription.Type.ANSWER, sdpDesc)
                        pc.setRemoteDescription(object : SdpObserver {
                            override fun onCreateSuccess(p0: SessionDescription?) {}
                            override fun onSetSuccess() {
                                rtcExecutor.execute {
                                    synchronized(lock) {
                                        remoteDescriptionSet = true
                                        val targetPc = peerConnection ?: return@execute
                                        for (cand in pendingIceCandidates) {
                                            try {
                                                targetPc.addIceCandidate(cand)
                                            } catch (_: Exception) {}
                                        }
                                        pendingIceCandidates.clear()
                                    }
                                }
                            }

                            override fun onCreateFailure(err: String?) {}
                            override fun onSetFailure(err: String?) {
                                Log.w(TAG, "setRemoteDescription failed: $err")
                            }
                        }, remoteSdp)
                    }

                    "rtc_ice" -> {
                        if (msg.optString("role", "") != "board") return@execute
                        val candObj = msg.optJSONObject("candidate") ?: return@execute
                        val sdp = candObj.optString("candidate", "")
                        if (sdp.isBlank()) return@execute
                        val sdpMid = candObj.optString("sdpMid", "")
                        val mLineIndex = candObj.optInt("sdpMLineIndex", 0)
                        val ice = IceCandidate(sdpMid, mLineIndex, sdp)
                        if (remoteDescriptionSet) {
                            try {
                                pc.addIceCandidate(ice)
                            } catch (_: Exception) {}
                        } else {
                            pendingIceCandidates.add(ice)
                        }
                    }
                }
            }
        }
    }

    fun sendFrameBytes(frameSeq: Long, jpegBytes: ByteArray): Boolean {
        val dc = dataChannel ?: return false
        if (!channelOpen || dc.state() != DataChannel.State.OPEN) return false
        if (dc.bufferedAmount() >= MAX_BUFFERED_AMOUNT) return false

        val totalLen = jpegBytes.size
        val totalChunks = ((totalLen + MAX_CHUNK_BYTES - 1) / MAX_CHUNK_BYTES).coerceAtLeast(1)
        val seqInt = (frameSeq and 0x7FFFFFFFL).toInt()

        return try {
            for (chunkIdx in 0 until totalChunks) {
                val start = chunkIdx * MAX_CHUNK_BYTES
                val end = minOf(totalLen, start + MAX_CHUNK_BYTES)
                val sliceLen = end - start
                val packet = ByteBuffer.allocate(12 + sliceLen).order(ByteOrder.BIG_ENDIAN)
                packet.put('B'.code.toByte())
                packet.put('R'.code.toByte())
                packet.put('Z'.code.toByte())
                packet.put('F'.code.toByte())
                packet.putInt(seqInt)
                packet.putShort(chunkIdx.toShort())
                packet.putShort(totalChunks.toShort())
                packet.put(jpegBytes, start, sliceLen)
                packet.flip()
                if (!dc.send(DataChannel.Buffer(packet, true))) {
                    return false
                }
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "sendFrameBytes failed: ${e.message}")
            false
        }
    }

    fun sendControlJson(json: JSONObject): Boolean {
        val dc = dataChannel ?: return false
        if (!channelOpen || dc.state() != DataChannel.State.OPEN) return false
        return try {
            val bytes = json.toString().toByteArray(Charsets.UTF_8)
            dc.send(DataChannel.Buffer(ByteBuffer.wrap(bytes), false))
        } catch (_: Exception) {
            false
        }
    }

    fun stopSession() {
        rtcExecutor.execute {
            synchronized(lock) {
                closeInternalLocked()
            }
        }
    }

    private fun closeInternalLocked() {
        channelOpen = false
        connectionStateLabel = "KAPALI"
        remoteDescriptionSet = false
        pendingIceCandidates.clear()
        signalCallback = null
        activeSessionCode = null
        activeConnectionId = null
        try {
            dataChannel?.unregisterObserver()
        } catch (_: Exception) {}
        try {
            dataChannel?.close()
        } catch (_: Exception) {}
        try {
            dataChannel?.dispose()
        } catch (_: Exception) {}
        dataChannel = null
        try {
            peerConnection?.close()
        } catch (_: Exception) {}
        try {
            peerConnection?.dispose()
        } catch (_: Exception) {}
        peerConnection = null
    }
}
