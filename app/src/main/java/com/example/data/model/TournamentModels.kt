package com.example.data.model

import java.util.UUID

/**
 * Turnuva Türleri
 */
enum class TournamentType(val displayName: String, val icon: String) {
    ELEME("Eleme", "🏆"),
    LIG("Lig", "📊"),
    GRUP("Grup", "👥"),
    GRUP_ELEME("Grup + Eleme", "🌍")
}

/**
 * Karşılaşma Biçimi
 */
enum class TournamentMatchType(val displayName: String) {
    TEK_MAC("Tek Maç"),
    CIFT_MAC("Çift Maç (Rövanşlı)")
}

/**
 * Kazanan Belirleme Yöntemi
 */
enum class WinnerSelectionMethod(val displayName: String) {
    WINNER("Galip Seçimi"),
    SCORE("Skor Girişi")
}

/**
 * Turnuva Durumu
 */
enum class TournamentStatus(val displayName: String) {
    DEVAM_EDIYOR("Devam Ediyor"),
    TAMAMLANDI("Tamamlandı")
}

/**
 * Yıldızlar Sınıfı Ödül Konfigürasyonu
 */
data class TournamentStarRewards(
    val enabled: Boolean = true,
    val firstPlaceStars: Int = 10,       // Şampiyon ödülü (Örn: 10 ⭐)
    val secondPlaceStars: Int = 6,       // 2.lik ödülü (Örn: 6 ⭐)
    val thirdPlaceStars: Int = 4,        // 3.lük ödülü (Örn: 4 ⭐)
    val participationStars: Int = 1,     // Her katılımcıya teselli yıldızı (Örn: 1 ⭐)
    val winBonusStars: Int = 0,          // Maç başı galibiyet primi (Opsiyonel)
    val distributed: Boolean = false,    // Dağıtıldı mı?
    val distributedAt: Long? = null
)

/**
 * Turnuva Modeli
 */
