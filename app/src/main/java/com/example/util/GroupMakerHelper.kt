package com.example.util

import com.example.data.model.ClassRoom
import com.example.data.model.GroupColorTheme
import com.example.data.model.GroupDistributionConfig
import com.example.data.model.GroupDistributionMode
import com.example.data.model.GroupTeam
import com.example.data.model.Student
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.ceil

object GroupMakerHelper {

    val FUN_GROUP_NAMES = listOf(
        "Kartallar", "Yıldızlar", "Kaşifler", "Mucitler",
        "Bilginler", "Gezginler", "Dahiler", "Şampiyonlar",
        "Fırtınalar", "Gezegenler", "Atom Karıncalar", "Pusulalar"
    )

    fun calculateGroupCount(studentCount: Int, config: GroupDistributionConfig): Int {
        if (studentCount <= 0) return 1
        return when (config.mode) {
            GroupDistributionMode.BY_GROUP_COUNT -> {
                config.targetGroupCount.coerceIn(1, 12)
            }
            GroupDistributionMode.BY_STUDENTS_PER_GROUP -> {
                val targetPerGroup = config.targetStudentsPerGroup.coerceIn(1, 15)
                val count = ceil(studentCount.toDouble() / targetPerGroup).toInt()
                count.coerceIn(1, 12)
            }
        }
    }

    /**
     * Akıllı Dağıtım Algoritması:
     * - Cinsiyet dengeleme aktifken: Erkekler ve kızlar ayrı karıştırılır,
     *   erkekler gruplara sırayla paylaştırılır; kızlar erkeklerin bittiği ofsetten itibaren
     *   dağıtılarak hem toplam kişi hem kız/erkek oranları dengelenir.
     * - Pasifken: Tüm öğrenciler tamamen rastgele harmanlanarak gruplara atanır.
     */
    fun distributeStudents(
        allStudents: List<Student>,
        config: GroupDistributionConfig,
        existingGroups: List<GroupTeam> = emptyList()
    ): Pair<List<GroupTeam>, List<String>> {
        val activeStudents = allStudents.filter { !config.excludedStudentIds.contains(it.id) }
        val unassignedIds = allStudents.filter { config.excludedStudentIds.contains(it.id) }.map { it.id }

        val targetCount = calculateGroupCount(activeStudents.size, config)

        // Mevcut grupların isim/renklerini koru veya yenilerini üret
        val colorThemes = GroupColorTheme.values()
        val groups = (0 until targetCount).map { idx ->
            val existing = existingGroups.getOrNull(idx)
            if (existing != null) {
                existing.copy(studentIds = emptyList(), leaderStudentId = null)
            } else {
                val presetName = FUN_GROUP_NAMES.getOrNull(idx) ?: "${idx + 1}. Grup"
                GroupTeam(
                    id = UUID.randomUUID().toString(),
                    name = presetName,
                    colorTheme = colorThemes[idx % colorThemes.size],
                    studentIds = emptyList(),
                    leaderStudentId = null
                )
            }
        }

        if (activeStudents.isEmpty()) {
            return Pair(groups, unassignedIds)
        }

        val buckets = List(targetCount) { mutableListOf<String>() }

        if (config.balanceGender) {
            val boys = activeStudents.filter { it.gender.equals("ERKEK", ignoreCase = true) }.shuffled()
            val girls = activeStudents.filter { !it.gender.equals("ERKEK", ignoreCase = true) }.shuffled()

            boys.forEachIndexed { index, boy ->
                buckets[index % targetCount].add(boy.id)
            }

            val boyOffset = boys.size % targetCount
            girls.forEachIndexed { index, girl ->
                buckets[(index + boyOffset) % targetCount].add(girl.id)
            }
        } else {
            val shuffled = activeStudents.shuffled()
            shuffled.forEachIndexed { index, student ->
                buckets[index % targetCount].add(student.id)
            }
        }

        val finalGroups = groups.mapIndexed { idx, group ->
            val members = buckets[idx]
            group.copy(
                studentIds = members,
                leaderStudentId = null
            )
        }

        return Pair(finalGroups, unassignedIds)
    }

