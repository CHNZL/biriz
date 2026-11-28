package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.R
import com.example.data.model.ClassRoom
import com.example.data.model.LinkedParentAccount
import com.example.data.model.Student
import com.example.data.model.UserProfile
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Oturum açma ve rol çözümleme akışında Firestore gRPC SDK'sının ağ geçişi veya
 * soğuk başlangıçta takılmasını önlemek için kullanılan yüksek hızlı HTTPS REST v1 istemcisi.
 *
 * - API anahtarı ve proje/veritabanı kimliği çalışma zamanında FirebaseApp.options ve
 *   R.string.firestore_database_id üzerinden okunur.
 * - HTTP 200 (Found), HTTP 404 (NotFound) ve ağ/kota hataları (Error) net biçimde ayrıştırılır.
 */
object FirestoreAuthRestHelper {

    private const val TAG = "FirestoreAuthRest"

    sealed class RestFetchResult<out T> {
        data class Found<T>(val data: T) : RestFetchResult<T>()
        object NotFound : RestFetchResult<Nothing>()
        data class Error(val httpCode: Int = 0, val detail: String = "") : RestFetchResult<Nothing>()
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(4, TimeUnit.SECONDS)
            .callTimeout(7, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private data class RestConfig(
        val projectId: String,
        val apiKey: String,
        val dbId: String
    )

    private fun resolveConfig(context: Context): RestConfig? {
        return try {
            val opts = FirebaseApp.getInstance().options
            val projectId = opts.projectId?.trim().orEmpty()
            val apiKey = opts.apiKey?.trim().orEmpty()
            if (projectId.isBlank() || apiKey.isBlank()) return null
            val dbId = try {
                context.applicationContext.getString(R.string.firestore_database_id).trim()
                    .ifBlank { "(default)" }
            } catch (_: Exception) {
                "(default)"
            }
            RestConfig(projectId = projectId, apiKey = apiKey, dbId = dbId)
        } catch (_: Exception) {
            null
        }
    }

    private fun buildDocumentUrl(cfg: RestConfig, pathSegments: List<String>): String {
        val encodedPath = pathSegments.joinToString("/") { Uri.encode(it) }
        return "https://firestore.googleapis.com/v1/projects/${Uri.encode(cfg.projectId)}" +
            "/databases/${Uri.encode(cfg.dbId)}/documents/$encodedPath?key=${Uri.encode(cfg.apiKey)}"
    }

    private fun buildRunQueryUrl(cfg: RestConfig, parentPathSegments: List<String> = emptyList()): String {
        val parentSuffix = if (parentPathSegments.isEmpty()) {
            ""
        } else {
            "/" + parentPathSegments.joinToString("/") { Uri.encode(it) }
        }
        return "https://firestore.googleapis.com/v1/projects/${Uri.encode(cfg.projectId)}" +
            "/databases/${Uri.encode(cfg.dbId)}/documents$parentSuffix:runQuery?key=${Uri.encode(cfg.apiKey)}"
    }

    private fun executeGet(url: String, bearerToken: String?): Pair<Int, String> {
        fun doRequest(token: String?): Pair<Int, String> {
            val builder = Request.Builder()
                .url(url)
                .get()
                .header("Accept", "application/json")
                .header("Cache-Control", "no-store")
            if (!token.isNullOrBlank()) {
                builder.header("Authorization", "Bearer $token")
            }
            httpClient.newCall(builder.build()).execute().use { resp ->
                return Pair(resp.code, resp.body?.string().orEmpty())
            }
        }

        val first = doRequest(bearerToken)
        // Süresi dolmuş ID token yüzünden 401 alınırsa yalnızca API key ile bir kez tekrar dene
        if (first.first == 401 && !bearerToken.isNullOrBlank()) {
            return doRequest(null)
        }
        return first
    }

    private fun executePostJson(url: String, jsonBody: String, bearerToken: String?): Pair<Int, String> {
        val mediaType = "application/json; charset=utf-8".toMediaType()
        fun doRequest(token: String?): Pair<Int, String> {
            val builder = Request.Builder()
                .url(url)
                .post(jsonBody.toRequestBody(mediaType))
                .header("Accept", "application/json")
            if (!token.isNullOrBlank()) {
                builder.header("Authorization", "Bearer $token")
            }
            httpClient.newCall(builder.build()).execute().use { resp ->
                return Pair(resp.code, resp.body?.string().orEmpty())
            }
        }

        val first = doRequest(bearerToken)
        if (first.first == 401 && !bearerToken.isNullOrBlank()) {
            return doRequest(null)
        }
        return first
    }

    // ---- Saf JSON Ayrıştırıcılar (Birim Test Edilebilir) ----

    internal fun extractDocumentId(documentName: String?): String {
        val clean = documentName?.trim().orEmpty()
        if (clean.isBlank()) return ""
        return clean.substringAfterLast('/')
    }

    internal fun extractParentClassroomIdFromStudentPath(documentName: String?): String {
        // .../documents/classrooms/{classId}/students/{studentId}
        val clean = documentName?.trim().orEmpty()
        val marker = "/classrooms/"
        val idx = clean.indexOf(marker)
        if (idx < 0) return ""
        val after = clean.substring(idx + marker.length)
        return after.substringBefore("/students/", missingDelimiterValue = "").trim()
    }

    internal fun readStringField(fields: JSONObject?, key: String): String? {
        val obj = fields?.optJSONObject(key) ?: return null
        if (obj.has("nullValue")) return null
        return if (obj.has("stringValue")) obj.optString("stringValue") else null
    }

    internal fun readBooleanField(fields: JSONObject?, key: String): Boolean? {
        val obj = fields?.optJSONObject(key) ?: return null
        if (obj.has("nullValue")) return null
        return if (obj.has("booleanValue")) obj.optBoolean("booleanValue") else null
    }

    internal fun readLongField(fields: JSONObject?, key: String): Long? {
        val obj = fields?.optJSONObject(key) ?: return null
        if (obj.has("nullValue")) return null
        if (obj.has("integerValue")) {
            return obj.optString("integerValue").toLongOrNull()
        }
        if (obj.has("doubleValue")) {
            return obj.optDouble("doubleValue").toLong()
        }
        return null
    }

    internal fun readStringListField(fields: JSONObject?, key: String): List<String> {
        val arr = fields?.optJSONObject(key)
            ?.optJSONObject("arrayValue")
            ?.optJSONArray("values")
            ?: return emptyList()
        val result = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            val value = item.optString("stringValue").trim()
            if (value.isNotBlank()) {
                result.add(value)
            }
        }
        return result
    }

