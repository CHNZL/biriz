package com.example.util

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore

/**
 * BİRİZ Uygulaması için Merkezi ve Güvenilir Firestore Sağlayıcısı.
 *
 * Uygulamanın tüm modüllerinin (Sınıf, Öğrenciler, Ders Programı, Oturma Planı,
 * Duyurular, Mesajlaşma, Turnuva ve Kitaplık) aynı Firestore veritabanına
 * bağlanmasını garanti eder.
 */
object FirestoreProvider {
    private const val TAG = "FirestoreProvider"

    @Volatile
    private var cachedInstance: FirebaseFirestore? = null

    fun get(context: Context): FirebaseFirestore {
        cachedInstance?.let { return it }

        synchronized(this) {
            cachedInstance?.let { return it }

            val appContext = context.applicationContext ?: context
            val instance = try {
                val dbId = appContext.getString(R.string.firestore_database_id)
                if (dbId.isNotBlank() && dbId != "default" && dbId != "(default)") {
                    FirebaseFirestore.getInstance(FirebaseApp.getInstance(), dbId)
                } else {
                    FirebaseFirestore.getInstance()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Custom database unavailable, fallback to default", e)
                try {
                    FirebaseFirestore.getInstance()
                } catch (ex: Exception) {
                    Log.e(TAG, "Failed to get default Firestore instance", ex)
                    throw ex
                }
            }

            cachedInstance = instance
            return instance
        }
    }
}
