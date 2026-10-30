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

    /**
     * Kullanıcının kayıtlı veya varsayılan GitHub deposunu döner (örn: "kullanici/repo").
     */
    fun getGitHubRepo(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_SAVED_REPO, null)?.trim()
        if (!saved.isNullOrEmpty()) return saved

        val buildRepo = BuildConfig.GITHUB_REPOSITORY.trim()
        return buildRepo
    }

    /**
     * GitHub depo adını günceller.
     */
    fun setGitHubRepo(context: Context, repo: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SAVED_REPO, repo.trim()).apply()
    }

    /**
     * GitHub Releases veya raw version.json üzerinden en son sürümü denetler.
     */
    suspend fun checkForUpdates(context: Context, customRepo: String? = null): UpdateInfo? = withContext(Dispatchers.IO) {
        val repo = (customRepo ?: getGitHubRepo(context)).trim().trim('/')
        if (repo.isEmpty() || !repo.contains("/")) {
            return@withContext null
        }

        val currentVersionName = BuildConfig.VERSION_NAME
        val currentVersionCode = BuildConfig.VERSION_CODE

        // 1. Yol: GitHub Releases API
        var info = fetchFromReleasesApi(repo, currentVersionName, currentVersionCode)

        // 2. Yol: Eğer Releases API 404/rate limit verirse raw version.json dene
        if (info == null) {
            info = fetchFromRawVersionJson(repo, currentVersionName, currentVersionCode)
        }

        return@withContext info
    }

    private fun fetchFromReleasesApi(repo: String, currentVersionName: String, currentVersionCode: Int): UpdateInfo? {
        return try {
            val apiUrl = "https://api.github.com/repos/$repo/releases/latest"
            val connection = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "Biriz-Android-App")
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
            val rawUrl = "https://raw.githubusercontent.com/$repo/main/version.json"
            val connection = (URL(rawUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "Biriz-Android-App")
            }

            if (connection.responseCode !in 200..299) {
                return null
            }

            val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(jsonStr)

            val latestVersionName = json.optString("versionName", "1.0.0")
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

    private fun extractVersionCode(versionName: String): Int {
        val parts = versionName.split(".")
        val lastPart = parts.lastOrNull()?.toIntOrNull() ?: 1
        return 1000 + lastPart
    }

    private fun isRemoteNewer(
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
     * İndirilen APK dosyasını sistem paket yükleyicisiyle açarak güncellemeyi başlatır.
     */
    fun installApk(context: Context, apkFile: File) {
        try {
            // Android 8.0+ için bilinmeyen uygulama yükleme izni kontrolü
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    Toast.makeText(
                        context,
                        "Lütfen BİRİZ uygulamasına güncelleme yükleme izni verin.",
                        Toast.LENGTH_LONG
                    ).show()
                    val permissionIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(permissionIntent)
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