    /**
     * Mevcut grupları, isimleri ve renkleri bozmadan öğrencileri sıfırdan yeniden karıştırır.
     */
    fun reshuffleStudents(
        allStudents: List<Student>,
        currentGroups: List<GroupTeam>,
        balanceGender: Boolean
    ): List<GroupTeam> {
        if (currentGroups.isEmpty()) return emptyList()

        val currentlyAssignedIds = currentGroups.flatMap { it.studentIds }.toSet()
        val studentsToShuffle = allStudents.filter { currentlyAssignedIds.contains(it.id) }
        val count = currentGroups.size

        val buckets = List(count) { mutableListOf<String>() }

        if (balanceGender) {
            val boys = studentsToShuffle.filter { it.gender.equals("ERKEK", ignoreCase = true) }.shuffled()
            val girls = studentsToShuffle.filter { !it.gender.equals("ERKEK", ignoreCase = true) }.shuffled()

            boys.forEachIndexed { index, boy ->
                buckets[index % count].add(boy.id)
            }

            val boyOffset = boys.size % count
            girls.forEachIndexed { index, girl ->
                buckets[(index + boyOffset) % count].add(girl.id)
            }
        } else {
            val shuffled = studentsToShuffle.shuffled()
            shuffled.forEachIndexed { index, student ->
                buckets[index % count].add(student.id)
            }
        }

        return currentGroups.mapIndexed { idx, group ->
            val members = buckets[idx]
            val validLeader = if (members.contains(group.leaderStudentId)) group.leaderStudentId else null
            group.copy(studentIds = members, leaderStudentId = validLeader)
        }
    }

    /**
     * Açıkta kalan (atanmamış) öğrencileri mevcut gruplara eşit ve dengeli paylaştırır.
     */
    fun distributeUnassigned(
        unassignedIds: List<String>,
        allStudents: List<Student>,
        currentGroups: List<GroupTeam>,
        balanceGender: Boolean = true
    ): Pair<List<GroupTeam>, List<String>> {
        if (unassignedIds.isEmpty() || currentGroups.isEmpty()) {
            return Pair(currentGroups, unassignedIds)
        }

        val unassignedStudents = allStudents.filter { unassignedIds.contains(it.id) }
        val workingGroups = currentGroups.map { it.studentIds.toMutableList() }

        val studentsToAdd = if (balanceGender) {
            val boys = unassignedStudents.filter { it.gender.equals("ERKEK", ignoreCase = true) }.shuffled()
            val girls = unassignedStudents.filter { !it.gender.equals("ERKEK", ignoreCase = true) }.shuffled()
            boys + girls
        } else {
            unassignedStudents.shuffled()
        }

        studentsToAdd.forEach { student ->
            // En az üyeye sahip grubu bul
            var minIndex = 0
            var minSize = workingGroups[0].size
            for (i in 1 until workingGroups.size) {
                if (workingGroups[i].size < minSize) {
                    minSize = workingGroups[i].size
                    minIndex = i
                }
            }
            workingGroups[minIndex].add(student.id)
        }

        val updatedGroups = currentGroups.mapIndexed { idx, group ->
            group.copy(studentIds = workingGroups[idx])
        }

        return Pair(updatedGroups, emptyList())
    }

    /**
     * Tüm gruplardan tek tıkla rastgele birer sözcü / lider seçer.
     */
    fun pickRandomLeadersForAll(groups: List<GroupTeam>): List<GroupTeam> {
        return groups.map { group ->
            if (group.studentIds.isNotEmpty()) {
                val randomStudentId = group.studentIds.random()
                group.copy(leaderStudentId = randomStudentId)
            } else {
                group.copy(leaderStudentId = null)
            }
        }
    }

    /**
     * Tek bir grup için rastgele lider seçer.
     */
    fun pickRandomLeaderForGroup(group: GroupTeam): GroupTeam {
        return if (group.studentIds.isNotEmpty()) {
            val randomStudentId = group.studentIds.random()
            group.copy(leaderStudentId = randomStudentId)
        } else {
            group.copy(leaderStudentId = null)
        }
    }

    /**
     * İki öğrenciyi gruplar arasında karşılıklı takas eder (Swap).
     */
    fun swapStudents(
        groups: List<GroupTeam>,
        studentAId: String,
        studentBId: String
    ): List<GroupTeam> {
        if (studentAId == studentBId) return groups

        var groupAIndex = -1
        var groupBIndex = -1

        groups.forEachIndexed { index, group ->
            if (group.studentIds.contains(studentAId)) groupAIndex = index
            if (group.studentIds.contains(studentBId)) groupBIndex = index
        }

        if (groupAIndex == -1 || groupBIndex == -1) return groups

        val updatedGroups = groups.toMutableList()

        if (groupAIndex == groupBIndex) {
            // Aynı gruptalar, yerleri değiştiğinde liste aynı kalır
            return groups
        }

        val groupA = updatedGroups[groupAIndex]
        val groupB = updatedGroups[groupBIndex]

        val listA = groupA.studentIds.toMutableList()
        val listB = groupB.studentIds.toMutableList()

        val idxA = listA.indexOf(studentAId)
        val idxB = listB.indexOf(studentBId)

        if (idxA != -1 && idxB != -1) {
            listA[idxA] = studentBId
            listB[idxB] = studentAId
        }

        val leaderA = if (groupA.leaderStudentId == studentAId) studentBId else groupA.leaderStudentId
        val leaderB = if (groupB.leaderStudentId == studentBId) studentAId else groupB.leaderStudentId

        updatedGroups[groupAIndex] = groupA.copy(studentIds = listA, leaderStudentId = leaderA)
        updatedGroups[groupBIndex] = groupB.copy(studentIds = listB, leaderStudentId = leaderB)

        return updatedGroups
    }

