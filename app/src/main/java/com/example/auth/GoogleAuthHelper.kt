package com.example.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

object GoogleAuthHelper {

    @Volatile
    private var activeClearJob: Job? = null

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

    fun getServerClientId(context: Context): String {
        return try {
            context.getString(R.string.default_web_client_id).trim()
        } catch (_: Exception) {
            ""
        }
    }

    /**
     * Etkileşimli Google ile Giriş isteği (yalnızca tek seçenek içerir).
     * GetSignInWithGoogleOption diğer seçeneklerle birleştirilemez.
     */
    internal fun buildInteractiveSignInRequest(serverClientId: String): GetCredentialRequest {
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = serverClientId).build()
        return GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()
    }

    /**
     * Sessiz veya yedek hesap seçimi için tek seçenekli Google ID isteği.
     */
    internal fun buildGoogleIdRequest(
        serverClientId: String,
        filterByAuthorizedAccounts: Boolean,
        autoSelectEnabled: Boolean
    ): GetCredentialRequest {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(autoSelectEnabled)
            .build()
        return GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
    }

    private fun extractGoogleIdToken(credential: Credential): String {
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            return GoogleIdTokenCredential.createFrom(credential.data).idToken
        }
        throw IllegalStateException("Beklenmeyen kimlik türü: ${credential.type}")
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

        val request = buildGoogleIdRequest(
            serverClientId = clientId,
            filterByAuthorizedAccounts = true,
            autoSelectEnabled = true
        )

        scope.launch {
            try {
                awaitPendingClearState()
                val targetContext = context.findActivity() ?: context
                val result = credentialManager.getCredential(targetContext, request)
                val googleIdToken = extractGoogleIdToken(result.credential)
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                onUnauthenticated()
            }
        }
    }

    suspend fun clearCredentialState(context: Context) {
        try {
            val cm = CredentialManager.create(context.applicationContext ?: context)
            cm.clearCredentialState(ClearCredentialStateRequest())
            Log.d("Auth", "Credential state successfully cleared")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("Auth", "Failed to clear credential state", e)
        }
    }

    fun launchClearCredentialState(scope: CoroutineScope, context: Context) {
        val appCtx = context.applicationContext ?: context
        activeClearJob?.cancel()
        activeClearJob = scope.launch {
            withContext(NonCancellable) {
                clearCredentialState(appCtx)
            }
        }
    }

    private suspend fun awaitPendingClearState() {
        val job = activeClearJob ?: return
        if (job.isActive) {
            withTimeoutOrNull(1200L) {
                job.join()
            }
        }
        activeClearJob = null
    }

    private suspend fun fetchInteractiveGoogleIdToken(
        activity: Activity,
        credentialManager: CredentialManager,
        clientId: String
    ): String {
        val primaryRequest = buildInteractiveSignInRequest(clientId)
        val result = try {
            credentialManager.getCredential(activity, primaryRequest)
        } catch (e: GetCredentialCancellationException) {
            throw e
        } catch (e: NoCredentialException) {
            Log.w("Auth", "Primary GetSignInWithGoogleOption returned NoCredentialException, trying GetGoogleIdOption fallback", e)
            val fallbackRequest = buildGoogleIdRequest(
                serverClientId = clientId,
                filterByAuthorizedAccounts = false,
                autoSelectEnabled = false
            )
            credentialManager.getCredential(activity, fallbackRequest)
        } catch (e: GetCredentialProviderConfigurationException) {
            Log.w("Auth", "Primary GetSignInWithGoogleOption config issue, trying GetGoogleIdOption fallback", e)
            val fallbackRequest = buildGoogleIdRequest(
                serverClientId = clientId,
                filterByAuthorizedAccounts = false,
                autoSelectEnabled = false
            )
            credentialManager.getCredential(activity, fallbackRequest)
        }
        return extractGoogleIdToken(result.credential)
    }

    /**
     * Kalıcı hesap silme öncesi Firebase Auth son oturum süresi dolmuşsa
     * kullanıcının kimliğini Google Credential Manager ile yeniden doğrular.
     * İptal edilirse false döner, başarılıysa true döner.
     */
    suspend fun reauthenticateCurrentUser(context: Context): Boolean {
        val currentUser = Firebase.auth.currentUser ?: return false
        val clientId = getServerClientId(context)
        if (clientId.isBlank()) {
            throw IllegalStateException("Google kimlik doğrulama yapılandırması bulunamadı.")
        }
        val activity = context.findActivity()
            ?: throw IllegalStateException("Güvenlik doğrulaması için aktif pencere bulunamadı.")

        val credentialManager = CredentialManager.create(activity)
        return try {
            awaitPendingClearState()
            val googleIdToken = fetchInteractiveGoogleIdToken(activity, credentialManager, clientId)
            val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
            currentUser.reauthenticate(authCredential).await()
            true
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Reauthentication cancelled by user", e)
            false
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Log.w("Auth", "Reauthentication account mismatch", e)
            throw IllegalStateException("Güvenlik doğrulaması için lütfen oturumu açık olan Google hesabınızı seçin.")
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
            onAuthError("Google Sign-In yapılandırması eksik (default_web_client_id bulunamadı).")
            return
        }

        val activity = context.findActivity()
        if (activity == null) {
            onAuthError("Giriş penceresi başlatılamadı (Activity bağlamı bulunamadı).")
            return
        }

        scope.launch {
            try {
                // Önceki çıkış işleminden kalan clearCredentialState çağrısının yeni seçiciyle çakışmasını önle
                awaitPendingClearState()

                // Önemli: GetSignInWithGoogleOption tek başına gönderilmelidir;
                // GetGoogleIdOption ile aynı istekte birleştirilmesi Credential Manager hatasına yol açar.
                val googleIdToken = fetchInteractiveGoogleIdToken(activity, credentialManager, clientId)
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                try {
                    withTimeoutOrNull(2500L) {
                        Firebase.auth.currentUser?.getIdToken(false)?.await()
                    }
                } catch (_: Exception) {}
                onAuthSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: GetCredentialCancellationException) {
                Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
                onAuthCancelled()
            } catch (e: NoCredentialException) {
                Log.e("Auth", "No credential available", e)
                onAuthError("Cihazda kullanılabilir Google hesabı bulunamadı veya Google Play Hizmetleri doğrulama penceresi açılamadı. Lütfen cihazınızda Google hesabınızın açık olduğundan emin olup tekrar deneyin.")
            } catch (e: GetCredentialUnsupportedException) {
                Log.e("Auth", "Credential Manager unsupported on device", e)
                onAuthError("Bu cihazda Google Play Hizmetleri kimlik yöneticisi desteklenmiyor veya güncel değil.")
            } catch (e: GetCredentialException) {
                Log.e("Auth", "Google Sign-In CredentialException", e)
                onAuthError("Google ile giriş başarısız oldu: ${e.localizedMessage ?: "Bilinmeyen kimlik doğrulama hatası"}")
            } catch (e: Exception) {
                Log.e("Auth", "Google Sign-In failed", e)
                onAuthError(e.localizedMessage ?: "Giriş işlemi başarısız oldu.")
            }
        }
    }

    fun signOut(
        credentialManager: CredentialManager,
        onSignOutComplete: () -> Unit = {},
        scope: CoroutineScope
    ) {
        Firebase.auth.signOut()
        activeClearJob?.cancel()
        activeClearJob = scope.launch {
            withContext(NonCancellable) {
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w("Auth", "Failed to clear credential state: ${e.message}")
                } finally {
                    onSignOutComplete()
                }
            }
        }
    }
}
