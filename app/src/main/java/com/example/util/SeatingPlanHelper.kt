package com.example.util

import com.example.data.model.SeatingConfig
import com.example.data.model.SeatingPlacementMode
import com.example.data.model.SeatingPlanState
import com.example.data.model.Student
import java.util.Locale

data class SeatCoordinate(
    val groupIndex: Int,
    val rowIndex: Int,
    val seatIndex: Int
) {
    val seatId: String get() = "g$groupIndex-r$rowIndex-s$seatIndex"
}

object SeatingPlanHelper {

    /**
     * Koltuk kimliğini ayrıştırır: `g{groupIndex}-r{rowIndex}-s{seatIndex}`
     */
    fun parseSeatId(seatId: String): SeatCoordinate? {
        val parts = seatId.split("-")
        if (parts.size != 3) return null
        val g = parts[0].removePrefix("g").toIntOrNull() ?: return null
        val r = parts[1].removePrefix("r").toIntOrNull() ?: return null
        val s = parts[2].removePrefix("s").toIntOrNull() ?: return null
        return SeatCoordinate(g, r, s)
    }

    /**
     * Sınıftaki tüm koltuk kimliklerini ön sıradan (r=0) arka sıraya doğru sıralı listeler.
     */
    fun generateAllSeatIdsFrontToBack(config: SeatingConfig): List<String> {
        val maxRows = (0 until config.groupCount).maxOfOrNull { config.rowsPerGroup.getOrElse(it) { 5 } } ?: 5
        val list = mutableListOf<String>()

        for (r in 0 until maxRows) {
            for (g in 0 until config.groupCount) {
                val groupRowCount = config.rowsPerGroup.getOrElse(g) { 5 }
                if (r < groupRowCount) {
                    for (s in 0 until config.peoplePerRow) {
                        list.add("g$g-r$r-s$s")
                    }
                }
            }
        }
        return list
    }

    /**
     * Belirli bir grup ve satırdaki sıra koltuk kimliklerini döner.
     */
    fun getDeskSeatIds(groupIndex: Int, rowIndex: Int, peoplePerRow: Int): List<String> {
        return (0 until peoplePerRow).map { s -> "g$groupIndex-r$rowIndex-s$s" }
    }