    /**
     * Bir öğrenciyi hedef gruba taşır.
     */
    fun moveStudentToGroup(
        groups: List<GroupTeam>,
        unassigned: List<String>,
        studentId: String,
        targetGroupId: String
    ): Pair<List<GroupTeam>, List<String>> {
        val newUnassigned = unassigned.filter { it != studentId }
        val newGroups = groups.map { group ->
            val wasInThisGroup = group.studentIds.contains(studentId)
            val isTargetGroup = group.id == targetGroupId

            when {
                wasInThisGroup && isTargetGroup -> group
                wasInThisGroup && !isTargetGroup -> {
                    val remaining = group.studentIds.filter { it != studentId }
                    val newLeader = if (group.leaderStudentId == studentId) null else group.leaderStudentId
                    group.copy(studentIds = remaining, leaderStudentId = newLeader)
                }
                !wasInThisGroup && isTargetGroup -> {
                    group.copy(studentIds = group.studentIds + studentId)
                }
                else -> group
            }
        }
        return Pair(newGroups, newUnassigned)
    }

    /**
     * Öğrenciyi grubundan çıkarıp atanmamışlar havuzuna gönderir.
     */
    fun removeStudentFromGroup(
        groups: List<GroupTeam>,
        unassigned: List<String>,
        studentId: String
    ): Pair<List<GroupTeam>, List<String>> {
        val newGroups = groups.map { group ->
            if (group.studentIds.contains(studentId)) {
                val newMembers = group.studentIds.filter { it != studentId }
                val newLeader = if (group.leaderStudentId == studentId) null else group.leaderStudentId
                group.copy(studentIds = newMembers, leaderStudentId = newLeader)
            } else {
                group
            }
        }
        val newUnassigned = if (unassigned.contains(studentId)) unassigned else unassigned + studentId
        return Pair(newGroups, newUnassigned)
    }

    /**
     * Grubu siler ve içindeki öğrencileri otomatik olarak atanmamışlar havuzuna aktarır.
     */
    fun deleteGroup(
        groups: List<GroupTeam>,
        unassigned: List<String>,
        groupId: String
    ): Pair<List<GroupTeam>, List<String>> {
        val groupToDelete = groups.firstOrNull { it.id == groupId } ?: return Pair(groups, unassigned)
        val newGroups = groups.filter { it.id != groupId }
        val newUnassigned = (unassigned + groupToDelete.studentIds).distinct()
        return Pair(newGroups, newUnassigned)
    }

    /**
     * WhatsApp / SMS Formatında Paylaşım Metni Üretir.
     */
    fun generateShareText(
        classRoom: ClassRoom,
        groups: List<GroupTeam>,
        allStudents: List<Student>
    ): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("tr", "TR"))
        val dateStr = dateFormat.format(Date())

        val sb = StringBuilder()
        sb.append("🏫 ${classRoom.schoolName} - ${classRoom.gradeLevel}/${classRoom.section} Sınıfı\n")
        sb.append("👥 Sınıf Çalışma Grupları ($dateStr)\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━\n\n")

        groups.forEachIndexed { index, group ->
            val members = group.studentIds.mapNotNull { sid -> allStudents.firstOrNull { it.id == sid } }
            val leader = allStudents.firstOrNull { it.id == group.leaderStudentId }

            val leaderSuffix = if (leader != null) " [👑 Sözcü: ${leader.studentNumber} - ${leader.fullName}]" else ""
            sb.append("📌 ${group.name.uppercase(Locale("tr", "TR"))} (${members.size} Öğrenci)$leaderSuffix\n")

            members.forEachIndexed { mIdx, student ->
                val isLeader = student.id == group.leaderStudentId
                val leaderBadge = if (isLeader) " 👑 (Grup Sözcüsü)" else ""
                sb.append("  ${mIdx + 1}. No: ${student.studentNumber} - ${student.fullName}$leaderBadge\n")
            }
            sb.append("\n")
        }

        sb.append("BİRİZ Sınıf Yönetim Sistemi ile oluşturuldu ✨")
        return sb.toString()
    }
}