data class Tournament(
    val id: String = UUID.randomUUID().toString(),
    val classRoomId: String,
    val name: String,                                          // Örn: "Sınıf Satranç Turnuvası"
    val description: String = "",
    val type: TournamentType = TournamentType.ELEME,
    val matchType: TournamentMatchType = TournamentMatchType.TEK_MAC, // Çift maçta rövanşlı toplam skor
    val fixtureType: String = "all",                           // 'all': Tüm ağaç baştan hazır, 'round-by-round': Tur bittikçe kura
    val winnerSelectionMethod: WinnerSelectionMethod = WinnerSelectionMethod.WINNER, // 'WINNER': Galip seç, 'SCORE': Skor/Sayı gir
    val currentRound: Int = 1,                                  // Aktif tur/hafta no
    val participants: List<String> = emptyList(),              // Öğrenci ID'leri veya "extra:MisafirAdı"
    val extraParticipants: List<String> = emptyList(),          // Sınıf dışı misafir katılımcılar
    val status: TournamentStatus = TournamentStatus.DEVAM_EDIYOR,
    val winnerName: String? = null,
    val winnerId: String? = null,
    val runnerUpName: String? = null,
    val runnerUpId: String? = null,
    val thirdPlaceName: String? = null,
    val thirdPlaceId: String? = null,
    val teacherUid: String = "",
    val pointsWin: Int = 3,                                    // Galibiyet puanı (Örn: 3)
    val pointsDraw: Int = 1,                                   // Beraberlik puanı (Örn: 1)
    val pointsLoss: Int = 0,                                   // Mağlubiyet puanı (Örn: 0)
    val groupCount: Int = 2,                                   // Grup sayısı (2, 3, 4, 6, 8)
    val groupNaming: String = "letters",                       // 'letters': A, B, C | 'colors': Kırmızı, Mavi
    val advancingPerGroup: Int = 2,                            // Gruptan çıkacak kişi sayısı (Örn: 2)
    val currentStage: String = "group",                        // Grup+Eleme için aktif aşama: 'group' | 'knockout'
    val advancingPlayers: List<String> = emptyList(),          // Gruptan eleme ağacına yükselenler
    val starRewards: TournamentStarRewards = TournamentStarRewards(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Karşılaşma (Maç) Modeli
 */
data class TournamentMatch(
    val id: String = UUID.randomUUID().toString(),
    val tournamentId: String,
    val round: Int,                                            // Tur / Hafta Numarası
    val player1Id: String,                                     // Öğrenci ID, "BYE" veya "WINNER_OF_R1-M1"
    val player2Id: String,
    val player1Name: String = "",
    val player2Name: String = "",
    val winnerId: String? = null,
    val score1: Int? = null,
    val score2: Int? = null,
    val status: String = "pending",                            // 'pending' | 'completed'
    val nextMatchId: String? = null,
    val matchCode: String = "",                                // Örn: "R1-M1", "G1-W1-M1"
    val isDoubleMatch: Boolean = false,                        // Rövanşlı maç bayrağı
    val pairCode: String? = null,                              // Çift maçları birbirine bağlayan kod
    val isTieBreaker: Boolean = false,                         // Beraberlik bozma maçı mı?
    val isThirdPlaceMatch: Boolean = false,                    // 3.lük maçı mı? (Yarı final mağlupları)
    val player1Source: String? = null,                         // Örn: "WINNER:R1-M1", "LOSER:R2-M1"
    val player2Source: String? = null,                         // Örn: "WINNER:R1-M2", "LOSER:R2-M2"
    val groupId: String? = null,                               // Ait olduğu grup ID'si (G1, G2...)
    val groupName: String? = null,                             // Ait olduğu grup adı (A Grubu, Kırmızı...)
    val stage: String = "knockout",                            // 'group' | 'knockout'
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Canlı Lig / Grup Puan Tablosu Satırı
 */
data class TournamentStanding(
    val playerId: String,
    val playerName: String,
    val played: Int = 0,
    val won: Int = 0,
    val drawn: Int = 0,
    val lost: Int = 0,
    val goalsFor: Int = 0,
    val goalsAgainst: Int = 0,
    val goalDiff: Int = 0,
    val points: Int = 0
)

/**
 * Hazır Branş Şablonu
 */
data class ActivityPreset(
    val id: String,
    val title: String,
    val icon: String,
    val defaultType: TournamentType,
    val defaultMatchType: TournamentMatchType,
    val defaultWinnerMethod: WinnerSelectionMethod,
    val description: String
) {
    companion object {
        val PRESETS = listOf(
            ActivityPreset(
                id = "chess",
                title = "Satranç",
                icon = "♟️",
                defaultType = TournamentType.ELEME,
                defaultMatchType = TournamentMatchType.TEK_MAC,
                defaultWinnerMethod = WinnerSelectionMethod.WINNER,
                description = "Eleme formatı, tek maç, doğrudan kazanan seçimi"
            ),
            ActivityPreset(
                id = "table_tennis",
                title = "Masa Tenisi",
                icon = "🏓",
                defaultType = TournamentType.ELEME,
                defaultMatchType = TournamentMatchType.TEK_MAC,
                defaultWinnerMethod = WinnerSelectionMethod.SCORE,
                description = "Eleme formatı, tek maç, set skoru girişi"
            ),
            ActivityPreset(
                id = "mangala",
                title = "Mangala",
                icon = "🎲",
                defaultType = TournamentType.LIG,
                defaultMatchType = TournamentMatchType.TEK_MAC,
                defaultWinnerMethod = WinnerSelectionMethod.WINNER,
                description = "Lig formatı, herkes herkesle, doğrudan galip seçimi"
            ),
            ActivityPreset(
                id = "mind_games",
                title = "Akıl ve Zeka Oyunları",
                icon = "🎯",
                defaultType = TournamentType.GRUP_ELEME,
                defaultMatchType = TournamentMatchType.TEK_MAC,
                defaultWinnerMethod = WinnerSelectionMethod.WINNER,
                description = "Önce grup maçları, ardından çeyrek/yarı final eleme ağacı"
            ),
            ActivityPreset(
                id = "football",
                title = "Futsal / Futbol",
                icon = "⚽",
                defaultType = TournamentType.GRUP_ELEME,
                defaultMatchType = TournamentMatchType.CIFT_MAC,
                defaultWinnerMethod = WinnerSelectionMethod.SCORE,
                description = "Grup maçları ardından çift maçlı (rövanşlı) eleme turları"
            ),
            ActivityPreset(
                id = "quiz",
                title = "Bilgi Yarışması",
                icon = "🧠",
                defaultType = TournamentType.ELEME,
                defaultMatchType = TournamentMatchType.TEK_MAC,
                defaultWinnerMethod = WinnerSelectionMethod.SCORE,
                description = "Eleme formatı, tek maç, puan/soru sayısı girişi"
            )
        )
    }
}
