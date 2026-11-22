package com.example.util

import android.util.Log
import com.example.data.model.BirizNotification
import com.example.data.model.NotificationType
import com.example.data.model.UserRole
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Bulut taraflı FCM anlık bildirimlerini karşılar ve yenilenen cihaz token'ını
 * Firestore'daki kullanıcı belgesine senkronize eder.
 */
class BirizFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (token.isBlank() || com.example.data.repository.ClassRegistryRepository.isGlobalAccountDeleting) return
        val uid = try {
            Firebase.auth.currentUser?.uid
        } catch (_: Exception) {
            null
        } ?: return

        serviceScope.launch {
            if (com.example.data.repository.ClassRegistryRepository.isGlobalAccountDeleting) return@launch
            try {
                val db = FirestoreProvider.get(applicationContext)
                db.collection("users").document(uid)
                    .update(
                        mapOf(
                            "fcmToken" to token,
                            "fcmUpdatedAt" to FieldValue.serverTimestamp()
                        )
                    )
            } catch (e: Exception) {
                Log.w("BirizFCM", "Skipped saving refreshed FCM token: ${e.message}")
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        val appPrefs = applicationContext.getSharedPreferences("biriz_app_settings", MODE_PRIVATE)
        val role = UserRole.fromCode(appPrefs.getString("key_saved_role", "") ?: "")
        val classId = appPrefs.getString("key_linked_classroom_id", "")?.trim().orEmpty()
        val email = appPrefs.getString("key_user_email", "").orEmpty()

        val isTeacher = role == UserRole.TEACHER
        val viewerKey = when (role) {
            UserRole.TEACHER -> "teacher"
            UserRole.PARENT -> ChatVisibilityHelper.viewerDocumentKey(false, email)
            else -> "default"
        }

        val syncType = data["syncType"]
        if (syncType == "sync_tick" || data.isEmpty()) {
            serviceScope.launch {
                try {
                    BackgroundNotificationSyncHelper.runSyncOnce(applicationContext)
                } catch (_: Exception) {
                }
            }
            return
        }

        val rawTitle = data["title"] ?: message.notification?.title ?: return
        val rawBody = data["body"] ?: data["message"] ?: message.notification?.body ?: ""
        val notifId = data["notifId"]?.takeIf { it.isNotBlank() } ?: "fcm_${System.currentTimeMillis()}"
        val parsedType = data["type"]?.let {
            try {
                NotificationType.valueOf(it)
            } catch (_: Exception) {
                NotificationType.ANNOUNCEMENT
            }
        } ?: NotificationType.ANNOUNCEMENT

        val notif = BirizNotification(
            id = notifId,
            title = rawTitle,
            message = rawBody,
            timestamp = System.currentTimeMillis(),
            type = parsedType,
            targetSubPageName = data["targetSubPage"] ?: "HOME",
            targetId = data["targetId"]?.takeIf { it.isNotBlank() },
            isRead = false
        )

        val prefs = NotificationPreferencesHelper.loadPreferences(applicationContext, viewerKey)
        if (!NotificationPreferencesHelper.isNotificationEnabled(prefs, notif)) return

        if (classId.isNotBlank() && classId != "default") {
            val existing = CommunicationPersistenceHelper.loadNotifications(applicationContext, classId, viewerKey)
            if (existing.none { it.id == notif.id }) {
                val updated = (listOf(notif) + existing).distinctBy { it.id }.take(150)
                CommunicationPersistenceHelper.saveNotifications(applicationContext, classId, updated, viewerKey)
            }
        }

        val quietConfig = NotificationPreferencesHelper.loadQuietHours(applicationContext, viewerKey)
        val teacherDeferral = if (!isTeacher && classId.isNotBlank() && classId != "default") {
            NotificationPreferencesHelper.loadTeacherParentDeferral(applicationContext, classId)
        } else {
            ParentNotificationDeferralConfig(enabled = false)
        }
        val shouldDefer = if (isTeacher) {
            NotificationPreferencesHelper.shouldDeferForQuietHours(quietConfig, notif)
        } else {
            NotificationPreferencesHelper.shouldDeferForParent(quietConfig, teacherDeferral, notif)
        }
        if (shouldDefer) {
            NotificationPreferencesHelper.enqueueDeferredNotifications(
                applicationContext,
                viewerKey,
                listOf(DeferredSystemNotification(notif, isTeacher))
            )
            BackgroundNotificationSyncHelper.scheduleQuietHoursFlush(
                context = applicationContext,
                config = quietConfig,
                viewerKey = viewerKey,
                teacherDeferral = teacherDeferral
            )
        } else {
            SystemActivityNotificationHelper.showNotification(
                context = applicationContext,
                notification = notif,
                isRecipientTeacher = isTeacher
            )
        }
    }
}
