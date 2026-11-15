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
    const val EXTRA_OPEN_NOTIFICATION_CENTER = "extra_open_notification_center"

    const val GROUP_KEY_ACTIVITIES = "com.example.biriz.GROUP_ACTIVITIES"
    const val GROUP_KEY_ANNOUNCEMENTS = "com.example.biriz.GROUP_ANNOUNCEMENTS"
    const val GROUP_KEY_PARENT_REG = "com.example.biriz.GROUP_PARENT_REG"

    private const val SUMMARY_ID_ACTIVITIES = 14901
    private const val SUMMARY_ID_ANNOUNCEMENTS = 14902
    private const val SUMMARY_ID_PARENT_REG = 14903

    private const val NOTIFICATION_ID_BASE = 15000

    data class GroupSummaryPayload(
        val title: String,
        val summaryText: String,
        val lines: List<String>
    )

    fun notificationIdForKey(notifId: String): Int {
        return NOTIFICATION_ID_BASE + ((notifId.hashCode() and 0x7FFFFFFF) % 40000)
    }

    fun groupKeyForType(type: NotificationType): String {
        return when (type) {
            NotificationType.ANNOUNCEMENT -> GROUP_KEY_ANNOUNCEMENTS
            NotificationType.PARENT_REGISTRATION -> GROUP_KEY_PARENT_REG
            else -> GROUP_KEY_ACTIVITIES
        }
    }

    fun summaryIdForGroup(groupKey: String): Int {
        return when (groupKey) {
            GROUP_KEY_ANNOUNCEMENTS -> SUMMARY_ID_ANNOUNCEMENTS
            GROUP_KEY_PARENT_REG -> SUMMARY_ID_PARENT_REG
            else -> SUMMARY_ID_ACTIVITIES
        }
    }

    fun buildGroupSummaryText(
        notifications: List<BirizNotification>,
        groupKey: String,
        isMorningDigest: Boolean = false
    ): GroupSummaryPayload {
        val count = notifications.size.coerceAtLeast(1)
        val lines = notifications.map { notif ->
            val preview = notif.message.trim().take(64)
            if (preview.isNotBlank()) "${notif.title} • $preview" else notif.title
        }
        return if (isMorningDigest) {
            GroupSummaryPayload(
                title = "🌅 Günaydın! Gece Gelen $count Bildirim",
                summaryText = "Sessiz saatlerde biriken $count bildiriminiz hazır",
                lines = lines
            )
        } else {
            when (groupKey) {
                GROUP_KEY_ANNOUNCEMENTS -> GroupSummaryPayload(
                    title = "📢 Bugün $count Yeni Sınıf Duyurusu",
                    summaryText = "$count yeni sınıf duyurusu ve anket gelişmesi",
                    lines = lines
                )
                GROUP_KEY_PARENT_REG -> GroupSummaryPayload(
                    title = "👨‍👩‍👧 $count Yeni Veli Bağlantı Bildirimi",
                    summaryText = "$count yeni veli güvenlik ve katılım hareketi",
                    lines = lines
                )
                else -> GroupSummaryPayload(
                    title = "✨ Bugün $count Yeni Sınıf Hareketi",
                    summaryText = "Yıldızlar, kitaplık, turnuva ve sınıf güncellemeleri",
                    lines = lines
                )
            }
        }
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

    private fun isEligibleSystemActivity(notification: BirizNotification): Boolean {
        val isSpecialMessageAlert = notification.id.startsWith("msg_react_") ||
            notification.id.startsWith("sched_confirm_")
        return !(notification.type == NotificationType.APP_UPDATE ||
            (notification.type == NotificationType.MESSAGE && !isSpecialMessageAlert))
    }

    fun showNotification(
        context: Context,
        notification: BirizNotification,
        isRecipientTeacher: Boolean
    ) {
        showNotificationsBatch(
            context = context,
            notifications = listOf(notification),
            isRecipientTeacher = isRecipientTeacher,
            isMorningDigest = false
        )
    }

    fun showNotificationsBatch(
        context: Context,
        notifications: List<BirizNotification>,
        isRecipientTeacher: Boolean,
        isMorningDigest: Boolean = false
    ) {
        val eligible = notifications
            .filter { if (isMorningDigest) it.type != NotificationType.APP_UPDATE else isEligibleSystemActivity(it) }
            .distinctBy { it.id }
        if (eligible.isEmpty()) return

        createNotificationChannels(context)
        if (eligible.any { it.type == NotificationType.MESSAGE }) {
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

        val compatManager = NotificationManagerCompat.from(context)
        val systemManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val activeNotifications = try {
            systemManager?.activeNotifications?.toList().orEmpty()
        } catch (_: Exception) {
            emptyList()
        }

        val groupedBatches = if (isMorningDigest) {
            mapOf(GROUP_KEY_ACTIVITIES to eligible)
        } else {
            eligible.groupBy { groupKeyForType(it.type) }
        }

        groupedBatches.forEach { (groupKey, groupItems) ->
            val summaryId = summaryIdForGroup(groupKey)
            val existingInGroup = activeNotifications.filter { sbn ->
                sbn.id != summaryId && sbn.notification.group == groupKey
            }
            val newIdsSet = groupItems.map { notificationIdForKey(it.id) }.toSet()
            val carriedOverItems = existingInGroup
                .filter { it.id !in newIdsSet }
                .map { sbn ->
                    val extras = sbn.notification.extras
                    val title = extras?.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString().orEmpty()
                    val text = extras?.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString().orEmpty()
                    BirizNotification(
                        id = "active_${sbn.id}",
                        title = title.ifBlank { "BİRİZ Bildirimi" },
                        message = text,
                        timestamp = sbn.postTime,
                        type = groupItems.first().type,
                        targetSubPageName = "HOME",
                        isRead = false
                    )
                }

            val combinedForSummary = (groupItems + carriedOverItems).distinctBy { it.id }
            val shouldPostSummary = isMorningDigest || combinedForSummary.size >= 2

            groupItems.forEach { notification ->
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

                val builder = NotificationCompat.Builder(context, channelId)
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
                    .setGroup(groupKey)
                    .setGroupAlertBehavior(
                        if (shouldPostSummary) NotificationCompat.GROUP_ALERT_SUMMARY
                        else NotificationCompat.GROUP_ALERT_ALL
                    )
                if (!shouldPostSummary) {
                    builder.setDefaults(NotificationCompat.DEFAULT_ALL)
                }

                try {
                    compatManager.notify(notifId, builder.build())
                } catch (_: SecurityException) {
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (shouldPostSummary) {
                val summaryPayload = buildGroupSummaryText(
                    notifications = combinedForSummary,
                    groupKey = groupKey,
                    isMorningDigest = isMorningDigest
                )
                val summaryChannel = channelIdForType(groupItems.first().type)
                val inboxStyle = NotificationCompat.InboxStyle()
                    .setBigContentTitle(summaryPayload.title)
                    .setSummaryText(summaryPayload.summaryText)
                summaryPayload.lines.take(5).forEach { line ->
                    inboxStyle.addLine(line)
                }
                if (summaryPayload.lines.size > 5) {
                    inboxStyle.addLine("+${summaryPayload.lines.size - 5} diğer bildirim…")
                }

                val summaryIntent = Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(EXTRA_OPEN_NOTIFICATION_CENTER, true)
                }
                val summaryPendingIntent = PendingIntent.getActivity(
                    context,
                    summaryId,
                    summaryIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val summaryNotif = NotificationCompat.Builder(context, summaryChannel)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(summaryPayload.title)
                    .setContentText(summaryPayload.summaryText)
                    .setStyle(inboxStyle)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setGroup(groupKey)
                    .setGroupSummary(true)
                    .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_ALL)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setAutoCancel(true)
                    .setContentIntent(summaryPendingIntent)
                    .build()

                try {
                    compatManager.notify(summaryId, summaryNotif)
                } catch (_: SecurityException) {
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun cleanupOrphanedGroupSummaries(context: Context) {
        val systemManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val active = try {
            systemManager.activeNotifications?.toList().orEmpty()
        } catch (_: Exception) {
            return
        }
        val compatManager = NotificationManagerCompat.from(context)
        listOf(GROUP_KEY_ACTIVITIES, GROUP_KEY_ANNOUNCEMENTS, GROUP_KEY_PARENT_REG).forEach { groupKey ->
            val summaryId = summaryIdForGroup(groupKey)
            val childCount = active.count { it.id != summaryId && it.notification.group == groupKey }
            if (childCount <= 1) {
                try {
                    compatManager.cancel(summaryId)
                } catch (_: Exception) {
                }
            }
        }
    }

    fun cancelNotification(context: Context, notificationId: String) {
        try {
            NotificationManagerCompat.from(context).cancel(notificationIdForKey(notificationId))
            cleanupOrphanedGroupSummaries(context)
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
        listOf(SUMMARY_ID_ACTIVITIES, SUMMARY_ID_ANNOUNCEMENTS, SUMMARY_ID_PARENT_REG).forEach { sId ->
            try {
                manager.cancel(sId)
            } catch (_: Exception) {
            }
        }
    }
}
