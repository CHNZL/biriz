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

    private const val NOTIFICATION_ID_BASE = 3000

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
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        } catch (_: SecurityException) {
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelMessageNotification(context: Context, studentId: String) {
        try {
            NotificationManagerCompat.from(context).cancel(notificationIdForStudent(studentId))
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
    }
}
