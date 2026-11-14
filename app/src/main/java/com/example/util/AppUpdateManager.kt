package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val isUpdateAvailable: Boolean,
    val currentVersionName: String,
    val currentVersionCode: Int,
    val latestVersionName: String,
    val latestVersionCode: Int,
    val downloadUrl: String,
    val releaseNotes: String,
    val releaseTag: String = ""
)

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

        // Bulunan adaylar arasından en güncel (en yüksek versionCode'lu) olanı seç
        return@withContext candidates.maxByOrNull { it.latestVersionCode }
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
            val releaseNotes = json.optString("releaseNotes", "BİRİZ v$rawName yeni sürümü kullanıma hazır.")

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
                releaseTag = tag
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
                releaseTag = tagName
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
            val releaseNotes = json.optString("releaseNotes", "Yeni sürüm güncellemeleri.")
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
                releaseTag = tag
            )
        } catch (e: Exception) {
            null
        }
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

    /**
     * Cihaz hafızasında birikmemesi için eski indirilen APK artıklarını temizler.
     * context.cacheDir ve harici indirme dizinindeki eski APK dosyalarını siler.
     */
    fun cleanOldApkFiles(context: Context, keepFile: File? = null) {
        try {
            val directories = listOfNotNull(
                context.cacheDir,
                context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS),
                context.filesDir
            )
            for (dir in directories) {
                if (dir.exists() && dir.isDirectory) {
                    val files = dir.listFiles { file ->
                        file.isFile && file.name.endsWith(".apk", ignoreCase = true)
                    }
                    files?.forEach { apk ->
                        if (keepFile == null || apk.absolutePath != keepFile.absolutePath) {
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
     * APK'yı arka planda indirir ve indirme ilerlemesini bildirir (0f..1f).
     * İndirmeye başlamadan önce cihazdaki tüm eski APK artıklarını otomatik temizler.
     */
    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        onProgress: (Float) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            // Önceki eski APK dosyalarını silerek hafızada yer aç
            cleanOldApkFiles(context)

            var currentUrl = downloadUrl
            var connection: HttpURLConnection
            var redirects = 0

            // GitHub Releases AWS S3 yönlendirmelerini (302/301) takip et
            while (true) {
                connection = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    connectTimeout = 15000
                    readTimeout = 20000
                    setRequestProperty("User-Agent", "Biriz-Android-App")
                }
                val code = connection.responseCode
                if (code in 300..399) {
                    val location = connection.getHeaderField("Location")
                    if (!location.isNullOrEmpty()) {
                        currentUrl = location
                        redirects++
                        if (redirects > 8) throw IllegalStateException("Çok fazla yönlendirme!")
                        continue
                    }
                }
                break
            }

            val totalBytes = connection.contentLength.toLong()
            val outputFile = File(context.cacheDir, "Biriz_latest.apk")
            if (outputFile.exists()) {
                outputFile.delete()
            }

            connection.inputStream.use { input ->
                FileOutputStream(outputFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (totalBytes > 0) {
                            val progress = (totalRead.toFloat() / totalBytes).coerceIn(0f, 1f)
                            onProgress(progress)
                        }
                    }
                }
            }

            onProgress(1f)
            return@withContext outputFile
        } catch (e: Exception) {
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

    /**
     * İndirilen APK dosyasını doğrudan sistem paket yükleyicisine aktararak
     * gereksiz soru ve onay adımları olmadan güncellemeyi başlatır.
     */
    fun installApk(context: Context, apkFile: File) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    Toast.makeText(
                        context,
                        "Lütfen BİRİZ uygulamasına güncelleme yükleme izni verin.",
                        Toast.LENGTH_LONG
                    ).show()
                    requestInstallPermission(context)
                    return
                }
            }

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

            context.startActivity(installIntent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Güncelleme başlatılamadı: ${e.localizedMessage}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