    internal fun parseUserProfileFromRestJson(
        uid: String,
        fallbackName: String,
        fallbackEmail: String,
        rawJson: String
    ): UserProfile? {
        return try {
            val root = JSONObject(rawJson)
            val fields = root.optJSONObject("fields") ?: return null
            val docId = extractDocumentId(root.optString("name")).ifBlank { uid }
            UserProfile(
                id = docId,
                userId = readStringField(fields, "userId")?.takeIf { it.isNotBlank() } ?: uid,
                fullName = readStringField(fields, "fullName")?.takeIf { it.isNotBlank() } ?: fallbackName,
                email = readStringField(fields, "email")?.takeIf { it.isNotBlank() } ?: fallbackEmail,
                role = readStringField(fields, "role")?.takeIf { it.isNotBlank() } ?: "UYE",
                kvkkAccepted = readBooleanField(fields, "kvkkAccepted") ?: false,
                assignedClassId = readStringField(fields, "assignedClassId")?.takeIf { it.isNotBlank() },
                assignedSchoolId = readStringField(fields, "assignedSchoolId")?.takeIf { it.isNotBlank() },
                linkedClassroomId = readStringField(fields, "linkedClassroomId")?.takeIf { it.isNotBlank() },
                linkedStudentIds = readStringListField(fields, "linkedStudentIds"),
                isFreshRegistration = false,
                loadedFromCloud = true
            )
        } catch (_: Exception) {
            null
        }
    }

