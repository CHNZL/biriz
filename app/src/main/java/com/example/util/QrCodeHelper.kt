package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object QrCodeHelper {

    /**
     * BİRİZ uygulamasının her zaman en son güncel APK indirme bağlantısı.
     * GitHub Releases üzerindeki /releases/latest/download/Biriz.apk adresi,
     * yeni sürümler çıktığında bile daima en güncel APK'ya otomatik yönlendirir.
     */
    const val LATEST_APK_DOWNLOAD_URL = "https://github.com/CHNZL/biriz/releases/latest/download/Biriz.apk"

    /**
     * Verilen URL/metin için yüksek çözünürlüklü ve taranması kolay Bitmap QR Kod üretir.
     */
    fun generateQrBitmap(
        content: String = LATEST_APK_DOWNLOAD_URL,
        size: Int = 256,
        foregroundColor: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE
    ): Bitmap? {
        return try {
            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 1,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )
            val bitMatrix = QRCodeWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                size,
                size,
                hints
            )
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) foregroundColor else backgroundColor
                }
            }
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
