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
import com.example.data.model.ChatMessage

object ChatNotificationHelper {

    const val CHANNEL_ID = "biriz_chat_notifications"
    const val EXTRA_OPEN_CHAT_STUDENT_ID = "extra_open_chat_student_id"
    const val EXTRA_OPEN_CHAT_IS_TEACHER = "extra_open_chat_is_teacher"
    const val GROUP_KEY_MESSAGES = "com.example.biriz.GROUP_MESSAGES"

    private const val NOTIFICATION_ID_BASE = 3000
    private const val SUMMARY_ID_MESSAGES = 2999

    fun notificationIdForStudent(studentId: String): Int {
        return NOTIFICATION_ID_BASE + ((studentId.hashCode() and 0x7FFFFFFF) % 10000)
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Veli-Öğretmen Mesajları",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Veli ve öğretmen arasındaki anlık mesaj bildirimleri"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun showIncomingMessageNotification(
        context: Context,
        message: ChatMessage,
        isRecipientTeacher: Boolean,
        unreadCountForStudent: Int = 1
    ) {
        createNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
            if (permission != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val notifId = notificationIdForStudent(message.studentId)
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_CHAT_STUDENT_ID, message.studentId)
            putExtra(EXTRA_OPEN_CHAT_IS_TEACHER, isRecipientTeacher)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isRecipientTeacher) {
            val studentLabel = listOf(message.studentNumber, message.studentName)
                .filter { it.isNotBlank() }
                .joinToString(" ")
            val countSuffix = if (unreadCountForStudent > 1) " ($unreadCountForStudent yeni mesaj)" else ""
            "💬 ${message.senderName} ($studentLabel)$countSuffix"
        } else {
            val countSuffix = if (unreadCountForStudent > 1) " ($unreadCountForStudent yeni mesaj)" else ""
            "💬 Öğretmen ${message.senderName}$countSuffix"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setGroup(GROUP_KEY_MESSAGES)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val compatManager = NotificationManagerCompat.from(context)
        try {
            compatManager.notify(notifId, notification)
            postGroupSummaryIfNeeded(context, compatManager, title, message.text, notifId)
        } catch (_: SecurityException) {
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun postGroupSummaryIfNeeded(
        context: Context,
        compatManager: NotificationManagerCompat,
        latestTitle: String,
        latestText: String,
        latestNotifId: Int
    ) {
        val systemManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val active = try {
            systemManager.activeNotifications?.toList().orEmpty()
        } catch (_: Exception) {
            return
        }
        val otherActive = active.filter { sbn ->
            sbn.id != SUMMARY_ID_MESSAGES &&
                sbn.id != latestNotifId &&
                sbn.notification.group == GROUP_KEY_MESSAGES
        }
        val totalCount = otherActive.size + 1
        if (totalCount < 2) return

        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle("💬 $totalCount Sohbette Yeni Mesaj")
            .setSummaryText("$totalCount öğrenciden okunmamış mesaj var")
        inboxStyle.addLine("$latestTitle • ${latestText.take(60)}")
        otherActive.take(4).forEach { sbn ->
            val t = sbn.notification.extras?.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString().orEmpty()
            val m = sbn.notification.extras?.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString().orEmpty()
            if (t.isNotBlank()) {
                inboxStyle.addLine("$t • ${m.take(60)}")
            }
        }

        val summaryIntent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(SystemActivityNotificationHelper.EXTRA_OPEN_NOTIFICATION_CENTER, true)
        }
        val summaryPending = PendingIntent.getActivity(
            context,
            SUMMARY_ID_MESSAGES,
            summaryIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val summaryNotif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("💬 $totalCount Öğrenciden Yeni Mesaj")
            .setContentText("Okunmamış mesajları görüntülemek için dokunun")
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setGroup(GROUP_KEY_MESSAGES)
            .setGroupSummary(true)
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setContentIntent(summaryPending)
            .build()

        compatManager.notify(SUMMARY_ID_MESSAGES, summaryNotif)
    }

    fun cancelMessageNotification(context: Context, studentId: String) {
        try {
            val compatManager = NotificationManagerCompat.from(context)
            compatManager.cancel(notificationIdForStudent(studentId))
            val systemManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val remaining = try {
                systemManager?.activeNotifications?.count { sbn ->
                    sbn.id != SUMMARY_ID_MESSAGES && sbn.notification.group == GROUP_KEY_MESSAGES
                } ?: 0
            } catch (_: Exception) {
                0
            }
            if (remaining <= 1) {
                compatManager.cancel(SUMMARY_ID_MESSAGES)
            }
        } catch (_: Exception) {
        }
    }

    fun cancelAllMessageNotifications(context: Context, studentIds: Collection<String>) {
        val manager = try {
            NotificationManagerCompat.from(context)
        } catch (_: Exception) {
            return
        }
        studentIds.distinct().forEach { studentId ->
            try {
                manager.cancel(notificationIdForStudent(studentId))
            } catch (_: Exception) {
            }
        }
        try {
            manager.cancel(SUMMARY_ID_MESSAGES)
        } catch (_: Exception) {
        }
    }
}
