package com.example.util

import com.example.data.model.Announcement
import com.example.data.model.AnnouncementPoll
import com.example.data.model.AnnouncementTargetType
import com.example.data.model.PollOption
import com.example.data.model.Student

/**
 * Duyuru anket oylamaları ve okunma/teslim raporlarında:
 * - Öğretmenin her oy ve okuma kaydında velinin HANGİ ÖĞRENCİNİN velisi olduğunu net görmesini,
 * - Ankette oy kullandıktan sonra cevabın kilitlenip değiştirilememesini (tek oy kuralı)
 * deterministik şekilde sağlar.
 */
object AnnouncementIdentityHelper {

    fun formatStudentParentLabel(student: Student, rawParentName: String?): String {
        val cleanNum = student.studentNumber.trim()
        val numPrefix = if (cleanNum.isNotBlank()) "$cleanNum " else ""
        val studentPart = "${numPrefix}${student.fullName.trim()}"
        val cleanParent = rawParentName
            ?.trim()
            ?.takeIf { it.isNotBlank() && !it.equals("Veli", ignoreCase = true) }

        return if (cleanParent != null) {
            if (cleanParent.contains(student.fullName.trim(), ignoreCase = true)) {
                cleanParent
            } else {
                "$studentPart Velisi ($cleanParent)"
            }
        } else {
            "$studentPart Velisi"
        }
    }

    fun formatParentActionLabel(
        activeStudent: Student?,
        linkedStudents: List<Student>,
        rawParentName: String
    ): String {
        val cleanRaw = rawParentName.trim().ifBlank { "Veli" }
        val distinctLinked = (listOfNotNull(activeStudent) + linkedStudents)
            .distinctBy { it.id }

        if (distinctLinked.isEmpty()) {
            return cleanRaw
        }
        if (distinctLinked.size == 1) {
            return formatStudentParentLabel(distinctLinked.first(), cleanRaw)
        }

        val studentSummary = distinctLinked.joinToString(" & ") { s ->
            val num = s.studentNumber.trim()
            if (num.isNotBlank()) "$num ${s.fullName.trim()}" else s.fullName.trim()
        }
        val cleanParent = cleanRaw.takeIf { !it.equals("Veli", ignoreCase = true) }
        return if (cleanParent != null) {
            "$studentSummary Velisi ($cleanParent)"
        } else {
            "$studentSummary Velisi"
        }
    }

    fun formatExpectedRecipientsForAnnouncement(
        targetType: AnnouncementTargetType,
        targetStudentIds: List<String>,
        allStudents: List<Student>
    ): List<String> {
        val targets = if (targetType == AnnouncementTargetType.ALL_CLASS) {
            allStudents
        } else {
            allStudents.filter { it.id in targetStudentIds }
        }
        return targets.map { student ->
            val primaryParentName = student.allLinkedParents.firstOrNull()?.name ?: student.parentName
            formatStudentParentLabel(student, primaryParentName)
        }
    }

    fun resolveDisplayLabelForTeacher(rawLabel: String, students: List<Student>): String {
        val trimmed = rawLabel.trim()
        if (trimmed.isEmpty()) return rawLabel
        if (trimmed.contains("Velisi (", ignoreCase = true)) return trimmed

        val matchedByParentName = students.filter { s ->
            s.parentName?.trim().equals(trimmed, ignoreCase = true) ||
                s.allLinkedParents.any { it.name.trim().equals(trimmed, ignoreCase = true) } ||
                "${s.fullName.trim()} Velisi".equals(trimmed, ignoreCase = true) ||
                "${s.studentNumber.trim()} ${s.fullName.trim()} Velisi".trim().equals(trimmed, ignoreCase = true)
        }
        if (matchedByParentName.size == 1) {
            val s = matchedByParentName.first()
            val effectiveParent = if ("${s.fullName.trim()} Velisi".equals(trimmed, ignoreCase = true)) {
                s.allLinkedParents.firstOrNull()?.name ?: s.parentName
            } else {
                trimmed
            }
            return formatStudentParentLabel(s, effectiveParent)
        }
        if (matchedByParentName.size > 1) {
            val studentsDesc = matchedByParentName.joinToString(" & ") { s ->
                val num = s.studentNumber.trim()
                if (num.isNotBlank()) "$num ${s.fullName.trim()}" else s.fullName.trim()
            }
            return "$studentsDesc Velisi ($trimmed)"
        }
        return trimmed
    }

