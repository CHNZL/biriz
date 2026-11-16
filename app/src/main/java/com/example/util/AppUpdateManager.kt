package com.example.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

data class UpdateReleaseItem(
    val title: String,
    val summary: String
)

data class UpdateInfo(
    val isUpdateAvailable: Boolean,
    val currentVersionName: String,
    val currentVersionCode: Int,
    val latestVersionName: String,
    val latestVersionCode: Int,
    val downloadUrl: String,
    val releaseNotes: String,
    val releaseTag: String = "",
    val releaseTitle: String = "",
    val releaseItems: List<UpdateReleaseItem> = emptyList()
) {
    fun parsedReleaseItems(): List<UpdateReleaseItem> =
        releaseItems.ifEmpty { AppUpdateManager.parseReleaseNotes(releaseNotes) }
}

object AppUpdateManager {

    private const val PREFS_NAME = "biriz_update_prefs"
    private const val KEY_SAVED_REPO = "github_repo_owner_name"
    const val DEFAULT_REPO = "CHNZL/biriz"

    /**
     * Kullanıcının kayıtlı veya varsayılan GitHub deposunu döner (örn: "kullanici/repo").
     */
    fun getGitHubRepo(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_SAVED_REPO, null)?.trim()
        if (!saved.isNullOrEmpty()) return saved

        val buildRepo = BuildConfig.GITHUB_REPOSITORY.trim()
        if (buildRepo.isNotEmpty()) return buildRepo

