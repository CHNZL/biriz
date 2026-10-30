package com.example.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

object GoogleAuthHelper {

    private const val FALLBACK_WEB_CLIENT_ID = "1053897004873-s0gplmvfrfmsdqajb076el3chkmf3ltk.apps.googleusercontent.com"

    fun getServerClientId(context: Context): String {
        return try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) {
                val value = context.getString(resId)
                if (value.isNotBlank()) value else FALLBACK_WEB_CLIENT_ID
            } else {
                FALLBACK_WEB_CLIENT_ID
            }
        } catch (_: Exception) {
            FALLBACK_WEB_CLIENT_ID
        }
    }

    fun attemptAutoSignIn(
        context: Context,
        credentialManager: CredentialManager,
        onAuthSuccess: () -> Unit,
        onUnauthenticated: () -> Unit,
        scope: CoroutineScope
    ) {
        if (Firebase.auth.currentUser != null) {
            onAuthSuccess()
            return
        }

        val clientId = getServerClientId(context)
        if (clientId.isBlank()) {
            onUnauthenticated()
            return
        }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        scope.launch {
            try {
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                    val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                    Firebase.auth.signInWithCredential(authCredential).await()
                    onAuthSuccess()
                } else {
                    onUnauthenticated()
                }
            } catch (e: Exception) {
                onUnauthenticated()
            }
        }
    }

    suspend fun clearCredentialState(context: Context) {
        try {
            val cm = CredentialManager.create(context)
            cm.clearCredentialState(ClearCredentialStateRequest())
            Log.d("Auth", "Credential state successfully cleared")
        } catch (e: Exception) {
            Log.w("Auth", "Failed to clear credential state", e)
        }
    }

    fun onGoogleSignInClicked(
        context: Context,
        credentialManager: CredentialManager,
        onAuthSuccess: () -> Unit,
        onAuthError: (String) -> Unit,
        scope: CoroutineScope,
        onAuthCancelled: () -> Unit = {}
    ) {
        val clientId = getServerClientId(context)
        if (clientId.isBlank()) {
            onAuthError("Google Sign-In yapılandırması eksik.")
            return
        }

        scope.launch {
            // Kullanıcının her zaman hesap seçebilmesi ve otomatik oturum açmanın
            // farklı hesap seçimini engellememesi için önce credential state'i sıfırlıyoruz.
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("Auth", "clearCredentialState error before sign in", e)
            }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(false)
                .build()

            val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .addCredentialOption(googleIdOption)
                .build()

            try {
                val activity = context as? Activity
                    ?: throw IllegalStateException("Context must be an Activity for CredentialManager")
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                val googleIdToken = try {
                    GoogleIdTokenCredential.createFrom(credential.data).idToken
                } catch (parseEx: Exception) {
                    if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                        GoogleIdTokenCredential.createFrom(credential.data).idToken
                    } else {
                        throw parseEx
                    }
                }
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } catch (e: GetCredentialCancellationException) {
                Log.w("Auth", "Google Sign-In flow cancelled: ${e.message}")
                onAuthCancelled()
            } catch (e: androidx.credentials.exceptions.NoCredentialException) {
                Log.e("Auth", "No credential available", e)
                onAuthError("Google hesabı doğrulanamadı. Uygulamanın imza sertifikası kontrol ediliyor...")
            } catch (e: Exception) {
                Log.e("Auth", "Google Sign-In failed", e)
                onAuthError(e.localizedMessage ?: "Giriş işlemi başarısız oldu")
            }
        }
    }

    fun signOut(
        credentialManager: CredentialManager,
        onSignOutComplete: () -> Unit = {},
        scope: CoroutineScope
    ) {
        Firebase.auth.signOut()
        scope.launch {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.e("Auth", "Failed to clear credential state", e)
            } finally {
                onSignOutComplete()
            }
        }
    }
}