    fun voterAliasesForParent(
        activeStudent: Student?,
        linkedStudents: List<Student>,
        rawParentName: String
    ): Set<String> {
        val aliases = mutableSetOf<String>()
        val cleanRaw = rawParentName.trim()
        if (cleanRaw.isNotEmpty()) {
            aliases.add(cleanRaw)
        }
        val combinedLabel = formatParentActionLabel(activeStudent, linkedStudents, cleanRaw)
        if (combinedLabel.isNotBlank()) aliases.add(combinedLabel)

        val allRelevant = (listOfNotNull(activeStudent) + linkedStudents).distinctBy { it.id }
        allRelevant.forEach { s ->
            aliases.add(formatStudentParentLabel(s, cleanRaw))
            aliases.add(formatStudentParentLabel(s, s.parentName))
            aliases.add("${s.fullName.trim()} Velisi")
            val numPrefixed = "${s.studentNumber.trim()} ${s.fullName.trim()} Velisi".trim()
            if (numPrefixed.isNotBlank()) aliases.add(numPrefixed)
            s.parentName?.trim()?.takeIf { it.isNotEmpty() }?.let { aliases.add(it) }
            s.allLinkedParents.forEach { lp ->
                val lpName = lp.name.trim()
                if (lpName.isNotEmpty()) {
                    aliases.add(lpName)
                    aliases.add(formatStudentParentLabel(s, lpName))
                }
            }
        }
        return aliases
    }

    fun isOptionVotedByParent(option: PollOption, voterAliases: Set<String>): Boolean {
        val normalizedAliases = voterAliases.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
        return option.voterParentNames.any { existing ->
            existing.trim().lowercase() in normalizedAliases
        }
    }

    fun findVotedOption(poll: AnnouncementPoll, voterAliases: Set<String>): PollOption? {
        return poll.options.firstOrNull { isOptionVotedByParent(it, voterAliases) }
    }

    fun hasVotedInPoll(poll: AnnouncementPoll, voterAliases: Set<String>): Boolean {
        return findVotedOption(poll, voterAliases) != null
    }

    /**
     * Tek oy kuralı: Kullanıcı bu ankette daha önce oy verdiyse null döner ve oy değişikliğine izin vermez.
     */
    fun applySingleVote(
        poll: AnnouncementPoll,
        optionId: String,
        voterLabel: String,
        voterAliases: Set<String>
    ): AnnouncementPoll? {
        val allAliases = voterAliases + voterLabel
        if (hasVotedInPoll(poll, allAliases)) {
            return null
        }
        val targetExists = poll.options.any { it.id == optionId }
        if (!targetExists) return null

        val updatedOptions = poll.options.map { opt ->
            if (opt.id == optionId) {
                opt.copy(voterParentNames = opt.voterParentNames + voterLabel)
            } else {
                opt
            }
        }
        return poll.copy(options = updatedOptions)
    }

    fun findStudentReadTimestamp(student: Student, readReceipts: Map<String, Long>): Long? {
        if (readReceipts.isEmpty()) return null
        val candidateKeys = buildList {
            add(formatStudentParentLabel(student, student.allLinkedParents.firstOrNull()?.name ?: student.parentName))
            add("${student.studentNumber.trim()} ${student.fullName.trim()} Velisi".trim())
            add("${student.fullName.trim()} Velisi")
            student.parentName?.trim()?.takeIf { it.isNotEmpty() }?.let { p ->
                add(p)
                add(formatStudentParentLabel(student, p))
            }
            student.allLinkedParents.forEach { lp ->
                val n = lp.name.trim()
                if (n.isNotEmpty()) {
                    add(n)
                    add(formatStudentParentLabel(student, n))
                }
            }
        }.map { it.trim().lowercase() }.toSet()

        readReceipts.forEach { (key, ts) ->
            val normKey = key.trim().lowercase()
            if (normKey in candidateKeys) {
                return ts
            }
            val fullNameLower = student.fullName.trim().lowercase()
            if (fullNameLower.isNotEmpty() && normKey.contains("$fullNameLower velisi")) {
                return ts
            }
            if (fullNameLower.isNotEmpty() && normKey.contains(fullNameLower) && normKey.contains("velisi")) {
                return ts
            }
        }
        return null
    }

    fun resolveAnnouncementReadSummary(
        announcement: Announcement,
        allStudents: List<Student>
    ): Pair<Int, Int> {
        val targetStudents = if (announcement.targetType == AnnouncementTargetType.ALL_CLASS) {
            allStudents
        } else {
            allStudents.filter { it.id in announcement.targetStudentIds }
        }
        if (targetStudents.isEmpty()) {
            return announcement.readCount to announcement.deliveredCount
        }
        val readStudentCount = targetStudents.count { findStudentReadTimestamp(it, announcement.readReceipts) != null }
        val effectiveRead = maxOf(readStudentCount, announcement.readCount)
        val effectiveDelivered = maxOf(targetStudents.size, announcement.deliveredCount)
        return effectiveRead.coerceAtMost(effectiveDelivered) to effectiveDelivered
    }
}
