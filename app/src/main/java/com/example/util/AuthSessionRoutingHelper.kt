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

    /**
     * Kullanıcının doğrulanmış oturum yer imi (yalnızca kendi UID/e-postası için).
     * Çıkış yapıp tekrar giriş yapan aktif kullanıcıların geçici ağ/sunucu kesintisinde
     * kilitlenmeden doğrudan kendi paneline erişebilmesini sağlar.
     * Hesap silme veya rol silme işlemlerinde ilgili kayıt kalıcı olarak temizlenir.
     */
    data class AccountSessionBookmark(
        val uid: String,
        val email: String,
        val roleCode: String,
        val classId: String,
        val cityName: String = "",
        val districtName: String = "",
        val schoolName: String = "",
        val gradeLevel: Int = 4,
        val section: String = "A",
        val teacherName: String = "",
        val teacherEmail: String = "",
        val inviteCode: String = "",
        val linkedStudentIds: List<String> = emptyList(),
        val updatedAtMs: Long = System.currentTimeMillis()
    )

    fun upsertBookmarkJson(existingJson: String?, bookmark: AccountSessionBookmark): String {
        val cleanUid = bookmark.uid.trim()
        val cleanEmail = bookmark.email.trim().lowercase(Locale.ROOT)
        if (cleanUid.isBlank() && cleanEmail.isBlank()) return existingJson.orEmpty()
        val map = parseBookmarksMap(existingJson).toMutableMap()
        val key = cleanUid.ifBlank { "email:$cleanEmail" }
        // Aynı e-postaya ait eski anahtar varsa güncelle
        if (cleanEmail.isNotBlank()) {
            val staleKeys = map.entries
                .filter { it.key != key && it.value.email.equals(cleanEmail, ignoreCase = true) }
                .map { it.key }
            staleKeys.forEach { map.remove(it) }
        }
        map[key] = bookmark.copy(uid = cleanUid, email = cleanEmail)
        return serializeBookmarksMap(map)
    }

    fun findBookmarkFromJson(
        existingJson: String?,
        uid: String?,
        email: String?
    ): AccountSessionBookmark? {
        val cleanUid = uid?.trim().orEmpty()
        val cleanEmail = email?.trim()?.lowercase(Locale.ROOT).orEmpty()
        if (cleanUid.isBlank() && cleanEmail.isBlank()) return null
        val map = parseBookmarksMap(existingJson)
        if (cleanUid.isNotBlank()) {
            map[cleanUid]?.let { return it }
            map.values.firstOrNull { it.uid == cleanUid }?.let { return it }
        }
        if (cleanEmail.isNotBlank()) {
            map.values.firstOrNull {
                it.email.equals(cleanEmail, ignoreCase = true) &&
                    (it.uid.isBlank() || cleanUid.isBlank() || it.uid == cleanUid)
            }?.let { return it }
        }
        return null
    }

    fun removeBookmarkFromJson(
        existingJson: String?,
        uid: String?,
        email: String?
    ): String {
        val cleanUid = uid?.trim().orEmpty()
        val cleanEmail = email?.trim()?.lowercase(Locale.ROOT).orEmpty()
        if (existingJson.isNullOrBlank()) return ""
        val map = parseBookmarksMap(existingJson).toMutableMap()
        val keysToRemove = map.entries.filter { (k, v) ->
            (cleanUid.isNotBlank() && (k == cleanUid || v.uid == cleanUid)) ||
                (cleanEmail.isNotBlank() && v.email.equals(cleanEmail, ignoreCase = true))
        }.map { it.key }
        keysToRemove.forEach { map.remove(it) }
        return serializeBookmarksMap(map)
    }

    private fun parseBookmarksMap(rawJson: String?): Map<String, AccountSessionBookmark> {
        if (rawJson.isNullOrBlank()) return emptyMap()
        return try {
            val arr = org.json.JSONArray(rawJson)
            val result = LinkedHashMap<String, AccountSessionBookmark>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val uid = obj.optString("uid").trim()
                val email = obj.optString("email").trim().lowercase(Locale.ROOT)
                val roleCode = obj.optString("roleCode").trim()
                val classId = obj.optString("classId").trim()
                if ((uid.isBlank() && email.isBlank()) || roleCode.isBlank()) continue
                val idsArr = obj.optJSONArray("linkedStudentIds")
                val linkedIds = mutableListOf<String>()
                if (idsArr != null) {
                    for (j in 0 until idsArr.length()) {
                        val id = idsArr.optString(j).trim()
                        if (id.isNotBlank()) linkedIds.add(id)
                    }
                }
                val key = uid.ifBlank { "email:$email" }
                result[key] = AccountSessionBookmark(
                    uid = uid,
                    email = email,
                    roleCode = roleCode,
                    classId = classId,
                    cityName = obj.optString("cityName"),
                    districtName = obj.optString("districtName"),
                    schoolName = obj.optString("schoolName"),
                    gradeLevel = obj.optInt("gradeLevel", 4),
                    section = obj.optString("section", "A").ifBlank { "A" },
                    teacherName = obj.optString("teacherName"),
                    teacherEmail = obj.optString("teacherEmail"),
                    inviteCode = obj.optString("inviteCode"),
                    linkedStudentIds = linkedIds,
                    updatedAtMs = obj.optLong("updatedAtMs", 0L)
                )
            }
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun serializeBookmarksMap(map: Map<String, AccountSessionBookmark>): String {
        val arr = org.json.JSONArray()
        map.values.sortedByDescending { it.updatedAtMs }.take(8).forEach { b ->
            val idsArr = org.json.JSONArray()
            b.linkedStudentIds.forEach { idsArr.put(it) }
            arr.put(
                org.json.JSONObject()
                    .put("uid", b.uid)
                    .put("email", b.email)
                    .put("roleCode", b.roleCode)
                    .put("classId", b.classId)
                    .put("cityName", b.cityName)
                    .put("districtName", b.districtName)
                    .put("schoolName", b.schoolName)
                    .put("gradeLevel", b.gradeLevel)
                    .put("section", b.section)
                    .put("teacherName", b.teacherName)
                    .put("teacherEmail", b.teacherEmail)
                    .put("inviteCode", b.inviteCode)
                    .put("linkedStudentIds", idsArr)
                    .put("updatedAtMs", b.updatedAtMs)
            )
        }
        return arr.toString()
    }
}

