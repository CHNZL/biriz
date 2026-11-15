package com.example.util

import com.example.data.model.LinkedParentAccount
import com.example.data.model.Student
import java.util.Locale

data class BulkMessageRecipient(
    val student: Student,
    val parent: LinkedParentAccount
) {
    val key: String
        get() = BulkMessageHelper.recipientKey(student.id, parent.email)
}

/**
 * Öğretmen toplu mesajlaşma akışı için saf alıcı çözümleme,
 * tekilleştirme ve kişi etiketleme yardımcısı.
 */
object BulkMessageHelper {

    fun recipientKey(studentId: String, parentEmail: String): String {
        val cleanEmail = parentEmail.trim().lowercase(Locale.ROOT)
        return "${studentId.trim()}::$cleanEmail"
    }

    fun collectAllLinkedRecipients(students: List<Student>): List<BulkMessageRecipient> {
        val sorted = students
            .filter { it.isParentLinked }
            .sortedWith(compareBy<Student> { it.studentNumber.toIntOrNull() ?: Int.MAX_VALUE }.thenBy { it.fullName })

        val result = mutableListOf<BulkMessageRecipient>()
        val seenKeys = HashSet<String>()
        sorted.forEach { student ->
            student.allLinkedParents.forEach { parent ->
                val key = recipientKey(student.id, parent.email)
                if (seenKeys.add(key)) {
                    result.add(BulkMessageRecipient(student = student, parent = parent))
                }
            }
        }
        return result
    }

    fun collectPrimaryParentRecipients(students: List<Student>): List<BulkMessageRecipient> {
        val sorted = students
            .filter { it.isParentLinked }
            .sortedWith(compareBy<Student> { it.studentNumber.toIntOrNull() ?: Int.MAX_VALUE }.thenBy { it.fullName })

        val result = mutableListOf<BulkMessageRecipient>()
        val seenKeys = HashSet<String>()
        sorted.forEach { student ->
            val primary = student.allLinkedParents.firstOrNull() ?: return@forEach
            val key = recipientKey(student.id, primary.email)
            if (seenKeys.add(key)) {
                result.add(BulkMessageRecipient(student = student, parent = primary))
            }
        }
        return result
    }

    fun deduplicateRecipients(recipients: List<BulkMessageRecipient>): List<BulkMessageRecipient> {
        if (recipients.isEmpty()) return emptyList()
        val seen = HashSet<String>()
        return recipients.filter { seen.add(it.key) }
    }

    fun filterLinkedStudents(students: List<Student>, query: String): List<Student> {
        val linked = students
            .filter { it.isParentLinked }
            .sortedWith(compareBy<Student> { it.studentNumber.toIntOrNull() ?: Int.MAX_VALUE }.thenBy { it.fullName })

        val q = query.trim()
        if (q.isEmpty()) return linked

        val qFolded = TurkishTextHelper.toTurkishUpperCase(q)
        return linked.filter { student ->
            TurkishTextHelper.toTurkishUpperCase(student.fullName).contains(qFolded) ||
                student.studentNumber.contains(q, ignoreCase = true) ||
                student.allLinkedParents.any { p ->
                    TurkishTextHelper.toTurkishUpperCase(p.name).contains(qFolded) ||
                        p.email.contains(q, ignoreCase = true) ||
                        TurkishTextHelper.toTurkishUpperCase(p.relationship).contains(qFolded)
                }
        }
    }

    fun personalizeBulkMessageText(
        template: String,
        studentName: String,
        parentName: String
    ): String {
        val resolvedParent = parentName.trim().ifBlank { "Değerli Velimiz" }
        val resolvedStudent = studentName.trim().ifBlank { "Öğrencimiz" }
        val veliRegex = Regex("\\{[vV][eE][lL][iİıI]\\}")
        val ogrenciRegex = Regex("\\{[oöOÖ][gğGĞ][rR][eE][nN][cC][iİıI]\\}")
        return template
            .replace(veliRegex, resolvedParent)
            .replace(ogrenciRegex, resolvedStudent)
            .trim()
    }
}