        return DEFAULT_REPO
    }

    /**
     * GitHub depo adını günceller.
     */
    fun setGitHubRepo(context: Context, repo: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SAVED_REPO, repo.trim()).apply()
    }

    /**
     * GitHub Releases (doğrudan sürüm varlığı, etiket yönlendirmesi ve API) üzerinden en son sürümü denetler.
     * GitHub REST API saatlik 60 istek sınırına (HTTP 403 Rate Limit) takılmamak için
     * önce doğrudan Release Asset (releases/latest/download/version.json) ve Release Redirect
     * (releases/latest -> /releases/tag/v1.0.X) kanallarını kontrol eder ve en yüksek sürüm kodunu seçer.
     */
    suspend fun checkForUpdates(context: Context, customRepo: String? = null): UpdateInfo? = withContext(Dispatchers.IO) {
        val repo = (customRepo ?: getGitHubRepo(context)).trim().trim('/')
        if (repo.isEmpty() || !repo.contains("/")) {
            return@withContext null
        }

        val currentVersionName = BuildConfig.VERSION_NAME
        val currentVersionCode = BuildConfig.VERSION_CODE

        val candidates = mutableListOf<UpdateInfo>()

        // 1. Yol (Rate-Limit Yok): GitHub Latest Release içindeki version.json asset dosyası
        fetchFromLatestReleaseVersionJson(repo, currentVersionName, currentVersionCode)?.let {
            candidates.add(it)
        }

        // 2. Yol (Rate-Limit Yok): GitHub releases/latest 302 Location yönlendirmesinden etiket (tag) okuma
        fetchFromLatestReleaseRedirect(repo, currentVersionName, currentVersionCode)?.let {
            candidates.add(it)
        }

        // 3. Yol: Eğer yukarıdaki iki kanaldan sonuç alınamazsa GitHub Releases REST API dene
        if (candidates.isEmpty()) {
            fetchFromReleasesApi(repo, currentVersionName, currentVersionCode)?.let {
                candidates.add(it)
            }
        }

        // 4. Yol: Son çare olarak raw version.json kontrol et
        if (candidates.isEmpty()) {
            fetchFromRawVersionJson(repo, currentVersionName, currentVersionCode)?.let {
                candidates.add(it)
            }
        }

        // Bulunan adaylar arasından en güncel (aynı sürümdeyse zengin sürüm notu olan) adayı seç
        var best = candidates.maxWithOrNull(
            compareBy<UpdateInfo> { it.latestVersionCode }
                .thenBy { it.releaseItems.size }
                .thenBy { it.releaseTitle.length }
        )

        // Yönlendirme (redirect) adayı seçildiyse ve sürüm notu boşsa Releases API ile zenginleştir
        if (best != null && best.releaseItems.isEmpty()) {
            fetchFromReleasesApi(repo, currentVersionName, currentVersionCode)?.let { apiInfo ->
                if (apiInfo.latestVersionCode >= best!!.latestVersionCode && apiInfo.releaseItems.isNotEmpty()) {
                    best = best!!.copy(
                        releaseNotes = apiInfo.releaseNotes,
                        releaseTitle = best!!.releaseTitle.ifBlank { apiInfo.releaseTitle },
                        releaseItems = apiInfo.releaseItems
                    )
                }
            }
        }

        return@withContext best
    }

    /**
     * GitHub'ın her sürümde (Release) yayınladığı `releases/latest/download/version.json` dosyasını
     * 302 yönlendirmelerini takip ederek doğrudan okur (API Rate Limit'e takılmaz).
     */
    private fun fetchFromLatestReleaseVersionJson(
        repo: String,
        currentVersionName: String,
        currentVersionCode: Int
    ): UpdateInfo? {
        return try {
            val assetUrl = "https://github.com/$repo/releases/latest/download/version.json?t=${System.currentTimeMillis()}"
            val jsonStr = fetchUrlTextFollowingRedirects(assetUrl) ?: return null
            val json = JSONObject(jsonStr)

            val rawName = json.optString("versionName", "").trim().removePrefix("v").removePrefix("V")
            if (rawName.isEmpty()) return null
            val latestVersionCode = json.optInt("versionCode", extractVersionCode(rawName))
            val tag = json.optString("tag", "v$rawName").ifBlank { "v$rawName" }
            var apkUrl = json.optString("apkUrl", "").trim()
            val releaseTitle = json.optString("releaseTitle", "").trim()
            val releaseNotes = json.optString("releaseNotes", "BİRİZ v$rawName yeni sürümü kullanıma hazır.")
            val releaseItems = parseReleaseNotes(releaseNotes, json.optJSONArray("releaseItems"))

            if (apkUrl.isEmpty()) {
                apkUrl = "https://github.com/$repo/releases/download/$tag/Biriz.apk"
            }

            val isUpdate = isRemoteNewer(rawName, latestVersionCode, currentVersionName, currentVersionCode)
            UpdateInfo(
                isUpdateAvailable = isUpdate,
                currentVersionName = currentVersionName,
                currentVersionCode = currentVersionCode,
                latestVersionName = rawName,
                latestVersionCode = latestVersionCode,
                downloadUrl = apkUrl,
                releaseNotes = releaseNotes,
                releaseTag = tag,
                releaseTitle = releaseTitle,
                releaseItems = releaseItems
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * `https://github.com/{repo}/releases/latest` adresinin döndürdüğü 302 Location başlığından
     * (`/releases/tag/v1.0.84` gibi) en son sürüm etiketini anında tespit eder (API Rate Limit'e takılmaz).
     */
    private fun fetchFromLatestReleaseRedirect(
        repo: String,
        currentVersionName: String,
        currentVersionCode: Int
    ): UpdateInfo? {
        return try {
            val latestUrl = "https://github.com/$repo/releases/latest?t=${System.currentTimeMillis()}"
            val connection = (URL(latestUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "Biriz-Android-App")
                setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                setRequestProperty("Pragma", "no-cache")
            }

            val code = connection.responseCode
            val location = if (code in 300..399) {
                connection.getHeaderField("Location") ?: ""
            } else {
                connection.url?.toString() ?: ""
            }

            if (!location.contains("/releases/tag/")) {
                return null
            }

            val tagName = location
                .substringAfterLast("/releases/tag/")
                .substringBefore("?")
                .substringBefore("#")
                .trim()
            if (tagName.isEmpty()) return null

            val rawVersionName = if (tagName.startsWith("v", ignoreCase = true)) tagName.substring(1) else tagName
            val latestVersionCode = extractVersionCode(rawVersionName)
            val apkUrl = "https://github.com/$repo/releases/download/$tagName/Biriz.apk"
            val isUpdate = isRemoteNewer(rawVersionName, latestVersionCode, currentVersionName, currentVersionCode)

            UpdateInfo(
                isUpdateAvailable = isUpdate,
                currentVersionName = currentVersionName,
                currentVersionCode = currentVersionCode,
                latestVersionName = rawVersionName,
                latestVersionCode = latestVersionCode,
                downloadUrl = apkUrl,
                releaseNotes = "BİRİZ v$rawVersionName yeni sürümü kullanıma hazır.",
                releaseTag = tagName
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchUrlTextFollowingRedirects(initialUrl: String): String? {
        var currentUrl = initialUrl
        var redirects = 0
        while (redirects <= 8) {
            val connection = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "Biriz-Android-App")
                setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                setRequestProperty("Pragma", "no-cache")
            }
            val code = connection.responseCode
            if (code in 300..399) {
                val location = connection.getHeaderField("Location")
                if (!location.isNullOrBlank()) {
                    currentUrl = if (location.startsWith("http://") || location.startsWith("https://")) {
                        location
                    } else {
                        URL(URL(currentUrl), location).toString()
                    }
                    redirects++
                    continue
                }
            }
            if (code !in 200..299) return null
            return connection.inputStream.bufferedReader().use { it.readText() }
        }
        return null
    }

    private fun fetchFromReleasesApi(repo: String, currentVersionName: String, currentVersionCode: Int): UpdateInfo? {
        return try {
            val apiUrl = "https://api.github.com/repos/$repo/releases/latest?t=${System.currentTimeMillis()}"
            val connection = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "Biriz-Android-App")
                setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                setRequestProperty("Pragma", "no-cache")
            }

            if (connection.responseCode !in 200..299) {
                return null
            }

            val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(jsonStr)

            val tagName = json.optString("tag_name", "").trim()
            val rawVersionName = if (tagName.startsWith("v", ignoreCase = true)) tagName.substring(1) else tagName
            val releaseNotes = json.optString("body", "Yeni BİRİZ sürümü yayınlandı.")
            val releaseItems = parseReleaseNotes(releaseNotes)

            // Sürüm kodunu tag'den çıkar (Örn: v1.0.5 -> 1005)
            val latestVersionCode = extractVersionCode(rawVersionName)

            // APK download URL bul
            var apkUrl = ""
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val assetName = asset.optString("name", "")
                    if (assetName.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url", "")
                        if (assetName.equals("Biriz.apk", ignoreCase = true)) {
                            break // Tam eşleşmeyi tercih et
                        }
                    }
                }
            }

            if (apkUrl.isEmpty()) {
                apkUrl = "https://github.com/$repo/releases/download/$tagName/Biriz.apk"
            }

            val isUpdate = isRemoteNewer(rawVersionName, latestVersionCode, currentVersionName, currentVersionCode)

            UpdateInfo(
                isUpdateAvailable = isUpdate,
                currentVersionName = currentVersionName,
                currentVersionCode = currentVersionCode,
                latestVersionName = rawVersionName,
                latestVersionCode = latestVersionCode,
                downloadUrl = apkUrl,
                releaseNotes = releaseNotes,
                releaseTag = tagName,
                releaseItems = releaseItems
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun fetchFromRawVersionJson(repo: String, currentVersionName: String, currentVersionCode: Int): UpdateInfo? {
        return try {
            val rawUrl = "https://raw.githubusercontent.com/$repo/main/version.json?t=${System.currentTimeMillis()}"
            val connection = (URL(rawUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "Biriz-Android-App")
                setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                setRequestProperty("Pragma", "no-cache")
            }

            if (connection.responseCode !in 200..299) {
                return null
            }

            val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(jsonStr)

            val latestVersionName = json.optString("versionName", "1.0.0").trim().removePrefix("v").removePrefix("V")
            val latestVersionCode = json.optInt("versionCode", extractVersionCode(latestVersionName))
            var apkUrl = json.optString("apkUrl", "")
            val releaseTitle = json.optString("releaseTitle", "").trim()
            val releaseNotes = json.optString("releaseNotes", "Yeni sürüm güncellemeleri.")
            val releaseItems = parseReleaseNotes(releaseNotes, json.optJSONArray("releaseItems"))
            val tag = json.optString("tag", "v$latestVersionName")

            if (apkUrl.isEmpty()) {
                apkUrl = "https://github.com/$repo/releases/download/$tag/Biriz.apk"
            }

            val isUpdate = isRemoteNewer(latestVersionName, latestVersionCode, currentVersionName, currentVersionCode)

            UpdateInfo(
                isUpdateAvailable = isUpdate,
                currentVersionName = currentVersionName,
                currentVersionCode = currentVersionCode,
                latestVersionName = latestVersionName,
                latestVersionCode = latestVersionCode,
                downloadUrl = apkUrl,
                releaseNotes = releaseNotes,
                releaseTag = tag,
                releaseTitle = releaseTitle,
                releaseItems = releaseItems
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Ham sürüm notlarını veya JSON listesini kısa Türkçe başlık + tek cümle açıklama maddelerine dönüştürür.
     */
    fun parseReleaseNotes(rawNotes: String, itemsJson: JSONArray? = null): List<UpdateReleaseItem> {
        val parsedFromJson = mutableListOf<UpdateReleaseItem>()
        if (itemsJson != null) {
            for (i in 0 until itemsJson.length()) {
                val obj = itemsJson.optJSONObject(i) ?: continue
                val title = obj.optString("title", "").trim()
                val summary = obj.optString("summary", "").trim()
                if (title.isNotEmpty() && summary.isNotEmpty()) {
                    parsedFromJson.add(UpdateReleaseItem(title = title, summary = summary))
                }
            }
        }
        if (parsedFromJson.isNotEmpty()) return parsedFromJson

        if (rawNotes.isBlank()) return emptyList()

        val result = mutableListOf<UpdateReleaseItem>()
        val ignoredFragments = listOf(
            "indirme seçenekleri",
            "doğrudan apk indir",
            "sürümlü apk",
            "uygulamayı açtığınızda",
            "bu sürüm github senkronizasyonu",
            "otomatik derlemesi oluşturuldu",
            "yeni sürümü kullanıma hazır"
        )

        for (rawLine in rawNotes.lines()) {
            val trimmed = rawLine.trim()
            if (trimmed.isBlank()) continue
            if (trimmed.startsWith("#")) continue
            val lower = trimmed.lowercase()
            if (ignoredFragments.any { lower.contains(it) }) continue
            if (lower.contains("releases/download/") || lower.contains("[biriz")) continue

            val clean = trimmed
                .trimStart('-', '*', '•', ' ')
                .replace("**", "")
                .replace("*", "")
                .trim()
            if (clean.isBlank()) continue

            val colonIdx = clean.indexOf(':')
            if (colonIdx in 2..42) {
                val head = clean.substring(0, colonIdx).trim()
                val body = clean.substring(colonIdx + 1).trim()
                if (head.isNotBlank() && body.isNotBlank()) {
                    result.add(UpdateReleaseItem(title = head, summary = body))
                    continue
                }
            }
            result.add(UpdateReleaseItem(title = "Yenilik", summary = clean))
        }
        return result.take(6)
    }

    internal fun extractVersionCode(versionName: String): Int {
        val clean = versionName.trim().removePrefix("v").removePrefix("V")
        val parts = clean.split(".").mapNotNull { part ->
            part.filter { it.isDigit() }.toIntOrNull()
        }
        return when {
            parts.size >= 3 -> parts[0] * 1000 + parts[1] * 100 + parts[2]
            parts.size == 2 -> parts[0] * 1000 + parts[1] * 100
            parts.size == 1 -> 1000 + parts[0]
            else -> 1001
        }
    }

    internal fun isRemoteNewer(
        remoteName: String,
        remoteCode: Int,
        currentName: String,
        currentCode: Int
    ): Boolean {
        if (remoteCode > currentCode) return true
        if (remoteCode < currentCode) return false

        // Sürüm string karşılaştırma (1.0.5 vs 1.0.2)
        val rParts = remoteName.split(".").mapNotNull { it.toIntOrNull() }
        val cParts = currentName.split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(rParts.size, cParts.size)
        for (i in 0 until maxLen) {
            val r = rParts.getOrElse(i) { 0 }
            val c = cParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    private val apkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    internal fun parseVersionCodeFromApkFileName(fileName: String): Int? {
        val match = Regex("""^Biriz_v(\d+)\.apk(?:\.tmp)?$""", RegexOption.IGNORE_CASE).find(fileName)
        return match?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    internal fun shouldPreserveApkFile(
        file: File,
        currentVersionCode: Int,
        keepFile: File?,
        activeKeepPath: String?,
        isDownloading: Boolean
    ): Boolean {
        if (keepFile != null && file.absolutePath == keepFile.absolutePath) return true
        if (!activeKeepPath.isNullOrBlank() && file.absolutePath == activeKeepPath) return true
        if (isDownloading && file.name.endsWith(".apk.tmp", ignoreCase = true)) return true
        val parsedVersion = parseVersionCodeFromApkFileName(file.name)
        if (parsedVersion != null && parsedVersion > currentVersionCode && !file.name.endsWith(".tmp", ignoreCase = true)) {
            return true
        }
        return false
    }

    fun getDownloadedApkForVersion(context: Context, versionCode: Int): File? {
        if (versionCode <= BuildConfig.VERSION_CODE) return null
        val candidate = File(context.cacheDir, "Biriz_v$versionCode.apk")
        return if (candidate.exists() && candidate.isFile && candidate.length() > 50_000L) {
            candidate
        } else {
            null
        }
    }

    /**
     * Cihaz hafızasında birikmemesi için eski indirilen APK artıklarını temizler.
     * Aktif indirme veya mevcut sürümden daha yeni indirilmiş hazır APK dosyalarını korur.
     */
    fun cleanOldApkFiles(context: Context, keepFile: File? = null) {
        try {
            val activeKeepPath = AppUpdateDownloadController.getActiveKeepFilePath()
            val isDownloading = AppUpdateDownloadController.isActivelyDownloading()
            val directories = listOfNotNull(
                context.cacheDir,
                context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS),
                context.filesDir
            )
            for (dir in directories) {
                if (dir.exists() && dir.isDirectory) {
                    val files = dir.listFiles { file ->
                        file.isFile && (
                            file.name.endsWith(".apk", ignoreCase = true) ||
                                file.name.endsWith(".apk.tmp", ignoreCase = true)
                            )
                    }
                    files?.forEach { apk ->
                        val preserve = shouldPreserveApkFile(
                            file = apk,
                            currentVersionCode = BuildConfig.VERSION_CODE,
                            keepFile = keepFile,
                            activeKeepPath = activeKeepPath,
                            isDownloading = isDownloading
                        )
                        if (!preserve) {
                            try {
                                apk.delete()
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * APK'yı arka planda da kesintisiz çalışacak şekilde OkHttp ile indirir ve ilerlemeyi bildirir.
     */
    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        onProgress: (Float) -> Unit
    ): File? = downloadApk(
        context = context,
        downloadUrl = downloadUrl,
        targetVersionCode = 0,
        onProgress = { progress, _, _ -> onProgress(progress) }
    )

    internal fun parseTotalBytesFromContentRange(contentRangeHeader: String?): Long {
        if (contentRangeHeader.isNullOrBlank()) return -1L
        val slashPart = contentRangeHeader.substringAfterLast('/', "").trim()
        return slashPart.toLongOrNull()?.takeIf { it > 0L } ?: -1L
    }

    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        targetVersionCode: Int,
        onProgress: (progress: Float, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        val targetName = if (targetVersionCode > 0) "Biriz_v$targetVersionCode.apk" else "Biriz_latest.apk"
        val outputFile = File(context.cacheDir, targetName)
        val tempFile = File(context.cacheDir, "$targetName.tmp")

        try {
            // Eğer aynı sürüm daha önce eksiksiz indirilmişse tekrar indirme
            if (targetVersionCode > BuildConfig.VERSION_CODE && outputFile.exists() && outputFile.length() > 50_000L) {
                onProgress(1f, outputFile.length(), outputFile.length())
                return@withContext outputFile
            }

            // Sadece eski/gereksiz APK dosyalarını temizle (yarım kalan temp dosyasını devam edebilmek için koru)
            cleanOldApkFiles(context, keepFile = tempFile)

            var lastException: Exception? = null
            val maxAttempts = 4
            val backoffDelaysMs = longArrayOf(1200L, 2500L, 4000L)

            for (attempt in 1..maxAttempts) {
                coroutineContext.ensureActive()
                try {
                    val existingBytes = if (tempFile.exists() && tempFile.isFile) tempFile.length() else 0L
                    val requestBuilder = Request.Builder()
                        .url(downloadUrl)
                        .header("User-Agent", "Biriz-Android-App")
                    if (existingBytes > 0L) {
                        requestBuilder.header("Range", "bytes=$existingBytes-")
                    }

                    apkHttpClient.newCall(requestBuilder.build()).execute().use { response ->
                        // HTTP 416 (Range Not Satisfiable): Yarım dosya bozulmuş veya sunucu boyutunu aşmış -> sıfırla
                        if (response.code == 416) {
                            if (tempFile.exists()) tempFile.delete()
                            throw IllegalStateException("Kısmi dosya aralığı geçersiz, baştan deneniyor (HTTP 416)")
                        }
                        if (!response.isSuccessful) {
                            throw IllegalStateException("Sunucu yanıtı başarısız: HTTP ${response.code}")
                        }
                        val body = response.body ?: throw IllegalStateException("Boş indirme yanıtı")

                        val isPartialResume = response.code == 206 && existingBytes > 0L
                        val appendMode = isPartialResume
                        val startOffset = if (isPartialResume) existingBytes else 0L
                        val bodyLen = body.contentLength()
                        val totalBytes = if (isPartialResume) {
                            val fromRange = parseTotalBytesFromContentRange(response.header("Content-Range"))
                            when {
                                fromRange > 0L -> fromRange
                                bodyLen > 0L -> startOffset + bodyLen
                                else -> -1L
                            }
                        } else {
                            bodyLen
                        }

                        var totalRead = startOffset
                        var lastReportTime = 0L
                        var lastReportedPct = -1

                        if (isPartialResume && totalBytes > 0L) {
                            onProgress((totalRead.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f), totalRead, totalBytes)
                        }

                        body.byteStream().use { input ->
                            FileOutputStream(tempFile, appendMode).use { output ->
                                val buffer = ByteArray(16 * 1024)
                                var bytesRead: Int
                                while (input.read(buffer).also { bytesRead = it } != -1) {
                                    coroutineContext.ensureActive()
                                    output.write(buffer, 0, bytesRead)
                                    totalRead += bytesRead

                                    val rawProgress = if (totalBytes > 0L) {
                                        (totalRead.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                                    } else {
                                        0f
                                    }
                                    val pct = (rawProgress * 100).toInt()
                                    val now = System.currentTimeMillis()
                                    if (pct != lastReportedPct || now - lastReportTime >= 200L) {
                                        lastReportedPct = pct
                                        lastReportTime = now
                                        onProgress(rawProgress, totalRead, totalBytes)
                                    }
                                }
                                output.flush()
                            }
                        }

                        if (totalBytes > 0L && tempFile.length() < totalBytes) {
                            throw IllegalStateException("İndirme eksik kaldı (${tempFile.length()} / $totalBytes)")
                        }
                    }

                    if (outputFile.exists()) {
                        outputFile.delete()
                    }
                    if (!tempFile.renameTo(outputFile)) {
                        tempFile.copyTo(outputFile, overwrite = true)
                        tempFile.delete()
                    }
                    onProgress(1f, outputFile.length(), outputFile.length())
                    cleanOldApkFiles(context, keepFile = outputFile)
                    return@withContext outputFile
                } catch (ce: CancellationException) {
                    if (tempFile.exists()) tempFile.delete()
                    throw ce
                } catch (e: Exception) {
                    lastException = e
                    if (attempt < maxAttempts) {
                        delay(backoffDelaysMs.getOrElse(attempt - 1) { 3000L })
                    }
                }
            }
            if (tempFile.exists()) tempFile.delete()
            lastException?.printStackTrace()
            return@withContext null
        } catch (ce: CancellationException) {
            if (tempFile.exists()) tempFile.delete()
            throw ce
        } catch (e: Exception) {
            if (tempFile.exists()) tempFile.delete()
            e.printStackTrace()
            return@withContext null
        }
    }

    /**
     * Cihazın bilinmeyen kaynaktan APK yükleme iznine sahip olup olmadığını kontrol eder.
     */
    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Kullanıcıyı doğrudan BİRİZ uygulamasının 'Bilinmeyen Uygulamaları Yükle' ayar ekranına yönlendirir.
     */
    fun requestInstallPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val permissionIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(permissionIntent)
        }
    }

    private val installScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * İndirilen APK dosyasını PackageInstaller oturumuyla doğrudan sistem yükleyicisine aktarır.
     * Ara "Uygulama hazırlanıyor..." ekranını atlar ve Android 12+ cihazlarda sessiz güncellemeye olanak tanır.
     */
    fun installApk(context: Context, apkFile: File, targetVersionCode: Int = -1) {
        val appContext = context.applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!appContext.packageManager.canRequestPackageInstalls()) {
                AppUpdateDownloadController.revertInstallingToReady(apkFile.absolutePath, targetVersionCode)
                Toast.makeText(
                    appContext,
                    "Lütfen BİRİZ uygulamasına güncelleme yükleme izni verin.",
                    Toast.LENGTH_LONG
                ).show()
                requestInstallPermission(appContext)
                return
            }
        }

        AppUpdateNotificationHelper.cancelUpdateNotification(appContext)

        installScope.launch {
            var sessionId = -1
            try {
                val packageInstaller = appContext.packageManager.packageInstaller
                // Yarım kalmış eski oturumları temizle
                try {
                    packageInstaller.mySessions.forEach { existing ->
                        try {
                            packageInstaller.abandonSession(existing.sessionId)
                        } catch (_: Exception) {}
                    }
                } catch (_: Exception) {}

                val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                    setAppPackageName(appContext.packageName)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        setInstallReason(PackageManager.INSTALL_REASON_USER)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                    }
                }

                sessionId = packageInstaller.createSession(params)
                val session = packageInstaller.openSession(sessionId)
                session.use { activeSession ->
                    activeSession.openWrite("biriz_update.apk", 0L, apkFile.length()).use { out ->
                        apkFile.inputStream().use { input ->
                            input.copyTo(out, bufferSize = 64 * 1024)
                        }
                        activeSession.fsync(out)
                    }

                    val callbackIntent = Intent(appContext, AppUpdateInstallReceiver::class.java).apply {
                        action = AppUpdateInstallReceiver.ACTION_INSTALL_RESULT
                        putExtra(AppUpdateInstallReceiver.EXTRA_APK_PATH, apkFile.absolutePath)
                        putExtra(AppUpdateInstallReceiver.EXTRA_VERSION_CODE, targetVersionCode)
                    }
                    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                    } else {
                        PendingIntent.FLAG_UPDATE_CURRENT
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        appContext,
                        sessionId,
                        callbackIntent,
                        flags
                    )
                    activeSession.commit(pendingIntent.intentSender)
                }
            } catch (e: Exception) {
                if (sessionId != -1) {
                    try {
                        appContext.packageManager.packageInstaller.abandonSession(sessionId)
                    } catch (_: Exception) {}
                }
                withContext(Dispatchers.Main) {
                    installApkViaIntent(appContext, apkFile, targetVersionCode)
                }
            }
        }
    }

    private fun installApkViaIntent(context: Context, apkFile: File, targetVersionCode: Int) {
        try {
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                putExtra(Intent.EXTRA_RETURN_RESULT, true)
            }

            AppUpdateDownloadController.revertInstallingToReady(apkFile.absolutePath, targetVersionCode)
            context.startActivity(installIntent)
        } catch (e: Exception) {
            AppUpdateDownloadController.revertInstallingToReady(apkFile.absolutePath, targetVersionCode)
            Toast.makeText(
                context,
                "Güncelleme başlatılamadı: ${e.localizedMessage}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