    /**
     * Akıllı Yerleştirme Algoritması
     * - Sabitlenmiş (pinned) öğrencileri ASLA yerinden oynatmaz.
     * - Kalan boş koltuklara öğrencileri seçilen algoritmaya göre yerleştirir.
     */
    fun autoAssignSeats(
        students: List<Student>,
        currentState: SeatingPlanState,
        mode: SeatingPlacementMode
    ): SeatingPlanState {
        val config = currentState.config
        val allSeatIds = generateAllSeatIdsFrontToBack(config)

        // 1. Sabitlenmiş koltukları ve öğrencileri koru
        val preservedAssignments = mutableMapOf<String, String>()
        val fixedStudentIds = mutableSetOf<String>()

        currentState.assignments.forEach { (seatId, studentId) ->
            if (currentState.pinnedStudents.contains(studentId) && allSeatIds.contains(seatId)) {
                preservedAssignments[seatId] = studentId
                fixedStudentIds.add(studentId)
            }
        }

        // 2. Dağıtılacak serbest öğrencileri belirle
        val studentsToPlace = students.filter { !fixedStudentIds.contains(it.id) }.toMutableList()

        // 3. Yerleşime açık boş koltukları belirle (Ön sıralar öncelikli sırada)
        val availableSeatIds = allSeatIds.filter { !preservedAssignments.containsKey(it) }

        // 4. Modlara Göre Öğrencileri Sırala / Eşle
        val newAssignments = preservedAssignments.toMutableMap()

        when (mode) {
            SeatingPlacementMode.PRIORITY_FRONT -> {
                // Öncelikli öğrenciler (gözlük/boy/işitme) en ön sıralara
                val priorityList = studentsToPlace.filter {
                    currentState.priorityStudents.contains(it.id) || it.isPriority
                }.sortedBy { it.fullName }

                val nonPriorityList = studentsToPlace.filter {
                    !currentState.priorityStudents.contains(it.id) && !it.isPriority
                }.shuffled()

                val orderedStudents = priorityList + nonPriorityList

                for (i in availableSeatIds.indices) {
                    if (i < orderedStudents.size) {
                        newAssignments[availableSeatIds[i]] = orderedStudents[i].id
                    }
                }
            }

            SeatingPlacementMode.GENDER_BALANCED -> {
                // Kız ve Erkek öğrencileri yan yana veya eşit dengeli sıralara yerleştir
                val girls = studentsToPlace.filter { it.gender.equals("KIZ", ignoreCase = true) }.shuffled().toMutableList()
                val boys = studentsToPlace.filter { !it.gender.equals("KIZ", ignoreCase = true) }.shuffled().toMutableList()

                // Masaları çiftler halinde eşleştir
                val deskSeatPairs = mutableListOf<List<String>>()
                for (g in 0 until config.groupCount) {
                    val rowCount = config.rowsPerGroup.getOrElse(g) { 5 }
                    for (r in 0 until rowCount) {
                        val deskSeats = getDeskSeatIds(g, r, config.peoplePerRow).filter { !newAssignments.containsKey(it) }
                        if (deskSeats.isNotEmpty()) {
                            deskSeatPairs.add(deskSeats)
                        }
                    }
                }

                deskSeatPairs.forEach { deskSeats ->
                    if (deskSeats.size >= 2) {
                        // 1 Kız, 1 Erkek yan yana
                        if (girls.isNotEmpty()) {
                            newAssignments[deskSeats[0]] = girls.removeAt(0).id
                        } else if (boys.isNotEmpty()) {
                            newAssignments[deskSeats[0]] = boys.removeAt(0).id
                        }

                        if (boys.isNotEmpty()) {
                            newAssignments[deskSeats[1]] = boys.removeAt(0).id
                        } else if (girls.isNotEmpty()) {
                            newAssignments[deskSeats[1]] = girls.removeAt(0).id
                        }

                        // Eğer sıra 3'lü ise kalan koltuk
                        if (deskSeats.size > 2) {
                            if (girls.isNotEmpty()) {
                                newAssignments[deskSeats[2]] = girls.removeAt(0).id
                            } else if (boys.isNotEmpty()) {
                                newAssignments[deskSeats[2]] = boys.removeAt(0).id
                            }
                        }
                    } else if (deskSeats.size == 1) {
                        if (girls.isNotEmpty()) {
                            newAssignments[deskSeats[0]] = girls.removeAt(0).id
                        } else if (boys.isNotEmpty()) {
                            newAssignments[deskSeats[0]] = boys.removeAt(0).id
                        }
                    }
                }
            }

            SeatingPlacementMode.RANDOM -> {
                val shuffled = studentsToPlace.shuffled()
                for (i in availableSeatIds.indices) {
                    if (i < shuffled.size) {
                        newAssignments[availableSeatIds[i]] = shuffled[i].id
                    }
                }
            }

            SeatingPlacementMode.ALPHABETICAL -> {
                val collator = java.text.Collator.getInstance(TurkishTextHelper.TR_LOCALE)
                val sorted = studentsToPlace.sortedWith { a, b -> collator.compare(a.fullName, b.fullName) }
                for (i in availableSeatIds.indices) {
                    if (i < sorted.size) {
                        newAssignments[availableSeatIds[i]] = sorted[i].id
                    }
                }
            }

            SeatingPlacementMode.NUMBER_ORDER -> {
                val sorted = studentsToPlace.sortedBy { it.studentNumber.toIntOrNull() ?: 9999 }
                for (i in availableSeatIds.indices) {
                    if (i < sorted.size) {
                        newAssignments[availableSeatIds[i]] = sorted[i].id
                    }
                }
            }

            SeatingPlacementMode.MANUAL,
            SeatingPlacementMode.PLANNED -> {
                // Manuel veya Planlı yerleştirme ekranları kendi özel sihirbazları/arayüzleri üzerinden yönetilir
            }
        }

        return currentState.copy(assignments = newAssignments)
    }

