package com.example.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SentimentSatisfiedAlt
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Yıldız Kategorisi Modeli (Ders ve Davranış/Değerler Rozetleri)
 */
data class StarCategory(
    val id: String,
    val name: String,
    val isAcademic: Boolean,
    val colorHex: Long,
    val iconKey: String
) {
    val icon: ImageVector
        get() = when (iconKey) {
            "hayat_bilgisi" -> Icons.Default.Favorite
            "sosyal_bilgiler" -> Icons.Default.Public
            "fen_bilimleri" -> Icons.Default.Science
            "matematik" -> Icons.Default.Psychology
            "turkce" -> Icons.Default.Language
            "ingilizce" -> Icons.Default.Language
            "gorsel_sanatlar" -> Icons.Default.Palette
            "muzik" -> Icons.Default.MusicNote
            "beden_egitimi" -> Icons.Default.EmojiEvents
            "kitap_kurdu" -> Icons.Default.MenuBook
            "sorumluluk" -> Icons.Default.Shield
            "yardimsever" -> Icons.Default.VolunteerActivism
            "temiz_duzenli" -> Icons.Default.AutoAwesome
            "nazik" -> Icons.Default.SentimentSatisfiedAlt
            "azimli" -> Icons.Default.TrendingUp
            "isbirlikci" -> Icons.Default.Groups
            "ogretmen_ozel" -> Icons.Default.Diamond
            else -> Icons.Default.Star
        }

    companion object {
        val ALL_CATEGORIES = listOf(
            // 1. Dersler (Akademik)
            StarCategory("hayat_bilgisi", "Hayat Bilgisi", true, 0xFF10B981, "hayat_bilgisi"),
            StarCategory("sosyal_bilgiler", "Sosyal Bilgiler", true, 0xFFF97316, "sosyal_bilgiler"),
            StarCategory("fen_bilimleri", "Fen Bilimleri", true, 0xFF0EA5E9, "fen_bilimleri"),
            StarCategory("matematik", "Matematik", true, 0xFF8B5CF6, "matematik"),
            StarCategory("turkce", "Türkçe", true, 0xFFF43F5E, "turkce"),
            StarCategory("ingilizce", "İngilizce", true, 0xFFEC4899, "ingilizce"),
            StarCategory("gorsel_sanatlar", "Görsel Sanatlar", true, 0xFFA855F7, "gorsel_sanatlar"),
            StarCategory("muzik", "Müzik", true, 0xFF6366F1, "muzik"),
            StarCategory("beden_egitimi", "Beden Eğitimi", true, 0xFF14B8A6, "beden_egitimi"),

            // 2. Değerler ve Örnek Davranışlar
            StarCategory("kitap_kurdu", "Kitap Kurdu", false, 0xFFF59E0B, "kitap_kurdu"),
            StarCategory("sorumluluk", "Sorumluluk Sahibi Öğrenci", false, 0xFF3B82F6, "sorumluluk"),
            StarCategory("yardimsever", "Yardımsever Öğrenci", false, 0xFF22C55E, "yardimsever"),
            StarCategory("temiz_duzenli", "Temiz ve Düzenli Öğrenci", false, 0xFF06B6D4, "temiz_duzenli"),
            StarCategory("nazik", "Nazik Öğrenci", false, 0xFFD946EF, "nazik"),
            StarCategory("azimli", "Azimli Öğrenci", false, 0xFF6366F1, "azimli"),
            StarCategory("isbirlikci", "İşbirlikçi Öğrenci", false, 0xFF84CC16, "isbirlikci"),
            StarCategory("ogretmen_ozel", "Öğretmen Özel Ödülü", false, 0xFFEF4444, "ogretmen_ozel")
        )

        fun findById(id: String): StarCategory =
            ALL_CATEGORIES.firstOrNull { it.id == id } ?: ALL_CATEGORIES.first()
    }
}

/**
 * Verilen Yıldız İşlem Kaydı
 */
data class StarRewardRecord(
    val id: String,
    val classId: String,
    val studentId: String,
    val studentName: String,
    val studentNumber: String,
    val categoryId: String,
    val categoryName: String,
    val starCount: Int,
    val note: String,
    val isLuckyBonus: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Etkinlikli Yıldız Dağıtım Ayarları Taslağı (Adım 1)
 */
data class ActivityStarDraft(
    val categoryId: String = "matematik",
    val note: String = "",
    val starCount: Int = 1,
    val timeLimitSeconds: Int = 0, // 0 = Süresiz
    val targetStudentLimit: Int = 0 // 0 = Sınırsız
)
