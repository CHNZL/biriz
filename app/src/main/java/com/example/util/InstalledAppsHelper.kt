package com.example.util

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val icon: ImageBitmap?
)

object InstalledAppsHelper {

    suspend fun loadLaunchableApps(context: Context): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val selfPkg = context.packageName
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolved = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(launcherIntent, PackageManager.ResolveInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(launcherIntent, 0)
            }
        } catch (_: Exception) {
            emptyList()
        }

        val collator = Collator.getInstance(Locale.forLanguageTag("tr-TR")).apply {
            strength = Collator.PRIMARY
        }

        val seen = HashSet<String>()
        val list = ArrayList<InstalledAppInfo>(resolved.size)
        for (ri in resolved) {
            val activityInfo = ri.activityInfo ?: continue
            val pkg = activityInfo.packageName ?: continue
            if (pkg == selfPkg || !seen.add(pkg)) continue

            val label = try {
                ri.loadLabel(pm)?.toString()?.trim()?.ifBlank { pkg } ?: pkg
            } catch (_: Exception) {
                pkg
            }

            val iconBitmap = try {
                val drawable = ri.loadIcon(pm)
                drawableToImageBitmap(drawable, 72)
            } catch (_: Exception) {
                null
            }

            list.add(
                InstalledAppInfo(
                    packageName = pkg,
                    appName = label,
                    icon = iconBitmap
                )
            )
        }

        list.sortWith { a, b -> collator.compare(a.appName, b.appName) }
        list
    }

    private fun drawableToImageBitmap(drawable: android.graphics.drawable.Drawable, sizePx: Int): ImageBitmap? {
        return try {
            if (drawable is BitmapDrawable && drawable.bitmap != null) {
                val scaled = Bitmap.createScaledBitmap(drawable.bitmap, sizePx, sizePx, true)
                return scaled.asImageBitmap()
            }
            val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bmp.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    fun launchAppByPackage(context: Context, packageName: String): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            } ?: return false
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun hasUsageStatsPermission(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }

    fun openUsageStatsSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    /**
     * Belirtilen zaman aralığında ön plana gelen son uygulamanın paket adını döndürür.
     */
    fun detectLatestResumedPackageSince(context: Context, startMs: Long, endMs: Long): String? {
        if (!hasUsageStatsPermission(context)) return null
        return try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
            val events = usm.queryEvents(startMs, endMs) ?: return null
            val event = UsageEvents.Event()
            var latestPkg: String? = null
            var latestTs = 0L
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val isResumed = event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                    (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                        @Suppress("DEPRECATION")
                        event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND)
                if (isResumed && event.timeStamp >= latestTs && !event.packageName.isNullOrBlank()) {
                    latestTs = event.timeStamp
                    latestPkg = event.packageName
                }
            }
            latestPkg
        } catch (_: Exception) {
            null
        }
    }
}
