package com.example.util

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log

/**
 * Şanslı Öğrenci, Oyunlaştırma ve Kura Çekimleri için Ses Efektleri Yardımcısı.
 * Native Android ToneGenerator kullanarak herhangi bir dış MP3 dosyasına ihtiyaç duymadan
 * net ve coşkulu sesler üretir.
 */
object SoundHelper {
    private const val TAG = "SoundHelper"
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
        } catch (e: Exception) {
            Log.w(TAG, "ToneGenerator initialized fallback: ${e.message}")
        }
    }

    /**
     * Dönen çark/kura tıkırtı sesi (Short tick)
     */
    fun playTickSound() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 25)
        } catch (e: Exception) {
            Log.w(TAG, "playTickSound error: ${e.message}")
        }
    }

    /**
     * Kazanan açıklandığında coşkulu zafer sesi (Fanfare / Victory chime)
     */
    fun playFanfareSound() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_DTMF_A, 180)
        } catch (e: Exception) {
            Log.w(TAG, "playFanfareSound error: ${e.message}")
        }
    }
}
