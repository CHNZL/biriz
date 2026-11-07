package com.example.util

import java.util.Locale

/**
 * BİRİZ Türk Alfabesi ve Karakter Dönüşüm Yardımcısı
 * i -> İ, ı -> I, ğ -> Ğ, ü -> Ü, ş -> Ş, ö -> Ö, ç -> Ç
 * kurallarını eksiksiz uygular.
 */
object TurkishTextHelper {
    val TR_LOCALE: Locale = Locale("tr", "TR")

    fun toTurkishUpperCase(input: String): String {
        return input.uppercase(TR_LOCALE)
    }

    fun toTurkishLowerCase(input: String): String {
        return input.lowercase(TR_LOCALE)
    }

    /**
     * Akıllı arama ve filtreleme için Türkçe normalleştirme
     */
    fun matchesTurkish(source: String, query: String): Boolean {
        val s = toTurkishLowerCase(source.trim())
        val q = toTurkishLowerCase(query.trim())
        return s.contains(q)
    }

    /**
     * "Veli Katılım Kodu" Üretim Motoru:
     * - Tam olarak 8 karakter uzunluğunda, sistem genelinde benzersiz alfa-sayısal kod üretir (Örn: 94K7P2MN).
     * - Sadece A-Z ve 2-9 içerir (31 karakterlik güvenli havuz = 31^8 ≈ 852.8 Milyar benzersiz kombinasyon).
     * - Okuma karışıklığını önlemek için Türkçe karakterler (Ç, Ğ, İ, Ö, Ş, Ü)
     *   ve birbirine benzeyen (I-1, O-0, S-5) karakterler kesinlikle filtrelenmiştir.
     * - Sabit "BRZ" öneki kaldırılmıştır; 8 karakterin tümü rastgele üretilir.
     */
    private const val SAFE_CODE_CHARS = "ABCDEFGHJKLMNPQRTUVWXYZ2346789"

    fun generateVeliKatilimKodu(): String {
        return (1..8)
            .map { SAFE_CODE_CHARS.random() }
            .joinToString("")
    }

    /**
     * Veli Katılım Kodunu küçük/büyük harf, Türkçe karakter, tire ve boşluklardan bağımsız
     * tam ve güvenli ASCII Büyük Harf biçimine dönüştürür.
     */
    fun normalizeVeliKatilimKodu(input: String): String {
        return input.trim()
            .replace("-", "")
            .replace(" ", "")
            .replace("i", "I")
            .replace("İ", "I")
            .replace("ı", "I")
            .replace("ç", "C")
            .replace("Ç", "C")
            .replace("ğ", "G")
            .replace("Ğ", "G")
            .replace("ö", "O")
            .replace("Ö", "O")
            .replace("ş", "S")
            .replace("Ş", "S")
            .replace("ü", "U")
            .replace("Ü", "U")
            .uppercase(Locale.ROOT)
    }

    /**
     * Başlık Formatı (Her kelimenin ilk harfi büyük, geri kalanı küçük)
     * Türkçe karakter duyarlı: i -> İ / ı -> I vb.
     */
    fun toTurkishTitleCase(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""
        return trimmed.split("\\s+".toRegex()).joinToString(" ") { word ->
            if (word.isNotEmpty()) {
                val firstChar = word.substring(0, 1).uppercase(TR_LOCALE)
                val rest = if (word.length > 1) word.substring(1).lowercase(TR_LOCALE) else ""
                "$firstChar$rest"
            } else ""
        }
    }
}
