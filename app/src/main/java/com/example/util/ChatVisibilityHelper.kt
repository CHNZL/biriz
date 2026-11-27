package com.example.util

import com.example.data.model.ChatMessage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * WhatsApp mantığında kullanıcı bazlı mesaj görünürlüğü, geçmiş temizleme zaman damgaları
 * ve sohbet tarih ayırıcılarını deterministik olarak yöneten yardımcı katman.
 *
 * Her kullanıcının (Öğretmen veya Veli) sildiği/temizlediği geçmiş yalnızca kendi görünümünü etkiler;
 * karşı tarafın sohbet geçmişine dokunulmaz.
 */
object ChatVisibilityHelper {

    fun normalizeParentKey(parentEmail: String, studentId: String = ""): String {
        val trimmed = parentEmail.trim().lowercase()
        return if (trimmed.isNotBlank() && trimmed != "veli@biriz.app") {
            trimmed
        } else {
            "parent_${studentId.ifBlank { "default" }}"
        }
    }

    fun viewerDocumentKey(viewerIsTeacher: Boolean, userEmail: String): String {
        return if (viewerIsTeacher) {
            "teacher"
        } else {
            val trimmed = userEmail.trim().lowercase()
            if (trimmed.isNotBlank() && trimmed != "veli@biriz.app") {
                "parent_$trimmed"
            } else {
                "parent_default"
            }
        }
    }

    fun teacherConversationClearKeys(
        studentId: String,
        parentEmail: String,
        isSingleParentStudent: Boolean
    ): List<String> {
        val cleanEmail = parentEmail.trim().lowercase()
        val keys = mutableListOf<String>()
        if (cleanEmail.isNotBlank()) {
            keys.add("$studentId|$cleanEmail")
        }
        if (cleanEmail.isBlank() || isSingleParentStudent) {
            keys.add(studentId)
        }
        return keys.distinct()
    }

    fun parentConversationClearKeys(studentId: String): List<String> = listOf(studentId)

    fun resolvedTeacherClearTimestamp(
        studentId: String,
        parentEmail: String,
        clearTimestamps: Map<String, Long>
    ): Long {
        val cleanEmail = parentEmail.trim().lowercase()
        val exact = if (cleanEmail.isNotBlank()) {
            clearTimestamps["$studentId|$cleanEmail"] ?: 0L
        } else {
            0L
        }
        val studentWide = clearTimestamps[studentId] ?: 0L
        return maxOf(exact, studentWide)
    }

    /**
     * Veli oturumunda bir sohbet mesajının yalnızca o velinin bağlı öğrencisine ve
     * (çoklu veli durumunda) o velinin kendi özel sohbetine ait olduğunu fail-closed doğrular.
     */
    fun isMessageOwnedByParentViewer(
        msg: ChatMessage,
        linkedParentStudentIds: Collection<String>,
        currentParentEmail: String
    ): Boolean {
        if (linkedParentStudentIds.isEmpty()) return false
        if (msg.studentId.isBlank() || msg.studentId !in linkedParentStudentIds) return false

        val msgEmail = msg.parentEmail.trim().lowercase()
        val currEmail = currentParentEmail.trim().lowercase()
        if (msgEmail.isNotBlank() && msgEmail != "veli@biriz.app") {
            if (currEmail.isNotBlank() && currEmail != "veli@biriz.app") {
                return msgEmail == currEmail
            }
        }
        return true
    }

    fun isMessageVisibleToViewer(
        msg: ChatMessage,
        viewerIsTeacher: Boolean,
        viewerIsParent: Boolean,
        currentParentEmail: String,
        clearTimestamps: Map<String, Long>,
        nowMillis: Long = System.currentTimeMillis(),
        linkedParentStudentIds: Collection<String>? = null
    ): Boolean {
        return when {
            viewerIsTeacher -> {
                if (msg.hiddenForTeacher) return false
                val clearTs = resolvedTeacherClearTimestamp(
                    studentId = msg.studentId,
                    parentEmail = msg.parentEmail,
                    clearTimestamps = clearTimestamps
                )
                msg.timestamp > clearTs
            }
            viewerIsParent -> {
                if (linkedParentStudentIds != null && !isMessageOwnedByParentViewer(
                        msg = msg,
                        linkedParentStudentIds = linkedParentStudentIds,
                        currentParentEmail = currentParentEmail
                    )
                ) {
                    return false
                }
                if (msg.isPendingScheduled(nowMillis)) {
                    return false
                }
                val parentKey = normalizeParentKey(currentParentEmail, msg.studentId)
                val fallbackDefaultKey = "parent_${msg.studentId}"
                if (parentKey in msg.hiddenForParentEmails || fallbackDefaultKey in msg.hiddenForParentEmails) {
                    return false
                }
                val clearTs = clearTimestamps[msg.studentId] ?: 0L
                msg.timestamp > clearTs
            }
            else -> false
        }
    }

    fun filterVisibleMessages(
        messages: List<ChatMessage>,
        viewerIsTeacher: Boolean,
        viewerIsParent: Boolean,
        currentParentEmail: String,
        clearTimestamps: Map<String, Long>,
        nowMillis: Long = System.currentTimeMillis(),
        linkedParentStudentIds: Collection<String>? = null
    ): List<ChatMessage> {
        return messages.filter { msg ->
            isMessageVisibleToViewer(
                msg = msg,
                viewerIsTeacher = viewerIsTeacher,
                viewerIsParent = viewerIsParent,
                currentParentEmail = currentParentEmail,
                clearTimestamps = clearTimestamps,
                nowMillis = nowMillis,
                linkedParentStudentIds = linkedParentStudentIds
            )
        }
    }

    fun dayKeyForTimestamp(timestamp: Long): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return fmt.format(Date(timestamp))
    }

    fun formatChatDateDivider(timestamp: Long, now: Long = System.currentTimeMillis()): String {
        val calMsg = Calendar.getInstance().apply { timeInMillis = timestamp }
        val calNow = Calendar.getInstance().apply { timeInMillis = now }

        val sameYear = calMsg.get(Calendar.YEAR) == calNow.get(Calendar.YEAR)
        val msgDay = calMsg.get(Calendar.DAY_OF_YEAR)
        val nowDay = calNow.get(Calendar.DAY_OF_YEAR)

        if (sameYear && msgDay == nowDay) return "Bugün"

        val calYesterday = (calNow.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
        if (calMsg.get(Calendar.YEAR) == calYesterday.get(Calendar.YEAR) &&
            calMsg.get(Calendar.DAY_OF_YEAR) == calYesterday.get(Calendar.DAY_OF_YEAR)
        ) {
            return "Dün"
        }

        val trLocale = Locale("tr", "TR")
        return SimpleDateFormat("d MMMM yyyy", trLocale).format(Date(timestamp))
    }
}
