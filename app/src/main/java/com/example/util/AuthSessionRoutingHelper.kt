package com.example.util

import com.example.data.model.UserRole
import com.example.ui.viewmodel.ScreenState
import java.util.Locale

/**
 * Uygulama açılışı ve oturum çözümlemesi sırasında kullanıcının gerçek rolüne göre
 * hangi ekrana yönlendirileceğini deterministik ve hataya dayanıklı biçimde belirler.
 *
 * Temel garanti:
 * - Kayıtlı bir öğretmen veya veli, ağ gecikmesi veya önbellek eksikliği yüzünden asla
 *   Rol ve KVKK seçim ekranına (ROLE_AND_KVKK) düşürülmez.
 * - Rol doğrulaması sürerken rol seçim ekranı gösterilmez.
 */
object AuthSessionRoutingHelper {

    data class InitialLaunchDecision(
        val screen: ScreenState,
        val role: UserRole,
        val isAuthResolving: Boolean,
        val matchedSavedSession: Boolean
    )

    enum class AuthenticatedRouteTarget {
        TEACHER_DASHBOARD,
        TEACHER_SETUP,
        PARENT_PORTAL,
        ROLE_AND_KVKK,
        RETRY_NETWORK_ERROR
    }

    fun normalizeRole(role: String?): String {
        return role?.trim()?.uppercase(Locale.ROOT).orEmpty()
    }

    fun isTeacherRole(role: String?): Boolean {
        return when (normalizeRole(role)) {
            "TEACHER", "OGRETMEN", "ÖĞRETMEN" -> true
            else -> false
        }
    }

    fun isParentRole(role: String?): Boolean {
        return when (normalizeRole(role)) {
            "PARENT", "VELI", "VELİ" -> true
            else -> false
        }
    }

    fun isExplicitMemberRole(role: String?): Boolean {
        return when (normalizeRole(role)) {
            "UYE", "ÜYE", "MEMBER" -> true
            else -> false
        }
    }

    fun doesSavedSessionMatchUser(
        savedUid: String?,
        currentUid: String?,
        savedEmail: String?,
        currentEmail: String?
    ): Boolean {
        val cleanSavedUid = savedUid?.trim().orEmpty()
        val cleanCurrentUid = currentUid?.trim().orEmpty()
        if (cleanSavedUid.isNotBlank() && cleanCurrentUid.isNotBlank()) {
            return cleanSavedUid == cleanCurrentUid
        }
        val cleanSavedEmail = savedEmail?.trim()?.lowercase(Locale.ROOT).orEmpty()
        val cleanCurrentEmail = currentEmail?.trim()?.lowercase(Locale.ROOT).orEmpty()
        if (cleanSavedEmail.isNotBlank() && cleanCurrentEmail.isNotBlank()) {
            return cleanSavedEmail == cleanCurrentEmail
        }
        // Eski sürümden gelen ve henüz UID/e-posta yazılmamış tek kullanıcılı yerel oturumlar
        return cleanSavedUid.isBlank() && cleanSavedEmail.isBlank()
    }

    /**
     * Uygulama ilk açıldığında yerel oturum önbelleği ve Firebase oturum durumuna göre
     * açılış ekranını belirler. Hiçbir zaman doğrulanmamış kullanıcıyı ROLE_AND_KVKK ekranına atmaz.
     */
    fun resolveInitialLaunchDecision(
        hasFirebaseUser: Boolean,
        currentUid: String?,
        currentEmail: String?,
        savedUid: String?,
        savedEmail: String?,
        savedRoleCode: String?,
        hasSavedClassroom: Boolean
    ): InitialLaunchDecision {
        if (!hasFirebaseUser) {
            return InitialLaunchDecision(
                screen = ScreenState.GOOGLE_SIGN_IN,
                role = UserRole.MEMBER,
                isAuthResolving = false,
                matchedSavedSession = false
            )
        }

        val sessionMatches = doesSavedSessionMatchUser(
            savedUid = savedUid,
            currentUid = currentUid,
            savedEmail = savedEmail,
            currentEmail = currentEmail
        )
        if (sessionMatches) {
            val savedRole = UserRole.fromCode(savedRoleCode ?: "")
            if (savedRole == UserRole.TEACHER && hasSavedClassroom) {
                return InitialLaunchDecision(
                    screen = ScreenState.TEACHER_DASHBOARD,
                    role = UserRole.TEACHER,
                    isAuthResolving = false,
                    matchedSavedSession = true
                )
            }
            if (savedRole == UserRole.PARENT && hasSavedClassroom) {
                return InitialLaunchDecision(
                    screen = ScreenState.PARENT_PORTAL,
                    role = UserRole.PARENT,
                    isAuthResolving = false,
                    matchedSavedSession = true
                )
            }
        }

        // Yerel oturum yoksa veya farklı kullanıcıysa, bulut doğrulaması bitene kadar giriş/yükleme ekranında kal
        return InitialLaunchDecision(
            screen = ScreenState.GOOGLE_SIGN_IN,
            role = UserRole.MEMBER,
            isAuthResolving = true,
            matchedSavedSession = false
        )
    }

    /**
     * Bulut (ve gerekirse doğrulanmış yerel yedek) sinyallerine göre giriş yapan kullanıcının
     * kesin hedef ekranını belirler.
     */
    fun resolveAuthenticatedRoute(
        isFreshRegistration: Boolean,
        profileLoadedFromCloud: Boolean,
        profileRole: String?,
        hasResolvedTeacherClass: Boolean,
        hasCloudLinkedParentStudents: Boolean,
        hasMatchingLocalTeacherClass: Boolean,
        hasMatchingLocalParentSession: Boolean
    ): AuthenticatedRouteTarget {
        if (isFreshRegistration) {
            return AuthenticatedRouteTarget.ROLE_AND_KVKK
        }

        // 1. Buluttan doğrulanmış öğretmen sınıfı varsa doğrudan öğretmen paneline git
        if (hasResolvedTeacherClass) {
            return AuthenticatedRouteTarget.TEACHER_DASHBOARD
        }

        val isProfileTeacher = isTeacherRole(profileRole)
        val isProfileParent = isParentRole(profileRole)
        val isExplicitMember = isExplicitMemberRole(profileRole)

        // 2. Buluttan bağlı öğrenci veya veli rolü bulunduysa doğrudan veli portalına git
        if (hasCloudLinkedParentStudents || isProfileParent || (!isExplicitMember && hasMatchingLocalParentSession)) {
            return AuthenticatedRouteTarget.PARENT_PORTAL
        }

        // 3. Öğretmen olarak kayıtlı ama henüz sınıf oluşturmamış kullanıcı
        if (isProfileTeacher) {
            return if (hasMatchingLocalTeacherClass) {
                AuthenticatedRouteTarget.TEACHER_DASHBOARD
            } else {
                AuthenticatedRouteTarget.TEACHER_SETUP
            }
        }

        // 4. Bulut profili ağ hatası / zaman aşımı nedeniyle hiç okunamadıysa kullanıcıyı rol seçimine DÜŞÜRME
        if (!profileLoadedFromCloud) {
            return when {
                hasMatchingLocalTeacherClass -> AuthenticatedRouteTarget.TEACHER_DASHBOARD
                hasMatchingLocalParentSession -> AuthenticatedRouteTarget.PARENT_PORTAL
                else -> AuthenticatedRouteTarget.RETRY_NETWORK_ERROR
            }
        }

        // 5. Bulut profili başarıyla okundu ve gerçekten henüz rol seçilmemiş yeni/rolsüz üye
        return AuthenticatedRouteTarget.ROLE_AND_KVKK
    }
}
