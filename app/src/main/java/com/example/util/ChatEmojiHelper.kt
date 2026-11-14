package com.example.util

import android.content.Context
import java.text.BreakIterator

enum class ChatEmojiCategory(
    val title: String,
    val icon: String,
    val testTag: String
) {
    RECENT("Son Kullanılanlar", "🕒", "emoji_cat_recent"),
    SMILEYS("Yüzler & Duygular", "😀", "emoji_cat_smileys"),
    GESTURES("El & Jestler", "👋", "emoji_cat_gestures"),
    SCHOOL("Okul & Başarı", "🎓", "emoji_cat_school"),
    HEARTS("Kalpler & Tebrik", "❤️", "emoji_cat_hearts"),
    OBJECTS("Günlük & Doğa", "🌟", "emoji_cat_objects")
}

/**
 * WhatsApp tarzı kategorili emoji seçici, imleç konumuna emoji ekleme,
 * bileşik emojiyi tek hamlede silme ve son kullanılanları saklama yardımcısı.
 */
object ChatEmojiHelper {

    private const val PREFS_NAME = "biriz_chat_emoji_prefs"
    private const val KEY_RECENT_EMOJIS = "recent_emojis_v1"
    private const val MAX_RECENT = 24

    val defaultRecentEmojis = listOf(
        "😊", "👍", "🙏", "❤️", "⭐", "👏", "📚", "🎉",
        "😄", "🤝", "💯", "✅", "🌸", "🏆", "✏️", "👋"
    )

    val categoryEmojis: Map<ChatEmojiCategory, List<String>> = mapOf(
        ChatEmojiCategory.SMILEYS to listOf(
            "😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂",
            "🙂", "🙃", "😉", "😊", "😇", "🥰", "😍", "🤩",
            "😘", "😗", "😚", "😋", "😛", "😜", "🤪", "🤗",
            "🤭", "🤫", "🤔", "🫡", "😐", "😑", "😶", "😏",
            "😌", "😔", "😪", "😴", "😷", "🤒", "🤕", "🥳",
            "😎", "🤓", "🧐", "😕", "😟", "🙁", "😮", "😯",
            "😲", "😳", "🥺", "🥹", "😢", "😭", "😤", "😠"
        ),
        ChatEmojiCategory.GESTURES to listOf(
            "👋", "🤚", "🖐️", "✋", "🖖", "👌", "🤌", "🤏",
            "✌️", "🤞", "🤟", "🤘", "🤙", "👈", "👉", "👆",
            "👇", "☝️", "👍", "👎", "✊", "👊", "🤛", "🤜",
            "👏", "🙌", "🫶", "👐", "🤲", "🤝", "🙏", "💪",
            "✍️", "👀", "🗣️", "🫂"
        ),
        ChatEmojiCategory.SCHOOL to listOf(
            "📚", "📖", "📕", "📗", "📘", "📙", "📓", "📔",
            "📒", "📝", "✏️", "🖊️", "🖍️", "📐", "📏", "✂️",
            "📌", "📍", "📎", "🗂️", "📅", "📆", "⏰", "🔔",
            "🎓", "🏫", "🎒", "🧮", "🔬", "🔭", "🎨", "🎵",
            "⚽", "🏀", "♟️", "🧩", "🏆", "🥇", "🥈", "🥉",
            "🏅", "⭐", "🌟", "💯", "💡", "🧠", "✅", "☑️"
        ),
        ChatEmojiCategory.HEARTS to listOf(
            "❤️", "🧡", "💛", "💚", "💙", "💜", "🩷", "🩵",
            "🤍", "🤎", "🖤", "💖", "💗", "💓", "💞", "💕",
            "❣️", "❤️‍🔥", "🫶", "🎉", "🎊", "🎈", "🎁", "✨",
            "🔥", "💫", "⚡", "👑", "🎀", "💐", "🌷", "🌹"
        ),
        ChatEmojiCategory.OBJECTS to listOf(
            "☀️", "🌤️", "⛅", "🌈", "🌸", "🌺", "🌻", "🌼",
            "🍀", "🌿", "🌱", "🌳", "🍎", "🍐", "🍊", "🍓",
            "☕", "🫖", "🥛", "🍪", "🧁", "🎂", "🚌", "🚗",
            "🏠", "☎️", "📱", "💬", "📢", "🔒", "🔑", "🕊️"
        )
    )

    fun loadRecentEmojis(context: Context): List<String> {
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val raw = prefs.getString(KEY_RECENT_EMOJIS, null)
            if (raw.isNullOrBlank()) {
                defaultRecentEmojis
            } else {
                raw.split("|").filter { it.isNotBlank() }.ifEmpty { defaultRecentEmojis }
            }
        } catch (_: Exception) {
            defaultRecentEmojis
        }
    }

    fun recordRecentEmoji(context: Context, emoji: String): List<String> {
        val current = loadRecentEmojis(context)
        val updated = (listOf(emoji) + current.filterNot { it == emoji }).take(MAX_RECENT)
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_RECENT_EMOJIS, updated.joinToString("|")).apply()
        } catch (_: Exception) {
            // Tercih kaydedilemese de bellek içi liste döner
        }
        return updated
    }

    /**
     * Mevcut imleç konumuna (veya seçili metnin yerine) emoji ekler ve yeni imleç konumunu döner.
     */
    fun insertEmojiAtSelection(
        text: String,
        selectionStart: Int,
        selectionEnd: Int,
        emoji: String
    ): Pair<String, Int> {
        val safeMin = minOf(selectionStart, selectionEnd).coerceIn(0, text.length)
        val safeMax = maxOf(selectionStart, selectionEnd).coerceIn(0, text.length)
        val updatedText = buildString(text.length + emoji.length) {
            append(text, 0, safeMin)
            append(emoji)
            append(text, safeMax, text.length)
        }
        val newCursor = safeMin + emoji.length
        return updatedText to newCursor
    }

    /**
     * İmleçten önceki tek karakteri veya çok baytlı emoji kümesini tek seferde siler.
     */
    fun deleteGraphemeBeforeCursor(
        text: String,
        selectionStart: Int,
        selectionEnd: Int
    ): Pair<String, Int> {
        val safeMin = minOf(selectionStart, selectionEnd).coerceIn(0, text.length)
        val safeMax = maxOf(selectionStart, selectionEnd).coerceIn(0, text.length)
        if (safeMin != safeMax) {
            val updated = text.removeRange(safeMin, safeMax)
            return updated to safeMin
        }
        if (safeMin == 0 || text.isEmpty()) return text to 0

        val iterator = BreakIterator.getCharacterInstance()
        iterator.setText(text)
        val prevBoundary = iterator.preceding(safeMin)
        val deleteFrom = if (prevBoundary == BreakIterator.DONE || prevBoundary < 0) {
            (safeMin - 1).coerceAtLeast(0)
        } else {
            prevBoundary
        }
        val updated = text.removeRange(deleteFrom, safeMin)
        return updated to deleteFrom
    }
}