    /**
     * Planlı Yerleştirme Sihirbazı Algoritması:
     * - Önceki (referans) plana göre:
     *   1. Sabit öğrencileri seçilen koltuklara yerleştirir ve kilitler.
     *   2. Öncelikli öğrencileri öğretmen masasına en yakın 1. sıralardan başlayarak öne dizer.
     *   3. Kalan öğrencileri belirlenen kurallara (farklı grup, farklı sıra, farklı sıra arkadaşı, kız-erkek dengeli)
     *      göre en uygun boş sıralara yerleştirir.
     */
    fun generatePlannedSeating(
        students: List<Student>,
        config: SeatingConfig,
        referencePlanState: SeatingPlanState?,
        rules: com.example.data.model.PlannedSeatingRules,
        fixedPlacements: List<com.example.data.model.FixedStudentPlacement>,
        priorityStudentIds: List<String>,
        planName: String
    ): SeatingPlanState {
        val allSeatIds = generateAllSeatIdsFrontToBack(config)
        val newAssignments = mutableMapOf<String, String>()
        val assignedStudentIds = mutableSetOf<String>()

        // 1. ADIM: Sabit öğrencileri hedef koltuklara kilitle
        val pinnedSet = mutableSetOf<String>()
        fixedPlacements.forEach { fixed ->
            if (allSeatIds.contains(fixed.seatId) && students.any { it.id == fixed.studentId }) {
                newAssignments[fixed.seatId] = fixed.studentId
                assignedStudentIds.add(fixed.studentId)
                pinnedSet.add(fixed.studentId)
            }
        }

        // 2. ADIM: Öncelikli öğrencileri öğretmen masasına en yakın sıralardan başlayarak yerleştir
        val orderedGroups = when (config.deskPosition) {
            com.example.data.model.DeskPosition.LEFT -> (0 until config.groupCount).toList()
            com.example.data.model.DeskPosition.RIGHT -> (config.groupCount - 1 downTo 0).toList()
            com.example.data.model.DeskPosition.CENTER -> {
                val mid = config.groupCount / 2
                val list = mutableListOf(mid)
                var offset = 1
                while (list.size < config.groupCount) {
                    if (mid - offset >= 0) list.add(mid - offset)
                    if (mid + offset < config.groupCount) list.add(mid + offset)
                    offset++
                }
                list
            }
        }

        val prioritySeatOrder = mutableListOf<String>()
        val maxRows = (0 until config.groupCount).maxOfOrNull { config.rowsPerGroup.getOrElse(it) { 5 } } ?: 5
        for (r in 0 until maxRows) {
            for (g in orderedGroups) {
                val rowCount = config.rowsPerGroup.getOrElse(g) { 5 }
                if (r < rowCount) {
                    for (s in 0 until config.peoplePerRow) {
                        prioritySeatOrder.add("g$g-r$r-s$s")
                    }
                }
            }
        }

        val availablePrioritySeats = prioritySeatOrder.filter { !newAssignments.containsKey(it) }
        var prioSeatIdx = 0
        priorityStudentIds.forEach { prioId ->
            if (!assignedStudentIds.contains(prioId) && prioSeatIdx < availablePrioritySeats.size) {
                val seatId = availablePrioritySeats[prioSeatIdx++]
                newAssignments[seatId] = prioId
                assignedStudentIds.add(prioId)
            }
        }

        // 3. ADIM: Kalan öğrencileri referans plana göre kurallarla yerleştir
        val remainingStudents = students.filter { !assignedStudentIds.contains(it.id) }.shuffled().toMutableList()
        val availableSeatIds = allSeatIds.filter { !newAssignments.containsKey(it) }.toMutableList()

        // Öğrencinin referans plandaki geçmiş koltuk bilgisi
        val prevLocations = mutableMapOf<String, SeatCoordinate>()
        val prevNeighbors = mutableMapOf<String, String>() // studentId -> neighborStudentId

        if (referencePlanState != null) {
            referencePlanState.assignments.forEach { (seatId, studentId) ->
                val coord = parseSeatId(seatId)
                if (coord != null) {
                    prevLocations[studentId] = coord
                    for (s in 0 until referencePlanState.config.peoplePerRow) {
                        if (s != coord.seatIndex) {
                            val neighborSeatId = "g${coord.groupIndex}-r${coord.rowIndex}-s$s"
                            val neighborId = referencePlanState.assignments[neighborSeatId]
                            if (neighborId != null && neighborId.isNotBlank()) {
                                prevNeighbors[studentId] = neighborId
                            }
                        }
                    }
                }
            }
        }

        // Kalan koltuklara yerleştirirken kural puanlaması (Minimum ceza)
        for (seatId in availableSeatIds) {
            if (remainingStudents.isEmpty()) break
            val coord = parseSeatId(seatId) ?: continue

            var currentDeskNeighbor: Student? = null
            for (s in 0 until config.peoplePerRow) {
                if (s != coord.seatIndex) {
                    val otherSeat = "g${coord.groupIndex}-r${coord.rowIndex}-s$s"
                    val nId = newAssignments[otherSeat]
                    if (nId != null) {
                        currentDeskNeighbor = students.firstOrNull { it.id == nId }
                        break
                    }
                }
            }

            var bestStudent: Student = remainingStudents[0]
            var minPenalty = Int.MAX_VALUE

            for (candidate in remainingStudents) {
                var penalty = kotlin.random.Random.nextInt(0, 5)
                val prevCoord = prevLocations[candidate.id]

                // Kural 1: Farklı Gruba Yerleştirilsin
                if (rules.differentGroup && prevCoord != null && prevCoord.groupIndex == coord.groupIndex) {
                    penalty += 120
                }

                // Kural 2: Farklı Sıraya Yerleştirilsin
                if (rules.differentRow && prevCoord != null && prevCoord.rowIndex == coord.rowIndex) {
                    penalty += 80
                }

                // Kural 3: Farklı Kişi İle Otursun
                val prevNeighbor = prevNeighbors[candidate.id]
                if (rules.differentNeighbor && prevNeighbor != null && currentDeskNeighbor != null && currentDeskNeighbor.id == prevNeighbor) {
                    penalty += 350
                }

                // Kural 4: Kız - Erkek Karışık Otursun
                if (rules.genderMixed && currentDeskNeighbor != null) {
                    val isCandidateGirl = candidate.gender.equals("Kız", ignoreCase = true)
                    val isNeighborGirl = currentDeskNeighbor.gender.equals("Kız", ignoreCase = true)
                    if (isCandidateGirl == isNeighborGirl) {
                        penalty += 60
                    } else {
                        penalty -= 40
                    }
                }

                // Kural 5: Yan Yana Oturmaması Gerekenler (Uyumsuz İkililer Çapraz Kuralı)
                if (rules.incompatiblePairs.isNotEmpty() && currentDeskNeighbor != null) {
                    val isIncompatible = rules.incompatiblePairs.any { (s1, s2) ->
                        (s1 == candidate.id && s2 == currentDeskNeighbor.id) ||
                        (s2 == candidate.id && s1 == currentDeskNeighbor.id)
                    }
                    if (isIncompatible) {
                        penalty += 100000 // Çok yüksek ceza puanıyla yan yana oturtulmaz
                    }
                }

                if (penalty < minPenalty) {
                    minPenalty = penalty
                    bestStudent = candidate
                }
            }

            newAssignments[seatId] = bestStudent.id
            assignedStudentIds.add(bestStudent.id)
            remainingStudents.remove(bestStudent)
        }

        return SeatingPlanState(
            config = config,
            assignments = newAssignments,
            pinnedStudents = pinnedSet,
            priorityStudents = priorityStudentIds.toSet(),
            activePlanName = planName.ifBlank { SeatingPlanPersistenceHelper.generateDefaultPlanName() }
        )
    }