    internal fun parseClassRoomFromRestDoc(
        docObj: JSONObject,
        fallbackUid: String = "",
        fallbackEmail: String = ""
    ): ClassRoom? {
        return try {
            val fields = docObj.optJSONObject("fields") ?: return null
            val docId = extractDocumentId(docObj.optString("name"))
            if (docId.isBlank()) return null
            val deferralConfig = ParentNotificationDeferralConfig(
                enabled = readBooleanField(fields, "parentNotifDeferralEnabled") ?: false,
                startHour = (readLongField(fields, "parentNotifDeferralStartHour") ?: 21L).toInt().coerceIn(0, 23),
                startMinute = (readLongField(fields, "parentNotifDeferralStartMinute") ?: 0L).toInt().coerceIn(0, 59),
                deliverHour = (readLongField(fields, "parentNotifDeferralDeliverHour") ?: 10L).toInt().coerceIn(0, 23),
                deliverMinute = (readLongField(fields, "parentNotifDeferralDeliverMinute") ?: 0L).toInt().coerceIn(0, 59)
            )
            ClassRoom(
                id = docId,
                cityName = readStringField(fields, "cityName") ?: "",
                districtName = readStringField(fields, "districtName") ?: "",
                schoolName = readStringField(fields, "schoolName") ?: "",
                gradeLevel = (readLongField(fields, "gradeLevel") ?: 1L).toInt(),
                section = readStringField(fields, "section") ?: "A",
                teacherId = readStringField(fields, "teacherId") ?: fallbackUid,
                teacherName = readStringField(fields, "teacherName") ?: "",
                teacherEmail = readStringField(fields, "teacherEmail") ?: fallbackEmail,
                inviteCode = readStringField(fields, "inviteCode") ?: "",
                isOccupied = true,
                teacherIsOnline = readBooleanField(fields, "teacherIsOnline") ?: false,
                teacherLastActiveMillis = readLongField(fields, "teacherLastActiveMillis"),
                parentNotificationDeferral = deferralConfig
            )
        } catch (_: Exception) {
            null
        }
    }

