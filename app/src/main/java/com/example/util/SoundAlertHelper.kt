package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * Zamanlayıcı ses ve titreşim uyarı yöneticisi
 * - Sistem bildirim sesi (RingtoneManager / ToneGenerator)
 * - Dokunsal titreşim (VibrationEffect)
 */
object SoundAlertHelper {
    private const val TAG = "SoundAlertHelper"

    fun playTimerCompleteAlert(context: Context, playSound: Boolean = true, vibrate: Boolean = true) {
        if (playSound) {
            try {
                val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                val ringtone = RingtoneManager.getRingtone(context.applicationContext, notificationUri)
                if (ringtone != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        ringtone.audioAttributes = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    }
                    ringtone.play()
                } else {
                    // Ringtone bulunamazsa ToneGenerator ile bip sesi
                    val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                    toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 600)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Ringtone çalınırken hata: ${e.message}")
                try {
                    val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
                    toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 500)
                } catch (_: Exception) {}
            }
        }

        if (vibrate) {
            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vibratorManager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }

                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val pattern = longArrayOf(0, 300, 150, 300, 150, 400)
                        val amplitudes = intArrayOf(0, 200, 0, 255, 0, 255)
                        vibrator.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(longArrayOf(0, 300, 150, 300, 150, 400), -1)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Titreşim çalıştırılırken hata: ${e.message}")
            }
        }
    }

    fun playTickTone() {
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 60)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
        } catch (_: Exception) {}
    }
}
