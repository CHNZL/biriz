package com.example.util

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Base64
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

data class ScreenMirrorStatus(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val isStoppedKeepingFrozenFrame: Boolean = false,
    val pauseReason: String = "",
    val targetPackageName: String = "",
    val targetAppName: String = "",
    val castId: String = "",
    val sessionCode: String = "",
    val classId: String = "",
    val framesSent: Long = 0L
)

class SmartBoardScreenCastService : Service() {

    companion object {
        private const val TAG = "SmartBoardScreenCast"
        private const val CHANNEL_ID = "biriz_smart_board_cast_channel"
        private const val NOTIFICATION_ID = 4401

        const val ACTION_START = "com.example.action.SMART_BOARD_MIRROR_START"
        const val ACTION_STOP = "com.example.action.SMART_BOARD_MIRROR_STOP"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val EXTRA_SESSION_CODE = "extra_session_code"
        const val EXTRA_CLASS_ID = "extra_class_id"
        const val EXTRA_TARGET_PACKAGE = "extra_target_package"
        const val EXTRA_TARGET_APP_NAME = "extra_target_app_name"
        const val EXTRA_SKIP_FIRESTORE_STOP = "extra_skip_firestore_stop"

        @Volatile
        private var forceAbortRequested: Boolean = false

        private val _status = MutableStateFlow(ScreenMirrorStatus())
        val status: StateFlow<ScreenMirrorStatus> = _status.asStateFlow()

        fun stopCast(
            context: Context,
            classId: String,
            sessionCode: String?,
            skipFirestoreStop: Boolean = false
        ) {
            if (skipFirestoreStop) {
                forceAbortRequested = true
            }
            val current = _status.value
            if (current.isRunning) {
                val stopIntent = Intent(context, SmartBoardScreenCastService::class.java).apply {
                    action = ACTION_STOP
                    putExtra(EXTRA_CLASS_ID, classId)
                    putExtra(EXTRA_SESSION_CODE, sessionCode ?: "")
                    putExtra(EXTRA_SKIP_FIRESTORE_STOP, skipFirestoreStop)
                }
                try {
                    context.startService(stopIntent)
                } catch (_: Exception) {
                    _status.value = ScreenMirrorStatus()
                    if (!skipFirestoreStop) {
                        SmartBoardHelper.stopBoardCast(context, classId, sessionCode)
                    }
                }
                if (skipFirestoreStop) {
                    _status.value = ScreenMirrorStatus()
                }
            } else {
                _status.value = ScreenMirrorStatus()
                if (!skipFirestoreStop) {
                    SmartBoardHelper.stopBoardCast(context, classId, sessionCode)
                }
            }
        }

        fun stopServiceOnly(context: Context) {
            forceAbortRequested = true
            try {
                context.stopService(Intent(context, SmartBoardScreenCastService::class.java))
            } catch (_: Exception) {}
            _status.value = ScreenMirrorStatus()
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var captureJob: Job? = null

    private var sessionCode: String = ""
    private var classId: String = ""
    private var targetPackageName: String = ""
    private var targetAppName: String = ""
    private var castId: String = ""

    private var captureWidth: Int = 540
    private var captureHeight: Int = 960
    private var captureDensity: Int = 240

    @Volatile
    private var isCapturedContentVisible: Boolean = true

    @Volatile
    private var isExplicitStopping: Boolean = false

    @Volatile
    private var lastFramePixelHash: Long = 0L

    @Volatile
    private var framesSentCount: Long = 0L

    @Volatile
    private var lastUsagePollMs: Long = 0L

    @Volatile
    private var lastDetectedForegroundPkg: String? = null

    @Volatile
    private var lastPublishedPaused: Boolean? = null

    private var screenReceiverRegistered = false
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    syncFreezeState(
                        paused = true,
                        reason = "Telefon ekranı kapandı — $targetAppName son görüntüsü tahtada sabitlendi"
                    )
                }
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                    // Kilit açılınca kontrol döngüsü tekrar hedef uygulamada olup olmadığını doğrulayacak
                }
            }
        }
    }

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            Log.d(TAG, "MediaProjection onStop triggered (explicit=$isExplicitStopping, forceAbort=$forceAbortRequested)")
            if (!isExplicitStopping && !forceAbortRequested) {
                val activeClass = classId
                val activeCode = sessionCode
                val activeCastId = castId
                val activeAppName = targetAppName
                val activePkg = targetPackageName

                releaseProjectionOnly()
                _status.value = _status.value.copy(
                    isRunning = false,
                    isPaused = true,
                    isStoppedKeepingFrozenFrame = true,
                    pauseReason = "Ekran kapandı veya sistem yansıtmayı durdurdu — son görüntü tahtada sabit"
                )
                serviceScope.launch {
                    if (!isExplicitStopping && !forceAbortRequested) {
                        SmartBoardHelper.updateScreenMirrorPauseState(
                            context = applicationContext,
                            classId = activeClass,
                            sessionCode = activeCode,
                            castId = activeCastId,
                            appName = activeAppName,
                            packageName = activePkg,
                            isPaused = true,
                            pauseReason = "Ekran kapatıldığı için $activeAppName son görüntüsü tahtada sabitlendi"
                        )
                    }
                }
                stopForegroundCompat()
                stopSelf()
            }
        }

        override fun onCapturedContentVisibilityChanged(isVisible: Boolean) {
            isCapturedContentVisible = isVisible
            if (!isVisible) {
                syncFreezeState(
                    paused = true,
                    reason = "$targetAppName şu an ekranda değil — son görüntü tahtada sabitlendi"
                )
            }
        }

        override fun onCapturedContentResize(width: Int, height: Int) {
            if (width <= 0 || height <= 0) return
            val (scaledW, scaledH) = scaleDimensions(width, height, 520)
            if (scaledW != captureWidth || scaledH != captureHeight) {
                captureWidth = scaledW
                captureHeight = scaledH
                mainHandler.post { recreateVirtualDisplaySurface() }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        registerScreenReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                isExplicitStopping = true
                val skipFirestore = intent.getBooleanExtra(EXTRA_SKIP_FIRESTORE_STOP, false) || forceAbortRequested
                val stopClassId = intent.getStringExtra(EXTRA_CLASS_ID).orEmpty().ifBlank { classId }
                val stopCode = intent.getStringExtra(EXTRA_SESSION_CODE).orEmpty().ifBlank { sessionCode }
                releaseAllAndResetStatus()
                if (!skipFirestore && stopCode.isNotBlank()) {
                    SmartBoardHelper.stopBoardCast(applicationContext, stopClassId, stopCode)
                }
                stopForegroundCompat()
                stopSelf()
                return START_NOT_STICKY
            }

            ACTION_START -> {
                forceAbortRequested = false
                val resCode = intent.getIntExtra(EXTRA_RESULT_CODE, Int.MIN_VALUE)
                val resData: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }
                sessionCode = intent.getStringExtra(EXTRA_SESSION_CODE).orEmpty()
                classId = intent.getStringExtra(EXTRA_CLASS_ID).orEmpty()
                targetPackageName = intent.getStringExtra(EXTRA_TARGET_PACKAGE).orEmpty()
                targetAppName = intent.getStringExtra(EXTRA_TARGET_APP_NAME).orEmpty().ifBlank { "Seçilen Uygulama" }

                if (resCode == Int.MIN_VALUE || resData == null || sessionCode.isBlank()) {
                    stopSelf()
                    return START_NOT_STICKY
                }

                startForegroundProjectionNotification()
                startProjectionSession(resCode, resData)
                return START_NOT_STICKY
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundProjectionNotification() {
        val notification = buildNotification(isPaused = false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startProjectionSession(resultCode: Int, resultData: Intent) {
        releaseProjectionOnly()
        isExplicitStopping = false
        isCapturedContentVisible = true
        lastFramePixelHash = 0L
        framesSentCount = 0L
        lastPublishedPaused = null
        lastDetectedForegroundPkg = null
        lastUsagePollMs = System.currentTimeMillis() - 10_000L
        castId = "mirror_${System.currentTimeMillis()}"

        val pm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val projection = try {
            pm.getMediaProjection(resultCode, resultData)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to obtain MediaProjection: ${e.message}", e)
            null
        }
        if (projection == null) {
            _status.value = ScreenMirrorStatus()
            stopForegroundCompat()
            stopSelf()
            return
        }

        mediaProjection = projection
        projection.registerCallback(projectionCallback, mainHandler)

        val (rawW, rawH, density) = computeDisplaySizeAndDensity()
        val (scaledW, scaledH) = scaleDimensions(rawW, rawH, 520)
        captureWidth = scaledW
        captureHeight = scaledH
        captureDensity = density

        recreateVirtualDisplaySurface()

        _status.value = ScreenMirrorStatus(
            isRunning = true,
            isPaused = false,
            isStoppedKeepingFrozenFrame = false,
            pauseReason = "",
            targetPackageName = targetPackageName,
            targetAppName = targetAppName,
            castId = castId,
            sessionCode = sessionCode,
            classId = classId,
            framesSent = 0L
        )

        // Seçilen programı ekranda başlat
        if (targetPackageName.isNotBlank()) {
            mainHandler.postDelayed({
                InstalledAppsHelper.launchAppByPackage(applicationContext, targetPackageName)
            }, 180L)
        }

        startCaptureLoop()
    }

    private fun recreateVirtualDisplaySurface() {
        val projection = mediaProjection ?: return
        try {
            imageReader?.close()
        } catch (_: Exception) {}

        val reader = ImageReader.newInstance(captureWidth, captureHeight, PixelFormat.RGBA_8888, 2)
        imageReader = reader

        if (virtualDisplay == null) {
            virtualDisplay = projection.createVirtualDisplay(
                "BirizSmartBoardMirror",
                captureWidth,
                captureHeight,
                captureDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                mainHandler
            )
        } else {
            virtualDisplay?.resize(captureWidth, captureHeight, captureDensity)
            virtualDisplay?.surface = reader.surface
        }
    }

    private fun startCaptureLoop() {
        captureJob?.cancel()
        captureJob = serviceScope.launch {
            // İlk açılışta seçilen uygulamanın ekrana gelmesi için kısa bekleme
            delay(950L)

            while (isActive && mediaProjection != null && !isExplicitStopping && !forceAbortRequested) {
                try {
                    checkDisplayOrientationChange()
                    val eval = evaluateShouldMirrorNow()
                    if (!eval.allowed) {
                        syncFreezeState(paused = true, reason = eval.reason)
                        delay(350L)
                        continue
                    }

                    val candidateBitmap = captureLatestBitmap()
                    if (candidateBitmap == null) {
                        delay(320L)
                        continue
                    }

                    val pixelHash = computeQuickHash(candidateBitmap)
                    if (pixelHash == lastFramePixelHash && lastPublishedPaused == false && framesSentCount > 0L) {
                        candidateBitmap.recycle()
                        delay(420L)
                        continue
                    }

                    // Sızıntı Önleme Penceresi: Kareden 160ms sonra hala izinli uygulamada mıyız doğrula
                    delay(160L)
                    val recheck = evaluateShouldMirrorNow()
                    if (!recheck.allowed || !isActive || isExplicitStopping || forceAbortRequested) {
                        candidateBitmap.recycle()
                        if (!isExplicitStopping && !forceAbortRequested) {
                            syncFreezeState(paused = true, reason = recheck.reason)
                        }
                        delay(300L)
                        continue
                    }

                    if (SmartBoardHelper.isQuotaCooldownActive()) {
                        delay(8000L)
                        continue
                    }

                    val encoded = encodeBitmapToBase64Jpeg(candidateBitmap)
                    candidateBitmap.recycle()

                    if (!encoded.isNullOrBlank() && !isExplicitStopping && !forceAbortRequested) {
                        lastFramePixelHash = pixelHash
                        framesSentCount += 1L
                        lastPublishedPaused = false
                        SmartBoardHelper.publishScreenMirrorFrame(
                            context = applicationContext,
                            classId = classId,
                            sessionCode = sessionCode,
                            castId = castId,
                            packageName = targetPackageName,
                            appName = targetAppName,
                            frameSeq = framesSentCount,
                            base64Jpeg = encoded
                        )
                        _status.value = _status.value.copy(
                            isRunning = true,
                            isPaused = false,
                            pauseReason = "",
                            framesSent = framesSentCount
                        )
                        updateNotificationText(isPaused = false)
                    }

                    delay(if (SmartBoardHelper.isQuotaCooldownActive()) 8000L else 1500L)
                } catch (e: Exception) {
                    Log.w(TAG, "Capture loop warning: ${e.message}")
                    delay(500L)
                }
            }
        }
    }

    private data class MirrorEvalResult(val allowed: Boolean, val reason: String = "")

    private fun evaluateShouldMirrorNow(): MirrorEvalResult {
        val powerMgr = getSystemService(Context.POWER_SERVICE) as? PowerManager
        if (powerMgr != null && !powerMgr.isInteractive) {
            return MirrorEvalResult(false, "Ekran kapalı — $targetAppName son görüntüsü tahtada sabit")
        }

        val keyguardMgr = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (keyguardMgr != null && keyguardMgr.isKeyguardLocked) {
            return MirrorEvalResult(false, "Telefon kilitli — $targetAppName son görüntüsü tahtada sabit")
        }

        if (AppUpdateDownloadController.isAppInForeground) {
            return MirrorEvalResult(false, "BİRİZ ekranındasınız — $targetAppName son görüntüsü sabitlendi")
        }

        if (!isCapturedContentVisible) {
            return MirrorEvalResult(false, "$targetAppName şu an ekranda değil — son görüntü sabitlendi")
        }

        if (targetPackageName.isNotBlank() && InstalledAppsHelper.hasUsageStatsPermission(applicationContext)) {
            val now = System.currentTimeMillis()
            val queryStart = max(lastUsagePollMs - 1200L, now - 15_000L)
            val newlyDetected = InstalledAppsHelper.detectLatestResumedPackageSince(
                context = applicationContext,
                startMs = queryStart,
                endMs = now
            )
            lastUsagePollMs = now
            if (!newlyDetected.isNullOrBlank()) {
                lastDetectedForegroundPkg = newlyDetected
            }
            val fgPkg = lastDetectedForegroundPkg
            if (fgPkg != null && fgPkg != targetPackageName) {
                return MirrorEvalResult(
                    false,
                    "$targetAppName dışına çıkıldı — son görüntü tahtada sabitlendi"
                )
            }
        }

        return MirrorEvalResult(true)
    }

    private fun syncFreezeState(paused: Boolean, reason: String) {
        if (!paused || isExplicitStopping || forceAbortRequested) return
        if (lastPublishedPaused == true && _status.value.pauseReason == reason) return
        lastPublishedPaused = true

        _status.value = _status.value.copy(
            isPaused = true,
            pauseReason = reason
        )
        updateNotificationText(isPaused = true)

        val curClass = classId
        val curCode = sessionCode
        val curCastId = castId
        val curAppName = targetAppName
        val curPkg = targetPackageName
        serviceScope.launch {
            if (isExplicitStopping || forceAbortRequested) return@launch
            SmartBoardHelper.updateScreenMirrorPauseState(
                context = applicationContext,
                classId = curClass,
                sessionCode = curCode,
                castId = curCastId,
                appName = curAppName,
                packageName = curPkg,
                isPaused = true,
                pauseReason = reason
            )
        }
    }

    private fun captureLatestBitmap(): Bitmap? {
        val reader = imageReader ?: return null
        var image: Image? = null
        return try {
            image = reader.acquireLatestImage() ?: return null
            val planes = image.planes
            if (planes.isEmpty()) return null
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val width = image.width
            val height = image.height
            if (width <= 0 || height <= 0 || pixelStride <= 0) return null

            val rowPadding = rowStride - pixelStride * width
            val fullWidth = width + rowPadding / pixelStride
            val tempBmp = Bitmap.createBitmap(fullWidth, height, Bitmap.Config.ARGB_8888)
            buffer.rewind()
            tempBmp.copyPixelsFromBuffer(buffer)

            val cropped = if (fullWidth == width) {
                tempBmp
            } else {
                val c = Bitmap.createBitmap(tempBmp, 0, 0, width, height)
                tempBmp.recycle()
                c
            }

            // Tamamen siyah kareleri (örn. ekran kapanırken oluşan geçiş karesi) yayınlama
            if (isMostlyBlackFrame(cropped)) {
                cropped.recycle()
                return null
            }
            cropped
        } catch (_: Exception) {
            null
        } finally {
            try {
                image?.close()
            } catch (_: Exception) {}
        }
    }

    private fun isMostlyBlackFrame(bmp: Bitmap): Boolean {
        val w = bmp.width
        val h = bmp.height
        if (w <= 0 || h <= 0) return true
        var nonBlack = 0
        val stepX = max(1, w / 8)
        val stepY = max(1, h / 8)
        var y = stepY / 2
        while (y < h) {
            var x = stepX / 2
            while (x < w) {
                val px = bmp.getPixel(x, y)
                val r = (px shr 16) and 0xFF
                val g = (px shr 8) and 0xFF
                val b = px and 0xFF
                if (r > 10 || g > 10 || b > 10) {
                    nonBlack++
                    if (nonBlack >= 3) return false
                }
                x += stepX
            }
            y += stepY
        }
        return true
    }

    private fun computeQuickHash(bmp: Bitmap): Long {
        val w = bmp.width
        val h = bmp.height
        var hash = 1125899906842597L
        val stepX = max(1, w / 16)
        val stepY = max(1, h / 16)
        var y = 0
        while (y < h) {
            var x = 0
            while (x < w) {
                val px = bmp.getPixel(x, y)
                // RGB'nin üst bitlerini alarak mikro anti-aliasing titreşimlerini yok say
                val quantized = (px and 0x00F8F8F8).toLong()
                hash = hash * 31L + quantized
                x += stepX
            }
            y += stepY
        }
        return hash
    }

    private fun encodeBitmapToBase64Jpeg(bmp: Bitmap): String? {
        return try {
            val out = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 42, out)
            var bytes = out.toByteArray()
            if (bytes.size > 45 * 1024) {
                out.reset()
                bmp.compress(Bitmap.CompressFormat.JPEG, 28, out)
                bytes = out.toByteArray()
            }
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
    }

    private fun checkDisplayOrientationChange() {
        val (rawW, rawH, _) = computeDisplaySizeAndDensity()
        val (scaledW, scaledH) = scaleDimensions(rawW, rawH, 520)
        // Yalnızca tam ekran modunda yön değişimini kontrol et (tek uygulama modunda onCapturedContentResize çalışır)
        val isSingleAppAspectMismatch = (captureWidth > captureHeight) != (scaledW > scaledH)
        if (isSingleAppAspectMismatch && lastPublishedPaused == false) {
            captureWidth = scaledW
            captureHeight = scaledH
            mainHandler.post { recreateVirtualDisplaySurface() }
        }
    }

    private fun computeDisplaySizeAndDensity(): Triple<Int, Int, Int> {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val density = resources.displayMetrics.densityDpi.coerceAtLeast(160)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.maximumWindowMetrics.bounds
            Triple(bounds.width().coerceAtLeast(360), bounds.height().coerceAtLeast(640), density)
        } else {
            val dm = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(dm)
            Triple(dm.widthPixels.coerceAtLeast(360), dm.heightPixels.coerceAtLeast(640), dm.densityDpi.coerceAtLeast(160))
        }
    }

    private fun scaleDimensions(srcW: Int, srcH: Int, maxSide: Int): Pair<Int, Int> {
        val maxVal = max(srcW, srcH).coerceAtLeast(1)
        if (maxVal <= maxSide) {
            return (srcW - (srcW % 2)).coerceAtLeast(240) to (srcH - (srcH % 2)).coerceAtLeast(240)
        }
        val ratio = maxSide.toFloat() / maxVal.toFloat()
        val w = ((srcW * ratio).roundToInt()).let { if (it % 2 != 0) it - 1 else it }.coerceAtLeast(240)
        val h = ((srcH * ratio).roundToInt()).let { if (it % 2 != 0) it - 1 else it }.coerceAtLeast(240)
        return w to h
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Akıllı Tahta Canlı Uygulama Paylaşımı",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Seçilen uygulamanın Akıllı Tahtaya canlı yansıtma durumu"
            }
            mgr.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(isPaused: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, SmartBoardScreenCastService::class.java).apply {
            action = ACTION_STOP
            putExtra(EXTRA_CLASS_ID, classId)
            putExtra(EXTRA_SESSION_CODE, sessionCode)
        }
        val stopPending = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = if (isPaused) {
            "⏸️ Tahta Sabitlendi: $targetAppName"
        } else {
            "📡 Tahtaya Yansıtılıyor: $targetAppName"
        }
        val text = if (isPaused) {
            "Uygulama dışındasınız. Tahtada son görüntü donuk tutuluyor."
        } else {
            "Yalnızca bu uygulama açıkken tahta canlı güncellenir."
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_biriz_emblem)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openPending)
            .addAction(0, "⏹️ Paylaşımı Durdur", stopPending)
            .build()
    }

    private fun updateNotificationText(isPaused: Boolean) {
        try {
            val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            mgr.notify(NOTIFICATION_ID, buildNotification(isPaused))
        } catch (_: Exception) {}
    }

    private fun registerScreenReceiver() {
        if (screenReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(screenReceiver, filter)
        }
        screenReceiverRegistered = true
    }

    private fun releaseProjectionOnly() {
        captureJob?.cancel()
        captureJob = null
        try {
            virtualDisplay?.release()
        } catch (_: Exception) {}
        virtualDisplay = null
        try {
            imageReader?.close()
        } catch (_: Exception) {}
        imageReader = null
        try {
            mediaProjection?.unregisterCallback(projectionCallback)
            mediaProjection?.stop()
        } catch (_: Exception) {}
        mediaProjection = null
    }

    private fun releaseAllAndResetStatus() {
        releaseProjectionOnly()
        _status.value = ScreenMirrorStatus()
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    override fun onDestroy() {
        if (screenReceiverRegistered) {
            try {
                unregisterReceiver(screenReceiver)
            } catch (_: Exception) {}
            screenReceiverRegistered = false
        }
        releaseProjectionOnly()
        serviceScope.cancel()
        super.onDestroy()
    }
}