    internal fun parseStudentFromRestDoc(docObj: JSONObject): Student? {
        return try {
            val fields = docObj.optJSONObject("fields") ?: return null
            val docId = extractDocumentId(docObj.optString("name"))
            if (docId.isBlank()) return null
            val studentNo = (readLongField(fields, "studentNo") ?: 0L).toInt()
            val fullName = readStringField(fields, "fullName") ?: ""
            val gender = readStringField(fields, "gender") ?: "Erkek"
            val rawCode = readStringField(fields, "parentCode")
                ?: readStringField(fields, "parentInviteCode")
                ?: readStringField(fields, "veliKodu")
                ?: readStringField(fields, "code")
                ?: ""
            val linkedParents = mutableListOf<LinkedParentAccount>()
            val arr = fields.optJSONObject("linkedParents")
                ?.optJSONObject("arrayValue")
                ?.optJSONArray("values")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val mapFields = arr.optJSONObject(i)
                        ?.optJSONObject("mapValue")
                        ?.optJSONObject("fields")
                        ?: continue
                    val pEmail = readStringField(mapFields, "email")?.trim().orEmpty()
                    val pName = readStringField(mapFields, "name")?.trim().orEmpty()
                    if (pEmail.isNotBlank() && pName.isNotBlank()) {
                        linkedParents.add(
                            LinkedParentAccount(
                                email = pEmail,
                                name = pName,
                                phone = readStringField(mapFields, "phone") ?: "",
                                relationship = readStringField(mapFields, "relationship") ?: "Veli",
                                linkedAt = readStringField(mapFields, "linkedAt") ?: "Bağlandı",
                                lastLoginAt = readStringField(mapFields, "lastLoginAt"),
                                isOnline = readBooleanField(mapFields, "isOnline") ?: false,
                                lastActiveMillis = readLongField(mapFields, "lastActiveMillis")
                            )
                        )
                    }
                }
            }
            Student(
                id = docId,
                studentNumber = studentNo.toString(),
                fullName = fullName,
                gender = gender,
                parentInviteCode = rawCode,
                parentName = readStringField(fields, "parentName"),
                parentEmail = readStringField(fields, "parentEmail"),
                parentLinkedAt = readStringField(fields, "parentLinkedAt"),
                parentLastLoginAt = readStringField(fields, "parentLastLoginAt"),
                parentIsOnline = readBooleanField(fields, "parentIsOnline") ?: false,
                parentLastActiveMillis = readLongField(fields, "parentLastActiveMillis"),
                linkedParents = linkedParents
            )
        } catch (_: Exception) {
            null
        }
    }

    internal fun parseRunQueryDocuments(rawJson: String): List<JSONObject> {
        return try {
            val arr = JSONArray(rawJson)
            val docs = mutableListOf<JSONObject>()
            for (i in 0 until arr.length()) {
                val entry = arr.optJSONObject(i) ?: continue
                val doc = entry.optJSONObject("document") ?: continue
                docs.add(doc)
            }
            docs
        } catch (_: Exception) {
            emptyList()
        }
    }

    internal fun buildEqualityQueryPayload(
        collectionId: String,
        fieldPath: String,
        stringValue: String,
        limit: Int = 1,
        allDescendants: Boolean = false
    ): String {
        val fromEntry = JSONObject().put("collectionId", collectionId)
        if (allDescendants) {
            fromEntry.put("allDescendants", true)
        }
        val fieldFilter = JSONObject()
            .put("field", JSONObject().put("fieldPath", fieldPath))
            .put("op", "EQUAL")
            .put("value", JSONObject().put("stringValue", stringValue))
        val structuredQuery = JSONObject()
            .put("from", JSONArray().put(fromEntry))
            .put("where", JSONObject().put("fieldFilter", fieldFilter))
            .put("limit", limit)
        return JSONObject().put("structuredQuery", structuredQuery).toString()
    }

    // ---- Ağ Çağrıları ----

    suspend fun fetchUserProfile(
        context: Context,
        uid: String,
        fallbackName: String,
        fallbackEmail: String,
        bearerToken: String? = null
    ): RestFetchResult<UserProfile> = withContext(Dispatchers.IO) {
        val cfg = resolveConfig(context) ?: return@withContext RestFetchResult.Error(0, "config_missing")
        val url = buildDocumentUrl(cfg, listOf("users", uid))
        try {
            val (code, body) = executeGet(url, bearerToken)
            when {
                code == 404 -> RestFetchResult.NotFound
                code in 200..299 -> {
                    val parsed = parseUserProfileFromRestJson(uid, fallbackName, fallbackEmail, body)
                    if (parsed != null) RestFetchResult.Found(parsed)
                    else RestFetchResult.NotFound
                }
                else -> {
                    Log.w(TAG, "fetchUserProfile HTTP $code for uid=$uid: ${body.take(180)}")
                    RestFetchResult.Error(code, body.take(180))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "fetchUserProfile network error: ${e.message}")
            RestFetchResult.Error(0, e.message ?: "network_error")
        }
    }

    suspend fun checkDeletedAccountTombstone(
        context: Context,
        tombstoneId: String,
        bearerToken: String? = null
    ): RestFetchResult<JSONObject> = withContext(Dispatchers.IO) {
        val cfg = resolveConfig(context) ?: return@withContext RestFetchResult.Error(0, "config_missing")
        val url = buildDocumentUrl(cfg, listOf("deleted_accounts", tombstoneId))
        try {
            val (code, body) = executeGet(url, bearerToken)
            when {
                code == 404 -> RestFetchResult.NotFound
                code in 200..299 -> {
                    val json = JSONObject(body)
                    if (json.has("fields")) RestFetchResult.Found(json) else RestFetchResult.NotFound
                }
                else -> RestFetchResult.Error(code, body.take(160))
            }
        } catch (e: Exception) {
            RestFetchResult.Error(0, e.message ?: "network_error")
        }
    }

    suspend fun fetchClassroomById(
        context: Context,
        classroomId: String,
        bearerToken: String? = null
    ): RestFetchResult<ClassRoom> = withContext(Dispatchers.IO) {
        val cfg = resolveConfig(context) ?: return@withContext RestFetchResult.Error(0, "config_missing")
        val url = buildDocumentUrl(cfg, listOf("classrooms", classroomId))
        try {
            val (code, body) = executeGet(url, bearerToken)
            when {
                code == 404 -> RestFetchResult.NotFound
                code in 200..299 -> {
                    val room = parseClassRoomFromRestDoc(JSONObject(body))
                    if (room != null) RestFetchResult.Found(room) else RestFetchResult.NotFound
                }
                else -> {
                    Log.w(TAG, "fetchClassroomById HTTP $code for $classroomId")
                    RestFetchResult.Error(code, body.take(160))
                }
            }
        } catch (e: Exception) {
            RestFetchResult.Error(0, e.message ?: "network_error")
        }
    }

    suspend fun findTeacherClassroomByField(
        context: Context,
        fieldPath: String,
        fieldValue: String,
        fallbackUid: String,
        fallbackEmail: String,
        bearerToken: String? = null
    ): RestFetchResult<ClassRoom> = withContext(Dispatchers.IO) {
        if (fieldValue.isBlank()) return@withContext RestFetchResult.NotFound
        val cfg = resolveConfig(context) ?: return@withContext RestFetchResult.Error(0, "config_missing")
        val url = buildRunQueryUrl(cfg)
        val payload = buildEqualityQueryPayload(
            collectionId = "classrooms",
            fieldPath = fieldPath,
            stringValue = fieldValue,
            limit = 1,
            allDescendants = false
        )
        try {
            val (code, body) = executePostJson(url, payload, bearerToken)
            if (code in 200..299) {
                val docs = parseRunQueryDocuments(body)
                val firstRoom = docs.firstOrNull()?.let {
                    parseClassRoomFromRestDoc(it, fallbackUid = fallbackUid, fallbackEmail = fallbackEmail)
                }
                if (firstRoom != null) RestFetchResult.Found(firstRoom) else RestFetchResult.NotFound
            } else {
                Log.w(TAG, "findTeacherClassroomByField($fieldPath) HTTP $code")
                RestFetchResult.Error(code, body.take(160))
            }
        } catch (e: Exception) {
            RestFetchResult.Error(0, e.message ?: "network_error")
        }
    }

    suspend fun fetchStudentsInClassroom(
        context: Context,
        classroomId: String,
        bearerToken: String? = null
    ): RestFetchResult<List<Student>> = withContext(Dispatchers.IO) {
        val cfg = resolveConfig(context) ?: return@withContext RestFetchResult.Error(0, "config_missing")
        val url = buildDocumentUrl(cfg, listOf("classrooms", classroomId, "students")) + "&pageSize=100"
        try {
            val (code, body) = executeGet(url, bearerToken)
            if (code == 404) return@withContext RestFetchResult.NotFound
            if (code !in 200..299) return@withContext RestFetchResult.Error(code, body.take(160))
            val docsArr = JSONObject(body).optJSONArray("documents") ?: return@withContext RestFetchResult.Found(emptyList())
            val list = mutableListOf<Student>()
            for (i in 0 until docsArr.length()) {
                val doc = docsArr.optJSONObject(i) ?: continue
                parseStudentFromRestDoc(doc)?.let { list.add(it) }
            }
            RestFetchResult.Found(list)
        } catch (e: Exception) {
            RestFetchResult.Error(0, e.message ?: "network_error")
        }
    }

    suspend fun findStudentsLinkedToParentByEmail(
        context: Context,
        userEmail: String,
        bearerToken: String? = null
    ): RestFetchResult<List<Pair<Student, ClassRoom>>> = withContext(Dispatchers.IO) {
        val cleanEmail = userEmail.trim()
        val lowerEmail = cleanEmail.lowercase(Locale.ROOT)
        if (cleanEmail.isBlank()) return@withContext RestFetchResult.NotFound
        val cfg = resolveConfig(context) ?: return@withContext RestFetchResult.Error(0, "config_missing")
        val url = buildRunQueryUrl(cfg)
        val emailVariants = listOfNotNull(cleanEmail, lowerEmail.takeIf { it != cleanEmail }).distinct()
        val matchedDocs = mutableListOf<JSONObject>()
        var hadHttpError = false
        var lastErrCode = 0
        var lastErrDetail = ""

        for (emailVar in emailVariants) {
            val payload = buildEqualityQueryPayload(
                collectionId = "students",
                fieldPath = "parentEmail",
                stringValue = emailVar,
                limit = 10,
                allDescendants = true
            )
            try {
                val (code, body) = executePostJson(url, payload, bearerToken)
                if (code in 200..299) {
                    parseRunQueryDocuments(body).forEach { doc ->
                        val name = doc.optString("name")
                        if (matchedDocs.none { it.optString("name") == name }) {
                            matchedDocs.add(doc)
                        }
                    }
                } else {
                    hadHttpError = true
                    lastErrCode = code
                    lastErrDetail = body.take(160)
                }
            } catch (e: Exception) {
                hadHttpError = true
                lastErrDetail = e.message ?: "network_error"
            }
        }

        if (matchedDocs.isEmpty()) {
            return@withContext if (hadHttpError) {
                RestFetchResult.Error(lastErrCode, lastErrDetail)
            } else {
                RestFetchResult.NotFound
            }
        }

        val classCache = mutableMapOf<String, ClassRoom?>()
        val pairs = mutableListOf<Pair<Student, ClassRoom>>()
        for (doc in matchedDocs) {
            val student = parseStudentFromRestDoc(doc) ?: continue
            val matches = student.parentEmail?.trim().equals(cleanEmail, ignoreCase = true) ||
                student.allLinkedParents.any { it.email.trim().equals(cleanEmail, ignoreCase = true) }
            if (!matches) continue
            val classId = extractParentClassroomIdFromStudentPath(doc.optString("name"))
            if (classId.isBlank()) continue
            val room = classCache.getOrPut(classId) {
                when (val roomRes = fetchClassroomById(context, classId, bearerToken)) {
                    is RestFetchResult.Found -> roomRes.data
                    else -> null
                }
            } ?: continue
            if (pairs.none { it.first.id == student.id }) {
                pairs.add(Pair(student, room))
            }
        }

        if (pairs.isNotEmpty()) RestFetchResult.Found(pairs) else RestFetchResult.NotFound
    }
}