    /**
     * Dönemsel Kaydırmalı Rotasyon (Önden Arkaya Kaydır):
     * Sıralar 1 arkaya kayar; en arkadaki sıra en öne geçer.
     * Sabitlenmiş (🔒 Kilitli) öğrenciler kendi yerlerinde korunur.
     */
    fun rotateSeatsFrontToBack(
        config: SeatingConfig,
        currentAssignments: Map<String, String>,
        pinnedStudents: Set<String>
    ): Map<String, String> {
        val newAssignments = currentAssignments.toMutableMap()
        for (g in 0 until config.groupCount) {
            val rowCount = config.rowsPerGroup.getOrElse(g) { 5 }
            for (s in 0 until config.peoplePerRow) {
                val columnSeats = (0 until rowCount).map { r -> "g$g-r$r-s$s" }
                val rotatableSeats = columnSeats.filter { seatId ->
                    val studentId = currentAssignments[seatId]
                    studentId == null || !pinnedStudents.contains(studentId)
                }
                if (rotatableSeats.size > 1) {
                    val currentOccupants = rotatableSeats.map { currentAssignments[it] }
                    // 1 geriye kaydır: sonuncu başa geçer
                    val rotatedOccupants = listOf(currentOccupants.last()) + currentOccupants.dropLast(1)
                    for (i in rotatableSeats.indices) {
                        val seatId = rotatableSeats[i]
                        val studentId = rotatedOccupants[i]
                        if (studentId != null) {
                            newAssignments[seatId] = studentId
                        } else {
                            newAssignments.remove(seatId)
                        }
                    }
                }
            }
        }
        return newAssignments
    }

