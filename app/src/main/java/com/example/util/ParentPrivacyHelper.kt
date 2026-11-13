package com.example.util

import com.example.data.model.StarRewardRecord
import com.example.data.model.Student
import com.example.data.model.TournamentMatch
import com.example.data.model.TournamentStanding

data class MaskedClassStarRank(
    val rank: Int,
    val studentId: String,
    val displayName: String,
    val starCount: Int,
    val isMyChild: Boolean,
    val isActiveChild: Boolean
)

/**
 * BİRİZ Veli Modülü KVKK ve Veri Gizliliği Yardımcısı
 *
 * Temel İlke:
 * Hiçbir veli, velisi olduğu öğrenci(ler) dışındaki diğer sınıf arkadaşlarının
 * gerçek isimlerini, numaralarını veya kişisel bilgilerini göremez.
 * Diğer tüm öğrenciler "Öğrenci" veya anonim biçimde maskelenir.
 */
object ParentPrivacyHelper {

    /**
     * Sınıftaki TÜM öğrencilerin yıldız sıralamasını KVKK'ya uygun biçimde oluşturur.
     * Kaç öğrenci varsa hepsi dahil edilir; kendi çocuğu/çocukları açık, diğerleri "Öğrenci" olarak gösterilir.
     */
    fun buildMaskedClassStarRanking(
        allStudents: List<Student>,
        allClassStars: List<StarRewardRecord>,
        linkedStudentIds: Set<String>,
        activeStudentId: String
    ): List<MaskedClassStarRank> {
        val starSumByStudent = allClassStars.groupBy { it.studentId }
            .mapValues { (_, list) -> list.sumOf { it.starCount } }
        val sorted = allStudents.sortedWith(
            compareByDescending<Student> { starSumByStudent[it.id] ?: 0 }
                .thenByDescending { it.id == activeStudentId }
                .thenByDescending { linkedStudentIds.contains(it.id) }
        )
        return sorted.mapIndexed { index, s ->
            val isMine = linkedStudentIds.contains(s.id)
            MaskedClassStarRank(
                rank = index + 1,
                studentId = s.id,
                displayName = if (isMine) "${s.fullName} (Çocuğunuz)" else "Öğrenci",
                starCount = starSumByStudent[s.id] ?: 0,
                isMyChild = isMine,
                isActiveChild = s.id == activeStudentId
            )
        }
    }

    /**
     * Öğrenci ismini velinin bağlı çocuklarına göre maskeler.
     */
    fun maskStudentName(
        studentId: String?,
        studentFullName: String?,
        linkedStudentIds: Set<String>
    ): String {
        if (studentId.isNullOrBlank() || studentFullName.isNullOrBlank()) {
            return "Öğrenci"
        }
        if (studentId == "BYE") {
            return "BAY (Tur Atladı)"
        }
        return if (linkedStudentIds.contains(studentId)) {
            studentFullName
        } else {
            "Öğrenci"
        }
    }

    /**
     * Öğrenci numarasını maskeler.
     */
    fun maskStudentNumber(
        studentId: String?,
        studentNumber: String?,
        linkedStudentIds: Set<String>
    ): String {
        if (studentId.isNullOrBlank() || studentNumber.isNullOrBlank()) {
            return "-"
        }
        return if (linkedStudentIds.contains(studentId)) {
            studentNumber
        } else {
            "-"
        }
    }

    /**
     * Sıralama tablosundaki (standings) öğrencileri maskeler.
     * Çocuğun kendi sırası ve puanı tam görünürken, diğer öğrenciler "Öğrenci" olarak anonimleştirilir.
     */
    fun maskStandings(
        standings: List<TournamentStanding>,
        linkedStudentIds: Set<String>
    ): List<TournamentStanding> {
        return standings.map { item ->
            if (linkedStudentIds.contains(item.playerId)) {
                item
            } else {
                item.copy(playerName = "Öğrenci")
            }
        }
    }

    /**
     * Turnuva maç eşleşmelerini veli için maskeler.
     * Kendi çocuğunun adı açık görünür, rakip ise "Öğrenci" olarak görünür.
     */
    fun maskTournamentMatch(
        match: TournamentMatch,
        linkedStudentIds: Set<String>
    ): TournamentMatch {
        val p1Masked = maskStudentName(match.player1Id, match.player1Name, linkedStudentIds)
        val p2Masked = maskStudentName(match.player2Id, match.player2Name, linkedStudentIds)
        return match.copy(
            player1Name = p1Masked,
            player2Name = p2Masked
        )
    }

    /**
     * Oturma planında belirli bir sıra hücresinin etiketini veliye göre maskeler.
     */
    fun getDeskDisplayInfo(
        studentId: String?,
        student: Student?,
        linkedStudentIds: Set<String>
    ): Pair<String, Boolean> {
        if (studentId.isNullOrBlank() || student == null) {
            return Pair("Boş Sıra", false)
        }
        val isChild = linkedStudentIds.contains(studentId)
        return if (isChild) {
            Pair(student.fullName, true)
        } else {
            Pair("Öğrenci", false)
        }
    }
}
