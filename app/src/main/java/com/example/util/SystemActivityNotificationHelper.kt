package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.BirizNotification
import com.example.data.model.NotificationType

/**
 * Mesaj ve uygulama güncellemesi dışındaki tüm bildirimlerin (Duyurular, Yıldız Ödülleri,
 * Sınıf Kitaplığı, Turnuva Gelişmeleri ve Yeni Veli Katılımı) Android sistem bildirim çubuğunda
 * görünmesini ve dokunulduğunda ilgili ekrana gitmesini yönetir.
 */
object SystemActivityNotificationHelper {

    const val CHANNEL_ANNOUNCEMENTS_ID = "biriz_announcements"
    const val CHANNEL_ACTIVITIES_ID = "biriz_student_activities"
    const val CHANNEL_PARENT_REG_ID = "biriz_parent_registrations"

    const val EXTRA_OPEN_NOTIF_ID = "extra_open_notif_id"
    const val EXTRA_OPEN_NOTIF_TYPE = "extra_open_notif_type"
    const val EXTRA_OPEN_NOTIF_SUBPAGE = "extra_open_notif_subpage"
    const val EXTRA_OPEN_NOTIF_TARGET_ID = "extra_open_notif_target_id"
    const val EXTRA_OPEN_NOTIF_IS_TEACHER = "extra_open_notif_is_teacher"

    private const val NOTIFICATION_ID_BASE = 15000

    fun notificationIdForKey(notifId: String): Int {
        return NOTIFICATION_ID_BASE + ((notifId.hashCode() and 0x7FFFFFFF) % 40000)
    }

    fun channelIdForType(type: NotificationType): String {
        return when (type) {
            NotificationType.ANNOUNCEMENT -> CHANNEL_ANNOUNCEMENTS_ID
            NotificationType.PARENT_REGISTRATION -> CHANNEL_PARENT_REG_ID
            NotificationType.STAR_REWARD,
            NotificationType.LIBRARY,
            NotificationType.TOURNAMENT,
            NotificationType.SCHEDULE,
            NotificationType.SEATING,
            NotificationType.GROUP -> CHANNEL_ACTIVITIES_ID
            NotificationType.MESSAGE -> ChatNotificationHelper.CHANNEL_ID
            NotificationType.APP_UPDATE -> AppUpdateNotificationHelper.CHANNEL_ID
        }
    }

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_ANNOUNCEMENTS_ID,
                    "Sınıf Duyuruları",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Öğretmen tarafından paylaşılan yeni ve güncellenen sınıf duyuruları"
                    enableLights(true)
                    enableVibration(true)
                    setShowBadge(true)
                },
                NotificationChannel(
                    CHANNEL_ACTIVITIES_ID,
                    "Öğrenci Etkinlikleri",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Yıldız ödülleri, kitaplık hareketleri ve turnuva bildirimleri"
                    enableLights(true)
                    enableVibration(true)
                    setShowBadge(true)
                },
                NotificationChannel(
                    CHANNEL_PARENT_REG_ID,
                    "Veli Katılımları",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Sınıfa yeni veli katılım bildirimleri"
                    enableLights(true)
                    enableVibration(true)
                    setShowBadge(true)
                }
            )
            channels.forEach { manager.createNotificationChannel(it) }
        }
    }

    fun showNotification(
        context: Context,
        notification: BirizNotification,
        isRecipientTeacher: Boolean
    ) {
        val isSpecialMessageAlert = notification.id.startsWith("msg_react_") ||
            notification.id.startsWith("sched_confirm_")
        if (notification.type == NotificationType.APP_UPDATE ||
            (notification.type == NotificationType.MESSAGE && !isSpecialMessageAlert)
        ) {
            return
        }

        createNotificationChannels(context)
        if (notification.type == NotificationType.MESSAGE) {
            ChatNotificationHelper.createNotificationChannel(context)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
            if (permission != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val notifId = notificationIdForKey(notification.id)
        val channelId = channelIdForType(notification.type)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_NOTIF_ID, notification.id)
            putExtra(EXTRA_OPEN_NOTIF_TYPE, notification.type.name)
            putExtra(EXTRA_OPEN_NOTIF_SUBPAGE, notification.targetSubPageName)
            putExtra(EXTRA_OPEN_NOTIF_TARGET_ID, notification.targetId)
            putExtra(EXTRA_OPEN_NOTIF_IS_TEACHER, isRecipientTeacher)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val built = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(notification.title)
            .setContentText(notification.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notification.message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(
                if (notification.type == NotificationType.ANNOUNCEMENT) {
                    NotificationCompat.CATEGORY_STATUS
                } else {
                    NotificationCompat.CATEGORY_EVENT
                }
            )
            .setWhen(notification.timestamp)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId, built)
        } catch (_: SecurityException) {
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelNotification(context: Context, notificationId: String) {
        try {
            NotificationManagerCompat.from(context).cancel(notificationIdForKey(notificationId))
        } catch (_: Exception) {
        }
    }

    fun cancelNotifications(context: Context, notificationIds: Collection<String>) {
        val manager = try {
            NotificationManagerCompat.from(context)
        } catch (_: Exception) {
            return
        }
        notificationIds.distinct().forEach { id ->
            try {
                manager.cancel(notificationIdForKey(id))
            } catch (_: Exception) {
            }
        }
    }
}