    /**
     * Dönemsel Grup Rotasyonu (Grupları Döndür):
     * Sol grup ortaya, orta sağa, sağ sola geçer.
     * Sabitlenmiş (🔒 Kilitli) öğrenciler yerlerinde korunur.
     */
    fun rotateGroups(
        config: SeatingConfig,
        currentAssignments: Map<String, String>,
        pinnedStudents: Set<String>
    ): Map<String, String> {
        if (config.groupCount <= 1) return currentAssignments
        val newAssignments = currentAssignments.toMutableMap()
        val rotatableAssignments = currentAssignments.filter { (_, studentId) ->
            !pinnedStudents.contains(studentId)
        }

        val groupShifted = mutableMapOf<String, String>()
        rotatableAssignments.forEach { (seatId, studentId) ->
            val coord = parseSeatId(seatId)
            if (coord != null) {
                val nextGroup = (coord.groupIndex + 1) % config.groupCount
                val nextRowCount = config.rowsPerGroup.getOrElse(nextGroup) { 5 }
                val nextRow = minOf(coord.rowIndex, nextRowCount - 1)
                val targetSeatId = "g$nextGroup-r$nextRow-s${coord.seatIndex}"
                groupShifted[targetSeatId] = studentId
            }
        }

        rotatableAssignments.keys.forEach { newAssignments.remove(it) }
        groupShifted.forEach { (seatId, studentId) ->
            val existingStudentId = newAssignments[seatId]
            if (existingStudentId == null || !pinnedStudents.contains(existingStudentId)) {
                newAssignments[seatId] = studentId
            }
        }
        return newAssignments
    }

    /**
     * İki koltuk arasında öğrenci yer değişimi (Swap) veya boş koltuğa taşıma yapar.
     */
    fun swapSeats(
        assignments: Map<String, String>,
        seatId1: String,
        seatId2: String
    ): Map<String, String> {
        val mutable = assignments.toMutableMap()
        val student1 = mutable[seatId1]
        val student2 = mutable[seatId2]

        if (student1 != null && student2 != null) {
            mutable[seatId1] = student2
            mutable[seatId2] = student1
        } else if (student1 != null) {
            mutable.remove(seatId1)
            mutable[seatId2] = student1
        } else if (student2 != null) {
            mutable.remove(seatId2)
            mutable[seatId1] = student2
        }
        return mutable
    }

    /**
     * Belirli bir koltuğa öğrenci atar veya koltuğu boşaltır.
     * Öğrenci başka bir koltukta oturuyorsa eski koltuğundan otomatik kaldırılır.
     */
    fun assignStudent(
        assignments: Map<String, String>,
        seatId: String,
        studentId: String?
    ): Map<String, String> {
        val mutable = assignments.toMutableMap()
        if (studentId == null) {
            mutable.remove(seatId)
            return mutable
        }

        // Eğer öğrenci zaten bir koltuktaysa eski koltuğunu boşalt
        val existingSeat = mutable.entries.firstOrNull { it.value == studentId }?.key
        if (existingSeat != null) {
            mutable.remove(existingSeat)
        }

        mutable[seatId] = studentId
        return mutable
    }

    /**
     * İlk açılışta sınıf listesindeki öğrencileri varsayılan olarak sıralara yerleştirir.
     */
    fun generateInitialAssignments(students: List<Student>, config: SeatingConfig): Map<String, String> {
        val seatIds = generateAllSeatIdsFrontToBack(config)
        val map = mutableMapOf<String, String>()
        val sortedStudents = students.sortedBy { it.studentNumber.toIntOrNull() ?: 9999 }

        for (i in seatIds.indices) {
            if (i < sortedStudents.size) {
                map[seatIds[i]] = sortedStudents[i].id
            }
        }
        return map
    }
}
